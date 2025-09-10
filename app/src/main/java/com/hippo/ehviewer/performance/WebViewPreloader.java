package com.hippo.ehviewer.performance;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.hippo.ehviewer.ui.browser.EnhancedWebViewClient;
import com.hippo.ehviewer.ui.browser.EnhancedWebChromeClient;
import com.hippo.ehviewer.util.UserAgentManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebView预加载管理器
 * 通过预创建和配置WebView实例来显著提升浏览器启动性能
 * 
 * 核心特性：
 * 1. 应用启动时预创建WebView实例
 * 2. 预配置常用设置和客户端
 * 3. 智能内存管理和回收策略
 * 4. X5内核预初始化
 */
public class WebViewPreloader {
    private static final String TAG = "WebViewPreloader";
    
    // 预加载WebView池
    private static WebView sPreloadedWebView;
    private static boolean sIsPreloading = false;
    private static boolean sIsPreloaded = false;
    
    // 后台线程池
    private static final ExecutorService sBackgroundExecutor = Executors.newSingleThreadExecutor();
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    
    // 预加载监听器
    public interface PreloadListener {
        void onPreloadStarted();
        void onPreloadCompleted();
        void onPreloadFailed(Exception e);
    }
    
    /**
     * 预加载WebView实例
     * 应在应用启动时调用，在后台线程中执行以避免阻塞主线程
     */
    public static void preloadWebView(Context context) {
        preloadWebView(context, null);
    }
    
    public static void preloadWebView(Context context, PreloadListener listener) {
        if (sIsPreloading || sIsPreloaded) {
            Log.d(TAG, "WebView preloading already in progress or completed");
            if (listener != null && sIsPreloaded) {
                listener.onPreloadCompleted();
            }
            return;
        }
        
        Log.d(TAG, "Starting WebView preload process");
        sIsPreloading = true;
        
        if (listener != null) {
            listener.onPreloadStarted();
        }
        
        // 在后台线程中进行预加载准备工作
        sBackgroundExecutor.execute(() -> {
            try {
                // 预初始化X5内核相关设置
                preInitializeX5Kernel(context);
                
                // 在主线程中创建WebView实例
                sMainHandler.post(() -> createPreloadedWebView(context, listener));
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to preload WebView", e);
                sIsPreloading = false;
                if (listener != null) {
                    listener.onPreloadFailed(e);
                }
            }
        });
    }
    
    /**
     * 预初始化X5内核
     */
    private static void preInitializeX5Kernel(Context context) {
        try {
            // 预热X5内核初始化过程
            Log.d(TAG, "Pre-initializing X5 WebView kernel");
            
            // 这里可以添加X5内核的预初始化代码
            // 例如：QbSdk.preInit(context, callback);
            
            // 预设Cookie管理器
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(null, true);
            
        } catch (Exception e) {
            Log.w(TAG, "X5 kernel pre-initialization failed", e);
        }
    }
    
    /**
     * 在主线程中创建预加载的WebView实例
     */
    private static void createPreloadedWebView(Context context, PreloadListener listener) {
        try {
            Log.d(TAG, "Creating preloaded WebView instance");
            
            // 使用Application Context避免内存泄漏
            Context appContext = context.getApplicationContext();
            
            // 创建WebView实例
            sPreloadedWebView = new WebView(appContext);
            
            // 配置WebView设置
            setupWebViewSettings(sPreloadedWebView, appContext);
            
            // 设置客户端（使用简化版本，避免Activity依赖）
            setupWebViewClients(sPreloadedWebView);
            
            // 预加载空白页面以完成初始化
            sPreloadedWebView.loadUrl("about:blank");
            
            sIsPreloaded = true;
            sIsPreloading = false;
            
            Log.d(TAG, "WebView preload completed successfully");
            
            if (listener != null) {
                listener.onPreloadCompleted();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create preloaded WebView", e);
            sIsPreloading = false;
            
            if (listener != null) {
                listener.onPreloadFailed(e);
            }
        }
    }
    
    /**
     * 配置WebView基础设置
     */
    private static void setupWebViewSettings(WebView webView, Context context) {
        WebSettings settings = webView.getSettings();
        
        // 基本设置
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        // settings.setAppCacheEnabled(true); // Deprecated in API 33
        
        // 缓存设置
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 用户代理
        try {
            com.hippo.ehviewer.util.UserAgentManager userAgentManager = 
                new com.hippo.ehviewer.util.UserAgentManager(context);
            String userAgent = userAgentManager.getDefaultUserAgent();
            if (userAgent != null) {
                settings.setUserAgentString(userAgent);
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to set user agent", e);
        }
        
        // 支持缩放
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        // 文件访问
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        
        // 混合内容
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        Log.d(TAG, "WebView settings configured");
    }
    
    /**
     * 设置WebView客户端（简化版本）
     */
    private static void setupWebViewClients(WebView webView) {
        try {
            // 由于预加载时没有Activity上下文，这里设置一个基础的WebViewClient
            webView.setWebViewClient(new android.webkit.WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // 预加载阶段不处理URL跳转
                    return false;
                }
            });
            
            // 设置基础的ChromeClient
            webView.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    // 预加载阶段不处理进度更新
                }
            });
            
            Log.d(TAG, "WebView clients configured");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to setup WebView clients during preload", e);
        }
    }
    
    /**
     * 获取预加载的WebView实例
     * 调用后会清空预加载池，需要重新预加载
     */
    public static WebView getPreloadedWebView() {
        WebView webView = sPreloadedWebView;
        if (webView != null) {
            Log.d(TAG, "Providing preloaded WebView instance");
            
            // 清空引用，避免重复使用
            sPreloadedWebView = null;
            sIsPreloaded = false;
            
            return webView;
        }
        
        Log.d(TAG, "No preloaded WebView available");
        return null;
    }
    
    /**
     * 检查预加载状态
     */
    public static boolean isPreloaded() {
        return sIsPreloaded && sPreloadedWebView != null;
    }
    
    public static boolean isPreloading() {
        return sIsPreloading;
    }
    
    /**
     * 重新配置WebView以适应特定Activity
     * 在Activity中使用预加载的WebView时调用
     */
    public static void reconfigureForActivity(WebView webView, Context activityContext) {
        if (webView == null) return;
        
        try {
            Log.d(TAG, "Reconfiguring WebView for activity context");
            
            // 这里可以根据Activity的具体需求重新配置WebView
            // 例如重新设置WebViewClient和WebChromeClient
            
            // 清理预加载时的内容
            webView.clearHistory();
            webView.clearCache(false);
            
            Log.d(TAG, "WebView reconfiguration completed");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to reconfigure WebView for activity", e);
        }
    }
    
    /**
     * 清理预加载资源
     * 在应用退出或内存紧张时调用
     */
    public static void cleanup() {
        Log.d(TAG, "Cleaning up WebView preloader resources");
        
        if (sPreloadedWebView != null) {
            try {
                sPreloadedWebView.clearHistory();
                sPreloadedWebView.clearCache(true);
                sPreloadedWebView.destroy();
            } catch (Exception e) {
                Log.w(TAG, "Error during WebView cleanup", e);
            }
            sPreloadedWebView = null;
        }
        
        sIsPreloaded = false;
        sIsPreloading = false;
    }
    
    /**
     * 获取预加载性能统计
     */
    public static String getPreloadStats() {
        return String.format("Preloaded: %s, Preloading: %s", 
                           sIsPreloaded, sIsPreloading);
    }
}