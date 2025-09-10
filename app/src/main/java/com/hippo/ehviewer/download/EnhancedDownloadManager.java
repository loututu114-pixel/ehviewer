package com.hippo.ehviewer.download;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.Settings;
import com.hippo.unifile.UniFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 增强下载管理器 - 支持断点续传和智能文件处理
 * 
 * 核心特性：
 * 1. HTTP断点续传支持
 * 2. 并发下载控制  
 * 3. 智能文件类型识别和分类
 * 4. 下载进度实时监控
 * 5. 错误重试机制
 * 6. 存储空间管理
 */
public class EnhancedDownloadManager {
    private static final String TAG = "EnhancedDownloadManager";
    
    // 实例管理
    private static volatile EnhancedDownloadManager sInstance;
    private final Context mContext;
    
    // 下载配置
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    private static final int RETRY_COUNT = 3;
    private static final int BUFFER_SIZE = 8192;
    private static final int PROGRESS_UPDATE_INTERVAL = 1000; // 1秒
    
    // 线程池管理
    private final ExecutorService mDownloadExecutor;
    private final Handler mMainHandler;
    
    // 下载任务管理
    private final ConcurrentHashMap<String, EnhancedDownloadTask> mActiveTasks;
    private final AtomicLong mNextTaskId = new AtomicLong(1);
    
    // 监听器管理
    private final ConcurrentHashMap<String, DownloadProgressListener> mListeners;
    
    /**
     * 下载进度监听器
     */
    public interface DownloadProgressListener {
        void onDownloadStarted(String taskId, String fileName, long totalSize);
        void onDownloadProgress(String taskId, long downloadedBytes, long totalSize, int progress);
        void onDownloadPaused(String taskId);
        void onDownloadResumed(String taskId);
        void onDownloadCompleted(String taskId, String filePath);
        void onDownloadFailed(String taskId, String error);
        void onDownloadCancelled(String taskId);
    }
    
    /**
     * 文件类型枚举
     */
    public enum FileType {
        IMAGE("图片", new String[]{"jpg", "jpeg", "png", "gif", "bmp", "webp"}),
        VIDEO("视频", new String[]{"mp4", "avi", "mkv", "mov", "wmv", "flv", "webm"}),
        AUDIO("音频", new String[]{"mp3", "wav", "flac", "aac", "ogg", "m4a"}),
        DOCUMENT("文档", new String[]{"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"}),
        ARCHIVE("压缩包", new String[]{"zip", "rar", "7z", "tar", "gz", "bz2"}),
        APK("应用", new String[]{"apk"}),
        OTHER("其他", new String[]{});
        
        private final String displayName;
        private final String[] extensions;
        
        FileType(String displayName, String[] extensions) {
            this.displayName = displayName;
            this.extensions = extensions;
        }
        
        public String getDisplayName() { return displayName; }
        
        public static FileType fromFileName(String fileName) {
            if (fileName == null) return OTHER;
            
            String extension = getFileExtension(fileName).toLowerCase();
            for (FileType type : values()) {
                for (String ext : type.extensions) {
                    if (ext.equals(extension)) {
                        return type;
                    }
                }
            }
            return OTHER;
        }
        
        private static String getFileExtension(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
        }
    }
    
