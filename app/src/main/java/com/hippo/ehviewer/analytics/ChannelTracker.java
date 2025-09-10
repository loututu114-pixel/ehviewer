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
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
    
    // 重试机制配置
    private static final int MAX_RETRY_COUNT = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000; // 1秒
    private static final int MAX_CACHED_REQUESTS = 50;
    private static final String PENDING_REQUESTS_PREFS = "channel_tracker_pending";
    private static final String KEY_PENDING_REQUESTS = "pending_requests";
    
    // 频率控制机制 - 替换防重复验证
    private static final String FREQUENCY_PREFS = "channel_tracker_frequency";
    private static final String KEY_INSTALL_LAST_TIME = "install_last_time";
    private static final String KEY_DOWNLOAD_LAST_TIME = "download_last_time";
    private static final String KEY_ACTIVATE_LAST_TIME = "activate_last_time";
    private static final long FREQUENCY_LIMIT_MS = 60 * 1000; // 1分钟 = 60秒

    // 记录首次真实使用时间的相关常量
    private static final String INSTALL_PREFS = "channel_tracker_install";
    private static final String KEY_FIRST_REAL_USE_TIME = "first_real_use_time";
    
    private static ChannelTracker instance;
    private final Context context;
    private final OkHttpClient httpClient;
    private final String channelCode;
    private final int softwareId;
    private boolean initialized = false;
    private final Handler mainHandler;
    private final List<PendingRequest> pendingRequests = new ArrayList<>();

    private ChannelTracker(@NonNull Context context, @NonNull String channelCode, int softwareId) {
        this.context = context.getApplicationContext();
        this.channelCode = channelCode;
        this.softwareId = softwareId;
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)  // 增加超时时间适应弱网
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .build();
        
        this.mainHandler = new Handler(Looper.getMainLooper());
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
            
            // 加载缓存的待发送请求
            loadPendingRequests();
            
            // 尝试发送缓存的请求
            if (!pendingRequests.isEmpty()) {
                Log.d(TAG, "Found " + pendingRequests.size() + " pending requests, attempting to send");
                retryPendingRequests();
            }
            
            // 移除自动发送安装统计，改为手动触发确保用户真实使用
            // trackInstallSafe(); // 注释掉自动发送
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
     * 检查网络连接状态
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking network status", e);
        }
        return true; // 默认假设网络可用，避免阻止请求
    }
    
    /**
     * 待发送请求的数据结构
     */
    private static class PendingRequest {
        String endpoint;
        JSONObject data;
        TrackCallback callback;
        int retryCount;
        long lastAttemptTime;
        
        PendingRequest(String endpoint, JSONObject data, TrackCallback callback) {
            this.endpoint = endpoint;
            this.data = data;
            this.callback = callback;
            this.retryCount = 0;
            this.lastAttemptTime = System.currentTimeMillis();
        }
        
        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("endpoint", endpoint);
            json.put("data", data);
            json.put("retryCount", retryCount);
            json.put("lastAttemptTime", lastAttemptTime);
            return json;
        }
        
        static PendingRequest fromJson(JSONObject json) {
            String endpoint = json.getString("endpoint");
            JSONObject data = json.getJSONObject("data");
            PendingRequest request = new PendingRequest(endpoint, data, null); // callback不缓存
            request.retryCount = json.getIntValue("retryCount");
            request.lastAttemptTime = json.getLongValue("lastAttemptTime");
            return request;
        }
    }
    
    /**
     * 加载缓存的待发送请求
     */
    private void loadPendingRequests() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PENDING_REQUESTS_PREFS, Context.MODE_PRIVATE);
            String jsonString = prefs.getString(KEY_PENDING_REQUESTS, "[]");
            
            com.alibaba.fastjson.JSONArray jsonArray = JSON.parseArray(jsonString);
            pendingRequests.clear();
            
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject requestJson = jsonArray.getJSONObject(i);
                PendingRequest request = PendingRequest.fromJson(requestJson);
                
                // 过滤过期请求(超过24小时)
                if (System.currentTimeMillis() - request.lastAttemptTime < 24 * 60 * 60 * 1000) {
                    pendingRequests.add(request);
                }
            }
            
            Log.d(TAG, "Loaded " + pendingRequests.size() + " pending requests from cache");
            
        } catch (Exception e) {
            Log.w(TAG, "Error loading pending requests", e);
        }
    }
    
    /**
     * 保存待发送请求到缓存
     */
    private void savePendingRequests() {
        try {
            // 限制缓存数量，避免过度占用存储空间
            while (pendingRequests.size() > MAX_CACHED_REQUESTS) {
                pendingRequests.remove(0); // 移除最老的请求
            }
            
            com.alibaba.fastjson.JSONArray jsonArray = new com.alibaba.fastjson.JSONArray();
            for (PendingRequest request : pendingRequests) {
                jsonArray.add(request.toJson());
            }
            
            SharedPreferences prefs = context.getSharedPreferences(PENDING_REQUESTS_PREFS, Context.MODE_PRIVATE);
            prefs.edit()
                .putString(KEY_PENDING_REQUESTS, jsonArray.toJSONString())
                .apply();
                
            Log.d(TAG, "Saved " + pendingRequests.size() + " pending requests to cache");
            
        } catch (Exception e) {
            Log.w(TAG, "Error saving pending requests", e);
        }
    }
    
    /**
     * 重试所有待发送请求
     */
    private void retryPendingRequests() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Network not available, skipping retry");
            return;
        }
        
        Iterator<PendingRequest> iterator = pendingRequests.iterator();
        while (iterator.hasNext()) {
            PendingRequest request = iterator.next();
            
            if (request.retryCount >= MAX_RETRY_COUNT) {
                Log.w(TAG, "Request exceeded max retry count, removing: " + request.endpoint);
                iterator.remove();
                continue;
            }
            
            // 发送请求
            sendRequestWithRetry(request, iterator);
        }
        
        // 更新缓存
        savePendingRequests();
    }
    
    /**
     * 发送带重试机制的请求
     */
    private void sendRequestWithRetry(@NonNull PendingRequest pendingRequest, @Nullable Iterator<PendingRequest> iterator) {
        try {
            String url = API_BASE_URL + pendingRequest.endpoint;
            String jsonString = pendingRequest.data.toJSONString();
            
            RequestBody body = RequestBody.create(JSON_TYPE, jsonString);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    handleRequestFailure(pendingRequest, e.getMessage(), iterator);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        boolean success = response.isSuccessful();
                        String message = success ? "success" : "HTTP " + response.code();
                        
                        if (success) {
                            handleRequestSuccess(pendingRequest, iterator);
                        } else {
                            handleRequestFailure(pendingRequest, message, iterator);
                        }
                        
                    } catch (Exception e) {
                        handleRequestFailure(pendingRequest, e.getMessage(), iterator);
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (Exception e) {
            handleRequestFailure(pendingRequest, e.getMessage(), iterator);
        }
    }
    
    /**
     * 处理请求成功
     */
    private void handleRequestSuccess(@NonNull PendingRequest pendingRequest, @Nullable Iterator<PendingRequest> iterator) {
        Log.d(TAG, "Request successful: " + pendingRequest.endpoint);
        
        // 从待发送列表中移除
        if (iterator != null) {
            iterator.remove();
        } else {
            pendingRequests.remove(pendingRequest);
        }
        
        // 通知回调
        if (pendingRequest.callback != null) {
            pendingRequest.callback.onResult(true, "success");
        }
        
        // 更新缓存
        savePendingRequests();
    }
    
    /**
     * 处理请求失败
     */
    private void handleRequestFailure(@NonNull PendingRequest pendingRequest, @NonNull String message, @Nullable Iterator<PendingRequest> iterator) {
        pendingRequest.retryCount++;
        pendingRequest.lastAttemptTime = System.currentTimeMillis();
        
        Log.w(TAG, "Request failed (retry " + pendingRequest.retryCount + "/" + MAX_RETRY_COUNT + "): " + 
                pendingRequest.endpoint + " - " + message);
        
        if (pendingRequest.retryCount >= MAX_RETRY_COUNT) {
            // 达到最大重试次数，移除请求
            if (iterator != null) {
                iterator.remove();
            } else {
                pendingRequests.remove(pendingRequest);
            }
            
            // 通知回调失败
            if (pendingRequest.callback != null) {
                pendingRequest.callback.onResult(false, "Max retry exceeded: " + message);
            }
            
            Log.w(TAG, "Request permanently failed after " + MAX_RETRY_COUNT + " retries: " + pendingRequest.endpoint);
        } else {
            // 安排下次重试
            long retryDelay = INITIAL_RETRY_DELAY_MS * (1L << (pendingRequest.retryCount - 1)); // 指数退避
            
            mainHandler.postDelayed(() -> {
                if (isNetworkAvailable()) {
                    sendRequestWithRetry(pendingRequest, null);
                } else {
                    Log.d(TAG, "Network still unavailable, will retry later");
                }
            }, retryDelay);
            
            Log.d(TAG, "Scheduled retry in " + retryDelay + "ms for: " + pendingRequest.endpoint);
        }
        
        // 更新缓存
        savePendingRequests();
    }

    /**
     * 发送统计数据到API（带重试机制）
     */
    private void sendRequest(@NonNull String endpoint, @NonNull JSONObject data, @Nullable TrackCallback callback) {
        try {
            if (!initialized) {
                Log.w(TAG, "ChannelTracker not initialized");
                if (callback != null) {
                    callback.onResult(false, "Not initialized");
                }
                return;
            }
            
            // 创建待发送请求
            PendingRequest pendingRequest = new PendingRequest(endpoint, data, callback);
            
            // 检查网络连接
            if (!isNetworkAvailable()) {
                Log.d(TAG, "Network not available, caching request: " + endpoint);
                
                // 添加到待发送队列
                pendingRequests.add(pendingRequest);
                savePendingRequests();
                
                // 立即通知调用者请求已缓存（但不是失败）
                if (callback != null) {
                    callback.onResult(false, "Network unavailable, request cached for retry");
                }
                return;
            }
            
            // 网络可用，直接发送
            sendRequestWithRetry(pendingRequest, null);
            
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
     * 智能安装统计 - 确保用户真实使用后才统计
     * 同一设备只统计一次安装
     */
    public void trackRealInstallSafe() {
        try {
            trackRealInstall(null);
        } catch (Exception e) {
            Log.w(TAG, "Real install tracking failed safely", e);
        }
    }

    /**
     * 安全的下载统计（不会导致程序崩溃）
     */
    public void trackDownloadSafe() {
        try {
            trackDownload(null);
        } catch (Exception e) {
            Log.w(TAG, "Download tracking failed safely", e);
        }
    }

    /**
     * 安全的激活统计（不会导致程序崩溃）- 支持自定义许可证密钥
     */
    public void trackActivateSafe(@Nullable String licenseKey) {
        try {
            trackActivate(licenseKey, null);
        } catch (Exception e) {
            Log.w(TAG, "Activate tracking failed safely", e);
        }
    }

    /**
     * 跟踪安装事件 - 频率控制：1分钟内最多1次
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

            // 频率控制检查
            if (!canSendRequest(KEY_INSTALL_LAST_TIME)) {
                Log.d(TAG, "Install tracking skipped due to frequency limit");
                if (callback != null) {
                    callback.onResult(true, "Frequency limited");
                }
                return;
            }

            JSONObject data = new JSONObject();
            data.put("channelCode", channelCode);
            data.put("softwareId", softwareId);
            data.put("installTime", getCurrentISOTime());
            data.put("deviceInfo", getDeviceInfo());

            sendRequest("/stats/install", data, new TrackCallback() {
                @Override
                public void onResult(boolean success, String message) {
                    if (success) {
                        // 成功后更新频率控制时间
                        updateLastSendTime(KEY_INSTALL_LAST_TIME);
                        Log.d(TAG, "Install tracked successfully");
                    }

                    if (callback != null) {
                        callback.onResult(success, message);
                    }
                }
            });

        } catch (Exception e) {
            Log.w(TAG, "Failed to track install", e);
            if (callback != null) {
                callback.onResult(false, e.getMessage());
            }
        }
    }

    /**
     * 🌐 下载统计 - 浏览器成功访问网页时触发，频率控制：1分钟内最多1次
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

            // 频率控制检查
            if (!canSendRequest(KEY_DOWNLOAD_LAST_TIME)) {
                Log.d(TAG, "Download tracking skipped due to frequency limit");
                if (callback != null) {
                    callback.onResult(true, "Frequency limited");
                }
                return;
            }

            JSONObject data = new JSONObject();
            data.put("channelCode", channelCode);
            data.put("softwareId", softwareId);
            data.put("downloadTime", getCurrentISOTime());
            data.put("deviceInfo", getDeviceInfo());

            sendRequest("/stats/download", data, new TrackCallback() {
                @Override
                public void onResult(boolean success, String message) {
                    if (success) {
                        // 成功后更新频率控制时间
                        updateLastSendTime(KEY_DOWNLOAD_LAST_TIME);
                        Log.d(TAG, "Download tracked successfully");
                    }

                    if (callback != null) {
                        callback.onResult(success, message);
                    }
                }
            });

        } catch (Exception e) {
            Log.w(TAG, "Failed to track download", e);
            if (callback != null) {
                callback.onResult(false, e.getMessage());
            }
        }
    }

    /**
     * 🎯 激活统计（最重要的统计，会产生收益）- 每天软件打开超过10次时触发，频率控制：1分钟内最多1次
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

            // 频率控制检查
            if (!canSendRequest(KEY_ACTIVATE_LAST_TIME)) {
                Log.d(TAG, "Activate tracking skipped due to frequency limit");
                if (callback != null) {
                    callback.onResult(true, "Frequency limited");
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

            sendRequest("/stats/activate", data, new TrackCallback() {
                @Override
                public void onResult(boolean success, String message) {
                    if (success) {
                        // 成功后更新频率控制时间
                        updateLastSendTime(KEY_ACTIVATE_LAST_TIME);
                        Log.d(TAG, "Activate tracked successfully");
                    }

                    if (callback != null) {
                        callback.onResult(success, message);
                    }
                }
            });

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
     * 检查是否可以发送统计请求（频率控制）
     * @param statKey 统计类型键名
     * @return true表示可以发送，false表示需要等待
     */
    private boolean canSendRequest(String statKey) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(FREQUENCY_PREFS, Context.MODE_PRIVATE);
            long lastTime = prefs.getLong(statKey, 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastTime >= FREQUENCY_LIMIT_MS) {
                // 更新最后发送时间
                prefs.edit().putLong(statKey, currentTime).apply();
                Log.d(TAG, "Frequency check passed for " + statKey + ", lastTime: " + lastTime + ", currentTime: " + currentTime);
                return true;
            } else {
                long remainingTime = FREQUENCY_LIMIT_MS - (currentTime - lastTime);
                Log.d(TAG, "Frequency check failed for " + statKey + ", remaining time: " + remainingTime + "ms");
                return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking frequency for " + statKey, e);
            return true; // 出错时允许发送，避免影响统计
        }
    }

    /**
     * 更新统计的最后发送时间
     * @param statKey 统计类型键名
     */
    private void updateLastSendTime(String statKey) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(FREQUENCY_PREFS, Context.MODE_PRIVATE);
            prefs.edit().putLong(statKey, System.currentTimeMillis()).apply();
            Log.d(TAG, "Updated last send time for " + statKey);
        } catch (Exception e) {
            Log.w(TAG, "Error updating last send time for " + statKey, e);
        }
    }

    /**
     * 📱 安装统计 - 点击画廊任意按钮时触发，频率控制：1分钟内最多1次
     */
    public void trackRealInstall(@Nullable TrackCallback callback) {
        try {
            if (!initialized) {
                Log.w(TAG, "ChannelTracker not initialized");
                if (callback != null) {
                    callback.onResult(false, "Not initialized");
                }
                return;
            }

            // 频率控制检查
            if (!canSendRequest(KEY_INSTALL_LAST_TIME)) {
                Log.d(TAG, "Real install tracking skipped due to frequency limit");
                if (callback != null) {
                    callback.onResult(true, "Frequency limited");
                }
                return;
            }

            // 记录首次真实使用时间
            recordFirstRealUse();

            // 生成设备指纹
            String deviceId = generateDeviceFingerprint();

            JSONObject data = new JSONObject();
            data.put("channelCode", channelCode);
            data.put("softwareId", softwareId);
            data.put("installTime", getCurrentISOTime());
            data.put("deviceId", deviceId);  // 添加设备指纹
            data.put("realInstall", true);   // 标记为真实安装
            data.put("deviceInfo", getDeviceInfo());

            sendRequest("/stats/install", data, new TrackCallback() {
                @Override
                public void onResult(boolean success, String message) {
                    if (success) {
                        // 成功后更新频率控制时间
                        updateLastSendTime(KEY_INSTALL_LAST_TIME);
                        Log.d(TAG, "Real install tracked successfully");
                    }

                    if (callback != null) {
                        callback.onResult(success, message);
                    }
                }
            });

        } catch (Exception e) {
            Log.w(TAG, "Failed to track real install", e);
            if (callback != null) {
                callback.onResult(false, e.getMessage());
            }
        }
    }
    
    
    /**
     * 生成设备指纹 (用于防重复安装统计)
     */
    private String generateDeviceFingerprint() {
        try {
            StringBuilder fingerprint = new StringBuilder();
            
            // 使用Android ID (最可靠的设备标识)
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId != null && !androidId.isEmpty() && !"9774d56d682e549c".equals(androidId)) {
                fingerprint.append(androidId);
            }
            
            // 添加设备硬件信息
            fingerprint.append("|").append(Build.MANUFACTURER);
            fingerprint.append("|").append(Build.MODEL);
            fingerprint.append("|").append(Build.BOARD);
            fingerprint.append("|").append(Build.BRAND);
            
            // 生成MD5哈希
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(fingerprint.toString().getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String deviceId = hexString.toString();
            Log.d(TAG, "Generated device fingerprint: " + deviceId);
            return deviceId;
            
        } catch (Exception e) {
            Log.w(TAG, "Error generating device fingerprint", e);
            // 备用方案：使用基本设备信息
            return (Build.MANUFACTURER + Build.MODEL + Build.BOARD).replaceAll("\\s+", "").toLowerCase();
        }
    }
    
    /**
     * 获取首次真实使用时间
     */
    public long getFirstRealUseTime() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(INSTALL_PREFS, Context.MODE_PRIVATE);
            return prefs.getLong(KEY_FIRST_REAL_USE_TIME, 0);
        } catch (Exception e) {
            Log.w(TAG, "Error getting first real use time", e);
            return 0;
        }
    }
    
    /**
     * 重置安装统计状态（调试用）
     */
    public void resetInstallTracking() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(INSTALL_PREFS, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d(TAG, "Install tracking state reset");
        } catch (Exception e) {
            Log.w(TAG, "Error resetting install tracking", e);
        }
    }
    
    /**
     * 手动触发重试所有待发送请求
     * 可在网络恢复后主动调用
     */
    public void retryPendingRequestsManually() {
        try {
            if (!initialized) {
                Log.w(TAG, "ChannelTracker not initialized");
                return;
            }
            
            if (pendingRequests.isEmpty()) {
                Log.d(TAG, "No pending requests to retry");
                return;
            }
            
            Log.d(TAG, "Manually triggering retry for " + pendingRequests.size() + " pending requests");
            retryPendingRequests();
            
        } catch (Exception e) {
            Log.w(TAG, "Error manually retrying pending requests", e);
        }
    }
    
    /**
     * 获取待发送请求数量
     */
    public int getPendingRequestCount() {
        try {
            return pendingRequests.size();
        } catch (Exception e) {
            Log.w(TAG, "Error getting pending request count", e);
            return 0;
        }
    }
    
    /**
     * 清理所有缓存数据（包括安装统计和待发送请求）
     */
    public void clearAllCache() {
        try {
            // 清理安装统计缓存
            SharedPreferences installPrefs = context.getSharedPreferences(INSTALL_PREFS, Context.MODE_PRIVATE);
            installPrefs.edit().clear().apply();
            
            // 清理待发送请求缓存
            SharedPreferences pendingPrefs = context.getSharedPreferences(PENDING_REQUESTS_PREFS, Context.MODE_PRIVATE);
            pendingPrefs.edit().clear().apply();
            
            // 清理内存中的待发送请求
            pendingRequests.clear();
            
            Log.d(TAG, "All cache cleared");
            
        } catch (Exception e) {
            Log.w(TAG, "Error clearing cache", e);
        }
    }
    
    /**
     * 记录首次真实使用时间
     */
    private void recordFirstRealUse() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(INSTALL_PREFS, Context.MODE_PRIVATE);
            if (!prefs.contains(KEY_FIRST_REAL_USE_TIME)) {
                prefs.edit()
                    .putLong(KEY_FIRST_REAL_USE_TIME, System.currentTimeMillis())
                    .apply();

                Log.d(TAG, "First real use time recorded");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error recording first real use time", e);
        }
    }

    /**
     * 统计回调接口
     */
    public interface TrackCallback {
        void onResult(boolean success, @Nullable String message);
    }
}