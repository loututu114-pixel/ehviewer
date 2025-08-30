package com.hippo.ehviewer.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.hippo.ehviewer.client.NetworkDetector;

/**
 * WebView错误处理增强器
 * 全面处理各种加载失败情况，彻底杜绝错误提示
 */
public class WebViewErrorHandler {
    private static final String TAG = "WebViewErrorHandler";

    private final Context context;
    private final WebView webView;
    private int consecutiveErrors = 0;
    private long lastErrorTime = 0;

    // 错误恢复策略
    private static final int MAX_CONSECUTIVE_ERRORS = 3;
    private static final long ERROR_RESET_TIME = 30000; // 30秒

    public WebViewErrorHandler(Context context, WebView webView) {
        this.context = context.getApplicationContext();
        this.webView = webView;
    }

    /**
     * 处理WebView错误
     */
    public boolean handleError(int errorCode, String description, String failingUrl) {
        Log.e(TAG, "WebView error: " + errorCode + " - " + description + " - " + failingUrl);

        // 检查是否需要重置错误计数
        if (System.currentTimeMillis() - lastErrorTime > ERROR_RESET_TIME) {
            consecutiveErrors = 0;
        }
        lastErrorTime = System.currentTimeMillis();
        consecutiveErrors++;

        // 根据错误代码进行不同处理
        switch (errorCode) {
            case WebViewClient.ERROR_UNKNOWN:
                return handleUnknownError(failingUrl);
            case WebViewClient.ERROR_HOST_LOOKUP:
                return handleHostLookupError(failingUrl);
            case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
                return handleUnsupportedSchemeError(failingUrl);
            case WebViewClient.ERROR_CONNECT:
                return handleConnectError(failingUrl);
            case WebViewClient.ERROR_TIMEOUT:
                return handleTimeoutError(failingUrl);
            case WebViewClient.ERROR_REDIRECT_LOOP:
                return handleRedirectLoopError(failingUrl);
            case WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME:
                return handleAuthSchemeError(failingUrl);
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                return handleSslError(failingUrl);
            case WebViewClient.ERROR_BAD_URL:
                return handleBadUrlError(failingUrl);
            case WebViewClient.ERROR_FILE:
                return handleFileError(failingUrl);
            case WebViewClient.ERROR_FILE_NOT_FOUND:
                return handleFileNotFoundError(failingUrl);
            case WebViewClient.ERROR_TOO_MANY_REQUESTS:
                return handleTooManyRequestsError(failingUrl);
            default:
                return handleGenericError(errorCode, description, failingUrl);
        }
    }

    /**
     * 处理HTTP错误
     */
    public boolean handleHttpError(WebResourceRequest request, WebResourceResponse errorResponse) {
        int statusCode = errorResponse.getStatusCode();
        String url = request.getUrl().toString();

        Log.e(TAG, "HTTP error: " + statusCode + " - " + url);

        switch (statusCode) {
            case 403:
                return handle403Error(url);
            case 404:
                return handle404Error(url);
            case 500:
                return handle500Error(url);
            case 502:
                return handle502Error(url);
            case 503:
                return handle503Error(url);
            default:
                return handleGenericHttpError(statusCode, url);
        }
    }

    // 具体错误处理方法
    private boolean handleUnknownError(String url) {
        if (!isNetworkAvailable()) {
            showNetworkErrorPage();
            return true;
        }

        if (consecutiveErrors < MAX_CONSECUTIVE_ERRORS) {
            retryWithDelay(url, 2000);
            return true;
        }

        showGenericErrorPage(url, "未知错误", "请检查网络连接或稍后重试");
        return true;
    }

    private boolean handleHostLookupError(String url) {
        if (!isNetworkAvailable()) {
            showNetworkErrorPage();
            return true;
        }

        // 可能是DNS问题，尝试搜索
        performSearch(url);
        return true;
    }

    private boolean handleUnsupportedSchemeError(String url) {
        // 检查是否是特殊协议
        if (url.startsWith("intent://") || url.startsWith("market://")) {
            handleIntentUrl(url);
            return true;
        }

        showGenericErrorPage(url, "不支持的协议", "此链接使用了不支持的协议");
        return true;
    }

    private boolean handleConnectError(String url) {
        if (!isNetworkAvailable()) {
            showNetworkErrorPage();
            return true;
        }

        if (consecutiveErrors < MAX_CONSECUTIVE_ERRORS) {
            retryWithDelay(url, 3000);
            return true;
        }

        showGenericErrorPage(url, "连接失败", "无法连接到服务器");
        return true;
    }

    private boolean handleTimeoutError(String url) {
        if (consecutiveErrors < MAX_CONSECUTIVE_ERRORS) {
            retryWithDelay(url, 5000);
            return true;
        }

        showGenericErrorPage(url, "连接超时", "服务器响应超时");
        return true;
    }

    private boolean handleRedirectLoopError(String url) {
        showGenericErrorPage(url, "重定向循环", "页面重定向次数过多");
        return true;
    }

    private boolean handleAuthSchemeError(String url) {
        showGenericErrorPage(url, "认证失败", "不支持的认证方式");
        return true;
    }

