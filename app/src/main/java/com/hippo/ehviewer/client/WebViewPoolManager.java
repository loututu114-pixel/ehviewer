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

package com.hippo.ehviewer.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.widget.UnifiedWebView;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebView对象池管理器
 * 用于优化WebView的创建和销毁，提升性能和用户体验
 */
public class WebViewPoolManager {

    private static final String TAG = "WebViewPoolManager";

    private static final int MAX_POOL_SIZE = 3; // 最大池大小
    private static final int MIN_POOL_SIZE = 1; // 最小池大小
    private static final long WEBVIEW_TIMEOUT = 5 * 60 * 1000; // 5分钟超时

    private static WebViewPoolManager sInstance;

    // WebView对象池
    private final Queue<UnifiedWebView> mWebViewPool = new LinkedList<>();
    private final Queue<Long> mWebViewTimestamps = new LinkedList<>();

    // 计数器
    private final AtomicInteger mActiveWebViews = new AtomicInteger(0);
    private final AtomicInteger mPoolSize = new AtomicInteger(0);

    // 上下文
    private Context mContext;

    // 清理线程
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCleanupRunnable = this::cleanupExpiredWebViews;

    /**
     * 获取单例实例
     */
    public static synchronized WebViewPoolManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new WebViewPoolManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private WebViewPoolManager(Context context) {
        this.mContext = context;
        initializePool();
        startCleanupTimer();
    }

    /**
     * 初始化WebView池
     */
    private void initializePool() {
        mHandler.post(() -> {
            for (int i = 0; i < MIN_POOL_SIZE; i++) {
                createAndAddToPool();
            }
            Log.d(TAG, "WebView pool initialized with " + mPoolSize.get() + " instances");
        });
    }

    /**
     * 创建WebView并添加到池中
     */
    private void createAndAddToPool() {
        try {
            UnifiedWebView webView = new UnifiedWebView(mContext);
            configureWebViewForPooling(webView);

            synchronized (mWebViewPool) {
                mWebViewPool.offer(webView);
                mWebViewTimestamps.offer(System.currentTimeMillis());
                mPoolSize.incrementAndGet();
            }

            Log.d(TAG, "Created and pooled WebView, pool size: " + mPoolSize.get());
        } catch (Exception e) {
            Log.e(TAG, "Failed to create WebView for pool", e);
        }
    }

    /**
     * 为池中的WebView配置基础设置
     */
    private void configureWebViewForPooling(UnifiedWebView webView) {
        // 设置基本的WebView配置，优化性能
        android.webkit.WebSettings settings = webView.getSettings();

        // 启用硬件加速
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
        }

