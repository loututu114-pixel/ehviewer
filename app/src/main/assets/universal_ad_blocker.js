// ==UserScript==
// @name         EhViewer 通用广告拦截器
// @namespace    http://ehviewer.com/
// @version      2.5.0
// @description  强大的通用广告拦截器，拦截各种类型的广告、弹窗和跟踪脚本
// @author       EhViewer Team
// @match        *://*/*
// @exclude      *://*.google.com/*
// @exclude      *://*.baidu.com/*
// @exclude      *://*.bing.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 通用广告拦截器已启动');

    // 统计数据
    let stats = {
        adsBlocked: GM_getValue('adsBlocked', 0),
        popupsBlocked: GM_getValue('popupsBlocked', 0),
        trackersBlocked: GM_getValue('trackersBlocked', 0),
        lastReset: GM_getValue('lastReset', Date.now())
    };

    // 检查是否需要重置统计
    if (Date.now() - stats.lastReset > 7 * 24 * 60 * 60 * 1000) { // 7天重置一次
        stats = { adsBlocked: 0, popupsBlocked: 0, trackersBlocked: 0, lastReset: Date.now() };
        GM_setValue('adsBlocked', 0);
        GM_setValue('popupsBlocked', 0);
        GM_setValue('trackersBlocked', 0);
        GM_setValue('lastReset', Date.now());
    }

    // 配置选项
    const config = {
        enabled: GM_getValue('adBlockEnabled', true),
        showStats: GM_getValue('showStats', true),
        blockTrackers: GM_getValue('blockTrackers', true),
        blockPopups: GM_getValue('blockPopups', true),
        cosmeticFiltering: GM_getValue('cosmeticFiltering', true),
        whitelist: GM_getValue('whitelist', [])
    };

    // 检查是否在白名单中
    function isWhitelisted(url) {
        return config.whitelist.some(domain => url.includes(domain));
    }

    // 广告选择器规则库
    const adSelectors = [
        // 通用广告选择器
        '.ad', '.ads', '.advertisement', '.ad-banner', '.ad-container',
        '.advert', '.sponsor', '.promotion', '.commercial',
        '[class*="ad"]', '[id*="ad"]', '[class*="advert"]', '[id*="advert"]',
        '[class*="sponsor"]', '[id*="sponsor"]', '[data-ad]', '[data-advertisement]',

        // 弹窗和遮罩层
        '.popup', '.modal', '.overlay', '.lightbox', '.dialog',
        '[class*="popup"]', '[id*="popup"]', '[class*="modal"]', '[id*="modal"]',
        '[class*="overlay"]', '[id*="overlay"]', '.mask', '.shade',

        // 侧边栏广告
        '.sidebar-ad', '.side-ad', '.right-ad', '.left-ad',

        // 页头页尾广告
        '.header-ad', '.footer-ad', '.top-ad', '.bottom-ad',

        // 移动端广告
        '.mobile-ad', '.m-ad', '.app-ad', '.download-ad',

        // 视频广告
        '.video-ad', '.pre-roll', '.mid-roll', '.post-roll',
        '.ad-video', '.commercial-video',

        // 图片广告
        '.banner', '.banner-ad', '[class*="banner"]',

        // 社交媒体广告
        '.social-ad', '.facebook-ad', '.twitter-ad', '.instagram-ad',

        // 跟踪器
        '[class*="tracking"]', '[id*="tracking"]', '[class*="analytics"]',

        // 中文网站广告
        '.gg', '.guanggao', '.guanggao2', '.adsbygoogle',
        '.bdshare', '.share-btn', '.download-btn',

        // 网站特定广告
        '.google-ad', '.baidu-ad', '.ali-ad', '.tencent-ad',
        '.toutiao-ad', '.weibo-ad', '.zhihu-ad'
    ];

    // 广告域名黑名单
    const adDomains = [
        'doubleclick.net', 'googlesyndication.com', 'googleadservices.com',
        'googletagmanager.com', 'googletagservices.com', 'amazon-adsystem.com',
        'facebook.com/tr', 'facebook.net', 'connect.facebook.net',
        'adsystem.amazon.com', 'amazon.com/aan', 'alibaba.com',
        'baidu.com', 'bdstatic.com', 'bdimg.com',
        'tencent.com', 'qq.com', 'sina.com.cn',
        'toutiao.com', 'bytedance.com', 'ixigua.com',
        'analytics.google.com', 'googletagmanager.com'
    ];

    // 创建统计面板
    function createStatsPanel() {
        if (!config.showStats) return;

        const panel = document.createElement('div');
        panel.id = 'ehviewer-adblock-stats';
        panel.innerHTML = `
            <div style="position: fixed; bottom: 10px; left: 10px; z-index: 10000;
                        background: rgba(0,0,0,0.8); color: white; padding: 8px 12px;
                        border-radius: 5px; font-size: 11px; font-family: Arial; cursor: pointer;">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="color: #4CAF50;">🚫 ${stats.adsBlocked}</span>
                    <span style="color: #FF9800;">📋 ${stats.popupsBlocked}</span>
                    <span style="color: #2196F3;">👁️ ${stats.trackersBlocked}</span>
                </div>
                <div style="font-size: 9px; color: #ccc; margin-top: 2px;">
                    点击隐藏 | EhViewer广告拦截器
                </div>
            </div>
        `;

        panel.onclick = () => panel.remove();
        document.body.appendChild(panel);

        // 5秒后自动隐藏
        setTimeout(() => {
            if (panel.parentNode) {
                panel.remove();
            }
        }, 5000);
    }

    // CSS广告过滤
    function applyCosmeticFilters() {
        if (!config.cosmeticFiltering) return;

        let cssRules = '';

        // 生成CSS隐藏规则
        adSelectors.forEach(selector => {
            cssRules += `${selector} { display: none !important; visibility: hidden !important; }\n`;
        });

        // 添加特殊规则
        cssRules += `
            /* 隐藏常见的广告容器 */
            div[style*="position: fixed"] { display: none !important; }
            div[style*="position: absolute"][style*="z-index: 999"] { display: none !important; }

            /* 隐藏跟踪像素 */
            img[width="1"][height="1"] { display: none !important; }
            img[src*="pixel"] { display: none !important; }
            img[src*="tracker"] { display: none !important; }

            /* 隐藏社交分享按钮（可选） */
            .share-btn, .social-share { opacity: 0.3 !important; }
        `;

        GM_addStyle(cssRules);
        GM_log('已应用CSS广告过滤规则');
    }

    // 元素级广告拦截
    function blockElementAds() {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === 1) { // Element节点
                            removeAdsFromElement(node);
                        }
                    });
                }
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        // 处理现有元素
        removeAdsFromElement(document.body);
    }

    function removeAdsFromElement(root) {
        if (isWhitelisted(window.location.href)) return;

        adSelectors.forEach(selector => {
            const elements = root.querySelectorAll(selector);
            elements.forEach(element => {
                if (element.offsetParent !== null || element.offsetWidth > 0) {
                    element.style.display = 'none';
                    element.remove();
                    stats.adsBlocked++;
                    GM_log(`已拦截广告元素: ${selector}`);
                }
            });
        });

        // 处理特殊情况
        handleSpecialAds(root);
    }

    function handleSpecialAds(root) {
        // 处理固定定位的广告
        const fixedElements = root.querySelectorAll('*[style*="position: fixed"], *[style*="position: absolute"]');
        fixedElements.forEach(element => {
            const rect = element.getBoundingClientRect();
            if (rect.width > 200 && rect.height > 100) { // 大尺寸的固定元素
                element.style.display = 'none';
                stats.adsBlocked++;
            }
        });

        // 处理iframe广告
        const iframes = root.querySelectorAll('iframe');
        iframes.forEach(iframe => {
            const src = iframe.src || '';
            if (adDomains.some(domain => src.includes(domain))) {
                iframe.remove();
                stats.adsBlocked++;
                GM_log('已拦截iframe广告: ' + src);
            }
        });
    }

    // 弹窗拦截
    function blockPopups() {
        if (!config.blockPopups) return;

        // 拦截window.open
        const originalOpen = window.open;
        window.open = function(url, name, specs) {
            if (!isWhitelisted(url || '')) {
                stats.popupsBlocked++;
                GM_log('已拦截弹窗: ' + url);
                return null;
            }
            return originalOpen.call(this, url, name, specs);
        };

        // 移除弹窗遮罩层
        const overlays = document.querySelectorAll('.popup-overlay, .modal-overlay, .lightbox-overlay');
        overlays.forEach(overlay => {
            overlay.remove();
            stats.popupsBlocked++;
        });

        // 监听弹窗事件
        document.addEventListener('click', function(e) {
            const target = e.target;
            if (target.matches('a[target="_blank"], a[href*="popup"], a[href*="modal"]')) {
                const href = target.href || '';
                if (!isWhitelisted(href) && adDomains.some(domain => href.includes(domain))) {
                    e.preventDefault();
                    e.stopPropagation();
                    stats.popupsBlocked++;
                    GM_log('已阻止广告链接点击: ' + href);
                }
            }
        }, true);
    }

    // 跟踪器拦截
    function blockTrackers() {
        if (!config.blockTrackers) return;

        // 移除跟踪脚本
        const trackerSelectors = [
            'script[src*="google-analytics"]', 'script[src*="googletagmanager"]',
            'script[src*="facebook"]', 'script[src*="twitter"]', 'script[src*="linkedin"]',
            'script[src*="baidu"]', 'script[src*="tencent"]', 'script[src*="sina"]',
            'script[src*="tracking"]', 'script[src*="analytics"]', 'script[src*="stats"]'
        ];

        trackerSelectors.forEach(selector => {
            const scripts = document.querySelectorAll(selector);
            scripts.forEach(script => {
                script.remove();
                stats.trackersBlocked++;
                GM_log('已移除跟踪脚本: ' + script.src);
            });
        });

        // 阻止Cookie设置
        const originalSetCookie = document.__lookupSetter__('cookie');
        if (originalSetCookie) {
            document.__defineSetter__('cookie', function(value) {
                if (value && (value.includes('_ga') || value.includes('__utm') || value.includes('_fb'))) {
                    GM_log('已阻止跟踪Cookie: ' + value);
                    return;
                }
                originalSetCookie.call(this, value);
            });
        }
    }

    // 网络请求拦截
    function interceptRequests() {
        // 拦截XMLHttpRequest
        const originalXHR = window.XMLHttpRequest;
        window.XMLHttpRequest = function() {
            const xhr = new originalXHR();
            const originalOpen = xhr.open;

            xhr.open = function(method, url) {
                if (!isWhitelisted(url) && adDomains.some(domain => url.includes(domain))) {
                    GM_log('已拦截广告请求: ' + url);
                    stats.adsBlocked++;
                    return;
                }
                return originalOpen.apply(this, arguments);
            };

            return xhr;
        };

        // 拦截Fetch
        const originalFetch = window.fetch;
        window.fetch = function(url, options) {
            if (typeof url === 'string' && !isWhitelisted(url) && adDomains.some(domain => url.includes(domain))) {
                GM_log('已拦截广告请求: ' + url);
                stats.adsBlocked++;
                return Promise.reject(new Error('广告请求已被拦截'));
            }
            return originalFetch.apply(this, arguments);
        };
    }

    // 保存统计数据
    function saveStats() {
        GM_setValue('adsBlocked', stats.adsBlocked);
        GM_setValue('popupsBlocked', stats.popupsBlocked);
        GM_setValue('trackersBlocked', stats.trackersBlocked);
    }

    // 定期保存统计
    setInterval(saveStats, 30000);

    // 创建控制面板
    function createControlPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-adblock-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 50%; right: -20px; z-index: 10000;
                        background: rgba(0,0,0,0.8); color: white; padding: 8px;
                        border-radius: 5px 0 0 5px; font-size: 10px; cursor: pointer;
                        transform: rotate(-90deg); transform-origin: bottom right;">
                🚫 广告拦截器
            </div>
        `;

        panel.onclick = showSettingsDialog;
        document.body.appendChild(panel);
    }

    function showSettingsDialog() {
        const dialog = document.createElement('div');
        dialog.innerHTML = `
            <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                        background: rgba(0,0,0,0.8); z-index: 10001; display: flex;
                        align-items: center; justify-content: center;">
                <div style="background: white; padding: 20px; border-radius: 10px; max-width: 400px;">
                    <h3>EhViewer 广告拦截器设置</h3>
                    <div style="margin: 10px 0;">
                        <label><input type="checkbox" id="enable-adblock" ${config.enabled ? 'checked' : ''}> 启用广告拦截</label><br>
                        <label><input type="checkbox" id="enable-popups" ${config.blockPopups ? 'checked' : ''}> 拦截弹窗</label><br>
                        <label><input type="checkbox" id="enable-trackers" ${config.blockTrackers ? 'checked' : ''}> 拦截跟踪器</label><br>
                        <label><input type="checkbox" id="show-stats" ${config.showStats ? 'checked' : ''}> 显示统计</label>
                    </div>
                    <div style="text-align: right; margin-top: 15px;">
                        <button id="save-settings" style="margin-right: 10px;">保存</button>
                        <button id="close-dialog">关闭</button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(dialog);

        document.getElementById('save-settings').onclick = function() {
            config.enabled = document.getElementById('enable-adblock').checked;
            config.blockPopups = document.getElementById('enable-popups').checked;
            config.blockTrackers = document.getElementById('enable-trackers').checked;
            config.showStats = document.getElementById('show-stats').checked;

            GM_setValue('adBlockEnabled', config.enabled);
            GM_setValue('blockPopups', config.blockPopups);
            GM_setValue('blockTrackers', config.blockTrackers);
            GM_setValue('showStats', config.showStats);

            location.reload();
        };

        document.getElementById('close-dialog').onclick = () => dialog.remove();
    }

    // 初始化
    function init() {
        if (!config.enabled || isWhitelisted(window.location.href)) {
            GM_log('广告拦截器已禁用或当前网站在白名单中');
            return;
        }

        setTimeout(() => {
            applyCosmeticFilters();
            blockElementAds();
            blockPopups();
            blockTrackers();
            interceptRequests();
            createControlPanel();

            // 显示统计信息
            setTimeout(createStatsPanel, 2000);

            GM_log('EhViewer 通用广告拦截器初始化完成');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // 页面卸载时保存统计
    window.addEventListener('beforeunload', saveStats);

})();
