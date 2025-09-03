package com.hippo.ehviewer.ui.browser;

import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.client.BookmarkManager;
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.util.DomainSuggestionManager;
import com.hippo.ehviewer.util.SearchSuggestionProvider;

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
        public final long score;
        public final long timestamp;

        public SuggestionItem(String text, String url, String displayText,
                            SuggestionType type, long score, long timestamp) {
            this.text = text;
            this.url = url;
            this.displayText = displayText;
            this.type = type;
            this.score = score;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(SuggestionItem other) {
            // 按分数降序排序，相同分数按时间戳降序
            if (this.score != other.score) {
                return Long.compare(other.score, this.score);
            }
            return Long.compare(other.timestamp, this.timestamp);
        }

        @Override
        public String toString() {
            return String.format("SuggestionItem{text='%s', url='%s', type=%s, score=%d}",
                    text, url, type.name(), score);
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

    // 数据提供者
    private final HistoryManager mHistoryManager;
    private final BookmarkManager mBookmarkManager;
    private final SearchSuggestionProvider mSearchProvider;
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

    public static RealtimeSuggestionManager getInstance() {
        if (sInstance == null) {
            synchronized (RealtimeSuggestionManager.class) {
                if (sInstance == null) {
                    sInstance = new RealtimeSuggestionManager();
                }
            }
        }
        return sInstance;
    }

    private RealtimeSuggestionManager() {
        mHistoryManager = HistoryManager.getInstance();
        mBookmarkManager = BookmarkManager.getInstance();
        mSearchProvider = SearchSuggestionProvider.getInstance();
        mDomainManager = DomainSuggestionManager.getInstance();
        mNetworkProvider = NetworkSuggestionProvider.getInstance();

        // 初始化网络建议提供者
        mNetworkProvider.setCurrentEngine(NetworkSuggestionProvider.SearchEngine.GOOGLE);
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
     * 内部建议获取逻辑
     */
    private List<SuggestionItem> getSuggestionsInternal(String query) {
        List<SuggestionItem> allSuggestions = new ArrayList<>();

        // 1. 获取本地建议（历史记录、书签、域名）
        allSuggestions.addAll(getLocalSuggestions(query));

        // 2. 获取网络搜索建议
        try {
            List<SuggestionItem> networkSuggestions = getNetworkSuggestionsSync(query);
            if (networkSuggestions != null) {
                allSuggestions.addAll(networkSuggestions);
            }
        } catch (Exception e) {
            // 网络建议失败不影响其他建议
        }

        // 3. 智能排序和过滤
        return smartSortAndFilter(allSuggestions, query);
    }

    /**
     * 获取本地建议
     */
    private List<SuggestionItem> getLocalSuggestions(String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        long currentTime = System.currentTimeMillis();

        // 历史记录建议
        List<HistoryManager.HistoryItem> historyItems = mHistoryManager.getHistoryItems();
        for (HistoryManager.HistoryItem item : historyItems) {
            if (isSmartMatch(query, item.title, item.url)) {
                suggestions.add(new SuggestionItem(
                    item.title,
                    item.url,
                    item.title,
                    SuggestionType.HISTORY,
                    calculateHistoryScore(item),
                    item.timestamp
                ));
            }
        }

        // 书签建议
        List<BookmarkManager.BookmarkItem> bookmarkItems = mBookmarkManager.getBookmarkItems();
        for (BookmarkManager.BookmarkItem item : bookmarkItems) {
            if (isSmartMatch(query, item.title, item.url)) {
                suggestions.add(new SuggestionItem(
                    item.title,
                    item.url,
                    item.title,
                    SuggestionType.BOOKMARK,
                    calculateBookmarkScore(item),
                    currentTime
                ));
            }
        }

        // 域名建议
        List<String> domainSuggestions = mDomainManager.getDomainSuggestions(query);
        for (String domain : domainSuggestions) {
            suggestions.add(new SuggestionItem(
                domain,
                "https://" + domain,
                domain,
                SuggestionType.DOMAIN,
                80, // 域名建议基础分数
                currentTime
            ));
        }

        return suggestions;
    }

    /**
     * 获取网络搜索建议（同步版本）
     */
    private List<SuggestionItem> getNetworkSuggestionsSync(String query) {
        List<String> networkSuggestions = mNetworkProvider.requestSuggestionsSync(query);
        if (networkSuggestions == null || networkSuggestions.isEmpty()) {
            return new ArrayList<>();
        }

        List<SuggestionItem> suggestions = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (String suggestion : networkSuggestions) {
            suggestions.add(new SuggestionItem(
                suggestion,
                null, // 搜索建议没有URL
                suggestion,
                SuggestionType.SEARCH,
                60, // 搜索建议基础分数
                currentTime
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
     * 智能匹配算法
     */
    private boolean isSmartMatch(String query, String title, String url) {
        if (query == null || query.isEmpty()) return false;
        if (title == null && url == null) return false;

        String lowerQuery = query.toLowerCase();

        // 精确匹配标题开头
        if (title != null && title.toLowerCase().startsWith(lowerQuery)) {
            return true;
        }

        // 包含匹配
        if (title != null && title.toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // URL匹配
        if (url != null && url.toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // 特殊关联匹配（例如 "porn" -> "pornhub"）
        return isAssociativeMatch(lowerQuery, title, url);
    }

    /**
     * 关联匹配（基于语义关联）
     */
    private boolean isAssociativeMatch(String query, String title, String url) {
        // 示例：输入 "xvideos" 或 "xv" 应该匹配相关内容
        String[] pornKeywords = {"porn", "sex", "adult", "video", "xv", "xvideos"};
        String[] pornSites = {"pornhub", "xvideos", "xhamster", "redtube"};

        for (String keyword : pornKeywords) {
            if (query.contains(keyword) || keyword.contains(query)) {
                if (title != null) {
                    for (String site : pornSites) {
                        if (title.toLowerCase().contains(site)) {
                            return true;
                        }
                    }
                }
                if (url != null) {
                    for (String site : pornSites) {
                        if (url.toLowerCase().contains(site)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 智能排序和过滤
     */
    private List<SuggestionItem> smartSortAndFilter(List<SuggestionItem> suggestions, String query) {
        if (suggestions.isEmpty()) return suggestions;

        // 按分数排序
        Collections.sort(suggestions);

        // 限制数量
        if (suggestions.size() > MAX_SUGGESTIONS) {
            suggestions = suggestions.subList(0, MAX_SUGGESTIONS);
        }

        return suggestions;
    }

    /**
     * 计算历史记录分数
     */
    private long calculateHistoryScore(HistoryManager.HistoryItem item) {
        long baseScore = 100;
        long timeBonus = Math.max(0, (System.currentTimeMillis() - item.timestamp) / (24 * 60 * 60 * 1000)); // 天数
        return baseScore + timeBonus;
    }

    /**
     * 计算书签分数
     */
    private long calculateBookmarkScore(BookmarkManager.BookmarkItem item) {
        return 90; // 书签基础分数较高
    }

    /**
     * 记录URL访问（用于学习）
     */
    public void recordUrlVisit(String url, String title) {
        if (url != null && !url.isEmpty()) {
            mHistoryManager.addHistoryItem(url, title != null ? title : url);
        }
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
