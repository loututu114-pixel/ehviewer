# EhViewer 工具类模块 (Utils Module)

## 概述

工具类模块为EhViewer应用提供常用的工具类和辅助功能，包括文件操作、网络检测、字符串处理、日期时间处理等。该模块提供高质量的工具类，提高开发效率。

## 主要功能

### 1. 文件操作工具
- 文件读写操作
- 文件压缩解压
- 文件夹管理
- 文件类型检测

### 2. 网络工具
- 网络状态检测
- IP地址获取
- 域名解析
- 网络类型判断

### 3. 字符串处理工具
- 字符串编码转换
- 文本格式化
- 正则表达式匹配
- 字符串验证

### 4. 日期时间工具
- 日期格式化
- 时间戳转换
- 时区处理
- 日期计算

## 使用方法

```java
// 文件操作
FileUtils.createFile("/sdcard/test.txt");
String content = FileUtils.readTextFile("/sdcard/test.txt");
FileUtils.writeTextFile("/sdcard/test.txt", "Hello World!");

// 网络检测
if (NetworkUtils.isNetworkAvailable(context)) {
    String networkType = NetworkUtils.getNetworkType(context);
    Log.d(TAG, "Network type: " + networkType);
}

// 字符串处理
String encoded = StringUtils.encodeBase64("Hello World!");
String decoded = StringUtils.decodeBase64(encoded);

// 日期时间
String formattedDate = DateUtils.formatDate(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss");
long timestamp = DateUtils.parseDate("2023-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss");
```

## 许可证

本模块遵循Apache License 2.0协议。
