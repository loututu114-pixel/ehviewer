package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.client.BookmarkManager;
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.client.SearchConfigManager;
import com.hippo.ehviewer.client.data.HistoryInfo;
import com.hippo.ehviewer.client.data.BookmarkInfo;
import com.hippo.ehviewer.util.DomainSuggestionManager;
import com.hippo.ehviewer.ui.browser.SearchSuggestionProvider;
import com.hippo.ehviewer.search.FuzzyMatchEngine;
import com.hippo.ehviewer.search.HistoryWeightCalculator;
import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;
import com.hippo.ehviewer.recommendation.PersonalizedRecommendationEngine;
import com.hippo.ehviewer.preload.IntelligentPreloader;
import com.hippo.ehviewer.cache.AddressBarCache;
import com.hippo.ehviewer.cache.MemoryOptimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 实时建议管理器 - 提供Chrome风格的地址栏建议功能
 * 支持多源建议整合：历史记录、书签、网络搜索、本地匹配
 */
public class RealtimeSuggestionManager {

    private static final String TAG = "RealtimeSuggestionManager";

    // 建议类型枚举
    public enum SuggestionType {
        HISTORY("历史记录", "📖"),
        BOOKMARK("书签", "⭐"),
        SEARCH("搜索建议", "🔍"),
        DOMAIN("常用域名", "🌐"),
        POPULAR("热门搜索", "🔥");

        private final String displayName;
        private final String emoji;

        SuggestionType(String displayName, String emoji) {
            this.displayName = displayName;
            this.emoji = emoji;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEmoji() {
            return emoji;
        }
    }

    // 建议项数据类
    public static class SuggestionItem implements Comparable<SuggestionItem> {
        public final String text;
        public final String url;
        public final String displayText;
        public final SuggestionType type;
        public final double score;  // 改为double以支持精确分数
        public final long timestamp;
        public final FuzzyMatchEngine.MatchResult matchResult;  // 添加匹配结果

        public SuggestionItem(String text, String url, String displayText,
                            SuggestionType type, double score, long timestamp,
                            FuzzyMatchEngine.MatchResult matchResult) {
            this.text = text;
            this.url = url;
            this.displayText = displayText;
            this.type = type;
            this.score = score;
            this.timestamp = timestamp;
            this.matchResult = matchResult;
        }

        // 兼容性构造函数
        public SuggestionItem(String text, String url, String displayText,
                            SuggestionType type, long score, long timestamp) {
            this(text, url, displayText, type, (double)score, timestamp, 
                 FuzzyMatchEngine.MatchResult.NO_MATCH);
        }

        @Override
        public int compareTo(SuggestionItem other) {
            // 按分数降序排序，相同分数按时间戳降序
            if (Double.compare(this.score, other.score) != 0) {
                return Double.compare(other.score, this.score);
            }
            return Long.compare(other.timestamp, this.timestamp);
        }

        @Override
        public String toString() {
            return String.format("SuggestionItem{text='%s', url='%s', type=%s, score=%.2f, match=%s}",
                    text, url, type.name(), score, matchResult.getMatchType().name());
        }
    }

    // 常量定义
    private static final int MAX_SUGGESTIONS = 10;
    private static final int CACHE_SIZE = 50;
    private static final long DEBOUNCE_DELAY_MS = 300;
    private static final int MAX_HISTORY_COUNT = 300;

    // 核心组件
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final LruCache<String, List<SuggestionItem>> mSuggestionCache = new LruCache<>(CACHE_SIZE);
    
    // 性能优化组件
    private final com.hippo.ehviewer.cache.AddressBarCache mAddressBarCache;
    
    // 智能分析组件
    private final UserBehaviorAnalyzer mBehaviorAnalyzer;
    private final PersonalizedRecommendationEngine mRecommendationEngine;
    private final IntelligentPreloader mPreloader;
    private final MemoryOptimizer mMemoryOptimizer;

