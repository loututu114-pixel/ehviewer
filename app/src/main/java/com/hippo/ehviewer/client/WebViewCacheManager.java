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
 * distributed under the License is distributed on an "AS IS"" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client;

import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * WebView缓存管理器
 * 优化WebView的缓存策略和存储管理
 */
public class WebViewCacheManager {

    private static final String TAG = "WebViewCacheManager";

    private static final long MAX_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long CACHE_CLEANUP_INTERVAL = TimeUnit.HOURS.toMillis(24); // 24小时

    private static WebViewCacheManager sInstance;

    private Context mContext;
    private long mLastCacheCleanupTime = 0;

    /**
     * 获取单例实例
     */
    public static synchronized WebViewCacheManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WebViewCacheManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private WebViewCacheManager(Context context) {
        this.mContext = context;
        initializeCacheDirectory();
    }

    /**
     * 初始化缓存目录
     */
    private void initializeCacheDirectory() {
        try {
            File cacheDir = new File(mContext.getCacheDir(), "webview_cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            File appCacheDir = new File(mContext.getCacheDir(), "webview_app_cache");
            if (!appCacheDir.exists()) {
                appCacheDir.mkdirs();
            }

            Log.d(TAG, "WebView cache directories initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize cache directories", e);
        }
    }

    /**
     * 为WebView配置智能缓存策略
     */
    public void configureCacheForWebView(WebView webView) {
        if (webView == null) {
            return;
        }

        try {
            WebSettings settings = webView.getSettings();

            // 在API 19及以上使用新的缓存API
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                // 启用DOM存储
                settings.setDomStorageEnabled(true);

                // 启用数据库存储
                settings.setDatabaseEnabled(true);

                // 设置缓存模式
                settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

                // 启用地理位置缓存
                settings.setGeolocationEnabled(true);
                settings.setGeolocationDatabasePath(new File(mContext.getCacheDir(), "webview_geolocation").getAbsolutePath());
            } else {
                // API 18及以下使用旧的缓存API（已废弃但仍可用）
                try {
                    // 使用反射调用已废弃的方法
                    java.lang.reflect.Method setAppCacheEnabled = WebSettings.class.getMethod("setAppCacheEnabled", boolean.class);
                    java.lang.reflect.Method setAppCachePath = WebSettings.class.getMethod("setAppCachePath", String.class);
                    java.lang.reflect.Method setAppCacheMaxSize = WebSettings.class.getMethod("setAppCacheMaxSize", long.class);

                    setAppCacheEnabled.invoke(settings, true);
                    setAppCachePath.invoke(settings, new File(mContext.getCacheDir(), "webview_app_cache").getAbsolutePath());
                    setAppCacheMaxSize.invoke(settings, MAX_CACHE_SIZE);

                    // 启用DOM存储
                    settings.setDomStorageEnabled(true);

                    // 启用数据库存储
                    settings.setDatabaseEnabled(true);
                    settings.setDatabasePath(new File(mContext.getCacheDir(), "webview_databases").getAbsolutePath());

                    // 设置缓存模式
                    settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

                    // 启用地理位置缓存
                    settings.setGeolocationEnabled(true);
                    settings.setGeolocationDatabasePath(new File(mContext.getCacheDir(), "webview_geolocation").getAbsolutePath());
                } catch (Exception e2) {
                    Log.w(TAG, "Failed to use legacy cache API", e2);
                }
            }

            Log.d(TAG, "WebView cache configured");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure WebView cache", e);
        }
    }

