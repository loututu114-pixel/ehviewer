// ==UserScript==
// @name         EhViewer 哔哩哔哩增强器
// @namespace    http://ehviewer.com/
// @version      3.0.0
// @description  深度优化哔哩哔哩体验：智能广告拦截、视频画质增强、弹幕优化、播放体验升级
// @author       EhViewer Team
// @match        *://*.bilibili.com/*
// @exclude      *://*.google.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 哔哩哔哩增强器已启动');

    // 高级配置选项
    const config = {
        enabled: GM_getValue('biliEnabled', true),
        adBlock: GM_getValue('biliAdBlock', true),
        qualityBoost: GM_getValue('biliQualityBoost', true),
        danmuOptimize: GM_getValue('biliDanmuOptimize', true),
        autoPlayNext: GM_getValue('biliAutoPlayNext', true),
        skipIntro: GM_getValue('biliSkipIntro', true),
        theaterMode: GM_getValue('biliTheaterMode', false),
        downloadVideo: GM_getValue('biliDownloadVideo', false),
        keyboardShortcuts: GM_getValue('biliKeyboardShortcuts', true),
        autoLike: GM_getValue('biliAutoLike', false),
        hideRecommend: GM_getValue('biliHideRecommend', false),
        volumeBoost: GM_getValue('biliVolumeBoost', false)
    };

    // 创建高级控制面板
    function createAdvancedControlPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-bili-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #00AEEC, #FB7299); color: white;
                        padding: 12px; border-radius: 8px; font-size: 11px; font-family: Arial;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3); min-width: 200px;">
                <div style="display: flex; align-items: center; margin-bottom: 8px;">
                    <img src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJDOC4xMzYgMiA1IDUuMTM2IDUgOThWMTRMMTEuNSA5LjVMMTggMTRWOHoiIGZpbGw9IndoaXRlIi8+Cjwvc3ZnPgo=" style="width: 20px; height: 20px; margin-right: 8px;">
                    <strong>bilibili 增强器</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-bottom: 8px;">
                    <button id="toggle-ads" title="广告拦截" style="padding: 4px 6px; border: none; border-radius: 4px; background: rgba(255,255,255,0.2); color: white;">🚫</button>
                    <button id="toggle-quality" title="画质增强" style="padding: 4px 6px; border: none; border-radius: 4px; background: rgba(255,255,255,0.2); color: white;">🎬</button>
                    <button id="toggle-danmu" title="弹幕优化" style="padding: 4px 6px; border: none; border-radius: 4px; background: rgba(255,255,255,0.2); color: white;">💬</button>
                    <button id="theater-mode" title="剧场模式" style="padding: 4px 6px; border: none; border-radius: 4px; background: rgba(255,255,255,0.2); color: white;">🎭</button>
                    <button id="download-btn" title="下载视频" style="padding: 4px 6px; border: none; border-radius: 4px; background: rgba(255,255,255,0.2); color: white;">💾</button>
                    <button id="settings-btn" title="设置" style="padding: 4px 6px; border: none; border-radius: 4px; background: rgba(255,255,255,0.2); color: white;">⚙️</button>
                </div>
                <div style="font-size: 9px; color: rgba(255,255,255,0.8); text-align: center; border-top: 1px solid rgba(255,255,255,0.2); padding-top: 4px;">
                    D跳过广告 | F全屏 | C弹幕 | N下一集
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('toggle-ads').onclick = () => toggleFeature('adBlock');
        document.getElementById('toggle-quality').onclick = () => toggleFeature('qualityBoost');
        document.getElementById('toggle-danmu').onclick = () => toggleFeature('danmuOptimize');
        document.getElementById('theater-mode').onclick = toggleTheaterMode;
        document.getElementById('download-btn').onclick = showDownloadOptions;
        document.getElementById('settings-btn').onclick = showAdvancedSettings;
    }

    // 深度广告拦截系统
    function advancedAdBlock() {
        if (!config.adBlock) return;

        GM_addStyle(`
            /* Bilibili 深度广告拦截 */
            .ad-report, .video-ad, .banner-ad, .pop-live-small-header,
            .ad-floor, .ad-banner, .ad-video, .commercial,
            .sponsor-card, .recommend-ad, .feed-ad,
            [class*="ad"], [id*="ad"], [data-ad] {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                overflow: hidden !important;
            }

            /* 移除弹窗广告 */
            .bili-modal, .ad-modal, .popup-modal {
                display: none !important;
            }

            /* 隐藏右侧广告栏 */
            .right-container .ad-card,
            .recommend-list .ad-item {
                display: none !important;
            }

            /* 移除视频内广告 */
            .bpx-player-ad, .video-ad-container {
                display: none !important;
            }
        `);

        // 动态移除广告元素
        const adObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === 1) {
                            removeAdsFromElement(node);
                        }
                    });
                }
            });
        });

        adObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        // 处理现有广告
        removeAdsFromElement(document.body);

        // 拦截广告请求
        interceptAdRequests();
    }

    function removeAdsFromElement(root) {
        const adSelectors = [
            '.ad-report', '.video-ad', '.banner-ad', '.pop-live-small-header',
            '.ad-floor', '.ad-banner', '.ad-video', '.commercial',
            '.sponsor-card', '.recommend-ad', '.feed-ad',
            '[class*="ad"]', '[id*="ad"]', '[data-ad]'
        ];

        adSelectors.forEach(selector => {
            const elements = root.querySelectorAll(selector);
            elements.forEach(element => {
                element.remove();
                GM_log('已移除广告元素: ' + selector);
            });
        });
    }

    function interceptAdRequests() {
        // 拦截XMLHttpRequest
        const originalXHR = window.XMLHttpRequest;
        window.XMLHttpRequest = function() {
            const xhr = new originalXHR();
            const originalOpen = xhr.open;

            xhr.open = function(method, url) {
                if (url.includes('cm.bilibili.com') || url.includes('ads') || url.includes('sponsor')) {
                    GM_log('已拦截广告请求: ' + url);
                    return;
                }
                return originalOpen.apply(this, arguments);
            };

            return xhr;
        };
    }

    // 智能画质增强
    function enhanceVideoQuality() {
        if (!config.qualityBoost) return;

        // 监听视频加载
        const videoObserver = new MutationObserver(() => {
            const videoContainer = document.querySelector('.bpx-player-video-wrap');
            if (videoContainer && !videoContainer.dataset.qualityEnhanced) {
                videoContainer.dataset.qualityEnhanced = 'true';

                // 等待播放器加载完成
                setTimeout(() => {
                    autoSelectBestQuality();
                    addQualityIndicator();
                }, 2000);
            }
        });

        videoObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        // 立即尝试提升画质
        autoSelectBestQuality();
    }

    function autoSelectBestQuality() {
        try {
            // 点击画质按钮
            const qualityBtn = document.querySelector('.bpx-player-ctrl-quality');
            if (qualityBtn) {
                qualityBtn.click();

                setTimeout(() => {
                    // 选择最高画质
                    const qualityOptions = document.querySelectorAll('.bpx-player-ctrl-quality-menu-item');
                    let bestOption = null;
                    let maxQuality = 0;

                    qualityOptions.forEach(option => {
                        const text = option.textContent;
                        const quality = extractQualityNumber(text);
                        if (quality > maxQuality) {
                            maxQuality = quality;
                            bestOption = option;
                        }
                    });

                    if (bestOption) {
                        bestOption.click();
                        GM_log('已自动选择最高画质: ' + bestOption.textContent);
                        showQualityNotification(bestOption.textContent);
                    }
                }, 500);
            }
        } catch (e) {
            GM_log('画质增强失败: ' + e.message);
        }
    }

    function extractQualityNumber(text) {
        const match = text.match(/(\d+)P/);
        return match ? parseInt(match[1]) : 0;
    }

    function addQualityIndicator() {
        const player = document.querySelector('.bpx-player-container');
        if (player && !document.getElementById('quality-indicator')) {
            const indicator = document.createElement('div');
            indicator.id = 'quality-indicator';
            indicator.innerHTML = `
                <div style="position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,0.7);
                           color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px;
                           z-index: 1000;">
                    🎬 已优化画质
                </div>
            `;
            player.appendChild(indicator);

            setTimeout(() => indicator.remove(), 3000);
        }
    }

    function showQualityNotification(quality) {
        const notification = document.createElement('div');
        notification.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: linear-gradient(135deg, #00AEEC, #FB7299); color: white;
                        padding: 15px 25px; border-radius: 8px; font-size: 14px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.3); z-index: 10001;">
                🎬 已自动选择画质: ${quality}
            </div>
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 2000);
    }

    // 弹幕深度优化
    function optimizeDanmu() {
        if (!config.danmuOptimize) return;

        GM_addStyle(`
            /* 弹幕基础优化 */
            .danmaku-wrap {
                opacity: 0.8 !important;
                transition: opacity 0.3s ease !important;
            }

            .danmaku-wrap:hover {
                opacity: 1 !important;
            }

            /* 弹幕过滤 */
            .danmaku-item.danmaku-filtered {
                display: none !important;
            }

            /* 弹幕设置面板增强 */
            .bpx-player-dm-setting {
                background: rgba(0,0,0,0.9) !important;
                border-radius: 8px !important;
            }
        `);

        // 添加弹幕控制按钮
        addDanmuControls();

        // 智能弹幕过滤
        setupDanmuFilter();
    }

    function addDanmuControls() {
        const danmuBtn = document.querySelector('.bpx-player-ctrl-danmaku');
        if (danmuBtn && !document.getElementById('danmu-enhancer')) {
            const enhancer = document.createElement('div');
            enhancer.id = 'danmu-enhancer';
            enhancer.innerHTML = `
                <div style="position: absolute; top: -40px; left: 0; background: rgba(0,0,0,0.8);
                           color: white; padding: 8px; border-radius: 4px; font-size: 12px;
                           display: none; z-index: 1000;">
                    <button id="danmu-opacity" style="margin-right: 5px; padding: 2px 6px;">透明</button>
                    <button id="danmu-filter" style="margin-right: 5px; padding: 2px 6px;">过滤</button>
                    <button id="danmu-size" style="padding: 2px 6px;">大小</button>
                </div>
            `;
            danmuBtn.parentElement.style.position = 'relative';
            danmuBtn.parentElement.appendChild(enhancer);

            // 显示/隐藏控制面板
            danmuBtn.addEventListener('mouseenter', () => {
                enhancer.style.display = 'block';
            });

            danmuBtn.addEventListener('mouseleave', () => {
                setTimeout(() => {
                    if (!enhancer.matches(':hover')) {
                        enhancer.style.display = 'none';
                    }
                }, 100);
            });

            // 绑定事件
            document.getElementById('danmu-opacity').onclick = toggleDanmuOpacity;
            document.getElementById('danmu-filter').onclick = toggleDanmuFilter;
            document.getElementById('danmu-size').onclick = adjustDanmuSize;
        }
    }

    function toggleDanmuOpacity() {
        const danmuWrap = document.querySelector('.danmaku-wrap');
        if (danmuWrap) {
            const currentOpacity = danmuWrap.style.opacity || 0.8;
            danmuWrap.style.opacity = currentOpacity === '0.8' ? '0.3' : '0.8';
        }
    }

    function toggleDanmuFilter() {
        const danmuItems = document.querySelectorAll('.danmaku-item');
        danmuItems.forEach(item => {
            const text = item.textContent;
            // 过滤广告弹幕和重复弹幕
            if (text.includes('广告') || text.includes('推广') || text.length > 50) {
                item.classList.add('danmaku-filtered');
            }
        });
    }

    function adjustDanmuSize() {
        const danmuItems = document.querySelectorAll('.danmaku-item');
        danmuItems.forEach(item => {
            const currentSize = parseInt(getComputedStyle(item).fontSize);
            item.style.fontSize = (currentSize === 25 ? 18 : 25) + 'px';
        });
    }

    function setupDanmuFilter() {
        // 监听新弹幕
        const danmuObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.classList && node.classList.contains('danmaku-item')) {
                            filterNewDanmu(node);
                        }
                    });
                }
            });
        });

        const danmuContainer = document.querySelector('.danmaku-wrap');
        if (danmuContainer) {
            danmuObserver.observe(danmuContainer, {
                childList: true,
                subtree: true
            });
        }
    }

    function filterNewDanmu(danmuItem) {
        const text = danmuItem.textContent;
        const filters = [
            /广告|推广|淘宝|微信|QQ|联系方式/i,
            /.{100,}/, // 过长弹幕
            /(.)\1{5,}/, // 重复字符
        ];

        filters.forEach(filter => {
            if (filter.test(text)) {
                danmuItem.classList.add('danmaku-filtered');
            }
        });
    }

    // 剧场模式
    function toggleTheaterMode() {
        config.theaterMode = !config.theaterMode;
        GM_setValue('biliTheaterMode', config.theaterMode);

        if (config.theaterMode) {
            enterTheaterMode();
        } else {
            exitTheaterMode();
        }
    }

    function enterTheaterMode() {
        GM_addStyle(`
            .bpx-player-container {
                position: fixed !important;
                top: 0 !important;
                left: 0 !important;
                width: 100vw !important;
                height: 100vh !important;
                z-index: 9999 !important;
                background: black !important;
            }

            .bpx-player-video-wrap video {
                width: 100% !important;
                height: 100% !important;
                object-fit: contain !important;
            }

            /* 隐藏其他页面元素 */
            header, .nav-bar, .sidebar, .recommend-list,
            .footer, .comment-section {
                display: none !important;
            }
        `);

        // 添加退出按钮
        const exitBtn = document.createElement('button');
        exitBtn.innerHTML = '✕';
        exitBtn.style.cssText = `
            position: fixed; top: 20px; right: 20px; z-index: 10000;
            background: rgba(0,0,0,0.7); color: white; border: none;
            padding: 10px 15px; border-radius: 50%; font-size: 18px;
            cursor: pointer;
        `;
        exitBtn.onclick = toggleTheaterMode;
        document.body.appendChild(exitBtn);
    }

    function exitTheaterMode() {
        // 移除剧场模式样式
        const style = document.querySelector('#ehviewer-theater-style');
        if (style) style.remove();

        // 刷新页面恢复原始布局
        location.reload();
    }

    // 视频下载功能
    function showDownloadOptions() {
        if (!config.downloadVideo) {
            alert('视频下载功能已禁用，请在设置中启用');
            return;
        }

        const video = document.querySelector('video');
        if (!video || !video.src) {
            alert('未找到可下载的视频');
            return;
        }

        const downloadDialog = document.createElement('div');
        downloadDialog.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 20px; border-radius: 10px; z-index: 10001;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.3);">
                <h3 style="margin: 0 0 15px 0; color: #333;">视频下载选项</h3>
                <div style="margin-bottom: 15px;">
                    <label><input type="radio" name="quality" value="current" checked> 当前画质</label><br>
                    <label><input type="radio" name="quality" value="highest"> 最高画质</label><br>
                    <label><input type="radio" name="quality" value="original"> 原始质量</label>
                </div>
                <div style="text-align: right;">
                    <button id="start-download" style="margin-right: 10px; padding: 8px 15px;
                                                    background: #007bff; color: white; border: none;
                                                    border-radius: 5px; cursor: pointer;">
                        开始下载
                    </button>
                    <button id="cancel-download" style="padding: 8px 15px; background: #6c757d;
                                                     color: white; border: none; border-radius: 5px;
                                                     cursor: pointer;">
                        取消
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(downloadDialog);

        document.getElementById('start-download').onclick = () => {
            const quality = document.querySelector('input[name="quality"]:checked').value;
            downloadVideo(video.src, quality);
            downloadDialog.remove();
        };

        document.getElementById('cancel-download').onclick = () => downloadDialog.remove();
    }

    function downloadVideo(src, quality) {
        // 获取视频信息
        const title = document.querySelector('.video-title')?.textContent || 'bilibili_video';
        const filename = `${title}_${quality}_${Date.now()}.mp4`;

        const link = document.createElement('a');
        link.href = src;
        link.download = filename;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        GM_log('开始下载视频: ' + filename);
        showDownloadNotification(filename);
    }

    function showDownloadNotification(filename) {
        const notification = document.createElement('div');
        notification.innerHTML = `
            <div style="position: fixed; bottom: 20px; right: 20px; background: #28a745;
                        color: white; padding: 15px; border-radius: 8px; z-index: 10001;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3);">
                ✅ 正在下载: ${filename}
            </div>
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    // 键盘快捷键增强
    function setupKeyboardShortcuts() {
        if (!config.keyboardShortcuts) return;

        document.addEventListener('keydown', function(e) {
            // 只有在非输入框中才响应快捷键
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            switch (e.keyCode) {
                case 68: // D - 跳过广告
                    if (config.skipIntro) {
                        e.preventDefault();
                        skipCurrentAd();
                    }
                    break;
                case 70: // F - 全屏
                    e.preventDefault();
                    toggleFullscreen();
                    break;
                case 67: // C - 弹幕开关
                    e.preventDefault();
                    toggleDanmu();
                    break;
                case 78: // N - 下一集
                    if (config.autoPlayNext) {
                        e.preventDefault();
                        playNextEpisode();
                    }
                    break;
                case 76: // L - 点赞
                    if (config.autoLike) {
                        e.preventDefault();
                        autoLikeVideo();
                    }
                    break;
                case 82: // R - 重新加载
                    e.preventDefault();
                    location.reload();
                    break;
            }
        });
    }

    function skipCurrentAd() {
        const skipBtn = document.querySelector('.bpx-player-ctrl-btn[aria-label*="跳过"], .skip-button');
        if (skipBtn) {
            skipBtn.click();
            GM_log('已跳过广告');
        } else {
            // 尝试快进
            const video = document.querySelector('video');
            if (video && video.currentTime < 30) {
                video.currentTime = 30;
                GM_log('已跳过前30秒');
            }
        }
    }

    function toggleFullscreen() {
        const video = document.querySelector('video');
        if (video) {
            if (document.fullscreenElement) {
                document.exitFullscreen();
            } else {
                video.requestFullscreen();
            }
        }
    }

    function toggleDanmu() {
        const danmuBtn = document.querySelector('.bpx-player-ctrl-danmaku');
        if (danmuBtn) {
            danmuBtn.click();
        }
    }

    function playNextEpisode() {
        const nextBtn = document.querySelector('.next-button, .recommend-list .video-card:first-child a');
        if (nextBtn) {
            nextBtn.click();
            GM_log('正在播放下一集');
        }
    }

    function autoLikeVideo() {
        const likeBtn = document.querySelector('.like-button, .bpx-player-ctrl-like');
        if (likeBtn && !likeBtn.classList.contains('active')) {
            likeBtn.click();
            GM_log('已自动点赞');
        }
    }

    // 高级设置面板
    function showAdvancedSettings() {
        const settings = document.createElement('div');
        settings.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                        box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 500px; max-height: 80vh;
                        overflow-y: auto;">
                <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">哔哩哔哩增强器设置</h3>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-enabled" style="margin-right: 8px;" ${config.enabled ? 'checked' : ''}>
                        启用增强器
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-adblock" style="margin-right: 8px;" ${config.adBlock ? 'checked' : ''}>
                        广告拦截
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-quality" style="margin-right: 8px;" ${config.qualityBoost ? 'checked' : ''}>
                        画质增强
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-danmu" style="margin-right: 8px;" ${config.danmuOptimize ? 'checked' : ''}>
                        弹幕优化
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-autoplay" style="margin-right: 8px;" ${config.autoPlayNext ? 'checked' : ''}>
                        自动播放下一个
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-skip" style="margin-right: 8px;" ${config.skipIntro ? 'checked' : ''}>
                        跳过片头
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-download" style="margin-right: 8px;" ${config.downloadVideo ? 'checked' : ''}>
                        视频下载
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-keyboard" style="margin-right: 8px;" ${config.keyboardShortcuts ? 'checked' : ''}>
                        键盘快捷键
                    </label>
                </div>

                <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                    <h4 style="margin: 0 0 10px 0; color: #666;">高级选项</h4>
                    <label style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" id="setting-autolike" style="margin-right: 8px;" ${config.autoLike ? 'checked' : ''}>
                        自动点赞
                    </label>
                    <label style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" id="setting-hiderecommend" style="margin-right: 8px;" ${config.hideRecommend ? 'checked' : ''}>
                        隐藏推荐内容
                    </label>
                    <label style="display: block;">
                        <input type="checkbox" id="setting-volumeboost" style="margin-right: 8px;" ${config.volumeBoost ? 'checked' : ''}>
                        音量增强
                    </label>
                </div>

                <div style="text-align: right;">
                    <button id="reset-settings" style="margin-right: 10px; padding: 8px 15px;
                                                    background: #ffc107; color: black; border: none;
                                                    border-radius: 5px; cursor: pointer;">
                        重置设置
                    </button>
                    <button id="save-advanced-settings" style="padding: 8px 15px;
                                                    background: #28a745; color: white; border: none;
                                                    border-radius: 5px; cursor: pointer;">
                        保存设置
                    </button>
                    <button id="close-advanced-settings" style="margin-left: 10px; padding: 8px 15px;
                                                     background: #6c757d; color: white; border: none;
                                                     border-radius: 5px; cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(settings);

        document.getElementById('save-advanced-settings').onclick = saveAdvancedSettings;
        document.getElementById('reset-settings').onclick = resetSettings;
        document.getElementById('close-advanced-settings').onclick = () => settings.remove();
    }

    function saveAdvancedSettings() {
        config.enabled = document.getElementById('setting-enabled').checked;
        config.adBlock = document.getElementById('setting-adblock').checked;
        config.qualityBoost = document.getElementById('setting-quality').checked;
        config.danmuOptimize = document.getElementById('setting-danmu').checked;
        config.autoPlayNext = document.getElementById('setting-autoplay').checked;
        config.skipIntro = document.getElementById('setting-skip').checked;
        config.downloadVideo = document.getElementById('setting-download').checked;
        config.keyboardShortcuts = document.getElementById('setting-keyboard').checked;
        config.autoLike = document.getElementById('setting-autolike').checked;
        config.hideRecommend = document.getElementById('setting-hiderecommend').checked;
        config.volumeBoost = document.getElementById('setting-volumeboost').checked;

        // 保存所有设置
        Object.keys(config).forEach(key => {
            GM_setValue('bili' + key.charAt(0).toUpperCase() + key.slice(1), config[key]);
        });

        // 重新初始化
        location.reload();
    }

    function resetSettings() {
        if (confirm('确定要重置所有设置为默认值吗？')) {
            Object.keys(config).forEach(key => {
                GM_setValue('bili' + key.charAt(0).toUpperCase() + key.slice(1), false);
            });
            config.enabled = true;
            config.adBlock = true;
            config.qualityBoost = true;
            GM_setValue('biliEnabled', true);
            GM_setValue('biliAdBlock', true);
            GM_setValue('biliQualityBoost', true);

            location.reload();
        }
    }

    // 功能开关
    function toggleFeature(featureName) {
        config[featureName] = !config[featureName];
        GM_setValue('bili' + featureName.charAt(0).toUpperCase() + featureName.slice(1), config[featureName]);

        // 重新应用功能
        applyFeatures();

        GM_log(`${featureName} ${config[featureName] ? '已启用' : '已禁用'}`);
    }

    function applyFeatures() {
        if (config.adBlock) advancedAdBlock();
        if (config.qualityBoost) enhanceVideoQuality();
        if (config.danmuOptimize) optimizeDanmu();
        if (config.volumeBoost) enhanceVolume();
        if (config.hideRecommend) hideRecommendations();
    }

    function enhanceVolume() {
        const video = document.querySelector('video');
        if (video && config.volumeBoost) {
            try {
                const audioContext = new (window.AudioContext || window.webkitAudioContext)();
                const source = audioContext.createMediaElementSource(video);
                const gainNode = audioContext.createGain();
                gainNode.gain.value = 1.3;
                source.connect(gainNode);
                gainNode.connect(audioContext.destination);
                video.volumeEnhanced = true;
            } catch (e) {
                GM_log('音量增强失败: ' + e.message);
            }
        }
    }

    function hideRecommendations() {
        GM_addStyle(`
            .recommend-list, .related-video, .feed-card {
                display: none !important;
            }
        `);
    }

    // 初始化
    function init() {
        if (!config.enabled) {
            GM_log('哔哩哔哩增强器已禁用');
            return;
        }

        setTimeout(() => {
            createAdvancedControlPanel();
            applyFeatures();
            setupKeyboardShortcuts();

            GM_log('EhViewer 哔哩哔哩增强器初始化完成');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
