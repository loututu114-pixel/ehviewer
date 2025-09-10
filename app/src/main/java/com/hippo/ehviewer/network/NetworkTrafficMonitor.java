package com.hippo.ehviewer.network;

import android.content.Context;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 网络流量监控器 - 实时监控和统计网络使用情况
 * 
 * 核心功能：
 * 1. 实时流量监控
 * 2. 应用级流量统计
 * 3. 节省流量统计
 * 4. 网络速度测量
 * 5. 流量使用分析
 * 6. 监控事件通知
 */
public class NetworkTrafficMonitor {
    private static final String TAG = "NetworkTrafficMonitor";
    
    private final Context mContext;
    private final Handler mHandler;
    
    // 监控状态
    private final AtomicBoolean mIsMonitoring = new AtomicBoolean(false);
    private final AtomicLong mStartTime = new AtomicLong(0);
    
    // 流量统计
    private final AtomicLong mInitialRxBytes = new AtomicLong(0);
    private final AtomicLong mInitialTxBytes = new AtomicLong(0);
    private final AtomicLong mCurrentRxBytes = new AtomicLong(0);
    private final AtomicLong mCurrentTxBytes = new AtomicLong(0);
    private final AtomicLong mSavedBytes = new AtomicLong(0); // 通过优化节省的流量
    
    // 速度测量
    private final AtomicLong mLastMeasureTime = new AtomicLong(0);
    private final AtomicLong mLastRxBytes = new AtomicLong(0);
    private final AtomicLong mLastTxBytes = new AtomicLong(0);
    private final AtomicLong mCurrentDownloadSpeed = new AtomicLong(0); // bytes/s
    private final AtomicLong mCurrentUploadSpeed = new AtomicLong(0); // bytes/s
    
    // 监控配置
    private static final long MONITOR_INTERVAL_MS = 2000; // 2秒监控一次
    private static final long SPEED_CALCULATION_WINDOW_MS = 5000; // 5秒窗口计算速度
    
    // 监听器
    private final List<TrafficMonitorListener> mListeners = new CopyOnWriteArrayList<>();
    
    // 监控任务
    private Runnable mMonitoringRunnable;
    
    /**
     * 流量监控监听器
     */
    public interface TrafficMonitorListener {
        void onTrafficUpdate(TrafficInfo trafficInfo);
        void onSpeedUpdate(SpeedInfo speedInfo);
        void onDataSaved(long savedBytes, String reason);
    }
    
    /**
     * 流量信息
     */
    public static class TrafficInfo {
        public long sessionRxBytes = 0;     // 本次会话接收字节
        public long sessionTxBytes = 0;     // 本次会话发送字节
        public long sessionTotalBytes = 0;  // 本次会话总字节
        public long totalRxBytes = 0;       // 应用总接收字节
        public long totalTxBytes = 0;       // 应用总发送字节
        public long totalBytes = 0;         // 应用总字节
        public long savedBytes = 0;         // 节省的字节
        public long monitoringDurationMs = 0; // 监控持续时间
        
        @Override
        public String toString() {
            return String.format("流量信息: 本次%.1fMB (收%.1f/发%.1f), 总计%.1fMB, 节省%.1fMB, 时长%ds", 
                sessionTotalBytes / 1024.0 / 1024.0,
                sessionRxBytes / 1024.0 / 1024.0,
                sessionTxBytes / 1024.0 / 1024.0,
                totalBytes / 1024.0 / 1024.0,
                savedBytes / 1024.0 / 1024.0,
                monitoringDurationMs / 1000);
        }
    }
    
    /**
     * 速度信息
     */
    public static class SpeedInfo {
        public long downloadSpeedBps = 0;   // 下载速度 (bytes/s)
        public long uploadSpeedBps = 0;     // 上传速度 (bytes/s)
        public long totalSpeedBps = 0;      // 总速度 (bytes/s)
        public String downloadSpeedText;    // 下载速度文本
        public String uploadSpeedText;      // 上传速度文本
        public String totalSpeedText;       // 总速度文本
        
        public SpeedInfo() {
            updateSpeedTexts();
        }
        
        public void updateSpeedTexts() {
            downloadSpeedText = formatSpeed(downloadSpeedBps);
            uploadSpeedText = formatSpeed(uploadSpeedBps);
            totalSpeedBps = downloadSpeedBps + uploadSpeedBps;
            totalSpeedText = formatSpeed(totalSpeedBps);
        }
        
