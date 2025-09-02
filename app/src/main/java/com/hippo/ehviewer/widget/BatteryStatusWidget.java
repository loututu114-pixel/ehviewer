package com.hippo.ehviewer.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.notification.NotificationManager;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 电池状态桌面小部件
 * 显示电池电量、充电状态、温度、健康度，提供电源管理建议
 */
public class BatteryStatusWidget extends BaseEhWidget {
    
    private static final String TAG = "BatteryStatusWidget";
    
    // 自定义Actions
    private static final String ACTION_BATTERY_SETTINGS = "com.hippo.ehviewer.widget.BATTERY_SETTINGS";
    private static final String ACTION_POWER_SAVER = "com.hippo.ehviewer.widget.POWER_SAVER";
    private static final String ACTION_BATTERY_OPTIMIZATION = "com.hippo.ehviewer.widget.BATTERY_OPTIMIZATION";
    
    // 电池状态监听器
    private static BatteryReceiver batteryReceiver;
    
    // 电池历史记录
    private static final String PREFS_NAME = "battery_widget";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        
        // 注册电池状态监听
        registerBatteryReceiver(context);
        
        // 启动定时更新
        startPeriodicUpdates(context);
    }
    
    @Override
    protected RemoteViews createRemoteViews(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_battery_status);
        
        // 获取当前电池状态
        Intent batteryStatus = getBatteryStatus(context);
        
        // 更新电池信息显示
        updateBatteryInfo(context, views, batteryStatus);
        
        // 更新充电状态
        updateChargingStatus(context, views, batteryStatus);
        
        // 更新电池健康和温度
        updateBatteryHealth(context, views, batteryStatus);
        
        // 更新电源管理建议
        updatePowerRecommendations(context, views, batteryStatus);
        
        return views;
    }
    
    @Override
    protected void setupCustomClickActions(Context context, RemoteViews views, int appWidgetId) {
        // 点击电池图标打开电池设置
        Bundle batterySettingsExtras = new Bundle();
        batterySettingsExtras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.battery_icon,
            createClickPendingIntent(context, appWidgetId * 10, "battery_settings", batterySettingsExtras));
        
        // 点击省电模式按钮
        Bundle powerSaverExtras = new Bundle();
        powerSaverExtras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.power_saver_button,
            createClickPendingIntent(context, appWidgetId * 10 + 1, "power_saver", powerSaverExtras));
        
        // 点击充电状态查看电源管理
        views.setOnClickPendingIntent(R.id.charging_status,
            createBrowserPendingIntent(context, appWidgetId * 10 + 2,
                "https://support.google.com/android/answer/9079661", "Android电源管理"));
        
        // 点击温度查看电池健康建议
        views.setOnClickPendingIntent(R.id.battery_temperature,
            createBrowserPendingIntent(context, appWidgetId * 10 + 3,
                "https://support.google.com/android/answer/7664358", "电池健康建议"));
    }
    
    @Override
    protected void handleCustomAction(Context context, Intent intent, String action) {
        switch (action) {
            case ACTION_BATTERY_SETTINGS:
                openBatterySettings(context);
                break;
                
            case ACTION_POWER_SAVER:
                togglePowerSaver(context);
                break;
                
            case ACTION_BATTERY_OPTIMIZATION:
                openBatteryOptimization(context);
                break;
        }
    }
    
    @Override
    protected void handleWidgetClick(Context context, Intent intent) {
        String clickType = intent.getStringExtra("click_type");
        
        switch (clickType) {
            case "battery_settings":
                openBatterySettings(context);
                break;
                
            case "power_saver":
                togglePowerSaver(context);
                break;
                
            case "browser":
                super.handleWidgetClick(context, intent);
                break;
                
            default:
                // 默认打开电池设置
                openBatterySettings(context);
                break;
        }
    }
    
    /**
     * 获取电池状态
     */
    private Intent getBatteryStatus(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        return context.registerReceiver(null, ifilter);
    }
    
    /**
     * 更新电池信息显示
     */
    private void updateBatteryInfo(Context context, RemoteViews views, Intent batteryStatus) {
        if (batteryStatus == null) return;
        
        // 获取电池电量
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        
        if (level != -1 && scale != -1) {
            float batteryPct = level * 100 / (float) scale;
            
            // 更新电量显示
            views.setTextViewText(R.id.battery_percentage, String.format("%.0f%%", batteryPct));
            
            // 更新电池图标
            updateBatteryIcon(views, (int) batteryPct, batteryStatus);
            
            // 更新进度条
            views.setProgressBar(R.id.battery_progress, 100, (int) batteryPct, false);
            
            // 保存电池历史
            saveBatteryHistory(context, (int) batteryPct);
            
            // 计算剩余时间
            String remainingTime = calculateRemainingTime(context, (int) batteryPct, batteryStatus);
            views.setTextViewText(R.id.remaining_time, remainingTime);
        }
    }
    
    /**
     * 更新充电状态
     */
    private void updateChargingStatus(Context context, RemoteViews views, Intent batteryStatus) {
        if (batteryStatus == null) return;
        
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        
        String chargingText = "";
        String chargingDetails = "";
        
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                chargingText = "充电中";
                chargingDetails = getChargingTypeText(plugged);
                break;
                
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                chargingText = "放电中";
                chargingDetails = "未连接电源";
                break;
                
            case BatteryManager.BATTERY_STATUS_FULL:
                chargingText = "已充满";
                chargingDetails = "可拔出充电器";
                break;
                
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                chargingText = "未充电";
                chargingDetails = "已连接但未充电";
                break;
                
            default:
                chargingText = "状态未知";
                chargingDetails = "";
                break;
        }
        
        views.setTextViewText(R.id.charging_status, chargingText);
        views.setTextViewText(R.id.charging_details, chargingDetails);
        
        // 检查充电异常
        checkChargingAnomalies(context, status, plugged);
    }
    
    /**
     * 更新电池健康和温度
     */
    private void updateBatteryHealth(Context context, RemoteViews views, Intent batteryStatus) {
        if (batteryStatus == null) return;
        
        // 电池温度
        int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        if (temperature != -1) {
            float tempCelsius = temperature / 10.0f;
            views.setTextViewText(R.id.battery_temperature, 
                String.format("温度: %.1f°C", tempCelsius));
            
            // 温度警告
            if (tempCelsius > 45) {
                views.setTextColor(R.id.battery_temperature, 
                    context.getColor(android.R.color.holo_red_dark));
                showTemperatureWarning(context, tempCelsius);
            } else if (tempCelsius > 35) {
                views.setTextColor(R.id.battery_temperature, 
                    context.getColor(android.R.color.holo_orange_dark));
            } else {
                views.setTextColor(R.id.battery_temperature, 
                    context.getColor(android.R.color.primary_text_light));
            }
        }
        
        // 电池健康状态
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        String healthText = getBatteryHealthText(health);
        views.setTextViewText(R.id.battery_health, "健康度: " + healthText);
        
        // 电压信息
        int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        if (voltage != -1) {
            views.setTextViewText(R.id.battery_voltage, 
                String.format("电压: %.2fV", voltage / 1000.0f));
        }
    }
    
    /**
     * 更新电源管理建议
     */
    private void updatePowerRecommendations(Context context, RemoteViews views, Intent batteryStatus) {
        if (batteryStatus == null) return;
        
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        
        if (level != -1 && scale != -1) {
            float batteryPct = level * 100 / (float) scale;
            String recommendation = getPowerRecommendation(batteryPct, status);
            views.setTextViewText(R.id.power_recommendation, recommendation);
            
            // 更新省电模式按钮状态
            boolean isPowerSaveMode = isPowerSaveModeEnabled(context);
            views.setTextViewText(R.id.power_saver_button, 
                isPowerSaveMode ? "退出省电" : "省电模式");
            views.setInt(R.id.power_saver_button, "setBackgroundResource",
                isPowerSaveMode ? R.drawable.button_power_save_on : R.drawable.button_power_save_off);
        }
    }
    
    /**
     * 更新电池图标
     */
    private void updateBatteryIcon(RemoteViews views, int batteryLevel, Intent batteryStatus) {
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        
        int iconResource;
        if (isCharging) {
            iconResource = R.drawable.ic_battery_charging;
        } else if (batteryLevel >= 90) {
            iconResource = R.drawable.ic_battery_full;
        } else if (batteryLevel >= 60) {
            iconResource = R.drawable.ic_battery_60;
        } else if (batteryLevel >= 30) {
            iconResource = R.drawable.ic_battery_30;
        } else if (batteryLevel >= 15) {
            iconResource = R.drawable.ic_battery_alert;
        } else {
            iconResource = R.drawable.ic_battery_critical;
        }
        
        views.setInt(R.id.battery_icon, "setImageResource", iconResource);
        
        // 低电量时设置红色
        if (batteryLevel < 15 && !isCharging) {
            views.setInt(R.id.battery_percentage, "setTextColor", 
                android.graphics.Color.RED);
        } else {
            views.setInt(R.id.battery_percentage, "setTextColor", 
                android.graphics.Color.BLACK);
        }
    }
    
    /**
     * 获取充电类型文本
     */
    private String getChargingTypeText(int plugged) {
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "交流电充电";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "USB充电";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "无线充电";
            default:
                return "充电中";
        }
    }
    
    /**
     * 获取电池健康状态文本
     */
    private String getBatteryHealthText(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "良好";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "过热";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "损坏";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "电压过高";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "故障";
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "过冷";
            default:
                return "未知";
        }
    }
    
    /**
     * 获取电源管理建议
     */
    private String getPowerRecommendation(float batteryLevel, int status) {
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        
        if (batteryLevel < 10 && !isCharging) {
            return "电量极低，请立即充电";
        } else if (batteryLevel < 20 && !isCharging) {
            return "电量较低，建议充电";
        } else if (batteryLevel > 95 && isCharging) {
            return "电量充足，可拔出充电器";
        } else if (batteryLevel > 80 && isCharging) {
            return "即将充满，避免过充";
        } else if (isCharging) {
            return "充电中，请耐心等待";
        } else {
            return "电量正常，正常使用";
        }
    }
    
    /**
     * 计算剩余时间
     */
    private String calculateRemainingTime(Context context, int currentLevel, Intent batteryStatus) {
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        
        // 从历史记录计算电池使用/充电速度
        long[] history = getBatteryHistory(context);
        if (history.length < 2) {
            return isCharging ? "计算充电时间中..." : "计算使用时间中...";
        }
        
        // 简单的线性预测（实际应用中可以使用更复杂的算法）
        long timeDiff = history[1] - history[0]; // 时间差
        if (timeDiff <= 0) return "计算中...";
        
        if (isCharging) {
            int remainingToCharge = 100 - currentLevel;
            if (remainingToCharge <= 0) {
                return "已充满";
            }
            
            // 假设充电速度相对稳定
            long estimatedTime = (remainingToCharge * timeDiff) / 10; // 假设每10分钟充10%
            return "约" + formatDuration(estimatedTime) + "充满";
        } else {
            // 预测放电时间
            if (currentLevel <= 0) {
                return "电量耗尽";
            }
            
            long estimatedTime = (currentLevel * timeDiff) / 5; // 假设每5分钟耗电1%
            return "约可使用" + formatDuration(estimatedTime);
        }
    }
    
    /**
     * 格式化持续时间
     */
    private String formatDuration(long milliseconds) {
        long hours = milliseconds / (1000 * 60 * 60);
        long minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
    
    /**
     * 保存电池历史记录
     */
    private void saveBatteryHistory(Context context, int batteryLevel) {
        long currentTime = System.currentTimeMillis();
        
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("last_battery_level", batteryLevel)
            .putLong("last_update_time", currentTime)
            .apply();
    }
    
    /**
     * 获取电池历史记录
     */
    private long[] getBatteryHistory(Context context) {
        long lastTime = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong("last_update_time", 0);
        long currentTime = System.currentTimeMillis();
        
        return new long[]{lastTime, currentTime};
    }
    
    /**
     * 检查充电异常
     */
    private void checkChargingAnomalies(Context context, int status, int plugged) {
        // 检测是否长时间充电但电量不增加
        if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING && plugged > 0) {
            showChargingAnomalyNotification(context, "充电异常", 
                "设备已连接电源但未充电，可能是充电器或接口问题");
        }
    }
    
    /**
     * 显示温度警告
     */
    private void showTemperatureWarning(Context context, float temperature) {
        if (temperature > 50) {
            sendNotification(context, "电池温度过高", 
                String.format("当前温度%.1f°C，请停止使用并让设备冷却", temperature));
        } else if (temperature > 45) {
            sendNotification(context, "电池温度偏高", 
                String.format("当前温度%.1f°C，建议减少使用或关闭部分应用", temperature));
        }
    }
    
    /**
     * 显示充电异常通知
     */
    private void showChargingAnomalyNotification(Context context, String title, String message) {
        NotificationManager notificationManager = NotificationManager.getInstance(context);
        
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(title, message);
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setBigText(message + " 点击查看电池健康建议");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 开关省电模式
     */
    private void togglePowerSaver(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0+ 打开省电模式设置
            Intent intent = new Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                context.startActivity(intent);
                sendNotification(context, "省电模式", "请在设置中开启或关闭省电模式");
            } catch (Exception e) {
                Log.e(TAG, "Failed to open battery saver settings", e);
                openBatterySettings(context);
            }
        } else {
            openBatterySettings(context);
        }
    }
    
    /**
     * 检查省电模式是否启用
     */
    private boolean isPowerSaveModeEnabled(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.os.PowerManager powerManager = 
                (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isPowerSaveMode();
        }
        return false;
    }
    
    /**
     * 打开电池设置
     */
    private void openBatterySettings(Context context) {
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open battery settings", e);
            // 降级到通用设置
            try {
                Intent settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settingsIntent);
            } catch (Exception e2) {
                Log.e(TAG, "Failed to open settings", e2);
            }
        }
    }
    
    /**
     * 打开电池优化设置
     */
    private void openBatteryOptimization(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open battery optimization settings", e);
                openBatterySettings(context);
            }
        } else {
            openBatterySettings(context);
        }
    }
    
    /**
     * 注册电池状态监听
     */
    private void registerBatteryReceiver(Context context) {
        if (batteryReceiver == null) {
            batteryReceiver = new BatteryReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            filter.addAction(Intent.ACTION_BATTERY_LOW);
            filter.addAction(Intent.ACTION_BATTERY_OKAY);
            
            try {
                context.registerReceiver(batteryReceiver, filter);
            } catch (Exception e) {
                Log.e(TAG, "Failed to register battery receiver", e);
            }
        }
    }
    
    /**
     * 启动定时更新
     */
    private void startPeriodicUpdates(Context context) {
        // 每30秒更新一次电池状态
        widgetExecutor.scheduleAtFixedRate(() -> {
            forceUpdateAllWidgets(context);
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 发送通知
     */
    private void sendNotification(Context context, String title, String message) {
        NotificationManager notificationManager = NotificationManager.getInstance(context);
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(title, message);
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 电池状态监听器
     */
    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                Log.d(TAG, "Battery state changed: " + action);
                
                // 延迟1秒更新，确保状态稳定
                widgetExecutor.schedule(() -> {
                    forceUpdateAllWidgets(context);
                }, 1, TimeUnit.SECONDS);
                
                // 处理特殊事件
                switch (action) {
                    case Intent.ACTION_POWER_CONNECTED:
                        sendNotification(context, "开始充电", "电源已连接");
                        break;
                        
                    case Intent.ACTION_POWER_DISCONNECTED:
                        sendNotification(context, "停止充电", "电源已断开");
                        break;
                        
                    case Intent.ACTION_BATTERY_LOW:
                        sendNotification(context, "电量不足", "请及时充电");
                        break;
                        
                    case Intent.ACTION_BATTERY_OKAY:
                        sendNotification(context, "电量恢复", "电量已恢复正常");
                        break;
                }
            }
        }
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        
        // 注销电池监听器
        if (batteryReceiver != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
                batteryReceiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister battery receiver", e);
            }
        }
    }
}