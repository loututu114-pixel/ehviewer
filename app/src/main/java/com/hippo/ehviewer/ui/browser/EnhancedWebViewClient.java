package com.hippo.ehviewer.ui.browser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.hippo.ehviewer.R;

import java.util.Stack;

/**
 * 增强版WebViewClient
 * 参考YCWebView实现，提供完整的协议处理和错误处理
 * 修复了以下问题：
 * 1. 增加Activity生命周期检查
 * 2. 智能重定向处理，避免循环重定向
 * 3. URL栈管理，支持前进后退
 * 4. 完善错误类型区分
 * 5. 内存泄漏防护
 */
public class EnhancedWebViewClient extends WebViewClient {

    private static final String TAG = "EnhancedWebViewClient";
    private Activity mActivity;

    // YCWebView最佳实践：重定向处理
    private long mLastRedirectTime = 0;
    private static final long DEFAULT_REDIRECT_INTERVAL = 3000; // 3秒
    private final Stack<String> mUrlStack = new Stack<>();
    private boolean mIsLoading = false;
    private String mUrlBeforeRedirect;

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

        // YCWebView最佳实践：Activity生命周期检查
        if (!isActivityAlive()) {
            android.util.Log.w(TAG, "Activity not alive, ignoring URL loading: " + url);
            return false;
        }

        // YCWebView最佳实践：URL解码处理
        String decodedUrl = decodeUrl(url);
        if (decodedUrl == null) {
            return false;
        }

        // YCWebView最佳实践：记录URL到栈中
        recordUrl(decodedUrl);

        // 处理特殊协议
        if (handleSpecialProtocols(decodedUrl)) {
            return true;
        }

        // 处理文件下载
        if (handleFileDownload(decodedUrl)) {
            return true;
        }

        // 处理外部应用调用
        if (handleExternalApp(decodedUrl)) {
            return true;
        }

