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
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.PasswordManager;

/**
 * 添加密码对话框
 */
public class AddPasswordDialog extends DialogFragment {

    private EditText etDomain;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnSave;
    private Button btnCancel;
    private Button btnGenerate;

    private PasswordManager passwordManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_password, container, false);

        passwordManager = PasswordManager.getInstance(requireContext());

        initViews(view);
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        etDomain = view.findViewById(R.id.et_domain);
        etUsername = view.findViewById(R.id.et_username);
        etPassword = view.findViewById(R.id.et_password);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnGenerate = view.findViewById(R.id.btn_generate);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> {
            String domain = etDomain.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (validateInput(domain, username, password)) {
                savePassword(domain, username, password);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnGenerate.setOnClickListener(v -> {
            PasswordGeneratorDialog dialog = new PasswordGeneratorDialog();
            dialog.setOnPasswordGeneratedListener(generatedPassword -> {
                etPassword.setText(generatedPassword);
            });
            dialog.show(getParentFragmentManager(), "password_generator");
        });
    }

    private boolean validateInput(String domain, String username, String password) {
        if (domain.isEmpty()) {
            Toast.makeText(requireContext(), "请输入域名", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "请输入密码", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void savePassword(String domain, String username, String password) {
        boolean success = passwordManager.savePassword(domain, username, password);
        if (success) {
            Toast.makeText(requireContext(), "密码保存成功", Toast.LENGTH_SHORT).show();
            dismiss();
        } else {
            Toast.makeText(requireContext(), "密码保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
