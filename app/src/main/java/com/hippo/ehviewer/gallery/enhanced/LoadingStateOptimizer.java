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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.lib.image.Image;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 第二阶段：加载状态优化器
 * 
 * 设计目标：
 * 1. 平滑加载动画：消除加载状态跳跃
 * 2. 智能重试机制：网络错误时自动重试
 * 3. 用户友好反馈：清晰的状态提示
 * 4. 性能监控：加载时间和成功率统计
 */
public class LoadingStateOptimizer {

    private static final String TAG = "LoadingStateOptimizer";
    
    // 动画配置
    private static final int FADE_ANIMATION_DURATION = 300;
    private static final int PROGRESS_ANIMATION_DURATION = 200;
    private static final int ERROR_ANIMATION_DURATION = 500;
    
    // 重试配置
    private static final int MAX_AUTO_RETRY_COUNT = 3;
    private static final long INITIAL_RETRY_DELAY = 2000; // 2秒
    private static final long MAX_RETRY_DELAY = 10000; // 10秒
    
    // 超时配置
    private static final long LOADING_TIMEOUT = 30000; // 30秒
    private static final long PROGRESS_UPDATE_INTERVAL = 100; // 100毫秒
    
    private final Handler mMainHandler;
    
    // 状态管理
    private final ConcurrentHashMap<Integer, LoadingState> mLoadingStates = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong mTotalLoadingTime = new AtomicLong(0);
    private final AtomicInteger mSuccessfulLoads = new AtomicInteger(0);
    private final AtomicInteger mFailedLoads = new AtomicInteger(0);
    private final AtomicInteger mAutoRetries = new AtomicInteger(0);
    private final AtomicInteger mTimeouts = new AtomicInteger(0);
    
    // 回调接口
    private StateChangeListener mStateChangeListener;
    
