package com.hippo.ehviewer.performance;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.View;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 优化的WebView管理器
 * 参考YCWebView的最佳实践，提供高性能的WebView管理
 * 
 * 核心特性：
 * 1. 智能内存管理 - 防止内存泄漏
 * 2. 生命周期管理 - 合理的暂停/恢复机制
 * 3. 缓存优化 - 提升加载性能
 * 4. 视频播放优化 - 解决卡顿问题
 */
public class OptimizedWebViewManager {
    private static final String TAG = "OptimizedWebViewManager";
    
    // 单例实例
    private static OptimizedWebViewManager sInstance;
    private Context mContext;
    
    // WebView实例管理
    private final Map<String, WebView> mWebViewPool = new ConcurrentHashMap<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    
    // 配置参数
    private static final long BACKGROUND_CLEANUP_DELAY = 5 * 60 * 1000L; // 5分钟后清理
    private static final String USER_AGENT_SUFFIX = " YCWebView/1.0";
    
    private OptimizedWebViewManager(Context context) {
        mContext = context.getApplicationContext();
        Log.d(TAG, "OptimizedWebViewManager initialized");
    }
    
    public static synchronized OptimizedWebViewManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new OptimizedWebViewManager(context);
        }
        return sInstance;
    }
    
    /**
     * 创建优化的WebView实例
     */
    public WebView createOptimizedWebView(Context activityContext) {
        try {
            WebView webView = new WebView(activityContext);
            setupOptimizedSettings(webView);
            Log.d(TAG, "Created optimized WebView");
            return webView;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create optimized WebView", e);
            return null;
        }
    }
    
    /**
     * 设置优化的WebView配置
     * 参考YCWebView的最佳实践
     */
    private void setupOptimizedSettings(WebView webView) {
        if (webView == null) return;
        
        try {
            WebSettings settings = webView.getSettings();
            
            // ===== 基础设置 =====
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            
            // ===== 缓存优化设置 =====
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            // ApplicationCache已在API 33中被移除，无需设置
            
            // ===== 视频播放优化 =====
            settings.setMediaPlaybackRequiresUserGesture(false); // 允许自动播放
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            
            // ===== 性能优化设置 =====
            settings.setRenderPriority(WebSettings.RenderPriority.HIGH); // 提高渲染优先级
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            
            // ===== 安全设置 =====
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            }
            
            // ===== User Agent优化 =====
            String userAgent = settings.getUserAgentString();
            if (userAgent != null && !userAgent.contains("YCWebView")) {
                settings.setUserAgentString(userAgent + USER_AGENT_SUFFIX);
            }
            
            // ===== 硬件加速优化 =====
            setupHardwareAcceleration(webView);
            
            // ===== 图片加载优化 =====
            settings.setLoadsImagesAutomatically(false); // 延迟加载图片，提升首屏速度
            settings.setBlockNetworkImage(false);
            
            Log.d(TAG, "WebView optimized settings applied");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to setup optimized WebView settings", e);
        }
    }
    
    /**
     * 设置硬件加速
     * 优化视频播放性能
     */
    private void setupHardwareAcceleration(WebView webView) {
        try {
            // 启用硬件加速以优化视频播放
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            Log.d(TAG, "Hardware acceleration enabled for video playback");
        } catch (Exception e) {
            Log.w(TAG, "Failed to enable hardware acceleration", e);
            // 降级到软件渲染
            try {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            } catch (Exception fallbackE) {
                Log.e(TAG, "Failed to fallback to software rendering", fallbackE);
            }
        }
    }
    
    /**
     * 优化的页面加载完成处理
     * 启用图片加载，优化用户体验
     */
    public void onPageFinished(WebView webView) {
        if (webView == null) return;
        
        try {
            // 页面加载完成后启用图片加载
            WebSettings settings = webView.getSettings();
            if (!settings.getLoadsImagesAutomatically()) {
                settings.setLoadsImagesAutomatically(true);
                Log.d(TAG, "Images loading enabled after page finished");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to enable images loading", e);
        }
    }
    
    /**
     * WebView暂停优化
     * 释放资源，防止内存泄漏
     */
    public void pauseWebView(WebView webView) {
        if (webView == null) return;
        
        try {
            webView.onPause();
            webView.pauseTimers();
            
            // 暂停JavaScript执行，节省CPU和内存
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(false);
            
            Log.d(TAG, "WebView paused and optimized");
        } catch (Exception e) {
            Log.w(TAG, "Failed to pause WebView", e);
        }
    }
    
    /**
     * WebView恢复优化
     */
    public void resumeWebView(WebView webView) {
        if (webView == null) return;
        
        try {
            webView.onResume();
            webView.resumeTimers();
            
            // 恢复JavaScript执行
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            
            Log.d(TAG, "WebView resumed and optimized");
        } catch (Exception e) {
            Log.w(TAG, "Failed to resume WebView", e);
        }
    }
    
    /**
     * 优化的WebView销毁
     * 完全清理资源，防止内存泄漏
     */
    public void destroyWebView(WebView webView) {
        if (webView == null) return;
        
        try {
            // 停止所有加载
            webView.stopLoading();
            
            // 清除历史和缓存
            webView.clearHistory();
            webView.clearCache(false); // 保留磁盘缓存
            webView.clearFormData();
            
            // 加载空白页释放内存
            webView.loadUrl("about:blank");
            
            // 暂停所有活动
            webView.onPause();
            webView.pauseTimers();
            
            // 移除所有视图
            webView.removeAllViews();
            
            // 最终销毁
            webView.destroy();
            
            Log.d(TAG, "WebView destroyed with optimized cleanup");
            
        } catch (Exception e) {
            Log.w(TAG, "Error during WebView destruction", e);
        }
    }
    
    /**
     * 内存优化清理
     * 当系统内存不足时调用
     */
    public void onLowMemory() {
        Log.w(TAG, "Low memory detected - performing cleanup");
        
        try {
            // 清理WebView池中的实例
            for (WebView webView : mWebViewPool.values()) {
                if (webView != null) {
                    webView.clearCache(true);
                    webView.freeMemory();
                }
            }
            
            // 强制垃圾回收
            System.gc();
            
            Log.d(TAG, "Low memory cleanup completed");
        } catch (Exception e) {
            Log.w(TAG, "Failed to perform low memory cleanup", e);
        }
    }
    
    /**
     * 预加载优化
     * 提前初始化WebView，减少首次加载时间
     */
    public void preloadWebView(Context activityContext) {
        mMainHandler.post(() -> {
            try {
                WebView webView = createOptimizedWebView(activityContext);
                if (webView != null) {
                    // 预加载一个简单页面来初始化内核
                    webView.loadUrl("about:blank");
                    Log.d(TAG, "WebView preloaded successfully");
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to preload WebView", e);
            }
        });
    }
    
    /**
     * 获取WebView内存使用情况
     */
    public String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return String.format(
            "Memory: %d/%d MB (%.1f%%), Pool: %d WebViews",
            usedMemory / 1024 / 1024,
            maxMemory / 1024 / 1024,
            (double) usedMemory / maxMemory * 100,
            mWebViewPool.size()
        );
    }
    
    /**
     * 销毁管理器
     */
    public void destroy() {
        try {
            // 清理所有WebView
            for (WebView webView : mWebViewPool.values()) {
                destroyWebView(webView);
            }
            mWebViewPool.clear();
            
            // 清理Handler
            mMainHandler.removeCallbacksAndMessages(null);
            
            Log.d(TAG, "OptimizedWebViewManager destroyed");
        } catch (Exception e) {
            Log.w(TAG, "Error during manager destruction", e);
        }
    }
}