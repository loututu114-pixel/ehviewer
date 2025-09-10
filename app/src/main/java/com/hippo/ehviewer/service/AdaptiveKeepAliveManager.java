package com.hippo.ehviewer.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.hippo.ehviewer.util.BatteryOptimizationManager;
import com.hippo.ehviewer.notification.SystemMonitor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 自适应保活策略管理器
 * 根据设备状态、用户行为和系统环境动态调整保活策略
 */
public class AdaptiveKeepAliveManager {
    
    private static final String TAG = "AdaptiveKeepAliveManager";
    private static final String PREFS_NAME = "adaptive_keep_alive";
    
    // 策略模式枚举
    public enum KeepAliveStrategy {
        AGGRESSIVE("积极模式", "最强保活，适用于充电时"),
        NORMAL("正常模式", "平衡性能和电量"),
        CONSERVATIVE("保守模式", "优先省电，低电量时使用"),
        MINIMAL("最小模式", "仅核心功能，极低电量时使用"),
        ADAPTIVE("自适应", "根据环境自动调整");
        
        private final String displayName;
        private final String description;
        
        KeepAliveStrategy(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // 环境因素权重
    private static final class EnvironmentWeights {
        static final float BATTERY_LEVEL = 0.3f;
        static final float CHARGING_STATUS = 0.25f;
        static final float SCREEN_STATE = 0.15f;
        static final float APP_USAGE = 0.15f;
        static final float MEMORY_PRESSURE = 0.1f;
        static final float THERMAL_STATE = 0.05f;
    }
    
    private final Context context;
    private final Handler mainHandler;
    private final ScheduledExecutorService executor;
    private final BatteryOptimizationManager batteryOptManager;
    private final SharedPreferences prefs;
    
    private KeepAliveStrategy currentStrategy;
    private KeepAliveStrategy userPreferredStrategy;
    private float currentEnvironmentScore;
    private long lastStrategyChange;
    
    // 环境监控数据
    private float batteryLevel = 100f;
    private boolean isCharging = false;
    private boolean isScreenOn = true;
    private long lastUserInteraction = System.currentTimeMillis();
    private float memoryPressure = 0f;
    private int thermalState = 0; // 0=正常, 1=轻微发热, 2=中度发热, 3=严重发热
    
    private static AdaptiveKeepAliveManager instance;
    
    public static synchronized AdaptiveKeepAliveManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdaptiveKeepAliveManager(context);
        }
        return instance;
    }
    
    private AdaptiveKeepAliveManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.batteryOptManager = new BatteryOptimizationManager(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 加载用户偏好
        String preferredStrategy = prefs.getString("preferred_strategy", KeepAliveStrategy.ADAPTIVE.name());
        this.userPreferredStrategy = KeepAliveStrategy.valueOf(preferredStrategy);
        this.currentStrategy = KeepAliveStrategy.NORMAL;
        this.lastStrategyChange = System.currentTimeMillis();
        
        // 开始监控
        startEnvironmentMonitoring();
        
        Log.i(TAG, "Adaptive KeepAlive Manager initialized");
    }
    
