package com.hippo.ehviewer.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.util.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import com.google.android.material.tabs.TabLayout;

/**
 * 多标签管理界面
 * Chrome风格的标签页管理器
 */
public class TabsManagerActivity extends AppCompatActivity {
    private static final String TAG = "TabsManager";
    public static final String EXTRA_SELECTED_TAB = "selected_tab";
    public static final String EXTRA_ACTION = "action";
    public static final String ACTION_NEW_TAB = "new_tab";
    public static final String ACTION_NEW_INCOGNITO_TAB = "new_incognito_tab";
    
    // UI控件
    private ImageButton mCloseButton;
    private TextView mTitleText;
    private TextView mTabCountText;
    private ImageButton mNewTabButton;
    private ImageButton mMenuButton;
    
    private TextView mNormalTabsButton;
    private TextView mIncognitoTabsButton;
    
    private RecyclerView mTabsRecyclerView;
    private LinearLayout mEmptyState;
    private LinearLayout mBottomActions;
    
    private TextView mCloseAllButton;
    private TextView mNewIncognitoTabButton;
    
    // 适配器和数据
    private TabsAdapter mTabsAdapter;
    private List<TabInfo> mNormalTabs = new ArrayList<>();
    private List<TabInfo> mIncognitoTabs = new ArrayList<>();
    private boolean mShowingIncognito = false;
    
    // 分组和分类功能
    private TabLayout mCategoryTabs;
    private Spinner mGroupSpinner;
    private String mCurrentCategory = "all";
    private String mCurrentGroup = "";
    private boolean mGroupingMode = false;
    
    // 增强的标签页数据类
    public static class TabInfo {
        public String title = "新标签页";
        public String url = "about:blank";
        public Bitmap preview;
        public Bitmap favicon;
        public boolean isIncognito = false;
        public boolean isActive = false;
        public int tabId;
        public String category = "default";  // 分类
        public String group = "";  // 组
        public int color = 0;  // 标签颜色
        public long createTime = System.currentTimeMillis();
        public long lastVisitTime = System.currentTimeMillis();
        
        public TabInfo(int tabId) {
            this.tabId = tabId;
        }
        
        // 获取域名
        public String getDomain() {
            if (url == null || url.isEmpty() || url.equals("about:blank")) {
                return "";
            }
            try {
                java.net.URL urlObj = new java.net.URL(url);
                return urlObj.getHost();
            } catch (Exception e) {
                return url;
            }
        }
        
