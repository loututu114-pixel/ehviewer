package com.hippo.ehviewer.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * 网络优化管理器 - 数据节省和性能优化
 * 
 * 核心功能：
 * 1. 数据节省模式
 * 2. 网络流量监控
 * 3. 智能资源压缩
 * 4. 离线缓存管理
 * 5. 网络质量检测
 * 6. 预加载优化
 */
public class NetworkOptimizationManager {
    private static final String TAG = "NetworkOptimizationManager";
    
    // 单例实例
    private static volatile NetworkOptimizationManager sInstance;
    private final Context mContext;
    
    // 配置常量
    private static final String PREFS_NAME = "network_optimization_prefs";
    private static final String KEY_DATA_SAVER_ENABLED = "data_saver_enabled";
    private static final String KEY_OFFLINE_CACHE_ENABLED = "offline_cache_enabled";
    private static final String KEY_IMAGE_COMPRESSION_ENABLED = "image_compression_enabled";
    private static final String KEY_PRELOAD_ENABLED = "preload_enabled";
    private static final String KEY_NETWORK_QUALITY_CHECK = "network_quality_check";
    private static final String KEY_MONTHLY_DATA_LIMIT = "monthly_data_limit_mb";
    private static final String KEY_CURRENT_MONTH_USAGE = "current_month_usage";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    
    // 默认配置
    private static final long DEFAULT_MONTHLY_LIMIT_MB = 1024; // 1GB
    private static final int IMAGE_QUALITY_WIFI = 90;
    private static final int IMAGE_QUALITY_MOBILE = 60;
    private static final int IMAGE_QUALITY_DATA_SAVER = 40;
    
    // 状态管理
    private boolean mDataSaverEnabled = false;
    private boolean mOfflineCacheEnabled = true;
    private boolean mImageCompressionEnabled = true;
    private boolean mPreloadEnabled = true;
    private boolean mNetworkQualityCheck = true;
    private long mMonthlyDataLimitMB = DEFAULT_MONTHLY_LIMIT_MB;
    
    // 流量统计
    private final AtomicLong mCurrentMonthUsage = new AtomicLong(0);
    private final AtomicLong mSessionUsage = new AtomicLong(0);
    
    // 网络状态
    private NetworkType mCurrentNetworkType = NetworkType.UNKNOWN;
    private NetworkQuality mCurrentNetworkQuality = NetworkQuality.UNKNOWN;
    
    // 监听器管理
    private final List<NetworkOptimizationListener> mListeners = new ArrayList<>();
    
    // 缓存管理
    private OfflineCacheManager mOfflineCacheManager;
    private NetworkTrafficMonitor mTrafficMonitor;
    
    /**
     * 网络类型枚举
     */
    public enum NetworkType {
        WIFI,           // WiFi网络
        MOBILE_2G,      // 2G移动网络
        MOBILE_3G,      // 3G移动网络
        MOBILE_4G,      // 4G/LTE网络
        MOBILE_5G,      // 5G网络
        ETHERNET,       // 以太网
        UNKNOWN         // 未知网络
    }
    
    /**
     * 网络质量枚举
     */
    public enum NetworkQuality {
        EXCELLENT,      // 优秀 (>50Mbps)
        GOOD,          // 良好 (10-50Mbps)  
        FAIR,          // 一般 (1-10Mbps)
        POOR,          // 较差 (<1Mbps)
        UNKNOWN        // 未知质量
    }
    
    /**
     * 网络优化监听器
     */
    public interface NetworkOptimizationListener {
        void onNetworkTypeChanged(NetworkType oldType, NetworkType newType);
        void onNetworkQualityChanged(NetworkQuality quality);
        void onDataUsageUpdate(long sessionUsage, long monthlyUsage, long monthlyLimit);
        void onDataLimitWarning(float usagePercentage);
        void onOptimizationApplied(String description);
    }
    
    /**
     * 网络优化配置
     */
    public static class OptimizationConfig {
        public boolean enableDataSaver = false;
        public boolean enableOfflineCache = true;
        public boolean enableImageCompression = true;
        public boolean enablePreload = true;
        public boolean enableQualityCheck = true;
        public int imageQualityWifi = IMAGE_QUALITY_WIFI;
        public int imageQualityMobile = IMAGE_QUALITY_MOBILE;
        public int imageQualityDataSaver = IMAGE_QUALITY_DATA_SAVER;
        public long monthlyDataLimitMB = DEFAULT_MONTHLY_LIMIT_MB;
        
