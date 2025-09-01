/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.client;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存管理器
 * 负责监控和优化应用内存使用
 */
public class MemoryManager implements ComponentCallbacks2 {

    private static final String TAG = "MemoryManager";

    // 内存阈值
    private static final float LOW_MEMORY_THRESHOLD = 0.1f; // 10%
    private static final float CRITICAL_MEMORY_THRESHOLD = 0.05f; // 5%
    private static final long MEMORY_CHECK_INTERVAL = 30 * 1000; // 30秒

    private static MemoryManager sInstance;

    private Context mContext;
    private ActivityManager mActivityManager;
    private Handler mHandler;

    // 内存信息
    private ActivityManager.MemoryInfo mMemoryInfo;
    private long mTotalMemory;
    private long mAvailableMemory;
    private long mLowMemoryThreshold;
    private long mLastMemoryCheckTime;

    // 内存监听器
    private final List<MemoryListener> mMemoryListeners = new CopyOnWriteArrayList<>();

    // 内存优化任务
    private final Runnable mMemoryOptimizationTask = this::performMemoryOptimization;
    private final Runnable mMemoryCheckTask = this::checkMemoryStatus;

    /**
     * 内存监听器接口
     */
    public interface MemoryListener {
        void onLowMemory();
        void onMemoryOptimized();
        void onMemoryCritical();
    }

    /**
     * 获取单例实例
     */
    public static synchronized MemoryManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new MemoryManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private MemoryManager(Context context) {
        this.mContext = context;
        this.mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mMemoryInfo = new ActivityManager.MemoryInfo();

        initializeMemoryInfo();
        registerComponentCallbacks();
        startMemoryMonitoring();
    }

    /**
     * 初始化内存信息
     */
    private void initializeMemoryInfo() {
        try {
            mActivityManager.getMemoryInfo(mMemoryInfo);
            mTotalMemory = mMemoryInfo.totalMem;
            mAvailableMemory = mMemoryInfo.availMem;
            mLowMemoryThreshold = mMemoryInfo.threshold;

            Log.d(TAG, "Memory initialized - Total: " + formatMemorySize(mTotalMemory) +
                      ", Available: " + formatMemorySize(mAvailableMemory) +
                      ", Threshold: " + formatMemorySize(mLowMemoryThreshold));
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize memory info", e);
        }
    }

    /**
     * 注册组件回调
     */
    private void registerComponentCallbacks() {
        try {
            mContext.registerComponentCallbacks(this);
            Log.d(TAG, "Memory manager registered as component callback");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register component callbacks", e);
        }
    }

    /**
     * 开始内存监控
     */
    private void startMemoryMonitoring() {
        mHandler.postDelayed(mMemoryCheckTask, MEMORY_CHECK_INTERVAL);
        Log.d(TAG, "Memory monitoring started");
    }

    /**
     * 检查内存状态
     */
    private void checkMemoryStatus() {
        try {
            long currentTime = System.currentTimeMillis();

            // 更新内存信息
            mActivityManager.getMemoryInfo(mMemoryInfo);
            long newAvailableMemory = mMemoryInfo.availMem;

            // 计算内存使用率
            float memoryUsageRatio = 1.0f - (float) newAvailableMemory / mTotalMemory;

            // 检查内存状态
            if (memoryUsageRatio >= (1.0f - CRITICAL_MEMORY_THRESHOLD)) {
                // 内存严重不足
                Log.w(TAG, "Critical memory usage detected: " + String.format("%.1f%%", memoryUsageRatio * 100));
                notifyMemoryCritical();
                performUrgentMemoryOptimization();
            } else if (memoryUsageRatio >= (1.0f - LOW_MEMORY_THRESHOLD)) {
                // 内存不足
                Log.w(TAG, "Low memory usage detected: " + String.format("%.1f%%", memoryUsageRatio * 100));
                notifyLowMemory();
                performMemoryOptimization();
            }

            // 更新内存信息
            mAvailableMemory = newAvailableMemory;
            mLastMemoryCheckTime = currentTime;

            // 安排下次检查
            mHandler.postDelayed(mMemoryCheckTask, MEMORY_CHECK_INTERVAL);

        } catch (Exception e) {
            Log.e(TAG, "Failed to check memory status", e);
            // 即使出错也要继续监控
            mHandler.postDelayed(mMemoryCheckTask, MEMORY_CHECK_INTERVAL);
        }
    }

    /**
     * 执行内存优化
     */
    private void performMemoryOptimization() {
        Log.d(TAG, "Performing memory optimization");

        try {
            // 1. 清理图片缓存
            if (ImageLazyLoader.getInstance() != null) {
                ImageLazyLoader.getInstance().clearCache();
                Log.d(TAG, "Image cache cleared");
            }

            // 2. 清理WebView缓存
            if (WebViewCacheManager.getInstance(mContext) != null) {
                WebViewCacheManager.getInstance(mContext).cleanupExpiredCache();
                Log.d(TAG, "WebView cache cleaned");
            }

            // 3. 清理WebView池
            // 注意：WebViewPoolManager.getInstance方法不存在，这里先注释掉
            // TODO: 实现WebView池管理器的内存清理
            Log.d(TAG, "WebView pool optimization skipped - manager not available");

            // 4. 强制垃圾回收
            System.gc();
            System.runFinalization();
            Log.d(TAG, "Garbage collection triggered");

            // 通知监听器
            notifyMemoryOptimized();

        } catch (Exception e) {
            Log.e(TAG, "Failed to perform memory optimization", e);
        }
    }

