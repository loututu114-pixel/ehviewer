package com.hippo.ehviewer.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.widget.EmbeddedVideoPlayer;
import com.hippo.ehviewer.ui.widget.VideoJavaScriptInterface;

/**
 * 视频播放增强器
 * 为EhViewer提供完整的视频播放体验
 */
public class VideoPlayerEnhancer {
    private static final String TAG = "VideoPlayerEnhancer";

    private final Context context;
    private final Activity activity;
    private WebView webView;
    private EmbeddedVideoPlayer embeddedVideoPlayer;
    private VideoJavaScriptInterface jsInterface;
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
        "// XVideos特殊处理 - 改进版" +
        "function enhanceXVideos() {" +
        "    if (window.location.hostname.includes('xvideos.com')) {" +
        "        console.log('Applying XVideos enhancements');" +
        "        " +
        "        // 延迟执行，确保页面完全加载" +
        "        setTimeout(function() {" +
        "            // 查找所有可能的播放器容器" +
        "            var playerSelectors = [" +
        "                '#player'," +
        "                '.video-player'," +
        "                '#video-player'," +
        "                '.player-container'," +
        "                '#main-video-player'," +
        "                '.video-container'," +
        "                '#videoWrapper'," +
        "                '.video-wrapper'" +
        "            ];" +
        "            " +
        "            var playerContainer = null;" +
        "            for (var i = 0; i < playerSelectors.length; i++) {" +
        "                playerContainer = document.querySelector(playerSelectors[i]);" +
        "                if (playerContainer) {" +
        "                    console.log('Found XVideos player container:', playerSelectors[i]);" +
        "                    break;" +
        "                }" +
        "            }" +
        "            " +
        "            if (!playerContainer) {" +
        "                // 如果没找到容器，查找视频元素本身" +
        "                var video = document.querySelector('video');" +
        "                if (video) {" +
        "                    playerContainer = video.parentElement;" +
        "                    console.log('Using video parent as container');" +
        "                }" +
        "            }" +
        "            " +
        "            if (playerContainer) {" +
        "                playerContainer.style.position = 'relative';" +
        "                " +
        "                // 检查是否已经有全屏按钮" +
        "                var existingBtn = playerContainer.querySelector('.ehviewer-fullscreen-btn');" +
        "                if (!existingBtn) {" +
        "                    // 添加EhViewer全屏按钮" +
        "                    var fullscreenBtn = document.createElement('button');" +
        "                    fullscreenBtn.className = 'ehviewer-fullscreen-btn';" +
        "                    fullscreenBtn.innerHTML = '⛶';" +
        "                    fullscreenBtn.style.cssText = " +
        "                        'position: absolute; top: 10px; right: 10px; z-index: 10000; " +
        "                         background: rgba(0,0,0,0.8); color: white; border: 2px solid rgba(255,255,255,0.3); " +
        "                         width: 45px; height: 45px; border-radius: 50%; " +
        "                         font-size: 20px; cursor: pointer; " +
        "                         transition: all 0.3s ease; " +
        "                         box-shadow: 0 2px 8px rgba(0,0,0,0.3);';" +
        "                    " +
        "                    fullscreenBtn.onmouseover = function() {" +
        "                        this.style.background = 'rgba(255,255,255,0.2);';" +
        "                        this.style.transform = 'scale(1.1);';" +
        "                    };" +
        "                    fullscreenBtn.onmouseout = function() {" +
        "                        this.style.background = 'rgba(0,0,0,0.8);';" +
        "                        this.style.transform = 'scale(1);';" +
        "                    };" +
        "                    " +
        "                    fullscreenBtn.onclick = function(e) {" +
        "                        e.preventDefault();" +
        "                        e.stopPropagation();" +
        "                        console.log('XVideos fullscreen button clicked');" +
        "                        window.VideoEnhancer.requestFullscreen();" +
        "                        return false;" +
        "                    };" +
        "                    " +
        "                    playerContainer.appendChild(fullscreenBtn);" +
        "                    console.log('Added XVideos fullscreen button');" +
        "                }" +
        "                " +
        "                // 增强视频元素" +
        "                var videos = playerContainer.querySelectorAll('video');" +
        "                for (var j = 0; j < videos.length; j++) {" +
        "                    var video = videos[j];" +
        "                    if (!video.hasAttribute('data-xvideos-enhanced')) {" +
        "                        video.setAttribute('data-xvideos-enhanced', 'true');" +
        "                        " +
        "                        // 启用全屏属性" +
        "                        video.setAttribute('playsinline', 'false');" +
        "                        video.setAttribute('webkit-playsinline', 'false');" +
        "                        video.setAttribute('controls', 'true');" +
        "                        video.style.width = '100%';" +
        "                        " +
        "                        // 监听播放事件" +
        "                        video.addEventListener('play', function() {" +
        "                            console.log('XVideos video started playing');" +
        "                            window.VideoEnhancer.onVideoPlay();" +
        "                        });" +
        "                        " +
        "                        video.addEventListener('pause', function() {" +
        "                            console.log('XVideos video paused');" +
        "                            window.VideoEnhancer.onVideoPause();" +
        "                        });" +
        "                        " +
        "                        // 双击全屏" +
        "                        video.addEventListener('dblclick', function(e) {" +
        "                            e.preventDefault();" +
        "                            console.log('XVideos video double-clicked, requesting fullscreen');" +
        "                            window.VideoEnhancer.requestFullscreen();" +
        "                        });" +
        "                        " +
        "                        // 监听全屏变化" +
        "                        document.addEventListener('fullscreenchange', function() {" +
        "                            if (document.fullscreenElement) {" +
        "                                console.log('XVideos video entered fullscreen');" +
        "                                window.VideoEnhancer.onFullscreenChange(true);" +
        "                            } else {" +
        "                                console.log('XVideos video exited fullscreen');" +
        "                                window.VideoEnhancer.onFullscreenChange(false);" +
        "                            }" +
        "                        });" +
        "                    }" +
        "                }" +
        "                " +
        "                console.log('XVideos enhancement completed');" +
        "            } else {" +
        "                console.log('XVideos player container not found');" +
        "            }" +
        "        }, 2000); // 等待页面完全加载" +
        "    }" +
        "}" +
        "" +
        "// Pornhub特殊处理" +
        "function enhancePornhub() {" +
        "    if (window.location.hostname.includes('pornhub.com')) {" +
        "        console.log('Applying Pornhub enhancements');" +
        "        " +
        "        // 延迟执行，确保页面完全加载" +
        "        setTimeout(function() {" +
        "            // 查找播放器容器" +
        "            var playerSelectors = [" +
        "                '#player'," +
        "                '.video-player'," +
        "                '#video-player'," +
        "                '.player-container'," +
        "                '#main-video-player'" +
        "            ];" +
        "            " +
        "            var playerContainer = null;" +
        "            for (var i = 0; i < playerSelectors.length; i++) {" +
        "                playerContainer = document.querySelector(playerSelectors[i]);" +
        "                if (playerContainer) {" +
        "                    console.log('Found Pornhub player container:', playerSelectors[i]);" +
        "                    break;" +
        "                }" +
        "            }" +
        "            " +
        "            if (!playerContainer) {" +
        "                // 如果没找到容器，查找视频元素本身" +
        "                var video = document.querySelector('video');" +
        "                if (video) {" +
        "                    playerContainer = video.parentElement;" +
        "                    console.log('Using video parent as container');" +
        "                }" +
        "            }" +
        "            " +
        "            if (playerContainer) {" +
        "                playerContainer.style.position = 'relative';" +
        "                " +
        "                // 检查是否已经有全屏按钮" +
        "                var existingBtn = playerContainer.querySelector('.ehviewer-fullscreen-btn');" +
        "                if (!existingBtn) {" +
        "                    // 添加EhViewer全屏按钮" +
        "                    var fullscreenBtn = document.createElement('button');" +
        "                    fullscreenBtn.className = 'ehviewer-fullscreen-btn';" +
        "                    fullscreenBtn.innerHTML = '⛶';" +
        "                    fullscreenBtn.style.cssText = " +
        "                        'position: absolute; top: 10px; right: 10px; z-index: 10000; " +
        "                         background: rgba(0,0,0,0.8); color: white; border: 2px solid rgba(255,255,255,0.3); " +
        "                         width: 45px; height: 45px; border-radius: 50%; " +
        "                         font-size: 20px; cursor: pointer; " +
        "                         transition: all 0.3s ease; " +
        "                         box-shadow: 0 2px 8px rgba(0,0,0,0.3);';" +
        "                    " +
        "                    fullscreenBtn.onmouseover = function() {" +
        "                        this.style.background = 'rgba(255,255,255,0.2);';" +
        "                        this.style.transform = 'scale(1.1);';" +
        "                    };" +
        "                    fullscreenBtn.onmouseout = function() {" +
        "                        this.style.background = 'rgba(0,0,0,0.8);';" +
        "                        this.style.transform = 'scale(1);';" +
        "                    };" +
        "                    " +
        "                    fullscreenBtn.onclick = function(e) {" +
        "                        e.preventDefault();" +
        "                        e.stopPropagation();" +
        "                        console.log('Pornhub fullscreen button clicked');" +
        "                        window.VideoEnhancer.requestFullscreen();" +
        "                        return false;" +
        "                    };" +
        "                    " +
        "                    playerContainer.appendChild(fullscreenBtn);" +
        "                    console.log('Added Pornhub fullscreen button');" +
        "                }" +
        "                " +
        "                // 增强视频元素" +
        "                var videos = playerContainer.querySelectorAll('video');" +
        "                for (var j = 0; j < videos.length; j++) {" +
        "                    var video = videos[j];" +
        "                    if (!video.hasAttribute('data-pornhub-enhanced')) {" +
        "                        video.setAttribute('data-pornhub-enhanced', 'true');" +
        "                        " +
        "                        // 启用全屏属性" +
        "                        video.setAttribute('playsinline', 'false');" +
        "                        video.setAttribute('webkit-playsinline', 'false');" +
        "                        video.setAttribute('controls', 'true');" +
        "                        " +
        "                        // 监听播放事件" +
        "                        video.addEventListener('play', function() {" +
        "                            console.log('Pornhub video started playing');" +
        "                            window.VideoEnhancer.onVideoPlay();" +
        "                        });" +
        "                        " +
        "                        video.addEventListener('pause', function() {" +
        "                            console.log('Pornhub video paused');" +
        "                            window.VideoEnhancer.onVideoPause();" +
        "                        });" +
        "                        " +
        "                        // 双击全屏" +
        "                        video.addEventListener('dblclick', function(e) {" +
        "                            e.preventDefault();" +
        "                            console.log('Pornhub video double-clicked, requesting fullscreen');" +
        "                            window.VideoEnhancer.requestFullscreen();" +
        "                        });" +
        "                    }" +
        "                }" +
        "                " +
        "                console.log('Pornhub enhancement completed');" +
        "            } else {" +
        "                console.log('Pornhub player container not found');" +
        "            }" +
        "        }, 2000); // 等待页面完全加载" +
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
        "        " +
        "        // 增强其他色情网站" +
        "        if (window.location.hostname.includes('xhamster.com')) {" +
        "            enhanceXHamster();" +
        "        }" +
        "        if (window.location.hostname.includes('xnxx.com')) {" +
        "            enhanceXNXX();" +
        "        }" +
        "        if (window.location.hostname.includes('redtube.com')) {" +
        "            enhanceRedTube();" +
        "        }" +
        "        if (window.location.hostname.includes('xvideos.es')) {" +
        "            enhanceXVideosES();" +
        "        }" +
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
        "window.detectVideoElements = detectVideoElements;" +
        "" +
        "// XHamster特殊处理" +
        "function enhanceXHamster() {" +
        "    console.log('Applying XHamster enhancements');" +
        "    setTimeout(function() {" +
        "        var playerSelectors = ['#player', '.video-player', '#video-player', '.player-container', '#main-video-player', '.video-container'];" +
        "        var playerContainer = null;" +
        "        for (var i = 0; i < playerSelectors.length; i++) {" +
        "            playerContainer = document.querySelector(playerSelectors[i]);" +
        "            if (playerContainer) break;" +
        "        }" +
        "        if (!playerContainer) {" +
        "            var video = document.querySelector('video');" +
        "            if (video) playerContainer = video.parentElement;" +
        "        }" +
        "        if (playerContainer) {" +
        "            playerContainer.style.position = 'relative';" +
        "            var existingBtn = playerContainer.querySelector('.ehviewer-fullscreen-btn');" +
        "            if (!existingBtn) {" +
        "                var fullscreenBtn = document.createElement('button');" +
        "                fullscreenBtn.className = 'ehviewer-fullscreen-btn';" +
        "                fullscreenBtn.innerHTML = '⛶';" +
        "                fullscreenBtn.style.cssText = 'position: absolute; top: 10px; right: 10px; z-index: 10000; background: rgba(0,0,0,0.8); color: white; border: 2px solid rgba(255,255,255,0.3); width: 45px; height: 45px; border-radius: 50%; font-size: 20px; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 2px 8px rgba(0,0,0,0.3);';" +
        "                fullscreenBtn.onclick = function(e) { e.preventDefault(); e.stopPropagation(); window.VideoEnhancer.requestFullscreen(); return false; };" +
        "                playerContainer.appendChild(fullscreenBtn);" +
        "            }" +
        "            var videos = playerContainer.querySelectorAll('video');" +
        "            for (var j = 0; j < videos.length; j++) {" +
        "                var video = videos[j];" +
        "                if (!video.hasAttribute('data-xhamster-enhanced')) {" +
        "                    video.setAttribute('data-xhamster-enhanced', 'true');" +
        "                    video.setAttribute('playsinline', 'false');" +
        "                    video.setAttribute('webkit-playsinline', 'false');" +
        "                    video.setAttribute('controls', 'true');" +
        "                    video.addEventListener('dblclick', function(e) { e.preventDefault(); window.VideoEnhancer.requestFullscreen(); });" +
        "                }" +
        "            }" +
        "            console.log('XHamster enhancement completed');" +
        "        }" +
        "    }, 2000);" +
        "}" +
        "" +
        "// XNXX特殊处理" +
        "function enhanceXNXX() {" +
        "    console.log('Applying XNXX enhancements');" +
        "    setTimeout(function() {" +
        "        var playerSelectors = ['#player', '.video-player', '#video-player', '.player-container', '#main-video-player', '.video-container'];" +
        "        var playerContainer = null;" +
        "        for (var i = 0; i < playerSelectors.length; i++) {" +
        "            playerContainer = document.querySelector(playerSelectors[i]);" +
        "            if (playerContainer) break;" +
        "        }" +
        "        if (!playerContainer) {" +
        "            var video = document.querySelector('video');" +
        "            if (video) playerContainer = video.parentElement;" +
        "        }" +
        "        if (playerContainer) {" +
        "            playerContainer.style.position = 'relative';" +
        "            var existingBtn = playerContainer.querySelector('.ehviewer-fullscreen-btn');" +
        "            if (!existingBtn) {" +
        "                var fullscreenBtn = document.createElement('button');" +
        "                fullscreenBtn.className = 'ehviewer-fullscreen-btn';" +
        "                fullscreenBtn.innerHTML = '⛶';" +
        "                fullscreenBtn.style.cssText = 'position: absolute; top: 10px; right: 10px; z-index: 10000; background: rgba(0,0,0,0.8); color: white; border: 2px solid rgba(255,255,255,0.3); width: 45px; height: 45px; border-radius: 50%; font-size: 20px; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 2px 8px rgba(0,0,0,0.3);';" +
        "                fullscreenBtn.onclick = function(e) { e.preventDefault(); e.stopPropagation(); window.VideoEnhancer.requestFullscreen(); return false; };" +
        "                playerContainer.appendChild(fullscreenBtn);" +
        "            }" +
        "            var videos = playerContainer.querySelectorAll('video');" +
        "            for (var j = 0; j < videos.length; j++) {" +
        "                var video = videos[j];" +
        "                if (!video.hasAttribute('data-xnxx-enhanced')) {" +
        "                    video.setAttribute('data-xnxx-enhanced', 'true');" +
        "                    video.setAttribute('playsinline', 'false');" +
        "                    video.setAttribute('webkit-playsinline', 'false');" +
        "                    video.setAttribute('controls', 'true');" +
        "                    video.addEventListener('dblclick', function(e) { e.preventDefault(); window.VideoEnhancer.requestFullscreen(); });" +
        "                }" +
        "            }" +
        "            console.log('XNXX enhancement completed');" +
        "        }" +
        "    }, 2000);" +
        "}" +
        "" +
        "// RedTube特殊处理" +
        "function enhanceRedTube() {" +
        "    console.log('Applying RedTube enhancements');" +
        "    setTimeout(function() {" +
        "        var playerSelectors = ['#player', '.video-player', '#video-player', '.player-container', '#main-video-player', '.video-container'];" +
        "        var playerContainer = null;" +
        "        for (var i = 0; i < playerSelectors.length; i++) {" +
        "            playerContainer = document.querySelector(playerSelectors[i]);" +
        "            if (playerContainer) break;" +
        "        }" +
        "        if (!playerContainer) {" +
        "            var video = document.querySelector('video');" +
        "            if (video) playerContainer = video.parentElement;" +
        "        }" +
        "        if (playerContainer) {" +
        "            playerContainer.style.position = 'relative';" +
        "            var existingBtn = playerContainer.querySelector('.ehviewer-fullscreen-btn');" +
        "            if (!existingBtn) {" +
        "                var fullscreenBtn = document.createElement('button');" +
        "                fullscreenBtn.className = 'ehviewer-fullscreen-btn';" +
        "                fullscreenBtn.innerHTML = '⛶';" +
        "                fullscreenBtn.style.cssText = 'position: absolute; top: 10px; right: 10px; z-index: 10000; background: rgba(0,0,0,0.8); color: white; border: 2px solid rgba(255,255,255,0.3); width: 45px; height: 45px; border-radius: 50%; font-size: 20px; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 2px 8px rgba(0,0,0,0.3);';" +
        "                fullscreenBtn.onclick = function(e) { e.preventDefault(); e.stopPropagation(); window.VideoEnhancer.requestFullscreen(); return false; };" +
        "                playerContainer.appendChild(fullscreenBtn);" +
        "            }" +
        "            var videos = playerContainer.querySelectorAll('video');" +
        "            for (var j = 0; j < videos.length; j++) {" +
        "                var video = videos[j];" +
        "                if (!video.hasAttribute('data-redtube-enhanced')) {" +
        "                    video.setAttribute('data-redtube-enhanced', 'true');" +
        "                    video.setAttribute('playsinline', 'false');" +
        "                    video.setAttribute('webkit-playsinline', 'false');" +
        "                    video.setAttribute('controls', 'true');" +
        "                    video.addEventListener('dblclick', function(e) { e.preventDefault(); window.VideoEnhancer.requestFullscreen(); });" +
        "                }" +
        "            }" +
        "            console.log('RedTube enhancement completed');" +
        "        }" +
        "    }, 2000);" +
        "}" +
        "" +
        "// XVideosES特殊处理" +
        "function enhanceXVideosES() {" +
        "    console.log('Applying XVideosES enhancements');" +
        "    setTimeout(function() {" +
        "        var playerSelectors = ['#player', '.video-player', '#video-player', '.player-container', '#main-video-player', '.video-container'];" +
        "        var playerContainer = null;" +
        "        for (var i = 0; i < playerSelectors.length; i++) {" +
        "            playerContainer = document.querySelector(playerSelectors[i]);" +
        "            if (playerContainer) break;" +
        "        }" +
        "        if (!playerContainer) {" +
        "            var video = document.querySelector('video');" +
        "            if (video) playerContainer = video.parentElement;" +
        "        }" +
        "        if (playerContainer) {" +
        "            playerContainer.style.position = 'relative';" +
        "            var existingBtn = playerContainer.querySelector('.ehviewer-fullscreen-btn');" +
        "            if (!existingBtn) {" +
        "                var fullscreenBtn = document.createElement('button');" +
        "                fullscreenBtn.className = 'ehviewer-fullscreen-btn';" +
        "                fullscreenBtn.innerHTML = '⛶';" +
        "                fullscreenBtn.style.cssText = 'position: absolute; top: 10px; right: 10px; z-index: 10000; background: rgba(0,0,0,0.8); color: white; border: 2px solid rgba(255,255,255,0.3); width: 45px; height: 45px; border-radius: 50%; font-size: 20px; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 2px 8px rgba(0,0,0,0.3);';" +
        "                fullscreenBtn.onclick = function(e) { e.preventDefault(); e.stopPropagation(); window.VideoEnhancer.requestFullscreen(); return false; };" +
        "                playerContainer.appendChild(fullscreenBtn);" +
        "            }" +
        "            var videos = playerContainer.querySelectorAll('video');" +
        "            for (var j = 0; j < videos.length; j++) {" +
        "                var video = videos[j];" +
        "                if (!video.hasAttribute('data-xvideoses-enhanced')) {" +
        "                    video.setAttribute('data-xvideoses-enhanced', 'true');" +
        "                    video.setAttribute('playsinline', 'false');" +
        "                    video.setAttribute('webkit-playsinline', 'false');" +
        "                    video.setAttribute('controls', 'true');" +
        "                    video.addEventListener('dblclick', function(e) { e.preventDefault(); window.VideoEnhancer.requestFullscreen(); });" +
        "                }" +
        "            }" +
        "            console.log('XVideosES enhancement completed');" +
        "        }" +
        "    }, 2000);" +
        "}";

