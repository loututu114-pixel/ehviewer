package com.hippo.ehviewer.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 用户行为分析器
 * 分析用户使用模式，预测最佳拉活时机，优化应用体验
 */
public class UserBehaviorAnalyzer {
    
    private static final String TAG = "UserBehaviorAnalyzer";
    private static final String PREFS_NAME = "user_behavior_analytics";
    private static final String KEY_USAGE_PATTERNS = "usage_patterns";
    private static final String KEY_SESSION_DATA = "session_data";
    private static final String KEY_FEATURE_USAGE = "feature_usage";
    private static final String KEY_USER_PREFERENCES = "user_preferences";
    
    // 分析周期（天）
    private static final int ANALYSIS_PERIOD_DAYS = 30;
    private static final int MAX_SESSION_RECORDS = 1000;
    
    private final Context context;
    private final SharedPreferences prefs;
    private final ScheduledExecutorService executor;
    private final Map<String, Object> currentSession;
    
    // 用户行为数据
    private final Map<String, FeatureUsageStats> featureUsage;
    private final List<SessionRecord> recentSessions;
    private final Map<Integer, List<Long>> hourlyUsagePattern; // 小时 -> 使用时间戳列表
    private final Map<Integer, List<Long>> weeklyUsagePattern; // 星期 -> 使用时间戳列表
    
    private long sessionStartTime;
    private boolean isAnalyzing = false;
    
    private static UserBehaviorAnalyzer instance;
    
    public static synchronized UserBehaviorAnalyzer getInstance(Context context) {
        if (instance == null) {
            instance = new UserBehaviorAnalyzer(context);
        }
        return instance;
    }
    
    private UserBehaviorAnalyzer(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.currentSession = new ConcurrentHashMap<>();
        this.featureUsage = new ConcurrentHashMap<>();
        this.recentSessions = new ArrayList<>();
        this.hourlyUsagePattern = new ConcurrentHashMap<>();
        this.weeklyUsagePattern = new ConcurrentHashMap<>();
        
        // 初始化小时和星期模式
        for (int i = 0; i < 24; i++) {
            hourlyUsagePattern.put(i, new ArrayList<>());
        }
        for (int i = 1; i <= 7; i++) { // 1=周日, 7=周六
            weeklyUsagePattern.put(i, new ArrayList<>());
        }
        
        loadExistingData();
        startPeriodicAnalysis();
        
        Log.i(TAG, "User Behavior Analyzer initialized");
    }
    
    /**
     * 开始会话
     */
    public void startSession() {
        sessionStartTime = System.currentTimeMillis();
        currentSession.clear();
        currentSession.put("start_time", sessionStartTime);
        currentSession.put("features_used", new HashSet<String>());
        
        // 记录启动时间模式
        recordUsageTime(sessionStartTime);
        
        Log.d(TAG, "Session started");
    }
    
    /**
     * 结束会话
     */
    public void endSession() {
        if (sessionStartTime == 0) return;
        
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        currentSession.put("end_time", System.currentTimeMillis());
        currentSession.put("duration", sessionDuration);
        
        // 保存会话记录
        saveSessionRecord();
        
        // 异步分析
        executor.submit(this::performSessionAnalysis);
        
        sessionStartTime = 0;
        Log.d(TAG, "Session ended, duration: " + sessionDuration + "ms");
    }
    
    /**
     * 记录功能使用
     */
    public void recordFeatureUsage(String featureName) {
        recordFeatureUsage(featureName, null);
    }

    /**
     * 跟踪事件（兼容性方法）
     */
    public static void trackEvent(String eventName) {
        getInstance(null).recordFeatureUsage(eventName);
    }

    /**
     * 跟踪事件（带参数）
     */
    public static void trackEvent(String eventName, String param1, String value1) {
        getInstance(null).recordFeatureUsage(eventName, java.util.Collections.singletonMap(param1, value1));
    }

