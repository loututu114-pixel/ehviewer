# 🔍 智能URL处理器模块 (Smart URL Processor Module)

## 🎯 概述

Android Library智能URL处理器提供强大的URL智能识别和处理功能，能够自动判断用户输入的内容类型，并提供最合适的处理方式。该模块集成了搜索引擎选择、协议处理、文件路径识别等高级功能。

## ✨ 主要特性

- ✅ **智能识别**: 自动识别URL、搜索查询、特殊协议
- ✅ **搜索引擎适配**: 根据用户地区自动选择最适合的搜索引擎
- ✅ **协议支持**: 支持mailto、tel、sms、geo等多种协议
- ✅ **文件路径处理**: 支持本地文件访问
- ✅ **历史记录**: 保存和学习用户的搜索偏好
- ✅ **别名支持**: 支持常用网站的缩写输入
- ✅ **实时建议**: 输入时提供智能补全建议
- ✅ **错误处理**: 完善的输入验证和错误处理

## 🚀 快速开始

### 初始化智能URL处理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化智能URL处理器
        SmartUrlProcessor.initialize(this);
    }
}
```

### 处理用户输入

```java
// 获取智能URL处理器实例
SmartUrlProcessor processor = SmartUrlProcessor.getInstance();

// 处理用户输入
String userInput = "java tutorial";

// 处理输入并获取结果
ProcessingResult result = processor.processInput(userInput);

// 检查处理结果
if (result.isValidUrl()) {
    // 是有效URL，直接访问
    webView.loadUrl(result.getUrl());
} else if (result.isSearchQuery()) {
    // 是搜索查询，构造搜索URL
    String searchUrl = result.getSearchUrl();
    webView.loadUrl(searchUrl);
} else if (result.isSpecialProtocol()) {
    // 是特殊协议，交由系统处理
    processor.handleSpecialProtocol(result);
}
```

### 高级使用

```java
// 配置处理器参数
SmartUrlProcessorConfig config = new SmartUrlProcessorConfig.Builder()
    .setDefaultSearchEngine(SearchEngine.GOOGLE)
    .enableHistoryLearning(true)
    .setMaxHistorySize(1000)
    .enableAutoComplete(true)
    .setAutoCompleteDelay(300) // 300ms延迟
    .build();

// 应用配置
processor.setConfig(config);

// 添加自定义别名
processor.addAlias("yt", "https://youtube.com");
processor.addAlias("gh", "https://github.com");

// 获取建议列表
List<String> suggestions = processor.getSuggestions("jav");
for (String suggestion : suggestions) {
    Log.d(TAG, "Suggestion: " + suggestion);
}
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `SmartUrlProcessor` | 智能URL处理器主类 |
| `ProcessingResult` | 处理结果数据类 |
| `SmartUrlProcessorConfig` | 处理器配置类 |
| `SearchEngine` | 搜索引擎枚举 |

### 主要方法

#### SmartUrlProcessor

```java
// 初始化处理器
void initialize(Context context)

// 获取单例实例
SmartUrlProcessor getInstance()

// 处理用户输入
ProcessingResult processInput(String input)

// 验证URL格式
boolean isValidUrl(String input)

// 构造搜索URL
String buildSearchUrl(String query, SearchEngine engine)

// 处理特殊协议
boolean handleSpecialProtocol(ProcessingResult result)

// 获取输入建议
List<String> getSuggestions(String partialInput)

// 添加别名
void addAlias(String alias, String url)

// 移除别名
void removeAlias(String alias)

// 获取历史记录
List<String> getHistory()

// 清除历史记录
void clearHistory()

// 设置配置
void setConfig(SmartUrlProcessorConfig config)

// 获取当前配置
SmartUrlProcessorConfig getConfig()
```

#### ProcessingResult

