[根目录](../../../../../CLAUDE.md) > [app](../../../../..) > [java](../../..) > [com](../..) > [hippo](..) > [ehviewer](.) > **client**

# 网络客户端模块文档

> EhViewer的网络通信层，负责HTTP请求、HTML解析、Cookie管理和数据处理

## 模块职责

客户端模块是EhViewer与服务器通信的核心，主要负责：
- **HTTP通信**: 基于OkHttp3的网络请求管理
- **HTML解析**: 使用JSoup解析网页内容和数据提取
- **Cookie管理**: 用户认证和会话状态维护
- **数据模型**: 网络数据到应用对象的转换
- **请求构建**: 各种API请求的封装和参数处理
- **SSL安全**: 自定义SSL工厂和证书验证
- **代理支持**: 网络代理和DNS over HTTPS

## 入口与启动

### 核心引擎初始化
```java
// app/src/main/java/com/hippo/ehviewer/client/EhEngine.java
public class EhEngine {
    public static void initialize() {
        // 初始化网络引擎
        // 设置默认配置
        // 注册解析器
    }
    
    // 核心网络请求方法
    public static Call<GalleryListResult> getGalleryList(
        OkHttpClient httpClient, String url, long refId) throws Exception {
        // 构建画廊列表请求
    }
}
```

### 客户端实例化
```java
// app/src/main/java/com/hippo/ehviewer/client/EhClient.java
public class EhClient {
    private final OkHttpClient mHttpClient;
    private final EhCookieStore mCookieStore;
    
    public EhClient(Context context) {
        mHttpClient = EhApplication.getOkHttpClient(context);
        mCookieStore = EhApplication.getEhCookieStore(context);
    }
}
```

## 对外接口

### 主要API接口

| 接口方法 | 功能 | 返回类型 | 说明 |
|---------|------|---------|------|
| `getGalleryList()` | 获取画廊列表 | `Call<GalleryListResult>` | 支持搜索、分页、筛选 |
| `getGalleryDetail()` | 获取画廊详情 | `Call<GalleryDetail>` | 包含图片列表、标签、评论 |
| `getPreviewSet()` | 获取预览图集 | `Call<PreviewSet>` | 缩略图和预览信息 |
| `getImageToken()` | 获取图片令牌 | `Call<String>` | 图片访问认证 |
| `downloadArchive()` | 下载压缩包 | `Call<Void>` | 批量下载支持 |
| `getProfile()` | 获取用户资料 | `Call<Profile>` | 用户信息和设置 |
| `login()` | 用户登录 | `Call<Void>` | 认证和Cookie设置 |
| `submitComment()` | 提交评论 | `Call<Void>` | 用户交互功能 |

### HTTP客户端配置
```java
// EhApplication中的OkHttpClient配置
OkHttpClient.Builder builder = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)  
    .writeTimeout(10, TimeUnit.SECONDS)
    .callTimeout(30, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .cookieJar(getEhCookieStore(application))
    .cache(getOkHttpCache(application))
    .dns(new EhHosts(application))
    .proxySelector(getEhProxySelector(application));
```

### 请求构建器
```java
// app/src/main/java/com/hippo/ehviewer/client/EhRequestBuilder.java
public class EhRequestBuilder {
    public static String getGalleryListUrl(int mode, String keyword, 
                                         int category, int page) {
        // 构建画廊列表URL
    }
    
    public static Request.Builder getImageRequest(String url, String referer) {
        // 构建图片请求
    }
}
```

## 关键依赖与配置

### 网络库依赖
```kotlin
// 核心网络库
implementation("com.squareup.okhttp3:okhttp:3.14.7")
implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:3.14.7")

// HTML解析
implementation("org.jsoup:jsoup:1.15.3")
implementation("org.ccil.cowan.tagsoup:tagsoup:1.2.1")

// SSL安全
implementation("org.conscrypt:conscrypt-android:2.5.1")

// JSON处理
implementation("com.alibaba:fastjson:1.2.83")
implementation("com.google.code.gson:gson:2.10.1")
```

