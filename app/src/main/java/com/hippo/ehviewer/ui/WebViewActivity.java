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
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.hippo.ehviewer.R;
import com.hippo.util.ExceptionUtils;

/**
 * 独立的WebView浏览器Activity
 */
public class WebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "url";

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

    private WebView mWebView;
    private ProgressBar mProgressBar;
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

            mWebView = findViewById(R.id.web_view);
            mProgressBar = findViewById(R.id.progress_bar);
            mToolbar = findViewById(R.id.toolbar);
            mUrlInput = findViewById(R.id.url_input);
            mRefreshButton = findViewById(R.id.refresh_button);
            mBackButton = findViewById(R.id.back_button);
            mForwardButton = findViewById(R.id.forward_button);
            mHomeButton = findViewById(R.id.home_button);
            mTabsButton = findViewById(R.id.tabs_button);
            mMenuButton = findViewById(R.id.menu_button);

            // 设置Toolbar
            if (mToolbar != null) {
                setSupportActionBar(mToolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(R.string.browser);
                }
                mToolbar.setNavigationOnClickListener(v -> finish());
            }

            // 设置地址栏
            if (mUrlInput != null) {
                mUrlInput.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                        String url = mUrlInput.getText().toString().trim();
                        if (!url.isEmpty()) {
                            loadUrl(url);
                        }
                        return true;
                    }
                    return false;
                });

                mUrlInput.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        mUrlInput.selectAll();
                    }
                });
            }

            // 设置按钮监听器
            if (mRefreshButton != null) {
                mRefreshButton.setOnClickListener(v -> {
                    if (mWebView != null) {
                        mWebView.reload();
                    }
                });
            }

            if (mBackButton != null) {
                mBackButton.setOnClickListener(v -> {
                    if (mWebView != null && mWebView.canGoBack()) {
                        mWebView.goBack();
                    }
                });
            }

            if (mForwardButton != null) {
                mForwardButton.setOnClickListener(v -> {
                    if (mWebView != null && mWebView.canGoForward()) {
                        mWebView.goForward();
                    }
                });
            }

            if (mHomeButton != null) {
                mHomeButton.setOnClickListener(v -> {
                    if (mWebView != null) {
                        mWebView.loadUrl("https://www.google.com");
                    }
                });
            }

            if (mTabsButton != null) {
                mTabsButton.setOnClickListener(v -> {
                    showTabsDialog();
                });
            }

            if (mMenuButton != null) {
                mMenuButton.setOnClickListener(v -> {
                    showMenuDialog();
                });
            }

            String url = getIntent().getStringExtra(EXTRA_URL);
            android.util.Log.d("WebViewActivity", "URL to load: " + url);

            if (!TextUtils.isEmpty(url)) {
                setupWebView();
                mWebView.loadUrl(url);
            } else {
                android.util.Log.e("WebViewActivity", "URL is empty!");
                finish();
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
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
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
            if (mWebView != null) {
                mWebView.stopLoading();
                mWebView.setWebViewClient(null);
                mWebView.setWebChromeClient(null);
                mWebView.destroy();
                mWebView = null;
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewActivity", "Error in onDestroy", e);
            ExceptionUtils.throwIfFatal(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
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
                // 认为是搜索关键词，使用Google搜索
                url = "https://www.google.com/search?q=" + android.net.Uri.encode(url);
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
            } else if (itemId == R.id.menu_bookmarks) {
                // 书签
                android.widget.Toast.makeText(this, "书签功能开发中", android.widget.Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_history) {
                // 历史记录
                android.widget.Toast.makeText(this, "历史记录功能开发中", android.widget.Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_settings) {
                // 设置
                android.widget.Toast.makeText(this, "浏览器设置功能开发中", android.widget.Toast.LENGTH_SHORT).show();
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
            }
            return false;
        });

        popupMenu.show();
    }
}
