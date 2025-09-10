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
import com.hippo.ehviewer.spider.SpiderQueen;
import com.hippo.lib.glgallery.GalleryProvider;
import com.hippo.lib.image.Image;
import com.hippo.unifile.UniFile;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 第一阶段：EhGalleryProvider的透明包装器
 * 
 * 设计目标：
 * 1. 完全透明的代理，不改变任何行为
 * 2. 为后续优化预留扩展点
 * 3. 提供降级机制，确保稳定性
 * 4. 添加基础日志和监控能力
 */
public class EhGalleryProviderWrapper extends GalleryProvider2 implements SpiderQueen.OnSpiderListener {

    private static final String TAG = "GalleryWrapper";
    
    // 原始提供者实例
    private final EhGalleryProvider mOriginalProvider;
    
    // 包装器状态标记
    private final AtomicBoolean mWrapperEnabled = new AtomicBoolean(true);
    private final AtomicBoolean mInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mOptimizationEnabled = new AtomicBoolean(false);
    
    // 优化组件（第二阶段）
    private SmartCacheManager mCacheManager;
    private EnhancedImageLoader mImageLoader;
    private LoadingStateOptimizer mStateOptimizer;
    
    // 构造函数：创建原始提供者
    public EhGalleryProviderWrapper(Context context, GalleryInfo galleryInfo) {
        Log.d(TAG, "Creating EhGalleryProviderWrapper for gallery: " + 
              (galleryInfo != null ? galleryInfo.title : "unknown"));
        
        // 创建原始提供者实例
        mOriginalProvider = new EhGalleryProvider(context, galleryInfo);
        
        // 初始化优化组件（第二阶段）
        try {
            initializeOptimizationComponents(context);
            mOptimizationEnabled.set(true);
            Log.d(TAG, "Optimization components initialized successfully");
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize optimization components, using basic wrapper only", e);
            mOptimizationEnabled.set(false);
        }
        
        mInitialized.set(true);
        Log.d(TAG, "EhGalleryProviderWrapper initialized successfully");
    }

    // ===============================
    // GalleryProvider 基础接口代理
    // ===============================

    @Override
    public void start() {
        Log.d(TAG, "start() called");
        try {
            mOriginalProvider.start();
            Log.d(TAG, "Original provider started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start original provider", e);
            throw e; // 重新抛出异常，保持原有行为
        }
    }


    @Override
    public int size() {
        try {
            int size = mOriginalProvider.size();
            Log.d(TAG, "size() returned: " + size);
            return size;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get size", e);
            throw e;
        }
    }


    @Override
    @NonNull
    public String getImageFilename(int index) {
        try {
            String filename = mOriginalProvider.getImageFilename(index);
            Log.d(TAG, "getImageFilename(" + index + ") = " + filename);
            return filename;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get image filename for index: " + index, e);
            throw e;
        }
    }

    @Override
    public boolean save(int index, @NonNull UniFile file) {
        Log.d(TAG, "save() called for index: " + index + ", file: " + file.getName());
        try {
            boolean result = mOriginalProvider.save(index, file);
            Log.d(TAG, "save() result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save index: " + index, e);
            throw e;
        }
    }

