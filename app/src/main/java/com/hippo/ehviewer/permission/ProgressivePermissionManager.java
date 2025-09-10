package com.hippo.ehviewer.permission;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 渐进式权限管理器
 * 实现智能权限引导和重试机制，提升用户体验
 * 
 * 核心特性：
 * 1. 渐进式引导 - 在用户实际需要功能时才请求权限
 * 2. 智能重试机制 - 支持次日再试，避免过度打扰
 * 3. 状态栏显示 - 权限状态可视化
 */
public class ProgressivePermissionManager {
    private static final String TAG = "ProgressivePermissionManager";
    
    // SharedPreferences键名
    private static final String PREF_NAME = "progressive_permissions";
    private static final String PREF_LOCATION_DENIED_TIME = "location_denied_time";
    private static final String PREF_LOCATION_DENIED_COUNT = "location_denied_count";
    private static final String PREF_STORAGE_DENIED_TIME = "storage_denied_time";
    private static final String PREF_STORAGE_DENIED_COUNT = "storage_denied_count";
    
    // 智能重试配置
    private static final long RETRY_DELAY_ONE_DAY = 24 * 60 * 60 * 1000L; // 1天
    private static final long RETRY_DELAY_THREE_DAYS = 3 * 24 * 60 * 60 * 1000L; // 3天
    private static final int MAX_RETRY_COUNT = 3; // 最多重试3次
    
