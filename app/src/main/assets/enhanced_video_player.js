// EhViewer 增强视频播放器脚本
// 专门优化xvideos等成人网站的视频播放体验

(function() {
    'use strict';

    console.log('EhViewer Enhanced Video Player loaded');

    // 防止重复加载
    if (window.EhEnhancedVideoPlayer) {
        console.log('EhEnhancedVideoPlayer already loaded');
        return;
    }
    window.EhEnhancedVideoPlayer = true;

    // 增强的等待机制，专门处理xvideos等成人网站
    function waitForPageReady(callback, retries) {
        retries = retries || 0;
        if (retries > 15) {
            console.log('EhViewer: Page ready timeout, proceeding anyway');
            callback();
            return;
        }

        // 检查多种页面就绪条件
        var isReady = false;

        // 检查DOM基本元素
        if (document.body && document.body.children.length > 0) {
            isReady = true;
        }

        // 检查视频相关元素（针对视频网站）
        if (window.location.hostname.includes('xvideos') ||
            window.location.hostname.includes('pornhub') ||
            window.location.hostname.includes('xhamster')) {
            var videos = document.querySelectorAll('video');
            var playButtons = document.querySelectorAll('.play-button, #play-button, .btn-play');
            if (videos.length > 0 || playButtons.length > 0) {
                isReady = true;
                console.log('EhViewer: Video site elements detected');
            }
        }

        // 检查页面加载状态
        if (document.readyState === 'complete' || document.readyState === 'interactive') {
            isReady = true;
        }

        if (isReady) {
            console.log('EhViewer: Page ready, proceeding with enhancement');
            callback();
        } else {
            console.log('EhViewer: Waiting for page ready, attempt ' + (retries + 1));
            setTimeout(function() { waitForPageReady(callback, retries + 1); }, 800);
        }
    }

    // XVideos特殊处理
    function enhanceXVideos() {
        if (window.location.hostname.includes('xvideos.com') || window.location.hostname.includes('xvideos.es')) {
            console.log('Applying XVideos enhancements');

            // 查找播放按钮
            var playButtonSelectors = [
                '.play-button',
                '#play-button',
                '.btn-play',
                '#btn-play',
                '.play-icon',
                '.play-btn',
                '[data-play-button]',
                '.video-play-button',
                '#video-play-button',
                '.big-play-button',
                '.play-overlay'
            ];

            var playButton = null;
            for (var j = 0; j < playButtonSelectors.length; j++) {
                playButton = document.querySelector(playButtonSelectors[j]);
                if (playButton) {
                    console.log('Found XVideos play button:', playButtonSelectors[j]);
                    break;
                }
            }

            // 增强播放按钮
            if (playButton) {
                console.log('Enhancing XVideos play button');

                // 移除pointer-events限制
                playButton.style.pointerEvents = 'auto';
                playButton.style.cursor = 'pointer';
                playButton.style.zIndex = '9999';

                // 确保按钮可见
                playButton.style.display = 'block';
                playButton.style.visibility = 'visible';
                playButton.style.opacity = '1';

                // 保存原始事件处理
                var originalOnClick = playButton.onclick;

                // 添加增强的点击事件监听器
                playButton.addEventListener('click', function(e) {
                    console.log('XVideos play button clicked (enhanced)');
                    e.preventDefault();
                    e.stopPropagation();

                    // 尝试播放/暂停
                    try {
                        var video = document.querySelector('video');
                        if (video) {
                            if (video.paused) {
                                video.play();
                                console.log('Video started via enhanced play button');
                            } else {
                                video.pause();
                                console.log('Video paused via enhanced play button');
                            }
                        }
                    } catch (err) {
                        console.error('Error in enhanced play button:', err);
                    }

                    // 调用原始事件处理（如果存在）
                    if (originalOnClick) {
                        originalOnClick.call(this, e);
                    }

                    return false;
                }, true); // 使用捕获阶段

                // 移除CSS阻止点击
                var computedStyle = window.getComputedStyle(playButton);
                if (computedStyle.pointerEvents === 'none') {
                    playButton.style.setProperty('pointer-events', 'auto', 'important');
                }

                console.log('XVideos play button enhanced successfully');
            }
        }
    }

    // 等待视频元素
    function waitForVideos(callback, retries) {
        retries = retries || 0;
        if (retries > 10) return;

        var videos = document.querySelectorAll('video');
        if (videos.length > 0) {
            callback(videos);
        } else {
            setTimeout(function() { waitForVideos(callback, retries + 1); }, 1000);
        }
    }

    // 初始化
    waitForPageReady(function() {
        console.log('EhViewer: Initializing enhanced video player');

        // 应用网站特定的增强
        enhanceXVideos();

        // 通用视频增强
        waitForVideos(function(videos) {
            console.log('EhViewer: Found ' + videos.length + ' video element(s)');

            for (var i = 0; i < videos.length; i++) {
                var video = videos[i];

                // 设置视频属性
                video.setAttribute('controls', 'true');
                video.setAttribute('playsinline', 'false');
                video.style.width = '100%';
                video.style.height = 'auto';

                // 添加双击全屏
                video.addEventListener('dblclick', function(e) {
                    e.preventDefault();
                    console.log('EhViewer: Video double-clicked');
                    try {
                        if (typeof Android !== 'undefined' && Android.requestFullscreen) {
                            Android.requestFullscreen();
                        } else {
                            // 原生全屏回退
                            if (this.requestFullscreen) this.requestFullscreen();
                            else if (this.webkitRequestFullscreen) this.webkitRequestFullscreen();
                            else if (this.mozRequestFullScreen) this.mozRequestFullScreen();
                            else if (this.msRequestFullscreen) this.msRequestFullscreen();
                        }
                    } catch (err) {
                        console.error('EhViewer: Fullscreen error:', err);
                    }
                });

                // 添加播放状态监听
                video.addEventListener('play', function() {
                    console.log('EhViewer: Video playing');
                    if (typeof Android !== 'undefined' && Android.onPlayStateChanged) {
                        Android.onPlayStateChanged(true);
                    }
                });

                video.addEventListener('pause', function() {
                    console.log('EhViewer: Video paused');
                    if (typeof Android !== 'undefined' && Android.onPlayStateChanged) {
                        Android.onPlayStateChanged(false);
                    }
                });

                console.log('EhViewer: Enhanced video element', i + 1);
            }
        });
    });

    console.log('EhViewer: Enhanced video player initialization complete');

})();
