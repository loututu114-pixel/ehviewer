# EhViewer 性能优化测试指南

## 优化内容概览

### 1. 画廊阅读方向
- ✅ **默认阅读方向**: 从"右到左"修改为"从上到下"
- **文件**: `Settings.java:520`
- **影响**: 更符合现代阅读习惯，提升用户体验

### 2. 动态内存缓存优化
- ✅ **图片内存缓存**: 20MB → 20-64MB (基于设备内存)
- ✅ **磁盘缓存**: 320MB → 320MB-1GB (基于设备内存)  
- ✅ **画廊详情缓存**: 25个 → 25-100个 (基于设备内存)
- **文件**: `EhApplication.java:773-836`

### 3. 动态线程池优化
- ✅ **IO线程池核心数**: 3个 → 3-8个 (基于CPU核心数和内存)
- ✅ **IO线程池最大数**: 32个 → 32-64个 (基于CPU核心数和内存)
- **文件**: `IoThreadPoolExecutor.java:32-62`

### 4. 智能预加载策略
- ✅ **预加载数量**: 15张 → 10-30张 (基于设备内存)
- **支持**: 12GB+设备30张，8GB+设备25张，6GB+设备20张
- **文件**: `Settings.java:1659-1678`

### 5. 图片处理优化
- ✅ **图片缓存尺寸**: 512×512 → 512×512-1024×1024 (基于设备内存)
- **文件**: `ImageBitmapHelper.java:30-45`

### 6. 构建时内存优化
- ✅ **DEX最大堆内存**: 默认 → 4GB
- ✅ **预编译**: 启用
- ✅ **最大进程数**: 8个
- **文件**: `build.gradle.kts:49-53`

## 设备内存分级策略

| 设备内存 | 图片内存缓存 | 磁盘缓存 | 画廊缓存 | 预加载数量 | IO核心线程 | IO最大线程 |
|---------|-------------|----------|----------|-----------|-----------|-----------|
| 12GB+   | 64MB        | 1GB      | 100个    | 30张      | 8个       | 64个      |
| 8GB+    | 48MB        | 800MB    | 75个     | 25张      | 6-8个     | 48-64个   |
| 6GB+    | 32MB        | 640MB    | 50个     | 20张      | 4-6个     | 36-48个   |
| 4GB+    | 32MB        | 640MB    | 50个     | 15张      | 3-4个     | 32-36个   |
| <4GB    | 20MB        | 320MB    | 25个     | 10张      | 3个       | 32个      |

## 测试方法

### 1. 基础功能测试
```bash
# 构建并安装APK
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.hippo.ehviewer/.ui.MainActivity
```

### 2. 内存使用监控
```bash
# 监控应用内存使用
adb shell dumpsys meminfo com.hippo.ehviewer

# 持续监控
watch -n 5 'adb shell dumpsys meminfo com.hippo.ehviewer | grep -E "TOTAL|Native Heap|Dalvik Heap"'
```

### 3. 性能分析
```bash
# CPU使用率监控
adb shell top -p $(adb shell pidof com.hippo.ehviewer)

# 网络连接数
adb shell netstat | grep ehviewer
```

### 4. 画廊浏览测试场景

#### 场景1: 大量图片浏览
1. 打开包含100+图片的画廊
2. 连续快速滑动浏览
3. 观察：
   - 图片加载速度
   - 内存使用变化
   - 应用响应性

#### 场景2: 多画廊切换
1. 连续打开10个不同画廊
2. 在画廊间快速切换
3. 观察：
   - 切换流畅度
   - 缓存命中率
   - 内存回收情况

#### 场景3: 长时间使用
1. 持续浏览2小时
2. 定期监控内存使用
3. 观察：
   - 内存泄漏
   - 性能降级
   - 崩溃概率

## 期望改善效果

### 高内存设备 (8GB+)
- 🎯 **图片加载速度**: 提升50%+
- 🎯 **缓存命中率**: 提升30%+  
- 🎯 **多任务并发**: 支持更多同时下载
- 🎯 **预加载覆盖**: 增加到25-30张图片

### 中等内存设备 (4-8GB)
- 🎯 **整体流畅度**: 提升30%+
- 🎯 **缓存有效性**: 提升20%+
- 🎯 **响应速度**: 明显改善

### 低内存设备 (<4GB)  
- 🎯 **稳定性**: 保持原有水平
- 🎯 **兼容性**: 确保向后兼容
- 🎯 **资源占用**: 适度增加但可控

## 监控指标

### 关键性能指标 (KPI)
1. **图片加载时间**: 平均加载时间 < 500ms
2. **内存使用峰值**: 不超过系统可用内存的60%
3. **缓存命中率**: 图片缓存命中率 > 80%
4. **应用启动时间**: 冷启动 < 3秒，热启动 < 1秒
5. **崩溃率**: OOM崩溃率 < 0.1%

### 监控命令
```bash
# 应用启动时间
adb shell am start -W com.hippo.ehviewer/.ui.MainActivity

# 内存详细信息  
adb shell dumpsys meminfo com.hippo.ehviewer --package

# 线程信息
adb shell ps -T -p $(adb shell pidof com.hippo.ehviewer)
```

## 回滚方案

如果优化导致问题，可以快速回滚关键配置：

### 1. 恢复保守内存设置
```java
// Settings.java - 恢复原始预加载数量
return getInt(KEY_ENHANCED_PRELOAD_IMAGE, 8); // 所有设备统一8张

// EhApplication.java - 恢复原始缓存大小
return Math.min(20 * 1024 * 1024, (int) OSUtils.getAppMaxMemory()); // 固定20MB
```

### 2. 恢复原始线程池配置
```java
// IoThreadPoolExecutor.java - 恢复原始配置
IoThreadPoolExecutor.newInstance(3, 32, 1L, TimeUnit.SECONDS, ...);
```

### 3. 恢复原始阅读方向
```java
// Settings.java - 恢复右到左阅读
private static final int DEFAULT_READING_DIRECTION = GalleryView.LAYOUT_RIGHT_TO_LEFT;
```

---

*性能优化测试指南 - 最后更新: 2025-09-10*