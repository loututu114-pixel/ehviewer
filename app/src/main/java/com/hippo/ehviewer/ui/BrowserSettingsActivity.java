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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;

/**
 * 浏览器设置Activity
 * 提供浏览器相关的设置选项
 */
public class BrowserSettingsActivity extends AppCompatActivity {

    private static final String TAG = "BrowserSettingsActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("浏览器设置");
        }

        // 加载设置Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new BrowserSettingsFragment())
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * 浏览器设置Fragment
     */
    public static class BrowserSettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String PREF_DEFAULT_SEARCH_ENGINE = "pref_default_search_engine";
        private static final String PREF_ENABLE_AD_BLOCKING = "pref_enable_ad_blocking";
        private static final String PREF_ENABLE_JAVASCRIPT = "pref_enable_javascript";
        private static final String PREF_ENABLE_IMAGES = "pref_enable_images";
        private static final String PREF_SET_AS_DEFAULT_BROWSER = "pref_set_as_default_browser";
        private static final String PREF_CLEAR_BROWSER_DATA = "pref_clear_browser_data";
        private static final String PREF_ABOUT_BROWSER = "pref_about_browser";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // 设置偏好设置
            getPreferenceManager().setSharedPreferencesName("browser_settings");

            // 创建设置项
            addPreferencesFromResource(createPreferencesXml());

            // 设置偏好点击监听器
            setupPreferenceListeners();
        }

        /**
         * 创建偏好设置XML（动态创建）
         */
        private int createPreferencesXml() {
            // 这里我们动态创建偏好设置，而不是使用XML文件
            // 为简单起见，我们直接在代码中创建设置项

            // 默认搜索引擎设置
            androidx.preference.ListPreference searchEnginePref = new androidx.preference.ListPreference(getContext());
            searchEnginePref.setKey(PREF_DEFAULT_SEARCH_ENGINE);
            searchEnginePref.setTitle("默认搜索引擎");
            searchEnginePref.setSummary("选择默认的搜索引擎");
            searchEnginePref.setEntries(new CharSequence[]{"Google", "百度", "必应", "DuckDuckGo"});
            searchEnginePref.setEntryValues(new CharSequence[]{"google", "baidu", "bing", "duckduckgo"});
            searchEnginePref.setDefaultValue("google");
            getPreferenceScreen().addPreference(searchEnginePref);

            // 广告拦截开关
            androidx.preference.SwitchPreference adBlockPref = new androidx.preference.SwitchPreference(getContext());
            adBlockPref.setKey(PREF_ENABLE_AD_BLOCKING);
            adBlockPref.setTitle("启用广告拦截");
            adBlockPref.setSummary("拦截网页中的广告内容");
            adBlockPref.setDefaultValue(true);
            getPreferenceScreen().addPreference(adBlockPref);

            // JavaScript开关
            androidx.preference.SwitchPreference jsPref = new androidx.preference.SwitchPreference(getContext());
            jsPref.setKey(PREF_ENABLE_JAVASCRIPT);
            jsPref.setTitle("启用JavaScript");
            jsPref.setSummary("允许网页运行JavaScript代码");
            jsPref.setDefaultValue(true);
            getPreferenceScreen().addPreference(jsPref);

            // 图片加载开关
            androidx.preference.SwitchPreference imagePref = new androidx.preference.SwitchPreference(getContext());
            imagePref.setKey(PREF_ENABLE_IMAGES);
            imagePref.setTitle("自动加载图片");
            imagePref.setSummary("自动加载网页中的图片");
            imagePref.setDefaultValue(true);
            getPreferenceScreen().addPreference(imagePref);

            // 设置为默认浏览器
            androidx.preference.Preference defaultBrowserPref = new androidx.preference.Preference(getContext());
            defaultBrowserPref.setKey(PREF_SET_AS_DEFAULT_BROWSER);
            defaultBrowserPref.setTitle("设为默认浏览器");

            // 根据当前状态设置摘要
            String status = com.hippo.ehviewer.util.DefaultBrowserHelper.getBrowserStatusDescription(getContext());
            defaultBrowserPref.setSummary(status);

            getPreferenceScreen().addPreference(defaultBrowserPref);

            // 清除浏览器数据
            androidx.preference.Preference clearDataPref = new androidx.preference.Preference(getContext());
            clearDataPref.setKey(PREF_CLEAR_BROWSER_DATA);
            clearDataPref.setTitle("清除浏览器数据");
            clearDataPref.setSummary("清除缓存、历史记录和Cookie");
            getPreferenceScreen().addPreference(clearDataPref);

            // 阅读模式设置
            androidx.preference.Preference readingSettingsPref = new androidx.preference.Preference(getContext());
            readingSettingsPref.setKey("pref_reading_settings");
            readingSettingsPref.setTitle("阅读模式设置");
            readingSettingsPref.setSummary("自定义阅读模式的字体、主题等");
            getPreferenceScreen().addPreference(readingSettingsPref);

            // 关于浏览器
            androidx.preference.Preference aboutPref = new androidx.preference.Preference(getContext());
            aboutPref.setKey(PREF_ABOUT_BROWSER);
            aboutPref.setTitle("关于EhViewer浏览器");
            aboutPref.setSummary("版本信息和帮助");
            getPreferenceScreen().addPreference(aboutPref);

            return 0; // 返回0表示我们动态创建了所有设置项
        }

        /**
         * 设置偏好点击监听器
         */
        private void setupPreferenceListeners() {
            // 设置为默认浏览器
            androidx.preference.Preference defaultBrowserPref = findPreference(PREF_SET_AS_DEFAULT_BROWSER);
            if (defaultBrowserPref != null) {
                defaultBrowserPref.setOnPreferenceClickListener(preference -> {
                    setAsDefaultBrowser();
                    return true;
                });
            }

            // 清除浏览器数据
            androidx.preference.Preference clearDataPref = findPreference(PREF_CLEAR_BROWSER_DATA);
            if (clearDataPref != null) {
                clearDataPref.setOnPreferenceClickListener(preference -> {
                    clearBrowserData();
                    return true;
                });
            }

            // 阅读模式设置
            androidx.preference.Preference readingSettingsPref = findPreference("pref_reading_settings");
            if (readingSettingsPref != null) {
                readingSettingsPref.setOnPreferenceClickListener(preference -> {
                    Intent readingSettingsIntent = new Intent(getContext(), ReadingModeSettingsActivity.class);
                    getContext().startActivity(readingSettingsIntent);
                    return true;
                });
            }

            // 关于浏览器
            androidx.preference.Preference aboutPref = findPreference(PREF_ABOUT_BROWSER);
            if (aboutPref != null) {
                aboutPref.setOnPreferenceClickListener(preference -> {
                    showAboutBrowser();
                    return true;
                });
            }
        }

        /**
         * 设置为默认浏览器
         */
        private void setAsDefaultBrowser() {
            boolean success = com.hippo.ehviewer.util.DefaultBrowserHelper.trySetAsDefaultBrowser(getContext());

            if (!success) {
                // 如果自动设置失败，显示手动设置提示
                androidx.appcompat.app.AlertDialog.Builder builder =
                        new androidx.appcompat.app.AlertDialog.Builder(getContext());
                builder.setTitle("设置默认浏览器");
                builder.setMessage("请在系统设置中手动将EhViewer设为默认浏览器应用。");
                builder.setPositiveButton("去设置", (dialog, which) -> {
                    openDefaultBrowserSettings();
                });
                builder.setNegativeButton("取消", null);
                builder.show();
            }
        }

        /**
         * 打开默认浏览器设置
         */
        private void openDefaultBrowserSettings() {
            boolean success = com.hippo.ehviewer.util.DefaultBrowserHelper.openDefaultBrowserSettings(getContext());

            if (!success) {
                androidx.appcompat.app.AlertDialog.Builder builder =
                        new androidx.appcompat.app.AlertDialog.Builder(getContext());
                builder.setTitle("无法打开设置");
                builder.setMessage("请手动前往系统设置 > 应用 > 默认应用 > 浏览器 中设置EhViewer为默认浏览器。");
                builder.setPositiveButton("确定", null);
                builder.show();
            }
        }

        /**
         * 清除浏览器数据
         */
        private void clearBrowserData() {
            androidx.appcompat.app.AlertDialog.Builder builder =
                    new androidx.appcompat.app.AlertDialog.Builder(getContext());
            builder.setTitle("清除浏览器数据");
            builder.setMessage("这将清除缓存、历史记录和Cookie。确定要继续吗？");
            builder.setPositiveButton("确定", (dialog, which) -> {
                // 这里应该调用WebViewActivity的clearBrowsingData方法
                // 由于我们在Fragment中，可以通过广播或回调来实现
                performClearBrowserData();
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        }

        /**
         * 执行清除浏览器数据操作
         */
        private void performClearBrowserData() {
            try {
                // 清除SharedPreferences
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                if (prefs != null) {
                    prefs.edit().clear().apply();
                }

                // 使用WebViewCacheManager清除缓存
                com.hippo.ehviewer.client.WebViewCacheManager cacheManager =
                    com.hippo.ehviewer.client.WebViewCacheManager.getInstance(getContext());
                cacheManager.clearAllCache();

                // 显示成功提示
                androidx.appcompat.app.AlertDialog.Builder successDialog =
                        new androidx.appcompat.app.AlertDialog.Builder(getContext());
                successDialog.setTitle("清除完成");
                successDialog.setMessage("浏览器数据已清除");
                successDialog.setPositiveButton("确定", null);
                successDialog.show();

            } catch (Exception e) {
                Log.e(TAG, "Failed to clear browser data", e);
                androidx.appcompat.app.AlertDialog.Builder errorDialog =
                        new androidx.appcompat.app.AlertDialog.Builder(getContext());
                errorDialog.setTitle("清除失败");
                errorDialog.setMessage("清除浏览器数据时发生错误: " + e.getMessage());
                errorDialog.setPositiveButton("确定", null);
                errorDialog.show();
            }
        }

        /**
         * 显示关于浏览器信息
         */
        private void showAboutBrowser() {
            androidx.appcompat.app.AlertDialog.Builder builder =
                    new androidx.appcompat.app.AlertDialog.Builder(getContext());
            builder.setTitle("关于EhViewer浏览器");
            builder.setMessage("EhViewer浏览器功能\n\n" +
                    "• 多标签页浏览\n" +
                    "• 广告拦截\n" +
                    "• X5高速引擎\n" +
                    "• 书签管理\n" +
                    "• 隐私保护\n\n" +
                    "版本: 1.0.0");
            builder.setPositiveButton("确定", null);
            builder.show();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, "Preference changed: " + key);

            // 处理偏好设置变化
            switch (key) {
                case PREF_DEFAULT_SEARCH_ENGINE:
                    String searchEngine = sharedPreferences.getString(key, "google");
                    Log.d(TAG, "Default search engine changed to: " + searchEngine);
                    break;

                case PREF_ENABLE_AD_BLOCKING:
                    boolean adBlocking = sharedPreferences.getBoolean(key, true);
                    Log.d(TAG, "Ad blocking " + (adBlocking ? "enabled" : "disabled"));
                    break;

                case PREF_ENABLE_JAVASCRIPT:
                    boolean jsEnabled = sharedPreferences.getBoolean(key, true);
                    Log.d(TAG, "JavaScript " + (jsEnabled ? "enabled" : "disabled"));
                    break;

                case PREF_ENABLE_IMAGES:
                    boolean imagesEnabled = sharedPreferences.getBoolean(key, true);
                    Log.d(TAG, "Image loading " + (imagesEnabled ? "enabled" : "disabled"));
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}
