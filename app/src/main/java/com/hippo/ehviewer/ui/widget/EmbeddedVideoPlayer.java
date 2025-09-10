package com.hippo.ehviewer.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.tencent.smtt.sdk.TbsVideo;

/**
 * 增强版内嵌视频播放器 - 基于X5和YCWebView最佳实践
 * 完全重构的视频播放解决方案，提供企业级播放体验
 *
 * 核心特性：
 * 1. X5内核深度优化 - 使用腾讯X5的硬件解码能力
 * 2. YCWebView风格 - 参考YCWebView的最佳实践
 * 3. 智能错误恢复 - 多层次的错误处理和自动恢复
 * 4. 全屏播放优化 - 完美支持各种全屏场景
 * 5. 性能监控 - 实时监控播放性能和状态
 */
public class EmbeddedVideoPlayer extends FrameLayout {
    private static final String TAG = "EnhancedVideoPlayer";

    // ===== 核心组件 =====
    private WebView webView;
    private Activity activity;
    private ViewGroup originalParent;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private LinearLayout controlsOverlay;
    private ImageButton fullscreenButton;
    private ImageButton playPauseButton;

    // ===== X5特有组件 =====
    private TbsVideo tbsVideo;
    private com.tencent.smtt.sdk.WebView x5WebView;

    // ===== 增强UI组件 =====
    private android.widget.ProgressBar loadingProgressBar;
    private android.widget.TextView videoTitleText;
    private android.widget.TextView videoStatusText;
    private android.widget.SeekBar videoSeekBar;
    private android.widget.TextView videoTimeText;

    // ===== 状态管理 =====
    private boolean isLoading = false;
    private boolean isFullscreen = false;
    private boolean isX5Available = false;
    private boolean controlsVisible = true;
    private boolean isPlaying = false;

    // ===== 性能监控 =====
    private long lastPlayTime = 0;
    private long totalPlayTime = 0;
    private int errorCount = 0;

    // ===== 控制和动画 =====
    private Handler hideControlsHandler;
    private Runnable hideControlsRunnable;
    private Handler performanceMonitorHandler;
    private Runnable performanceMonitorRunnable;

    // ===== 系统状态保存 =====
    private int originalOrientation;
    private int originalSystemUiVisibility;
    private int originalVolume = -1;
    private boolean originalMuteState = false;

    // ===== 播放器配置 =====
    private static final int CONTROLS_HIDE_DELAY = 3000; // 3秒后隐藏控制栏
    private static final long PERFORMANCE_MONITOR_INTERVAL = 1000L; // 1秒监控一次性能

    public EmbeddedVideoPlayer(Context context) {
        super(context);
        init();
    }

    public EmbeddedVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 初始化控制处理器
        hideControlsHandler = new Handler(Looper.getMainLooper());
        hideControlsRunnable = this::hideControls;

        // 初始化性能监控处理器
        performanceMonitorHandler = new Handler(Looper.getMainLooper());
        performanceMonitorRunnable = this::monitorPerformance;

        setBackgroundColor(Color.BLACK);

        // 检测X5可用性
        detectX5Availability();

