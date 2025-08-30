package com.hippo.ehviewer.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import com.hippo.ehviewer.R;

/**
 * 视频播放增强器
 * 为EhViewer提供完整的视频播放体验
 */
public class VideoPlayerEnhancer {
    private static final String TAG = "VideoPlayerEnhancer";

    private final Context context;
    private final Activity activity;
    private WebView webView;
    private boolean isFullscreen = false;
    private FrameLayout fullscreenContainer;
    private View customView;

    // 屏幕方向状态
    private int originalOrientation;
    private boolean isLandscapeForced = false;

    // 音频管理
    private AudioManager audioManager;
    private int originalVolume = -1;

    // JavaScript注入内容
    private static final String VIDEO_ENHANCER_SCRIPT = "" +
        "console.log('EhViewer Video Enhancer loaded');" +
        "" +
        "// 检测视频元素" +
        "function detectVideoElements() {" +
        "    var videos = document.querySelectorAll('video');" +
        "    console.log('Found ' + videos.length + ' video elements');" +
        "    return videos.length;" +
        "}" +
        "" +
        "// 增强视频播放器" +
        "function enhanceVideoPlayers() {" +
        "    var videos = document.querySelectorAll('video');" +
        "    for (var i = 0; i < videos.length; i++) {" +
        "        var video = videos[i];" +
        "        " +
        "        // 添加EhViewer标记" +
        "        video.setAttribute('data-ehviewer-enhanced', 'true');" +
        "        " +
        "        // 启用全屏API" +
        "        video.setAttribute('playsinline', 'false');" +
        "        video.setAttribute('webkit-playsinline', 'false');" +
        "        " +
        "        // 设置控制属性" +
        "        video.setAttribute('controls', 'true');" +
        "        video.setAttribute('preload', 'metadata');" +
        "        " +
        "        // 添加事件监听器" +
        "        video.addEventListener('play', function() {" +
        "            console.log('Video started playing');" +
        "            window.VideoEnhancer.onVideoPlay();" +
        "        });" +
        "        " +
        "        video.addEventListener('pause', function() {" +
        "            console.log('Video paused');" +
        "            window.VideoEnhancer.onVideoPause();" +
        "        });" +
        "        " +
        "        video.addEventListener('ended', function() {" +
        "            console.log('Video ended');" +
        "            window.VideoEnhancer.onVideoEnd();" +
        "        });" +
        "        " +
        "        // 双击全屏" +
        "        video.addEventListener('dblclick', function() {" +
        "            console.log('Video double-clicked, requesting fullscreen');" +
        "            if (video.requestFullscreen) {" +
        "                video.requestFullscreen();" +
        "            } else if (video.webkitRequestFullscreen) {" +
        "                video.webkitRequestFullscreen();" +
        "            } else if (video.mozRequestFullScreen) {" +
        "                video.mozRequestFullScreen();" +
        "            } else if (video.msRequestFullscreen) {" +
        "                video.msRequestFullscreen();" +
        "            } else {" +
        "                // 回退到EhViewer全屏" +
        "                window.VideoEnhancer.requestFullscreen();" +
        "            }" +
        "        });" +
        "    }" +
        "}" +
        "" +
        "// XVideos特殊处理" +
        "function enhanceXVideos() {" +
        "    if (window.location.hostname.includes('xvideos.com')) {" +
        "        console.log('Applying XVideos enhancements');" +
        "        " +
        "        // 查找播放器容器" +
        "        var playerContainer = document.querySelector('#player');" +
        "        if (playerContainer) {" +
        "            playerContainer.style.position = 'relative';" +
        "            " +
        "            // 添加EhViewer全屏按钮" +
        "            var fullscreenBtn = document.createElement('button');" +
        "            fullscreenBtn.innerHTML = '⛶';" +
        "            fullscreenBtn.style.cssText = " +
        "                'position: absolute; top: 10px; right: 10px; z-index: 1000; " +
        "                 background: rgba(0,0,0,0.7); color: white; border: none; " +
        "                 width: 40px; height: 40px; border-radius: 50%; " +
        "                 font-size: 18px; cursor: pointer;';" +
        "            fullscreenBtn.onclick = function() {" +
        "                window.VideoEnhancer.requestFullscreen();" +
        "                return false;" +
        "            };" +
        "            playerContainer.appendChild(fullscreenBtn);" +
        "        }" +
        "    }" +
        "}" +
        "" +
        "// Pornhub特殊处理" +
        "function enhancePornhub() {" +
        "    if (window.location.hostname.includes('pornhub.com')) {" +
        "        console.log('Applying Pornhub enhancements');" +
        "        " +
        "        // 查找播放器" +
        "        setTimeout(function() {" +
        "            var player = document.querySelector('#player');" +
        "            if (player) {" +
        "                // 添加全屏事件监听" +
        "                player.addEventListener('click', function(e) {" +
        "                    if (e.target.classList.contains('fullscreen-button')) {" +
        "                        window.VideoEnhancer.requestFullscreen();" +
        "                    }" +
        "                });" +
        "            }" +
        "        }, 2000);" +
        "    }" +
        "}" +
        "" +
        "// YouTube特殊处理" +
        "function enhanceYouTube() {" +
        "    if (window.location.hostname.includes('youtube.com') || " +
        "        window.location.hostname.includes('youtu.be')) {" +
        "        console.log('Applying YouTube enhancements');" +
        "        " +
        "        // 等待播放器加载" +
        "        setTimeout(function() {" +
        "            var player = document.querySelector('.html5-video-player');" +
        "            if (player) {" +
        "                // 增强全屏按钮" +
        "                var fullscreenBtn = player.querySelector('.ytp-fullscreen-button');" +
        "                if (fullscreenBtn) {" +
        "                    fullscreenBtn.addEventListener('click', function() {" +
        "                        setTimeout(function() {" +
        "                            window.VideoEnhancer.onFullscreenChange(true);" +
        "                        }, 100);" +
        "                    });" +
        "                }" +
        "            }" +
        "        }, 3000);" +
        "    }" +
        "}" +
        "" +
        "// 通用网页播放器支持 (JW Player, Video.js, Plyr等)" +
        "function enhanceWebPlayers() {" +
        "    console.log('Enhancing web players');" +
        "    " +
        "    // JW Player" +
        "    if (typeof jwplayer !== 'undefined') {" +
        "        console.log('JW Player detected');" +
        "        jwplayer().on('ready', function() {" +
        "            var player = jwplayer();" +
        "            player.addButton('⛶', 'EhViewer全屏', function() {" +
        "                window.VideoEnhancer.requestFullscreen();" +
        "            }, 'fullscreen-btn');" +
        "        });" +
        "    }" +
        "    " +
        "    // Video.js" +
        "    if (typeof videojs !== 'undefined') {" +
        "        console.log('Video.js detected');" +
        "        videojs(document.querySelector('.video-js')).ready(function() {" +
        "            var player = this;" +
        "            player.addChild('button', {" +
        "                text: '⛶'," +
        "                className: 'ehviewer-fullscreen-btn'" +
        "            }).el_.onclick = function() {" +
        "                window.VideoEnhancer.requestFullscreen();" +
        "            };" +
        "        });" +
        "    }" +
        "    " +
        "    // Plyr" +
        "    if (typeof Plyr !== 'undefined') {" +
        "        console.log('Plyr detected');" +
        "        document.addEventListener('DOMContentLoaded', function() {" +
        "            var players = Plyr.setup();" +
        "            players.forEach(function(player) {" +
        "                player.on('ready', function() {" +
        "                    // Plyr已经有全屏支持，这里添加额外控制" +
        "                    window.VideoEnhancer.onPlayerReady('plyr');" +
        "                });" +
        "            });" +
        "        });" +
        "    }" +
        "}" +
        "" +
        "// 初始化函数" +
        "function initVideoEnhancer() {" +
        "    console.log('Initializing EhViewer Video Enhancer');" +
        "    " +
        "    // 创建EhViewer接口" +
        "    window.VideoEnhancer = {" +
        "        onVideoPlay: function() {" +
        "            console.log('Video play event from web');" +
        "        }," +
        "        onVideoPause: function() {" +
        "            console.log('Video pause event from web');" +
        "        }," +
        "        onVideoEnd: function() {" +
        "            console.log('Video end event from web');" +
        "        }," +
        "        requestFullscreen: function() {" +
        "            console.log('Fullscreen requested from web');" +
        "            if (typeof Android !== 'undefined' && Android.requestFullscreen) {" +
        "                Android.requestFullscreen();" +
        "            }" +
        "        }," +
        "        onFullscreenChange: function(isFullscreen) {" +
        "            console.log('Fullscreen change:', isFullscreen);" +
        "        }," +
        "        onPlayerReady: function(playerType) {" +
        "            console.log('Player ready:', playerType);" +
        "        }" +
        "    };" +
        "    " +
        "    // 延迟执行增强" +
        "    setTimeout(function() {" +
        "        enhanceVideoPlayers();" +
        "        enhanceXVideos();" +
        "        enhancePornhub();" +
        "        enhanceYouTube();" +
        "        enhanceWebPlayers();" +
        "    }, 1000);" +
        "}" +
        "" +
        "// 页面加载完成后初始化" +
        "if (document.readyState === 'loading') {" +
        "    document.addEventListener('DOMContentLoaded', initVideoEnhancer);" +
        "} else {" +
        "    initVideoEnhancer();" +
        "}" +
        "" +
        "// 暴露检测函数" +
        "window.detectVideoElements = detectVideoElements;";

