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

package com.hippo.ehviewer.gallery.enhanced;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider2;

/**
 * 画廊优化性能测试工具
 * 
 * 用于验证优化功能是否正常工作，包括：
 * 1. 设置项读取测试
 * 2. 优化组件初始化测试  
 * 3. 包装器创建测试
 * 4. 性能统计收集测试
 */
public class OptimizationTestUtil {

    private static final String TAG = "OptimizationTestUtil";

    /**
     * 运行完整的优化功能测试
     */
    public static void runOptimizationTest(@NonNull Context context) {
        Log.d(TAG, "=== 开始画廊优化功能测试 ===");
        
        // 1. 测试设置项读取
        testSettingsReading();
        
        // 2. 测试优化管理器
        testOptimizationManager(context);
        
        // 3. 测试包装器创建
        testWrapperCreation(context);
        
        // 4. 测试设置更新
        testSettingsUpdate();
        
        Log.d(TAG, "=== 画廊优化功能测试完成 ===");
    }

    /**
     * 测试设置项读取
     */
    private static void testSettingsReading() {
        Log.d(TAG, "--- 测试设置项读取 ---");
        
        boolean optimizationEnabled = Settings.getGalleryOptimizationEnabled();
        boolean cacheEnabled = Settings.getGallerySmartCacheEnabled();
        boolean preloadEnabled = Settings.getGalleryPreloadEnabled();
        int preloadCount = Settings.getGalleryPreloadCount();
        
        Log.d(TAG, "优化总开关: " + optimizationEnabled);
        Log.d(TAG, "智能缓存: " + cacheEnabled);
        Log.d(TAG, "预加载功能: " + preloadEnabled);
        Log.d(TAG, "预加载数量: " + preloadCount);
        
        // 验证默认值是否正确
        if (!optimizationEnabled) {
            Log.w(TAG, "警告：优化总开关默认应该为true");
        }
        if (preloadCount < 1 || preloadCount > 8) {
            Log.w(TAG, "警告：预加载数量超出预期范围(1-8): " + preloadCount);
        }
    }

    /**
     * 测试优化管理器
     */
    private static void testOptimizationManager(@NonNull Context context) {
        Log.d(TAG, "--- 测试优化管理器 ---");
        
        try {
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            
            // 测试统计信息
            String stats = manager.getPerformanceStats();
            Log.d(TAG, "管理器统计信息:\n" + stats);
            
            // 测试启用/禁用功能
            boolean wasEnabled = manager.isOptimizationEnabled();
            Log.d(TAG, "管理器优化状态: " + wasEnabled);
            
            // 创建测试用的GalleryInfo
            GalleryInfo testGalleryInfo = new GalleryInfo();
            testGalleryInfo.gid = 12345L;
            testGalleryInfo.title = "Test Gallery";
            testGalleryInfo.titleJpn = "テストギャラリー";
            
            // 测试provider创建
            GalleryProvider2 provider = manager.createGalleryProvider(testGalleryInfo);
            
            if (provider instanceof EhGalleryProviderWrapper) {
                Log.d(TAG, "✓ 成功创建优化包装器");
                EhGalleryProviderWrapper wrapper = (EhGalleryProviderWrapper) provider;
                Log.d(TAG, "包装器优化状态: " + wrapper.isOptimizationEnabled());
            } else if (provider instanceof EhGalleryProvider) {
                Log.d(TAG, "✓ 创建了原始provider（可能由于设置或错误）");
            } else {
                Log.w(TAG, "⚠ 创建了未知类型的provider: " + provider.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 优化管理器测试失败", e);
        }
    }

    /**
     * 测试包装器创建和功能
     */
    private static void testWrapperCreation(@NonNull Context context) {
        Log.d(TAG, "--- 测试包装器创建 ---");
        
        try {
            // 创建测试用的GalleryInfo
            GalleryInfo testGalleryInfo = new GalleryInfo();
            testGalleryInfo.gid = 67890L;
            testGalleryInfo.title = "Test Wrapper Gallery";
            
            // 直接创建包装器
            EhGalleryProviderWrapper wrapper = new EhGalleryProviderWrapper(context, testGalleryInfo);
            
            Log.d(TAG, "✓ 包装器创建成功");
            Log.d(TAG, "优化状态: " + wrapper.isOptimizationEnabled());
            
            // 获取统计信息
            String wrapperStats = wrapper.getComprehensiveStats();
            Log.d(TAG, "包装器统计信息:\n" + wrapperStats);
            
            // 测试设置更新功能
            wrapper.updateOptimizationSettings();
            Log.d(TAG, "✓ 设置更新功能调用成功");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 包装器创建测试失败", e);
        }
    }

    /**
     * 测试设置更新
     */
    private static void testSettingsUpdate() {
        Log.d(TAG, "--- 测试设置更新 ---");
        
        try {
            // 保存原始设置
            boolean originalOptimization = Settings.getGalleryOptimizationEnabled();
            boolean originalPreload = Settings.getGalleryPreloadEnabled();
            int originalPreloadCount = Settings.getGalleryPreloadCount();
            
            Log.d(TAG, "原始设置 - 优化: " + originalOptimization + 
                       ", 预加载: " + originalPreload + 
                       ", 数量: " + originalPreloadCount);
            
            // 测试设置修改
            Settings.putGalleryOptimizationEnabled(false);
            Settings.putGalleryPreloadEnabled(false);
            Settings.putGalleryPreloadCount(5);
            
            // 验证修改
            boolean newOptimization = Settings.getGalleryOptimizationEnabled();
            boolean newPreload = Settings.getGalleryPreloadEnabled();
            int newPreloadCount = Settings.getGalleryPreloadCount();
            
            Log.d(TAG, "修改后设置 - 优化: " + newOptimization + 
                       ", 预加载: " + newPreload + 
                       ", 数量: " + newPreloadCount);
            
            if (newOptimization == false && newPreload == false && newPreloadCount == 5) {
                Log.d(TAG, "✓ 设置修改功能正常");
            } else {
                Log.w(TAG, "⚠ 设置修改可能存在问题");
            }
            
            // 恢复原始设置
            Settings.putGalleryOptimizationEnabled(originalOptimization);
            Settings.putGalleryPreloadEnabled(originalPreload);
            Settings.putGalleryPreloadCount(originalPreloadCount);
            
            Log.d(TAG, "✓ 原始设置已恢复");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 设置更新测试失败", e);
        }
    }

    /**
     * 获取优化功能状态报告
     */
    @NonNull
    public static String getOptimizationStatusReport(@NonNull Context context) {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 画廊优化状态报告 ===\n");
        
        // 设置状态
        report.append("\n【用户设置】\n");
        report.append("优化总开关: ").append(Settings.getGalleryOptimizationEnabled()).append("\n");
        report.append("智能缓存: ").append(Settings.getGallerySmartCacheEnabled()).append("\n");
        report.append("预加载功能: ").append(Settings.getGalleryPreloadEnabled()).append("\n");
        report.append("预加载数量: ").append(Settings.getGalleryPreloadCount()).append("\n");
        
        // 管理器状态
        try {
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            report.append("\n【优化管理器】\n");
            report.append("管理器可用: true\n");
            report.append("优化启用: ").append(manager.isOptimizationEnabled()).append("\n");
            report.append(manager.getPerformanceStats());
        } catch (Exception e) {
            report.append("\n【优化管理器】\n");
            report.append("管理器错误: ").append(e.getMessage()).append("\n");
        }
        
        report.append("\n========================");
        
        return report.toString();
    }
}