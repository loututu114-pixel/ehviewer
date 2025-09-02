package com.hippo.ehviewer.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import android.app.Service;
import android.content.Context;
import android.os.IBinder;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.MainActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * FCM推送消息服务
 * 处理来自Firebase的远程推送消息
 * 已启用Firebase功能，支持完整的推送消息处理
 */
public class PushMessageService extends FirebaseMessagingService {
    
    private static final String TAG = "PushMessageService";
    
    private NotificationManager notificationManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = NotificationManager.getInstance(this);
    }
    
    /**
     * 接收推送消息
     * Firebase消息接收处理
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // 检查消息是否包含数据
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }
        
        // 检查消息是否包含通知
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }
    
    /**
     * 模拟接收推送消息（测试用）
     */
    public void simulateMessageReceived(Map<String, String> data, String title, String body) {
        if (data != null && data.size() > 0) {
            handleDataMessage(data);
        } else if (title != null && body != null) {
            handleSimpleNotification(title, body);
        }
    }
    
    /**
     * 处理数据消息
     */
    private void handleDataMessage(Map<String, String> data) {
        try {
            String type = data.get("type");
            String title = data.get("title");
            String message = data.get("message");
            String action = data.get("action");
            String payload = data.get("payload");
            
            // 根据类型处理不同的推送
            if (type != null) {
                switch (type) {
                    case "news":
                        handleNewsNotification(data);
                        break;
                        
                    case "message":
                        handleMessageNotification(data);
                        break;
                        
                    case "file":
                        handleFileNotification(data);
                        break;
                        
                    case "system":
                        handleSystemNotification(data);
                        break;
                        
                    case "update":
                        handleUpdateNotification(data);
                        break;
                        
                    case "deeplink":
                        handleDeepLinkNotification(data);
                        break;
                        
                    default:
                        // 默认通知
                        showDefaultNotification(title, message, payload);
                        break;
                }
            } else {
                showDefaultNotification(title, message, payload);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing data message", e);
        }
    }
    
    /**
     * 处理Firebase通知消息
     */
    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        String title = notification.getTitle();
        String body = notification.getBody();
        String imageUrl = notification.getImageUrl() != null ? notification.getImageUrl().toString() : null;
        
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(title, body);
        
        // 添加图片支持
        if (imageUrl != null) {
            Bitmap bitmap = getBitmapFromURL(imageUrl);
            if (bitmap != null) {
                data.setBigPicture(bitmap);
            }
        }
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 处理简单通知消息
     */
    private void handleSimpleNotification(String title, String body) {
        
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(title, body);
        
        // 可以添加图片支持
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 处理新闻通知
     */
    private void handleNewsNotification(Map<String, String> data) {
        String title = data.get("title");
        String content = data.get("content");
        String newsId = data.get("news_id");
        String newsUrl = data.get("news_url");
        String imageUrl = data.get("image_url");
        
        NotificationManager.NotificationData notifData = 
            new NotificationManager.NotificationData(title, content);
        
        Bundle extras = new Bundle();
        extras.putString("news_id", newsId);
        extras.putString("news_url", newsUrl);
        
        notifData.setType(NotificationManager.NotificationType.NEWS)
                 .setExtras(extras);
        
        // 加载图片
        if (imageUrl != null) {
            Bitmap bitmap = getBitmapFromURL(imageUrl);
            if (bitmap != null) {
                notifData.setBigPicture(bitmap);
            }
        }
        
        notificationManager.showNotification(notifData);
    }
    
    /**
     * 处理消息通知
     */
    private void handleMessageNotification(Map<String, String> data) {
        String sender = data.get("sender");
        String message = data.get("message");
        String messageId = data.get("message_id");
        String conversationId = data.get("conversation_id");
        
        NotificationManager.NotificationData notifData = 
            new NotificationManager.NotificationData(sender, message);
        
        Bundle extras = new Bundle();
        extras.putString("message_id", messageId);
        extras.putString("conversation_id", conversationId);
        extras.putString("sender", sender);
        
        notifData.setType(NotificationManager.NotificationType.MESSAGE)
                 .setExtras(extras)
                 .setPriority(NotificationCompat.PRIORITY_HIGH);
        
        // 添加快速回复操作
        Intent replyIntent = new Intent(this, MainActivity.class);
        replyIntent.setAction("com.hippo.ehviewer.ACTION_REPLY");
        replyIntent.putExtra("conversation_id", conversationId);
        
        PendingIntent replyPendingIntent = PendingIntent.getActivity(
            this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        NotificationManager.NotificationAction replyAction = 
            new NotificationManager.NotificationAction(
                R.drawable.ic_reply, "回复", replyPendingIntent);
        
        notifData.actions = new NotificationManager.NotificationAction[]{replyAction};
        
        notificationManager.showNotification(notifData);
    }
    
    /**
     * 处理文件通知
     */
    private void handleFileNotification(Map<String, String> data) {
        String fileName = data.get("file_name");
        String filePath = data.get("file_path");
        String fileType = data.get("file_type");
        String fileSize = data.get("file_size");
        
        String title = "文件准备就绪";
        String message = fileName + " (" + fileSize + ")";
        
        NotificationManager.NotificationData notifData = 
            new NotificationManager.NotificationData(title, message);
        
        Bundle extras = new Bundle();
        extras.putString("file_path", filePath);
        extras.putString("mime_type", getMimeTypeFromExtension(fileType));
        
        notifData.setType(NotificationManager.NotificationType.FILE_OPEN)
                 .setExtras(extras)
                 .setBigText("点击使用 EhViewer 打开文件\n" + fileName);
        
        notificationManager.showNotification(notifData);
    }
    
    /**
     * 处理系统通知
     */
    private void handleSystemNotification(Map<String, String> data) {
        String alert = data.get("alert");
        String description = data.get("description");
        String severity = data.get("severity");
        
        NotificationManager.NotificationData notifData = 
            new NotificationManager.NotificationData(alert, description);
        
        // 根据严重程度设置优先级
        int priority = NotificationCompat.PRIORITY_DEFAULT;
        if ("high".equals(severity)) {
            priority = NotificationCompat.PRIORITY_HIGH;
        } else if ("low".equals(severity)) {
            priority = NotificationCompat.PRIORITY_LOW;
        }
        
        notifData.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
                 .setPriority(priority);
        
        notificationManager.showNotification(notifData);
    }
    
    /**
     * 处理更新通知
     */
    private void handleUpdateNotification(Map<String, String> data) {
        String version = data.get("version");
        String updateUrl = data.get("update_url");
        String changelog = data.get("changelog");
        
        NotificationManager.NotificationData notifData = 
            new NotificationManager.NotificationData(
                "新版本可用",
                "版本 " + version + " 现已推出"
            );
        
        Bundle extras = new Bundle();
        extras.putString("update_url", updateUrl);
        extras.putString("version", version);
        
        notifData.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
                 .setExtras(extras)
                 .setBigText(changelog);
        
        // 添加更新操作 - 使用EhViewer内置浏览器
        Intent updateIntent = new Intent(this, com.hippo.ehviewer.ui.WebViewActivity.class);
        updateIntent.putExtra(com.hippo.ehviewer.ui.WebViewActivity.EXTRA_URL, updateUrl);
        
        PendingIntent updatePendingIntent = PendingIntent.getActivity(
            this, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        NotificationManager.NotificationAction updateAction = 
            new NotificationManager.NotificationAction(
                R.drawable.ic_update, "立即更新", updatePendingIntent);
        
        notifData.actions = new NotificationManager.NotificationAction[]{updateAction};
        
        notificationManager.showNotification(notifData);
    }
    
    /**
     * 处理深度链接通知
     */
    private void handleDeepLinkNotification(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");
        String deepLink = data.get("deep_link");
        String params = data.get("params");
        
        NotificationManager.NotificationData notifData = 
            new NotificationManager.NotificationData(title, message);
        
        // 创建深度链接意图 - 使用EhViewer内置浏览器
        Intent deepLinkIntent = new Intent(this, com.hippo.ehviewer.ui.WebViewActivity.class);
        deepLinkIntent.putExtra(com.hippo.ehviewer.ui.WebViewActivity.EXTRA_URL, deepLink);
        
        if (params != null) {
            try {
                JSONObject jsonParams = new JSONObject(params);
                Bundle extras = new Bundle();
                for (java.util.Iterator<String> it = jsonParams.keys(); it.hasNext(); ) {
                    String key = it.next();
                    extras.putString(key, jsonParams.getString(key));
                }
                deepLinkIntent.putExtras(extras);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing params", e);
            }
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, deepLinkIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // 手动设置pendingIntent
        Bundle extras = new Bundle();
        extras.putParcelable("deep_link_intent", pendingIntent);
        notifData.setExtras(extras);
        
        notificationManager.showNotification(notifData);
    }
    
    /**
     * 显示默认通知
     */
    private void showDefaultNotification(String title, String message, String payload) {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                title != null ? title : "新通知",
                message != null ? message : "您有一条新消息"
            );
        
        if (payload != null) {
            try {
                JSONObject json = new JSONObject(payload);
                Bundle extras = new Bundle();
                for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
                    String key = it.next();
                    extras.putString(key, json.getString(key));
                }
                data.setExtras(extras);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing payload", e);
            }
        }
        
        notificationManager.showNotification(data);
    }
    
    /**
     * Token刷新
     * Firebase token更新处理
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // 将token发送到服务器
        sendTokenToServer(token);
    }
    
    /**
     * 发送Token到服务器
     */
    private void sendTokenToServer(String token) {
        // 实现将FCM token发送到后端服务器的逻辑
        // 这样服务器就可以向这个设备发送推送了
        
        // 保存到本地
        getSharedPreferences("push_config", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply();
    }
    
    /**
     * 从URL获取Bitmap
     */
    private Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 根据扩展名获取MIME类型
     */
    private String getMimeTypeFromExtension(String extension) {
        if (extension == null) return "*/*";
        
        switch (extension.toLowerCase()) {
            case "pdf": return "application/pdf";
            case "epub": return "application/epub+zip";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp4": return "video/mp4";
            case "txt": return "text/plain";
            case "html": return "text/html";
            default: return "*/*";
        }
    }
}