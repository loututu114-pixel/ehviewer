package com.hippo.ehviewer.privacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 隐私模式管理器 - 无痕浏览和数据隔离
 * 
 * 核心功能：
 * 1. 无痕浏览模式切换
 * 2. 数据隔离和清理
 * 3. Cookie和存储隔离
 * 4. 历史记录隔离
 * 5. 下载记录隔离
 * 6. 自动清理机制
 * 7. 隐私状态监控
 */
public class PrivacyModeManager {
    private static final String TAG = "PrivacyModeManager";
    
    // 单例实例
    private static volatile PrivacyModeManager sInstance;
    private final Context mContext;
    
    // 配置常量
    private static final String PREFS_NAME = "privacy_mode_prefs";
    private static final String KEY_PRIVACY_MODE_ENABLED = "privacy_mode_enabled";
    private static final String KEY_AUTO_CLEAR_ENABLED = "auto_clear_enabled";
    private static final String KEY_CLEAR_ON_EXIT = "clear_on_exit";
    private static final String KEY_CLEAR_INTERVAL = "clear_interval_minutes";
    private static final String KEY_LAST_CLEAR_TIME = "last_clear_time";
    
    // 默认配置
    private static final int DEFAULT_CLEAR_INTERVAL = 30; // 30分钟
    
    // 状态管理
    private boolean mIsPrivacyModeEnabled = false;
    private boolean mIsAutoClearEnabled = true;
    private boolean mClearOnExit = true;
    private int mClearInterval = DEFAULT_CLEAR_INTERVAL;
    
    // 监听器管理
    private final List<PrivacyModeListener> mListeners = new CopyOnWriteArrayList<>();
    
    // 自动清理管理
    private Handler mCleanupHandler;
    private Runnable mCleanupRunnable;
    
    // 数据隔离管理
    private PrivacyDataManager mDataManager;
    private PrivacyWebViewManager mWebViewManager;
    
    /**
     * 隐私模式状态监听器
     */
    public interface PrivacyModeListener {
        void onPrivacyModeChanged(boolean enabled);
        void onDataCleared(ClearResult result);
        void onPrivacyViolationDetected(String description);
    }
    
    /**
     * 数据清理结果
     */
    public static class ClearResult {
        public boolean success;
        public int clearedCookies;
        public int clearedHistoryItems;
        public int clearedCacheFiles;
        public long clearedCacheSize;
        public String errorMessage;
        
        public ClearResult() {
            this.success = true;
        }
        
        @Override
        public String toString() {
            if (!success) {
                return "清理失败: " + errorMessage;
            }
            
            return String.format("清理完成: Cookie %d个, 历史记录 %d条, 缓存 %d个文件 (%.2f MB)", 
                clearedCookies, clearedHistoryItems, clearedCacheFiles, 
                clearedCacheSize / 1024.0 / 1024.0);
        }
    }
    
    /**
     * 隐私模式配置
     */
    public static class PrivacyConfig {
        public boolean enablePrivacyMode = false;
        public boolean clearCookies = true;
        public boolean clearHistory = true;
        public boolean clearCache = true;
        public boolean clearDownloads = false;
        public boolean clearFormData = true;
        public boolean clearPasswords = false;
        public boolean isolateStorage = true;
        public boolean disableJavaScript = false;
        public boolean blockThirdPartyCookies = true;
        public boolean enableDNT = true; // Do Not Track
        
        @Override
        public String toString() {
            return String.format("Privacy Config: Mode=%s, Clear=%s/%s/%s, Isolate=%s", 
                enablePrivacyMode ? "ON" : "OFF",
                clearCookies ? "Cookie" : "",
                clearHistory ? "History" : "",
                clearCache ? "Cache" : "",
                isolateStorage ? "ON" : "OFF");
        }
    }
    
    private PrivacyModeManager(Context context) {
        mContext = context.getApplicationContext();
        mCleanupHandler = new Handler(Looper.getMainLooper());
        
        // 初始化子管理器
        mDataManager = new PrivacyDataManager(mContext);
        mWebViewManager = new PrivacyWebViewManager(mContext);
        
        // 加载配置
        loadConfiguration();
        
        // 启动自动清理
        startAutoCleanup();
    }
    
