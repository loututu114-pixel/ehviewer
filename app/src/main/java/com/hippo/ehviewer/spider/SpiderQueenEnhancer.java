/*
 * Copyright 2025 EhViewer Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.spider;

import android.content.Context;
import android.util.Log;

import com.hippo.ehviewer.Settings;
import com.hippo.lib.yorozuya.MathUtils;

/**
 * SpiderQueen增强器
 * 
 * 基于用户现有的SpiderQueen系统进行增强，而不是覆盖
 * 提供智能的配置优化和设备适配功能
 */
public class SpiderQueenEnhancer {
    
    private static final String TAG = "SpiderQueenEnhancer";
    
    /**
     * 获取优化后的多线程下载数量
     * 优先使用增强配置，降级到原始配置
     */
    public static int getOptimizedMultiThreadDownload() {
        try {
            // 首先尝试获取增强配置
            int enhanced = Settings.getEnhancedMultiThreadDownload();
            Log.d(TAG, "Enhanced multi-thread download: " + enhanced);
            return MathUtils.clamp(enhanced, 1, 10);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get enhanced multi-thread config, using original", e);
            // 降级到原始配置
            return Settings.getMultiThreadDownload();
        }
    }
    
    /**
     * 获取优化后的预加载图片数量
     * 优先使用增强配置，降级到原始配置
     */
    public static int getOptimizedPreloadImage() {
        try {
            // 首先尝试获取增强配置
            int enhanced = Settings.getEnhancedPreloadImage();
            Log.d(TAG, "Enhanced preload image: " + enhanced);
            return MathUtils.clamp(enhanced, 0, 100);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get enhanced preload config, using original", e);
            // 降级到原始配置
            return Settings.getPreloadImage();
        }
    }
    
    /**
     * 获取优化后的读取缓存大小
     * 优先使用增强配置，降级到原始配置
     */
    public static int getOptimizedReadCacheSize() {
        try {
            // 首先尝试获取增强配置
            int enhanced = Settings.getEnhancedReadCacheSize();
            Log.d(TAG, "Enhanced read cache size: " + enhanced + "MB");
            return MathUtils.clamp(enhanced, 40, 1024);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get enhanced cache config, using original", e);
            // 降级到原始配置
            return Settings.getReadCacheSize();
        }
    }
    
    /**
     * 应用SpiderQueen增强配置
     * 动态调整现有设置以发挥SpiderQueen的最佳性能
     */
    public static void applyEnhancedSettings(Context context) {
        Log.d(TAG, "Applying SpiderQueen enhanced settings");
        
        try {
            // 获取当前设备信息
            boolean isHighPerformance = Settings.isHighPerformanceDevice();
            boolean smartMode = Settings.getSmartPerformanceMode();
            
            Log.d(TAG, "Device info: high-performance=" + isHighPerformance + ", smart-mode=" + smartMode);
            
            // 启用加载卡死解决器
            boolean stuckResolverEnabled = Settings.getBoolean("loading_stuck_resolver_enabled", true);
            if (stuckResolverEnabled) {
                LoadingStuckResolver.getInstance(context).setEnabled(true);
                Log.d(TAG, "LoadingStuckResolver enabled");
            }
            
            if (smartMode) {
                // 检查是否启用激进模式
                boolean aggressiveMode = Settings.getBoolean("aggressive_speed_mode", false);
                
                if (aggressiveMode && isHighPerformance) {
                    Log.d(TAG, "Applying AGGRESSIVE speed optimizations");
                    AggressiveSpeedOptimizer.applyAggressiveOptimizations(context);
                } else {
                    // 标准智能模式：根据设备性能调整原始Settings
                    int optimizedThreads = getOptimizedMultiThreadDownload();
                    int optimizedPreload = getOptimizedPreloadImage();
                    
                    // 如果增强配置更好，则更新原始设置
                    if (optimizedThreads > Settings.getMultiThreadDownload()) {
                        Log.d(TAG, "Updating multi-thread download from " + 
                            Settings.getMultiThreadDownload() + " to " + optimizedThreads);
                        Settings.putMultiThreadDownload(optimizedThreads);
                    }
                    
                    if (optimizedPreload > Settings.getPreloadImage()) {
                        Log.d(TAG, "Updating preload image from " + 
                            Settings.getPreloadImage() + " to " + optimizedPreload);
                        Settings.putPreloadImage(optimizedPreload);
                    }
                }
                
                Log.d(TAG, "SpiderQueen enhanced settings applied successfully");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply enhanced settings", e);
        }
    }
    
    /**
     * 获取当前SpiderQueen配置摘要
     */
    public static String getConfigurationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("SpiderQueen Configuration Summary:\n");
        sb.append("- Smart Performance Mode: ").append(Settings.getSmartPerformanceMode()).append("\n");
        sb.append("- High Performance Device: ").append(Settings.isHighPerformanceDevice()).append("\n");
        sb.append("- Multi-Thread Download: ").append(getOptimizedMultiThreadDownload()).append("\n");
        sb.append("- Preload Image: ").append(getOptimizedPreloadImage()).append("\n");
        sb.append("- Read Cache Size: ").append(getOptimizedReadCacheSize()).append("MB\n");
        sb.append("- Device Memory: ").append(Runtime.getRuntime().maxMemory() / 1024 / 1024).append("MB\n");
        return sb.toString();
    }
    