    @Override
    @Nullable
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        Log.d(TAG, "save() called for index: " + index + ", dir: " + dir.getName() + ", filename: " + filename);
        try {
            UniFile result = mOriginalProvider.save(index, dir, filename);
            Log.d(TAG, "save() result: " + (result != null ? result.getName() : "null"));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save index: " + index + " to dir", e);
            throw e;
        }
    }

    @Override
    public int getStartPage() {
        try {
            int startPage = mOriginalProvider.getStartPage();
            Log.d(TAG, "getStartPage() = " + startPage);
            return startPage;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get start page", e);
            throw e;
        }
    }

    @Override
    public void putStartPage(int page) {
        Log.d(TAG, "putStartPage() called with page: " + page);
        try {
            mOriginalProvider.putStartPage(page);
        } catch (Exception e) {
            Log.e(TAG, "Failed to put start page: " + page, e);
            throw e;
        }
    }

    @Override
    public String getError() {
        try {
            String error = mOriginalProvider.getError();
            Log.d(TAG, "getError() = " + error);
            return error;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get error", e);
            return "Error getting error info: " + e.getMessage();
        }
    }

    // ===============================
    // SpiderQueen.OnSpiderListener 接口代理
    // ===============================

    @Override
    public void onGetPages(int pages) {
        Log.d(TAG, "onGetPages() called with pages: " + pages);
        try {
            // 直接转发到原始provider的监听器
            if (mOriginalProvider instanceof SpiderQueen.OnSpiderListener) {
                ((SpiderQueen.OnSpiderListener) mOriginalProvider).onGetPages(pages);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle onGetPages", e);
            // 事件处理失败不应该影响主流程
        }
    }

    @Override
    public void onGet509(int index) {
        Log.d(TAG, "onGet509() called for index: " + index);
        try {
            if (mOriginalProvider instanceof SpiderQueen.OnSpiderListener) {
                ((SpiderQueen.OnSpiderListener) mOriginalProvider).onGet509(index);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle onGet509", e);
        }
    }

    @Override
    public void onPageDownload(int index, long contentLength, long receivedSize, int bytesRead) {
        // 这个方法调用频繁，只在调试模式下记录
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onPageDownload() index: " + index + ", progress: " + receivedSize + "/" + contentLength);
        }
        try {
            if (mOriginalProvider instanceof SpiderQueen.OnSpiderListener) {
                ((SpiderQueen.OnSpiderListener) mOriginalProvider).onPageDownload(index, contentLength, receivedSize, bytesRead);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle onPageDownload", e);
        }
    }

    @Override
    public void onPageSuccess(int index, int finished, int downloaded, int total) {
        Log.d(TAG, "onPageSuccess() index: " + index + ", progress: " + finished + "/" + total);
        try {
            if (mOriginalProvider instanceof SpiderQueen.OnSpiderListener) {
                ((SpiderQueen.OnSpiderListener) mOriginalProvider).onPageSuccess(index, finished, downloaded, total);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle onPageSuccess", e);
        }
    }

    @Override
    public void onPageFailure(int index, String error, int finished, int downloaded, int total) {
        Log.w(TAG, "onPageFailure() index: " + index + ", error: " + error + ", progress: " + finished + "/" + total);
        try {
            if (mOriginalProvider instanceof SpiderQueen.OnSpiderListener) {
                ((SpiderQueen.OnSpiderListener) mOriginalProvider).onPageFailure(index, error, finished, downloaded, total);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle onPageFailure", e);
        }
    }

    @Override
    public void onFinish(int finished, int downloaded, int total) {
        Log.d(TAG, "onFinish() called, progress: " + finished + "/" + total);
        try {
            if (mOriginalProvider instanceof SpiderQueen.OnSpiderListener) {
                ((SpiderQueen.OnSpiderListener) mOriginalProvider).onFinish(finished, downloaded, total);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle onFinish", e);
        }
    }

    @Override
    public void onGetImageSuccess(int index, Image image) {
        Log.d(TAG, "onGetImageSuccess() called for index: " + index);
        
        // 优化处理：存入缓存并通知状态优化器
        if (isOptimizationEnabled()) {
            try {
                // 将图片存入智能缓存
                String cacheKey = generateImageCacheKey(index);
                mCacheManager.putImage(cacheKey, image, SmartCacheManager.PRIORITY_HIGH);
                
                // 通知状态优化器加载成功
                mStateOptimizer.onLoadSuccess(index, image);
                
                Log.d(TAG, "Image cached and state updated for index: " + index);
            } catch (Exception e) {
                Log.e(TAG, "Failed to process optimized image success", e);
            }
        }
        
        // 调用原始provider处理
        try {
            if (mOriginalProvider instanceof SpiderQueen.OnSpiderListener) {
                ((SpiderQueen.OnSpiderListener) mOriginalProvider).onGetImageSuccess(index, image);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle onGetImageSuccess", e);
        }
    }

    @Override
    public void onGetImageFailure(int index, String error) {
        Log.w(TAG, "onGetImageFailure() index: " + index + ", error: " + error);
        
        // 优化处理：通知状态优化器加载失败
        if (isOptimizationEnabled()) {
            try {
                mStateOptimizer.onLoadFailure(index, error);
                Log.d(TAG, "State optimizer notified of failure for index: " + index);
            } catch (Exception e) {
                Log.e(TAG, "Failed to process optimized image failure", e);
            }
        }
        
        // 调用原始provider处理
        try {
            if (mOriginalProvider instanceof SpiderQueen.OnSpiderListener) {
                ((SpiderQueen.OnSpiderListener) mOriginalProvider).onGetImageFailure(index, error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle onGetImageFailure", e);
        }
    }

    // ===============================
    // 包装器状态和工具方法
    // ===============================

    /**
     * 检查包装器是否已初始化
     */
    public boolean isInitialized() {
        return mInitialized.get();
    }

    /**
     * 检查包装器是否启用
     */
    public boolean isWrapperEnabled() {
        return mWrapperEnabled.get();
    }

    /**
     * 启用/禁用包装器（预留接口）
     */
    public void setWrapperEnabled(boolean enabled) {
        mWrapperEnabled.set(enabled);
        Log.d(TAG, "Wrapper enabled set to: " + enabled);
    }

    /**
     * 获取原始提供者实例（用于调试和测试）
     */
    @NonNull
    public EhGalleryProvider getOriginalProvider() {
        return mOriginalProvider;
    }

    /**
     * 获取包装器状态信息
     */
    @NonNull
    public String getWrapperStatus() {
        return String.format("EhGalleryProviderWrapper[initialized=%s, enabled=%s, optimization=%s, size=%d]",
                mInitialized.get(), mWrapperEnabled.get(), mOptimizationEnabled.get(),
                mOriginalProvider != null ? mOriginalProvider.size() : -1);
    }

    /**
     * 检查优化功能是否可用
     */
    public boolean isOptimizationEnabled() {
        // 检查用户设置中的总开关
        if (!Settings.getGalleryOptimizationEnabled()) {
            return false;
        }
        
        // 检查组件初始化状态
        return mOptimizationEnabled.get() && mCacheManager != null && mImageLoader != null;
    }

    /**
     * 获取综合性能统计
     */
    @NonNull
    public String getComprehensiveStats() {
        if (!isOptimizationEnabled()) {
            return "Optimization components not available\n" + getWrapperStatus();
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("=== EhGalleryProviderWrapper Comprehensive Stats ===\n");
        stats.append("Wrapper Status: ").append(getWrapperStatus()).append("\n\n");
        
        if (mCacheManager != null) {
            stats.append(mCacheManager.getCacheStats()).append("\n\n");
        }
        
        if (mImageLoader != null) {
            stats.append(mImageLoader.getLoadingStats()).append("\n\n");
        }
        
        if (mStateOptimizer != null) {
            stats.append(mStateOptimizer.getPerformanceStats()).append("\n");
        }
        
        return stats.toString();
    }

    // ===============================
    // 第二阶段优化组件初始化和集成
    // ===============================

    /**
     * 初始化优化组件
     */
    private void initializeOptimizationComponents(Context context) {
        Log.d(TAG, "Initializing optimization components");
        
        // 初始化智能缓存管理器
        mCacheManager = new SmartCacheManager(context);
        context.registerComponentCallbacks(mCacheManager);
        
        // 初始化增强图片加载器
        mImageLoader = new EnhancedImageLoader(mOriginalProvider, mCacheManager);
        
        // 初始化加载状态优化器
        mStateOptimizer = new LoadingStateOptimizer();
        
        // 设置状态变化监听器，集成各组件
        mStateOptimizer.setStateChangeListener(new LoadingStateOptimizer.StateChangeListener() {
            @Override
            public void onStateChanged(int index, LoadingStateOptimizer.StateType type, 
                                     float progress, @Nullable Image image, @Nullable String message) {
                handleOptimizedStateChange(index, type, progress, image, message);
            }
        });
        
        // 配置优化组件使用Settings中的用户设置
        configureOptimizationComponents();
        
        Log.d(TAG, "All optimization components initialized successfully");
    }

    /**
     * 处理优化后的状态变化
     */
    private void handleOptimizedStateChange(int index, LoadingStateOptimizer.StateType type, 
                                          float progress, @Nullable Image image, @Nullable String message) {
        switch (type) {
            case LOADING_SUCCESS:
                if (image != null) {
                    // 将图片存储到缓存
                    String cacheKey = generateImageCacheKey(index);
                    mCacheManager.putImage(cacheKey, image, SmartCacheManager.PRIORITY_NORMAL);
                    
                    // 通知原始Provider的监听器
                    notifyPageSucceed(index, image);
                }
                break;
                
            case LOADING_ERROR:
                if (message != null) {
                    notifyPageFailed(index, message);
                }
                break;
                
            case LOADING_PROGRESS:
                notifyPagePercent(index, progress);
                break;
                
            default:
                // 其他状态暂不处理
                break;
        }
    }

    /**
     * 更新优化组件配置（外部调用）
     */
    public void updateOptimizationSettings() {
        Log.d(TAG, "Updating optimization settings from external call");
        configureOptimizationComponents();
    }

    /**
     * 配置优化组件使用用户设置
     */
    private void configureOptimizationComponents() {
        Log.d(TAG, "Configuring optimization components with user settings");
        
        // 配置预加载设置
        boolean preloadEnabled = Settings.getGalleryPreloadEnabled();
        int preloadCount = Settings.getGalleryPreloadCount();
        
        if (mImageLoader != null) {
            mImageLoader.setPreloadConfig(preloadEnabled, preloadCount);
            Log.d(TAG, "Image loader configured: preload=" + preloadEnabled + ", count=" + preloadCount);
        }
        
        // 智能缓存默认总是启用，但可以通过设置控制某些行为
        // 这里可以根据需要添加更多配置
        
        Log.d(TAG, "Optimization components configuration completed");
    }

    /**
     * 调用原始Provider的onRequest方法
     */
    private void callOriginalOnRequest(int index) {
        try {
            java.lang.reflect.Method method = EhGalleryProvider.class.getDeclaredMethod("onRequest", int.class);
            method.setAccessible(true);
            method.invoke(mOriginalProvider, index);
            Log.d(TAG, "Called original onRequest for index: " + index);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original onRequest for index: " + index, e);
            notifyPageFailed(index, "Request failed: " + e.getMessage());
        }
    }

    /**
     * 触发智能预加载
     */
    private void triggerSmartPreload(int currentIndex) {
        if (mCacheManager == null) return;
        
        // 在后台线程中执行预加载逻辑
        new Thread(() -> {
            try {
                int totalSize = size();
                if (totalSize <= 0) return;
                
                int preloadCount = Settings.getGalleryPreloadCount();
                int[] predictedIndices = mCacheManager.predictNextIndices(currentIndex, totalSize, preloadCount);
                
                Log.d(TAG, "Smart preload: current=" + currentIndex + ", predicted=" + predictedIndices.length + " images");
                
                // 预加载预测的图片
                for (int index : predictedIndices) {
                    if (index >= 0 && index < totalSize && index != currentIndex) {
                        // 检查是否已经在加载或已缓存
                        String cacheKey = generateImageCacheKey(index);
                        if (mCacheManager.getImage(cacheKey, SmartCacheManager.PRIORITY_NORMAL) == null) {
                            // 触发预加载请求（低优先级）
                            callOriginalOnRequest(index);
                            
                            // 避免过于频繁的预加载请求
                            Thread.sleep(100);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Smart preload failed", e);
            }
        }, "SmartPreload-" + currentIndex).start();
    }

    /**
     * 生成图片缓存键
     */
    private String generateImageCacheKey(int index) {
        return mOriginalProvider.getImageFilename(index);
    }

    /**
     * 优化后的图片请求处理
     */
    @Override
    protected void onRequest(int index) {
        if (isOptimizationEnabled()) {
            Log.d(TAG, "Optimized onRequest() for index: " + index);
            
            // 使用状态优化器开始加载
            mStateOptimizer.startLoading(index, null);
            
            // 直接调用原始实现，让原始的SpiderQueen机制工作
            // 我们会在notifyPageSucceed等回调方法中拦截结果
            callOriginalOnRequest(index);
            
            // 触发预加载（如果启用）
            if (Settings.getGalleryPreloadEnabled()) {
                triggerSmartPreload(index);
            }
        } else {
            // 降级到原始实现
            callOriginalOnRequest(index);
        }
    }

    /**
     * 优化后的强制请求处理
     */
    @Override
    protected void onForceRequest(int index) {
        if (isOptimizationEnabled()) {
            Log.d(TAG, "Optimized onForceRequest() for index: " + index);
            
            // 强制重新加载
            mImageLoader.forceReload(index, SmartCacheManager.PRIORITY_CRITICAL,
                new EnhancedImageLoader.LoadingCallback() {
                    @Override
                    public void onLoadSuccess(int idx, @NonNull Image image) {
                        mStateOptimizer.onLoadSuccess(idx, image);
                    }
                    
                    @Override
                    public void onLoadFailure(int idx, @NonNull String error) {
                        mStateOptimizer.onLoadFailure(idx, error);
                    }
                    
                    @Override
                    public void onLoadProgress(int idx, float progress) {
                        mStateOptimizer.updateProgress(idx, progress);
                    }
                });
        } else {
            // 降级到原始实现（使用反射调用）
            try {
                java.lang.reflect.Method method = EhGalleryProvider.class.getDeclaredMethod("onForceRequest", int.class);
                method.setAccessible(true);
                method.invoke(mOriginalProvider, index);
            } catch (Exception e) {
                Log.e(TAG, "Failed to call original onForceRequest for index: " + index, e);
                notifyPageFailed(index, "Force request failed: " + e.getMessage());
            }
        }
    }

    /**
     * 优化后的取消请求处理
     */
    @Override
    protected void onCancelRequest(int index) {
        if (isOptimizationEnabled()) {
            Log.d(TAG, "Optimized onCancelRequest() for index: " + index);
            
            // 取消优化组件中的加载
            mImageLoader.cancelLoad(index);
            mStateOptimizer.cancelLoading(index);
        }
        
        // 同时取消原始Provider的请求（使用反射调用）
        try {
            java.lang.reflect.Method method = EhGalleryProvider.class.getDeclaredMethod("onCancelRequest", int.class);
            method.setAccessible(true);
            method.invoke(mOriginalProvider, index);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original onCancelRequest for index: " + index, e);
            // 取消请求失败不需要特殊处理
        }
    }

    /**
     * 清理优化组件资源
     */
    @Override
    public void stop() {
        Log.d(TAG, "Stopping optimized wrapper");
        
        // 清理优化组件
        if (mImageLoader != null) {
            mImageLoader.destroy();
        }
        
        if (mStateOptimizer != null) {
            mStateOptimizer.clearAllStates();
        }
        
        if (mCacheManager != null) {
            // 注意：不要清理缓存，可能有其他实例在使用
            // mCacheManager.clearCache();
        }
        
        // 调用原始stop方法
        try {
            mOriginalProvider.stop();
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop original provider", e);
        }
    }

}