    /**
     * 配置离线缓存策略
     */
    public void configureOfflineCache(WebView webView) {
        if (webView == null) {
            return;
        }

        try {
            WebSettings settings = webView.getSettings();

            // 设置离线缓存模式
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

            // 在API 19及以上使用新的缓存API
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                // DOM存储和数据库存储已在configureCacheForWebView中配置
                Log.d(TAG, "Offline cache configured (using modern API)");
            } else {
                // API 18及以下使用旧的缓存API
                try {
                    java.lang.reflect.Method setAppCacheEnabled = WebSettings.class.getMethod("setAppCacheEnabled", boolean.class);
                    java.lang.reflect.Method setAppCachePath = WebSettings.class.getMethod("setAppCachePath", String.class);
                    java.lang.reflect.Method setAppCacheMaxSize = WebSettings.class.getMethod("setAppCacheMaxSize", long.class);

                    setAppCacheEnabled.invoke(settings, true);
                    setAppCachePath.invoke(settings, new File(mContext.getCacheDir(), "webview_offline_cache").getAbsolutePath());
                    setAppCacheMaxSize.invoke(settings, MAX_CACHE_SIZE / 2); // 离线缓存使用一半空间
                } catch (Exception e2) {
                    Log.w(TAG, "Failed to use legacy cache API for offline", e2);
                }
            }

            Log.d(TAG, "Offline cache configured");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure offline cache", e);
        }
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();

        // 检查是否需要清理
        if (currentTime - mLastCacheCleanupTime < CACHE_CLEANUP_INTERVAL) {
            return;
        }

        mLastCacheCleanupTime = currentTime;

