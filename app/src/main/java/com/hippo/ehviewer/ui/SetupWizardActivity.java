package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.adapter.SetupWizardAdapter;

/**
 * 首次启动设置向导
 * 引导用户完成必要的系统设置
 */
public class SetupWizardActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "setup_wizard";
    private static final String KEY_SETUP_COMPLETED = "setup_completed";
    private static final String KEY_SETUP_VERSION = "setup_version";
    private static final int CURRENT_SETUP_VERSION = 1;

    // UI组件
    private ViewPager mViewPager;
    private Button mPreviousButton;
    private Button mNextButton;
    private Button mSkipButton;
    private TextView mStepIndicator;

    // 数据
    private SetupWizardAdapter mAdapter;
    private int mCurrentStep = 0;
    private int mTotalSteps = 2; // 🎯 减少步骤数 - 从4步减少到2步

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 检查是否需要显示设置向导
        if (!shouldShowSetupWizard()) {
            proceedToMainApp();
            return;
        }

        setContentView(R.layout.activity_setup_wizard);
        initViews();
        setupWizard();
    }

    /**
     * 检查是否需要显示设置向导
     */
    private boolean shouldShowSetupWizard() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean setupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false);
        int setupVersion = prefs.getInt(KEY_SETUP_VERSION, 0);

        // 如果设置已完成且版本匹配，则不需要显示向导
        return !setupCompleted || setupVersion < CURRENT_SETUP_VERSION;
    }

    private void initViews() {
        mViewPager = findViewById(R.id.setup_viewpager);
        mPreviousButton = findViewById(R.id.previous_button);
        mNextButton = findViewById(R.id.next_button);
        mSkipButton = findViewById(R.id.skip_button);
        mStepIndicator = findViewById(R.id.step_indicator);

        mPreviousButton.setOnClickListener(v -> previousStep());
        mNextButton.setOnClickListener(v -> nextStep());
        mSkipButton.setOnClickListener(v -> skipSetup());
    }

    private void setupWizard() {
        mAdapter = new SetupWizardAdapter(this);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                mCurrentStep = position;
                updateUI();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        updateUI();
    }

    private void updateUI() {
        // 更新步骤指示器
        mStepIndicator.setText(String.format("%d / %d", mCurrentStep + 1, mTotalSteps));

        // 更新按钮状态
        mPreviousButton.setVisibility(mCurrentStep == 0 ? View.INVISIBLE : View.VISIBLE);
        
        if (mCurrentStep == mTotalSteps - 1) {
            mNextButton.setText("完成");
        } else {
            mNextButton.setText("下一步");
        }

        // 跳过按钮只在前几步显示
        mSkipButton.setVisibility(mCurrentStep < mTotalSteps - 1 ? View.VISIBLE : View.GONE);
    }

    private void previousStep() {
        if (mCurrentStep > 0) {
            mViewPager.setCurrentItem(mCurrentStep - 1);
        }
    }

    private void nextStep() {
        if (mCurrentStep < mTotalSteps - 1) {
            mViewPager.setCurrentItem(mCurrentStep + 1);
        } else {
            completeSetup();
        }
    }

    private void skipSetup() {
        completeSetup();
    }

    private void completeSetup() {
        // 显示完成设置的提示
        if (mNextButton != null) {
            mNextButton.setText("正在进入...");
            mNextButton.setEnabled(false);
        }
        
        // 保存设置完成状态
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .putInt(KEY_SETUP_VERSION, CURRENT_SETUP_VERSION)
            .apply();

        // 短暂延迟后进入主应用，给用户反馈
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            proceedToMainApp();
        }, 500);
    }

    private void proceedToMainApp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 检查是否应该显示设置向导（静态方法）
     */
    public static boolean shouldShowSetupWizard(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean setupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false);
        int setupVersion = prefs.getInt(KEY_SETUP_VERSION, 0);
        return !setupCompleted || setupVersion < CURRENT_SETUP_VERSION;
    }

    /**
     * 重置设置向导状态（用于测试）
     */
    public static void resetSetupWizard(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    @Override
    public void onBackPressed() {
        if (mCurrentStep > 0) {
            previousStep();
        } else {
            super.onBackPressed();
        }
    }
}