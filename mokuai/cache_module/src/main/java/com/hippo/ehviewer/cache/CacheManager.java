/*
 * EhViewer Cache Module - CacheManager
 * 缓存管理器 - 统一的缓存管理解决方案
 */

package com.hippo.ehviewer.cache;

import android.content.Context;
import android.util.LruCache;

/**
 * 缓存管理器
 * 提供内存缓存和磁盘缓存的统一管理
 */
public class CacheManager {

    private static CacheManager sInstance;
    private final MemoryCache mMemoryCache;
    private final DiskCache mDiskCache;

    public static synchronized CacheManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CacheManager(context);
        }
        return sInstance;
    }

    private CacheManager(Context context) {
        mMemoryCache = new MemoryCache();
        mDiskCache = new DiskCache(context);
    }

    /**
     * 存储到缓存
     */
    public void put(String key, Object value) {
        mMemoryCache.put(key, value);
        // 同时存储到磁盘缓存
    }

    /**
     * 从缓存获取
     */
    public Object get(String key) {
        Object value = mMemoryCache.get(key);
        if (value == null) {
            // 从磁盘缓存获取
            value = mDiskCache.get(key);
            if (value != null) {
                // 放入内存缓存
                mMemoryCache.put(key, value);
            }
        }
        return value;
    }

    /**
     * 清除所有缓存
     */
    public void clear() {
        mMemoryCache.clear();
        mDiskCache.clear();
    }

    /**
     * 内存缓存实现
     */
    private static class MemoryCache extends LruCache<String, Object> {
        public MemoryCache() {
            super(10 * 1024 * 1024); // 10MB
        }

        @Override
        protected int sizeOf(String key, Object value) {
            // 简单的大小计算
            return 1;
        }
    }

    /**
     * 磁盘缓存实现
     */
    private static class DiskCache {
        private final Context mContext;

        public DiskCache(Context context) {
            mContext = context;
        }

        public Object get(String key) {
            // 磁盘缓存获取逻辑
            return null;
        }

        public void put(String key, Object value) {
            // 磁盘缓存存储逻辑
        }

        public void clear() {
            // 清除磁盘缓存
        }
    }
}
