package com.hippo.ehviewer.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 错误恢复管理器
 * 基于腾讯X5和YCWebView最佳实践，实现智能的错误检测和恢复
 *
 * 核心特性：
 * - 错误类型自动识别
 * - 智能恢复策略
 * - 用户友好的错误页面
 * - 崩溃检测和恢复
 * - 网络异常处理
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ErrorRecoveryManager {

    private static final String TAG = "ErrorRecoveryManager";

    // 错误处理配置
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    private static final long RECOVERY_DELAY = 2000; // 2秒延迟
    private static final long CRASH_TIMEOUT = 30000; // 30秒崩溃超时

    // 错误处理器映射
    private final java.util.Map<Integer, ErrorHandler> errorHandlers = new java.util.HashMap<>();
    private final Context context;
    private final Handler mainHandler;

    // 状态跟踪
    private int recoveryAttempts = 0;
    private long lastErrorTime = 0;
    private String lastErrorUrl = null;

    // 崩溃检测
    private WebView currentWebView;
    private long lastPageLoadTime = 0;
    private boolean isCrashDetectionEnabled = false;

    public ErrorRecoveryManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());

        initializeErrorHandlers();
        Log.i(TAG, "ErrorRecoveryManager initialized");
    }

    /**
     * 初始化错误处理器
     */
    private void initializeErrorHandlers() {
        // 网络错误处理器
        errorHandlers.put(WebViewClient.ERROR_CONNECT, new NetworkErrorHandler());
        errorHandlers.put(WebViewClient.ERROR_TIMEOUT, new TimeoutErrorHandler());
        errorHandlers.put(WebViewClient.ERROR_HOST_LOOKUP, new DnsErrorHandler());

        // HTTP错误处理器
        errorHandlers.put(403, new AccessDeniedErrorHandler());
        errorHandlers.put(404, new NotFoundErrorHandler());
        errorHandlers.put(500, new ServerErrorHandler());
        errorHandlers.put(502, new BadGatewayErrorHandler());
        errorHandlers.put(503, new ServiceUnavailableErrorHandler());

        // SSL错误处理器
        errorHandlers.put(WebViewClient.ERROR_FAILED_SSL_HANDSHAKE, new SSLErrorHandler());

        // 其他错误处理器
        errorHandlers.put(WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME, new AuthErrorHandler());
        errorHandlers.put(WebViewClient.ERROR_REDIRECT_LOOP, new RedirectLoopErrorHandler());
        errorHandlers.put(WebViewClient.ERROR_UNSUPPORTED_SCHEME, new UnsupportedSchemeErrorHandler());
    }

    /**
     * 设置WebView错误处理
     */
    public void setupWebView(WebView webView) {
        this.currentWebView = webView;

        // 启用崩溃检测
        enableCrashDetection(webView);

        Log.d(TAG, "Error recovery setup completed for WebView");
    }

    /**
     * 处理错误
     */
    public boolean handleError(int errorCode, String description, String failingUrl) {
        Log.e(TAG, "Handling error - Code: " + errorCode + ", Description: " + description + ", URL: " + failingUrl);

        // 更新错误状态
        lastErrorTime = System.currentTimeMillis();
        lastErrorUrl = failingUrl;

        // 获取对应的错误处理器
        ErrorHandler handler = errorHandlers.get(errorCode);
        if (handler != null) {
            return handler.handleError(currentWebView, errorCode, description, failingUrl);
        }

        // 默认错误处理
        return handleDefaultError(currentWebView, errorCode, description, failingUrl);
    }

    /**
     * 处理HTTP错误
     */
    public boolean handleHttpError(int statusCode, String description, String failingUrl) {
        Log.e(TAG, "Handling HTTP error - Status: " + statusCode + ", Description: " + description + ", URL: " + failingUrl);

        // 更新错误状态
        lastErrorTime = System.currentTimeMillis();
        lastErrorUrl = failingUrl;

        // 获取对应的错误处理器
        ErrorHandler handler = errorHandlers.get(statusCode);
        if (handler != null) {
            return handler.handleError(currentWebView, statusCode, description, failingUrl);
        }

        // 默认HTTP错误处理
        return handleDefaultHttpError(currentWebView, statusCode, description, failingUrl);
    }

    /**
     * 默认错误处理
     */
    private boolean handleDefaultError(WebView webView, int errorCode, String description, String failingUrl) {
        if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
            Log.w(TAG, "Max recovery attempts reached, showing error page");
            showErrorPage(webView, errorCode, description, failingUrl);
            return true;
        }

        recoveryAttempts++;
        Log.d(TAG, "Attempting default recovery (attempt " + recoveryAttempts + ")");

        // 延迟后重试
        mainHandler.postDelayed(() -> {
            if (webView != null) {
                Log.d(TAG, "Retrying page load after error");
                webView.reload();
            }
        }, RECOVERY_DELAY);

        return true; // 消费掉错误，不显示默认错误页面
    }

    /**
     * 默认HTTP错误处理
     */
    private boolean handleDefaultHttpError(WebView webView, int statusCode, String description, String failingUrl) {
        if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
            showErrorPage(webView, statusCode, description, failingUrl);
            return true;
        }

        recoveryAttempts++;
        Log.d(TAG, "Attempting HTTP error recovery (attempt " + recoveryAttempts + ")");

        // 对于某些HTTP错误，可以尝试不同的策略
        if (statusCode == 403) {
            // 403错误可能需要更换User-Agent
            attemptUserAgentRecovery(webView, failingUrl);
        } else {
            // 其他HTTP错误直接重试
            mainHandler.postDelayed(() -> {
                if (webView != null) {
                    webView.reload();
                }
            }, RECOVERY_DELAY);
        }

        return true;
    }

    /**
     * 尝试User-Agent恢复策略
     */
    private void attemptUserAgentRecovery(WebView webView, String failingUrl) {
        Log.d(TAG, "Attempting User-Agent recovery for: " + failingUrl);

        // 这里可以集成UserAgentManager来尝试不同的User-Agent
        // 简化实现，直接重试
        mainHandler.postDelayed(() -> {
            if (webView != null) {
                webView.reload();
            }
        }, RECOVERY_DELAY);
    }

    /**
     * 显示错误页面
     */
    private void showErrorPage(WebView webView, int errorCode, String description, String failingUrl) {
        try {
            String errorPageHtml = generateErrorPageHtml(errorCode, description, failingUrl);
            webView.loadDataWithBaseURL(null, errorPageHtml, "text/html", "UTF-8", null);
            Log.d(TAG, "Error page displayed for error: " + errorCode);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show error page", e);
        }
    }

    /**
     * 生成错误页面HTML
     */
    private String generateErrorPageHtml(int errorCode, String description, String failingUrl) {
        String errorType = getErrorTypeDescription(errorCode);

        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<title>加载失败 - EhViewer</title>" +
               "<style>" +
               "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; }" +
               ".error-container { max-width: 600px; background: white; border-radius: 15px; padding: 40px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); text-align: center; }" +
               ".error-icon { font-size: 72px; margin-bottom: 20px; }" +
               ".error-title { color: #333; margin-bottom: 10px; font-size: 28px; font-weight: 600; }" +
               ".error-type { color: #666; margin-bottom: 20px; font-size: 16px; padding: 8px 16px; background: #f8f9fa; border-radius: 20px; display: inline-block; }" +
               ".error-description { color: #666; margin-bottom: 30px; line-height: 1.6; font-size: 16px; }" +
               ".error-url { background: #f8f9fa; padding: 15px; border-radius: 8px; font-family: 'Consolas', 'Monaco', monospace; margin-bottom: 20px; word-break: break-all; font-size: 14px; color: #495057; }" +
               ".retry-btn { background: linear-gradient(45deg, #ff6b35, #f7931e); color: white; border: none; padding: 15px 30px; border-radius: 25px; font-size: 16px; font-weight: 600; cursor: pointer; margin: 8px; transition: all 0.3s ease; }" +
               ".retry-btn:hover { transform: translateY(-2px); box-shadow: 0 5px 15px rgba(255, 107, 53, 0.4); }" +
               ".secondary-btn { background: #6c757d; color: white; border: none; padding: 12px 25px; border-radius: 20px; font-size: 14px; cursor: pointer; margin: 8px; transition: all 0.3s ease; }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='error-container'>" +
               "<div class='error-icon'>🚨</div>" +
               "<h1 class='error-title'>网页加载失败</h1>" +
               "<div class='error-type'>" + errorType + "</div>" +
               "<div class='error-description'>" +
               "<p>错误代码: <strong>" + errorCode + "</strong></p>" +
               "<p>" + (description != null ? description : "未知错误") + "</p>" +
               "</div>" +
               (failingUrl != null ? "<div class='error-url'>" + failingUrl + "</div>" : "") +
               "<button class='retry-btn' onclick='location.reload()'>重新加载</button>" +
               "<button class='secondary-btn' onclick='history.back()'>返回上一页</button>" +
               "</div>" +
               "</body>" +
               "</html>";
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
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                return "SSL证书错误";
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
     * 启用崩溃检测
     */
    private void enableCrashDetection(WebView webView) {
        if (isCrashDetectionEnabled) return;

        isCrashDetectionEnabled = true;

        // 启动崩溃检测定时器
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForCrash();
                if (isCrashDetectionEnabled) {
                    mainHandler.postDelayed(this, CRASH_TIMEOUT);
                }
            }
        }, CRASH_TIMEOUT);

        Log.d(TAG, "Crash detection enabled");
    }

    /**
     * 检查WebView是否崩溃
     */
    private void checkForCrash() {
        if (currentWebView == null) return;

        long currentTime = System.currentTimeMillis();
        long timeSinceLastLoad = currentTime - lastPageLoadTime;

        if (timeSinceLastLoad > CRASH_TIMEOUT && lastPageLoadTime > 0) {
            Log.w(TAG, "WebView crash detected - no activity for " + (timeSinceLastLoad / 1000) + " seconds");
            handleCrashRecovery();
        }
    }

    /**
     * 处理崩溃恢复
     */
    private void handleCrashRecovery() {
        Log.i(TAG, "Initiating crash recovery");

        try {
            if (currentWebView != null) {
                // 停止当前WebView
                currentWebView.stopLoading();

                // 显示崩溃恢复页面
                showCrashRecoveryPage(currentWebView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during crash recovery", e);
        }
    }

    /**
     * 显示崩溃恢复页面
     */
    private void showCrashRecoveryPage(WebView webView) {
        String crashPageHtml = generateCrashRecoveryPageHtml();
        webView.loadDataWithBaseURL(null, crashPageHtml, "text/html", "UTF-8", null);
        Log.d(TAG, "Crash recovery page displayed");
    }

    /**
     * 生成崩溃恢复页面HTML
     */
    private String generateCrashRecoveryPageHtml() {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<title>浏览器恢复 - EhViewer</title>" +
               "<style>" +
               "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #dc3545 0%, #c82333 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; }" +
               ".recovery-container { max-width: 500px; background: white; border-radius: 15px; padding: 40px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); text-align: center; }" +
               ".recovery-icon { font-size: 64px; margin-bottom: 20px; }" +
               ".recovery-title { color: #333; margin-bottom: 20px; font-size: 24px; font-weight: 600; }" +
               ".recovery-description { color: #666; margin-bottom: 30px; line-height: 1.6; }" +
               ".recovery-btn { background: linear-gradient(45deg, #28a745, #20c997); color: white; border: none; padding: 15px 30px; border-radius: 25px; font-size: 16px; font-weight: 600; cursor: pointer; margin: 8px; transition: all 0.3s ease; }" +
               ".recovery-btn:hover { transform: translateY(-2px); box-shadow: 0 5px 15px rgba(40, 167, 69, 0.4); }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='recovery-container'>" +
               "<div class='recovery-icon'>🔧</div>" +
               "<h1 class='recovery-title'>浏览器恢复</h1>" +
               "<div class='recovery-description'>" +
               "<p>检测到浏览器异常，已自动恢复</p>" +
               "<p>请点击下方按钮重新加载页面</p>" +
               "</div>" +
               "<button class='recovery-btn' onclick='location.reload()'>重新加载页面</button>" +
               "</div>" +
               "</body>" +
               "</html>";
    }

    /**
     * 更新页面加载时间戳
     */
    public void updatePageLoadTime() {
        lastPageLoadTime = System.currentTimeMillis();
    }

    /**
     * 重置恢复尝试次数
     */
    public void resetRecoveryAttempts() {
        recoveryAttempts = 0;
        Log.d(TAG, "Recovery attempts reset");
    }

    /**
     * 获取错误恢复统计信息
     */
    public ErrorRecoveryStats getStats() {
        return new ErrorRecoveryStats(
            recoveryAttempts,
            lastErrorTime,
            lastErrorUrl
        );
    }

    /**
     * 错误处理器接口
     */
    private interface ErrorHandler {
        boolean handleError(WebView webView, int errorCode, String description, String failingUrl);
    }

    /**
     * 网络错误处理器
     */
    private class NetworkErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            Log.d(TAG, "Handling network error: " + errorCode);
            // 网络错误通常需要延迟重试
            mainHandler.postDelayed(() -> {
                if (webView != null) {
                    webView.reload();
                }
            }, RECOVERY_DELAY * 2);
            return true;
        }
    }

    /**
     * 超时错误处理器
     */
    private class TimeoutErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            Log.d(TAG, "Handling timeout error");
            // 超时错误增加延迟时间
            mainHandler.postDelayed(() -> {
                if (webView != null) {
                    webView.reload();
                }
            }, RECOVERY_DELAY * 3);
            return true;
        }
    }

    /**
     * DNS错误处理器
     */
    private class DnsErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            Log.d(TAG, "Handling DNS error");
            // DNS错误可能需要等待网络恢复
            mainHandler.postDelayed(() -> {
                if (webView != null) {
                    webView.reload();
                }
            }, RECOVERY_DELAY * 5);
            return true;
        }
    }

    /**
     * 访问拒绝错误处理器
     */
    private class AccessDeniedErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            Log.d(TAG, "Handling 403 access denied error");
            // 403错误需要特殊的处理策略
            attemptUserAgentRecovery(webView, failingUrl);
            return true;
        }
    }

    /**
     * 其他错误处理器（可以继续扩展）
     */
    private class NotFoundErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            return handleDefaultError(webView, errorCode, description, failingUrl);
        }
    }

    private class ServerErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            return handleDefaultHttpError(webView, errorCode, description, failingUrl);
        }
    }

    private class BadGatewayErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            return handleDefaultHttpError(webView, errorCode, description, failingUrl);
        }
    }

    private class ServiceUnavailableErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            return handleDefaultHttpError(webView, errorCode, description, failingUrl);
        }
    }

    private class SSLErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            return handleDefaultError(webView, errorCode, description, failingUrl);
        }
    }

    private class AuthErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            return handleDefaultError(webView, errorCode, description, failingUrl);
        }
    }

    private class RedirectLoopErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            return handleDefaultError(webView, errorCode, description, failingUrl);
        }
    }

    private class UnsupportedSchemeErrorHandler implements ErrorHandler {
        @Override
        public boolean handleError(WebView webView, int errorCode, String description, String failingUrl) {
            return handleDefaultError(webView, errorCode, description, failingUrl);
        }
    }

    /**
     * 错误恢复统计信息类
     */
    public static class ErrorRecoveryStats {
        public final int recoveryAttempts;
        public final long lastErrorTime;
        public final String lastErrorUrl;

        public ErrorRecoveryStats(int recoveryAttempts, long lastErrorTime, String lastErrorUrl) {
            this.recoveryAttempts = recoveryAttempts;
            this.lastErrorTime = lastErrorTime;
            this.lastErrorUrl = lastErrorUrl;
        }

        @Override
        public String toString() {
            return String.format("ErrorRecoveryStats{attempts=%d, lastErrorTime=%d, lastErrorUrl='%s'}",
                    recoveryAttempts, lastErrorTime, lastErrorUrl);
        }
    }
}
