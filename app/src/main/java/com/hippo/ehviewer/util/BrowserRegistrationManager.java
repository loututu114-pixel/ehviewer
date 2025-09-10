package com.hippo.ehviewer.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 浏览器注册管理器
 * 确保EhViewer在Android系统中正确注册为浏览器
 */
public class BrowserRegistrationManager {
    private static final String TAG = "BrowserRegistrationManager";

    private final Context context;

    public BrowserRegistrationManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 检查应用是否在浏览器列表中可见
     */
    public boolean isBrowserVisible() {
        try {
            List<ResolveInfo> browsers = getAllBrowsers();
            String packageName = context.getPackageName();

            for (ResolveInfo browser : browsers) {
                if (browser.activityInfo != null &&
                    packageName.equals(browser.activityInfo.packageName)) {
                    Log.d(TAG, "EhViewer found in browser list: " + browser.activityInfo.name);
                    return true;
                }
            }

            Log.w(TAG, "EhViewer not found in browser list");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking browser visibility", e);
            return false;
        }
    }

    /**
     * 获取所有浏览器应用
     */
    @NonNull
    public List<ResolveInfo> getAllBrowsers() {
        List<ResolveInfo> browsers = new ArrayList<>();

        try {
            // 测试HTTP URL
            Intent httpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
            List<ResolveInfo> httpBrowsers = context.getPackageManager().queryIntentActivities(
                httpIntent, PackageManager.MATCH_ALL);
            browsers.addAll(httpBrowsers);

            // 测试HTTPS URL
            Intent httpsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"));
            List<ResolveInfo> httpsBrowsers = context.getPackageManager().queryIntentActivities(
                httpsIntent, PackageManager.MATCH_ALL);

            // 添加未重复的HTTPS浏览器
            for (ResolveInfo httpsBrowser : httpsBrowsers) {
                boolean exists = false;
                for (ResolveInfo browser : browsers) {
                    if (browser.activityInfo != null && httpsBrowser.activityInfo != null &&
                        browser.activityInfo.packageName.equals(httpsBrowser.activityInfo.packageName) &&
                        browser.activityInfo.name.equals(httpsBrowser.activityInfo.name)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    browsers.add(httpsBrowser);
                }
            }

            Log.d(TAG, "Found " + browsers.size() + " browser apps");

        } catch (Exception e) {
            Log.e(TAG, "Error getting browser list", e);
        }

        return browsers;
    }

    /**
     * 强制重新注册浏览器组件
     */
    public boolean forceRegisterBrowser() {
        try {
            PackageManager pm = context.getPackageManager();

            // 启用WebViewActivity的所有组件
            ComponentName webViewComponent = new ComponentName(context, "com.hippo.ehviewer.ui.YCWebViewActivity");

            // 检查组件是否已启用
            int enabledState = pm.getComponentEnabledSetting(webViewComponent);
            if (enabledState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                // 启用组件
                pm.setComponentEnabledSetting(
                    webViewComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                );
                Log.d(TAG, "YCWebViewActivity component enabled");
            }

            // 强制系统重新扫描包信息
            Intent intent = new Intent(Intent.ACTION_PACKAGE_CHANGED);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.sendBroadcast(intent);

            Log.d(TAG, "Browser registration forced");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error forcing browser registration", e);
            return false;
        }
    }

    /**
     * 检查并修复浏览器intent-filter
     */
    public boolean checkAndFixIntentFilters() {
        try {
            // 检查WebViewActivity的intent-filter是否正确配置
            Intent testIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(
                testIntent, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                String activityName = resolveInfo.activityInfo.name;

                Log.d(TAG, "Default browser: " + packageName + "/" + activityName);

                if (context.getPackageName().equals(packageName) &&
                    "com.hippo.ehviewer.ui.YCWebViewActivity".equals(activityName)) {
                    Log.d(TAG, "EhViewer is default browser");
                    return true;
                }
            }

            Log.d(TAG, "EhViewer is not default browser");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking intent filters", e);
            return false;
        }
    }

    /**
     * 获取浏览器注册状态
     */
    @NonNull
    public BrowserRegistrationStatus getRegistrationStatus() {
        BrowserRegistrationStatus status = new BrowserRegistrationStatus();

        try {
            status.isVisible = isBrowserVisible();
            status.isDefault = DefaultBrowserHelper.isDefaultBrowser(context);
            status.browserCount = getAllBrowsers().size();
            status.canRequestRole = DefaultBrowserHelper.canRequestDefaultBrowserRole(context);

            // 检查intent-filter
            status.intentFiltersWorking = checkAndFixIntentFilters();

            // 检查Activity状态
            PackageManager pm = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, "com.hippo.ehviewer.ui.YCWebViewActivity");
            int componentState = pm.getComponentEnabledSetting(componentName);
            status.activityEnabled = (componentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                                    componentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

            Log.d(TAG, "Browser registration status: " + status.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error getting registration status", e);
        }

        return status;
    }

    /**
     * 修复浏览器注册问题
     */
    public boolean fixBrowserRegistration() {
        try {
            boolean success = true;

            // 1. 强制注册浏览器组件
            success &= forceRegisterBrowser();

            // 2. 检查intent-filter
            success &= checkAndFixIntentFilters();

            // 3. 清除包缓存（如果可能）
            clearPackageCache();

            Log.d(TAG, "Browser registration fix " + (success ? "successful" : "failed"));
            return success;

        } catch (Exception e) {
            Log.e(TAG, "Error fixing browser registration", e);
            return false;
        }
    }

    /**
     * 清除包缓存（尝试）
     */
    private void clearPackageCache() {
        try {
            // 发送包更新广播
            Intent intent = new Intent(Intent.ACTION_PACKAGE_REPLACED);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.sendBroadcast(intent);

            // 等待系统处理
            Thread.sleep(1000);

        } catch (Exception e) {
            Log.e(TAG, "Error clearing package cache", e);
        }
    }

    /**
     * 打开应用详情页面（强制用户手动设置）
     */
    public void openAppDetailsForBrowserSetting() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening app details", e);
        }
    }

