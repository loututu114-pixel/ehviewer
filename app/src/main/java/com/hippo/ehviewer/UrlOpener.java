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
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.client.EhUrlOpener;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.scene.WebViewScene;
import com.hippo.scene.Announcer;
import com.hippo.scene.StageActivity;
import com.hippo.util.ExceptionUtils;

public final class UrlOpener {

    private UrlOpener() {
    }

    /**
     * 判断URL是否是EHentai相关的网站
     */
    private static boolean isEhentaiRelatedUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        // EHentai相关域名
        boolean isEhUrl = lowerUrl.contains("e-hentai.org") ||
                         lowerUrl.contains("exhentai.org") ||
                         lowerUrl.contains("g.e-hentai.org") ||
                         lowerUrl.contains("lofi.e-hentai.org") ||
                         lowerUrl.contains("forums.e-hentai.org") ||
                         lowerUrl.contains("ehwiki.org");

        // 调试信息
        android.util.Log.d("UrlOpener", "isEhentaiRelatedUrl: " + url + " -> " + isEhUrl);

        return isEhUrl;
    }

    public static void openUrl(@NonNull Context context, String url, boolean ehUrl) {
        try {
            if (TextUtils.isEmpty(url)) {
                return;
            }
        } catch (VerifyError ignore) {
            return;
        }

        // 调试信息
        android.util.Log.d("UrlOpener", "Opening URL: " + url + ", ehUrl: " + ehUrl);

        Intent intent;
        Uri uri = Uri.parse(url);

        if (ehUrl) {
            Announcer announcer = EhUrlOpener.parseUrl(url);
            if (null != announcer) {
                intent = new Intent(context, MainActivity.class);
                intent.setAction(StageActivity.ACTION_START_SCENE);
                intent.putExtra(StageActivity.KEY_SCENE_NAME, announcer.getClazz().getName());
                intent.putExtra(StageActivity.KEY_SCENE_ARGS, announcer.getArgs());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
        }

        // 对URL进行判断，决定使用哪种方式打开
        boolean isEhentaiUrl = isEhentaiRelatedUrl(url);

        // 调试信息
        android.util.Log.d("UrlOpener", "URL: " + url + ", isEhentaiUrl: " + isEhentaiUrl);

        if (isEhentaiUrl) {
            // EHentai内部链接，使用系统浏览器
            Intent systemIntent = new Intent(Intent.ACTION_VIEW, uri);
            systemIntent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            try {
                context.startActivity(systemIntent);
            } catch (Throwable e) {
                ExceptionUtils.throwIfFatal(e);
                Toast.makeText(context, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show();
            }
        } else {
            // 外部链接，使用内置WebView浏览器Activity
            try {
                android.util.Log.d("UrlOpener", "Starting WebViewActivity with URL: " + url);

                Intent webViewIntent = new Intent(context, com.hippo.ehviewer.ui.WebViewActivity.class);
                webViewIntent.putExtra(com.hippo.ehviewer.ui.WebViewActivity.EXTRA_URL, url);
                webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webViewIntent);

                android.util.Log.d("UrlOpener", "WebViewActivity started successfully");

            } catch (Throwable e) {
                android.util.Log.e("UrlOpener", "Failed to start WebViewActivity", e);
                ExceptionUtils.throwIfFatal(e);
                // 如果WebView启动失败，回退到系统浏览器
                try {
                    android.util.Log.d("UrlOpener", "Falling back to system browser");
                    Intent systemIntent = new Intent(Intent.ACTION_VIEW, uri);
                    systemIntent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                    context.startActivity(systemIntent);
                } catch (Throwable ex) {
                    android.util.Log.e("UrlOpener", "Failed to start system browser", ex);
                    ExceptionUtils.throwIfFatal(ex);
                    Toast.makeText(context, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
