package com.hippo.ehviewer.util;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.MimeTypeMap;
import androidx.appcompat.app.AlertDialog;
import com.hippo.ehviewer.ui.YCWebViewActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 增强的浏览器管理器 - 处理默认浏览器设置和文件关联
 * 支持所有文件类型的打开和处理
 */
public class EnhancedBrowserManager {
    private static final String TAG = "EnhancedBrowserManager";
    private static EnhancedBrowserManager instance;
    private final Context context;
    private final PackageManager packageManager;
    
    // 支持的文件类型列表（全面覆盖）
    private static final List<String> SUPPORTED_SCHEMES = Arrays.asList(
        "http", "https", "ftp", "file", "content", "data", "about", "javascript"
    );
    
    // 支持的MIME类型
    private static final List<String> SUPPORTED_MIME_TYPES = Arrays.asList(
        "text/html", "text/plain", "text/xml", "text/css", "text/javascript",
        "application/xhtml+xml", "application/xml", "application/json",
        "application/pdf", "application/zip", "application/x-rar-compressed",
        "image/*", "video/*", "audio/*"
    );
    
    // 支持的文件扩展名
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
        // 网页文件
        ".html", ".htm", ".xhtml", ".xml", ".mhtml", ".mht",
        // 文本文件
        ".txt", ".log", ".md", ".json", ".js", ".css", ".java", ".py", ".cpp", ".c",
        // 文档文件
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".odt",
        // 图片文件
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".ico",
        // 视频文件
        ".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm", ".m4v",
        // 音频文件
        ".mp3", ".wav", ".flac", ".aac", ".ogg", ".wma", ".m4a",
        // 压缩文件
        ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2",
        // 应用文件
        ".apk", ".aab"
    );
    
    private EnhancedBrowserManager(Context context) {
        this.context = context.getApplicationContext();
        this.packageManager = context.getPackageManager();
    }
    
    public static EnhancedBrowserManager getInstance(Context context) {
        if (instance == null) {
            synchronized (EnhancedBrowserManager.class) {
                if (instance == null) {
                    instance = new EnhancedBrowserManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 请求设置为默认浏览器（全方位）
     */
    public void requestDefaultBrowser(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用RoleManager
            requestBrowserRole(activity);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7-9 使用默认应用设置
            requestDefaultAppSettings(activity);
        } else {
            // Android 6及以下，注册Intent Filter
            showManualSetupGuide(activity);
        }
        
        // 同时注册文件关联
        registerFileAssociations();
    }
    
    /**
     * 使用RoleManager请求浏览器角色（Android 10+）
     */
    private void requestBrowserRole(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER);
                    activity.startActivityForResult(intent, 1003);
                }
            }
        }
    }
    
    /**
     * 打开默认应用设置（Android 7-9）
     */
    private void requestDefaultAppSettings(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("设为默认浏览器")
            .setMessage("请在设置中将EhViewer设为默认浏览器，这样您可以：\n\n" +
                "• 直接打开所有网页链接\n" +
                "• 处理各种文件类型\n" +
                "• 获得更流畅的浏览体验")
            .setPositiveButton("去设置", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                activity.startActivity(intent);
            })
            .setNegativeButton("稍后", null)
            .show();
    }
    
    /**
     * 显示手动设置指南
     */
    private void showManualSetupGuide(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("设为默认浏览器")
            .setMessage("请按以下步骤设置：\n\n" +
                "1. 打开系统设置\n" +
                "2. 进入\"应用管理\"\n" +
                "3. 找到\"默认应用\"或\"默认打开\"\n" +
                "4. 选择\"浏览器\"\n" +
                "5. 选择\"EhViewer\"")
            .setPositiveButton("打开设置", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                activity.startActivity(intent);
            })
            .setNegativeButton("我知道了", null)
            .show();
    }
    
    /**
     * 注册文件关联
     */
    private void registerFileAssociations() {
        // 动态注册支持的文件类型
        ComponentName componentName = new ComponentName(context, YCWebViewActivity.class);
        
        // 启用所有文件处理组件
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        );
    }
    
    /**
     * 检查是否是默认浏览器
     */
    public boolean isDefaultBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 
            PackageManager.MATCH_DEFAULT_ONLY);
        
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return context.getPackageName().equals(resolveInfo.activityInfo.packageName);
        }
        return false;
    }
    
    /**
     * 打开任意文件
     */
    public void openFile(Context context, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        
        Uri uri = Uri.fromFile(file);
        String mimeType = getMimeType(filePath);
        
        Intent intent = new Intent(context, YCWebViewActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // 根据文件类型添加特殊处理标记
        if (isImageFile(filePath)) {
            intent.putExtra("is_image", true);
            intent.putExtra("enable_zoom", true);
        } else if (isVideoFile(filePath)) {
            intent.putExtra("is_video", true);
            intent.putExtra("enable_fullscreen", true);
        } else if (isAudioFile(filePath)) {
            intent.putExtra("is_audio", true);
        } else if (isPdfFile(filePath)) {
            intent.putExtra("is_pdf", true);
        }
        
        context.startActivity(intent);
    }
    
    /**
     * 打开URI
     */
    public void openUri(Context context, Uri uri) {
        Intent intent = new Intent(context, YCWebViewActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // 添加特殊处理
        String scheme = uri.getScheme();
        if ("file".equals(scheme) || "content".equals(scheme)) {
            String path = uri.getPath();
            if (path != null) {
                if (isImageFile(path)) {
                    intent.putExtra("is_image", true);
                } else if (isVideoFile(path)) {
                    intent.putExtra("is_video", true);
                }
            }
        }
        
        context.startActivity(intent);
    }
    
    /**
     * 获取MIME类型
     */
    private String getMimeType(String filePath) {
        String extension = getFileExtension(filePath);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        
        if (mimeType == null) {
            // 根据扩展名推测MIME类型
            if (isImageFile(filePath)) {
                mimeType = "image/*";
            } else if (isVideoFile(filePath)) {
                mimeType = "video/*";
            } else if (isAudioFile(filePath)) {
                mimeType = "audio/*";
            } else if (filePath.endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else if (filePath.endsWith(".apk")) {
                mimeType = "application/vnd.android.package-archive";
            } else {
                mimeType = "text/plain";
            }
        }
        
        return mimeType;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 检查是否是图片文件
     */
    private boolean isImageFile(String filePath) {
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".ico"};
        String lower = filePath.toLowerCase();
        for (String ext : imageExtensions) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否是视频文件
     */
    private boolean isVideoFile(String filePath) {
        String[] videoExtensions = {".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".3gp"};
        String lower = filePath.toLowerCase();
        for (String ext : videoExtensions) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否是音频文件
     */
    private boolean isAudioFile(String filePath) {
        String[] audioExtensions = {".mp3", ".wav", ".flac", ".aac", ".ogg", ".wma", ".m4a", ".opus"};
        String lower = filePath.toLowerCase();
        for (String ext : audioExtensions) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否是PDF文件
     */
    private boolean isPdfFile(String filePath) {
        return filePath.toLowerCase().endsWith(".pdf");
    }
    
    /**
     * 获取支持状态报告
     */
    public String getSupportStatusReport() {
        StringBuilder report = new StringBuilder();
        report.append("浏览器功能状态：\n\n");
        
        report.append("默认浏览器: ").append(isDefaultBrowser() ? "✓" : "✗").append("\n");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null) {
                report.append("浏览器角色: ")
                    .append(roleManager.isRoleHeld(RoleManager.ROLE_BROWSER) ? "✓" : "✗")
                    .append("\n");
            }
        }
        
        report.append("\n支持的协议：\n");
        for (String scheme : SUPPORTED_SCHEMES) {
            report.append("• ").append(scheme).append("\n");
        }
        
        report.append("\n支持的文件类型：\n");
        report.append("• 网页文件 (HTML, XML, MHTML)\n");
        report.append("• 文档文件 (PDF, DOC, XLS, PPT)\n");
        report.append("• 图片文件 (JPG, PNG, GIF, WEBP)\n");
        report.append("• 视频文件 (MP4, AVI, MKV, WEBM)\n");
        report.append("• 音频文件 (MP3, WAV, FLAC, AAC)\n");
        report.append("• 压缩文件 (ZIP, RAR, 7Z)\n");
        report.append("• 应用文件 (APK, AAB)\n");
        
        return report.toString();
    }
}