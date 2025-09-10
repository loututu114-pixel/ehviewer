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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;

/**
 * Eh浏览器桌面图标Activity - 简化版本
 * 这个Activity专门用于桌面上的"Eh浏览器"图标
 * 点击后直接进入浏览器界面，避免功能重复
 */
public class EhBrowserActivity extends AppCompatActivity {

    private static final String TAG = "EhBrowserActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // 直接启动全新的YCWebView浏览器Activity
            Intent browserIntent = new Intent(this, YCWebViewActivity.class);
            browserIntent.setAction(Intent.ACTION_VIEW);

            // 如果有URL数据，传递给浏览器
            Intent originalIntent = getIntent();
            if (originalIntent != null && originalIntent.getData() != null) {
                browserIntent.setData(originalIntent.getData());
            }

            // 传递原始intent的所有额外参数
            if (originalIntent != null && originalIntent.getExtras() != null) {
                browserIntent.putExtras(originalIntent.getExtras());
            }

            startActivity(browserIntent);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error launching browser", e);
            Toast.makeText(this, "启动浏览器失败", Toast.LENGTH_SHORT).show();
        }

        finish(); // 立即关闭这个Activity
    }

}
