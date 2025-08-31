# EhViewer 下载管理模块 (Download Module)

## 概述

下载管理模块为EhViewer应用提供完整的文件下载功能，包括多线程下载、断点续传、下载队列管理、状态监听等。该模块采用高效的下载算法，支持大文件下载和并发控制。

## 主要功能

### 1. 多线程下载
- 支持多线程并发下载
- 智能分片下载
- 动态调整线程数

### 2. 断点续传
- 自动断点续传
- 下载进度保存
- 网络异常恢复

### 3. 下载队列管理
- 任务队列调度
- 优先级管理
- 并发控制

### 4. 下载状态监听
- 实时进度回调
- 下载状态变化通知
- 错误处理和重试

### 5. 存储管理
- 下载路径管理
- 存储空间检测
- 文件完整性校验

### 6. 下载速度控制
- 下载速度限制
- 流量控制
- 后台下载支持

## 核心类

### DownloadManager - 下载管理器
```java
public class DownloadManager {
    // 获取单例实例
    public static DownloadManager getInstance(Context context)

    // 添加下载任务
    public void addDownloadTask(DownloadTask task)

    // 任务控制
    public void pauseDownloadTask(String url)
    public void resumeDownloadTask(String url)
    public void cancelDownloadTask(String url)
    public void cancelAllDownloadTasks()

    // 任务查询
    public DownloadTask getDownloadTask(String url)
    public List<DownloadTask> getAllDownloadTasks()

    // 状态查询
    public int getActiveDownloadCount()
    public int getQueuedDownloadCount()

    // 监听器设置
    public void setGlobalListener(DownloadListener listener)
}
```

### DownloadTask - 下载任务
```java
public class DownloadTask implements Runnable {
    // 任务属性
    private String url;
    private String localPath;
    private int priority;
    private DownloadStatus status;

    // 进度信息
    private long totalSize;
    private long downloadedSize;
    private float progress;

    // 控制方法
    public void pause()
    public void resume()
    public void cancel()
}
```

### DownloadListener - 下载监听器
```java
public interface DownloadListener {
    void onDownloadStart(DownloadTask task);
    void onDownloadProgress(DownloadTask task);
    void onDownloadComplete(DownloadTask task);
    void onDownloadError(DownloadTask task, Exception e);
    void onDownloadPause(DownloadTask task);
    void onDownloadResume(DownloadTask task);
    void onDownloadCancel(DownloadTask task);
}
```

## 使用方法

### 基本下载

```java
// 获取下载管理器
DownloadManager downloadManager = DownloadManager.getInstance(context);

// 创建下载任务
DownloadTask task = new DownloadTask();
task.setUrl("https://example.com/file.zip");
task.setLocalPath("/sdcard/downloads/file.zip");
task.setPriority(DownloadTask.PRIORITY_NORMAL);

// 设置监听器
downloadManager.setGlobalListener(new DownloadManager.DownloadListener() {
    @Override
    public void onDownloadStart(DownloadTask task) {
        Log.d(TAG, "下载开始: " + task.getUrl());
    }

    @Override
    public void onDownloadProgress(DownloadTask task) {
        Log.d(TAG, "下载进度: " + task.getProgress() + "%");
    }

    @Override
    public void onDownloadComplete(DownloadTask task) {
        Log.d(TAG, "下载完成: " + task.getLocalPath());
    }

    @Override
    public void onDownloadError(DownloadTask task, Exception e) {
        Log.e(TAG, "下载失败: " + task.getUrl(), e);
    }
});

// 添加下载任务
downloadManager.addDownloadTask(task);
```

### 高级功能

```java
// 批量下载
List<DownloadTask> tasks = new ArrayList<>();
for (String url : downloadUrls) {
    DownloadTask task = new DownloadTask();
    task.setUrl(url);
    task.setLocalPath(getLocalPath(url));
    tasks.add(task);
}

// 设置批量监听器
DownloadManager.BatchDownloadListener batchListener = new DownloadManager.BatchDownloadListener() {
    @Override
    public void onBatchProgress(int completed, int total) {
        Log.d(TAG, "批量下载进度: " + completed + "/" + total);
    }

    @Override
    public void onBatchComplete() {
        Log.d(TAG, "批量下载完成");
    }
};

// 执行批量下载
downloadManager.addDownloadTasks(tasks, batchListener);
```

### 任务控制

```java
DownloadManager downloadManager = DownloadManager.getInstance(context);

// 暂停任务
downloadManager.pauseDownloadTask("https://example.com/file.zip");

// 恢复任务
downloadManager.resumeDownloadTask("https://example.com/file.zip");

// 取消任务
downloadManager.cancelDownloadTask("https://example.com/file.zip");

// 取消所有任务
downloadManager.cancelAllDownloadTasks();
```

### 任务查询

```java
// 获取特定任务
DownloadTask task = downloadManager.getDownloadTask("https://example.com/file.zip");

// 获取所有任务
List<DownloadTask> allTasks = downloadManager.getAllDownloadTasks();

// 获取活跃任务数量
int activeCount = downloadManager.getActiveDownloadCount();

// 获取队列任务数量
int queuedCount = downloadManager.getQueuedDownloadCount();
```

### 配置管理

```java
// 设置最大并发下载数
downloadManager.setMaxConcurrentDownloads(5);

// 设置下载速度限制
downloadManager.setDownloadSpeedLimit(1024 * 1024); // 1MB/s

// 设置下载路径
downloadManager.setDefaultDownloadPath("/sdcard/downloads");

// 启用断点续传
downloadManager.setResumeSupported(true);
```

## 下载状态

```java
public enum DownloadStatus {
    NONE,       // 未开始
    WAITING,    // 等待中
    DOWNLOADING, // 下载中
    PAUSED,     // 已暂停
    COMPLETED,  // 已完成
    FAILED,     // 失败
    CANCELLED   // 已取消
}
```

## 优先级设置

```java
public class DownloadTask {
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HIGH = 2;
    public static final int PRIORITY_URGENT = 3;
}
```

## 依赖项

在你的`build.gradle`文件中添加：

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    // 如果需要OkHttp支持
    // implementation 'com.squareup.okhttp3:okhttp:4.10.0'
}
```

## 权限配置

在`AndroidManifest.xml`中添加必要的权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 性能优化建议

1. **并发控制**: 根据设备性能和网络状况调整并发下载数
2. **内存管理**: 大文件下载时注意内存使用，避免内存溢出
3. **存储管理**: 定期清理已完成的下载任务和临时文件
4. **网络优化**: 支持断点续传，减少网络资源浪费
5. **用户体验**: 提供清晰的进度反馈和错误处理

## 错误处理

模块内置了完善的错误处理机制：

```java
// 网络错误
NETWORK_ERROR,
// 文件系统错误
FILESYSTEM_ERROR,
// 存储空间不足
INSUFFICIENT_STORAGE,
// 权限不足
PERMISSION_DENIED,
// 服务器错误
SERVER_ERROR,
// 未知错误
UNKNOWN_ERROR
```

## 示例项目

查看完整的示例代码：
- `BasicDownloadActivity.java` - 基础下载功能示例
- `AdvancedDownloadActivity.java` - 高级下载功能示例
- `BatchDownloadActivity.java` - 批量下载示例

## 注意事项

1. **网络权限**: 确保应用具有网络访问权限
2. **存储权限**: 对于Android 6.0+需要动态申请存储权限
3. **后台下载**: 长时间下载建议使用前台服务
4. **电量优化**: 下载过程中注意设备电量管理
5. **流量控制**: 移动网络下注意流量使用情况

## 许可证

本模块遵循Apache License 2.0协议。
