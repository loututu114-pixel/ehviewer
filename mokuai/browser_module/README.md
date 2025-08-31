# EhViewer 浏览器模块 (Browser Module)

## 概述

浏览器模块为EhViewer应用提供内置的Web浏览器功能，支持网页浏览、JavaScript执行、Cookie管理等。该模块基于WebView实现，提供丰富的浏览器特性。

## 主要功能

### 1. 网页浏览
- WebView集成
- 网页加载和渲染
- 前进后退导航
- 页面刷新

### 2. JavaScript支持
- JavaScript执行
- JS桥接
- 脚本注入
- 控制台日志

### 3. Cookie管理
- Cookie存储
- Cookie同步
- Cookie清理

### 4. 下载管理
- 文件下载
- 下载进度
- 下载历史

## 使用方法

```java
// 创建浏览器实例
EhBrowser browser = new EhBrowser(context);

// 加载网页
browser.loadUrl("https://example.com");

// 设置网页加载监听器
browser.setWebViewClient(new WebViewClient() {
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // 页面开始加载
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // 页面加载完成
    }
});

// 执行JavaScript
browser.evaluateJavascript("console.log('Hello World!')", null);

// 管理Cookie
CookieManager cookieManager = browser.getCookieManager();
cookieManager.setCookie(url, "session=abc123");
```

## 许可证

本模块遵循Apache License 2.0协议。
