package com.hippo.ehviewer.ui.browser;

import android.util.LruCache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 网络建议提供者 - 从各种搜索引擎获取搜索建议
 */
public class NetworkSuggestionProvider {

    // 搜索引擎枚举
    public enum SearchEngine {
        GOOGLE("Google", "https://www.google.com/complete/search?client=firefox&q=%s"),
        BING("Bing", "https://www.bing.com/AS/Suggestions?query=%s&mkt=zh-cn"),
        BAIDU("百度", "https://www.baidu.com/su?wd=%s"),
        DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/ac/?q=%s"),
        SOGOU("搜狗", "https://www.sogou.com/web?query=%s");

        private final String name;
        private final String suggestUrl;

        SearchEngine(String name, String suggestUrl) {
            this.name = name;
            this.suggestUrl = suggestUrl;
        }

        public String getName() {
            return name;
        }

        public String getSuggestUrl() {
            return suggestUrl;
        }
    }

    private static final int CACHE_SIZE = 100;
    private static final int MAX_SUGGESTIONS = 8;
    private static final long CACHE_VALID_TIME = 5 * 60 * 1000; // 5分钟

    // 缓存数据类
    private static class CacheEntry {
        final List<String> suggestions;
        final long timestamp;

        CacheEntry(List<String> suggestions, long timestamp) {
            this.suggestions = suggestions;
            this.timestamp = timestamp;
        }
    }

    // 组件
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final LruCache<String, CacheEntry> mCache = new LruCache<>(CACHE_SIZE);
    private SearchEngine mCurrentEngine = SearchEngine.GOOGLE;

    // 单例模式
    private static volatile NetworkSuggestionProvider sInstance;

    public static NetworkSuggestionProvider getInstance() {
        if (sInstance == null) {
            synchronized (NetworkSuggestionProvider.class) {
                if (sInstance == null) {
                    sInstance = new NetworkSuggestionProvider();
                }
            }
        }
        return sInstance;
    }

    private NetworkSuggestionProvider() {
        // 初始化默认引擎
    }

    /**
     * 设置当前搜索引擎
     */
    public void setCurrentEngine(SearchEngine engine) {
        if (engine != null) {
            mCurrentEngine = engine;
        }
    }

    /**
     * 获取当前搜索引擎
     */
    public SearchEngine getCurrentEngine() {
        return mCurrentEngine;
    }

    /**
     * 获取支持的搜索引擎列表
     */
    public SearchEngine[] getSupportedEngines() {
        return SearchEngine.values();
    }

    /**
     * 根据地区获取推荐搜索引擎
     */
    public SearchEngine getEngineForRegion(String countryCode) {
        if (countryCode == null) return SearchEngine.GOOGLE;

        switch (countryCode.toUpperCase()) {
            case "CN":
            case "HK":
            case "TW":
            case "MO":
            case "SG":
            case "MY":
                return SearchEngine.BAIDU;
            default:
                return SearchEngine.GOOGLE;
        }
    }

    /**
     * 请求建议（异步）
     */
    public void requestSuggestions(String query, SuggestionCallback callback) {
        if (query == null || query.trim().isEmpty() || callback == null) {
            callback.onSuggestionsReady(new ArrayList<>());
            return;
        }

        String trimmedQuery = query.trim();
        String cacheKey = mCurrentEngine.name() + ":" + trimmedQuery;

        // 检查缓存
        CacheEntry cached = mCache.get(cacheKey);
        if (cached != null && isCacheValid(cached)) {
            callback.onSuggestionsReady(cached.suggestions);
            return;
        }

        // 异步请求
        mExecutor.submit(() -> {
            try {
                List<String> suggestions = fetchSuggestions(trimmedQuery);
                updateCache(cacheKey, suggestions);
                callback.onSuggestionsReady(suggestions);
            } catch (Exception e) {
                callback.onError("获取网络建议失败: " + e.getMessage());
            }
        });
    }

