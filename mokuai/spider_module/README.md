# EhViewer 爬虫模块 (Spider Module)

## 概述

爬虫模块为EhViewer应用提供网络爬虫功能，支持网页抓取、数据提取、内容解析等。该模块采用多线程架构，支持分布式爬取和智能调度。

## 主要功能

### 1. 网页抓取
- 多线程网页抓取
- 智能请求调度
- 反爬虫检测和处理
- 代理服务器支持

### 2. 数据提取
- HTML内容解析
- 结构化数据提取
- 正则表达式匹配
- XPath选择器支持

### 3. 任务管理
- 爬虫任务队列
- 任务优先级管理
- 任务状态跟踪
- 任务重试机制

### 4. 存储管理
- 爬取数据存储
- 缓存管理
- 重复数据过滤
- 数据持久化

### 5. 监控和统计
- 爬取进度监控
- 性能统计
- 错误日志记录
- 资源使用监控

## 核心类

### SpiderQueen - 爬虫女王（核心管理器）
```java
public class SpiderQueen {
    // 获取单例实例
    public static SpiderQueen getInstance(Context context)

    // 任务管理
    public SpiderInfo createSpider(String url)
    public void startSpider(SpiderInfo spiderInfo)
    public void stopSpider(SpiderInfo spiderInfo)

    // 资源管理
    public SpiderDen getSpiderDen()
    public void shutdown()
}
```

### SpiderInfo - 爬虫信息
```java
public class SpiderInfo {
    // 基本信息
    private String url;
    private String title;
    private int status;
    private long createTime;
    private long updateTime;

    // 统计信息
    private int pagesCount;
    private int imagesCount;
    private long totalSize;

    // 状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_STOPPED = 4;
}
```

### SpiderDen - 爬虫巢穴（数据存储）
```java
public class SpiderDen {
    // 数据存储
    public void saveSpiderInfo(SpiderInfo info)
    public SpiderInfo loadSpiderInfo(String url)
    public List<SpiderInfo> getAllSpiderInfo()
    public void deleteSpiderInfo(String url)

    // 数据清理
    public void clearExpiredData()
    public void clearAllData()
}
```

## 使用方法

### 基本爬取

```java
// 获取爬虫女王实例
SpiderQueen spiderQueen = SpiderQueen.getInstance(context);

// 创建爬虫任务
SpiderInfo spiderInfo = spiderQueen.createSpider("https://example.com/gallery/123");

// 设置回调监听器
spiderInfo.setCallback(new SpiderCallback() {
    @Override
    public void onPageLoaded(String url, String title, int imageCount) {
        Log.d(TAG, "Page loaded: " + title + " (" + imageCount + " images)");
    }

    @Override
    public void onImageDownloaded(String url, long size) {
        Log.d(TAG, "Image downloaded: " + url + " (" + size + " bytes)");
    }

    @Override
    public void onCompleted(SpiderInfo info) {
        Log.d(TAG, "Spider completed: " + info.getTitle());
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Spider error: " + error);
    }
});

// 启动爬虫
spiderQueen.startSpider(spiderInfo);
```

### 高级配置

```java
// 配置爬虫参数
SpiderConfig config = new SpiderConfig();
config.setThreadCount(4);
config.setDelayBetweenRequests(1000); // 1秒延迟
config.setMaxRetries(3);
config.setTimeout(30000); // 30秒超时
config.setUserAgent("EhViewer/1.0");
config.setProxyHost("proxy.example.com");
config.setProxyPort(8080);

// 应用配置
spiderInfo.setConfig(config);
```

### 数据管理

```java
// 获取爬虫巢穴
SpiderDen spiderDen = spiderQueen.getSpiderDen();

// 保存爬虫信息
spiderDen.saveSpiderInfo(spiderInfo);

// 加载爬虫信息
SpiderInfo loadedInfo = spiderDen.loadSpiderInfo("https://example.com/gallery/123");

// 获取所有爬虫任务
List<SpiderInfo> allSpiders = spiderDen.getAllSpiderInfo();

// 删除爬虫信息
spiderDen.deleteSpiderInfo("https://example.com/gallery/123");
```

### 批量操作