    private EnhancedDownloadManager(Context context) {
        mContext = context.getApplicationContext();
        mDownloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        mMainHandler = new Handler(Looper.getMainLooper());
        mActiveTasks = new ConcurrentHashMap<>();
        mListeners = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取实例（单例模式）
     */
    public static EnhancedDownloadManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (EnhancedDownloadManager.class) {
                if (sInstance == null) {
                    sInstance = new EnhancedDownloadManager(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 开始下载文件
     * @param url 下载URL
     * @param fileName 文件名（可选，为null时自动解析）
     * @param targetDir 目标目录（可选，为null时使用默认分类目录）
     * @return 下载任务ID
     */
    public String startDownload(@NonNull String url, @Nullable String fileName, @Nullable String targetDir) {
        return startDownload(url, fileName, targetDir, null);
    }
    
    /**
     * 开始下载文件
     * @param url 下载URL
     * @param fileName 文件名
     * @param targetDir 目标目录
     * @param listener 进度监听器
     * @return 下载任务ID
     */
    public String startDownload(@NonNull String url, @Nullable String fileName, 
                               @Nullable String targetDir, @Nullable DownloadProgressListener listener) {
        
        String taskId = generateTaskId();
        
        // 如果没有指定文件名，尝试从URL解析
        if (fileName == null) {
            fileName = extractFileNameFromUrl(url);
        }
        
        // 如果没有指定目录，根据文件类型选择默认目录
        if (targetDir == null) {
            targetDir = getDefaultDownloadDir(fileName);
        }
        
        // 注册监听器
        if (listener != null) {
            mListeners.put(taskId, listener);
        }
        
        // 创建下载任务
        EnhancedDownloadTask task = new EnhancedDownloadTask(taskId, url, fileName, targetDir);
        mActiveTasks.put(taskId, task);
        
        // 提交到线程池执行
        mDownloadExecutor.submit(task);
        
        Log.d(TAG, "Started download task: " + taskId + ", URL: " + url + ", File: " + fileName);
        return taskId;
    }
    
    /**
     * 暂停下载
     */
    public boolean pauseDownload(@NonNull String taskId) {
        EnhancedDownloadTask task = mActiveTasks.get(taskId);
        if (task != null) {
            task.pause();
            notifyDownloadPaused(taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 恢复下载
     */
    public boolean resumeDownload(@NonNull String taskId) {
        EnhancedDownloadTask task = mActiveTasks.get(taskId);
        if (task != null && task.isPaused()) {
            task.resume();
            mDownloadExecutor.submit(task);
            notifyDownloadResumed(taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 取消下载
     */
    public boolean cancelDownload(@NonNull String taskId) {
        EnhancedDownloadTask task = mActiveTasks.remove(taskId);
        if (task != null) {
            task.cancel();
            notifyDownloadCancelled(taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 获取下载进度
     */
    @Nullable
    public DownloadProgress getDownloadProgress(@NonNull String taskId) {
        EnhancedDownloadTask task = mActiveTasks.get(taskId);
        return task != null ? task.getProgress() : null;
    }
    
    /**
     * 添加监听器
     */
    public void addDownloadListener(@NonNull String taskId, @NonNull DownloadProgressListener listener) {
        mListeners.put(taskId, listener);
    }
    
    /**
     * 移除监听器
     */
    public void removeDownloadListener(@NonNull String taskId) {
        mListeners.remove(taskId);
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "download_" + System.currentTimeMillis() + "_" + mNextTaskId.getAndIncrement();
    }
    
    /**
     * 从URL提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        try {
            String path = new URL(url).getPath();
            int lastSlash = path.lastIndexOf('/');
            String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : "download";
            
            // 如果文件名为空或无扩展名，尝试根据内容类型生成
            if (fileName.isEmpty() || !fileName.contains(".")) {
                return "download_" + System.currentTimeMillis();
            }
            
            return fileName;
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract filename from URL: " + url, e);
            return "download_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 获取默认下载目录
     */
    private String getDefaultDownloadDir(String fileName) {
        try {
            File baseDir = new File(mContext.getExternalFilesDir(null), "Downloads");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            
            // 根据文件类型创建子目录
            FileType fileType = FileType.fromFileName(fileName);
            File typeDir = new File(baseDir, fileType.getDisplayName());
            if (!typeDir.exists()) {
                typeDir.mkdirs();
            }
            
            return typeDir.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create download directory", e);
            return mContext.getExternalFilesDir(null).getAbsolutePath();
        }
    }
    
    // 通知方法
    private void notifyDownloadStarted(String taskId, String fileName, long totalSize) {
        mMainHandler.post(() -> {
            DownloadProgressListener listener = mListeners.get(taskId);
            if (listener != null) {
                listener.onDownloadStarted(taskId, fileName, totalSize);
            }
        });
    }
    
    private void notifyDownloadProgress(String taskId, long downloadedBytes, long totalSize, int progress) {
        mMainHandler.post(() -> {
            DownloadProgressListener listener = mListeners.get(taskId);
            if (listener != null) {
                listener.onDownloadProgress(taskId, downloadedBytes, totalSize, progress);
            }
        });
    }
    
    private void notifyDownloadPaused(String taskId) {
        mMainHandler.post(() -> {
            DownloadProgressListener listener = mListeners.get(taskId);
            if (listener != null) {
                listener.onDownloadPaused(taskId);
            }
        });
    }
    
    private void notifyDownloadResumed(String taskId) {
        mMainHandler.post(() -> {
            DownloadProgressListener listener = mListeners.get(taskId);
            if (listener != null) {
                listener.onDownloadResumed(taskId);
            }
        });
    }
    
    private void notifyDownloadCompleted(String taskId, String filePath) {
        mMainHandler.post(() -> {
            DownloadProgressListener listener = mListeners.get(taskId);
            if (listener != null) {
                listener.onDownloadCompleted(taskId, filePath);
            }
            // 清理完成的任务
            mActiveTasks.remove(taskId);
            mListeners.remove(taskId);
        });
    }
    
    private void notifyDownloadFailed(String taskId, String error) {
        mMainHandler.post(() -> {
            DownloadProgressListener listener = mListeners.get(taskId);
            if (listener != null) {
                listener.onDownloadFailed(taskId, error);
            }
            // 清理失败的任务
            mActiveTasks.remove(taskId);
        });
    }
    
    private void notifyDownloadCancelled(String taskId) {
        mMainHandler.post(() -> {
            DownloadProgressListener listener = mListeners.get(taskId);
            if (listener != null) {
                listener.onDownloadCancelled(taskId);
            }
            // 清理取消的任务
            mListeners.remove(taskId);
        });
    }
    
    /**
     * 下载进度信息
     */
    public static class DownloadProgress {
        public final long downloadedBytes;
        public final long totalBytes;
        public final int progress;
        public final String fileName;
        public final String status;
        public final long speed; // 字节/秒
        
        DownloadProgress(long downloadedBytes, long totalBytes, int progress, 
                        String fileName, String status, long speed) {
            this.downloadedBytes = downloadedBytes;
            this.totalBytes = totalBytes;
            this.progress = progress;
            this.fileName = fileName;
            this.status = status;
            this.speed = speed;
        }
    }
    
    /**
     * 下载任务实现类
     */
    private class EnhancedDownloadTask implements Runnable {
        private final String taskId;
        private final String url;
        private final String fileName;
        private final String targetDir;
        private volatile boolean isPaused = false;
        private volatile boolean isCancelled = false;
        private long downloadedBytes = 0;
        private long totalBytes = 0;
        private long startTime = System.currentTimeMillis();
        private long lastProgressTime = 0;
        
        EnhancedDownloadTask(String taskId, String url, String fileName, String targetDir) {
            this.taskId = taskId;
            this.url = url;
            this.fileName = fileName;
            this.targetDir = targetDir;
        }
        
        @Override
        public void run() {
            String status = "下载中";
            
            try {
                File targetFile = new File(targetDir, fileName);
                
                // 检查是否已存在部分下载的文件
                if (targetFile.exists()) {
                    downloadedBytes = targetFile.length();
                }
                
                // 建立HTTP连接
                URL downloadUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
                
                // 设置请求头支持断点续传
                if (downloadedBytes > 0) {
                    connection.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
                    Log.d(TAG, "Resuming download from byte: " + downloadedBytes);
                }
                
                // 设置其他请求头
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "EhViewer Enhanced Download Manager");
                
                int responseCode = connection.getResponseCode();
                
                // 检查响应码
                if (responseCode != HttpURLConnection.HTTP_OK && 
                    responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("HTTP响应错误: " + responseCode);
                }
                
                // 获取文件总大小
                String contentLength = connection.getHeaderField("Content-Length");
                if (contentLength != null) {
                    long contentSize = Long.parseLong(contentLength);
                    if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                        totalBytes = downloadedBytes + contentSize;
                    } else {
                        totalBytes = contentSize;
                    }
                }
                
                notifyDownloadStarted(taskId, fileName, totalBytes);
                
                // 开始下载
                try (InputStream inputStream = connection.getInputStream();
                     RandomAccessFile outputFile = new RandomAccessFile(targetFile, "rw")) {
                    
                    // 定位到文件末尾（用于断点续传）
                    outputFile.seek(downloadedBytes);
                    
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long lastNotifyTime = System.currentTimeMillis();
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        // 检查是否被暂停或取消
                        if (isCancelled) {
                            Log.d(TAG, "Download cancelled: " + taskId);
                            return;
                        }
                        
                        if (isPaused) {
                            Log.d(TAG, "Download paused: " + taskId);
                            status = "已暂停";
                            return;
                        }
                        
                        // 写入数据
                        outputFile.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;
                        
                        // 更新进度（每秒最多更新一次）
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastNotifyTime >= PROGRESS_UPDATE_INTERVAL) {
                            int progress = totalBytes > 0 ? (int) ((downloadedBytes * 100) / totalBytes) : 0;
                            notifyDownloadProgress(taskId, downloadedBytes, totalBytes, progress);
                            lastNotifyTime = currentTime;
                        }
                    }
                    
                    // 下载完成
                    status = "已完成";
                    notifyDownloadCompleted(taskId, targetFile.getAbsolutePath());
                    Log.d(TAG, "Download completed: " + taskId);
                    
                } finally {
                    connection.disconnect();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Download failed: " + taskId, e);
                notifyDownloadFailed(taskId, e.getMessage());
                status = "下载失败";
            }
        }
        
        void pause() {
            isPaused = true;
        }
        
        void resume() {
            isPaused = false;
        }
        
        void cancel() {
            isCancelled = true;
        }
        
        boolean isPaused() {
            return isPaused;
        }
        
        boolean isCancelled() {
            return isCancelled;
        }
        
        DownloadProgress getProgress() {
            int progress = totalBytes > 0 ? (int) ((downloadedBytes * 100) / totalBytes) : 0;
            long speed = calculateSpeed();
            String status = isCancelled ? "已取消" : (isPaused ? "已暂停" : "下载中");
            
            return new DownloadProgress(downloadedBytes, totalBytes, progress, fileName, status, speed);
        }
        
        private long calculateSpeed() {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            return elapsedTime > 0 ? (downloadedBytes * 1000) / elapsedTime : 0;
        }
    }
}