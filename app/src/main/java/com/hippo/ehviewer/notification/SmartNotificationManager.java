package com.hippo.ehviewer.notification;

import android.content.Context;
import android.util.Log;

/**
 * 智能通知管理器
 * 提供智能的通知管理功能，包括优化通知显示、批量处理等
 */
public class SmartNotificationManager {

    private static final String TAG = "SmartNotificationManager";
    private static SmartNotificationManager instance;
    private final Context context;
    private final NotificationManager notificationManager;

    public static synchronized SmartNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new SmartNotificationManager(context);
        }
        return instance;
    }

    private SmartNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManager.getInstance(context);
    }

    /**
     * 显示优化完成通知（无参数版本）
     */
    public void showOptimizationComplete() {
        showOptimizationCompleted("系统优化已完成，性能得到提升");
    }

    /**
     * 显示优化进度通知
     */
    public void showOptimizationInProgress(String message) {
        try {
            NotificationManager.NotificationData data =
                new NotificationManager.NotificationData(
                    "系统优化进行中",
                    message != null ? message : "正在优化系统性能..."
                );

            data.setType(NotificationManager.NotificationType.SYSTEM_ALERT);
            data.setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW);

            notificationManager.showNotification(data);
            Log.d(TAG, "Optimization progress notification shown: " + message);

        } catch (Exception e) {
            Log.e(TAG, "Failed to show optimization notification", e);
        }
    }

    /**
     * 显示优化完成通知
     */
    public void showOptimizationCompleted(String message) {
        try {
            NotificationManager.NotificationData data =
                new NotificationManager.NotificationData(
                    "系统优化完成",
                    message != null ? message : "系统优化已完成，性能得到提升"
                );

            data.setType(NotificationManager.NotificationType.SYSTEM_ALERT);
            data.setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT);

            notificationManager.showNotification(data);
            Log.d(TAG, "Optimization completed notification shown: " + message);

        } catch (Exception e) {
            Log.e(TAG, "Failed to show optimization completed notification", e);
        }
    }

    /**
     * 显示智能提示通知
     */
    public void showSmartTip(String title, String message) {
        try {
            NotificationManager.NotificationData data =
                new NotificationManager.NotificationData(title, message);

            data.setType(NotificationManager.NotificationType.CUSTOM);
            data.setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT);

            notificationManager.showNotification(data);
            Log.d(TAG, "Smart tip notification shown: " + title);

        } catch (Exception e) {
            Log.e(TAG, "Failed to show smart tip notification", e);
        }
    }

    /**
     * 显示性能监控通知
     */
    public void showPerformanceAlert(String alertType, String message) {
        try {
            String title = getPerformanceAlertTitle(alertType);
            NotificationManager.NotificationData data =
                new NotificationManager.NotificationData(title, message);

            data.setType(NotificationManager.NotificationType.CPU_ALERT);
            data.setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);

            notificationManager.showNotification(data);
            Log.d(TAG, "Performance alert notification shown: " + alertType);

        } catch (Exception e) {
            Log.e(TAG, "Failed to show performance alert notification", e);
        }
    }

    /**
     * 获取性能告警标题
     */
    private String getPerformanceAlertTitle(String alertType) {
        switch (alertType) {
            case "cpu":
                return "CPU使用率过高";
            case "memory":
                return "内存使用率过高";
            case "battery":
                return "电池消耗过快";
            case "network":
                return "网络异常";
            default:
                return "性能告警";
        }
    }

    /**
     * 显示隐私保护通知
     */
    public void showPrivacyAlert(String alertType, String message) {
        try {
            String title = getPrivacyAlertTitle(alertType);
            NotificationManager.NotificationData data =
                new NotificationManager.NotificationData(title, message);

            data.setType(NotificationManager.NotificationType.SYSTEM_ALERT);
            data.setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);

            notificationManager.showNotification(data);
            Log.d(TAG, "Privacy alert notification shown: " + alertType);

        } catch (Exception e) {
            Log.e(TAG, "Failed to show privacy alert notification", e);
        }
    }

    /**
     * 获取隐私告警标题
     */
    private String getPrivacyAlertTitle(String alertType) {
        switch (alertType) {
            case "scan":
                return "隐私扫描完成";
            case "threat":
                return "发现隐私威胁";
            case "monitor":
                return "隐私监控告警";
            default:
                return "隐私保护提醒";
        }
    }

    /**
     * 显示智能推荐通知
     */
    public void showRecommendationNotification(String title, String message) {
        try {
            NotificationManager.NotificationData data =
                new NotificationManager.NotificationData(title, message);

            data.setType(NotificationManager.NotificationType.NEWS);
            data.setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT);

            notificationManager.showNotification(data);
            Log.d(TAG, "Recommendation notification shown: " + title);

        } catch (Exception e) {
            Log.e(TAG, "Failed to show recommendation notification", e);
        }
    }

    /**
     * 显示功能更新通知
     */
    public void showFeatureUpdateNotification(String featureName, String message) {
        try {
            String title = "功能更新: " + featureName;
            NotificationManager.NotificationData data =
                new NotificationManager.NotificationData(title, message);

            data.setType(NotificationManager.NotificationType.NEWS);
            data.setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT);

            notificationManager.showNotification(data);
            Log.d(TAG, "Feature update notification shown: " + featureName);

        } catch (Exception e) {
            Log.e(TAG, "Failed to show feature update notification", e);
        }
    }

    /**
     * 取消所有智能通知
     */
    public void cancelAllSmartNotifications() {
        try {
            notificationManager.cancelAllNotifications();
            Log.d(TAG, "All smart notifications cancelled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel smart notifications", e);
        }
    }

    /**
     * 检查通知权限
     */
    public boolean areNotificationsEnabled() {
        return notificationManager.areNotificationsEnabled();
    }

    /**
     * 获取基础通知管理器
     */
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