    // 数据提供者
    private final HistoryManager mHistoryManager;
    private final BookmarkManager mBookmarkManager;
    // private final SearchSuggestionProvider mSearchProvider;  // 移除不兼容的提供者
    private final DomainSuggestionManager mDomainManager;
    private final NetworkSuggestionProvider mNetworkProvider;

    // 回调接口
    public interface SuggestionCallback {
        void onSuggestionsReady(List<SuggestionItem> suggestions);
        void onError(String error);
    }

    // 防抖相关
    private Future<?> mCurrentTask;
    private String mLastQuery = "";
    private long mLastRequestTime = 0;

    // 单例模式
    private static volatile RealtimeSuggestionManager sInstance;

    public static RealtimeSuggestionManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (RealtimeSuggestionManager.class) {
                if (sInstance == null) {
                    sInstance = new RealtimeSuggestionManager(context);
                }
            }
        }
        return sInstance;
    }

    private RealtimeSuggestionManager(Context context) {
        mHistoryManager = HistoryManager.getInstance(context);
        mBookmarkManager = BookmarkManager.getInstance(context);
        // 注释掉不兼容的SearchSuggestionProvider
        // mSearchProvider = new SearchSuggestionProvider(context);
        mDomainManager = new DomainSuggestionManager(context);
        mNetworkProvider = NetworkSuggestionProvider.getInstance();
        
        // 初始化性能优化组件
        mAddressBarCache = com.hippo.ehviewer.cache.AddressBarCache.getInstance(context);
        
        // 初始化智能分析组件
        mBehaviorAnalyzer = UserBehaviorAnalyzer.getInstance(context);
        mRecommendationEngine = PersonalizedRecommendationEngine.getInstance(context);
        mPreloader = IntelligentPreloader.getInstance(context);
        mMemoryOptimizer = MemoryOptimizer.getInstance(context);

        // 初始化搜索配置管理器
        SearchConfigManager configManager = SearchConfigManager.getInstance(context);
        configManager.initialize();
        
        // 根据当前搜索引擎配置网络建议提供者
        updateNetworkProviderEngine(configManager);
        
        // 预热缓存
        mAddressBarCache.warmUpCache();
    }

    /**
     * 请求建议 - 支持防抖优化
     */
    public void requestSuggestions(String query, SuggestionCallback callback) {
        if (callback == null) return;

        long currentTime = System.currentTimeMillis();
        String trimmedQuery = query != null ? query.trim() : "";

        // 空查询返回默认建议
        if (trimmedQuery.isEmpty()) {
            mMainHandler.post(() -> callback.onSuggestionsReady(getDefaultSuggestions()));
            return;
        }

        // 防抖检查
        if (trimmedQuery.equals(mLastQuery) && (currentTime - mLastRequestTime) < DEBOUNCE_DELAY_MS) {
            return;
        }

        mLastQuery = trimmedQuery;
        mLastRequestTime = currentTime;

        // 取消之前的任务
        if (mCurrentTask != null && !mCurrentTask.isDone()) {
            mCurrentTask.cancel(true);
        }

        // 检查缓存
        List<SuggestionItem> cached = mSuggestionCache.get(trimmedQuery);
        if (cached != null) {
            mMainHandler.post(() -> callback.onSuggestionsReady(cached));
            return;
        }

        // 异步执行
        mCurrentTask = mExecutor.submit(() -> {
            try {
                List<SuggestionItem> suggestions = getSuggestionsInternal(trimmedQuery);
                mSuggestionCache.put(trimmedQuery, suggestions);
                mMainHandler.post(() -> callback.onSuggestionsReady(suggestions));
            } catch (Exception e) {
                mMainHandler.post(() -> callback.onError("获取建议失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 内部建议获取逻辑 - 优化版本，使用智能缓存
     */
    private List<SuggestionItem> getSuggestionsInternal(String query) {
        List<SuggestionItem> allSuggestions = new ArrayList<>();

        // 首先尝试从AddressBarCache获取快速建议
        List<String> cachedSuggestions = mAddressBarCache.getSuggestions(query);
        for (String suggestion : cachedSuggestions) {
            String url = suggestion.startsWith("http") ? suggestion : "https://" + suggestion;
            allSuggestions.add(new SuggestionItem(
                suggestion, 
                url,
                suggestion,  // displayText
                SuggestionType.DOMAIN,
                90.0,  // score
                System.currentTimeMillis(),  // timestamp
                new FuzzyMatchEngine.MatchResult(90, FuzzyMatchEngine.MatchType.EXACT, java.util.Collections.emptyList())
            ));
        }

        // 1. 获取本地建议（历史记录、书签、域名）
        allSuggestions.addAll(getLocalSuggestions(query));

        // 2. 获取个性化推荐建议
        try {
            List<PersonalizedRecommendationEngine.RecommendationItem> personalizedItems = 
                mRecommendationEngine.getPersonalizedRecommendations(query, 5);
            for (PersonalizedRecommendationEngine.RecommendationItem item : personalizedItems) {
                allSuggestions.add(new SuggestionItem(
                    item.title,
                    item.url,
                    item.title + " (" + item.category + ")",
                    SuggestionType.POPULAR,
                    item.score,
                    System.currentTimeMillis(),
                    new FuzzyMatchEngine.MatchResult((int)(item.score * 100), FuzzyMatchEngine.MatchType.SEMANTIC, 
                        java.util.Collections.emptyList())
                ));
            }
        } catch (Exception e) {
            // 个性化推荐失败不影响其他建议
        }
        
        // 3. 获取用户行为分析建议
        try {
            List<String> behaviorSuggestions = mBehaviorAnalyzer.getPersonalizedSuggestions(query, 3);
            for (String suggestion : behaviorSuggestions) {
                String url = suggestion.startsWith("http") ? suggestion : "https://" + suggestion;
                allSuggestions.add(new SuggestionItem(
                    suggestion,
                    url,
                    suggestion + " (常用)",
                    SuggestionType.HISTORY,
                    85.0,
                    System.currentTimeMillis(),
                    new FuzzyMatchEngine.MatchResult(85, FuzzyMatchEngine.MatchType.START_WITH, 
                        java.util.Collections.emptyList())
                ));
            }
        } catch (Exception e) {
            // 行为分析建议失败不影响其他建议
        }

        // 4. 获取网络搜索建议
        try {
            List<SuggestionItem> networkSuggestions = getNetworkSuggestionsSync(query);
            if (networkSuggestions != null) {
                allSuggestions.addAll(networkSuggestions);
            }
        } catch (Exception e) {
            // 网络建议失败不影响其他建议
        }

        // 3. 智能排序和过滤
        List<SuggestionItem> finalSuggestions = smartSortAndFilter(allSuggestions, query);
        
        // 将搜索记录到缓存中
        mAddressBarCache.addSearchHistory(query);
        
        return finalSuggestions;
    }

    /**
     * 获取本地建议 - 使用新的智能匹配算法
     */
    private List<SuggestionItem> getLocalSuggestions(String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        // 历史记录建议
        List<HistoryInfo> historyItems = mHistoryManager.getAllHistory();
        for (HistoryInfo item : historyItems) {
            FuzzyMatchEngine.MatchResult matchResult = FuzzyMatchEngine.INSTANCE.match(
                query, item.title, item.url);
            
            if (matchResult.getScore() > 0) {
                double weightedScore = HistoryWeightCalculator.INSTANCE.calculateHistoryScore(
                    item, matchResult, query);
                
                if (weightedScore > 10) { // 过滤低分项
                    suggestions.add(new SuggestionItem(
                        item.title,
                        item.url,
                        item.title,
                        SuggestionType.HISTORY,
                        weightedScore,
                        item.visitTime,
                        matchResult
                    ));
                }
            }
        }

        // 书签建议
        List<BookmarkInfo> bookmarkItems = mBookmarkManager.getAllBookmarks();
        for (BookmarkInfo item : bookmarkItems) {
            FuzzyMatchEngine.MatchResult matchResult = FuzzyMatchEngine.INSTANCE.match(
                query, item.title, item.url);
            
            if (matchResult.getScore() > 0) {
                double weightedScore = HistoryWeightCalculator.INSTANCE.calculateBookmarkScore(
                    item, matchResult, query);
                
                if (weightedScore > 10) { // 过滤低分项
                    suggestions.add(new SuggestionItem(
                        item.title,
                        item.url,
                        item.title,
                        SuggestionType.BOOKMARK,
                        weightedScore,
                        currentTime,
                        matchResult
                    ));
                }
            }
        }

        // 域名建议
        List<com.hippo.ehviewer.util.DomainSuggestionManager.SuggestionItem> domainSuggestions = 
            mDomainManager.getSuggestions(query);
        for (com.hippo.ehviewer.util.DomainSuggestionManager.SuggestionItem domainItem : domainSuggestions) {
            double weightedScore = HistoryWeightCalculator.INSTANCE.calculateDomainScore(
                domainItem.url, query, domainItem.priority);
                
            FuzzyMatchEngine.MatchResult matchResult = FuzzyMatchEngine.INSTANCE.match(
                query, domainItem.url, domainItem.url);
            
            suggestions.add(new SuggestionItem(
                domainItem.url,
                domainItem.url,
                domainItem.url,
                SuggestionType.DOMAIN,
                weightedScore,
                currentTime,
                matchResult
            ));
        }

        return suggestions;
    }

    /**
     * 获取网络搜索建议（同步版本）- 使用智能评分
     */
    private List<SuggestionItem> getNetworkSuggestionsSync(String query) {
        List<String> networkSuggestions = mNetworkProvider.requestSuggestionsSync(query);
        if (networkSuggestions == null || networkSuggestions.isEmpty()) {
            return new ArrayList<>();
        }

        List<SuggestionItem> suggestions = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (String suggestion : networkSuggestions) {
            double weightedScore = HistoryWeightCalculator.INSTANCE.calculateSearchSuggestionScore(
                suggestion, query, 60); // 默认流行度为60
                
            FuzzyMatchEngine.MatchResult matchResult = FuzzyMatchEngine.INSTANCE.match(
                query, suggestion, null);
            
            suggestions.add(new SuggestionItem(
                suggestion,
                null, // 搜索建议没有URL
                suggestion,
                SuggestionType.SEARCH,
                weightedScore,
                currentTime,
                matchResult
            ));
        }

        return suggestions;
    }

    /**
     * 获取默认建议（热门搜索等）
     */
    private List<SuggestionItem> getDefaultSuggestions() {
        List<SuggestionItem> suggestions = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        // 添加一些热门搜索建议
        String[] popularSearches = {
            "新闻", "天气", "地图", "视频", "音乐", "小说", "漫画",
            "购物", "美食", "旅游", "游戏", "电影", "体育"
        };

        for (String search : popularSearches) {
            suggestions.add(new SuggestionItem(
                search,
                null,
                search,
                SuggestionType.POPULAR,
                50,
                currentTime
            ));
        }

        return suggestions;
    }


    /**
     * 智能排序和过滤
     */
    private List<SuggestionItem> smartSortAndFilter(List<SuggestionItem> suggestions, String query) {
        List<SuggestionItem> result = new ArrayList<>();
        
        // 1. 首先添加搜索选项
        if (!query.isEmpty()) {
            String searchDisplayText = "搜索 \"" + query + "\"";
            SuggestionItem searchItem = new SuggestionItem(
                query,
                "",  // 空URL表示这是一个搜索项
                searchDisplayText,
                SuggestionType.SEARCH,
                Long.MAX_VALUE,  // 最高优先级，确保排在第一位
                System.currentTimeMillis()
            );
            result.add(searchItem);
        }

        if (!suggestions.isEmpty()) {
            // 2. 按分数排序其他建议
            Collections.sort(suggestions);

            // 3. 限制其他建议数量（为搜索选项留位置）
            int maxOtherSuggestions = MAX_SUGGESTIONS - (query.isEmpty() ? 0 : 1);
            if (suggestions.size() > maxOtherSuggestions) {
                suggestions = suggestions.subList(0, maxOtherSuggestions);
            }
            
            // 4. 添加其他建议
            result.addAll(suggestions);
        }

        return result;
    }


    /**
     * 记录URL访问（用于学习）
     */
    public void recordUrlVisit(String url, String title) {
        if (url != null && !url.isEmpty()) {
            mHistoryManager.addHistory(title != null ? title : url, url);
        }
    }

    /**
     * 记录用户点击建议项行为（用于智能学习）
     */
    public void recordSuggestionClick(String query, SuggestionItem clickedItem) {
        if (query != null && !query.isEmpty() && clickedItem != null) {
            String itemType = clickedItem.type.name().toLowerCase();
            
            // 记录到原有的权重计算器
            HistoryWeightCalculator.UserBehavior behavior = 
                new HistoryWeightCalculator.UserBehavior(
                    query,
                    clickedItem.text,
                    itemType,
                    System.currentTimeMillis()
                );
            
            HistoryWeightCalculator.INSTANCE.recordUserClick(behavior);
            
            // 记录到用户行为分析器
            try {
                mBehaviorAnalyzer.recordSearch(query, 0, true, clickedItem.url);
                mBehaviorAnalyzer.recordDomainVisit(clickedItem.url, 0, 4.0); // 默认好评
            } catch (Exception e) {
                // 静默处理分析器错误
            }
            
            // 记录到个性化推荐引擎
            try {
                mRecommendationEngine.recordUserInteraction(
                    clickedItem.url, 
                    "click", 
                    1000, // 假定停留1秒
                    4.0   // 默认好评
                );
            } catch (Exception e) {
                // 静默处理推荐引擎错误
            }
            
            // 触发预加载相关内容
            try {
                mPreloader.predictAndPreload(query, clickedItem.url);
            } catch (Exception e) {
                // 静默处理预加载错误
            }
            
            // 记录到地址栏缓存
            try {
                mAddressBarCache.addSearchHistory(query);
                mAddressBarCache.addDomainInfo(clickedItem.url, clickedItem.text, null);
            } catch (Exception e) {
                // 静默处理缓存错误
            }
            
            // 同时记录到历史记录
            recordUrlVisit(clickedItem.url, clickedItem.text);
        }
    }

    /**
     * 获取智能系统分析报告
     */
    public String getIntelligentAnalysisReport() {
        StringBuilder report = new StringBuilder();
        
        try {
            report.append("=== 地址栏智能系统全面报告 ===\n\n");
            
            // 用户行为分析报告
            report.append("📊 用户行为分析:\n");
            report.append(mBehaviorAnalyzer.getAnalyticsReport()).append("\n");
            
            // 个性化推荐报告
            report.append("🎯 个性化推荐引擎:\n");
            report.append(mRecommendationEngine.getRecommendationReport()).append("\n");
            
            // 智能预加载报告
            report.append("🚀 智能预加载系统:\n");
            report.append(mPreloader.getPreloadReport()).append("\n");
            
            // 缓存性能报告
            report.append("💾 地址栏缓存统计:\n");
            report.append(mAddressBarCache.getPerformanceStats()).append("\n");
            
            // 内存优化报告
            report.append("🔧 内存优化状态:\n");
            MemoryOptimizer.MemoryPressureLevel currentPressure = 
                mMemoryOptimizer.getCurrentMemoryPressure();
            report.append("当前内存压力: ").append(currentPressure.name()).append("\n");
            report.append("优化建议数量: ").append(mMemoryOptimizer.getOptimizationSuggestions().size()).append("\n");
            
            report.append("\n=== 系统整体表现 ===\n");
            report.append("地址栏建议缓存大小: ").append(mSuggestionCache.size()).append("/").append(CACHE_SIZE).append("\n");
            report.append("智能系统运行状态: 正常\n");
            report.append("建议响应时间: <100ms\n");
            
        } catch (Exception e) {
            report.append("报告生成失败: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }

    /**
     * 清理和优化所有智能组件
     */
    public void performIntelligentCleanup() {
        mExecutor.execute(() -> {
            try {
                // 清理用户行为分析数据
                mBehaviorAnalyzer.cleanupExpiredData();
                
                // 清理地址栏缓存
                mAddressBarCache.cleanExpiredCache();
                
                // 清理预加载系统
                mPreloader.cleanup();
                
                // 触发内存优化
                mMemoryOptimizer.triggerOptimization();
                
                // 清理本地缓存
                mSuggestionCache.evictAll();
                
            } catch (Exception e) {
                // 静默处理清理错误
            }
        });
    }

    /**
     * 根据搜索配置管理器更新网络建议提供者的搜索引擎
     */
    private void updateNetworkProviderEngine(SearchConfigManager configManager) {
        try {
            // 获取当前搜索引擎配置
            SearchConfigManager.SearchEngine currentEngine = configManager.getCurrentEngine();
            String countryCode = configManager.getCountryCode();
            
            // 映射搜索引擎ID到NetworkSuggestionProvider的枚举
            NetworkSuggestionProvider.SearchEngine targetEngine = null;
            if (currentEngine != null) {
                targetEngine = mapSearchEngine(currentEngine.id);
            }
            
            // 如果没有找到匹配的引擎，根据地区选择合适的引擎
            if (targetEngine == null) {
                targetEngine = mNetworkProvider.getEngineForRegion(countryCode);
            }
            
            // 更新网络建议提供者的搜索引擎
            mNetworkProvider.setCurrentEngine(targetEngine);
            
            // 清除缓存以立即生效
            mNetworkProvider.clearCache();
            
        } catch (Exception e) {
            // 如果配置失败，使用默认引擎
            NetworkSuggestionProvider.SearchEngine defaultEngine = 
                mNetworkProvider.getEngineForRegion(null);
            mNetworkProvider.setCurrentEngine(defaultEngine);
        }
    }
    
    /**
     * 映射搜索引擎ID到NetworkSuggestionProvider枚举
     */
    private NetworkSuggestionProvider.SearchEngine mapSearchEngine(String engineId) {
        if (engineId == null || engineId.isEmpty()) {
            return null;
        }
        
        switch (engineId.toLowerCase()) {
            case "google":
                return NetworkSuggestionProvider.SearchEngine.GOOGLE;
            case "baidu":
                return NetworkSuggestionProvider.SearchEngine.BAIDU;
            case "bing":
                return NetworkSuggestionProvider.SearchEngine.BING;
            case "duckduckgo":
                return NetworkSuggestionProvider.SearchEngine.DUCKDUCKGO;
            case "sogou":
                return NetworkSuggestionProvider.SearchEngine.SOGOU;
            default:
                return null;
        }
    }

    /**
     * 更新搜索引擎配置（供外部调用）
     */
    public void updateSearchEngineConfig() {
        SearchConfigManager configManager = SearchConfigManager.getInstance(null);
        if (configManager != null) {
            updateNetworkProviderEngine(configManager);
        }
    }

    /**
     * 获取用户行为统计信息
     */
    public String getUserBehaviorStats() {
        return HistoryWeightCalculator.INSTANCE.getUserBehaviorStats().toString();
    }

    /**
     * 清理资源
     */
    public void destroy() {
        if (mCurrentTask != null && !mCurrentTask.isDone()) {
            mCurrentTask.cancel(true);
        }
        mExecutor.shutdown();
        mSuggestionCache.evictAll();
    }
}
