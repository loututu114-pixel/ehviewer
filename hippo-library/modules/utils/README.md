# 🛠️ 工具类模块 (Utils Module)

## 🎯 概述

Android Library工具类模块提供Android开发中常用的工具类和辅助方法，包括字符串处理、日期时间操作、设备信息获取、文件操作等功能，帮助开发者快速实现常见功能。

## ✨ 主要特性

- ✅ **字符串工具**: 字符串验证、格式化、编码转换
- ✅ **日期时间**: 日期格式化、时间计算、时区处理
- ✅ **设备信息**: 设备型号、系统版本、网络状态
- ✅ **文件操作**: 文件读写、路径处理、权限检查
- ✅ **网络工具**: IP地址验证、URL处理、域名解析
- ✅ **加密解密**: MD5、SHA、Base64编解码
- ✅ **系统工具**: 应用信息、存储空间、电池状态
- ✅ **UI工具**: 尺寸转换、颜色处理、键盘管理

## 🚀 快速开始

### 初始化工具类

```java
// 在Application中初始化（可选）
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化工具类（大部分工具类都是静态方法，无需显式初始化）
        Utils.init(this);
    }
}
```

### 字符串工具使用

```java
// 字符串验证
boolean isValidEmail = StringUtils.isValidEmail("user@example.com");
boolean isValidPhone = StringUtils.isValidPhone("13800138000");

// 字符串格式化
String formatted = StringUtils.format("Hello, {}!", "World");
String capitalized = StringUtils.capitalize("hello world");

// 编码转换
String base64 = StringUtils.encodeBase64("Hello World");
String decoded = StringUtils.decodeBase64(base64);
```

### 日期时间工具使用

```java
// 日期格式化
String currentDate = DateUtils.formatCurrentDate("yyyy-MM-dd");
String currentTime = DateUtils.formatCurrentTime("HH:mm:ss");

// 时间计算
long daysBetween = DateUtils.daysBetween(startDate, endDate);
boolean isToday = DateUtils.isToday(someDate);

// 相对时间
String relativeTime = DateUtils.getRelativeTime(someTimestamp);
```

### 设备信息获取

```java
// 设备基本信息
String deviceModel = DeviceUtils.getDeviceModel();
String androidVersion = DeviceUtils.getAndroidVersion();
String appVersion = DeviceUtils.getAppVersion(context);

// 网络状态
boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(context);
String networkType = NetworkUtils.getNetworkType(context);

// 存储信息
long totalSpace = StorageUtils.getTotalSpace();
long availableSpace = StorageUtils.getAvailableSpace();
```

### 文件操作工具

```java
// 文件读写
String content = FileUtils.readTextFile(file);
boolean success = FileUtils.writeTextFile(file, content);

// 文件信息
String fileSize = FileUtils.formatFileSize(file.length());
String fileExtension = FileUtils.getFileExtension(file.getName());

// 路径处理
String fileName = PathUtils.getFileName("/path/to/file.txt");
String parentPath = PathUtils.getParentPath("/path/to/file.txt");
```

## 📋 API 参考

### 核心工具类

| 类名 | 说明 |
|------|------|
| `StringUtils` | 字符串处理工具类 |
| `DateUtils` | 日期时间处理工具类 |
| `DeviceUtils` | 设备信息获取工具类 |
| `FileUtils` | 文件操作工具类 |
| `NetworkUtils` | 网络工具类 |
| `CryptoUtils` | 加密解密工具类 |
| `SystemUtils` | 系统信息工具类 |
| `UIUtils` | UI相关工具类 |

### 常用方法示例

#### StringUtils

```java
// 字符串验证
boolean isEmpty(String str)
boolean isBlank(String str)
boolean isValidEmail(String email)
boolean isValidPhone(String phone)

// 字符串处理
String capitalize(String str)
String uncapitalize(String str)
String reverse(String str)
String truncate(String str, int maxLength)

// 编码转换
String encodeBase64(String str)
String decodeBase64(String str)
String encodeUrl(String str)
String decodeUrl(String str)
```

#### DateUtils

```java
// 日期格式化
String formatDate(Date date, String pattern)
String formatCurrentDate(String pattern)
Date parseDate(String dateStr, String pattern)

// 时间计算
long daysBetween(Date start, Date end)
long hoursBetween(Date start, Date end)
boolean isToday(Date date)
boolean isYesterday(Date date)

// 相对时间
String getRelativeTime(long timestamp)
String getTimeAgo(long timestamp)
```

#### DeviceUtils

```java
// 设备信息
String getDeviceModel()
String getDeviceBrand()
String getAndroidVersion()
String getAndroidVersionCode()

// 应用信息
String getAppVersion(Context context)
int getAppVersionCode(Context context)
String getPackageName(Context context)

// 屏幕信息
int getScreenWidth(Context context)
int getScreenHeight(Context context)
float getScreenDensity(Context context)
```

#### FileUtils

```java
// 文件操作
boolean writeTextFile(File file, String content)
String readTextFile(File file)
boolean copyFile(File src, File dst)
boolean deleteFile(File file)

// 文件信息
String formatFileSize(long bytes)
String getFileExtension(String fileName)
String getFileNameWithoutExtension(String fileName)
boolean isImageFile(String fileName)
boolean isVideoFile(String fileName)
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableLogging` | `boolean` | `true` | 是否启用日志输出 |
| `defaultDateFormat` | `String` | `yyyy-MM-dd` | 默认日期格式 |
| `defaultTimeFormat` | `String` | `HH:mm:ss` | 默认时间格式 |
| `enableCache` | `boolean` | `true` | 是否启用结果缓存 |

## 📦 依赖项

```gradle
dependencies {
    // Android Library工具类模块
    implementation 'com.hippo.library:utils:1.0.0'
}
```

## 🧪 测试

### 单元测试

```java
@Test
public void testStringUtils_isValidEmail_shouldReturnTrueForValidEmail() {
    // Given
    String validEmail = "user@example.com";

    // When
    boolean result = StringUtils.isValidEmail(validEmail);

    // Then
    assertTrue(result);
}

@Test
public void testDateUtils_formatCurrentDate_shouldReturnFormattedDate() {
    // Given
    String pattern = "yyyy-MM-dd";

    // When
    String result = DateUtils.formatCurrentDate(pattern);

    // Then
    assertNotNull(result);
    assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
}
```

### 性能测试

```java
@Test
public void testFileUtils_performance() {
    // 测试文件操作性能
    // 1. 创建大文件
    // 2. 测量读写时间
    // 3. 验证性能指标
}
```

## ⚠️ 注意事项

### 性能考虑
- 大文件操作使用异步方式
- 字符串处理注意内存使用
- 频繁操作使用缓存机制

### 安全考虑
- 文件操作检查权限
- 敏感信息不输出到日志
- URL编码防止注入攻击

### 兼容性考虑
- 不同Android版本的API差异
- 文件系统权限变化
- 网络状态变化处理

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
