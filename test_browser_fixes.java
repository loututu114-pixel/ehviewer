package com.hippo.ehviewer.test;

import android.content.Context;
import android.util.Log;
import com.hippo.ehviewer.client.X5WebViewManager;
import com.hippo.ehviewer.performance.WebViewMemoryManager;
import com.hippo.ehviewer.util.SystemErrorHandler;

/**
 * 浏览器修复验证测试类
 * 测试Unix域套接字错误和tile memory limits exceeded的修复效果
 */
public class BrowserFixesTest {

    private static final String TAG = "BrowserFixesTest";
    private Context context;
    private SystemErrorHandler errorHandler;

    public BrowserFixesTest(Context context) {
        this.context = context;
        this.errorHandler = SystemErrorHandler.getInstance(context);
    }

    /**
     * 运行所有修复验证测试
     */
    public void runAllTests() {
        Log.i(TAG, "=== 开始浏览器修复验证测试 ===");

        testX5WebViewInitialization();
        testTileMemoryManagement();
        testErrorHandlerIntegration();
        testSystemProperties();

        Log.i(TAG, "=== 浏览器修复验证测试完成 ===");
    }

    /**
     * 测试X5 WebView初始化修复
     */
    private void testX5WebViewInitialization() {
        Log.i(TAG, "测试X5 WebView初始化修复...");

        try {
            X5WebViewManager x5Manager = X5WebViewManager.getInstance();

            // 测试X5初始化
            x5Manager.initX5(context);

            // 验证初始化参数
            boolean isX5Available = x5Manager.isX5Available();
            String x5Version = x5Manager.getX5Version();

            Log.i(TAG, "X5可用: " + isX5Available);
            Log.i(TAG, "X5版本: " + x5Version);

            // 测试WebView创建
            Object webView = x5Manager.createWebView(context);
            if (webView != null) {
                Log.i(TAG, "✅ WebView创建成功");
            } else {
                Log.e(TAG, "❌ WebView创建失败");
            }

        } catch (Exception e) {
            Log.e(TAG, "X5 WebView测试失败", e);
        }
    }

    /**
     * 测试瓦片内存管理修复
     */
    private void testTileMemoryManagement() {
        Log.i(TAG, "测试瓦片内存管理修复...");

        try {
            WebViewMemoryManager memoryManager = WebViewMemoryManager.getInstance(context);

            // 测试内存监控
            WebViewMemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
            Log.i(TAG, "内存状态: " + stats.toString());

            // 测试瓦片内存压力检查
            memoryManager.checkTileMemoryPressure();

            // 验证系统属性设置
            String tileCacheSize = System.getProperty("webview.tile_cache_size");
            String maxThreads = System.getProperty("webview.max_rendering_threads");

            Log.i(TAG, "瓦片缓存大小: " + tileCacheSize);
            Log.i(TAG, "最大渲染线程: " + maxThreads);

            Log.i(TAG, "✅ 瓦片内存管理测试完成");

        } catch (Exception e) {
            Log.e(TAG, "瓦片内存管理测试失败", e);
        }
    }

    /**
     * 测试错误处理器集成
     */
    private void testErrorHandlerIntegration() {
        Log.i(TAG, "测试错误处理器集成...");

        try {
            // 模拟各种错误消息
            String[] testMessages = {
                "failed to create Unix domain socket: Operation not permitted",
                "WARNING: tile memory limits exceeded, some content may not draw",
                "chromium: [ERROR:cc/tiles/tile_manager.cc:999] WARNING: tile memory limits exceeded",
                "com.heytap.browser: failed to create Unix domain socket: Operation not permitted"
            };

            for (String message : testMessages) {
                Log.i(TAG, "测试错误消息: " + message);
                errorHandler.handleSystemError(message);
            }

            // 获取错误统计
            String errorStats = errorHandler.getSystemInfo();
            Log.i(TAG, "错误统计:\n" + errorStats);

            Log.i(TAG, "✅ 错误处理器集成测试完成");

        } catch (Exception e) {
            Log.e(TAG, "错误处理器集成测试失败", e);
        }
    }

