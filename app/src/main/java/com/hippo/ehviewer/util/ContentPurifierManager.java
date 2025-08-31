package com.hippo.ehviewer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.WebView;
import java.util.HashMap;
import java.util.Map;

/**
 * 内容净化管理器
 * 为视频网站和小说网站提供纯净模式
 */
public class ContentPurifierManager {
    private static final String TAG = "ContentPurifier";
    private static final String PREF_NAME = "content_purifier_prefs";
    private static final String KEY_VIDEO_MODE_ENABLED = "video_mode_enabled";
    private static final String KEY_READING_MODE_ENABLED = "reading_mode_enabled";
    
    private final Context context;
    private final SharedPreferences preferences;
    private static ContentPurifierManager instance;
    
    // 视频网站净化脚本
    private final Map<String, String> videoSitePurifiers = new HashMap<String, String>() {{
        // YouTube视频净化
        put("youtube.com", getYouTubePurifierScript());
        put("youtu.be", getYouTubePurifierScript());
        put("m.youtube.com", getYouTubePurifierScript());
        
        // B站视频净化
        put("bilibili.com", getBilibiliPurifierScript());
        put("m.bilibili.com", getBilibiliPurifierScript());
        
        // 其他视频网站
        put("vimeo.com", getVimeoPurifierScript());
        put("twitch.tv", getTwitchPurifierScript());
        put("netflix.com", getNetflixPurifierScript());
    }};
    
    // 小说网站净化脚本
    private final Map<String, String> novelSitePurifiers = new HashMap<String, String>() {{
        // 起点中文网
        put("qidian.com", getQidianPurifierScript());
        
        // 晋江文学城
        put("jjwxc.net", getJinjiangPurifierScript());
        
        // 纵横中文网
        put("zongheng.com", getZonghengPurifierScript());
        
        // 17K小说网
        put("17k.com", get17KPurifierScript());
        
        // 番茄小说
        put("fanqienovel.com", getFanqiePurifierScript());
        
        // 通用小说网站
        put("novel", getGenericNovelPurifierScript());
    }};
    
    public static ContentPurifierManager getInstance(Context context) {
        if (instance == null) {
            instance = new ContentPurifierManager(context);
        }
        return instance;
    }
    
    private ContentPurifierManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 为网站应用内容净化
     */
    public void applyContentPurification(WebView webView, String url) {
        if (webView == null || url == null) return;
        
        String domain = extractDomain(url);
        
        // 检查是否是视频网站
        if (isVideoSite(domain) && isVideoModeEnabled()) {
            String script = videoSitePurifiers.get(domain);
            if (script != null) {
                executeScript(webView, script);
                Log.d(TAG, "Applied video purification for: " + domain);
            }
        }
        
        // 检查是否是小说网站
        if (isNovelSite(domain) && isReadingModeEnabled()) {
            String script = novelSitePurifiers.get(domain);
            if (script == null) {
                // 尝试通用小说净化脚本
                script = novelSitePurifiers.get("novel");
            }
            if (script != null) {
                executeScript(webView, script);
                Log.d(TAG, "Applied novel purification for: " + domain);
            }
        }
    }
    
    /**
     * 检查是否是视频网站
     */
    private boolean isVideoSite(String domain) {
        return videoSitePurifiers.containsKey(domain) ||
               domain.contains("video") ||
               domain.contains("tube") ||
               domain.contains("tv") ||
               domain.contains("play");
    }
    
    /**
     * 检查是否是小说网站
     */
    private boolean isNovelSite(String domain) {
        return novelSitePurifiers.containsKey(domain) ||
               domain.contains("novel") ||
               domain.contains("book") ||
               domain.contains("read") ||
               domain.contains("txt") ||
               isChineseNovelSite(domain);
    }
    
