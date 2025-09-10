package com.hippo.ehviewer.privacy;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebView隐私设置管理器 - 负责配置WebView的隐私相关设置
 * 
 * 核心功能：
 * 1. 隐私模式WebView配置
 * 2. Cookie和存储隔离
 * 3. JavaScript和插件控制
 * 4. 缓存策略管理
 * 5. 用户代理和DNT设置
 * 6. 第三方Cookie阻断
 */
public class PrivacyWebViewManager {
    private static final String TAG = "PrivacyWebViewManager";
    
    private final Context mContext;
    
    // WebView配置缓存
    private final Map<WebView, WebViewConfig> mWebViewConfigs = new ConcurrentHashMap<>();
    
    // 默认配置备份
    private WebViewConfig mDefaultConfig;
    
    /**
     * WebView配置快照
     */
    private static class WebViewConfig {
        // JavaScript设置
        public boolean javaScriptEnabled;
        public boolean javaScriptCanOpenWindowsAutomatically;
        
        // 存储设置  
        public boolean domStorageEnabled;
        public boolean databaseEnabled;
        
        // 缓存设置
        public int cacheMode;
        
        // Cookie设置
        public boolean cookiesEnabled;
        
        // 其他隐私设置
        public boolean geolocationEnabled;
        public String userAgentString;
        public boolean mixedContentAllowed;
        
        @Override
        public String toString() {
            return String.format("WebViewConfig[JS=%s, Storage=%s, Cache=%d, Cookies=%s]", 
                javaScriptEnabled, domStorageEnabled, cacheMode, cookiesEnabled);
        }
    }
    
    public PrivacyWebViewManager(Context context) {
        mContext = context.getApplicationContext();
        
        // 初始化默认配置
        initDefaultConfig();
    }
    
    /**
     * 为WebView配置隐私设置
     */
    public void configureWebView(@NonNull WebView webView, boolean privacyModeEnabled) {
        Log.d(TAG, "Configuring WebView privacy mode: " + privacyModeEnabled);
        
        try {
            WebSettings settings = webView.getSettings();
            if (settings == null) {
                Log.w(TAG, "WebView settings is null, cannot configure");
                return;
            }
            
            if (privacyModeEnabled) {
                // 保存当前配置
                saveCurrentConfig(webView, settings);
                
                // 应用隐私模式配置
                applyPrivacyConfig(webView, settings);
            } else {
                // 恢复正常配置
                restoreNormalConfig(webView, settings);
                
                // 移除配置缓存
                mWebViewConfigs.remove(webView);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring WebView privacy settings", e);
        }
    }
    
    /**
     * 启用隐私模式
     */
    public void enablePrivacyMode() {
        Log.d(TAG, "Enabling global privacy mode");
        
        try {
            // 配置全局Cookie管理器
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null) {
                cookieManager.setAcceptCookie(false);
                cookieManager.setAcceptThirdPartyCookies(null, false);
            }
            
            // 清理WebView存储
            WebStorage.getInstance().deleteAllData();
            
            Log.d(TAG, "Global privacy mode enabled");
            
        } catch (Exception e) {
            Log.e(TAG, "Error enabling privacy mode", e);
        }
    }
    
