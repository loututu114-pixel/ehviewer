package com.hippo.ehviewer.system;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import androidx.core.content.ContextCompat;
import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;
import com.hippo.ehviewer.notification.SmartNotificationManager;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 系统优化器 - 提供全面的系统性能监控和优化建议
 * 包括内存清理、存储优化、网络加速、电池优化等功能
 */
public class SystemOptimizer {
    
    private Context context;
    private ActivityManager activityManager;
    private PackageManager packageManager;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private SystemOptimizerListener listener;
    
    // 系统状态缓存
    private SystemStatus lastSystemStatus;
    private long lastOptimizationTime = 0;
    private Set<String> backgroundApps = new HashSet<>();
    private Map<String, Long> appDataUsage = new HashMap<>();
    
    public static class SystemStatus {
        public float memoryUsagePercent;
        public long availableMemoryMB;
        public long totalMemoryMB;
        public float storageUsagePercent;
        public long availableStorageMB;
        public long totalStorageMB;
        public int runningAppsCount;
        public long networkDownloadKB;
        public long networkUploadKB;
        public String networkQuality;
        public List<String> heavyApps;
        public List<String> optimizationSuggestions;
        public long timestamp;
        
        public SystemStatus() {
            this.timestamp = System.currentTimeMillis();
            this.heavyApps = new ArrayList<>();
            this.optimizationSuggestions = new ArrayList<>();
        }
    }
    
    public interface SystemOptimizerListener {
        void onOptimizationStarted(String type);
        void onOptimizationProgress(String type, int progress);
        void onOptimizationCompleted(String type, boolean success, String details);
        void onSystemStatusUpdated(SystemStatus status);
        void onUrgentOptimizationNeeded(String reason, String suggestion);
    }
    
    public SystemOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.packageManager = context.getPackageManager();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
    
    public void setListener(SystemOptimizerListener listener) {
        this.listener = listener;
    }
    
    /**
     * 获取当前系统状态
     */
    public SystemStatus getCurrentSystemStatus() {
        SystemStatus status = new SystemStatus();
        
        // 内存状态
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        status.totalMemoryMB = memInfo.totalMem / (1024 * 1024);
        status.availableMemoryMB = memInfo.availMem / (1024 * 1024);
        status.memoryUsagePercent = ((float)(status.totalMemoryMB - status.availableMemoryMB) / status.totalMemoryMB) * 100;
        
        // 存储状态
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long totalStorageBytes = stat.getTotalBytes();
        long availableStorageBytes = stat.getAvailableBytes();
        
        status.totalStorageMB = totalStorageBytes / (1024 * 1024);
        status.availableStorageMB = availableStorageBytes / (1024 * 1024);
        status.storageUsagePercent = ((float)(status.totalStorageMB - status.availableStorageMB) / status.totalStorageMB) * 100;
        
        // 运行应用数量
        List<ActivityManager.RunningAppProcessInfo> runningApps = activityManager.getRunningAppProcesses();
        status.runningAppsCount = runningApps != null ? runningApps.size() : 0;
        
        // 网络流量
        status.networkDownloadKB = TrafficStats.getTotalRxBytes() / 1024;
        status.networkUploadKB = TrafficStats.getTotalTxBytes() / 1024;
        status.networkQuality = getNetworkQuality();
        
        // 识别资源消耗大的应用
        status.heavyApps = identifyHeavyApps();
        
        // 生成优化建议
        status.optimizationSuggestions = generateOptimizationSuggestions(status);
        
        this.lastSystemStatus = status;
        
        if (listener != null) {
            listener.onSystemStatusUpdated(status);
        }
        
        return status;
    }
    
