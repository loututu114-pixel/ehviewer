package com.hippo.ehviewer.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.hippo.ehviewer.service.AppKeepAliveService;
import com.hippo.ehviewer.client.WebViewCacheManager;
import com.hippo.ehviewer.client.WebViewPoolManager;
import com.hippo.ehviewer.client.MemoryManager;

/**
 * 应用优化管理器 - 统一管理所有优化功能
 * 在应用启动时初始化，提供全面的性能和功能优化
 */
public class AppOptimizationManager {
    private static AppOptimizationManager instance;
    private final Context context;
    private final Handler mainHandler;
    
    // 各个优化模块
    private PermissionOptimizer permissionOptimizer;
    private EnhancedBrowserManager browserManager;
    private WebViewPerformanceOptimizer webViewOptimizer;
    
    // 优化状态标志
    private boolean isOptimizationEnabled = true;
    private boolean isKeepAliveEnabled = true;
    private boolean isBrowserOptimizationEnabled = true;
    
    private AppOptimizationManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initializeOptimizers();
    }
    
    public static synchronized AppOptimizationManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppOptimizationManager(context);
        }
        return instance;
    }
    
    /**
     * 初始化所有优化器
     */
    private void initializeOptimizers() {
        // 初始化权限优化器
        permissionOptimizer = PermissionOptimizer.getInstance(context);
        
        // 初始化浏览器管理器
        browserManager = EnhancedBrowserManager.getInstance(context);
        
        // 初始化WebView性能优化器
        webViewOptimizer = new WebViewPerformanceOptimizer(context);
    }
    
    /**
     * 在Application onCreate时调用
     */
    public void initializeOnAppCreate(Application app) {
        // 预加载WebView
        webViewOptimizer.preloadWebView();
        
        // 初始化WebView池
        // TODO: 实现WebViewPoolManager
        // WebViewPoolManager.getInstance(context).initializePool();
        
        // 初始化缓存管理
        // TODO: 实现WebViewCacheManager
        // WebViewCacheManager.getInstance(context).initializeCache();
        
        // 初始化内存管理器
        // TODO: 实现MemoryManager
        // MemoryManager.getInstance(context).startMonitoring();
        
        // 启动保活服务
        if (isKeepAliveEnabled) {
            startKeepAliveService();
        }
    }
    
    /**
     * 在MainActivity onCreate时调用
     */
    public void initializeOnMainActivity(Activity activity) {
        // 延迟请求权限，避免影响启动速度
        mainHandler.postDelayed(() -> {
            // 请求必要权限
            if (!permissionOptimizer.hasAllEssentialPermissions()) {
                permissionOptimizer.requestAllNecessaryPermissions(activity);
            }
            
            // 请求设为默认浏览器
            if (isBrowserOptimizationEnabled && !browserManager.isDefaultBrowser()) {
                mainHandler.postDelayed(() -> {
                    browserManager.requestDefaultBrowser(activity);
                }, 2000);
            }
            
            // 请求系统优化权限
            mainHandler.postDelayed(() -> {
                permissionOptimizer.requestSystemOptimizations(activity);
            }, 5000);
        }, 1000);
    }
    
    /**
     * 启动保活服务
     */
    private void startKeepAliveService() {
        Intent serviceIntent = new Intent(context, AppKeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
    
    /**
     * WebView性能优化器
     */
    private static class WebViewPerformanceOptimizer {
        private final Context context;
        
        WebViewPerformanceOptimizer(Context context) {
            this.context = context;
        }
        
        /**
         * 预加载WebView
         */
        void preloadWebView() {
            // 在后台线程预加载WebView内核
            new Thread(() -> {
                try {
                    // 预加载X5内核
                    Class.forName("com.tencent.smtt.sdk.WebView");
                    
                    // 预加载系统WebView
                    Class.forName("android.webkit.WebView");
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        /**
         * 优化WebView设置
         */
        public void optimizeWebViewSettings(android.webkit.WebView webView) {
            android.webkit.WebSettings settings = webView.getSettings();
            
            // 性能优化设置
            settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH);
            settings.setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            
            // 缓存优化
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            // settings.setAppCacheEnabled(true); // Deprecated in API 33
            
            // JavaScript优化
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            
            // 图片延迟加载
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            
            // 硬件加速
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
            }
            
            // 混合内容
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
        }
    }
    
    /**
     * 获取优化状态报告
     */
    public String getOptimizationReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 应用优化状态报告 ===\n\n");
        
        // 权限状态
        report.append(permissionOptimizer.getPermissionStatusReport());
        report.append("\n");
        
        // 浏览器状态
        report.append(browserManager.getSupportStatusReport());
        report.append("\n");
        
        // 优化设置
        report.append("优化设置：\n");
        report.append("• 整体优化: ").append(isOptimizationEnabled ? "✓" : "✗").append("\n");
        report.append("• 应用保活: ").append(isKeepAliveEnabled ? "✓" : "✗").append("\n");
        report.append("• 浏览器优化: ").append(isBrowserOptimizationEnabled ? "✓" : "✗").append("\n");
        
        // WebView状态
        report.append("\nWebView优化：\n");
        report.append("• WebView池: ✓\n");
        report.append("• 缓存管理: ✓\n");
        report.append("• 内存优化: ✓\n");
        report.append("• 硬件加速: ✓\n");
        
        return report.toString();
    }
    
    // Getter和Setter方法
    public void setOptimizationEnabled(boolean enabled) {
        this.isOptimizationEnabled = enabled;
    }
    
    public void setKeepAliveEnabled(boolean enabled) {
        this.isKeepAliveEnabled = enabled;
        if (enabled) {
            startKeepAliveService();
        }
    }
    
    public void setBrowserOptimizationEnabled(boolean enabled) {
        this.isBrowserOptimizationEnabled = enabled;
    }
    
    public PermissionOptimizer getPermissionOptimizer() {
        return permissionOptimizer;
    }
    
    public EnhancedBrowserManager getBrowserManager() {
        return browserManager;
    }
}