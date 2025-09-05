package com.hippo.ehviewer.util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * WebView页面截图工具类
 * 提供多种截图方式：可见区域、全屏截图、长截图等
 */
public class WebViewScreenshotUtil {

    private static final String TAG = "WebViewScreenshotUtil";

    /**
     * 截图类型枚举
     */
    public enum ScreenshotType {
        VISIBLE_AREA,   // 可见区域
        FULL_PAGE,      // 全页截图
        CUSTOM_SIZE     // 自定义尺寸
    }

    /**
     * 截图回调接口
     */
    public interface ScreenshotCallback {
        void onScreenshotTaken(Bitmap bitmap, Uri imageUri);
        void onScreenshotError(String error);
    }

    /**
     * 截取WebView可见区域
     */
    public static void captureVisibleArea(@NonNull WebView webView, @NonNull Activity activity,
                                        @NonNull ScreenshotCallback callback) {
        try {
            // 启用绘图缓存
            webView.setDrawingCacheEnabled(true);
            webView.buildDrawingCache();

            Bitmap bitmap = Bitmap.createBitmap(webView.getDrawingCache());
            webView.setDrawingCacheEnabled(false);

            if (bitmap != null) {
                saveBitmapToGallery(activity, bitmap, "webview_screenshot_visible", callback);
            } else {
                callback.onScreenshotError("无法创建截图");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error capturing visible area", e);
            callback.onScreenshotError("截图失败: " + e.getMessage());
        }
    }

    /**
     * 截取WebView全屏
     */
    public static void captureFullScreenshot(@NonNull WebView webView, @NonNull Activity activity,
                                           @NonNull ScreenshotCallback callback) {
        try {
            // 创建一个足够大的Bitmap
            int width = webView.getWidth();
            int height = (int) (webView.getContentHeight() * webView.getScale());

            if (width <= 0 || height <= 0) {
                callback.onScreenshotError("WebView尺寸无效");
                return;
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // 绘制WebView内容
            webView.draw(canvas);

            saveBitmapToGallery(activity, bitmap, "webview_screenshot_full", callback);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error capturing full screenshot", e);
            callback.onScreenshotError("全屏截图失败: " + e.getMessage());
        }
    }

    /**
     * 使用系统截图API（推荐方式）
     */
    public static void captureUsingDrawingCache(@NonNull WebView webView, @NonNull Activity activity,
                                              @NonNull ScreenshotCallback callback) {
        try {
            webView.setDrawingCacheEnabled(true);
            webView.setDrawingCacheQuality(WebView.DRAWING_CACHE_QUALITY_HIGH);

            // 等待一帧渲染完成
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Bitmap bitmap = Bitmap.createBitmap(webView.getDrawingCache());
                    webView.setDrawingCacheEnabled(false);

                    if (bitmap != null) {
                        saveBitmapToGallery(activity, bitmap, "webview_screenshot_system", callback);
                    } else {
                        callback.onScreenshotError("系统截图失败");
                    }
                } catch (Exception e) {
                    callback.onScreenshotError("截图处理失败: " + e.getMessage());
                }
            }, 100);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error with system screenshot", e);
            callback.onScreenshotError("系统截图API失败: " + e.getMessage());
        }
    }

    /**
     * 截取指定尺寸的截图
     */
    public static void captureCustomSize(@NonNull WebView webView, @NonNull Activity activity,
                                       int width, int height, @NonNull ScreenshotCallback callback) {
        try {
            if (width <= 0 || height <= 0) {
                callback.onScreenshotError("无效的截图尺寸");
                return;
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // 缩放WebView内容以适应指定尺寸
            canvas.scale((float) width / webView.getWidth(), (float) height / webView.getHeight());
            webView.draw(canvas);

            saveBitmapToGallery(activity, bitmap, "webview_screenshot_custom", callback);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error capturing custom size", e);
            callback.onScreenshotError("自定义尺寸截图失败: " + e.getMessage());
        }
    }

    /**
     * 保存Bitmap到相册
     */
    private static void saveBitmapToGallery(@NonNull Activity activity, @NonNull Bitmap bitmap,
                                          String fileNamePrefix, @NonNull ScreenshotCallback callback) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileNamePrefix + "_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EhViewer");

            Uri uri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                        callback.onScreenshotTaken(bitmap, uri);
                        Toast.makeText(activity, "截图已保存到相册", Toast.LENGTH_SHORT).show();
                        android.util.Log.d(TAG, "Screenshot saved successfully: " + uri);
                    } else {
                        callback.onScreenshotError("无法打开输出流");
                    }
                }
            } else {
                // 降级到文件系统保存
                saveToFileSystem(activity, bitmap, fileNamePrefix, callback);
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error saving to gallery", e);
            // 降级到文件系统保存
            saveToFileSystem(activity, bitmap, fileNamePrefix, callback);
        }
    }

    /**
     * 降级保存到文件系统
     */
    private static void saveToFileSystem(@NonNull Activity activity, @NonNull Bitmap bitmap,
                                       String fileNamePrefix, @NonNull ScreenshotCallback callback) {
        try {
            File picturesDir = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "EhViewer");
            if (!picturesDir.exists()) {
                picturesDir.mkdirs();
            }

            File screenshotFile = new File(picturesDir, fileNamePrefix + "_" + System.currentTimeMillis() + ".png");

            try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                Uri fileUri = Uri.fromFile(screenshotFile);
                callback.onScreenshotTaken(bitmap, fileUri);
                Toast.makeText(activity, "截图已保存: " + screenshotFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                android.util.Log.d(TAG, "Screenshot saved to file: " + screenshotFile.getAbsolutePath());
            }

        } catch (IOException e) {
            android.util.Log.e(TAG, "Error saving to file system", e);
            callback.onScreenshotError("保存截图失败: " + e.getMessage());
        }
    }

    /**
     * 便捷方法：截取可见区域
     */
    public static void takeScreenshot(@NonNull WebView webView, @NonNull Activity activity) {
        takeScreenshot(webView, activity, ScreenshotType.VISIBLE_AREA);
    }

    /**
     * 便捷方法：指定截图类型
     */
    public static void takeScreenshot(@NonNull WebView webView, @NonNull Activity activity,
                                    ScreenshotType type) {
        ScreenshotCallback callback = new ScreenshotCallback() {
            @Override
            public void onScreenshotTaken(Bitmap bitmap, Uri imageUri) {
                android.util.Log.d(TAG, "Screenshot taken successfully: " + imageUri);
            }

            @Override
            public void onScreenshotError(String error) {
                android.util.Log.e(TAG, "Screenshot error: " + error);
                Toast.makeText(activity, "截图失败: " + error, Toast.LENGTH_SHORT).show();
            }
        };

        switch (type) {
            case VISIBLE_AREA:
                captureVisibleArea(webView, activity, callback);
                break;
            case FULL_PAGE:
                captureFullScreenshot(webView, activity, callback);
                break;
            case CUSTOM_SIZE:
                // 默认使用WebView尺寸
                captureCustomSize(webView, activity, webView.getWidth(), webView.getHeight(), callback);
                break;
        }
    }

    /**
     * 清理资源
     */
    public static void cleanup(@NonNull WebView webView) {
        webView.setDrawingCacheEnabled(false);
        webView.destroyDrawingCache();
    }
}
