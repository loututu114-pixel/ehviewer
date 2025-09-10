# ⚙️ 设置管理模块 (Settings Module)

## 🎯 概述

Android Library设置管理模块提供统一的配置管理和偏好设置功能，支持多种数据类型的存储、加密存储、设置监听等特性，帮助开发者轻松管理应用配置。

## ✨ 主要特性

- ✅ **多类型存储**: 支持String、int、boolean、long、float等多种数据类型
- ✅ **加密存储**: 支持敏感数据的AES加密存储
- ✅ **设置监听**: 实时监听设置变化
- ✅ **默认值支持**: 为每个设置项提供默认值
- ✅ **批量操作**: 支持批量读取和写入设置
- ✅ **导入导出**: 支持设置数据的导入导出
- ✅ **内存缓存**: 快速访问的内存缓存机制
- ✅ **跨进程同步**: 支持多进程间的设置同步

## 🚀 快速开始

### 初始化设置管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化设置管理器
        SettingsManager.initialize(this);
    }
}
```

### 基本设置操作

```java
// 获取设置管理器实例
SettingsManager settings = SettingsManager.getInstance();

// 存储不同类型的设置
settings.putString("user_name", "张三");
settings.putInt("user_age", 25);
settings.putBoolean("notifications_enabled", true);
settings.putLong("last_login_time", System.currentTimeMillis());

// 读取设置
String userName = settings.getString("user_name", "默认用户");
int userAge = settings.getInt("user_age", 0);
boolean notificationsEnabled = settings.getBoolean("notifications_enabled", true);
```

### 加密设置存储

```java
// 存储敏感信息
settings.putEncryptedString("api_key", "your_secret_api_key");
settings.putEncryptedString("user_token", "user_authentication_token");

// 读取加密信息
String apiKey = settings.getEncryptedString("api_key", "");
String userToken = settings.getEncryptedString("user_token", "");
```

### 设置监听器

```java
// 注册设置变化监听器
SettingsManager.getInstance().registerListener("user_name", new SettingChangeListener() {
    @Override
    public void onSettingChanged(String key, Object oldValue, Object newValue) {
        Log.d(TAG, "用户名从 " + oldValue + " 改为 " + newValue);
        // 更新UI或执行相关逻辑
        updateUserDisplay((String) newValue);
    }
});

// 移除监听器
settings.unregisterListener("user_name", listener);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `SettingsManager` | 设置管理器核心类 |
| `SettingsConfig` | 设置配置类 |
| `SettingChangeListener` | 设置变化监听器接口 |
| `EncryptedStorage` | 加密存储类 |

### 主要方法

#### SettingsManager

```java
// 初始化管理器
void initialize(Context context)

// 获取单例实例
SettingsManager getInstance()

// 存储字符串
void putString(String key, String value)

// 存储整数
void putInt(String key, int value)

// 存储布尔值
void putBoolean(String key, boolean value)

// 存储长整数
void putLong(String key, long value)

// 存储浮点数
void putFloat(String key, float value)

// 读取字符串
String getString(String key, String defaultValue)

// 读取整数
int getInt(String key, int defaultValue)

// 读取布尔值
boolean getBoolean(String key, boolean defaultValue)

// 读取长整数
long getLong(String key, long defaultValue)

// 读取浮点数
float getFloat(String key, float defaultValue)

// 存储加密字符串
void putEncryptedString(String key, String value)

// 读取加密字符串
String getEncryptedString(String key, String defaultValue)

// 检查设置是否存在
boolean contains(String key)

// 移除设置
void remove(String key)

// 清除所有设置
void clear()

// 注册监听器
void registerListener(String key, SettingChangeListener listener)

// 移除监听器
void unregisterListener(String key, SettingChangeListener listener)

// 导出设置
String exportSettings()

// 导入设置
boolean importSettings(String jsonData)
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableEncryption` | `boolean` | `true` | 是否启用加密存储 |
| `encryptionKey` | `String` | `auto-generated` | 加密密钥 |
| `enableCache` | `boolean` | `true` | 是否启用内存缓存 |
| `cacheSize` | `int` | `100` | 缓存最大条目数 |
| `enableBackup` | `boolean` | `true` | 是否启用自动备份 |
| `backupInterval` | `long` | `3600000` | 备份间隔(毫秒) |

## 📦 依赖项

```gradle
dependencies {
    // Android Library设置管理模块
    implementation 'com.hippo.library:settings:1.0.0'
}
```

## 🧪 测试

### 单元测试

```java
@Test
public void testSettingsManager_putAndGet_shouldWorkCorrectly() {
    // Given
    SettingsManager settings = SettingsManager.getInstance();
    String testKey = "test_key";
    String testValue = "test_value";

    // When
    settings.putString(testKey, testValue);
    String retrievedValue = settings.getString(testKey, "");

    // Then
    assertEquals(testValue, retrievedValue);
}
```

### 集成测试

```java
@RunWith(AndroidJUnit4::class)
public class SettingsIntegrationTest {

    @Test
    public void testSettingsPersistence() {
        // 测试设置的持久化
        // 1. 存储设置
        // 2. 重启应用
        // 3. 验证设置仍然存在
    }
}
```

## ⚠️ 注意事项

### 安全考虑
- 敏感信息使用加密存储
- 定期更换加密密钥
- 避免在日志中输出敏感信息

### 性能优化
- 频繁访问的设置使用缓存
- 批量操作减少I/O次数
- 合理设置缓存大小

### 数据迁移
- 应用升级时注意设置项兼容性
- 提供数据迁移方案
- 测试迁移过程的正确性

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
