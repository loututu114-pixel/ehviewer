package com.hippo.ehviewer.ui;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.AdBlockManager;
import com.hippo.ehviewer.client.BookmarkManager;
import com.hippo.ehviewer.client.data.BookmarkInfo;
import com.hippo.ehviewer.client.EnhancedWebViewManager;
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.client.ImageLazyLoader;
import com.hippo.ehviewer.client.JavaScriptOptimizer;
import com.hippo.ehviewer.client.MemoryManager;
import com.hippo.ehviewer.client.NetworkDetector;
import com.hippo.ehviewer.client.ReadingModeManager;
import com.hippo.ehviewer.client.SearchEngineManager;
import com.hippo.ehviewer.client.WebViewCacheManager;
import com.hippo.ehviewer.client.WebViewPoolManager;
import com.hippo.ehviewer.client.X5WebViewManager;
import com.hippo.ehviewer.util.DomainSuggestionManager;
import com.hippo.ehviewer.util.YouTubeCompatibilityManager;
import com.hippo.ehviewer.util.WebViewErrorHandler;
import com.hippo.ehviewer.util.VideoPlayerEnhancer;
import com.hippo.ehviewer.util.SmartUrlProcessor;
import com.hippo.ehviewer.util.UserAgentManager;
import com.hippo.util.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;

