package com.hippo.ehviewer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 浏览器兼容性管理器
 * 统一管理PC/移动端适配、错误处理和用户体验优化
 */
public class BrowserCompatibilityManager {
    private static final String TAG = "BrowserCompatibility";
    
    private static BrowserCompatibilityManager instance;
    private final Context context;
    private final SharedPreferences preferences;
    private final MobileUrlAdapter urlAdapter;
    private final EnhancedErrorPageGenerator errorPageGenerator;
    private final UserAgentManager userAgentManager;
    
    // 统计信息
    private final Map<String, CompatibilityStats> domainStats;
    private final Set<String> problematicDomains;
    private final Map<String, Integer> errorStats;
    
    // 配置选项 - 调整为被动兼容模式
    private boolean autoMobileRedirect = false; // 默认关闭强制跳转
    private boolean enhancedErrorPages = true;  // 保留美化错误页面
    private boolean adaptiveUserAgent = true;   // 保留智能UA
    private int maxRetryAttempts = 2;
    
    private static final String PREFS_NAME = "browser_compatibility";
    
    private BrowserCompatibilityManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.urlAdapter = new MobileUrlAdapter();
        this.errorPageGenerator = new EnhancedErrorPageGenerator(context);
        this.userAgentManager = new UserAgentManager(context);
        this.domainStats = new HashMap<>();
        this.problematicDomains = new HashSet<>();
        this.errorStats = new HashMap<>();

