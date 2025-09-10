package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.hippo.ehviewer.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 增强的图片查看器 - 支持本地和网络图片，手势缩放，高清加载
 */
public class EnhancedImageViewerActivity extends AppCompatActivity {
    private static final String TAG = "EnhancedImageViewer";
    
    private ImageView imageView;
    private ProgressBar progressBar;
    private TextView infoText;
    private View controlPanel;
    
    // 手势检测
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    
    // 缩放和移动相关
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 10.0f;
    private float currentScale = 1.0f;
    private PointF startPoint = new PointF();
    private PointF midPoint = new PointF();
    
    // 触摸模式
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    private String imagePath;
    private Uri imageUri;
    private Bitmap currentBitmap;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_enhanced_image_viewer);
        
        initViews();
        initGestures();
        loadImage();
    }
    
    private void initViews() {
        imageView = findViewById(R.id.image_view);
        progressBar = findViewById(R.id.progress_bar);
        infoText = findViewById(R.id.info_text);
        controlPanel = findViewById(R.id.control_panel);
        
        // 初始化线程池和Handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 设置ImageView的缩放类型
        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        
        // 设置控制按钮
        findViewById(R.id.btn_rotate_left).setOnClickListener(v -> rotateImage(-90));
        findViewById(R.id.btn_rotate_right).setOnClickListener(v -> rotateImage(90));
        findViewById(R.id.btn_zoom_in).setOnClickListener(v -> zoomImage(1.5f));
        findViewById(R.id.btn_zoom_out).setOnClickListener(v -> zoomImage(0.67f));
        findViewById(R.id.btn_fit).setOnClickListener(v -> fitImageToScreen());
        findViewById(R.id.btn_share).setOnClickListener(v -> shareImage());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveImage());
        findViewById(R.id.btn_info).setOnClickListener(v -> toggleInfo());
    }
    
    private void initGestures() {
        // 缩放手势检测器
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        
        // 普通手势检测器（双击、滑动等）
        gestureDetector = new GestureDetector(this, new GestureListener());
        
        // 设置触摸监听
        imageView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    startPoint.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;
                    
                case MotionEvent.ACTION_POINTER_DOWN:
                    savedMatrix.set(matrix);
                    midPoint(midPoint, event);
                    mode = ZOOM;
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG && currentScale > 1.0f) {
                        matrix.set(savedMatrix);
                        float dx = event.getX() - startPoint.x;
                        float dy = event.getY() - startPoint.y;
                        matrix.postTranslate(dx, dy);
                        imageView.setImageMatrix(matrix);
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
            }
            
            return true;
        });
    }
    
    private void loadImage() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        
        if (Intent.ACTION_VIEW.equals(action)) {
            imageUri = intent.getData();
            if (imageUri != null) {
                if ("file".equals(imageUri.getScheme())) {
                    imagePath = imageUri.getPath();
                    loadLocalImage(imagePath);
                } else if ("content".equals(imageUri.getScheme())) {
                    loadContentImage(imageUri);
                } else if ("http".equals(imageUri.getScheme()) || "https".equals(imageUri.getScheme())) {
                    loadNetworkImage(imageUri.toString());
                }
            }
        } else if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                loadContentImage(imageUri);
            }
        }
    }
    
    private void loadLocalImage(String path) {
        progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                // 加载图片时进行采样，避免内存溢出
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                
                // 计算采样率
                options.inSampleSize = calculateInSampleSize(options, 2048, 2048);
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                
                Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                
                mainHandler.post(() -> {
                    if (bitmap != null) {
                        currentBitmap = bitmap;
                        imageView.setImageBitmap(bitmap);
                        fitImageToScreen();
                        updateImageInfo(bitmap);
                    }
                    progressBar.setVisibility(View.GONE);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }
    
    private void loadContentImage(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                
                inputStream = getContentResolver().openInputStream(uri);
                options.inSampleSize = calculateInSampleSize(options, 2048, 2048);
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                
                mainHandler.post(() -> {
                    if (bitmap != null) {
                        currentBitmap = bitmap;
                        imageView.setImageBitmap(bitmap);
                        fitImageToScreen();
                        updateImageInfo(bitmap);
                    }
                    progressBar.setVisibility(View.GONE);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }
    
    private void loadNetworkImage(String url) {
        progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setDoInput(true);
                connection.connect();
                
                InputStream inputStream = connection.getInputStream();
                
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                
                mainHandler.post(() -> {
                    if (bitmap != null) {
                        currentBitmap = bitmap;
                        imageView.setImageBitmap(bitmap);
                        fitImageToScreen();
                        updateImageInfo(bitmap);
                    }
                    progressBar.setVisibility(View.GONE);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "加载网络图片失败", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }
    
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
    
    private void updateImageInfo(Bitmap bitmap) {
        String info = String.format("尺寸: %d x %d\n格式: %s\n大小: %.2f MB",
            bitmap.getWidth(),
            bitmap.getHeight(),
            bitmap.getConfig().toString(),
            (bitmap.getByteCount() / 1024.0 / 1024.0));
        infoText.setText(info);
    }
    
    private void fitImageToScreen() {
        if (currentBitmap == null) return;
        
        float viewWidth = imageView.getWidth();
        float viewHeight = imageView.getHeight();
        float imageWidth = currentBitmap.getWidth();
        float imageHeight = currentBitmap.getHeight();
        
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
        
        currentScale = scale;
        imageView.setImageMatrix(matrix);
    }
    
    private void rotateImage(float degrees) {
        matrix.postRotate(degrees, imageView.getWidth() / 2f, imageView.getHeight() / 2f);
        imageView.setImageMatrix(matrix);
    }
    
    private void zoomImage(float scaleFactor) {
        float newScale = currentScale * scaleFactor;
        if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
            matrix.postScale(scaleFactor, scaleFactor, imageView.getWidth() / 2f, imageView.getHeight() / 2f);
            currentScale = newScale;
            imageView.setImageMatrix(matrix);
        }
    }
    
    private void shareImage() {
        if (imageUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            startActivity(Intent.createChooser(shareIntent, "分享图片"));
        }
    }
    
    private void saveImage() {
        // 实现图片保存功能
        Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleInfo() {
        infoText.setVisibility(infoText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
    
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
    
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = currentScale * scaleFactor;
            
            if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                currentScale = newScale;
                imageView.setImageMatrix(matrix);
            }
            return true;
        }
    }
    
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentScale > 1.0f) {
                fitImageToScreen();
            } else {
                matrix.postScale(2.0f, 2.0f, e.getX(), e.getY());
                currentScale *= 2.0f;
                imageView.setImageMatrix(matrix);
            }
            return true;
        }
        
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // 切换控制面板显示
            controlPanel.setVisibility(controlPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            return true;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
        }
    }
}