public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";
    public static final String EXTRA_URL = "url";
    private static final String EXTRA_HTML_CONTENT = "html_content";
    private static final String EXTRA_IS_PREVIEW = "is_preview";
    private static final String EXTRA_FROM_BROWSER_LAUNCHER = "from_browser_launcher";
    private static final String EXTRA_BROWSER_MODE = "browser_mode";

    // UI控件
    private AutoCompleteTextView mUrlInput;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageButton mHomeButton;
    private ImageButton mRefreshButton;
    private ImageButton mTopRefreshButton;
    private ImageButton mMenuButton;
    private ImageButton mBookmarkButton;
    private android.widget.HorizontalScrollView mTabScrollView;
    private android.widget.LinearLayout mTabContainer;

    // 管理器
    private BookmarkManager mBookmarkManager;
    private HistoryManager mHistoryManager;
    private AdBlockManager mAdBlockManager;
    private X5WebViewManager mX5WebViewManager;
    private WebViewPoolManager mWebViewPoolManager;
    private JavaScriptOptimizer mJavaScriptOptimizer;
    private WebViewCacheManager mWebViewCacheManager;
    private ImageLazyLoader mImageLazyLoader;
    private MemoryManager mMemoryManager;
    private ReadingModeManager mReadingModeManager;
    private EnhancedWebViewManager mEnhancedWebViewManager;

    // 标签页管理
    private List<TabData> mTabs = new ArrayList<>();
    private int mCurrentTabIndex = -1;

    // 网络检测和搜索引擎管理
    private NetworkDetector mNetworkDetector;
    private SearchEngineManager mSearchEngineManager;

    // 域名补全管理器
    private UrlSuggestionAdapter mUrlSuggestionAdapter;

    // 错误处理器和视频增强器
    private WebViewErrorHandler mErrorHandler;
    private VideoPlayerEnhancer mVideoEnhancer;
    private SmartUrlProcessor mSmartUrlProcessor;
    private UserAgentManager mUserAgentManager;

    // 静态数据类
    private static class TabData {
        WebView webView;
        EnhancedWebViewManager enhancedWebViewManager;
        String title;
        String url;

        TabData(WebView webView, EnhancedWebViewManager manager) {
            this.webView = webView;
            this.enhancedWebViewManager = manager;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.util.Log.d("WebViewActivity", "onCreate called");

        try {
            setContentView(R.layout.activity_web_view);

            // 初始化标签页容器
            mTabScrollView = findViewById(R.id.tab_scroll_view);
            mTabContainer = findViewById(R.id.tab_container);

            // 初始化书签、历史记录、广告拦截和X5管理器
            mBookmarkManager = BookmarkManager.getInstance(this);
            mHistoryManager = HistoryManager.getInstance(this);
            mAdBlockManager = AdBlockManager.getInstance();
            mX5WebViewManager = X5WebViewManager.getInstance();

            // 初始化性能优化管理器
            mWebViewPoolManager = WebViewPoolManager.getInstance(this);
            mJavaScriptOptimizer = JavaScriptOptimizer.getInstance();
            mWebViewCacheManager = WebViewCacheManager.getInstance(this);
            mImageLazyLoader = ImageLazyLoader.getInstance();
            mMemoryManager = MemoryManager.getInstance(this);
            mReadingModeManager = ReadingModeManager.getInstance(this);

            // 初始化UI控件
            initializeViews();
            // 初始化增强WebView管理器（暂时设为null，会在创建标签页时设置）
            mEnhancedWebViewManager = null;

            // 预热WebView池
            mWebViewPoolManager.warmUpPool();

            // 处理Intent参数
            Intent intent = getIntent();
            String url = intent.getStringExtra(EXTRA_URL);
            String htmlContent = intent.getStringExtra("html_content");
            boolean isPreview = intent.getBooleanExtra("is_preview", false);
            Uri data = intent.getData();
            boolean fromBrowserLauncher = intent.getBooleanExtra("from_browser_launcher", false);
            boolean browserMode = intent.getBooleanExtra("browser_mode", false);

            android.util.Log.d("WebViewActivity", "URL to load: " + url + ", data: " + data +
                              ", from browser launcher: " + fromBrowserLauncher + ", is preview: " + isPreview);

            // 处理HTML内容预览
            if (htmlContent != null && !htmlContent.isEmpty()) {
                android.util.Log.d("WebViewActivity", "Loading HTML content preview");
                createNewTabWithHtmlContent(htmlContent, isPreview ? "阅读模式预览" : "HTML内容");
            } else {
                // 优先使用intent data（来自外部链接）
                if (data != null) {
                    url = data.toString();
                    android.util.Log.d("WebViewActivity", "Using URL from intent data: " + url);

                    // 如果是来自浏览器启动器的请求，显示浏览器模式提示
                    if (fromBrowserLauncher) {
                        showBrowserModeToast();
                    }
                }

                if (!TextUtils.isEmpty(url)) {
                    // 创建第一个标签页
                    createNewTab(url);

                    // 初始化标签页UI状态
                    updateTabUI();
                } else {
                    // 创建默认标签页（使用智能搜索引擎选择）
                    String defaultUrl = getSmartHomeUrl();
                    android.util.Log.d("WebViewActivity", "Using smart default URL: " + defaultUrl);
                    createNewTab(defaultUrl);

                    // 初始化标签页UI状态
                    updateTabUI();
                }
            }

            // 注册内存监听器
            setupMemoryListeners();

            // 紧凑布局：隐藏默认ActionBar，直接使用布局中的组件
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onCreate", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    /**
     * 初始化UI控件
     */
    private void initializeViews() {
        // 初始化UI控件
        mUrlInput = findViewById(R.id.url_input);

        // 初始化URL补全适配器
        setupUrlAutoComplete();
            mProgressBar = findViewById(R.id.progress_bar);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
            mBackButton = findViewById(R.id.back_button);
            mForwardButton = findViewById(R.id.forward_button);
            mHomeButton = findViewById(R.id.home_button);
        mRefreshButton = findViewById(R.id.refresh_button);
            mTopRefreshButton = findViewById(R.id.top_refresh_button);
            mMenuButton = findViewById(R.id.menu_button);
            mBookmarkButton = findViewById(R.id.bookmark_button);

        // 设置UI控件监听器
        setupUIListeners();
    }

    /**
     * 设置URL自动补全
     */
    private void setupUrlAutoComplete() {
        if (mUrlInput != null) {
            // 创建URL建议适配器
            mUrlSuggestionAdapter = new UrlSuggestionAdapter(this);
            mUrlInput.setAdapter(mUrlSuggestionAdapter);

            // 设置补全选择监听器
            mUrlInput.setOnItemClickListener((parent, view, position, id) -> {
                try {
                    DomainSuggestionManager.SuggestionItem selectedItem =
                        mUrlSuggestionAdapter.getItem(position);

                    if (selectedItem != null) {
                        String selectedUrl = selectedItem.url;

                        // 隐藏键盘
                        android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(mUrlInput.getWindowToken(), 0);
                        }

                        // 加载选中的URL
                        loadUrlInCurrentTab(selectedUrl);

                        // 更新域名使用频率
                        String domain = extractDomainFromUrl(selectedUrl);
                        if (domain != null) {
                            mUrlSuggestionAdapter.mSuggestionManager.increaseDomainPopularity(domain);
                        }

                        // 失去焦点
                        mUrlInput.clearFocus();
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error handling URL suggestion selection", e);
                }
            });

            // 设置点击监听器 - 处理about:blank的特殊情况
            mUrlInput.setOnClickListener(v -> {
                String currentText = mUrlInput.getText().toString().trim();
                boolean isAboutBlank = "about:blank".equals(currentText);

                if (isAboutBlank && !mUrlInput.hasFocus()) {
                    // 如果是about:blank且还没有焦点，清空文本
                    mUrlInput.setText("");
                    mUrlInput.requestFocus();
                }
            });

            // 设置下拉列表显示时的监听器
            mUrlInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // 检查当前文本是否是about:blank
                    String currentText = mUrlInput.getText().toString().trim();
                    boolean isAboutBlank = "about:blank".equals(currentText);

                    if (isAboutBlank) {
                        // 对于about:blank，清空文本让用户直接输入
                        mUrlInput.setText("");
                    } else if (!currentText.isEmpty()) {
                        // 只有在有内容且不是about:blank时才全选文本
                        mUrlInput.selectAll();
                    }

                    // 添加轻微的缩放动画效果
                    v.animate()
                     .scaleX(1.02f)
                     .scaleY(1.02f)
                     .setDuration(150)
                     .start();
                } else {
                    // 失去焦点时恢复原始大小
                    v.animate()
                     .scaleX(1.0f)
                     .scaleY(1.0f)
                     .setDuration(150)
                     .start();
                }
            });
        }
    }

    /**
     * 提取URL中的域名
     */
    private String extractDomainFromUrl(String url) {
        if (url == null) return null;

        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }

            int slashIndex = url.indexOf('/');
            if (slashIndex > 0) {
                url = url.substring(0, slashIndex);
            }

            return url;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error extracting domain from URL", e);
            return null;
        }
    }

    /**
     * 设置UI控件监听器
     */
    private void setupUIListeners() {
            // 设置地址栏
            if (mUrlInput != null) {
                mUrlInput.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                        String url = mUrlInput.getText().toString().trim();
                        if (!url.isEmpty()) {
                        loadUrlInCurrentTab(url);
                        // 隐藏键盘
                        android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(mUrlInput.getWindowToken(), 0);
                        }
                        // 失去焦点
                        mUrlInput.clearFocus();
                    }
                    return true;
                    }
                    return false;
                });

                // 长按地址栏显示快速访问菜单
                mUrlInput.setOnLongClickListener(v -> {
                    showQuickAccessMenu();
                    return true;
                });
            }

        // 设置按钮监听器 - 添加现代化的点击效果
            if (mRefreshButton != null) {
                mRefreshButton.setOnClickListener(v -> {
                // 点击动画效果
                animateButtonClick(v);
                TabData currentTab = getCurrentTab();
                if (currentTab != null && currentTab.webView != null) {
                    currentTab.webView.reload();
                    }
                });
            }

            if (mBackButton != null) {
                mBackButton.setOnClickListener(v -> {
                animateButtonClick(v);
                TabData currentTab = getCurrentTab();
                if (currentTab != null && currentTab.webView != null && currentTab.webView.canGoBack()) {
                    currentTab.webView.goBack();
                    }
                });
            }

            if (mForwardButton != null) {
                mForwardButton.setOnClickListener(v -> {
                animateButtonClick(v);
                TabData currentTab = getCurrentTab();
                if (currentTab != null && currentTab.webView != null && currentTab.webView.canGoForward()) {
                    currentTab.webView.goForward();
                    }
                });
            }

            if (mHomeButton != null) {
                mHomeButton.setOnClickListener(v -> {
                animateButtonClick(v);
                TabData currentTab = getCurrentTab();
                if (currentTab != null && currentTab.webView != null) {
                    // 智能选择主页搜索引擎
                    String homeUrl = getSmartHomeUrl();
                    loadUrlInCurrentTab(homeUrl);
                    }
                });
            }

            if (mTopRefreshButton != null) {
                mTopRefreshButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    TabData currentTab = getCurrentTab();
                    if (currentTab != null && currentTab.webView != null) {
                        currentTab.webView.reload();
                    }
                });
            }



            if (mMenuButton != null) {
                mMenuButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    android.util.Log.d("WebViewActivity", "Menu button clicked");
                    showMenuDialog();
                });
                // 确保菜单按钮始终可见和可用
                mMenuButton.setVisibility(View.VISIBLE);
                mMenuButton.setEnabled(true);
                android.util.Log.d("WebViewActivity", "Menu button initialized successfully");
            } else {
                android.util.Log.e("WebViewActivity", "Menu button is null!");
            }

            if (mBookmarkButton != null) {
                mBookmarkButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    android.util.Log.d("WebViewActivity", "Bookmark button clicked");
                    toggleBookmarkForCurrentPage();
                });
                // 确保书签按钮始终可见和可用
                mBookmarkButton.setVisibility(View.VISIBLE);
                mBookmarkButton.setEnabled(true);
                android.util.Log.d("WebViewActivity", "Bookmark button initialized successfully");
            } else {
                android.util.Log.e("WebViewActivity", "Bookmark button is null!");
            }

        // 设置下拉刷新
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setOnRefreshListener(() -> {
                TabData currentTab = getCurrentTab();
                if (currentTab != null && currentTab.webView != null) {
                    currentTab.webView.reload();
            } else {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });

            // 设置刷新颜色
            mSwipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                android.R.color.holo_blue_bright,
                R.color.colorPrimary,
                android.R.color.holo_blue_bright
            );

            // 设置下拉刷新偏移量（优化顶部布局高度）
            // 顶部工具栏总高度84dp，进度条2dp，总共86dp
            mSwipeRefreshLayout.setProgressViewOffset(false, 0, 88);

            // 优化下拉刷新颜色
            mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        }
    }

    /**
     * 按钮点击动画效果
     */
    private void animateButtonClick(android.view.View view) {
        if (view != null) {
            // 创建缩放动画
            view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    // 恢复原始大小
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .start();
                })
                .start();
        }
    }

    // 标签页管理方法
    private void createNewTab(String url) {
        android.util.Log.d("WebViewActivity", "Creating new tab with URL: " + url);

        try {
            // 创建新的WebView
            WebView webView = mWebViewPoolManager.acquireWebView();
            if (webView == null) {
                webView = mX5WebViewManager.createWebView(this);
            }

            if (webView == null) {
                android.util.Log.e("WebViewActivity", "Failed to create WebView");
                return;
            }

            // 创建增强WebView管理器
            EnhancedWebViewManager enhancedManager = new EnhancedWebViewManager(this, webView, mHistoryManager);

            // 创建标签页数据
            TabData tabData = new TabData(webView, enhancedManager);
            mTabs.add(tabData);
            mCurrentTabIndex = mTabs.size() - 1;

            // 设置WebView
            setupWebViewForTab(webView);
            setupEnhancedWebViewCallbacks(enhancedManager);

            // 加载URL
            loadUrlInTab(tabData, url);

            // 切换到新标签页
            switchToTab(mCurrentTabIndex);

            // 更新UI
            updateTabUI();

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error creating new tab", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }

    private void setupWebViewForTab(WebView webView) {
        if (webView == null) return;

        try {
            WebSettings webSettings = webView.getSettings();

            // 基础设置
            webSettings.setJavaScriptEnabled(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(false);

            // 存储设置
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true);

            // 缓存设置
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

            // 缩放设置
            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);

            // 其他设置
            webSettings.setLoadsImagesAutomatically(true);
            webSettings.setBlockNetworkLoads(false);

            // 设置用户代理（稍后会根据网站类型动态调整）
            webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");

            // 设置编码
            webSettings.setDefaultTextEncodingName("UTF-8");

            // 初始化所有管理器
            mErrorHandler = new WebViewErrorHandler(this, webView);
            mVideoEnhancer = new VideoPlayerEnhancer(this);
            mVideoEnhancer.enhanceWebView(webView);
            mSmartUrlProcessor = new SmartUrlProcessor(this);
            mUserAgentManager = new UserAgentManager(this);

            // 设置WebViewClient来处理历史记录
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    // 保存历史记录
                    try {
                        String title = view.getTitle();
                        if (title != null && !title.isEmpty() && url != null && !url.isEmpty()) {
                            mHistoryManager.addHistory(title, url);
                            android.util.Log.d("WebViewActivity", "History saved: " + title + " - " + url);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WebViewActivity", "Error saving history", e);
                    }

                    // 更新UI标题
                    updateTitle(view.getTitle());

                    // 页面加载完成后，如果地址栏还是空的且当前页面不是about:blank，更新地址栏
                    if (mUrlInput != null && mUrlInput.getText().toString().trim().isEmpty()) {
                        String currentUrl = view.getUrl();
                        if (currentUrl != null && !"about:blank".equals(currentUrl)) {
                            mUrlInput.setText(currentUrl);
                        }
                    }
                }

                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);

                    // 检查是否为特殊网站，如果是则应用兼容性设置
                    if (YouTubeCompatibilityManager.isSpecialSite(url)) {
                        YouTubeCompatibilityManager.applyYouTubeCompatibility(view, url, mUserAgentManager);
                        android.util.Log.d("WebViewActivity", "Applied YouTube compatibility for: " + url);
                    } else {
                        // 为普通网站也设置智能UA
                        if (mUserAgentManager != null) {
                            mUserAgentManager.setSmartUserAgent(view, url);
                        }
                    }

                    // 更新进度条
                    if (mProgressBar != null) {
                        mProgressBar.setVisibility(View.VISIBLE);
                    }

                    // 更新地址栏
                    if (mUrlInput != null) {
                        // 如果是有效的URL（不是about:blank），则更新地址栏
                        if (url != null && !"about:blank".equals(url)) {
                            mUrlInput.setText(url);
                        } else if ("about:blank".equals(url)) {
                            // 对于about:blank，保持地址栏为空，方便用户输入
                            mUrlInput.setText("");
                        }
                    }
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // 强制所有HTTP/HTTPS链接都在EhViewer中打开，杜绝Chrome等外部浏览器
                    if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                        android.util.Log.d("WebViewActivity", "Intercepting URL: " + url + " - keeping in EhViewer");

                        // 检查是否是来自外部应用的链接
                        android.content.Intent intent = getIntent();
                        if (intent != null && android.content.Intent.ACTION_VIEW.equals(intent.getAction())) {
                            android.util.Log.d("WebViewActivity", "External link detected, handling internally");
                        }

                        // 检查是否是YouTube的重定向循环
                        String currentUrl = view.getUrl();
                        if (currentUrl != null && isYouTubeRedirectLoop(currentUrl, url)) {
                            android.util.Log.w("WebViewActivity", "YouTube redirect loop detected: " + currentUrl + " -> " + url);
                            // 强制使用桌面版UA来打破循环
                            if (mUserAgentManager != null) {
                                mUserAgentManager.setSmartUserAgent(view, url);
                            }
                        }

                        // 在当前WebView中加载，不允许外部浏览器接管
                        view.loadUrl(url);
                        return true;
                    }

                    // 处理特殊URL
                    if (url.startsWith("tel:")) {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } else if (url.startsWith("mailto:")) {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } else if (url.startsWith("sms:")) {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }

                    return false;
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);

                    // 使用增强的错误处理器
                    if (mErrorHandler != null) {
                        boolean handled = mErrorHandler.handleError(errorCode, description, failingUrl);
                        if (handled) {
                            return; // 错误已被处理
                        }
                    }

                    // 回退到旧的处理方式
                    android.util.Log.e("WebViewActivity", "WebView error (fallback): " + errorCode + " - " + description + " - " + failingUrl);
                    showErrorPage(errorCode, description, failingUrl);
                }

                @Override
                public void onReceivedHttpError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                    super.onReceivedHttpError(view, request, errorResponse);

                    // 使用增强的错误处理器
                    if (mErrorHandler != null) {
                        boolean handled = mErrorHandler.handleHttpError(request, errorResponse);
                        if (handled) {
                            return; // 错误已被处理
                        }
                    }

                    // 回退到旧的处理方式
                    android.util.Log.e("WebViewActivity", "HTTP error (fallback): " + errorResponse.getStatusCode() + " - " + request.getUrl());
                    showErrorPage(errorResponse.getStatusCode(), "HTTP Error", request.getUrl().toString());
                }
            });

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error setting up WebView", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }

    /**
     * 更新页面标题
     */
    private void updateTitle(String title) {
        try {
            if (title != null && !title.isEmpty() && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error updating title", e);
        }
    }

    /**
     * 退出视频全屏（供JavaScript调用）
     */
    public void exitVideoFullscreen() {
        if (mVideoEnhancer != null && mVideoEnhancer.isFullscreen()) {
            // 这里可以添加退出全屏的逻辑
            android.util.Log.d(TAG, "Exiting video fullscreen");
        }
    }

    /**
     * 处理403错误（YouTube等特殊网站）
     */
    private void handle403Error(WebView view, String failingUrl) {
        try {
            android.util.Log.d("WebViewActivity", "Handling 403 error for: " + failingUrl);

            // 尝试不同的User-Agent策略
            YouTubeCompatibilityManager.tryDifferentUserAgents(view, failingUrl);

            // 延迟重新加载页面，给User-Agent设置一点时间生效
            view.postDelayed(() -> {
                try {
                    view.reload();
                    android.util.Log.d("WebViewActivity", "Retrying load after User-Agent change");
                } catch (Exception e) {
                    android.util.Log.e("WebViewActivity", "Error retrying load", e);
                }
            }, 500);

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error handling 403 error", e);
            // 如果处理失败，显示错误页面
            showErrorPage(403, "访问被拒绝，请检查网络或稍后重试", failingUrl);
        }
    }

    /**
     * 检测YouTube重定向循环
     * YouTube经常在 youtube.com -> m.youtube.com -> youtube.com 之间循环
     */
    private boolean isYouTubeRedirectLoop(String currentUrl, String newUrl) {
        if (currentUrl == null || newUrl == null) return false;

        try {
            // 提取域名
            String currentDomain = extractDomainFromUrl(currentUrl);
            String newDomain = extractDomainFromUrl(newUrl);

            // 检查是否都是YouTube相关域名
            boolean isYouTubeRelated = (currentDomain.contains("youtube.com") || currentDomain.contains("youtu.be")) &&
                                      (newDomain.contains("youtube.com") || newDomain.contains("youtu.be"));

            if (!isYouTubeRelated) return false;

            // 检查是否在不同版本之间跳转
            boolean currentIsMobile = currentDomain.startsWith("m.youtube.com");
            boolean newIsMobile = newDomain.startsWith("m.youtube.com");

            // 如果从桌面版跳转到移动版，或者从移动版跳转到桌面版，可能形成循环
            if (currentIsMobile != newIsMobile) {
                android.util.Log.d(TAG, "YouTube version switch detected: " +
                    currentDomain + " -> " + newDomain);
                return true;
            }

            return false;

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking YouTube redirect loop", e);
            return false;
        }
    }

    /**
     * 处理网络错误
     */
    private void handleNetworkError(String failingUrl) {
        try {
            // 检查是否是URL，如果不是则尝试搜索
            if (failingUrl != null && !failingUrl.startsWith("http")) {
                // 不是有效的URL，尝试搜索 - 使用Google搜索
                String searchUrl = "https://www.google.com/search?q=" + android.net.Uri.encode(failingUrl);
                android.util.Log.d("WebViewActivity", "Redirecting to search: " + searchUrl);
                loadUrlInCurrentTab(searchUrl);
                return;
            }

            // 如果是网络错误，显示错误页面并提供重试选项
            showErrorPage(6, "网络连接失败", failingUrl);

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error handling network error", e);
            showErrorPage(6, "网络连接失败", failingUrl);
        }
    }

    /**
     * 显示错误页面
     */
    private void showErrorPage(int errorCode, String description, String failingUrl) {
        try {
            String htmlContent = generateErrorPageHtml(errorCode, description, failingUrl);
            loadHtmlContent(htmlContent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing error page", e);
        }
    }

    /**
     * 生成错误页面HTML
     */
    private String generateErrorPageHtml(int errorCode, String description, String failingUrl) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("<title>加载失败</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }")
            .append(".error-container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }")
            .append(".error-icon { font-size: 64px; color: #ff6b35; margin-bottom: 20px; }")
            .append(".error-title { color: #333; margin-bottom: 20px; }")
            .append(".error-description { color: #666; margin-bottom: 30px; line-height: 1.6; }")
            .append(".error-url { background: #f8f8f8; padding: 10px; border-radius: 5px; font-family: monospace; margin-bottom: 20px; word-break: break-all; }")
            .append(".retry-btn { background: #ff6b35; color: white; border: none; padding: 12px 30px; border-radius: 5px; font-size: 16px; cursor: pointer; margin: 10px; }")
            .append(".search-btn { background: #007bff; color: white; border: none; padding: 12px 30px; border-radius: 5px; font-size: 16px; cursor: pointer; margin: 10px; }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class='error-container'>")
            .append("<div class='error-icon'>⚠️</div>")
            .append("<h1 class='error-title'>加载失败</h1>")
            .append("<div class='error-description'>")
            .append("<p>错误代码: ").append(errorCode).append("</p>")
            .append("<p>").append(description).append("</p>")
            .append("</div>");

        if (failingUrl != null && !failingUrl.isEmpty()) {
            html.append("<div class='error-url'>").append(failingUrl).append("</div>");
        }

        html.append("<button class='retry-btn' onclick='retryLoad()'>重试</button>")
            .append("<button class='search-btn' onclick='searchInstead()'>搜索页面</button>")
            .append("</div>")
            .append("<script>")
            .append("function retryLoad() {")
            .append("  window.location.reload();")
            .append("}")
            .append("function searchInstead() {")
            .append("  if (window.WebViewJavascriptBridge) {")
            .append("    window.WebViewJavascriptBridge.callHandler('searchInstead', '").append(failingUrl != null ? failingUrl : "").append("');")
            .append("  }")
            .append("}")
            .append("</script>")
            .append("</body>")
            .append("</html>");

        return html.toString();
    }

    /**
     * 加载HTML内容
     */
    private void loadHtmlContent(String html) {
        try {
            if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
                TabData currentTab = mTabs.get(mCurrentTabIndex);
                if (currentTab != null && currentTab.webView != null) {
                    currentTab.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error loading HTML content", e);
        }
    }

    /**
     * 在当前标签页加载URL
     */
    private void loadUrlInCurrentTab(String url) {
        try {
            if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
                TabData currentTab = mTabs.get(mCurrentTabIndex);
                if (currentTab != null && currentTab.webView != null) {
                    currentTab.webView.loadUrl(url);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error loading URL in current tab", e);
        }
    }

    /**
     * 强制处理来自外部应用的浏览器链接
     * 确保所有链接都在EhViewer中打开，杜绝Chrome等外部浏览器
     */
    public static void handleExternalBrowserIntent(Context context, android.content.Intent intent) {
        try {
            android.net.Uri uri = intent.getData();
            if (uri != null) {
                String url = uri.toString();
                android.util.Log.d("WebViewActivity", "Handling external browser intent: " + url);

                // 创建EhViewer的intent，确保链接在我们自己的浏览器中打开
                android.content.Intent ehViewerIntent = new android.content.Intent(context, WebViewActivity.class);
                ehViewerIntent.setAction(android.content.Intent.ACTION_VIEW);
                ehViewerIntent.setData(uri);
                ehViewerIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                ehViewerIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // 添加标识，表示这是从外部链接过来的
                ehViewerIntent.putExtra("from_external_link", true);

                context.startActivity(ehViewerIntent);

                android.util.Log.d("WebViewActivity", "Redirected external link to EhViewer: " + url);
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error handling external browser intent", e);
        }
    }

    /**
     * 注册浏览器链接拦截器
     * 在应用启动时调用，确保我们能拦截所有浏览器相关的intent
     */
    public static void registerBrowserInterceptor(Context context) {
        try {
            // 创建一个广播接收器来监听浏览器相关的intent
            android.content.BroadcastReceiver browserInterceptor = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(Context context, android.content.Intent intent) {
                    if (android.content.Intent.ACTION_VIEW.equals(intent.getAction())) {
                        android.net.Uri uri = intent.getData();
                        if (uri != null && (uri.getScheme().startsWith("http") || uri.getScheme().startsWith("https"))) {
                            android.util.Log.d("BrowserInterceptor", "Intercepted browser intent: " + uri.toString());
                            handleExternalBrowserIntent(context, intent);
                            // 中止广播，防止其他浏览器处理
                            abortBroadcast();
                        }
                    }
                }
            };

            // 注册广播接收器
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction(android.content.Intent.ACTION_VIEW);
            filter.addDataScheme("http");
            filter.addDataScheme("https");
            filter.setPriority(999); // 最高优先级

            context.registerReceiver(browserInterceptor, filter);
            android.util.Log.d("WebViewActivity", "Browser interceptor registered with high priority");

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error registering browser interceptor", e);
        }
    }

    private void setupEnhancedWebViewCallbacks(EnhancedWebViewManager manager) {
        if (manager == null) return;

        try {
            // 暂时注释掉回调设置，避免接口兼容性问题
            // TODO: 修复回调接口定义
            /*
            // 设置进度回调
            manager.setProgressCallback((progress) -> {
                runOnUiThread(() -> {
                    if (mProgressBar != null) {
                        mProgressBar.setProgress(progress);
                        if (progress >= 100) {
                            mProgressBar.setVisibility(View.GONE);
                        } else {
                            mProgressBar.setVisibility(View.VISIBLE);
                        }
                    }
                });
            });

            // 设置错误回调
            manager.setErrorCallback((error) -> {
                runOnUiThread(() -> {
                    android.util.Log.e("WebViewActivity", "WebView error: " + error);
                });
            });
            */

            // 设置下载回调
            manager.setDownloadCallback((url, userAgent, contentDisposition, mimetype, contentLength) -> {
                runOnUiThread(() -> {
                    try {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.addRequestHeader("User-Agent", userAgent);
                        request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                        String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
                        request.setTitle(filename);
                        request.setDescription(getString(R.string.downloading));
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                        request.setAllowedOverRoaming(true);
                        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        if (downloadManager != null) {
                            downloadManager.enqueue(request);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WebViewActivity", "Error starting download", e);
                    }
                });
            });

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error setting up enhanced callbacks", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }

    private void loadUrlInTab(TabData tabData, String url) {
        if (tabData == null || tabData.webView == null) return;

        try {
            String processedUrl = processUrl(url);
            android.util.Log.d("WebViewActivity", "Loading URL in tab: " + processedUrl);
            tabData.webView.loadUrl(processedUrl);
            tabData.url = processedUrl;
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error loading URL", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }



    private TabData getCurrentTab() {
        if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
            return mTabs.get(mCurrentTabIndex);
        }
        return null;
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= mTabs.size()) return;

        try {
            // 隐藏当前WebView
            if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
                TabData oldTab = mTabs.get(mCurrentTabIndex);
                if (oldTab.webView != null && mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.removeView(oldTab.webView);
                }
            }

            // 切换到新标签页
            mCurrentTabIndex = index;
            TabData newTab = mTabs.get(index);

            if (newTab.webView != null && mSwipeRefreshLayout != null) {
                // 添加新WebView到布局
                mSwipeRefreshLayout.addView(newTab.webView, 0);
            }

            // 更新UI
            updateTabUI();
            updateNavigationButtons();

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error switching tab", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }

    private void updateTabUI() {
        // 更新标签页UI（简化实现）
        runOnUiThread(() -> {
            try {
                TabData currentTab = getCurrentTab();
                if (currentTab != null) {
                    if (mUrlInput != null && currentTab.url != null) {
                        mUrlInput.setText(currentTab.url);
                    }
                }

                // 根据标签页数量动态显示/隐藏标签页容器
                updateTabContainerVisibility();

            } catch (Exception e) {
                android.util.Log.e("WebViewActivity", "Error updating tab UI", e);
            }
        });
    }

    private void updateTabContainerVisibility() {
        try {
            if (mTabScrollView != null) {
                // 如果只有一个标签页，隐藏标签页容器节省空间
                // 如果有多个标签页，显示标签页容器
                boolean shouldShow = mTabs.size() > 1;
                int visibility = shouldShow ? View.VISIBLE : View.GONE;
                mTabScrollView.setVisibility(visibility);

                android.util.Log.d("WebViewActivity",
                    "Tab container visibility updated: " + (shouldShow ? "VISIBLE" : "GONE") +
                    " (tabs: " + mTabs.size() + ")");
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error updating tab container visibility", e);
        }
    }

    private void updateNavigationButtons() {
        runOnUiThread(() -> {
            try {
                TabData currentTab = getCurrentTab();
                if (currentTab != null && currentTab.webView != null) {
                    if (mBackButton != null) {
                        mBackButton.setEnabled(currentTab.webView.canGoBack());
                        mBackButton.setAlpha(currentTab.webView.canGoBack() ? 1.0f : 0.5f);
                    }
                    if (mForwardButton != null) {
                        mForwardButton.setEnabled(currentTab.webView.canGoForward());
                        mForwardButton.setAlpha(currentTab.webView.canGoForward() ? 1.0f : 0.5f);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("WebViewActivity", "Error updating navigation buttons", e);
            }
        });
    }

    private String processUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            // 使用智能URL处理器获取默认主页
            return mSmartUrlProcessor != null ? mSmartUrlProcessor.getDefaultHomePage() : "https://www.google.com";
        }

        // 使用智能URL处理器处理输入
        if (mSmartUrlProcessor != null) {
            String processedUrl = mSmartUrlProcessor.processInput(url);

            // 记录处理结果用于调试
            String inputType = mSmartUrlProcessor.getInputTypeDescription(url);
            android.util.Log.d(TAG, "URL Processing: '" + url + "' -> '" + processedUrl + "' (" + inputType + ")");

            return processedUrl;
        }

        // 回退到原有的处理逻辑
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        } else if (url.contains(".")) {
            return "https://" + url;
        } else {
            return getSmartSearchUrl(url);
        }
    }

    private String getSmartHomeUrl() {
        try {
            // 暂时使用简单的逻辑，避免NetworkDetector依赖问题
            // TODO: 修复NetworkDetector.isGfwBlocked方法
            // 检查网络状态，如果GFW检测到，则使用百度
            // boolean isGfwBlocked = NetworkDetector.isGfwBlocked(this);
            // if (isGfwBlocked) {
            //     return "https://www.baidu.com";
            // } else {
                return "https://www.google.com";
            // }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error getting smart home URL", e);
            return "https://www.google.com";
        }
    }

    private String getSmartSearchUrl(String query) {
        try {
            // 暂时使用简单的逻辑，避免NetworkDetector依赖问题
            // TODO: 修复NetworkDetector.isGfwBlocked方法
            // boolean isGfwBlocked = NetworkDetector.isGfwBlocked(this);
            // if (isGfwBlocked) {
            //     return "https://www.baidu.com/s?wd=" + Uri.encode(query);
            // } else {
                return "https://www.google.com/search?q=" + Uri.encode(query);
            // }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error getting smart search URL", e);
            return "https://www.google.com/search?q=" + Uri.encode(query);
        }
    }

    private void showBrowserModeToast() {
        try {
            Toast.makeText(this, "浏览器模式已启动", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing browser mode toast", e);
        }
    }

    private void showTabsDialog() {
        try {
            // 简单的标签页管理对话框
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("标签页管理");

            String[] tabTitles = new String[mTabs.size()];
            for (int i = 0; i < mTabs.size(); i++) {
                TabData tab = mTabs.get(i);
                tabTitles[i] = (tab.title != null ? tab.title : "标签页 " + (i + 1));
            }

            builder.setItems(tabTitles, (dialog, which) -> {
                switchToTab(which);
            });

            builder.setPositiveButton("新建标签页", (dialog, which) -> {
                createNewTab(getSmartHomeUrl());
            });

            builder.setNegativeButton("关闭", null);
            builder.show();

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing tabs dialog", e);
        }
    }

    private void showQuickAccessMenu() {
        try {
            android.util.Log.d("WebViewActivity", "Showing quick access menu");

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("快速访问");

            String[] menuItems = {
                "⭐ 书签",
                "🕐 历史记录",
                "🔍 搜索当前页面",
                "🔄 刷新页面"
            };

            builder.setItems(menuItems, (dialog, which) -> {
                try {
                    switch (which) {
                        case 0: // 书签
                            android.util.Log.d("WebViewActivity", "Quick access: bookmarks");
                            startBookmarksActivity();
                            break;
                        case 1: // 历史记录
                            android.util.Log.d("WebViewActivity", "Quick access: history");
                            startHistoryActivity();
                            break;
                        case 2: // 搜索当前页面
                            android.util.Log.d("WebViewActivity", "Quick access: search page");
                            Toast.makeText(this, "页面搜索功能开发中", Toast.LENGTH_SHORT).show();
                            break;
                        case 3: // 刷新页面
                            android.util.Log.d("WebViewActivity", "Quick access: refresh");
                            TabData currentTab = getCurrentTab();
                            if (currentTab != null && currentTab.webView != null) {
                                currentTab.webView.reload();
                            }
                            break;
                    }
                } catch (Exception e) {
                    android.util.Log.e("WebViewActivity", "Error in quick access menu: " + which, e);
                    Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("取消", null);

            android.app.AlertDialog dialog = builder.create();
            dialog.show();

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing quick access menu", e);
            Toast.makeText(this, "无法显示快速访问菜单: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showMenuDialog() {
        try {
            android.util.Log.d("WebViewActivity", "Showing menu dialog");

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("浏览器菜单");

            String[] menuItems = {
                "⭐ 书签管理",
                "🕐 历史记录",
                "⚙️ 浏览器设置",
                "📖 阅读模式",
                "📸 截图",
                "💻 桌面模式"
            };

            builder.setItems(menuItems, (dialog, which) -> {
                try {
                    android.util.Log.d("WebViewActivity", "Menu item selected: " + which);
                    switch (which) {
                        case 0: // 书签
                            android.util.Log.d("WebViewActivity", "Starting bookmarks activity");
                            startBookmarksActivity();
                            break;
                        case 1: // 历史记录
                            android.util.Log.d("WebViewActivity", "Starting history activity");
                            startHistoryActivity();
                            break;
                        case 2: // 设置
                            android.util.Log.d("WebViewActivity", "Starting browser settings");
                            startBrowserSettingsActivity();
                            break;
                        case 3: // 阅读模式
                            android.util.Log.d("WebViewActivity", "Toggling reading mode");
                            toggleReadingMode();
                            break;
                        case 4: // 截图
                            android.util.Log.d("WebViewActivity", "Taking screenshot");
                            takeScreenshot();
                            break;
                        case 5: // 桌面模式
                            android.util.Log.d("WebViewActivity", "Toggling desktop mode");
                            toggleDesktopMode();
                            break;
                    }
                } catch (Exception e) {
                    android.util.Log.e("WebViewActivity", "Error handling menu item: " + which, e);
                    Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("关闭", null);

            android.app.AlertDialog dialog = builder.create();
            dialog.show();

            android.util.Log.d("WebViewActivity", "Menu dialog shown successfully");

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing menu dialog", e);
            Toast.makeText(this, "无法显示菜单: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        TabData currentTab = getCurrentTab();
        if (currentTab != null && currentTab.webView != null && currentTab.webView.canGoBack()) {
            currentTab.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            // 清理所有标签页的WebView
            for (TabData tab : mTabs) {
                if (tab.webView != null) {
                    tab.webView.setWebViewClient(null);
                    tab.webView.setWebChromeClient(null);
                    tab.webView.destroy();
                }
            }
            mTabs.clear();

            // 清理管理器
            // TODO: 修复管理器cleanup方法
            /*
            if (mWebViewPoolManager != null) {
                mWebViewPoolManager.cleanup();
            }
            if (mMemoryManager != null) {
                mMemoryManager.cleanup();
            }
            */

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onDestroy", e);
            ExceptionUtils.throwIfFatal(e);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        try {
            // 暂停所有WebView
            for (TabData tab : mTabs) {
                if (tab.webView != null) {
                    tab.webView.onPause();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onPause", e);
            ExceptionUtils.throwIfFatal(e);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        try {
            // 恢复所有WebView
            for (TabData tab : mTabs) {
                if (tab.webView != null) {
                    tab.webView.onResume();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onResume", e);
            ExceptionUtils.throwIfFatal(e);
        }
        super.onResume();
    }

    private void toggleBookmarkForCurrentPage() {
        try {
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null && mBookmarkManager != null) {
                String url = currentTab.webView.getUrl();
                String title = currentTab.webView.getTitle();

                if (url != null && !url.isEmpty()) {
                    // 检查是否已经收藏
                    boolean isBookmarked = mBookmarkManager.isBookmarked(url);

                    if (isBookmarked) {
                        // 移除书签 - 通过遍历所有书签找到匹配的URL
                        List<BookmarkInfo> bookmarks = mBookmarkManager.getAllBookmarks();
                        boolean removed = false;
                        for (BookmarkInfo bookmark : bookmarks) {
                            if (url.equals(bookmark.url)) {
                                removed = mBookmarkManager.deleteBookmark(bookmark.id);
                                break;
                            }
                        }
                        if (removed) {
                            Toast.makeText(this, "已移除书签", Toast.LENGTH_SHORT).show();
                            updateBookmarkButtonState(false);
                        } else {
                            Toast.makeText(this, "移除书签失败", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // 添加书签
                        if (title == null || title.isEmpty()) {
                            title = url;
                        }
                        long bookmarkId = mBookmarkManager.addBookmark(title, url);
                        if (bookmarkId > 0) {
                            Toast.makeText(this, "已添加书签", Toast.LENGTH_SHORT).show();
                            updateBookmarkButtonState(true);
                        } else {
                            Toast.makeText(this, "添加书签失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "无法获取页面信息", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error toggling bookmark", e);
            Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBookmarkButtonState(boolean isBookmarked) {
        if (mBookmarkButton != null) {
            // 可以在这里更新书签按钮的外观，比如改变图标或颜色
            // 例如：改变tint颜色或者图标
            if (isBookmarked) {
                mBookmarkButton.setColorFilter(getResources().getColor(android.R.color.holo_blue_bright));
            } else {
                mBookmarkButton.clearColorFilter();
            }
        }
    }

    private void startBookmarksActivity() {
        try {
            Intent intent = new Intent(this, BookmarksActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error starting bookmarks activity", e);
        }
    }

    private void startHistoryActivity() {
        try {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error starting history activity", e);
        }
    }

    private void startBrowserSettingsActivity() {
        try {
            Intent intent = new Intent(this, BrowserSettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error starting browser settings activity", e);
        }
    }

    private void toggleReadingMode() {
        try {
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null && mReadingModeManager != null) {
                if (mReadingModeManager.isReadingModeEnabled()) {
                    // 禁用阅读模式
                    mReadingModeManager.disableReadingMode(currentTab.webView);
                    Toast.makeText(this, "已退出阅读模式", Toast.LENGTH_SHORT).show();
                } else {
                    // 启用阅读模式
                    mReadingModeManager.enableReadingMode(currentTab.webView);
                    Toast.makeText(this, "已进入阅读模式", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error toggling reading mode", e);
            Toast.makeText(this, "阅读模式切换失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void takeScreenshot() {
        try {
            // TODO: 修复EnhancedWebViewManager的方法
            /*
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.enhancedWebViewManager != null) {
                currentTab.enhancedWebViewManager.takeScreenshot();
            }
            */
            Toast.makeText(this, "截图功能开发中", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error taking screenshot", e);
        }
    }

    private void toggleDesktopMode() {
        try {
            // TODO: 修复EnhancedWebViewManager的方法
            /*
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.enhancedWebViewManager != null) {
                currentTab.enhancedWebViewManager.toggleDesktopMode();
            }
            */
            Toast.makeText(this, "桌面模式功能开发中", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error toggling desktop mode", e);
        }
    }

    private void setupMemoryListeners() {
        try {
            // TODO: 修复MemoryManager的回调设置
            /*
            if (mMemoryManager != null) {
                mMemoryManager.setMemoryWarningCallback(() -> {
                    runOnUiThread(() -> {
                        try {
                            // 清理不必要的缓存
                            for (TabData tab : mTabs) {
                                if (tab.webView != null) {
                                    tab.webView.clearCache(true);
                                }
                            }
                            Toast.makeText(this, "内存不足，已清理缓存", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            android.util.Log.e("WebViewActivity", "Error in memory warning callback", e);
                        }
                    });
                });
            }
            */
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error setting up memory listeners", e);
        }
    }

    private void createNewTabWithHtmlContent(String htmlContent, String title) {
        try {
            WebView webView = mWebViewPoolManager.acquireWebView();
            if (webView == null) {
                webView = mX5WebViewManager.createWebView(this);
            }

            if (webView == null) {
                android.util.Log.e("WebViewActivity", "Failed to create WebView for HTML content");
                return;
            }

            EnhancedWebViewManager enhancedManager = new EnhancedWebViewManager(this, webView, mHistoryManager);
            TabData tabData = new TabData(webView, enhancedManager);
            tabData.title = title;

            mTabs.add(tabData);
            mCurrentTabIndex = mTabs.size() - 1;

            setupWebViewForTab(webView);
            setupEnhancedWebViewCallbacks(enhancedManager);

            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            switchToTab(mCurrentTabIndex);
            updateTabUI();

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error creating tab with HTML content", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }

    // 静态方法用于启动Activity
    public static void startWebView(Context context, String url) {
        try {
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra(EXTRA_URL, url);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error starting WebViewActivity", e);
        }
    }

    public static void startWebViewActivity(Context context, String url) {
        startWebView(context, url);
    }

    public static void startWebViewActivityWithHtml(Context context, String htmlContent, String title) {
        try {
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("html_content", htmlContent);
            intent.putExtra("is_preview", true);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error starting WebViewActivity with HTML", e);
        }
    }
}
