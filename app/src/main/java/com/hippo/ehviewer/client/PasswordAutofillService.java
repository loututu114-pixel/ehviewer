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

package com.hippo.ehviewer.client;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EhViewer密码自动填充服务
 * 提供系统级的密码自动填充功能
 */
public class PasswordAutofillService extends AutofillService {

    private static final String TAG = "PasswordAutofillService";

    private PasswordManager mPasswordManager;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        mPasswordManager = PasswordManager.getInstance(this);
        mPrefs = getSharedPreferences("autofill_prefs", Context.MODE_PRIVATE);
        Log.d(TAG, "PasswordAutofillService created");
    }

    @Override
    public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellationSignal,
                             @NonNull FillCallback callback) {

        Log.d(TAG, "onFillRequest called");

        // 检查密码管理器是否已解锁
        if (!mPasswordManager.isUnlocked()) {
            Log.w(TAG, "Password manager is locked, cannot provide autofill");
            callback.onSuccess(null);
            return;
        }

        // 检查用户是否启用了自动填充
        if (!isAutofillEnabled()) {
            Log.d(TAG, "Autofill is disabled by user");
            callback.onSuccess(null);
            return;
        }

        AssistStructure structure = request.getFillContexts().get(request.getFillContexts().size() - 1)
                .getStructure();

        if (structure == null) {
            callback.onSuccess(null);
            return;
        }

        // 解析表单字段
        ParsedStructure parsedStructure = parseStructure(structure);
        if (parsedStructure == null || parsedStructure.usernameFields.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        // 构建填充响应
        FillResponse response = buildFillResponse(parsedStructure);
        callback.onSuccess(response);
    }

    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {
        Log.d(TAG, "onSaveRequest called");

        // 检查密码管理器是否已解锁
        if (!mPasswordManager.isUnlocked()) {
            Log.w(TAG, "Password manager is locked, cannot save password");
            callback.onSuccess();
            return;
        }

        AssistStructure structure = request.getFillContexts().get(request.getFillContexts().size() - 1)
                .getStructure();

        if (structure == null) {
            callback.onSuccess();
            return;
        }

        // 解析并保存密码
        boolean saved = savePasswordFromStructure(structure);
        if (saved) {
            callback.onSuccess();
            Log.d(TAG, "Password saved successfully");
        } else {
            callback.onFailure("Failed to save password");
            Log.w(TAG, "Failed to save password");
        }
    }

    /**
     * 解析AssistStructure以查找用户名和密码字段
     */
    private ParsedStructure parseStructure(AssistStructure structure) {
        ParsedStructure result = new ParsedStructure();
        result.domain = extractDomain(structure);

        int windowCount = structure.getWindowNodeCount();
        for (int i = 0; i < windowCount; i++) {
            AssistStructure.WindowNode window = structure.getWindowNodeAt(i);
            parseViewNode(window.getRootViewNode(), result);
        }

        return result.hasCredentials() ? result : null;
    }

    /**
     * 递归解析View节点
     */
    private void parseViewNode(AssistStructure.ViewNode node, ParsedStructure structure) {
        if (node == null) return;

        // 检查是否是输入字段
        if (node.getAutofillHints() != null || node.getHint() != null) {
            String[] hints = node.getAutofillHints();
            if (hints != null) {
                for (String hint : hints) {
                    if (isUsernameHint(hint)) {
                        structure.usernameFields.add(node);
                    } else if (isPasswordHint(hint)) {
                        structure.passwordFields.add(node);
                    }
                }
            }

            // 检查HTML输入类型
            int inputTypeInt = node.getInputType();
            String inputType = String.valueOf(inputTypeInt);
            if (inputType != null) {
                if (inputType.contains("1") || inputType.contains("33") || inputType.contains("32")) { // TYPE_TEXT_VARIATION_EMAIL, TYPE_TEXT, TYPE_TEXT_VARIATION_NORMAL
                    if (isLikelyUsernameField(node)) {
                        structure.usernameFields.add(node);
                    }
                } else if (inputType.contains("password")) {
                    structure.passwordFields.add(node);
                }
            }
        }

        // 递归处理子节点
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            parseViewNode(node.getChildAt(i), structure);
        }
    }

    /**
     * 检查是否是用户名相关的提示
     */
    private boolean isUsernameHint(String hint) {
        return hint.contains("username") || hint.contains("email") || hint.contains("account") ||
               hint.contains("login") || hint.contains("user");
    }

    /**
     * 检查是否是密码相关的提示
     */
    private boolean isPasswordHint(String hint) {
        return hint.contains("password") || hint.contains("pass") || hint.contains("pwd");
    }

    /**
     * 判断是否可能是用户名字段
     */
    private boolean isLikelyUsernameField(AssistStructure.ViewNode node) {
        String hint = node.getHint();
        String id = node.getIdEntry();

        if (hint != null) {
            String lowerHint = hint.toLowerCase();
            if (lowerHint.contains("user") || lowerHint.contains("email") ||
                lowerHint.contains("account") || lowerHint.contains("login")) {
                return true;
            }
        }

        if (id != null) {
            String lowerId = id.toLowerCase();
            if (lowerId.contains("user") || lowerId.contains("email") ||
                lowerId.contains("account") || lowerId.contains("login")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 提取域名信息
     */
    private String extractDomain(AssistStructure structure) {
        // 从WebView中提取域名
        int windowCount = structure.getWindowNodeCount();
        for (int i = 0; i < windowCount; i++) {
            AssistStructure.WindowNode window = structure.getWindowNodeAt(i);
            CharSequence titleCharSequence = window.getTitle();
            String title = titleCharSequence != null ? titleCharSequence.toString() : null;
            if (title != null && title.contains(".")) {
                // 尝试从标题中提取域名
                return extractDomainFromUrl(title);
            }
        }
        return null;
    }

    /**
     * 从URL中提取域名
     */
    private String extractDomainFromUrl(String url) {
        if (url == null) return null;

        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                java.net.URL parsedUrl = new java.net.URL(url);
                return parsedUrl.getHost();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse domain from URL: " + url, e);
        }
        return url;
    }

    /**
     * 构建填充响应
     */
    private FillResponse buildFillResponse(ParsedStructure structure) {
        FillResponse.Builder responseBuilder = new FillResponse.Builder();

        if (structure.domain != null) {
            List<String> usernames = mPasswordManager.getSuggestedUsernames(structure.domain);

            for (String username : usernames) {
                PasswordManager.PasswordEntry entry = mPasswordManager.getPassword(structure.domain, username);
                if (entry != null) {
                    Dataset.Builder datasetBuilder = new Dataset.Builder();

                    // 添加用户名字段
                    for (AssistStructure.ViewNode usernameField : structure.usernameFields) {
                        datasetBuilder.setValue(usernameField.getAutofillId(),
                                AutofillValue.forText(entry.username));
                    }

                    // 添加密码字段
                    for (AssistStructure.ViewNode passwordField : structure.passwordFields) {
                        datasetBuilder.setValue(passwordField.getAutofillId(),
                                AutofillValue.forText(entry.password));
                    }

                    // 设置显示文本
                    String displayText = entry.username + " (" + structure.domain + ")";
                    RemoteViews presentation = createPresentation(displayText);
                    try {
                        // 尝试使用setPresentation方法（可能在某些API版本中不存在）
                        java.lang.reflect.Method setPresentationMethod = datasetBuilder.getClass().getMethod("setPresentation", RemoteViews.class);
                        setPresentationMethod.invoke(datasetBuilder, presentation);
                    } catch (Exception e) {
                        // 如果setPresentation不可用，使用其他方式
                        android.util.Log.w("PasswordAutofillService", "setPresentation not available, skipping presentation setup", e);
                    }

                    responseBuilder.addDataset(datasetBuilder.build());
                }
            }
        }

        return responseBuilder.build();
    }

    /**
     * 创建数据集的显示视图
     */
    private RemoteViews createPresentation(String text) {
        RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
        presentation.setTextViewText(android.R.id.text1, text);
        return presentation;
    }

    /**
     * 从结构中保存密码
     */
    private boolean savePasswordFromStructure(AssistStructure structure) {
        ParsedStructure parsed = parseStructure(structure);
        if (parsed == null || parsed.usernameFields.isEmpty() || parsed.passwordFields.isEmpty()) {
            return false;
        }

        // 提取用户名和密码值
        String username = null;
        String password = null;

        // 从用户名字段获取值
        for (AssistStructure.ViewNode field : parsed.usernameFields) {
            AutofillValue value = field.getAutofillValue();
            if (value != null && value.isText()) {
                username = value.getTextValue().toString();
                break;
            }
        }

        // 从密码字段获取值
        for (AssistStructure.ViewNode field : parsed.passwordFields) {
            AutofillValue value = field.getAutofillValue();
            if (value != null && value.isText()) {
                password = value.getTextValue().toString();
                break;
            }
        }

        if (username != null && password != null && parsed.domain != null) {
            return mPasswordManager.savePassword(parsed.domain, username, password);
        }

        return false;
    }

    /**
     * 检查自动填充是否启用
     */
    private boolean isAutofillEnabled() {
        return mPrefs.getBoolean("autofill_enabled", true);
    }

    /**
     * 设置自动填充启用状态
     */
    public static void setAutofillEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences("autofill_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("autofill_enabled", enabled).apply();
    }

    /**
     * 解析结果类
     */
    private static class ParsedStructure {
        public String domain;
        public List<AssistStructure.ViewNode> usernameFields = new ArrayList<>();
        public List<AssistStructure.ViewNode> passwordFields = new ArrayList<>();

        public boolean hasCredentials() {
            return !usernameFields.isEmpty() && !passwordFields.isEmpty();
        }
    }
}
