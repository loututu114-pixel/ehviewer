package com.hippo.ehviewer.ui.browser;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserDownloadManager {
    private static final String TAG = "BrowserDownloadManager";
    
    private static BrowserDownloadManager instance;
    private Context context;
    private DownloadManager downloadManager;
    private Map<Long, DownloadInfo> activeDownloads;
    private List<DownloadListener> listeners;
    
    public interface DownloadListener {
        void onDownloadStarted(DownloadInfo info);
        void onDownloadProgress(DownloadInfo info);
        void onDownloadCompleted(DownloadInfo info);
        void onDownloadFailed(DownloadInfo info, String reason);
    }
    
    public static class DownloadInfo {
        public long id;
        public String fileName;
        public String url;
        public String mimeType;
        public long totalSize;
        public long downloadedSize;
        public int status;
        public int progress;
        public String localPath;
        public long startTime;
        public long endTime;
        public String speed;
        public String timeRemaining;
        
        public enum Status {
            PENDING(DownloadManager.STATUS_PENDING),
            RUNNING(DownloadManager.STATUS_RUNNING),
            PAUSED(DownloadManager.STATUS_PAUSED),
            SUCCESSFUL(DownloadManager.STATUS_SUCCESSFUL),
            FAILED(DownloadManager.STATUS_FAILED);
            
            private final int value;
            
            Status(int value) {
                this.value = value;
            }
            
            public int getValue() {
                return value;
            }
            
            public static Status fromValue(int value) {
                for (Status status : values()) {
                    if (status.value == value) {
                        return status;
                    }
                }
                return PENDING;
            }
        }
    }
    
    private BrowserDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.activeDownloads = new HashMap<>();
        this.listeners = new ArrayList<>();
        
        registerReceiver();
    }
    
    public static synchronized BrowserDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new BrowserDownloadManager(context);
        }
        return instance;
    }
    
    public void addListener(DownloadListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(DownloadListener listener) {
        listeners.remove(listener);
    }
    
    // 开始下载
    public long startDownload(String url, String userAgent, String contentDisposition, 
                              String mimeType, long contentLength) {
        try {
            // 获取文件名
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            
            // 创建下载目录
            File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "EhViewer");
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            
            // 构建下载请求
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription("正在下载...");
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, "EhViewer/" + fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.addRequestHeader("User-Agent", userAgent);
            
            if (mimeType != null) {
                request.setMimeType(mimeType);
            }
            
            // 开始下载
            long downloadId = downloadManager.enqueue(request);
            
            // 创建下载信息
            DownloadInfo info = new DownloadInfo();
            info.id = downloadId;
            info.fileName = fileName;
            info.url = url;
            info.mimeType = mimeType;
            info.totalSize = contentLength;
            info.startTime = System.currentTimeMillis();
            info.status = DownloadManager.STATUS_PENDING;
            
            activeDownloads.put(downloadId, info);
            
            // 通知监听器
            for (DownloadListener listener : listeners) {
                listener.onDownloadStarted(info);
            }
            
            Log.d(TAG, "Download started: " + fileName);
            return downloadId;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start download", e);
            return -1;
        }
    }
    
    // 暂停下载
    public void pauseDownload(long downloadId) {
        // Android DownloadManager 不直接支持暂停，需要取消然后重新下载
        // 这里只是移除下载任务
        downloadManager.remove(downloadId);
        activeDownloads.remove(downloadId);
    }
    
    // 恢复下载
    public void resumeDownload(DownloadInfo info) {
        if (info != null && info.url != null) {
            startDownload(info.url, "Mozilla/5.0", null, info.mimeType, info.totalSize);
        }
    }
    
    // 取消下载
    public void cancelDownload(long downloadId) {
        downloadManager.remove(downloadId);
        activeDownloads.remove(downloadId);
    }
    
    // 获取下载信息
    public DownloadInfo getDownloadInfo(long downloadId) {
        DownloadInfo info = activeDownloads.get(downloadId);
        if (info != null) {
            updateDownloadInfo(info);
        }
        return info;
    }
    
    // 获取所有下载
    public List<DownloadInfo> getAllDownloads() {
        List<DownloadInfo> downloads = new ArrayList<>();
        
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = downloadManager.query(query);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadInfo info = createDownloadInfoFromCursor(cursor);
                if (info != null) {
                    downloads.add(info);
                }
            }
            cursor.close();
        }
        
        return downloads;
    }
    
    // 获取进行中的下载
    public List<DownloadInfo> getActiveDownloads() {
        List<DownloadInfo> downloads = new ArrayList<>();
        
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PENDING);
        
        Cursor cursor = downloadManager.query(query);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadInfo info = createDownloadInfoFromCursor(cursor);
                if (info != null) {
                    downloads.add(info);
                }
            }
            cursor.close();
        }
        
        return downloads;
    }
    
    // 获取已完成的下载
    public List<DownloadInfo> getCompletedDownloads() {
        List<DownloadInfo> downloads = new ArrayList<>();
        
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
        
        Cursor cursor = downloadManager.query(query);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadInfo info = createDownloadInfoFromCursor(cursor);
                if (info != null) {
                    downloads.add(info);
                }
            }
            cursor.close();
        }
        
        return downloads;
    }
    
    // 清除下载记录
    public void clearDownloadHistory() {
        List<DownloadInfo> completed = getCompletedDownloads();
        for (DownloadInfo info : completed) {
            downloadManager.remove(info.id);
        }
    }
    
    private void updateDownloadInfo(DownloadInfo info) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(info.id);
        
        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            
            info.status = cursor.getInt(statusIndex);
            info.downloadedSize = cursor.getLong(bytesIndex);
            info.totalSize = cursor.getLong(totalIndex);
            
            if (info.totalSize > 0) {
                info.progress = (int) ((info.downloadedSize * 100) / info.totalSize);
            }
            
            // 计算速度和剩余时间
            long elapsedTime = System.currentTimeMillis() - info.startTime;
            if (elapsedTime > 0 && info.downloadedSize > 0) {
                long speed = (info.downloadedSize * 1000) / elapsedTime; // bytes per second
                info.speed = formatSpeed(speed);
                
                if (speed > 0) {
                    long remainingBytes = info.totalSize - info.downloadedSize;
                    long remainingTime = remainingBytes / speed; // seconds
                    info.timeRemaining = formatTime(remainingTime);
                }
            }
            
            cursor.close();
        }
    }
    
    private DownloadInfo createDownloadInfoFromCursor(Cursor cursor) {
        try {
            DownloadInfo info = new DownloadInfo();
            
            int idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
            int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
            int localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            
            info.id = cursor.getLong(idIndex);
            info.fileName = cursor.getString(titleIndex);
            info.status = cursor.getInt(statusIndex);
            info.downloadedSize = cursor.getLong(bytesIndex);
            info.totalSize = cursor.getLong(totalIndex);
            info.url = cursor.getString(uriIndex);
            
            String localUri = cursor.getString(localUriIndex);
            if (localUri != null) {
                info.localPath = Uri.parse(localUri).getPath();
            }
            
            if (info.totalSize > 0) {
                info.progress = (int) ((info.downloadedSize * 100) / info.totalSize);
            }
            
            return info;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating download info from cursor", e);
            return null;
        }
    }
    
    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024));
        }
    }
    
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return "剩余 " + seconds + " 秒";
        } else if (seconds < 3600) {
            return "剩余 " + (seconds / 60) + " 分钟";
        } else {
            return "剩余 " + (seconds / 3600) + " 小时";
        }
    }
    
    private void registerReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    
                    if (downloadId != -1) {
                        DownloadInfo info = getDownloadInfo(downloadId);
                        if (info != null) {
                            info.endTime = System.currentTimeMillis();
                            
                            if (info.status == DownloadManager.STATUS_SUCCESSFUL) {
                                for (DownloadListener listener : listeners) {
                                    listener.onDownloadCompleted(info);
                                }
                            } else if (info.status == DownloadManager.STATUS_FAILED) {
                                for (DownloadListener listener : listeners) {
                                    listener.onDownloadFailed(info, "Download failed");
                                }
                            }
                        }
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        context.registerReceiver(receiver, filter);
    }
}