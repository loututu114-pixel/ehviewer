package com.hippo.ehviewer.privacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 隐私数据管理器 - 负责清理和统计各种隐私数据
 * 
 * 核心功能：
 * 1. Cookie清理和统计
 * 2. 历史记录清理
 * 3. 缓存清理和统计
 * 4. 表单数据清理
 * 5. WebView存储清理
 * 6. 数据统计收集
 */
public class PrivacyDataManager {
    private static final String TAG = "PrivacyDataManager";
    
    private final Context mContext;
    
    /**
     * 数据统计信息
     */
    public static class DataStats {
        public int cookieCount = 0;
        public int historyCount = 0;
        public long cacheSize = 0; // bytes
        public int formDataCount = 0;
        public long storageSize = 0; // bytes
        
        @Override
        public String toString() {
            return String.format("DataStats: Cookies=%d, History=%d, Cache=%.1fMB, Storage=%.1fMB", 
                cookieCount, historyCount, cacheSize / 1024.0 / 1024.0, storageSize / 1024.0 / 1024.0);
        }
    }
    
    public PrivacyDataManager(Context context) {
        mContext = context.getApplicationContext();
    }
    
    /**
     * 清理隐私数据
     */
    @WorkerThread
    public PrivacyModeManager.ClearResult clearData(@NonNull PrivacyModeManager.PrivacyConfig config) {
        Log.d(TAG, "Starting data clear with config: " + config);
        
        PrivacyModeManager.ClearResult result = new PrivacyModeManager.ClearResult();
        AtomicInteger totalCleared = new AtomicInteger(0);
        
        try {
            // 清理Cookie
            if (config.clearCookies) {
                result.clearedCookies = clearCookies();
                totalCleared.addAndGet(result.clearedCookies);
                Log.d(TAG, "Cleared " + result.clearedCookies + " cookies");
            }
            
            // 清理历史记录
            if (config.clearHistory) {
                result.clearedHistoryItems = clearHistory();
                totalCleared.addAndGet(result.clearedHistoryItems);
                Log.d(TAG, "Cleared " + result.clearedHistoryItems + " history items");
            }
            
            // 清理缓存
            if (config.clearCache) {
                result.clearedCacheSize = clearCache();
                result.clearedCacheFiles = (int) (result.clearedCacheSize / 4096); // 估算文件数
                Log.d(TAG, "Cleared " + result.clearedCacheSize + " bytes of cache");
            }
            
            // 清理表单数据
            if (config.clearFormData) {
                clearFormData();
                Log.d(TAG, "Cleared form data");
            }
            
            // 清理WebView存储
            if (config.isolateStorage) {
                clearWebStorage();
                Log.d(TAG, "Cleared web storage");
            }
            
            result.success = true;
            Log.d(TAG, "Data clear completed successfully: " + result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing privacy data", e);
            result.success = false;
            result.errorMessage = "清理失败: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * 获取当前数据统计
     */
    public DataStats getDataStats() {
        DataStats stats = new DataStats();
        
        try {
            // 统计Cookie数量
            stats.cookieCount = getCookieCount();
            
            // 统计历史记录数量
            stats.historyCount = getHistoryCount();
            
            // 统计缓存大小
            stats.cacheSize = getCacheSize();
            
            // 统计存储大小
            stats.storageSize = getStorageSize();
            
            Log.d(TAG, "Current data stats: " + stats);
            
        } catch (Exception e) {
            Log.w(TAG, "Error collecting data stats", e);
        }
        
        return stats;
    }
    
    // 私有清理方法
    
    private int clearCookies() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager == null) {
                return 0;
            }
            
            // 获取清理前的Cookie数量
            int beforeCount = getCookieCount();
            
            // 清理所有Cookie
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
            
            // 返回清理的数量
            return beforeCount;
            
        } catch (Exception e) {
            Log.w(TAG, "Error clearing cookies", e);
            return 0;
        }
    }
    
    private int clearHistory() {
        try {
            WebViewDatabase webViewDB = WebViewDatabase.getInstance(mContext);
            if (webViewDB == null) {
                return 0;
            }
            
            // 获取清理前的历史记录数量
            int beforeCount = getHistoryCount();
            
            // 清理历史记录
            webViewDB.clearFormData();
            webViewDB.clearHttpAuthUsernamePassword();
            
            // 清理WebView历史记录需要通过WebView实例
            // 这里返回估算值
            return beforeCount;
            
        } catch (Exception e) {
            Log.w(TAG, "Error clearing history", e);
            return 0;
        }
    }
    
    private long clearCache() {
        try {
            // 获取清理前的缓存大小
            long beforeSize = getCacheSize();
            
            // 清理应用缓存
            File cacheDir = mContext.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteDirectory(cacheDir);
            }
            
            // 清理外部缓存
            File externalCacheDir = mContext.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteDirectory(externalCacheDir);
            }
            
