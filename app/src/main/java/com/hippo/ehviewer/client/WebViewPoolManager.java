package com.hippo.ehviewer.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebView连接池管理器
 * 基于腾讯X5和YCWebView最佳实践，实现高性能的WebView复用
 *
 * 核心特性：
 * - WebView实例复用，减少创建销毁开销
 * - 智能预热和优化
 * - 内存管理和泄漏防护
 * - 性能监控和调优
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class WebViewPoolManager {

    private static final String TAG = "WebViewPoolManager";

    // 连接池配置
    private static final int MAX_POOL_SIZE = 3;
    private static final int MIN_POOL_SIZE = 1;
    private static final long WEBVIEW_TIMEOUT = 300000; // 5分钟超时
    private static final long CLEANUP_INTERVAL = 60000; // 1分钟清理一次

    // 连接池状态
    private final LinkedBlockingQueue<WebView> availableWebViews = new LinkedBlockingQueue<>();
    private final Set<WebView> inUseWebViews = new HashSet<>();
    private final Set<WebView> allWebViews = new HashSet<>();

    // 管理组件
    private final Context context;
    private final Handler mainHandler;
    private final WebViewOptimizer optimizer;
    private boolean isCleanupRunning = false;

    // 统计信息
    private long totalCreated = 0;
    private long totalDestroyed = 0;
    private long totalAcquired = 0;
    private long totalReleased = 0;

    public WebViewPoolManager(Context context, int maxPoolSize) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.optimizer = new WebViewOptimizer();

        // 初始化连接池
        initializePool(maxPoolSize);

        // 启动清理服务
        startCleanupService();

        Log.i(TAG, "WebViewPoolManager initialized with max size: " + maxPoolSize);
    }

    /**
     * 初始化连接池
     */
    private void initializePool(int maxPoolSize) {
        mainHandler.post(() -> {
            try {
                // 创建最小数量的WebView实例
                for (int i = 0; i < MIN_POOL_SIZE; i++) {
                    WebView webView = createWebView();
                    if (webView != null) {
                        availableWebViews.offer(webView);
                        allWebViews.add(webView);
                    }
                }

                Log.d(TAG, "Initialized WebView pool with " + availableWebViews.size() + " instances");

            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize WebView pool", e);
            }
        });
    }

    /**
     * 创建优化的WebView实例
     */
    private WebView createWebView() {
        try {
            WebView webView = new WebView(context);

            // 配置基本设置
            configureWebView(webView);

            // 应用优化设置
            optimizer.optimizeWebView(webView);

            // 设置标识
            webView.setTag("pool_webview_" + System.currentTimeMillis());

            totalCreated++;
            Log.d(TAG, "Created new WebView instance, total created: " + totalCreated);

            return webView;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create WebView", e);
            return null;
        }
    }

    /**
     * 配置WebView基本设置
     */
    private void configureWebView(WebView webView) {
        WebSettings settings = webView.getSettings();

        // 基础设置
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        // 注意：setAppCacheEnabled在新版本中已被弃用

        // 渲染优化
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // 网络优化
        settings.setBlockNetworkImage(false);
        settings.setLoadsImagesAutomatically(true);

        // 缓存设置 - 使用现代API
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 安全设置
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        // 其他优化
        settings.setTextZoom(100);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
    }

    /**
     * 获取WebView实例
     */
    public WebView acquire() {
        WebView webView = availableWebViews.poll();

        if (webView == null) {
            // 连接池为空，创建新实例
            webView = createWebView();
            if (webView != null) {
                allWebViews.add(webView);
            }
        }

        if (webView != null) {
            inUseWebViews.add(webView);
            totalAcquired++;

            // 验证WebView状态
            if (!isWebViewValid(webView)) {
                Log.w(TAG, "Invalid WebView acquired, creating new one");
                release(webView); // 释放无效实例
                return acquire(); // 递归获取新实例
            }

            Log.d(TAG, "Acquired WebView from pool, available: " + availableWebViews.size() +
                      ", in use: " + inUseWebViews.size());
        }

        return webView;
    }

    /**
     * 释放WebView实例
     */
    public void release(WebView webView) {
        if (webView == null || !inUseWebViews.contains(webView)) {
            return;
        }

        try {
            inUseWebViews.remove(webView);

            // 重置WebView状态
            resetWebView(webView);

            // 检查连接池是否已满
            if (availableWebViews.size() < MAX_POOL_SIZE) {
                availableWebViews.offer(webView);
                Log.d(TAG, "Released WebView to pool, available: " + availableWebViews.size());
            } else {
                // 连接池已满，销毁实例
                destroyWebView(webView);
            }

            totalReleased++;

        } catch (Exception e) {
            Log.e(TAG, "Failed to release WebView", e);
            destroyWebView(webView);
        }
    }

    /**
     * 重置WebView状态
     */
    private void resetWebView(WebView webView) {
        try {
            mainHandler.post(() -> {
                try {
                    // 停止加载
                    webView.stopLoading();

                    // 清理缓存和历史记录
                    webView.clearCache(true);
                    webView.clearHistory();
                    webView.clearFormData();
                    webView.clearMatches();

                    // 加载空白页
                    webView.loadUrl("about:blank");

                    // 移除所有JavaScript接口
                    webView.removeJavascriptInterface("android");
                    webView.removeJavascriptInterface("ehviewer");

                    // 重置缩放
                    webView.setInitialScale(100);

                    Log.d(TAG, "WebView reset successfully");

                } catch (Exception e) {
                    Log.e(TAG, "Failed to reset WebView", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to post WebView reset", e);
        }
    }

    /**
     * 销毁WebView实例
     */
    private void destroyWebView(WebView webView) {
        try {
            mainHandler.post(() -> {
                try {
                    // 从父容器中移除
                    ViewGroup parent = (ViewGroup) webView.getParent();
                    if (parent != null) {
                        parent.removeView(webView);
                    }

                    // 销毁WebView
                    webView.destroy();

                    // 从集合中移除
                    allWebViews.remove(webView);

                    totalDestroyed++;
                    Log.d(TAG, "Destroyed WebView, total destroyed: " + totalDestroyed);

                } catch (Exception e) {
                    Log.e(TAG, "Failed to destroy WebView", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to post WebView destroy", e);
        }
    }

    /**
     * 验证WebView状态
     */
    private boolean isWebViewValid(WebView webView) {
        try {
            // 检查WebView是否还有父容器
            if (webView.getParent() != null) {
                Log.w(TAG, "WebView still has parent view");
                return false;
            }

            // 检查WebView的基本功能
            WebSettings settings = webView.getSettings();
            if (settings == null) {
                Log.w(TAG, "WebView settings is null");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error validating WebView", e);
            return false;
        }
    }

    /**
     * 启动清理服务
     */
    private void startCleanupService() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isCleanupRunning) {
                    performCleanup();
                }
                mainHandler.postDelayed(this, CLEANUP_INTERVAL);
            }
        }, CLEANUP_INTERVAL);
    }

    /**
     * 执行清理操作
     */
    private void performCleanup() {
        isCleanupRunning = true;

        try {
            Log.d(TAG, "Performing WebView pool cleanup");

            // 清理超时的WebView
            long currentTime = System.currentTimeMillis();
            // 这里可以添加超时检测逻辑

            // 清理无效的WebView
            availableWebViews.removeIf(webView -> !isWebViewValid(webView));

            // 记录清理结果
            Log.d(TAG, "Cleanup completed - available: " + availableWebViews.size() +
                      ", in use: " + inUseWebViews.size() +
                      ", total: " + allWebViews.size());

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        } finally {
            isCleanupRunning = false;
        }
    }

    /**
     * 清理所有资源
     */
    public void cleanup() {
        try {
            Log.i(TAG, "Cleaning up WebView pool...");

            // 销毁所有WebView实例
            for (WebView webView : allWebViews) {
                destroyWebView(webView);
            }

            // 清空集合
            availableWebViews.clear();
            inUseWebViews.clear();
            allWebViews.clear();

            Log.i(TAG, "WebView pool cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error during pool cleanup", e);
        }
    }

    /**
     * 获取池状态信息
     */
    public PoolStats getPoolStats() {
        return new PoolStats(
            availableWebViews.size(),
            inUseWebViews.size(),
            allWebViews.size(),
            totalCreated,
            totalDestroyed,
            totalAcquired,
            totalReleased
        );
    }

    /**
     * 预热连接池
     */
    public void warmUp() {
        mainHandler.post(() -> {
            int needed = MAX_POOL_SIZE - availableWebViews.size();
            for (int i = 0; i < needed; i++) {
                WebView webView = createWebView();
                if (webView != null) {
                    availableWebViews.offer(webView);
                    allWebViews.add(webView);
                }
            }
            Log.d(TAG, "Pool warmed up, available: " + availableWebViews.size());
        });
    }

    /**
     * 连接池统计信息类
     */
    public static class PoolStats {
        public final int availableCount;
        public final int inUseCount;
        public final int totalCount;
        public final long totalCreated;
        public final long totalDestroyed;
        public final long totalAcquired;
        public final long totalReleased;

        public PoolStats(int availableCount, int inUseCount, int totalCount,
                        long totalCreated, long totalDestroyed,
                        long totalAcquired, long totalReleased) {
            this.availableCount = availableCount;
            this.inUseCount = inUseCount;
            this.totalCount = totalCount;
            this.totalCreated = totalCreated;
            this.totalDestroyed = totalDestroyed;
            this.totalAcquired = totalAcquired;
            this.totalReleased = totalReleased;
        }

        @Override
        public String toString() {
            return String.format("PoolStats{available=%d, inUse=%d, total=%d, created=%d, destroyed=%d, acquired=%d, released=%d}",
                    availableCount, inUseCount, totalCount, totalCreated, totalDestroyed, totalAcquired, totalReleased);
        }
    }

    /**
     * WebView优化器内部类
     */
    private static class WebViewOptimizer {

        public void optimizeWebView(WebView webView) {
            try {
                // 启用硬件加速（根据需要）
                webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

                // 优化内存使用
                WebSettings settings = webView.getSettings();
                settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

                // 启用数据库存储
                settings.setDatabaseEnabled(true);
                settings.setDomStorageEnabled(true);

                        // 缓存大小设置已移除，使用现代缓存机制

                Log.d(TAG, "WebView optimized successfully");

            } catch (Exception e) {
                Log.e(TAG, "Failed to optimize WebView", e);
            }
        }
    }
}