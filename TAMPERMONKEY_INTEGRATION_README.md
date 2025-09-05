# EhViewer 油猴脚本集成说明

## 概述

EhViewer 现已内置完整的油猴（Tampermonkey）功能，支持用户脚本的安装、管理和自动更新，为用户提供强大的网页定制能力。

## 主要功能

### 🚀 核心特性
- ✅ **用户脚本管理**：安装、启用/禁用、卸载用户脚本
- ✅ **JavaScript API 支持**：完整实现Greasemonkey API (GM_*)
- ✅ **自动更新**：支持脚本自动检查和更新
- ✅ **脚本编辑器**：内置脚本编辑功能
- ✅ **脚本导出**：支持将脚本导出到外部存储
- ✅ **安全隔离**：脚本运行在安全的沙箱环境中

### 🔧 支持的GM API
- `GM_getValue(key, defaultValue)` - 获取存储的值
- `GM_setValue(key, value)` - 存储值
- `GM_deleteValue(key)` - 删除存储的值
- `GM_addStyle(css)` - 添加CSS样式
- `GM_log(message)` - 输出日志
- `GM_xmlhttpRequest(details)` - 发送XMLHttpRequest

## 如何使用

### 1. 访问脚本管理
1. 在浏览器中点击设置按钮（⚙️）
2. 选择"🐒 用户脚本管理"
3. 在脚本管理界面中进行各种操作

### 2. 安装脚本
- **方法一**：点击"➕ 安装新脚本"，粘贴脚本内容或输入脚本URL
- **方法二**：从外部文件导入脚本

### 3. 管理脚本
- ✅ 启用/禁用脚本
- 📝 查看脚本详情
- ✏️ 编辑脚本内容
- 📤 导出脚本到下载文件夹
- 🗑️ 卸载脚本

### 4. 脚本设置
- 🔄 检查脚本更新
- ⚙️ 配置脚本相关设置
- 📊 查看存储使用情况

## 脚本格式要求

用户脚本必须遵循标准的Tampermonkey格式：

```javascript
// ==UserScript==
// @name         脚本名称
// @namespace    脚本命名空间
// @version      1.0.0
// @description  脚本描述
// @author       作者名称
// @match        *://*/*
// @grant        GM_addStyle
// @grant        GM_log
// ==/UserScript==

(function() {
    'use strict';

    // 脚本内容
    GM_log('脚本已加载');

    // 示例：隐藏页面元素
    GM_addStyle('.annoying-element { display: none !important; }');

})();
```

## 脚本匹配规则

支持以下匹配模式：
- `@match *://*/*` - 匹配所有网站
- `@match https://example.com/*` - 匹配特定域名
- `@match https://*.example.com/*` - 匹配子域名
- `@include /pattern/` - 正则表达式匹配
- `@exclude pattern` - 排除匹配

## 安全注意事项

### 🔒 安全特性
- 脚本运行在独立的JavaScript上下文中
- API调用受到严格限制
- 不允许访问系统资源
- 支持脚本权限控制

### ⚠️ 使用建议
- 只安装来自可信任来源的脚本
- 定期检查脚本更新
- 注意脚本的权限要求
- 避免运行未知或可疑的脚本

## 示例脚本

项目包含一个示例广告拦截脚本（`example_userscript.js`），演示了：
- 如何使用GM_addStyle添加CSS样式
- 如何监听DOM变化
- 如何动态隐藏页面元素

## 技术实现

### 核心组件
1. **UserScriptManager** - 脚本管理器
2. **UserScriptParser** - 脚本解析器
3. **ScriptInjector** - 脚本注入器
4. **ScriptStorage** - 脚本存储管理
5. **ScriptUpdater** - 脚本更新器

### 架构特点
- 🏗️ 模块化设计，易于扩展
- 💾 本地存储，支持脚本持久化
- 🔄 异步更新，不阻塞UI
- 🛡️ 错误处理，防止脚本崩溃影响应用

## 常见问题

### Q: 脚本不生效怎么办？
A: 检查脚本的`@match`规则是否正确匹配当前网站，确认脚本已启用。

### Q: 如何调试脚本？
A: 使用`GM_log()`输出调试信息，或在浏览器开发者工具中查看控制台。

### Q: 脚本更新失败怎么办？
A: 检查网络连接，确保脚本的`@updateURL`字段正确设置。

### Q: 可以同时运行多个脚本吗？
A: 是的，系统支持同时运行多个用户脚本。

## 更新日志

### v1.0.0 (当前版本)
- ✅ 实现完整的Tampermonkey功能
- ✅ 支持GM API
- ✅ 脚本管理界面
- ✅ 自动更新机制
- ✅ 脚本导入导出

## 贡献

欢迎提交用户脚本或功能改进建议！

## 许可证

本功能基于Apache License 2.0开源协议。
