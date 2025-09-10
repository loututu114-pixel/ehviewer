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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.lib.image.Image;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 第二阶段：增强图片加载器
 * 
 * 设计目标：
 * 1. 多线程并发加载：提高加载并发度
 * 2. 智能预加载：基于用户行为预测性加载
 * 3. 优先级调度：确保重要图片优先加载
 * 4. 自适应重试：网络问题时智能重试
 */
public class EnhancedImageLoader {

    private static final String TAG = "EnhancedImageLoader";
    
    // 线程池配置
    private static final int CORE_POOL_SIZE = 3;
    private static final int MAX_POOL_SIZE = 6;
    private static final int KEEP_ALIVE_TIME = 10; // 秒
    private static final int QUEUE_CAPACITY = 50;
    
    // 预加载配置
    private static final int DEFAULT_PRELOAD_COUNT = 3;
    private static final int MAX_PRELOAD_COUNT = 5;
    
    // 重试配置
    private static final int MAX_RETRY_COUNT = 3;
    private static final long INITIAL_RETRY_DELAY = 1000; // 1秒
    private static final long MAX_RETRY_DELAY = 8000; // 8秒
    
    private final EhGalleryProvider mOriginalProvider;
    private final SmartCacheManager mCacheManager;
    private final Handler mMainHandler;
    private final Handler mBackgroundHandler;
    private final HandlerThread mBackgroundThread;
    
    // 线程池和任务队列
    private final ThreadPoolExecutor mLoadingExecutor;
    private final PriorityBlockingQueue<Runnable> mTaskQueue;
    private final ConcurrentHashMap<Integer, LoadingTask> mActiveTasks = new ConcurrentHashMap<>();
    
    // 预加载控制
    private volatile int mCurrentIndex = -1;
    private volatile int mTotalSize = 0;
    private volatile boolean mPreloadEnabled = true;
    private volatile int mPreloadCount = DEFAULT_PRELOAD_COUNT;
    
    // 统计信息
    private final AtomicLong mLoadRequestCount = new AtomicLong(0);
    private final AtomicLong mLoadSuccessCount = new AtomicLong(0);
    private final AtomicLong mLoadFailureCount = new AtomicLong(0);
    private final AtomicLong mPreloadHitCount = new AtomicLong(0);
    private final AtomicInteger mActiveTaskCount = new AtomicInteger(0);

