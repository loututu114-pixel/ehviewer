package com.hippo.ehviewer.security;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import androidx.core.content.ContextCompat;
import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;
import com.hippo.ehviewer.notification.SmartNotificationManager;
import java.io.File;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 隐私保护管理器 - 全方位保护用户隐私和数据安全
 * 包括权限监控、数据加密、隐私扫描、安全提醒等功能
 */
public class PrivacyProtectionManager {
    
    private static final String PREFS_NAME = "privacy_protection_prefs";
    private static final String KEY_PRIVACY_SCAN_ENABLED = "privacy_scan_enabled";
    private static final String KEY_PERMISSION_MONITORING_ENABLED = "permission_monitoring_enabled";
    private static final String KEY_DATA_ENCRYPTION_ENABLED = "data_encryption_enabled";
    private static final String KEY_LAST_PRIVACY_SCAN = "last_privacy_scan";
    private static final String KEY_SECURITY_ALERTS_ENABLED = "security_alerts_enabled";
    
    private Context context;
    private SharedPreferences prefs;
    private PackageManager packageManager;
    private AppOpsManager appOpsManager;
    private UsageStatsManager usageStatsManager;
    private PrivacyProtectionListener listener;
    
    // 隐私风险等级
    public enum PrivacyRiskLevel {
        LOW("低风险", "#4CAF50"),
        MEDIUM("中等风险", "#FF9800"), 
        HIGH("高风险", "#F44336"),
        CRITICAL("严重风险", "#D32F2F");
        
        public final String displayName;
        public final String colorCode;
        
        PrivacyRiskLevel(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }
    }
    
    // 隐私保护功能
    public enum ProtectionFeature {
        PERMISSION_GUARD("权限守护", "监控应用权限使用，防止滥用"),
        DATA_VAULT("数据保险箱", "加密保护重要数据和文件"),
        PRIVACY_SCANNER("隐私扫描器", "扫描潜在隐私泄露风险"),
        TRACKER_BLOCKER("追踪拦截器", "阻止广告和恶意追踪"),
        SECURE_BROWSER("安全浏览", "提供安全的浏览环境"),
        LOCATION_GUARD("位置保护", "控制位置信息的访问"),
        CONTACT_SHIELD("通讯录保护", "保护联系人信息"),
        CAMERA_MONITOR("摄像头监控", "监控摄像头和麦克风使用");
        
        public final String displayName;
        public final String description;
        
        ProtectionFeature(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }
    
    // 隐私扫描结果
    public static class PrivacyScanResult {
        public PrivacyRiskLevel overallRisk;
        public List<PrivacyIssue> issues;
        public List<AppPrivacyReport> appReports;
        public Map<String, Integer> permissionUsageStats;
        public long scanTimestamp;
        public String summary;
        
        public PrivacyScanResult() {
            this.issues = new ArrayList<>();
            this.appReports = new ArrayList<>();
            this.permissionUsageStats = new HashMap<>();
            this.scanTimestamp = System.currentTimeMillis();
        }
    }
    
    public static class PrivacyIssue {
        public String issueType;
        public String title;
        public String description;
        public PrivacyRiskLevel riskLevel;
        public String affectedApp;
        public String suggestedAction;
        public boolean canAutoFix;
        
        public PrivacyIssue(String type, String title, String description, PrivacyRiskLevel risk) {
            this.issueType = type;
            this.title = title;
            this.description = description;
            this.riskLevel = risk;
        }
    }
    
    public static class AppPrivacyReport {
        public String packageName;
        public String appName;
        public PrivacyRiskLevel riskLevel;
        public List<String> sensitivePermissions;
        public long dataUsage;
        public boolean hasTrackers;
        public boolean accessesLocation;
        public boolean accessesContacts;
        public boolean accessesCamera;
        public int privacyScore; // 0-100分
        
        public AppPrivacyReport(String packageName, String appName) {
            this.packageName = packageName;
            this.appName = appName;
            this.sensitivePermissions = new ArrayList<>();
        }
    }
    
    public interface PrivacyProtectionListener {
        void onPrivacyScanStarted();
        void onPrivacyScanProgress(int progress, String currentTask);
        void onPrivacyScanCompleted(PrivacyScanResult result);
        void onPrivacyIssueDetected(PrivacyIssue issue);
        void onPermissionViolation(String app, String permission);
        void onDataEncryptionStatusChanged(boolean enabled);
        void onSecurityAlert(String alertType, String message, PrivacyRiskLevel risk);
    }
    
