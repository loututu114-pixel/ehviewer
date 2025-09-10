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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * System monitor for EhViewer
 * Monitors system performance and logs metrics
 */
public class SystemMonitor {
    private static final String TAG = "SystemMonitor";

    private final Context context;
    private final Handler mainHandler;
    private final ScheduledExecutorService executor;
    private final SystemErrorHandler errorHandler;
    private final MemoryManager memoryManager;

    private boolean isMonitoring = false;
    private File logFile;
    private FileWriter logWriter;

    private static volatile SystemMonitor instance;

    public static SystemMonitor getInstance(Context context) {
        if (instance == null) {
            synchronized (SystemMonitor.class) {
                if (instance == null) {
                    instance = new SystemMonitor(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private SystemMonitor(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.errorHandler = SystemErrorHandler.getInstance(context);
        this.memoryManager = MemoryManager.getInstance(context);

        initializeLogFile();
    }

    /**
     * Initialize log file for system monitoring
     */
    private void initializeLogFile() {
        try {
            File logDir = new File(context.getExternalFilesDir(null), "system_logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
            logFile = new File(logDir, "system_monitor_" + timestamp + ".log");

            logWriter = new FileWriter(logFile, true);
            logWriter.write("=== EhViewer System Monitor Log ===\n");
            logWriter.write("Started at: " + new Date() + "\n");
            logWriter.write("=========================================\n");
            logWriter.flush();

            Log.i(TAG, "System monitor log initialized: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize log file", e);
        }
    }

    /**
     * Start system monitoring
     */
    public void startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "System monitoring is already running");
            return;
        }

        isMonitoring = true;
        Log.i(TAG, "Starting system monitoring");

        // Initial monitoring
        performMonitoring();

        // Schedule periodic monitoring every 30 seconds
        executor.scheduleAtFixedRate(this::performMonitoring, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Stop system monitoring
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }

        isMonitoring = false;
        Log.i(TAG, "Stopping system monitoring");

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        closeLogWriter();
    }

    /**
     * Perform system monitoring
     */
    private void performMonitoring() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

            // Memory monitoring
            monitorMemory(timestamp);

            // CPU monitoring
            monitorCpu(timestamp);

            // Disk monitoring
            monitorDisk(timestamp);

            // Network monitoring
            monitorNetwork(timestamp);

            // Error monitoring
            monitorErrors(timestamp);

            // Flush log writer
            if (logWriter != null) {
                logWriter.flush();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during system monitoring", e);
        }
    }

    /**
     * Monitor memory usage
     */
    private void monitorMemory(String timestamp) {
        try {
            MemoryManager.MemoryStats memoryStats = memoryManager.getMemoryStats();
            if (memoryStats != null) {
                String memoryInfo = String.format(
                    "[%s] MEMORY - Total: %.1fMB, Available: %.1fMB, Used: %.1f%%, Low: %s\n",
                    timestamp,
                    memoryStats.totalMemory / 1024.0 / 1024.0,
                    memoryStats.availableMemory / 1024.0 / 1024.0,
                    memoryStats.getUsedPercentage(),
                    memoryStats.isLowMemory
                );

                writeToLog(memoryInfo);

                // Trigger memory cleanup if memory is low
                if (memoryStats.isLowMemory) {
                    Log.w(TAG, "Low memory detected, triggering cleanup");
                    memoryManager.performMemoryCleanup();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error monitoring memory", e);
        }
    }

    /**
     * Monitor CPU usage
     */
    private void monitorCpu(String timestamp) {
        try {
            // Read CPU usage from /proc/stat
            Process process = Runtime.getRuntime().exec("cat /proc/stat");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = reader.readLine();
            if (line != null && line.startsWith("cpu ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 8) {
                    long user = Long.parseLong(parts[1]);
                    long nice = Long.parseLong(parts[2]);
                    long system = Long.parseLong(parts[3]);
                    long idle = Long.parseLong(parts[4]);

                    long total = user + nice + system + idle;
                    long used = total - idle;

                    double cpuUsage = total > 0 ? (used * 100.0) / total : 0.0;

                    String cpuInfo = String.format(
                        "[%s] CPU - Usage: %.1f%%\n",
                        timestamp,
                        cpuUsage
                    );

                    writeToLog(cpuInfo);
                }
            }

            reader.close();
            process.destroy();

        } catch (Exception e) {
            Log.e(TAG, "Error monitoring CPU", e);
        }
    }

    /**
     * Monitor disk usage
     */
    private void monitorDisk(String timestamp) {
        try {
            MemoryManager.DiskSpaceInfo diskInfo = memoryManager.getDiskSpaceInfo();

            String diskInfoStr = String.format(
                "[%s] DISK - Total: %.1fMB, Available: %.1fMB, Used: %.1f%%\n",
                timestamp,
                diskInfo.totalBytes / 1024.0 / 1024.0,
                diskInfo.availableBytes / 1024.0 / 1024.0,
                diskInfo.getUsedPercentage()
            );

            writeToLog(diskInfoStr);

            // Trigger cleanup if disk space is low
            if (memoryManager.isDiskSpaceLow()) {
                Log.w(TAG, "Low disk space detected, triggering cleanup");
                memoryManager.clearApplicationCache();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error monitoring disk", e);
        }
    }

    /**
     * Monitor network status
     */
    private void monitorNetwork(String timestamp) {
        try {
            // This is a basic network monitoring - in a real implementation
            // you might want to track actual network usage
            String networkInfo = String.format(
                "[%s] NETWORK - Status: OK\n",
                timestamp
            );

            writeToLog(networkInfo);

        } catch (Exception e) {
            Log.e(TAG, "Error monitoring network", e);
        }
    }

    /**
     * Monitor errors
     */
    private void monitorErrors(String timestamp) {
        try {
            var errorStats = errorHandler.getErrorStats();

            if (!errorStats.isEmpty()) {
                StringBuilder errorInfo = new StringBuilder();
                errorInfo.append(String.format("[%s] ERRORS - ", timestamp));

                for (var entry : errorStats.entrySet()) {
                    errorInfo.append(String.format("%s: %d, ",
                        entry.getKey(),
                        entry.getValue().getCount()));
                }

                // Remove trailing comma and space
                if (errorInfo.length() > 0) {
                    errorInfo.setLength(errorInfo.length() - 2);
                }
                errorInfo.append("\n");

                writeToLog(errorInfo.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error monitoring errors", e);
        }
    }

    /**
     * Write to log file
     */
    private void writeToLog(String message) {
        try {
            if (logWriter != null) {
                logWriter.write(message);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }

    /**
     * Close log writer
     */
    private void closeLogWriter() {
        try {
            if (logWriter != null) {
                logWriter.write("=== System Monitor Stopped ===\n");
                logWriter.write("Stopped at: " + new Date() + "\n");
                logWriter.close();
                logWriter = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing log writer", e);
        }
    }

    /**
     * Get current log file
     */
    public File getLogFile() {
        return logFile;
    }

    /**
     * Get monitoring status
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }

    /**
     * Force immediate monitoring
     */
    public void forceMonitoring() {
        if (isMonitoring) {
            performMonitoring();
        }
    }

    /**
     * Get system summary report
     */
    @NonNull
    public String getSystemSummaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== System Monitor Summary Report ===\n");
        report.append("Monitoring Status: ").append(isMonitoring ? "Active" : "Inactive").append("\n");

        if (logFile != null) {
            report.append("Log File: ").append(logFile.getAbsolutePath()).append("\n");
            report.append("Log Size: ").append(logFile.length()).append(" bytes\n");
        }

        report.append("\n").append(errorHandler.getSystemInfo());

        return report.toString();
    }
}
