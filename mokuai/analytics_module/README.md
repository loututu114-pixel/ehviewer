# EhViewer 统计分析模块 (Analytics Module)

## 概述

统计分析模块为EhViewer应用提供完整的用户行为分析、应用统计和事件跟踪功能。该模块支持多种分析服务提供商，提供了统一的分析接口。

## 主要功能

### 1. 事件跟踪
- 用户行为事件跟踪
- 页面访问统计
- 搜索行为分析
- 下载行为记录
- 错误统计

### 2. 用户分析
- 用户属性设置
- 用户ID管理
- 用户行为模式分析
- 用户活跃度统计

### 3. 性能监控
- 应用启动时间
- 页面加载时间
- 网络请求性能
- 内存使用情况

### 4. 会话管理
- 会话开始/结束跟踪
- 会话时长统计
- 用户留存分析

### 5. 自定义分析
- 自定义事件定义
- 自定义属性设置
- 灵活的参数传递

## 核心类

### AnalyticsManager - 统计分析管理器
```java
public class AnalyticsManager {
    // 获取单例实例
    public static AnalyticsManager getInstance(Context context)

    // 事件记录
    public void logEvent(String eventName)
    public void logEvent(String eventName, Bundle parameters)

    // 用户行为记录
    public void logUserAction(String action, String category)
    public void logScreenView(String screenName)
    public void logSearch(String query, String category)
    public void logDownload(String itemId, String itemName, long fileSize)
    public void logError(String errorType, String errorMessage)

    // 用户属性设置
    public void setUserProperty(String propertyName, String propertyValue)
    public void setUserId(String userId)

    // 应用生命周期
    public void logAppStart()
    public void logAppStop()
    public void logSessionStart()
    public void logSessionEnd(long duration)

    // 性能监控
    public void logPerformance(String metricName, long value)
    public void logNetworkRequest(String url, long responseTime, int statusCode)

    // 配置管理
    public void setEnabled(boolean enabled)
    public boolean isEnabled()
}
```

### AnalyticsProvider - 分析服务提供者接口
```java
public interface AnalyticsProvider {
    void logEvent(String eventName, Bundle parameters);
    void setUserProperty(String propertyName, String propertyValue);
    void setUserId(String userId);
    void setScreenName(String screenName);
}
```

## 使用方法

### 基本设置

```java
// 获取分析管理器
AnalyticsManager analytics = AnalyticsManager.getInstance(context);

// 设置分析服务提供者（可选）
analytics.setProvider(new FirebaseAnalyticsProvider());

// 启用/禁用分析
analytics.setEnabled(true);

// 设置用户ID
analytics.setUserId("user123");

// 设置用户属性
analytics.setUserProperty("user_type", "premium");
```

### 事件跟踪

```java
AnalyticsManager analytics = AnalyticsManager.getInstance(context);

// 记录用户行为
analytics.logUserAction("button_click", "main_screen");

// 记录页面访问
analytics.logScreenView("GalleryListActivity");

// 记录搜索
analytics.logSearch("漫画", "gallery");

// 记录下载
analytics.logDownload("12345", "Sample Manga", 1024 * 1024);

// 记录错误
analytics.logError("network_error", "Connection timeout");

// 自定义事件
Bundle params = new Bundle();
params.putString("item_type", "manga");
params.putInt("item_count", 5);
analytics.logEvent("item_view", params);
```

### 应用生命周期跟踪

```java
public class MyApplication extends Application {
    private long sessionStartTime;

    @Override
    public void onCreate() {
        super.onCreate();

        AnalyticsManager analytics = AnalyticsManager.getInstance(this);

        // 记录应用启动
        analytics.logAppStart();

        // 记录会话开始
        sessionStartTime = System.currentTimeMillis();
        analytics.logSessionStart();
    }

    // 在合适的地方记录会话结束
    public void onAppStop() {
        AnalyticsManager analytics = AnalyticsManager.getInstance(this);
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        analytics.logSessionEnd(sessionDuration);
        analytics.logAppStop();
    }
}
```

### 性能监控

```java
AnalyticsManager analytics = AnalyticsManager.getInstance(context);

// 记录页面加载时间
long startTime = System.currentTimeMillis();
// ... 执行页面加载逻辑 ...
long loadTime = System.currentTimeMillis() - startTime;
analytics.logPerformance("page_load_time", loadTime);

// 记录网络请求性能
long requestStartTime = System.currentTimeMillis();
// ... 执行网络请求 ...
long responseTime = System.currentTimeMillis() - requestStartTime;
analytics.logNetworkRequest("https://api.example.com/data", responseTime, 200);
```

### Firebase集成示例

```java
public class FirebaseAnalyticsProvider implements AnalyticsProvider {
    private final FirebaseAnalytics mFirebaseAnalytics;

    public FirebaseAnalyticsProvider() {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(FirebaseApp.getInstance().getApplicationContext());
    }

    @Override
    public void logEvent(String eventName, Bundle parameters) {
        mFirebaseAnalytics.logEvent(eventName, parameters);
    }

    @Override
    public void setUserProperty(String propertyName, String propertyValue) {
        mFirebaseAnalytics.setUserProperty(propertyName, propertyValue);
    }

    @Override
    public void setUserId(String userId) {
        mFirebaseAnalytics.setUserId(userId);
    }

    @Override
    public void setScreenName(String screenName) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
    }
}
```

## 依赖项

在你的`build.gradle`文件中添加：

```gradle
dependencies {
    // Firebase Analytics (可选)
    implementation 'com.google.firebase:firebase-analytics:21.2.0'

    // 或者其他分析服务提供商
    // implementation 'com.amplitude:android-sdk:2.36.1'
}
```

## 权限配置

在`AndroidManifest.xml`中添加网络权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 隐私合规

### GDPR合规
```java
// 检查用户同意状态
boolean userConsented = checkUserConsent();
analytics.setEnabled(userConsented);

// 敏感数据处理
analytics.logEvent("user_action", sanitizeParameters(parameters));
```

### 数据收集控制
```java
// 禁用特定类型的数据收集
analytics.setDataCollectionEnabled("advertising", false);

// 匿名化用户数据
analytics.setAnonymizeIp(true);
```

## 最佳实践

1. **事件命名规范**: 使用清晰、有意义的英文名称
2. **参数限制**: 控制事件参数的数量和大小
3. **性能考虑**: 避免在性能关键路径上记录过多事件
4. **隐私保护**: 遵守相关隐私法规，不收集敏感信息
5. **测试验证**: 在开发阶段验证事件记录的正确性

## 调试和测试

```java
// 启用调试模式
analytics.setDebugMode(true);

// 验证事件记录
analytics.setEventValidator(new EventValidator() {
    @Override
    public boolean validate(String eventName, Bundle parameters) {
        // 验证事件名称和参数的合法性
        return isValidEvent(eventName) && areValidParameters(parameters);
    }
});
```

## 示例项目

查看完整的示例代码：
- `AnalyticsIntegrationActivity.java` - 分析集成示例
- `CustomAnalyticsProvider.java` - 自定义分析提供者示例
- `PrivacyComplianceActivity.java` - 隐私合规示例

## 注意事项

1. **性能影响**: 分析功能可能会对应用性能产生轻微影响
2. **网络消耗**: 事件上传会消耗网络流量
3. **存储使用**: 本地事件缓存会占用存储空间
4. **隐私考虑**: 确保遵守相关隐私保护法规
5. **准确性**: 验证事件记录的准确性和完整性

## 许可证

本模块遵循Apache License 2.0协议。
