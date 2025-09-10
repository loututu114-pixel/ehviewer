# EhViewer 浏览器设置功能

## 🎯 功能概述

EhViewer 现在具备了完整的浏览器设置和管理功能，能够确保应用在Android系统中正确显示为浏览器选项，并提供便捷的默认浏览器设置管理。

## 🔧 核心功能

### 1. 浏览器注册管理
- **自动注册检测**: 应用启动时自动检测浏览器注册状态
- **智能修复**: 如果注册有问题，自动尝试修复
- **状态监控**: 实时监控浏览器注册状态

### 2. 默认浏览器设置
- **一键设置**: 支持Android 10+的RoleManager API
- **兼容性处理**: 为低版本Android提供手动设置指导
- **状态检查**: 检查当前默认浏览器状态

### 3. 系统集成优化
- **intent-filter完善**: 扩展了各种协议和MIME类型的处理
- **权限优化**: 添加了必要的浏览器角色权限
- **组件管理**: 自动管理Activity的启用状态

## 📱 用户界面

### 浏览器设置活动
提供以下功能按钮：

1. **检查状态**
   - 显示EhViewer是否在浏览器列表中
   - 检查默认浏览器设置状态
   - 显示系统中的浏览器数量

2. **修复注册**
   - 强制重新注册浏览器组件
   - 清理系统缓存
   - 重新扫描包信息

3. **设为默认浏览器**
   - 使用RoleManager请求默认浏览器角色
   - 兼容Android不同版本
   - 提供手动设置指导

4. **打开系统设置**
   - 跳转到系统默认应用设置页面
   - 提供手动设置的快捷方式

5. **测试浏览器**
   - 测试EhViewer的浏览器功能
   - 验证intent-filter是否正常工作

## 🔍 技术实现

### 核心类

#### BrowserRegistrationManager
```java
public class BrowserRegistrationManager {
    // 检测浏览器可见性
    public boolean isBrowserVisible()

    // 获取所有浏览器应用
    public List<ResolveInfo> getAllBrowsers()

    // 修复浏览器注册
    public boolean fixBrowserRegistration()

    // 检查注册状态
    public BrowserRegistrationStatus getRegistrationStatus()
}
```

#### BrowserSettingsActivity
```java
public class BrowserSettingsActivity extends AppCompatActivity {
    // 状态检查
    private void checkBrowserStatus()

    // 注册修复
    private void fixBrowserRegistration()

    // 默认浏览器设置
    private void setAsDefaultBrowser()
}
```

### AndroidManifest配置

#### 扩展的intent-filter
```xml
<!-- 浏览器intent-filter：处理所有HTTP/HTTPS链接 -->
<intent-filter android:autoVerify="true" android:priority="999">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="http" />
    <data android:scheme="https" />
    <data android:host="*" />
</intent-filter>

<!-- 新增的协议支持 -->
<intent-filter android:priority="999">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="about" />
    <data android:scheme="javascript" />
    <data android:scheme="data" />
    <data android:scheme="blob" />
</intent-filter>
```

#### 权限配置
```xml
<!-- 浏览器角色权限 -->
<uses-permission android:name="com.android.permission.REQUEST_ROLE" />
```

## 🚀 智能检测机制

### 用户地区检测
```java
private boolean isChineseUser() {
    // 检测系统语言
    String language = context.getResources().getConfiguration().locale.getLanguage();
    if ("zh".equals(language)) return true;

    // 检测国家代码
    String country = context.getResources().getConfiguration().locale.getCountry();
    if ("CN".equals(country) || "HK".equals(country) || "TW".equals(country)) return true;

    // 检测时区
    TimeZone tz = TimeZone.getDefault();
    String tzId = tz.getID();
    if (tzId.contains("Asia/Shanghai") || tzId.contains("Asia/Hong_Kong") ||
        tzId.contains("Asia/Taipei")) return true;

    return false;
}
```

