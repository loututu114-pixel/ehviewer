package com.hippo.ehviewer.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.RequiresApi;
import com.hippo.ehviewer.notification.NotificationManager;
import androidx.core.app.NotificationCompat;
import android.os.Bundle;

/**
 * 电池优化白名单管理器
 * 智能检测和申请电池优化白名单，提高应用保活成功率
 */
public class BatteryOptimizationManager {
    
    private static final String TAG = "BatteryOptimizationManager";
    private static final String PREFS_NAME = "battery_optimization";
    private static final String KEY_LAST_REQUEST_TIME = "last_request_time";
    private static final String KEY_USER_DENIED_COUNT = "user_denied_count";
    private static final String KEY_WHITELIST_ACHIEVED = "whitelist_achieved";
    
    // 请求间隔（7天）
    private static final long REQUEST_INTERVAL = 7 * 24 * 60 * 60 * 1000L;
    private static final int MAX_DENIAL_COUNT = 3;
    
    private final Context context;
    private final NotificationManager notificationManager;
    
    public BatteryOptimizationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManager.getInstance(context);
    }
    
    /**
     * 检查是否已在电池优化白名单中
     */
    public boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager != null && 
                   powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true; // Android 6.0以下版本不需要处理
    }
    
    /**
     * 智能请求电池优化白名单
     * 根据用户之前的行为智能决定是否显示请求
     */
    public void smartRequestWhitelistIfNeeded() {
        if (isIgnoringBatteryOptimizations()) {
            markWhitelistAchieved();
            return;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        
        var prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastRequestTime = prefs.getLong(KEY_LAST_REQUEST_TIME, 0);
        int deniedCount = prefs.getInt(KEY_USER_DENIED_COUNT, 0);
        boolean whitelistAchieved = prefs.getBoolean(KEY_WHITELIST_ACHIEVED, false);
        
        // 如果已经获得白名单但现在检测到没有，说明用户手动关闭了
        if (whitelistAchieved) {
            sendWhitelistRevokedNotification();
            resetWhitelistStatus();
            return;
        }
        
        // 如果用户拒绝次数过多，降低请求频率
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (deniedCount >= MAX_DENIAL_COUNT) {
            // 用户多次拒绝，使用更长的间隔（30天）
            if (timeSinceLastRequest < REQUEST_INTERVAL * 4) {
                return;
            }
        } else if (timeSinceLastRequest < REQUEST_INTERVAL) {
            // 正常间隔检查
            return;
        }
        
        // 发送通知请求用户设置白名单
        sendWhitelistRequestNotification();
        
        // 更新请求时间
        prefs.edit()
            .putLong(KEY_LAST_REQUEST_TIME, currentTime)
            .apply();
    }
    
    /**
     * 请求电池优化白名单（由Activity调用）
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestBatteryOptimizationWhitelist(Activity activity) {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                activity.startActivityForResult(intent, 1001);
                
                // 记录请求
                recordWhitelistRequest();
            } else {
                // 如果不支持直接请求，引导用户手动设置
                openBatteryOptimizationSettings(activity);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to request battery optimization whitelist", e);
            openBatteryOptimizationSettings(activity);
        }
    }
    
    /**
     * 打开电池优化设置页面
     */
    public void openBatteryOptimizationSettings(Activity activity) {
        try {
            Intent intent = new Intent();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                
                if (intent.resolveActivity(context.getPackageManager()) == null) {
                    // 如果不支持，尝试打开应用详情页
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                }
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            }
            
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                activity.startActivity(intent);
                sendManualSettingGuideNotification();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open battery optimization settings", e);
        }
    }
    
    /**
     * 处理白名单请求结果
     */
    public void handleWhitelistRequestResult(boolean granted) {
        var prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        var editor = prefs.edit();
        
        if (granted) {
            // 用户同意了白名单
            markWhitelistAchieved();
            sendWhitelistSuccessNotification();
            
            // 重置拒绝计数
            editor.putInt(KEY_USER_DENIED_COUNT, 0);
        } else {
            // 用户拒绝了白名单
            int deniedCount = prefs.getInt(KEY_USER_DENIED_COUNT, 0) + 1;
            editor.putInt(KEY_USER_DENIED_COUNT, deniedCount);
            
            if (deniedCount == 1) {
                sendFirstDenialEducationNotification();
            } else if (deniedCount >= MAX_DENIAL_COUNT) {
                sendFinalEducationNotification();
            }
        }
        
        editor.apply();
    }
    
    /**
     * 标记已获得白名单
     */
    private void markWhitelistAchieved() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WHITELIST_ACHIEVED, true)
            .putInt(KEY_USER_DENIED_COUNT, 0)
            .apply();
    }
    
    /**
     * 重置白名单状态
     */
    private void resetWhitelistStatus() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WHITELIST_ACHIEVED, false)
            .apply();
    }
    
    /**
     * 记录白名单请求
     */
    private void recordWhitelistRequest() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_REQUEST_TIME, System.currentTimeMillis())
            .apply();
    }
    
    /**
     * 发送白名单请求通知
     */
    private void sendWhitelistRequestNotification() {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "提升应用性能",
                "点击优化EhViewer后台运行"
            );
        
        Bundle extras = new Bundle();
        extras.putString("action", "request_whitelist");
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setExtras(extras)
            .setBigText("为了确保EhViewer能够在后台正常运行并及时推送通知，建议将应用添加到电池优化白名单。这不会显著影响电池寿命，但能大幅提升应用体验。");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送白名单被撤销通知
     */
    private void sendWhitelistRevokedNotification() {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "后台运行受限",
                "EhViewer后台运行可能受到限制"
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setBigText("检测到应用已从电池优化白名单中移除，这可能影响后台功能和通知推送。点击重新设置白名单。");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送白名单设置成功通知
     */
    private void sendWhitelistSuccessNotification() {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "优化完成",
                "EhViewer已获得最佳运行权限"
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setBigText("感谢您的设置！EhViewer现在可以在后台稳定运行，为您提供更好的浏览体验。");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送首次拒绝教育通知
     */
    private void sendFirstDenialEducationNotification() {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "了解后台运行",
                "为什么EhViewer需要后台运行权限？"
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setBigText("EhViewer需要在后台运行以便：\n• 及时推送重要通知\n• 监控下载进度\n• 保持浏览历史同步\n• 提供更流畅的启动体验\n\n这不会显著影响电池寿命。");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送最终教育通知
     */
    private void sendFinalEducationNotification() {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "功能可能受限",
                "应用将以受限模式运行"
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setBigText("由于未获得后台运行权限，EhViewer的部分功能可能受到限制：\n• 通知推送延迟\n• 后台下载中断\n• 启动速度变慢\n\n如需最佳体验，请考虑在设置中启用后台运行。");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送手动设置指导通知
     */
    private void sendManualSettingGuideNotification() {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "设置指导",
                "手动优化后台运行"
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setBigText("请在打开的设置页面中：\n1. 找到\"电池优化\"或\"后台应用刷新\"\n2. 搜索\"EhViewer\"\n3. 选择\"不优化\"或\"允许\"\n4. 返回应用即可");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 获取厂商特定的电池优化设置方法
     */
    public String getManufacturerSpecificGuide() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        
        switch (manufacturer) {
            case "huawei":
                return "华为设备：设置 → 应用 → 应用启动管理 → EhViewer → 手动管理 → 允许自启动、关联启动、后台活动";
                
            case "xiaomi":
                return "小米设备：设置 → 应用设置 → 应用管理 → EhViewer → 省电策略 → 无限制";
                
            case "oppo":
                return "OPPO设备：设置 → 电池 → 应用耗电管理 → EhViewer → 允许后台运行";
                
            case "vivo":
                return "vivo设备：设置 → 电池 → 后台应用管理 → EhViewer → 允许后台高耗电";
                
            case "samsung":
                return "三星设备：设置 → 设备保养 → 电池 → 应用程序功耗监控 → EhViewer → 不受监控";
                
            case "oneplus":
                return "一加设备：设置 → 电池 → 电池优化 → 应用 → EhViewer → 不优化";
                
            default:
                return "请在电池优化或应用管理中找到EhViewer，设置为\"不优化\"或\"允许后台运行\"";
        }
    }
    
    /**
     * 检测设备制造商并提供针对性建议
     */
    public void sendManufacturerSpecificGuide() {
        String guide = getManufacturerSpecificGuide();
        
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "设备优化建议",
                "针对您的" + Build.MANUFACTURER + "设备"
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setBigText(guide);
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 获取白名单状态统计信息
     */
    public WhitelistStatus getWhitelistStatus() {
        var prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        return new WhitelistStatus(
            isIgnoringBatteryOptimizations(),
            prefs.getBoolean(KEY_WHITELIST_ACHIEVED, false),
            prefs.getInt(KEY_USER_DENIED_COUNT, 0),
            prefs.getLong(KEY_LAST_REQUEST_TIME, 0)
        );
    }
    
    /**
     * 白名单状态信息
     */
    public static class WhitelistStatus {
        public final boolean isCurrentlyWhitelisted;
        public final boolean wasEverWhitelisted;
        public final int denialCount;
        public final long lastRequestTime;
        
        public WhitelistStatus(boolean isCurrentlyWhitelisted, boolean wasEverWhitelisted, 
                             int denialCount, long lastRequestTime) {
            this.isCurrentlyWhitelisted = isCurrentlyWhitelisted;
            this.wasEverWhitelisted = wasEverWhitelisted;
            this.denialCount = denialCount;
            this.lastRequestTime = lastRequestTime;
        }
        
        public boolean shouldShowRequest() {
            return !isCurrentlyWhitelisted && 
                   denialCount < MAX_DENIAL_COUNT &&
                   (System.currentTimeMillis() - lastRequestTime) > REQUEST_INTERVAL;
        }
    }
}