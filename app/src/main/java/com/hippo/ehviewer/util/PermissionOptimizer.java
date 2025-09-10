package com.hippo.ehviewer.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限优化管理器 - 智能化权限申请和管理
 * 提供一站式权限申请、引导和优化
 */
public class PermissionOptimizer {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static PermissionOptimizer instance;
    private final Context context;
    
    // 核心权限组
    private static final String[] ESSENTIAL_PERMISSIONS = {
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };
    
    // 浏览器增强权限组
    private static final String[] BROWSER_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    // 系统优化权限组
    private static final String[] OPTIMIZATION_PERMISSIONS = {
        Manifest.permission.REQUEST_INSTALL_PACKAGES,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    };
    
    private PermissionOptimizer(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static PermissionOptimizer getInstance(Context context) {
        if (instance == null) {
            synchronized (PermissionOptimizer.class) {
                if (instance == null) {
                    instance = new PermissionOptimizer(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 智能请求所有必要权限
     */
    public void requestAllNecessaryPermissions(Activity activity) {
        List<String> missingPermissions = new ArrayList<>();
        
        // 检查基础权限
        for (String permission : ESSENTIAL_PERMISSIONS) {
            if (!hasPermission(permission)) {
                missingPermissions.add(permission);
            }
        }
        
        // 检查浏览器权限
        for (String permission : BROWSER_PERMISSIONS) {
            if (!hasPermission(permission)) {
                missingPermissions.add(permission);
            }
        }
        
        if (!missingPermissions.isEmpty()) {
            // 显示权限说明对话框
            showPermissionRationale(activity, missingPermissions);
        } else {
            // 请求系统级优化权限
            requestSystemOptimizations(activity);
        }
    }
    
    /**
     * 显示权限理由说明
     */
    private void showPermissionRationale(Activity activity, List<String> permissions) {
        new AlertDialog.Builder(activity)
            .setTitle("权限申请")
            .setMessage("为了提供完整的浏览器体验，我们需要以下权限：\n\n" +
                "• 存储权限：保存下载文件和缓存\n" +
                "• 相机权限：扫描二维码和拍照上传\n" +
                "• 位置权限：提供基于位置的服务\n" +
                "• 录音权限：语音搜索和视频录制\n\n" +
                "请授予这些权限以获得最佳体验。")
            .setPositiveButton("授予权限", (dialog, which) -> {
                ActivityCompat.requestPermissions(activity,
                    permissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            })
            .setNegativeButton("稍后", null)
            .show();
    }
    
    /**
     * 请求系统级优化
     */
    public void requestSystemOptimizations(Activity activity) {
        // 请求忽略电池优化
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestBatteryOptimizationExemption(activity);
        }
        
        // 请求悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestOverlayPermission(activity);
        }
        
        // 请求安装应用权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestInstallPermission(activity);
        }
        
        // 请求所有文件访问权限 (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestAllFilesAccess(activity);
        }
    }
    
    /**
     * 请求忽略电池优化
     */
    private void requestBatteryOptimizationExemption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            String packageName = context.getPackageName();
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                new AlertDialog.Builder(activity)
                    .setTitle("电池优化")
                    .setMessage("关闭电池优化可以让应用在后台持续运行，提供更好的浏览体验。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        activity.startActivity(intent);
                    })
                    .setNegativeButton("稍后", null)
                    .show();
            }
        }
    }
    
    /**
     * 请求悬浮窗权限
     */
    private void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                new AlertDialog.Builder(activity)
                    .setTitle("悬浮窗权限")
                    .setMessage("悬浮窗权限可以让您在使用其他应用时快速访问浏览器功能。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + context.getPackageName()));
                        activity.startActivity(intent);
                    })
                    .setNegativeButton("稍后", null)
                    .show();
            }
        }
    }
    
    /**
     * 请求安装应用权限
     */
    private void requestInstallPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                new AlertDialog.Builder(activity)
                    .setTitle("安装应用权限")
                    .setMessage("允许安装应用可以让您直接安装下载的APK文件。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + context.getPackageName()));
                        activity.startActivityForResult(intent, 1002);
                    })
                    .setNegativeButton("稍后", null)
                    .show();
            }
        }
    }
    
    /**
     * 请求所有文件访问权限 (Android 11+)
     */
    private void requestAllFilesAccess(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(activity)
                    .setTitle("文件访问权限")
                    .setMessage("完整的文件访问权限可以让您通过浏览器打开和管理所有类型的文件。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        activity.startActivity(intent);
                    })
                    .setNegativeButton("稍后", null)
                    .show();
            }
        }
    }
    
    /**
     * 检查是否有权限
     */
    public boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 检查是否有所有基础权限
     */
    public boolean hasAllEssentialPermissions() {
        for (String permission : ESSENTIAL_PERMISSIONS) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 获取权限状态报告
     */
    public String getPermissionStatusReport() {
        StringBuilder report = new StringBuilder();
        report.append("权限状态报告：\n\n");
        
        report.append("基础权限：\n");
        for (String permission : ESSENTIAL_PERMISSIONS) {
            report.append("• ").append(getPermissionName(permission))
                .append(": ").append(hasPermission(permission) ? "✓" : "✗").append("\n");
        }
        
        report.append("\n浏览器权限：\n");
        for (String permission : BROWSER_PERMISSIONS) {
            report.append("• ").append(getPermissionName(permission))
                .append(": ").append(hasPermission(permission) ? "✓" : "✗").append("\n");
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            report.append("\n系统优化：\n");
            report.append("• 电池优化豁免: ")
                .append(pm.isIgnoringBatteryOptimizations(context.getPackageName()) ? "✓" : "✗")
                .append("\n");
            report.append("• 悬浮窗权限: ")
                .append(Settings.canDrawOverlays(context) ? "✓" : "✗")
                .append("\n");
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            report.append("• 安装应用权限: ")
                .append(context.getPackageManager().canRequestPackageInstalls() ? "✓" : "✗")
                .append("\n");
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            report.append("• 所有文件访问: ")
                .append(Environment.isExternalStorageManager() ? "✓" : "✗")
                .append("\n");
        }
        
        return report.toString();
    }
    
    private String getPermissionName(String permission) {
        String name = permission.substring(permission.lastIndexOf('.') + 1);
        return name.replace('_', ' ').toLowerCase();
    }
}