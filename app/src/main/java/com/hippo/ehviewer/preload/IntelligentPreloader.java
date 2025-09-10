package com.hippo.ehviewer.preload;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;
import com.hippo.ehviewer.cache.NetworkCacheManager;
import com.hippo.ehviewer.recommendation.PersonalizedRecommendationEngine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 智能预加载系统
 * 基于用户行为分析和机器学习预测，智能预加载用户可能访问的内容
 */
public class IntelligentPreloader {
    
    private static final String TAG = "IntelligentPreloader";
    private static final String PREFS_NAME = "intelligent_preloader";
    private static final String KEY_PRELOAD_STATS = "preload_stats";
    private static final String KEY_PRELOAD_RULES = "preload_rules";
    private static final String KEY_PRELOAD_QUEUE = "preload_queue";
    
    // 预加载配置
    private static final int MAX_CONCURRENT_PRELOADS = 3;
    private static final int MAX_PRELOAD_QUEUE_SIZE = 50;
    private static final int PRELOAD_TIMEOUT_MS = 30000; // 30秒超时
    private static final double MIN_PREDICTION_CONFIDENCE = 0.6; // 最小预测置信度
    
    // 资源限制
    private static final int MIN_BATTERY_LEVEL = 20;    // 最低电量要求
    private static final double MAX_MEMORY_USAGE = 0.8; // 最大内存使用率
    private static final long MAX_DAILY_PRELOAD_DATA = 100 * 1024 * 1024; // 100MB日限制
    
    // 单例实例
    private static volatile IntelligentPreloader sInstance;
    
    // 核心组件
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final ExecutorService mPreloadExecutor;
    private final ScheduledExecutorService mScheduler;
    
    // 依赖组件
    private final UserBehaviorAnalyzer mBehaviorAnalyzer;
    private final PersonalizedRecommendationEngine mRecommendationEngine;
    private final NetworkCacheManager mNetworkCache;
    
    // 预加载管理
    private final Queue<PreloadTask> mPreloadQueue;
    private final Map<String, PreloadTask> mActivePreloads;
    private final PreloadStatistics mStats;
    private final PreloadRuleEngine mRuleEngine;
    
    // 系统状态监控
    private final SystemResourceMonitor mResourceMonitor;
    private final AtomicInteger mConcurrentPreloads;
    
    private IntelligentPreloader(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mPreloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_PRELOADS);
        mScheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 初始化依赖组件
        mBehaviorAnalyzer = UserBehaviorAnalyzer.getInstance(context);
        mRecommendationEngine = PersonalizedRecommendationEngine.getInstance(context);
        mNetworkCache = NetworkCacheManager.getInstance(context);
        
        // 初始化预加载管理
        mPreloadQueue = new ConcurrentLinkedQueue<>();
        mActivePreloads = new HashMap<>();
        mStats = new PreloadStatistics();
        mRuleEngine = new PreloadRuleEngine();
        
        // 初始化系统监控
        mResourceMonitor = new SystemResourceMonitor();
        mConcurrentPreloads = new AtomicInteger(0);
        
        // 加载历史数据
        loadPreloadData();
        
