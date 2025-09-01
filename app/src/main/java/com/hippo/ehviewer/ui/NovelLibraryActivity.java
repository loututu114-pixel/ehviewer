package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.NovelLibraryManager;
import com.hippo.ehviewer.util.EroNovelDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * 小说书库Activity
 * 显示和管理收藏的小说
 */
public class NovelLibraryActivity extends AppCompatActivity {

    private static final String TAG = "NovelLibraryActivity";

    private RecyclerView recyclerView;
    private TextView emptyText;
    private ImageButton btnBack;
    private ImageButton btnSort;

    private NovelLibraryManager libraryManager;
    private NovelAdapter adapter;
    private List<EroNovelDetector.NovelInfo> novelList;
    private boolean showEroOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_library);

        libraryManager = NovelLibraryManager.getInstance(this);

        initViews();
        loadNovels();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        emptyText = findViewById(R.id.empty_text);
        btnBack = findViewById(R.id.btn_back);
        btnSort = findViewById(R.id.btn_sort);

        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        novelList = new ArrayList<>();
        adapter = new NovelAdapter();
        recyclerView.setAdapter(adapter);

        // 设置按钮监听器
        btnBack.setOnClickListener(v -> finish());
        btnSort.setOnClickListener(v -> toggleSortMode());
    }

    private void loadNovels() {
        novelList.clear();

        if (showEroOnly) {
            novelList.addAll(libraryManager.getEroNovels());
        } else {
            novelList.addAll(libraryManager.getAllNovels());
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (novelList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(showEroOnly ? "暂无收藏的色情小说" : "暂无收藏的小说");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    private void toggleSortMode() {
        showEroOnly = !showEroOnly;
        loadNovels();

        String modeText = showEroOnly ? "色情小说" : "全部小说";
        Toast.makeText(this, "已切换到: " + modeText, Toast.LENGTH_SHORT).show();
    }

    /**
     * 小说适配器
     */
    private class NovelAdapter extends RecyclerView.Adapter<NovelViewHolder> {

        @NonNull
        @Override
        public NovelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_novel, parent, false);
            return new NovelViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NovelViewHolder holder, int position) {
            EroNovelDetector.NovelInfo novel = novelList.get(position);
            holder.bind(novel);
        }

        @Override
        public int getItemCount() {
            return novelList.size();
        }
    }

    /**
     * 小说ViewHolder
     */
    private class NovelViewHolder extends RecyclerView.ViewHolder {
        private TextView titleText;
        private TextView authorText;
        private TextView descriptionText;
        private TextView progressText;
        private TextView chapterCountText;
        private ImageButton btnRead;
        private ImageButton btnDelete;

        public NovelViewHolder(@NonNull View itemView) {
            super(itemView);

            titleText = itemView.findViewById(R.id.title_text);
            authorText = itemView.findViewById(R.id.author_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            progressText = itemView.findViewById(R.id.progress_text);
            chapterCountText = itemView.findViewById(R.id.chapter_count_text);
            btnRead = itemView.findViewById(R.id.btn_read);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(EroNovelDetector.NovelInfo novel) {
            // 设置基本信息
            titleText.setText(novel.title);
            authorText.setText("作者: " + (novel.author.isEmpty() ? "未知" : novel.author));
            descriptionText.setText(novel.description.isEmpty() ? "暂无简介" : novel.description);

            // 设置进度
            progressText.setText("阅读进度: " + novel.readProgress + "%");

            // 设置章节数量
            chapterCountText.setText("章节: " + novel.chapters.size());

            // 设置点击监听器
            btnRead.setOnClickListener(v -> openNovelReader(novel));
            btnDelete.setOnClickListener(v -> deleteNovel(novel));

            // 设置整个item的点击监听器
            itemView.setOnClickListener(v -> openNovelReader(novel));
        }

        private void openNovelReader(EroNovelDetector.NovelInfo novel) {
            Intent intent = new Intent(NovelLibraryActivity.this, NovelReaderActivity.class);
            intent.putExtra(NovelReaderActivity.EXTRA_NOVEL_URL, novel.url);
            intent.putExtra(NovelReaderActivity.EXTRA_NOVEL_TITLE, novel.title);
            // 这里可以传递小说内容，如果有的话
            startActivity(intent);
        }

        private void deleteNovel(EroNovelDetector.NovelInfo novel) {
            // 显示确认对话框
            new androidx.appcompat.app.AlertDialog.Builder(NovelLibraryActivity.this)
                    .setTitle("删除确认")
                    .setMessage("确定要删除小说《" + novel.title + "》吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        if (libraryManager.deleteNovel(novel.url)) {
                            int position = novelList.indexOf(novel);
                            if (position >= 0) {
                                novelList.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateEmptyState();
                            }
                            Toast.makeText(NovelLibraryActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(NovelLibraryActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新加载数据，以防有变化
        loadNovels();
    }
}
