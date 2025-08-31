package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能地址栏识别系统
 * 自动识别用户输入意图：URL、搜索、文件路径等
 */
public class SmartAddressBar {
    
    private static final String TAG = "SmartAddressBar";
    
    // 常见顶级域名
    private static final String[] COMMON_TLDS = {
        ".com", ".cn", ".net", ".org", ".edu", ".gov", ".io", ".me", ".tv",
        ".info", ".biz", ".cc", ".co", ".app", ".dev", ".xyz", ".top"
    };
    
    // 常见协议
    private static final String[] PROTOCOLS = {
        "http://", "https://", "ftp://", "file://", "about:", "chrome://",
        "javascript:", "data:", "blob:"
    };
    
    // 搜索引擎配置
    public enum SearchEngine {
        BAIDU("百度", "https://www.baidu.com/s?wd=%s"),
        GOOGLE("谷歌", "https://www.google.com/search?q=%s"),
        BING("必应", "https://www.bing.com/search?q=%s"),
        SOGOU("搜狗", "https://www.sogou.com/web?query=%s"),
        DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s");
        
        private final String name;
        private final String searchUrl;
        
        SearchEngine(String name, String searchUrl) {
            this.name = name;
            this.searchUrl = searchUrl;
        }
        
        public String getSearchUrl(String query) {
            return String.format(searchUrl, android.net.Uri.encode(query));
        }
    }
    
    // 输入类型
    public enum InputType {
        URL,              // 完整URL
        DOMAIN,           // 域名
        IP_ADDRESS,       // IP地址
        SEARCH_QUERY,     // 搜索查询
        FILE_PATH,        // 文件路径
        QUICK_COMMAND,    // 快捷命令
        BOOKMARK_SEARCH,  // 书签搜索
        HISTORY_SEARCH    // 历史搜索
    }
    
    // 输入分析结果
    public static class InputAnalysis {
        public InputType type;
        public String originalInput;
        public String processedUrl;
        public String displayText;
        public List<Suggestion> suggestions;
        public float confidence;  // 置信度 0-1
        
        public InputAnalysis() {
            suggestions = new ArrayList<>();
            confidence = 0;
        }
    }
    
    // 建议项
    public static class Suggestion {
        public String title;
        public String subtitle;
        public String url;
        public String iconUrl;
        public SuggestionType type;
        public int priority;  // 优先级
        
        public enum SuggestionType {
            URL_COMPLETION,     // URL补全
            SEARCH_SUGGESTION,  // 搜索建议
            BOOKMARK,          // 书签
            HISTORY,           // 历史记录
            QUICK_ACCESS       // 快速访问
        }
    }
    
    private Context context;
    private SearchEngine currentSearchEngine = SearchEngine.BAIDU;
    
    public SmartAddressBar(Context context) {
        this.context = context;
    }
    
    /**
     * 智能分析用户输入
     */
    public InputAnalysis analyzeInput(String input) {
        InputAnalysis analysis = new InputAnalysis();
        analysis.originalInput = input;
        
        if (TextUtils.isEmpty(input)) {
            analysis.type = InputType.SEARCH_QUERY;
            analysis.confidence = 0;
            return analysis;
        }
        
        input = input.trim();
        
        // 1. 检查是否为快捷命令
        if (isQuickCommand(input)) {
            return processQuickCommand(input);
        }
        
        // 2. 检查是否为完整URL（包含协议）
        if (hasProtocol(input)) {
            analysis.type = InputType.URL;
            analysis.processedUrl = input;
            analysis.displayText = input;
            analysis.confidence = 1.0f;
            return analysis;
        }
        
        // 3. 检查是否为IP地址
        if (isIPAddress(input)) {
            analysis.type = InputType.IP_ADDRESS;
            analysis.processedUrl = "http://" + input;
            analysis.displayText = input;
            analysis.confidence = 0.95f;
            generateIPSuggestions(analysis, input);
            return analysis;
        }
        
        // 4. 检查是否为文件路径
        if (isFilePath(input)) {
            analysis.type = InputType.FILE_PATH;
            analysis.processedUrl = "file://" + input;
            analysis.displayText = input;
            analysis.confidence = 0.9f;
            return analysis;
        }
        
        // 5. 智能判断是域名还是搜索
        float domainScore = calculateDomainScore(input);
        float searchScore = calculateSearchScore(input);
        
        if (domainScore > searchScore && domainScore > 0.5f) {
            // 可能是域名
            analysis.type = InputType.DOMAIN;
            analysis.processedUrl = autoCompleteUrl(input);
            analysis.displayText = analysis.processedUrl;
            analysis.confidence = domainScore;
            generateDomainSuggestions(analysis, input);
        } else {
            // 作为搜索查询
            analysis.type = InputType.SEARCH_QUERY;
            analysis.processedUrl = currentSearchEngine.getSearchUrl(input);
            analysis.displayText = "搜索：" + input;
            analysis.confidence = searchScore;
            generateSearchSuggestions(analysis, input);
        }
        
        return analysis;
    }
    
