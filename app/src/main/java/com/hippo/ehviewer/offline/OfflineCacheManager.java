package com.hippo.ehviewer.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.webkit.URLUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 离线缓存管理器
 * 负责管理图片、视频、漫画等内容的离线缓存
 */
public class OfflineCacheManager {
    private static final String TAG = "OfflineCacheManager";
    
    private static OfflineCacheManager instance;
    private Context context;
    private File cacheDir;
    private File offlineDir;
    private SharedPreferences prefs;
    private ExecutorService downloadExecutor;
    private ConcurrentHashMap<String, DownloadTask> activeDownloads;
    
    // 缓存配置
    private static final long MAX_CACHE_SIZE = 500 * 1024 * 1024; // 500MB
    private static final long MAX_OFFLINE_SIZE = 2 * 1024 * 1024 * 1024L; // 2GB
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    
    // 缓存类型
    public enum CacheType {
        IMAGE("images", 100 * 1024 * 1024L), // 100MB
        VIDEO("videos", 500 * 1024 * 1024L), // 500MB
        MANGA("manga", 200 * 1024 * 1024L),  // 200MB
        NOVEL("novels", 50 * 1024 * 1024L),  // 50MB
        TEMP("temp", 50 * 1024 * 1024L);     // 50MB
        
        private final String folderName;
        private final long maxSize;
        
        CacheType(String folderName, long maxSize) {
            this.folderName = folderName;
            this.maxSize = maxSize;
        }
    }
    
    // 缓存策略
    public enum CacheStrategy {
        WIFI_ONLY,      // 仅WiFi下缓存
        ALWAYS,          // 总是缓存
        SMART,           // 智能缓存（根据网络状态和文件大小）
        MANUAL           // 手动缓存
    }
    
    private CacheStrategy currentStrategy = CacheStrategy.SMART;
    private boolean isOfflineMode = false;
    
