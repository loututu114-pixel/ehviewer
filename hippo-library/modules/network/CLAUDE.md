# Network 网络模块

> [根目录](../../../CLAUDE.md) > [hippo-library](../../) > [modules](../) > **network**

---

## 模块职责

Network模块是hippo-library的网络通信子模块，提供了标准化的网络配置管理、异常处理、工具类等网络基础设施。该模块为上层应用提供统一的网络访问接口和最佳实践。

### 核心功能
- **网络配置管理**: 统一的网络请求参数配置
- **异常处理机制**: 标准化的网络异常定义和处理
- **网络工具类**: 常用的网络操作辅助方法
- **接口定义**: 标准化的网络配置和回调接口

---

## 入口与启动

### 主要入口点
- **NetworkManager**: 网络管理器主类
- **NetworkConfig**: 网络配置类
- **NetworkUtils**: 网络工具类

### 初始化流程
```java
// 典型的初始化流程
NetworkConfig config = new NetworkConfig.Builder()
    .setBaseUrl("https://api.example.com")
    .setConnectTimeout(10000)
    .setReadTimeout(30000)
    .setDebugEnabled(BuildConfig.DEBUG)
    .build();
    
NetworkManager manager = new NetworkManager(config);
```

---

## 对外接口

### 核心接口类

#### INetworkConfig 接口
```java
public interface INetworkConfig {
    /**
     * 获取基础URL
     */
    String getBaseUrl();
    
    /**
     * 获取连接超时时间
     */
    int getConnectTimeout();
    
    /**
     * 获取读取超时时间  
     */
    int getReadTimeout();
    
    /**
     * 是否启用调试模式
     */
    boolean isDebugEnabled();
    
    /**
     * 获取用户代理
     */
    String getUserAgent();
}
```

#### INetworkCallback 接口
```java
public interface INetworkCallback<T> {
    /**
     * 请求成功回调
     */
    void onSuccess(T result);
    
    /**
     * 请求失败回调
     */
    void onError(NetworkException exception);
    
    /**
     * 请求进度回调
     */
    void onProgress(int progress);
    
    /**
     * 请求开始回调
     */
    void onStart();
    
    /**
     * 请求完成回调（无论成功失败）
     */
    void onComplete();
}
```

### 主要API类

#### NetworkManager
```java
public class NetworkManager {
    // 网络管理器主要方法
    public void get(String url, INetworkCallback callback)
    public void post(String url, Object data, INetworkCallback callback)
    public void put(String url, Object data, INetworkCallback callback)
    public void delete(String url, INetworkCallback callback)
    public void download(String url, String savePath, INetworkCallback callback)
    public void upload(String url, File file, INetworkCallback callback)
    public void cancel(String requestTag)
    public void cancelAll()
}
```

#### NetworkConfig
```java
public class NetworkConfig implements INetworkConfig {
    // 网络配置参数
    private String baseUrl;
    private int connectTimeout;
    private int readTimeout;
    private int writeTimeout;
    private boolean debugEnabled;
    private String userAgent;
    private Map<String, String> defaultHeaders;
    
    public static class Builder {
        public Builder setBaseUrl(String baseUrl)
        public Builder setConnectTimeout(int timeout)
        public Builder setReadTimeout(int timeout)
        public Builder setWriteTimeout(int timeout)
        public Builder setDebugEnabled(boolean enabled)
        public Builder setUserAgent(String userAgent)
        public Builder addDefaultHeader(String key, String value)
        public NetworkConfig build()
    }
}
```

#### NetworkUtils
```java
public class NetworkUtils {
    // 网络工具辅助方法
    public static boolean isNetworkAvailable(Context context)
    public static boolean isWifiConnected(Context context)
    public static boolean isMobileConnected(Context context)
    public static String getNetworkType(Context context)
    public static String getIPAddress(Context context)
    public static boolean isValidUrl(String url)
    public static String encodeUrl(String url)
    public static String decodeUrl(String encodedUrl)
    public static Map<String, String> parseQueryParams(String url)
}
```

---

## 关键依赖与配置

### 模块依赖
```kotlin
// 假设的依赖配置
dependencies {
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // 测试依赖
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
    testImplementation 'org.mockito:mockito-core:4.11.0'
}
```

### 网络安全配置
```java
// NetworkConfig支持的安全配置
public class NetworkConfig {
    // SSL/TLS配置
    private boolean enableSslVerification = true;
    private String[] pinnedCertificates;
    private String[] trustedHosts;
    
    // 代理配置
    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    
    // 缓存配置
    private long cacheSize = 10 * 1024 * 1024; // 10MB
    private File cacheDirectory;
}
```

---

## 数据模型

### 核心实体类

