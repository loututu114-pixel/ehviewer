package com.hippo.ehviewer.client;

import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.hippo.ehviewer.util.UserAgentManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 浏览器兼容性管理器 - 复刻主流浏览器解决方案
 *
 * 参考浏览器：
 * - 夸克浏览器 (Quark)
 * - UC浏览器 (UC Browser)
 * - 手机百度 (Mobile Baidu)
 * - 微信浏览器 (WeChat Browser)
 *
 * 核心策略：
 * 1. 网站指纹识别和分类
 * 2. 动态UA策略管理
 * 3. 请求头优化
 * 4. Cookie策略处理
 * 5. 错误恢复机制
 * 6. 性能优化配置
 *
 * @author EhViewer Team
 * @version 2.0.0
 */
public class BrowserCompatibilityManager {

    private static final String TAG = "BrowserCompat";

    private static volatile BrowserCompatibilityManager instance;
    private final Context context;
    private final UserAgentManager userAgentManager;

    // 网站类型枚举
    public enum WebsiteType {
        SEARCH_ENGINE,      // 搜索引擎
        VIDEO_PLATFORM,     // 视频平台
        SOCIAL_MEDIA,       // 社交媒体
        E_COMMERCE,         // 电商平台
        NEWS_PORTAL,        // 新闻门户
        BANKING_FINANCE,    // 银行金融
        GOVERNMENT,         // 政府网站
        EDUCATION,          // 教育网站
        FORUM_COMMUNITY,    // 论坛社区
        BLOG_PERSONAL,      // 博客个人
        CDN_STATIC,         // CDN静态资源
        API_SERVICE,        // API服务
        GAMING,            // 游戏网站
        ADULT_CONTENT,     // 成人内容
        STREAMING,         // 直播平台
        UNKNOWN            // 未知类型
    }

