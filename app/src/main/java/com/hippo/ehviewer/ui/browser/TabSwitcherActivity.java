package com.hippo.ehviewer.ui.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hippo.ehviewer.R;

import java.util.List;

public class TabSwitcherActivity extends AppCompatActivity {
    
    public static final String EXTRA_SELECTED_TAB = "selected_tab";
    public static final String EXTRA_ACTION = "action";
    public static final String ACTION_SWITCH = "switch";
    public static final String ACTION_NEW_TAB = "new_tab";
    public static final String ACTION_CLOSE_ALL = "close_all";
    
    private RecyclerView tabRecycler;
    private TabAdapter tabAdapter;
    private TabManager tabManager;
    private LinearLayout emptyState;
    private Chip chipIncognito;
    private ImageButton btnCloseAll;
    private FloatingActionButton fabNewTab;
    private Button btnNewTabEmpty;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser_tab_switcher);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        loadTabs();
    }
    
    private void initViews() {
        tabRecycler = findViewById(R.id.tab_recycler);
        emptyState = findViewById(R.id.empty_state);
        chipIncognito = findViewById(R.id.chip_incognito);
        btnCloseAll = findViewById(R.id.btn_close_all);
        fabNewTab = findViewById(R.id.fab_new_tab);
        btnNewTabEmpty = findViewById(R.id.btn_new_tab_empty);
        
        tabManager = TabManager.getInstance(this);
    }
    
    private void setupRecyclerView() {
        // 使用网格布局，每行2个标签页
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        tabRecycler.setLayoutManager(layoutManager);
        
        tabAdapter = new TabAdapter(this);
        tabAdapter.setOnTabClickListener(new TabAdapter.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                // 切换到选中的标签页
                Intent result = new Intent();
                result.putExtra(EXTRA_SELECTED_TAB, position);
                result.putExtra(EXTRA_ACTION, ACTION_SWITCH);
                setResult(RESULT_OK, result);
                finish();
            }
            
            @Override
            public void onTabClose(int position) {
                closeTab(position);
            }
            
            @Override
            public void onTabLongClick(int position) {
                showTabOptions(position);
            }
        });
        
        tabRecycler.setAdapter(tabAdapter);
    }
    
    private void setupListeners() {
        // 隐私模式切换
        chipIncognito.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tabManager.setIncognitoMode(isChecked);
            updateChipAppearance(isChecked);
            loadTabs();
        });
        
        // 关闭所有标签页
        btnCloseAll.setOnClickListener(v -> {
            if (tabManager.getTabCount() > 0) {
                new AlertDialog.Builder(this)
                    .setTitle("关闭所有标签页")
                    .setMessage("确定要关闭所有" + (tabManager.isIncognitoMode() ? "隐私" : "普通") + "标签页吗？")
                    .setPositiveButton("关闭", (dialog, which) -> closeAllTabs())
                    .setNegativeButton("取消", null)
                    .show();
            }
        });
        
        // 新建标签页
        fabNewTab.setOnClickListener(v -> createNewTab());
        btnNewTabEmpty.setOnClickListener(v -> createNewTab());
    }
    
    private void updateChipAppearance(boolean isIncognito) {
        if (isIncognito) {
            chipIncognito.setChipBackgroundColorResource(R.color.incognito_accent);
            chipIncognito.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            chipIncognito.setChipBackgroundColorResource(R.color.chip_background);
            chipIncognito.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }
    
    private void loadTabs() {
        List<TabAdapter.BrowserTab> tabs = tabManager.getAllTabs();
        tabAdapter.setTabs(tabs);
        tabAdapter.setSelectedPosition(tabManager.getCurrentIndex());
        
        // 显示或隐藏空状态
        if (tabs.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            tabRecycler.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            tabRecycler.setVisibility(View.VISIBLE);
        }
    }
    
    private void closeTab(int position) {
        boolean success = tabManager.closeTab(position, tabManager.isIncognitoMode());
        
        if (success) {
            tabAdapter.removeTab(position);
            
            // 检查是否还有标签页
            if (tabManager.getTabCount() == 0) {
                emptyState.setVisibility(View.VISIBLE);
                tabRecycler.setVisibility(View.GONE);
            }
            
            Toast.makeText(this, "标签页已关闭", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void closeAllTabs() {
        tabManager.closeAllTabs(tabManager.isIncognitoMode());
        loadTabs();
        Toast.makeText(this, "所有标签页已关闭", Toast.LENGTH_SHORT).show();
    }
    
    private void createNewTab() {
        Intent result = new Intent();
        result.putExtra(EXTRA_ACTION, ACTION_NEW_TAB);
        setResult(RESULT_OK, result);
        finish();
    }
    
    private void showTabOptions(int position) {
        String[] options = {"在新标签页中打开", "复制链接", "关闭其他标签页", "关闭"};
        
        new AlertDialog.Builder(this)
            .setTitle("标签页选项")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 在新标签页中打开
                        duplicateTab(position);
                        break;
                    case 1: // 复制链接
                        copyTabUrl(position);
                        break;
                    case 2: // 关闭其他标签页
                        closeOtherTabs(position);
                        break;
                    case 3: // 关闭
                        closeTab(position);
                        break;
                }
            })
            .show();
    }
    
    private void duplicateTab(int position) {
        List<TabAdapter.BrowserTab> tabs = tabManager.getAllTabs();
        if (position >= 0 && position < tabs.size()) {
            TabAdapter.BrowserTab originalTab = tabs.get(position);
            TabAdapter.BrowserTab newTab = tabManager.createNewTab(tabManager.isIncognitoMode());
            if (newTab != null) {
                newTab.setUrl(originalTab.getUrl());
                newTab.setTitle(originalTab.getTitle());
                loadTabs();
                Toast.makeText(this, "已在新标签页中打开", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void copyTabUrl(int position) {
        List<TabAdapter.BrowserTab> tabs = tabManager.getAllTabs();
        if (position >= 0 && position < tabs.size()) {
            String url = tabs.get(position).getUrl();
            
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = 
                android.content.ClipData.newPlainText("URL", url);
            clipboard.setPrimaryClip(clip);
            
            Toast.makeText(this, "链接已复制", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void closeOtherTabs(int position) {
        List<TabAdapter.BrowserTab> tabs = tabManager.getAllTabs();
        
        // 从后往前删除，避免索引问题
        for (int i = tabs.size() - 1; i >= 0; i--) {
            if (i != position) {
                tabManager.closeTab(i, tabManager.isIncognitoMode());
            }
        }
        
        // 切换到保留的标签页
        tabManager.switchToTab(0, tabManager.isIncognitoMode());
        
        loadTabs();
        Toast.makeText(this, "已关闭其他标签页", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}