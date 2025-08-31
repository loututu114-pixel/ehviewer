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

package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.DefaultBrowserHelper;

/**
 * Eh浏览器桌面图标Activity
 * 这个Activity专门用于桌面上的"Eh浏览器"图标
 * 点击后直接进入浏览器界面，不会显示主应用界面
 */
public class EhBrowserActivity extends AppCompatActivity {

    private static final String TAG = "EhBrowserActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 处理浏览器意图和私密模式
        Intent intent = getIntent();
        handleBrowserModeAndPrivacy(intent);
        
        finish(); // 立即关闭这个Activity
    }

    /**
     * 处理浏览器模式和私密模式逻辑
     */
    private void handleBrowserModeAndPrivacy(Intent intent) {
        // 检查私密模式状态
        int privateModeStatus = DefaultBrowserHelper.shouldEnterPrivateMode(this, intent);
        
        if (privateModeStatus == 1) {
            // 需要验证进入私密模式
            showPrivateModeVerification(intent);
            return;
        }
        
        // 处理普通浏览器意图或私密模式（已验证）
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            // 这是一个浏览器意图，转发给对应Activity
            if (privateModeStatus == 2) {
                handlePrivateBrowserIntent(intent);
            } else {
                handleBrowserIntent(intent);
            }
        } else {
            // 这是从桌面图标启动
            if (privateModeStatus == 2) {
                launchPrivateBrowser();
            } else {
                launchBrowser();
            }
        }
    }

    /**
     * 显示私密模式验证
     */
    private void showPrivateModeVerification(Intent originalIntent) {
        try {
            if (DefaultBrowserHelper.isPrivateModeBiometricEnabled(this)) {
                // 使用生物识别验证
                showBiometricAuthentication(originalIntent);
            } else {
                // 使用密码验证
                showPasswordAuthentication(originalIntent);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error showing private mode verification", e);
            Toast.makeText(this, "私密模式验证失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示生物识别验证
     */
    private void showBiometricAuthentication(Intent originalIntent) {
        // TODO: 实现生物识别验证
        // 这里可以使用 BiometricPrompt 实现指纹/面部识别
        android.util.Log.d(TAG, "Biometric authentication requested");
        
        // 暂时直接进入私密模式（实际项目中应该实现真正的生物识别）
        enterPrivateMode(originalIntent);
    }

    /**
     * 显示密码验证对话框
     */
    private void showPasswordAuthentication(Intent originalIntent) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            android.widget.EditText passwordInput = new android.widget.EditText(this);
            passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
                                     android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordInput.setHint("请输入私密模式密码");

            builder.setTitle("🔐 私密模式验证")
                    .setMessage("请输入密码以进入私密模式")
                    .setView(passwordInput)
                    .setPositiveButton("确认", (dialog, which) -> {
                        String password = passwordInput.getText().toString();
                        if (DefaultBrowserHelper.verifyPrivateModePassword(this, password)) {
                            enterPrivateMode(originalIntent);
                        } else {
                            Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error showing password dialog", e);
            finish();
        }
    }

    /**
     * 进入私密模式
     */
    private void enterPrivateMode(Intent originalIntent) {
        if (originalIntent != null && Intent.ACTION_VIEW.equals(originalIntent.getAction())) {
            handlePrivateBrowserIntent(originalIntent);
        } else {
            launchPrivateBrowser();
        }
    }

    /**
     * 处理浏览器意图 - 转发给WebViewActivity
     */
    private void handleBrowserIntent(Intent originalIntent) {
        try {
            Intent browserIntent = new Intent(this, WebViewActivity.class);
            browserIntent.setAction(originalIntent.getAction());
            browserIntent.setData(originalIntent.getData());
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // 传递所有额外数据
            if (originalIntent.getExtras() != null) {
                browserIntent.putExtras(originalIntent.getExtras());
            }
            
            // 标记为浏览器模式
            browserIntent.putExtra("browser_mode", true);
            browserIntent.putExtra("from_browser_selection", true);

            android.util.Log.d(TAG, "Handling browser intent: " + originalIntent.getDataString());
            startActivity(browserIntent);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to handle browser intent", e);
            Toast.makeText(this, "启动浏览器失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理私密浏览器意图 - 转发给MainActivity（完整应用）
     */
    private void handlePrivateBrowserIntent(Intent originalIntent) {
        try {
            Intent appIntent = new Intent(this, MainActivity.class);
            appIntent.setAction(originalIntent.getAction());
            appIntent.setData(originalIntent.getData());
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // 传递所有额外数据
            if (originalIntent.getExtras() != null) {
                appIntent.putExtras(originalIntent.getExtras());
            }
            
            // 标记为私密模式
            appIntent.putExtra("private_mode", true);
            appIntent.putExtra("from_private_browser", true);

            android.util.Log.d(TAG, "Handling private browser intent: " + originalIntent.getDataString());
            startActivity(appIntent);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to handle private browser intent", e);
            Toast.makeText(this, "启动私密浏览器失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 启动私密浏览器 - 完整应用
     */
    private void launchPrivateBrowser() {
        try {
            Intent appIntent = new Intent(this, MainActivity.class);
            appIntent.setAction(Intent.ACTION_MAIN);
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // 标记为私密模式
            appIntent.putExtra("private_mode", true);
            appIntent.putExtra("from_private_desktop", true);

            android.util.Log.d(TAG, "Launching private browser from desktop icon");
            startActivity(appIntent);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to launch private browser", e);
            Toast.makeText(this, "启动私密浏览器失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 启动浏览器界面
     */
    private void launchBrowser() {
        try {
            Intent browserIntent = new Intent(this, WebViewActivity.class);
            browserIntent.setAction(Intent.ACTION_VIEW);
            browserIntent.setData(android.net.Uri.parse("https://www.google.com"));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // 标记为从桌面浏览器图标启动
            browserIntent.putExtra("from_desktop_browser", true);
            browserIntent.putExtra("browser_mode", true);

            android.util.Log.d(TAG, "Launching browser from desktop icon");
            startActivity(browserIntent);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to launch browser from desktop", e);
            Toast.makeText(this, "启动浏览器失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 创建桌面快捷方式
     */
    public static void createDesktopShortcut(android.content.Context context) {
        try {
            // 检查快捷方式是否已存在
            if (shortcutExists(context)) {
                Toast.makeText(context, "浏览器快捷方式已存在", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent shortcutIntent = new Intent(context, EhBrowserActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent addShortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Eh浏览器");
            addShortcutIntent.putExtra("duplicate", false);

            // 设置图标
            android.content.res.Resources res = context.getResources();
            android.graphics.Bitmap icon = android.graphics.BitmapFactory.decodeResource(res, R.mipmap.ic_launcher);

            // 如果有专门的浏览器图标，使用它
            try {
                int browserIconRes = res.getIdentifier("ic_browser", "mipmap", context.getPackageName());
                if (browserIconRes != 0) {
                    icon = android.graphics.BitmapFactory.decodeResource(res, browserIconRes);
                }
            } catch (Exception e) {
                // 使用默认图标
            }

            addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);

            context.sendBroadcast(addShortcutIntent);
            Toast.makeText(context, "Eh浏览器快捷方式已添加到桌面", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to create desktop shortcut", e);
            Toast.makeText(context, "创建快捷方式失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检查快捷方式是否已存在
     */
    private static boolean shortcutExists(android.content.Context context) {
        try {
            // 这里可以实现更复杂的检查逻辑
            // 简单起见，返回false让用户总是可以创建
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
