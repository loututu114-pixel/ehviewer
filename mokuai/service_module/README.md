# 📱 服务管理模块 (Service Module)

## 🎯 概述

EhViewer服务管理模块提供完整的后台服务管理功能，包括应用保活、定时任务、资源监控等。通过多种技术手段确保应用的稳定运行和后台任务的正常执行。

## ✨ 主要特性

- ✅ **应用保活**：多种策略保证应用持续运行
- ✅ **定时任务**：JobScheduler实现精确定时执行
- ✅ **资源管理**：智能的内存和CPU资源管理
- ✅ **状态监控**：实时监控服务运行状态
- ✅ **错误处理**：完善的异常处理和恢复机制

## 🚀 快速开始

### 基本使用

```java
// 获取服务管理器实例
ServiceManager serviceManager = ServiceManager.getInstance(context);

// 设置状态监听器
serviceManager.setStatusListener(new ServiceManager.ServiceStatusListener() {
    @Override
    public void onServiceStarted() {
        Log.d(TAG, "服务已启动");
    }

    @Override
    public void onServiceStopped() {
        Log.d(TAG, "服务已停止");
    }

    @Override
    public void onServiceError(String error) {
        Log.e(TAG, "服务错误: " + error);
    }
});

// 启动应用保活服务
serviceManager.startKeepAliveService(true);

// 检查服务状态
boolean isRunning = serviceManager.isKeepAliveServiceRunning();

// 启动定时任务（15分钟间隔）
serviceManager.startScheduledTask(15, 1001);

// 停止定时任务
serviceManager.stopScheduledTask(1001);

// 停止保活服务
serviceManager.stopKeepAliveService();
```

### 高级配置

```java
// 自定义服务配置
ServiceConfig config = new ServiceConfig.Builder()
    .enableWakeLock(true)          // 启用WakeLock
    .enableSilentAudio(true)       // 启用无声音频
    .enableScreenMonitoring(true)  // 启用屏幕状态监听
    .setJobIntervalMinutes(15)     // 设置任务间隔
    .build();

// 应用配置
serviceManager.applyConfig(config);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `ServiceManager` | 服务管理器核心类 |
| `KeepAliveService` | 应用保活服务 |
| `ScheduledTaskService` | 定时任务服务 |

### 主要方法

#### ServiceManager

```java
// 获取实例
ServiceManager getInstance(Context context)

// 设置状态监听器
void setStatusListener(ServiceStatusListener listener)

// 启动/停止保活服务
void startKeepAliveService(boolean enable)
void stopKeepAliveService()

// 检查服务状态
boolean isKeepAliveServiceRunning()

// 定时任务管理
void startScheduledTask(int intervalMinutes, int jobId)
void stopScheduledTask(int jobId)

// 资源清理
void cleanup()
```

#### ServiceStatusListener

```java
// 状态回调接口
void onServiceStarted()           // 服务启动
void onServiceStopped()          // 服务停止
void onServiceError(String error) // 服务错误
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `wakeLockEnabled` | `boolean` | `true` | 是否启用WakeLock保持CPU运行 |
| `silentAudioEnabled` | `boolean` | `true` | 是否播放无声音频 |
| `screenMonitoringEnabled` | `boolean` | `true` | 是否监听屏幕状态 |
| `jobIntervalMinutes` | `int` | `15` | 定时任务执行间隔(分钟) |
| `autoRestartEnabled` | `boolean` | `true` | 服务异常退出时是否自动重启 |

## 📦 依赖项

```gradle
dependencies {
    // 核心依赖
    implementation 'com.example:service-module:1.0.0'

    // 可选依赖（根据需要添加）
    implementation 'androidx.core:core:1.10.0'  // 基础Android支持
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- Android 13+ 需要额外权限 -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 兼容性
- **最低版本**: Android API 21 (Android 5.0)
- **目标版本**: Android API 34 (Android 14)
- **编译版本**: Android API 34

### 电池优化
```xml
<!-- 电池优化白名单（可选）-->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

### 已知限制
- 某些设备厂商的深度省电模式可能影响保活效果
- Android 12+ 对后台服务有更严格的限制
- 需要用户手动授权忽略电池优化

## 🔄 工作原理

### 保活策略组合

1. **前台服务 (Foreground Service)**
   - 显示持久化通知
   - 提高进程优先级
   - 减少被系统杀死的概率

2. **WakeLock机制**
   - 保持CPU运行
   - 防止系统休眠
   - 确保后台任务执行

3. **无声音频播放**
   - 提高进程优先级
   - 模拟媒体播放
   - 减少被清理的概率

4. **JobScheduler定时任务**
   - 定期唤醒应用
   - 执行后台任务
   - 智能调度执行

5. **屏幕状态监听**
   - 监听屏幕开关事件
   - 根据状态调整策略
   - 优化资源使用

### 资源管理

```java
// 智能资源管理
public class ResourceManager {
    private WakeLock wakeLock;
    private AudioTrack audioTrack;
    private BroadcastReceiver receiver;

    public void acquireResources() {
        // 按需获取资源
        if (wakeLock == null) {
            acquireWakeLock();
        }
        if (audioTrack == null) {
            playSilentAudio();
        }
        if (receiver == null) {
            registerReceiver();
        }
    }

    public void releaseResources() {
        // 安全释放资源
        releaseWakeLock();
        stopSilentAudio();
        unregisterReceiver();
    }
}
```

## 🧪 测试

### 单元测试
```java
@Test
public void testStartKeepAliveService() {
    // Given
    ServiceManager serviceManager = ServiceManager.getInstance(context);

    // When
    serviceManager.startKeepAliveService(true);

    // Then
    assertTrue(serviceManager.isKeepAliveServiceRunning());
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4.class)
public class ServiceIntegrationTest {

    @Test
    public void testServiceLifecycle() {
        // 测试完整的服务生命周期
        // 1. 启动服务
        // 2. 验证服务状态
        // 3. 停止服务
        // 4. 验证清理
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingService`)
3. 提交更改 (`git commit -m 'Add some AmazingService'`)
4. 推送到分支 (`git push origin feature/AmazingService`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@example.com
- 📖 文档: [完整文档](https://docs.example.com)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/example/repo/issues)

---

**💡 提示**: 该模块适用于需要后台运行和保活功能的应用场景，如下载器、监控工具、即时通讯等。
