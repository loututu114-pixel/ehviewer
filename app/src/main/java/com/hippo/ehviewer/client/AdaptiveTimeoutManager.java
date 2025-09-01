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
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自适应超时管理器
 * 根据域名的历史连接表现动态调整超时时间
 */
public class AdaptiveTimeoutManager {
    
    private static final String TAG = "AdaptiveTimeoutManager";
    
    // 默认超时配置
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000; // 10秒
    private static final int DEFAULT_READ_TIMEOUT = 15000; // 15秒
    private static final int MIN_CONNECT_TIMEOUT = 5000; // 5秒
    private static final int MAX_CONNECT_TIMEOUT = 30000; // 30秒
    private static final int MIN_READ_TIMEOUT = 10000; // 10秒
    private static final int MAX_READ_TIMEOUT = 45000; // 45秒
    
    // 超时调整参数
    private static final int TIMEOUT_INCREASE_STEP = 2000; // 每次增加2秒
    private static final int TIMEOUT_DECREASE_STEP = 1000; // 每次减少1秒
    private static final int MAX_CONSECUTIVE_FAILURES = 3; // 最大连续失败次数
    private static final long TIMEOUT_CONFIG_EXPIRY = 3600000; // 1小时后过期
    
    private static final Map<String, TimeoutConfig> domainTimeouts = new ConcurrentHashMap<>();
    
    /**
     * 超时配置类
     */
    public static class TimeoutConfig {
        int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        int readTimeout = DEFAULT_READ_TIMEOUT;
        long lastSuccessTime = 0;
        long lastFailureTime = 0;
        int consecutiveFailures = 0;
        int consecutiveSuccesses = 0;
        long creationTime = System.currentTimeMillis();
        long totalResponseTime = 0;
        int totalRequests = 0;
        
        /**
         * 记录连接失败，增加超时时间
         */
        public void recordFailure() {
            consecutiveFailures++;
            consecutiveSuccesses = 0;
            lastFailureTime = System.currentTimeMillis();
            
            // 根据连续失败次数增加超时时间
            if (consecutiveFailures <= MAX_CONSECUTIVE_FAILURES) {
                connectTimeout = Math.min(connectTimeout + TIMEOUT_INCREASE_STEP, MAX_CONNECT_TIMEOUT);
                readTimeout = Math.min(readTimeout + TIMEOUT_INCREASE_STEP, MAX_READ_TIMEOUT);
            }
            
            Log.d(TAG, "Recorded failure, adjusted timeouts - connect: " + connectTimeout + "ms, read: " + readTimeout + "ms");
        }
        
        /**
         * 记录连接成功，可能减少超时时间
         */
        public void recordSuccess(long responseTime) {
            consecutiveSuccesses++;
            consecutiveFailures = 0;
            lastSuccessTime = System.currentTimeMillis();
            totalResponseTime += responseTime;
            totalRequests++;
            
            // 连续成功多次后，逐渐减少超时时间
            if (consecutiveSuccesses >= 3) {
                // 基于平均响应时间调整
                long avgResponseTime = totalResponseTime / totalRequests;
                
                // 保守地减少超时时间（仅当当前超时远大于平均响应时间时）
                if (connectTimeout > avgResponseTime * 3) {
                    connectTimeout = Math.max(connectTimeout - TIMEOUT_DECREASE_STEP, MIN_CONNECT_TIMEOUT);
                }
                
                if (readTimeout > avgResponseTime * 4) {
                    readTimeout = Math.max(readTimeout - TIMEOUT_DECREASE_STEP, MIN_READ_TIMEOUT);
                }
            }
            
            Log.d(TAG, "Recorded success (response: " + responseTime + "ms), adjusted timeouts - connect: " + connectTimeout + "ms, read: " + readTimeout + "ms");
        }
        
        /**
         * 检查配置是否过期
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - creationTime > TIMEOUT_CONFIG_EXPIRY;
        }
        
        /**
         * 获取平均响应时间
         */
        public long getAverageResponseTime() {
            return totalRequests > 0 ? totalResponseTime / totalRequests : 0;
        }
        
        /**
         * 获取成功率
         */
        public double getSuccessRate() {
            int totalAttempts = consecutiveFailures + consecutiveSuccesses;
            return totalAttempts > 0 ? (double) consecutiveSuccesses / totalAttempts : 0.0;
        }
        