    public EnhancedImageLoader(@NonNull EhGalleryProvider originalProvider, 
                              @NonNull SmartCacheManager cacheManager) {
        mOriginalProvider = originalProvider;
        mCacheManager = cacheManager;
        mMainHandler = new Handler(Looper.getMainLooper());
        
        // 创建后台线程
        mBackgroundThread = new HandlerThread("EnhancedImageLoader-Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        
        // 创建优先级任务队列
        mTaskQueue = new PriorityBlockingQueue<>(QUEUE_CAPACITY);
        
        // 创建线程池
        mLoadingExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            mTaskQueue,
            r -> {
                Thread thread = new Thread(r, "EnhancedImageLoader-Worker");
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        );
        
        Log.d(TAG, "EnhancedImageLoader initialized with " + CORE_POOL_SIZE + " core threads");
    }

    /**
     * 加载图片（带优先级）
     */
    public void loadImage(int index, int priority, @Nullable LoadingCallback callback) {
        Log.d(TAG, "loadImage() called for index: " + index + ", priority: " + priority);
        
        mLoadRequestCount.incrementAndGet();
        
        // 先检查缓存
        String cacheKey = generateCacheKey(index);
        Image cachedImage = mCacheManager.getImage(cacheKey, priority);
        if (cachedImage != null) {
            Log.d(TAG, "Cache hit for index: " + index);
            if (callback != null) {
                mMainHandler.post(() -> callback.onLoadSuccess(index, cachedImage));
            }
            if (priority >= SmartCacheManager.PRIORITY_HIGH) {
                mPreloadHitCount.incrementAndGet();
            }
            return;
        }
        
        // 创建加载任务
        LoadingTask task = new LoadingTask(index, priority, callback, 0);
        
        // 取消同一索引的旧任务
        LoadingTask oldTask = mActiveTasks.put(index, task);
        if (oldTask != null) {
            oldTask.cancel();
            Log.d(TAG, "Cancelled old task for index: " + index);
        }
        
        // 提交任务
        mLoadingExecutor.execute(task);
        mActiveTaskCount.incrementAndGet();
        
        // 触发智能预加载
        if (priority >= SmartCacheManager.PRIORITY_HIGH) {
            updateCurrentIndex(index);
            triggerSmartPreload();
        }
    }

    /**
     * 强制重新加载图片
     */
    public void forceReload(int index, int priority, @Nullable LoadingCallback callback) {
        Log.d(TAG, "forceReload() called for index: " + index);
        
        // 从缓存中移除
        String cacheKey = generateCacheKey(index);
        mCacheManager.removeImage(cacheKey);
        
        // 重新加载
        loadImage(index, priority, callback);
    }

    /**
     * 取消图片加载
     */
    public void cancelLoad(int index) {
        LoadingTask task = mActiveTasks.remove(index);
        if (task != null) {
            task.cancel();
            Log.d(TAG, "Cancelled loading task for index: " + index);
        }
    }

    /**
     * 更新总图片数量
     */
    public void updateTotalSize(int totalSize) {
        mTotalSize = totalSize;
        Log.d(TAG, "Total size updated to: " + totalSize);
    }

    /**
     * 设置预加载参数
     */
    public void setPreloadConfig(boolean enabled, int count) {
        mPreloadEnabled = enabled;
        mPreloadCount = Math.min(count, MAX_PRELOAD_COUNT);
        Log.d(TAG, "Preload config updated: enabled=" + enabled + ", count=" + count);
    }

    /**
     * 获取加载统计信息
     */
    @NonNull
    public String getLoadingStats() {
        long totalRequests = mLoadRequestCount.get();
        long successRate = totalRequests > 0 ? (mLoadSuccessCount.get() * 100 / totalRequests) : 0;
        
        return String.format(
            "EnhancedImageLoader Stats:\n" +
            "- Total Requests: %d\n" +
            "- Success Rate: %d%% (%d / %d)\n" +
            "- Failures: %d\n" +
            "- Preload Hits: %d\n" +
            "- Active Tasks: %d\n" +
            "- Queue Size: %d\n" +
            "- Thread Pool: %d / %d\n" +
            "- Preload Enabled: %s (count: %d)",
            totalRequests,
            successRate, mLoadSuccessCount.get(), totalRequests,
            mLoadFailureCount.get(),
            mPreloadHitCount.get(),
            mActiveTaskCount.get(),
            mTaskQueue.size(),
            mLoadingExecutor.getActiveCount(), mLoadingExecutor.getPoolSize(),
            mPreloadEnabled, mPreloadCount
        );
    }

    /**
     * 清理资源
     */
    public void destroy() {
        Log.d(TAG, "Destroying EnhancedImageLoader");
        
        // 取消所有任务
        for (LoadingTask task : mActiveTasks.values()) {
            task.cancel();
        }
        mActiveTasks.clear();
        
        // 关闭线程池
        mLoadingExecutor.shutdown();
        try {
            if (!mLoadingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                mLoadingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            mLoadingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 停止后台线程
        mBackgroundThread.quitSafely();
    }

    // ===============================
    // 私有方法
    // ===============================

    private void updateCurrentIndex(int index) {
        mCurrentIndex = index;
    }

    private void triggerSmartPreload() {
        if (!mPreloadEnabled || mCurrentIndex < 0 || mTotalSize <= 0) {
            return;
        }
        
        mBackgroundHandler.post(() -> {
            // 获取预测的索引
            int[] predictedIndices = mCacheManager.predictNextIndices(
                mCurrentIndex, mTotalSize, mPreloadCount);
            
            Log.d(TAG, "Triggering smart preload for " + predictedIndices.length + " indices");
            
            // 预加载预测的图片
            for (int index : predictedIndices) {
                if (index >= 0 && index < mTotalSize) {
                    String cacheKey = generateCacheKey(index);
                    
                    // 检查是否已经在缓存中
                    if (mCacheManager.getImage(cacheKey, SmartCacheManager.PRIORITY_NORMAL) == null) {
                        // 检查是否已经有加载任务
                        if (!mActiveTasks.containsKey(index)) {
                            loadImage(index, SmartCacheManager.PRIORITY_NORMAL, null);
                        }
                    }
                }
            }
        });
    }

    private String generateCacheKey(int index) {
        return mOriginalProvider.getImageFilename(index);
    }

    private void onTaskCompleted(LoadingTask task, boolean success) {
        mActiveTasks.remove(task.index);
        mActiveTaskCount.decrementAndGet();
        
        if (success) {
            mLoadSuccessCount.incrementAndGet();
        } else {
            mLoadFailureCount.incrementAndGet();
        }
    }

    // ===============================
    // 内部类
    // ===============================

    /**
     * 加载回调接口
     */
    public interface LoadingCallback {
        void onLoadSuccess(int index, @NonNull Image image);
        void onLoadFailure(int index, @NonNull String error);
        void onLoadProgress(int index, float progress);
    }

    /**
     * 优先级加载任务
     */
    private class LoadingTask implements Runnable, Comparable<LoadingTask> {
        final int index;
        final int priority;
        final LoadingCallback callback;
        final int retryCount;
        final long createTime;
        volatile boolean cancelled = false;

        LoadingTask(int index, int priority, LoadingCallback callback, int retryCount) {
            this.index = index;
            this.priority = priority;
            this.callback = callback;
            this.retryCount = retryCount;
            this.createTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            if (cancelled) {
                Log.d(TAG, "Task cancelled for index: " + index);
                onTaskCompleted(this, false);
                return;
            }

            try {
                Log.d(TAG, "Loading image for index: " + index + " (attempt " + (retryCount + 1) + ")");
                
                // 使用原始provider的反射调用来加载图片
                java.lang.reflect.Method method = EhGalleryProvider.class.getDeclaredMethod("onRequest", int.class);
                method.setAccessible(true);
                method.invoke(mOriginalProvider, index);
                
                // 注意：实际的图片加载完成会通过SpiderQueen的回调来通知
                // 这里我们只是触发加载请求
                Log.d(TAG, "Load request submitted for index: " + index);
                
                onTaskCompleted(this, true);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to load image for index: " + index, e);
                
                // 重试逻辑
                if (retryCount < MAX_RETRY_COUNT && !cancelled) {
                    scheduleRetry();
                } else {
                    // 最终失败
                    if (callback != null) {
                        mMainHandler.post(() -> callback.onLoadFailure(index, 
                            "Load failed after " + (retryCount + 1) + " attempts: " + e.getMessage()));
                    }
                    onTaskCompleted(this, false);
                }
            }
        }

        private void scheduleRetry() {
            long delay = Math.min(INITIAL_RETRY_DELAY * (1L << retryCount), MAX_RETRY_DELAY);
            Log.d(TAG, "Scheduling retry for index: " + index + " in " + delay + "ms");
            
            mBackgroundHandler.postDelayed(() -> {
                if (!cancelled) {
                    LoadingTask retryTask = new LoadingTask(index, priority, callback, retryCount + 1);
                    mActiveTasks.put(index, retryTask);
                    mLoadingExecutor.execute(retryTask);
                }
            }, delay);
        }

        void cancel() {
            cancelled = true;
        }

        @Override
        public int compareTo(@NonNull LoadingTask other) {
            // 优先级越高，排序越靠前
            int priorityCompare = Integer.compare(other.priority, this.priority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            
            // 优先级相同时，创建时间越早排序越靠前
            return Long.compare(this.createTime, other.createTime);
        }
    }
}