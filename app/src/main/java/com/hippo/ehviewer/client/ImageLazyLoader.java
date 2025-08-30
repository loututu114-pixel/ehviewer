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

package com.hippo.ehviewer.client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 图片懒加载管理器
 * 优化网页中图片的加载性能
 */
public class ImageLazyLoader {

    private static final String TAG = "ImageLazyLoader";

    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    private static final int IMAGE_CACHE_SIZE = 50; // 缓存最多50张图片

    private static ImageLazyLoader sInstance;

    // 线程池
    private final ExecutorService mExecutorService = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);

    // 图片缓存
    private final Map<String, Bitmap> mImageCache = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> mDownloadTasks = new ConcurrentHashMap<>();

    // 主线程Handler
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // 下载任务管理
    private final Map<String, ImageLoadCallback> mCallbacks = new ConcurrentHashMap<>();

    /**
     * 获取单例实例
     */
    public static synchronized ImageLazyLoader getInstance() {
        if (sInstance == null) {
            sInstance = new ImageLazyLoader();
        }
        return sInstance;
    }

    private ImageLazyLoader() {
        // 私有构造函数
    }

    /**
     * 图片加载回调接口
     */
    public interface ImageLoadCallback {
        void onImageLoaded(String url, Bitmap bitmap);
        void onImageLoadFailed(String url, Exception e);
    }

    /**
     * 懒加载图片
     */
    public void loadImageLazy(String imageUrl, ImageLoadCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        // 检查缓存
        Bitmap cachedBitmap = mImageCache.get(imageUrl);
        if (cachedBitmap != null) {
            callback.onImageLoaded(imageUrl, cachedBitmap);
            return;
        }

        // 检查是否已经在下载中
        if (mDownloadTasks.containsKey(imageUrl)) {
            // 注册回调
            mCallbacks.put(imageUrl, callback);
            return;
        }

        // 开始下载任务
        mCallbacks.put(imageUrl, callback);
        Future<?> future = mExecutorService.submit(() -> downloadImage(imageUrl));
        mDownloadTasks.put(imageUrl, future);
    }

    /**
     * 下载图片
     */
    private void downloadImage(String imageUrl) {
        try {
            Log.d(TAG, "Downloading image: " + imageUrl);

            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap != null) {
                    // 缓存图片
                    cacheImage(imageUrl, bitmap);

                    // 通知回调
                    mMainHandler.post(() -> notifyImageLoaded(imageUrl, bitmap));
                } else {
                    throw new IOException("Failed to decode bitmap");
                }
            } else {
                throw new IOException("HTTP error: " + responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Failed to download image: " + imageUrl, e);
            mMainHandler.post(() -> notifyImageLoadFailed(imageUrl, e));
        } finally {
            mDownloadTasks.remove(imageUrl);
        }
    }

    /**
     * 缓存图片
     */
    private void cacheImage(String url, Bitmap bitmap) {
        // 如果缓存过大，清理最旧的图片
        if (mImageCache.size() >= IMAGE_CACHE_SIZE) {
            // 简单清理策略：移除第一个
            String firstKey = mImageCache.keySet().iterator().next();
            mImageCache.remove(firstKey);
        }

        mImageCache.put(url, bitmap);
        Log.d(TAG, "Image cached: " + url + ", cache size: " + mImageCache.size());
    }

    /**
     * 通知图片加载成功
     */
    private void notifyImageLoaded(String url, Bitmap bitmap) {
        ImageLoadCallback callback = mCallbacks.remove(url);
        if (callback != null) {
            try {
                callback.onImageLoaded(url, bitmap);
            } catch (Exception e) {
                Log.e(TAG, "Callback failed for image loaded: " + url, e);
            }
        }
    }

    /**
     * 通知图片加载失败
     */
    private void notifyImageLoadFailed(String url, Exception e) {
        ImageLoadCallback callback = mCallbacks.remove(url);
        if (callback != null) {
            try {
                callback.onImageLoadFailed(url, e);
            } catch (Exception ex) {
                Log.e(TAG, "Callback failed for image load failed: " + url, ex);
            }
        }
    }

    /**
     * 预加载图片（用于预测性加载）
     */
    public void preloadImage(String imageUrl) {
        loadImageLazy(imageUrl, new ImageLoadCallback() {
            @Override
            public void onImageLoaded(String url, Bitmap bitmap) {
                Log.d(TAG, "Image preloaded: " + url);
            }

            @Override
            public void onImageLoadFailed(String url, Exception e) {
                Log.w(TAG, "Image preload failed: " + url, e);
            }
        });
    }

    /**
     * 取消图片加载
     */
    public void cancelImageLoad(String imageUrl) {
        Future<?> future = mDownloadTasks.remove(imageUrl);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            Log.d(TAG, "Image load cancelled: " + imageUrl);
        }
        mCallbacks.remove(imageUrl);
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        mImageCache.clear();
        mCallbacks.clear();

        // 取消所有下载任务
        for (Future<?> future : mDownloadTasks.values()) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        mDownloadTasks.clear();

        Log.d(TAG, "Image cache cleared");
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return mImageCache.size();
    }

    /**
     * 检查图片是否已缓存
     */
    public boolean isImageCached(String imageUrl) {
        return mImageCache.containsKey(imageUrl);
    }

    /**
     * 获取缓存的图片
     */
    public Bitmap getCachedImage(String imageUrl) {
        return mImageCache.get(imageUrl);
    }

    /**
     * JavaScript代码注入，用于实现图片懒加载
     */
    public static String getLazyLoadJavaScript() {
        return "javascript:(function(){" +
                "var images = document.getElementsByTagName('img');" +
                "for(var i = 0; i < images.length; i++){" +
                "  var img = images[i];" +
                "  if(img.getAttribute('data-src')){" +
                "    img.src = img.getAttribute('data-src');" +
                "    img.removeAttribute('data-src');" +
                "  }" +
                "}" +
                "})()";
    }

    /**
     * 为WebView启用图片懒加载
     */
    public void enableLazyLoadingForWebView(WebView webView) {
        if (webView != null) {
            // 注入懒加载JavaScript
            webView.evaluateJavascript(getLazyLoadJavaScript(), null);
        }
    }

    /**
     * 销毁实例
     */
    public void destroy() {
        clearCache();
        mExecutorService.shutdown();
        Log.d(TAG, "ImageLazyLoader destroyed");
    }
}
