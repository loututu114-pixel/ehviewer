package com.hippo.ehviewer.notification;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务触发服务
 * 监控系统状态并触发通知
 */
public class TaskTriggerService extends Service {
    
    private static final String TAG = "TaskTriggerService";
    
    // 监控类型
    public enum MonitorType {
        CPU_USAGE("cpu", "CPU使用率监控"),
        MEMORY_USAGE("memory", "内存使用监控"),
        STORAGE_SPACE("storage", "存储空间监控"),
        NETWORK_STATUS("network", "网络状态监控"),
        FILE_CHANGES("file", "文件变化监控"),
        APP_UPDATES("app", "应用更新监控");
        
        private final String type;
        private final String description;
        
        MonitorType(String type, String description) {
            this.type = type;
            this.description = description;
        }
    }
    
    // 触发条件
    public static class TriggerCondition {
        public MonitorType type;
        public String name;
        public Map<String, Object> params;
        public boolean enabled = true;
        public long interval = 60000; // 默认1分钟检查一次
        
        public TriggerCondition(MonitorType type, String name) {
            this.type = type;
            this.name = name;
            this.params = new HashMap<>();
        }
    }
    
    private final IBinder binder = new LocalBinder();
    private NotificationManager notificationManager;
    private ScheduledExecutorService scheduler;
    private Handler mainHandler;
    private SharedPreferences prefs;
    
    // 监控任务列表
    private List<TriggerCondition> triggers;
    private Map<String, MonitorTask> activeTasks;
    
    // 监控阈值
    private static final float CPU_THRESHOLD = 80.0f; // CPU使用率80%
    private static final float MEMORY_THRESHOLD = 85.0f; // 内存使用率85%
    private static final long STORAGE_THRESHOLD = 100 * 1024 * 1024L; // 剩余空间100MB
    
    // 监听器
    private List<TriggerListener> listeners = new ArrayList<>();
    
    public interface TriggerListener {
        void onTriggerFired(TriggerCondition condition, Map<String, Object> data);
        void onMonitorUpdate(MonitorType type, Map<String, Object> data);
    }
    
    public class LocalBinder extends Binder {
        public TaskTriggerService getService() {
            return TaskTriggerService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        notificationManager = NotificationManager.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("task_triggers", Context.MODE_PRIVATE);
        
        triggers = new ArrayList<>();
        activeTasks = new HashMap<>();
        scheduler = Executors.newScheduledThreadPool(3);
        
        // 初始化默认触发器
        initializeDefaultTriggers();
        
        // 启动监控
        startMonitoring();
        
        Log.d(TAG, "TaskTriggerService created");
    }
    
    /**
     * 初始化默认触发器
     */
    private void initializeDefaultTriggers() {
        // CPU监控
        TriggerCondition cpuTrigger = new TriggerCondition(MonitorType.CPU_USAGE, "CPU高负载警报");
        cpuTrigger.params.put("threshold", CPU_THRESHOLD);
        cpuTrigger.interval = 30000; // 30秒检查一次
        addTrigger(cpuTrigger);
        
        // 内存监控
        TriggerCondition memoryTrigger = new TriggerCondition(MonitorType.MEMORY_USAGE, "内存不足警报");
        memoryTrigger.params.put("threshold", MEMORY_THRESHOLD);
        memoryTrigger.interval = 60000; // 1分钟检查一次
        addTrigger(memoryTrigger);
        
        // 存储空间监控
        TriggerCondition storageTrigger = new TriggerCondition(MonitorType.STORAGE_SPACE, "存储空间不足");
        storageTrigger.params.put("threshold", STORAGE_THRESHOLD);
        storageTrigger.interval = 300000; // 5分钟检查一次
        addTrigger(storageTrigger);
        
        // 文件监控（监控下载目录）
        TriggerCondition fileTrigger = new TriggerCondition(MonitorType.FILE_CHANGES, "下载文件监控");
        fileTrigger.params.put("directory", "/storage/emulated/0/Download");
        fileTrigger.params.put("extensions", new String[]{".epub", ".pdf", ".cbz", ".zip"});
        fileTrigger.interval = 10000; // 10秒检查一次
        addTrigger(fileTrigger);
    }
    
    /**
     * 添加触发器
     */
    public void addTrigger(TriggerCondition trigger) {
        triggers.add(trigger);
        
        if (trigger.enabled) {
            startMonitorTask(trigger);
        }
    }
    
    /**
     * 移除触发器
     */
    public void removeTrigger(String name) {
        triggers.removeIf(t -> t.name.equals(name));
        stopMonitorTask(name);
    }
    
    /**
     * 启动监控
     */
    private void startMonitoring() {
        for (TriggerCondition trigger : triggers) {
            if (trigger.enabled) {
                startMonitorTask(trigger);
            }
        }
    }
    
