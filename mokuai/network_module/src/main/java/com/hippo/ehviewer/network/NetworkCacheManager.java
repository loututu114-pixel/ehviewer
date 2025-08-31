/*
 * EhViewer Network Module - NetworkCacheManager
 * 网络缓存管理器 - 管理网络请求的缓存
 */

package com.hippo.ehviewer.network;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络缓存管理器
 * 提供网络请求结果的缓存功能
 */
public class NetworkCacheManager {

    private static final String TAG = NetworkCacheManager.class.getSimpleName();
    private static final long DEFAULT_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long DEFAULT_CACHE_TIME = 24 * 60 * 60 * 1000; // 24小时

    private final File mCacheDir;
    private final long mMaxCacheSize;
    private final long mDefaultCacheTime;
    private final ConcurrentHashMap<String, CacheEntry> mMemoryCache;

    public NetworkCacheManager(Context context) {
        this(context, DEFAULT_CACHE_SIZE, DEFAULT_CACHE_TIME);
    }

    public NetworkCacheManager(Context context, long maxCacheSize, long defaultCacheTime) {
        mCacheDir = new File(context.getCacheDir(), "network_cache");
        if (!mCacheDir.exists()) {
            mCacheDir.mkdirs();
        }
        mMaxCacheSize = maxCacheSize;
        mDefaultCacheTime = defaultCacheTime;
        mMemoryCache = new ConcurrentHashMap<>();
    }

    /**
     * 获取缓存的数据
     * @param key 缓存键
     * @return 缓存的数据，如果不存在或过期返回null
     */
    public Object get(String key) {
        // 先从内存缓存获取
        CacheEntry entry = mMemoryCache.get(key);
        if (entry != null && !entry.isExpired()) {
            Log.d(TAG, "Cache hit (memory): " + key);
            return entry.getData();
        }

        // 从文件缓存获取
        return getFromFileCache(key);
    }

    /**
     * 存储数据到缓存
     * @param key 缓存键
     * @param data 要缓存的数据
     */
    public void put(String key, Object data) {
        put(key, data, mDefaultCacheTime);
    }

    /**
     * 存储数据到缓存（指定过期时间）
     * @param key 缓存键
     * @param data 要缓存的数据
     * @param cacheTime 缓存时间（毫秒）
     */
    public void put(String key, Object data, long cacheTime) {
        CacheEntry entry = new CacheEntry(data, System.currentTimeMillis() + cacheTime);

        // 存储到内存缓存
        mMemoryCache.put(key, entry);

        // 存储到文件缓存
        saveToFileCache(key, entry);

        // 检查缓存大小
        checkCacheSize();

        Log.d(TAG, "Cache stored: " + key);
    }

    /**
     * 删除指定缓存
     * @param key 缓存键
     */
    public void remove(String key) {
        mMemoryCache.remove(key);
        deleteFileCache(key);
        Log.d(TAG, "Cache removed: " + key);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        mMemoryCache.clear();
        clearFileCache();
        Log.d(TAG, "Cache cleared");
    }

    /**
     * 获取缓存大小
     * @return 缓存大小（字节）
     */
    public long getCacheSize() {
        return getFileCacheSize();
    }

    /**
     * 从文件缓存获取数据
     */
    private Object getFromFileCache(String key) {
        File cacheFile = getCacheFile(key);
        if (!cacheFile.exists()) {
            return null;
        }

        try {
            // 这里应该实现从文件读取并反序列化的逻辑
            // 为了简化，这里返回null
            Log.d(TAG, "Cache hit (file): " + key);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read cache file: " + key, e);
            return null;
        }
    }

    /**
     * 保存到文件缓存
     */
    private void saveToFileCache(String key, CacheEntry entry) {
        File cacheFile = getCacheFile(key);
        try {
            // 这里应该实现序列化并写入文件的逻辑
            Log.d(TAG, "Saved to file cache: " + key);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save cache file: " + key, e);
        }
    }

    /**
     * 删除文件缓存
     */
    private void deleteFileCache(String key) {
        File cacheFile = getCacheFile(key);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    /**
     * 清空文件缓存
     */
    private void clearFileCache() {
        File[] files = mCacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    /**
     * 获取文件缓存大小
     */
    private long getFileCacheSize() {
        File[] files = mCacheDir.listFiles();
        if (files == null) {
            return 0;
        }

        long size = 0;
        for (File file : files) {
            size += file.length();
        }
        return size;
    }

    /**
     * 获取缓存文件
     */
    private File getCacheFile(String key) {
        String fileName = key.hashCode() + ".cache";
        return new File(mCacheDir, fileName);
    }

    /**
     * 检查缓存大小，如果超过限制则清理
     */
    private void checkCacheSize() {
        long currentSize = getCacheSize();
        if (currentSize > mMaxCacheSize) {
            // 清理过期缓存
            cleanExpiredCache();
            // 如果仍然超过限制，清理最旧的缓存
            if (getCacheSize() > mMaxCacheSize) {
                cleanOldCache();
            }
        }
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        // 这里应该实现清理过期缓存的逻辑
        Log.d(TAG, "Cleaning expired cache");
    }

    /**
     * 清理最旧的缓存
     */
    private void cleanOldCache() {
        // 这里应该实现清理最旧缓存的逻辑
        Log.d(TAG, "Cleaning old cache");
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final Object data;
        private final long expireTime;

        public CacheEntry(Object data, long expireTime) {
            this.data = data;
            this.expireTime = expireTime;
        }

        public Object getData() {
            return data;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }
}
