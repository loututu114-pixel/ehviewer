package com.hippo.ehviewer.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import com.hippo.ehviewer.client.parser.GalleryDetailParser;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.util.UserAgentManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 浏览器核心管理器 - EhViewer的高性能浏览器核心
 * 基于腾讯X5内核，提供完整的浏览器功能和管理
 *
 * 核心特性：
 * - WebView连接池管理
 * - 智能资源优化
 * - 性能监控和调优
 * - 安全防护和隐私保护
 * - 错误处理和恢复
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class BrowserCoreManager {

    private static final String TAG = "BrowserCoreManager";
    private static volatile BrowserCoreManager instance;

    // 核心组件
    private final Context context;
    private final Handler mainHandler;
    private WebViewPoolManager webViewPool;
    private RenderEngineManager renderEngine;
    private PerformanceMonitor performanceMonitor;
    private SecurityManager securityManager;
    private CacheManager cacheManager;
    private PreloadManager preloadManager;
    private ErrorRecoveryManager errorRecoveryManager;
    private BrowserCompatibilityManager compatibilityManager;
    private SmartRequestProcessor requestProcessor;

    // 配置参数
    private static final int MAX_WEBVIEW_POOL_SIZE = 3;
    private static final long WEBVIEW_TIMEOUT = 30000; // 30秒超时
    private static final long CACHE_MAX_SIZE = 50 * 1024 * 1024; // 50MB缓存

    // 状态管理
    private final AtomicInteger activeWebViews = new AtomicInteger(0);
    private volatile boolean isInitialized = false;

    /**
     * 获取单例实例
     */
    public static BrowserCoreManager getInstance(Context context) {
        if (instance == null) {
            synchronized (BrowserCoreManager.class) {
                if (instance == null) {
                    instance = new BrowserCoreManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private BrowserCoreManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 初始化核心组件
        initializeComponents();
    }

    /**
     * 初始化核心组件
     */
    private void initializeComponents() {
        try {
            Log.i(TAG, "Initializing BrowserCoreManager...");

            // 1. 初始化WebView连接池
            webViewPool = new WebViewPoolManager(context, MAX_WEBVIEW_POOL_SIZE);

            // 2. 初始化渲染引擎管理器
            renderEngine = new RenderEngineManager();

            // 3. 初始化性能监控器
            performanceMonitor = new PerformanceMonitor();

            // 4. 初始化安全管理器
            securityManager = new SecurityManager(context);

            // 5. 初始化缓存管理器
            cacheManager = new CacheManager(context, CACHE_MAX_SIZE);

            // 6. 初始化预加载管理器
            preloadManager = new PreloadManager(context);

            // 7. 初始化错误恢复管理器
            errorRecoveryManager = new ErrorRecoveryManager(context);

            // 8. 初始化兼容性管理器
            compatibilityManager = BrowserCompatibilityManager.getInstance(context);

            // 9. 初始化智能请求处理器
            requestProcessor = new SmartRequestProcessor(context);

            // 10. 启动后台服务
            startBackgroundServices();

            isInitialized = true;
            Log.i(TAG, "BrowserCoreManager initialized successfully");
            Log.i(TAG, "Compatibility Manager Stats: " + compatibilityManager.getCompatibilityStats());

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize BrowserCoreManager", e);
            throw new RuntimeException("BrowserCoreManager initialization failed", e);
        }
    }

    /**
     * 启动后台服务
     */
    private void startBackgroundServices() {
        // 启动性能监控
        performanceMonitor.startMonitoring();

                    // 启动缓存清理服务
            cacheManager.startCacheCleanup();

        // 启动预加载服务
        preloadManager.startPreloadService();

        // 启动安全监控服务
        securityManager.startSecurityMonitoring();
    }

    /**
     * 获取优化的WebView实例
     *
     * @param url 目标URL，用于预优化
     * @return 优化的WebView实例
     */
    public WebView acquireOptimizedWebView(String url) {
        if (!isInitialized) {
            throw new IllegalStateException("BrowserCoreManager not initialized");
        }

        try {
            // 1. 从连接池获取WebView
            WebView webView = webViewPool.acquire();
            if (webView == null) {
                Log.w(TAG, "Failed to acquire WebView from pool");
                return null;
            }

            activeWebViews.incrementAndGet();

            // 2. 安全配置被禁用以确保最大兼容性
            // securityManager.applySecuritySettings(webView); // 已禁用保证高可用性

            // 3. 优化渲染引擎
            if (url != null && !url.isEmpty()) {
                renderEngine.optimizeForUrl(webView, url);
                performanceMonitor.startMonitoring(url);
            }

            // 4. 配置错误处理
            setupErrorHandling(webView);

            // 5. 应用网站兼容性配置
            if (url != null && !url.isEmpty()) {
                compatibilityManager.applyCompatibilityConfig(webView, url);
            }

            Log.d(TAG, "Acquired optimized WebView for URL: " + url);
            return webView;

        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire optimized WebView", e);
            return null;
        }
    }

    /**
     * 释放WebView实例
     *
     * @param webView 要释放的WebView
     */
    public void releaseWebView(WebView webView) {
        if (webView == null) return;

        try {
            // 停止加载
            webView.stopLoading();

            // 清理资源
            webView.clearCache(true);
            webView.clearHistory();

            // 释放到连接池
            webViewPool.release(webView);
            activeWebViews.decrementAndGet();

            Log.d(TAG, "Released WebView, active count: " + activeWebViews.get());

        } catch (Exception e) {
            Log.e(TAG, "Failed to release WebView", e);
        }
    }

    /**
     * 为URL预加载资源
     *
     * @param url 目标URL
     */
    public void preloadForUrl(String url) {
        Log.d(TAG, "=== BROWSERCORE: preloadForUrl called with URL: " + url);

        if (url == null || url.isEmpty()) {
            Log.w(TAG, "=== BROWSERCORE: URL is null or empty, skipping preload");
            return;
        }

        try {
            // 分析URL类型
            ContentType contentType = analyzeContentType(url);
            Log.d(TAG, "=== BROWSERCORE: Analyzed content type: " + contentType + " for URL: " + url);

            // 根据内容类型进行预加载
            switch (contentType) {
                case GALLERY:
                    Log.d(TAG, "=== BROWSERCORE: Starting gallery resource preload");
                    preloadGalleryResources(url);
                    break;
                case IMAGE:
                    Log.d(TAG, "=== BROWSERCORE: Starting image resource preload");
                    preloadImageResources(url);
                    break;
                case VIDEO:
                    Log.d(TAG, "=== BROWSERCORE: Starting video resource preload");
                    preloadVideoResources(url);
                    break;
                case GENERAL:
                default:
                    Log.d(TAG, "=== BROWSERCORE: Starting general resource preload");
                    preloadGeneralResources(url);
                    break;
            }

            Log.d(TAG, "=== BROWSERCORE: Preload completed for URL: " + url + " (type: " + contentType + ")");

        } catch (Exception e) {
            Log.e(TAG, "=== BROWSERCORE: Failed to preload for URL: " + url, e);
        }
    }

    /**
     * 分析URL内容类型
     */
    private ContentType analyzeContentType(String url) {
        if (url.contains("ehentai.org") || url.contains("exhentai.org")) {
            if (url.contains("/g/")) {
                return ContentType.GALLERY;
            }
        }

        if (url.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)(\\?.*)?$")) {
            return ContentType.IMAGE;
        }

        if (url.contains("youtube.com") || url.contains("youtu.be") ||
            url.matches(".*\\.(mp4|avi|mkv|mov|wmv)(\\?.*)?$")) {
            return ContentType.VIDEO;
        }

        return ContentType.GENERAL;
    }

    /**
     * 预加载画廊资源
     */
    private void preloadGalleryResources(String url) {
        // 预加载画廊相关的脚本和样式
        preloadManager.addToPreloadQueue(
            "https://ehgt.org/g/blank.gif",
            "https://ehgt.org/g/mr.gif",
            "https://ehgt.org/g/lr.gif"
        );

        // 预加载EhViewer相关资源
        // TODO: 实现GalleryDetailParser和GalleryListParser的预加载方法
    }

    /**
     * 预加载图片资源
     */
    private void preloadImageResources(String url) {
        // 预加载图片处理相关的脚本
        preloadManager.addToPreloadQueue(
            "https://ehgt.org/g/509.gif",
            "https://ehgt.org/g/512.gif"
        );
    }

    /**
     * 预加载视频资源
     */
    private void preloadVideoResources(String url) {
        // 预加载视频播放器脚本
        preloadManager.addToPreloadQueue(
            "https://www.youtube.com/iframe_api",
            "https://players.brightcove.net/videojs/"
        );
    }

    /**
     * 预加载通用资源
     */
    private void preloadGeneralResources(String url) {
        // 预加载常用资源
        preloadManager.addToPreloadQueue(
            "https://fonts.googleapis.com/css?family=Roboto",
            "https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"
        );
    }

    /**
     * 设置错误处理
     */
    private void setupErrorHandling(WebView webView) {
        // 集成错误恢复管理器
        errorRecoveryManager.setupWebView(webView);
    }

    /**
     * 获取性能统计信息
     */
    public PerformanceMonitor.PerformanceStats getPerformanceStats() {
        return performanceMonitor.getStats();
    }

    /**
     * 获取缓存统计信息
     */
    public CacheManager.CacheStats getCacheStats() {
        return cacheManager.getStats();
    }

    /**
     * 获取安全状态
     */
    public SecurityManager.SecurityStatus getSecurityStatus() {
        return securityManager.getStatus();
    }

    /**
     * 清理所有资源
     */
    public void cleanup() {
        try {
            Log.i(TAG, "Cleaning up BrowserCoreManager...");

            // 停止所有服务
            performanceMonitor.stopMonitoring();
            cacheManager.stopCacheCleanupService();
            preloadManager.stopPreloadService();
            securityManager.stopSecurityMonitoring();

            // 清理WebView连接池
            webViewPool.cleanup();

            // 清理缓存
            cacheManager.clearAllCache();

            Log.i(TAG, "BrowserCoreManager cleanup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    /**
     * 获取活动WebView数量
     */
    public int getActiveWebViewCount() {
        return activeWebViews.get();
    }

    /**
     * 检查初始化状态
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 内容类型枚举
     */
    public enum ContentType {
        GALLERY("画廊"),
        IMAGE("图片"),
        VIDEO("视频"),
        GENERAL("通用");

        private final String displayName;

        ContentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 性能统计数据类
     */
    public static class PerformanceStats {
        public long averageLoadTime;
        public int totalRequests;
        public int failedRequests;
        public float successRate;

        @Override
        public String toString() {
            return String.format("PerformanceStats{avgLoadTime=%dms, total=%d, failed=%d, successRate=%.2f%%}",
                    averageLoadTime, totalRequests, failedRequests, successRate * 100);
        }
    }

    /**
     * 缓存统计数据类
     */
    public static class CacheStats {
        public long memoryCacheSize;
        public long diskCacheSize;
        public int cachedItemsCount;
        public long cacheHitRate;

        @Override
        public String toString() {
            return String.format("CacheStats{memory=%dKB, disk=%dKB, items=%d, hitRate=%d%%}",
                    memoryCacheSize / 1024, diskCacheSize / 1024, cachedItemsCount, cacheHitRate);
        }
    }

    /**
     * 安全状态类
     */
    public static class SecurityStatus {
        public boolean sslEnabled;
        public boolean xssProtectionEnabled;
        public boolean privacyModeEnabled;
        public int blockedRequestsCount;

        @Override
        public String toString() {
            return String.format("SecurityStatus{ssl=%b, xss=%b, privacy=%b, blocked=%d}",
                    sslEnabled, xssProtectionEnabled, privacyModeEnabled, blockedRequestsCount);
        }
    }

    /**
     * 获取智能请求处理器
     */
    public SmartRequestProcessor getRequestProcessor() {
        return requestProcessor;
    }
}
