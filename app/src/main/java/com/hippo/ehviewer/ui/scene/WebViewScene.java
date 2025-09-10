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

package com.hippo.ehviewer.ui.scene;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.ui.YCWebViewActivity;

/**
 * WebView浏览器Scene - 重定向到独立的WebViewActivity
 */
public class WebViewScene extends BaseScene {

    public static final String TAG = WebViewScene.class.getSimpleName();

    public static final String KEY_URL = "url";

    private String mUrl;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.util.Log.d("WebViewScene", "WebViewScene created, redirecting to YCWebViewActivity");

        // 获取URL参数
        Bundle args = getArguments();
        if (args != null) {
            mUrl = args.getString(KEY_URL);
        }

        // 立即启动独立的WebViewActivity
        if (!TextUtils.isEmpty(mUrl)) {
            YCWebViewActivity.start(getContext(), mUrl);
        }

        // 关闭当前Scene
        finish();
    }

    @Nullable
    @Override
    public android.view.View onCreateView2(android.view.LayoutInflater inflater,
                              @Nullable android.view.ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        // 这个方法不会被调用，因为我们在onCreate中就finish了
        return null;
    }

}
