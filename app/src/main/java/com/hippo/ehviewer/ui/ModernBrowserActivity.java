package com.hippo.ehviewer.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.offline.OfflineCacheManager;
import com.hippo.ehviewer.ui.browser.SmartAddressBar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 现代化浏览器主界面
 * 参考Chrome、Edge等主流浏览器设计
 * 提供完整的浏览器功能和优秀的用户体验
 */
public class ModernBrowserActivity extends AppCompatActivity {
    
    // UI组件
    private CoordinatorLayout mainLayout;
    private LinearLayout topToolbar;
    private CardView addressBarCard;
    private EditText addressBar;
    private ImageButton btnBack;
    private ImageButton btnForward;
    private ImageButton btnHome;
    private ImageButton btnRefresh;
    private ImageButton btnMenu;
    private ImageButton btnTabs;
    private TextView tabCounter;
    private ProgressBar progressBar;
    private FrameLayout webViewContainer;
    private WebView currentWebView;
    private View bottomToolbar;
    private RecyclerView suggestionRecycler;
    private LinearLayout searchSuggestionPanel;
    
    // 标签页管理
    private List<BrowserTab> tabs = new ArrayList<>();
    private int currentTabIndex = 0;
    private static final int MAX_TABS = 99;
    
    // 浏览历史
    private Stack<String> historyStack = new Stack<>();
    private Stack<String> forwardStack = new Stack<>();
    
    // 设置和状态
    private boolean isIncognitoMode = false;
    private boolean isDesktopMode = false;
    private boolean isNightMode = false;
    private boolean isFullscreen = false;
    private boolean isAddressBarFocused = false;
    