    public VideoPlayerEnhancer(Activity activity) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * 为WebView应用视频增强功能 - 新版本使用EmbeddedVideoPlayer
     */
    public void enhanceWebView(WebView webView) {
        this.webView = webView;

        try {
            // 创建内嵌视频播放器
            setupEmbeddedVideoPlayer();
            
            // 添加JavaScript接口
            setupJavaScriptInterface();
            
            // 延迟注入脚本，确保页面加载完成
            webView.post(() -> {
                injectSimplifiedVideoScript();
            });
            
            Log.d(TAG, "Enhanced WebView with embedded video player");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enhance WebView", e);
            // 回退到原始方法
            fallbackEnhanceWebView();
        }
    }

    /**
     * 设置内嵌视频播放器
     */
    private void setupEmbeddedVideoPlayer() {
        if (embeddedVideoPlayer == null && activity != null && webView != null) {
            embeddedVideoPlayer = new EmbeddedVideoPlayer(context);
            embeddedVideoPlayer.attachToWebView(webView, activity);
            Log.d(TAG, "Embedded video player created and attached");
        }
    }

    /**
     * 设置JavaScript接口
     */
    private void setupJavaScriptInterface() {
        if (webView != null && embeddedVideoPlayer != null) {
            jsInterface = new VideoJavaScriptInterface(embeddedVideoPlayer);
            webView.addJavascriptInterface(jsInterface, "Android");
            Log.d(TAG, "JavaScript interface added");
        }
    }

