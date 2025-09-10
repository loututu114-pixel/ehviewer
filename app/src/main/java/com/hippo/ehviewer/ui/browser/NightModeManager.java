package com.hippo.ehviewer.ui.browser;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * 夜间模式管理器
 * 参考YCWebView实现，支持自动和手动夜间模式切换
 */
public class NightModeManager {

    private static final String TAG = "NightModeManager";
    private static final String PREF_NAME = "night_mode_settings";
    private static final String KEY_NIGHT_MODE_ENABLED = "night_mode_enabled";
    private static final String KEY_AUTO_NIGHT_MODE = "auto_night_mode_enabled";
    
    // JavaScript代码用于实现网页夜间模式
    private static final String NIGHT_MODE_JS = 
        "(function() {" +
        "  var nightModeId = 'eh-night-mode-style';" +
        "  var existingStyle = document.getElementById(nightModeId);" +
        "  if (existingStyle) {" +
        "    existingStyle.remove();" +
        "  }" +
        "  var style = document.createElement('style');" +
        "  style.id = nightModeId;" +
        "  style.innerHTML = '" +
        "    html { filter: invert(1) hue-rotate(180deg) !important; }" +
        "    img, video, iframe, svg, embed, object { filter: invert(1) hue-rotate(180deg) !important; }" +
        "    [style*=\"background-image\"] { filter: invert(1) hue-rotate(180deg) !important; }" +
        "    input, textarea, select { background-color: #2b2b2b !important; color: #ffffff !important; }" +
        "  ';" +
        "  document.head.appendChild(style);" +
        "})();";
        
    // 移除夜间模式的JavaScript代码
    private static final String REMOVE_NIGHT_MODE_JS = 
        "(function() {" +
        "  var nightModeId = 'eh-night-mode-style';" +
        "  var existingStyle = document.getElementById(nightModeId);" +
        "  if (existingStyle) {" +
        "    existingStyle.remove();" +
        "  }" +
        "})();";
    
    private Context mContext;
    private SharedPreferences mPrefs;
    private boolean mNightModeEnabled = false;
    private boolean mAutoNightModeEnabled = false;
    
    public NightModeManager(Context context) {
        this.mContext = context;
        this.mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadSettings();
    }
    
    /**
     * 加载夜间模式设置
     */
    private void loadSettings() {
        mNightModeEnabled = mPrefs.getBoolean(KEY_NIGHT_MODE_ENABLED, false);
        mAutoNightModeEnabled = mPrefs.getBoolean(KEY_AUTO_NIGHT_MODE, true);
        
        android.util.Log.d(TAG, "Night mode settings loaded: enabled=" + mNightModeEnabled + 
                              ", auto=" + mAutoNightModeEnabled);
    }
    
    /**
     * 保存夜间模式设置
     */
    private void saveSettings() {
        mPrefs.edit()
            .putBoolean(KEY_NIGHT_MODE_ENABLED, mNightModeEnabled)
            .putBoolean(KEY_AUTO_NIGHT_MODE, mAutoNightModeEnabled)
            .apply();
        
        android.util.Log.d(TAG, "Night mode settings saved");
    }
    
    /**
     * 切换夜间模式
     */
    public void toggleNightMode() {
        setNightModeEnabled(!mNightModeEnabled);
    }
    
    /**
     * 设置夜间模式状态
     */
    public void setNightModeEnabled(boolean enabled) {
        if (mNightModeEnabled != enabled) {
            mNightModeEnabled = enabled;
            saveSettings();
            
            // 应用系统夜间模式
            if (mContext instanceof Activity) {
                applySystemNightMode(enabled);
            }
            
            android.util.Log.d(TAG, "Night mode " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 应用系统夜间模式
     */
    private void applySystemNightMode(boolean enabled) {
        int nightMode = enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
        
        // 注意：这里不需要调用Activity的delegate，因为MODE设置会自动应用
        android.util.Log.d(TAG, "System night mode applied: " + (enabled ? "NIGHT" : "DAY"));
    }
    
    /**
     * 为WebView应用夜间模式
     */
    public void applyNightModeToWebView(WebView webView) {
        if (webView == null) return;
        
        try {
            if (mNightModeEnabled) {
                // 应用夜间模式
                webView.evaluateJavascript(NIGHT_MODE_JS, null);
                android.util.Log.d(TAG, "Applied night mode to WebView");
            } else {
                // 移除夜间模式
                webView.evaluateJavascript(REMOVE_NIGHT_MODE_JS, null);
                android.util.Log.d(TAG, "Removed night mode from WebView");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to apply night mode to WebView", e);
        }
    }
    
    /**
     * 检查是否应该自动启用夜间模式
     */
    public boolean shouldAutoEnableNightMode() {
        if (!mAutoNightModeEnabled) return false;
        
        // 获取当前时间
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        // 夜间时间：22:00 - 06:00
        return hour >= 22 || hour < 6;
    }
    
    /**
     * 检查并应用自动夜间模式
     */
    public void checkAndApplyAutoNightMode() {
        if (mAutoNightModeEnabled) {
            boolean shouldEnable = shouldAutoEnableNightMode();
            if (shouldEnable != mNightModeEnabled) {
                setNightModeEnabled(shouldEnable);
                android.util.Log.d(TAG, "Auto night mode applied: " + shouldEnable);
            }
        }
    }
    
    /**
     * 设置自动夜间模式
     */
    public void setAutoNightModeEnabled(boolean enabled) {
        if (mAutoNightModeEnabled != enabled) {
            mAutoNightModeEnabled = enabled;
            saveSettings();
            
            if (enabled) {
                checkAndApplyAutoNightMode();
            }
            
            android.util.Log.d(TAG, "Auto night mode " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 是否启用夜间模式
     */
    public boolean isNightModeEnabled() {
        return mNightModeEnabled;
    }
    
    /**
     * 是否启用自动夜间模式
     */
    public boolean isAutoNightModeEnabled() {
        return mAutoNightModeEnabled;
    }
    
    /**
     * 获取夜间模式状态文本
     */
    public String getNightModeStatusText() {
        if (mNightModeEnabled) {
            return mAutoNightModeEnabled ? "夜间模式 (自动)" : "夜间模式 (手动)";
        } else {
            return mAutoNightModeEnabled ? "日间模式 (自动)" : "日间模式 (手动)";
        }
    }
    
    /**
     * 获取当前时间段描述
     */
    public String getTimeDescription() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        if (hour >= 6 && hour < 12) {
            return "上午";
        } else if (hour >= 12 && hour < 18) {
            return "下午";  
        } else if (hour >= 18 && hour < 22) {
            return "傍晚";
        } else {
            return "夜间";
        }
    }
}