package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 浏览器错误处理器
 * 提供全面的错误处理和恢复机制
 */
public class BrowserErrorHandler {
    
    private static final String TAG = "BrowserErrorHandler";
    
    // 错误类型
    public enum ErrorType {
        NETWORK_ERROR,          // 网络错误
        SSL_ERROR,             // SSL证书错误
        TIMEOUT_ERROR,         // 超时错误
        DNS_ERROR,             // DNS解析错误
        RESOURCE_ERROR,        // 资源加载错误
        SCRIPT_ERROR,          // JavaScript错误
        ENCODING_ERROR,        // 编码错误
        PERMISSION_ERROR,      // 权限错误
        MEMORY_ERROR,          // 内存错误
        UNKNOWN_ERROR          // 未知错误
    }
    
    // 错误恢复策略
    public enum RecoveryStrategy {
        RETRY,                 // 重试
        RELOAD,               // 重新加载
        USE_CACHE,            // 使用缓存
        FALLBACK_URL,         // 使用备用URL
        SHOW_ERROR_PAGE,      // 显示错误页面
        IGNORE                // 忽略错误
    }
    
    private Context context;
    private Handler retryHandler;
    private Map<String, AtomicInteger> retryCountMap;
    private Map<String, Long> lastRetryTimeMap;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_BASE = 1000; // 基础重试延迟（毫秒）
    
    // 错误回调接口
    public interface ErrorCallback {
        void onErrorResolved(String url);
        void onErrorPersists(String url, ErrorType errorType, String description);
        void onRetrying(String url, int retryCount);
    }
    
    public BrowserErrorHandler(Context context) {
        this.context = context;
        this.retryHandler = new Handler(Looper.getMainLooper());
        this.retryCountMap = new HashMap<>();
        this.lastRetryTimeMap = new HashMap<>();
    }
    
    /**
     * 处理错误
     */
    public void handleError(WebView webView, String url, int errorCode, 
                           String description, ErrorCallback callback) {
        ErrorType errorType = classifyError(errorCode, description);
        RecoveryStrategy strategy = determineRecoveryStrategy(errorType, url);
        
        switch (strategy) {
            case RETRY:
                retryLoading(webView, url, errorType, callback);
                break;
                
            case RELOAD:
                reloadPage(webView, url, callback);
                break;
                
            case USE_CACHE:
                loadFromCache(webView, url, callback);
                break;
                
            case FALLBACK_URL:
                loadFallbackUrl(webView, url, callback);
                break;
                
            case SHOW_ERROR_PAGE:
                showErrorPage(webView, url, errorType, description, callback);
                break;
                
            case IGNORE:
            default:
                // 忽略错误
                break;
        }
    }
    
    /**
     * 分类错误类型
     */
    private ErrorType classifyError(int errorCode, String description) {
        // 根据错误代码分类
        switch (errorCode) {
            case -2: // net::ERR_INTERNET_DISCONNECTED
            case -6: // net::ERR_CONNECTION_FAILED
            case -7: // net::ERR_CONNECTION_REFUSED
                return ErrorType.NETWORK_ERROR;
                
            case -8: // net::ERR_CONNECTION_TIMED_OUT
            case -15: // net::ERR_SOCKET_NOT_CONNECTED
                return ErrorType.TIMEOUT_ERROR;
                
            case -105: // net::ERR_NAME_NOT_RESOLVED
            case -137: // net::ERR_NAME_RESOLUTION_FAILED
                return ErrorType.DNS_ERROR;
                
            case -200: // net::ERR_CERT_COMMON_NAME_INVALID
            case -201: // net::ERR_CERT_DATE_INVALID
            case -202: // net::ERR_CERT_AUTHORITY_INVALID
                return ErrorType.SSL_ERROR;
                
            default:
                if (description != null) {
                    if (description.contains("memory") || description.contains("OOM")) {
                        return ErrorType.MEMORY_ERROR;
                    } else if (description.contains("permission")) {
                        return ErrorType.PERMISSION_ERROR;
                    } else if (description.contains("encoding")) {
                        return ErrorType.ENCODING_ERROR;
                    }
                }
                return ErrorType.UNKNOWN_ERROR;
        }
    }
    
    /**
     * 确定恢复策略
     */
    private RecoveryStrategy determineRecoveryStrategy(ErrorType errorType, String url) {
        // 检查网络状态
        if (!isNetworkAvailable() && errorType == ErrorType.NETWORK_ERROR) {
            return RecoveryStrategy.USE_CACHE;
        }
        
        // 根据错误类型确定策略
        switch (errorType) {
            case NETWORK_ERROR:
            case TIMEOUT_ERROR:
            case DNS_ERROR:
                // 网络相关错误，尝试重试
                if (getRetryCount(url) < MAX_RETRY_COUNT) {
                    return RecoveryStrategy.RETRY;
                } else {
                    return RecoveryStrategy.SHOW_ERROR_PAGE;
                }
                
            case SSL_ERROR:
                // SSL错误需要用户确认
                return RecoveryStrategy.SHOW_ERROR_PAGE;
                
            case RESOURCE_ERROR:
                // 资源错误可以尝试重新加载
                return RecoveryStrategy.RELOAD;
                
            case MEMORY_ERROR:
                // 内存错误，清理并重新加载
                return RecoveryStrategy.RELOAD;
                
            case ENCODING_ERROR:
                // 编码错误，尝试其他编码
                return RecoveryStrategy.FALLBACK_URL;
                
            default:
                return RecoveryStrategy.SHOW_ERROR_PAGE;
        }
    }
    
