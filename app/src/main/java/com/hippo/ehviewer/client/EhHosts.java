/*
 * Copyright 2018 Hippo Seven
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

/*
 * Created by Hippo on 2018/3/23.
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.Hosts;
import com.hippo.ehviewer.Settings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;


public class EhHosts implements Dns {

    private static final String TAG = "EhHosts";
    private static final Map<String, List<InetAddress>> builtInHosts;

    static {
        Map<String, List<InetAddress>> map = new HashMap<>();
        if (Settings.getBuiltInHosts()) {
            put(map, "e-hentai.org",
                    "104.20.18.168",
                    "104.20.19.168",
                    "172.67.2.238",
                    "172.66.132.196",
                    "172.66.140.62"
//                    "194.126.173.115"
            );
            put(map, "repo.e-hentai.org",   "104.20.18.168",
                    "104.20.19.168",
                    "172.67.2.238");
            put(map, "forums.e-hentai.org",   "104.20.18.168",
                    "104.20.19.168",
                    "172.67.2.238");
            put(map, "upld.e-hentai.org", "89.149.221.236", "95.211.208.236");
            put(map, "ehgt.org",
                    "109.236.85.28",
                    "62.112.8.21",
                    "89.39.106.43",
                    "2a00:7c80:0:123::3a85",
                    "2a00:7c80:0:12d::38a1",
                    "2a00:7c80:0:13b::37a4");
//            put(map, "ehgt.org",
//                    "109.236.85.28",
//                    "62.112.8.21",
//                    "89.39.106.43");
            put(map, "raw.githubusercontent.com", "151.101.0.133", "151.101.64.133", "151.101.128.133", "151.101.192.133");

        }

        if (Settings.getBuiltEXHosts()) {
            put(map, "exhentai.org",
                    "178.175.128.251",
                    "178.175.128.252",
                    "178.175.128.253",
                    "178.175.128.254",
                    "178.175.129.251",
                    "178.175.129.252",
                    "178.175.129.253",
                    "178.175.129.254",
                    "178.175.132.19",
                    "178.175.132.20",
                    "178.175.132.21",
                    "178.175.132.22"
            );
            put(map, "upld.exhentai.org", "178.175.132.22", "178.175.129.254", "178.175.128.254");
            put(map, "s.exhentai.org",
//                    "178.175.129.251",
//                    "178.175.129.252",
                    "178.175.129.253",
                    "178.175.129.254",
//                    "178.175.128.251",
//                    "178.175.128.252",
                    "178.175.128.253",
                    "178.175.128.254",
//                    "178.175.132.19",
//                    "178.175.132.20",
                    "178.175.132.21",
                    "178.175.132.22"
            );
        }

        builtInHosts = map;
    }

    private final Hosts hosts;
    private final Context context;
    private static DnsOverHttps dnsOverHttps;

    // 增强DNS解析器配置
    private static final int DNS_TIMEOUT_MS = 8000; // 8秒超时
    private static final int DNS_RETRY_COUNT = 3;   // 重试3次
    private static final int DNS_RETRY_DELAY_MS = 1000; // 1秒基础延迟
    private static final ExecutorService dnsExecutor = Executors.newCachedThreadPool();

    // DNS健康监控
    private static final Map<String, DnsHealthStatus> dnsHealthMap = new ConcurrentHashMap<>();
    private static final long HEALTH_CHECK_INTERVAL_MS = 5 * 60 * 1000; // 5分钟健康检查间隔

    // DNS缓存 - 提升解析速度
    private static final Map<String, DnsCacheEntry> dnsCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cacheCleaner = Executors.newSingleThreadScheduledExecutor();
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5分钟缓存

    static {
        // 启动缓存清理任务
        cacheCleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            dnsCache.entrySet().removeIf(entry -> now - entry.getValue().timestamp > CACHE_DURATION_MS);
        }, CACHE_DURATION_MS, CACHE_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * DNS健康状态
     */
    private static class DnsHealthStatus {
        long lastCheckTime = 0;
        int consecutiveFailures = 0;
        int totalAttempts = 0;
        int successfulAttempts = 0;
        long averageResponseTime = 0;

        boolean isHealthy() {
            if (consecutiveFailures >= 3) return false;
            if (totalAttempts > 0) {
                double successRate = (double) successfulAttempts / totalAttempts;
                return successRate >= 0.7; // 70%成功率
            }
            return true;
        }

        void recordSuccess(long responseTime) {
            lastCheckTime = System.currentTimeMillis();
            consecutiveFailures = 0;
            totalAttempts++;
            successfulAttempts++;
            // 更新平均响应时间
            if (averageResponseTime == 0) {
                averageResponseTime = responseTime;
            } else {
                averageResponseTime = (averageResponseTime + responseTime) / 2;
            }
        }

        void recordFailure() {
            lastCheckTime = System.currentTimeMillis();
            consecutiveFailures++;
            totalAttempts++;
        }
    }

    /**
     * DNS缓存条目
     */
    private static class DnsCacheEntry {
        final List<InetAddress> addresses;
        final long timestamp;

        DnsCacheEntry(List<InetAddress> addresses) {
            this.addresses = addresses;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    public EhHosts(Context context) {
        this.context = context.getApplicationContext();
        hosts = EhApplication.getHosts(context);
        DnsOverHttps.Builder builder = new DnsOverHttps.Builder()
                .client(new OkHttpClient.Builder().cache(EhApplication.getOkHttpCache(context)).build())
                .url(HttpUrl.get("https://77.88.8.1/dns-query"));
        dnsOverHttps = builder.post(true).build();
    }

    private static void put(Map<String, List<InetAddress>> map, String host, String... ips) {
        List<InetAddress> addresses = new ArrayList<>();
        for (String ip : ips) {
            addresses.add(Hosts.toInetAddress(host, ip));
        }
        map.put(host, addresses);
    }


    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        // 首先检查DNS缓存
        DnsCacheEntry cacheEntry = dnsCache.get(hostname);
        if (cacheEntry != null && !cacheEntry.isExpired()) {
            List<InetAddress> cachedAddresses = new ArrayList<>(cacheEntry.addresses);
            Collections.shuffle(cachedAddresses, new Random(System.currentTimeMillis()));
            return cachedAddresses;
        }

        List<InetAddress> inetAddresses = null;

        // 1. 检查用户自定义hosts
        inetAddresses = hosts.getList(hostname);
        if (inetAddresses != null && !inetAddresses.isEmpty()) {
            inetAddresses = new ArrayList<>(inetAddresses);
            Collections.shuffle(inetAddresses, new Random(System.currentTimeMillis()));
            dnsCache.put(hostname, new DnsCacheEntry(inetAddresses));
            return inetAddresses;
        }

        // 2. 检查内置hosts
        if (Settings.getBuiltInHosts() || Settings.getBuiltEXHosts()) {
            inetAddresses = builtInHosts.get(hostname);
            if (inetAddresses != null) {
                inetAddresses = new ArrayList<>(inetAddresses);
                Collections.shuffle(inetAddresses, new Random(System.currentTimeMillis()));
                dnsCache.put(hostname, new DnsCacheEntry(inetAddresses));
                return inetAddresses;
            }
        }

        // 3. DNS over HTTPS
        if (Settings.getDoH()) {
            try {
                inetAddresses = dnsOverHttps.lookup(hostname);
                if (!inetAddresses.isEmpty()) {
                    inetAddresses = new ArrayList<>(inetAddresses);
                    Collections.shuffle(inetAddresses, new Random(System.currentTimeMillis()));
                    dnsCache.put(hostname, new DnsCacheEntry(inetAddresses));
                    return inetAddresses;
                }
            } catch (Exception e) {
                // DoH失败，继续使用系统DNS
            }
        }

        // 4. 增强系统DNS解析（带超时和重试）
        inetAddresses = performRobustDnsLookup(hostname);
        if (inetAddresses != null && !inetAddresses.isEmpty()) {
            inetAddresses = new ArrayList<>(inetAddresses);
            Collections.shuffle(inetAddresses, new Random(System.currentTimeMillis()));
            dnsCache.put(hostname, new DnsCacheEntry(inetAddresses));
            return inetAddresses;
        }

        // 如果所有方法都失败
        throw new UnknownHostException("Unable to resolve host: " + hostname);
    }

    /**
     * 检测网络状态
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to check network availability", e);
        }
        return false;
    }

    /**
     * 获取网络类型
     */
    private String getNetworkType() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    return activeNetwork.getTypeName();
                }
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to get network type", e);
        }
        return "UNKNOWN";
    }

    /**
     * 执行健壮的DNS解析（带超时和重试机制）
     */
    private List<InetAddress> performRobustDnsLookup(String hostname) {
        android.util.Log.d(TAG, "Performing robust DNS lookup for: " + hostname);

        // 网络状态感知：调整策略
        boolean networkAvailable = isNetworkAvailable();
        String networkType = getNetworkType();

        if (!networkAvailable) {
            android.util.Log.w(TAG, "No network available for DNS lookup of " + hostname);
            return null;
        }

        android.util.Log.d(TAG, "Network type: " + networkType + " for DNS lookup of " + hostname);

        // 根据网络类型调整超时时间
        int adjustedTimeout = DNS_TIMEOUT_MS;
        int adjustedRetries = DNS_RETRY_COUNT;

        if ("WIFI".equals(networkType)) {
            // WiFi网络，通常更快
            adjustedTimeout = 6000; // 6秒
            adjustedRetries = 2;    // 减少重试次数
        } else if ("MOBILE".equals(networkType)) {
            // 移动网络，可能较慢
            adjustedTimeout = 10000; // 10秒
            adjustedRetries = 4;     // 增加重试次数
        }

        for (int attempt = 1; attempt <= adjustedRetries; attempt++) {
            try {
                android.util.Log.d(TAG, "DNS lookup attempt " + attempt + "/" + adjustedRetries + " for: " + hostname);

                // 使用异步DNS解析避免阻塞
                Future<List<InetAddress>> future = dnsExecutor.submit(new DnsLookupTask(hostname));

                // 等待结果，带超时
                List<InetAddress> result = future.get(adjustedTimeout, TimeUnit.MILLISECONDS);

                if (result != null && !result.isEmpty()) {
                    android.util.Log.d(TAG, "DNS lookup successful for " + hostname + ", got " + result.size() + " addresses");
                    return result;
                }

            } catch (TimeoutException e) {
                android.util.Log.w(TAG, "DNS lookup timeout for " + hostname + " (attempt " + attempt + ")", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof UnknownHostException) {
                    android.util.Log.w(TAG, "DNS lookup failed for " + hostname + " (attempt " + attempt + "): Unknown host", cause);
                } else {
                    android.util.Log.w(TAG, "DNS lookup failed for " + hostname + " (attempt " + attempt + ")", cause);
                }
            } catch (Exception e) {
                android.util.Log.w(TAG, "Unexpected error during DNS lookup for " + hostname + " (attempt " + attempt + ")", e);
            }

            // 如果不是最后一次尝试，等待后重试
            if (attempt < adjustedRetries) {
                try {
                    // 指数退避：第n次重试等待 n * DNS_RETRY_DELAY_MS
                    long delay = attempt * DNS_RETRY_DELAY_MS;
                    android.util.Log.d(TAG, "Waiting " + delay + "ms before retry for " + hostname);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    android.util.Log.w(TAG, "DNS retry interrupted for " + hostname);
                    break;
                }
            }
        }

        android.util.Log.w(TAG, "All DNS lookup attempts failed for " + hostname);

        // 降级策略：尝试备用DNS服务器
        List<InetAddress> fallbackResult = performFallbackDnsLookup(hostname);
        if (fallbackResult != null && !fallbackResult.isEmpty()) {
            android.util.Log.i(TAG, "Fallback DNS lookup successful for " + hostname);
            return fallbackResult;
        }

        return null;
    }

    /**
     * 执行降级DNS解析（使用备用DNS服务器）
     */
    private List<InetAddress> performFallbackDnsLookup(String hostname) {
        android.util.Log.d(TAG, "Performing fallback DNS lookup for: " + hostname);

        // 尝试使用内置的IP映射作为最后的降级方案
        List<InetAddress> builtInAddresses = builtInHosts.get(hostname);
        if (builtInAddresses != null && !builtInAddresses.isEmpty()) {
            android.util.Log.i(TAG, "Using built-in IP mapping for " + hostname);
            return new ArrayList<>(builtInAddresses);
        }

        // 对于特定的域名，尝试一些常见的备用解析
        if (hostname.contains("e-hentai.org") || hostname.contains("exhentai.org")) {
            android.util.Log.i(TAG, "Attempting emergency fallback for hentai domains: " + hostname);
            // 这里可以添加一些紧急的备用IP，但需要谨慎处理
            // 由于安全原因，这里不添加具体的IP地址
        }

        android.util.Log.w(TAG, "Fallback DNS lookup failed for " + hostname);
        return null;
    }

    /**
     * 异步DNS解析
     */
    public void lookupAsync(String hostname, Consumer<List<InetAddress>> onSuccess, Consumer<Exception> onError) {
        dnsExecutor.execute(() -> {
            try {
                List<InetAddress> result = lookup(hostname);
                if (result != null && !result.isEmpty()) {
                    onSuccess.accept(result);
                } else {
                    onError.accept(new UnknownHostException("DNS lookup returned empty result for " + hostname));
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    /**
     * DNS解析任务
     */
    private static class DnsLookupTask implements Callable<List<InetAddress>> {
        private final String hostname;

        DnsLookupTask(String hostname) {
            this.hostname = hostname;
        }

        @Override
        public List<InetAddress> call() throws Exception {
            try {
                return Arrays.asList(InetAddress.getAllByName(hostname));
            } catch (NullPointerException e) {
                UnknownHostException unknownHostException =
                        new UnknownHostException("Broken system behaviour for dns lookup of " + hostname);
                unknownHostException.initCause(e);
                throw unknownHostException;
            }
        }
    }
}
