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
import androidx.annotation.Nullable;

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider2;

/**
 * 第一阶段：画廊优化测试验证工具
 * 
 * 设计目标：
 * 1. 验证包装器正常工作
 * 2. 测试优化管理器功能
 * 3. 验证降级机制
 * 4. 提供诊断信息
 */
public class GalleryOptimizationTest {

    private static final String TAG = "GalleryOptimizationTest";
    
    /**
     * 运行完整的优化测试
     */
    public static void runFullOptimizationTest(@NonNull Context context) {
        Log.i(TAG, "=== Starting Gallery Optimization Full Test ===");
        
        try {
            // 测试1: 管理器初始化
            testManagerInitialization(context);
            
            // 测试2: 包装器创建
            testWrapperCreation(context);
            
            // 测试3: 降级机制
            testFallbackMechanism(context);
            
            // 测试4: 统计信息
            testStatistics(context);
            
            Log.i(TAG, "=== Gallery Optimization Full Test Completed Successfully ===");
            
        } catch (Exception e) {
            Log.e(TAG, "=== Gallery Optimization Full Test Failed ===", e);
        }
    }

    /**
     * 测试管理器初始化
     */
    public static void testManagerInitialization(@NonNull Context context) {
        Log.i(TAG, "--- Testing Manager Initialization ---");
        
        try {
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            
            if (manager.isManagerInitialized()) {
                Log.i(TAG, "✓ Manager initialized successfully");
            } else {
                Log.e(TAG, "✗ Manager initialization failed");
                return;
            }
            
            if (manager.isOptimizationEnabled()) {
                Log.i(TAG, "✓ Optimization is enabled by default");
            } else {
                Log.w(TAG, "⚠ Optimization is disabled by default");
            }
            
            Log.i(TAG, "Manager Status: " + manager.getStatusSummary());
            
        } catch (Exception e) {
            Log.e(TAG, "Manager initialization test failed", e);
            throw e;
        }
    }

