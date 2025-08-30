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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.R;
import com.tencent.smtt.sdk.QbSdk;

/**
 * 腾讯X5浏览器管理器
 * 负责X5 SDK的初始化和WebView的创建管理
 */
public class X5WebViewManager {

    private static final String TAG = "X5WebViewManager";

    private static X5WebViewManager sInstance;
    private static boolean sIsX5Initialized = false;
    private static boolean sIsX5Available = false;

    // X5初始化回调
    private static QbSdk.PreInitCallback sPreInitCallback;

    /**
     * 获取单例实例
     */
    public static synchronized X5WebViewManager getInstance() {
        if (sInstance == null) {
            sInstance = new X5WebViewManager();
        }
        return sInstance;
    }

    /**
     * 初始化腾讯X5浏览器SDK
     * 应该在Application的onCreate中调用
     */
    public void initX5(Context context) {
        if (sIsX5Initialized) {
            Log.d(TAG, "X5 already initialized");
            return;
        }

        Log.i(TAG, "Initializing X5 WebView SDK...");

        // 设置X5初始化回调
        sPreInitCallback = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean success) {
                Log.i(TAG, "X5 WebView init finished, success: " + success);
                sIsX5Initialized = true;
                sIsX5Available = success;

                if (success) {
                    // X5初始化成功，可以使用X5 WebView
                    Log.i(TAG, "X5 WebView is available for use");
                } else {
                    // X5初始化失败，回退到系统WebView
                    Log.w(TAG, "X5 WebView init failed, fallback to system WebView");
                }
            }

            @Override
            public void onCoreInitFinished() {
                Log.i(TAG, "X5 core init finished");
            }
        };

        // 初始化X5 SDK
        QbSdk.initX5Environment(context, sPreInitCallback);

        // 设置一些X5配置
        QbSdk.setDownloadWithoutWifi(true); // 允许非WiFi网络下载X5内核

        // 设置X5日志级别（如果支持）
        try {
            // 一些X5版本可能不支持日志设置，这里使用try-catch
            Log.i(TAG, "X5 SDK initialized with basic configuration");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set X5 log configuration", e);
        }
    }

    /**
     * 创建WebView（优先使用X5 WebView）
     */
    @NonNull
    public android.webkit.WebView createWebView(Context context) {
        android.webkit.WebView webView;

        if (sIsX5Available) {
            try {
                // 创建X5 WebView包装器
                com.tencent.smtt.sdk.WebView x5WebView = new com.tencent.smtt.sdk.WebView(context);
                // 创建系统WebView作为包装器
                webView = new android.webkit.WebView(context);
                // 将X5 WebView存储在tag中供后续使用
                webView.setTag(R.id.x5_webview_tag, x5WebView);
                Log.d(TAG, "Created X5 WebView wrapper");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create X5 WebView, fallback to system WebView", e);
                // 回退到系统WebView
                webView = new android.webkit.WebView(context);
            }
        } else {
            // 使用系统WebView
            webView = new android.webkit.WebView(context);
            Log.d(TAG, "Created system WebView (X5 not available)");
        }

        return webView;
    }

    /**
     * 检查X5是否可用
     */
    public boolean isX5Available() {
        return sIsX5Available;
    }

    /**
     * 检查X5是否已初始化
     */
    public boolean isX5Initialized() {
        return sIsX5Initialized;
    }

    /**
     * 获取X5版本信息
     */
    @Nullable
    public String getX5Version() {
        if (sIsX5Available) {
            try {
                // 尝试获取X5版本信息
                int version = QbSdk.getTbsVersion(null); // 传入null作为context
                return "TBS " + version;
            } catch (Exception e) {
                Log.e(TAG, "Failed to get X5 version", e);
                return "TBS Unknown";
            }
        }
        return null;
    }

    /**
     * 获取X5内核版本
     */
    public int getX5CoreVersion() {
        if (sIsX5Available) {
            try {
                return QbSdk.getTbsVersion(null); // 传入null作为context
            } catch (Exception e) {
                Log.e(TAG, "Failed to get X5 core version", e);
            }
        }
        return 0;
    }

    /**
     * 清理X5缓存
     */
    public void clearX5Cache(Context context) {
        try {
            QbSdk.clearAllWebViewCache(context, true);
            Log.d(TAG, "X5 cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear X5 cache", e);
        }
    }

    /**
     * 重置X5环境
     */
    public void resetX5Environment(Context context) {
        try {
            QbSdk.reset(context);
            sIsX5Initialized = false;
            sIsX5Available = false;
            Log.d(TAG, "X5 environment reset");
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset X5 environment", e);
        }
    }

    /**
     * 检查X5是否需要下载
     */
    public boolean isX5NeedDownload() {
        try {
            return QbSdk.getTbsVersion(null) == 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check X5 download status", e);
            return true;
        }
    }

    /**
     * 获取X5下载状态
     */
    public String getX5DownloadStatus() {
        try {
            int tbsVersion = QbSdk.getTbsVersion(null);
            if (tbsVersion == 0) {
                return "未下载";
            } else {
                return "版本: " + tbsVersion;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get X5 download status", e);
            return "未知";
        }
    }

    /**
     * 获取X5初始化状态详情
     */
    public String getX5InitStatusDetail() {
        StringBuilder status = new StringBuilder();
        status.append("X5已初始化: ").append(sIsX5Initialized).append("\n");
        status.append("X5可用: ").append(sIsX5Available).append("\n");
        status.append("X5版本: ").append(getX5Version() != null ? getX5Version() : "未知").append("\n");
        status.append("X5核心版本: ").append(getX5CoreVersion()).append("\n");
        status.append("下载状态: ").append(getX5DownloadStatus());

        return status.toString();
    }

    /**
     * 检查X5是否可以加速
     */
    public boolean canX5Accelerate() {
        try {
            return sIsX5Available && QbSdk.getTbsVersion(null) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check X5 acceleration capability", e);
            return false;
        }
    }

    /**
     * 获取推荐的WebView类型
     */
    public String getRecommendedWebViewType() {
        if (sIsX5Available) {
            return "推荐使用腾讯X5 WebView (性能更佳)";
        } else if (sIsX5Initialized) {
            return "使用系统WebView (X5初始化失败)";
        } else {
            return "使用系统WebView (X5未初始化)";
        }
    }

    /**
     * 获取WebView类型描述
     */
    @NonNull
    public String getWebViewTypeDescription() {
        if (sIsX5Available) {
            return "腾讯X5 WebView (版本: " + getX5CoreVersion() + ")";
        } else if (sIsX5Initialized) {
            return "系统WebView (X5初始化失败)";
        } else {
            return "系统WebView (X5未初始化)";
        }
    }
}
