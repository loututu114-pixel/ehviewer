/*
 * Copyright 2025 EhViewer Contributors
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

import com.hippo.ehviewer.Settings;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 网络超时增强器
 * 
 * 专门解决网络请求超时导致的加载卡死问题
 * 提供智能超时配置和网络状态适配
 */
public class NetworkTimeoutEnhancer {
    
    private static final String TAG = "NetworkTimeoutEnhancer";
    
    // 基础超时配置（秒）
    private static final int WIFI_CONNECT_TIMEOUT = 15;     // WiFi连接超时
    private static final int WIFI_READ_TIMEOUT = 60;        // WiFi读取超时
    private static final int WIFI_WRITE_TIMEOUT = 30;       // WiFi写入超时
    
    private static final int MOBILE_CONNECT_TIMEOUT = 20;   // 移动网络连接超时
    private static final int MOBILE_READ_TIMEOUT = 90;      // 移动网络读取超时
    private static final int MOBILE_WRITE_TIMEOUT = 45;     // 移动网络写入超时
    
    private static final int SLOW_CONNECT_TIMEOUT = 30;     // 慢速网络连接超时
    private static final int SLOW_READ_TIMEOUT = 120;       // 慢速网络读取超时
    private static final int SLOW_WRITE_TIMEOUT = 60;       // 慢速网络写入超时
    
    /**
     * 获取增强的HTTP客户端
     * 根据网络状况自动配置超时参数
     */
    public static OkHttpClient getEnhancedClient(Context context, OkHttpClient baseClient) {
        NetworkType networkType = detectNetworkType(context);
        
        OkHttpClient.Builder builder = baseClient.newBuilder();
        
        // 根据网络类型配置超时
        switch (networkType) {
            case WIFI:
                builder.connectTimeout(WIFI_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                       .readTimeout(WIFI_READ_TIMEOUT, TimeUnit.SECONDS)
                       .writeTimeout(WIFI_WRITE_TIMEOUT, TimeUnit.SECONDS);
                break;
                
            case MOBILE_FAST:
                builder.connectTimeout(MOBILE_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                       .readTimeout(MOBILE_READ_TIMEOUT, TimeUnit.SECONDS)
                       .writeTimeout(MOBILE_WRITE_TIMEOUT, TimeUnit.SECONDS);
                break;
                
            case MOBILE_SLOW:
            case UNKNOWN:
                builder.connectTimeout(SLOW_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                       .readTimeout(SLOW_READ_TIMEOUT, TimeUnit.SECONDS)
                       .writeTimeout(SLOW_WRITE_TIMEOUT, TimeUnit.SECONDS);
                break;
        }
        
        // 添加超时监控拦截器
        builder.addInterceptor(new TimeoutMonitorInterceptor());
        
        // 添加重试拦截器
        builder.addInterceptor(new SmartRetryInterceptor(context));
        
        Log.d(TAG, String.format("Enhanced HTTP client created for %s network", networkType));
        
        return builder.build();
    }
    
    /**
     * 为SpiderInfo请求创建专用客户端
     */
    public static OkHttpClient getSpiderInfoClient(Context context, OkHttpClient baseClient) {
        NetworkType networkType = detectNetworkType(context);
        
        int connectTimeout, readTimeout;
        switch (networkType) {
            case WIFI:
                connectTimeout = 20; readTimeout = 45;
                break;
            case MOBILE_FAST:
                connectTimeout = 30; readTimeout = 60;
                break;
            default:
                connectTimeout = 40; readTimeout = 90;
                break;
        }
        
        return baseClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new SpiderInfoRetryInterceptor())
                .build();
    }
    
    /**
     * 为pToken请求创建专用客户端
     */
    public static OkHttpClient getPTokenClient(Context context, OkHttpClient baseClient) {
        NetworkType networkType = detectNetworkType(context);
        
        int connectTimeout, readTimeout;
        switch (networkType) {
            case WIFI:
                connectTimeout = 15; readTimeout = 30;
                break;
            case MOBILE_FAST:
                connectTimeout = 20; readTimeout = 45;
                break;
            default:
                connectTimeout = 25; readTimeout = 60;
                break;
        }
        
        return baseClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new PTokenRetryInterceptor())
                .build();
    }
    
    /**
     * 检测网络类型
     */
    private static NetworkType detectNetworkType(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return NetworkType.UNKNOWN;
            
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                return NetworkType.UNKNOWN;
            }
            
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return NetworkType.WIFI;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // 检测移动网络速度
                int subtype = activeNetwork.getSubtype();
                if (isFastMobileNetwork(subtype)) {
                    return NetworkType.MOBILE_FAST;
                } else {
                    return NetworkType.MOBILE_SLOW;
                }
            }
            
