// ==UserScript==
// @name         EhViewer 微信支付宝专用拦截器
// @namespace    http://ehviewer.com/
// @version      2.0.0
// @description  专门拦截网站上的微信和支付宝相关跳转，支持安全的支付功能
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

    GM_log('EhViewer 微信支付宝专用拦截器启动');

    // 百度App提示的各种表现形式 - 增强版
    const baiduAppSelectors = [
        // 常见的百度App提示元素
        '.app-download',
        '.download-guide',
        '.app-guide',
        '.app-popup',
        '.download-popup',
        '.app-modal',
        '.download-modal',
        '.app-banner',
        '.download-banner',
        '.app-promotion',
        '.app-open-tip',
        '.open-app-tip',
        '.app-jump-tip',
        '.app-float',

        // 百度搜索结果页面的App提示
        '.c-result-app',
        '.app-download-wrap',
        '.app-open-wrap',
        '.app-jump-wrap',

        // 百度移动端页面的App提示
        '.open-in-app',
        '.download-app-btn',
        '.app-btn',
        '.open-app-btn',
        '.jump-to-app',

        // 百度知道、贴吧等子站点的App提示
        '.app-entry',
        '.app-entrance',
        '.app-guide-entry',
        '.app-download-entry',

        // 新增的百度App元素选择器
        '.app-suggest',
        '.app-recommend',
        '.app-ad',
        '.app-promote',
        '.app-notice',
        '.app-remind',
        '.app-alert',
        '.app-toast',
        '.app-layer',
        '.app-overlay',

        // 百度移动搜索新增元素
        '.m-app-download',
        '.m-app-open',
        '.m-app-jump',
        '.m-app-guide',
        '.m-app-banner',

        // 百度App唤起相关元素
        '.call-app',
        '.invoke-app',
        '.launch-app',
        '.start-app',
        '.open-client',

        // 通用App相关元素 - 增强版
        '[class*="app-"]',
        '[class*="download-"]',
        '[id*="app-"]',
        '[id*="download-"]',
        '[data-app]',
        '[data-download]',

        // 新增的属性选择器
        '[data-type*="app"]',
        '[data-action*="app"]',
        '[data-scheme]',
        '[data-href*="baidu://"]',
        '[data-href*="baidusearch://"]',

        // 百度App特有的元素
        '.baidu-app',
        '.baidu-client',
        '.baidu-mobile',
        '.baidu-lite',
        '.baidu-browser',

        // 动态生成的App元素
        '[style*="display"][style*="block"]',
        '[style*="visibility"][style*="visible"]',
        '[class*="show"]',
        '[class*="visible"]'
    ];

    // 仅支持微信和支付宝的Scheme URL模式
    const supportedSchemes = [
        // 微信相关
        'weixin://',
        'com.tencent.mm://',
        'wechat://',
        'wxpay://',
        'com.tencent.wechatpay://',
        // 支付宝相关
        'alipay://',
        'com.eg.android.AlipayGphone://',
        'alipayqr://'
    ];

    // 仅支持微信和支付宝的JavaScript函数名
    const supportedAppFunctions = [
        // 微信相关函数
        'openWeChat',
        'wechatApp',
        'openWeixin',
        'weixinApp',
        'openWXPay',
        'wxpayApp',
        // 支付宝相关函数
        'openAlipay',
        'alipayApp',
        'openAliPay',
        'aliPayApp',
        'openAlipayQR',
        'alipayQRApp',
        // 通用支付函数
        'openPayment',
        'paymentApp',
        'payApp'
    ];

    // 仅支持微信和支付宝的文本关键词
    const supportedAppKeywords = [
        // 微信相关关键词
        '打开微信',
        '微信支付',
        '用微信打开',
        '微信扫码',
        '微信登录',
        '微信分享',
        // 支付宝相关关键词
        '打开支付宝',
        '支付宝支付',
        '用支付宝打开',
        '支付宝扫码',
        '支付宝登录',
        '支付宝分享',
        // 通用支付关键词
        '支付方式',
        '选择支付',
        '在线支付',
        '扫码支付'
    ];

    // 配置选项 - 增强版默认设置
    const config = {
        enabled: GM_getValue('baiduBlockEnabled', true), // 默认启用
        blockElements: GM_getValue('baiduBlockElements', true), // 默认拦截元素
        blockSchemes: GM_getValue('baiduBlockSchemes', true), // 默认拦截跳转
        blockFunctions: GM_getValue('baiduBlockFunctions', true), // 默认拦截函数
        blockByText: GM_getValue('baiduBlockByText', true), // 默认文本拦截
        showNotifications: GM_getValue('baiduShowNotifications', false), // 默认关闭通知（避免过多弹窗）
        logBlocked: GM_getValue('baiduLogBlocked', false), // 默认关闭日志
        aggressiveMode: GM_getValue('baiduAggressiveMode', true), // 新增：激进模式
        blockDynamicContent: GM_getValue('baiduBlockDynamicContent', true), // 新增：拦截动态内容
        blockInlineStyles: GM_getValue('baiduBlockInlineStyles', true) // 新增：拦截内联样式
    };

    // 拦截统计 - 增强版
    let blockedStats = {
        elements: 0,
        schemes: 0,
        functions: 0,
        textBlocks: 0,
        dynamicContent: 0,
        inlineStyles: 0,
        aggressiveBlocks: 0,
        total: 0
    };

    /**
     * 创建百度专用拦截控制面板
     */
    function createBaiduBlockPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-baidu-block-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 60px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #2937f0, #1e3aef); color: white;
                        padding: 8px 12px; border-radius: 6px; font-size: 10px; font-family: Arial;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.3); min-width: 140px;">
                <div style="display: flex; align-items: center; margin-bottom: 4px;">
                    <span style="font-size: 12px; margin-right: 4px;">🛡️</span>
                    <strong>百度拦截</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 2px;">
                    <button id="baidu-block-elements" title="拦截元素">元素</button>
                    <button id="baidu-block-schemes" title="拦截跳转">跳转</button>
                    <button id="baidu-block-functions" title="拦截函数">函数</button>
                    <button id="baidu-show-stats" title="查看统计">统计</button>
                </div>
                <div style="font-size: 8px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 2px; margin-top: 4px;">
                    已拦截: <span id="baidu-blocked-count">0</span> 次
                </div>
            </div>
        `;

        // 应用按钮样式
        const buttons = panel.querySelectorAll('button');
        buttons.forEach(btn => {
            btn.style.cssText = `
                padding: 2px 4px;
                border: none;
                border-radius: 3px;
                background: rgba(255,255,255,0.2);
                color: white;
                font-size: 8px;
                cursor: pointer;
                transition: background 0.2s;
                width: 100%;
            `;
            btn.onmouseover = () => btn.style.background = 'rgba(255,255,255,0.4)';
            btn.onmouseout = () => btn.style.background = 'rgba(255,255,255,0.2)';
        });

        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('baidu-block-elements').onclick = () => blockElements();
        document.getElementById('baidu-block-schemes').onclick = () => blockSchemes();
        document.getElementById('baidu-block-functions').onclick = () => blockFunctions();
        document.getElementById('baidu-show-stats').onclick = () => showStats();

        return panel;
    }

    /**
     * 拦截App提示元素
     */
    function blockElements() {
        if (!config.blockElements) return;

        // 添加CSS隐藏规则
        let cssRules = '';
        baiduAppSelectors.forEach(selector => {
            cssRules += `${selector} { display: none !important; visibility: hidden !important; opacity: 0 !important; height: 0 !important; overflow: hidden !important; } `;
        });

        // 添加文本内容拦截
        if (config.blockByText) {
            supportedAppKeywords.forEach(keyword => {
                cssRules += `[class*="${keyword.replace(/\s+/g, '').toLowerCase()}"] { display: none !important; } `;
                cssRules += `[id*="${keyword.replace(/\s+/g, '').toLowerCase()}"] { display: none !important; } `;
            });
        }

        GM_addStyle(cssRules);

        // 动态移除现有元素 - 增强版
        function removeAppElements() {
            baiduAppSelectors.forEach(selector => {
                const elements = document.querySelectorAll(selector);
                elements.forEach(element => {
                    if (element && element.parentNode) {
                        element.remove();
                        logBlocked('元素拦截', selector);
                    }
                });
            });

            // 激进模式：拦截更多潜在的App元素
            if (config.aggressiveMode) {
                // 拦截所有包含特定文本的元素
                const allElements = document.querySelectorAll('*');
                allElements.forEach(element => {
                    if (element && element.textContent && element.children.length === 0) {
                        const text = element.textContent.toLowerCase();
                        if (supportedAppKeywords.some(keyword => text.includes(keyword.toLowerCase()))) {
                            if (element && element.parentNode) {
                                element.style.display = 'none';
                                logBlocked('激进拦截', element.textContent.substring(0, 20) + '...');
                            }
                        }
                    }
                });
            }

            // 拦截动态内容
            if (config.blockDynamicContent) {
                const dynamicSelectors = [
                    '[class*="app"][class*="show"]',
                    '[class*="download"][class*="visible"]',
                    '[style*="display: block"][class*="app"]',
                    '[style*="visibility: visible"][class*="download"]'
                ];

                dynamicSelectors.forEach(selector => {
                    const elements = document.querySelectorAll(selector);
                    elements.forEach(element => {
                        if (element && element.parentNode) {
                            element.remove();
                            logBlocked('动态内容拦截', selector);
                        }
                    });
                });
            }

            // 按文本内容移除
            if (config.blockByText) {
                const allElements = document.querySelectorAll('*');
                allElements.forEach(element => {
                    if (element.textContent && supportedAppKeywords.some(keyword =>
                        element.textContent.includes(keyword))) {
                        if (element && element.parentNode) {
                            element.style.display = 'none';
                            logBlocked('文本拦截', element.textContent.substring(0, 20) + '...');
                        }
                    }
                });
            }
        }

        // 立即执行一次
        removeAppElements();

        // 监听DOM变化
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === Node.ELEMENT_NODE) {
                            // 延迟执行以确保元素完全加载
                            setTimeout(removeAppElements, 100);
                        }
                    });
                }
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        GM_log('百度App元素拦截已启用');
        showNotification('百度App元素拦截已启用', 'success');
    }

    /**
     * 拦截Scheme跳转
     */
    function blockSchemes() {
        if (!config.blockSchemes) return;

        const schemePattern = new RegExp(`^(${supportedSchemes.map(s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')})`, 'i');

        // 拦截点击事件
        document.addEventListener('click', function(e) {
            const target = e.target.closest('a');
            if (target && target.href && schemePattern.test(target.href)) {
                e.preventDefault();
                e.stopPropagation();
                logBlocked('Scheme跳转', target.href);
                showNotification('已拦截百度App跳转', 'success');
                return false;
            }
        }, true);

        // 拦截window.open
        const originalOpen = window.open;
        window.open = function(url, ...args) {
            if (url && schemePattern.test(url)) {
                logBlocked('window.open跳转', url);
                showNotification('已拦截百度App跳转', 'success');
                return null;
            }
            return originalOpen.apply(this, [url, ...args]);
        };

        GM_log('百度App Scheme拦截已启用');
        showNotification('百度App跳转拦截已启用', 'success');
    }

    /**
     * 拦截JavaScript函数
     */
    function blockFunctions() {
        if (!config.blockFunctions) return;

        supportedAppFunctions.forEach(funcName => {
            if (window[funcName]) {
                window[funcName] = function() {
                    logBlocked('JavaScript函数', funcName);
                    showNotification(`已拦截 ${funcName} 调用`, 'warning');
                    return false;
                };
            }
        });

        // 拦截动态函数创建
        const originalCreateElement = document.createElement;
        document.createElement = function(tagName) {
            const element = originalCreateElement.apply(this, arguments);
            if (tagName.toLowerCase() === 'script') {
                const originalText = element.text;
                Object.defineProperty(element, 'text', {
                    set: function(value) {
                        if (value && supportedAppFunctions.some(func =>
                            value.includes(func))) {
                            logBlocked('动态脚本函数', func);
                            showNotification('已拦截动态脚本中的App函数', 'warning');
                            return;
                        }
                        originalText = value;
                    },
                    get: function() {
                        return originalText;
                    }
                });
            }
            return element;
        };

        GM_log('百度App JavaScript拦截已启用');
        showNotification('百度App函数拦截已启用', 'success');
    }

    /**
     * 显示拦截统计
     */
    function showStats() {
        const stats = document.createElement('div');
        stats.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 20px; border-radius: 10px; z-index: 10001;
                        box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 300px;">
                <h3 style="margin: 0 0 15px 0; color: #333; text-align: center;">百度App拦截统计</h3>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 15px;">
                    <div style="text-align: center; padding: 8px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 18px; font-weight: bold; color: #2937f0;">${blockedStats.total}</div>
                        <div style="font-size: 9px; color: #666;">总拦截数</div>
                    </div>
                    <div style="text-align: center; padding: 8px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 18px; font-weight: bold; color: #28a745;">${blockedStats.elements}</div>
                        <div style="font-size: 9px; color: #666;">元素拦截</div>
                    </div>
                    <div style="text-align: center; padding: 8px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 18px; font-weight: bold; color: #ffc107;">${blockedStats.schemes}</div>
                        <div style="font-size: 9px; color: #666;">跳转拦截</div>
                    </div>
                    <div style="text-align: center; padding: 8px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 18px; font-weight: bold; color: #dc3545;">${blockedStats.functions}</div>
                        <div style="font-size: 9px; color: #666;">函数拦截</div>
                    </div>
                    <div style="text-align: center; padding: 8px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 18px; font-weight: bold; color: #ff6b6b;">${blockedStats.aggressiveBlocks}</div>
                        <div style="font-size: 9px; color: #666;">激进拦截</div>
                    </div>
                    <div style="text-align: center; padding: 8px; background: #f8f9fa; border-radius: 6px;">
                        <div style="font-size: 18px; font-weight: bold; color: #6f42c1;">${blockedStats.dynamicContent}</div>
                        <div style="font-size: 9px; color: #666;">动态内容</div>
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
     * 记录拦截日志
     */
    function logBlocked(type, details) {
        blockedStats.total++;
        updateBlockedCount();

        switch (type) {
            case '元素拦截':
                blockedStats.elements++;
                break;
            case 'Scheme跳转':
            case 'window.open跳转':
                blockedStats.schemes++;
                break;
            case 'JavaScript函数':
            case '动态脚本函数':
                blockedStats.functions++;
                break;
            case '激进拦截':
                blockedStats.aggressiveBlocks++;
                break;
            case '动态内容拦截':
                blockedStats.dynamicContent++;
                break;
            case '内联样式拦截':
                blockedStats.inlineStyles++;
                break;
            default:
                blockedStats.textBlocks++;
        }

        if (config.logBlocked) {
            GM_log(`百度拦截${type}: ${details}`);
        }
    }

    /**
     * 更新拦截计数
     */
    function updateBlockedCount() {
        const countElement = document.getElementById('baidu-blocked-count');
        if (countElement) {
            countElement.textContent = blockedStats.total;
        }
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
            info: '#2937f0'
        };

        const notification = document.createElement('div');
        notification.innerHTML = `
            <div style="position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
                        background: ${colors[type]}; color: white; padding: 10px 20px;
                        border-radius: 6px; z-index: 10001; box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                        font-size: 12px; font-weight: bold; max-width: 300px; text-align: center;">
                ${message}
            </div>
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    /**
     * 初始化百度App拦截器
     */
    function init() {
        if (!config.enabled) {
            GM_log('百度App拦截器已禁用');
            return;
        }

        setTimeout(() => {
            createBaiduBlockPanel();

            // 根据配置自动启用功能 - 增强版
            if (config.blockElements) blockElements();
            if (config.blockSchemes) blockSchemes();
            if (config.blockFunctions) blockFunctions();

            // 新增：激进模式自动启用
            if (config.aggressiveMode) {
                GM_log('百度App拦截器激进模式已启用');
            }

            // 新增：动态内容拦截自动启用
            if (config.blockDynamicContent) {
                GM_log('百度App动态内容拦截已启用');
            }

            GM_log('EhViewer 百度App拦截器初始化完成');
            showNotification('百度App拦截器已启动！🎉', 'success');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
