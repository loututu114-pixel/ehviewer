# 🎬 YouTube兼容性管理器模块 (YouTube Compatibility Manager Module)

## 🎯 概述

Android Library YouTube兼容性管理器专门解决YouTube访问的各种兼容性问题，提供无缝的YouTube浏览体验。

## ✨ 主要特性

- ✅ **重定向处理**: 智能处理YouTube的重定向逻辑
- ✅ **UA优化**: 为YouTube量身定制的UA策略
- ✅ **循环检测**: 防止无限重定向循环
- ✅ **兼容性检测**: 检测不同设备的兼容性
- ✅ **自动修复**: 自动修复已知的兼容性问题

## 🚀 快速开始

```java
// 初始化YouTube兼容性管理器
YouTubeCompatibilityManager.initialize(context);

// 检查并应用YouTube兼容性
WebView webView = new WebView(context);
String url = "https://youtube.com/watch?v=videoId";

if (YouTubeCompatibilityManager.getInstance().isYouTubeUrl(url)) {
    YouTubeCompatibilityManager.getInstance().applyCompatibility(webView, url);
}

webView.loadUrl(url);
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
