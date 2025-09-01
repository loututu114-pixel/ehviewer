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
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.client.data.HistoryInfo;
import com.hippo.util.ExceptionUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史记录Activity
 */
public class HistoryActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private RecyclerView mRecyclerView;
    private HistoryAdapter mAdapter;
    private HistoryManager mHistoryManager;
    private List<HistoryInfo> mAllHistory;
    private List<HistoryInfo> mFilteredHistory;
    private SearchView mSearchView;

    // 排序相关
    private static final String PREF_SORT_ORDER = "history_sort_order";
    private String mCurrentSortOrder = "last_visit"; // 默认按访问时间排序

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        try {
            mHistoryManager = HistoryManager.getInstance(this);

            // 加载排序偏好
            android.content.SharedPreferences prefs = getSharedPreferences("history_prefs", MODE_PRIVATE);
            mCurrentSortOrder = prefs.getString(PREF_SORT_ORDER, "last_visit");

            // 设置Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(R.string.browser_history);
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            }

            // 设置RecyclerView
            mRecyclerView = findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new HistoryAdapter();
            mRecyclerView.setAdapter(mAdapter);

            // 加载历史记录
            loadHistory();

        } catch (Exception e) {
            android.util.Log.e("HistoryActivity", "Error in onCreate", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);

        // 设置搜索功能
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setQueryHint(getString(R.string.search_browser_history));
        }

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
        } else if (itemId == R.id.action_sort_visit_count) {
            setSortOrder("visit_count");
            return true;
        } else if (itemId == R.id.action_sort_alphabetical) {
            setSortOrder("alphabetical");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        filterHistory(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filterHistory(newText);
        return true;
    }

    /**
     * 加载历史记录
     */
    private void loadHistory() {
        mAllHistory = mHistoryManager.getAllHistory();

        // 根据当前排序方式排序历史记录
        sortHistory();

        mFilteredHistory = new ArrayList<>(mAllHistory);
        mAdapter.notifyDataSetChanged();

        // 显示空状态
        updateEmptyState();
    }

    /**
     * 根据当前排序方式排序历史记录
     */
    private void sortHistory() {
        if (mAllHistory == null || mAllHistory.isEmpty()) {
            return;
        }

        switch (mCurrentSortOrder) {
            case "last_visit":
                // 按访问时间排序（最新的在前面）
                mAllHistory.sort((h1, h2) -> Long.compare(h2.visitTime, h1.visitTime));
                break;
            case "visit_count":
                // 按访问次数排序
                mAllHistory.sort((h1, h2) -> Integer.compare(h2.visitCount, h1.visitCount));
                break;
            case "alphabetical":
                // 按字母顺序排序
                mAllHistory.sort((h1, h2) -> {
                    String title1 = h1.title != null ? h1.title : "";
                    String title2 = h2.title != null ? h2.title : "";
                    return title1.compareToIgnoreCase(title2);
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
        android.content.SharedPreferences prefs = getSharedPreferences("history_prefs", MODE_PRIVATE);
        prefs.edit().putString(PREF_SORT_ORDER, sortOrder).apply();

        // 重新加载历史记录
        loadHistory();
    }

    /**
     * 过滤历史记录
     */
    private void filterHistory(String query) {
        mFilteredHistory.clear();
        if (query == null || query.trim().isEmpty()) {
            mFilteredHistory.addAll(mAllHistory);
        } else {
            String lowerQuery = query.toLowerCase();
            for (HistoryInfo history : mAllHistory) {
                if (history.title != null && history.title.toLowerCase().contains(lowerQuery)) {
                    mFilteredHistory.add(history);
                } else if (history.url != null && history.url.toLowerCase().contains(lowerQuery)) {
                    mFilteredHistory.add(history);
                } else if (history.getDomain().toLowerCase().contains(lowerQuery)) {
                    mFilteredHistory.add(history);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    /**
     * 更新空状态显示
     */
    private void updateEmptyState() {
        View emptyView = findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(mFilteredHistory.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(mFilteredHistory.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * 显示清空所有历史记录对话框
     */
    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_all_history)
                .setMessage("确定要删除所有浏览器历史记录吗？此操作不可恢复。")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mHistoryManager.clearAllHistory();
                    loadHistory();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 显示历史记录操作菜单
     */
    private void showHistoryMenu(HistoryInfo history) {
        String[] options = {"打开", "删除"};

        new AlertDialog.Builder(this)
                .setTitle(history.getDisplayTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // 打开
                            WebViewActivity.startWebView(this, history.url);
                            finish();
                            break;
                        case 1: // 删除
                            showDeleteHistoryDialog(history);
                            break;
                    }
                })
                .show();
    }

    /**
     * 显示删除历史记录对话框
     */
    private void showDeleteHistoryDialog(HistoryInfo history) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_browser_history)
                .setMessage("确定要删除历史记录 \"" + history.getDisplayTitle() + "\" 吗？")
                .setPositiveButton(R.string.delete_browser_history, (dialog, which) -> {
                    mHistoryManager.deleteHistory(history.id);
                    loadHistory();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 历史记录列表适配器
     */
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            HistoryInfo history = mFilteredHistory.get(position);
            holder.bind(history);
        }

        @Override
        public int getItemCount() {
            return mFilteredHistory.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            private final ImageView mFaviconView;
            private final TextView mTitleView;
            private final TextView mUrlView;
            private final TextView mTimeView;
            private final ImageView mMenuButton;

            HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                mFaviconView = itemView.findViewById(R.id.favicon_view);
                mTitleView = itemView.findViewById(R.id.title_view);
                mUrlView = itemView.findViewById(R.id.url_view);
                mTimeView = itemView.findViewById(R.id.time_view);
                mMenuButton = itemView.findViewById(R.id.menu_button);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        HistoryInfo history = mFilteredHistory.get(position);
                        WebViewActivity.startWebView(HistoryActivity.this, history.url);
                        finish();
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        HistoryInfo history = mFilteredHistory.get(position);
                        showHistoryMenu(history);
                    }
                    return true;
                });

                mMenuButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        HistoryInfo history = mFilteredHistory.get(position);
                        showHistoryMenu(history);
                    }
                });
            }

            void bind(HistoryInfo history) {
                mTitleView.setText(history.getDisplayTitle());
                
                // 只显示域名，不显示完整URL
                String domain = history.getDomain();
                mUrlView.setText(domain != null ? domain : history.url);
                
                mTimeView.setText(history.getRelativeTime());

                // 设置网页图标
                mFaviconView.setImageResource(R.drawable.ic_web);
            }
        }
    }

    /**
     * 启动历史记录Activity
     */
    public static void startHistory(Context context) {
        try {
            Intent intent = new Intent(context, HistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("HistoryActivity", "Failed to start HistoryActivity", e);
        }
    }
}
