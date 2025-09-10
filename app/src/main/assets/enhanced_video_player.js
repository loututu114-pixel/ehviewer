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

    // 通用视频增强处理
    function enhanceVideoControls() {
        console.log('Applying video controls enhancement');

        // 简化遮挡层移除
        function removeBlockingOverlays() {
            try {
                var overlays = document.querySelectorAll('.overlay, .video-overlay, .ad-overlay');
                overlays.forEach(function(overlay) {
                    if (overlay && overlay.style) {
                        overlay.style.pointerEvents = 'none';
                        overlay.style.display = 'none';
                    }
                });
            } catch (e) {
                console.error('Error removing overlays:', e);
            }
        }

        // 移除遮挡层
        removeBlockingOverlays();

            // 查找播放按钮 - 简化选择器
            var playButtonSelectors = [
                '.play-button',
                '#play-button',
                '.btn-play',
                '.play-icon',
                '.play-btn',
                '.big-play-button',
                '.thumb-play',
                'button[title*="play" i]',
                'div[title*="play" i]'
            ];

            var playButton = null;
            for (var j = 0; j < playButtonSelectors.length; j++) {
                playButton = document.querySelector(playButtonSelectors[j]);
                if (playButton) {
                    console.log('Found play button:', playButtonSelectors[j]);
                    break;
                }
            }

            // 查找全屏按钮
            var fullscreenButtonSelectors = [
                '.fullscreen-button',
                '#fullscreen-button',
                '.btn-fullscreen',
                '#btn-fullscreen',
                '.fullscreen-icon',
                '.fullscreen-btn',
                '[data-fullscreen-button]',
                '.video-fullscreen-button',
                '#video-fullscreen-button',
                '.fullscreen'
            ];

            var fullscreenButton = null;
            for (var k = 0; k < fullscreenButtonSelectors.length; k++) {
                fullscreenButton = document.querySelector(fullscreenButtonSelectors[k]);
                if (fullscreenButton) {
                    console.log('Found XVideos fullscreen button:', fullscreenButtonSelectors[k]);
                    break;
                }
            }

            // 增强播放按钮 - 简化版本
            if (playButton) {
                console.log('Enhancing XVideos play button:', playButton);

                // 针对xvideos优化按钮定位
                if (window.location.hostname.includes('xvideos.com')) {
                    // 设置按钮的绝对定位和样式
                    playButton.style.position = 'absolute';
                    playButton.style.top = '50%';
                    playButton.style.left = '50%';
                    playButton.style.transform = 'translate(-50%, -50%)';
                    playButton.style.zIndex = '10000';
                    playButton.style.pointerEvents = 'auto';
                    playButton.style.cursor = 'pointer';
                    playButton.style.display = 'block';
                    playButton.style.visibility = 'visible';
                    playButton.style.opacity = '1';

                    // 确保按钮在视频容器的正确位置
                    var videoContainer = document.querySelector('.video-player, .video-container, .video-wrapper');
                    if (videoContainer && videoContainer.style) {
                        videoContainer.style.position = 'relative';
                        videoContainer.appendChild(playButton);
                    }

                    // 添加视觉效果
                    playButton.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
                    playButton.style.border = '2px solid rgba(255, 255, 255, 0.8)';
                    playButton.style.borderRadius = '50%';
                    playButton.style.width = '60px';
                    playButton.style.height = '60px';
                    playButton.style.color = 'white';
                    playButton.style.fontSize = '20px';
                    playButton.style.display = 'flex';
                    playButton.style.alignItems = 'center';
                    playButton.style.justifyContent = 'center';

                    console.log('EhViewer: XVideos play button repositioned');
                } else {
                    // 其他网站的默认样式
                    playButton.style.pointerEvents = 'auto';
                    playButton.style.cursor = 'pointer';
                    playButton.style.zIndex = '999999';
                    playButton.style.display = 'block';
                    playButton.style.visibility = 'visible';
                    playButton.style.opacity = '1';
                }

                // 移除disabled属性
                if (playButton.hasAttribute('disabled')) {
                    playButton.removeAttribute('disabled');
                }

                // 简化的点击处理函数
                var simpleClickHandler = function(e) {
                    console.log('XVideos play button clicked (simplified)');

                    e.preventDefault();
                    e.stopPropagation();

                    try {
                        var video = document.querySelector('video');
                        if (!video) {
                            var videoSelectors = ['video', '#video', '.video', '[data-video]'];
                            for (var vs = 0; vs < videoSelectors.length; vs++) {
                                video = document.querySelector(videoSelectors[vs]);
                                if (video) break;
                            }
                        }

                        if (video) {
                            console.log('Found video element, current state:', video.paused ? 'paused' : 'playing');

                            if (video.paused) {
                                // 显示加载进度 - 参考 ByWebView 实现
                                if (typeof Android !== 'undefined' && Android.showLoadingProgress) {
                                    Android.showLoadingProgress(true);
                                }

                                video.play().then(function() {
                                    console.log('Video started successfully');
                                    // 隐藏加载进度
                                    if (typeof Android !== 'undefined' && Android.showLoadingProgress) {
                                        Android.showLoadingProgress(false);
                                    }
                                    // 通知播放状态改变
                                    if (typeof Android !== 'undefined' && Android.onPlayStateChanged) {
                                        Android.onPlayStateChanged(true);
                                    }
                                }).catch(function(error) {
                                    console.error('Video play failed:', error);
                                    // 隐藏加载进度
                                    if (typeof Android !== 'undefined' && Android.showLoadingProgress) {
                                        Android.showLoadingProgress(false);
                                    }
                                    // 尝试静音播放作为备选
                                    video.muted = true;
                                    video.play().then(function() {
                                        console.log('Video started with muted workaround');
                                        if (typeof Android !== 'undefined' && Android.onPlayStateChanged) {
                                            Android.onPlayStateChanged(true);
                                        }
                                    }).catch(function(err2) {
                                        console.error('Muted play also failed:', err2);
                                        if (typeof Android !== 'undefined' && Android.onVideoError) {
                                            Android.onVideoError('播放失败，请检查网络连接');
                                        }
                                    });
                                });
                            } else {
                                video.pause();
                                console.log('Video paused');
                                if (typeof Android !== 'undefined' && Android.onPlayStateChanged) {
                                    Android.onPlayStateChanged(false);
                                }
                            }
                        } else {
                            console.error('No video element found on page');
                            if (typeof Android !== 'undefined' && Android.onVideoError) {
                                Android.onVideoError('未找到视频元素');
                            }
                        }
                    } catch (err) {
                        console.error('Error in play button:', err);
                    }

                    return false;
                };

                // 移除现有事件监听器并重新绑定
                var newButton = playButton.cloneNode(true);
                playButton.parentNode.replaceChild(newButton, playButton);
                playButton = newButton;

                // 重新应用样式
                playButton.style.pointerEvents = 'auto';
                playButton.style.cursor = 'pointer';
                playButton.style.zIndex = '999999';

                // 绑定点击事件
                playButton.addEventListener('click', simpleClickHandler, true);

                console.log('XVideos play button enhanced successfully (simplified)');
            }

            // 增强全屏按钮
            if (fullscreenButton) {
                console.log('Enhancing XVideos fullscreen button:', fullscreenButton);

                // 强制使全屏按钮可访问
                function forceFullscreenButtonAccessible(button) {
                    // 针对xvideos的全屏按钮定位优化
                    if (window.location.hostname.includes('xvideos.com')) {
                        // 设置全屏按钮在视频右下角
                        button.style.setProperty('position', 'absolute', 'important');
                        button.style.setProperty('bottom', '10px', 'important');
                        button.style.setProperty('right', '10px', 'important');
                        button.style.setProperty('z-index', '10000', 'important');
                        button.style.setProperty('pointer-events', 'auto', 'important');
                        button.style.setProperty('cursor', 'pointer', 'important');

                        // 确保按钮可见且可点击
                        button.style.setProperty('display', 'block', 'important');
                        button.style.setProperty('visibility', 'visible', 'important');
                        button.style.setProperty('opacity', '1', 'important');

                        // 添加视觉样式
                        button.style.setProperty('background-color', 'rgba(0, 0, 0, 0.7)', 'important');
                        button.style.setProperty('border', '1px solid rgba(255, 255, 255, 0.5)', 'important');
                        button.style.setProperty('border-radius', '4px', 'important');
                        button.style.setProperty('padding', '8px', 'important');
                        button.style.setProperty('color', 'white', 'important');
                        button.style.setProperty('font-size', '14px', 'important');

                        // 确保按钮在正确的容器中
                        var videoContainer = document.querySelector('.video-player, .video-container, .video-wrapper');
                        if (videoContainer && videoContainer.style) {
                            videoContainer.style.position = 'relative';
                            videoContainer.appendChild(button);
                        }

                        console.log('EhViewer: XVideos fullscreen button repositioned');
                    } else {
                        // 其他网站的默认设置
                        button.style.setProperty('pointer-events', 'auto', 'important');
                        button.style.setProperty('cursor', 'pointer', 'important');
                        button.style.setProperty('z-index', '999999', 'important');
                        button.style.setProperty('display', 'block', 'important');
                        button.style.setProperty('visibility', 'visible', 'important');
                        button.style.setProperty('opacity', '1', 'important');
                        button.style.setProperty('position', 'relative', 'important');
                    }

                    // 移除可能阻挡点击的CSS类
                    if (button.classList) {
                        var blockingClasses = ['disabled', 'inactive', 'blocked', 'overlay-blocked'];
                        blockingClasses.forEach(function(cls) {
                            button.classList.remove(cls);
                        });
                    }

                    // 强制移除disabled属性
                    if (button.hasAttribute('disabled')) {
                        button.removeAttribute('disabled');
                    }

                    console.log('Fullscreen button made forcefully accessible');
                }

                forceFullscreenButtonAccessible(fullscreenButton);

                // 创建增强的全屏处理函数
                var enhancedFullscreenHandler = function(e) {
                    console.log('XVideos fullscreen button clicked (enhanced)');
                    
                    // 立即阻止默认行为和冒泡
                    if (e) {
                        e.preventDefault();
                        e.stopPropagation();
                        e.stopImmediatePropagation();
                    }

                    // 请求全屏
                    try {
                        var video = document.querySelector('video');
                        if (!video) {
                            // 尝试其他视频选择器
                            var videoSelectors = ['video', '#video', '.video', '[data-video]'];
                            for (var vs = 0; vs < videoSelectors.length; vs++) {
                                video = document.querySelector(videoSelectors[vs]);
                                if (video) break;
                            }
                        }

                        var fullscreenRequested = false;

                        // 优先使用Android原生全屏接口
                        if (typeof Android !== 'undefined' && Android.requestFullscreen) {
                            try {
                                Android.requestFullscreen();
                                fullscreenRequested = true;
                                console.log('Fullscreen requested via Android interface');
                            } catch (androidError) {
                                console.warn('Android fullscreen failed:', androidError);
                            }
                        }

                        // 回退到WebView原生全屏API
                        if (!fullscreenRequested && video) {
                            var fullscreenMethods = [
                                'requestFullscreen',
                                'webkitRequestFullscreen', 
                                'webkitRequestFullScreen',
                                'mozRequestFullScreen',
                                'msRequestFullscreen'
                            ];

                            for (var fm = 0; fm < fullscreenMethods.length; fm++) {
                                var method = fullscreenMethods[fm];
                                if (video[method] && typeof video[method] === 'function') {
                                    try {
                                        var result = video[method]();
                                        if (result && typeof result.then === 'function') {
                                            result.then(function() {
                                                console.log('Video fullscreen successful via', method);
                                            }).catch(function(error) {
                                                console.warn('Video fullscreen failed:', error);
                                            });
                                        } else {
                                            console.log('Video fullscreen called via', method);
                                        }
                                        fullscreenRequested = true;
                                        break;
                                    } catch (methodError) {
                                        console.warn('Fullscreen method', method, 'failed:', methodError);
                                        continue;
                                    }
                                }
                            }
                        }

                        // 尝试文档级别全屏作为最后回退
                        if (!fullscreenRequested) {
                            var docFullscreenMethods = [
                                'requestFullscreen',
                                'webkitRequestFullscreen',
                                'mozRequestFullScreen', 
                                'msRequestFullscreen'
                            ];

                            var targetElement = video || document.documentElement;
                            
                            for (var dfm = 0; dfm < docFullscreenMethods.length; dfm++) {
                                var docMethod = docFullscreenMethods[dfm];
                                if (targetElement[docMethod] && typeof targetElement[docMethod] === 'function') {
                                    try {
                                        targetElement[docMethod]();
                                        fullscreenRequested = true;
                                        console.log('Document fullscreen requested via', docMethod);
                                        break;
                                    } catch (docError) {
                                        console.warn('Document fullscreen method', docMethod, 'failed:', docError);
                                        continue;
                                    }
                                }
                            }
                        }

                        if (!fullscreenRequested) {
                            console.error('All fullscreen methods failed');
                            // 通知Android层尝试处理
                            if (typeof Android !== 'undefined' && Android.onVideoError) {
                                Android.onVideoError('Fullscreen not supported');
                            }
                        }

                    } catch (err) {
                        console.error('Error in enhanced fullscreen button:', err);
                        if (typeof Android !== 'undefined' && Android.onVideoError) {
                            Android.onVideoError('Fullscreen error: ' + err.message);
                        }
                    }

                    return false;
                };

                // 移除所有现有的事件监听器
                var newFullscreenButton = fullscreenButton.cloneNode(true);
                fullscreenButton.parentNode.replaceChild(newFullscreenButton, fullscreenButton);
                fullscreenButton = newFullscreenButton;

                // 重新应用样式修复
                forceFullscreenButtonAccessible(fullscreenButton);

                // 绑定多种事件类型确保响应
                var eventTypes = ['click', 'mousedown', 'mouseup', 'touchstart', 'touchend'];
                eventTypes.forEach(function(eventType) {
                    fullscreenButton.addEventListener(eventType, enhancedFullscreenHandler, true);
                });

                // 添加鼠标悬停效果
                fullscreenButton.addEventListener('mouseover', function() {
                    this.style.setProperty('transform', 'scale(1.1)', 'important');
                    this.style.setProperty('transition', 'transform 0.2s', 'important');
                }, false);

                fullscreenButton.addEventListener('mouseout', function() {
                    this.style.setProperty('transform', 'scale(1.0)', 'important');
                }, false);

                console.log('XVideos fullscreen button enhanced successfully with full API support');
            }

            // 增强双击全屏功能作为通用回退
            function enhanceVideoDoubleClickFullscreen() {
                var videos = document.querySelectorAll('video');
                videos.forEach(function(video) {
                    if (!video.hasAttribute('data-fullscreen-enhanced')) {
                        video.setAttribute('data-fullscreen-enhanced', 'true');
                        
                        video.addEventListener('dblclick', function(e) {
                            console.log('Video double-clicked for fullscreen');
                            e.preventDefault();
                            e.stopPropagation();
                            
                            try {
                                var fullscreenSuccess = false;
                                
                                // 优先使用Android全屏接口
                                if (typeof Android !== 'undefined' && Android.requestFullscreen) {
                                    try {
                                        Android.requestFullscreen();
                                        fullscreenSuccess = true;
                                        console.log('Double-click fullscreen via Android interface');
                                    } catch (androidError) {
                                        console.warn('Android double-click fullscreen failed:', androidError);
                                    }
                                }
                                
                                // 原生全屏API回退
                                if (!fullscreenSuccess) {
                                    var methods = [
                                        'requestFullscreen',
                                        'webkitRequestFullscreen',
                                        'webkitRequestFullScreen',
                                        'mozRequestFullScreen',
                                        'msRequestFullscreen'
                                    ];
                                    
                                    for (var m = 0; m < methods.length; m++) {
                                        var method = methods[m];
                                        if (this[method] && typeof this[method] === 'function') {
                                            try {
                                                this[method]();
                                                fullscreenSuccess = true;
                                                console.log('Double-click fullscreen via', method);
                                                break;
                                            } catch (methodError) {
                                                console.warn('Method', method, 'failed:', methodError);
                                                continue;
                                            }
                                        }
                                    }
                                }
                                
                                if (!fullscreenSuccess) {
                                    console.error('All double-click fullscreen methods failed');
                                }
                                
                            } catch (err) {
                                console.error('Error in video double-click fullscreen:', err);
                            }
                        });
                        
                        console.log('Video double-click fullscreen enhanced for video element');
                    }
                });
            }

            // 增强双击全屏
            enhanceVideoDoubleClickFullscreen();
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

        // 应用视频控制增强
        enhanceVideoControls();

        // 通用视频增强
        waitForVideos(function(videos) {
            console.log('EhViewer: Found ' + videos.length + ' video element(s)');

            for (var i = 0; i < videos.length; i++) {
                var video = videos[i];

                // 设置视频属性
                video.setAttribute('controls', 'true');
                video.setAttribute('playsinline', 'false');

                // 针对xvideos优化视频尺寸
                if (window.location.hostname.includes('xvideos.com')) {
                    // 查找视频容器
                    var videoContainer = video.closest('.video-player, .video-container, .video-wrapper, #video-player, #video-container');
                    if (videoContainer) {
                        // 设置容器为相对定位
                        videoContainer.style.position = 'relative';
                        videoContainer.style.width = '100%';
                        videoContainer.style.maxWidth = '100%';
                        videoContainer.style.height = 'auto';
                        videoContainer.style.minHeight = '300px'; // 设置最小高度

                        // 确保容器可见
                        videoContainer.style.display = 'block';
                        videoContainer.style.visibility = 'visible';
                        videoContainer.style.opacity = '1';
                    }

                    // 设置视频本身的尺寸
                    video.style.width = '100%';
                    video.style.height = 'auto';
                    video.style.maxWidth = '100%';
                    video.style.objectFit = 'contain';
                    video.style.display = 'block';

                    // 移除可能限制尺寸的CSS类
                    if (video.classList) {
                        var sizeLimitingClasses = ['video-small', 'video-mini', 'video-thumb', 'video-preview'];
                        sizeLimitingClasses.forEach(function(cls) {
                            video.classList.remove(cls);
                        });
                    }

                    console.log('EhViewer: XVideos video size optimized');
                } else {
                    // 其他网站的默认设置
                    video.style.width = '100%';
                    video.style.height = 'auto';
                }

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
