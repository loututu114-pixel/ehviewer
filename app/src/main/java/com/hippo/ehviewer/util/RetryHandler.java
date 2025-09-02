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

package com.hippo.ehviewer.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重试处理器
 * 提供自动重试机制处理临时性错误
 */
public class RetryHandler {

    private static final String TAG = "RetryHandler";

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000; // 1秒

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    /**
     * 重试配置
     */
    public static class RetryConfig {
        public int maxRetries = DEFAULT_MAX_RETRIES;
        public long initialDelayMs = DEFAULT_RETRY_DELAY_MS;
        public double backoffMultiplier = 2.0;
        public boolean exponentialBackoff = true;

        public RetryConfig() {}

        public RetryConfig(int maxRetries, long initialDelayMs) {
            this.maxRetries = maxRetries;
            this.initialDelayMs = initialDelayMs;
        }
    }

    /**
     * 重试回调接口
     */
    public interface RetryCallback<T> {
        T onRetry(int attempt) throws Exception;
        void onSuccess(T result);
        void onFailure(Exception lastException, int totalAttempts);
    }

    /**
     * 简单重试任务
     */
    public interface RetryableTask {
        void execute() throws Exception;
    }

    /**
     * 执行可重试的任务
     */
    public void executeWithRetry(RetryableTask task, RetryConfig config, Runnable onSuccess, Runnable onFailure) {
        try {
            task.execute();
            if (onSuccess != null) onSuccess.run();
        } catch (Exception e) {
            if (onFailure != null) onFailure.run();
        }
    }

    /**
     * 执行可重试的任务（带返回值）
     */
    public <T> void executeWithRetry(Callable<T> callable, RetryConfig config, RetryCallback<T> callback) {
        if (config == null) {
            config = new RetryConfig();
        }

        AtomicInteger attempts = new AtomicInteger(0);
        executeWithRetryInternal(callable, config, attempts, callback);
    }

    private <T> void executeWithRetryInternal(Callable<T> callable, RetryConfig config,
                                            AtomicInteger attempts, RetryCallback<T> callback) {
        int currentAttempt = attempts.incrementAndGet();

        try {
            Log.d(TAG, "Attempting operation (attempt " + currentAttempt + "/" + (config.maxRetries + 1) + ")");

            T result = callable.call();

            Log.d(TAG, "Operation succeeded on attempt " + currentAttempt);
            if (callback != null) {
                mMainHandler.post(() -> callback.onSuccess(result));
            }

        } catch (Exception e) {
            Log.w(TAG, "Operation failed on attempt " + currentAttempt + ": " + e.getMessage());

            if (currentAttempt <= config.maxRetries) {
                // 计算下次重试的延迟时间
                long delayMs = calculateDelay(config, currentAttempt);

                Log.d(TAG, "Scheduling retry in " + delayMs + "ms");

                mMainHandler.postDelayed(() -> {
                    executeWithRetryInternal(callable, config, attempts, callback);
                }, delayMs);

            } else {
                Log.e(TAG, "Operation failed after " + currentAttempt + " attempts");
                if (callback != null) {
                    mMainHandler.post(() -> callback.onFailure(e, currentAttempt));
                }
            }
        }
    }

    /**
     * 计算重试延迟时间
     */
    private long calculateDelay(RetryConfig config, int attempt) {
        if (!config.exponentialBackoff) {
            return config.initialDelayMs;
        }

        // 指数退避：delay = initialDelay * (backoffMultiplier ^ (attempt - 1))
        long delay = (long) (config.initialDelayMs * Math.pow(config.backoffMultiplier, attempt - 1));

        // 最大延迟限制为30秒
        return Math.min(delay, 30000);
    }

    /**
     * 为WebView操作创建重试配置
     */
    public static RetryConfig createWebViewRetryConfig() {
        RetryConfig config = new RetryConfig();
        config.maxRetries = 2;
        config.initialDelayMs = 500; // WebView操作使用较短延迟
        config.backoffMultiplier = 1.5;
        return config;
    }

    /**
     * 为网络操作创建重试配置
     */
    public static RetryConfig createNetworkRetryConfig() {
        RetryConfig config = new RetryConfig();
        config.maxRetries = 3;
        config.initialDelayMs = 2000; // 网络操作使用较长延迟
        config.backoffMultiplier = 2.0;
        return config;
    }

    /**
     * 为系统服务操作创建重试配置
     */
    public static RetryConfig createSystemServiceRetryConfig() {
        RetryConfig config = new RetryConfig();
        config.maxRetries = 1; // 系统服务错误通常不需要重试
        config.initialDelayMs = 1000;
        config.exponentialBackoff = false;
        return config;
    }
}
