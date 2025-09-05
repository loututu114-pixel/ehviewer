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

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

/**
 * 安全的窗口管理器
 * 防止CrossDeviceService权限错误
 */
public class SafeWindowManager {

    private static final String TAG = "SafeWindowManager";

    /**
     * 安全地设置窗口标志
     */
    public static void safeSetFlags(Activity activity, int flags, int mask) {
        if (activity == null) {
            Log.w(TAG, "Activity is null, cannot set window flags");
            return;
        }

        try {
            Window window = activity.getWindow();
            if (window != null) {
                window.setFlags(flags, mask);
                Log.d(TAG, "Window flags set successfully: " + flags);
            } else {
                Log.w(TAG, "Window is null, cannot set flags");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error setting window flags", e);
            // 不抛出异常，保持应用稳定
        }
    }

    /**
     * 安全地清除窗口标志
     */
    public static void safeClearFlags(Activity activity, int flags) {
        if (activity == null) {
            Log.w(TAG, "Activity is null, cannot clear window flags");
            return;
        }

        try {
            Window window = activity.getWindow();
            if (window != null) {
                window.clearFlags(flags);
                Log.d(TAG, "Window flags cleared successfully: " + flags);
            } else {
                Log.w(TAG, "Window is null, cannot clear flags");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error clearing window flags", e);
        }
    }

    /**
     * 安全地添加窗口标志
     */
    public static void safeAddFlags(Activity activity, int flags) {
        if (activity == null) {
            Log.w(TAG, "Activity is null, cannot add window flags");
            return;
        }

        try {
            Window window = activity.getWindow();
            if (window != null) {
                window.addFlags(flags);
                Log.d(TAG, "Window flags added successfully: " + flags);
            } else {
                Log.w(TAG, "Window is null, cannot add flags");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error adding window flags", e);
        }
    }

    /**
     * 安全地设置软键盘模式
     */
    public static void safeSetSoftInputMode(Activity activity, int mode) {
        if (activity == null) {
            Log.w(TAG, "Activity is null, cannot set soft input mode");
            return;
        }

        try {
            Window window = activity.getWindow();
            if (window != null) {
                window.setSoftInputMode(mode);
                Log.d(TAG, "Soft input mode set successfully: " + mode);
            } else {
                Log.w(TAG, "Window is null, cannot set soft input mode");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error setting soft input mode", e);
        }
    }

    /**
     * 安全地设置窗口属性
     */
    public static void safeSetAttributes(Activity activity, WindowManager.LayoutParams params) {
        if (activity == null || params == null) {
            Log.w(TAG, "Activity or params is null, cannot set window attributes");
            return;
        }

        try {
            Window window = activity.getWindow();
            if (window != null) {
                window.setAttributes(params);
                Log.d(TAG, "Window attributes set successfully");
            } else {
                Log.w(TAG, "Window is null, cannot set attributes");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error setting window attributes", e);
        }
    }

    /**
     * 安全地获取窗口属性
     */
    public static WindowManager.LayoutParams safeGetAttributes(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "Activity is null, cannot get window attributes");
            return null;
        }

        try {
            Window window = activity.getWindow();
            if (window != null) {
                return window.getAttributes();
            } else {
                Log.w(TAG, "Window is null, cannot get attributes");
                return null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting window attributes", e);
            return null;
        }
    }

    /**
     * 安全地设置系统UI可见性
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static void safeSetSystemUiVisibility(View view, int visibility) {
        if (view == null) {
            Log.w(TAG, "View is null, cannot set system UI visibility");
            return;
        }

        try {
            view.setSystemUiVisibility(visibility);
            Log.d(TAG, "System UI visibility set successfully: " + visibility);
        } catch (Exception e) {
            Log.w(TAG, "Error setting system UI visibility", e);
        }
    }

    /**
     * 检查窗口操作是否安全
     */
    public static boolean isWindowOperationSafe(Activity activity) {
        if (activity == null) {
            return false;
        }

        try {
            Window window = activity.getWindow();
            return window != null && !activity.isFinishing() && !activity.isDestroyed();
        } catch (Exception e) {
            Log.w(TAG, "Error checking window operation safety", e);
            return false;
        }
    }
}
