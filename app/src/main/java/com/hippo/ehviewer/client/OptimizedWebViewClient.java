package com.hippo.ehviewer.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hippo.ehviewer.cache.WebViewCacheInterceptor;
import com.hippo.ehviewer.performance.OptimizedWebViewManager;
import com.hippo.ehviewer.video.VideoPlaybackOptimizer;
import com.hippo.ehviewer.optimization.YouTubeOptimizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 优化的WebViewClient
 * 集成缓存拦截、视频优化、性能提升等功能
 * 
 * 核心特性：
 * 1. 资源拦截缓存
 * 2. 视频播放优化
 * 3. 错误处理优化
 * 4. DNS优化支持
 * 5. 安全性增强
 */
public class OptimizedWebViewClient extends WebViewClient {
    private static final String TAG = "OptimizedWebViewClient";
    
    private final Context mContext;
    private final WebViewCacheInterceptor mCacheInterceptor;
    private final OptimizedWebViewManager mWebViewManager;
    private final VideoPlaybackOptimizer mVideoOptimizer;
    private final YouTubeOptimizer mYouTubeOptimizer;
    
    // 优化配置
    private boolean mCacheEnabled = true;
    private boolean mVideoOptimizationEnabled = true;
    private boolean mDnsOptimizationEnabled = true;
    
    // 自定义错误页面
    private String mCustomErrorPage = null;
    
    // 白名单域名（可选）
    private final Map<String, Boolean> mDomainWhitelist = new HashMap<>();
    
    public OptimizedWebViewClient(Context context) {
        mContext = context;
        mCacheInterceptor = new WebViewCacheInterceptor(context);
        mWebViewManager = OptimizedWebViewManager.getInstance(context);
        mVideoOptimizer = new VideoPlaybackOptimizer(context);
        mYouTubeOptimizer = new YouTubeOptimizer(context);
        
        // 初始化白名单
        initializeDomainWhitelist();
        
        Log.d(TAG, "OptimizedWebViewClient initialized");
    }
    
    /**
     * 初始化域名白名单
     */
    private void initializeDomainWhitelist() {
        // 添加常见的安全域名
        mDomainWhitelist.put("youtube.com", true);
        mDomainWhitelist.put("googlevideo.com", true);
        mDomainWhitelist.put("googleapis.com", true);
        mDomainWhitelist.put("gstatic.com", true);
        mDomainWhitelist.put("ytimg.com", true);
        // 可以根据需要添加更多域名
    }
    
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Log.d(TAG, "Page started: " + url);
        
        try {
            // YouTube特殊优化
            if (mYouTubeOptimizer.isYouTubeUrl(url)) {
                mYouTubeOptimizer.optimizeForYouTube(view);
                mVideoOptimizer.optimizeForYouTube(view);
                Log.d(TAG, "YouTube optimizations applied for: " + url);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error in onPageStarted", e);
        }
    }
    
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Log.d(TAG, "Page finished: " + url);
        
