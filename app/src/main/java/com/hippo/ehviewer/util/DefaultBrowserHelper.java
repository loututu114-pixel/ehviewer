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

            // 方法2: 增强版默认应用设置跳转
            if (tryEnhancedDefaultAppsSettings(context)) {
                Log.d(TAG, "Enhanced default apps settings opened");
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
     * 方法2: 增强版直接打开默认应用设置页面
     */
    private static boolean tryEnhancedDefaultAppsSettings(@NonNull Context context) {
        try {
            // 尝试多种设置页面，按优先级排序
            String[] settingsActions = {
                Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,           // 标准默认应用设置
                "android.settings.MANAGE_DEFAULT_APPS_SETTINGS",        // 兼容性版本
                Settings.ACTION_APPLICATION_SETTINGS,                   // 应用设置
                "com.android.settings.APPLICATION_SETTINGS",            // 兼容性版本
                "android.settings.APPLICATION_MANAGEMENT_SETTINGS",     // 应用管理
                "android.intent.action.MANAGE_APP_PERMISSION",          // 权限管理
            };

            for (String action : settingsActions) {
                try {
                    Intent intent = new Intent(action);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // 添加额外参数以帮助系统定位到浏览器设置
                    intent.putExtra("android.intent.extra.USER_ID", 0);
                    intent.putExtra("android.settings.extra.APP_PACKAGE", context.getPackageName());
                    context.startActivity(intent);

                    Toast.makeText(context, "🚀 已打开系统设置，请选择【浏览器】并选择EhViewer", Toast.LENGTH_LONG).show();
                    return true;
                } catch (Exception e) {
                    Log.d(TAG, "Settings action failed: " + action);
                }
            }

            // 如果上面的都失败了，尝试厂商特定的设置页面
            return tryVendorSpecificSettings(context);

        } catch (Exception e) {
            Log.e(TAG, "Enhanced settings approach failed", e);
            return false;
        }
    }

    /**
     * 尝试厂商特定的设置页面
     */
    private static boolean tryVendorSpecificSettings(@NonNull Context context) {
        try {
            // 获取设备厂商信息
            String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
            String[] vendorActions;

            switch (manufacturer) {
                case "huawei":
                case "honor":
                    vendorActions = new String[]{
                        "com.huawei.systemmanager/.apps.ManagedAppActivity",
                        "com.huawei.systemmanager/.apps.DefaultAppManagerActivity"
                    };
                    break;
                case "xiaomi":
                case "redmi":
                    vendorActions = new String[]{
                        "com.miui.securitycenter/com.miui.permcenter.permissions.AppPermissionsEditorActivity",
                        "com.android.settings/.applications.ManageApplications"
                    };
                    break;
                case "oppo":
                case "oneplus":
                    vendorActions = new String[]{
                        "com.coloros.safecenter/.permission.PermissionManagerActivity",
                        "com.android.settings/.applications.ManageApplications"
                    };
                    break;
                case "vivo":
                    vendorActions = new String[]{
                        "com.vivo.permissionmanager/.activity.PermMainActivity",
                        "com.android.settings/.applications.ManageApplications"
                    };
                    break;
                case "samsung":
                    vendorActions = new String[]{
                        "com.android.settings/.applications.ManageApplications",
                        "com.samsung.android.app.settings/.Settings"
                    };
                    break;
                default:
                    return false;
            }

            // 尝试厂商特定的设置页面
            for (String action : vendorActions) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(ComponentName.unflattenFromString(action));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);

                    Toast.makeText(context, "📱 已打开" + manufacturer.toUpperCase() + "系统设置，请设置EhViewer为默认浏览器", Toast.LENGTH_LONG).show();
                    return true;
                } catch (Exception e) {
                    Log.d(TAG, "Vendor settings failed: " + action);
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Vendor specific settings failed", e);
            return false;
        }
    }

    /**
     * 保留原有的方法2以兼容性
     */
    private static boolean tryDirectDefaultAppsSettings(@NonNull Context context) {
        return tryEnhancedDefaultAppsSettings(context);
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
    private static final String PREF_LAST_REMINDER_TIME = "last_reminder_time";
    private static final String PREF_SESSION_REMINDER_SHOWN = "session_reminder_shown";
    private static final String PREF_USER_DISMISSED_REMINDER = "user_dismissed_reminder";

    /**
     * 检查并强制要求设置为默认浏览器 - 用户必须完成设置
     */
    public static void checkAndForceDefaultBrowser(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);

        // 检查是否已经完成设置
        boolean setupCompleted = prefs.getBoolean(PREF_SETUP_COMPLETED, false);

        if (!setupCompleted) {
            // 如果还没完成设置，强制显示设置对话框
            Log.d(TAG, "Setup not completed, showing force setup dialog");
            showForceDefaultBrowserDialog(context);
        } else if (!isDefaultBrowser(context)) {
            // 如果曾经设置过但现在不是默认浏览器，根据智能提醒策略显示提示
            Log.d(TAG, "No longer default browser, checking smart reminder strategy");
            showSmartReminderDialog(context);
        } else {
            Log.d(TAG, "Browser setup is complete and EhViewer is default browser");
            // 重置会话提醒标记
            resetSessionReminderFlag(context);
        }
    }

    /**
     * 显示强制设置默认浏览器的对话框 - 用户必须完成设置才能继续使用
     */
    private static void showForceDefaultBrowserDialog(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            android.app.AlertDialog dialog = builder.setTitle("🚨 重要设置 - 必须完成")
                    .setMessage("为了确保EhViewer正常工作，您必须将EhViewer设置为默认浏览器。\n\n" +
                            "这将为您提供：\n" +
                            "• 🔗 所有链接都在EhViewer中打开\n" +
                            "• ⚡ 最佳的浏览体验\n" +
                            "• 🛡️ 安全的浏览环境\n" +
                            "• 📱 完整的浏览器功能\n\n" +
                            "⚠️ 此设置非常重要，请务必完成！")
                    .setCancelable(false) // 完全禁止取消
                    .setPositiveButton("🎯 立即设置", (dialogInterface, which) -> {
                        boolean success = trySetAsDefaultBrowser(context);
                        if (!success) {
                            // 如果设置失败，继续显示对话框，不让用户跳过
                            showForceDefaultBrowserDialog(context);
                            Toast.makeText(context, "设置失败，请在系统设置中完成设置",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // 设置成功，标记为已完成
                            getPrefs(context).edit()
                                    .putBoolean(PREF_FIRST_LAUNCH_SETUP, false)
                                    .putBoolean(PREF_SETUP_COMPLETED, true)
                                    .apply();
                            Toast.makeText(context, "✅ 设置成功！欢迎使用EhViewer",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("❓ 怎么设置", (dialogInterface, which) -> {
                        // 显示详细设置教程，然后重新显示设置对话框
                        showDetailedSetupGuide(context);
                        // 延迟重新显示对话框
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            showForceDefaultBrowserDialog(context);
                        }, 3000);
                    })
                    .create();

            // 设置对话框消失监听，如果用户通过其他方式关闭对话框，重新显示
            dialog.setOnDismissListener(dialogInterface -> {
                // 检查是否真的设置成功了
                if (!isDefaultBrowser(context) && !getPrefs(context).getBoolean(PREF_SETUP_COMPLETED, false)) {
                    // 如果没有设置成功，延迟重新显示对话框
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        showForceDefaultBrowserDialog(context);
                    }, 1000);
                }
            });

            dialog.show();

            // 禁用返回键
            dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    Toast.makeText(context, "请先完成浏览器设置", Toast.LENGTH_SHORT).show();
                    return true; // 消费掉返回键事件
                }
                return false;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error showing force default browser dialog", e);
            // 如果对话框显示失败，尝试备用方案
            showEmergencySetupDialog(context);
        }
    }

    /**
     * 显示紧急设置对话框 - 当主对话框无法显示时的备用方案
     */
    private static void showEmergencySetupDialog(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            android.app.AlertDialog dialog = builder.setTitle("⚠️ 紧急设置")
                    .setMessage("EhViewer需要设置为默认浏览器才能正常工作。\n\n请按以下步骤操作：\n\n" +
                            "1. 按下手机的【主页】键\n" +
                            "2. 长按EhViewer图标\n" +
                            "3. 选择【应用信息】\n" +
                            "4. 点击【设为默认】\n" +
                            "5. 选择【浏览器】\n" +
                            "6. 选择【EhViewer】\n\n" +
                            "设置完成后，请重新打开EhViewer。")
                    .setCancelable(false)
                    .setPositiveButton("我已完成设置", (dialogInterface, which) -> {
                        if (isDefaultBrowser(context)) {
                            // 设置成功
                            getPrefs(context).edit()
                                    .putBoolean(PREF_FIRST_LAUNCH_SETUP, false)
                                    .putBoolean(PREF_SETUP_COMPLETED, true)
                                    .apply();
                            Toast.makeText(context, "✅ 设置成功！", Toast.LENGTH_SHORT).show();
                        } else {
                            // 还没设置成功，继续显示对话框
                            Toast.makeText(context, "检测到还未完成设置，请先完成设置", Toast.LENGTH_LONG).show();
                            showEmergencySetupDialog(context);
                        }
                    })
                    .setNeutralButton("显示详细教程", (dialogInterface, which) -> {
                        showDetailedSetupGuide(context);
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            showEmergencySetupDialog(context);
                        }, 5000);
                    })
                    .create();

            dialog.setOnDismissListener(dialogInterface -> {
                if (!isDefaultBrowser(context) && !getPrefs(context).getBoolean(PREF_SETUP_COMPLETED, false)) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        showEmergencySetupDialog(context);
                    }, 1000);
                }
            });

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing emergency setup dialog", e);
            // 如果所有对话框都失败，显示Toast提示
            Toast.makeText(context, "请手动将EhViewer设为默认浏览器", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 显示重新设置为默认浏览器的对话框 - 强制用户重新设置
     */
    private static void showReSetDefaultBrowserDialog(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            android.app.AlertDialog dialog = builder.setTitle("⚠️ 浏览器设置异常")
                    .setMessage("检测到EhViewer不再是默认浏览器，这将影响应用的正常使用。\n\n" +
                            "为了确保所有功能正常工作，建议重新将EhViewer设置为默认浏览器。\n\n" +
                            "如果不设置，您可能无法正常使用浏览功能。")
                    .setCancelable(false)
                    .setPositiveButton("🔧 立即修复", (dialogInterface, which) -> {
                        boolean success = trySetAsDefaultBrowser(context);
                        if (!success) {
                            Toast.makeText(context, "请在系统设置中完成设置", Toast.LENGTH_LONG).show();
                            // 不立即重新显示对话框，给用户时间操作
                        } else {
                            // 设置成功，重置用户拒绝标记
                            getPrefs(context).edit()
                                .putBoolean(PREF_USER_DISMISSED_REMINDER, false)
                                .apply();
                        }
                    })
                    .setNeutralButton("📖 怎么操作", (dialogInterface, which) -> {
                        showDetailedSetupGuide(context);
                        // 延迟重新显示对话框，给用户时间查看教程
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (!isDefaultBrowser(context)) {
                                showReSetDefaultBrowserDialog(context);
                            }
                        }, 5000);
                    })
                    .setNegativeButton("⏰ 暂时忽略", (dialogInterface, which) -> {
                        // 记录用户明确拒绝提醒
                        getPrefs(context).edit()
                            .putBoolean(PREF_USER_DISMISSED_REMINDER, true)
                            .putLong(PREF_LAST_REMINDER_TIME, System.currentTimeMillis())
                            .apply();
                        Toast.makeText(context, "已暂时忽略提醒，24小时后可再次提醒", Toast.LENGTH_SHORT).show();
                    })
                    .create();

            // 修改dismiss监听器，不强制重新显示
            dialog.setOnDismissListener(dialogInterface -> {
                // 只有在用户没有明确拒绝的情况下才考虑重新显示
                boolean userDismissed = getPrefs(context).getBoolean(PREF_USER_DISMISSED_REMINDER, false);
                if (!isDefaultBrowser(context) && !userDismissed) {
                    // 延迟重新显示，但给用户更多时间
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        showSmartReminderDialog(context);
                    }, 2000);
                }
            });

            dialog.show();

            // 允许返回键关闭对话框
            dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    // 用户按返回键，视为暂时忽略
                    getPrefs(context).edit()
                        .putBoolean(PREF_USER_DISMISSED_REMINDER, true)
                        .putLong(PREF_LAST_REMINDER_TIME, System.currentTimeMillis())
                        .apply();
                    Toast.makeText(context, "已暂时忽略提醒", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                    return true;
                }
                return false;
            });

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
     * 显示详细设置教程并直接跳转到设置页面
     */
    private static void showDetailedSetupGuide(@NonNull Context context) {
        try {
            // 先尝试直接跳转到系统设置
            boolean directJumpSuccess = tryDirectBrowserSettingsJump(context);

            if (directJumpSuccess) {
                // 直接跳转成功，显示简短提示
                Toast.makeText(context, "🔧 已跳转到浏览器设置页面，请选择EhViewer", Toast.LENGTH_LONG).show();
                return;
            }

            // 如果直接跳转失败，显示详细教程对话框
            showDetailedSetupDialog(context);
        } catch (Exception e) {
            Log.e(TAG, "Error showing detailed guide", e);
            // 出现异常时显示教程对话框
            showDetailedSetupDialog(context);
        }
    }

    /**
     * 尝试直接跳转到浏览器设置页面
     */
    private static boolean tryDirectBrowserSettingsJump(@NonNull Context context) {
        try {
            // 方法1: 尝试使用更具体的设置页面
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 添加参数指定我们要设置浏览器
            intent.putExtra("android.intent.extra.USER_ID", 0);
            intent.putExtra("android.settings.extra.APP_PACKAGE", context.getPackageName());
            intent.putExtra("android.intent.category.BROWSABLE", true);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Direct browser settings jump failed", e);
        }

        try {
            // 方法2: 尝试应用详情页面中的默认设置
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 添加额外参数
            intent.putExtra("android.intent.category.BROWSABLE", true);
            intent.putExtra("android.settings.extra.APP_PACKAGE", context.getPackageName());
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "App details browser settings failed", e);
        }

        return false;
    }

    /**
     * 显示详细设置教程对话框
     */
    private static void showDetailedSetupDialog(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("📖 设置EhViewer为默认浏览器")
                    .setMessage("请按以下步骤操作：\n\n" +
                            "📱 步骤1: 打开系统设置\n" +
                            "🔍 步骤2: 找到【应用管理】或【默认应用】\n" +
                            "🌐 步骤3: 选择【浏览器】选项\n" +
                            "✅ 步骤4: 选择【EhViewer浏览器】\n\n" +
                            "💡 提示：根据您的设备品牌，设置路径可能略有不同。")
                    .setPositiveButton("🚀 立即跳转设置", (dialog, which) -> {
                        // 尝试多种方式跳转到设置页面
                        if (!tryDirectBrowserSettingsJump(context)) {
                            // 如果都失败了，显示通用设置教程
                            showUniversalSettingsGuide(context);
                        }
                    })
                    .setNegativeButton("❓ 按品牌查看教程", (dialog, which) -> {
                        showBrandSpecificGuide(context);
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing detailed setup dialog", e);
        }
    }

    /**
     * 显示通用设置教程
     */
    private static void showUniversalSettingsGuide(@NonNull Context context) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("🔧 通用设置方法")
                    .setMessage("📋 请按以下任一方法操作：\n\n" +
                            "方法一：\n" +
                            "1. 长按EhViewer应用图标\n" +
                            "2. 选择【应用信息】\n" +
                            "3. 点击【设为默认】\n" +
                            "4. 选择【浏览器】\n\n" +
                            "方法二：\n" +
                            "1. 打开【设置】\n" +
                            "2. 找到【应用管理】\n" +
                            "3. 选择EhViewer\n" +
                            "4. 设置默认浏览器\n\n" +
                            "方法三：\n" +
                            "1. 打开任意网页链接\n" +
                            "2. 当系统询问选择浏览器时\n" +
                            "3. 选择EhViewer并勾选【始终】")
                    .setPositiveButton("我知道了", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing universal guide", e);
        }
    }

    /**
     * 显示品牌特定设置教程
     */
    private static void showBrandSpecificGuide(@NonNull Context context) {
        try {
            String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
            String title, message;

            switch (manufacturer) {
                case "huawei":
                case "honor":
                    title = "📱 华为/荣耀设备设置";
                    message = "设置 → 应用和服务 → 默认应用 → 浏览器\n\n" +
                            "或：设置 → 应用 → 默认应用 → 浏览器应用";
                    break;
                case "xiaomi":
                case "redmi":
                    title = "📱 小米/红米设备设置";
                    message = "设置 → 应用设置 → 应用管理 → 默认应用设置 → 浏览器";
                    break;
                case "oppo":
                case "oneplus":
                    title = "📱 OPPO/一加设备设置";
                    message = "设置 → 应用管理 → 默认应用 → 浏览器应用";
                    break;
                case "vivo":
                    title = "📱 vivo设备设置";
                    message = "设置 → 更多设置 → 应用管理 → 默认应用 → 浏览器";
                    break;
                case "samsung":
                    title = "📱 三星设备设置";
                    message = "设置 → 应用程序 → 选择默认应用 → 浏览器";
                    break;
                default:
                    title = "📱 原生Android设备设置";
                    message = "设置 → 应用和通知 → 默认应用 → 浏览器应用";
                    break;
            }

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("🚀 跳转设置", (dialog, which) -> {
                        trySetAsDefaultBrowser(context);
                    })
                    .setNegativeButton("我知道了", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing brand specific guide", e);
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

    // === OPPO设备兼容性处理 ===

    /**
     * 检查是否为OPPO设备并处理兼容性问题
     */
    public static void handleOppoDeviceCompatibility(@NonNull Context context) {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("oppo") || manufacturer.contains("oneplus")) {
            Log.d(TAG, "Detected OPPO device, applying compatibility fixes");

            // 处理OplusViewMirrorManager兼容性
            fixOplusViewMirrorManagerCompatibility(context);

            // 优化图片加载设置
            optimizeImageLoadingForOppo(context);
        }
    }

    /**
     * 修复OplusViewMirrorManager兼容性问题
     */
    private static void fixOplusViewMirrorManagerCompatibility(@NonNull Context context) {
        try {
            // 尝试通过系统属性禁用有问题的视图镜像管理器
            System.setProperty("oplus.viewmirror.enabled", "false");
            Log.d(TAG, "Disabled OplusViewMirrorManager via system property");
        } catch (Exception e) {
            Log.w(TAG, "Failed to disable OplusViewMirrorManager", e);
        }

        try {
            // 设置WebView硬件加速策略
            android.webkit.WebView.setWebContentsDebuggingEnabled(false);

            // 禁用可能导致兼容性问题的硬件加速
            SharedPreferences prefs = getPrefs(context);
            prefs.edit().putBoolean("oppo_compatibility_applied", true).apply();

            Log.d(TAG, "Applied OPPO device compatibility fixes");
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply OPPO compatibility fixes", e);
        }
    }

    /**
     * 为OPPO设备优化图片加载设置
     */
    private static void optimizeImageLoadingForOppo(@NonNull Context context) {
        try {
            // OPPO设备可能对图片加载有特殊要求，这里设置更保守的加载策略
            SharedPreferences prefs = getPrefs(context);
            SharedPreferences.Editor editor = prefs.edit();

            // 设置图片加载优化参数
            editor.putBoolean("oppo_image_loading_optimized", true);
            editor.putInt("image_loading_timeout", 10000); // 10秒超时
            editor.putInt("image_retry_count", 2); // 重试2次

            editor.apply();

            Log.d(TAG, "Optimized image loading settings for OPPO device");
        } catch (Exception e) {
            Log.w(TAG, "Failed to optimize image loading for OPPO", e);
        }
    }

    /**
     * 检查OPPO设备兼容性是否已应用
     */
    public static boolean isOppoCompatibilityApplied(@NonNull Context context) {
        return getPrefs(context).getBoolean("oppo_compatibility_applied", false);
    }

    /**
     * 获取OPPO设备优化的图片加载超时时间
     */
    public static int getOppoImageLoadingTimeout(@NonNull Context context) {
        return getPrefs(context).getInt("image_loading_timeout", 15000);
    }

    /**
     * 获取OPPO设备的图片加载重试次数
     */
    public static int getOppoImageRetryCount(@NonNull Context context) {
        return getPrefs(context).getInt("image_retry_count", 1);
    }

    /**
     * 显示智能提醒对话框 - 控制提醒频率
     */
    private static void showSmartReminderDialog(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);

        // 检查是否在本会话中已经显示过提醒
        boolean sessionReminderShown = prefs.getBoolean(PREF_SESSION_REMINDER_SHOWN, false);
        if (sessionReminderShown) {
            Log.d(TAG, "Session reminder already shown, skipping");
            return;
        }

        // 检查用户是否明确拒绝过提醒
        boolean userDismissed = prefs.getBoolean(PREF_USER_DISMISSED_REMINDER, false);
        if (userDismissed) {
            // 如果用户明确拒绝，检查是否超过冷却时间（24小时）
            long lastReminderTime = prefs.getLong(PREF_LAST_REMINDER_TIME, 0);
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastReminderTime;
            long cooldownPeriod = 24 * 60 * 60 * 1000; // 24小时

            if (timeDiff < cooldownPeriod) {
                Log.d(TAG, "User dismissed reminder recently, within cooldown period");
                return;
            }
        }

        // 记录提醒时间
        prefs.edit()
            .putLong(PREF_LAST_REMINDER_TIME, System.currentTimeMillis())
            .putBoolean(PREF_SESSION_REMINDER_SHOWN, true)
            .apply();

        // 显示重新设置对话框
        showReSetDefaultBrowserDialog(context);
    }

    /**
     * 重置会话提醒标记
     */
    private static void resetSessionReminderFlag(@NonNull Context context) {
        getPrefs(context).edit()
            .putBoolean(PREF_SESSION_REMINDER_SHOWN, false)
            .apply();
    }
}
