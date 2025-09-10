# App 主应用模块

> [根目录](../CLAUDE.md) > **app**

---

## 模块职责

App模块是EhViewerh的核心应用模块，包含了所有的UI组件、业务逻辑、资源文件和应用配置。该模块实现了一个功能丰富的Android图片浏览器和文件管理应用。

### 核心功能
- **图片浏览**: 多格式图片显示、缩放、旋转等操作
- **浏览器引擎**: 基于腾讯X5 WebView的完整浏览器功能
- **文件管理**: 文件浏览、复制、移动、删除、搜索功能
- **下载管理**: 支持断点续传的下载系统
- **画廊系统**: 网络画廊浏览和本地收藏管理
- **多媒体播放**: 视频、音频文件播放支持
- **APK管理**: APK文件安装和应用管理

---

## 入口与启动

### 主入口点
- **Application类**: `com.hippo.ehviewer.EhApplication`
- **启动Activity**: `com.hippo.ehviewer.ui.splash.SplashActivity`
- **主Activity**: `com.hippo.ehviewer.ui.MainActivity`

### 启动流程
```
SplashActivity -> MainActivity -> 功能模块
     ↓
WebView核心初始化 -> 用户引导 -> 主界面
```

### 关键启动逻辑
```java
// EhApplication.onCreate()中的核心初始化
1. 系统兼容性设置
2. 异常处理器配置  
3. 核心组件初始化（Settings, EhDB, Native等）
4. WebView管理器初始化
5. 内存管理器启动
6. 渠道统计初始化
7. 主题系统初始化
8. WebView预加载优化
```

---

## 对外接口

### Activity接口

#### 主要Activity列表
| Activity | 功能描述 | Intent Filter | 导出状态 |
|----------|----------|---------------|----------|
| `SplashActivity` | 启动屏和初始化 | LAUNCHER | ✅ 导出 |
| `MainActivity` | 主界面和画廊浏览 | VIEW (eh域名) | ✅ 导出 |
| `WebViewActivity` | 通用浏览器 | VIEW (HTTP/HTTPS) | ✅ 导出 |
| `YCWebViewActivity` | 优化浏览器 | VIEW (HTTP/HTTPS) | ✅ 导出 |
| `GalleryActivity` | 图片画廊查看 | VIEW (压缩文件) | ✅ 导出 |
| `MediaPlayerActivity` | 媒体播放器 | VIEW (音视频) | ✅ 导出 |
| `ApkInstallerActivity` | APK安装器 | VIEW (APK文件) | ✅ 导出 |
| `FileManagerActivity` | 文件管理器 | APP_FILES | ✅ 导出 |

#### 关键Intent Filter配置
```xml
<!-- 浏览器功能 - 处理所有HTTP/HTTPS链接 -->
<intent-filter android:priority="999">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="http" android:scheme="https" />
</intent-filter>

<!-- 文件关联 - 支持多种文件格式 -->
<intent-filter android:label="EhViewer文件查看器">
    <action android:name="android.intent.action.VIEW" />
    <data android:mimeType="application/pdf" />
    <data android:mimeType="image/*" />
    <data android:mimeType="video/*" />
</intent-filter>
```

### Service接口

#### 核心服务
- **DownloadService**: 下载管理服务
- **PasswordAutofillService**: 密码自动填充服务
- **TaskTriggerService**: 任务触发服务
- **WifiMonitorService**: WiFi监控服务
- **SmsCodeExtractorService**: 短信验证码提取服务
- **PushMessageService**: FCM推送服务

### 广播接收器
- **BootReceiver**: 启动时自动启动服务
- **SystemEventReceiver**: 系统事件监听
- **桌面小部件**: ClockWeatherWidget, WiFiManagerWidget等

---

## 关键依赖与配置

### 构建配置 (build.gradle.kts)

#### 版本信息
```kotlin
compileSdk = 35
minSdk = 23
targetSdk = 34
versionCode = 200005
versionName = "2.0.0.5"
```

#### 产品变种
```kotlin
productFlavors {
    create("appRelease") {
        dimension = "distribute"
    }
}
```

#### 核心依赖分类

**网络和解析**
```kotlin
implementation("com.squareup.okhttp3:okhttp:3.14.7")
implementation("org.jsoup:jsoup:1.15.3") 
implementation("com.alibaba:fastjson:1.2.83")
implementation("com.google.code.gson:gson:2.10.1")
```

**浏览器引擎**
```kotlin
implementation("com.tencent.tbs:tbssdk:44286")
implementation("androidx.webkit:webkit:1.13.0")
```

**数据库**
```kotlin
implementation("org.greenrobot:greendao:3.0.0")
```

**Firebase分析**
```kotlin
implementation("com.google.firebase:firebase-crashlytics:19.4.2")
implementation("com.google.firebase:firebase-analytics:22.4.0")
```

### Android权限配置

#### 核心权限
- `INTERNET`: 网络访问（必需）
- `WRITE_EXTERNAL_STORAGE`: 文件管理（必需）
- `ACCESS_NETWORK_STATE`: 网络状态检测
- `FOREGROUND_SERVICE`: 后台服务支持

