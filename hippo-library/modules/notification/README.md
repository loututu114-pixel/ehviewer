# 🔔 通知管理模块 (Notification Module)

## 🎯 概述

Android Library通知管理模块提供完整的通知管理功能，包括系统通知、本地提醒、推送消息处理等，帮助开发者轻松实现丰富的通知体验。

## ✨ 主要特性

- ✅ **系统通知**: 创建和管理系统通知栏消息
- ✅ **本地提醒**: 定时提醒和闹钟功能
- ✅ **推送处理**: FCM和其他推送服务的集成
- ✅ **通知分组**: 支持通知分组和渠道管理
- ✅ **自定义样式**: 丰富的通知样式和布局
- ✅ **交互处理**: 处理通知点击和操作
- ✅ **权限管理**: 通知权限申请和管理
- ✅ **统计分析**: 通知送达率和交互统计

## 🚀 快速开始

### 初始化通知管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化通知管理器
        NotificationManager.initialize(this);

        // 创建通知渠道（Android 8.0+）
        NotificationManager.getInstance().createNotificationChannel(
            "default",
            "默认通知",
            "应用默认通知渠道",
            NotificationManager.IMPORTANCE_DEFAULT
        );
    }
}
```

### 发送简单通知

```java
// 发送简单文本通知
NotificationManager.getInstance().showNotification(
    "标题",
    "这是一条通知消息",
    R.drawable.ic_notification
);
```

### 发送高级通知

```java
// 创建高级通知
NotificationCompat.Builder builder = NotificationManager.getInstance()
    .createNotificationBuilder("default")
    .setContentTitle("高级通知")
    .setContentText("这是高级通知的内容")
    .setSmallIcon(R.drawable.ic_notification)
    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_large_icon))
    .setAutoCancel(true)
    .setPriority(NotificationCompat.PRIORITY_HIGH);

// 添加操作按钮
builder.addAction(R.drawable.ic_reply, "回复",
    createReplyPendingIntent(messageId));

// 显示通知
NotificationManager.getInstance().showNotification(1, builder.build());
```

### 设置定时提醒

```java
// 设置一次性定时提醒
NotificationManager.getInstance().scheduleNotification(
    "提醒标题",
    "提醒内容",
    System.currentTimeMillis() + 3600000, // 1小时后
    R.drawable.ic_alarm
);

// 设置重复提醒
NotificationManager.getInstance().scheduleRepeatingNotification(
    "每日提醒",
    "这是每日提醒内容",
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
    }.timeInMillis,
    AlarmManager.INTERVAL_DAY, // 每日重复
    R.drawable.ic_daily
);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `NotificationManager` | 通知管理器核心类 |
| `NotificationConfig` | 通知配置类 |
| `NotificationChannel` | 通知渠道类 |
| `ScheduledNotification` | 定时通知类 |

### 主要方法

#### NotificationManager

```java
// 初始化管理器
void initialize(Context context)

// 获取单例实例
NotificationManager getInstance()

// 创建通知渠道
void createNotificationChannel(String id, String name, String description, int importance)

// 显示简单通知
void showNotification(String title, String message, int iconResId)

// 显示高级通知
void showNotification(int id, Notification notification)

// 创建通知构建器
NotificationCompat.Builder createNotificationBuilder(String channelId)

// 取消通知
void cancelNotification(int id)

// 取消所有通知
void cancelAllNotifications()

// 设置定时通知
void scheduleNotification(String title, String message, long triggerAtMillis, int iconResId)

// 设置重复通知
void scheduleRepeatingNotification(String title, String message, long triggerAtMillis, long intervalMillis, int iconResId)

// 取消定时通知
void cancelScheduledNotification(int id)

// 检查通知权限
boolean hasNotificationPermission()

// 请求通知权限
void requestNotificationPermission(Activity activity)
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableVibration` | `boolean` | `true` | 是否启用振动 |
| `enableSound` | `boolean` | `true` | 是否启用声音 |
| `enableLights` | `boolean` | `true` | 是否启用LED灯 |
| `maxNotifications` | `int` | `50` | 最大通知数量 |
| `autoCancel` | `boolean` | `true` | 点击后自动取消 |
| `showTimestamp` | `boolean` | `true` | 显示时间戳 |

## 📦 依赖项

```gradle
dependencies {
    // AndroidX Core
    implementation 'androidx.core:core:1.12.0'

    // Android Library通知模块
    implementation 'com.hippo.library:notification:1.0.0'
}
```

## 🧪 测试

### 通知功能测试

```java
@Test
public void testNotificationManager_showNotification_shouldDisplayNotification() {
    // Given
    NotificationManager manager = NotificationManager.getInstance();
    String title = "Test Title";
    String message = "Test Message";

    // When
    manager.showNotification(title, message, R.drawable.ic_test);

    // Then
    // 验证通知是否正确显示（需要UI测试框架）
}
```

### 定时通知测试

```java
@Test
public void testScheduledNotification() {
    // 测试定时通知功能
    // 1. 设置定时通知
    // 2. 等待触发时间
    // 3. 验证通知是否按时显示
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### Android版本兼容性
- Android 8.0+ (API 26): 需要创建通知渠道
- Android 13+ (API 33): 需要请求通知权限
- 不同版本的通知样式和功能差异

### 性能考虑
- 避免频繁创建通知
- 合理使用通知渠道
- 及时清理过期通知

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
