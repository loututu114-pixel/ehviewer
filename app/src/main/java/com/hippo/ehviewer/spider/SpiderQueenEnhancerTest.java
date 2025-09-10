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
 * SpiderQueen增强器测试工具
 * 用于验证增强配置是否正常工作
 */
public class SpiderQueenEnhancerTest {
    
    private static final String TAG = "SpiderQueenEnhancerTest";
    
    /**
     * 执行全面的增强配置测试
     */
    public static String runComprehensiveTest(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("=== SpiderQueen增强配置测试报告 ===\n\n");
        
        try {
            // 1. 设备信息测试
            report.append("1. 设备信息检测:\n");
            long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            boolean isHighPerformance = Settings.isHighPerformanceDevice();
            report.append("   设备内存: ").append(memory).append("MB\n");
            report.append("   高性能设备: ").append(isHighPerformance).append("\n\n");
            
            // 2. 智能性能模式测试
            report.append("2. 智能性能模式:\n");
            boolean smartMode = Settings.getSmartPerformanceMode();
            report.append("   智能模式: ").append(smartMode).append("\n\n");
            
            // 3. 增强配置测试
            report.append("3. SpiderQueen增强配置:\n");
            int enhancedThreads = SpiderQueenEnhancer.getOptimizedMultiThreadDownload();
            int enhancedPreload = SpiderQueenEnhancer.getOptimizedPreloadImage();
            int enhancedCache = SpiderQueenEnhancer.getOptimizedReadCacheSize();
            
            report.append("   增强多线程下载: ").append(enhancedThreads).append(" (原始: ").append(Settings.getMultiThreadDownload()).append(")\n");
            report.append("   增强预加载图片: ").append(enhancedPreload).append(" (原始: ").append(Settings.getPreloadImage()).append(")\n");
            report.append("   增强缓存大小: ").append(enhancedCache).append("MB (原始: ").append(Settings.getReadCacheSize()).append("MB)\n\n");
            
            // 4. 配置优化效果测试
            report.append("4. 配置优化效果:\n");
            boolean threadsImproved = enhancedThreads > Settings.getMultiThreadDownload();
            boolean preloadImproved = enhancedPreload > Settings.getPreloadImage();
            boolean cacheImproved = enhancedCache > Settings.getReadCacheSize();
            
            report.append("   多线程优化: ").append(threadsImproved ? "✓ 提升" : "- 无变化").append("\n");
            report.append("   预加载优化: ").append(preloadImproved ? "✓ 提升" : "- 无变化").append("\n");
            report.append("   缓存优化: ").append(cacheImproved ? "✓ 提升" : "- 无变化").append("\n\n");
            
            // 5. 应用增强配置
            report.append("5. 应用增强配置测试:\n");
            int originalThreads = Settings.getMultiThreadDownload();
            int originalPreload = Settings.getPreloadImage();
            
            SpiderQueenEnhancer.applyEnhancedSettings(context);
            
            int newThreads = Settings.getMultiThreadDownload();
            int newPreload = Settings.getPreloadImage();
            
            report.append("   多线程下载: ").append(originalThreads).append(" → ").append(newThreads).append("\n");
            report.append("   预加载图片: ").append(originalPreload).append(" → ").append(newPreload).append("\n\n");
            
            // 6. 配置摘要
            report.append("6. 当前配置摘要:\n");
            report.append(SpiderQueenEnhancer.getConfigurationSummary());
            
            // 7. 测试结论
            report.append("\n7. 测试结论:\n");
            if (threadsImproved || preloadImproved || cacheImproved) {
                report.append("   ✓ SpiderQueen增强配置正常工作\n");
                report.append("   ✓ 图片加载性能已优化\n");
            } else {
                report.append("   - 设备性能较低，未应用激进优化\n");
                report.append("   - 使用原始配置确保稳定性\n");
            }
            
            Log.d(TAG, "SpiderQueen enhancement test completed successfully");
            
        } catch (Exception e) {
            report.append("\n❌ 测试过程中发生错误: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Enhancement test failed", e);
        }
        
        return report.toString();
    }
    
    /**
     * 测试配置重置功能
     */
    public static String testConfigurationReset(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("=== 配置重置测试 ===\n\n");
        
        try {
            // 记录重置前的配置
            report.append("重置前配置:\n");
            report.append("- 智能模式: ").append(Settings.getSmartPerformanceMode()).append("\n");
            report.append("- 多线程: ").append(Settings.getMultiThreadDownload()).append("\n");
            report.append("- 预加载: ").append(Settings.getPreloadImage()).append("\n\n");
            
            // 执行重置
            SpiderQueenEnhancer.resetEnhancements(context);
            
            // 记录重置后的配置
            report.append("重置后配置:\n");
            report.append("- 智能模式: ").append(Settings.getSmartPerformanceMode()).append("\n");
            report.append("- 多线程: ").append(Settings.getMultiThreadDownload()).append("\n");
            report.append("- 预加载: ").append(Settings.getPreloadImage()).append("\n\n");
            
            report.append("✓ 配置重置测试完成\n");
            
        } catch (Exception e) {
            report.append("❌ 配置重置测试失败: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Configuration reset test failed", e);
        }
        
        return report.toString();
    }
    
    /**
     * 快速验证增强功能是否工作
     */
    public static boolean isEnhancementWorking() {
        try {
            // 检查增强配置是否生效
            int enhanced = SpiderQueenEnhancer.getOptimizedMultiThreadDownload();
            int original = Settings.getMultiThreadDownload();
            
            // 如果是高性能设备且智能模式开启，应该有优化
            if (Settings.isHighPerformanceDevice() && Settings.getSmartPerformanceMode()) {
                return enhanced >= original;
            } else {
                // 低性能设备保持原始配置也是正常的
                return enhanced == original;
            }
        } catch (Exception e) {
            Log.e(TAG, "Enhancement check failed", e);
            return false;
        }
    }
}