        @Override
        public String toString() {
            return String.format("OptimizationConfig[DataSaver=%s, Cache=%s, Compression=%s, Preload=%s]", 
                enableDataSaver, enableOfflineCache, enableImageCompression, enablePreload);
        }
    }
    
    /**
     * 流量使用统计
     */
    public static class TrafficStats {
        public long sessionUsageBytes = 0;
        public long monthlyUsageBytes = 0;
        public long monthlyLimitBytes = 0;
        public float monthlyUsagePercentage = 0.0f;
        public long savedBytes = 0; // 通过优化节省的流量
        
        @Override
        public String toString() {
            return String.format("流量统计: 本次%.1fMB, 本月%.1fMB/%.1fMB (%.1f%%), 已节省%.1fMB", 
                sessionUsageBytes / 1024.0 / 1024.0,
                monthlyUsageBytes / 1024.0 / 1024.0,
                monthlyLimitBytes / 1024.0 / 1024.0,
                monthlyUsagePercentage * 100,
                savedBytes / 1024.0 / 1024.0);
        }
    }
    
    private NetworkOptimizationManager(Context context) {
        mContext = context.getApplicationContext();
        
        // 初始化子组件
        mOfflineCacheManager = new OfflineCacheManager(mContext);
        mTrafficMonitor = new NetworkTrafficMonitor(mContext);
        
        // 加载配置
        loadConfiguration();
        
        // 检测当前网络状态
        detectNetworkState();
        
        // 启动流量监控
        startTrafficMonitoring();
    }
    