        try {
            // 启用图片加载
            mWebViewManager.onPageFinished(view);
            
            // 注入视频优化脚本
            if (mVideoOptimizationEnabled) {
                mVideoOptimizer.injectVideoOptimizationScript(view);
            }
            
            // YouTube特殊优化脚本
            if (mYouTubeOptimizer.isYouTubeUrl(url)) {
                // 延迟注入YouTube优化脚本，确保页面完全加载
                view.postDelayed(() -> {
                    mYouTubeOptimizer.injectYouTubeOptimizationScript(view);
                }, 1000);
                Log.d(TAG, "YouTube optimization script scheduled for: " + url);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error in onPageFinished", e);
        }
    }
    
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request == null || request.getUrl() == null) {
            return super.shouldInterceptRequest(view, request);
        }
        
        String url = request.getUrl().toString();
        
        try {
            // 0. YouTube URL和DNS优化
            if (mYouTubeOptimizer.isYouTubeUrl(url)) {
                url = mYouTubeOptimizer.optimizeYouTubeUrl(url);
                url = mYouTubeOptimizer.applyDnsOptimization(url);
            }
            
            // 1. 检查缓存
            if (mCacheEnabled) {
                WebResourceResponse cacheResponse = mCacheInterceptor.shouldInterceptRequest(url);
                if (cacheResponse != null) {
                    return cacheResponse;
                }
            }
            
            // 2. 域名白名单检查（可选）
            if (!isDomainAllowed(url)) {
                Log.w(TAG, "Domain not in whitelist: " + url);
                // return null; // 可以选择是否拦截
            }
            
            // 3. 特殊资源处理
            WebResourceResponse specialResponse = handleSpecialRequests(url, request);
            if (specialResponse != null) {
                return specialResponse;
            }
            
            // 4. 网络请求优化
            return optimizedNetworkRequest(url, request);
            
        } catch (Exception e) {
            Log.w(TAG, "Error in shouldInterceptRequest for: " + url, e);
            return super.shouldInterceptRequest(view, request);
        }
    }
    
    /**
     * 优化的网络请求
     */
    private WebResourceResponse optimizedNetworkRequest(String url, WebResourceRequest request) {
        try {
            // DNS优化处理
            if (mDnsOptimizationEnabled) {
                url = optimizeDnsUrl(url);
            }
            
            // 创建连接
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            
            // 设置请求头
            Map<String, String> headers = request.getRequestHeaders();
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            // 设置超时
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(15000);    // 15秒读取超时
            
            // 设置User-Agent优化
            if (!headers.containsKey("User-Agent")) {
                connection.setRequestProperty("User-Agent", 
                    "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36");
            }
            
            // 执行请求
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                byte[] data = readInputStream(inputStream);
                
                // 缓存响应
                if (mCacheEnabled && data != null) {
                    String mimeType = connection.getContentType();
                    String encoding = connection.getContentEncoding();
                    mCacheInterceptor.cacheResponse(url, data, mimeType, encoding);
                }
                
                return new WebResourceResponse(
                    connection.getContentType(),
                    connection.getContentEncoding(),
                    new java.io.ByteArrayInputStream(data)
                );
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to load resource: " + url, e);
        }
        
        return null;
    }
    
    /**
     * 处理特殊请求
     */
    private WebResourceResponse handleSpecialRequests(String url, WebResourceRequest request) {
        // YouTube视频请求优化
        if (url.contains("googlevideo.com") || url.contains("youtube.com/videoplayback")) {
            return handleYouTubeVideoRequest(url, request);
        }
        
        // 广告拦截（可选）
        if (isAdRequest(url)) {
            Log.d(TAG, "Blocked ad request: " + url);
            return new WebResourceResponse("text/plain", "utf-8", 
                new java.io.ByteArrayInputStream("".getBytes()));
        }
        
        return null;
    }
    
    /**
     * 处理YouTube视频请求
     */
    private WebResourceResponse handleYouTubeVideoRequest(String url, WebResourceRequest request) {
        try {
            Log.d(TAG, "Optimizing YouTube video request: " + url);
            
            // 可以在这里添加特殊的YouTube视频加载优化
            // 例如：添加特殊的请求头、使用不同的DNS等
            
            return null; // 返回null使用默认处理
        } catch (Exception e) {
            Log.w(TAG, "Failed to handle YouTube video request", e);
            return null;
        }
    }
    
    /**
     * DNS优化
     */
    private String optimizeDnsUrl(String url) {
        try {
            // 可以在这里实现DNS优化逻辑
            // 例如：使用HttpDNS、CDN加速等
            
            // 示例：YouTube域名优化
            if (url.contains("googlevideo.com")) {
                // 可以替换为更快的CDN节点
                // url = url.replace("rr3---sn-", "rr1---sn-");
            }
            
            return url;
        } catch (Exception e) {
            Log.w(TAG, "DNS optimization failed", e);
            return url;
        }
    }
    
    /**
     * 检查域名是否允许
     */
    private boolean isDomainAllowed(String url) {
        try {
            if (url == null) return false;
            
            // 如果白名单为空，允许所有域名
            if (mDomainWhitelist.isEmpty()) return true;
            
            for (String domain : mDomainWhitelist.keySet()) {
                if (url.contains(domain)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error checking domain whitelist", e);
            return true; // 出错时允许访问
        }
    }
    
    /**
     * 检查是否是广告请求
     */
    private boolean isAdRequest(String url) {
        if (url == null) return false;
        
        String lowerUrl = url.toLowerCase();
        String[] adKeywords = {
            "doubleclick", "googleads", "googlesyndication", 
            "googletagmanager", "google-analytics", "/ads/",
            "adsystem", "adsense"
        };
        
        for (String keyword : adKeywords) {
            if (lowerUrl.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 读取输入流为字节数组
     */
    private byte[] readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        return buffer.toByteArray();
    }
    
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Log.w(TAG, "Received error: " + error.getDescription() + " for " + request.getUrl());
        }
        
        // 显示自定义错误页面
        if (mCustomErrorPage != null) {
            view.loadData(mCustomErrorPage, "text/html", "UTF-8");
        }
    }
    
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        Log.w(TAG, "SSL Error: " + error.toString());
        
        // 可以根据需要选择处理SSL错误的策略
        // handler.proceed(); // 忽略SSL错误（不推荐用于生产环境）
        handler.cancel(); // 取消请求（安全选择）
    }
    
    // ===== 配置方法 =====
    
    public void setCacheEnabled(boolean enabled) {
        mCacheEnabled = enabled;
        Log.d(TAG, "Cache " + (enabled ? "enabled" : "disabled"));
    }
    
    public void setVideoOptimizationEnabled(boolean enabled) {
        mVideoOptimizationEnabled = enabled;
        Log.d(TAG, "Video optimization " + (enabled ? "enabled" : "disabled"));
    }
    
    public void setDnsOptimizationEnabled(boolean enabled) {
        mDnsOptimizationEnabled = enabled;
        Log.d(TAG, "DNS optimization " + (enabled ? "enabled" : "disabled"));
    }
    
    public void setCustomErrorPage(String errorPageHtml) {
        mCustomErrorPage = errorPageHtml;
    }
    
    public void addDomainToWhitelist(String domain) {
        mDomainWhitelist.put(domain, true);
        Log.d(TAG, "Added domain to whitelist: " + domain);
    }
    
    public void clearDomainWhitelist() {
        mDomainWhitelist.clear();
        Log.d(TAG, "Domain whitelist cleared");
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return mCacheInterceptor.getCacheStats();
    }
    
    /**
     * 清理所有缓存
     */
    public void clearCache() {
        mCacheInterceptor.clearAllCache();
    }
}