            return NetworkType.UNKNOWN;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to detect network type", e);
            return NetworkType.UNKNOWN;
        }
    }
    
    /**
     * 判断是否为快速移动网络
     */
    private static boolean isFastMobileNetwork(int subtype) {
        switch (subtype) {
            case android.telephony.TelephonyManager.NETWORK_TYPE_LTE:     // 4G
            case android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP:   // 3.5G
            case android.telephony.TelephonyManager.NETWORK_TYPE_HSPA:    // 3.5G
            case android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA:   // 3.5G
            case android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA:   // 3.5G
            case android.telephony.TelephonyManager.NETWORK_TYPE_UMTS:    // 3G
            case android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0:  // 3G
            case android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A:  // 3G
            case android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B:  // 3G
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 网络类型枚举
     */
    private enum NetworkType {
        WIFI,
        MOBILE_FAST,
        MOBILE_SLOW,
        UNKNOWN
    }
    
    /**
     * 超时监控拦截器
     */
    private static class TimeoutMonitorInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws java.io.IOException {
            Request request = chain.request();
            long startTime = System.currentTimeMillis();
            
            try {
                Response response = chain.proceed(request);
                long duration = System.currentTimeMillis() - startTime;
                
                if (duration > 30000) { // 超过30秒的请求
                    Log.w(TAG, String.format("Slow request detected: %s took %dms", 
                        request.url(), duration));
                }
                
                return response;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                Log.e(TAG, String.format("Request failed: %s after %dms - %s", 
                    request.url(), duration, e.getMessage()));
                throw e;
            }
        }
    }
    
    /**
     * 智能重试拦截器
     */
    private static class SmartRetryInterceptor implements Interceptor {
        private final Context mContext;
        
        SmartRetryInterceptor(Context context) {
            mContext = context;
        }
        
        @Override
        public Response intercept(Chain chain) throws java.io.IOException {
            Request request = chain.request();
            Response response = null;
            java.io.IOException exception = null;
            
            // 根据网络状况决定重试次数
            NetworkType networkType = detectNetworkType(mContext);
            int maxRetries = (networkType == NetworkType.WIFI) ? 2 : 3;
            
            for (int retry = 0; retry <= maxRetries; retry++) {
                try {
                    response = chain.proceed(request);
                    
                    // 检查响应状态
                    if (response.isSuccessful()) {
                        return response;
                    } else if (response.code() == 503 || response.code() == 504) {
                        // 服务器繁忙，值得重试
                        if (retry < maxRetries) {
                            response.close();
                            try {
                                Thread.sleep(1000 * (retry + 1)); // 指数退避
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            continue;
                        }
                    }
                    
                    return response;
                    
                } catch (java.net.SocketTimeoutException e) {
                    exception = e;
                    if (retry < maxRetries) {
                        Log.w(TAG, String.format("Socket timeout, retry %d/%d: %s", 
                            retry + 1, maxRetries, request.url()));
                        try {
                            Thread.sleep(2000 * (retry + 1)); // 超时重试间隔更长
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (java.io.IOException e) {
                    exception = e;
                    if (retry < maxRetries && isRetryableException(e)) {
                        Log.w(TAG, String.format("Retryable exception, retry %d/%d: %s", 
                            retry + 1, maxRetries, e.getMessage()));
                        try {
                            Thread.sleep(1500 * (retry + 1));
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            
            if (exception != null) {
                throw exception;
            }
            
            return response;
        }
        
        private boolean isRetryableException(java.io.IOException e) {
            String message = e.getMessage();
            if (message == null) return false;
            
            return message.contains("Connection reset") ||
                   message.contains("Connection refused") ||
                   message.contains("timeout") ||
                   message.contains("No route to host");
        }
    }
    
    /**
     * SpiderInfo重试拦截器
     */
    private static class SpiderInfoRetryInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws java.io.IOException {
            Request request = chain.request();
            
            for (int retry = 0; retry < 3; retry++) {
                try {
                    Response response = chain.proceed(request);
                    
                    if (response.isSuccessful()) {
                        String body = response.peekBody(1024).string();
                        // 检查响应内容是否有效
                        if (body.contains("gid") || body.contains("token")) {
                            return response;
                        } else if (retry < 2) {
                            response.close();
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            continue;
                        }
                    }
                    
                    return response;
                    
                } catch (Exception e) {
                    if (retry < 2) {
                        Log.w(TAG, String.format("SpiderInfo retry %d: %s", retry + 1, e.getMessage()));
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        throw new java.io.IOException("SpiderInfo request failed after retries", e);
                    }
                }
            }
            
            throw new java.io.IOException("SpiderInfo request failed");
        }
    }
    
    /**
     * pToken重试拦截器
     */
    private static class PTokenRetryInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws java.io.IOException {
            Request request = chain.request();
            
            for (int retry = 0; retry < 5; retry++) { // pToken重试更多次
                try {
                    Response response = chain.proceed(request);
                    
                    if (response.isSuccessful()) {
                        return response;
                    } else if (response.code() == 509) {
                        // 509错误特殊处理，延长等待时间
                        response.close();
                        try {
                            Thread.sleep(5000 + retry * 2000);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    } else if (retry < 4) {
                        response.close();
                        Thread.sleep(1000 + retry * 500);
                        continue;
                    }
                    
                    return response;
                    
                } catch (Exception e) {
                    if (retry < 4) {
                        Log.w(TAG, String.format("pToken retry %d: %s", retry + 1, e.getMessage()));
                        try {
                            Thread.sleep(2000 + retry * 1000);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        throw new java.io.IOException("pToken request failed after retries", e);
                    }
                }
            }
            
            throw new java.io.IOException("pToken request failed");
        }
    }
}