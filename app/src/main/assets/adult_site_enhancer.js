// ==UserScript==
// @name         EhViewer 成人网站增强器
// @namespace    http://ehviewer.com/
// @version      2.0.0
// @description  优化成人网站体验：广告拦截、视频优化、隐私保护、快捷键支持
// @author       EhViewer Team
// @match        *://*.pornhub.com/*
// @match        *://*.xvideos.com/*
// @match        *://*.xhamster.com/*
// @match        *://*.xnxx.com/*
// @match        *://*.youporn.com/*
// @match        *://*.redtube.com/*
// @match        *://*.tube8.com/*
// @match        *://*.spankbang.com/*
// @match        *://*.adultfriendfinder.com/*
// @match        *://*.onlyfans.com/*
// @match        *://*.manyvids.com/*
// @match        *://*.brazzers.com/*
// @match        *://*.realitykings.com/*
// @match        *://*.naughtyamerica.com/*
// @exclude      *://*.google.com/*
// @exclude      *://*.facebook.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 成人网站增强器已启动');

    // 配置选项
    const config = {
        blockAds: GM_getValue('blockAds', true),
        autoPlay: GM_getValue('autoPlay', false),
        hdQuality: GM_getValue('hdQuality', true),
        skipIntro: GM_getValue('skipIntro', true),
        keyboardShortcuts: GM_getValue('keyboardShortcuts', true),
        privacyMode: GM_getValue('privacyMode', true),
        imageOptimization: GM_getValue('imageOptimization', true)
    };

    // 创建增强控制面板
    function createEnhancementPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-adult-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; left: 10px; z-index: 10001;
                        background: rgba(0,0,0,0.8); color: white; padding: 8px;
                        border-radius: 5px; font-size: 11px; font-family: Arial; max-width: 200px;">
                <div style="display: flex; flex-wrap: wrap; gap: 3px; margin-bottom: 5px;">
                    <button id="toggle-ads" title="广告拦截" style="padding: 2px 6px;">🚫</button>
                    <button id="toggle-hd" title="HD画质" style="padding: 2px 6px;">HD</button>
                    <button id="skip-intro" title="跳过片头" style="padding: 2px 6px;">⏭️</button>
                    <button id="fullscreen" title="全屏" style="padding: 2px 6px;">⛶</button>
                </div>
                <div style="font-size: 9px; color: #ccc; line-height: 1.2;">
                    P暂停 | F全屏 | M静音<br/>
                    ↑音量+ | ↓音量- | ESC隐藏
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('toggle-ads').onclick = toggleAdBlock;
        document.getElementById('toggle-hd').onclick = toggleHDQuality;
        document.getElementById('skip-intro').onclick = skipIntroVideo;
        document.getElementById('fullscreen').onclick = toggleFullscreen;
    }

    // 高级广告拦截
    function advancedAdBlock() {
        if (!config.blockAds) return;

        GM_addStyle(`
            /* 通用广告拦截 */
            .ad, .ads, .advertisement, .ad-banner, .ad-container,
            .popup, .modal, .overlay, .sponsor, .promotion,
            [class*="ad"], [id*="ad"], [class*="popup"], [class*="modal"],
            [class*="overlay"], [class*="sponsor"], [data-ad], [data-popup] {
                display: none !important;
                visibility: hidden !important;
                opacity: 0 !important;
            }

            /* Pornhub 特定广告 */
            .ad-link, .videoAdUi, .adContainer, .preRollContainer,
            #pb_template, .header-ad, .sidebar-ad, .footer-ad {
                display: none !important;
            }

            /* Xvideos 广告 */
            .adserver, .videoad, .banner, .sponsered,
            .adsbytrafficjunky, .fake-player {
                display: none !important;
            }

            /* 通用弹窗拦截 */
            .popup-overlay, .modal-overlay, .lightbox-overlay {
                display: none !important;
            }

            /* 移除下载提示 */
            .download-prompt, .app-banner, .mobile-app {
                display: none !important;
            }
        `);

        // 动态移除广告元素
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === 1) { // Element节点
                            removeDynamicAds(node);
                        }
                    });
                }
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        // 移除现有广告
        removeDynamicAds(document.body);
    }

    function removeDynamicAds(root) {
        const adSelectors = [
            '.ad', '.ads', '.advertisement', '.ad-banner', '.ad-container',
            '.popup', '.modal', '.overlay', '.sponsor', '.promotion',
            '[class*="ad"]', '[id*="ad"]', '[data-ad]', '[data-popup]',
            '.videoAdUi', '.preRollContainer', '.adserver', '.videoad'
        ];

        adSelectors.forEach(selector => {
            const elements = root.querySelectorAll(selector);
            elements.forEach(element => {
                element.style.display = 'none';
                element.remove();
            });
        });
    }

    // 视频播放优化
    function optimizeVideoPlayback() {
        // 自动播放设置
        if (config.autoPlay) {
            setTimeout(() => {
                const videos = document.querySelectorAll('video');
                videos.forEach(video => {
                    video.autoplay = true;
                    video.muted = false; // 取消静音以允许自动播放
                    video.play().catch(e => GM_log('自动播放失败: ' + e.message));
                });
            }, 2000);
        }

        // HD画质选择
        if (config.hdQuality) {
            selectBestQuality();
        }

        // 跳过片头
        if (config.skipIntro) {
            skipIntroVideo();
        }
    }

    function selectBestQuality() {
        // Pornhub HD选择
        const qualityButtons = document.querySelectorAll('.quality-button, .resolution-button, [data-quality]');
        qualityButtons.forEach(button => {
            const quality = button.textContent || button.dataset.quality;
            if (quality && (quality.includes('1080') || quality.includes('720') || quality.includes('HD'))) {
                button.click();
                GM_log('已选择HD画质: ' + quality);
            }
        });

        // Xvideos 画质选择
        const qualitySelectors = document.querySelectorAll('select[name*="quality"], select[class*="quality"]');
        qualitySelectors.forEach(select => {
            for (let i = 0; i < select.options.length; i++) {
                const option = select.options[i];
                if (option.text.includes('1080') || option.text.includes('HD')) {
                    select.selectedIndex = i;
                    select.dispatchEvent(new Event('change'));
                    GM_log('已选择HD画质');
                    break;
                }
            }
        });
    }

    function skipIntroVideo() {
        // 查找跳过按钮
        const skipButtons = document.querySelectorAll(
            'button:contains("跳过"), button:contains("Skip"), .skip-button, .skip-intro, [data-skip]'
        );

        skipButtons.forEach(button => {
            if (button.offsetParent !== null) { // 可见的按钮
                button.click();
                GM_log('已跳过片头视频');
            }
        });

        // 自动快进前30秒
        const videos = document.querySelectorAll('video');
        videos.forEach(video => {
            if (video.currentTime < 30) {
                video.currentTime = 30;
                GM_log('已跳过前30秒');
            }
        });
    }

    // 图片优化
    function optimizeImages() {
        if (!config.imageOptimization) return;

        const images = document.querySelectorAll('img[data-src], img[data-original], img[data-lazy]');
        images.forEach(img => {
            const lazySrc = img.dataset.src || img.dataset.original || img.dataset.lazy;
            if (lazySrc && !img.src) {
                img.src = lazySrc;
                img.classList.add('ehviewer-loaded');
            }
        });

        // 预加载下一页图片
        preloadNextPageImages();
    }

    function preloadNextPageImages() {
        const nextPageLinks = document.querySelectorAll('a:contains("下一页"), a:contains("Next"), .next-page');
        if (nextPageLinks.length > 0) {
            const link = nextPageLinks[0];
            if (link.href) {
                // 预加载下一页的图片（这里只是示例，实际实现可能需要服务器端支持）
                GM_log('发现下一页链接: ' + link.href);
            }
        }
    }

    // 隐私保护
    function enhancePrivacy() {
        if (!config.privacyMode) return;

        // 移除追踪脚本
        const trackingScripts = document.querySelectorAll('script[src*="google-analytics"], script[src*="facebook"], script[src*="tracking"]');
        trackingScripts.forEach(script => script.remove());

        // 阻止第三方Cookie
        document.cookie.split(';').forEach(cookie => {
            const [name] = cookie.split('=');
            if (name.includes('__utm') || name.includes('_ga') || name.includes('fb')) {
                document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/;';
            }
        });

        // 添加隐私提示
        const privacyNotice = document.createElement('div');
        privacyNotice.innerHTML = `
            <div style="position: fixed; bottom: 10px; right: 10px; z-index: 10000;
                        background: rgba(255,0,0,0.8); color: white; padding: 8px;
                        border-radius: 5px; font-size: 11px; cursor: pointer;">
                🔒 隐私模式已启用
            </div>
        `;
        privacyNotice.onclick = () => privacyNotice.remove();
        document.body.appendChild(privacyNotice);

        setTimeout(() => privacyNotice.remove(), 5000);
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        if (!config.keyboardShortcuts) return;

        document.addEventListener('keydown', function(e) {
            const video = document.querySelector('video');

            switch (e.keyCode) {
                case 32: // 空格 - 播放/暂停
                    e.preventDefault();
                    if (video) {
                        video.paused ? video.play() : video.pause();
                    }
                    break;
                case 70: // F - 全屏
                    e.preventDefault();
                    toggleFullscreen();
                    break;
                case 77: // M - 静音
                    e.preventDefault();
                    if (video) {
                        video.muted = !video.muted;
                    }
                    break;
                case 38: // ↑ - 音量+
                    e.preventDefault();
                    if (video && video.volume < 1) {
                        video.volume = Math.min(1, video.volume + 0.1);
                    }
                    break;
                case 40: // ↓ - 音量-
                    e.preventDefault();
                    if (video && video.volume > 0) {
                        video.volume = Math.max(0, video.volume - 0.1);
                    }
                    break;
                case 27: // ESC - 退出全屏/隐藏面板
                    e.preventDefault();
                    if (document.fullscreenElement) {
                        document.exitFullscreen();
                    } else {
                        togglePanelVisibility();
                    }
                    break;
                case 78: // N - 下一视频
                    e.preventDefault();
                    playNextVideo();
                    break;
                case 80: // P - 上一视频
                    e.preventDefault();
                    playPreviousVideo();
                    break;
            }
        });
    }

    // 切换功能
    function toggleAdBlock() {
        config.blockAds = !config.blockAds;
        GM_setValue('blockAds', config.blockAds);
        location.reload();
    }

    function toggleHDQuality() {
        config.hdQuality = !config.hdQuality;
        GM_setValue('hdQuality', config.hdQuality);
        selectBestQuality();
    }

    function toggleFullscreen() {
        if (!document.fullscreenElement) {
            const video = document.querySelector('video');
            if (video) {
                video.requestFullscreen();
            } else {
                document.documentElement.requestFullscreen();
            }
        } else {
            document.exitFullscreen();
        }
    }

    function togglePanelVisibility() {
        const panel = document.getElementById('ehviewer-adult-panel');
        if (panel) {
            panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
        }
    }

    function playNextVideo() {
        const nextButtons = document.querySelectorAll('a:contains("下一"), a:contains("Next"), .next-video');
        if (nextButtons.length > 0) {
            nextButtons[0].click();
        }
    }

    function playPreviousVideo() {
        const prevButtons = document.querySelectorAll('a:contains("上一"), a:contains("Previous"), .prev-video');
        if (prevButtons.length > 0) {
            prevButtons[0].click();
        }
    }

    // 性能优化
    function performanceOptimization() {
        // 延迟加载非关键资源
        const lazyElements = document.querySelectorAll('img[data-src], iframe[data-src]');
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const element = entry.target;
                    if (element.dataset.src) {
                        element.src = element.dataset.src;
                        element.classList.remove('lazy');
                        observer.unobserve(element);
                    }
                }
            });
        });

        lazyElements.forEach(element => observer.observe(element));

        // 移除不必要的脚本
        const unnecessaryScripts = document.querySelectorAll('script[src*="analytics"], script[src*="tracking"]');
        unnecessaryScripts.forEach(script => script.remove());
    }

    // 初始化
    function init() {
        setTimeout(() => {
            createEnhancementPanel();
            advancedAdBlock();
            optimizeVideoPlayback();
            optimizeImages();
            enhancePrivacy();
            setupKeyboardShortcuts();
            performanceOptimization();

            GM_log('EhViewer 成人网站增强器初始化完成');
        }, 1500);
    }

    // 页面加载完成后的初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
