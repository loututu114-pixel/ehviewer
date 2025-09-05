package com.hippo.ehviewer.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 地址栏智能缓存系统
 * 专门优化地址栏输入响应速度和搜索建议性能
 */
public class AddressBarCache {
    
    private static final String TAG = "AddressBarCache";
    private static final String PREFS_NAME = "address_bar_cache";
    private static final String KEY_SUGGESTION_CACHE = "suggestion_cache";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final String KEY_DOMAIN_CACHE = "domain_cache";
    
    // 缓存大小配置
    private static final int SUGGESTION_CACHE_SIZE = 500; // 建议缓存数量
    private static final int DOMAIN_CACHE_SIZE = 200;     // 域名缓存数量
    private static final int SEARCH_HISTORY_SIZE = 100;   // 搜索历史数量
    
    // 缓存过期时间（毫秒）
    private static final long SUGGESTION_CACHE_EXPIRE = 30 * 60 * 1000; // 30分钟
    private static final long DOMAIN_CACHE_EXPIRE = 24 * 60 * 60 * 1000; // 24小时
    
    // 单例实例
    private static volatile AddressBarCache sInstance;
    
    // 核心组件
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final ExecutorService mExecutor;
    
    // 内存缓存
    private final LruCache<String, SuggestionCacheItem> mSuggestionCache;
    private final LruCache<String, DomainCacheItem> mDomainCache;
    private final Map<String, Long> mSearchHistory; // 搜索词 -> 最后使用时间
    
    // 性能统计
    private int mCacheHitCount = 0;
    private int mCacheMissCount = 0;
    private long mTotalResponseTime = 0;
    private int mResponseCount = 0;
    
    private AddressBarCache(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mExecutor = Executors.newSingleThreadExecutor();
        
        // 初始化内存缓存
        mSuggestionCache = new LruCache<String, SuggestionCacheItem>(SUGGESTION_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, SuggestionCacheItem item) {
                return item.estimateMemorySize();
            }
        };
        
