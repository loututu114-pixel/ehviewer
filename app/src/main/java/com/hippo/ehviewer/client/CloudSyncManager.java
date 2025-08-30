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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.client.data.BookmarkInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 云同步管理器
 * 处理书签和密码数据的云端同步
 */
public class CloudSyncManager {

    private static final String TAG = "CloudSyncManager";

    // 云端API配置（示例）
    private static final String API_BASE_URL = "https://api.ehviewer.cloud/v1";
    private static final String ENDPOINT_BOOKMARKS = "/bookmarks";
    private static final String ENDPOINT_PASSWORDS = "/passwords";
    private static final String ENDPOINT_SYNC = "/sync";

    private static CloudSyncManager sInstance;

    private Context mContext;
    private ExecutorService mExecutorService;
    private String mUserToken = null;
    private boolean mIsOnlineSyncEnabled = true;

    // 同步状态监听器
    public interface SyncListener {
        void onSyncStarted();
        void onSyncProgress(int progress);
        void onSyncCompleted();
        void onSyncFailed(String error);
    }

    /**
     * 获取单例实例
     */
    public static synchronized CloudSyncManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new CloudSyncManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private CloudSyncManager(Context context) {
        this.mContext = context;
        this.mExecutorService = Executors.newSingleThreadExecutor();

        // 初始化时检查网络状态
        checkNetworkStatus();
    }

    /**
     * 设置用户认证令牌
     */
    public void setUserToken(String token) {
        this.mUserToken = token;
        Log.d(TAG, "User token set");
    }

    /**
     * 检查网络状态
     */
    private boolean checkNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    /**
     * 同步书签数据
     */
    public void syncBookmarks(@Nullable SyncListener listener) {
        if (!checkNetworkStatus()) {
            if (listener != null) {
                listener.onSyncFailed("网络不可用");
            }
            return;
        }

        if (mUserToken == null) {
            if (listener != null) {
                listener.onSyncFailed("用户未登录");
            }
            return;
        }

        mExecutorService.execute(() -> {
            try {
                if (listener != null) {
                    listener.onSyncStarted();
                }

                // 获取本地书签数据
                BookmarkManager bookmarkManager = BookmarkManager.getInstance(mContext);
                List<BookmarkInfo> localBookmarks = bookmarkManager.getAllBookmarks();

                // 转换为JSON
                JSONArray bookmarksJson = new JSONArray();
                for (BookmarkInfo bookmark : localBookmarks) {
                    JSONObject bookmarkObj = new JSONObject();
                    bookmarkObj.put("title", bookmark.title);
                    bookmarkObj.put("url", bookmark.url);
                    bookmarkObj.put("id", bookmark.id);
                    bookmarksJson.put(bookmarkObj);
                }

                // 上传到云端
                JSONObject syncData = new JSONObject();
                syncData.put("bookmarks", bookmarksJson);
                syncData.put("lastSyncTime", System.currentTimeMillis());

                String response = makeHttpRequest("POST", ENDPOINT_SYNC, syncData.toString());

                if (listener != null) {
                    listener.onSyncCompleted();
                }

                Log.d(TAG, "Bookmarks sync completed");

            } catch (Exception e) {
                Log.e(TAG, "Failed to sync bookmarks", e);
                if (listener != null) {
                    listener.onSyncFailed(e.getMessage());
                }
            }
        });
    }

    /**
     * 同步密码数据
     */
    public void syncPasswords(@Nullable SyncListener listener) {
        if (!checkNetworkStatus()) {
            if (listener != null) {
                listener.onSyncFailed("网络不可用");
            }
            return;
        }

        if (mUserToken == null) {
            if (listener != null) {
                listener.onSyncFailed("用户未登录");
            }
            return;
        }

        // 密码同步需要额外的安全措施
        mExecutorService.execute(() -> {
            try {
                if (listener != null) {
                    listener.onSyncStarted();
                }

                // 获取本地密码数据（这里需要PasswordManager的统计接口）
                PasswordManager passwordManager = PasswordManager.getInstance(mContext);
                PasswordManager.PasswordStats stats = passwordManager.getStats();

                // 密码数据需要端到端加密后再上传
                // 这里是概念验证，实际实现需要更复杂的安全机制

                JSONObject syncData = new JSONObject();
                syncData.put("passwordCount", stats.totalPasswords);
                syncData.put("domainCount", stats.getUniqueDomains());
                syncData.put("lastSyncTime", System.currentTimeMillis());

                String response = makeHttpRequest("POST", ENDPOINT_PASSWORDS + "/sync", syncData.toString());

                if (listener != null) {
                    listener.onSyncCompleted();
                }

                Log.d(TAG, "Passwords sync completed");

            } catch (Exception e) {
                Log.e(TAG, "Failed to sync passwords", e);
                if (listener != null) {
                    listener.onSyncFailed(e.getMessage());
                }
            }
        });
    }

