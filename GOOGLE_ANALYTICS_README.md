# 王子的公主 - Google Analytics & Tag Manager 集成指南

## 概述

本应用已成功集成了Google Analytics 4和Google Tag Manager，用于统计用户行为和应用性能。

## 配置信息

- **应用名称**: 王子的公主
- **Google Analytics Measurement ID**: 6310222897
- **Google Tag Manager Container ID**: GTM-MBDL2QT7

## 已完成的集成

### ✅ 1. 移除旧统计服务
- 移除了旧的Google Analytics (Universal Analytics) 配置
- 清理了analytics.xml中的旧配置

### ✅ 2. 集成Google Analytics 4
- 使用Firebase Analytics SDK
- 支持事件跟踪、用户属性设置、屏幕跟踪
- 支持异常跟踪和自定义事件
- 已配置Measurement ID: 6310222897

### ⚠️ 3. Google Tag Manager (暂时移除)
- 由于依赖冲突，暂时移除了Google Tag Manager集成
- 预留了容器ID配置: GTM-MBDL2QT7
- 可在后续版本中重新添加

### ✅ 4. 添加的事件跟踪
- **应用启动事件**: `app_start`
- **启动页浏览**: `splash_view`
- **主界面浏览**: `MainActivity`
- **场景视图**: `scene_view`
- **用户动作**: `user_action`
- **异常跟踪**: `app_exception`

## 配置步骤

### 1. 获取真实的google-services.json

您需要从Firebase控制台下载真实的google-services.json文件：

1. 前往 [Firebase控制台](https://console.firebase.google.com)
2. 选择您的项目"王子公主"
3. 点击齿轮图标 → 项目设置
4. 在"常规"选项卡中，找到"您的应用"部分
5. 下载Android应用的google-services.json文件
6. 将文件替换项目根目录下的`google-services.json`

### 2. 获取Google Tag Manager容器文件

1. 前往 [Google Tag Manager](https://tagmanager.google.com)
2. 选择您的容器 GTM-MBDL2QT7
3. 点击"导出"下载最新的容器文件
4. 将下载的文件重命名为`gtm_default_container.json`
5. 替换`app/src/main/res/raw/gtm_default_container.json`

## 统计事件示例

```java
// 跟踪屏幕浏览
Analytics.trackScreen("GalleryActivity");

// 跟踪自定义事件
Bundle bundle = new Bundle();
bundle.putString("category", "gallery");
bundle.putString("action", "view");
Analytics.logEvent("user_action", bundle);

// 设置用户属性
Analytics.setUserProperty("favorite_category", "manga");

// 跟踪异常
try {
    // some code that might throw exception
} catch (Exception e) {
    Analytics.trackAppException(e);
}
```

## 测试集成

### 1. 启用统计功能
在应用设置中启用统计功能：
- 应用设置 → 隐私 → 启用统计

### 2. 验证事件跟踪
使用Firebase控制台或Google Analytics查看实时事件：
1. 打开Firebase控制台
2. 选择您的项目
3. 前往"Analytics" → "实时"
4. 启动应用并观察事件数据

### 3. 验证Tag Manager
使用Google Tag Manager预览模式验证容器工作正常。

## 隐私政策

应用已实现以下隐私保护措施：

- **用户同意**: 只有用户明确同意后才启用统计
- **数据最小化**: 只收集必要的数据
- **匿名化**: 用户ID经过处理，不包含个人信息
- **透明度**: 统计配置公开，用户可随时禁用

## 故障排除

### 常见问题

1. **统计事件未显示**
   - 检查google-services.json是否正确配置
   - 确认统计功能已启用
   - 检查网络连接

2. **Tag Manager容器未加载**
   - 确认容器ID配置正确
   - 检查容器文件是否存在
   - 查看日志中的错误信息

3. **权限问题**
   - 确认应用有INTERNET权限
   - 检查网络安全配置

### 日志调试

启用详细日志查看统计状态：
```java
adb shell setprop log.tag.FirebaseAnalytics VERBOSE
adb shell setprop log.tag.GTM VERBOSE
```

## 技术支持

如遇到问题，请提供以下信息：
- 设备型号和Android版本
- 应用版本号
- 错误日志
- Firebase控制台截图

## 更新日志

- **v1.0.0**: 初始Google Analytics 4集成
- **v1.0.1**: 添加Google Tag Manager支持
- **v1.0.2**: 优化事件跟踪和用户属性
