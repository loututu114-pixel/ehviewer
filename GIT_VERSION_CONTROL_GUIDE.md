# 🔒 EhViewerh 项目 Git 版本控制安全指南

**版本**: v1.0  
**创建时间**: 2025-09-10  
**适用范围**: EhViewerh Android 项目  
**维护者**: 项目开发团队  

## 🎯 方案目标

确保每次版本更新时遵循安全规范，防止敏感信息泄露，建立标准化的发布流程。

---

## 🚨 安全风险评估

### 当前发现的安全问题

| 风险等级 | 问题描述 | 影响 | 状态 |
|---------|---------|------|-----|
| 🔴 **高危** | `gradle.properties` 包含签名密码明文 | 签名密钥泄露 | ⚠️ 待修复 |
| 🔴 **高危** | `google-services.json` 包含 Firebase API Key | 服务密钥泄露 | ⚠️ 待修复 |
| 🟡 **中危** | `@apk/` 目录包含发布包 | 存储空间浪费 | ⚠️ 待清理 |
| 🟡 **中危** | `release.keystore` 未被忽略 | 签名文件泄露风险 | ⚠️ 待修复 |

---

## 📋 标准操作检查清单

### 🔧 版本发布前检查清单

```bash
# ✅ 1. 安全检查
□ 确认敏感文件已被正确忽略
□ 验证 API 密钥不在代码中明文出现  
□ 检查签名文件未被跟踪
□ 确认个人信息和路径未泄露

# ✅ 2. 代码质量检查  
□ 运行代码检查工具 (lint/detekt)
□ 执行所有测试用例
□ 确认构建成功
□ 代码审查已完成

# ✅ 3. 版本信息更新
□ 更新版本号 (versionCode/versionName)
□ 更新 CHANGELOG.md 或 RELEASE_NOTES.md
□ 确认分支策略符合项目规范
□ 标签命名符合语义化版本规范

# ✅ 4. 提交前最终检查
□ git status 确认只提交必要文件
□ git diff 审查所有变更
□ 提交信息清晰描述变更内容
```

### 🚀 发布操作流程

```bash
# 步骤 1: 环境准备
git status                          # 检查工作区状态
git pull origin main               # 同步最新代码

# 步骤 2: 安全验证
./scripts/security_check.sh        # 运行安全检查脚本 (如果有)
git ls-files | grep -E "(key|secret|password)" # 检查敏感文件

# 步骤 3: 构建验证
./gradlew clean                    # 清理构建
./gradlew assembleRelease          # 构建发布版本
./gradlew test                     # 运行测试

# 步骤 4: 提交代码
git add .                          # 添加文件
git status                         # 再次确认
git commit -m "release: v2.0.0.6 - 功能描述"
git push origin main

# 步骤 5: 创建标签 (可选)
git tag -a v2.0.0.6 -m "Release v2.0.0.6"
git push origin v2.0.0.6
```

---

## ⚠️ 紧急修复方案

### 如果敏感信息已经提交到 Git

```bash
# 🚨 紧急处理: 移除敏感文件的历史记录
# ⚠️ 警告: 这会重写 Git 历史，团队成员需要重新克隆

# 1. 移除文件并删除历史
git filter-branch --force --index-filter \
'git rm --cached --ignore-unmatch gradle.properties' \
--prune-empty --tag-name-filter cat -- --all

# 2. 强制推送 (危险操作)
git push origin --force --all
git push origin --force --tags

# 3. 清理本地引用
git for-each-ref --format="delete %(refname)" refs/original | git update-ref --stdin
git reflog expire --expire=now --all
git gc --prune=now
```

### 替代安全方案 (推荐)

```bash
# 🔒 安全方案: 使用环境变量和模板文件

# 1. 创建模板文件
cp gradle.properties gradle.properties.template

# 2. 在模板文件中使用占位符
# RELEASE_STORE_PASSWORD={{KEYSTORE_PASSWORD}}
# RELEASE_KEY_PASSWORD={{KEY_PASSWORD}}

# 3. 在 .gitignore 中忽略实际文件，只提交模板
echo "gradle.properties" >> .gitignore
git add gradle.properties.template
```

