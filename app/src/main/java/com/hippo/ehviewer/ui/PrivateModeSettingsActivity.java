/*
 * Copyright 2024 EhViewer
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
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.DefaultBrowserHelper;

/**
 * 私密模式设置Activity
 */
public class PrivateModeSettingsActivity extends AppCompatActivity {

    private static final String TAG = "PrivateModeSettings";

    private SwitchCompat privateModeSwitch;
    private SwitchCompat biometricSwitch;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_mode_settings);
        
        setupToolbar();
        setupViews();
        loadSettings();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("🔐 私密模式设置");
        }
    }

    private void setupViews() {
        privateModeSwitch = findViewById(R.id.switch_private_mode);
        biometricSwitch = findViewById(R.id.switch_biometric);
        
        // 私密模式开关
        privateModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showEnablePrivateModeDialog();
            } else {
                showDisablePrivateModeDialog();
            }
        });

        // 生物识别开关
        biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DefaultBrowserHelper.setPrivateModeBiometricEnabled(this, isChecked);
            Toast.makeText(this, isChecked ? "已启用生物识别验证" : "已禁用生物识别验证", 
                          Toast.LENGTH_SHORT).show();
        });

        // 创建私密模式快捷方式按钮
        findViewById(R.id.btn_create_shortcut).setOnClickListener(v -> {
            DefaultBrowserHelper.createPrivateModeShortcut(this);
        });

        // 测试私密模式按钮
        findViewById(R.id.btn_test_private_mode).setOnClickListener(v -> {
            testPrivateMode();
        });
    }

    private void loadSettings() {
        privateModeSwitch.setChecked(DefaultBrowserHelper.isPrivateModeEnabled(this));
        biometricSwitch.setChecked(DefaultBrowserHelper.isPrivateModeBiometricEnabled(this));
        biometricSwitch.setEnabled(DefaultBrowserHelper.isPrivateModeEnabled(this));
    }

    private void showEnablePrivateModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.widget.EditText passwordInput = new android.widget.EditText(this);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
                                 android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("设置私密模式密码");

        builder.setTitle("🔐 启用私密模式")
                .setMessage("请设置私密模式密码:\n\n" +
                          "• 密码用于保护您的隐私\n" +
                          "• 启用后，私密浏览将进入完整应用\n" +
                          "• 普通浏览器模式保持简洁体验")
                .setView(passwordInput)
                .setPositiveButton("启用", (dialog, which) -> {
                    String password = passwordInput.getText().toString().trim();
                    if (password.length() < 4) {
                        Toast.makeText(this, "密码长度至少4位", Toast.LENGTH_SHORT).show();
                        privateModeSwitch.setChecked(false);
                        return;
                    }
                    
                    DefaultBrowserHelper.setPrivateModePassword(this, password);
                    DefaultBrowserHelper.setPrivateModeEnabled(this, true);
                    biometricSwitch.setEnabled(true);
                    Toast.makeText(this, "🎉 私密模式已启用！", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    privateModeSwitch.setChecked(false);
                })
                .setCancelable(false)
                .show();
    }

    private void showDisablePrivateModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ 禁用私密模式")
                .setMessage("确定要禁用私密模式吗？\n\n" +
                          "这将删除所有私密模式设置。")
                .setPositiveButton("确定", (dialog, which) -> {
                    DefaultBrowserHelper.setPrivateModeEnabled(this, false);
                    biometricSwitch.setEnabled(false);
                    biometricSwitch.setChecked(false);
                    Toast.makeText(this, "私密模式已禁用", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    privateModeSwitch.setChecked(true);
                })
                .show();
    }

    private void testPrivateMode() {
        if (!DefaultBrowserHelper.isPrivateModeEnabled(this)) {
            Toast.makeText(this, "请先启用私密模式", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, EhBrowserActivity.class);
        intent.putExtra("enter_private_mode", true);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}