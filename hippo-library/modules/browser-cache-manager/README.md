# 💾 浏览器缓存管理器模块 (Browser Cache Manager Module)

## 🎯 概述

Android Library浏览器缓存管理器提供多级缓存系统，支持内存缓存、磁盘缓存和WebView缓存的统一管理。

## ✨ 主要特性

- ✅ **多级缓存**: 内存+磁盘+WebView三级缓存
- ✅ **智能清理**: 根据使用频率和存储压力自动清理
- ✅ **缓存预加载**: 智能预加载常用资源
- ✅ **压缩存储**: 支持缓存数据的压缩存储
- ✅ **缓存统计**: 详细的缓存使用统计

## 🚀 快速开始

```java
// 初始化缓存管理器
BrowserCacheManager.initialize(context);

// 配置缓存策略
CacheConfig config = new CacheConfig.Builder()
    .setMemoryCacheSize(50 * 1024 * 1024)  // 50MB内存缓存
    .setDiskCacheSize(100 * 1024 * 1024)   // 100MB磁盘缓存
    .enableCompression(true)               // 启用压缩
    .build();

BrowserCacheManager.getInstance().setConfig(config);
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
