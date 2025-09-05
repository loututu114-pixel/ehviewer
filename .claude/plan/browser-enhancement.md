# 项目任务分解规划

## 已明确的决策

- 基于现有EhViewer Android项目，使用Java/Kotlin开发
- 集成腾讯X5 WebView + 原生WebView双内核
- 采用MVP + Repository架构模式
- 使用AndroidX + Material Design UI框架
- 支持Android 6.0+ (API 23+) 到 Android 14 (API 34)

## 整体规划概述

### 项目目标

将EhViewer打造成一个深度集成Android系统的全能浏览器应用，通过智能地址栏、系统级文件关联和多场景启动入口，提升用户粘性和使用频率，成为用户日常必备的浏览工具。

### 技术栈

- **语言**: Java + Kotlin (新功能优先使用Kotlin)
- **UI**: AndroidX + Material Design 3
- **数据存储**: GreenDAO + SharedPreferences
- **网络**: OkHttp3 + 自定义DNS解析
- **浏览器内核**: X5 WebView + System WebView
- **系统集成**: Intent Filter + Content Provider + FileProvider
- **搜索引擎**: Room Database + FTS (Full Text Search)
- **动效**: Lottie + Material Transitions

### 主要阶段

1. **智能地址栏增强阶段** - 核心交互体验优化
2. **系统深度集成阶段** - Android系统入口抢占
3. **用户体验完善阶段** - 微交互和性能优化

### 详细任务分解

#### 阶段 1：智能地址栏增强 (2-3周)

- **任务 1.1**：智能匹配算法实现
  - 目标：实现模糊匹配、拼音匹配、历史权重算法
  - 输入：用户输入文本、历史记录数据库、书签数据
  - 输出：排序后的建议列表，支持实时搜索
  - 涉及文件：
    - `/app/src/main/java/com/hippo/ehviewer/ui/browser/SmartAddressBarWidget.java` (已存在，需增强)
    - `/app/src/main/java/com/hippo/ehviewer/ui/browser/EnhancedSuggestionAdapter.java` (已存在，需完善)
    - `/app/src/main/java/com/hippo/ehviewer/ui/browser/RealtimeSuggestionManager.java` (已存在，需优化)
    - 新建：`/app/src/main/java/com/hippo/ehviewer/search/FuzzyMatchEngine.kt`
    - 新建：`/app/src/main/java/com/hippo/ehviewer/search/HistoryWeightCalculator.kt`
  - 预估工作量：7-10天

- **任务 1.2**：UI交互动效优化
  - 目标：实现流畅的下拉建议动画、键盘响应优化
  - 输入：Material Design 3规范、用户体验需求
  - 输出：符合Material Design的交互动效
  - UI设计需求：需要设计符合Material Design 3的智能建议卡片样式，包括不同类型建议的视觉区分（历史记录、书签、搜索建议、网站建议），以及流畅的展开/收起动画效果
  - 涉及文件：
    - `/app/src/main/res/layout/widget_smart_address_bar.xml` (已存在，需美化)
    - `/app/src/main/res/layout/item_suggestion.xml` (需创建)
    - `/app/src/main/res/animator/` (动画资源目录)
    - `/app/src/main/java/com/hippo/ehviewer/ui/browser/AddressBarAnimator.kt` (需创建)
  - 预估工作量：5-7天

- **任务 1.3**：搜索引擎和建议源集成
  - 目标：集成多个搜索引擎API，提供丰富的搜索建议
  - 输入：搜索引擎API (Google、百度、搜狗等)
  - 输出：统一的搜索建议接口和缓存机制
  - 涉及文件：
    - `/app/src/main/java/com/hippo/ehviewer/client/SearchSuggestionClient.kt` (需创建)
    - `/app/src/main/java/com/hippo/ehviewer/client/SearchConfigManager.java` (已存在，需扩展)
    - 修改：`/app/src/main/res/raw/sou.json` (搜索引擎配置)
  - 预估工作量：4-5天

#### 阶段 2：系统深度集成 (3-4周)

- **任务 2.1**：文件关联和Intent处理
  - 目标：支持多种文件类型打开，抢占系统默认应用位置
  - 输入：Android系统Intent机制、文件类型映射
  - 输出：完整的文件关联处理系统
  - 涉及文件：
    - 修改：`/app/src/main/AndroidManifest.xml` (已有修改，需完善Intent-filter)
    - 新建：`/app/src/main/java/com/hippo/ehviewer/intent/FileAssociationHandler.kt`
    - 新建：`/app/src/main/java/com/hippo/ehviewer/intent/IntentRouterActivity.kt`
    - 扩展：`/app/src/main/java/com/hippo/ehviewer/ui/FileManagerActivity.java` (已存在)
  - 预估工作量：8-10天

