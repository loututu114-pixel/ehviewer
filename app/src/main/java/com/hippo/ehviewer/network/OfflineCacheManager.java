package com.hippo.ehviewer.network;

import android.content.Context;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 离线缓存管理器 - 智能资源缓存和离线访问
 * 
 * 核心功能：
 * 1. 资源智能缓存
 * 2. 离线资源访问
 * 3. 缓存策略管理
 * 4. 缓存清理和优化
 * 5. 预加载管理
 * 6. 缓存统计分析
 */
public class OfflineCacheManager {
    private static final String TAG = "OfflineCacheManager";
    
    private final Context mContext;
    private boolean mEnabled = true;
    
    // 缓存配置
    private static final String CACHE_DIR_NAME = "offline_cache";
    private static final long MAX_CACHE_SIZE = 200 * 1024 * 1024; // 200MB
    private static final long MAX_SINGLE_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_CACHE_AGE_DAYS = 30; // 30天
    
    // 缓存目录
    private final File mCacheDir;
    private final File mMetadataFile;
    
    // 缓存索引
    private final Map<String, CacheEntry> mCacheIndex = new ConcurrentHashMap<>();
    
    // 线程池
    private final ExecutorService mExecutorService = Executors.newFixedThreadPool(3);
    
    // 预加载队列
    private final Queue<String> mPreloadQueue = new LinkedList<>();
    private final Set<String> mPreloadingUrls = ConcurrentHashMap.newKeySet();
    
    /**
     * 缓存条目信息
     */
    private static class CacheEntry {
        public String url;
        public String fileName;
        public String mimeType;
        public long fileSize;
        public long createdTime;
        public long lastAccessTime;
        public int accessCount;
        public boolean isPermanent; // 是否永久缓存
        
        public CacheEntry(String url, String fileName, String mimeType) {
            this.url = url;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = this.createdTime;
            this.accessCount = 0;
            this.isPermanent = false;
        }
        
        public boolean isExpired() {
            if (isPermanent) return false;
            
            long ageMillis = System.currentTimeMillis() - createdTime;
            long maxAgeMillis = MAX_CACHE_AGE_DAYS * 24 * 60 * 60 * 1000L;
            return ageMillis > maxAgeMillis;
        }
        
        @Override
        public String toString() {
            return String.format("CacheEntry[url=%s, size=%d, age=%dd, access=%d]", 
                url, fileSize, (System.currentTimeMillis() - createdTime) / (24 * 60 * 60 * 1000), accessCount);
        }
    }
    
    /**
     * 缓存策略枚举
     */
    public enum CacheStrategy {
        CACHE_FIRST,        // 优先使用缓存
        NETWORK_FIRST,      // 优先使用网络
        CACHE_ONLY,         // 仅使用缓存
        NETWORK_ONLY        // 仅使用网络
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public int totalEntries = 0;
        public long totalSize = 0;
        public int hitCount = 0;
        public int missCount = 0;
        public double hitRate = 0.0;
        
        @Override
        public String toString() {
            return String.format("缓存统计: %d个条目, %.1fMB, 命中率%.1f%% (%d/%d)", 
                totalEntries, totalSize / 1024.0 / 1024.0, hitRate * 100, hitCount, hitCount + missCount);
        }
    }
    
    public OfflineCacheManager(Context context) {
        mContext = context.getApplicationContext();
        
        // 初始化缓存目录
        mCacheDir = new File(mContext.getCacheDir(), CACHE_DIR_NAME);
        if (!mCacheDir.exists()) {
            boolean created = mCacheDir.mkdirs();
            Log.d(TAG, "Cache directory created: " + created);
        }
        
        mMetadataFile = new File(mCacheDir, "cache_metadata.dat");
        
        // 加载缓存索引
        loadCacheIndex();
        
        // 启动缓存维护
        scheduleCacheMaintenance();
        
        Log.d(TAG, "OfflineCacheManager initialized, cache dir: " + mCacheDir.getAbsolutePath());
    }
    
    /**
     * 设置缓存开关
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        Log.d(TAG, "Cache enabled: " + enabled);
    }
    
    /**
     * 检查资源是否已缓存
     */
    public boolean isCached(@NonNull String url) {
        if (!mEnabled) return false;
        
        String cacheKey = generateCacheKey(url);
        CacheEntry entry = mCacheIndex.get(cacheKey);
        
        if (entry != null && !entry.isExpired()) {
            File cacheFile = new File(mCacheDir, entry.fileName);
            return cacheFile.exists();
        }
        
        return false;
    }
    
    /**
     * 获取缓存的资源
     */
    @Nullable
    public WebResourceResponse getCachedResource(@NonNull String url) {
        if (!mEnabled) return null;
        
        try {
            String cacheKey = generateCacheKey(url);
            CacheEntry entry = mCacheIndex.get(cacheKey);
            
            if (entry == null || entry.isExpired()) {
                return null;
            }
            
            File cacheFile = new File(mCacheDir, entry.fileName);
            if (!cacheFile.exists()) {
                // 缓存文件不存在，清理索引
                mCacheIndex.remove(cacheKey);
                return null;
            }
            
            // 更新访问统计
            entry.lastAccessTime = System.currentTimeMillis();
            entry.accessCount++;
            
            // 创建输入流
            FileInputStream inputStream = new FileInputStream(cacheFile);
            
            Log.d(TAG, "Cache hit for: " + url);
            return new WebResourceResponse(entry.mimeType, "UTF-8", inputStream);
            
        } catch (Exception e) {
            Log.w(TAG, "Error getting cached resource for: " + url, e);
            return null;
        }
    }
    
