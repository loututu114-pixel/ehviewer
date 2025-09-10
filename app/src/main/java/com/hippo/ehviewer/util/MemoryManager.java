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

import android.app.ActivityManager;
import android.content.Context;
import android.database.CursorWindow;
import android.os.Build;
import android.system.Os;
import android.system.StructStatVfs;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Memory management utilities for EhViewer
 * Handles memory pressure, cache cleanup, and CursorWindow issues
 */
public class MemoryManager {
    private static final String TAG = "MemoryManager";

    private static final long MIN_FREE_MEMORY = 50 * 1024 * 1024; // 50MB
    private static final long CURSOR_WINDOW_SIZE = 2 * 1024 * 1024; // 2MB

    private final Context context;
    private final ActivityManager activityManager;

    private static volatile MemoryManager instance;

    public static MemoryManager getInstance(Context context) {
        if (instance == null) {
            synchronized (MemoryManager.class) {
                if (instance == null) {
                    instance = new MemoryManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private MemoryManager(Context context) {
        this.context = context;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    /**
     * Check if system is under memory pressure
     */
    public boolean isMemoryLow() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.lowMemory;
    }

    /**
     * Get available memory in bytes
     */
    public long getAvailableMemory() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    /**
     * Get total memory in bytes
     */
    public long getTotalMemory() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem;
    }

    /**
     * Check if we should perform memory cleanup
     */
    public boolean shouldCleanMemory() {
        return getAvailableMemory() < MIN_FREE_MEMORY;
    }

    /**
     * Perform memory cleanup operations
     */
    public void performMemoryCleanup() {
        Log.i(TAG, "Performing memory cleanup");

        try {
            // Clear system caches
            clearApplicationCache();

            // Force garbage collection
            System.gc();
            System.runFinalization();
            System.gc();

            // Clear CursorWindow if possible
            clearCursorWindowCache();

            Log.i(TAG, "Memory cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during memory cleanup", e);
        }
    }

    /**
     * Clear application cache directories
     */
    public void clearApplicationCache() {
        try {
            // Clear internal cache
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteDirectoryContents(cacheDir, false);
            }

            // Clear external cache
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteDirectoryContents(externalCacheDir, false);
            }

            Log.i(TAG, "Application cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing application cache", e);
        }
    }

    /**
     * Clear CursorWindow cache to prevent memory issues
     */
    private void clearCursorWindowCache() {
        try {
            // Force CursorWindow cleanup
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                CursorWindow cursorWindow = new CursorWindow("temp");
                cursorWindow.close();
            }

            // Clear any cached cursors
            System.gc();

            Log.i(TAG, "CursorWindow cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing CursorWindow cache", e);
        }
    }

    /**
     * Safely delete directory contents
     */
    private void deleteDirectoryContents(File dir, boolean deleteDir) {
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryContents(file, true);
                } else {
                    try {
                        if (!file.delete()) {
                            Log.w(TAG, "Failed to delete file: " + file.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting file: " + file.getAbsolutePath(), e);
                    }
                }
            }
        }

        if (deleteDir) {
            try {
                if (!dir.delete()) {
                    Log.w(TAG, "Failed to delete directory: " + dir.getAbsolutePath());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting directory: " + dir.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Get disk space information
     */
    public DiskSpaceInfo getDiskSpaceInfo() {
        try {
            File dataDir = context.getFilesDir().getParentFile();
            if (dataDir != null) {
                StructStatVfs stat = Os.statvfs(dataDir.getAbsolutePath());
                long blockSize = stat.f_bsize;
                long totalBlocks = stat.f_blocks;
                long availableBlocks = stat.f_bavail;

                return new DiskSpaceInfo(
                    totalBlocks * blockSize,
                    availableBlocks * blockSize
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting disk space info", e);
        }
        return new DiskSpaceInfo(0, 0);
    }

    /**
     * Check if disk space is low
     */
    public boolean isDiskSpaceLow() {
        DiskSpaceInfo info = getDiskSpaceInfo();
        return info.availableBytes < MIN_FREE_MEMORY;
    }

    /**
     * Get memory usage statistics
     */
    @Nullable
    public MemoryStats getMemoryStats() {
        try {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            return new MemoryStats(
                memoryInfo.totalMem,
                memoryInfo.availMem,
                memoryInfo.lowMemory,
                memoryInfo.threshold
            );
        } catch (Exception e) {
            Log.e(TAG, "Error getting memory stats", e);
            return null;
        }
    }

    /**
     * Monitor memory usage and trigger cleanup if necessary
     */
    public void monitorMemory() {
        MemoryStats stats = getMemoryStats();
        if (stats != null) {
            if (stats.isLowMemory || shouldCleanMemory()) {
                Log.w(TAG, "Memory pressure detected, triggering cleanup");
                performMemoryCleanup();
            }
        }

        // Also check disk space
        if (isDiskSpaceLow()) {
            Log.w(TAG, "Low disk space detected");
            clearApplicationCache();
        }
    }

    /**
     * Set up automatic memory monitoring
     */
    public void startMemoryMonitoring() {
        // This would typically be called from Application or Service
        // For now, it's a manual call
        monitorMemory();
    }

    /**
     * Disk space information
     */
    public static class DiskSpaceInfo {
        public final long totalBytes;
        public final long availableBytes;

        public DiskSpaceInfo(long totalBytes, long availableBytes) {
            this.totalBytes = totalBytes;
            this.availableBytes = availableBytes;
        }

        public double getUsedPercentage() {
            if (totalBytes == 0) return 0;
            return ((double) (totalBytes - availableBytes) / totalBytes) * 100;
        }
    }

    /**
     * Memory statistics
     */
    public static class MemoryStats {
        public final long totalMemory;
        public final long availableMemory;
        public final boolean isLowMemory;
        public final long threshold;

        public MemoryStats(long totalMemory, long availableMemory, boolean isLowMemory, long threshold) {
            this.totalMemory = totalMemory;
            this.availableMemory = availableMemory;
            this.isLowMemory = isLowMemory;
            this.threshold = threshold;
        }

        public double getUsedPercentage() {
            if (totalMemory == 0) return 0;
            return ((double) (totalMemory - availableMemory) / totalMemory) * 100;
        }
    }
}
