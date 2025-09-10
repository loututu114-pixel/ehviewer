[根目录](../../CLAUDE.md) > [app](../../..) > [src](..) > [main](.) > **assets**

# 用户脚本资源模块文档

> EhViewer内置的用户脚本系统，提供20+个预置脚本用于网站功能增强和广告拦截

## 模块职责

Assets模块包含EhViewer的用户脚本资源，主要功能：
- **广告拦截**: 通用和特定网站的广告拦截脚本
- **站点优化**: 针对特定网站的功能增强脚本
- **媒体增强**: 视频播放器和图片浏览优化
- **应用拦截**: 防止网页强制跳转到应用商店
- **用户体验**: 界面美化和交互优化
- **安全防护**: 恶意网站和内容拦截

## 脚本清单与功能

### 广告拦截类脚本

| 脚本文件 | 功能描述 | 适用范围 | 更新频率 |
|---------|---------|---------|---------|
| `universal_ad_blocker.js` | 通用广告拦截脚本 | 所有网站 | 经常更新 |
| `baidu_app_blocker.js` | 百度应用跳转拦截 | 百度系网站 | 定期更新 |
| `enhanced_app_blocker.js` | 增强应用拦截器 | 移动网站 | 定期更新 |
| `floating_ad_blocker.js` | 浮动广告拦截器 | 通用网站 | 定期更新 |
| `super_app_blocker.js` | 超级应用拦截器 | 所有网站 | 经常更新 |

### 站点优化类脚本

| 脚本文件 | 功能描述 | 目标网站 | 主要功能 |
|---------|---------|---------|---------|
| `bilibili_enhancer.js` | B站功能增强 | bilibili.com | 播放优化、界面美化 |
| `zhihu_enhancer.js` | 知乎体验优化 | zhihu.com | 内容过滤、阅读优化 |
| `twitter_enhancer.js` | Twitter增强器 | twitter.com | 时间线优化、功能增强 |
| `chinese_sites_optimizer.js` | 中文站点优化器 | 中文网站 | 字体、排版优化 |
| `youth_sites_optimizer.js` | 青年网站优化器 | 青年向网站 | 内容筛选、界面优化 |

### 媒体增强类脚本

| 脚本文件 | 功能描述 | 适用场景 | 核心特性 |
|---------|---------|---------|---------|
| `video_player_enhancer.js` | 视频播放增强器 | 视频网站 | 播放控制、画质优化 |
| `enhanced_video_player.js` | 增强视频播放器 | 所有视频 | 快捷键、手势控制 |
| `image_optimizer.js` | 图片优化器 | 图片网站 | 加载优化、缩放控制 |
| `novel_reader_enhancer.js` | 小说阅读增强器 | 小说网站 | 阅读模式、字体优化 |
| `novel_drama_enhancer.js` | 小说剧集增强器 | 文学网站 | 章节导航、收藏管理 |

### 功能增强类脚本

| 脚本文件 | 功能描述 | 应用范围 | 实用功能 |
|---------|---------|---------|---------|
| `mobile_adaptation_enhancer.js` | 移动端适配增强 | 移动网站 | 触摸优化、布局调整 |
| `download_sites_optimizer.js` | 下载站点优化器 | 下载网站 | 链接净化、下载优化 |
| `adult_site_enhancer.js` | 成人网站增强器 | 成人内容 | 隐私保护、广告过滤 |
| `malware_blocker.js` | 恶意软件拦截器 | 所有网站 | 安全防护、风险提示 |

### 系统集成脚本

| 脚本文件 | 功能描述 | 集成层级 | 技术特点 |
|---------|---------|---------|---------|
| `requestOverride.js` | 请求重写脚本 | 网络层 | 请求拦截、重定向 |
| `example_userscript.js` | 示例用户脚本 | 开发参考 | 模板代码、最佳实践 |

## 脚本架构与API

### 脚本头部规范
```javascript
// ==UserScript==
// @name         脚本名称
// @namespace    http://tampermonkey.net/
// @version      版本号
// @description  功能描述
// @author       作者信息
// @match        *://*/*        // 匹配规则
// @grant        GM_addStyle    // 权限声明
// @grant        GM_log
// ==/UserScript==
```

### Tampermonkey兼容API
```javascript
// 样式注入API
GM_addStyle(css) {
    const style = document.createElement('style');
    style.textContent = css;
    document.head.appendChild(style);
}

// 日志输出API
GM_log(message) {
    console.log('[UserScript]', message);
}

// 值存储API
GM_setValue(key, value) {
    localStorage.setItem('GM_' + key, JSON.stringify(value));
}

GM_getValue(key, defaultValue) {
    const stored = localStorage.getItem('GM_' + key);
    return stored ? JSON.parse(stored) : defaultValue;
}
```