    /**
     * 重试加载
     */
    private void retryLoading(WebView webView, String url, ErrorType errorType, 
                             ErrorCallback callback) {
        int retryCount = incrementRetryCount(url);
        
        if (retryCount <= MAX_RETRY_COUNT) {
            // 计算重试延迟（指数退避）
            long delay = RETRY_DELAY_BASE * (long) Math.pow(2, retryCount - 1);
            
            callback.onRetrying(url, retryCount);
            
            retryHandler.postDelayed(() -> {
                if (isNetworkAvailable() || errorType != ErrorType.NETWORK_ERROR) {
                    webView.loadUrl(url);
                } else {
                    // 网络仍然不可用，显示错误页面
                    showErrorPage(webView, url, errorType, "网络连接失败", callback);
                }
            }, delay);
        } else {
            // 重试次数超限
            callback.onErrorPersists(url, errorType, "重试次数超限");
            showErrorPage(webView, url, errorType, "加载失败", callback);
        }
    }
    
    /**
     * 重新加载页面
     */
    private void reloadPage(WebView webView, String url, ErrorCallback callback) {
        // 清理WebView缓存
        webView.clearCache(false);
        
        // 重新加载
        retryHandler.postDelayed(() -> {
            webView.reload();
        }, 500);
    }
    
    /**
     * 从缓存加载
     */
    private void loadFromCache(WebView webView, String url, ErrorCallback callback) {
        // 设置缓存模式为只使用缓存
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ONLY);
        webView.loadUrl(url);
        
