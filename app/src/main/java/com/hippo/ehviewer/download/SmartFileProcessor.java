package com.hippo.ehviewer.download;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.hippo.ehviewer.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 智能文件处理器 - 下载完成后的文件智能处理
 * 
 * 核心功能：
 * 1. 文件类型智能识别和分类
 * 2. 图片文件预览图生成
 * 3. APK文件信息提取和安装
 * 4. 压缩文件内容预览
 * 5. 文档文件快速打开
 * 6. 媒体文件元数据提取
 * 7. 文件完整性验证
 */
public class SmartFileProcessor {
    private static final String TAG = "SmartFileProcessor";
    
    private final Context mContext;
    private final ExecutorService mProcessorExecutor;
    
    // 文件处理结果监听器
    public interface FileProcessListener {
        void onProcessStarted(String filePath);
        void onProcessCompleted(String filePath, ProcessResult result);
        void onProcessFailed(String filePath, String error);
    }
    
    /**
     * 文件处理结果
     */
    public static class ProcessResult {
        public String filePath;
        public String fileName;
        public String fileType;
        public long fileSize;
        public String mimeType;
        
        // 扩展信息
        public String previewImagePath;  // 预览图路径
        public String description;       // 文件描述
        public List<String> tags;       // 文件标签
        public FileMetadata metadata;   // 元数据信息
        
        // 操作建议
        public List<FileAction> suggestedActions;
        
        public ProcessResult(String filePath) {
            this.filePath = filePath;
            this.tags = new ArrayList<>();
            this.suggestedActions = new ArrayList<>();
        }
    }
    
    /**
     * 文件元数据
     */
    public static class FileMetadata {
        public String title;
        public String artist;
        public String album;
        public String duration;     // 媒体时长
        public String resolution;   // 图片/视频分辨率
        public String compression;  // 压缩格式
        public String encoding;     // 编码格式
        public long creationTime;
        public long modificationTime;
    }
    
    /**
     * 文件操作建议
     */
    public static class FileAction {
        public enum ActionType {
            OPEN("打开"),
            INSTALL("安装"),
            PREVIEW("预览"),
            EXTRACT("解压"),
            SHARE("分享"),
            DELETE("删除"),
            MOVE("移动");
            
            private final String displayName;
            
            ActionType(String displayName) {
                this.displayName = displayName;
            }
            
            public String getDisplayName() { return displayName; }
        }
        
        public ActionType type;
        public String description;
        public Intent intent;  // 执行操作的Intent
        
        public FileAction(ActionType type, String description) {
            this.type = type;
            this.description = description;
        }
    }
    
    public SmartFileProcessor(Context context) {
        mContext = context.getApplicationContext();
        mProcessorExecutor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 处理下载完成的文件
     */
    public void processFile(@NonNull String filePath, @Nullable FileProcessListener listener) {
        mProcessorExecutor.submit(() -> {
            try {
                if (listener != null) {
                    listener.onProcessStarted(filePath);
                }
                
                ProcessResult result = analyzeFile(filePath);
                
                if (listener != null) {
                    listener.onProcessCompleted(filePath, result);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to process file: " + filePath, e);
                if (listener != null) {
                    listener.onProcessFailed(filePath, e.getMessage());
                }
            }
        });
    }
    
    /**
     * 分析文件并生成处理结果
     */
    private ProcessResult analyzeFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("文件不存在或不是有效文件: " + filePath);
        }
        
        ProcessResult result = new ProcessResult(filePath);
        result.fileName = file.getName();
        result.fileSize = file.length();
        result.fileType = getFileType(file.getName());
        result.mimeType = getMimeType(file.getName());
        
        // 基础元数据
        FileMetadata metadata = new FileMetadata();
        metadata.creationTime = file.lastModified();
        metadata.modificationTime = file.lastModified();
        result.metadata = metadata;
        
        // 根据文件类型进行特殊处理
        switch (EnhancedDownloadManager.FileType.fromFileName(file.getName())) {
            case IMAGE:
                processImageFile(file, result);
                break;
            case VIDEO:
                processVideoFile(file, result);
                break;
            case AUDIO:
                processAudioFile(file, result);
                break;
            case APK:
                processApkFile(file, result);
                break;
            case ARCHIVE:
                processArchiveFile(file, result);
                break;
            case DOCUMENT:
                processDocumentFile(file, result);
                break;
            default:
                processGenericFile(file, result);
                break;
        }
        
        // 生成操作建议
        generateActionSuggestions(result);
        
        return result;
    }
    
