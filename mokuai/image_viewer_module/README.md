# 🖼️ 图片查看器模块 (Image Viewer Module)

## 🎯 概述

EhViewer图片查看器模块提供完整的图片查看和处理功能，支持本地图片、网络图片、手势缩放、旋转等高级操作。通过优化的内存管理和多线程加载，确保流畅的用户体验。

## ✨ 主要特性

- ✅ **多源图片加载**：支持本地、网络、内容URI等多种图片源
- ✅ **手势操作**：缩放、拖拽、旋转、双击等丰富的交互
- ✅ **内存优化**：智能采样和Bitmap内存管理
- ✅ **自适应缩放**：自动适应屏幕大小和方向
- ✅ **异步加载**：多线程图片加载，不阻塞UI线程
- ✅ **错误处理**：完善的加载失败处理和用户提示

## 🚀 快速开始

### 基本使用

```java
// 获取图片查看器管理器实例
ImageViewerManager imageViewer = ImageViewerManager.getInstance(context);

// 设置回调监听器
imageViewer.setCallback(new ImageViewerManager.ImageViewerCallback() {
    @Override
    public void onImageLoaded(Bitmap bitmap, String source) {
        Log.d(TAG, "图片加载成功: " + source);
    }

    @Override
    public void onImageLoadError(String error, String source) {
        Log.e(TAG, "图片加载失败: " + error);
    }

    @Override
    public void onScaleChanged(float scale) {
        Log.d(TAG, "缩放比例: " + scale);
    }

    @Override
    public void onImageClicked() {
        Log.d(TAG, "图片被点击");
    }

    @Override
    public void onImageDoubleTapped() {
        Log.d(TAG, "图片被双击");
    }

    @Override
    public void onImageRotated(float degrees) {
        Log.d(TAG, "图片旋转: " + degrees + "度");
    }
});

// 加载本地图片
imageViewer.loadLocalImage("/sdcard/images/photo.jpg", imageView);

// 加载网络图片
imageViewer.loadNetworkImage("https://example.com/image.jpg", imageView);

// 加载内容URI图片
Uri imageUri = Uri.parse("content://media/external/images/media/123");
imageViewer.loadContentImage(imageUri, imageView);
```

### 高级配置

```java
// 创建自定义配置
ImageViewerManager.ImageViewerConfig config = new ImageViewerManager.ImageViewerConfig.Builder()
    .setMinScale(0.3f)                    // 最小缩放比例
    .setMaxScale(5.0f)                   // 最大缩放比例
    .enableRotation(true)                // 启用旋转
    .enableZoom(true)                    // 启用缩放
    .enableDrag(true)                    // 启用拖拽
    .enableDoubleTap(true)               // 启用双击
    .setMaxImageSize(4096)               // 最大图片尺寸
    .useHardwareAcceleration(true)       // 使用硬件加速
    .build();

// 应用配置
imageViewer.setConfig(config);

// 手动控制图片变换
imageViewer.rotateImage(90, imageView);     // 旋转90度
imageViewer.zoomImage(1.5f, imageView);     // 放大1.5倍
imageViewer.resetTransform(imageView);      // 重置变换
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `ImageViewerManager` | 图片查看器管理器核心类 |
| `ImageViewerConfig` | 配置类 |
| `ImageViewerCallback` | 回调接口 |

### 主要方法

#### ImageViewerManager

```java
// 获取实例
ImageViewerManager getInstance(Context context)

// 设置配置
void setConfig(ImageViewerConfig config)

// 设置回调监听器
void setCallback(ImageViewerCallback callback)

// 加载图片
void loadLocalImage(String path, ImageView imageView)
void loadNetworkImage(String url, ImageView imageView)
void loadContentImage(Uri uri, ImageView imageView)

// 图片操作
void rotateImage(float degrees, ImageView imageView)
void zoomImage(float scaleFactor, ImageView imageView)
void resetTransform(ImageView imageView)

// 获取状态
float getCurrentScale()

// 清理资源
void cleanup()
```

#### ImageViewerCallback

```java
// 回调方法
void onImageLoaded(Bitmap bitmap, String source)      // 图片加载成功
void onImageLoadError(String error, String source)    // 图片加载失败
void onScaleChanged(float scale)                      // 缩放比例改变
void onImageClicked()                                 // 图片被点击
void onImageDoubleTapped()                            // 图片被双击
void onImageRotated(float degrees)                    // 图片被旋转
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `minScale` | `float` | `0.5f` | 最小缩放比例 |
| `maxScale` | `float` | `10.0f` | 最大缩放比例 |
| `enableRotation` | `boolean` | `true` | 是否启用旋转功能 |
| `enableZoom` | `boolean` | `true` | 是否启用缩放功能 |
| `enableDrag` | `boolean` | `true` | 是否启用拖拽功能 |
| `enableDoubleTap` | `boolean` | `true` | 是否启用双击功能 |
| `maxImageSize` | `int` | `2048` | 最大图片尺寸(像素) |
| `useHardwareAcceleration` | `boolean` | `true` | 是否使用硬件加速 |