### SSL配置
```java
// app/src/main/java/com/hippo/network/EhSSLSocketFactory.java
public class EhSSLSocketFactory extends SSLSocketFactory {
    // 自定义SSL Socket工厂
    // 支持TLS 1.2和现代加密套件
}

// app/src/main/java/com/hippo/network/EhX509TrustManager.java  
public class EhX509TrustManager implements X509TrustManager {
    // 自定义证书验证管理器
    // 支持自定义证书链验证
}
```

### DNS配置
```java
// app/src/main/java/com/hippo/ehviewer/client/EhHosts.java
public class EhHosts implements Dns {
    // 自定义DNS解析
    // 支持Hosts文件和DoH
    
    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        // 实现自定义域名解析逻辑
    }
}
```

## 数据模型

### 核心数据实体
```java
// 画廊信息
public class GalleryInfo implements Parcelable {
    public long gid;           // 画廊ID
    public String token;       // 访问令牌
    public String title;       // 标题
    public String thumb;       // 缩略图URL
    public int category;       // 分类
    public String posted;      // 发布时间
    public float rating;       // 评分
    public boolean favorited;  // 收藏状态
}

// 画廊详情
public class GalleryDetail extends GalleryInfo {
    public int pages;          // 页数
    public long size;          // 文件大小
    public String uploader;    // 上传者
    public List<GalleryTag> tags;  // 标签列表
    public List<GalleryComment> comments;  // 评论列表
    public PreviewSet previewSet;  // 预览图集
}
```

### 网络响应包装
```java
// 通用响应包装器
public class EhResponse<T> {
    public boolean success;    // 请求是否成功
    public T data;            // 响应数据
    public String error;      // 错误信息
    public int errorCode;     // 错误码
}

// 分页响应
public class PagedResponse<T> {
    public List<T> items;     // 数据列表
    public int totalPages;    // 总页数
    public int currentPage;   // 当前页
    public boolean hasMore;   // 是否有更多
}
```

## 解析器系统

### HTML解析器架构
```java
// 基础解析器接口
public interface Parser<T> {
    T parse(Document doc) throws ParseException;
}

// 具体解析器实现
public class GalleryListParser implements Parser<GalleryListResult> {
    @Override
    public GalleryListResult parse(Document doc) throws ParseException {
        // 解析画廊列表页面
        Elements galleryElements = doc.select(".gltc .gl1e");
        // 提取画廊信息
        return result;
    }
}
```

### 主要解析器列表
```
app/src/main/java/com/hippo/ehviewer/client/parser/
├── GalleryListParser.java         # 画廊列表解析
├── GalleryDetailParser.java       # 画廊详情解析  
├── PreviewParser.java             # 预览图解析
├── CommentParser.java             # 评论解析
├── ProfileParser.java             # 用户资料解析
├── FavoritesParser.java           # 收藏夹解析
├── ArchiveParser.java             # 压缩包解析
├── TorrentParser.java             # 种子文件解析
├── NewsParser.java                # 新闻解析
└── EventParser.java               # 事件解析
```

## 测试与质量

### 网络层测试策略
```java
// 模拟HTTP响应测试
@RunWith(RobolectricTestRunner.class)
public class EhEngineTest {
    private MockWebServer mockServer;
    private OkHttpClient testClient;
    
    @Before
    public void setUp() {
        mockServer = new MockWebServer();
        testClient = new OkHttpClient.Builder()
            .dispatcher(new Dispatcher(Executors.newSingleThreadExecutor()))
            .build();
    }
    
    @Test
    public void testGalleryListParsing() throws Exception {
        // 测试画廊列表解析
        mockServer.enqueue(new MockResponse()
            .setBody(loadTestResource("gallery_list.html")));
            
        GalleryListResult result = EhEngine.getGalleryList(
            testClient, mockServer.url("/").toString(), 0)
            .execute().body();
            
        assertThat(result.galleryInfoArray).hasLength(25);
    }
}
```