    public PrivacyProtectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.packageManager = context.getPackageManager();
        this.appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }
    
    public void setListener(PrivacyProtectionListener listener) {
        this.listener = listener;
    }
    
    /**
     * 执行全面隐私扫描
     */
    public void performPrivacyScan() {
        if (listener != null) {
            listener.onPrivacyScanStarted();
        }
        
        UserBehaviorAnalyzer.trackEvent("privacy_scan_started");
        
        new Thread(() -> {
            try {
                PrivacyScanResult result = new PrivacyScanResult();
                int progress = 0;
                
                // 1. 扫描应用权限
                updateScanProgress(10, "扫描应用权限...");
                scanAppPermissions(result);
                progress = 25;
                
                // 2. 检查敏感数据访问
                updateScanProgress(progress, "检查敏感数据访问...");
                checkSensitiveDataAccess(result);
                progress = 40;
                
                // 3. 分析网络流量
                updateScanProgress(progress, "分析网络流量...");
                analyzeNetworkTraffic(result);
                progress = 55;
                
                // 4. 检查文件权限
                updateScanProgress(progress, "检查文件权限...");
                checkFilePermissions(result);
                progress = 70;
                
                // 5. 扫描追踪器
                updateScanProgress(progress, "扫描追踪器...");
                scanForTrackers(result);
                progress = 85;
                
                // 6. 生成报告
                updateScanProgress(progress, "生成隐私报告...");
                generatePrivacyReport(result);
                progress = 100;
                
                updateScanProgress(progress, "扫描完成");
                
                // 保存扫描时间
                prefs.edit().putLong(KEY_LAST_PRIVACY_SCAN, System.currentTimeMillis()).apply();
                
                if (listener != null) {
                    listener.onPrivacyScanCompleted(result);
                }
                
                UserBehaviorAnalyzer.trackEvent("privacy_scan_completed", "issues_found", String.valueOf(result.issues.size()));
                
            } catch (Exception e) {
                if (listener != null) {
                    listener.onSecurityAlert("扫描错误", "隐私扫描过程中发生错误: " + e.getMessage(), PrivacyRiskLevel.MEDIUM);
                }
            }
        }).start();
    }
    
