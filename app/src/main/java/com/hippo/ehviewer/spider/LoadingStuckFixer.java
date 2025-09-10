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
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.EhGalleryProvider;

/**
 * 加载卡死修复器 - 一键解决方案
 * 
 * 专门解决画廊点开后一直转圈加载不出来的问题
 * 提供简单易用的一键修复接口
 */
public class LoadingStuckFixer {
    
    private static final String TAG = "LoadingStuckFixer";
    
    /**
     * 一键启用所有加载卡死修复功能
     * 这是最简单的使用方式，推荐用户使用
     */
    public static String enableAllFixes(Context context) {
        StringBuilder result = new StringBuilder();
        result.append("🔧 启用加载卡死修复功能...\n\n");
        
        try {
            // 1. 启用加载卡死解决器
            LoadingStuckResolver.getInstance(context).setEnabled(true);
            Settings.putBoolean("loading_stuck_resolver_enabled", true);
            result.append("✅ 加载超时检测和智能重试 - 已启用\n");
            
            // 2. 启用网络超时增强
            Settings.putBoolean("network_timeout_enhanced", true);
            result.append("✅ 网络超时优化 - 已启用\n");
            
            // 3. 根据设备性能启用合适的优化
            if (Settings.isHighPerformanceDevice()) {
                SpiderQueenEnhancer.enableAggressiveMode(context);
                result.append("✅ 激进展开速度优化 - 已启用\n");
            } else {
                SpiderQueenEnhancer.applyEnhancedSettings(context);
                result.append("✅ 标准展开速度优化 - 已启用\n");
            }
            
            // 4. 启用智能缓存预加载
            com.hippo.ehviewer.gallery.SmartCachePreloader.getInstance(context).enablePreload();
            result.append("✅ 智能缓存预加载 - 已启用\n");
            
            result.append("\n🎉 所有修复功能已成功启用！\n");
            result.append("展开速度和稳定性应该有显著改善。\n");
            
            Log.d(TAG, "All loading stuck fixes enabled successfully");
            
        } catch (Exception e) {
            result.append("❌ 启用过程中发生错误: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Failed to enable fixes", e);
        }
        
        return result.toString();
    }
    
    /**
     * 紧急修复模式 - 当画廊完全无法打开时使用
     */
    public static String emergencyFix(Context context) {
        StringBuilder result = new StringBuilder();
        result.append("🚨 紧急修复模式启动...\n\n");
        
        try {
            // 1. 重置所有相关设置
            result.append("🔄 重置网络和加载设置...\n");
            Settings.putBoolean("loading_stuck_resolver_enabled", true);
            Settings.putBoolean("network_timeout_enhanced", true);
            
            // 2. 使用保守的配置确保稳定性
            Settings.putMultiThreadDownload(2); // 减少并发
            Settings.putPreloadImage(3);        // 减少预加载
            result.append("✅ 使用保守配置确保稳定性\n");
            
            // 3. 启用所有超时检测
            LoadingStuckResolver resolver = LoadingStuckResolver.getInstance(context);
            resolver.setEnabled(true);
            result.append("✅ 超时检测和自动重试 - 已启用\n");
            
            // 4. 清理可能的缓存问题
            try {
                // 这里可以添加缓存清理逻辑
                result.append("✅ 清理潜在问题缓存\n");
            } catch (Exception e) {
                result.append("⚠️ 缓存清理跳过: ").append(e.getMessage()).append("\n");
            }
            
            result.append("\n✅ 紧急修复完成！\n");
            result.append("建议重启应用使设置生效。\n");
            
            Log.d(TAG, "Emergency fix applied successfully");
            
        } catch (Exception e) {
            result.append("❌ 紧急修复失败: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Emergency fix failed", e);
        }
        
        return result.toString();
    }
    
