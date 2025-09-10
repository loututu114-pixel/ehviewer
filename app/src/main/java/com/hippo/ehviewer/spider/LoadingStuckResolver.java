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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.EhGalleryProvider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 加载卡死问题解决器
 * 
 * 专门解决画廊点开后一直转圈加载不出来的问题
 * 提供超时检测、智能重试、降级处理等功能
 */
public class LoadingStuckResolver {
    
    private static final String TAG = "LoadingStuckResolver";
    
    // 超时配置
    private static final long SPIDER_INFO_TIMEOUT = 45000;    // SpiderInfo获取超时：45秒
    private static final long PTOKEN_TIMEOUT = 30000;        // pToken获取超时：30秒  
    private static final long GALLERY_LOAD_TIMEOUT = 90000;  // 画廊总体加载超时：90秒
    private static final long PAGE_LOAD_TIMEOUT = 60000;     // 单页加载超时：60秒
    
    // 重试配置
    private static final int MAX_SPIDER_INFO_RETRY = 3;      // SpiderInfo最大重试次数
    private static final int MAX_PTOKEN_RETRY = 5;           // pToken最大重试次数
    private static final int MAX_PAGE_RETRY = 3;             // 单页最大重试次数
    
    // 单例管理
    private static volatile LoadingStuckResolver sInstance;
    private static final Object sLock = new Object();
    
    private final Context mContext;
    private final Handler mMainHandler;
    private final ConcurrentHashMap<Long, LoadingSession> mActiveSessions;
    private final AtomicBoolean mEnabled;
    
    private LoadingStuckResolver(Context context) {
        mContext = context.getApplicationContext();
        mMainHandler = new Handler(Looper.getMainLooper());
        mActiveSessions = new ConcurrentHashMap<>();
        mEnabled = new AtomicBoolean(true);
        Log.d(TAG, "LoadingStuckResolver initialized");
    }
    
