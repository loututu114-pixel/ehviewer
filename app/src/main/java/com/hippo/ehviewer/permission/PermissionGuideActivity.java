package com.hippo.ehviewer.permission;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;

/**
 * 权限引导界面 - 包含精美的微交互动画
 */
public class PermissionGuideActivity extends Activity {
    
    private TextView titleText;
    private TextView descriptionText;
    private ImageView permissionIcon;
    private Button allowButton;
    private Button skipButton;
    private LinearLayout contentContainer;
    private View progressIndicator;
    
    private String permissionGroup;
    private String displayName;
    private String description;
    
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_guide);
        
        // 获取传递的参数
        permissionGroup = getIntent().getStringExtra("permission_group");
        displayName = getIntent().getStringExtra("display_name");
        description = getIntent().getStringExtra("description");
        
        initViews();
        setupContent();
        startEntranceAnimation();
        
        UserBehaviorAnalyzer.trackEvent("permission_guide_shown", "group", permissionGroup);
    }
    
    private void initViews() {
        titleText = findViewById(R.id.permission_title);
        descriptionText = findViewById(R.id.permission_description);
        permissionIcon = findViewById(R.id.permission_icon);
        allowButton = findViewById(R.id.allow_button);
        skipButton = findViewById(R.id.skip_button);
        contentContainer = findViewById(R.id.content_container);
        progressIndicator = findViewById(R.id.progress_indicator);
        
        // 设置点击监听
        allowButton.setOnClickListener(v -> onAllowClicked());
        skipButton.setOnClickListener(v -> onSkipClicked());
    }
    
    private void setupContent() {
        titleText.setText(displayName);
        descriptionText.setText(description);
        
        // 根据权限类型设置图标
        int iconRes = getIconForPermissionGroup(permissionGroup);
        permissionIcon.setImageResource(iconRes);
        
        // 设置按钮样式
        setupButtonStyles();
    }
    
    private int getIconForPermissionGroup(String group) {
        switch (group) {
            case "LOCATION": return R.drawable.ic_location;
            case "WIFI_MANAGEMENT": return R.drawable.ic_wifi;
            case "BATTERY_OPTIMIZATION": return R.drawable.ic_battery;
            case "NOTIFICATION": return R.drawable.ic_notification;
            case "STORAGE": return R.drawable.ic_storage;
            case "OVERLAY": return R.drawable.ic_overlay;
            default: return R.drawable.ic_permission_default;
        }
    }
    
    private void setupButtonStyles() {
        // 允许按钮 - 主要操作样式
        allowButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_primary_rounded));
        allowButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        
        // 跳过按钮 - 次要操作样式
        skipButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_secondary_rounded));
        skipButton.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        
        // 按钮点击效果
        setupButtonClickEffects();
    }
    
    private void setupButtonClickEffects() {
        setupButtonClickEffect(allowButton);
        setupButtonClickEffect(skipButton);
    }
    
    private void setupButtonClickEffect(Button button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // 按下时缩放效果
                    ObjectAnimator scaleDown = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.95f);
                    scaleDown.setDuration(100);
                    scaleDown.start();
                    
                    ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.95f);
                    scaleDownY.setDuration(100);
                    scaleDownY.start();
                    break;
                    
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // 释放时回弹效果
                    ObjectAnimator scaleUp = ObjectAnimator.ofFloat(v, "scaleX", 0.95f, 1f);
                    scaleUp.setDuration(200);
                    scaleUp.setInterpolator(new OvershootInterpolator(1.5f));
                    scaleUp.start();
                    
                    ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(v, "scaleY", 0.95f, 1f);
                    scaleUpY.setDuration(200);
                    scaleUpY.setInterpolator(new OvershootInterpolator(1.5f));
                    scaleUpY.start();
                    break;
            }
            return false;
        });
    }
    
    private void startEntranceAnimation() {
        // 初始状态：所有元素都隐藏
        contentContainer.setAlpha(0f);
        contentContainer.setTranslationY(100f);
        
        permissionIcon.setScaleX(0f);
        permissionIcon.setScaleY(0f);
        
        allowButton.setAlpha(0f);
        allowButton.setTranslationX(-50f);
        
        skipButton.setAlpha(0f);
        skipButton.setTranslationX(50f);
        
        // 1. 容器淡入并上移
        ObjectAnimator containerFadeIn = ObjectAnimator.ofFloat(contentContainer, "alpha", 0f, 1f);
        containerFadeIn.setDuration(400);
        containerFadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        
        ObjectAnimator containerSlideUp = ObjectAnimator.ofFloat(contentContainer, "translationY", 100f, 0f);
        containerSlideUp.setDuration(400);
        containerSlideUp.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // 2. 图标弹性缩放
        animationHandler.postDelayed(() -> {
            ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(permissionIcon, "scaleX", 0f, 1f);
            iconScaleX.setDuration(600);
            iconScaleX.setInterpolator(new BounceInterpolator());
            
            ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(permissionIcon, "scaleY", 0f, 1f);
            iconScaleY.setDuration(600);
            iconScaleY.setInterpolator(new BounceInterpolator());
            
            iconScaleX.start();
            iconScaleY.start();
            
            // 图标呼吸效果
            startIconPulseEffect();
            
        }, 200);
        
        // 3. 按钮从两侧滑入
        animationHandler.postDelayed(() -> {
            ObjectAnimator allowFadeIn = ObjectAnimator.ofFloat(allowButton, "alpha", 0f, 1f);
            allowFadeIn.setDuration(300);
            
            ObjectAnimator allowSlideIn = ObjectAnimator.ofFloat(allowButton, "translationX", -50f, 0f);
            allowSlideIn.setDuration(300);
            allowSlideIn.setInterpolator(new OvershootInterpolator(1.2f));
            
            ObjectAnimator skipFadeIn = ObjectAnimator.ofFloat(skipButton, "alpha", 0f, 1f);
            skipFadeIn.setDuration(300);
            
            ObjectAnimator skipSlideIn = ObjectAnimator.ofFloat(skipButton, "translationX", 50f, 0f);
            skipSlideIn.setDuration(300);
            skipSlideIn.setInterpolator(new OvershootInterpolator(1.2f));
            
            allowFadeIn.start();
            allowSlideIn.start();
            skipFadeIn.start();
            skipSlideIn.start();
            
        }, 500);
        
        containerFadeIn.start();
        containerSlideUp.start();
    }
    
    private void startIconPulseEffect() {
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        pulseAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            permissionIcon.setScaleX(scale);
            permissionIcon.setScaleY(scale);
        });
        
        pulseAnimator.start();
    }
    
    private void onAllowClicked() {
        UserBehaviorAnalyzer.trackEvent("permission_allow_clicked", "group", permissionGroup);
        
        // 开始成功动画
        startSuccessAnimation(() -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("permission_granted", true);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
    
    private void onSkipClicked() {
        UserBehaviorAnalyzer.trackEvent("permission_skip_clicked", "group", permissionGroup);
        
        // 开始跳过动画
        startSkipAnimation(() -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("permission_granted", false);
            setResult(RESULT_CANCELED, resultIntent);
            finish();
        });
    }
    
    private void startSuccessAnimation(Runnable onComplete) {
        // 按钮变绿并展开
        ObjectAnimator buttonExpand = ObjectAnimator.ofFloat(allowButton, "scaleX", 1f, 1.2f);
        buttonExpand.setDuration(200);
        
        // 图标变为对勾并旋转
        permissionIcon.setImageResource(R.drawable.ic_check_circle);
        ObjectAnimator iconRotate = ObjectAnimator.ofFloat(permissionIcon, "rotation", 0f, 360f);
        iconRotate.setDuration(400);
        iconRotate.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // 整个界面淡出
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(contentContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(300);
        fadeOut.setStartDelay(600);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        
        buttonExpand.start();
        iconRotate.start();
        fadeOut.start();
    }
    
    private void startSkipAnimation(Runnable onComplete) {
        // 内容向右滑出
        ObjectAnimator slideOut = ObjectAnimator.ofFloat(contentContainer, "translationX", 0f, 300f);
        slideOut.setDuration(300);
        slideOut.setInterpolator(new AccelerateDecelerateInterpolator());
        
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(contentContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(300);
        
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        
        slideOut.start();
        fadeOut.start();
    }
    
    @Override
    public void onBackPressed() {
        // 禁用返回键，引导用户必须做出选择
        // 可以添加提示动画
        shakeAnimation(contentContainer);
    }
    
    private void shakeAnimation(View view) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 0, -25, 25, -25, 25, 0);
        shake.setDuration(500);
        shake.start();
    }
}