    /**
     * 请求建议（同步）- 用于紧急情况
     */
    public List<String> requestSuggestionsSync(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String trimmedQuery = query.trim();
        String cacheKey = mCurrentEngine.name() + ":" + trimmedQuery;

        // 检查缓存
        CacheEntry cached = mCache.get(cacheKey);
        if (cached != null && isCacheValid(cached)) {
            return cached.suggestions;
        }

        try {
            List<String> suggestions = fetchSuggestions(trimmedQuery);
            updateCache(cacheKey, suggestions);
            return suggestions;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 获取建议（内部方法）
     */
    private List<String> fetchSuggestions(String query) throws IOException {
        String suggestUrl = String.format(mCurrentEngine.getSuggestUrl(),
            URLEncoder.encode(query, "UTF-8"));

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(suggestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return parseResponse(response.toString(), query);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 解析响应数据
     */
    private List<String> parseResponse(String response, String originalQuery) {
        List<String> suggestions = new ArrayList<>();

        try {
            switch (mCurrentEngine) {
                case GOOGLE:
                    suggestions = parseGoogleResponse(response);
                    break;
                case BING:
                    suggestions = parseBingResponse(response);
                    break;
                case BAIDU:
                    suggestions = parseBaiduResponse(response);
                    break;
                case DUCKDUCKGO:
                    suggestions = parseDuckDuckGoResponse(response);
                    break;
                case SOGOU:
                    suggestions = parseSogouResponse(response);
                    break;
            }
        } catch (Exception e) {
            // 解析失败，返回空列表
        }

        // 限制数量
        if (suggestions.size() > MAX_SUGGESTIONS) {
            suggestions = suggestions.subList(0, MAX_SUGGESTIONS);
        }

        return suggestions;
    }

    /**
     * 解析Google响应
     */
    private List<String> parseGoogleResponse(String response) {
        List<String> suggestions = new ArrayList<>();
        // Google返回JSONP格式: window.google.ac.h(["query",["suggestion1","suggestion2",...]])
        try {
            int start = response.indexOf("[\"");
            int end = response.lastIndexOf("\"]");
            if (start >= 0 && end > start) {
                String jsonArray = response.substring(start, end + 2);
                // 简单解析，去掉引号和逗号
                String[] parts = jsonArray.replace("[\"", "").replace("\"]", "").split("\",\"");
                for (String part : parts) {
                    if (!part.trim().isEmpty()) {
                        suggestions.add(part.trim());
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return suggestions;
    }

    /**
     * 解析Bing响应
     */
    private List<String> parseBingResponse(String response) {
        List<String> suggestions = new ArrayList<>();
        // Bing返回XML格式，简单解析
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("<Query>")) {
                    String suggestion = line.replaceAll(".*<Query>(.*?)</Query>.*", "$1");
                    if (!suggestion.trim().isEmpty()) {
                        suggestions.add(suggestion.trim());
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return suggestions;
    }

    /**
     * 解析百度响应
     */
    private List<String> parseBaiduResponse(String response) {
        List<String> suggestions = new ArrayList<>();
        // 百度返回JSONP格式
        try {
            int start = response.indexOf("([");
            int end = response.lastIndexOf("])");
            if (start >= 0 && end > start) {
                String jsonPart = response.substring(start + 1, end);
                // 简单解析
                String[] parts = jsonPart.split(",");
                for (String part : parts) {
                    String suggestion = part.replaceAll("\"", "").trim();
                    if (!suggestion.isEmpty() && !suggestion.equals("null")) {
                        suggestions.add(suggestion);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return suggestions;
    }

    /**
     * 解析DuckDuckGo响应
     */
    private List<String> parseDuckDuckGoResponse(String response) {
        List<String> suggestions = new ArrayList<>();
        // DuckDuckGo返回JSON格式
        try {
            // 简单JSON解析
            String[] parts = response.split("\"phrase\":\"");
            for (int i = 1; i < parts.length; i++) {
                int endIndex = parts[i].indexOf("\"");
                if (endIndex > 0) {
                    String suggestion = parts[i].substring(0, endIndex);
                    if (!suggestion.trim().isEmpty()) {
                        suggestions.add(suggestion.trim());
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return suggestions;
    }

    /**
     * 解析搜狗响应
     */
    private List<String> parseSogouResponse(String response) {
        List<String> suggestions = new ArrayList<>();
        // 搜狗返回XML格式，简单解析
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("<item>")) {
                    String suggestion = line.replaceAll(".*<item>(.*?)</item>.*", "$1");
                    if (!suggestion.trim().isEmpty()) {
                        suggestions.add(suggestion.trim());
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return suggestions;
    }

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(CacheEntry entry) {
        return System.currentTimeMillis() - entry.timestamp < CACHE_VALID_TIME;
    }

    /**
     * 更新缓存
     */
    private void updateCache(String key, List<String> suggestions) {
        mCache.put(key, new CacheEntry(new ArrayList<>(suggestions), System.currentTimeMillis()));
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        mCache.evictAll();
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("缓存项数量: %d, 最大容量: %d",
            mCache.size(), CACHE_SIZE);
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        mExecutor.shutdown();
        clearCache();
    }

    // 回调接口
    public interface SuggestionCallback {
        void onSuggestionsReady(List<String> suggestions);
        void onError(String error);
    }
}
