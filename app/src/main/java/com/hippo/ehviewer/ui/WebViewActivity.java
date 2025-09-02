package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.BookmarkManager;
import com.hippo.ehviewer.client.data.BookmarkInfo;
import com.hippo.ehviewer.client.AdBlockManager;
import com.hippo.ehviewer.client.SearchConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 简化的WebView浏览器Activity
 * 移除了历史记录和标签管理功能，只保留基本的浏览功能
 */
public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "WebViewActivity";

    // 常量定义
    public static final String EXTRA_URL = "url";

    // UI组件
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WebView mWebView;
    private EditText mAddressBar;
    private ImageButton mGoButton;
    private ImageButton mHomeButton;
    private ImageButton mHistoryButton;
    private ImageButton mBookmarkButton;
    private ImageButton mTabsButton;
    private ImageButton mSettingsButton;
    private ProgressBar mProgressBar;

    // 管理器
    private BookmarkManager mBookmarkManager;
    private AdBlockManager mAdBlockManager;
    private SearchConfigManager mSearchConfigManager;

    // 状态变量
    private boolean isDesktopMode = false;
    private String currentUrl = "";

    // 多窗口管理
    private List<BrowserTab> mTabs = new ArrayList<>();
    private int mCurrentTabIndex = 0;

    /**
     * 浏览器窗口类
     */
    class BrowserTab {
        String title;
        String url;
        WebView webView;
        boolean isActive;

        BrowserTab(String title, String url, WebView webView) {
            this.title = title;
            this.url = url;
            this.webView = webView;
            this.isActive = false;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_web_view);

        // 初始化管理器
            mBookmarkManager = BookmarkManager.getInstance(this);
            mAdBlockManager = AdBlockManager.getInstance();
        mSearchConfigManager = SearchConfigManager.getInstance(this);

        // 初始化搜索配置管理器
        mSearchConfigManager.initialize();

        // 初始化UI
            initializeViews();

        // 设置WebView
        setupWebView();

        // 设置监听器
        setupListeners();

        // 处理初始URL
        handleInitialUrl();
    }

    /**
     * 初始化UI控件
     */
    private void initializeViews() {
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mWebView = findViewById(R.id.web_view);
        mAddressBar = findViewById(R.id.address_bar);
        mGoButton = findViewById(R.id.go_button);
        mHomeButton = findViewById(R.id.home_button);
        mHistoryButton = findViewById(R.id.history_button);
        mBookmarkButton = findViewById(R.id.bookmark_button);
            mTabsButton = findViewById(R.id.tabs_button);
        mSettingsButton = findViewById(R.id.settings_button);
        mProgressBar = findViewById(R.id.progress_bar);
    }

    /**
     * 设置WebView
     */
    private void setupWebView() {
        if (mWebView == null) return;

        // 创建第一个标签页
        createNewTab("新标签页", "about:blank", true);

        WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true);
        // App cache is deprecated in newer Android versions
        // webSettings.setAppCacheEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                currentUrl = url;
                updateAddressBar(url);
                showProgress(true);
            }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                currentUrl = url;
                updateAddressBar(url);
                updateBookmarkButton();
                showProgress(false);

                // 更新当前标签页的信息
                updateCurrentTabInfo(url, view.getTitle());
                }

                @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false; // 在WebView中加载
                } else {
                    // 处理其他scheme
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                } catch (Exception e) {
                        Toast.makeText(WebViewActivity.this,
                            "无法处理链接: " + url, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (mProgressBar != null) {
                    mProgressBar.setProgress(newProgress);
                }
                }

                @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // 更新标签页标题
                updateCurrentTabInfo(view.getUrl(), title);
            }
        });
    }

    /**
     * 创建新标签页
     */
    private void createNewTab(String title, String url, boolean setActive) {
        WebView newWebView = new WebView(this);
        setupWebViewSettings(newWebView);

        BrowserTab newTab = new BrowserTab(title, url, newWebView);
        mTabs.add(newTab);

        if (setActive) {
            switchToTab(mTabs.size() - 1);
        }
    }

    /**
     * 设置WebView的基本设置
     */
    private void setupWebViewSettings(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
    }

    /**
     * 切换到指定标签页
     */
    private void switchToTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= mTabs.size()) return;

        // 隐藏当前WebView
        if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
            mTabs.get(mCurrentTabIndex).isActive = false;
            mSwipeRefreshLayout.removeView(mTabs.get(mCurrentTabIndex).webView);
        }

        // 显示新WebView
        mCurrentTabIndex = tabIndex;
        BrowserTab activeTab = mTabs.get(tabIndex);
        activeTab.isActive = true;
        mWebView = activeTab.webView;
        mSwipeRefreshLayout.addView(mWebView, 0);

        // 更新UI
        updateAddressBar(activeTab.url);
        currentUrl = activeTab.url;
        updateBookmarkButton();
    }

    /**
     * 更新当前标签页信息
     */
    private void updateCurrentTabInfo(String url, String title) {
        if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
            BrowserTab tab = mTabs.get(mCurrentTabIndex);
            tab.url = url != null ? url : tab.url;
            tab.title = title != null && !title.isEmpty() ? title : tab.title;
        }
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 地址栏
        if (mAddressBar != null) {
            mAddressBar.setOnEditorActionListener((v, actionId, event) -> {
                String input = mAddressBar.getText().toString().trim();
                if (!input.isEmpty()) {
                    // 使用SearchConfigManager处理输入
                    String processedUrl = mSearchConfigManager.processInput(input);
                    loadUrl(processedUrl);
                    }
                    return true;
            });
        }

        // 搜索/访问按钮
        if (mGoButton != null) {
            mGoButton.setOnClickListener(v -> {
                String input = mAddressBar.getText().toString().trim();
                if (!input.isEmpty()) {
                    // 使用SearchConfigManager处理输入
                    String processedUrl = mSearchConfigManager.processInput(input);
                    loadUrl(processedUrl);
                    // 隐藏软键盘
                        android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                        imm.hideSoftInputFromWindow(mAddressBar.getWindowToken(), 0);
                        }
                    }
                });
        }

        // 主页按钮
        if (mHomeButton != null) {
            mHomeButton.setOnClickListener(v -> {
                String homepageUrl = "https://www.google.com";
                if (mSearchConfigManager != null && mSearchConfigManager.isHomepageEnabled()) {
                    homepageUrl = mSearchConfigManager.getDefaultHomepageUrl();
                    android.util.Log.d(TAG, "Home button clicked, using configured homepage: " + homepageUrl);
            } else {
                    android.util.Log.d(TAG, "Home button clicked, using default homepage: " + homepageUrl);
                }
                loadUrl(homepageUrl);
            });
        }

        // 历史按钮
            if (mHistoryButton != null) {
            mHistoryButton.setOnClickListener(v -> showHistory());
        }

        // 书签按钮
        if (mBookmarkButton != null) {
            mBookmarkButton.setOnClickListener(v -> showBookmarks());
        }

        // 多窗口按钮
        if (mTabsButton != null) {
            mTabsButton.setOnClickListener(v -> showTabs());
        }

        // 设置按钮
        if (mSettingsButton != null) {
            mSettingsButton.setOnClickListener(v -> showSettings());
        }

        // 下拉刷新
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setOnRefreshListener(() -> {
                if (mWebView != null) {
                    mWebView.reload();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    /**
     * 处理初始URL
     */
    private void handleInitialUrl() {
        Intent intent = getIntent();
        String url = null;

        if (intent != null) {
            Uri data = intent.getData();
            if (data != null) {
                url = data.toString();
            }

            // 检查是否有传递的URL参数
            String urlParam = intent.getStringExtra(EXTRA_URL);
            if (urlParam != null && !urlParam.isEmpty()) {
                url = urlParam;
            }
        }

        if (url == null || url.isEmpty()) {
            // 使用配置的默认首页
            if (mSearchConfigManager != null && mSearchConfigManager.isHomepageEnabled()) {
                url = mSearchConfigManager.getDefaultHomepageUrl();
                android.util.Log.d(TAG, "Using configured homepage: " + url);
                            } else {
                url = "https://www.google.com";
                android.util.Log.d(TAG, "Using default homepage: " + url);
            }
        }

        loadUrl(url);
    }

    /**
     * 加载URL
     */
    private void loadUrl(String url) {
        if (url == null || url.isEmpty()) return;

        // 如果当前标签页的WebView为空，创建一个新的
        if (mWebView == null) {
            if (!mTabs.isEmpty() && mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
                mWebView = mTabs.get(mCurrentTabIndex).webView;
            }
        }

        if (mWebView == null) return;

        // URL已经被SearchConfigManager处理过，直接加载
        mWebView.loadUrl(url);

        // 更新当前标签页的URL
        if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
            mTabs.get(mCurrentTabIndex).url = url;
        }
    }

    /**
     * 更新地址栏
     */
    private void updateAddressBar(String url) {
        if (mAddressBar != null && url != null) {
            mAddressBar.setText(url);
        }
    }


    
    /**
     * 更新书签按钮状态
     */
    private void updateBookmarkButton() {
        if (mBookmarkManager == null || mBookmarkButton == null || currentUrl == null) return;

        boolean isBookmarked = mBookmarkManager.isBookmarked(currentUrl);
        // 可以在这里更新书签按钮的图标状态
    }

    /**
     * 切换书签状态
     */
    private void toggleBookmark() {
        if (mBookmarkManager == null || currentUrl == null) return;

        try {
            if (mBookmarkManager.isBookmarked(currentUrl)) {
                // 移除书签
                BookmarkInfo bookmark = mBookmarkManager.findBookmarkByUrl(currentUrl);
                if (bookmark != null) {
                    boolean result = mBookmarkManager.deleteBookmark(bookmark.id);
                    if (result) {
                        Toast.makeText(this, "已从书签中移除", Toast.LENGTH_SHORT).show();
                } else {
                        Toast.makeText(this, "移除书签失败", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                // 添加书签
                String title = currentUrl;
                if (mWebView != null) {
                    String pageTitle = mWebView.getTitle();
                    if (pageTitle != null && !pageTitle.isEmpty()) {
                        title = pageTitle;
                    }
                }

                long result = mBookmarkManager.addBookmark(title, currentUrl);
                if (result > 0) {
                    Toast.makeText(this, "已添加到书签", Toast.LENGTH_SHORT).show();
            } else {
                    Toast.makeText(this, "添加书签失败", Toast.LENGTH_SHORT).show();
                }
            }
            updateBookmarkButton();
        } catch (Exception e) {
            Toast.makeText(this, "书签操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示历史记录
     */
    private void showHistory() {
        if (mWebView == null) {
            android.widget.Toast.makeText(this, "WebView未初始化", android.widget.Toast.LENGTH_SHORT).show();
                            return;
        }

        // 尝试刷新历史记录
        mWebView.loadUrl("javascript:void(0)");

        android.webkit.WebBackForwardList historyList = mWebView.copyBackForwardList();
        if (historyList == null) {
            android.widget.Toast.makeText(this, "无法获取历史记录", android.widget.Toast.LENGTH_SHORT).show();
                            return;
        }

        int size = historyList.getSize();
        android.util.Log.d(TAG, "历史记录大小: " + size);

        // 显示历史记录对话框，即使只有一个页面也显示
        java.util.List<String> historyTitles = new java.util.ArrayList<>();
        java.util.List<String> historyUrls = new java.util.ArrayList<>();
        java.util.List<String> historyDetails = new java.util.ArrayList<>();

        for (int i = 0; i < size; i++) {
            android.webkit.WebHistoryItem item = historyList.getItemAtIndex(i);
            if (item != null) {
                String title = item.getTitle();
                String url = item.getUrl();

                if (title != null && !title.isEmpty()) {
                    historyTitles.add(title);
                    historyDetails.add("📄 " + title + "\n🌐 " + (url != null ? url : "未知地址"));
                } else {
                    String displayTitle = url != null ? url : "未知页面";
                    historyTitles.add(displayTitle);
                    historyDetails.add("🌐 " + displayTitle);
                }
                historyUrls.add(url != null ? url : "");

                android.util.Log.d(TAG, "历史项目 " + i + ": " + title + " - " + url);
            }
        }

        if (historyTitles.isEmpty()) {
            android.widget.Toast.makeText(this, "历史记录为空", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

        // 显示历史记录对话框
        new android.app.AlertDialog.Builder(this)
            .setTitle("历史记录 (" + size + "项)")
            .setItems(historyTitles.toArray(new String[0]), (dialog, which) -> {
                String selectedUrl = historyUrls.get(which);
                if (!selectedUrl.isEmpty()) {
                    android.widget.Toast.makeText(this, "正在跳转到: " + historyTitles.get(which), android.widget.Toast.LENGTH_SHORT).show();
                    loadUrl(selectedUrl);
                }
            })
            .setPositiveButton("清除历史", (dialog, which) -> {
                clearHistory();
            })
            .setNeutralButton("查看详情", (dialog, which) -> {
                // 显示第一个历史项目的详情
                if (!historyDetails.isEmpty()) {
                    android.widget.Toast.makeText(this, historyDetails.get(0), android.widget.Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("关闭", null)
                        .show();
    }

    /**
     * 显示书签管理
     */
    private void showBookmarks() {
        showBookmarksPage(0, 30); // 显示第一页，每页30项
    }

    /**
     * 显示书签分页
     */
    private void showBookmarksPage(int page, int pageSize) {
        java.util.List<com.hippo.ehviewer.client.data.BookmarkInfo> allBookmarks = mBookmarkManager.getAllBookmarks();
        int totalBookmarks = allBookmarks.size();
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalBookmarks);

        if (totalBookmarks == 0) {
            android.widget.Toast.makeText(this, "暂无书签", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前页的书签
        java.util.List<com.hippo.ehviewer.client.data.BookmarkInfo> pageBookmarks =
            allBookmarks.subList(startIndex, endIndex);

        // 构建书签选项
        java.util.List<String> bookmarkOptions = new java.util.ArrayList<>();
        for (com.hippo.ehviewer.client.data.BookmarkInfo bookmark : pageBookmarks) {
            bookmarkOptions.add("⭐ " + bookmark.title);
        }

        // 添加操作选项
        boolean hasMore = endIndex < totalBookmarks;
        if (hasMore) {
            bookmarkOptions.add("⬇️ 加载更多 (" + (totalBookmarks - endIndex) + "项)");
        }
        bookmarkOptions.add("➕ 添加当前页面");
        bookmarkOptions.add("🗑️ 清空所有书签");

        String[] options = bookmarkOptions.toArray(new String[0]);
        final int nextPage = page + 1;
        final int pageSizeFinal = pageSize;

        new android.app.AlertDialog.Builder(this)
            .setTitle(String.format("书签管理 (%d-%d/%d)",
                startIndex + 1, endIndex, totalBookmarks))
            .setItems(options, (dialog, which) -> {
                if (which < pageBookmarks.size()) {
                    // 选择书签
                    com.hippo.ehviewer.client.data.BookmarkInfo selectedBookmark = pageBookmarks.get(which);
                    loadUrl(selectedBookmark.url);
                    android.widget.Toast.makeText(this, "正在跳转: " + selectedBookmark.title, android.widget.Toast.LENGTH_SHORT).show();
                } else if (which == pageBookmarks.size() && hasMore) {
                    // 加载更多
                    showBookmarksPage(nextPage, pageSizeFinal);
                } else if (which == pageBookmarks.size() + (hasMore ? 1 : 0)) {
                    // 添加当前页面
                    addCurrentPageToBookmarks();
                } else {
                    // 清空书签
                    clearAllBookmarks();
                }
            })
            .setPositiveButton("书签统计", (dialog, which) -> {
                showBookmarkStats();
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    /**
     * 显示书签统计信息
     */
    private void showBookmarkStats() {
        java.util.List<com.hippo.ehviewer.client.data.BookmarkInfo> bookmarks = mBookmarkManager.getAllBookmarks();
        int totalCount = bookmarks.size();

        if (totalCount == 0) {
            android.widget.Toast.makeText(this, "暂无书签统计信息", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

        // 统计访问信息
        int totalVisits = 0;
        long oldestTime = Long.MAX_VALUE;
        long newestTime = 0;

        for (com.hippo.ehviewer.client.data.BookmarkInfo bookmark : bookmarks) {
            totalVisits += bookmark.visitCount;
            oldestTime = Math.min(oldestTime, bookmark.createTime);
            newestTime = Math.max(newestTime, bookmark.lastVisitTime);
        }

        StringBuilder stats = new StringBuilder();
        stats.append("📊 书签统计\n\n");
        stats.append("📝 总数: ").append(totalCount).append(" 个\n");
        stats.append("👁️ 总访问: ").append(totalVisits).append(" 次\n");
        stats.append("📅 创建最早: ").append(formatTime(oldestTime)).append("\n");
        stats.append("🔄 最近访问: ").append(formatTime(newestTime)).append("\n");
        stats.append("📈 平均访问: ").append(String.format("%.1f", (float)totalVisits / totalCount)).append(" 次/书签");

        new android.app.AlertDialog.Builder(this)
            .setTitle("书签统计")
            .setMessage(stats.toString())
            .setPositiveButton("确定", null)
            .show();
    }

    /**
     * 格式化时间显示
     */
    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "未知";

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

        /**
     * 显示多窗口管理
     */
    private void showTabs() {
        if (mTabs.isEmpty()) {
            android.widget.Toast.makeText(this, "暂无标签页", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建标签页列表
        java.util.List<String> tabTitles = new java.util.ArrayList<>();
        for (int i = 0; i < mTabs.size(); i++) {
            BrowserTab tab = mTabs.get(i);
            String indicator = (i == mCurrentTabIndex) ? "● " : "○ ";
            String title = tab.title != null && !tab.title.isEmpty() ? tab.title : "新标签页";
            tabTitles.add(indicator + title);
        }

        // 添加操作选项
        tabTitles.add("➕ 新建标签页");
        tabTitles.add("❌ 关闭当前标签页");

        String[] options = tabTitles.toArray(new String[0]);

        new android.app.AlertDialog.Builder(this)
            .setTitle("标签页管理 (" + mTabs.size() + "个)")
            .setItems(options, (dialog, which) -> {
                if (which < mTabs.size()) {
                    // 切换到指定标签页
                    switchToTab(which);
                    android.widget.Toast.makeText(this, "已切换到标签页", android.widget.Toast.LENGTH_SHORT).show();
                } else if (which == mTabs.size()) {
                    // 新建标签页
                    createNewTab("新标签页", "about:blank", true);
                    android.widget.Toast.makeText(this, "已创建新标签页", android.widget.Toast.LENGTH_SHORT).show();
                } else if (which == mTabs.size() + 1) {
                    // 关闭当前标签页
                    closeCurrentTab();
                }
            })
            .setPositiveButton("标签页详情", (dialog, which) -> {
                showTabDetails();
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    /**
     * 关闭当前标签页
     */
    private void closeCurrentTab() {
        if (mTabs.size() <= 1) {
            android.widget.Toast.makeText(this, "至少保留一个标签页", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 销毁当前WebView
            if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
            BrowserTab tabToClose = mTabs.get(mCurrentTabIndex);
            mSwipeRefreshLayout.removeView(tabToClose.webView);
            tabToClose.webView.destroy();
            mTabs.remove(mCurrentTabIndex);

            // 切换到前一个标签页
            int newIndex = Math.max(0, mCurrentTabIndex - 1);
            if (newIndex >= mTabs.size()) {
                newIndex = mTabs.size() - 1;
            }
            switchToTab(newIndex);
        }

        android.widget.Toast.makeText(this, "标签页已关闭", android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示标签页详情
     */
    private void showTabDetails() {
        if (mTabs.isEmpty()) return;

        StringBuilder details = new StringBuilder();
        details.append("标签页详情:\n\n");

        for (int i = 0; i < mTabs.size(); i++) {
            BrowserTab tab = mTabs.get(i);
            String indicator = (i == mCurrentTabIndex) ? "[当前] " : "";
            details.append(String.format("%d. %s%s\n", i + 1, indicator, tab.title));
            details.append(String.format("   URL: %s\n\n", tab.url));
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("标签页详情")
            .setMessage(details.toString())
            .setPositiveButton("确定", null)
            .show();
    }

    /**
     * 清除历史记录
     */
    private void clearHistory() {
        if (mWebView != null) {
            mWebView.clearHistory();
            android.widget.Toast.makeText(this, "历史记录已清除", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 添加当前页面到书签
     */
    private void addCurrentPageToBookmarks() {
        if (currentUrl == null || currentUrl.isEmpty()) {
            android.widget.Toast.makeText(this, "无法获取当前页面信息", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否已收藏
        if (mBookmarkManager.isBookmarked(currentUrl)) {
            android.widget.Toast.makeText(this, "该页面已在书签中", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
        // 获取页面标题
        String pageTitle = currentUrl;
        if (mWebView != null) {
            String title = mWebView.getTitle();
            if (title != null && !title.isEmpty()) {
                pageTitle = title;
            }
        }

        // 创建BookmarkInfo对象
        com.hippo.ehviewer.client.data.BookmarkInfo bookmark = new com.hippo.ehviewer.client.data.BookmarkInfo();
        bookmark.title = pageTitle;
        bookmark.url = currentUrl;
                    bookmark.createTime = System.currentTimeMillis();
        bookmark.lastVisitTime = System.currentTimeMillis();
                    bookmark.visitCount = 1;
                    
        // 添加到数据库
        long result = mBookmarkManager.addBookmark(bookmark);
        if (result > 0) {
            android.widget.Toast.makeText(this, "已添加到书签: " + pageTitle, android.widget.Toast.LENGTH_SHORT).show();
            } else {
            android.widget.Toast.makeText(this, "添加书签失败", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 清空所有书签
     */
    private void clearAllBookmarks() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要清空所有书签吗？此操作不可恢复。")
            .setPositiveButton("确定", (dialog, which) -> {
                boolean result = mBookmarkManager.clearAllBookmarks();
                if (result) {
                    android.widget.Toast.makeText(this, "已清空所有书签", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                    android.widget.Toast.makeText(this, "清空书签失败", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示设置菜单
     */
    private void showSettings() {
        String[] menuItems = {
            "📥 下载管理",
            "⭐ 书签管理",
            "📚 历史记录",
            "🔍 搜索引擎",
            "🏠 设置首页",
            "🌐 隐私设置",
            "⚡ 性能设置"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("设置")
            .setItems(menuItems, (dialog, which) -> {
                switch (which) {
                    case 0: // 下载管理
                        showDownloadManager();
                        break;
                    case 1: // 书签管理
                        showBookmarks();
                        break;
                    case 2: // 历史记录
                        showHistory();
                        break;
                    case 3: // 搜索引擎
                        showSearchEngineSelector();
                        break;
                    case 4: // 设置首页
                        showHomepageSettings();
                        break;
                    case 5: // 隐私设置
                        showPrivacySettings();
                        break;
                    case 6: // 性能设置
                        showPerformanceSettings();
                        break;
                }
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    /**
     * 显示下载管理
     */
    private void showDownloadManager() {
        android.widget.Toast.makeText(this, "下载管理功能开发中", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: 实现下载管理功能
    }

    /**
     * 显示隐私设置
     */
    private void showPrivacySettings() {
        String[] privacyOptions = {
            "清除浏览数据",
            "隐私模式",
            "广告拦截设置",
            "Cookie管理"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("隐私设置")
            .setItems(privacyOptions, (dialog, which) -> {
                    switch (which) {
                    case 0: // 清除浏览数据
                        clearBrowsingData();
                            break;
                    case 1: // 隐私模式
                        toggleIncognitoMode();
                            break;
                    case 2: // 广告拦截设置
                        showAdBlockSettings();
                            break;
                    case 3: // Cookie管理
                        showCookieSettings();
                            break;
                    }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示性能设置
     */
    private void showPerformanceSettings() {
        String[] performanceOptions = {
            "缓存管理",
            "图像加载设置",
            "JavaScript设置",
            "网络设置"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("性能设置")
            .setItems(performanceOptions, (dialog, which) -> {
                    switch (which) {
                    case 0: // 缓存管理
                        showCacheManager();
                            break;
                    case 1: // 图像加载设置
                        toggleImageLoading();
                            break;
                    case 2: // JavaScript设置
                        toggleJavaScript();
                            break;
                    case 3: // 网络设置
                        showNetworkSettings();
                            break;
                    }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 清除浏览数据
     */
    private void clearBrowsingData() {
        if (mWebView != null) {
            mWebView.clearCache(true);
            mWebView.clearHistory();
            mWebView.clearFormData();
            android.widget.Toast.makeText(this, "已清除浏览数据", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 切换隐私模式
     */
    private void toggleIncognitoMode() {
        android.widget.Toast.makeText(this, "隐私模式功能开发中", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: 实现隐私模式
    }

    /**
     * 显示广告拦截设置
     */
    private void showAdBlockSettings() {
        android.widget.Toast.makeText(this, "广告拦截设置功能开发中", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: 实现广告拦截设置
    }

    /**
     * 显示Cookie设置
     */
    private void showCookieSettings() {
        android.widget.Toast.makeText(this, "Cookie管理功能开发中", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: 实现Cookie管理
    }

    /**
     * 显示缓存管理
     */
    private void showCacheManager() {
        android.widget.Toast.makeText(this, "缓存管理功能开发中", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: 实现缓存管理
    }

    /**
     * 切换图像加载
     */
    private void toggleImageLoading() {
        android.widget.Toast.makeText(this, "图像加载设置功能开发中", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: 实现图像加载设置
    }

    /**
     * 切换JavaScript
     */
    private void toggleJavaScript() {
        android.widget.Toast.makeText(this, "JavaScript设置功能开发中", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: 实现JavaScript设置
    }

    /**
     * 显示网络设置
     */
    private void showNetworkSettings() {
        android.widget.Toast.makeText(this, "网络设置功能开发中", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: 实现网络设置
    }

    /**
     * 显示搜索引擎选择器
     */
    private void showSearchEngineSelector() {
        if (mSearchConfigManager == null) return;

        java.util.List<SearchConfigManager.SearchEngine> engines = mSearchConfigManager.getAvailableEngines();
        SearchConfigManager.SearchEngine currentEngine = mSearchConfigManager.getCurrentEngine();

        String[] engineNames = new String[engines.size()];
        int checkedItem = -1;

        for (int i = 0; i < engines.size(); i++) {
            SearchConfigManager.SearchEngine engine = engines.get(i);
            engineNames[i] = engine.name;
            if (currentEngine != null && currentEngine.id.equals(engine.id)) {
                checkedItem = i;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("选择搜索引擎")
            .setSingleChoiceItems(engineNames, checkedItem, (dialog, which) -> {
                SearchConfigManager.SearchEngine selectedEngine = engines.get(which);
                mSearchConfigManager.switchEngine(selectedEngine.id);
                android.widget.Toast.makeText(this,
                    "已切换到: " + selectedEngine.name,
                    android.widget.Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示首页设置
     */
    private void showHomepageSettings() {
        if (mSearchConfigManager == null) return;

        String currentHomepage = mSearchConfigManager.getDefaultHomepageUrl();
        java.util.List<SearchConfigManager.HomepageOption> customOptions = mSearchConfigManager.getCustomHomepageOptions();

        // 构建选项列表
        java.util.List<String> optionsList = new java.util.ArrayList<>();
        optionsList.add("📄 使用默认首页 (" + currentHomepage + ")");

        // 添加自定义选项
        for (SearchConfigManager.HomepageOption option : customOptions) {
            optionsList.add("🏠 " + option.name);
        }

        optionsList.add("⭐ 设置当前页面为首页");
        optionsList.add("🔄 重置为Google首页");
        optionsList.add("❌ 禁用默认首页");

        String[] options = optionsList.toArray(new String[0]);

        new android.app.AlertDialog.Builder(this)
            .setTitle("设置首页")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // 使用默认首页
                    android.widget.Toast.makeText(this,
                        "当前首页: " + currentHomepage,
                        android.widget.Toast.LENGTH_LONG).show();
                } else if (which <= customOptions.size()) {
                    // 选择自定义首页
                    SearchConfigManager.HomepageOption selectedOption = customOptions.get(which - 1);
                    android.widget.Toast.makeText(this,
                        "已切换到: " + selectedOption.name,
                        android.widget.Toast.LENGTH_SHORT).show();
                    loadUrl(selectedOption.url);
                        } else {
                    // 处理其他选项
                    int optionIndex = which - customOptions.size() - 1;
                    switch (optionIndex) {
                        case 0: // 设置当前页面为首页
                            if (currentUrl != null && !currentUrl.isEmpty()) {
                                android.widget.Toast.makeText(this,
                                    "已设置当前页面为首页: " + currentUrl,
                                    android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                                android.widget.Toast.makeText(this,
                                    "无法获取当前页面URL",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 1: // 重置为Google首页
                            android.widget.Toast.makeText(this,
                                "已重置为Google首页",
                                android.widget.Toast.LENGTH_SHORT).show();
                            loadUrl("https://www.google.com");
                            break;
                        case 2: // 禁用默认首页
                            android.widget.Toast.makeText(this,
                                "默认首页功能已禁用",
                                android.widget.Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }



    /**
     * 显示/隐藏进度条
     */
    private void showProgress(boolean show) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
            } else {
            super.onBackPressed();
        }
                    }

                    @Override
    protected void onDestroy() {
        // 清理所有标签页的WebView资源
        for (BrowserTab tab : mTabs) {
                                    if (tab.webView != null) {
                tab.webView.destroy();
            }
        }
        mTabs.clear();

        // 清理当前WebView引用
        mWebView = null;

        super.onDestroy();
    }

    /**
     * 启动WebViewActivity
     */
    public static void startWebView(android.content.Context context, String url) {
            Intent intent = new Intent(context, WebViewActivity.class);
        if (url != null && !url.isEmpty()) {
            intent.putExtra(EXTRA_URL, url);
        }
            context.startActivity(intent);
    }

    /**
     * 启动简化版WebViewActivity（兼容性方法）
     */
    public static void startSimpleWebView(android.content.Context context, String url) {
            Intent intent = new Intent(context, WebViewActivity.class);
        if (url != null && !url.isEmpty()) {
            intent.putExtra(EXTRA_URL, url);
        }
            context.startActivity(intent);
    }

    /**
     * 注册浏览器拦截器（空实现，保持兼容性）
     */
    public static void registerBrowserInterceptor(android.content.Context context) {
        // 空实现，简化版本不需要复杂的拦截器
    }

    /**
     * 退出视频全屏（空实现，保持兼容性）
     */
    public void exitVideoFullscreen() {
        // 空实现，简化版本不支持视频全屏
    }
}