        private String formatSpeed(long bytesPerSecond) {
            if (bytesPerSecond < 1024) {
                return bytesPerSecond + " B/s";
            } else if (bytesPerSecond < 1024 * 1024) {
                return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
            } else {
                return String.format("%.1f MB/s", bytesPerSecond / 1024.0 / 1024.0);
            }
        }
        
        @Override
        public String toString() {
            return String.format("网速: 下载%s, 上传%s, 总计%s", 
                downloadSpeedText, uploadSpeedText, totalSpeedText);
        }
    }
    
    /**
     * 流量节省记录
     */
    public static class DataSavingRecord {
        public long savedBytes;
        public String reason;
        public long timestamp;
        
        public DataSavingRecord(long savedBytes, String reason) {
            this.savedBytes = savedBytes;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return String.format("节省%.1fMB: %s", savedBytes / 1024.0 / 1024.0, reason);
        }
    }
    
    public NetworkTrafficMonitor(Context context) {
        mContext = context.getApplicationContext();
        mHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "NetworkTrafficMonitor initialized");
    }
    
    /**
     * 开始流量监控
     */
    public void startMonitoring() {
        if (mIsMonitoring.get()) {
            Log.d(TAG, "Traffic monitoring already started");
            return;
        }
        
        Log.d(TAG, "Starting traffic monitoring");
        
        // 初始化统计数据
        initializeStats();
        
        // 创建监控任务
        mMonitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsMonitoring.get()) {
                    updateTrafficStats();
                    calculateNetworkSpeed();
                    
                    // 调度下次监控
                    mHandler.postDelayed(this, MONITOR_INTERVAL_MS);
                }
            }
        };
        
        mIsMonitoring.set(true);
        mStartTime.set(System.currentTimeMillis());
        
        // 启动监控
        mHandler.post(mMonitoringRunnable);
        
        Log.d(TAG, "Traffic monitoring started");
    }
    
    /**
     * 停止流量监控
     */
    public void stopMonitoring() {
        if (!mIsMonitoring.get()) {
            Log.d(TAG, "Traffic monitoring not running");
            return;
        }
        
        Log.d(TAG, "Stopping traffic monitoring");
        
        mIsMonitoring.set(false);
        
        if (mMonitoringRunnable != null) {
            mHandler.removeCallbacks(mMonitoringRunnable);
        }
        
        Log.d(TAG, "Traffic monitoring stopped");
    }
    
    /**
     * 获取当前流量信息
     */
    public TrafficInfo getCurrentTrafficInfo() {
        TrafficInfo info = new TrafficInfo();
        
        try {
            long appRxBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid());
            long appTxBytes = TrafficStats.getUidTxBytes(android.os.Process.myUid());
            
            if (appRxBytes != TrafficStats.UNSUPPORTED && appTxBytes != TrafficStats.UNSUPPORTED) {
                info.totalRxBytes = appRxBytes;
                info.totalTxBytes = appTxBytes;
                info.totalBytes = appRxBytes + appTxBytes;
                
                if (mIsMonitoring.get()) {
                    info.sessionRxBytes = appRxBytes - mInitialRxBytes.get();
                    info.sessionTxBytes = appTxBytes - mInitialTxBytes.get();
                    info.sessionTotalBytes = info.sessionRxBytes + info.sessionTxBytes;
                    info.monitoringDurationMs = System.currentTimeMillis() - mStartTime.get();
                }
            }
            
            info.savedBytes = mSavedBytes.get();
            
        } catch (Exception e) {
            Log.w(TAG, "Error getting traffic info", e);
        }
        
        return info;
    }
    
    /**
     * 获取当前网速信息
     */
    public SpeedInfo getCurrentSpeedInfo() {
        SpeedInfo info = new SpeedInfo();
        info.downloadSpeedBps = mCurrentDownloadSpeed.get();
        info.uploadSpeedBps = mCurrentUploadSpeed.get();
        info.updateSpeedTexts();
        
        return info;
    }
    
    /**
     * 记录节省的流量
     */
    public void recordDataSaving(long savedBytes, @NonNull String reason) {
        if (savedBytes <= 0) return;
        
        mSavedBytes.addAndGet(savedBytes);
        
        Log.d(TAG, String.format("Data saved: %.1f MB, reason: %s", 
            savedBytes / 1024.0 / 1024.0, reason));
        
        // 通知监听器
        for (TrafficMonitorListener listener : mListeners) {
            try {
                listener.onDataSaved(savedBytes, reason);
            } catch (Exception e) {
                Log.w(TAG, "Error notifying data saving", e);
            }
        }
    }
    
    /**
     * 获取节省的总流量
     */
    public long getSavedBytes() {
        return mSavedBytes.get();
    }
    
    /**
     * 重置流量统计
     */
    public void resetStats() {
        Log.d(TAG, "Resetting traffic stats");
        
        mSavedBytes.set(0);
        mCurrentDownloadSpeed.set(0);
        mCurrentUploadSpeed.set(0);
        
        if (mIsMonitoring.get()) {
            initializeStats();
        }
    }
    
    /**
     * 添加监听器
     */
    public void addListener(@NonNull TrafficMonitorListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(@NonNull TrafficMonitorListener listener) {
        mListeners.remove(listener);
    }
    
    /**
     * 检查流量监控是否支持
     */
    public boolean isTrafficStatsSupported() {
        try {
            long rxBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid());
            return rxBytes != TrafficStats.UNSUPPORTED;
        } catch (Exception e) {
            Log.w(TAG, "Error checking traffic stats support", e);
            return false;
        }
    }
    
    // 私有方法
    
    private void initializeStats() {
        try {
            long appRxBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid());
            long appTxBytes = TrafficStats.getUidTxBytes(android.os.Process.myUid());
            
            if (appRxBytes != TrafficStats.UNSUPPORTED && appTxBytes != TrafficStats.UNSUPPORTED) {
                mInitialRxBytes.set(appRxBytes);
                mInitialTxBytes.set(appTxBytes);
                mCurrentRxBytes.set(appRxBytes);
                mCurrentTxBytes.set(appTxBytes);
                
                mLastMeasureTime.set(System.currentTimeMillis());
                mLastRxBytes.set(appRxBytes);
                mLastTxBytes.set(appTxBytes);
                
                Log.d(TAG, String.format("Stats initialized: RX=%d, TX=%d", appRxBytes, appTxBytes));
            } else {
                Log.w(TAG, "Traffic stats not supported on this device");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing stats", e);
        }
    }
    
    private void updateTrafficStats() {
        try {
            long appRxBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid());
            long appTxBytes = TrafficStats.getUidTxBytes(android.os.Process.myUid());
            
            if (appRxBytes != TrafficStats.UNSUPPORTED && appTxBytes != TrafficStats.UNSUPPORTED) {
                mCurrentRxBytes.set(appRxBytes);
                mCurrentTxBytes.set(appTxBytes);
                
                // 通知监听器
                TrafficInfo trafficInfo = getCurrentTrafficInfo();
                for (TrafficMonitorListener listener : mListeners) {
                    try {
                        listener.onTrafficUpdate(trafficInfo);
                    } catch (Exception e) {
                        Log.w(TAG, "Error notifying traffic update", e);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error updating traffic stats", e);
        }
    }
    
    private void calculateNetworkSpeed() {
        try {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - mLastMeasureTime.get();
            
            if (timeDiff >= SPEED_CALCULATION_WINDOW_MS) {
                long currentRx = mCurrentRxBytes.get();
                long currentTx = mCurrentTxBytes.get();
                
                long rxDiff = currentRx - mLastRxBytes.get();
                long txDiff = currentTx - mLastTxBytes.get();
                
                if (timeDiff > 0) {
                    long downloadSpeed = (rxDiff * 1000) / timeDiff; // bytes/s
                    long uploadSpeed = (txDiff * 1000) / timeDiff; // bytes/s
                    
                    mCurrentDownloadSpeed.set(Math.max(0, downloadSpeed));
                    mCurrentUploadSpeed.set(Math.max(0, uploadSpeed));
                    
                    // 更新上次测量值
                    mLastMeasureTime.set(currentTime);
                    mLastRxBytes.set(currentRx);
                    mLastTxBytes.set(currentTx);
                    
                    // 通知监听器
                    SpeedInfo speedInfo = getCurrentSpeedInfo();
                    for (TrafficMonitorListener listener : mListeners) {
                        try {
                            listener.onSpeedUpdate(speedInfo);
                        } catch (Exception e) {
                            Log.w(TAG, "Error notifying speed update", e);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error calculating network speed", e);
        }
    }
    
    /**
     * 获取流量统计摘要
     */
    public String getTrafficSummary() {
        TrafficInfo trafficInfo = getCurrentTrafficInfo();
        SpeedInfo speedInfo = getCurrentSpeedInfo();
        
        StringBuilder summary = new StringBuilder();
        summary.append("流量监控摘要:\n");
        summary.append(trafficInfo.toString()).append("\n");
        summary.append(speedInfo.toString());
        
        return summary.toString();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up NetworkTrafficMonitor");
        
        stopMonitoring();
        mListeners.clear();
        
        Log.d(TAG, "NetworkTrafficMonitor cleanup completed");
    }
}