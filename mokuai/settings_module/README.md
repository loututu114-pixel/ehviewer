# EhViewer 设置管理模块 (Settings Module)

## 概述

设置管理模块为EhViewer应用提供完整的设置管理系统，包括用户偏好设置、应用配置、数据持久化等。该模块支持多种设置类型和实时设置同步。

## 主要功能

### 1. 设置存储
- SharedPreferences存储
- 数据库存储
- 文件存储
- 云端同步

### 2. 设置类型
- 布尔值设置
- 整数设置
- 字符串设置
- 列表选择设置

### 3. 设置界面
- 设置页面生成
- 设置项分组
- 设置搜索
- 设置导入导出

### 4. 设置监听
- 设置变化监听
- 实时设置同步
- 设置验证

## 使用方法

```java
// 获取设置管理器
SettingsManager settings = SettingsManager.getInstance(context);

// 设置值
settings.putBoolean("enable_cache", true);
settings.putString("download_path", "/sdcard/downloads");
settings.putInt("max_downloads", 3);

// 获取值
boolean enableCache = settings.getBoolean("enable_cache", false);
String downloadPath = settings.getString("download_path", "/sdcard/downloads");
int maxDownloads = settings.getInt("max_downloads", 1);

// 注册设置变化监听器
settings.registerListener("enable_cache", new SettingChangeListener() {
    @Override
    public void onSettingChanged(String key, Object newValue) {
        // 处理设置变化
    }
});
```

## 许可证

本模块遵循Apache License 2.0协议。