        /**
         * 重置统计信息
         */
        public void reset() {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            readTimeout = DEFAULT_READ_TIMEOUT;
            consecutiveFailures = 0;
            consecutiveSuccesses = 0;
            totalResponseTime = 0;
            totalRequests = 0;
            creationTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 获取域名的超时配置
     */
    public static TimeoutConfig getTimeoutConfig(String url) {
        String domain = extractDomain(url);
        if (domain == null || domain.isEmpty()) {
            return getDefaultTimeoutConfig();
        }
        
        TimeoutConfig config = domainTimeouts.get(domain);
        
        // 检查配置是否过期
        if (config != null && config.isExpired()) {
            domainTimeouts.remove(domain);
            config = null;
        }
        
        // 创建新配置
        if (config == null) {
            config = new TimeoutConfig();
            domainTimeouts.put(domain, config);
            Log.d(TAG, "Created new timeout config for domain: " + domain);
        }
        
        return config;
    }
    
    /**
     * 记录连接成功
     */
    public static void recordConnectionSuccess(String url, long responseTime) {
        String domain = extractDomain(url);
        if (domain != null && !domain.isEmpty()) {
            TimeoutConfig config = getTimeoutConfig(url);
            config.recordSuccess(responseTime);
        }
    }
    
    /**
     * 记录连接失败
     */
    public static void recordConnectionFailure(String url) {
        String domain = extractDomain(url);
        if (domain != null && !domain.isEmpty()) {
            TimeoutConfig config = getTimeoutConfig(url);
            config.recordFailure();
        }
    }
    
    /**
     * 获取默认超时配置
     */
    public static TimeoutConfig getDefaultTimeoutConfig() {
        return new TimeoutConfig();
    }
    
    /**
     * 从URL中提取域名
     */
    private static String extractDomain(String urlString) {
        try {
            if (urlString == null || urlString.isEmpty()) {
                return null;
            }
            
            // 如果不包含协议，添加http://
            if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                urlString = "http://" + urlString;
            }
            
            URL url = new URL(urlString);
            String host = url.getHost();
            
            if (host != null) {
                // 移除www前缀以统一域名
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                return host.toLowerCase();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract domain from URL: " + urlString, e);
        }
        
        return null;
    }
    
    /**
     * 清理过期的超时配置
     */
    public static void cleanupExpiredConfigs() {
        int removedCount = 0;
        for (Map.Entry<String, TimeoutConfig> entry : domainTimeouts.entrySet()) {
            if (entry.getValue().isExpired()) {
                domainTimeouts.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            Log.d(TAG, "Cleaned up " + removedCount + " expired timeout configs");
        }
    }
    
    /**
     * 获取所有域名的统计信息
     */
    public static String getStatistics() {
        int totalDomains = domainTimeouts.size();
        int expiredDomains = 0;
        long totalAvgResponseTime = 0;
        int domainsWithStats = 0;
        
        for (TimeoutConfig config : domainTimeouts.values()) {
            if (config.isExpired()) {
                expiredDomains++;
            } else if (config.totalRequests > 0) {
                totalAvgResponseTime += config.getAverageResponseTime();
                domainsWithStats++;
            }
        }
        
        long globalAvgResponseTime = domainsWithStats > 0 ? totalAvgResponseTime / domainsWithStats : 0;
        
        return String.format("Timeout Stats - Domains: %d, Expired: %d, Avg Response: %dms", 
                           totalDomains, expiredDomains, globalAvgResponseTime);
    }
    
    /**
     * 重置所有超时配置
     */
    public static void resetAllConfigs() {
        int size = domainTimeouts.size();
        domainTimeouts.clear();
        Log.d(TAG, "Reset all timeout configs (" + size + " entries)");
    }
    
    /**
     * 获取特定域名的统计信息
     */
    public static String getDomainStatistics(String url) {
        String domain = extractDomain(url);
        if (domain == null || domain.isEmpty()) {
            return "No domain statistics available";
        }
        
        TimeoutConfig config = domainTimeouts.get(domain);
        if (config == null) {
            return "No statistics for domain: " + domain;
        }
        
        return String.format("Domain: %s - Connect: %dms, Read: %dms, Avg Response: %dms, Success Rate: %.1f%%, Consecutive: %d success / %d failures",
                           domain, config.connectTimeout, config.readTimeout, 
                           config.getAverageResponseTime(), config.getSuccessRate() * 100,
                           config.consecutiveSuccesses, config.consecutiveFailures);
    }
    
    /**
     * 为特定域名预设超时配置
     */
    public static void presetTimeoutConfig(String domain, int connectTimeout, int readTimeout) {
        if (domain == null || domain.isEmpty()) {
            return;
        }
        
        domain = domain.toLowerCase();
        if (domain.startsWith("www.")) {
            domain = domain.substring(4);
        }
        
        TimeoutConfig config = new TimeoutConfig();
        config.connectTimeout = Math.max(MIN_CONNECT_TIMEOUT, Math.min(connectTimeout, MAX_CONNECT_TIMEOUT));
        config.readTimeout = Math.max(MIN_READ_TIMEOUT, Math.min(readTimeout, MAX_READ_TIMEOUT));
        
        domainTimeouts.put(domain, config);
        Log.d(TAG, "Preset timeout config for " + domain + " - connect: " + config.connectTimeout + "ms, read: " + config.readTimeout + "ms");
    }
}