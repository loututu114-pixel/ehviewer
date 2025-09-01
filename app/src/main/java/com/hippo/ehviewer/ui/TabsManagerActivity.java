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

/**
 * 多标签管理界面
 * 简洁的Chrome风格标签页管理器
 * 支持无痕和正常模式切换，移除复杂的分组功能
 *
 * 主要特性：
 * - 网格布局显示标签页
 * - 无痕/正常模式切换
 * - 简洁的关闭和新建功能
 * - 类似Chrome的视觉风格
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

    // 简化的标签页数据类
    public static class TabInfo {
        public String title = "新标签页";
        public String url = "about:blank";
        public Bitmap preview;
        public Bitmap favicon;
        public boolean isIncognito = false;
        public boolean isActive = false;
        public int tabId;
        public long createTime = System.currentTimeMillis();

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

        mNormalTabsButton = findViewById(R.id.normal_tabs_button);
        mIncognitoTabsButton = findViewById(R.id.incognito_tabs_button);

        mTabsRecyclerView = findViewById(R.id.tabs_recycler_view);
        mEmptyState = findViewById(R.id.empty_state);
        mBottomActions = findViewById(R.id.bottom_actions);

        mCloseAllButton = findViewById(R.id.close_all_button);
        mNewIncognitoTabButton = findViewById(R.id.new_incognito_tab_button);

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

            // 更新适配器
            if (mTabsAdapter != null) {
                mTabsAdapter.updateTabs(currentTabs);
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
     * 静态方法启动TabsManagerActivity
     */
    public static void startTabsManager(Context context) {
        Intent intent = new Intent(context, TabsManagerActivity.class);
        context.startActivity(intent);
    }
}