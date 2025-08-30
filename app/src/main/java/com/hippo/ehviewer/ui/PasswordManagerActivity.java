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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.PasswordManager;
import com.hippo.ehviewer.client.PasswordAutofillService;
import com.hippo.ehviewer.databinding.ActivityPasswordManagerBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 密码管理器活动
 * 提供密码查看、管理和设置功能
 */
public class PasswordManagerActivity extends AppCompatActivity {

    private ActivityPasswordManagerBinding binding;
    private PasswordManager passwordManager;
    private PasswordAdapter passwordAdapter;
    private List<PasswordManager.PasswordEntry> passwordList = new ArrayList<>();
    private boolean isUnlocked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPasswordManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        passwordManager = PasswordManager.getInstance(this);

        setupRecyclerView();
        setupFab();
        checkUnlockStatus();
    }

    private void setupRecyclerView() {
        passwordAdapter = new PasswordAdapter(passwordList, new PasswordAdapter.OnPasswordClickListener() {
            @Override
            public void onPasswordClick(PasswordManager.PasswordEntry entry) {
                showPasswordDetails(entry);
            }

            @Override
            public void onPasswordLongClick(PasswordManager.PasswordEntry entry) {
                showPasswordActions(entry);
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(passwordAdapter);
    }

    private void setupFab() {
        binding.fabAddPassword.setOnClickListener(v -> showAddPasswordDialog());
    }

    private void checkUnlockStatus() {
        if (passwordManager.isUnlocked()) {
            isUnlocked = true;
            loadPasswords();
        } else {
            showUnlockPrompt();
        }
    }

    private void showUnlockPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("解锁密码管理器")
                .setSubtitle("使用生物识别验证身份")
                .setDescription("需要验证您的身份才能访问密码管理器")
                .setNegativeButtonText("取消")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                getMainExecutor(),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        isUnlocked = true;
                        loadPasswords();
                        Toast.makeText(PasswordManagerActivity.this, "密码管理器已解锁", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(PasswordManagerActivity.this, "验证失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private void loadPasswords() {
        if (!isUnlocked) return;

        passwordList.clear();
        // 临时添加一些示例数据用于演示
        // 在实际使用中，这些数据应该从passwordManager加载

        updateEmptyState();
        passwordAdapter.notifyDataSetChanged();
    }

    private void updateEmptyState() {
        if (passwordList.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showPasswordDetails(PasswordManager.PasswordEntry entry) {
        PasswordDetailsDialog dialog = PasswordDetailsDialog.newInstance(entry);
        dialog.show(getSupportFragmentManager(), "password_details");
    }

    private void showPasswordActions(PasswordManager.PasswordEntry entry) {
        PasswordActionsDialog dialog = PasswordActionsDialog.newInstance(entry);
        dialog.show(getSupportFragmentManager(), "password_actions");
    }

    private void showAddPasswordDialog() {
        AddPasswordDialog dialog = new AddPasswordDialog();
        dialog.show(getSupportFragmentManager(), "add_password");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_password_manager, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchPasswords(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchPasswords(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_settings) {
            showSettings();
            return true;
        } else if (itemId == R.id.action_generate_password) {
            showPasswordGenerator();
            return true;
        } else if (itemId == R.id.action_export) {
            exportPasswords();
            return true;
        } else if (itemId == R.id.action_import) {
            importPasswords();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void searchPasswords(String query) {
        if (!isUnlocked) return;

        if (query.isEmpty()) {
            loadPasswords();
        } else {
            List<PasswordManager.PasswordEntry> searchResults = passwordManager.searchPasswords(query);
            passwordList.clear();
            passwordList.addAll(searchResults);
            passwordAdapter.notifyDataSetChanged();
            updateEmptyState();
        }
    }

    private void showSettings() {
        Intent intent = new Intent(this, PasswordSettingsActivity.class);
        startActivity(intent);
    }

    private void showPasswordGenerator() {
        PasswordGeneratorDialog dialog = new PasswordGeneratorDialog();
        dialog.show(getSupportFragmentManager(), "password_generator");
    }

    private void exportPasswords() {
        if (!isUnlocked) return;

        String exportedData = passwordManager.exportPasswords();
        if (exportedData != null) {
            // 分享导出的数据
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, exportedData);
            startActivity(Intent.createChooser(shareIntent, "导出密码数据"));
        } else {
            Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void importPasswords() {
        // 这里可以实现密码导入功能
        Toast.makeText(this, "密码导入功能即将推出", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isUnlocked) {
            loadPasswords();
        }
    }
}
