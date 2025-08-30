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
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 搜索引擎管理器
 * 根据网络状况智能选择搜索引擎
 */
public class SearchEngineManager {

    private static final String TAG = "SearchEngineManager";

    private static SearchEngineManager sInstance;

    private Context mContext;
    private NetworkDetector mNetworkDetector;
    private SharedPreferences mPreferences;

    // 搜索引擎配置
    public enum SearchEngine {
        GOOGLE("Google", "https://www.google.com/search?q=", "https://www.google.com"),
        BAIDU("百度", "https://www.baidu.com/s?wd=", "https://www.baidu.com"),
        BING("Bing", "https://www.bing.com/search?q=", "https://www.bing.com"),
        SOUGOU("搜狗", "https://www.sogou.com/web?query=", "https://www.sogou.com"),
        AUTO("自动", "", ""); // 自动选择

        public final String displayName;
        public final String searchUrl;
        public final String homepage;

        SearchEngine(String displayName, String searchUrl, String homepage) {
            this.displayName = displayName;
            this.searchUrl = searchUrl;
            this.homepage = homepage;
        }
    }

    // 偏好设置键
    private static final String PREF_SEARCH_ENGINE = "search_engine";
    private static final String PREF_AUTO_SWITCH = "auto_switch_enabled";

    /**
     * 搜索回调
     */
    public interface SearchCallback {
        void onSearchUrlGenerated(String url, String engineName);
        void onSearchFailed(String error);
    }

    /**
     * 获取单例实例
     */
    public static synchronized SearchEngineManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new SearchEngineManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private SearchEngineManager(Context context) {
        this.mContext = context;
        this.mNetworkDetector = NetworkDetector.getInstance(context);
        this.mPreferences = context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE);

