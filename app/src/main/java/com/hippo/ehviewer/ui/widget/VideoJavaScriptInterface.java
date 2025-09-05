package com.hippo.ehviewer.ui.widget;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * 视频播放器JavaScript接口
 * 处理网页视频与原生播放器的通信
 */
public class VideoJavaScriptInterface {
    private static final String TAG = "VideoJSInterface";
    
    private EmbeddedVideoPlayer videoPlayer;
    
    public VideoJavaScriptInterface(EmbeddedVideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
    }
    
    /**
     * JavaScript请求全屏
     */
    @JavascriptInterface
    public void requestFullscreen() {
        Log.d(TAG, "JavaScript requested fullscreen");
        if (videoPlayer != null) {
            videoPlayer.post(() -> {
                // 触发全屏请求
                videoPlayer.requestVideoFullscreen();
            });
        }
    }
    
    /**
     * 进入全屏回调
     */
    @JavascriptInterface
    public void onEnterFullscreen() {
        Log.d(TAG, "Entered fullscreen mode");
    }
    
    /**
     * 退出全屏回调
     */
    @JavascriptInterface
    public void onExitFullscreen() {
        Log.d(TAG, "Exited fullscreen mode");
    }
    
    /**
     * 视频播放状态改变
     */
    @JavascriptInterface
    public void onPlayStateChanged(boolean isPlaying) {
        Log.d(TAG, "Play state changed: " + isPlaying);
    }
    
    /**
     * 视频加载完成
     */
    @JavascriptInterface
    public void onVideoLoaded(String videoUrl, String title) {
        Log.d(TAG, "Video loaded: " + title + " - " + videoUrl);
    }
    
    /**
     * 视频播放错误
     */
    @JavascriptInterface
    public void onVideoError(String error) {
        Log.e(TAG, "Video error: " + error);
    }

    /**
     * 播放视频
     */
    @JavascriptInterface
    public void playVideo() {
        Log.d(TAG, "JavaScript requested play");
        // 这个方法可以用来处理原生播放控制
    }

    /**
     * 暂停视频
     */
    @JavascriptInterface
    public void pauseVideo() {
        Log.d(TAG, "JavaScript requested pause");
        // 这个方法可以用来处理原生播放控制
    }

    /**
     * 切换播放/暂停状态
     */
    @JavascriptInterface
    public void togglePlayPause() {
        Log.d(TAG, "JavaScript requested toggle play/pause");
        // 这个方法可以用来处理原生播放控制
    }
}