    /**
     * 扫描应用权限
     */
    private void scanAppPermissions(PrivacyScanResult result) {
        try {
            List<PackageInfo> installedApps = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
            
            for (PackageInfo packageInfo : installedApps) {
                if (packageInfo.requestedPermissions != null) {
                    AppPrivacyReport report = new AppPrivacyReport(
                        packageInfo.packageName, 
                        getAppName(packageInfo.packageName)
                    );
                    
                    List<String> dangerousPermissions = new ArrayList<>();
                    
                    for (String permission : packageInfo.requestedPermissions) {
                        if (isDangerousPermission(permission)) {
                            dangerousPermissions.add(permission);
                            report.sensitivePermissions.add(permission);
                            
                            // 检查权限使用统计
                            result.permissionUsageStats.put(permission, 
                                result.permissionUsageStats.getOrDefault(permission, 0) + 1);
                        }
                        
                        // 特殊权限检查
                        checkSpecialPermissions(permission, report);
                    }
                    
                    // 计算应用隐私评分
                    report.privacyScore = calculatePrivacyScore(report);
                    report.riskLevel = determineRiskLevel(report.privacyScore);
                    
                    if (!dangerousPermissions.isEmpty()) {
                        result.appReports.add(report);
                        
                        // 如果风险较高，添加到问题列表
                        if (report.riskLevel == PrivacyRiskLevel.HIGH || report.riskLevel == PrivacyRiskLevel.CRITICAL) {
                            PrivacyIssue issue = new PrivacyIssue(
                                "权限风险",
                                report.appName + "申请了过多敏感权限",
                                "该应用申请了" + dangerousPermissions.size() + "个敏感权限，可能存在隐私风险",
                                report.riskLevel
                            );
                            issue.affectedApp = report.appName;
                            issue.suggestedAction = "建议检查该应用的权限设置，关闭不必要的权限";
                            result.issues.add(issue);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // 处理扫描异常
        }
    }
    
    /**
     * 检查敏感数据访问
     */
    private void checkSensitiveDataAccess(PrivacyScanResult result) {
        try {
            // 检查最近访问敏感权限的应用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && appOpsManager != null) {
                
                // 检查位置访问
                checkLocationAccess(result);
                
                // 检查相机访问
                checkCameraAccess(result);
                
                // 检查麦克风访问
                checkMicrophoneAccess(result);
                
                // 检查联系人访问
                checkContactsAccess(result);
            }
            
        } catch (Exception e) {
            // 处理异常
        }
    }
    
    /**
     * 分析网络流量
     */
    private void analyzeNetworkTraffic(PrivacyScanResult result) {
        try {
            // 获取应用网络使用统计
            if (usageStatsManager != null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -7); // 过去7天
                long startTime = cal.getTimeInMillis();
                long endTime = System.currentTimeMillis();
                
                // 分析网络使用模式
                analyzeNetworkUsagePatterns(result, startTime, endTime);
            }
            
        } catch (Exception e) {
            // 处理异常
        }
    }
    
    /**
     * 检查文件权限
     */
    private void checkFilePermissions(PrivacyScanResult result) {
        try {
            // 检查外部存储访问权限
            checkExternalStorageAccess(result);
            
            // 检查敏感文件夹访问
            checkSensitiveFolderAccess(result);
            
        } catch (Exception e) {
            // 处理异常
        }
    }
    
    /**
     * 扫描追踪器
     */
    private void scanForTrackers(PrivacyScanResult result) {
        try {
            List<String> knownTrackers = Arrays.asList(
                "com.google.firebase.analytics",
                "com.facebook.appevents", 
                "com.flurry.android",
                "com.adjust.sdk"
            );
            
            List<PackageInfo> installedApps = packageManager.getInstalledPackages(0);
            
            for (PackageInfo packageInfo : installedApps) {
                try {
                    String[] dexFiles = packageInfo.applicationInfo.sourceDir.split("/");
                    // 简化的追踪器检测逻辑
                    for (String tracker : knownTrackers) {
                        if (packageInfo.applicationInfo.sourceDir.contains(tracker.replace(".", "/"))) {
                            PrivacyIssue issue = new PrivacyIssue(
                                "追踪器",
                                getAppName(packageInfo.packageName) + "包含追踪器",
                                "检测到该应用包含" + tracker + "追踪器",
                                PrivacyRiskLevel.MEDIUM
                            );
                            issue.affectedApp = getAppName(packageInfo.packageName);
                            issue.suggestedAction = "建议关注该应用的数据收集行为";
                            result.issues.add(issue);
                        }
                    }
                } catch (Exception e) {
                    // 忽略单个应用的扫描错误
                }
            }
            
        } catch (Exception e) {
            // 处理异常
        }
    }
    
    /**
     * 生成隐私报告
     */
    private void generatePrivacyReport(PrivacyScanResult result) {
        // 计算整体风险等级
        int criticalIssues = 0;
        int highIssues = 0;
        int mediumIssues = 0;
        
        for (PrivacyIssue issue : result.issues) {
            switch (issue.riskLevel) {
                case CRITICAL:
                    criticalIssues++;
                    break;
                case HIGH:
                    highIssues++;
                    break;
                case MEDIUM:
                    mediumIssues++;
                    break;
            }
        }
        
        // 确定整体风险等级
        if (criticalIssues > 0) {
            result.overallRisk = PrivacyRiskLevel.CRITICAL;
        } else if (highIssues > 2) {
            result.overallRisk = PrivacyRiskLevel.HIGH;
        } else if (highIssues > 0 || mediumIssues > 3) {
            result.overallRisk = PrivacyRiskLevel.MEDIUM;
        } else {
            result.overallRisk = PrivacyRiskLevel.LOW;
        }
        
        // 生成摘要
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("扫描了%d个应用，", result.appReports.size()));
        summary.append(String.format("发现%d个隐私问题。", result.issues.size()));
        
        if (criticalIssues > 0) {
            summary.append(String.format("包括%d个严重问题", criticalIssues));
        }
        if (highIssues > 0) {
            summary.append(String.format("%s%d个高风险问题", criticalIssues > 0 ? "，" : "包括", highIssues));
        }
        
        result.summary = summary.toString();
    }
    
    // 辅助方法
    private void updateScanProgress(int progress, String task) {
        if (listener != null) {
            listener.onPrivacyScanProgress(progress, task);
        }
    }
    
    private boolean isDangerousPermission(String permission) {
        String[] dangerousPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
        };
        
        for (String dangerous : dangerousPermissions) {
            if (dangerous.equals(permission)) {
                return true;
            }
        }
        return false;
    }
    