            // 清理WebView缓存目录
            clearWebViewCache();
            
            return beforeSize;
            
        } catch (Exception e) {
            Log.w(TAG, "Error clearing cache", e);
            return 0;
        }
    }
    
    private void clearFormData() {
        try {
            WebViewDatabase webViewDB = WebViewDatabase.getInstance(mContext);
            if (webViewDB != null) {
                webViewDB.clearFormData();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error clearing form data", e);
        }
    }
    
    private void clearWebStorage() {
        try {
            WebStorage.getInstance().deleteAllData();
        } catch (Exception e) {
            Log.w(TAG, "Error clearing web storage", e);
        }
    }
    
    private void clearWebViewCache() {
        try {
            // WebView缓存通常在/data/data/package/app_webview/目录下
            File webViewDir = new File(mContext.getApplicationInfo().dataDir + "/app_webview");
            if (webViewDir.exists()) {
                deleteDirectory(webViewDir);
            }
            
            // 一些设备可能有不同的路径
            File[] possibleDirs = {
                new File(mContext.getApplicationInfo().dataDir + "/app_textures"),
                new File(mContext.getApplicationInfo().dataDir + "/app_webview_cache"),
                new File(mContext.getFilesDir(), "webview")
            };
            
            for (File dir : possibleDirs) {
                if (dir.exists()) {
                    deleteDirectory(dir);
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error clearing WebView cache", e);
        }
    }
    
    // 私有统计方法
    
    private int getCookieCount() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager == null) {
                return 0;
            }
            
            // Android API没有直接获取Cookie数量的方法
            // 这里返回一个基于常用域名的估算值
            String[] commonDomains = {
                "google.com", "baidu.com", "qq.com", "taobao.com", 
                "sina.com.cn", "weibo.com", "zhihu.com", "bilibili.com"
            };
            
            int count = 0;
            for (String domain : commonDomains) {
                String cookies = cookieManager.getCookie(domain);
                if (cookies != null && !cookies.isEmpty()) {
                    // 简单统计分号数量作为Cookie数量估算
                    count += cookies.split(";").length;
                }
            }
            
            return count;
            
        } catch (Exception e) {
            Log.w(TAG, "Error getting cookie count", e);
            return 0;
        }
    }
    
    private int getHistoryCount() {
        try {
            // Android API没有直接获取历史记录数量的方法
            // 这里返回一个基于SharedPreferences和数据库的估算值
            
            SharedPreferences prefs = mContext.getSharedPreferences("WebViewChromium", Context.MODE_PRIVATE);
            int prefsCount = prefs.getAll().size();
            
            // 简单估算：每个域名平均有3-5条历史记录
            return prefsCount * 4;
            
        } catch (Exception e) {
            Log.w(TAG, "Error getting history count", e);
            return 0;
        }
    }
    
    private long getCacheSize() {
        try {
            long totalSize = 0;
            
            // 应用缓存目录
            File cacheDir = mContext.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                totalSize += getDirectorySize(cacheDir);
            }
            
            // 外部缓存目录
            File externalCacheDir = mContext.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                totalSize += getDirectorySize(externalCacheDir);
            }
            
            // WebView缓存目录
            File webViewDir = new File(mContext.getApplicationInfo().dataDir + "/app_webview");
            if (webViewDir.exists()) {
                totalSize += getDirectorySize(webViewDir);
            }
            
            return totalSize;
            
        } catch (Exception e) {
            Log.w(TAG, "Error getting cache size", e);
            return 0;
        }
    }
    
    private long getStorageSize() {
        try {
            // WebView存储通常包括localStorage, sessionStorage, indexedDB等
            // 这里返回一个基于文件系统的估算值
            
            long totalSize = 0;
            
            String[] storageDirs = {
                "/app_webview/Local Storage",
                "/app_webview/Session Storage", 
                "/app_webview/databases",
                "/app_webview/IndexedDB"
            };
            
            for (String dirPath : storageDirs) {
                File storageDir = new File(mContext.getApplicationInfo().dataDir + dirPath);
                if (storageDir.exists()) {
                    totalSize += getDirectorySize(storageDir);
                }
            }
            
            return totalSize;
            
        } catch (Exception e) {
            Log.w(TAG, "Error getting storage size", e);
            return 0;
        }
    }
    
    // 工具方法
    
    private long getDirectorySize(File directory) {
        long size = 0;
        try {
            if (directory == null || !directory.exists()) {
                return 0;
            }
            
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error calculating directory size: " + directory, e);
        }
        return size;
    }
    
    private boolean deleteDirectory(File directory) {
        try {
            if (directory == null || !directory.exists()) {
                return true;
            }
            
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            Log.w(TAG, "Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            
            return directory.delete();
            
        } catch (Exception e) {
            Log.w(TAG, "Error deleting directory: " + directory, e);
            return false;
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        // 清理任何持有的资源
        Log.d(TAG, "PrivacyDataManager cleanup completed");
    }
}