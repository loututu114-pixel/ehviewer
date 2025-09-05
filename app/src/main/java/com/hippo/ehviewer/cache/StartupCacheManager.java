package com.hippo.ehviewer.cache;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 启动缓存管理器 - 实现速度优先的缓存策略
 * 用于加速应用的启动过程
 */
public class StartupCacheManager {
    private static final String TAG = "StartupCacheManager";
    private static StartupCacheManager instance;

    private final Context context;
    private final ExecutorService cacheExecutor;
    private final Handler mainHandler;

    // 快速访问缓存
    private final LruCache<String, Object> fastCache;

    // 预加载状态
    private boolean isPreloaded = false;
    private long preloadStartTime = 0;

    private StartupCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.cacheExecutor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 初始化快速缓存，最大存储100个对象
        this.fastCache = new LruCache<>(100);
    }

    public static synchronized StartupCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new StartupCacheManager(context);
        }
        return instance;
    }

    /**
     * 开始预加载关键资源
     */
    public void startPreloading() {
        if (isPreloaded) {
            Log.d(TAG, "Resources already preloaded");
            return;
        }

        preloadStartTime = System.currentTimeMillis();
        Log.d(TAG, "Starting preload at: " + preloadStartTime);

        cacheExecutor.execute(() -> {
            try {
                // 预加载WebView相关资源
                preloadWebViewResources();

                // 预加载图库相关资源
                preloadGalleryResources();

                // 预加载用户脚本（核心脚本）
                preloadEssentialScripts();

                isPreloaded = true;
                long preloadTime = System.currentTimeMillis() - preloadStartTime;

                mainHandler.post(() -> {
                    Log.d(TAG, "Preloading completed in " + preloadTime + "ms");
                    onPreloadCompleted();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during preloading", e);
            }
        });
    }

    /**
     * 预加载WebView相关资源
     */
    private void preloadWebViewResources() {
        // 缓存常用URL
        fastCache.put("homepage_url", "https://www.google.com");
        fastCache.put("search_url", "https://www.google.com/search");

        // 预加载用户偏好设置
        preloadUserPreferences();
    }

    /**
     * 预加载图库相关资源
     */
    private void preloadGalleryResources() {
        // 预加载图库设置
        fastCache.put("gallery_reading_direction", 0); // 默认阅读方向
        fastCache.put("gallery_page_scaling", 0);     // 默认缩放模式

        // 预加载常用配置
        preloadGallerySettings();
    }

    /**
     * 预加载核心脚本
     */
    private void preloadEssentialScripts() {
        // 只预加载最核心的脚本ID
        String[] essentialScripts = {
            "baidu_app_blocker",
            "app_intercept_blocker"
        };

        for (String scriptId : essentialScripts) {
            fastCache.put("script_" + scriptId, true);
        }
    }

    /**
     * 预加载用户偏好设置
     */
    private void preloadUserPreferences() {
        // 这里可以预加载用户的偏好设置
        // 例如：默认搜索引擎、主题设置等
    }

    /**
     * 预加载图库设置
     */
    private void preloadGallerySettings() {
        // 预加载图库相关的设置
        // 例如：阅读方向、缩放模式、全屏设置等
    }

    /**
     * 获取缓存的对象
     */
    public Object getCachedObject(String key) {
        return fastCache.get(key);
    }

    /**
     * 缓存对象
     */
    public void putCachedObject(String key, Object value) {
        fastCache.put(key, value);
    }

    /**
     * 检查资源是否已预加载
     */
    public boolean isPreloaded() {
        return isPreloaded;
    }

    /**
     * 预加载完成回调
     */
    private void onPreloadCompleted() {
        // 可以在这里触发一些初始化完成后的操作
        Log.d(TAG, "All critical resources preloaded successfully");
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        fastCache.evictAll();
        isPreloaded = false;
        Log.d(TAG, "Cache cleared");
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("Cache size: %d/%d, Preloaded: %s",
                fastCache.size(), fastCache.maxSize(), isPreloaded);
    }
}
