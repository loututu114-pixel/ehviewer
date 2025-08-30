package com.hippo.ehviewer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能User-Agent管理器
 * 根据网站类型和用户偏好动态调整User-Agent
 */
public class UserAgentManager {
    private static final String TAG = "UserAgentManager";
    private static final String PREF_NAME = "user_agent_prefs";
    private static final String KEY_DEFAULT_MODE = "default_mode";

    private final Context context;
    private final SharedPreferences preferences;

    // 预定义的User-Agent字符串
    public static final String UA_CHROME_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    public static final String UA_CHROME_MOBILE = "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    public static final String UA_FIREFOX_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0";
    public static final String UA_SAFARI_DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15";
    public static final String UA_EDGE_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";

    // 网站特定的User-Agent映射
    private final Map<String, String> siteSpecificUAs = new HashMap<String, String>() {{
        // YouTube - 需要桌面版UA来避免重定向
        put("youtube.com", UA_CHROME_DESKTOP);
        put("youtu.be", UA_CHROME_DESKTOP);
        put("m.youtube.com", UA_CHROME_DESKTOP);

        // Facebook - 桌面版更好
        put("facebook.com", UA_CHROME_DESKTOP);
        put("fb.com", UA_CHROME_DESKTOP);
        put("m.facebook.com", UA_CHROME_DESKTOP);

        // Twitter - 桌面版体验更好
        put("twitter.com", UA_CHROME_DESKTOP);
        put("x.com", UA_CHROME_DESKTOP);
        put("mobile.twitter.com", UA_CHROME_DESKTOP);

        // Instagram - 桌面版
        put("instagram.com", UA_CHROME_DESKTOP);

        // LinkedIn - 桌面版
        put("linkedin.com", UA_CHROME_DESKTOP);

        // GitHub - 桌面版更好
        put("github.com", UA_CHROME_DESKTOP);

        // Stack Overflow - 桌面版
        put("stackoverflow.com", UA_CHROME_DESKTOP);

        // Reddit - 桌面版
        put("reddit.com", UA_CHROME_DESKTOP);

        // Wikipedia - 桌面版
        put("wikipedia.org", UA_CHROME_DESKTOP);

        // 新闻网站 - 桌面版
        put("bbc.com", UA_CHROME_DESKTOP);
        put("cnn.com", UA_CHROME_DESKTOP);
        put("nytimes.com", UA_CHROME_DESKTOP);
        put("reuters.com", UA_CHROME_DESKTOP);

        // 电商网站
        put("amazon.com", UA_CHROME_DESKTOP);
        put("ebay.com", UA_CHROME_DESKTOP);
        put("alibaba.com", UA_CHROME_DESKTOP);
        put("taobao.com", UA_CHROME_DESKTOP);

        // 视频网站 - 需要桌面版避免移动版重定向
        put("vimeo.com", UA_CHROME_DESKTOP);
        put("twitch.tv", UA_CHROME_DESKTOP);
        put("netflix.com", UA_CHROME_DESKTOP);
        put("hulu.com", UA_CHROME_DESKTOP);

        // 成人网站 - 桌面版
        put("pornhub.com", UA_CHROME_DESKTOP);
        put("xvideos.com", UA_CHROME_DESKTOP);
        put("xhamster.com", UA_CHROME_DESKTOP);

        // 搜索引擎 - 桌面版
        put("google.com", UA_CHROME_DESKTOP);
        put("bing.com", UA_CHROME_DESKTOP);
        put("duckduckgo.com", UA_CHROME_DESKTOP);
        put("yahoo.com", UA_CHROME_DESKTOP);

        // 百度 - 移动版（中国用户）
        put("baidu.com", UA_CHROME_MOBILE);
        put("m.baidu.com", UA_CHROME_MOBILE);

        // 微博 - 移动版
        put("weibo.com", UA_CHROME_MOBILE);
        put("m.weibo.com", UA_CHROME_MOBILE);

        // 知乎 - 移动版
        put("zhihu.com", UA_CHROME_MOBILE);

        // B站 - 移动版
        put("bilibili.com", UA_CHROME_MOBILE);
        put("m.bilibili.com", UA_CHROME_MOBILE);

        // 淘宝 - 移动版
        put("taobao.com", UA_CHROME_MOBILE);
        put("m.taobao.com", UA_CHROME_MOBILE);

        // 京东 - 移动版
        put("jd.com", UA_CHROME_MOBILE);
        put("m.jd.com", UA_CHROME_MOBILE);
    }};

    // 域名别名映射
    private final Map<String, String> domainAliases = new HashMap<String, String>() {{
        put("yt", "youtube.com");
        put("fb", "facebook.com");
        put("tw", "twitter.com");
        put("ig", "instagram.com");
        put("gh", "github.com");
        put("so", "stackoverflow.com");
        put("wiki", "wikipedia.org");
        put("bbc", "bbc.com");
        put("cnn", "cnn.com");
        put("amz", "amazon.com");
        put("ebay", "ebay.com");
        put("reddit", "reddit.com");
        put("linkedin", "linkedin.com");
        put("netflix", "netflix.com");
        put("twitch", "twitch.tv");
        put("vimeo", "vimeo.com");
        put("pornhub", "pornhub.com");
        put("xvideos", "xvideos.com");
        put("baidu", "baidu.com");
        put("weibo", "weibo.com");
        put("zhihu", "zhihu.com");
        put("bilibili", "bilibili.com");
        put("taobao", "taobao.com");
        put("jd", "jd.com");
    }};

    public enum UserAgentMode {
        AUTO,       // 智能模式 - 根据网站自动选择
        DESKTOP,    // 强制桌面版
        MOBILE,     // 强制移动版
        CUSTOM      // 自定义UA
    }