```java
// 创建多个爬虫任务
List<String> urls = Arrays.asList(
    "https://example.com/gallery/1",
    "https://example.com/gallery/2",
    "https://example.com/gallery/3"
);

SpiderBatch batch = new SpiderBatch();
for (String url : urls) {
    SpiderInfo spiderInfo = spiderQueen.createSpider(url);
    batch.addSpider(spiderInfo);
}

// 设置批量回调
batch.setCallback(new SpiderBatchCallback() {
    @Override
    public void onBatchProgress(int completed, int total) {
        Log.d(TAG, "Batch progress: " + completed + "/" + total);
    }

    @Override
    public void onBatchCompleted() {
        Log.d(TAG, "Batch completed");
    }
});

// 启动批量任务
batch.start();
```

## 爬虫策略

### 深度优先策略
```java
SpiderStrategy depthFirst = new DepthFirstStrategy();
spiderInfo.setStrategy(depthFirst);
```

### 广度优先策略
```java
SpiderStrategy breadthFirst = new BreadthFirstStrategy();
spiderInfo.setStrategy(breadthFirst);
```

### 自定义策略
```java
SpiderStrategy customStrategy = new SpiderStrategy() {
    @Override
    public String getNextUrl() {
        // 自定义URL选择逻辑
        return selectOptimalUrl();
    }
};
spiderInfo.setStrategy(customStrategy);
```

## 数据解析

### HTML解析
```java
HtmlParser parser = new HtmlParser();
Document document = parser.parseHtml(htmlContent);

// 提取标题
String title = parser.extractTitle(document);

// 提取图片链接
List<String> imageUrls = parser.extractImageUrls(document);

// 提取元数据
Map<String, String> metadata = parser.extractMetadata(document);
```

### JSON解析
```java
JsonParser jsonParser = new JsonParser();
JSONObject jsonObject = jsonParser.parseJson(jsonString);

// 提取数据
String title = jsonParser.getString(jsonObject, "title");
int count = jsonParser.getInt(jsonObject, "count");
List<String> items = jsonParser.getStringList(jsonObject, "items");
```

## 错误处理

模块内置完善的错误处理机制：

```java
// 网络错误
NETWORK_ERROR,
// 解析错误
PARSE_ERROR,
// 权限错误
PERMISSION_ERROR,
// 存储错误
STORAGE_ERROR,
// 超时错误
TIMEOUT_ERROR,
// 未知错误
UNKNOWN_ERROR
```

## 性能优化

1. **连接池**: 复用HTTP连接，提高抓取效率
2. **并发控制**: 智能控制并发数量，避免服务器过载
3. **缓存机制**: 本地缓存已抓取的内容
4. **压缩传输**: 支持GZIP压缩，减少网络传输
5. **断点续传**: 支持大文件断点续传

## 反爬虫措施

1. **请求间隔**: 在请求间添加随机延迟
2. **User-Agent轮换**: 使用不同的User-Agent
3. **IP轮换**: 支持代理服务器轮换
4. **Cookie管理**: 自动管理会话Cookie
5. **请求头伪装**: 模拟真实浏览器请求

## 依赖项

在你的`build.gradle`文件中添加：

```gradle
dependencies {
    implementation 'org.jsoup:jsoup:1.15.3'        // HTML解析
    implementation 'com.squareup.okhttp3:okhttp:4.10.0'  // HTTP客户端
    implementation 'com.google.code.gson:gson:2.9.0'    // JSON解析
}
```

## 权限配置

在`AndroidManifest.xml`中添加必要权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## 监控和调试

```java
// 启用调试模式
SpiderQueen.setDebugMode(true);

// 设置日志监听器
SpiderQueen.setLogListener(new SpiderLogListener() {
    @Override
    public void onLog(String level, String message) {
        Log.d("Spider", "[" + level + "] " + message);
    }
});

// 获取性能统计
SpiderStats stats = spiderQueen.getStats();
Log.d(TAG, "Total requests: " + stats.getTotalRequests());
Log.d(TAG, "Success rate: " + stats.getSuccessRate() + "%");
```

## 示例项目

查看完整的示例代码：
- `BasicSpiderActivity.java` - 基础爬虫功能示例
- `AdvancedSpiderActivity.java` - 高级爬虫功能示例
- `BatchSpiderActivity.java` - 批量爬虫示例

## 注意事项

1. **合规性**: 确保爬虫行为符合网站服务条款
2. **频率控制**: 避免对目标服务器造成过大压力
3. **资源消耗**: 监控内存和网络资源使用情况
4. **错误处理**: 妥善处理各种异常情况
5. **数据质量**: 验证抓取数据的准确性和完整性

## 许可证

本模块遵循Apache License 2.0协议。