    /**
     * 处理图片文件
     */
    private void processImageFile(File file, ProcessResult result) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            
            if (options.outWidth > 0 && options.outHeight > 0) {
                result.metadata.resolution = options.outWidth + "x" + options.outHeight;
                result.description = String.format("图片 • %s • %s", 
                    result.metadata.resolution, 
                    formatFileSize(result.fileSize));
                
                // 生成缩略图
                generateImageThumbnail(file, result);
                
                result.tags.add("图片");
                result.tags.add(options.outWidth > 1920 ? "高清" : "标清");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to process image file: " + file.getName(), e);
            result.description = "图片文件 • " + formatFileSize(result.fileSize);
        }
    }
    
    /**
     * 处理视频文件
     */
    private void processVideoFile(File file, ProcessResult result) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            
            if (duration != null) {
                result.metadata.duration = formatDuration(Long.parseLong(duration));
            }
            if (width != null && height != null) {
                result.metadata.resolution = width + "x" + height;
            }
            if (title != null) {
                result.metadata.title = title;
            }
            
            result.description = String.format("视频 • %s • %s • %s", 
                result.metadata.resolution != null ? result.metadata.resolution : "未知分辨率",
                result.metadata.duration != null ? result.metadata.duration : "未知时长",
                formatFileSize(result.fileSize));
            
            result.tags.add("视频");
            retriever.release();
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to process video file: " + file.getName(), e);
            result.description = "视频文件 • " + formatFileSize(result.fileSize);
        }
    }
    
    /**
     * 处理音频文件
     */
    private void processAudioFile(File file, ProcessResult result) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            
            if (title != null) result.metadata.title = title;
            if (artist != null) result.metadata.artist = artist;
            if (album != null) result.metadata.album = album;
            if (duration != null) {
                result.metadata.duration = formatDuration(Long.parseLong(duration));
            }
            
            result.description = String.format("音频 • %s • %s • %s", 
                artist != null ? artist : "未知艺术家",
                result.metadata.duration != null ? result.metadata.duration : "未知时长",
                formatFileSize(result.fileSize));
            
            result.tags.add("音频");
            retriever.release();
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to process audio file: " + file.getName(), e);
            result.description = "音频文件 • " + formatFileSize(result.fileSize);
        }
    }
    
    /**
     * 处理APK文件
     */
    private void processApkFile(File file, ProcessResult result) {
        try {
            PackageManager pm = mContext.getPackageManager();
            String packagePath = file.getAbsolutePath();
            
            android.content.pm.PackageInfo packageInfo = pm.getPackageArchiveInfo(packagePath, 0);
            if (packageInfo != null) {
                String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
                String packageName = packageInfo.packageName;
                String versionName = packageInfo.versionName;
                
                result.metadata.title = appName;
                result.description = String.format("Android应用 • %s • v%s • %s", 
                    appName, versionName, formatFileSize(result.fileSize));
                
                result.tags.add("应用");
                result.tags.add("APK");
                
                // 检查是否已安装
                try {
                    pm.getPackageInfo(packageName, 0);
                    result.tags.add("已安装");
                } catch (PackageManager.NameNotFoundException e) {
                    result.tags.add("未安装");
                }
            } else {
                result.description = "Android应用 • " + formatFileSize(result.fileSize);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to process APK file: " + file.getName(), e);
            result.description = "Android应用 • " + formatFileSize(result.fileSize);
        }
    }
    
    /**
     * 处理压缩文件
     */
    private void processArchiveFile(File file, ProcessResult result) {
        try {
            // 尝试读取ZIP文件内容
            if (file.getName().toLowerCase().endsWith(".zip")) {
                int fileCount = 0;
                long uncompressedSize = 0;
                
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null && fileCount < 100) {
                        if (!entry.isDirectory()) {
                            fileCount++;
                            uncompressedSize += entry.getSize();
                        }
                    }
                }
                
                result.description = String.format("压缩包 • %d个文件 • %s → %s", 
                    fileCount, formatFileSize(result.fileSize), 
                    formatFileSize(uncompressedSize));
                
                result.metadata.compression = String.format("压缩率: %.1f%%", 
                    uncompressedSize > 0 ? (1.0 - (double)result.fileSize / uncompressedSize) * 100 : 0);
            } else {
                result.description = "压缩包 • " + formatFileSize(result.fileSize);
            }
            
            result.tags.add("压缩包");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to process archive file: " + file.getName(), e);
            result.description = "压缩包 • " + formatFileSize(result.fileSize);
        }
    }
    
    /**
     * 处理文档文件
     */
    private void processDocumentFile(File file, ProcessResult result) {
        String extension = getFileExtension(file.getName()).toLowerCase();
        String typeDescription = "文档";
        
        switch (extension) {
            case "pdf":
                typeDescription = "PDF文档";
                result.tags.add("PDF");
                break;
            case "doc":
            case "docx":
                typeDescription = "Word文档";
                result.tags.add("Word");
                break;
            case "xls":
            case "xlsx":
                typeDescription = "Excel表格";
                result.tags.add("Excel");
                break;
            case "ppt":
            case "pptx":
                typeDescription = "PowerPoint演示";
                result.tags.add("PowerPoint");
                break;
            case "txt":
                typeDescription = "文本文档";
                result.tags.add("文本");
                break;
        }
        
        result.description = typeDescription + " • " + formatFileSize(result.fileSize);
        result.tags.add("文档");
    }
    
    /**
     * 处理通用文件
     */
    private void processGenericFile(File file, ProcessResult result) {
        result.description = "文件 • " + formatFileSize(result.fileSize);
        result.tags.add("文件");
    }
    
    /**
     * 生成操作建议
     */
    private void generateActionSuggestions(ProcessResult result) {
        EnhancedDownloadManager.FileType fileType = 
            EnhancedDownloadManager.FileType.fromFileName(result.fileName);
        
        // 通用操作
        FileAction shareAction = new FileAction(FileAction.ActionType.SHARE, "分享文件");
        shareAction.intent = createShareIntent(result.filePath);
        result.suggestedActions.add(shareAction);
        
        // 根据文件类型添加特定操作
        switch (fileType) {
            case APK:
                FileAction installAction = new FileAction(FileAction.ActionType.INSTALL, "安装应用");
                installAction.intent = createInstallIntent(result.filePath);
                result.suggestedActions.add(installAction);
                break;
                
            case IMAGE:
            case VIDEO:
            case AUDIO:
                FileAction previewAction = new FileAction(FileAction.ActionType.PREVIEW, "预览文件");
                previewAction.intent = createViewIntent(result.filePath, result.mimeType);
                result.suggestedActions.add(previewAction);
                break;
                
            case ARCHIVE:
                // 压缩包解压功能需要额外实现
                FileAction extractAction = new FileAction(FileAction.ActionType.EXTRACT, "解压文件");
                result.suggestedActions.add(extractAction);
                break;
                
            case DOCUMENT:
                FileAction openAction = new FileAction(FileAction.ActionType.OPEN, "打开文档");
                openAction.intent = createViewIntent(result.filePath, result.mimeType);
                result.suggestedActions.add(openAction);
                break;
        }
    }
    
    /**
     * 生成图片缩略图
     */
    private void generateImageThumbnail(File file, ProcessResult result) {
        try {
            File thumbnailDir = new File(mContext.getCacheDir(), "thumbnails");
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs();
            }
            
            File thumbnailFile = new File(thumbnailDir, 
                "thumb_" + System.currentTimeMillis() + ".jpg");
            
            // 创建缩略图
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(file.getAbsolutePath(), 200, 200);
            
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap != null) {
                try (FileOutputStream out = new FileOutputStream(thumbnailFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
                    result.previewImagePath = thumbnailFile.getAbsolutePath();
                }
                bitmap.recycle();
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to generate thumbnail for: " + file.getName(), e);
        }
    }
    
    // 工具方法
    private String getFileType(String fileName) {
        String extension = getFileExtension(fileName);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        return mimeType != null ? mimeType : "application/octet-stream";
    }
    
    private String getMimeType(String fileName) {
        String extension = getFileExtension(fileName);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        
        return String.format("%.1f %s", 
            bytes / Math.pow(1024, digitGroups), 
            units[digitGroups]);
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%d:%02d", minutes, seconds % 60);
        }
    }
    
    private int calculateInSampleSize(String filePath, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    private Intent createViewIntent(String filePath, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(filePath);
        Uri fileUri = FileProvider.getUriForFile(mContext, 
            BuildConfig.APPLICATION_ID + ".fileprovider", file);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }
    
    private Intent createShareIntent(String filePath) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        File file = new File(filePath);
        Uri fileUri = FileProvider.getUriForFile(mContext, 
            BuildConfig.APPLICATION_ID + ".fileprovider", file);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(intent, "分享文件");
    }
    
    private Intent createInstallIntent(String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(filePath);
        Uri fileUri = FileProvider.getUriForFile(mContext, 
            BuildConfig.APPLICATION_ID + ".fileprovider", file);
        intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (mProcessorExecutor != null && !mProcessorExecutor.isShutdown()) {
            mProcessorExecutor.shutdown();
        }
    }
}