        // 基础性能优化设置
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // 应用缓存设置（兼容不同API版本）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        } else {
            try {
                java.lang.reflect.Method setAppCacheEnabled = android.webkit.WebSettings.class.getMethod("setAppCacheEnabled", boolean.class);
                setAppCacheEnabled.invoke(settings, true);
                settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            } catch (Exception e2) {
                Log.w(TAG, "Failed to set app cache enabled", e2);
            }
        }

        // 禁用图片自动加载（可选，由调用者决定）
        settings.setLoadsImagesAutomatically(true);

        // 设置User Agent
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");

        // 其他性能优化
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        Log.d(TAG, "WebView configured for pooling");
    }

    /**
     * 从池中获取WebView
     */
    @Nullable
    public UnifiedWebView acquireWebView() {
        synchronized (mWebViewPool) {
            UnifiedWebView webView = mWebViewPool.poll();
            if (webView != null) {
                Long timestamp = mWebViewTimestamps.poll();
                mPoolSize.decrementAndGet();
                mActiveWebViews.incrementAndGet();

                Log.d(TAG, "Acquired WebView from pool, remaining pool size: " + mPoolSize.get() +
                          ", active WebViews: " + mActiveWebViews.get());

                return webView;
            }
        }

        // 池为空时，创建新的WebView
        Log.d(TAG, "Pool empty, creating new WebView");
        try {
            UnifiedWebView webView = new UnifiedWebView(mContext);
            configureWebViewForPooling(webView);
            mActiveWebViews.incrementAndGet();
            return webView;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create new WebView", e);
            return null;
        }
    }

    /**
     * 将WebView放回池中
     */
    public void releaseWebView(@NonNull UnifiedWebView webView) {
        if (webView == null) {
            return;
        }

        mHandler.post(() -> {
            try {
                // 清理WebView状态
                webView.clearHistory();
                // clearCache方法的参数因API版本而异
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        webView.clearCache(true);
                    } else {
                        // API 18及以下使用无参数版本
                        java.lang.reflect.Method clearCache = android.webkit.WebView.class.getMethod("clearCache", boolean.class);
                        clearCache.invoke(webView, true);
                    }
                } catch (Exception e2) {
                    Log.w(TAG, "Failed to clear WebView cache", e2);
                }
                webView.clearFormData();

                // 停止加载
                webView.stopLoading();

                // 移除所有视图
                if (webView.getParent() != null && webView.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) webView.getParent()).removeView(webView);
                }

                synchronized (mWebViewPool) {
                    if (mPoolSize.get() < MAX_POOL_SIZE) {
                        mWebViewPool.offer(webView);
                        mWebViewTimestamps.offer(System.currentTimeMillis());
                        mPoolSize.incrementAndGet();

                        Log.d(TAG, "WebView released back to pool, pool size: " + mPoolSize.get());
                    } else {
                        // 池已满，销毁WebView
                        webView.destroy();
                        Log.d(TAG, "Pool full, destroyed WebView");
                    }
                }

                mActiveWebViews.decrementAndGet();
            } catch (Exception e) {
                Log.e(TAG, "Failed to release WebView", e);
                try {
                    webView.destroy();
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to destroy WebView", e2);
                }
                mActiveWebViews.decrementAndGet();
            }
        });
    }

    /**
     * 清理过期的WebView
     */
    private void cleanupExpiredWebViews() {
        synchronized (mWebViewPool) {
            long currentTime = System.currentTimeMillis();
            int cleanedCount = 0;

            while (!mWebViewTimestamps.isEmpty()) {
                Long timestamp = mWebViewTimestamps.peek();
                if (timestamp != null && (currentTime - timestamp) > WEBVIEW_TIMEOUT) {
                    UnifiedWebView webView = mWebViewPool.poll();
                    mWebViewTimestamps.poll();

                    if (webView != null) {
                        try {
                            webView.destroy();
                            cleanedCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to destroy expired WebView", e);
                        }
                    }
                } else {
                    break; // 时间戳是有序的，遇到未过期的就可以停止
                }
            }

            if (cleanedCount > 0) {
                mPoolSize.addAndGet(-cleanedCount);
                Log.d(TAG, "Cleaned up " + cleanedCount + " expired WebViews, pool size: " + mPoolSize.get());
            }
        }

        // 安排下次清理
        startCleanupTimer();
    }

    /**
     * 启动清理定时器
     */
    private void startCleanupTimer() {
        mHandler.removeCallbacks(mCleanupRunnable);
        mHandler.postDelayed(mCleanupRunnable, WEBVIEW_TIMEOUT);
    }

    /**
     * 获取池状态信息
     */
    public String getPoolStatus() {
        return String.format("Pool: %d, Active: %d, Total: %d",
                mPoolSize.get(), mActiveWebViews.get(),
                mPoolSize.get() + mActiveWebViews.get());
    }

    /**
     * 预热池（在应用启动时调用）
     */
    public void warmUpPool() {
        mHandler.post(() -> {
            synchronized (mWebViewPool) {
                int currentSize = mPoolSize.get();
                int targetSize = Math.min(MAX_POOL_SIZE, MIN_POOL_SIZE + 1);

                for (int i = currentSize; i < targetSize; i++) {
                    createAndAddToPool();
                }
            }
            Log.d(TAG, "Pool warmed up, current size: " + mPoolSize.get());
        });
    }

    /**
     * 清空池（在应用退出时调用）
     */
    public void clearPool() {
        mHandler.removeCallbacks(mCleanupRunnable);

        synchronized (mWebViewPool) {
            while (!mWebViewPool.isEmpty()) {
                UnifiedWebView webView = mWebViewPool.poll();
                if (webView != null) {
                    try {
                        webView.destroy();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to destroy WebView during clear", e);
                    }
                }
            }
            mWebViewTimestamps.clear();
            mPoolSize.set(0);
        }

        Log.d(TAG, "WebView pool cleared");
    }

    /**
     * 获取池大小
     */
    public int getPoolSize() {
        return mPoolSize.get();
    }

    /**
     * 获取活跃WebView数量
     */
    public int getActiveWebViewCount() {
        return mActiveWebViews.get();
    }
}
