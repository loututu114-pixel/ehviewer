package com.hippo.ehviewer.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.BrowserRegistrationManager;
import com.hippo.ehviewer.util.DefaultBrowserHelper;

/**
 * 浏览器设置活动
 * 提供浏览器注册管理和默认浏览器设置功能
 */
public class BrowserSettingsActivity extends AppCompatActivity {
    private static final String TAG = "BrowserSettingsActivity";

    // UI控件
    private TextView statusTextView;
    private Button checkStatusButton;
    private Button fixRegistrationButton;
    private Button setDefaultButton;
    private Button openSettingsButton;
    private Button testBrowserButton;

    // 管理器
    private BrowserRegistrationManager registrationManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_settings);

        // 初始化管理器
        registrationManager = new BrowserRegistrationManager(this);

        // 初始化UI
        initializeViews();

        // 设置事件监听器
        setupListeners();

        // 首次检查状态
        checkBrowserStatus();
    }

    /**
     * 初始化UI控件
     */
    private void initializeViews() {
        statusTextView = findViewById(R.id.status_text_view);
        checkStatusButton = findViewById(R.id.check_status_button);
        fixRegistrationButton = findViewById(R.id.fix_registration_button);
        setDefaultButton = findViewById(R.id.set_default_button);
        openSettingsButton = findViewById(R.id.open_settings_button);
        testBrowserButton = findViewById(R.id.test_browser_button);

        // 设置标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("浏览器设置");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        if (checkStatusButton != null) {
            checkStatusButton.setOnClickListener(v -> checkBrowserStatus());
        }

        if (fixRegistrationButton != null) {
            fixRegistrationButton.setOnClickListener(v -> fixBrowserRegistration());
        }

        if (setDefaultButton != null) {
            setDefaultButton.setOnClickListener(v -> setAsDefaultBrowser());
        }

        if (openSettingsButton != null) {
            openSettingsButton.setOnClickListener(v -> openBrowserSettings());
        }

        if (testBrowserButton != null) {
            testBrowserButton.setOnClickListener(v -> testBrowserFunction());
        }
    }

    /**
     * 检查浏览器状态
     */
    private void checkBrowserStatus() {
        try {
            BrowserRegistrationManager.BrowserRegistrationStatus status =
                registrationManager.getRegistrationStatus();

            String statusText = status.getStatusDescription();
            updateStatusText(statusText);

            // 根据状态启用/禁用按钮
            updateButtonStates(status);

            Toast.makeText(this, "状态检查完成", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking browser status", e);
            updateStatusText("检查状态时出错: " + e.getMessage());
        }
    }

    /**
     * 修复浏览器注册
     */
    private void fixBrowserRegistration() {
        try {
            boolean success = registrationManager.fixBrowserRegistration();

            if (success) {
                Toast.makeText(this, "浏览器注册修复成功", Toast.LENGTH_SHORT).show();
                // 重新检查状态
                checkBrowserStatus();
            } else {
                Toast.makeText(this, "浏览器注册修复失败", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error fixing browser registration", e);
            Toast.makeText(this, "修复过程中出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置为默认浏览器
     */
    private void setAsDefaultBrowser() {
        try {
            boolean success = DefaultBrowserHelper.trySetAsDefaultBrowser(this);

            if (success) {
                Toast.makeText(this, "正在请求设置为默认浏览器...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "无法设置默认浏览器", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error setting default browser", e);
            Toast.makeText(this, "设置默认浏览器时出错", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开浏览器设置
     */
    private void openBrowserSettings() {
        try {
            boolean success = DefaultBrowserHelper.openDefaultBrowserSettings(this);

            if (!success) {
                // 如果系统设置页面打不开，打开应用详情页面
                registrationManager.openAppDetailsForBrowserSetting();
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error opening browser settings", e);
            Toast.makeText(this, "无法打开设置页面", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 测试浏览器功能
     */
    private void testBrowserFunction() {
        try {
            Intent testIntent = registrationManager.createBrowserTestIntent();
            startActivity(testIntent);
            Toast.makeText(this, "正在测试浏览器功能...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error testing browser", e);
            Toast.makeText(this, "测试浏览器功能时出错", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新状态文本
     */
    private void updateStatusText(String text) {
        if (statusTextView != null) {
            statusTextView.setText(text);
        }
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates(BrowserRegistrationManager.BrowserRegistrationStatus status) {
        // 根据状态启用/禁用相关按钮
        if (fixRegistrationButton != null) {
            fixRegistrationButton.setEnabled(!status.isAllGood());
        }

        if (setDefaultButton != null) {
            setDefaultButton.setEnabled(status.canRequestRole || !status.isDefault);
        }

        if (openSettingsButton != null) {
            openSettingsButton.setEnabled(true);
        }

        if (testBrowserButton != null) {
            testBrowserButton.setEnabled(status.isVisible);
        }
    }

    /**
     * 页面重新显示时刷新状态
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 重新检查状态，因为用户可能从系统设置页面返回
        checkBrowserStatus();
    }

    /**
     * 处理返回按钮
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * 启动浏览器设置活动
     */
    public static void startBrowserSettings(Context context) {
        try {
            Intent intent = new Intent(context, BrowserSettingsActivity.class);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error starting browser settings activity", e);
        }
    }
}