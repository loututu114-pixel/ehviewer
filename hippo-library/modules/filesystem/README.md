# 📁 文件系统模块 (Filesystem Module)

## 🎯 概述

Android Library文件系统模块提供完整的文件和存储管理功能，包括文件操作、权限管理、存储空间监控、安全文件访问等特性，帮助开发者安全高效地管理应用数据。

## ✨ 主要特性

- ✅ **文件操作**: 文件的创建、读取、写入、删除、重命名
- ✅ **目录管理**: 目录的创建、遍历、清理、权限设置
- ✅ **存储监控**: 存储空间使用情况监控和报告
- ✅ **权限管理**: 文件访问权限检查和申请
- ✅ **安全访问**: 安全的文件访问和数据保护
- ✅ **缓存管理**: 临时文件和缓存的自动清理
- ✅ **路径处理**: 文件路径的标准化和验证
- ✅ **MIME类型**: 文件类型的识别和处理

## 🚀 快速开始

### 初始化文件系统管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化文件系统管理器
        FilesystemManager.initialize(this);

        // 配置存储路径
        FilesystemConfig config = new FilesystemConfig.Builder()
            .setCacheDir(getCacheDir())
            .setFilesDir(getFilesDir())
            .setExternalFilesDir(getExternalFilesDir(null))
            .enableAutoCleanup(true)
            .setCleanupInterval(24 * 60 * 60 * 1000L) // 24小时
            .build();

        FilesystemManager.getInstance().setConfig(config);
    }
}
```

### 基本文件操作

```java
// 获取文件系统管理器
FilesystemManager fs = FilesystemManager.getInstance();

// 创建文件
File textFile = new File(getFilesDir(), "example.txt");
boolean success = fs.createFile(textFile);
if (success) {
    // 写入文本内容
    fs.writeTextFile(textFile, "Hello, World!");

    // 读取文本内容
    String content = fs.readTextFile(textFile);
    Log.d(TAG, "File content: " + content);
}

// 创建目录
File imageDir = new File(getFilesDir(), "images");
fs.createDirectory(imageDir);

// 复制文件
File sourceFile = new File(getFilesDir(), "source.txt");
File destFile = new File(getFilesDir(), "destination.txt");
fs.copyFile(sourceFile, destFile);
```

### 存储空间监控

```java
// 获取存储信息
StorageInfo internalStorage = fs.getInternalStorageInfo();
StorageInfo externalStorage = fs.getExternalStorageInfo();

// 检查可用空间
long availableBytes = internalStorage.getAvailableBytes();
long totalBytes = internalStorage.getTotalBytes();
double usagePercent = (double) (totalBytes - availableBytes) / totalBytes * 100;

Log.d(TAG, "Internal storage usage: " + String.format("%.1f%%", usagePercent));

// 监控存储空间
fs.setStorageThreshold(0.9); // 90%阈值
fs.setStorageListener(new StorageListener() {
    @Override
    public void onStorageLow(StorageInfo info) {
        Log.w(TAG, "Storage space is low!");
        // 执行清理操作
        fs.cleanupCache();
    }

    @Override
    public void onStorageFull(StorageInfo info) {
        Log.e(TAG, "Storage is full!");
        // 紧急清理
        fs.cleanupTempFiles();
    }
});
```

### 权限管理

```java
// 检查文件访问权限
boolean canRead = fs.checkFileReadPermission(file);
boolean canWrite = fs.checkFileWritePermission(file);

// 请求存储权限
if (!fs.hasStoragePermission()) {
    fs.requestStoragePermission(activity);
}

// 处理权限结果
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == FilesystemManager.PERMISSION_REQUEST_STORAGE) {
        if (fs.handlePermissionResult(grantResults)) {
            // 权限已授予
            Log.d(TAG, "Storage permission granted");
        } else {
            // 权限被拒绝
            Log.w(TAG, "Storage permission denied");
        }
    }
}
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `FilesystemManager` | 文件系统管理器核心类 |
| `FilesystemConfig` | 文件系统配置类 |
| `StorageInfo` | 存储信息类 |
| `FileInfo` | 文件信息类 |
| `StorageListener` | 存储监听器接口 |

### 主要方法

#### FilesystemManager

```java
// 初始化管理器
void initialize(Context context)

// 获取单例实例
FilesystemManager getInstance()

// 文件操作
boolean createFile(File file)
boolean createDirectory(File directory)
boolean deleteFile(File file)
boolean deleteDirectory(File directory)
boolean copyFile(File src, File dst)
boolean moveFile(File src, File dst)

// 文件读写
boolean writeTextFile(File file, String content)
String readTextFile(File file)
boolean writeBinaryFile(File file, byte[] data)
byte[] readBinaryFile(File file)

// 目录操作
List<File> listFiles(File directory)
List<File> listFilesRecursive(File directory)
long getDirectorySize(File directory)

// 存储信息
StorageInfo getInternalStorageInfo()
StorageInfo getExternalStorageInfo()
boolean isExternalStorageAvailable()

// 权限管理
boolean hasStoragePermission()
void requestStoragePermission(Activity activity)
boolean handlePermissionResult(int[] grantResults)

// 清理操作
void cleanupCache()
void cleanupTempFiles()
long cleanupOldFiles(long maxAge)

// 路径处理
String getCanonicalPath(File file)
boolean isValidPath(String path)
String sanitizeFileName(String fileName)
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableAutoCleanup` | `boolean` | `true` | 是否启用自动清理 |
| `cleanupInterval` | `long` | `86400000` | 清理间隔(毫秒) |
| `maxCacheSize` | `long` | `104857600` | 最大缓存大小(字节) |
| `maxTempAge` | `long` | `604800000` | 临时文件最大年龄(毫秒) |
| `storageThreshold` | `double` | `0.8` | 存储空间阈值(百分比) |

## 📦 依赖项

```gradle
dependencies {
    // Android Library文件系统模块
    implementation 'com.hippo.library:filesystem:1.0.0'
}
```

## 🧪 测试

### 文件操作测试

```java
@Test
public void testFilesystemManager_createAndReadFile_shouldWorkCorrectly() {
    // Given
    FilesystemManager fs = FilesystemManager.getInstance();
    File testFile = new File(context.getCacheDir(), "test.txt");
    String testContent = "Test content";

    // When
    boolean created = fs.createFile(testFile);
    boolean written = fs.writeTextFile(testFile, testContent);
    String readContent = fs.readTextFile(testFile);

    // Then
    assertTrue(created);
    assertTrue(written);
    assertEquals(testContent, readContent);

    // Cleanup
    fs.deleteFile(testFile);
}
```

### 存储监控测试

```java
@Test
public void testStorageMonitoring() {
    // 测试存储空间监控功能
    // 1. 获取存储信息
    // 2. 设置阈值和监听器
    // 3. 模拟存储空间变化
    // 4. 验证监听器回调
}
```

## ⚠️ 注意事项

### 权限处理
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 安全考虑
- 文件操作要检查路径有效性
- 敏感文件要加密存储
- 避免路径遍历攻击

### 性能优化
- 大文件操作使用异步方式
- 定期清理临时文件和缓存
- 合理使用存储空间监控

### 兼容性
- 不同Android版本的文件系统差异
- 外部存储的可用性检查
- 分区存储的适配

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
