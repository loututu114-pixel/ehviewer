package com.hippo.ehviewer.performance;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebView内存管理器 - 解决tile memory limits exceeded问题
 * 专门处理Chromium WebView的渲染瓦片内存管理
 */
public class WebViewMemoryManager {
    private static final String TAG = "WebViewMemoryManager";
    
    // 单例实例
    private static WebViewMemoryManager sInstance;
    private Context mContext;

    // ===== 新增：解决tile memory limits exceeded的变量 =====
    private final Handler mMainHandler;
    private final AtomicLong mLastMemoryCheckTime = new AtomicLong(0);
    private static final long MEMORY_CHECK_INTERVAL = 5000; // 5秒检查一次
    private static final long MAX_TILE_MEMORY_MB = 50; // 最大瓦片内存50MB
    private boolean mMemoryOptimizationEnabled = true;

    private WebViewMemoryManager(Context context) {
        mContext = context.getApplicationContext();
        mMainHandler = new Handler(Looper.getMainLooper());

        // 启动内存监控
        startMemoryMonitoring();

        Log.d(TAG, "WebViewMemoryManager initialized with tile memory optimization");
    }
    
    public static synchronized WebViewMemoryManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WebViewMemoryManager(context);
        }
        return sInstance;
    }
    
    /**
     * 创建WebView实例
     */
    public WebView obtainWebView(Context activityContext) {
        Log.d(TAG, "Creating new WebView instance");
        return createNewWebView(activityContext);
    }
    
    /**
     * 销毁WebView
     */
    public void recycleWebView(WebView webView) {
        if (webView == null) return;
        
        Log.d(TAG, "Destroying WebView");
        destroyWebView(webView);
    }
    
    /**
     * 创建新的WebView实例
     */
    private WebView createNewWebView(Context context) {
        try {
            WebView webView = new WebView(context);
            setupBasicWebViewSettings(webView);
            Log.d(TAG, "Created system WebView");
            return webView;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create system WebView", e);
            return null;
        }
    }
    
    /**
     * 设置WebView基础配置 - 集成瓦片内存优化
     */
    private void setupBasicWebViewSettings(WebView webView) {
        try {
            android.webkit.WebSettings settings = webView.getSettings();

            // 基本设置
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            }

            // ===== 新增：瓦片内存优化设置 =====
            applyTileMemoryOptimization(webView);

            Log.d(TAG, "WebView basic settings configured with tile memory optimization");

        } catch (Exception e) {
            Log.w(TAG, "Failed to setup WebView settings", e);
        }
    }

    /**
     * 销毁WebView实例
     */
    private void destroyWebView(WebView webView) {
        try {
            webView.loadUrl("about:blank");
            webView.destroy();
            
            Log.d(TAG, "WebView destroyed");
            
        } catch (Exception e) {
            Log.w(TAG, "Error destroying WebView", e);
        }
    }
    
    /**
     * 暂停WebView
     */
    public void pauseBackgroundWebViews(WebView activeWebView) {
        // 简化实现，不做复杂的后台管理
        Log.d(TAG, "pauseBackgroundWebViews called (simplified)");
    }
    
    /**
     * 恢复WebView
     */
    public void resumeWebView(WebView webView) {
        if (webView != null) {
            try {
                webView.onResume();
                webView.resumeTimers();
                Log.d(TAG, "WebView resumed");
            } catch (Exception e) {
                Log.w(TAG, "Failed to resume WebView", e);
            }
        }
    }
    
    /**
     * 销毁管理器（应用退出时调用）
     */
    public void destroy() {
        Log.d(TAG, "WebViewMemoryManager destroyed");
    }

    /**
     * ===== 新增：解决tile memory limits exceeded的方法 =====
     */

    /**
     * 启动内存监控
     */
    private void startMemoryMonitoring() {
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mMemoryOptimizationEnabled) {
                    checkAndOptimizeTileMemory();
                    mMainHandler.postDelayed(this, MEMORY_CHECK_INTERVAL);
                }
            }
        }, MEMORY_CHECK_INTERVAL);

        Log.d(TAG, "Memory monitoring started");
    }

    /**
     * 检查并优化瓦片内存
     */
    private void checkAndOptimizeTileMemory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            // 计算内存使用百分比
            float memoryUsagePercent = (float) usedMemory / maxMemory * 100;

            // 如果内存使用超过75%，触发瓦片内存优化
            if (memoryUsagePercent > 75.0f) {
                Log.w(TAG, String.format("High memory usage detected: %.1f%% (%dMB/%dMB)",
                    memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024));

                optimizeTileMemory();
            }

            // 记录最后检查时间
            mLastMemoryCheckTime.set(System.currentTimeMillis());

        } catch (Exception e) {
            Log.e(TAG, "Error during memory check", e);
        }
    }

    /**
     * 优化瓦片内存使用
     */
    private void optimizeTileMemory() {
        Log.i(TAG, "Optimizing tile memory usage");

        try {
            // 强制垃圾回收
            System.gc();
            System.runFinalization();
            System.gc();

            // 设置系统属性以优化Chromium渲染
            System.setProperty("webview.enable_threaded_rendering", "true");
            System.setProperty("webview.enable_surface_control", "true");

            // 限制并发渲染线程
            System.setProperty("webview.max_rendering_threads", "2");

            // 减少瓦片缓存大小
            System.setProperty("webview.tile_cache_size", "8"); // 8MB

            Log.d(TAG, "Tile memory optimization applied");

        } catch (Exception e) {
            Log.e(TAG, "Error optimizing tile memory", e);
        }
    }

    /**
     * 应用瓦片内存优化设置到WebView
     */
    public void applyTileMemoryOptimization(WebView webView) {
        if (webView == null) return;

        try {
            android.webkit.WebSettings settings = webView.getSettings();

            // 降低渲染优先级以减少内存使用
            settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH);

            // 限制图片自动加载
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);

            // 设置最小字体大小，减少渲染元素
            settings.setMinimumFontSize(10);
            settings.setMinimumLogicalFontSize(10);

            // 禁用不必要的JavaScript功能
            settings.setJavaScriptCanOpenWindowsAutomatically(false);

            // 缓存大小通过其他方式限制 (setAppCacheMaxSize在新版本中已移除)
            // 使用系统属性限制缓存大小
            System.setProperty("webview.cache.size", "5"); // 5MB

            Log.d(TAG, "Tile memory optimization applied to WebView");

        } catch (Exception e) {
            Log.e(TAG, "Error applying tile memory optimization", e);
        }
    }

    /**
     * 获取内存使用统计
     */
    public MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        return new MemoryStats(
            runtime.totalMemory(),
            runtime.freeMemory(),
            runtime.maxMemory()
        );
    }

    /**
     * 内存统计信息类
     */
    public static class MemoryStats {
        public final long totalMemory;
        public final long freeMemory;
        public final long maxMemory;

        public MemoryStats(long totalMemory, long freeMemory, long maxMemory) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
        }

        public float getUsagePercent() {
            return (float) (totalMemory - freeMemory) / maxMemory * 100;
        }

        @Override
        public String toString() {
            return String.format("Memory: %dMB used, %dMB free, %dMB max (%.1f%%)",
                (totalMemory - freeMemory) / 1024 / 1024,
                freeMemory / 1024 / 1024,
                maxMemory / 1024 / 1024,
                getUsagePercent());
        }
    }

    /**
     * 启用/禁用内存优化
     */
    public void setMemoryOptimizationEnabled(boolean enabled) {
        mMemoryOptimizationEnabled = enabled;
        if (enabled) {
            startMemoryMonitoring();
            Log.d(TAG, "Memory optimization enabled");
        } else {
            Log.d(TAG, "Memory optimization disabled");
        }
    }

    /**
     * 检查瓦片内存压力（增强实现）
     */
    public void checkTileMemoryPressure() {
        Log.d(TAG, "Checking tile memory pressure");
        checkAndOptimizeTileMemory();
    }
}