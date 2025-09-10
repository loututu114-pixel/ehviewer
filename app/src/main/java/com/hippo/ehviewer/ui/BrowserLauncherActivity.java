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
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

/**
 * EhViewer浏览器启动器
 * 处理来自系统的外部链接跳转请求，并支持桌面浏览器图标启动
 * - 从外部链接启动：直接进入浏览器界面
 * - 从桌面浏览器图标启动：显示浏览器主页
 * - 正常应用启动：进入主界面
 */
public class BrowserLauncherActivity extends AppCompatActivity {

    private static final String TAG = "BrowserLauncherActivity";

    // 浏览器启动模式
    public static final String EXTRA_BROWSER_MODE = "browser_mode";
    public static final String BROWSER_MODE_HOME = "home";         // 浏览器主页
    public static final String BROWSER_MODE_URL = "url";           // 指定URL
    public static final String BROWSER_MODE_DEFAULT = "default";   // 默认浏览器行为

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 立即处理intent，不要显示界面
        handleBrowserIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleBrowserIntent(intent);
    }

    /**
     * 处理浏览器intent - 简化版本
     */
    private void handleBrowserIntent(Intent intent) {
        try {
            if (intent == null) {
                finish();
                return;
            }

            // 统一转发给WebViewActivity，避免重复逻辑
            Intent webViewIntent = new Intent(this, YCWebViewActivity.class);
            webViewIntent.setAction(intent.getAction());
            webViewIntent.setData(intent.getData());

            // 传递所有额外参数
            if (intent.getExtras() != null) {
                webViewIntent.putExtras(intent.getExtras());
            }

            Log.d(TAG, "Forwarding intent to YCWebViewActivity: " + intent.getData());
            startActivity(webViewIntent);

        } catch (Exception e) {
            Log.e(TAG, "Error handling browser intent", e);
        }

        finish(); // 处理完后立即关闭
    }

}
