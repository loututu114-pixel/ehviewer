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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 网络连接检测器
 * 检测网络状态并判断是否被GFW影响
 */
public class NetworkDetector {

    private static final String TAG = "NetworkDetector";

    private static NetworkDetector sInstance;

    private Context mContext;
    private ExecutorService mExecutorService;
    private NetworkStatus mCurrentStatus = NetworkStatus.UNKNOWN;

    // GFW检测用的测试URL
    private static final String[] GFW_TEST_URLS = {
        "https://www.google.com",
        "https://www.youtube.com",
        "https://www.twitter.com",
        "https://www.facebook.com"
    };

    // 百度搜索URL
    public static final String BAIDU_SEARCH_URL = "https://www.baidu.com/s?wd=";

    // 超时时间
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    /**
     * 网络状态枚举
     */
    public enum NetworkStatus {
        UNKNOWN,        // 未知状态
        CONNECTED,      // 正常连接
        NO_NETWORK,     // 无网络连接
        GFW_BLOCKED,    // 被GFW屏蔽
        NETWORK_ERROR   // 网络错误
    }

    /**
     * 检测结果回调
     */
    public interface NetworkCallback {
        void onNetworkStatusDetected(NetworkStatus status);
        void onDetectionFailed(String error);
    }

    /**
     * 获取单例实例
     */
    public static synchronized NetworkDetector getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NetworkDetector(context.getApplicationContext());
        }
        return sInstance;
    }

    private NetworkDetector(Context context) {
        this.mContext = context;
        this.mExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 检测网络状态
     */
    public void detectNetworkStatus(NetworkCallback callback) {
        mExecutorService.execute(() -> {
            try {
                NetworkStatus status = detectNetworkInternal();
                mCurrentStatus = status;

                if (callback != null) {
                    callback.onNetworkStatusDetected(status);
                }

                Log.d(TAG, "Network status detected: " + status);

            } catch (Exception e) {
                Log.e(TAG, "Failed to detect network status", e);
                if (callback != null) {
                    callback.onDetectionFailed(e.getMessage());
                }
            }
        });
    }

    /**
     * 内部网络检测逻辑
     */
    private NetworkStatus detectNetworkInternal() throws Exception {
        // 首先检查基础网络连接
        if (!isNetworkAvailable()) {
            return NetworkStatus.NO_NETWORK;
        }

        // 尝试连接百度（国内网络测试）
        if (testConnection("https://www.baidu.com")) {
            // 百度可访问，说明是国内网络
            return NetworkStatus.CONNECTED;
        }

        // 百度不可访问，尝试连接Google等国外网站
        boolean canAccessForeign = false;
        for (String url : GFW_TEST_URLS) {
            if (testConnection(url)) {
                canAccessForeign = true;
                break;
            }
        }

        if (canAccessForeign) {
            // 可以访问国外网站，但百度不可访问（可能是网络问题）
            return NetworkStatus.NETWORK_ERROR;
        } else {
            // 无法访问国外网站，很可能是被GFW屏蔽
            return NetworkStatus.GFW_BLOCKED;
        }
    }

    /**
     * 测试连接到指定URL
     */
    private boolean testConnection(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestMethod("HEAD"); // 只获取头部信息
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;

        } catch (IOException e) {
            Log.d(TAG, "Connection test failed for " + urlString + ": " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 检查是否有网络连接
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    /**
     * 获取当前网络状态
     */
    public NetworkStatus getCurrentStatus() {
        return mCurrentStatus;
    }

    /**
     * 判断是否需要切换到百度搜索
     */
    public boolean shouldUseBaiduSearch() {
        return mCurrentStatus == NetworkStatus.GFW_BLOCKED;
    }

    /**
     * 获取推荐的搜索引擎URL
     */
    public String getRecommendedSearchUrl(String query) {
        if (shouldUseBaiduSearch()) {
            // 使用百度搜索
            return BAIDU_SEARCH_URL + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            // 使用Google搜索或其他默认搜索引擎
            return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取搜索引擎名称
     */
    public String getCurrentSearchEngineName() {
        if (shouldUseBaiduSearch()) {
            return "百度搜索";
        } else {
            return "Google搜索";
        }
    }

    /**
     * 重新检测网络状态
     */
    public void refreshNetworkStatus(NetworkCallback callback) {
        detectNetworkStatus(callback);
    }

    /**
     * 获取网络状态描述
     */
    public String getStatusDescription(NetworkStatus status) {
        switch (status) {
            case CONNECTED:
                return "网络连接正常";
            case NO_NETWORK:
                return "无网络连接";
            case GFW_BLOCKED:
                return "网络可能被防火墙限制，建议使用百度搜索";
            case NETWORK_ERROR:
                return "网络连接异常";
            case UNKNOWN:
            default:
                return "网络状态未知";
        }
    }

    /**
     * 清理资源
     */
    public void destroy() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
        Log.d(TAG, "NetworkDetector destroyed");
    }

    /**
     * 网络状态监听器
     */
    public interface NetworkStatusListener {
        void onNetworkStatusChanged(NetworkStatus oldStatus, NetworkStatus newStatus);
    }
}
