# 🚀 EhViewer GitHub 更新配置指南

## 📋 配置概览

✅ **远程仓库**: `https://github.com/loututu114-pixel/ehviewer.git`
✅ **用户名**: `loututu114-pixel`
✅ **邮箱**: `loututu114@gmail.com`
✅ **更新URL**: `https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/update.json`

## 🛠️ 第一步：创建GitHub仓库

### 方法一：使用GitHub网页创建
1. 打开 [GitHub.com](https://github.com)
2. 点击右上角 **"+"** → **"New repository"**
3. 填写信息：
   ```
   Repository name: ehviewer
   Description: EhViewer 更新配置文件
   Public/Private: Public (推荐公开)
   ```
4. 点击 **"Create repository"**

### 方法二：使用Git命令创建
```bash
# 初始化本地仓库
cd /path/to/your/folder
git init
echo "# EhViewer Updates" > README.md
git add README.md
git commit -m "Initial commit"

# 添加远程仓库
git remote add origin https://github.com/loututu114-pixel/ehviewer.git
git push -u origin main
```

## 📝 第二步：上传配置文件

### 1. 上传update.json
```bash
# 复制配置文件到仓库目录
cp /Users/lu/AndroidStudioProjects/EhViewerh/update.json /path/to/your/repo/

# 提交并推送
cd /path/to/your/repo
git add update.json
git commit -m "Add update configuration"
git push origin main
```

### 2. 验证文件访问
打开浏览器访问：
```
https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/update.json
```

## 📱 第三步：上传APK文件

### 方法一：使用GitHub Releases
1. 在仓库页面点击 **"Releases"**
2. 点击 **"Create a new release"**
3. 填写信息：
   ```
   Tag version: v1.9.9.18
   Release title: EhViewer 卡通风格版本
   Description: 全新的卡通风格UI设计版本
   ```
4. 上传APK文件：`EhViewer_CN_SXJ_v1.9.9.17_Cartoon_Style.apk`
5. 点击 **"Publish release"**

### 方法二：直接上传到仓库
```bash
# 创建downloads目录
mkdir downloads
cp EhViewer_CN_SXJ_v1.9.9.17_Cartoon_Style.apk downloads/

# 提交APK文件
git add downloads/
git commit -m "Add APK download"
git push origin main
```

## 🔧 第四步：更新配置

### 自动配置已完成 ✅
应用已自动配置为使用您的仓库：
```xml
<string-array name="update_metadata">
    <item>update_json</item>
    <item>https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/update.json</item>
</string-array>
```

## 📋 第五步：自定义update.json

### 基本结构
```json
{
  "version": "1.9.9.18",
  "versionCode": 189918,
  "mustUpdate": false,
  "updateContent": {
    "title": "EhViewer卡通风格新版本",
    "content": [
      "🎨 全新的卡通风格UI设计",
      "📱 完美适配手机、平板等多屏幕设备",
      "🔧 优化详情页样式和图标显示",
      "⚡ 提升应用性能和响应速度",
      "🎯 改进标签显示效果和交互体验"
    ],
    "fileDownloadUrl": "https://github.com/loututu114-pixel/ehviewer/releases/download/v1.9.9.18/EhViewer_CN_SXJ_v1.9.9.18.apk"
  }
}
```

### 参数说明
| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `version` | String | 版本号 | `"1.9.9.18"` |
| `versionCode` | Integer | 内部版本号 | `189918` |
| `mustUpdate` | Boolean | 是否强制更新 | `false` |
| `title` | String | 更新标题 | `"新版本发布"` |
| `content` | Array | 更新内容列表 | `["新功能1", "修复2"]` |
| `fileDownloadUrl` | String | APK下载地址 | `"https://..."` |

## 🧪 第六步：测试配置

### 1. 验证JSON文件
```bash
curl https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/update.json
```

### 2. 测试应用更新
1. 安装应用：`EhViewer_CN_SXJ_v1.9.9.17_Cartoon_Style.apk`
2. 打开应用 → 设置 → 关于 → 检查更新
3. 验证是否显示更新提示

## 🚀 第七步：发布新版本

### 定期更新流程
1. **修改update.json**
   ```json
   {
     "version": "1.9.9.19",
     "versionCode": 189919,
     "updateContent": {
       "title": "EhViewer v1.9.9.19 更新",
       "content": ["新增功能...", "修复bug..."],
       "fileDownloadUrl": "https://github.com/loututu114-pixel/ehviewer/releases/download/v1.9.9.19/app.apk"
     }
   }
   ```

2. **编译新版本APK**
   ```bash
   ./gradlew assembleRelease
   ```

3. **创建GitHub Release**
   - 上传新APK文件
   - 更新update.json中的下载链接
   - 提交更改到仓库

4. **推送更新**
   ```bash
   git add update.json
   git commit -m "Update to v1.9.9.19"
   git push origin main
   ```

## 📊 第八步：监控和统计

### GitHub统计
- 查看仓库的访问统计
- 监控Release下载次数
- 分析用户活跃度

### 自定义统计（可选）
```json
{
  "version": "1.9.9.18",
  "versionCode": 189918,
  "updateContent": {
    "title": "EhViewer更新",
    "content": ["更新内容..."],
    "fileDownloadUrl": "https://your-cdn.com/app.apk"
  }
}
```

## ⚠️ 注意事项

### 🔒 安全提醒
- 定期更新APK文件的下载链接
- 验证APK文件的完整性
- 监控异常访问和下载

### 📱 用户体验
- 保持更新内容的简洁明了
- 使用emoji让更新内容更生动
- 及时响应用户反馈

### 🔧 技术要点
- JSON格式必须正确
- 版本号要递增
- 下载链接要可访问

## 🎯 快速开始

1. ✅ **创建GitHub仓库**
2. ✅ **上传update.json**
3. ✅ **配置已自动完成**
4. ✅ **测试更新功能**

## 📞 技术支持

如有问题，请检查：
1. GitHub仓库是否公开
2. update.json格式是否正确
3. 网络连接是否正常
4. APK下载链接是否有效

---

**🎉 配置完成！您的EhViewer现在使用您自己的GitHub仓库进行更新管理！**