    /**
     * 执行紧急内存优化
     */
    private void performUrgentMemoryOptimization() {
        Log.w(TAG, "Performing urgent memory optimization");

        try {
            // 1. 强制清理所有缓存
            clearAllCaches();

            // 2. 强制垃圾回收（多次）
            for (int i = 0; i < 3; i++) {
                System.gc();
                System.runFinalization();
                try {
                    Thread.sleep(100); // 短暂等待
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 3. 清理系统内存（如果可用）
            trimMemory();

            Log.w(TAG, "Urgent memory optimization completed");

        } catch (Exception e) {
            Log.e(TAG, "Failed to perform urgent memory optimization", e);
        }
    }

    /**
     * 清理所有缓存
     */
    private void clearAllCaches() {
        try {
            // 清理图片缓存
            if (ImageLazyLoader.getInstance() != null) {
                ImageLazyLoader.getInstance().clearCache();
            }

            // 清理WebView缓存
            if (WebViewCacheManager.getInstance(mContext) != null) {
                WebViewCacheManager.getInstance(mContext).clearAllCache();
            }

            Log.d(TAG, "All caches cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear all caches", e);
        }
    }

    /**
     * 内存整理
     */
    private void trimMemory() {
        try {
            // 调用系统的内存整理
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                mContext.getSystemService(ActivityManager.class).clearApplicationUserData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to trim memory", e);
        }
    }

    /**
     * 添加内存监听器
     */
    public void addMemoryListener(MemoryListener listener) {
        if (listener != null && !mMemoryListeners.contains(listener)) {
            mMemoryListeners.add(listener);
        }
    }

    /**
     * 移除内存监听器
     */
    public void removeMemoryListener(MemoryListener listener) {
        mMemoryListeners.remove(listener);
    }

    /**
     * 通知内存不足
     */
    private void notifyLowMemory() {
        for (MemoryListener listener : mMemoryListeners) {
            try {
                listener.onLowMemory();
            } catch (Exception e) {
                Log.e(TAG, "Failed to notify low memory listener", e);
            }
        }
    }

    /**
     * 通知内存优化完成
     */
    private void notifyMemoryOptimized() {
        for (MemoryListener listener : mMemoryListeners) {
            try {
                listener.onMemoryOptimized();
            } catch (Exception e) {
                Log.e(TAG, "Failed to notify memory optimized listener", e);
            }
        }
    }

    /**
     * 通知内存严重不足
     */
    private void notifyMemoryCritical() {
        for (MemoryListener listener : mMemoryListeners) {
            try {
                listener.onMemoryCritical();
            } catch (Exception e) {
                Log.e(TAG, "Failed to notify memory critical listener", e);
            }
        }
    }

    /**
     * 获取内存使用信息
     */
    public MemoryUsageInfo getMemoryUsageInfo() {
        MemoryUsageInfo info = new MemoryUsageInfo();

        try {
            mActivityManager.getMemoryInfo(mMemoryInfo);

            info.totalMemory = mMemoryInfo.totalMem;
            info.availableMemory = mMemoryInfo.availMem;
            info.usedMemory = info.totalMemory - info.availableMemory;
            info.usageRatio = (float) info.usedMemory / info.totalMemory;
            info.lowMemory = mMemoryInfo.lowMemory;
            info.threshold = mMemoryInfo.threshold;

        } catch (Exception e) {
            Log.e(TAG, "Failed to get memory usage info", e);
        }

        return info;
    }

    /**
     * 格式化内存大小
     */
    public static String formatMemorySize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * ComponentCallbacks2实现
     */
    @Override
    public void onTrimMemory(int level) {
        Log.d(TAG, "onTrimMemory called with level: " + level);

        switch (level) {
            case TRIM_MEMORY_RUNNING_MODERATE:
            case TRIM_MEMORY_RUNNING_LOW:
                // 应用仍在运行，但系统内存不足
                performMemoryOptimization();
                break;

            case TRIM_MEMORY_RUNNING_CRITICAL:
                // 应用仍在运行，但系统内存严重不足
                performUrgentMemoryOptimization();
                break;

            case TRIM_MEMORY_UI_HIDDEN:
                // 应用界面被隐藏，清理UI相关资源
                clearAllCaches();
                break;

            case TRIM_MEMORY_BACKGROUND:
            case TRIM_MEMORY_MODERATE:
            case TRIM_MEMORY_COMPLETE:
                // 应用在后台，执行全面的内存清理
                clearAllCaches();
                System.gc();
                break;
        }
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "onLowMemory called");
        performUrgentMemoryOptimization();
        notifyLowMemory();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // 配置变化，通常不需要特殊处理
        Log.d(TAG, "Configuration changed");
    }

    /**
     * 销毁内存管理器
     */
    public void destroy() {
        try {
            mHandler.removeCallbacks(mMemoryCheckTask);
            mHandler.removeCallbacks(mMemoryOptimizationTask);
            mMemoryListeners.clear();

            // 注销组件回调
            mContext.unregisterComponentCallbacks(this);

            Log.d(TAG, "Memory manager destroyed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to destroy memory manager", e);
        }
    }

    /**
     * 内存使用信息
     */
    public static class MemoryUsageInfo {
        public long totalMemory = 0;
        public long availableMemory = 0;
        public long usedMemory = 0;
        public float usageRatio = 0.0f;
        public boolean lowMemory = false;
        public long threshold = 0;

        public String getUsagePercentage() {
            return String.format("%.1f%%", usageRatio * 100);
        }

        public String getTotalMemoryString() {
            return formatMemorySize(totalMemory);
        }

        public String getAvailableMemoryString() {
            return formatMemorySize(availableMemory);
        }

        public String getUsedMemoryString() {
            return formatMemorySize(usedMemory);
        }

        public boolean isLowMemory() {
            return lowMemory || usageRatio > 0.8f; // 80%作为低内存阈值
        }

        public boolean isCriticalMemory() {
            return usageRatio > 0.9f; // 90%作为严重内存不足阈值
        }
    }
}
