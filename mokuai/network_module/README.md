# EhViewer 网络请求模块 (Network Module)

## 概述

网络请求模块为EhViewer应用提供完整的网络通信功能，包括HTTP请求处理、Cookie管理、缓存机制、请求重试等。该模块采用OkHttp作为底层HTTP客户端，支持异步请求和并发控制。

## 主要功能

### 1. HTTP请求处理
- 支持GET、POST、PUT、DELETE等HTTP方法
- 异步请求处理
- 请求取消机制
- 请求超时控制

### 2. Cookie管理
- 自动Cookie存储和管理
- 跨域Cookie支持
- Cookie持久化存储

### 3. 缓存机制
- 内存缓存和文件缓存
- 缓存过期时间控制
- 缓存大小限制
- 自动缓存清理

### 4. 网络状态检测
- 网络连接状态监控
- 网络类型检测（WiFi/移动网络）
- 网络变化监听

### 5. 请求重试机制
- 自动请求重试
- 可配置重试次数
- 重试间隔控制

## 使用方法

### 基本使用

```java
// 1. 初始化网络客户端
Context context = getApplicationContext();
NetworkClient client = new NetworkClient(context);

// 2. 创建网络请求
NetworkClient.NetworkCallback<String> callback = new NetworkClient.NetworkCallback<String>() {
    @Override
    public void onSuccess(String result) {
        // 处理成功结果
        Log.d(TAG, "Request success: " + result);
    }

    @Override
    public void onFailure(Exception e) {
        // 处理失败
        Log.e(TAG, "Request failed: " + e.getMessage());
    }

    @Override
    public void onCancel() {
        // 处理取消
        Log.d(TAG, "Request cancelled");
    }
};

// 3. 执行GET请求
Object[] args = {"https://api.example.com/data"};
NetworkRequest request = new NetworkRequest(NetworkClient.METHOD_GET, args, callback);
client.execute(request);

// 4. 取消请求（可选）
request.cancel();
```

### 配置网络请求

```java
// 创建自定义配置
NetworkClient.NetworkConfig config = new NetworkClient.NetworkConfig();
config.setTimeout(5000); // 5秒超时
config.setEnableCache(true); // 启用缓存
config.setEnableRetry(true); // 启用重试
config.setMaxRetries(3); // 最大重试3次

// 使用配置创建请求
NetworkRequest request = new NetworkRequest(NetworkClient.METHOD_GET, args, callback, config);
```

### Cookie管理

```java
// 获取Cookie管理器
CookieManager cookieManager = new CookieManager(context);

// 添加Cookie
URI uri = new URI("https://api.example.com");
HttpCookie cookie = new HttpCookie("session", "abc123");
cookieManager.add(uri, cookie);

// 获取Cookie
List<HttpCookie> cookies = cookieManager.get(uri);

// 清除Cookie
cookieManager.removeAll();
```

### 缓存管理

```java
// 获取缓存管理器
NetworkCacheManager cacheManager = new NetworkCacheManager(context);

// 存储缓存
cacheManager.put("user_data", userData);

// 获取缓存
Object cachedData = cacheManager.get("user_data");

// 删除缓存
cacheManager.remove("user_data");

// 清空所有缓存
cacheManager.clear();
```

## 依赖项

在你的`build.gradle`文件中添加以下依赖：

```gradle
dependencies {
    implementation 'com.squareup.okhttp3:okhttp:4.10.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.10.0'
    implementation 'org.jsoup:jsoup:1.15.3' // 如果需要HTML解析
}
```

## 权限配置

在`AndroidManifest.xml`中添加必要的权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 高级用法

### 自定义HTTP客户端

```java
public class CustomNetworkClient extends NetworkClient {
    @Override
    protected OkHttpClient createOkHttpClient() {
        return new OkHttpClient.Builder()
            .addInterceptor(new CustomInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }
}
```

### 网络状态监听

```java
public class NetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // 网络已连接
            Log.d(TAG, "Network connected");
        } else {
            // 网络已断开
            Log.d(TAG, "Network disconnected");
        }
    }
}
```

## 注意事项

1. **线程安全**: 网络客户端是线程安全的，可以在多个线程中同时使用。

2. **内存泄漏**: 使用完网络请求后，确保调用`cancel()`方法取消请求，避免内存泄漏。

3. **缓存清理**: 定期清理缓存，避免占用过多存储空间。

4. **网络权限**: 确保应用具有网络访问权限。

5. **超时设置**: 根据网络环境合理设置请求超时时间。

## 示例项目

查看`examples/`目录中的完整示例代码，了解如何在实际项目中使用网络模块。

## 许可证

本模块遵循Apache License 2.0协议。
