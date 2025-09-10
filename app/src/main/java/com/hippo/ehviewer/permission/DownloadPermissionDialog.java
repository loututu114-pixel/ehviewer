/*
 * Copyright 2025 EhViewer Project
 */

package com.hippo.ehviewer.permission;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.DefaultBrowserHelper;

/**
 * 下载权限请求对话框 - 用户友好的权限请求界面
 * 
 * 设计原则：
 * 1. 清晰解释为什么需要权限
 * 2. 让用户理解权限的作用和必要性
 * 3. 提供跳过选项但说明限制
 * 4. 3天提醒一次的选项
 */
public class DownloadPermissionDialog {
    
    private static final String TAG = "DownloadPermissionDialog";
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 1001;
    
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
        void onPermissionSkipped();
    }
    
    /**
     * 显示下载权限请求对话框
     */
    public static void showDownloadPermissionDialog(Activity activity, PermissionCallback callback) {
        Log.d(TAG, "显示下载权限对话框");
        
        try {
            // 创建自定义布局
            LayoutInflater inflater = LayoutInflater.from(activity);
            View dialogView = inflater.inflate(R.layout.dialog_download_permission, null);
            
            // 获取控件
            TextView titleText = dialogView.findViewById(R.id.title_text);
            TextView messageText = dialogView.findViewById(R.id.message_text);
            TextView storageText = dialogView.findViewById(R.id.storage_permission_text);
            TextView browserText = dialogView.findViewById(R.id.browser_permission_text);
            CheckBox remindCheckbox = dialogView.findViewById(R.id.remind_checkbox);
            
            // 设置内容
            titleText.setText("完成设置以开始下载");
            messageText.setText("为了提供完整的下载体验，我们需要设置以下权限：");
            
            DeferredPermissionManager permissionManager = DeferredPermissionManager.getInstance(activity);
            
            // 检查存储权限状态
            boolean needStoragePermission = !hasStoragePermission(activity);
            if (needStoragePermission) {
                storageText.setText("📁 文件存储权限 - 用于保存下载的漫画到您指定的目录");
                storageText.setVisibility(View.VISIBLE);
            } else {
                storageText.setVisibility(View.GONE);
            }
            
            // 检查默认浏览器状态
            boolean needDefaultBrowser = !DefaultBrowserHelper.isDefaultBrowser(activity);
            if (needDefaultBrowser && permissionManager.shouldRemindDefaultBrowser(activity)) {
                browserText.setText("🌐 默认浏览器设置 - 让EhViewer处理网页链接，提升浏览体验");
                browserText.setVisibility(View.VISIBLE);
            } else {
                browserText.setVisibility(View.GONE);
            }
            
            // 如果都不需要，直接执行成功回调
            if (!needStoragePermission && !needDefaultBrowser) {
                Log.d(TAG, "所有权限都已具备，直接执行下载");
                callback.onPermissionGranted();
                return;
            }
            
            // 创建对话框
            AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("立即设置", (d, which) -> {
                    Log.d(TAG, "用户选择立即设置权限");
                    
                    // 记录用户已经看过这个对话框
                    if (needStoragePermission) {
                        permissionManager.markStoragePermissionRequested();
                    }
                    if (needDefaultBrowser) {
                        permissionManager.markDefaultBrowserRequested();
                    }
                    
                    // 开始权限设置流程
                    startPermissionSetup(activity, needStoragePermission, needDefaultBrowser, callback);
                })
                .setNegativeButton("稍后设置", (d, which) -> {
                    Log.d(TAG, "用户选择稍后设置");
                    
                    // 检查是否勾选了提醒选项
                    if (!remindCheckbox.isChecked()) {
                        // 如果不勾选，标记为已请求，避免再次提醒
                        if (needStoragePermission) {
                            permissionManager.markStoragePermissionRequested();
                        }
                        if (needDefaultBrowser) {
                            permissionManager.markDefaultBrowserRequested();
                        }
                    }
                    
                    callback.onPermissionSkipped();
                })
                .setNeutralButton("了解详情", (d, which) -> {
                    // 显示详细的权限说明
                    showDetailedPermissionExplanation(activity, callback);
                })
                .create();
                
            dialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "显示权限对话框失败", e);
            callback.onPermissionDenied();
        }
    }
    
    /**
     * 开始权限设置流程
     */
    private static void startPermissionSetup(Activity activity, boolean needStorage, boolean needBrowser, PermissionCallback callback) {
        Log.d(TAG, "开始权限设置流程 - 存储: " + needStorage + ", 浏览器: " + needBrowser);
        
        if (needStorage) {
            // 首先请求存储权限
            requestStoragePermission(activity, () -> {
                if (needBrowser) {
                    // 存储权限设置完成后，设置默认浏览器
                    setupDefaultBrowser(activity, callback);
                } else {
                    // 只需要存储权限，完成
                    callback.onPermissionGranted();
                }
            }, () -> {
                // 存储权限被拒绝
                callback.onPermissionDenied();
            });
        } else if (needBrowser) {
            // 只需要设置默认浏览器
            setupDefaultBrowser(activity, callback);
        }
    }
    
    /**
     * 请求存储权限
     */
    private static void requestStoragePermission(Activity activity, Runnable onSuccess, Runnable onFailure) {
        Log.d(TAG, "请求存储权限");
        
        try {
            // 显示提示对话框
            new AlertDialog.Builder(activity)
                .setTitle("存储权限设置")
                .setMessage("即将跳转到系统设置页面，请：\n\n1. 找到「EhViewer」应用\n2. 开启「所有文件访问权限」\n3. 返回应用继续下载")
                .setPositiveButton("去设置", (d, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
                        
                        // 延迟检查权限状态 (用户返回应用后)
                        activity.getWindow().getDecorView().postDelayed(() -> {
                            if (hasStoragePermission(activity)) {
                                Log.d(TAG, "存储权限设置成功");
                                onSuccess.run();
                            } else {
                                Log.d(TAG, "存储权限设置失败或用户取消");
                                onFailure.run();
                            }
                        }, 2000);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "跳转存储权限设置失败", e);
                        onFailure.run();
                    }
                })
                .setNegativeButton("取消", (d, which) -> onFailure.run())
                .show();
                
        } catch (Exception e) {
            Log.e(TAG, "显示存储权限对话框失败", e);
            onFailure.run();
        }
    }
    
    /**
     * 设置默认浏览器
     */
    private static void setupDefaultBrowser(Activity activity, PermissionCallback callback) {
        Log.d(TAG, "设置默认浏览器");
        
        try {
            // 使用现有的DefaultBrowserHelper
            DefaultBrowserHelper.checkAndForceDefaultBrowser(activity);
            
            // 延迟检查是否设置成功
            activity.getWindow().getDecorView().postDelayed(() -> {
                if (DefaultBrowserHelper.isDefaultBrowser(activity)) {
                    Log.d(TAG, "默认浏览器设置成功");
                    callback.onPermissionGranted();
                } else {
                    Log.d(TAG, "默认浏览器设置失败，但不影响下载功能");
                    callback.onPermissionGranted(); // 不影响下载，仍然认为成功
                }
            }, 1000);
            
        } catch (Exception e) {
            Log.e(TAG, "设置默认浏览器失败", e);
            callback.onPermissionGranted(); // 不影响下载，仍然认为成功
        }
    }
    
    /**
     * 显示详细权限说明
     */
    private static void showDetailedPermissionExplanation(Activity activity, PermissionCallback callback) {
        Log.d(TAG, "显示详细权限说明");
        
        String detailedMessage = 
            "🔒 关于权限的详细说明：\n\n" +
            "📁 文件存储权限：\n" +
            "• 用途：保存下载的漫画图片到您的设备\n" +
            "• 必要性：没有此权限无法保存文件\n" +
            "• 安全性：我们只访问您指定的下载目录\n\n" +
            
            "🌐 默认浏览器设置：\n" +
            "• 用途：处理网页链接，在应用内打开\n" +
            "• 必要性：提升浏览体验，可跳过\n" +
            "• 说明：您随时可以在系统设置中更改\n\n" +
            
            "✅ 我们的承诺：\n" +
            "• 仅在需要时请求权限\n" +
            "• 不会访问您的隐私文件\n" +
            "• 所有权限可随时撤销";
            
        new AlertDialog.Builder(activity)
            .setTitle("权限详细说明")
            .setMessage(detailedMessage)
            .setPositiveButton("我已了解", (d, which) -> {
                // 返回权限设置对话框
                showDownloadPermissionDialog(activity, callback);
            })
            .show();
    }
    
    /**
     * 检查是否有存储权限
     */
    private static boolean hasStoragePermission(Activity activity) {
        try {
            return android.os.Environment.isExternalStorageManager();
        } catch (Exception e) {
            Log.w(TAG, "检查存储权限失败", e);
            return false;
        }
    }
}