    /**
     * 获取单例实例
     */
    public static PrivacyModeManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PrivacyModeManager.class) {
                if (sInstance == null) {
                    sInstance = new PrivacyModeManager(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 启用/禁用隐私模式
     */
    public void setPrivacyModeEnabled(boolean enabled) {
        if (mIsPrivacyModeEnabled == enabled) {
            return;
        }
        
        Log.d(TAG, "Privacy mode " + (enabled ? "enabled" : "disabled"));
        mIsPrivacyModeEnabled = enabled;
        
        // 保存配置
        saveConfiguration();
        
        // 应用隐私设置
        applyPrivacySettings(enabled);
        
        // 通知监听器
        notifyPrivacyModeChanged(enabled);
        
        if (enabled) {
            // 启用隐私模式时清理现有数据
            clearPrivacyData(getDefaultPrivacyConfig());
        }
    }
    
    /**
     * 检查隐私模式是否启用
     */
    public boolean isPrivacyModeEnabled() {
        return mIsPrivacyModeEnabled;
    }
    
    /**
     * 配置WebView隐私设置
     */
    public void configureWebViewForPrivacy(@NonNull WebView webView) {
        mWebViewManager.configureWebView(webView, mIsPrivacyModeEnabled);
    }
    
    /**
     * 清理隐私数据
     */
    public void clearPrivacyData(@NonNull PrivacyConfig config) {
        Log.d(TAG, "Starting privacy data cleanup with config: " + config);
        
        // 异步执行清理操作
        new Thread(() -> {
            ClearResult result = mDataManager.clearData(config);
            
            // 在主线程通知结果
            mCleanupHandler.post(() -> {
                notifyDataCleared(result);
            });
        }).start();
    }
    
    /**
     * 获取默认隐私配置
     */
    public PrivacyConfig getDefaultPrivacyConfig() {
        PrivacyConfig config = new PrivacyConfig();
        config.enablePrivacyMode = mIsPrivacyModeEnabled;
        config.clearCookies = true;
        config.clearHistory = true;
        config.clearCache = true;
        config.clearFormData = true;
        config.isolateStorage = true;
        config.blockThirdPartyCookies = true;
        config.enableDNT = true;
        return config;
    }
    
    /**
     * 设置自动清理配置
     */
    public void setAutoCleanConfig(boolean enabled, int intervalMinutes, boolean clearOnExit) {
        mIsAutoClearEnabled = enabled;
        mClearInterval = intervalMinutes;
        mClearOnExit = clearOnExit;
        
        saveConfiguration();
        
        // 重启自动清理
        stopAutoCleanup();
        if (enabled) {
            startAutoCleanup();
        }
    }
    
    /**
     * 获取隐私状态摘要
     */
    public String getPrivacyStatusSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("隐私模式: ").append(mIsPrivacyModeEnabled ? "启用" : "禁用").append("\n");
        summary.append("自动清理: ").append(mIsAutoClearEnabled ? "启用" : "禁用");
        
        if (mIsAutoClearEnabled) {
            summary.append(" (").append(mClearInterval).append("分钟)");
        }
        
        summary.append("\n");
        summary.append("退出时清理: ").append(mClearOnExit ? "启用" : "禁用").append("\n");
        
        // 获取数据统计
        PrivacyDataManager.DataStats stats = mDataManager.getDataStats();
        summary.append("当前数据: Cookie ").append(stats.cookieCount)
            .append("个, 历史记录 ").append(stats.historyCount)
            .append("条, 缓存 ").append(String.format("%.1f MB", stats.cacheSize / 1024.0 / 1024.0));
        
        return summary.toString();
    }
    
    /**
     * 检测潜在的隐私泄露
     */
    public void detectPrivacyViolations() {
        if (!mIsPrivacyModeEnabled) {
            return;
        }
        
        new Thread(() -> {
            List<String> violations = new ArrayList<>();
            
            // 检查是否有敏感数据泄露
            PrivacyDataManager.DataStats stats = mDataManager.getDataStats();
            
            if (stats.cookieCount > 0) {
                violations.add("检测到 " + stats.cookieCount + " 个Cookie");
            }
            
            if (stats.historyCount > 0) {
                violations.add("检测到 " + stats.historyCount + " 条浏览记录");
            }
            
            if (stats.cacheSize > 10 * 1024 * 1024) { // 10MB
                violations.add("缓存大小超过 " + String.format("%.1f MB", stats.cacheSize / 1024.0 / 1024.0));
            }
            
            // 通知检测结果
            if (!violations.isEmpty()) {
                String description = "隐私模式下发现数据残留: " + String.join(", ", violations);
                mCleanupHandler.post(() -> notifyPrivacyViolationDetected(description));
            }
        }).start();
    }
    
    /**
     * 应用退出时清理
     */
    public void onApplicationExit() {
        if (mClearOnExit && mIsPrivacyModeEnabled) {
            Log.d(TAG, "Clearing data on application exit");
            clearPrivacyData(getDefaultPrivacyConfig());
        }
    }
    
    /**
     * 添加监听器
     */
    public void addListener(@NonNull PrivacyModeListener listener) {
        mListeners.add(listener);
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(@NonNull PrivacyModeListener listener) {
        mListeners.remove(listener);
    }
    
    // 私有方法
    
    private void loadConfiguration() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        mIsPrivacyModeEnabled = prefs.getBoolean(KEY_PRIVACY_MODE_ENABLED, false);
        mIsAutoClearEnabled = prefs.getBoolean(KEY_AUTO_CLEAR_ENABLED, true);
        mClearOnExit = prefs.getBoolean(KEY_CLEAR_ON_EXIT, true);
        mClearInterval = prefs.getInt(KEY_CLEAR_INTERVAL, DEFAULT_CLEAR_INTERVAL);
        
        Log.d(TAG, String.format("Loaded config: Privacy=%s, AutoClear=%s, ClearOnExit=%s, Interval=%d", 
            mIsPrivacyModeEnabled, mIsAutoClearEnabled, mClearOnExit, mClearInterval));
    }
    
    private void saveConfiguration() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_PRIVACY_MODE_ENABLED, mIsPrivacyModeEnabled)
            .putBoolean(KEY_AUTO_CLEAR_ENABLED, mIsAutoClearEnabled)
            .putBoolean(KEY_CLEAR_ON_EXIT, mClearOnExit)
            .putInt(KEY_CLEAR_INTERVAL, mClearInterval)
            .putLong(KEY_LAST_CLEAR_TIME, System.currentTimeMillis())
            .apply();
    }
    
    private void applyPrivacySettings(boolean enabled) {
        if (enabled) {
            Log.d(TAG, "Applying privacy protection settings");
            // 应用隐私保护设置
            mWebViewManager.enablePrivacyMode();
        } else {
            Log.d(TAG, "Disabling privacy protection settings");
            // 恢复正常设置
            mWebViewManager.disablePrivacyMode();
        }
    }
    
    private void startAutoCleanup() {
        if (!mIsAutoClearEnabled || mClearInterval <= 0) {
            return;
        }
        
        if (mCleanupRunnable != null) {
            mCleanupHandler.removeCallbacks(mCleanupRunnable);
        }
        
        mCleanupRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsPrivacyModeEnabled) {
                    Log.d(TAG, "Auto cleanup triggered");
                    clearPrivacyData(getDefaultPrivacyConfig());
                }
                
                // 调度下次清理
                mCleanupHandler.postDelayed(this, mClearInterval * 60 * 1000); // 转换为毫秒
            }
        };
        
        // 启动第一次清理
        mCleanupHandler.postDelayed(mCleanupRunnable, mClearInterval * 60 * 1000);
        
        Log.d(TAG, "Auto cleanup started with interval: " + mClearInterval + " minutes");
    }
    
    private void stopAutoCleanup() {
        if (mCleanupRunnable != null) {
            mCleanupHandler.removeCallbacks(mCleanupRunnable);
            mCleanupRunnable = null;
        }
        Log.d(TAG, "Auto cleanup stopped");
    }
    
    // 通知方法
    
    private void notifyPrivacyModeChanged(boolean enabled) {
        for (PrivacyModeListener listener : mListeners) {
            try {
                listener.onPrivacyModeChanged(enabled);
            } catch (Exception e) {
                Log.w(TAG, "Error notifying privacy mode change", e);
            }
        }
    }
    
    private void notifyDataCleared(ClearResult result) {
        Log.d(TAG, "Data clear result: " + result);
        
        for (PrivacyModeListener listener : mListeners) {
            try {
                listener.onDataCleared(result);
            } catch (Exception e) {
                Log.w(TAG, "Error notifying data cleared", e);
            }
        }
    }
    
    private void notifyPrivacyViolationDetected(String description) {
        Log.w(TAG, "Privacy violation detected: " + description);
        
        for (PrivacyModeListener listener : mListeners) {
            try {
                listener.onPrivacyViolationDetected(description);
            } catch (Exception e) {
                Log.w(TAG, "Error notifying privacy violation", e);
            }
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        stopAutoCleanup();
        mListeners.clear();
        
        if (mDataManager != null) {
            mDataManager.cleanup();
        }
        
        if (mWebViewManager != null) {
            mWebViewManager.cleanup();
        }
    }
}