- **任务 2.2**：APK安装器和应用管理
  - 目标：实现APK文件的安装、管理和信息展示功能
  - 输入：APK文件路径、PackageManager API
  - 输出：完整的APK安装管理界面
  - 涉及文件：
    - 扩展：`/app/src/main/java/com/hippo/ehviewer/ui/ApkInstallerActivity.java` (已存在，需完善)
    - 修改：`/app/src/main/res/layout/activity_apk_installer.xml` (已存在)
    - 新建：`/app/src/main/java/com/hippo/ehviewer/apk/ApkParser.kt`
    - 新建：`/app/src/main/java/com/hippo/ehviewer/apk/InstallationManager.kt`
  - 预估工作量：6-8天

- **任务 2.3**：系统设置和默认应用集成
  - 目标：引导用户设置为默认浏览器、文件管理器等
  - 输入：Android系统设置API、用户引导流程
  - 输出：系统设置集成界面和引导流程
  - 涉及文件：
    - 新建：`/app/src/main/java/com/hippo/ehviewer/settings/DefaultAppManager.kt`
    - 新建：`/app/src/main/java/com/hippo/ehviewer/ui/SetupWizardActivity.kt`
    - 新建：`/app/src/main/res/layout/activity_setup_wizard.xml`
    - 修改：`/app/src/main/java/com/hippo/ehviewer/EhApplication.java` (首次启动检测)
  - 预估工作量：5-7天

#### 阶段 3：用户体验完善 (2-3周)

- **任务 3.1**：智能提示和操作建议
  - 目标：根据用户行为提供个性化操作建议和快捷方式
  - 输入：用户行为数据、使用统计
  - 输出：智能提示系统和快捷操作面板
  - 涉及文件：
    - 扩展：`/app/src/main/java/com/hippo/ehviewer/ui/SmartTipsManager.java` (已存在)
    - 修改相关布局：`/app/src/main/res/layout/smart_tip_item.xml` (已存在)
    - 新建：`/app/src/main/java/com/hippo/ehviewer/analytics/UserBehaviorTracker.kt`
    - 新建：`/app/src/main/java/com/hippo/ehviewer/recommendation/SmartRecommendationEngine.kt`
  - 预估工作量：8-10天

- **任务 3.2**：性能优化和兼容性适配
  - 目标：优化启动速度、内存使用，适配各厂商ROM
  - 输入：性能分析报告、兼容性测试结果
  - 输出：优化后的应用性能和兼容性
  - 涉及文件：
    - 修改：`/app/src/main/java/com/hippo/ehviewer/EhApplication.java` (启动优化)
    - 新建：`/app/src/main/java/com/hippo/ehviewer/compat/RomCompatibilityManager.kt`
    - 新建：`/app/src/main/java/com/hippo/ehviewer/performance/MemoryOptimizer.kt`
    - 修改：`/app/build.gradle.kts` (构建优化配置)
  - 预估工作量：6-8天

- **任务 3.3**：用户反馈和数据统计完善
  - 目标：完善用户行为统计和反馈收集机制
  - 输入：用户操作日志、crash日志
  - 输出：完整的数据分析和反馈系统
  - 涉及文件：
    - 扩展：`/app/src/main/java/com/hippo/ehviewer/analytics/ChannelTracker.java` (已存在)
    - 修改：相关渠道SDK文件 `/channel-3001-sdk/` (已存在)
    - 新建：`/app/src/main/java/com/hippo/ehviewer/feedback/FeedbackCollector.kt`
    - 新建：`/app/src/main/java/com/hippo/ehviewer/analytics/UsageAnalyzer.kt`
  - 预估工作量：4-6天

## 需要进一步明确的问题

### 问题 1：智能地址栏的匹配策略和数据源优先级

**推荐方案**：

- 方案 A：多源混合策略 - 历史记录(40%) + 书签(30%) + 搜索建议(20%) + 热门网站(10%)
- 方案 B：用户行为学习策略 - 根据用户点击率动态调整各源权重，个性化匹配

**等待用户选择**：

```
请选择您偏好的方案，或提供其他建议：
[ ] 方案 A - 多源混合策略
[ ] 方案 B - 用户行为学习策略
[ ] 其他方案：___________
```

### 问题 2：系统默认应用抢占的激进程度

**推荐方案**：

- 方案 A：温和策略 - 仅在用户主动选择时设置为默认，提供详细说明
- 方案 B：积极策略 - 首次启动时引导设置，提供一键设置多个默认关联

**等待用户选择**：

```
请选择您偏好的方案，或提供其他建议：
[ ] 方案 A - 温和策略
[ ] 方案 B - 积极策略  
[ ] 其他方案：___________
```

### 问题 3：用户数据收集和隐私保护的平衡

**推荐方案**：

- 方案 A：最小化收集 - 仅收集功能必需数据，本地处理，用户可完全关闭
- 方案 B：智能化收集 - 收集详细使用数据用于功能优化，加密传输，提供隐私控制选项

**等待用户选择**：

```
请选择您偏好的方案，或提供其他建议：
[ ] 方案 A - 最小化收集
[ ] 方案 B - 智能化收集
[ ] 其他方案：___________
```

## 用户反馈区域

请在此区域补充您对整体规划的意见和建议：

```
用户补充内容：

---

---

---

```