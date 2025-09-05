// ==UserScript==
// @name         EhViewer 微信支付宝专用拦截器
// @namespace    http://ehviewer.com/
// @version      2.2.0
// @description  仅支持微信和支付宝的APP拦截，支持安全的支付功能
// @author       EhViewer Team
// @match        *://*/*
// @exclude      *://*.google.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 微信支付宝专用拦截器已启动');

    // 配置选项 - 增强版默认设置
    const config = {
        enabled: GM_getValue('appInterceptEnabled', true), // 默认启用
        blockSchemes: GM_getValue('blockSchemes', true), // 默认拦截Scheme
        blockJavaScript: GM_getValue('blockJavaScript', true), // 默认拦截JavaScript
        blockDownloadPrompts: GM_getValue('blockDownloadPrompts', true), // 默认拦截下载提示
        blockAppBanners: GM_getValue('blockAppBanners', true), // 默认拦截横幅
        showNotifications: GM_getValue('showNotifications', false), // 默认关闭通知（避免过多弹窗）
        logBlocked: GM_getValue('logBlocked', false), // 默认关闭日志
        aggressiveMode: GM_getValue('appInterceptAggressiveMode', true), // 新增：激进模式
        blockDynamicContent: GM_getValue('appInterceptBlockDynamic', true), // 新增：拦截动态内容
        customRules: GM_getValue('customRules', [])
    };

    // 仅支持微信和支付宝的Scheme映射表
    const appSchemes = {
        // 微信相关
        'wechat': ['weixin://', 'com.tencent.mm://', 'wechat://', 'wework://'],
        'wechatpay': ['wxpay://', 'com.tencent.wechatpay://'],
        // 支付宝相关
        'alipay': ['alipay://', 'com.eg.android.AlipayGphone://', 'alipayqr://']
    };

    // 简化的通用拦截规则 - 仅支持微信和支付宝
    const siteSpecificRules = {
        'default': {
            selectors: {
                appDownload: '.app-download, .download-app, .app-banner, .download-banner',
                schemeLinks: 'a[href*="weixin://"], a[href*="alipay://"], a[href*="wxpay://"]',
                jsHooks: ['openWeChat', 'openAlipay', 'wechatApp', 'alipayApp'],
                banners: '.app-popup, .download-popup, .app-modal, .download-modal'
            },
            patterns: [
                /weixin:\/\/[^"'\s]+/g,
                /alipay:\/\/[^"'\s]+/g,
                /wxpay:\/\/[^"'\s]+/g,
                /window\.(openWeChat|openAlipay|wechatApp|alipayApp)\([^)]+\)/g
            ]
        }
        'weibo.com': {
            selectors: {
                appDownload: '.app-download, .weibo-app, .download-banner',
                schemeLinks: 'a[href*="weibo://"], a[href*="sinaweibo://"]',
                jsHooks: ['openWeibo', 'weiboApp', 'sinaWeibo'],
                banners: '.app-promotion, .download-modal'
            },
            patterns: [
                /weibo:\/\/[^"'\s]+/g,
                /sinaweibo:\/\/[^"'\s]+/g,
                /window\.openWeibo\([^)]+\)/g,
                /location\.href\s*=\s*['"`]weibo:\/\/[^'"`]+['"`]/g
            ]
        },
        'bilibili.com': {
            selectors: {
                appDownload: '.app-download, .bili-app, .download-banner',
                schemeLinks: 'a[href*="bilibili://"], a[href*="com.bilibili.app"]',
                jsHooks: ['openBiliApp', 'biliApp', 'bililive'],
                banners: '.app-promotion, .download-modal, .app-popup'
            },
            patterns: [
                /bilibili:\/\/[^"'\s]+/g,
                /com\.bilibili\.app:\/\/[^"'\s]+/g,
                /window\.openBiliApp\([^)]+\)/g,
                /location\.href\s*=\s*['"`]bilibili:\/\/[^'"`]+['"`]/g
            ]
        }
    };

    // 创建APP拦截控制面板
    function createInterceptPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-app-intercept-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #ff4757, #ff3838); color: white;
                        padding: 10px; border-radius: 8px; font-size: 11px; font-family: Arial;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3); min-width: 180px;">
                <div style="display: flex; align-items: center; margin-bottom: 6px;">
                    <span style="font-size: 14px; margin-right: 6px;">🚫</span>
                    <strong>APP拦截器</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 3px; margin-bottom: 6px;">
                    <button id="block-schemes" title="拦截Scheme">🔗Scheme</button>
                    <button id="block-js" title="拦截JS">📜JS</button>
                    <button id="block-downloads" title="拦截下载">📥下载</button>
                    <button id="block-banners" title="拦截横幅">📢横幅</button>
                    <button id="show-stats" title="拦截统计">📊统计</button>
                    <button id="settings-btn" title="设置">⚙️</button>
                </div>
                <div style="font-size: 8px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 3px;">
                    已拦截: <span id="blocked-count">0</span> 次
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('block-schemes').onclick = () => toggleSchemeBlocking();
        document.getElementById('block-js').onclick = () => toggleJavaScriptBlocking();
        document.getElementById('block-downloads').onclick = () => toggleDownloadBlocking();
        document.getElementById('block-banners').onclick = () => toggleBannerBlocking();
        document.getElementById('show-stats').onclick = () => showInterceptStats();
        document.getElementById('settings-btn').onclick = showInterceptSettings();
    }

    // 检测当前网站规则
    function detectSiteRules() {
        const hostname = window.location.hostname.toLowerCase();

        for (const [domain, rules] of Object.entries(siteSpecificRules)) {
            if (hostname.includes(domain.replace('www.', '').replace('.com', ''))) {
                return rules;
            }
        }

        // 返回通用规则
        return {
            selectors: {
                appDownload: '.app-download, .download-app, .app-banner, .download-banner',
                schemeLinks: 'a[href*="://"]',
                jsHooks: ['openApp', 'launchApp', 'downloadApp', 'callApp'],
                banners: '.app-popup, .download-popup, .app-modal, .download-modal'
            },
            patterns: [
                /[a-zA-Z0-9]+\:\/\/[^"'\s]+/g,
                /window\.(openApp|launchApp|downloadApp|callApp)\([^)]+\)/g,
                /location\.href\s*=\s*['"`][a-zA-Z0-9]+\:\/\/[^'"`]+['"`]/g
            ]
        };
    }

    // 拦截Scheme URL跳转
    function toggleSchemeBlocking() {
        if (!config.blockSchemes) return;

        // 拦截所有APP Scheme链接
        const allSchemes = Object.values(appSchemes).flat();
        const schemePattern = new RegExp(`^(${allSchemes.join('|').replace(/:/g, '\\:')})`, 'i');

        // 拦截点击事件
        document.addEventListener('click', function(e) {
            const target = e.target.closest('a');
            if (target && target.href && schemePattern.test(target.href)) {
                e.preventDefault();
                e.stopPropagation();
                logBlocked('Scheme URL', target.href);
                showInterceptNotification('已拦截APP跳转', 'success');
                return false;
            }
        }, true);

        // 拦截JavaScript设置的href
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        const links = node.querySelectorAll ? node.querySelectorAll('a') : [];
                        links.forEach(link => {
                            if (link.href && schemePattern.test(link.href)) {
                                link.href = 'javascript:void(0)';
                                link.setAttribute('data-blocked', 'true');
                                logBlocked('动态Scheme URL', link.href);
                            }
                        });
                    }
                });
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        GM_log('Scheme URL拦截已启用');
        showInterceptNotification('Scheme拦截已启用', 'success');
    }

    // 拦截JavaScript客户端唤起
    function toggleJavaScriptBlocking() {
        if (!config.blockJavaScript) return;

        const rules = detectSiteRules();

        // 拦截特定的JavaScript函数
        rules.jsHooks.forEach(hook => {
            if (window[hook]) {
                window[hook] = function() {
                    logBlocked('JavaScript Hook', hook);
                    showInterceptNotification(`已拦截 ${hook} 调用`, 'warning');
                    return false;
                };
            }
        });

        // 拦截eval中的scheme
        const originalEval = window.eval;
        window.eval = function(code) {
            if (typeof code === 'string') {
                rules.patterns.forEach(pattern => {
                    if (pattern.test(code)) {
                        logBlocked('Eval Scheme', code.substring(0, 100) + '...');
                        showInterceptNotification('已拦截eval中的APP跳转', 'warning');
                        return;
                    }
                });
            }
            return originalEval.apply(this, arguments);
        };

        // 拦截动态脚本执行
        const originalCreateElement = document.createElement;
        document.createElement = function(tagName) {
            const element = originalCreateElement.apply(this, arguments);
            if (tagName.toLowerCase() === 'script') {
                const originalText = element.text;
                const originalSrc = element.src;

                Object.defineProperty(element, 'text', {
                    set: function(value) {
                        rules.patterns.forEach(pattern => {
                            if (pattern.test(value)) {
                                logBlocked('动态脚本', value.substring(0, 100) + '...');
                                showInterceptNotification('已拦截动态脚本中的APP代码', 'warning');
                                return;
                            }
                        });
                        originalText = value;
                    },
                    get: function() {
                        return originalText;
                    }
                });

                Object.defineProperty(element, 'src', {
                    set: function(value) {
                        if (value && rules.patterns.some(pattern => pattern.test(value))) {
                            logBlocked('外部脚本', value);
                            showInterceptNotification('已拦截外部脚本中的APP代码', 'warning');
                            return;
                        }
                        originalSrc = value;
                    },
                    get: function() {
                        return originalSrc;
                    }
                });
            }
            return element;
        };

        GM_log('JavaScript拦截已启用');
        showInterceptNotification('JavaScript拦截已启用', 'success');
    }

    // 拦截下载提示
    function toggleDownloadBlocking() {
        if (!config.blockDownloadPrompts) return;

        const rules = detectSiteRules();

        // 移除下载相关元素
        const removeElements = (selector) => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                element.remove();
                logBlocked('下载元素', selector);
            });
        };

        // 立即清理现有元素
        removeElements(rules.selectors.appDownload);
        removeElements(rules.selectors.schemeLinks);

        // 监控新元素
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        removeElements(rules.selectors.appDownload);
                        removeElements(rules.selectors.schemeLinks);
                    }
                });
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        // 拦截下载相关事件
        document.addEventListener('click', function(e) {
            const target = e.target.closest('[data-download], .download-btn, .app-btn');
            if (target) {
                e.preventDefault();
                logBlocked('下载按钮', target.textContent || target.innerText);
                showInterceptNotification('已拦截app下载', 'success');
                return false;
            }
        }, true);

        GM_log('下载提示拦截已启用');
        showInterceptNotification('下载提示拦截已启用', 'success');
    }

    // 拦截横幅和弹窗
    function toggleBannerBlocking() {
        if (!config.blockAppBanners) return;

        const rules = detectSiteRules();

        // 隐藏横幅样式
        GM_addStyle(`
            ${rules.selectors.appDownload} {
                display: none !important;
                visibility: hidden !important;
                opacity: 0 !important;
                height: 0 !important;
                overflow: hidden !important;
            }

            ${rules.selectors.banners} {
                display: none !important;
                visibility: hidden !important;
                opacity: 0 !important;
                pointer-events: none !important;
            }

            ${rules.selectors.schemeLinks} {
                display: none !important;
                visibility: hidden !important;
            }

            /* 通用APP相关元素隐藏 */
            .app-banner, .download-banner, .app-popup, .download-popup,
            .app-modal, .download-modal, .app-promotion, .app-guide,
            [class*="app-"], [class*="download-"] {
                display: none !important;
                visibility: hidden !important;
                opacity: 0 !important;
            }
        `);

        GM_log('横幅拦截已启用');
        showInterceptNotification('横幅拦截已启用', 'success');
    }

    // 统计拦截信息
    let blockedStats = {
        schemes: 0,
        javascript: 0,
        downloads: 0,
        banners: 0,
        total: 0
    };

    function logBlocked(type, details) {
        if (!config.logBlocked) return;

        blockedStats.total++;

        switch (type) {
            case 'Scheme URL':
            case '动态Scheme URL':
                blockedStats.schemes++;
                break;
            case 'JavaScript Hook':
            case '动态脚本':
            case '外部脚本':
            case 'Eval Scheme':
                blockedStats.javascript++;
                break;
            case '下载元素':
            case '下载按钮':
                blockedStats.downloads++;
                break;
            default:
                blockedStats.banners++;
        }

        GM_log(`拦截${type}: ${details}`);
        updateBlockedCount();
    }

    function updateBlockedCount() {
        const countElement = document.getElementById('blocked-count');
        if (countElement) {
            countElement.textContent = blockedStats.total;
        }
    }

    function showInterceptStats() {
        const stats = document.createElement('div');
        stats.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                        box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 300px;">
                <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">拦截统计</h3>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <div style="text-align: center; padding: 15px; background: #f8f9fa; border-radius: 8px;">
                        <div style="font-size: 24px; font-weight: bold; color: #ff4757;">${blockedStats.total}</div>
                        <div style="font-size: 12px; color: #666;">总拦截数</div>
                    </div>
                    <div style="text-align: center; padding: 15px; background: #f8f9fa; border-radius: 8px;">
                        <div style="font-size: 24px; font-weight: bold; color: #3742fa;">${blockedStats.schemes}</div>
                        <div style="font-size: 12px; color: #666;">Scheme拦截</div>
                    </div>
                    <div style="text-align: center; padding: 15px; background: #f8f9fa; border-radius: 8px;">
                        <div style="font-size: 24px; font-weight: bold; color: #ffa502;">${blockedStats.javascript}</div>
                        <div style="font-size: 12px; color: #666;">JS拦截</div>
                    </div>
                    <div style="text-align: center; padding: 15px; background: #f8f9fa; border-radius: 8px;">
                        <div style="font-size: 24px; font-weight: bold; color: #ff6b6b;">${blockedStats.downloads}</div>
                        <div style="font-size: 12px; color: #666;">下载拦截</div>
                    </div>
                </div>

                <div style="text-align: center;">
                    <button onclick="this.parentElement.parentElement.remove()" style="padding: 10px 20px;
                                     background: #6c757d; color: white; border: none;
                                     border-radius: 6px; cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(stats);
    }

    // 通用拦截逻辑
    function applyGeneralBlocking() {
        // 拦截所有已知APP的scheme URL
        const allSchemes = Object.values(appSchemes).flat();
        const schemeRegex = new RegExp(`^(${allSchemes.map(s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')})`, 'i');

        // 全局拦截函数
        const interceptUrl = (url) => {
            if (schemeRegex.test(url)) {
                logBlocked('通用Scheme', url);
                showInterceptNotification('已拦截APP跳转', 'success');
                return false;
            }
            return true;
        };

        // 拦截window.open
        const originalOpen = window.open;
        window.open = function(url, ...args) {
            if (url && !interceptUrl(url)) {
                return null;
            }
            return originalOpen.apply(this, [url, ...args]);
        };

        // 拦截location.href
        let originalHref = location.href;
        Object.defineProperty(location, 'href', {
            get: function() {
                return originalHref;
            },
            set: function(value) {
                if (value && !interceptUrl(value)) {
                    return;
                }
                originalHref = value;
                // 这里不能直接设置location.href，否则会造成死循环
                // 实际应该通过其他方式处理
            }
        });

        // 拦截表单提交
        document.addEventListener('submit', function(e) {
            const form = e.target;
            if (form.action && !interceptUrl(form.action)) {
                e.preventDefault();
                return false;
            }
        }, true);

        GM_log('通用拦截已启用');
    }

    // 设置面板
    function showInterceptSettings() {
        return function() {
            const settings = document.createElement('div');
            settings.innerHTML = `
                <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                            background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                            box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 400px;">
                    <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">APP拦截屏蔽设置</h3>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-enabled" style="margin-right: 8px;" ${config.enabled ? 'checked' : ''}>
                            启用拦截器
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-schemes" style="margin-right: 8px;" ${config.blockSchemes ? 'checked' : ''}>
                            拦截Scheme
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-js" style="margin-right: 8px;" ${config.blockJavaScript ? 'checked' : ''}>
                            拦截JavaScript
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-downloads" style="margin-right: 8px;" ${config.blockDownloadPrompts ? 'checked' : ''}>
                            拦截下载提示
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-banners" style="margin-right: 8px;" ${config.blockAppBanners ? 'checked' : ''}>
                            拦截横幅
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-notifications" style="margin-right: 8px;" ${config.showNotifications ? 'checked' : ''}>
                            显示通知
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-log" style="margin-right: 8px;" ${config.logBlocked ? 'checked' : ''}>
                            记录日志
                        </label>
                    </div>

                    <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                        <h4 style="margin: 0 0 10px 0; color: #666;">自定义规则</h4>
                        <textarea id="custom-rules" placeholder="每行一个规则，支持正则表达式"
                                  style="width: 100%; height: 60px; padding: 8px; border: 1px solid #ddd; border-radius: 4px; resize: vertical;">${config.customRules.join('\n')}</textarea>
                    </div>

                    <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                        <h4 style="margin: 0 0 10px 0; color: #666;">支持拦截的APP</h4>
                        <div style="font-size: 12px; color: #666; max-height: 100px; overflow-y: auto;">
                            ${Object.keys(appSchemes).slice(0, 10).join(', ')} 等${Object.keys(appSchemes).length}个APP
                        </div>
                    </div>

                    <div style="text-align: right;">
                        <button id="save-intercept-settings" style="padding: 10px 20px;
                                        background: linear-gradient(135deg, #ff4757, #ff3838);
                                        color: white; border: none; border-radius: 6px;
                                        cursor: pointer; margin-right: 10px;">
                            保存设置
                        </button>
                        <button id="close-intercept-settings" style="padding: 10px 20px;
                                         background: #6c757d; color: white; border: none;
                                         border-radius: 6px; cursor: pointer;">
                            关闭
                        </button>
                    </div>
                </div>
            `;

            document.body.appendChild(settings);

            document.getElementById('save-intercept-settings').onclick = saveInterceptSettings;
            document.getElementById('close-intercept-settings').onclick = () => settings.remove();
        };
    }

    function saveInterceptSettings() {
        config.enabled = document.getElementById('setting-enabled').checked;
        config.blockSchemes = document.getElementById('setting-schemes').checked;
        config.blockJavaScript = document.getElementById('setting-js').checked;
        config.blockDownloadPrompts = document.getElementById('setting-downloads').checked;
        config.blockAppBanners = document.getElementById('setting-banners').checked;
        config.showNotifications = document.getElementById('setting-notifications').checked;
        config.logBlocked = document.getElementById('setting-log').checked;

        const customRulesText = document.getElementById('custom-rules').value;
        config.customRules = customRulesText.split('\n').filter(rule => rule.trim());

        // 保存设置
        Object.keys(config).forEach(key => {
            GM_setValue('appIntercept' + key.charAt(0).toUpperCase() + key.slice(1), config[key]);
        });

        location.reload();
    }

    // 通知系统
    function showInterceptNotification(message, type = 'info') {
        if (!config.showNotifications) return;

        const colors = {
            success: '#28a745',
            error: '#dc3545',
            warning: '#ffc107',
            info: '#17a2b8'
        };

        const notification = document.createElement('div');
        notification.innerHTML = `
            <div style="position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
                        background: ${colors[type]}; color: white; padding: 12px 24px;
                        border-radius: 8px; z-index: 10001; box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                        font-size: 14px; font-weight: bold;">
                ${message}
            </div>
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        document.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            switch (e.keyCode) {
                case 112: // F1 - 显示统计
                    if (e.ctrlKey) {
                        e.preventDefault();
                        showInterceptStats();
                    }
                    break;
                case 113: // F2 - 重新加载拦截
                    if (e.ctrlKey) {
                        e.preventDefault();
                        location.reload();
                    }
                    break;
            }
        });
    }

    // 初始化
    function init() {
        if (!config.enabled) {
            GM_log('APP拦截屏蔽器已禁用');
            return;
        }

        setTimeout(() => {
            createInterceptPanel();
            setupKeyboardShortcuts();
            applyGeneralBlocking();

            // 根据配置自动应用功能
            if (config.blockSchemes) toggleSchemeBlocking();
            if (config.blockJavaScript) toggleJavaScriptBlocking();
            if (config.blockDownloadPrompts) toggleDownloadBlocking();
            if (config.blockAppBanners) toggleBannerBlocking();

            GM_log('EhViewer APP拦截屏蔽器初始化完成');

            // 显示欢迎信息
            showInterceptNotification('APP拦截屏蔽器已启动！保护您的浏览器体验', 'success');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
