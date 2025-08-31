package com.hippo.ehviewer.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.MainActivity;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 通知管理器
 * 管理所有类型的推送通知
 */
public class NotificationManager {
    
    private static final String TAG = "NotificationManager";
    private static NotificationManager instance;
    
    // 通知渠道
    public static final String CHANNEL_DEFAULT = "default";
    public static final String CHANNEL_NEWS = "news";
    public static final String CHANNEL_MESSAGE = "message";
    public static final String CHANNEL_SYSTEM = "system";
    public static final String CHANNEL_FILE = "file_open";
    public static final String CHANNEL_MONITOR = "monitor";
    
    // 通知类型
    public enum NotificationType {
        NEWS("news", "新闻通知", CHANNEL_NEWS),
        MESSAGE("message", "消息通知", CHANNEL_MESSAGE),
        SYSTEM_ALERT("system", "系统提醒", CHANNEL_SYSTEM),
        FILE_OPEN("file", "文件打开", CHANNEL_FILE),
        CPU_ALERT("cpu", "CPU监控", CHANNEL_MONITOR),
        MEMORY_ALERT("memory", "内存监控", CHANNEL_MONITOR),
        CUSTOM("custom", "自定义通知", CHANNEL_DEFAULT);
        
        private final String type;
        private final String title;
        private final String channel;
        
        NotificationType(String type, String title, String channel) {
            this.type = type;
            this.title = title;
            this.channel = channel;
        }
    }
    
    private Context context;
    private NotificationManagerCompat notificationManager;
    private Map<String, NotificationChannel> channels;
    
    private NotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManagerCompat.from(context);
        this.channels = new HashMap<>();
        
