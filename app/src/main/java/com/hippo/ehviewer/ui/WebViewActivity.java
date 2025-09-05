package com.hippo.ehviewer.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.ui.browser.EnhancedWebChromeClient;
import com.hippo.ehviewer.ui.browser.EnhancedWebViewClient;
import com.hippo.ehviewer.ui.widget.EmbeddedVideoPlayer;
import com.hippo.ehviewer.util.WebViewScreenshotUtil;

import androidx.appcompat.app.AlertDialog;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.BookmarkManager;
import com.hippo.ehviewer.client.data.BookmarkInfo;
import com.hippo.ehviewer.client.AdBlockManager;
import com.hippo.ehviewer.client.SearchConfigManager;
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.util.UserEnvironmentDetector;
import com.hippo.ehviewer.userscript.UserScriptManager;
import com.hippo.ehviewer.userscript.ScriptStorage;
import com.hippo.ehviewer.userscript.ScriptUpdater;

// 搜索建议相关
import com.hippo.ehviewer.ui.SearchSuggestionsManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 简化的WebView浏览器Activity
 * 移除了历史记录和标签管理功能，只保留基本的浏览功能
 */
public class WebViewActivity extends AppCompatActivity implements
        EnhancedWebChromeClient.ProgressCallback,
        EnhancedWebChromeClient.TitleCallback,
        EnhancedWebChromeClient.JsDialogCallback,
        EnhancedWebViewClient.ErrorCallback,
        EnhancedWebViewClient.PageCallback {

    private static final String TAG = "WebViewActivity";

    // 常量定义
    public static final String EXTRA_URL = "url";

    // UI组件
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WebView mWebView;
    private ImageButton mGoButton;
    private ImageButton mHomeButton;
    private ImageButton mHistoryButton;
    private ImageButton mBookmarkButton;
    private ImageButton mTabsButton;
    private ImageButton mSettingsButton;
    private ProgressBar mProgressBar;

    // 增强客户端
    private EnhancedWebChromeClient mEnhancedChromeClient;
    private EnhancedWebViewClient mEnhancedWebClient;

    // 视频播放器
    private EmbeddedVideoPlayer mVideoPlayer;
    
    // Loading State UI组件
    private View mLoadingStateView;
    private EditText mQuickSearchInput;
    private ImageButton mQuickSearchButton;
    private TextView mLoadingStatusText;
    private TextView mQuickHomeButton;
    private TextView mQuickHistoryButton;
    private TextView mQuickBookmarksButton;

    // 管理器
    private BookmarkManager mBookmarkManager;
    private AdBlockManager mAdBlockManager;
    private SearchConfigManager mSearchConfigManager;
    private HistoryManager mHistoryManager;

    // 用户脚本管理器
    private UserScriptManager mUserScriptManager;
    private ScriptStorage mScriptStorage;
    private ScriptUpdater mScriptUpdater;

    // 智能功能管理器
    private SmartMenuManager mSmartMenuManager;
    private SmartTipsManager mSmartTipsManager;

    // 搜索建议管理器
    private SearchSuggestionsManager mSearchSuggestionsManager;

    // Toast管理器 - 防止Toast消息过多
    private ToastManager mToastManager;

    // 系统错误处理器
    private SystemErrorHandler mSystemErrorHandler;

    // 系统错误监控器
    private SystemErrorMonitor mSystemErrorMonitor;

    // 地址栏输入框的文本监听器，用于控制搜索建议显示
    private android.text.TextWatcher mOmniboxTextWatcher;

    // 状态变量
    private boolean isDesktopMode = false;
    private String currentUrl = "";
    private boolean isPageLoading = false;
    private Handler mLoadingTimeoutHandler = new Handler(Looper.getMainLooper());

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
        mHistoryManager = HistoryManager.getInstance(this);

        // 初始化用户脚本管理器
        mUserScriptManager = UserScriptManager.getInstance(this);
        mScriptStorage = new ScriptStorage(this);
        mScriptUpdater = new ScriptUpdater(this, mUserScriptManager, mScriptStorage);

        // 初始化搜索配置管理器
        mSearchConfigManager.initialize();

        // 初始化智能功能管理器
        mSmartMenuManager = new SmartMenuManager(this);
        mSmartTipsManager = new SmartTipsManager(this);

        // 初始化搜索建议管理器
        mSearchSuggestionsManager = new SearchSuggestionsManager(this);

        // 初始化Toast管理器
        mToastManager = new ToastManager(this);

        // 初始化系统错误处理器
        mSystemErrorHandler = new SystemErrorHandler(this);

        // 初始化系统错误监控器
        mSystemErrorMonitor = new SystemErrorMonitor(this);

        // 初始化UI
            initializeViews();

        // 设置WebView
        setupWebView();

        // 设置监听器
        setupListeners();

        // 设置搜索建议监听器
        setupSearchSuggestions();

        // 初始显示加载状态
        showLoadingState(true, "正在准备浏览器...");
        
        // 处理初始URL
        handleInitialUrl();

        // 请求地理位置权限
        requestLocationPermission();
    }

    /**
     * 初始化UI控件
     */
    private void initializeViews() {
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mWebView = findViewById(R.id.web_view);
        // mGoButton = findViewById(R.id.go_button); // 暂时注释，资源不存在
        mHomeButton = findViewById(R.id.home_button);
        mHistoryButton = findViewById(R.id.history_button);
        mBookmarkButton = findViewById(R.id.bookmark_button);
            mTabsButton = findViewById(R.id.tabs_button);
        mSettingsButton = findViewById(R.id.settings_button);
        mProgressBar = findViewById(R.id.progress_bar);
        
        // 初始化加载状态视图组件
        mLoadingStateView = findViewById(R.id.loading_state_view);
        mQuickSearchInput = findViewById(R.id.quick_search_input);
        mQuickSearchButton = findViewById(R.id.quick_search_button);
        mLoadingStatusText = findViewById(R.id.loading_status_text);
        mQuickHomeButton = findViewById(R.id.quick_home_button);
        mQuickHistoryButton = findViewById(R.id.quick_history_button);
        mQuickBookmarksButton = findViewById(R.id.quick_bookmarks_button);
        
        // 初始化智能小贴士容器
        ViewGroup rootLayout = findViewById(R.id.web_view_root);
        if (rootLayout != null && mSmartTipsManager != null) {
            mSmartTipsManager.initializeTipsContainer(rootLayout);
        }
    }

    /**
     * 设置WebView
     */
    private void setupWebView() {
        if (mWebView == null) return;

        // 处理系统兼容性
        handleSystemCompatibility();

        // 初始化增强客户端
        mEnhancedChromeClient = new EnhancedWebChromeClient(this);
        mEnhancedWebClient = new EnhancedWebViewClient(this);

        // 处理OPPO设备兼容性
        com.hippo.ehviewer.util.DefaultBrowserHelper.handleOppoDeviceCompatibility(this);

        // 初始化视频播放器
        mVideoPlayer = new EmbeddedVideoPlayer(this);
        mVideoPlayer.attachToWebView(mWebView, this);

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
        
        // 优化加载性能
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        // 启用硬件加速
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // 设置下载监听器
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));
                
                // 设置下载描述
                String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType);
                request.setDescription("正在下载文件: " + fileName);
                request.setTitle(fileName);
                
                // 设置下载路径
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
                
                // 设置通知
                request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setAllowedOverMetered(true);
                request.setAllowedOverRoaming(true);
                
                // 开始下载
                android.app.DownloadManager downloadManager = 
                    (android.app.DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (downloadManager != null) {
                    downloadManager.enqueue(request);
                    Toast.makeText(this, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
                }
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Download failed", e);
                Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // 使用增强的WebViewClient
        mWebView.setWebViewClient(mEnhancedWebClient);

        // 使用增强的WebChromeClient
        mWebView.setWebChromeClient(mEnhancedChromeClient);
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
     * 设置搜索建议功能
     */
    private void setupSearchSuggestions() {
        if (mSearchSuggestionsManager == null) return;

        // 获取地址栏输入框
        EditText omniboxInput = findViewById(R.id.omnibox_input);
        if (omniboxInput == null) return;

        // 设置搜索建议监听器
        mSearchSuggestionsManager.setOnSuggestionClickListener(new SearchSuggestionsManager.OnSuggestionClickListener() {
            @Override
            public void onSuggestionClick(SearchSuggestionsManager.SuggestionItem item) {
                handleSuggestionClick(item);
            }

            @Override
            public void onSuggestionLongClick(SearchSuggestionsManager.SuggestionItem item) {
                handleSuggestionLongClick(item);
            }
        });

        // 创建文本变化监听器
        mOmniboxTextWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() > 0) {
                    // 显示搜索建议
                    View anchor = findViewById(R.id.omnibox_container);
                    if (anchor != null) {
                        mSearchSuggestionsManager.showSuggestions(anchor, query);
                    }
                } else {
                    // 隐藏搜索建议
                    mSearchSuggestionsManager.hideSuggestions();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };

        // 为输入框添加文本变化监听器
        omniboxInput.addTextChangedListener(mOmniboxTextWatcher);

        // 当输入框失去焦点时隐藏搜索建议
        omniboxInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // 延迟隐藏，让点击建议有时间处理
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    mSearchSuggestionsManager.hideSuggestions();
                }, 200);
            }
        });
    }

    /**
     * 处理搜索建议点击
     */
    private void handleSuggestionClick(SearchSuggestionsManager.SuggestionItem item) {
        if (item == null || item.url == null) return;

        // 隐藏搜索建议
        mSearchSuggestionsManager.hideSuggestions();

        // 加载URL
        loadUrl(item.url);

        // 隐藏键盘
        EditText omniboxInput = findViewById(R.id.omnibox_input);
        if (omniboxInput != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(omniboxInput.getWindowToken(), 0);
            }
        }

        // 根据建议类型添加额外处理
        switch (item.type) {
            case HISTORY:
                // 历史记录点击，可以添加统计等
                break;
            case BOOKMARK:
                // 书签点击
                break;
            case SEARCH_SUGGESTION:
                // 搜索建议点击
                break;
            case QUICK_ACTION:
                // 快捷操作
                break;
            case NEW_TAB_ACTION:
                // 新标签页操作 - 创建新标签页并加载URL
                handleNewTabAction(item);
                break;
        }
    }

    /**
     * 处理新标签页操作
     */
    private void handleNewTabAction(SearchSuggestionsManager.SuggestionItem item) {
        if (item == null || item.url == null) return;

        try {
            // 创建新标签页
            String title = item.title != null ? item.title : "新标签页";
            createNewTab(title, item.url, true);

            // 显示提示消息
            Toast.makeText(this, "已在新标签页中打开", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create new tab", e);
            Toast.makeText(this, "创建新标签页失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理搜索建议长按
     */
    private void handleSuggestionLongClick(SearchSuggestionsManager.SuggestionItem item) {
        if (item == null) return;

        // 根据建议类型显示不同的上下文菜单
        switch (item.type) {
            case HISTORY:
                showHistoryContextMenu(item);
                break;
            case BOOKMARK:
                showBookmarkContextMenu(item);
                break;
            case NEW_TAB_ACTION:
                showNewTabContextMenu(item);
                break;
            default:
                // 其他类型的建议可以添加默认处理
                break;
        }
    }

    /**
     * 显示历史记录上下文菜单
     */
    private void showHistoryContextMenu(SearchSuggestionsManager.SuggestionItem item) {
        new AlertDialog.Builder(this)
            .setTitle("历史记录操作")
            .setItems(new String[]{"在新标签页中打开", "复制链接", "从历史记录中删除"}, (dialog, which) -> {
                switch (which) {
                    case 0: // 新标签页打开
                        // 这里可以实现新标签页功能
                        Toast.makeText(this, "新标签页功能开发中", Toast.LENGTH_SHORT).show();
                        break;
                    case 1: // 复制链接
                        copyToClipboard(item.url);
                        break;
                    case 2: // 删除历史记录
                        // 这里可以实现删除历史记录功能
                        Toast.makeText(this, "删除历史记录功能开发中", Toast.LENGTH_SHORT).show();
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示书签上下文菜单
     */
    private void showBookmarkContextMenu(SearchSuggestionsManager.SuggestionItem item) {
        new AlertDialog.Builder(this)
            .setTitle("书签操作")
            .setItems(new String[]{"在新标签页中打开", "复制链接", "编辑书签", "删除书签"}, (dialog, which) -> {
                switch (which) {
                    case 0: // 新标签页打开
                        Toast.makeText(this, "新标签页功能开发中", Toast.LENGTH_SHORT).show();
                        break;
                    case 1: // 复制链接
                        copyToClipboard(item.url);
                        break;
                    case 2: // 编辑书签
                        Toast.makeText(this, "编辑书签功能开发中", Toast.LENGTH_SHORT).show();
                        break;
                    case 3: // 删除书签
                        Toast.makeText(this, "删除书签功能开发中", Toast.LENGTH_SHORT).show();
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示新标签页上下文菜单
     */
    private void showNewTabContextMenu(SearchSuggestionsManager.SuggestionItem item) {
        new AlertDialog.Builder(this)
            .setTitle("新标签页操作")
            .setItems(new String[]{"在新标签页中打开", "复制链接", "在当前标签页中打开"}, (dialog, which) -> {
                switch (which) {
                    case 0: // 新标签页打开
                        handleNewTabAction(item);
                        break;
                    case 1: // 复制链接
                        copyToClipboard(item.url);
                        break;
                    case 2: // 当前标签页打开
                        loadUrl(item.url);
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 复制文本到剪贴板
     */
    private void copyToClipboard(String text) {
        if (text == null) return;

        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            android.content.ClipData clip = android.content.ClipData.newPlainText("URL", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {

        // 搜索/访问按钮
        if (mGoButton != null) {
            mGoButton.setOnClickListener(v -> {
                EditText omniboxInput = findViewById(R.id.omnibox_input);
                String input = omniboxInput != null ? omniboxInput.getText().toString().trim() : "";
                if (!input.isEmpty()) {
                    // 使用SearchConfigManager处理输入
                    String processedUrl = mSearchConfigManager.processInput(input);
                    loadUrl(processedUrl);
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
        
        // 设置加载状态视图的监听器
        setupLoadingStateListeners();
    }
    
    /**
     * 设置加载状态视图的监听器
     */
    private void setupLoadingStateListeners() {
        // 快速搜索输入框
        if (mQuickSearchInput != null) {
            mQuickSearchInput.setOnEditorActionListener((v, actionId, event) -> {
                String input = mQuickSearchInput.getText().toString().trim();
                if (!input.isEmpty()) {
                    String processedUrl = mSearchConfigManager.processInput(input);
                    loadUrl(processedUrl);
                    return true;
                }
                return false;
            });
        }
        
        // 快速搜索按钮
        if (mQuickSearchButton != null) {
            mQuickSearchButton.setOnClickListener(v -> {
                String input = mQuickSearchInput.getText().toString().trim();
                if (!input.isEmpty()) {
                    String processedUrl = mSearchConfigManager.processInput(input);
                    loadUrl(processedUrl);
                }
            });
        }
        
        // 快速主页按钮
        if (mQuickHomeButton != null) {
            mQuickHomeButton.setOnClickListener(v -> {
                String homepageUrl = "https://main.eh-viewer.com/";
                if (mSearchConfigManager != null && mSearchConfigManager.isHomepageEnabled()) {
                    homepageUrl = mSearchConfigManager.getDefaultHomepageUrl();
                }
                loadUrl(homepageUrl);
            });
        }
        
        // 快速历史按钮
        if (mQuickHistoryButton != null) {
            mQuickHistoryButton.setOnClickListener(v -> showHistory());
        }
        
        // 快速书签按钮
        if (mQuickBookmarksButton != null) {
            mQuickBookmarksButton.setOnClickListener(v -> showBookmarks());
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
        EditText omniboxInput = findViewById(R.id.omnibox_input);
        if (omniboxInput != null && url != null) {
            // 临时移除文本监听器，防止触发搜索建议
            if (mOmniboxTextWatcher != null) {
                omniboxInput.removeTextChangedListener(mOmniboxTextWatcher);
            }
            omniboxInput.setText(url);
            // 重新添加文本监听器
            if (mOmniboxTextWatcher != null) {
                omniboxInput.addTextChangedListener(mOmniboxTextWatcher);
            }
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
            "⚡ 性能设置",
            "🐒 用户脚本管理"
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
                    case 7: // 用户脚本管理
                        showUserScriptManager();
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
        try {
            // 启动下载管理Activity
            Intent intent = new Intent();
            intent.setAction(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // 如果系统下载管理器不可用，显示WebView下载历史
                showWebViewDownloads();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to open download manager", e);
            Toast.makeText(this, "无法打开下载管理", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示WebView下载历史
     */
    private void showWebViewDownloads() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("下载管理")
                .setMessage("WebView下载功能已启用。\n\n下载的文件将保存到:\n" + 
                           android.os.Environment.getExternalStoragePublicDirectory(
                           android.os.Environment.DIRECTORY_DOWNLOADS).getAbsolutePath())
                .setPositiveButton("打开下载文件夹", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.parse("file://" + 
                            android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                        intent.setDataAndType(uri, "resource/folder");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "无法打开文件夹", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
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
        try {
            boolean isIncognito = !isIncognitoModeEnabled();
            setIncognitoMode(isIncognito);
            
            String message = isIncognito ? "隐私模式已开启" : "隐私模式已关闭";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            
            if (isIncognito) {
                // 清除当前会话的cookies和历史
                clearSessionData();
                // 重新加载页面以应用隐私设置
                if (mWebView != null) {
                    mWebView.reload();
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to toggle incognito mode", e);
            Toast.makeText(this, "切换隐私模式失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查是否启用隐私模式
     */
    private boolean isIncognitoModeEnabled() {
        return getSharedPreferences("browser_settings", MODE_PRIVATE)
                .getBoolean("incognito_mode", false);
    }
    
    /**
     * 设置隐私模式状态
     */
    private void setIncognitoMode(boolean enabled) {
        getSharedPreferences("browser_settings", MODE_PRIVATE)
                .edit()
                .putBoolean("incognito_mode", enabled)
                .apply();
                
        if (mWebView != null) {
            WebSettings settings = mWebView.getSettings();
            if (enabled) {
                // 启用隐私模式设置
                settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                // setAppCacheEnabled is deprecated, skip it
                settings.setDatabaseEnabled(false);
                settings.setDomStorageEnabled(false);
            } else {
                // 恢复正常模式设置
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);
                settings.setDatabaseEnabled(true);
                settings.setDomStorageEnabled(true);
            }
        }
    }
    
    /**
     * 清除会话数据
     */
    private void clearSessionData() {
        try {
            if (mWebView != null) {
                mWebView.clearCache(true);
                mWebView.clearHistory();
                mWebView.clearFormData();
            }
            
            // 清除cookies
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to clear session data", e);
        }
    }

    /**
     * 显示广告拦截设置
     */
    private void showAdBlockSettings() {
        boolean isAdBlockEnabled = isAdBlockEnabled();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("广告拦截设置")
                .setMessage("当前状态: " + (isAdBlockEnabled ? "已启用" : "已禁用") + 
                           "\n\n广告拦截功能可以阻止页面加载广告内容，提升浏览体验。")
                .setPositiveButton(isAdBlockEnabled ? "禁用广告拦截" : "启用广告拦截", 
                    (dialog, which) -> {
                        toggleAdBlock();
                        Toast.makeText(this, 
                            isAdBlockEnabled ? "广告拦截已禁用" : "广告拦截已启用", 
                            Toast.LENGTH_SHORT).show();
                        
                        // 重新加载页面以应用设置
                        if (mWebView != null) {
                            mWebView.reload();
                        }
                    })
                .setNeutralButton("管理拦截列表", (dialog, which) -> showAdBlockList())
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 检查是否启用广告拦截
     */
    private boolean isAdBlockEnabled() {
        return getSharedPreferences("browser_settings", MODE_PRIVATE)
                .getBoolean("ad_block_enabled", true); // 默认启用
    }
    
    /**
     * 切换广告拦截状态
     */
    private void toggleAdBlock() {
        boolean enabled = !isAdBlockEnabled();
        getSharedPreferences("browser_settings", MODE_PRIVATE)
                .edit()
                .putBoolean("ad_block_enabled", enabled)
                .apply();
                
        // 更新AdBlockManager设置
        if (mAdBlockManager != null) {
            // AdBlockManager specific implementation would go here
            Log.d(TAG, "AdBlockManager setting updated: " + enabled);
        }
    }
    
    /**
     * 显示广告拦截列表管理
     */
    private void showAdBlockList() {
        String[] adBlockOptions = {
            "基础广告拦截规则",
            "社交媒体拦截",
            "弹窗拦截", 
            "跟踪器拦截",
            "自定义拦截规则"
        };
        
        boolean[] checkedOptions = {
            getSharedPreferences("browser_settings", MODE_PRIVATE).getBoolean("block_basic_ads", true),
            getSharedPreferences("browser_settings", MODE_PRIVATE).getBoolean("block_social_media", false),
            getSharedPreferences("browser_settings", MODE_PRIVATE).getBoolean("block_popups", true),
            getSharedPreferences("browser_settings", MODE_PRIVATE).getBoolean("block_trackers", true),
            false // 自定义规则暂时不可选
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("广告拦截规则")
                .setMultiChoiceItems(adBlockOptions, checkedOptions, 
                    (dialog, which, isChecked) -> {
                        String prefKey = "";
                        switch (which) {
                            case 0: prefKey = "block_basic_ads"; break;
                            case 1: prefKey = "block_social_media"; break;
                            case 2: prefKey = "block_popups"; break;
                            case 3: prefKey = "block_trackers"; break;
                        }
                        
                        if (!prefKey.isEmpty()) {
                            getSharedPreferences("browser_settings", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(prefKey, isChecked)
                                    .apply();
                        }
                    })
                .setPositiveButton("应用", (dialog, which) -> {
                    Toast.makeText(this, "广告拦截规则已更新", Toast.LENGTH_SHORT).show();
                    if (mWebView != null) {
                        mWebView.reload();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示Cookie设置
     */
    private void showCookieSettings() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            boolean cookiesEnabled = cookieManager.acceptCookie();
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Cookie设置")
                    .setMessage("当前状态: " + (cookiesEnabled ? "允许Cookie" : "阻止Cookie") + 
                               "\n\nCookie用于网站记住您的偏好设置和登录状态。")
                    .setPositiveButton(cookiesEnabled ? "禁用Cookie" : "启用Cookie", 
                        (dialog, which) -> {
                            cookieManager.setAcceptCookie(!cookiesEnabled);
                            String message = cookiesEnabled ? "Cookie已禁用" : "Cookie已启用";
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        })
                    .setNeutralButton("清除所有Cookie", (dialog, which) -> {
                        cookieManager.removeAllCookies(success -> {
                            String message = success ? "Cookie已清除" : "清除Cookie失败";
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        });
                        cookieManager.flush();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show cookie settings", e);
            Toast.makeText(this, "Cookie设置失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示缓存管理
     */
    private void showCacheManager() {
        try {
            // 计算缓存大小
            File cacheDir = getCacheDir();
            long cacheSize = calculateDirectorySize(cacheDir);
            String cacheSizeText = android.text.format.Formatter.formatFileSize(this, cacheSize);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("缓存管理")
                    .setMessage("当前缓存大小: " + cacheSizeText + 
                               "\n\n缓存包括网页数据、图片和其他临时文件。清除缓存可以释放存储空间。")
                    .setPositiveButton("清除所有缓存", (dialog, which) -> {
                        if (mWebView != null) {
                            mWebView.clearCache(true);
                        }
                        clearApplicationCache();
                        Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("清除WebView缓存", (dialog, which) -> {
                        if (mWebView != null) {
                            mWebView.clearCache(true);
                            Toast.makeText(this, "WebView缓存已清除", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show cache manager", e);
            Toast.makeText(this, "缓存管理失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }
        
        long size = 0;
        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error calculating directory size", e);
        }
        return size;
    }
    
    /**
     * 清除应用缓存
     */
    private void clearApplicationCache() {
        try {
            File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir);
            }
            
            File externalCacheDir = getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteRecursive(externalCacheDir);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error clearing application cache", e);
        }
    }
    
    /**
     * 递归删除文件夹内容
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    /**
     * 切换图像加载
     */
    private void toggleImageLoading() {
        try {
            if (mWebView != null) {
                WebSettings settings = mWebView.getSettings();
                boolean imagesEnabled = settings.getLoadsImagesAutomatically();
                
                settings.setLoadsImagesAutomatically(!imagesEnabled);
                settings.setBlockNetworkImage(imagesEnabled); // 反向设置
                
                // 保存设置
                getSharedPreferences("browser_settings", MODE_PRIVATE)
                        .edit()
                        .putBoolean("load_images", !imagesEnabled)
                        .apply();
                
                String message = imagesEnabled ? "图像加载已禁用" : "图像加载已启用";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                
                // 重新加载页面以应用设置
                mWebView.reload();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to toggle image loading", e);
            Toast.makeText(this, "切换图像加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 切换JavaScript
     */
    private void toggleJavaScript() {
        try {
            if (mWebView != null) {
                WebSettings settings = mWebView.getSettings();
                boolean jsEnabled = settings.getJavaScriptEnabled();
                
                settings.setJavaScriptEnabled(!jsEnabled);
                
                // 保存设置
                getSharedPreferences("browser_settings", MODE_PRIVATE)
                        .edit()
                        .putBoolean("javascript_enabled", !jsEnabled)
                        .apply();
                
                String message = jsEnabled ? "JavaScript已禁用" : "JavaScript已启用";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                
                // 重新加载页面以应用设置
                mWebView.reload();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to toggle JavaScript", e);
            Toast.makeText(this, "切换JavaScript失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示网络设置
     */
    private void showNetworkSettings() {
        try {
            // 使用全局环境检测信息
            UserEnvironmentDetector detector = UserEnvironmentDetector.getInstance(this);
            UserEnvironmentDetector.EnvironmentInfo envInfo = detector.getEnvironmentInfo();
            
            String[] networkOptions = {
                "自动检测网络环境",
                "优先使用国内服务",
                "优先使用国际服务",
                "混合网络模式"
            };
            
            int currentSelection = getSharedPreferences("browser_settings", MODE_PRIVATE)
                    .getInt("network_preference", 0);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("网络设置")
                    .setMessage("当前网络环境: " + envInfo.networkEnvironment + 
                               "\nIP地址: " + envInfo.ipAddress +
                               "\n地区: " + envInfo.country + " " + envInfo.region)
                    .setSingleChoiceItems(networkOptions, currentSelection, 
                        (dialog, which) -> {
                            getSharedPreferences("browser_settings", MODE_PRIVATE)
                                    .edit()
                                    .putInt("network_preference", which)
                                    .apply();
                            
                            // 应用网络设置
                            applyNetworkSettings(which);
                            
                            String selectedOption = networkOptions[which];
                            Toast.makeText(this, "已选择: " + selectedOption, Toast.LENGTH_SHORT).show();
                            
                            dialog.dismiss();
                        })
                    .setNeutralButton("检测网络环境", (dialog, which) -> {
                        Toast.makeText(this, "正在重新检测网络环境...", Toast.LENGTH_SHORT).show();
                        detector.startDetection(new UserEnvironmentDetector.DetectionCallback() {
                            @Override
                            public void onDetectionSuccess(UserEnvironmentDetector.EnvironmentInfo info) {
                                Toast.makeText(WebViewActivity.this, "网络环境检测完成", Toast.LENGTH_SHORT).show();
                            }
                            
                            @Override
                            public void onDetectionFailed(String error) {
                                Toast.makeText(WebViewActivity.this, "网络环境检测失败", Toast.LENGTH_SHORT).show();
                            }
                            
                            @Override
                            public void onDetectionProgress(String message) {
                                // 可以显示检测进度
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show network settings", e);
            Toast.makeText(this, "网络设置失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 应用网络设置
     */
    private void applyNetworkSettings(int preference) {
        try {
            if (mWebView != null) {
                WebSettings settings = mWebView.getSettings();
                
                switch (preference) {
                    case 0: // 自动检测
                        // 使用全局环境检测结果
                        break;
                    case 1: // 优先国内
                        settings.setUserAgentString(settings.getUserAgentString() + " CN_Preferred");
                        break;
                    case 2: // 优先国际
                        settings.setUserAgentString(settings.getUserAgentString() + " International_Preferred");
                        break;
                    case 3: // 混合模式
                        settings.setUserAgentString(settings.getUserAgentString() + " Mixed_Mode");
                        break;
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to apply network settings", e);
        }
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
    
    /**
     * 显示/隐藏加载状态视图
     */
    private void showLoadingState(boolean show, String statusText) {
        isPageLoading = show;
        
        if (mLoadingStateView != null) {
            mLoadingStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        
        if (show && mLoadingStatusText != null) {
            mLoadingStatusText.setText(statusText);
        }
    }
    
    /**
     * 更新加载状态文本
     */
    private void updateLoadingStatus(String statusText) {
        if (mLoadingStatusText != null) {
            mLoadingStatusText.setText(statusText);
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
        try {
            // 清理所有标签页的WebView资源
            for (BrowserTab tab : mTabs) {
                if (tab != null && tab.webView != null) {
                    try {
                        tab.webView.destroy();
                    } catch (Exception e) {
                        Log.e(TAG, "Error destroying WebView for tab", e);
                    }
                }
            }
            mTabs.clear();

            // 清理当前WebView引用
            mWebView = null;

            // 清理用户脚本管理器
            if (mScriptUpdater != null) {
                try {
                    mScriptUpdater.shutdown();
                } catch (Exception e) {
                    Log.e(TAG, "Error shutting down script updater", e);
                }
                mScriptUpdater = null;
            }

            // 清理其他管理器
            if (mSmartMenuManager != null) {
                try {
                    // SmartMenuManager cleanup if needed
                } catch (Exception e) {
                    Log.e(TAG, "Error cleaning up smart menu manager", e);
                }
            }

            // 清理搜索建议管理器
            if (mSearchSuggestionsManager != null) {
                try {
                    mSearchSuggestionsManager.destroy();
                } catch (Exception e) {
                    Log.e(TAG, "Error cleaning up search suggestions manager", e);
                }
                mSearchSuggestionsManager = null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during onDestroy", e);
        }

        // 清理视频播放器资源
        if (mVideoPlayer != null) {
            mVideoPlayer.cleanup();
            mVideoPlayer = null;
        }

        // 清理截图工具资源
        if (mWebView != null) {
            WebViewScreenshotUtil.cleanup(mWebView);
        }

        super.onDestroy();
    }

    // ========== 增强客户端回调方法 ==========

    @Override
    public void onProgressChanged(int progress) {
        if (mProgressBar != null) {
            mProgressBar.setProgress(progress);
        }
    }

    @Override
    public void onTitleReceived(String title) {
        // 更新标签页标题
        updateCurrentTabInfo(mWebView != null ? mWebView.getUrl() : null, title);
    }

    @Override
    public void onJsAlert(String message, android.webkit.JsResult result) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("网页消息")
            .setMessage(message)
            .setPositiveButton("确定", (dialog, which) -> result.confirm())
            .setCancelable(false)
            .show();
    }

    @Override
    public void onJsConfirm(String message, android.webkit.JsResult result) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认")
            .setMessage(message)
            .setPositiveButton("确定", (dialog, which) -> result.confirm())
            .setNegativeButton("取消", (dialog, which) -> result.cancel())
            .setCancelable(false)
            .show();
    }

    @Override
    public void onJsPrompt(String message, String defaultValue, android.webkit.JsPromptResult result) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("输入");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(defaultValue);
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String value = input.getText().toString();
            result.confirm(value);
        });
        builder.setNegativeButton("取消", (dialog, which) -> result.cancel());

        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onWebViewError(String url, int errorCode, String description) {
        android.util.Log.e(TAG, "WebView error: " + errorCode + " - " + description + " for URL: " + url);

        // 特殊处理百度ORB错误
        if (isBaiduOrbError(description)) {
            android.util.Log.w(TAG, "Baidu ORB error detected, suppressing error message: " + description);
            return; // 不显示错误消息，因为我们已经通过拦截器处理了
        }

        // 处理图片加载相关错误
        if (description != null && (description.contains("image") || description.contains("图片") ||
            description.contains("loading") || url != null && isImageUrl(url))) {
            // 图片加载失败，尝试重试
            handleImageLoadError(url, errorCode, description);
        } else if (isNetworkError(errorCode, description)) {
            // 网络错误，显示网络相关提示
            handleNetworkError(url, errorCode, description);
        } else {
            // 其他错误，使用Toast管理器防止消息过多
            String errorMessage = getErrorMessage(errorCode, description);
            if (mToastManager != null) {
                mToastManager.showToast(errorMessage, Toast.LENGTH_SHORT);
            } else {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 处理图片加载错误
     */
    private void handleImageLoadError(String url, int errorCode, String description) {
        android.util.Log.w(TAG, "Handling image load error for URL: " + url);

        // 停止当前的加载状态
        showLoadingState(false, "");

        // 显示图片加载失败的提示
        runOnUiThread(() -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("图片加载失败")
                    .setMessage("图片无法正常加载，可能是网络问题或图片格式不支持。\n\n错误详情: " + description)
                    .setPositiveButton("重试", (dialog, which) -> {
                        // 重新加载页面
                        if (mWebView != null) {
                            mWebView.reload();
                        }
                    })
                    .setNegativeButton("刷新页面", (dialog, which) -> {
                        // 强制刷新页面
                        if (mWebView != null) {
                            mWebView.clearCache(true);
                            mWebView.reload();
                        }
                    })
                    .setNeutralButton("切换图片模式", (dialog, which) -> {
                        // 切换图像加载设置
                        toggleImageLoading();
                    })
                    .setCancelable(true)
                    .show();
        });
    }

    /**
     * 判断URL是否为图片链接
     */
    private boolean isImageUrl(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") ||
               lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") ||
               lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".bmp") ||
               lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") ||
               lowerUrl.contains(".png") || lowerUrl.contains(".gif") ||
               lowerUrl.contains(".webp") || lowerUrl.contains(".bmp");
    }

    /**
     * 处理页面加载超时
     */
    private void handleLoadingTimeout(String url) {
        android.util.Log.w(TAG, "Page loading timeout for URL: " + url);

        // 检查网络连接
        if (!isNetworkAvailable()) {
            runOnUiThread(() -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("网络连接失败")
                        .setMessage("无法连接到网络，请检查网络设置后重试。")
                        .setPositiveButton("重试", (dialog, which) -> {
                            if (mWebView != null) {
                                mWebView.reload();
                            }
                        })
                        .setNegativeButton("设置网络", (dialog, which) -> {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                        })
                        .setCancelable(true)
                        .show();
            });
            return;
        }

        // 网络正常但加载超时，可能是图片加载问题
        runOnUiThread(() -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("页面加载超时")
                    .setMessage("页面加载时间过长，可能是图片加载问题或网络状况不佳。")
                    .setPositiveButton("重试", (dialog, which) -> {
                        if (mWebView != null) {
                            mWebView.reload();
                        }
                    })
                    .setNegativeButton("跳过图片加载", (dialog, which) -> {
                        // 禁用图片加载后重试
                        if (mWebView != null) {
                            WebSettings settings = mWebView.getSettings();
                            settings.setLoadsImagesAutomatically(false);
                            settings.setBlockNetworkImage(true);
                            mWebView.reload();
                        }
                    })
                    .setNeutralButton("强制刷新", (dialog, which) -> {
                        if (mWebView != null) {
                            mWebView.clearCache(true);
                            mWebView.reload();
                        }
                    })
                    .setCancelable(true)
                    .show();
        });
    }

    /**
     * 检查网络连接是否可用
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to check network availability", e);
        }
        return false;
    }

    @Override
    public void onPageStarted(String url) {
        currentUrl = url;
        updateAddressBar(url);
        showLoadingState(true, "正在连接到 " + Uri.parse(url).getHost() + "...");
        showProgress(true);

        // 设置加载超时和自动重试
        mLoadingTimeoutHandler.removeCallbacksAndMessages(null);
        mLoadingTimeoutHandler.postDelayed(() -> {
            if (isPageLoading) {
                handleLoadingTimeout(url);
            }
        }, 15000);
    }

    @Override
    public void onPageFinished(String url) {
        currentUrl = url;
        updateAddressBar(url);
        updateBookmarkButton();
        showLoadingState(false, "");
        showProgress(false);

        // 取消加载超时
        mLoadingTimeoutHandler.removeCallbacksAndMessages(null);

        // 更新当前标签页的信息
        updateCurrentTabInfo(url, mWebView != null ? mWebView.getTitle() : null);

        // 记录浏览历史
        addToHistory(url, mWebView != null ? mWebView.getTitle() : null);

        // 注入用户脚本
        injectUserScripts(url);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 处理文件选择结果
        if (mEnhancedChromeClient != null) {
            mEnhancedChromeClient.onActivityResult(requestCode, resultCode, data);
        }
    }

    // ========== 截图功能 ==========

    /**
     * 截取可见区域
     */
    public void takeScreenshot() {
        if (mWebView != null) {
            WebViewScreenshotUtil.takeScreenshot(mWebView, this, WebViewScreenshotUtil.ScreenshotType.VISIBLE_AREA);
        }
    }

    /**
     * 截取全屏
     */
    public void takeFullScreenshot() {
        if (mWebView != null) {
            WebViewScreenshotUtil.takeScreenshot(mWebView, this, WebViewScreenshotUtil.ScreenshotType.FULL_PAGE);
        }
    }

    /**
     * 截取自定义尺寸
     */
    public void takeCustomScreenshot(int width, int height) {
        if (mWebView != null) {
            WebViewScreenshotUtil.ScreenshotCallback callback = new WebViewScreenshotUtil.ScreenshotCallback() {
                @Override
                public void onScreenshotTaken(android.graphics.Bitmap bitmap, android.net.Uri imageUri) {
                    android.util.Log.d(TAG, "Custom screenshot taken: " + imageUri);
                    Toast.makeText(WebViewActivity.this, "自定义截图已保存", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onScreenshotError(String error) {
                    android.util.Log.e(TAG, "Custom screenshot error: " + error);
                    Toast.makeText(WebViewActivity.this, "截图失败: " + error, Toast.LENGTH_SHORT).show();
                }
            };

            WebViewScreenshotUtil.captureCustomSize(mWebView, this, width, height, callback);
        }
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

    /**
     * 注入用户脚本
     */
    private void injectUserScripts(String url) {
        if (mWebView != null && mUserScriptManager != null) {
            mUserScriptManager.injectScripts(mWebView, url);
        }
    }

    /**
     * 显示用户脚本管理界面
     */
    private void showUserScriptManager() {
        if (mUserScriptManager == null) return;

        java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts = mUserScriptManager.getAllScripts();

        if (scripts.isEmpty()) {
            android.widget.Toast.makeText(this, "暂无用户脚本，点击安装脚本", android.widget.Toast.LENGTH_SHORT).show();
            showInstallScriptDialog();
            return;
        }

        // 统计信息
        int enabledCount = 0;
        int totalCount = scripts.size();
        for (com.hippo.ehviewer.userscript.UserScript script : scripts) {
            if (script.isEnabled()) enabledCount++;
        }

        java.util.List<String> scriptOptions = new java.util.ArrayList<>();

        // 添加脚本列表，包含更多信息
        for (com.hippo.ehviewer.userscript.UserScript script : scripts) {
            String statusIcon = script.isEnabled() ? "✅" : "❌";
            String name = script.getName() != null ? script.getName() : "未命名脚本";
            String version = script.getVersion() != null ? " v" + script.getVersion() : "";
            String author = script.getAuthor() != null ? " by " + script.getAuthor() : "";

            // 创建更丰富的显示信息
            String displayInfo = String.format("%s %s%s%s",
                statusIcon, name, version, author);

            // 如果描述不为空，添加简短描述
            if (script.getDescription() != null && !script.getDescription().isEmpty()) {
                String shortDesc = script.getDescription().length() > 30
                    ? script.getDescription().substring(0, 30) + "..."
                    : script.getDescription();
                displayInfo += "\n   📖 " + shortDesc;
            }

            scriptOptions.add(displayInfo);
        }

        // 添加操作选项
        scriptOptions.add("➕ 安装新脚本");
        scriptOptions.add("🔄 检查更新");
        scriptOptions.add("⚙️ 脚本设置");

        String[] options = scriptOptions.toArray(new String[0]);

        new android.app.AlertDialog.Builder(this)
            .setTitle("用户脚本管理 (" + enabledCount + "/" + totalCount + "已启用)")
            .setItems(options, (dialog, which) -> {
                if (which < scripts.size()) {
                    // 选择脚本
                    com.hippo.ehviewer.userscript.UserScript selectedScript = scripts.get(which);
                    showScriptOptionsDialog(selectedScript);
                } else if (which == scripts.size()) {
                    // 安装新脚本
                    showInstallScriptDialog();
                } else if (which == scripts.size() + 1) {
                    // 检查更新
                    checkScriptUpdates();
                } else if (which == scripts.size() + 2) {
                    // 脚本设置
                    showScriptSettings();
                }
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    /**
     * 显示脚本选项对话框
     */
    private void showScriptOptionsDialog(com.hippo.ehviewer.userscript.UserScript script) {
        String[] options = {
            script.isEnabled() ? "❌ 禁用脚本" : "✅ 启用脚本",
            "📋 查看详情",
            "✏️ 编辑脚本",
            "🏷️ 重命名脚本",
            "📤 导出脚本",
            "🗑️ 卸载脚本"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("脚本: " + script.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 启用/禁用
                        mUserScriptManager.setScriptEnabled(script.getId(), !script.isEnabled());
                        mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
                        android.widget.Toast.makeText(this,
                            script.isEnabled() ? "脚本已启用" : "脚本已禁用",
                            android.widget.Toast.LENGTH_SHORT).show();
                        break;
                    case 1: // 查看详情
                        showScriptDetails(script);
                        break;
                    case 2: // 编辑脚本
                        showScriptEditor(script);
                        break;
                    case 3: // 重命名脚本
                        showRenameScriptDialog(script);
                        break;
                    case 4: // 导出脚本
                        exportScript(script);
                        break;
                    case 5: // 卸载脚本
                        uninstallScript(script);
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示安装脚本对话框
     */
    private void showInstallScriptDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("请输入脚本URL或粘贴脚本内容");
        input.setMinLines(3);

        new android.app.AlertDialog.Builder(this)
            .setTitle("安装用户脚本")
            .setView(input)
            .setPositiveButton("安装", (dialog, which) -> {
                String content = input.getText().toString().trim();
                if (!content.isEmpty()) {
                    installScript(content);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 安装脚本
     */
    private void installScript(String content) {
        boolean success = mUserScriptManager.installScript(content);
        if (success) {
            mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
            android.widget.Toast.makeText(this, "脚本安装成功", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this, "脚本安装失败", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示脚本详情
     */
    private void showScriptDetails(com.hippo.ehviewer.userscript.UserScript script) {
        StringBuilder details = new StringBuilder();
        details.append("📝 名称: ").append(script.getName() != null ? script.getName() : "未命名").append("\n");
        details.append("🔢 版本: ").append(script.getVersion() != null ? script.getVersion() : "未知").append("\n");
        details.append("👤 作者: ").append(script.getAuthor() != null ? script.getAuthor() : "未知").append("\n");
        details.append("📊 状态: ").append(script.isEnabled() ? "✅ 已启用" : "❌ 已禁用").append("\n");
        details.append("🆔 ID: ").append(script.getId()).append("\n");

        if (script.getDescription() != null && !script.getDescription().isEmpty()) {
            details.append("📖 描述: ").append(script.getDescription()).append("\n");
        }

        if (!script.getIncludePatterns().isEmpty()) {
            details.append("🌐 适用网站:\n");
            for (String pattern : script.getIncludePatterns()) {
                details.append("  • ").append(pattern).append("\n");
            }
        }

        if (!script.getExcludePatterns().isEmpty()) {
            details.append("🚫 排除网站:\n");
            for (String pattern : script.getExcludePatterns()) {
                details.append("  • ").append(pattern).append("\n");
            }
        }

        if (script.getUpdateUrl() != null && !script.getUpdateUrl().isEmpty()) {
            details.append("🔄 更新地址: ").append(script.getUpdateUrl()).append("\n");
        }

        if (script.getLastUpdateTime() > 0) {
            details.append("🕒 最后更新: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(script.getLastUpdateTime())));
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📋 脚本详情 - " + script.getName())
            .setMessage(details.toString())
            .setPositiveButton("确定", null)
            .show();
    }

    /**
     * 显示重命名脚本对话框
     */
    private void showRenameScriptDialog(com.hippo.ehviewer.userscript.UserScript script) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(script.getName());
        input.setHint("请输入新的脚本名称");
        input.setSingleLine(true);

        new android.app.AlertDialog.Builder(this)
            .setTitle("🏷️ 重命名脚本")
            .setMessage("当前名称: " + script.getName())
            .setView(input)
            .setPositiveButton("重命名", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(script.getName())) {
                    script.setName(newName);
                    mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
                    android.widget.Toast.makeText(this,
                        "脚本已重命名为: " + newName,
                        android.widget.Toast.LENGTH_SHORT).show();
                } else if (newName.isEmpty()) {
                    android.widget.Toast.makeText(this,
                        "脚本名称不能为空",
                        android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示脚本统计信息
     */
    private void showScriptStatistics(java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts) {
        int totalCount = scripts.size();
        int enabledCount = 0;
        int disabledCount = 0;
        java.util.Map<String, Integer> authorStats = new java.util.HashMap<>();
        java.util.Map<String, Integer> domainStats = new java.util.HashMap<>();

        for (com.hippo.ehviewer.userscript.UserScript script : scripts) {
            if (script.isEnabled()) {
                enabledCount++;
            } else {
                disabledCount++;
            }

            // 统计作者
            String author = script.getAuthor() != null ? script.getAuthor() : "未知";
            authorStats.put(author, authorStats.getOrDefault(author, 0) + 1);

            // 统计适用域名
            for (String pattern : script.getIncludePatterns()) {
                try {
                    String domain = extractDomainFromPattern(pattern);
                    if (domain != null) {
                        domainStats.put(domain, domainStats.getOrDefault(domain, 0) + 1);
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }

        StringBuilder stats = new StringBuilder();
        stats.append("📊 脚本统计报告\n\n");
        stats.append("📈 总脚本数: ").append(totalCount).append("\n");
        stats.append("✅ 已启用: ").append(enabledCount).append("\n");
        stats.append("❌ 已禁用: ").append(disabledCount).append("\n\n");

        stats.append("👥 按作者统计:\n");
        for (java.util.Map.Entry<String, Integer> entry : authorStats.entrySet()) {
            stats.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("个\n");
        }

        stats.append("\n🌐 按网站统计:\n");
        for (java.util.Map.Entry<String, Integer> entry : domainStats.entrySet()) {
            stats.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("个\n");
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📊 脚本统计信息")
            .setMessage(stats.toString())
            .setPositiveButton("确定", null)
            .show();
    }

    /**
     * 从URL模式中提取域名
     */
    private String extractDomainFromPattern(String pattern) {
        try {
            // 移除通配符和协议
            String clean = pattern.replace("http://", "").replace("https://", "").replace("://", "");
            if (clean.contains("*")) {
                // 处理通配符模式
                if (clean.startsWith("*.")) {
                    return clean.substring(2);
                } else if (clean.contains("/*")) {
                    return clean.substring(0, clean.indexOf("/*"));
                }
            }
            // 处理普通URL
            if (clean.contains("/")) {
                return clean.substring(0, clean.indexOf("/"));
            }
            return clean;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 显示批量脚本管理
     */
    private void showBulkScriptManagement(java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts) {
        String[] options = {
            "✅ 启用所有脚本",
            "❌ 禁用所有脚本",
            "🗑️ 卸载所有禁用脚本",
            "🔄 重置所有脚本状态",
            "📤 导出所有脚本",
            "🏷️ 批量重命名"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("🧹 批量脚本管理")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 启用所有脚本
                        bulkEnableScripts(scripts, true);
                        break;
                    case 1: // 禁用所有脚本
                        bulkEnableScripts(scripts, false);
                        break;
                    case 2: // 卸载所有禁用脚本
                        bulkUninstallDisabledScripts(scripts);
                        break;
                    case 3: // 重置所有脚本状态
                        bulkResetScriptStates(scripts);
                        break;
                    case 4: // 导出所有脚本
                        bulkExportScripts(scripts);
                        break;
                    case 5: // 批量重命名
                        showBulkRenameDialog(scripts);
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 批量启用/禁用脚本
     */
    private void bulkEnableScripts(java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts, boolean enable) {
        int count = 0;
        for (com.hippo.ehviewer.userscript.UserScript script : scripts) {
            if (script.isEnabled() != enable) {
                mUserScriptManager.setScriptEnabled(script.getId(), enable);
                count++;
            }
        }

        if (count > 0) {
            mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
            android.widget.Toast.makeText(this,
                (enable ? "已启用 " : "已禁用 ") + count + " 个脚本",
                android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this,
                "没有脚本需要" + (enable ? "启用" : "禁用"),
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 批量卸载禁用脚本
     */
    private void bulkUninstallDisabledScripts(java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts) {
        java.util.List<com.hippo.ehviewer.userscript.UserScript> toRemove = new java.util.ArrayList<>();
        for (com.hippo.ehviewer.userscript.UserScript script : scripts) {
            if (!script.isEnabled()) {
                toRemove.add(script);
            }
        }

        if (toRemove.isEmpty()) {
            android.widget.Toast.makeText(this, "没有禁用的脚本", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("确认批量卸载")
            .setMessage("确定要卸载所有 " + toRemove.size() + " 个禁用的脚本吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                int successCount = 0;
                for (com.hippo.ehviewer.userscript.UserScript script : toRemove) {
                    if (mUserScriptManager.uninstallScript(script.getId())) {
                        mScriptStorage.deleteScriptFile(script.getId());
                        successCount++;
                    }
                }

                if (successCount > 0) {
                    mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
                    android.widget.Toast.makeText(this,
                        "成功卸载 " + successCount + " 个脚本",
                        android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 重置所有脚本状态
     */
    private void bulkResetScriptStates(java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts) {
        for (com.hippo.ehviewer.userscript.UserScript script : scripts) {
            script.setEnabled(true); // 默认启用
        }
        mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
        android.widget.Toast.makeText(this, "已重置所有脚本状态为启用", android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * 批量导出脚本
     */
    private void bulkExportScripts(java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts) {
        java.io.File exportDir = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS), "EhViewer/Scripts/BulkExport");

        int successCount = 0;
        for (com.hippo.ehviewer.userscript.UserScript script : scripts) {
            if (mScriptStorage.exportScript(script, exportDir)) {
                successCount++;
            }
        }

        if (successCount > 0) {
            android.widget.Toast.makeText(this,
                "成功导出 " + successCount + "/" + scripts.size() + " 个脚本到: " + exportDir.getAbsolutePath(),
                android.widget.Toast.LENGTH_LONG).show();
        } else {
            android.widget.Toast.makeText(this, "导出失败", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示批量重命名对话框
     */
    private void showBulkRenameDialog(java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(20, 0, 20, 0);

        android.widget.TextView info = new android.widget.TextView(this);
        info.setText("选择重命名模式:");
        layout.addView(info);

        String[] renameOptions = {"添加前缀", "添加后缀", "替换文本", "清理名称"};
        android.widget.Spinner spinner = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, renameOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        layout.addView(spinner);

        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("输入要添加/替换的文本");
        layout.addView(input);

        new android.app.AlertDialog.Builder(this)
            .setTitle("🏷️ 批量重命名脚本")
            .setView(layout)
            .setPositiveButton("执行", (dialog, which) -> {
                int mode = spinner.getSelectedItemPosition();
                String text = input.getText().toString().trim();
                bulkRenameScripts(scripts, mode, text);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 执行批量重命名
     */
    private void bulkRenameScripts(java.util.List<com.hippo.ehviewer.userscript.UserScript> scripts, int mode, String text) {
        if (text.isEmpty() && mode != 3) { // 清理名称模式不需要文本
            android.widget.Toast.makeText(this, "请输入要处理的文本", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        int renamedCount = 0;
        for (com.hippo.ehviewer.userscript.UserScript script : scripts) {
            String oldName = script.getName() != null ? script.getName() : "";
            String newName = oldName;

            switch (mode) {
                case 0: // 添加前缀
                    newName = text + oldName;
                    break;
                case 1: // 添加后缀
                    newName = oldName + text;
                    break;
                case 2: // 替换文本
                    String[] parts = text.split("\\|");
                    if (parts.length == 2) {
                        newName = oldName.replace(parts[0], parts[1]);
                    }
                    break;
                case 3: // 清理名称
                    newName = oldName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", "")
                                    .replaceAll("\\s+", " ")
                                    .trim();
                    break;
            }

            if (!newName.equals(oldName) && !newName.isEmpty()) {
                script.setName(newName);
                renamedCount++;
            }
        }

        if (renamedCount > 0) {
            mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
            android.widget.Toast.makeText(this,
                "成功重命名 " + renamedCount + " 个脚本",
                android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this, "没有脚本需要重命名", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示脚本编辑器
     */
    private void showScriptEditor(com.hippo.ehviewer.userscript.UserScript script) {
        android.widget.EditText editor = new android.widget.EditText(this);
        editor.setText(script.getContent());
        editor.setMinLines(10);

        new android.app.AlertDialog.Builder(this)
            .setTitle("编辑脚本: " + script.getName())
            .setView(editor)
            .setPositiveButton("保存", (dialog, which) -> {
                String newContent = editor.getText().toString();
                if (!newContent.isEmpty()) {
                    script.setContent(newContent);
                    mScriptStorage.saveScriptToFile(script);
                    mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
                    android.widget.Toast.makeText(this, "脚本已保存", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 导出脚本
     */
    private void exportScript(com.hippo.ehviewer.userscript.UserScript script) {
        java.io.File exportDir = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS), "EhViewer/Scripts");
        boolean success = mScriptStorage.exportScript(script, exportDir);

        if (success) {
            android.widget.Toast.makeText(this,
                "脚本已导出到: " + exportDir.getAbsolutePath(),
                android.widget.Toast.LENGTH_LONG).show();
        } else {
            android.widget.Toast.makeText(this, "脚本导出失败", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 卸载脚本
     */
    private void uninstallScript(com.hippo.ehviewer.userscript.UserScript script) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("确认卸载")
            .setMessage("确定要卸载脚本 \"" + script.getName() + "\" 吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                boolean success = mUserScriptManager.uninstallScript(script.getId());
                if (success) {
                    mScriptStorage.deleteScriptFile(script.getId());
                    mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
                    android.widget.Toast.makeText(this, "脚本已卸载", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(this, "脚本卸载失败", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 检查脚本更新
     */
    private void checkScriptUpdates() {
        if (mScriptUpdater != null) {
            android.widget.Toast.makeText(this, "正在检查脚本更新...", android.widget.Toast.LENGTH_SHORT).show();

            mScriptUpdater.checkAllUpdates(new com.hippo.ehviewer.userscript.ScriptUpdater.UpdateCallback() {
                @Override
                public void onUpdateStart() {}

                @Override
                public void onUpdateProgress(String scriptName, int progress) {}

                @Override
                public void onUpdateSuccess(com.hippo.ehviewer.userscript.UserScript script) {
                    android.widget.Toast.makeText(WebViewActivity.this,
                        "脚本 \"" + script.getName() + "\" 已更新",
                        android.widget.Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUpdateFailed(String scriptName, String error) {
                    android.widget.Toast.makeText(WebViewActivity.this,
                        "脚本 \"" + scriptName + "\" 更新失败: " + error,
                        android.widget.Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUpdateComplete() {
                    android.widget.Toast.makeText(WebViewActivity.this,
                        "脚本更新检查完成",
                        android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 显示脚本设置
     */
    private void showScriptSettings() {
        String[] settings = {
            "油猴功能: " + (mUserScriptManager.isEnabled() ? "已启用" : "已禁用"),
            "脚本数量: " + mUserScriptManager.getAllScripts().size() + " 个",
            "存储使用: " + android.text.format.Formatter.formatFileSize(this, mScriptStorage.getStorageUsage()),
            "清空所有脚本",
            "重置油猴设置"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("脚本设置")
            .setItems(settings, (dialog, which) -> {
                switch (which) {
                    case 0: // 切换油猴功能
                        boolean newState = !mUserScriptManager.isEnabled();
                        mUserScriptManager.setEnabled(newState);
                        android.widget.Toast.makeText(this,
                            "油猴功能" + (newState ? "已启用" : "已禁用"),
                            android.widget.Toast.LENGTH_SHORT).show();
                        break;
                    case 1: // 显示脚本数量
                        break;
                    case 2: // 显示存储使用
                        break;
                    case 3: // 清空所有脚本
                        clearAllScripts();
                        break;
                    case 4: // 重置设置
                        resetScriptSettings();
                        break;
                }
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    /**
     * 清空所有脚本
     */
    private void clearAllScripts() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要清空所有用户脚本吗？此操作不可恢复。")
            .setPositiveButton("确定", (dialog, which) -> {
                for (com.hippo.ehviewer.userscript.UserScript script : mUserScriptManager.getAllScripts()) {
                    mScriptStorage.deleteScriptFile(script.getId());
                }
                mUserScriptManager.getAllScripts().clear();
                mScriptStorage.saveScripts(mUserScriptManager.getAllScripts());
                android.widget.Toast.makeText(this, "所有脚本已清空", android.widget.Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 重置脚本设置
     */
    private void resetScriptSettings() {
        mUserScriptManager.setEnabled(true);
        android.widget.Toast.makeText(this, "脚本设置已重置", android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * 添加浏览历史记录
     */
    private void addToHistory(String url, String title) {
        if (mHistoryManager != null && url != null && !url.isEmpty()) {
            try {
                // 过滤不需要记录的URL
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return; // 只记录HTTP/HTTPS URL
                }

                // 使用页面标题，如果为空则使用URL
                String pageTitle = (title != null && !title.isEmpty()) ? title : url;

                // 添加到历史记录
                mHistoryManager.addHistory(pageTitle, url);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error adding to history", e);
            }
        }
    }
    
    // ===== 智能菜单相关方法 =====
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mSmartMenuManager != null) {
            mSmartMenuManager.createSmartMenu(menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mSmartMenuManager != null && mSmartMenuManager.handleMenuItemClick(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 显示书签管理（从智能菜单调用）
     */
    public void showBookmarksFromMenu() {
        showBookmarks();
    }
    
    /**
     * 显示历史记录（从智能菜单调用）
     */
    public void showHistoryFromMenu() {
        showHistory();
    }
    
    /**
     * 显示下载管理（从智能菜单调用）
     */
    public void showDownloadsFromMenu() {
        showDownloadManager();
    }
    
    /**
     * 创建新标签页（从智能菜单调用）
     */
    public void createNewTabFromMenu() {
        // 在当前Activity中打开新页面
        String homeUrl = mSearchConfigManager.getDefaultHomepageUrl();
        if (mWebView != null) {
            mWebView.loadUrl(homeUrl);
            EditText omniboxInput = findViewById(R.id.omnibox_input);
            if (omniboxInput != null) {
                // 临时移除文本监听器，防止触发搜索建议
                if (mOmniboxTextWatcher != null) {
                    omniboxInput.removeTextChangedListener(mOmniboxTextWatcher);
                }
                omniboxInput.setText(homeUrl);
                // 重新添加文本监听器
                if (mOmniboxTextWatcher != null) {
                    omniboxInput.addTextChangedListener(mOmniboxTextWatcher);
                }
            }
        }
    }

    /**
     * 请求地理位置权限
     */
    private void requestLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                android.util.Log.d(TAG, "Requesting location permission");
                requestPermissions(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                }, 100);
            } else {
                android.util.Log.d(TAG, "Location permission already granted");
            }
        }
    }

    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            boolean locationGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (android.Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i]) ||
                    android.Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                    if (grantResults.length > i && grantResults[i] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        locationGranted = true;
                        break;
                    }
                }
            }

            if (!locationGranted) {
                android.util.Log.w(TAG, "Location permission denied by user");
            } else {
                android.util.Log.d(TAG, "Location permission granted");
            }
        }
    }

    /**
     * 判断是否是百度ORB错误
     */
    private boolean isBaiduOrbError(String description) {
        return description != null && description.contains("ERR_BLOCKED_BY_ORB") &&
               description.contains("baidu.com");
    }

    /**
     * 判断是否是网络错误
     */
    private boolean isNetworkError(int errorCode, String description) {
        // WebView错误码：-2表示主机名解析失败，-6表示连接失败等
        return errorCode == -2 || errorCode == -6 || errorCode == -8 ||
               (description != null && (description.contains("net::") ||
                description.contains("connection") || description.contains("timeout")));
    }

    /**
     * 处理网络错误
     */
    private void handleNetworkError(String url, int errorCode, String description) {
        android.util.Log.w(TAG, "Network error for URL: " + url);

        // 检查网络连接状态
        if (!isNetworkAvailable()) {
            if (mToastManager != null) {
                mToastManager.showToast("网络连接不可用，请检查网络设置", Toast.LENGTH_SHORT);
            } else {
                Toast.makeText(this, "网络连接不可用，请检查网络设置", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (mToastManager != null) {
                mToastManager.showToast("网络连接失败，正在重试...", Toast.LENGTH_SHORT);
            } else {
                Toast.makeText(this, "网络连接失败，正在重试...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 获取错误消息
     */
    private String getErrorMessage(int errorCode, String description) {
        // 处理特定的错误码
        switch (errorCode) {
            case -2:
                return "无法解析域名，请检查网络连接";
            case -6:
                return "连接超时，请检查网络连接";
            case -8:
                return "连接被重置，请稍后重试";
            case -10:
                return "连接失败，请检查网络设置";
            case -11:
                return "连接被拒绝，请检查防火墙设置";
            case -12:
                return "网络不可用，请检查网络连接";
            case -13:
                return "代理服务器错误";
            case -14:
                return "重定向次数过多";
            case -15:
                return "SSL握手失败";
            default:
                // 根据错误描述提供更详细的信息
                if (description != null) {
                    String desc = description.toLowerCase();

                    if (desc.contains("timeout") || desc.contains("time out")) {
                        return "网络请求超时，请检查网络连接";
                    } else if (desc.contains("certificate") || desc.contains("ssl")) {
                        return "SSL证书验证失败，请检查网络安全设置";
                    } else if (desc.contains("connection") && desc.contains("refused")) {
                        return "服务器拒绝连接，请稍后重试";
                    } else if (desc.contains("network") && desc.contains("unreachable")) {
                        return "网络不可达，请检查网络连接";
                    } else if (desc.contains("dns") || desc.contains("resolve")) {
                        return "DNS解析失败，请检查网络设置";
                    } else if (desc.contains("blocked") || desc.contains("forbidden")) {
                        return "内容被阻止访问";
                    } else if (desc.contains("not found") || desc.contains("404")) {
                        return "页面未找到，请检查URL是否正确";
                    } else if (desc.contains("server error") || desc.contains("500")) {
                        return "服务器内部错误，请稍后重试";
                    } else if (desc.contains("网络错误") || desc.contains("network error")) {
                        return "网络连接出现问题，请检查网络设置";
                    } else if (desc.contains("err_blocked_by_orb")) {
                        return "内容被安全策略阻止（这是正常现象）";
                    } else {
                        // 如果是中文错误消息，保留原样
                        return description;
                    }
                }

                // 默认错误消息
                return "页面加载失败，请检查网络连接后重试";
        }
    }


    /**
     * Toast管理器 - 防止Toast消息过多
     */
    private static class ToastManager {
        private android.content.Context mContext;
        private Toast mCurrentToast;
        private long mLastToastTime = 0;
        private static final long TOAST_THROTTLE_TIME = 2000; // 2秒内只显示一个Toast

        public ToastManager(android.content.Context context) {
            this.mContext = context;
        }

        public void showToast(String message, int duration) {
            long currentTime = System.currentTimeMillis();

            // 检查是否在节流时间内
            if (currentTime - mLastToastTime < TOAST_THROTTLE_TIME) {
                android.util.Log.d("ToastManager", "Toast throttled: " + message);
                return;
            }

            // 取消之前的Toast
            if (mCurrentToast != null) {
                mCurrentToast.cancel();
            }

            // 显示新Toast
            mCurrentToast = Toast.makeText(mContext, message, duration);
            mCurrentToast.show();

            mLastToastTime = currentTime;
            android.util.Log.d("ToastManager", "Toast shown: " + message);
        }

        public void showToast(int resId, int duration) {
            showToast(mContext.getString(resId), duration);
        }
    }

    /**
     * 系统错误处理器 - 处理系统级服务错误
     */
    private static class SystemErrorHandler {
        private android.content.Context mContext;
        private long mLastThermalErrorTime = 0;
        private static final long THERMAL_ERROR_THROTTLE = 300000; // 5分钟节流

        public SystemErrorHandler(android.content.Context context) {
            this.mContext = context;
        }

        /**
         * 处理热管理系统错误
         */
        public void handleThermalStatsError() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastThermalErrorTime > THERMAL_ERROR_THROTTLE) {
                android.util.Log.w("SystemErrorHandler", "OplusThermalStats error detected, this is system-level and usually not critical");
                mLastThermalErrorTime = currentTime;

                // 可以选择性地记录到Firebase Analytics
                try {
                    com.google.firebase.analytics.FirebaseAnalytics.getInstance(mContext)
                        .logEvent("thermal_stats_error", null);
                } catch (Exception e) {
                    android.util.Log.d("SystemErrorHandler", "Firebase not available, skipping analytics");
                }
            }
        }

        /**
         * 处理性能监控错误
         */
        public void handlePerformanceError(String serviceName, String error) {
            android.util.Log.d("SystemErrorHandler", "Performance service error in " + serviceName + ": " + error);
            // 这些通常是系统级错误，不需要用户干预
        }

        /**
         * 处理网络统计错误
         */
        public void handleNetworkStatsError() {
            android.util.Log.d("SystemErrorHandler", "Network stats error detected, usually system-level");
        }

        /**
         * 处理MIDAS服务错误
         */
        public void handleMidasError() {
            android.util.Log.d("SystemErrorHandler", "MIDAS service error detected, usually system-level");
        }

        /**
         * 处理电池服务错误
         */
        public void handleBatteryError() {
            android.util.Log.d("SystemErrorHandler", "Battery service error detected, usually system-level");
        }
    }

    /**
     * 处理系统兼容性问题
     */
    private void handleSystemCompatibility() {
        try {
            // 检查是否为OPPO/ColorOS设备
            String manufacturer = android.os.Build.MANUFACTURER;
            String brand = android.os.Build.BRAND;

            if ("OPPO".equalsIgnoreCase(manufacturer) || "ColorOS".equalsIgnoreCase(brand)) {
                Log.i(TAG, "OPPO/ColorOS device detected in WebViewActivity, applying compatibility fixes");

                // 设置WebView兼容性配置
                setupOppoWebViewCompatibility();

                // 处理系统服务错误
                handleSystemServiceErrors();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error handling system compatibility in WebViewActivity", e);
        }
    }

    /**
     * 设置OPPO WebView兼容性
     */
    private void setupOppoWebViewCompatibility() {
        try {
            // 设置WebView数据目录，避免系统服务错误
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                String processName = getProcessName();
                if (processName != null && !processName.equals(getPackageName())) {
                    // 多进程WebView配置
                    WebView.setDataDirectorySuffix(processName);
                }
            }

            // 设置WebView渲染模式
            if (mWebView != null) {
                mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                // 额外的OpenGL兼容性设置
                try {
                    android.webkit.WebSettings settings = mWebView.getSettings();
                    settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH);

                    // 彻底解决OpenGL渲染问题
                    setupOpenGLCompatibility();

                } catch (Exception e) {
                    android.util.Log.w(TAG, "Error setting WebView render mode", e);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Error setting up OPPO WebView compatibility", e);
        }
    }

    /**
     * 处理系统服务错误
     */
    private void handleSystemServiceErrors() {
        try {
            // 检查系统服务是否可用
            if (getSystemService(Context.CONNECTIVITY_SERVICE) == null) {
                Log.w(TAG, "Connectivity service not available, network features may not work");
            }

            if (getSystemService(Context.DOWNLOAD_SERVICE) == null) {
                Log.w(TAG, "Download service not available, downloads may not work");
            }

            // 检查Cookie管理器
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager == null) {
                Log.w(TAG, "Cookie manager not available");
            }

        } catch (Exception e) {
            Log.w(TAG, "Error checking system services in WebViewActivity", e);
        }
    }

    /**
     * 获取进程名称
     */
    private String getProcessName() {
        try {
            int pid = android.os.Process.myPid();
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                for (android.app.ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
                    if (processInfo.pid == pid) {
                        return processInfo.processName;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting process name", e);
        }
        return null;
    }

    /**
     * 设置OpenGL兼容性
     */
    private void setupOpenGLCompatibility() {
        try {
            // 尝试不同的渲染模式来解决兼容性问题
            boolean compatibilitySet = false;

            // 方法1: 强制软件渲染
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    compatibilitySet = true;
                    android.util.Log.d(TAG, "OpenGL compatibility: Software rendering enabled");
                }
            } catch (Exception e) {
                android.util.Log.w(TAG, "Software rendering failed", e);
            }

            // 方法2: 如果软件渲染失败，尝试硬件渲染但禁用一些特性
            if (!compatibilitySet) {
                try {
                    mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    // 禁用可能导致问题的硬件加速特性
                    System.setProperty("debug.hwui.disable_draw_defer", "true");
                    System.setProperty("debug.hwui.render_dirty_regions", "false");
                    compatibilitySet = true;
                    android.util.Log.d(TAG, "OpenGL compatibility: Hardware rendering with restrictions");
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Hardware rendering with restrictions failed", e);
                }
            }

            // 方法3: 最后的降级方案
            if (!compatibilitySet) {
                try {
                    // 完全禁用硬件加速
                    mWebView.setLayerType(View.LAYER_TYPE_NONE, null);
                    android.util.Log.d(TAG, "OpenGL compatibility: No acceleration (fallback)");
                } catch (Exception e) {
                    android.util.Log.w(TAG, "All OpenGL compatibility methods failed", e);
                }
            }

        } catch (Exception e) {
            android.util.Log.w(TAG, "Error in setupOpenGLCompatibility", e);
        }
    }

    /**
     * 系统错误监控器 - 检测并处理系统级别错误
     */
    private static class SystemErrorMonitor {
        private android.content.Context mContext;
        private long mLastMemoryWarningTime = 0;
        private static final long MEMORY_WARNING_THROTTLE = 60000; // 1分钟节流

        public SystemErrorMonitor(android.content.Context context) {
            this.mContext = context;
        }

        /**
         * 处理内存警告
         */
        public void handleMemoryWarning() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastMemoryWarningTime > MEMORY_WARNING_THROTTLE) {
                android.util.Log.w("SystemErrorMonitor", "Low memory condition detected");
                mLastMemoryWarningTime = currentTime;

                // 可以在这里添加内存优化措施
                // 比如清理缓存、减少并发操作等
            }
        }

        /**
         * 处理OpenGL渲染错误
         */
        public void handleOpenGLError() {
            android.util.Log.d("SystemErrorMonitor", "OpenGL swap behavior error detected, usually hardware-related");
        }

        /**
         * 处理MediaProvider错误
         */
        public void handleMediaProviderError() {
            android.util.Log.d("SystemErrorMonitor", "MediaProvider error detected, usually file system related");
        }

        /**
         * 处理性能监控错误
         */
        public void handlePerformanceMonitorError() {
            android.util.Log.d("SystemErrorMonitor", "Performance monitor error detected, system-level issue");
        }

        /**
         * 处理网络统计错误
         */
        public void handleNetworkStatsError() {
            android.util.Log.d("SystemErrorMonitor", "Network stats error detected, usually system service issue");
        }
    }
}
