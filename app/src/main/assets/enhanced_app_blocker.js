// ==UserScript==
// @name         EhViewer 微信支付宝专用增强拦截器
// @namespace    http://ehviewer.com/
// @version      2.1.0
// @description  增强版微信支付宝专用拦截器，支持安全的支付功能，默认开启激进模式
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

    GM_log('EhViewer 微信支付宝专用增强拦截器启动 - 激进模式');

    // 增强版配置 - 默认开启所有拦截功能
    const config = {
        enabled: GM_getValue('enhancedBlockEnabled', true),
        aggressiveMode: GM_getValue('enhancedAggressiveMode', true), // 默认开启激进模式
        blockAllSchemes: GM_getValue('enhancedBlockAllSchemes', true),
        blockAllElements: GM_getValue('enhancedBlockAllElements', true),
        blockJavaScript: GM_getValue('enhancedBlockJavaScript', true),
        blockDynamicContent: GM_getValue('enhancedBlockDynamicContent', true),
        showNotifications: GM_getValue('enhancedShowNotifications', false), // 默认关闭通知
        logBlocked: GM_getValue('enhancedLogBlocked', false)
    };

    // 仅支持微信和支付宝的Scheme拦截列表
    const enhancedSchemes = [
        // 微信相关
        'weixin://', 'com.tencent.mm://', 'wechat://', 'wework://',
        'wxpay://', 'com.tencent.wechatpay://',
        // 支付宝相关
        'alipay://', 'com.eg.android.AlipayGphone://', 'alipayqr://'
    ];

    // 增强版元素选择器
    const enhancedSelectors = [
        // 通用APP相关元素
        '.app-download', '.download-app', '.app-banner', '.download-banner',
        '.app-popup', '.download-popup', '.app-modal', '.download-modal',
        '.app-promotion', '.download-promotion', '.app-guide', '.download-guide',
        '.open-in-app', '.app-open', '.download-open', '.app-jump',
        '.call-app', '.invoke-app', '.launch-app', '.start-app',

        // 动态类名匹配
        '[class*="app-"]', '[class*="download-"]', '[class*="open-"]',
        '[id*="app-"]', '[id*="download-"]', '[id*="open-"]',
        '[data-app]', '[data-download]', '[data-open]',

        // 属性选择器增强
        '[onclick*="app"]', '[onclick*="download"]', '[onclick*="open"]',
        '[href*="://"]', '[data-href*="://"]', '[data-url*="://"]',

        // 样式属性匹配
        '[style*="display"][style*="block"]',
        '[style*="visibility"][style*="visible"]',
        '[class*="show"]', '[class*="visible"]', '[class*="active"]'
    ];

    // 仅支持微信和支付宝的关键词列表
    const enhancedKeywords = [
        // 微信相关关键词
        '打开微信', '微信支付', '用微信打开', '微信扫码', '微信登录', '微信分享',
        // 支付宝相关关键词
        '打开支付宝', '支付宝支付', '用支付宝打开', '支付宝扫码', '支付宝登录', '支付宝分享',
        // 通用支付关键词
        '支付方式', '选择支付', '在线支付', '扫码支付'
    ];

    // 拦截统计
    let blockStats = {
        schemes: 0,
        elements: 0,
        javascript: 0,
        dynamic: 0,
        aggressive: 0,
        total: 0
    };

    /**
     * 创建增强版拦截控制面板
     */
    function createEnhancedPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-enhanced-block-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 50px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #ff6b6b, #ee5a52); color: white;
                        padding: 8px 12px; border-radius: 6px; font-size: 10px; font-family: Arial;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.3); min-width: 140px;">
                <div style="display: flex; align-items: center; margin-bottom: 4px;">
                    <span style="font-size: 12px; margin-right: 4px;">🚀</span>
                    <strong>增强拦截</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 2px;">
                    <button id="enhanced-block-all" title="拦截全部">全部</button>
                    <button id="enhanced-block-schemes" title="拦截跳转">跳转</button>
                    <button id="enhanced-block-elements" title="拦截元素">元素</button>
                    <button id="enhanced-show-stats" title="查看统计">统计</button>
                </div>
                <div style="font-size: 8px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 2px; margin-top: 4px;">
                    已拦截: <span id="enhanced-blocked-count">0</span> 次
                </div>
            </div>
        `;

        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('enhanced-block-all').onclick = () => blockAll();
        document.getElementById('enhanced-block-schemes').onclick = () => blockSchemes();
        document.getElementById('enhanced-block-elements').onclick = () => blockElements();
        document.getElementById('enhanced-show-stats').onclick = () => showStats();
    }

    /**
     * 拦截全部 - 激进模式
     */
    function blockAll() {
        if (!config.aggressiveMode) return;

        // 1. 拦截所有Scheme
        blockSchemes();

        // 2. 移除所有APP相关元素
        blockElements();

        // 3. 拦截JavaScript
        blockJavaScript();

        // 4. 拦截动态内容
        blockDynamicContent();

        GM_log('增强版APP拦截器激进模式已启用');
        showNotification('增强拦截激进模式已启用！', 'success');
    }

    /**
     * 拦截所有Scheme
     */
    function blockSchemes() {
        if (!config.blockAllSchemes) return;

        const schemePattern = new RegExp(`^(${enhancedSchemes.map(s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')})`, 'i');

        // 拦截点击事件
        document.addEventListener('click', function(e) {
            const target = e.target.closest('a, button, [onclick], [data-href]');
            if (target) {
                const href = target.href || target.getAttribute('data-href') || target.onclick;
                if (href && (typeof href === 'string') && schemePattern.test(href)) {
                    e.preventDefault();
                    e.stopPropagation();
                    logBlocked('Scheme拦截', href);
                    showNotification('已拦截APP跳转', 'success');
                    return false;
                }
            }
        }, true);

        // 拦截动态设置的href
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        const links = node.querySelectorAll('a[href], [data-href]');
                        links.forEach(link => {
                            const href = link.href || link.getAttribute('data-href');
                            if (href && schemePattern.test(href)) {
                                link.href = 'javascript:void(0)';
                                link.setAttribute('data-blocked', 'true');
                                logBlocked('动态Scheme', href);
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

        GM_log('增强版Scheme拦截已启用');
    }

    /**
     * 移除所有APP相关元素
     */
    function blockElements() {
        if (!config.blockAllElements) return;

        // 添加CSS隐藏规则
        let cssRules = '';
        enhancedSelectors.forEach(selector => {
            cssRules += `${selector} { display: none !important; visibility: hidden !important; opacity: 0 !important; height: 0 !important; overflow: hidden !important; } `;
        });

        // 添加关键词拦截
        enhancedKeywords.forEach(keyword => {
            cssRules += `[class*="${keyword.replace(/\s+/g, '').toLowerCase()}"] { display: none !important; } `;
            cssRules += `[id*="${keyword.replace(/\s+/g, '').toLowerCase()}"] { display: none !important; } `;
        });

        GM_addStyle(cssRules);

        // 动态移除元素
        function removeElements() {
            enhancedSelectors.forEach(selector => {
                const elements = document.querySelectorAll(selector);
                elements.forEach(element => {
                    if (element && element.parentNode) {
                        element.remove();
                        logBlocked('元素移除', selector);
                    }
                });
            });

            // 按文本内容移除
            const allElements = document.querySelectorAll('*');
            allElements.forEach(element => {
                if (element && element.textContent && element.children.length === 0) {
                    const text = element.textContent.toLowerCase();
                    if (enhancedKeywords.some(keyword => text.includes(keyword.toLowerCase()))) {
                        if (element && element.parentNode) {
                            element.style.display = 'none';
                            logBlocked('关键词移除', element.textContent.substring(0, 20) + '...');
                        }
                    }
                }
            });
        }

        // 立即执行一次
        removeElements();

        // 监听DOM变化
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    setTimeout(removeElements, 100);
                }
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        GM_log('增强版元素拦截已启用');
    }

    /**
     * 拦截JavaScript
     */
    function blockJavaScript() {
        if (!config.blockJavaScript) return;

        const jsFunctions = [
            'openApp', 'launchApp', 'downloadApp', 'callApp',
            'openClient', 'launchClient', 'downloadClient',
            'gotoApp', 'jumpToApp', 'startApp'
        ];

        // 拦截函数调用
        jsFunctions.forEach(funcName => {
            if (window[funcName]) {
                window[funcName] = function() {
                    logBlocked('JavaScript函数', funcName);
                    showNotification(`已拦截 ${funcName} 调用`, 'warning');
                    return false;
                };
            }
        });

        GM_log('增强版JavaScript拦截已启用');
    }

    /**
     * 拦截动态内容
     */
    function blockDynamicContent() {
        if (!config.blockDynamicContent) return;

        const dynamicObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        // 检查新添加的元素是否包含APP相关内容
                        const element = node;
                        const text = element.textContent || '';
                        const className = element.className || '';
                        const id = element.id || '';

                        const hasAppContent = enhancedKeywords.some(keyword =>
                            text.toLowerCase().includes(keyword.toLowerCase()) ||
                            className.toLowerCase().includes(keyword.toLowerCase()) ||
                            id.toLowerCase().includes(keyword.toLowerCase())
                        );

                        if (hasAppContent) {
                            element.style.display = 'none';
                            logBlocked('动态内容拦截', text.substring(0, 20) + '...');
                        }
                    }
                });
            });
        });

        dynamicObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        GM_log('增强版动态内容拦截已启用');
    }

    /**
     * 记录拦截日志
     */
    function logBlocked(type, details) {
        blockStats.total++;

        switch (type) {
            case 'Scheme拦截':
            case '动态Scheme':
                blockStats.schemes++;
                break;
            case '元素移除':
            case '关键词移除':
                blockStats.elements++;
                break;
            case 'JavaScript函数':
                blockStats.javascript++;
                break;
            case '动态内容拦截':
                blockStats.dynamic++;
                break;
            default:
                blockStats.aggressive++;
        }

        updateBlockedCount();

        if (config.logBlocked) {
            GM_log(`增强拦截${type}: ${details}`);
        }
    }

    /**
     * 更新拦截计数
     */
    function updateBlockedCount() {
        const countElement = document.getElementById('enhanced-blocked-count');
        if (countElement) {
            countElement.textContent = blockStats.total;
        }
    }

    /**
     * 显示统计
     */
    function showStats() {
        const stats = document.createElement('div');
        stats.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 20px; border-radius: 10px; z-index: 10001;
                        box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 300px;">
                <h3 style="margin: 0 0 15px 0; color: #333; text-align: center;">增强版拦截统计</h3>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 15px;">
                    <div style="text-align: center; padding: 10px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 20px; font-weight: bold; color: #ff6b6b;">${blockStats.total}</div>
                        <div style="font-size: 10px; color: #666;">总拦截数</div>
                    </div>
                    <div style="text-align: center; padding: 10px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 20px; font-weight: bold; color: #28a745;">${blockStats.schemes}</div>
                        <div style="font-size: 10px; color: #666;">Scheme拦截</div>
                    </div>
                    <div style="text-align: center; padding: 10px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 20px; font-weight: bold; color: #ffc107;">${blockStats.elements}</div>
                        <div style="font-size: 10px; color: #666;">元素拦截</div>
                    </div>
                    <div style="text-align: center; padding: 10px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 20px; font-weight: bold; color: #6f42c1;">${blockStats.dynamic}</div>
                        <div style="font-size: 10px; color: #666;">动态拦截</div>
                    </div>
                </div>

                <div style="text-align: center;">
                    <button onclick="this.parentElement.parentElement.remove()" style="padding: 8px 16px;
                                     background: #6c757d; color: white; border: none;
                                     border-radius: 5px; cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(stats);
    }

    /**
     * 显示通知
     */
    function showNotification(message, type = 'info') {
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
                        background: ${colors[type]}; color: white; padding: 10px 20px;
                        border-radius: 6px; z-index: 10001; box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                        font-size: 12px; font-weight: bold;">
                ${message}
            </div>
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    /**
     * 初始化增强版拦截器
     */
    function init() {
        if (!config.enabled) {
            GM_log('增强版APP拦截器已禁用');
            return;
        }

        setTimeout(() => {
            createEnhancedPanel();

            // 默认开启激进模式
            if (config.aggressiveMode) {
                blockAll();
            } else {
                // 否则按配置开启各项功能
                if (config.blockAllSchemes) blockSchemes();
                if (config.blockAllElements) blockElements();
                if (config.blockJavaScript) blockJavaScript();
                if (config.blockDynamicContent) blockDynamicContent();
            }

            GM_log('EhViewer 增强版APP拦截器初始化完成');
            showNotification('增强版APP拦截器已启动！保护您的浏览体验', 'success');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
