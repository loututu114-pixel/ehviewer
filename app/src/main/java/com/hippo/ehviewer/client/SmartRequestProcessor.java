package com.hippo.ehviewer.client;

import android.content.Context;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hippo.ehviewer.client.BrowserCompatibilityManager.WebsiteType;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能请求处理器 - 复刻主流浏览器的请求处理机制
 *
 * 处理策略：
 * 1. 请求拦截和修改
 * 2. 响应头处理
 * 3. 资源加载优化
 * 4. 安全策略执行
 * 5. 隐私保护
 * 6. 广告过滤
 *
 * @author EhViewer Team
 * @version 1.0.0
 */
public class SmartRequestProcessor {

    private static final String TAG = "SmartRequestProcessor";

    private final Context context;
    private final BrowserCompatibilityManager compatibilityManager;
    private final AdBlockManager adBlockManager;

    // 请求统计
    private final Map<String, Integer> requestStats = new HashMap<>();
    private final Map<String, Long> requestTimestamps = new HashMap<>();

    // 特殊处理规则
    private final Map<String, RequestRule> requestRules = new HashMap<>();

    /**
     * 请求规则类
     */
    public static class RequestRule {
        public final boolean allow;
        public final boolean modifyHeaders;
        public final Map<String, String> additionalHeaders;
        public final boolean blockIfMatch;

        public RequestRule(boolean allow, boolean modifyHeaders,
                          Map<String, String> additionalHeaders, boolean blockIfMatch) {
            this.allow = allow;
            this.modifyHeaders = modifyHeaders;
            this.additionalHeaders = additionalHeaders;
            this.blockIfMatch = blockIfMatch;
        }
    }

    public SmartRequestProcessor(Context context) {
        Log.d(TAG, "=== SMARTREQUEST: Constructor called");
        this.context = context;
        this.compatibilityManager = BrowserCompatibilityManager.getInstance(context);
        Log.d(TAG, "=== SMARTREQUEST: CompatibilityManager obtained");

        this.adBlockManager = AdBlockManager.getInstance();
        Log.d(TAG, "=== SMARTREQUEST: AdBlockManager obtained");

        initializeRequestRules();
        Log.d(TAG, "=== SMARTREQUEST: Request rules initialized: " + requestRules.size());
        Log.d(TAG, "=== SMARTREQUEST: SmartRequestProcessor fully initialized");
    }

    /**
     * 初始化请求规则
     */
    private void initializeRequestRules() {
        // 百度相关规则
        Map<String, String> baiduHeaders = new HashMap<>();
        baiduHeaders.put("Referer", "https://www.baidu.com/");
        baiduHeaders.put("Sec-Fetch-Site", "same-origin");
        baiduHeaders.put("Sec-Fetch-Mode", "cors");

        requestRules.put("baidu.com", new RequestRule(true, true, baiduHeaders, false));
        requestRules.put("ext.baidu.com", new RequestRule(true, true, baiduHeaders, false));
        requestRules.put("api.baidu.com", new RequestRule(true, true, baiduHeaders, false));

        // 视频网站规则
        Map<String, String> videoHeaders = new HashMap<>();
        videoHeaders.put("Sec-Fetch-Dest", "video");
        videoHeaders.put("Sec-Fetch-Mode", "no-cors");
        videoHeaders.put("Range", "bytes=0-");

        requestRules.put("youtube.com", new RequestRule(true, true, videoHeaders, false));
        requestRules.put("googlevideo.com", new RequestRule(true, true, videoHeaders, false));

        // 广告过滤规则
        requestRules.put("adsystem.", new RequestRule(false, false, null, true));
        requestRules.put("doubleclick.", new RequestRule(false, false, null, true));
        requestRules.put("googlesyndication.", new RequestRule(false, false, null, true));

        Log.d(TAG, "Request rules initialized: " + requestRules.size() + " rules");
    }

    /**
     * 处理资源请求 - WebViewClient.shouldInterceptRequest的回调
     */
    public WebResourceResponse processRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        String method = request.getMethod();

        // 统计请求
        updateRequestStats(url, method);

        // 检查是否应该拦截
        if (shouldBlockRequest(url)) {
            Log.d(TAG, "Blocked request: " + url);
            return createBlockedResponse();
        }

        // 应用请求规则
        RequestRule rule = getRequestRule(url);
        if (rule != null && rule.modifyHeaders) {
            // 这里可以修改请求头，但WebView API限制了直接修改
            Log.d(TAG, "Applied rule for: " + url + " (allow: " + rule.allow + ")");
        }

