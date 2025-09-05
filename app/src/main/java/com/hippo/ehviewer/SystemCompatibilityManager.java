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

package com.hippo.ehviewer;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 系统兼容性管理器
 * 处理各种Android系统兼容性问题
 */
public class SystemCompatibilityManager {

    private static final String TAG = "SystemCompatibility";

    private static SystemCompatibilityManager instance;

    private SystemCompatibilityManager() {}

    public static synchronized SystemCompatibilityManager getInstance() {
        if (instance == null) {
            instance = new SystemCompatibilityManager();
        }
        return instance;
    }

    /**
     * 处理运行时标志兼容性问题
     */
    public void handleRuntimeFlagsCompatibility() {
        try {
            // 检查并处理runtime_flags警告
            suppressRuntimeFlagsWarnings();

            // 检查ART虚拟机版本兼容性
            checkArtCompatibility();

            Log.i(TAG, "Runtime flags compatibility handled successfully");
        } catch (Exception e) {
            Log.w(TAG, "Error handling runtime flags compatibility", e);
        }
    }

    /**
     * 抑制运行时标志警告
     * 通过反射方式处理ART虚拟机的警告
     */
    private void suppressRuntimeFlagsWarnings() {
        try {
            // 尝试通过反射访问ART虚拟机设置
            Class<?> vmClass = Class.forName("dalvik.system.VMRuntime");
            Method getRuntime = vmClass.getMethod("getRuntime");
            Object runtime = getRuntime.invoke(null);

            // 尝试设置运行时标志以避免警告
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android P及以上版本的处理
                try {
                    Method setHiddenApiExemptions = vmClass.getMethod("setHiddenApiExemptions", String[].class);
                    setHiddenApiExemptions.invoke(runtime, (Object) new String[]{"L"});
                    Log.d(TAG, "Hidden API exemptions set successfully");
                } catch (Exception e) {
                    Log.d(TAG, "Hidden API exemptions not available or failed", e);
                }
            }

        } catch (Exception e) {
            Log.d(TAG, "VMRuntime access failed, this is normal on some devices", e);
        }
    }

    /**
     * 检查ART虚拟机兼容性
     */
    private void checkArtCompatibility() {
        try {
            String vmVersion = System.getProperty("java.vm.version");
            String vmName = System.getProperty("java.vm.name");

            Log.d(TAG, "VM Version: " + vmVersion + ", VM Name: " + vmName);

            // 检查是否为ART虚拟机
            if (vmName != null && vmName.contains("ART")) {
                Log.i(TAG, "Running on ART virtual machine");

                // 可以在这里添加ART特定的兼容性处理
                handleArtSpecificIssues();
            } else {
                Log.i(TAG, "Running on non-ART virtual machine: " + vmName);
            }

        } catch (Exception e) {
            Log.w(TAG, "Error checking ART compatibility", e);
        }
    }

    /**
     * 处理ART特定的问题
     */
    private void handleArtSpecificIssues() {
        // 处理已知的ART兼容性问题
        try {
            // 设置一些系统属性来避免警告
            System.setProperty("dalvik.vm.checkjni", "false");

            // 可以在这里添加更多ART特定的修复

        } catch (Exception e) {
            Log.w(TAG, "Error handling ART specific issues", e);
        }
    }

    /**
     * 检查系统架构兼容性
     */
    public void checkArchitectureCompatibility() {
        try {
            String arch = System.getProperty("os.arch");
            String abi = Build.CPU_ABI;
            String abi2 = Build.CPU_ABI2;

            Log.d(TAG, "System architecture: " + arch + ", ABI: " + abi + ", ABI2: " + abi2);

            // 检查64位支持
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String[] supportedAbis = Build.SUPPORTED_ABIS;
                boolean has64Bit = false;
                for (String supportedAbi : supportedAbis) {
                    if (supportedAbi.contains("64")) {
                        has64Bit = true;
                        break;
                    }
                }

                if (has64Bit) {
                    Log.i(TAG, "Device supports 64-bit architecture");
                } else {
                    Log.i(TAG, "Device is 32-bit only");
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Error checking architecture compatibility", e);
        }
    }

    /**
     * 初始化系统兼容性检查
     */
    public void initialize(Context context) {
        Log.i(TAG, "Initializing system compatibility manager");

        // 处理运行时标志兼容性
        handleRuntimeFlagsCompatibility();

        // 检查架构兼容性
        checkArchitectureCompatibility();

        // 可以在这里添加更多兼容性检查
    }
}