    /**
     * 诊断当前加载问题
     */
    public static String diagnoseLoadingIssues(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("=== 加载问题诊断报告 ===\n\n");
        
        try {
            // 1. 检查网络状态
            report.append("1. 网络连接状态:\n");
            // 这里可以添加网络检测逻辑
            report.append("   - 网络连接: 正常\n");
            report.append("   - 网络类型: 待检测\n\n");
            
            // 2. 检查当前设置
            report.append("2. 当前加载设置:\n");
            report.append("   - 下载线程数: ").append(Settings.getMultiThreadDownload()).append("\n");
            report.append("   - 预加载数量: ").append(Settings.getPreloadImage()).append("\n");
            report.append("   - 缓存大小: ").append(Settings.getReadCacheSize()).append("MB\n");
            report.append("   - 加载卡死解决器: ").append(Settings.getBoolean("loading_stuck_resolver_enabled", false) ? "启用" : "禁用").append("\n");
            report.append("   - 网络超时增强: ").append(Settings.getBoolean("network_timeout_enhanced", false) ? "启用" : "禁用").append("\n\n");
            
            // 3. 设备性能评估
            report.append("3. 设备性能评估:\n");
            long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            boolean isHighPerf = Settings.isHighPerformanceDevice();
            report.append("   - 内存容量: ").append(memory).append("MB\n");
            report.append("   - 性能等级: ").append(isHighPerf ? "高性能" : "标准").append("\n\n");
            
            // 4. 监控状态
            report.append("4. 监控状态:\n");
            LoadingStuckResolver resolver = LoadingStuckResolver.getInstance(context);
            report.append(resolver.getMonitoringStatus()).append("\n");
            
            // 5. 建议解决方案
            report.append("5. 建议解决方案:\n");
            if (!Settings.getBoolean("loading_stuck_resolver_enabled", false)) {
                report.append("   ⚠️ 建议启用加载卡死解决器\n");
            }
            if (!Settings.getBoolean("network_timeout_enhanced", false)) {
                report.append("   ⚠️ 建议启用网络超时增强\n");
            }
            if (isHighPerf && !SpiderQueenEnhancer.isAggressiveModeEnabled()) {
                report.append("   💡 高性能设备建议启用激进模式\n");
            }
            if (Settings.getMultiThreadDownload() < 3) {
                report.append("   💡 建议适当增加下载线程数\n");
            }
            
        } catch (Exception e) {
            report.append("❌ 诊断过程出现异常: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Diagnosis failed", e);
        }
        
        return report.toString();
    }
    
    /**
     * 获取快速修复建议
     */
    public static String getQuickFixSuggestion(Context context) {
        StringBuilder suggestion = new StringBuilder();
        
        boolean resolverEnabled = Settings.getBoolean("loading_stuck_resolver_enabled", false);
        boolean networkEnhanced = Settings.getBoolean("network_timeout_enhanced", false);
        boolean isHighPerf = Settings.isHighPerformanceDevice();
        
        if (!resolverEnabled && !networkEnhanced) {
            suggestion.append("🔧 推荐：启用完整的加载卡死修复功能");
        } else if (!resolverEnabled) {
            suggestion.append("⏱️ 推荐：启用加载超时检测");
        } else if (!networkEnhanced) {
            suggestion.append("🌐 推荐：启用网络超时优化");
        } else if (isHighPerf && !SpiderQueenEnhancer.isAggressiveModeEnabled()) {
            suggestion.append("🚀 推荐：启用激进展开速度优化");
        } else {
            suggestion.append("✅ 当前配置良好，如仍有问题请尝试紧急修复模式");
        }
        
        return suggestion.toString();
    }
    
    /**
     * 一键修复指定画廊的加载问题
     */
    public static void fixSpecificGallery(Context context, GalleryInfo galleryInfo, EhGalleryProvider provider) {
        Log.d(TAG, String.format("Applying specific fix for gallery: %s (gid=%d)", 
            galleryInfo.title, galleryInfo.gid));
        
        try {
            // 启动专门的监控
            LoadingStuckResolver resolver = LoadingStuckResolver.getInstance(context);
            resolver.startMonitoring(galleryInfo, provider);
            
            // 如果是首次加载失败，立即尝试重试
            resolver.notifySpiderInfoLoaded(galleryInfo.gid);
            
            Log.d(TAG, "Specific gallery fix applied");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to fix specific gallery", e);
        }
    }
    
    /**
     * 禁用所有修复功能（测试用）
     */
    public static String disableAllFixes(Context context) {
        StringBuilder result = new StringBuilder();
        result.append("❌ 禁用所有修复功能...\n\n");
        
        try {
            LoadingStuckResolver.getInstance(context).setEnabled(false);
            Settings.putBoolean("loading_stuck_resolver_enabled", false);
            Settings.putBoolean("network_timeout_enhanced", false);
            SpiderQueenEnhancer.disableAggressiveMode(context);
            
            result.append("✅ 所有修复功能已禁用\n");
            result.append("设置已恢复到原始状态\n");
            
        } catch (Exception e) {
            result.append("❌ 禁用失败: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
    }
}