#### NetworkException 网络异常
```java
public class NetworkException extends Exception {
    public static final int ERROR_NETWORK_UNAVAILABLE = 1001;
    public static final int ERROR_TIMEOUT = 1002;
    public static final int ERROR_SERVER_ERROR = 1003;
    public static final int ERROR_PARSE_ERROR = 1004;
    public static final int ERROR_UNKNOWN = 1005;
    
    private int errorCode;
    private String errorMessage;
    private Throwable cause;
    
    public NetworkException(int errorCode, String message)
    public NetworkException(int errorCode, String message, Throwable cause)
    
    // Getter方法
    public int getErrorCode()
    public String getErrorMessage()
}
```

#### RequestInfo 请求信息
```java
public class RequestInfo {
    private String requestId;          // 请求ID
    private String url;               // 请求URL
    private String method;            // HTTP方法
    private Map<String, String> headers; // 请求头
    private Object requestBody;       // 请求体
    private long startTime;          // 开始时间
    private long endTime;            // 结束时间
    private int responseCode;        // 响应码
    private String tag;              // 请求标签
}
```

#### ResponseInfo 响应信息
```java
public class ResponseInfo<T> {
    private int statusCode;          // HTTP状态码
    private String statusMessage;   // 状态消息
    private Map<String, String> headers; // 响应头
    private T data;                 // 响应数据
    private long responseTime;      // 响应时间
    private long contentLength;     // 内容长度
}
```

---

## 测试与质量

### 测试文件结构
```
src/test/java/
└── com/hippo/library/module/network/
    ├── NetworkManagerTest.java         # 网络管理器测试
    ├── NetworkConfigTest.java          # 网络配置测试
    ├── NetworkUtilsTest.java           # 网络工具测试
    ├── exception/
    │   └── NetworkExceptionTest.java   # 网络异常测试
    └── interfaces/
        ├── INetworkConfigTest.java     # 配置接口测试
        └── INetworkCallbackTest.java   # 回调接口测试
```

### 测试覆盖目标

#### ✅ 应该覆盖的测试
- **网络配置测试**: 配置参数的验证和默认值
- **请求发送测试**: GET、POST、PUT、DELETE请求
- **异常处理测试**: 各种网络异常的正确处理
- **工具类测试**: 网络状态检测、URL处理等工具方法
- **回调机制测试**: 成功、失败、进度回调的正确触发

#### ❌ 当前缺失的测试
- 所有测试文件都需要创建
- 需要使用MockWebServer模拟网络请求
- 需要测试不同网络环境下的行为

### 建议的测试用例

#### NetworkManagerTest.java
```java
@Test
public void testGetRequest() {
    // 测试GET请求
}

@Test
public void testPostRequest() {
    // 测试POST请求
}

@Test
public void testRequestTimeout() {
    // 测试请求超时处理
}

@Test
public void testRequestCancel() {
    // 测试请求取消
}
```

#### NetworkUtilsTest.java
```java
@Test
public void testIsNetworkAvailable() {
    // 测试网络可用性检测
}

@Test
public void testUrlValidation() {
    // 测试URL格式验证
}

@Test
public void testQueryParamsParsing() {
    // 测试查询参数解析
}
```

---

## 常见问题 (FAQ)

### Q: 如何配置请求超时时间？
A: 通过NetworkConfig的Builder配置：
```java
NetworkConfig config = new NetworkConfig.Builder()
    .setConnectTimeout(15000)  // 连接超时15秒
    .setReadTimeout(30000)     // 读取超时30秒
    .setWriteTimeout(30000)    // 写入超时30秒
    .build();
```

### Q: 如何处理网络异常？
A: 使用标准化的NetworkException：
```java
networkManager.get(url, new INetworkCallback<String>() {
    @Override
    public void onSuccess(String result) {
        // 处理成功响应
    }
    
    @Override
    public void onError(NetworkException exception) {
        switch (exception.getErrorCode()) {
            case NetworkException.ERROR_TIMEOUT:
                // 处理超时
                break;
            case NetworkException.ERROR_NETWORK_UNAVAILABLE:
                // 处理网络不可用
                break;
            default:
                // 处理其他错误
                break;
        }
    }
});
```

### Q: 如何设置默认请求头？
A: 在NetworkConfig中配置默认请求头：
```java
NetworkConfig config = new NetworkConfig.Builder()
    .addDefaultHeader("User-Agent", "MyApp/1.0")
    .addDefaultHeader("Accept", "application/json")
    .build();
```

### Q: 如何实现请求缓存？
A: 通过配置缓存目录和大小：
```java
NetworkConfig config = new NetworkConfig.Builder()
    .setCacheDirectory(new File(context.getCacheDir(), "network_cache"))
    .setCacheSize(20 * 1024 * 1024)  // 20MB缓存
    .build();
```

---

## 相关文件清单

