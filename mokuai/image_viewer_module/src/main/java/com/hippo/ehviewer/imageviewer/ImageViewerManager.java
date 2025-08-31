package com.hippo.ehviewer.imageviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图片查看器管理器 - 统一管理图片查看功能
 * 支持本地图片、网络图片、手势缩放、旋转等高级功能
 * 基于EnhancedImageViewerActivity的完整功能实现
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ImageViewerManager {

    private static final String TAG = "ImageViewerManager";
    private static volatile ImageViewerManager instance;

    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private ImageViewerCallback callback;
    private ImageViewerConfig config;

    // 手势检测
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private PointF startPoint = new PointF();
    private PointF midPoint = new PointF();
    private float currentScale = 1.0f;
    private int mode = NONE;

    // 配置常量
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 10.0f;

    // 图片源类型
    public enum ImageSource {
        LOCAL_FILE,    // 本地文件
        NETWORK_URL,   // 网络URL
        CONTENT_URI,   // Content URI
        ASSET_FILE     // 资源文件
    }

    // 图片配置
    public static class ImageViewerConfig {
        private float minScale = MIN_SCALE;
        private float maxScale = MAX_SCALE;
        private boolean enableZoom = true;
        private boolean enableDrag = true;
        private boolean enableRotation = true;
        private boolean enableDoubleTap = true;
        private int maxImageSize = 2048; // 最大图片尺寸（像素）
        private boolean useHardwareAcceleration = true;
        private boolean enableProgressBar = true;
        private boolean enableControlPanel = true;

        public static class Builder {
            private final ImageViewerConfig config = new ImageViewerConfig();

            public Builder setMinScale(float minScale) {
                config.minScale = minScale;
                return this;
            }

            public Builder setMaxScale(float maxScale) {
                config.maxScale = maxScale;
                return this;
            }

            public Builder enableZoom(boolean enable) {
                config.enableZoom = enable;
                return this;
            }

            public Builder enableDrag(boolean enable) {
                config.enableDrag = enable;
                return this;
            }

            public Builder enableRotation(boolean enable) {
                config.enableRotation = enable;
                return this;
            }

            public Builder enableDoubleTap(boolean enable) {
                config.enableDoubleTap = enable;
                return this;
            }

            public Builder setMaxImageSize(int size) {
                config.maxImageSize = size;
                return this;
            }

            public Builder useHardwareAcceleration(boolean use) {
                config.useHardwareAcceleration = use;
                return this;
            }

            public Builder enableProgressBar(boolean enable) {
                config.enableProgressBar = enable;
                return this;
            }

            public Builder enableControlPanel(boolean enable) {
                config.enableControlPanel = enable;
                return this;
            }

            public ImageViewerConfig build() {
                return config;
            }
        }
    }

    // 图片查看器回调接口
    public interface ImageViewerCallback {
        void onImageLoaded(Bitmap bitmap, String source);
        void onImageLoadError(String error, String source);
        void onScaleChanged(float scale);
        void onImageClicked();
        void onImageDoubleTapped();
        void onImageRotated(float degrees);
    }

    /**
     * 获取单例实例
     */
    public static ImageViewerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ImageViewerManager.class) {
                if (instance == null) {
                    instance = new ImageViewerManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ImageViewerManager(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.config = new ImageViewerConfig();
    }

    /**
     * 设置配置
     */
    public void setConfig(ImageViewerConfig config) {
        this.config = config != null ? config : new ImageViewerConfig();
    }

    /**
     * 设置回调
     */
    public void setCallback(ImageViewerCallback callback) {
        this.callback = callback;
    }

    /**
     * 加载本地图片
     */
    public void loadLocalImage(String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.isEmpty()) {
            notifyError("图片路径为空", imagePath);
            return;
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            notifyError("图片文件不存在", imagePath);
            return;
        }

        executorService.execute(() -> {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                // 获取图片尺寸
                BitmapFactory.decodeFile(imagePath, options);

                // 计算缩放比例
                options.inSampleSize = calculateInSampleSize(options, config.maxImageSize, config.maxImageSize);
                options.inJustDecodeBounds = false;

                // 加载图片
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

                if (bitmap != null) {
                    mainHandler.post(() -> {
                        displayImage(bitmap, imageView, imagePath);
                        notifySuccess(bitmap, imagePath);
                    });
                } else {
                    notifyError("图片加载失败", imagePath);
                }

            } catch (Exception e) {
                notifyError("图片加载异常: " + e.getMessage(), imagePath);
            }
        });
    }

    /**
     * 加载网络图片
     */
    public void loadNetworkImage(String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            notifyError("图片URL为空", imageUrl);
            return;
        }

        executorService.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    if (bitmap != null) {
                        mainHandler.post(() -> {
                            displayImage(bitmap, imageView, imageUrl);
                            notifySuccess(bitmap, imageUrl);
                        });
                    } else {
                        notifyError("图片解码失败", imageUrl);
                    }

                    inputStream.close();
                } else {
                    notifyError("HTTP错误: " + responseCode, imageUrl);
                }

                connection.disconnect();

            } catch (Exception e) {
                notifyError("网络图片加载异常: " + e.getMessage(), imageUrl);
            }
        });
    }

    /**
     * 加载Content URI图片
     */
    public void loadContentImage(Uri imageUri, ImageView imageView) {
        if (imageUri == null) {
            notifyError("图片URI为空", imageUri != null ? imageUri.toString() : null);
            return;
        }

        executorService.execute(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    if (bitmap != null) {
                        mainHandler.post(() -> {
                            displayImage(bitmap, imageView, imageUri.toString());
                            notifySuccess(bitmap, imageUri.toString());
                        });
                    } else {
                        notifyError("图片解码失败", imageUri.toString());
                    }

                    inputStream.close();
                } else {
                    notifyError("无法打开图片URI", imageUri.toString());
                }

            } catch (Exception e) {
                notifyError("Content图片加载异常: " + e.getMessage(), imageUri.toString());
            }
        });
    }

    /**
     * 计算采样率
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 显示图片
     */
    private void displayImage(Bitmap bitmap, ImageView imageView, String source) {
        if (imageView != null && bitmap != null) {
            imageView.setImageBitmap(bitmap);

            // 适配图片到屏幕
            fitImageToScreen(bitmap, imageView);
        }
    }

    /**
     * 适配图片到屏幕
     */
    private void fitImageToScreen(Bitmap bitmap, ImageView imageView) {
        if (bitmap == null || imageView == null) {
            return;
        }

        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();

        if (viewWidth == 0 || viewHeight == 0) {
            // 如果视图还没有测量完成，延迟执行
            imageView.post(() -> fitImageToScreen(bitmap, imageView));
            return;
        }

        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();

        float scaleX = viewWidth / bitmapWidth;
        float scaleY = viewHeight / bitmapHeight;
        float scale = Math.min(scaleX, scaleY);

        // 应用缩放
        matrix.setScale(scale, scale);
        imageView.setImageMatrix(matrix);
        currentScale = scale;
    }

    /**
     * 初始化手势检测器
     */
    private void initGestureDetectors(ImageView imageView) {
        if (!config.enableZoom && !config.enableDrag && !config.enableDoubleTap) {
            return;
        }

        // 缩放手势检测器
        if (config.enableZoom) {
            scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener(imageView));
        }

        // 普通手势检测器
        gestureDetector = new GestureDetector(context, new GestureListener(imageView));

        // 设置触摸监听器
        imageView.setOnTouchListener((v, event) -> {
            boolean handled = false;

            if (gestureDetector != null) {
                handled = gestureDetector.onTouchEvent(event);
            }

            if (scaleGestureDetector != null) {
                handled = scaleGestureDetector.onTouchEvent(event) || handled;
            }

            if (config.enableDrag) {
                handled = handleDragGesture(event, imageView) || handled;
            }

            return handled;
        });
    }

    /**
     * 处理拖拽手势
     */
    private boolean handleDragGesture(MotionEvent event, ImageView imageView) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                startPoint.set(event.getX(), event.getY());
                mode = DRAG;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    float dx = event.getX() - startPoint.x;
                    float dy = event.getY() - startPoint.y;

                    matrix.set(savedMatrix);
                    matrix.postTranslate(dx, dy);
                    imageView.setImageMatrix(matrix);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                return true;
        }

        return false;
    }

    /**
     * 缩放监听器
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private final ImageView imageView;

        ScaleListener(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!config.enableZoom) {
                return false;
            }

            float scaleFactor = detector.getScaleFactor();
            float newScale = currentScale * scaleFactor;

            // 限制缩放范围
            newScale = Math.max(config.minScale, Math.min(config.maxScale, newScale));

            if (newScale != currentScale) {
                matrix.set(savedMatrix);
                matrix.postScale(newScale, newScale, detector.getFocusX(), detector.getFocusY());
                imageView.setImageMatrix(matrix);
                currentScale = newScale;

                // 通知缩放变化
                if (callback != null) {
                    callback.onScaleChanged(currentScale);
                }
            }

            return true;
        }
    }

    /**
     * 手势监听器
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final ImageView imageView;

        GestureListener(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!config.enableDoubleTap) {
                return false;
            }

            // 双击重置缩放
            matrix.reset();
            currentScale = 1.0f;
            fitImageToScreen(((android.graphics.drawable.BitmapDrawable) imageView.getDrawable()).getBitmap(), imageView);

            if (callback != null) {
                callback.onImageDoubleTapped();
            }

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (callback != null) {
                callback.onImageClicked();
            }
            return true;
        }
    }

    /**
     * 旋转图片
     */
    public void rotateImage(float degrees, ImageView imageView) {
        if (!config.enableRotation) {
            return;
        }

        matrix.postRotate(degrees);
        imageView.setImageMatrix(matrix);

        if (callback != null) {
            callback.onImageRotated(degrees);
        }
    }

    /**
     * 缩放图片
     */
    public void zoomImage(float scaleFactor, ImageView imageView) {
        if (!config.enableZoom) {
            return;
        }

        float newScale = currentScale * scaleFactor;
        newScale = Math.max(config.minScale, Math.min(config.maxScale, newScale));

        if (newScale != currentScale) {
            matrix.postScale(scaleFactor, scaleFactor);
            imageView.setImageMatrix(matrix);
            currentScale = newScale;

            if (callback != null) {
                callback.onScaleChanged(currentScale);
            }
        }
    }

    /**
     * 重置变换
     */
    public void resetTransform(ImageView imageView) {
        matrix.reset();
        currentScale = 1.0f;
        imageView.setImageMatrix(matrix);

        if (callback != null) {
            callback.onScaleChanged(currentScale);
        }
    }

    /**
     * 获取当前缩放比例
     */
    public float getCurrentScale() {
        return currentScale;
    }

    /**
     * 设置中间点
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * 通知成功
     */
    private void notifySuccess(Bitmap bitmap, String source) {
        if (callback != null) {
            mainHandler.post(() -> callback.onImageLoaded(bitmap, source));
        }
    }

    /**
     * 通知错误
     */
    private void notifyError(String error, String source) {
        if (callback != null) {
            mainHandler.post(() -> callback.onImageLoadError(error, source));
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (executorService != null) {
            executorService.shutdown();
        }
        instance = null;
    }
}.0
 * @since 2024-01-01
 */
