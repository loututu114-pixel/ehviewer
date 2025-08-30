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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreferenceCompat;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.ReadingModeManager;

/**
 * 阅读模式设置Activity
 */
public class ReadingModeSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("阅读模式设置");
        }

        // 加载设置Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new ReadingModeSettingsFragment())
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * 阅读模式设置Fragment
     */
    public static class ReadingModeSettingsFragment extends PreferenceFragmentCompat {

        private ReadingModeManager mReadingModeManager;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            mReadingModeManager = ReadingModeManager.getInstance(requireContext());

            // 创建设置项
            setupPreferences();
        }

        /**
         * 设置偏好项
         */
        private void setupPreferences() {
            // 字体大小设置
            SeekBarPreference fontSizePref = new SeekBarPreference(requireContext());
            fontSizePref.setKey("reading_font_size");
            fontSizePref.setTitle("字体大小");
            fontSizePref.setSummary("设置阅读模式的字体大小");
            fontSizePref.setMin(12);
            fontSizePref.setMax(32);
            fontSizePref.setValue(mReadingModeManager.getFontSize());
            fontSizePref.setShowSeekBarValue(true);
            fontSizePref.setOnPreferenceChangeListener((preference, newValue) -> {
                mReadingModeManager.setFontSize((Integer) newValue);
                return true;
            });
            getPreferenceScreen().addPreference(fontSizePref);

            // 行高设置
            SeekBarPreference lineHeightPref = new SeekBarPreference(requireContext());
            lineHeightPref.setKey("reading_line_height");
            lineHeightPref.setTitle("行高");
            lineHeightPref.setSummary("设置文字行间距");
            lineHeightPref.setMin(12);
            lineHeightPref.setMax(25);
            lineHeightPref.setValue((int) (mReadingModeManager.getLineHeight() * 10));
            lineHeightPref.setShowSeekBarValue(true);
            lineHeightPref.setOnPreferenceChangeListener((preference, newValue) -> {
                float lineHeight = ((Integer) newValue) / 10.0f;
                mReadingModeManager.setLineHeight(lineHeight);
                return true;
            });
            getPreferenceScreen().addPreference(lineHeightPref);

            // 字体选择
            ListPreference fontPref = new ListPreference(requireContext());
            fontPref.setKey("reading_font_family");
            fontPref.setTitle("字体");
            fontPref.setSummary("选择阅读字体");
            fontPref.setEntries(new CharSequence[]{"衬线体 (Serif)", "无衬线体 (Sans-serif)", "等宽字体 (Monospace)"});
            fontPref.setEntryValues(new CharSequence[]{"serif", "sans-serif", "monospace"});
            fontPref.setValue(mReadingModeManager.getFontFamily());
            fontPref.setOnPreferenceChangeListener((preference, newValue) -> {
                mReadingModeManager.setFontFamily((String) newValue);
                return true;
            });
            getPreferenceScreen().addPreference(fontPref);

            // 主题选择
            ListPreference themePref = new ListPreference(requireContext());
            themePref.setKey("reading_theme");
            themePref.setTitle("主题");
            themePref.setSummary("选择阅读主题");
            themePref.setEntries(new CharSequence[]{"浅色主题", "深色主题"});
            themePref.setEntryValues(new CharSequence[]{"light", "dark"});
            themePref.setValue(mReadingModeManager.getTheme());
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                mReadingModeManager.setTheme((String) newValue);
                return true;
            });
            getPreferenceScreen().addPreference(themePref);

            // 边距设置
            SeekBarPreference marginPref = new SeekBarPreference(requireContext());
            marginPref.setKey("reading_margin");
            marginPref.setTitle("边距");
            marginPref.setSummary("设置页面边距");
            marginPref.setMin(0);
            marginPref.setMax(50);
            marginPref.setValue(mReadingModeManager.getMargin());
            marginPref.setShowSeekBarValue(true);
            marginPref.setOnPreferenceChangeListener((preference, newValue) -> {
                mReadingModeManager.setMargin((Integer) newValue);
                return true;
            });
            getPreferenceScreen().addPreference(marginPref);

            // 重置设置
            Preference resetPref = new Preference(requireContext());
            resetPref.setKey("reading_reset");
            resetPref.setTitle("重置设置");
            resetPref.setSummary("恢复默认阅读设置");
            resetPref.setOnPreferenceClickListener(preference -> {
                mReadingModeManager.resetToDefaults();

                // 刷新所有设置项的值
                fontSizePref.setValue(mReadingModeManager.getFontSize());
                lineHeightPref.setValue((int) (mReadingModeManager.getLineHeight() * 10));
                fontPref.setValue(mReadingModeManager.getFontFamily());
                themePref.setValue(mReadingModeManager.getTheme());
                marginPref.setValue(mReadingModeManager.getMargin());

                return true;
            });
            getPreferenceScreen().addPreference(resetPref);

            // 预览设置
            Preference previewPref = new Preference(requireContext());
            previewPref.setKey("reading_preview");
            previewPref.setTitle("预览效果");
            previewPref.setSummary("查看当前阅读设置的效果");
            previewPref.setOnPreferenceClickListener(preference -> {
                showPreview();
                return true;
            });
            getPreferenceScreen().addPreference(previewPref);
        }

        /**
         * 显示预览
         */
        private void showPreview() {
            // 创建预览HTML内容
            String previewTitle = "阅读模式预览";
            String previewContent =
                "<p>这是阅读模式的预览效果。你可以在这里看到当前设置的字体、大小、行高和主题效果。</p>" +
                "<h2>二级标题示例</h2>" +
                "<p>阅读模式会自动去除网页中的广告和干扰元素，为你提供干净、舒适的阅读体验。可以通过设置调整字体大小、行高、字体类型和主题色彩。</p>" +
                "<blockquote>这是一段引用文字的示例，用来展示引用内容的样式效果。</blockquote>" +
                "<p>继续阅读更多内容，你会发现阅读模式让网页内容更加清晰易读。无论是新闻文章、技术文档还是博客文章，都能在阅读模式下获得更好的阅读体验。</p>" +
                "<h3>三级标题示例</h3>" +
                "<p>最后，这是一个关于如何使用阅读模式的小提示：点击浏览器工具栏上的阅读模式按钮，即可随时切换到舒适的阅读界面。</p>";

            String previewHtml = mReadingModeManager.getReadingModeHtmlTemplate(previewTitle, previewContent);

            // 启动WebViewActivity显示预览
            Intent intent = new Intent(requireContext(), WebViewActivity.class);
            intent.putExtra("html_content", previewHtml);
            intent.putExtra("is_preview", true);
            startActivity(intent);
        }
    }
}
