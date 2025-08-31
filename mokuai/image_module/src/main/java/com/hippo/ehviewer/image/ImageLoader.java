/*
 * EhViewer Image Module - ImageLoader
 * 图片加载器 - 提供异步图片加载、缓存、多格式支持等功能
 */

package com.hippo.ehviewer.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图片加载器
 * 支持异步加载、内存缓存、磁盘缓存、缩略图等功能
 */
public class ImageLoader {

    private static final String TAG = ImageLoader.class.getSimpleName();
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;

    private final Context mContext;
    private final ImageCache mImageCache;
    private final ExecutorService mExecutorService;

    private static ImageLoader sInstance;

    /**
     * 图片加载回调接口
     */
    public interface ImageLoadCallback {
        void onSuccess(Bitmap bitmap);
        void onFailure(Exception e);
    }

    /**
     * 获取单例实例
     */
    public static synchronized ImageLoader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageLoader(context.getApplicationContext());
        }
        return sInstance;
    }

    private ImageLoader(Context context) {
        mContext = context;
        mImageCache = new ImageCache(context);
        mExecutorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * 加载图片到ImageView
     * @param url 图片URL
     * @param imageView 目标ImageView
     */
    public void loadImage(String url, ImageView imageView) {
        loadImage(url, imageView, null);
    }

    /**
     * 加载图片到ImageView（带回调）
     * @param url 图片URL
     * @param imageView 目标ImageView
     * @param callback 加载回调
     */
    public void loadImage(String url, ImageView imageView, ImageLoadCallback callback) {
        if (url == null || url.isEmpty()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("URL is null or empty"));
            }
            return;
        }

        // 设置占位符
        imageView.setTag(url);

        // 先尝试从缓存加载
        Bitmap cachedBitmap = mImageCache.getFromMemory(url);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            if (callback != null) {
                callback.onSuccess(cachedBitmap);
            }
            return;
        }

        // 异步加载图片
        ImageLoadTask task = new ImageLoadTask(url, imageView, callback);
        mExecutorService.execute(task);
    }

    /**
     * 预加载图片到缓存
     * @param url 图片URL
     */
    public void preloadImage(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }

        // 检查是否已在缓存中
        if (mImageCache.getFromMemory(url) != null) {
            return;
        }

        // 异步预加载
        ImagePreloadTask task = new ImagePreloadTask(url);
        mExecutorService.execute(task);
    }

    /**
     * 清除内存缓存
     */
    public void clearMemoryCache() {
        mImageCache.clearMemoryCache();
    }

    /**
     * 清除磁盘缓存
     */
    public void clearDiskCache() {
        mImageCache.clearDiskCache();
    }

    /**
     * 获取缓存大小
     * @return 缓存大小（字节）
     */
    public long getCacheSize() {
        return mImageCache.getCacheSize();
    }

    /**
     * 关闭图片加载器
     */
    public void shutdown() {
        mExecutorService.shutdown();
    }

    /**
     * 图片加载任务
     */
    private class ImageLoadTask implements Runnable {
        private final String mUrl;
        private final ImageView mImageView;
        private final ImageLoadCallback mCallback;

        public ImageLoadTask(String url, ImageView imageView, ImageLoadCallback callback) {
            mUrl = url;
            mImageView = imageView;
            mCallback = callback;
        }

        @Override
        public void run() {
            try {
                // 检查URL是否匹配（避免加载到错误的ImageView）
                if (mImageView != null && !mUrl.equals(mImageView.getTag())) {
                    return;
                }

                // 从网络加载图片
                Bitmap bitmap = loadBitmapFromNetwork(mUrl);
                if (bitmap == null) {
                    throw new IOException("Failed to load bitmap from network");
                }

                // 保存到缓存
                mImageCache.put(mUrl, bitmap);

                // 在UI线程更新ImageView
                if (mImageView != null && mUrl.equals(mImageView.getTag())) {
                    mImageView.post(() -> {
                        mImageView.setImageBitmap(bitmap);
                        if (mCallback != null) {
                            mCallback.onSuccess(bitmap);
                        }
                    });
                } else if (mCallback != null) {
                    mImageView.post(() -> mCallback.onSuccess(bitmap));
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to load image: " + mUrl, e);
                if (mCallback != null) {
                    mImageView.post(() -> mCallback.onFailure(e));
                }
            }
        }

        /**
         * 从网络加载Bitmap
         */
        private Bitmap loadBitmapFromNetwork(String url) throws IOException {
            HttpURLConnection connection = null;
            InputStream inputStream = null;

            try {
                URL imageUrl = new URL(url);
                connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setConnectTimeout(10000); // 10秒连接超时
                connection.setReadTimeout(15000); // 15秒读取超时
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                inputStream = connection.getInputStream();
                return BitmapFactory.decodeStream(inputStream);

            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to close input stream", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    /**
     * 图片预加载任务
     */
    private class ImagePreloadTask implements Runnable {
        private final String mUrl;

        public ImagePreloadTask(String url) {
            mUrl = url;
        }

        @Override
        public void run() {
            try {
                // 检查是否已在缓存中
                if (mImageCache.getFromMemory(mUrl) != null) {
                    return;
                }

                // 加载图片
                Bitmap bitmap = loadBitmapFromNetwork(mUrl);
                if (bitmap != null) {
                    mImageCache.put(mUrl, bitmap);
                    Log.d(TAG, "Preloaded image: " + mUrl);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to preload image: " + mUrl, e);
            }
        }

        private Bitmap loadBitmapFromNetwork(String url) throws IOException {
            HttpURLConnection connection = null;
            InputStream inputStream = null;

            try {
                URL imageUrl = new URL(url);
                connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                inputStream = connection.getInputStream();
                return BitmapFactory.decodeStream(inputStream);

            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to close input stream", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }
}
