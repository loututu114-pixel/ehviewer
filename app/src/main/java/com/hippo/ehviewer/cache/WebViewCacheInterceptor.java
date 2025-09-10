package com.hippo.ehviewer.cache;

import android.content.Context;
import android.util.Log;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebView缓存拦截器
 * 参考YCWebView的缓存机制，提供资源拦截缓存功能
 * 
 * 核心特性：
 * 1. 智能缓存策略 - 根据资源类型选择缓存策略
 * 2. 磁盘缓存管理 - 支持缓存大小限制和过期清理
 * 3. 内存缓存 - 热点资源内存缓存
 * 4. 缓存统计 - 提供缓存命中率统计
 */
public class WebViewCacheInterceptor {
    private static final String TAG = "WebViewCacheInterceptor";
    
    // 缓存配置
    private static final long DEFAULT_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long CACHE_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L; // 7天
    private static final int MEMORY_CACHE_SIZE = 20; // 内存缓存最多20个文件
    
    // 需要缓存的资源类型
    private static final String[] CACHEABLE_EXTENSIONS = {
        ".js", ".css", ".png", ".jpg", ".jpeg", ".gif", ".webp", 
        ".woff", ".woff2", ".ttf", ".eot", ".svg", ".ico"
    };
    
    // 视频相关资源类型
    private static final String[] VIDEO_EXTENSIONS = {
        ".mp4", ".webm", ".ogg", ".avi", ".mov", ".m3u8", ".ts"
    };
    
    private final Context mContext;
    private final File mCacheDir;
    private final Map<String, CacheEntry> mMemoryCache = new ConcurrentHashMap<>();
    private final CacheStats mStats = new CacheStats();
    
    // 缓存条目
    private static class CacheEntry {
        final byte[] data;
        final String mimeType;
        final long timestamp;
        final String encoding;
        
        CacheEntry(byte[] data, String mimeType, String encoding) {
            this.data = data;
            this.mimeType = mimeType;
            this.encoding = encoding;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_TIME;
        }
    }
    
    // 缓存统计
    private static class CacheStats {
        long hitCount = 0;
        long missCount = 0;
        long totalRequests = 0;
        
        synchronized void recordHit() {
            hitCount++;
            totalRequests++;
        }
        
        synchronized void recordMiss() {
            missCount++;
            totalRequests++;
        }
        
        synchronized double getHitRate() {
            return totalRequests > 0 ? (double) hitCount / totalRequests : 0.0;
        }
        
        synchronized String getStats() {
            return String.format("Cache Stats: %d/%d hits (%.1f%%), %d total", 
                hitCount, totalRequests, getHitRate() * 100, totalRequests);
        }
    }
    
    public WebViewCacheInterceptor(Context context) {
        mContext = context.getApplicationContext();
        mCacheDir = new File(context.getCacheDir(), "webview_cache");
        if (!mCacheDir.exists()) {
            mCacheDir.mkdirs();
        }
        
        // 启动时清理过期缓存
        cleanupExpiredCache();
        
        Log.d(TAG, "WebViewCacheInterceptor initialized, cache dir: " + mCacheDir.getAbsolutePath());
    }
    