        // 恢复正常缓存模式
        retryHandler.postDelayed(() -> {
            webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        }, 1000);
    }
    
    /**
     * 加载备用URL
     */
    private void loadFallbackUrl(WebView webView, String url, ErrorCallback callback) {
        // 尝试不同的协议或编码
        String fallbackUrl = url;
        
        if (url.startsWith("https://")) {
            // 尝试HTTP
            fallbackUrl = url.replace("https://", "http://");
        } else if (!url.contains("://")) {
            // 添加协议
            fallbackUrl = "http://" + url;
        }
        
        webView.loadUrl(fallbackUrl);
    }
    
    /**
     * 显示错误页面
     */
    private void showErrorPage(WebView webView, String url, ErrorType errorType,
                              String description, ErrorCallback callback) {
        String errorHtml = generateErrorHtml(errorType, description, url);
        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
        callback.onErrorPersists(url, errorType, description);
    }
    
    /**
     * 生成错误页面HTML
     */
    private String generateErrorHtml(ErrorType errorType, String description, String url) {
        String title = getErrorTitle(errorType);
        String message = getErrorMessage(errorType, description);
        String suggestions = getErrorSuggestions(errorType);
        
        return "<!DOCTYPE html>" +
            "<html lang='zh-CN'>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>" + title + "</title>" +
            "<style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
            "text-align: center; padding: 50px 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
            "color: white; margin: 0; min-height: 100vh; display: flex; flex-direction: column; justify-content: center; }" +
            ".container { background: rgba(255, 255, 255, 0.95); border-radius: 20px; padding: 40px; " +
            "max-width: 500px; margin: 0 auto; box-shadow: 0 20px 60px rgba(0,0,0,0.3); color: #333; }" +
            ".icon { font-size: 72px; margin-bottom: 20px; }" +
            "h1 { font-size: 24px; margin: 20px 0; color: #333; }" +
            "p { font-size: 16px; line-height: 1.6; color: #666; margin: 15px 0; }" +
            ".suggestions { text-align: left; background: #f8f9fa; border-radius: 10px; padding: 20px; margin: 20px 0; }" +
            ".suggestions h3 { font-size: 16px; margin-bottom: 10px; color: #333; }" +
            ".suggestions ul { margin: 0; padding-left: 20px; }" +
            ".suggestions li { margin: 8px 0; color: #666; }" +
            ".buttons { margin-top: 30px; }" +
            ".btn { display: inline-block; padding: 12px 30px; margin: 0 10px; background: #667eea; " +
            "color: white; text-decoration: none; border-radius: 25px; font-size: 14px; " +
            "transition: all 0.3s; border: none; cursor: pointer; }" +
            ".btn:hover { background: #5a67d8; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4); }" +
            ".btn-secondary { background: #48bb78; }" +
            ".btn-secondary:hover { background: #38a169; }" +
            ".error-code { font-size: 12px; color: #999; margin-top: 20px; font-family: monospace; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='container'>" +
            "<div class='icon'>" + getErrorIcon(errorType) + "</div>" +
            "<h1>" + title + "</h1>" +
            "<p>" + message + "</p>" +
            "<div class='suggestions'>" +
            "<h3>建议尝试：</h3>" +
            "<ul>" + suggestions + "</ul>" +
            "</div>" +
            "<div class='buttons'>" +
            "<button class='btn' onclick='location.reload()'>重试</button>" +
            "<button class='btn btn-secondary' onclick='history.back()'>返回</button>" +
            "</div>" +
            "<p class='error-code'>错误类型: " + errorType + "</p>" +
            "</div>" +
            "</body>" +
            "</html>";
    }
    
    /**
     * 获取错误图标
     */
    private String getErrorIcon(ErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "📡";
            case SSL_ERROR:
                return "🔒";
            case TIMEOUT_ERROR:
                return "⏱️";
            case DNS_ERROR:
                return "🌐";
            case MEMORY_ERROR:
                return "💾";
            case PERMISSION_ERROR:
                return "🚫";
            default:
                return "⚠️";
        }
    }
    
    /**
     * 获取错误标题
     */
    private String getErrorTitle(ErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "无法连接到网络";
            case SSL_ERROR:
                return "安全连接失败";
            case TIMEOUT_ERROR:
                return "连接超时";
            case DNS_ERROR:
                return "无法找到服务器";
            case MEMORY_ERROR:
                return "内存不足";
            case PERMISSION_ERROR:
                return "权限被拒绝";
            case ENCODING_ERROR:
                return "编码错误";
            default:
                return "页面加载失败";
        }
    }
    
    /**
     * 获取错误消息
     */
    private String getErrorMessage(ErrorType errorType, String description) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "请检查您的网络连接是否正常，或稍后再试。";
            case SSL_ERROR:
                return "该网站的安全证书存在问题，可能不安全。";
            case TIMEOUT_ERROR:
                return "服务器响应时间过长，请稍后重试。";
            case DNS_ERROR:
                return "无法解析该网址，请检查网址是否正确。";
            case MEMORY_ERROR:
                return "设备内存不足，请关闭一些应用后重试。";
            case PERMISSION_ERROR:
                return "没有权限访问该资源。";
            case ENCODING_ERROR:
                return "页面编码存在问题，无法正确显示。";
            default:
                return description != null ? description : "发生了未知错误。";
        }
    }
    
    /**
     * 获取错误建议
     */
    private String getErrorSuggestions(ErrorType errorType) {
        StringBuilder suggestions = new StringBuilder();
        
        switch (errorType) {
            case NETWORK_ERROR:
                suggestions.append("<li>检查Wi-Fi或移动数据是否开启</li>");
                suggestions.append("<li>尝试访问其他网站确认网络状态</li>");
                suggestions.append("<li>重启路由器或切换网络</li>");
                break;
                
            case SSL_ERROR:
                suggestions.append("<li>检查设备时间是否正确</li>");
                suggestions.append("<li>避免在公共Wi-Fi下访问敏感网站</li>");
                suggestions.append("<li>尝试使用其他浏览器访问</li>");
                break;
                
            case TIMEOUT_ERROR:
                suggestions.append("<li>等待几分钟后重试</li>");
                suggestions.append("<li>检查网络速度</li>");
                suggestions.append("<li>尝试在网络较好的环境下访问</li>");
                break;
                
            case DNS_ERROR:
                suggestions.append("<li>检查网址拼写是否正确</li>");
                suggestions.append("<li>尝试使用IP地址直接访问</li>");
                suggestions.append("<li>更换DNS服务器</li>");
                break;
                
            default:
                suggestions.append("<li>刷新页面重试</li>");
                suggestions.append("<li>清除浏览器缓存</li>");
                suggestions.append("<li>检查网址是否正确</li>");
                break;
        }
        
        return suggestions.toString();
    }
    
    /**
     * 检查网络是否可用
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
    
    /**
     * 获取重试次数
     */
    private int getRetryCount(String url) {
        AtomicInteger count = retryCountMap.get(url);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 增加重试次数
     */
    private int incrementRetryCount(String url) {
        AtomicInteger count = retryCountMap.get(url);
        if (count == null) {
            count = new AtomicInteger(0);
            retryCountMap.put(url, count);
        }
        
        // 检查是否需要重置（超过一定时间后重置计数）
        Long lastRetryTime = lastRetryTimeMap.get(url);
        long currentTime = System.currentTimeMillis();
        
        if (lastRetryTime == null || currentTime - lastRetryTime > 60000) { // 60秒后重置
            count.set(0);
        }
        
        lastRetryTimeMap.put(url, currentTime);
        return count.incrementAndGet();
    }
    
    /**
     * 重置重试计数
     */
    public void resetRetryCount(String url) {
        retryCountMap.remove(url);
        lastRetryTimeMap.remove(url);
    }
    
    /**
     * 清理所有重试记录
     */
    public void clearAllRetryRecords() {
        retryCountMap.clear();
        lastRetryTimeMap.clear();
    }
}