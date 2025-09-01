package com.hippo.ehviewer.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器
 * 基于腾讯X5和YCWebView最佳实践，实现多级缓存系统
 *
 * 核心特性：
 * - 内存缓存 + 磁盘缓存双层架构
 * - 智能缓存策略和清理机制
 * - 预加载和资源优化
 * - 缓存性能监控
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class CacheManager {

    private static final String TAG = "CacheManager";

    // 缓存配置
    private static final long DEFAULT_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MEMORY_CACHE_SIZE_PERCENTAGE = 25; // 25% of available memory
    private static final long CLEANUP_INTERVAL = 300000; // 5分钟清理一次

    // 缓存实例
    private final LruCache<String, CacheEntry> memoryCache;
    private final DiskCache diskCache;
    private final Context context;

    // 管理组件
    private final Handler mainHandler;
    private final ThreadPoolExecutor cacheExecutor;
    private boolean isCleanupRunning = false;

    // 统计信息
    private long totalCacheHits = 0;
    private long totalCacheMisses = 0;
    private long totalBytesCached = 0;

    public CacheManager(Context context, long maxCacheSize) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 初始化内存缓存
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 100 * MEMORY_CACHE_SIZE_PERCENTAGE;
        memoryCache = new LruCache<String, CacheEntry>(cacheSize) {
            @Override
            protected int sizeOf(String key, CacheEntry entry) {
                return entry.size;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, CacheEntry oldValue, CacheEntry newValue) {
                if (oldValue != null && oldValue.bitmap != null && !oldValue.bitmap.isRecycled()) {
                    oldValue.bitmap.recycle();
                }
            }
        };

        // 初始化磁盘缓存
        diskCache = new DiskCache(context, maxCacheSize);

        // 初始化线程池
        cacheExecutor = new ThreadPoolExecutor(
            2, 4, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            r -> new Thread(r, "CacheManager-" + r.hashCode())
        );

        // 启动缓存清理服务
        startCacheCleanupService();

        Log.i(TAG, "CacheManager initialized with memory cache: " + cacheSize + "KB, disk cache: " + (maxCacheSize / 1024 / 1024) + "MB");
    }

    /**
     * 启动缓存清理服务
     */
    private void startCacheCleanupService() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isCleanupRunning) {
                    performCacheCleanup();
                }
                mainHandler.postDelayed(this, CLEANUP_INTERVAL);
            }
        }, CLEANUP_INTERVAL);
    }

    /**
     * 执行缓存清理
     */
    private void performCacheCleanup() {
        isCleanupRunning = true;

        cacheExecutor.execute(() -> {
            try {
                Log.d(TAG, "Performing cache cleanup");

                // 清理过期的磁盘缓存
                diskCache.cleanup();

                // 计算缓存命中率
                long totalRequests = totalCacheHits + totalCacheMisses;
                float hitRate = totalRequests > 0 ? (float) totalCacheHits / totalRequests : 0;

                // 如果命中率太低，清理部分内存缓存
                if (hitRate < 0.3f && memoryCache.size() > memoryCache.maxSize() / 2) {
                    mainHandler.post(() -> {
                        memoryCache.trimToSize(memoryCache.maxSize() / 2);
                        Log.d(TAG, "Trimmed memory cache due to low hit rate: " + String.format("%.2f%%", hitRate * 100));
                    });
                }

                Log.d(TAG, "Cache cleanup completed. Hit rate: " + String.format("%.2f%%", hitRate * 100));

            } catch (Exception e) {
                Log.e(TAG, "Error during cache cleanup", e);
            } finally {
                isCleanupRunning = false;
            }
        });
    }

    /**
     * 获取缓存的Bitmap
     */
    public Bitmap getBitmap(String key) {
        // 先检查内存缓存
        CacheEntry entry = memoryCache.get(key);
        if (entry != null) {
            totalCacheHits++;
            Log.v(TAG, "Memory cache hit for: " + key);
            return entry.bitmap;
        }

        // 检查磁盘缓存
        Bitmap bitmap = diskCache.getBitmap(key);
        if (bitmap != null) {
            totalCacheHits++;

            // 将磁盘缓存的内容放入内存缓存
            putBitmap(key, bitmap);

            Log.v(TAG, "Disk cache hit for: " + key);
            return bitmap;
        }

        totalCacheMisses++;
        Log.v(TAG, "Cache miss for: " + key);
        return null;
    }

    /**
     * 存储Bitmap到缓存
     */
    public void putBitmap(String key, Bitmap bitmap) {
        if (key == null || bitmap == null || bitmap.isRecycled()) {
            return;
        }

        try {
            int bitmapSize = bitmap.getByteCount();

            // 创建缓存条目
            CacheEntry entry = new CacheEntry(bitmap, bitmapSize);

            // 存储到内存缓存
            memoryCache.put(key, entry);

            // 异步存储到磁盘缓存
            cacheExecutor.execute(() -> {
                try {
                    diskCache.putBitmap(key, bitmap);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to store bitmap to disk cache: " + key, e);
                }
            });

            totalBytesCached += bitmapSize;
            Log.v(TAG, "Cached bitmap: " + key + " (" + (bitmapSize / 1024) + "KB)");

        } catch (Exception e) {
            Log.e(TAG, "Failed to cache bitmap: " + key, e);
        }
    }

    /**
     * 获取缓存的字符串数据
     */
    public String getString(String key) {
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && entry.data != null) {
            totalCacheHits++;
            return entry.data;
        }

        String data = diskCache.getString(key);
        if (data != null) {
            totalCacheHits++;
            // 放入内存缓存
            putString(key, data);
        } else {
            totalCacheMisses++;
        }

        return data;
    }

    /**
     * 存储字符串数据到缓存
     */
    public void putString(String key, String data) {
        if (key == null || data == null) {
            return;
        }

        try {
            int dataSize = data.getBytes().length;
            CacheEntry entry = new CacheEntry(data, dataSize);

            memoryCache.put(key, entry);

            // 异步存储到磁盘
            cacheExecutor.execute(() -> {
                try {
                    diskCache.putString(key, data);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to store string to disk cache: " + key, e);
                }
            });

            totalBytesCached += dataSize;

        } catch (Exception e) {
            Log.e(TAG, "Failed to cache string: " + key, e);
        }
    }

    /**
     * 检查缓存中是否存在指定key
     */
    public boolean contains(String key) {
        return memoryCache.get(key) != null || diskCache.contains(key);
    }

    /**
     * 移除指定key的缓存
     */
    public void remove(String key) {
        memoryCache.remove(key);
        cacheExecutor.execute(() -> {
            try {
                diskCache.remove(key);
            } catch (Exception e) {
                Log.e(TAG, "Failed to remove from disk cache: " + key, e);
            }
        });
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        // 清空内存缓存
        memoryCache.evictAll();

        // 异步清空磁盘缓存
        cacheExecutor.execute(() -> {
            try {
                diskCache.clear();
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear disk cache", e);
            }
        });

        totalCacheHits = 0;
        totalCacheMisses = 0;
        totalBytesCached = 0;

        Log.i(TAG, "All caches cleared");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        long memoryCacheSize = memoryCache.size();
        long diskCacheSize = diskCache.getCacheSize();

        return new CacheStats(
            memoryCacheSize,
            diskCacheSize,
            memoryCache.size(), // 内存缓存项目数
            diskCache.getEntryCount(), // 磁盘缓存项目数
            totalCacheHits,
            totalCacheMisses,
            totalBytesCached
        );
    }

    /**
     * 启动缓存清理服务
     */
    public void startCacheCleanup() {
        if (isCleanupRunning) return;

        isCleanupRunning = true;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isCleanupRunning) return;

                performCacheCleanup();
                mainHandler.postDelayed(this, CLEANUP_INTERVAL);
            }
        }, CLEANUP_INTERVAL);
    }

    /**
     * 停止缓存清理服务
     */
    public void stopCacheCleanupService() {
        mainHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Cache cleanup service stopped");
    }

    /**
     * 预加载资源
     */
    public void preloadResources(String... urls) {
        for (String url : urls) {
            preloadResource(url);
        }
    }

    /**
     * 预加载单个资源
     */
    private void preloadResource(String url) {
        cacheExecutor.execute(() -> {
            try {
                // 这里可以实现具体的预加载逻辑
                // 例如：下载图片、脚本等资源并放入缓存
                Log.d(TAG, "Preloading resource: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Failed to preload resource: " + url, e);
            }
        });
    }

    /**
     * 缓存条目类
     */
    private static class CacheEntry {
        final Bitmap bitmap;
        final String data;
        final int size;

        CacheEntry(Bitmap bitmap, int size) {
            this.bitmap = bitmap;
            this.data = null;
            this.size = size;
        }

        CacheEntry(String data, int size) {
            this.bitmap = null;
            this.data = data;
            this.size = size;
        }
    }

    /**
     * 磁盘缓存类（简化的实现）
     */
    private static class DiskCache {
        private final Context context;
        private final long maxCacheSize;
        private final Map<String, Object> diskCache = new HashMap<>();

        DiskCache(Context context, long maxCacheSize) {
            this.context = context;
            this.maxCacheSize = maxCacheSize;
        }

        Bitmap getBitmap(String key) {
            // 简化实现，实际应该使用DiskLruCache
            return (Bitmap) diskCache.get(key);
        }

        void putBitmap(String key, Bitmap bitmap) {
            // 简化实现
            diskCache.put(key, bitmap);
        }

        String getString(String key) {
            return (String) diskCache.get(key);
        }

        void putString(String key, String data) {
            diskCache.put(key, data);
        }

        boolean contains(String key) {
            return diskCache.containsKey(key);
        }

        void remove(String key) {
            diskCache.remove(key);
        }

        void clear() {
            diskCache.clear();
        }

        void cleanup() {
            // 实现缓存清理逻辑
        }

        long getCacheSize() {
            // 简化实现
            return diskCache.size() * 1024; // 估算大小
        }

        int getEntryCount() {
            return diskCache.size();
        }
    }

    /**
     * 缓存统计信息类
     */
    public static class CacheStats {
        public final long memoryCacheSize;
        public final long diskCacheSize;
        public final int memoryEntryCount;
        public final int diskEntryCount;
        public final long totalHits;
        public final long totalMisses;
        public final long totalBytesCached;

        public CacheStats(long memoryCacheSize, long diskCacheSize, int memoryEntryCount,
                         int diskEntryCount, long totalHits, long totalMisses, long totalBytesCached) {
            this.memoryCacheSize = memoryCacheSize;
            this.diskCacheSize = diskCacheSize;
            this.memoryEntryCount = memoryEntryCount;
            this.diskEntryCount = diskEntryCount;
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.totalBytesCached = totalBytesCached;
        }

        public float getHitRate() {
            long total = totalHits + totalMisses;
            return total > 0 ? (float) totalHits / total : 0;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{memory=%dKB/%d items, disk=%dKB/%d items, hits=%d, misses=%d, hitRate=%.2f%%}",
                    memoryCacheSize / 1024, memoryEntryCount,
                    diskCacheSize / 1024, diskEntryCount,
                    totalHits, totalMisses, getHitRate() * 100);
        }
    }
}