        // 初始化默认设置
        initializeDefaults();
    }

    /**
     * 初始化默认设置
     */
    private void initializeDefaults() {
        if (!mPreferences.contains(PREF_SEARCH_ENGINE)) {
            setSearchEngine(SearchEngine.AUTO);
        }
        if (!mPreferences.contains(PREF_AUTO_SWITCH)) {
            setAutoSwitchEnabled(true);
        }
    }

    /**
     * 执行搜索
     */
    public void performSearch(String query, @Nullable SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            if (callback != null) {
                callback.onSearchFailed("搜索关键词不能为空");
            }
            return;
        }

        SearchEngine currentEngine = getCurrentSearchEngine();

        if (currentEngine == SearchEngine.AUTO) {
            // 自动选择搜索引擎
            performAutoSearch(query, callback);
        } else {
            // 使用指定搜索引擎
            String url = generateSearchUrl(query, currentEngine);
            if (callback != null) {
                callback.onSearchUrlGenerated(url, currentEngine.displayName);
            }
        }
    }

    /**
     * 自动选择搜索引擎
     */
    private void performAutoSearch(String query, @Nullable SearchCallback callback) {
        if (!isAutoSwitchEnabled()) {
            // 如果自动切换被禁用，使用Google
            String url = generateSearchUrl(query, SearchEngine.GOOGLE);
            if (callback != null) {
                callback.onSearchUrlGenerated(url, SearchEngine.GOOGLE.displayName);
            }
            return;
        }

        // 检测网络状态
        mNetworkDetector.detectNetworkStatus(new NetworkDetector.NetworkCallback() {
            @Override
            public void onNetworkStatusDetected(NetworkDetector.NetworkStatus status) {
                SearchEngine selectedEngine;

                switch (status) {
                    case GFW_BLOCKED:
                        selectedEngine = SearchEngine.BAIDU;
                        Log.d(TAG, "Detected GFW blockage, switching to Baidu");
                        break;
                    case CONNECTED:
                        // 网络正常，优先使用Google
                        selectedEngine = SearchEngine.GOOGLE;
                        break;
                    case NO_NETWORK:
                    case NETWORK_ERROR:
                        // 网络问题，使用百度作为备选
                        selectedEngine = SearchEngine.BAIDU;
                        break;
                    default:
                        selectedEngine = SearchEngine.GOOGLE;
                        break;
                }

                String url = generateSearchUrl(query, selectedEngine);
                if (callback != null) {
                    callback.onSearchUrlGenerated(url, selectedEngine.displayName);
                }
            }

            @Override
            public void onDetectionFailed(String error) {
                // 检测失败时使用Google
                Log.w(TAG, "Network detection failed: " + error + ", using Google as default");
                String url = generateSearchUrl(query, SearchEngine.GOOGLE);
                if (callback != null) {
                    callback.onSearchUrlGenerated(url, SearchEngine.GOOGLE.displayName);
                }
            }
        });
    }

    /**
     * 生成搜索URL
     */
    private String generateSearchUrl(String query, SearchEngine engine) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8");
            return engine.searchUrl + encodedQuery;
        } catch (Exception e) {
            Log.e(TAG, "Failed to encode search query", e);
            return engine.homepage;
        }
    }

    /**
     * 获取当前搜索引擎
     */
    public SearchEngine getCurrentSearchEngine() {
        String engineName = mPreferences.getString(PREF_SEARCH_ENGINE, SearchEngine.AUTO.name());
        try {
            return SearchEngine.valueOf(engineName);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid search engine preference, using AUTO");
            return SearchEngine.AUTO;
        }
    }

    /**
     * 设置搜索引擎
     */
    public void setSearchEngine(SearchEngine engine) {
        mPreferences.edit()
                .putString(PREF_SEARCH_ENGINE, engine.name())
                .apply();
        Log.d(TAG, "Search engine set to: " + engine.displayName);
    }

    /**
     * 检查是否启用自动切换
     */
    public boolean isAutoSwitchEnabled() {
        return mPreferences.getBoolean(PREF_AUTO_SWITCH, true);
    }

    /**
     * 设置自动切换
     */
    public void setAutoSwitchEnabled(boolean enabled) {
        mPreferences.edit()
                .putBoolean(PREF_AUTO_SWITCH, enabled)
                .apply();
        Log.d(TAG, "Auto switch " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * 获取搜索引擎显示名称
     */
    public String getSearchEngineDisplayName(SearchEngine engine) {
        switch (engine) {
            case GOOGLE:
                return "Google";
            case BAIDU:
                return "百度";
            case BING:
                return "Bing";
            case SOUGOU:
                return "搜狗";
            case AUTO:
                return "自动选择";
            default:
                return engine.displayName;
        }
    }

    /**
     * 获取所有可用搜索引擎
     */
    public SearchEngine[] getAvailableEngines() {
        return new SearchEngine[] {
            SearchEngine.AUTO,
            SearchEngine.GOOGLE,
            SearchEngine.BAIDU,
            SearchEngine.BING,
            SearchEngine.SOUGOU
        };
    }

    /**
     * 获取搜索引擎图标资源ID
     */
    public int getSearchEngineIconResId(SearchEngine engine) {
        // 这里需要根据实际的drawable资源返回相应的ID
        // 示例实现，实际需要根据资源文件调整
        switch (engine) {
            case GOOGLE:
                return android.R.drawable.ic_menu_search; // 临时使用系统图标
            case BAIDU:
                return android.R.drawable.ic_menu_search;
            case BING:
                return android.R.drawable.ic_menu_search;
            case SOUGOU:
                return android.R.drawable.ic_menu_search;
            case AUTO:
                return android.R.drawable.ic_menu_search;
            default:
                return android.R.drawable.ic_menu_search;
        }
    }

    /**
     * 重置为默认设置
     */
    public void resetToDefaults() {
        setSearchEngine(SearchEngine.AUTO);
        setAutoSwitchEnabled(true);
        Log.d(TAG, "Search settings reset to defaults");
    }

    /**
     * 获取设置摘要
     */
    public String getSettingsSummary() {
        SearchEngine engine = getCurrentSearchEngine();
        boolean autoSwitch = isAutoSwitchEnabled();

        StringBuilder summary = new StringBuilder();
        summary.append("搜索引擎: ").append(getSearchEngineDisplayName(engine));

        if (engine == SearchEngine.AUTO) {
            summary.append("\n自动切换: ").append(autoSwitch ? "开启" : "关闭");
            if (autoSwitch) {
                summary.append("\n网络检测: ").append(mNetworkDetector.getStatusDescription(mNetworkDetector.getCurrentStatus()));
            }
        }

        return summary.toString();
    }

    /**
     * 清理资源
     */
    public void destroy() {
        if (mNetworkDetector != null) {
            mNetworkDetector.destroy();
        }
        Log.d(TAG, "SearchEngineManager destroyed");
    }
}