    public static LoadingStuckResolver getInstance(Context context) {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new LoadingStuckResolver(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 开始监控画廊加载，防止卡死
     */
    public void startMonitoring(GalleryInfo galleryInfo, EhGalleryProvider provider) {
        if (!mEnabled.get()) {
            return;
        }
        
        Long gid = galleryInfo.gid;
        LoadingSession session = new LoadingSession(galleryInfo, provider);
        mActiveSessions.put(gid, session);
        
        Log.d(TAG, String.format("Started monitoring gallery: %s (gid=%d)", galleryInfo.title, gid));
        
        // 启动总体超时检测
        mMainHandler.postDelayed(() -> {
            if (mActiveSessions.containsKey(gid)) {
                handleGalleryTimeout(session);
            }
        }, GALLERY_LOAD_TIMEOUT);
        
        // 启动SpiderInfo超时检测
        mMainHandler.postDelayed(() -> {
            if (mActiveSessions.containsKey(gid) && !session.spiderInfoLoaded.get()) {
                handleSpiderInfoTimeout(session);
            }
        }, SPIDER_INFO_TIMEOUT);
    }
    
    /**
     * 通知SpiderInfo已加载
     */
    public void notifySpiderInfoLoaded(long gid) {
        LoadingSession session = mActiveSessions.get(gid);
        if (session != null) {
            session.spiderInfoLoaded.set(true);
            Log.d(TAG, "SpiderInfo loaded for gid: " + gid);
        }
    }
    
    /**
     * 通知pToken获取开始
     */
    public void notifyPTokenRequest(long gid, int index) {
        LoadingSession session = mActiveSessions.get(gid);
        if (session != null) {
            session.addPTokenRequest(index);
            
            // 启动pToken超时检测
            mMainHandler.postDelayed(() -> {
                if (mActiveSessions.containsKey(gid)) {
                    handlePTokenTimeout(session, index);
                }
            }, PTOKEN_TIMEOUT);
        }
    }
    
    /**
     * 通知pToken获取完成
     */
    public void notifyPTokenReceived(long gid, int index) {
        LoadingSession session = mActiveSessions.get(gid);
        if (session != null) {
            session.completePTokenRequest(index);
            Log.d(TAG, String.format("pToken received for gid=%d, index=%d", gid, index));
        }
    }
    
    /**
     * 通知页面加载开始
     */
    public void notifyPageLoadStart(long gid, int index) {
        LoadingSession session = mActiveSessions.get(gid);
        if (session != null) {
            session.addPageLoad(index);
            
            // 启动页面超时检测
            mMainHandler.postDelayed(() -> {
                if (mActiveSessions.containsKey(gid)) {
                    handlePageTimeout(session, index);
                }
            }, PAGE_LOAD_TIMEOUT);
        }
    }
    
    /**
     * 通知页面加载完成
     */
    public void notifyPageLoadComplete(long gid, int index) {
        LoadingSession session = mActiveSessions.get(gid);
        if (session != null) {
            session.completePageLoad(index);
        }
    }
    
    /**
     * 处理画廊整体超时
     */
    private void handleGalleryTimeout(LoadingSession session) {
        Log.w(TAG, String.format("Gallery loading timeout: %s (gid=%d)", 
            session.galleryInfo.title, session.galleryInfo.gid));
        
        try {
            // 强制重新初始化SpiderQueen
            forceRestartSpiderQueen(session);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle gallery timeout", e);
            // 最后手段：通知用户刷新
            notifyUserToRefresh(session);
        }
    }
    
    /**
     * 处理SpiderInfo获取超时
     */
    private void handleSpiderInfoTimeout(LoadingSession session) {
        Log.w(TAG, String.format("SpiderInfo timeout for gid=%d, retry=%d", 
            session.galleryInfo.gid, session.spiderInfoRetryCount.get()));
        
        if (session.spiderInfoRetryCount.incrementAndGet() < MAX_SPIDER_INFO_RETRY) {
            try {
                // 重试获取SpiderInfo
                retrySpiderInfoLoad(session);
            } catch (Exception e) {
                Log.e(TAG, "SpiderInfo retry failed", e);
                forceRestartSpiderQueen(session);
            }
        } else {
            Log.e(TAG, "SpiderInfo max retries exceeded, forcing restart");
            forceRestartSpiderQueen(session);
        }
    }
    
    /**
     * 处理pToken获取超时
     */
    private void handlePTokenTimeout(LoadingSession session, int index) {
        String key = "ptoken_" + index;
        int retryCount = session.incrementRetryCount(key);
        
        Log.w(TAG, String.format("pToken timeout for gid=%d, index=%d, retry=%d", 
            session.galleryInfo.gid, index, retryCount));
        
        if (retryCount < MAX_PTOKEN_RETRY) {
            try {
                // 重试获取pToken
                retryPTokenLoad(session, index);
            } catch (Exception e) {
                Log.e(TAG, "pToken retry failed", e);
                markPageAsFailed(session, index, "pToken retry failed: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "pToken max retries exceeded for index: " + index);
            markPageAsFailed(session, index, "pToken获取失败，已达最大重试次数");
        }
    }
    
    /**
     * 处理页面加载超时
     */
    private void handlePageTimeout(LoadingSession session, int index) {
        String key = "page_" + index;
        int retryCount = session.incrementRetryCount(key);
        
        Log.w(TAG, String.format("Page timeout for gid=%d, index=%d, retry=%d", 
            session.galleryInfo.gid, index, retryCount));
        
        if (retryCount < MAX_PAGE_RETRY) {
            try {
                // 重试页面加载
                retryPageLoad(session, index);
            } catch (Exception e) {
                Log.e(TAG, "Page retry failed", e);
                markPageAsFailed(session, index, "页面重试失败: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Page max retries exceeded for index: " + index);
            markPageAsFailed(session, index, "页面加载失败，已达最大重试次数");
        }
    }
    
    /**
     * 强制重启SpiderQueen
     */
    private void forceRestartSpiderQueen(LoadingSession session) {
        Log.d(TAG, "Force restarting SpiderQueen for gid: " + session.galleryInfo.gid);
        
        try {
            // 停止当前的SpiderQueen
            if (session.provider != null) {
                session.provider.stop();
                
                // 延迟重启
                mMainHandler.postDelayed(() -> {
                    try {
                        session.provider.start();
                        Log.d(TAG, "SpiderQueen restarted successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to restart SpiderQueen", e);
                        notifyUserToRefresh(session);
                    }
                }, 2000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to force restart SpiderQueen", e);
            notifyUserToRefresh(session);
        }
    }
    
    /**
     * 重试SpiderInfo加载
     */
    private void retrySpiderInfoLoad(LoadingSession session) {
        Log.d(TAG, "Retrying SpiderInfo load for gid: " + session.galleryInfo.gid);
        
        // 通过强制刷新重新获取SpiderInfo
        if (session.provider != null) {
            // 这里可以通过反射或其他方式触发SpiderInfo重新获取
            // 暂时使用重启方式
            forceRestartSpiderQueen(session);
        }
    }
    
    /**
     * 重试pToken加载
     */
    private void retryPTokenLoad(LoadingSession session, int index) {
        Log.d(TAG, String.format("Retrying pToken load for gid=%d, index=%d", 
            session.galleryInfo.gid, index));
        
        // 通过重启provider来重试pToken获取
        if (session.provider != null) {
            try {
                Log.d(TAG, "Triggering provider restart for pToken retry");
                // 通过重新请求来触发pToken重新获取
                session.provider.request(index);
            } catch (Exception e) {
                Log.e(TAG, "pToken retry failed", e);
                markPageAsFailed(session, index, "pToken重试失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 重试页面加载
     */
    private void retryPageLoad(LoadingSession session, int index) {
        Log.d(TAG, String.format("Retrying page load for gid=%d, index=%d", 
            session.galleryInfo.gid, index));
        
        if (session.provider != null) {
            try {
                // 取消当前请求并重新开始
                session.provider.cancelRequest(index);
                
                // 短暂延迟后重新请求
                mMainHandler.postDelayed(() -> {
                    try {
                        session.provider.request(index);
                        Log.d(TAG, "Page retry request triggered");
                    } catch (Exception e) {
                        markPageAsFailed(session, index, "重试请求失败: " + e.getMessage());
                    }
                }, 1000);
                
            } catch (Exception e) {
                Log.e(TAG, "Page retry failed", e);
                markPageAsFailed(session, index, "页面重试异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 标记页面为失败状态
     */
    private void markPageAsFailed(LoadingSession session, int index, String error) {
        Log.d(TAG, String.format("Marking page as failed: gid=%d, index=%d, error=%s", 
            session.galleryInfo.gid, index, error));
        
        try {
            if (session.provider != null) {
                // 通知页面加载失败
                session.provider.notifyPageFailed(index, error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark page as failed", e);
        }
    }
    
    /**
     * 通知用户刷新
     */
    private void notifyUserToRefresh(LoadingSession session) {
        Log.d(TAG, "Notifying user to refresh gallery: " + session.galleryInfo.gid);
        
        // 这里可以通过EventBus或其他机制通知UI显示刷新按钮
        // 暂时记录日志，具体UI通知需要根据项目架构实现
        try {
            if (session.provider != null) {
                session.provider.notifyPageFailed(0, "加载超时，请尝试刷新页面");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to notify user", e);
        }
    }
    
    /**
     * 停止监控指定画廊
     */
    public void stopMonitoring(long gid) {
        LoadingSession session = mActiveSessions.remove(gid);
        if (session != null) {
            Log.d(TAG, "Stopped monitoring gallery: " + gid);
        }
    }
    
    /**
     * 获取当前监控状态
     */
    public String getMonitoringStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== 加载卡死监控状态 ===\n");
        status.append("监控启用: ").append(mEnabled.get()).append("\n");
        status.append("活跃会话: ").append(mActiveSessions.size()).append("\n");
        status.append("超时配置:\n");
        status.append("- SpiderInfo: ").append(SPIDER_INFO_TIMEOUT / 1000).append("秒\n");
        status.append("- pToken: ").append(PTOKEN_TIMEOUT / 1000).append("秒\n");
        status.append("- 页面加载: ").append(PAGE_LOAD_TIMEOUT / 1000).append("秒\n");
        status.append("- 画廊总体: ").append(GALLERY_LOAD_TIMEOUT / 1000).append("秒\n");
        return status.toString();
    }
    
    /**
     * 启用/禁用监控
     */
    public void setEnabled(boolean enabled) {
        mEnabled.set(enabled);
        if (!enabled) {
            mActiveSessions.clear();
        }
        Log.d(TAG, "LoadingStuckResolver " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * 加载会话类
     */
    private static class LoadingSession {
        final GalleryInfo galleryInfo;
        final EhGalleryProvider provider;
        final AtomicBoolean spiderInfoLoaded;
        final AtomicLong startTime;
        final ConcurrentHashMap<String, AtomicLong> activePTokenRequests;
        final ConcurrentHashMap<String, AtomicLong> activePageLoads;
        final ConcurrentHashMap<String, Integer> retryCountMap;
        final AtomicLong spiderInfoRetryCount;
        
        LoadingSession(GalleryInfo galleryInfo, EhGalleryProvider provider) {
            this.galleryInfo = galleryInfo;
            this.provider = provider;
            this.spiderInfoLoaded = new AtomicBoolean(false);
            this.startTime = new AtomicLong(System.currentTimeMillis());
            this.activePTokenRequests = new ConcurrentHashMap<>();
            this.activePageLoads = new ConcurrentHashMap<>();
            this.retryCountMap = new ConcurrentHashMap<>();
            this.spiderInfoRetryCount = new AtomicLong(0);
        }
        
        void addPTokenRequest(int index) {
            activePTokenRequests.put(String.valueOf(index), new AtomicLong(System.currentTimeMillis()));
        }
        
        void completePTokenRequest(int index) {
            activePTokenRequests.remove(String.valueOf(index));
        }
        
        void addPageLoad(int index) {
            activePageLoads.put(String.valueOf(index), new AtomicLong(System.currentTimeMillis()));
        }
        
        void completePageLoad(int index) {
            activePageLoads.remove(String.valueOf(index));
        }
        
        int incrementRetryCount(String key) {
            return retryCountMap.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
        }
    }
}