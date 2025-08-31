/*
 * EhViewer Browser Module - EhBrowser
 * EhViewer浏览器 - 内置Web浏览器功能
 */

package com.hippo.ehviewer.browser;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * EhViewer浏览器
 * 封装WebView，提供便捷的浏览器功能
 */
public class EhBrowser {

    private final Context mContext;
    private final WebView mWebView;

    public EhBrowser(Context context) {
        mContext = context;
        mWebView = new WebView(context);
        initWebView();
    }

    private void initWebView() {
        // 配置WebView
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.setWebViewClient(new WebViewClient());
    }

    /**
     * 加载网页
     */
    public void loadUrl(String url) {
        mWebView.loadUrl(url);
    }

    /**
     * 执行JavaScript
     */
    public void evaluateJavascript(String script) {
        mWebView.evaluateJavascript(script, null);
    }

    /**
     * 获取WebView
     */
    public WebView getWebView() {
        return mWebView;
    }

    /**
     * 设置WebViewClient
     */
    public void setWebViewClient(WebViewClient client) {
        mWebView.setWebViewClient(client);
    }

    /**
     * 销毁浏览器
     */
    public void destroy() {
        mWebView.destroy();
    }
}
