# EhViewer UI小部件模块 (Widget Module)

## 概述

UI小部件模块为EhViewer应用提供丰富的Android小部件，包括桌面小部件、信息卡片、快捷操作等。该模块支持多种小部件样式和交互方式。

## 主要功能

### 1. 桌面小部件
- 下载进度小部件
- 收藏夹快捷小部件
- 搜索快捷小部件

### 2. 通知小部件
- 下载完成通知
- 更新提醒通知
- 进度通知

### 3. 快捷操作
- 应用快捷方式
- 浮动操作按钮
- 快捷菜单

### 4. 信息卡片
- 画廊信息卡片
- 下载状态卡片
- 设置选项卡片

## 使用方法

```java
// 创建下载进度小部件
DownloadProgressWidget widget = new DownloadProgressWidget(context);
widget.setDownloadInfo(downloadInfo);
widget.updateProgress(progress);

// 创建收藏夹快捷小部件
FavoriteShortcutWidget shortcutWidget = new FavoriteShortcutWidget(context);
shortcutWidget.setOnFavoriteClickListener(gallery -> {
    // 处理收藏点击
});

// 显示通知
NotificationManager notificationManager = NotificationManager.getInstance(context);
notificationManager.showDownloadCompleteNotification(galleryTitle, filePath);

// 创建信息卡片
GalleryInfoCard card = new GalleryInfoCard(context);
card.setGalleryInfo(galleryInfo);
card.setOnCardClickListener(() -> {
    // 处理卡片点击
});
```

## 许可证

本模块遵循Apache License 2.0协议。
