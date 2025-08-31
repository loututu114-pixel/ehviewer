# EhViewer 图片处理模块 (Image Module)

## 概述

图片处理模块为EhViewer应用提供完整的图片处理功能，包括图片加载、缓存、显示、缩放、格式转换等。该模块采用高效的图片处理算法，支持多种图片格式和大尺寸图片的处理。

## 主要功能

### 1. 图片加载
- 异步图片加载
- 多级缓存（内存+磁盘）
- 图片预加载
- 加载进度监听

### 2. 图片显示
- 图片缩放和裁剪
- 图片旋转
- 图片翻转
- 支持手势缩放

### 3. 图片处理
- 格式转换
- 图片压缩
- 图片滤镜
- 图片水印

### 4. 缓存管理
- 内存缓存
- 磁盘缓存
- 缓存大小限制
- 缓存清理策略

## 核心类

### ImageLoader - 图片加载器
```java
public class ImageLoader {
    // 获取单例实例
    public static ImageLoader getInstance(Context context)

    // 加载图片到ImageView
    public void loadImage(String url, ImageView imageView)
    public void loadImage(String url, ImageView imageView, ImageLoadCallback callback)

    // 预加载图片
    public void preloadImage(String url)

    // 缓存管理
    public void clearMemoryCache()
    public void clearDiskCache()
    public long getCacheSize()
}
```

### ImageCache - 图片缓存管理器
```java
public class ImageCache {
    // 缓存操作
    public Bitmap get(String key)
    public void put(String key, Bitmap bitmap)
    public void remove(String key)

    // 缓存清理
    public void clearMemoryCache()
    public void clearDiskCache()
    public void clearAll()
}
```

### ImageProcessor - 图片处理器
```java
public class ImageProcessor {
    // 加载图片
    public ImageProcessor load(Bitmap bitmap)

    // 尺寸处理
    public ImageProcessor resize(int width, int height)
    public ImageProcessor scale(float scaleX, float scaleY)

    // 变换处理
    public ImageProcessor rotate(float degrees)
    public ImageProcessor flip(boolean horizontal, boolean vertical)

    // 裁剪处理
    public ImageProcessor crop(int x, int y, int width, int height)
    public ImageProcessor roundCorner(float radius)
    public ImageProcessor circleCrop()

    // 效果处理
    public ImageProcessor applyGrayscaleFilter()
    public ImageProcessor adjustBrightness(int brightness)
    public ImageProcessor adjustContrast(float contrast)

    // 水印处理
    public ImageProcessor addWatermark(Bitmap watermark, int x, int y, float alpha)

    // 获取结果
    public Bitmap getResult()
    public boolean saveToFile(String filePath)
}
```

## 使用方法

### 基本图片加载

```java
// 初始化图片加载器
ImageLoader imageLoader = ImageLoader.getInstance(context);

// 简单加载图片
imageLoader.loadImage("https://example.com/image.jpg", imageView);

// 带回调的加载
imageLoader.loadImage("https://example.com/image.jpg", imageView,
    new ImageLoader.ImageLoadCallback() {
        @Override
        public void onSuccess(Bitmap bitmap) {
            Log.d(TAG, "图片加载成功");
            // 处理成功加载的图片
        }

        @Override
        public void onFailure(Exception e) {
            Log.e(TAG, "图片加载失败", e);
            // 处理加载失败的情况
        }
    });

// 预加载图片（提升后续加载速度）
imageLoader.preloadImage("https://example.com/image2.jpg");
```

### 高级图片处理

```java
// 创建图片处理器
ImageProcessor processor = new ImageProcessor();

// 链式处理图片
Bitmap processedBitmap = processor
    .load(originalBitmap)           // 加载原图
    .resize(800, 600)              // 缩放到800x600
    .rotate(90)                    // 旋转90度
    .roundCorner(20)               // 添加圆角
    .applyGrayscaleFilter()        // 应用灰度滤镜
    .adjustBrightness(20)          // 增加亮度
    .getResult();                  // 获取处理结果

// 显示处理后的图片
imageView.setImageBitmap(processedBitmap);
```

### 缓存管理

```java
ImageLoader imageLoader = ImageLoader.getInstance(context);

// 获取缓存大小
long cacheSize = imageLoader.getCacheSize();
Log.d(TAG, "缓存大小: " + cacheSize + " bytes");

// 清理内存缓存
imageLoader.clearMemoryCache();

// 清理磁盘缓存
imageLoader.clearDiskCache();
```

### 复杂图片处理示例

```java
public Bitmap createAvatar(Bitmap original) {
    return new ImageProcessor()
        .load(original)
        .resize(200, 200)              // 缩放到头像尺寸
        .circleCrop()                  // 圆形裁剪
        .adjustBrightness(10)          // 轻微提亮
        .adjustContrast(1.1f)          // 增加对比度
        .getResult();
}

public Bitmap addWatermark(Bitmap image, Bitmap watermark) {
    return new ImageProcessor()
        .load(image)
        .addWatermark(watermark,
            image.getWidth() - watermark.getWidth() - 20,  // 右下角位置
            image.getHeight() - watermark.getHeight() - 20,
            0.7f)  // 70%透明度
        .getResult();
}
```

### 保存处理结果

```java
ImageProcessor processor = new ImageProcessor();
processor.load(originalBitmap)
    .resize(1024, 768)
    .applyGrayscaleFilter();

// 保存为PNG格式
boolean success = processor.saveToFile("/sdcard/processed_image.png");

// 保存为JPEG格式（质量80）
boolean success2 = processor.saveToFile("/sdcard/processed_image.jpg",
    Bitmap.CompressFormat.JPEG, 80);
```

## 依赖项

在你的`build.gradle`文件中添加：

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
```

## 权限配置

在`AndroidManifest.xml`中添加网络权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 性能优化建议

1. **缓存策略**: 合理设置缓存大小，避免内存溢出
2. **图片尺寸**: 在加载大图时先进行尺寸压缩
3. **异步处理**: 图片处理操作应该在工作线程中执行
4. **资源回收**: 使用完Bitmap后及时回收
5. **内存监控**: 监控应用内存使用情况

## 示例项目

查看完整的示例代码：
- `BasicImageLoadingActivity.java` - 基础图片加载示例
- `AdvancedImageProcessingActivity.java` - 高级图片处理示例
- `ImageCacheManagementActivity.java` - 缓存管理示例

## 注意事项

1. **内存管理**: 大尺寸图片处理时注意内存使用
2. **线程安全**: UI相关的操作要在主线程执行
3. **异常处理**: 网络加载和文件操作需要适当的异常处理
4. **权限检查**: 使用前检查必要的权限
5. **格式支持**: 不同Android版本对图片格式支持可能不同

## 许可证

本模块遵循Apache License 2.0协议。
