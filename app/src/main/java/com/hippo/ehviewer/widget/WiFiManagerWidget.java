package com.hippo.ehviewer.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.notification.NotificationManager;
import androidx.core.app.NotificationCompat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WiFi管理桌面小部件
 * 显示WiFi状态、信号强度、可用网络，提供连接建议
 */
public class WiFiManagerWidget extends BaseEhWidget {
    
    private static final String TAG = "WiFiManagerWidget";
    
    // 自定义Actions
    private static final String ACTION_WIFI_TOGGLE = "com.hippo.ehviewer.widget.WIFI_TOGGLE";
    private static final String ACTION_WIFI_SCAN = "com.hippo.ehviewer.widget.WIFI_SCAN";
    private static final String ACTION_WIFI_CONNECT = "com.hippo.ehviewer.widget.WIFI_CONNECT";
    private static final String ACTION_NETWORK_CHANGED = "com.hippo.ehviewer.widget.NETWORK_CHANGED";
    
    // 网络状态监听器
    private static NetworkStateReceiver networkReceiver;
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        
        // 注册网络状态监听
        registerNetworkReceiver(context);
        
        // 启动定时扫描
        startPeriodicScan(context);
    }
    
    @Override
    protected RemoteViews createRemoteViews(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_wifi_manager);
        
        // 更新WiFi状态显示
        updateWiFiStatus(context, views);
        
        // 更新网络信息显示
        updateNetworkInfo(context, views);
        
        // 扫描并显示可用网络
        updateAvailableNetworks(context, views);
        
        return views;
    }
    
    @Override
    protected void setupCustomClickActions(Context context, RemoteViews views, int appWidgetId) {
        // WiFi开关点击
        Bundle wifiToggleExtras = new Bundle();
        wifiToggleExtras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.wifi_toggle_button,
            createClickPendingIntent(context, appWidgetId * 10, "wifi_toggle", wifiToggleExtras));
        
        // WiFi扫描点击
        Bundle scanExtras = new Bundle();
        scanExtras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.wifi_scan_button,
            createClickPendingIntent(context, appWidgetId * 10 + 1, "wifi_scan", scanExtras));
        
        // 点击WiFi名称打开WiFi设置
        views.setOnClickPendingIntent(R.id.current_wifi_name,
            createBrowserPendingIntent(context, appWidgetId * 10 + 2,
                "https://192.168.1.1", "路由器管理"));
                
        // 点击信号强度打开网络测速
        views.setOnClickPendingIntent(R.id.signal_strength,
            createBrowserPendingIntent(context, appWidgetId * 10 + 3,
                "https://fast.com", "网络测速"));
    }
    
    @Override
    protected void handleCustomAction(Context context, Intent intent, String action) {
        switch (action) {
            case ACTION_WIFI_TOGGLE:
                toggleWiFi(context);
                break;
                
            case ACTION_WIFI_SCAN:
                scanWiFiNetworks(context);
                break;
                
            case ACTION_WIFI_CONNECT:
                String ssid = intent.getStringExtra("ssid");
                connectToWiFi(context, ssid);
                break;
                
            case ACTION_NETWORK_CHANGED:
                handleNetworkChange(context, intent);
                break;
        }
    }
    
    @Override
    protected void handleWidgetClick(Context context, Intent intent) {
        String clickType = intent.getStringExtra("click_type");
        
        switch (clickType) {
            case "wifi_toggle":
                toggleWiFi(context);
                break;
                
            case "wifi_scan":
                scanWiFiNetworks(context);
                break;
                
            case "browser":
                super.handleWidgetClick(context, intent);
                break;
                
            default:
                // 默认打开WiFi设置
                openWiFiSettings(context);
                break;
        }
    }
    
    /**
     * 更新WiFi状态显示
     */
    private void updateWiFiStatus(Context context, RemoteViews views) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        
        if (wifiManager != null) {
            boolean isWifiEnabled = wifiManager.isWifiEnabled();
            
            // 更新WiFi开关状态
            views.setTextViewText(R.id.wifi_status, isWifiEnabled ? "WiFi 已开启" : "WiFi 已关闭");
            views.setInt(R.id.wifi_toggle_button, "setBackgroundResource", 
                isWifiEnabled ? R.drawable.button_wifi_on : R.drawable.button_wifi_off);
            
            if (isWifiEnabled) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                    // 已连接到WiFi
                    String ssid = wifiInfo.getSSID().replace("\"", "");
                    int rssi = wifiInfo.getRssi();
                    int signalLevel = WifiManager.calculateSignalLevel(rssi, 5);
                    
                    views.setTextViewText(R.id.current_wifi_name, ssid);
                    views.setTextViewText(R.id.signal_strength, getSignalStrengthText(signalLevel, rssi));
                    views.setInt(R.id.signal_strength_icon, "setImageResource", getSignalIcon(signalLevel));
                    
                    // 显示连接信息
                    String ipAddress = formatIpAddress(wifiInfo.getIpAddress());
                    views.setTextViewText(R.id.connection_info, 
                        String.format("IP: %s", ipAddress));
                        
                } else {
                    // WiFi开启但未连接
                    views.setTextViewText(R.id.current_wifi_name, "未连接");
                    views.setTextViewText(R.id.signal_strength, "");
                    views.setTextViewText(R.id.connection_info, "");
                    views.setInt(R.id.signal_strength_icon, "setImageResource", R.drawable.ic_wifi_off);
                }
            } else {
                // WiFi关闭
                views.setTextViewText(R.id.current_wifi_name, "WiFi已关闭");
                views.setTextViewText(R.id.signal_strength, "");
                views.setTextViewText(R.id.connection_info, "");
                views.setInt(R.id.signal_strength_icon, "setImageResource", R.drawable.ic_wifi_off);
            }
        }
    }
    
    /**
     * 更新网络信息显示
     */
    private void updateNetworkInfo(Context context, RemoteViews views) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                String networkType = getNetworkTypeName(activeNetwork.getType());
                String statusText = String.format("网络: %s", networkType);
                views.setTextViewText(R.id.network_type, statusText);
                
                // 检测网络切换
                checkNetworkSwitch(context, activeNetwork);
            } else {
                views.setTextViewText(R.id.network_type, "网络: 未连接");
            }
        }
    }
    
    /**
     * 更新可用网络显示
     */
    private void updateAvailableNetworks(Context context, RemoteViews views) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            
            if (scanResults != null && !scanResults.isEmpty()) {
                // 找到信号最强的开放网络
                ScanResult bestOpenNetwork = findBestOpenNetwork(scanResults);
                
                if (bestOpenNetwork != null) {
                    views.setTextViewText(R.id.available_networks, 
                        String.format("发现开放WiFi: %s", bestOpenNetwork.SSID));
                    views.setTextViewText(R.id.network_suggestion,
                        String.format("信号强度: %d dBm - 点击连接", bestOpenNetwork.level));
                    
                    // 显示连接建议
                    showConnectionSuggestion(context, bestOpenNetwork);
                } else {
                    int networkCount = scanResults.size();
                    views.setTextViewText(R.id.available_networks, 
                        String.format("发现 %d 个网络", networkCount));
                    views.setTextViewText(R.id.network_suggestion, "点击扫描更多网络");
                }
            } else {
                views.setTextViewText(R.id.available_networks, "正在扫描...");
                views.setTextViewText(R.id.network_suggestion, "");
            }
        } else {
            views.setTextViewText(R.id.available_networks, "WiFi未开启");
            views.setTextViewText(R.id.network_suggestion, "");
        }
    }
    
    /**
     * 开关WiFi
     */
    private void toggleWiFi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        
        if (wifiManager != null) {
            boolean currentState = wifiManager.isWifiEnabled();
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ 不允许直接控制WiFi，打开设置页面
                openWiFiSettings(context);
                sendNotification(context, "WiFi设置", 
                    currentState ? "请手动关闭WiFi" : "请手动开启WiFi");
            } else {
                // 较旧版本可以直接控制
                wifiManager.setWifiEnabled(!currentState);
                
                widgetExecutor.schedule(() -> {
                    forceUpdateAllWidgets(context);
                }, 2, TimeUnit.SECONDS);
                
                sendNotification(context, "WiFi状态", 
                    currentState ? "WiFi已关闭" : "WiFi已开启");
            }
        }
    }
    
    /**
     * 扫描WiFi网络
     */
    private void scanWiFiNetworks(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            boolean scanStarted = wifiManager.startScan();
            
            if (scanStarted) {
                sendNotification(context, "WiFi扫描", "正在扫描可用网络...");
                
                // 3秒后更新小部件
                widgetExecutor.schedule(() -> {
                    forceUpdateAllWidgets(context);
                }, 3, TimeUnit.SECONDS);
            } else {
                sendNotification(context, "WiFi扫描", "扫描失败，请稍后重试");
            }
        } else {
            sendNotification(context, "WiFi扫描", "请先开启WiFi");
        }
    }
    
    /**
     * 连接到指定WiFi
     */
    private void connectToWiFi(Context context, String ssid) {
        // Android 10+ 需要通过系统设置连接WiFi
        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(intent);
            sendNotification(context, "WiFi连接", 
                String.format("请在WiFi设置中连接到 %s", ssid));
        } catch (Exception e) {
            Log.e(TAG, "Failed to open WiFi settings", e);
        }
    }
    
    /**
     * 检测网络切换
     */
    private void checkNetworkSwitch(Context context, NetworkInfo currentNetwork) {
        String currentType = getNetworkTypeName(currentNetwork.getType());
        
        // 从SharedPreferences获取上次的网络类型
        String lastNetworkType = context.getSharedPreferences("wifi_widget", Context.MODE_PRIVATE)
            .getString("last_network_type", "");
        
        if (!lastNetworkType.equals(currentType)) {
            // 网络类型发生变化
            String message = "";
            if ("WiFi".equals(currentType) && "移动网络".equals(lastNetworkType)) {
                message = "已切换到WiFi网络，网速更快！";
            } else if ("移动网络".equals(currentType) && "WiFi".equals(lastNetworkType)) {
                message = "已切换到移动网络，注意流量使用";
            }
            
            if (!message.isEmpty()) {
                showNetworkSwitchNotification(context, message, currentType);
            }
            
            // 保存当前网络类型
            context.getSharedPreferences("wifi_widget", Context.MODE_PRIVATE)
                .edit()
                .putString("last_network_type", currentType)
                .apply();
        }
    }
    
    /**
     * 显示网络切换通知
     */
    private void showNetworkSwitchNotification(Context context, String message, String networkType) {
        NotificationManager notificationManager = NotificationManager.getInstance(context);
        
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData("网络切换", message);
        
        Bundle extras = new Bundle();
        extras.putString("network_type", networkType);
        extras.putString("action_url", "WiFi".equals(networkType) ? 
            "https://fast.com" : "https://speedtest.net");
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setExtras(extras)
            .setBigText(message + " 点击测试网速");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 显示连接建议
     */
    private void showConnectionSuggestion(Context context, ScanResult network) {
        // 检查是否已经连接到更好的网络
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo currentWifi = wifiManager.getConnectionInfo();
            
            // 如果当前未连接WiFi或信号较弱，显示连接建议
            if (currentWifi == null || currentWifi.getNetworkId() == -1 || 
                currentWifi.getRssi() < network.level - 10) {
                
                widgetExecutor.schedule(() -> {
                    sendNotification(context, "发现更好的WiFi", 
                        String.format("发现开放网络 %s，信号更强。点击连接", network.SSID));
                }, 5, TimeUnit.SECONDS);
            }
        }
    }
    
    /**
     * 找到最佳开放网络
     */
    private ScanResult findBestOpenNetwork(List<ScanResult> scanResults) {
        ScanResult bestNetwork = null;
        
        for (ScanResult result : scanResults) {
            // 检查是否为开放网络（无加密）
            if (result.capabilities.contains("OPEN") || 
                (!result.capabilities.contains("WPA") && !result.capabilities.contains("WEP"))) {
                
                if (bestNetwork == null || result.level > bestNetwork.level) {
                    bestNetwork = result;
                }
            }
        }
        
        return bestNetwork;
    }
    
    /**
     * 获取信号强度文本
     */
    private String getSignalStrengthText(int level, int rssi) {
        String[] levels = {"很差", "较差", "一般", "良好", "优秀"};
        return String.format("%s (%d dBm)", levels[level], rssi);
    }
    
    /**
     * 获取信号图标
     */
    private int getSignalIcon(int level) {
        switch (level) {
            case 0: return R.drawable.ic_wifi_signal_0;
            case 1: return R.drawable.ic_wifi_signal_1;
            case 2: return R.drawable.ic_wifi_signal_2;
            case 3: return R.drawable.ic_wifi_signal_3;
            case 4: return R.drawable.ic_wifi_signal_4;
            default: return R.drawable.ic_wifi_off;
        }
    }
    
    /**
     * 获取网络类型名称
     */
    private String getNetworkTypeName(int type) {
        switch (type) {
            case ConnectivityManager.TYPE_WIFI:
                return "WiFi";
            case ConnectivityManager.TYPE_MOBILE:
                return "移动网络";
            case ConnectivityManager.TYPE_ETHERNET:
                return "以太网";
            default:
                return "未知网络";
        }
    }
    
    /**
     * 格式化IP地址
     */
    private String formatIpAddress(int ipAddress) {
        return String.format("%d.%d.%d.%d",
            (ipAddress & 0xff),
            (ipAddress >> 8 & 0xff),
            (ipAddress >> 16 & 0xff),
            (ipAddress >> 24 & 0xff));
    }
    
    /**
     * 注册网络状态监听
     */
    private void registerNetworkReceiver(Context context) {
        if (networkReceiver == null) {
            networkReceiver = new NetworkStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            
            try {
                context.registerReceiver(networkReceiver, filter);
            } catch (Exception e) {
                Log.e(TAG, "Failed to register network receiver", e);
            }
        }
    }
    
    /**
     * 启动定时扫描
     */
    private void startPeriodicScan(Context context) {
        // 每2分钟扫描一次WiFi
        widgetExecutor.scheduleAtFixedRate(() -> {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                wifiManager.startScan();
                
                // 3秒后更新显示
                widgetExecutor.schedule(() -> {
                    forceUpdateAllWidgets(context);
                }, 3, TimeUnit.SECONDS);
            }
        }, 10, 120, TimeUnit.SECONDS);
    }
    
    /**
     * 打开WiFi设置
     */
    private void openWiFiSettings(Context context) {
        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open WiFi settings", e);
            // 降级到浏览器
            openBrowser(context, "https://192.168.1.1", "路由器设置");
        }
    }
    
    /**
     * 发送通知
     */
    private void sendNotification(Context context, String title, String message) {
        NotificationManager notificationManager = NotificationManager.getInstance(context);
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(title, message);
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_LOW);
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 处理网络变化
     */
    private void handleNetworkChange(Context context, Intent intent) {
        // 网络状态发生变化时更新小部件
        widgetExecutor.schedule(() -> {
            forceUpdateAllWidgets(context);
        }, 1, TimeUnit.SECONDS);
    }
    
    /**
     * 网络状态监听器
     */
    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                Log.d(TAG, "Network state changed: " + action);
                handleNetworkChange(context, intent);
            }
        }
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        
        // 注销网络监听器
        if (networkReceiver != null) {
            try {
                context.unregisterReceiver(networkReceiver);
                networkReceiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister network receiver", e);
            }
        }
    }
}