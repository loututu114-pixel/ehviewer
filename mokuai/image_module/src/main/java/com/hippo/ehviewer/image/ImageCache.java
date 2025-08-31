/*
 * EhViewer Image Module - ImageCache
 * 图片缓存管理器 - 提供内存缓存和磁盘缓存功能
 */

package com.hippo.ehviewer.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 图片缓存管理器
 * 支持内存缓存和磁盘缓存
 */
public class ImageCache {

    private static final String TAG = ImageCache.class.getSimpleName();
    private static final int DEFAULT_MEMORY_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int DEFAULT_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String CACHE_DIR = "image_cache";

    private final Context mContext;
    private final LruCache<String, Bitmap> mMemoryCache;
    private final DiskLruCache mDiskCache;
    private final Object mDiskCacheLock = new Object();

    public ImageCache(Context context) {
        this(context, DEFAULT_MEMORY_CACHE_SIZE, DEFAULT_DISK_CACHE_SIZE);
    }

    public ImageCache(Context context, int memoryCacheSize, int diskCacheSize) {
        mContext = context;

        // 初始化内存缓存
        mMemoryCache = new LruCache<String, Bitmap>(memoryCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                // 可以在这里处理被移除的缓存项
            }
        };

        // 初始化磁盘缓存
        File cacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        try {
            mDiskCache = new DiskLruCache(cacheDir, diskCacheSize);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize disk cache", e);
        }
    }

    /**
     * 从内存缓存获取图片
     * @param key 缓存键
     * @return 缓存的Bitmap，如果不存在返回null
     */
    public Bitmap getFromMemory(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * 从磁盘缓存获取图片
     * @param key 缓存键
     * @return 缓存的Bitmap，如果不存在返回null
     */
    public Bitmap getFromDisk(String key) {
        synchronized (mDiskCacheLock) {
            try {
                return mDiskCache.get(key);
            } catch (IOException e) {
                return null;
            }
        }
    }

    /**
     * 获取图片（先内存后磁盘）
     * @param key 缓存键
     * @return 缓存的Bitmap，如果不存在返回null
     */
    public Bitmap get(String key) {
        // 先从内存缓存获取
        Bitmap bitmap = getFromMemory(key);
        if (bitmap != null) {
            return bitmap;
        }

        // 从磁盘缓存获取
        bitmap = getFromDisk(key);
        if (bitmap != null) {
            // 放入内存缓存
            putToMemory(key, bitmap);
        }

        return bitmap;
    }

    /**
     * 放入内存缓存
     * @param key 缓存键
     * @param bitmap 图片
     */
    public void putToMemory(String key, Bitmap bitmap) {
        if (key != null && bitmap != null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * 放入磁盘缓存
     * @param key 缓存键
     * @param bitmap 图片
     */
    public void putToDisk(String key, Bitmap bitmap) {
        synchronized (mDiskCacheLock) {
            try {
                mDiskCache.put(key, bitmap);
            } catch (IOException e) {
                // 忽略磁盘缓存失败
            }
        }
    }

    /**
     * 放入缓存（同时放入内存和磁盘）
     * @param key 缓存键
     * @param bitmap 图片
     */
    public void put(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) {
            return;
        }

        // 放入内存缓存
        putToMemory(key, bitmap);

        // 放入磁盘缓存
        putToDisk(key, bitmap);
    }

    /**
     * 移除缓存
     * @param key 缓存键
     */
    public void remove(String key) {
        if (key == null) {
            return;
        }

        // 从内存缓存移除
        mMemoryCache.remove(key);

        // 从磁盘缓存移除
        synchronized (mDiskCacheLock) {
            try {
                mDiskCache.remove(key);
            } catch (IOException e) {
                // 忽略移除失败
            }
        }
    }

    /**
     * 清空内存缓存
     */
    public void clearMemoryCache() {
        mMemoryCache.evictAll();
    }

    /**
     * 清空磁盘缓存
     */
    public void clearDiskCache() {
        synchronized (mDiskCacheLock) {
            try {
                mDiskCache.clear();
            } catch (IOException e) {
                // 忽略清空失败
            }
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        clearMemoryCache();
        clearDiskCache();
    }

    /**
     * 获取缓存大小
     * @return 缓存大小（字节）
     */
    public long getCacheSize() {
        long memorySize = getMemoryCacheSize();
        long diskSize = getDiskCacheSize();
        return memorySize + diskSize;
    }

    /**
     * 获取内存缓存大小
     * @return 内存缓存大小（字节）
     */
    public long getMemoryCacheSize() {
        return mMemoryCache.size();
    }

    /**
     * 获取磁盘缓存大小
     * @return 磁盘缓存大小（字节）
     */
    public long getDiskCacheSize() {
        synchronized (mDiskCacheLock) {
            try {
                return mDiskCache.size();
            } catch (IOException e) {
                return 0;
            }
        }
    }

    /**
     * 检查内存缓存是否包含指定键
     * @param key 缓存键
     * @return 是否包含
     */
    public boolean containsInMemory(String key) {
        return mMemoryCache.get(key) != null;
    }

    /**
     * 检查磁盘缓存是否包含指定键
     * @param key 缓存键
     * @return 是否包含
     */
    public boolean containsInDisk(String key) {
        synchronized (mDiskCacheLock) {
            try {
                return mDiskCache.contains(key);
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * 检查缓存是否包含指定键
     * @param key 缓存键
     * @return 是否包含
     */
    public boolean contains(String key) {
        return containsInMemory(key) || containsInDisk(key);
    }

    /**
     * 获取内存缓存最大大小
     * @return 最大大小（字节）
     */
    public int getMaxMemoryCacheSize() {
        return mMemoryCache.maxSize();
    }

    /**
     * 获取磁盘缓存最大大小
     * @return 最大大小（字节）
     */
    public long getMaxDiskCacheSize() {
        return mDiskCache.getMaxSize();
    }

    /**
     * 设置内存缓存大小
     * @param maxSize 最大大小（字节）
     */
    public void setMaxMemoryCacheSize(int maxSize) {
        mMemoryCache.resize(maxSize);
    }

    /**
     * 修剪内存缓存
     */
    public void trimMemory() {
        mMemoryCache.trimToSize(mMemoryCache.maxSize() / 2);
    }

    /**
     * 关闭缓存
     */
    public void close() {
        synchronized (mDiskCacheLock) {
            try {
                mDiskCache.close();
            } catch (IOException e) {
                // 忽略关闭失败
            }
        }
    }
}
