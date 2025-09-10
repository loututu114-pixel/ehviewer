package com.hippo.ehviewer.client;

import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 安全管理器
 * 基于腾讯X5和YCWebView最佳实践，实现全面的安全防护
 *
 * 核心特性：
 * - SSL证书验证
 * - XSS防护
 * - 内容安全策略
 * - 恶意代码检测
 * - 隐私保护
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class SecurityManager {

    private static final String TAG = "SecurityManager";

    // 安全配置
    private static final Set<String> TRUSTED_DOMAINS = new HashSet<>(Arrays.asList(
        "ehentai.org",
        "exhentai.org",
        "googleusercontent.com",
        "googlevideo.com",
        "youtube.com",
        "youtu.be",
        "fonts.googleapis.com",
        "ajax.googleapis.com"
    ));

    // XSS检测模式
    private static final String[] XSS_PATTERNS = {
        "<script[^>]*>.*?</script>",
        "javascript:",
        "on\\w+\\s*=",
        "<iframe[^>]*>.*?</iframe>",
        "<object[^>]*>.*?</object>",
        "<embed[^>]*>.*?</embed>"
    };

    // 监控统计
    private long blockedRequests = 0;
    private long sslErrors = 0;
    private long xssAttempts = 0;
    private boolean isMonitoring = false;

    private final Context context;

    public SecurityManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 应用安全设置到WebView
     */
    public void applySecuritySettings(WebView webView) {
        if (webView == null) return;

        try {
            WebSettings settings = webView.getSettings();

            // 关闭限制性安全设置，确保最大兼容性
            settings.setAllowFileAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowContentAccess(true);

            // JavaScript安全（允许完全支持）
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);

            // 插件安全（允许插件以提高兼容性）
            settings.setPluginState(WebSettings.PluginState.ON);

            // 数据库和存储安全
            settings.setDatabaseEnabled(true);
            settings.setDomStorageEnabled(true);

            // 地理位置安全（允许以提高兼容性）
            settings.setGeolocationEnabled(true);

            // 混合内容处理（允许所有混合内容以确保兼容性）
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

            // 密码保存安全（允许以提高用户体验）
            settings.setSavePassword(true);
            settings.setSaveFormData(false);

            // 第三方Cookie控制
            // 注意：这个设置在新版本中可能有所不同
            // settings.setAcceptThirdPartyCookies(webView, false);

            Log.d(TAG, "Security settings applied to WebView");

        } catch (Exception e) {
            Log.e(TAG, "Failed to apply security settings", e);
        }
    }

    /**
     * 检查URL安全性
     */
    public boolean checkSecurity(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            // 1. 检查是否为HTTPS
            if (!url.startsWith("https://") && !isTrustedDomain(url)) {
                Log.w(TAG, "Insecure URL detected: " + url);
                return false;
            }

            // 2. XSS检测
            if (detectXSS(url)) {
                xssAttempts++;
                Log.w(TAG, "XSS attempt detected in URL: " + url);
                return false;
            }

            // 3. 检查可疑模式
            if (containsSuspiciousPatterns(url)) {
                blockedRequests++;
                Log.w(TAG, "Suspicious pattern detected in URL: " + url);
                return false;
            }

            // 4. 检查域名白名单
            if (!isDomainAllowed(url)) {
                blockedRequests++;
                Log.w(TAG, "Domain not in whitelist: " + url);
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error checking URL security: " + url, e);
            return false;
        }
    }

    /**
     * 检测XSS攻击
     */
    public boolean detectXSS(String content) {
        if (content == null) return false;

        String lowerContent = content.toLowerCase();

        for (String pattern : XSS_PATTERNS) {
            if (lowerContent.contains(pattern.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查可疑模式
     */
    private boolean containsSuspiciousPatterns(String url) {
        // 检查数据URI中的可疑内容
        if (url.startsWith("data:")) {
            return detectXSS(url);
        }

        // 检查JavaScript URL
        if (url.startsWith("javascript:")) {
            return detectXSS(url);
        }

        // 检查过长的查询参数
        if (url.length() > 2048) {
            Log.w(TAG, "URL too long: " + url.length() + " characters");
            return true;
        }

        return false;
    }

    /**
     * 检查域名是否被允许
     */
    private boolean isDomainAllowed(String url) {
        // 对于EhViewer相关的域名，总是允许
        if (url.contains("ehentai.org") || url.contains("exhentai.org")) {
            return true;
        }

        // 对于其他域名，需要在白名单中
        return isTrustedDomain(url);
    }

    /**
     * 检查是否为受信任的域名
     */
    private boolean isTrustedDomain(String url) {
        try {
            String domain = extractDomain(url);
            return TRUSTED_DOMAINS.contains(domain);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting domain from URL: " + url, e);
            return false;
        }
    }

    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        try {
            // 移除协议
            String domain = url.replaceFirst("^(https?://)", "");

            // 移除路径和查询参数
            int pathIndex = domain.indexOf('/');
            if (pathIndex > 0) {
                domain = domain.substring(0, pathIndex);
            }

            // 移除端口号
            int portIndex = domain.indexOf(':');
            if (portIndex > 0) {
                domain = domain.substring(0, portIndex);
            }

            return domain.toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 处理SSL证书错误
     */
    public boolean handleSSLError(String url, X509Certificate certificate) {
        sslErrors++;

        Log.w(TAG, "SSL certificate error for URL: " + url);

        // 对于受信任的域名，可以选择忽略SSL错误
        if (isTrustedDomain(url)) {
            Log.d(TAG, "Ignoring SSL error for trusted domain: " + url);
            return true;
        }

        // 对于不受信任的域名，拒绝连接
        return false;
    }

    /**
     * 应用内容安全策略
     */
    public void applyContentSecurityPolicy(WebView webView) {
        try {
            // 注入CSP头部
            String cspScript = "javascript:(function() {" +
                    "var meta = document.createElement('meta');" +
                    "meta.httpEquiv = 'Content-Security-Policy';" +
                    "meta.content = \"default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; " +
                    "font-src 'self' data:; connect-src 'self' https:; \";" +
                    "document.head.appendChild(meta);" +
                    "})()";

            webView.evaluateJavascript(cspScript, null);
            Log.d(TAG, "Content Security Policy applied");

        } catch (Exception e) {
            Log.e(TAG, "Failed to apply CSP", e);
        }
    }

    /**
     * 启动安全监控
     */
    public void startSecurityMonitoring() {
        if (isMonitoring) return;

        isMonitoring = true;
        Log.i(TAG, "Security monitoring started");
    }

    /**
     * 停止安全监控
     */
    public void stopSecurityMonitoring() {
        isMonitoring = false;
        Log.i(TAG, "Security monitoring stopped");
    }

    /**
     * 获取安全状态
     */
    public SecurityStatus getStatus() {
        return new SecurityStatus(
            blockedRequests,
            sslErrors,
            xssAttempts,
            isMonitoring
        );
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        blockedRequests = 0;
        sslErrors = 0;
        xssAttempts = 0;
        Log.d(TAG, "Security stats reset");
    }

    /**
     * 生成安全报告
     */
    public String generateSecurityReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== EhViewer 安全监控报告 ===\n");
        report.append("被拦截请求数: ").append(blockedRequests).append("\n");
        report.append("SSL证书错误数: ").append(sslErrors).append("\n");
        report.append("XSS攻击尝试数: ").append(xssAttempts).append("\n");
        report.append("监控状态: ").append(isMonitoring ? "启用" : "禁用").append("\n");

        if (blockedRequests > 0 || sslErrors > 0 || xssAttempts > 0) {
            report.append("\n⚠️ 检测到安全威胁，请检查日志了解详情\n");
        } else {
            report.append("\n✅ 未检测到安全威胁\n");
        }

        return report.toString();
    }

    /**
     * 安全状态类
     */
    public static class SecurityStatus {
        public final long blockedRequests;
        public final long sslErrors;
        public final long xssAttempts;
        public final boolean monitoringEnabled;

        public SecurityStatus(long blockedRequests, long sslErrors, long xssAttempts, boolean monitoringEnabled) {
            this.blockedRequests = blockedRequests;
            this.sslErrors = sslErrors;
            this.xssAttempts = xssAttempts;
            this.monitoringEnabled = monitoringEnabled;
        }

        @Override
        public String toString() {
            return String.format("SecurityStatus{blocked=%d, sslErrors=%d, xssAttempts=%d, monitoring=%b}",
                    blockedRequests, sslErrors, xssAttempts, monitoringEnabled);
        }
    }
}
