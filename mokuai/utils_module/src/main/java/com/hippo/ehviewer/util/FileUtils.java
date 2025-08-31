/*
 * EhViewer Utils Module - FileUtils
 * 文件操作工具类
 */

package com.hippo.ehviewer.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * 文件操作工具类
 * 提供常用的文件操作功能
 */
public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    /**
     * 创建文件
     * @param filePath 文件路径
     * @return 是否创建成功
     */
    public static boolean createFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                return file.createNewFile();
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create file: " + filePath, e);
            return false;
        }
    }

    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return 是否创建成功
     */
    public static boolean createDir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }

    /**
     * 删除文件或目录
     * @param path 文件或目录路径
     * @return 是否删除成功
     */
    public static boolean delete(String path) {
        return delete(new File(path));
    }

    /**
     * 删除文件或目录
     * @param file 文件或目录
     * @return 是否删除成功
     */
    public static boolean delete(File file) {
        if (!file.exists()) {
            return true;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    delete(child);
                }
            }
        }

        return file.delete();
    }

    /**
     * 复制文件
     * @param srcPath 源文件路径
     * @param destPath 目标文件路径
     * @return 是否复制成功
     */
    public static boolean copyFile(String srcPath, String destPath) {
        return copyFile(new File(srcPath), new File(destPath));
    }

    /**
     * 复制文件
     * @param srcFile 源文件
     * @param destFile 目标文件
     * @return 是否复制成功
     */
    public static boolean copyFile(File srcFile, File destFile) {
        if (!srcFile.exists()) {
            return false;
        }

        // 确保目标目录存在
        File destDir = destFile.getParentFile();
        if (destDir != null && !destDir.exists()) {
            destDir.mkdirs();
        }

        try (FileInputStream fis = new FileInputStream(srcFile);
             FileOutputStream fos = new FileOutputStream(destFile);
             FileChannel input = fis.getChannel();
             FileChannel output = fos.getChannel()) {

            output.transferFrom(input, 0, input.size());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Failed to copy file", e);
            return false;
        }
    }

    /**
     * 移动文件
     * @param srcPath 源文件路径
     * @param destPath 目标文件路径
     * @return 是否移动成功
     */
    public static boolean moveFile(String srcPath, String destPath) {
        if (copyFile(srcPath, destPath)) {
            return delete(srcPath);
        }
        return false;
    }

    /**
     * 读取文本文件
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readTextFile(String filePath) {
        return readTextFile(new File(filePath));
    }

    /**
     * 读取文本文件
     * @param file 文件
     * @return 文件内容
     */
    public static String readTextFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return new String(data, "UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read text file", e);
            return null;
        }
    }

    /**
     * 写入文本文件
     * @param filePath 文件路径
     * @param content 内容
     * @return 是否写入成功
     */
    public static boolean writeTextFile(String filePath, String content) {
        return writeTextFile(new File(filePath), content);
    }

    /**
     * 写入文本文件
     * @param file 文件
     * @param content 内容
     * @return 是否写入成功
     */
    public static boolean writeTextFile(File file, String content) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes("UTF-8"));
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write text file", e);
            return false;
        }
    }

    /**
     * 获取文件大小
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    /**
     * 获取目录大小
     * @param dirPath 目录路径
     * @return 目录大小（字节）
     */
    public static long getDirSize(String dirPath) {
        return getDirSize(new File(dirPath));
    }

    /**
     * 获取目录大小
     * @param dir 目录
     * @return 目录大小（字节）
     */
    public static long getDirSize(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }

        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getDirSize(file);
                }
            }
        }
        return size;
    }

    /**
     * 格式化文件大小
     * @param size 字节数
     * @return 格式化的文件大小字符串
     */
    public static String formatFileSize(long size) {
        if (size <= 0) {
            return "0 B";
        }

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 扩展名
     */
    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * 获取文件名（不含扩展名）
     * @param fileName 文件名
     * @return 文件名
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }

        return fileName;
    }

    /**
     * 检查是否为图片文件
     * @param filePath 文件路径
     * @return 是否为图片文件
     */
    public static boolean isImageFile(String filePath) {
        String extension = getExtension(filePath);
        return extension.equals("jpg") || extension.equals("jpeg") ||
               extension.equals("png") || extension.equals("gif") ||
               extension.equals("bmp") || extension.equals("webp");
    }

    /**
     * 获取应用的缓存目录
     * @param context 上下文
     * @return 缓存目录
     */
    public static File getAppCacheDir(Context context) {
        return context.getCacheDir();
    }

    /**
     * 获取应用的文件目录
     * @param context 上下文
     * @return 文件目录
     */
    public static File getAppFilesDir(Context context) {
        return context.getFilesDir();
    }

    /**
     * 获取外部存储目录
     * @return 外部存储目录
     */
    public static File getExternalStorageDir() {
        return Environment.getExternalStorageDirectory();
    }

    /**
     * 检查外部存储是否可用
     * @return 是否可用
     */
    public static boolean isExternalStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}
