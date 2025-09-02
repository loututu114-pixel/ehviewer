package com.hippo.ehviewer.permission;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;

/**
 * 权限说明对话框 - 当用户拒绝权限时显示详细说明
 */
public class PermissionRationaleActivity extends Activity {
    
    private TextView titleText;
    private TextView descriptionText;
    private TextView detailsText;
    private ImageView warningIcon;
    private Button settingsButton;
    private Button cancelButton;
    private View contentContainer;
    
    private String permissionGroup;
    private String displayName;
    private String description;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_rationale);
        
        // 获取参数
        permissionGroup = getIntent().getStringExtra("permission_group");
        displayName = getIntent().getStringExtra("display_name");
        description = getIntent().getStringExtra("description");
        
        initViews();
        setupContent();
        startEntranceAnimation();
        
        UserBehaviorAnalyzer.trackEvent("permission_rationale_shown", "group", permissionGroup);
    }
    
    private void initViews() {
        titleText = findViewById(R.id.rationale_title);
        descriptionText = findViewById(R.id.rationale_description);
        detailsText = findViewById(R.id.rationale_details);
        warningIcon = findViewById(R.id.warning_icon);
        settingsButton = findViewById(R.id.settings_button);
        cancelButton = findViewById(R.id.cancel_button);
        contentContainer = findViewById(R.id.rationale_container);
        
        // 设置点击监听
        settingsButton.setOnClickListener(v -> openSettings());
        cancelButton.setOnClickListener(v -> onCancelClicked());
    }
    
    private void setupContent() {
        titleText.setText("需要" + displayName + "权限");
        descriptionText.setText(description);
        
        // 根据权限类型设置详细说明
        String details = getDetailedExplanation(permissionGroup);
        detailsText.setText(details);
        
        // 开始警告图标的闪烁动画
        startWarningIconAnimation();
    }
    
    private String getDetailedExplanation(String group) {
        switch (group) {
            case "LOCATION":
                return "位置权限用于:\n" +
                       "• 获取当地天气信息\n" +
                       "• 提供基于位置的服务推荐\n" +
                       "• 优化网络连接质量\n\n" +
                       "我们不会追踪您的位置或分享给第三方";
                       
            case "WIFI_MANAGEMENT":
                return "WiFi管理权限用于:\n" +
                       "• 检测可用的WiFi网络\n" +
                       "• 提供智能连接建议\n" +
                       "• 优化网络切换体验\n\n" +
                       "我们只读取网络状态，不会修改您的网络设置";
                       
            case "BATTERY_OPTIMIZATION":
                return "电池优化权限用于:\n" +
                       "• 监控电池状态和温度\n" +
                       "• 提供省电模式建议\n" +
                       "• 防止应用被系统强制关闭\n\n" +
                       "这有助于保持小部件正常运行";
                       
            case "NOTIFICATION":
                return "通知权限用于:\n" +
                       "• 网络状态变化提醒\n" +
                       "• 重要系统消息通知\n" +
                       "• 电池状态异常警告\n\n" +
                       "您可以随时在设置中关闭特定类型的通知";
                       
            case "STORAGE":
                return "存储权限用于:\n" +
                       "• 保存浏览历史和书签\n" +
                       "• 缓存网页内容以提高加载速度\n" +
                       "• 下载文件到本地存储\n\n" +
                       "我们只访问应用相关的文件，不会扫描您的私人文件";
                       
            case "OVERLAY":
                return "悬浮窗权限用于:\n" +
                       "• 在其他应用上显示快捷操作\n" +
                       "• 提供便捷的网络切换提示\n" +
                       "• 显示重要的系统状态信息\n\n" +
                       "悬浮窗会谨慎使用，不会干扰您的正常使用";
                       
            default:
                return "此权限对于应用的正常运行是必需的，请在设置中允许此权限。";
        }
    }
    
    private void startWarningIconAnimation() {
        // 闪烁动画
        ValueAnimator blinkAnimator = ValueAnimator.ofFloat(1f, 0.3f, 1f);
        blinkAnimator.setDuration(1500);
        blinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        blinkAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        blinkAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            warningIcon.setAlpha(alpha);
        });
        
        blinkAnimator.start();
    }
    
    private void startEntranceAnimation() {
        // 初始状态
        contentContainer.setAlpha(0f);
        contentContainer.setScaleX(0.8f);
        contentContainer.setScaleY(0.8f);
        
        // 淡入并放大
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(contentContainer, "alpha", 0f, 1f);
        fadeIn.setDuration(300);
        
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(contentContainer, "scaleX", 0.8f, 1f);
        scaleUpX.setDuration(300);
        scaleUpX.setInterpolator(new AccelerateDecelerateInterpolator());
        
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(contentContainer, "scaleY", 0.8f, 1f);
        scaleUpY.setDuration(300);
        scaleUpY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        fadeIn.start();
        scaleUpX.start();
        scaleUpY.start();
    }
    
    private void openSettings() {
        UserBehaviorAnalyzer.trackEvent("permission_settings_clicked", "group", permissionGroup);
        
        try {
            Intent intent = new Intent();
            
            switch (permissionGroup) {
                case "BATTERY_OPTIMIZATION":
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    break;
                    
                case "OVERLAY":
                    intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    break;
                    
                default:
                    // 打开应用权限设置页面
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    break;
            }
            
            startActivity(intent);
            
            // 延迟关闭当前界面，给用户时间处理设置
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finish();
            }, 1000);
            
        } catch (Exception e) {
            // 如果无法打开设置，则打开应用信息页面
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            finish();
        }
    }
    
    private void onCancelClicked() {
        UserBehaviorAnalyzer.trackEvent("permission_rationale_cancelled", "group", permissionGroup);
        
        // 缩小并淡出动画
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(contentContainer, "scaleX", 1f, 0.8f);
        scaleDownX.setDuration(200);
        
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(contentContainer, "scaleY", 1f, 0.8f);
        scaleDownY.setDuration(200);
        
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(contentContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(200);
        
        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                finish();
            }
        });
        
        scaleDownX.start();
        scaleDownY.start();
        fadeOut.start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 当从设置返回时，检查权限状态
        PermissionGuideManager manager = new PermissionGuideManager(this);
        PermissionGuideManager.PermissionGroup group = PermissionGuideManager.PermissionGroup.valueOf(permissionGroup);
        
        boolean hasPermission = false;
        switch (group) {
            case LOCATION:
                hasPermission = manager.hasPermissions(group.permissions);
                break;
            case WIFI_MANAGEMENT:
                hasPermission = manager.hasPermissions(group.permissions);
                break;
            case BATTERY_OPTIMIZATION:
                hasPermission = manager.isBatteryOptimizationIgnored();
                break;
            case NOTIFICATION:
                hasPermission = manager.hasPermissions(group.permissions);
                break;
            case STORAGE:
                hasPermission = manager.hasPermissions(group.permissions);
                break;
            case OVERLAY:
                hasPermission = manager.canDrawOverlays();
                break;
        }
        
        if (hasPermission) {
            // 权限已授予，关闭界面
            UserBehaviorAnalyzer.trackEvent("permission_granted_from_settings", "group", permissionGroup);
            finish();
        }
    }
}