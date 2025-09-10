package com.hippo.ehviewer.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 移动端URL适配器
 * 处理PC到移动端的URL转换、WAP兼容性和智能重定向
 */
public class MobileUrlAdapter {
    private static final String TAG = "MobileUrlAdapter";
    
    // PC到移动端URL转换规则
    private static final Map<String, String> PC_TO_MOBILE_RULES = new HashMap<>();
    
    // WAP到标准URL转换规则
    private static final Map<String, String> WAP_TO_STANDARD_RULES = new HashMap<>();
    
    // 需要强制使用桌面版的网站
    private static final Map<String, Boolean> FORCE_DESKTOP_SITES = new HashMap<>();
    
    // URL模式匹配器
    private static final Map<Pattern, UrlConverter> URL_CONVERTERS = new HashMap<>();
    
    static {
        initializePcToMobileRules();
        initializeWapToStandardRules();
        initializeForceDesktopSites();
        initializePatternConverters();
    }
    
    /**
     * 初始化PC到移动端转换规则
     */
    private static void initializePcToMobileRules() {
        // 社交媒体
        PC_TO_MOBILE_RULES.put("www.facebook.com", "m.facebook.com");
        PC_TO_MOBILE_RULES.put("facebook.com", "m.facebook.com");
        PC_TO_MOBILE_RULES.put("www.twitter.com", "mobile.twitter.com");
        PC_TO_MOBILE_RULES.put("twitter.com", "mobile.twitter.com");
        PC_TO_MOBILE_RULES.put("x.com", "mobile.twitter.com");
        PC_TO_MOBILE_RULES.put("www.instagram.com", "instagram.com"); // Instagram已经响应式
        PC_TO_MOBILE_RULES.put("www.linkedin.com", "m.linkedin.com");
        PC_TO_MOBILE_RULES.put("linkedin.com", "m.linkedin.com");
        
        // 新闻网站
        PC_TO_MOBILE_RULES.put("www.bbc.com", "m.bbc.com");
        PC_TO_MOBILE_RULES.put("bbc.com", "m.bbc.com");
        PC_TO_MOBILE_RULES.put("www.cnn.com", "m.cnn.com");
        PC_TO_MOBILE_RULES.put("cnn.com", "m.cnn.com");
        PC_TO_MOBILE_RULES.put("www.reddit.com", "m.reddit.com");
        PC_TO_MOBILE_RULES.put("reddit.com", "m.reddit.com");
        
        // 购物网站
        PC_TO_MOBILE_RULES.put("www.amazon.com", "m.amazon.com");
        PC_TO_MOBILE_RULES.put("amazon.com", "m.amazon.com");
        PC_TO_MOBILE_RULES.put("www.ebay.com", "m.ebay.com");
        PC_TO_MOBILE_RULES.put("ebay.com", "m.ebay.com");
        
        // 中文网站
        PC_TO_MOBILE_RULES.put("www.baidu.com", "m.baidu.com");
        PC_TO_MOBILE_RULES.put("baidu.com", "m.baidu.com");
        PC_TO_MOBILE_RULES.put("www.weibo.com", "m.weibo.com");
        PC_TO_MOBILE_RULES.put("weibo.com", "m.weibo.com");
        PC_TO_MOBILE_RULES.put("www.zhihu.com", "www.zhihu.com"); // 知乎已经响应式
        PC_TO_MOBILE_RULES.put("www.taobao.com", "m.taobao.com");
        PC_TO_MOBILE_RULES.put("taobao.com", "m.taobao.com");
        PC_TO_MOBILE_RULES.put("www.jd.com", "m.jd.com");
        PC_TO_MOBILE_RULES.put("jd.com", "m.jd.com");
        PC_TO_MOBILE_RULES.put("www.bilibili.com", "m.bilibili.com");
        PC_TO_MOBILE_RULES.put("bilibili.com", "m.bilibili.com");
        
        // 视频网站
        PC_TO_MOBILE_RULES.put("www.youtube.com", "m.youtube.com");
        PC_TO_MOBILE_RULES.put("youtube.com", "m.youtube.com");
        PC_TO_MOBILE_RULES.put("www.vimeo.com", "vimeo.com"); // Vimeo已经响应式
        
        // 成人网站优化（移动端访问更好）
        PC_TO_MOBILE_RULES.put("www.pornhub.com", "pornhub.com"); // PornHub移动端友好
        PC_TO_MOBILE_RULES.put("www.xvideos.com", "xvideos.com"); // XVideos移动端友好
        PC_TO_MOBILE_RULES.put("www.xhamster.com", "m.xhamster.com");
        PC_TO_MOBILE_RULES.put("xhamster.com", "m.xhamster.com");
        PC_TO_MOBILE_RULES.put("www.redtube.com", "redtube.com"); // 移动端友好
    }
    
