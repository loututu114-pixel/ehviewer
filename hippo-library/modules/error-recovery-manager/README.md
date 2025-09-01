# 🔄 错误恢复管理器模块 (Error Recovery Manager Module)

## 🎯 概述

Android Library错误恢复管理器提供智能的错误检测和自动恢复机制，提升应用稳定性和用户体验。

## ✨ 主要特性

- ✅ **错误检测**: 自动检测各种类型的错误
- ✅ **智能恢复**: 根据错误类型选择最优恢复策略
- ✅ **用户友好**: 无缝的错误恢复过程
- ✅ **统计分析**: 错误类型和恢复成功率的统计
- ✅ **自定义策略**: 支持自定义错误恢复策略

## 🚀 快速开始

```java
// 初始化错误恢复管理器
ErrorRecoveryManager.initialize(context);

// 注册错误处理器
ErrorRecoveryManager.getInstance().registerHandler(
    WebViewClient.ERROR_TIMEOUT,
    new TimeoutErrorHandler()
);

// 处理错误
WebView webView = new WebView(context);
ErrorRecoveryManager.getInstance().handleError(
    webView, WebViewClient.ERROR_TIMEOUT, "Request timeout", url
);
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
