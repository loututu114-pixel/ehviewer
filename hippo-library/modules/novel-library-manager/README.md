# 📚 小说书库管理器模块 (Novel Library Manager Module)

## 🎯 概述

Android Library小说书库管理器提供完整的本地小说收藏和阅读管理功能，支持小说内容识别、书库组织、阅读进度同步等高级特性。该模块为用户提供类似UC浏览器书库的专业小说阅读体验。

## ✨ 主要特性

- ✅ **智能识别**: 自动识别网页中的小说内容
- ✅ **本地收藏**: 将小说保存到本地书库
- ✅ **分类管理**: 自动区分普通小说和色情小说
- ✅ **阅读进度**: 保存和恢复阅读进度
- ✅ **书签功能**: 为小说添加书签标记
- ✅ **搜索功能**: 支持小说标题和内容的搜索
- ✅ **导入导出**: 支持书库的备份和恢复
- ✅ **阅读统计**: 阅读时长、进度等统计信息

## 🚀 快速开始

### 初始化小说书库管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化小说书库管理器
        NovelLibraryManager.initialize(this);
    }
}
```

### 添加小说到书库

```java
// 创建小说信息
NovelInfo novel = new NovelInfo();
novel.setTitle("小说标题");
novel.setAuthor("作者");
novel.setUrl("https://example.com/novel/123");
novel.setCategory(NovelCategory.ADULT); // 成人小说
novel.setContent("小说正文内容...");

// 添加到书库
boolean success = NovelLibraryManager.getInstance()
    .addNovel(novel);

// 检查添加结果
if (success) {
    Toast.makeText(context, "小说已添加到书库", Toast.LENGTH_SHORT).show();
}
```

### 阅读小说

```java
// 获取小说阅读器
NovelReader reader = NovelLibraryManager.getInstance()
    .getReader(novelId);

// 设置阅读监听器
reader.setReaderListener(new NovelReaderListener() {
    @Override
    public void onProgressChanged(int progress) {
        // 更新进度显示
        updateProgressBar(progress);
    }

    @Override
    public void onPageChanged(int currentPage, int totalPages) {
        // 更新页面显示
        updatePageIndicator(currentPage, totalPages);
    }
});

// 开始阅读
reader.startReading();
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `NovelLibraryManager` | 小说书库管理器主类 |
| `NovelInfo` | 小说信息数据类 |
| `NovelReader` | 小说阅读器 |
| `NovelCategory` | 小说分类枚举 |

### 主要方法

#### NovelLibraryManager

```java
// 初始化小说书库
void initialize(Context context)

// 获取单例实例
NovelLibraryManager getInstance()

// 添加小说
boolean addNovel(NovelInfo novel)

// 删除小说
boolean deleteNovel(long novelId)

// 更新小说信息
boolean updateNovel(NovelInfo novel)

// 获取所有小说
List<NovelInfo> getAllNovels()

// 根据分类获取小说
List<NovelInfo> getNovelsByCategory(NovelCategory category)

// 搜索小说
List<NovelInfo> searchNovels(String keyword)

// 获取小说阅读器
NovelReader getReader(long novelId)

// 导出书库
String exportLibrary()

// 导入书库
boolean importLibrary(String data)

// 获取书库统计信息
LibraryStats getStats()
```

#### NovelReader

```java
// 开始阅读
void startReading()

// 暂停阅读
void pauseReading()

// 停止阅读
void stopReading()

// 跳转到指定位置
void seekTo(int position)

// 获取当前进度
int getCurrentProgress()

// 获取总长度
int getTotalLength()

// 设置阅读监听器
void setReaderListener(NovelReaderListener listener)

// 获取阅读统计
ReadingStats getReadingStats()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableAutoSave` | `boolean` | `true` | 是否自动保存阅读进度 |
| `maxCacheSize` | `long` | `100MB` | 缓存最大大小 |
| `enableNightMode` | `boolean` | `false` | 是否启用夜间模式 |
| `fontSize` | `int` | `16` | 默认字体大小 |
| `lineSpacing` | `float` | `1.2f` | 行间距 |
| `pageSize` | `int` | `2000` | 每页字符数 |

## 📦 依赖项

```gradle
dependencies {
    // 小说书库管理器模块
    implementation 'com.hippo.ehviewer:novel-library-manager:1.0.0'

    // 相关模块
    implementation 'com.hippo.ehviewer:novel-content-detector:1.0.0'
    implementation 'com.hippo.ehviewer:novel-reader:1.0.0'
}
```

## ⚠️ 注意事项

### 存储管理
- 小说内容较大，注意存储空间管理
- 定期清理过期缓存，避免磁盘空间不足
- 支持断点续传，防止数据丢失

### 性能优化
- 大型小说采用分页加载，避免内存溢出
- 支持后台预加载，提升阅读流畅度
- 智能缓存策略，平衡内存和性能

### 隐私保护
- 本地存储，不上传用户数据
- 支持数据导出和删除
- 符合隐私保护相关法规

## 🧪 测试

### 书库管理测试
```java
@Test
public void testNovelLibraryManager_addAndRetrieveNovel_shouldWorkCorrectly() {
    // Given
    NovelLibraryManager manager = NovelLibraryManager.getInstance();
    NovelInfo novel = createTestNovel();

    // When
    boolean added = manager.addNovel(novel);
    List<NovelInfo> novels = manager.getAllNovels();

    // Then
    assertTrue(added);
    assertFalse(novels.isEmpty());
    assertEquals(novel.getTitle(), novels.get(0).getTitle());
}
```

### 阅读器测试
```java
@Test
public void testNovelReader_progressTracking_shouldWorkCorrectly() {
    // Given
    NovelReader reader = NovelLibraryManager.getInstance().getReader(novelId);

    // When
    reader.startReading();
    reader.seekTo(1000); // 跳转到1000字符位置

    // Then
    assertEquals(1000, reader.getCurrentProgress());
}
```

### 搜索功能测试
```java
@Test
public void testNovelLibraryManager_search_shouldReturnCorrectResults() {
    // Given
    NovelLibraryManager manager = NovelLibraryManager.getInstance();
    String keyword = "测试小说";

    // When
    List<NovelInfo> results = manager.searchNovels(keyword);

    // Then
    assertNotNull(results);
    for (NovelInfo novel : results) {
        assertTrue(novel.getTitle().contains(keyword) ||
                  novel.getAuthor().contains(keyword));
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingNovelLibrary`)
3. 提交更改 (`git commit -m 'Add some AmazingNovelLibrary'`)
4. 推送到分支 (`git push origin feature/AmazingNovelLibrary`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/novel-library-manager/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
