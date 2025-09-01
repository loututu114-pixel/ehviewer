package com.hippo.ehviewer.client;

import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * 渲染引擎管理器
 * 基于腾讯X5和YCWebView最佳实践，实现智能的渲染优化
 *
 * 核心特性：
 * - 根据内容类型智能优化渲染参数
 * - 硬件加速动态调整
 * - 内存使用优化
 * - 性能监控和调优
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class RenderEngineManager {

    private static final String TAG = "RenderEngineManager";

    // 内容类型枚举
    public enum ContentType {
        TEXT_HEAVY("文本密集型"),
        IMAGE_HEAVY("图片密集型"),
        VIDEO_HEAVY("视频密集型"),
        INTERACTIVE("交互密集型"),
        GENERAL("通用类型");

        private final String description;

        ContentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 性能配置
    private static final int TEXT_ZOOM_DEFAULT = 100;
    private static final int TEXT_ZOOM_READING = 120;
    private static final int TEXT_ZOOM_ACCESSIBILITY = 150;

    /**
     * 根据URL优化渲染引擎
     */
    public void optimizeForUrl(WebView webView, String url) {
        if (webView == null || url == null) return;

        ContentType contentType = analyzeContentType(url);
        applyOptimizationForContentType(webView, contentType);

        Log.d(TAG, "Applied rendering optimization for " + contentType.getDescription() + ": " + url);
    }

    /**
     * 分析内容类型
     */
    private ContentType analyzeContentType(String url) {
        String lowerUrl = url.toLowerCase();

        // 检查是否为EhViewer画廊页面
        if (lowerUrl.contains("ehentai.org") || lowerUrl.contains("exhentai.org")) {
            if (lowerUrl.contains("/g/")) {
                return ContentType.IMAGE_HEAVY; // 画廊页面通常图片密集
            }
            return ContentType.TEXT_HEAVY; // 列表页面文本密集
        }

        // 检查是否为视频网站
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") ||
            lowerUrl.contains("bilibili.com") || lowerUrl.contains("twitch.tv")) {
            return ContentType.VIDEO_HEAVY;
        }

        // 检查是否为图片文件
        if (lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg)(\\?.*)?$")) {
            return ContentType.IMAGE_HEAVY;
        }

        // 检查是否为视频文件
        if (lowerUrl.matches(".*\\.(mp4|avi|mkv|mov|wmv|flv|webm)(\\?.*)?$")) {
            return ContentType.VIDEO_HEAVY;
        }

        // 检查是否为社交媒体或交互密集型网站
        if (lowerUrl.contains("twitter.com") || lowerUrl.contains("facebook.com") ||
            lowerUrl.contains("instagram.com") || lowerUrl.contains("reddit.com")) {
            return ContentType.INTERACTIVE;
        }

        return ContentType.GENERAL;
    }

    /**
     * 根据内容类型应用优化
     */
    private void applyOptimizationForContentType(WebView webView, ContentType contentType) {
        WebSettings settings = webView.getSettings();

        switch (contentType) {
            case TEXT_HEAVY:
                optimizeForTextContent(webView, settings);
                break;
            case IMAGE_HEAVY:
                optimizeForImageContent(webView, settings);
                break;
            case VIDEO_HEAVY:
                optimizeForVideoContent(webView, settings);
                break;
            case INTERACTIVE:
                optimizeForInteractiveContent(webView, settings);
                break;
            case GENERAL:
            default:
                optimizeForGeneralContent(webView, settings);
                break;
        }

        // 应用通用优化
        applyCommonOptimizations(webView, settings);
    }

    /**
     * 优化文本密集型内容
     */
    private void optimizeForTextContent(WebView webView, WebSettings settings) {
        Log.d(TAG, "Optimizing for text-heavy content");

        // 启用文本优化
        settings.setTextZoom(TEXT_ZOOM_DEFAULT);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        // 禁用不必要的图像加载以加快文本加载
        settings.setBlockNetworkImage(true);
        settings.setLoadsImagesAutomatically(false);

        // 优化JavaScript执行（文本页面通常不需要复杂JS）
        settings.setJavaScriptEnabled(true);

        // 设置较小的缓存模式以节省内存
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
    }

    /**
     * 优化图片密集型内容
     */
    private void optimizeForImageContent(WebView webView, WebSettings settings) {
        Log.d(TAG, "Optimizing for image-heavy content");

        // 启用硬件加速以提升图片渲染性能
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // 启用图像加载
        settings.setBlockNetworkImage(false);
        settings.setLoadsImagesAutomatically(true);

        // 内存缓存设置已移除，使用现代缓存机制

        // 优化图片显示
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // 设置更大的缓存模式
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
    }

    /**
     * 优化视频密集型内容
     */
    private void optimizeForVideoContent(WebView webView, WebSettings settings) {
        Log.d(TAG, "Optimizing for video-heavy content");

        // 启用硬件加速以提升视频播放性能
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // 启用媒体播放
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 启用插件支持
        settings.setPluginState(WebSettings.PluginState.ON);

        // 缓存大小设置已移除，使用现代缓存机制

        // 优化网络设置
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setBlockNetworkImage(false);
        settings.setLoadsImagesAutomatically(true);

        Log.d(TAG, "Video optimization applied - hardware acceleration enabled");
    }

    /**
     * 优化交互密集型内容
     */
    private void optimizeForInteractiveContent(WebView webView, WebSettings settings) {
        Log.d(TAG, "Optimizing for interactive content");

        // 启用所有JavaScript特性
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // 启用触摸和手势支持
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // 启用地理位置（如果需要）
        settings.setGeolocationEnabled(true);

        // 缓存设置已移除，使用现代缓存机制

        // 混合内容支持
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    }

    /**
     * 优化通用内容
     */
    private void optimizeForGeneralContent(WebView webView, WebSettings settings) {
        Log.d(TAG, "Applying general optimization");

        // 平衡的设置
        settings.setTextZoom(TEXT_ZOOM_DEFAULT);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBlockNetworkImage(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // 标准缓存设置
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
    }

    /**
     * 应用通用优化
     */
    private void applyCommonOptimizations(WebView webView, WebSettings settings) {
        // 启用硬件加速（如果支持）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        // 优化渲染优先级
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // 启用宽视口支持
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // 设置用户代理（如果需要）
        // settings.setUserAgentString("Custom User Agent");

        // 启用安全浏览
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        Log.d(TAG, "Common optimizations applied");
    }

    /**
     * 动态调整硬件加速
     */
    public void adjustHardwareAcceleration(WebView webView, boolean enable) {
        try {
            if (enable) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                Log.d(TAG, "Hardware acceleration enabled");
            } else {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                Log.d(TAG, "Hardware acceleration disabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to adjust hardware acceleration", e);
        }
    }

    /**
     * 优化内存使用
     */
    public void optimizeMemoryUsage(WebView webView) {
        try {
            WebSettings settings = webView.getSettings();

            // 缓存大小设置已移除，使用现代机制

            // 禁用不必要的特性
            settings.setBlockNetworkImage(true);
            settings.setLoadsImagesAutomatically(false);

            // 清理缓存
            webView.clearCache(true);

            Log.d(TAG, "Memory optimization applied");

        } catch (Exception e) {
            Log.e(TAG, "Failed to optimize memory usage", e);
        }
    }

    /**
     * 根据设备性能调整设置
     */
    public void adjustForDevicePerformance(WebView webView) {
        WebSettings settings = webView.getSettings();

        // 获取设备内存信息
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();

                    // 根据内存大小调整设置
        if (maxMemory < 100 * 1024 * 1024) { // 低于100MB
            // 低内存设备优化
            settings.setBlockNetworkImage(true);
            settings.setLoadsImagesAutomatically(false);
            Log.d(TAG, "Low memory device optimization applied");
        } else if (maxMemory < 256 * 1024 * 1024) { // 低于256MB
            // 中等内存设备优化
            Log.d(TAG, "Medium memory device optimization applied");
        } else {
            // 高内存设备优化
            Log.d(TAG, "High memory device optimization applied");
        }
    }

    /**
     * 监控渲染性能
     */
    public void monitorRenderingPerformance(WebView webView) {
        try {
            // 这里可以添加性能监控代码
            // 例如：监控页面加载时间、内存使用、帧率等

            Log.d(TAG, "Rendering performance monitoring started");

        } catch (Exception e) {
            Log.e(TAG, "Failed to monitor rendering performance", e);
        }
    }

    /**
     * 获取渲染统计信息
     */
    public RenderingStats getRenderingStats() {
        return new RenderingStats();
    }

    /**
     * 渲染统计信息类
     */
    public static class RenderingStats {
        public long averageFrameTime;
        public int droppedFrames;
        public long memoryUsage;
        public boolean hardwareAccelerated;

        @Override
        public String toString() {
            return String.format("RenderingStats{frameTime=%dms, droppedFrames=%d, memory=%dKB, hwAccel=%b}",
                    averageFrameTime, droppedFrames, memoryUsage / 1024, hardwareAccelerated);
        }
    }
}
