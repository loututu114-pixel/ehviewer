package com.hippo.ehviewer.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;
import java.util.*;

/**
 * 权限引导管理器 - 提供渐进式权限请求和用户引导
 * 包含微交互动画和智能权限策略
 */
public class PermissionGuideManager {
    private static final String PREFS_NAME = "permission_guide_prefs";
    private static final String KEY_PERMISSION_REQUESTED_PREFIX = "permission_requested_";
    private static final String KEY_USER_GUIDE_SHOWN_PREFIX = "guide_shown_";
    private static final String KEY_PERMISSION_DENIED_COUNT_PREFIX = "denied_count_";
    
    // 权限请求代码
    public static final int REQUEST_LOCATION = 1001;
    public static final int REQUEST_WIFI_ACCESS = 1002;
    public static final int REQUEST_BATTERY_OPTIMIZATION = 1003;
    public static final int REQUEST_NOTIFICATION = 1004;
    public static final int REQUEST_STORAGE = 1005;
    public static final int REQUEST_OVERLAY = 1006;
    
    private Context context;
    private SharedPreferences prefs;
    private PermissionGuideListener listener;
    
    // 权限组定义
    public enum PermissionGroup {
        ESSENTIAL("核心功能", "让应用正常运行的基础权限", 
            new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE}),
        LOCATION("位置服务", "为您提供准确的天气信息", 
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}),
        WIFI_MANAGEMENT("WiFi管理", "帮您管理WiFi连接和网络切换", 
            new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE}),
        BATTERY_OPTIMIZATION("电池优化", "提供电池状态监控和优化建议", 
            new String[]{}),
        NOTIFICATION("通知推送", "及时提醒网络变化和系统状态", 
            new String[]{Manifest.permission.POST_NOTIFICATIONS}),
        STORAGE("存储访问", "保存您的浏览记录和下载文件", 
            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}),
        OVERLAY("悬浮窗口", "在其他应用上显示便捷操作", 
            new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW});
        
        public final String displayName;
        public final String description;
        public final String[] permissions;
        
        PermissionGroup(String displayName, String description, String[] permissions) {
            this.displayName = displayName;
            this.description = description;
            this.permissions = permissions;
        }
    }
    
    public interface PermissionGuideListener {
        void onPermissionGuideStart(PermissionGroup group);
        void onPermissionGranted(PermissionGroup group);
        void onPermissionDenied(PermissionGroup group, boolean shouldShowRationale);
        void onAllPermissionsGranted();
        void onGuideCompleted();
    }
    
    public PermissionGuideManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void setListener(PermissionGuideListener listener) {
        this.listener = listener;
    }
    
    /**
     * 开始渐进式权限引导流程
     */
    public void startPermissionGuide(Activity activity) {
        UserBehaviorAnalyzer.trackEvent("permission_guide_started");
        
        // 按优先级顺序请求权限
        List<PermissionGroup> requiredPermissions = getPendingPermissions();
        
        if (requiredPermissions.isEmpty()) {
            if (listener != null) {
                listener.onAllPermissionsGranted();
            }
            return;
        }
        
        // 开始第一个权限的引导
        startPermissionGuideForGroup(activity, requiredPermissions.get(0));
    }
    
    /**
     * 获取需要请求的权限组列表
     */
    private List<PermissionGroup> getPendingPermissions() {
        List<PermissionGroup> pending = new ArrayList<>();
        
        for (PermissionGroup group : PermissionGroup.values()) {
            if (shouldRequestPermissionGroup(group)) {
                pending.add(group);
            }
        }
        
        return pending;
    }
    
    /**
     * 检查是否需要请求某个权限组
     */
    private boolean shouldRequestPermissionGroup(PermissionGroup group) {
        switch (group) {
            case ESSENTIAL:
                return !hasPermissions(group.permissions);
            case LOCATION:
                return !hasPermissions(group.permissions) && !hasUserDeclinedPermanently(group);
            case WIFI_MANAGEMENT:
                return !hasPermissions(group.permissions);
            case BATTERY_OPTIMIZATION:
                return !isBatteryOptimizationIgnored() && !hasUserDeclinedPermanently(group);
            case NOTIFICATION:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                       !hasPermissions(group.permissions) && !hasUserDeclinedPermanently(group);
            case STORAGE:
                return !hasPermissions(group.permissions) && !hasUserDeclinedPermanently(group);
            case OVERLAY:
                return !canDrawOverlays() && !hasUserDeclinedPermanently(group);
            default:
                return false;
        }
    }
    
    /**
     * 开始特定权限组的引导
     */
    private void startPermissionGuideForGroup(Activity activity, PermissionGroup group) {
        if (listener != null) {
            listener.onPermissionGuideStart(group);
        }
        
        // 显示权限引导界面
        Intent intent = new Intent(context, PermissionGuideActivity.class);
        intent.putExtra("permission_group", group.name());
        intent.putExtra("display_name", group.displayName);
        intent.putExtra("description", group.description);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(intent, getRequestCodeForGroup(group));
    }
    
    /**
     * 检查是否拥有所有指定权限
     */
    public boolean hasPermissions(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查电池优化白名单状态
     */
    public boolean isBatteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                return pm.isIgnoringBatteryOptimizations(context.getPackageName());
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查悬浮窗权限
     */
    public boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
    
    /**
     * 处理权限请求结果
     */
    public void handlePermissionResult(Activity activity, PermissionGroup group, boolean granted) {
        String key = KEY_PERMISSION_REQUESTED_PREFIX + group.name();
        prefs.edit().putBoolean(key, true).apply();
        
        if (granted) {
            UserBehaviorAnalyzer.trackEvent("permission_granted", "group", group.name());
            if (listener != null) {
                listener.onPermissionGranted(group);
            }
            
            // 继续下一个权限的引导
            continuePermissionGuide(activity);
        } else {
            // 记录拒绝次数
            String countKey = KEY_PERMISSION_DENIED_COUNT_PREFIX + group.name();
            int deniedCount = prefs.getInt(countKey, 0) + 1;
            prefs.edit().putInt(countKey, deniedCount).apply();
            
            UserBehaviorAnalyzer.trackEvent("permission_denied", "group", group.name(), "count", String.valueOf(deniedCount));
            
            boolean shouldShowRationale = deniedCount < 3; // 最多显示3次说明
            if (listener != null) {
                listener.onPermissionDenied(group, shouldShowRationale);
            }
            
            if (shouldShowRationale) {
                // 显示权限重要性说明，然后重新请求
                showPermissionRationaleDialog(activity, group);
            } else {
                // 用户永久拒绝，跳过这个权限
                continuePermissionGuide(activity);
            }
        }
    }
    
    /**
     * 显示权限说明对话框
     */
    private void showPermissionRationaleDialog(Activity activity, PermissionGroup group) {
        Intent intent = new Intent(context, PermissionRationaleActivity.class);
        intent.putExtra("permission_group", group.name());
        intent.putExtra("display_name", group.displayName);
        intent.putExtra("description", group.description);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }
    
    /**
     * 继续权限引导流程
     */
    private void continuePermissionGuide(Activity activity) {
        List<PermissionGroup> pending = getPendingPermissions();
        
        if (pending.isEmpty()) {
            // 所有权限处理完成
            UserBehaviorAnalyzer.trackEvent("permission_guide_completed");
            if (listener != null) {
                listener.onAllPermissionsGranted();
                listener.onGuideCompleted();
            }
        } else {
            // 处理下一个权限
            startPermissionGuideForGroup(activity, pending.get(0));
        }
    }
    
    /**
     * 检查用户是否已永久拒绝某个权限组
     */
    private boolean hasUserDeclinedPermanently(PermissionGroup group) {
        String countKey = KEY_PERMISSION_DENIED_COUNT_PREFIX + group.name();
        return prefs.getInt(countKey, 0) >= 3;
    }
    
    /**
     * 获取权限组对应的请求代码
     */
    private int getRequestCodeForGroup(PermissionGroup group) {
        switch (group) {
            case LOCATION: return REQUEST_LOCATION;
            case WIFI_MANAGEMENT: return REQUEST_WIFI_ACCESS;
            case BATTERY_OPTIMIZATION: return REQUEST_BATTERY_OPTIMIZATION;
            case NOTIFICATION: return REQUEST_NOTIFICATION;
            case STORAGE: return REQUEST_STORAGE;
            case OVERLAY: return REQUEST_OVERLAY;
            default: return 1000;
        }
    }
    
    /**
     * 重置权限引导状态（用于测试）
     */
    public void resetPermissionGuide() {
        prefs.edit().clear().apply();
    }
    
    /**
     * 获取权限状态统计
     */
    public Map<String, Object> getPermissionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        for (PermissionGroup group : PermissionGroup.values()) {
            Map<String, Object> groupStats = new HashMap<>();
            groupStats.put("hasPermission", shouldRequestPermissionGroup(group));
            groupStats.put("deniedCount", prefs.getInt(KEY_PERMISSION_DENIED_COUNT_PREFIX + group.name(), 0));
            groupStats.put("wasRequested", prefs.getBoolean(KEY_PERMISSION_REQUESTED_PREFIX + group.name(), false));
            stats.put(group.name(), groupStats);
        }
        
        return stats;
    }
}