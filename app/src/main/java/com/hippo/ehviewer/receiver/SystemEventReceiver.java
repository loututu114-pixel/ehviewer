package com.hippo.ehviewer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;
import com.hippo.ehviewer.notification.NotificationManager;
import com.hippo.ehviewer.service.AppKeepAliveService;
import com.hippo.ehviewer.notification.TaskTriggerService;
import androidx.core.app.NotificationCompat;
import android.os.Bundle;

/**
 * 系统事件广播接收器
 * 监听更多系统事件以增强应用拉活能力
 */
public class SystemEventReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SystemEventReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        
        String action = intent.getAction();
        Log.d(TAG, "Received system event: " + action);
        
        switch (action) {
            // 网络状态变化
            case ConnectivityManager.CONNECTIVITY_ACTION:
                handleConnectivityChange(context, intent);
                break;
                
            // 电池状态变化
            case Intent.ACTION_BATTERY_CHANGED:
                handleBatteryChange(context, intent);
                break;
                
            // 充电状态变化
            case Intent.ACTION_POWER_CONNECTED:
                handlePowerConnected(context);
                break;
                
            case Intent.ACTION_POWER_DISCONNECTED:
                handlePowerDisconnected(context);
                break;
                
            // 时区变化
            case Intent.ACTION_TIMEZONE_CHANGED:
                handleTimezoneChanged(context);
                break;
                
            // 时间变化
            case Intent.ACTION_TIME_CHANGED:
                handleTimeChanged(context);
                break;
                
            // 日期变化
            case Intent.ACTION_DATE_CHANGED:
                handleDateChanged(context);
                break;
                
            // 存储卡状态
            case Intent.ACTION_MEDIA_MOUNTED:
            case Intent.ACTION_MEDIA_UNMOUNTED:
                handleMediaStateChange(context, intent);
                break;
                
            // 包安装/卸载
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REMOVED:
            case Intent.ACTION_PACKAGE_REPLACED:
                handlePackageChange(context, intent);
                break;
                
            // 屏幕开关
            case Intent.ACTION_SCREEN_ON:
                handleScreenOn(context);
                break;
                
            case Intent.ACTION_SCREEN_OFF:
                handleScreenOff(context);
                break;
                
            // 用户解锁
            case Intent.ACTION_USER_PRESENT:
                handleUserPresent(context);
                break;
                
            // 电池低电量
            case Intent.ACTION_BATTERY_LOW:
                handleBatteryLow(context);
                break;
                
            // 电池恢复正常
            case Intent.ACTION_BATTERY_OKAY:
                handleBatteryOkay(context);
                break;
                
            // 设备重启
            case Intent.ACTION_REBOOT:
                handleDeviceReboot(context);
                break;
                
            // 语言区域变化
            case Intent.ACTION_LOCALE_CHANGED:
                handleLocaleChanged(context);
                break;
        }
        
        // 每次接收到系统事件都尝试启动保活服务
        ensureKeepAliveService(context);
    }
    
    /**
     * 处理网络连接变化
     */
    private void handleConnectivityChange(Context context, Intent intent) {
        boolean noConnectivity = intent.getBooleanExtra(
            ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        
        if (!noConnectivity) {
            // 网络已连接，发送通知
            sendSystemNotification(context, "网络已连接", "应用服务已恢复正常");
            
            // 启动网络相关服务
            startNetworkServices(context);
        } else {
            // 网络断开
            sendSystemNotification(context, "网络已断开", "应用将在网络恢复后继续服务");
        }
    }
    
    /**
     * 处理电池状态变化
     */
    private void handleBatteryChange(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        
        if (level != -1 && scale != -1) {
            float batteryPct = level * 100 / (float) scale;
            
            // 根据电池电量调整服务策略
            adjustServiceStrategy(context, batteryPct, status);
        }
    }
    
    /**
     * 处理充电连接
     */
    private void handlePowerConnected(Context context) {
        sendSystemNotification(context, "开始充电", "应用服务已优化为充电模式");
        
        // 充电时可以更积极地保活
        enableAggressiveKeepAlive(context);
    }
    
    /**
     * 处理充电断开
     */
    private void handlePowerDisconnected(Context context) {
        sendSystemNotification(context, "停止充电", "应用服务已切换为节能模式");
        
        // 断开充电时切换为节能模式
        enablePowerSavingMode(context);
    }
    
    /**
     * 处理时区变化
     */
    private void handleTimezoneChanged(Context context) {
        // 时区改变可能影响定时任务，重新初始化
        restartTimerServices(context);
    }
    
    /**
     * 处理时间变化
     */
    private void handleTimeChanged(Context context) {
        // 系统时间变化，重新校准定时任务
        recalibrateTimers(context);
    }
    
    /**
     * 处理日期变化
     */
    private void handleDateChanged(Context context) {
        // 日期变化，触发每日任务
        triggerDailyTasks(context);
        sendSystemNotification(context, "新的一天", "EhViewer为您服务新的一天！");
    }
    
    /**
     * 处理存储媒体状态变化
     */
    private void handleMediaStateChange(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            // 存储卡挂载，扫描新文件
            scanForNewFiles(context);
        }
    }
    
    /**
     * 处理包安装/卸载
     */
    private void handlePackageChange(Context context, Intent intent) {
        String packageName = intent.getData() != null ? 
            intent.getData().getSchemeSpecificPart() : null;
            
        if (packageName != null) {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                Log.d(TAG, "Package installed: " + packageName);
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                Log.d(TAG, "Package removed: " + packageName);
            }
        }
    }
    
    /**
     * 处理屏幕开启
     */
    private void handleScreenOn(Context context) {
        // 屏幕开启时增强保活
        enhanceKeepAlive(context);
    }
    
    /**
     * 处理屏幕关闭
     */
    private void handleScreenOff(Context context) {
        // 屏幕关闭时确保后台服务正常运行
        ensureBackgroundServices(context);
    }
    
    /**
     * 处理用户解锁
     */
    private void handleUserPresent(Context context) {
        // 用户解锁设备，这是一个很好的拉活时机
        sendWelcomeNotification(context);
        startAllServices(context);
    }
    
    /**
     * 处理电池低电量
     */
    private void handleBatteryLow(Context context) {
        // 电池低电量时切换为极致节能模式
        enableUltraPowerSavingMode(context);
    }
    
    /**
     * 处理电池恢复正常
     */
    private void handleBatteryOkay(Context context) {
        // 电池恢复正常，恢复正常服务
        restoreNormalMode(context);
    }
    
    /**
     * 处理设备重启
     */
    private void handleDeviceReboot(Context context) {
        // 设备即将重启，保存重要状态
        saveImportantState(context);
    }
    
    /**
     * 处理语言区域变化
     */
    private void handleLocaleChanged(Context context) {
        // 语言变化，更新本地化内容
        updateLocalizedContent(context);
    }
    
    /**
     * 确保保活服务运行
     */
    private void ensureKeepAliveService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, AppKeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start keep alive service", e);
        }
    }
    
    /**
     * 发送系统通知
     */
    private void sendSystemNotification(Context context, String title, String message) {
        try {
            NotificationManager notificationManager = NotificationManager.getInstance(context);
            
            NotificationManager.NotificationData data = 
                new NotificationManager.NotificationData(title, message);
            
            data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setBigText(message);
            
            notificationManager.showNotification(data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send system notification", e);
        }
    }
    
    /**
     * 启动网络相关服务
     */
    private void startNetworkServices(Context context) {
        // 启动网络监控服务等
        Intent intent = new Intent(context, TaskTriggerService.class);
        context.startService(intent);
    }
    
    /**
     * 根据电池状态调整服务策略
     */
    private void adjustServiceStrategy(Context context, float batteryLevel, int chargingStatus) {
        if (batteryLevel < 15) {
            // 低电量模式
            enablePowerSavingMode(context);
        } else if (batteryLevel > 80) {
            // 电量充足，可以更积极
            enableAggressiveKeepAlive(context);
        }
        
        // 保存电池状态供其他组件使用
        context.getSharedPreferences("system_state", Context.MODE_PRIVATE)
            .edit()
            .putFloat("battery_level", batteryLevel)
            .putInt("charging_status", chargingStatus)
            .putLong("last_battery_update", System.currentTimeMillis())
            .apply();
    }
    
    /**
     * 启用积极保活模式
     */
    private void enableAggressiveKeepAlive(Context context) {
        // 发送广播给保活服务，切换为积极模式
        Intent intent = new Intent("com.hippo.ehviewer.ACTION_AGGRESSIVE_MODE");
        context.sendBroadcast(intent);
    }
    
    /**
     * 启用节能模式
     */
    private void enablePowerSavingMode(Context context) {
        Intent intent = new Intent("com.hippo.ehviewer.ACTION_POWER_SAVING_MODE");
        context.sendBroadcast(intent);
    }
    
    /**
     * 启用极致节能模式
     */
    private void enableUltraPowerSavingMode(Context context) {
        Intent intent = new Intent("com.hippo.ehviewer.ACTION_ULTRA_POWER_SAVING_MODE");
        context.sendBroadcast(intent);
    }
    
    /**
     * 恢复正常模式
     */
    private void restoreNormalMode(Context context) {
        Intent intent = new Intent("com.hippo.ehviewer.ACTION_NORMAL_MODE");
        context.sendBroadcast(intent);
    }
    
    /**
     * 重启定时服务
     */
    private void restartTimerServices(Context context) {
        // 重启定时相关的服务
    }
    
    /**
     * 重新校准定时器
     */
    private void recalibrateTimers(Context context) {
        // 重新校准所有定时任务
    }
    
    /**
     * 触发每日任务
     */
    private void triggerDailyTasks(Context context) {
        sendWelcomeNotification(context);
        // 执行每日清理、统计等任务
    }
    
    /**
     * 扫描新文件
     */
    private void scanForNewFiles(Context context) {
        // 扫描新挂载存储中的文件
    }
    
    /**
     * 增强保活
     */
    private void enhanceKeepAlive(Context context) {
        ensureKeepAliveService(context);
    }
    
    /**
     * 确保后台服务
     */
    private void ensureBackgroundServices(Context context) {
        ensureKeepAliveService(context);
    }
    
    /**
     * 启动所有服务
     */
    private void startAllServices(Context context) {
        ensureKeepAliveService(context);
        startNetworkServices(context);
    }
    
    /**
     * 发送欢迎通知
     */
    private void sendWelcomeNotification(Context context) {
        String[] welcomeMessages = {
            "欢迎回来！EhViewer为您服务",
            "新的开始！发现更多精彩内容",
            "Hello！今天要看什么呢？",
            "回来了！您的专属浏览器随时待命"
        };
        
        int randomIndex = (int) (Math.random() * welcomeMessages.length);
        sendSystemNotification(context, "EhViewer", welcomeMessages[randomIndex]);
    }
    
    /**
     * 保存重要状态
     */
    private void saveImportantState(Context context) {
        // 保存当前状态到持久化存储
        context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_shutdown", System.currentTimeMillis())
            .putBoolean("clean_shutdown", true)
            .apply();
    }
    
    /**
     * 更新本地化内容
     */
    private void updateLocalizedContent(Context context) {
        // 语言变化时更新本地化内容
    }
}