# 🕵️ User-Agent管理器模块 (User-Agent Manager Module)

## 🎯 概述

Android Library User-Agent管理器提供智能的User-Agent选择和管理功能，解决网站兼容性问题和访问限制。

## ✨ 主要特性

- ✅ **智能选择**: 根据网站类型智能选择最优UA
- ✅ **多UA策略**: 支持桌面版、移动版等多种UA
- ✅ **别名解析**: 支持域名别名到完整域名的转换
- ✅ **性能监控**: 监控UA策略的成功率
- ✅ **动态调整**: 根据访问结果动态调整策略

## 🚀 快速开始

```java
// 初始化User-Agent管理器
UserAgentManager.initialize(context);

// 为网站选择最优UA
String domain = "youtube.com";
String optimalUA = UserAgentManager.getInstance().getOptimalUserAgent(domain);

// 应用到WebView
WebView webView = new WebView(context);
webView.getSettings().setUserAgentString(optimalUA);

// 添加自定义UA策略
UserAgentManager.getInstance().addCustomStrategy(
    "example.com", UserAgentType.DESKTOP_CHROME
);
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
