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

/**
 * 激进模式管理器
 * 
 * 提供简单的接口来管理和测试激进展开速度优化
 */
public class AggressiveModeManager {
    
    private static final String TAG = "AggressiveModeManager";
    
    /**
     * 自动选择最适合的优化模式并启用
     */
    public static boolean enableBestOptimization(Context context) {
        Log.d(TAG, "Auto-selecting best optimization mode");
        
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        boolean isHighPerf = Settings.isHighPerformanceDevice();
        
        if (!isHighPerf) {
            Log.d(TAG, "Standard device, using enhanced mode only");
            SpiderQueenEnhancer.applyEnhancedSettings(context);
            return false;
        }
        
        if (memory >= 8 * 1024) {
            // 8GB+设备：启用极限模式
            Log.d(TAG, "8GB+ device detected, enabling ULTRA mode");
            SpiderQueenEnhancer.enableUltraMode(context);
            return true;
        } else if (memory >= 6 * 1024) {
            // 6GB+设备：启用激进模式
            Log.d(TAG, "6GB+ device detected, enabling aggressive mode");
            SpiderQueenEnhancer.enableAggressiveMode(context);
            return true;
        } else if (memory >= 4 * 1024) {
            // 4GB+设备：启用激进模式但较保守
            Log.d(TAG, "4GB+ device detected, enabling moderate aggressive mode");
            SpiderQueenEnhancer.enableAggressiveMode(context);
            return true;
        } else {
            // 其他高性能设备：仅使用增强模式
            Log.d(TAG, "High-performance device with limited memory, using enhanced mode");
            SpiderQueenEnhancer.applyEnhancedSettings(context);
            return false;
        }
    }
    
    /**
     * 手动启用激进模式（供用户选择）
     */
    public static String manualEnableAggressive(Context context) {
        StringBuilder result = new StringBuilder();
        
        if (!Settings.isHighPerformanceDevice()) {
            result.append("❌ 激进模式需要高性能设备支持\n");
            result.append("当前设备不满足条件，建议使用标准优化\n");
            return result.toString();
        }
        
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        result.append("🔧 启用激进展开速度优化...\n\n");
        
        try {
            SpiderQueenEnhancer.enableAggressiveMode(context);
            
            result.append("✅ 激进模式已启用\n");
            result.append("配置详情:\n");
            result.append("- 下载线程: ").append(Settings.getMultiThreadDownload()).append("\n");
            result.append("- 预加载数: ").append(Settings.getPreloadImage()).append("张\n");
            result.append("- 缓存大小: ").append(Settings.getReadCacheSize()).append("MB\n");
            result.append("- 智能预加载: 已启用\n\n");
            
            result.append("⚡ 展开速度应该有显著提升！\n");
            
            if (memory >= 8 * 1024) {
                result.append("\n🚀 检测到8GB+内存，可尝试极限模式获得更佳效果");
            }
            
        } catch (Exception e) {
            result.append("❌ 激进模式启用失败: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Failed to enable aggressive mode", e);
        }
        
        return result.toString();
    }
    
    /**
     * 启用极限模式（8GB+设备专用）
     */
    public static String manualEnableUltra(Context context) {
        StringBuilder result = new StringBuilder();
        
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        if (memory < 8 * 1024) {
            result.append("❌ 极限模式需要8GB+内存\n");
            result.append("当前内存: ").append(memory).append("MB\n");
            result.append("建议使用激进模式\n");
            return result.toString();
        }
        
        result.append("🚀 启用极限展开速度优化...\n\n");
        
        try {
            SpiderQueenEnhancer.enableUltraMode(context);
            
            result.append("✅ 极限模式已启用\n");
            result.append("⚠️  注意：这是最激进的配置，可能增加电量消耗\n\n");
            result.append("配置详情:\n");
            result.append("- 下载线程: ").append(Settings.getMultiThreadDownload()).append("\n");
            result.append("- 预加载数: ").append(Settings.getPreloadImage()).append("张\n");
            result.append("- 缓存大小: ").append(Settings.getReadCacheSize()).append("MB\n");
            result.append("- 智能预加载: 已启用\n\n");
            
            result.append("⚡ 应该获得最快的展开速度！\n");
            
        } catch (Exception e) {
            result.append("❌ 极限模式启用失败: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Failed to enable ultra mode", e);
        }
        
        return result.toString();
    }
    
