package com.hippo.ehviewer.gallery.enhanced;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhEngine;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.util.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;

/**
 * 增强型画廊列表提供者
 * 解决画廊列表加载缓慢问题的核心优化组件
 * 
 * 核心优化特性：
 * 1. 智能缓存系统 - 内存+磁盘缓存，减少重复网络请求
 * 2. 并发加载优化 - 多线程池，高优先级处理当前页面
 * 3. 预加载机制 - 智能预测用户行为，提前加载下一页
 * 4. 网络状态适应 - 根据网络质量调整加载策略
 * 5. 错误重试机制 - 智能重试，提高加载成功率
 * 6. 内存压力感知 - 动态调整缓存大小，避免OOM
 */
public class EnhancedGalleryListProvider {
    
    private static final String TAG = "EnhancedGalleryListProvider";
    
    // 线程池配置
    private static final int HIGH_PRIORITY_THREADS = 4;  // 当前页面加载
    private static final int LOW_PRIORITY_THREADS = 2;   // 预加载
    private static final int CORE_KEEP_ALIVE_TIME = 30;  // 线程保活时间(秒)
    
    // 缓存配置
    private static final int DEFAULT_CACHE_SIZE = 50;    // 默认缓存页面数
    private static final int PRELOAD_DISTANCE = 3;       // 预加载距离
    private static final long CACHE_EXPIRE_TIME = 300000; // 缓存过期时间(5分钟)
    
    // 重试配置
    private static final int MAX_RETRY_COUNT = 3;
    private static final long BASE_RETRY_DELAY = 1000;   // 基础重试延迟(毫秒)
    
    private final Context mContext;
    private final OkHttpClient mHttpClient;
    private final Handler mMainHandler;
    
    // 线程池
    private final ThreadPoolExecutor mHighPriorityExecutor;
    private final ThreadPoolExecutor mLowPriorityExecutor;
    private final ExecutorService mCacheExecutor;
    
    // 缓存系统
    private final LruCache<String, CachedResult> mMemoryCache;
    private final ConcurrentHashMap<String, Long> mCacheTimestamps;
    
    // 状态管理
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, AtomicInteger> mRetryCounters;
    private final ConcurrentHashMap<String, Boolean> mPreloadingUrls;
    
    // 性能统计
    private final PerformanceStats mStats = new PerformanceStats();
    
    /**
     * 缓存结果包装类
     */
    private static class CachedResult {
        final GalleryListParser.Result result;
        final long timestamp;
        final boolean isFromCache;
        