    /**
     * 检查是否需要应用增强配置
     */
    public static boolean shouldApplyEnhancements() {
        return Settings.getSmartPerformanceMode() && Settings.isHighPerformanceDevice();
    }
    
    /**
     * 启用激进模式 - 极限优化展开速度
     */
    public static void enableAggressiveMode(Context context) {
        if (!Settings.isHighPerformanceDevice()) {
            Log.w(TAG, "Aggressive mode requires high performance device");
            return;
        }
        
        Log.d(TAG, "Enabling aggressive speed mode");
        Settings.putBoolean("aggressive_speed_mode", true);
        
        // 立即应用激进优化
        AggressiveSpeedOptimizer.applyAggressiveOptimizations(context);
        
        // 启用智能缓存预加载
        try {
            com.hippo.ehviewer.gallery.SmartCachePreloader preloader = 
                com.hippo.ehviewer.gallery.SmartCachePreloader.getInstance(context);
            preloader.enablePreload();
            Log.d(TAG, "Smart cache preloader enabled");
        } catch (Exception e) {
            Log.w(TAG, "Failed to enable smart preloader", e);
        }
    }
    
    /**
     * 启用极限模式 - 仅限8GB+设备
     */
    public static void enableUltraMode(Context context) {
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        if (memory < 8 * 1024) {
            Log.w(TAG, "Ultra mode requires 8GB+ memory, current: " + memory + "MB");
            return;
        }
        
        Log.d(TAG, "Enabling ULTRA speed mode");
        Settings.putBoolean("aggressive_speed_mode", true);
        Settings.putBoolean("ultra_speed_mode", true);
        
        AggressiveSpeedOptimizer.applyUltraOptimizations(context);
    }
    
    /**
     * 禁用激进模式
     */
    public static void disableAggressiveMode(Context context) {
        Log.d(TAG, "Disabling aggressive speed mode");
        Settings.putBoolean("aggressive_speed_mode", false);
        Settings.putBoolean("ultra_speed_mode", false);
        
        // 重置为标准配置
        applyEnhancedSettings(context);
        
        // 禁用智能缓存预加载
        try {
            com.hippo.ehviewer.gallery.SmartCachePreloader preloader = 
                com.hippo.ehviewer.gallery.SmartCachePreloader.getInstance(context);
            preloader.disablePreload();
            Log.d(TAG, "Smart cache preloader disabled");
        } catch (Exception e) {
            Log.w(TAG, "Failed to disable smart preloader", e);
        }
    }
    
    /**
     * 检查激进模式是否启用
     */
    public static boolean isAggressiveModeEnabled() {
        return Settings.getBoolean("aggressive_speed_mode", false);
    }
    
    /**
     * 获取激进模式状态报告
     */
    public static String getAggressiveModeReport(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("=== 激进展开速度优化报告 ===\n\n");
        
        boolean aggressive = isAggressiveModeEnabled();
        boolean ultra = Settings.getBoolean("ultra_speed_mode", false);
        boolean isHighPerf = Settings.isHighPerformanceDevice();
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        
        report.append("激进模式状态: ").append(aggressive ? "✓ 启用" : "✗ 禁用").append("\n");
        report.append("极限模式状态: ").append(ultra ? "✓ 启用" : "✗ 禁用").append("\n");
        report.append("设备性能等级: ").append(isHighPerf ? "高性能" : "标准").append("\n");
        report.append("设备内存容量: ").append(memory).append("MB\n\n");
        
        if (aggressive) {
            report.append("当前优化配置:\n");
            report.append("- 下载线程数: ").append(Settings.getMultiThreadDownload()).append("\n");
            report.append("- 预加载图片: ").append(Settings.getPreloadImage()).append("张\n");
            report.append("- 缓存大小: ").append(Settings.getReadCacheSize()).append("MB\n\n");
            
            try {
                com.hippo.ehviewer.gallery.SmartCachePreloader preloader = 
                    com.hippo.ehviewer.gallery.SmartCachePreloader.getInstance(context);
                report.append(preloader.getPreloadStats()).append("\n");
            } catch (Exception e) {
                report.append("智能预加载: 状态获取失败\n\n");
            }
            
            report.append(AggressiveSpeedOptimizer.getOptimizationStatus(context));
        } else {
            report.append("标准配置 (激进模式未启用):\n");
            report.append("- 下载线程数: ").append(Settings.getMultiThreadDownload()).append("\n");
            report.append("- 预加载图片: ").append(Settings.getPreloadImage()).append("张\n");
            report.append("- 缓存大小: ").append(Settings.getReadCacheSize()).append("MB\n");
        }
        
        return report.toString();
    }
    
    /**
     * 重置所有增强配置
     */
    public static void resetEnhancements(Context context) {
        Log.d(TAG, "Resetting SpiderQueen enhancements");
        
        // 先禁用激进模式
        disableAggressiveMode(context);
        
        Settings.resetSpiderQueenEnhancedSettings();
        
        // 恢复原始默认值
        Settings.putMultiThreadDownload(3);   // 原始默认值
        Settings.putPreloadImage(5);          // 原始默认值
    }
}