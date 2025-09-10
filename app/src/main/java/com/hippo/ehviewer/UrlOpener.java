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



    public static void openUrl(@NonNull Context context, String url, boolean ehUrl) {
        try {
            if (TextUtils.isEmpty(url)) {
                return;
            }
        } catch (VerifyError ignore) {
            return;
        }

        // 调试信息
        android.util.Log.d("UrlOpener", "=== UrlOpener.openUrl called ===");
        android.util.Log.d("UrlOpener", "URL: " + url);
        android.util.Log.d("UrlOpener", "ehUrl: " + ehUrl);
        android.util.Log.d("UrlOpener", "Context: " + context.getClass().getSimpleName());

        Intent intent;
        Uri uri = Uri.parse(url);

        // 首先检查是否是EHentai内部链接（能被EhUrlOpener解析的）
        if (ehUrl) {
            Announcer announcer = EhUrlOpener.parseUrl(url);
            if (null != announcer) {
                // 如果是EHentai内部链接，使用原有的Scene处理
                android.util.Log.d("UrlOpener", "EHentai internal URL, using original Scene: " + url);
                intent = new Intent(context, MainActivity.class);
                intent.setAction(StageActivity.ACTION_START_SCENE);
                intent.putExtra(StageActivity.KEY_SCENE_NAME, announcer.getClazz().getName());
                intent.putExtra(StageActivity.KEY_SCENE_ARGS, announcer.getArgs());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
        }

        // 所有其他情况都使用内置WebView浏览器
        android.util.Log.d("UrlOpener", "Using built-in WebView for URL: " + url);

        try {
            Intent webViewIntent = new Intent(context, com.hippo.ehviewer.ui.YCWebViewActivity.class);
            webViewIntent.putExtra(com.hippo.ehviewer.ui.YCWebViewActivity.EXTRA_URL, url);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webViewIntent);

            android.util.Log.d("UrlOpener", "YCWebViewActivity started successfully");

        } catch (Throwable e) {
            android.util.Log.e("UrlOpener", "Failed to start YCWebViewActivity", e);
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
