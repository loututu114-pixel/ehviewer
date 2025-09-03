# Chrome 风格地址栏功能

## 概述

本项目实现了一个完整的 Chrome 风格智能地址栏系统，提供实时搜索建议、多源数据整合、智能排序和键盘导航功能。

## 核心功能

### 🎯 实时建议系统
- **智能匹配**: 支持精确匹配、包含匹配和关联匹配
- **多源整合**: 历史记录、书签、网络搜索、本地数据
- **防抖优化**: 300ms 防抖，防止过度请求
- **LRU缓存**: 50项缓存，提升响应速度

### ⌨️ 键盘导航
- **方向键导航**: ↑↓ 键选择建议项
- **Tab 补全**: Tab 键补全当前选择
- **Enter 确认**: Enter 键提交 URL
- **ESC 取消**: ESC 键取消选择

### 📊 智能排序
- **多维度评分**: 基于访问频率、时间衰减、类型权重
- **分组显示**: 按类型分组展示（历史、书签、搜索等）
- **实时学习**: 记录用户行为，优化建议排序

### 🌐 网络建议
- **多搜索引擎**: 支持 Google、Bing、百度、DuckDuckGo、搜狗
- **自动适配**: 根据地区自动选择搜索引擎
- **缓存机制**: 5分钟缓存，减少网络请求

## 架构设计

### 核心组件

```
RealtimeSuggestionManager (核心管理器)
├── SmartAddressBarWidget (UI组件)
├── EnhancedSuggestionAdapter (列表适配器)
├── NetworkSuggestionProvider (网络建议)
├── HistoryManager (历史记录)
├── BookmarkManager (书签管理)
└── DomainSuggestionManager (域名建议)
```

### 数据流

1. **用户输入** → SmartAddressBarWidget
2. **文本变化** → RealtimeSuggestionManager.requestSuggestions()
3. **多源查询** → 历史 + 书签 + 网络 + 本地
4. **智能排序** → 按分数和时间排序
5. **分组显示** → EnhancedSuggestionAdapter
6. **用户选择** → 提交URL或搜索

## 技术实现

### 防抖机制
```java
private static final long DEBOUNCE_DELAY_MS = 300;
private long mLastRequestTime = 0;

if (query.equals(mLastQuery) &&
    (currentTime - mLastRequestTime) < DEBOUNCE_DELAY_MS) {
    return; // 忽略重复请求
}
```

### 智能匹配算法
```java
private boolean isSmartMatch(String query, String title, String url) {
    // 精确匹配标题开头
    if (title.toLowerCase().startsWith(query.toLowerCase())) {
        return true;
    }

    // 关联匹配（例如 "porn" -> "pornhub"）
    return isAssociativeMatch(query, title, url);
}
```

### 多线程处理
```java
private final ExecutorService mExecutor = Executors.newCachedThreadPool();

// 异步处理，不阻塞UI线程
mExecutor.submit(() -> {
    List<SuggestionItem> suggestions = getSuggestionsInternal(query);
    mMainHandler.post(() -> callback.onSuggestionsReady(suggestions));
});
```

## 使用方法

### 基本使用

```java
// 1. 创建地址栏组件
SmartAddressBarWidget addressBar = new SmartAddressBarWidget(context);

// 2. 设置监听器
addressBar.setOnAddressBarListener(new OnAddressBarListener() {
    @Override
    public void onUrlSubmit(String url) {
        // 处理URL提交
        loadUrl(url);
    }

    @Override
    public void onSuggestionClick(SuggestionItem item) {
        // 处理建议点击
        handleSuggestion(item);
    }
});

// 3. 设置当前URL
addressBar.setCurrentUrl("https://main.eh-viewer.com/");
```

### 高级配置

```java
// 配置网络建议提供者
NetworkSuggestionProvider networkProvider = NetworkSuggestionProvider.getInstance();
networkProvider.setCurrentEngine(NetworkSuggestionProvider.SearchEngine.BAIDU);

// 获取建议管理器实例
RealtimeSuggestionManager suggestionManager = RealtimeSuggestionManager.getInstance();
suggestionManager.recordUrlVisit(url, title);
```

## 演示和测试

### 演示活动
运行 `ChromeStyleAddressBarDemoActivity` 来体验完整功能：

```java
// AndroidManifest.xml 中注册活动
<activity android:name=".ui.browser.ChromeStyleAddressBarDemoActivity"
    android:label="Chrome风格地址栏演示" />
```

### 测试建议输入
- **xvideos** 或 **xv**: 测试关联匹配
- **github**: 测试历史记录匹配
- **news**: 测试热门搜索
- **https://**: 测试URL补全

## 性能优化

### 内存管理
- **对象池**: 复用缓存对象，减少GC压力
- **LRU缓存**: 智能淘汰最少使用的缓存项
- **线程池**: 复用线程，避免频繁创建销毁

### 网络优化
- **请求合并**: 将多个相似请求合并处理
- **缓存策略**: 多层缓存（内存 + 磁盘）
- **超时控制**: 5秒连接超时，5秒读取超时

### UI优化
- **异步加载**: 所有耗时操作都在后台线程
- **增量更新**: 只更新变化的部分
- **虚拟滚动**: 大列表使用RecyclerView虚拟化

## 扩展性

### 添加新的建议源
```java
public class CustomSuggestionProvider implements SuggestionProvider {
    @Override
    public List<SuggestionItem> getSuggestions(String query) {
        // 实现自定义建议逻辑
        return customSuggestions;
    }
}

// 注册到管理器
mSuggestionManager.addProvider(customProvider);
```

### 自定义排序算法
```java
public class CustomSorter implements SuggestionSorter {
    @Override
    public void sort(List<SuggestionItem> suggestions) {
        // 实现自定义排序逻辑
        Collections.sort(suggestions, customComparator);
    }
}
```

## 已知限制

1. **网络依赖**: 网络建议需要网络连接
2. **地区限制**: 某些搜索引擎在特定地区可能被屏蔽
3. **内存占用**: 大量历史记录会增加内存使用
4. **精确度**: 智能匹配算法可能产生误匹配

## 故障排除

### 常见问题

**Q: 建议列表不显示？**
A: 检查网络连接和权限设置

**Q: 键盘导航不工作？**
A: 确保地址栏获得焦点，且键盘导航已启用

**Q: 建议排序不准确？**
A: 清除缓存，让系统重新学习用户偏好

**Q: 网络建议加载慢？**
A: 检查网络连接，或切换到其他搜索引擎

### 调试模式
启用调试模式查看详细日志：
```java
RealtimeSuggestionManager.enableDebugLogging(true);
```

## 更新日志

### v1.0.1 (最新)
- ✅ 修改默认首页为 https://main.eh-viewer.com/
- ✅ 支持中国地区首页配置
- ✅ 完善智能匹配算法
- ✅ 优化网络建议缓存
- ✅ 改进UI分组显示

### v1.0.0
- ✅ 基础实时建议功能
- ✅ 多源数据整合
- ✅ 键盘导航支持
- ✅ 防抖和缓存优化

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情