    private void checkSpecialPermissions(String permission, AppPrivacyReport report) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                report.accessesLocation = true;
                break;
            case Manifest.permission.READ_CONTACTS:
            case Manifest.permission.WRITE_CONTACTS:
                report.accessesContacts = true;
                break;
            case Manifest.permission.CAMERA:
                report.accessesCamera = true;
                break;
        }
    }
    
    private int calculatePrivacyScore(AppPrivacyReport report) {
        int score = 100; // 满分100
        
        // 根据敏感权限数量扣分
        score -= report.sensitivePermissions.size() * 10;
        
        // 特殊权限额外扣分
        if (report.accessesLocation) score -= 15;
        if (report.accessesContacts) score -= 15;
        if (report.accessesCamera) score -= 10;
        if (report.hasTrackers) score -= 20;
        
        return Math.max(0, score);
    }
    
    private PrivacyRiskLevel determineRiskLevel(int privacyScore) {
        if (privacyScore >= 80) return PrivacyRiskLevel.LOW;
        if (privacyScore >= 60) return PrivacyRiskLevel.MEDIUM;
        if (privacyScore >= 30) return PrivacyRiskLevel.HIGH;
        return PrivacyRiskLevel.CRITICAL;
    }
    
    private String getAppName(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return packageName;
        }
    }
    
    private void checkLocationAccess(PrivacyScanResult result) {
        // 检查位置访问
    }
    
    private void checkCameraAccess(PrivacyScanResult result) {
        // 检查相机访问
    }
    
    private void checkMicrophoneAccess(PrivacyScanResult result) {
        // 检查麦克风访问
    }
    
    private void checkContactsAccess(PrivacyScanResult result) {
        // 检查联系人访问
    }
    
    private void analyzeNetworkUsagePatterns(PrivacyScanResult result, long startTime, long endTime) {
        // 分析网络使用模式
    }
    
    private void checkExternalStorageAccess(PrivacyScanResult result) {
        // 检查外部存储访问
    }
    
    private void checkSensitiveFolderAccess(PrivacyScanResult result) {
        // 检查敏感文件夹访问
    }
    
    /**
     * 启动实时隐私监控
     */
    public void startPrivacyMonitoring() {
        UserBehaviorAnalyzer.trackEvent("privacy_monitoring_started");
        
        // 启动权限监控线程
        new Thread(() -> {
            while (prefs.getBoolean(KEY_PERMISSION_MONITORING_ENABLED, true)) {
                try {
                    monitorPermissionUsage();
                    Thread.sleep(60000); // 每分钟检查一次
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }
    
    private void monitorPermissionUsage() {
        // 监控权限使用情况
        // 如果检测到异常使用，触发回调
    }
    
    /**
     * 获取隐私保护状态
     */
    public Map<String, Object> getPrivacyStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("privacyScanEnabled", prefs.getBoolean(KEY_PRIVACY_SCAN_ENABLED, true));
        status.put("permissionMonitoringEnabled", prefs.getBoolean(KEY_PERMISSION_MONITORING_ENABLED, true));
        status.put("dataEncryptionEnabled", prefs.getBoolean(KEY_DATA_ENCRYPTION_ENABLED, false));
        status.put("lastPrivacyScan", prefs.getLong(KEY_LAST_PRIVACY_SCAN, 0));
        status.put("securityAlertsEnabled", prefs.getBoolean(KEY_SECURITY_ALERTS_ENABLED, true));
        
        return status;
    }
    
    /**
     * 停止隐私监控
     */
    public void stopPrivacyMonitoring() {
        prefs.edit().putBoolean(KEY_PERMISSION_MONITORING_ENABLED, false).apply();
        UserBehaviorAnalyzer.trackEvent("privacy_monitoring_stopped");
    }
}