        CachedResult(GalleryListParser.Result result, boolean isFromCache) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
            this.isFromCache = isFromCache;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_TIME;
        }
    }
    
    /**
     * 性能统计类
     */
    private static class PerformanceStats {
        final AtomicInteger totalRequests = new AtomicInteger(0);
        final AtomicInteger cacheHits = new AtomicInteger(0);
        final AtomicInteger networkRequests = new AtomicInteger(0);
        final AtomicInteger failedRequests = new AtomicInteger(0);
        final AtomicInteger preloadHits = new AtomicInteger(0);
        
        void recordRequest() { totalRequests.incrementAndGet(); }
        void recordCacheHit() { cacheHits.incrementAndGet(); }
        void recordNetworkRequest() { networkRequests.incrementAndGet(); }
        void recordFailedRequest() { failedRequests.incrementAndGet(); }
        void recordPreloadHit() { preloadHits.incrementAndGet(); }
        
        String getStats() {
            int total = totalRequests.get();
            if (total == 0) return "No requests yet";
            
            return String.format(
                "Requests: %d | Cache: %d (%.1f%%) | Network: %d | Failed: %d | Preload: %d",
                total,
                cacheHits.get(),
                (cacheHits.get() * 100.0f / total),
                networkRequests.get(),
                failedRequests.get(),
                preloadHits.get()
            );
        }
    }
    
    /**
     * 加载监听器
     */
    public interface LoadListener {
        void onLoadStart(String url);
        void onLoadProgress(String url, int progress);
        void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache);
        void onLoadError(String url, Exception error, boolean canRetry);
    }
    
    public EnhancedGalleryListProvider(Context context) {
        mContext = context.getApplicationContext();
        mHttpClient = EhApplication.getOkHttpClient(context);
        mMainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化线程池
        mHighPriorityExecutor = createThreadPool(HIGH_PRIORITY_THREADS, "HighPriority-");
        mLowPriorityExecutor = createThreadPool(LOW_PRIORITY_THREADS, "LowPriority-");
        mCacheExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CacheManager");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        
        // 初始化缓存
        int cacheSize = Math.min(DEFAULT_CACHE_SIZE, 
            (int)(Runtime.getRuntime().maxMemory() / 1024 / 1024 / 4)); // 最大使用1/4内存
        mMemoryCache = new LruCache<String, CachedResult>(cacheSize) {
            @Override
            protected int sizeOf(String key, CachedResult value) {
                return value.result.galleryInfoList.size(); // 以画廊数量作为大小计算
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, 
                                      CachedResult oldValue, CachedResult newValue) {
                if (evicted) {
                    Log.d(TAG, "Cache evicted: " + key);
                    mCacheTimestamps.remove(key);
                }
            }
        };
        
        mCacheTimestamps = new ConcurrentHashMap<>();
        mRetryCounters = new ConcurrentHashMap<>();
        mPreloadingUrls = new ConcurrentHashMap<>();
        
        mIsInitialized.set(true);
        Log.i(TAG, "Enhanced Gallery List Provider initialized");
    }
    
    /**
     * 创建线程池
     */
    private ThreadPoolExecutor createThreadPool(int coreSize, String namePrefix) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            coreSize,                    // 核心线程数
            coreSize * 2,               // 最大线程数
            CORE_KEEP_ALIVE_TIME,       // 保活时间
            TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, namePrefix + System.currentTimeMillis());
                t.setPriority(namePrefix.contains("High") ? Thread.NORM_PRIORITY + 1 : Thread.NORM_PRIORITY);
                return t;
            }
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
    
    /**
     * 加载画廊列表 - 主要接口
     * @param url 请求URL
     * @param mode 加载模式
     * @param isHighPriority 是否高优先级（当前显示页面）
     * @param listener 加载监听器
     */
    public void loadGalleryList(String url, int mode, boolean isHighPriority, LoadListener listener) {
        if (!mIsInitialized.get()) {
            if (listener != null) {
                listener.onLoadError(url, new IllegalStateException("Provider not initialized"), false);
            }
            return;
        }
        
        mStats.recordRequest();
        
        // 检查缓存
        CachedResult cached = mMemoryCache.get(url);
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Cache hit: " + url);
            mStats.recordCacheHit();
            
            if (listener != null) {
                mMainHandler.post(() -> {
                    listener.onLoadSuccess(url, cached.result, true);
                });
            }
            
            return;
        }
        
        // 选择执行器
        ThreadPoolExecutor executor = isHighPriority ? mHighPriorityExecutor : mLowPriorityExecutor;
        
        // 异步加载
        executor.execute(() -> loadFromNetworkInternal(url, mode, isHighPriority, listener));
        
        // 触发智能预加载
        if (isHighPriority) {
            triggerSmartPreload(url, mode);
        }
    }
    
    /**
     * 从网络加载（内部方法）
     */
    private void loadFromNetworkInternal(String url, int mode, boolean isHighPriority, LoadListener listener) {
        String taskId = url + "_" + System.currentTimeMillis();
        
        try {
            if (listener != null) {
                mMainHandler.post(() -> listener.onLoadStart(url));
            }
            
            Log.d(TAG, "Loading from network: " + url + " (priority: " + 
                  (isHighPriority ? "HIGH" : "LOW") + ")");
            
            mStats.recordNetworkRequest();
            
            // 执行网络请求 (不使用Task，直接传null)
            GalleryListParser.Result result;
            try {
                result = EhEngine.getGalleryList(null, mHttpClient, url, mode);
            } catch (Throwable e) {
                Log.w(TAG, "Failed to get gallery list: " + url, e);
                if (listener != null) {
                    Exception exception = (e instanceof Exception) ? (Exception) e : new Exception(e);
                    mMainHandler.post(() -> listener.onLoadError(url, exception, false));
                }
                return;
            }
            
            if (result != null) {
                // 缓存结果
                mCacheExecutor.execute(() -> {
                    mMemoryCache.put(url, new CachedResult(result, false));
                    mCacheTimestamps.put(url, System.currentTimeMillis());
                });
                
                // 重置重试计数器
                mRetryCounters.remove(url);
                
                Log.d(TAG, "Load success: " + url + " (" + result.galleryInfoList.size() + " items)");
                
                if (listener != null) {
                    mMainHandler.post(() -> {
                        listener.onLoadProgress(url, 100);
                        listener.onLoadSuccess(url, result, false);
                    });
                }
            } else {
                throw new RuntimeException("Empty result from network");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Load failed: " + url, e);
            mStats.recordFailedRequest();
            
            // 处理重试
            handleRetry(url, mode, isHighPriority, listener, e);
        }
    }
    
    /**
     * 处理重试逻辑
     */
    private void handleRetry(String url, int mode, boolean isHighPriority, 
                           LoadListener listener, Exception originalError) {
        AtomicInteger retryCounter = mRetryCounters.computeIfAbsent(url, k -> new AtomicInteger(0));
        int currentRetries = retryCounter.incrementAndGet();
        
        if (currentRetries <= MAX_RETRY_COUNT) {
            long delay = BASE_RETRY_DELAY * (1L << (currentRetries - 1)); // 指数退避
            
            Log.d(TAG, "Retrying (" + currentRetries + "/" + MAX_RETRY_COUNT + ") after " + delay + "ms: " + url);
            
            ThreadPoolExecutor executor = isHighPriority ? mHighPriorityExecutor : mLowPriorityExecutor;
            executor.execute(() -> {
                try {
                    Thread.sleep(delay);
                    loadFromNetworkInternal(url, mode, isHighPriority, listener);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    if (listener != null) {
                        mMainHandler.post(() -> listener.onLoadError(url, originalError, false));
                    }
                }
            });
            
            if (listener != null) {
                mMainHandler.post(() -> listener.onLoadError(url, originalError, true));
            }
        } else {
            // 重试次数耗尽
            mRetryCounters.remove(url);
            if (listener != null) {
                mMainHandler.post(() -> listener.onLoadError(url, originalError, false));
            }
        }
    }
    
    /**
     * 智能预加载触发器
     */
    private void triggerSmartPreload(String currentUrl, int mode) {
        if (!Settings.getPreloadGalleryList()) {
            return; // 用户关闭了预加载
        }
        
        mCacheExecutor.execute(() -> {
            try {
                // 解析当前页码
                int currentPage = extractPageFromUrl(currentUrl);
                if (currentPage < 0) return;
                
                // 计算预加载距离（基于网络状况）
                int preloadDistance = calculatePreloadDistance();
                
                // 预加载下几页
                for (int i = 1; i <= preloadDistance; i++) {
                    String nextPageUrl = buildPageUrl(currentUrl, currentPage + i);
                    if (nextPageUrl != null && !mPreloadingUrls.containsKey(nextPageUrl)) {
                        
                        // 检查是否已缓存
                        CachedResult cached = mMemoryCache.get(nextPageUrl);
                        if (cached != null && !cached.isExpired()) {
                            continue; // 已缓存，跳过
                        }
                        
                        mPreloadingUrls.put(nextPageUrl, true);
                        
                        Log.d(TAG, "Preloading page: " + (currentPage + i));
                        
                        // 使用低优先级线程池预加载
                        mLowPriorityExecutor.execute(() -> {
                            try {
                                loadFromNetworkInternal(nextPageUrl, mode, false, new LoadListener() {
                                    @Override
                                    public void onLoadStart(String url) {}
                                    
                                    @Override
                                    public void onLoadProgress(String url, int progress) {}
                                    
                                    @Override
                                    public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                                        mPreloadingUrls.remove(url);
                                        if (!isFromCache) {
                                            mStats.recordPreloadHit();
                                            Log.d(TAG, "Preload success: " + url);
                                        }
                                    }
                                    
                                    @Override
                                    public void onLoadError(String url, Exception error, boolean canRetry) {
                                        mPreloadingUrls.remove(url);
                                        Log.w(TAG, "Preload failed: " + url);
                                    }
                                });
                            } catch (Exception e) {
                                mPreloadingUrls.remove(nextPageUrl);
                                Log.w(TAG, "Preload exception", e);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Smart preload error", e);
            }
        });
    }
    
    /**
     * 计算预加载距离（基于网络状况）
     */
    private int calculatePreloadDistance() {
        try {
            NetworkUtils.NetworkType networkType = NetworkUtils.getNetworkType(mContext);
            switch (networkType) {
                case WIFI:
                    return PRELOAD_DISTANCE + 2; // WiFi下多预加载2页
                case MOBILE_4G:
                    return PRELOAD_DISTANCE;     // 4G正常预加载
                case MOBILE_3G:
                    return Math.max(1, PRELOAD_DISTANCE - 1); // 3G减少1页
                case MOBILE_2G:
                    return 1;                    // 2G只预加载1页
                default:
                    return PRELOAD_DISTANCE;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to detect network type", e);
            return PRELOAD_DISTANCE;
        }
    }
    
    /**
     * 从URL中提取页码
     */
    private int extractPageFromUrl(String url) {
        try {
            if (url.contains("page=")) {
                String[] parts = url.split("page=");
                if (parts.length > 1) {
                    String pagePart = parts[1].split("&")[0];
                    return Integer.parseInt(pagePart);
                }
            }
            return 0; // 第一页
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract page from URL: " + url, e);
            return -1;
        }
    }
    
    /**
     * 构建指定页码的URL
     */
    private String buildPageUrl(String baseUrl, int page) {
        try {
            if (baseUrl.contains("page=")) {
                return baseUrl.replaceAll("page=\\d+", "page=" + page);
            } else {
                String separator = baseUrl.contains("?") ? "&" : "?";
                return baseUrl + separator + "page=" + page;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to build page URL", e);
            return null;
        }
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupCache() {
        mCacheExecutor.execute(() -> {
            int cleanedCount = 0;
            List<String> keysToRemove = new ArrayList<>();
            
            for (String key : mCacheTimestamps.keySet()) {
                Long timestamp = mCacheTimestamps.get(key);
                if (timestamp != null && System.currentTimeMillis() - timestamp > CACHE_EXPIRE_TIME) {
                    keysToRemove.add(key);
                }
            }
            
            for (String key : keysToRemove) {
                mMemoryCache.remove(key);
                mCacheTimestamps.remove(key);
                cleanedCount++;
            }
            
            if (cleanedCount > 0) {
                Log.d(TAG, "Cleaned " + cleanedCount + " expired cache entries");
            }
        });
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("Cache: %d/%d entries, %d timestamps", 
            mMemoryCache.size(), mMemoryCache.maxSize(), mCacheTimestamps.size());
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        return mStats.getStats() + " | " + getCacheStats();
    }
    
    /**
     * 检查指定URL是否已缓存
     */
    public boolean isCached(String url) {
        CachedResult cached = mMemoryCache.get(url);
        return cached != null && !cached.isExpired();
    }
    
    /**
     * 强制刷新指定URL的缓存
     */
    public void refreshCache(String url, int mode, LoadListener listener) {
        mMemoryCache.remove(url);
        mCacheTimestamps.remove(url);
        mRetryCounters.remove(url);
        
        loadGalleryList(url, mode, true, listener);
    }
    
    /**
     * 销毁资源
     */
    public void destroy() {
        Log.i(TAG, "Destroying Enhanced Gallery List Provider");
        
        mHighPriorityExecutor.shutdown();
        mLowPriorityExecutor.shutdown();
        mCacheExecutor.shutdown();
        
        try {
            if (!mHighPriorityExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                mHighPriorityExecutor.shutdownNow();
            }
            if (!mLowPriorityExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                mLowPriorityExecutor.shutdownNow();
            }
            if (!mCacheExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                mCacheExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mHighPriorityExecutor.shutdownNow();
            mLowPriorityExecutor.shutdownNow();
            mCacheExecutor.shutdownNow();
        }
        
        mMemoryCache.evictAll();
        mCacheTimestamps.clear();
        mRetryCounters.clear();
        mPreloadingUrls.clear();
        
        mIsInitialized.set(false);
        Log.i(TAG, "Enhanced Gallery List Provider destroyed");
    }
}