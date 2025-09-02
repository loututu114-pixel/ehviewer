package com.hippo.ehviewer.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;
import com.hippo.ehviewer.permission.PermissionGuideManager;
import com.hippo.ehviewer.ui.MainActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户首次使用引导界面 - 介绍应用功能和引导权限设置
 */
public class OnboardingActivity extends Activity implements PermissionGuideManager.PermissionGuideListener {
    
    private static final String PREFS_NAME = "onboarding_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    
    private ViewPager viewPager;
    private LinearLayout dotsContainer;
    private Button nextButton;
    private Button skipButton;
    private TextView progressText;
    
    private List<OnboardingPage> pages;
    private int currentPage = 0;
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    private PermissionGuideManager permissionManager;
    
    public static class OnboardingPage {
        public final String title;
        public final String description;
        public final int iconRes;
        public final boolean isPermissionPage;
        public final String permissionGroup;
        
        public OnboardingPage(String title, String description, int iconRes) {
            this(title, description, iconRes, false, null);
        }
        
        public OnboardingPage(String title, String description, int iconRes, boolean isPermissionPage, String permissionGroup) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.isPermissionPage = isPermissionPage;
            this.permissionGroup = permissionGroup;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        
        // 检查是否已完成引导
        if (isOnboardingCompleted()) {
            startMainActivity();
            return;
        }
        
        initPages();
        initViews();
        setupViewPager();
        setupPermissionManager();
        startEntranceAnimation();
        
        UserBehaviorAnalyzer.trackEvent("onboarding_started");
    }
    
