package com.hippo.ehviewer.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 网络请求缓存管理器
 * 智能缓存网络请求，提升加载速度，节省流量
 */
public class NetworkCacheManager {
    
    private static final String TAG = "NetworkCacheManager";
    private static final String PREFS_NAME = "network_cache";
    private static final String CACHE_DIR_NAME = "network_cache";
    
    // 缓存策略
    private static final long DEFAULT_CACHE_DURATION = 24 * 60 * 60 * 1000; // 24小时
    private static final long FAVICON_CACHE_DURATION = 7 * 24 * 60 * 60 * 1000; // 7天
    private static final long SEARCH_SUGGESTION_CACHE_DURATION = 30 * 60 * 1000; // 30分钟
    private static final long DNS_CACHE_DURATION = 10 * 60 * 1000; // 10分钟
    
    // 缓存大小限制
    private static final long MAX_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_SINGLE_CACHE_SIZE = 5 * 1024 * 1024; // 5MB单个文件
    private static final int MAX_CACHE_ENTRIES = 1000;
    
    private static volatile NetworkCacheManager sInstance;
    
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final File mCacheDir;
    private final ExecutorService mExecutor;
    
    // 内存缓存
    private final Map<String, CacheEntry> mMemoryCache;
    private final Map<String, Long> mCacheIndex; // URL -> 创建时间
    
    // 缓存统计
    private int mHitCount = 0;
    private int mMissCount = 0;
    private long mTotalSavedBytes = 0;
    private long mCurrentCacheSize = 0;
    
    /**
     * 缓存项数据结构
     */
    public static class CacheEntry {
        public final String url;
        public final byte[] data;
        public final Map<String, String> headers;
        public final long timestamp;
        public final long duration;
        public final String contentType;
        
        public CacheEntry(String url, byte[] data, Map<String, String> headers, 
                         long duration, String contentType) {
            this.url = url;
            this.data = data;
            this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
            this.timestamp = System.currentTimeMillis();
            this.duration = duration;
            this.contentType = contentType;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > duration;
        }
        
        public long getSize() {
            return data.length + url.length() * 2 + (contentType != null ? contentType.length() * 2 : 0);
        }
        
        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("url", url);
                json.put("timestamp", timestamp);
                json.put("duration", duration);
                json.put("contentType", contentType != null ? contentType : "");
                json.put("size", data.length);
                
                JSONObject headersJson = new JSONObject();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    headersJson.put(entry.getKey(), entry.getValue());
                }
                json.put("headers", headersJson);
                
