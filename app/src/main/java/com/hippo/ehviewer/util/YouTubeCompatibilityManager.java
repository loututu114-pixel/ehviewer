package com.hippo.ehviewer.util;

import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.util.HashMap;
import java.util.Map;

/**
 * YouTube兼容性管理器
 * 处理YouTube等网站在WebView中的兼容性问题
 */
public class YouTubeCompatibilityManager {
    private static final String TAG = "YouTubeCompatibility";

    // 重定向循环检测
    private static final Map<String, Long> redirectHistory = new HashMap<>();
    private static final long REDIRECT_TIMEOUT = 10000; // 10秒内不重复处理

    // User-Agent常量
    private static final String DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String MOBILE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    // Firefox User-Agent (备选)
    private static final String FIREFOX_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0";

    // Safari User-Agent (备选)
    private static final String SAFARI_USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15";

    // 需要特殊处理的网站
    private static final String[] SPECIAL_SITES = {
        "youtube.com",
        "youtu.be",
        "vimeo.com",
        "twitch.tv",
        "netflix.com",
        "hulu.com",
        "pornhub.com",
        "xvideos.com",
        "facebook.com",
        "twitter.com",
        "instagram.com"
    };

    /**
     * 检查是否为需要特殊处理的网站
     */
    public static boolean isSpecialSite(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();
        for (String site : SPECIAL_SITES) {
            if (lowerUrl.contains(site)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 应用YouTube兼容性设置（集成UA管理器）
     */
    public static void applyYouTubeCompatibility(WebView webView, String url, UserAgentManager uaManager) {
        if (webView == null || url == null) return;

        // 检查重定向循环
        if (isRedirectLoop(url)) {
            Log.w(TAG, "Detected redirect loop for: " + url + ", skipping compatibility settings");
            return;
        }

        try {
            WebSettings settings = webView.getSettings();

            // 1. 使用智能UA管理器设置User-Agent
            if (uaManager != null) {
                uaManager.setSmartUserAgent(webView, url);
            } else {
                // 回退到默认桌面版UA
                settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            }

            // 2. 启用JavaScript（YouTube需要）
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);

            // 3. 设置DOM存储
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);

            // 4. 设置缓存策略
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);

            // 5. 设置媒体播放
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

            // 6. 设置视口和缩放
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);

            // 7. 设置编码
            settings.setDefaultTextEncodingName("UTF-8");

            // 8. 设置Cookie管理
            android.webkit.CookieManager.getInstance().setAcceptCookie(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
            }

            // 9. 设置其他兼容性选项
            settings.setAllowContentAccess(true);
            settings.setAllowFileAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);

            // 10. 记录处理历史
            recordRedirect(url);

            Log.d(TAG, "Applied YouTube compatibility settings successfully for: " + url);

        } catch (Exception e) {
            Log.e(TAG, "Error applying YouTube compatibility settings", e);
        }
    }

    /**
     * 检测重定向循环
     */
    private static boolean isRedirectLoop(String url) {
        if (url == null) return false;

        try {
            String domain = extractDomain(url);
            Long lastProcessed = redirectHistory.get(domain);

            if (lastProcessed != null) {
                long timeDiff = System.currentTimeMillis() - lastProcessed;
                if (timeDiff < REDIRECT_TIMEOUT) {
                    Log.w(TAG, "Redirect loop detected for domain: " + domain + " (time diff: " + timeDiff + "ms)");
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking redirect loop", e);
            return false;
        }
    }

    /**
     * 记录重定向处理
     */
    private static void recordRedirect(String url) {
        if (url == null) return;

        try {
            String domain = extractDomain(url);
            redirectHistory.put(domain, System.currentTimeMillis());

            // 清理过期的记录
            cleanupRedirectHistory();

        } catch (Exception e) {
            Log.e(TAG, "Error recording redirect", e);
        }
    }

    /**
     * 清理过期的重定向记录
     */
    private static void cleanupRedirectHistory() {
        try {
            long currentTime = System.currentTimeMillis();
            redirectHistory.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > REDIRECT_TIMEOUT * 2);
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning redirect history", e);
        }
    }

    /**
     * 提取域名
     */
    private static String extractDomain(String url) {
        if (url == null) return null;

        try {
            // 移除协议
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }

            // 移除路径和查询参数
            int slashIndex = url.indexOf('/');
            if (slashIndex > 0) {
                url = url.substring(0, slashIndex);
            }

            // 移除端口号
            int colonIndex = url.indexOf(':');
            if (colonIndex > 0) {
                url = url.substring(0, colonIndex);
            }

            return url.toLowerCase();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting domain", e);
            return url;
        }
    }

    /**
     * 清除重定向历史
     */
    public static void clearRedirectHistory() {
        redirectHistory.clear();
        Log.d(TAG, "Redirect history cleared");
    }

    /**
     * 获取重定向历史大小
     */
    public static int getRedirectHistorySize() {
        return redirectHistory.size();
    }

    /**
     * 应用Firefox User-Agent（备选方案）
     */
    public static void applyFirefoxUserAgent(WebView webView) {
        if (webView == null) return;

        try {
            webView.getSettings().setUserAgentString(FIREFOX_USER_AGENT);
            Log.d(TAG, "Applied Firefox User-Agent");
        } catch (Exception e) {
            Log.e(TAG, "Error applying Firefox User-Agent", e);
        }
    }

    /**
     * 应用Safari User-Agent（备选方案）
     */
    public static void applySafariUserAgent(WebView webView) {
        if (webView == null) return;

        try {
            webView.getSettings().setUserAgentString(SAFARI_USER_AGENT);
            Log.d(TAG, "Applied Safari User-Agent");
        } catch (Exception e) {
            Log.e(TAG, "Error applying Safari User-Agent", e);
        }
    }

    /**
     * 重置为默认User-Agent
     */
    public static void resetToDefaultUserAgent(WebView webView) {
        if (webView == null) return;

        try {
            webView.getSettings().setUserAgentString(MOBILE_USER_AGENT);
            Log.d(TAG, "Reset to default mobile User-Agent");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting User-Agent", e);
        }
    }

    /**
     * 获取当前User-Agent
     */
    public static String getCurrentUserAgent(WebView webView) {
        if (webView == null) return null;

        try {
            return webView.getSettings().getUserAgentString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting current User-Agent", e);
            return null;
        }
    }

    /**
     * 尝试不同的User-Agent策略
     */
    public static void tryDifferentUserAgents(WebView webView, String url) {
        if (webView == null || url == null) return;

        try {
            String currentUA = getCurrentUserAgent(webView);

            // 如果当前是移动版，切换到桌面版
            if (currentUA != null && currentUA.contains("Mobile")) {
                applyYouTubeCompatibility(webView, url, null);
                Log.d(TAG, "Switched from mobile to desktop UA for: " + url);
            }
            // 如果当前是桌面版，尝试Firefox
            else if (currentUA != null && currentUA.contains("Chrome")) {
                applyFirefoxUserAgent(webView);
                Log.d(TAG, "Switched to Firefox UA for: " + url);
            }
            // 最后尝试Safari
            else {
                applySafariUserAgent(webView);
                Log.d(TAG, "Switched to Safari UA for: " + url);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error trying different User-Agents", e);
        }
    }
}