    /**
     * 缓存资源
     */
    public Future<Boolean> cacheResource(@NonNull String url, @NonNull InputStream inputStream, 
                                       @Nullable String mimeType) {
        return mExecutorService.submit(() -> {
            if (!mEnabled) return false;
            
            try {
                String cacheKey = generateCacheKey(url);
                String fileName = generateFileName(cacheKey, mimeType);
                File cacheFile = new File(mCacheDir, fileName);
                
                // 检查文件大小限制
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > MAX_SINGLE_FILE_SIZE) {
                        Log.w(TAG, "Resource too large to cache: " + url + ", size: " + totalBytes);
                        return false;
                    }
                    baos.write(buffer, 0, bytesRead);
                }
                
                // 写入缓存文件
                try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                    baos.writeTo(fos);
                    fos.flush();
                }
                
                // 更新缓存索引
                CacheEntry entry = new CacheEntry(url, fileName, mimeType != null ? mimeType : "application/octet-stream");
                entry.fileSize = totalBytes;
                mCacheIndex.put(cacheKey, entry);
                
                // 保存索引
                saveCacheIndex();
                
                // 检查缓存大小限制
                checkCacheSizeLimit();
                
                Log.d(TAG, "Resource cached: " + url + ", size: " + totalBytes + " bytes");
                return true;
                
            } catch (Exception e) {
                Log.e(TAG, "Error caching resource: " + url, e);
                return false;
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing input stream", e);
                }
            }
        });
    }
    
    /**
     * 预加载资源
     */
    public void preloadResource(@NonNull String url) {
        if (!mEnabled) return;
        
        if (isCached(url) || mPreloadingUrls.contains(url)) {
            return;
        }
        
        synchronized (mPreloadQueue) {
            if (!mPreloadQueue.contains(url)) {
                mPreloadQueue.offer(url);
                processPreloadQueue();
            }
        }
    }
    
    /**
     * 批量预加载资源
     */
    public void preloadResources(@NonNull List<String> urls) {
        if (!mEnabled) return;
        
        synchronized (mPreloadQueue) {
            for (String url : urls) {
                if (!isCached(url) && !mPreloadingUrls.contains(url) && !mPreloadQueue.contains(url)) {
                    mPreloadQueue.offer(url);
                }
            }
            processPreloadQueue();
        }
    }
    
    /**
     * 清理过期缓存
     */
    public Future<Void> cleanExpiredCache() {
        return mExecutorService.submit(() -> {
            Log.d(TAG, "Starting expired cache cleanup");
            
            int removedCount = 0;
            long removedSize = 0;
            
            Iterator<Map.Entry<String, CacheEntry>> iterator = mCacheIndex.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry> entry = iterator.next();
                CacheEntry cacheEntry = entry.getValue();
                
                if (cacheEntry.isExpired()) {
                    File cacheFile = new File(mCacheDir, cacheEntry.fileName);
                    if (cacheFile.exists()) {
                        removedSize += cacheFile.length();
                        if (cacheFile.delete()) {
                            removedCount++;
                        }
                    }
                    iterator.remove();
                }
            }
            
            if (removedCount > 0) {
                saveCacheIndex();
                Log.d(TAG, String.format("Expired cache cleanup completed: %d files, %.1f MB", 
                    removedCount, removedSize / 1024.0 / 1024.0));
            }
            
            return null;
        });
    }
    
    /**
     * 清理所有缓存
     */
    public Future<Void> clearAllCache() {
        return mExecutorService.submit(() -> {
            Log.d(TAG, "Clearing all cache");
            
            int removedCount = 0;
            long removedSize = 0;
            
            for (CacheEntry entry : mCacheIndex.values()) {
                File cacheFile = new File(mCacheDir, entry.fileName);
                if (cacheFile.exists()) {
                    removedSize += cacheFile.length();
                    if (cacheFile.delete()) {
                        removedCount++;
                    }
                }
            }
            
            mCacheIndex.clear();
            saveCacheIndex();
            
            Log.d(TAG, String.format("All cache cleared: %d files, %.1f MB", 
                removedCount, removedSize / 1024.0 / 1024.0));
            
            return null;
        });
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        
        stats.totalEntries = mCacheIndex.size();
        
        for (CacheEntry entry : mCacheIndex.values()) {
            stats.totalSize += entry.fileSize;
            if (entry.accessCount > 0) {
                stats.hitCount += entry.accessCount;
            }
        }
        
        // 这里简化计算命中率
        int totalAccess = stats.hitCount + stats.missCount;
        if (totalAccess > 0) {
            stats.hitRate = (double) stats.hitCount / totalAccess;
        }
        
        return stats;
    }
    
    // 私有方法
    
    private String generateCacheKey(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "Error generating cache key", e);
            return String.valueOf(url.hashCode());
        }
    }
    
    private String generateFileName(String cacheKey, String mimeType) {
        String extension = getExtensionFromMimeType(mimeType);
        return cacheKey + extension;
    }
    
    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return "";
        
        if (mimeType.startsWith("text/html")) return ".html";
        if (mimeType.startsWith("text/css")) return ".css";
        if (mimeType.startsWith("application/javascript")) return ".js";
        if (mimeType.startsWith("image/jpeg")) return ".jpg";
        if (mimeType.startsWith("image/png")) return ".png";
        if (mimeType.startsWith("image/gif")) return ".gif";
        if (mimeType.startsWith("image/webp")) return ".webp";
        if (mimeType.startsWith("application/json")) return ".json";
        if (mimeType.startsWith("application/xml")) return ".xml";
        
        return "";
    }
    
    private void loadCacheIndex() {
        if (!mMetadataFile.exists()) {
            Log.d(TAG, "No cache metadata file found");
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mMetadataFile))) {
            @SuppressWarnings("unchecked")
            Map<String, CacheEntry> loadedIndex = (Map<String, CacheEntry>) ois.readObject();
            mCacheIndex.putAll(loadedIndex);
            
            Log.d(TAG, "Cache index loaded: " + mCacheIndex.size() + " entries");
            
        } catch (Exception e) {
            Log.w(TAG, "Error loading cache index", e);
            // 如果加载失败，删除损坏的元数据文件
            if (mMetadataFile.delete()) {
                Log.d(TAG, "Corrupted metadata file deleted");
            }
        }
    }
    
    private void saveCacheIndex() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(mMetadataFile))) {
            oos.writeObject(new HashMap<>(mCacheIndex));
            oos.flush();
            
            Log.d(TAG, "Cache index saved: " + mCacheIndex.size() + " entries");
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving cache index", e);
        }
    }
    
    private void checkCacheSizeLimit() {
        long totalSize = 0;
        for (CacheEntry entry : mCacheIndex.values()) {
            totalSize += entry.fileSize;
        }
        
        if (totalSize > MAX_CACHE_SIZE) {
            Log.d(TAG, "Cache size limit exceeded, cleaning up");
            cleanupLeastUsedCache(totalSize - MAX_CACHE_SIZE);
        }
    }
    
    private void cleanupLeastUsedCache(long bytesToRemove) {
        // 按访问频率和时间排序，删除最少使用的缓存
        List<CacheEntry> sortedEntries = new ArrayList<>(mCacheIndex.values());
        sortedEntries.sort((a, b) -> {
            // 首先按访问次数排序
            int accessCompare = Integer.compare(a.accessCount, b.accessCount);
            if (accessCompare != 0) return accessCompare;
            
            // 然后按最后访问时间排序
            return Long.compare(a.lastAccessTime, b.lastAccessTime);
        });
        
        long removedBytes = 0;
        int removedCount = 0;
        
        for (CacheEntry entry : sortedEntries) {
            if (entry.isPermanent) continue; // 跳过永久缓存
            
            File cacheFile = new File(mCacheDir, entry.fileName);
            if (cacheFile.exists()) {
                removedBytes += cacheFile.length();
                if (cacheFile.delete()) {
                    removedCount++;
                }
            }
            
            String cacheKey = generateCacheKey(entry.url);
            mCacheIndex.remove(cacheKey);
            
            if (removedBytes >= bytesToRemove) {
                break;
            }
        }
        
        if (removedCount > 0) {
            saveCacheIndex();
            Log.d(TAG, String.format("Cleanup completed: %d files, %.1f MB", 
                removedCount, removedBytes / 1024.0 / 1024.0));
        }
    }
    
    private void processPreloadQueue() {
        mExecutorService.execute(() -> {
            String url;
            synchronized (mPreloadQueue) {
                url = mPreloadQueue.poll();
            }
            
            if (url != null && !isCached(url)) {
                mPreloadingUrls.add(url);
                
                try {
                    // 这里实现资源预加载逻辑
                    // 简化实现，实际应该发起网络请求
                    Log.d(TAG, "Preloading resource: " + url);
                    
                    // 模拟预加载过程
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error preloading resource: " + url, e);
                } finally {
                    mPreloadingUrls.remove(url);
                }
            }
        });
    }
    
    private void scheduleCacheMaintenance() {
        // 定期清理过期缓存
        mExecutorService.execute(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(60 * 60 * 1000); // 每小时检查一次
                    cleanExpiredCache();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.d(TAG, "Cache maintenance thread interrupted");
            }
        });
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up OfflineCacheManager");
        
        // 保存缓存索引
        saveCacheIndex();
        
        // 关闭线程池
        mExecutorService.shutdown();
        
        synchronized (mPreloadQueue) {
            mPreloadQueue.clear();
        }
        mPreloadingUrls.clear();
        
        Log.d(TAG, "OfflineCacheManager cleanup completed");
    }
}