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
import android.os.Debug;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 应用启动日志记录器
 * 记录详细的应用启动过程和系统信息
 */
public class StartupLogger {

    private static final String TAG = "StartupLogger";
    private static final String STARTUP_LOG_FILENAME = "startup_log.txt";

    private final Context context;
    private final long startTime;
    private final StringBuilder logBuffer;
    private final SimpleDateFormat timestampFormat;

    private static volatile StartupLogger instance;

    public static StartupLogger getInstance(Context context) {
        if (instance == null) {
            synchronized (StartupLogger.class) {
                if (instance == null) {
                    instance = new StartupLogger(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private StartupLogger(Context context) {
        this.context = context;
        this.startTime = System.currentTimeMillis();
        this.logBuffer = new StringBuilder();
        this.timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

        initializeStartupLog();
    }

    /**
     * 初始化启动日志
     */
    private void initializeStartupLog() {
        log("=== EhViewer Startup Log ===");
        log("Timestamp: " + timestampFormat.format(new Date()));
        log("Process ID: " + Process.myPid());
        log("Thread ID: " + Thread.currentThread().getId());
        log("Thread Name: " + Thread.currentThread().getName());

        // 记录系统信息
        recordSystemInfo();

        // 记录内存信息
        recordMemoryInfo();

        log("");
    }

    /**
     * 记录系统信息
     */
    private void recordSystemInfo() {
        log("=== System Information ===");
        log("Android Version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        log("Build ID: " + Build.ID);
        log("Device: " + Build.DEVICE);
        log("Model: " + Build.MODEL);
        log("Manufacturer: " + Build.MANUFACTURER);
        log("Brand: " + Build.BRAND);
        log("Product: " + Build.PRODUCT);
        log("Hardware: " + Build.HARDWARE);
        log("Board: " + Build.BOARD);
        log("Bootloader: " + Build.BOOTLOADER);
        log("CPU ABI: " + Build.CPU_ABI);
        log("CPU ABI2: " + Build.CPU_ABI2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            log("Supported ABIs: " + java.util.Arrays.toString(Build.SUPPORTED_ABIS));
        }
        log("Serial: " + Build.SERIAL);
        log("Tags: " + Build.TAGS);
        log("Type: " + Build.TYPE);
        log("User: " + Build.USER);
        log("Host: " + Build.HOST);
        log("Fingerprint: " + Build.FINGERPRINT);
        log("Display: " + Build.DISPLAY);

        // Java虚拟机信息
        log("Java VM Version: " + System.getProperty("java.vm.version"));
        log("Java VM Name: " + System.getProperty("java.vm.name"));
        log("Java VM Vendor: " + System.getProperty("java.vm.vendor"));
        log("Java Version: " + System.getProperty("java.version"));
        log("Java Vendor: " + System.getProperty("java.vendor"));

        // 系统属性
        log("OS Name: " + System.getProperty("os.name"));
        log("OS Version: " + System.getProperty("os.version"));
        log("OS Arch: " + System.getProperty("os.arch"));
    }

    /**
     * 记录内存信息
     */
    private void recordMemoryInfo() {
        log("=== Memory Information ===");
        log("Max Memory: " + formatBytes(Runtime.getRuntime().maxMemory()));
        log("Total Memory: " + formatBytes(Runtime.getRuntime().totalMemory()));
        log("Free Memory: " + formatBytes(Runtime.getRuntime().freeMemory()));
        log("Used Memory: " + formatBytes(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        log("Native Allocated: " + formatBytes(Debug.getNativeHeapAllocatedSize()));
        log("Native Free: " + formatBytes(Debug.getNativeHeapFreeSize()));
        log("Native Size: " + formatBytes(Debug.getNativeHeapSize()));
    }

    /**
     * 记录启动步骤
     */
    public void logStartupStep(String step, String details) {
        log("STARTUP: " + step + " - " + (details != null ? details : ""));
    }

    /**
     * 记录启动步骤开始
     */
    public void logStartupStepStart(String step) {
        log("STARTUP_START: " + step);
    }

    /**
     * 记录启动步骤完成
     */
    public void logStartupStepEnd(String step, long durationMs) {
        log("STARTUP_END: " + step + " (duration: " + durationMs + "ms)");
    }

    /**
     * 记录警告
     */
    public void logWarning(String message, Throwable throwable) {
        log("WARNING: " + message);
        if (throwable != null) {
            log("WARNING_EXCEPTION: " + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    /**
     * 记录错误
     */
    public void logError(String message, Throwable throwable) {
        log("ERROR: " + message);
        if (throwable != null) {
            log("ERROR_EXCEPTION: " + throwable.getClass().getName() + ": " + throwable.getMessage());
            for (StackTraceElement element : throwable.getStackTrace()) {
                log("ERROR_STACK: " + element.toString());
            }
        }
    }

    /**
     * 记录启动完成
     */
    public void logStartupComplete() {
        long duration = System.currentTimeMillis() - startTime;
        log("STARTUP_COMPLETE: Total startup time: " + duration + "ms");
        log("=== Startup Log End ===");

        // 保存日志到文件
        saveLogToFile();
    }

    /**
     * 获取启动持续时间
     */
    public long getStartupDuration() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 记录通用日志
     */
    private void log(String message) {
        String timestampedMessage = "[" + timestampFormat.format(new Date()) + "] " + message;
        logBuffer.append(timestampedMessage).append("\n");

        // 同时输出到Android日志
        Log.d(TAG, message);
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 保存日志到文件
     */
    private void saveLogToFile() {
        try {
            File logDir = new File(context.getExternalFilesDir(null), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            File logFile = new File(logDir, STARTUP_LOG_FILENAME);
            try (FileWriter writer = new FileWriter(logFile, false)) {
                writer.write(logBuffer.toString());
                writer.flush();
            }

            Log.i(TAG, "Startup log saved to: " + logFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e(TAG, "Failed to save startup log", e);
        }
    }

    /**
     * 获取日志内容
     */
    public String getLogContent() {
        return logBuffer.toString();
    }

    /**
     * 清理旧的日志文件
     */
    public void cleanupOldLogs() {
        try {
            File logDir = new File(context.getExternalFilesDir(null), "logs");
            if (logDir.exists() && logDir.isDirectory()) {
                File[] logFiles = logDir.listFiles((dir, name) -> name.startsWith("startup_log"));
                if (logFiles != null && logFiles.length > 5) {
                    // 保留最新的5个日志文件
                    java.util.Arrays.sort(logFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                    for (int i = 5; i < logFiles.length; i++) {
                        if (logFiles[i].delete()) {
                            Log.d(TAG, "Deleted old log file: " + logFiles[i].getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error cleaning up old logs", e);
        }
    }
}
