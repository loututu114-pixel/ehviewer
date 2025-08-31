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
     * 增强版设置默认浏览器 - 多种方式尝试
     */
    public static boolean trySetAsDefaultBrowser(@NonNull Context context) {
        Log.d(TAG, "Starting enhanced default browser setup...");
        
        try {
            // 方法1: Android 10+ RoleManager (最直接)
            if (tryRoleManagerApproach(context)) {
                Log.d(TAG, "RoleManager approach succeeded");
                return true;
            }

            // 方法2: 直接打开默认应用设置
            if (tryDirectDefaultAppsSettings(context)) {
                Log.d(TAG, "Direct default apps settings opened");
                return true;
            }

            // 方法3: 打开应用详情页
            if (tryAppDetailsSettings(context)) {
                Log.d(TAG, "App details settings opened");
                return true;
            }

            // 方法4: 强制引导用户设置
            if (tryForceUserGuidance(context)) {
                Log.d(TAG, "Force user guidance initiated");
                return true;
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "All default browser setup methods failed", e);
            return tryEmergencyGuidance(context);
        }
    }

    /**
     * 方法1: RoleManager方式 (Android 10+)
     */
    private static boolean tryRoleManagerApproach(@NonNull Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.app.role.RoleManager roleManager = (android.app.role.RoleManager)
                    context.getSystemService(Context.ROLE_SERVICE);

                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER)) {
                    if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)) {
                        Intent intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        return true;
                    } else {
                        Toast.makeText(context, "EhViewer已是默认浏览器", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "RoleManager approach failed", e);
            return false;
        }
    }

    /**
     * 方法2: 直接打开默认应用设置页面
     */
    private static boolean tryDirectDefaultAppsSettings(@NonNull Context context) {
        try {
            // 尝试多种设置页面
            String[] settingsActions = {
                Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
                "android.settings.MANAGE_DEFAULT_APPS_SETTINGS",
                Settings.ACTION_APPLICATION_SETTINGS
            };

            for (String action : settingsActions) {
                try {
                    Intent intent = new Intent(action);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    
                    Toast.makeText(context, "🚀 请在【浏览器】选项中选择EhViewer", Toast.LENGTH_LONG).show();
                    return true;
                } catch (Exception e) {
                    Log.d(TAG, "Settings action failed: " + action);
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Direct settings approach failed", e);
            return false;
        }
    }

    /**
     * 方法3: 打开应用详情设置页面
     */
    private static boolean tryAppDetailsSettings(@NonNull Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            Toast.makeText(context, "🔧 请在【打开链接】中设置为默认浏览器", Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "App details settings failed", e);
            return false;
        }
    }

    /**
     * 方法4: 强制用户引导
     */
    private static boolean tryForceUserGuidance(@NonNull Context context) {
        try {
            showEnhancedBrowserSetupDialog(context);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Force user guidance failed", e);
            return false;
        }
    }

    /**
     * 紧急引导方案
     */
    private static boolean tryEmergencyGuidance(@NonNull Context context) {
        try {
            Toast.makeText(context, 
                "🆘 手动设置方法:\n" +
                "1. 打开系统设置\n" +
                "2. 找到【应用管理】或【默认应用】\n" +
                "3. 选择【浏览器】\n" +
                "4. 选择EhViewer", 
                Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Emergency guidance failed", e);
            return false;
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
     * 显示增强版浏览器设置对话框
     */
    private static void showEnhancedBrowserSetupDialog(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("🚀 成为设备浏览器之王")
                    .setMessage("让EhViewer统治您的浏览体验！\n\n" +
                            "🎯 一键设置指南:\n" +
                            "1. 点击【立即设置】\n" +
                            "2. 在设置页面找到【浏览器】\n" +
                            "3. 选择【EhViewer浏览器】\n" +
                            "4. 返回享受王者体验\n\n" +
                            "✨ 王者特权:\n" +
                            "• ⚡ 超快加载速度\n" +
                            "• 🛡️ 强力广告拦截\n" +
                            "• 🔐 密码管理器\n" +
                            "• 🎨 完美界面体验")
                    .setCancelable(false)
                    .setPositiveButton("🎯 立即设置", (dialog, which) -> {
                        tryDirectDefaultAppsSettings(context);
                    })
                    .setNegativeButton("🔧 手动设置", (dialog, which) -> {
                        tryAppDetailsSettings(context);
                    })
                    .setNeutralButton("📖 详细教程", (dialog, which) -> {
                        showDetailedSetupGuide(context);
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing enhanced setup dialog", e);
        }
    }

    /**
     * 显示详细设置教程
     */
    private static void showDetailedSetupGuide(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("📖 详细设置教程")
                    .setMessage("根据您的设备品牌选择对应方法:\n\n" +
                            "🔸 华为/荣耀:\n" +
                            "设置 → 应用和服务 → 默认应用 → 浏览器\n\n" +
                            "🔸 小米/红米:\n" +
                            "设置 → 应用设置 → 应用管理 → 默认应用设置\n\n" +
                            "🔸 OPPO/一加:\n" +
                            "设置 → 应用管理 → 默认应用 → 浏览器应用\n\n" +
                            "🔸 vivo:\n" +
                            "设置 → 更多设置 → 应用管理 → 默认应用\n\n" +
                            "🔸 三星:\n" +
                            "设置 → 应用程序 → 选择默认应用 → 浏览器\n\n" +
                            "🔸 原生Android:\n" +
                            "设置 → 应用和通知 → 默认应用 → 浏览器应用")
                    .setPositiveButton("💪 我来试试", (dialog, which) -> {
                        trySetAsDefaultBrowser(context);
                    })
                    .setNegativeButton("👍 明白了", (dialog, which) -> {
                        // 什么都不做
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing detailed guide", e);
        }
    }

    // === 私密模式功能 ===
    
    private static final String PREF_PRIVATE_MODE_ENABLED = "private_mode_enabled";
    private static final String PREF_PRIVATE_MODE_PASSWORD = "private_mode_password";
    private static final String PREF_PRIVATE_MODE_USE_BIOMETRIC = "private_mode_use_biometric";

    /**
     * 检查是否启用了私密模式
     */
    public static boolean isPrivateModeEnabled(@NonNull Context context) {
        return getPrefs(context).getBoolean(PREF_PRIVATE_MODE_ENABLED, false);
    }

    /**
     * 设置私密模式状态
     */
    public static void setPrivateModeEnabled(@NonNull Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(PREF_PRIVATE_MODE_ENABLED, enabled).apply();
        Log.d(TAG, "Private mode " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * 设置私密模式密码
     */
    public static void setPrivateModePassword(@NonNull Context context, String password) {
        // 简单加密存储
        String encrypted = android.util.Base64.encodeToString(password.getBytes(), android.util.Base64.DEFAULT);
        getPrefs(context).edit().putString(PREF_PRIVATE_MODE_PASSWORD, encrypted).apply();
    }

    /**
     * 验证私密模式密码
     */
    public static boolean verifyPrivateModePassword(@NonNull Context context, String password) {
        try {
            String stored = getPrefs(context).getString(PREF_PRIVATE_MODE_PASSWORD, "");
            if (stored.isEmpty()) return false;
            
            String decrypted = new String(android.util.Base64.decode(stored, android.util.Base64.DEFAULT));
            return password.equals(decrypted);
        } catch (Exception e) {
            Log.e(TAG, "Error verifying private mode password", e);
            return false;
        }
    }

    /**
     * 是否使用生物识别验证
     */
    public static boolean isPrivateModeBiometricEnabled(@NonNull Context context) {
        return getPrefs(context).getBoolean(PREF_PRIVATE_MODE_USE_BIOMETRIC, false);
    }

    /**
     * 设置生物识别验证
     */
    public static void setPrivateModeBiometricEnabled(@NonNull Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(PREF_PRIVATE_MODE_USE_BIOMETRIC, enabled).apply();
    }

    /**
     * 判断是否应该进入私密模式
     * 返回0: 普通浏览器模式
     * 返回1: 需要验证进入私密模式  
     * 返回2: 直接进入私密模式
     */
    public static int shouldEnterPrivateMode(@NonNull Context context, Intent intent) {
        // 如果没有启用私密模式，直接返回普通模式
        if (!isPrivateModeEnabled(context)) {
            return 0; // 普通浏览器模式
        }

        // 检查是否来自特殊入口
        if (intent != null) {
            // 来自私密模式专用入口
            if (intent.getBooleanExtra("enter_private_mode", false)) {
                return 1; // 需要验证
            }
            
            // 来自桌面图标或浏览器选择，正常浏览器模式
            if (intent.getBooleanExtra("from_desktop_browser", false) ||
                intent.getBooleanExtra("from_browser_selection", false)) {
                return 0; // 普通浏览器模式
            }
        }

        // 默认普通浏览器模式
        return 0;
    }

    /**
     * 获取私密模式状态描述
     */
    @NonNull
    public static String getPrivateModeStatusDescription(@NonNull Context context) {
        if (isPrivateModeEnabled(context)) {
            String biometric = isPrivateModeBiometricEnabled(context) ? "生物识别" : "密码";
            return "私密模式已启用 (" + biometric + "验证)";
        } else {
            return "私密模式未启用";
        }
    }

    /**
     * 创建私密模式快捷方式
     */
    public static void createPrivateModeShortcut(@NonNull Context context) {
        try {
            Intent shortcutIntent = new Intent(context, com.hippo.ehviewer.ui.EhBrowserActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.putExtra("enter_private_mode", true);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Intent addShortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "🔐 私密浏览");
            addShortcutIntent.putExtra("duplicate", false);

            // 设置图标 (可以用不同的图标表示私密模式)
            android.content.res.Resources res = context.getResources();
            android.graphics.Bitmap icon = android.graphics.BitmapFactory.decodeResource(res, android.R.mipmap.sym_def_app_icon);
            addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);

            context.sendBroadcast(addShortcutIntent);
            Toast.makeText(context, "🔐 私密模式快捷方式已添加到桌面", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error creating private mode shortcut", e);
            Toast.makeText(context, "创建私密模式快捷方式失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 重置所有浏览器设置（用于调试）
     */
    public static void resetBrowserSettings(@NonNull Context context) {
        getPrefs(context).edit().clear().apply();
        Toast.makeText(context, "浏览器设置已重置", Toast.LENGTH_SHORT).show();
    }
}