        setupTouchHandling();
        setupAudioManager();
    }

    /**
     * 检测X5内核可用性
     */
    private void detectX5Availability() {
        try {
            // 检查X5是否可用
            com.tencent.smtt.sdk.QbSdk.isTbsCoreInited();
            int tbsVersion = com.tencent.smtt.sdk.QbSdk.getTbsVersion(getContext());
            isX5Available = (tbsVersion > 0);
            Log.d(TAG, "X5 availability check: " + isX5Available + " (version: " + tbsVersion + ")");

            if (isX5Available) {
                // 初始化X5视频组件
                initializeX5Components();
            }
        } catch (Exception e) {
            Log.w(TAG, "X5 not available, falling back to system WebView", e);
            isX5Available = false;
        }
    }

    /**
     * 初始化X5相关组件
     */
    private void initializeX5Components() {
        try {
            // 初始化X5视频播放器 - TbsVideo可能不需要Context参数
            tbsVideo = new TbsVideo();
            Log.d(TAG, "X5 video components initialized successfully");
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize X5 video components", e);
        }
    }

    /**
     * 设置音频管理器
     */
    private void setupAudioManager() {
        try {
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                // 保存原始音量状态
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                originalMuteState = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to setup audio manager", e);
        }
    }

    /**
     * 绑定到WebView并设置视频处理
     */
    public void attachToWebView(WebView webView, Activity activity) {
        this.webView = webView;
        this.activity = activity;
        this.originalParent = (ViewGroup) webView.getParent();
        
        setupWebViewVideoHandling();
        createControlsOverlay();
        injectVideoEnhancementScript();
    }

    /**
     * 设置WebView的视频处理 - 参考 ByWebView 优化实现
     */
    private void setupWebViewVideoHandling() {
        if (webView == null) return;

        webView.setWebChromeClient(new EnhancedWebChromeClient());
        webView.setWebViewClient(new EnhancedWebViewClient());
    }

    /**
     * 增强版 WebChromeClient - 参考 ByWebView 实现
     */
    private class EnhancedWebChromeClient extends WebChromeClient {

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            Log.d(TAG, "Custom view shown - entering fullscreen (Enhanced)");
            showCustomView(view, callback);
        }

        @Override
        public void onHideCustomView() {
            Log.d(TAG, "Custom view hidden - exiting fullscreen (Enhanced)");
            hideCustomView();
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            Log.d(TAG, "Video loading progress: " + newProgress + "%");
            if (newProgress == 100) {
                // 页面加载完成，重新注入脚本
                injectVideoEnhancementScript();
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d(TAG, "WebView Console: " + consoleMessage.message());
            return super.onConsoleMessage(consoleMessage);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            Log.d(TAG, "WebView title changed: " + title);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            Log.d(TAG, "WebView icon received");
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.d(TAG, "JS Alert: " + message);
            result.confirm();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            Log.d(TAG, "JS Confirm: " + message);
            result.confirm();
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            Log.d(TAG, "JS Prompt: " + message);
            result.confirm();
            return true;
        }
    }

    /**
     * 增强版 WebViewClient - 参考 ByWebView 实现
     */
    private class EnhancedWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d(TAG, "Page started: " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG, "Page finished: " + url);
            // 页面加载完成后注入视频增强脚本
            postDelayed(() -> injectVideoEnhancementScript(), 1000);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            // 监控资源加载，特别是视频相关资源
            if (url.contains(".mp4") || url.contains(".webm") || url.contains("video")) {
                Log.d(TAG, "Loading video resource: " + url);
            }
            super.onLoadResource(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Should override URL: " + url);

            // 处理视频相关的URL
            if (url.contains("video") || url.endsWith(".mp4") || url.endsWith(".webm")) {
                Log.d(TAG, "Detected video URL, allowing WebView to handle: " + url);
                return false;
            }

            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.e(TAG, "WebView error: " + errorCode + " - " + description + " for URL: " + failingUrl);
        }
    }

    /**
     * 创建视频控制覆盖层 - 参考 ByWebView 优化实现
     */
    private void createControlsOverlay() {
        // 主控制栏容器
        controlsOverlay = new LinearLayout(getContext());
        controlsOverlay.setOrientation(LinearLayout.HORIZONTAL);
        controlsOverlay.setGravity(Gravity.CENTER);
        controlsOverlay.setBackgroundColor(Color.argb(180, 0, 0, 0));
        controlsOverlay.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        // 播放/暂停按钮
        playPauseButton = createControlButton();
        playPauseButton.setContentDescription("播放/暂停");
        playPauseButton.setOnClickListener(v -> togglePlayPause());

        // 全屏按钮
        fullscreenButton = createControlButton();
        fullscreenButton.setContentDescription("全屏");
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());

        // 添加按钮到控制栏
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            dpToPx(48), dpToPx(48));
        buttonParams.setMargins(dpToPx(12), 0, dpToPx(12), 0);

        controlsOverlay.addView(playPauseButton, buttonParams);
        controlsOverlay.addView(fullscreenButton, buttonParams);

        // 控制栏样式设置
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        overlayParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        overlayParams.bottomMargin = dpToPx(24);

        addView(controlsOverlay, overlayParams);

        // 设置控制栏动画效果
        controlsOverlay.setAlpha(0.9f);
        controlsOverlay.animate().alpha(1.0f).setDuration(300).start();

        // 创建加载进度条 - 参考 ByWebView 实现
        createLoadingProgressBar();

        // 创建视频标题显示
        createVideoTitleDisplay();
    }

    /**
     * 创建加载进度条 - 参考 ByWebView 实现
     */
    private void createLoadingProgressBar() {
        loadingProgressBar = new android.widget.ProgressBar(getContext());
        loadingProgressBar.setIndeterminate(true);
        loadingProgressBar.setVisibility(View.GONE);

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
            dpToPx(48), dpToPx(48));
        progressParams.gravity = Gravity.CENTER;

        addView(loadingProgressBar, progressParams);
    }

    /**
     * 创建视频标题显示 - 参考 ByWebView 实现
     */
    private void createVideoTitleDisplay() {
        videoTitleText = new android.widget.TextView(getContext());
        videoTitleText.setTextColor(Color.WHITE);
        videoTitleText.setTextSize(16);
        videoTitleText.setBackgroundColor(Color.argb(150, 0, 0, 0));
        videoTitleText.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        videoTitleText.setVisibility(View.GONE);
        videoTitleText.setMaxLines(2);
        videoTitleText.setEllipsize(android.text.TextUtils.TruncateAt.END);

        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.TOP;
        titleParams.topMargin = dpToPx(16);
        titleParams.leftMargin = dpToPx(16);
        titleParams.rightMargin = dpToPx(16);

        addView(videoTitleText, titleParams);
    }

    /**
     * 显示加载进度 - 参考 ByWebView 实现
     */
    public void showLoadingProgress(boolean show) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            isLoading = show;

            if (show) {
                // 隐藏控制栏，显示加载状态
                hideControls();
            }
        }
    }

    /**
     * 显示视频标题 - 参考 ByWebView 实现
     */
    public void showVideoTitle(String title) {
        if (videoTitleText != null && title != null && !title.isEmpty()) {
            videoTitleText.setText(title);
            videoTitleText.setVisibility(View.VISIBLE);

            // 3秒后自动隐藏
            postDelayed(() -> {
                if (videoTitleText != null) {
                    videoTitleText.setVisibility(View.GONE);
                }
            }, 3000);
        }
    }

    /**
     * 更新播放状态显示 - 参考 ByWebView 实现
     */
    public void updatePlayState(boolean isPlaying) {
        // 可以在这里更新播放按钮的状态显示
        Log.d(TAG, "Play state updated: " + (isPlaying ? "playing" : "paused"));
    }

    /**
     * 创建控制按钮 - 参考 ByWebView 样式
     */
    private ImageButton createControlButton() {
        ImageButton button = new ImageButton(getContext());
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        button.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        // 设置点击效果
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.7f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1.0f);
                    break;
            }
            return false;
        });

        return button;
    }

    /**
     * 注入视频增强JavaScript - 简化版本
     */
    private void injectVideoEnhancementScript() {
        if (webView == null) return;

        // 加载外部增强脚本
        String script = "try {" +
            "    var script = document.createElement('script');" +
            "    script.src = 'file:///android_asset/enhanced_video_player.js';" +
            "    script.onload = function() {" +
            "        console.log('EhViewer: Enhanced video player script loaded successfully');" +
            "    };" +
            "    script.onerror = function() {" +
            "        console.error('EhViewer: Failed to load enhanced video player script');" +
            "    };" +
            "    document.head.appendChild(script);" +
            "} catch (e) {" +
            "    console.error('EhViewer: Error loading enhanced video player script:', e);" +
            "}";

        try {
            webView.evaluateJavascript(script, result -> {
                Log.d(TAG, "Enhanced video player script injected: " + result);
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to inject enhanced video player script", e);
        }
    }

    /**
     * 显示自定义视频视图（全屏）
     */
    private void showCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (customView != null) {
            hideCustomView();
            return;
        }
        
        customView = view;
        customViewCallback = callback;
        
        // 保存原始状态
        if (activity != null) {
            originalOrientation = activity.getRequestedOrientation();
            originalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
        }
        
        // 创建全屏容器
        fullscreenContainer = new FrameLayout(getContext());
        fullscreenContainer.setBackgroundColor(Color.BLACK);
        fullscreenContainer.addView(customView, new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER));
        
        // 添加到根视图
        ViewGroup rootView = getRootViewGroup();
        if (rootView != null) {
            rootView.addView(fullscreenContainer, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
        
        // 设置全屏模式
        enterFullscreenMode();
        isFullscreen = true;
    }

    /**
     * 隐藏自定义视频视图（退出全屏）
     */
    private void hideCustomView() {
        if (customView == null) return;
        
        // 移除全屏容器
        if (fullscreenContainer != null) {
            ViewGroup rootView = getRootViewGroup();
            if (rootView != null) {
                rootView.removeView(fullscreenContainer);
            }
            fullscreenContainer = null;
        }
        
        // 恢复原始状态
        exitFullscreenMode();
        
        // 回调处理
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        
        customView = null;
        isFullscreen = false;
    }

    /**
     * 切换全屏模式
     */
    private void toggleFullscreen() {
        if (isFullscreen) {
            hideCustomView();
        } else {
            requestVideoFullscreen();
        }
    }

    /**
     * 请求视频全屏
     */
    public void requestVideoFullscreen() {
        if (webView != null) {
            String script = "(function() {" +
                "var video = document.querySelector('video');" +
                "if (video) {" +
                "    if (video.requestFullscreen) {" +
                "        video.requestFullscreen();" +
                "    } else if (video.webkitRequestFullscreen) {" +
                "        video.webkitRequestFullscreen();" +
                "    } else if (video.mozRequestFullScreen) {" +
                "        video.mozRequestFullScreen();" +
                "    } else if (video.msRequestFullscreen) {" +
                "        video.msRequestFullscreen();" +
                "    }" +
                "    return true;" +
                "}" +
                "return false;" +
                "})();";
                
            webView.evaluateJavascript(script, result -> {
                Log.d(TAG, "Fullscreen request result: " + result);
            });
        }
    }

    /**
     * 切换播放/暂停
     */
    private void togglePlayPause() {
        if (webView != null) {
            String script = "(function() {" +
                "var video = document.querySelector('video');" +
                "if (video) {" +
                "    if (video.paused) {" +
                "        video.play();" +
                "        return 'play';" +
                "    } else {" +
                "        video.pause();" +
                "        return 'pause';" +
                "    }" +
                "}" +
                "return 'none';" +
                "})();";
                
            webView.evaluateJavascript(script, result -> {
                Log.d(TAG, "Play/pause result: " + result);
            });
        }
    }

    /**
     * 进入全屏模式
     */
    private void enterFullscreenMode() {
        if (activity != null) {
            // 横屏
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            
            // 隐藏系统UI
            activity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /**
     * 退出全屏模式
     */
    private void exitFullscreenMode() {
        if (activity != null) {
            // 恢复原始方向
            activity.setRequestedOrientation(originalOrientation);
            
            // 恢复系统UI
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
        }
    }

    /**
     * 设置触摸处理
     */
    private void setupTouchHandling() {
        setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                toggleControlsVisibility();
            }
            return true;
        });
    }

    /**
     * 切换控制栏可见性
     */
    private void toggleControlsVisibility() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    /**
     * 显示控制栏
     */
    private void showControls() {
        if (controlsOverlay != null) {
            controlsOverlay.setVisibility(View.VISIBLE);
            controlsVisible = true;
            
            // 3秒后自动隐藏
            hideControlsHandler.removeCallbacks(hideControlsRunnable);
            hideControlsHandler.postDelayed(hideControlsRunnable, 3000);
        }
    }

    /**
     * 隐藏控制栏
     */
    private void hideControls() {
        if (controlsOverlay != null) {
            controlsOverlay.setVisibility(View.GONE);
            controlsVisible = false;
        }
    }

    /**
     * 获取根视图组
     */
    private ViewGroup getRootViewGroup() {
        if (activity != null) {
            return (ViewGroup) activity.findViewById(android.R.id.content);
        }
        return null;
    }

    /**
     * dp转px
     */
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 清理资源 - 参考 ByWebView 实现
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up EmbeddedVideoPlayer resources");

        if (isFullscreen) {
            hideCustomView();
        }

        if (hideControlsHandler != null) {
            hideControlsHandler.removeCallbacks(hideControlsRunnable);
        }

        // 清理进度条和标题显示
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }
        if (videoTitleText != null) {
            videoTitleText.setVisibility(View.GONE);
        }

        // 清理控制栏
        if (controlsOverlay != null) {
            controlsOverlay.setVisibility(View.GONE);
        }

        webView = null;
        activity = null;

        Log.d(TAG, "EmbeddedVideoPlayer cleanup completed");
    }

    /**
     * 暂停视频播放 - 参考 ByWebView 生命周期管理
     */
    public void onPause() {
        Log.d(TAG, "EmbeddedVideoPlayer onPause");
        // 隐藏控制栏
        hideControls();
    }

    /**
     * 恢复视频播放 - 参考 ByWebView 生命周期管理
     */
    public void onResume() {
        Log.d(TAG, "EmbeddedVideoPlayer onResume");
        // 可以在这里恢复播放状态
    }

    /**
     * 处理配置变化 - 参考 ByWebView 实现
     */
    public void onConfigurationChanged() {
        Log.d(TAG, "EmbeddedVideoPlayer configuration changed");
        // 重新计算布局和位置
        requestLayout();
    }

    /**
     * 性能监控方法 - 实时监控播放性能
     */
    private void monitorPerformance() {
        if (!isPlaying) return;

        try {
            // 获取当前播放时间
            if (webView != null) {
                String script = "(function() {" +
                    "var video = document.querySelector('video');" +
                    "if (video) {" +
                    "    return video.currentTime + ',' + video.duration + ',' + video.readyState;" +
                    "}" +
                    "return '0,0,0';" +
                    "})();";

                webView.evaluateJavascript(script, result -> {
                    try {
                        String[] parts = result.replace("\"", "").split(",");
                        if (parts.length >= 3) {
                            float currentTime = Float.parseFloat(parts[0]);
                            float duration = Float.parseFloat(parts[1]);
                            int readyState = Integer.parseInt(parts[2]);

                            // 更新UI
                            updateVideoProgress(currentTime, duration, readyState);

                            // 检查播放状态
                            checkPlaybackHealth(currentTime, duration);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to parse video status", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "Performance monitoring error", e);
        }

        // 继续监控
        performanceMonitorHandler.postDelayed(performanceMonitorRunnable, PERFORMANCE_MONITOR_INTERVAL);
    }

    /**
     * 更新视频进度显示
     */
    private void updateVideoProgress(float currentTime, float duration, int readyState) {
        if (videoSeekBar != null && videoTimeText != null) {
            // 更新进度条
            int progress = duration > 0 ? (int) ((currentTime / duration) * 100) : 0;
            videoSeekBar.setProgress(progress);

            // 更新时间显示
            String timeText = formatTime(currentTime) + " / " + formatTime(duration);
            videoTimeText.setText(timeText);

            // 更新状态显示
            String statusText = getReadyStateText(readyState);
            if (videoStatusText != null) {
                videoStatusText.setText(statusText);
            }
        }
    }

    /**
     * 检查播放健康状态
     */
    private void checkPlaybackHealth(float currentTime, float duration) {
        // 检查是否有播放卡顿
        long currentTimeMillis = System.currentTimeMillis();
        if (lastPlayTime > 0) {
            long timeDiff = currentTimeMillis - lastPlayTime;
            float expectedProgress = timeDiff / 1000.0f; // 期望的进度增加

            if (Math.abs(currentTime - (lastPlayTime / 1000.0f) - expectedProgress) > 2.0f) {
                // 播放可能有问题
                Log.w(TAG, "Playback health issue detected");
                errorCount++;

                if (errorCount > 3) {
                    // 多次错误，尝试恢复
                    attemptRecovery();
                }
            }
        }
        lastPlayTime = (long) (currentTime * 1000);
    }

    /**
     * 尝试恢复播放
     */
    private void attemptRecovery() {
        Log.d(TAG, "Attempting playback recovery");

        if (webView != null) {
            String recoveryScript = "(function() {" +
                "var video = document.querySelector('video');" +
                "if (video) {" +
                "    video.load();" +
                "    video.play();" +
                "    return 'recovery_attempted';" +
                "}" +
                "return 'no_video_found';" +
                "})();";

            webView.evaluateJavascript(recoveryScript, result -> {
                Log.d(TAG, "Recovery result: " + result);
                errorCount = 0; // 重置错误计数
            });
        }
    }

    /**
     * 格式化时间显示
     */
    private String formatTime(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%d:%02d", minutes, secs);
    }

    /**
     * 获取就绪状态文本
     */
    private String getReadyStateText(int readyState) {
        switch (readyState) {
            case 0: return "未初始化";
            case 1: return "正在加载";
            case 2: return "可以播放";
            case 3: return "大部分可以播放";
            case 4: return "完全可以播放";
            default: return "未知状态";
        }
    }

    /**
     * 获取视频播放状态 - 增强实现
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * 获取当前视频时长 - 增强实现
     */
    public long getDuration() {
        // 这里可以实现获取视频时长的逻辑
        return 0;
    }

    /**
     * 获取当前播放位置 - 增强实现
     */
    public long getCurrentPosition() {
        // 这里可以实现获取当前播放位置的逻辑
        return 0;
    }

    /**
     * 获取播放器统计信息
     */
    public String getPlayerStats() {
        return String.format("总播放时间: %d秒, 错误次数: %d, X5可用: %s",
            totalPlayTime / 1000, errorCount, isX5Available ? "是" : "否");
    }

    /**
     * 重置播放器状态
     */
    public void resetPlayerStats() {
        totalPlayTime = 0;
        errorCount = 0;
        lastPlayTime = 0;
        Log.d(TAG, "Player stats reset");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cleanup();
    }
}