## 📦 依赖项

```gradle
dependencies {
    // 核心依赖
    implementation 'com.example:image-viewer-module:1.0.0'

    // 可选依赖（如果需要扩展功能）
    implementation 'androidx.core:core-ktx:1.10.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 内存管理
```java
// 正确处理Bitmap内存
public class ImageViewerActivity extends AppCompatActivity {
    private Bitmap currentBitmap;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放Bitmap内存
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
        // 清理ImageViewerManager
        ImageViewerManager.getInstance(this).cleanup();
    }
}
```

### 图片格式支持
- ✅ JPEG, PNG, WebP, GIF (静态)
- ✅ BMP, TIFF (部分设备支持)
- ❌ 动态GIF, WebP动画 (需要额外处理)

### 网络图片加载
```java
// 为网络图片添加超时设置
public class CustomImageLoader {
    public void loadWithTimeout(String url, ImageView imageView) {
        // 设置网络超时
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);  // 10秒连接超时
        connection.setReadTimeout(15000);     // 15秒读取超时

        // 使用ImageViewerManager加载
        ImageViewerManager.getInstance(context).loadNetworkImage(url, imageView);
    }
}
```

## 🔄 工作原理

### 图片加载流程

1. **预处理阶段**
   ```java
   // 采样检测图片尺寸
   BitmapFactory.Options options = new BitmapFactory.Options();
   options.inJustDecodeBounds = true;
   BitmapFactory.decodeStream(inputStream, null, options);

   // 计算采样率，避免内存溢出
   options.inSampleSize = calculateInSampleSize(options);
   options.inJustDecodeBounds = false;
   ```

2. **内存优化加载**
   ```java
   // 使用RGB_565减少内存占用
   options.inPreferredConfig = Bitmap.Config.RGB_565;
   Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
   ```

3. **显示和交互**
   ```java
   // 设置Matrix变换
   imageView.setScaleType(ImageView.ScaleType.MATRIX);
   imageView.setImageMatrix(matrix);

   // 初始化手势检测器
   initGestureDetectors(imageView);
   ```

### 手势处理机制

```java
// 多点触控处理
@Override
public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            // 单指按下，开始拖拽模式
            mode = DRAG;
            break;

        case MotionEvent.ACTION_POINTER_DOWN:
            // 多指按下，切换到缩放模式
            mode = ZOOM;
            break;

        case MotionEvent.ACTION_MOVE:
            if (mode == DRAG) {
                // 处理拖拽
                handleDrag(event);
            }
            break;
    }
    return true;
}
```

### 内存管理策略

```java
// Bitmap内存监控
public class BitmapMemoryManager {
    private Set<Bitmap> trackedBitmaps = new HashSet<>();

    public Bitmap loadAndTrack(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);

        // 跟踪Bitmap对象
        trackedBitmaps.add(bitmap);

        // 设置回收监听
        bitmap.setOnRecycleListener(() -> {
            trackedBitmaps.remove(bitmap);
        });

        return bitmap;
    }

    public void releaseAll() {
        for (Bitmap bitmap : trackedBitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        trackedBitmaps.clear();
    }
}
```

## 🧪 测试

### 单元测试
```java
@Test
public void testLoadLocalImage_Success() {
    // Given
    ImageViewerManager manager = ImageViewerManager.getInstance(context);
    ImageView imageView = new ImageView(context);
    String testPath = "/sdcard/test/image.jpg";

    // When
    manager.loadLocalImage(testPath, imageView);

    // Then
    // 验证图片加载和显示
    assertNotNull(imageView.getDrawable());
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4.class)
public class ImageViewerIntegrationTest {

    @Test
    public void testImageViewerFullFlow() {
        // 测试完整的图片查看流程
        // 1. 加载图片
        // 2. 执行缩放操作
        // 3. 执行旋转操作
        // 4. 验证变换结果
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingImageViewer`)
3. 提交更改 (`git commit -m 'Add some AmazingImageViewer'`)
4. 推送到分支 (`git push origin feature/AmazingImageViewer`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@example.com
- 📖 文档: [完整文档](https://docs.example.com)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/example/repo/issues)

---

**💡 提示**: 该模块特别适用于图片查看器、图库应用、图片编辑工具等需要高质量图片显示和交互的应用场景。
