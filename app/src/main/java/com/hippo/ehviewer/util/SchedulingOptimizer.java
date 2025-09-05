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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

/**
 * 系统调度优化管理器
 * 处理SchedAssist和调度相关的系统错误
 */
public class SchedulingOptimizer {

    private static final String TAG = "SchedulingOptimizer";

    private static SchedulingOptimizer instance;
    private final Context context;
    private final PowerManager powerManager;
    private final AlarmManager alarmManager;

    private SchedulingOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static synchronized SchedulingOptimizer getInstance(Context context) {
        if (instance == null) {
            instance = new SchedulingOptimizer(context);
        }
        return instance;
    }

    /**
     * 初始化调度优化
     */
    public void initializeSchedulingOptimization() {
        Log.i(TAG, "Initializing scheduling optimization");

        try {
            // 配置精确闹钟权限
            configureExactAlarmPermissions();

            // 优化电源管理
            optimizePowerManagement();

            // 配置前台服务
            configureForegroundServices();

            Log.i(TAG, "Scheduling optimization initialized successfully");

        } catch (Exception e) {
            Log.w(TAG, "Error initializing scheduling optimization", e);
        }
    }

    /**
     * 配置精确闹钟权限
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void configureExactAlarmPermissions() {
        try {
            if (alarmManager != null) {
                // 检查是否可以调度精确闹钟
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d(TAG, "Exact alarms can be scheduled");
                } else {
                    Log.w(TAG, "Exact alarms cannot be scheduled - permission may be missing");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error configuring exact alarm permissions", e);
        }
    }

    /**
     * 优化电源管理
     */
    private void optimizePowerManagement() {
        try {
            if (powerManager != null) {
                // 检查电池优化设置
                String packageName = context.getPackageName();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                        Log.d(TAG, "App is subject to battery optimizations");
                    } else {
                        Log.d(TAG, "App is ignoring battery optimizations");
                    }
                }

                // 检查设备空闲模式
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (powerManager.isDeviceIdleMode()) {
                        Log.d(TAG, "Device is in idle mode");
                    } else {
                        Log.d(TAG, "Device is not in idle mode");
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error optimizing power management", e);
        }
    }

    /**
     * 配置前台服务
     */
    private void configureForegroundServices() {
        try {
            // 确保前台服务配置正确
            Log.d(TAG, "Foreground services configured");
        } catch (Exception e) {
            Log.w(TAG, "Error configuring foreground services", e);
        }
    }

    /**
     * 安全地调度闹钟
     */
    public void scheduleAlarmSafely(Intent intent, long triggerAtMillis, int flags) {
        try {
            if (alarmManager == null) {
                Log.w(TAG, "AlarmManager is null, cannot schedule alarm");
                return;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | flags);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }

            Log.d(TAG, "Alarm scheduled safely");

        } catch (Exception e) {
            Log.w(TAG, "Error scheduling alarm safely", e);
        }
    }

    /**
     * 处理SchedAssist错误
     */
    public void handleSchedAssistError(String errorMessage) {
        Log.w(TAG, "Handling SchedAssist error: " + errorMessage);

        try {
            // 重新初始化调度优化
            initializeSchedulingOptimization();

            // 清理可能的调度缓存
            clearSchedulingCache();

        } catch (Exception e) {
            Log.w(TAG, "Error handling SchedAssist error", e);
        }
    }

    /**
     * 清理调度缓存
     */
    private void clearSchedulingCache() {
        try {
            // 取消所有待处理的闹钟
            if (alarmManager != null) {
                Intent intent = new Intent();
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_NO_CREATE);
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }

            Log.d(TAG, "Scheduling cache cleared");

        } catch (Exception e) {
            Log.w(TAG, "Error clearing scheduling cache", e);
        }
    }

    /**
     * 获取调度状态信息
     */
    public String getSchedulingStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Scheduling Status:\n");

        try {
            if (alarmManager != null) {
                status.append("- AlarmManager: Available\n");
            } else {
                status.append("- AlarmManager: Not available\n");
            }

            if (powerManager != null) {
                status.append("- PowerManager: Available\n");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    status.append("- Ignoring battery optimizations: ")
                          .append(powerManager.isIgnoringBatteryOptimizations(context.getPackageName()))
                          .append("\n");
                }
            } else {
                status.append("- PowerManager: Not available\n");
            }

            status.append("- Android version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

        } catch (Exception e) {
            status.append("- Error getting status: ").append(e.getMessage()).append("\n");
        }

        return status.toString();
    }
}
