/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.client;

import android.util.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接重试管理器，实现指数退避算法
 */
public class ConnectionRetryManager {
    
    private static final String TAG = "ConnectionRetryManager";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int BASE_RETRY_DELAY = 1000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final int MAX_RETRY_DELAY = 16000; // 16 seconds
    private static final long RETRY_INFO_EXPIRY = 300000; // 5 minutes
    
    private final Map<String, RetryInfo> retryMap = new ConcurrentHashMap<>();
    
    private static class RetryInfo {
        int attemptCount = 0;
        long lastRetryTime = 0;
        long creationTime = System.currentTimeMillis();
        
        long getNextRetryDelay() {
            long delay = (long) (BASE_RETRY_DELAY * Math.pow(BACKOFF_MULTIPLIER, attemptCount));
            // Add jitter to prevent thundering herd (0-1000ms random delay)
            delay += (long) (Math.random() * 1000);
            return Math.min(delay, MAX_RETRY_DELAY);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - creationTime > RETRY_INFO_EXPIRY;
        }
    }
    
    /**
     * 检查是否应该重试连接
     */
    public boolean shouldRetry(String url, String errorType) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String key = generateKey(url);
        RetryInfo info = retryMap.get(key);

        // 清理过期的重试信息
        if (info != null && info.isExpired()) {
            retryMap.remove(key);
            info = null;
        }

        // 创建新的重试信息
        if (info == null) {
            info = new RetryInfo();
            retryMap.put(key, info);
        }

        // 检查是否已达到最大重试次数
        if (info.attemptCount >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Max retry attempts reached for URL: " + url);
            retryMap.remove(key);
            return false;
        }

        // 检查是否需要等待更长时间
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRetry = currentTime - info.lastRetryTime;
        long requiredDelay = getAdjustedRetryDelay(info, errorType);

        if (info.lastRetryTime > 0 && timeSinceLastRetry < requiredDelay) {
            Log.d(TAG, "Need to wait " + (requiredDelay - timeSinceLastRetry) + "ms before retry for: " + url);
            return false;
        }

        // 更新重试信息
        info.attemptCount++;
        info.lastRetryTime = currentTime;

        Log.d(TAG, "Allowing retry attempt " + info.attemptCount + " for URL: " + url + " (error: " + errorType + ")");
        return true;
    }

    /**
     * 获取调整后的重试延迟时间（针对不同错误类型优化）
     */
    private long getAdjustedRetryDelay(RetryInfo info, String errorType) {
        long baseDelay = info.getNextRetryDelay();

        // 对连接关闭错误使用更长的延迟
        if (errorType != null && errorType.contains("ERR_CONNECTION_CLOSED")) {
            // 连接关闭错误通常需要更长时间等待服务器恢复
            baseDelay = (long) (baseDelay * 1.5); // 增加50%的延迟
            if (info.attemptCount > 1) {
                // 多次重试后，进一步增加延迟
                baseDelay = (long) (baseDelay * 1.2);
            }
            Log.d(TAG, "Using extended delay for ERR_CONNECTION_CLOSED: " + baseDelay + "ms");
        }

        return Math.min(baseDelay, MAX_RETRY_DELAY);
    }
    
    /**
     * 获取重试延迟时间
     */
    public long getRetryDelay(String url) {
        if (url == null || url.isEmpty()) {
            return BASE_RETRY_DELAY;
        }
        
        String key = generateKey(url);
        RetryInfo info = retryMap.get(key);
        return info != null ? info.getNextRetryDelay() : BASE_RETRY_DELAY;
    }
    
    /**
     * 清除URL的重试信息（成功连接后调用）
     */
    public void clearRetryInfo(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        
        String key = generateKey(url);
        RetryInfo removed = retryMap.remove(key);
        if (removed != null) {
            Log.d(TAG, "Cleared retry info for successful connection: " + url);
        }
    }
    
    /**
     * 获取当前重试次数
     */
    public int getRetryCount(String url) {
        if (url == null || url.isEmpty()) {
            return 0;
        }
        
        String key = generateKey(url);
        RetryInfo info = retryMap.get(key);
        return info != null ? info.attemptCount : 0;
    }
    
    /**
     * 清除所有重试信息
     */
    public void clearAllRetryInfo() {
        int size = retryMap.size();
        retryMap.clear();
        Log.d(TAG, "Cleared all retry info (" + size + " entries)");
    }
    
    /**
     * 清理过期的重试信息
     */
    public void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        int removed = 0;
        
        for (Map.Entry<String, RetryInfo> entry : retryMap.entrySet()) {
            if (entry.getValue().isExpired()) {
                retryMap.remove(entry.getKey());
                removed++;
            }
        }
        
        if (removed > 0) {
            Log.d(TAG, "Cleaned up " + removed + " expired retry entries");
        }
    }
    
    /**
     * 生成URL的唯一键（移除查询参数和片段）
     */
    private String generateKey(String url) {
        try {
            // 移除查询参数和片段，只保留主要URL
            String cleanUrl = url.replaceAll("[?#].*$", "");
            return cleanUrl.toLowerCase();
        } catch (Exception e) {
            Log.w(TAG, "Error generating key for URL: " + url, e);
            return url;
        }
    }
    
    /**
     * 获取重试统计信息
     */
    public String getRetryStatistics() {
        int totalEntries = retryMap.size();
        int expiredEntries = 0;
        int maxRetryAttempts = 0;
        
        for (RetryInfo info : retryMap.values()) {
            if (info.isExpired()) {
                expiredEntries++;
            }
            maxRetryAttempts = Math.max(maxRetryAttempts, info.attemptCount);
        }
        
        return String.format("Retry Stats - Total: %d, Expired: %d, Max Attempts: %d", 
                           totalEntries, expiredEntries, maxRetryAttempts);
    }
}