    public VideoPlayerEnhancer(Activity activity) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * 为WebView应用视频增强功能
     */
    public void enhanceWebView(WebView webView) {
        this.webView = webView;

        // 注入JavaScript
        injectVideoEnhancerScript();

        // 设置WebChromeClient来处理全屏
        setupWebChromeClient();
    }

    /**
     * 注入视频增强脚本
     */
    private void injectVideoEnhancerScript() {
        if (webView != null) {
            webView.evaluateJavascript(VIDEO_ENHANCER_SCRIPT, null);
            Log.d(TAG, "Video enhancer script injected");
        }
    }

    /**
     * 设置WebChromeClient处理全屏
     */
    private void setupWebChromeClient() {
        if (webView != null) {
            webView.setWebChromeClient(new android.webkit.WebChromeClient() {

                @Override
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    Log.d(TAG, "onShowCustomView called");
                    enterFullscreen(view, callback);
                }

                @Override
                public void onHideCustomView() {
                    Log.d(TAG, "onHideCustomView called");
                    exitFullscreen();
                }
            });
        }
    }

    /**
     * 进入全屏模式
     */
    private void enterFullscreen(View view, android.webkit.WebChromeClient.CustomViewCallback callback) {
        if (isFullscreen) return;

        try {
            isFullscreen = true;
            customView = view;

            // 保存原始方向
            originalOrientation = activity.getRequestedOrientation();

            // 强制横屏
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isLandscapeForced = true;

            // 隐藏系统UI
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // 创建全屏容器
            if (fullscreenContainer == null) {
                fullscreenContainer = new FrameLayout(activity);
                fullscreenContainer.setBackgroundColor(0xFF000000);
            }

            // 添加视频视图
            fullscreenContainer.addView(view);
            activity.setContentView(fullscreenContainer);

            Log.d(TAG, "Entered fullscreen mode");

        } catch (Exception e) {
            Log.e(TAG, "Error entering fullscreen", e);
        }
    }

    /**
     * 退出全屏模式
     */
    private void exitFullscreen() {
        if (!isFullscreen) return;

        try {
            isFullscreen = false;

            // 恢复原始方向
            if (isLandscapeForced) {
                activity.setRequestedOrientation(originalOrientation);
                isLandscapeForced = false;
            }

            // 显示系统UI
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // 移除全屏视图
            if (fullscreenContainer != null && customView != null) {
                fullscreenContainer.removeView(customView);
            }

            // 恢复原始布局
            activity.setContentView(R.layout.activity_web_view);

            // 清理引用
            customView = null;

            Log.d(TAG, "Exited fullscreen mode");

        } catch (Exception e) {
            Log.e(TAG, "Error exiting fullscreen", e);
        }
    }

    /**
     * 请求全屏（由JavaScript调用）
     */
    public void requestFullscreen() {
        activity.runOnUiThread(() -> {
            Log.d(TAG, "Fullscreen requested from JavaScript");
            // 这里可以触发WebView的全屏模式
            // 或者直接调用系统的全屏处理
        });
    }

    /**
     * 切换屏幕方向
     */
    public void toggleOrientation() {
        try {
            int currentOrientation = activity.getRequestedOrientation();

            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                currentOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                // 切换到横屏
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                Log.d(TAG, "Switched to landscape");
            } else {
                // 切换到竖屏
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                Log.d(TAG, "Switched to portrait");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling orientation", e);
        }
    }

    /**
     * 增加音量
     */
    public void increaseVolume() {
        try {
            if (audioManager != null) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                Log.d(TAG, "Volume increased");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error increasing volume", e);
        }
    }

    /**
     * 减少音量
     */
    public void decreaseVolume() {
        try {
            if (audioManager != null) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                Log.d(TAG, "Volume decreased");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decreasing volume", e);
        }
    }

    /**
     * 静音切换
     */
    public void toggleMute() {
        try {
            if (audioManager != null) {
                int flags = AudioManager.FLAG_SHOW_UI;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_UNMUTE, flags);
                        Log.d(TAG, "Unmuted");
                    } else {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_MUTE, flags);
                        Log.d(TAG, "Muted");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling mute", e);
        }
    }

    /**
     * 获取当前音量
     */
    public int getCurrentVolume() {
        try {
            if (audioManager != null) {
                return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current volume", e);
        }
        return 0;
    }

    /**
     * 设置音量
     */
    public void setVolume(int volume) {
        try {
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                Log.d(TAG, "Volume set to: " + volume);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting volume", e);
        }
    }

    /**
     * 获取最大音量
     */
    public int getMaxVolume() {
        try {
            if (audioManager != null) {
                return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting max volume", e);
        }
        return 15; // 默认最大值
    }

    /**
     * 检查是否处于全屏模式
     */
    public boolean isFullscreen() {
        return isFullscreen;
    }

    /**
     * 释放资源
     */
    public void release() {
        try {
            if (isFullscreen) {
                exitFullscreen();
            }

            webView = null;
            fullscreenContainer = null;
            customView = null;
            audioManager = null;

            Log.d(TAG, "VideoPlayerEnhancer released");
        } catch (Exception e) {
            Log.e(TAG, "Error releasing VideoPlayerEnhancer", e);
        }
    }
}
