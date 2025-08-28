# 获取 google-services.json 文件的完整指南

## 📋 前提条件

在开始之前，请确保您拥有：
- Google 账号
- Firebase 项目的所有权或编辑权限
- 王子的公主应用的包名: `com.hippo.ehviewer`

## 🚀 获取 google-services.json 的步骤

### 步骤 1: 访问 Firebase 控制台

1. 打开浏览器，访问 [Firebase 控制台](https://console.firebase.google.com/)
2. 使用您的 Google 账号登录

### 步骤 2: 选择或创建项目

#### 情况 1: 如果您已经有"王子公主"项目
1. 在项目列表中找到 **王子公主** 项目
2. 点击进入项目

#### 情况 2: 如果需要创建新项目
1. 点击 **创建项目**
2. 输入项目名称: `王子公主`
3. 按照提示完成项目创建

### 步骤 3: 添加 Android 应用

1. 在 Firebase 项目主页，点击 **添加应用** 按钮
2. 选择 **Android** 图标
3. 在 **Android 包名** 字段中输入: `com.hippo.ehviewer`
4. 可选: 输入应用昵称: "王子的公主"
5. 点击 **注册应用**

### 步骤 4: 下载配置文件

1. Firebase 会显示下载 **google-services.json** 的选项
2. 点击 **下载 google-services.json** 按钮
3. 文件会下载到您的电脑

### 步骤 5: 配置应用信息（重要）

在 Firebase 控制台中完成以下配置：

#### 5.1 设置应用信息
1. **应用昵称**: 王子的公主
2. **调试签名证书 SHA-1**: （可选，用于调试模式）
   - 如果需要，可以使用以下命令获取 SHA-1：
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

#### 5.2 启用 Google Analytics
1. 确保 **启用 Google Analytics** 已勾选
2. 选择或创建 Google Analytics 账户
3. 设置账户名称（例如：王子公主Analytics）
4. 接受数据共享设置

### 步骤 6: 验证配置

1. 在 Firebase 控制台的 **项目设置 > 常规** 标签页
2. 向下滚动到 **您的应用** 部分
3. 确认您的 Android 应用已正确添加
4. 包名应该是: `com.hippo.ehviewer`

## 📂 放置 google-services.json 文件

下载完成后：

1. 将 `google-services.json` 文件复制到项目根目录:
   ```
   /Users/lu/AndroidStudioProjects/EhViewerh/google-services.json
   ```

2. **重要**: 确保文件路径正确，不要放在 `app/` 子目录中

## 🔍 验证配置是否正确

### 方法 1: 检查 Firebase 控制台
1. 进入 Firebase 控制台
2. 选择 **Analytics** 部分
3. 查看 **实时** 数据
4. 启动应用后应该能看到实时事件

### 方法 2: 查看应用日志
运行应用时查看 Android Studio 的 Logcat，应该能看到：
```
Firebase Analytics 启用
Google Analytics 已初始化
```

## ⚠️ 常见问题

### 问题 1: 下载失败
**解决方案**:
- 检查网络连接
- 确认您有项目的编辑权限
- 尝试刷新页面重新下载

### 问题 2: 包名不匹配
**解决方案**:
- 确认包名完全正确: `com.hippo.ehviewer`
- 如果不匹配，需要重新添加应用

### 问题 3: Analytics 数据不显示
**解决方案**:
- 确认应用已启用统计功能（设置中开启）
- 等待 24 小时让数据完全同步
- 检查 google-services.json 文件是否正确放置

## 🔧 高级配置（可选）

### 启用 Crashlytics
如果需要崩溃报告功能：
1. 在 Firebase 控制台选择 **Crashlytics**
2. 按照提示添加依赖和配置

### 配置多个环境
如果有调试和发布两个版本：
1. 为每个版本创建单独的应用配置
2. 使用不同的包名（如 `com.hippo.ehviewer.debug`）

## 📞 获取帮助

如果遇到问题：
1. 查看 [Firebase 文档](https://firebase.google.com/docs/android/setup)
2. 访问 [Firebase 帮助中心](https://support.google.com/firebase)
3. 检查 [Stack Overflow](https://stackoverflow.com/questions/tagged/firebase) 上的相关问题

## ✅ 下一步

配置完成后：
1. 重新构建应用
2. 测试统计功能
3. 在 Firebase 控制台查看数据

---

**🎯 重要提醒**: google-services.json 文件包含敏感信息，请不要将其提交到版本控制系统（如 Git）。确保在 .gitignore 文件中已添加此文件的排除规则。
