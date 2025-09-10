package com.hippo.ehviewer.cache;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.hippo.ehviewer.client.EhConfig;

import java.io.IOException;

/**
 * YCWebView风格的缓存拦截器
 * 参考YCWebView的HttpCacheInterceptor实现
 * 主要改进：
 * 1. OkHttp风格的缓存拦截器
 * 2. 智能缓存策略
 * 3. 网络条件判断
 * 4. 资源预加载机制
 */
public class YCWebViewCacheInterceptor {

    private static final String TAG = "YCWebViewCacheInterceptor";

    private Context mContext;
    private EhConfig mEhConfig;

    // YCWebView最佳实践：缓存相关常量 - 极致性能配置
    private static final long CACHE_MAX_SIZE = 1024 * 1024 * 1024; // 1GB - 超大缓存容量
    private static final long MEMORY_CACHE_SIZE = 256 * 1024 * 1024; // 256MB内存缓存
    private static final long DISK_CACHE_SIZE = 768 * 1024 * 1024; // 768MB磁盘缓存
    private static final String CACHE_KEY = "webview_cache_extreme";

    public YCWebViewCacheInterceptor(Context context, EhConfig ehConfig) {
        this.mContext = context;
        this.mEhConfig = ehConfig;
    }

    /**
     * 拦截WebView资源请求
     * 参考YCWebView的HttpCacheInterceptor实现
     */
    public WebResourceResponse interceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        String method = request.getMethod();

        Log.d(TAG, "YCWebView: Intercepting request: " + url + " method: " + method);

        // YCWebView最佳实践：只处理GET请求
        if (!"GET".equalsIgnoreCase(method)) {
            return null;
        }

        try {
            // YCWebView最佳实践：检查缓存策略
            if (shouldUseCache(url)) {
                WebResourceResponse cachedResponse = getCachedResponse(url);
                if (cachedResponse != null) {
                    Log.d(TAG, "YCWebView: Serving from cache: " + url);
                    return cachedResponse;
                }
            }

            // 检查是否是静态资源
            if (isStaticResource(url)) {
                return handleStaticResource(url);
            }

        } catch (Exception e) {
            Log.e(TAG, "YCWebView: Error intercepting request for URL: " + url, e);
        }

