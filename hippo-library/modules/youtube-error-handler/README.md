# 🎥 YouTube错误处理器模块 (YouTube Error Handler Module)

## 🎯 概述

Android Library YouTube错误处理器专门解决YouTube访问中的403错误和其他访问限制问题，提供智能的重试机制和User-Agent轮换策略。该模块基于腾讯X5和YCWebView的最佳实践，实现了企业级的YouTube访问解决方案。

## ✨ 主要特性

- ✅ **智能错误检测**: 自动识别403和其他访问拒绝错误
- ✅ **多层User-Agent策略**: 6种不同的User-Agent策略
- ✅ **延迟重试机制**: 智能延迟避免检测
- ✅ **重定向循环防护**: 检测和打破重定向循环
- ✅ **性能监控**: 实时监控访问成功率
- ✅ **自动恢复**: 无需用户干预的自动错误恢复
- ✅ **统计报告**: 详细的错误处理统计
- ✅ **扩展支持**: 轻松支持其他视频网站

## 🚀 快速开始

### 初始化YouTube错误处理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化YouTube错误处理器
        YouTubeErrorHandler.initialize(this);
    }
}
```

### WebView集成

```java
// 在WebView中集成错误处理器
public class MyWebView extends WebView {

    public MyWebView(Context context) {
        super(context);
        setupErrorHandler();
    }

    private void setupErrorHandler() {
        // 设置WebViewClient
        setWebViewClient(new YouTubeWebViewClient());

        // 启用YouTube错误处理
        YouTubeErrorHandler.getInstance().enableForWebView(this);
    }
}

// 自定义WebViewClient
public class YouTubeWebViewClient extends WebViewClient {

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        // 处理错误
        if (YouTubeErrorHandler.getInstance().shouldHandleError(failingUrl, errorCode)) {
            YouTubeErrorHandler.getInstance().handleError(view, errorCode, description, failingUrl);
        } else {
            // 处理其他错误
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        // 处理HTTP错误
        if (errorResponse.getStatusCode() == 403) {
            YouTubeErrorHandler.getInstance().handle403Error(view, request.getUrl().toString());
        }
    }
}
```

### 高级配置

```java
// 配置错误处理器参数
YouTubeConfig config = new YouTubeConfig.Builder()
    .setMaxRetryAttempts(6)           // 最大重试次数
    .setRetryDelayFirst(1000)         // 首次重试延迟(ms)
    .setRetryDelaySubsequent(1500)    // 后续重试延迟(ms)
    .enableDebugLogging(true)         // 启用调试日志
    .setSuccessThreshold(0.8f)        // 成功率阈值
    .enableAutoRecovery(true)         // 启用自动恢复
    .build();

// 应用配置
YouTubeErrorHandler.getInstance().setConfig(config);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `YouTubeErrorHandler` | YouTube错误处理器主类 |
| `YouTubeConfig` | 配置类 |
| `YouTubeWebViewClient` | 专用WebViewClient |
| `ErrorStats` | 错误统计信息 |

### 主要方法

#### YouTubeErrorHandler