        // 获取智能分类
        public String getSmartCategory() {
            String domain = getDomain();
            if (domain.isEmpty()) {
                return "default";
            }
            
            if (domain.contains("youtube.com") || domain.contains("youtu.be") || 
                domain.contains("bilibili.com") || domain.contains("tiktok.com")) {
                return "video";
            } else if (domain.contains("github.com") || domain.contains("stackoverflow.com") ||
                      domain.contains("developer")) {
                return "development";
            } else if (domain.contains("news") || domain.contains("新闻")) {
                return "news";
            } else if (domain.contains("shop") || domain.contains("buy") ||
                      domain.contains("mall") || domain.contains("购物")) {
                return "shopping";
            } else if (domain.contains("social") || domain.contains("facebook") ||
                      domain.contains("twitter") || domain.contains("weibo")) {
                return "social";
            }
            return "default";
        }
    }
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_tabs_manager);
            
            // 初始化UI控件
            initializeViews();
            
            // 设置监听器
            setupListeners();
            
            // 初始化标签页数据
            initializeTabsData();
            
            // 设置RecyclerView
            setupRecyclerView();
            
            // 更新UI
            updateUI();
            
            Log.d(TAG, "TabsManagerActivity created successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating TabsManagerActivity", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }
    
    /**
     * 初始化UI控件
     */
    private void initializeViews() {
        mCloseButton = findViewById(R.id.close_button);
        mTitleText = findViewById(R.id.title);
        mTabCountText = findViewById(R.id.tab_count_text);
        mNewTabButton = findViewById(R.id.new_tab_button);
        mMenuButton = findViewById(R.id.menu_button);
        
        mNormalTabsButton = findViewById(R.id.normal_tabs_button);
        mIncognitoTabsButton = findViewById(R.id.incognito_tabs_button);
        
        mTabsRecyclerView = findViewById(R.id.tabs_recycler_view);
        mEmptyState = findViewById(R.id.empty_state);
        mBottomActions = findViewById(R.id.bottom_actions);
        
        mCloseAllButton = findViewById(R.id.close_all_button);
        mNewIncognitoTabButton = findViewById(R.id.new_incognito_tab_button);
        
        // 初始化分组和分类控件
        mCategoryTabs = findViewById(R.id.category_tabs);
        mGroupSpinner = findViewById(R.id.group_spinner);
        
        // 初始化分类标签
        setupCategoryTabs();
        
        // 初始化分组选择器
        setupGroupSpinner();
        
        Log.d(TAG, "Views initialized");
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 关闭按钮
        mCloseButton.setOnClickListener(v -> finish());
        
        // 新建标签按钮
        mNewTabButton.setOnClickListener(v -> createNewTab(false));
        
        // 菜单按钮
        mMenuButton.setOnClickListener(v -> showTabsMenu());
        
        // 标签类型切换
        mNormalTabsButton.setOnClickListener(v -> switchToNormalTabs());
        mIncognitoTabsButton.setOnClickListener(v -> switchToIncognitoTabs());
        
        // 底部操作
        mCloseAllButton.setOnClickListener(v -> closeAllTabs());
        mNewIncognitoTabButton.setOnClickListener(v -> createNewTab(true));
        
        Log.d(TAG, "Listeners setup completed");
    }
    
    /**
     * 初始化标签页数据（模拟数据，实际应该从WebViewActivity获取）
     */
    private void initializeTabsData() {
        try {
            // 创建默认标签页（模拟）
            TabInfo defaultTab = new TabInfo(1);
            defaultTab.title = "新标签页";
            defaultTab.url = "about:blank";
            defaultTab.isActive = true;
            mNormalTabs.add(defaultTab);
            
            // 可以添加更多模拟数据
            if (false) { // 调试用，可以设置为true来测试多标签
                TabInfo tab2 = new TabInfo(2);
                tab2.title = "Google";
                tab2.url = "https://www.google.com";
                mNormalTabs.add(tab2);
                
                TabInfo incognitoTab = new TabInfo(3);
                incognitoTab.title = "无痕浏览";
                incognitoTab.url = "https://www.example.com";
                incognitoTab.isIncognito = true;
                mIncognitoTabs.add(incognitoTab);
            }
            
            Log.d(TAG, "Tabs data initialized: " + mNormalTabs.size() + " normal tabs, " + 
                mIncognitoTabs.size() + " incognito tabs");
                
        } catch (Exception e) {
            Log.e(TAG, "Error initializing tabs data", e);
        }
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        try {
            // 网格布局，每行2列
            GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
            mTabsRecyclerView.setLayoutManager(layoutManager);
            
            // 创建适配器
            mTabsAdapter = new TabsAdapter(this, getCurrentTabsList());
            mTabsAdapter.setOnTabClickListener(this::onTabClicked);
            mTabsAdapter.setOnTabCloseListener(this::onTabClosed);
            
            mTabsRecyclerView.setAdapter(mTabsAdapter);
            
            Log.d(TAG, "RecyclerView setup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }
    
    /**
     * 获取当前显示的标签列表
     */
    private List<TabInfo> getCurrentTabsList() {
        return mShowingIncognito ? mIncognitoTabs : mNormalTabs;
    }
    
    /**
     * 更新UI
     */
    private void updateUI() {
        try {
            List<TabInfo> currentTabs = getCurrentTabsList();
            int tabCount = currentTabs.size();
            
            // 更新标签数量
            mTabCountText.setText(tabCount + " 个标签");
            
            // 更新标签类型按钮状态
            updateTabTypeButtons();
            
            // 更新空状态
            if (tabCount == 0) {
                mTabsRecyclerView.setVisibility(View.GONE);
                mEmptyState.setVisibility(View.VISIBLE);
                mBottomActions.setVisibility(View.GONE);
            } else {
                mTabsRecyclerView.setVisibility(View.VISIBLE);
                mEmptyState.setVisibility(View.GONE);
                mBottomActions.setVisibility(View.VISIBLE);
            }
            
            // 更新适配器（使用过滤后的数据）
            if (mTabsAdapter != null) {
                updateFilteredTabs(); // 使用过滤系统而不是直接更新
            }
            
            Log.d(TAG, "UI updated: " + tabCount + " tabs, showing incognito: " + mShowingIncognito);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }
    
    /**
     * 更新标签类型按钮状态
     */
    private void updateTabTypeButtons() {
        if (mShowingIncognito) {
            // 显示无痕标签
            mNormalTabsButton.setTextColor(getColor(android.R.color.darker_gray));
            mIncognitoTabsButton.setTextColor(getColor(R.color.colorPrimary));
            mIncognitoTabsButton.setTypeface(null, android.graphics.Typeface.BOLD);
            mNormalTabsButton.setTypeface(null, android.graphics.Typeface.NORMAL);
            mTitleText.setText("无痕标签管理");
        } else {
            // 显示普通标签
            mNormalTabsButton.setTextColor(getColor(R.color.colorPrimary));
            mIncognitoTabsButton.setTextColor(getColor(android.R.color.darker_gray));
            mNormalTabsButton.setTypeface(null, android.graphics.Typeface.BOLD);
            mIncognitoTabsButton.setTypeface(null, android.graphics.Typeface.NORMAL);
            mTitleText.setText("多标签管理");
        }
    }
    
    /**
     * 切换到普通标签
     */
    private void switchToNormalTabs() {
        if (!mShowingIncognito) return;
        
        mShowingIncognito = false;
        updateUI();
        Log.d(TAG, "Switched to normal tabs");
    }
    
    /**
     * 切换到无痕标签
     */
    private void switchToIncognitoTabs() {
        if (mShowingIncognito) return;
        
        mShowingIncognito = true;
        updateUI();
        Log.d(TAG, "Switched to incognito tabs");
    }
    
    /**
     * 创建新标签页
     */
    private void createNewTab(boolean incognito) {
        try {
            int newId = (int)(System.currentTimeMillis() & 0x7FFFFFFF) % 10000;
            TabInfo newTab = new TabInfo((int) newId);
            newTab.isIncognito = incognito;
            
            if (incognito) {
                newTab.title = "新无痕标签";
                mIncognitoTabs.add(newTab);
                if (!mShowingIncognito) {
                    switchToIncognitoTabs();
                }
            } else {
                newTab.title = "新标签页";
                mNormalTabs.add(newTab);
                if (mShowingIncognito) {
                    switchToNormalTabs();
                }
            }
            
            updateUI();
            
            // 返回结果给调用者
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_ACTION, incognito ? ACTION_NEW_INCOGNITO_TAB : ACTION_NEW_TAB);
            setResult(RESULT_OK, resultIntent);
            finish();
            
            Log.d(TAG, "Created new tab: " + (incognito ? "incognito" : "normal"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating new tab", e);
        }
    }
    
    /**
     * 标签页点击事件
     */
    private void onTabClicked(TabInfo tab) {
        try {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SELECTED_TAB, tab.tabId);
            setResult(RESULT_OK, resultIntent);
            finish();
            
            Log.d(TAG, "Tab clicked: " + tab.tabId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling tab click", e);
        }
    }
    
    /**
     * 标签页关闭事件
     */
    private void onTabClosed(TabInfo tab) {
        try {
            List<TabInfo> currentTabs = getCurrentTabsList();
            currentTabs.remove(tab);
            updateUI();
            
            Log.d(TAG, "Tab closed: " + tab.tabId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error closing tab", e);
        }
    }
    
    /**
     * 关闭所有标签页
     */
    private void closeAllTabs() {
        try {
            getCurrentTabsList().clear();
            updateUI();
            
            Log.d(TAG, "All tabs closed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error closing all tabs", e);
        }
    }
    
    /**
     * 显示标签页菜单
     */
    private void showTabsMenu() {
        try {
            // 创建菜单选项
            String[] menuItems = {
                "关闭所有标签",
                "关闭其他标签", 
                "自动智能分组",
                "新建无痕标签",
                "清除所有分组",
                "设置"
            };
            
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("标签页选项");
            
            builder.setItems(menuItems, (dialog, which) -> {
                switch (which) {
                    case 0: // 关闭所有标签
                        closeAllTabs();
                        break;
                    case 1: // 关闭其他标签
                        closeOtherTabs();
                        break;
                    case 2: // 自动智能分组
                        autoGroupSimilarTabs();
                        break;
                    case 3: // 新建无痕标签
                        createNewTab(true);
                        break;
                    case 4: // 清除所有分组
                        clearAllGroups();
                        break;
                    case 5: // 设置
                        // TODO: 打开设置页面
                        break;
                }
            });
            
            builder.setNegativeButton("取消", null);
            builder.show();
            
            Log.d(TAG, "Tabs menu shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing tabs menu", e);
        }
    }
    
    /**
     * 关闭其他标签页
     */
    private void closeOtherTabs() {
        try {
            List<TabInfo> currentTabs = getCurrentTabsList();
            TabInfo activeTab = null;
            
            // 找到活跃的标签页
            for (TabInfo tab : currentTabs) {
                if (tab.isActive) {
                    activeTab = tab;
                    break;
                }
            }
            
            // 清空列表，只保留活跃标签
            currentTabs.clear();
            if (activeTab != null) {
                currentTabs.add(activeTab);
            }
            
            updateUI();
            
            Log.d(TAG, "Other tabs closed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error closing other tabs", e);
        }
    }
    
    /**
     * 初始化分类标签
     */
    private void setupCategoryTabs() {
        try {
            mCategoryTabs.removeAllTabs();
            
            // 添加基础分类
            mCategoryTabs.addTab(mCategoryTabs.newTab().setText("全部").setTag("all"));
            mCategoryTabs.addTab(mCategoryTabs.newTab().setText("默认").setTag("default"));
            mCategoryTabs.addTab(mCategoryTabs.newTab().setText("视频").setTag("video"));
            mCategoryTabs.addTab(mCategoryTabs.newTab().setText("开发").setTag("development"));
            mCategoryTabs.addTab(mCategoryTabs.newTab().setText("新闻").setTag("news"));
            mCategoryTabs.addTab(mCategoryTabs.newTab().setText("购物").setTag("shopping"));
            mCategoryTabs.addTab(mCategoryTabs.newTab().setText("社交").setTag("social"));
            
            // 设置选中监听器
            mCategoryTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    mCurrentCategory = (String) tab.getTag();
                    updateFilteredTabs();
                }
                
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}
                
                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
            
            Log.d(TAG, "Category tabs setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up category tabs", e);
        }
    }
    
    /**
     * 初始化分组选择器
     */
    private void setupGroupSpinner() {
        try {
            List<String> groups = new ArrayList<>();
            groups.add("全部分组");
            
            // 获取所有分组
            List<TabInfo> currentTabs = getCurrentTabsList();
            Map<String, Integer> groupCounts = new HashMap<>();
            for (TabInfo tab : currentTabs) {
                if (tab.group != null && !tab.group.isEmpty()) {
                    groupCounts.put(tab.group, groupCounts.getOrDefault(tab.group, 0) + 1);
                }
            }
            
            for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) {
                groups.add(entry.getKey() + " (" + entry.getValue() + ")");
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, groups);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mGroupSpinner.setAdapter(adapter);
            
            // 设置选中监听器
            mGroupSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (position == 0) {
                        mCurrentGroup = "";
                    } else {
                        String selectedItem = (String) parent.getItemAtPosition(position);
                        mCurrentGroup = selectedItem.split(" ")[0]; // 只取分组名，去除数量
                    }
                    updateFilteredTabs();
                }
                
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            
            Log.d(TAG, "Group spinner setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up group spinner", e);
        }
    }
    
    /**
     * 根据分类和分组过滤标签页
     */
    private void updateFilteredTabs() {
        try {
            List<TabInfo> allTabs = getCurrentTabsList();
            List<TabInfo> filteredTabs = new ArrayList<>();
            
            for (TabInfo tab : allTabs) {
                boolean matchCategory = true;
                boolean matchGroup = true;
                
                // 分类过滤
                if (!"all".equals(mCurrentCategory)) {
                    String tabCategory = tab.category != null ? tab.category : tab.getSmartCategory();
                    matchCategory = mCurrentCategory.equals(tabCategory);
                }
                
                // 分组过滤
                if (!mCurrentGroup.isEmpty()) {
                    matchGroup = mCurrentGroup.equals(tab.group);
                }
                
                if (matchCategory && matchGroup) {
                    filteredTabs.add(tab);
                }
            }
            
            if (mTabsAdapter != null) {
                mTabsAdapter.updateTabs(filteredTabs);
            }
            
            // 更新空状态
            if (filteredTabs.isEmpty()) {
                mTabsRecyclerView.setVisibility(View.GONE);
                mEmptyState.setVisibility(View.VISIBLE);
                mBottomActions.setVisibility(View.GONE);
            } else {
                mTabsRecyclerView.setVisibility(View.VISIBLE);
                mEmptyState.setVisibility(View.GONE);
                mBottomActions.setVisibility(View.VISIBLE);
            }
            
            Log.d(TAG, "Filtered tabs: " + filteredTabs.size() + " / " + allTabs.size());
        } catch (Exception e) {
            Log.e(TAG, "Error filtering tabs", e);
        }
    }
    
    /**
     * 自动分组相似标签页
     */
    private void autoGroupSimilarTabs() {
        try {
            List<TabInfo> currentTabs = getCurrentTabsList();
            Map<String, List<TabInfo>> domainMap = new HashMap<>();
            
            // 按域名分组
            for (TabInfo tab : currentTabs) {
                String domain = tab.getDomain();
                if (!domain.isEmpty()) {
                    domainMap.computeIfAbsent(domain, k -> new ArrayList<>()).add(tab);
                }
            }
            
            // 为有多个标签页的域名创建分组
            for (Map.Entry<String, List<TabInfo>> entry : domainMap.entrySet()) {
                List<TabInfo> tabs = entry.getValue();
                if (tabs.size() >= 2) {
                    String groupName = entry.getKey();
                    for (TabInfo tab : tabs) {
                        tab.group = groupName;
                    }
                    Log.d(TAG, "Auto-grouped " + tabs.size() + " tabs under '" + groupName + "'");
                }
            }
            
            // 重新初始化分组选择器和更新UI
            setupGroupSpinner();
            updateFilteredTabs();
            
        } catch (Exception e) {
            Log.e(TAG, "Error auto-grouping tabs", e);
        }
    }
    
    /**
     * 清除所有分组
     */
    private void clearAllGroups() {
        try {
            List<TabInfo> currentTabs = getCurrentTabsList();
            for (TabInfo tab : currentTabs) {
                tab.group = "";
            }
            
            // 重新初始化分组选择器和更新UI
            setupGroupSpinner();
            updateFilteredTabs();
            
            Log.d(TAG, "All groups cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing groups", e);
        }
    }
    
    /**
     * 静态方法启动TabsManagerActivity
     */
    public static void startTabsManager(Context context) {
        Intent intent = new Intent(context, TabsManagerActivity.class);
        context.startActivity(intent);
    }
}