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
import com.hippo.ehviewer.client.BookmarkManager;
import com.hippo.ehviewer.client.data.BookmarkInfo;
import com.hippo.util.ExceptionUtils;
import java.util.List;

/**
 * 书签管理Activity
 */
public class BookmarksActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private BookmarksAdapter mAdapter;
    private BookmarkManager mBookmarkManager;
    private List<BookmarkInfo> mBookmarks;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        try {
            mBookmarkManager = BookmarkManager.getInstance(this);

            // 设置Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(R.string.bookmarks);
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            }

            // 设置RecyclerView
            mRecyclerView = findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new BookmarksAdapter();
            mRecyclerView.setAdapter(mAdapter);

            // 加载书签
            loadBookmarks();

        } catch (Exception e) {
            android.util.Log.e("BookmarksActivity", "Error in onCreate", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bookmarks_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add_bookmark) {
            showAddBookmarkDialog();
            return true;
        } else if (itemId == R.id.action_clear_all) {
            showClearAllDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 加载书签列表
     */
    private void loadBookmarks() {
        mBookmarks = mBookmarkManager.getAllBookmarks();
        mAdapter.notifyDataSetChanged();

        // 显示空状态
        View emptyView = findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(mBookmarks.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(mBookmarks.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * 显示添加书签对话框
     */
    private void showAddBookmarkDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_bookmark, null);
        android.widget.EditText titleEdit = dialogView.findViewById(R.id.title_edit);
        android.widget.EditText urlEdit = dialogView.findViewById(R.id.url_edit);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_bookmark)
                .setView(dialogView)
                .setPositiveButton(R.string.add_bookmark, (dialog, which) -> {
                    String title = titleEdit.getText().toString().trim();
                    String url = urlEdit.getText().toString().trim();

                    if (!title.isEmpty() && !url.isEmpty()) {
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }

                        BookmarkInfo bookmark = new BookmarkInfo(title, url);
                        mBookmarkManager.addBookmark(bookmark);
                        loadBookmarks();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示清空所有书签对话框
     */
    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_all_bookmarks)
                .setMessage("确定要删除所有书签吗？此操作不可恢复。")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // 逐个删除所有书签
                    for (BookmarkInfo bookmark : mBookmarks) {
                        mBookmarkManager.deleteBookmark(bookmark.id);
                    }
                    loadBookmarks();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示书签操作菜单
     */
    private void showBookmarkMenu(BookmarkInfo bookmark) {
        String[] options = {"打开", "编辑", "删除"};

        new AlertDialog.Builder(this)
                .setTitle(bookmark.getDisplayTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // 打开
                            WebViewActivity.startWebView(this, bookmark.url);
                            finish();
                            break;
                        case 1: // 编辑
                            showEditBookmarkDialog(bookmark);
                            break;
                        case 2: // 删除
                            showDeleteBookmarkDialog(bookmark);
                            break;
                    }
                })
                .show();
    }

    /**
     * 显示编辑书签对话框
     */
    private void showEditBookmarkDialog(BookmarkInfo bookmark) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_bookmark, null);
        android.widget.EditText titleEdit = dialogView.findViewById(R.id.title_edit);
        android.widget.EditText urlEdit = dialogView.findViewById(R.id.url_edit);

        titleEdit.setText(bookmark.title);
        urlEdit.setText(bookmark.url);

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_bookmark)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String title = titleEdit.getText().toString().trim();
                    String url = urlEdit.getText().toString().trim();

                    if (!title.isEmpty() && !url.isEmpty()) {
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }

                        bookmark.title = title;
                        bookmark.url = url;
                        mBookmarkManager.updateBookmark(bookmark);
                        loadBookmarks();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示删除书签对话框
     */
    private void showDeleteBookmarkDialog(BookmarkInfo bookmark) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_bookmark)
                .setMessage("确定要删除书签 \"" + bookmark.getDisplayTitle() + "\" 吗？")
                .setPositiveButton(R.string.delete_bookmark, (dialog, which) -> {
                    mBookmarkManager.deleteBookmark(bookmark.id);
                    loadBookmarks();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 书签列表适配器
     */
    private class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkViewHolder> {

        @NonNull
        @Override
        public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bookmark, parent, false);
            return new BookmarkViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
            BookmarkInfo bookmark = mBookmarks.get(position);
            holder.bind(bookmark);
        }

        @Override
        public int getItemCount() {
            return mBookmarks.size();
        }

        class BookmarkViewHolder extends RecyclerView.ViewHolder {
            private final ImageView mFaviconView;
            private final TextView mTitleView;
            private final TextView mUrlView;
            private final TextView mDomainView;

            BookmarkViewHolder(@NonNull View itemView) {
                super(itemView);
                mFaviconView = itemView.findViewById(R.id.favicon_view);
                mTitleView = itemView.findViewById(R.id.title_view);
                mUrlView = itemView.findViewById(R.id.url_view);
                mDomainView = itemView.findViewById(R.id.domain_view);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        BookmarkInfo bookmark = mBookmarks.get(position);
                        WebViewActivity.startWebView(BookmarksActivity.this, bookmark.url);
                        finish();
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        BookmarkInfo bookmark = mBookmarks.get(position);
                        showBookmarkMenu(bookmark);
                    }
                    return true;
                });
            }

            void bind(BookmarkInfo bookmark) {
                mTitleView.setText(bookmark.getDisplayTitle());
                mUrlView.setText(bookmark.url);
                mDomainView.setText(bookmark.getDomain());

                // 设置默认图标
                mFaviconView.setImageResource(R.mipmap.ic_launcher);
            }
        }
    }

    /**
     * 启动书签Activity
     */
    public static void startBookmarks(Context context) {
        try {
            Intent intent = new Intent(context, BookmarksActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("BookmarksActivity", "Failed to start BookmarksActivity", e);
        }
    }
}