        createNotificationChannels();
    }
    
    public static synchronized NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context);
        }
        return instance;
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 默认渠道
            createChannel(CHANNEL_DEFAULT, "默认通知", 
                android.app.NotificationManager.IMPORTANCE_DEFAULT);
            
            // 新闻渠道
            createChannel(CHANNEL_NEWS, "新闻推送", 
                android.app.NotificationManager.IMPORTANCE_DEFAULT);
            
            // 消息渠道
            createChannel(CHANNEL_MESSAGE, "消息通知", 
                android.app.NotificationManager.IMPORTANCE_HIGH);
            
            // 系统渠道
            createChannel(CHANNEL_SYSTEM, "系统提醒", 
                android.app.NotificationManager.IMPORTANCE_HIGH);
            
            // 文件打开渠道
            createChannel(CHANNEL_FILE, "文件处理", 
                android.app.NotificationManager.IMPORTANCE_DEFAULT);
            
            // 监控渠道
            createChannel(CHANNEL_MONITOR, "性能监控", 
                android.app.NotificationManager.IMPORTANCE_HIGH);
        }
    }
    
    /**
     * 创建单个渠道
     */
    private void createChannel(String id, String name, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            channel.setDescription(name + "相关通知");
            channel.enableLights(true);
            channel.enableVibration(true);
            
            android.app.NotificationManager manager = 
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            
            channels.put(id, channel);
        }
    }
    
    /**
     * 显示通知
     */
    public void showNotification(NotificationData data) {
        NotificationCompat.Builder builder = createNotificationBuilder(data);
        
        // 设置通知内容
        builder.setContentTitle(data.title)
               .setContentText(data.message)
               .setSmallIcon(R.drawable.ic_notification)
               .setAutoCancel(true)
               .setPriority(data.priority);
        
        // 设置大图标
        if (data.largeIcon != null) {
            builder.setLargeIcon(data.largeIcon);
        }
        
        // 设置大文本样式
        if (data.bigText != null) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(data.bigText));
        }
        
        // 设置大图片样式
        if (data.bigPicture != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                .bigPicture(data.bigPicture)
                .setBigContentTitle(data.title));
        }
        
        // 设置声音
        if (data.soundUri != null) {
            builder.setSound(data.soundUri);
        } else {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }
        
        // 设置点击动作
        PendingIntent pendingIntent = createPendingIntent(data);
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }
        
        // 添加操作按钮
        if (data.actions != null) {
            for (NotificationAction action : data.actions) {
                builder.addAction(action.icon, action.title, action.pendingIntent);
            }
        }
        
        // 显示通知
        int notificationId = data.id != 0 ? data.id : new Random().nextInt(10000);
        notificationManager.notify(data.tag, notificationId, builder.build());
    }
    
    /**
     * 创建通知构建器
     */
    private NotificationCompat.Builder createNotificationBuilder(NotificationData data) {
        String channelId = data.channelId != null ? data.channelId : CHANNEL_DEFAULT;
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        
        // 设置通知时间
        builder.setWhen(System.currentTimeMillis());
        builder.setShowWhen(true);
        
        // 设置通知颜色
        builder.setColor(context.getResources().getColor(R.color.colorPrimary));
        
        return builder;
    }
    
    /**
     * 创建点击意图
     */
    private PendingIntent createPendingIntent(NotificationData data) {
        Intent intent = null;
        
        // 根据通知类型创建不同的意图
        switch (data.type) {
            case NEWS:
                intent = createNewsIntent(data);
                break;
                
            case MESSAGE:
                intent = createMessageIntent(data);
                break;
                
            case FILE_OPEN:
                intent = createFileOpenIntent(data);
                break;
                
            case CPU_ALERT:
            case MEMORY_ALERT:
                intent = createMonitorIntent(data);
                break;
                
            case SYSTEM_ALERT:
            case CUSTOM:
            default:
                intent = createDefaultIntent(data);
                break;
        }
        
        if (intent == null) {
            intent = new Intent(context, MainActivity.class);
        }
        
        // 添加额外数据
        if (data.extras != null) {
            intent.putExtras(data.extras);
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        return PendingIntent.getActivity(context, 
            data.requestCode != 0 ? data.requestCode : new Random().nextInt(10000),
            intent, flags);
    }
    
    /**
     * 创建新闻意图
     */
    private Intent createNewsIntent(NotificationData data) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("com.hippo.ehviewer.ACTION_VIEW_NEWS");
        intent.putExtra("news_id", data.extras != null ? 
            data.extras.getString("news_id") : "");
        intent.putExtra("news_url", data.extras != null ? 
            data.extras.getString("news_url") : "");
        return intent;
    }
    
    /**
     * 创建消息意图
     */
    private Intent createMessageIntent(NotificationData data) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("com.hippo.ehviewer.ACTION_VIEW_MESSAGE");
        intent.putExtra("message_id", data.extras != null ? 
            data.extras.getString("message_id") : "");
        intent.putExtra("sender", data.extras != null ? 
            data.extras.getString("sender") : "");
        return intent;
    }
    
    /**
     * 创建文件打开意图
     */
    private Intent createFileOpenIntent(NotificationData data) {
        if (data.extras != null && data.extras.containsKey("file_path")) {
            String filePath = data.extras.getString("file_path");
            String mimeType = data.extras.getString("mime_type", "*/*");
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri fileUri = Uri.parse("file://" + filePath);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // 指定使用我们的应用打开
            intent.setPackage(context.getPackageName());
            
            return intent;
        }
        return null;
    }
    
    /**
     * 创建监控意图
     */
    private Intent createMonitorIntent(NotificationData data) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("com.hippo.ehviewer.ACTION_VIEW_MONITOR");
        intent.putExtra("monitor_type", data.type.type);
        return intent;
    }
    
    /**
     * 创建默认意图
     */
    private Intent createDefaultIntent(NotificationData data) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("com.hippo.ehviewer.ACTION_NOTIFICATION");
        return intent;
    }
    
    /**
     * 取消通知
     */
    public void cancelNotification(int id) {
        notificationManager.cancel(id);
    }
    
    /**
     * 取消所有通知
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
    
    /**
     * 检查通知权限
     */
    public boolean areNotificationsEnabled() {
        return notificationManager.areNotificationsEnabled();
    }
    
    /**
     * 通知数据类
     */
    public static class NotificationData {
        public int id;
        public String tag;
        public String title;
        public String message;
        public String bigText;
        public Bitmap largeIcon;
        public Bitmap bigPicture;
        public Uri soundUri;
        public NotificationType type = NotificationType.CUSTOM;
        public String channelId;
        public int priority = NotificationCompat.PRIORITY_DEFAULT;
        public int requestCode;
        public android.os.Bundle extras;
        public NotificationAction[] actions;
        
        public NotificationData(String title, String message) {
            this.title = title;
            this.message = message;
        }
        
        // Builder模式
        public NotificationData setType(NotificationType type) {
            this.type = type;
            this.channelId = type.channel;
            return this;
        }
        
        public NotificationData setPriority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public NotificationData setExtras(android.os.Bundle extras) {
            this.extras = extras;
            return this;
        }
        
        public NotificationData setBigText(String bigText) {
            this.bigText = bigText;
            return this;
        }
        
        public NotificationData setBigPicture(Bitmap bigPicture) {
            this.bigPicture = bigPicture;
            return this;
        }
    }
    
    /**
     * 通知操作类
     */
    public static class NotificationAction {
        public int icon;
        public String title;
        public PendingIntent pendingIntent;
        
        public NotificationAction(int icon, String title, PendingIntent pendingIntent) {
            this.icon = icon;
            this.title = title;
            this.pendingIntent = pendingIntent;
        }
    }
}