    /**
     * 跟踪事件（带两个参数）
     */
    public static void trackEvent(String eventName, String param1, String value1, String param2, String value2) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put(param1, value1);
        params.put(param2, value2);
        getInstance(null).recordFeatureUsage(eventName, params);
    }

    /**
     * 跟踪事件（带三个参数）
     */
    public static void trackEvent(String eventName, String param1, String value1, String param2, String value2, String param3, String value3) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put(param1, value1);
        params.put(param2, value2);
        params.put(param3, value3);
        getInstance(null).recordFeatureUsage(eventName, params);
    }

    /**
     * 跟踪事件（带四个参数）
     */
    public static void trackEvent(String eventName, String param1, String value1, String param2, String value2, String param3, String value3, String param4, String value4) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put(param1, value1);
        params.put(param2, value2);
        params.put(param3, value3);
        params.put(param4, value4);
        getInstance(null).recordFeatureUsage(eventName, params);
    }
    
    /**
     * 记录功能使用（带参数）
     */
    public void recordFeatureUsage(String featureName, Map<String, Object> parameters) {
        long currentTime = System.currentTimeMillis();
        
        // 更新当前会话
        @SuppressWarnings("unchecked")
        Set<String> featuresUsed = (Set<String>) currentSession.get("features_used");
        if (featuresUsed != null) {
            featuresUsed.add(featureName);
        }
        
        // 更新功能使用统计
        FeatureUsageStats stats = featureUsage.computeIfAbsent(featureName, 
            k -> new FeatureUsageStats(featureName));
        stats.recordUsage(currentTime, parameters);
        
        Log.d(TAG, "Feature usage recorded: " + featureName);
    }
    
    /**
     * 记录用户偏好
     */
    public void recordUserPreference(String key, Object value) {
        try {
            JSONObject preferences = getUserPreferences();
            preferences.put(key, value);
            preferences.put("updated_at", System.currentTimeMillis());
            
            prefs.edit()
                .putString(KEY_USER_PREFERENCES, preferences.toString())
                .apply();
                
            Log.d(TAG, "User preference recorded: " + key + " = " + value);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to record user preference", e);
        }
    }
    
    /**
     * 获取用户偏好
     */
    public JSONObject getUserPreferences() {
        String prefsJson = prefs.getString(KEY_USER_PREFERENCES, "{}");
        try {
            return new JSONObject(prefsJson);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse user preferences", e);
            return new JSONObject();
        }
    }
    
    /**
     * 记录使用时间模式
     */
    private void recordUsageTime(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // 记录小时模式
        List<Long> hourlyTimes = hourlyUsagePattern.get(hour);
        if (hourlyTimes != null) {
            hourlyTimes.add(timestamp);
            // 保持最近30天的数据
            long thirtyDaysAgo = timestamp - (30L * 24 * 60 * 60 * 1000);
            hourlyTimes.removeIf(time -> time < thirtyDaysAgo);
        }
        
        // 记录星期模式
        List<Long> weeklyTimes = weeklyUsagePattern.get(dayOfWeek);
        if (weeklyTimes != null) {
            weeklyTimes.add(timestamp);
            long thirtyDaysAgo = timestamp - (30L * 24 * 60 * 60 * 1000);
            weeklyTimes.removeIf(time -> time < thirtyDaysAgo);
        }
    }
    
    /**
     * 保存会话记录
     */
    private void saveSessionRecord() {
        try {
            SessionRecord record = new SessionRecord(
                (Long) currentSession.get("start_time"),
                (Long) currentSession.get("end_time"),
                (Long) currentSession.get("duration"),
                (Set<String>) currentSession.get("features_used")
            );
            
            recentSessions.add(record);
            
            // 保持最近的会话记录
            if (recentSessions.size() > MAX_SESSION_RECORDS) {
                recentSessions.remove(0);
            }
            
            // 异步保存到SharedPreferences
            executor.submit(this::persistSessionData);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to save session record", e);
        }
    }
    
    /**
     * 持久化会话数据
     */
    private void persistSessionData() {
        try {
            JSONArray sessionsArray = new JSONArray();
            for (SessionRecord record : recentSessions) {
                sessionsArray.put(record.toJson());
            }
            
            prefs.edit()
                .putString(KEY_SESSION_DATA, sessionsArray.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "Failed to persist session data", e);
        }
    }
    
    /**
     * 加载现有数据
     */
    private void loadExistingData() {
        try {
            // 加载会话数据
            String sessionDataJson = prefs.getString(KEY_SESSION_DATA, "[]");
            JSONArray sessionsArray = new JSONArray(sessionDataJson);
            
            recentSessions.clear();
            for (int i = 0; i < sessionsArray.length(); i++) {
                JSONObject sessionObj = sessionsArray.getJSONObject(i);
                recentSessions.add(SessionRecord.fromJson(sessionObj));
            }
            
            // 重建使用模式
            for (SessionRecord record : recentSessions) {
                recordUsageTime(record.startTime);
            }
            
            Log.d(TAG, "Loaded " + recentSessions.size() + " session records");
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load existing data", e);
        }
    }
    
    /**
     * 开始定期分析
     */
    private void startPeriodicAnalysis() {
        // 每小时进行一次分析
        executor.scheduleAtFixedRate(this::performPeriodicAnalysis, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * 执行会话分析
     */
    private void performSessionAnalysis() {
        if (isAnalyzing) return;
        
        isAnalyzing = true;
        try {
            // 分析最近的会话模式
            analyzeRecentSessionPatterns();
            
            // 分析功能使用模式
            analyzeFeatureUsagePatterns();
            
            // 预测下次使用时间
            predictNextUsage();
            
        } finally {
            isAnalyzing = false;
        }
    }
    
    /**
     * 执行定期分析
     */
    private void performPeriodicAnalysis() {
        if (isAnalyzing) return;
        
        isAnalyzing = true;
        try {
            // 清理旧数据
            cleanupOldData();
            
            // 分析长期趋势
            analyzeLongTermTrends();
            
            // 生成用户画像
            generateUserProfile();
            
            // 优化推荐
            optimizeRecommendations();
            
        } finally {
            isAnalyzing = false;
        }
    }
    
    /**
     * 分析最近会话模式
     */
    private void analyzeRecentSessionPatterns() {
        if (recentSessions.size() < 3) return;
        
        // 计算平均会话时长
        long totalDuration = recentSessions.stream()
            .mapToLong(record -> record.duration)
            .sum();
        long avgDuration = totalDuration / recentSessions.size();
        
        // 分析会话间隔
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < recentSessions.size(); i++) {
            long interval = recentSessions.get(i).startTime - recentSessions.get(i - 1).endTime;
            intervals.add(interval);
        }
        
        // 保存分析结果
        try {
            JSONObject patterns = new JSONObject();
            patterns.put("avg_session_duration", avgDuration);
            patterns.put("session_count", recentSessions.size());
            patterns.put("avg_interval", intervals.stream().mapToLong(Long::longValue).average().orElse(0));
            patterns.put("analyzed_at", System.currentTimeMillis());
            
            prefs.edit()
                .putString(KEY_USAGE_PATTERNS, patterns.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save session patterns", e);
        }
        
        Log.d(TAG, "Session patterns analyzed: avg_duration=" + avgDuration + "ms, sessions=" + recentSessions.size());
    }
    
    /**
     * 分析功能使用模式
     */
    private void analyzeFeatureUsagePatterns() {
        try {
            JSONObject featureStats = new JSONObject();
            
            for (Map.Entry<String, FeatureUsageStats> entry : featureUsage.entrySet()) {
                FeatureUsageStats stats = entry.getValue();
                JSONObject featureData = new JSONObject();
                featureData.put("usage_count", stats.usageCount);
                featureData.put("first_used", stats.firstUsed);
                featureData.put("last_used", stats.lastUsed);
                featureData.put("avg_daily_usage", stats.getAverageDailyUsage());
                
                featureStats.put(entry.getKey(), featureData);
            }
            
            prefs.edit()
                .putString(KEY_FEATURE_USAGE, featureStats.toString())
                .apply();
                
            Log.d(TAG, "Feature usage patterns analyzed for " + featureUsage.size() + " features");
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save feature usage patterns", e);
        }
    }
    
    /**
     * 预测下次使用时间
     */
    private void predictNextUsage() {
        // 基于历史模式预测用户最可能使用应用的时间
        Map<Integer, Double> hourlyProbability = calculateHourlyUsageProbability();
        Map<Integer, Double> weeklyProbability = calculateWeeklyUsageProbability();
        
        // 找到概率最高的时间段
        int mostLikelyHour = Collections.max(hourlyProbability.entrySet(), 
            Map.Entry.comparingByValue()).getKey();
        int mostLikelyDay = Collections.max(weeklyProbability.entrySet(), 
            Map.Entry.comparingByValue()).getKey();
            
        // 计算下次预测使用时间
        Calendar nextUsage = Calendar.getInstance();
        nextUsage.set(Calendar.HOUR_OF_DAY, mostLikelyHour);
        nextUsage.set(Calendar.MINUTE, 0);
        nextUsage.set(Calendar.SECOND, 0);
        
        // 如果预测时间已经过了，移到明天
        if (nextUsage.getTimeInMillis() <= System.currentTimeMillis()) {
            nextUsage.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // 保存预测结果
        recordUserPreference("predicted_next_usage", nextUsage.getTimeInMillis());
        recordUserPreference("most_likely_hour", mostLikelyHour);
        recordUserPreference("most_likely_day", mostLikelyDay);
        
        Log.d(TAG, "Next usage predicted for: " + nextUsage.getTime() + 
              " (hour=" + mostLikelyHour + ", day=" + mostLikelyDay + ")");
    }
    
    /**
     * 计算每小时使用概率
     */
    private Map<Integer, Double> calculateHourlyUsageProbability() {
        Map<Integer, Double> probability = new HashMap<>();
        int totalUsage = 0;
        
        // 计算总使用次数
        for (List<Long> times : hourlyUsagePattern.values()) {
            totalUsage += times.size();
        }
        
        if (totalUsage == 0) {
            // 如果没有历史数据，返回平均概率
            for (int i = 0; i < 24; i++) {
                probability.put(i, 1.0 / 24.0);
            }
        } else {
            // 计算每小时的概率
            for (int hour = 0; hour < 24; hour++) {
                List<Long> times = hourlyUsagePattern.get(hour);
                double prob = times != null ? (double) times.size() / totalUsage : 0.0;
                probability.put(hour, prob);
            }
        }
        
        return probability;
    }
    
    /**
     * 计算每周使用概率
     */
    private Map<Integer, Double> calculateWeeklyUsageProbability() {
        Map<Integer, Double> probability = new HashMap<>();
        int totalUsage = 0;
        
        for (List<Long> times : weeklyUsagePattern.values()) {
            totalUsage += times.size();
        }
        
        if (totalUsage == 0) {
            for (int i = 1; i <= 7; i++) {
                probability.put(i, 1.0 / 7.0);
            }
        } else {
            for (int day = 1; day <= 7; day++) {
                List<Long> times = weeklyUsagePattern.get(day);
                double prob = times != null ? (double) times.size() / totalUsage : 0.0;
                probability.put(day, prob);
            }
        }
        
        return probability;
    }
    
    /**
     * 清理旧数据
     */
    private void cleanupOldData() {
        long cutoffTime = System.currentTimeMillis() - (ANALYSIS_PERIOD_DAYS * 24L * 60 * 60 * 1000);
        
        // 清理旧会话
        recentSessions.removeIf(record -> record.startTime < cutoffTime);
        
        // 清理旧的使用时间
        for (List<Long> times : hourlyUsagePattern.values()) {
            times.removeIf(time -> time < cutoffTime);
        }
        
        for (List<Long> times : weeklyUsagePattern.values()) {
            times.removeIf(time -> time < cutoffTime);
        }
        
        // 持久化清理后的数据
        persistSessionData();
    }
    
    /**
     * 分析长期趋势
     */
    private void analyzeLongTermTrends() {
        // 分析使用频率趋势、功能偏好变化等
        // 这里可以实现更复杂的趋势分析算法
    }
    
    /**
     * 生成用户画像
     */
    private void generateUserProfile() {
        try {
            JSONObject profile = new JSONObject();
            
            // 基本统计
            profile.put("total_sessions", recentSessions.size());
            profile.put("avg_session_duration", getAverageSessionDuration());
            profile.put("most_active_hour", getMostActiveHour());
            profile.put("most_active_day", getMostActiveDay());
            profile.put("favorite_features", getFavoriteFeatures());
            
            // 使用模式
            profile.put("usage_pattern", getUserUsagePattern());
            profile.put("engagement_level", getEngagementLevel());
            
            recordUserPreference("user_profile", profile.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to generate user profile", e);
        }
    }
    
    /**
     * 优化推荐
     */
    private void optimizeRecommendations() {
        // 基于用户行为生成个性化推荐
        // 例如：推荐合适的保活策略、功能提醒等
    }
    
    /**
     * 获取平均会话时长
     */
    private long getAverageSessionDuration() {
        if (recentSessions.isEmpty()) return 0;
        return recentSessions.stream()
            .mapToLong(record -> record.duration)
            .sum() / recentSessions.size();
    }
    
    /**
     * 获取最活跃小时
     */
    private int getMostActiveHour() {
        return Collections.max(hourlyUsagePattern.entrySet(),
            (e1, e2) -> Integer.compare(e1.getValue().size(), e2.getValue().size())
        ).getKey();
    }
    
    /**
     * 获取最活跃日期
     */
    private int getMostActiveDay() {
        return Collections.max(weeklyUsagePattern.entrySet(),
            (e1, e2) -> Integer.compare(e1.getValue().size(), e2.getValue().size())
        ).getKey();
    }
    
    /**
     * 获取最喜欢的功能
     */
    private JSONArray getFavoriteFeatures() {
        JSONArray favorites = new JSONArray();
        featureUsage.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().usageCount, e1.getValue().usageCount))
            .limit(5)
            .forEach(entry -> favorites.put(entry.getKey()));
        return favorites;
    }
    
    /**
     * 获取使用模式
     */
    private String getUserUsagePattern() {
        long avgDuration = getAverageSessionDuration();
        int sessionCount = recentSessions.size();
        
        if (sessionCount < 5) return "新用户";
        if (avgDuration > 30 * 60 * 1000) return "深度用户"; // 超过30分钟
        if (sessionCount > 50) return "频繁用户";
        if (avgDuration > 5 * 60 * 1000) return "常规用户"; // 超过5分钟
        return "轻度用户";
    }
    
    /**
     * 获取参与度等级
     */
    private String getEngagementLevel() {
        int uniqueFeatures = featureUsage.size();
        int totalUsage = featureUsage.values().stream()
            .mapToInt(stats -> (int) stats.usageCount)
            .sum();
            
        if (uniqueFeatures > 10 && totalUsage > 100) return "高";
        if (uniqueFeatures > 5 && totalUsage > 20) return "中";
        return "低";
    }
    
    /**
     * 获取使用统计摘要
     */
    public UserBehaviorSummary getBehaviorSummary() {
        return new UserBehaviorSummary(
            recentSessions.size(),
            getAverageSessionDuration(),
            getMostActiveHour(),
            getMostActiveDay(),
            featureUsage.size(),
            getUserUsagePattern(),
            getEngagementLevel()
        );
    }
    
    /**
     * 功能使用统计
     */
    private static class FeatureUsageStats {
        final String featureName;
        long usageCount = 0;
        long firstUsed = 0;
        long lastUsed = 0;
        final List<Long> usageHistory = new ArrayList<>();
        
        FeatureUsageStats(String featureName) {
            this.featureName = featureName;
        }
        
        void recordUsage(long timestamp, Map<String, Object> parameters) {
            usageCount++;
            if (firstUsed == 0) {
                firstUsed = timestamp;
            }
            lastUsed = timestamp;
            usageHistory.add(timestamp);
            
            // 保持最近1000次使用记录
            if (usageHistory.size() > 1000) {
                usageHistory.remove(0);
            }
        }
        
        double getAverageDailyUsage() {
            if (firstUsed == 0) return 0;
            long daysSinceFirst = (System.currentTimeMillis() - firstUsed) / (24 * 60 * 60 * 1000);
            return daysSinceFirst > 0 ? (double) usageCount / daysSinceFirst : usageCount;
        }
    }
    
    /**
     * 会话记录
     */
    private static class SessionRecord {
        final long startTime;
        final long endTime;
        final long duration;
        final Set<String> featuresUsed;
        
        SessionRecord(long startTime, long endTime, long duration, Set<String> featuresUsed) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.featuresUsed = new HashSet<>(featuresUsed != null ? featuresUsed : Collections.emptySet());
        }
        
        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("start_time", startTime);
            json.put("end_time", endTime);
            json.put("duration", duration);
            json.put("features_used", new JSONArray(featuresUsed));
            return json;
        }
        
        static SessionRecord fromJson(JSONObject json) throws JSONException {
            Set<String> features = new HashSet<>();
            JSONArray featuresArray = json.getJSONArray("features_used");
            for (int i = 0; i < featuresArray.length(); i++) {
                features.add(featuresArray.getString(i));
            }
            
            return new SessionRecord(
                json.getLong("start_time"),
                json.getLong("end_time"),
                json.getLong("duration"),
                features
            );
        }
    }
    
    /**
     * 用户行为摘要
     */
    public static class UserBehaviorSummary {
        public final int totalSessions;
        public final long averageSessionDuration;
        public final int mostActiveHour;
        public final int mostActiveDay;
        public final int uniqueFeatures;
        public final String usagePattern;
        public final String engagementLevel;
        
        UserBehaviorSummary(int totalSessions, long averageSessionDuration, 
                           int mostActiveHour, int mostActiveDay, int uniqueFeatures,
                           String usagePattern, String engagementLevel) {
            this.totalSessions = totalSessions;
            this.averageSessionDuration = averageSessionDuration;
            this.mostActiveHour = mostActiveHour;
            this.mostActiveDay = mostActiveDay;
            this.uniqueFeatures = uniqueFeatures;
            this.usagePattern = usagePattern;
            this.engagementLevel = engagementLevel;
        }
    }
    
    /**
     * 停止分析器
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        instance = null;
    }
}