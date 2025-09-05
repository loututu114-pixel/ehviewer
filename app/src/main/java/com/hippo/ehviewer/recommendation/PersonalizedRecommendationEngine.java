package com.hippo.ehviewer.recommendation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 个性化推荐引擎
 * 基于用户行为分析，提供智能化的个性化内容推荐
 */
public class PersonalizedRecommendationEngine {
    
    private static final String TAG = "PersonalizedRecommendationEngine";
    private static final String PREFS_NAME = "personalized_recommendations";
    private static final String KEY_RECOMMENDATION_HISTORY = "recommendation_history";
    private static final String KEY_USER_PROFILE = "user_profile";
    private static final String KEY_CONTENT_SCORES = "content_scores";
    
    // 推荐参数配置
    private static final int MAX_RECOMMENDATIONS = 20;
    private static final int RECOMMENDATION_CACHE_SIZE = 100;
    private static final double FRESHNESS_DECAY_FACTOR = 0.95; // 新鲜度衰减系数
    private static final double POPULARITY_WEIGHT = 0.3;       // 流行度权重
    private static final double PERSONALIZATION_WEIGHT = 0.7;  // 个性化权重
    
    // 单例实例
    private static volatile PersonalizedRecommendationEngine sInstance;
    
    // 核心组件
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final ExecutorService mExecutor;
    private final UserBehaviorAnalyzer mBehaviorAnalyzer;
    
    // 推荐数据存储
    private final Map<String, RecommendationItem> mRecommendationCache;
    private final Map<String, Double> mContentScores;     // 内容评分
    private final Map<String, UserInteraction> mUserInteractions; // 用户交互记录
    private final UserProfile mUserProfile;              // 用户画像
    
    // 推荐算法组件
    private final ContentBasedFilter mContentBasedFilter;
    private final CollaborativeFilter mCollaborativeFilter;
    private final TrendingAnalyzer mTrendingAnalyzer;
    private final TimeBasedOptimizer mTimeBasedOptimizer;
    
    private PersonalizedRecommendationEngine(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mExecutor = Executors.newSingleThreadExecutor();
        mBehaviorAnalyzer = UserBehaviorAnalyzer.getInstance(context);
        
        mRecommendationCache = new ConcurrentHashMap<>();
        mContentScores = new ConcurrentHashMap<>();
        mUserInteractions = new ConcurrentHashMap<>();
        mUserProfile = new UserProfile();
        
        // 初始化推荐算法组件
        mContentBasedFilter = new ContentBasedFilter();
        mCollaborativeFilter = new CollaborativeFilter();
        mTrendingAnalyzer = new TrendingAnalyzer();
        mTimeBasedOptimizer = new TimeBasedOptimizer();
        
        // 异步加载历史数据
        loadRecommendationData();
        
        // 定期更新推荐
        startPeriodicRecommendationUpdate();
    }
    
