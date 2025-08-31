# 🌐 现代浏览器模块 (Modern Browser Module)

## 🎯 概述

EhViewer现代浏览器模块提供完整的浏览器功能体验，参考主流浏览器的设计理念，包含标签页管理、智能地址栏、书签系统、隐私模式等高级功能。通过优化的WebView配置和智能缓存策略，确保快速流畅的浏览体验。

## ✨ 主要特性

### 🔍 智能地址栏
- ✅ **智能识别**：自动识别URL、搜索查询、文件路径、快捷命令
- ✅ **实时建议**：智能补全和搜索建议
- ✅ **多搜索引擎**：支持百度、谷歌、必应、搜狗、DuckDuckGo
- ✅ **快捷命令**：支持 `:history`、`:bookmarks`、`:downloads` 等命令

### 📑 标签页管理
- ✅ **多标签页支持**：同时管理多个网页
- ✅ **隐私模式**：独立的隐私浏览标签页
- ✅ **标签页切换**：平滑的标签页切换体验
- ✅ **状态保持**：标签页状态自动保存和恢复

### 🔒 隐私保护
- ✅ **隐私浏览模式**：不记录历史和Cookie
- ✅ **Cookie管理**：精细的Cookie控制
- ✅ **痕迹清理**：一键清理浏览痕迹
- ✅ **无痕搜索**：隐私保护的搜索体验

### 🌙 界面特性
- ✅ **夜间模式**：护眼的深色主题
- ✅ **桌面模式**：切换桌面版网页显示
- ✅ **自适应缩放**：智能调整页面缩放
- ✅ **手势支持**：丰富的触摸手势操作

### 📥 下载管理
- ✅ **智能下载**：自动识别下载链接
- ✅ **断点续传**：支持大文件断点续传
- ✅ **下载历史**：完整的下载记录管理
- ✅ **文件管理**：智能的文件类型识别和处理

### ⭐ 书签系统
- ✅ **书签管理**：添加、编辑、删除书签
- ✅ **分类组织**：书签文件夹和分类
- ✅ **快速搜索**：书签快速搜索功能
- ✅ **同步支持**：书签数据同步

### 📚 历史记录
- ✅ **访问历史**：完整的网页访问记录
- ✅ **搜索历史**：搜索关键词历史记录
- ✅ **智能分组**：按时间和类型分组显示
- ✅ **隐私清理**：选择性清理历史记录

## 🚀 快速开始

### 基本使用

```java
// 获取浏览器管理器实例
ModernBrowserManager browserManager = ModernBrowserManager.getInstance(context);

// 设置回调监听器
browserManager.setCallback(new ModernBrowserManager.BrowserCallback() {
    @Override
    public void onPageStarted(String url, String title) {
        Log.d(TAG, "开始加载: " + url);
        showProgressBar();
    }

    @Override
    public void onPageFinished(String url, String title) {
        Log.d(TAG, "加载完成: " + title);
        hideProgressBar();
        updateTitle(title);
        updateUrl(url);
    }

    @Override
    public void onProgressChanged(int progress) {
        updateProgressBar(progress);
    }

    @Override
    public void onReceivedError(int errorCode, String description, String failingUrl) {
        Log.e(TAG, "加载错误: " + description);
        showErrorPage(errorCode, description);
    }

    @Override
    public void onTabCreated(ModernBrowserManager.BrowserTab tab) {
        Log.d(TAG, "标签页创建: " + tab.title);
        updateTabList();
    }

    @Override
    public void onTabClosed(ModernBrowserManager.BrowserTab tab) {
        Log.d(TAG, "标签页关闭: " + tab.title);
        updateTabList();
    }

    @Override
    public void onTabSwitched(ModernBrowserManager.BrowserTab tab) {
        Log.d(TAG, "切换标签页: " + tab.title);
        updateCurrentTab(tab);
    }

    @Override
    public void onUrlChanged(String url) {
        updateAddressBar(url);
    }

    @Override
    public void onTitleChanged(String title) {
        updateWindowTitle(title);
    }
});

// 创建第一个标签页
ModernBrowserManager.BrowserTab firstTab = browserManager.createNewTab();

// 加载网页
browserManager.loadUrl("https://www.google.com");

// 导航操作
browserManager.goBack();     // 后退
browserManager.goForward();  // 前进
browserManager.refresh();    // 刷新
browserManager.stopLoading(); // 停止加载
```

