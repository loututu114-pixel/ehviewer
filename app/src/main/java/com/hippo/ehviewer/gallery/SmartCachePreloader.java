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

package com.hippo.ehviewer.gallery;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.spider.SpiderQueen;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智能缓存预加载器
 * 
 * 在用户浏览画廊列表时预热缓存，大幅提升画廊展开速度
 */
public class SmartCachePreloader {
    
    private static final String TAG = "SmartCachePreloader";
    
    // 单例模式
    private static volatile SmartCachePreloader sInstance;
    private static final Object sLock = new Object();
    
    // 预加载配置
    private static final int PRELOAD_DELAY_MS = 2000;      // 延迟2秒开始预加载
    private static final int MAX_PRELOAD_GALLERIES = 5;    // 最多预加载5个画廊
    private static final int PRELOAD_IMAGES_PER_GALLERY = 3; // 每个画廊预加载3张图片
    
    private final Context mContext;
    private final ExecutorService mPreloadExecutor;
    private final Handler mMainHandler;
    private final ConcurrentHashMap<Long, PreloadSession> mActiveSessions;
    private final AtomicBoolean mEnabled;
    
    private SmartCachePreloader(Context context) {
        mContext = context.getApplicationContext();
        mPreloadExecutor = Executors.newFixedThreadPool(2); // 2个专门的预加载线程
        mMainHandler = new Handler(Looper.getMainLooper());
        mActiveSessions = new ConcurrentHashMap<>();
        mEnabled = new AtomicBoolean(true);
        
        Log.d(TAG, "SmartCachePreloader initialized");
    }
    
    /**
     * 获取单例实例
     */
    public static SmartCachePreloader getInstance(Context context) {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new SmartCachePreloader(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 预热画廊缓存 - 当用户在画廊列表中浏览时调用
     */
    public void preloadGalleryCache(GalleryInfo galleryInfo, int priority) {
        if (!mEnabled.get() || !shouldPreload()) {
            return;
        }
        
        Long gid = galleryInfo.gid;
        if (mActiveSessions.containsKey(gid)) {
            return; // 已经在预加载
        }
        
        Log.d(TAG, String.format("Starting preload for gallery: %s (gid=%d, priority=%d)", 
            galleryInfo.title, gid, priority));
        
        PreloadSession session = new PreloadSession(galleryInfo, priority);
        mActiveSessions.put(gid, session);
        
        // 延迟执行预加载，避免影响列表滚动
        mMainHandler.postDelayed(() -> {
            if (mActiveSessions.containsKey(gid)) {
                mPreloadExecutor.submit(() -> executePreload(session));
            }
        }, PRELOAD_DELAY_MS);
    }
    
    /**
     * 取消画廊预加载
     */
    public void cancelPreload(long gid) {
        PreloadSession session = mActiveSessions.remove(gid);
        if (session != null) {
            session.cancel();
            Log.d(TAG, "Cancelled preload for gid: " + gid);
        }
    }
    
    /**
     * 批量预热多个画廊
     */
    public void batchPreloadGalleries(GalleryInfo[] galleries) {
        if (!mEnabled.get() || galleries == null) {
            return;
        }
        
        int count = Math.min(galleries.length, MAX_PRELOAD_GALLERIES);
        for (int i = 0; i < count; i++) {
            preloadGalleryCache(galleries[i], i); // 优先级递减
        }
    }
    
    /**
     * 执行预加载逻辑
     */
    private void executePreload(PreloadSession session) {
        try {
            if (session.isCancelled()) {
                return;
            }
            
            Log.d(TAG, "Executing preload for: " + session.galleryInfo.title);
            
            // 创建临时的SpiderQueen进行预加载
            SpiderQueen tempQueen = SpiderQueen.obtainSpiderQueen(
                mContext, session.galleryInfo, SpiderQueen.MODE_READ);
            
            if (tempQueen == null) {
                Log.w(TAG, "Failed to obtain SpiderQueen for preload");
                return;
            }
            
            try {
                // 预加载前几张图片
                for (int i = 0; i < PRELOAD_IMAGES_PER_GALLERY && !session.isCancelled(); i++) {
                    Object result = tempQueen.request(i);
                    
                    if (result == null) {
                        // 请求已加入队列，这就足够了
                        Log.d(TAG, String.format("Preload request queued: gid=%d, index=%d", 
                            session.galleryInfo.gid, i));
                    }
                    
                    // 短暂延迟，避免过度占用资源
                    Thread.sleep(100);
                }
                
                Log.d(TAG, "Preload completed for: " + session.galleryInfo.title);
                
            } finally {
                // 释放临时SpiderQueen
                mMainHandler.postDelayed(() -> {
                    SpiderQueen.releaseSpiderQueen(tempQueen, SpiderQueen.MODE_READ);
                }, 5000); // 5秒后释放
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Preload execution failed", e);
        } finally {
            mActiveSessions.remove(session.galleryInfo.gid);
        }
    }
    
    /**
     * 判断是否应该执行预加载
     */
    private boolean shouldPreload() {
        // 只在高性能设备且WiFi环境下激进预加载
        if (!Settings.isHighPerformanceDevice()) {
            return false;
        }
        
        // 检查当前活跃的预加载会话数量
        if (mActiveSessions.size() >= MAX_PRELOAD_GALLERIES) {
            return false;
        }
        
        // 检查内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory;
        
        if (memoryUsage > 0.8) { // 内存使用超过80%时停止预加载
            Log.d(TAG, String.format("Skipping preload due to high memory usage: %.1f%%", 
                memoryUsage * 100));
            return false;
        }
        
        return true;
    }
    
    /**
     * 启用预加载
     */
    public void enablePreload() {
        mEnabled.set(true);
        Log.d(TAG, "Cache preloading enabled");
    }
    
    /**
     * 禁用预加载
     */
    public void disablePreload() {
        mEnabled.set(false);
        clearAllSessions();
        Log.d(TAG, "Cache preloading disabled");
    }
    
    /**
     * 清理所有预加载会话
     */
    private void clearAllSessions() {
        for (PreloadSession session : mActiveSessions.values()) {
            session.cancel();
        }
        mActiveSessions.clear();
    }
    
    /**
     * 获取预加载统计信息
     */
    public String getPreloadStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== 智能缓存预加载状态 ===\n");
        stats.append("预加载启用: ").append(mEnabled.get()).append("\n");
        stats.append("活跃会话: ").append(mActiveSessions.size()).append("\n");
        stats.append("最大画廊数: ").append(MAX_PRELOAD_GALLERIES).append("\n");
        stats.append("每画廊预加载: ").append(PRELOAD_IMAGES_PER_GALLERY).append("张\n");
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory;
        stats.append("内存使用: ").append(String.format("%.1f%%", memoryUsage * 100)).append("\n");
        
        return stats.toString();
    }
    
    /**
     * 销毁预加载器
     */
    public void destroy() {
        disablePreload();
        mPreloadExecutor.shutdown();
        Log.d(TAG, "SmartCachePreloader destroyed");
    }
    
    /**
     * 预加载会话
     */
    private static class PreloadSession {
        final GalleryInfo galleryInfo;
        final int priority;
        final AtomicBoolean cancelled;
        final long startTime;
        
        PreloadSession(GalleryInfo galleryInfo, int priority) {
            this.galleryInfo = galleryInfo;
            this.priority = priority;
            this.cancelled = new AtomicBoolean(false);
            this.startTime = System.currentTimeMillis();
        }
        
        boolean isCancelled() {
            return cancelled.get();
        }
        
        void cancel() {
            cancelled.set(true);
        }
        
        long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
    }
}