        loadSettings();
        loadStatistics();
    }
    
    public static synchronized BrowserCompatibilityManager getInstance(Context context) {
        if (instance == null) {
            instance = new BrowserCompatibilityManager(context);
        }
        return instance;
    }
    
    /**
     * 为WebView应用兼容性配置
     */
    public void applyCompatibilityConfig(WebView webView, String url) {
        try {
            Log.d(TAG, "Applying compatibility config for: " + url);
            
            // 1. URL适配处理
            String adaptedUrl = processUrlForCompatibility(url);
            if (!adaptedUrl.equals(url)) {
                Log.d(TAG, "URL adapted: " + url + " -> " + adaptedUrl);
                webView.loadUrl(adaptedUrl);
                return; // 使用新URL加载，不继续处理原URL
            }
            
            // 2. 最小化UA干预（只对已知兼容性问题）
            // 不主动设置UA，让网站根据真实的设备信息适配
            if (adaptiveUserAgent) {
                userAgentManager.setSmartUserAgent(webView, url);
            }
            
            // 3. WebView客户端设置
            setupCompatibilityWebViewClient(webView);
            
            // 4. 记录访问统计
            recordDomainAccess(MobileUrlAdapter.extractDomain(url));
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying compatibility config", e);
        }
    }
    
    /**
     * URL兼容性处理 - 被动兼容模式
     * 只处理已知的问题情况，不主动改变用户访问意图
     * 重要：避免与桌面版访问冲突
     */
    private String processUrlForCompatibility(String originalUrl) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            return originalUrl;
        }

        String processedUrl = originalUrl;

        try {
            String domain = MobileUrlAdapter.extractDomain(processedUrl);

            // 1. 仅处理已知的问题域名（避免访问崩溃）
            if (problematicDomains.contains(domain)) {
                processedUrl = handleProblematicDomain(processedUrl, domain);
                Log.d(TAG, "Fixed problematic domain: " + domain);
            }

            // 2. 仅修复明显的WAP格式错误（不是用户主动选择的）
            if (needsWapFix(originalUrl)) {
                processedUrl = urlAdapter.adaptUrl(processedUrl, false);
                Log.d(TAG, "Fixed WAP format: " + originalUrl + " -> " + processedUrl);
            }

            // 3. 重要修改：不执行任何PC到移动端的强制转换
            // 让网站自己处理移动端适配，避免与用户桌面版访问意图冲突
            // 只有在明确需要时（如WAP修复）才进行URL转换

            return processedUrl;

        } catch (Exception e) {
            Log.e(TAG, "Error processing URL: " + originalUrl, e);
            return originalUrl; // 出错时返回原始URL
        }
    }
    
    /**
     * 检查是否需要修复WAP格式
     * 只修复明显错误的WAP格式，不影响用户主动访问WAP页面
     */
    private boolean needsWapFix(String url) {
        try {
            String domain = MobileUrlAdapter.extractDomain(url);
            
            // 只修复这些明显有问题的WAP格式
            return domain.startsWith("wap.baidu.") ||      // 百度WAP有问题
                   domain.equals("wap.sina.com.cn") ||     // 新浪WAP有问题
                   domain.startsWith("wap.163.");          // 网易WAP有问题
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 处理问题域名
     */
    private String handleProblematicDomain(String url, String domain) {
        Log.d(TAG, "Handling problematic domain: " + domain);
        
        // 根据已知的问题域名进行特殊处理
        switch (domain) {
            case "m.facebook.com":
                // Facebook移动版可能需要特殊处理
                return url.replace("m.facebook.com", "touch.facebook.com");
                
            case "m.twitter.com":
                // Twitter移动版问题处理
                return url.replace("m.twitter.com", "mobile.twitter.com");
                
            default:
                // 通用问题域名处理：尝试使用桌面版
                if (domain.startsWith("m.")) {
                    return url.replace(domain, domain.substring(2));
                }
                break;
        }
        
        return url;
    }
    
    /**
     * 设置兼容性WebViewClient
     */
    private void setupCompatibilityWebViewClient(WebView webView) {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                handleLoadingError(view, error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                handleLoadingError(view, errorCode, description, failingUrl);
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 完全禁用URL重定向 - 让网站自己处理移动端适配
                // 用户体验优先，不干预正常的页面跳转流程
                return super.shouldOverrideUrlLoading(view, request);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // 页面加载完成统计（仅统计，不干预）
                String domain = MobileUrlAdapter.extractDomain(url);
                recordSuccessfulLoad(domain);
                
                // 不再注入任何兼容性脚本 - 避免干扰页面正常功能
            }
        });
    }
    
    /**
     * 处理加载错误 - 最小干预原则
     * 优先保证用户体验，只在必要时才干预
     */
    private void handleLoadingError(WebView webView, int errorCode, String description, String failingUrl) {
        Log.d(TAG, "Loading error: " + errorCode + " - " + description + " for " + failingUrl);
        
        String domain = MobileUrlAdapter.extractDomain(failingUrl);
        recordLoadingError(domain, errorCode);
        
        // 1. 所有子资源错误都直接忽略，不做任何干预
        if (isSubResourceError(webView, failingUrl, errorCode)) {
            Log.d(TAG, "Sub-resource error ignored: " + failingUrl + " (error: " + errorCode + ")");
            return;
        }
        
        // 2. 检查是否是用户主动导航的页面
        String currentUrl = webView.getUrl();
        if (!isUserNavigationError(currentUrl, failingUrl)) {
            Log.d(TAG, "Non-navigation error ignored: " + failingUrl);
            return;
        }
        
        // 3. 只有在用户无法继续浏览时才显示错误页面
        if (isNavigationBlockingError(errorCode) && webView.getUrl() == null) {
            Log.d(TAG, "Critical navigation error, showing error page: " + failingUrl);
            
            if (enhancedErrorPages) {
                String errorPageHtml = errorPageGenerator.generateErrorPage(errorCode, description, failingUrl);
                webView.loadDataWithBaseURL("about:blank", errorPageHtml, "text/html", "UTF-8", failingUrl);
            }
        } else {
            Log.d(TAG, "Error logged but not blocking navigation: " + failingUrl);
        }
    }
    
    /**
     * 判断是否是子资源错误 - 采用宽泛策略，尽可能识别为子资源
     */
    private boolean isSubResourceError(WebView webView, String failingUrl, int errorCode) {
        try {
            String currentUrl = webView.getUrl();
            
            // 1. 如果有当前页面，且失败的URL不是当前页面URL，大概率是子资源
            if (currentUrl != null && !failingUrl.equals(currentUrl)) {
                return true;
            }
            
            // 2. HTTP状态错误（403, 404, 502等）通常是子资源问题
            if (errorCode == WebViewClient.ERROR_FILE_NOT_FOUND || 
                errorCode == WebViewClient.ERROR_IO ||
                errorCode == WebViewClient.ERROR_TOO_MANY_REQUESTS ||
                errorCode == WebViewClient.ERROR_UNSAFE_RESOURCE) {
                return true;
            }
            
            // 3. 明显的子资源文件类型
            String lowerUrl = failingUrl.toLowerCase();
            if (lowerUrl.matches(".*\\.(css|js|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot)($|\\?.*$)")) {
                return true;
            }
            
            // 4. API、AJAX请求路径
            if (lowerUrl.contains("/api/") || lowerUrl.contains("/ajax/") || 
                lowerUrl.contains("analytics") || lowerUrl.contains("tracking") ||
                lowerUrl.contains("ads") || lowerUrl.contains("beacon")) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking if sub-resource", e);
            return true; // 出错时假设是子资源，避免干扰用户体验
        }
    }
    
    /**
     * 判断是否是用户导航错误（区别于页面内的异步请求错误）
     */
    private boolean isUserNavigationError(String currentUrl, String failingUrl) {
        // 如果当前没有页面，说明这是导航错误
        if (currentUrl == null) {
            return true;
        }
        
        // 如果失败的URL就是当前URL，说明这是用户导航错误
        if (failingUrl.equals(currentUrl)) {
            return true;
        }
        
        // 如果是同一个页面的不同版本（如协议切换），认为是导航错误
        String currentDomain = MobileUrlAdapter.extractDomain(currentUrl);
        String failingDomain = MobileUrlAdapter.extractDomain(failingUrl);
        if (currentDomain.equals(failingDomain) && isSamePageDifferentVersion(currentUrl, failingUrl)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 判断是否会阻止用户导航的严重错误
     */
    private boolean isNavigationBlockingError(int errorCode) {
        switch (errorCode) {
            case WebViewClient.ERROR_HOST_LOOKUP:         // DNS解析失败
            case WebViewClient.ERROR_CONNECT:             // 连接失败
            case WebViewClient.ERROR_TIMEOUT:             // 连接超时
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 判断是否是同一页面的不同版本（如HTTPS/HTTP切换）
     */
    private boolean isSamePageDifferentVersion(String url1, String url2) {
        try {
            // 移除协议部分比较
            String normalized1 = url1.replaceFirst("^https?://", "");
            String normalized2 = url2.replaceFirst("^https?://", "");
            return normalized1.equals(normalized2);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 尝试错误恢复
     */
    private boolean attemptErrorRecovery(WebView webView, int errorCode, String failingUrl) {
        // 检查统计信息，决定是否尝试恢复
        String domain = MobileUrlAdapter.extractDomain(failingUrl);
        CompatibilityStats stats = domainStats.get(domain);
        
        if (stats != null && stats.consecutiveErrors < maxRetryAttempts) {
            // 根据错误类型尝试不同的恢复策略
            String recoveryUrl = getRecoveryUrl(failingUrl, errorCode);
            if (recoveryUrl != null && !recoveryUrl.equals(failingUrl)) {
                Log.d(TAG, "Trying recovery URL: " + recoveryUrl);
                webView.loadUrl(recoveryUrl);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取恢复URL
     */
    private String getRecoveryUrl(String originalUrl, int errorCode) {
        String domain = MobileUrlAdapter.extractDomain(originalUrl);
        
        switch (errorCode) {
            case WebViewClient.ERROR_HOST_LOOKUP:
            case WebViewClient.ERROR_CONNECT:
                // 尝试切换移动端/桌面端版本
                if (domain.startsWith("m.")) {
                    return originalUrl.replace(domain, domain.substring(2));
                } else if (domain.startsWith("www.")) {
                    return originalUrl.replace("www." + domain.substring(4), "m." + domain.substring(4));
                }
                break;
                
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                // 尝试HTTP版本
                if (originalUrl.startsWith("https://")) {
                    return originalUrl.replace("https://", "http://");
                }
                break;
        }
        
        return null;
    }
    
    /**
     * 注入兼容性脚本
     */
    private void injectCompatibilityScript(WebView webView, String url) {
        String domain = MobileUrlAdapter.extractDomain(url);
        
        // 根据不同网站注入特定的兼容性脚本
        String script = generateCompatibilityScript(domain);
        if (script != null && !script.isEmpty()) {
            webView.evaluateJavascript(script, result -> {
                Log.d(TAG, "Compatibility script injected for " + domain);
            });
        }
    }
    
    /**
     * 生成兼容性脚本
     */
    private String generateCompatibilityScript(String domain) {
        StringBuilder script = new StringBuilder();
        script.append("(function() {");
        
        // 通用移动端优化
        script.append("" +
            "var meta = document.createElement('meta');" +
            "meta.name = 'viewport';" +
            "meta.content = 'width=device-width, initial-scale=1.0';" +
            "if (!document.querySelector('meta[name=\"viewport\"]')) {" +
            "    document.head.appendChild(meta);" +
            "}");
        
        // 特定网站优化
        switch (domain) {
            case "m.facebook.com":
            case "touch.facebook.com":
                script.append("" +
                    "document.addEventListener('DOMContentLoaded', function() {" +
                    "    var images = document.querySelectorAll('img');" +
                    "    images.forEach(function(img) {" +
                    "        img.style.maxWidth = '100%';" +
                    "        img.style.height = 'auto';" +
                    "    });" +
                    "});");
                break;
                
            case "m.twitter.com":
            case "mobile.twitter.com":
                script.append("" +
                    "document.addEventListener('DOMContentLoaded', function() {" +
                    "    var videos = document.querySelectorAll('video');" +
                    "    videos.forEach(function(video) {" +
                    "        video.setAttribute('playsinline', 'true');" +
                    "    });" +
                    "});");
                break;
        }
        
        script.append("})();");
        return script.toString();
    }
    
    /**
     * 记录域名访问
     */
    private void recordDomainAccess(String domain) {
        CompatibilityStats stats = domainStats.computeIfAbsent(domain, k -> new CompatibilityStats());
        stats.totalAccess++;
        stats.lastAccess = System.currentTimeMillis();
    }
    
    /**
     * 记录成功加载
     */
    private void recordSuccessfulLoad(String domain) {
        CompatibilityStats stats = domainStats.computeIfAbsent(domain, k -> new CompatibilityStats());
        stats.successfulLoads++;
        stats.consecutiveErrors = 0; // 重置连续错误计数
    }
    
    /**
     * 记录加载错误
     */
    private void recordLoadingError(String domain, int errorCode) {
        CompatibilityStats stats = domainStats.computeIfAbsent(domain, k -> new CompatibilityStats());
        stats.errorCount++;
        stats.consecutiveErrors++;
        stats.lastErrorCode = errorCode;
        
        // 如果连续错误太多，标记为问题域名
        if (stats.consecutiveErrors >= 3) {
            problematicDomains.add(domain);
        }
    }
    
    /**
     * 获取兼容性统计信息
     */
    public String getCompatibilityStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Domain Stats: ").append(domainStats.size()).append(", ");
        stats.append("Problematic: ").append(problematicDomains.size());
        return stats.toString();
    }
    
    /**
     * 设置配置选项
     */
    public void setAutoMobileRedirect(boolean enabled) {
        this.autoMobileRedirect = enabled;
        preferences.edit().putBoolean("auto_mobile_redirect", enabled).apply();
    }
    
    public void setEnhancedErrorPages(boolean enabled) {
        this.enhancedErrorPages = enabled;
        preferences.edit().putBoolean("enhanced_error_pages", enabled).apply();
    }
    
    public void setAdaptiveUserAgent(boolean enabled) {
        this.adaptiveUserAgent = enabled;
        preferences.edit().putBoolean("adaptive_user_agent", enabled).apply();
    }
    
    /**
     * 加载设置
     */
    private void loadSettings() {
        autoMobileRedirect = preferences.getBoolean("auto_mobile_redirect", true);
        enhancedErrorPages = preferences.getBoolean("enhanced_error_pages", true);
        adaptiveUserAgent = preferences.getBoolean("adaptive_user_agent", true);
        maxRetryAttempts = preferences.getInt("max_retry_attempts", 3);
    }
    
    /**
     * 加载统计数据
     */
    private void loadStatistics() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("browser_compatibility_stats", Context.MODE_PRIVATE);
            
            // 加载错误统计
            errorStats.clear();
            Map<String, ?> allPrefs = prefs.getAll();
            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("error_") && entry.getValue() instanceof Integer) {
                    String url = key.substring(6); // 移除"error_"前缀
                    errorStats.put(url, (Integer) entry.getValue());
                }
            }
            
            Log.d(TAG, "Statistics loaded: " + errorStats.size() + " error records");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load statistics", e);
        }
    }
    
    /**
     * 保存统计数据
     */
    public void saveStatistics() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("browser_compatibility_stats", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // 清除旧数据
            editor.clear();
            
            // 保存错误统计（限制保存数量避免占用过多空间）
            int saveCount = 0;
            final int MAX_SAVE_COUNT = 100;
            for (Map.Entry<String, Integer> entry : errorStats.entrySet()) {
                if (saveCount >= MAX_SAVE_COUNT) break;
                editor.putInt("error_" + entry.getKey(), entry.getValue());
                saveCount++;
            }
            
            // 保存时间戳
            editor.putLong("last_save_time", System.currentTimeMillis());
            
            editor.apply();
            Log.d(TAG, "Statistics saved: " + saveCount + " records");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save statistics", e);
        }
    }
    
    /**
     * 兼容性统计数据类
     */
    private static class CompatibilityStats {
        int totalAccess = 0;
        int successfulLoads = 0;
        int errorCount = 0;
        int consecutiveErrors = 0;
        int lastErrorCode = 0;
        long lastAccess = 0;
        
        double getSuccessRate() {
            return totalAccess > 0 ? (double) successfulLoads / totalAccess : 0.0;
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        saveStatistics();
    }
}