### 核心源代码文件
```
src/main/java/com/hippo/library/module/network/
├── NetworkManager.java              # 网络管理器主类
├── NetworkConfig.java               # 网络配置类
├── utils/
│   └── NetworkUtils.java           # 网络工具类
├── exception/
│   └── NetworkException.java       # 网络异常类
└── interfaces/
    ├── INetworkConfig.java          # 网络配置接口
    └── INetworkCallback.java        # 网络回调接口
```

### 配置文件
```
src/main/resources/
├── network-config.properties       # 默认网络配置
└── certs/                          # SSL证书文件
    ├── trusted-certs.pem
    └── pinned-certs.pem
```

### 测试文件（需要创建）
```
src/test/java/com/hippo/library/module/network/
├── NetworkManagerTest.java
├── NetworkConfigTest.java
├── utils/
│   └── NetworkUtilsTest.java
├── exception/
│   └── NetworkExceptionTest.java
└── interfaces/
    ├── INetworkConfigTest.java
    └── INetworkCallbackTest.java

src/androidTest/java/com/hippo/library/module/network/
├── NetworkIntegrationTest.java
└── NetworkUtilsInstrumentedTest.java
```

---

## API使用示例

### 基础使用
```java
// 1. 创建网络配置
NetworkConfig config = new NetworkConfig.Builder()
    .setBaseUrl("https://api.example.com/v1/")
    .setConnectTimeout(10000)
    .setReadTimeout(30000)
    .setDebugEnabled(true)
    .addDefaultHeader("Authorization", "Bearer " + token)
    .build();

// 2. 创建网络管理器
NetworkManager networkManager = new NetworkManager(config);

// 3. 发送GET请求
networkManager.get("/users/profile", new INetworkCallback<UserProfile>() {
    @Override
    public void onSuccess(UserProfile profile) {
        // 处理成功响应
        updateUI(profile);
    }
    
    @Override
    public void onError(NetworkException exception) {
        // 处理错误
        showErrorMessage(exception.getErrorMessage());
    }
    
    @Override
    public void onProgress(int progress) {
        // 更新进度
        updateProgressBar(progress);
    }
});
```

### 文件下载
```java
// 文件下载示例
String downloadUrl = "https://example.com/files/document.pdf";
String savePath = "/sdcard/downloads/document.pdf";

networkManager.download(downloadUrl, savePath, new INetworkCallback<File>() {
    @Override
    public void onSuccess(File file) {
        // 下载成功
        openFile(file);
    }
    
    @Override
    public void onError(NetworkException exception) {
        // 下载失败
        handleDownloadError(exception);
    }
    
    @Override
    public void onProgress(int progress) {
        // 更新下载进度
        downloadProgressBar.setProgress(progress);
    }
});
```

### 网络状态检测
```java
// 网络工具类使用示例
if (NetworkUtils.isNetworkAvailable(context)) {
    if (NetworkUtils.isWifiConnected(context)) {
        // WiFi连接，可以进行大量数据传输
        performHeavyNetworkOperation();
    } else if (NetworkUtils.isMobileConnected(context)) {
        // 移动网络连接，控制数据使用量
        performLightNetworkOperation();
    }
} else {
    // 无网络连接
    showNoNetworkMessage();
}
```

### 高级配置
```java
NetworkConfig advancedConfig = new NetworkConfig.Builder()
    .setBaseUrl("https://secure-api.example.com/")
    .setConnectTimeout(15000)
    .setReadTimeout(60000)
    .setWriteTimeout(60000)
    .setDebugEnabled(BuildConfig.DEBUG)
    .setUserAgent("MyApp/2.0 (Android)")
    .addDefaultHeader("Accept", "application/json")
    .addDefaultHeader("Content-Type", "application/json")
    .setEnableSslVerification(true)
    .setPinnedCertificates(new String[]{"sha256/HASH1", "sha256/HASH2"})
    .setCacheSize(50 * 1024 * 1024)  // 50MB缓存
    .setProxyHost("proxy.company.com")
    .setProxyPort(8080)
    .build();
```

---

## 变更记录 (Changelog)

### 2025-09-10
- **模块文档创建**: 完成network模块的架构分析
- **接口设计整理**: 定义了核心的网络配置和回调接口
- **异常处理机制**: 设计了统一的网络异常处理体系
- **工具类规划**: 规划了常用的网络工具方法
- **测试策略**: 制定了完整的测试覆盖计划

### 待办事项
- [ ] 实现NetworkManager的完整功能
- [ ] 创建完整的单元测试套件
- [ ] 添加SSL证书固定功能
- [ ] 实现智能重试机制
- [ ] 增加网络请求缓存功能
- [ ] 完善网络监控和统计功能

---

<div align="center">

[⬆ 返回根目录](../../../CLAUDE.md) | [📚 hippo-library](../../) | [🌐 Network模块](./CLAUDE.md)

**Network模块文档** - EhViewerh v2.0.0.5

</div>