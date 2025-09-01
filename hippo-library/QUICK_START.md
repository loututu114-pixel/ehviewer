# 🚀 Hippo Library 快速开始指南

## 📖 目录
1. [项目简介](#项目简介)
2. [环境准备](#环境准备)
3. [快速集成](#快速集成)
4. [核心功能演示](#核心功能演示)
5. [最佳实践](#最佳实践)
6. [故障排除](#故障排除)

## 🎯 项目简介

Hippo Library 是专为Android开发者打造的通用组件库，提供30个功能模块，帮助你快速构建高质量的Android应用。

### 🌟 核心优势
- **拿来就用**: 标准化API，开箱即用
- **模块化**: 按需引入，避免包体积过大
- **高质量**: 完善的测试和文档
- **易维护**: 模块独立，更新不影响其他功能

### 📦 模块概览
- **8个核心模块**: Network, Database, UI, Utils, Settings, Notification, Image, Filesystem
- **22个专业模块**: 覆盖数据分析、安全、内容处理、网络增强、浏览器优化等各个领域

## 🔧 环境准备

### 系统要求
- **JDK**: 11+
- **Android Studio**: Arctic Fox (2020.3.1) 或更高版本
- **Android SDK**: API 21+ (Android 5.0+)
- **Gradle**: 8.0+

### 项目配置
```gradle
// Project build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
```

## ⚡ 快速集成

### 步骤1: 添加依赖

在项目根目录的 `settings.gradle.kts` 中添加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 步骤2: 创建应用

创建基本的Android应用：

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.example.myapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
```

### 步骤3: 集成Hippo Library

选择你需要的模块并添加依赖：

```kotlin
dependencies {
    // 核心模块 - 推荐所有应用都引入
    implementation("com.hippo.library:network:1.0.0")
    implementation("com.hippo.library:database:1.0.0")
    implementation("com.hippo.library:settings:1.0.0")
    implementation("com.hippo.library:utils:1.0.0")

    // 根据需求添加其他模块
    implementation("com.hippo.library:image-helper:1.0.0")     // 图片处理
    implementation("com.hippo.library:analytics:1.0.0")       // 数据分析
    implementation("com.hippo.library:notification:1.0.0")    // 通知管理
}
```

### 步骤4: 初始化模块

在Application类中初始化：

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化核心模块
        SettingsManager.initialize(this)

        // 初始化其他模块
        NetworkManager.getInstance(this)
        DatabaseManager.getInstance(this)
    }
}
```

## 🎮 核心功能演示

### 1. 网络请求

```kotlin
// 发送GET请求
NetworkManager.getInstance(context)
    .get("https://api.example.com/users")
    .enqueue(object : INetworkCallback<String> {
        override fun onSuccess(result: String) {
            Log.d(TAG, "Response: $result")
            // 处理成功响应
        }

        override fun onFailure(error: Exception) {
            Log.e(TAG, "Error: ${error.message}")
            // 处理错误
        }
    })
```

### 2. 数据库操作

```kotlin
// 初始化数据库
val dbManager = DatabaseManager.getInstance(context)

// 假设有User实体类
val userDao = dbManager.getDao(UserDao::class.java)

// 插入用户
val user = User(name = "张三", email = "zhangsan@example.com")
userDao.insert(user)

// 查询用户
val users = userDao.loadAll()
users.forEach { user ->
    Log.d(TAG, "User: ${user.name}")
}
```

### 3. 设置管理

```kotlin
// 存储设置
SettingsManager.getInstance().apply {
    putString("user_name", "张三")
    putBoolean("notifications_enabled", true)
    putInt("theme_mode", 1)
}

// 读取设置
val userName = SettingsManager.getInstance()
    .getString("user_name", "默认用户")
val notificationsEnabled = SettingsManager.getInstance()
    .getBoolean("notifications_enabled", false)
```

### 4. 工具类使用

```kotlin
// 字符串工具
val isValidEmail = StringUtils.isValidEmail("user@example.com")
val capitalized = StringUtils.capitalize("hello world")

// 日期工具
val currentDate = DateUtils.formatCurrentDate("yyyy-MM-dd")
val relativeTime = DateUtils.getRelativeTime(System.currentTimeMillis())

// 设备信息
val deviceModel = DeviceUtils.getDeviceModel()
val androidVersion = DeviceUtils.getAndroidVersion()
```

### 5. 图片处理

```kotlin
// 加载图片
ImageHelper.loadImage(context, imageUrl, imageView)

// 压缩图片
val compressedFile = ImageHelper.compressImage(
    originalFile = imageFile,
    maxWidth = 1920,
    maxHeight = 1080,
    quality = 80
)

// 获取图片信息
val imageInfo = ImageHelper.getImageInfo(imageFile)
Log.d(TAG, "Image size: ${imageInfo.width}x${imageInfo.height}")
```

### 6. 通知管理

```kotlin
// 发送简单通知
NotificationManager.getInstance().showNotification(
    title = "消息提醒",
    message = "您有一条新消息",
    iconResId = R.drawable.ic_notification
)

// 创建高级通知
val builder = NotificationManager.getInstance()
    .createNotificationBuilder("default")
    .setContentTitle("高级通知")
    .setContentText("通知内容")
    .setSmallIcon(R.drawable.ic_notification)
    .setAutoCancel(true)

NotificationManager.getInstance().showNotification(1, builder.build())
```

## 🏆 最佳实践

### 1. 模块选择原则
- **按需引入**: 只添加项目需要的模块
- **核心优先**: 先集成核心模块，再添加专业模块
- **版本统一**: 所有模块使用相同版本号

### 2. 性能优化
```kotlin
// 使用缓存
SettingsManager.getInstance().enableCache(true)

// 批量操作
databaseManager.runInTransaction {
    // 在事务中执行多个数据库操作
    userDao.insert(user1)
    userDao.insert(user2)
    settingsDao.update(setting1)
}
```

### 3. 错误处理
```kotlin
// 网络请求错误处理
networkManager.get(url).enqueue(object : INetworkCallback<String> {
    override fun onFailure(error: Exception) {
        when (error) {
            is NetworkException -> {
                when (error.errorCode) {
                    NetworkException.ERROR_TIMEOUT -> showRetryDialog()
                    NetworkException.ERROR_NO_NETWORK -> showNetworkDialog()
                    else -> showGenericError()
                }
            }
            else -> showGenericError()
        }
    }
})
```

### 4. 资源管理
```kotlin
// 在Application中统一管理
class MyApplication : Application() {
    override fun onTerminate() {
        super.onTerminate()
        // 清理所有模块资源
        NetworkManager.getInstance(this).cleanup()
        DatabaseManager.getInstance(this).close()
        NotificationManager.getInstance(this).cleanup()
    }
}
```

## 🔧 故障排除

### 常见问题

#### 1. 依赖冲突
```gradle
// 在gradle.properties中添加
android.enableJetifier=true
```

#### 2. 权限问题
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

#### 3. ProGuard混淆
```pro
# 保留Hippo Library类
-keep class com.hippo.library.** { *; }
```

#### 4. 版本兼容性
- 确保所有Hippo Library模块使用相同版本
- 检查Android Gradle Plugin版本兼容性
- 确认targetSdk和compileSdk设置正确

### 调试技巧

#### 启用调试日志
```kotlin
// 在Application中启用
SettingsManager.getInstance().putBoolean("debug_mode", true)
NetworkManager.getInstance(this).enableLogging(true)
```

#### 性能监控
```kotlin
// 监控模块性能
PerformanceMonitor.getInstance().startMonitoring()
PerformanceMonitor.getInstance().logMetrics()
```

## 📚 更多资源

### 官方文档
- [完整API文档](https://docs.hippo-library.dev/)
- [模块详细文档](./modules/)
- [示例代码](./examples/)

### 社区支持
- [GitHub Issues](https://github.com/your-org/hippo-library/issues)
- [GitHub Discussions](https://github.com/your-org/hippo-library/discussions)
- [Discord社区](https://discord.gg/hippo-library)

### 学习资源
- [快速开始视频教程](https://youtube.com/hippo-library)
- [博客文章](https://blog.hippo-library.dev/)
- [示例项目](https://github.com/your-org/hippo-library-examples)

---

## 🎉 恭喜！

你已经成功集成了Hippo Library！现在你可以享受到：

✅ **快速开发**: 标准化的API接口，开箱即用
✅ **高质量**: 完善的测试覆盖，保证稳定性
✅ **易维护**: 模块化设计，便于更新和维护
✅ **强扩展**: 支持灵活的功能扩展

开始构建你的下一个优秀应用吧！ 🚀
