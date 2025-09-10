/*
 * Copyright 2025 EhViewer Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.gallery.enhanced;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.lib.image.Image;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 第二阶段：智能缓存管理器
 * 
 * 设计目标：
 * 1. 多级缓存：内存缓存 + 优先级缓存
 * 2. 内存感知：根据系统内存压力自动调整
 * 3. 智能清理：基于LRU + 访问频率 + 图片重要性
 * 4. 预测性加载：基于用户浏览模式预测
 */
public class SmartCacheManager implements ComponentCallbacks2 {

    private static final String TAG = "SmartCacheManager";
    
    // 缓存配置
    private static final int DEFAULT_MEMORY_CACHE_SIZE = 32 * 1024 * 1024; // 32MB
    private static final int HIGH_PRIORITY_CACHE_SIZE = 8 * 1024 * 1024;   // 8MB
    private static final int MAX_CACHE_ENTRIES = 200;
    
    // 优先级定义
    public static final int PRIORITY_CRITICAL = 3;  // 当前查看的图片
    public static final int PRIORITY_HIGH = 2;      // 相邻图片
    public static final int PRIORITY_NORMAL = 1;    // 预加载图片
    public static final int PRIORITY_LOW = 0;       // 其他图片
    
    private final Context mContext;
    
    // 多级缓存
    private final LruCache<String, CacheEntry> mMemoryCache;
    private final LruCache<String, CacheEntry> mHighPriorityCache;
    
    // 访问统计
    private final ConcurrentHashMap<String, AccessInfo> mAccessStats = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong mHitCount = new AtomicLong(0);
    private final AtomicLong mMissCount = new AtomicLong(0);
    private final AtomicLong mEvictCount = new AtomicLong(0);
    private final AtomicInteger mCurrentMemoryUsage = new AtomicInteger(0);
    
    // 内存监控
    private volatile int mMemoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE;
    private volatile boolean mLowMemoryMode = false;
    
    public SmartCacheManager(@NonNull Context context) {
        mContext = context.getApplicationContext();
        
        // 根据设备内存计算缓存大小
        int memoryCacheSize = calculateOptimalCacheSize();
        int highPriorityCacheSize = memoryCacheSize / 4;
        
        // 创建内存缓存
        mMemoryCache = new LruCache<String, CacheEntry>(memoryCacheSize) {
            @Override
            protected int sizeOf(String key, CacheEntry entry) {
                return entry.getMemorySize();
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, CacheEntry oldValue, CacheEntry newValue) {
                if (evicted) {
                    mEvictCount.incrementAndGet();
                    mCurrentMemoryUsage.addAndGet(-oldValue.getMemorySize());
                    Log.d(TAG, "Cache entry evicted: " + key + ", size: " + oldValue.getMemorySize());
                }
            }
        };
        
        // 创建高优先级缓存
        mHighPriorityCache = new LruCache<String, CacheEntry>(highPriorityCacheSize) {
            @Override
            protected int sizeOf(String key, CacheEntry entry) {
                return entry.getMemorySize();
            }
        };
        
        Log.d(TAG, "SmartCacheManager initialized with memory cache: " + memoryCacheSize + 
              ", high priority cache: " + highPriorityCacheSize);
    }

    /**
     * 获取图片缓存
     */
    @Nullable
    public Image getImage(@NonNull String key, int priority) {
        // 先检查高优先级缓存
        CacheEntry entry = mHighPriorityCache.get(key);
        if (entry != null) {
            mHitCount.incrementAndGet();
            updateAccessInfo(key, priority, true);
            Log.d(TAG, "High priority cache hit: " + key);
            return entry.image;
        }
        
        // 检查普通内存缓存
        entry = mMemoryCache.get(key);
        if (entry != null) {
            mHitCount.incrementAndGet();
            updateAccessInfo(key, priority, true);
            
            // 高优先级图片提升到高优先级缓存
            if (priority >= PRIORITY_HIGH) {
                promoteToHighPriority(key, entry);
            }
            
            Log.d(TAG, "Memory cache hit: " + key);
            return entry.image;
        }
        
        mMissCount.incrementAndGet();
        updateAccessInfo(key, priority, false);
        Log.d(TAG, "Cache miss: " + key);
        return null;
    }