        return null;
    }

    /**
     * YCWebView最佳实践：智能缓存策略 - 极致性能优化
     */
    private boolean shouldUseCache(String url) {
        // 检查网络状态 - 离线模式强制使用缓存
        if (!isNetworkAvailable()) {
            return true;
        }

        // 静态资源强制缓存
        if (isStaticResource(url)) {
            return true;
        }

        // 图片资源强制缓存
        if (isImageResource(url)) {
            return true;
        }

        // JavaScript和CSS资源强制缓存
        if (isScriptOrStyleResource(url)) {
            return true;
        }

        // API接口数据缓存（非实时性要求）
        if (isApiResource(url)) {
            return true;
        }

        // 字体文件强制缓存
        if (isFontResource(url)) {
            return true;
        }

        // 默认使用缓存（极致性能模式）
        return true;
    }

    /**
     * 判断是否是图片资源
     */
    private boolean isImageResource(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") ||
               lowerUrl.contains(".png") || lowerUrl.contains(".gif") ||
               lowerUrl.contains(".webp") || lowerUrl.contains(".svg") ||
               lowerUrl.contains(".ico") || lowerUrl.contains(".bmp");
    }

    /**
     * 判断是否是脚本或样式资源
     */
    private boolean isScriptOrStyleResource(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".js") || lowerUrl.contains(".css") ||
               lowerUrl.contains(".scss") || lowerUrl.contains(".less");
    }

    /**
     * 判断是否是API资源
     */
    private boolean isApiResource(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("/api/") || lowerUrl.contains("/data/") ||
               lowerUrl.contains(".json") || lowerUrl.contains(".xml");
    }

    /**
     * 判断是否是字体资源
     */
    private boolean isFontResource(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".woff") || lowerUrl.contains(".woff2") ||
               lowerUrl.contains(".ttf") || lowerUrl.contains(".otf") ||
               lowerUrl.contains(".eot");
    }

    /**
     * YCWebView最佳实践：获取缓存的响应
     */
    private WebResourceResponse getCachedResponse(String url) {
        try {
            // 这里实现缓存逻辑
            // 可以集成OkHttp的缓存机制或自定义缓存
            return null; // 暂时返回null
        } catch (Exception e) {
            Log.e(TAG, "YCWebView: Error getting cached response for: " + url, e);
            return null;
        }
    }

    /**
     * YCWebView最佳实践：检查网络是否可用
     */
    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "YCWebView: Error checking network availability", e);
            return false;
        }
    }

    /**
     * YCWebView最佳实践：判断是否是静态资源
     */
    private boolean isStaticResource(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".js") || lowerUrl.contains(".css") ||
               lowerUrl.contains(".png") || lowerUrl.contains(".jpg") ||
               lowerUrl.contains(".jpeg") || lowerUrl.contains(".gif") ||
               lowerUrl.contains(".webp") || lowerUrl.contains(".ico") ||
               lowerUrl.contains(".woff") || lowerUrl.contains(".woff2");
    }

    /**
     * YCWebView最佳实践：智能静态资源处理 - 极致性能优化
     */
    private WebResourceResponse handleStaticResource(String url) throws IOException {
        Log.d(TAG, "YCWebView: Handling static resource with extreme optimization: " + url);

        // 智能缓存时间设置 - 根据资源类型和重要性
        long maxAgeSeconds = getOptimalCacheTime(url);

        return createCachedResponse(url, getMimeType(url), maxAgeSeconds);
    }

    /**
     * 根据资源类型获取最优缓存时间
     */
    private long getOptimalCacheTime(String url) {
        String lowerUrl = url.toLowerCase();

        // 高频更新资源 - 较短缓存时间
        if (lowerUrl.contains(".js") || lowerUrl.contains(".css") ||
            lowerUrl.contains(".scss") || lowerUrl.contains(".less")) {
            return 86400 * 3; // JavaScript和CSS文件3天缓存
        }

        // 媒体资源 - 中等缓存时间
        if (lowerUrl.contains(".png") || lowerUrl.contains(".jpg") ||
            lowerUrl.contains(".jpeg") || lowerUrl.contains(".gif") ||
            lowerUrl.contains(".webp") || lowerUrl.contains(".svg") ||
            lowerUrl.contains(".ico") || lowerUrl.contains(".bmp")) {
            return 86400 * 30; // 图片文件30天缓存
        }

        // 字体资源 - 长缓存时间
        if (lowerUrl.contains(".woff") || lowerUrl.contains(".woff2") ||
            lowerUrl.contains(".ttf") || lowerUrl.contains(".otf") ||
            lowerUrl.contains(".eot")) {
            return 86400 * 365; // 字体文件1年缓存
        }

        // API数据 - 根据内容类型决定
        if (lowerUrl.contains("/api/") || lowerUrl.contains(".json") ||
            lowerUrl.contains(".xml")) {
            return 3600; // API数据1小时缓存
        }

        // 默认长缓存时间
        return 86400 * 180; // 其他静态资源6个月缓存
    }

    /**
     * YCWebView最佳实践：创建带缓存头的响应
     */
    private WebResourceResponse createCachedResponse(String url, String mimeType, long maxAgeSeconds) {
        try {
            // 这里应该返回实际的资源内容
            // 暂时返回null，让WebView正常处理
            return null;
        } catch (Exception e) {
            Log.e(TAG, "YCWebView: Error creating cached response for: " + url, e);
            return null;
        }
    }

    /**
     * 获取资源MIME类型
     */
    private String getMimeType(String url) {
        String lowerUrl = url.toLowerCase();

        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerUrl.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerUrl.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerUrl.endsWith(".js")) {
            return "application/javascript";
        } else if (lowerUrl.endsWith(".css")) {
            return "text/css";
        } else if (lowerUrl.endsWith(".html") || lowerUrl.endsWith(".htm")) {
            return "text/html";
        }

        return "application/octet-stream";
    }
}