### 高级配置

```java
// 创建自定义配置
ModernBrowserManager.ModernBrowserConfig config =
    new ModernBrowserManager.ModernBrowserConfig.Builder()
        .enableJavaScript(true)           // 启用JavaScript
        .enableCookies(true)              // 启用Cookie
        .enableCache(true)                // 启用缓存
        .enableImages(true)               // 启用图片
        .enableAdsBlock(false)            // 禁用广告拦截
        .enableNightMode(false)           // 禁用夜间模式
        .enableDesktopMode(false)         // 禁用桌面模式
        .setTextZoom(100)                 // 文字缩放100%
        .setUserAgent("Custom/1.0")       // 自定义User-Agent
        .build();

// 应用配置
browserManager.setConfig(config);

// 模式切换
browserManager.setIncognitoMode(true);    // 隐私模式
browserManager.setDesktopMode(true);      // 桌面模式
browserManager.setNightMode(true);        // 夜间模式
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `ModernBrowserManager` | 浏览器管理器核心类 |
| `ModernBrowserConfig` | 浏览器配置类 |
| `BrowserCallback` | 浏览器回调接口 |
| `BrowserTab` | 浏览器标签页类 |

### 主要方法

#### ModernBrowserManager

```java
// 获取实例
ModernBrowserManager getInstance(Context context)

// 设置配置和回调
void setConfig(ModernBrowserConfig config)
void setCallback(BrowserCallback callback)

// 标签页管理
BrowserTab createNewTab()
boolean closeTab(BrowserTab tab)
boolean switchToTab(int index)
BrowserTab getCurrentTab()
List<BrowserTab> getAllTabs()
int getTabCount()

// 页面导航
void loadUrl(String url)
void goBack()
void goForward()
void refresh()
void stopLoading()

// 状态查询
boolean canGoBack()
boolean canGoForward()
boolean isLoading()

// 模式设置
void setIncognitoMode(boolean incognito)
void setDesktopMode(boolean desktop)
void setNightMode(boolean night)

// 获取状态
ModernBrowserConfig getConfig()
boolean isIncognitoMode()
boolean isDesktopMode()
boolean isNightMode()

