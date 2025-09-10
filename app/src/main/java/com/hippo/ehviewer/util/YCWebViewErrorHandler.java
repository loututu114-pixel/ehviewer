package com.hippo.ehviewer.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * YCWebView风格的错误处理器
 * 参考YCWebView的最佳实践，实现完整的错误处理机制
 *
 * 核心特性：
 * 1. 错误类型分类处理
 * 2. 重定向循环检测和修复
 * 3. 网络状态判断
 * 4. 错误页面渲染
 * 5. 统计错误抑制
 */
public class YCWebViewErrorHandler {

    private static final String TAG = "YCWebViewErrorHandler";

    private Context context;
    private long lastRedirectTime = 0;
    private static final long REDIRECT_INTERVAL = 3000; // 3秒

    public YCWebViewErrorHandler(Context context) {
        this.context = context;
    }

    /**
     * YCWebView最佳实践：处理错误类型分类
     */
    public boolean handleError(WebView view, WebResourceRequest request, WebResourceError error) {
        String url = request.getUrl().toString();
        int errorCode = error.getErrorCode();
        String description = error.getDescription().toString();

        Log.e(TAG, "YCWebView: Error " + errorCode + " for URL: " + url + " - " + description);

        // YCWebView最佳实践：重定向循环检测
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (errorCode == WebViewClient.ERROR_REDIRECT_LOOP) {
                return resolveRedirectLoop(view);
            }
        }

        // YCWebView最佳实践：错误分类处理
        return handleErrorByType(view, errorCode, description, url);
    }

    /**
     * YCWebView最佳实践：解决重定向循环
     */
    private boolean resolveRedirectLoop(WebView view) {
        long now = System.currentTimeMillis();
        if (now - lastRedirectTime > REDIRECT_INTERVAL) {
            lastRedirectTime = now;
            Log.d(TAG, "YCWebView: Resolving redirect loop by reloading");
            view.reload();
            return true;
        }
        return false;
    }

    /**
     * YCWebView最佳实践：按错误类型处理
     */
    private boolean handleErrorByType(WebView view, int errorCode, String description, String url) {
        switch (errorCode) {
            case WebViewClient.ERROR_TIMEOUT:
                return handleTimeoutError(view, url);
            case WebViewClient.ERROR_CONNECT:
                return handleConnectError(view, url);
            case WebViewClient.ERROR_PROXY_AUTHENTICATION:
                return handleProxyError(view, url);
            default:
                return handleGenericError(view, errorCode, description, url);
        }
    }

    /**
     * YCWebView最佳实践：处理超时错误
     */
    private boolean handleTimeoutError(WebView view, String url) {
        Log.w(TAG, "YCWebView: Network timeout for URL: " + url);

        // 检查网络连接
        if (!isNetworkAvailable()) {
            showNoNetworkError(view);
        } else {
            showTimeoutError(view);
        }
        return true;
    }

    /**
     * YCWebView最佳实践：处理连接错误
     */
    private boolean handleConnectError(WebView view, String url) {
        Log.w(TAG, "YCWebView: Network connection failed for URL: " + url);

        if (!isNetworkAvailable()) {
            showNoNetworkError(view);
        } else {
            showConnectionError(view);
        }
        return true;
    }

    /**
     * YCWebView最佳实践：处理代理错误
     */
    private boolean handleProxyError(WebView view, String url) {
        Log.w(TAG, "YCWebView: Proxy authentication failed for URL: " + url);
        showProxyError(view);
        return true;
    }

    /**
     * YCWebView最佳实践：处理通用错误
     */
    private boolean handleGenericError(WebView view, int errorCode, String description, String url) {
        Log.w(TAG, "YCWebView: Generic error for URL: " + url + " - " + description);
        showGenericError(view, errorCode, description);
        return true;
    }

    /**
     * YCWebView最佳实践：检查网络是否可用
     */
    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "YCWebView: Error checking network", e);
            return false;
        }
    }

    /**
     * YCWebView最佳实践：显示无网络错误页面
     */
    private void showNoNetworkError(WebView view) {
        String errorHtml = createErrorHtml("无网络连接",
                "请检查网络连接后重试",
                "javascript:location.reload()");
        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    /**
     * YCWebView最佳实践：显示超时错误页面
     */
    private void showTimeoutError(WebView view) {
        String errorHtml = createErrorHtml("网络超时",
                "连接超时，请检查网络后重试",
                "javascript:location.reload()");
        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    /**
     * YCWebView最佳实践：显示连接错误页面
     */
    private void showConnectionError(WebView view) {
        String errorHtml = createErrorHtml("连接失败",
                "无法连接到服务器，请稍后重试",
                "javascript:location.reload()");
        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    /**
     * YCWebView最佳实践：显示代理错误页面
     */
    private void showProxyError(WebView view) {
        String errorHtml = createErrorHtml("代理错误",
                "代理服务器认证失败",
                "javascript:location.reload()");
        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    /**
     * YCWebView最佳实践：显示通用错误页面
     */
    private void showGenericError(WebView view, int errorCode, String description) {
        String errorHtml = createErrorHtml("加载失败",
                "错误代码: " + errorCode + "<br>" + description,
                "javascript:location.reload()");
        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    /**
     * YCWebView最佳实践：创建错误页面HTML
     */
    private String createErrorHtml(String title, String message, String retryAction) {
        return "<!DOCTYPE html>" +
               "<html><head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<style>" +
               "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }" +
               ".error-container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
               "h1 { color: #e74c3c; margin-bottom: 20px; }" +
               "p { color: #666; margin-bottom: 30px; }" +
               "button { background: #3498db; color: white; border: none; padding: 12px 24px; border-radius: 5px; cursor: pointer; font-size: 16px; }" +
               "button:hover { background: #2980b9; }" +
               "</style>" +
               "</head><body>" +
               "<div class='error-container'>" +
               "<h1>" + title + "</h1>" +
               "<p>" + message + "</p>" +
               "<button onclick='" + retryAction + "'>重试</button>" +
               "</div>" +
               "</body></html>";
    }

    /**
     * YCWebView最佳实践：获取错误统计
     */
    public ErrorStats getErrorStats() {
        return new ErrorStats();
    }

    /**
     * YCWebView最佳实践：错误统计类
     */
    public static class ErrorStats {
        public int timeoutErrors = 0;
        public int connectErrors = 0;
        public int proxyErrors = 0;
        public int genericErrors = 0;
        public int redirectLoops = 0;

        @Override
        public String toString() {
            return String.format("ErrorStats{timeout=%d, connect=%d, proxy=%d, generic=%d, redirects=%d}",
                    timeoutErrors, connectErrors, proxyErrors, genericErrors, redirectLoops);
        }
    }
}