    /**
     * 执行完整同步
     */
    public void performFullSync(@Nullable SyncListener listener) {
        if (listener != null) {
            listener.onSyncStarted();
        }

        // 先同步书签
        syncBookmarks(new SyncListener() {
            @Override
            public void onSyncStarted() {}

            @Override
            public void onSyncProgress(int progress) {
                if (listener != null) {
                    listener.onSyncProgress(progress / 2); // 书签占50%
                }
            }

            @Override
            public void onSyncCompleted() {
                // 书签同步完成，开始同步密码
                syncPasswords(new SyncListener() {
                    @Override
                    public void onSyncStarted() {}

                    @Override
                    public void onSyncProgress(int progress) {
                        if (listener != null) {
                            listener.onSyncProgress(50 + progress / 2); // 密码占50%
                        }
                    }

                    @Override
                    public void onSyncCompleted() {
                        if (listener != null) {
                            listener.onSyncCompleted();
                        }
                    }

                    @Override
                    public void onSyncFailed(String error) {
                        if (listener != null) {
                            listener.onSyncFailed("密码同步失败: " + error);
                        }
                    }
                });
            }

            @Override
            public void onSyncFailed(String error) {
                if (listener != null) {
                    listener.onSyncFailed("书签同步失败: " + error);
                }
            }
        });
    }

    /**
     * 从云端拉取数据
     */
    public void pullFromCloud(@Nullable SyncListener listener) {
        if (!checkNetworkStatus()) {
            if (listener != null) {
                listener.onSyncFailed("网络不可用");
            }
            return;
        }

        if (mUserToken == null) {
            if (listener != null) {
                listener.onSyncFailed("用户未登录");
            }
            return;
        }

        mExecutorService.execute(() -> {
            try {
                if (listener != null) {
                    listener.onSyncStarted();
                }

                // 从云端获取数据
                String response = makeHttpRequest("GET", ENDPOINT_SYNC, null);

                if (response != null) {
                    JSONObject syncData = new JSONObject(response);

                    // 处理书签数据
                    if (syncData.has("bookmarks")) {
                        JSONArray bookmarks = syncData.getJSONArray("bookmarks");
                        mergeBookmarksFromCloud(bookmarks);
                    }

                    // 处理密码数据（这里需要更复杂的安全处理）
                    // ...

                    if (listener != null) {
                        listener.onSyncCompleted();
                    }

                    Log.d(TAG, "Data pulled from cloud successfully");
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to pull data from cloud", e);
                if (listener != null) {
                    listener.onSyncFailed(e.getMessage());
                }
            }
        });
    }

    /**
     * 合并云端的书签数据
     */
    private void mergeBookmarksFromCloud(JSONArray cloudBookmarks) throws Exception {
        BookmarkManager bookmarkManager = BookmarkManager.getInstance(mContext);

        for (int i = 0; i < cloudBookmarks.length(); i++) {
            JSONObject bookmarkObj = cloudBookmarks.getJSONObject(i);

            String title = bookmarkObj.getString("title");
            String url = bookmarkObj.getString("url");

            // 检查本地是否已存在
            if (!bookmarkManager.isBookmarked(url)) {
                // 添加新的书签
                bookmarkManager.addBookmark(title, url);
            }
        }
    }

    /**
     * 发起HTTP请求
     */
    private String makeHttpRequest(String method, String endpoint, String data) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + mUserToken);

            if (data != null && (method.equals("POST") || method.equals("PUT"))) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Length", String.valueOf(data.length()));

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(data.getBytes("UTF-8"));
                    os.flush();
                }
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                return response.toString();
            } else {
                throw new Exception("HTTP Error: " + responseCode);
            }

        } finally {
            connection.disconnect();
        }
    }

    /**
     * 启用/禁用在线同步
     */
    public void setOnlineSyncEnabled(boolean enabled) {
        this.mIsOnlineSyncEnabled = enabled;
        Log.d(TAG, "Online sync " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * 检查在线同步是否启用
     */
    public boolean isOnlineSyncEnabled() {
        return mIsOnlineSyncEnabled;
    }

    /**
     * 清理资源
     */
    public void destroy() {
        mExecutorService.shutdown();
        Log.d(TAG, "CloudSyncManager destroyed");
    }

    /**
     * 获取同步状态
     */
    public SyncStatus getSyncStatus() {
        SyncStatus status = new SyncStatus();
        status.isOnline = checkNetworkStatus();
        status.isLoggedIn = mUserToken != null;
        status.isOnlineSyncEnabled = mIsOnlineSyncEnabled;
        return status;
    }

    /**
     * 同步状态
     */
    public static class SyncStatus {
        public boolean isOnline;
        public boolean isLoggedIn;
        public boolean isOnlineSyncEnabled;

        public boolean canSync() {
            return isOnline && isLoggedIn && isOnlineSyncEnabled;
        }
    }
}