        mDomainCache = new LruCache<String, DomainCacheItem>(DOMAIN_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, DomainCacheItem item) {
                return item.estimateMemorySize();
            }
        };
        
        mSearchHistory = new ConcurrentHashMap<>();
        
        // 异步加载持久化数据
        loadCacheFromDisk();
    }
    
    public static AddressBarCache getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AddressBarCache.class) {
                if (sInstance == null) {
                    sInstance = new AddressBarCache(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 建议缓存项
     */
    public static class SuggestionCacheItem {
        public final String query;
        public final List<String> suggestions;
        public final long timestamp;
        public final int hitCount;
        
        public SuggestionCacheItem(String query, List<String> suggestions) {
            this.query = query;
            this.suggestions = new ArrayList<>(suggestions);
            this.timestamp = System.currentTimeMillis();
            this.hitCount = 1;
        }
        
        public SuggestionCacheItem(String query, List<String> suggestions, long timestamp, int hitCount) {
            this.query = query;
            this.suggestions = suggestions;
            this.timestamp = timestamp;
            this.hitCount = hitCount;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > SUGGESTION_CACHE_EXPIRE;
        }
        
        public int estimateMemorySize() {
            int size = query.length() * 2; // String内存占用粗略估计
            for (String suggestion : suggestions) {
                size += suggestion.length() * 2;
            }
            return size + 32; // 对象头和其他字段
        }
        
        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("query", query);
                json.put("timestamp", timestamp);
                json.put("hitCount", hitCount);
                
                JSONArray suggestionsArray = new JSONArray();
                for (String suggestion : suggestions) {
                    suggestionsArray.put(suggestion);
                }
                json.put("suggestions", suggestionsArray);
                
                return json;
            } catch (Exception e) {
                Log.e(TAG, "Error converting SuggestionCacheItem to JSON", e);
                return new JSONObject();
            }
        }
        
        public static SuggestionCacheItem fromJson(JSONObject json) {
            try {
                String query = json.getString("query");
                long timestamp = json.getLong("timestamp");
                int hitCount = json.getInt("hitCount");
                
                JSONArray suggestionsArray = json.getJSONArray("suggestions");
                List<String> suggestions = new ArrayList<>();
                for (int i = 0; i < suggestionsArray.length(); i++) {
                    suggestions.add(suggestionsArray.getString(i));
                }
                
                return new SuggestionCacheItem(query, suggestions, timestamp, hitCount);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing SuggestionCacheItem from JSON", e);
                return null;
            }
        }
    }
    
    /**
     * 域名缓存项
     */
    public static class DomainCacheItem {
        public final String domain;
        public final String title;
        public final String favicon;
        public final long timestamp;
        public final int visitCount;
        
        public DomainCacheItem(String domain, String title, String favicon) {
            this.domain = domain;
            this.title = title;
            this.favicon = favicon;
            this.timestamp = System.currentTimeMillis();
            this.visitCount = 1;
        }
        
        public DomainCacheItem(String domain, String title, String favicon, long timestamp, int visitCount) {
            this.domain = domain;
            this.title = title;
            this.favicon = favicon;
            this.timestamp = timestamp;
            this.visitCount = visitCount;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > DOMAIN_CACHE_EXPIRE;
        }
        
        public int estimateMemorySize() {
            int size = domain.length() * 2;
            if (title != null) size += title.length() * 2;
            if (favicon != null) size += favicon.length() * 2;
            return size + 32;
        }
        
        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("domain", domain);
                json.put("title", title != null ? title : "");
                json.put("favicon", favicon != null ? favicon : "");
                json.put("timestamp", timestamp);
                json.put("visitCount", visitCount);
                return json;
            } catch (Exception e) {
                Log.e(TAG, "Error converting DomainCacheItem to JSON", e);
                return new JSONObject();
            }
        }
        
        public static DomainCacheItem fromJson(JSONObject json) {
            try {
                String domain = json.getString("domain");
                String title = json.optString("title", "");
                String favicon = json.optString("favicon", "");
                long timestamp = json.getLong("timestamp");
                int visitCount = json.getInt("visitCount");
                
                return new DomainCacheItem(domain, title.isEmpty() ? null : title, 
                    favicon.isEmpty() ? null : favicon, timestamp, visitCount);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing DomainCacheItem from JSON", e);
                return null;
            }
        }
    }
    
    /**
     * 获取搜索建议（快速响应）
     */
    public List<String> getSuggestions(@NonNull String query) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 标准化查询
            String normalizedQuery = normalizeQuery(query);
            
            // 首先检查内存缓存
            SuggestionCacheItem cached = mSuggestionCache.get(normalizedQuery);
            if (cached != null && !cached.isExpired()) {
                mCacheHitCount++;
                recordResponseTime(System.currentTimeMillis() - startTime);
                Log.d(TAG, "Cache hit for query: " + normalizedQuery);
                return new ArrayList<>(cached.suggestions);
            }
            
            // 缓存未命中，生成新建议
            mCacheMissCount++;
            List<String> suggestions = generateSuggestions(normalizedQuery);
            
            // 缓存结果
            if (!suggestions.isEmpty()) {
                mSuggestionCache.put(normalizedQuery, new SuggestionCacheItem(normalizedQuery, suggestions));
            }
            
            recordResponseTime(System.currentTimeMillis() - startTime);
            Log.d(TAG, "Generated " + suggestions.size() + " suggestions for: " + normalizedQuery);
            return suggestions;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting suggestions for query: " + query, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 生成搜索建议
     */
    private List<String> generateSuggestions(@NonNull String query) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            // 1. 从搜索历史中匹配
            for (String historyQuery : mSearchHistory.keySet()) {
                if (historyQuery.toLowerCase().contains(query.toLowerCase()) && !historyQuery.equals(query)) {
                    suggestions.add(historyQuery);
                    if (suggestions.size() >= 3) break; // 限制历史建议数量
                }
            }
            
            // 2. 从域名缓存中匹配
            Map<String, DomainCacheItem> domainSnapshot = mDomainCache.snapshot();
            for (Map.Entry<String, DomainCacheItem> entry : domainSnapshot.entrySet()) {
                if (suggestions.size() >= 8) break; // 限制建议总数
                
                DomainCacheItem item = entry.getValue();
                if (item != null && !item.isExpired()) {
                    if (item.domain.toLowerCase().contains(query.toLowerCase()) || 
                        (item.title != null && item.title.toLowerCase().contains(query.toLowerCase()))) {
                        suggestions.add(item.domain);
                    }
                }
            }
            
            // 3. 智能域名补全
            if (query.length() >= 2 && !query.contains(" ")) {
                suggestions.addAll(getSmartDomainSuggestions(query));
            }
            
            // 4. 移除重复项并排序
            suggestions = dedupAndSort(suggestions, query);
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating suggestions", e);
        }
        
        return suggestions;
    }
    
    /**
     * 智能域名建议
     */
    private List<String> getSmartDomainSuggestions(@NonNull String query) {
        List<String> suggestions = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        // 常见网站匹配
        Map<String, String> commonSites = getCommonSitesMap();
        for (Map.Entry<String, String> entry : commonSites.entrySet()) {
            if (entry.getKey().startsWith(lowerQuery)) {
                suggestions.add(entry.getValue());
            }
        }
        
        return suggestions;
    }
    
    /**
     * 获取常见网站映射
     */
    private Map<String, String> getCommonSitesMap() {
        Map<String, String> sites = new HashMap<>();
        sites.put("goo", "https://www.google.com");
        sites.put("google", "https://www.google.com");
        sites.put("bai", "https://www.baidu.com");
        sites.put("baidu", "https://www.baidu.com");
        sites.put("bili", "https://www.bilibili.com");
        sites.put("bilibili", "https://www.bilibili.com");
        sites.put("zhihu", "https://www.zhihu.com");
        sites.put("weibo", "https://weibo.com");
        sites.put("taobao", "https://www.taobao.com");
        sites.put("jd", "https://www.jd.com");
        sites.put("youku", "https://www.youku.com");
        sites.put("iqiyi", "https://www.iqiyi.com");
        sites.put("douban", "https://www.douban.com");
        sites.put("github", "https://github.com");
        sites.put("stackoverflow", "https://stackoverflow.com");
        return sites;
    }
    
    /**
     * 去重和排序
     */
    private List<String> dedupAndSort(@NonNull List<String> suggestions, @NonNull String query) {
        // 移除重复项
        List<String> deduped = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (!deduped.contains(suggestion)) {
                deduped.add(suggestion);
            }
        }
        
        // 按相关性排序（简单的字符串匹配优先级）
        Collections.sort(deduped, (s1, s2) -> {
            int score1 = calculateRelevanceScore(s1, query);
            int score2 = calculateRelevanceScore(s2, query);
            return Integer.compare(score2, score1); // 降序
        });
        
        // 限制数量
        return deduped.subList(0, Math.min(deduped.size(), 8));
    }
    
    /**
     * 计算相关性分数
     */
    private int calculateRelevanceScore(@NonNull String suggestion, @NonNull String query) {
        String lowerSuggestion = suggestion.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        int score = 0;
        
        // 前缀匹配得分最高
        if (lowerSuggestion.startsWith(lowerQuery)) {
            score += 100;
        }
        
        // 包含匹配
        if (lowerSuggestion.contains(lowerQuery)) {
            score += 50;
        }
        
        // 长度惩罚（更短的建议优先）
        score -= suggestion.length();
        
        // 搜索历史加权
        if (mSearchHistory.containsKey(suggestion)) {
            score += 30;
        }
        
        return score;
    }
    
    /**
     * 添加搜索历史
     */
    public void addSearchHistory(@NonNull String query) {
        try {
            String normalizedQuery = normalizeQuery(query);
            mSearchHistory.put(normalizedQuery, System.currentTimeMillis());
            
            // 异步保存
            mExecutor.execute(() -> saveSearchHistoryToDisk());
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding search history", e);
        }
    }
    
    /**
     * 添加域名信息到缓存
     */
    public void addDomainInfo(@NonNull String domain, @Nullable String title, @Nullable String favicon) {
        try {
            String normalizedDomain = normalizeDomain(domain);
            DomainCacheItem existing = mDomainCache.get(normalizedDomain);
            
            DomainCacheItem item;
            if (existing != null) {
                // 更新现有项
                item = new DomainCacheItem(normalizedDomain, title != null ? title : existing.title,
                    favicon != null ? favicon : existing.favicon, System.currentTimeMillis(), 
                    existing.visitCount + 1);
            } else {
                // 创建新项
                item = new DomainCacheItem(normalizedDomain, title, favicon);
            }
            
            mDomainCache.put(normalizedDomain, item);
            
            // 异步保存
            mExecutor.execute(() -> saveDomainCacheToDisk());
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding domain info", e);
        }
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanExpiredCache() {
        mExecutor.execute(() -> {
            try {
                // 清理建议缓存
                Map<String, SuggestionCacheItem> snapshot = mSuggestionCache.snapshot();
                for (Map.Entry<String, SuggestionCacheItem> entry : snapshot.entrySet()) {
                    if (entry.getValue().isExpired()) {
                        mSuggestionCache.remove(entry.getKey());
                    }
                }
                
                // 清理域名缓存
                Map<String, DomainCacheItem> domainSnapshot = mDomainCache.snapshot();
                for (Map.Entry<String, DomainCacheItem> entry : domainSnapshot.entrySet()) {
                    if (entry.getValue().isExpired()) {
                        mDomainCache.remove(entry.getKey());
                    }
                }
                
                // 清理搜索历史（保留最近100条）
                if (mSearchHistory.size() > SEARCH_HISTORY_SIZE) {
                    List<Map.Entry<String, Long>> sortedHistory = new ArrayList<>(mSearchHistory.entrySet());
                    sortedHistory.sort(Map.Entry.<String, Long>comparingByValue().reversed());
                    
                    mSearchHistory.clear();
                    for (int i = 0; i < Math.min(SEARCH_HISTORY_SIZE, sortedHistory.size()); i++) {
                        Map.Entry<String, Long> entry = sortedHistory.get(i);
                        mSearchHistory.put(entry.getKey(), entry.getValue());
                    }
                }
                
                Log.d(TAG, "Cache cleanup completed");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning expired cache", e);
            }
        });
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        float hitRate = mCacheHitCount + mCacheMissCount > 0 ? 
            (float) mCacheHitCount / (mCacheHitCount + mCacheMissCount) * 100 : 0;
        
        float avgResponseTime = mResponseCount > 0 ? 
            (float) mTotalResponseTime / mResponseCount : 0;
        
        return String.format("缓存命中率: %.1f%% | 平均响应时间: %.1fms | 缓存大小: %d/%d",
            hitRate, avgResponseTime, mSuggestionCache.size(), SUGGESTION_CACHE_SIZE);
    }
    
    /**
     * 预热缓存
     */
    public void warmUpCache() {
        mExecutor.execute(() -> {
            try {
                // 预生成常见查询的建议
                String[] commonQueries = {"g", "go", "goo", "b", "ba", "bai", "bili", "zh", "zhi"};
                for (String query : commonQueries) {
                    getSuggestions(query);
                }
                Log.d(TAG, "Cache warm-up completed");
            } catch (Exception e) {
                Log.e(TAG, "Error warming up cache", e);
            }
        });
    }
    
    // 辅助方法
    private String normalizeQuery(@NonNull String query) {
        return query.trim().toLowerCase();
    }
    
    private String normalizeDomain(@NonNull String domain) {
        return domain.toLowerCase().replaceAll("^https?://", "").replaceAll("^www\\.", "");
    }
    
    private void recordResponseTime(long responseTime) {
        mTotalResponseTime += responseTime;
        mResponseCount++;
    }
    
    // 持久化方法
    private void loadCacheFromDisk() {
        mExecutor.execute(() -> {
            try {
                loadSearchHistoryFromDisk();
                loadDomainCacheFromDisk();
                Log.d(TAG, "Cache loaded from disk");
            } catch (Exception e) {
                Log.e(TAG, "Error loading cache from disk", e);
            }
        });
    }
    
    private void loadSearchHistoryFromDisk() {
        // 实现从SharedPreferences加载搜索历史
    }
    
    private void loadDomainCacheFromDisk() {
        // 实现从SharedPreferences加载域名缓存
    }
    
    private void saveSearchHistoryToDisk() {
        // 实现保存搜索历史到SharedPreferences
    }
    
    private void saveDomainCacheToDisk() {
        // 实现保存域名缓存到SharedPreferences
    }
}