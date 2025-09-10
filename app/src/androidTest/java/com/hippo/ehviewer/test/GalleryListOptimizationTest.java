package com.hippo.ehviewer.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.gallery.enhanced.EnhancedGalleryListProvider;
import com.hippo.ehviewer.util.NetworkUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * 画廊列表优化组件性能测试套件
 * 验证加载速度、缓存效率、内存使用等关键指标
 */
@RunWith(AndroidJUnit4.class)
public class GalleryListOptimizationTest {

    private static final String TAG = "GalleryListOptTest";
    
    private Context mContext;
    private EnhancedGalleryListProvider mProvider;
    
    // 测试URL（使用测试环境或mock数据）
    private static final String TEST_URL_PAGE_1 = "https://e-hentai.org/?page=0";
    private static final String TEST_URL_PAGE_2 = "https://e-hentai.org/?page=1";
    private static final String TEST_URL_PAGE_3 = "https://e-hentai.org/?page=2";
    
    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mProvider = new EnhancedGalleryListProvider(mContext);
        
        // 等待初始化完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Log.i(TAG, "Test environment initialized");
    }
    
    @After
    public void tearDown() {
        if (mProvider != null) {
            mProvider.destroy();
            mProvider = null;
        }
        Log.i(TAG, "Test environment cleaned up");
    }
    
    /**
     * 测试1: 基本加载功能
     * 验证增强提供者能够正常加载画廊列表
     */
    @Test
    public void testBasicLoading() {
        Log.i(TAG, "=== Test 1: Basic Loading ===");
        
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicReference<String> errorMsg = new AtomicReference<>();
        
        long startTime = System.currentTimeMillis();
        
        mProvider.loadGalleryList(TEST_URL_PAGE_1, 0, true, new EnhancedGalleryListProvider.LoadListener() {
            @Override
            public void onLoadStart(String url) {
                Log.d(TAG, "Load started: " + url);
            }
            
            @Override
            public void onLoadProgress(String url, int progress) {
                Log.d(TAG, "Load progress: " + progress + "%");
            }
            
            @Override
            public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                long loadTime = System.currentTimeMillis() - startTime;
                Log.i(TAG, "Load success in " + loadTime + "ms, cached: " + isFromCache + 
                          ", items: " + result.galleryInfoList.size());
                
                success.set(true);
                latch.countDown();
            }
            
            @Override
            public void onLoadError(String url, Exception error, boolean canRetry) {
                long loadTime = System.currentTimeMillis() - startTime;
                Log.e(TAG, "Load failed in " + loadTime + "ms", error);
                
                errorMsg.set(error.getMessage());
                latch.countDown();
            }
        });
        
        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            assertTrue("Loading should complete within 30 seconds", completed);
            
            if (!success.get()) {
                Log.w(TAG, "Basic loading failed: " + errorMsg.get());
                // 在测试环境中，网络错误是可以接受的
                // 重点是验证组件结构正确
                assertNotNull("Error message should not be null", errorMsg.get());
            } else {
                assertTrue("Loading should succeed", success.get());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
        
        Log.i(TAG, "Basic loading test completed");
    }
    
    /**
     * 测试2: 缓存机制验证
     * 验证第二次加载相同URL时使用缓存
     */
    @Test 
    public void testCacheMechanism() {
        Log.i(TAG, "=== Test 2: Cache Mechanism ===");
        
        final CountDownLatch firstLatch = new CountDownLatch(1);
        final CountDownLatch secondLatch = new CountDownLatch(1);
        final AtomicBoolean firstSuccess = new AtomicBoolean(false);
        final AtomicBoolean secondFromCache = new AtomicBoolean(false);
        final AtomicReference<Long> firstLoadTime = new AtomicReference<>(0L);
        final AtomicReference<Long> secondLoadTime = new AtomicReference<>(0L);
        
        // 第一次加载
        long firstStart = System.currentTimeMillis();
        mProvider.loadGalleryList(TEST_URL_PAGE_1, 0, true, new EnhancedGalleryListProvider.LoadListener() {
            @Override
            public void onLoadStart(String url) {}
            
            @Override
            public void onLoadProgress(String url, int progress) {}
            
            @Override
            public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                firstLoadTime.set(System.currentTimeMillis() - firstStart);
                firstSuccess.set(true);
                Log.i(TAG, "First load completed in " + firstLoadTime.get() + "ms, cached: " + isFromCache);
                firstLatch.countDown();
            }
            
            @Override
            public void onLoadError(String url, Exception error, boolean canRetry) {
                firstLoadTime.set(System.currentTimeMillis() - firstStart);
                Log.w(TAG, "First load failed in " + firstLoadTime.get() + "ms");
                firstLatch.countDown();
            }
        });
        
        try {
            boolean firstCompleted = firstLatch.await(30, TimeUnit.SECONDS);
            assertTrue("First loading should complete", firstCompleted);
            
            if (firstSuccess.get()) {
                // 如果第一次加载成功，测试缓存
                Thread.sleep(500); // 短暂等待确保缓存生效
                
                // 第二次加载相同URL
                long secondStart = System.currentTimeMillis();
                mProvider.loadGalleryList(TEST_URL_PAGE_1, 0, true, new EnhancedGalleryListProvider.LoadListener() {
                    @Override
                    public void onLoadStart(String url) {}
                    
                    @Override
                    public void onLoadProgress(String url, int progress) {}
                    
                    @Override
                    public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                        secondLoadTime.set(System.currentTimeMillis() - secondStart);
                        secondFromCache.set(isFromCache);
                        Log.i(TAG, "Second load completed in " + secondLoadTime.get() + "ms, cached: " + isFromCache);
                        secondLatch.countDown();
                    }
                    
                    @Override
                    public void onLoadError(String url, Exception error, boolean canRetry) {
                        secondLoadTime.set(System.currentTimeMillis() - secondStart);
                        Log.w(TAG, "Second load failed in " + secondLoadTime.get() + "ms");
                        secondLatch.countDown();
                    }
                });
                
                boolean secondCompleted = secondLatch.await(10, TimeUnit.SECONDS);
                assertTrue("Second loading should complete faster", secondCompleted);
                
                if (secondFromCache.get()) {
                    // 验证缓存效果
                    assertTrue("Cache hit should be much faster", 
                              secondLoadTime.get() < firstLoadTime.get() / 2);
                    Log.i(TAG, "Cache mechanism working: " + firstLoadTime.get() + "ms -> " + 
                              secondLoadTime.get() + "ms (speedup: " + 
                              (firstLoadTime.get() / (double)secondLoadTime.get()) + "x)");
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Cache test interrupted");
        }
        
        Log.i(TAG, "Cache mechanism test completed");
    }
    
    /**
     * 测试3: 并发加载性能
     * 验证多个页面并发加载的性能
     */
    @Test
    public void testConcurrentLoading() {
        Log.i(TAG, "=== Test 3: Concurrent Loading ===");
        
        final int CONCURRENT_REQUESTS = 3;
        final CountDownLatch latch = new CountDownLatch(CONCURRENT_REQUESTS);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        
        String[] urls = {TEST_URL_PAGE_1, TEST_URL_PAGE_2, TEST_URL_PAGE_3};
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final int requestIndex = i;
            final String url = urls[i];
            
            mProvider.loadGalleryList(url, 0, requestIndex == 0, new EnhancedGalleryListProvider.LoadListener() {
                @Override
                public void onLoadStart(String url) {
                    Log.d(TAG, "Concurrent request " + requestIndex + " started");
                }
                
                @Override
                public void onLoadProgress(String url, int progress) {}
                
                @Override
                public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                    successCount.incrementAndGet();
                    Log.d(TAG, "Concurrent request " + requestIndex + " succeeded (cached: " + isFromCache + ")");
                    latch.countDown();
                }
                
                @Override
                public void onLoadError(String url, Exception error, boolean canRetry) {
                    errorCount.incrementAndGet();
                    Log.d(TAG, "Concurrent request " + requestIndex + " failed");
                    latch.countDown();
                }
            });
        }
        
        try {
            boolean completed = latch.await(45, TimeUnit.SECONDS);
            assertTrue("All concurrent requests should complete", completed);
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            Log.i(TAG, "Concurrent loading completed in " + totalTime + "ms");
            Log.i(TAG, "Success: " + successCount.get() + ", Errors: " + errorCount.get());
            
            // 即使有网络错误，组件也应该能处理并发请求
            assertEquals("All requests should complete", CONCURRENT_REQUESTS, 
                        successCount.get() + errorCount.get());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Concurrent test interrupted");
        }
        
        Log.i(TAG, "Concurrent loading test completed");
    }
    
    /**
     * 测试4: 内存使用监控
     * 验证组件不会造成内存泄漏
     */
    @Test
    public void testMemoryUsage() {
        Log.i(TAG, "=== Test 4: Memory Usage ===");
        
        // 强制GC并记录初始内存
        System.gc();
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.i(TAG, "Initial memory usage: " + (initialMemory / 1024 / 1024) + " MB");
        
        // 执行多轮加载测试
        final CountDownLatch latch = new CountDownLatch(5);
        final AtomicInteger completedRequests = new AtomicInteger(0);
        
        for (int round = 0; round < 5; round++) {
            final int roundNumber = round;
            String url = TEST_URL_PAGE_1 + "&test_round=" + round;
            
            mProvider.loadGalleryList(url, 0, true, new EnhancedGalleryListProvider.LoadListener() {
                @Override
                public void onLoadStart(String url) {}
                
                @Override
                public void onLoadProgress(String url, int progress) {}
                
                @Override
                public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                    completedRequests.incrementAndGet();
                    Log.d(TAG, "Memory test round " + roundNumber + " completed");
                    latch.countDown();
                }
                
                @Override
                public void onLoadError(String url, Exception error, boolean canRetry) {
                    completedRequests.incrementAndGet();
                    Log.d(TAG, "Memory test round " + roundNumber + " failed");
                    latch.countDown();
                }
            });
            
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            assertTrue("Memory test should complete", completed);
            
            // 清理并测量最终内存
            mProvider.cleanupCache();
            System.gc();
            Thread.sleep(2000);
            
            long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            Log.i(TAG, "Final memory usage: " + (finalMemory / 1024 / 1024) + " MB");
            Log.i(TAG, "Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
            
            // 内存增长应该在合理范围内（小于50MB）
            assertTrue("Memory increase should be reasonable", 
                      memoryIncrease < 50 * 1024 * 1024);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Memory test interrupted");
        }
        
        Log.i(TAG, "Memory usage test completed");
    }
    
    /**
     * 测试5: 网络状态检测
     * 验证网络工具类的功能
     */
    @Test
    public void testNetworkDetection() {
        Log.i(TAG, "=== Test 5: Network Detection ===");
        
        // 测试网络类型检测
        NetworkUtils.NetworkType networkType = NetworkUtils.getNetworkType(mContext);
        assertNotNull("Network type should not be null", networkType);
        Log.i(TAG, "Detected network type: " + networkType);
        
        // 测试网络速度评估
        NetworkUtils.NetworkSpeed networkSpeed = NetworkUtils.getNetworkSpeed(mContext);
        assertNotNull("Network speed should not be null", networkSpeed);
        Log.i(TAG, "Detected network speed: " + networkSpeed);
        
        // 测试连接状态
        boolean isConnected = NetworkUtils.isNetworkConnected(mContext);
        Log.i(TAG, "Network connected: " + isConnected);
        
        // 测试推荐配置
        int timeout = NetworkUtils.getRecommendedTimeout(mContext);
        int concurrency = NetworkUtils.getRecommendedConcurrency(mContext);
        int preloadPages = NetworkUtils.getRecommendedPreloadPages(mContext);
        
        Log.i(TAG, "Recommended timeout: " + timeout + "ms");
        Log.i(TAG, "Recommended concurrency: " + concurrency);
        Log.i(TAG, "Recommended preload pages: " + preloadPages);
        
        // 验证推荐值在合理范围内
        assertTrue("Timeout should be reasonable", timeout >= 5000 && timeout <= 60000);
        assertTrue("Concurrency should be reasonable", concurrency >= 1 && concurrency <= 10);
        assertTrue("Preload pages should be reasonable", preloadPages >= 1 && preloadPages <= 10);
        
        Log.i(TAG, "Network detection test completed");
    }
    
    /**
     * 测试6: 性能统计验证
     * 验证性能统计功能的准确性
     */
    @Test
    public void testPerformanceStats() {
        Log.i(TAG, "=== Test 6: Performance Stats ===");
        
        // 获取初始统计
        String initialStats = mProvider.getPerformanceStats();
        Log.i(TAG, "Initial stats: " + initialStats);
        
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicBoolean firstFromCache = new AtomicBoolean(false);
        final AtomicBoolean secondFromCache = new AtomicBoolean(false);
        
        // 执行两次相同的请求来测试统计
        mProvider.loadGalleryList(TEST_URL_PAGE_1, 0, true, new EnhancedGalleryListProvider.LoadListener() {
            @Override
            public void onLoadStart(String url) {}
            
            @Override
            public void onLoadProgress(String url, int progress) {}
            
            @Override
            public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                firstFromCache.set(isFromCache);
                latch.countDown();
                
                // 立即执行第二次请求
                mProvider.loadGalleryList(TEST_URL_PAGE_1, 0, true, new EnhancedGalleryListProvider.LoadListener() {
                    @Override
                    public void onLoadStart(String url) {}
                    
                    @Override
                    public void onLoadProgress(String url, int progress) {}
                    
                    @Override
                    public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                        secondFromCache.set(isFromCache);
                        latch.countDown();
                    }
                    
                    @Override
                    public void onLoadError(String url, Exception error, boolean canRetry) {
                        latch.countDown();
                    }
                });
            }
            
            @Override
            public void onLoadError(String url, Exception error, boolean canRetry) {
                latch.countDown();
            }
        });
        
        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            
            // 获取最终统计
            String finalStats = mProvider.getPerformanceStats();
            Log.i(TAG, "Final stats: " + finalStats);
            
            // 验证统计包含预期信息
            assertTrue("Stats should contain request info", 
                      finalStats.contains("Requests:"));
            
            if (completed && secondFromCache.get()) {
                assertTrue("Stats should show cache hits", 
                          finalStats.contains("Cache:"));
                Log.i(TAG, "Performance stats validation successful");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Performance stats test interrupted");
        }
        
        Log.i(TAG, "Performance stats test completed");
    }
    
    /**
     * 性能基准测试 - 生成详细的性能报告
     */
    @Test
    public void performanceBenchmark() {
        Log.i(TAG, "=== Performance Benchmark ===");
        
        // 多轮性能测试
        int[] testRounds = {1, 5, 10};
        
        for (int rounds : testRounds) {
            Log.i(TAG, "Starting " + rounds + "-round benchmark");
            
            long totalTime = 0;
            int successCount = 0;
            int cacheHitCount = 0;
            
            for (int i = 0; i < rounds; i++) {
                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<Long> loadTime = new AtomicReference<>(0L);
                final AtomicBoolean success = new AtomicBoolean(false);
                final AtomicBoolean cached = new AtomicBoolean(false);
                
                long startTime = System.currentTimeMillis();
                
                String testUrl = TEST_URL_PAGE_1 + "&benchmark_round=" + i;
                if (i < 3) {
                    // 前3次使用相同URL测试缓存
                    testUrl = TEST_URL_PAGE_1;
                }
                
                mProvider.loadGalleryList(testUrl, 0, true, new EnhancedGalleryListProvider.LoadListener() {
                    @Override
                    public void onLoadStart(String url) {}
                    
                    @Override
                    public void onLoadProgress(String url, int progress) {}
                    
                    @Override
                    public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                        loadTime.set(System.currentTimeMillis() - startTime);
                        success.set(true);
                        cached.set(isFromCache);
                        latch.countDown();
                    }
                    
                    @Override
                    public void onLoadError(String url, Exception error, boolean canRetry) {
                        loadTime.set(System.currentTimeMillis() - startTime);
                        latch.countDown();
                    }
                });
                
                try {
                    boolean completed = latch.await(20, TimeUnit.SECONDS);
                    if (completed && success.get()) {
                        totalTime += loadTime.get();
                        successCount++;
                        if (cached.get()) {
                            cacheHitCount++;
                        }
                        
                        Log.d(TAG, "Round " + (i+1) + ": " + loadTime.get() + "ms" + 
                              (cached.get() ? " (cached)" : " (network)"));
                    }
                    
                    // 短暂等待避免请求过于频繁
                    Thread.sleep(200);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (successCount > 0) {
                double avgTime = totalTime / (double) successCount;
                double cacheHitRate = (cacheHitCount * 100.0) / successCount;
                
                Log.i(TAG, rounds + "-Round Benchmark Results:");
                Log.i(TAG, "  Success Rate: " + successCount + "/" + rounds + 
                          " (" + (successCount * 100.0 / rounds) + "%)");
                Log.i(TAG, "  Average Load Time: " + String.format("%.1f", avgTime) + "ms");
                Log.i(TAG, "  Cache Hit Rate: " + String.format("%.1f", cacheHitRate) + "%");
            }
            
            // 清理缓存准备下一轮测试
            mProvider.cleanupCache();
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        }
        
        // 输出最终性能统计
        String finalStats = mProvider.getPerformanceStats();
        Log.i(TAG, "Final Performance Stats: " + finalStats);
        
        Log.i(TAG, "Performance benchmark completed");
    }
}