    private OfflineCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("offline_cache", Context.MODE_PRIVATE);
        this.activeDownloads = new ConcurrentHashMap<>();
        this.downloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        
        initializeCacheDirectories();
    }
    
    public static synchronized OfflineCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineCacheManager(context);
        }
        return instance;
    }
    
    /**
     * 初始化缓存目录
     */
    private void initializeCacheDirectories() {
        // 内部缓存目录（自动管理）
        cacheDir = new File(context.getCacheDir(), "offline");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        // 外部离线目录（用户可管理）
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            offlineDir = new File(context.getExternalFilesDir(null), "offline");
            if (!offlineDir.exists()) {
                offlineDir.mkdirs();
            }
        }
        
        // 为每种类型创建子目录
        for (CacheType type : CacheType.values()) {
            File typeDir = new File(cacheDir, type.folderName);
            if (!typeDir.exists()) {
                typeDir.mkdirs();
            }
            
            if (offlineDir != null) {
                File offlineTypeDir = new File(offlineDir, type.folderName);
                if (!offlineTypeDir.exists()) {
                    offlineTypeDir.mkdirs();
                }
            }
        }
    }
    
    /**
     * 缓存图片
     */
    public void cacheImage(String url, CacheCallback callback) {
        if (isFileCached(url, CacheType.IMAGE)) {
            if (callback != null) {
                callback.onCached(getCachedFile(url, CacheType.IMAGE));
            }
            return;
        }
        
        downloadAndCache(url, CacheType.IMAGE, callback);
    }
    
    /**
     * 批量缓存图片（用于漫画）
     */
    public void cacheMangaChapter(String chapterId, List<String> imageUrls, BatchCacheCallback callback) {
        MangaChapterCache chapterCache = new MangaChapterCache(chapterId, imageUrls, callback);
        chapterCache.startCaching();
    }
    
    /**
     * 缓存视频
     */
    public void cacheVideo(String url, CacheCallback callback) {
        if (isFileCached(url, CacheType.VIDEO)) {
            if (callback != null) {
                callback.onCached(getCachedFile(url, CacheType.VIDEO));
            }
            return;
        }
        
        // 检查文件大小和网络状态
        if (shouldCache(url, CacheType.VIDEO)) {
            downloadAndCache(url, CacheType.VIDEO, callback);
        }
    }
    
    /**
     * 获取缓存文件
     */
    public File getCachedFile(String url, CacheType type) {
        String fileName = generateFileName(url);
        File typeDir = new File(isOfflineMode ? offlineDir : cacheDir, type.folderName);
        File file = new File(typeDir, fileName);
        
        if (file.exists()) {
            return file;
        }
        
        // 检查另一个目录
        typeDir = new File(isOfflineMode ? cacheDir : offlineDir, type.folderName);
        file = new File(typeDir, fileName);
        
        return file.exists() ? file : null;
    }
    
    /**
     * 检查文件是否已缓存
     */
    public boolean isFileCached(String url, CacheType type) {
        return getCachedFile(url, type) != null;
    }
    
    /**
     * 下载并缓存文件
     */
    private void downloadAndCache(String url, CacheType type, CacheCallback callback) {
        String key = generateFileName(url);
        
        // 检查是否已在下载中
        if (activeDownloads.containsKey(key)) {
            return;
        }
        
        DownloadTask task = new DownloadTask(url, type, callback);
        activeDownloads.put(key, task);
        downloadExecutor.execute(task);
    }
    
    /**
     * 下载任务
     */
    private class DownloadTask implements Runnable {
        private String url;
        private CacheType type;
        private CacheCallback callback;
        
        DownloadTask(String url, CacheType type, CacheCallback callback) {
            this.url = url;
            this.type = type;
            this.callback = callback;
        }
        
        @Override
        public void run() {
            File file = null;
            try {
                file = downloadFile(url, type);
                
                if (callback != null) {
                    callback.onCached(file);
                }
            } catch (Exception e) {
                Log.e(TAG, "Download failed: " + url, e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            } finally {
                activeDownloads.remove(generateFileName(url));
            }
        }
    }
    
    /**
     * 实际下载文件
     */
    private File downloadFile(String urlString, CacheType type) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        
        String fileName = generateFileName(urlString);
        File typeDir = new File(isOfflineMode ? offlineDir : cacheDir, type.folderName);
        File file = new File(typeDir, fileName);
        File tempFile = new File(typeDir, fileName + ".tmp");
        
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream output = new FileOutputStream(tempFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;
            long contentLength = connection.getContentLength();
            
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // 通知下载进度
                if (contentLength > 0) {
                    int progress = (int) ((totalBytes * 100) / contentLength);
                    notifyProgress(urlString, progress);
                }
            }
        }
        
        // 重命名临时文件
        if (tempFile.renameTo(file)) {
            // 更新缓存统计
            updateCacheStats(type, file.length());
            return file;
        } else {
            throw new IOException("Failed to rename temp file");
        }
    }
    
    /**
     * 漫画章节缓存
     */
    private class MangaChapterCache {
        private String chapterId;
        private List<String> imageUrls;
        private BatchCacheCallback callback;
        private int totalImages;
        private int cachedImages;
        private List<File> cachedFiles;
        
        MangaChapterCache(String chapterId, List<String> imageUrls, BatchCacheCallback callback) {
            this.chapterId = chapterId;
            this.imageUrls = imageUrls;
            this.callback = callback;
            this.totalImages = imageUrls.size();
            this.cachedImages = 0;
            this.cachedFiles = new ArrayList<>();
        }
        
        void startCaching() {
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                final int index = i;
                
                cacheImage(url, new CacheCallback() {
                    @Override
                    public void onCached(File file) {
                        synchronized (MangaChapterCache.this) {
                            cachedImages++;
                            cachedFiles.add(file);
                            
                            if (callback != null) {
                                callback.onProgress(cachedImages, totalImages);
                            }
                            
                            if (cachedImages == totalImages) {
                                if (callback != null) {
                                    callback.onComplete(cachedFiles);
                                }
                                
                                // 保存章节信息
                                saveMangaChapterInfo(chapterId, cachedFiles);
                            }
                        }
                    }
                    
                    @Override
                    public void onError(String message) {
                        if (callback != null) {
                            callback.onError(index, message);
                        }
                    }
                });
            }
        }
    }
    
    /**
     * 清理缓存
     */
    public void clearCache(CacheType type) {
        File typeDir = new File(cacheDir, type.folderName);
        deleteRecursive(typeDir);
        typeDir.mkdirs();
        
        updateCacheStats(type, 0);
    }
    
    /**
     * 清理所有缓存
     */
    public void clearAllCache() {
        for (CacheType type : CacheType.values()) {
            clearCache(type);
        }
    }
    
    /**
     * 获取缓存大小
     */
    public long getCacheSize(CacheType type) {
        File typeDir = new File(cacheDir, type.folderName);
        return getDirectorySize(typeDir);
    }
    
    /**
     * 获取总缓存大小
     */
    public long getTotalCacheSize() {
        long total = 0;
        for (CacheType type : CacheType.values()) {
            total += getCacheSize(type);
        }
        return total;
    }
    
    /**
     * 智能清理缓存（基于LRU）
     */
    public void smartCleanCache() {
        for (CacheType type : CacheType.values()) {
            long size = getCacheSize(type);
            if (size > type.maxSize) {
                cleanOldestFiles(type, size - type.maxSize * 8 / 10); // 清理到80%
            }
        }
    }
    
    private void cleanOldestFiles(CacheType type, long bytesToDelete) {
        File typeDir = new File(cacheDir, type.folderName);
        File[] files = typeDir.listFiles();
        
        if (files == null || files.length == 0) return;
        
        // 按最后修改时间排序
        java.util.Arrays.sort(files, (f1, f2) -> 
            Long.compare(f1.lastModified(), f2.lastModified()));
        
        long deleted = 0;
        for (File file : files) {
            if (deleted >= bytesToDelete) break;
            
            deleted += file.length();
            file.delete();
        }
    }
    
    // 工具方法
    private String generateFileName(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            
            // 添加文件扩展名
            String extension = getFileExtension(url);
            if (extension != null) {
                sb.append(".").append(extension);
            }
            
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(url.hashCode());
        }
    }
    
    private String getFileExtension(String url) {
        String extension = URLUtil.guessFileName(url, null, null);
        int lastDot = extension.lastIndexOf('.');
        if (lastDot > 0) {
            return extension.substring(lastDot + 1);
        }
        return null;
    }
    
    private long getDirectorySize(File dir) {
        long size = 0;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    }
                }
            }
        }
        return size;
    }
    
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
    
    private boolean shouldCache(String url, CacheType type) {
        switch (currentStrategy) {
            case WIFI_ONLY:
                return isWifiConnected();
            case ALWAYS:
                return true;
            case SMART:
                // 根据文件类型和网络状态智能判断
                if (type == CacheType.IMAGE) {
                    return true; // 图片总是缓存
                } else if (type == CacheType.VIDEO) {
                    return isWifiConnected(); // 视频仅WiFi缓存
                }
                return true;
            case MANUAL:
                return false;
            default:
                return true;
        }
    }
    
    private boolean isWifiConnected() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected() && 
               networkInfo.getType() == android.net.ConnectivityManager.TYPE_WIFI;
    }
    
    private void notifyProgress(String url, int progress) {
        // 可以通过EventBus或其他方式通知进度
    }
    
    private void updateCacheStats(CacheType type, long size) {
        prefs.edit().putLong(type.folderName + "_size", size).apply();
    }
    
    private void saveMangaChapterInfo(String chapterId, List<File> files) {
        // 保存章节缓存信息到数据库或SharedPreferences
    }
    
    // 设置和获取方法
    public void setCacheStrategy(CacheStrategy strategy) {
        this.currentStrategy = strategy;
    }
    
    public CacheStrategy getCacheStrategy() {
        return currentStrategy;
    }
    
    public void setOfflineMode(boolean offline) {
        this.isOfflineMode = offline;
    }
    
    public boolean isOfflineMode() {
        return isOfflineMode;
    }
    
    // 回调接口
    public interface CacheCallback {
        void onCached(File file);
        void onError(String message);
    }
    
    public interface BatchCacheCallback {
        void onProgress(int cached, int total);
        void onComplete(List<File> files);
        void onError(int index, String message);
    }
}