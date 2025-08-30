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
 * 浏览器启动器Activity
 * 处理来自系统的外部链接跳转请求，并转发给WebViewActivity
 */
public class BrowserLauncherActivity extends AppCompatActivity {

    private static final String TAG = "BrowserLauncherActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取启动intent
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        String action = intent.getAction();
        Uri data = intent.getData();

        Log.d(TAG, "BrowserLauncherActivity received: action=" + action + ", data=" + data);

        // 处理不同的intent类型
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            handleViewIntent(data, intent);
        } else {
            // 不支持的intent类型，直接关闭
            Log.w(TAG, "Unsupported intent action: " + action);
            finish();
        }
    }

    /**
     * 处理VIEW intent（主要是URL链接）
     */
    private void handleViewIntent(Uri uri, Intent originalIntent) {
        String scheme = uri.getScheme();
        String url = uri.toString();

        Log.d(TAG, "Handling VIEW intent: " + url);

        // 检查是否是HTTP/HTTPS链接
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            // HTTP/HTTPS链接，转发给WebViewActivity
            launchWebView(url);
        } else if ("tel".equalsIgnoreCase(scheme)) {
            // 电话链接
            handleTelUri(uri);
        } else if ("mailto".equalsIgnoreCase(scheme)) {
            // 邮件链接
            handleMailtoUri(uri);
        } else if ("geo".equalsIgnoreCase(scheme)) {
            // 地理位置链接
            handleGeoUri(uri);
        } else if ("market".equalsIgnoreCase(scheme)) {
            // 应用市场链接
            handleMarketUri(uri);
        } else if ("intent".equalsIgnoreCase(scheme)) {
            // Intent链接
            handleIntentUri(uri);
        } else {
            // 其他类型的链接，尝试用系统默认应用打开
            Log.d(TAG, "Unknown scheme: " + scheme + ", using system default");
            try {
                Intent systemIntent = new Intent(originalIntent);
                systemIntent.setPackage(null); // 清除包名，使用系统默认
                startActivity(systemIntent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch system app for: " + url, e);
            }
        }

        finish();
    }

    /**
     * 启动WebViewActivity处理URL
     */
    private void launchWebView(String url) {
        try {
            Intent webViewIntent = new Intent(this, WebViewActivity.class);
            webViewIntent.setData(Uri.parse(url));
            webViewIntent.setAction(Intent.ACTION_VIEW);
            webViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 添加额外参数
            webViewIntent.putExtra("from_browser_launcher", true);
            webViewIntent.putExtra("browser_mode", true);

            Log.d(TAG, "Launching WebViewActivity with URL: " + url);
            startActivity(webViewIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch WebViewActivity", e);
        }
    }

    /**
     * 处理电话链接
     */
    private void handleTelUri(Uri uri) {
        String phoneNumber = uri.getSchemeSpecificPart();
        Log.d(TAG, "Handling tel URI: " + phoneNumber);

        try {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(uri);
            startActivity(callIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle tel URI", e);
        }
    }

    /**
     * 处理邮件链接
     */
    private void handleMailtoUri(Uri uri) {
        Log.d(TAG, "Handling mailto URI: " + uri);

        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(uri);
            startActivity(emailIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle mailto URI", e);
        }
    }

    /**
     * 处理地理位置链接
     */
    private void handleGeoUri(Uri uri) {
        Log.d(TAG, "Handling geo URI: " + uri);

        try {
            Intent mapIntent = new Intent(Intent.ACTION_VIEW);
            mapIntent.setData(uri);
            startActivity(mapIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle geo URI", e);
        }
    }

    /**
     * 处理应用市场链接
     */
    private void handleMarketUri(Uri uri) {
        Log.d(TAG, "Handling market URI: " + uri);

        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
            marketIntent.setData(uri);
            startActivity(marketIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle market URI", e);
        }
    }

    /**
     * 处理Intent链接
     */
    private void handleIntentUri(Uri uri) {
        Log.d(TAG, "Handling intent URI: " + uri);

        try {
            Intent intentUri = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
            startActivity(intentUri);
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle intent URI", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BrowserLauncherActivity destroyed");
    }
}
