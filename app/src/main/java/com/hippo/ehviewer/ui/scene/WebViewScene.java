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

package com.hippo.ehviewer.ui.scene;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.R;
import com.hippo.scene.SceneFragment;
import com.hippo.util.ExceptionUtils;

/**
 * 内置WebView浏览器
 */
public class WebViewScene extends BaseScene {

    public static final String TAG = WebViewScene.class.getSimpleName();

    public static final String KEY_URL = "url";

    private String mUrl;
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private androidx.appcompat.widget.Toolbar mToolbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.util.Log.d("WebViewScene", "onCreate called");

        try {
            if (savedInstanceState == null) {
                onInit();
            } else {
                onRestore(savedInstanceState);
            }
        } catch (Exception e) {
            android.util.Log.e("WebViewScene", "Error in onCreate", e);
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    private void handleArgs(Bundle args) {
        if (args == null) {
            return;
        }

        mUrl = args.getString(KEY_URL);
    }

    private void onInit() {
        android.util.Log.d("WebViewScene", "onInit called");
        handleArgs(getArguments());
        android.util.Log.d("WebViewScene", "URL to load: " + mUrl);
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        android.util.Log.d("WebViewScene", "onRestore called");
        mUrl = savedInstanceState.getString(KEY_URL);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_URL, mUrl);
    }

    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater,
                              @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.util.Log.d("WebViewScene", "onCreateView2 called");
        View view = inflater.inflate(R.layout.scene_web_view, container, false);

        mWebView = view.findViewById(R.id.web_view);
        mProgressBar = view.findViewById(R.id.progress_bar);
        mToolbar = view.findViewById(R.id.toolbar);

        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
            mToolbar.setNavigationOnClickListener(v -> onNavigationClick(v));
            mToolbar.setTitle(R.string.browser);
        }

        if (mWebView != null) {
            setupWebView();

            if (!TextUtils.isEmpty(mUrl)) {
                try {
                    mWebView.loadUrl(mUrl);
                } catch (Exception e) {
                    ExceptionUtils.throwIfFatal(e);
                    // 如果加载失败，显示错误提示
                    showTip("Failed to load URL", LENGTH_LONG);
                    finish();
                }
            }
        }

        return view;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        android.util.Log.d("WebViewScene", "setupWebView called");
        if (mWebView == null) {
            android.util.Log.e("WebViewScene", "WebView is null!");
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
                setTitleInternal(R.string.loading);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.GONE);
                }
                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    setTitleInternal(title);
                } else {
                    setTitleInternal(R.string.browser);
                }
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
                if (title != null && !title.isEmpty()) {
                    setTitleInternal(title);
                }
            }
        });

        // 设置下载监听器
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                      String mimetype, long contentLength) {
                try {
                    Context context = getContext();
                    if (context == null) return;

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
                    DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    if (downloadManager != null) {
                        downloadManager.enqueue(request);
                        showTip(R.string.download_started, LENGTH_SHORT);
                    }

                } catch (Exception e) {
                    ExceptionUtils.throwIfFatal(e);
                    showTip(R.string.download_failed, LENGTH_SHORT);
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
            ExceptionUtils.throwIfFatal(e);
            showTip("WebView initialization failed", LENGTH_LONG);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.setWebViewClient(null);
            mWebView.setWebChromeClient(null);
            mWebView.destroy();
            mWebView = null;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Toolbar已经在onCreateView2中初始化了
    }

    private void setTitleInternal(CharSequence title) {
        if (mToolbar != null) {
            mToolbar.setTitle(title);
        }
    }

    private void setTitleInternal(int resId) {
        if (mToolbar != null) {
            mToolbar.setTitle(resId);
        }
    }

    public void onNavigationClick(View view) {
        try {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                onBackPressed();
            }
        } catch (Exception e) {
            ExceptionUtils.throwIfFatal(e);
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                finish();
            }
        } catch (Exception e) {
            ExceptionUtils.throwIfFatal(e);
            finish();
        }
    }

    // 处理返回键
    public boolean handleBackPressed() {
        try {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
                return true;
            }
        } catch (Exception e) {
            ExceptionUtils.throwIfFatal(e);
        }
        return false;
    }
}
