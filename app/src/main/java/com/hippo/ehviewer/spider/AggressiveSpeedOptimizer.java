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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.hippo.ehviewer.Settings;
import com.hippo.lib.yorozuya.MathUtils;

/**
 * 激进的画廊展开速度优化器
 * 
 * 针对展开速度进行极限优化，大幅提升预加载和并发配置
 */
public class AggressiveSpeedOptimizer {
    
    private static final String TAG = "AggressiveSpeedOptimizer";
    
    // 激进配置常量
    private static final int WIFI_MAX_THREADS = 12;      // WiFi环境最大线程数
    private static final int MOBILE_MAX_THREADS = 8;     // 移动网络最大线程数
    private static final int WIFI_MAX_PRELOAD = 50;      // WiFi环境最大预加载
    private static final int MOBILE_MAX_PRELOAD = 30;    // 移动网络最大预加载
    private static final int MAX_CACHE_SIZE = 800;       // 最大缓存大小(MB)
    
    /**
     * 应用激进的展开速度优化配置
     */
    public static void applyAggressiveOptimizations(Context context) {
        Log.d(TAG, "Applying aggressive gallery loading speed optimizations");
        
        try {
            boolean isWiFi = isWiFiConnected(context);
            boolean isHighPerformance = Settings.isHighPerformanceDevice();
            long deviceMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            
            Log.d(TAG, String.format("Device info: WiFi=%s, HighPerf=%s, Memory=%dMB", 
                isWiFi, isHighPerformance, deviceMemory));
            
            // 1. 激进的并发下载配置
            int aggressiveThreads = calculateAggressiveThreadCount(isWiFi, isHighPerformance, deviceMemory);
            
            // 2. 激进的预加载配置
            int aggressivePreload = calculateAggressivePreloadCount(isWiFi, isHighPerformance, deviceMemory);
            
            // 3. 激进的缓存配置
            int aggressiveCache = calculateAggressiveCacheSize(deviceMemory);
            
            // 应用配置
            Settings.putMultiThreadDownload(aggressiveThreads);
            Settings.putPreloadImage(aggressivePreload);
            
            // 更新增强配置
            Settings.putEnhancedMultiThreadDownload(aggressiveThreads);
            Settings.putEnhancedPreloadImage(aggressivePreload);
            Settings.putEnhancedReadCacheSize(aggressiveCache);
            
            Log.d(TAG, String.format("Applied aggressive config: threads=%d, preload=%d, cache=%dMB", 
                aggressiveThreads, aggressivePreload, aggressiveCache));
                
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply aggressive optimizations", e);
        }
    }
    
    /**
     * 计算激进的线程数配置
     */
    private static int calculateAggressiveThreadCount(boolean isWiFi, boolean isHighPerformance, long memory) {
        if (!isHighPerformance) {
            return 6; // 即使是低性能设备也给6个线程
        }
        
        if (isWiFi) {
            if (memory >= 8 * 1024) { // 8GB+
                return WIFI_MAX_THREADS; // 12线程
            } else if (memory >= 6 * 1024) { // 6GB+
                return 10;
            } else {
                return 8;
            }
        } else {
            // 移动网络稍微保守一些
            if (memory >= 6 * 1024) {
                return MOBILE_MAX_THREADS; // 8线程
            } else {
                return 6;
            }
        }
    }
    
    /**
     * 计算激进的预加载数量
     */
    private static int calculateAggressivePreloadCount(boolean isWiFi, boolean isHighPerformance, long memory) {
        if (isWiFi) {
            // WiFi环境下激进预加载
            if (memory >= 8 * 1024) { // 8GB+
                return WIFI_MAX_PRELOAD; // 50张
            } else if (memory >= 6 * 1024) { // 6GB+
                return 40;
            } else if (memory >= 4 * 1024) { // 4GB+
                return 30;
            } else {
                return 25; // 即使是低性能设备也预加载25张
            }
        } else {
            // 移动网络下考虑流量
            if (memory >= 6 * 1024) {
                return MOBILE_MAX_PRELOAD; // 30张
            } else if (memory >= 4 * 1024) {
                return 25;
            } else {
                return 20;
            }
        }
    }
    