    /**
     * 测试当前配置的展开速度
     */
    public static String testCurrentConfiguration(Context context) {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 展开速度配置测试 ===\n\n");
        
        // 设备信息
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        boolean isHighPerf = Settings.isHighPerformanceDevice();
        boolean isAggressive = SpiderQueenEnhancer.isAggressiveModeEnabled();
        
        report.append("设备信息:\n");
        report.append("- 内存容量: ").append(memory).append("MB\n");
        report.append("- 性能等级: ").append(isHighPerf ? "高性能" : "标准").append("\n");
        report.append("- 激进模式: ").append(isAggressive ? "✓ 启用" : "✗ 禁用").append("\n\n");
        
        // 当前配置
        report.append("当前配置:\n");
        report.append("- 下载线程: ").append(Settings.getMultiThreadDownload()).append(" (原始默认: 3)\n");
        report.append("- 预加载数: ").append(Settings.getPreloadImage()).append("张 (原始默认: 5)\n");
        report.append("- 缓存大小: ").append(Settings.getReadCacheSize()).append("MB (原始默认: 160)\n\n");
        
        // 性能评估
        report.append("性能评估:\n");
        int score = calculatePerformanceScore(context);
        if (score >= 90) {
            report.append("🚀 极限配置 - 预期展开速度: 极快\n");
        } else if (score >= 70) {
            report.append("⚡ 激进配置 - 预期展开速度: 很快\n");
        } else if (score >= 50) {
            report.append("📈 优化配置 - 预期展开速度: 较快\n");
        } else {
            report.append("📱 标准配置 - 预期展开速度: 正常\n");
        }
        report.append("性能评分: ").append(score).append("/100\n\n");
        
        // 优化建议
        if (!isAggressive && isHighPerf) {
            report.append("💡 优化建议:\n");
            report.append("- 检测到高性能设备，建议启用激进模式\n");
            if (memory >= 8 * 1024) {
                report.append("- 8GB+内存设备，可尝试极限模式\n");
            }
            report.append("- 激进模式将显著提升展开速度\n");
        } else if (isAggressive) {
            report.append("✅ 当前已是最优配置，享受快速展开体验！\n");
        }
        
        return report.toString();
    }
    
    /**
     * 计算性能评分
     */
    private static int calculatePerformanceScore(Context context) {
        int score = 0;
        
        // 基础分数
        score += 20;
        
        // 线程数评分 (0-25分)
        int threads = Settings.getMultiThreadDownload();
        score += Math.min(threads * 3, 25);
        
        // 预加载数评分 (0-25分)
        int preload = Settings.getPreloadImage();
        score += Math.min(preload, 25);
        
        // 缓存大小评分 (0-20分)
        int cache = Settings.getReadCacheSize();
        score += Math.min(cache / 20, 20);
        
        // 激进模式加分 (0-10分)
        if (SpiderQueenEnhancer.isAggressiveModeEnabled()) {
            score += 10;
        }
        
        return Math.min(score, 100);
    }
    
    /**
     * 获取快速启用建议
     */
    public static String getQuickEnableAdvice(Context context) {
        if (!Settings.isHighPerformanceDevice()) {
            return "⚠️ 当前设备性能有限，建议使用标准优化模式";
        }
        
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        boolean isAggressive = SpiderQueenEnhancer.isAggressiveModeEnabled();
        
        if (isAggressive) {
            return "✅ 激进模式已启用，享受快速展开！";
        }
        
        if (memory >= 8 * 1024) {
            return "🚀 推荐启用极限模式以获得最快的展开速度";
        } else if (memory >= 4 * 1024) {
            return "⚡ 推荐启用激进模式以显著提升展开速度";
        } else {
            return "📈 推荐启用标准优化以提升展开速度";
        }
    }
}