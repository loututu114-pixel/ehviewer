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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.UniversalFileOpener;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件打开方式设置Activity
 */
public class FileOpenerSettingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileTypeSettingsAdapter adapter;
    private List<FileTypeSetting> fileTypeSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_opener_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("文件打开设置");
        }

        initData();
        initViews();
    }

    private void initData() {
        fileTypeSettings = new ArrayList<>();

        // 添加各种文件类型的设置
        fileTypeSettings.add(new FileTypeSetting(
            UniversalFileOpener.FileType.DOCUMENT,
            "文档文件",
            "PDF, Word, Excel, PowerPoint, 文本文件等"
        ));

        fileTypeSettings.add(new FileTypeSetting(
            UniversalFileOpener.FileType.VIDEO,
            "视频文件",
            "MP4, WebM, AVI, MOV, WMV, FLV, MKV等"
        ));

        fileTypeSettings.add(new FileTypeSetting(
            UniversalFileOpener.FileType.AUDIO,
            "音频文件",
            "MP3, WAV, OGG, AAC, M4A, FLAC等"
        ));

        fileTypeSettings.add(new FileTypeSetting(
            UniversalFileOpener.FileType.IMAGE,
            "图片文件",
            "JPG, PNG, GIF, BMP, WebP, SVG等"
        ));

        fileTypeSettings.add(new FileTypeSetting(
            UniversalFileOpener.FileType.ARCHIVE,
            "压缩文件",
            "ZIP, RAR, 7Z等压缩包"
        ));

        fileTypeSettings.add(new FileTypeSetting(
            UniversalFileOpener.FileType.APK,
            "应用安装包",
            "APK安装文件"
        ));
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileTypeSettingsAdapter(fileTypeSettings);
        recyclerView.setAdapter(adapter);

        // 设置默认应用按钮
        findViewById(R.id.btn_set_as_default).setOnClickListener(v -> setAsDefaultApps());

        // 清除设置按钮
        findViewById(R.id.btn_clear_settings).setOnClickListener(v -> clearAllSettings());
    }

    private void setAsDefaultApps() {
        try {
            // 打开系统默认应用设置页面
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "请在系统设置中将EhViewer设置为默认应用", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("FileOpenerSettings", "Failed to open default apps settings", e);
            Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAllSettings() {
        // 清除所有文件打开方式的偏好设置
        for (FileTypeSetting setting : fileTypeSettings) {
            UniversalFileOpener.setPreferredOpenMethod(this, setting.fileType,
                UniversalFileOpener.OpenMethod.SYSTEM_DEFAULT);
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "已清除所有设置", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * 文件类型设置项
     */
    private static class FileTypeSetting {
        UniversalFileOpener.FileType fileType;
        String name;
        String description;

        FileTypeSetting(UniversalFileOpener.FileType fileType, String name, String description) {
            this.fileType = fileType;
            this.name = name;
            this.description = description;
        }
    }

    /**
     * 文件类型设置适配器
     */
    private class FileTypeSettingsAdapter extends RecyclerView.Adapter<FileTypeSettingsAdapter.ViewHolder> {

        private List<FileTypeSetting> settings;

        FileTypeSettingsAdapter(List<FileTypeSetting> settings) {
            this.settings = settings;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_file_type_setting, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FileTypeSetting setting = settings.get(position);
            holder.bind(setting);
        }

        @Override
        public int getItemCount() {
            return settings.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFileType;
            TextView tvDescription;
            Spinner spinnerOpenMethod;

            ViewHolder(View itemView) {
                super(itemView);
                tvFileType = itemView.findViewById(R.id.tv_file_type);
                tvDescription = itemView.findViewById(R.id.tv_description);
                spinnerOpenMethod = itemView.findViewById(R.id.spinner_open_method);
            }

            void bind(FileTypeSetting setting) {
                tvFileType.setText(setting.name);
                tvDescription.setText(setting.description);

                // 设置Spinner适配器
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(itemView.getContext(),
                    android.R.layout.simple_spinner_item);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // 添加打开方式选项
                for (UniversalFileOpener.OpenMethod method : UniversalFileOpener.OpenMethod.values()) {
                    spinnerAdapter.add(method.getDisplayName());
                }

                spinnerOpenMethod.setAdapter(spinnerAdapter);

                // 设置当前选中的打开方式
                UniversalFileOpener.OpenMethod currentMethod = UniversalFileOpener.OpenMethod.SYSTEM_DEFAULT;
                try {
                    // 这里应该从SharedPreferences获取当前设置
                    // 暂时使用默认值
                    currentMethod = UniversalFileOpener.OpenMethod.INTERNAL_VIEWER;
                } catch (Exception e) {
                    android.util.Log.w("FileOpenerSettings", "Failed to get current open method", e);
                }

                int position = 0;
                for (int i = 0; i < UniversalFileOpener.OpenMethod.values().length; i++) {
                    if (UniversalFileOpener.OpenMethod.values()[i] == currentMethod) {
                        position = i;
                        break;
                    }
                }
                spinnerOpenMethod.setSelection(position);

                // 设置选择监听器
                spinnerOpenMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        UniversalFileOpener.OpenMethod selectedMethod =
                            UniversalFileOpener.OpenMethod.values()[position];
                        UniversalFileOpener.setPreferredOpenMethod(
                            itemView.getContext(), setting.fileType, selectedMethod);
                        Toast.makeText(itemView.getContext(),
                            setting.name + "打开方式已设置为: " + selectedMethod.getDisplayName(),
                            Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // 不做处理
                    }
                });
            }
        }
    }
}