### 注册状态检查
```java
public BrowserRegistrationStatus getRegistrationStatus() {
    BrowserRegistrationStatus status = new BrowserRegistrationStatus();

    status.isVisible = isBrowserVisible();
    status.isDefault = DefaultBrowserHelper.isDefaultBrowser(context);
    status.browserCount = getAllBrowsers().size();
    status.canRequestRole = DefaultBrowserHelper.canRequestDefaultBrowserRole(context);
    status.intentFiltersWorking = checkAndFixIntentFilters();
    status.activityEnabled = checkActivityEnabled();

    return status;
}
```

## 📊 状态监控

### 浏览器注册状态
- ✅ **是否可见**: EhViewer是否在系统浏览器列表中
- ✅ **是否默认**: 是否为当前默认浏览器
- ✅ **浏览器数量**: 系统中的浏览器应用数量
- ✅ **角色请求**: 是否可以请求默认浏览器角色
- ✅ **intent-filter**: intent-filter是否正常工作
- ✅ **Activity状态**: WebViewActivity是否已启用

### 状态报告
```
浏览器注册状态报告:
系统浏览器数量: 5
✓ EhViewer在浏览器列表中可见
✗ EhViewer不是默认浏览器
✓ WebViewActivity已启用
✓ Intent-filter正常工作
✓ 可以请求默认浏览器角色
```

## 🔄 自动修复机制

### 启动时自动检查
```java
private void checkBrowserRegistration() {
    BrowserRegistrationManager registrationManager =
        new BrowserRegistrationManager(this);

    boolean isVisible = registrationManager.isBrowserVisible();

    if (!isVisible) {
        Log.w(TAG, "EhViewer not visible in browser list, attempting to fix");
        boolean fixed = registrationManager.fixBrowserRegistration();
        if (fixed) {
            Log.d(TAG, "Browser registration fixed successfully");
        } else {
            Log.w(TAG, "Failed to fix browser registration");
        }
    }
}
```

### 强制注册修复
1. **启用Activity组件**
2. **发送包更新广播**
3. **清理系统缓存**
4. **重新扫描包信息**

## 🎨 用户体验

### 界面设计
- **直观的按钮布局**: 每个功能都有清晰的按钮
- **状态实时显示**: 检查结果实时更新
- **操作反馈**: 所有操作都有Toast提示
- **帮助信息**: 提供详细的使用说明

### 操作流程
1. **启动应用** → 自动检查浏览器注册状态
2. **发现问题** → 自动尝试修复
3. **需要手动设置** → 跳转到系统设置页面
4. **验证功能** → 测试浏览器是否正常工作

## 🔧 兼容性处理

### Android版本兼容
- **Android 10+**: 使用RoleManager API
- **Android 9及以下**: 提供手动设置指导

### 系统差异处理
- **不同厂商**: 适配各种厂商的系统设置
- **权限处理**: 处理各种权限申请场景
- **组件状态**: 处理Activity的启用/禁用状态

## 📈 性能优化

### 启动优化
- **延迟执行**: 浏览器检查延迟到应用启动2秒后
- **异步处理**: 所有检查操作都在后台线程执行
- **缓存机制**: 缓存检查结果避免重复操作

### 资源优化
- **轻量级界面**: 浏览器设置界面设计简洁
- **按需加载**: 只在需要时加载相关资源
- **内存管理**: 及时清理临时对象和缓存

## 🚀 优势特点

1. **自动化**: 大部分问题可以自动检测和修复
2. **全面性**: 覆盖浏览器注册的所有方面
3. **兼容性**: 支持各种Android版本和厂商
4. **用户友好**: 提供清晰的状态反馈和操作指导
5. **性能优秀**: 轻量级设计，不影响应用性能
6. **可靠性**: 多重检查机制，确保功能稳定

这个浏览器设置功能让EhViewer能够完美集成到Android系统中，确保用户可以方便地使用它作为默认浏览器！🎯
