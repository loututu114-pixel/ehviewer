# EhViewer 模块功能库 (Module Library)

## 概述

EhViewer模块功能库是基于EhViewer应用的完整模块化组件库，每个模块都是独立的、可重用的功能单元。所有模块都遵循统一的接口设计和编码规范，便于在其他Android项目中快速集成和使用。

## 模块列表

### 1. 网络请求模块 (Network Module)
**目录**: `network_module/`  
**包名**: `com.hippo.ehviewer.network`

#### 功能特性
- ✅ HTTP请求处理 (GET/POST/PUT/DELETE)
- ✅ Cookie管理
- ✅ 请求缓存
- ✅ 网络状态检测
- ✅ 请求重试机制
- ✅ 并发请求管理

#### 核心类
- `NetworkClient` - 网络客户端核心类
- `NetworkRequest` - 网络请求封装类
- `CookieManager` - Cookie管理器
- `NetworkCacheManager` - 网络缓存管理器

#### 使用场景
- RESTful API调用
- 文件下载/上传
- 图片资源加载
- 用户认证

---

### 2. 数据库模块 (Database Module)
**目录**: `database_module/`  
**包名**: `com.hippo.ehviewer.database`

#### 功能特性
- ✅ SQLite数据库操作
- ✅ 数据实体管理
- ✅ 查询构建器
- ✅ 数据迁移
- ✅ 数据库备份和恢复
- ✅ 事务管理

#### 核心类
- `DatabaseManager` - 数据库管理器
- `DownloadInfoDao` - 下载信息数据访问对象
- `DownloadInfo` - 下载信息实体

#### 支持的数据类型
- 下载记录 (DownloadInfo)
- 历史记录 (HistoryInfo)
- 收藏夹 (FavoriteInfo)
- 黑名单 (BlackList)
- 标签信息 (GalleryTags)
- 快速搜索 (QuickSearch)

---

### 3. 下载管理模块 (Download Module)
**目录**: `download_module/`  
**包名**: `com.hippo.ehviewer.download`

#### 功能特性
- ✅ 多线程文件下载
- ✅ 断点续传
- ✅ 下载队列管理
- ✅ 下载状态监听
- ✅ 网络状态检测
- ✅ 存储空间管理
- ✅ 下载速度限制

#### 核心类
- `DownloadManager` - 下载管理器
- `DownloadTask` - 下载任务
- `DownloadListener` - 下载监听器

#### 下载状态
- STATE_NONE - 未开始
- STATE_WAIT - 等待中
- STATE_DOWNLOAD - 下载中
- STATE_FINISH - 已完成
- STATE_FAILED - 失败

---

### 4. 图片处理模块 (Image Module)
**目录**: `image_module/`  
**包名**: `com.hippo.ehviewer.image`

#### 功能特性
- ✅ 异步图片加载
- ✅ 多级缓存（内存+磁盘）
- ✅ 图片预加载
- ✅ 加载进度监听
- ✅ 图片缩放和裁剪
- ✅ 图片旋转
- ✅ 图片翻转
- ✅ 支持手势缩放
- ✅ 格式转换
- ✅ 图片压缩
- ✅ 图片滤镜
- ✅ 图片水印

#### 核心类
- `ImageLoader` - 图片加载器
- `ImageProcessor` - 图片处理器
- `ImageCache` - 图片缓存

---

### 5. UI组件模块 (UI Module)
**目录**: `ui_module/`  
**包名**: `com.hippo.ehviewer.ui`

#### 功能特性
- ✅ 自定义ImageView
- ✅ 进度条组件
- ✅ 按钮组件
- ✅ 输入框组件
- ✅ 画廊列表
- ✅ 下载列表
- ✅ 历史记录列表
- ✅ 收藏夹列表
- ✅ 确认对话框
- ✅ 进度对话框
- ✅ 选择对话框
- ✅ 自定义对话框
- ✅ 底部导航栏
- ✅ 标签页导航
- ✅ 抽屉导航
- ✅ 面包屑导航

#### 核心类
- `GalleryListView` - 画廊列表视图
- `ProgressDialog` - 进度对话框
- `EhButton` - 自定义按钮
- `NavigationBar` - 导航栏

---

### 6. 设置管理模块 (Settings Module)
**目录**: `settings_module/`  
**包名**: `com.hippo.ehviewer.settings`

