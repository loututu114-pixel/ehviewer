# 📖 小说阅读器模块 (Novel Reader Module)

## 🎯 概述

Android Library小说阅读器提供专业的本地小说阅读体验，支持夜间模式、字体调节、进度保存等高级功能。

## ✨ 主要特性

- ✅ **沉浸阅读**: 全屏沉浸式阅读体验
- ✅ **夜间模式**: 专为成人内容优化的夜间阅读模式
- ✅ **字体调节**: 支持字体大小、颜色、间距调节
- ✅ **进度保存**: 自动保存和恢复阅读进度
- ✅ **书签功能**: 支持添加书签和快速跳转
- ✅ **手势操作**: 丰富的阅读手势支持

## 🚀 快速开始

```java
// 初始化小说阅读器
NovelReader.initialize(context);

// 创建阅读会话
NovelInfo novel = getNovelInfo();
NovelReader reader = NovelReader.create(novel);

// 设置阅读监听器
reader.setReaderListener(new NovelReaderListener() {
    @Override
    public void onProgressChanged(int progress) {
        updateProgressBar(progress);
    }

    @Override
    public void onPageChanged(int currentPage, int totalPages) {
        updatePageIndicator(currentPage, totalPages);
    }
});

// 开始阅读
reader.startReading();
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