                return json;
            } catch (Exception e) {
                Log.e(TAG, "Error converting cache entry to JSON", e);
                return new JSONObject();
            }
        }
        
        public static CacheEntry fromJson(JSONObject json, byte[] data) {
            try {
                String url = json.getString("url");
                long timestamp = json.getLong("timestamp");
                long duration = json.getLong("duration");
                String contentType = json.optString("contentType", null);
                
                Map<String, String> headers = new HashMap<>();
                JSONObject headersJson = json.optJSONObject("headers");
                if (headersJson != null) {
                    Iterator<String> keys = headersJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        headers.put(key, headersJson.getString(key));
                    }
                }
                
                CacheEntry entry = new CacheEntry(url, data, headers, duration, contentType);
                // 设置正确的时间戳
                return new CacheEntry(url, data, headers, duration, contentType) {
                    @Override
                    public boolean isExpired() {
                        return System.currentTimeMillis() - timestamp > duration;
                    }
                };
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cache entry from JSON", e);
                return null;
            }
        }
    }
    
    /**
     * 缓存策略接口
     */
    public interface CacheStrategy {
        boolean shouldCache(String url, int responseCode, Map<String, String> headers);
        long getCacheDuration(String url, Map<String, String> headers);
        boolean shouldUseCache(String url, CacheEntry entry);
    }
    
    /**
     * 默认缓存策略
     */
    public static class DefaultCacheStrategy implements CacheStrategy {
        @Override
        public boolean shouldCache(String url, int responseCode, Map<String, String> headers) {
            // 只缓存成功的响应
            if (responseCode < 200 || responseCode >= 300) {
                return false;
            }
            
            // 检查Cache-Control头
            String cacheControl = headers.get("cache-control");
            if (cacheControl != null && cacheControl.contains("no-cache")) {
                return false;
            }
            
            // 不缓存过大的响应
            String contentLength = headers.get("content-length");
            if (contentLength != null) {
                try {
                    long size = Long.parseLong(contentLength);
                    if (size > MAX_SINGLE_CACHE_SIZE) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
            
            return true;
        }
        
        @Override
        public long getCacheDuration(String url, Map<String, String> headers) {
            // 检查Expires头
            String expires = headers.get("expires");
            if (expires != null) {
                // TODO: 解析Expires头
            }
            
            // 检查Cache-Control的max-age
            String cacheControl = headers.get("cache-control");
            if (cacheControl != null && cacheControl.contains("max-age")) {
                // TODO: 解析max-age
            }
            
            // 根据文件类型决定缓存时长
            if (url.contains("favicon")) {
                return FAVICON_CACHE_DURATION;
            } else if (url.contains("suggest") || url.contains("search")) {
                return SEARCH_SUGGESTION_CACHE_DURATION;
            } else {
                return DEFAULT_CACHE_DURATION;
            }
        }
        
        @Override
        public boolean shouldUseCache(String url, CacheEntry entry) {
            return !entry.isExpired();
        }
    }
    
    private CacheStrategy mCacheStrategy;
    
    private NetworkCacheManager(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mCacheDir = new File(mContext.getCacheDir(), CACHE_DIR_NAME);
        mExecutor = Executors.newSingleThreadExecutor();
        
        mMemoryCache = new ConcurrentHashMap<>();
        mCacheIndex = new ConcurrentHashMap<>();
        mCacheStrategy = new DefaultCacheStrategy();
        
        // 确保缓存目录存在
        if (!mCacheDir.exists()) {
            mCacheDir.mkdirs();
        }
        
        // 加载缓存索引
        loadCacheIndex();
        
        // 定期清理过期缓存
        scheduleCleanup();
    }
    
    public static NetworkCacheManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (NetworkCacheManager.class) {
                if (sInstance == null) {
                    sInstance = new NetworkCacheManager(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 设置缓存策略
     */
    public void setCacheStrategy(@NonNull CacheStrategy strategy) {
        mCacheStrategy = strategy;
    }
    
    /**
     * 获取缓存的响应
     */
    @Nullable
    public CacheEntry getCachedResponse(@NonNull String url) {
        try {
            // 首先检查内存缓存
            CacheEntry memoryEntry = mMemoryCache.get(url);
            if (memoryEntry != null) {
                if (mCacheStrategy.shouldUseCache(url, memoryEntry)) {
                    mHitCount++;
                    Log.d(TAG, "Memory cache hit: " + url);
                    return memoryEntry;
                } else {
                    // 缓存过期，从内存中移除
                    mMemoryCache.remove(url);
                }
            }
            
            // 检查磁盘缓存
            CacheEntry diskEntry = loadFromDisk(url);
            if (diskEntry != null) {
                if (mCacheStrategy.shouldUseCache(url, diskEntry)) {
                    // 加载到内存缓存
                    mMemoryCache.put(url, diskEntry);
                    mHitCount++;
                    Log.d(TAG, "Disk cache hit: " + url);
                    return diskEntry;
                } else {
                    // 缓存过期，删除磁盘文件
                    deleteDiskCache(url);
                }
            }
            
            mMissCount++;
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting cached response: " + url, e);
            mMissCount++;
            return null;
        }
    }
    
    /**
     * 缓存响应
     */
    public void cacheResponse(@NonNull String url, @NonNull byte[] data, 
                             @Nullable Map<String, String> headers, 
                             int responseCode, @Nullable String contentType) {
        
        if (!mCacheStrategy.shouldCache(url, responseCode, headers)) {
            Log.d(TAG, "Response not cacheable: " + url);
            return;
        }
        
        mExecutor.execute(() -> {
            try {
                long duration = mCacheStrategy.getCacheDuration(url, headers);
                CacheEntry entry = new CacheEntry(url, data, headers, duration, contentType);
                
                // 检查缓存大小限制
                if (data.length > MAX_SINGLE_CACHE_SIZE) {
                    Log.w(TAG, "Response too large to cache: " + url + " (" + data.length + " bytes)");
                    return;
                }
                
                // 加到内存缓存
                mMemoryCache.put(url, entry);
                
                // 保存到磁盘
                saveToDisk(url, entry);
                
                // 更新统计
                mTotalSavedBytes += data.length;
                mCurrentCacheSize += data.length;
                
                // 检查是否需要清理
                if (mCurrentCacheSize > MAX_CACHE_SIZE || mCacheIndex.size() > MAX_CACHE_ENTRIES) {
                    cleanupCache();
                }
                
                Log.d(TAG, "Cached response: " + url + " (" + data.length + " bytes)");
                
            } catch (Exception e) {
                Log.e(TAG, "Error caching response: " + url, e);
            }
        });
    }
    
    /**
     * 从磁盘加载缓存
     */
    @Nullable
    private CacheEntry loadFromDisk(@NonNull String url) {
        try {
            String filename = generateCacheFilename(url);
            File cacheFile = new File(mCacheDir, filename);
            File metaFile = new File(mCacheDir, filename + ".meta");
            
            if (!cacheFile.exists() || !metaFile.exists()) {
                return null;
            }
            
            // 读取数据
            byte[] data = readFileToByteArray(cacheFile);
            if (data == null) {
                return null;
            }
            
            // 读取元数据
            String metaJson = readFileToString(metaFile);
            if (metaJson == null) {
                return null;
            }
            
            JSONObject meta = new JSONObject(metaJson);
            return CacheEntry.fromJson(meta, data);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading from disk: " + url, e);
            return null;
        }
    }
    
    /**
     * 保存到磁盘
     */
    private void saveToDisk(@NonNull String url, @NonNull CacheEntry entry) {
        try {
            String filename = generateCacheFilename(url);
            File cacheFile = new File(mCacheDir, filename);
            File metaFile = new File(mCacheDir, filename + ".meta");
            
            // 保存数据
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(entry.data);
            }
            
            // 保存元数据
            try (FileOutputStream fos = new FileOutputStream(metaFile)) {
                fos.write(entry.toJson().toString().getBytes());
            }
            
            // 更新索引
            mCacheIndex.put(url, entry.timestamp);
            saveCacheIndex();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving to disk: " + url, e);
        }
    }
    
    /**
     * 删除磁盘缓存
     */
    private void deleteDiskCache(@NonNull String url) {
        try {
            String filename = generateCacheFilename(url);
            File cacheFile = new File(mCacheDir, filename);
            File metaFile = new File(mCacheDir, filename + ".meta");
            
            if (cacheFile.exists()) {
                mCurrentCacheSize -= cacheFile.length();
                cacheFile.delete();
            }
            if (metaFile.exists()) {
                metaFile.delete();
            }
            
            mCacheIndex.remove(url);
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting disk cache: " + url, e);
        }
    }
    
    /**
     * 清理过期和过多的缓存
     */
    private void cleanupCache() {
        mExecutor.execute(() -> {
            try {
                Log.d(TAG, "Starting cache cleanup...");
                
                // 1. 清理过期的内存缓存
                for (String url : mMemoryCache.keySet()) {
                    CacheEntry entry = mMemoryCache.get(url);
                    if (entry != null && entry.isExpired()) {
                        mMemoryCache.remove(url);
                    }
                }
                
                // 2. 清理过期的磁盘缓存
                for (String url : mCacheIndex.keySet()) {
                    CacheEntry entry = loadFromDisk(url);
                    if (entry == null || entry.isExpired()) {
                        deleteDiskCache(url);
                    }
                }
                
                // 3. 如果还是超出限制，按时间删除最老的缓存
                while (mCurrentCacheSize > MAX_CACHE_SIZE * 0.8 && !mCacheIndex.isEmpty()) {
                    // 找到最老的缓存项
                    String oldestUrl = null;
                    long oldestTime = Long.MAX_VALUE;
                    
                    for (Map.Entry<String, Long> entry : mCacheIndex.entrySet()) {
                        if (entry.getValue() < oldestTime) {
                            oldestTime = entry.getValue();
                            oldestUrl = entry.getKey();
                        }
                    }
                    
                    if (oldestUrl != null) {
                        deleteDiskCache(oldestUrl);
                        mMemoryCache.remove(oldestUrl);
                    }
                }
                
                saveCacheIndex();
                Log.d(TAG, "Cache cleanup completed. Current size: " + formatSize(mCurrentCacheSize));
                
            } catch (Exception e) {
                Log.e(TAG, "Error in cache cleanup", e);
            }
        });
    }
    
    /**
     * 定期清理
     */
    private void scheduleCleanup() {
        // 每小时执行一次清理
        mExecutor.execute(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(60 * 60 * 1000); // 1小时
                    cleanupCache();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * 加载缓存索引
     */
    private void loadCacheIndex() {
        try {
            String indexJson = mPrefs.getString("cache_index", "{}");
            JSONObject index = new JSONObject(indexJson);
            
            Iterator<String> urls = index.keys();
            while (urls.hasNext()) {
                String url = urls.next();
                mCacheIndex.put(url, index.getLong(url));
            }
            
            // 计算当前缓存大小
            calculateCurrentCacheSize();
            
            Log.d(TAG, "Loaded cache index: " + mCacheIndex.size() + " entries, " + 
                formatSize(mCurrentCacheSize));
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading cache index", e);
        }
    }
    
    /**
     * 保存缓存索引
     */
    private void saveCacheIndex() {
        try {
            JSONObject index = new JSONObject();
            for (Map.Entry<String, Long> entry : mCacheIndex.entrySet()) {
                index.put(entry.getKey(), entry.getValue());
            }
            
            mPrefs.edit().putString("cache_index", index.toString()).apply();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving cache index", e);
        }
    }
    
    /**
     * 计算当前缓存大小
     */
    private void calculateCurrentCacheSize() {
        mCurrentCacheSize = 0;
        if (mCacheDir.exists()) {
            File[] files = mCacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.getName().endsWith(".meta")) {
                        mCurrentCacheSize += file.length();
                    }
                }
            }
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        mExecutor.execute(() -> {
            try {
                // 清空内存缓存
                mMemoryCache.clear();
                
                // 删除所有磁盘文件
                if (mCacheDir.exists()) {
                    File[] files = mCacheDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            file.delete();
                        }
                    }
                }
                
                // 清空索引
                mCacheIndex.clear();
                saveCacheIndex();
                
                // 重置统计
                mCurrentCacheSize = 0;
                
                Log.d(TAG, "All cache cleared");
                
            } catch (Exception e) {
                Log.e(TAG, "Error clearing all cache", e);
            }
        });
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            mHitCount,
            mMissCount,
            mHitCount + mMissCount > 0 ? (float) mHitCount / (mHitCount + mMissCount) * 100 : 0,
            mCurrentCacheSize,
            mTotalSavedBytes,
            mMemoryCache.size(),
            mCacheIndex.size()
        );
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final int hitCount;
        public final int missCount;
        public final float hitRate;
        public final long currentCacheSize;
        public final long totalSavedBytes;
        public final int memoryCacheSize;
        public final int diskCacheSize;
        
        public CacheStats(int hitCount, int missCount, float hitRate, 
                         long currentCacheSize, long totalSavedBytes,
                         int memoryCacheSize, int diskCacheSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.currentCacheSize = currentCacheSize;
            this.totalSavedBytes = totalSavedBytes;
            this.memoryCacheSize = memoryCacheSize;
            this.diskCacheSize = diskCacheSize;
        }
        
        public String getFormattedStats() {
            return String.format("命中率: %.1f%% (%d/%d) | 缓存大小: %s | 节省流量: %s | 内存/磁盘: %d/%d",
                hitRate, hitCount, hitCount + missCount,
                formatSize(currentCacheSize), formatSize(totalSavedBytes),
                memoryCacheSize, diskCacheSize);
        }
    }
    
    // 辅助方法
    private String generateCacheFilename(@NonNull String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(url.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(url.hashCode());
        }
    }
    
    private byte[] readFileToByteArray(@NonNull File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + file.getPath(), e);
            return null;
        }
    }
    
    private String readFileToString(@NonNull File file) {
        byte[] data = readFileToByteArray(file);
        return data != null ? new String(data) : null;
    }
    
    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}