#### 功能特性
- ✅ SharedPreferences存储
- ✅ 数据库存储
- ✅ 文件存储
- ✅ 云端同步
- ✅ 布尔值设置
- ✅ 整数设置
- ✅ 字符串设置
- ✅ 列表选择设置
- ✅ 设置页面生成
- ✅ 设置项分组
- ✅ 设置搜索
- ✅ 设置导入导出
- ✅ 设置变化监听
- ✅ 实时设置同步
- ✅ 设置验证

#### 核心类
- `SettingsManager` - 设置管理器
- `SettingChangeListener` - 设置变化监听器
- `SettingsActivity` - 设置页面

---

### 7. 浏览器模块 (Browser Module)
**目录**: `browser_module/`  
**包名**: `com.hippo.ehviewer.browser`

#### 功能特性
- ✅ WebView集成
- ✅ 网页加载和渲染
- ✅ 前进后退导航
- ✅ 页面刷新
- ✅ JavaScript执行
- ✅ JS桥接
- ✅ 脚本注入
- ✅ 控制台日志
- ✅ Cookie存储
- ✅ Cookie同步
- ✅ Cookie清理
- ✅ 文件下载
- ✅ 下载进度
- ✅ 下载历史

#### 核心类
- `EhBrowser` - EhViewer浏览器
- `WebViewClient` - 网页客户端
- `CookieManager` - Cookie管理器

---

### 8. 缓存管理模块 (Cache Module)
**目录**: `cache_module/`  
**包名**: `com.hippo.ehviewer.cache`

#### 功能特性
- ✅ LruCache实现
- ✅ 缓存大小限制
- ✅ 自动清理机制
- ✅ 文件系统缓存
- ✅ 缓存过期时间
- ✅ 存储空间管理
- ✅ HTTP缓存
- ✅ 缓存策略配置
- ✅ 离线缓存支持
- ✅ 缓存命中率统计
- ✅ 缓存大小监控
- ✅ 缓存清理报告

#### 核心类
- `CacheManager` - 缓存管理器
- `MemoryCache` - 内存缓存
- `DiskCache` - 磁盘缓存
- `CacheStats` - 缓存统计

---

### 9. 工具类模块 (Utils Module)
**目录**: `utils_module/`  
**包名**: `com.hippo.ehviewer.util`

#### 功能特性
- ✅ 文件读写操作
- ✅ 文件压缩解压
- ✅ 文件夹管理
- ✅ 文件类型检测
- ✅ 网络状态检测
- ✅ IP地址获取
- ✅ 域名解析
- ✅ 网络类型判断
- ✅ 字符串编码转换
- ✅ 文本格式化
- ✅ 正则表达式匹配
- ✅ 字符串验证
- ✅ 日期格式化
- ✅ 时间戳转换
- ✅ 时区处理
- ✅ 日期计算

#### 核心类
- `FileUtils` - 文件操作工具
- `NetworkUtils` - 网络工具
- `StringUtils` - 字符串处理工具
- `DateUtils` - 日期时间工具

---

### 10. UI小部件模块 (Widget Module)
**目录**: `widget_module/`  
**包名**: `com.hippo.ehviewer.widget`

#### 功能特性
- ✅ 下载进度小部件
- ✅ 收藏夹快捷小部件
- ✅ 搜索快捷小部件
- ✅ 下载完成通知
- ✅ 更新提醒通知
- ✅ 进度通知
- ✅ 应用快捷方式
- ✅ 浮动操作按钮
- ✅ 快捷菜单
- ✅ 画廊信息卡片
- ✅ 下载状态卡片
- ✅ 设置选项卡片

#### 核心类
- `DownloadProgressWidget` - 下载进度小部件
- `FavoriteShortcutWidget` - 收藏夹快捷小部件
- `NotificationManager` - 通知管理器
- `GalleryInfoCard` - 画廊信息卡片

---

### 11. 统计分析模块 (Analytics Module)
**目录**: `analytics_module/`  
**包名**: `com.hippo.ehviewer.analytics`

#### 功能特性
- ✅ 用户行为分析
- ✅ 事件跟踪统计
- ✅ 性能监控
- ✅ 会话管理
- ✅ 自定义分析

#### 核心类
- `AnalyticsManager` - 统计分析管理器
- `AnalyticsProvider` - 分析服务提供者接口

#### 使用场景
- 用户行为分析
- 应用性能监控
- 事件统计跟踪
- 数据驱动决策

---

### 12. 爬虫模块 (Spider Module)
**目录**: `spider_module/`  
**包名**: `com.hippo.ehviewer.spider`

#### 功能特性
- ✅ 网络爬虫功能
- ✅ 多线程抓取
- ✅ 数据提取解析
- ✅ 任务队列管理
- ✅ 反爬虫处理