### 脚本注入机制
```java
// Java端注入逻辑
public class UserScriptManager {
    public void injectScript(WebView webView, String scriptContent) {
        // 在页面加载完成后注入脚本
        webView.evaluateJavascript(scriptContent, null);
    }
    
    public boolean shouldInjectScript(String url, UserScript script) {
        // 根据@match规则判断是否应该注入
        return script.matches(url);
    }
}
```

## 脚本示例分析

### 通用广告拦截脚本
```javascript
// universal_ad_blocker.js 核心逻辑
(function() {
    'use strict';
    
    // 广告选择器列表
    const adSelectors = [
        '.ads', '.advertisement', '.ad-banner',
        '.ad-container', '.popup-ad', '.overlay-ad',
        '[class*="ad-"]', '[id*="ad-"]',
        '[class*="advert"]', '[id*="advert"]'
    ];
    
    // 移除广告元素
    function removeAds() {
        adSelectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(el => {
                el.remove();
            });
        });
    }
    
    // 页面加载完成后执行
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', removeAds);
    } else {
        removeAds();
    }
    
    // 监听动态内容
    const observer = new MutationObserver(removeAds);
    observer.observe(document.body, { childList: true, subtree: true });
})();
```

### 视频播放增强脚本
```javascript
// video_player_enhancer.js 核心功能
(function() {
    'use strict';
    
    // 增强视频播放器
    function enhanceVideoPlayer(video) {
        // 添加快捷键支持
        document.addEventListener('keydown', function(e) {
            if (e.target.tagName.toLowerCase() !== 'input') {
                switch(e.key) {
                    case ' ':
                        e.preventDefault();
                        video.paused ? video.play() : video.pause();
                        break;
                    case 'ArrowLeft':
                        video.currentTime -= 10;
                        break;
                    case 'ArrowRight':
                        video.currentTime += 10;
                        break;
                }
            }
        });
        
        // 添加播放速度控制
        const speedControl = document.createElement('div');
        speedControl.innerHTML = `
            <button onclick="this.parentElement.video.playbackRate = 0.5">0.5x</button>
            <button onclick="this.parentElement.video.playbackRate = 1.0">1.0x</button>
            <button onclick="this.parentElement.video.playbackRate = 1.5">1.5x</button>
            <button onclick="this.parentElement.video.playbackRate = 2.0">2.0x</button>
        `;
        speedControl.video = video;
        video.parentElement.appendChild(speedControl);
    }
    
    // 监听视频元素
    document.querySelectorAll('video').forEach(enhanceVideoPlayer);
    
    // 监听新增的视频元素
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            mutation.addedNodes.forEach(function(node) {
                if (node.nodeType === 1) {
                    const videos = node.querySelectorAll ? 
                        node.querySelectorAll('video') : [];
                    videos.forEach(enhanceVideoPlayer);
                }
            });
        });
    });
    
    observer.observe(document.body, { childList: true, subtree: true });
})();
```

## 脚本管理系统

### 脚本加载流程
```
1. WebView初始化
   ↓
2. 页面开始加载
   ↓  
3. 检查URL匹配规则
   ↓
4. 选择适用的脚本
   ↓
5. 在适当时机注入脚本
   ↓
6. 执行脚本功能
   ↓
7. 监听页面变化
   ↓
8. 动态调整脚本行为
```

### 脚本冲突处理
```javascript
// 脚本命名空间隔离
(function(global) {
    'use strict';
    
    // 创建独立的脚本作用域
    const scriptScope = {
        name: 'UniversalAdBlocker',
        version: '1.0.0',
        
        // 检查是否已经加载
        isLoaded: function() {
            return global._userScripts && 
                   global._userScripts[this.name];
        },
        
        // 注册脚本
        register: function() {
            if (!global._userScripts) {
                global._userScripts = {};
            }
            global._userScripts[this.name] = this;
        }
    };
    
    // 避免重复加载
    if (scriptScope.isLoaded()) {
        return;
    }
    
    // 注册并执行脚本
    scriptScope.register();
    // ... 脚本主要逻辑
    
})(window);
```

## 性能优化

### 脚本执行优化
- **延迟加载**: 页面关键内容加载完成后再执行
- **节流处理**: 限制DOM操作频率避免性能问题
- **内存管理**: 及时清理事件监听器和定时器
- **选择器优化**: 使用高效的CSS选择器减少查询时间

