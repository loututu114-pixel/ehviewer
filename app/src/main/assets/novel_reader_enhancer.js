// ==UserScript==
// @name         EhViewer 小说阅读增强器
// @namespace    http://ehviewer.com/
// @version      2.0.0
// @description  优化小说阅读体验：自动翻页、字体优化、广告拦截、夜间模式、阅读进度保存
// @author       EhViewer Team
// @match        *://*/*
// @include      *://*/chapter/*
// @include      *://*/read/*
// @include      *://*/book/*
// @include      *://*/novel/*
// @include      *://*/txt/*
// @include      *://*/content/*
// @exclude      *://*.baidu.com/*
// @exclude      *://*.google.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 小说阅读增强器已启动');

    // 配置选项
    const config = {
        autoScroll: GM_getValue('autoScroll', true),
        nightMode: GM_getValue('nightMode', false),
        fontSize: GM_getValue('fontSize', 18),
        lineHeight: GM_getValue('lineHeight', 1.8),
        hideAds: GM_getValue('hideAds', true),
        autoSaveProgress: GM_getValue('autoSaveProgress', true),
        keyboardShortcuts: GM_getValue('keyboardShortcuts', true)
    };

    // 创建控制面板
    function createControlPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-novel-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: rgba(0,0,0,0.8); color: white; padding: 10px;
                        border-radius: 5px; font-size: 12px; font-family: Arial;">
                <div style="margin-bottom: 5px;">
                    <button id="toggle-night" style="margin-right: 5px;">🌙</button>
                    <button id="font-larger" style="margin-right: 5px;">A+</button>
                    <button id="font-smaller" style="margin-right: 5px;">A-</button>
                    <button id="toggle-autoscroll">📖</button>
                </div>
                <div style="font-size: 10px; color: #ccc;">
                    空格键翻页 | ↑↓滚动 | ESC隐藏
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('toggle-night').onclick = toggleNightMode;
        document.getElementById('font-larger').onclick = () => adjustFontSize(2);
        document.getElementById('font-smaller').onclick = () => adjustFontSize(-2);
        document.getElementById('toggle-autoscroll').onclick = toggleAutoScroll;
    }

    // 优化页面布局
    function optimizeLayout() {
        GM_addStyle(`
            .ehviewer-optimized {
                max-width: 800px !important;
                margin: 0 auto !important;
                padding: 20px !important;
                line-height: ${config.lineHeight} !important;
                font-size: ${config.fontSize}px !important;
                font-family: 'Microsoft YaHei', 'PingFang SC', 'Hiragino Sans GB', sans-serif !important;
                text-align: justify !important;
                background: ${config.nightMode ? '#1a1a1a' : 'white'} !important;
                color: ${config.nightMode ? '#e0e0e0' : 'black'} !important;
            }

            .ehviewer-hide {
                display: none !important;
            }

            .ehviewer-night {
                background: #1a1a1a !important;
                color: #e0e0e0 !important;
            }

            .ehviewer-night a {
                color: #4da6ff !important;
            }

            .ehviewer-night img {
                filter: brightness(0.8) contrast(1.1) !important;
            }
        `);

        // 识别主要内容区域
        const contentSelectors = [
            '.content', '.chapter-content', '.read-content', '.novel-content',
            '.txt', '.book-content', '.article-content', '.story-content',
            '[class*="content"]', '[id*="content"]',
            'article', 'main', '.main-content'
        ];

        let mainContent = null;
        for (let selector of contentSelectors) {
            const elements = document.querySelectorAll(selector);
            for (let element of elements) {
                if (element.textContent.length > 500) { // 内容足够长的区域
                    mainContent = element;
                    break;
                }
            }
            if (mainContent) break;
        }

        if (mainContent) {
            mainContent.classList.add('ehviewer-optimized');
            GM_log('已优化小说内容区域');
        }
    }

    // 移除广告和干扰元素
    function removeAds() {
        const adSelectors = [
            '.ads', '.advertisement', '.ad-banner', '.ad-container',
            '.popup-ad', '.overlay-ad', '.sponsor', '.promotion',
            '[class*="ad"]', '[id*="ad"]', '[class*="advert"]',
            '[class*="popup"]', '[class*="modal"]', '.announcement',
            '.watermark', '.copyright-notice', '.download-link'
        ];

        adSelectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                element.classList.add('ehviewer-hide');
            });
        });

        // 移除固定定位的广告
        const fixedElements = document.querySelectorAll('*[style*="position: fixed"], *[style*="position: absolute"]');
        fixedElements.forEach(element => {
            const rect = element.getBoundingClientRect();
            if (rect.width > 300 && rect.height > 100) { // 大尺寸的固定元素可能是广告
                element.classList.add('ehviewer-hide');
            }
        });
    }

    // 夜间模式切换
    function toggleNightMode() {
        config.nightMode = !config.nightMode;
        GM_setValue('nightMode', config.nightMode);

        document.body.classList.toggle('ehviewer-night', config.nightMode);

        const content = document.querySelector('.ehviewer-optimized');
        if (content) {
            content.style.background = config.nightMode ? '#1a1a1a' : 'white';
            content.style.color = config.nightMode ? '#e0e0e0' : 'black';
        }

        document.getElementById('toggle-night').textContent = config.nightMode ? '☀️' : '🌙';
    }

    // 调整字体大小
    function adjustFontSize(delta) {
        config.fontSize = Math.max(12, Math.min(32, config.fontSize + delta));
        GM_setValue('fontSize', config.fontSize);

        const content = document.querySelector('.ehviewer-optimized');
        if (content) {
            content.style.fontSize = config.fontSize + 'px';
        }
    }

    // 自动滚动
    let autoScrollInterval = null;
    function toggleAutoScroll() {
        config.autoScroll = !config.autoScroll;
        GM_setValue('autoScroll', config.autoScroll);

        if (config.autoScroll) {
            startAutoScroll();
            document.getElementById('toggle-autoscroll').textContent = '⏸️';
        } else {
            stopAutoScroll();
            document.getElementById('toggle-autoscroll').textContent = '📖';
        }
    }

    function startAutoScroll() {
        if (autoScrollInterval) clearInterval(autoScrollInterval);

        autoScrollInterval = setInterval(() => {
            const content = document.querySelector('.ehviewer-optimized');
            if (content) {
                const scrollSpeed = GM_getValue('scrollSpeed', 1);
                window.scrollBy(0, scrollSpeed);

                // 检查是否到达底部
                if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 100) {
                    // 尝试自动翻页
                    autoTurnPage();
                }
            }
        }, 100);
    }

    function stopAutoScroll() {
        if (autoScrollInterval) {
            clearInterval(autoScrollInterval);
            autoScrollInterval = null;
        }
    }

    // 自动翻页
    function autoTurnPage() {
        const nextPageSelectors = [
            '.next-page', '.next-chapter', '.next', '[rel="next"]',
            'a:contains("下一页")', 'a:contains("下章")',
            'a:contains("下一章")', 'a:contains("继续阅读")'
        ];

        for (let selector of nextPageSelectors) {
            const link = document.querySelector(selector);
            if (link && link.href) {
                GM_log('自动翻页到: ' + link.href);
                saveReadingProgress();
                window.location.href = link.href;
                return;
            }
        }
    }

    // 阅读进度保存
    function saveReadingProgress() {
        if (!config.autoSaveProgress) return;

        const progress = {
            url: window.location.href,
            scrollTop: window.scrollY,
            timestamp: Date.now(),
            title: document.title
        };

        GM_setValue('reading_progress_' + encodeURIComponent(window.location.href), progress);
        GM_log('阅读进度已保存');
    }

    function loadReadingProgress() {
        const progress = GM_getValue('reading_progress_' + encodeURIComponent(window.location.href));
        if (progress && Date.now() - progress.timestamp < 7 * 24 * 60 * 60 * 1000) { // 7天内有效
            setTimeout(() => {
                window.scrollTo(0, progress.scrollTop);
                GM_log('已恢复阅读进度');
            }, 1000);
        }
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        document.addEventListener('keydown', function(e) {
            if (!config.keyboardShortcuts) return;

            switch (e.keyCode) {
                case 32: // 空格键 - 翻页
                    e.preventDefault();
                    autoTurnPage();
                    break;
                case 38: // ↑ - 向上滚动
                    e.preventDefault();
                    window.scrollBy(0, -50);
                    break;
                case 40: // ↓ - 向下滚动
                    e.preventDefault();
                    window.scrollBy(0, 50);
                    break;
                case 78: // N - 夜间模式
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        toggleNightMode();
                    }
                    break;
                case 27: // ESC - 隐藏控制面板
                    e.preventDefault();
                    toggleControlPanel();
                    break;
            }
        });
    }

    // 控制面板显示切换
    function toggleControlPanel() {
        const panel = document.getElementById('ehviewer-novel-panel');
        if (panel) {
            panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
        }
    }

    // 初始化
    function init() {
        // 延迟执行，确保页面加载完成
        setTimeout(() => {
            optimizeLayout();
            if (config.hideAds) removeAds();
            createControlPanel();
            setupKeyboardShortcuts();
            loadReadingProgress();

            if (config.nightMode) {
                document.body.classList.add('ehviewer-night');
            }

            // 定期保存进度
            setInterval(saveReadingProgress, 30000); // 每30秒保存一次

            GM_log('EhViewer 小说阅读增强器初始化完成');
        }, 1000);
    }

    // 页面卸载时保存进度
    window.addEventListener('beforeunload', saveReadingProgress);

    // 启动脚本
    init();

})();