#### 浏览器相关权限
- `ACCESS_FINE_LOCATION`: 位置服务
- `RECORD_AUDIO`: 录音功能
- `CAMERA`: 相机访问

#### 高级功能权限
- `USE_BIOMETRIC`: 生物识别认证
- `REQUEST_INSTALL_PACKAGES`: APK安装
- `SYSTEM_ALERT_WINDOW`: 悬浮窗显示
- `MANAGE_EXTERNAL_STORAGE`: 完整文件访问（Android 10+）

---

## 数据模型

### 核心数据实体

#### 画廊相关
```java
// GalleryDetail: 画廊详细信息
class GalleryDetail {
    long gid;              // 画廊ID
    String title;          // 标题
    String thumb;          // 缩略图URL
    String category;       // 分类
    String posted;         // 发布时间
    String uploader;       // 上传者
    int favoriteSlot;      // 收藏槽位
}

// GalleryInfo: 画廊基本信息
class GalleryInfo {
    long gid;
    String token;
    String title;
    String titleJpn;
    String thumb;
    int category;
    String posted;
    String uploader;
    float rating;
    boolean posted;
}
```

#### 下载相关
```java
// DownloadInfo: 下载信息
class DownloadInfo {
    long gid;
    String title;
    String dirname;
    int state;            // 下载状态
    int legacy;           // 遗留标记
    long time;            // 时间戳
}
```

#### 用户数据
```java
// UserTagList: 用户标签列表
class UserTagList {
    List<UserTag> userTags;
}

// HistoryInfo: 历史记录
class HistoryInfo {
    long gid;
    String title;
    long time;
    int mode;
}
```

### 数据库表结构
- **DOWNLOADS**: 下载记录表
- **DOWNLOAD_DIRNAME**: 下载目录名映射
- **DOWNLOAD_LABELS**: 下载标签
- **GALLERIES**: 画廊信息表
- **LOCAL_FAVORITES**: 本地收藏
- **HISTORY**: 浏览历史
- **QUICK_SEARCH**: 快速搜索
- **BOOKMARKS**: 书签数据
- **FILTER**: 过滤规则

---

## 测试与质量

### 测试文件结构
```
src/test/java/                    # 单元测试
├── com/hippo/ehviewer/
│   ├── ui/browser/
│   │   └── InputValidatorTest.java        # UI输入验证测试
│   ├── client/
│   │   └── NetworkDetectorUnitTest.java   # 网络检测测试
│   └── analytics/
│       └── ChannelTrackerRetryTest.java   # 渠道统计测试
└── com/EhViewer/tw/
    └── ExampleUnitTest.kt                 # 示例单元测试

src/androidTest/java/             # 集成测试
├── com/hippo/ehviewer/
│   ├── BrowserStabilityTest.java          # 浏览器稳定性测试
│   ├── BrowserPerformanceTest.java        # 浏览器性能测试
│   ├── BrowserCompatibilityTest.java      # 浏览器兼容性测试
│   ├── X5BrowserFunctionalityTest.java    # X5浏览器功能测试
│   ├── BrowserMonkeyTest.java             # 浏览器压力测试
│   └── test/
│       └── GalleryListOptimizationTest.java # 画廊列表优化测试
```

### 测试覆盖范围

#### ✅ 已覆盖的测试
- **网络层测试**: 网络连接检测和状态管理
- **UI输入验证**: 浏览器输入框验证逻辑
- **渠道统计**: 统计API重试机制和错误处理
- **浏览器功能**: X5浏览器核心功能验证
- **性能测试**: 浏览器性能基准测试
- **画廊优化**: 画廊列表加载优化验证

#### ❌ 需要补充的测试
- 文件管理功能测试
- 下载管理测试
- 数据库操作测试
- 图片缓存测试
- WebView兼容性测试
- 权限管理测试

### 质量保证工具
```kotlin
// build.gradle.kts中的质量配置
lint {
    disable += "MissingTranslation"
    abortOnError = false
    baseline = file("lint-baseline.xml")
    checkReleaseBuilds = false
}

testOptions {
    unitTests.isIncludeAndroidResources = true
}
```

---

## 常见问题 (FAQ)

### Q: 浏览器引擎如何选择？
A: 应用使用智能降级策略：
1. 优先使用腾讯X5 WebView（高性能）
2. 如果X5不可用，降级到原生WebView
3. CompatibleWebViewManager负责自动切换

### Q: 为什么需要这么多权限？
A: 权限按功能模块分类：
- 文件管理需要存储权限
- 浏览器功能需要网络和位置权限
- APK安装需要安装包权限
- 生物识别用于安全功能

### Q: 如何处理内存占用过大？
A: 应用有完整的内存管理策略：
- 基于设备内存动态调整缓存大小
- LRU策略清理过期缓存
- 内存压力时自动释放资源
- 图片解码优化减少内存占用

