package com.hippo.ehviewer.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Firebase推送管理器
 * 管理Firebase初始化、token获取和推送订阅
 */
public class FirebaseManager {
    
    private static final String TAG = "FirebaseManager";
    private static final String PREFS_NAME = "firebase_config";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_TOKEN_SENT_TO_SERVER = "token_sent_to_server";
    private static final String KEY_SUBSCRIPTIONS = "subscriptions";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final ScheduledExecutorService executor;
    private String currentToken;
    
    private static FirebaseManager instance;
    
    public static synchronized FirebaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseManager(context);
        }
        return instance;
    }
    
    private FirebaseManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadScheduledExecutor();
        
        initializeFirebase();
    }
    
    /**
     * 初始化Firebase
     */
    private void initializeFirebase() {
        try {
            // 初始化Firebase App
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
                Log.i(TAG, "Firebase initialized");
            } else {
                Log.i(TAG, "Firebase already initialized");
            }
            
            // 获取FCM Token
            retrieveAndSaveToken();
            
            // 设置默认订阅
            setupDefaultSubscriptions();
            
            // 定期检查token状态
            scheduleTokenCheck();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
        }
    }
    
    /**
     * 获取并保存FCM Token
     */
    private void retrieveAndSaveToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    
                    // 保存token
                    saveToken(token);
                    
                    // 发送到服务器
                    sendTokenToServer(token);
                }
            });
    }
    
    /**
     * 保存token到本地
     */
    private void saveToken(String token) {
        currentToken = token;
        prefs.edit()
            .putString(KEY_FCM_TOKEN, token)
            .putBoolean(KEY_TOKEN_SENT_TO_SERVER, false)
            .apply();
    }
    
    /**
     * 发送token到服务器
     */
    public void sendTokenToServer(String token) {
        executor.submit(() -> {
            try {
                // 这里实现将token发送到你的后端服务器的逻辑
                boolean success = sendTokenToBackend(token);
                
                if (success) {
                    prefs.edit()
                        .putBoolean(KEY_TOKEN_SENT_TO_SERVER, true)
                        .apply();
                    Log.i(TAG, "Token sent to server successfully");
                } else {
                    Log.w(TAG, "Failed to send token to server");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending token to server", e);
            }
        });
    }
    
    /**
     * 实际发送token到后端的实现
     * 这里需要根据你的后端API进行实现
     */
    private boolean sendTokenToBackend(String token) {
        try {
            // 示例：发送到后端API
            /*
            JSONObject tokenData = new JSONObject();
            tokenData.put("token", token);
            tokenData.put("platform", "android");
            tokenData.put("app_version", BuildConfig.VERSION_NAME);
            tokenData.put("device_id", getDeviceId());
            
            // 使用HTTP客户端发送POST请求到你的服务器
            // String response = httpClient.post("https://your-api.com/register-token", tokenData);
            // return response.contains("success");
            */
            
            // 暂时返回true，表示成功
            Log.d(TAG, "Token would be sent to backend: " + token);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send token to backend", e);
            return false;
        }
    }
    
    /**
     * 设置默认订阅
     */
    private void setupDefaultSubscriptions() {
        // 订阅通用话题
        subscribeToTopic("all_users");
        subscribeToTopic("android_users");
        subscribeToTopic("app_updates");
        
        // 根据用户设置订阅特定话题
        if (shouldSubscribeToNews()) {
            subscribeToTopic("news");
        }
        
        if (shouldSubscribeToFeatures()) {
            subscribeToTopic("new_features");
        }
    }
    
    /**
     * 订阅话题
     */
    public void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to topic: " + topic);
                        saveSubscription(topic, true);
                    } else {
                        Log.w(TAG, "Failed to subscribe to topic: " + topic, task.getException());
                    }
                }
            });
    }
    
    /**
     * 取消订阅话题
     */
    public void unsubscribeFromTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Unsubscribed from topic: " + topic);
                        saveSubscription(topic, false);
                    } else {
                        Log.w(TAG, "Failed to unsubscribe from topic: " + topic, task.getException());
                    }
                }
            });
    }
    
    /**
     * 保存订阅状态
     */
    private void saveSubscription(String topic, boolean subscribed) {
        try {
            String subscriptions = prefs.getString(KEY_SUBSCRIPTIONS, "{}");
            org.json.JSONObject subscriptionsJson = new org.json.JSONObject(subscriptions);
            subscriptionsJson.put(topic, subscribed);
            
            prefs.edit()
                .putString(KEY_SUBSCRIPTIONS, subscriptionsJson.toString())
                .apply();
                
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Failed to save subscription", e);
        }
    }
    
    /**
     * 检查是否订阅了话题
     */
    public boolean isSubscribedToTopic(String topic) {
        try {
            String subscriptions = prefs.getString(KEY_SUBSCRIPTIONS, "{}");
            org.json.JSONObject subscriptionsJson = new org.json.JSONObject(subscriptions);
            return subscriptionsJson.optBoolean(topic, false);
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Failed to check subscription", e);
            return false;
        }
    }
    
    /**
     * 定期检查token状态
     */
    private void scheduleTokenCheck() {
        executor.scheduleAtFixedRate(() -> {
            try {
                boolean tokenSent = prefs.getBoolean(KEY_TOKEN_SENT_TO_SERVER, false);
                if (!tokenSent && currentToken != null) {
                    Log.d(TAG, "Retrying token upload");
                    sendTokenToServer(currentToken);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during token check", e);
            }
        }, 1, 6, TimeUnit.HOURS); // 每6小时检查一次
    }
    
    /**
     * 获取当前FCM Token
     */
    public String getCurrentToken() {
        if (currentToken != null) {
            return currentToken;
        }
        return prefs.getString(KEY_FCM_TOKEN, null);
    }
    
    /**
     * 是否应该订阅新闻
     */
    private boolean shouldSubscribeToNews() {
        // 这里可以根据用户设置或应用配置决定
        return true; // 默认订阅
    }
    
    /**
     * 是否应该订阅新功能通知
     */
    private boolean shouldSubscribeToFeatures() {
        // 这里可以根据用户设置或应用配置决定
        return true; // 默认订阅
    }
    
    /**
     * 启用/禁用推送通知
     */
    public void setPushNotificationsEnabled(boolean enabled) {
        if (enabled) {
            // 重新订阅所有话题
            setupDefaultSubscriptions();
        } else {
            // 取消所有订阅
            unsubscribeFromTopic("all_users");
            unsubscribeFromTopic("android_users");
            unsubscribeFromTopic("app_updates");
            unsubscribeFromTopic("news");
            unsubscribeFromTopic("new_features");
        }
        
        prefs.edit()
            .putBoolean("push_notifications_enabled", enabled)
            .apply();
    }
    
    /**
     * 检查推送通知是否启用
     */
    public boolean isPushNotificationsEnabled() {
        return prefs.getBoolean("push_notifications_enabled", true);
    }
    
    /**
     * 发送测试推送（用于调试）
     */
    public void sendTestNotification() {
        executor.submit(() -> {
            try {
                // 这里可以实现向自己发送测试推送的逻辑
                Log.d(TAG, "Test notification would be sent");
                
                // 或者使用本地通知模拟
                NotificationManager notificationManager = NotificationManager.getInstance(context);
                NotificationManager.NotificationData data = 
                    new NotificationManager.NotificationData(
                        "测试推送",
                        "Firebase推送服务工作正常"
                    );
                
                data.setType(NotificationManager.NotificationType.SYSTEM_ALERT);
                notificationManager.showNotification(data);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to send test notification", e);
            }
        });
    }
    
    /**
     * 获取设备唯一标识
     */
    private String getDeviceId() {
        return android.provider.Settings.Secure.getString(
            context.getContentResolver(), 
            android.provider.Settings.Secure.ANDROID_ID
        );
    }
    
    /**
     * 获取推送统计信息
     */
    public PushStats getPushStats() {
        String token = getCurrentToken();
        boolean tokenSent = prefs.getBoolean(KEY_TOKEN_SENT_TO_SERVER, false);
        boolean notificationsEnabled = isPushNotificationsEnabled();
        
        int subscribedTopics = 0;
        try {
            String subscriptions = prefs.getString(KEY_SUBSCRIPTIONS, "{}");
            org.json.JSONObject subscriptionsJson = new org.json.JSONObject(subscriptions);
            
            // 统计已订阅的话题数量
            org.json.JSONArray names = subscriptionsJson.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String topic = names.getString(i);
                    if (subscriptionsJson.getBoolean(topic)) {
                        subscribedTopics++;
                    }
                }
            }
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Failed to calculate subscribed topics", e);
        }
        
        return new PushStats(
            token != null,
            tokenSent,
            notificationsEnabled,
            subscribedTopics
        );
    }
    
    /**
     * 推送统计信息
     */
    public static class PushStats {
        public final boolean hasToken;
        public final boolean tokenSentToServer;
        public final boolean notificationsEnabled;
        public final int subscribedTopics;
        
        PushStats(boolean hasToken, boolean tokenSentToServer, 
                 boolean notificationsEnabled, int subscribedTopics) {
            this.hasToken = hasToken;
            this.tokenSentToServer = tokenSentToServer;
            this.notificationsEnabled = notificationsEnabled;
            this.subscribedTopics = subscribedTopics;
        }
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        instance = null;
    }
}