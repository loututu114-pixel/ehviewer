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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
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

    // === 新增功能：强制设置默认浏览器 ===

    private static final String PREF_IS_DEFAULT_BROWSER = "is_default_browser";
    private static final String PREF_SHORTCUT_CREATED = "shortcut_created";
    private static final String PREF_FIRST_LAUNCH_SETUP = "first_launch_setup";
    private static final String PREF_SETUP_COMPLETED = "setup_completed";

    /**
     * 检查并强制要求设置为默认浏览器
     */
    public static void checkAndForceDefaultBrowser(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);

        // 检查是否是第一次启动
        boolean isFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH_SETUP, true);
        boolean setupCompleted = prefs.getBoolean(PREF_SETUP_COMPLETED, false);

        if (isFirstLaunch && !setupCompleted) {
            // 显示强制设置默认浏览器的对话框
            showForceDefaultBrowserDialog(context);
        } else if (!isDefaultBrowser(context)) {
            // 不是默认浏览器，显示重新设置提示
            showReSetDefaultBrowserDialog(context);
        }
    }

    /**
     * 显示强制设置默认浏览器的对话框
     */
    private static void showForceDefaultBrowserDialog(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("🚀 成为EhViewer的超级用户")
                    .setMessage("为了获得最佳浏览体验，请将EhViewer设置为您的默认浏览器应用。\n\n" +
                            "这将解锁以下超能力：\n" +
                            "• ⚡ 闪电般的网页加载速度\n" +
                            "• 🛡️ 强大的广告拦截保护\n" +
                            "• 📱 专属桌面快捷方式\n" +
                            "• 🌟 成为您设备上的王者应用\n\n" +
                            "准备好统治您的浏览体验了吗？")
                    .setCancelable(false) // 禁止取消，用户必须做出选择
                    .setPositiveButton("👑 立即称王", (dialog, which) -> {
                        boolean success = trySetAsDefaultBrowser(context);
                        if (!success) {
                            Toast.makeText(context, "请在系统设置中将EhViewer设为默认浏览器",
                                    Toast.LENGTH_LONG).show();
                        }
                        // 记录用户已看过设置提示
                        getPrefs(context).edit().putBoolean(PREF_FIRST_LAUNCH_SETUP, false).apply();
                    })
                    .setNegativeButton("稍后称王", (dialog, which) -> {
                        // 用户选择稍后设置，记录状态但不强制
                        getPrefs(context).edit().putBoolean(PREF_FIRST_LAUNCH_SETUP, false).apply();
                        Toast.makeText(context, "您可以在设置中随时将EhViewer设为默认浏览器",
                                Toast.LENGTH_SHORT).show();
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing force default browser dialog", e);
        }
    }

    /**
     * 显示重新设置为默认浏览器的对话框
     */
    private static void showReSetDefaultBrowserDialog(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("⚠️ 检测到浏览器地位变更")
                    .setMessage("EhViewer不再是您的默认浏览器应用。\n\n" +
                            "为了继续享受王者般的浏览体验，请重新将EhViewer设置为默认浏览器。")
                    .setPositiveButton("👑 夺回王位", (dialog, which) -> {
                        trySetAsDefaultBrowser(context);
                    })
                    .setNegativeButton("稍后", (dialog, which) -> {
                        // 什么都不做
                    })
                    .setNeutralButton("不再提醒", (dialog, which) -> {
                        getPrefs(context).edit().putBoolean(PREF_SETUP_COMPLETED, true).apply();
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing re-set default browser dialog", e);
        }
    }

    /**
     * 当设置为默认浏览器后执行的操作
     */
    public static void onSetAsDefaultBrowser(@NonNull Context context) {
        try {
            SharedPreferences prefs = getPrefs(context);
            prefs.edit()
                    .putBoolean(PREF_IS_DEFAULT_BROWSER, true)
                    .putBoolean(PREF_FIRST_LAUNCH_SETUP, false)
                    .putBoolean(PREF_SETUP_COMPLETED, true)
                    .apply();

            // 自动创建桌面快捷方式
            createEhBrowserShortcut(context);

            // 显示成功提示
            Toast.makeText(context, "🎉 恭喜！EhViewer已成为默认浏览器！\n🏆 您现在是浏览王者！\n📱 桌面快捷方式已创建",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error in onSetAsDefaultBrowser", e);
        }
    }

    /**
     * 创建Eh浏览器桌面快捷方式
     */
    private static void createEhBrowserShortcut(@NonNull Context context) {
        try {
            SharedPreferences prefs = getPrefs(context);
            boolean shortcutCreated = prefs.getBoolean(PREF_SHORTCUT_CREATED, false);

            if (!shortcutCreated) {
                Intent shortcutIntent = new Intent(context, com.hippo.ehviewer.ui.EhBrowserActivity.class);
                shortcutIntent.setAction(Intent.ACTION_MAIN);
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                Intent addShortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
                addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "🚀 Eh浏览器");
                addShortcutIntent.putExtra("duplicate", false);

                // 设置图标
                android.content.res.Resources res = context.getResources();
                Bitmap icon = BitmapFactory.decodeResource(res, android.R.mipmap.sym_def_app_icon);

                // 尝试使用自定义图标
                try {
                    int browserIconRes = res.getIdentifier("ic_browser", "mipmap", context.getPackageName());
                    if (browserIconRes != 0) {
                        icon = BitmapFactory.decodeResource(res, browserIconRes);
                    }
                } catch (Exception e) {
                    // 使用默认图标
                }

                addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
                context.sendBroadcast(addShortcutIntent);

                // 标记快捷方式已创建
                prefs.edit().putBoolean(PREF_SHORTCUT_CREATED, true).apply();

                Log.d(TAG, "EhBrowser desktop shortcut created successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating EhBrowser shortcut", e);
            Toast.makeText(context, "创建快捷方式失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检查应用启动时的浏览器设置状态
     */
    public static void checkAppLaunchBrowserStatus(@NonNull Context context) {
        try {
            SharedPreferences prefs = getPrefs(context);

            // 检查是否需要显示默认浏览器提示
            boolean isDefault = isDefaultBrowser(context);
            boolean wasDefault = prefs.getBoolean(PREF_IS_DEFAULT_BROWSER, false);

            if (isDefault && !wasDefault) {
                // 用户刚刚设置为默认浏览器
                onSetAsDefaultBrowser(context);
            } else if (!isDefault && prefs.getBoolean(PREF_SETUP_COMPLETED, false)) {
                // 不再是默认浏览器，显示重新设置提示
                showReSetDefaultBrowserDialog(context);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking app launch browser status", e);
        }
    }

    /**
     * 获取应用在设备上的"统治力"状态
     */
    public static String getAppDominanceStatus(@NonNull Context context) {
        StringBuilder status = new StringBuilder();

        if (isDefaultBrowser(context)) {
            status.append("👑 默认浏览器 ✅\n");
        } else {
            status.append("⚠️ 非默认浏览器 ❌\n");
        }

        SharedPreferences prefs = getPrefs(context);
        if (prefs.getBoolean(PREF_SHORTCUT_CREATED, false)) {
            status.append("📱 桌面快捷方式 ✅\n");
        } else {
            status.append("📱 无桌面快捷方式 ❌\n");
        }

        if (prefs.getBoolean(PREF_SETUP_COMPLETED, false)) {
            status.append("⚙️ 设置完成 ✅");
        } else {
            status.append("⚙️ 设置未完成 ❌");
        }

        return status.toString();
    }

    /**
     * 强化应用的存在感
     */
    public static void strengthenAppPresence(@NonNull Context context) {
        try {
            if (isDefaultBrowser(context)) {
                // 已经是默认浏览器，确保快捷方式存在
                createEhBrowserShortcut(context);

                // 显示统治力状态
                String status = getAppDominanceStatus(context);
                Log.d(TAG, "App Dominance Status:\n" + status);

            } else {
                // 不是默认浏览器，显示设置提示
                checkAndForceDefaultBrowser(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error strengthening app presence", e);
        }
    }

    /**
     * 获取SharedPreferences
     */
    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences("ehviewer_browser_prefs", Context.MODE_PRIVATE);
    }

    /**
     * 重置所有浏览器设置（用于调试）
     */
    public static void resetBrowserSettings(@NonNull Context context) {
        getPrefs(context).edit().clear().apply();
        Toast.makeText(context, "浏览器设置已重置", Toast.LENGTH_SHORT).show();
    }
}