    /**
     * 启动监控任务
     */
    private void startMonitorTask(TriggerCondition trigger) {
        if (activeTasks.containsKey(trigger.name)) {
            return;
        }
        
        MonitorTask task = new MonitorTask(trigger);
        activeTasks.put(trigger.name, task);
        
        scheduler.scheduleWithFixedDelay(task, 0, trigger.interval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 停止监控任务
     */
    private void stopMonitorTask(String name) {
        MonitorTask task = activeTasks.remove(name);
        if (task != null) {
            task.stop();
        }
    }
    
    /**
     * 监控任务
     */
    private class MonitorTask implements Runnable {
        private TriggerCondition condition;
        private boolean running = true;
        
        public MonitorTask(TriggerCondition condition) {
            this.condition = condition;
        }
        
        @Override
        public void run() {
            if (!running) return;
            
            try {
                switch (condition.type) {
                    case CPU_USAGE:
                        monitorCPU();
                        break;
                        
                    case MEMORY_USAGE:
                        monitorMemory();
                        break;
                        
                    case STORAGE_SPACE:
                        monitorStorage();
                        break;
                        
                    case FILE_CHANGES:
                        monitorFiles();
                        break;
                        
                    case NETWORK_STATUS:
                        monitorNetwork();
                        break;
                        
                    case APP_UPDATES:
                        monitorAppUpdates();
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Monitor task error: " + e.getMessage());
            }
        }
        
        /**
         * 监控CPU使用率
         */
        private void monitorCPU() {
            float cpuUsage = SystemMonitor.getCPUUsage();
            
            Map<String, Object> data = new HashMap<>();
            data.put("usage", cpuUsage);
            data.put("threshold", condition.params.get("threshold"));
            
            // 通知监听器
            notifyMonitorUpdate(condition.type, data);
            
            // 检查是否超过阈值
            Float threshold = (Float) condition.params.get("threshold");
            if (threshold != null && cpuUsage > threshold) {
                data.put("alert", true);
                
                // 触发通知
                fireTrigger(condition, data);
                
                // 发送推送通知
                sendCPUAlert(cpuUsage);
            }
        }
        
        /**
         * 监控内存使用
         */
        private void monitorMemory() {
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;
            float memoryUsage = (float) usedMemory / totalMemory * 100;
            
            Map<String, Object> data = new HashMap<>();
            data.put("total", totalMemory);
            data.put("used", usedMemory);
            data.put("free", freeMemory);
            data.put("usage", memoryUsage);
            
            notifyMonitorUpdate(condition.type, data);
            
            Float threshold = (Float) condition.params.get("threshold");
            if (threshold != null && memoryUsage > threshold) {
                data.put("alert", true);
                fireTrigger(condition, data);
                sendMemoryAlert(memoryUsage, usedMemory, totalMemory);
            }
        }
        
        /**
         * 监控存储空间
         */
        private void monitorStorage() {
            File storage = android.os.Environment.getExternalStorageDirectory();
            long totalSpace = storage.getTotalSpace();
            long freeSpace = storage.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            Map<String, Object> data = new HashMap<>();
            data.put("total", totalSpace);
            data.put("used", usedSpace);
            data.put("free", freeSpace);
            
            notifyMonitorUpdate(condition.type, data);
            
            Long threshold = (Long) condition.params.get("threshold");
            if (threshold != null && freeSpace < threshold) {
                data.put("alert", true);
                fireTrigger(condition, data);
                sendStorageAlert(freeSpace, totalSpace);
            }
        }
        
        /**
         * 监控文件变化
         */
        private void monitorFiles() {
            String directory = (String) condition.params.get("directory");
            String[] extensions = (String[]) condition.params.get("extensions");
            
            if (directory == null) return;
            
            File dir = new File(directory);
            if (!dir.exists() || !dir.isDirectory()) return;
            
            File[] files = dir.listFiles();
            if (files == null) return;
            
            // 检查新文件
            SharedPreferences.Editor editor = prefs.edit();
            String lastCheckKey = "last_check_" + condition.name;
            long lastCheck = prefs.getLong(lastCheckKey, 0);
            
            List<File> newFiles = new ArrayList<>();
            for (File file : files) {
                if (file.lastModified() > lastCheck) {
                    // 检查文件扩展名
                    if (extensions != null && extensions.length > 0) {
                        String fileName = file.getName().toLowerCase();
                        for (String ext : extensions) {
                            if (fileName.endsWith(ext.toLowerCase())) {
                                newFiles.add(file);
                                break;
                            }
                        }
                    } else {
                        newFiles.add(file);
                    }
                }
            }
            
            if (!newFiles.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("files", newFiles);
                data.put("directory", directory);
                
                fireTrigger(condition, data);
                
                // 发送文件发现通知
                for (File file : newFiles) {
                    sendFileDiscoveryNotification(file);
                }
            }
            
            editor.putLong(lastCheckKey, System.currentTimeMillis());
            editor.apply();
        }
        
        /**
         * 监控网络状态
         */
        private void monitorNetwork() {
            boolean isConnected = SystemMonitor.isNetworkConnected(TaskTriggerService.this);
            String networkType = SystemMonitor.getNetworkType(TaskTriggerService.this);
            
            Map<String, Object> data = new HashMap<>();
            data.put("connected", isConnected);
            data.put("type", networkType);
            
            notifyMonitorUpdate(condition.type, data);
        }
        
        /**
         * 监控应用更新
         */
        private void monitorAppUpdates() {
            // 检查应用更新逻辑
            // 这里可以连接到服务器检查版本
        }
        
        public void stop() {
            running = false;
        }
    }
    
    /**
     * 发送CPU警报
     */
    private void sendCPUAlert(float usage) {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "CPU使用率过高",
                String.format("当前CPU使用率: %.1f%%", usage)
            );
        
        data.setType(NotificationManager.NotificationType.CPU_ALERT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setBigText("CPU使用率已超过设定阈值，可能影响应用性能。建议关闭不必要的应用或重启设备。");
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送内存警报
     */
    private void sendMemoryAlert(float usage, long used, long total) {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "内存使用率过高",
                String.format("当前内存使用: %.1f%%", usage)
            );
        
        data.setType(NotificationManager.NotificationType.MEMORY_ALERT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setBigText(String.format("已使用 %s / %s 内存。建议清理后台应用释放内存。",
                formatBytes(used), formatBytes(total)));
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送存储空间警报
     */
    private void sendStorageAlert(long free, long total) {
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(
                "存储空间不足",
                String.format("剩余空间: %s", formatBytes(free))
            );
        
        data.setType(NotificationManager.NotificationType.SYSTEM_ALERT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setBigText(String.format("存储空间仅剩 %s / %s。建议清理不必要的文件。",
                formatBytes(free), formatBytes(total)));
        
        notificationManager.showNotification(data);
    }
    
    /**
     * 发送文件发现通知
     */
    private void sendFileDiscoveryNotification(File file) {
        String fileName = file.getName();
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }
        
        // 判断是否应该用我们的应用打开
        boolean shouldOpen = isOurFileType(extension);
        
        if (shouldOpen) {
            NotificationManager.NotificationData data = 
                new NotificationManager.NotificationData(
                    "发现新文件",
                    fileName
                );
            
            android.os.Bundle extras = new android.os.Bundle();
            extras.putString("file_path", file.getAbsolutePath());
            extras.putString("mime_type", getMimeType(extension));
            
            data.setType(NotificationManager.NotificationType.FILE_OPEN)
                .setExtras(extras)
                .setBigText("点击使用 EhViewer 打开此文件");
            
            notificationManager.showNotification(data);
        }
    }
    
    /**
     * 判断是否是我们支持的文件类型
     */
    private boolean isOurFileType(String extension) {
        String[] supportedTypes = {
            "epub", "pdf", "cbz", "cbr", "zip", "rar",
            "jpg", "jpeg", "png", "gif", "webp",
            "mp4", "webm", "mkv",
            "txt", "html", "htm"
        };
        
        for (String type : supportedTypes) {
            if (type.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取MIME类型
     */
    private String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case "epub": return "application/epub+zip";
            case "pdf": return "application/pdf";
            case "cbz": return "application/x-cbz";
            case "cbr": return "application/x-cbr";
            case "zip": return "application/zip";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "mp4": return "video/mp4";
            case "webm": return "video/webm";
            case "txt": return "text/plain";
            case "html":
            case "htm": return "text/html";
            default: return "*/*";
        }
    }
    
    /**
     * 触发事件
     */
    private void fireTrigger(TriggerCondition condition, Map<String, Object> data) {
        mainHandler.post(() -> {
            for (TriggerListener listener : listeners) {
                listener.onTriggerFired(condition, data);
            }
        });
    }
    
    /**
     * 通知监控更新
     */
    private void notifyMonitorUpdate(MonitorType type, Map<String, Object> data) {
        mainHandler.post(() -> {
            for (TriggerListener listener : listeners) {
                listener.onMonitorUpdate(type, data);
            }
        });
    }
    
    /**
     * 格式化字节
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * 添加监听器
     */
    public void addListener(TriggerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(TriggerListener listener) {
        listeners.remove(listener);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        activeTasks.clear();
        listeners.clear();
    }
}