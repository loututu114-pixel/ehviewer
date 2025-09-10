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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.dao.HistoryInfo;
import com.hippo.ehviewer.ui.scene.gallery.detail.GalleryDetailScene;
import com.hippo.scene.Announcer;
import com.hippo.util.ExceptionUtils;
import java.util.List;

/**
 * EhViewer图库历史记录Activity
 */
public class GalleryHistoryActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private GalleryHistoryAdapter mAdapter;
    private List<HistoryInfo> mHistoryList;

    // 排序相关
    private static final String PREF_SORT_ORDER = "gallery_history_sort_order";
    private String mCurrentSortOrder = "last_visit"; // 默认按访问时间排序

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_history);

        try {
            // 加载排序偏好
            android.content.SharedPreferences prefs = getSharedPreferences("gallery_history_prefs", MODE_PRIVATE);
            mCurrentSortOrder = prefs.getString(PREF_SORT_ORDER, "last_visit");

            // 设置Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(R.string.gallery_history);
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            }

            // 设置RecyclerView
            mRecyclerView = findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new GalleryHistoryAdapter();
            mRecyclerView.setAdapter(mAdapter);

            // 加载历史记录
            loadHistory();

        } catch (Exception e) {
            android.util.Log.e("GalleryHistoryActivity", "Error in onCreate", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery_history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_clear_all) {
            showClearAllDialog();
            return true;
        } else if (itemId == R.id.action_sort_last_visit) {
            setSortOrder("last_visit");
            return true;
        } else if (itemId == R.id.action_sort_title) {
            setSortOrder("title");
            return true;
        } else if (itemId == R.id.action_sort_uploader) {
            setSortOrder("uploader");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 加载历史记录列表
     */
    private void loadHistory() {
        mHistoryList = EhDB.getHistoryLazyList();

        // 根据当前排序方式排序历史记录
        sortHistory();

        mAdapter.notifyDataSetChanged();

        // 显示空状态
        View emptyView = findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(mHistoryList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(mHistoryList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * 根据当前排序方式排序历史记录
     */
    private void sortHistory() {
        if (mHistoryList == null || mHistoryList.isEmpty()) {
            return;
        }

        switch (mCurrentSortOrder) {
            case "last_visit":
                // 按访问时间排序（最新的在前面）
                mHistoryList.sort((h1, h2) -> Long.compare(h2.time, h1.time));
                break;
            case "title":
                // 按标题排序
                mHistoryList.sort((h1, h2) -> {
                    String title1 = h1.title != null ? h1.title : "";
                    String title2 = h2.title != null ? h2.title : "";
                    return title1.compareToIgnoreCase(title2);
                });
                break;
            case "uploader":
                // 按上传者排序
                mHistoryList.sort((h1, h2) -> {
                    String uploader1 = h1.uploader != null ? h1.uploader : "";
                    String uploader2 = h2.uploader != null ? h2.uploader : "";
                    return uploader1.compareToIgnoreCase(uploader2);
                });
                break;
        }
    }

    /**
     * 设置排序方式
     */
    private void setSortOrder(String sortOrder) {
        mCurrentSortOrder = sortOrder;

        // 保存排序偏好
        android.content.SharedPreferences prefs = getSharedPreferences("gallery_history_prefs", MODE_PRIVATE);
        prefs.edit().putString(PREF_SORT_ORDER, sortOrder).apply();

        // 重新加载历史记录
        loadHistory();
    }

    /**
     * 显示清空所有历史记录对话框
     */
    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_all_gallery_history)
                .setMessage("确定要删除所有EhViewer图库浏览历史记录吗？此操作不可恢复。")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    EhDB.clearHistoryInfo();
                    loadHistory();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 显示历史记录操作菜单
     */
    private void showHistoryMenu(HistoryInfo history) {
        String[] options = {"查看详情", "删除"};

        new AlertDialog.Builder(this)
                .setTitle(history.title)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // 查看详情
                            openGalleryDetail(history);
                            break;
                        case 1: // 删除
                            showDeleteHistoryDialog(history);
                            break;
                    }
                })
                .show();
    }

    /**
     * 打开图库详情
     */
    private void openGalleryDetail(HistoryInfo history) {
        // 使用EhViewer的标准方式打开图库详情
        Intent intent = new Intent(this, com.hippo.ehviewer.ui.GalleryActivity.class);
        intent.putExtra("gid", history.gid);
        intent.putExtra("token", history.token);
        startActivity(intent);
        finish();
    }

    /**
     * 显示删除历史记录对话框
     */
    private void showDeleteHistoryDialog(HistoryInfo history) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_gallery_history)
                .setMessage("确定要删除历史记录 \"" + history.title + "\" 吗？")
                .setPositiveButton(R.string.delete_gallery_history, (dialog, which) -> {
                    EhDB.deleteHistoryInfo(history);
                    loadHistory();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 图库历史记录列表适配器
     */
    private class GalleryHistoryAdapter extends RecyclerView.Adapter<GalleryHistoryAdapter.HistoryViewHolder> {

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gallery_history, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            HistoryInfo history = mHistoryList.get(position);
            holder.bind(history);
        }

        @Override
        public int getItemCount() {
            return mHistoryList.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            private final ImageView mThumbView;
            private final TextView mTitleView;
            private final TextView mUploaderView;
            private final TextView mTimeView;
            private final TextView mCategoryView;

            HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                mThumbView = itemView.findViewById(R.id.thumb_view);
                mTitleView = itemView.findViewById(R.id.title_view);
                mUploaderView = itemView.findViewById(R.id.uploader_view);
                mTimeView = itemView.findViewById(R.id.time_view);
                mCategoryView = itemView.findViewById(R.id.category_view);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        HistoryInfo history = mHistoryList.get(position);
                        openGalleryDetail(history);
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        HistoryInfo history = mHistoryList.get(position);
                        showHistoryMenu(history);
                    }
                    return true;
                });
            }

            void bind(HistoryInfo history) {
                mTitleView.setText(history.title);
                mUploaderView.setText(history.uploader != null ? history.uploader : "未知");
                mTimeView.setText(formatTime(history.time));
                mCategoryView.setText(getCategoryText(history.category));

                // 设置缩略图
                if (history.thumb != null) {
                    // TODO: 加载缩略图
                    // 这里需要使用图片加载库来加载缩略图
                } else {
                    mThumbView.setImageResource(R.mipmap.ic_launcher);
                }
            }

            private String formatTime(long time) {
                // 格式化时间显示
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                return sdf.format(new java.util.Date(time));
            }

            private String getCategoryText(int category) {
                // 根据分类ID返回分类名称
                String[] categories = getResources().getStringArray(R.array.gallery_categories);
                if (category >= 0 && category < categories.length) {
                    return categories[category];
                }
                return "未知";
            }
        }
    }

    /**
     * 启动图库历史记录Activity
     */
    public static void startGalleryHistory(Context context) {
        try {
            Intent intent = new Intent(context, GalleryHistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("GalleryHistoryActivity", "Failed to start GalleryHistoryActivity", e);
        }
    }
}
