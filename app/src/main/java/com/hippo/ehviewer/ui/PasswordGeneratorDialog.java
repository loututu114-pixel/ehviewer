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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.PasswordManager;

/**
 * 密码生成器对话框
 */
public class PasswordGeneratorDialog extends DialogFragment {

    private TextView tvGeneratedPassword;
    private TextView tvPasswordLength;
    private SeekBar seekBarLength;
    private CheckBox cbUppercase;
    private CheckBox cbLowercase;
    private CheckBox cbNumbers;
    private CheckBox cbSymbols;
    private TextView tvStrength;
    private Button btnGenerate;
    private Button btnCopy;
    private Button btnUse;

    private PasswordManager passwordManager;
    private String generatedPassword = "";
    private OnPasswordGeneratedListener listener;

    public interface OnPasswordGeneratedListener {
        void onPasswordGenerated(String password);
    }

    public void setOnPasswordGeneratedListener(OnPasswordGeneratedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_password_generator, container, false);

        passwordManager = PasswordManager.getInstance(requireContext());

        initViews(view);
        setupListeners();
        generatePassword();

        return view;
    }

    private void initViews(View view) {
        tvGeneratedPassword = view.findViewById(R.id.tv_generated_password);
        tvPasswordLength = view.findViewById(R.id.tv_password_length);
        seekBarLength = view.findViewById(R.id.seekbar_length);
        cbUppercase = view.findViewById(R.id.cb_uppercase);
        cbLowercase = view.findViewById(R.id.cb_lowercase);
        cbNumbers = view.findViewById(R.id.cb_numbers);
        cbSymbols = view.findViewById(R.id.cb_symbols);
        tvStrength = view.findViewById(R.id.tv_strength);
        btnGenerate = view.findViewById(R.id.btn_generate);
        btnCopy = view.findViewById(R.id.btn_copy);
        btnUse = view.findViewById(R.id.btn_use);

        // 设置默认选中状态
        cbUppercase.setChecked(true);
        cbLowercase.setChecked(true);
        cbNumbers.setChecked(true);
        cbSymbols.setChecked(true);

        // 设置长度范围
        seekBarLength.setMin(8);
        seekBarLength.setMax(32);
        seekBarLength.setProgress(16);
        updateLengthDisplay(16);
    }

    private void setupListeners() {
        seekBarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateLengthDisplay(progress);
                if (fromUser) {
                    generatePassword();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        View.OnClickListener checkboxListener = v -> generatePassword();
        cbUppercase.setOnClickListener(checkboxListener);
        cbLowercase.setOnClickListener(checkboxListener);
        cbNumbers.setOnClickListener(checkboxListener);
        cbSymbols.setOnClickListener(checkboxListener);

        btnGenerate.setOnClickListener(v -> generatePassword());

        btnCopy.setOnClickListener(v -> {
            if (!generatedPassword.isEmpty()) {
                copyToClipboard(generatedPassword);
                Toast.makeText(requireContext(), "密码已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });

        btnUse.setOnClickListener(v -> {
            if (!generatedPassword.isEmpty() && listener != null) {
                listener.onPasswordGenerated(generatedPassword);
                dismiss();
            }
        });
    }

    private void updateLengthDisplay(int length) {
        tvPasswordLength.setText("长度: " + length);
    }

    private void generatePassword() {
        if (!isAtLeastOneTypeSelected()) {
            Toast.makeText(requireContext(), "请至少选择一种字符类型", Toast.LENGTH_SHORT).show();
            return;
        }

        int length = seekBarLength.getProgress();
        generatedPassword = passwordManager.generateStrongPassword(length);

        tvGeneratedPassword.setText(generatedPassword);

        // 检查密码强度
        PasswordManager.PasswordStrength strength = passwordManager.checkPasswordStrength(generatedPassword);
        tvStrength.setText("强度: " + strength.getDescription());
        tvStrength.setTextColor(getStrengthColor(strength));
    }

    private boolean isAtLeastOneTypeSelected() {
        return cbUppercase.isChecked() || cbLowercase.isChecked() ||
               cbNumbers.isChecked() || cbSymbols.isChecked();
    }

    private int getStrengthColor(PasswordManager.PasswordStrength strength) {
        switch (strength) {
            case VERY_WEAK:
                return requireContext().getColor(android.R.color.holo_red_dark);
            case WEAK:
                return requireContext().getColor(android.R.color.holo_orange_dark);
            case MEDIUM:
                return requireContext().getColor(android.R.color.holo_blue_dark);
            case STRONG:
                return requireContext().getColor(android.R.color.holo_green_dark);
            case VERY_STRONG:
                return requireContext().getColor(android.R.color.holo_green_light);
            default:
                return requireContext().getColor(android.R.color.darker_gray);
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("generated_password", text);
            clipboard.setPrimaryClip(clip);
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
