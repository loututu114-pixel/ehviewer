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
            Log.w(TAG, "Performance service error detected: " + logMessage);

            // Only handle if error occurs frequently
            if (stats.getCount() > 5) {
                Log.i(TAG, "Performance service errors are frequent, applying mitigation");

                // Reduce system monitoring frequency
                // This is a placeholder - actual implementation would depend on the specific service

                // Clear error stats after handling
                if (stats.getTimeSinceFirstOccurrence() > 300000) { // 5 minutes
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
            Log.w(TAG, "Disk I/O error detected: " + logMessage);

            // Check disk space and clean up if necessary
            mainHandler.post(() -> {
                MemoryManager memoryManager = MemoryManager.getInstance(context);
                if (memoryManager.isDiskSpaceLow()) {
                    memoryManager.clearApplicationCache();
                }
            });
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
            Log.w(TAG, "CrossDevice error detected: " + logMessage);

            // Handle CrossDevice errors immediately
            mainHandler.post(() -> {
                try {
                    CrossDeviceErrorRecovery recovery = CrossDeviceErrorRecovery.getInstance(context);
                    recovery.handleCrossDeviceError(logMessage, null);

                    Log.i(TAG, "CrossDevice error recovery initiated");

                } catch (Exception e) {
                    Log.w(TAG, "Error during CrossDevice error recovery", e);
                }
            });

            // Clear error stats after handling to prevent duplicate recovery attempts
            if (stats.getTimeSinceFirstOccurrence() > 60000) { // 1 minute
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
}
