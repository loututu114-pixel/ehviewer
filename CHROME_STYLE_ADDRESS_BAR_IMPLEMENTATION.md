# Chrome 风格地址栏实现文档

## 项目结构

```
app/src/main/java/com/hippo/ehviewer/ui/browser/
├── RealtimeSuggestionManager.java          # 实时建议管理器 ⭐
├── EnhancedSuggestionAdapter.java          # 增强建议适配器
├── SmartAddressBarWidget.java              # 智能地址栏组件
├── NetworkSuggestionProvider.java          # 网络建议提供者
└── ChromeStyleAddressBarDemoActivity.java  # 演示活动

app/src/main/res/layout/
├── widget_smart_address_bar.xml            # 地址栏布局
├── item_enhanced_suggestion.xml            # 建议项布局
├── item_suggestion_group_header.xml        # 分组头布局
└── activity_chrome_style_demo.xml          # 演示活动布局

app/src/main/res/drawable/
├── shape_rounded_background.xml            # 圆角背景
└── suggestion_item_background.xml          # 建议项背景选择器
```

## 核心类详细说明

### RealtimeSuggestionManager

**职责**: 统筹管理所有建议源，协调数据获取和排序

**关键方法**:
```java
// 请求建议（异步）
public void requestSuggestions(String query, SuggestionCallback callback)

// 获取内部建议（同步）
private List<SuggestionItem> getSuggestionsInternal(String query)

// 智能匹配算法
private boolean isSmartMatch(String query, String title, String url)

// 记录URL访问
public void recordUrlVisit(String url, String title)
```

**设计模式**: 单例模式，保证全局唯一实例

### EnhancedSuggestionAdapter

**职责**: 处理建议列表的显示、分组和交互

**关键特性**:
- 支持分组显示（历史、书签、搜索等）
- 键盘导航高亮
- 查询文本高亮
- 类型图标和颜色区分

**ViewHolder 结构**:
```java
static class SuggestionViewHolder extends RecyclerView.ViewHolder {
    TextView titleText, urlText, typeText;
    ImageView typeIcon;
}
```

### SmartAddressBarWidget

**职责**: 封装地址栏UI和交互逻辑

**核心功能**:
- 实时文本监听和建议请求
- 键盘事件处理（方向键、Tab、Enter、ESC）
- 建议选择和导航
- URL提交和状态管理

**监听器接口**:
```java
public interface OnAddressBarListener {
    void onUrlSubmit(String url);
    void onSuggestionClick(SuggestionItem item);
    void onSuggestionLongClick(SuggestionItem item);
}
```

### NetworkSuggestionProvider

**职责**: 从各大搜索引擎获取搜索建议

