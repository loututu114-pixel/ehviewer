package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;

import java.io.File;

public class IncognitoModeManager {
    private static final String TAG = "IncognitoModeManager";
    
    private static IncognitoModeManager instance;
    private boolean isIncognitoMode = false;
    private Context context;
    
    private IncognitoModeManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized IncognitoModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new IncognitoModeManager(context);
        }
        return instance;
    }
    
    // 配置WebView为隐私模式
    public void configureWebViewForIncognito(WebView webView) {
        if (webView == null) return;
        
        WebSettings settings = webView.getSettings();
        
        // 禁用缓存
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        
        // 禁用Cookie
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        }
        CookieManager.getInstance().setAcceptCookie(false);
        
        // 禁用表单数据保存
        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        
        // 禁用DOM存储
        settings.setDomStorageEnabled(false);
        
        // 禁用数据库
        settings.setDatabaseEnabled(false);
        
        // 禁用地理位置
        settings.setGeolocationEnabled(false);
        
        // 清除WebView缓存
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        
        Log.d(TAG, "WebView configured for incognito mode");
    }
    
    // 配置普通模式WebView
    public void configureWebViewForNormal(WebView webView) {
        if (webView == null) return;
        
        WebSettings settings = webView.getSettings();
        
        // 启用缓存
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 启用Cookie
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);
        
        // 启用表单数据保存
        settings.setSaveFormData(true);
        
        // 启用DOM存储
        settings.setDomStorageEnabled(true);
        
        // 启用数据库
        settings.setDatabaseEnabled(true);
        
        // 启用地理位置
        settings.setGeolocationEnabled(true);
        
        Log.d(TAG, "WebView configured for normal mode");
    }
    
    // 进入隐私模式
    public void enterIncognitoMode() {
        isIncognitoMode = true;
        
        // 清除所有Cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        
        // 清除WebStorage
        WebStorage.getInstance().deleteAllData();
        
        // 清除应用缓存
        clearAppCache();
        
        Log.d(TAG, "Entered incognito mode");
    }
    
    // 退出隐私模式
    public void exitIncognitoMode() {
        isIncognitoMode = false;
        
        // 清除隐私模式期间的所有数据
        clearIncognitoData();
        
        Log.d(TAG, "Exited incognito mode");
    }
    
    // 清除隐私模式数据
    public void clearIncognitoData() {
        // 清除Cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        
        // 清除WebStorage
        WebStorage.getInstance().deleteAllData();
        
        // 清除缓存
        clearAppCache();
        
        // 清除WebView数据库
        clearWebViewDatabase();
        
        Log.d(TAG, "Incognito data cleared");
    }
    
    // 清除应用缓存
    private void clearAppCache() {
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteDir(cacheDir);
            }
            
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.isDirectory()) {
                deleteDir(externalCacheDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing app cache", e);
        }
    }
    
    // 清除WebView数据库
    private void clearWebViewDatabase() {
        try {
            // 清除WebView数据目录
            File webViewCacheDir = new File(context.getFilesDir().getParent() + "/app_webview");
            if (webViewCacheDir.exists() && webViewCacheDir.isDirectory()) {
                deleteDir(webViewCacheDir);
            }
            
            // 清除数据库目录
            File databaseDir = new File(context.getFilesDir().getParent() + "/databases");
            if (databaseDir.exists() && databaseDir.isDirectory()) {
                deleteDir(databaseDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing WebView database", e);
        }
    }
    
    // 递归删除目录
    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
    
    // 是否为隐私模式
    public boolean isIncognitoMode() {
        return isIncognitoMode;
    }
    
    // 设置隐私模式
    public void setIncognitoMode(boolean incognito) {
        if (incognito != isIncognitoMode) {
            if (incognito) {
                enterIncognitoMode();
            } else {
                exitIncognitoMode();
            }
        }
    }
    
    // 创建隐私模式提示信息
    public static class IncognitoInfo {
        public static final String TITLE = "您正在使用隐私浏览模式";
        public static final String MESSAGE = 
            "在隐私模式下：\n" +
            "• 不会保存浏览历史记录\n" +
            "• 不会保存Cookie和网站数据\n" +
            "• 不会保存表单填写信息\n" +
            "• 下载的文件仍会保存到设备\n" +
            "• 您的活动仍可能对网站、雇主或网络服务提供商可见";
        
        public static final String EXIT_MESSAGE = 
            "退出隐私模式后，所有隐私标签页将被关闭，浏览数据将被清除。";
    }
}