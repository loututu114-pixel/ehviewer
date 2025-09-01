# 🔍 URL类型检测器模块 (URL Type Detector Module)

## 🎯 概述

Android Library URL类型检测器提供精确的URL类型识别功能，支持有效URL、搜索查询、特殊协议等多种类型的检测。

## ✨ 主要特性

- ✅ **精确识别**: 多层级URL类型检测算法
- ✅ **正则优化**: 预编译正则表达式保证性能
- ✅ **类型丰富**: 支持URL、搜索、协议、文件等多种类型
- ✅ **边界处理**: 完善的边界情况处理
- ✅ **性能监控**: 实时监控检测性能

## 🚀 快速开始

```java
// 初始化URL类型检测器
UrlTypeDetector.initialize(context);

// 检测URL类型
String input = "github.com";
UrlTypeDetector.getInstance().detectType(input, new TypeDetectionCallback() {
    @Override
    public void onDetected(UrlType type, String processedUrl) {
        switch (type) {
            case VALID_URL:
                // 处理有效URL
                webView.loadUrl(processedUrl);
                break;
            case SEARCH_QUERY:
                // 处理搜索查询
                String searchUrl = buildSearchUrl(processedUrl);
                webView.loadUrl(searchUrl);
                break;
            case SPECIAL_PROTOCOL:
                // 处理特殊协议
                handleSpecialProtocol(processedUrl);
                break;
        }
    }
});
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
