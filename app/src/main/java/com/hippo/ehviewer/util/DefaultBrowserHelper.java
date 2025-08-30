/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import java.util.List;

/**
 * 默认浏览器助手类
 * 提供检查和设置默认浏览器的功能
 */
public class DefaultBrowserHelper {

    private static final String TAG = "DefaultBrowserHelper";

    /**
     * 检查应用是否为默认浏览器
     */
    public static boolean isDefaultBrowser(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String defaultPackage = resolveInfo.activityInfo.packageName;
                String currentPackage = context.getPackageName();

                Log.d(TAG, "Default browser package: " + defaultPackage);
                Log.d(TAG, "Current app package: " + currentPackage);

                return currentPackage.equals(defaultPackage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check default browser", e);
        }

        return false;
    }

    /**
     * 检查是否有默认浏览器设置
     */
    public static boolean hasDefaultBrowser(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return resolveInfo != null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check if default browser exists", e);
        }
        return false;
    }

    /**
     * 获取当前默认浏览器信息
     */
    @Nullable
    public static String getDefaultBrowserInfo(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                PackageManager pm = context.getPackageManager();
                String label = resolveInfo.activityInfo.loadLabel(pm).toString();
                String packageName = resolveInfo.activityInfo.packageName;

                return label + " (" + packageName + ")";
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get default browser info", e);
        }

        return null;
    }

    /**
     * 获取所有可用的浏览器应用
     */
    @NonNull
    public static List<ResolveInfo> getAvailableBrowsers(@NonNull Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
        PackageManager pm = context.getPackageManager();
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
    }

    /**
     * 打开默认浏览器设置页面
     */
    public static boolean openDefaultBrowserSettings(@NonNull Context context) {
        try {
            // 尝试打开默认应用设置页面
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to open default browser settings", e);

            try {
                // 备用方法：打开应用详情页
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
                return true;
            } catch (Exception e2) {
                Log.e(TAG, "Failed to open app details", e2);
                return false;
            }
        }
    }

    /**
     * 尝试设置应用为默认浏览器（仅在API 29+上有效）
     */
    public static boolean trySetAsDefaultBrowser(@NonNull Context context) {
        try {
            // 在Android 10+上，可以通过RoleManager请求默认浏览器角色
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.app.role.RoleManager roleManager = (android.app.role.RoleManager)
                    context.getSystemService(Context.ROLE_SERVICE);

                if (roleManager != null) {
                    if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER)) {
                        if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)) {
                            Intent intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER);
                            context.startActivity(intent);
                            return true;
                        }
                    }
                }
            }

            // 对于低版本Android或RoleManager不可用，回退到手动设置
            return openDefaultBrowserSettings(context);

        } catch (Exception e) {
            Log.e(TAG, "Failed to set as default browser", e);
            return openDefaultBrowserSettings(context);
        }
    }

    /**
     * 获取浏览器设置状态描述
     */
    @NonNull
    public static String getBrowserStatusDescription(@NonNull Context context) {
        if (isDefaultBrowser(context)) {
            return "EhViewer已是默认浏览器";
        } else {
            String defaultBrowser = getDefaultBrowserInfo(context);
            if (defaultBrowser != null) {
                return "当前默认浏览器: " + defaultBrowser;
            } else {
                return "未设置默认浏览器";
            }
        }
    }

    /**
     * 检查是否可以请求默认浏览器角色
     */
    public static boolean canRequestDefaultBrowserRole(@NonNull Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.app.role.RoleManager roleManager = (android.app.role.RoleManager)
                context.getSystemService(Context.ROLE_SERVICE);

            if (roleManager != null) {
                return roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER) &&
                       !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER);
            }
        }
        return false;
    }

    /**
     * 创建浏览器测试Intent
     */
    @NonNull
    public static Intent createBrowserTestIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
    }
}