    /**
     * 测试系统属性设置
     */
    private void testSystemProperties() {
        Log.i(TAG, "测试系统属性设置...");

        try {
            // 检查关键的系统属性
            String[] properties = {
                "webview.enable_threaded_rendering",
                "webview.enable_surface_control",
                "webview.tile_cache_size",
                "webview.max_rendering_threads",
                "webview.enable_low_end_mode"
            };

            for (String prop : properties) {
                String value = System.getProperty(prop);
                Log.i(TAG, "属性 " + prop + " = " + value);
            }

            Log.i(TAG, "✅ 系统属性测试完成");

        } catch (Exception e) {
            Log.e(TAG, "系统属性测试失败", e);
        }
    }

    /**
     * 生成测试报告
     */
    public String generateTestReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 浏览器修复测试报告 ===\n");
        report.append("测试时间: ").append(System.currentTimeMillis()).append("\n\n");

        // X5 WebView状态
        X5WebViewManager x5Manager = X5WebViewManager.getInstance();
        report.append("X5 WebView状态:\n");
        report.append("  - 已初始化: ").append(x5Manager.isX5Initialized()).append("\n");
        report.append("  - 可用: ").append(x5Manager.isX5Available()).append("\n");
        report.append("  - 版本: ").append(x5Manager.getX5Version()).append("\n\n");

        // 内存状态
        WebViewMemoryManager memoryManager = WebViewMemoryManager.getInstance(context);
        WebViewMemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        report.append("内存状态:\n");
        report.append("  ").append(stats.toString()).append("\n\n");

        // 系统属性
        report.append("关键系统属性:\n");
        String[] keyProps = {
            "webview.tile_cache_size",
            "webview.max_rendering_threads",
            "webview.enable_threaded_rendering"
        };

        for (String prop : keyProps) {
            String value = System.getProperty(prop);
            report.append("  ").append(prop).append(" = ").append(value).append("\n");
        }

        report.append("\n=== 测试完成 ===");
        return report.toString();
    }

    /**
     * 验证修复是否生效
     */
    public boolean validateFixes() {
        boolean allGood = true;

        try {
            // 检查X5是否正常初始化
            X5WebViewManager x5Manager = X5WebViewManager.getInstance();
            if (!x5Manager.isX5Initialized()) {
                Log.w(TAG, "❌ X5未正确初始化");
                allGood = false;
            } else {
                Log.i(TAG, "✅ X5初始化正常");
            }

            // 检查内存管理器是否工作
            WebViewMemoryManager memoryManager = WebViewMemoryManager.getInstance(context);
            WebViewMemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
            if (stats.getUsagePercent() > 90) {
                Log.w(TAG, "❌ 内存使用率过高: " + stats.getUsagePercent() + "%");
                allGood = false;
            } else {
                Log.i(TAG, "✅ 内存使用正常: " + stats.getUsagePercent() + "%");
            }

            // 检查关键系统属性
            String tileCacheSize = System.getProperty("webview.tile_cache_size");
            if (tileCacheSize != null) {
                Log.i(TAG, "✅ 瓦片缓存大小已设置: " + tileCacheSize);
            } else {
                Log.w(TAG, "❌ 瓦片缓存大小未设置");
                allGood = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "验证修复时发生错误", e);
            allGood = false;
        }

        return allGood;
    }

    /**
     * 主测试方法
     */
    public static void runBrowserFixesTest(Context context) {
        BrowserFixesTest test = new BrowserFixesTest(context);

        // 运行所有测试
        test.runAllTests();

        // 验证修复效果
        boolean fixesValid = test.validateFixes();

        // 生成报告
        String report = test.generateTestReport();

        Log.i(TAG, "修复验证结果: " + (fixesValid ? "✅ 通过" : "❌ 失败"));
        Log.i(TAG, "测试报告:\n" + report);
    }
}