        // 启动定期预测和预加载
        startPreloadScheduler();
    }
    
    public static IntelligentPreloader getInstance(Context context) {
        if (sInstance == null) {
            synchronized (IntelligentPreloader.class) {
                if (sInstance == null) {
                    sInstance = new IntelligentPreloader(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 预加载任务数据类
     */
    public static class PreloadTask {
        public final String id;
        public final String url;
        public final String title;
        public final PreloadType type;
        public final PreloadPriority priority;
        public double predictionConfidence; // 预测置信度
        public long estimatedSize;          // 预估大小（字节）
        public long createdTime;            // 创建时间
        public long startTime;              // 开始时间
        public long completedTime;          // 完成时间
        public PreloadStatus status;        // 状态
        public String errorMessage;         // 错误信息
        public final Map<String, Object> metadata; // 元数据
        
        public PreloadTask(String id, String url, String title, PreloadType type, PreloadPriority priority) {
            this.id = id;
            this.url = url;
            this.title = title;
            this.type = type;
            this.priority = priority;
            this.predictionConfidence = 0.0;
            this.estimatedSize = 0;
            this.createdTime = System.currentTimeMillis();
            this.startTime = 0;
            this.completedTime = 0;
            this.status = PreloadStatus.PENDING;
            this.errorMessage = null;
            this.metadata = new HashMap<>();
        }
        
        public long getDuration() {
            if (startTime == 0) return 0;
            long endTime = completedTime > 0 ? completedTime : System.currentTimeMillis();
            return endTime - startTime;
        }
        
        public boolean isExpired(long expirationTime) {
            return System.currentTimeMillis() - createdTime > expirationTime;
        }
        
        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("url", url);
                json.put("title", title);
                json.put("type", type.name());
                json.put("priority", priority.name());
                json.put("predictionConfidence", predictionConfidence);
                json.put("estimatedSize", estimatedSize);
                json.put("createdTime", createdTime);
                json.put("status", status.name());
                
                JSONObject metadataJson = new JSONObject();
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    metadataJson.put(entry.getKey(), entry.getValue());
                }
                json.put("metadata", metadataJson);
                
                return json;
            } catch (Exception e) {
                Log.e(TAG, "Error converting PreloadTask to JSON", e);
                return new JSONObject();
            }
        }
        
        public static PreloadTask fromJson(JSONObject json) {
            try {
                String id = json.getString("id");
                String url = json.getString("url");
                String title = json.getString("title");
                PreloadType type = PreloadType.valueOf(json.getString("type"));
                PreloadPriority priority = PreloadPriority.valueOf(json.getString("priority"));
                
                PreloadTask task = new PreloadTask(id, url, title, type, priority);
                task.predictionConfidence = json.optDouble("predictionConfidence", 0.0);
                task.estimatedSize = json.optLong("estimatedSize", 0);
                task.createdTime = json.optLong("createdTime", System.currentTimeMillis());
                task.status = PreloadStatus.valueOf(json.optString("status", "PENDING"));
                
                return task;
            } catch (Exception e) {
                Log.e(TAG, "Error parsing PreloadTask from JSON", e);
                return null;
            }
        }
    }
    
    /**
     * 预加载类型
     */
    public enum PreloadType {
        WEB_PAGE("网页内容"),
        IMAGE("图片"),
        VIDEO("视频"),
        SCRIPT("脚本"),
        API_DATA("API数据"),
        SEARCH_RESULT("搜索结果");
        
        private final String displayName;
        
        PreloadType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 预加载优先级
     */
    public enum PreloadPriority {
        CRITICAL(4),    // 关键内容
        HIGH(3),        // 高优先级
        MEDIUM(2),      // 中优先级
        LOW(1),         // 低优先级
        BACKGROUND(0);  // 后台预加载
        
        private final int value;
        
        PreloadPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * 预加载状态
     */
    public enum PreloadStatus {
        PENDING,     // 等待中
        IN_PROGRESS, // 执行中
        COMPLETED,   // 已完成
        FAILED,      // 失败
        CANCELLED,   // 已取消
        EXPIRED      // 已过期
    }
    
    /**
     * 预加载统计数据
     */
    public static class PreloadStatistics {
        public int totalTasks = 0;
        public int successfulTasks = 0;
        public int failedTasks = 0;
        public long totalDataPreloaded = 0; // 字节
        public long totalTimeSpent = 0;     // 毫秒
        public double averageAccuracy = 0.0; // 预测准确率
        public long dailyDataUsed = 0;       // 今日使用流量
        public long lastResetTime = System.currentTimeMillis(); // 最后重置时间
        
        public double getSuccessRate() {
            return totalTasks > 0 ? (double) successfulTasks / totalTasks : 0.0;
        }
        
        public long getAveragePreloadTime() {
            return successfulTasks > 0 ? totalTimeSpent / successfulTasks : 0;
        }
        
        public void recordTask(PreloadTask task, boolean wasUseful) {
            totalTasks++;
            
            if (task.status == PreloadStatus.COMPLETED) {
                successfulTasks++;
                totalDataPreloaded += task.estimatedSize;
                totalTimeSpent += task.getDuration();
                
                // 更新日流量
                Calendar now = Calendar.getInstance();
                Calendar lastReset = Calendar.getInstance();
                lastReset.setTimeInMillis(lastResetTime);
                
                if (now.get(Calendar.DAY_OF_YEAR) != lastReset.get(Calendar.DAY_OF_YEAR)) {
                    dailyDataUsed = 0;
                    lastResetTime = System.currentTimeMillis();
                }
                
                dailyDataUsed += task.estimatedSize;
                
                // 更新准确率
                if (wasUseful) {
                    averageAccuracy = (averageAccuracy + 1.0) / 2.0;
                } else {
                    averageAccuracy = averageAccuracy * 0.9; // 衰减
                }
                
            } else {
                failedTasks++;
            }
        }
        
        public boolean isDailyLimitExceeded() {
            return dailyDataUsed >= MAX_DAILY_PRELOAD_DATA;
        }
    }
    
    /**
     * 预加载规则引擎
     */
    private class PreloadRuleEngine {
        private final List<PreloadRule> mRules;
        
        public PreloadRuleEngine() {
            mRules = new ArrayList<>();
            initializeDefaultRules();
        }
        
        private void initializeDefaultRules() {
            // 规则1：高频访问的域名优先预加载
            mRules.add(new PreloadRule("high_frequency_domains") {
                @Override
                public double evaluate(String url, Map<String, Object> context) {
                    // 从用户行为分析器获取域名访问频率
                    String domain = extractDomain(url);
                    List<String> suggestions = mBehaviorAnalyzer.getPersonalizedSuggestions(domain, 10);
                    return suggestions.contains(domain) ? 0.8 : 0.2;
                }
            });
            
            // 规则2：时间模式匹配
            mRules.add(new PreloadRule("time_pattern_match") {
                @Override
                public double evaluate(String url, Map<String, Object> context) {
                    int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    // 根据时间模式调整预加载优先级
                    if (currentHour >= 19 && currentHour <= 23) {
                        return url.contains("video") || url.contains("entertainment") ? 0.7 : 0.3;
                    }
                    return 0.5;
                }
            });
            
            // 规则3：搜索相关性
            mRules.add(new PreloadRule("search_relevance") {
                @Override
                public double evaluate(String url, Map<String, Object> context) {
                    String lastQuery = (String) context.get("lastSearchQuery");
                    if (lastQuery != null && !lastQuery.isEmpty()) {
                        return calculateTextSimilarity(url, lastQuery) > 0.5 ? 0.9 : 0.4;
                    }
                    return 0.5;
                }
            });
        }
        
        public double evaluatePreloadPriority(String url, Map<String, Object> context) {
            double totalScore = 0.0;
            double totalWeight = 0.0;
            
            for (PreloadRule rule : mRules) {
                double score = rule.evaluate(url, context);
                double weight = rule.getWeight();
                totalScore += score * weight;
                totalWeight += weight;
            }
            
            return totalWeight > 0 ? totalScore / totalWeight : 0.0;
        }
    }
    
    /**
     * 预加载规则基类
     */
    private abstract static class PreloadRule {
        protected final String name;
        protected double weight = 1.0;
        
        public PreloadRule(String name) {
            this.name = name;
        }
        
        public abstract double evaluate(String url, Map<String, Object> context);
        
        public double getWeight() {
            return weight;
        }
        
        public void setWeight(double weight) {
            this.weight = weight;
        }
    }
    
    /**
     * 系统资源监控器
     */
    private class SystemResourceMonitor {
        
        public boolean isPreloadAllowed() {
            return hasEnoughBattery() && 
                   hasGoodNetworkCondition() && 
                   hasAvailableMemory() && 
                   !mStats.isDailyLimitExceeded();
        }
        
        private boolean hasEnoughBattery() {
            try {
                BatteryManager batteryManager = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
                if (batteryManager != null) {
                    int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    return batteryLevel >= MIN_BATTERY_LEVEL;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking battery level", e);
            }
            return true; // 默认允许
        }
        
        private boolean hasGoodNetworkCondition() {
            try {
                ConnectivityManager connectivityManager = 
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    
                    // 只在WiFi或良好的移动网络下预加载
                    return networkInfo != null && networkInfo.isConnected() && 
                           (networkInfo.getType() == ConnectivityManager.TYPE_WIFI ||
                            networkInfo.getSubtype() >= 13); // LTE及以上
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking network condition", e);
            }
            return false;
        }
        
        private boolean hasAvailableMemory() {
            try {
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                double memoryUsage = (double) usedMemory / maxMemory;
                
                return memoryUsage < MAX_MEMORY_USAGE;
            } catch (Exception e) {
                Log.w(TAG, "Error checking memory usage", e);
                return true;
            }
        }
        
        public PreloadPriority adjustPriorityBasedOnResources(PreloadPriority originalPriority) {
            if (!hasEnoughBattery()) {
                // 低电量时降低优先级
                return PreloadPriority.values()[
                    Math.max(0, originalPriority.ordinal() - 1)];
            }
            
            if (!hasGoodNetworkCondition()) {
                // 网络条件差时只预加载关键内容
                return originalPriority == PreloadPriority.CRITICAL ? 
                    PreloadPriority.CRITICAL : PreloadPriority.BACKGROUND;
            }
            
            return originalPriority;
        }
    }
    
    // 公共API方法
    
    /**
     * 添加预加载任务
     */
    public boolean addPreloadTask(@NonNull String url, @Nullable String title, 
                                PreloadType type, PreloadPriority priority) {
        try {
            if (!mResourceMonitor.isPreloadAllowed()) {
                Log.d(TAG, "Preload not allowed due to resource constraints");
                return false;
            }
            
            String taskId = generateTaskId(url);
            
            // 检查是否已存在
            if (mActivePreloads.containsKey(taskId) || 
                mPreloadQueue.stream().anyMatch(task -> task.id.equals(taskId))) {
                return false;
            }
            
            // 调整优先级
            PreloadPriority adjustedPriority = mResourceMonitor.adjustPriorityBasedOnResources(priority);
            
            // 创建预加载任务
            PreloadTask task = new PreloadTask(taskId, url, title != null ? title : url, type, adjustedPriority);
            
            // 计算预测置信度
            Map<String, Object> context = buildPreloadContext();
            task.predictionConfidence = mRuleEngine.evaluatePreloadPriority(url, context);
            
            // 检查置信度阈值
            if (task.predictionConfidence < MIN_PREDICTION_CONFIDENCE) {
                Log.d(TAG, "Task confidence too low: " + task.predictionConfidence);
                return false;
            }
            
            // 添加到队列
            if (mPreloadQueue.size() >= MAX_PRELOAD_QUEUE_SIZE) {
                // 移除最低优先级的任务
                removeLowestPriorityTask();
            }
            
            mPreloadQueue.offer(task);
            
            // 立即尝试执行高优先级任务
            if (adjustedPriority.getValue() >= PreloadPriority.HIGH.getValue()) {
                tryExecuteNextTask();
            }
            
            Log.d(TAG, "Added preload task: " + url + " (confidence: " + 
                String.format("%.2f", task.predictionConfidence) + ")");
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding preload task", e);
            return false;
        }
    }
    
    /**
     * 基于用户行为预测并添加预加载任务
     */
    public void predictAndPreload(@Nullable String currentQuery, @Nullable String currentUrl) {
        mScheduler.execute(() -> {
            try {
                // 获取个性化推荐
                List<PersonalizedRecommendationEngine.RecommendationItem> recommendations = 
                    mRecommendationEngine.getPersonalizedRecommendations(currentQuery, 10);
                
                for (PersonalizedRecommendationEngine.RecommendationItem item : recommendations) {
                    PreloadType type = inferPreloadType(item.url);
                    PreloadPriority priority = item.score > 0.8 ? 
                        PreloadPriority.HIGH : PreloadPriority.MEDIUM;
                    
                    addPreloadTask(item.url, item.title, type, priority);
                }
                
                // 基于搜索历史预测
                if (currentQuery != null && !currentQuery.isEmpty()) {
                    List<String> relatedSuggestions = 
                        mBehaviorAnalyzer.getPersonalizedSuggestions(currentQuery, 5);
                    
                    for (String suggestion : relatedSuggestions) {
                        String predictedUrl = "https://www.google.com/search?q=" + 
                            suggestion.replace(" ", "+");
                        
                        addPreloadTask(predictedUrl, suggestion, 
                            PreloadType.SEARCH_RESULT, PreloadPriority.MEDIUM);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in predictive preloading", e);
            }
        });
    }
    
    /**
     * 检查URL是否已预加载
     */
    public boolean isPreloaded(@NonNull String url) {
        String taskId = generateTaskId(url);
        PreloadTask task = mActivePreloads.get(taskId);
        
        return task != null && task.status == PreloadStatus.COMPLETED;
    }
    
    /**
     * 获取预加载统计报告
     */
    public String getPreloadReport() {
        StringBuilder report = new StringBuilder();
        
        try {
            report.append("=== 智能预加载系统报告 ===\n\n");
            
            // 基本统计
            report.append("总任务数: ").append(mStats.totalTasks).append("\n");
            report.append("成功率: ").append(String.format("%.1f%%", mStats.getSuccessRate() * 100)).append("\n");
            report.append("预测准确率: ").append(String.format("%.1f%%", mStats.averageAccuracy * 100)).append("\n");
            report.append("平均预加载时间: ").append(mStats.getAveragePreloadTime()).append("ms\n");
            
            // 流量统计
            report.append("已预加载数据: ").append(formatBytes(mStats.totalDataPreloaded)).append("\n");
            report.append("今日流量使用: ").append(formatBytes(mStats.dailyDataUsed)).append("\n");
            report.append("日限制: ").append(formatBytes(MAX_DAILY_PRELOAD_DATA)).append("\n\n");
            
            // 当前状态
            report.append("队列中任务: ").append(mPreloadQueue.size()).append("\n");
            report.append("执行中任务: ").append(mConcurrentPreloads.get()).append("\n");
            report.append("系统状态: ").append(mResourceMonitor.isPreloadAllowed() ? "允许预加载" : "限制中").append("\n");
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating preload report", e);
            report.append("报告生成失败: ").append(e.getMessage());
        }
        
        return report.toString();
    }
    
    // 私有辅助方法
    
    private void startPreloadScheduler() {
        // 每5分钟检查一次预加载队列
        mScheduler.scheduleAtFixedRate(() -> {
            try {
                processPreloadQueue();
                cleanupExpiredTasks();
            } catch (Exception e) {
                Log.e(TAG, "Error in preload scheduler", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }
    
    private void processPreloadQueue() {
        while (mConcurrentPreloads.get() < MAX_CONCURRENT_PRELOADS && 
               !mPreloadQueue.isEmpty() && 
               mResourceMonitor.isPreloadAllowed()) {
            
            tryExecuteNextTask();
        }
    }
    
    private void tryExecuteNextTask() {
        PreloadTask task = getNextHighestPriorityTask();
        if (task != null) {
            executePreloadTask(task);
        }
    }
    
    private PreloadTask getNextHighestPriorityTask() {
        return mPreloadQueue.stream()
            .filter(task -> task.status == PreloadStatus.PENDING)
            .max((a, b) -> {
                int priorityCompare = Integer.compare(a.priority.getValue(), b.priority.getValue());
                if (priorityCompare != 0) return priorityCompare;
                return Double.compare(a.predictionConfidence, b.predictionConfidence);
            })
            .orElse(null);
    }
    
    private void executePreloadTask(PreloadTask task) {
        if (mConcurrentPreloads.get() >= MAX_CONCURRENT_PRELOADS) {
            return;
        }
        
        task.status = PreloadStatus.IN_PROGRESS;
        task.startTime = System.currentTimeMillis();
        
        mPreloadQueue.remove(task);
        mActivePreloads.put(task.id, task);
        mConcurrentPreloads.incrementAndGet();
        
        mPreloadExecutor.execute(() -> {
            try {
                performPreload(task);
            } finally {
                mConcurrentPreloads.decrementAndGet();
                mActivePreloads.remove(task.id);
            }
        });
    }
    
    private void performPreload(PreloadTask task) {
        try {
            Log.d(TAG, "Starting preload: " + task.url);
            
            // 使用网络缓存管理器执行预加载
            switch (task.type) {
                case WEB_PAGE:
                case API_DATA:
                case SEARCH_RESULT:
                    preloadWebContent(task);
                    break;
                case IMAGE:
                    preloadImage(task);
                    break;
                case VIDEO:
                    preloadVideo(task);
                    break;
                default:
                    preloadGeneric(task);
                    break;
            }
            
            task.status = PreloadStatus.COMPLETED;
            task.completedTime = System.currentTimeMillis();
            
            Log.d(TAG, "Preload completed: " + task.url + " in " + task.getDuration() + "ms");
            
        } catch (Exception e) {
            task.status = PreloadStatus.FAILED;
            task.errorMessage = e.getMessage();
            task.completedTime = System.currentTimeMillis();
            
            Log.e(TAG, "Preload failed: " + task.url, e);
        } finally {
            // 记录统计信息
            mStats.recordTask(task, false); // 这里简化，实际需要跟踪是否被使用
            savePreloadStats();
        }
    }
    
    private void preloadWebContent(PreloadTask task) {
        // 使用NetworkCacheManager预加载网页内容
        NetworkCacheManager.CacheEntry cached = mNetworkCache.getCachedResponse(task.url);
        if (cached == null) {
            // 模拟网络请求预加载
            task.estimatedSize = estimateContentSize(task.url, task.type);
            
            // 实际项目中这里会发起真实的网络请求
            try {
                Thread.sleep(1000); // 模拟网络延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Preload interrupted", e);
            }
        }
    }
    
    private void preloadImage(PreloadTask task) {
        task.estimatedSize = estimateContentSize(task.url, task.type);
        try {
            Thread.sleep(500); // 模拟图片下载
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Preload interrupted", e);
        }
    }
    
    private void preloadVideo(PreloadTask task) {
        // 视频只预加载元数据和前几秒内容
        task.estimatedSize = Math.min(estimateContentSize(task.url, task.type), 5 * 1024 * 1024); // 限制5MB
        try {
            Thread.sleep(2000); // 模拟视频预加载
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Preload interrupted", e);
        }
    }
    
    private void preloadGeneric(PreloadTask task) {
        task.estimatedSize = estimateContentSize(task.url, task.type);
        try {
            Thread.sleep(800); // 模拟通用内容预加载
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Preload interrupted", e);
        }
    }
    
    private long estimateContentSize(String url, PreloadType type) {
        // 简化的内容大小估算
        switch (type) {
            case WEB_PAGE:
            case SEARCH_RESULT:
                return 100 * 1024; // 100KB
            case IMAGE:
                return 500 * 1024; // 500KB
            case VIDEO:
                return 10 * 1024 * 1024; // 10MB
            case SCRIPT:
            case API_DATA:
                return 50 * 1024; // 50KB
            default:
                return 200 * 1024; // 200KB
        }
    }
    
    private PreloadType inferPreloadType(String url) {
        url = url.toLowerCase();
        
        if (url.contains("search") || url.contains("query")) {
            return PreloadType.SEARCH_RESULT;
        } else if (url.matches(".*\\.(jpg|jpeg|png|gif|webp).*")) {
            return PreloadType.IMAGE;
        } else if (url.matches(".*\\.(mp4|avi|mov|webm).*")) {
            return PreloadType.VIDEO;
        } else if (url.matches(".*\\.(js|css).*")) {
            return PreloadType.SCRIPT;
        } else if (url.contains("api/") || url.contains("/api")) {
            return PreloadType.API_DATA;
        }
        
        return PreloadType.WEB_PAGE;
    }
    
    private Map<String, Object> buildPreloadContext() {
        Map<String, Object> context = new HashMap<>();
        
        // 添加时间上下文
        Calendar now = Calendar.getInstance();
        context.put("currentHour", now.get(Calendar.HOUR_OF_DAY));
        context.put("dayOfWeek", now.get(Calendar.DAY_OF_WEEK));
        
        // 添加系统状态
        context.put("batteryLevel", getBatteryLevel());
        context.put("networkType", getNetworkType());
        context.put("memoryUsage", getMemoryUsage());
        
        return context;
    }
    
    private void removeLowestPriorityTask() {
        PreloadTask lowestPriorityTask = mPreloadQueue.stream()
            .min((a, b) -> {
                int priorityCompare = Integer.compare(a.priority.getValue(), b.priority.getValue());
                if (priorityCompare != 0) return priorityCompare;
                return Double.compare(a.predictionConfidence, b.predictionConfidence);
            })
            .orElse(null);
        
        if (lowestPriorityTask != null) {
            mPreloadQueue.remove(lowestPriorityTask);
            lowestPriorityTask.status = PreloadStatus.CANCELLED;
        }
    }
    
    private void cleanupExpiredTasks() {
        long expirationTime = 2 * 60 * 60 * 1000; // 2小时过期
        
        mPreloadQueue.removeIf(task -> task.isExpired(expirationTime));
        mActivePreloads.entrySet().removeIf(entry -> {
            PreloadTask task = entry.getValue();
            if (task.isExpired(expirationTime)) {
                task.status = PreloadStatus.EXPIRED;
                return true;
            }
            return false;
        });
    }
    
    private String generateTaskId(String url) {
        return String.valueOf(url.hashCode());
    }
    
    private String extractDomain(String url) {
        try {
            return url.replaceAll("^https?://", "")
                     .replaceAll("^www\\.", "")
                     .split("/")[0];
        } catch (Exception e) {
            return url;
        }
    }
    
    private double calculateTextSimilarity(String text1, String text2) {
        // 简化的文本相似度计算
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
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
        return String.format("%.1fGB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private int getBatteryLevel() {
        try {
            BatteryManager batteryManager = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
            return batteryManager != null ? 
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : 50;
        } catch (Exception e) {
            return 50;
        }
    }
    
    private String getNetworkType() {
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm != null ? cm.getActiveNetworkInfo() : null;
            
            if (networkInfo == null || !networkInfo.isConnected()) {
                return "none";
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return "wifi";
            } else {
                return "mobile";
            }
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private double getMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            return (double) usedMemory / maxMemory;
        } catch (Exception e) {
            return 0.5;
        }
    }
    
    // 数据持久化方法
    
    private void loadPreloadData() {
        mScheduler.execute(() -> {
            try {
                loadPreloadStats();
                loadPreloadQueue();
                Log.d(TAG, "Preload data loaded");
            } catch (Exception e) {
                Log.e(TAG, "Error loading preload data", e);
            }
        });
    }
    
    private void loadPreloadStats() {
        try {
            String data = mPrefs.getString(KEY_PRELOAD_STATS, "{}");
            JSONObject json = new JSONObject(data);
            
            mStats.totalTasks = json.optInt("totalTasks", 0);
            mStats.successfulTasks = json.optInt("successfulTasks", 0);
            mStats.failedTasks = json.optInt("failedTasks", 0);
            mStats.totalDataPreloaded = json.optLong("totalDataPreloaded", 0);
            mStats.totalTimeSpent = json.optLong("totalTimeSpent", 0);
            mStats.averageAccuracy = json.optDouble("averageAccuracy", 0.0);
            mStats.dailyDataUsed = json.optLong("dailyDataUsed", 0);
            mStats.lastResetTime = json.optLong("lastResetTime", System.currentTimeMillis());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading preload stats", e);
        }
    }
    
    private void loadPreloadQueue() {
        try {
            String data = mPrefs.getString(KEY_PRELOAD_QUEUE, "[]");
            JSONArray jsonArray = new JSONArray(data);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject taskJson = jsonArray.getJSONObject(i);
                PreloadTask task = PreloadTask.fromJson(taskJson);
                
                if (task != null && !task.isExpired(2 * 60 * 60 * 1000)) { // 2小时过期
                    if (task.status == PreloadStatus.IN_PROGRESS) {
                        task.status = PreloadStatus.PENDING; // 重置执行中的任务
                    }
                    
                    if (task.status == PreloadStatus.PENDING) {
                        mPreloadQueue.offer(task);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading preload queue", e);
        }
    }
    
    private void savePreloadStats() {
        mScheduler.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("totalTasks", mStats.totalTasks);
                json.put("successfulTasks", mStats.successfulTasks);
                json.put("failedTasks", mStats.failedTasks);
                json.put("totalDataPreloaded", mStats.totalDataPreloaded);
                json.put("totalTimeSpent", mStats.totalTimeSpent);
                json.put("averageAccuracy", mStats.averageAccuracy);
                json.put("dailyDataUsed", mStats.dailyDataUsed);
                json.put("lastResetTime", mStats.lastResetTime);
                
                mPrefs.edit().putString(KEY_PRELOAD_STATS, json.toString()).apply();
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving preload stats", e);
            }
        });
    }
    
    private void savePreloadQueue() {
        mScheduler.execute(() -> {
            try {
                JSONArray jsonArray = new JSONArray();
                
                for (PreloadTask task : mPreloadQueue) {
                    if (task.status == PreloadStatus.PENDING) {
                        jsonArray.put(task.toJson());
                    }
                }
                
                mPrefs.edit().putString(KEY_PRELOAD_QUEUE, jsonArray.toString()).apply();
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving preload queue", e);
            }
        });
    }
    
    /**
     * 清理过期数据和统计
     */
    public void cleanup() {
        mScheduler.execute(() -> {
            try {
                cleanupExpiredTasks();
                savePreloadStats();
                savePreloadQueue();
                
                Log.d(TAG, "Cleanup completed - Queue: " + mPreloadQueue.size() + 
                    ", Active: " + mActivePreloads.size());
                
            } catch (Exception e) {
                Log.e(TAG, "Error during cleanup", e);
            }
        });
    }
}