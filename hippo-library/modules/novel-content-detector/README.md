# 🔍 小说内容检测器模块 (Novel Content Detector Module)

## 🎯 概述

Android Library小说内容检测器提供智能的小说内容识别功能，能够自动识别网页中的小说内容并提取相关信息。

## ✨ 主要特性

- ✅ **智能识别**: 基于AI的小说内容识别算法
- ✅ **多格式支持**: 支持各种小说网站的格式
- ✅ **内容提取**: 自动提取小说标题、作者、内容等信息
- ✅ **分类识别**: 区分普通小说和成人小说
- ✅ **质量评估**: 评估小说内容的完整性和质量

## 🚀 快速开始

```java
// 初始化内容检测器
NovelContentDetector.initialize(context);

// 检测网页内容
String url = "https://example.com/novel/123";
NovelContentDetector.getInstance().detectContent(url, new DetectionCallback() {
    @Override
    public void onDetected(NovelInfo novelInfo) {
        // 处理检测到的内容
        Log.d(TAG, "Detected novel: " + novelInfo.getTitle());
    }

    @Override
    public void onNotDetected() {
        // 内容不是小说
        Log.d(TAG, "Content is not a novel");
    }
});
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