### Q: 支持哪些文件格式？
A: 支持的格式包括：
- **图片**: JPG, PNG, GIF, WebP, BMP, SVG
- **视频**: MP4, AVI, MKV, WebM, FLV等
- **音频**: MP3, AAC, FLAC, OGG等
- **文档**: PDF, DOC, XLS, PPT等
- **压缩**: ZIP, RAR, 7Z等

### Q: 如何自定义浏览器设置？
A: 浏览器设置通过以下方式配置：
1. Settings类中的全局配置
2. BrowserSettingsActivity用户界面
3. 网络适配器自动优化
4. 用户脚本系统自定义增强

---

## 相关文件清单

### 核心源代码文件（前50个最重要的）
```
src/main/java/com/hippo/ehviewer/
├── EhApplication.java                     # 应用主类
├── Settings.java                          # 全局设置
├── EhDB.java                             # 数据库管理
├── AppConfig.java                        # 应用配置
├── Analytics.java                        # 分析统计
├── Crash.java                           # 崩溃处理
├── Hosts.java                           # 主机管理
├── GetText.java                         # 文本国际化
├── EhProxySelector.java                 # 代理选择器
├── FavouriteStatusRouter.java           # 收藏状态路由
├── SystemCompatibilityManager.java      # 系统兼容性
├── ImageBitmapHelper.java               # 图像辅助
├── UrlOpener.java                       # URL打开器
├── ui/
│   ├── MainActivity.java                # 主界面
│   ├── WebViewActivity.java            # WebView浏览器
│   ├── YCWebViewActivity.java          # 优化浏览器
│   ├── GalleryActivity.java            # 画廊查看
│   ├── SettingsActivity.java           # 设置界面
│   ├── EhActivity.java                 # 基础Activity
│   ├── DownloadManagerActivity.java     # 下载管理
│   ├── BrowserSettingsActivity.java    # 浏览器设置
│   ├── FileManagerActivity.java        # 文件管理
│   ├── MediaPlayerActivity.java        # 媒体播放
│   ├── ApkInstallerActivity.java       # APK安装
│   ├── PasswordManagerActivity.java    # 密码管理
│   ├── NovelReaderActivity.java        # 小说阅读
│   ├── BookmarksActivity.java          # 书签管理
│   └── splash/
│       └── SplashActivity.kt           # 启动屏
├── client/
│   ├── EhClient.java                   # EH客户端
│   ├── EhEngine.java                   # EH引擎
│   ├── EhCookieStore.java              # Cookie存储
│   ├── EhHosts.java                    # EH主机
│   ├── BandwidthManager.java           # 带宽管理
│   ├── MemoryManager.java              # 内存管理
│   ├── X5WebViewManager.java           # X5管理
│   └── data/                           # 数据模型
│       ├── GalleryDetail.java          # 画廊详情
│       ├── GalleryInfo.java            # 画廊信息
│       └── userTag/
│           └── UserTagList.java        # 用户标签
├── download/
│   ├── DownloadManager.java            # 下载管理器
│   ├── DownloadService.kt              # 下载服务
│   └── DownloadTorrentManager.kt       # 种子下载管理
├── spider/
│   ├── SpiderDen.java                  # 爬虫窝点
│   └── SpiderQueenEnhancer.java        # 爬虫增强
├── browser/
│   ├── CompatibleWebViewManager.java   # 兼容WebView管理
│   └── AddressBarAnimator.kt           # 地址栏动画
├── preference/
│   └── SignOutPreference.kt            # 登出偏好
├── analytics/
│   └── ChannelTracker.java             # 渠道统计
└── gallery/
    └── Pipe.kt                         # 画廊管道
```

### 资源文件结构
```
src/main/res/
├── layout/                             # 布局文件
├── drawable/                           # 图标和图片资源
├── mipmap-*/                          # 应用图标
├── values/                            # 字符串、颜色、尺寸
├── xml/                              # 配置文件
├── anim/                             # 动画资源
└── color/                            # 颜色资源
```

### 配置文件
```
├── build.gradle.kts                   # 构建脚本
├── proguard-rules.pro                 # 代码混淆规则
└── src/main/
    ├── AndroidManifest.xml            # 应用清单
    └── cpp/
        └── CMakeLists.txt             # Native构建脚本
```

---

## 变更记录 (Changelog)

### 2025-09-10
- **模块文档创建**: 完成app模块的完整架构分析
- **接口梳理**: 整理了Activity、Service、Receiver的完整列表
- **测试覆盖分析**: 评估了当前测试状况和缺失的测试领域
- **文件清单**: 按重要性列出了核心源代码文件

### 待办事项
- [ ] 补充文件管理功能的单元测试
- [ ] 添加数据库操作的集成测试
- [ ] 完善WebView兼容性测试覆盖
- [ ] 优化内存管理测试策略
- [ ] 增加权限管理相关测试

---

<div align="center">

[⬆ 返回根目录](../CLAUDE.md) | [📱 App模块架构](./CLAUDE.md)

**App主应用模块文档** - EhViewerh v2.0.0.5

</div>