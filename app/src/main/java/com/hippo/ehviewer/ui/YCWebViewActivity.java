package com.hippo.ehviewer.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.browser.YCWebViewBrowserManager;
import com.hippo.ehviewer.util.AppLaunchInterceptor;
import com.hippo.ehviewer.analytics.ChannelTracker;
import com.hippo.ehviewer.client.SearchConfigManager;

/**
 * 全新浏览器Activity - 基于YCWebView最佳实践
 * 完全重构，功能完整，专注于现代浏览体验
 *
 * 核心特性：
 * 1. 基于YCWebView的稳定架构
 * 2. X5内核深度优化
 * 3. 智能地址栏和搜索建议
 * 4. 书签管理和历史记录
 * 5. 多标签页支持
 * 6. 完整的视频播放支持
 * 7. 文件加载和管理
 * 8. JS交互接口
 * 9. 搜索引擎配置
 * 10. 默认首页设置
 */
public class YCWebViewActivity extends AppCompatActivity {
    private static final String TAG = "YCWebViewActivity";

    // 常量定义 (兼容旧版本API)
    public static final String EXTRA_URL = "url";

    // UI组件
    private FrameLayout mWebViewContainer;
    private ProgressBar mProgressBar;
    private EditText mAddressBar;
    private ImageButton mClearButton;
    private TextView mTitleText;
    private ImageView mSslIndicator;

    // 顶部按钮
    private ImageButton mBookmarkAddButton;
    private ImageButton mSearchButton;

    // 底部导航按钮
    private ImageButton mBottomBackButton;
    private ImageButton mBottomTabsButton;
    private ImageButton mBottomHistoryButton;
    private ImageButton mBottomMenuButton;

    // 隐藏的兼容性按钮
    private ImageButton mMenuButton;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageButton mRefreshButton;
    private ImageButton mHomeButton;
    private ImageButton mHistoryButton;
    private ImageButton mStatsButton;
    private ImageButton mTabsButton;
    private ImageButton mBookmarkButton;

    // 浏览器管理器
    private YCWebViewBrowserManager mBrowserManager;
    private android.view.View mWebView;

    // 状态
    private String mCurrentUrl = "";
    private String mCurrentTitle = "";
    private boolean mIsSecureConnection = false;

    // 数据管理
    private SharedPreferences mPreferences;
    private Handler mHandler;

    // 智能地址栏模块1 - SmartAddressBar类
    private SmartAddressBar mSmartAddressBar;

    // 智能地址栏模块2 - 传统建议系统
    private AppLaunchInterceptor mAppLaunchInterceptor;
    private ArrayAdapter<String> mSuggestionsAdapter;
    private PopupWindow mSuggestionsPopup;

    // 书签和历史记录
    private java.util.List<String> mBookmarks;
    private java.util.List<String> mHistory;
    private java.util.Map<String, String> mBookmarkTitles;

    // 多标签页管理
    private java.util.List<TabInfo> mTabs;
    private int mCurrentTabIndex = 0;
    private static final int MAX_TABS = 10;

    // 搜索引擎配置
    private static final String PREF_SEARCH_ENGINE = "search_engine";
    private static final String PREF_HOMEPAGE = "homepage";
    private static final String DEFAULT_SEARCH_ENGINE = "https://www.google.com/search?q=";
    private static final String DEFAULT_HOMEPAGE = "https://www.google.com";

    // Tab信息类
    private static class TabInfo {
        String url;
        String title;
        android.graphics.Bitmap favicon;

        TabInfo(String url, String title) {
            this.url = url;
            this.title = title;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yc_webview);

        // 初始化远端搜索/首页配置管理器
        try {
            SearchConfigManager.getInstance(this).initialize();
        } catch (Exception e) {
            Log.w(TAG, "SearchConfigManager initialize failed", e);
        }

        initializeViews();
        setupBrowser();
        handleIntent();
    }

    /**
     * 初始化UI组件
     */
    private void initializeViews() {
        // 初始化基本组件
        mWebViewContainer = findViewById(R.id.webview_container);
        mProgressBar = findViewById(R.id.progress_bar);

        // 初始化地址栏组件
        mAddressBar = findViewById(R.id.address_bar);
        mClearButton = findViewById(R.id.clear_button);

        // 初始化页面信息组件
        mTitleText = findViewById(R.id.title_text);
        mSslIndicator = findViewById(R.id.ssl_indicator);

        // 初始化顶部按钮
        mBookmarkAddButton = findViewById(R.id.bookmark_add_button);
        mSearchButton = findViewById(R.id.search_button);

        // 初始化底部导航按钮
        mBottomBackButton = findViewById(R.id.bottom_back_button);
        mBottomTabsButton = findViewById(R.id.bottom_tabs_button);
        mBottomHistoryButton = findViewById(R.id.bottom_history_button);
        mBottomMenuButton = findViewById(R.id.bottom_menu_button);

        // 初始化隐藏的兼容性按钮
        mMenuButton = findViewById(R.id.menu_button);
        mBackButton = findViewById(R.id.back_button);
        mForwardButton = findViewById(R.id.forward_button);
        mRefreshButton = findViewById(R.id.refresh_button);
        mHomeButton = findViewById(R.id.home_button);
        mHistoryButton = findViewById(R.id.history_button);
        mStatsButton = findViewById(R.id.stats_button);
        mTabsButton = findViewById(R.id.tabs_button);
        mBookmarkButton = findViewById(R.id.bookmark_button);

        // 设置按钮点击事件
        setupButtonListeners();

        // 设置进度条
        mProgressBar.setMax(100);
        mProgressBar.setProgress(0);

        // 初始化地址栏功能
        setupAddressBar();

        // 初始化智能地址栏
        setupSmartAddressBar();

        // 初始化应用启动拦截器
        mAppLaunchInterceptor = new AppLaunchInterceptor(this);

        // 初始化底部导航栏
        updateNavigationButtons();
    }

