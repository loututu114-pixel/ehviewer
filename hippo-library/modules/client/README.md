# 🌐 客户端模块 (Client Module)

## 🎯 概述

Android Library客户端模块提供统一的网络客户端管理功能，支持HTTP/HTTPS请求、连接池管理、请求拦截器等高级特性，帮助开发者轻松管理网络通信。

## ✨ 主要特性

- ✅ **统一客户端管理**: 集中管理所有HTTP客户端实例
- ✅ **连接池优化**: 智能连接复用，减少资源消耗
- ✅ **请求拦截器**: 支持请求和响应的拦截处理
- ✅ **证书管理**: HTTPS证书验证和信任管理
- ✅ **超时控制**: 灵活的连接和读取超时配置
- ✅ **重试机制**: 自动重试失败的请求
- ✅ **并发控制**: 限制并发请求数量
- ✅ **日志记录**: 详细的网络请求日志

## 🚀 快速开始

### 初始化客户端管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化客户端管理器
        ClientManager.initialize(this);

        // 配置客户端
        ClientConfig config = new ClientConfig.Builder()
            .setMaxConnections(10)
            .setConnectionTimeout(30000)
            .setReadTimeout(60000)
            .enableRetryOnFailure(true)
            .build();

        ClientManager.getInstance().setConfig(config);
    }
}
```

### 创建HTTP客户端

```java
// 获取配置好的客户端
OkHttpClient client = ClientManager.getInstance().getClient();

// 使用客户端发送请求
Request request = new Request.Builder()
    .url("https://api.example.com/data")
    .build();

client.newCall(request).enqueue(new Callback() {
    @Override
    public void onResponse(Call call, Response response) throws IOException {
        // 处理响应
        String result = response.body().string();
        Log.d(TAG, "Response: " + result);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        // 处理错误
        Log.e(TAG, "Request failed", e);
    }
});
```

### 添加请求拦截器

```java
// 添加请求头拦截器
ClientManager.getInstance().addInterceptor(new Interceptor() {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request request = original.newBuilder()
            .header("User-Agent", "MyApp/1.0")
            .header("Authorization", "Bearer " + getToken())
            .build();
        return chain.proceed(request);
    }
});

// 添加日志拦截器
ClientManager.getInstance().addInterceptor(new HttpLoggingInterceptor()
    .setLevel(HttpLoggingInterceptor.Level.BODY));
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `ClientManager` | 客户端管理器核心类 |
| `ClientConfig` | 客户端配置类 |
| `ConnectionPool` | 连接池管理类 |
| `CertificateManager` | 证书管理类 |

### 主要方法

#### ClientManager

```java
// 初始化管理器
void initialize(Context context)

// 获取单例实例
ClientManager getInstance()

// 获取HTTP客户端
OkHttpClient getClient()

// 设置配置
void setConfig(ClientConfig config)

// 添加拦截器
void addInterceptor(Interceptor interceptor)

// 移除拦截器
void removeInterceptor(Interceptor interceptor)

// 清理资源
void cleanup()

// 关闭所有连接
void shutdown()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `maxConnections` | `int` | `10` | 最大连接数 |
| `connectionTimeout` | `long` | `30000` | 连接超时(毫秒) |
| `readTimeout` | `long` | `60000` | 读取超时(毫秒) |
| `writeTimeout` | `long` | `60000` | 写入超时(毫秒) |
| `retryOnFailure` | `boolean` | `true` | 是否重试失败请求 |
| `maxRetryCount` | `int` | `3` | 最大重试次数 |
| `enableLogging` | `boolean` | `true` | 是否启用日志 |

## 📦 依赖项

```gradle
dependencies {
    // OkHttp客户端
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // Android Library客户端模块
    implementation 'com.hippo.library:client:1.0.0'
}
```

## 🧪 测试

### 单元测试

```java
@Test
public void testClientManager_initialization_shouldCreateValidClient() {
    // Given
    Context context = ApplicationProvider.getApplicationContext();

    // When
    ClientManager manager = ClientManager.getInstance();
    OkHttpClient client = manager.getClient();

    // Then
    assertNotNull(client);
    assertNotNull(client.connectionPool());
}
```

### 集成测试

```java
@RunWith(AndroidJUnit4::class)
public class ClientIntegrationTest {

    @Test
    public void testFullRequestFlow() {
        // 完整的请求流程测试
        // 1. 初始化客户端
        // 2. 发送请求
        // 3. 验证响应
        // 4. 清理资源
    }
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
