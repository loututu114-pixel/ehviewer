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
     * 处理浏览器intent
     */
    private void handleBrowserIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        String action = intent.getAction();
        String browserMode = intent.getStringExtra(EXTRA_BROWSER_MODE);
        Uri data = intent.getData();

        Log.d(TAG, "BrowserLauncherActivity received: action=" + action +
                   ", browserMode=" + browserMode + ", data=" + data);

        // 检查是否是从桌面浏览器图标启动
        if (BROWSER_MODE_HOME.equals(browserMode)) {
            // 从桌面浏览器图标启动，直接进入浏览器主页
            startBrowserWithHomePage();
            finish();
            return;
        }

        // 处理VIEW action（从外部链接启动）
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            handleViewIntent(data, intent);
        } else if (Intent.ACTION_MAIN.equals(action) && BROWSER_MODE_HOME.equals(browserMode)) {
            // 从桌面图标启动，显示浏览器主页
            startBrowserWithHomePage();
            finish();
        } else {
            // 其他情况，启动主应用
            Log.d(TAG, "Starting main application for unsupported intent");
            startMainApplication(intent);
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

    /**
     * 启动浏览器主页
     */
    private void startBrowserWithHomePage() {
        try {
            Intent browserIntent = new Intent(this, WebViewActivity.class);
            browserIntent.setAction(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse("https://www.google.com"));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // 标记为从浏览器启动
            browserIntent.putExtra("from_browser_launcher", true);
            browserIntent.putExtra("browser_mode", true);

            Log.d(TAG, "Starting browser with home page");
            startActivity(browserIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start browser with home page", e);
        }
    }

    /**
     * 启动浏览器并打开指定URL
     */
    private void startBrowserWithUrl(String url) {
        try {
            Intent browserIntent = new Intent(this, WebViewActivity.class);
            browserIntent.setAction(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(url));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // 标记为从浏览器启动
            browserIntent.putExtra("from_browser_launcher", true);
            browserIntent.putExtra("browser_mode", true);

            Log.d(TAG, "Starting browser with URL: " + url);
            startActivity(browserIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start browser with URL: " + url, e);
        }
    }

    /**
     * 启动主应用程序
     */
    private void startMainApplication(Intent originalIntent) {
        try {
            Intent mainIntent = new Intent(this, MainActivity.class);
            if (originalIntent != null) {
                // 传递原始intent的数据
                mainIntent.setAction(originalIntent.getAction());
                mainIntent.setData(originalIntent.getData());
                mainIntent.putExtras(originalIntent.getExtras());
            }
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Log.d(TAG, "Starting main application");
            startActivity(mainIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start main application", e);
        }
    }

    /**
     * 启动EhViewer浏览器（静态方法）
     */
    public static void startEhBrowser(android.content.Context context) {
        try {
            Intent intent = new Intent(context, BrowserLauncherActivity.class);
            intent.putExtra(EXTRA_BROWSER_MODE, BROWSER_MODE_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start EhViewer browser", e);
        }
    }

    /**
     * 处理浏览器URL（静态方法）
     */
    public static void handleBrowserUrl(android.content.Context context, String url) {
        try {
            Intent intent = new Intent(context, BrowserLauncherActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle browser URL: " + url, e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BrowserLauncherActivity destroyed");
    }
}