    /**
     * 计算激进的缓存大小
     */
    private static int calculateAggressiveCacheSize(long memory) {
        if (memory >= 8 * 1024) { // 8GB+
            return MAX_CACHE_SIZE; // 800MB
        } else if (memory >= 6 * 1024) { // 6GB+
            return 600;
        } else if (memory >= 4 * 1024) { // 4GB+
            return 450;
        } else {
            return 320; // 保守一些
        }
    }
    
    /**
     * 检查是否连接WiFi
     */
    private static boolean isWiFiConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && 
                       activeNetwork.isConnected() && 
                       activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to check WiFi status", e);
        }
        return false;
    }
    
    /**
     * 应用极限优化 - 只在高性能设备上使用
     */
    public static void applyUltraOptimizations(Context context) {
        if (!Settings.isHighPerformanceDevice()) {
            Log.w(TAG, "Ultra optimizations skipped - not a high performance device");
            return;
        }
        
        Log.d(TAG, "Applying ULTRA aggressive optimizations");
        
        boolean isWiFi = isWiFiConnected(context);
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        
        if (isWiFi && memory >= 6 * 1024) {
            // 极限配置 - 只在WiFi且6GB+内存设备上启用
            int ultraThreads = memory >= 8 * 1024 ? 15 : 12; // 12-15线程
            int ultraPreload = memory >= 8 * 1024 ? 80 : 60;  // 60-80张预加载
            int ultraCache = memory >= 8 * 1024 ? 1000 : 800; // 800MB-1GB缓存
            
            Settings.putMultiThreadDownload(MathUtils.clamp(ultraThreads, 1, 15));
            Settings.putPreloadImage(MathUtils.clamp(ultraPreload, 0, 100));
            Settings.putEnhancedReadCacheSize(MathUtils.clamp(ultraCache, 40, 1024));
            
            Log.d(TAG, String.format("Applied ULTRA config: threads=%d, preload=%d, cache=%dMB", 
                ultraThreads, ultraPreload, ultraCache));
        }
    }
    
    /**
     * 智能预加载策略 - 根据用户浏览模式调整
     */
    public static void enableSmartPreloadStrategy(Context context, int currentIndex, int totalPages) {
        try {
            // 根据当前位置动态调整预加载
            boolean isWiFi = isWiFiConnected(context);
            long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            
            int basePreload = Settings.getPreloadImage();
            int smartPreload = basePreload;
            
            // 在画廊开头时预加载更多
            if (currentIndex < 10) {
                smartPreload = (int) (basePreload * 1.5);
            }
            
            // 在WiFi环境下更激进
            if (isWiFi && memory >= 4 * 1024) {
                smartPreload = (int) (smartPreload * 1.3);
            }
            
            // 应用动态预加载
            smartPreload = MathUtils.clamp(smartPreload, basePreload, 100);
            
            if (smartPreload != basePreload) {
                Settings.putPreloadImage(smartPreload);
                Log.d(TAG, String.format("Smart preload adjusted: %d → %d (index=%d)", 
                    basePreload, smartPreload, currentIndex));
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Smart preload strategy failed", e);
        }
    }
    
    /**
     * 获取当前激进优化状态
     */
    public static String getOptimizationStatus(Context context) {
        StringBuilder status = new StringBuilder();
        status.append("=== 激进优化状态 ===\n");
        
        boolean isWiFi = isWiFiConnected(context);
        boolean isHighPerf = Settings.isHighPerformanceDevice();
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        
        status.append("网络状态: ").append(isWiFi ? "WiFi" : "移动网络").append("\n");
        status.append("设备性能: ").append(isHighPerf ? "高性能" : "标准").append("\n");
        status.append("设备内存: ").append(memory).append("MB\n");
        status.append("当前配置:\n");
        status.append("- 下载线程: ").append(Settings.getMultiThreadDownload()).append("\n");
        status.append("- 预加载数: ").append(Settings.getPreloadImage()).append("\n");
        status.append("- 缓存大小: ").append(Settings.getReadCacheSize()).append("MB\n");
        
        return status.toString();
    }
    
    /**
     * 重置为保守配置
     */
    public static void resetToConservative(Context context) {
        Log.d(TAG, "Resetting to conservative configuration");
        
        Settings.putMultiThreadDownload(3);  // 原始默认值
        Settings.putPreloadImage(5);         // 原始默认值
        Settings.putEnhancedReadCacheSize(160); // 原始默认值
    }
}