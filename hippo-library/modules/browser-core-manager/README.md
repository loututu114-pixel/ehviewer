# 🌐 浏览器核心管理器模块 (Browser Core Manager Module)

## 🎯 概述

Android Library浏览器核心管理器提供完整的浏览器核心控制和管理功能，基于腾讯X5内核和YCWebView最佳实践，打造企业级浏览器解决方案。该模块是整个浏览器架构的核心控制器，负责协调和管理各种浏览器组件。

## ✨ 主要特性

- ✅ **核心控制**: 统一的浏览器核心控制中心
- ✅ **组件协调**: 协调WebView池、渲染引擎、缓存管理器等
- ✅ **性能监控**: 实时监控浏览器性能指标
- ✅ **错误处理**: 统一的错误检测和恢复机制
- ✅ **资源管理**: 智能管理浏览器资源分配
- ✅ **策略优化**: 根据内容类型进行智能优化
- ✅ **生命周期管理**: 完整的浏览器生命周期控制
- ✅ **扩展支持**: 支持插件化和功能扩展

## 🚀 快速开始

### 初始化浏览器核心管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化浏览器核心管理器
        BrowserCoreManager.initialize(this);
    }
}
```

### 基本使用

```java
// 获取浏览器核心管理器实例
BrowserCoreManager browserManager = BrowserCoreManager.getInstance();

// 创建WebView
WebView webView = browserManager.acquireWebView();

// 加载网页前进行优化
String url = "https://example.com";
browserManager.optimizeForUrl(webView, url);

// 加载网页
webView.loadUrl(url);

// 使用完毕后释放
browserManager.releaseWebView(webView);
```

### 高级配置

```java
// 配置浏览器核心参数
BrowserConfig config = new BrowserConfig.Builder()
    .enableHardwareAcceleration(true)
    .setWebViewPoolSize(3)
    .enablePreloading(true)
    .setCacheSize(50 * 1024 * 1024) // 50MB
    .enableSecurityChecks(true)
    .build();

// 应用配置
browserManager.setConfig(config);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `BrowserCoreManager` | 浏览器核心管理器主类 |
| `BrowserConfig` | 浏览器配置类 |
| `WebViewController` | WebView控制器 |
| `PerformanceMonitor` | 性能监控器 |

### 主要方法

#### BrowserCoreManager

```java
// 初始化浏览器核心
void initialize(Context context)

// 获取单例实例
BrowserCoreManager getInstance()

// 获取WebView实例
WebView acquireWebView()

// 释放WebView实例
void releaseWebView(WebView webView)

// 为URL进行优化
void optimizeForUrl(WebView webView, String url)

// 获取浏览器统计信息
BrowserStats getStats()

// 设置浏览器配置
void setConfig(BrowserConfig config)

// 获取当前配置
BrowserConfig getConfig()

// 清理所有资源
void cleanup()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableHardwareAcceleration` | `boolean` | `true` | 是否启用硬件加速 |
| `webViewPoolSize` | `int` | `3` | WebView连接池大小 |
| `enablePreloading` | `boolean` | `true` | 是否启用预加载 |
| `cacheSize` | `long` | `50MB` | 缓存大小 |
| `enableSecurityChecks` | `boolean` | `true` | 是否启用安全检查 |
| `enablePerformanceMonitoring` | `boolean` | `true` | 是否启用性能监控 |

## 📦 依赖项

```gradle
dependencies {
    // 浏览器核心模块
    implementation 'com.hippo.ehviewer:browser-core-manager:1.0.0'

    // 相关模块
    implementation 'com.hippo.ehviewer:webview-pool-manager:1.0.0'
    implementation 'com.hippo.ehviewer:render-engine-manager:1.0.0'
    implementation 'com.hippo.ehviewer:browser-cache-manager:1.0.0'
    implementation 'com.hippo.ehviewer:performance-monitor:1.0.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 兼容性
- **最低版本**: Android API 21 (Android 5.0)
- **目标版本**: Android API 34 (Android 14)
- **编译版本**: Android API 34

### 性能优化
- 合理设置WebView池大小，避免资源浪费
- 根据设备性能调整硬件加速设置
- 定期清理缓存，避免存储空间不足

## 🧪 测试

### 单元测试
```java
@Test
public void testBrowserCoreManager_acquireWebView_shouldReturnValidWebView() {
    // Given
    BrowserCoreManager manager = BrowserCoreManager.getInstance();

    // When
    WebView webView = manager.acquireWebView();

    // Then
    assertNotNull(webView);
    assertTrue(webView instanceof WebView);
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4::class)
public class BrowserCoreIntegrationTest {

    @Test
    public void testFullBrowserFlow() {
        // 测试完整的浏览器流程
        // 1. 初始化浏览器核心
        // 2. 获取WebView实例
        // 3. 加载网页并优化
        // 4. 监控性能指标
        // 5. 释放资源
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingBrowserCore`)
3. 提交更改 (`git commit -m 'Add some AmazingBrowserCore'`)
4. 推送到分支 (`git push origin feature/AmazingBrowserCore`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/browser-core-manager/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
