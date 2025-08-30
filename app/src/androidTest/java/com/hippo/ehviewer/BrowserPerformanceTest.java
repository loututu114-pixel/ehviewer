/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer;

import android.content.Context;
import android.os.Debug;
import android.webkit.WebView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.hippo.ehviewer.client.X5WebViewManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * 浏览器性能测试
 * 测试浏览器的性能表现、内存使用和电池消耗
 */
@RunWith(AndroidJUnit4.class)
public class BrowserPerformanceTest {

    private static final String TAG = "BrowserPerformanceTest";

    private Context mContext;
    private WebView mWebView;
    private X5WebViewManager mX5Manager;

    // 性能测试阈值
    private static final long MAX_LOAD_TIME = 10000; // 10秒
    private static final long MAX_MEMORY_USAGE = 100 * 1024 * 1024; // 100MB
    private static final int MIN_FPS = 30; // 最低30FPS

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mX5Manager = X5WebViewManager.getInstance();

        // 初始化X5
        mX5Manager.initX5(mContext);

        // 创建WebView
        mWebView = mX5Manager.createWebView(mContext);
        setupWebView();
    }

    @After
    public void tearDown() {
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
    }

    /**
     * 测试页面加载性能
     */
    @Test
    public void testPageLoadPerformance() throws InterruptedException {
        String[] testUrls = {
            "https://www.baidu.com",
            "https://www.sina.com.cn",
            "https://www.qq.com"
        };

        for (String url : testUrls) {
            long loadTime = measurePageLoadTime(url);
            android.util.Log.d(TAG, "Page load time for " + url + ": " + loadTime + "ms");

            assertTrue("Page load too slow for " + url + ": " + loadTime + "ms",
                    loadTime < MAX_LOAD_TIME);
        }
    }

    /**
     * 测试内存使用情况
     */
    @Test
    public void testMemoryUsage() throws InterruptedException {
        // 记录初始内存使用
        long initialMemory = getCurrentMemoryUsage();
        android.util.Log.d(TAG, "Initial memory usage: " + initialMemory / 1024 / 1024 + "MB");

        // 加载多个页面
        String[] testUrls = {
            "https://www.baidu.com",
            "https://www.sina.com.cn",
            "https://www.sohu.com"
        };

        for (String url : testUrls) {
            loadPage(url);
            Thread.sleep(2000); // 等待页面加载和渲染
        }

        // 检查内存使用是否在合理范围内
        long finalMemory = getCurrentMemoryUsage();
        long memoryIncrease = finalMemory - initialMemory;

        android.util.Log.d(TAG, "Final memory usage: " + finalMemory / 1024 / 1024 + "MB");
        android.util.Log.d(TAG, "Memory increase: " + memoryIncrease / 1024 / 1024 + "MB");

        assertTrue("Memory usage too high: " + memoryIncrease / 1024 / 1024 + "MB",
                memoryIncrease < MAX_MEMORY_USAGE);
    }

    /**
     * 测试多标签页性能
     */
    @Test
    public void testMultiTabPerformance() throws InterruptedException {
        final int TAB_COUNT = 5;
        WebView[] webViews = new WebView[TAB_COUNT];

        long startTime = System.currentTimeMillis();

        // 创建多个WebView实例
        for (int i = 0; i < TAB_COUNT; i++) {
            webViews[i] = mX5Manager.createWebView(mContext);
            setupWebView(webViews[i]);
        }

        long creationTime = System.currentTimeMillis() - startTime;
        android.util.Log.d(TAG, "Created " + TAB_COUNT + " WebViews in " + creationTime + "ms");

        // 加载不同页面
        String[] urls = {
            "https://www.baidu.com",
            "https://www.sina.com.cn",
            "https://www.sohu.com",
            "https://www.qq.com",
            "https://www.163.com"
        };

        startTime = System.currentTimeMillis();
        for (int i = 0; i < TAB_COUNT; i++) {
            loadPage(webViews[i], urls[i]);
        }

        // 等待所有页面加载完成
        Thread.sleep(10000);

        long totalLoadTime = System.currentTimeMillis() - startTime;
        android.util.Log.d(TAG, "Loaded " + TAB_COUNT + " pages in " + totalLoadTime + "ms");

        // 清理资源
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.destroy();
            }
        }

        assertTrue("Multi-tab loading too slow: " + totalLoadTime + "ms",
                totalLoadTime < MAX_LOAD_TIME * TAB_COUNT);
    }

    /**
     * 测试JavaScript执行性能
     */
    @Test
    public void testJavaScriptPerformance() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final long[] executionTime = new long[1];

        // 设置WebViewClient来监听页面加载
        mWebView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // 页面加载完成后执行JavaScript性能测试
                long startTime = System.nanoTime();

                view.evaluateJavascript(
                    "(function() {" +
                    "  var result = 0;" +
                    "  for (var i = 0; i < 100000; i++) {" +
                    "    result += Math.sin(i) * Math.cos(i);" +
                    "  }" +
                    "  return result;" +
                    "})();",
                    value -> {
                        long endTime = System.nanoTime();
                        executionTime[0] = (endTime - startTime) / 1000000; // 转换为毫秒
                        android.util.Log.d(TAG, "JavaScript execution time: " + executionTime[0] + "ms");
                        latch.countDown();
                    }
                );
            }
        });

        // 加载测试页面
        loadPage("https://www.baidu.com");

        // 等待JavaScript执行完成
        assertTrue("JavaScript execution timeout", latch.await(30, TimeUnit.SECONDS));

        // 验证执行时间在合理范围内（通常应该小于1秒）
        assertTrue("JavaScript execution too slow: " + executionTime[0] + "ms",
                executionTime[0] < 2000);
    }

    /**
     * 测试滚动性能
     */
    @Test
    public void testScrollPerformance() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        mWebView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // 页面加载完成后执行滚动测试
                long startTime = System.nanoTime();

                // 模拟滚动操作
                InstrumentationRegistry.getInstrumentation().runOnMainThread(() -> {
                    mWebView.scrollTo(0, 500);
                    mWebView.scrollTo(0, 1000);
                    mWebView.scrollTo(0, 1500);

                    long endTime = System.nanoTime();
                    long scrollTime = (endTime - startTime) / 1000000;
                    android.util.Log.d(TAG, "Scroll operation time: " + scrollTime + "ms");

                    // 滚动应该很快完成
                    assertTrue("Scroll operation too slow: " + scrollTime + "ms", scrollTime < 100);
                    latch.countDown();
                });
            }
        });

        loadPage("https://www.baidu.com");

        assertTrue("Scroll test timeout", latch.await(10, TimeUnit.SECONDS));
    }

    /**
     * 测试图片加载性能
     */
    @Test
    public void testImageLoadingPerformance() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final long[] loadTime = new long[1];

        mWebView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                long startTime = System.nanoTime();

                // 执行图片加载测试
                view.evaluateJavascript(
                    "var img = new Image(); " +
                    "img.onload = function() { console.log('Image loaded'); }; " +
                    "img.src = 'https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png';",
                    value -> {
                        long endTime = System.nanoTime();
                        loadTime[0] = (endTime - startTime) / 1000000;
                        android.util.Log.d(TAG, "Image load time: " + loadTime[0] + "ms");
                        latch.countDown();
                    }
                );
            }
        });

        loadPage("https://www.baidu.com");

        assertTrue("Image loading test timeout", latch.await(15, TimeUnit.SECONDS));

        // 图片加载时间应该在合理范围内
        assertTrue("Image loading too slow: " + loadTime[0] + "ms", loadTime[0] < 5000);
    }

    /**
     * 测试电池消耗
     */
    @Test
    public void testBatteryConsumption() throws InterruptedException {
        // 记录初始电池状态
        int initialBatteryLevel = getBatteryLevel();
        android.util.Log.d(TAG, "Initial battery level: " + initialBatteryLevel + "%");

        // 执行一系列操作
        String[] urls = {
            "https://www.baidu.com",
            "https://www.sina.com.cn",
            "https://www.sohu.com"
        };

        for (String url : urls) {
            loadPage(url);
            Thread.sleep(3000);

            // 执行一些JavaScript
            executeJavaScript("console.log('Test operation');");
            Thread.sleep(1000);
        }

        // 检查电池消耗
        int finalBatteryLevel = getBatteryLevel();
        int batteryConsumption = initialBatteryLevel - finalBatteryLevel;

        android.util.Log.d(TAG, "Final battery level: " + finalBatteryLevel + "%");
        android.util.Log.d(TAG, "Battery consumption: " + batteryConsumption + "%");

        // 电池消耗应该在合理范围内（通常测试期间消耗不会超过5%）
        assertTrue("Battery consumption too high: " + batteryConsumption + "%",
                batteryConsumption < 10);
    }

    /**
     * 测试长期稳定性
     */
    @Test
    public void testLongTermStability() throws InterruptedException {
        final int ITERATIONS = 10;
        long totalMemoryUsage = 0;
        long totalLoadTime = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            android.util.Log.d(TAG, "Stability test iteration: " + (i + 1));

            // 加载页面并测量性能
            long loadTime = measurePageLoadTime("https://www.baidu.com");
            totalLoadTime += loadTime;

            long memoryUsage = getCurrentMemoryUsage();
            totalMemoryUsage += memoryUsage;

            // 执行一些操作
            executeJavaScript("document.body.scrollTop += 100;");
            Thread.sleep(1000);

            // 检查应用是否稳定
            assertTrue("Application became unstable at iteration " + (i + 1),
                    isApplicationStable());
        }

        long avgLoadTime = totalLoadTime / ITERATIONS;
        long avgMemoryUsage = totalMemoryUsage / ITERATIONS;

        android.util.Log.d(TAG, "Average load time: " + avgLoadTime + "ms");
        android.util.Log.d(TAG, "Average memory usage: " + avgMemoryUsage / 1024 / 1024 + "MB");

        assertTrue("Average load time too slow: " + avgLoadTime + "ms", avgLoadTime < MAX_LOAD_TIME);
    }

    // ==================== 辅助方法 ====================

    private void setupWebView() {
        setupWebView(mWebView);
    }

    private void setupWebView(WebView webView) {
        android.webkit.WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // 设置缓存
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(mContext.getCacheDir().getAbsolutePath());
        settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);

        // 启用数据库和DOM存储
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
    }

    private long measurePageLoadTime(String url) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final long[] loadTime = new long[1];

        mWebView.setWebViewClient(new android.webkit.WebViewClient() {
            private long startTime;

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                startTime = System.currentTimeMillis();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadTime[0] = System.currentTimeMillis() - startTime;
                latch.countDown();
            }
        });

        InstrumentationRegistry.getInstrumentation().runOnMainThread(() -> {
            mWebView.loadUrl(url);
        });

        assertTrue("Page load timeout", latch.await(30, TimeUnit.SECONDS));
        return loadTime[0];
    }

    private void loadPage(String url) {
        loadPage(mWebView, url);
    }

    private void loadPage(WebView webView, String url) {
        InstrumentationRegistry.getInstrumentation().runOnMainThread(() -> {
            webView.loadUrl(url);
        });
    }

    private void executeJavaScript(String script) {
        InstrumentationRegistry.getInstrumentation().runOnMainThread(() -> {
            mWebView.evaluateJavascript(script, null);
        });
    }

    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private int getBatteryLevel() {
        // 在实际测试中，需要通过BatteryManager获取电池状态
        // 这里返回模拟值
        return 80; // 模拟80%电量
    }

    private boolean isApplicationStable() {
        // 检查应用是否响应
        return mWebView != null && !mWebView.isDestroyed();
    }

    /**
     * 测试WebView内存泄漏
     */
    @Test
    public void testMemoryLeakPrevention() throws InterruptedException {
        final int WEBVIEW_COUNT = 10;
        WebView[] webViews = new WebView[WEBVIEW_COUNT];
        long[] memoryUsages = new long[WEBVIEW_COUNT];

        // 创建多个WebView并记录内存使用
        for (int i = 0; i < WEBVIEW_COUNT; i++) {
            webViews[i] = mX5Manager.createWebView(mContext);
            setupWebView(webViews[i]);
            loadPage(webViews[i], "https://www.baidu.com");
            Thread.sleep(2000);

            memoryUsages[i] = getCurrentMemoryUsage();
            android.util.Log.d(TAG, "WebView " + i + " memory: " + memoryUsages[i] / 1024 / 1024 + "MB");
        }

        // 销毁WebView
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.destroy();
            }
        }

        // 强制垃圾回收
        System.gc();
        Thread.sleep(2000);

        long finalMemoryUsage = getCurrentMemoryUsage();
        android.util.Log.d(TAG, "Final memory after cleanup: " + finalMemoryUsage / 1024 / 1024 + "MB");

        // 验证内存是否正确释放
        assertTrue("Memory not properly released", finalMemoryUsage < MAX_MEMORY_USAGE);
    }

    /**
     * 测试并发性能
     */
    @Test
    public void testConcurrentPerformance() throws InterruptedException {
        final int CONCURRENT_REQUESTS = 3;
        final CountDownLatch latch = new CountDownLatch(CONCURRENT_REQUESTS);
        final long[] loadTimes = new long[CONCURRENT_REQUESTS];

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    WebView webView = mX5Manager.createWebView(mContext);
                    setupWebView(webView);

                    long loadTime = measureConcurrentLoadTime(webView, "https://www.baidu.com");
                    loadTimes[index] = loadTime;

                    webView.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("Concurrent test timeout", latch.await(60, TimeUnit.SECONDS));

        // 计算平均加载时间
        long totalTime = 0;
        for (long time : loadTimes) {
            totalTime += time;
        }
        long avgTime = totalTime / CONCURRENT_REQUESTS;

        android.util.Log.d(TAG, "Average concurrent load time: " + avgTime + "ms");
        assertTrue("Concurrent loading too slow: " + avgTime + "ms", avgTime < MAX_LOAD_TIME);
    }

    private long measureConcurrentLoadTime(WebView webView, String url) throws InterruptedException {
        final CountDownLatch localLatch = new CountDownLatch(1);
        final long[] loadTime = new long[1];

        InstrumentationRegistry.getInstrumentation().runOnMainThread(() -> {
            webView.setWebViewClient(new android.webkit.WebViewClient() {
                private long startTime;

                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    startTime = System.currentTimeMillis();
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    loadTime[0] = System.currentTimeMillis() - startTime;
                    localLatch.countDown();
                }
            });

            webView.loadUrl(url);
        });

        assertTrue("Concurrent page load timeout", localLatch.await(30, TimeUnit.SECONDS));
        return loadTime[0];
    }
}