    /**
     * 计算域名可能性分数
     */
    private float calculateDomainScore(String input) {
        float score = 0;
        
        // 包含点号
        if (input.contains(".")) {
            score += 0.3f;
            
            // 检查是否有常见顶级域名
            for (String tld : COMMON_TLDS) {
                if (input.endsWith(tld) || input.contains(tld + "/")) {
                    score += 0.5f;
                    break;
                }
            }
        }
        
        // 不包含空格
        if (!input.contains(" ")) {
            score += 0.1f;
        }
        
        // 包含www
        if (input.startsWith("www.") || input.startsWith("m.")) {
            score += 0.3f;
        }
        
        // 包含常见域名模式
        if (input.matches("^[a-zA-Z0-9][a-zA-Z0-9-]*\\.[a-zA-Z]{2,}.*$")) {
            score += 0.2f;
        }
        
        // 包含端口号
        if (input.matches(".*:\\d{2,5}.*")) {
            score += 0.2f;
        }
        
        return Math.min(score, 1.0f);
    }
    
    /**
     * 计算搜索查询可能性分数
     */
    private float calculateSearchScore(String input) {
        float score = 0.5f;  // 基础分数
        
        // 包含空格（多个词）
        if (input.contains(" ")) {
            score += 0.3f;
        }
        
        // 包含中文
        if (containsChinese(input)) {
            score += 0.2f;
        }
        
        // 包含问号（疑问句）
        if (input.contains("?") || input.contains("？")) {
            score += 0.1f;
        }
        
        // 不像域名格式
        if (!input.matches(".*\\.[a-zA-Z]{2,}.*")) {
            score += 0.1f;
        }
        
        return Math.min(score, 1.0f);
    }
    
    /**
     * 自动补全URL
     */
    private String autoCompleteUrl(String input) {
        // 如果没有协议，添加http://
        if (!hasProtocol(input)) {
            // 如果看起来像域名，添加协议
            if (input.contains(".") || input.startsWith("localhost")) {
                return "http://" + input;
            }
        }
        return input;
    }
    