    /**
     * 设置按钮监听器
     */
    private void setupButtonListeners() {
        // 顶部按钮
        mBookmarkAddButton.setOnClickListener(v -> addCurrentPageToBookmarks());
        mSearchButton.setOnClickListener(v -> performSearch());

        // 底部导航按钮
        mBottomBackButton.setOnClickListener(v -> goBack());
        mBottomTabsButton.setOnClickListener(v -> showTabs());
        mBottomHistoryButton.setOnClickListener(v -> showHistory());
        mBottomMenuButton.setOnClickListener(v -> showMenu());

        // 隐藏的兼容性按钮（用于向后兼容）
        mMenuButton.setOnClickListener(v -> showMenu());
        mBackButton.setOnClickListener(v -> goBack());
        mForwardButton.setOnClickListener(v -> goForward());
        mRefreshButton.setOnClickListener(v -> refresh());
        mHomeButton.setOnClickListener(v -> goHome());
        mHistoryButton.setOnClickListener(v -> showHistory());
        mStatsButton.setOnClickListener(v -> showPerformanceStats());
        mTabsButton.setOnClickListener(v -> showTabs());
        mBookmarkButton.setOnClickListener(v -> addCurrentPageToBookmarks());
    }

    /**
     * 设置智能地址栏模块1 - SmartAddressBar类
     * 提供现代化的智能建议体验
     */
    private void setupSmartAddressBar() {
        Log.d(TAG, "初始化智能地址栏模块1 (SmartAddressBar)");
        mSmartAddressBar = new SmartAddressBar(this, mAddressBar, this);
        mSmartAddressBar.setListener(new SmartAddressBar.SmartAddressBarListener() {
            @Override
            public void onUrlSelected(String url) {
                Log.d(TAG, "SmartAddressBar: URL选择 - " + url);
                navigateToUrl(url);
            }

            @Override
            public void onSearchSelected(String searchUrl) {
                Log.d(TAG, "SmartAddressBar: 搜索选择 - " + searchUrl);
                navigateToUrl(searchUrl);
            }
        });
        Log.d(TAG, "智能地址栏模块1初始化完成");
    }

    /**
     * 获取历史记录（供SmartAddressBar使用）
     */
    public java.util.List<String> getHistory() {
        return new java.util.ArrayList<>(mHistory);
    }

    /**
     * 获取书签（供SmartAddressBar使用）
     */
    public java.util.List<String> getBookmarks() {
        return new java.util.ArrayList<>(mBookmarks);
    }

    /**
     * 获取书签标题（供SmartAddressBar使用）
     */
    public String getBookmarkTitle(String url) {
        return mBookmarkTitles.get(url);
    }

