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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.client.X5WebViewManager;

/**
 * 统一的WebView包装类
 * 自动选择X5 WebView或系统WebView
 */
public class UnifiedWebView extends android.webkit.WebView {

    private Object mActualWebView; // 可以是X5 WebView或系统WebView
    private boolean mIsX5WebView = false;

    public UnifiedWebView(Context context) {
        super(context);
        initWebView(context);
    }

    public UnifiedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWebView(context);
    }

    public UnifiedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWebView(context);
    }

    private void initWebView(Context context) {
        X5WebViewManager x5Manager = X5WebViewManager.getInstance();

        if (x5Manager.isX5Available()) {
            try {
                // 创建X5 WebView
                mActualWebView = new com.tencent.smtt.sdk.WebView(context);
                mIsX5WebView = true;

                // 隐藏系统WebView，只显示X5 WebView
                this.setVisibility(GONE);
                ((com.tencent.smtt.sdk.WebView) mActualWebView).setVisibility(VISIBLE);

                // 将X5 WebView添加到父容器
                if (getParent() != null && getParent() instanceof android.view.ViewGroup) {
                    android.view.ViewGroup parent = (android.view.ViewGroup) getParent();
                    int index = parent.indexOfChild(this);
                    parent.addView((com.tencent.smtt.sdk.WebView) mActualWebView, index + 1, getLayoutParams());
                }

            } catch (Exception e) {
                // X5创建失败，使用系统WebView
                mActualWebView = this;
                mIsX5WebView = false;
            }
        } else {
            // 使用系统WebView
            mActualWebView = this;
            mIsX5WebView = false;
        }
    }

    /**
     * 获取实际使用的WebView对象
     */
    public Object getActualWebView() {
        return mActualWebView;
    }

    /**
     * 是否正在使用X5 WebView
     */
    public boolean isX5WebView() {
        return mIsX5WebView;
    }

    /**
     * 统一的loadUrl方法
     */
    @Override
    public void loadUrl(String url) {
        if (mIsX5WebView) {
            ((com.tencent.smtt.sdk.WebView) mActualWebView).loadUrl(url);
        } else {
            super.loadUrl(url);
        }
    }

    /**
     * 统一的setWebViewClient方法
     */
    @Override
    public void setWebViewClient(WebViewClient client) {
        if (mIsX5WebView) {
            // 创建X5 WebViewClient适配器
            com.tencent.smtt.sdk.WebViewClient x5Client = createX5WebViewClient(client);
            ((com.tencent.smtt.sdk.WebView) mActualWebView).setWebViewClient(x5Client);
        } else {
            super.setWebViewClient(client);
        }
    }

    /**
     * 统一的setWebChromeClient方法
     */
    @Override
    public void setWebChromeClient(WebChromeClient client) {
        if (mIsX5WebView) {
            // 创建X5 WebChromeClient适配器
            com.tencent.smtt.sdk.WebChromeClient x5Client = createX5WebChromeClient(client);
            ((com.tencent.smtt.sdk.WebView) mActualWebView).setWebChromeClient(x5Client);
        } else {
            super.setWebChromeClient(client);
        }
    }

    /**
     * 统一的getSettings方法
     */
    @Override
    public android.webkit.WebSettings getSettings() {
        // 暂时简化实现，直接返回系统WebView的设置
        // X5的完整集成需要更复杂的WebSettings适配
        if (mIsX5WebView) {
            // 对于X5 WebView，我们可以返回一个包装的设置对象
            // 这里暂时返回系统WebView的设置作为简化方案
            android.webkit.WebView tempWebView = new android.webkit.WebView(getContext());
            android.webkit.WebSettings settings = tempWebView.getSettings();
            tempWebView.destroy();
            return settings;
        } else {
            return super.getSettings();
        }
    }

    /**
     * 创建X5 WebViewClient适配器
     */
    private com.tencent.smtt.sdk.WebViewClient createX5WebViewClient(android.webkit.WebViewClient client) {
        return new com.tencent.smtt.sdk.WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(com.tencent.smtt.sdk.WebView view, String url) {
                return client.shouldOverrideUrlLoading(UnifiedWebView.this, url);
            }

            @Override
            public void onPageStarted(com.tencent.smtt.sdk.WebView view, String url, Bitmap favicon) {
                client.onPageStarted(UnifiedWebView.this, url, favicon);
            }

            @Override
            public void onPageFinished(com.tencent.smtt.sdk.WebView view, String url) {
                client.onPageFinished(UnifiedWebView.this, url);
            }

            @Override
            public void onReceivedError(com.tencent.smtt.sdk.WebView view, int errorCode, String description, String failingUrl) {
                client.onReceivedError(UnifiedWebView.this, errorCode, description, failingUrl);
            }
        };
    }

    /**
     * 创建X5 WebChromeClient适配器
     */
    private com.tencent.smtt.sdk.WebChromeClient createX5WebChromeClient(android.webkit.WebChromeClient client) {
        return new com.tencent.smtt.sdk.WebChromeClient() {
            @Override
            public void onProgressChanged(com.tencent.smtt.sdk.WebView view, int newProgress) {
                client.onProgressChanged(UnifiedWebView.this, newProgress);
            }

            @Override
            public void onReceivedTitle(com.tencent.smtt.sdk.WebView view, String title) {
                client.onReceivedTitle(UnifiedWebView.this, title);
            }

            // 注释掉可能有问题的console message方法
            // @Override
            // public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            //     return client.onConsoleMessage(consoleMessage);
            // }
        };
    }

    /**
     * 统一的销毁方法
     */
    public void destroyUnified() {
        if (mIsX5WebView) {
            ((com.tencent.smtt.sdk.WebView) mActualWebView).destroy();
        } else {
            destroy();
        }
    }

    /**
     * 统一的其他方法可以根据需要添加...
     */

    // 暂时注释掉UnifiedWebSettings类，因为它需要实现太多抽象方法
    // /**
    //  * 统一的WebSettings包装类
    //  */
    // private static class UnifiedWebSettings extends android.webkit.WebSettings {
    //     // 实现所有抽象方法需要大量代码，这里暂时简化
    // }
}