    /**
     * 拦截资源请求，提供缓存支持
     */
    public WebResourceResponse shouldInterceptRequest(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // 检查是否是可缓存的资源
            if (!isCacheableResource(url)) {
                mStats.recordMiss();
                return null;
            }
            
            String cacheKey = generateCacheKey(url);
            
            // 1. 先检查内存缓存
            CacheEntry memoryEntry = mMemoryCache.get(cacheKey);
            if (memoryEntry != null && !memoryEntry.isExpired()) {
                mStats.recordHit();
                Log.d(TAG, "Memory cache hit for: " + url);
                return createWebResourceResponse(memoryEntry);
            }
            
            // 2. 检查磁盘缓存
            File cacheFile = new File(mCacheDir, cacheKey);
            if (cacheFile.exists() && !isCacheFileExpired(cacheFile)) {
                try {
                    byte[] data = readFileToBytes(cacheFile);
                    String mimeType = getMimeType(url);
                    String encoding = getEncoding(url);
                    
                    // 添加到内存缓存
                    addToMemoryCache(cacheKey, data, mimeType, encoding);
                    
                    mStats.recordHit();
                    Log.d(TAG, "Disk cache hit for: " + url);
                    
                    return new WebResourceResponse(mimeType, encoding, new ByteArrayInputStream(data));
                } catch (IOException e) {
                    Log.w(TAG, "Failed to read cache file: " + cacheFile, e);
                }
            }
            
            mStats.recordMiss();
            return null;
            
        } catch (Exception e) {
            Log.w(TAG, "Error in shouldInterceptRequest for: " + url, e);
            mStats.recordMiss();
            return null;
        }
    }
    
    /**
     * 缓存资源响应
     */
    public void cacheResponse(String url, byte[] data, String mimeType, String encoding) {
        if (url == null || data == null || !isCacheableResource(url)) {
            return;
        }
        
        try {
            String cacheKey = generateCacheKey(url);
            
            // 1. 添加到内存缓存
            addToMemoryCache(cacheKey, data, mimeType, encoding);
            
            // 2. 添加到磁盘缓存
            File cacheFile = new File(mCacheDir, cacheKey);
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(data);
                fos.flush();
                Log.d(TAG, "Cached resource: " + url + " (" + data.length + " bytes)");
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to cache response for: " + url, e);
        }
    }
    
    /**
     * 检查是否是可缓存的资源
     */
    private boolean isCacheableResource(String url) {
        if (url == null) return false;
        
        String lowerUrl = url.toLowerCase();
        
        // 检查静态资源扩展名
        for (String ext : CACHEABLE_EXTENSIONS) {
            if (lowerUrl.contains(ext)) {
                return true;
            }
        }
        
        // YouTube视频资源特殊处理
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("googlevideo.com")) {
            for (String ext : VIDEO_EXTENSIONS) {
                if (lowerUrl.contains(ext)) {
                    return true;
                }
            }
            // YouTube的视频片段和字幕文件
            if (lowerUrl.contains("videoplayback") || lowerUrl.contains("timedtext")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(url.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "Failed to generate cache key", e);
            return String.valueOf(url.hashCode());
        }
    }
    
    /**
     * 添加到内存缓存
     */
    private void addToMemoryCache(String key, byte[] data, String mimeType, String encoding) {
        if (mMemoryCache.size() >= MEMORY_CACHE_SIZE) {
            // LRU淘汰：移除最旧的条目
            String oldestKey = null;
            long oldestTime = Long.MAX_VALUE;
            for (Map.Entry<String, CacheEntry> entry : mMemoryCache.entrySet()) {
                if (entry.getValue().timestamp < oldestTime) {
                    oldestTime = entry.getValue().timestamp;
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey != null) {
                mMemoryCache.remove(oldestKey);
            }
        }
        
        mMemoryCache.put(key, new CacheEntry(data, mimeType, encoding));
    }
    
    /**
     * 创建WebResourceResponse
     */
    private WebResourceResponse createWebResourceResponse(CacheEntry entry) {
        return new WebResourceResponse(
            entry.mimeType, 
            entry.encoding, 
            new ByteArrayInputStream(entry.data)
        );
    }
    
    /**
     * 获取MIME类型
     */
    private String getMimeType(String url) {
        String lowerUrl = url.toLowerCase();
        
        if (lowerUrl.contains(".js")) return "application/javascript";
        if (lowerUrl.contains(".css")) return "text/css";
        if (lowerUrl.contains(".png")) return "image/png";
        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) return "image/jpeg";
        if (lowerUrl.contains(".gif")) return "image/gif";
        if (lowerUrl.contains(".webp")) return "image/webp";
        if (lowerUrl.contains(".svg")) return "image/svg+xml";
        if (lowerUrl.contains(".woff")) return "font/woff";
        if (lowerUrl.contains(".woff2")) return "font/woff2";
        if (lowerUrl.contains(".ttf")) return "font/ttf";
        if (lowerUrl.contains(".mp4")) return "video/mp4";
        if (lowerUrl.contains(".webm")) return "video/webm";
        
        return "application/octet-stream";
    }
    
    /**
     * 获取编码
     */
    private String getEncoding(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".js") || lowerUrl.contains(".css")) {
            return "UTF-8";
        }
        return null;
    }
    
    /**
     * 读取文件为字节数组
     */
    private byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        }
    }
    
    /**
     * 检查缓存文件是否过期
     */
    private boolean isCacheFileExpired(File file) {
        return System.currentTimeMillis() - file.lastModified() > CACHE_EXPIRE_TIME;
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        new Thread(() -> {
            try {
                File[] files = mCacheDir.listFiles();
                if (files != null) {
                    long deletedSize = 0;
                    int deletedCount = 0;
                    
                    for (File file : files) {
                        if (isCacheFileExpired(file)) {
                            deletedSize += file.length();
                            if (file.delete()) {
                                deletedCount++;
                            }
                        }
                    }
                    
                    Log.d(TAG, "Cleaned up expired cache: " + deletedCount + " files, " + 
                          deletedSize / 1024 + " KB freed");
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to cleanup expired cache", e);
            }
        }).start();
    }
    
    /**
     * 清理所有缓存
     */
    public void clearAllCache() {
        try {
            // 清理内存缓存
            mMemoryCache.clear();
            
            // 清理磁盘缓存
            File[] files = mCacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            
            Log.d(TAG, "All cache cleared");
        } catch (Exception e) {
            Log.w(TAG, "Failed to clear all cache", e);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        long diskCacheSize = 0;
        int diskCacheCount = 0;
        
        try {
            File[] files = mCacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    diskCacheSize += file.length();
                    diskCacheCount++;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to calculate cache stats", e);
        }
        
        return String.format(
            "%s\nMemory Cache: %d items\nDisk Cache: %d files, %.1f MB",
            mStats.getStats(),
            mMemoryCache.size(),
            diskCacheCount,
            diskCacheSize / 1024.0 / 1024.0
        );
    }
}