    private boolean isOnboardingCompleted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }
    
    private void initPages() {
        pages = new ArrayList<>();
        
        // 欢迎页
        pages.add(new OnboardingPage(
            "欢迎使用 EhViewer",
            "一个功能强大的浏览器\n让您的上网体验更加便捷",
            R.drawable.ic_welcome
        ));
        
        // 功能介绍页
        pages.add(new OnboardingPage(
            "桌面小部件",
            "添加时钟、天气、WiFi管理等小部件\n快速访问常用功能",
            R.drawable.ic_widget_demo
        ));
        
        pages.add(new OnboardingPage(
            "智能网络管理",
            "自动检测WiFi信号\n智能提醒网络切换",
            R.drawable.ic_network_smart
        ));
        
        pages.add(new OnboardingPage(
            "个性化推荐",
            "根据您的使用习惯\n提供个性化的网站推荐",
            R.drawable.ic_personalization
        ));
        
        // 权限说明页
        pages.add(new OnboardingPage(
            "权限设置",
            "为了提供最佳体验\n需要您授权一些必要权限",
            R.drawable.ic_permission_setup,
            true,
            null
        ));
    }
    
    private void initViews() {
        viewPager = findViewById(R.id.viewpager_onboarding);
        dotsContainer = findViewById(R.id.dots_container);
        nextButton = findViewById(R.id.next_button);
        skipButton = findViewById(R.id.skip_button);
        progressText = findViewById(R.id.progress_text);
        
        nextButton.setOnClickListener(v -> nextPage());
        skipButton.setOnClickListener(v -> skipOnboarding());
        
        setupButtonEffects();
    }
    
    private void setupButtonEffects() {
        setupButtonClickEffect(nextButton);
        setupButtonClickEffect(skipButton);
    }
    
    private void setupButtonClickEffect(Button button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.95f).setDuration(100).start();
                    ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
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
    
    private void setupViewPager() {
        OnboardingPagerAdapter adapter = new OnboardingPagerAdapter();
        viewPager.setAdapter(adapter);
        
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // 页面滚动时的动画效果
                updateProgressIndicator(position + positionOffset);
            }
            
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updateUI();
                animatePageTransition();
                
                UserBehaviorAnalyzer.trackEvent("onboarding_page_viewed", "page", String.valueOf(position));
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {}
        });
        
        setupDots();
    }
    
    private void setupDots() {
        dotsContainer.removeAllViews();
        
        for (int i = 0; i < pages.size(); i++) {
            ImageView dot = new ImageView(this);
            dot.setImageResource(R.drawable.dot_unselected);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            
            dotsContainer.addView(dot);
        }
        
        updateDots();
    }
    
    private void updateDots() {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            ImageView dot = (ImageView) dotsContainer.getChildAt(i);
            if (i == currentPage) {
                dot.setImageResource(R.drawable.dot_selected);
                // 选中点的弹跳动画
                ObjectAnimator bounce = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 1.3f, 1f);
                bounce.setDuration(300);
                bounce.setInterpolator(new BounceInterpolator());
                bounce.start();
                
                ObjectAnimator bounceY = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 1.3f, 1f);
                bounceY.setDuration(300);
                bounceY.setInterpolator(new BounceInterpolator());
                bounceY.start();
            } else {
                dot.setImageResource(R.drawable.dot_unselected);
            }
        }
    }
    
    private void updateProgressIndicator(float position) {
        int total = pages.size();
        int progress = (int) ((position + 1) / total * 100);
        progressText.setText(progress + "%");
    }
    
    private void updateUI() {
        OnboardingPage page = pages.get(currentPage);
        
        if (currentPage == pages.size() - 1) {
            nextButton.setText("开始使用");
        } else {
            nextButton.setText("下一步");
        }
        
        // 权限页面特殊处理
        if (page.isPermissionPage) {
            nextButton.setText("设置权限");
            skipButton.setText("稍后设置");
        }
        
        updateDots();
    }
    
    private void animatePageTransition() {
        // 页面切换时的动画效果
        ObjectAnimator buttonPulse = ObjectAnimator.ofFloat(nextButton, "alpha", 0.6f, 1f);
        buttonPulse.setDuration(300);
        buttonPulse.start();
    }
    
    private void nextPage() {
        OnboardingPage page = pages.get(currentPage);
        
        if (page.isPermissionPage) {
            // 开始权限引导
            startPermissionGuide();
        } else if (currentPage == pages.size() - 1) {
            // 完成引导
            completeOnboarding();
        } else {
            // 下一页
            viewPager.setCurrentItem(currentPage + 1, true);
        }
    }
    
    private void skipOnboarding() {
        UserBehaviorAnalyzer.trackEvent("onboarding_skipped", "page", String.valueOf(currentPage));
        completeOnboarding();
    }
    
    private void startPermissionGuide() {
        UserBehaviorAnalyzer.trackEvent("permission_guide_started_from_onboarding");
        
        if (permissionManager != null) {
            permissionManager.startPermissionGuide(this);
        } else {
            // 如果权限管理器未初始化，直接完成引导
            completeOnboarding();
        }
    }
    
    private void setupPermissionManager() {
        permissionManager = new PermissionGuideManager(this);
        permissionManager.setListener(this);
    }
    
    private void completeOnboarding() {
        UserBehaviorAnalyzer.trackEvent("onboarding_completed");
        
        // 保存完成状态
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply();
        
        // 完成动画后启动主界面
        startCompleteAnimation();
    }
    
    private void startCompleteAnimation() {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(findViewById(R.id.onboarding_container), "alpha", 1f, 0f);
        fadeOut.setDuration(500);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startMainActivity();
            }
        });
        fadeOut.start();
    }
    
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
    
    private void startEntranceAnimation() {
        View container = findViewById(R.id.onboarding_container);
        container.setAlpha(0f);
        container.setTranslationY(100f);
        
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(container, "alpha", 0f, 1f);
        fadeIn.setDuration(600);
        
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(container, "translationY", 100f, 0f);
        slideUp.setDuration(600);
        slideUp.setInterpolator(new AccelerateDecelerateInterpolator());
        
        fadeIn.start();
        slideUp.start();
    }
    
    // PermissionGuideListener 实现
    @Override
    public void onPermissionGuideStart(PermissionGuideManager.PermissionGroup group) {
        // 权限引导开始
    }
    
    @Override
    public void onPermissionGranted(PermissionGuideManager.PermissionGroup group) {
        // 权限已授予
    }
    
    @Override
    public void onPermissionDenied(PermissionGuideManager.PermissionGroup group, boolean shouldShowRationale) {
        // 权限被拒绝
    }
    
    @Override
    public void onAllPermissionsGranted() {
        // 所有权限已授予
        UserBehaviorAnalyzer.trackEvent("all_permissions_granted_onboarding");
    }
    
    @Override
    public void onGuideCompleted() {
        // 权限引导完成，继续引导流程
        completeOnboarding();
    }
    
    // ViewPager 适配器
    private class OnboardingPagerAdapter extends PagerAdapter {
        
        @Override
        public int getCount() {
            return pages.size();
        }
        
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
        
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = getLayoutInflater().inflate(R.layout.onboarding_page, container, false);
            
            ImageView icon = view.findViewById(R.id.onboarding_icon);
            TextView title = view.findViewById(R.id.onboarding_title);
            TextView description = view.findViewById(R.id.onboarding_description);
            
            OnboardingPage page = pages.get(position);
            icon.setImageResource(page.iconRes);
            title.setText(page.title);
            description.setText(page.description);
            
            // 图标动画
            startPageIconAnimation(icon, position);
            
            container.addView(view);
            return view;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
        
        private void startPageIconAnimation(ImageView icon, int position) {
            // 不同页面使用不同的动画效果
            switch (position) {
                case 0:
                    // 欢迎页 - 缩放脉冲
                    ValueAnimator pulseAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f);
                    pulseAnimator.setDuration(2000);
                    pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                    pulseAnimator.addUpdateListener(animation -> {
                        float scale = (float) animation.getAnimatedValue();
                        icon.setScaleX(scale);
                        icon.setScaleY(scale);
                    });
                    pulseAnimator.start();
                    break;
                    
                case 1:
                    // 小部件页 - 轻微摆动
                    ObjectAnimator wiggle = ObjectAnimator.ofFloat(icon, "rotation", -3f, 3f, -3f);
                    wiggle.setDuration(3000);
                    wiggle.setRepeatCount(ValueAnimator.INFINITE);
                    wiggle.start();
                    break;
                    
                case 2:
                    // 网络管理页 - 上下浮动
                    ObjectAnimator float_anim = ObjectAnimator.ofFloat(icon, "translationY", -10f, 10f, -10f);
                    float_anim.setDuration(2500);
                    float_anim.setRepeatCount(ValueAnimator.INFINITE);
                    float_anim.start();
                    break;
                    
                default:
                    // 默认脉冲动画
                    ValueAnimator defaultPulse = ValueAnimator.ofFloat(1f, 1.05f, 1f);
                    defaultPulse.setDuration(2000);
                    defaultPulse.setRepeatCount(ValueAnimator.INFINITE);
                    defaultPulse.addUpdateListener(animation -> {
                        float scale = (float) animation.getAnimatedValue();
                        icon.setScaleX(scale);
                        icon.setScaleY(scale);
                    });
                    defaultPulse.start();
                    break;
            }
        }
    }
}