### 兼容性处理
```javascript
// 特性检测和降级处理
function addEventListenerSafe(element, event, handler) {
    if (element.addEventListener) {
        element.addEventListener(event, handler);
    } else if (element.attachEvent) {
        element.attachEvent('on' + event, handler);
    } else {
        element['on' + event] = handler;
    }
}

// 现代API的兼容性处理
const querySelectorAllSafe = function(selector) {
    if (document.querySelectorAll) {
        return document.querySelectorAll(selector);
    } else {
        // 降级到jQuery或其他方案
        return $(selector).get();
    }
};
```

## 测试与调试

### 脚本测试方法
```javascript
// 测试工具函数
const ScriptTester = {
    // 模拟DOM环境
    mockDOM: function(html) {
        document.body.innerHTML = html;
    },
    
    // 断言函数
    assert: function(condition, message) {
        if (!condition) {
            console.error('Test failed:', message);
        } else {
            console.log('Test passed:', message);
        }
    },
    
    // 性能测试
    timeTest: function(fn, name) {
        const start = performance.now();
        fn();
        const end = performance.now();
        console.log(`${name} took ${end - start} ms`);
    }
};

// 测试用例示例
function testAdBlocker() {
    ScriptTester.mockDOM(`
        <div class="ads">广告内容</div>
        <div class="content">正常内容</div>
    `);
    
    // 执行广告拦截逻辑
    removeAds();
    
    // 验证结果
    ScriptTester.assert(
        document.querySelectorAll('.ads').length === 0,
        'Ads should be removed'
    );
}
```

## 常见问题 (FAQ)

### Q: 脚本不生效或执行失败？
A:
1. 检查@match规则是否正确匹配URL
2. 确认脚本语法没有错误
3. 查看控制台错误日志
4. 验证权限声明是否足够

### Q: 脚本冲突导致页面功能异常？
A:
1. 使用命名空间隔离脚本作用域
2. 检查是否修改了全局对象
3. 避免覆盖原生DOM方法
4. 使用更精确的CSS选择器

### Q: 脚本性能问题影响页面加载？
A:
1. 优化DOM查询频率和范围
2. 使用事件代理减少监听器数量
3. 避免在循环中进行DOM操作
4. 实现节流和防抖机制

### Q: 如何添加新的用户脚本？
A:
1. 在assets目录创建新的.js文件
2. 添加标准的UserScript头部注释
3. 实现具体的功能逻辑
4. 在应用中配置脚本加载规则

## 相关文件清单

### 用户脚本文件
```
app/src/main/assets/
├── universal_ad_blocker.js        # 通用广告拦截
├── baidu_app_blocker.js          # 百度应用拦截
├── enhanced_app_blocker.js       # 增强应用拦截
├── floating_ad_blocker.js        # 浮动广告拦截
├── super_app_blocker.js          # 超级应用拦截
├── bilibili_enhancer.js          # B站功能增强
├── zhihu_enhancer.js             # 知乎体验优化
├── twitter_enhancer.js           # Twitter增强器
├── video_player_enhancer.js      # 视频播放增强
├── enhanced_video_player.js      # 增强视频播放器
├── image_optimizer.js            # 图片优化器
├── novel_reader_enhancer.js      # 小说阅读增强
├── mobile_adaptation_enhancer.js # 移动端适配
├── download_sites_optimizer.js   # 下载站点优化
├── chinese_sites_optimizer.js    # 中文站点优化
├── youth_sites_optimizer.js      # 青年网站优化
├── adult_site_enhancer.js        # 成人网站增强
├── malware_blocker.js            # 恶意软件拦截
├── novel_drama_enhancer.js       # 小说剧集增强
├── requestOverride.js            # 请求重写脚本
└── example_userscript.js         # 示例用户脚本
```

### 脚本管理相关
```
app/src/main/java/com/hippo/ehviewer/userscript/
├── UserScriptManager.java        # 用户脚本管理器
├── ScriptInjector.java           # 脚本注入器  
├── ScriptMatcher.java            # 脚本匹配器
└── UserScript.java               # 脚本对象模型
```

## 变更记录 (Changelog)

### 2025-09-06 03:01:06 - 用户脚本资源文档初始化
- 创建用户脚本模块文档
- 整理20+个预置脚本的功能和用途
- 添加脚本架构和API说明
- 补充性能优化和兼容性处理
- 完善测试方法和调试指南

---

*本文档自动生成 - 最后更新: 2025-09-06 03:01:06*