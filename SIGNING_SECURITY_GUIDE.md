# 🔐 EhViewerh 签名配置安全指南

## 🚨 重要安全警告

签名文件和密码是 Android 应用发布的核心安全资产，一旦泄露将导致严重后果：
- ✅ 恶意者可以发布伪造的应用更新
- ✅ 用户设备可能被恶意软件感染
- ✅ 应用商店账户可能被劫持

## 📋 安全配置清单

### ✅ 已完成的安全措施
- [x] 签名文件已从 Git 跟踪中移除
- [x] gradle.properties 已从版本控制中移除
- [x] 创建了安全的配置模板
- [x] 更新了 .gitignore 防止意外提交

### 🔧 配置步骤

#### 1. 恢复配置文件
```bash
# 复制模板文件
cp gradle.properties.template gradle.properties
cp hippo-library/main-base/gradle.properties.template hippo-library/main-base/gradle.properties
```

#### 2. 填写实际配置
编辑 `gradle.properties`，替换占位符：
```properties
# 将这些占位符替换为实际值：
RELEASE_STORE_PASSWORD={{KEYSTORE_PASSWORD}}  -> 您的实际密码
RELEASE_KEY_ALIAS={{KEY_ALIAS}}              -> 您的密钥别名  
RELEASE_KEY_PASSWORD={{KEY_PASSWORD}}        -> 您的密钥密码
```

#### 3. 验证签名文件
确保以下文件存在且未被 Git 跟踪：
- `app/release.keystore` - 正式版签名文件
- `debug.keystore` - 调试版签名文件（如果有）

## 🔒 最佳实践建议

### 本地开发环境
```bash
# 方式1: 使用环境变量（推荐）
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="ehviewer_release" 
export KEY_PASSWORD="your_key_password"

# 在 gradle.properties 中引用：
RELEASE_STORE_PASSWORD=${KEYSTORE_PASSWORD}
RELEASE_KEY_ALIAS=${KEY_ALIAS}
RELEASE_KEY_PASSWORD=${KEY_PASSWORD}
```

### 生产环境/CI-CD
```yaml
# GitHub Actions 示例
env:
  KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
  KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
  KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

## 🆘 应急处理

### 如果密码已经泄露
1. **立即更换密码**：
   ```bash
   keytool -storepasswd -keystore app/release.keystore
   keytool -keypasswd -alias ehviewer_release -keystore app/release.keystore
   ```

2. **撤销已发布的版本**（如果可能）

3. **生成新的签名文件**：
   ```bash
   keytool -genkey -v -keystore new_release.keystore -alias ehviewer_new \
           -keyalg RSA -keysize 2048 -validity 10000
   ```

### 密钥备份策略
- ✅ 将签名文件存储在至少2个安全位置
- ✅ 使用强密码和多因素认证
- ✅ 定期验证备份的完整性
- ✅ 记录密钥的创建和使用历史

## 🔍 安全检查脚本

创建定期检查脚本 `security_check.sh`：
```bash
#!/bin/bash
echo "🔍 EhViewerh 安全检查..."

# 检查敏感文件是否被 Git 跟踪
echo "检查签名文件..."
if git ls-files | grep -q "\.keystore\|\.jks"; then
    echo "❌ 发现签名文件被跟踪！"
    exit 1
fi

# 检查配置文件
echo "检查配置文件..."
if git ls-files | grep -q "gradle\.properties$"; then
    echo "❌ 发现配置文件被跟踪！"
    exit 1
fi

# 检查明文密码
echo "检查明文密码..."
if grep -r "EhViewer2025!" --include="*.properties" --include="*.gradle" .; then
    echo "❌ 发现明文密码！"
    exit 1
fi

echo "✅ 安全检查通过！"
```

## 📞 紧急联系信息

| 安全事件类型 | 联系方式 | 响应时间 |
|-------------|---------|---------|
| 密钥泄露 | 项目负责人 | 立即 |
| 签名问题 | 技术团队 | 2小时内 |
| 发布阻塞 | 发布团队 | 4小时内 |

## 📊 合规性检查

定期审查以下项目：
- [ ] 签名文件访问权限
- [ ] 密码复杂度和更新频率  
- [ ] 备份和恢复流程
- [ ] 团队成员访问记录

---

**⚠️ 记住：安全无小事，密钥管理是应用安全的第一道防线！**