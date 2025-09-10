# EhViewer 版本发布流程文档

## 概述

本文档描述了 EhViewer 项目的完整版本发布流程，包括版本管理、渠道设置、构建打包、测试验证、发布部署等关键步骤。

## 版本管理策略

### 版本号规范
- **格式**: `MAJOR.MINOR.PATCH.BUILD`
- **示例**: `2.0.0.1`
  - `MAJOR`: 主要版本号，重大功能更新
  - `MINOR`: 次要版本号，新增功能
  - `PATCH`: 修复版本号，错误修复
  - `BUILD`: 构建版本号，细微调整

### versionCode 规范
- **格式**: `MAJOR * 100000 + MINOR * 10000 + PATCH * 1000 + BUILD`
- **示例**: `200001` (对应 v2.0.0.1)

## 渠道管理

### 渠道代码设置
- **官方正式版 0000**: 官方正式发布版本，推荐普通用户使用
- **渠道包 3001**: 特定渠道发布版本，包含渠道统计
- **开发测试版 9999**: 开发调试版本，用于内部测试

### 渠道配置位置
在 `app/build.gradle.kts` 中设置：
```kotlin
buildTypes {
    release {
        buildConfigField("String", "CHANNEL_CODE", "\"0000\"")  // 默认官方正式版
        // 或者 "3001" 渠道包
        // 或者 "9999" 开发测试版
    }
    debug {
        buildConfigField("String", "CHANNEL_CODE", "\"0000\"")  // Debug默认也是官方版
    }
}
```

## 发布流程步骤

### 1. 准备阶段

#### 1.1 检查项目状态
```bash
# 检查当前分支和状态
git status
git log --oneline -5

# 确认工作目录干净或所有更改已提交
```

#### 1.2 更新版本信息
编辑 `app/build.gradle.kts`：
```kotlin
defaultConfig {
    versionCode = 200001      // 计算新的版本代码
    versionName = "2.0.0.1"   // 设置新的版本名称
}
```

#### 1.3 确认渠道设置
检查发布构建类型中的渠道配置：
```kotlin
buildConfigField("String", "CHANNEL_CODE", "\"3001\"")
```

### 2. 构建阶段

#### 2.1 清理项目
```bash
./gradlew clean
```

#### 2.2 构建发布版本
```bash
./gradlew assembleRelease
```

#### 2.3 验证构建结果
```bash
# 检查APK文件
ls -la app/build/outputs/apk/appRelease/release/

# 确认APK大小合理（通常 20-30MB）
ls -lh app/build/outputs/apk/appRelease/release/app-appRelease-release.apk

# 验证APK结构
unzip -l app/build/outputs/apk/appRelease/release/app-appRelease-release.apk | head -10
```

### 3. 测试阶段

#### 3.1 基础验证
- ✅ APK 文件生成成功
- ✅ 文件大小合理
- ✅ APK 结构完整
- ✅ 包含必要的 classes.dex 和 AndroidManifest.xml

#### 3.2 功能测试（可选）
```bash
# 安装到测试设备
adb install app/build/outputs/apk/appRelease/release/app-appRelease-release.apk

# 验证关键功能
# - 应用启动正常
# - 版本号显示正确
# - 渠道功能正常
# - 核心功能无异常
```

### 4. 发布阶段

#### 4.1 创建版本提交
```bash
# 添加版本更改
git add app/build.gradle.kts

# 如有其他相关更改，一并添加
git add .

# 创建版本发布提交
git commit -m "Release v2.0.0.1: EhViewer 渠道3001正式发布

- 升级版本号到 2.0.0.1 (versionCode: 200001)
- 设置渠道代码为 3001
- [其他功能更新说明]

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

#### 4.2 创建版本标签
```bash
git tag -a "v2.0.0.1" -m "Release v2.0.0.1: EhViewer TUTU 渠道3001正式版"
```

#### 4.3 推送到远程仓库
```bash
# 推送代码
git push origin main

