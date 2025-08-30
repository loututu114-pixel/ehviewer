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

package com.hippo.ehviewer.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

/**
 * 阅读模式管理器
 * 提供网页阅读模式的切换和自定义功能
 */
public class ReadingModeManager {

    private static final String TAG = "ReadingModeManager";

    private static final String PREFS_NAME = "reading_mode_prefs";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_LINE_HEIGHT = "line_height";
    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String KEY_THEME = "theme";
    private static final String KEY_MARGIN = "margin";

    private static ReadingModeManager sInstance;

    private Context mContext;
    private SharedPreferences mPreferences;

    // 阅读模式状态
    private boolean mIsReadingModeEnabled = false;
    private String mOriginalContent = null;

    // 阅读模式设置
    private int mFontSize = 18;
    private float mLineHeight = 1.6f;
    private String mFontFamily = "serif";
    private String mTheme = "light";
    private int mMargin = 20;

    /**
     * 获取单例实例
     */
    public static synchronized ReadingModeManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new ReadingModeManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private ReadingModeManager(Context context) {
        this.mContext = context;
        this.mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadSettings();
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        mFontSize = mPreferences.getInt(KEY_FONT_SIZE, 18);
        mLineHeight = mPreferences.getFloat(KEY_LINE_HEIGHT, 1.6f);
        mFontFamily = mPreferences.getString(KEY_FONT_FAMILY, "serif");
        mTheme = mPreferences.getString(KEY_THEME, "light");
        mMargin = mPreferences.getInt(KEY_MARGIN, 20);
    }

    /**
     * 保存设置
     */
    private void saveSettings() {
        mPreferences.edit()
                .putInt(KEY_FONT_SIZE, mFontSize)
                .putFloat(KEY_LINE_HEIGHT, mLineHeight)
                .putString(KEY_FONT_FAMILY, mFontFamily)
                .putString(KEY_THEME, mTheme)
                .putInt(KEY_MARGIN, mMargin)
                .apply();
    }