public class ImageViewerManager {

    private static final String TAG = "ImageViewerManager";
    private static volatile ImageViewerManager instance;

    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private ImageViewerCallback callback;
    private ImageViewerConfig config;
    private ImageViewerCallback callback;

    // 手势和缩放相关
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private PointF startPoint = new PointF();
    private PointF midPoint = new PointF();
    private float currentScale = 1.0f;
    private int mode = NONE;

    // 触摸模式常量
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    // 缩放限制
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 10.0f;

    /**
     * 图片查看器配置类
     */
    public static class ImageViewerConfig {
        private float minScale = MIN_SCALE;
        private float maxScale = MAX_SCALE;
        private boolean enableRotation = true;
        private boolean enableZoom = true;
        private boolean enableDrag = true;
        private boolean enableDoubleTap = true;
        private int maxImageSize = 2048; // 最大图片尺寸（像素）
        private boolean useHardwareAcceleration = true;

        public static class Builder {
            private final ImageViewerConfig config = new ImageViewerConfig();

            public Builder setMinScale(float minScale) {
                config.minScale = minScale;
                return this;
            }

            public Builder setMaxScale(float maxScale) {
                config.maxScale = maxScale;
                return this;
            }

            public Builder enableRotation(boolean enable) {
                config.enableRotation = enable;
                return this;
            }

