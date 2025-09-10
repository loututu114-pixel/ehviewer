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
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.client.data.HistoryInfo;
import com.hippo.scene.Announcer;
import com.hippo.util.ExceptionUtils;
import java.util.List;

/**
 * 浏览器历史记录Activity
 */
public class BrowserHistoryActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private BrowserHistoryAdapter mAdapter;
    private List<HistoryInfo> mHistoryList;
    private HistoryManager mHistoryManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_history);

        try {
            // 获取历史管理器实例
            mHistoryManager = HistoryManager.getInstance(this);

            // 设置Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("浏览器历史");
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            }

            // 设置RecyclerView
            mRecyclerView = findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new BrowserHistoryAdapter();
            mRecyclerView.setAdapter(mAdapter);

            // 加载历史记录
            loadHistory();

        } catch (Exception e) {
            android.util.Log.e("BrowserHistoryActivity", "Error in onCreate", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browser_history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_clear_history) {
            // 清空历史记录
            showClearHistoryDialog();
            return true;
        } else if (itemId == android.R.id.home) {
            // 返回按钮
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示清空历史记录对话框
     */
    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清空历史记录")
                .setMessage("确定要清空所有浏览器历史记录吗？此操作不可撤销。")
                .setPositiveButton("确定", (dialog, which) -> {
                    mHistoryManager.clearAllHistory();
                    loadHistory(); // 重新加载历史记录
                    android.widget.Toast.makeText(this, "历史记录已清空", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 加载历史记录
     */
    private void loadHistory() {
        try {
            mHistoryList = mHistoryManager.getAllHistory();
            mAdapter.notifyDataSetChanged();

            // 如果没有历史记录，显示提示
            if (mHistoryList.isEmpty()) {
                findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else {
                findViewById(R.id.empty_view).setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            android.util.Log.e("BrowserHistoryActivity", "Error loading history", e);
        }
    }

    /**
     * 启动浏览器历史Activity的静态方法
     */
    public static void startBrowserHistory(Context context) {
        Intent intent = new Intent(context, BrowserHistoryActivity.class);
        context.startActivity(intent);
    }

    /**
     * 浏览器历史记录适配器
     */
    private class BrowserHistoryAdapter extends RecyclerView.Adapter<BrowserHistoryViewHolder> {

        @NonNull
        @Override
        public BrowserHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_browser_history, parent, false);
            return new BrowserHistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BrowserHistoryViewHolder holder, int position) {
            HistoryInfo history = mHistoryList.get(position);
            holder.bind(history);
        }

        @Override
        public int getItemCount() {
            return mHistoryList != null ? mHistoryList.size() : 0;
        }
    }

    /**
     * 浏览器历史记录ViewHolder
     */
    private class BrowserHistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleText;
        private final TextView urlText;
        private final TextView timeText;
        private final ImageView deleteButton;

        public BrowserHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            titleText = itemView.findViewById(R.id.title_text);
            urlText = itemView.findViewById(R.id.url_text);
            timeText = itemView.findViewById(R.id.time_text);
            deleteButton = itemView.findViewById(R.id.delete_button);

            // 点击项打开URL
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    HistoryInfo history = mHistoryList.get(position);
                    openUrl(history.url);
                }
            });

            // 删除按钮
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    HistoryInfo history = mHistoryList.get(position);
                    showDeleteHistoryDialog(history);
                }
            });
        }

        public void bind(HistoryInfo history) {
            titleText.setText(history.title != null ? history.title : "无标题");
            urlText.setText(history.url);

            // 格式化时间显示
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",
                    java.util.Locale.getDefault());
            timeText.setText(sdf.format(new java.util.Date(history.visitTime)));
        }

        /**
         * 打开URL
         */
        private void openUrl(String url) {
            try {
                Intent intent = new Intent(BrowserHistoryActivity.this, YCWebViewActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("BrowserHistoryActivity", "Error opening URL", e);
                android.widget.Toast.makeText(BrowserHistoryActivity.this, "无法打开链接",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 显示删除历史记录对话框
         */
        private void showDeleteHistoryDialog(HistoryInfo history) {
            new AlertDialog.Builder(BrowserHistoryActivity.this)
                    .setTitle("删除历史记录")
                    .setMessage("确定要删除这条历史记录吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        if (mHistoryManager.deleteHistory(history.id)) {
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                mHistoryList.remove(position);
                                mAdapter.notifyItemRemoved(position);
                                android.widget.Toast.makeText(BrowserHistoryActivity.this,
                                        "历史记录已删除", android.widget.Toast.LENGTH_SHORT).show();

                                // 检查是否为空
                                if (mHistoryList.isEmpty()) {
                                    findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                                    mRecyclerView.setVisibility(View.GONE);
                                }
                            }
                        } else {
                            android.widget.Toast.makeText(BrowserHistoryActivity.this,
                                    "删除失败", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }
}
