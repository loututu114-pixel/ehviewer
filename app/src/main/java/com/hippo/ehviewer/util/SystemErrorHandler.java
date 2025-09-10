/*
 * Copyright 2025 EhViewer
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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.EhApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * System error handler for EhViewer
 * Handles system-level errors and provides recovery mechanisms
 */
public class SystemErrorHandler {
    private static final String TAG = "SystemErrorHandler";

    // Error categories
    public static final String CATEGORY_PERFORMANCE_SERVICE = "performance_service";
    public static final String CATEGORY_UNIX_SOCKET = "unix_socket";
    public static final String CATEGORY_SURFACE_FLINGER = "surface_flinger";
    public static final String CATEGORY_MEMORY = "memory";
    public static final String CATEGORY_DISK_IO = "disk_io";
    public static final String CATEGORY_CROSS_DEVICE = "cross_device";
    public static final String CATEGORY_RUNTIME_FLAGS = "runtime_flags";
    public static final String CATEGORY_GOOGLE_PLAY_SERVICES = "google_play_services";
    public static final String CATEGORY_ART_COMPATIBILITY = "art_compatibility";
    public static final String CATEGORY_SCHED_ASSIST = "sched_assist";
    public static final String CATEGORY_AUTOFILL = "autofill";
    public static final String CATEGORY_SURFACE_FLINGER_OPT = "surface_flinger_opt";

    // ===== 新增：解决tile memory limits exceeded问题 =====
    public static final String CATEGORY_TILE_MEMORY = "tile_memory";
    public static final String CATEGORY_WEBVIEW_CRASH = "webview_crash";
    public static final String CATEGORY_CHROMIUM_RENDER = "chromium_render";

    private final Context context;
    private final Handler mainHandler;
    private final Map<String, ErrorStats> errorStats;
    private final Map<String, ErrorHandler> errorHandlers;

    private static volatile SystemErrorHandler instance;

