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

        // 立即启动浏览器，不要显示任何界面
        launchBrowser();
        finish(); // 立即关闭这个Activity
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