    /**
     * 初始化WAP到标准URL转换规则
     */
    private static void initializeWapToStandardRules() {
        // 移除常见的WAP前缀，转换为标准移动版
        WAP_TO_STANDARD_RULES.put("wap.", "m.");
        WAP_TO_STANDARD_RULES.put("3g.", "m.");
        WAP_TO_STANDARD_RULES.put("mobile.", "m.");
        WAP_TO_STANDARD_RULES.put("touch.", "m.");
        
        // 特殊网站的WAP处理
        WAP_TO_STANDARD_RULES.put("wap.baidu.com", "m.baidu.com");
        WAP_TO_STANDARD_RULES.put("wap.sina.com.cn", "sina.cn");
        WAP_TO_STANDARD_RULES.put("3g.163.com", "3g.163.com"); // 保持原样，网易3g版本很好
    }
    
    /**
     * 初始化需要强制使用桌面版的网站
     */
    private static void initializeForceDesktopSites() {
        // 办公类网站
        FORCE_DESKTOP_SITES.put("docs.google.com", true);
        FORCE_DESKTOP_SITES.put("sheets.google.com", true);
        FORCE_DESKTOP_SITES.put("slides.google.com", true);
        FORCE_DESKTOP_SITES.put("drive.google.com", true);
        FORCE_DESKTOP_SITES.put("office.com", true);
        FORCE_DESKTOP_SITES.put("outlook.com", true);
        
        // 开发者工具
        FORCE_DESKTOP_SITES.put("github.com", true);
        FORCE_DESKTOP_SITES.put("stackoverflow.com", true);
        FORCE_DESKTOP_SITES.put("developer.mozilla.org", true);
        FORCE_DESKTOP_SITES.put("codepen.io", true);
        
        // 管理后台
        FORCE_DESKTOP_SITES.put("wordpress.com", true); // 只对管理后台
        FORCE_DESKTOP_SITES.put("blogger.com", true);
    }
    
    /**
     * 初始化模式转换器
     */
    private static void initializePatternConverters() {
        // YouTube URL转换器
        URL_CONVERTERS.put(
            Pattern.compile("(https?://)?(www\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)(.*)"),
            new UrlConverter() {
                @Override
                public String convert(String url, Matcher matcher) {
                    String videoId = matcher.group(3);
                    String extraParams = matcher.group(4) != null ? matcher.group(4) : "";
                    return "https://m.youtube.com/watch?v=" + videoId + extraParams;
                }
            }
        );
        
        // Twitter/X URL转换器
        URL_CONVERTERS.put(
            Pattern.compile("(https?://)?(www\\.)?(twitter\\.com|x\\.com)/(.+)"),
            new UrlConverter() {
                @Override
                public String convert(String url, Matcher matcher) {
                    String path = matcher.group(4);
                    return "https://mobile.twitter.com/" + path;
                }
            }
        );
        
        // 百度搜索转换器
        URL_CONVERTERS.put(
            Pattern.compile("(https?://)?(www\\.)?baidu\\.com/s\\?(.+)"),
            new UrlConverter() {
                @Override
                public String convert(String url, Matcher matcher) {
                    String queryParams = matcher.group(3);
                    return "https://m.baidu.com/s?" + queryParams;
                }
            }
        );
        
        // Reddit URL转换器
        URL_CONVERTERS.put(
            Pattern.compile("(https?://)?(www\\.)?reddit\\.com/(.+)"),
            new UrlConverter() {
                @Override
                public String convert(String url, Matcher matcher) {
                    String path = matcher.group(3);
                    return "https://m.reddit.com/" + path;
                }
            }
        );
    }
    
