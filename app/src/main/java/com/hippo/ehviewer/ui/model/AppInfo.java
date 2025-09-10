package com.hippo.ehviewer.ui.model;

import android.graphics.drawable.Drawable;

/**
 * 应用信息数据模型
 */
public class AppInfo {
    
    // 基本信息
    public String packageName;
    public String appName;
    public String versionName;
    public long versionCode;
    public Drawable icon;
    
    // 时间信息
    public long firstInstallTime;
    public long lastUpdateTime;
    
    // 文件信息
    public String apkPath;
    public long apkSize;
    
    // 应用属性
    public boolean isSystemApp;
    public boolean isUserApp;
    public boolean canLaunch;
    public boolean isEnabled;
    
    // 权限信息
    public String[] permissions;
    public int targetSdkVersion;
    public int minSdkVersion;
    
    // 使用统计 (可选)
    public long lastUsedTime;
    public long totalTimeInForeground;
    
    @Override
    public String toString() {
        return appName + " (" + packageName + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AppInfo appInfo = (AppInfo) obj;
        return packageName != null ? packageName.equals(appInfo.packageName) : appInfo.packageName == null;
    }
    
    @Override
    public int hashCode() {
        return packageName != null ? packageName.hashCode() : 0;
    }
}