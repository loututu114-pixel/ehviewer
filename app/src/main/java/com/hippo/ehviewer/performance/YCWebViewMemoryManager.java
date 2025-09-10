package com.hippo.ehviewer.performance;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YCWebView风格的内存管理器
 * 参考YCWebView的最佳实践，实现内存泄漏防护和性能优化
 *
 * 核心特性：
 * 1. Activity生命周期监听
 * 2. 页面关闭后停止JS执行
 * 3. 智能内存清理
 * 4. WebView复用管理
 * 5. 后台资源限制
 */
public class YCWebViewMemoryManager {

    private static final String TAG = "YCWebViewMemoryManager";

    private static YCWebViewMemoryManager instance;
    private final Context context;
    private final Handler mainHandler;

    // WebView引用管理
    private final Map<String, WeakReference<WebView>> webViewRefs = new ConcurrentHashMap<>();

    // 内存监控
    private static final long MEMORY_CHECK_INTERVAL = 30000; // 30秒
    private final Handler memoryHandler = new Handler(Looper.getMainLooper());
    private final Runnable memoryCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndOptimizeMemory();
            memoryHandler.postDelayed(this, MEMORY_CHECK_INTERVAL);
        }
    };

    private YCWebViewMemoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 启动内存监控
        startMemoryMonitoring();
    }

    public static synchronized YCWebViewMemoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new YCWebViewMemoryManager(context);
        }
        return instance;
    }

    /**
     * YCWebView最佳实践：注册WebView
     */
    public void registerWebView(String tag, WebView webView) {
        if (webView != null) {
            webViewRefs.put(tag, new WeakReference<>(webView));
            Log.d(TAG, "YCWebView: Registered WebView with tag: " + tag);
        }
    }

    /**
     * YCWebView最佳实践：注销WebView
     */
    public void unregisterWebView(String tag) {
        WeakReference<WebView> ref = webViewRefs.remove(tag);
        if (ref != null && ref.get() != null) {
            cleanupWebView(ref.get());
            Log.d(TAG, "YCWebView: Unregistered and cleaned up WebView with tag: " + tag);
        }
    }

    /**
     * YCWebView最佳实践：清理WebView资源
     */
    private void cleanupWebView(WebView webView) {
        if (webView == null) return;

        try {
            // YCWebView最佳实践：停止加载
            webView.stopLoading();

            // YCWebView最佳实践：清理缓存
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();

            // YCWebView最佳实践：停止JS执行
            webView.getSettings().setJavaScriptEnabled(false);

            // YCWebView最佳实践：移除所有视图
            if (webView.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) webView.getParent()).removeView(webView);
            }

            // YCWebView最佳实践：加载空白页面
            webView.loadUrl("about:blank");

            Log.d(TAG, "YCWebView: Cleaned up WebView resources");

        } catch (Exception e) {
            Log.e(TAG, "YCWebView: Error cleaning up WebView", e);
        }
    }

    /**
     * YCWebView最佳实践：Activity销毁时清理
     */
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "YCWebView: Activity destroyed, cleaning up all WebViews");

        // 清理所有注册的WebView
        for (Map.Entry<String, WeakReference<WebView>> entry : webViewRefs.entrySet()) {
            WeakReference<WebView> ref = entry.getValue();
            if (ref.get() != null) {
                cleanupWebView(ref.get());
            }
        }

        webViewRefs.clear();
    }

    /**
     * YCWebView最佳实践：内存优化
     */
    public void optimizeMemoryUsage() {
        Log.d(TAG, "YCWebView: Optimizing memory usage");

        // 清理软引用
        Runtime.getRuntime().gc();

        // 遍历所有WebView进行优化
        for (Map.Entry<String, WeakReference<WebView>> entry : webViewRefs.entrySet()) {
            WeakReference<WebView> ref = entry.getValue();
            WebView webView = ref.get();
            if (webView != null) {
                optimizeWebView(webView);
            }
        }
    }

    /**
     * YCWebView最佳实践：优化单个WebView
     */
    private void optimizeWebView(WebView webView) {
        try {
            // YCWebView最佳实践：禁用图片加载以节省内存
            if (webView.getSettings().getLoadsImagesAutomatically()) {
                webView.getSettings().setLoadsImagesAutomatically(false);
            }

            // YCWebView最佳实践：清理不必要的缓存
            webView.clearCache(false);

            // YCWebView最佳实践：降低渲染质量
            webView.getSettings().setRenderPriority(android.webkit.WebSettings.RenderPriority.LOW);

        } catch (Exception e) {
            Log.e(TAG, "YCWebView: Error optimizing WebView", e);
        }
    }

    /**
     * YCWebView最佳实践：启动内存监控
     */
    private void startMemoryMonitoring() {
        memoryHandler.postDelayed(memoryCheckRunnable, MEMORY_CHECK_INTERVAL);
        Log.d(TAG, "YCWebView: Memory monitoring started");
    }

    /**
     * YCWebView最佳实践：停止内存监控
     */
    public void stopMemoryMonitoring() {
        memoryHandler.removeCallbacks(memoryCheckRunnable);
        Log.d(TAG, "YCWebView: Memory monitoring stopped");
    }

    /**
     * YCWebView最佳实践：检查并优化内存
     */
    private void checkAndOptimizeMemory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();

            // 如果内存使用超过80%，触发优化
            if (usedMemory > maxMemory * 0.8) {
                Log.w(TAG, "YCWebView: High memory usage detected, triggering optimization");
                optimizeMemoryUsage();
            }

        } catch (Exception e) {
            Log.e(TAG, "YCWebView: Error during memory check", e);
        }
    }

    /**
     * YCWebView最佳实践：获取内存统计信息
     */
    public MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        return new MemoryStats(
            runtime.totalMemory(),
            runtime.freeMemory(),
            runtime.maxMemory(),
            webViewRefs.size()
        );
    }

    /**
     * YCWebView最佳实践：内存统计信息类
     */
    public static class MemoryStats {
        public final long totalMemory;
        public final long freeMemory;
        public final long maxMemory;
        public final int activeWebViews;

        public MemoryStats(long totalMemory, long freeMemory, long maxMemory, int activeWebViews) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
            this.activeWebViews = activeWebViews;
        }

        @Override
        public String toString() {
            return String.format("MemoryStats{total=%dKB, free=%dKB, max=%dKB, webViews=%d}",
                    totalMemory / 1024, freeMemory / 1024, maxMemory / 1024, activeWebViews);
        }
    }
}