    /**
     * 注入简化的视频脚本
     */
    private void injectSimplifiedVideoScript() {
        if (webView == null) return;
        
        String script = "(function() {" +
            "if (window.EhVideoEnhanced) return;" +
            "window.EhVideoEnhanced = true;" +
            
            // 等待DOM准备就绪，增加重试机制
            "function waitForVideos(callback, retries) {" +
            "    retries = retries || 0;" +
            "    if (retries > 10) return;" + // 最多重试10次
            "    " +
            "    var videos = document.querySelectorAll('video');" +
            "    if (videos.length > 0) {" +
            "        callback(videos);" +
            "    } else {" +
            "        setTimeout(function() { waitForVideos(callback, retries + 1); }, 1000);" +
            "    }" +
            "}" +
            
            // 增强视频元素
            "waitForVideos(function(videos) {" +
            "    console.log('EhViewer: Found ' + videos.length + ' video(s)');" +
            "    " +
            "    for (var i = 0; i < videos.length; i++) {" +
            "        var video = videos[i];" +
            "        " +
            "        try {" +
            "            // 基本设置" +
            "            video.setAttribute('controls', 'true');" +
            "            video.setAttribute('playsinline', 'false');" + // 允许全屏
            "            " +
            "            // 添加双击全屏" +
            "            video.addEventListener('dblclick', function(e) {" +
            "                e.preventDefault();" +
            "                console.log('EhViewer: Video double-clicked');" +
            "                try {" +
            "                    if (typeof Android !== 'undefined' && Android.requestFullscreen) {" +
            "                        Android.requestFullscreen();" +
            "                    } else {" +
            "                        // 原生全屏回退" +
            "                        if (this.requestFullscreen) this.requestFullscreen();" +
            "                        else if (this.webkitRequestFullscreen) this.webkitRequestFullscreen();" +
            "                        else if (this.mozRequestFullScreen) this.mozRequestFullScreen();" +
            "                        else if (this.msRequestFullscreen) this.msRequestFullscreen();" +
            "                    }" +
            "                } catch (err) {" +
            "                    console.error('EhViewer: Fullscreen error:', err);" +
            "                }" +
            "            });" +
            "            " +
            "            // 播放状态监听" +
            "            video.addEventListener('play', function() {" +
            "                console.log('EhViewer: Video playing');" +
            "                if (typeof Android !== 'undefined' && Android.onPlayStateChanged) {" +
            "                    Android.onPlayStateChanged(true);" +
            "                }" +
            "            });" +
            "            " +
            "            video.addEventListener('pause', function() {" +
            "                console.log('EhViewer: Video paused');" +
            "                if (typeof Android !== 'undefined' && Android.onPlayStateChanged) {" +
            "                    Android.onPlayStateChanged(false);" +
            "                }" +
            "            });" +
            "            " +
            "            // 视频加载监听" +
            "            video.addEventListener('loadedmetadata', function() {" +
            "                console.log('EhViewer: Video metadata loaded');" +
            "                if (typeof Android !== 'undefined' && Android.onVideoLoaded) {" +
            "                    var src = this.currentSrc || this.src || 'unknown';" +
            "                    Android.onVideoLoaded(src, document.title || 'Video');" +
            "                }" +
            "            });" +
            "            " +
            "            // 错误监听" +
            "            video.addEventListener('error', function() {" +
            "                console.error('EhViewer: Video error:', this.error);" +
            "                if (typeof Android !== 'undefined' && Android.onVideoError) {" +
            "                    var errorMsg = this.error ? this.error.message : 'Unknown video error';" +
            "                    Android.onVideoError(errorMsg);" +
            "                }" +
            "            });" +
            "            " +
            "            console.log('EhViewer: Enhanced video element', i + 1);" +
            "            " +
            "        } catch (e) {" +
            "            console.error('EhViewer: Error enhancing video:', e);" +
            "        }" +
            "    }" +
            "});" +
            
            "console.log('EhViewer: Video enhancement script loaded');" +
            "})();";
        
        try {
            webView.evaluateJavascript(script, result -> {
                Log.d(TAG, "Simplified video script injected, result: " + result);
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to inject simplified video script", e);
        }
    }

    /**
     * 回退到原始增强方法
     */
    private void fallbackEnhanceWebView() {
        try {
            // 原始的JavaScript注入
            injectVideoEnhancerScript();
            
            // 设置WebChromeClient来处理全屏
            setupWebChromeClient();
            
            Log.d(TAG, "Using fallback video enhancement");
            
        } catch (Exception e) {
            Log.e(TAG, "Fallback enhancement also failed", e);
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        try {
            if (embeddedVideoPlayer != null) {
                embeddedVideoPlayer.cleanup();
                embeddedVideoPlayer = null;
            }
            
            jsInterface = null;
            webView = null;
            
            Log.d(TAG, "VideoPlayerEnhancer cleaned up");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    /**
     * 注入视频增强脚本 - 原始版本
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
     * 进入全屏模式 - 改进版本，使用覆盖层而不是替换整个Activity布局
     */
    private void enterFullscreen(View view, android.webkit.WebChromeClient.CustomViewCallback callback) {
        if (isFullscreen) return;

        try {
            isFullscreen = true;
            customView = view;

            // 启动独立的视频播放Activity
            launchVideoPlayerActivity();

            Log.d(TAG, "Launched video player activity");

        } catch (Exception e) {
            Log.e(TAG, "Error entering fullscreen", e);
            // 回退到原来的方法
            enterFullscreenFallback(view, callback);
        }
    }
    
    /**
     * 启动视频播放Activity - 改进版，支持多种网站
     */
    private void launchVideoPlayerActivity() {
        try {
            // 通过JavaScript获取视频信息
            if (webView != null) {
                // 获取当前URL来判断网站类型
                String currentUrl = webView.getUrl();

                String script;
                if (currentUrl != null && currentUrl.contains("xvideos.com")) {
                    // XVideos特殊处理
                    script = "(function() {" +
                        "try {" +
                        "    // 查找视频元素" +
                        "    var video = document.querySelector('video');" +
                        "    if (!video) {" +
                        "        // 尝试查找其他可能的视频元素" +
                        "        var videos = document.querySelectorAll('video, [data-video-src], .video-js video');" +
                        "        video = videos.length > 0 ? videos[0] : null;" +
                        "    }" +
                        "    " +
                        "    if (video) {" +
                        "        var videoUrl = video.currentSrc || video.src;" +
                        "        " +
                        "        // 如果没有直接的src，尝试从data属性或其他地方获取" +
                        "        if (!videoUrl) {" +
                        "            videoUrl = video.getAttribute('data-video-src') || " +
                        "                       video.getAttribute('data-src') || " +
                        "                       video.getAttribute('data-url');" +
                        "        }" +
                        "        " +
                        "        // 尝试从source标签获取" +
                        "        if (!videoUrl) {" +
                        "            var sources = video.querySelectorAll('source');" +
                        "            for (var i = 0; i < sources.length; i++) {" +
                        "                var src = sources[i].getAttribute('src');" +
                        "                if (src && src.includes('.mp4')) {" +
                        "                    videoUrl = src;" +
                        "                    break;" +
                        "                }" +
                        "            }" +
                        "        }" +
                        "        " +
                        "        // XVideos特定：查找script标签中的视频信息" +
                        "        if (!videoUrl) {" +
                        "            var scripts = document.querySelectorAll('script');" +
                        "            for (var i = 0; i < scripts.length; i++) {" +
                        "                var scriptContent = scripts[i].textContent || scripts[i].innerText;" +
                        "                if (scriptContent && scriptContent.includes('html5player.setVideoUrl')) {" +
                        "                    var urlMatch = scriptContent.match(/html5player\\.setVideoUrl\\(['\"](.*?)['\"]/);" +
                        "                    if (urlMatch && urlMatch[1]) {" +
                        "                        videoUrl = urlMatch[1];" +
                        "                        break;" +
                        "                    }" +
                        "                }" +
                        "                if (scriptContent && scriptContent.includes('html5player.setVideoHLS')) {" +
                        "                    var hlsMatch = scriptContent.match(/html5player\\.setVideoHLS\\(['\"](.*?)['\"]/);" +
                        "                    if (hlsMatch && hlsMatch[1]) {" +
                        "                        videoUrl = hlsMatch[1];" +
                        "                        break;" +
                        "                    }" +
                        "                }" +
                        "            }" +
                        "        }" +
                        "        " +
                        "        return JSON.stringify({" +
                        "            src: videoUrl," +
                        "            title: document.title || 'XVideos'," +
                        "            currentTime: video.currentTime || 0," +
                        "            duration: video.duration || 0," +
                        "            poster: video.poster || ''," +
                        "            type: 'xvideos'" +
                        "        });" +
                        "    }" +
                        "    " +
                        "    return null;" +
                        "} catch (e) {" +
                        "    console.error('Error getting XVideos video info:', e);" +
                        "    return null;" +
                        "}" +
                        "})();";
                } else {
                    // 通用视频处理
                    script = "(function() {" +
                        "var videos = document.querySelectorAll('video');" +
                        "if (videos.length > 0) {" +
                        "    var video = videos[0];" +
                        "    return JSON.stringify({" +
                        "        src: video.currentSrc || video.src," +
                        "        title: document.title || 'Video'," +
                        "        currentTime: video.currentTime || 0," +
                        "        duration: video.duration || 0," +
                        "        poster: video.poster || ''," +
                        "        type: 'generic'" +
                        "    });" +
                        "}" +
                        "return null;" +
                        "})();";
                }

                android.util.Log.d(TAG, "Executing JavaScript for video URL extraction...");
                android.util.Log.d(TAG, "Current URL: " + currentUrl);
                android.util.Log.d(TAG, "JavaScript length: " + script.length());

                // 添加超时处理
                final android.os.Handler timeoutHandler = new android.os.Handler();
                final Runnable timeoutRunnable = () -> {
                    android.util.Log.e(TAG, "JavaScript execution timeout!");
                    showErrorAndCleanup("JavaScript执行超时");
                };

                // 设置5秒超时
                timeoutHandler.postDelayed(timeoutRunnable, 5000);

                webView.evaluateJavascript(script, result -> {
                    // 取消超时
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    android.util.Log.d(TAG, "=== JavaScript Execution Result ===");
                    android.util.Log.d(TAG, "Raw result: " + result);
                    android.util.Log.d(TAG, "Result type: " + (result != null ? result.getClass().getSimpleName() : "null"));
                    android.util.Log.d(TAG, "Result length: " + (result != null ? result.length() : 0));

                    if (result == null) {
                        android.util.Log.e(TAG, "JavaScript execution returned null");
                        showErrorAndCleanup("JavaScript执行失败，返回null");
                        return;
                    }

                    if ("null".equals(result.trim())) {
                        android.util.Log.e(TAG, "JavaScript execution returned 'null' string");
                        showErrorAndCleanup("JavaScript返回null结果");
                        return;
                    }

                    if (result.trim().isEmpty()) {
                        android.util.Log.e(TAG, "JavaScript execution returned empty result");
                        showErrorAndCleanup("JavaScript返回空结果");
                        return;
                    }

                    android.util.Log.d(TAG, "Video info result: " + result);
                    if (result != null && !"null".equals(result)) {
                        try {
                            // 解析结果并启动MediaPlayerActivity
                            // 简化JSON解析 - 移除首尾引号
                            String jsonResult = result;
                            String quote = String.valueOf((char)34); // ASCII 34 = \"
                            if (jsonResult.startsWith(quote)) {
                                jsonResult = jsonResult.substring(1);
                            }
                            if (jsonResult.endsWith(quote)) {
                                jsonResult = jsonResult.substring(0, jsonResult.length() - 1);
                            }

                            // 处理转义字符
                            jsonResult = jsonResult.replaceAll("\\\\\"", "\"");
                            jsonResult = jsonResult.replaceAll("\\\\\\\\", "\\\\");

                            android.util.Log.d(TAG, "Parsed JSON: " + jsonResult);

                            org.json.JSONObject videoInfo = new org.json.JSONObject(jsonResult);
                            String videoUrl = videoInfo.optString("src");
                            String title = videoInfo.optString("title", "Video");
                            String videoType = videoInfo.optString("type", "generic");

                            android.util.Log.d(TAG, "Video URL: " + videoUrl + ", Type: " + videoType);

                            if (!videoUrl.isEmpty() && !"null".equals(videoUrl)) {
                                // 启动MediaPlayerActivity
                                android.content.Intent intent = new android.content.Intent(activity,
                                    com.hippo.ehviewer.ui.MediaPlayerActivity.class);
                                intent.putExtra(com.hippo.ehviewer.ui.MediaPlayerActivity.EXTRA_MEDIA_URI,
                                    android.net.Uri.parse(videoUrl));
                                intent.putExtra(com.hippo.ehviewer.ui.MediaPlayerActivity.EXTRA_MEDIA_TYPE, "video/*");
                                intent.putExtra(com.hippo.ehviewer.ui.MediaPlayerActivity.EXTRA_TITLE, title);

                                // 添加额外信息用于调试
                                intent.putExtra("video_type", videoType);
                                intent.putExtra("original_url", currentUrl);

                                activity.startActivity(intent);

                                // 退出当前全屏状态
                                activity.runOnUiThread(() -> exitFullscreen());

                                android.util.Log.d(TAG, "Successfully launched MediaPlayerActivity for " + videoType);
                            } else {
                                android.util.Log.w(TAG, "No valid video URL found");
                                // 回退到WebView全屏
                                activity.runOnUiThread(() -> {
                                    android.widget.Toast.makeText(activity, "无法获取视频URL，使用网页全屏", android.widget.Toast.LENGTH_SHORT).show();
                                });
                            }
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error parsing video info", e);
                            // 回退到WebView全屏
                            activity.runOnUiThread(() -> {
                                android.widget.Toast.makeText(activity, "解析视频信息失败", android.widget.Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        android.util.Log.w(TAG, "No video info returned from JavaScript");
                        // 回退到WebView全屏
                        activity.runOnUiThread(() -> {
                            android.widget.Toast.makeText(activity, "未找到视频，使用网页全屏", android.widget.Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching video player activity", e);
            // 回退到WebView全屏
            activity.runOnUiThread(() -> {
                android.widget.Toast.makeText(activity, "启动视频播放器失败", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    /**
     * 回退的全屏方法 - 在覆盖层中显示视频，而不是替换整个Activity
     */
    private void enterFullscreenFallback(View view, android.webkit.WebChromeClient.CustomViewCallback callback) {
        try {
            // 获取根视图
            android.view.ViewGroup rootView = (android.view.ViewGroup) activity.findViewById(android.R.id.content);
            if (rootView == null) return;
            
            // 保存原始方向
            originalOrientation = activity.getRequestedOrientation();

            // 强制横屏 - 只针对视频播放
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isLandscapeForced = true;

            // 隐藏状态栏和导航栏
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            // 创建全屏覆盖容器
            if (fullscreenContainer == null) {
                fullscreenContainer = new FrameLayout(activity);
                fullscreenContainer.setBackgroundColor(0xFF000000);
                fullscreenContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
                    
                // 添加退出按钮
                android.widget.ImageButton exitButton = new android.widget.ImageButton(activity);
                exitButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                exitButton.setBackgroundColor(0x80000000);
                FrameLayout.LayoutParams exitParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
                exitParams.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;
                exitParams.setMargins(0, 50, 50, 0);
                exitButton.setLayoutParams(exitParams);
                exitButton.setOnClickListener(v -> exitFullscreen());
                fullscreenContainer.addView(exitButton);
            }

            // 添加视频视图到容器
            fullscreenContainer.addView(view, 0); // 添加到退出按钮下方
            
            // 添加到根视图
            rootView.addView(fullscreenContainer);

            Log.d(TAG, "Entered fullscreen mode with overlay");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in fallback fullscreen", e);
        }
    }

    /**
     * 退出全屏模式 - 改进版本，只移除覆盖层
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

            // 恢复系统UI
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE);

            // 从根视图中移除全屏容器
            if (fullscreenContainer != null) {
                android.view.ViewGroup rootView = (android.view.ViewGroup) activity.findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.removeView(fullscreenContainer);
                }
                
                // 清理全屏容器
                if (customView != null) {
                    fullscreenContainer.removeView(customView);
                }
                fullscreenContainer.removeAllViews();
            }

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
            
            // 直接启动视频播放Activity
            launchVideoPlayerActivity();
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

    /**
     * 清理全屏加载状态
     */
    private void cleanupFullscreenLoadingState() {
        if (webView != null) {
            String cleanupScript = "var loadingMsg = document.getElementById('ehviewer-loading-msg');" +
                "if (loadingMsg) { loadingMsg.remove(); }" +
                "var fullscreenBtn = document.querySelector('.ehviewer-fullscreen-btn');" +
                "if (fullscreenBtn) {" +
                "    fullscreenBtn.innerHTML = '⛶';" +
                "    fullscreenBtn.style.background = 'rgba(0,0,0,0.8);';" +
                "    fullscreenBtn.disabled = false;" +
                "}";
            webView.evaluateJavascript(cleanupScript, null);
        }
    }

    /**
     * 显示错误并清理状态
     */
    private void showErrorAndCleanup(String errorMessage) {
        android.util.Log.e(TAG, "Video extraction error: " + errorMessage);

        activity.runOnUiThread(() -> {
            // 清理加载状态
            cleanupFullscreenLoadingState();

            // 显示错误提示
            android.widget.Toast.makeText(activity,
                "视频提取失败: " + errorMessage + "\n使用网页全屏模式",
                android.widget.Toast.LENGTH_LONG).show();

            // 延迟后尝试网页全屏
            android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(() -> {
                try {
                    android.widget.Toast.makeText(activity, "尝试网页全屏...",
                        android.widget.Toast.LENGTH_SHORT).show();
                    enterFullscreenFallback(null, null);
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Fallback fullscreen failed", e);
                    android.widget.Toast.makeText(activity, "网页全屏也失败了",
                        android.widget.Toast.LENGTH_SHORT).show();
                }
            }, 1000);
        });
    }
}