### 解析器单元测试
```java
public class GalleryDetailParserTest {
    @Test
    public void testParseGalleryDetail() throws Exception {
        Document doc = Jsoup.parse(loadTestHtml("gallery_detail.html"));
        GalleryDetailParser parser = new GalleryDetailParser();
        
        GalleryDetail detail = parser.parse(doc);
        
        assertThat(detail.title).isNotEmpty();
        assertThat(detail.pages).isGreaterThan(0);
        assertThat(detail.tags).isNotEmpty();
    }
}
```

## 错误处理

### 异常体系
```java
// 基础异常类
public class EhException extends Exception {
    public EhException(String message) {
        super(message);
    }
}

// 具体异常类型
public class ParseException extends EhException { /* 解析错误 */ }
public class NoHAtHClientException extends EhException { /* 客户端错误 */ }
public class CancelledException extends EhException { /* 取消错误 */ }
```

### 网络错误处理
```java
// 统一错误处理拦截器
public class ErrorHandlingInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        
        if (!response.isSuccessful()) {
            // 处理HTTP错误状态码
            handleHttpError(response.code());
        }
        
        return response;
    }
}
```

## 常见问题 (FAQ)

### Q: 网络请求超时如何处理？
A: 
1. 使用多级超时设置（连接、读取、写入、总超时）
2. 实现指数退避重试策略
3. 根据网络状况动态调整超时时间
4. 提供手动重试选项

### Q: SSL证书验证失败？
A: 
1. 检查系统时间是否正确
2. 使用自定义TrustManager处理特殊证书
3. 支持用户添加自定义证书
4. 提供证书pinning选项

### Q: Cookie丢失或失效？
A: 
1. 实现持久化Cookie存储
2. 定期验证Cookie有效性
3. 自动刷新过期Cookie
4. 提供重新登录机制

### Q: 解析器失效问题？
A: 
1. 版本化解析器实现
2. 提供多版本兼容性
3. 实现解析错误回退机制
4. 及时更新解析规则

## 相关文件清单

### 核心文件
```
app/src/main/java/com/hippo/ehviewer/client/
├── EhEngine.java                  # 网络引擎核心
├── EhClient.java                  # HTTP客户端封装
├── EhCookieStore.java            # Cookie存储管理
├── EhRequestBuilder.java         # 请求构建器
├── EhCacheKeyFactory.java        # 缓存键生成
├── EhUrlOpener.java              # URL处理器
├── EhUtils.java                  # 工具方法集合
├── EhConfig.java                 # 配置管理
├── EhUrl.java                    # URL常量定义
└── EhFilter.java                 # 内容过滤器
```

### 数据模型
```
app/src/main/java/com/hippo/ehviewer/client/data/
├── GalleryInfo.java              # 画廊基础信息
├── GalleryDetail.java            # 画廊详细信息
├── GalleryComment.java           # 画廊评论
├── GalleryTag.java               # 画廊标签
├── PreviewSet.java               # 预览图集
├── ArchiverData.java             # 压缩包数据
├── HomeDetail.java               # 首页数据
├── Profile.java                  # 用户资料
└── ... (20+ 数据模型)
```

### 网络基础设施
```
app/src/main/java/com/hippo/network/
├── EhSSLSocketFactory.java       # SSL Socket工厂
├── EhSSLSocketFactoryLowSDK.java # 低版本SSL工厂
├── EhX509TrustManager.java       # 证书信任管理
├── StatusCodeException.java      # HTTP状态码异常
├── CookieDatabase.java           # Cookie数据库
├── CookieRepository.java         # Cookie仓库
└── Network.java                  # 网络工具类
```

## 变更记录 (Changelog)

### 2025-09-06 03:01:06 - Client模块文档初始化
- 创建网络客户端模块文档
- 整理HTTP通信架构和接口
- 添加解析器系统和数据模型说明
- 补充SSL配置和安全策略
- 完善错误处理和测试策略

---

*本文档自动生成 - 最后更新: 2025-09-06 03:01:06*