---

## 📁 文件分类详细规范

### ✅ **必须提交的文件**

```
核心源码:
app/src/main/java/**/*.java
app/src/main/java/**/*.kt
app/src/main/res/**/*.xml
app/src/main/assets/**/*

构建配置:
build.gradle
app/build.gradle
settings.gradle
gradle/wrapper/gradle-wrapper.properties
gradlew, gradlew.bat

项目文档:
README.md
CHANGELOG.md
CONTRIBUTING.md
LICENSE
docs/**/*.md
```

### 🔒 **绝对禁止提交的文件**

```
安全凭据:
*.keystore, *.jks
google-services.json
gradle.properties (如包含密码)
local.properties
api_keys.xml
any_secrets.json

构建产物:
*.apk, *.aab
build/, app/build/
.gradle/
@apk/, release/

个人环境:
.idea/ (IDE 配置)
.vscode/settings.json
.DS_Store, Thumbs.db
*.log, *.tmp
```

### 🤔 **条件性提交的文件**

```
配置文件 (需要审查):
gradle.properties -> 需要移除敏感信息后提交
proguard-rules.pro -> 可以提交
lint.xml -> 可以提交

测试文件:
app/src/test/ -> 应该提交
app/src/androidTest/ -> 应该提交
test_*.py, test_*.java -> 临时测试文件不提交

文档:
自动生成的文档 -> 不提交
手写的项目文档 -> 应该提交
```

---

## 🛠️ 实施步骤

### 立即执行 (高优先级)

1. **应用新的 .gitignore 配置**
```bash
# 备份当前配置
cp .gitignore .gitignore.backup

# 应用新配置  
cp .gitignore.new .gitignore

# 清理已跟踪的敏感文件
git rm --cached gradle.properties
git rm --cached app/google-services.json  
git rm --cached app/release.keystore
```

2. **创建安全的配置文件模板**
```bash
# 创建模板文件
cp gradle.properties gradle.properties.template

# 编辑模板，将敏感信息替换为占位符
# RELEASE_STORE_PASSWORD={{KEYSTORE_PASSWORD}}
```

### 中期优化 (1-2周内)

1. **建立自动化检查脚本**
2. **配置 GitHub Actions 进行安全扫描** 
3. **建立团队培训机制**

### 长期改进 (1个月内)

1. **集成密钥管理系统**
2. **建立完整的 CI/CD 流程**
3. **定期安全审计**

---

## 🔍 定期审查机制

### 月度检查项目

- [ ] 审查 .gitignore 规则是否需要更新
- [ ] 检查是否有新的敏感文件类型
- [ ] 验证团队成员是否遵循规范
- [ ] 评估安全风险和改进建议

### 版本发布审查

- [ ] 确认所有敏感信息已被正确处理
- [ ] 验证构建产物不包含调试信息
- [ ] 检查依赖项是否存在安全漏洞
- [ ] 确认发布包符合应用商店要求

---

## 🆘 应急联系和支持

| 情况 | 联系方式 | 处理时间 |
|-----|---------|----------|
| 敏感信息泄露 | 项目负责人 | 立即 |
| 构建失败 | 技术支持 | 2小时内 |
| 版本发布问题 | 发布团队 | 4小时内 |

---

## 📊 合规性和最佳实践

遵循以下安全标准:
- ✅ OWASP Mobile Security Guidelines
- ✅ Google Play 安全政策  
- ✅ Android 开发最佳实践
- ✅ Git 安全操作规范

---

## 📝 变更日志

| 版本 | 日期 | 变更内容 | 作者 |
|-----|------|---------|------|
| v1.0 | 2025-09-10 | 初始版本创建 | AI Assistant |

---

**⚠️ 重要提醒**: 此指南是项目安全的重要组成部分，请所有团队成员严格遵守。如有疑问或建议，请及时与项目负责人沟通。