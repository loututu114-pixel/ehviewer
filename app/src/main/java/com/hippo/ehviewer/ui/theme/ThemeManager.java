package com.hippo.ehviewer.ui.theme;

import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * 主题管理器
 * 提供夜间模式支持和自动适应系统主题功能
 * 
 * 核心特性：
 * 1. 自动跟随系统夜间模式
 * 2. 手动主题切换
 * 3. WebView深色模式适配
 * 4. 主题状态监听
 */
public class ThemeManager {
    private static final String TAG = "ThemeManager";
    
    // SharedPreferences键值
    private static final String PREFS_NAME = "theme_settings";
    private static final String PREF_THEME_MODE = "theme_mode";
    private static final String PREF_AUTO_THEME = "auto_theme";
    private static final String PREF_WEBVIEW_DARK_MODE = "webview_dark_mode";
    
    // 主题模式
    public enum ThemeMode {
        LIGHT(0, AppCompatDelegate.MODE_NIGHT_NO),
        DARK(1, AppCompatDelegate.MODE_NIGHT_YES),
        AUTO(2, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        public final int value;
        public final int delegateMode;
        
        ThemeMode(int value, int delegateMode) {
            this.value = value;
            this.delegateMode = delegateMode;
        }
        
        public static ThemeMode fromValue(int value) {
            for (ThemeMode mode : values()) {
                if (mode.value == value) {
                    return mode;
                }
            }
            return AUTO; // 默认自动模式
        }
    }
    
    // 单例实例
    private static volatile ThemeManager sInstance;
    
    // 组件引用
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final UiModeManager mUiModeManager;
    
    // 当前状态
    private ThemeMode mCurrentThemeMode = ThemeMode.AUTO;
    private boolean mIsDarkMode = false;
    private boolean mWebViewDarkModeEnabled = true;
    
    // 主题变化监听器
    private ThemeChangeListener mThemeChangeListener;
    
    /**
     * 主题变化监听器接口
     */
    public interface ThemeChangeListener {
        void onThemeChanged(boolean isDarkMode, ThemeMode themeMode);
        void onWebViewDarkModeChanged(boolean enabled);
    }
    
    /**
     * 获取单例实例
     */
    public static ThemeManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (ThemeManager.class) {
                if (sInstance == null) {
                    sInstance = new ThemeManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }
    
    private ThemeManager(@NonNull Context context) {
        mContext = context;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        
        // 加载保存的设置
        loadSettings();
        
        // 初始化主题
        initializeTheme();
        
        Log.d(TAG, "ThemeManager initialized with mode: " + mCurrentThemeMode + 
               ", isDark: " + mIsDarkMode);
    }
    
    /**
     * 加载保存的设置
     */
    private void loadSettings() {
        int themeValue = mPrefs.getInt(PREF_THEME_MODE, ThemeMode.AUTO.value);
        mCurrentThemeMode = ThemeMode.fromValue(themeValue);
        mWebViewDarkModeEnabled = mPrefs.getBoolean(PREF_WEBVIEW_DARK_MODE, true);
        
        Log.d(TAG, "Loaded settings: theme=" + mCurrentThemeMode + 
               ", webViewDark=" + mWebViewDarkModeEnabled);
    }
    
    /**
     * 初始化主题
     */
    private void initializeTheme() {
        // 设置AppCompat主题模式
        AppCompatDelegate.setDefaultNightMode(mCurrentThemeMode.delegateMode);
        
        // 更新当前夜间模式状态
        updateDarkModeState();
        
        Log.d(TAG, "Theme initialized: " + mCurrentThemeMode.delegateMode);
    }
    
    /**
     * 更新夜间模式状态
     */
    private void updateDarkModeState() {
        boolean newDarkMode = false;
        
        switch (mCurrentThemeMode) {
            case LIGHT:
                newDarkMode = false;
                break;
            case DARK:
                newDarkMode = true;
                break;
            case AUTO:
                newDarkMode = isSystemInDarkMode();
                break;
        }
        
        if (mIsDarkMode != newDarkMode) {
            mIsDarkMode = newDarkMode;
            Log.d(TAG, "Dark mode state changed to: " + mIsDarkMode);
            
            // 通知监听器
            if (mThemeChangeListener != null) {
                mThemeChangeListener.onThemeChanged(mIsDarkMode, mCurrentThemeMode);
            }
        }
    }
    
    /**
     * 检查系统是否处于夜间模式
     */
    private boolean isSystemInDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+：使用UiModeManager
            return (mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES);
        } else {
            // Android 10以下：使用Configuration
            int nightModeFlags = mContext.getResources().getConfiguration().uiMode & 
                                Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }
    }
    
