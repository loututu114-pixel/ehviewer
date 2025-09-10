package com.hippo.ehviewer.video;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.tencent.smtt.sdk.QbSdk;

/**
 * 增强版视频播放优化器
 * 基于X5内核和YCWebView最佳实践，提供企业级视频播放解决方案
 *
 * 核心特性：
 * 1. X5内核深度优化 - 利用腾讯X5的硬件解码能力
 * 2. YCWebView风格 - 参考YCWebView的最佳实践
 * 3. 智能内核选择 - 自动选择最佳的播放内核
 * 4. 全屏播放增强 - 完美支持各种全屏场景
 * 5. YouTube深度优化 - 专门优化YouTube播放体验
 * 6. 多层防护机制 - 智能错误恢复和性能监控
 */
public class VideoPlaybackOptimizer {
    private static final String TAG = "VideoPlaybackOptimizer";
    
    private final Context mContext;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private FrameLayout mFullscreenContainer;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;

    // X5相关
    private boolean mIsX5Available = false;
    private int mX5Version = 0;
    private X5VideoPlayerOptimizer mX5Optimizer;
    
    // 视频播放监听器
    public interface VideoPlaybackListener {
        void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);
        void onHideCustomView();
        FrameLayout getVideoContainer();
    }
    
    private VideoPlaybackListener mListener;
    
    public VideoPlaybackOptimizer(Context context) {
        mContext = context;

        // 检测X5可用性
        detectX5Availability();

        // 初始化X5优化器（如果可用）
        if (mIsX5Available) {
            mX5Optimizer = new X5VideoPlayerOptimizer(context);
            Log.d(TAG, "X5VideoPlayerOptimizer initialized");
        }

        Log.d(TAG, "Enhanced VideoPlaybackOptimizer initialized - X5: " + mIsX5Available + " (v" + mX5Version + ")");
    }

    /**
     * 检测X5内核可用性
     */
    private void detectX5Availability() {
        try {
            // 检查X5是否可用
            com.tencent.smtt.sdk.QbSdk.isTbsCoreInited();
            mX5Version = com.tencent.smtt.sdk.QbSdk.getTbsVersion(mContext);
            mIsX5Available = (mX5Version > 0);

            if (mIsX5Available) {
                Log.d(TAG, "X5 kernel available, version: " + mX5Version);
            } else {
                Log.d(TAG, "X5 kernel not available, falling back to system WebView");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to detect X5 availability", e);
            mIsX5Available = false;
            mX5Version = 0;
        }
    }
    
    public void setVideoPlaybackListener(VideoPlaybackListener listener) {
        mListener = listener;
    }
    
    /**
     * 智能优化WebView视频播放设置 - 自动选择最佳策略
     */
    public void optimizeVideoPlayback(WebView webView) {
        if (webView == null) return;

        Log.d(TAG, "Starting intelligent video playback optimization - X5: " + mIsX5Available);

        if (mIsX5Available && mX5Optimizer != null) {
            // 使用X5优化器进行深度优化
            Log.d(TAG, "Using X5 optimizer for enhanced video playback");
            mX5Optimizer.optimizeVideoPlaybackForX5(webView);
            injectX5VideoOptimizationScript(webView);
        } else {
            // 使用传统优化策略
            Log.d(TAG, "Using traditional optimizer for video playback");
            applyTraditionalVideoOptimization(webView);
        }

        // 应用通用的优化设置
        applyCommonVideoOptimizations(webView);
    }

    /**
     * 应用X5视频优化脚本
     */
    private void injectX5VideoOptimizationScript(WebView webView) {
        if (mX5Optimizer != null) {
            mX5Optimizer.injectX5VideoOptimizationScript(webView);
        }
    }

    /**
     * 应用传统视频优化（当X5不可用时）
     */
    private void applyTraditionalVideoOptimization(WebView webView) {
        try {
            WebSettings settings = webView.getSettings();

            // ===== 视频播放基础设置 =====
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);

            // ===== 硬件加速优化 =====
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            Log.d(TAG, "Hardware acceleration enabled for video playback");

            // ===== DOM存储和数据库支持 =====
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);

            // ===== User Agent优化 =====
            String userAgent = settings.getUserAgentString();
            if (userAgent != null && !userAgent.contains("Chrome")) {
                settings.setUserAgentString(userAgent + " Chrome/120.0.0.0");
                Log.d(TAG, "UserAgent updated for better video compatibility");
            }

            // ===== 插件支持 =====
            try {
                settings.setPluginState(android.webkit.WebSettings.PluginState.ON);
                Log.d(TAG, "Plugin support enabled for video playback");
            } catch (Exception e) {
                Log.w(TAG, "Plugin support not available on this Android version", e);
            }

            // ===== 混合内容支持 =====
            settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

            // ===== JavaScript支持 =====
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);

            // ===== 缓存设置 =====
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);

            Log.d(TAG, "Traditional video playback optimization applied successfully");

        } catch (Exception e) {
            Log.w(TAG, "Failed to apply traditional video optimization", e);
        }
    }

    /**
     * 应用通用视频优化设置
     */
    private void applyCommonVideoOptimizations(WebView webView) {
        try {
            WebSettings settings = webView.getSettings();

            // ===== 性能优化设置 =====
            settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH);

            // ===== 加载优化 =====
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);

            // ===== 安全设置 =====
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setAllowFileAccessFromFileURLs(false);

            Log.d(TAG, "Common video optimizations applied");

        } catch (Exception e) {
            Log.w(TAG, "Failed to apply common video optimizations", e);
        }
    }
    
    /**
     * 智能创建优化的WebChromeClient - 自动选择最佳策略
     */
    public WebChromeClient createVideoOptimizedChromeClient() {
        if (mIsX5Available && mX5Optimizer != null) {
            Log.d(TAG, "Creating X5 optimized ChromeClient");
            return createX5ChromeClient();
        } else {
            Log.d(TAG, "Creating traditional optimized ChromeClient");
            return createTraditionalChromeClient();
        }
    }

    /**
     * 创建X5优化的ChromeClient
     */
    private WebChromeClient createX5ChromeClient() {
        if (mX5Optimizer != null) {
            // 设置监听器
            mX5Optimizer.setVideoPlaybackListener(new X5VideoPlayerOptimizer.VideoPlaybackListener() {
                @Override
                public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                    if (mListener != null) {
                        mListener.onShowCustomView(view, callback);
                    }
                }

                @Override
                public void onHideCustomView() {
                    if (mListener != null) {
                        mListener.onHideCustomView();
                    }
                }

                @Override
                public FrameLayout getVideoContainer() {
                    if (mListener != null) {
                        return mListener.getVideoContainer();
                    }
                    return null;
                }

                @Override
                public void onVideoError(String error) {
                    Log.e(TAG, "X5 Video error: " + error);
                }

                @Override
                public void onVideoReady() {
                    Log.d(TAG, "X5 Video ready for playback");
                }

                @Override
                public void onVideoBuffering(boolean isBuffering) {
                    Log.d(TAG, "X5 Video buffering: " + isBuffering);
                }
            });

            return mX5Optimizer.createX5OptimizedChromeClient();
        }

        // fallback到传统ChromeClient
        return createTraditionalChromeClient();
    }

    /**
     * 创建传统的ChromeClient
     */
    private WebChromeClient createTraditionalChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                Log.d(TAG, "Entering fullscreen video mode");
                
                if (mCustomView != null) {
                    onHideCustomView();
                    return;
                }
                
                mCustomView = view;
                mCustomViewCallback = callback;
                
                // 保存原始状态
                if (mContext instanceof android.app.Activity) {
                    android.app.Activity activity = (android.app.Activity) mContext;
                    mOriginalOrientation = activity.getRequestedOrientation();
                    mOriginalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
                    
                    // 设置全屏和横屏
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    
                    // 隐藏系统UI
                    activity.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    );
                }
                
                // 添加视频视图到容器
                if (mListener != null) {
                    mFullscreenContainer = mListener.getVideoContainer();
                    if (mFullscreenContainer != null) {
                        // 确保容器是可见的
                        mFullscreenContainer.setVisibility(View.VISIBLE);

                        // 移除之前的视频视图（如果存在）
                        if (mFullscreenContainer.getChildCount() > 0) {
                            mFullscreenContainer.removeAllViews();
                        }

                        // 添加新的视频视图
                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        );
                        mFullscreenContainer.addView(mCustomView, params);

                        // 确保视图正确显示
                        mCustomView.setVisibility(View.VISIBLE);
                        mCustomView.requestLayout();

                        Log.d(TAG, "Video view added to fullscreen container");
                    } else {
                        Log.w(TAG, "Fullscreen container is null, video may not display properly");
                    }
                    mListener.onShowCustomView(view, callback);
                }
            }
            
            @Override
            public void onHideCustomView() {
                Log.d(TAG, "Exiting fullscreen video mode");
                
                if (mCustomView == null) {
                    return;
                }
                
                // 恢复原始状态
                if (mContext instanceof android.app.Activity) {
                    android.app.Activity activity = (android.app.Activity) mContext;
                    activity.setRequestedOrientation(mOriginalOrientation);
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    activity.getWindow().getDecorView().setSystemUiVisibility(mOriginalSystemUiVisibility);
                }
                
                // 移除视频视图
                if (mFullscreenContainer != null) {
                    mFullscreenContainer.removeView(mCustomView);
                    mFullscreenContainer.setVisibility(View.GONE);
                }
                
                // 清理资源
                if (mCustomViewCallback != null) {
                    mCustomViewCallback.onCustomViewHidden();
                }
                
                mCustomView = null;
                mCustomViewCallback = null;
                
                if (mListener != null) {
                    mListener.onHideCustomView();
                }
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // 可以在这里处理进度条更新
            }
        };
    }
    
    /**
     * YouTube特定优化
     */
    public void optimizeForYouTube(WebView webView) {
        if (webView == null) return;
        
        try {
            WebSettings settings = webView.getSettings();
            
            // YouTube特定的User Agent
            String userAgent = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36";
            settings.setUserAgentString(userAgent);
            
            // 启用JavaScript
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            
            // 支持多窗口
            settings.setSupportMultipleWindows(true);
            
            // 缓存策略优化
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            
            // 视频播放优化
            settings.setMediaPlaybackRequiresUserGesture(false);
            
            Log.d(TAG, "YouTube optimization applied");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to optimize for YouTube", e);
        }
    }
    
    /**
     * 注入视频播放优化脚本
     */
    public void injectVideoOptimizationScript(WebView webView) {
        if (webView == null) return;
        
        String script = 
            "(function() {" +
            "  // 优化视频播放性能" +
            "  var videos = document.getElementsByTagName('video');" +
            "  for (var i = 0; i < videos.length; i++) {" +
            "    var video = videos[i];" +
            "    // 设置预加载策略" +
            "    video.preload = 'metadata';" +
            "    // 启用硬件加速" +
            "    video.style.transform = 'translateZ(0)';" +
            "    // 防止视频过宽" +
            "    if (video.offsetWidth > window.innerWidth) {" +
            "      video.style.width = '100%';" +
            "      video.style.height = 'auto';" +
            "    }" +
            "  }" +
            "  " +
            "  // YouTube特殊处理" +
            "  if (window.location.hostname.includes('youtube.com')) {" +
            "    // 跳过广告" +
            "    var skipButton = document.querySelector('.ytp-ad-skip-button');" +
            "    if (skipButton) skipButton.click();" +
            "    " +
            "    // 设置画质为自动" +
            "    var player = document.getElementById('movie_player');" +
            "    if (player && player.setPlaybackQuality) {" +
            "      player.setPlaybackQuality('auto');" +
            "    }" +
            "  }" +
            "})();";
        
        try {
            webView.evaluateJavascript(script, null);
            Log.d(TAG, "Video optimization script injected");
        } catch (Exception e) {
            Log.w(TAG, "Failed to inject video optimization script", e);
        }
    }
    
    /**
     * 处理返回键，退出全屏视频
     */
    public boolean onBackPressed() {
        if (mCustomView != null) {
            if (mCustomViewCallback != null) {
                mCustomViewCallback.onCustomViewHidden();
            }
            return true; // 消费返回键事件
        }
        return false;
    }
    
    /**
     * 检查是否在全屏视频模式
     */
    public boolean isInFullscreenMode() {
        return mCustomView != null;
    }
    
    /**
     * 强制退出全屏模式
     */
    public void exitFullscreen() {
        if (mCustomView != null && mCustomViewCallback != null) {
            mCustomViewCallback.onCustomViewHidden();
        }
    }
    
    /**
     * 视频播放诊断工具
     */
    public void diagnoseVideoPlayback(WebView webView) {
        if (webView == null) return;

        Log.d(TAG, "=== VIDEO PLAYBACK DIAGNOSTICS ===");

        try {
            WebSettings settings = webView.getSettings();

            // 检查硬件加速
            Log.d(TAG, "Hardware acceleration layer: " + webView.getLayerType());

            // 检查JavaScript
            Log.d(TAG, "JavaScript enabled: " + settings.getJavaScriptEnabled());

            // 检查媒体播放设置
            Log.d(TAG, "Media playback requires gesture: " + settings.getMediaPlaybackRequiresUserGesture());

            // 检查插件支持
            try {
                Log.d(TAG, "Plugin state: " + settings.getPluginState());
            } catch (Exception e) {
                Log.d(TAG, "Plugin state: Not available on this Android version");
            }

            // 检查User Agent
            Log.d(TAG, "User Agent: " + settings.getUserAgentString());

            // 检查DOM存储
            Log.d(TAG, "DOM storage enabled: " + settings.getDomStorageEnabled());

            // 检查混合内容
            Log.d(TAG, "Mixed content mode: " + settings.getMixedContentMode());

            // 检查全屏容器状态
            if (mFullscreenContainer != null) {
                Log.d(TAG, "Fullscreen container exists, visibility: " + mFullscreenContainer.getVisibility());
                Log.d(TAG, "Fullscreen container child count: " + mFullscreenContainer.getChildCount());
            } else {
                Log.w(TAG, "Fullscreen container is null!");
            }

            // 检查是否在全屏模式
            Log.d(TAG, "In fullscreen mode: " + isInFullscreenMode());

        } catch (Exception e) {
            Log.e(TAG, "Error during video diagnostics", e);
        }

        Log.d(TAG, "=== END VIDEO DIAGNOSTICS ===");
    }

    /**
     * 强制重新初始化视频播放
     */
    public void reinitializeVideoPlayback(WebView webView) {
        if (webView == null) return;

        try {
            Log.d(TAG, "Reinitializing video playback settings");

            // 强制退出全屏
            exitFullscreen();

            // 重新应用优化设置
            optimizeVideoPlayback(webView);

            // 重新注入优化脚本
            injectVideoOptimizationScript(webView);

            Log.d(TAG, "Video playback reinitialized");

        } catch (Exception e) {
            Log.e(TAG, "Failed to reinitialize video playback", e);
        }
    }

    /**
     * 清理资源
     */
    public void destroy() {
        try {
            exitFullscreen();
            mListener = null;
            mFullscreenContainer = null;
            Log.d(TAG, "VideoPlaybackOptimizer destroyed");
        } catch (Exception e) {
            Log.w(TAG, "Error during VideoPlaybackOptimizer destruction", e);
        }
    }
}