// 清理资源
void cleanup()
```

#### BrowserTab

```java
// 标签页属性
WebView webView        // WebView实例
String url            // 当前URL
String title          // 页面标题
Bitmap favicon        // 网站图标
boolean isIncognito   // 是否为隐私标签页
long createTime       // 创建时间
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableJavaScript` | `boolean` | `true` | 是否启用JavaScript |
| `enableCookies` | `boolean` | `true` | 是否启用Cookie |
| `enableCache` | `boolean` | `true` | 是否启用缓存 |
| `enableImages` | `boolean` | `true` | 是否自动加载图片 |
| `enableAdsBlock` | `boolean` | `false` | 是否启用广告拦截 |
| `enableNightMode` | `boolean` | `false` | 是否启用夜间模式 |
| `enableDesktopMode` | `boolean` | `false` | 是否启用桌面模式 |
| `textZoom` | `int` | `100` | 文字缩放百分比 |
| `userAgent` | `String` | `""` | 自定义User-Agent字符串 |

## 📦 依赖项

```gradle
dependencies {
    // 核心依赖
    implementation 'com.example:modern-browser-module:1.0.0'

    // 可选依赖（根据需要添加）
    implementation 'androidx.core:core-ktx:1.10.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 可选权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### WebView配置
```xml
<!-- 在AndroidManifest.xml中添加 -->
<application>
    <meta-data
        android:name="android.webkit.WebView.EnableSafeBrowsing"
        android:value="true" />
</application>
```

### 兼容性
- **最低版本**: Android API 21 (Android 5.0)
- **目标版本**: Android API 34 (Android 14)
- **WebView版本**: 依赖系统WebView版本

### 性能优化
```java
// WebView性能优化配置
public class WebViewOptimizer {
    public static void optimize(WebView webView) {
        WebSettings settings = webView.getSettings();

        // 硬件加速
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // 缓存策略
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAppCacheEnabled(true);

        // 渲染优化
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
    }
}
```

### 内存管理
```java
// WebView内存管理
public class WebViewMemoryManager {
    public static void cleanup(WebView webView) {
        if (webView != null) {
            // 清除缓存
            webView.clearCache(true);
            webView.clearHistory();

            // 移除所有视图
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }

            // 销毁WebView
            webView.destroy();
        }
    }
}
```

## 🔄 工作原理

### 标签页管理系统

```java
// 标签页管理架构
public class TabManager {
    private List<BrowserTab> tabs = new ArrayList<>();
    private int currentTabIndex = 0;
    private static final int MAX_TABS = 99;

    public BrowserTab createTab() {
        if (tabs.size() >= MAX_TABS) {
            return null; // 达到上限
        }

        BrowserTab tab = new BrowserTab();
        tab.webView = createWebView();
        tabs.add(tab);

        return tab;
    }

    public boolean switchTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            currentTabIndex = index;
            BrowserTab tab = tabs.get(index);

            // 更新UI状态
            updateTabUI(tab);
            return true;
        }
        return false;
    }
}
```

### 智能地址栏系统

```java
// 地址栏智能处理
public class SmartAddressBar {
    public enum InputType {
        URL,           // 完整URL
        DOMAIN,        // 域名
        SEARCH_QUERY,  // 搜索关键词
        UNKNOWN       // 未知类型
    }

    public static class InputAnalysis {
        public InputType type;
        public String processedUrl;
        public float confidence;
        public List<String> suggestions;
    }

    public InputAnalysis analyzeInput(String input) {
        InputAnalysis analysis = new InputAnalysis();

        // URL检测
        if (input.contains("://")) {
            analysis.type = InputType.URL;
            analysis.processedUrl = input;
            analysis.confidence = 1.0f;
        }
        // 域名检测
        else if (input.contains(".") && !input.contains(" ")) {
            analysis.type = InputType.DOMAIN;
            analysis.processedUrl = "https://" + input;
            analysis.confidence = 0.9f;
        }
        // 搜索关键词
        else {
            analysis.type = InputType.SEARCH_QUERY;
            analysis.processedUrl = getSearchEngine().getSearchUrl(input);
            analysis.confidence = 0.7f;
        }

        // 生成建议
        analysis.suggestions = generateSuggestions(input, analysis.type);

        return analysis;
    }
}
```

### 隐私保护机制

```java
// 隐私模式实现
public class PrivacyManager {
    public static void enablePrivacyMode(WebView webView) {
        WebSettings settings = webView.getSettings();

        // 禁用缓存
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setAppCacheEnabled(false);

        // 禁用存储
        settings.setDatabaseEnabled(false);
        settings.setDomStorageEnabled(false);

        // 禁用定位
        settings.setGeolocationEnabled(false);

        // 禁用表单保存
        settings.setSaveFormData(false);
        settings.setSavePassword(false);

        // 清除数据
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
    }
}
```

## 🧪 测试

### 单元测试
```java
@Test
public void testCreateNewTab() {
    // Given
    ModernBrowserManager manager = ModernBrowserManager.getInstance(context);

    // When
    BrowserTab tab = manager.createNewTab();

    // Then
    assertNotNull(tab);
    assertNotNull(tab.webView);
    assertEquals("新标签页", tab.title);
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4.class)
public class BrowserIntegrationTest {

    @Test
    public void testFullBrowsingFlow() {
        // 测试完整的浏览流程
        // 1. 创建标签页
        // 2. 加载网页
        // 3. 等待加载完成
        // 4. 验证页面内容
        // 5. 关闭标签页
    }
}
```

### UI测试
```java
@RunWith(AndroidJUnit4.class)
public class BrowserUITest {

    @Test
    public void testAddressBarInput() {
        // 测试地址栏输入和加载
        onView(withId(R.id.address_bar))
            .perform(typeText("https://www.google.com"))
            .perform(pressKey(KeyEvent.KEYCODE_ENTER));

        // 验证页面加载
        onView(withId(R.id.webview_container))
            .check(matches(isDisplayed()));
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingBrowser`)
3. 提交更改 (`git commit -m 'Add some AmazingBrowser'`)
4. 推送到分支 (`git push origin feature/AmazingBrowser`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@example.com
- 📖 文档: [完整文档](https://docs.example.com)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/example/repo/issues)

---

**💡 提示**: 该模块适用于需要完整浏览器功能的应用程序，如新闻阅读器、内容聚合器、网页浏览器等。