    /**
     * 测试包装器创建
     */
    public static void testWrapperCreation(@NonNull Context context) {
        Log.i(TAG, "--- Testing Wrapper Creation ---");
        
        try {
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            
            // 创建测试用的GalleryInfo
            GalleryInfo testGalleryInfo = createTestGalleryInfo();
            
            // 测试包装器创建
            GalleryProvider2 provider = manager.createGalleryProvider(testGalleryInfo);
            
            if (provider != null) {
                Log.i(TAG, "✓ Provider created successfully: " + provider.getClass().getSimpleName());
                
                if (provider instanceof EhGalleryProviderWrapper) {
                    EhGalleryProviderWrapper wrapper = (EhGalleryProviderWrapper) provider;
                    Log.i(TAG, "✓ Provider is optimized wrapper");
                    Log.i(TAG, "Wrapper Status: " + wrapper.getWrapperStatus());
                    
                    if (wrapper.isInitialized()) {
                        Log.i(TAG, "✓ Wrapper is properly initialized");
                    } else {
                        Log.e(TAG, "✗ Wrapper initialization failed");
                    }
                    
                } else if (provider instanceof EhGalleryProvider) {
                    Log.w(TAG, "⚠ Provider is original implementation (fallback)");
                } else {
                    Log.w(TAG, "⚠ Provider is unexpected type: " + provider.getClass().getSimpleName());
                }
                
            } else {
                Log.e(TAG, "✗ Provider creation failed - returned null");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Wrapper creation test failed", e);
            throw e;
        }
    }

    /**
     * 测试降级机制
     */
    public static void testFallbackMechanism(@NonNull Context context) {
        Log.i(TAG, "--- Testing Fallback Mechanism ---");
        
        try {
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            
            // 禁用优化
            manager.disableOptimizations();
            Log.i(TAG, "Optimization disabled for fallback test");
            
            // 创建提供者（应该是原始实现）
            GalleryInfo testGalleryInfo = createTestGalleryInfo();
            GalleryProvider2 provider = manager.createGalleryProvider(testGalleryInfo);
            
            if (provider instanceof EhGalleryProvider && !(provider instanceof EhGalleryProviderWrapper)) {
                Log.i(TAG, "✓ Fallback to original provider successful");
            } else {
                Log.w(TAG, "⚠ Fallback test unexpected result: " + provider.getClass().getSimpleName());
            }
            
            // 重新启用优化
            manager.enableOptimizations();
            Log.i(TAG, "Optimization re-enabled");
            
            // 再次创建提供者（应该是包装器）
            GalleryProvider2 provider2 = manager.createGalleryProvider(testGalleryInfo);
            
            if (provider2 instanceof EhGalleryProviderWrapper) {
                Log.i(TAG, "✓ Re-enable optimization successful");
            } else {
                Log.w(TAG, "⚠ Re-enable test unexpected result: " + provider2.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Fallback mechanism test failed", e);
            throw e;
        }
    }

    /**
     * 测试统计信息
     */
    public static void testStatistics(@NonNull Context context) {
        Log.i(TAG, "--- Testing Statistics ---");
        
        try {
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            
            // 重置统计
            manager.resetStats();
            Log.i(TAG, "Statistics reset");
            
            // 创建几个提供者
            GalleryInfo testGalleryInfo = createTestGalleryInfo();
            
            manager.createGalleryProvider(testGalleryInfo);
            manager.createGalleryProvider(testGalleryInfo);
            manager.createGalleryProvider(testGalleryInfo);
            
            // 获取统计信息
            String stats = manager.getPerformanceStats();
            Log.i(TAG, "Performance Statistics:\n" + stats);
            
            String summary = manager.getStatusSummary();
            Log.i(TAG, "Status Summary: " + summary);
            
        } catch (Exception e) {
            Log.e(TAG, "Statistics test failed", e);
            throw e;
        }
    }

    /**
     * 创建测试用的GalleryInfo
     */
    @NonNull
    private static GalleryInfo createTestGalleryInfo() {
        GalleryInfo galleryInfo = new GalleryInfo();
        galleryInfo.gid = 12345;
        galleryInfo.title = "Test Gallery for Optimization";
        galleryInfo.pages = 50;
        return galleryInfo;
    }

    /**
     * 快速验证优化是否可用
     */
    public static boolean isOptimizationAvailable(@NonNull Context context) {
        try {
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            return manager.isOptimizationEnabled() && manager.isManagerInitialized();
        } catch (Exception e) {
            Log.e(TAG, "Failed to check optimization availability", e);
            return false;
        }
    }

    /**
     * 获取优化状态摘要
     */
    @NonNull
    public static String getOptimizationStatusSummary(@NonNull Context context) {
        try {
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            return manager.getStatusSummary();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get status summary", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 强制清理资源（测试用）
     */
    public static void forceCleanup() {
        Log.i(TAG, "Force cleanup requested");
        GalleryOptimizationManager.destroyInstance();
        Log.i(TAG, "Cleanup completed");
    }

    /**
     * 简单健康检查
     */
    public static void runHealthCheck(@NonNull Context context) {
        Log.i(TAG, "=== Running Health Check ===");
        
        try {
            // 检查管理器
            GalleryOptimizationManager manager = GalleryOptimizationManager.getInstance(context);
            boolean available = manager.isOptimizationEnabled();
            
            // 检查包装器
            GalleryInfo testInfo = createTestGalleryInfo();
            GalleryProvider2 provider = manager.createGalleryProvider(testInfo);
            boolean wrapperWorking = provider instanceof EhGalleryProviderWrapper;
            
            Log.i(TAG, String.format("Health Check Result: Available=%s, Wrapper=%s", 
                  available, wrapperWorking));
            
            if (available && wrapperWorking) {
                Log.i(TAG, "✓ Gallery optimization is healthy");
            } else {
                Log.w(TAG, "⚠ Gallery optimization has issues");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Health check failed", e);
        }
    }
}