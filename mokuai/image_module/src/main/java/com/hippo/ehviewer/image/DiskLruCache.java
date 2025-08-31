/*
 * EhViewer Image Module - DiskLruCache
 * 磁盘缓存实现 - 简单的磁盘缓存功能
 */

package com.hippo.ehviewer.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简单的磁盘缓存实现
 * 支持LRU淘汰策略
 */
public class DiskLruCache {

    private static final String TAG = DiskLruCache.class.getSimpleName();
    private static final int DEFAULT_MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String CACHE_SUFFIX = ".cache";

    private final File mCacheDir;
    private final long mMaxSize;
    private long mCurrentSize;
    private final Map<String, CacheEntry> mCacheMap;

    public DiskLruCache(File cacheDir, long maxSize) throws IOException {
        mCacheDir = cacheDir;
        mMaxSize = maxSize;
        mCacheMap = new LinkedHashMap<>(16, 0.75f, true);
        initialize();
    }

    public DiskLruCache(File cacheDir) throws IOException {
        this(cacheDir, DEFAULT_MAX_SIZE);
    }

    /**
     * 初始化缓存
     */
    private void initialize() throws IOException {
        if (!mCacheDir.exists()) {
            if (!mCacheDir.mkdirs()) {
                throw new IOException("Failed to create cache directory");
            }
        }

        // 加载现有缓存文件
        File[] files = mCacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(CACHE_SUFFIX)) {
                    String key = file.getName().substring(0, file.getName().length() - CACHE_SUFFIX.length());
                    long lastModified = file.lastModified();
                    long fileSize = file.length();
                    mCacheMap.put(key, new CacheEntry(file, lastModified, fileSize));
                    mCurrentSize += fileSize;
                }
            }
        }
    }

    /**
     * 获取缓存
     */
    public Bitmap get(String key) throws IOException {
        CacheEntry entry = mCacheMap.get(key);
        if (entry == null) {
            return null;
        }

        // 更新访问时间
        entry.lastAccessTime = System.currentTimeMillis();
        mCacheMap.put(key, entry);

        // 从文件加载Bitmap
        try (FileInputStream fis = new FileInputStream(entry.file)) {
            return BitmapFactory.decodeStream(fis);
        }
    }

    /**
     * 放入缓存
     */
    public void put(String key, Bitmap bitmap) throws IOException {
        String fileName = generateFileName(key);
        File cacheFile = new File(mCacheDir, fileName);

        // 压缩并保存Bitmap
        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }

        long fileSize = cacheFile.length();

        // 检查是否超过缓存大小限制
        mCurrentSize += fileSize;
        if (mCurrentSize > mMaxSize) {
            trimToSize();
        }

        // 更新缓存映射
        CacheEntry entry = new CacheEntry(cacheFile, System.currentTimeMillis(), fileSize);
        mCacheMap.put(key, entry);
    }

    /**
     * 移除缓存
     */
    public void remove(String key) throws IOException {
        CacheEntry entry = mCacheMap.remove(key);
        if (entry != null) {
            if (entry.file.delete()) {
                mCurrentSize -= entry.fileSize;
            }
        }
    }

    /**
     * 检查是否包含指定键
     */
    public boolean contains(String key) {
        return mCacheMap.containsKey(key);
    }

    /**
     * 清空缓存
     */
    public void clear() throws IOException {
        for (CacheEntry entry : mCacheMap.values()) {
            entry.file.delete();
        }
        mCacheMap.clear();
        mCurrentSize = 0;
    }

    /**
     * 获取缓存大小
     */
    public long size() {
        return mCurrentSize;
    }

    /**
     * 获取最大缓存大小
     */
    public long getMaxSize() {
        return mMaxSize;
    }

    /**
     * 关闭缓存
     */
    public void close() throws IOException {
        // 清理资源
        clear();
    }

    /**
     * 修剪缓存到指定大小
     */
    private void trimToSize() throws IOException {
        // 简单的LRU淘汰策略
        while (mCurrentSize > mMaxSize && !mCacheMap.isEmpty()) {
            // 移除最少使用的项
            Map.Entry<String, CacheEntry> eldest = mCacheMap.entrySet().iterator().next();
            CacheEntry entry = eldest.getValue();

            if (entry.file.delete()) {
                mCurrentSize -= entry.fileSize;
                mCacheMap.remove(eldest.getKey());
            } else {
                // 如果删除失败，移除映射
                mCacheMap.remove(eldest.getKey());
            }
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString() + CACHE_SUFFIX;
        } catch (NoSuchAlgorithmException e) {
            // fallback to simple hash
            return String.valueOf(key.hashCode()) + CACHE_SUFFIX;
        }
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final File file;
        long lastAccessTime;
        final long fileSize;

        CacheEntry(File file, long lastAccessTime, long fileSize) {
            this.file = file;
            this.lastAccessTime = lastAccessTime;
            this.fileSize = fileSize;
        }
    }
}
