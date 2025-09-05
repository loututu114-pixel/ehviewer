package com.hippo.ehviewer.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * 内嵌视频播放器 - 解决xvideos全屏播放问题
 * 提供内置的视频播放控制和全屏功能
 */
public class EmbeddedVideoPlayer extends FrameLayout {
    private static final String TAG = "EmbeddedVideoPlayer";
    
    private WebView webView;
    private Activity activity;
    private ViewGroup originalParent;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private LinearLayout controlsOverlay;
    private ImageButton fullscreenButton;
    private ImageButton playPauseButton;
    
    private boolean isFullscreen = false;
    private boolean controlsVisible = true;
    private Handler hideControlsHandler;
    private Runnable hideControlsRunnable;
    
    // 保存原始状态
    private int originalOrientation;
    private int originalSystemUiVisibility;

    public EmbeddedVideoPlayer(Context context) {
        super(context);
        init();
    }

    public EmbeddedVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        hideControlsHandler = new Handler(Looper.getMainLooper());
        hideControlsRunnable = this::hideControls;
        
        setBackgroundColor(Color.BLACK);
        setupTouchHandling();
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
     * 设置WebView的视频处理
     */
    private void setupWebViewVideoHandling() {
        if (webView == null) return;
        
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                Log.d(TAG, "Custom view shown - entering fullscreen");
                showCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                Log.d(TAG, "Custom view hidden - exiting fullscreen");
                hideCustomView();
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    // 页面加载完成，重新注入脚本
                    injectVideoEnhancementScript();
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后注入视频增强脚本
                postDelayed(() -> injectVideoEnhancementScript(), 1000);
            }
        });
    }

    /**
     * 创建视频控制覆盖层
     */
    private void createControlsOverlay() {
        controlsOverlay = new LinearLayout(getContext());
        controlsOverlay.setOrientation(LinearLayout.HORIZONTAL);
        controlsOverlay.setGravity(Gravity.CENTER);
        controlsOverlay.setBackgroundColor(Color.argb(150, 0, 0, 0));
        
        // 播放/暂停按钮
        playPauseButton = new ImageButton(getContext());
        playPauseButton.setBackgroundColor(Color.TRANSPARENT);
        playPauseButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        
        // 全屏按钮
        fullscreenButton = new ImageButton(getContext());
        fullscreenButton.setBackgroundColor(Color.TRANSPARENT);
        fullscreenButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        
        // 按钮样式和布局
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            dpToPx(48), dpToPx(48));
        buttonParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        
        controlsOverlay.addView(playPauseButton, buttonParams);
        controlsOverlay.addView(fullscreenButton, buttonParams);
        
        // 控制栏位置
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        overlayParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        overlayParams.bottomMargin = dpToPx(16);
        
        addView(controlsOverlay, overlayParams);
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
     * 清理资源
     */
    public void cleanup() {
        if (isFullscreen) {
            hideCustomView();
        }
        
        if (hideControlsHandler != null) {
            hideControlsHandler.removeCallbacks(hideControlsRunnable);
        }
        
        webView = null;
        activity = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cleanup();
    }
}