    private static ProgressivePermissionManager sInstance;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    
    // 权限请求回调接口
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
        void onPermissionBlocked(); // 用户选择了"不再提示"
    }
    
    private ProgressivePermissionManager(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized ProgressivePermissionManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ProgressivePermissionManager(context);
        }
        return sInstance;
    }
    
    /**
     * 智能请求地理位置权限
     * 根据使用场景和用户历史行为决定是否请求权限
     */
    public void requestLocationPermissionSmart(Activity activity, String reason, PermissionCallback callback) {
        Log.d(TAG, "Smart location permission request for reason: " + reason);
        
        // 检查权限是否已授予
        if (isLocationPermissionGranted()) {
            Log.d(TAG, "Location permission already granted");
            callback.onPermissionGranted();
            return;
        }
        
        // 检查是否应该进行智能重试
        if (!shouldRetryLocationPermission()) {
            Log.d(TAG, "Location permission retry blocked by smart policy");
            callback.onPermissionBlocked();
            return;
        }
        
        // 显示权限引导对话框
        showPermissionRationale(activity, 
            "获取位置权限", 
            "为了" + reason + "，需要获取您的位置信息。这将帮助提供更好的服务。", 
            () -> {
                // 用户同意，请求权限
                ActivityCompat.requestPermissions(activity,
                    new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_CODE_LOCATION);
            },
            () -> {
                // 用户拒绝
                recordPermissionDenied(PREF_LOCATION_DENIED_TIME, PREF_LOCATION_DENIED_COUNT);
                callback.onPermissionDenied();
            });
    }
    
    /**
     * 智能请求存储权限
     */
    public void requestStoragePermissionSmart(Activity activity, String reason, PermissionCallback callback) {
        Log.d(TAG, "Smart storage permission request for reason: " + reason);
        
        // 检查权限是否已授予
        if (isStoragePermissionGranted()) {
            Log.d(TAG, "Storage permission already granted");
            callback.onPermissionGranted();
            return;
        }
        
        // 检查是否应该进行智能重试
        if (!shouldRetryStoragePermission()) {
            Log.d(TAG, "Storage permission retry blocked by smart policy");
            callback.onPermissionBlocked();
            return;
        }
        
        // 显示权限引导对话框
        showPermissionRationale(activity,
            "获取存储权限",
            "为了" + reason + "，需要访问您的存储空间。这将用于保存和管理文件。",
            () -> {
                // 用户同意，请求权限
                ActivityCompat.requestPermissions(activity,
                    new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE_STORAGE);
            },
            () -> {
                // 用户拒绝
                recordPermissionDenied(PREF_STORAGE_DENIED_TIME, PREF_STORAGE_DENIED_COUNT);
                callback.onPermissionDenied();
            });
    }
    
    /**
     * 检查是否应该重试地理位置权限请求
     */
    private boolean shouldRetryLocationPermission() {
        return shouldRetryPermission(PREF_LOCATION_DENIED_TIME, PREF_LOCATION_DENIED_COUNT);
    }
    
    /**
     * 检查是否应该重试存储权限请求
     */
    private boolean shouldRetryStoragePermission() {
        return shouldRetryPermission(PREF_STORAGE_DENIED_TIME, PREF_STORAGE_DENIED_COUNT);
    }
    
    /**
     * 智能重试算法
     * 实现基于时间衰减的重试机制
     */
    private boolean shouldRetryPermission(String timeKey, String countKey) {
        long lastDeniedTime = mPrefs.getLong(timeKey, 0);
        int deniedCount = mPrefs.getInt(countKey, 0);
        
        // 如果从未被拒绝，可以请求
        if (lastDeniedTime == 0) {
            return true;
        }
        
        // 如果超过最大重试次数，不再请求
        if (deniedCount >= MAX_RETRY_COUNT) {
            Log.d(TAG, "Permission retry blocked: exceeded max count " + deniedCount);
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastDeniedTime;
        
        // 根据拒绝次数确定重试延迟
        long retryDelay;
        if (deniedCount == 1) {
            retryDelay = RETRY_DELAY_ONE_DAY; // 第一次拒绝后1天
        } else {
            retryDelay = RETRY_DELAY_THREE_DAYS; // 第二次及以后拒绝后3天
        }
        
        boolean canRetry = timeDiff >= retryDelay;
        Log.d(TAG, "Permission retry check: denied " + deniedCount + " times, " +
              "last denied " + (timeDiff / (60 * 60 * 1000)) + " hours ago, " +
              "can retry: " + canRetry);
        
        return canRetry;
    }
    
    /**
     * 记录权限被拒绝
     */
    private void recordPermissionDenied(String timeKey, String countKey) {
        long currentTime = System.currentTimeMillis();
        int currentCount = mPrefs.getInt(countKey, 0) + 1;
        
        mPrefs.edit()
            .putLong(timeKey, currentTime)
            .putInt(countKey, currentCount)
            .apply();
            
        Log.d(TAG, "Permission denied recorded: count = " + currentCount);
    }
    
    /**
     * 显示权限说明对话框
     */
    private void showPermissionRationale(Activity activity, String title, String message, 
                                       Runnable onPositive, Runnable onNegative) {
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(activity);
        
        builder.setTitle(title)
               .setMessage(message)
               .setPositiveButton("允许", (dialog, which) -> {
                   dialog.dismiss();
                   onPositive.run();
               })
               .setNegativeButton("暂不", (dialog, which) -> {
                   dialog.dismiss();
                   onNegative.run();
               })
               .setCancelable(false)
               .show();
    }
    
    /**
     * 检查地理位置权限状态
     */
    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) 
               == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) 
               == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 检查存储权限状态
     */
    private boolean isStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) 
               == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
               == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 获取权限状态描述
     */
    public String getPermissionStatusDescription() {
        StringBuilder status = new StringBuilder();
        
        if (isLocationPermissionGranted()) {
            status.append("📍 位置权限已授予 ");
        } else {
            status.append("❌ 位置权限未授予 ");
        }
        
        if (isStoragePermissionGranted()) {
            status.append("📁 存储权限已授予 ");
        } else {
            status.append("❌ 存储权限未授予 ");
        }
        
        return status.toString();
    }
    
    /**
     * 清理权限拒绝记录（用于测试或重置）
     */
    public void clearPermissionHistory() {
        mPrefs.edit().clear().apply();
        Log.d(TAG, "Permission history cleared");
    }
    
    // 权限请求码
    public static final int REQUEST_CODE_LOCATION = 1001;
    public static final int REQUEST_CODE_STORAGE = 1002;
}