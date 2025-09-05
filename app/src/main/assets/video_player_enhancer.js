// ==UserScript==
// @name         EhViewer 视频播放增强器
// @namespace    http://ehviewer.com/
// @version      2.0.0
// @description  增强视频播放体验：自动画质选择、广告跳过、播放控制、快捷键支持
// @author       EhViewer Team
// @match        *://*.youtube.com/*
// @match        *://*.bilibili.com/*
// @match        *://*.iqiyi.com/*
// @match        *://*.qq.com/*
// @match        *://*.youku.com/*
// @match        *://*.tudou.com/*
// @match        *://*.sohu.com/*
// @match        *://*.pptv.com/*
// @match        *://*.letv.com/*
// @match        *://*.acfun.cn/*
// @match        *://*.nicovideo.jp/*
// @match        *://*.dailymotion.com/*
// @match        *://*.vimeo.com/*
// @match        *://*.twitch.tv/*
// @match        *://*.hulu.com/*
// @match        *://*.netflix.com/*
// @exclude      *://*.google.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 视频播放增强器已启动');

    // 配置选项
    const config = {
        autoQuality: GM_getValue('autoQuality', true),
        preferredQuality: GM_getValue('preferredQuality', '1080p'),
        skipAds: GM_getValue('skipAds', true),
        autoPlay: GM_getValue('autoPlay', false),
        speedControl: GM_getValue('speedControl', true),
        keyboardShortcuts: GM_getValue('keyboardShortcuts', true),
        theaterMode: GM_getValue('theaterMode', false),
        volumeBoost: GM_getValue('volumeBoost', false)
    };

    // 视频网站检测
    const siteConfig = {
        'youtube.com': {
            selectors: {
                video: 'video',
                ads: '.ad-showing, .ytp-ad-overlay-container',
                skipButton: '.ytp-ad-skip-button, .ytp-skip-ad-button',
                qualityMenu: '.ytp-settings-menu .ytp-quality-menu',
                theaterButton: '.ytp-size-button'
            }
        },
        'bilibili.com': {
            selectors: {
                video: 'video',
                ads: '.bilibili-player-video-ad, .ad-report',
                skipButton: '.bilibili-player-video-btn-skip',
                qualityMenu: '.bilibili-player-video-quality-menu',
                theaterButton: '.bilibili-player-video-btn-theater'
            }
        },
        'iqiyi.com': {
            selectors: {
                video: 'video',
                ads: '.iqp-ad, .ad-skip',
                skipButton: '.ad-skip-btn',
                qualityMenu: '.iqp-quality-menu',
                theaterButton: '.iqp-theater-btn'
            }
        }
    };

    // 获取当前网站配置
    function getCurrentSiteConfig() {
        const hostname = window.location.hostname;
        for (const [domain, config] of Object.entries(siteConfig)) {
            if (hostname.includes(domain)) {
                return config;
            }
        }
        return null;
    }

    // 创建增强控制面板
    function createVideoPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-video-panel';
        panel.innerHTML = `
            <div style="position: fixed; bottom: 20px; right: 20px; z-index: 10000;
                        background: rgba(0,0,0,0.8); color: white; padding: 12px;
                        border-radius: 8px; font-size: 12px; font-family: Arial;">
                <div style="display: flex; gap: 8px; margin-bottom: 8px;">
                    <button id="toggle-quality" title="画质选择">🎬</button>
                    <button id="toggle-speed" title="播放速度">⚡</button>
                    <button id="toggle-theater" title="剧场模式">🎭</button>
                    <button id="volume-boost" title="音量增强">🔊</button>
                    <button id="skip-ad" title="跳过广告">⏭️</button>
                </div>
                <div style="font-size: 10px; color: #ccc; text-align: center;">
                    K暂停 | ←→进度 | ↑↓音量 | F全屏
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('toggle-quality').onclick = showQualitySelector;
        document.getElementById('toggle-speed').onclick = showSpeedSelector;
        document.getElementById('toggle-theater').onclick = toggleTheaterMode;
        document.getElementById('volume-boost').onclick = toggleVolumeBoost;
        document.getElementById('skip-ad').onclick = skipCurrentAd;
    }

    // 画质选择器
    function showQualitySelector() {
        const video = document.querySelector('video');
        if (!video) return;

        const qualities = ['144p', '240p', '360p', '480p', '720p', '1080p', '1440p', '2160p'];
        const currentQuality = getCurrentQuality();

        const selector = document.createElement('div');
        selector.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 20px; border-radius: 10px; z-index: 10001;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.3);">
                <h4 style="margin: 0 0 15px 0; color: #333;">选择画质</h4>
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px;">
                    ${qualities.map(quality => `
                        <button style="padding: 8px; border: 1px solid #ddd; border-radius: 5px;
                                       background: ${quality === currentQuality ? '#007bff' : 'white'};
                                       color: ${quality === currentQuality ? 'white' : 'black'};
                                       cursor: pointer;" onclick="setVideoQuality('${quality}')">
                            ${quality}
                        </button>
                    `).join('')}
                </div>
                <button style="width: 100%; margin-top: 15px; padding: 8px; background: #dc3545;
                              color: white; border: none; border-radius: 5px; cursor: pointer;"
                        onclick="this.parentElement.parentElement.remove()">
                    关闭
                </button>
            </div>
        `;

        document.body.appendChild(selector);

        // 添加全局函数
        window.setVideoQuality = setVideoQuality;
    }

    function getCurrentQuality() {
        // 这里需要根据不同网站实现获取当前画质的逻辑
        return '720p'; // 默认值
    }

    function setVideoQuality(quality) {
        const siteConfig = getCurrentSiteConfig();
        if (!siteConfig) return;

        // 根据网站实现画质切换逻辑
        switch (window.location.hostname) {
            case 'youtube.com':
                setYouTubeQuality(quality);
                break;
            case 'bilibili.com':
                setBiliBiliQuality(quality);
                break;
            // 添加其他网站的画质切换逻辑
        }

        GM_setValue('preferredQuality', quality);
        GM_log('已设置视频画质: ' + quality);
    }

    function setYouTubeQuality(quality) {
        // YouTube画质设置逻辑
        const qualityMap = {
            '144p': 'tiny',
            '240p': 'small',
            '360p': 'medium',
            '480p': 'large',
            '720p': 'hd720',
            '1080p': 'hd1080',
            '1440p': 'hd1440',
            '2160p': 'hd2160'
        };

        const qualityCode = qualityMap[quality];
        if (qualityCode) {
            // 模拟点击YouTube设置菜单
            const settingsButton = document.querySelector('.ytp-settings-button');
            if (settingsButton) {
                settingsButton.click();
                setTimeout(() => {
                    const qualityButton = document.querySelector('.ytp-quality-menu .ytp-menuitem');
                    if (qualityButton) {
                        qualityButton.click();
                    }
                }, 100);
            }
        }
    }

    function setBiliBiliQuality(quality) {
        // 哔哩哔哩画质设置逻辑
        const qualityButtons = document.querySelectorAll('.bilibili-player-video-quality-menu-item');
        qualityButtons.forEach(button => {
            if (button.textContent.includes(quality.replace('p', ''))) {
                button.click();
            }
        });
    }

    // 播放速度控制
    function showSpeedSelector() {
        const speeds = [0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0];
        const currentSpeed = getCurrentSpeed();

        const selector = document.createElement('div');
        selector.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 20px; border-radius: 10px; z-index: 10001;">
                <h4 style="margin: 0 0 15px 0;">播放速度</h4>
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px;">
                    ${speeds.map(speed => `
                        <button style="padding: 8px; border: 1px solid #ddd; border-radius: 5px;
                                       background: ${speed === currentSpeed ? '#28a745' : 'white'};
                                       color: ${speed === currentSpeed ? 'white' : 'black'};"
                                onclick="setPlaybackSpeed(${speed})">
                            ${speed}x
                        </button>
                    `).join('')}
                </div>
                <button style="width: 100%; margin-top: 15px; padding: 8px; background: #dc3545;
                              color: white; border: none; border-radius: 5px;"
                        onclick="this.parentElement.parentElement.remove()">
                    关闭
                </button>
            </div>
        `;

        document.body.appendChild(selector);
        window.setPlaybackSpeed = setPlaybackSpeed;
    }

    function getCurrentSpeed() {
        const video = document.querySelector('video');
        return video ? video.playbackRate : 1.0;
    }

    function setPlaybackSpeed(speed) {
        const videos = document.querySelectorAll('video');
        videos.forEach(video => {
            video.playbackRate = speed;
        });
        GM_log('已设置播放速度: ' + speed + 'x');
    }

    // 剧场模式
    function toggleTheaterMode() {
        config.theaterMode = !config.theaterMode;
        GM_setValue('theaterMode', config.theaterMode);

        const siteConfig = getCurrentSiteConfig();
        if (siteConfig && siteConfig.selectors.theaterButton) {
            const theaterButton = document.querySelector(siteConfig.selectors.theaterButton);
            if (theaterButton) {
                theaterButton.click();
            }
        }

        // 通用剧场模式实现
        applyTheaterMode();
    }

    function applyTheaterMode() {
        if (config.theaterMode) {
            GM_addStyle(`
                .ehviewer-theater-mode {
                    position: fixed !important;
                    top: 0 !important;
                    left: 0 !important;
                    width: 100vw !important;
                    height: 100vh !important;
                    background: black !important;
                    z-index: 9999 !important;
                }

                .ehviewer-theater-mode video {
                    width: 100% !important;
                    height: 100% !important;
                    object-fit: contain !important;
                }
            `);

            const videoContainer = document.querySelector('video').parentElement;
            videoContainer.classList.add('ehviewer-theater-mode');
        } else {
            document.querySelector('.ehviewer-theater-mode').classList.remove('ehviewer-theater-mode');
        }
    }

    // 音量增强
    function toggleVolumeBoost() {
        config.volumeBoost = !config.volumeBoost;
        GM_setValue('volumeBoost', config.volumeBoost);

        const videos = document.querySelectorAll('video');
        videos.forEach(video => {
            if (config.volumeBoost) {
                // 创建音频上下文进行音量增强
                enhanceAudio(video);
            } else {
                // 恢复原始音量
                video.volume = Math.min(video.volume, 1.0);
            }
        });
    }

    function enhanceAudio(video) {
        try {
            const audioContext = new (window.AudioContext || window.webkitAudioContext)();
            const source = audioContext.createMediaElementSource(video);
            const gainNode = audioContext.createGain();

            gainNode.gain.value = 1.5; // 1.5倍音量增强
            source.connect(gainNode);
            gainNode.connect(audioContext.destination);

            video.audioEnhanced = true;
            GM_log('音频增强已启用');
        } catch (e) {
            GM_log('音频增强失败: ' + e.message);
        }
    }

    // 广告跳过
    function skipCurrentAd() {
        const siteConfig = getCurrentSiteConfig();
        if (!siteConfig) return;

        // 查找跳过按钮
        const skipSelectors = siteConfig.selectors.skipButton.split(', ');
        for (const selector of skipSelectors) {
            const skipButton = document.querySelector(selector);
            if (skipButton && skipButton.offsetParent !== null) {
                skipButton.click();
                GM_log('已跳过广告');
                return;
            }
        }

        // 如果没有跳过按钮，尝试快进
        const video = document.querySelector('video');
        if (video && video.currentTime < 30) {
            video.currentTime = 30;
            GM_log('已跳过前30秒');
        }
    }

    // 自动跳过广告
    function autoSkipAds() {
        if (!config.skipAds) return;

        const siteConfig = getCurrentSiteConfig();
        if (!siteConfig) return;

        const checkForAds = () => {
            const adElements = document.querySelectorAll(siteConfig.selectors.ads);
            if (adElements.length > 0) {
                GM_log('检测到广告，开始跳过流程');
                skipCurrentAd();
            }
        };

        // 每秒检查一次广告
        setInterval(checkForAds, 1000);
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        if (!config.keyboardShortcuts) return;

        document.addEventListener('keydown', function(e) {
            const video = document.querySelector('video');
            if (!video) return;

            switch (e.keyCode) {
                case 75: // K - 播放/暂停
                    e.preventDefault();
                    video.paused ? video.play() : video.pause();
                    break;
                case 70: // F - 全屏
                    e.preventDefault();
                    if (video.requestFullscreen) {
                        video.requestFullscreen();
                    }
                    break;
                case 77: // M - 静音
                    e.preventDefault();
                    video.muted = !video.muted;
                    break;
                case 37: // ← - 后退10秒
                    e.preventDefault();
                    video.currentTime = Math.max(0, video.currentTime - 10);
                    break;
                case 39: // → - 前进10秒
                    e.preventDefault();
                    video.currentTime = Math.min(video.duration, video.currentTime + 10);
                    break;
                case 38: // ↑ - 音量+
                    e.preventDefault();
                    video.volume = Math.min(1, video.volume + 0.1);
                    break;
                case 40: // ↓ - 音量-
                    e.preventDefault();
                    video.volume = Math.max(0, video.volume - 0.1);
                    break;
                case 67: // C - 字幕
                    e.preventDefault();
                    toggleSubtitles();
                    break;
                case 78: // N - 下一集
                    e.preventDefault();
                    playNextEpisode();
                    break;
            }
        });
    }

    function toggleSubtitles() {
        const subtitleTracks = document.querySelectorAll('track[kind="subtitles"]');
        subtitleTracks.forEach(track => {
            track.mode = track.mode === 'showing' ? 'hidden' : 'showing';
        });
    }

    function playNextEpisode() {
        // 查找下一集按钮
        const nextButtons = document.querySelectorAll(
            'a:contains("下一集"), a:contains("Next"), .next-episode, .episode-next'
        );

        if (nextButtons.length > 0) {
            nextButtons[0].click();
            GM_log('正在播放下一集');
        }
    }

    // 自动播放
    function setupAutoPlay() {
        if (!config.autoPlay) return;

        const video = document.querySelector('video');
        if (video) {
            video.autoplay = true;
            video.play().catch(e => GM_log('自动播放失败: ' + e.message));
        }
    }

    // 播放统计
    function trackPlaybackStats() {
        const video = document.querySelector('video');
        if (!video) return;

        let watchTime = 0;
        const startTime = Date.now();

        const updateStats = () => {
            if (!video.paused) {
                watchTime += 1;
            }
        };

        setInterval(updateStats, 1000);

        video.addEventListener('ended', () => {
            const totalWatchTime = GM_getValue('totalWatchTime', 0) + watchTime;
            GM_setValue('totalWatchTime', totalWatchTime);
            GM_log('观看时长统计已更新: ' + totalWatchTime + '秒');
        });
    }

    // 初始化
    function init() {
        setTimeout(() => {
            createVideoPanel();
            setupKeyboardShortcuts();
            setupAutoPlay();
            autoSkipAds();
            trackPlaybackStats();

            if (config.autoQuality) {
                setVideoQuality(config.preferredQuality);
            }

            GM_log('EhViewer 视频播放增强器初始化完成');
        }, 2000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
