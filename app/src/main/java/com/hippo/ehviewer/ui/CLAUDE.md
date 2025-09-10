[根目录](../../../../../CLAUDE.md) > [app](../../../../..) > [java](../../..) > [com](../..) > [hippo](..) > [ehviewer](.) > **ui**

# UI层模块文档

> EhViewer Android应用的用户界面层，包含39个Activity和相关UI组件

## 模块职责

UI层负责EhViewer的所有用户交互界面，包括：
- **主界面**: 应用入口和导航中心
- **浏览器界面**: 完整的WebView浏览器实现
- **画廊界面**: 图片浏览和管理
- **文件管理**: 文件系统浏览和操作
- **设置界面**: 应用配置和偏好设置
- **多媒体支持**: 图片、视频、音频播放
- **工具界面**: APK安装、缓存管理等

## 入口与启动

### 主入口
```java
// app/src/main/java/com/hippo/ehviewer/ui/MainActivity.java
public class MainActivity extends EhActivity {
    // 应用主入口，负责初始化和路由分发
    // 集成导航抽屉、搜索功能、画廊列表等
}
```

### 核心启动流程
1. **EhApplication.onCreate()** → 初始化核心组件
2. **MainActivity.onCreate()** → 加载主界面
3. **检查权限和设置** → 引导用户完成必要配置
4. **加载默认页面** → 显示画廊列表或设置的首页

## 对外接口

### 主要Activity接口

| Activity | 职责 | 启动方式 | 主要功能 |
|---------|------|---------|---------|
| `MainActivity` | 应用主入口 | Launcher | 画廊浏览、导航中心 |
| `WebViewActivity` | 浏览器 | Intent/内部调用 | 网页浏览、用户脚本支持 |
| `GalleryActivity` | 画廊详情 | 内部调用 | 图片查看、下载管理 |
| `FileManagerActivity` | 文件管理 | 内部调用 | 文件浏览、文件操作 |
| `SettingsActivity` | 设置中心 | 内部调用 | 应用配置、偏好管理 |
| `DownloadManagerActivity` | 下载管理 | 内部调用 | 下载任务、进度监控 |
| `MediaPlayerActivity` | 媒体播放 | Intent处理 | 视频/音频播放 |
| `ApkInstallerActivity` | APK安装 | Intent处理 | 应用安装、权限管理 |

### Intent过滤器
```xml
<!-- 浏览器功能 -->
<intent-filter android:label="@string/app_name">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="http" />
    <data android:scheme="https" />
</intent-filter>

<!-- 文件处理 -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="application/vnd.android.package-archive" />
</intent-filter>
```

## 关键依赖与配置

### UI框架依赖
```kotlin
// AndroidX核心
implementation("androidx.appcompat:appcompat:1.7.0")
implementation("androidx.fragment:fragment:1.8.6")
implementation("androidx.recyclerview:recyclerview:1.2.0")
implementation("androidx.cardview:cardview:1.0.0")

// Material Design
implementation("com.google.android.material:material:1.12.0")

// ViewBinding
android {
    buildFeatures {
        viewBinding = true
    }
}
```

### 自定义组件依赖
```kotlin
// EhViewer自定义UI组件
implementation("com.github.seven332:drawerlayout:ea2bb388f0")
implementation("com.github.seven332:easyrecyclerview:0.1.1")
implementation("com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0")
```

### 主题配置
```xml
<!-- app/src/main/res/values/styles.xml -->
<style name="AppTheme" parent="Theme.MaterialComponents.DayNight">
    <!-- 自定义主题配置 -->
    <item name="colorPrimary">@color/primary</item>
    <item name="colorPrimaryVariant">@color/primary_dark</item>
    <item name="colorOnPrimary">@color/on_primary</item>
</style>
```

## 数据模型

### UI状态管理
```java
// 使用Repository模式管理UI数据
public class GalleryListRepository {
    private final EhClient mClient;
    private final Cache mCache;
    
    public Observable<GalleryListResult> getGalleryList(String keyword, int page) {
        // 获取画廊列表数据
    }
}
```

### ViewBinding使用
```java
public class MainActivity extends EhActivity {
    private ActivityMainBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
```

## 测试与质量

### UI测试覆盖
- **单元测试**: Activity生命周期、数据绑定逻辑
- **集成测试**: Activity间跳转、Intent处理
- **UI测试**: Espresso自动化测试

### 测试示例
```java
// app/src/androidTest/java/com/EhViewer/tw/ui/MainActivityTest.java
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = 
        new ActivityTestRule<>(MainActivity.class);
    
    @Test
    public void testNavigationDrawer() {
        // 测试导航抽屉功能
        onView(withId(R.id.drawer_layout))
            .perform(DrawerActions.open());
    }
}
```

### 性能优化
- **布局优化**: 使用ConstraintLayout减少层级
- **RecyclerView**: 优化ViewHolder复用
- **图片加载**: Conaco异步加载和缓存
- **内存管理**: Activity生命周期内存清理

## 常见问题 (FAQ)

### Q: 如何添加新的Activity？
A: 
1. 继承`EhActivity`基类
2. 在AndroidManifest.xml中注册
3. 添加对应的布局文件
4. 实现必要的生命周期方法

### Q: WebView相关问题如何处理？
A: 
1. 检查X5内核是否初始化成功
2. 使用原生WebView作为降级方案
3. 处理WebView权限和安全策略
4. 参考`WebViewActivity`的实现

### Q: 如何处理Android 11+的存储权限？
A: 
1. 使用`MANAGE_EXTERNAL_STORAGE`权限
2. 引导用户到系统设置页面
3. 使用UniFile抽象层处理文件访问
4. 参考`FileManagerActivity`的权限处理

### Q: 屏幕适配问题？
A: 
1. 使用dp单位而非px
2. 提供多种密度的资源文件
3. 使用ConstraintLayout响应式布局
4. 考虑平板和横屏适配

## 相关文件清单

### 核心Activity文件
```
app/src/main/java/com/hippo/ehviewer/ui/
├── MainActivity.java              # 主入口Activity
├── WebViewActivity.java           # 浏览器Activity  
├── GalleryActivity.java           # 画廊Activity
├── FileManagerActivity.java       # 文件管理Activity
├── SettingsActivity.java          # 设置Activity
├── DownloadManagerActivity.java   # 下载管理Activity
├── MediaPlayerActivity.java       # 媒体播放Activity
├── ApkInstallerActivity.java      # APK安装Activity
├── SetupWizardActivity.java       # 设置向导Activity
└── ... (30+ 其他Activity)
```

### 布局资源文件
```
app/src/main/res/layout/
├── activity_main.xml              # 主界面布局
├── activity_webview.xml           # 浏览器布局  
├── activity_gallery.xml           # 画廊布局
├── activity_file_manager.xml      # 文件管理布局
├── fragment_gallery_list.xml      # 画廊列表片段
└── ... (50+ 布局文件)
```

### 自定义控件
```
app/src/main/java/com/hippo/ehviewer/widget/
├── EhDrawerView.java              # 自定义抽屉视图
├── GalleryHeader.java             # 画廊头部组件
├── ImageSearchLayout.java         # 图片搜索布局
├── CategoryTable.java             # 分类表格
└── ... (40+ 自定义控件)
```

## 变更记录 (Changelog)

### 2025-09-06 03:01:06 - UI模块文档初始化
- 创建UI层模块文档
- 整理39个Activity的职责和接口
- 添加UI测试策略和性能优化指南
- 补充常见问题和解决方案

---

*本文档自动生成 - 最后更新: 2025-09-06 03:01:06*