```java
// 获取处理后的URL
String getUrl()

// 获取原始输入
String getOriginalInput()

// 获取处理类型
ProcessingType getType()

// 是否为有效URL
boolean isValidUrl()

// 是否为搜索查询
boolean isSearchQuery()

// 是否为特殊协议
boolean isSpecialProtocol()

// 获取搜索URL
String getSearchUrl()

// 获取协议类型
String getProtocolType()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `defaultSearchEngine` | `SearchEngine` | `AUTO` | 默认搜索引擎 |
| `enableHistoryLearning` | `boolean` | `true` | 是否启用历史学习 |
| `maxHistorySize` | `int` | `1000` | 历史记录最大数量 |
| `enableAutoComplete` | `boolean` | `true` | 是否启用自动补全 |
| `autoCompleteDelay` | `long` | `300` | 自动补全延迟(毫秒) |
| `enableAnalytics` | `boolean` | `true` | 是否启用使用统计 |

## 📦 依赖项

```gradle
dependencies {
    // 智能URL处理器模块
    implementation 'com.hippo.ehviewer:smart-url-processor:1.0.0'

    // 相关模块
    implementation 'com.hippo.ehviewer:url-type-detector:1.0.0'
    implementation 'com.hippo.ehviewer:domain-suggestion-manager:1.0.0'
}
```

## ⚠️ 注意事项

### 输入验证
- 支持的URL协议：http, https, ftp, file
- 支持的特殊协议：mailto, tel, sms, geo
- 自动检测和处理无效输入

### 搜索引擎选择
- 自动检测用户地区（中文用户使用百度，国际用户使用Google）
- 支持自定义搜索引擎配置
- 提供搜索引擎切换功能

### 隐私保护
- 本地存储历史记录，不上传到服务器
- 支持手动清除历史记录
- 符合隐私保护相关法规

## 🧪 测试

### URL处理测试
```java
@Test
public void testSmartUrlProcessor_validUrl_shouldBeRecognized() {
    // Given
    SmartUrlProcessor processor = SmartUrlProcessor.getInstance();
    String validUrl = "https://github.com";

    // When
    ProcessingResult result = processor.processInput(validUrl);

    // Then
    assertTrue(result.isValidUrl());
    assertEquals(validUrl, result.getUrl());
    assertEquals(ProcessingType.VALID_URL, result.getType());
}
```

### 搜索查询测试
```java
@Test
public void testSmartUrlProcessor_searchQuery_shouldBuildSearchUrl() {
    // Given
    SmartUrlProcessor processor = SmartUrlProcessor.getInstance();
    String searchQuery = "java tutorial";

    // When
    ProcessingResult result = processor.processInput(searchQuery);

    // Then
    assertTrue(result.isSearchQuery());
    assertNotNull(result.getSearchUrl());
    assertTrue(result.getSearchUrl().contains(searchQuery));
}
```

### 特殊协议测试
```java
@Test
public void testSmartUrlProcessor_specialProtocol_shouldBeHandled() {
    // Given
    SmartUrlProcessor processor = SmartUrlProcessor.getInstance();
    String emailUrl = "mailto:test@example.com";

    // When
    ProcessingResult result = processor.processInput(emailUrl);

    // Then
    assertTrue(result.isSpecialProtocol());
    assertEquals("mailto", result.getProtocolType());
}
```

### 别名功能测试
```java
@Test
public void testSmartUrlProcessor_alias_shouldBeResolved() {
    // Given
    SmartUrlProcessor processor = SmartUrlProcessor.getInstance();
    processor.addAlias("yt", "https://youtube.com");

    // When
    ProcessingResult result = processor.processInput("yt");

    // Then
    assertTrue(result.isValidUrl());
    assertEquals("https://youtube.com", result.getUrl());
}
```

### 建议功能测试
```java
@Test
public void testSmartUrlProcessor_suggestions_shouldReturnRelevantResults() {
    // Given
    SmartUrlProcessor processor = SmartUrlProcessor.getInstance();
    String partialInput = "goog";

    // When
    List<String> suggestions = processor.getSuggestions(partialInput);

    // Then
    assertNotNull(suggestions);
    assertFalse(suggestions.isEmpty());
    for (String suggestion : suggestions) {
        assertTrue(suggestion.toLowerCase().contains("goog") ||
                  suggestion.toLowerCase().startsWith("goog"));
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingSmartUrlProcessor`)
3. 提交更改 (`git commit -m 'Add some AmazingSmartUrlProcessor'`)
4. 推送到分支 (`git commit -m 'Add some AmazingSmartUrlProcessor'`)
5. 推送到分支 (`git push origin feature/AmazingSmartUrlProcessor`)
6. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/smart-url-processor/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