```java
// 初始化错误处理器
void initialize(Context context)

// 获取单例实例
YouTubeErrorHandler getInstance()

// 为WebView启用错误处理
void enableForWebView(WebView webView)

// 检查是否应该处理错误
boolean shouldHandleError(String url, int errorCode)

// 处理错误
boolean handleError(WebView webView, int errorCode, String description, String failingUrl)

// 处理403错误
void handle403Error(WebView webView, String url)

// 获取恢复User-Agent
String getRecoveryUserAgent(String url)

// 记录错误统计
void recordError(String url, int errorCode, boolean recovered)

// 获取错误统计
ErrorStats getStats()

// 重置统计数据
void resetStats()

// 设置配置
void setConfig(YouTubeConfig config)

// 获取当前配置
YouTubeConfig getConfig()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `maxRetryAttempts` | `int` | `6` | 最大重试次数 |
| `retryDelayFirst` | `long` | `1000` | 首次重试延迟(毫秒) |
| `retryDelaySubsequent` | `long` | `1500` | 后续重试延迟(毫秒) |
| `enableDebugLogging` | `boolean` | `false` | 是否启用调试日志 |
| `successThreshold` | `float` | `0.8f` | 成功率阈值 |
| `enableAutoRecovery` | `boolean` | `true` | 是否启用自动恢复 |
| `enableStatsTracking` | `boolean` | `true` | 是否启用统计跟踪 |

## 📦 依赖项

```gradle
dependencies {
    // YouTube错误处理器模块
    implementation 'com.hippo.ehviewer:youtube-error-handler:1.0.0'

    // 相关模块
    implementation 'com.hippo.ehviewer:user-agent-manager:1.0.0'
    implementation 'com.hippo.ehviewer:youtube-compatibility-manager:1.0.0'
}
```

## ⚠️ 注意事项

### 错误处理策略
- 只对YouTube相关域名启用特殊处理
- 避免过度重试造成服务器压力
- 智能延迟策略防止被识别为攻击

### User-Agent管理
- 使用经过验证的User-Agent字符串
- 支持多种浏览器的UA策略
- 定期更新UA库以适应变化

### 性能考虑
- 最小化重试对用户体验的影响
- 后台处理错误恢复逻辑
- 智能缓存成功策略

## 🧪 测试

### 错误处理测试
```java
@Test
public void testYouTubeErrorHandler_403Error_shouldTriggerRecovery() {
    // Given
    YouTubeErrorHandler handler = YouTubeErrorHandler.getInstance();
    WebView mockWebView = mock(WebView.class);
    String youtubeUrl = "https://www.youtube.com/watch?v=test";

    // When
    boolean handled = handler.handle403Error(mockWebView, youtubeUrl);

    // Then
    assertTrue(handled);
    verify(mockWebView).getSettings();
    // 验证UA切换逻辑
}
```

### 重试机制测试
```java
@Test
public void testYouTubeErrorHandler_retryLogic_shouldFollowStrategy() {
    // Given
    YouTubeErrorHandler handler = YouTubeErrorHandler.getInstance();
    String url = "https://youtube.com/watch?v=test";

    // When - 模拟多次失败
    for (int i = 0; i < 3; i++) {
        handler.recordError(url, 403, false);
    }

    // Then - 验证重试策略
    String recoveryUA = handler.getRecoveryUserAgent(url);
    assertNotNull(recoveryUA);
    assertNotEquals("", recoveryUA.trim());
}
```

### 统计功能测试
```java
@Test
public void testYouTubeErrorHandler_stats_shouldTrackCorrectly() {
    // Given
    YouTubeErrorHandler handler = YouTubeErrorHandler.getInstance();

    // When - 记录一些错误和成功
    handler.recordError("https://youtube.com/1", 403, true);
    handler.recordError("https://youtube.com/2", 403, false);
    handler.recordError("https://youtube.com/3", 403, true);

    // Then - 验证统计数据
    ErrorStats stats = handler.getStats();
    assertEquals(3, stats.getTotalErrors());
    assertEquals(2, stats.getRecoveredErrors());
    assertEquals(66.7f, stats.getRecoveryRate(), 0.1f);
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4::class)
public class YouTubeErrorHandlerIntegrationTest {

    @Test
    public void testFullErrorHandlingFlow() {
        // 1. 初始化错误处理器
        // 2. 创建WebView并启用错误处理
        // 3. 模拟403错误场景
        // 4. 验证UA切换和重试逻辑
        // 5. 确认最终访问成功
        // 6. 检查统计数据准确性
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingYouTubeHandler`)
3. 提交更改 (`git commit -m 'Add some AmazingYouTubeHandler'`)
4. 推送到分支 (`git push origin feature/AmazingYouTubeHandler`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/youtube-error-handler/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