    /**
     * 存储图片到缓存
     */
    public void putImage(@NonNull String key, @NonNull Image image, int priority) {
        if (image == null) {
            return;
        }
        
        int imageSize = estimateImageSize(image);
        CacheEntry entry = new CacheEntry(image, priority, System.currentTimeMillis(), imageSize);
        
        // 检查内存压力
        if (mLowMemoryMode && priority < PRIORITY_HIGH) {
            Log.d(TAG, "Low memory mode, skipping cache for: " + key);
            return;
        }
        
        // 高优先级图片直接进入高优先级缓存
        if (priority >= PRIORITY_HIGH) {
            mHighPriorityCache.put(key, entry);
            Log.d(TAG, "Added to high priority cache: " + key + ", size: " + imageSize);
        } else {
            mMemoryCache.put(key, entry);
            Log.d(TAG, "Added to memory cache: " + key + ", size: " + imageSize);
        }
        
        mCurrentMemoryUsage.addAndGet(imageSize);
        updateAccessInfo(key, priority, false);
    }

    /**
     * 移除缓存项
     */
    public void removeImage(@NonNull String key) {
        CacheEntry entry = mHighPriorityCache.remove(key);
        if (entry != null) {
            mCurrentMemoryUsage.addAndGet(-entry.getMemorySize());
        }
        
        entry = mMemoryCache.remove(key);
        if (entry != null) {
            mCurrentMemoryUsage.addAndGet(-entry.getMemorySize());
        }
        
        mAccessStats.remove(key);
        Log.d(TAG, "Removed from cache: " + key);
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        mHighPriorityCache.evictAll();
        mMemoryCache.evictAll();
        mAccessStats.clear();
        mCurrentMemoryUsage.set(0);
        Log.d(TAG, "Cache cleared");
    }

    /**
     * 智能清理：基于内存压力自动清理
     */
    public void smartCleanup(int trimLevel) {
        Log.d(TAG, "Smart cleanup triggered, trim level: " + trimLevel);
        
        switch (trimLevel) {
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                // 严重内存不足：清理所有低优先级缓存
                clearLowPriorityCache();
                mLowMemoryMode = true;
                break;
                
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                // 内存不足：清理部分缓存
                reduceCacheSize(0.6f);
                mLowMemoryMode = true;
                break;
                
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                // 中等内存压力：清理老的缓存项
                reduceCacheSize(0.8f);
                mLowMemoryMode = false;
                break;
                
            default:
                mLowMemoryMode = false;
                break;
        }
        
        mMemoryPressureLevel = trimLevel;
    }

    /**
     * 获取缓存统计信息
     */
    @NonNull
    public String getCacheStats() {
        long totalAccess = mHitCount.get() + mMissCount.get();
        double hitRate = totalAccess > 0 ? (double) mHitCount.get() / totalAccess * 100 : 0;
        
        return String.format(
            "SmartCacheManager Stats:\n" +
            "- Memory Cache Size: %d / %d entries\n" +
            "- High Priority Cache Size: %d entries\n" +
            "- Memory Usage: %d bytes\n" +
            "- Hit Rate: %.1f%% (%d / %d)\n" +
            "- Evictions: %d\n" +
            "- Low Memory Mode: %s\n" +
            "- Memory Pressure: %d",
            mMemoryCache.size(), MAX_CACHE_ENTRIES,
            mHighPriorityCache.size(),
            mCurrentMemoryUsage.get(),
            hitRate, mHitCount.get(), totalAccess,
            mEvictCount.get(),
            mLowMemoryMode,
            mMemoryPressureLevel
        );
    }

