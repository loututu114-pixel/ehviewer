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

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebStorage;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.AdBlockManager;
import com.hippo.ehviewer.client.BookmarkManager;
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.client.X5WebViewManager;
import com.hippo.ehviewer.client.WebViewPoolManager;
import com.hippo.ehviewer.client.JavaScriptOptimizer;
import com.hippo.ehviewer.client.WebViewCacheManager;
import com.hippo.ehviewer.client.ImageLazyLoader;
import com.hippo.ehviewer.client.MemoryManager;
import com.hippo.ehviewer.client.ReadingModeManager;
import com.hippo.ehviewer.client.EnhancedWebViewManager;
import com.hippo.ehviewer.client.data.BookmarkInfo;
import com.hippo.ehviewer.widget.UnifiedWebView;
import com.hippo.util.ExceptionUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * 独立的WebView浏览器Activity
 */
public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "WebViewActivity";

    public static final String EXTRA_URL = "url";

    /**
     * 标签页数据模型
     */
    private static class TabData {
        WebView webView;
        String url;
        String title;
        Bitmap favicon;
        View tabView;
        boolean isActive;

        TabData(WebView webView, String url, String title) {
            this.webView = webView;
            this.url = url;
            this.title = title;
            this.isActive = false;
        }
    }

    /**
     * 智能选择主页URL（根据GFW状态）
     */
    private String getSmartHomeUrl() {
        return getSmartSearchUrl(""); // 空字符串返回主页
    }

    /**
     * 显示错误页面
     */
    private void showErrorPage(WebView view, int errorCode, String description, String failingUrl) {
        String errorHtml = "<html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; text-align: center; padding: 50px;\">" +
                "<h1 style=\"color: #e74c3c;\">无法访问页面</h1>" +
                "<p style=\"color: #666; font-size: 16px;\">错误代码: " + errorCode + "</p>" +
                "<p style=\"color: #666; font-size: 14px;\">描述: " + description + "</p>" +
                "<p style=\"color: #666; font-size: 14px;\">URL: " + failingUrl + "</p>" +
                "<br>" +
                "<button onclick=\"location.reload()\" style=\"padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer;\">重新加载</button>" +
                "<br><br>" +
                "<button onclick=\"window.location.href='https://www.baidu.com'\" style=\"padding: 10px 20px; background: #2ecc71; color: white; border: none; border-radius: 4px; cursor: pointer;\">访问百度</button>" +
                "</body></html>";

        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    /**
     * 智能选择搜索URL（根据GFW状态）
     */
    private String getSmartSearchUrl(String query) {
        try {
            // 尝试检测Google访问性
            java.net.URL testUrl = new java.net.URL("https://www.google.com");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) testUrl.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("HEAD");

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            if (responseCode >= 200 && responseCode < 400) {
                // Google可访问
                if (query.isEmpty()) {
                    return "https://www.google.com";
                } else {
                    return "https://www.google.com/search?q=" + android.net.Uri.encode(query);
                }
            } else {
                // Google不可访问，可能是GFW，使用百度
                if (query.isEmpty()) {
                    return "https://www.baidu.com";
                } else {
                    return "https://www.baidu.com/s?wd=" + android.net.Uri.encode(query);
                }
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Network detection failed, using Baidu as fallback", e);
            // 网络检测失败，使用百度作为备选
            if (query.isEmpty()) {
                return "https://www.baidu.com";
            } else {
                return "https://www.baidu.com/s?wd=" + android.net.Uri.encode(query);
            }
        }
    }

    /**
     * 启动WebViewActivity的便捷方法
     */
    public static void startWebView(Context context, String url) {
        try {
            android.util.Log.d("WebViewActivity", "Starting WebViewActivity with URL: " + url);
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra(EXTRA_URL, url);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Failed to start WebViewActivity", e);
            // 回退到系统浏览器
            try {
                Intent systemIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(systemIntent);
            } catch (Exception ex) {
                android.util.Log.e("WebViewActivity", "Failed to start system browser", ex);
            }
        }
    }

    // 标签页管理
    private List<TabData> mTabs = new ArrayList<>();
    private int mCurrentTabIndex = -1;
    private LinearLayout mTabContainer;
    private HorizontalScrollView mTabScrollView;

    // 书签、历史记录、广告拦截和X5管理
    private BookmarkManager mBookmarkManager;
    private HistoryManager mHistoryManager;
    private AdBlockManager mAdBlockManager;
    private X5WebViewManager mX5WebViewManager;

    // 性能优化管理器
    private WebViewPoolManager mWebViewPoolManager;
    private JavaScriptOptimizer mJavaScriptOptimizer;
    private WebViewCacheManager mWebViewCacheManager;
    private ImageLazyLoader mImageLazyLoader;
    private MemoryManager mMemoryManager;
    private ReadingModeManager mReadingModeManager;
    private EnhancedWebViewManager mEnhancedWebViewManager;

    // 性能监控
    private long mPageLoadStartTime;
    private boolean mIsMonitoringPerformance = true;

    // 当前活跃的WebView组件（为了兼容现有代码）
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Toolbar mToolbar;
    private EditText mUrlInput;
    private ImageButton mRefreshButton;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageButton mHomeButton;
    private ImageButton mTabsButton;
    private ImageButton mMenuButton;

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

            // 注意：WebView现在通过动态方式添加到容器中，不再使用静态定义
            // 初始化增强WebView管理器（暂时设为null，会在创建标签页时设置）
            mEnhancedWebViewManager = null;

            // 预热WebView池
            mWebViewPoolManager.warmUpPool();

            // 注册内存监听器
            setupMemoryListeners();
            mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
            mProgressBar = findViewById(R.id.progress_bar);
            mToolbar = findViewById(R.id.toolbar);
            mUrlInput = findViewById(R.id.url_input);
            mRefreshButton = findViewById(R.id.refresh_button);
            mBackButton = findViewById(R.id.back_button);
            mForwardButton = findViewById(R.id.forward_button);
            mHomeButton = findViewById(R.id.home_button);
            mTabsButton = findViewById(R.id.tabs_button);
            mMenuButton = findViewById(R.id.menu_button);

            // 紧凑布局：隐藏默认ActionBar，直接使用布局中的组件
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            // 设置返回按钮（如果布局中有的话）
            if (mBackButton != null) {
                mBackButton.setOnClickListener(v -> finish());
            }

            // 设置地址栏 - 增强交互体验
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

                mUrlInput.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        mUrlInput.selectAll();
                        // 添加轻微的缩放动画效果
                        v.animate()
                         .scaleX(1.02f)
                         .scaleY(1.02f)
                         .setDuration(150)
                         .start();
                    } else {
                        // 恢复原始大小
                        v.animate()
                         .scaleX(1.0f)
                         .scaleY(1.0f)
                         .setDuration(150)
                         .start();
                    }
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

            if (mTabsButton != null) {
                mTabsButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    showTabsDialog();
                });
            }

            if (mMenuButton != null) {
                mMenuButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    showMenuDialog();
                });
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

        // 设置返回按钮（如果布局中有的话）
        if (mBackButton != null) {
            mBackButton.setOnClickListener(v -> finish());
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

    /**
     * 智能选择主页URL（根据GFW状态）
     */
            }

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
                } else {
                    // 创建默认标签页（使用智能搜索引擎选择）
                    String defaultUrl = getSmartHomeUrl();
                    android.util.Log.d("WebViewActivity", "Using smart default URL: " + defaultUrl);
                    createNewTab(defaultUrl);
                }
            }

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onCreate", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        android.util.Log.d("WebViewActivity", "setupWebView called");

        if (mWebView == null) {
            android.util.Log.e("WebViewActivity", "WebView is null!");
            return;
        }

        try {
            WebSettings webSettings = mWebView.getSettings();

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

            // 设置用户代理
            webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");

            // 设置编码
            webSettings.setDefaultTextEncodingName("UTF-8");

            // 设置WebViewClient
            mWebView.setWebViewClient(new WebViewClient() {
                            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.loading);
                }
                // 更新地址栏
                updateUrlInput(url);
                // 更新导航按钮状态
                updateNavigationButtons();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.GONE);
                }
                String title = view.getTitle();
                if (getSupportActionBar() != null) {
                    if (title != null && !title.isEmpty()) {
                        getSupportActionBar().setTitle(title);
                    } else {
                        getSupportActionBar().setTitle(R.string.browser);
                    }
                }
                // 更新导航按钮状态
                updateNavigationButtons();
            }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // 处理特殊协议
                    if (url.startsWith("intent://") || url.startsWith("market://")) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    }

                    // 在当前WebView中加载
                    view.loadUrl(url);
                    return true;
                }
            });

            // 设置WebChromeClient
            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    if (mProgressBar != null) {
                        mProgressBar.setProgress(newProgress);
                        if (newProgress == 100) {
                            mProgressBar.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onReceivedTitle(WebView view, String title) {
                    super.onReceivedTitle(view, title);
                    if (getSupportActionBar() != null) {
                        if (title != null && !title.isEmpty()) {
                            getSupportActionBar().setTitle(title);
                        }
                    }
                }
            });

            // 设置下载监听器
            mWebView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                          String mimetype, long contentLength) {
                    try {
                        android.util.Log.d("WebViewActivity", "Download started: " + url);

                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                        // 设置请求头
                        request.addRequestHeader("User-Agent", userAgent);
                        request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));

                        // 设置下载文件信息
                        String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
                        request.setTitle(filename);
                        request.setDescription(getString(R.string.downloading));

                        // 设置下载路径
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

                        // 设置通知
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                        // 允许使用移动网络下载
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                                                     DownloadManager.Request.NETWORK_MOBILE);

                        // 允许漫游时下载
                        request.setAllowedOverRoaming(true);

                        // 开始下载
                        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        if (downloadManager != null) {
                            downloadManager.enqueue(request);
                            android.util.Log.d("WebViewActivity", "Download enqueued successfully");
                        }

                    } catch (Exception e) {
                        android.util.Log.e("WebViewActivity", "Download error", e);
                        ExceptionUtils.throwIfFatal(e);
                    }
                }
            });

            // 启用cookies
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(mWebView, true);
            }

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in setupWebView", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null && currentTab.webView.canGoBack()) {
                currentTab.webView.goBack();
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onBackPressed", e);
            ExceptionUtils.throwIfFatal(e);
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            // 清理所有标签页的WebView
            for (TabData tab : mTabs) {
                if (tab.webView != null) {
                    tab.webView.stopLoading();
                    tab.webView.setWebViewClient(null);
                    tab.webView.setWebChromeClient(null);
                    tab.webView.destroy();
                }
            }
            mTabs.clear();

            // 清理兼容性变量
            mWebView = null;
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onDestroy", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            // 暂停所有标签页的WebView
            for (TabData tab : mTabs) {
                if (tab.webView != null) {
                    tab.webView.onPause();
                }
            }
            // 兼容性处理
            if (mWebView != null) {
                mWebView.onPause();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onPause", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // 恢复所有标签页的WebView
            for (TabData tab : mTabs) {
                if (tab.webView != null) {
                    tab.webView.onResume();
                }
            }
            // 兼容性处理
            if (mWebView != null) {
                mWebView.onResume();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onResume", e);
        }
    }

    /**
     * 加载URL，支持搜索和网址
     */
    private void loadUrl(String input) {
        if (TextUtils.isEmpty(input)) return;

        String url = input.trim();

        // 如果不包含协议，添加https://
        if (!url.contains("://")) {
            // 检查是否是搜索关键词
            if (url.contains(" ") || !url.contains(".")) {
                        // 认为是搜索关键词，使用智能搜索引擎
        url = getSmartSearchUrl(url);
            } else {
                // 认为是网址，添加https协议
                url = "https://" + url;
            }
        }

        android.util.Log.d("WebViewActivity", "Loading URL: " + url);
        if (mWebView != null) {
            mWebView.loadUrl(url);
        }
    }

    /**
     * 更新地址栏显示
     */
    private void updateUrlInput(String url) {
        if (mUrlInput != null && url != null) {
            // 移除协议前缀以获得更清洁的显示
            String displayUrl = url;
            if (displayUrl.startsWith("https://")) {
                displayUrl = displayUrl.substring(8);
            } else if (displayUrl.startsWith("http://")) {
                displayUrl = displayUrl.substring(7);
            }
            mUrlInput.setText(displayUrl);
        }
    }

    /**
     * 更新导航按钮状态
     */
    private void updateNavigationButtons() {
        if (mWebView == null) return;

        if (mBackButton != null) {
            mBackButton.setEnabled(mWebView.canGoBack());
            mBackButton.setAlpha(mWebView.canGoBack() ? 1.0f : 0.5f);
        }

        if (mForwardButton != null) {
            mForwardButton.setEnabled(mWebView.canGoForward());
            mForwardButton.setAlpha(mWebView.canGoForward() ? 1.0f : 0.5f);
        }
    }

    /**
     * 显示标签页对话框
     */
    private void showTabsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("标签页管理")
                .setMessage("当前只有一个标签页\n\n未来版本将支持多标签页功能")
                .setPositiveButton("确定", null)
                .show();
    }

    /**
     * 显示菜单对话框
     */
    private void showMenuDialog() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, mMenuButton);
        popupMenu.getMenuInflater().inflate(R.menu.browser_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_new_tab) {
                // 新建标签页
                showTabsDialog();
                return true;
            } else if (itemId == R.id.menu_add_bookmark) {
                // 添加到书签
                showAddBookmarkDialog();
                return true;
            } else if (itemId == R.id.menu_bookmarks) {
                // 打开书签管理
                BookmarksActivity.startBookmarks(this);
                return true;
            } else if (itemId == R.id.menu_history) {
                // 打开历史记录
                HistoryActivity.startHistory(this);
                return true;
            } else if (itemId == R.id.menu_reading_mode) {
                // 阅读模式切换
                toggleReadingMode();
                return true;
            } else if (itemId == R.id.menu_reading_settings) {
                // 阅读设置
                Intent readingSettingsIntent = new Intent(this, ReadingModeSettingsActivity.class);
                startActivity(readingSettingsIntent);
                return true;
            } else if (itemId == R.id.menu_privacy) {
                // 隐私保护设置
                showPrivacySettingsDialog();
                return true;
            } else if (itemId == R.id.menu_browser_settings) {
                // 浏览器设置
                Intent browserSettingsIntent = new Intent(this, BrowserSettingsActivity.class);
                startActivity(browserSettingsIntent);
                return true;
            } else if (itemId == R.id.menu_share) {
                // 分享
                if (mWebView != null) {
                    String url = mWebView.getUrl();
                    if (url != null) {
                        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, url);
                        startActivity(android.content.Intent.createChooser(shareIntent, "分享网页"));
                    }
                }
                return true;
            } else if (itemId == R.id.menu_screenshot) {
                // 页面截图
                takeScreenshot();
                return true;
            } else if (itemId == R.id.menu_desktop_mode) {
                // 桌面模式切换
                toggleDesktopMode();
                return true;
            } else if (itemId == R.id.menu_clear_cache) {
                // 清除缓存
                clearWebViewCache();
                return true;
            } else if (itemId == R.id.menu_ad_block) {
                // 广告拦截设置
                toggleAdBlock();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    // ==================== 浏览器模式提示 ====================

    /**
     * 显示浏览器模式提示
     */
    private void showBrowserModeToast() {
        try {
            android.widget.Toast.makeText(this,
                "EhViewer 浏览器模式 - 享受更快的浏览体验",
                android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show browser mode toast", e);
        }
    }

    // ==================== 阅读模式 ====================

    /**
     * 切换阅读模式
     */
    private void toggleReadingMode() {
        TabData currentTab = getCurrentTab();
        if (currentTab == null || currentTab.webView == null) {
            return;
        }

        try {
            if (mReadingModeManager.isReadingModeEnabled()) {
                // 禁用阅读模式
                mReadingModeManager.disableReadingMode(currentTab.webView);
                android.widget.Toast.makeText(this, "已退出阅读模式", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // 启用阅读模式
                mReadingModeManager.enableReadingMode(currentTab.webView);
                android.widget.Toast.makeText(this, "已进入阅读模式", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to toggle reading mode", e);
            android.widget.Toast.makeText(this, "阅读模式切换失败", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== 内存管理 ====================

    /**
     * 设置内存监听器
     */
    private void setupMemoryListeners() {
        mMemoryManager.addMemoryListener(new MemoryManager.MemoryListener() {
            @Override
            public void onLowMemory() {
                android.util.Log.w(TAG, "Low memory detected, optimizing WebView");
                runOnUiThread(() -> {
                    // 显示内存不足提示
                    android.widget.Toast.makeText(WebViewActivity.this,
                        "内存不足，正在优化性能",
                        android.widget.Toast.LENGTH_SHORT).show();

                    // 优化当前标签页
                    optimizeCurrentTabForMemory();
                });
            }

            @Override
            public void onMemoryOptimized() {
                android.util.Log.d(TAG, "Memory optimization completed");
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(WebViewActivity.this,
                        "内存优化完成",
                        android.widget.Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onMemoryCritical() {
                android.util.Log.e(TAG, "Critical memory detected, taking urgent actions");
                runOnUiThread(() -> {
                    // 显示严重内存不足警告
                    android.widget.Toast.makeText(WebViewActivity.this,
                        "内存严重不足，正在清理资源",
                        android.widget.Toast.LENGTH_LONG).show();

                    // 执行紧急优化
                    performUrgentMemoryOptimization();
                });
            }
        });
    }

    /**
     * 优化当前标签页以节省内存
     */
    private void optimizeCurrentTabForMemory() {
        try {
            // 清理图片缓存
            if (mImageLazyLoader != null) {
                mImageLazyLoader.clearCache();
            }

            // 清理WebView缓存
            if (mWebViewCacheManager != null) {
                mWebViewCacheManager.cleanupExpiredCache();
            }

            // 优化当前WebView
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null) {
                // 停止不必要的JavaScript
                currentTab.webView.getSettings().setJavaScriptEnabled(false);
                // 清理WebView历史记录
                currentTab.webView.clearHistory();
                // 清理表单数据
                currentTab.webView.clearFormData();
            }

            android.util.Log.d(TAG, "Current tab optimized for memory");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to optimize current tab for memory", e);
        }
    }

    /**
     * 执行紧急内存优化
     */
    private void performUrgentMemoryOptimization() {
        try {
            // 关闭非活跃的标签页
            closeInactiveTabs();

            // 清理所有缓存
            if (mImageLazyLoader != null) {
                mImageLazyLoader.clearCache();
            }
            if (mWebViewCacheManager != null) {
                mWebViewCacheManager.clearAllCache();
            }

            // 强制垃圾回收
            System.gc();
            System.runFinalization();

            android.util.Log.w(TAG, "Urgent memory optimization performed");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to perform urgent memory optimization", e);
        }
    }

    /**
     * 关闭非活跃的标签页
     */
    private void closeInactiveTabs() {
        try {
            List<TabData> tabsToClose = new java.util.ArrayList<>();

            // 找出非活跃的标签页（保留当前标签页和最近使用的标签页）
            for (TabData tab : mTabs) {
                if (!tab.isActive && mTabs.size() > 2) {
                    // 如果有超过2个标签页，关闭非活跃的标签页
                    tabsToClose.add(tab);
                }
            }

            // 关闭标签页
            for (TabData tab : tabsToClose) {
                closeTab(mTabs.indexOf(tab));
            }

            if (!tabsToClose.isEmpty()) {
                android.util.Log.d(TAG, "Closed " + tabsToClose.size() + " inactive tabs for memory optimization");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to close inactive tabs", e);
        }
    }

    // ==================== 多标签页管理功能 ====================

    /**
     * 创建新标签页
     */
    private void createNewTab(String url) {
        // 使用UnifiedWebView（自动选择X5或系统WebView）
        com.hippo.ehviewer.widget.UnifiedWebView webView = new com.hippo.ehviewer.widget.UnifiedWebView(this);
        setupWebViewForTab(webView);

        // 初始化增强WebView管理器（如果还没有初始化）
        if (mEnhancedWebViewManager == null) {
            mEnhancedWebViewManager = new EnhancedWebViewManager(this, webView);
            setupEnhancedWebViewCallbacks();
        }

        TabData tabData = new TabData(webView, url, "新标签页");
        mTabs.add(tabData);

        // 如果是第一个标签页，立即显示
        if (mTabs.size() == 1) {
            switchToTab(0);
        }

        // 立即加载URL
        if (url != null && !url.isEmpty()) {
            loadUrlInTab(tabData, url);
        }

        // 如果URL适合阅读模式，自动启用阅读模式
        if (mReadingModeManager.isSuitableForReadingMode(url, "新标签页")) {
            // 延迟启用阅读模式，等待页面加载完成
            webView.postDelayed(() -> {
                if (!mReadingModeManager.isReadingModeEnabled()) {
                    mReadingModeManager.enableReadingMode(webView);
                }
            }, 2000);
        }
    }

    /**
     * 创建新标签页并加载HTML内容
     */
    private void createNewTabWithHtmlContent(String htmlContent, String title) {
        // 使用UnifiedWebView（自动选择X5或系统WebView）
        com.hippo.ehviewer.widget.UnifiedWebView webView = new com.hippo.ehviewer.widget.UnifiedWebView(this);
        setupWebViewForTab(webView);

        // 加载HTML内容
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);

        TabData tabData = new TabData(webView, "about:reading", title);
        mTabs.add(tabData);

        // 创建标签页UI
        createTabView(tabData);

        // 切换到新标签页
        switchToTab(mTabs.size() - 1);
    }

    /**
     * 为标签页设置WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebViewForTab(android.webkit.WebView webView) {
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

        // 网络和安全设置
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setBlockNetworkImage(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);

        // 设置User Agent
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android " + android.os.Build.VERSION.RELEASE +
                "; " + android.os.Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");

        // 编码设置
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setDefaultFontSize(16);
        webSettings.setMinimumFontSize(8);

        // 夜间模式支持
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            webSettings.setAlgorithmicDarkeningAllowed(true);
        }

        // 检测系统主题
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                webSettings.setForceDark(WebSettings.FORCE_DARK_ON);
            }
        }

        // 配置缓存管理
        mWebViewCacheManager.configureCacheForWebView(webView);

        // 应用JavaScript优化
        mJavaScriptOptimizer.optimizeWebView(webView);
        mJavaScriptOptimizer.injectImageLazyLoad(webView);

        // X5 WebView特殊优化
        if (webView instanceof com.hippo.ehviewer.widget.UnifiedWebView &&
            ((com.hippo.ehviewer.widget.UnifiedWebView) webView).isX5WebView()) {
            try {
                // 获取X5 WebView的实际对象
                Object actualWebView = ((com.hippo.ehviewer.widget.UnifiedWebView) webView).getActualWebView();
                if (actualWebView instanceof com.tencent.smtt.sdk.WebView) {
                    com.tencent.smtt.sdk.WebView x5WebView = (com.tencent.smtt.sdk.WebView) actualWebView;
                    com.tencent.smtt.sdk.WebSettings x5Settings = x5WebView.getSettings();

                    // X5特有优化配置
                    x5Settings.setUseWideViewPort(true);  // 支持视口元标签
                    x5Settings.setLoadWithOverviewMode(true);  // 自适应屏幕
                    x5Settings.setLayoutAlgorithm(com.tencent.smtt.sdk.WebSettings.LayoutAlgorithm.SINGLE_COLUMN);  // 布局算法优化

                    // 启用硬件加速（如果系统支持）
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        x5WebView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
                    }

                    android.util.Log.d("X5Optimization", "X5 WebView optimizations applied");
                }
            } catch (Exception e) {
                android.util.Log.e("X5Optimization", "Failed to apply X5 optimizations", e);
            }
        }

        // 设置WebViewClient（带广告拦截）
        webView.setWebViewClient(mAdBlockManager.createAdBlockWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                TabData tab = findTabByWebView(view);
                if (tab != null) {
                    tab.url = url;
                    updateTabView(tab);
                    if (tab.isActive) {
                        updateUIForCurrentTab();
                    }

                    // 性能监控：记录页面开始加载时间
                    if (mIsMonitoringPerformance && tab.isActive) {
                        mPageLoadStartTime = System.currentTimeMillis();
                        android.util.Log.d("WebViewPerformance", "Page load started: " + url);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                TabData tab = findTabByWebView(view);
                if (tab != null) {
                    // 记录历史记录
                    if (tab.title != null && !tab.title.equals("新标签页")) {
                        mHistoryManager.addHistory(tab.title, url);
                    }

                    if (tab.isActive) {
                        updateNavigationButtons();
                        // 停止下拉刷新动画
                        if (mSwipeRefreshLayout != null) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }

                        // 性能监控：记录页面加载完成时间
                        if (mIsMonitoringPerformance && mPageLoadStartTime > 0) {
                            long loadTime = System.currentTimeMillis() - mPageLoadStartTime;
                            android.util.Log.d("WebViewPerformance", "Page load finished: " + url +
                                    " - Load time: " + loadTime + "ms");

                            // 可以在这里添加更详细的性能统计
                            // 例如：平均加载时间、最快/最慢加载时间等
                        }
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                android.util.Log.e(TAG, "WebView error: " + errorCode + " - " + description + " - URL: " + failingUrl);

                TabData tab = findTabByWebView(view);
                if (tab != null && tab.isActive) {
                    // 显示错误提示
                    showErrorPage(view, errorCode, description, failingUrl);

                    // 停止下拉刷新动画
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                android.util.Log.e(TAG, "HTTP error: " + errorResponse.getStatusCode() + " - " + errorResponse.getReasonPhrase() +
                        " - URL: " + request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("intent://") || url.startsWith("market://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                view.loadUrl(url);
                return true;
            }
        }));

        // 设置WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                TabData tab = findTabByWebView(view);
                if (tab != null && tab.isActive) {
                    if (mProgressBar != null) {
                        mProgressBar.setProgress(newProgress);
                        if (newProgress == 100) {
                            mProgressBar.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                TabData tab = findTabByWebView(view);
                if (tab != null) {
                    tab.title = title != null ? title : "无标题";
                    updateTabView(tab);
                    if (tab.isActive && getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(tab.title);
                    }
                }
            }
        });

        // 设置下载监听器
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                      String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.addRequestHeader("User-Agent", userAgent);
                    request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));

                    String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    request.setTitle(filename);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                                                 DownloadManager.Request.NETWORK_MOBILE);
                    request.setAllowedOverRoaming(true);

                    DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (downloadManager != null) {
                        downloadManager.enqueue(request);
                    }

                } catch (Exception e) {
                    android.util.Log.e("WebViewActivity", "Download error", e);
                }
            }
        });

        // 启用cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
    }

    /**
     * 创建标签页UI
     */
    private void createTabView(TabData tabData) {
        // 创建标签页容器
        LinearLayout tabLayout = new LinearLayout(this);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setBackgroundResource(R.drawable.tab_background);
        tabLayout.setPadding(16, 12, 16, 12);
        tabLayout.setMinimumWidth(220);
        tabLayout.setElevation(4f);

        // 添加点击效果
        android.content.res.ColorStateList colorStateList = new android.content.res.ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_pressed},
                new int[]{}
            },
            new int[]{
                android.graphics.Color.parseColor("#20000000"),
                android.graphics.Color.TRANSPARENT
            }
        );
        android.graphics.drawable.RippleDrawable rippleDrawable =
            new android.graphics.drawable.RippleDrawable(colorStateList, tabLayout.getBackground(), null);
        tabLayout.setBackground(rippleDrawable);

        // 网站图标
        ImageView faviconView = new ImageView(this);
        faviconView.setLayoutParams(new LinearLayout.LayoutParams(24, 24));
        faviconView.setImageResource(R.mipmap.ic_launcher);
        tabLayout.addView(faviconView);

        // 标题文本
        TextView titleView = new TextView(this);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleView.setText(tabData.title);
        titleView.setTextSize(14);
        titleView.setMaxLines(1);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleView.setPadding(8, 0, 8, 0);
        tabLayout.addView(titleView);

        // 关闭按钮
        ImageButton closeButton = new ImageButton(this);
        closeButton.setLayoutParams(new LinearLayout.LayoutParams(24, 24));
        closeButton.setImageResource(R.drawable.v_close_dark_x24);
        closeButton.setBackgroundResource(android.R.color.transparent);
        closeButton.setPadding(4, 4, 4, 4);
        tabLayout.addView(closeButton);

        // 设置点击事件
        tabLayout.setOnClickListener(v -> {
            int index = mTabs.indexOf(tabData);
            if (index >= 0) {
                switchToTab(index);
            }
        });

        closeButton.setOnClickListener(v -> {
            int index = mTabs.indexOf(tabData);
            if (index >= 0) {
                closeTab(index);
            }
        });

        tabData.tabView = tabLayout;
        mTabContainer.addView(tabLayout);
    }

    /**
     * 更新标签页显示
     */
    private void updateTabView(TabData tabData) {
        if (tabData.tabView instanceof LinearLayout) {
            LinearLayout tabLayout = (LinearLayout) tabData.tabView;
            // 更新标题
            if (tabLayout.getChildCount() >= 2) {
                View titleView = tabLayout.getChildAt(1);
                if (titleView instanceof TextView) {
                    ((TextView) titleView).setText(tabData.title);
                }
            }
            // 更新激活状态
            tabLayout.setBackgroundResource(tabData.isActive ?
                R.drawable.tab_background_active : R.drawable.tab_background);
        }
    }

    /**
     * 切换到指定标签页
     */
    private void switchToTab(int index) {
        if (index < 0 || index >= mTabs.size()) return;

        // 取消当前标签页的激活状态
        if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
            TabData oldTab = mTabs.get(mCurrentTabIndex);
            oldTab.isActive = false;
            updateTabView(oldTab);

            // 隐藏当前WebView
            if (oldTab.webView != null && oldTab.webView.getParent() != null) {
                ((android.view.ViewGroup) oldTab.webView.getParent()).removeView(oldTab.webView);
            }
        }

        // 激活新标签页
        mCurrentTabIndex = index;
        TabData newTab = mTabs.get(index);
        newTab.isActive = true;
        updateTabView(newTab);

        // 显示新WebView
        if (newTab.webView != null && mSwipeRefreshLayout != null) {
            // 确保WebView没有父视图
            if (newTab.webView.getParent() != null) {
                ((android.view.ViewGroup) newTab.webView.getParent()).removeView(newTab.webView);
            }

            // 添加到SwipeRefreshLayout
            mSwipeRefreshLayout.addView(newTab.webView, 0, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
            android.util.Log.d(TAG, "Switched to tab: " + index + ", URL: " + newTab.url);
        }

        // 更新UI
        updateUIForCurrentTab();

        // 滚动到当前标签页
        if (newTab.tabView != null && mTabScrollView != null) {
            mTabScrollView.post(() -> {
                int scrollX = newTab.tabView.getLeft() - (mTabScrollView.getWidth() - newTab.tabView.getWidth()) / 2;
                mTabScrollView.smoothScrollTo(Math.max(0, scrollX), 0);
            });
        }
    }

    /**
     * 关闭标签页
     */
    private void closeTab(int index) {
        if (index < 0 || index >= mTabs.size()) return;

        TabData tabToClose = mTabs.get(index);

        // 如果只剩一个标签页，不允许关闭
        if (mTabs.size() <= 1) {
            android.widget.Toast.makeText(this, "至少保留一个标签页", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 从容器中移除UI
        if (tabToClose.tabView != null && mTabContainer != null) {
            mTabContainer.removeView(tabToClose.tabView);
        }

        // 销毁WebView
        if (tabToClose.webView != null) {
            tabToClose.webView.stopLoading();
            tabToClose.webView.setWebViewClient(null);
            tabToClose.webView.setWebChromeClient(null);
            tabToClose.webView.destroy();
        }

        // 从列表中移除
        mTabs.remove(index);

        // 调整当前索引
        if (mCurrentTabIndex > index) {
            mCurrentTabIndex--;
        } else if (mCurrentTabIndex == index) {
            if (mCurrentTabIndex >= mTabs.size()) {
                mCurrentTabIndex = mTabs.size() - 1;
            }
            if (mCurrentTabIndex >= 0) {
                switchToTab(mCurrentTabIndex);
            }
        }
    }

    /**
     * 获取当前标签页
     */
    private TabData getCurrentTab() {
        if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
            return mTabs.get(mCurrentTabIndex);
        }
        return null;
    }

    /**
     * 根据WebView查找标签页
     */
    private TabData findTabByWebView(WebView webView) {
        for (TabData tab : mTabs) {
            if (tab.webView == webView) {
                return tab;
            }
        }
        return null;
    }

    /**
     * 在指定标签页中加载URL
     */
    private void loadUrlInTab(TabData tab, String input) {
        if (tab == null || tab.webView == null) return;

        String url = input.trim();

        // 如果不包含协议，智能处理
        if (!url.contains("://")) {
            if (url.contains(" ") || !url.contains(".")) {
                // 认为是搜索关键词，使用智能搜索引擎
                url = getSmartSearchUrl(url);
            } else {
                // 认为是网址，添加https协议
                url = "https://" + url;
            }
        }

        android.util.Log.d(TAG, "Loading URL in tab: " + url);
        try {
            tab.webView.loadUrl(url);
            android.util.Log.d(TAG, "URL loaded successfully: " + url);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to load URL: " + url, e);
            // 如果加载失败，尝试备用的加载方式
            try {
                if (url.startsWith("https://")) {
                    String httpUrl = url.replace("https://", "http://");
                    tab.webView.loadUrl(httpUrl);
                    android.util.Log.d(TAG, "Fallback to HTTP URL: " + httpUrl);
                }
            } catch (Exception e2) {
                android.util.Log.e(TAG, "Fallback loading also failed", e2);
            }
        }
    }

    /**
     * 在当前标签页中加载URL
     */
    private void loadUrlInCurrentTab(String url) {
        TabData currentTab = getCurrentTab();
        if (currentTab != null) {
            loadUrlInTab(currentTab, url);
        }
    }

    /**
     * 更新UI以反映当前标签页状态
     */
    private void updateUIForCurrentTab() {
        TabData currentTab = getCurrentTab();
        if (currentTab == null) return;

        // 更新地址栏
        updateUrlInput(currentTab.url);

        // 更新标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentTab.title);
        }

        // 更新导航按钮状态
        updateNavigationButtons();
    }

    /**
     * 显示标签页管理对话框
     */
    private void showTabManagementDialog() {
        String[] tabTitles = new String[mTabs.size()];
        for (int i = 0; i < mTabs.size(); i++) {
            TabData tab = mTabs.get(i);
            String prefix = (i == mCurrentTabIndex) ? "● " : "○ ";
            tabTitles[i] = prefix + (tab.title.length() > 20 ?
                tab.title.substring(0, 20) + "..." : tab.title);
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("标签页列表 (" + mTabs.size() + ")")
                .setItems(tabTitles, (dialog, which) -> switchToTab(which))
                .setPositiveButton("关闭", null)
                .show();
    }



    /**
     * 显示添加书签对话框
     */
    private void showAddBookmarkDialog() {
        TabData currentTab = getCurrentTab();
        if (currentTab == null || currentTab.url == null) {
            android.widget.Toast.makeText(this, "当前页面无法添加书签", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否已经收藏
        if (mBookmarkManager.isBookmarked(currentTab.url)) {
            android.widget.Toast.makeText(this, R.string.bookmark_exists, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.add_bookmark);

        // 创建输入框
        final android.widget.EditText titleEdit = new android.widget.EditText(this);
        titleEdit.setHint(R.string.bookmark_title);
        titleEdit.setText(currentTab.title);
        titleEdit.setSelection(titleEdit.getText().length());

        // 设置对话框布局
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        android.widget.TextView urlLabel = new android.widget.TextView(this);
        urlLabel.setText("网址：");
        urlLabel.setTextSize(12);
        urlLabel.setTextColor(getResources().getColor(android.R.color.secondary_text_light));

        android.widget.TextView urlText = new android.widget.TextView(this);
        urlText.setText(currentTab.url);
        urlText.setTextSize(12);
        urlText.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
        urlText.setMaxLines(2);
        urlText.setEllipsize(android.text.TextUtils.TruncateAt.END);

        layout.addView(titleEdit);
        layout.addView(urlLabel);
        layout.addView(urlText);

        builder.setView(layout);

        builder.setPositiveButton("添加", (dialog, which) -> {
            String title = titleEdit.getText().toString().trim();
            if (title.isEmpty()) {
                title = currentTab.title != null ? currentTab.title : "无标题";
            }

            BookmarkInfo bookmark = new BookmarkInfo(title, currentTab.url);
            long result = mBookmarkManager.addBookmark(bookmark);

            if (result > 0) {
                android.widget.Toast.makeText(this, R.string.bookmark_added, android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, R.string.bookmark_add_failed, android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 显示隐私保护设置对话框
     */
    private void showPrivacySettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.privacy_protection);

        // 创建设置项
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        // 广告拦截开关
        android.widget.LinearLayout adBlockLayout = new android.widget.LinearLayout(this);
        adBlockLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        adBlockLayout.setPadding(0, 8, 0, 8);

        // 显示当前WebView类型和详细信息
        android.widget.TextView webViewInfo = new android.widget.TextView(this);
        String webViewInfoText = mX5WebViewManager.getRecommendedWebViewType() + "\n\n" +
                                "详细信息:\n" + mX5WebViewManager.getX5InitStatusDetail();
        webViewInfo.setText(webViewInfoText);
        webViewInfo.setTextSize(12);
        webViewInfo.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
        webViewInfo.setPadding(0, 0, 0, 16);
        webViewInfo.setLineSpacing(4, 1.2f);

        // 广告拦截开关标签
        android.widget.TextView adBlockLabel = new android.widget.TextView(this);
        adBlockLabel.setText(R.string.ad_blocking);
        adBlockLabel.setTextSize(16);
        adBlockLabel.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.Switch adBlockSwitch = new android.widget.Switch(this);
        adBlockSwitch.setChecked(mAdBlockManager.isAdBlockEnabled());
        adBlockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mAdBlockManager.setAdBlockEnabled(isChecked);
            int messageRes = isChecked ? R.string.ad_block_enabled : R.string.ad_block_disabled;
            android.widget.Toast.makeText(this, messageRes, android.widget.Toast.LENGTH_SHORT).show();
        });

        adBlockLayout.addView(adBlockLabel);
        adBlockLayout.addView(adBlockSwitch);

        // 清除浏览数据按钮
        android.widget.Button clearDataButton = new android.widget.Button(this);
        clearDataButton.setText(R.string.clear_browsing_data);
        clearDataButton.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        clearDataButton.setOnClickListener(v -> showClearDataDialog());

        layout.addView(webViewInfo);
        layout.addView(adBlockLayout);
        layout.addView(clearDataButton);

        builder.setView(layout);
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    /**
     * 显示清除浏览数据对话框
     */
    private void showClearDataDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.clear_browsing_data)
                .setMessage(R.string.clear_data_confirm)
                .setPositiveButton(R.string.clear_browsing_data, (dialog, which) -> {
                    clearBrowsingData();
                    android.widget.Toast.makeText(this, R.string.browsing_data_cleared, android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 清除浏览数据
     */
    private void clearBrowsingData() {
        // 清除所有标签页的WebView数据
        for (TabData tab : mTabs) {
            if (tab.webView != null) {
                tab.webView.clearCache(true);
                tab.webView.clearHistory();
                // 清除Cookie
                android.webkit.CookieManager.getInstance().removeAllCookies(null);
            }
        }

        // 清除应用级别的缓存
        try {
            // 清除WebView缓存目录
            java.io.File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir);
            }

            // 清除应用数据缓存
            android.webkit.WebStorage.getInstance().deleteAllData();
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error clearing cache", e);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteRecursive(java.io.File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (java.io.File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    /**
     * 设置增强WebView回调
     */
    private void setupEnhancedWebViewCallbacks() {
        // 设置进度回调
        mEnhancedWebViewManager.setProgressCallback(new EnhancedWebViewManager.ProgressCallback() {
            @Override
            public void onProgressChanged(int progress) {
                if (mProgressBar != null) {
                    mProgressBar.setProgress(progress);
                    mProgressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onPageStarted(String url) {
                if (mIsMonitoringPerformance) {
                    mPageLoadStartTime = System.currentTimeMillis();
                }
            }

            @Override
            public void onPageFinished(String url, String title) {
                if (mIsMonitoringPerformance && mPageLoadStartTime > 0) {
                    long loadTime = System.currentTimeMillis() - mPageLoadStartTime;
                    android.util.Log.d(TAG, "页面加载完成: " + url + " - 用时: " + loadTime + "ms");
                }

                // 更新标题
                if (title != null && !title.isEmpty()) {
                    updateTabTitle(url, title);
                }
            }

            @Override
            public void onReceivedTitle(String title) {
                // 更新当前标签页标题
                TabData currentTab = getCurrentTab();
                if (currentTab != null) {
                    currentTab.title = title;
                    updateTabView(currentTab);
                    updateUIForCurrentTab();
                }
            }

            @Override
            public void onReceivedFavicon(Bitmap favicon) {
                // 更新favicon
                TabData currentTab = getCurrentTab();
                if (currentTab != null) {
                    currentTab.favicon = favicon;
                    updateTabView(currentTab);
                }
            }
        });

        // 设置错误回调
        mEnhancedWebViewManager.setErrorCallback(new EnhancedWebViewManager.ErrorCallback() {
            @Override
            public void onReceivedError(int errorCode, String description, String failingUrl) {
                android.util.Log.e(TAG, "WebView错误: " + errorCode + " - " + description + " - " + failingUrl);

                // 显示用户友好的错误提示
                runOnUiThread(() -> {
                    String errorMessage = "加载失败 (" + errorCode + "): " + description;
                    Toast.makeText(WebViewActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onReceivedHttpError(int statusCode, String reasonPhrase, String url) {
                android.util.Log.e(TAG, "HTTP错误: " + statusCode + " - " + reasonPhrase + " - " + url);

                runOnUiThread(() -> {
                    String errorMessage = "网络错误 " + statusCode + ": " + reasonPhrase;
                    Toast.makeText(WebViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });

        // 设置下载回调
        mEnhancedWebViewManager.setDownloadCallback(new EnhancedWebViewManager.DownloadCallback() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                       String mimetype, long contentLength) {
                android.util.Log.d(TAG, "开始下载: " + url + " - 类型: " + mimetype);

                runOnUiThread(() -> {
                    String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype);
                    Toast.makeText(WebViewActivity.this, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 更新标签页标题
     */
    private void updateTabTitle(String url, String title) {
        TabData tab = findTabByUrl(url);
        if (tab != null) {
            tab.title = title;
            updateTabView(tab);
        }
    }

    /**
     * 根据URL查找标签页
     */
    private TabData findTabByUrl(String url) {
        for (TabData tab : mTabs) {
            if (url.equals(tab.url)) {
                return tab;
            }
        }
        return null;
    }

    /**
     * 截图功能
     */
    private void takeScreenshot() {
        if (mEnhancedWebViewManager != null) {
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.title != null) {
                String fileName = currentTab.title.replaceAll("[^a-zA-Z0-9]", "_") + "_" +
                    System.currentTimeMillis();
                boolean success = mEnhancedWebViewManager.saveScreenshot(fileName);
                if (success) {
                    Toast.makeText(this, "截图已保存", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "截图保存失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "无法截图：页面未加载完成", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "截图功能不可用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 切换桌面模式
     */
    private void toggleDesktopMode() {
        if (mWebView != null) {
            WebSettings webSettings = mWebView.getSettings();
            String currentUA = webSettings.getUserAgentString();

            if (currentUA != null && currentUA.contains("Mobile")) {
                // 切换到桌面模式
                String desktopUA = currentUA.replace("Mobile", "Desktop");
                webSettings.setUserAgentString(desktopUA);
                webSettings.setUseWideViewPort(true);
                webSettings.setLoadWithOverviewMode(true);
                Toast.makeText(this, "已切换到桌面模式", Toast.LENGTH_SHORT).show();
            } else {
                // 切换到移动模式
                String mobileUA = "Mozilla/5.0 (Linux; Android " + android.os.Build.VERSION.RELEASE +
                    "; " + android.os.Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36";
                webSettings.setUserAgentString(mobileUA);
                webSettings.setUseWideViewPort(false);
                webSettings.setLoadWithOverviewMode(false);
                Toast.makeText(this, "已切换到移动模式", Toast.LENGTH_SHORT).show();
            }

            // 刷新页面
            mWebView.reload();
        }
    }

    /**
     * 清除WebView缓存
     */
    private void clearWebViewCache() {
        if (mWebView != null) {
            mWebView.clearCache(true);
            mWebView.clearHistory();
            WebStorage.getInstance().deleteAllData();

            // 清除Cookie
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);

            Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();

            // 刷新当前页面
            mWebView.reload();
        }
    }

    /**
     * 切换广告拦截
     */
    private void toggleAdBlock() {
        boolean isEnabled = mAdBlockManager.isAdBlockEnabled();
        mAdBlockManager.setAdBlockEnabled(!isEnabled);

        String message = !isEnabled ? "广告拦截已开启" : "广告拦截已关闭";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // 刷新页面以应用设置
        if (mWebView != null) {
            mWebView.reload();
        }
    }

    /**
     * 处理Activity结果（用于文件选择）
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 处理增强WebView管理器的Activity结果
        if (mEnhancedWebViewManager != null) {
            mEnhancedWebViewManager.onActivityResult(requestCode, resultCode, data);
        }
    }
}