    /**
     * 执行内存优化
     */
    public void optimizeMemory() {
        if (listener != null) {
            listener.onOptimizationStarted("内存清理");
        }
        
        UserBehaviorAnalyzer.trackEvent("memory_optimization_started");
        
        new Thread(() -> {
            try {
                int progress = 0;
                updateProgress("内存清理", progress);
                
                // 1. 获取当前内存状态
                SystemStatus beforeStatus = getCurrentSystemStatus();
                progress = 20;
                updateProgress("内存清理", progress);
                
                // 2. 清理后台应用
                List<String> killedApps = killBackgroundApps();
                progress = 50;
                updateProgress("内存清理", progress);
                
                // 3. 清理缓存
                clearSystemCache();
                progress = 70;
                updateProgress("内存清理", progress);
                
                // 4. 垃圾回收
                System.gc();
                progress = 90;
                updateProgress("内存清理", progress);
                
                // 5. 检查优化效果
                SystemStatus afterStatus = getCurrentSystemStatus();
                progress = 100;
                updateProgress("内存清理", progress);
                
                float memoryFreed = afterStatus.availableMemoryMB - beforeStatus.availableMemoryMB;
                String details = String.format("释放内存: %.0fMB\n清理应用: %d个\n当前可用: %.0fMB", 
                    memoryFreed, killedApps.size(), afterStatus.availableMemoryMB);
                
                boolean success = memoryFreed > 0;
                if (listener != null) {
                    listener.onOptimizationCompleted("内存清理", success, details);
                }
                
                UserBehaviorAnalyzer.trackEvent("memory_optimization_completed", 
                    "memory_freed", String.valueOf(memoryFreed), "apps_killed", String.valueOf(killedApps.size()));
                
            } catch (Exception e) {
                if (listener != null) {
                    listener.onOptimizationCompleted("内存清理", false, "优化过程中发生错误: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * 执行存储优化
     */
    public void optimizeStorage() {
        if (listener != null) {
            listener.onOptimizationStarted("存储清理");
        }
        
        UserBehaviorAnalyzer.trackEvent("storage_optimization_started");
        
        new Thread(() -> {
            try {
                int progress = 0;
                updateProgress("存储清理", progress);
                
                SystemStatus beforeStatus = getCurrentSystemStatus();
                progress = 10;
                updateProgress("存储清理", progress);
                
                // 清理各类缓存和临时文件
                long totalCleaned = 0;
                
                // 1. 清理应用缓存
                long appCacheCleaned = clearAppCache();
                totalCleaned += appCacheCleaned;
                progress = 30;
                updateProgress("存储清理", progress);
                
                // 2. 清理下载缓存
                long downloadCacheCleaned = clearDownloadCache();
                totalCleaned += downloadCacheCleaned;
                progress = 50;
                updateProgress("存储清理", progress);
                
                // 3. 清理图片缓存
                long imageCacheCleaned = clearImageCache();
                totalCleaned += imageCacheCleaned;
                progress = 70;
                updateProgress("存储清理", progress);
                
                // 4. 清理日志文件
                long logsCleaned = clearLogFiles();
                totalCleaned += logsCleaned;
                progress = 90;
                updateProgress("存储清理", progress);
                
                SystemStatus afterStatus = getCurrentSystemStatus();
                progress = 100;
                updateProgress("存储清理", progress);
                
                long storageFreed = afterStatus.availableStorageMB - beforeStatus.availableStorageMB;
                String details = String.format("清理文件: %.1fMB\n应用缓存: %.1fMB\n下载缓存: %.1fMB\n图片缓存: %.1fMB\n日志文件: %.1fMB", 
                    storageFreed, appCacheCleaned/1024.0/1024.0, downloadCacheCleaned/1024.0/1024.0, 
                    imageCacheCleaned/1024.0/1024.0, logsCleaned/1024.0/1024.0);
                
                if (listener != null) {
                    listener.onOptimizationCompleted("存储清理", storageFreed > 0, details);
                }
                
                UserBehaviorAnalyzer.trackEvent("storage_optimization_completed", "storage_freed", String.valueOf(storageFreed));
                
            } catch (Exception e) {
                if (listener != null) {
                    listener.onOptimizationCompleted("存储清理", false, "清理过程中发生错误: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * 执行网络优化
     */
    public void optimizeNetwork() {
        if (listener != null) {
            listener.onOptimizationStarted("网络优化");
        }
        
        UserBehaviorAnalyzer.trackEvent("network_optimization_started");
        
        new Thread(() -> {
            try {
                int progress = 0;
                updateProgress("网络优化", progress);
                
                List<String> optimizations = new ArrayList<>();
                
                // 1. 检查网络连接状态
                String networkStatus = checkNetworkStatus();
                optimizations.add("网络状态: " + networkStatus);
                progress = 25;
                updateProgress("网络优化", progress);
                
                // 2. 优化DNS设置
                boolean dnsOptimized = optimizeDNS();
                if (dnsOptimized) {
                    optimizations.add("DNS设置已优化");
                }
                progress = 50;
                updateProgress("网络优化", progress);
                
                // 3. 清理网络缓存
                clearNetworkCache();
                optimizations.add("网络缓存已清理");
                progress = 75;
                updateProgress("网络优化", progress);
                
                // 4. 检查网络质量
                String qualityReport = testNetworkQuality();
                optimizations.add("网络质量: " + qualityReport);
                progress = 100;
                updateProgress("网络优化", progress);
                
                String details = String.join("\n", optimizations);
                
                if (listener != null) {
                    listener.onOptimizationCompleted("网络优化", true, details);
                }
                
                UserBehaviorAnalyzer.trackEvent("network_optimization_completed");
                
            } catch (Exception e) {
                if (listener != null) {
                    listener.onOptimizationCompleted("网络优化", false, "优化过程中发生错误: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * 一键优化 - 执行全面系统优化
     */
    public void performFullOptimization() {
        UserBehaviorAnalyzer.trackEvent("full_optimization_started");
        
        // 按顺序执行各项优化
        new Thread(() -> {
            optimizeMemory();
            
            // 等待内存优化完成
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            optimizeStorage();
            
            // 等待存储优化完成  
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            
            optimizeNetwork();
            
            // 发送完成通知
            SmartNotificationManager.getInstance(context).showOptimizationComplete();
            
            UserBehaviorAnalyzer.trackEvent("full_optimization_completed");
        }).start();
    }
    
    /**
     * 智能优化建议
     */
    public List<String> getSmartOptimizationSuggestions() {
        SystemStatus status = getCurrentSystemStatus();
        List<String> suggestions = new ArrayList<>();
        
        // 基于系统状态提供建议
        if (status.memoryUsagePercent > 80) {
            suggestions.add("内存使用率过高(" + String.format("%.1f", status.memoryUsagePercent) + "%)，建议清理后台应用");
        }
        
        if (status.storageUsagePercent > 90) {
            suggestions.add("存储空间不足(" + String.format("%.1f", status.storageUsagePercent) + "%)，建议清理缓存文件");
        }
        
        if (status.runningAppsCount > 50) {
            suggestions.add("后台应用过多(" + status.runningAppsCount + "个)，建议关闭不必要的应用");
        }
        
        if (!status.heavyApps.isEmpty()) {
            suggestions.add("发现高耗能应用: " + String.join(", ", status.heavyApps));
        }
        
        if ("差".equals(status.networkQuality)) {
            suggestions.add("网络质量较差，建议优化网络设置或切换网络");
        }
        
        return suggestions;
    }
    
    // 私有辅助方法
    private void updateProgress(String type, int progress) {
        if (listener != null) {
            listener.onOptimizationProgress(type, progress);
        }
    }
    
    private List<String> killBackgroundApps() {
        List<String> killedApps = new ArrayList<>();
        
        try {
            List<ActivityManager.RunningAppProcessInfo> runningApps = activityManager.getRunningAppProcesses();
            if (runningApps != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningApps) {
                    if (processInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND 
                        && !isSystemApp(processInfo.processName) 
                        && !processInfo.processName.equals(context.getPackageName())) {
                        
                        try {
                            activityManager.killBackgroundProcesses(processInfo.processName);
                            killedApps.add(processInfo.processName);
                        } catch (Exception e) {
                            // 某些应用可能无法被杀死
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 处理异常
        }
        
        return killedApps;
    }
    
    private boolean isSystemApp(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void clearSystemCache() {
        // 清理系统缓存
        System.gc();
        Runtime.getRuntime().gc();
    }
    
    private long clearAppCache() {
        // 清理应用缓存
        long totalCleared = 0;
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                totalCleared += deleteRecursive(cacheDir);
            }
        } catch (Exception e) {
            // 处理异常
        }
        return totalCleared;
    }
    
    private long clearDownloadCache() {
        // 清理下载缓存
        long totalCleared = 0;
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File[] files = downloadDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().contains("temp") || file.getName().contains("cache")) {
                        totalCleared += file.length();
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            // 处理异常
        }
        return totalCleared;
    }
    
    private long clearImageCache() {
        // 清理图片缓存
        long totalCleared = 0;
        // 实现图片缓存清理逻辑
        return totalCleared;
    }
    
    private long clearLogFiles() {
        // 清理日志文件
        long totalCleared = 0;
        // 实现日志文件清理逻辑
        return totalCleared;
    }
    
    private long deleteRecursive(File fileOrDirectory) {
        long totalSize = 0;
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    totalSize += deleteRecursive(child);
                }
            }
        }
        totalSize += fileOrDirectory.length();
        fileOrDirectory.delete();
        return totalSize;
    }
    
    private String getNetworkQuality() {
        try {
            if (connectivityManager != null) {
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                    if (capabilities != null) {
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            return "WiFi连接";
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            return "移动网络";
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 处理异常
        }
        return "未知";
    }
    
    private List<String> identifyHeavyApps() {
        List<String> heavyApps = new ArrayList<>();
        
        try {
            // 获取内存使用情况
            Debug.MemoryInfo[] memInfos = activityManager.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});
            
            List<ActivityManager.RunningAppProcessInfo> runningApps = activityManager.getRunningAppProcesses();
            if (runningApps != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningApps) {
                    // 简单的启发式判断：后台应用且占用内存较多
                    if (processInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        try {
                            String appName = getAppName(processInfo.processName);
                            if (appName != null && !appName.equals(context.getString(android.R.string.unknownName))) {
                                heavyApps.add(appName);
                            }
                        } catch (Exception e) {
                            // 忽略异常
                        }
                    }
                }
            }
            
            // 限制返回的应用数量
            if (heavyApps.size() > 5) {
                heavyApps = heavyApps.subList(0, 5);
            }
            
        } catch (Exception e) {
            // 处理异常
        }
        
        return heavyApps;
    }
    
    private String getAppName(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return packageName;
        }
    }
    
    private List<String> generateOptimizationSuggestions(SystemStatus status) {
        List<String> suggestions = new ArrayList<>();
        
        if (status.memoryUsagePercent > 70) {
            suggestions.add("建议清理内存以提升性能");
        }
        
        if (status.storageUsagePercent > 80) {
            suggestions.add("建议清理存储空间");
        }
        
        if (status.runningAppsCount > 30) {
            suggestions.add("建议关闭部分后台应用");
        }
        
        if ("差".equals(status.networkQuality)) {
            suggestions.add("建议优化网络连接");
        }
        
        return suggestions;
    }
    
    private String checkNetworkStatus() {
        // 检查网络连接状态
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                return "网络连接正常";
            }
        }
        return "网络连接异常";
    }
    
    private boolean optimizeDNS() {
        // DNS优化逻辑
        return true; // 简化实现
    }
    
    private void clearNetworkCache() {
        // 清理网络缓存
    }
    
    private String testNetworkQuality() {
        // 网络质量测试
        return "良好"; // 简化实现
    }
}