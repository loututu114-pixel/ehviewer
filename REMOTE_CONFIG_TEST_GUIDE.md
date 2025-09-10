# 远程配置测试指南

## 概述

EhViewer 现在支持通过 GitHub 远程获取 `sou.json` 配置文件，实现动态配置搜索和首页功能。本文档介绍如何测试和验证远程配置功能。

## 配置文件位置

### GitHub 远程配置
- **主配置文件**: https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/sou.json
- **示例配置文件**: https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/sou_example.json

### 本地默认配置
- **位置**: `app/src/main/res/raw/sou.json`
- **用途**: 应用首次安装或网络不可用时的后备配置

## 测试步骤

### 1. 验证远程配置下载

#### 方法一：使用浏览器测试
1. 打开浏览器访问：https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/sou.json
2. 确认能正常访问并显示 JSON 内容
3. 检查 JSON 格式是否正确

#### 方法二：使用 curl 命令测试
```bash
# 测试主配置文件
curl -I https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/sou.json

# 测试完整下载
curl https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/sou.json
```

### 2. 验证应用内配置加载

#### 启动日志检查
1. 安装应用并启动
2. 查看 Android Studio 或设备日志
3. 查找以下关键日志：
```log
SearchConfigManager: Detected country code: CN
SearchConfigManager: Detected channel code: google
SearchConfigManager: Loaded local config
SearchConfigManager: Successfully downloaded config, size: XXX bytes
SearchConfigManager: Config updated successfully from remote
SearchConfigManager: Default homepage URL: https://www.baidu.com
```

#### 功能验证
1. **默认首页**: 应用启动后应该加载配置的默认首页
2. **搜索引擎**: 在地址栏输入关键词，应该使用配置的搜索引擎
3. **菜单选项**: 菜单中应该显示"🏠 设置首页"选项
4. **自定义首页**: 点击设置首页应该显示预设的快捷选项

### 3. 测试网络异常情况

#### 断网测试
1. 关闭设备网络连接
2. 重启应用
3. 确认应用能正常使用本地缓存的配置
4. 查看日志确认网络检查逻辑：
```log
SearchConfigManager: Network not available, skipping config update
```

#### 网络恢复测试
1. 恢复网络连接
2. 重启应用
3. 确认应用能下载并应用最新的远程配置

### 4. 测试配置更新

#### 修改远程配置
1. 在 GitHub 上修改 `sou.json` 文件
2. 提交并推送更改
3. 等待几分钟让 GitHub raw 内容更新
4. 在应用中触发配置更新：
   - 等待24小时自动更新
   - 或调用 `SearchConfigManager.getInstance(context).forceUpdate()`

#### 验证配置生效
1. 检查应用行为是否按新配置工作
2. 查看日志确认配置已更新

## 配置参数说明

### homepage 配置
```json
{
  "homepage": {
    "enabled": true,
    "url": "https://www.google.com",      // 默认首页URL
    "china": "https://www.baidu.com",     // 中国用户首页
    "customOptions": [                    // 自定义首页选项
      {
        "name": "新闻首页",
        "url": "https://news.google.com",
        "china": "https://news.baidu.com"
      }
    ]
  }
}
```

### 搜索引擎配置
```json
{
  "engines": {
    "google": {
      "name": "Google",
      "url": "https://www.google.com/search?q=%s",
      "suggest": "https://www.google.com/complete/search?client=firefox&q=%s"
    }
  }
}
```

## 缓存策略

### 缓存时间
- **定期更新**: 24小时检查一次远程配置
- **缓存有效期**: 7天，过期后强制更新
- **首次启动**: 立即尝试下载远程配置

### 缓存位置
- **SharedPreferences**: `search_config` 文件
- **键值**:
  - `config_json`: 配置JSON字符串
  - `last_update`: 最后更新时间戳

## 故障排除

### 常见问题

#### 1. 配置无法下载
**症状**: 日志显示下载失败
**解决**:
- 检查网络连接
- 确认 GitHub 访问权限
- 查看防火墙设置

#### 2. 配置格式错误
**症状**: 日志显示 JSON 解析失败
**解决**:
- 验证 `sou.json` 文件格式
- 使用在线 JSON 验证工具检查语法

#### 3. 缓存未更新
**症状**: 配置修改后应用未生效
**解决**:
- 等待24小时自动更新
- 强制清除应用数据重置缓存
- 或调用强制更新方法

#### 4. 网络权限问题
**症状**: Android 9+ 设备无法访问 HTTP
**解决**:
- 确保在 `AndroidManifest.xml` 中添加：
```xml
<application android:usesCleartextTraffic="true">
```

### 日志分析

#### 成功日志示例
```log
SearchConfigManager: Network available, attempting config update
SearchConfigManager: Successfully downloaded config, size: 1024 bytes
SearchConfigManager: Config updated successfully from remote
SearchConfigManager: Default homepage URL: https://www.baidu.com
```

#### 错误日志示例
```log
SearchConfigManager: Network not available, skipping config update
SearchConfigManager: Failed to download config, response code: 404
SearchConfigManager: Failed to parse downloaded config
```

## 性能优化

### 网络请求优化
- **超时设置**: 连接15秒，读取20秒
- **重试机制**: 最多3次重试，递增延迟
- **User-Agent**: 包含应用版本信息

### 缓存优化
- **本地缓存**: 避免重复网络请求
- **智能更新**: 只在必要时下载配置
- **内存缓存**: 运行时缓存解析后的配置对象

## 监控和统计

### 配置使用统计
应用会记录以下统计信息：
- 配置下载成功/失败次数
- 缓存命中率
- 用户选择的搜索引擎偏好
- 首页访问统计

### 日志级别
- **DEBUG**: 详细的配置加载过程
- **INFO**: 重要的配置更新事件
- **WARN**: 配置下载失败但不影响功能
- **ERROR**: 配置解析失败或严重错误

## 最佳实践

### 配置管理
1. **版本控制**: 为配置文件建立版本管理
2. **向后兼容**: 确保新配置兼容旧版本应用
3. **测试验证**: 在发布前充分测试配置更改
4. **回滚计划**: 准备配置回滚方案

### 网络策略
1. **离线优先**: 确保应用在无网络时正常工作
2. **智能重试**: 避免频繁失败的网络请求
3. **流量控制**: 合理控制配置下载频率

### 用户体验
1. **无缝更新**: 配置更新不中断用户使用
2. **渐进式**: 新功能逐步推出
3. **个性化**: 支持用户自定义配置偏好

## 技术支持

如果遇到配置相关问题，请提供以下信息：
- 设备型号和 Android 版本
- 网络环境（WiFi/移动数据）
- 完整错误日志
- 配置文件内容（脱敏后）
