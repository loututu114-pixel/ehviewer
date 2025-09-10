package com.hippo.ehviewer.ui.model;

/**
 * 文件列表项数据模型
 */
public class FileItem {
    public String name;
    public String path;
    public boolean isDirectory;
    public boolean isParent; // 是否为返回上级目录的特殊项
    public long size;
    public long lastModified;
    public boolean canRead;
    public boolean canWrite;

    public FileItem() {
        this.isDirectory = false;
        this.isParent = false;
        this.size = 0;
        this.lastModified = 0;
        this.canRead = false;
        this.canWrite = false;
    }

    public String getDisplaySize() {
        if (isDirectory) {
            return "目录";
        } else if (size == 0) {
            return "0 B";
        } else {
            return formatFileSize(size);
        }
    }

    public String getFileExtension() {
        if (isDirectory || name == null) {
            return "";
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}