    /**
     * 设置主题模式
     */
    public void setThemeMode(@NonNull ThemeMode themeMode) {
        if (mCurrentThemeMode != themeMode) {
            Log.d(TAG, "Setting theme mode: " + mCurrentThemeMode + " -> " + themeMode);
            
            mCurrentThemeMode = themeMode;
            
            // 保存设置
            saveThemeMode();
            
            // 应用主题
            AppCompatDelegate.setDefaultNightMode(themeMode.delegateMode);
            
            // 更新状态
            updateDarkModeState();
        }
    }
    
    /**
     * 获取当前主题模式
     */
    @NonNull
    public ThemeMode getCurrentThemeMode() {
        return mCurrentThemeMode;
    }
    
    /**
     * 是否处于夜间模式
     */
    public boolean isDarkMode() {
        return mIsDarkMode;
    }
    
    /**
     * 切换主题模式
     * LIGHT -> DARK -> AUTO -> LIGHT
     */
    public void toggleTheme() {
        ThemeMode nextMode;
        switch (mCurrentThemeMode) {
            case LIGHT:
                nextMode = ThemeMode.DARK;
                break;
            case DARK:
                nextMode = ThemeMode.AUTO;
                break;
            case AUTO:
            default:
                nextMode = ThemeMode.LIGHT;
                break;
        }
        setThemeMode(nextMode);
    }
    
    /**
     * 设置WebView深色模式
     */
    public void setWebViewDarkModeEnabled(boolean enabled) {
        if (mWebViewDarkModeEnabled != enabled) {
            Log.d(TAG, "Setting WebView dark mode: " + mWebViewDarkModeEnabled + " -> " + enabled);
            
            mWebViewDarkModeEnabled = enabled;
            
            // 保存设置
            saveWebViewDarkMode();
            
            // 通知监听器
            if (mThemeChangeListener != null) {
                mThemeChangeListener.onWebViewDarkModeChanged(enabled);
            }
        }
    }
    
    /**
     * 是否启用WebView深色模式
     */
    public boolean isWebViewDarkModeEnabled() {
        return mWebViewDarkModeEnabled;
    }
    
    /**
     * 是否应该应用WebView深色模式
     * 只有在夜间模式且启用WebView深色模式时才应该应用
     */
    public boolean shouldApplyWebViewDarkMode() {
        return mIsDarkMode && mWebViewDarkModeEnabled;
    }
    
    /**
     * 设置主题变化监听器
     */
    public void setThemeChangeListener(ThemeChangeListener listener) {
        mThemeChangeListener = listener;
    }
    
    /**
     * 处理配置变化（系统主题变化）
     */
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "Configuration changed, checking theme state");
        
        if (mCurrentThemeMode == ThemeMode.AUTO) {
            // 自动模式下，检查系统主题变化
            updateDarkModeState();
        }
    }
    
    /**
     * 获取主题描述
     */
    public String getThemeDescription() {
        switch (mCurrentThemeMode) {
            case LIGHT:
                return "浅色主题";
            case DARK:
                return "深色主题";
            case AUTO:
                return "跟随系统 (" + (mIsDarkMode ? "深色" : "浅色") + ")";
            default:
                return "未知主题";
        }
    }
    
    /**
     * 获取主题统计信息
     */
    public String getThemeStats() {
        return String.format("主题模式: %s\n当前状态: %s\nWebView深色: %s\n系统支持: %s",
                           mCurrentThemeMode,
                           mIsDarkMode ? "深色" : "浅色",
                           mWebViewDarkModeEnabled ? "启用" : "禁用",
                           Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? "完全支持" : "基础支持");
    }
    
    /**
     * 保存主题模式设置
     */
    private void saveThemeMode() {
        mPrefs.edit()
              .putInt(PREF_THEME_MODE, mCurrentThemeMode.value)
              .apply();
    }
    
    /**
     * 保存WebView深色模式设置
     */
    private void saveWebViewDarkMode() {
        mPrefs.edit()
              .putBoolean(PREF_WEBVIEW_DARK_MODE, mWebViewDarkModeEnabled)
              .apply();
    }
}