**支持的搜索引擎**:
- Google (https://www.google.com/complete/search)
- Bing (https://www.bing.com/AS/Suggestions)
- 百度 (https://www.baidu.com/su)
- DuckDuckGo (https://duckduckgo.com/ac)
- 搜狗 (https://www.sogou.com/web)

**缓存策略**:
- LRU缓存: 100项
- 有效期: 5分钟
- 自动清理过期项

## 数据模型

### SuggestionItem
```java
public static class SuggestionItem implements Comparable<SuggestionItem> {
    public final String text;           // 原始文本
    public final String url;            // 关联URL
    public final String displayText;    // 显示文本
    public final SuggestionType type;   // 建议类型
    public final long score;           // 排序分数
    public final long timestamp;       // 时间戳
}
```

### SuggestionType 枚举
```java
public enum SuggestionType {
    HISTORY("历史记录", "📖"),
    BOOKMARK("书签", "⭐"),
    SEARCH("搜索建议", "🔍"),
    DOMAIN("常用域名", "🌐"),
    POPULAR("热门搜索", "🔥");
}
```

## 算法实现

### 智能匹配算法

1. **精确匹配**: 查询词与建议开头完全匹配
2. **包含匹配**: 查询词包含在建议中
3. **URL匹配**: 查询词匹配URL部分
4. **关联匹配**: 基于语义关联（如"porn"→"pornhub"）

```java
private boolean isAssociativeMatch(String query, String title, String url) {
    String[] pornKeywords = {"porn", "sex", "adult", "video", "xv", "xvideos"};
    String[] pornSites = {"pornhub", "xvideos", "xhamster", "redtube"};

    for (String keyword : pornKeywords) {
        if (query.contains(keyword) || keyword.contains(query)) {
            // 检查标题或URL是否包含相关站点
            return matchesAnySite(title, url, pornSites);
        }
    }
    return false;
}
```

### 评分算法

```java
private long calculateHistoryScore(HistoryManager.HistoryItem item) {
    long baseScore = 100;
    // 时间衰减：越近访问分数越高
    long timeBonus = Math.max(0, (System.currentTimeMillis() - item.timestamp)
        / (24 * 60 * 60 * 1000)); // 天数
    return baseScore + timeBonus;
}
```

### 排序算法

1. **分数优先**: 高分数项优先
2. **时间排序**: 同分数按时间倒序
3. **类型分组**: UI层面按类型分组显示

## 性能优化

### 1. 防抖机制
```java
private static final long DEBOUNCE_DELAY_MS = 300;
private String mLastQuery = "";
private long mLastRequestTime = 0;

// 避免频繁请求
if (query.equals(mLastQuery) &&
    (currentTime - mLastRequestTime) < DEBOUNCE_DELAY_MS) {
    return;
}
```

### 2. LRU缓存
```java
private final LruCache<String, List<SuggestionItem>> mSuggestionCache =
    new LruCache<>(CACHE_SIZE);
private final LruCache<String, CacheEntry> mNetworkCache =
    new LruCache<>(NETWORK_CACHE_SIZE);
```

### 3. 异步处理
```java
private final ExecutorService mExecutor = Executors.newCachedThreadPool();
private final Handler mMainHandler = new Handler(Looper.getMainLooper());

// 后台处理，前台更新UI
mExecutor.submit(() -> {
    List<SuggestionItem> suggestions = getSuggestionsInternal(query);
    mMainHandler.post(() -> callback.onSuggestionsReady(suggestions));
});
```

### 4. 对象池
```java
public static class ObjectPool<T> {
    private final Queue<T> pool = new LinkedList<>();
    private final ObjectFactory<T> factory;

    public T acquire() {
        T obj = pool.poll();
        return obj != null ? obj : factory.create();
    }

    public void release(T obj) {
        if (obj != null) {
            factory.reset(obj);
            pool.offer(obj);
        }
    }
}
```

## 网络请求处理

### 请求构建
```java
private List<String> fetchSuggestions(String query) throws IOException {
    String suggestUrl = String.format(
        mCurrentEngine.getSuggestUrl(),
        URLEncoder.encode(query, "UTF-8")
    );

    HttpURLConnection connection = null;
    BufferedReader reader = null;

    try {
        URL url = new URL(suggestUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", USER_AGENT);

        // ... 处理响应
    } finally {
        // 清理资源
    }
}
```

### 响应解析

**Google 格式**:
```javascript
window.google.ac.h(["query", ["suggestion1", "suggestion2", ...]])
```

**百度格式**:
```javascript
s: ["suggestion1", "suggestion2", ...]
```

**解析逻辑**:
```java
private List<String> parseGoogleResponse(String response) {
    List<String> suggestions = new ArrayList<>();
    try {
        int start = response.indexOf("[\"");
        int end = response.lastIndexOf("\"]");
        if (start >= 0 && end > start) {
            String jsonArray = response.substring(start, end + 2);
            // 简单解析
            String[] parts = jsonArray.replace("[\"", "").replace("\"]", "")
                .split("\",\"");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    suggestions.add(part.trim());
                }
            }
        }
    } catch (Exception e) {
        // 忽略解析错误
    }
    return suggestions;
}
```

## UI交互设计

### 键盘导航
```java
private boolean handleKeyEvent(int keyCode, KeyEvent event) {
    switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_DOWN:
            return handleArrowKey(true);
        case KeyEvent.KEYCODE_DPAD_UP:
            return handleArrowKey(false);
        case KeyEvent.KEYCODE_TAB:
            return handleTabKey();
        case KeyEvent.KEYCODE_ENTER:
            return handleEnterKey();
        case KeyEvent.KEYCODE_ESCAPE:
            return handleEscapeKey();
    }
    return false;
}
```

### 选择状态管理
```java
private void updateSelectionHighlight() {
    mSuggestionAdapter.setSelectedPosition(
        findActualPositionForSuggestionIndex(mSelectedSuggestionIndex)
    );
    scrollToSelectedItem();
}
```

### 滚动优化
```java
private void scrollToSelectedItem() {
    int actualPosition = findActualPositionForSuggestionIndex(mSelectedSuggestionIndex);
    if (actualPosition >= 0) {
        mSuggestionsRecyclerView.smoothScrollToPosition(actualPosition);
    }
}
```

## 测试和调试

### 单元测试
```java
@Test
public void testSmartMatch() {
    RealtimeSuggestionManager manager = RealtimeSuggestionManager.getInstance();

    // 测试精确匹配
    assertTrue(manager.isSmartMatch("git", "GitHub", "github.com"));

    // 测试包含匹配
    assertTrue(manager.isSmartMatch("hub", "GitHub", "github.com"));

    // 测试URL匹配
    assertTrue(manager.isSmartMatch("github", "GitHub", "github.com"));
}
```

### 性能测试
```java
@UiThreadTest
public void testSuggestionPerformance() {
    SmartAddressBarWidget addressBar = new SmartAddressBarWidget(context);

    long startTime = System.currentTimeMillis();
    addressBar.setAddressText("test query");
    // 等待建议加载...

    long duration = System.currentTimeMillis() - startTime;
    assertTrue("建议加载时间应小于500ms", duration < 500);
}
```

### 内存泄漏检测
```java
@Test
public void testNoMemoryLeaks() {
    // 使用LeakCanary或其他内存检测工具
    SmartAddressBarWidget addressBar = new SmartAddressBarWidget(context);
    // 模拟大量操作...

    // 断言没有内存泄漏
}
```

## 错误处理

### 网络错误
```java
try {
    List<String> suggestions = fetchSuggestions(query);
    return suggestions;
} catch (IOException e) {
    Log.w(TAG, "网络请求失败", e);
    return new ArrayList<>(); // 返回空列表，不影响其他建议
}
```

### 解析错误
```java
private List<String> parseResponse(String response, String query) {
    try {
        switch (mCurrentEngine) {
            case GOOGLE:
                return parseGoogleResponse(response);
            // ... 其他引擎
        }
    } catch (Exception e) {
        Log.w(TAG, "响应解析失败", e);
    }
    return new ArrayList<>();
}
```

### UI更新错误
```java
mMainHandler.post(() -> {
    try {
        callback.onSuggestionsReady(suggestions);
    } catch (Exception e) {
        Log.e(TAG, "UI更新失败", e);
        callback.onError("界面更新失败");
    }
});
```

## 部署和发布

### Gradle 配置
```gradle
dependencies {
    implementation 'androidx.recyclerview:recyclerview:1.2.1'
    implementation 'androidx.appcompat:appcompat:1.4.1'
    // ... 其他依赖
}
```

### 权限配置
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### ProGuard 规则
```proguard
-keep class com.hippo.ehviewer.ui.browser.** { *; }
-dontwarn com.hippo.ehviewer.ui.browser.**
```

## 监控和分析

### 性能监控
```java
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";

    public static void logSuggestionRequest(String query, long duration) {
        Log.d(TAG, String.format("建议请求: query=%s, duration=%dms",
            query, duration));
    }

    public static void logCacheHit(String key) {
        Log.d(TAG, "缓存命中: " + key);
    }
}
```

### 使用统计
```java
public class UsageAnalytics {
    public static void trackSuggestionClick(SuggestionItem item) {
        // 发送统计数据到分析服务
        Analytics.trackEvent("suggestion_click", new Bundle()
            .putString("type", item.type.name())
            .putString("query", item.text));
    }

    public static void trackUrlSubmit(String url, String source) {
        Analytics.trackEvent("url_submit", new Bundle()
            .putString("url", url)
            .putString("source", source));
    }
}
```

## 未来优化方向

### 1. 机器学习排序
- 使用TensorFlow Lite进行客户端排序优化
- 基于用户历史行为预测建议优先级

### 2. 离线支持
- 缓存常用建议到本地数据库
- 支持完全离线的建议功能

### 3. 个性化学习
- 分析用户使用模式
- 提供个性化建议定制

### 4. 多语言支持
- 支持不同语言的智能匹配
- 地区化建议内容

### 5. 云端同步
- 跨设备同步历史记录和偏好
- 云端个性化建议训练