    /**
     * 启用阅读模式
     */
    public void enableReadingMode(WebView webView) {
        if (webView == null) {
            return;
        }

        try {
            // 保存原始内容
            webView.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })();",
                value -> {
                    if (value != null && !value.equals("null")) {
                        mOriginalContent = value.replaceAll("^\"|\"$", ""); // 移除引号
                    }
                }
            );

            // 应用阅读模式样式
            applyReadingModeStyles(webView);

            mIsReadingModeEnabled = true;
            Log.d(TAG, "Reading mode enabled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable reading mode", e);
        }
    }

    /**
     * 禁用阅读模式
     */
    public void disableReadingMode(WebView webView) {
        if (webView == null) {
            return;
        }

        try {
            // 恢复原始内容
            if (mOriginalContent != null) {
                webView.loadDataWithBaseURL(null, mOriginalContent, "text/html", "UTF-8", null);
            } else {
                // 如果没有原始内容，刷新页面
                webView.reload();
            }

            mIsReadingModeEnabled = false;
            Log.d(TAG, "Reading mode disabled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable reading mode", e);
        }
    }

    /**
     * 应用阅读模式样式
     */
    private void applyReadingModeStyles(WebView webView) {
        String readingModeCSS = generateReadingModeCSS();
        String readingModeJS = generateReadingModeJS();

        // 注入CSS样式
        webView.evaluateJavascript(
            "javascript:(function() { " +
            "var style = document.createElement('style'); " +
            "style.type = 'text/css'; " +
            "style.innerHTML = `" + readingModeCSS + "`; " +
            "document.head.appendChild(style); " +
            "})();",
            null
        );

        // 注入JavaScript处理
        webView.evaluateJavascript(readingModeJS, null);
    }

    /**
     * 生成阅读模式CSS样式
     */
    private String generateReadingModeCSS() {
        String backgroundColor = mTheme.equals("dark") ? "#1a1a1a" : "#f8f6f0";
        String textColor = mTheme.equals("dark") ? "#e8e6e0" : "#2c2c2c";
        String linkColor = mTheme.equals("dark") ? "#4a9eff" : "#0066cc";

        return
            "* { " +
            "  margin: 0; " +
            "  padding: 0; " +
            "  box-sizing: border-box; " +
            "} " +
            "" +
            "body { " +
            "  font-family: '" + mFontFamily + "', serif; " +
            "  font-size: " + mFontSize + "px; " +
            "  line-height: " + mLineHeight + "; " +
            "  color: " + textColor + "; " +
            "  background-color: " + backgroundColor + "; " +
            "  margin: " + mMargin + "px; " +
            "  max-width: none; " +
            "  text-align: left; " +
            "} " +
            "" +
            "h1, h2, h3, h4, h5, h6 { " +
            "  margin: 1.5em 0 0.5em 0; " +
            "  font-weight: bold; " +
            "  line-height: 1.3; " +
            "} " +
            "" +
            "h1 { font-size: " + (mFontSize * 1.6) + "px; } " +
            "h2 { font-size: " + (mFontSize * 1.4) + "px; } " +
            "h3 { font-size: " + (mFontSize * 1.2) + "px; } " +
            "" +
            "p { " +
            "  margin: 0 0 1em 0; " +
            "  text-align: justify; " +
            "  text-justify: inter-word; " +
            "} " +
            "" +
            "a { " +
            "  color: " + linkColor + "; " +
            "  text-decoration: none; " +
            "} " +
            "" +
            "a:hover { " +
            "  text-decoration: underline; " +
            "} " +
            "" +
            "img { " +
            "  max-width: 100%; " +
            "  height: auto; " +
            "  margin: 1em 0; " +
            "  border-radius: 4px; " +
            "} " +
            "" +
            "blockquote { " +
            "  margin: 1em 2em; " +
            "  padding: 0.5em 1em; " +
            "  border-left: 4px solid " + linkColor + "; " +
            "  background-color: " + (mTheme.equals("dark") ? "#2a2a2a" : "#f0f0f0") + "; " +
            "  font-style: italic; " +
            "} " +
            "" +
            "code, pre { " +
            "  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace; " +
            "  background-color: " + (mTheme.equals("dark") ? "#2a2a2a" : "#f0f0f0") + "; " +
            "  padding: 0.2em 0.4em; " +
            "  border-radius: 3px; " +
            "} " +
            "" +
            "pre { " +
            "  white-space: pre-wrap; " +
            "  margin: 1em 0; " +
            "  padding: 1em; " +
            "  overflow-x: auto; " +
            "} " +
            "" +
            "ul, ol { " +
            "  margin: 1em 0; " +
            "  padding-left: 2em; " +
            "} " +
            "" +
            "li { " +
            "  margin: 0.5em 0; " +
            "} " +
            "" +
            "table { " +
            "  width: 100%; " +
            "  border-collapse: collapse; " +
            "  margin: 1em 0; " +
            "} " +
            "" +
            "th, td { " +
            "  border: 1px solid " + (mTheme.equals("dark") ? "#444" : "#ddd") + "; " +
            "  padding: 0.5em; " +
            "  text-align: left; " +
            "} " +
            "" +
            "th { " +
            "  background-color: " + (mTheme.equals("dark") ? "#333" : "#f5f5f5") + "; " +
            "  font-weight: bold; " +
            "} " +
            "" +
            "// 隐藏广告和干扰元素 " +
            ".ad, .ads, .advertisement, .banner, .popup, .modal, .overlay, " +
            ".social-share, .related-posts, .sidebar, .comments, .footer, " +
            ".header, .nav, .menu, .widget { " +
            "  display: none !important; " +
            "} ";
    }

    /**
     * 生成阅读模式JavaScript
     */
    private String generateReadingModeJS() {
        return
            "(function() {" +
            "  console.log('Reading mode activated');" +
            "" +
            "  // 移除脚本和样式" +
            "  var scripts = document.querySelectorAll('script, link[rel=\"stylesheet\"], style');" +
            "  scripts.forEach(function(script) {" +
            "    if (!script.innerHTML.includes('reading-mode-style')) {" +
            "      script.remove();" +
            "    }" +
            "  });" +
            "" +
            "  // 移除不需要的元素" +
            "  var unwantedSelectors = [" +
            "    '.ad', '.ads', '.advertisement', '.banner', '.popup', '.modal', '.overlay'," +
            "    '.social-share', '.related-posts', '.sidebar', '.comments', '.footer'," +
            "    '.header', '.nav', '.menu', '.widget', 'aside', 'nav', 'header', 'footer'" +
            "  ];" +
            "" +
            "  unwantedSelectors.forEach(function(selector) {" +
            "    var elements = document.querySelectorAll(selector);" +
            "    elements.forEach(function(el) {" +
            "      el.remove();" +
            "    });" +
            "  });" +
            "" +
            "  // 提取主要内容" +
            "  var contentSelectors = [" +
            "    'article', '.post', '.entry', '.content', '.main', '#main'," +
            "    '.article', '.story', '.post-content', '.entry-content'" +
            "  ];" +
            "" +
            "  var mainContent = null;" +
            "  for (var i = 0; i < contentSelectors.length; i++) {" +
            "    var element = document.querySelector(contentSelectors[i]);" +
            "    if (element && element.textContent.trim().length > 200) {" +
            "      mainContent = element;" +
            "      break;" +
            "    }" +
            "  }" +
            "" +
            "  // 如果找到主要内容，替换body" +
            "  if (mainContent) {" +
            "    document.body.innerHTML = mainContent.innerHTML;" +
            "  }" +
            "" +
            "  // 重新设置页面标题" +
            "  var titleSelectors = ['h1', 'h2', '.title', '.headline'];" +
            "  var title = '';" +
            "  for (var i = 0; i < titleSelectors.length; i++) {" +
            "    var titleEl = document.querySelector(titleSelectors[i]);" +
            "    if (titleEl && titleEl.textContent.trim()) {" +
            "      title = titleEl.textContent.trim();" +
            "      break;" +
            "    }" +
            "  }" +
            "" +
            "  if (title) {" +
            "    document.title = title;" +
            "  }" +
            "" +
            "  console.log('Reading mode processing completed');" +
            "})();";
    }

    /**
     * 检查网页是否适合阅读模式
     */
    public boolean isSuitableForReadingMode(String url, String title) {
        if (url == null || title == null) {
            return false;
        }

        // 检查URL是否为文章页面
        String lowerUrl = url.toLowerCase();
        String lowerTitle = title.toLowerCase();

        // 排除非文章页面
        if (lowerUrl.contains("login") ||
            lowerUrl.contains("register") ||
            lowerUrl.contains("search") ||
            lowerUrl.contains("category") ||
            lowerUrl.contains("tag") ||
            lowerUrl.contains("author")) {
            return false;
        }

        // 检查标题长度（文章通常有较长的标题）
        return title.length() > 10 && title.length() < 200;
    }

    /**
     * 获取阅读模式状态
     */
    public boolean isReadingModeEnabled() {
        return mIsReadingModeEnabled;
    }

    /**
     * 设置字体大小
     */
    public void setFontSize(int fontSize) {
        this.mFontSize = Math.max(12, Math.min(32, fontSize));
        saveSettings();
    }

    /**
     * 获取字体大小
     */
    public int getFontSize() {
        return mFontSize;
    }

    /**
     * 设置行高
     */
    public void setLineHeight(float lineHeight) {
        this.mLineHeight = Math.max(1.2f, Math.min(2.5f, lineHeight));
        saveSettings();
    }

    /**
     * 获取行高
     */
    public float getLineHeight() {
        return mLineHeight;
    }

    /**
     * 设置字体
     */
    public void setFontFamily(String fontFamily) {
        this.mFontFamily = fontFamily != null ? fontFamily : "serif";
        saveSettings();
    }

    /**
     * 获取字体
     */
    public String getFontFamily() {
        return mFontFamily;
    }

    /**
     * 设置主题
     */
    public void setTheme(String theme) {
        this.mTheme = theme != null ? theme : "light";
        saveSettings();
    }

    /**
     * 获取主题
     */
    public String getTheme() {
        return mTheme;
    }

    /**
     * 设置边距
     */
    public void setMargin(int margin) {
        this.mMargin = Math.max(0, Math.min(100, margin));
        saveSettings();
    }

    /**
     * 获取边距
     */
    public int getMargin() {
        return mMargin;
    }

    /**
     * 切换主题
     */
    public void toggleTheme() {
        mTheme = mTheme.equals("dark") ? "light" : "dark";
        saveSettings();
    }

    /**
     * 重置为默认设置
     */
    public void resetToDefaults() {
        mFontSize = 18;
        mLineHeight = 1.6f;
        mFontFamily = "serif";
        mTheme = "light";
        mMargin = 20;
        saveSettings();
    }

    /**
     * 获取阅读模式的HTML模板
     */
    public String getReadingModeHtmlTemplate(String title, String content) {
        String backgroundColor = mTheme.equals("dark") ? "#1a1a1a" : "#f8f6f0";
        String textColor = mTheme.equals("dark") ? "#e8e6e0" : "#2c2c2c";

        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "  <meta charset='UTF-8'>" +
               "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "  <title>" + (title != null ? title : "阅读模式") + "</title>" +
               "  <style>" +
               "    body {" +
               "      font-family: '" + mFontFamily + "', serif;" +
               "      font-size: " + mFontSize + "px;" +
               "      line-height: " + mLineHeight + ";" +
               "      color: " + textColor + ";" +
               "      background-color: " + backgroundColor + ";" +
               "      margin: " + mMargin + "px;" +
               "      padding: 0;" +
               "      max-width: none;" +
               "    }" +
               "    .reading-title {" +
               "      font-size: " + (mFontSize * 1.4) + "px;" +
               "      font-weight: bold;" +
               "      margin-bottom: 1em;" +
               "      text-align: center;" +
               "      border-bottom: 2px solid " + (mTheme.equals("dark") ? "#444" : "#ddd") + ";" +
               "      padding-bottom: 0.5em;" +
               "    }" +
               "    .reading-content {" +
               "      text-align: justify;" +
               "      text-justify: inter-word;" +
               "    }" +
               "  </style>" +
               "</head>" +
               "<body>" +
               "  <h1 class='reading-title'>" + (title != null ? title : "阅读模式") + "</h1>" +
               "  <div class='reading-content'>" + (content != null ? content : "") + "</div>" +
               "</body>" +
               "</html>";
    }
}
