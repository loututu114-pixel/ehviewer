/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.ui.FileViewerActivity;
import com.hippo.ehviewer.ui.MediaPlayerActivity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用文件打开器 - 统一处理各种文件类型的打开
 */
public class UniversalFileOpener {

    private static final String TAG = "UniversalFileOpener";
    private static final String PREFS_NAME = "file_opener_prefs";

    // 文件类型映射
    private static final Map<String, FileType> MIME_TYPE_MAP = new HashMap<>();

    static {
        // 文档文件
        MIME_TYPE_MAP.put("application/pdf", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("application/msword", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("application/vnd.ms-excel", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("application/vnd.ms-powerpoint", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("text/plain", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("text/html", FileType.DOCUMENT);
        MIME_TYPE_MAP.put("application/rtf", FileType.DOCUMENT);

        // 视频文件
        MIME_TYPE_MAP.put("video/mp4", FileType.VIDEO);
        MIME_TYPE_MAP.put("video/webm", FileType.VIDEO);
        MIME_TYPE_MAP.put("video/ogg", FileType.VIDEO);
        MIME_TYPE_MAP.put("video/avi", FileType.VIDEO);
        MIME_TYPE_MAP.put("video/quicktime", FileType.VIDEO);
        MIME_TYPE_MAP.put("video/x-ms-wmv", FileType.VIDEO);
        MIME_TYPE_MAP.put("video/x-flv", FileType.VIDEO);
        MIME_TYPE_MAP.put("video/x-matroska", FileType.VIDEO);
        MIME_TYPE_MAP.put("video/*", FileType.VIDEO);

        // 音频文件
        MIME_TYPE_MAP.put("audio/mpeg", FileType.AUDIO);
        MIME_TYPE_MAP.put("audio/wav", FileType.AUDIO);
        MIME_TYPE_MAP.put("audio/ogg", FileType.AUDIO);
        MIME_TYPE_MAP.put("audio/aac", FileType.AUDIO);
        MIME_TYPE_MAP.put("audio/mp4", FileType.AUDIO);
        MIME_TYPE_MAP.put("audio/flac", FileType.AUDIO);
        MIME_TYPE_MAP.put("audio/*", FileType.AUDIO);

        // 图片文件
        MIME_TYPE_MAP.put("image/jpeg", FileType.IMAGE);
        MIME_TYPE_MAP.put("image/png", FileType.IMAGE);
        MIME_TYPE_MAP.put("image/gif", FileType.IMAGE);
        MIME_TYPE_MAP.put("image/bmp", FileType.IMAGE);
        MIME_TYPE_MAP.put("image/webp", FileType.IMAGE);
        MIME_TYPE_MAP.put("image/svg+xml", FileType.IMAGE);
        MIME_TYPE_MAP.put("image/*", FileType.IMAGE);

        // 压缩文件
        MIME_TYPE_MAP.put("application/zip", FileType.ARCHIVE);
        MIME_TYPE_MAP.put("application/x-zip-compressed", FileType.ARCHIVE);
        MIME_TYPE_MAP.put("application/rar", FileType.ARCHIVE);
        MIME_TYPE_MAP.put("application/x-rar-compressed", FileType.ARCHIVE);
        MIME_TYPE_MAP.put("application/7z", FileType.ARCHIVE);
        MIME_TYPE_MAP.put("application/x-7z-compressed", FileType.ARCHIVE);

        // 应用文件
        MIME_TYPE_MAP.put("application/vnd.android.package-archive", FileType.APK);
    }

    /**
     * 文件类型枚举
     */
    public enum FileType {
        DOCUMENT("文档"),
        VIDEO("视频"),
        AUDIO("音频"),
        IMAGE("图片"),
        ARCHIVE("压缩包"),
        APK("安装包"),
        OTHER("其他");

        private final String displayName;

        FileType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 打开方式枚举
     */
    public enum OpenMethod {
        SYSTEM_DEFAULT("系统默认"),
        INTERNAL_VIEWER("EhViewer查看器"),
        EXTERNAL_APP("外部应用"),
        DOWNLOAD_ONLY("仅下载");

        private final String displayName;

        OpenMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 打开文件
     */
    public static boolean openFile(@NonNull Context context, @NonNull Uri fileUri, String mimeType, String fileName) {
        if (mimeType == null) {
            mimeType = getMimeTypeFromUri(context, fileUri);
        }

        FileType fileType = getFileType(mimeType);
        OpenMethod openMethod = getPreferredOpenMethod(context, fileType);

        android.util.Log.d(TAG, "Opening file: " + fileName + ", type: " + fileType + ", method: " + openMethod);

        switch (openMethod) {
            case INTERNAL_VIEWER:
                return openWithInternalViewer(context, fileUri, mimeType, fileName, fileType);
            case EXTERNAL_APP:
                return openWithExternalApp(context, fileUri, mimeType);
            case DOWNLOAD_ONLY:
                Toast.makeText(context, "文件已下载到本地", Toast.LENGTH_SHORT).show();
                return true;
            case SYSTEM_DEFAULT:
            default:
                return openWithSystemDefault(context, fileUri, mimeType);
        }
    }

    /**
     * 使用EhViewer内部查看器打开
     */
    private static boolean openWithInternalViewer(@NonNull Context context, @NonNull Uri fileUri,
                                                 String mimeType, String fileName, FileType fileType) {
        try {
            switch (fileType) {
                case DOCUMENT:
                    // 使用FileViewerActivity打开文档
                    FileViewerActivity.startFileViewer(context, fileUri, mimeType);
                    return true;

                case VIDEO:
                case AUDIO:
                    // 使用MediaPlayerActivity播放媒体文件
                    MediaPlayerActivity.startMediaPlayer(context, fileUri, mimeType, fileName);
                    return true;

                case IMAGE:
                    // 使用系统图片查看器或EhViewer的图片查看器
                    return openImageWithSystem(context, fileUri, mimeType);

                case ARCHIVE:
                    // 压缩文件使用系统应用打开
                    return openWithSystemDefault(context, fileUri, mimeType);

                case APK:
                    // APK文件使用系统安装器
                    return installApk(context, fileUri);

                default:
                    // 其他文件尝试用FileViewerActivity
                    FileViewerActivity.startFileViewer(context, fileUri, mimeType);
                    return true;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to open with internal viewer", e);
            // 如果内部查看器失败，尝试外部应用
            return openWithExternalApp(context, fileUri, mimeType);
        }
    }

    /**
     * 使用外部应用打开
     */
    private static boolean openWithExternalApp(@NonNull Context context, @NonNull Uri fileUri, String mimeType) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No external app found for type: " + mimeType, e);
            Toast.makeText(context, "未找到可以打开此文件的应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 使用系统默认应用打开
     */
    private static boolean openWithSystemDefault(@NonNull Context context, @NonNull Uri fileUri, String mimeType) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(Intent.createChooser(intent, "选择打开方式"));
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No app found for type: " + mimeType, e);
            Toast.makeText(context, "无法打开此文件类型", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 使用系统应用打开图片
     */
    private static boolean openImageWithSystem(@NonNull Context context, @NonNull Uri fileUri, String mimeType) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No image viewer found", e);
            // 如果没有图片查看器，尝试通用方式
            return openWithSystemDefault(context, fileUri, mimeType);
        }
    }

    /**
     * 安装APK文件
     */
    private static boolean installApk(@NonNull Context context, @NonNull Uri fileUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No package installer found", e);
            Toast.makeText(context, "未找到应用安装器", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 获取文件类型
     */
    private static FileType getFileType(String mimeType) {
        if (mimeType == null) return FileType.OTHER;

        FileType fileType = MIME_TYPE_MAP.get(mimeType);
        if (fileType != null) {
            return fileType;
        }

        // 检查MIME类型前缀
        if (mimeType.startsWith("video/")) return FileType.VIDEO;
        if (mimeType.startsWith("audio/")) return FileType.AUDIO;
        if (mimeType.startsWith("image/")) return FileType.IMAGE;
        if (mimeType.startsWith("text/")) return FileType.DOCUMENT;

        return FileType.OTHER;
    }

    /**
     * 获取首选打开方式
     */
    private static OpenMethod getPreferredOpenMethod(@NonNull Context context, FileType fileType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String methodStr = prefs.getString("open_method_" + fileType.name(), null);

        if (methodStr != null) {
            try {
                return OpenMethod.valueOf(methodStr);
            } catch (IllegalArgumentException e) {
                android.util.Log.w(TAG, "Invalid open method: " + methodStr);
            }
        }

        // 默认打开方式
        switch (fileType) {
            case DOCUMENT:
                return OpenMethod.INTERNAL_VIEWER;
            case VIDEO:
            case AUDIO:
                return OpenMethod.INTERNAL_VIEWER;
            case IMAGE:
                return OpenMethod.SYSTEM_DEFAULT;
            case ARCHIVE:
            case APK:
                return OpenMethod.SYSTEM_DEFAULT;
            default:
                return OpenMethod.SYSTEM_DEFAULT;
        }
    }

    /**
     * 设置首选打开方式
     */
    public static void setPreferredOpenMethod(@NonNull Context context, FileType fileType, OpenMethod method) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("open_method_" + fileType.name(), method.name()).apply();
    }

    /**
     * 获取MIME类型
     */
    private static String getMimeTypeFromUri(@NonNull Context context, @NonNull Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);

        if (mimeType == null) {
            // 从文件扩展名推断MIME类型
            String extension = getFileExtension(uri);
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }
        }

        return mimeType;
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0 && lastDot < path.length() - 1) {
                return path.substring(lastDot + 1);
            }
        }
        return null;
    }

    /**
     * 从文件路径获取MIME类型
     */
    public static String getMimeTypeFromPath(String filePath) {
        if (filePath == null) return null;

        File file = new File(filePath);
        String extension = getFileExtension(Uri.fromFile(file));

        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }

        return null;
    }

    /**
     * 检查文件是否可以被EhViewer处理
     */
    public static boolean canHandleFile(String mimeType) {
        if (mimeType == null) return false;

        FileType fileType = getFileType(mimeType);
        return fileType != FileType.OTHER;
    }

    /**
     * 获取文件类型的显示名称
     */
    public static String getFileTypeDisplayName(String mimeType) {
        return getFileType(mimeType).getDisplayName();
    }
}