    // 网站指纹数据库 - 基于域名特征识别
    private final Map<String, WebsiteType> websiteFingerprints = new HashMap<String, WebsiteType>() {{
        // 搜索引擎
        put("baidu.com", WebsiteType.SEARCH_ENGINE);
        put("google.com", WebsiteType.SEARCH_ENGINE);
        put("bing.com", WebsiteType.SEARCH_ENGINE);
        put("sogou.com", WebsiteType.SEARCH_ENGINE);
        put("so.com", WebsiteType.SEARCH_ENGINE);
        put("duckduckgo.com", WebsiteType.SEARCH_ENGINE);

        // 视频平台
        put("youtube.com", WebsiteType.VIDEO_PLATFORM);
        put("bilibili.com", WebsiteType.VIDEO_PLATFORM);
        put("iqiyi.com", WebsiteType.VIDEO_PLATFORM);
        put("youku.com", WebsiteType.VIDEO_PLATFORM);
        put("tudou.com", WebsiteType.VIDEO_PLATFORM);
        put("vimeo.com", WebsiteType.VIDEO_PLATFORM);
        put("dailymotion.com", WebsiteType.VIDEO_PLATFORM);

        // 社交媒体
        put("weibo.com", WebsiteType.SOCIAL_MEDIA);
        put("zhihu.com", WebsiteType.SOCIAL_MEDIA);
        put("douban.com", WebsiteType.SOCIAL_MEDIA);
        put("tieba.baidu.com", WebsiteType.SOCIAL_MEDIA);
        put("twitter.com", WebsiteType.SOCIAL_MEDIA);
        put("facebook.com", WebsiteType.SOCIAL_MEDIA);
        put("instagram.com", WebsiteType.SOCIAL_MEDIA);
        put("linkedin.com", WebsiteType.SOCIAL_MEDIA);

        // 电商平台
        put("taobao.com", WebsiteType.E_COMMERCE);
        put("tmall.com", WebsiteType.E_COMMERCE);
        put("jd.com", WebsiteType.E_COMMERCE);
        put("suning.com", WebsiteType.E_COMMERCE);
        put("gome.com", WebsiteType.E_COMMERCE);
        put("amazon.com", WebsiteType.E_COMMERCE);
        put("ebay.com", WebsiteType.E_COMMERCE);

        // 新闻门户
        put("sina.com", WebsiteType.NEWS_PORTAL);
        put("sohu.com", WebsiteType.NEWS_PORTAL);
        put("163.com", WebsiteType.NEWS_PORTAL);
        put("qq.com", WebsiteType.NEWS_PORTAL);
        put("ifeng.com", WebsiteType.NEWS_PORTAL);
        put("xinhuanet.com", WebsiteType.NEWS_PORTAL);
        put("people.com.cn", WebsiteType.NEWS_PORTAL);

        // 银行金融
        put("icbc.com", WebsiteType.BANKING_FINANCE);
        put("ccb.com", WebsiteType.BANKING_FINANCE);
        put("boc.cn", WebsiteType.BANKING_FINANCE);
        put("abc.com", WebsiteType.BANKING_FINANCE);
        put("bankofchina.com", WebsiteType.BANKING_FINANCE);

        // 政府网站
        put("gov.cn", WebsiteType.GOVERNMENT);
        put("edu.cn", WebsiteType.GOVERNMENT);

        // 教育网站
        put("mooc.cn", WebsiteType.EDUCATION);
        put("icourse163.org", WebsiteType.EDUCATION);
        put("coursera.org", WebsiteType.EDUCATION);

        // 论坛社区
        put("reddit.com", WebsiteType.FORUM_COMMUNITY);
        put("discuz.net", WebsiteType.FORUM_COMMUNITY);
        put("phpwind.net", WebsiteType.FORUM_COMMUNITY);

        // CDN静态资源
        put("bdstatic.com", WebsiteType.CDN_STATIC);
        put("baidustatic.com", WebsiteType.CDN_STATIC);
        put("bdimg.com", WebsiteType.CDN_STATIC);
        put("alicdn.com", WebsiteType.CDN_STATIC);
        put("gtimg.cn", WebsiteType.CDN_STATIC);

        // API服务
        put("api.baidu.com", WebsiteType.API_SERVICE);
        put("openapi.baidu.com", WebsiteType.API_SERVICE);
        put("rest.baidu.com", WebsiteType.API_SERVICE);
        put("ext.baidu.com", WebsiteType.API_SERVICE);

        // 游戏网站
        put("4399.com", WebsiteType.GAMING);
        put("7k7k.com", WebsiteType.GAMING);
        put("17173.com", WebsiteType.GAMING);
        put("steamcommunity.com", WebsiteType.GAMING);

        // 成人内容
        put("xvideos.com", WebsiteType.ADULT_CONTENT);
        put("pornhub.com", WebsiteType.ADULT_CONTENT);
        put("xhamster.com", WebsiteType.ADULT_CONTENT);

        // 直播平台
        put("douyu.com", WebsiteType.STREAMING);
        put("huya.com", WebsiteType.STREAMING);
        put("twitch.tv", WebsiteType.STREAMING);
    }};

    // 兼容性配置数据库 - 针对不同网站类型的优化配置
    private final Map<WebsiteType, CompatibilityConfig> compatibilityConfigs = new HashMap<>();

    /**
     * 兼容性配置类
     */
    public static class CompatibilityConfig {
        public final String userAgent;
        public final Map<String, String> headers;
        public final WebSettingsConfig webSettings;
        public final boolean enableJavaScript;
        public final boolean enableCookies;
        public final boolean enableCache;
        public final int timeout;

        public CompatibilityConfig(String userAgent, Map<String, String> headers,
                                 WebSettingsConfig webSettings, boolean enableJavaScript,
                                 boolean enableCookies, boolean enableCache, int timeout) {
            this.userAgent = userAgent;
            this.headers = headers;
            this.webSettings = webSettings;
            this.enableJavaScript = enableJavaScript;
            this.enableCookies = enableCookies;
            this.enableCache = enableCache;
            this.timeout = timeout;
        }
    }

