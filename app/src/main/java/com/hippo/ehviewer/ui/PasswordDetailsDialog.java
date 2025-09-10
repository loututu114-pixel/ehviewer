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

package com.hippo.ehviewer.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.PasswordManager;

/**
 * 密码详情对话框
 */
public class PasswordDetailsDialog extends DialogFragment {

    private static final String ARG_PASSWORD_ENTRY = "password_entry";

    private PasswordManager.PasswordEntry passwordEntry;

    public static PasswordDetailsDialog newInstance(PasswordManager.PasswordEntry entry) {
        PasswordDetailsDialog dialog = new PasswordDetailsDialog();
        Bundle args = new Bundle();
        // 这里需要序列化PasswordEntry，暂时用简单的方式
        args.putString("domain", entry.domain);
        args.putString("username", entry.username);
        args.putString("password", entry.password);
        args.putLong("createdTime", entry.createdTime);
        args.putLong("lastUsedTime", entry.lastUsedTime);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String domain = getArguments().getString("domain");
            String username = getArguments().getString("username");
            String password = getArguments().getString("password");
            long createdTime = getArguments().getLong("createdTime");
            long lastUsedTime = getArguments().getLong("lastUsedTime");

            passwordEntry = new PasswordManager.PasswordEntry(domain, username, password);
            passwordEntry.createdTime = createdTime;
            passwordEntry.lastUsedTime = lastUsedTime;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            // 显示密码详情的简单实现
            TextView textView = new TextView(requireContext());
            textView.setText("域名: " + passwordEntry.domain + "\n" +
                           "用户名: " + passwordEntry.username + "\n" +
                           "密码: " + passwordEntry.password);
            textView.setPadding(32, 32, 32, 32);
            dialog.setContentView(textView);

            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setLayout(width, -2); // WRAP_CONTENT for height
        }
    }
}