#### 核心类
- `SpiderQueen` - 爬虫核心管理器
- `SpiderInfo` - 爬虫任务信息
- `SpiderDen` - 爬虫数据存储

#### 使用场景
- 网页内容抓取
- 数据收集整理
- 内容监控分析
- 自动化测试

## 快速集成指南

### 1. 添加依赖

在你的`build.gradle`文件中添加所需的模块依赖：

```gradle
dependencies {
    implementation project(':network_module')
    implementation project(':database_module')
    implementation project(':download_module')
    // 根据需要添加其他模块
}
```

### 2. 初始化模块

在Application类中初始化所需模块：

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化网络模块
        NetworkClient.init(this);

        // 初始化数据库模块
        DatabaseManager.init(this);

        // 初始化下载模块
        DownloadManager.init(this);
    }
}
```

### 3. 使用模块

```java
// 网络请求
NetworkClient client = NetworkClient.getInstance();
client.get("https://api.example.com/data", callback);

// 数据库操作
DatabaseManager db = DatabaseManager.getInstance();
List<DownloadInfo> downloads = db.getAllDownloads();

// 下载管理
DownloadManager downloadManager = DownloadManager.getInstance();
downloadManager.addDownload(url, localPath);
```

## 模块依赖关系

```
UI Module          → Cache Module
    ↓                     ↓
Browser Module  → Network Module
    ↓                     ↓
Download Module → Database Module
    ↓                     ↓
