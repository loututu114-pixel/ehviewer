package com.hippo.ehviewer.ui.browser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.hippo.ehviewer.R;

/**
 * 增强版WebViewClient
 * 参考YCWebView实现，提供完整的协议处理和错误处理
 */
public class EnhancedWebViewClient extends WebViewClient {

    private static final String TAG = "EnhancedWebViewClient";
    private Activity mActivity;

    public EnhancedWebViewClient(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        android.util.Log.d(TAG, "Loading URL: " + url);

        // 处理特殊协议
        if (handleSpecialProtocols(url)) {
            return true;
        }

        // 处理文件下载
        if (handleFileDownload(url)) {
            return true;
        }

        // 处理外部应用调用
        if (handleExternalApp(url)) {
            return true;
        }

        // 让WebView处理HTTP/HTTPS链接
        return false;
    }

    /**
     * 处理特殊协议 (电话、短信、邮件等)
     */
    private boolean handleSpecialProtocols(String url) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();

            if ("tel".equals(scheme)) {
                // 打电话
                Intent intent = new Intent(Intent.ACTION_DIAL, uri);
                mActivity.startActivity(intent);
                return true;
            } else if ("sms".equals(scheme)) {
                // 发短信
                Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                mActivity.startActivity(intent);
                return true;
            } else if ("mailto".equals(scheme)) {
                // 发邮件
                Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                mActivity.startActivity(intent);
                return true;
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error handling special protocol: " + url, e);
        }
        return false;
    }

    /**
     * 处理文件下载和文档查看
     */
    private boolean handleFileDownload(String url) {
        // 检查是否是下载链接或文档
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".pdf") || lowerUrl.contains(".doc") || lowerUrl.contains(".docx") ||
            lowerUrl.contains(".xls") || lowerUrl.contains(".xlsx") || lowerUrl.contains(".ppt") ||
            lowerUrl.contains(".pptx") || lowerUrl.contains(".txt") || lowerUrl.contains(".zip") ||
            lowerUrl.contains(".rar") || lowerUrl.contains(".apk")) {

            try {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW);

                // 根据文件类型设置MIME类型
                String mimeType = getMimeTypeFromUrl(url);
                if (mimeType != null) {
                    intent.setDataAndType(uri, mimeType);
                } else {
                    intent.setData(uri);
                }

                // 添加标志以提高兼容性
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // 检查是否有应用可以处理此文件
                if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                    mActivity.startActivity(intent);
                    android.util.Log.d(TAG, "Opening document with system viewer: " + url);
                    return true;
                } else {
                    // 没有合适的查看器，使用浏览器下载
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                    mActivity.startActivity(browserIntent);
                    android.util.Log.d(TAG, "No suitable viewer found, using browser: " + url);
                    return true;
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error handling document: " + url, e);
                Toast.makeText(mActivity, "无法打开文档文件", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    /**
     * 根据URL获取MIME类型
     */
    private String getMimeTypeFromUrl(String url) {
        String lowerUrl = url.toLowerCase();

        if (lowerUrl.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerUrl.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerUrl.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerUrl.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerUrl.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerUrl.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (lowerUrl.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (lowerUrl.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerUrl.endsWith(".zip")) {
            return "application/zip";
        } else if (lowerUrl.endsWith(".rar")) {
            return "application/x-rar-compressed";
        } else if (lowerUrl.endsWith(".apk")) {
            return "application/vnd.android.package-archive";
        }

        return null;
    }

    /**
     * 处理外部应用调用
     */
    private boolean handleExternalApp(String url) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();

            // 检查是否是外部应用协议
            if (!"http".equals(scheme) && !"https".equals(scheme) && !"file".equals(scheme)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                    mActivity.startActivity(intent);
                    return true;
                } else {
                    Toast.makeText(mActivity, "没有应用可以处理此链接", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error handling external app: " + url, e);
        }
        return false;
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);

        String errorDescription = error.getDescription() != null ? error.getDescription().toString() : "未知错误";
        android.util.Log.e(TAG, "WebView error: " + errorDescription + " for URL: " + request.getUrl());

        // 通知Activity处理错误
        if (mActivity instanceof ErrorCallback) {
            ((ErrorCallback) mActivity).onWebViewError(request.getUrl().toString(), error.getErrorCode(),
                errorDescription);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        // 通知Activity页面加载完成
        if (mActivity instanceof PageCallback) {
            ((PageCallback) mActivity).onPageFinished(url);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

        // 通知Activity页面开始加载
        if (mActivity instanceof PageCallback) {
            ((PageCallback) mActivity).onPageStarted(url);
        }
    }

    /**
     * 错误回调接口
     */
    public interface ErrorCallback {
        void onWebViewError(String url, int errorCode, String description);
    }

    /**
     * 页面回调接口
     */
    public interface PageCallback {
        void onPageStarted(String url);
        void onPageFinished(String url);
    }

    @Override
    public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();

        // 处理百度相关域名的ORB阻塞问题
        if (shouldBlockBaiduRequest(url)) {
            android.util.Log.d(TAG, "Blocking Baidu request to prevent ORB: " + url);
            // 返回空的响应来阻止请求
            return new android.webkit.WebResourceResponse("text/plain", "utf-8", null);
        }

        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        // 处理百度相关域名的ORB阻塞问题
        if (shouldBlockBaiduRequest(url)) {
            android.util.Log.d(TAG, "Blocking Baidu request to prevent ORB: " + url);
            // 返回空的响应来阻止请求
            return new android.webkit.WebResourceResponse("text/plain", "utf-8", null);
        }

        return super.shouldInterceptRequest(view, url);
    }

    /**
     * 判断是否应该阻止百度相关的请求
     */
    private boolean shouldBlockBaiduRequest(String url) {
        if (url == null) return false;

        // 检查是否包含百度域名
        if (url.contains("baidu.com") || url.contains("bdstatic.com") ||
            url.contains("bdimg.com") || url.contains("baidustatic.com")) {

            // 特别检查h2tcbox.baidu.com的统计请求
            if (url.contains("h2tcbox.baidu.com") && url.contains("action=zpblog")) {
                return true;
            }

            // 检查hpd.baidu.com的统计请求
            if (url.contains("hpd.baidu.com") && url.contains("v.gif")) {
                return true;
            }

            // 检查百度统计和分析相关的请求
            if (url.contains("hm.baidu.com") || url.contains("push.zhanzhang.baidu.com")) {
                return true;
            }
        }

        return false;
    }
}