    /**
     * 预测下一个需要的图片索引
     */
    public int[] predictNextIndices(int currentIndex, int totalSize, int maxPredictions) {
        // 简单的预测算法：当前图片的前后相邻图片
        int[] predictions = new int[Math.min(maxPredictions, 6)];
        int count = 0;
        
        // 优先预测相邻图片
        if (currentIndex + 1 < totalSize && count < predictions.length) {
            predictions[count++] = currentIndex + 1;
        }
        if (currentIndex - 1 >= 0 && count < predictions.length) {
            predictions[count++] = currentIndex - 1;
        }
        
        // 扩展预测范围
        for (int offset = 2; offset <= 3 && count < predictions.length; offset++) {
            if (currentIndex + offset < totalSize && count < predictions.length) {
                predictions[count++] = currentIndex + offset;
            }
            if (currentIndex - offset >= 0 && count < predictions.length) {
                predictions[count++] = currentIndex - offset;
            }
        }
        
        // 返回实际预测的数量
        int[] result = new int[count];
        System.arraycopy(predictions, 0, result, 0, count);
        return result;
    }

    // ===============================
    // ComponentCallbacks2 接口实现
    // ===============================

    @Override
    public void onTrimMemory(int level) {
        Log.d(TAG, "onTrimMemory called with level: " + level);
        smartCleanup(level);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // 配置变化时不需要特殊处理
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "onLowMemory called");
        smartCleanup(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);
    }

    // ===============================
    // 私有辅助方法
    // ===============================

    private int calculateOptimalCacheSize() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            int memoryClass = am.getMemoryClass();
            // 使用1/8的可用内存作为缓存
            return Math.max(DEFAULT_MEMORY_CACHE_SIZE, memoryClass * 1024 * 1024 / 8);
        }
        return DEFAULT_MEMORY_CACHE_SIZE;
    }

    private int estimateImageSize(@NonNull Image image) {
        if (image.getWidth() > 0 && image.getHeight() > 0) {
            // 估算: width * height * 4 (ARGB_8888)
            return image.getWidth() * image.getHeight() * 4;
        }
        return 1024 * 1024; // 默认1MB
    }

    private void promoteToHighPriority(@NonNull String key, @NonNull CacheEntry entry) {
        if (entry.priority < PRIORITY_HIGH) {
            CacheEntry promoted = new CacheEntry(entry.image, PRIORITY_HIGH, 
                                               System.currentTimeMillis(), entry.getMemorySize());
            mHighPriorityCache.put(key, promoted);
            Log.d(TAG, "Promoted to high priority: " + key);
        }
    }

    private void updateAccessInfo(@NonNull String key, int priority, boolean hit) {
        AccessInfo info = mAccessStats.get(key);
        if (info == null) {
            info = new AccessInfo();
            mAccessStats.put(key, info);
        }
        info.accessCount++;
        info.lastAccessTime = System.currentTimeMillis();
        info.lastPriority = priority;
        if (hit) {
            info.hitCount++;
        }
    }

    private void clearLowPriorityCache() {
        // 清理普通缓存中的低优先级项目
        for (String key : mMemoryCache.snapshot().keySet()) {
            CacheEntry entry = mMemoryCache.get(key);
            if (entry != null && entry.priority <= PRIORITY_NORMAL) {
                mMemoryCache.remove(key);
            }
        }
        Log.d(TAG, "Low priority cache cleared");
    }

    private void reduceCacheSize(float factor) {
        int targetSize = (int) (mMemoryCache.size() * factor);
        while (mMemoryCache.size() > targetSize) {
            // LruCache会自动移除最老的项目
            mMemoryCache.trimToSize(targetSize);
        }
        Log.d(TAG, "Cache size reduced to: " + mMemoryCache.size());
    }

    // ===============================
    // 内部数据类
    // ===============================

    private static class CacheEntry {
        final Image image;
        final int priority;
        final long createTime;
        final int memorySize;

        CacheEntry(Image image, int priority, long createTime, int memorySize) {
            this.image = image;
            this.priority = priority;
            this.createTime = createTime;
            this.memorySize = memorySize;
        }

        int getMemorySize() {
            return memorySize;
        }
    }

    private static class AccessInfo {
        int accessCount = 0;
        int hitCount = 0;
        long lastAccessTime = 0;
        int lastPriority = PRIORITY_NORMAL;
    }
}