    /**
     * 获取单例实例
     */
    public static NetworkOptimizationManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (NetworkOptimizationManager.class) {
                if (sInstance == null) {
                    sInstance = new NetworkOptimizationManager(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 应用网络优化到WebView
     */
    public void applyOptimizationToWebView(@NonNull WebView webView) {
        Log.d(TAG, "Applying network optimization to WebView");
        
        OptimizationConfig config = getCurrentOptimizationConfig();
        
        try {
            android.webkit.WebSettings settings = webView.getSettings();
            if (settings == null) {
                Log.w(TAG, "WebView settings is null");
                return;
            }
            
            // 根据网络类型和数据节省模式调整缓存策略
            applyCacheStrategy(settings, config);
            
            // 应用图片加载优化
            applyImageOptimization(settings, config);
            
            // 应用预加载设置
            applyPreloadSettings(settings, config);
            
            // 通知监听器
            notifyOptimizationApplied("WebView优化已应用: " + config.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying optimization to WebView", e);
        }
    }
    
    /**
     * 更新数据使用量
     */
    public void updateDataUsage(long bytes) {
        if (bytes <= 0) return;
        
        mSessionUsage.addAndGet(bytes);
        long monthlyUsage = mCurrentMonthUsage.addAndGet(bytes);
        
        // 检查是否需要重置月度统计
        checkAndResetMonthlyStats();
        
        // 保存到SharedPreferences
        saveCurrentUsage(monthlyUsage);
        
        // 检查数据限制警告
        checkDataLimitWarning(monthlyUsage);
        
        // 通知监听器
        notifyDataUsageUpdate();
        
        Log.d(TAG, String.format("Data usage updated: +%d bytes, session: %d bytes, monthly: %d bytes", 
            bytes, mSessionUsage.get(), monthlyUsage));
    }
    
    /**
     * 获取当前流量统计
     */
    public TrafficStats getTrafficStats() {
        TrafficStats stats = new TrafficStats();
        stats.sessionUsageBytes = mSessionUsage.get();
        stats.monthlyUsageBytes = mCurrentMonthUsage.get();
        stats.monthlyLimitBytes = mMonthlyDataLimitMB * 1024 * 1024;
        stats.monthlyUsagePercentage = (float) stats.monthlyUsageBytes / stats.monthlyLimitBytes;
        stats.savedBytes = mTrafficMonitor.getSavedBytes();
        
        return stats;
    }
    
    /**
     * 设置优化配置
     */
    public void setOptimizationConfig(@NonNull OptimizationConfig config) {
        Log.d(TAG, "Setting optimization config: " + config);
        
        mDataSaverEnabled = config.enableDataSaver;
        mOfflineCacheEnabled = config.enableOfflineCache;
        mImageCompressionEnabled = config.enableImageCompression;
        mPreloadEnabled = config.enablePreload;
        mNetworkQualityCheck = config.enableQualityCheck;
        mMonthlyDataLimitMB = config.monthlyDataLimitMB;
        
        // 保存配置
        saveConfiguration();
        
        // 更新离线缓存管理器
        mOfflineCacheManager.setEnabled(mOfflineCacheEnabled);
        
        // 通知监听器
        notifyOptimizationApplied("优化配置已更新");
    }
    
    /**
     * 获取当前优化配置
     */
    public OptimizationConfig getCurrentOptimizationConfig() {
        OptimizationConfig config = new OptimizationConfig();
        config.enableDataSaver = mDataSaverEnabled;
        config.enableOfflineCache = mOfflineCacheEnabled;
        config.enableImageCompression = mImageCompressionEnabled;
        config.enablePreload = mPreloadEnabled;
        config.enableQualityCheck = mNetworkQualityCheck;
        config.monthlyDataLimitMB = mMonthlyDataLimitMB;
        
        // 根据网络类型调整图片质量
        switch (mCurrentNetworkType) {
            case WIFI:
            case ETHERNET:
                config.imageQualityWifi = IMAGE_QUALITY_WIFI;
                break;
            case MOBILE_4G:
            case MOBILE_5G:
                config.imageQualityMobile = mDataSaverEnabled ? 
                    IMAGE_QUALITY_DATA_SAVER : IMAGE_QUALITY_MOBILE;
                break;
            case MOBILE_2G:
            case MOBILE_3G:
                config.imageQualityDataSaver = IMAGE_QUALITY_DATA_SAVER;
                break;
            default:
                // 使用保守设置
                config.imageQualityDataSaver = IMAGE_QUALITY_DATA_SAVER;
                break;
        }
        
        return config;
    }
    
    /**
     * 检测并更新网络状态
     */
    public void detectNetworkState() {
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                Log.w(TAG, "ConnectivityManager is null");
                return;
            }
            
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            NetworkType newNetworkType = detectNetworkType(activeNetwork);
            
            if (newNetworkType != mCurrentNetworkType) {
                NetworkType oldType = mCurrentNetworkType;
                mCurrentNetworkType = newNetworkType;
                
                Log.d(TAG, "Network type changed: " + oldType + " -> " + newNetworkType);
                notifyNetworkTypeChanged(oldType, newNetworkType);
            }
            
            // 检测网络质量
            if (mNetworkQualityCheck) {
                detectNetworkQuality();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting network state", e);
        }
    }
    
    /**
     * 添加优化监听器
     */
    public void addListener(@NonNull NetworkOptimizationListener listener) {
        synchronized (mListeners) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }
    
    /**
     * 移除优化监听器
     */
    public void removeListener(@NonNull NetworkOptimizationListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }
    
    /**
     * 获取离线缓存管理器
     */
    public OfflineCacheManager getOfflineCacheManager() {
        return mOfflineCacheManager;
    }
    
    /**
     * 获取网络质量描述
     */
    public String getNetworkQualityDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("网络类型: ").append(getNetworkTypeDescription(mCurrentNetworkType));
        desc.append(", 质量: ").append(getNetworkQualityDescription(mCurrentNetworkQuality));
        desc.append(", 数据节省: ").append(mDataSaverEnabled ? "启用" : "禁用");
        return desc.toString();
    }
    
    // 私有方法
    
    private void loadConfiguration() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        mDataSaverEnabled = prefs.getBoolean(KEY_DATA_SAVER_ENABLED, false);
        mOfflineCacheEnabled = prefs.getBoolean(KEY_OFFLINE_CACHE_ENABLED, true);
        mImageCompressionEnabled = prefs.getBoolean(KEY_IMAGE_COMPRESSION_ENABLED, true);
        mPreloadEnabled = prefs.getBoolean(KEY_PRELOAD_ENABLED, true);
        mNetworkQualityCheck = prefs.getBoolean(KEY_NETWORK_QUALITY_CHECK, true);
        mMonthlyDataLimitMB = prefs.getLong(KEY_MONTHLY_DATA_LIMIT, DEFAULT_MONTHLY_LIMIT_MB);
        mCurrentMonthUsage.set(prefs.getLong(KEY_CURRENT_MONTH_USAGE, 0));
        
        Log.d(TAG, String.format("Configuration loaded: DataSaver=%s, Cache=%s, Compression=%s", 
            mDataSaverEnabled, mOfflineCacheEnabled, mImageCompressionEnabled));
    }
    
    private void saveConfiguration() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_DATA_SAVER_ENABLED, mDataSaverEnabled)
            .putBoolean(KEY_OFFLINE_CACHE_ENABLED, mOfflineCacheEnabled)
            .putBoolean(KEY_IMAGE_COMPRESSION_ENABLED, mImageCompressionEnabled)
            .putBoolean(KEY_PRELOAD_ENABLED, mPreloadEnabled)
            .putBoolean(KEY_NETWORK_QUALITY_CHECK, mNetworkQualityCheck)
            .putLong(KEY_MONTHLY_DATA_LIMIT, mMonthlyDataLimitMB)
            .apply();
        