    /**
     * 创建浏览器测试Intent
     */
    @NonNull
    public Intent createBrowserTestIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
    }

    /**
     * 浏览器注册状态类
     */
    public static class BrowserRegistrationStatus {
        public boolean isVisible = false;        // 是否在浏览器列表中可见
        public boolean isDefault = false;        // 是否为默认浏览器
        public int browserCount = 0;             // 系统中的浏览器数量
        public boolean canRequestRole = false;   // 是否可以请求默认浏览器角色
        public boolean intentFiltersWorking = false; // intent-filter是否正常工作
        public boolean activityEnabled = false;  // Activity是否已启用

        @Override
        public String toString() {
            return "BrowserRegistrationStatus{" +
                    "isVisible=" + isVisible +
                    ", isDefault=" + isDefault +
                    ", browserCount=" + browserCount +
                    ", canRequestRole=" + canRequestRole +
                    ", intentFiltersWorking=" + intentFiltersWorking +
                    ", activityEnabled=" + activityEnabled +
                    '}';
        }

        /**
         * 获取状态描述
         */
        public String getStatusDescription() {
            StringBuilder sb = new StringBuilder();

            sb.append("系统浏览器数量: ").append(browserCount).append("\n");

            if (isVisible) {
                sb.append("✓ EhViewer在浏览器列表中可见\n");
            } else {
                sb.append("✗ EhViewer不在浏览器列表中\n");
            }

            if (isDefault) {
                sb.append("✓ EhViewer是默认浏览器\n");
            } else {
                sb.append("✗ EhViewer不是默认浏览器\n");
            }

            if (activityEnabled) {
                sb.append("✓ WebViewActivity已启用\n");
            } else {
                sb.append("✗ WebViewActivity未启用\n");
            }

            if (intentFiltersWorking) {
                sb.append("✓ Intent-filter正常工作\n");
            } else {
                sb.append("✗ Intent-filter存在问题\n");
            }

            if (canRequestRole) {
                sb.append("✓ 可以请求默认浏览器角色\n");
            } else {
                sb.append("✓ 无法请求默认浏览器角色\n");
            }

            return sb.toString();
        }

        /**
         * 是否所有设置都正常
         */
        public boolean isAllGood() {
            return isVisible && activityEnabled && intentFiltersWorking;
        }
    }
}