    /**
     * 检查是否是中文小说网站
     */
    private boolean isChineseNovelSite(String domain) {
        String[] novelKeywords = {"小说", "书", "文学", "阅读", "txt", "起点", "晋江", "纵横", "17k", "番茄"};
        for (String keyword : novelKeywords) {
            if (domain.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * YouTube视频净化脚本
     */
    private String getYouTubePurifierScript() {
        return "(function() {" +
            // 隐藏侧边栏和推荐视频
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                // 隐藏侧边栏
                "#secondary { display: none !important; }" +
                // 隐藏评论区
                "#comments { display: none !important; }" +
                // 隐藏推荐视频
                ".ytp-endscreen-content { display: none !important; }" +
                // 隐藏广告
                ".ytp-ad-overlay-container { display: none !important; }" +
                ".ytp-ad-text { display: none !important; }" +
                // 隐藏顶部导航
                "#masthead-container { display: none !important; }" +
                // 扩展主视频区域
                "#primary { width: 100% !important; max-width: 100% !important; }" +
                "#player { width: 100% !important; }" +
                // 隐藏底部推荐
                "#related { display: none !important; }" +
            "';" +
            "document.head.appendChild(style);" +
            
            // 移除不需要的元素
            "setTimeout(function() {" +
                "var elementsToHide = [" +
                    "'#secondary', '#comments', '.ytp-endscreen-content'," +
                    "'.ytp-ad-overlay-container', '#masthead-container'" +
                "];" +
                "elementsToHide.forEach(function(selector) {" +
                    "var elements = document.querySelectorAll(selector);" +
                    "elements.forEach(function(el) { el.style.display = 'none'; });" +
                "});" +
            "}, 1000);" +
        "})();";
    }
    
    /**
     * B站视频净化脚本
     */
    private String getBilibiliPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                // 隐藏侧边栏推荐
                ".recommend-container { display: none !important; }" +
                ".rec-list { display: none !important; }" +
                // 隐藏评论区
                ".bb-comment { display: none !important; }" +
                ".comment { display: none !important; }" +
                // 隐藏顶部导航
                ".bili-header { display: none !important; }" +
                // 隐藏底部推荐
                ".recommend { display: none !important; }" +
                // 隐藏弹幕设置（保留弹幕本身）
                ".bilibili-player-video-panel { display: none !important; }" +
                // 扩展视频区域
                ".video-info-container { width: 100% !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * Vimeo视频净化脚本
     */
    private String getVimeoPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                ".sidebar { display: none !important; }" +
                ".related_videos { display: none !important; }" +
                ".comments { display: none !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * Twitch视频净化脚本
     */
    private String getTwitchPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                ".chat-shell { display: none !important; }" +
                ".side-nav { display: none !important; }" +
                ".top-nav { display: none !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * Netflix视频净化脚本
     */
    private String getNetflixPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                ".billboard-row { display: none !important; }" +
                ".previewModal { display: none !important; }" +
                ".evidence-overlay { display: none !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * 起点中文网净化脚本
     */
    private String getQidianPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                // 隐藏广告
                ".ad, .advertisement, .banner { display: none !important; }" +
                // 隐藏侧边栏
                ".sidebar { display: none !important; }" +
                // 隐藏评论
                ".comment-list { display: none !important; }" +
                // 隐藏推荐
                ".recommend-list { display: none !important; }" +
                // 优化阅读区域
                ".read-content { width: 100% !important; max-width: 800px !important; margin: 0 auto !important; }" +
                // 优化字体
                ".read-content { font-size: 18px !important; line-height: 1.8 !important; }" +
                // 夜间阅读优化
                "body { background: #1a1a1a !important; color: #e0e0e0 !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * 晋江文学城净化脚本
     */
    private String getJinjiangPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                ".ad, .advertisement { display: none !important; }" +
                ".sidebar { display: none !important; }" +
                "body { background: #f5f5f5 !important; }" +
                ".noveltext { max-width: 800px !important; margin: 0 auto !important; padding: 20px !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * 纵横中文网净化脚本
     */
    private String getZonghengPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                ".ad-container { display: none !important; }" +
                ".right-sidebar { display: none !important; }" +
                ".content { width: 100% !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * 17K小说网净化脚本
     */
    private String get17KPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                ".ad { display: none !important; }" +
                ".sidebar { display: none !important; }" +
                "#content { width: 100% !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * 番茄小说净化脚本
     */
    private String getFanqiePurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                ".recommend { display: none !important; }" +
                ".comment { display: none !important; }" +
                ".article-content { max-width: 800px !important; margin: 0 auto !important; }" +
            "';" +
            "document.head.appendChild(style);" +
        "})();";
    }
    
    /**
     * 通用小说网站净化脚本
     */
    private String getGenericNovelPurifierScript() {
        return "(function() {" +
            "var style = document.createElement('style');" +
            "style.textContent = '" +
                // 隐藏常见广告类名
                ".ad, .advertisement, .banner, .popup, .modal { display: none !important; }" +
                // 隐藏侧边栏
                ".sidebar, .side-bar, .right-bar, .left-bar { display: none !important; }" +
                // 隐藏评论
                ".comment, .comments, .comment-list, .discuss { display: none !important; }" +
                // 隐藏推荐
                ".recommend, .related, .suggestion { display: none !important; }" +
                // 优化内容区域
                ".content, .article, .chapter, .text { " +
                    "width: 100% !important; " +
                    "max-width: 800px !important; " +
                    "margin: 0 auto !important; " +
                    "padding: 20px !important; " +
                    "font-size: 18px !important; " +
                    "line-height: 1.8 !important; " +
                "}" +
                // 夜间模式
                "body { " +
                    "background: #1e1e1e !important; " +
                    "color: #e0e0e0 !important; " +
                "}" +
            "';" +
            "document.head.appendChild(style);" +
            
            // 移除干扰元素
            "setTimeout(function() {" +
                "var distractions = document.querySelectorAll('.ad, .advertisement, .sidebar, .popup');" +
                "distractions.forEach(function(el) { el.remove(); });" +
            "}, 2000);" +
        "})();";
    }
    
    /**
     * 执行JavaScript脚本
     */
    private void executeScript(WebView webView, String script) {
        webView.post(() -> {
            webView.evaluateJavascript(script, null);
        });
    }
    
    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        if (url == null) return "";
        
        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }
            
            int slashIndex = url.indexOf('/');
            if (slashIndex > 0) {
                url = url.substring(0, slashIndex);
            }
            
            return url.toLowerCase();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting domain", e);
            return "";
        }
    }
    
    /**
     * 检查视频模式是否启用
     */
    public boolean isVideoModeEnabled() {
        return preferences.getBoolean(KEY_VIDEO_MODE_ENABLED, true);
    }
    
    /**
     * 设置视频模式
     */
    public void setVideoModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VIDEO_MODE_ENABLED, enabled).apply();
    }
    
    /**
     * 检查阅读模式是否启用
     */
    public boolean isReadingModeEnabled() {
        return preferences.getBoolean(KEY_READING_MODE_ENABLED, true);
    }
    
    /**
     * 设置阅读模式
     */
    public void setReadingModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_READING_MODE_ENABLED, enabled).apply();
    }
    
    /**
     * 强制刷新页面净化
     */
    public void refreshPurification(WebView webView, String url) {
        webView.post(() -> {
            // 等待页面加载完成后再净化
            webView.postDelayed(() -> {
                applyContentPurification(webView, url);
            }, 3000);
        });
    }
}