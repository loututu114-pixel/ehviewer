# EhViewer UI组件模块 (UI Module)

## 概述

UI组件模块为EhViewer应用提供丰富的用户界面组件，包括自定义View、对话框、列表组件、导航组件等。该模块采用Material Design设计语言，提供一致的用户体验。

## 主要功能

### 1. 基础组件
- 自定义ImageView
- 进度条组件
- 按钮组件
- 输入框组件

### 2. 列表组件
- 画廊列表
- 下载列表
- 历史记录列表
- 收藏夹列表

### 3. 对话框组件
- 确认对话框
- 进度对话框
- 选择对话框
- 自定义对话框

### 4. 导航组件
- 底部导航栏
- 标签页导航
- 抽屉导航
- 面包屑导航

## 使用方法

```java
// 创建画廊列表
GalleryListView galleryList = new GalleryListView(context);
galleryList.setAdapter(new GalleryAdapter(galleries));
galleryList.setOnItemClickListener((gallery, position) -> {
    // 处理点击事件
});

// 显示进度对话框
ProgressDialog dialog = new ProgressDialog(context);
dialog.setMessage("Loading...");
dialog.show();

// 创建自定义按钮
EhButton button = new EhButton(context);
button.setText("Download");
button.setOnClickListener(v -> {
    // 处理点击事件
});
```

## 许可证

本模块遵循Apache License 2.0协议。
