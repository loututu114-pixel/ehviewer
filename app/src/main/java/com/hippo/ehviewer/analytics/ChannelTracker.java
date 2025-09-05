/*
 * Copyright 2025 EhViewer
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

package com.hippo.ehviewer.analytics;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 渠道统计SDK
 * 支持任意渠道号配置，具有容错机制
 * 默认渠道号：0000
 */
public class ChannelTracker {

    private static final String TAG = "ChannelTracker";
    private static final String API_BASE_URL = "https://qudao.eh-viewer.com/api";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_CHANNEL = "0000";
    private static final int DEFAULT_SOFTWARE_ID = 1;
    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    
    private static ChannelTracker instance;
    private final Context context;
    private final OkHttpClient httpClient;
    private final String channelCode;
    private final int softwareId;
    private boolean initialized = false;

    private ChannelTracker(@NonNull Context context, @NonNull String channelCode, int softwareId) {
        this.context = context.getApplicationContext();
        this.channelCode = channelCode;
        this.softwareId = softwareId;
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 初始化渠道统计SDK
     * @param context 应用上下文
     * @param channelCode 渠道号，传入null使用默认值0000
     */
    public static synchronized void initialize(@NonNull Context context, @Nullable String channelCode) {
        if (instance == null) {
            String finalChannelCode = channelCode != null ? channelCode : DEFAULT_CHANNEL;
            instance = new ChannelTracker(context, finalChannelCode, DEFAULT_SOFTWARE_ID);
            instance.init();
        }
    }

    /**
     * 获取单例实例
     */
    public static ChannelTracker getInstance() {
        return instance;
    }

    private void init() {
        if (initialized) return;
        initialized = true;
        
        try {
            Log.d(TAG, "ChannelTracker initialized - Channel: " + channelCode + ", SoftwareId: " + softwareId);
            
            // 自动发送安装统计
            trackInstallSafe();
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize ChannelTracker", e);
        }
    }

    /**
     * 获取设备信息
     */
    private JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.put("os", "Android " + Build.VERSION.RELEASE);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("brand", Build.BRAND);
            deviceInfo.put("sdk", Build.VERSION.SDK_INT);
        } catch (Exception e) {
            Log.w(TAG, "Failed to collect device info", e);
        }
        return deviceInfo;
    }

    /**
     * 获取ISO格式的时间戳
     */
    private String getCurrentISOTime() {
        synchronized (ISO_DATE_FORMAT) {
            return ISO_DATE_FORMAT.format(new Date());
        }
    }

    /**
     * 发送统计数据到API
     */
    private void sendRequest(@NonNull String endpoint, @NonNull JSONObject data, @Nullable TrackCallback callback) {
        try {
            String url = API_BASE_URL + endpoint;
            String jsonString = data.toJSONString();
            
            RequestBody body = RequestBody.create(JSON_TYPE, jsonString);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.w(TAG, "Request failed: " + endpoint, e);
                    if (callback != null) {
                        callback.onResult(false, e.getMessage());
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        boolean success = response.isSuccessful();
                        String message = success ? "success" : "HTTP " + response.code();
                        
                        if (success) {
                            Log.d(TAG, "Request successful: " + endpoint);
                        } else {
                            Log.w(TAG, "Request failed with HTTP " + response.code() + ": " + endpoint);
                        }
                        
                        if (callback != null) {
                            callback.onResult(success, message);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to process response", e);
                        if (callback != null) {
                            callback.onResult(false, e.getMessage());
                        }
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to send request: " + endpoint, e);
            if (callback != null) {
                callback.onResult(false, e.getMessage());
            }
        }
    }

    /**
     * 安全的安装统计（不会导致程序崩溃）
     */
    public void trackInstallSafe() {
        try {
            trackInstall(null);
        } catch (Exception e) {
            Log.w(TAG, "Install tracking failed safely", e);
        }
    }

    /**
     * 跟踪安装事件
     */
    public void trackInstall(@Nullable TrackCallback callback) {
        try {
            if (!initialized) {
                Log.w(TAG, "ChannelTracker not initialized");
                if (callback != null) {
                    callback.onResult(false, "Not initialized");
                }
                return;
            }

            JSONObject data = new JSONObject();
            data.put("channelCode", channelCode);
            data.put("softwareId", softwareId);
            data.put("installTime", getCurrentISOTime());
            data.put("deviceInfo", getDeviceInfo());

            sendRequest("/stats/install", data, callback);
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to track install", e);
            if (callback != null) {
                callback.onResult(false, e.getMessage());
            }
        }
    }

    /**
     * 跟踪下载事件
     */
    public void trackDownload(@Nullable TrackCallback callback) {
        try {
            if (!initialized) {
                Log.w(TAG, "ChannelTracker not initialized");
                if (callback != null) {
                    callback.onResult(false, "Not initialized");
                }
                return;
            }

            JSONObject data = new JSONObject();
            data.put("channelCode", channelCode);
            data.put("softwareId", softwareId);
            data.put("downloadTime", getCurrentISOTime());
            data.put("deviceInfo", getDeviceInfo());

            sendRequest("/stats/download", data, callback);
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to track download", e);
            if (callback != null) {
                callback.onResult(false, e.getMessage());
            }
        }
    }

    /**
     * 跟踪激活事件（最重要的统计，会产生收益）
     */
    public void trackActivate(@Nullable String licenseKey, @Nullable TrackCallback callback) {
        try {
            if (!initialized) {
                Log.w(TAG, "ChannelTracker not initialized");
                if (callback != null) {
                    callback.onResult(false, "Not initialized");
                }
                return;
            }

            JSONObject data = new JSONObject();
            data.put("channelCode", channelCode);
            data.put("softwareId", softwareId);
            data.put("activateTime", getCurrentISOTime());
            if (licenseKey != null) {
                data.put("licenseKey", licenseKey);
            }
            data.put("deviceInfo", getDeviceInfo());

            sendRequest("/stats/activate", data, callback);
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to track activate", e);
            if (callback != null) {
                callback.onResult(false, e.getMessage());
            }
        }
    }

    /**
     * 获取当前渠道号
     */
    public String getChannelCode() {
        return channelCode;
    }

    /**
     * 获取软件ID
     */
    public int getSoftwareId() {
        return softwareId;
    }

    /**
     * 统计回调接口
     */
    public interface TrackCallback {
        void onResult(boolean success, @Nullable String message);
    }
}