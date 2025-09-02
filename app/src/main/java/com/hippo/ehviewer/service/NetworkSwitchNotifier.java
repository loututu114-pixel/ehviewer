package com.hippo.ehviewer.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.hippo.ehviewer.notification.NotificationManager;
import androidx.core.app.NotificationCompat;
import android.os.Bundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 网络切换提醒服务
 * 监控网络状态变化，提供智能切换建议，引导用户使用浏览器
 */
public class NetworkSwitchNotifier extends BroadcastReceiver {
    
    private static final String TAG = "NetworkSwitchNotifier";
    private static final String PREFS_NAME = "network_switch_notifier";
    
    // 网络状态
    private static final int NETWORK_NONE = 0;
    private static final int NETWORK_WIFI = 1;
    private static final int NETWORK_MOBILE = 2;
    private static final int NETWORK_OTHER = 3;
    
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        
        Log.d(TAG, "Received network event: " + action);
        
        switch (action) {
            case ConnectivityManager.CONNECTIVITY_ACTION:
                handleConnectivityChange(context, intent);
                break;
                
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                handleWiFiStateChange(context, intent);
                break;
                
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                handleWiFiToggle(context, intent);
                break;
                
            case Intent.ACTION_BOOT_COMPLETED:
                // 开机后初始化网络状态
                initializeNetworkState(context);
                break;
        }
    }
    
    /**
     * 处理网络连接变化
     */
    private void handleConnectivityChange(Context context, Intent intent) {
        // 延迟处理，确保网络状态稳定
        executor.schedule(() -> {
            processNetworkChange(context);
        }, 2, TimeUnit.SECONDS);
    }
    
    /**
     * 处理WiFi状态变化
     */
    private void handleWiFiStateChange(Context context, Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        
        if (networkInfo != null) {
            Log.d(TAG, "WiFi network state: " + networkInfo.getDetailedState());
            
            if (networkInfo.isConnected()) {
                // WiFi已连接
                executor.schedule(() -> {
                    onWiFiConnected(context);
                }, 1, TimeUnit.SECONDS);
            } else if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {
                // WiFi断开连接
                onWiFiDisconnected(context);
            }
        }
    }
    
    /**
     * 处理WiFi开关
     */
    private void handleWiFiToggle(Context context, Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                onWiFiEnabled(context);
                break;
                
            case WifiManager.WIFI_STATE_DISABLED:
                onWiFiDisabled(context);
                break;
        }
    }
    
    /**
     * 处理网络变化
     */
    private void processNetworkChange(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        int currentNetworkType = getNetworkType(activeNetwork);
        int previousNetworkType = getPreviousNetworkType(context);
        
        Log.d(TAG, "Network change: " + previousNetworkType + " -> " + currentNetworkType);
        
        // 保存当前网络类型
        saveCurrentNetworkType(context, currentNetworkType);
        
        // 处理网络切换
        handleNetworkSwitch(context, previousNetworkType, currentNetworkType, activeNetwork);
    }
    
    /**
     * 处理网络切换
     */
    private void handleNetworkSwitch(Context context, int fromType, int toType, NetworkInfo networkInfo) {
        if (fromType == toType) return;
        
        String fromTypeName = getNetworkTypeName(fromType);
        String toTypeName = getNetworkTypeName(toType);
        
        Log.i(TAG, "Network switched: " + fromTypeName + " -> " + toTypeName);
        
        switch (toType) {
            case NETWORK_WIFI:
                handleSwitchToWiFi(context, fromType, networkInfo);
                break;
                
            case NETWORK_MOBILE:
                handleSwitchToMobile(context, fromType);
                break;
                
            case NETWORK_NONE:
                handleNetworkDisconnected(context, fromType);
                break;
                
            case NETWORK_OTHER:
                handleSwitchToOther(context, fromType);
                break;
        }
        
        // 记录切换历史
        recordNetworkSwitchHistory(context, fromType, toType);
    }
    
    /**
     * 切换到WiFi网络
     */
    private void handleSwitchToWiFi(Context context, int fromType, NetworkInfo networkInfo) {
        String message = "已连接到WiFi网络，网速更快！";
        String action_url = "https://fast.com";
        String action_title = "测试网速";
        
        // 获取WiFi网络信息
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID().replace("\"", "");
                int rssi = wifiInfo.getRssi();
                int signalLevel = WifiManager.calculateSignalLevel(rssi, 5);
                
                message = String.format("已连接到WiFi：%s\n信号强度：%s", 
                    ssid, getSignalLevelText(signalLevel));
                
                // 根据信号强度提供不同建议
                if (signalLevel >= 3) {
                    action_title = "享受高速浏览";
                    action_url = "https://www.google.com";
                } else if (signalLevel >= 1) {
                    action_title = "测试连接速度";
                    action_url = "https://fast.com";
                } else {
                    message += "\n信号较弱，可能影响浏览体验";
                    action_title = "寻找更好信号";
                    action_url = "https://wifianalyzer.com";
                }
            }
        }
        
        showNetworkSwitchNotification(context, "WiFi已连接", message, action_url, action_title, "wifi_connected");
    }
    
    /**
     * 切换到移动网络
     */
    private void handleSwitchToMobile(Context context, int fromType) {
        String message = "已切换到移动网络";
        String action_url = "https://m.baidu.com"; // 轻量版网页
        String action_title = "节省流量浏览";
        
        if (fromType == NETWORK_WIFI) {
            message = "WiFi已断开，切换到移动网络\n注意流量使用";
            
            // 检查是否有流量套餐信息
            String trafficTip = getTrafficUsageTip(context);
            if (!trafficTip.isEmpty()) {
                message += "\n" + trafficTip;
            }
            
            action_title = "省流量模式浏览";
        }
        
        showNetworkSwitchNotification(context, "移动网络连接", message, action_url, action_title, "mobile_connected");
    }
    
    /**
     * 网络断开连接
     */
    private void handleNetworkDisconnected(Context context, int fromType) {
        String message = "网络连接已断开";
        String action_url = "https://cached.google.com"; // 缓存页面
        String action_title = "查看离线内容";
        
        if (fromType == NETWORK_WIFI) {
            message = "WiFi连接已断开\n可尝试重新连接或使用移动网络";
            action_title = "重新连接WiFi";
            action_url = null; // 将打开WiFi设置而不是浏览器
        }
        
        showNetworkSwitchNotification(context, "网络已断开", message, action_url, action_title, "network_disconnected");
    }
    
    /**
     * 切换到其他网络
     */
    private void handleSwitchToOther(Context context, int fromType) {
        String message = "已连接到网络";
        String action_url = "https://www.google.com";
        String action_title = "测试连接";
        
        showNetworkSwitchNotification(context, "网络已连接", message, action_url, action_title, "other_connected");
    }
    
    /**
     * WiFi连接成功
     */
    private void onWiFiConnected(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                String ssid = wifiInfo.getSSID().replace("\"", "");
                
                // 检查是否是新的WiFi网络
                String lastWiFiSSID = getLastWiFiSSID(context);
                if (!ssid.equals(lastWiFiSSID)) {
                    saveLastWiFiSSID(context, ssid);
                    
                    String message = String.format("新WiFi网络：%s\n点击开始浏览", ssid);
                    showNetworkSwitchNotification(context, "WiFi网络已连接", message, 
                        "https://www.google.com", "开始浏览", "new_wifi_connected");
                }
                
                // 检查是否可能需要认证
                executor.schedule(() -> {
                    checkWiFiPortalAuthentication(context, ssid);
                }, 5, TimeUnit.SECONDS);
            }
        }
    }
    
    /**
     * WiFi断开连接
     */
    private void onWiFiDisconnected(Context context) {
        // 检查是否还有其他网络可用
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                showQuickReconnectNotification(context);
            }
        }
    }
    
    /**
     * WiFi已开启
     */
    private void onWiFiEnabled(Context context) {
        executor.schedule(() -> {
            suggestWiFiConnection(context);
        }, 3, TimeUnit.SECONDS);
    }
    
    /**
     * WiFi已关闭
     */
    private void onWiFiDisabled(Context context) {
        String message = "WiFi已关闭，将使用移动网络\n注意流量消耗";
        showNetworkSwitchNotification(context, "WiFi已关闭", message, 
            "https://m.baidu.com", "省流量浏览", "wifi_disabled");
    }
    
    /**
     * 检查WiFi门户认证
     */
    private void checkWiFiPortalAuthentication(Context context, String ssid) {
        // 简单的门户检测
        try {
            java.net.URL url = new java.net.URL("http://www.google.com/generate_204");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(false);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 204) {
                // 可能需要门户认证
                String message = String.format("网络 %s 可能需要登录认证\n点击完成认证", ssid);
                showNetworkSwitchNotification(context, "需要网络认证", message,
                    "http://captive.apple.com", "完成认证", "portal_auth_required");
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to check portal authentication", e);
        }
    }
    
    /**
     * 建议WiFi连接
     */
    private void suggestWiFiConnection(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            // 扫描可用网络
            boolean scanStarted = wifiManager.startScan();
            if (scanStarted) {
                executor.schedule(() -> {
                    var scanResults = wifiManager.getScanResults();
                    if (scanResults != null && !scanResults.isEmpty()) {
                        // 寻找最佳开放网络
                        var bestOpenNetwork = scanResults.stream()
                            .filter(result -> !result.capabilities.contains("WPA") && !result.capabilities.contains("WEP"))
                            .max((a, b) -> Integer.compare(a.level, b.level))
                            .orElse(null);
                        
                        if (bestOpenNetwork != null) {
                            String message = String.format("发现开放WiFi：%s\n信号强度：%d dBm",
                                bestOpenNetwork.SSID, bestOpenNetwork.level);
                            showNetworkSwitchNotification(context, "发现可用WiFi", message,
                                null, "连接WiFi", "open_wifi_found");
                        }
                    }
                }, 3, TimeUnit.SECONDS);
            }
        }
    }
    
    /**
     * 显示快速重连通知
     */
    private void showQuickReconnectNotification(Context context) {
        String message = "网络已断开\n点击重新连接或查看离线内容";
        showNetworkSwitchNotification(context, "网络连接断开", message,
            null, "重新连接", "quick_reconnect");
    }
    
    /**
     * 显示网络切换通知
     */
    private void showNetworkSwitchNotification(Context context, String title, String message, 
                                             String actionUrl, String actionTitle, String notifyType) {
        
        NotificationManager notificationManager = NotificationManager.getInstance(context);
        
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(title, message);
        
        Bundle extras = new Bundle();
        extras.putString("notify_type", notifyType);
        
        if (actionUrl != null) {
            extras.putString("action_url", actionUrl);
            extras.putString("action_title", actionTitle);
        } else {
            // 没有URL时，设置为打开网络设置
            extras.putString("open_settings", "wifi");
        }
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setExtras(extras)
            .setBigText(message + (actionUrl != null ? "\n\n点击使用EhViewer浏览器" + actionTitle : ""));
        
        // 添加快捷操作
        if (actionUrl != null) {
            android.content.Intent browserIntent = new android.content.Intent(context, 
                com.hippo.ehviewer.ui.WebViewActivity.class);
            browserIntent.setData(android.net.Uri.parse(actionUrl));
            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            browserIntent.putExtra("title", actionTitle);
            browserIntent.putExtra("from_network_notifier", true);
            
            android.app.PendingIntent browserPendingIntent = android.app.PendingIntent.getActivity(
                context, notifyType.hashCode(), browserIntent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            
            NotificationManager.NotificationAction browserAction = 
                new NotificationManager.NotificationAction(
                    com.hippo.ehviewer.R.drawable.ic_browser, actionTitle, browserPendingIntent);
            
            data.actions = new NotificationManager.NotificationAction[]{browserAction};
        }
        
        notificationManager.showNotification(data);
        
        // 记录通知
        recordNotificationShown(context, notifyType, title);
    }
    
    /**
     * 获取网络类型
     */
    private int getNetworkType(NetworkInfo networkInfo) {
        if (networkInfo == null || !networkInfo.isConnected()) {
            return NETWORK_NONE;
        }
        
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return NETWORK_WIFI;
            case ConnectivityManager.TYPE_MOBILE:
                return NETWORK_MOBILE;
            default:
                return NETWORK_OTHER;
        }
    }
    
    /**
     * 获取网络类型名称
     */
    private String getNetworkTypeName(int networkType) {
        switch (networkType) {
            case NETWORK_WIFI: return "WiFi";
            case NETWORK_MOBILE: return "移动网络";
            case NETWORK_OTHER: return "其他网络";
            default: return "无网络";
        }
    }
    
    /**
     * 获取信号强度文本
     */
    private String getSignalLevelText(int level) {
        switch (level) {
            case 4: return "优秀";
            case 3: return "良好";
            case 2: return "一般";
            case 1: return "较差";
            default: return "很差";
        }
    }
    
    /**
     * 获取流量使用提示
     */
    private String getTrafficUsageTip(Context context) {
        // 这里可以集成实际的流量监控API
        // 目前返回通用提示
        return "建议使用省流量模式浏览";
    }
    
    /**
     * 初始化网络状态
     */
    private void initializeNetworkState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            int currentType = getNetworkType(activeNetwork);
            saveCurrentNetworkType(context, currentType);
        }
    }
    
    /**
     * 获取上次网络类型
     */
    private int getPreviousNetworkType(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("previous_network_type", NETWORK_NONE);
    }
    
    /**
     * 保存当前网络类型
     */
    private void saveCurrentNetworkType(Context context, int networkType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("previous_network_type", networkType)
            .putLong("last_network_change", System.currentTimeMillis())
            .apply();
    }
    
    /**
     * 获取上次WiFi SSID
     */
    private String getLastWiFiSSID(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("last_wifi_ssid", "");
    }
    
    /**
     * 保存WiFi SSID
     */
    private void saveLastWiFiSSID(Context context, String ssid) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("last_wifi_ssid", ssid)
            .apply();
    }
    
    /**
     * 记录网络切换历史
     */
    private void recordNetworkSwitchHistory(Context context, int fromType, int toType) {
        long currentTime = System.currentTimeMillis();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 保存切换历史
        int switchCount = prefs.getInt("switch_count", 0) + 1;
        prefs.edit()
            .putInt("switch_count", switchCount)
            .putLong("last_switch_time", currentTime)
            .putString("last_switch", fromType + "->" + toType)
            .apply();
    }
    
    /**
     * 记录通知显示
     */
    private void recordNotificationShown(Context context, String type, String title) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong("last_notification_time", System.currentTimeMillis())
            .putString("last_notification_type", type)
            .putString("last_notification_title", title)
            .apply();
    }
    
    /**
     * 注册网络监听器
     */
    public static void registerNetworkReceiver(Context context) {
        NetworkSwitchNotifier receiver = new NetworkSwitchNotifier();
        IntentFilter filter = new IntentFilter();
        
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        
        try {
            context.registerReceiver(receiver, filter);
            Log.i(TAG, "Network switch notifier registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network receiver", e);
        }
    }
}