            public Builder enableZoom(boolean enable) {
                config.enableZoom = enable;
                return this;
            }

            public Builder enableDrag(boolean enable) {
                config.enableDrag = enable;
                return this;
            }

            public Builder enableDoubleTap(boolean enable) {
                config.enableDoubleTap = enable;
                return this;
            }

            public Builder setMaxImageSize(int size) {
                config.maxImageSize = size;
                return this;
            }

            public Builder useHardwareAcceleration(boolean use) {
                config.useHardwareAcceleration = use;
                return this;
            }

            public ImageViewerConfig build() {
                return config;
            }
        }
    }

    /**
     * 图片查看器回调接口
     */
    public interface ImageViewerCallback {
        void onImageLoaded(Bitmap bitmap, String source);
        void onImageLoadError(String error, String source);
        void onScaleChanged(float scale);
        void onImageClicked();
        void onImageDoubleTapped();
        void onImageRotated(float degrees);
    }

    /**
     * 获取单例实例
     *
     * @param context 应用上下文
     * @return ImageViewerManager实例
     */
    public static ImageViewerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ImageViewerManager.class) {
                if (instance == null) {
                    instance = new ImageViewerManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ImageViewerManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.config = new ImageViewerConfig();
    }

    /**
     * 设置配置
     *
     * @param config 图片查看器配置
     */
    public void setConfig(ImageViewerConfig config) {
        this.config = config != null ? config : new ImageViewerConfig();
    }

    /**
     * 设置回调监听器
     *
     * @param callback 回调监听器
     */
    public void setCallback(ImageViewerCallback callback) {
        this.callback = callback;
    }

    /**
     * 加载本地图片
     *
     * @param imagePath 图片文件路径
     * @param imageView 显示图片的ImageView
     */
    public void loadLocalImage(String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.isEmpty()) {
            notifyError("图片路径为空", imagePath);
            return;
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            notifyError("图片文件不存在", imagePath);
            return;
        }

        executorService.execute(() -> {
            try {
                // 采样加载大图片
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, options);

                // 计算采样率
                options.inSampleSize = calculateInSampleSize(options);
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;

                Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

                mainHandler.post(() -> {
                    if (bitmap != null) {
                        displayImage(bitmap, imageView, imagePath);
                    } else {
                        notifyError("图片解码失败", imagePath);
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> notifyError("加载图片失败: " + e.getMessage(), imagePath));
            }
        });
    }

    /**
     * 加载网络图片
     *
     * @param imageUrl 图片URL
     * @param imageView 显示图片的ImageView
     */
    public void loadNetworkImage(String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            notifyError("图片URL为空", imageUrl);
            return;
        }

        executorService.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream inputStream = connection.getInputStream();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                mainHandler.post(() -> {
                    if (bitmap != null) {
                        displayImage(bitmap, imageView, imageUrl);
                    } else {
                        notifyError("网络图片解码失败", imageUrl);
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> notifyError("加载网络图片失败: " + e.getMessage(), imageUrl));
            }
        });
    }

    /**
     * 加载内容URI图片
     *
     * @param imageUri 图片URI
     * @param imageView 显示图片的ImageView
     */
    public void loadContentImage(Uri imageUri, ImageView imageView) {
        if (imageUri == null) {
            notifyError("图片URI为空", null);
            return;
        }

        executorService.execute(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                inputStream = context.getContentResolver().openInputStream(imageUri);
                options.inSampleSize = calculateInSampleSize(options);
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                mainHandler.post(() -> {
                    if (bitmap != null) {
                        displayImage(bitmap, imageView, imageUri.toString());
                    } else {
                        notifyError("内容图片解码失败", imageUri.toString());
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> notifyError("加载内容图片失败: " + e.getMessage(), imageUri.toString()));
            }
        });
    }

    /**
     * 计算图片采样率
     */
    private int calculateInSampleSize(BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > config.maxImageSize || width > config.maxImageSize) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= config.maxImageSize
                    && (halfWidth / inSampleSize) >= config.maxImageSize) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 显示图片
     */
    private void displayImage(Bitmap bitmap, ImageView imageView, String source) {
        imageView.setImageBitmap(bitmap);

        // 设置ImageView的缩放类型
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        // 适应屏幕大小
        fitImageToScreen(bitmap, imageView);

        // 初始化手势检测器
        initGestureDetectors(imageView);

        // 通知加载成功
        if (callback != null) {
            callback.onImageLoaded(bitmap, source);
        }
    }

    /**
     * 适应屏幕大小
     */
    private void fitImageToScreen(Bitmap bitmap, ImageView imageView) {
        if (bitmap == null || imageView == null) return;

        float viewWidth = imageView.getWidth();
        float viewHeight = imageView.getHeight();
        float imageWidth = bitmap.getWidth();
        float imageHeight = bitmap.getHeight();

        if (viewWidth == 0 || viewHeight == 0) {
            // View还未测量完成，使用默认缩放
            matrix.reset();
            matrix.postScale(1.0f, 1.0f);
        } else {
            float scaleX = viewWidth / imageWidth;
            float scaleY = viewHeight / imageHeight;
            float scale = Math.min(scaleX, scaleY);

            matrix.reset();
            matrix.postScale(scale, scale);

            float scaledWidth = imageWidth * scale;
            float scaledHeight = imageHeight * scale;
            float dx = (viewWidth - scaledWidth) / 2;
            float dy = (viewHeight - scaledHeight) / 2;
            matrix.postTranslate(dx, dy);
        }

        currentScale = 1.0f;
        imageView.setImageMatrix(matrix);
    }

    /**
     * 初始化手势检测器
     */
    private void initGestureDetectors(ImageView imageView) {
        // 缩放手势检测器
        if (config.enableZoom) {
            scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener(imageView));
        }

        // 普通手势检测器
        if (config.enableDoubleTap || config.enableDrag) {
            gestureDetector = new GestureDetector(context, new GestureListener(imageView));
        }

        // 设置触摸监听
        imageView.setOnTouchListener((v, event) -> {
            boolean handled = false;

            // 处理缩放手势
            if (scaleGestureDetector != null) {
                handled = scaleGestureDetector.onTouchEvent(event);
            }

            // 处理普通手势
            if (gestureDetector != null) {
                handled = gestureDetector.onTouchEvent(event) || handled;
            }

            // 处理拖拽
            if (config.enableDrag) {
                handled = handleDragGesture(event, imageView) || handled;
            }

            return handled;
        });
    }

    /**
     * 处理拖拽手势
     */
    private boolean handleDragGesture(MotionEvent event, ImageView imageView) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                startPoint.set(event.getX(), event.getY());
                mode = DRAG;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                savedMatrix.set(matrix);
                midPoint(midPoint, event);
                mode = ZOOM;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG && currentScale > 1.0f) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - startPoint.x;
                    float dy = event.getY() - startPoint.y;
                    matrix.postTranslate(dx, dy);
                    imageView.setImageMatrix(matrix);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                return true;
        }
        return false;
    }

    /**
     * 缩放手势监听器
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private final ImageView imageView;

        ScaleListener(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!config.enableZoom) return false;

            float scaleFactor = detector.getScaleFactor();
            float newScale = currentScale * scaleFactor;

            if (newScale >= config.minScale && newScale <= config.maxScale) {
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                currentScale = newScale;
                imageView.setImageMatrix(matrix);

                if (callback != null) {
                    callback.onScaleChanged(currentScale);
                }
            }
            return true;
        }
    }

    /**
     * 普通手势监听器
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final ImageView imageView;

        GestureListener(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!config.enableDoubleTap) return false;

            if (callback != null) {
                callback.onImageDoubleTapped();
            }

            if (currentScale > 1.0f) {
                fitImageToScreen(null, imageView);
            } else {
                matrix.postScale(2.0f, 2.0f, e.getX(), e.getY());
                currentScale *= 2.0f;
                imageView.setImageMatrix(matrix);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (callback != null) {
                callback.onImageClicked();
            }
            return true;
        }
    }

    /**
     * 旋转图片
     *
     * @param degrees 旋转角度
     * @param imageView 图片视图
     */
    public void rotateImage(float degrees, ImageView imageView) {
        if (!config.enableRotation || imageView == null) return;

        matrix.postRotate(degrees, imageView.getWidth() / 2f, imageView.getHeight() / 2f);
        imageView.setImageMatrix(matrix);

        if (callback != null) {
            callback.onImageRotated(degrees);
        }
    }

    /**
     * 缩放图片
     *
     * @param scaleFactor 缩放因子
     * @param imageView 图片视图
     */
    public void zoomImage(float scaleFactor, ImageView imageView) {
        if (!config.enableZoom || imageView == null) return;

        float newScale = currentScale * scaleFactor;
        if (newScale >= config.minScale && newScale <= config.maxScale) {
            matrix.postScale(scaleFactor, scaleFactor,
                imageView.getWidth() / 2f, imageView.getHeight() / 2f);
            currentScale = newScale;
            imageView.setImageMatrix(matrix);

            if (callback != null) {
                callback.onScaleChanged(currentScale);
            }
        }
    }

    /**
     * 获取当前缩放比例
     */
    public float getCurrentScale() {
        return currentScale;
    }

    /**
     * 重置图片变换
     */
    public void resetTransform(ImageView imageView) {
        matrix.reset();
        currentScale = 1.0f;
        imageView.setImageMatrix(matrix);
    }

    /**
     * 计算两点间中点
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * 通知图片加载成功
     */
    private void notifySuccess(Bitmap bitmap, String source) {
        if (callback != null) {
            mainHandler.post(() -> callback.onImageLoaded(bitmap, source));
        }
    }

    /**
     * 通知图片加载错误
     */
    private void notifyError(String error, String source) {
        if (callback != null) {
            mainHandler.post(() -> callback.onImageLoadError(error, source));
        } else {
            // 默认错误处理
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        executorService.shutdown();
        instance = null;
    }
}