Widget Module   → Utils Module
```

## 版本信息

- **当前版本**: v1.0.0
- **兼容性**: Android API 21+
- **许可证**: Apache License 2.0
- **维护状态**: 活跃维护

## 贡献指南

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 问题反馈

如果您在使用过程中遇到问题，请：

1. 查看各模块的README文档
2. 检查示例代码
3. 在GitHub Issues中提交问题

## 更新日志

### v1.0.0 (2024-01-XX)
- ✅ 初始版本发布
- ✅ 包含12个核心模块
- ✅ 提供完整的API文档
- ✅ 包含使用示例和演示代码

---

## 📊 模块完成状态总览

### ✅ 已完成的完整模块 (16个)

| 模块名称 | 状态 | 功能描述 | 核心特性 |
|---------|------|---------|----------|
| 🔗 **网络请求模块** | ✅ 完整源码+文档 | HTTP请求、Cookie管理 | 异步请求、缓存、重试机制 |
| 💾 **数据库模块** | ✅ 完整源码+文档 | 数据持久化、查询管理 | SQLite操作、事务管理 |
| 📥 **下载管理模块** | ✅ 完整源码+文档 | 文件下载、多线程处理 | 断点续传、队列管理 |
| 🖼️ **图片处理模块** | ✅ 完整源码+文档 | 图片加载、处理、缓存 | 多级缓存、格式转换 |
| 🎨 **UI组件模块** | ✅ 完整源码+文档 | 用户界面组件 | Material Design、自定义View |
| ⚙️ **设置管理模块** | ✅ 完整源码+文档 | 应用设置、配置管理 | 多存储方式、实时同步 |
| 🌐 **浏览器模块** | ✅ 完整源码+文档 | WebView、网页浏览 | JS支持、Cookie管理 |
| 🗄️ **缓存管理模块** | ✅ 完整源码+文档 | 多级缓存系统 | LRU算法、自动清理 |
| 🔧 **工具类模块** | ✅ 完整源码+文档 | 通用工具函数 | 文件、网络、字符串处理 |
| 📱 **UI小部件模块** | ✅ 完整源码+文档 | Android小部件 | 桌面小部件、通知组件 |
| 📊 **统计分析模块** | ✅ 完整源码+文档 | 用户行为分析 | 事件跟踪、性能监控 |
| 🕷️ **爬虫模块** | ✅ 完整源码+文档 | 网络爬虫 | 网页抓取、数据提取 |
| 📱 **服务模块** | ✅ 完整源码+文档 | 应用服务管理 | 多策略保活、定时任务、资源监控 |
| 🖼️ **图片查看器模块** | ✅ 完整源码+文档 | 增强图片查看器 | 手势缩放、多种图片源、内存优化 |
| 🌐 **现代浏览器模块** | ✅ 完整源码+文档 | 完整浏览器体验 | 智能地址栏、标签页管理、隐私保护、书签历史 |
| 💥 **崩溃处理模块** | ✅ 完整源码+文档 | 崩溃检测和处理 | 日志记录、异常上报 |

### 🟡 基础目录模块 (7个)

| 模块名称 | 状态 | 计划功能 | 优先级 |
|---------|------|---------|--------|
| 📱 **事件模块** | 🟡 基础目录 | 事件总线、消息传递 | 中等 |
| 🖼️ **图库模块** | 🟡 基础目录 | 图片库管理、相册功能 | 中等 |
| 📄 **解析器模块** | 🟡 基础目录 | 数据解析、格式转换 | 中等 |
| 🔗 **快捷方式模块** | 🟡 基础目录 | 应用快捷方式管理 | 低等 |
| 🔄 **同步模块** | 🟡 基础目录 | 数据同步、云备份 | 中等 |
| 📦 **更新模块** | 🟡 基础目录 | 应用更新、版本管理 | 中等 |

## 🔧 Pro模块标准规范

### 📋 规范概述

为了确保EhViewer模块库的高质量产出和标准化管理，我们制定了完整的**Pro模块标准规范**：

### 🎯 规范核心内容

#### 1. **模块生成规范**
- **功能内聚性分析**：识别和划分功能模块的科学方法
- **标准化架构设计**：统一的模块结构和设计模式
- **接口设计规范**：标准化的回调接口和配置接口
- **异常处理体系**：统一的异常分类和处理机制
- **资源管理规范**：内存管理和资源释放的最佳实践

#### 2. **文档生成规范**
- **README.md标准模板**：包含功能特性、使用方法、配置选项等
- **API文档规范**：JavaDoc注释的标准格式和要求
- **示例代码规范**：完整的使用示例和最佳实践
- **变更日志管理**：规范的版本更新记录格式

#### 3. **模块引入规范**
- **项目结构规划**：推荐的项目组织结构
- **依赖管理配置**：Gradle配置和版本管理
- **应用初始化流程**：标准化的模块初始化顺序
- **生命周期管理**：Activity和ViewModel的生命周期处理

#### 4. **质量保证规范**
- **代码质量检查**：KtLint、Detekt等工具配置
- **测试覆盖率要求**：单元测试、集成测试、UI测试标准
- **性能基准测试**：性能测试的标准和工具
- **静态代码分析**：代码质量自动化检查

#### 5. **版本管理规范**
- **语义化版本**：标准的版本号格式和规则
- **发布流程**：完整的版本发布和部署流程
- **变更日志**：规范的更新记录格式
- **依赖管理**：统一的依赖版本管理

### 🚀 规范优势

#### **标准化生产**
```java
// ✅ 统一的模块结构
module-name/
├── src/main/java/com/hippo/ehviewer/modulename/
│   ├── ModuleNameManager.java          // 核心管理类
│   ├── interfaces/                     // 标准接口
│   ├── impl/                          // 实现类
│   ├── utils/                         // 工具类
│   ├── exception/                     // 异常处理
│   └── constants/                     // 常量定义
├── build.gradle                       // 标准配置
└── README.md                         // 完整文档
```

#### **高质量保证**
```kotlin
// ✅ 完整的测试覆盖
class ModuleNameManagerTest {
    @Test
    fun `perform action should succeed with valid input`() {
        // 标准测试用例
    }

    @Test
    fun `perform action should handle errors gracefully`() {
        // 错误处理测试
    }
}
```

#### **快速引入**
```gradle
// ✅ 标准化的依赖配置
dependencies {
    implementation project(':libraries:network')
    implementation project(':libraries:database')
    implementation project(':libraries:settings')
    implementation project(':libraries:utils')
}
```

### 📖 详细文档

📋 **[Pro模块标准规范](pro_module_standard.md)** - 完整的模块生成、引入、质量保证和版本管理规范

### 🎯 适用场景

#### **适合使用规范的项目**：
- ✅ **大型Android项目** - 需要模块化的复杂应用
- ✅ **团队开发项目** - 需要统一规范的多人员协作
- ✅ **开源项目** - 需要高质量和标准化产出的项目
- ✅ **商业产品** - 需要稳定可靠和可维护的代码

#### **规范价值**：
- **提高开发效率**：标准化的流程和模板
- **保证代码质量**：统一的规范和检查标准
- **便于维护**：清晰的结构和完整的文档
- **支持扩展**：模块化的设计便于功能扩展

---

**注意**: 所有模块都经过充分测试，可以直接在生产环境中使用。如有特殊需求，可以基于这些模块进行定制开发。

**🔧 提示**: 建议所有基于EhViewer模块库的项目都遵循 **[Pro模块标准规范](pro_module_standard.md)** 来确保高质量的产出。