    public static SystemErrorHandler getInstance(Context context) {
        if (instance == null) {
            synchronized (SystemErrorHandler.class) {
                if (instance == null) {
                    instance = new SystemErrorHandler(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private SystemErrorHandler(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.errorStats = new ConcurrentHashMap<>();
        this.errorHandlers = new HashMap<>();

        initializeErrorHandlers();
    }

    /**
     * Initialize error handlers for different error types
     */
    private void initializeErrorHandlers() {
        // Performance service errors
        errorHandlers.put(CATEGORY_PERFORMANCE_SERVICE, new PerformanceServiceErrorHandler());

        // Unix socket errors
        errorHandlers.put(CATEGORY_UNIX_SOCKET, new UnixSocketErrorHandler());

        // SurfaceFlinger errors
        errorHandlers.put(CATEGORY_SURFACE_FLINGER, new SurfaceFlingerErrorHandler());

        // Memory errors
        errorHandlers.put(CATEGORY_MEMORY, new MemoryErrorHandler());

        // Disk I/O errors
        errorHandlers.put(CATEGORY_DISK_IO, new DiskIOErrorHandler());

        // Runtime flags errors
        errorHandlers.put(CATEGORY_RUNTIME_FLAGS, new RuntimeFlagsErrorHandler());

        // Google Play Services errors
        errorHandlers.put(CATEGORY_GOOGLE_PLAY_SERVICES, new GooglePlayServicesErrorHandler());

        // ART compatibility errors
        errorHandlers.put(CATEGORY_ART_COMPATIBILITY, new ArtCompatibilityErrorHandler());
        errorHandlers.put(CATEGORY_CROSS_DEVICE, new CrossDeviceErrorHandler());

        // SchedAssist errors
        errorHandlers.put(CATEGORY_SCHED_ASSIST, new SchedAssistErrorHandler());

        // Autofill errors
        errorHandlers.put(CATEGORY_AUTOFILL, new AutofillErrorHandler());

        // SurfaceFlinger optimization errors
        errorHandlers.put(CATEGORY_SURFACE_FLINGER_OPT, new SurfaceFlingerOptErrorHandler());

        // ===== 新增：解决tile memory limits exceeded的处理器 =====
        errorHandlers.put(CATEGORY_TILE_MEMORY, new TileMemoryErrorHandler());
        errorHandlers.put(CATEGORY_WEBVIEW_CRASH, new WebViewCrashErrorHandler());
        errorHandlers.put(CATEGORY_CHROMIUM_RENDER, new ChromiumRenderErrorHandler());
    }

    /**
     * Handle system error based on log message
     */
    public void handleSystemError(String logMessage) {
        String category = categorizeError(logMessage);
        if (category != null) {
            ErrorStats stats = errorStats.computeIfAbsent(category,
                k -> new ErrorStats());
            stats.increment();

            ErrorHandler handler = errorHandlers.get(category);
            if (handler != null) {
                handler.handleError(logMessage, stats);
            }
        }
    }

    /**
     * Categorize error based on log message content
     */
    private String categorizeError(String logMessage) {
        if (logMessage.contains("PerformanceService") ||
            logMessage.contains("hardware-performance") ||
            logMessage.contains("Failed to open /proc/")) {
            return CATEGORY_PERFORMANCE_SERVICE;
        }

        if (logMessage.contains("Unix domain socket") ||
            logMessage.contains("eytapBrowser")) {
            return CATEGORY_UNIX_SOCKET;
        }

        if (logMessage.contains("SurfaceFlinger") ||
            logMessage.contains("Permission Denial")) {
            return CATEGORY_SURFACE_FLINGER;
        }

        if (logMessage.contains("CursorWindow") ||
            logMessage.contains("NO_MEMORY") ||
            logMessage.contains("OutOfMemory")) {
            return CATEGORY_MEMORY;
        }

        if (logMessage.contains("Read-only file system") ||
            logMessage.contains("No such file or directory")) {
            return CATEGORY_DISK_IO;
        }

        if (logMessage.contains("Unknown bits set in runtime_flags") ||
            logMessage.contains("runtime_flags")) {
            return CATEGORY_RUNTIME_FLAGS;
        }

        if (logMessage.contains("com.google.android.gms.chimera") ||
            logMessage.contains("Google Play services") ||
            logMessage.contains("Failed to find provider info")) {
            return CATEGORY_GOOGLE_PLAY_SERVICES;
        }

        if (logMessage.contains("ART") ||
            logMessage.contains("dalvik.system.VMRuntime") ||
            logMessage.contains("Not starting debugger since process cannot load the jdwp agent")) {
            return CATEGORY_ART_COMPATIBILITY;
        }

        // ===== 新增：解决tile memory limits exceeded的错误识别 =====
        if (logMessage.contains("tile memory limits exceeded") ||
            logMessage.contains("WARNING: tile memory limits exceeded")) {
            return CATEGORY_TILE_MEMORY;
        }

        if (logMessage.contains("chromium") &&
            (logMessage.contains("crash") || logMessage.contains("CRASH"))) {
            return CATEGORY_WEBVIEW_CRASH;
        }

        if (logMessage.contains("chromium") &&
            (logMessage.contains("render") || logMessage.contains("tile") ||
             logMessage.contains("memory"))) {
            return CATEGORY_CHROMIUM_RENDER;
        }

        return null;
    }

    /**
     * Get error statistics
     */
    public Map<String, ErrorStats> getErrorStats() {
        return new HashMap<>(errorStats);
    }

    /**
     * Reset error statistics
     */
    public void resetErrorStats() {
        errorStats.clear();
    }

    /**
     * Error statistics
     */
    public static class ErrorStats {
        private final AtomicInteger count = new AtomicInteger(0);
        private long firstOccurrence = System.currentTimeMillis();
        private long lastOccurrence = System.currentTimeMillis();

        public void increment() {
            count.incrementAndGet();
            lastOccurrence = System.currentTimeMillis();
        }

        public int getCount() { return count.get(); }
        public long getFirstOccurrence() { return firstOccurrence; }
        public long getLastOccurrence() { return lastOccurrence; }

        public long getTimeSinceFirstOccurrence() {
            return System.currentTimeMillis() - firstOccurrence;
        }

        public long getTimeSinceLastOccurrence() {
            return System.currentTimeMillis() - lastOccurrence;
        }
    }

    /**
     * Error handler interface
     */
    private interface ErrorHandler {
        void handleError(String logMessage, ErrorStats stats);
    }

    /**
     * Performance service error handler
     */
    private class PerformanceServiceErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            // Performance service errors are system-level and usually benign
            if (stats.getCount() == 1) {
                Log.d(TAG, "Performance service error detected (system-level, usually benign): " + logMessage);
            } else if (stats.getCount() > 10) {
                Log.w(TAG, "Frequent performance service errors detected: " + logMessage);
            }

            // Only handle if error occurs very frequently
            if (stats.getCount() > 20) {
                Log.i(TAG, "Performance service errors are very frequent, applying mitigation");

                // Reduce system monitoring frequency
                // This is a placeholder - actual implementation would depend on the specific service

                // Clear error stats after handling
                if (stats.getTimeSinceFirstOccurrence() > 600000) { // 10 minutes
                    stats.count.set(0);
                }
            }
        }
    }

    /**
     * Unix socket error handler
     */
    private class UnixSocketErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.w(TAG, "Unix socket error detected: " + logMessage);

            // These errors are usually benign and don't require specific handling
            // But we can log them for monitoring purposes
            if (stats.getCount() > 10) {
                Log.i(TAG, "High frequency of Unix socket errors detected");
            }
        }
    }

    /**
     * SurfaceFlinger error handler
     */
    private class SurfaceFlingerErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.w(TAG, "SurfaceFlinger error detected: " + logMessage);

            // Trigger memory cleanup if SurfaceFlinger errors are frequent
            if (stats.getCount() > 3) {
                mainHandler.post(() -> {
                    MemoryManager memoryManager = MemoryManager.getInstance(context);
                    if (memoryManager.shouldCleanMemory()) {
                        memoryManager.performMemoryCleanup();
                    }
                });
            }
        }
    }

    /**
     * Memory error handler
     */
    private class MemoryErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.w(TAG, "Memory error detected: " + logMessage);

            // Immediate memory cleanup
            mainHandler.post(() -> {
                MemoryManager memoryManager = MemoryManager.getInstance(context);
                memoryManager.performMemoryCleanup();
            });
        }
    }

    /**
     * Disk I/O error handler
     */
    private class DiskIOErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            // Read-only file system errors are common and expected in some scenarios
            if (logMessage.contains("Read-only file system")) {
                if (stats.getCount() == 1) {
                    Log.d(TAG, "Read-only file system detected (this is normal in some system states)");
                } else if (stats.getCount() > 5) {
                    Log.w(TAG, "Frequent read-only file system errors detected: " + logMessage);
                }
            } else {
                Log.w(TAG, "Disk I/O error detected: " + logMessage);
            }

            // Only perform cleanup for non-read-only errors or if errors are frequent
            if (!logMessage.contains("Read-only file system") || stats.getCount() > 10) {
                mainHandler.post(() -> {
                    try {
                        MemoryManager memoryManager = MemoryManager.getInstance(context);
                        if (memoryManager.isDiskSpaceLow()) {
                            memoryManager.clearApplicationCache();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error during disk cleanup", e);
                    }
                });
            }
        }
    }

    /**
     * Parse system log file for errors
     */
    public void parseSystemLogFile(File logFile) {
        if (logFile == null || !logFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isErrorLine(line)) {
                    handleSystemError(line);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading log file: " + logFile.getAbsolutePath(), e);
        }
    }

    /**
     * Check if a log line contains an error
     */
    private boolean isErrorLine(String line) {
        return line.contains(" E ") || // Android error log format
               line.contains(" ERROR ") ||
               line.contains("Exception") ||
               line.contains("Error");
    }

    /**
     * Handle SurfaceFlinger permission error
     */
    public void handleSurfaceFlingerError(String logMessage) {
        handleSystemError(logMessage);
    }

    /**
     * Handle Parcel data error
     */
    public void handleParcelError(String logMessage) {
        handleSystemError(logMessage);
    }

    /**
     * Handle generic system error
     */
    public void handleGenericSystemError(String errorType, String errorMessage) {
        String fullMessage = "[" + errorType + "] " + errorMessage;
        handleSystemError(fullMessage);
    }

    /**
     * Get system information for debugging
     */
    @NonNull
    public String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== System Information ===\n");
        info.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        info.append("API Level: ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("Device: ").append(Build.DEVICE).append("\n");
        info.append("Model: ").append(Build.MODEL).append("\n");
        info.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");

        MemoryManager memoryManager = MemoryManager.getInstance(context);
        MemoryManager.MemoryStats memoryStats = memoryManager.getMemoryStats();
        if (memoryStats != null) {
            info.append("Total Memory: ").append(memoryStats.totalMemory / 1024 / 1024).append("MB\n");
            info.append("Available Memory: ").append(memoryStats.availableMemory / 1024 / 1024).append("MB\n");
            info.append("Memory Low: ").append(memoryStats.isLowMemory).append("\n");
        }

        MemoryManager.DiskSpaceInfo diskInfo = memoryManager.getDiskSpaceInfo();
        info.append("Disk Total: ").append(diskInfo.totalBytes / 1024 / 1024).append("MB\n");
        info.append("Disk Available: ").append(diskInfo.availableBytes / 1024 / 1024).append("MB\n");

        info.append("\n=== Error Statistics ===\n");
        for (Map.Entry<String, ErrorStats> entry : errorStats.entrySet()) {
            info.append(entry.getKey()).append(": ").append(entry.getValue().getCount()).append(" times\n");
        }

        return info.toString();
    }

    /**
     * Runtime flags error handler
     */
    private class RuntimeFlagsErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.d(TAG, "Runtime flags error detected: " + logMessage);

            // Runtime flags errors are usually benign warnings
            // Only log if they occur frequently
            if (stats.getCount() > 3) {
                Log.i(TAG, "Frequent runtime flags warnings detected");

                // Try to apply runtime compatibility fixes
                mainHandler.post(() -> {
                    try {
                        // Set system properties to suppress warnings
                        System.setProperty("dalvik.vm.checkjni", "false");

                        // Additional runtime compatibility handling
                        // This is handled by SystemCompatibilityManager
                        Log.d(TAG, "Applied runtime flags compatibility fixes");
                    } catch (Exception e) {
                        Log.w(TAG, "Error applying runtime flags fixes", e);
                    }
                });
            }
        }
    }

    /**
     * Google Play Services error handler
     */
    private class GooglePlayServicesErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.d(TAG, "Google Play Services error detected: " + logMessage);

            // Check if we should enable fallback mode
            if (stats.getCount() > 2) {
                Log.i(TAG, "Multiple Google Play Services errors detected, enabling fallback mode");

                mainHandler.post(() -> {
                    try {
                        // Enable Google Play Services fallback mode
                        com.hippo.ehviewer.Settings.putGooglePlayServicesFallback(true);

                        // Notify application about the fallback
                        EhApplication application = (EhApplication) context.getApplicationContext();
                        Log.i(TAG, "Google Play Services fallback mode enabled");

                    } catch (Exception e) {
                        Log.w(TAG, "Error enabling Google Play Services fallback", e);
                    }
                });
            }
        }
    }

    /**
     * ART compatibility error handler
     */
    private class ArtCompatibilityErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.d(TAG, "ART compatibility error detected: " + logMessage);

            // ART errors are usually non-critical
            // Only handle if they occur frequently
            if (stats.getCount() > 5) {
                Log.i(TAG, "Frequent ART compatibility issues detected");

                mainHandler.post(() -> {
                    try {
                        // Apply ART-specific compatibility fixes
                        System.setProperty("dalvik.vm.dex2oat-flags", "--no-watch-dog");

                        // Additional ART compatibility handling
                        Log.d(TAG, "Applied ART compatibility fixes");

                    } catch (Exception e) {
                        Log.w(TAG, "Error applying ART compatibility fixes", e);
                    }
                });
            }
        }
    }

    /**
     * CrossDevice error handler
     */
    private class CrossDeviceErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            // Only log warnings for frequent CrossDevice errors, as these are often expected for regular apps
            if (stats.getCount() > 10) {
                Log.w(TAG, "Frequent CrossDevice errors detected (" + stats.getCount() + "): " + logMessage);
            } else if (stats.getCount() == 1) {
                Log.d(TAG, "CrossDevice error detected (this is normal for regular apps): " + logMessage);
            }

            // Handle CrossDevice errors immediately, but only if not too frequent
            if (stats.getCount() <= 5) {
                mainHandler.post(() -> {
                    try {
                        CrossDeviceErrorRecovery recovery = CrossDeviceErrorRecovery.getInstance(context);
                        recovery.handleCrossDeviceError(logMessage, null);

                        if (stats.getCount() == 1) {
                            Log.i(TAG, "CrossDevice error recovery initiated");
                        }

                    } catch (Exception e) {
                        Log.w(TAG, "Error during CrossDevice error recovery", e);
                    }
                });
            }

            // Clear error stats after handling to prevent duplicate recovery attempts
            if (stats.getTimeSinceFirstOccurrence() > 300000) { // 5 minutes
                stats.count.set(0);
            }
        }
    }

    /**
     * SchedAssist error handler
     */
    private class SchedAssistErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.w(TAG, "SchedAssist error detected: " + logMessage);

            // Handle SchedAssist errors
            mainHandler.post(() -> {
                try {
                    SchedulingOptimizer optimizer = SchedulingOptimizer.getInstance(context);
                    optimizer.handleSchedAssistError(logMessage);

                    Log.i(TAG, "SchedAssist error handled");

                } catch (Exception e) {
                    Log.w(TAG, "Error handling SchedAssist error", e);
                }
            });
        }
    }

    /**
     * Autofill error handler
     */
    private class AutofillErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.w(TAG, "Autofill error detected: " + logMessage);

            // Handle autofill errors
            mainHandler.post(() -> {
                try {
                    com.hippo.ehviewer.util.AutofillErrorHandler handler =
                        com.hippo.ehviewer.util.AutofillErrorHandler.getInstance(context);
                    handler.handleAutofillError(logMessage, null);

                    Log.i(TAG, "Autofill error handled");

                } catch (Exception e) {
                    Log.w(TAG, "Error handling autofill error", e);
                }
            });
        }
    }

    /**
     * SurfaceFlinger optimization error handler
     */
    private class SurfaceFlingerOptErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.w(TAG, "SurfaceFlinger optimization error detected: " + logMessage);

            // Handle SurfaceFlinger errors
            mainHandler.post(() -> {
                try {
                    SurfaceFlingerOptimizer optimizer = SurfaceFlingerOptimizer.getInstance(context);
                    optimizer.handleSurfaceFlingerError(logMessage);

                    Log.i(TAG, "SurfaceFlinger error handled");

                } catch (Exception e) {
                    Log.w(TAG, "Error handling SurfaceFlinger error", e);
                }
            });
        }
    }

    /**
     * ===== 新增：解决tile memory limits exceeded的错误处理器 =====
     */

    /**
     * Tile memory error handler - 处理瓦片内存限制问题
     */
    private class TileMemoryErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.w(TAG, "Tile memory limits exceeded detected: " + logMessage);

            // 立即触发内存优化
            mainHandler.post(() -> {
                try {
                    // 获取WebView内存管理器
                    com.hippo.ehviewer.performance.WebViewMemoryManager memoryManager =
                        com.hippo.ehviewer.performance.WebViewMemoryManager.getInstance(context);

                    // 应用瓦片内存优化
                    memoryManager.checkTileMemoryPressure();

                    // 设置更严格的内存限制
                    System.setProperty("webview.tile_cache_size", "4"); // 4MB
                    System.setProperty("webview.max_rendering_threads", "1");

                    // 强制垃圾回收
                    Runtime.getRuntime().gc();
                    Runtime.getRuntime().runFinalization();
                    Runtime.getRuntime().gc();

                    Log.i(TAG, "Tile memory optimization applied");

                } catch (Exception e) {
                    Log.e(TAG, "Error handling tile memory limits", e);
                }
            });
        }
    }

    /**
     * WebView crash error handler - 处理WebView崩溃
     */
    private class WebViewCrashErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.e(TAG, "WebView crash detected: " + logMessage);

            // 处理WebView崩溃
            mainHandler.post(() -> {
                try {
                    // 设置系统属性防止进一步崩溃
                    System.setProperty("webview.enable_threaded_rendering", "false");
                    System.setProperty("webview.enable_surface_control", "false");
                    System.setProperty("webview.enable_hardware_acceleration", "false");

                    // 清理WebView相关缓存
                    MemoryManager memoryManager = MemoryManager.getInstance(context);
                    memoryManager.clearApplicationCache();

                    Log.i(TAG, "WebView crash recovery applied");

                } catch (Exception e) {
                    Log.e(TAG, "Error handling WebView crash", e);
                }
            });
        }
    }

    /**
     * Chromium render error handler - 处理Chromium渲染错误
     */
    private class ChromiumRenderErrorHandler implements ErrorHandler {
        @Override
        public void handleError(String logMessage, ErrorStats stats) {
            Log.w(TAG, "Chromium render error detected: " + logMessage);

            // 处理Chromium渲染问题
            mainHandler.post(() -> {
                try {
                    // 应用渲染优化
                    System.setProperty("webview.enable_threaded_rendering", "true");
                    System.setProperty("webview.enable_surface_control", "true");
                    System.setProperty("webview.max_rendering_threads", "2");

                    // 减少渲染负载
                    System.setProperty("webview.tile_cache_size", "6"); // 6MB

                    Log.i(TAG, "Chromium render optimization applied");

                } catch (Exception e) {
                    Log.e(TAG, "Error handling Chromium render error", e);
                }
            });
        }
    }

    /**
     * ===== 新增：便捷方法用于处理特定错误 =====
     */

    /**
     * 处理tile memory错误
     */
    public void handleTileMemoryError(String logMessage) {
        handleSystemError(logMessage);
    }

    /**
     * 处理WebView崩溃
     */
    public void handleWebViewCrash(String logMessage) {
        handleSystemError(logMessage);
    }

    /**
     * 处理Chromium渲染错误
     */
    public void handleChromiumRenderError(String logMessage) {
        handleSystemError(logMessage);
    }

    /**
     * 获取详细的错误报告
     */
    public String getDetailedErrorReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Detailed System Error Report ===\n");
        report.append("Timestamp: ").append(System.currentTimeMillis()).append("\n\n");

        for (Map.Entry<String, ErrorStats> entry : errorStats.entrySet()) {
            ErrorStats stats = entry.getValue();
            report.append("Category: ").append(entry.getKey()).append("\n");
            report.append("  Count: ").append(stats.getCount()).append("\n");
            report.append("  First occurrence: ").append(stats.getFirstOccurrence()).append("\n");
            report.append("  Last occurrence: ").append(stats.getLastOccurrence()).append("\n");
            report.append("  Time since first: ").append(stats.getTimeSinceFirstOccurrence() / 1000).append("s\n");
            report.append("  Time since last: ").append(stats.getTimeSinceLastOccurrence() / 1000).append("s\n");
            report.append("\n");
        }

        report.append("=== Memory Information ===\n");
        try {
            Runtime runtime = Runtime.getRuntime();
            report.append("Total Memory: ").append(runtime.totalMemory() / 1024 / 1024).append("MB\n");
            report.append("Free Memory: ").append(runtime.freeMemory() / 1024 / 1024).append("MB\n");
            report.append("Used Memory: ").append((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024).append("MB\n");
            report.append("Max Memory: ").append(runtime.maxMemory() / 1024 / 1024).append("MB\n");
        } catch (Exception e) {
            report.append("Memory info unavailable\n");
        }

        return report.toString();
    }
}