        // 让WebView处理HTTP/HTTPS链接
        return false;
    }

    /**
     * YCWebView最佳实践：Activity生命周期检查
     */
    private boolean isActivityAlive() {
        return mActivity != null && !mActivity.isFinishing() && !mActivity.isDestroyed();
    }

    /**
     * YCWebView最佳实践：URL解码
     */
    private String decodeUrl(String url) {
        try {
            return Uri.decode(url);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to decode URL: " + url, e);
            return url;
        }
    }

    /**
     * YCWebView最佳实践：记录URL到栈中
     */
    private void recordUrl(String url) {
        if (url != null && !url.isEmpty() && !url.equals(getUrl())) {
            if (mUrlBeforeRedirect != null) {
                mUrlStack.push(mUrlBeforeRedirect);
                mUrlBeforeRedirect = null;
            }
        }
    }

    /**
     * YCWebView最佳实践：获取最后停留页面的URL
     */
    private String getUrl() {
        return mUrlStack.size() > 0 ? mUrlStack.peek() : null;
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

            // 百度应用调用拦截 - 彻底屏蔽百度相关应用跳转，提升用户体验
            if (shouldBlockBaiduAppLaunch(scheme, url)) {
                android.util.Log.d(TAG, "Blocking Baidu app launch for URL: " + url);
                return true; // 阻止应用启动，不显示选择对话框
            }

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

    /**
     * 判断是否应该拦截百度应用启动
     * 彻底屏蔽百度相关的应用调用提示，让用户在WebView内正常使用百度搜索
     */
    private boolean shouldBlockBaiduAppLaunch(String scheme, String url) {
        if (scheme == null || url == null) return false;
        
        // 拦截所有百度相关的scheme协议
        return scheme.equals("baidu") ||
               scheme.equals("baidusearch") ||
               scheme.equals("bdapp") ||
               scheme.equals("baiduboxapp") ||
               scheme.equals("baidumap") ||
               scheme.equals("baidunavi") ||
               url.startsWith("baidu://") ||
               url.startsWith("baidusearch://") ||
               url.startsWith("bdapp://") ||
               url.startsWith("baiduboxapp://") ||
               url.startsWith("baidumap://") ||
               url.startsWith("baidunavi://");
    }

    /**
     * 检查是否应该抑制统计相关的错误（让用户无感）
     */
    private boolean shouldSuppressAnalyticsError(String url, String description) {
        if (url == null) return false;

        // 检查URL是否是统计分析相关
        if (url.contains("hm.baidu.com") ||
            url.contains("hpd.baidu.com") ||
            url.contains("h2tcbox.baidu.com") ||
            url.contains("push.zhanzhang.baidu.com") ||
            url.contains("google-analytics.com") ||
            url.contains("googletagmanager.com") ||
            url.contains("doubleclick.net") ||
            url.contains("googlesyndication.com") ||
            url.contains("googleadservices.com") ||
            url.contains("amazon-adsystem.com") ||
            url.contains("facebook.com/tr") ||
            url.contains("connect.facebook.net") ||
            url.contains("twitter.com/i/adsct") ||
            url.contains("tiktok.com/i18n/pixel")) {
            return true;
        }

        // 检查是否是统计或分析相关的错误描述
        if (description != null) {
            String lowerDesc = description.toLowerCase();
            if (lowerDesc.contains("baidu") ||
                lowerDesc.contains("统计") ||
                lowerDesc.contains("analytics") ||
                lowerDesc.contains("tracking") ||
                lowerDesc.contains("广告") ||
                lowerDesc.contains("ad")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);

        String url = request.getUrl().toString();
        String errorDescription = error.getDescription() != null ? error.getDescription().toString() : "未知错误";
        int errorCode = error.getErrorCode();

        // YCWebView最佳实践：重定向循环检测
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (errorCode == WebViewClient.ERROR_REDIRECT_LOOP) {
                resolveRedirect(view);
                return;
            }
        }

        // 检查是否是需要忽略的统计相关错误
        if (shouldSuppressAnalyticsError(url, errorDescription)) {
            android.util.Log.d(TAG, "Suppressing analytics error for URL: " + url);
            return; // 不传递给Activity，让用户无感
        }

        android.util.Log.e(TAG, "WebView error: " + errorDescription + " (code: " + errorCode + ") for URL: " + url);

        // YCWebView最佳实践：错误类型分类处理
        handleErrorByType(view, errorCode, errorDescription, url);
    }

    /**
     * YCWebView最佳实践：错误类型分类处理
     */
    private void handleErrorByType(WebView view, int errorCode, String description, String failingUrl) {
        // YCWebView错误分类处理
        if (errorCode == WebViewClient.ERROR_TIMEOUT) {
            // 网络连接超时
            handleTimeoutError(view, failingUrl);
        } else if (errorCode == WebViewClient.ERROR_CONNECT) {
            // 断网或连接失败
            handleConnectError(view, failingUrl);
        } else if (errorCode == WebViewClient.ERROR_PROXY_AUTHENTICATION) {
            // 代理异常
            handleProxyError(view, failingUrl);
        } else {
            // 其他错误
            handleGenericError(view, errorCode, description, failingUrl);
        }
    }

    /**
     * YCWebView最佳实践：处理超时错误
     */
    private void handleTimeoutError(WebView view, String url) {
        android.util.Log.w(TAG, "Network timeout for URL: " + url);
        if (mActivity instanceof ErrorCallback) {
            ((ErrorCallback) mActivity).onWebViewError(url, WebViewClient.ERROR_TIMEOUT, "网络连接超时");
        }
    }

    /**
     * YCWebView最佳实践：处理连接错误
     */
    private void handleConnectError(WebView view, String url) {
        android.util.Log.w(TAG, "Network connection failed for URL: " + url);
        if (mActivity instanceof ErrorCallback) {
            ((ErrorCallback) mActivity).onWebViewError(url, WebViewClient.ERROR_CONNECT, "网络连接失败");
        }
    }

    /**
     * YCWebView最佳实践：处理代理错误
     */
    private void handleProxyError(WebView view, String url) {
        android.util.Log.w(TAG, "Proxy authentication failed for URL: " + url);
        if (mActivity instanceof ErrorCallback) {
            ((ErrorCallback) mActivity).onWebViewError(url, WebViewClient.ERROR_PROXY_AUTHENTICATION, "代理认证失败");
        }
    }

    /**
     * YCWebView最佳实践：处理通用错误
     */
    private void handleGenericError(WebView view, int errorCode, String description, String url) {
        android.util.Log.w(TAG, "Generic error for URL: " + url + ", error: " + description);
        if (mActivity instanceof ErrorCallback) {
            ((ErrorCallback) mActivity).onWebViewError(url, errorCode, description);
        }
    }

    /**
     * YCWebView最佳实践：解决重定向循环
     */
    private void resolveRedirect(WebView view) {
        final long now = System.currentTimeMillis();
        if (now - mLastRedirectTime > DEFAULT_REDIRECT_INTERVAL) {
            mLastRedirectTime = now;
            android.util.Log.d(TAG, "Resolving redirect loop by reloading");
            view.reload();
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

        android.util.Log.d(TAG, "Page started loading: " + url);

        // YCWebView最佳实践：处理重定向前的URL
        if (mIsLoading && mUrlStack.size() > 0) {
            mUrlBeforeRedirect = mUrlStack.pop();
        }
        recordUrl(url);
        mIsLoading = true;

        // 通知Activity页面开始加载
        if (mActivity instanceof PageCallback) {
            ((PageCallback) mActivity).onPageStarted(url);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        android.util.Log.d(TAG, "Page finished loading: " + url);

        // YCWebView最佳实践：重置加载状态
        if (mIsLoading) {
            mIsLoading = false;
        }

        // YCWebView最佳实践：设置网页在加载的时候暂时不加载图片
        if (!view.getSettings().getLoadsImagesAutomatically()) {
            view.getSettings().setLoadsImagesAutomatically(true);
        }

        // 通知Activity页面加载完成
        if (mActivity instanceof PageCallback) {
            ((PageCallback) mActivity).onPageFinished(url);
        }
    }

    /**
     * YCWebView最佳实践：处理缩放变化，解决视频全屏播放返回时页面被放大的问题
     */
    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        super.onScaleChanged(view, oldScale, newScale);

        android.util.Log.d(TAG, "Scale changed from " + oldScale + " to " + newScale);

        // YCWebView最佳实践：视频全屏播放按返回页面被放大的问题
        if (newScale - oldScale > 7) {
            // 异常放大，缩回去
            view.setInitialScale((int) (oldScale / newScale * 100));
            android.util.Log.d(TAG, "Abnormal scale detected, resetting to: " + oldScale);
        }
    }

    /**
     * YCWebView最佳实践：检查是否可以回退操作
     */
    public boolean pageCanGoBack() {
        return mUrlStack.size() >= 2;
    }

    /**
     * YCWebView最佳实践：执行回退操作
     */
    public boolean pageGoBack(WebView webView) {
        if (pageCanGoBack()) {
            final String url = popBackUrl();
            if (url != null) {
                webView.loadUrl(url);
                return true;
            }
        }
        return false;
    }

    /**
     * YCWebView最佳实践：弹出回退URL
     */
    private String popBackUrl() {
        if (mUrlStack.size() >= 2) {
            mUrlStack.pop(); // 移除当前页面
            return mUrlStack.pop(); // 返回上一页
        }
        return null;
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
        // 暂时禁用所有请求拦截，以解决页面空白问题
        // String url = request.getUrl().toString();
        // if (shouldBlockBaiduRequest(url)) {
        //     android.util.Log.d(TAG, "Blocking Baidu request to prevent ORB: " + url);
        //     return new android.webkit.WebResourceResponse("text/plain", "utf-8", null);
        // }

        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        // 暂时禁用所有请求拦截，以解决页面空白问题
        // if (shouldBlockBaiduRequest(url)) {
        //     android.util.Log.d(TAG, "Blocking Baidu request to prevent ORB: " + url);
        //     return new android.webkit.WebResourceResponse("text/plain", "utf-8", null);
        // }

        return super.shouldInterceptRequest(view, url);
    }

    /**
     * 判断是否应该阻止百度相关的请求
     * 只拦截特定的统计和分析请求，不影响正常的页面内容
     */
    private boolean shouldBlockBaiduRequest(String url) {
        if (url == null) return false;

        // 只拦截特定的统计和分析请求，不拦截正常内容
        
        // 检查h2tcbox.baidu.com的统计请求
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
        
        // 检查其他已知的百度统计域名，但不拦截主要内容域名
        if (url.contains("tongji.baidu.com") || 
            url.contains("analytics.baidu.com") ||
            url.contains("mtj.baidu.com")) {
            return true;
        }

        return false;
    }

}