    /**
     * 开始环境监控
     */
    private void startEnvironmentMonitoring() {
        // 每30秒评估一次环境并调整策略
        executor.scheduleAtFixedRate(this::evaluateAndAdjustStrategy, 0, 30, TimeUnit.SECONDS);
        
        // 每5分钟进行一次深度分析
        executor.scheduleAtFixedRate(this::performDeepAnalysis, 1, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 评估环境并调整策略
     */
    private void evaluateAndAdjustStrategy() {
        try {
            updateEnvironmentData();
            float environmentScore = calculateEnvironmentScore();
            
            KeepAliveStrategy newStrategy = determineOptimalStrategy(environmentScore);
            
            if (shouldChangeStrategy(newStrategy)) {
                changeStrategy(newStrategy);
            }
            
            logStrategyDecision(environmentScore, newStrategy);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during strategy evaluation", e);
        }
    }
    
    /**
     * 更新环境数据
     */
    private void updateEnvironmentData() {
        // 更新电池信息
        updateBatteryInfo();
        
        // 更新内存压力
        updateMemoryPressure();
        
        // 更新热状态（如果可用）
        updateThermalState();
        
        // 更新用户交互时间
        updateUserInteractionTime();
    }
    
    /**
     * 更新电池信息
     */
    private void updateBatteryInfo() {
        try {
            Intent batteryIntent = context.registerReceiver(null, 
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                
                if (level != -1 && scale != -1) {
                    this.batteryLevel = level * 100 / (float) scale;
                }
                
                this.isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to update battery info", e);
        }
    }
    
    /**
     * 更新内存压力
     */
    private void updateMemoryPressure() {
        try {
            ActivityManager activityManager = (ActivityManager) 
                context.getSystemService(Context.ACTIVITY_SERVICE);
            
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            
            // 计算内存压力 (0.0 - 1.0)
            this.memoryPressure = 1.0f - ((float) memoryInfo.availMem / memoryInfo.totalMem);
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to update memory pressure", e);
        }
    }
    
    /**
     * 更新热状态
     */
    private void updateThermalState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                android.os.PowerManager powerManager = 
                    (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                
                this.thermalState = powerManager.getCurrentThermalStatus();
            } catch (Exception e) {
                Log.w(TAG, "Failed to get thermal state", e);
                this.thermalState = 0;
            }
        } else {
            this.thermalState = 0;
        }
    }
    
    /**
     * 更新用户交互时间
     */
    private void updateUserInteractionTime() {
        // 从SharedPreferences读取最后用户交互时间
        this.lastUserInteraction = prefs.getLong("last_user_interaction", System.currentTimeMillis());
    }
    
    /**
     * 计算环境评分 (0.0 - 1.0, 1.0表示最适合积极保活)
     */
    private float calculateEnvironmentScore() {
        float score = 0f;
        
        // 电池电量贡献 (电量越高分数越高)
        float batteryScore = Math.min(batteryLevel / 100f, 1f);
        score += batteryScore * EnvironmentWeights.BATTERY_LEVEL;
        
        // 充电状态贡献
        float chargingScore = isCharging ? 1f : 0f;
        score += chargingScore * EnvironmentWeights.CHARGING_STATUS;
        
        // 屏幕状态贡献
        float screenScore = isScreenOn ? 0.7f : 0.3f; // 屏幕开启时适度降低保活强度
        score += screenScore * EnvironmentWeights.SCREEN_STATE;
        
        // 应用使用频率贡献
        long timeSinceLastUse = System.currentTimeMillis() - lastUserInteraction;
        float usageScore = Math.max(0f, 1f - (timeSinceLastUse / (24f * 60 * 60 * 1000))); // 24小时内使用过
        score += usageScore * EnvironmentWeights.APP_USAGE;
        
        // 内存压力贡献 (内存压力越大分数越低)
        float memoryScore = Math.max(0f, 1f - memoryPressure);
        score += memoryScore * EnvironmentWeights.MEMORY_PRESSURE;
        
        // 热状态贡献 (发热时降低保活强度)
        float thermalScore = Math.max(0f, 1f - (thermalState / 3f));
        score += thermalScore * EnvironmentWeights.THERMAL_STATE;
        
        this.currentEnvironmentScore = Math.min(Math.max(score, 0f), 1f);
        return this.currentEnvironmentScore;
    }
    
    /**
     * 确定最优策略
     */
    private KeepAliveStrategy determineOptimalStrategy(float environmentScore) {
        // 如果用户设置了非自适应模式，尊重用户选择（但在极端情况下仍会调整）
        if (userPreferredStrategy != KeepAliveStrategy.ADAPTIVE) {
            // 在极低电量或严重发热时强制保守模式
            if ((batteryLevel < 10 && !isCharging) || thermalState >= 3) {
                return KeepAliveStrategy.MINIMAL;
            }
            // 在低电量时适度调整
            if (batteryLevel < 20 && !isCharging && userPreferredStrategy == KeepAliveStrategy.AGGRESSIVE) {
                return KeepAliveStrategy.CONSERVATIVE;
            }
            return userPreferredStrategy;
        }
        
        // 自适应模式：根据环境评分选择策略
        if (environmentScore >= 0.8f) {
            return KeepAliveStrategy.AGGRESSIVE;
        } else if (environmentScore >= 0.6f) {
            return KeepAliveStrategy.NORMAL;
        } else if (environmentScore >= 0.3f) {
            return KeepAliveStrategy.CONSERVATIVE;
        } else {
            return KeepAliveStrategy.MINIMAL;
        }
    }
    
    /**
     * 判断是否应该改变策略
     */
    private boolean shouldChangeStrategy(KeepAliveStrategy newStrategy) {
        if (newStrategy == currentStrategy) {
            return false;
        }
        
        // 避免频繁切换策略（至少间隔2分钟）
        long timeSinceLastChange = System.currentTimeMillis() - lastStrategyChange;
        if (timeSinceLastChange < 2 * 60 * 1000) {
            // 除非是紧急情况（极低电量或严重发热）
            if (newStrategy != KeepAliveStrategy.MINIMAL) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 改变策略
     */
    private void changeStrategy(KeepAliveStrategy newStrategy) {
        KeepAliveStrategy oldStrategy = currentStrategy;
        currentStrategy = newStrategy;
        lastStrategyChange = System.currentTimeMillis();
        
        Log.i(TAG, String.format("Strategy changed: %s -> %s (score: %.2f)", 
            oldStrategy.getDisplayName(), newStrategy.getDisplayName(), currentEnvironmentScore));
        
        // 应用新策略
        applyStrategy(newStrategy);
        
        // 保存策略变化历史
        saveStrategyChange(oldStrategy, newStrategy);
        
        // 通知其他组件
        notifyStrategyChange(oldStrategy, newStrategy);
    }
    
    /**
     * 应用策略
     */
    private void applyStrategy(KeepAliveStrategy strategy) {
        Intent intent = new Intent("com.hippo.ehviewer.ACTION_STRATEGY_CHANGE");
        intent.putExtra("strategy", strategy.name());
        intent.putExtra("battery_level", batteryLevel);
        intent.putExtra("is_charging", isCharging);
        intent.putExtra("environment_score", currentEnvironmentScore);
        
        context.sendBroadcast(intent);
        
        // 根据策略调整具体参数
        switch (strategy) {
            case AGGRESSIVE:
                applyAggressiveStrategy();
                break;
            case NORMAL:
                applyNormalStrategy();
                break;
            case CONSERVATIVE:
                applyConservativeStrategy();
                break;
            case MINIMAL:
                applyMinimalStrategy();
                break;
        }
    }
    
    /**
     * 应用积极策略
     */
    private void applyAggressiveStrategy() {
        // 启用所有保活机制
        startService(AppKeepAliveService.class);
        // 缩短监控间隔
        // 启用音频保活
        // 更频繁的WakeLock
    }
    
    /**
     * 应用正常策略
     */
    private void applyNormalStrategy() {
        // 标准保活机制
        startService(AppKeepAliveService.class);
        // 正常监控间隔
    }
    
    /**
     * 应用保守策略
     */
    private void applyConservativeStrategy() {
        // 减少保活强度
        // 延长监控间隔
        // 禁用音频保活
    }
    
    /**
     * 应用最小策略
     */
    private void applyMinimalStrategy() {
        // 仅保留核心功能
        // 最长监控间隔
        // 禁用所有可选的保活机制
    }
    
    /**
     * 启动服务
     */
    private void startService(Class<?> serviceClass) {
        try {
            Intent intent = new Intent(context, serviceClass);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service: " + serviceClass.getSimpleName(), e);
        }
    }
    
    /**
     * 执行深度分析
     */
    private void performDeepAnalysis() {
        try {
            // 分析应用使用模式
            analyzeUsagePattern();
            
            // 分析保活效果
            analyzeKeepAliveEffectiveness();
            
            // 优化策略参数
            optimizeStrategyParameters();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during deep analysis", e);
        }
    }
    
    /**
     * 分析使用模式
     */
    private void analyzeUsagePattern() {
        // 分析用户使用应用的时间模式
        // 预测用户下次使用时间
        // 调整保活策略的时间窗口
    }
    
    /**
     * 分析保活效果
     */
    private void analyzeKeepAliveEffectiveness() {
        // 统计应用被系统杀死的频率
        // 评估不同策略的效果
        // 收集设备特定的优化参数
    }
    
    /**
     * 优化策略参数
     */
    private void optimizeStrategyParameters() {
        // 根据设备特性和历史数据调整参数
        // 学习最优的环境阈值
    }
    
    /**
     * 保存策略变化
     */
    private void saveStrategyChange(KeepAliveStrategy oldStrategy, KeepAliveStrategy newStrategy) {
        prefs.edit()
            .putString("last_strategy", oldStrategy.name())
            .putString("current_strategy", newStrategy.name())
            .putLong("last_strategy_change", lastStrategyChange)
            .putFloat("environment_score_at_change", currentEnvironmentScore)
            .apply();
    }
    
    /**
     * 通知策略变化
     */
    private void notifyStrategyChange(KeepAliveStrategy oldStrategy, KeepAliveStrategy newStrategy) {
        // 可以发送通知告知用户策略变化（仅在必要时）
        if (newStrategy == KeepAliveStrategy.MINIMAL) {
            // 切换到最小模式时通知用户
            // notificationManager.showLowPowerModeNotification();
        }
    }
    
    /**
     * 记录策略决策日志
     */
    private void logStrategyDecision(float environmentScore, KeepAliveStrategy strategy) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format(
                "Strategy evaluation: score=%.2f, battery=%.1f%%, charging=%s, thermal=%d, memory=%.2f, strategy=%s",
                environmentScore, batteryLevel, isCharging, thermalState, memoryPressure, strategy.name()
            ));
        }
    }
    
