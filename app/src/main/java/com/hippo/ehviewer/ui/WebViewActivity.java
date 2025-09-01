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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Set;

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
import com.hippo.ehviewer.client.BrowserCoreManager;
import com.hippo.ehviewer.client.X5WebViewManager;
import com.hippo.ehviewer.util.DomainSuggestionManager;
import com.hippo.ehviewer.util.YouTubeCompatibilityManager;
import com.hippo.ehviewer.util.WebViewErrorHandler;
import com.hippo.ehviewer.util.VideoPlayerEnhancer;
import com.hippo.ehviewer.util.SmartUrlProcessor;
import com.hippo.ehviewer.util.UserAgentManager;
import com.hippo.ehviewer.util.BrowserCompatibilityManager;
import com.hippo.ehviewer.util.ContentPurifierManager;
import com.hippo.ehviewer.util.EroNovelDetector;
import com.hippo.ehviewer.client.NovelLibraryManager;
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
    private static final int REQUEST_CODE_TABS_MANAGER = 1001;

    // UI控件
    private AutoCompleteTextView mUrlInput;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageView mIncognitoIcon;
    private ImageView mSearchIcon;
    private ImageButton mClearButton;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageButton mHomeButton; // 移除：已移至顶部
    private ImageButton mRefreshButton; // 移除：已移至顶部
    private ImageButton mTopRefreshButton;
    private ImageButton mTopHomeButton;
    private ImageButton mTopBookmarkButton;
    private ImageButton mMenuButton;
    private ImageButton mBookmarkButton; // 移除：已移至顶部
    private ImageButton mBookmarkManagerButton;
    private ImageButton mHistoryButton;
    private FrameLayout mTabsButtonContainer;
    private ImageView mTabsButton;
    private TextView mTabsCount;
    private android.widget.HorizontalScrollView mTabScrollView;
    private android.widget.LinearLayout mTabContainer;

    // 管理器
    private BookmarkManager mBookmarkManager;
    private HistoryManager mHistoryManager;
    private AdBlockManager mAdBlockManager;
    private X5WebViewManager mX5WebViewManager;
    private BrowserCoreManager mBrowserCoreManager;
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
    private ContentPurifierManager mContentPurifier;

    // 小说相关
    private EroNovelDetector mNovelDetector;
    private NovelLibraryManager mNovelLibraryManager;
    
    // 视频全屏相关
    private FrameLayout mVideoFullscreenContainer;
    private View mCustomVideoView;
    private boolean mIsVideoFullscreen = false;

    // 静态数据类
    private static class TabData {
        WebView webView;
        EnhancedWebViewManager enhancedWebViewManager;
        String title;
        String url;
        boolean isIncognito = false;
        int tabId;

        TabData(WebView webView, EnhancedWebViewManager manager) {
            this.webView = webView;
            this.enhancedWebViewManager = manager;
            this.tabId = (int) (System.currentTimeMillis() % 10000); // 简单的ID生成
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
            // 注意：WebViewPoolManager.getInstance方法不存在，使用BrowserCoreManager替代
            mBrowserCoreManager = BrowserCoreManager.getInstance(this);
            mJavaScriptOptimizer = JavaScriptOptimizer.getInstance();
            mWebViewCacheManager = WebViewCacheManager.getInstance(this);
            mImageLazyLoader = ImageLazyLoader.getInstance();
            mMemoryManager = MemoryManager.getInstance(this);
            mReadingModeManager = ReadingModeManager.getInstance(this);

            // 提前初始化关键管理器，避免initializeViews中的NPE
            if (mUserAgentManager == null) {
                mUserAgentManager = new UserAgentManager(this);
            }
            if (mSmartUrlProcessor == null) {
                mSmartUrlProcessor = new SmartUrlProcessor(this);
            }

            // 初始化UI控件
            initializeViews();
            // 初始化增强WebView管理器（暂时设为null，会在创建标签页时设置）
            mEnhancedWebViewManager = null;

            // 处理Intent参数
            Intent intent = getIntent();
            String url = intent.getStringExtra(EXTRA_URL);
            String htmlContent = intent.getStringExtra("html_content");
            boolean isPreview = intent.getBooleanExtra("is_preview", false);
            Uri data = intent.getData();
            boolean fromBrowserLauncher = intent.getBooleanExtra("from_browser_launcher", false);
            boolean browserMode = intent.getBooleanExtra("browser_mode", false);
            
            // 处理翻译功能请求
            if (handleTranslationIntent(intent)) {
                return; // 如果是翻译请求，已经处理完毕，直接返回
            }

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
        mIncognitoIcon = findViewById(R.id.incognito_icon);
        mSearchIcon = findViewById(R.id.search_icon);
        mClearButton = findViewById(R.id.clear_button);

        // 调试日志
        android.util.Log.d("WebViewActivity", "=== INITIALIZE: UrlInput: " + mUrlInput);
        android.util.Log.d("WebViewActivity", "=== INITIALIZE: ClearButton: " + mClearButton);

        // 设置地址栏文本变化监听器，用于显示/隐藏清除按钮
        setupAddressBarTextWatcher();

        // 测试功能是否正常工作
        testModernFeatures();

        // 输出兼容性统计信息
        if (mBrowserCoreManager != null) {
            Log.d("WebViewActivity", "=== BROWSER COMPATIBILITY STATS ===");
            Log.d("WebViewActivity", "=== BrowserCoreManager initialized: " + mBrowserCoreManager);
            Log.d("WebViewActivity", "=== CompatibilityManager available: " + (mBrowserCoreManager.getRequestProcessor() != null));

            // 测试百度URL识别
            String testBaiduUrl = "https://ext.baidu.com/rest/id-mapping/cuid?callback=_box_jsonp810";
            Log.d("WebViewActivity", "=== TEST BAIDU URL: " + testBaiduUrl);
            if (mUserAgentManager != null) {
                Log.d("WebViewActivity", "=== BAIDU DETECTED: " + mUserAgentManager.isBaiduRelatedUrl(testBaiduUrl));
            } else {
                Log.w("WebViewActivity", "=== UserAgentManager is null, cannot test Baidu URL");
            }

            // 移除测试代码 - 确保高可用性，不在生产环境自动加载网页
        }

        // 初始化URL补全适配器
        setupUrlAutoComplete();
            mProgressBar = findViewById(R.id.progress_bar);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        
        // 底部按钮
        mBackButton = findViewById(R.id.back_button);
        mForwardButton = findViewById(R.id.forward_button);
        mBookmarkManagerButton = findViewById(R.id.bookmark_manager_button);
        mHistoryButton = findViewById(R.id.history_button);
        mMenuButton = findViewById(R.id.menu_button);
        
        // 顶部按钮
        mTopRefreshButton = findViewById(R.id.top_refresh_button);
        mTopHomeButton = findViewById(R.id.top_home_button);
        mTopBookmarkButton = findViewById(R.id.top_bookmark_button);
            
            // 多标签按钮
            mTabsButtonContainer = findViewById(R.id.tabs_button_container);
            mTabsButton = findViewById(R.id.tabs_button);
            mTabsCount = findViewById(R.id.tabs_count);

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
    /**
     * 测试现代化功能是否正常工作
     */
    private void testModernFeatures() {
        android.util.Log.d("WebViewActivity", "=== TEST: Testing modern features...");
        android.util.Log.d("WebViewActivity", "=== TEST: BrowserCoreManager: " + mBrowserCoreManager);
        android.util.Log.d("WebViewActivity", "=== TEST: ClearButton visibility: " + (mClearButton != null ? mClearButton.getVisibility() : "null"));

        // 测试智能搜索功能
        if (mBrowserCoreManager != null) {
            android.util.Log.d("WebViewActivity", "=== TEST: BrowserCoreManager is initialized, testing preload");
            mBrowserCoreManager.preloadForUrl("https://www.google.com");
        } else {
            android.util.Log.w("WebViewActivity", "=== TEST: BrowserCoreManager is null!");
        }

        // 移除测试代码 - 避免Handler内存泄漏和不必要的UI测试
    }

    /**
     * 设置地址栏文本变化监听器
     */
    private void setupAddressBarTextWatcher() {
        if (mUrlInput != null) {
            android.util.Log.d("WebViewActivity", "Setting up address bar text watcher - ClearButton: " + mClearButton);
            mUrlInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    // 根据文本长度显示/隐藏清除按钮
                    if (mClearButton != null) {
                        boolean hasText = s != null && s.length() > 0;
                        android.util.Log.d("WebViewActivity", "ClearButton visibility change: " + hasText + ", text: " + s);
                        mClearButton.setVisibility(hasText ? android.view.View.VISIBLE : android.view.View.GONE);

                        // 根据是否有文本调整搜索图标的状态
                        if (mSearchIcon != null) {
                            // 动态调整搜索图标的透明度
                            float alpha = hasText ? 0.7f : 1.0f;
                            mSearchIcon.setAlpha(alpha);
                        }
                    } else {
                        android.util.Log.w("WebViewActivity", "ClearButton is null!");
                    }
                }
            });
        }
    }

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
        // 注意：mRefreshButton已移除，功能已整合到顶部按钮

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

            // 新的顶部按钮监听器
            if (mTopHomeButton != null) {
                mTopHomeButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    TabData currentTab = getCurrentTab();
                    if (currentTab != null && currentTab.webView != null) {
                        // 智能选择主页搜索引擎
                        String homeUrl = getSmartHomeUrl();
                        loadUrlInCurrentTab(homeUrl);
                    }
                });
            }

            if (mTopBookmarkButton != null) {
                mTopBookmarkButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    android.util.Log.d("WebViewActivity", "Top bookmark button clicked");
                    toggleBookmarkCurrentPage();
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

            // 设置清除按钮监听器
            if (mClearButton != null) {
                android.util.Log.d("WebViewActivity", "=== WEBVIEWACTIVITY: Setting up clear button listener");

                mClearButton.setOnClickListener(v -> {
                    android.util.Log.d("WebViewActivity", "=== WEBVIEWACTIVITY: Clear button clicked");

                    // 点击动画效果
                    animateButtonClick(v);

                    // 清空地址栏文本
                    if (mUrlInput != null) {
                        mUrlInput.setText("");
                        mUrlInput.requestFocus();

                        // 显示软键盘
                        android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(mUrlInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                });
            } else {
                android.util.Log.w("WebViewActivity", "=== WEBVIEWACTIVITY: ClearButton is null in setupUIListeners");
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

            // 新的底部按钮监听器
            if (mBookmarkManagerButton != null) {
                mBookmarkManagerButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    android.util.Log.d("WebViewActivity", "Bookmark manager button clicked");
                    showBookmarkManager();
                });
                mBookmarkManagerButton.setVisibility(View.VISIBLE);
                mBookmarkManagerButton.setEnabled(true);
                android.util.Log.d("WebViewActivity", "Bookmark manager button initialized successfully");
            } else {
                android.util.Log.e("WebViewActivity", "Bookmark manager button is null!");
            }

            if (mHistoryButton != null) {
                mHistoryButton.setOnClickListener(v -> {
                    animateButtonClick(v);
                    android.util.Log.d("WebViewActivity", "History button clicked");
                    showHistoryManager();
                });
                mHistoryButton.setVisibility(View.VISIBLE);
                mHistoryButton.setEnabled(true);
                android.util.Log.d("WebViewActivity", "History button initialized successfully");
            } else {
                android.util.Log.e("WebViewActivity", "History button is null!");
            }
            
            // 多标签按钮监听器
            if (mTabsButtonContainer != null) {
                mTabsButtonContainer.setOnClickListener(v -> {
                    animateButtonClick(v);
                    android.util.Log.d("WebViewActivity", "Tabs button clicked");
                    openTabsManager();
                });
                mTabsButtonContainer.setVisibility(View.VISIBLE);
                mTabsButtonContainer.setEnabled(true);
                android.util.Log.d("WebViewActivity", "Tabs button initialized successfully");
                
                // 更新标签数量显示
                updateTabsCount();
            } else {
                android.util.Log.e("WebViewActivity", "Tabs button container is null!");
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
            // 创建新的WebView - 使用BrowserCoreManager
            WebView webView = mBrowserCoreManager.acquireOptimizedWebView(url);
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

            // 设置编码
            webSettings.setDefaultTextEncodingName("UTF-8");

            // 先初始化管理器
            if (mErrorHandler == null) {
                mErrorHandler = new WebViewErrorHandler(this, webView);
            }
            if (mVideoEnhancer == null) {
                mVideoEnhancer = new VideoPlayerEnhancer(this);
                mVideoEnhancer.enhanceWebView(webView);
            }
            
            // 初始化兼容性管理器
            BrowserCompatibilityManager compatibilityManager = BrowserCompatibilityManager.getInstance(this);
            // 兼容性配置会在loadUrl时应用
            // mSmartUrlProcessor和mUserAgentManager已在onCreate中初始化
            if (mContentPurifier == null) {
                mContentPurifier = ContentPurifierManager.getInstance(this);
            }
            if (mNovelDetector == null) {
                mNovelDetector = EroNovelDetector.getInstance();
            }
            if (mNovelLibraryManager == null) {
                mNovelLibraryManager = NovelLibraryManager.getInstance(this);
            }

            // 设置默认的移动版UA，让网站自己决定是否跳转
            // 使用系统默认UA，不主动设置
            // 让网站根据真实的设备信息进行响应式适配
            webSettings.setUserAgentString(null);
            android.util.Log.d("WebViewActivity", "Using system default UA for new WebView");

            // 设置WebViewClient来处理历史记录
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    // 如果是YouTube相关的页面加载成功，重置失败计数器
                    if (url != null && mUserAgentManager != null &&
                        (url.contains("youtube.com") || url.contains("youtu.be") || url.contains("googlevideo.com"))) {
                        mUserAgentManager.resetYouTubeFailureCount();
                        android.util.Log.d("WebViewActivity", "YouTube access successful, reset failure counter");
                    }

                    // 如果是百度相关的页面加载成功，重置失败计数器
                    if (url != null && mUserAgentManager != null && mUserAgentManager.isBaiduRelatedUrl(url)) {
                        mUserAgentManager.resetBaiduFailureCount();
                        android.util.Log.d("WebViewActivity", "Baidu access successful, reset failure counter");
                    }

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
                    
                    // 应用内容净化（视频和小说网站）
                    if (mContentPurifier != null) {
                        view.postDelayed(() -> {
                            mContentPurifier.applyContentPurification(view, url);
                        }, 2000); // 等待2秒让页面完全加载
                    }
                    
                    // 隐藏进度条
                    if (mProgressBar != null) {
                        mProgressBar.setVisibility(View.GONE);
                    }
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                    
                    // 更新书签按钮状态
                    if (mBookmarkManager != null && url != null) {
                        boolean isBookmarked = mBookmarkManager.isBookmarked(url);
                        updateBookmarkButtonState(isBookmarked);
                    }
                }

                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);

                    // 检查是否为特殊网站，如果是则应用兼容性设置
                    if (YouTubeCompatibilityManager.isSpecialSite(url)) {
                        YouTubeCompatibilityManager.applyYouTubeCompatibility(view, url, mUserAgentManager);
                        android.util.Log.d("WebViewActivity", "Applied YouTube compatibility for: " + url);
                    }
                    // 注意：不在onPageStarted中重复设置UA，避免干扰网站自身的跳转机制

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
                            // 移除UA干预 - UA伪造是导致循环的根本原因
                            // 现在使用系统默认UA，让网站自己处理适配
                            android.util.Log.d("WebViewActivity", "Using system default UA to prevent redirect loops - no UA intervention");
                        }

                        // 移除YouTube 403错误时的UA恢复策略
                        // UA切换会导致重定向循环问题

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

                    android.util.Log.e("WebViewActivity", "WebView error: " + errorCode + " - " + description + " - " + failingUrl);

                    // 处理403 Forbidden错误 - YouTube访问被拒绝
                    if ((errorCode == 403 || errorCode == WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME) &&
                        mUserAgentManager != null && failingUrl != null) {

                        // 检查是否是YouTube相关的URL
                        if (mUserAgentManager.isYouTubeRelatedUrl(failingUrl)) {
                            android.util.Log.d("WebViewActivity", "YouTube 403 error detected");

                            // 移除UA切换策略 - UA伪造会导致重定向循环
                            // 让网站根据真实的设备UA进行访问控制
                            android.util.Log.d("WebViewActivity", "Using system default UA for YouTube access");

                            // 简单的延迟重试，不改变UA
                            view.postDelayed(() -> {
                                view.reload();
                            }, 2000); // 增加延迟时间

                            return; // 等待重试结果
                        }

                        // 检查是否是百度相关的URL
                        else if (mUserAgentManager.isBaiduRelatedUrl(failingUrl)) {
                            android.util.Log.d("WebViewActivity", "=== BAIDU ERROR: " + errorCode + " - " + description + " - URL: " + failingUrl);

                            // 移除UA切换策略 - UA伪造会导致重定向循环
                            // 让网站根据真实的设备UA进行访问控制
                            android.util.Log.d("WebViewActivity", "Using system default UA for Baidu access");

                            // 显示用户友好的提示
                            android.widget.Toast.makeText(WebViewActivity.this,
                                "百度访问受限，请稍后重试", android.widget.Toast.LENGTH_SHORT).show();

                                // 延迟重试
                                view.postDelayed(() -> {
                                    android.util.Log.d("WebViewActivity", "=== BAIDU RETRY: Reloading with new UA");
                                    view.reload();
                                }, 1500); // 百度API可能需要更长的延迟

                                return; // 等待重试结果
                            } else {
                                android.util.Log.w("WebViewActivity", "=== BAIDU EXHAUSTED: All UA strategies tried");
                                mUserAgentManager.resetBaiduFailureCount();

                                // 显示失败提示
                                android.widget.Toast.makeText(WebViewActivity.this,
                                    "百度访问暂时不可用，请稍后重试", android.widget.Toast.LENGTH_LONG).show();
                            }
                        }
                    }


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

                @Override
                public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                    // 使用智能请求处理器处理请求
                    if (mBrowserCoreManager != null && mBrowserCoreManager.getRequestProcessor() != null) {
                        android.util.Log.d("WebViewActivity", "=== REQUEST INTERCEPT: " + request.getUrl());
                        return mBrowserCoreManager.getRequestProcessor().processRequest(view, request);
                    }

                    // 如果没有请求处理器，返回null让WebView正常处理
                    return super.shouldInterceptRequest(view, request);
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
     *
     * 注意：这个检测现在主要用于日志记录，因为我们不再通过UA干预来"修复"循环
     * UA伪造才是导致循环的根本原因，现在我们使用系统默认UA来避免这个问题
     */
    private boolean isYouTubeRedirectLoop(String currentUrl, String newUrl) {
        if (currentUrl == null || newUrl == null) return false;

        try {
            // 提取域名
            String currentDomain = extractDomainFromUrl(currentUrl);
            String newDomain = extractDomainFromUrl(newUrl);

            // 检查是否都是YouTube相关域名
            boolean isYouTubeRelated = (currentDomain != null && currentDomain.contains("youtube.com")) ||
                                      (newDomain != null && newDomain.contains("youtube.com")) ||
                                      (currentDomain != null && currentDomain.contains("youtu.be")) ||
                                      (newDomain != null && newDomain.contains("youtu.be"));

            if (!isYouTubeRelated) return false;

            // 检查是否在不同版本之间跳转 - 这通常是正常的网站适配行为
            boolean currentIsMobile = currentDomain != null && currentDomain.startsWith("m.youtube.com");
            boolean newIsMobile = newDomain != null && newDomain.startsWith("m.youtube.com");

            // 记录版本切换，但不认为是"循环"
            // 网站根据设备类型进行自动适配是正常的
            if (currentIsMobile != newIsMobile) {
                android.util.Log.d(TAG, "YouTube version switch (normal adaptation): " +
                    currentDomain + " -> " + newDomain + " (using system UA)");
                // 不认为是循环，因为这是网站正常的响应式适配
                return false;
            }

            // 检查是否是真正的循环（完全相同的URL）
            if (currentUrl.equals(newUrl)) {
                android.util.Log.w(TAG, "True YouTube redirect loop detected: " + currentUrl);
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
     * 显示增强的错误页面（支持智能重试）
     */
    private void showEnhancedErrorPage(WebView view, int errorCode, String description, String failingUrl) {
        try {
            String htmlContent = generateEnhancedErrorPageHtml(errorCode, description, failingUrl);
            loadHtmlContent(htmlContent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing enhanced error page", e);
            // 回退到简单错误页面
            showErrorPage(errorCode, description, failingUrl);
        }
    }

    /**
     * 生成增强的错误页面HTML（包含智能重试功能）
     */
    private String generateEnhancedErrorPageHtml(int errorCode, String description, String failingUrl) {
        StringBuilder html = new StringBuilder();

        // 获取错误类型描述
        String errorType = getErrorTypeDescription(errorCode);
        String userAgentStats = (mUserAgentManager != null) ? mUserAgentManager.getYouTubeFailureStats() : "";

        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("<title>加载失败 - EhViewer</title>")
            .append("<style>")
            .append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; }")
            .append(".error-container { max-width: 600px; background: white; border-radius: 15px; padding: 40px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); text-align: center; }")
            .append(".error-icon { font-size: 72px; margin-bottom: 20px; }")
            .append(".error-title { color: #333; margin-bottom: 10px; font-size: 28px; font-weight: 600; }")
            .append(".error-type { color: #666; margin-bottom: 20px; font-size: 16px; padding: 8px 16px; background: #f8f9fa; border-radius: 20px; display: inline-block; }")
            .append(".error-description { color: #666; margin-bottom: 30px; line-height: 1.6; font-size: 16px; }")
            .append(".error-url { background: #f8f9fa; padding: 15px; border-radius: 8px; font-family: 'Consolas', 'Monaco', monospace; margin-bottom: 20px; word-break: break-all; font-size: 14px; color: #495057; }")
            .append(".retry-btn { background: linear-gradient(45deg, #ff6b35, #f7931e); color: white; border: none; padding: 15px 30px; border-radius: 25px; font-size: 16px; font-weight: 600; cursor: pointer; margin: 8px; transition: all 0.3s ease; }")
            .append(".retry-btn:hover { transform: translateY(-2px); box-shadow: 0 5px 15px rgba(255, 107, 53, 0.4); }")
            .append(".secondary-btn { background: #6c757d; color: white; border: none; padding: 12px 25px; border-radius: 20px; font-size: 14px; cursor: pointer; margin: 8px; transition: all 0.3s ease; }")
            .append(".secondary-btn:hover { background: #5a6268; }")
            .append(".stats-info { background: #e9ecef; padding: 15px; border-radius: 8px; margin-top: 20px; font-size: 12px; color: #495057; text-align: left; }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class='error-container'>")
            .append("<div class='error-icon'>🚨</div>")
            .append("<h1 class='error-title'>网页加载失败</h1>")
            .append("<div class='error-type'>").append(errorType).append("</div>")
            .append("<div class='error-description'>")
            .append("<p>错误代码: <strong>").append(errorCode).append("</strong></p>")
            .append("<p>").append(description).append("</p>")
            .append("</div>");

        if (failingUrl != null && !failingUrl.isEmpty()) {
            html.append("<div class='error-url'>").append(failingUrl).append("</div>");
        }

        // 智能重试按钮（仅对YouTube相关错误）
        if (failingUrl != null && failingUrl.contains("youtube.com") && mUserAgentManager != null && mUserAgentManager.shouldRetryYouTube()) {
            html.append("<button class='retry-btn' onclick='smartRetry()'>智能重试 (换UA)</button>");
        }

        html.append("<button class='retry-btn' onclick='retryLoad()'>重新加载</button>")
            .append("<button class='secondary-btn' onclick='searchInstead()'>搜索页面</button>")
            .append("<button class='secondary-btn' onclick='copyError()'>复制错误信息</button>");

        // 显示User-Agent统计信息
        if (!userAgentStats.isEmpty()) {
            html.append("<div class='stats-info'>")
                .append("<strong>重试统计:</strong><br>")
                .append(userAgentStats)
                .append("</div>");
        }

        html.append("</div>")
            .append("<script>")
            .append("function retryLoad() { location.reload(); }")
            .append("function searchInstead() { ")
            .append("  var query = encodeURIComponent('" + (failingUrl != null ? failingUrl : "") + "');")
            .append("  window.location.href = 'https://www.baidu.com/s?wd=' + query;")
            .append("}")
            .append("function smartRetry() { location.reload(); }")
            .append("function copyError() { ")
            .append("  var errorInfo = 'EhViewer错误报告\\n';")
            .append("  errorInfo += '时间: ' + new Date().toLocaleString() + '\\n';")
            .append("  errorInfo += '错误代码: ").append(errorCode).append("\\n';")
            .append("  errorInfo += '错误描述: ").append(description.replace("'", "\\'")).append("\\n';")
            .append("  errorInfo += 'URL: ").append((failingUrl != null ? failingUrl : "")).append("\\n';")
            .append("  errorInfo += '").append(userAgentStats.replace("'", "\\'")).append("\\n';")
            .append("  navigator.clipboard.writeText(errorInfo).then(function() {")
            .append("    alert('错误信息已复制到剪贴板');")
            .append("  }).catch(function(err) {")
            .append("    alert('复制失败: ' + err);")
            .append("  });")
            .append("}")
            .append("</script>")
            .append("</body>")
            .append("</html>");

        return html.toString();
    }

    /**
     * 获取错误类型描述
     */
    private String getErrorTypeDescription(int errorCode) {
        switch (errorCode) {
            case 403:
                return "访问被拒绝";
            case 404:
                return "页面未找到";
            case 500:
            case 502:
            case 503:
                return "服务器错误";
            case WebViewClient.ERROR_HOST_LOOKUP:
                return "DNS解析失败";
            case WebViewClient.ERROR_TIMEOUT:
                return "请求超时";
            case WebViewClient.ERROR_CONNECT:
                return "连接失败";
            case WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME:
                return "认证失败";
            default:
                if (errorCode >= 400 && errorCode < 500) {
                    return "客户端错误";
                } else if (errorCode >= 500) {
                    return "服务器错误";
                } else {
                    return "网络错误";
                }
        }
    }

    /**
     * 生成错误页面HTML（原有方法保持兼容）
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
    /**
     * 智能加载URL或搜索查询到当前标签页
     * 支持自动识别URL和搜索关键词
     */
    private void loadUrlInCurrentTab(String input) {
        android.util.Log.d("WebViewActivity", "=== WEBVIEWACTIVITY: loadUrlInCurrentTab called with input: " + input);
        android.util.Log.d("WebViewActivity", "=== WEBVIEWACTIVITY: BrowserCoreManager instance: " + mBrowserCoreManager);

        try {
            if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
                TabData currentTab = mTabs.get(mCurrentTabIndex);
                android.util.Log.d("WebViewActivity", "=== WEBVIEWACTIVITY: Current tab: " + currentTab + ", WebView: " + (currentTab != null ? currentTab.webView : "null"));

                if (currentTab != null && currentTab.webView != null) {

                    // 智能处理输入：URL或搜索关键词
                    String processedUrl = processInput(input);
                    android.util.Log.d("WebViewActivity", "=== WEBVIEWACTIVITY: Processed URL: " + processedUrl);

                    // 使用BrowserCoreManager进行优化加载
                    if (mBrowserCoreManager != null) {
                        android.util.Log.d("WebViewActivity", "=== WEBVIEWACTIVITY: Using BrowserCoreManager for preloading");

                        // 预加载相关资源
                        mBrowserCoreManager.preloadForUrl(processedUrl);

                        // 获取优化的WebView进行加载
                        currentTab.webView.loadUrl(processedUrl);
                    } else {
                        android.util.Log.w("WebViewActivity", "=== WEBVIEWACTIVITY: BrowserCoreManager is null, using fallback");
                        // 回退到直接加载
                        currentTab.webView.loadUrl(processedUrl);
                    }
                } else {
                    android.util.Log.w("WebViewActivity", "=== WEBVIEWACTIVITY: Current tab or WebView is null");
                }
            } else {
                android.util.Log.w("WebViewActivity", "=== WEBVIEWACTIVITY: Invalid tab index: " + mCurrentTabIndex + ", tabs size: " + mTabs.size());
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "=== WEBVIEWACTIVITY: Error loading URL in current tab", e);
        }
    }

    /**
     * 智能处理用户输入：自动识别URL或转换为搜索
     */
    private String processInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "https://www.google.com";
        }

        String trimmedInput = input.trim();

        // 检查是否是URL
        if (isValidUrl(trimmedInput)) {
            // 如果没有协议，添加https://
            if (!trimmedInput.contains("://")) {
                return "https://" + trimmedInput;
            }
            return trimmedInput;
        }

        // 检查是否是搜索关键词（包含空格或中文字符）
        if (trimmedInput.contains(" ") || containsChinese(trimmedInput)) {
            return buildSearchUrl(trimmedInput);
        }

        // 检查是否可能是域名（包含点号）
        if (trimmedInput.contains(".")) {
            return "https://" + trimmedInput;
        }

        // 默认为搜索
        return buildSearchUrl(trimmedInput);
    }

    /**
     * 检查是否是有效的URL
     */
    private boolean isValidUrl(String input) {
        try {
            // 检查基本URL格式
            if (input.contains("://")) {
                java.net.URL url = new java.net.URL(input);
                return true;
            }

            // 检查域名格式
            if (input.contains(".") && !input.contains(" ")) {
                // 简单的域名验证
                String[] parts = input.split("\\.");
                return parts.length >= 2 && parts[parts.length - 1].length() >= 2;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否包含中文字符
     */
    private boolean containsChinese(String input) {
        for (char c : input.toCharArray()) {
            if ((c >= 0x4E00 && c <= 0x9FFF) || // 中文字符范围
                (c >= 0x3400 && c <= 0x4DBF) || // 扩展A
                (c >= 0x20000 && c <= 0x2A6DF) || // 扩展B
                (c >= 0x2A700 && c <= 0x2B73F) || // 扩展C
                (c >= 0x2B740 && c <= 0x2B81F) || // 扩展D
                (c >= 0x2B820 && c <= 0x2CEAF)) { // 扩展E
                return true;
            }
        }
        return false;
    }

    /**
     * 构建搜索URL
     */
    private String buildSearchUrl(String query) {
        try {
            // 对查询进行URL编码
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");

            // 默认使用Google搜索，也可以使用百度等
            if (containsChinese(query)) {
                // 中文查询使用百度
                return "https://www.baidu.com/s?wd=" + encodedQuery;
            } else {
                // 英文查询使用Google
                return "https://www.google.com/search?q=" + encodedQuery;
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error encoding search query", e);
            return "https://www.google.com";
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

            // 设置视频播放回调
            manager.setVideoPlaybackCallback(new EnhancedWebViewManager.VideoPlaybackCallback() {
                @Override
                public void onShowVideoFullscreen(View view, android.webkit.WebChromeClient.CustomViewCallback callback) {
                    runOnUiThread(() -> showVideoFullscreen(view, callback));
                }

                @Override
                public void onHideVideoFullscreen() {
                    runOnUiThread(() -> hideVideoFullscreen());
                }

                @Override
                public boolean isVideoFullscreen() {
                    return mIsVideoFullscreen;
                }
            });

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
            
            // 使用兼容性管理器进行全面的兼容性处理
            BrowserCompatibilityManager compatibilityManager = BrowserCompatibilityManager.getInstance(this);
            compatibilityManager.applyCompatibilityConfig(tabData.webView, processedUrl);
            
            // 传统UA设置作为备用（兼容性管理器已处理，这里保留作为fallback）
            if (mUserAgentManager != null) {
                String domain = mUserAgentManager.extractDomain(processedUrl);
                android.util.Log.d("WebViewActivity", "Domain: " + domain + " (兼容性管理器已处理)");
            }
            
            // 兼容性管理器会处理URL适配，如果没有重定向则正常加载
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

    private WebView getCurrentWebView() {
        TabData currentTab = getCurrentTab();
        return currentTab != null ? currentTab.webView : null;
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
                            android.util.Log.d("WebViewActivity", "Page search feature in development");
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
            builder.setTitle("菜单");

            // 紧凑的菜单项，移除了已经在顶部/底部的功能
            String[] menuItems = {
                "📸 网页截图",
                "💻 桌面/移动模式", 
                "📖 阅读模式",
                "🎬 视频净化模式",
                "📚 小说净化模式",
                "📖 检测小说内容",
                "📚 小说书库",
                "🚫 元素屏蔽模式",
                "🛡️ 屏蔽记录管理",
                "🔐 进入私密模式",
                "⚙️ 浏览器设置"
            };

            builder.setItems(menuItems, (dialog, which) -> {
                try {
                    android.util.Log.d("WebViewActivity", "Menu item selected: " + which);
                    switch (which) {
                        case 0: // 网页截图
                            android.util.Log.d("WebViewActivity", "Taking screenshot");
                            takeScreenshot();
                            break;
                        case 1: // 桌面/移动模式
                            android.util.Log.d("WebViewActivity", "Toggling desktop mode");
                            toggleDesktopMode();
                            break;
                        case 2: // 阅读模式
                            android.util.Log.d("WebViewActivity", "Toggling reading mode");
                            toggleReadingMode();
                            break;
                        case 3: // 视频净化模式
                            android.util.Log.d("WebViewActivity", "Toggling video purification mode");
                            toggleVideoPurificationMode();
                            break;
                        case 4: // 小说净化模式
                            android.util.Log.d("WebViewActivity", "Toggling novel purification mode");
                            toggleNovelPurificationMode();
                            break;
                        case 5: // 检测小说内容
                            android.util.Log.d("WebViewActivity", "Detecting novel content");
                            detectNovelContent();
                            break;
                        case 6: // 小说书库
                            android.util.Log.d("WebViewActivity", "Opening novel library");
                            openNovelLibrary();
                            break;
                        case 7: // 元素屏蔽模式
                            android.util.Log.d("WebViewActivity", "Starting element blocking mode");
                            startElementBlockingMode();
                            break;
                        case 8: // 屏蔽记录管理
                            android.util.Log.d("WebViewActivity", "Opening block list manager");
                            showBlockListManager();
                            break;
                        case 9: // 进入私密模式
                            android.util.Log.d("WebViewActivity", "Entering incognito mode");
                            Toast.makeText(this, "私密模式功能开发中", Toast.LENGTH_SHORT).show();
                            break;
                        case 10: // 浏览器设置
                            android.util.Log.d("WebViewActivity", "Opening browser settings");
                            Toast.makeText(this, "浏览器设置功能开发中", Toast.LENGTH_SHORT).show();
                            break;
                    }
                } catch (Exception e) {
                    android.util.Log.e("WebViewActivity", "Error handling menu item: " + which, e);
                    Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("关闭", null);

            android.app.AlertDialog dialog = builder.create();
            
            // 自定义紧凑菜单样式
            dialog.show();
            
            // 设置更小的字体和紧凑间距
            try {
                android.widget.ListView listView = dialog.getListView();
                if (listView != null) {
                    listView.setPadding(16, 8, 16, 8);
                    listView.setDividerHeight(1);
                    
                    // 调整每个菜单项的样式
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        android.view.View child = listView.getChildAt(i);
                        if (child instanceof android.widget.TextView) {
                            android.widget.TextView textView = (android.widget.TextView) child;
                            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14); // 更小字体
                            textView.setPadding(24, 8, 24, 8); // 紧凑间距
                        }
                    }
                }
                
                // 设置窗口属性
                android.view.Window window = dialog.getWindow();
                if (window != null) {
                    android.view.WindowManager.LayoutParams lp = window.getAttributes();
                    lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8); // 窗口宽度80%
                    window.setAttributes(lp);
                }
            } catch (Exception e) {
                android.util.Log.w("WebViewActivity", "Could not customize menu dialog style", e);
            }

            android.util.Log.d("WebViewActivity", "Menu dialog shown successfully");

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing menu dialog", e);
            Toast.makeText(this, "无法显示菜单: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // 如果处于视频全屏模式，先退出全屏
        if (mIsVideoFullscreen) {
            hideVideoFullscreen();
            return;
        }
        
        // 检查WebView是否可以后退
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

            // 清理管理器 - 防止内存泄漏
            try {
                if (mMemoryManager != null) {
                    // MemoryManager没有cleanup方法，直接设为null释放引用
                    mMemoryManager = null;
                }
                
                // 清理其他管理器
                if (mBookmarkManager != null) {
                    mBookmarkManager = null;
                }
                
                if (mHistoryManager != null) {
                    mHistoryManager = null;
                }
                
                if (mUserAgentManager != null) {
                    mUserAgentManager = null;
                }
                
                if (mAdBlockManager != null) {
                    mAdBlockManager = null;
                }
            } catch (Exception e) {
                android.util.Log.w("WebViewActivity", "Error cleaning managers", e);
            }
            
            // 清理视频增强器
            if (mVideoEnhancer != null) {
                mVideoEnhancer.cleanup();
                mVideoEnhancer = null;
            }
            
            // 清理兼容性管理器
            BrowserCompatibilityManager compatibilityManager = BrowserCompatibilityManager.getInstance(this);
            compatibilityManager.cleanup();

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

    /**
     * 切换当前页面的书签状态（顶部收藏按钮使用）
     */
    private void toggleBookmarkCurrentPage() {
        toggleBookmarkForCurrentPage();
    }

    /**
     * 显示书签管理器
     */
    private void showBookmarkManager() {
        try {
            android.util.Log.d("WebViewActivity", "Opening bookmark manager");
            if (mBookmarkManager != null) {
                // 显示书签列表对话框
                showBookmarkListDialog();
            } else {
                Toast.makeText(this, "书签管理器未初始化", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error opening bookmark manager", e);
            Toast.makeText(this, "打开书签管理器失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示历史记录管理器
     */
    private void showHistoryManager() {
        try {
            android.util.Log.d("WebViewActivity", "Opening history manager");
            if (mHistoryManager != null) {
                // 显示历史记录列表对话框
                showHistoryListDialog();
            } else {
                Toast.makeText(this, "历史记录管理器未初始化", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error opening history manager", e);
            Toast.makeText(this, "打开历史记录管理器失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示书签列表对话框
     */
    private void showBookmarkListDialog() {
        try {
            if (mBookmarkManager != null) {
                // 获取所有书签
                java.util.List<com.hippo.ehviewer.client.data.BookmarkInfo> bookmarks = mBookmarkManager.getAllBookmarks();
                
                if (bookmarks.isEmpty()) {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("书签管理");
                    builder.setMessage("还没有收藏任何书签\n\n在浏览网页时点击顶部的收藏按钮即可添加书签");
                    builder.setPositiveButton("确定", null);
                    builder.show();
                    return;
                }
                
                // 创建简洁的书签列表
                String[] bookmarkTitles = new String[bookmarks.size()];
                for (int i = 0; i < bookmarks.size(); i++) {
                    com.hippo.ehviewer.client.data.BookmarkInfo bookmark = bookmarks.get(i);
                    
                    // 限制标题长度
                    String displayTitle = (bookmark.title != null && !bookmark.title.trim().isEmpty()) 
                        ? bookmark.title : "未命名书签";
                    if (displayTitle.length() > 35) {
                        displayTitle = displayTitle.substring(0, 32) + "...";
                    }
                    
                    // 简化URL显示
                    String shortUrl = bookmark.url;
                    if (shortUrl.length() > 45) {
                        try {
                            java.net.URL url = new java.net.URL(shortUrl);
                            shortUrl = url.getHost() + "...";
                        } catch (Exception e) {
                            shortUrl = shortUrl.substring(0, 42) + "...";
                        }
                    }
                    
                    // 格式化收藏时间
                    String createTimeStr = "";
                    if (bookmark.createTime > 0) {
                        long timeDiff = System.currentTimeMillis() - bookmark.createTime;
                        if (timeDiff < 86400000) { // 24小时内
                            createTimeStr = " • 今天收藏";
                        } else if (timeDiff < 604800000) { // 7天内
                            createTimeStr = " • " + (timeDiff / 86400000) + "天前收藏";
                        } else {
                            createTimeStr = " • " + android.text.format.DateFormat.format("MM-dd", bookmark.createTime).toString() + " 收藏";
                        }
                    }
                    
                    bookmarkTitles[i] = "⭐ " + displayTitle + "\n🔗 " + shortUrl + createTimeStr;
                }
                
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("书签管理 (" + bookmarks.size() + "个)");
                
                // 书签列表点击事件
                builder.setItems(bookmarkTitles, (dialog, which) -> {
                    com.hippo.ehviewer.client.data.BookmarkInfo selectedBookmark = bookmarks.get(which);
                    loadUrlInCurrentTab(selectedBookmark.url);
                });
                
                // 添加长按删除功能的说明
                builder.setMessage("点击书签打开，长按可删除书签");
                
                // 清除所有书签选项
                builder.setNeutralButton("清空所有", (dialog, which) -> {
                    android.app.AlertDialog.Builder confirmBuilder = new android.app.AlertDialog.Builder(this);
                    confirmBuilder.setTitle("确认清空");
                    confirmBuilder.setMessage("确定要删除所有书签吗？此操作不可撤销。");
                    confirmBuilder.setPositiveButton("确定", (d, w) -> {
                        mBookmarkManager.clearAllBookmarks();
                    });
                    confirmBuilder.setNegativeButton("取消", null);
                    confirmBuilder.show();
                });
                
                builder.setNegativeButton("关闭", null);
                builder.show();
                
            } else {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("错误");
                builder.setMessage("书签管理器未初始化，请重启应用后重试");
                builder.setPositiveButton("确定", null);
                builder.show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing bookmark list", e);
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("错误");
            builder.setMessage("无法加载书签列表：" + e.getMessage());
            builder.setPositiveButton("确定", null);
            builder.show();
        }
    }

    /**
     * 显示历史记录列表对话框
     */
    private void showHistoryListDialog() {
        try {
            if (mHistoryManager != null) {
                // 获取最近30条历史记录
                java.util.List<com.hippo.ehviewer.client.data.HistoryInfo> historyList = mHistoryManager.getRecentHistory(30);
                
                if (historyList.isEmpty()) {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("历史记录");
                    builder.setMessage("还没有浏览历史记录\n\n开始浏览网页后这里会显示访问过的网站");
                    builder.setPositiveButton("确定", null);
                    builder.show();
                    return;
                }
                
                // 创建简洁的历史记录列表
                String[] historyTitles = new String[historyList.size()];
                for (int i = 0; i < historyList.size(); i++) {
                    com.hippo.ehviewer.client.data.HistoryInfo history = historyList.get(i);
                    
                    // 限制标题长度，确保显示美观
                    String displayTitle = (history.title != null && !history.title.trim().isEmpty()) 
                        ? history.title : "未命名页面";
                    if (displayTitle.length() > 35) {
                        displayTitle = displayTitle.substring(0, 32) + "...";
                    }
                    
                    // 简化URL显示
                    String shortUrl = history.url;
                    if (shortUrl.length() > 45) {
                        try {
                            java.net.URL url = new java.net.URL(shortUrl);
                            shortUrl = url.getHost() + "...";
                        } catch (Exception e) {
                            shortUrl = shortUrl.substring(0, 42) + "...";
                        }
                    }
                    
                    // 格式化访问时间
                    String timeStr;
                    long timeDiff = System.currentTimeMillis() - history.visitTime;
                    if (timeDiff < 60000) { // 1分钟内
                        timeStr = "刚刚";
                    } else if (timeDiff < 3600000) { // 1小时内
                        timeStr = (timeDiff / 60000) + "分钟前";
                    } else if (timeDiff < 86400000) { // 24小时内
                        timeStr = (timeDiff / 3600000) + "小时前";
                    } else if (timeDiff < 604800000) { // 7天内
                        timeStr = (timeDiff / 86400000) + "天前";
                    } else {
                        timeStr = android.text.format.DateFormat.format("MM-dd", history.visitTime).toString();
                    }
                    
                    // 访问次数显示优化
                    String visitInfo = "";
                    if (history.visitCount > 1) {
                        visitInfo = " • " + history.visitCount + "次";
                    }
                    
                    historyTitles[i] = "📄 " + displayTitle + "\n🔗 " + shortUrl + "\n⏰ " + timeStr + visitInfo;
                }
                
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("历史记录 (最近" + historyList.size() + "条)");
                
                // 历史记录列表点击事件
                builder.setItems(historyTitles, (dialog, which) -> {
                    com.hippo.ehviewer.client.data.HistoryInfo selectedHistory = historyList.get(which);
                    loadUrlInCurrentTab(selectedHistory.url);
                });
                
                // 清除历史记录选项
                builder.setNeutralButton("清空历史", (dialog, which) -> {
                    android.app.AlertDialog.Builder confirmBuilder = new android.app.AlertDialog.Builder(this);
                    confirmBuilder.setTitle("确认清空");
                    confirmBuilder.setMessage("确定要清除所有浏览历史记录吗？此操作不可撤销。");
                    confirmBuilder.setPositiveButton("确定", (d, w) -> {
                        mHistoryManager.clearAllHistory();
                    });
                    confirmBuilder.setNegativeButton("取消", null);
                    confirmBuilder.show();
                });
                
                builder.setNegativeButton("关闭", null);
                builder.show();
                
            } else {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("错误");
                builder.setMessage("历史记录管理器未初始化，请重启应用后重试");
                builder.setPositiveButton("确定", null);
                builder.show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing history list", e);
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("错误");
            builder.setMessage("无法加载历史记录：" + e.getMessage());
            builder.setPositiveButton("确定", null);
            builder.show();
        }
    }

    private void updateBookmarkButtonState(boolean isBookmarked) {
        // 更新顶部收藏按钮状态
        if (mTopBookmarkButton != null) {
            try {
                if (isBookmarked) {
                    // 收藏状态：使用实心星（填充颜色）
                    mTopBookmarkButton.setImageResource(R.drawable.v_heart_black_x24);
                    mTopBookmarkButton.setColorFilter(getResources().getColor(android.R.color.holo_red_light));
                } else {
                    // 未收藏状态：使用空心星（无填充）
                    mTopBookmarkButton.setImageResource(R.drawable.v_heart_black_x24);
                    mTopBookmarkButton.clearColorFilter();
                }
            } catch (Exception e) {
                android.util.Log.w("WebViewActivity", "Error updating bookmark button state", e);
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
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null) {
                // 使用WebView的截图功能
                currentTab.webView.post(() -> {
                    try {
                        android.graphics.Bitmap screenshot = android.graphics.Bitmap.createBitmap(
                            currentTab.webView.getWidth(),
                            currentTab.webView.getHeight(),
                            android.graphics.Bitmap.Config.ARGB_8888
                        );
                        android.graphics.Canvas canvas = new android.graphics.Canvas(screenshot);
                        currentTab.webView.draw(canvas);
                        
                        // 保存到图库
                        String fileName = "EhViewer_Screenshot_" + System.currentTimeMillis() + ".png";
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
                        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                        
                        android.content.ContentResolver resolver = getContentResolver();
                        android.net.Uri imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        
                        if (imageUri != null) {
                            try (java.io.OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream);
                                Toast.makeText(this, "截图已保存到图库", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WebViewActivity", "Error saving screenshot", e);
                        Toast.makeText(this, "截图保存失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error taking screenshot", e);
            Toast.makeText(this, "截图失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDesktopMode() {
        try {
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null && mUserAgentManager != null) {
                String currentUA = currentTab.webView.getSettings().getUserAgentString();
                
                // 移除UA修改，让WebView使用系统默认UA
                // 网站应该根据真实的设备信息进行响应式适配
                if (currentTab.webView != null && currentTab.webView.getSettings() != null) {
                    // 清除任何自定义UA设置，使用系统默认
                    currentTab.webView.getSettings().setUserAgentString(null);
                    currentTab.webView.reload();

                    String modeName = isDesktopMode ? "桌面显示模式" : "移动显示模式";
                    Toast.makeText(this, "已切换到" + modeName + "（使用系统默认UA）", Toast.LENGTH_SHORT).show();
                    android.util.Log.d("WebViewActivity", "Switched to " + modeName + " with system default UA");
                }
            } else {
                Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error toggling desktop mode", e);
            Toast.makeText(this, "模式切换失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addCurrentPageToBookmarks() {
        try {
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null) {
                String url = currentTab.webView.getUrl();
                String title = currentTab.webView.getTitle();
                
                if (url != null && !url.isEmpty()) {
                    if (title == null || title.isEmpty()) {
                        title = url;
                    }
                    
                    BookmarkInfo bookmark = new BookmarkInfo();
                    bookmark.title = title;
                    bookmark.url = url;
                    bookmark.faviconUrl = null;
                    bookmark.createTime = System.currentTimeMillis();
                    bookmark.lastVisitTime = bookmark.createTime;
                    bookmark.visitCount = 1;
                    
                    mBookmarkManager.addBookmark(bookmark);
                    Toast.makeText(this, "已添加到书签: " + title, Toast.LENGTH_SHORT).show();
                    android.util.Log.d("WebViewActivity", "Bookmark added: " + title + " - " + url);
                } else {
                    Toast.makeText(this, "无法获取当前页面信息", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error adding bookmark", e);
            Toast.makeText(this, "添加书签失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshCurrentPage() {
        try {
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null) {
                currentTab.webView.reload();
                Toast.makeText(this, "正在刷新页面", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error refreshing page", e);
            Toast.makeText(this, "刷新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHomepage() {
        try {
            TabData currentTab = getCurrentTab();
            if (currentTab != null && currentTab.webView != null) {
                currentTab.webView.loadUrl("https://www.google.com");
                Toast.makeText(this, "返回主页", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无法获取当前页面", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error going to homepage", e);
            Toast.makeText(this, "加载主页失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void enterPrivateMode() {
        try {
            Intent intent = new Intent(this, EhBrowserActivity.class);
            intent.putExtra("enter_private_mode", true);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error entering private mode", e);
            Toast.makeText(this, "进入私密模式失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void toggleVideoPurificationMode() {
        try {
            if (mContentPurifier != null) {
                boolean enabled = mContentPurifier.isVideoModeEnabled();
                mContentPurifier.setVideoModeEnabled(!enabled);
                
                String status = !enabled ? "已启用" : "已禁用";
                Toast.makeText(this, "视频净化模式" + status, Toast.LENGTH_SHORT).show();
                
                // 刷新当前页面以应用新设置
                TabData currentTab = getCurrentTab();
                if (currentTab != null && currentTab.webView != null) {
                    if (!enabled) {
                        // 如果刚启用净化模式，立即应用净化
                        String currentUrl = currentTab.webView.getUrl();
                        if (currentUrl != null) {
                            mContentPurifier.refreshPurification(currentTab.webView, currentUrl);
                        }
                    } else {
                        // 如果禁用了净化模式，刷新页面恢复原样
                        currentTab.webView.reload();
                    }
                }
                
                android.util.Log.d("WebViewActivity", "Video purification mode " + (!enabled ? "enabled" : "disabled"));
            } else {
                Toast.makeText(this, "内容净化器不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error toggling video purification mode", e);
            Toast.makeText(this, "切换视频净化模式失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开多标签管理器
     */
    private void openTabsManager() {
        try {
            Intent intent = new Intent(this, TabsManagerActivity.class);
            startActivityForResult(intent, REQUEST_CODE_TABS_MANAGER);
            android.util.Log.d("WebViewActivity", "Opening tabs manager");
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error opening tabs manager", e);
            Toast.makeText(this, "打开标签管理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 更新标签数量显示
     */
    private void updateTabsCount() {
        try {
            if (mTabsCount != null) {
                int tabCount = mTabs.size();
                mTabsCount.setText(String.valueOf(tabCount));
                mTabsCount.setVisibility(tabCount > 0 ? View.VISIBLE : View.GONE);
                android.util.Log.d("WebViewActivity", "Updated tabs count: " + tabCount);
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error updating tabs count", e);
        }
    }
    
    /**
     * 更新无痕模式指示器
     */
    private void updateIncognitoIndicator() {
        try {
            TabData currentTab = getCurrentTab();
            boolean isIncognito = currentTab != null && currentTab.isIncognito;
            
            if (mIncognitoIcon != null && mSearchIcon != null) {
                if (isIncognito) {
                    mIncognitoIcon.setVisibility(View.VISIBLE);
                    mSearchIcon.setVisibility(View.GONE);
                    
                    // 为地址栏添加无痕模式样式
                    if (mUrlInput != null) {
                        mUrlInput.setHint("无痕浏览 - 搜索或输入网址");
                    }
                } else {
                    mIncognitoIcon.setVisibility(View.GONE);
                    mSearchIcon.setVisibility(View.VISIBLE);
                    
                    // 恢复正常地址栏样式
                    if (mUrlInput != null) {
                        mUrlInput.setHint("搜索或输入网址 (智能识别，例: youtube.com 或 'java教程')");
                    }
                }
                
                android.util.Log.d("WebViewActivity", "Updated incognito indicator: " + isIncognito);
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error updating incognito indicator", e);
        }
    }
    
    private void toggleNovelPurificationMode() {
        try {
            if (mContentPurifier != null) {
                boolean enabled = mContentPurifier.isReadingModeEnabled();
                mContentPurifier.setReadingModeEnabled(!enabled);

                String status = !enabled ? "已启用" : "已禁用";
                Toast.makeText(this, "小说净化模式" + status, Toast.LENGTH_SHORT).show();

                // 刷新当前页面以应用新设置
                TabData currentTab = getCurrentTab();
                if (currentTab != null && currentTab.webView != null) {
                    if (!enabled) {
                        // 如果刚启用净化模式，立即应用净化
                        String currentUrl = currentTab.webView.getUrl();
                        if (currentUrl != null) {
                            mContentPurifier.refreshPurification(currentTab.webView, currentUrl);
                        }
                    } else {
                        // 如果禁用了净化模式，刷新页面恢复原样
                        currentTab.webView.reload();
                    }
                }

                android.util.Log.d("WebViewActivity", "Novel purification mode " + (!enabled ? "enabled" : "disabled"));
            } else {
                Toast.makeText(this, "内容净化器不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error toggling novel purification mode", e);
            Toast.makeText(this, "切换小说净化模式失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检测当前页面的小说内容
     */
    private void detectNovelContent() {
        try {
            TabData currentTab = getCurrentTab();
            if (currentTab == null || currentTab.webView == null) {
                Toast.makeText(this, "当前页面不可用", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = currentTab.webView.getUrl();
            String title = currentTab.webView.getTitle();

            if (url == null || title == null) {
                Toast.makeText(this, "无法获取页面信息", Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示检测中提示
            Toast.makeText(this, "正在检测小说内容...", Toast.LENGTH_SHORT).show();

            // 执行JavaScript获取页面文本内容
            String script = "(function() {" +
                    "var elements = document.querySelectorAll('*');" +
                    "var text = '';" +
                    "for (var i = 0; i < elements.length; i++) {" +
                    "    var element = elements[i];" +
                    "    if (element.offsetParent !== null && " +
                    "        element.tagName.toLowerCase() !== 'script' && " +
                    "        element.tagName.toLowerCase() !== 'style' && " +
                    "        element.tagName.toLowerCase() !== 'noscript') {" +
                    "        var elementText = element.textContent || element.innerText || '';" +
                    "        if (elementText.trim().length > 10) {" +
                    "            text += elementText.trim() + '\\n';" +
                    "        }" +
                    "    }" +
                    "}" +
                    "return text.substring(0, 10000); " + // 限制长度避免内存问题
                    "})();";

            currentTab.webView.evaluateJavascript(script, result -> {
                try {
                    if (result != null && !"null".equals(result)) {
                        // 清理JavaScript结果
                        String content = result;
                        if (content.startsWith("\"") && content.endsWith("\"")) {
                            content = content.substring(1, content.length() - 1);
                        }
                        content = content.replace("\\\"", "\"");
                        content = content.replace("\\\\", "\\");
                        content = content.replace("\\n", "\n");

                        // 使用小说检测器检测内容
                        boolean isNovel = mNovelDetector.isEroNovelPage(url, title, content);

                        if (isNovel) {
                            // 检测到小说内容，显示收藏对话框
                            showNovelDetectedDialog(url, title, content);
                        } else {
                            Toast.makeText(WebViewActivity.this, "未检测到小说内容", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(WebViewActivity.this, "无法提取页面内容", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("WebViewActivity", "Error detecting novel content", e);
                    Toast.makeText(WebViewActivity.this, "检测失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in detectNovelContent", e);
            Toast.makeText(this, "检测小说内容失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示小说检测结果对话框
     */
    private void showNovelDetectedDialog(String url, String title, String content) {
        try {
            EroNovelDetector.NovelInfo novelInfo = mNovelDetector.extractNovelInfo(url, title, content);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("检测到小说内容");
            builder.setMessage("标题: " + novelInfo.title + "\n" +
                              "作者: " + novelInfo.author + "\n" +
                              "类型: " + (novelInfo.isEro ? "色情小说" : "普通小说") + "\n" +
                              "章节数: " + novelInfo.chapters.size() + "\n\n" +
                              "是否添加到小说书库?");

            builder.setPositiveButton("添加到书库", (dialog, which) -> {
                // 添加到小说书库
                long result = mNovelLibraryManager.addNovel(novelInfo);
                if (result > 0) {
                    Toast.makeText(this, "已添加到小说书库", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "添加失败，请重试", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNeutralButton("直接阅读", (dialog, which) -> {
                // 直接打开小说阅读器
                Intent intent = new Intent(this, NovelReaderActivity.class);
                intent.putExtra(NovelReaderActivity.EXTRA_NOVEL_URL, url);
                intent.putExtra(NovelReaderActivity.EXTRA_NOVEL_TITLE, title);
                intent.putExtra(NovelReaderActivity.EXTRA_NOVEL_CONTENT, content);
                startActivity(intent);
            });

            builder.setNegativeButton("取消", null);

            builder.show();

        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing novel detected dialog", e);
            Toast.makeText(this, "显示对话框失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开小说书库
     */
    private void openNovelLibrary() {
        try {
            Intent intent = new Intent(this, NovelLibraryActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error opening novel library", e);
            Toast.makeText(this, "打开小说书库失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_TABS_MANAGER && resultCode == RESULT_OK && data != null) {
            try {
                String action = data.getStringExtra(TabsManagerActivity.EXTRA_ACTION);
                int selectedTab = data.getIntExtra(TabsManagerActivity.EXTRA_SELECTED_TAB, -1);
                
                if (TabsManagerActivity.ACTION_NEW_TAB.equals(action)) {
                    // 创建新的普通标签页
                    createNewTab(false);
                    android.util.Log.d(TAG, "Created new normal tab from tabs manager");
                } else if (TabsManagerActivity.ACTION_NEW_INCOGNITO_TAB.equals(action)) {
                    // 创建新的无痕标签页
                    createNewTab(true);
                    android.util.Log.d(TAG, "Created new incognito tab from tabs manager");
                } else if (selectedTab != -1) {
                    // 切换到指定标签页
                    switchToTab(selectedTab);
                    android.util.Log.d(TAG, "Switched to tab: " + selectedTab);
                }
                
                // 更新标签数量显示
                updateTabsCount();
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error handling tabs manager result", e);
            }
        }
    }
    
    /**
     * 创建新标签页
     */
    private void createNewTab(boolean incognito) {
        try {
            // 创建新的WebView
            WebView webView = new WebView(this);
            
            // 创建增强WebView管理器
            EnhancedWebViewManager enhancedManager = new EnhancedWebViewManager(this, webView, mHistoryManager);
            
            // 创建标签页数据
            TabData newTab = new TabData(webView, enhancedManager);
            newTab.isIncognito = incognito;
            
            mTabs.add(newTab);
            
            // 设置WebView
            setupWebViewForTab(webView);
            setupEnhancedWebViewCallbacks(enhancedManager);
            
            // 为无痕模式设置特殊样式
            if (incognito) {
                setupIncognitoModeForWebView(webView);
                android.util.Log.d(TAG, "Created new incognito tab");
            } else {
                android.util.Log.d(TAG, "Created new normal tab");
            }
            
            // 切换到新标签页
            switchTab(mTabs.size() - 1);
            
            updateTabsCount();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error creating new tab", e);
        }
    }
    
    /**
     * 为WebView设置无痕模式
     */
    private void setupIncognitoModeForWebView(WebView webView) {
        try {
            WebSettings settings = webView.getSettings();
            
            // 禁用缓存（无痕模式核心特性）
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            
            // 禁用各种存储
            settings.setDatabaseEnabled(false);
            settings.setDomStorageEnabled(false);
            // setAppCacheEnabled deprecated in API 33+
            
            // 禁用地理位置缓存
            settings.setGeolocationEnabled(false);
            
            // 设置无痕模式的User-Agent（添加标识）
            String currentUA = settings.getUserAgentString();
            if (currentUA != null && !currentUA.contains("Incognito")) {
                settings.setUserAgentString(currentUA + " Incognito");
            }
            
            android.util.Log.d(TAG, "Incognito mode configured for WebView");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error setting up incognito mode", e);
        }
    }
    
    /**
     * 切换到指定标签页索引
     */
    private void switchTab(int index) {
        try {
            if (index >= 0 && index < mTabs.size()) {
                mCurrentTabIndex = index;
                TabData currentTab = mTabs.get(index);
                
                // 更新WebView容器
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.removeAllViews();
                    if (currentTab.webView != null) {
                        mSwipeRefreshLayout.addView(currentTab.webView);
                    }
                }
                
                // 更新UI状态
                updateTabsCount();
                updateIncognitoIndicator();
                updateTitle(currentTab.title);
                
                // 更新地址栏
                if (mUrlInput != null && currentTab.url != null) {
                    mUrlInput.setText(currentTab.url);
                }
                
                android.util.Log.d(TAG, "Switched to tab at index: " + index);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error switching tab", e);
        }
    }
    
    /**
     * 切换到指定标签页ID
     */
    private void switchToTabById(int tabId) {
        try {
            for (int i = 0; i < mTabs.size(); i++) {
                TabData tab = mTabs.get(i);
                if (tab != null && tab.tabId == tabId) {
                    switchToTab(i);
                    android.util.Log.d(TAG, "Switched to tab at index: " + i);
                    return;
                }
            }
            android.util.Log.w(TAG, "Tab with ID " + tabId + " not found");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error switching to tab", e);
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
            WebView webView = mBrowserCoreManager.acquireOptimizedWebView(null);
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

    /**
     * 显示视频全屏模式
     */
    private void showVideoFullscreen(View view, android.webkit.WebChromeClient.CustomViewCallback callback) {
        if (mIsVideoFullscreen) return;
        
        try {
            mIsVideoFullscreen = true;
            mCustomVideoView = view;
            
            // 获取根视图容器
            ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
            if (rootView == null) return;
            
            // 创建全屏视频容器
            if (mVideoFullscreenContainer == null) {
                mVideoFullscreenContainer = new FrameLayout(this);
                mVideoFullscreenContainer.setBackgroundColor(0xFF000000);
                mVideoFullscreenContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
                    
                // 添加退出按钮
                android.widget.ImageButton exitButton = new android.widget.ImageButton(this);
                exitButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                exitButton.setBackgroundColor(0x80000000);
                FrameLayout.LayoutParams exitParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
                exitParams.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;
                exitParams.setMargins(0, 100, 50, 0);
                exitButton.setLayoutParams(exitParams);
                exitButton.setOnClickListener(v -> {
                    hideVideoFullscreen();
                    if (callback != null) {
                        callback.onCustomViewHidden();
                    }
                });
                mVideoFullscreenContainer.addView(exitButton);
            }
            
            // 设置视频只横屏播放
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            
            // 隐藏系统UI实现沉浸式体验
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                
            // 保持屏幕常亮
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            // 将视频视图添加到全屏容器
            mVideoFullscreenContainer.addView(view, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                android.view.Gravity.CENTER));
                
            // 将全屏容器添加到根视图
            rootView.addView(mVideoFullscreenContainer);
            
            android.util.Log.d("WebViewActivity", "Video entered fullscreen mode");
            
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing video fullscreen", e);
        }
    }
    
    /**
     * 隐藏视频全屏模式
     */
    private void hideVideoFullscreen() {
        if (!mIsVideoFullscreen) return;
        
        try {
            mIsVideoFullscreen = false;
            
            // 恢复屏幕方向为自动
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            
            // 恢复系统UI
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            
            // 取消保持屏幕常亮
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            // 从根视图中移除全屏容器
            if (mVideoFullscreenContainer != null) {
                ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.removeView(mVideoFullscreenContainer);
                }
                
                // 清理全屏容器
                if (mCustomVideoView != null) {
                    mVideoFullscreenContainer.removeView(mCustomVideoView);
                }
                mVideoFullscreenContainer.removeAllViews();
            }
            
            // 清理引用
            mCustomVideoView = null;
            
            android.util.Log.d("WebViewActivity", "Video exited fullscreen mode");
            
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error hiding video fullscreen", e);
        }
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

    /**
     * 处理翻译功能的Intent请求
     */
    private boolean handleTranslationIntent(Intent intent) {
        if (intent == null) return false;
        
        String action = intent.getAction();
        if (action == null) return false;
        
        try {
            android.util.Log.d("WebViewActivity", "Handling translation intent: " + action);
            
            switch (action) {
                case Intent.ACTION_SEND:
                    // 处理分享的文本翻译
                    if ("text/plain".equals(intent.getType())) {
                        String textToTranslate = intent.getStringExtra(Intent.EXTRA_TEXT);
                        if (textToTranslate != null && !textToTranslate.trim().isEmpty()) {
                            android.util.Log.d("WebViewActivity", "Translating shared text: " + textToTranslate);
                            openTranslationPage(textToTranslate);
                            return true;
                        }
                    }
                    break;
                    
                case Intent.ACTION_PROCESS_TEXT:
                    // 处理选中文本翻译（Android 6.0+）
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        String selectedText = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
                        if (selectedText != null && !selectedText.trim().isEmpty()) {
                            android.util.Log.d("WebViewActivity", "Translating selected text: " + selectedText);
                            openTranslationPage(selectedText);
                            return true;
                        }
                    }
                    break;
                    
                case Intent.ACTION_VIEW:
                    // 处理translate:// scheme
                    Uri data = intent.getData();
                    if (data != null && "translate".equals(data.getScheme())) {
                        String text = data.getQueryParameter("text");
                        if (text != null && !text.trim().isEmpty()) {
                            android.util.Log.d("WebViewActivity", "Translating from translate scheme: " + text);
                            openTranslationPage(text);
                            return true;
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error handling translation intent", e);
        }
        
        return false;
    }

    /**
     * 打开翻译页面
     */
    private void openTranslationPage(String textToTranslate) {
        try {
            // URL编码文本
            String encodedText = java.net.URLEncoder.encode(textToTranslate, "UTF-8");
            
            // 构造翻译URL - 使用Google翻译
            String translateUrl = "https://translate.google.com/?sl=auto&tl=zh&text=" + encodedText + "&op=translate";
            
            // 创建新标签页并加载翻译页面
            android.util.Log.d("WebViewActivity", "Opening translation URL: " + translateUrl);
            
            // 直接使用createNewTab方法创建新标签页
            createNewTab(translateUrl);
            
            // 显示翻译提示
            Toast.makeText(this, "正在为您翻译文本...", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error opening translation page", e);
            Toast.makeText(this, "翻译功能暂时不可用", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 启动元素屏蔽模式
     */
    private void startElementBlockingMode() {
        try {
            if (getCurrentWebView() != null) {
                String script = "if (window.startElementSelection) { window.startElementSelection(); } else { alert('请稍后再试，功能正在加载中...'); }";
                getCurrentWebView().evaluateJavascript(script, null);
                Toast.makeText(this, "元素屏蔽模式已启动，点击选择元素，长按屏蔽", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "请先打开一个网页", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error starting element blocking mode", e);
            Toast.makeText(this, "启动元素屏蔽模式失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示屏蔽记录管理器
     */
    private void showBlockListManager() {
        try {
            AdBlockManager adBlockManager = AdBlockManager.getInstance();
            Set<String> domains = adBlockManager.getBlockedDomains();
            
            if (domains.isEmpty()) {
                Toast.makeText(this, "暂无屏蔽记录", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 创建域名列表
            String[] domainArray = domains.toArray(new String[0]);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("屏蔽记录管理 (" + domains.size() + "个域名)");
            
            builder.setItems(domainArray, (dialog, which) -> {
                String selectedDomain = domainArray[which];
                showDomainBlockDetails(selectedDomain);
            });
            
            builder.setPositiveButton("清除全部", (dialog, which) -> {
                AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
                confirmBuilder.setTitle("确认清除");
                confirmBuilder.setMessage("确定要清除所有屏蔽记录吗？");
                confirmBuilder.setPositiveButton("确定", (d2, w2) -> {
                    adBlockManager.clearAllBlockedElements();
                    Toast.makeText(this, "已清除所有屏蔽记录", Toast.LENGTH_SHORT).show();
                });
                confirmBuilder.setNegativeButton("取消", null);
                confirmBuilder.show();
            });
            
            builder.setNegativeButton("关闭", null);
            builder.show();
            
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing block list manager", e);
            Toast.makeText(this, "显示屏蔽记录失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示域名屏蔽详情
     */
    private void showDomainBlockDetails(String domain) {
        try {
            AdBlockManager adBlockManager = AdBlockManager.getInstance();
            Set<String> selectors = adBlockManager.getBlockedElements(domain);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(domain + " (已屏蔽" + selectors.size() + "个元素)");
            
            if (selectors.isEmpty()) {
                builder.setMessage("该域名没有屏蔽元素");
            } else {
                StringBuilder sb = new StringBuilder();
                int i = 1;
                for (String selector : selectors) {
                    sb.append(i++).append(". ").append(selector).append("\n");
                }
                builder.setMessage(sb.toString());
            }
            
            builder.setPositiveButton("清除该域名", (dialog, which) -> {
                adBlockManager.clearBlockedElements(domain);
                Toast.makeText(this, "已清除 " + domain + " 的屏蔽记录", Toast.LENGTH_SHORT).show();
            });
            
            builder.setNegativeButton("关闭", null);
            builder.show();
            
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error showing domain block details", e);
            Toast.makeText(this, "显示详情失败", Toast.LENGTH_SHORT).show();
        }
    }
}
