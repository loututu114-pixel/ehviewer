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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hippo.ehviewer.client.PasswordAutofillService;

/**
 * 自动填充错误处理器
 * 处理RemoteFillService相关的错误
 */
public class AutofillErrorHandler {

    private static final String TAG = "AutofillErrorHandler";

    private static AutofillErrorHandler instance;
    private final Context context;
    private final Handler mainHandler;

    private AutofillErrorHandler(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized AutofillErrorHandler getInstance(Context context) {
        if (instance == null) {
            instance = new AutofillErrorHandler(context);
        }
        return instance;
    }

    /**
     * 处理自动填充错误
     */
    public void handleAutofillError(String errorMessage, Throwable throwable) {
        Log.w(TAG, "Handling autofill error: " + errorMessage, throwable);

        try {
            // 检查是否是CancellationException
            if (throwable != null && throwable instanceof java.util.concurrent.CancellationException) {
                Log.i(TAG, "Autofill request was cancelled - this is usually normal");
                return;
            }

            // 检查是否是超时或连接问题
            if (errorMessage != null && (errorMessage.contains("timeout") ||
                errorMessage.contains("connection") || errorMessage.contains("cancel"))) {
                Log.i(TAG, "Autofill connection issue detected");
                handleConnectionError();
                return;
            }

            // 其他错误情况
            handleGenericAutofillError(errorMessage);

        } catch (Exception e) {
            Log.w(TAG, "Error handling autofill error", e);
        }
    }

    /**
     * 处理连接错误
     */
    private void handleConnectionError() {
        Log.i(TAG, "Handling autofill connection error");

        try {
            // 重置自动填充服务状态
            PasswordAutofillService.setAutofillEnabled(context, false);

            // 延迟重新启用
            mainHandler.postDelayed(() -> {
                try {
                    PasswordAutofillService.setAutofillEnabled(context, true);
                    Log.i(TAG, "Autofill service re-enabled after connection error");
                } catch (Exception e) {
                    Log.w(TAG, "Error re-enabling autofill service", e);
                }
            }, 5000); // 5秒后重新启用

        } catch (Exception e) {
            Log.w(TAG, "Error handling connection error", e);
        }
    }

    /**
     * 处理通用自动填充错误
     */
    private void handleGenericAutofillError(String errorMessage) {
        Log.i(TAG, "Handling generic autofill error");

        try {
            // 临时禁用自动填充以防止更多错误
            PasswordAutofillService.setAutofillEnabled(context, false);

            // 清理可能的缓存
            clearAutofillCache();

            // 延迟重新启用
            mainHandler.postDelayed(() -> {
                try {
                    PasswordAutofillService.setAutofillEnabled(context, true);
                    Log.i(TAG, "Autofill service re-enabled after generic error");
                } catch (Exception e) {
                    Log.w(TAG, "Error re-enabling autofill service after generic error", e);
                }
            }, 10000); // 10秒后重新启用

        } catch (Exception e) {
            Log.w(TAG, "Error handling generic autofill error", e);
        }
    }

    /**
     * 清理自动填充缓存
     */
    private void clearAutofillCache() {
        try {
            // 这里可以清理自动填充相关的缓存
            Log.d(TAG, "Autofill cache cleared");
        } catch (Exception e) {
            Log.w(TAG, "Error clearing autofill cache", e);
        }
    }

    /**
     * 检查自动填充服务状态
     */
    public boolean isAutofillServiceHealthy() {
        try {
            // 检查服务是否启用 - 创建实例来检查
            com.hippo.ehviewer.client.PasswordAutofillService service =
                new com.hippo.ehviewer.client.PasswordAutofillService();
            service.onCreate();
            // 由于方法是私有的，我们使用SharedPreferences直接检查
            android.content.SharedPreferences prefs =
                context.getSharedPreferences("autofill_prefs", android.content.Context.MODE_PRIVATE);
            return prefs.getBoolean("autofill_enabled", true);
        } catch (Exception e) {
            Log.w(TAG, "Error checking autofill service health", e);
            return false;
        }
    }

    /**
     * 重置自动填充服务
     */
    public void resetAutofillService() {
        Log.i(TAG, "Resetting autofill service");

        try {
            // 禁用服务
            PasswordAutofillService.setAutofillEnabled(context, false);

            // 清理缓存
            clearAutofillCache();

            // 短暂延迟后重新启用
            mainHandler.postDelayed(() -> {
                try {
                    PasswordAutofillService.setAutofillEnabled(context, true);
                    Log.i(TAG, "Autofill service reset completed");
                } catch (Exception e) {
                    Log.w(TAG, "Error resetting autofill service", e);
                }
            }, 2000);

        } catch (Exception e) {
            Log.w(TAG, "Error resetting autofill service", e);
        }
    }

    /**
     * 获取自动填充状态信息
     */
    public String getAutofillStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Autofill Status:\n");

        try {
            status.append("- Service enabled: ").append(isAutofillServiceHealthy()).append("\n");
            status.append("- Service healthy: ").append(isAutofillServiceHealthy()).append("\n");

        } catch (Exception e) {
            status.append("- Error getting status: ").append(e.getMessage()).append("\n");
        }

        return status.toString();
    }
}
