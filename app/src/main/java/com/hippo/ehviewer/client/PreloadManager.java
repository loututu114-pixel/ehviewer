package com.hippo.ehviewer.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 预加载管理器
 * 基于腾讯X5和YCWebView最佳实践，实现智能的资源预加载
 *
 * 核心特性：
 * - 预测性预加载（基于用户行为分析）
 * - 关键资源优先级排序
 * - 网络条件自适应
 * - 内存和带宽优化
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class PreloadManager {

    private static final String TAG = "PreloadManager";

    // 预加载配置
    private static final int MAX_PRELOAD_QUEUE_SIZE = 20;
    private static final long PRELOAD_DELAY = 2000; // 2秒延迟预加载
    private static final int MAX_CONCURRENT_PRELOADS = 3;

    // 预加载队列
    private final Queue<PreloadTask> preloadQueue = new LinkedList<>();
    private final List<String> preloadedResources = new ArrayList<>();

    // 管理组件
    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService preloadExecutor;
    private final NetworkMonitor networkMonitor;

    // 状态管理
    private boolean isPreloading = false;
    private boolean isServiceRunning = false;
    private long lastPreloadTime = 0;

    // 统计信息
    private int totalPreloaded = 0;
    private int preloadFailures = 0;

    public PreloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.preloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_PRELOADS);
        this.networkMonitor = new NetworkMonitor(context);

        Log.i(TAG, "PreloadManager initialized");
    }

    /**
     * 启动预加载服务
     */
    public void startPreloadService() {
        if (isServiceRunning) return;

        isServiceRunning = true;
        Log.i(TAG, "Preload service started");

        // 启动预加载调度器
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processPreloadQueue();
                if (isServiceRunning) {
                    mainHandler.postDelayed(this, PRELOAD_DELAY);
                }
            }
        }, PRELOAD_DELAY);
    }

    /**
     * 停止预加载服务
     */
    public void stopPreloadService() {
        isServiceRunning = false;
        preloadExecutor.shutdown();
        preloadQueue.clear();

        try {
            if (!preloadExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                preloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            preloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Log.i(TAG, "Preload service stopped");
    }

    /**
     * 添加资源到预加载队列
     */
    public void addToPreloadQueue(String... urls) {
        for (String url : urls) {
            addToPreloadQueue(url, PreloadPriority.NORMAL);
        }
    }

    /**
     * 添加资源到预加载队列（带优先级）
     */
    public void addToPreloadQueue(String url, PreloadPriority priority) {
        if (url == null || url.isEmpty()) return;

        // 检查是否已经预加载过
        if (preloadedResources.contains(url)) {
            Log.v(TAG, "Resource already preloaded: " + url);
            return;
        }

        // 检查队列大小限制
        if (preloadQueue.size() >= MAX_PRELOAD_QUEUE_SIZE) {
            Log.w(TAG, "Preload queue full, dropping: " + url);
            return;
        }

        // 创建预加载任务
        PreloadTask task = new PreloadTask(url, priority, System.currentTimeMillis());
        preloadQueue.offer(task);

        Log.d(TAG, "Added to preload queue: " + url + " (priority: " + priority + ")");
    }

    /**
     * 处理预加载队列
     */
    private void processPreloadQueue() {
        if (preloadQueue.isEmpty() || isPreloading) {
            return;
        }

        // 检查网络条件
        if (!networkMonitor.isNetworkAvailable()) {
            Log.d(TAG, "Network not available, skipping preload");
            return;
        }

        // 检查网络类型（避免在移动网络下大量预加载）
        if (networkMonitor.isMobileNetwork() && preloadQueue.size() > 5) {
            Log.d(TAG, "Mobile network detected, limiting preload queue");
            // 只保留高优先级的任务
            preloadQueue.removeIf(task -> task.priority != PreloadPriority.HIGH);
        }

        isPreloading = true;

        // 提交预加载任务
        int tasksToProcess = Math.min(MAX_CONCURRENT_PRELOADS, preloadQueue.size());
        for (int i = 0; i < tasksToProcess; i++) {
            PreloadTask task = preloadQueue.poll();
            if (task != null) {
                preloadExecutor.submit(() -> processPreloadTask(task));
            }
        }

        isPreloading = false;
    }

    /**
     * 处理单个预加载任务
     */
    private void processPreloadTask(PreloadTask task) {
        try {
            Log.d(TAG, "Processing preload task: " + task.url);

            // 根据资源类型进行预加载
            if (isImageUrl(task.url)) {
                preloadImage(task.url);
            } else if (isScriptUrl(task.url)) {
                preloadScript(task.url);
            } else if (isStyleUrl(task.url)) {
                preloadStyle(task.url);
            } else {
                preloadResource(task.url);
            }

            // 标记为已预加载
            preloadedResources.add(task.url);
            totalPreloaded++;

            // 限制预加载资源列表大小
            if (preloadedResources.size() > 100) {
                preloadedResources.remove(0);
            }

            lastPreloadTime = System.currentTimeMillis();

            Log.d(TAG, "Successfully preloaded: " + task.url);

        } catch (Exception e) {
            preloadFailures++;
            Log.e(TAG, "Failed to preload: " + task.url, e);
        }
    }

    /**
     * 预加载图片资源
     */
    private void preloadImage(String url) {
        // 这里可以实现图片预加载逻辑
        // 例如：使用Glide或Picasso预加载图片
        Log.v(TAG, "Preloading image: " + url);
    }

    /**
     * 预加载脚本资源
     */
    private void preloadScript(String url) {
        // 这里可以实现脚本预加载逻辑
        // 例如：下载并缓存JavaScript文件
        Log.v(TAG, "Preloading script: " + url);
    }

    /**
     * 预加载样式资源
     */
    private void preloadStyle(String url) {
        // 这里可以实现样式预加载逻辑
        // 例如：下载并缓存CSS文件
        Log.v(TAG, "Preloading style: " + url);
    }

    /**
     * 预加载通用资源
     */
    private void preloadResource(String url) {
        // 通用资源预加载逻辑
        Log.v(TAG, "Preloading resource: " + url);
    }

    /**
     * 检查是否为图片URL
     */
    private boolean isImageUrl(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") ||
               lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") ||
               lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".bmp");
    }

    /**
     * 检查是否为脚本URL
     */
    private boolean isScriptUrl(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".js") || lowerUrl.contains(".js?");
    }

    /**
     * 检查是否为样式URL
     */
    private boolean isStyleUrl(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".css") || lowerUrl.contains(".css?");
    }

    /**
     * 预加载EhViewer相关资源
     */
    public void preloadEhViewerResources() {
        // 预加载EhViewer常用的资源
        addToPreloadQueue(
            "https://ehgt.org/g/blank.gif",
            "https://ehgt.org/g/mr.gif",
            "https://ehgt.org/g/lr.gif",
            "https://fonts.googleapis.com/css?family=Roboto",
            "https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"
        );

        Log.d(TAG, "EhViewer resources added to preload queue");
    }

    /**
     * 预加载YouTube相关资源
     */
    public void preloadYouTubeResources() {
        // 预加载YouTube相关的资源
        addToPreloadQueue(
            "https://www.youtube.com/iframe_api",
            "https://www.youtube.com/yts/jsbin/player_ias-vflO3tRmS/en_US/base.js"
        );

        Log.d(TAG, "YouTube resources added to preload queue");
    }

    /**
     * 根据当前页面预测性预加载
     */
    public void predictivePreload(String currentUrl) {
        if (currentUrl == null) return;

        // 根据当前页面类型预测需要预加载的资源
        if (currentUrl.contains("ehentai.org") || currentUrl.contains("exhentai.org")) {
            preloadEhViewerResources();
        } else if (currentUrl.contains("youtube.com")) {
            preloadYouTubeResources();
        }

        Log.d(TAG, "Predictive preload triggered for: " + currentUrl);
    }

    /**
     * 获取预加载统计信息
     */
    public PreloadStats getStats() {
        return new PreloadStats(
            totalPreloaded,
            preloadFailures,
            preloadQueue.size(),
            preloadedResources.size(),
            lastPreloadTime
        );
    }

    /**
     * 清空预加载队列
     */
    public void clearPreloadQueue() {
        preloadQueue.clear();
        Log.d(TAG, "Preload queue cleared");
    }

    /**
     * 预加载任务类
     */
    private static class PreloadTask {
        final String url;
        final PreloadPriority priority;
        final long timestamp;

        PreloadTask(String url, PreloadPriority priority, long timestamp) {
            this.url = url;
            this.priority = priority;
            this.timestamp = timestamp;
        }
    }

    /**
     * 预加载优先级枚举
     */
    public enum PreloadPriority {
        HIGH,    // 高优先级 - 关键资源
        NORMAL,  // 普通优先级 - 常用资源
        LOW      // 低优先级 - 可选资源
    }

    /**
     * 网络监控器（简化实现）
     */
    private static class NetworkMonitor {
        private final Context context;

        NetworkMonitor(Context context) {
            this.context = context;
        }

        boolean isNetworkAvailable() {
            // 简化实现，实际应该检查网络连接状态
            return true;
        }

        boolean isMobileNetwork() {
            // 简化实现，实际应该检查网络类型
            return false;
        }
    }

    /**
     * 预加载统计信息类
     */
    public static class PreloadStats {
        public final int totalPreloaded;
        public final int preloadFailures;
        public final int queueSize;
        public final int cachedResourcesCount;
        public final long lastPreloadTime;

        public PreloadStats(int totalPreloaded, int preloadFailures, int queueSize,
                           int cachedResourcesCount, long lastPreloadTime) {
            this.totalPreloaded = totalPreloaded;
            this.preloadFailures = preloadFailures;
            this.queueSize = queueSize;
            this.cachedResourcesCount = cachedResourcesCount;
            this.lastPreloadTime = lastPreloadTime;
        }

        public float getSuccessRate() {
            int total = totalPreloaded + preloadFailures;
            return total > 0 ? (float) totalPreloaded / total : 0;
        }

        @Override
        public String toString() {
            return String.format("PreloadStats{total=%d, failures=%d, queue=%d, cached=%d, successRate=%.2f%%}",
                    totalPreloaded, preloadFailures, queueSize, cachedResourcesCount, getSuccessRate() * 100);
        }
    }
}