    /**
     * WebSettings配置类
     */
    public static class WebSettingsConfig {
        public final boolean loadImages;
        public final boolean enableZoom;
        public final boolean enableDatabase;
        public final boolean enableDomStorage;
        public final boolean enableAppCache;
        public final String cacheMode;

        public WebSettingsConfig(boolean loadImages, boolean enableZoom,
                               boolean enableDatabase, boolean enableDomStorage,
                               boolean enableAppCache, String cacheMode) {
            this.loadImages = loadImages;
            this.enableZoom = enableZoom;
            this.enableDatabase = enableDatabase;
            this.enableDomStorage = enableDomStorage;
            this.enableAppCache = enableAppCache;
            this.cacheMode = cacheMode;
        }
    }

    /**
     * 获取单例实例
     */
    public static BrowserCompatibilityManager getInstance(Context context) {
        if (instance == null) {
            synchronized (BrowserCompatibilityManager.class) {
                if (instance == null) {
                    instance = new BrowserCompatibilityManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private BrowserCompatibilityManager(Context context) {
        this.context = context;
        Log.d(TAG, "=== BROWSERCOMPAT: Constructor called");

        this.userAgentManager = new UserAgentManager(context);
        Log.d(TAG, "=== BROWSERCOMPAT: UserAgentManager initialized");

        initializeCompatibilityConfigs();
        Log.d(TAG, "=== BROWSERCOMPAT: Compatibility configs initialized");
        Log.d(TAG, "=== BROWSERCOMPAT: Total website fingerprints: " + websiteFingerprints.size());
        Log.d(TAG, "=== BROWSERCOMPAT: BrowserCompatibilityManager fully initialized");
    }

    /**
     * 初始化兼容性配置
     */
    private void initializeCompatibilityConfigs() {
        // 搜索引擎配置
        Map<String, String> searchHeaders = new HashMap<>();
        searchHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        searchHeaders.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        searchHeaders.put("DNT", "1");

        compatibilityConfigs.put(WebsiteType.SEARCH_ENGINE, new CompatibilityConfig(
            null, // 使用系统默认UA
            searchHeaders,
            new WebSettingsConfig(true, true, true, true, true, "LOAD_DEFAULT"),
            true, true, true, 10000
        ));

        // 视频平台配置
        Map<String, String> videoHeaders = new HashMap<>();
        videoHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        videoHeaders.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        videoHeaders.put("Sec-Fetch-Dest", "document");
        videoHeaders.put("Sec-Fetch-Mode", "navigate");
        videoHeaders.put("Sec-Fetch-Site", "none");

        compatibilityConfigs.put(WebsiteType.VIDEO_PLATFORM, new CompatibilityConfig(
            null, // 使用系统默认UA
            videoHeaders,
            new WebSettingsConfig(true, true, true, true, true, "LOAD_DEFAULT"),
            true, true, true, 15000
        ));

        // 电商平台配置
        Map<String, String> eCommerceHeaders = new HashMap<>();
        eCommerceHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        eCommerceHeaders.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        eCommerceHeaders.put("Cache-Control", "no-cache");

        compatibilityConfigs.put(WebsiteType.E_COMMERCE, new CompatibilityConfig(
            null, // 使用系统默认UA
            eCommerceHeaders,
            new WebSettingsConfig(true, true, true, true, false, "LOAD_NO_CACHE"),
            true, true, false, 8000
        ));

        // 银行金融配置 - 安全优先
        Map<String, String> bankingHeaders = new HashMap<>();
        bankingHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        bankingHeaders.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        bankingHeaders.put("Cache-Control", "no-cache");
        bankingHeaders.put("Pragma", "no-cache");

        compatibilityConfigs.put(WebsiteType.BANKING_FINANCE, new CompatibilityConfig(
            null, // 使用系统默认UA
            bankingHeaders,
            new WebSettingsConfig(true, false, false, false, false, "LOAD_NO_CACHE"),
            true, false, false, 12000
        ));

        // API服务配置
        Map<String, String> apiHeaders = new HashMap<>();
        apiHeaders.put("Accept", "application/json,text/plain,*/*");
        apiHeaders.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        apiHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        apiHeaders.put("X-Requested-With", "XMLHttpRequest");

        compatibilityConfigs.put(WebsiteType.API_SERVICE, new CompatibilityConfig(
            null, // 使用系统默认UA
            apiHeaders,
            new WebSettingsConfig(false, false, true, true, false, "LOAD_NO_CACHE"),
            true, true, false, 5000
        ));

        // CDN静态资源配置
        Map<String, String> cdnHeaders = new HashMap<>();
        cdnHeaders.put("Accept", "*/*");
        cdnHeaders.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        cdnHeaders.put("Cache-Control", "max-age=31536000");

        compatibilityConfigs.put(WebsiteType.CDN_STATIC, new CompatibilityConfig(
            null, // 使用系统默认UA
            cdnHeaders,
            new WebSettingsConfig(true, false, false, false, true, "LOAD_CACHE_ELSE_NETWORK"),
            false, false, true, 3000
        ));

        Log.d(TAG, "Compatibility configurations initialized for " + compatibilityConfigs.size() + " website types");
    }

    /**
     * 为WebView应用兼容性配置
     */
    public void applyCompatibilityConfig(WebView webView, String url) {
        if (webView == null || url == null || url.isEmpty()) {
            Log.w(TAG, "Invalid parameters for applyCompatibilityConfig");
            return;
        }

        WebsiteType websiteType = identifyWebsiteType(url);
        Log.d(TAG, "Applying compatibility config for " + url + " (type: " + websiteType + ")");

        CompatibilityConfig config = compatibilityConfigs.get(websiteType);
        if (config != null) {
            applyConfigToWebView(webView, config, url);
        } else {
            // 使用默认配置
            applyDefaultConfig(webView, url);
        }
    }

    /**
     * 识别网站类型
     */
    public WebsiteType identifyWebsiteType(String url) {
        if (url == null || url.isEmpty()) {
            return WebsiteType.UNKNOWN;
        }

        try {
            // 提取域名
            String domain = extractDomain(url);
            if (domain == null) {
                return WebsiteType.UNKNOWN;
            }

            // 直接匹配
            WebsiteType type = websiteFingerprints.get(domain);
            if (type != null) {
                return type;
            }

            // 模式匹配
            for (Map.Entry<String, WebsiteType> entry : websiteFingerprints.entrySet()) {
                if (domain.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }

            // 基于URL特征的智能识别
            return intelligentTypeDetection(url);

        } catch (Exception e) {
            Log.e(TAG, "Error identifying website type for: " + url, e);
            return WebsiteType.UNKNOWN;
        }
    }

    /**
     * 智能类型检测 - 基于URL特征
     */
    private WebsiteType intelligentTypeDetection(String url) {
        String lowerUrl = url.toLowerCase();

        // API检测
        if (lowerUrl.contains("/api/") || lowerUrl.contains("/rest/") ||
            lowerUrl.contains("/openapi/") || lowerUrl.contains("callback=")) {
            return WebsiteType.API_SERVICE;
        }

        // CDN检测
        if (lowerUrl.contains("cdn") || lowerUrl.contains("static") ||
            lowerUrl.matches(".*\\.(js|css|png|jpg|jpeg|gif|webp|ico|woff|woff2|ttf|eot)(\\?.*)?$")) {
            return WebsiteType.CDN_STATIC;
        }

        // 视频检测
        if (lowerUrl.contains("video") || lowerUrl.contains("player") ||
            lowerUrl.contains("youtube") || lowerUrl.contains("bilibili")) {
            return WebsiteType.VIDEO_PLATFORM;
        }

        // 搜索检测
        if (lowerUrl.contains("search") || lowerUrl.contains("query") ||
            lowerUrl.contains("?q=") || lowerUrl.contains("&q=")) {
            return WebsiteType.SEARCH_ENGINE;
        }

        return WebsiteType.UNKNOWN;
    }

    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                String withoutProtocol = url.substring(url.indexOf("://") + 3);
                int slashIndex = withoutProtocol.indexOf("/");
                String domain = slashIndex > 0 ? withoutProtocol.substring(0, slashIndex) : withoutProtocol;

                // 处理端口号
                int portIndex = domain.indexOf(":");
                if (portIndex > 0) {
                    domain = domain.substring(0, portIndex);
                }

                return domain.toLowerCase();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting domain from: " + url, e);
        }
        return null;
    }

    /**
     * 应用配置到WebView
     */
    private void applyConfigToWebView(WebView webView, CompatibilityConfig config, String url) {
        try {
            WebSettings settings = webView.getSettings();

            // 应用User-Agent
            if (config.userAgent != null && !config.userAgent.isEmpty()) {
                settings.setUserAgentString(config.userAgent);
                Log.d(TAG, "Applied UA: " + userAgentManager.getUserAgentType(config.userAgent));
            }

            // 应用WebSettings配置
            if (config.webSettings != null) {
                settings.setLoadsImagesAutomatically(config.webSettings.loadImages);
                settings.setSupportZoom(config.webSettings.enableZoom);
                settings.setDatabaseEnabled(config.webSettings.enableDatabase);
                settings.setDomStorageEnabled(config.webSettings.enableDomStorage);
                // 注意：setAppCacheEnabled在新版本Android中已移除，使用替代方案
                Log.d(TAG, "WebSettings config applied (AppCache deprecated)");
            }

            // JavaScript设置
            settings.setJavaScriptEnabled(config.enableJavaScript);

            // Cookie设置
            if (config.enableCookies) {
                android.webkit.CookieManager.getInstance().setAcceptCookie(true);
            }

            Log.d(TAG, "Compatibility config applied successfully for: " + url);

        } catch (Exception e) {
            Log.e(TAG, "Error applying compatibility config", e);
        }
    }

    /**
     * 应用默认配置
     */
    private void applyDefaultConfig(WebView webView, String url) {
        try {
            WebSettings settings = webView.getSettings();

            // 使用系统默认User-Agent，不进行UA伪造
            settings.setUserAgentString(null); // 系统默认UA

            // 默认WebSettings
            settings.setLoadsImagesAutomatically(true);
            settings.setSupportZoom(true);
            settings.setDatabaseEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setJavaScriptEnabled(true);

            Log.d(TAG, "Default compatibility config applied for: " + url);

        } catch (Exception e) {
            Log.e(TAG, "Error applying default config", e);
        }
    }

    /**
     * 获取网站类型的兼容性配置
     */
    public CompatibilityConfig getCompatibilityConfig(WebsiteType type) {
        return compatibilityConfigs.get(type);
    }

    /**
     * 添加自定义网站指纹
     */
    public void addWebsiteFingerprint(String domain, WebsiteType type) {
        websiteFingerprints.put(domain.toLowerCase(), type);
        Log.d(TAG, "Added custom fingerprint: " + domain + " -> " + type);
    }

    /**
     * 获取所有支持的网站类型
     */
    public Map<WebsiteType, Integer> getSupportedTypesCount() {
        Map<WebsiteType, Integer> counts = new HashMap<>();
        for (WebsiteType type : websiteFingerprints.values()) {
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }
        return counts;
    }

    /**
     * 获取兼容性统计信息
     */
    public String getCompatibilityStats() {
        Map<WebsiteType, Integer> counts = getSupportedTypesCount();
        StringBuilder stats = new StringBuilder();
        stats.append("Browser Compatibility Stats:\n");
        stats.append("Total fingerprints: ").append(websiteFingerprints.size()).append("\n");
        stats.append("Supported types: ").append(counts.size()).append("\n");

        for (Map.Entry<WebsiteType, Integer> entry : counts.entrySet()) {
            stats.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return stats.toString();
    }
}