    private boolean handleSslError(String url) {
        // 对于HTTPS网站，尝试HTTP版本
        if (url.startsWith("https://")) {
            String httpUrl = url.replace("https://", "http://");
            loadUrl(httpUrl);
            return true;
        }

        showGenericErrorPage(url, "SSL证书错误", "网站证书验证失败");
        return true;
    }

    private boolean handleBadUrlError(String url) {
        // 可能是搜索关键词，执行搜索
        performSearch(url);
        return true;
    }

    private boolean handleFileError(String url) {
        showGenericErrorPage(url, "文件错误", "无法访问文件");
        return true;
    }

    private boolean handleFileNotFoundError(String url) {
        showGenericErrorPage(url, "文件未找到", "请求的文件不存在");
        return true;
    }

    private boolean handleTooManyRequestsError(String url) {
        // 等待一段时间后重试
        retryWithDelay(url, 10000);
        return true;
    }

    private boolean handle403Error(String url) {
        // YouTube等网站可能需要特殊处理
        if (YouTubeCompatibilityManager.isSpecialSite(url)) {
            YouTubeCompatibilityManager.tryDifferentUserAgents(webView, url);
            retryWithDelay(url, 2000);
            return true;
        }

        showGenericErrorPage(url, "访问被拒绝", "您没有权限访问此页面");
        return true;
    }

    private boolean handle404Error(String url) {
        showGenericErrorPage(url, "页面未找到", "请求的页面不存在 (404)");
        return true;
    }

    private boolean handle500Error(String url) {
        if (consecutiveErrors < MAX_CONSECUTIVE_ERRORS) {
            retryWithDelay(url, 3000);
            return true;
        }

        showGenericErrorPage(url, "服务器错误", "服务器内部错误 (500)");
        return true;
    }

    private boolean handle502Error(String url) {
        if (consecutiveErrors < MAX_CONSECUTIVE_ERRORS) {
            retryWithDelay(url, 5000);
            return true;
        }

        showGenericErrorPage(url, "网关错误", "服务器网关错误 (502)");
        return true;
    }

    private boolean handle503Error(String url) {
        // 服务不可用，通常是临时问题
        retryWithDelay(url, 10000);
        return true;
    }

    private boolean handleGenericHttpError(int statusCode, String url) {
        showGenericErrorPage(url, "HTTP错误", "HTTP " + statusCode + " 错误");
        return true;
    }

    private boolean handleGenericError(int errorCode, String description, String url) {
        if (!isNetworkAvailable()) {
            showNetworkErrorPage();
            return true;
        }

        if (consecutiveErrors < MAX_CONSECUTIVE_ERRORS) {
            retryWithDelay(url, 2000);
            return true;
        }

        showGenericErrorPage(url, "加载失败", description);
        return true;
    }

    // 工具方法
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability", e);
        }
        return false;
    }

    private void retryWithDelay(String url, long delay) {
        webView.postDelayed(() -> {
            try {
                loadUrl(url);
                Log.d(TAG, "Retrying load after " + delay + "ms: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Error retrying load", e);
            }
        }, delay);
    }

    private void loadUrl(String url) {
        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    private void performSearch(String query) {
        try {
            String searchUrl = "https://www.google.com/search?q=" +
                android.net.Uri.encode(query);
            loadUrl(searchUrl);
            Log.d(TAG, "Performing search for: " + query);
        } catch (Exception e) {
            Log.e(TAG, "Error performing search", e);
        }
    }

    private void handleIntentUrl(String url) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error handling intent URL", e);
        }
    }

    private void showNetworkErrorPage() {
        String html = generateErrorPageHtml("网络连接失败",
            "请检查网络连接后重试",
            "network_error");
        loadHtmlContent(html);
    }

    private void showGenericErrorPage(String url, String title, String message) {
        String html = generateErrorPageHtml(title, message, url);
        loadHtmlContent(html);
    }

    private void loadHtmlContent(String html) {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        }
    }

    private String generateErrorPageHtml(String title, String message, String url) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>" + title + "</title>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }" +
            ".error-container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
            ".error-icon { font-size: 64px; color: #ff6b35; margin-bottom: 20px; }" +
            ".error-title { color: #333; margin-bottom: 20px; }" +
            ".error-description { color: #666; margin-bottom: 30px; line-height: 1.6; }" +
            ".retry-btn { background: #ff6b35; color: white; border: none; padding: 12px 30px; border-radius: 5px; font-size: 16px; cursor: pointer; margin: 10px; }" +
            ".url-info { background: #f8f8f8; padding: 10px; border-radius: 5px; font-family: monospace; margin-bottom: 20px; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='error-container'>" +
            "<div class='error-icon'>⚠️</div>" +
            "<h1 class='error-title'>" + title + "</h1>" +
            "<div class='error-description'>" + message + "</div>" +
            (url != null && !url.equals("network_error") ?
                "<div class='url-info'>" + url + "</div>" : "") +
            "<button class='retry-btn' onclick='location.reload()'>重试</button>" +
            "</div>" +
            "</body>" +
            "</html>";
    }

    /**
     * 重置错误计数器
     */
    public void resetErrorCount() {
        consecutiveErrors = 0;
        lastErrorTime = 0;
    }

    /**
     * 获取连续错误次数
     */
    public int getConsecutiveErrors() {
        return consecutiveErrors;
    }
}