        Log.d(TAG, "Configuration saved");
    }
    
    private void saveCurrentUsage(long monthlyUsage) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putLong(KEY_CURRENT_MONTH_USAGE, monthlyUsage)
            .putLong(KEY_LAST_RESET_DATE, System.currentTimeMillis())
            .apply();
    }
    
    private void applyCacheStrategy(android.webkit.WebSettings settings, OptimizationConfig config) {
        if (config.enableOfflineCache) {
            // 根据网络类型设置缓存模式
            switch (mCurrentNetworkType) {
                case WIFI:
                case ETHERNET:
                    settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
                    break;
                case MOBILE_4G:
                case MOBILE_5G:
                    if (config.enableDataSaver) {
                        settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    } else {
                        settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
                    }
                    break;
                case MOBILE_2G:
                case MOBILE_3G:
                    settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    break;
                default:
                    settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    break;
            }
        } else {
            settings.setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
        }
        
        Log.d(TAG, "Cache strategy applied: " + settings.getCacheMode());
    }
    
    private void applyImageOptimization(android.webkit.WebSettings settings, OptimizationConfig config) {
        if (config.enableImageCompression) {
            // 根据网络类型和数据节省模式调整图片加载
            if (mCurrentNetworkType == NetworkType.MOBILE_2G || 
                (config.enableDataSaver && (mCurrentNetworkType == NetworkType.MOBILE_3G || 
                                          mCurrentNetworkType == NetworkType.MOBILE_4G))) {
                // 低速网络或数据节省模式：禁用图片加载
                settings.setBlockNetworkImage(true);
                Log.d(TAG, "Image loading blocked for data saving");
            } else {
                settings.setBlockNetworkImage(false);
                Log.d(TAG, "Image loading enabled");
            }
        } else {
            settings.setBlockNetworkImage(false);
        }
    }
    
    private void applyPreloadSettings(android.webkit.WebSettings settings, OptimizationConfig config) {
        // 根据预加载设置和网络类型调整
        boolean enablePreload = config.enablePreload && 
            (mCurrentNetworkType == NetworkType.WIFI || 
             mCurrentNetworkType == NetworkType.ETHERNET ||
             mCurrentNetworkType == NetworkType.MOBILE_5G);
        
        // 这里可以设置WebView的预加载相关参数
        // 由于Android WebView没有直接的预加载API，可以通过其他方式实现
        
        Log.d(TAG, "Preload settings applied: " + enablePreload);
    }
    
    private NetworkType detectNetworkType(@Nullable NetworkInfo networkInfo) {
        if (networkInfo == null || !networkInfo.isConnected()) {
            return NetworkType.UNKNOWN;
        }
        
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return NetworkType.WIFI;
            case ConnectivityManager.TYPE_ETHERNET:
                return NetworkType.ETHERNET;
            case ConnectivityManager.TYPE_MOBILE:
                return detectMobileNetworkType(networkInfo);
            default:
                return NetworkType.UNKNOWN;
        }
    }
    
    private NetworkType detectMobileNetworkType(NetworkInfo networkInfo) {
        // 这里可以根据networkInfo.getSubtype()进一步判断移动网络类型
        // 简化实现，默认返回4G
        return NetworkType.MOBILE_4G;
    }
    
    private void detectNetworkQuality() {
        // 这里可以实现网络质量检测逻辑
        // 简化实现，根据网络类型估算质量
        NetworkQuality newQuality;
        
        switch (mCurrentNetworkType) {
            case WIFI:
            case ETHERNET:
            case MOBILE_5G:
                newQuality = NetworkQuality.EXCELLENT;
                break;
            case MOBILE_4G:
                newQuality = NetworkQuality.GOOD;
                break;
            case MOBILE_3G:
                newQuality = NetworkQuality.FAIR;
                break;
            case MOBILE_2G:
                newQuality = NetworkQuality.POOR;
                break;
            default:
                newQuality = NetworkQuality.UNKNOWN;
                break;
        }
        
        if (newQuality != mCurrentNetworkQuality) {
            mCurrentNetworkQuality = newQuality;
            notifyNetworkQualityChanged(newQuality);
        }
    }
    
    private void startTrafficMonitoring() {
        if (mTrafficMonitor != null) {
            mTrafficMonitor.startMonitoring();
        }
    }
    
    private void checkAndResetMonthlyStats() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, System.currentTimeMillis());
        
        // 检查是否需要重置月度统计（简化：30天重置一次）
        long thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000;
        if (System.currentTimeMillis() - lastResetDate > thirtyDaysMillis) {
            Log.d(TAG, "Resetting monthly data usage statistics");
            mCurrentMonthUsage.set(0);
            saveCurrentUsage(0);
        }
    }
    
    private void checkDataLimitWarning(long monthlyUsage) {
        long limitBytes = mMonthlyDataLimitMB * 1024 * 1024;
        float usagePercentage = (float) monthlyUsage / limitBytes;
        
        // 在80%和95%时发出警告
        if (usagePercentage >= 0.95f || usagePercentage >= 0.8f) {
            notifyDataLimitWarning(usagePercentage);
        }
    }
    
    private String getNetworkTypeDescription(NetworkType type) {
        switch (type) {
            case WIFI: return "WiFi";
            case MOBILE_2G: return "2G";
            case MOBILE_3G: return "3G";
            case MOBILE_4G: return "4G";
            case MOBILE_5G: return "5G";
            case ETHERNET: return "以太网";
            default: return "未知";
        }
    }
    
    private String getNetworkQualityDescription(NetworkQuality quality) {
        switch (quality) {
            case EXCELLENT: return "优秀";
            case GOOD: return "良好";
            case FAIR: return "一般";
            case POOR: return "较差";
            default: return "未知";
        }
    }
    
    // 通知方法
    
    private void notifyNetworkTypeChanged(NetworkType oldType, NetworkType newType) {
        synchronized (mListeners) {
            for (NetworkOptimizationListener listener : mListeners) {
                try {
                    listener.onNetworkTypeChanged(oldType, newType);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying network type change", e);
                }
            }
        }
    }
    
    private void notifyNetworkQualityChanged(NetworkQuality quality) {
        synchronized (mListeners) {
            for (NetworkOptimizationListener listener : mListeners) {
                try {
                    listener.onNetworkQualityChanged(quality);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying network quality change", e);
                }
            }
        }
    }
    
    private void notifyDataUsageUpdate() {
        TrafficStats stats = getTrafficStats();
        synchronized (mListeners) {
            for (NetworkOptimizationListener listener : mListeners) {
                try {
                    listener.onDataUsageUpdate(stats.sessionUsageBytes, stats.monthlyUsageBytes, stats.monthlyLimitBytes);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying data usage update", e);
                }
            }
        }
    }
    
    private void notifyDataLimitWarning(float usagePercentage) {
        synchronized (mListeners) {
            for (NetworkOptimizationListener listener : mListeners) {
                try {
                    listener.onDataLimitWarning(usagePercentage);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying data limit warning", e);
                }
            }
        }
    }
    
    private void notifyOptimizationApplied(String description) {
        synchronized (mListeners) {
            for (NetworkOptimizationListener listener : mListeners) {
                try {
                    listener.onOptimizationApplied(description);
                } catch (Exception e) {
                    Log.w(TAG, "Error notifying optimization applied", e);
                }
            }
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        synchronized (mListeners) {
            mListeners.clear();
        }
        
        if (mOfflineCacheManager != null) {
            mOfflineCacheManager.cleanup();
        }
        
        if (mTrafficMonitor != null) {
            mTrafficMonitor.stopMonitoring();
        }
        
        Log.d(TAG, "NetworkOptimizationManager cleanup completed");
    }
}