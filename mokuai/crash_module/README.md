# 💥 崩溃处理模块 (Crash Module)

## 🎯 概述

EhViewer崩溃处理模块提供完整的应用崩溃检测、日志记录和异常处理功能。通过自动捕获未处理的异常、生成详细的崩溃报告，并提供恢复机制，确保应用的稳定性和用户体验。

## ✨ 主要特性

- ✅ **自动崩溃捕获**：捕获所有未处理的异常
- ✅ **详细日志记录**：生成包含设备信息和堆栈的崩溃报告
- ✅ **多重存储**：文件存储和SharedPreferences双重保存
- ✅ **崩溃回调**：提供回调接口处理崩溃事件
- ✅ **手动异常记录**：支持手动记录异常信息
- ✅ **日志管理**：清理和查询历史崩溃日志

## 🚀 快速开始

### 基本使用

```java
// 获取崩溃处理器实例
CrashHandler crashHandler = CrashHandler.getInstance(context);

// 设置崩溃回调
crashHandler.setCallback(new CrashHandler.CrashCallback() {
    @Override
    public void onCrash(Thread thread, Throwable throwable) {
        Log.e(TAG, "Application crashed", throwable);
        // 执行崩溃时的清理工作
        cleanupOnCrash();
    }

    @Override
    public void onCrashReported(String crashLog) {
        Log.i(TAG, "Crash reported: " + crashLog);
        // 可以在这里上报崩溃日志到服务器
        reportCrashToServer(crashLog);
    }

    @Override
    public boolean shouldRestartApp() {
        // 返回true表示崩溃后重启应用
        return true;
    }
});

// 启用崩溃处理（默认启用）
crashHandler.setEnabled(true);

// 手动记录异常
try {
    riskyOperation();
} catch (Exception e) {
    crashHandler.logException(e);
}

// 获取上次崩溃信息
String lastCrashLog = crashHandler.getLastCrashLog();
long lastCrashTime = crashHandler.getLastCrashTime();

// 获取所有崩溃日志文件
File[] crashFiles = crashHandler.getCrashLogFiles();

// 清理崩溃日志
crashHandler.clearCrashLogs();
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `CrashHandler` | 崩溃处理器核心类 |

### 主要方法

#### CrashHandler

```java
// 获取实例
CrashHandler getInstance(Context context)

// 设置回调和配置
void setCallback(CrashCallback callback)
void setEnabled(boolean enabled)

// 日志管理
String getLastCrashLog()
long getLastCrashTime()
void clearCrashLogs()
File[] getCrashLogFiles()

// 手动记录异常
void logException(Throwable throwable)
void logException(Thread thread, Throwable throwable, String tag)

// 测试功能
void triggerTestCrash()
```

#### CrashCallback

```java
// 崩溃事件回调
void onCrash(Thread thread, Throwable throwable)
void onCrashReported(String crashLog)
boolean shouldRestartApp()
```

## 📦 依赖项

```gradle
dependencies {
    // 核心依赖
    implementation 'com.example:crash-module:1.0.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 初始化时机
```java
// 在Application的onCreate中尽早初始化
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 首先初始化崩溃处理器
        CrashHandler crashHandler = CrashHandler.getInstance(this);
        crashHandler.setCallback(new CrashCallbackImpl());

        // 然后初始化其他组件
        initOtherComponents();
    }
}
```

### 日志存储位置
- **内部存储**: `/data/data/{package}/shared_prefs/crash_logs.xml`
- **外部存储**: `/Android/data/{package}/files/crash_logs/`

## 🔄 工作原理

### 崩溃捕获机制

```java
// 全局异常处理器设置
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private CrashHandler(Context context) {
        // 保存默认处理器
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        // 设置自己为默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // 处理崩溃
        handleCrash(thread, throwable);

        // 调用默认处理器
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}
```

### 日志生成格式

```java
// 崩溃日志格式
private String generateCrashLog(Thread thread, Throwable throwable) {
    return "=== EhViewer Crash Report ===\n" +
           "Time: " + timestamp + "\n" +
           "Thread: " + thread.getName() + "\n" +
           "App Version: " + versionName + "\n" +
           "Device: " + Build.MODEL + "\n" +
           "Android Version: " + Build.VERSION.RELEASE + "\n" +
           "=== Stack Trace ===\n" +
           stackTrace;
}
```

## 🧪 测试

### 单元测试
```java
@Test
public void testCrashHandlerInitialization() {
    // Given
    CrashHandler crashHandler = CrashHandler.getInstance(context);

    // When
    crashHandler.setEnabled(true);

    // Then
    assertNotNull(crashHandler);
    assertNotNull(crashHandler.getLastCrashLog());
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4.class)
public class CrashIntegrationTest {

    @Test
    public void testCrashLogPersistence() {
        // 测试崩溃日志的持久化存储
        // 1. 触发测试崩溃
        // 2. 验证日志文件存在
        // 3. 验证SharedPreferences中保存了日志
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/CrashHandler`)
3. 提交更改 (`git commit -m 'Add crash handling feature'`)
4. 推送到分支 (`git push origin feature/CrashHandler`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

---

**💡 提示**: 该模块适用于所有需要崩溃检测和错误处理的应用，是应用稳定性的重要保障。
