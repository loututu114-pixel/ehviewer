# 默认首页配置说明

## 概述

EhViewer 现在支持通过远程配置文件动态配置默认首页，可以根据国家和渠道提供不同的首页体验。

## 配置文件结构

### sou.json 配置格式

```json
{
  "version": "1.1.0",
  "lastUpdate": "2024-01-01T00:00:00Z",
  "searchConfig": {
    "homepage": {
      "enabled": true,
      "url": "https://www.google.com",
      "china": "https://www.baidu.com",
      "title": "默认首页",
      "customOptions": [
        {
          "name": "新闻首页",
          "url": "https://news.google.com",
          "china": "https://news.baidu.com"
        }
      ]
    }
  }
}
```

### 配置参数说明

#### homepage 对象
- `enabled`: 是否启用默认首页功能 (boolean)
- `url`: 默认首页URL (string)
- `china`: 中国用户使用的首页URL (string)
- `title`: 首页标题 (string)
- `customOptions`: 自定义首页选项数组 (array)

#### customOptions 数组
每个选项包含：
- `name`: 显示名称 (string)
- `url`: 默认URL (string)
- `china`: 中国用户URL (string，可选)

## 功能特性

### 1. 智能国家检测
- 自动检测用户国家代码
- 中国用户 (CN, HK, TW, MO, SG, MY) 使用百度等本土服务
- 其他国家使用Google等国际服务

### 2. 渠道优先级
- 支持不同应用渠道配置不同首页
- xiaomi、huawei、oppo、vivo 等渠道可配置专用首页

### 3. 动态更新
- 24小时自动检查远程配置更新
- 无需应用更新即可调整首页配置
- 本地缓存保证离线可用

### 4. 用户界面
- 菜单中"🏠 设置首页"选项
- 显示当前默认首页
- 提供预设的快捷首页选项
- 支持设置当前页面为首页

## 使用方法

### 1. 远程配置
将 `sou.json` 文件上传到 GitHub 或其他可访问的服务器：
```
https://raw.githubusercontent.com/your-username/your-repo/main/sou.json
```

### 2. 本地配置
修改 `app/src/main/res/raw/sou.json` 文件中的 homepage 配置

### 3. 运行时配置
应用会自动：
- 检测用户国家和渠道
- 下载并应用远程配置
- 选择最合适的首页

## 示例配置

### 基础配置
```json
{
  "homepage": {
    "enabled": true,
    "url": "https://www.google.com",
    "china": "https://www.baidu.com"
  }
}
```

### 高级配置
```json
{
  "homepage": {
    "enabled": true,
    "url": "https://www.google.com",
    "china": "https://www.baidu.com",
    "customOptions": [
      {
        "name": "新闻",
        "url": "https://news.google.com",
        "china": "https://news.baidu.com"
      },
      {
        "name": "购物",
        "url": "https://www.amazon.com",
        "china": "https://www.taobao.com"
      }
    ]
  }
}
```

## 创收机制

### 1. 渠道广告
```json
{
  "ads": {
    "enabled": true,
    "channels": ["xiaomi", "huawei"],
    "homepageAds": {
      "enabled": true,
      "position": "top",
      "providers": ["baidu"]
    }
  }
}
```

### 2. 搜索广告
```json
{
  "ads": {
    "searchAds": {
      "enabled": true,
      "keywords": ["购物", "买", "价格"],
      "providers": ["baidu", "google"]
    }
  }
}
```

## 技术实现

### SearchConfigManager
- 单例模式管理配置
- 异步下载远程配置
- 本地缓存机制
- 国家/渠道检测

### WebViewActivity
- 集成 SearchConfigManager
- 智能首页加载
- 用户界面交互

## 注意事项

1. **网络权限**: 确保应用有网络权限下载远程配置
2. **缓存策略**: 本地缓存保证离线可用
3. **版本控制**: 使用 version 字段管理配置版本
4. **向后兼容**: 支持旧版本配置格式

## 更新日志

### v1.1.0
- ✅ 新增默认首页配置功能
- ✅ 支持国家/渠道智能选择
- ✅ 添加自定义首页选项
- ✅ 支持远程配置更新
- ✅ 集成广告创收机制

### v1.0.0
- ✅ 基础搜索功能
- ✅ 多搜索引擎支持
- ✅ URL智能识别
