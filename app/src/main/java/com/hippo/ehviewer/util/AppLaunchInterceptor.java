package com.hippo.ehviewer.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.HashSet;
import java.util.Set;

/**
 * 应用启动拦截器
 * 处理其他应用尝试打开浏览器时的拦截和永久禁止功能
 *
 * 核心特性：
 * 1. 拦截其他应用打开浏览器的请求
 * 2. 提供永久禁止特定应用的选项
 * 3. 白名单机制
 * 4. 用户友好提示
 */
public class AppLaunchInterceptor {
    private static final String TAG = "AppLaunchInterceptor";

    // SharedPreferences键值
    private static final String PREFS_NAME = "app_launch_interceptor";
    private static final String PREF_BLOCKED_PACKAGES = "blocked_packages";
    private static final String PREF_WHITELIST_PACKAGES = "whitelist_packages";
    private static final String PREF_INTERCEPT_ENABLED = "intercept_enabled";

    // 组件引用
    private final Context mContext;
    private final SharedPreferences mPrefs;

    // 拦截状态
    private boolean mInterceptEnabled = true;
    private final Set<String> mBlockedPackages = new HashSet<>();
    private final Set<String> mWhitelistPackages = new HashSet<>();

    /**
     * 构造函数
     */
    public AppLaunchInterceptor(Context context) {
        mContext = context;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadSettings();
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        mInterceptEnabled = mPrefs.getBoolean(PREF_INTERCEPT_ENABLED, true);
        mBlockedPackages.addAll(mPrefs.getStringSet(PREF_BLOCKED_PACKAGES, new HashSet<>()));
        mWhitelistPackages.addAll(mPrefs.getStringSet(PREF_WHITELIST_PACKAGES, new HashSet<>()));
    }

    /**
     * 保存设置
     */
    private void saveSettings() {
        mPrefs.edit()
            .putBoolean(PREF_INTERCEPT_ENABLED, mInterceptEnabled)
            .putStringSet(PREF_BLOCKED_PACKAGES, new HashSet<>(mBlockedPackages))
            .putStringSet(PREF_WHITELIST_PACKAGES, new HashSet<>(mWhitelistPackages))
            .apply();
    }

    /**
     * 处理应用启动意图
     */
    public boolean handleAppLaunch(Intent intent) {
        if (!mInterceptEnabled) {
            return false; // 不拦截，直接处理
        }

        String callingPackage = getCallingPackage(intent);
        if (callingPackage == null) {
            return false; // 无法获取调用包名，直接处理
        }

        // 检查是否在白名单中
        if (mWhitelistPackages.contains(callingPackage)) {
            Log.d(TAG, "Package in whitelist: " + callingPackage);
            return false;
        }

        // 检查是否在黑名单中
        if (mBlockedPackages.contains(callingPackage)) {
            Log.d(TAG, "Package blocked: " + callingPackage);
            showBlockedMessage(callingPackage);
            return true; // 拦截，不处理
        }

        // 显示拦截对话框
        showInterceptDialog(callingPackage, intent);
        return true; // 先拦截，等待用户选择
    }

    /**
     * 获取调用包名
     */
    private String getCallingPackage(Intent intent) {
        // 尝试从Intent中获取调用信息
        String callingPackage = intent.getStringExtra("calling_package");
        if (callingPackage != null) {
            return callingPackage;
        }

        // 尝试从Uri中提取包名信息
        Uri data = intent.getData();
        if (data != null) {
            String scheme = data.getScheme();
            if ("market".equals(scheme) || "intent".equals(scheme)) {
                return data.getHost();
            }
        }

        return null;
    }

    /**
     * 显示拦截对话框
     */
    private void showInterceptDialog(String packageName, Intent originalIntent) {
        String appName = getAppName(packageName);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("应用拦截提示")
               .setMessage(appName + " 尝试打开浏览器\n\n是否允许此操作？")
               .setPositiveButton("允许", (dialog, which) -> {
                   // 添加到白名单
                   mWhitelistPackages.add(packageName);
                   saveSettings();

                   // 处理原始意图
                   handleIntent(originalIntent);
               })
               .setNegativeButton("拒绝", (dialog, which) -> {
                   // 不做任何操作
                   Toast.makeText(mContext, "已拒绝 " + appName + " 的请求", Toast.LENGTH_SHORT).show();
               })
               .setNeutralButton("永久禁止", (dialog, which) -> {
                   // 添加到黑名单
                   mBlockedPackages.add(packageName);
                   saveSettings();

                   Toast.makeText(mContext, "已永久禁止 " + appName, Toast.LENGTH_SHORT).show();
               })
               .setCancelable(false)
               .show();
    }

    /**
     * 显示被拦截消息
     */
    private void showBlockedMessage(String packageName) {
        String appName = getAppName(packageName);
        Toast.makeText(mContext, appName + " 已被永久禁止访问浏览器", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理意图
     */
    private void handleIntent(Intent intent) {
        try {
            Uri data = intent.getData();
            if (data != null) {
                String url = data.toString();
                // 这里可以调用浏览器的URL处理方法
                Log.d(TAG, "Processing URL: " + url);
                // TODO: 实际处理URL
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle intent", e);
        }
    }

    /**
     * 获取应用名称
     */
    private String getAppName(String packageName) {
        try {
            android.content.pm.PackageManager pm = mContext.getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(info);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get app name for package: " + packageName, e);
            return packageName; // 返回包名作为后备
        }
    }

    /**
     * 检查包是否被拦截
     */
    public boolean isPackageBlocked(String packageName) {
        return mBlockedPackages.contains(packageName);
    }

    /**
     * 检查包是否在白名单中
     */
    public boolean isPackageWhitelisted(String packageName) {
        return mWhitelistPackages.contains(packageName);
    }

    /**
     * 添加包到黑名单
     */
    public void blockPackage(String packageName) {
        mBlockedPackages.add(packageName);
        mWhitelistPackages.remove(packageName);
        saveSettings();
    }

    /**
     * 添加包到白名单
     */
    public void whitelistPackage(String packageName) {
        mWhitelistPackages.add(packageName);
        mBlockedPackages.remove(packageName);
        saveSettings();
    }

    /**
     * 从黑名单移除包
     */
    public void unblockPackage(String packageName) {
        mBlockedPackages.remove(packageName);
        saveSettings();
    }

    /**
     * 从白名单移除包
     */
    public void removeFromWhitelist(String packageName) {
        mWhitelistPackages.remove(packageName);
        saveSettings();
    }

    /**
     * 获取黑名单包列表
     */
    public Set<String> getBlockedPackages() {
        return new HashSet<>(mBlockedPackages);
    }

    /**
     * 获取白名单包列表
     */
    public Set<String> getWhitelistedPackages() {
        return new HashSet<>(mWhitelistPackages);
    }

    /**
     * 设置拦截启用状态
     */
    public void setInterceptEnabled(boolean enabled) {
        mInterceptEnabled = enabled;
        saveSettings();
    }

    /**
     * 获取拦截启用状态
     */
    public boolean isInterceptEnabled() {
        return mInterceptEnabled;
    }

    /**
     * 清除所有设置
     */
    public void clearAllSettings() {
        mBlockedPackages.clear();
        mWhitelistPackages.clear();
        mInterceptEnabled = true;
        saveSettings();
    }
}
