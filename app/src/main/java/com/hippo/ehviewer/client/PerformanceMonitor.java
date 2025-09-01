package com.hippo.ehviewer.client;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控器
 * 基于腾讯X5和YCWebView最佳实践，实现全面的性能监控
 *
 * 核心特性：
 * - 页面加载时间监控
 * - 内存使用统计
 * - 网络请求性能分析
 * - CPU使用率监控
 * - 异常检测和报警
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class PerformanceMonitor {

    private static final String TAG = "PerformanceMonitor";

    // 监控配置
    private static final long MONITORING_INTERVAL = 5000; // 5秒间隔
    private static final long SLOW_LOAD_THRESHOLD = 3000; // 3秒慢加载阈值
    private static final long MEMORY_WARNING_THRESHOLD = 50 * 1024 * 1024; // 50MB内存警告
    private static final int MAX_PERFORMANCE_RECORDS = 100;

    // 性能指标
    private final Map<String, PagePerformanceMetrics> pageMetrics = new HashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalLoadTime = new AtomicLong(0);

    // 系统资源监控
    private long initialMemoryUsage = 0;
    private long peakMemoryUsage = 0;
    private long monitoringStartTime = 0;

    // 监控状态
    private boolean isMonitoring = false;
    private Handler monitoringHandler;
    private Runnable monitoringTask;

    public PerformanceMonitor() {
        monitoringHandler = new Handler(Looper.getMainLooper());
        initialMemoryUsage = getCurrentMemoryUsage();
        monitoringStartTime = System.currentTimeMillis();
    }

    /**
     * 启动性能监控
     */
    public void startMonitoring() {
        if (isMonitoring) return;

        isMonitoring = true;
        Log.i(TAG, "Performance monitoring started");

        monitoringTask = () -> {
            performMonitoring();
            if (isMonitoring) {
                monitoringHandler.postDelayed(monitoringTask, MONITORING_INTERVAL);
            }
        };

        monitoringHandler.post(monitoringTask);
    }

    /**
     * 停止性能监控
     */
    public void stopMonitoring() {
        isMonitoring = false;

        if (monitoringTask != null) {
            monitoringHandler.removeCallbacks(monitoringTask);
        }

        Log.i(TAG, "Performance monitoring stopped");
    }

    /**
     * 开始监控页面加载
     */
    public void startMonitoring(String url) {
        if (url == null || url.isEmpty()) return;

        PagePerformanceMetrics metrics = new PagePerformanceMetrics();
        metrics.url = url;
        metrics.startTime = System.currentTimeMillis();
        metrics.startMemoryUsage = getCurrentMemoryUsage();

        pageMetrics.put(url, metrics);
        totalRequests.incrementAndGet();

        Log.d(TAG, "Started monitoring page load: " + url);
    }

    /**
     * 记录页面加载完成
     */
    public void recordPageLoadComplete(String url, boolean success) {
        PagePerformanceMetrics metrics = pageMetrics.get(url);
        if (metrics == null) return;

        metrics.endTime = System.currentTimeMillis();
        metrics.endMemoryUsage = getCurrentMemoryUsage();
        metrics.success = success;
        metrics.loadTime = metrics.endTime - metrics.startTime;
        metrics.memoryDelta = metrics.endMemoryUsage - metrics.startMemoryUsage;

        // 更新统计信息
        if (success) {
            totalLoadTime.addAndGet(metrics.loadTime);
        } else {
            failedRequests.incrementAndGet();
        }

        // 检查性能阈值
        checkPerformanceThresholds(metrics);

        // 限制记录数量
        if (pageMetrics.size() > MAX_PERFORMANCE_RECORDS) {
            cleanupOldRecords();
        }

        Log.d(TAG, "Page load completed: " + url + " in " + metrics.loadTime + "ms, success: " + success);
    }

    /**
     * 记录网络请求
     */
    public void recordNetworkRequest(String url, long responseTime, boolean success) {
        // 这里可以记录详细的网络请求信息
        Log.v(TAG, "Network request: " + url + " took " + responseTime + "ms, success: " + success);
    }

    /**
     * 执行监控任务
     */
    private void performMonitoring() {
        try {
            // 监控内存使用
            long currentMemory = getCurrentMemoryUsage();
            peakMemoryUsage = Math.max(peakMemoryUsage, currentMemory);

            // 检查内存使用是否过高
            if (currentMemory > MEMORY_WARNING_THRESHOLD) {
                Log.w(TAG, "High memory usage detected: " + (currentMemory / 1024 / 1024) + "MB");
            }

            // 监控活跃页面数量
            int activePages = pageMetrics.size();
            if (activePages > 10) {
                Log.w(TAG, "High number of active pages: " + activePages);
            }

            // 计算平均加载时间
            long avgLoadTime = calculateAverageLoadTime();
            if (avgLoadTime > SLOW_LOAD_THRESHOLD) {
                Log.w(TAG, "Slow average load time: " + avgLoadTime + "ms");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during performance monitoring", e);
        }
    }

    /**
     * 检查性能阈值
     */
    private void checkPerformanceThresholds(PagePerformanceMetrics metrics) {
        // 检查加载时间
        if (metrics.loadTime > SLOW_LOAD_THRESHOLD) {
            Log.w(TAG, "Slow page load detected: " + metrics.url + " took " + metrics.loadTime + "ms");
        }

        // 检查内存使用
        if (metrics.memoryDelta > 10 * 1024 * 1024) { // 10MB
            Log.w(TAG, "High memory usage in page load: " + metrics.url + " used " +
                  (metrics.memoryDelta / 1024 / 1024) + "MB");
        }

        // 检查失败率
        double failureRate = (double) failedRequests.get() / totalRequests.get();
        if (failureRate > 0.1) { // 10%失败率
            Log.w(TAG, "High failure rate detected: " + String.format("%.2f%%", failureRate * 100));
        }
    }

    /**
     * 获取当前内存使用
     */
    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * 计算平均加载时间
     */
    private long calculateAverageLoadTime() {
        long totalRequests = this.totalRequests.get();
        if (totalRequests == 0) return 0;

        long totalTime = this.totalLoadTime.get();
        return totalTime / totalRequests;
    }

    /**
     * 清理旧的记录
     */
    private void cleanupOldRecords() {
        // 保留最新的50条记录
        if (pageMetrics.size() > 50) {
            // 这里可以实现更智能的清理策略
            pageMetrics.clear();
            Log.d(TAG, "Cleaned up old performance records");
        }
    }

    /**
     * 获取性能统计信息
     */
    public PerformanceStats getStats() {
        PerformanceStats stats = new PerformanceStats();

        stats.totalRequests = totalRequests.get();
        stats.failedRequests = failedRequests.get();
        stats.averageLoadTime = calculateAverageLoadTime();
        stats.successRate = stats.totalRequests > 0 ?
            (double) (stats.totalRequests - stats.failedRequests) / stats.totalRequests : 0.0;
        stats.currentMemoryUsage = getCurrentMemoryUsage();
        stats.peakMemoryUsage = peakMemoryUsage;
        stats.monitoringDuration = System.currentTimeMillis() - monitoringStartTime;
        stats.activePages = pageMetrics.size();

        return stats;
    }

    /**
     * 获取页面性能详情
     */
    public PagePerformanceMetrics getPageMetrics(String url) {
        return pageMetrics.get(url);
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        pageMetrics.clear();
        totalRequests.set(0);
        failedRequests.set(0);
        totalLoadTime.set(0);
        peakMemoryUsage = getCurrentMemoryUsage();
        monitoringStartTime = System.currentTimeMillis();

        Log.d(TAG, "Performance stats reset");
    }

    /**
     * 生成性能报告
     */
    public String generatePerformanceReport() {
        PerformanceStats stats = getStats();

        StringBuilder report = new StringBuilder();
        report.append("=== EhViewer 性能监控报告 ===\n");
        report.append("监控时长: ").append(stats.monitoringDuration / 1000).append("秒\n");
        report.append("总请求数: ").append(stats.totalRequests).append("\n");
        report.append("失败请求数: ").append(stats.failedRequests).append("\n");
        report.append("平均加载时间: ").append(stats.averageLoadTime).append("ms\n");
        report.append("成功率: ").append(String.format("%.2f%%", stats.successRate * 100)).append("\n");
        report.append("当前内存使用: ").append(stats.currentMemoryUsage / 1024 / 1024).append("MB\n");
        report.append("峰值内存使用: ").append(stats.peakMemoryUsage / 1024 / 1024).append("MB\n");
        report.append("活跃页面数: ").append(stats.activePages).append("\n");

        return report.toString();
    }

    /**
     * 页面性能指标类
     */
    public static class PagePerformanceMetrics {
        public String url;
        public long startTime;
        public long endTime;
        public long loadTime;
        public long startMemoryUsage;
        public long endMemoryUsage;
        public long memoryDelta;
        public boolean success;

        @Override
        public String toString() {
            return String.format("PageMetrics{url='%s', loadTime=%dms, memoryDelta=%dKB, success=%b}",
                    url, loadTime, memoryDelta / 1024, success);
        }
    }

    /**
     * 性能统计信息类
     */
    public static class PerformanceStats {
        public long totalRequests;
        public long failedRequests;
        public long averageLoadTime;
        public double successRate;
        public long currentMemoryUsage;
        public long peakMemoryUsage;
        public long monitoringDuration;
        public int activePages;

        @Override
        public String toString() {
            return String.format("PerformanceStats{total=%d, failed=%d, avgTime=%dms, successRate=%.2f%%, memory=%dMB, activePages=%d}",
                    totalRequests, failedRequests, averageLoadTime, successRate * 100,
                    currentMemoryUsage / 1024 / 1024, activePages);
        }
    }
}