    public static PersonalizedRecommendationEngine getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PersonalizedRecommendationEngine.class) {
                if (sInstance == null) {
                    sInstance = new PersonalizedRecommendationEngine(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 推荐项数据类
     */
    public static class RecommendationItem {
        public final String id;
        public final String title;
        public final String url;
        public final String category;
        public final RecommendationType type;
        public double score;              // 推荐分数
        public long timestamp;           // 推荐时间
        public int impressions;          // 展示次数
        public int clicks;               // 点击次数
        public final Map<String, String> metadata; // 附加元数据
        
        public RecommendationItem(String id, String title, String url, String category, 
                                RecommendationType type) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.category = category;
            this.type = type;
            this.score = 0.0;
            this.timestamp = System.currentTimeMillis();
            this.impressions = 0;
            this.clicks = 0;
            this.metadata = new HashMap<>();
        }
        
        public double getClickThroughRate() {
            return impressions > 0 ? (double) clicks / impressions : 0.0;
        }
        
        public double getFreshnessScore() {
            long age = System.currentTimeMillis() - timestamp;
            return Math.pow(FRESHNESS_DECAY_FACTOR, age / (24.0 * 60 * 60 * 1000)); // 按天衰减
        }
        
        public void recordImpression() {
            impressions++;
        }
        
        public void recordClick() {
            clicks++;
        }
        
        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("title", title);
                json.put("url", url);
                json.put("category", category);
                json.put("type", type.name());
                json.put("score", score);
                json.put("timestamp", timestamp);
                json.put("impressions", impressions);
                json.put("clicks", clicks);
                
                JSONObject metadataJson = new JSONObject();
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    metadataJson.put(entry.getKey(), entry.getValue());
                }
                json.put("metadata", metadataJson);
                
                return json;
            } catch (Exception e) {
                Log.e(TAG, "Error converting RecommendationItem to JSON", e);
                return new JSONObject();
            }
        }
        
        public static RecommendationItem fromJson(JSONObject json) {
            try {
                String id = json.getString("id");
                String title = json.getString("title");
                String url = json.getString("url");
                String category = json.getString("category");
                RecommendationType type = RecommendationType.valueOf(json.getString("type"));
                
                RecommendationItem item = new RecommendationItem(id, title, url, category, type);
                item.score = json.optDouble("score", 0.0);
                item.timestamp = json.optLong("timestamp", System.currentTimeMillis());
                item.impressions = json.optInt("impressions", 0);
                item.clicks = json.optInt("clicks", 0);
                
                JSONObject metadataJson = json.optJSONObject("metadata");
                if (metadataJson != null) {
                    Iterator<String> keys = metadataJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        item.metadata.put(key, metadataJson.getString(key));
                    }
                }
                
                return item;
            } catch (Exception e) {
                Log.e(TAG, "Error parsing RecommendationItem from JSON", e);
                return null;
            }
        }
    }
    
    /**
     * 推荐类型枚举
     */
    public enum RecommendationType {
        WEBSITE("网站推荐"),
        SEARCH_SUGGESTION("搜索建议"),
        CONTENT("内容推荐"),
        TRENDING("趋势推荐"),
        PERSONALIZED("个性化推荐"),
        TIME_BASED("基于时间的推荐"),
        SIMILAR("相似推荐");
        
        private final String displayName;
        
        RecommendationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 用户交互记录
     */
    public static class UserInteraction {
        public final String itemId;
        public long lastInteractionTime;
        public int totalInteractions;
        public double averageRating;
        public long totalTimeSpent;
        public final List<String> actions; // 用户行为序列
        
        public UserInteraction(String itemId) {
            this.itemId = itemId;
            this.lastInteractionTime = System.currentTimeMillis();
            this.totalInteractions = 1;
            this.averageRating = 3.0; // 默认中性评分
            this.totalTimeSpent = 0;
            this.actions = new ArrayList<>();
        }
        
        public void recordInteraction(String action, long timeSpent, double rating) {
            totalInteractions++;
            lastInteractionTime = System.currentTimeMillis();
            totalTimeSpent += timeSpent;
            averageRating = (averageRating + rating) / 2.0;
            
            actions.add(action);
            
            // 限制行为历史长度
            while (actions.size() > 50) {
                actions.remove(0);
            }
        }
        
        public double getEngagementScore() {
            // 基于交互频率、时间投入、评分计算参与度分数
            double frequencyScore = Math.min(totalInteractions / 10.0, 1.0);
            double timeScore = Math.min(totalTimeSpent / 300000.0, 1.0); // 5分钟为满分
            double ratingScore = averageRating / 5.0;
            
            return (frequencyScore * 0.4 + timeScore * 0.4 + ratingScore * 0.2);
        }
    }
    
    /**
     * 用户画像
     */
    public static class UserProfile {
        public final Map<String, Double> categoryPreferences;  // 分类偏好
        public final Map<String, Double> keywordInterests;     // 关键词兴趣
        public final Map<Integer, Double> timePatterns;        // 时间使用模式
        public double explorationTendency;                     // 探索倾向（0-1）
        public double qualityThreshold;                        // 质量阈值
        public String primaryCategory;                         // 主要感兴趣的分类
        
        public UserProfile() {
            this.categoryPreferences = new ConcurrentHashMap<>();
            this.keywordInterests = new ConcurrentHashMap<>();
            this.timePatterns = new ConcurrentHashMap<>();
            this.explorationTendency = 0.3; // 30%的探索性
            this.qualityThreshold = 0.6;    // 60分以上才推荐
            this.primaryCategory = "general";
        }
        
        public void updatePreferences(String category, List<String> keywords, double score) {
            // 更新分类偏好
            categoryPreferences.put(category, 
                categoryPreferences.getOrDefault(category, 0.0) + score);
            
            // 更新关键词兴趣
            for (String keyword : keywords) {
                keywordInterests.put(keyword, 
                    keywordInterests.getOrDefault(keyword, 0.0) + score * 0.1);
            }
            
            // 更新时间模式
            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            timePatterns.put(currentHour, 
                timePatterns.getOrDefault(currentHour, 0.0) + 1.0);
            
            // 重新计算主要分类
            recalculatePrimaryCategory();
        }
        
        private void recalculatePrimaryCategory() {
            primaryCategory = categoryPreferences.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("general");
        }
        
        public double getCategoryRelevance(String category) {
            return categoryPreferences.getOrDefault(category, 0.0);
        }
        
        public double getTimeRelevance() {
            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            return timePatterns.getOrDefault(currentHour, 0.0);
        }
    }
    
    // 推荐算法组件
    
    /**
     * 基于内容的过滤器
     */
    private class ContentBasedFilter {
        
        public List<RecommendationItem> recommend(String query, List<String> userHistory, int count) {
            List<RecommendationItem> recommendations = new ArrayList<>();
            
            try {
                // 从用户历史中提取兴趣特征
                Map<String, Double> interestProfile = extractInterestProfile(userHistory);
                
                // 基于兴趣特征匹配内容
                for (RecommendationItem item : mRecommendationCache.values()) {
                    double relevanceScore = calculateContentRelevance(item, interestProfile, query);
                    if (relevanceScore > mUserProfile.qualityThreshold) {
                        item.score = relevanceScore;
                        recommendations.add(item);
                    }
                }
                
                // 按相关性排序
                recommendations.sort((a, b) -> Double.compare(b.score, a.score));
                
            } catch (Exception e) {
                Log.e(TAG, "Error in content-based filtering", e);
            }
            
            return recommendations.subList(0, Math.min(recommendations.size(), count));
        }
        
        private Map<String, Double> extractInterestProfile(List<String> userHistory) {
            Map<String, Double> profile = new HashMap<>();
            
            for (String item : userHistory) {
                String[] keywords = extractKeywords(item);
                for (String keyword : keywords) {
                    profile.put(keyword, profile.getOrDefault(keyword, 0.0) + 1.0);
                }
            }
            
            // 归一化分数
            double maxScore = profile.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            for (Map.Entry<String, Double> entry : profile.entrySet()) {
                entry.setValue(entry.getValue() / maxScore);
            }
            
            return profile;
        }
        
        private double calculateContentRelevance(RecommendationItem item, 
                                              Map<String, Double> interestProfile, String query) {
            double titleScore = calculateTextSimilarity(item.title, query) * 0.4;
            double categoryScore = mUserProfile.getCategoryRelevance(item.category) * 0.3;
            double interestScore = calculateInterestAlignment(item, interestProfile) * 0.3;
            
            return titleScore + categoryScore + interestScore;
        }
        
        private double calculateInterestAlignment(RecommendationItem item, Map<String, Double> interests) {
            String[] itemKeywords = extractKeywords(item.title + " " + item.category);
            double alignment = 0.0;
            
            for (String keyword : itemKeywords) {
                alignment += interests.getOrDefault(keyword, 0.0);
            }
            
            return alignment / Math.max(itemKeywords.length, 1);
        }
    }
    
    /**
     * 协同过滤器
     */
    private class CollaborativeFilter {
        
        public List<RecommendationItem> recommend(String userId, int count) {
            List<RecommendationItem> recommendations = new ArrayList<>();
            
            try {
                // 查找相似用户（简化实现）
                List<String> similarUsers = findSimilarUsers(userId);
                
                // 聚合相似用户的偏好
                Map<String, Double> collaborativeScores = aggregateUserPreferences(similarUsers);
                
                // 生成推荐
                for (Map.Entry<String, Double> entry : collaborativeScores.entrySet()) {
                    RecommendationItem item = mRecommendationCache.get(entry.getKey());
                    if (item != null && entry.getValue() > mUserProfile.qualityThreshold) {
                        item.score = entry.getValue();
                        recommendations.add(item);
                    }
                }
                
                recommendations.sort((a, b) -> Double.compare(b.score, a.score));
                
            } catch (Exception e) {
                Log.e(TAG, "Error in collaborative filtering", e);
            }
            
            return recommendations.subList(0, Math.min(recommendations.size(), count));
        }
        
        private List<String> findSimilarUsers(String userId) {
            // 简化实现：基于用户交互相似度
            List<String> similarUsers = new ArrayList<>();
            // 在实际应用中，这里会有复杂的用户相似度计算
            return similarUsers;
        }
        
        private Map<String, Double> aggregateUserPreferences(List<String> users) {
            Map<String, Double> preferences = new HashMap<>();
            // 简化实现：聚合相似用户的偏好数据
            return preferences;
        }
    }
    
    /**
     * 趋势分析器
     */
    private class TrendingAnalyzer {
        
        public List<RecommendationItem> getTrendingRecommendations(int count) {
            List<RecommendationItem> trending = new ArrayList<>();
            
            try {
                // 基于点击率、新鲜度、互动率计算趋势分数
                for (RecommendationItem item : mRecommendationCache.values()) {
                    double trendScore = calculateTrendScore(item);
                    if (trendScore > 0.5) { // 趋势阈值
                        item.score = trendScore;
                        trending.add(item);
                    }
                }
                
                trending.sort((a, b) -> Double.compare(b.score, a.score));
                
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing trends", e);
            }
            
            return trending.subList(0, Math.min(trending.size(), count));
        }
        
        private double calculateTrendScore(RecommendationItem item) {
            double ctrScore = Math.min(item.getClickThroughRate() * 2, 1.0); // CTR权重
            double freshnessScore = item.getFreshnessScore();                // 新鲜度权重
            double popularityScore = Math.min(item.impressions / 1000.0, 1.0); // 流行度权重
            
            return ctrScore * 0.4 + freshnessScore * 0.3 + popularityScore * 0.3;
        }
    }
    
    /**
     * 基于时间的优化器
     */
    private class TimeBasedOptimizer {
        
        public List<RecommendationItem> optimizeForCurrentTime(List<RecommendationItem> items) {
            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            double timeRelevance = mUserProfile.getTimeRelevance();
            
            // 根据时间模式调整推荐分数
            for (RecommendationItem item : items) {
                double timeMultiplier = calculateTimeMultiplier(item, currentHour);
                item.score *= timeMultiplier;
            }
            
            items.sort((a, b) -> Double.compare(b.score, a.score));
            return items;
        }
        
        private double calculateTimeMultiplier(RecommendationItem item, int currentHour) {
            // 根据用户的时间使用模式和内容类型调整推荐分数
            double baseMultiplier = 1.0;
            
            // 工作时间偏好调整
            if (currentHour >= 9 && currentHour <= 17) {
                if (item.category.contains("work") || item.category.contains("productivity")) {
                    baseMultiplier *= 1.3;
                }
            }
            
            // 晚间娱乐偏好调整
            if (currentHour >= 19 && currentHour <= 23) {
                if (item.category.contains("entertainment") || item.category.contains("video")) {
                    baseMultiplier *= 1.2;
                }
            }
            
            return baseMultiplier;
        }
    }
    
    // 公共API方法
    
    /**
     * 获取个性化推荐
     */
    public List<RecommendationItem> getPersonalizedRecommendations(@Nullable String query, int maxResults) {
        List<RecommendationItem> allRecommendations = new ArrayList<>();
        
        try {
            // 1. 基于内容的推荐
            List<String> userHistory = getUserHistory();
            List<RecommendationItem> contentBased = mContentBasedFilter.recommend(
                query != null ? query : "", userHistory, maxResults / 2);
            allRecommendations.addAll(contentBased);
            
            // 2. 协同过滤推荐
            List<RecommendationItem> collaborative = mCollaborativeFilter.recommend(
                "current_user", maxResults / 4);
            allRecommendations.addAll(collaborative);
            
            // 3. 趋势推荐
            List<RecommendationItem> trending = mTrendingAnalyzer.getTrendingRecommendations(maxResults / 4);
            allRecommendations.addAll(trending);
            
            // 4. 去重和混合
            List<RecommendationItem> uniqueRecommendations = deduplicateRecommendations(allRecommendations);
            
            // 5. 时间优化
            List<RecommendationItem> optimized = mTimeBasedOptimizer.optimizeForCurrentTime(uniqueRecommendations);
            
            // 6. 最终排序和截取
            return optimized.subList(0, Math.min(optimized.size(), maxResults));
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating personalized recommendations", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 记录用户交互
     */
    public void recordUserInteraction(@NonNull String itemId, @NonNull String action, 
                                    long timeSpent, double rating) {
        mExecutor.execute(() -> {
            try {
                UserInteraction interaction = mUserInteractions.get(itemId);
                if (interaction == null) {
                    interaction = new UserInteraction(itemId);
                    mUserInteractions.put(itemId, interaction);
                }
                
                interaction.recordInteraction(action, timeSpent, rating);
                
                // 更新用户画像
                RecommendationItem item = mRecommendationCache.get(itemId);
                if (item != null) {
                    List<String> keywords = List.of(extractKeywords(item.title));
                    mUserProfile.updatePreferences(item.category, keywords, rating);
                }
                
                // 记录到行为分析器
                if ("click".equals(action) && item != null) {
                    mBehaviorAnalyzer.recordSearch(item.title, 0, true, item.url);
                    mBehaviorAnalyzer.recordDomainVisit(item.url, timeSpent, rating);
                }
                
                Log.d(TAG, "Recorded interaction: " + itemId + " - " + action);
                
            } catch (Exception e) {
                Log.e(TAG, "Error recording user interaction", e);
            }
        });
    }
    
    /**
     * 添加推荐内容
     */
    public void addRecommendationContent(@NonNull RecommendationItem item) {
        mRecommendationCache.put(item.id, item);
        
        // 限制缓存大小
        while (mRecommendationCache.size() > RECOMMENDATION_CACHE_SIZE) {
            String oldestId = mRecommendationCache.entrySet().stream()
                .min(Map.Entry.comparingByValue((a, b) -> Long.compare(a.timestamp, b.timestamp)))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (oldestId != null) {
                mRecommendationCache.remove(oldestId);
            }
        }
    }
    
    /**
     * 获取推荐性能报告
     */
    public String getRecommendationReport() {
        StringBuilder report = new StringBuilder();
        
        try {
            report.append("=== 个性化推荐引擎报告 ===\n\n");
            
            // 基本统计
            report.append("推荐内容总数: ").append(mRecommendationCache.size()).append("\n");
            report.append("用户交互记录: ").append(mUserInteractions.size()).append("\n");
            report.append("主要兴趣分类: ").append(mUserProfile.primaryCategory).append("\n\n");
            
            // 推荐效果统计
            double totalCTR = mRecommendationCache.values().stream()
                .mapToDouble(RecommendationItem::getClickThroughRate)
                .average().orElse(0.0);
            report.append("平均点击率: ").append(String.format("%.2f%%", totalCTR * 100)).append("\n");
            
            // 分类偏好Top5
            report.append("\n用户偏好分类:\n");
            mUserProfile.categoryPreferences.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> report.append("  ")
                    .append(entry.getKey()).append(": ")
                    .append(String.format("%.1f", entry.getValue())).append("\n"));
            
            // 时间使用模式
            report.append("\n活跃时段分布:\n");
            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            report.append("  当前时段(").append(currentHour).append("时)活跃度: ")
                .append(String.format("%.1f", mUserProfile.getTimeRelevance())).append("\n");
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating recommendation report", e);
            report.append("报告生成失败: ").append(e.getMessage());
        }
        
        return report.toString();
    }
    
    // 私有辅助方法
    
    private List<String> getUserHistory() {
        // 从用户行为分析器获取历史数据
        return mBehaviorAnalyzer.getPersonalizedSuggestions("", 50);
    }
    
    private List<RecommendationItem> deduplicateRecommendations(List<RecommendationItem> recommendations) {
        Map<String, RecommendationItem> uniqueItems = new HashMap<>();
        
        for (RecommendationItem item : recommendations) {
            if (!uniqueItems.containsKey(item.id) || item.score > uniqueItems.get(item.id).score) {
                uniqueItems.put(item.id, item);
            }
        }
        
        return new ArrayList<>(uniqueItems.values());
    }
    
    private String[] extractKeywords(String text) {
        // 简单的关键词提取（实际项目中可能需要更复杂的NLP处理）
        return text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff\\s]", " ")
            .split("\\s+");
    }
    
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        text1 = text1.toLowerCase();
        text2 = text2.toLowerCase();
        
        if (text1.contains(text2) || text2.contains(text1)) {
            return 0.8;
        }
        
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    commonWords++;
                    break;
                }
            }
        }
        
        return (double) commonWords / Math.max(words1.length, words2.length);
    }
    
    // 数据持久化方法
    
    private void loadRecommendationData() {
        mExecutor.execute(() -> {
            try {
                loadRecommendationCache();
                loadUserProfile();
                Log.d(TAG, "Recommendation data loaded");
            } catch (Exception e) {
                Log.e(TAG, "Error loading recommendation data", e);
            }
        });
    }
    
    private void loadRecommendationCache() {
        try {
            String data = mPrefs.getString(KEY_RECOMMENDATION_HISTORY, "{}");
            JSONObject json = new JSONObject(data);
            
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String id = keys.next();
                JSONObject itemJson = json.getJSONObject(id);
                RecommendationItem item = RecommendationItem.fromJson(itemJson);
                if (item != null) {
                    mRecommendationCache.put(id, item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading recommendation cache", e);
        }
    }
    
    private void loadUserProfile() {
        try {
            String data = mPrefs.getString(KEY_USER_PROFILE, "{}");
            JSONObject json = new JSONObject(data);
            
            // 加载分类偏好
            JSONObject categoryPrefs = json.optJSONObject("categoryPreferences");
            if (categoryPrefs != null) {
                Iterator<String> keys = categoryPrefs.keys();
                while (keys.hasNext()) {
                    String category = keys.next();
                    mUserProfile.categoryPreferences.put(category, categoryPrefs.getDouble(category));
                }
            }
            
            // 加载其他用户画像数据
            mUserProfile.explorationTendency = json.optDouble("explorationTendency", 0.3);
            mUserProfile.qualityThreshold = json.optDouble("qualityThreshold", 0.6);
            mUserProfile.primaryCategory = json.optString("primaryCategory", "general");
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading user profile", e);
        }
    }
    
    private void saveRecommendationData() {
        mExecutor.execute(() -> {
            try {
                saveRecommendationCache();
                saveUserProfile();
            } catch (Exception e) {
                Log.e(TAG, "Error saving recommendation data", e);
            }
        });
    }
    
    private void saveRecommendationCache() {
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, RecommendationItem> entry : mRecommendationCache.entrySet()) {
                json.put(entry.getKey(), entry.getValue().toJson());
            }
            mPrefs.edit().putString(KEY_RECOMMENDATION_HISTORY, json.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving recommendation cache", e);
        }
    }
    
    private void saveUserProfile() {
        try {
            JSONObject json = new JSONObject();
            
            JSONObject categoryPrefs = new JSONObject();
            for (Map.Entry<String, Double> entry : mUserProfile.categoryPreferences.entrySet()) {
                categoryPrefs.put(entry.getKey(), entry.getValue());
            }
            json.put("categoryPreferences", categoryPrefs);
            
            json.put("explorationTendency", mUserProfile.explorationTendency);
            json.put("qualityThreshold", mUserProfile.qualityThreshold);
            json.put("primaryCategory", mUserProfile.primaryCategory);
            
            mPrefs.edit().putString(KEY_USER_PROFILE, json.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving user profile", e);
        }
    }
    
    private void startPeriodicRecommendationUpdate() {
        // 每30分钟更新一次推荐
        mExecutor.execute(() -> {
            try {
                Thread.sleep(30 * 60 * 1000); // 30分钟
                
                // 清理过期推荐
                cleanupExpiredRecommendations();
                
                // 保存数据
                saveRecommendationData();
                
                Log.d(TAG, "Periodic recommendation update completed");
                
                // 递归调用实现周期性更新
                startPeriodicRecommendationUpdate();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e(TAG, "Error in periodic recommendation update", e);
            }
        });
    }
    
    private void cleanupExpiredRecommendations() {
        long expirationTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // 7天
        
        mRecommendationCache.entrySet().removeIf(entry -> 
            entry.getValue().timestamp < expirationTime);
        
        mUserInteractions.entrySet().removeIf(entry -> 
            entry.getValue().lastInteractionTime < expirationTime);
    }
}