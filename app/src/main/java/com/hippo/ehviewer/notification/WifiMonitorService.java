package com.hippo.ehviewer.notification;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.WebViewActivity;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * WiFi监控服务
 * 监控WiFi连接状态，检测登录门户，自动唤起浏览器
 */
public class WifiMonitorService extends Service {
    
    private static final String TAG = "WifiMonitorService";
    
    // 门户检测URL
    private static final String[] PORTAL_CHECK_URLS = {
        "http://captive.apple.com/hotspot-detect.html",
        "http://connectivitycheck.android.com/generate_204",
        "http://www.msftconnecttest.com/connecttest.txt",
        "http://www.google.com/generate_204"
    };
    
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private NotificationManager notificationManager;
    private Handler mainHandler;
    private Timer portalCheckTimer;
    
    // 网络回调
    private ConnectivityManager.NetworkCallback networkCallback;
    
    // WiFi状态广播接收器
    private BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                
                if (networkInfo != null && networkInfo.isConnected()) {
                    // WiFi已连接
                    onWifiConnected();
                }
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 
                    WifiManager.WIFI_STATE_UNKNOWN);
                    
                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    // WiFi已开启
                    checkAvailableNetworks();
                }
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        notificationManager = NotificationManager.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 注册WiFi状态监听
        registerWifiStateReceiver();
        
        // 注册网络回调（Android 5.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerNetworkCallback();
        }
        
        Log.d(TAG, "WiFi Monitor Service started");
    }
    
    /**
     * 注册WiFi状态接收器
     */
    private void registerWifiStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        
        registerReceiver(wifiStateReceiver, filter);
    }
    
    /**
     * 注册网络回调
     */
    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.d(TAG, "WiFi network available");
                    onWifiConnected();
                }
                
                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                    
                    // 检查是否需要登录门户
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                            // 网络未验证，可能需要登录
                            checkPortalAuthentication();
                        }
                    }
                }
                
                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    Log.d(TAG, "WiFi network lost");
                }
            };
            
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }
    
    /**
     * WiFi连接成功
     */
    private void onWifiConnected() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            String ssid = wifiInfo.getSSID();
            ssid = ssid.replace("\"", ""); // 移除引号
            
            Log.d(TAG, "Connected to WiFi: " + ssid);
            
            // 发送连接通知
            sendWifiConnectedNotification(ssid);
            
            // 延迟检查是否需要门户认证
            mainHandler.postDelayed(this::checkPortalAuthentication, 2000);
        }
    }
    
    /**
     * 检查可用的WiFi网络
     */
    private void checkAvailableNetworks() {
        // 扫描可用的WiFi网络
        if (wifiManager.isWifiEnabled()) {
            wifiManager.startScan();
            
            // 获取扫描结果并提醒用户
            mainHandler.postDelayed(() -> {
                var scanResults = wifiManager.getScanResults();
                if (scanResults != null && !scanResults.isEmpty()) {
                    // 找到开放的WiFi或之前连接过的WiFi
                    for (var result : scanResults) {
                        if (result.capabilities.contains("OPEN") || 
                            result.capabilities.contains("WPS")) {
                            sendAvailableWifiNotification(result.SSID);
                            break;
                        }
                    }
                }
            }, 3000);
        }
    }
    
    /**
     * 检查门户认证
     */
    private void checkPortalAuthentication() {
        new Thread(() -> {
            boolean needsAuth = false;
            String portalUrl = null;
            
            for (String checkUrl : PORTAL_CHECK_URLS) {
                try {
                    URL url = new URL(checkUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setInstanceFollowRedirects(false);
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    connection.setRequestMethod("GET");
                    
                    int responseCode = connection.getResponseCode();
                    
                    // 检查是否被重定向（通常是登录门户）
                    if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                        
                        portalUrl = connection.getHeaderField("Location");
                        needsAuth = true;
                        break;
                    }
                    
                    // 检查是否返回了非预期的内容
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // 对于generate_204应该返回204，如果返回200说明被劫持
                        if (checkUrl.contains("generate_204")) {
                            needsAuth = true;
                            portalUrl = checkUrl;
                            break;
                        }
                    }
                    
                    connection.disconnect();
                    
                } catch (IOException e) {
                    Log.e(TAG, "Portal check failed: " + e.getMessage());
                }
            }
            
            if (needsAuth) {
                final String finalPortalUrl = portalUrl;
                mainHandler.post(() -> handlePortalAuthentication(finalPortalUrl));
            }
        }).start();
    }
    
    /**
     * 处理门户认证
     */
    private void handlePortalAuthentication(String portalUrl) {
        Log.d(TAG, "Portal authentication required: " + portalUrl);
        
        // 发送通知
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "需要登录WiFi",
                "点击完成WiFi认证"
            );
        
        Bundle extras = new Bundle();
        extras.putString("portal_url", portalUrl != null ? portalUrl : "http://192.168.1.1");
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setExtras(extras)
            .setBigText("检测到当前WiFi需要登录认证，点击打开浏览器完成认证。");
        
        notificationManager.showNotification(data);
        
        // 自动打开浏览器
        openPortalInBrowser(portalUrl);
    }
    
    /**
     * 在浏览器中打开门户
     */
    private void openPortalInBrowser(String portalUrl) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(android.net.Uri.parse(
            portalUrl != null ? portalUrl : "http://192.168.1.1"));
        intent.putExtra("is_portal", true);
        intent.putExtra("title", "WiFi登录");
        
        startActivity(intent);
    }
    
    /**
     * 发送WiFi连接通知
     */
    private void sendWifiConnectedNotification(String ssid) {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "已连接到WiFi",
                ssid
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_LOW);
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送可用WiFi通知
     */
    private void sendAvailableWifiNotification(String ssid) {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "发现可用WiFi",
                ssid
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setBigText("发现可用的WiFi网络，点击连接");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 开始定期检查门户
     */
    private void startPortalCheck() {
        if (portalCheckTimer != null) {
            portalCheckTimer.cancel();
        }
        
        portalCheckTimer = new Timer();
        portalCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isWifiConnected()) {
                    checkPortalAuthentication();
                }
            }
        }, 0, 30000); // 每30秒检查一次
    }
    
    /**
     * 停止门户检查
     */
    private void stopPortalCheck() {
        if (portalCheckTimer != null) {
            portalCheckTimer.cancel();
            portalCheckTimer = null;
        }
    }
    
    /**
     * 检查是否连接WiFi
     */
    private boolean isWifiConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && 
               networkInfo.isConnected() && 
               networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 注销接收器
        try {
            unregisterReceiver(wifiStateReceiver);
        } catch (Exception e) {
            // 忽略
        }
        
        // 注销网络回调
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        
        // 停止定时器
        stopPortalCheck();
    }
}