    // 手势和动画
    private float startY;
    private boolean isToolbarHidden = false;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    
    // 智能地址栏和离线缓存
    private SmartAddressBar smartAddressBar;
    private OfflineCacheManager cacheManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modern_browser);
        
        initializeViews();
        initializeManagers();
        setupToolbars();
        setupWebView();
        setupGestures();
        loadHomePage();
        
        // 处理外部Intent
        handleIntent(getIntent());
    }
    
    /**
     * 初始化管理器
     */
    private void initializeManagers() {
        smartAddressBar = new SmartAddressBar(this);
        cacheManager = OfflineCacheManager.getInstance(this);
    }
    
    /**
     * 初始化视图组件
     */
    private void initializeViews() {
        mainLayout = findViewById(R.id.main_layout);
        topToolbar = findViewById(R.id.top_toolbar);
        addressBarCard = findViewById(R.id.address_bar_card);
        addressBar = findViewById(R.id.address_bar);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnHome = findViewById(R.id.btn_home);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnMenu = findViewById(R.id.btn_menu);
        btnTabs = findViewById(R.id.btn_tabs);
        tabCounter = findViewById(R.id.tab_counter);
        progressBar = findViewById(R.id.progress_bar);
        webViewContainer = findViewById(R.id.webview_container);
        bottomToolbar = findViewById(R.id.bottom_toolbar);
        suggestionRecycler = findViewById(R.id.suggestion_recycler);
        searchSuggestionPanel = findViewById(R.id.search_suggestion_panel);
    }
    
    /**
     * 设置工具栏
     */
    private void setupToolbars() {
        // 返回按钮
        btnBack.setOnClickListener(v -> navigateBack());
        btnBack.setOnLongClickListener(v -> {
            showHistoryMenu();
            return true;
        });
        
        // 前进按钮
        btnForward.setOnClickListener(v -> navigateForward());
        
        // 主页按钮
        btnHome.setOnClickListener(v -> loadHomePage());
        
        // 刷新按钮
        btnRefresh.setOnClickListener(v -> {
            if (currentWebView != null) {
                currentWebView.reload();
            }
        });
        
        // 菜单按钮
        btnMenu.setOnClickListener(v -> showMainMenu());
        
        // 标签页按钮
        btnTabs.setOnClickListener(v -> showTabSwitcher());
        
        // 地址栏
        setupAddressBar();
        
        // 底部工具栏按钮
        setupBottomToolbar();
    }
    
    /**
     * 设置地址栏
     */
    private void setupAddressBar() {
        // 点击地址栏时的行为
        addressBar.setOnFocusChangeListener((v, hasFocus) -> {
            isAddressBarFocused = hasFocus;
            if (hasFocus) {
                showAddressBarSuggestions();
                addressBar.selectAll();
            } else {
                hideAddressBarSuggestions();
            }
        });
        
        // 输入监听
        addressBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isAddressBarFocused) {
                    updateSearchSuggestions(s.toString());
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 回车键监听
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String url = addressBar.getText().toString();
                loadUrl(url);
                hideKeyboard();
                addressBar.clearFocus();
                return true;
            }
            return false;
        });
        
        // 安全图标
        updateSecurityIcon();
    }
    
    /**
     * 设置底部工具栏
     */
    private void setupBottomToolbar() {
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> toggleBookmark());
        findViewById(R.id.btn_share).setOnClickListener(v -> shareCurrentPage());
        findViewById(R.id.btn_find).setOnClickListener(v -> showFindInPage());
        findViewById(R.id.btn_desktop).setOnClickListener(v -> toggleDesktopMode());
        findViewById(R.id.btn_settings).setOnClickListener(v -> openSettings());
    }
    
    /**
     * 设置WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        // 创建第一个标签页
        BrowserTab firstTab = createNewTab();
        tabs.add(firstTab);
        currentWebView = firstTab.webView;
        webViewContainer.addView(currentWebView);
        
        updateTabCounter();
        
        // 加载默认主页
        if (isNetworkAvailable()) {
            currentWebView.loadUrl("https://www.baidu.com");
        } else {
            showErrorPage(-1, "无网络连接");
        }
    }
    
    /**
     * 检查网络连接
     */
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
            getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
    
    /**
     * 创建新标签页
     */
    private BrowserTab createNewTab() {
        BrowserTab tab = new BrowserTab();
        tab.webView = new WebView(this);
        
        WebSettings settings = tab.webView.getSettings();
        // 基础设置
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        // 网络设置 - 重要！
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBlockNetworkImage(false);
        settings.setBlockNetworkLoads(false);
        settings.setLoadsImagesAutomatically(true);
        
        // 设置User-Agent
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " EhViewer/1.0");
        
        // 缓存设置
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        
        // 混合内容
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // 允许JavaScript打开窗口
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        
        // 隐私模式设置
        if (isIncognitoMode) {
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setDatabaseEnabled(false);
            settings.setDomStorageEnabled(false);
        }
        
        // WebViewClient
        tab.webView.setWebViewClient(new ModernWebViewClient());
        
        // WebChromeClient
        tab.webView.setWebChromeClient(new ModernWebChromeClient());
        
        return tab;
    }
    
    /**
     * 自定义WebViewClient
     */
    private class ModernWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            updateAddressBar(url);
            updateNavigationButtons();
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            updateCurrentTab(view.getTitle(), url);
            addToHistory(url);
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            
            // 处理特殊协议
            if (url.startsWith("http://") || url.startsWith("https://")) {
                view.loadUrl(url);
                return false;
            } else if (url.startsWith("intent://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            
            return false;
        }
        
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            showErrorPage(errorCode, description);
        }
        
        @Override
        public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
            // 对于开发环境，暂时接受所有SSL证书
            // 生产环境应该提示用户
            new AlertDialog.Builder(ModernBrowserActivity.this)
                .setTitle("SSL证书错误")
                .setMessage("该网站的安全证书存在问题。是否继续访问？")
                .setPositiveButton("继续", (dialog, which) -> handler.proceed())
                .setNegativeButton("取消", (dialog, which) -> handler.cancel())
                .show();
        }
    }
    
    /**
     * 自定义WebChromeClient
     */
    private class ModernWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
            
            if (newProgress == 100) {
                uiHandler.postDelayed(() -> progressBar.setVisibility(View.GONE), 500);
            }
        }
        
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            updateCurrentTab(title, view.getUrl());
        }
        
        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            updateCurrentTabIcon(icon);
        }
    }
    
    /**
     * 显示主菜单
     */
    private void showMainMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View menuView = LayoutInflater.from(this).inflate(R.layout.browser_main_menu, null);
        
        // 新标签页
        menuView.findViewById(R.id.menu_new_tab).setOnClickListener(v -> {
            createNewTabAndSwitch();
            dialog.dismiss();
        });
        
        // 新隐私标签页
        menuView.findViewById(R.id.menu_new_incognito_tab).setOnClickListener(v -> {
            createNewIncognitoTab();
            dialog.dismiss();
        });
        
        // 书签
        menuView.findViewById(R.id.menu_bookmarks).setOnClickListener(v -> {
            showBookmarks();
            dialog.dismiss();
        });
        
        // 历史记录
        menuView.findViewById(R.id.menu_history).setOnClickListener(v -> {
            showHistory();
            dialog.dismiss();
        });
        
        // 下载
        menuView.findViewById(R.id.menu_downloads).setOnClickListener(v -> {
            showDownloads();
            dialog.dismiss();
        });
        
        // 离线缓存管理
        menuView.findViewById(R.id.menu_offline_cache).setOnClickListener(v -> {
            openCacheManagement();
            dialog.dismiss();
        });
        
        // 保存页面为离线
        menuView.findViewById(R.id.menu_save_offline).setOnClickListener(v -> {
            savePageOffline();
            dialog.dismiss();
        });
        
        // 切换搜索引擎
        menuView.findViewById(R.id.menu_search_engine).setOnClickListener(v -> {
            showSearchEngineSelector();
            dialog.dismiss();
        });
        
        // 查找
        menuView.findViewById(R.id.menu_find).setOnClickListener(v -> {
            showFindInPage();
            dialog.dismiss();
        });
        
        // 桌面版网站
        View desktopItem = menuView.findViewById(R.id.menu_desktop_site);
        TextView desktopText = desktopItem.findViewById(R.id.menu_text);
        if (isDesktopMode) {
            desktopText.setText("移动版网站");
        }
        desktopItem.setOnClickListener(v -> {
            toggleDesktopMode();
            dialog.dismiss();
        });
        
        // 设置
        menuView.findViewById(R.id.menu_settings).setOnClickListener(v -> {
            openSettings();
            dialog.dismiss();
        });
        
        // 退出
        menuView.findViewById(R.id.menu_exit).setOnClickListener(v -> {
            finish();
            dialog.dismiss();
        });
        
        dialog.setContentView(menuView);
        dialog.show();
    }
    
    /**
     * 显示标签页切换器
     */
    private void showTabSwitcher() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View tabView = LayoutInflater.from(this).inflate(R.layout.browser_tab_switcher, null);
        
        RecyclerView tabRecycler = tabView.findViewById(R.id.tab_recycler);
        tabRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        
        TabAdapter adapter = new TabAdapter(tabs, position -> {
            switchToTab(position);
            dialog.dismiss();
        });
        tabRecycler.setAdapter(adapter);
        
        // 新建标签页按钮
        FloatingActionButton fabNewTab = tabView.findViewById(R.id.fab_new_tab);
        fabNewTab.setOnClickListener(v -> {
            createNewTabAndSwitch();
            dialog.dismiss();
        });
        
        dialog.setContentView(tabView);
        dialog.show();
    }
    
    /**
     * 创建新标签页并切换
     */
    private void createNewTabAndSwitch() {
        if (tabs.size() >= MAX_TABS) {
            Toast.makeText(this, "标签页数量已达上限", Toast.LENGTH_SHORT).show();
            return;
        }
        
        BrowserTab newTab = createNewTab();
        tabs.add(newTab);
        switchToTab(tabs.size() - 1);
        loadHomePage();
    }
    
    /**
     * 切换标签页
     */
    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        
        // 移除当前WebView
        webViewContainer.removeAllViews();
        
        // 添加新的WebView
        currentTabIndex = index;
        BrowserTab tab = tabs.get(index);
        currentWebView = tab.webView;
        webViewContainer.addView(currentWebView);
        
        // 更新UI
        updateAddressBar(tab.url);
        updateNavigationButtons();
        updateTabCounter();
    }
    
    /**
     * 更新标签页计数器
     */
    private void updateTabCounter() {
        int count = tabs.size();
        if (count > 99) {
            tabCounter.setText(":D");
        } else {
            tabCounter.setText(String.valueOf(count));
        }
        
        // 更新标签按钮样式
        if (isIncognitoMode) {
            btnTabs.setColorFilter(ContextCompat.getColor(this, R.color.incognito_accent));
        }
    }
    
    /**
     * 加载URL
     */
    private void loadUrl(String url) {
        if (url == null || url.isEmpty()) return;
        
        // 使用智能地址栏分析URL
        SmartAddressBar.InputAnalysis analysis = smartAddressBar.analyzeInput(url);
        String finalUrl = analysis.processedUrl;
        
        // 如果有高置信度，直接使用分析结果
        if (analysis.confidence > 0.7f) {
            finalUrl = analysis.processedUrl;
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // 低置信度时的后备处理
            if (url.contains(".") && !url.contains(" ")) {
                finalUrl = "https://" + url;
            } else {
                // 使用当前搜索引擎
                finalUrl = smartAddressBar.getSearchEngine().getSearchUrl(url);
            }
        }
        
        if (currentWebView != null) {
            // 检查是否可以使用离线缓存
            if (!isNetworkAvailable() && cacheManager.isFileCached(finalUrl, OfflineCacheManager.CacheType.TEMP)) {
                loadFromCache(finalUrl);
            } else {
                currentWebView.loadUrl(finalUrl);
            }
        }
    }
    
    /**
     * 从缓存加载内容
     */
    private void loadFromCache(String url) {
        File cachedFile = cacheManager.getCachedFile(url, OfflineCacheManager.CacheType.TEMP);
        if (cachedFile != null && cachedFile.exists() && currentWebView != null) {
            // 读取缓存文件内容
            try {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(cachedFile));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                currentWebView.loadDataWithBaseURL(url, content.toString(), "text/html", "UTF-8", null);
                Snackbar.make(mainLayout, "正在使用离线缓存", Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                currentWebView.loadUrl(url);
            }
        }
    }
    
    /**
     * 加载主页
     */
    private void loadHomePage() {
        String homePage = "https://www.google.com";
        loadUrl(homePage);
    }
    
    /**
     * 导航后退
     */
    private void navigateBack() {
        if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack();
        }
    }
    
    /**
     * 导航前进
     */
    private void navigateForward() {
        if (currentWebView != null && currentWebView.canGoForward()) {
            currentWebView.goForward();
        }
    }
    
    /**
     * 更新导航按钮状态
     */
    private void updateNavigationButtons() {
        if (currentWebView != null) {
            btnBack.setEnabled(currentWebView.canGoBack());
            btnForward.setEnabled(currentWebView.canGoForward());
            btnBack.setAlpha(currentWebView.canGoBack() ? 1.0f : 0.5f);
            btnForward.setAlpha(currentWebView.canGoForward() ? 1.0f : 0.5f);
        }
    }
    
    /**
     * 更新地址栏
     */
    private void updateAddressBar(String url) {
        if (!isAddressBarFocused && url != null) {
            addressBar.setText(url);
        }
    }
    
    /**
     * 更新安全图标
     */
    private void updateSecurityIcon() {
        // 根据HTTPS状态更新图标
        if (currentWebView != null) {
            String url = currentWebView.getUrl();
            if (url != null && url.startsWith("https://")) {
                addressBar.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock, 0, 0, 0);
            } else {
                addressBar.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_info, 0, 0, 0);
            }
        }
    }
    
    /**
     * 显示地址栏建议
     */
    private void showAddressBarSuggestions() {
        searchSuggestionPanel.setVisibility(View.VISIBLE);
        // 动画效果
        searchSuggestionPanel.setAlpha(0f);
        searchSuggestionPanel.animate()
            .alpha(1f)
            .setDuration(200)
            .start();
    }
    
    /**
     * 隐藏地址栏建议
     */
    private void hideAddressBarSuggestions() {
        searchSuggestionPanel.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction(() -> searchSuggestionPanel.setVisibility(View.GONE))
            .start();
    }
    
    /**
     * 更新搜索建议
     */
    private void updateSearchSuggestions(String query) {
        // 使用智能地址栏分析用户输入
        SmartAddressBar.InputAnalysis analysis = smartAddressBar.analyzeInput(query);
        
        // 根据分析结果显示建议
        if (analysis.suggestions != null && !analysis.suggestions.isEmpty()) {
            // 更新建议列表UI
            updateSuggestionList(analysis);
        }
    }
    
    /**
     * 更新建议列表UI
     */
    private void updateSuggestionList(SmartAddressBar.InputAnalysis analysis) {
        // 创建建议适配器并显示
        SuggestionAdapter adapter = new SuggestionAdapter(analysis.suggestions, suggestion -> {
            loadUrl(suggestion.url);
            hideKeyboard();
            addressBar.clearFocus();
            hideAddressBarSuggestions();
        });
        
        suggestionRecycler.setAdapter(adapter);
        
        // 根据输入类型显示提示
        if (analysis.type == SmartAddressBar.InputType.SEARCH_QUERY) {
            // 显示搜索图标和提示
        } else if (analysis.type == SmartAddressBar.InputType.URL || 
                   analysis.type == SmartAddressBar.InputType.DOMAIN) {
            // 显示网址图标
        }
    }
    
    /**
     * 设置手势
     */
    private void setupGestures() {
        // 实现工具栏自动隐藏等手势
    }
    
    /**
     * 处理外部Intent
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                loadUrl(data.toString());
            }
        }
    }
    
    /**
     * 显示错误页面
     */
    private void showErrorPage(int errorCode, String description) {
        String errorHtml = "<html><head>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<style>" +
            "body { font-family: sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }" +
            "h1 { color: #333; font-size: 24px; }" +
            "p { color: #666; font-size: 16px; margin: 20px 0; }" +
            ".error-code { color: #999; font-size: 14px; }" +
            ".btn { display: inline-block; padding: 12px 24px; background: #FF6B35; color: white; " +
            "text-decoration: none; border-radius: 4px; margin: 10px; }" +
            "</style></head><body>" +
            "<h1>⚠️ 无法访问此网站</h1>" +
            "<p>" + description + "</p>" +
            "<p class='error-code'>错误代码: " + errorCode + "</p>" +
            "<a href='javascript:location.reload()' class='btn'>重试</a>" +
            "<a href='javascript:history.back()' class='btn'>返回</a>" +
            "</body></html>";
            
        if (currentWebView != null) {
            currentWebView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
        }
    }
    
    /**
     * 更新当前标签页信息
     */
    private void updateCurrentTab(String title, String url) {
        if (currentTabIndex < tabs.size()) {
            BrowserTab tab = tabs.get(currentTabIndex);
            tab.title = title;
            tab.url = url;
        }
    }
    
    /**
     * 更新当前标签页图标
     */
    private void updateCurrentTabIcon(Bitmap icon) {
        if (currentTabIndex < tabs.size()) {
            tabs.get(currentTabIndex).icon = icon;
        }
    }
    
    /**
     * 添加到历史记录
     */
    private void addToHistory(String url) {
        if (!isIncognitoMode && url != null && !url.isEmpty()) {
            // 保存到历史记录数据库
        }
    }
    
    /**
     * 切换书签状态
     */
    private void toggleBookmark() {
        // 实现书签功能
        Toast.makeText(this, "已添加到书签", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 分享当前页面
     */
    private void shareCurrentPage() {
        if (currentWebView != null) {
            String url = currentWebView.getUrl();
            String title = currentWebView.getTitle();
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, title + "\n" + url);
            startActivity(Intent.createChooser(shareIntent, "分享"));
        }
    }
    
    /**
     * 显示页内查找
     */
    private void showFindInPage() {
        // 实现页内查找功能
    }
    
    /**
     * 切换桌面模式
     */
    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        if (currentWebView != null) {
            WebSettings settings = currentWebView.getSettings();
            if (isDesktopMode) {
                settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            } else {
                settings.setUserAgentString(null);
            }
            currentWebView.reload();
        }
        
        Toast.makeText(this, isDesktopMode ? "已切换到桌面版" : "已切换到移动版", 
            Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 打开设置
     */
    private void openSettings() {
        Intent intent = new Intent(this, BrowserSettingsActivity.class);
        startActivity(intent);
    }
    
    /**
     * 显示历史菜单
     */
    private void showHistoryMenu() {
        // 长按返回按钮显示历史记录
    }
    
    /**
     * 显示书签
     */
    private void showBookmarks() {
        Intent intent = new Intent(this, BookmarksActivity.class);
        startActivity(intent);
    }
    
    /**
     * 显示历史记录
     */
    private void showHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }
    
    /**
     * 显示下载
     */
    private void showDownloads() {
        Intent intent = new Intent(this, DownloadManagerActivity.class);
        startActivity(intent);
    }
    
    /**
     * 创建隐私标签页
     */
    private void createNewIncognitoTab() {
        isIncognitoMode = true;
        createNewTabAndSwitch();
        
        // 更新UI为隐私模式
        topToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.incognito_toolbar));
        Snackbar.make(mainLayout, "您正在使用隐私浏览模式", Snackbar.LENGTH_SHORT).show();
    }
    
    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && addressBar != null) {
            imm.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
        }
    }
    
    /**
     * 打开缓存管理
     */
    private void openCacheManagement() {
        Intent intent = new Intent(this, CacheManagementActivity.class);
        startActivity(intent);
    }
    
    /**
     * 保存页面为离线
     */
    private void savePageOffline() {
        if (currentWebView != null) {
            String url = currentWebView.getUrl();
            String title = currentWebView.getTitle();
            
            // 确定缓存类型
            OfflineCacheManager.CacheType type = OfflineCacheManager.CacheType.TEMP;
            if (url != null) {
                if (url.contains("image") || url.contains("jpg") || url.contains("png")) {
                    type = OfflineCacheManager.CacheType.IMAGE;
                } else if (url.contains("video") || url.contains("mp4")) {
                    type = OfflineCacheManager.CacheType.VIDEO;
                } else if (url.contains("novel") || url.contains("book")) {
                    type = OfflineCacheManager.CacheType.NOVEL;
                } else if (url.contains("manga") || url.contains("comic")) {
                    type = OfflineCacheManager.CacheType.MANGA;
                }
            }
            
            // 根据类型调用不同的缓存方法
            if (type == OfflineCacheManager.CacheType.IMAGE) {
                cacheManager.cacheImage(url, new OfflineCacheManager.CacheCallback() {
                    @Override
                    public void onCached(java.io.File file) {
                        runOnUiThread(() -> {
                            Snackbar.make(mainLayout, "页面已保存到离线缓存", Snackbar.LENGTH_SHORT)
                                .setAction("查看", v -> openCacheManagement())
                                .show();
                        });
                    }
                    
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Snackbar.make(mainLayout, "保存失败: " + message, Snackbar.LENGTH_SHORT).show();
                        });
                    }
                });
            } else if (type == OfflineCacheManager.CacheType.VIDEO) {
                cacheManager.cacheVideo(url, new OfflineCacheManager.CacheCallback() {
                    @Override
                    public void onCached(java.io.File file) {
                        runOnUiThread(() -> {
                            Snackbar.make(mainLayout, "视频已保存到离线缓存", Snackbar.LENGTH_SHORT)
                                .setAction("查看", v -> openCacheManagement())
                                .show();
                        });
                    }
                    
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Snackbar.make(mainLayout, "保存失败: " + message, Snackbar.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                // 其他类型使用图片缓存方法
                cacheManager.cacheImage(url, new OfflineCacheManager.CacheCallback() {
                    @Override
                    public void onCached(java.io.File file) {
                        runOnUiThread(() -> {
                            Snackbar.make(mainLayout, "页面已保存到离线缓存", Snackbar.LENGTH_SHORT)
                                .setAction("查看", v -> openCacheManagement())
                                .show();
                        });
                    }
                    
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Snackbar.make(mainLayout, "保存失败: " + message, Snackbar.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            Toast.makeText(this, "正在保存页面...", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示搜索引擎选择器
     */
    private void showSearchEngineSelector() {
        String[] engines = {"百度", "谷歌", "必应", "搜狗", "DuckDuckGo"};
        SmartAddressBar.SearchEngine[] engineValues = SmartAddressBar.SearchEngine.values();
        
        new AlertDialog.Builder(this)
            .setTitle("选择默认搜索引擎")
            .setSingleChoiceItems(engines, smartAddressBar.getSearchEngine().ordinal(), (dialog, which) -> {
                smartAddressBar.setSearchEngine(engineValues[which]);
                dialog.dismiss();
                Toast.makeText(this, "已切换到" + engines[which], Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 浏览器标签页数据类
     */
    private static class BrowserTab {
        WebView webView;
        String title = "新标签页";
        String url = "";
        Bitmap icon;
        boolean isIncognito = false;
    }
    
    /**
     * 搜索建议适配器
     */
    private class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {
        private List<SmartAddressBar.Suggestion> suggestions;
        private OnSuggestionClickListener listener;
        
        public SuggestionAdapter(List<SmartAddressBar.Suggestion> suggestions, OnSuggestionClickListener listener) {
            this.suggestions = suggestions;
            this.listener = listener;
        }
        
        @Override
        public SuggestionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
            return new SuggestionViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(SuggestionViewHolder holder, int position) {
            SmartAddressBar.Suggestion suggestion = suggestions.get(position);
            holder.title.setText(suggestion.title);
            holder.subtitle.setText(suggestion.subtitle);
            
            // 根据类型设置图标
            switch (suggestion.type) {
                case URL_COMPLETION:
                    holder.icon.setImageResource(R.drawable.ic_link);
                    break;
                case SEARCH_SUGGESTION:
                    holder.icon.setImageResource(R.drawable.ic_search);
                    break;
                case BOOKMARK:
                    holder.icon.setImageResource(R.drawable.ic_bookmark);
                    break;
                case HISTORY:
                    holder.icon.setImageResource(R.drawable.ic_history);
                    break;
                case QUICK_ACCESS:
                    holder.icon.setImageResource(R.drawable.ic_star);
                    break;
            }
            
            holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(suggestion));
        }
        
        @Override
        public int getItemCount() {
            return suggestions.size();
        }
        
        class SuggestionViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            TextView subtitle;
            
            SuggestionViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.suggestion_icon);
                title = itemView.findViewById(R.id.suggestion_title);
                subtitle = itemView.findViewById(R.id.suggestion_subtitle);
            }
        }
    }
    
    private interface OnSuggestionClickListener {
        void onSuggestionClick(SmartAddressBar.Suggestion suggestion);
    }
    
    /**
     * 标签页适配器
     */
    private class TabAdapter extends RecyclerView.Adapter<TabAdapter.TabViewHolder> {
        private List<BrowserTab> tabs;
        private OnTabClickListener listener;
        
        public TabAdapter(List<BrowserTab> tabs, OnTabClickListener listener) {
            this.tabs = tabs;
            this.listener = listener;
        }
        
        @Override
        public TabViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_browser_tab, parent, false);
            return new TabViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(TabViewHolder holder, int position) {
            BrowserTab tab = tabs.get(position);
            holder.title.setText(tab.title);
            holder.url.setText(tab.url);
            
            if (tab.icon != null) {
                holder.icon.setImageBitmap(tab.icon);
            }
            
            holder.itemView.setOnClickListener(v -> listener.onTabClick(position));
            
            holder.closeButton.setOnClickListener(v -> {
                if (tabs.size() > 1) {
                    tabs.remove(position);
                    notifyItemRemoved(position);
                    updateTabCounter();
                } else {
                    // 最后一个标签页，创建新的
                    createNewTabAndSwitch();
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return tabs.size();
        }
        
        class TabViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            TextView url;
            ImageButton closeButton;
            
            TabViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.tab_icon);
                title = itemView.findViewById(R.id.tab_title);
                url = itemView.findViewById(R.id.tab_url);
                closeButton = itemView.findViewById(R.id.tab_close);
            }
        }
    }
    
    private interface OnTabClickListener {
        void onTabClick(int position);
    }
    
    @Override
    public void onBackPressed() {
        if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (currentWebView != null) {
            currentWebView.onPause();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (currentWebView != null) {
            currentWebView.onResume();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (BrowserTab tab : tabs) {
            if (tab.webView != null) {
                tab.webView.destroy();
            }
        }
    }
}