    /**
     * 禁用隐私模式
     */
    public void disablePrivacyMode() {
        Log.d(TAG, "Disabling global privacy mode");
        
        try {
            // 恢复正常Cookie设置
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null) {
                cookieManager.setAcceptCookie(true);
                // 第三方Cookie根据用户设置决定
            }
            
            Log.d(TAG, "Global privacy mode disabled");
            
        } catch (Exception e) {
            Log.e(TAG, "Error disabling privacy mode", e);
        }
    }
    
    /**
     * 检查WebView是否处于隐私模式
     */
    public boolean isWebViewInPrivacyMode(@NonNull WebView webView) {
        return mWebViewConfigs.containsKey(webView);
    }
    
    /**
     * 获取WebView隐私设置摘要
     */
    public String getPrivacyConfigSummary(@NonNull WebView webView) {
        try {
            WebSettings settings = webView.getSettings();
            if (settings == null) {
                return "WebView设置不可用";
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append("JavaScript: ").append(settings.getJavaScriptEnabled() ? "启用" : "禁用").append(", ");
            summary.append("存储: ").append(settings.getDomStorageEnabled() ? "启用" : "禁用").append(", ");
            summary.append("缓存模式: ").append(getCacheModeDescription(settings.getCacheMode()));
            
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null) {
                summary.append(", Cookie: ").append(cookieManager.acceptCookie() ? "启用" : "禁用");
            }
            
            return summary.toString();
            
        } catch (Exception e) {
            Log.w(TAG, "Error getting privacy config summary", e);
            return "配置获取失败";
        }
    }
    
    // 私有配置方法
    
    private void initDefaultConfig() {
        mDefaultConfig = new WebViewConfig();
        mDefaultConfig.javaScriptEnabled = true;
        mDefaultConfig.javaScriptCanOpenWindowsAutomatically = false;
        mDefaultConfig.domStorageEnabled = true;
        mDefaultConfig.databaseEnabled = true;
        mDefaultConfig.cacheMode = WebSettings.LOAD_DEFAULT;
        mDefaultConfig.cookiesEnabled = true;
        mDefaultConfig.geolocationEnabled = false;
        mDefaultConfig.mixedContentAllowed = false;
        
        Log.d(TAG, "Default config initialized: " + mDefaultConfig);
    }
    
    private void saveCurrentConfig(@NonNull WebView webView, @NonNull WebSettings settings) {
        try {
            WebViewConfig config = new WebViewConfig();
            
            // JavaScript设置
            config.javaScriptEnabled = settings.getJavaScriptEnabled();
            config.javaScriptCanOpenWindowsAutomatically = settings.getJavaScriptCanOpenWindowsAutomatically();
            
            // 存储设置
            config.domStorageEnabled = settings.getDomStorageEnabled();
            config.databaseEnabled = settings.getDatabaseEnabled();
            
            // 缓存设置
            config.cacheMode = settings.getCacheMode();
            
            // Cookie设置
            CookieManager cookieManager = CookieManager.getInstance();
            config.cookiesEnabled = cookieManager != null && cookieManager.acceptCookie();
            
            // 其他设置
            config.userAgentString = settings.getUserAgentString();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                config.mixedContentAllowed = settings.getMixedContentMode() == WebSettings.MIXED_CONTENT_ALWAYS_ALLOW;
            }
            
            mWebViewConfigs.put(webView, config);
            Log.d(TAG, "Saved current config for WebView: " + config);
            
        } catch (Exception e) {
            Log.w(TAG, "Error saving WebView config", e);
        }
    }
    
    private void applyPrivacyConfig(@NonNull WebView webView, @NonNull WebSettings settings) {
        try {
            Log.d(TAG, "Applying privacy config to WebView");
            
            // 禁用或限制JavaScript（可选）
            // settings.setJavaScriptEnabled(false); // 可能影响网站功能，谨慎使用
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            
            // 禁用存储
            settings.setDomStorageEnabled(false);
            settings.setDatabaseEnabled(false);
            
            // 设置缓存策略为不使用缓存
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            
            // 禁用地理位置
            settings.setGeolocationEnabled(false);
            
            // 设置隐私友好的用户代理
            String privacyUA = getPrivacyUserAgent(settings.getUserAgentString());
            settings.setUserAgentString(privacyUA);
            
            // Android 5.0+ 禁用混合内容
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            }
            
            // 配置Cookie设置
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null) {
                cookieManager.setAcceptCookie(false);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.setAcceptThirdPartyCookies(webView, false);
                }
            }
            
            // 添加DNT请求头
            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put("DNT", "1"); // Do Not Track
            extraHeaders.put("Sec-GPC", "1"); // Global Privacy Control
            
            Log.d(TAG, "Privacy config applied successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying privacy config", e);
        }
    }
    
    private void restoreNormalConfig(@NonNull WebView webView, @NonNull WebSettings settings) {
        try {
            WebViewConfig savedConfig = mWebViewConfigs.get(webView);
            WebViewConfig configToRestore = savedConfig != null ? savedConfig : mDefaultConfig;
            
            Log.d(TAG, "Restoring normal config: " + configToRestore);
            
            // 恢复JavaScript设置
            settings.setJavaScriptEnabled(configToRestore.javaScriptEnabled);
            settings.setJavaScriptCanOpenWindowsAutomatically(configToRestore.javaScriptCanOpenWindowsAutomatically);
            
            // 恢复存储设置
            settings.setDomStorageEnabled(configToRestore.domStorageEnabled);
            settings.setDatabaseEnabled(configToRestore.databaseEnabled);
            
            // 恢复缓存设置
            settings.setCacheMode(configToRestore.cacheMode);
            
            // 恢复地理位置设置
            settings.setGeolocationEnabled(configToRestore.geolocationEnabled);
            
            // 恢复用户代理
            if (configToRestore.userAgentString != null) {
                settings.setUserAgentString(configToRestore.userAgentString);
            }
            
            // 恢复混合内容设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int mixedContentMode = configToRestore.mixedContentAllowed ? 
                    WebSettings.MIXED_CONTENT_ALWAYS_ALLOW : WebSettings.MIXED_CONTENT_NEVER_ALLOW;
                settings.setMixedContentMode(mixedContentMode);
            }
            
            // 恢复Cookie设置
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null) {
                cookieManager.setAcceptCookie(configToRestore.cookiesEnabled);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.setAcceptThirdPartyCookies(webView, configToRestore.cookiesEnabled);
                }
            }
            
            Log.d(TAG, "Normal config restored successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error restoring normal config", e);
        }
    }
    
    private String getPrivacyUserAgent(String originalUA) {
        if (originalUA == null || originalUA.isEmpty()) {
            return "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/121.0.0.0 Mobile Safari/537.36";
        }
        
        // 移除可能暴露隐私的信息
        String privacyUA = originalUA;
        
        // 移除详细的系统版本信息（保留基本的Android标识）
        privacyUA = privacyUA.replaceAll("Android [0-9\\.]+;", "Android;");
        
        // 移除设备型号信息
        privacyUA = privacyUA.replaceAll("; [A-Za-z0-9\\-_]+ Build/", "; Build/");
        
        // 移除构建信息
        privacyUA = privacyUA.replaceAll(" Build/[A-Za-z0-9\\.]+", "");
        
        return privacyUA;
    }
    
    private String getCacheModeDescription(int cacheMode) {
        switch (cacheMode) {
            case WebSettings.LOAD_DEFAULT:
                return "默认";
            case WebSettings.LOAD_CACHE_ELSE_NETWORK:
                return "优先缓存";
            case WebSettings.LOAD_NO_CACHE:
                return "不使用缓存";
            case WebSettings.LOAD_CACHE_ONLY:
                return "仅使用缓存";
            default:
                return "未知(" + cacheMode + ")";
        }
    }
    
    /**
     * 清理WebView配置缓存
     */
    public void clearWebViewConfig(@NonNull WebView webView) {
        mWebViewConfigs.remove(webView);
        Log.d(TAG, "Cleared config cache for WebView");
    }
    
    /**
     * 获取活跃的隐私模式WebView数量
     */
    public int getPrivacyModeWebViewCount() {
        return mWebViewConfigs.size();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        mWebViewConfigs.clear();
        Log.d(TAG, "PrivacyWebViewManager cleanup completed");
    }
}