    /**
     * 检查是否包含协议
     */
    private boolean hasProtocol(String input) {
        for (String protocol : PROTOCOLS) {
            if (input.startsWith(protocol)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否为IP地址
     */
    private boolean isIPAddress(String input) {
        // IPv4
        Pattern ipv4Pattern = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(:\\d{1,5})?$"
        );
        
        // IPv6 简化版
        Pattern ipv6Pattern = Pattern.compile(
            "^\\[?[0-9a-fA-F:]+\\]?(:\\d{1,5})?$"
        );
        
        return ipv4Pattern.matcher(input).matches() || 
               ipv6Pattern.matcher(input).matches();
    }
    
    /**
     * 检查是否为文件路径
     */
    private boolean isFilePath(String input) {
        return input.startsWith("/") || 
               input.startsWith("~") ||
               input.matches("^[A-Za-z]:[/\\\\].*");
    }
    
    /**
     * 检查是否为快捷命令
     */
    private boolean isQuickCommand(String input) {
        return input.startsWith(":") || 
               input.startsWith("about:") ||
               input.startsWith("chrome://");
    }
    
    /**
     * 处理快捷命令
     */
    private InputAnalysis processQuickCommand(String input) {
        InputAnalysis analysis = new InputAnalysis();
        analysis.originalInput = input;
        analysis.type = InputType.QUICK_COMMAND;
        analysis.confidence = 1.0f;
        
        // 解析命令
        if (input.equals(":history")) {
            analysis.displayText = "打开历史记录";
            // 特殊处理
        } else if (input.equals(":bookmarks")) {
            analysis.displayText = "打开书签";
        } else if (input.equals(":downloads")) {
            analysis.displayText = "打开下载";
        } else if (input.equals(":settings")) {
            analysis.displayText = "打开设置";
        } else if (input.equals(":cache")) {
            analysis.displayText = "缓存管理";
        } else if (input.startsWith("about:")) {
            analysis.processedUrl = input;
            analysis.displayText = input;
        }
        
        return analysis;
    }
    
    /**
     * 检查是否包含中文
     */
    private boolean containsChinese(String str) {
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
        return pattern.matcher(str).find();
    }
    
    /**
     * 生成域名建议
     */
    private void generateDomainSuggestions(InputAnalysis analysis, String input) {
        // 添加不同协议的建议
        Suggestion httpSuggestion = new Suggestion();
        httpSuggestion.title = "http://" + input;
        httpSuggestion.subtitle = "使用HTTP访问";
        httpSuggestion.url = "http://" + input;
        httpSuggestion.type = Suggestion.SuggestionType.URL_COMPLETION;
        httpSuggestion.priority = 1;
        analysis.suggestions.add(httpSuggestion);
        
        Suggestion httpsSuggestion = new Suggestion();
        httpsSuggestion.title = "https://" + input;
        httpsSuggestion.subtitle = "使用HTTPS安全访问";
        httpsSuggestion.url = "https://" + input;
        httpsSuggestion.type = Suggestion.SuggestionType.URL_COMPLETION;
        httpsSuggestion.priority = 0;
        analysis.suggestions.add(httpsSuggestion);
        
        // 如果没有www，添加www建议
        if (!input.startsWith("www.")) {
            Suggestion wwwSuggestion = new Suggestion();
            wwwSuggestion.title = "www." + input;
            wwwSuggestion.subtitle = "添加www前缀";
            wwwSuggestion.url = "http://www." + input;
            wwwSuggestion.type = Suggestion.SuggestionType.URL_COMPLETION;
            wwwSuggestion.priority = 2;
            analysis.suggestions.add(wwwSuggestion);
        }
    }
    
    /**
     * 生成IP地址建议
     */
    private void generateIPSuggestions(InputAnalysis analysis, String input) {
        // HTTP访问
        Suggestion httpSuggestion = new Suggestion();
        httpSuggestion.title = "http://" + input;
        httpSuggestion.subtitle = "通过HTTP访问";
        httpSuggestion.url = "http://" + input;
        httpSuggestion.type = Suggestion.SuggestionType.URL_COMPLETION;
        analysis.suggestions.add(httpSuggestion);
        
        // HTTPS访问
        Suggestion httpsSuggestion = new Suggestion();
        httpsSuggestion.title = "https://" + input;
        httpsSuggestion.subtitle = "通过HTTPS访问";
        httpsSuggestion.url = "https://" + input;
        httpsSuggestion.type = Suggestion.SuggestionType.URL_COMPLETION;
        analysis.suggestions.add(httpsSuggestion);
    }
    
    /**
     * 生成搜索建议
     */
    private void generateSearchSuggestions(InputAnalysis analysis, String input) {
        // 添加不同搜索引擎的建议
        for (SearchEngine engine : SearchEngine.values()) {
            if (engine != currentSearchEngine) {
                Suggestion suggestion = new Suggestion();
                suggestion.title = "在" + engine.name + "中搜索";
                suggestion.subtitle = input;
                suggestion.url = engine.getSearchUrl(input);
                suggestion.type = Suggestion.SuggestionType.SEARCH_SUGGESTION;
                suggestion.priority = engine.ordinal() + 10;
                analysis.suggestions.add(suggestion);
            }
        }
        
        // 如果看起来也可能是域名，添加直接访问建议
        if (calculateDomainScore(input) > 0.3f) {
            Suggestion directAccess = new Suggestion();
            directAccess.title = "直接访问 " + input;
            directAccess.subtitle = "作为网址访问";
            directAccess.url = autoCompleteUrl(input);
            directAccess.type = Suggestion.SuggestionType.URL_COMPLETION;
            directAccess.priority = 5;
            analysis.suggestions.add(directAccess);
        }
    }
    
    // Getter和Setter
    public void setSearchEngine(SearchEngine engine) {
        this.currentSearchEngine = engine;
    }
    
    public SearchEngine getSearchEngine() {
        return currentSearchEngine;
    }
}