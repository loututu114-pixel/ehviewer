/*
 * EhViewer Analytics Module - AnalyticsManager
 * 统计分析管理器 - 提供应用统计和用户行为分析功能
 */

package com.hippo.ehviewer.analytics;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * 统计分析管理器
 * 提供应用统计、用户行为分析、事件跟踪等功能
 */
public class AnalyticsManager {

    private static final String TAG = AnalyticsManager.class.getSimpleName();
    private static AnalyticsManager sInstance;

    private final Context mContext;
    private boolean mEnabled = true;
    private AnalyticsProvider mProvider;

    /**
     * 分析服务提供者接口
     */
    public interface AnalyticsProvider {
        void logEvent(String eventName, Bundle parameters);
        void setUserProperty(String propertyName, String propertyValue);
        void setUserId(String userId);
        void setScreenName(String screenName);
    }

    /**
     * 获取单例实例
     */
    public static synchronized AnalyticsManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AnalyticsManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private AnalyticsManager(Context context) {
        mContext = context;
    }

    /**
     * 设置分析服务提供者
     */
    public void setProvider(AnalyticsProvider provider) {
        mProvider = provider;
    }

    /**
     * 启用/禁用统计分析
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        Log.d(TAG, "Analytics " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * 记录事件
     */
    public void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    /**
     * 记录事件（带参数）
     */
    public void logEvent(String eventName, Bundle parameters) {
        if (!mEnabled) {
            return;
        }

        if (mProvider != null) {
            mProvider.logEvent(eventName, parameters);
        }

        Log.d(TAG, "Event logged: " + eventName);
    }

    /**
     * 记录用户行为
     */
    public void logUserAction(String action, String category) {
        Bundle params = new Bundle();
        params.putString("category", category);
        params.putString("action", action);
        logEvent("user_action", params);
    }

    /**
     * 记录页面访问
     */
    public void logScreenView(String screenName) {
        if (!mEnabled) {
            return;
        }

        if (mProvider != null) {
            mProvider.setScreenName(screenName);
        }

        Bundle params = new Bundle();
        params.putString("screen_name", screenName);
        logEvent("screen_view", params);
    }

    /**
     * 记录搜索行为
     */
    public void logSearch(String query, String category) {
        Bundle params = new Bundle();
        params.putString("search_query", query);
        params.putString("search_category", category);
        logEvent("search", params);
    }

    /**
     * 记录下载行为
     */
    public void logDownload(String itemId, String itemName, long fileSize) {
        Bundle params = new Bundle();
        params.putString("item_id", itemId);
        params.putString("item_name", itemName);
        params.putLong("file_size", fileSize);
        logEvent("download", params);
    }

    /**
     * 记录错误
     */
    public void logError(String errorType, String errorMessage) {
        Bundle params = new Bundle();
        params.putString("error_type", errorType);
        params.putString("error_message", errorMessage);
        logEvent("error", params);
    }

    /**
     * 设置用户属性
     */
    public void setUserProperty(String propertyName, String propertyValue) {
        if (!mEnabled) {
            return;
        }

        if (mProvider != null) {
            mProvider.setUserProperty(propertyName, propertyValue);
        }

        Log.d(TAG, "User property set: " + propertyName + " = " + propertyValue);
    }

    /**
     * 设置用户ID
     */
    public void setUserId(String userId) {
        if (!mEnabled) {
            return;
        }

        if (mProvider != null) {
            mProvider.setUserId(userId);
        }

        Log.d(TAG, "User ID set: " + userId);
    }

    /**
     * 记录应用启动
     */
    public void logAppStart() {
        logEvent("app_start");
    }

    /**
     * 记录应用退出
     */
    public void logAppStop() {
        logEvent("app_stop");
    }

    /**
     * 记录会话开始
     */
    public void logSessionStart() {
        logEvent("session_start");
    }

    /**
     * 记录会话结束
     */
    public void logSessionEnd(long duration) {
        Bundle params = new Bundle();
        params.putLong("duration", duration);
        logEvent("session_end", params);
    }

    /**
     * 记录性能指标
     */
    public void logPerformance(String metricName, long value) {
        Bundle params = new Bundle();
        params.putString("metric_name", metricName);
        params.putLong("metric_value", value);
        logEvent("performance", params);
    }

    /**
     * 记录网络请求
     */
    public void logNetworkRequest(String url, long responseTime, int statusCode) {
        Bundle params = new Bundle();
        params.putString("url", url);
        params.putLong("response_time", responseTime);
        params.putInt("status_code", statusCode);
        logEvent("network_request", params);
    }
}
