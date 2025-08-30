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

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.util.ExceptionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 下载管理Activity
 * 显示和管理所有下载任务
 */
public class DownloadManagerActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private DownloadAdapter mAdapter;
    private DownloadManager mDownloadManager;
    private List<DownloadItem> mDownloads;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);

        try {
            mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            mDownloads = new ArrayList<>();

            // 设置Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("下载管理");
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            }

            // 设置RecyclerView
            mRecyclerView = findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new DownloadAdapter();
            mRecyclerView.setAdapter(mAdapter);

            // 加载下载列表
            loadDownloads();

            // 注册下载完成的广播接收器
            registerReceiver(mDownloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } catch (Exception e) {
            android.util.Log.e("DownloadManagerActivity", "Error in onCreate", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mDownloadReceiver);
        } catch (Exception e) {
            // Receiver可能没有注册，忽略异常
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.download_manager_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_clear_completed) {
            clearCompletedDownloads();
            return true;
        } else if (itemId == R.id.action_clear_all) {
            clearAllDownloads();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 加载下载列表
     */
    private void loadDownloads() {
        try {
            mDownloads.clear();

            DownloadManager.Query query = new DownloadManager.Query();
            Cursor cursor = mDownloadManager.query(query);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                    String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                    String description = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
                    String uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    long totalSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long downloadedSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    String localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                    DownloadItem item = new DownloadItem();
                    item.id = id;
                    item.title = title != null ? title : uri != null ? Uri.parse(uri).getLastPathSegment() : "未知文件";
                    item.description = description;
                    item.uri = uri;
                    item.status = status;
                    item.totalSize = totalSize;
                    item.downloadedSize = downloadedSize;
                    item.localUri = localUri;

                    mDownloads.add(item);
                } while (cursor.moveToNext());

                cursor.close();
            }

            mAdapter.notifyDataSetChanged();

            // 显示空状态
            updateEmptyState();

        } catch (Exception e) {
            android.util.Log.e("DownloadManagerActivity", "Error loading downloads", e);
        }
    }

    /**
     * 更新空状态显示
     */
    private void updateEmptyState() {
        View emptyView = findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(mDownloads.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(mDownloads.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * 清除已完成的下载
     */
    private void clearCompletedDownloads() {
        try {
            List<Long> idsToRemove = new ArrayList<>();
            for (DownloadItem item : mDownloads) {
                if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                    idsToRemove.add(item.id);
                }
            }

            for (Long id : idsToRemove) {
                mDownloadManager.remove(id);
            }

            loadDownloads();
        } catch (Exception e) {
            android.util.Log.e("DownloadManagerActivity", "Error clearing completed downloads", e);
        }
    }

    /**
     * 清除所有下载
     */
    private void clearAllDownloads() {
        try {
            for (DownloadItem item : mDownloads) {
                mDownloadManager.remove(item.id);
            }
            mDownloads.clear();
            mAdapter.notifyDataSetChanged();
            updateEmptyState();
        } catch (Exception e) {
            android.util.Log.e("DownloadManagerActivity", "Error clearing all downloads", e);
        }
    }

    /**
     * 下载完成广播接收器
     */
    private BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 下载完成，刷新列表
            loadDownloads();
        }
    };

    /**
     * 下载项数据类
     */
    private static class DownloadItem {
        long id;
        String title;
        String description;
        String uri;
        int status;
        long totalSize;
        long downloadedSize;
        String localUri;
    }

    /**
     * 下载适配器
     */
    private class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {

        @NonNull
        @Override
        public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_download, parent, false);
            return new DownloadViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
            DownloadItem item = mDownloads.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return mDownloads.size();
        }

        class DownloadViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTitleView;
            private final TextView mStatusView;
            private final ProgressBar mProgressBar;
            private final ImageButton mActionButton;

            DownloadViewHolder(@NonNull View itemView) {
                super(itemView);
                mTitleView = itemView.findViewById(R.id.title_view);
                mStatusView = itemView.findViewById(R.id.status_view);
                mProgressBar = itemView.findViewById(R.id.progress_bar);
                mActionButton = itemView.findViewById(R.id.action_button);

                mActionButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        DownloadItem item = mDownloads.get(position);
                        handleDownloadAction(item);
                    }
                });
            }

            private void handleDownloadAction(DownloadItem item) {
                try {
                    switch (item.status) {
                        case DownloadManager.STATUS_PENDING:
                        case DownloadManager.STATUS_RUNNING:
                            // 暂停下载
                            mDownloadManager.remove(item.id);
                            loadDownloads();
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            // 恢复下载（需要重新创建下载请求）
                            android.widget.Toast.makeText(DownloadManagerActivity.this,
                                "请重新下载", android.widget.Toast.LENGTH_SHORT).show();
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            // 打开文件
                            if (item.localUri != null) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(item.localUri));
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                            }
                            break;
                        case DownloadManager.STATUS_FAILED:
                            // 重试下载
                            android.widget.Toast.makeText(DownloadManagerActivity.this,
                                "下载失败，请重试", android.widget.Toast.LENGTH_SHORT).show();
                            break;
                    }
                } catch (Exception e) {
                    android.util.Log.e("DownloadManagerActivity", "Error handling download action", e);
                }
            }

            void bind(DownloadItem item) {
                // 设置标题
                if (mTitleView != null) {
                    mTitleView.setText(item.title != null ? item.title : "未知文件");
                }

                // 设置状态和进度
                if (mStatusView != null && mProgressBar != null) {
                    String statusText = "";
                    int progress = 0;

                    switch (item.status) {
                        case DownloadManager.STATUS_PENDING:
                            statusText = "等待中";
                            progress = 0;
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            if (item.totalSize > 0) {
                                progress = (int) ((item.downloadedSize * 100) / item.totalSize);
                                statusText = String.format(Locale.getDefault(),
                                    "%d%% (%s/%s)",
                                    progress,
                                    formatFileSize(item.downloadedSize),
                                    formatFileSize(item.totalSize));
                            } else {
                                statusText = "下载中...";
                                progress = 0;
                            }
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            statusText = "已暂停";
                            if (item.totalSize > 0) {
                                progress = (int) ((item.downloadedSize * 100) / item.totalSize);
                            }
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            statusText = "已完成";
                            progress = 100;
                            break;
                        case DownloadManager.STATUS_FAILED:
                            statusText = "失败";
                            progress = 0;
                            break;
                        default:
                            statusText = "未知状态";
                            progress = 0;
                            break;
                    }

                    mStatusView.setText(statusText);
                    mProgressBar.setProgress(progress);
                }

                // 设置操作按钮
                if (mActionButton != null) {
                    switch (item.status) {
                        case DownloadManager.STATUS_PENDING:
                        case DownloadManager.STATUS_RUNNING:
                            mActionButton.setImageResource(R.drawable.v_pause_dark_x24);
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            mActionButton.setImageResource(R.drawable.v_play_dark_x24);
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            mActionButton.setImageResource(R.drawable.v_book_open_primary_x24);
                            break;
                        case DownloadManager.STATUS_FAILED:
                            mActionButton.setImageResource(R.drawable.v_refresh_dark_x24);
                            break;
                        default:
                            mActionButton.setImageResource(R.drawable.v_dots_vertical_secondary_dark_x24);
                            break;
                    }
                }
            }

            private String formatFileSize(long size) {
                if (size <= 0) return "0 B";
                final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
                int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
                return String.format(Locale.getDefault(), "%.1f %s",
                    size / Math.pow(1024, digitGroups), units[digitGroups]);
            }
        }
    }

    /**
     * 启动下载管理Activity
     */
    public static void startDownloadManager(Context context) {
        try {
            Intent intent = new Intent(context, DownloadManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("DownloadManagerActivity", "Failed to start DownloadManagerActivity", e);
        }
    }
}
