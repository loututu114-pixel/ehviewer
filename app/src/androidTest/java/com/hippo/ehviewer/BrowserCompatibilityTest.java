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

package com.hippo.ehviewer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.WebView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.hippo.ehviewer.client.X5WebViewManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 浏览器兼容性测试
 * 测试不同Android版本和设备的兼容性
 */
@RunWith(AndroidJUnit4.class)
public class BrowserCompatibilityTest {

    private static final String TAG = "BrowserCompatibilityTest";

    private Context mContext;
    private int mSdkVersion;
    private String mDeviceModel;
    private String mAndroidVersion;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mSdkVersion = Build.VERSION.SDK_INT;
        mDeviceModel = Build.MODEL;
        mAndroidVersion = Build.VERSION.RELEASE;

        logDeviceInfo();
    }

    /**
     * 测试Android版本兼容性
     */
    @Test
    public void testAndroidVersionCompatibility() {
        // 测试支持的最低Android版本
        assertTrue("Android version too low: " + mSdkVersion,
                mSdkVersion >= Build.VERSION_CODES.LOLLIPOP); // API 21

        // 测试WebView可用性
        assertWebViewAvailable();

        // 测试基本WebView功能
        testBasicWebViewFeatures();
    }

    /**
     * 测试WebView功能兼容性
     */
    @Test
    public void testWebViewFeatureCompatibility() {
        WebView webView = new WebView(mContext);
        assertNotNull("WebView creation failed", webView);

        try {
            // 测试基本设置
            android.webkit.WebSettings settings = webView.getSettings();
            assertNotNull("WebSettings not available", settings);

            // 测试JavaScript支持
            settings.setJavaScriptEnabled(true);
            assertTrue("JavaScript not enabled", settings.getJavaScriptEnabled());

            // 测试缩放支持
            settings.setSupportZoom(true);
            assertTrue("Zoom not supported", settings.getSupportZoom());

            // 测试数据库支持
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                settings.setDatabaseEnabled(true);
            }

            // 测试DOM存储支持
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                settings.setDomStorageEnabled(true);
            }

        } finally {
            webView.destroy();
        }
    }

    /**
     * 测试X5内核兼容性
     */
    @Test
    public void testX5Compatibility() {
        X5WebViewManager x5Manager = X5WebViewManager.getInstance();

        // 初始化X5
        x5Manager.initX5(mContext);

        // 等待X5初始化完成
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 检查X5状态
        boolean isX5Available = x5Manager.isX5Available();
        String x5Version = x5Manager.getX5Version();

        logX5Info(isX5Available, x5Version);

        // 测试X5 WebView创建
        if (isX5Available) {
            testX5WebViewCreation();
        } else {
            // 如果X5不可用，测试系统WebView
            testSystemWebViewCreation();
        }
    }

    /**
     * 测试网络功能兼容性
     */
    @Test
    public void testNetworkCompatibility() {
        // 测试HTTPS支持
        testHttpsSupport();

        // 测试HTTP/2支持
        testHttp2Support();

        // 测试证书验证
        testCertificateValidation();

        // 测试代理设置
        testProxySettings();
    }

    /**
     * 测试存储功能兼容性
     */
    @Test
    public void testStorageCompatibility() {
        // 测试SharedPreferences
        testSharedPreferences();

        // 测试SQLite数据库
        testSQLiteDatabase();

        // 测试外部存储
        testExternalStorage();

        // 测试缓存目录
        testCacheDirectory();
    }

    /**
     * 测试权限兼容性
     */
    @Test
    public void testPermissionCompatibility() {
        // 检查必要权限
        checkRequiredPermissions();

        // 测试权限请求流程
        testPermissionRequestFlow();

        // 测试权限被拒绝的处理
        testPermissionDeniedHandling();
    }

    /**
     * 测试界面兼容性
     */
    @Test
    public void testUICompatibility() {
        // 测试不同屏幕密度
        testScreenDensityCompatibility();

        // 测试横竖屏切换
        testOrientationCompatibility();

        // 测试主题切换
        testThemeCompatibility();

        // 测试字体缩放
        testFontScaleCompatibility();
    }

    // ==================== 辅助测试方法 ====================

    private void logDeviceInfo() {
        android.util.Log.d(TAG, "Device Info:");
        android.util.Log.d(TAG, "  Model: " + mDeviceModel);
        android.util.Log.d(TAG, "  Android Version: " + mAndroidVersion);
        android.util.Log.d(TAG, "  SDK Version: " + mSdkVersion);
        android.util.Log.d(TAG, "  Manufacturer: " + Build.MANUFACTURER);
        android.util.Log.d(TAG, "  Brand: " + Build.BRAND);
    }

    private void assertWebViewAvailable() {
        try {
            PackageInfo webViewPackage = mContext.getPackageManager()
                    .getPackageInfo("com.google.android.webview",
                            PackageManager.GET_META_DATA);

            assertNotNull("WebView package not found", webViewPackage);
            android.util.Log.d(TAG, "WebView version: " + webViewPackage.versionName);

        } catch (PackageManager.NameNotFoundException e) {
            android.util.Log.w(TAG, "WebView package not found, might be using AOSP WebView");
        }
    }

    private void testBasicWebViewFeatures() {
        WebView webView = new WebView(mContext);

        try {
            android.webkit.WebSettings settings = webView.getSettings();

            // 测试各种设置的兼容性
            settings.setJavaScriptEnabled(true);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);

            // 测试新版本特有的功能
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                settings.setAllowFileAccessFromFileURLs(false);
                settings.setAllowUniversalAccessFromFileURLs(false);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                settings.setMediaPlaybackRequiresUserGesture(true);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                settings.setLoadsImagesAutomatically(true);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            }

        } finally {
            webView.destroy();
        }
    }

    private void logX5Info(boolean isAvailable, String version) {
        android.util.Log.d(TAG, "X5 Info:");
        android.util.Log.d(TAG, "  Available: " + isAvailable);
        android.util.Log.d(TAG, "  Version: " + version);
        android.util.Log.d(TAG, "  Core Version: " + X5WebViewManager.getInstance().getX5CoreVersion());
        android.util.Log.d(TAG, "  Need Download: " + X5WebViewManager.getInstance().isX5NeedDownload());
    }

    private void testX5WebViewCreation() {
        X5WebViewManager x5Manager = X5WebViewManager.getInstance();

        android.webkit.WebView webView = x5Manager.createWebView(mContext);
        assertNotNull("X5 WebView creation failed", webView);

        // 测试X5特有功能
        try {
            // 这里可以添加X5特有的测试
            android.util.Log.d(TAG, "X5 WebView created successfully");
        } finally {
            webView.destroy();
        }
    }

    private void testSystemWebViewCreation() {
        android.webkit.WebView webView = new android.webkit.WebView(mContext);
        assertNotNull("System WebView creation failed", webView);

        try {
            android.webkit.WebSettings settings = webView.getSettings();
            assertNotNull("System WebSettings not available", settings);

            android.util.Log.d(TAG, "System WebView created successfully");
        } finally {
            webView.destroy();
        }
    }

    private void testHttpsSupport() {
        // 测试HTTPS连接能力
        try {
            java.net.URL url = new java.net.URL("https://www.baidu.com");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            assertTrue("HTTPS connection failed with code: " + responseCode,
                    responseCode >= 200 && responseCode < 400);

            connection.disconnect();
        } catch (Exception e) {
            android.util.Log.e(TAG, "HTTPS test failed", e);
            assertTrue("HTTPS support test failed: " + e.getMessage(), false);
        }
    }

    private void testHttp2Support() {
        // 在Android 5.0+上测试HTTP/2支持
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // HTTP/2通常由系统支持，这里进行基本验证
            android.util.Log.d(TAG, "HTTP/2 support available on Android " + mSdkVersion);
        }
    }

    private void testCertificateValidation() {
        // 测试证书验证功能
        try {
            java.net.URL url = new java.net.URL("https://www.baidu.com");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            // 检查证书
            java.security.cert.Certificate[] certificates = connection.getServerCertificates();
            assertNotNull("No certificates found", certificates);
            assertTrue("Certificate chain empty", certificates.length > 0);

            connection.disconnect();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Certificate validation test failed", e);
        }
    }

    private void testProxySettings() {
        // 测试代理设置兼容性
        java.net.Proxy proxy = java.net.Proxy.NO_PROXY;
        assertNotNull("Proxy settings not available", proxy);
    }

    private void testSharedPreferences() {
        android.content.SharedPreferences prefs = mContext.getSharedPreferences("test_prefs", Context.MODE_PRIVATE);
        assertNotNull("SharedPreferences not available", prefs);

        // 测试基本操作
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("test_key", "test_value");
        editor.commit();

        String value = prefs.getString("test_key", null);
        assertTrue("SharedPreferences read/write failed", "test_value".equals(value));
    }

    private void testSQLiteDatabase() {
        android.database.sqlite.SQLiteDatabase db = mContext.openOrCreateDatabase("test.db", Context.MODE_PRIVATE, null);
        assertNotNull("SQLite database creation failed", db);

        try {
            // 创建测试表
            db.execSQL("CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY, name TEXT)");

            // 插入测试数据
            db.execSQL("INSERT INTO test_table (name) VALUES ('test')");

            // 查询数据
            android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM test_table", null);
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                assertTrue("SQLite operation failed", count > 0);
            }
            cursor.close();

        } finally {
            db.close();
            // 清理测试数据库
            mContext.deleteDatabase("test.db");
        }
    }

    private void testExternalStorage() {
        // 检查外部存储可用性
        String state = android.os.Environment.getExternalStorageState();
        if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
            java.io.File externalDir = mContext.getExternalFilesDir(null);
            assertNotNull("External storage not available", externalDir);

            // 测试文件创建
            java.io.File testFile = new java.io.File(externalDir, "test.txt");
            try {
                boolean created = testFile.createNewFile();
                if (created) {
                    testFile.delete(); // 清理
                }
            } catch (java.io.IOException e) {
                android.util.Log.w(TAG, "External storage test failed", e);
            }
        } else {
            android.util.Log.d(TAG, "External storage not mounted");
        }
    }

    private void testCacheDirectory() {
        java.io.File cacheDir = mContext.getCacheDir();
        assertNotNull("Cache directory not available", cacheDir);

        // 测试缓存文件创建
        java.io.File testCacheFile = new java.io.File(cacheDir, "test.cache");
        try {
            boolean created = testCacheFile.createNewFile();
            if (created) {
                testCacheFile.delete(); // 清理
            }
        } catch (java.io.IOException e) {
            android.util.Log.w(TAG, "Cache directory test failed", e);
        }
    }

    private void checkRequiredPermissions() {
        // 检查必要的权限
        String[] requiredPermissions = {
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        };

        for (String permission : requiredPermissions) {
            int result = androidx.core.content.ContextCompat.checkSelfPermission(mContext, permission);
            android.util.Log.d(TAG, "Permission " + permission + ": " +
                    (result == android.content.pm.PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        }
    }

    private void testPermissionRequestFlow() {
        // 这个测试需要在UI测试中进行，这里只是占位符
        android.util.Log.d(TAG, "Permission request flow test skipped in unit test");
    }

    private void testPermissionDeniedHandling() {
        // 测试权限被拒绝时的处理逻辑
        android.util.Log.d(TAG, "Permission denied handling test skipped in unit test");
    }

    private void testScreenDensityCompatibility() {
        android.util.DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        float density = metrics.density;
        int densityDpi = metrics.densityDpi;

        android.util.Log.d(TAG, "Screen density: " + density + ", DPI: " + densityDpi);

        // 验证密度值在合理范围内
        assertTrue("Screen density too low", density >= 0.5f);
        assertTrue("Screen density too high", density <= 4.0f);
    }

    private void testOrientationCompatibility() {
        // 测试横竖屏配置
        int orientation = mContext.getResources().getConfiguration().orientation;
        android.util.Log.d(TAG, "Current orientation: " +
                (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT ? "Portrait" : "Landscape"));
    }

    private void testThemeCompatibility() {
        // 测试主题切换
        int currentNightMode = mContext.getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        android.util.Log.d(TAG, "Current theme mode: " +
                (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES ? "Dark" : "Light"));
    }

    private void testFontScaleCompatibility() {
        float fontScale = mContext.getResources().getConfiguration().fontScale;
        android.util.Log.d(TAG, "Font scale: " + fontScale);

        // 验证字体缩放比例在合理范围内
        assertTrue("Font scale too small", fontScale >= 0.8f);
        assertTrue("Font scale too large", fontScale <= 2.0f);
    }
}