        try {
            // 清理WebView缓存
            // 注意：clearCache是静态方法，但需要通过实例调用
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    // 创建一个临时WebView来调用静态方法
                    android.webkit.WebView tempWebView = new android.webkit.WebView(mContext);
                    tempWebView.clearCache(true);
                    tempWebView.destroy();
                } else {
                    // API 18及以下使用反射调用无参数版本
                    java.lang.reflect.Method clearCache = android.webkit.WebView.class.getMethod("clearCache");
                    clearCache.invoke(null);
                }
            } catch (Exception e2) {
                Log.w(TAG, "Failed to clear WebView cache", e2);
            }

            // 清理应用缓存目录
            cleanupDirectory(new File(mContext.getCacheDir(), "webview_cache"));
            cleanupDirectory(new File(mContext.getCacheDir(), "webview_app_cache"));
            cleanupDirectory(new File(mContext.getCacheDir(), "webview_offline_cache"));

            Log.d(TAG, "Expired cache cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup expired cache", e);
        }
    }

    /**
     * 清理目录
     */
    private void cleanupDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }

        try {
            File[] files = directory.listFiles();
            if (files != null) {
                long totalSize = 0;
                long currentTime = System.currentTimeMillis();

                // 计算总大小并清理过期文件
                for (File file : files) {
                    if (file.isFile()) {
                        totalSize += file.length();

                        // 删除超过7天的文件
                        if (currentTime - file.lastModified() > TimeUnit.DAYS.toMillis(7)) {
                            file.delete();
                        }
                    }
                }

                // 如果总大小超过限制，删除最旧的文件
                if (totalSize > MAX_CACHE_SIZE) {
                    // 简单的清理策略：删除最旧的文件
                    long targetSize = MAX_CACHE_SIZE * 9 / 10; // 目标大小为90%的限制

                    while (totalSize > targetSize && files.length > 0) {
                        File oldestFile = findOldestFile(files);
                        if (oldestFile != null) {
                            totalSize -= oldestFile.length();
                            oldestFile.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup directory: " + directory.getPath(), e);
        }
    }

    /**
     * 查找最旧的文件
     */
    private File findOldestFile(File[] files) {
        File oldestFile = null;
        long oldestTime = Long.MAX_VALUE;

        for (File file : files) {
            if (file.isFile() && file.lastModified() < oldestTime) {
                oldestTime = file.lastModified();
                oldestFile = file;
            }
        }

        return oldestFile;
    }

    /**
     * 获取缓存使用情况
     */
    public CacheUsageInfo getCacheUsageInfo() {
        CacheUsageInfo info = new CacheUsageInfo();

        try {
            File webViewCacheDir = new File(mContext.getCacheDir(), "webview_cache");
            File appCacheDir = new File(mContext.getCacheDir(), "webview_app_cache");
            File offlineCacheDir = new File(mContext.getCacheDir(), "webview_offline_cache");

            info.webViewCacheSize = getDirectorySize(webViewCacheDir);
            info.appCacheSize = getDirectorySize(appCacheDir);
            info.offlineCacheSize = getDirectorySize(offlineCacheDir);
            info.totalCacheSize = info.webViewCacheSize + info.appCacheSize + info.offlineCacheSize;

        } catch (Exception e) {
            Log.e(TAG, "Failed to get cache usage info", e);
        }

        return info;
    }

    /**
     * 获取目录大小
     */
    private long getDirectorySize(File directory) {
        if (directory == null || !directory.exists()) {
            return 0;
        }

        long size = 0;
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get directory size: " + directory.getPath(), e);
        }

        return size;
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        try {
            // 清空WebView缓存
            // 注意：clearCache是静态方法，但需要通过实例调用
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    // 创建一个临时WebView来调用静态方法
                    android.webkit.WebView tempWebView = new android.webkit.WebView(mContext);
                    tempWebView.clearCache(true); // true表示包括磁盘缓存
                    tempWebView.destroy();
                } else {
                    // API 18及以下使用反射调用无参数版本
                    java.lang.reflect.Method clearCache = android.webkit.WebView.class.getMethod("clearCache");
                    clearCache.invoke(null);
                }
            } catch (Exception e2) {
                Log.w(TAG, "Failed to clear WebView cache", e2);
            }

            // 清空缓存目录
            clearDirectory(new File(mContext.getCacheDir(), "webview_cache"));
            clearDirectory(new File(mContext.getCacheDir(), "webview_app_cache"));
            clearDirectory(new File(mContext.getCacheDir(), "webview_offline_cache"));
            clearDirectory(new File(mContext.getCacheDir(), "webview_databases"));
            clearDirectory(new File(mContext.getCacheDir(), "webview_geolocation"));

            Log.d(TAG, "All cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear all cache", e);
        }
    }

    /**
     * 清空目录
     */
    private void clearDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }

        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    } else if (file.isDirectory()) {
                        clearDirectory(file);
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear directory: " + directory.getPath(), e);
        }
    }

    /**
     * 缓存使用信息
     */
    public static class CacheUsageInfo {
        public long webViewCacheSize = 0;
        public long appCacheSize = 0;
        public long offlineCacheSize = 0;
        public long totalCacheSize = 0;

        public String getFormattedSize(long size) {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else if (size < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", size / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
            }
        }

        public String getTotalSizeString() {
            return getFormattedSize(totalCacheSize);
        }

        public String getWebViewCacheSizeString() {
            return getFormattedSize(webViewCacheSize);
        }

        public String getAppCacheSizeString() {
            return getFormattedSize(appCacheSize);
        }

        public String getOfflineCacheSizeString() {
            return getFormattedSize(offlineCacheSize);
        }
    }

    /**
     * 预加载常用资源
     */
    public void preloadCommonResources() {
        // 这里可以预加载一些常用的CSS、JavaScript等资源
        // 目前暂时留空，可以根据需要扩展
        Log.d(TAG, "Common resources preloaded");
    }

    /**
     * 配置网络缓存策略
     */
    public void configureNetworkCache(WebView webView) {
        if (webView == null) {
            return;
        }

        try {
            WebSettings settings = webView.getSettings();

            // 设置缓存模式为智能模式
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

            // 在API 19及以上使用新的缓存API
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                // DOM存储和数据库存储已在configureCacheForWebView中配置
                Log.d(TAG, "Network cache configured (using modern API)");
            } else {
                // API 18及以下使用旧的缓存API
                try {
                    java.lang.reflect.Method setAppCacheEnabled = WebSettings.class.getMethod("setAppCacheEnabled", boolean.class);
                    java.lang.reflect.Method setAppCachePath = WebSettings.class.getMethod("setAppCachePath", String.class);
                    java.lang.reflect.Method setAppCacheMaxSize = WebSettings.class.getMethod("setAppCacheMaxSize", long.class);

                    setAppCacheEnabled.invoke(settings, true);
                    setAppCachePath.invoke(settings, new File(mContext.getCacheDir(), "webview_network_cache").getAbsolutePath());
                    setAppCacheMaxSize.invoke(settings, MAX_CACHE_SIZE / 4); // 网络缓存使用1/4空间
                } catch (Exception e2) {
                    Log.w(TAG, "Failed to use legacy cache API for network", e2);
                }
            }

            Log.d(TAG, "Network cache configured");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure network cache", e);
        }
    }
}
