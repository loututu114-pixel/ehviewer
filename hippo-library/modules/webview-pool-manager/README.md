# 🏊 WebView连接池管理器模块 (WebView Pool Manager Module)

## 🎯 概述

Android Library WebView连接池管理器提供高效的WebView实例管理和复用机制，通过连接池技术显著提升浏览器性能和用户体验。该模块解决了WebView创建和销毁的性能开销问题，实现WebView的智能缓存和复用。

## ✨ 主要特性

- ✅ **连接池管理**: 高效的WebView实例池化管理
- ✅ **智能复用**: 根据使用频率智能复用WebView实例
- ✅ **内存优化**: 自动清理和内存管理，防止内存泄漏
- ✅ **状态重置**: 完整的状态清理和重置机制
- ✅ **性能监控**: 实时监控连接池性能指标
- ✅ **自动扩容**: 根据负载自动调整池大小
- ✅ **优雅降级**: 在资源不足时提供降级策略
- ✅ **并发安全**: 线程安全的连接池操作

## 🚀 快速开始

### 初始化连接池管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化WebView连接池
        WebViewPoolManager.initialize(this);
    }
}
```

### 基本使用

```java
// 获取连接池管理器实例
WebViewPoolManager poolManager = WebViewPoolManager.getInstance();

// 从池中获取WebView
WebView webView = poolManager.acquire();

// 使用WebView加载网页
webView.loadUrl("https://example.com");

// 使用完毕后归还给池
poolManager.release(webView);
```

### 高级配置

```java
// 配置连接池参数
WebViewPoolConfig config = new WebViewPoolConfig.Builder()
    .setCorePoolSize(2)           // 核心池大小
    .setMaxPoolSize(5)            // 最大池大小
    .setKeepAliveTime(300000)     // 空闲保持时间(5分钟)
    .enablePreCreation(true)      // 启用预创建
    .setPreCreateCount(2)         // 预创建2个实例
    .enableMemoryMonitoring(true) // 启用内存监控
    .build();

// 应用配置
poolManager.setConfig(config);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `WebViewPoolManager` | WebView连接池管理器主类 |
| `WebViewPoolConfig` | 连接池配置类 |
| `PooledWebView` | 池化WebView封装类 |
| `PoolStats` | 连接池统计信息 |

### 主要方法

#### WebViewPoolManager

```java
// 初始化连接池
void initialize(Context context)

// 获取单例实例
WebViewPoolManager getInstance()

// 获取WebView实例
WebView acquire()

// 获取指定配置的WebView
WebView acquire(WebViewConfig config)

// 释放WebView实例
void release(WebView webView)

// 获取连接池统计信息
PoolStats getStats()

// 清理连接池
void clear()

// 关闭连接池
void shutdown()

// 设置连接池配置
void setConfig(WebViewPoolConfig config)

// 获取当前配置
WebViewPoolConfig getConfig()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `corePoolSize` | `int` | `2` | 核心池大小 |
| `maxPoolSize` | `int` | `5` | 最大池大小 |
| `keepAliveTime` | `long` | `300000` | 空闲保持时间(毫秒) |
| `enablePreCreation` | `boolean` | `true` | 是否启用预创建 |
| `preCreateCount` | `int` | `2` | 预创建实例数量 |
| `enableMemoryMonitoring` | `boolean` | `true` | 是否启用内存监控 |
| `enableAutoCleanup` | `boolean` | `true` | 是否启用自动清理 |

## 📦 依赖项

```gradle
dependencies {
    // WebView连接池管理器模块
    implementation 'com.hippo.ehviewer:webview-pool-manager:1.0.0'
}
```

## ⚠️ 注意事项

### 内存管理
- 合理设置池大小，避免过度占用内存
- 及时释放不再使用的WebView实例
- 监控内存使用情况，防止内存泄漏

### 性能优化
- 根据设备性能调整池大小
- 启用预创建可以提升首次加载速度
- 定期清理空闲实例释放资源

### 线程安全
- 所有池操作都是线程安全的
- 支持并发获取和释放WebView实例
- 无需额外的同步处理

## 🧪 测试

### 连接池测试
```java
@Test
public void testWebViewPoolManager_acquireAndRelease_shouldWorkCorrectly() {
    // Given
    WebViewPoolManager poolManager = WebViewPoolManager.getInstance();

    // When
    WebView webView1 = poolManager.acquire();
    WebView webView2 = poolManager.acquire();

    // Then
    assertNotNull(webView1);
    assertNotNull(webView2);
    assertNotSame(webView1, webView2);

    // 释放WebView
    poolManager.release(webView1);
    poolManager.release(webView2);
}
```

### 性能测试
```java
@Test
public void testWebViewPoolManager_performance_shouldBeEfficient() {
    // Given
    WebViewPoolManager poolManager = WebViewPoolManager.getInstance();

    // When - 模拟高频使用场景
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < 100; i++) {
        WebView webView = poolManager.acquire();
        // 模拟使用
        poolManager.release(webView);
    }

    long endTime = System.currentTimeMillis();

    // Then - 验证性能
    long totalTime = endTime - startTime;
    assertTrue("Pool operations should be fast", totalTime < 5000); // 5秒内完成
}
```

### 内存测试
```java
@Test
public void testWebViewPoolManager_memoryManagement_shouldPreventLeaks() {
    // Given
    WebViewPoolManager poolManager = WebViewPoolManager.getInstance();
    List<WebView> webViews = new ArrayList<>();

    // When - 创建多个WebView
    for (int i = 0; i < 10; i++) {
        WebView webView = poolManager.acquire();
        webViews.add(webView);
    }

    // Then - 验证池统计
    PoolStats stats = poolManager.getStats();
    assertEquals(10, stats.getActiveCount());

    // 释放所有WebView
    for (WebView webView : webViews) {
        poolManager.release(webView);
    }

    // 验证释放后状态
    stats = poolManager.getStats();
    assertEquals(0, stats.getActiveCount());
    assertTrue(stats.getIdleCount() > 0);
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingWebViewPool`)
3. 提交更改 (`git commit -m 'Add some AmazingWebViewPool'`)
4. 推送到分支 (`git push origin feature/AmazingWebViewPool`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/webview-pool-manager/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
