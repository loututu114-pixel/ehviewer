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

import com.hippo.ehviewer.client.PasswordManager;

/**
 * 密码操作对话框
 */
public class PasswordActionsDialog extends DialogFragment {

    private PasswordManager.PasswordEntry passwordEntry;

    public static PasswordActionsDialog newInstance(PasswordManager.PasswordEntry entry) {
        PasswordActionsDialog dialog = new PasswordActionsDialog();
        Bundle args = new Bundle();
        args.putString("domain", entry.domain);
        args.putString("username", entry.username);
        args.putString("password", entry.password);
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
            passwordEntry = new PasswordManager.PasswordEntry(domain, username, password);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            TextView textView = new TextView(requireContext());
            textView.setText("密码操作功能\n域名: " + passwordEntry.domain +
                           "\n用户名: " + passwordEntry.username +
                           "\n\n功能: 编辑、删除、复制等");
            textView.setPadding(32, 32, 32, 32);
            dialog.setContentView(textView);

            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setLayout(width, -2);
        }
    }
}