    /**
     * 设置用户偏好策略
     */
    public void setUserPreferredStrategy(KeepAliveStrategy strategy) {
        this.userPreferredStrategy = strategy;
        prefs.edit().putString("preferred_strategy", strategy.name()).apply();
        
        Log.i(TAG, "User preferred strategy set to: " + strategy.getDisplayName());
        
        // 立即重新评估策略
        executor.submit(this::evaluateAndAdjustStrategy);
    }
    
    /**
     * 获取当前策略
     */
    public KeepAliveStrategy getCurrentStrategy() {
        return currentStrategy;
    }
    
    /**
     * 获取用户偏好策略
     */
    public KeepAliveStrategy getUserPreferredStrategy() {
        return userPreferredStrategy;
    }
    
    /**
     * 获取当前环境评分
     */
    public float getCurrentEnvironmentScore() {
        return currentEnvironmentScore;
    }
    
    /**
     * 用户交互事件
     */
    public void onUserInteraction() {
        this.lastUserInteraction = System.currentTimeMillis();
        prefs.edit().putLong("last_user_interaction", lastUserInteraction).apply();
        
        // 用户交互后可能需要立即调整策略
        executor.submit(this::evaluateAndAdjustStrategy);
    }
    
    /**
     * 屏幕状态变化
     */
    public void onScreenStateChanged(boolean screenOn) {
        this.isScreenOn = screenOn;
        executor.submit(this::evaluateAndAdjustStrategy);
    }
    
