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

import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 第一阶段：画廊优化管理器
 * 
 * 设计目标：
 * 1. 统一管理优化组件的创建和销毁
 * 2. 提供全局开关控制优化功能
 * 3. 实现降级机制确保稳定性
 * 4. 收集基础统计信息
 */
public class GalleryOptimizationManager {

    private static final String TAG = "GalleryOptimizationMgr";
    
    // 单例实例
    private static volatile GalleryOptimizationManager sInstance;
    private static final Object sLock = new Object();
    
    // 上下文和配置
    private final Context mContext;
    
    // 全局优化开关
    private final AtomicBoolean mOptimizationEnabled = new AtomicBoolean(true);
    private final AtomicBoolean mManagerInitialized = new AtomicBoolean(false);
    
    // 提供者缓存（第一阶段暂不使用，预留给后续阶段）
    private final ConcurrentHashMap<Long, EhGalleryProviderWrapper> mProviderCache = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong mTotalProvidersCreated = new AtomicLong(0);
    private final AtomicLong mOptimizedProvidersCreated = new AtomicLong(0);
    private final AtomicLong mFallbackProvidersCreated = new AtomicLong(0);
    
    // 私有构造函数
    private GalleryOptimizationManager(Context context) {
        mContext = context.getApplicationContext();
        mManagerInitialized.set(true);
        Log.d(TAG, "GalleryOptimizationManager initialized");
    }

    /**
     * 获取单例实例
     */
    @NonNull
    public static GalleryOptimizationManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new GalleryOptimizationManager(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * 创建画廊提供者
     * 
     * 新方案：直接使用原始EhGalleryProvider，通过Settings优化现有SpiderQueen系统
     */
    @NonNull
    public GalleryProvider2 createGalleryProvider(@NonNull GalleryInfo galleryInfo) {
        Log.d(TAG, "Creating gallery provider for: " + galleryInfo.title);
        
        mTotalProvidersCreated.incrementAndGet();
        
        try {
            // 直接创建原始EhGalleryProvider，让SpiderQueen发挥其内置优化能力
            EhGalleryProvider provider = new EhGalleryProvider(mContext, galleryInfo);
            
            Log.d(TAG, "Successfully created original EhGalleryProvider with SpiderQueen optimizations");
            mOptimizedProvidersCreated.incrementAndGet();
            return provider;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create gallery provider", e);
            mFallbackProvidersCreated.incrementAndGet();
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    /**
     * 启用优化功能
     */
    public void enableOptimizations() {
        Log.d(TAG, "Enabling gallery optimizations");
        mOptimizationEnabled.set(true);
    }

    /**
     * 禁用优化功能
     */
    public void disableOptimizations() {
        Log.d(TAG, "Disabling gallery optimizations");
        mOptimizationEnabled.set(false);
        
        // 清理缓存的提供者（如果有的话）
        clearProviderCache();
    }

    /**
     * 检查优化是否启用
     */
    public boolean isOptimizationEnabled() {
        return mOptimizationEnabled.get() && mManagerInitialized.get();
    }

    /**
     * 检查管理器是否已初始化
     */
    public boolean isManagerInitialized() {
        return mManagerInitialized.get();
    }

    /**
     * 获取性能统计信息
     */
    @NonNull
    public String getPerformanceStats() {
        return String.format(
            "GalleryOptimizationManager Stats:\n" +
            "- Optimization Enabled: %s\n" +
            "- Manager Initialized: %s\n" +
            "- Total Providers Created: %d\n" +
            "- Optimized Providers: %d\n" +
            "- Fallback Providers: %d\n" +
            "- Optimization Success Rate: %.1f%%\n" +
            "- Active Cached Providers: %d",
            mOptimizationEnabled.get(),
            mManagerInitialized.get(),
            mTotalProvidersCreated.get(),
            mOptimizedProvidersCreated.get(),
            mFallbackProvidersCreated.get(),
            calculateSuccessRate(),
            mProviderCache.size()
        );
    }

    /**
     * 计算优化成功率
     */
    private double calculateSuccessRate() {
        long total = mTotalProvidersCreated.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) mOptimizedProvidersCreated.get() / total * 100.0;
    }

    /**
     * 清理提供者缓存
     */
    private void clearProviderCache() {
        Log.d(TAG, "Clearing provider cache, size: " + mProviderCache.size());
        mProviderCache.clear();
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        Log.d(TAG, "Resetting performance statistics");
        mTotalProvidersCreated.set(0);
        mOptimizedProvidersCreated.set(0);
        mFallbackProvidersCreated.set(0);
        clearProviderCache();
    }

    /**
     * 销毁管理器实例（测试用）
     */
    public static void destroyInstance() {
        synchronized (sLock) {
            if (sInstance != null) {
                sInstance.clearProviderCache();
                sInstance = null;
                Log.d(TAG, "GalleryOptimizationManager instance destroyed");
            }
        }
    }

    /**
     * 获取管理器状态摘要
     */
    @NonNull
    public String getStatusSummary() {
        return String.format("GalleryOptimizationManager[enabled=%s, providers=%d/%d, success=%.1f%%]",
                mOptimizationEnabled.get(),
                mOptimizedProvidersCreated.get(),
                mTotalProvidersCreated.get(),
                calculateSuccessRate());
    }

    // ===============================
    // 预留接口（后续阶段使用）
    // ===============================

    /**
     * 设置缓存大小限制（预留接口）
     */
    public void setCacheSizeLimit(int limit) {
        Log.d(TAG, "Cache size limit set to: " + limit + " (not implemented in phase 1)");
        // TODO: 在第二阶段实现
    }

    /**
     * 强制清理内存（预留接口）
     */
    public void forceMemoryCleanup() {
        Log.d(TAG, "Force memory cleanup requested (not implemented in phase 1)");
        // TODO: 在第二阶段实现
        clearProviderCache(); // 目前只清理缓存
    }

    /**
     * 获取内存使用统计（预留接口）
     */
    @NonNull
    public String getMemoryStats() {
        return "Memory stats not available in phase 1";
        // TODO: 在第二阶段实现
    }
}