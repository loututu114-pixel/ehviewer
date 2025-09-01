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

    // YouTube访问失败计数器
    private int youtubeFailureCount = 0;
    private int currentYoutubeUAIndex = 0;

    // YouTube专用User-Agent轮换列表
    private final String[] youtubeUserAgents = {
        UA_YOUTUBE_CHROME,
        UA_YOUTUBE_FIREFOX,
        UA_YOUTUBE_EDGE,
        UA_CHROME_MAC,
        UA_CHROME_LINUX,
        UA_CHROME_DESKTOP
    };

    // 预定义的User-Agent字符串 - 增强版，包含更多真实设备信息
    public static final String UA_CHROME_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    public static final String UA_CHROME_MOBILE = "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    public static final String UA_FIREFOX_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0";
    public static final String UA_SAFARI_DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15";
    public static final String UA_EDGE_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";

    // YouTube专用User-Agent - 模拟真实用户访问
    public static final String UA_YOUTUBE_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    public static final String UA_YOUTUBE_FIREFOX = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0";
    public static final String UA_YOUTUBE_EDGE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0";

    // 备用User-Agent策略
    public static final String UA_CHROME_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    public static final String UA_CHROME_LINUX = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    // 百度专用User-Agent - 模拟真实百度用户访问模式
    public static final String UA_BAIDU_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    public static final String UA_BAIDU_MOBILE = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    public static final String UA_BAIDU_API = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // 百度增强版User-Agent - 更接近真实用户
    public static final String UA_BAIDU_REAL_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    public static final String UA_BAIDU_REAL_EDGE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
    public static final String UA_BAIDU_REAL_FIREFOX = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0";

    // 百度访问失败计数器和轮换策略
    private int baiduFailureCount = 0;
    private int currentBaiduUAIndex = 0;

    // 百度专用User-Agent轮换列表 - 增强版
    private final String[] baiduUserAgents = {
        UA_BAIDU_REAL_CHROME,
        UA_BAIDU_REAL_EDGE,
        UA_BAIDU_REAL_FIREFOX,
        UA_BAIDU_CHROME,
        UA_BAIDU_MOBILE,
        UA_BAIDU_API,
        UA_CHROME_MAC,
        UA_CHROME_LINUX,
        UA_EDGE_DESKTOP
    };

    // 网站特定的User-Agent映射
    private final Map<String, String> siteSpecificUAs = new HashMap<String, String>() {{
        // YouTube - 使用专门优化的UA避免403错误
        put("youtube.com", UA_YOUTUBE_CHROME);
        put("www.youtube.com", UA_YOUTUBE_CHROME);
        put("youtu.be", UA_YOUTUBE_CHROME);
        put("m.youtube.com", UA_YOUTUBE_CHROME);
        put("googlevideo.com", UA_YOUTUBE_CHROME);

        // Facebook - 移动版触控体验更好
        put("facebook.com", UA_CHROME_MOBILE);
        put("fb.com", UA_CHROME_MOBILE);
        put("m.facebook.com", UA_CHROME_MOBILE);

        // Twitter - 移动版针对触控优化
        put("twitter.com", UA_CHROME_MOBILE);
        put("x.com", UA_CHROME_MOBILE);
        put("mobile.twitter.com", UA_CHROME_MOBILE);

        // Instagram - 移动优先平台
        put("instagram.com", UA_CHROME_MOBILE);
        
        // 视频网站 - 使用桌面版以获得更好的视频支持
        put("xvideos.com", UA_CHROME_DESKTOP);
        put("www.xvideos.com", UA_CHROME_DESKTOP);
        put("pornhub.com", UA_CHROME_DESKTOP);
        put("www.pornhub.com", UA_CHROME_DESKTOP);
        put("redtube.com", UA_CHROME_DESKTOP);
        put("xhamster.com", UA_CHROME_DESKTOP);
        
        // 其他视频平台
        put("vimeo.com", UA_CHROME_DESKTOP);
        put("dailymotion.com", UA_CHROME_DESKTOP);

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

        // 新闻网站 - 移动版阅读体验更佳
        put("bbc.com", UA_CHROME_MOBILE);
        put("cnn.com", UA_CHROME_MOBILE);
        put("nytimes.com", UA_CHROME_MOBILE);
        put("reuters.com", UA_CHROME_MOBILE);

        // 电商网站 - 移动版购物体验优化
        put("amazon.com", UA_CHROME_MOBILE);
        put("ebay.com", UA_CHROME_MOBILE);
        put("alibaba.com", UA_CHROME_MOBILE);

        // 视频网站 - 需要桌面版避免移动版重定向
        put("vimeo.com", UA_CHROME_DESKTOP);
        put("twitch.tv", UA_CHROME_DESKTOP);
        put("netflix.com", UA_CHROME_DESKTOP);
        put("hulu.com", UA_CHROME_DESKTOP);

        // 成人网站 - 移动版（根据统计数据，移动设备占比80-90%）
        // 注意：xvideos.com, pornhub.com, xhamster.com 已在上面的视频网站部分定义为桌面版
        // put("pornhub.com", UA_CHROME_MOBILE); // 重复定义，已删除
        // put("xvideos.com", UA_CHROME_MOBILE); // 重复定义，已删除
        // put("xhamster.com", UA_CHROME_MOBILE); // 重复定义，已删除
        put("xnxx.com", UA_CHROME_MOBILE);
        put("onlyfans.com", UA_CHROME_MOBILE);
        put("xvideos.es", UA_CHROME_MOBILE);
        put("xhamsterlive.com", UA_CHROME_MOBILE);
        put("thisvid.com", UA_CHROME_MOBILE);
        put("redtube.com", UA_CHROME_MOBILE);
        put("xhamster19.com", UA_CHROME_MOBILE);
        put("theporndude.com", UA_CHROME_MOBILE);
        put("yandex.com.tr", UA_CHROME_MOBILE);
        put("deviantart.com", UA_CHROME_MOBILE);
        put("dlsite.com", UA_CHROME_MOBILE);

        // 搜索引擎 - 移动版（适配移动端浏览）
        put("google.com", UA_CHROME_MOBILE);
        put("bing.com", UA_CHROME_MOBILE);
        put("duckduckgo.com", UA_CHROME_MOBILE);
        put("yahoo.com", UA_CHROME_MOBILE);

        // 百度 - 专用策略（避免API访问被拒绝）
        put("baidu.com", UA_BAIDU_CHROME);
        put("www.baidu.com", UA_BAIDU_CHROME);
        put("m.baidu.com", UA_BAIDU_MOBILE);
        put("ext.baidu.com", UA_BAIDU_API);
        put("api.baidu.com", UA_BAIDU_API);
        put("rest.baidu.com", UA_BAIDU_API);
        put("sp0.baidu.com", UA_BAIDU_API);
        put("sp1.baidu.com", UA_BAIDU_API);
        put("sp2.baidu.com", UA_BAIDU_API);

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
        put("xhamster", "xhamster.com");
        put("xnxx", "xnxx.com");
        put("onlyfans", "onlyfans.com");
        put("redtube", "redtube.com");
        put("theporndude", "theporndude.com");
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
     * 最小化UA干预策略
     * 原则：不主动设置UA，只在极少数兼容性问题时进行必要调整
     */
    public void setSmartUserAgent(WebView webView, String url) {
        if (webView == null || url == null) return;

        String domain = extractDomain(url);
        WebSettings settings = webView.getSettings();

        // 优先使用系统默认UA
        settings.setUserAgentString(null);

        // 只对极少数已知问题网站进行特殊处理
        if (needsMinimalUAIntervention(domain)) {
            String specialUA = getMinimalSpecialUA(domain);
            if (specialUA != null) {
                settings.setUserAgentString(specialUA);
                Log.d(TAG, "Applied minimal UA intervention for " + domain + " (compatibility)");
            } else {
                Log.d(TAG, "Using system default UA for " + domain);
            }
        } else {
            Log.d(TAG, "Using system default UA for " + domain + " (recommended)");
        }

        // 移除UA相关的WebView优化设置，让网站自己处理适配
        // 网站应该根据真实的设备UA进行响应式设计
    }

    /**
     * 检查是否需要最小的UA干预（只对已知问题网站）
     */
    private boolean needsMinimalUAIntervention(String domain) {
        // 只对极少数经过验证有问题的网站进行UA调整
        return domain.equals("youtube.com") ||
               domain.equals("www.youtube.com") ||
               domain.equals("m.youtube.com") ||
               domain.contains("baidu.com") ||
               domain.equals("mp.weixin.qq.com"); // 微信公众号文章
    }

    /**
     * 获取最小的特殊UA调整
     */
    private String getMinimalSpecialUA(String domain) {
        if (domain.contains("youtube")) {
            // YouTube的移动端适配问题（如果有的话）
            return UA_CHROME_MOBILE;
        } else if (domain.contains("baidu")) {
            // 百度搜索的兼容性
            return UA_CHROME_MOBILE;
        } else if (domain.equals("mp.weixin.qq.com")) {
            // 微信公众号文章
            return UA_CHROME_MOBILE;
        }

        return null; // 不进行UA干预
    }
    
    /**
     * 简单的UA切换（仅在必要时使用，不自动重试）
     */
    public boolean switchUserAgentSimple(WebView webView, String url) {
        if (webView == null || url == null) return false;
        
        String domain = extractDomain(url);
        String currentUA = webView.getSettings().getUserAgentString();
        
        // 只做简单的Mobile/Desktop切换，不自动重新加载页面
        if (currentUA != null && currentUA.contains("Mobile")) {
            webView.getSettings().setUserAgentString(UA_CHROME_DESKTOP);
            Log.d(TAG, "Switched to desktop UA for: " + domain);
            return true;
        } else {
            webView.getSettings().setUserAgentString(UA_CHROME_MOBILE);
            Log.d(TAG, "Switched to mobile UA for: " + domain);
            return true;
        }
    }
    
    /**
     * 移动端优化设置
     * 重要：避免与EnhancedWebViewManager的连接稳定性设置冲突
     */
    private void optimizeForMobile(WebSettings settings) {
        try {
            // 移动端优化：启用缩放，优化文字大小
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);

            // 移动端通常需要更高的文字缩放
            settings.setTextZoom(110);

            // 启用自适应布局 - 仅在支持时设置，避免与连接稳定性设置冲突
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
            }

            Log.d(TAG, "Applied mobile optimizations");
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply mobile optimizations", e);
        }
    }
    
    /**
     * 桌面端优化设置
     * 重要：避免与EnhancedWebViewManager的连接稳定性设置冲突
     */
    private void optimizeForDesktop(WebSettings settings) {
        try {
            // 桌面端优化：更宽的视口
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);

            // 桌面端保持标准文字大小
            settings.setTextZoom(100);

            // 使用标准布局算法 - 仅在支持时设置，避免冲突
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
            }

            Log.d(TAG, "Applied desktop optimizations");
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply desktop optimizations", e);
        }
    }
    

    /**
     * 获取最优的User-Agent (简化版)
     */
    public String getOptimalUserAgent(String domain) {
        if (domain == null || domain.isEmpty()) {
            return getDefaultUserAgent();
        }

        // 解析域名别名
        String resolvedDomain = resolveDomainAlias(domain);

        // 只做精确匹配，不做复杂的子域名匹配
        // 这样避免干扰网站自身的跳转机制（如 www.taobao.com -> m.taobao.com）
        String siteSpecificUA = siteSpecificUAs.get(resolvedDomain);
        if (siteSpecificUA != null) {
            return siteSpecificUA;
        }

        // 简单的www子域名处理
        if (resolvedDomain.startsWith("www.")) {
            String mainDomain = resolvedDomain.substring(4);
            siteSpecificUA = siteSpecificUAs.get(mainDomain);
            if (siteSpecificUA != null) {
                return siteSpecificUA;
            }
        }

        // 默认使用移动版UA，让网站自己决定是否需要跳转
        return UA_CHROME_MOBILE;
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
                return UA_CHROME_MOBILE; // 默认移动版，适合手机浏览
        }
    }

    /**
     * 获取移动版用户代理
     */
    public String getMobileUserAgent() {
        return UA_CHROME_MOBILE;
    }

    /**
     * 获取桌面版用户代理
     */
    public String getDesktopUserAgent() {
        return UA_CHROME_DESKTOP;
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
     * 测试域名匹配（调试用）
     */
    public String testDomainMatching(String url) {
        String domain = extractDomain(url);
        String ua = getOptimalUserAgent(domain);
        String type = ua.contains("Mobile") ? "MOBILE" : "DESKTOP";
        
        Log.d(TAG, "TEST: " + url + " -> domain: " + domain + " -> UA: " + type);
        return domain + " -> " + type;
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

    /**
     * 处理YouTube访问失败，尝试下一个User-Agent
     */
    public String getNextYouTubeUserAgent() {
        youtubeFailureCount++;
        currentYoutubeUAIndex = (currentYoutubeUAIndex + 1) % youtubeUserAgents.length;

        String nextUA = youtubeUserAgents[currentYoutubeUAIndex];
        Log.d(TAG, "YouTube access failed " + youtubeFailureCount + " times, trying UA: " + getUserAgentType(nextUA));

        return nextUA;
    }

    /**
     * 重置YouTube失败计数器（访问成功时调用）
     */
    public void resetYouTubeFailureCount() {
        if (youtubeFailureCount > 0) {
            Log.d(TAG, "YouTube access successful after " + youtubeFailureCount + " failures");
            youtubeFailureCount = 0;
        }
    }

    /**
     * 检查是否应该继续重试YouTube访问
     */
    public boolean shouldRetryYouTube() {
        return youtubeFailureCount < youtubeUserAgents.length;
    }

    /**
     * 获取YouTube专用User-Agent（带轮换策略）
     */
    public String getYouTubeUserAgent() {
        if (youtubeFailureCount == 0) {
            return UA_YOUTUBE_CHROME; // 默认使用Chrome
        } else {
            return youtubeUserAgents[currentYoutubeUAIndex];
        }
    }

    /**
     * 获取下一个百度User-Agent（轮换策略）
     */
    public String getNextBaiduUserAgent() {
        baiduFailureCount++;
        currentBaiduUAIndex = (currentBaiduUAIndex + 1) % baiduUserAgents.length;

        String nextUA = baiduUserAgents[currentBaiduUAIndex];
        Log.d(TAG, "Baidu access failed " + baiduFailureCount + " times, trying UA: " + getUserAgentType(nextUA));

        return nextUA;
    }

    /**
     * 重置百度失败计数器（访问成功时调用）
     */
    public void resetBaiduFailureCount() {
        if (baiduFailureCount > 0) {
            Log.d(TAG, "Baidu access successful after " + baiduFailureCount + " failures");
            baiduFailureCount = 0;
        }
    }

    /**
     * 检查是否应该继续重试百度访问
     */
    public boolean shouldRetryBaidu() {
        return baiduFailureCount < baiduUserAgents.length;
    }

    /**
     * 获取百度专用User-Agent（带轮换策略）
     */
    public String getBaiduUserAgent() {
        if (baiduFailureCount == 0) {
            return UA_BAIDU_CHROME; // 默认使用Chrome
        } else {
            return baiduUserAgents[currentBaiduUAIndex];
        }
    }

    /**
     * 检查是否是百度相关的URL
     */
    public boolean isBaiduRelatedUrl(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("baidu.com") ||
               lowerUrl.contains("baidustatic.com") ||
               lowerUrl.contains("bdstatic.com") ||
               lowerUrl.contains("bdimg.com");
    }

    /**
     * 处理403错误的智能恢复
     */
    public String getRecoveryUserAgent(String failedUrl) {
        if (failedUrl.contains("youtube.com") || failedUrl.contains("googlevideo.com")) {
            return getNextYouTubeUserAgent();
        }

        if (isBaiduRelatedUrl(failedUrl)) {
            return getNextBaiduUserAgent();
        }

        // 对于其他网站，使用备用策略
        return UA_CHROME_MAC; // 使用Mac版Chrome作为备用
    }

    /**
     * 获取百度失败统计信息
     */
    public String getBaiduFailureStats() {
        return "Baidu failures: " + baiduFailureCount + ", current UA index: " + currentBaiduUAIndex;
    }

    /**
     * 检查是否是YouTube相关的URL
     */
    public boolean isYouTubeRelatedUrl(String url) {
        if (url == null) return false;

        return url.contains("youtube.com") ||
               url.contains("youtu.be") ||
               url.contains("googlevideo.com");
    }

    /**
     * 获取YouTube失败统计信息
     */
    public String getYouTubeFailureStats() {
        return "YouTube失败次数: " + youtubeFailureCount +
               ", 当前UA索引: " + currentYoutubeUAIndex +
               ", 当前UA: " + getUserAgentType(youtubeUserAgents[currentYoutubeUAIndex]);
    }
}