    /**
     * 获取策略统计信息
     */
    public StrategyStats getStrategyStats() {
        return new StrategyStats(
            currentStrategy,
            userPreferredStrategy,
            currentEnvironmentScore,
            batteryLevel,
            isCharging,
            memoryPressure,
            thermalState,
            lastStrategyChange
        );
    }
    
    /**
     * 策略统计信息
     */
    public static class StrategyStats {
        public final KeepAliveStrategy currentStrategy;
        public final KeepAliveStrategy userPreferredStrategy;
        public final float environmentScore;
        public final float batteryLevel;
        public final boolean isCharging;
        public final float memoryPressure;
        public final int thermalState;
        public final long lastStrategyChange;
        
        public StrategyStats(KeepAliveStrategy currentStrategy, KeepAliveStrategy userPreferredStrategy,
                           float environmentScore, float batteryLevel, boolean isCharging,
                           float memoryPressure, int thermalState, long lastStrategyChange) {
            this.currentStrategy = currentStrategy;
            this.userPreferredStrategy = userPreferredStrategy;
            this.environmentScore = environmentScore;
            this.batteryLevel = batteryLevel;
            this.isCharging = isCharging;
            this.memoryPressure = memoryPressure;
            this.thermalState = thermalState;
            this.lastStrategyChange = lastStrategyChange;
        }
    }
    
    /**
     * 停止管理器
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        instance = null;
    }
}