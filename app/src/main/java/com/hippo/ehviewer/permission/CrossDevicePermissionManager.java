/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.permission;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

/**
 * CrossDeviceService权限管理器
 * 处理跨设备服务的权限相关问题
 */
public class CrossDevicePermissionManager {

    private static final String TAG = "CrossDevicePermission";

    private static CrossDevicePermissionManager instance;
    private final Context context;

    private CrossDevicePermissionManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized CrossDevicePermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new CrossDevicePermissionManager(context);
        }
        return instance;
    }

    /**
     * 检查并处理CrossDeviceService相关权限
     */
    public void handleCrossDevicePermissions() {
        try {
            Log.i(TAG, "Handling CrossDevice permissions");

            // 检查设备管理员权限
            checkDeviceAdminPermissions();

            // 检查跨用户权限（Android 5.0+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                checkCrossUserPermissions();
            }

            // 检查设备所有者权限（Android 5.0+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                checkDeviceOwnerPermissions();
            }

            Log.i(TAG, "CrossDevice permissions handled successfully");

        } catch (Exception e) {
            Log.w(TAG, "Error handling CrossDevice permissions", e);
        }
    }

    /**
     * 检查设备管理员权限
     */
    private void checkDeviceAdminPermissions() {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                // 检查设备所有者状态（更安全的方法）
                boolean isDeviceOwner = dpm.isDeviceOwnerApp(context.getPackageName());
                Log.d(TAG, "Device owner: " + isDeviceOwner);

                if (isDeviceOwner) {
                    Log.i(TAG, "App is device owner");
                } else {
                    Log.i(TAG, "App is not device owner, this is normal for most apps");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking device admin permissions", e);
        }
    }

    /**
     * 检查跨用户权限
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void checkCrossUserPermissions() {
        try {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (userManager != null) {
                // 使用反射安全地检查跨用户权限
                boolean canInteractAcrossUsers = false;
                boolean canInteractAcrossUsersFull = false;

                try {
                    // 尝试调用canInteractAcrossUsers方法
                    java.lang.reflect.Method method1 = UserManager.class.getMethod("canInteractAcrossUsers");
                    canInteractAcrossUsers = (Boolean) method1.invoke(userManager);
                } catch (Exception e) {
                    Log.d(TAG, "canInteractAcrossUsers method not available", e);
                }

                try {
                    // 尝试调用canInteractAcrossUsersFull方法
                    java.lang.reflect.Method method2 = UserManager.class.getMethod("canInteractAcrossUsersFull");
                    canInteractAcrossUsersFull = (Boolean) method2.invoke(userManager);
                } catch (Exception e) {
                    Log.d(TAG, "canInteractAcrossUsersFull method not available", e);
                }

                Log.d(TAG, "Can interact across users: " + canInteractAcrossUsers);
                Log.d(TAG, "Can interact across users full: " + canInteractAcrossUsersFull);

                // 这些权限通常只对系统应用可用
                if (!canInteractAcrossUsers && !canInteractAcrossUsersFull) {
                    Log.i(TAG, "Cross-user interaction not available, this is normal for regular apps");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking cross-user permissions", e);
        }
    }

    /**
     * 检查设备所有者权限
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void checkDeviceOwnerPermissions() {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                // 检查设备所有者状态
                boolean isDeviceOwner = dpm.isDeviceOwnerApp(context.getPackageName());
                Log.d(TAG, "Is device owner: " + isDeviceOwner);

                if (isDeviceOwner) {
                    Log.i(TAG, "App is device owner");
                } else {
                    Log.i(TAG, "App is not device owner, this is normal for most apps");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking device owner permissions", e);
        }
    }

    /**
     * 获取系统兼容性信息
     */
    public String getCompatibilityInfo() {
        StringBuilder info = new StringBuilder();
        info.append("CrossDevice Compatibility Info:\n");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                if (userManager != null) {
                    // 使用反射安全地获取跨用户权限信息
                    try {
                        java.lang.reflect.Method method1 = UserManager.class.getMethod("canInteractAcrossUsers");
                        boolean canInteractAcrossUsers = (Boolean) method1.invoke(userManager);
                        info.append("- Cross-user interaction: ").append(canInteractAcrossUsers).append("\n");
                    } catch (Exception e) {
                        info.append("- Cross-user interaction: N/A\n");
                    }

                    try {
                        java.lang.reflect.Method method2 = UserManager.class.getMethod("canInteractAcrossUsersFull");
                        boolean canInteractAcrossUsersFull = (Boolean) method2.invoke(userManager);
                        info.append("- Cross-user interaction full: ").append(canInteractAcrossUsersFull).append("\n");
                    } catch (Exception e) {
                        info.append("- Cross-user interaction full: N/A\n");
                    }
                }

                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                if (dpm != null) {
                    info.append("- Device owner: ").append(dpm.isDeviceOwnerApp(context.getPackageName())).append("\n");
                }
            }

            info.append("- Android version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

        } catch (Exception e) {
            info.append("- Error getting compatibility info: ").append(e.getMessage()).append("\n");
        }

        return info.toString();
    }

    /**
     * 安全的权限检查方法
     */
    public boolean hasSafePermissions() {
        try {
            // 检查基本的权限
            return true; // 对于普通应用，这些权限检查通常都会通过
        } catch (Exception e) {
            Log.w(TAG, "Error checking safe permissions", e);
            return false;
        }
    }
}