        // 特殊处理某些类型的请求
        WebsiteType siteType = compatibilityManager.identifyWebsiteType(url);
        return processByWebsiteType(view, request, siteType);
    }

    /**
     * 根据网站类型处理请求
     */
    private WebResourceResponse processByWebsiteType(WebView view, WebResourceRequest request, WebsiteType siteType) {
        String url = request.getUrl().toString();

        switch (siteType) {
            case SEARCH_ENGINE:
                return processSearchEngineRequest(view, request);
            case VIDEO_PLATFORM:
                return processVideoRequest(view, request);
            case API_SERVICE:
                return processApiRequest(view, request);
            case CDN_STATIC:
                return processCdnRequest(view, request);
            case ADULT_CONTENT:
                return processAdultContentRequest(view, request);
            default:
                return null; // 不拦截
        }
    }

    /**
     * 处理搜索引擎请求
     */
    private WebResourceResponse processSearchEngineRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();

        // 百度搜索特殊处理
        if (url.contains("baidu.com")) {
            if (url.contains("ext.baidu.com") || url.contains("/rest/")) {
                // 百度API请求，添加必要的头部信息
                Log.d(TAG, "Processing Baidu API request: " + url);
                // 可以在这里添加更多的百度API处理逻辑
            }
        }

        return null;
    }

    /**
     * 处理视频平台请求
     */
    private WebResourceResponse processVideoRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();

        // YouTube视频处理
        if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
            if (url.contains(".mp4") || url.contains(".webm") || url.contains("videoplayback")) {
                Log.d(TAG, "Processing YouTube video request: " + url);
                // 可以在这里添加视频播放优化逻辑
            }
        }

        return null;
    }

    /**
     * 处理API请求
     */
    private WebResourceResponse processApiRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();

        // 百度API特殊处理
        if (url.contains("ext.baidu.com") || url.contains("rest.baidu.com")) {
            Log.d(TAG, "Processing Baidu API: " + url);

            // 检查是否是JSONP请求
            if (url.contains("callback=")) {
                Log.d(TAG, "Detected JSONP request from Baidu API");
            }
        }

        return null;
    }

    /**
     * 处理CDN请求
     */
    private WebResourceResponse processCdnRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();

        // 百度CDN优化
        if (url.contains("bdstatic.com") || url.contains("baidustatic.com")) {
            Log.d(TAG, "Optimizing Baidu CDN request: " + url);
            // 可以在这里添加CDN缓存优化
        }

        return null;
    }

    /**
     * 处理成人内容请求
     */
    private WebResourceResponse processAdultContentRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();

        // 成人网站特殊处理
        if (url.contains("xvideos.com") || url.contains("pornhub.com")) {
            Log.d(TAG, "Processing adult content request: " + url);
            // 可以在这里添加相应的处理逻辑
        }

        return null;
    }

    /**
     * 检查是否应该拦截请求
     */
    private boolean shouldBlockRequest(String url) {
        // 广告过滤 - 暂时禁用，等待AdBlockManager完善
        // if (adBlockManager != null && adBlockManager.shouldBlock(url)) {
        //     return true;
        // }

        // 规则检查
        RequestRule rule = getRequestRule(url);
        if (rule != null && rule.blockIfMatch) {
            return true;
        }

        return false;
    }

    /**
     * 获取请求规则
     */
    private RequestRule getRequestRule(String url) {
        for (Map.Entry<String, RequestRule> entry : requestRules.entrySet()) {
            if (url.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 创建拦截响应
     */
    private WebResourceResponse createBlockedResponse() {
        return new WebResourceResponse(
            "text/plain",
            "UTF-8",
            new ByteArrayInputStream("Blocked by EhViewer".getBytes())
        );
    }

    /**
     * 更新请求统计
     */
    private void updateRequestStats(String url, String method) {
        String key = method + ":" + extractDomain(url);
        requestStats.put(key, requestStats.getOrDefault(key, 0) + 1);
        requestTimestamps.put(url, System.currentTimeMillis());
    }

    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                String withoutProtocol = url.substring(url.indexOf("://") + 3);
                int slashIndex = withoutProtocol.indexOf("/");
                return slashIndex > 0 ? withoutProtocol.substring(0, slashIndex) : withoutProtocol;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting domain from: " + url, e);
        }
        return "unknown";
    }

    /**
     * 获取请求统计信息
     */
    public Map<String, Integer> getRequestStats() {
        return new HashMap<>(requestStats);
    }

    /**
     * 清理过期统计数据
     */
    public void cleanupStats() {
        long currentTime = System.currentTimeMillis();
        requestTimestamps.entrySet().removeIf(entry ->
            currentTime - entry.getValue() > 24 * 60 * 60 * 1000); // 24小时
    }

    /**
     * 添加自定义请求规则
     */
    public void addRequestRule(String domain, RequestRule rule) {
        requestRules.put(domain, rule);
        Log.d(TAG, "Added custom request rule for: " + domain);
    }
}
