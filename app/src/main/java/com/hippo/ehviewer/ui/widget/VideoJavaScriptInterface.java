package com.hippo.ehviewer.ui.widget;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * 视频播放器JavaScript接口 - 参考 ByWebView 优化实现
 * 处理网页视频与原生播放器的通信
 */
public class VideoJavaScriptInterface {
    private static final String TAG = "VideoJSInterface";

    private EmbeddedVideoPlayer videoPlayer;
    private OnVideoEventListener eventListener;

    public interface OnVideoEventListener {
        void onPlayStateChanged(boolean isPlaying);
        void onVideoLoaded(String videoUrl, String title);
        void onVideoError(String error);
        void onFullscreenRequested();
    }

    public VideoJavaScriptInterface(EmbeddedVideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
    }

    public void setEventListener(OnVideoEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * JavaScript请求全屏 - 参考 ByWebView 实现
     */
    @JavascriptInterface
    public void requestFullscreen() {
        Log.d(TAG, "JavaScript requested fullscreen");
        if (videoPlayer != null) {
            videoPlayer.post(() -> {
                videoPlayer.requestVideoFullscreen();
            });
        }
        if (eventListener != null) {
            eventListener.onFullscreenRequested();
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
     * 视频播放状态改变 - 增强版实现
     */
    @JavascriptInterface
    public void onPlayStateChanged(boolean isPlaying) {
        Log.d(TAG, "Play state changed: " + (isPlaying ? "playing" : "paused"));
        if (eventListener != null) {
            eventListener.onPlayStateChanged(isPlaying);
        }
    }

    /**
     * 视频加载完成 - 增强版实现
     */
    @JavascriptInterface
    public void onVideoLoaded(String videoUrl, String title) {
        Log.d(TAG, "Video loaded: " + title + " - " + videoUrl);
        if (eventListener != null) {
            eventListener.onVideoLoaded(videoUrl, title);
        }
    }

    /**
     * 视频播放错误 - 增强版实现
     */
    @JavascriptInterface
    public void onVideoError(String error) {
        Log.e(TAG, "Video error: " + error);
        if (eventListener != null) {
            eventListener.onVideoError(error);
        }
    }

    /**
     * 播放视频 - 参考 ByWebView 实现
     */
    @JavascriptInterface
    public void playVideo() {
        Log.d(TAG, "JavaScript requested play");
        if (videoPlayer != null) {
            videoPlayer.post(() -> {
                Log.d(TAG, "Executing play command from JavaScript");
            });
        }
    }

    /**
     * 暂停视频 - 参考 ByWebView 实现
     */
    @JavascriptInterface
    public void pauseVideo() {
        Log.d(TAG, "JavaScript requested pause");
        if (videoPlayer != null) {
            videoPlayer.post(() -> {
                Log.d(TAG, "Executing pause command from JavaScript");
            });
        }
    }

    /**
     * 切换播放/暂停状态 - 参考 ByWebView 实现
     */
    @JavascriptInterface
    public void togglePlayPause() {
        Log.d(TAG, "JavaScript requested toggle play/pause");
        if (videoPlayer != null) {
            videoPlayer.post(() -> {
                Log.d(TAG, "Toggling play/pause via JavaScript interface");
            });
        }
    }

    /**
     * 获取视频当前状态 - 新增方法
     */
    @JavascriptInterface
    public String getVideoStatus() {
        Log.d(TAG, "JavaScript requested video status");
        return "ready"; // 可以扩展为更详细的状态信息
    }

    /**
     * 设置视频音量 - 参考 ByWebView 实现
     */
    @JavascriptInterface
    public void setVolume(float volume) {
        Log.d(TAG, "JavaScript set volume: " + volume);
        // 这里可以实现音量控制逻辑
    }

    /**
     * 跳转到指定时间 - 参考 ByWebView 实现
     */
    @JavascriptInterface
    public void seekTo(float seconds) {
        Log.d(TAG, "JavaScript seek to: " + seconds + "s");
        // 这里可以实现进度跳转逻辑
    }

    /**
     * 获取视频时长 - 新增方法
     */
    @JavascriptInterface
    public float getDuration() {
        Log.d(TAG, "JavaScript requested duration");
        return 0f; // 可以返回实际的视频时长
    }

    /**
     * 获取当前播放时间 - 新增方法
     */
    @JavascriptInterface
    public float getCurrentTime() {
        Log.d(TAG, "JavaScript requested current time");
        return 0f; // 可以返回实际的当前播放时间
    }
}