    /**
     * 智能URL适配 - 主要入口方法
     */
    public static String adaptUrl(String originalUrl, boolean preferMobile) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            return originalUrl;
        }
        
        try {
            String processedUrl = originalUrl.trim();
            
            // 添加协议如果缺失
            if (!processedUrl.startsWith("http://") && !processedUrl.startsWith("https://")) {
                processedUrl = "https://" + processedUrl;
            }
            
            URL url = new URL(processedUrl);
            String host = url.getHost().toLowerCase();
            
            Log.d(TAG, "Adapting URL: " + originalUrl + " -> Host: " + host + ", PreferMobile: " + preferMobile);
            
            // 1. 检查是否是WAP地址，需要转换为标准格式
            String wapConverted = convertWapToStandard(processedUrl, host);
            if (!wapConverted.equals(processedUrl)) {
                Log.d(TAG, "WAP converted: " + processedUrl + " -> " + wapConverted);
                return wapConverted;
            }
            
            // 2. 检查是否需要强制桌面版
            if (FORCE_DESKTOP_SITES.containsKey(host)) {
                Log.d(TAG, "Force desktop site: " + host);
                return processedUrl; // 保持桌面版，不进行任何转换
            }
            
            // 3. 如果偏好移动版，尝试转换
            if (preferMobile) {
                String mobileConverted = convertToMobile(processedUrl, host);
                if (!mobileConverted.equals(processedUrl)) {
                    Log.d(TAG, "Mobile converted: " + processedUrl + " -> " + mobileConverted);
                    return mobileConverted;
                }
            }
            
            // 4. 使用模式匹配器进行高级转换
            String patternConverted = applyPatternConverters(processedUrl);
            if (!patternConverted.equals(processedUrl)) {
                Log.d(TAG, "Pattern converted: " + processedUrl + " -> " + patternConverted);
                return patternConverted;
            }
            
            return processedUrl;
            
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid URL: " + originalUrl, e);
            return originalUrl; // 返回原始URL
        }
    }
    
    /**
     * 转换WAP地址为标准格式
     */
    private static String convertWapToStandard(String url, String host) {
        // 检查是否是WAP地址
        for (Map.Entry<String, String> entry : WAP_TO_STANDARD_RULES.entrySet()) {
            String wapPrefix = entry.getKey();
            String replacement = entry.getValue();
            
            if (host.startsWith(wapPrefix)) {
                String newHost = host.replace(wapPrefix, replacement);
                String newUrl = url.replace(host, newHost);
                Log.d(TAG, "WAP conversion: " + wapPrefix + " -> " + replacement);
                return newUrl;
            }
        }
        
        // 通用WAP模式处理
        if (host.startsWith("wap.") || host.startsWith("3g.") || host.startsWith("mobile.")) {
            String[] parts = host.split("\\.", 2);
            if (parts.length == 2) {
                String domain = parts[1];
                // 转换为移动版或保持原域名（如果移动版不存在）
                String newHost = "m." + domain;
                String newUrl = url.replace(host, newHost);
                Log.d(TAG, "Generic WAP conversion: " + host + " -> " + newHost);
                return newUrl;
            }
        }
        
        return url;
    }
    
    /**
     * 转换为移动端URL
     */
    private static String convertToMobile(String url, String host) {
        // 直接匹配规则
        if (PC_TO_MOBILE_RULES.containsKey(host)) {
            String mobileHost = PC_TO_MOBILE_RULES.get(host);
            String newUrl = url.replace(host, mobileHost);
            Log.d(TAG, "Direct mobile conversion: " + host + " -> " + mobileHost);
            return newUrl;
        }
        
        // 如果是www.开头，尝试转换为m.
        if (host.startsWith("www.")) {
            String domain = host.substring(4);
            String mobileHost = "m." + domain;
            String newUrl = url.replace(host, mobileHost);
            Log.d(TAG, "WWW to mobile conversion: " + host + " -> " + mobileHost);
            return newUrl;
        }
        
        return url;
    }
    
    /**
     * 应用模式转换器
     */
    private static String applyPatternConverters(String url) {
        for (Map.Entry<Pattern, UrlConverter> entry : URL_CONVERTERS.entrySet()) {
            Pattern pattern = entry.getKey();
            UrlConverter converter = entry.getValue();
            
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches()) {
                String converted = converter.convert(url, matcher);
                Log.d(TAG, "Pattern conversion applied: " + url + " -> " + converted);
                return converted;
            }
        }
        
        return url;
    }
    
    /**
     * 检查URL是否应该使用移动端版本
     */
    public static boolean shouldUseMobile(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true; // 默认偏好移动端
        }
        
        try {
            URL urlObj = new URL(url.startsWith("http") ? url : "https://" + url);
            String host = urlObj.getHost().toLowerCase();
            
            // 强制桌面版的不使用移动端
            if (FORCE_DESKTOP_SITES.containsKey(host)) {
                return false;
            }
            
            // 已经是移动版的保持移动版
            if (host.startsWith("m.") || host.startsWith("mobile.")) {
                return true;
            }
            
            // 有移动版转换规则的建议使用移动版
            if (PC_TO_MOBILE_RULES.containsKey(host)) {
                return true;
            }
            
            return true; // 默认偏好移动端
            
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid URL for mobile check: " + url, e);
            return true; // 错误时默认移动端
        }
    }
    
    /**
     * 获取URL的建议用户代理类型
     */
    public static String getSuggestedUserAgentType(String url) {
        if (!shouldUseMobile(url)) {
            return "desktop";
        }
        return "mobile";
    }
    
    /**
     * URL转换器接口
     */
    interface UrlConverter {
        String convert(String originalUrl, Matcher matcher);
    }
    
    /**
     * 验证URL是否可访问
     */
    public static boolean isUrlValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            String processedUrl = url.trim();
            if (!processedUrl.startsWith("http://") && !processedUrl.startsWith("https://")) {
                processedUrl = "https://" + processedUrl;
            }
            
            new URL(processedUrl);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    /**
     * 获取域名
     */
    public static String extractDomain(String url) {
        try {
            String processedUrl = url.trim();
            if (!processedUrl.startsWith("http://") && !processedUrl.startsWith("https://")) {
                processedUrl = "https://" + processedUrl;
            }
            
            URL urlObj = new URL(processedUrl);
            return urlObj.getHost().toLowerCase();
        } catch (MalformedURLException e) {
            Log.e(TAG, "Cannot extract domain from: " + url, e);
            return "";
        }
    }
}