    /**
     * 设置浏览器
     */
    private void setupBrowser() {
        // 创建浏览器管理器
        mBrowserManager = new YCWebViewBrowserManager(this);

        // 设置回调
        mBrowserManager.setBrowserCallback(new YCWebViewBrowserManager.BrowserCallback() {
            @Override
            public void onProgressChanged(int progress) {
                runOnUiThread(() -> {
                    if (mProgressBar != null) {
                        mProgressBar.setProgress(progress);
                        mProgressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
                    }
                });
            }

            @Override
            public void onTitleChanged(String title) {
                mCurrentTitle = title;
                runOnUiThread(() -> {
                    if (mTitleText != null) {
                        mTitleText.setText(title);
                    }
                    updateBookmarkButton(isBookmarked(mCurrentUrl));
                });
            }

            @Override
            public void onUrlChanged(String url) {
                mCurrentUrl = url;
                runOnUiThread(() -> {
                    updateAddressBar();
                    updateSslIndicator();
                    updateBookmarkButton(isBookmarked(url));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(YCWebViewActivity.this, "加载错误: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onVideoReady() {
                runOnUiThread(() -> {
                    Toast.makeText(YCWebViewActivity.this, "视频已准备就绪", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFileReady(String filePath) {
                runOnUiThread(() -> {
                    Toast.makeText(YCWebViewActivity.this, "文件已准备就绪: " + filePath, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onPageStarted(String url) {
                runOnUiThread(() -> {
                    if (mProgressBar != null) {
                        mProgressBar.setVisibility(View.VISIBLE);
                        mProgressBar.setProgress(0);
                    }
                    updateBookmarkButton(isBookmarked(url));
                });
            }

            @Override
            public void onPageFinished(String url) {
                runOnUiThread(() -> {
                    if (mProgressBar != null) {
                        mProgressBar.setVisibility(View.GONE);
                    }
                    updateBookmarkButton(isBookmarked(url));
                    updateBookmarkAddButton(); // 更新书签添加按钮状态
                    updateSslIndicator();
                    updateNavigationButtons(); // 更新导航按钮状态

                    // 🎯 下载统计触发：成功访问网页时统计
                    if (url != null && !url.startsWith("about:") && !url.startsWith("chrome-error://")) {
                        Log.d(TAG, "🌐 页面加载完成，触发下载统计: " + url);
                        ChannelTracker.getInstance().trackDownloadSafe();
                    }
                });
            }

            @Override
            public void onSecurityStatusChanged(boolean isSecure) {
                mIsSecureConnection = isSecure;
                runOnUiThread(() -> updateSslIndicator());
            }

            @Override
            public void onBookmarkStatusChanged(boolean isBookmarked) {
                runOnUiThread(() -> updateBookmarkButton(isBookmarked));
            }
        });

        // 创建WebView
        mWebView = mBrowserManager.createOptimizedWebView(mWebViewContainer);

        // 设置WebView可见
        if (mWebView != null) {
            mWebView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 处理启动Intent
     */
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            // 检查是否需要拦截
            if (mAppLaunchInterceptor != null && mAppLaunchInterceptor.handleAppLaunch(intent)) {
                // 被拦截，不继续处理
                Log.d(TAG, "Intent intercepted by AppLaunchInterceptor");
                return;
            }

            String url = intent.getStringExtra("url");
            if (url == null || url.isEmpty()) {
                // 检查是否有数据URI
                Uri data = intent.getData();
                if (data != null) {
                    url = data.toString();
                }
            }

            if (url != null && !url.isEmpty()) {
                loadUrl(url);
            } else {
                // 加载默认主页
                loadDefaultPage();
            }
        }
    }

    /**
     * 加载URL
     */
    private void loadUrl(String url) {
        if (mBrowserManager != null && url != null && !url.isEmpty()) {
            Log.d(TAG, "Loading URL: " + url);
            mBrowserManager.loadUrl(url);
        }
    }

    /**
     * 加载默认页面
     */
    private void loadDefaultPage() {
        // 优先使用用户设置
        String userHomepage = mPreferences.getString(PREF_HOMEPAGE, null);
        if (userHomepage != null && !userHomepage.trim().isEmpty()) {
            loadUrl(userHomepage);
            return;
        }

        // 其次遵循远端配置（无法访问时，管理器内部回退到 Bing）
        String remoteHomepage;
        try {
            SearchConfigManager scm = SearchConfigManager.getInstance(this);
            if (scm.isHomepageEnabled()) {
                remoteHomepage = scm.getDefaultHomepageUrl();
            } else {
                remoteHomepage = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Fetch remote homepage failed", e);
            remoteHomepage = null;
        }

        if (remoteHomepage != null && !remoteHomepage.isEmpty()) {
            loadUrl(remoteHomepage);
        } else {
            // 最后兜底：Bing（仅在完全无法访问配置时）
            loadUrl("https://www.bing.com");
        }
    }

    /**
     * 返回
     */
    private void goBack() {
        if (mBrowserManager != null) {
            boolean success = mBrowserManager.goBack();
            if (success) {
                // 延迟更新，确保WebView状态已更新
                mHandler.postDelayed(this::updateNavigationButtons, 100);
            }
        }
    }

    /**
     * 添加当前页面到书签
     */
    private void addCurrentPageToBookmarks() {
        if (mCurrentUrl.isEmpty()) {
            Toast.makeText(this, "当前页面无法添加到书签", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = mCurrentTitle.isEmpty() ? mCurrentUrl : mCurrentTitle;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加书签");

        final android.widget.EditText titleInput = new android.widget.EditText(this);
        titleInput.setText(title);
        titleInput.setHint("书签名");

        builder.setView(titleInput);
        builder.setPositiveButton("添加", (dialog, which) -> {
            String bookmarkTitle = titleInput.getText().toString().trim();
            if (bookmarkTitle.isEmpty()) {
                bookmarkTitle = title;
            }

            // 添加到书签列表
            mBookmarks.add(mCurrentUrl);
            mBookmarkTitles.put(mCurrentUrl, bookmarkTitle);
            saveBookmarks();

            Toast.makeText(this, "书签已添加: " + bookmarkTitle, Toast.LENGTH_SHORT).show();

            // 更新书签添加按钮状态
            updateBookmarkAddButton();
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 更新书签添加按钮状态
     */
    private void updateBookmarkAddButton() {
        if (mBookmarkAddButton != null) {
            boolean isBookmarked = isBookmarked(mCurrentUrl);
            mBookmarkAddButton.setImageResource(isBookmarked ?
                android.R.drawable.ic_menu_delete :
                android.R.drawable.ic_menu_add);
            mBookmarkAddButton.setContentDescription(isBookmarked ? "移除书签" : "添加书签");
        }
    }

    /**
     * 执行搜索
     */
    private void performSearch() {
        String query = mAddressBar.getText().toString().trim();
        if (!query.isEmpty()) {
            processInput(query);
        } else {
            // 如果地址栏为空，显示提示
            android.widget.Toast.makeText(this, "请输入搜索内容或网址", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 前进
     */
    private void goForward() {
        if (mBrowserManager != null) {
            boolean success = mBrowserManager.goForward();
            if (success) {
                // 延迟更新，确保WebView状态已更新
                mHandler.postDelayed(this::updateNavigationButtons, 100);
            }
        }
    }

    /**
     * 刷新
     */
    private void refresh() {
        if (mBrowserManager != null) {
            mBrowserManager.reload();
        }
    }

    /**
     * 返回主页
     */
    private void goHome() {
        loadDefaultPage();
    }

    /**
     * 显示性能统计信息
     */
    private void showPerformanceStats() {
        if (mBrowserManager != null) {
            String stats = mBrowserManager.getPerformanceStats();

            // 创建对话框显示性能统计
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("YCWebView 性能统计")
                  .setMessage(stats)
                  .setPositiveButton("确定", null)
                  .setNeutralButton("复制到剪贴板", (dialog, which) -> {
                      android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                          getSystemService(Context.CLIPBOARD_SERVICE);
                      if (clipboard != null) {
                          android.content.ClipData clip = android.content.ClipData.newPlainText("性能统计", stats);
                          clipboard.setPrimaryClip(clip);
                          android.widget.Toast.makeText(this, "性能统计已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show();
                      }
                  })
                  .show();
        }
    }

    /**
     * 更新导航按钮状态
     */
    private void updateNavigationButtons() {
        runOnUiThread(() -> {
            if (mBrowserManager != null) {
                // 检查WebView的导航状态
                boolean canGoBack = false;
                boolean canGoForward = false;

                try {
                    if (mBrowserManager.isX5Available() && mBrowserManager.getWebView() != null) {
                        // X5 WebView
                        com.tencent.smtt.sdk.WebView x5WebView = (com.tencent.smtt.sdk.WebView) mBrowserManager.getWebView();
                        canGoBack = x5WebView.canGoBack();
                        canGoForward = x5WebView.canGoForward();
                    } else if (mBrowserManager.getWebView() != null) {
                        // 系统WebView
                        android.webkit.WebView systemWebView = (android.webkit.WebView) mBrowserManager.getWebView();
                        canGoBack = systemWebView.canGoBack();
                        canGoForward = systemWebView.canGoForward();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error checking navigation state", e);
                }

                // 顶部没有导航按钮，只更新底部导航按钮

                // 更新底部导航按钮
                mBottomBackButton.setEnabled(canGoBack);
                mBottomBackButton.setAlpha(canGoBack ? 1.0f : 0.5f);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mBrowserManager != null && mBrowserManager.goBack()) {
            updateNavigationButtons();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // WebView的pause/resume现在由BrowserManager管理
        if (mBrowserManager != null) {
            // BrowserManager会处理WebView的生命周期
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // WebView的pause/resume现在由BrowserManager管理
        if (mBrowserManager != null) {
            // BrowserManager会处理WebView的生命周期
        }
    }


    /**
     * 启动浏览器Activity
     */
    public static void start(Context context, String url) {
        Intent intent = new Intent(context, YCWebViewActivity.class);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    /**
     * 检查X5可用性
     */
    public boolean isX5Available() {
        return mBrowserManager != null && mBrowserManager.isX5Available();
    }

    /**
     * 获取浏览器统计信息
     */
    public String getBrowserStats() {
        if (mBrowserManager != null) {
            return "X5可用: " + isX5Available() +
                   ", 当前URL: " + mCurrentUrl +
                   ", 标题: " + mCurrentTitle;
        }
        return "浏览器未初始化";
    }

    /**
     * 启动浏览器Activity (兼容旧版本API)
     */
    public static void startWebView(Context context, String url) {
        YCWebViewActivity.start(context, url);
    }

    /**
     * 注册浏览器拦截器 (兼容旧版本API)
     */
    public static void registerBrowserInterceptor(Context context) {
        // TODO: 实现浏览器拦截器注册
        Log.d(TAG, "Browser interceptor registration requested - to be implemented");
    }

    /**
     * 处理自定义视图显示 (兼容旧版本API)
     */
    public void onShowCustomView(View view, android.webkit.WebChromeClient.CustomViewCallback callback) {
        Log.d(TAG, "onShowCustomView called - video fullscreen handling");
        // TODO: 实现全屏视频处理
    }

    /**
     * 处理自定义视图隐藏 (兼容旧版本API)
     */
    public void onHideCustomView() {
        Log.d(TAG, "onHideCustomView called - video fullscreen exit");
        // TODO: 实现隐藏全屏视频
    }

    /**
     * 退出视频全屏 (兼容旧版本API)
     */
    public void exitVideoFullscreen() {
        Log.d(TAG, "exitVideoFullscreen called");
        // 调用onHideCustomView来处理全屏退出
        onHideCustomView();
    }

    // ================ 新增功能方法 ================

    /**
     * 设置地址栏功能 - 传统建议系统（模块2）
     * 注意：此系统与SmartAddressBar（模块1）同时工作
     */
    private void setupAddressBar() {
        // 初始化数据
        initData();

        // 🎯 优化地址栏点击行为：点击时清除内容并聚焦
        mAddressBar.setOnClickListener(v -> {
            Log.d(TAG, "地址栏被点击，清除内容并聚焦");
            mAddressBar.setText("");
            mAddressBar.requestFocus();
            mAddressBar.setSelection(0);
            
            // 显示键盘
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(mAddressBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
            
            // 显示清除按钮
            updateClearButtonVisibility();
        });

        // 设置回车键监听
        mAddressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                String text = mAddressBar.getText().toString().trim();
                if (!text.isEmpty()) {
                    navigateToUrl(text);
                    // 隐藏两个建议系统
                    if (mSmartAddressBar != null) {
                        mSmartAddressBar.hideSuggestions();
                    }
                    hideSuggestions();
                }
                return true;
            }
            return false;
        });

        // 🎯 增强清除按钮功能
        mClearButton.setOnClickListener(v -> {
            Log.d(TAG, "清除按钮被点击");
            mAddressBar.setText("");
            mAddressBar.requestFocus();
            mAddressBar.setSelection(0);
            
            // 隐藏两个建议系统
            if (mSmartAddressBar != null) {
                mSmartAddressBar.hideSuggestions();
            }
            hideSuggestions();
            
            // 隐藏清除按钮
            updateClearButtonVisibility();
        });

        // 🎯 添加文本变化监听以控制清除按钮显示
        mAddressBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateClearButtonVisibility();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 注意：焦点监听也由SmartAddressBar处理，这里不再重复设置
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mPreferences = getSharedPreferences("YCWebView", MODE_PRIVATE);
        mHandler = new Handler(Looper.getMainLooper());

        // 初始化书签和历史记录
        mBookmarks = new java.util.ArrayList<>();
        mHistory = new java.util.ArrayList<>();
        mBookmarkTitles = new java.util.HashMap<>();

        // 初始化标签页
        mTabs = new java.util.ArrayList<>();
        mTabs.add(new TabInfo("", "新标签页"));

        // 加载保存的数据
        loadBookmarks();
        loadHistory();
        loadTabs();

        // 初始化建议适配器
        mSuggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
    }

    /**
     * 导航到URL或搜索
     */
    private void navigateToUrl(String input) {
        String url = processInput(input);

        if (mBrowserManager != null) {
            mBrowserManager.loadUrl(url);
            addToHistory(url, mCurrentTitle);
            updateCurrentTab(url, mCurrentTitle);
        }
    }

    /**
     * 处理输入内容（URL或搜索词）
     */
    private String processInput(String input) {
        input = input.trim();

        try {
            // 统一交由 SearchConfigManager 处理（遵循远端配置；管理器内部会兜底到 Bing）
            SearchConfigManager scm = SearchConfigManager.getInstance(this);
            return scm.processInput(input);
        } catch (Exception e) {
            Log.w(TAG, "SearchConfigManager processInput failed, fallback to local", e);
        }

        // 本地后备处理：简单URL识别，否则用Bing搜索
        if (input.contains("://") || input.contains("www.") || input.matches("^[a-zA-Z0-9]+\\.[a-zA-Z]{2,}.*")) {
            if (!input.contains("://")) {
                input = "https://" + input;
            }
            return input;
        }
        return "https://www.bing.com/search?q=" + java.net.URLEncoder.encode(input);
    }

    /**
     * 显示搜索建议 - 传统建议系统（模块2）
     */
    private void showSuggestions(String query) {
        Log.d(TAG, "传统建议系统: 显示建议 for query: " + query);
        java.util.List<String> suggestions = new java.util.ArrayList<>();

        // 添加书签建议
        for (String bookmark : mBookmarks) {
            if (bookmark.toLowerCase().contains(query.toLowerCase())) {
                suggestions.add("🔖 " + bookmark);
            }
        }

        // 添加历史记录建议
        for (String history : mHistory) {
            if (history.toLowerCase().contains(query.toLowerCase())) {
                suggestions.add("🕒 " + history);
            }
        }

        // 添加搜索建议
        if (suggestions.size() < 5) {
            suggestions.add("🔍 搜索: " + query);
        }

        Log.d(TAG, "传统建议系统: 生成 " + suggestions.size() + " 个建议");

        if (!suggestions.isEmpty()) {
            mSuggestionsAdapter.clear();
            mSuggestionsAdapter.addAll(suggestions);

            if (mSuggestionsPopup == null) {
                ListView listView = new ListView(this);
                listView.setAdapter(mSuggestionsAdapter);
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    String selected = (String) parent.getItemAtPosition(position);
                    Log.d(TAG, "传统建议系统: 选择建议 - " + selected);
                    if (selected.startsWith("🔖 ")) {
                        navigateToUrl(selected.substring(2));
                    } else if (selected.startsWith("🕒 ")) {
                        navigateToUrl(selected.substring(2));
                    } else if (selected.startsWith("🔍 ")) {
                        String searchTerm = selected.substring(4);
                        navigateToUrl(searchTerm);
                    }
                    hideSuggestions();
                });

                mSuggestionsPopup = new PopupWindow(listView,
                    mAddressBar.getWidth(),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
                mSuggestionsPopup.setFocusable(false);
                mSuggestionsPopup.setOutsideTouchable(true);
            }

            mSuggestionsPopup.showAsDropDown(mAddressBar, 0, 0);
            Log.d(TAG, "传统建议系统: 建议弹窗已显示");
        }
    }

    /**
     * 隐藏搜索建议 - 传统建议系统（模块2）
     */
    private void hideSuggestions() {
        Log.d(TAG, "传统建议系统: 隐藏建议弹窗");
        if (mSuggestionsPopup != null && mSuggestionsPopup.isShowing()) {
            mSuggestionsPopup.dismiss();
        }
    }

    /**
     * 显示标签页
     */
    private void showTabs() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("标签页 (" + mTabs.size() + ")");

        String[] tabTitles = new String[mTabs.size()];
        for (int i = 0; i < mTabs.size(); i++) {
            TabInfo tab = mTabs.get(i);
            tabTitles[i] = (i == mCurrentTabIndex ? "● " : "○ ") +
                          (tab.title.isEmpty() ? "新标签页" : tab.title);
        }

        builder.setItems(tabTitles, (dialog, which) -> {
            switchToTab(which);
        });

        builder.setPositiveButton("新建标签页", (dialog, which) -> {
            createNewTab();
        });

        builder.setNegativeButton("关闭当前标签页", (dialog, which) -> {
            if (mTabs.size() > 1) {
                closeCurrentTab();
            } else {
                Toast.makeText(this, "至少保留一个标签页", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    /**
     * 切换标签页
     */
    private void switchToTab(int index) {
        if (index >= 0 && index < mTabs.size()) {
            mCurrentTabIndex = index;
            TabInfo tab = mTabs.get(index);
            if (!tab.url.isEmpty()) {
                navigateToUrl(tab.url);
            }
        }
    }

    /**
     * 创建新标签页
     */
    private void createNewTab() {
        if (mTabs.size() < MAX_TABS) {
            TabInfo newTab = new TabInfo("", "新标签页");
            mTabs.add(newTab);
            mCurrentTabIndex = mTabs.size() - 1;
            loadDefaultPage();
            Toast.makeText(this, "新标签页已创建", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "已达到最大标签页数量", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 关闭当前标签页
     */
    private void closeCurrentTab() {
        if (mTabs.size() > 1) {
            mTabs.remove(mCurrentTabIndex);
            if (mCurrentTabIndex >= mTabs.size()) {
                mCurrentTabIndex = mTabs.size() - 1;
            }
            switchToTab(mCurrentTabIndex);
        }
    }

    /**
     * 更新当前标签页
     */
    private void updateCurrentTab(String url, String title) {
        if (mCurrentTabIndex < mTabs.size()) {
            TabInfo tab = mTabs.get(mCurrentTabIndex);
            tab.url = url;
            tab.title = title.isEmpty() ? "新标签页" : title;
        }
    }

    /**
     * 切换书签状态
     */
    private void toggleBookmark() {
        if (mCurrentUrl.isEmpty()) {
            Toast.makeText(this, "当前页面无法添加书签", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mBookmarks.contains(mCurrentUrl)) {
            // 移除书签
            mBookmarks.remove(mCurrentUrl);
            mBookmarkTitles.remove(mCurrentUrl);
            saveBookmarks();
            Toast.makeText(this, "已从书签中移除", Toast.LENGTH_SHORT).show();
            updateBookmarkButton(false);
        } else {
            // 添加书签
            mBookmarks.add(mCurrentUrl);
            mBookmarkTitles.put(mCurrentUrl, mCurrentTitle);
            saveBookmarks();
            Toast.makeText(this, "已添加到书签", Toast.LENGTH_SHORT).show();
            updateBookmarkButton(true);
        }
    }

    /**
     * 更新书签按钮状态
     */
    private void updateBookmarkButton(boolean isBookmarked) {
        mBookmarkButton.setImageResource(isBookmarked ?
            android.R.drawable.btn_star_big_on :
            android.R.drawable.btn_star_big_off);
    }

    /**
     * 显示历史记录
     */
    private void showHistory() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("历史记录");

        String[] historyItems = new String[Math.min(mHistory.size(), 10)];
        for (int i = 0; i < historyItems.length; i++) {
            String url = mHistory.get(i);
            String title = mBookmarkTitles.getOrDefault(url, url);
            historyItems[i] = title;
        }

        builder.setItems(historyItems, (dialog, which) -> {
            String url = mHistory.get(which);
            navigateToUrl(url);
        });

        builder.setPositiveButton("清空历史记录", (dialog, which) -> {
            clearHistory();
        });

        builder.show();
    }

    /**
     * 显示菜单 - 修复菜单位置错位问题
     */
    private void showMenu() {
        // 🎯 使用底部菜单按钮作为锚点，确保菜单位置正确
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, mBottomMenuButton);
        popup.getMenuInflater().inflate(R.menu.browser_menu, popup.getMenu());

        // 🎯 设置菜单重力，确保在按钮上方显示
        try {
            // 使用反射设置重力，确保菜单在按钮上方显示
            java.lang.reflect.Field popupField = android.widget.PopupMenu.class.getDeclaredField("mPopup");
            popupField.setAccessible(true);
            Object popupMenu = popupField.get(popup);
            java.lang.reflect.Method setGravityMethod = popupMenu.getClass().getDeclaredMethod("setGravity", int.class);
            setGravityMethod.invoke(popupMenu, android.view.Gravity.TOP | android.view.Gravity.END);
        } catch (Exception e) {
            Log.w(TAG, "无法设置菜单重力，使用默认位置", e);
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_bookmarks) {
                showBookmarks();
                return true;
            } else if (itemId == R.id.menu_history) {
                showHistory();
                return true;
            } else if (itemId == R.id.menu_settings) {
                showSettings();
                return true;
            } else if (itemId == R.id.menu_share) {
                shareCurrentPage();
                return true;
            } else if (itemId == R.id.menu_desktop_mode) {
                toggleDesktopMode();
                return true;
            } else if (itemId == R.id.menu_incognito) {
                toggleIncognitoMode();
                return true;
            } else if (itemId == R.id.menu_page_info) {
                showPageInfo();
                return true;
            } else if (itemId == R.id.menu_downloads) {
                showDownloads();
                return true;
            } else if (itemId == R.id.menu_find) {
                showFindInPage();
                return true;
            } else if (itemId == R.id.menu_print) {
                printPage();
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * 🎯 切换隐私模式
     */
    private void toggleIncognitoMode() {
        // TODO: 实现隐私模式功能
        Toast.makeText(this, "隐私模式功能开发中", Toast.LENGTH_SHORT).show();
    }

    /**
     * 🎯 显示页面信息
     */
    private void showPageInfo() {
        if (mCurrentUrl.isEmpty()) {
            Toast.makeText(this, "没有页面信息", Toast.LENGTH_SHORT).show();
            return;
        }

        String pageInfo = "URL: " + mCurrentUrl + "\n" +
                         "标题: " + mCurrentTitle + "\n" +
                         "安全状态: " + (mIsSecureConnection ? "安全" : "不安全");
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("页面信息")
               .setMessage(pageInfo)
               .setPositiveButton("确定", null)
               .show();
    }

    /**
     * 🎯 显示下载管理
     */
    private void showDownloads() {
        // TODO: 实现下载管理功能
        Toast.makeText(this, "下载管理功能开发中", Toast.LENGTH_SHORT).show();
    }

    /**
     * 🎯 在页面中查找
     */
    private void showFindInPage() {
        // TODO: 实现页面查找功能
        Toast.makeText(this, "页面查找功能开发中", Toast.LENGTH_SHORT).show();
    }

    /**
     * 🎯 打印页面
     */
    private void printPage() {
        // TODO: 实现页面打印功能
        Toast.makeText(this, "页面打印功能开发中", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示书签
     */
    private void showBookmarks() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("书签");

        String[] bookmarkItems = new String[mBookmarks.size()];
        for (int i = 0; i < mBookmarks.size(); i++) {
            String url = mBookmarks.get(i);
            String title = mBookmarkTitles.getOrDefault(url, url);
            bookmarkItems[i] = title;
        }

        builder.setItems(bookmarkItems, (dialog, which) -> {
            String url = mBookmarks.get(which);
            navigateToUrl(url);
        });

        builder.setPositiveButton("管理书签", (dialog, which) -> {
            // TODO: 实现书签管理界面
            Toast.makeText(this, "书签管理功能开发中", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    /**
     * 显示设置
     */
    private void showSettings() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("浏览器设置");

        String[] options = {
            "搜索引擎设置",
            "默认首页设置",
            "隐私设置",
            "清除缓存",
            "页面截图",
            "长图截图"
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showSearchEngineSettings();
                    break;
                case 1:
                    showHomepageSettings();
                    break;
                case 2:
                    showPrivacySettings();
                    break;
                case 3:
                    clearCache();
                    break;
                case 4:
                    captureScreenshot();
                    break;
                case 5:
                    captureLongScreenshot();
                    break;
            }
        });

        builder.show();
    }

    /**
     * 显示搜索引擎设置
     */
    private void showSearchEngineSettings() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择搜索引擎");

        String[] engines = {
            "Google",
            "Bing",
            "百度",
            "DuckDuckGo"
        };

        final String[] urls = {
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://www.baidu.com/s?wd=",
            "https://duckduckgo.com/?q="
        };

        builder.setItems(engines, (dialog, which) -> {
            mPreferences.edit().putString(PREF_SEARCH_ENGINE, urls[which]).apply();
            Toast.makeText(this, "搜索引擎已设置为: " + engines[which], Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    /**
     * 显示首页设置
     */
    private void showHomepageSettings() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("设置默认首页");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(mPreferences.getString(PREF_HOMEPAGE, DEFAULT_HOMEPAGE));
        input.setHint("输入首页URL");

        builder.setView(input);
        builder.setPositiveButton("保存", (dialog, which) -> {
            String homepage = input.getText().toString().trim();
            if (!homepage.isEmpty()) {
                mPreferences.edit().putString(PREF_HOMEPAGE, homepage).apply();
                Toast.makeText(this, "默认首页已保存", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    /**
     * 显示隐私设置
     */
    private void showPrivacySettings() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("隐私设置");

        String[] options = {
            "清除浏览历史",
            "清除书签",
            "清除所有数据",
            "应用拦截管理"
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    clearHistory();
                    break;
                case 1:
                    clearBookmarks();
                    break;
                case 2:
                    clearAllData();
                    break;
                case 3:
                    showAppInterceptSettings();
                    break;
            }
        });

        builder.show();
    }

    /**
     * 🎯 更新清除按钮显示状态
     */
    private void updateClearButtonVisibility() {
        if (mClearButton != null) {
            String text = mAddressBar.getText().toString().trim();
            if (text.isEmpty()) {
                mClearButton.setVisibility(View.GONE);
            } else {
                mClearButton.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 分享当前页面
     */
    private void shareCurrentPage() {
        if (mCurrentUrl.isEmpty()) {
            Toast.makeText(this, "没有可分享的页面", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mCurrentTitle + "\n" + mCurrentUrl);

        startActivity(Intent.createChooser(shareIntent, "分享页面"));
    }

    /**
     * 切换桌面模式
     */
    private void toggleDesktopMode() {
        // TODO: 实现桌面模式切换
        Toast.makeText(this, "桌面模式功能开发中", Toast.LENGTH_SHORT).show();
    }

    // ================ 数据管理方法 ================

    /**
     * 加载书签
     */
    private void loadBookmarks() {
        String bookmarksStr = mPreferences.getString("bookmarks", "");
        if (!bookmarksStr.isEmpty()) {
            mBookmarks.addAll(java.util.Arrays.asList(bookmarksStr.split("\\|")));
        }

        String titlesStr = mPreferences.getString("bookmark_titles", "");
        if (!titlesStr.isEmpty()) {
            String[] entries = titlesStr.split("\\|");
            for (String entry : entries) {
                String[] parts = entry.split(":::");
                if (parts.length == 2) {
                    mBookmarkTitles.put(parts[0], parts[1]);
                }
            }
        }
    }

    /**
     * 保存书签
     */
    private void saveBookmarks() {
        mPreferences.edit()
            .putString("bookmarks", String.join("|", mBookmarks))
            .putString("bookmark_titles", mBookmarkTitles.entrySet().stream()
                .map(e -> e.getKey() + ":::" + e.getValue())
                .collect(java.util.stream.Collectors.joining("|")))
            .apply();
    }

    /**
     * 加载历史记录
     */
    private void loadHistory() {
        String historyStr = mPreferences.getString("history", "");
        if (!historyStr.isEmpty()) {
            mHistory.addAll(java.util.Arrays.asList(historyStr.split("\\|")));
        }
    }

    /**
     * 添加到历史记录
     */
    private void addToHistory(String url, String title) {
        if (!url.isEmpty() && !url.equals("about:blank")) {
            mHistory.remove(url); // 移除重复项
            mHistory.add(0, url); // 添加到开头

            // 限制历史记录数量
            while (mHistory.size() > 100) {
                mHistory.remove(mHistory.size() - 1);
            }

            saveHistory();
        }
    }

    /**
     * 保存历史记录
     */
    private void saveHistory() {
        mPreferences.edit()
            .putString("history", String.join("|", mHistory))
            .apply();
    }

    /**
     * 加载标签页
     */
    private void loadTabs() {
        String tabsStr = mPreferences.getString("tabs", "");
        if (!tabsStr.isEmpty()) {
            mTabs.clear();
            String[] tabEntries = tabsStr.split("\\|\\|\\|");
            for (String entry : tabEntries) {
                String[] parts = entry.split("\\|\\|");
                if (parts.length >= 2) {
                    mTabs.add(new TabInfo(parts[0], parts[1]));
                }
            }
        }
    }

    /**
     * 保存标签页
     */
    private void saveTabs() {
        StringBuilder tabsStr = new StringBuilder();
        for (int i = 0; i < mTabs.size(); i++) {
            if (i > 0) tabsStr.append("|||");
            TabInfo tab = mTabs.get(i);
            tabsStr.append(tab.url).append("||").append(tab.title);
        }
        mPreferences.edit().putString("tabs", tabsStr.toString()).apply();
    }

    /**
     * 清除历史记录
     */
    private void clearHistory() {
        mHistory.clear();
        saveHistory();
        Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除书签
     */
    private void clearBookmarks() {
        mBookmarks.clear();
        mBookmarkTitles.clear();
        saveBookmarks();
        updateBookmarkButton(false);
        Toast.makeText(this, "书签已清除", Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除缓存
     */
    private void clearCache() {
        try {
            // 清除WebView缓存
            if (mBrowserManager != null) {
                mBrowserManager.clearCache();
            }

            // 清除应用缓存
            android.webkit.CookieManager.getInstance().removeAllCookies(null);
            android.webkit.CookieManager.getInstance().flush();

            // 清除本地缓存目录
            clearCacheDirectory();

            Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear cache", e);
            Toast.makeText(this, "清除缓存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 截取页面截图
     */
    private void captureScreenshot() {
        try {
            if (mBrowserManager != null) {
                android.graphics.Bitmap screenshot = mBrowserManager.captureScreenshot();
                if (screenshot != null) {
                    String filePath = mBrowserManager.saveScreenshot(screenshot);
                    if (filePath != null) {
                        Toast.makeText(this, "截图已保存: " + filePath, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "截图保存失败", Toast.LENGTH_SHORT).show();
                    }
                    screenshot.recycle();
                } else {
                    Toast.makeText(this, "截图失败", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture screenshot", e);
            Toast.makeText(this, "截图失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 截取长图
     */
    private void captureLongScreenshot() {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("正在生成长图...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                if (mBrowserManager != null) {
                    android.graphics.Bitmap longScreenshot = mBrowserManager.captureLongScreenshot();
                    if (longScreenshot != null) {
                        String filePath = mBrowserManager.saveScreenshot(longScreenshot);
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            if (filePath != null) {
                                Toast.makeText(this, "长图已保存: " + filePath, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "长图保存失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                        longScreenshot.recycle();
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "长图生成失败", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to capture long screenshot", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "长图生成失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 清除缓存目录
     */
    private void clearCacheDirectory() {
        try {
            // 清除应用缓存目录
            java.io.File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                clearDirectory(cacheDir);
            }

            // 清除外部缓存目录
            java.io.File externalCacheDir = getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                clearDirectory(externalCacheDir);
            }

            Log.d(TAG, "Cache directory cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear cache directory", e);
        }
    }

    /**
     * 递归清除目录
     */
    private void clearDirectory(java.io.File dir) {
        if (dir == null || !dir.exists()) return;

        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    clearDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
    }

    /**
     * 清除所有数据
     */
    private void clearAllData() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认清除")
               .setMessage("这将清除所有书签、历史记录和设置。确定继续吗？")
               .setPositiveButton("确定", (dialog, which) -> {
                   clearBookmarks();
                   clearHistory();
                   mPreferences.edit().clear().apply();
                   Toast.makeText(this, "所有数据已清除", Toast.LENGTH_SHORT).show();
               })
               .setNegativeButton("取消", null)
               .show();
    }

    /**
     * 显示应用拦截设置
     */
    private void showAppInterceptSettings() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("应用拦截管理");

        String[] options = {
            "拦截开关: " + (mAppLaunchInterceptor.isInterceptEnabled() ? "开启" : "关闭"),
            "查看黑名单 (" + mAppLaunchInterceptor.getBlockedPackages().size() + ")",
            "查看白名单 (" + mAppLaunchInterceptor.getWhitelistedPackages().size() + ")",
            "清除所有拦截设置"
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    toggleInterceptEnabled();
                    break;
                case 1:
                    showBlockedPackages();
                    break;
                case 2:
                    showWhitelistedPackages();
                    break;
                case 3:
                    clearInterceptSettings();
                    break;
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 切换拦截开关
     */
    private void toggleInterceptEnabled() {
        boolean currentState = mAppLaunchInterceptor.isInterceptEnabled();
        mAppLaunchInterceptor.setInterceptEnabled(!currentState);

        String message = !currentState ? "已开启应用拦截" : "已关闭应用拦截";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示黑名单应用
     */
    private void showBlockedPackages() {
        java.util.Set<String> blocked = mAppLaunchInterceptor.getBlockedPackages();
        if (blocked.isEmpty()) {
            Toast.makeText(this, "黑名单为空", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[blocked.size()];
        String[] packageNames = blocked.toArray(new String[0]);

        for (int i = 0; i < packageNames.length; i++) {
            items[i] = getAppName(packageNames[i]);
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("黑名单应用")
               .setItems(items, (dialog, which) -> {
                   showPackageManagementDialog(packageNames[which], true);
               })
               .setNegativeButton("关闭", null)
               .show();
    }

    /**
     * 显示白名单应用
     */
    private void showWhitelistedPackages() {
        java.util.Set<String> whitelisted = mAppLaunchInterceptor.getWhitelistedPackages();
        if (whitelisted.isEmpty()) {
            Toast.makeText(this, "白名单为空", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[whitelisted.size()];
        String[] packageNames = whitelisted.toArray(new String[0]);

        for (int i = 0; i < packageNames.length; i++) {
            items[i] = getAppName(packageNames[i]);
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("白名单应用")
               .setItems(items, (dialog, which) -> {
                   showPackageManagementDialog(packageNames[which], false);
               })
               .setNegativeButton("关闭", null)
               .show();
    }

    /**
     * 显示应用管理对话框
     */
    private void showPackageManagementDialog(String packageName, boolean isBlocked) {
        String appName = getAppName(packageName);
        String currentList = isBlocked ? "黑名单" : "白名单";

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("管理应用: " + appName)
               .setMessage("当前在" + currentList + "中")
               .setPositiveButton("移除", (dialog, which) -> {
                   if (isBlocked) {
                       mAppLaunchInterceptor.unblockPackage(packageName);
                       Toast.makeText(this, "已从黑名单移除", Toast.LENGTH_SHORT).show();
                   } else {
                       mAppLaunchInterceptor.removeFromWhitelist(packageName);
                       Toast.makeText(this, "已从白名单移除", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("取消", null)
               .show();
    }

    /**
     * 清除所有拦截设置
     */
    private void clearInterceptSettings() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认清除")
               .setMessage("这将清除所有拦截设置，包括黑名单和白名单。确定继续吗？")
               .setPositiveButton("确定", (dialog, which) -> {
                   mAppLaunchInterceptor.clearAllSettings();
                   Toast.makeText(this, "所有拦截设置已清除", Toast.LENGTH_SHORT).show();
               })
               .setNegativeButton("取消", null)
               .show();
    }

    /**
     * 获取应用名称
     */
    private String getAppName(String packageName) {
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(info);
        } catch (Exception e) {
            return packageName; // 返回包名作为后备
        }
    }

    /**
     * 更新SSL状态指示器
     */
    private void updateSslIndicator() {
        if (mSslIndicator != null) {
            if (mIsSecureConnection && !mCurrentUrl.isEmpty()) {
                mSslIndicator.setImageResource(android.R.drawable.ic_lock_lock);
                mSslIndicator.setVisibility(View.VISIBLE);
            } else {
                mSslIndicator.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 更新地址栏显示
     */
    private void updateAddressBar() {
        if (mAddressBar != null && !mCurrentUrl.isEmpty()) {
            // 临时禁用智能建议，防止在页面加载时显示建议
            if (mSmartAddressBar != null) {
                mSmartAddressBar.disableSuggestions();
            }

            // 设置地址栏文本
            mAddressBar.setText(mCurrentUrl);
            mAddressBar.setSelection(mCurrentUrl.length());
            
            // 🎯 更新清除按钮状态
            updateClearButtonVisibility();

            // 重置用户输入状态，等待用户主动输入
            if (mSmartAddressBar != null) {
                mSmartAddressBar.resetUserTypingState();
                // 延迟重新启用建议，避免立即触发
                mHandler.postDelayed(() -> {
                    if (mSmartAddressBar != null) {
                        mSmartAddressBar.enableSuggestions();
                    }
                }, 500);
            }
        }
    }

    /**
     * 检查是否为书签
     */
    private boolean isBookmarked(String url) {
        return mBookmarks.contains(url);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveTabs(); // 保存标签页状态

        // 销毁智能地址栏模块1
        if (mSmartAddressBar != null) {
            mSmartAddressBar.destroy();
            mSmartAddressBar = null;
        }

        if (mBrowserManager != null) {
            mBrowserManager.destroy();
        }

        // 隐藏传统建议系统（模块2）
        hideSuggestions();
    }
}