    public UserAgentManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 为WebView设置智能User-Agent
     */
    public void setSmartUserAgent(WebView webView, String url) {
        if (webView == null || url == null) return;

        String domain = extractDomain(url);
        String userAgent = getOptimalUserAgent(domain);

        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(userAgent);

        Log.d(TAG, "Set UA for " + domain + ": " + userAgent.substring(0, 50) + "...");
    }

    /**
     * 获取最优的User-Agent
     */
    public String getOptimalUserAgent(String domain) {
        if (domain == null || domain.isEmpty()) {
            return getDefaultUserAgent();
        }

        // 解析域名别名
        String resolvedDomain = resolveDomainAlias(domain);

        // 检查是否有网站特定的UA
        String siteSpecificUA = siteSpecificUAs.get(resolvedDomain);
        if (siteSpecificUA != null) {
            return siteSpecificUA;
        }

        // 根据域名后缀判断地区
        if (isChineseDomain(resolvedDomain)) {
            return UA_CHROME_MOBILE; // 中国网站默认移动版
        }

        // 默认使用桌面版（更好的体验）
        return UA_CHROME_DESKTOP;
    }

    /**
     * 解析域名别名
     */
    private String resolveDomainAlias(String input) {
        if (input == null) return null;

        // 首先尝试作为别名查找
        String resolved = domainAliases.get(input.toLowerCase());
        if (resolved != null) {
            return resolved;
        }

        return input.toLowerCase();
    }

    /**
     * 判断是否为中国域名
     */
    private boolean isChineseDomain(String domain) {
        if (domain == null) return false;

        // 中国顶级域名
        if (domain.endsWith(".cn") ||
            domain.endsWith(".com.cn") ||
            domain.endsWith(".org.cn") ||
            domain.endsWith(".net.cn")) {
            return true;
        }

        // 已知的中文网站
        String[] chineseSites = {
            "baidu.com", "weibo.com", "zhihu.com", "bilibili.com",
            "taobao.com", "jd.com", "tmall.com", "alibaba.com",
            "qq.com", "163.com", "sina.com", "sohu.com",
            "ifeng.com", "xinhuanet.com", "people.com.cn"
        };

        for (String site : chineseSites) {
            if (domain.contains(site)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 提取域名
     */
    public String extractDomain(String url) {
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
            Log.e(TAG, "Error extracting domain from URL: " + url, e);
            return null;
        }
    }

    /**
     * 获取默认User-Agent
     */
    public String getDefaultUserAgent() {
        UserAgentMode mode = getDefaultMode();
        switch (mode) {
            case DESKTOP:
                return UA_CHROME_DESKTOP;
            case MOBILE:
                return UA_CHROME_MOBILE;
            case AUTO:
            default:
                return UA_CHROME_DESKTOP; // 默认桌面版
        }
    }

    /**
     * 设置默认模式
     */
    public void setDefaultMode(UserAgentMode mode) {
        preferences.edit().putString(KEY_DEFAULT_MODE, mode.name()).apply();
    }

    /**
     * 获取默认模式
     */
    public UserAgentMode getDefaultMode() {
        String modeStr = preferences.getString(KEY_DEFAULT_MODE, UserAgentMode.AUTO.name());
        try {
            return UserAgentMode.valueOf(modeStr);
        } catch (Exception e) {
            return UserAgentMode.AUTO;
        }
    }

    /**
     * 添加网站特定的User-Agent
     */
    public void addSiteSpecificUA(String domain, String userAgent) {
        siteSpecificUAs.put(domain.toLowerCase(), userAgent);
    }

    /**
     * 移除网站特定的User-Agent
     */
    public void removeSiteSpecificUA(String domain) {
        siteSpecificUAs.remove(domain.toLowerCase());
    }

    /**
     * 获取网站特定的User-Agent
     */
    public String getSiteSpecificUA(String domain) {
        return siteSpecificUAs.get(domain.toLowerCase());
    }

    /**
     * 重置为默认设置
     */
    public void resetToDefaults() {
        preferences.edit().clear().apply();
    }

    /**
     * 获取所有支持的网站列表
     */
    public String[] getSupportedSites() {
        return siteSpecificUAs.keySet().toArray(new String[0]);
    }

    /**
     * 检查是否支持某个网站
     */
    public boolean isSiteSupported(String domain) {
        return siteSpecificUAs.containsKey(domain.toLowerCase());
    }

    /**
     * 获取UA模式描述
     */
    public static String getModeDescription(UserAgentMode mode) {
        switch (mode) {
            case AUTO:
                return "智能模式 - 根据网站自动选择最优UA";
            case DESKTOP:
                return "桌面模式 - 强制使用桌面版浏览器UA";
            case MOBILE:
                return "移动模式 - 强制使用移动版浏览器UA";
            case CUSTOM:
                return "自定义模式 - 使用用户自定义UA";
            default:
                return "未知模式";
        }
    }

    /**
     * 获取当前使用的UA类型
     */
    public String getUserAgentType(String userAgent) {
        if (userAgent == null) return "未知";

        if (userAgent.equals(UA_CHROME_DESKTOP)) {
            return "Chrome桌面版";
        } else if (userAgent.equals(UA_CHROME_MOBILE)) {
            return "Chrome移动版";
        } else if (userAgent.equals(UA_FIREFOX_DESKTOP)) {
            return "Firefox桌面版";
        } else if (userAgent.equals(UA_SAFARI_DESKTOP)) {
            return "Safari桌面版";
        } else if (userAgent.equals(UA_EDGE_DESKTOP)) {
            return "Edge桌面版";
        } else {
            return "自定义UA";
        }
    }
}