# 推送标签
git push origin v2.0.0.1
```

### 5. 部署阶段

#### 5.1 GitHub Release
1. 访问 GitHub 仓库的 Releases 页面
2. 点击 "Create a new release"
3. 选择刚创建的标签 `v2.0.0.1`
4. 填写发布说明：
   ```markdown
   # EhViewer v2.0.0.1 正式发布
   
   ## 🚀 新功能
   - [列出主要新功能]
   
   ## 🐛 问题修复 
   - [列出重要修复]
   
   ## 🔧 技术更新
   - 渠道代码：3001
   - 版本代码：200001
   
   ## 📦 下载
   - APK大小：~24MB
   - 最低支持：Android 6.0 (API 23)
   ```

5. 上传 APK 文件作为发布资产
6. 发布 Release

#### 5.2 更新下载地址
更新项目文档中的下载链接，确保指向最新版本。

## 自动化脚本

### 快速发布脚本
创建 `scripts/release.sh`：
```bash
#!/bin/bash

# 快速发布脚本
VERSION=$1
CHANNEL=${2:-3001}

if [ -z "$VERSION" ]; then
    echo "Usage: ./release.sh <version> [channel]"
    echo "Example: ./release.sh 2.0.0.1 3001"
    exit 1
fi

echo "🚀 开始发布 EhViewer v$VERSION (渠道: $CHANNEL)"

# 1. 更新版本号
echo "📝 更新版本信息..."
# TODO: 自动更新 build.gradle.kts

# 2. 构建
echo "🔨 构建发布版本..."
./gradlew clean
./gradlew assembleRelease

# 3. 验证
echo "✅ 验证构建结果..."
APK_FILE="app/build/outputs/apk/appRelease/release/app-appRelease-release.apk"
if [ ! -f "$APK_FILE" ]; then
    echo "❌ APK 构建失败"
    exit 1
fi

# 4. Git 操作
echo "📦 创建版本提交和标签..."
git add .
git commit -m "Release v$VERSION: EhViewer 渠道$CHANNEL 正式发布"
git tag -a "v$VERSION" -m "Release v$VERSION: EhViewer TUTU 渠道$CHANNEL 正式版"

# 5. 推送
echo "🚀 推送到远程仓库..."
git push origin main
git push origin "v$VERSION"

echo "✅ 发布完成！"
echo "📁 APK 位置: $APK_FILE"
echo "🏷️  版本标签: v$VERSION"
```

## 质量检查清单

### 发布前检查
- [ ] 版本号正确更新
- [ ] 渠道代码正确设置 
- [ ] 代码已提交，工作目录干净
- [ ] APK 构建成功，大小合理
- [ ] 核心功能测试通过
- [ ] 发布说明已准备

### 发布后检查
- [ ] GitHub 提交推送成功
- [ ] 版本标签创建成功
- [ ] Release 页面更新
- [ ] APK 文件上传完成
- [ ] 下载链接可用
- [ ] 文档更新完成

## 常见问题处理

### 构建失败
1. **依赖问题**: `./gradlew clean` 后重新构建
2. **签名问题**: 检查 `debug.keystore` 文件存在
3. **内存不足**: 增加 Gradle 内存配置

### 版本冲突
1. **标签已存在**: 删除远程标签重新创建
   ```bash
   git tag -d v2.0.0.1
   git push origin :refs/tags/v2.0.0.1
   ```

### 推送失败
1. **权限问题**: 检查 GitHub 访问权限
2. **网络问题**: 重新尝试推送
3. **冲突解决**: 先拉取远程更改

## 版本历史

| 版本 | 发布日期 | 主要更新 |
|------|----------|----------|
| v2.0.0.1 | 2025-09-05 | 渠道3001正式发布，视频播放器增强 |
| v1.9.9.19 | 2025-09-04 | 浏览器功能修复和完善 |

## 联系方式

如有发布流程相关问题，请：
1. 查阅本文档
2. 检查 GitHub Issues
3. 联系项目维护者

---

*本文档持续更新，建议发布前检查最新版本*