    public LoadingStateOptimizer() {
        mMainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "LoadingStateOptimizer initialized");
    }

    /**
     * 开始加载状态
     */
    public void startLoading(int index, @Nullable String url) {
        Log.d(TAG, "Starting loading for index: " + index);
        
        LoadingState state = new LoadingState(index, url);
        mLoadingStates.put(index, state);
        
        // 通知状态变化
        notifyStateChange(index, StateType.LOADING_START, 0.0f, null, null);
        
        // 启动超时检查
        scheduleTimeoutCheck(index);
        
        // 启动进度动画
        startProgressAnimation(state);
    }

    /**
     * 更新加载进度
     */
    public void updateProgress(int index, float progress) {
        LoadingState state = mLoadingStates.get(index);
        if (state == null || state.isCompleted()) {
            return;
        }
        
        // 平滑进度更新
        smoothProgressUpdate(state, progress);
        
        Log.v(TAG, "Progress updated for index: " + index + ", progress: " + progress);
    }

    /**
     * 加载成功
     */
    public void onLoadSuccess(int index, @NonNull Image image) {
        LoadingState state = mLoadingStates.get(index);
        if (state == null) {
            return;
        }
        
        Log.d(TAG, "Load success for index: " + index);
        
        // 更新统计
        long loadingTime = System.currentTimeMillis() - state.startTime;
        mTotalLoadingTime.addAndGet(loadingTime);
        mSuccessfulLoads.incrementAndGet();
        
        // 完成进度动画
        completeProgressAnimation(state);
        
        // 标记为完成
        state.markCompleted(true);
        
        // 通知成功
        mMainHandler.post(() -> {
            notifyStateChange(index, StateType.LOADING_SUCCESS, 1.0f, image, null);
            // 延迟清理状态
            mMainHandler.postDelayed(() -> mLoadingStates.remove(index), FADE_ANIMATION_DURATION);
        });
    }

    /**
     * 加载失败
     */
    public void onLoadFailure(int index, @NonNull String error) {
        LoadingState state = mLoadingStates.get(index);
        if (state == null) {
            return;
        }
        
        Log.w(TAG, "Load failure for index: " + index + ", error: " + error);
        
        // 检查是否需要自动重试
        if (shouldAutoRetry(state, error)) {
            scheduleAutoRetry(state, error);
            return;
        }
        
        // 更新统计
        mFailedLoads.incrementAndGet();
        
        // 标记为失败
        state.markCompleted(false);
        
        // 通知失败
        notifyStateChange(index, StateType.LOADING_ERROR, state.currentProgress, null, error);
        
        // 错误动画
        startErrorAnimation(state);
        
        // 延迟清理状态
        mMainHandler.postDelayed(() -> mLoadingStates.remove(index), ERROR_ANIMATION_DURATION);
    }

    /**
     * 取消加载
     */
    public void cancelLoading(int index) {
        LoadingState state = mLoadingStates.remove(index);
        if (state != null) {
            Log.d(TAG, "Loading cancelled for index: " + index);
            state.cancel();
            notifyStateChange(index, StateType.LOADING_CANCELLED, state.currentProgress, null, null);
        }
    }

    /**
     * 手动重试加载
     */
    public void retryLoading(int index) {
        LoadingState state = mLoadingStates.get(index);
        if (state == null) {
            return;
        }
        
        Log.d(TAG, "Manual retry for index: " + index);
        
        // 重置状态
        state.resetForRetry();
        
        // 重新开始加载
        notifyStateChange(index, StateType.LOADING_RETRY, 0.0f, null, null);
        startProgressAnimation(state);
    }

    /**
     * 获取加载状态
     */
    @Nullable
    public LoadingState getLoadingState(int index) {
        return mLoadingStates.get(index);
    }

    /**
     * 设置状态变化监听器
     */
    public void setStateChangeListener(@Nullable StateChangeListener listener) {
        mStateChangeListener = listener;
    }

    /**
     * 获取性能统计
     */
    @NonNull
    public String getPerformanceStats() {
        int totalAttempts = mSuccessfulLoads.get() + mFailedLoads.get();
        if (totalAttempts == 0) {
            return "LoadingStateOptimizer: No loading attempts yet";
        }
        
        long avgLoadingTime = totalAttempts > 0 ? mTotalLoadingTime.get() / totalAttempts : 0;
        float successRate = (float) mSuccessfulLoads.get() / totalAttempts * 100;
        
        return String.format(
            "LoadingStateOptimizer Stats:\n" +
            "- Total Attempts: %d\n" +
            "- Success Rate: %.1f%% (%d successful)\n" +
            "- Failed Loads: %d\n" +
            "- Auto Retries: %d\n" +
            "- Timeouts: %d\n" +
            "- Average Loading Time: %dms\n" +
            "- Active States: %d",
            totalAttempts,
            successRate, mSuccessfulLoads.get(),
            mFailedLoads.get(),
            mAutoRetries.get(),
            mTimeouts.get(),
            avgLoadingTime,
            mLoadingStates.size()
        );
    }

    /**
     * 清理所有状态
     */
    public void clearAllStates() {
        Log.d(TAG, "Clearing all loading states");
        for (LoadingState state : mLoadingStates.values()) {
            state.cancel();
        }
        mLoadingStates.clear();
    }

    // ===============================
    // 私有方法
    // ===============================

    private void notifyStateChange(int index, StateType type, float progress, 
                                 @Nullable Image image, @Nullable String error) {
        if (mStateChangeListener != null) {
            mStateChangeListener.onStateChanged(index, type, progress, image, error);
        }
    }

    private void scheduleTimeoutCheck(int index) {
        mMainHandler.postDelayed(() -> {
            LoadingState state = mLoadingStates.get(index);
            if (state != null && !state.isCompleted()) {
                Log.w(TAG, "Loading timeout for index: " + index);
                mTimeouts.incrementAndGet();
                onLoadFailure(index, "Loading timeout after " + LOADING_TIMEOUT + "ms");
            }
        }, LOADING_TIMEOUT);
    }

    private void startProgressAnimation(LoadingState state) {
        // 确保动画器在主线程中创建和启动
        mMainHandler.post(() -> {
            if (state.progressAnimator != null) {
                state.progressAnimator.cancel();
            }
            
            // 创建平滑的进度动画
            state.progressAnimator = ValueAnimator.ofFloat(0f, 0.3f);
            state.progressAnimator.setDuration(2000);
            state.progressAnimator.setRepeatMode(ValueAnimator.REVERSE);
            state.progressAnimator.setRepeatCount(ValueAnimator.INFINITE);
            state.progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            
            state.progressAnimator.addUpdateListener(animation -> {
                if (!state.isCompleted()) {
                    float animatedProgress = (Float) animation.getAnimatedValue();
                    state.currentProgress = Math.max(state.currentProgress, animatedProgress);
                    notifyStateChange(state.index, StateType.LOADING_PROGRESS, 
                                   state.currentProgress, null, null);
                }
            });
            
            state.progressAnimator.start();
        });
    }

    private void smoothProgressUpdate(LoadingState state, float targetProgress) {
        // 确保动画器在主线程中操作
        mMainHandler.post(() -> {
            if (state.progressAnimator != null) {
                state.progressAnimator.cancel();
            }
            
            float startProgress = state.currentProgress;
            float finalTargetProgress = Math.max(startProgress, Math.min(targetProgress, 1.0f));
            
            if (Math.abs(finalTargetProgress - startProgress) < 0.01f) {
                return; // 进度变化太小，忽略
            }
            
            state.progressAnimator = ValueAnimator.ofFloat(startProgress, finalTargetProgress);
            state.progressAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
            state.progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            
            state.progressAnimator.addUpdateListener(animation -> {
                state.currentProgress = (Float) animation.getAnimatedValue();
                notifyStateChange(state.index, StateType.LOADING_PROGRESS, 
                               state.currentProgress, null, null);
            });
            
            state.progressAnimator.start();
        });
    }

    private void completeProgressAnimation(LoadingState state) {
        // 确保动画器在主线程中操作
        mMainHandler.post(() -> {
            if (state.progressAnimator != null) {
                state.progressAnimator.cancel();
            }
            
            // 快速完成到100%
            state.progressAnimator = ValueAnimator.ofFloat(state.currentProgress, 1.0f);
            state.progressAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
            state.progressAnimator.addUpdateListener(animation -> {
                state.currentProgress = (Float) animation.getAnimatedValue();
                notifyStateChange(state.index, StateType.LOADING_PROGRESS, 
                               state.currentProgress, null, null);
            });
            state.progressAnimator.start();
        });
    }

    private void startErrorAnimation(LoadingState state) {
        // 错误震动动画可以在这里实现
        // 这里只做简单的状态通知
        Log.d(TAG, "Error animation for index: " + state.index);
    }

    private boolean shouldAutoRetry(LoadingState state, String error) {
        // 检查重试次数
        if (state.retryCount >= MAX_AUTO_RETRY_COUNT) {
            return false;
        }
        
        // 检查错误类型（网络错误可重试，解析错误不重试）
        if (error.toLowerCase().contains("network") ||
            error.toLowerCase().contains("timeout") ||
            error.toLowerCase().contains("connection")) {
            return true;
        }
        
        return false;
    }

    private void scheduleAutoRetry(LoadingState state, String error) {
        state.retryCount++;
        mAutoRetries.incrementAndGet();
        
        long delay = Math.min(INITIAL_RETRY_DELAY * (1L << (state.retryCount - 1)), MAX_RETRY_DELAY);
        
        Log.d(TAG, "Scheduling auto retry for index: " + state.index + 
              " (attempt " + state.retryCount + ") in " + delay + "ms");
        
        notifyStateChange(state.index, StateType.LOADING_RETRY_SCHEDULED, 
                        state.currentProgress, null, "Retrying in " + (delay / 1000) + "s...");
        
        mMainHandler.postDelayed(() -> {
            if (!state.isCompleted()) {
                retryLoading(state.index);
            }
        }, delay);
    }

    // ===============================
    // 内部类和接口
    // ===============================

    /**
     * 加载状态类型
     */
    public enum StateType {
        LOADING_START,
        LOADING_PROGRESS,
        LOADING_SUCCESS,
        LOADING_ERROR,
        LOADING_CANCELLED,
        LOADING_RETRY,
        LOADING_RETRY_SCHEDULED
    }

    /**
     * 状态变化监听器
     */
    public interface StateChangeListener {
        void onStateChanged(int index, StateType type, float progress, 
                          @Nullable Image image, @Nullable String message);
    }

    /**
     * 加载状态数据类
     */
    public static class LoadingState {
        final int index;
        final String url;
        final long startTime;
        
        volatile float currentProgress = 0.0f;
        volatile int retryCount = 0;
        volatile boolean completed = false;
        volatile boolean cancelled = false;
        volatile boolean success = false;
        
        ValueAnimator progressAnimator;
        
        LoadingState(int index, String url) {
            this.index = index;
            this.url = url;
            this.startTime = System.currentTimeMillis();
        }
        
        void markCompleted(boolean success) {
            this.completed = true;
            this.success = success;
            if (progressAnimator != null) {
                progressAnimator.cancel();
                progressAnimator = null;
            }
        }
        
        void cancel() {
            this.cancelled = true;
            if (progressAnimator != null) {
                progressAnimator.cancel();
                progressAnimator = null;
            }
        }
        
        void resetForRetry() {
            this.currentProgress = 0.0f;
            this.completed = false;
            this.cancelled = false;
            if (progressAnimator != null) {
                progressAnimator.cancel();
                progressAnimator = null;
            }
        }
        
        boolean isCompleted() {
            return completed || cancelled;
        }
        
        long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
    }
}