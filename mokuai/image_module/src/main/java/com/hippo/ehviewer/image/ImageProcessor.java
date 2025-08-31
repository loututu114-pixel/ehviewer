/*
 * EhViewer Image Module - ImageProcessor
 * 图片处理器 - 提供图片缩放、裁剪、旋转、滤镜等处理功能
 */

package com.hippo.ehviewer.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * 图片处理器
 * 提供各种图片处理功能
 */
public class ImageProcessor {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mPaint;
    private Matrix mMatrix;

    public ImageProcessor() {
        init();
    }

    public ImageProcessor(Bitmap bitmap) {
        init();
        load(bitmap);
    }

    /**
     * 初始化处理器
     */
    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMatrix = new Matrix();
    }

    /**
     * 加载图片
     * @param bitmap 要处理的图片
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor load(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap cannot be null");
        }

        mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        mCanvas = new Canvas(mBitmap);
        return this;
    }

    /**
     * 缩放图片
     * @param width 目标宽度
     * @param height 目标高度
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor resize(int width, int height) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBitmap, width, height, true);
        mBitmap = scaledBitmap;
        mCanvas = new Canvas(mBitmap);
        return this;
    }

    /**
     * 按比例缩放
     * @param scaleX 水平缩放比例
     * @param scaleY 垂直缩放比例
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor scale(float scaleX, float scaleY) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        mMatrix.reset();
        mMatrix.setScale(scaleX, scaleY);

        Bitmap scaledBitmap = Bitmap.createBitmap(mBitmap, 0, 0,
                mBitmap.getWidth(), mBitmap.getHeight(), mMatrix, true);
        mBitmap = scaledBitmap;
        mCanvas = new Canvas(mBitmap);
        return this;
    }

    /**
     * 旋转图片
     * @param degrees 旋转角度（度）
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor rotate(float degrees) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        mMatrix.reset();
        mMatrix.setRotate(degrees, mBitmap.getWidth() / 2f, mBitmap.getHeight() / 2f);

        Bitmap rotatedBitmap = Bitmap.createBitmap(mBitmap, 0, 0,
                mBitmap.getWidth(), mBitmap.getHeight(), mMatrix, true);
        mBitmap = rotatedBitmap;
        mCanvas = new Canvas(mBitmap);
        return this;
    }

    /**
     * 翻转图片
     * @param horizontal 是否水平翻转
     * @param vertical 是否垂直翻转
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor flip(boolean horizontal, boolean vertical) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        mMatrix.reset();
        mMatrix.setScale(horizontal ? -1 : 1, vertical ? -1 : 1,
                mBitmap.getWidth() / 2f, mBitmap.getHeight() / 2f);

        Bitmap flippedBitmap = Bitmap.createBitmap(mBitmap, 0, 0,
                mBitmap.getWidth(), mBitmap.getHeight(), mMatrix, true);
        mBitmap = flippedBitmap;
        mCanvas = new Canvas(mBitmap);
        return this;
    }

    /**
     * 裁剪图片
     * @param x 起始X坐标
     * @param y 起始Y坐标
     * @param width 裁剪宽度
     * @param height 裁剪高度
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor crop(int x, int y, int width, int height) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
            x + width > mBitmap.getWidth() || y + height > mBitmap.getHeight()) {
            throw new IllegalArgumentException("Invalid crop parameters");
        }

        Bitmap croppedBitmap = Bitmap.createBitmap(mBitmap, x, y, width, height);
        mBitmap = croppedBitmap;
        mCanvas = new Canvas(mBitmap);
        return this;
    }

    /**
     * 圆角裁剪
     * @param radius 圆角半径
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor roundCorner(float radius) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        Bitmap output = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Rect rect = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        final RectF rectF = new RectF(rect);

        mPaint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        mPaint.setColor(color);
        canvas.drawRoundRect(rectF, radius, radius, mPaint);

        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(mBitmap, rect, rect, mPaint);

        mBitmap = output;
        mCanvas = new Canvas(mBitmap);
        return this;
    }

    /**
     * 圆形裁剪
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor circleCrop() {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        int size = Math.min(mBitmap.getWidth(), mBitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Rect rect = new Rect(0, 0, size, size);

        mPaint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        mPaint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, mPaint);

        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(mBitmap, rect, rect, mPaint);

        mBitmap = output;
        mCanvas = new Canvas(mBitmap);
        return this;
    }

    /**
     * 应用灰度滤镜
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor applyGrayscaleFilter() {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

        mPaint.setColorFilter(filter);
        mCanvas.drawBitmap(mBitmap, 0, 0, mPaint);

        return this;
    }

    /**
     * 应用自定义滤镜
     * @param filter 颜色滤镜
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor applyFilter(ColorMatrixColorFilter filter) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        mPaint.setColorFilter(filter);
        mCanvas.drawBitmap(mBitmap, 0, 0, mPaint);

        return this;
    }

    /**
     * 添加水印
     * @param watermark 水印图片
     * @param x 水印X坐标
     * @param y 水印Y坐标
     * @param alpha 水印透明度 (0.0f - 1.0f)
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor addWatermark(Bitmap watermark, int x, int y, float alpha) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        if (watermark == null) {
            throw new IllegalArgumentException("Watermark bitmap cannot be null");
        }

        mPaint.setAlpha((int) (255 * alpha));
        mCanvas.drawBitmap(watermark, x, y, mPaint);
        mPaint.setAlpha(255); // 恢复透明度

        return this;
    }

    /**
     * 调整亮度
     * @param brightness 亮度值 (-255 到 255)
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor adjustBrightness(int brightness) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[]{
                1, 0, 0, 0, brightness,
                0, 1, 0, 0, brightness,
                0, 0, 1, 0, brightness,
                0, 0, 0, 1, 0
        });

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        return applyFilter(filter);
    }

    /**
     * 调整对比度
     * @param contrast 对比度值 (0.0f - 2.0f)
     * @return ImageProcessor实例（支持链式调用）
     */
    public ImageProcessor adjustContrast(float contrast) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        float translate = (-0.5f * contrast + 0.5f) * 255f;
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[]{
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1, 0
        });

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        return applyFilter(filter);
    }

    /**
     * 获取处理后的图片
     * @return 处理后的Bitmap
     */
    public Bitmap getResult() {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }
        return mBitmap;
    }

    /**
     * 保存处理结果到文件
     * @param filePath 文件路径
     * @return 是否保存成功
     */
    public boolean saveToFile(String filePath) {
        return saveToFile(filePath, Bitmap.CompressFormat.PNG, 100);
    }

    /**
     * 保存处理结果到文件
     * @param filePath 文件路径
     * @param format 压缩格式
     * @param quality 质量 (0-100)
     * @return 是否保存成功
     */
    public boolean saveToFile(String filePath, Bitmap.CompressFormat format, int quality) {
        if (mBitmap == null) {
            throw new IllegalStateException("No bitmap loaded");
        }

        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(filePath);
            boolean success = mBitmap.compress(format, quality, out);
            out.close();
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取图片宽度
     */
    public int getWidth() {
        return mBitmap != null ? mBitmap.getWidth() : 0;
    }

    /**
     * 获取图片高度
     */
    public int getHeight() {
        return mBitmap != null ? mBitmap.getHeight() : 0;
    }

    /**
     * 回收资源
     */
    public void recycle() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
            mCanvas = null;
        }
    }
}
