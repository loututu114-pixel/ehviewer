# EhViewer 缓存管理模块 (Cache Module)

## 概述

缓存管理模块为EhViewer应用提供统一的缓存管理系统，包括内存缓存、磁盘缓存、网络缓存等。该模块采用LRU算法，支持多种缓存策略和自动清理。

## 主要功能

### 1. 内存缓存
- LruCache实现
- 缓存大小限制
- 自动清理机制

### 2. 磁盘缓存
- 文件系统缓存
- 缓存过期时间
- 存储空间管理

### 3. 网络缓存
- HTTP缓存
- 缓存策略配置
- 离线缓存支持

### 4. 缓存监控
- 缓存命中率统计
- 缓存大小监控
- 缓存清理报告

## 使用方法

```java
// 获取缓存管理器
CacheManager cacheManager = CacheManager.getInstance(context);

// 内存缓存
cacheManager.put("user_data", userData);
Object cachedData = cacheManager.get("user_data");

// 磁盘缓存
cacheManager.putToDisk("image_data", imageBytes, 24 * 60 * 60 * 1000); // 24小时过期
byte[] cachedImageData = cacheManager.getFromDisk("image_data");

// 清空缓存
cacheManager.clearMemoryCache();
cacheManager.clearDiskCache();

// 获取缓存统计
CacheStats stats = cacheManager.getCacheStats();
Log.d(TAG, "Cache hit rate: " + stats.getHitRate());
```

## 许可证

本模块遵循Apache License 2.0协议。
