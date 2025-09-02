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

import android.content.Context;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 日志监控器
 * 监控系统日志中的错误并进行相应处理
 */
public class LogMonitor {

    private static final String TAG = "LogMonitor";
    private static LogMonitor sInstance;

    private Context mContext;
    private SystemErrorHandler mErrorHandler;
    private ScheduledExecutorService mExecutorService;

    // 错误模式匹配
    private static final Pattern SURFACE_FLINGER_PATTERN =
        Pattern.compile(".*Permission Denial: can't access SurfaceFlinger.*");
    private static final Pattern PARCEL_ERROR_PATTERN =
        Pattern.compile(".*get parcel data error.*");
    private static final Pattern GPU_ERROR_PATTERN =
        Pattern.compile(".*SharedImageManager.*Trying to Produce.*");
    private static final Pattern UNIX_SOCKET_PATTERN =
        Pattern.compile(".*failed to create Unix domain socket.*");

    private LogMonitor(Context context) {
        this.mContext = context.getApplicationContext();
        this.mErrorHandler = SystemErrorHandler.getInstance(context);
        this.mExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public static synchronized LogMonitor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LogMonitor(context);
        }
        return sInstance;
    }

    /**
     * 启动日志监控
     */
    public void startMonitoring() {
        Log.i(TAG, "Starting log monitoring...");

        // 每30秒检查一次日志（实际项目中可能需要更频繁）
        mExecutorService.scheduleAtFixedRate(this::checkSystemLogs, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * 停止日志监控
     */
    public void stopMonitoring() {
        Log.i(TAG, "Stopping log monitoring...");
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
            try {
                if (!mExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    mExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                mExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 检查系统日志中的错误
     */
    private void checkSystemLogs() {
        try {
            // 注意：在实际的Android应用中，我们无法直接读取系统日志
            // 这里只是一个示例框架，展示如何处理已知的错误模式

            // 在实际实现中，可以通过以下方式获取日志：
            // 1. 使用Runtime.getRuntime().exec("logcat -d")获取日志
            // 2. 解析日志并匹配错误模式
            // 3. 调用相应的错误处理方法

            // 由于权限限制，这里我们只提供错误处理的方法
            // 实际的日志监控需要在有root权限或系统权限的情况下才能实现

            Log.d(TAG, "Log monitoring check completed");

        } catch (Exception e) {
            Log.w(TAG, "Failed to check system logs", e);
        }
    }

    /**
     * 处理检测到的错误
     * 这个方法应该在实际检测到错误时被调用
     */
    public void handleDetectedError(String logLine) {
        try {
            String lowerLogLine = logLine.toLowerCase();

            // 检查SurfaceFlinger错误
            if (SURFACE_FLINGER_PATTERN.matcher(logLine).matches()) {
                mErrorHandler.handleSurfaceFlingerError(logLine);
                return;
            }

            // 检查Parcel数据错误
            if (PARCEL_ERROR_PATTERN.matcher(logLine).matches()) {
                mErrorHandler.handleParcelError(logLine);
                return;
            }

            // 检查GPU渲染错误
            if (GPU_ERROR_PATTERN.matcher(logLine).matches()) {
                Log.w(TAG, "GPU rendering error detected: " + logLine);
                // GPU错误通常是WebView内部问题，记录即可
                return;
            }

            // 检查Unix socket错误
            if (UNIX_SOCKET_PATTERN.matcher(logLine).matches()) {
                Log.w(TAG, "Unix socket error detected: " + logLine);
                // Unix socket错误通常由X5初始化处理
                return;
            }

            // 处理其他未知错误
            if (logLine.contains("error") || logLine.contains("Error") || logLine.contains("ERROR")) {
                mErrorHandler.handleGenericSystemError("UNKNOWN", logLine);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to handle detected error", e);
        }
    }

    /**
     * 手动报告错误（用于测试或从其他地方调用）
     */
    public void reportError(String errorType, String errorMessage) {
        Log.i(TAG, "Manually reported error - Type: " + errorType + ", Message: " + errorMessage);
        mErrorHandler.handleGenericSystemError(errorType, errorMessage);
    }

    /**
     * 获取监控状态
     */
    public boolean isMonitoring() {
        return mExecutorService != null && !mExecutorService.isShutdown();
    }
}
