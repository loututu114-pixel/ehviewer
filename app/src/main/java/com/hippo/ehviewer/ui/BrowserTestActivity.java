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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;

/**
 * 浏览器功能测试Activity
 * 用于测试EhViewer的浏览器相关功能
 */
public class BrowserTestActivity extends AppCompatActivity {

    private static final String TAG = "BrowserTestActivity";

    private TextView tvTestResults;
    private Button btnTestDefaultBrowser;
    private Button btnTestShortcut;
    private Button btnTestBrowserLaunch;
    private Button btnTestAppDominance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_test);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("浏览器功能测试");
        }

        initViews();
        setupListeners();
        runInitialTests();
    }

    private void initViews() {
        tvTestResults = findViewById(R.id.tv_test_results);
        btnTestDefaultBrowser = findViewById(R.id.btn_test_default_browser);
        btnTestShortcut = findViewById(R.id.btn_test_shortcut);
        btnTestBrowserLaunch = findViewById(R.id.btn_test_browser_launch);
        btnTestAppDominance = findViewById(R.id.btn_test_app_dominance);
    }

    private void setupListeners() {
        btnTestDefaultBrowser.setOnClickListener(v -> testDefaultBrowser());
        btnTestShortcut.setOnClickListener(v -> testShortcutCreation());
        btnTestBrowserLaunch.setOnClickListener(v -> testBrowserLaunch());
        btnTestAppDominance.setOnClickListener(v -> testAppDominance());
    }

    private void runInitialTests() {
        StringBuilder results = new StringBuilder();
        results.append("🔍 浏览器功能测试结果：\n\n");

        // 测试默认浏览器状态
        boolean isDefault = com.hippo.ehviewer.util.DefaultBrowserHelper.isDefaultBrowser(this);
        results.append("默认浏览器: ").append(isDefault ? "✅ 是" : "❌ 否").append("\n");

        // 测试浏览器信息
        String browserInfo = com.hippo.ehviewer.util.DefaultBrowserHelper.getDefaultBrowserInfo(this);
        results.append("当前浏览器: ").append(browserInfo != null ? browserInfo : "未知").append("\n");

        // 测试应用统治力
        String dominance = com.hippo.ehviewer.util.DefaultBrowserHelper.getAppDominanceStatus(this);
        results.append("\n🏆 应用统治力状态:\n").append(dominance);

        tvTestResults.setText(results.toString());
    }

    private void testDefaultBrowser() {
        try {
            boolean success = com.hippo.ehviewer.util.DefaultBrowserHelper.trySetAsDefaultBrowser(this);
            if (success) {
                Toast.makeText(this, "已打开默认浏览器设置页面", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无法打开设置页面，请手动设置", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error testing default browser", e);
            Toast.makeText(this, "测试失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void testShortcutCreation() {
        try {
            // 使用EhBrowserActivity来创建快捷方式
            com.hippo.ehviewer.ui.EhBrowserActivity.createDesktopShortcut(this);
            Toast.makeText(this, "快捷方式创建请求已发送", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error testing shortcut", e);
            Toast.makeText(this, "快捷方式创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void testBrowserLaunch() {
        try {
            Intent intent = new Intent(this, EhBrowserActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            startActivity(intent);
            Toast.makeText(this, "Eh浏览器启动中...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error testing browser launch", e);
            Toast.makeText(this, "浏览器启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void testAppDominance() {
        try {
            String dominance = com.hippo.ehviewer.util.DefaultBrowserHelper.getAppDominanceStatus(this);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("🏆 应用统治力状态")
                    .setMessage(dominance)
                    .setPositiveButton("确定", null)
                    .setNeutralButton("设为默认", (dialog, which) -> {
                        com.hippo.ehviewer.util.DefaultBrowserHelper.trySetAsDefaultBrowser(this);
                    })
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error testing app dominance", e);
            Toast.makeText(this, "测试失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新运行测试，检查状态变化
        runInitialTests();
    }
}
