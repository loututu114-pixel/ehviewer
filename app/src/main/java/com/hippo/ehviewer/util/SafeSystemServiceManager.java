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

package com.hippo.ehviewer.util;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.UserManager;
import android.util.Log;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;

/**
 * 安全的系统服务管理器
 * 防止系统服务调用导致的权限错误
 */
public class SafeSystemServiceManager {

    private static final String TAG = "SafeSystemService";

    private static SafeSystemServiceManager instance;
    private final Context context;

    private SafeSystemServiceManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized SafeSystemServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new SafeSystemServiceManager(context);
        }
        return instance;
    }

    /**
     * 安全地获取ActivityManager
     */
    @Nullable
    public ActivityManager getActivityManager() {
        try {
            return (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        } catch (Exception e) {
            Log.w(TAG, "Error getting ActivityManager", e);
            return null;
        }
    }

    /**
     * 安全地获取WindowManager
     */
    @Nullable
    public WindowManager getWindowManager() {
        try {
            return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        } catch (Exception e) {
            Log.w(TAG, "Error getting WindowManager", e);
            return null;
        }
    }

    /**
     * 安全地获取PowerManager
     */
    @Nullable
    public PowerManager getPowerManager() {
        try {
            return (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        } catch (Exception e) {
            Log.w(TAG, "Error getting PowerManager", e);
            return null;
        }
    }

    /**
     * 安全地获取InputMethodManager
     */
    @Nullable
    public InputMethodManager getInputMethodManager() {
        try {
            return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        } catch (Exception e) {
            Log.w(TAG, "Error getting InputMethodManager", e);
            return null;
        }
    }

    /**
     * 安全地获取DevicePolicyManager
     */
    @Nullable
    public DevicePolicyManager getDevicePolicyManager() {
        try {
            return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        } catch (Exception e) {
            Log.w(TAG, "Error getting DevicePolicyManager", e);
            return null;
        }
    }

    /**
     * 安全地获取UserManager
     */
    @Nullable
    public UserManager getUserManager() {
        try {
            return (UserManager) context.getSystemService(Context.USER_SERVICE);
        } catch (Exception e) {
            Log.w(TAG, "Error getting UserManager", e);
            return null;
        }
    }

    /**
     * 安全地执行ActivityManager操作
     */
    public void safeActivityManagerOperation(SafeServiceOperation<ActivityManager> operation) {
        ActivityManager manager = getActivityManager();
        if (manager != null) {
            try {
                operation.execute(manager);
            } catch (Exception e) {
                Log.w(TAG, "Error executing ActivityManager operation", e);
            }
        } else {
            Log.w(TAG, "ActivityManager is not available");
        }
    }

    /**
     * 安全地执行WindowManager操作
     */
    public void safeWindowManagerOperation(SafeServiceOperation<WindowManager> operation) {
        WindowManager manager = getWindowManager();
        if (manager != null) {
            try {
                operation.execute(manager);
            } catch (Exception e) {
                Log.w(TAG, "Error executing WindowManager operation", e);
            }
        } else {
            Log.w(TAG, "WindowManager is not available");
        }
    }

    /**
     * 安全地执行PowerManager操作
     */
    public void safePowerManagerOperation(SafeServiceOperation<PowerManager> operation) {
        PowerManager manager = getPowerManager();
        if (manager != null) {
            try {
                operation.execute(manager);
            } catch (Exception e) {
                Log.w(TAG, "Error executing PowerManager operation", e);
            }
        } else {
            Log.w(TAG, "PowerManager is not available");
        }
    }

    /**
     * 安全地执行InputMethodManager操作
     */
    public void safeInputMethodManagerOperation(SafeServiceOperation<InputMethodManager> operation) {
        InputMethodManager manager = getInputMethodManager();
        if (manager != null) {
            try {
                operation.execute(manager);
            } catch (Exception e) {
                Log.w(TAG, "Error executing InputMethodManager operation", e);
            }
        } else {
            Log.w(TAG, "InputMethodManager is not available");
        }
    }

    /**
     * 安全地执行DevicePolicyManager操作
     */
    public void safeDevicePolicyManagerOperation(SafeServiceOperation<DevicePolicyManager> operation) {
        DevicePolicyManager manager = getDevicePolicyManager();
        if (manager != null) {
            try {
                operation.execute(manager);
            } catch (Exception e) {
                Log.w(TAG, "Error executing DevicePolicyManager operation", e);
            }
        } else {
            Log.w(TAG, "DevicePolicyManager is not available");
        }
    }

    /**
     * 安全地执行UserManager操作
     */
    public void safeUserManagerOperation(SafeServiceOperation<UserManager> operation) {
        UserManager manager = getUserManager();
        if (manager != null) {
            try {
                operation.execute(manager);
            } catch (Exception e) {
                Log.w(TAG, "Error executing UserManager operation", e);
            }
        } else {
            Log.w(TAG, "UserManager is not available");
        }
    }

    /**
     * 系统服务操作接口
     */
    public interface SafeServiceOperation<T> {
        void execute(T service) throws Exception;
    }
}
