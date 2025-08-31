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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.AppLauncher;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强版WebView管理器
 * 参考YCWebView项目，增强WebView功能
 */
public class EnhancedWebViewManager {

    private static final String TAG = "EnhancedWebViewManager";

    private Context mContext;
    private WebView mWebView;
    private WebViewClient mWebViewClient;
    private WebChromeClient mWebChromeClient;
    private HistoryManager mHistoryManager;
    private PasswordManager mPasswordManager;

    // 文件选择回调
    private ValueCallback<Uri[]> mFilePathCallback;
    private ValueCallback<Uri> mUploadMessage;

    // 进度监听器
    private ProgressCallback mProgressCallback;
    private ErrorCallback mErrorCallback;
    private DownloadCallback mDownloadCallback;
    
    // 视频播放相关
    private VideoPlaybackCallback mVideoPlaybackCallback;
    private View mCustomVideoView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private boolean mIsVideoFullscreen = false;

    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        void onProgressChanged(int progress);
        void onPageStarted(String url);
        void onPageFinished(String url, String title);
        void onReceivedTitle(String title);
        void onReceivedFavicon(Bitmap favicon);
    }

    /**
     * 错误回调接口
     */
    public interface ErrorCallback {
        void onReceivedError(int errorCode, String description, String failingUrl);
        void onReceivedHttpError(int statusCode, String reasonPhrase, String url);
    }

    /**
     * 下载回调接口
     */
    public interface DownloadCallback {
        void onDownloadStart(String url, String userAgent, String contentDisposition,
                           String mimetype, long contentLength);
    }
    
    /**
     * 视频播放回调接口
     */
    public interface VideoPlaybackCallback {
        void onShowVideoFullscreen(View view, WebChromeClient.CustomViewCallback callback);
        void onHideVideoFullscreen();
        boolean isVideoFullscreen();
    }

    public EnhancedWebViewManager(Context context, WebView webView) {
        this(context, webView, null);
    }

    public EnhancedWebViewManager(Context context, WebView webView, HistoryManager historyManager) {
        this.mContext = context;
        this.mWebView = webView;
        this.mHistoryManager = historyManager;

        // 添加null检查
        if (mWebView == null) {
            android.util.Log.e(TAG, "WebView is null, cannot initialize EnhancedWebViewManager");
            return;
        }

        initializeWebView();
        setupWebViewClient();
        setupWebChromeClient();
    }

    /**
     * 初始化WebView设置
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebView() {
        if (mWebView == null) {
            android.util.Log.e(TAG, "Cannot initialize WebView settings: WebView is null");
            return;
        }

        WebSettings webSettings = mWebView.getSettings();

        if (webSettings == null) {
            android.util.Log.e(TAG, "Cannot get WebSettings from WebView");
            return;
        }

        // 基础设置
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // 存储设置
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // 应用缓存设置（兼容性处理）
        try {
            // 这些方法在API 18+中可能不存在或已废弃
            java.lang.reflect.Method setAppCacheEnabled = webSettings.getClass().getMethod("setAppCacheEnabled", boolean.class);
            java.lang.reflect.Method setAppCachePath = webSettings.getClass().getMethod("setAppCachePath", String.class);

            setAppCacheEnabled.invoke(webSettings, true);
            setAppCachePath.invoke(webSettings, mContext.getCacheDir().getAbsolutePath());
        } catch (Exception e) {
            // 忽略反射调用失败
            android.util.Log.w("EnhancedWebViewManager", "App cache settings not available", e);
        }

        // 网络设置
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setBlockNetworkImage(false);

        // 文件访问设置 - 适度放宽以支持视频播放
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // 安全设置 - 允许混合内容以支持更多视频网站
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // 性能优化
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLoadsImagesAutomatically(true);
        }

        // User Agent设置 - 使用桌面版UA以提高兼容性
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 EhViewer/2.0";
        webSettings.setUserAgentString(userAgent);

        // 编码设置
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setDefaultFontSize(16);

        // 硬件加速 - 对视频播放至关重要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            // 启用调试模式
            WebView.setWebContentsDebuggingEnabled(true);
        } else {
            // 低版本使用软件渲染
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        
        // 设置WebView支持视频
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to set mixed content mode", e);
        }

        // 启用地理位置
        webSettings.setGeolocationEnabled(true);
        webSettings.setGeolocationDatabasePath(mContext.getFilesDir().getPath());

        // 启用媒体播放 - 关键设置
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        
        // 视频播放相关设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }
        
        // 启用HTML5视频播放
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        
        // 视频缓存设置
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 允许通过网络加载资源
        webSettings.setBlockNetworkLoads(false);
        webSettings.setBlockNetworkImage(false);

            // 添加JavaScript接口
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "EhViewer");
        
        // 添加Android接口供VideoPlayerEnhancer使用
        mWebView.addJavascriptInterface(new VideoJavaScriptInterface(), "Android");

        // 设置密码管理器引用
        mPasswordManager = PasswordManager.getInstance(mContext);
    }

    /**
     * 设置WebViewClient
     */
    private void setupWebViewClient() {
        mWebViewClient = new WebViewClient() {

@Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // 只记录Ajax请求，不拦截，让JavaScript层处理
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    String url = request.getUrl().toString();
                    if (isAjaxRequest(url, request)) {
                        android.util.Log.d(TAG, "Detected Ajax request (not intercepting): " + url);
                        Map<String, String> headers = request.getRequestHeaders();
                        for (Map.Entry<String, String> header : headers.entrySet()) {
                            android.util.Log.v(TAG, "Request header: " + header.getKey() + " = " + header.getValue());
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (mProgressCallback != null) {
                    mProgressCallback.onPageStarted(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mProgressCallback != null) {
                    mProgressCallback.onPageFinished(url, view.getTitle());
                }

                // 保存历史记录
                saveHistoryRecord(url, view.getTitle());

                // 注入增强功能脚本
                injectEnhancedScripts(view);
                
                // 注入Ajax兼容性脚本
                injectAjaxCompatibilityScript(view);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (mErrorCallback != null) {
                    mErrorCallback.onReceivedError(errorCode, description, failingUrl);
                }

                // 显示错误页面
                showErrorPage(view, errorCode, description, failingUrl);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (mErrorCallback != null && errorResponse != null) {
                    mErrorCallback.onReceivedHttpError(errorResponse.getStatusCode(),
                            errorResponse.getReasonPhrase() != null ? errorResponse.getReasonPhrase() : "Unknown error",
                            request.getUrl().toString());
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlOverride(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrlOverride(view, request.getUrl().toString());
            }
        };

        mWebView.setWebViewClient(mWebViewClient);
    }

    /**
     * 设置WebChromeClient
     */
    private void setupWebChromeClient() {
        mWebChromeClient = new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (mProgressCallback != null) {
                    mProgressCallback.onProgressChanged(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (mProgressCallback != null) {
                    mProgressCallback.onReceivedTitle(title);
                }
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
                if (mProgressCallback != null) {
                    mProgressCallback.onReceivedFavicon(icon);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                return handleFileChooser(filePathCallback, fileChooserParams);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
                // 自动允许地理位置权限
                callback.invoke(origin, true, false);
            }

@Override
            public void onPermissionRequest(android.webkit.PermissionRequest request) {
                // 处理权限请求
                String[] resources = request.getResources();
                for (String resource : resources) {
                    if (android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource) ||
                        android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                        request.grant(resources);
                        break;
                    }
                }
            }
            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // 处理视频全屏播放
                handleVideoFullscreen(view, callback);
            }
            
            @Override
            public void onHideCustomView() {
                // 退出视频全屏播放
                hideVideoFullscreen();
            }
            
            @Override
            public View getVideoLoadingProgressView() {
                // 返回视频加载进度视图
                return createVideoLoadingView();
            }

            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                // 捕获JavaScript控制台消息
                String message = consoleMessage.message();
                String source = consoleMessage.sourceId();
                int line = consoleMessage.lineNumber();
                android.webkit.ConsoleMessage.MessageLevel level = consoleMessage.messageLevel();
                
                String logTag = TAG + "_Console";
                String logMessage = "[" + level + "] " + source + ":" + line + " " + message;
                
                switch (level) {
                    case ERROR:
                        android.util.Log.e(logTag, logMessage);
                        break;
                    case WARNING:
                        android.util.Log.w(logTag, logMessage);
                        break;
                    case LOG:
                    case DEBUG:
                    case TIP:
                    default:
                        android.util.Log.d(logTag, logMessage);
                        break;
                }
                
                return true; // 返回true表示我们已经处理了这个消息
            }
        };

        mWebView.setWebChromeClient(mWebChromeClient);

        // 设置下载监听器
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (mDownloadCallback != null) {
                mDownloadCallback.onDownloadStart(url, userAgent, contentDisposition, mimetype, contentLength);
            } else {
                // 默认下载处理
                handleDownload(url, userAgent, contentDisposition, mimetype, contentLength);
            }
        });
    }

/**
     * 检查是否是Ajax请求
     */
    private boolean isAjaxRequest(String url, WebResourceRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, String> headers = request.getRequestHeaders();
            
            // 更广泛的Ajax请求检测
            boolean hasAjaxPath = url.contains("/ajax/") || 
                                 url.contains("/api/") ||
                                 url.contains("/touch/ajax/") ||
                                 url.contains("comment/illust") || // pixiv评论接口
                                 url.contains("/rpc/") ||
                                 url.contains("?format=json") ||
                                 url.contains("&format=json");
            
            boolean hasAjaxHeaders = headers.containsValue("application/json") ||
                                   headers.containsValue("XMLHttpRequest") ||
                                   headers.containsKey("X-Requested-With");
            
            // pixiv特殊检测 - 查询参数中包含特定关键词
            boolean isPixivAjax = url.contains("pixiv.net") && 
                                (url.contains("work_id=") || 
                                 url.contains("illust_id=") || 
                                 url.contains("page=") || 
                                 url.contains("lang=") ||
                                 url.contains("version="));
            
            return hasAjaxPath || hasAjaxHeaders || isPixivAjax;
        }
        return false;
    }
    
    /**
     * 使用增强的请求头重新发起请求
     */
    private WebResourceResponse makeEnhancedRequest(String url, String method, Map<String, String> originalHeaders) {
        try {
            java.net.URL requestUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) requestUrl.openConnection();
            
            // 设置请求方法
            connection.setRequestMethod(method);
            
            // 复制原始请求头
            for (Map.Entry<String, String> header : originalHeaders.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            
            // 添加增强的请求头
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            connection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            
            // pixiv特殊处理
            if (url.contains("pixiv.net")) {
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("Sec-Fetch-Dest", "empty");
                connection.setRequestProperty("Sec-Fetch-Mode", "cors");
                connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
                
                // 设置正确的Referer - 非常重要
                if (mWebView != null && mWebView.getUrl() != null) {
                    String currentUrl = mWebView.getUrl();
                    connection.setRequestProperty("Referer", currentUrl);
                    connection.setRequestProperty("Origin", "https://www.pixiv.net");
                    android.util.Log.d(TAG, "Setting Referer: " + currentUrl + " for request: " + url);
                }
                
                // 设置正确的User-Agent
                connection.setRequestProperty("User-Agent", 
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            }
            
            // 设置连接超时
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            // 获取响应
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            android.util.Log.d(TAG, "Ajax request response: " + responseCode + " (" + responseMessage + ") for " + url);
            
            // 输出响应头供调试
            Map<String, java.util.List<String>> responseHeaders = connection.getHeaderFields();
            for (Map.Entry<String, java.util.List<String>> header : responseHeaders.entrySet()) {
                android.util.Log.v(TAG, "Response header: " + header.getKey() + " = " + header.getValue());
            }
            
            if (responseCode >= 200 && responseCode < 300) {
                // 成功响应
                String mimeType = connection.getContentType();
                if (mimeType == null) {
                    mimeType = "application/json";
                }
                
                // 提取MIME类型和编码
                String[] parts = mimeType.split(";");
                String actualMimeType = parts[0].trim();
                String encoding = "UTF-8";
                
                for (String part : parts) {
                    if (part.trim().startsWith("charset=")) {
                        encoding = part.trim().substring(8);
                        break;
                    }
                }
                
                return new WebResourceResponse(actualMimeType, encoding, connection.getInputStream());
            } else {
                // 错误响应 - 读取错误信息
                android.util.Log.w(TAG, "Ajax request failed with code: " + responseCode + " (" + responseMessage + ") for " + url);
                
                // 读取错误响应内容
                java.io.InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    try {
                        java.util.Scanner scanner = new java.util.Scanner(errorStream).useDelimiter("\\A");
                        String errorBody = scanner.hasNext() ? scanner.next() : "";
                        android.util.Log.w(TAG, "Error response body: " + errorBody);
                        
                        // 将错误内容返回给WebView
                        java.io.ByteArrayInputStream errorInputStream = 
                            new java.io.ByteArrayInputStream(errorBody.getBytes("UTF-8"));
                        
                        // 转换响应头格式
                        Map<String, String> errorResponseHeaders = new HashMap<>();
                        for (Map.Entry<String, java.util.List<String>> header : connection.getHeaderFields().entrySet()) {
                            if (header.getKey() != null && header.getValue() != null && !header.getValue().isEmpty()) {
                                errorResponseHeaders.put(header.getKey(), header.getValue().get(0));
                            }
                        }
                        
                        return new WebResourceResponse("application/json", "UTF-8", responseCode, responseMessage, 
                            errorResponseHeaders, errorInputStream);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Failed to read error stream", e);
                    }
                }
            }
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to make enhanced request for: " + url, e);
        }
        
        return null;
    }

    /**
     * 处理URL重定向
     */
    private boolean handleUrlOverride(WebView view, String url) {
        // 使用AppLauncher处理各种URL scheme
        return AppLauncher.handleUniversalUrl(mContext, url);
    }

    /**
     * 处理文件选择
     */
    private boolean handleFileChooser(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (mContext instanceof Activity) {
            Activity activity = (Activity) mContext;

            // 检查权限
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
                return false;
            }

            mFilePathCallback = filePathCallback;

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            // 支持多种文件类型
            String[] mimeTypes = {"image/*", "video/*", "audio/*", "application/pdf",
                                "application/msword", "application/vnd.ms-excel"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            activity.startActivityForResult(Intent.createChooser(intent, "选择文件"), 1002);
            return true;
        }

        return false;
    }

    /**
     * 处理文件选择结果
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1002) {
            if (mFilePathCallback != null) {
                Uri[] results = null;

                if (resultCode == Activity.RESULT_OK && data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            }
        }
    }

    /**
     * 处理下载
     */
    private void handleDownload(String url, String userAgent, String contentDisposition,
                               String mimetype, long contentLength) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);

            // 设置下载路径
            String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            // 设置通知
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle(fileName);
            request.setDescription("正在下载文件...");

            // 开始下载
            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(mContext, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Download failed", e);
            Toast.makeText(mContext, "下载失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 注入增强功能脚本
     */
    private void injectEnhancedScripts(WebView view) {
        // 获取当前域名用于密码管理
        String currentUrl = view.getUrl();
        String domain = extractDomainFromUrl(currentUrl);

        String script = "(function() {" +
            "console.log('EhViewer enhanced scripts loaded');" +
            "" +
            "// 增强视频播放功能" +
            "function enhanceVideoPlayers() {" +
            "    console.log('Enhancing video players...');" +
            "    var videos = document.querySelectorAll('video');" +
            "    console.log('Found ' + videos.length + ' video elements');" +
            "    " +
            "    for (var i = 0; i < videos.length; i++) {" +
            "        var video = videos[i];" +
            "        " +
            "        // 确保视频可以播放" +
            "        video.setAttribute('playsinline', 'true');" +
            "        video.setAttribute('webkit-playsinline', 'true');" +
            "        video.setAttribute('controls', 'true');" +
            "        video.setAttribute('preload', 'metadata');" +
            "        " +
            "        // 设置视频样式确保正确显示" +
            "        video.style.width = '100%';" +
            "        video.style.height = 'auto';" +
            "        video.style.maxWidth = '100%';" +
            "        video.style.display = 'block';" +
            "        " +
            "        // 添加全屏双击事件" +
            "        video.addEventListener('dblclick', function(e) {" +
            "            console.log('Video double-clicked for fullscreen');" +
            "            if (this.requestFullscreen) {" +
            "                this.requestFullscreen();" +
            "            } else if (this.webkitRequestFullscreen) {" +
            "                this.webkitRequestFullscreen();" +
            "            } else if (this.mozRequestFullScreen) {" +
            "                this.mozRequestFullScreen();" +
            "            } else if (window.Android && window.Android.requestFullscreen) {" +
            "                window.Android.requestFullscreen();" +
            "            }" +
            "        });" +
            "        " +
            "        // 添加点击播放事件" +
            "        video.addEventListener('click', function(e) {" +
            "            console.log('Video clicked');" +
            "            if (this.paused) {" +
            "                this.play().catch(function(error) {" +
            "                    console.error('Video play failed:', error);" +
            "                });" +
            "            } else {" +
            "                this.pause();" +
            "            }" +
            "        });" +
            "        " +
            "        // 添加加载事件监听" +
            "        video.addEventListener('loadstart', function() {" +
            "            console.log('Video loading started');" +
            "        });" +
            "        " +
            "        video.addEventListener('canplay', function() {" +
            "            console.log('Video can start playing');" +
            "        });" +
            "        " +
            "        video.addEventListener('error', function(e) {" +
            "            console.error('Video error:', e);" +
            "        });" +
            "    }" +
            "}" +
            "" +
            "// 页面加载完成后增强视频" +
            "setTimeout(enhanceVideoPlayers, 1000);" +
            "// 监听DOM变化，处理动态加载的视频" +
            "if (typeof MutationObserver !== 'undefined') {" +
            "    var observer = new MutationObserver(function(mutations) {" +
            "        var hasNewVideos = false;" +
            "        mutations.forEach(function(mutation) {" +
            "            if (mutation.addedNodes) {" +
            "                for (var i = 0; i < mutation.addedNodes.length; i++) {" +
            "                    var node = mutation.addedNodes[i];" +
            "                    if (node.tagName === 'VIDEO' || (node.querySelectorAll && node.querySelectorAll('video').length > 0)) {" +
            "                        hasNewVideos = true;" +
            "                        break;" +
            "                    }" +
            "                }" +
            "            }" +
            "        });" +
            "        if (hasNewVideos) {" +
            "            setTimeout(enhanceVideoPlayers, 500);" +
            "        }" +
            "    });" +
            "    observer.observe(document.body, { childList: true, subtree: true });" +
            "}" +
            "" +
            "" +
            "// 密码表单检测和自动填充功能" +
            "var passwordForms = [];" +
            "var passwordFields = document.querySelectorAll('input[type=\"password\"]');" +
            "" +
            "function detectPasswordForms() {" +
            "    passwordForms = [];" +
            "    passwordFields = document.querySelectorAll('input[type=\"password\"]');" +
            "    " +
            "    for (var i = 0; i < passwordFields.length; i++) {" +
            "        var passwordField = passwordFields[i];" +
            "        var form = passwordField.form;" +
            "        if (form && passwordForms.indexOf(form) === -1) {" +
            "            passwordForms.push(form);" +
            "            setupPasswordForm(form, passwordField);" +
            "        }" +
            "    }" +
            "}" +
            "" +
            "function setupPasswordForm(form, passwordField) {" +
            "    // 查找对应的用户名字段" +
            "    var usernameField = findUsernameField(form, passwordField);" +
            "    if (usernameField) {" +
            "        setupAutoFill(usernameField, passwordField);" +
            "        setupPasswordSave(form, usernameField, passwordField);" +
            "    }" +
            "}" +
            "" +
            "function findUsernameField(form, passwordField) {" +
            "    // 查找可能的用户名字段" +
            "    var inputs = form.querySelectorAll('input[type=\"text\"], input[type=\"email\"], input:not([type])');" +
            "    for (var i = 0; i < inputs.length; i++) {" +
            "        var input = inputs[i];" +
            "        var name = (input.name || '').toLowerCase();" +
            "        var id = (input.id || '').toLowerCase();" +
            "        var placeholder = (input.placeholder || '').toLowerCase();" +
            "        " +
            "        if (name.indexOf('user') !== -1 || name.indexOf('email') !== -1 || " +
            "            id.indexOf('user') !== -1 || id.indexOf('email') !== -1 || " +
            "            placeholder.indexOf('user') !== -1 || placeholder.indexOf('email') !== -1) {" +
            "            return input;" +
            "        }" +
            "    }" +
            "    // 如果没找到特定的，返回第一个文本输入框" +
            "    return inputs.length > 0 ? inputs[0] : null;" +
            "}" +
            "" +
            "function setupAutoFill(usernameField, passwordField) {" +
            "    // 为用户名字段添加自动填充提示" +
            "    usernameField.setAttribute('autocomplete', 'username');" +
            "    passwordField.setAttribute('autocomplete', 'current-password');" +
            "    " +
            "    // 添加输入事件监听" +
            "    usernameField.addEventListener('focus', function() {" +
            "        if (window.EhViewer && window.EhViewer.onUsernameFocus) {" +
            "            window.EhViewer.onUsernameFocus('" + domain + "', usernameField.value);" +
            "        }" +
            "    });" +
            "}" +
            "" +
            "function setupPasswordSave(form, usernameField, passwordField) {" +
            "    // 监听表单提交事件" +
            "    form.addEventListener('submit', function(e) {" +
            "        if (window.EhViewer && window.EhViewer.onPasswordSubmit) {" +
            "            var username = usernameField.value;" +
            "            var password = passwordField.value;" +
            "            if (username && password) {" +
            "                window.EhViewer.onPasswordSubmit('" + domain + "', username, password);" +
            "            }" +
            "        }" +
            "    });" +
            "}" +
            "" +
            "// 初始化密码表单检测" +
            "detectPasswordForms();" +
            "" +
            "// 增强图片点击放大功能" +
            "var images = document.getElementsByTagName('img');" +
            "for (var i = 0; i < images.length; i++) {" +
            "    images[i].addEventListener('click', function(e) {" +
            "        if (window.EhViewer && window.EhViewer.onImageClick) {" +
            "            window.EhViewer.onImageClick(e.target.src);" +
            "        }" +
            "    });" +
            "}" +
            "" +
            "// 增强长按功能" +
            "document.addEventListener('contextmenu', function(e) {" +
            "    e.preventDefault();" +
            "    if (window.EhViewer && window.EhViewer.onContextMenu) {" +
            "        var target = e.target;" +
            "        var text = '';" +
            "        var imageUrl = '';" +
            "        " +
            "        if (target.tagName === 'IMG') {" +
            "            imageUrl = target.src;" +
            "        } else {" +
            "            text = window.getSelection().toString();" +
            "        }" +
            "        " +
            "        window.EhViewer.onContextMenu(text, imageUrl, target.tagName);" +
            "    }" +
            "});" +
            "" +
            "// 增强滚动到底部检测" +
            "window.addEventListener('scroll', function() {" +
            "    if (window.innerHeight + window.scrollY >= document.body.offsetHeight) {" +
            "        if (window.EhViewer && window.EhViewer.onScrollToBottom) {" +
            "            window.EhViewer.onScrollToBottom();" +
            "        }" +
            "    }" +
            "});" +
            "" +
            "console.log('Enhanced scripts injection completed');" +
            "})();";

        view.evaluateJavascript(script, null);
    }

    /**
     * 从URL中提取域名
     */
    private String extractDomainFromUrl(String url) {
        if (url == null) return null;

        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                java.net.URL parsedUrl = new java.net.URL(url);
                return parsedUrl.getHost();
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to parse domain from URL: " + url, e);
        }
        return url;
    }

    /**
     * 显示错误页面
     */
    private void showErrorPage(WebView view, int errorCode, String description, String failingUrl) {
        String errorHtml = "<html><head><meta charset=\"UTF-8\"><style>" +
                "body{text-align:center;padding:50px;font-family:Arial,sans-serif;background:#f5f5f5;}" +
                "h1{color:#e74c3c;margin-bottom:20px;}" +
                ".error-info{background:white;padding:20px;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:400px;margin:0 auto;}" +
                ".error-code{color:#666;font-size:14px;margin:10px 0;}" +
                ".error-desc{color:#666;font-size:12px;margin:10px 0;word-break:break-all;}" +
                "button{padding:10px 20px;background:#3498db;color:white;border:none;border-radius:4px;cursor:pointer;margin:10px;}" +
                "button:hover{background:#2980b9;}" +
                "</style></head><body>" +
                "<div class=\"error-info\">" +
                "<h1>⚠️ 加载失败</h1>" +
                "<div class=\"error-code\">错误代码: " + errorCode + "</div>" +
                "<div class=\"error-desc\">" + description + "</div>" +
                "<div class=\"error-desc\" style=\"font-size:10px;\">URL: " + failingUrl + "</div>" +
                "</div>" +
                "<button onclick=\"location.reload()\">🔄 重新加载</button>" +
                "<button onclick=\"history.back()\">⬅️ 返回上一页</button>" +
                "<button onclick=\"window.location.href='https://www.baidu.com'\">🏠 访问百度</button>" +
                "</body></html>";

        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    /**
     * 截图功能
     */
    public Bitmap captureScreenshot() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(mWebView.getWidth(), mWebView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            mWebView.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Screenshot failed", e);
            return null;
        }
    }

    /**
     * 保存截图
     */
    public boolean saveScreenshot(String fileName) {
        try {
            Bitmap bitmap = captureScreenshot();
            if (bitmap == null) return false;

            File screenshotDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EhViewer");
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }

            File screenshotFile = new File(screenshotDir, fileName + ".png");
            FileOutputStream fos = new FileOutputStream(screenshotFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();

            // 通知媒体库
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(screenshotFile));
            mContext.sendBroadcast(mediaScanIntent);

            Toast.makeText(mContext, "截图已保存: " + screenshotFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            android.util.Log.e(TAG, "Save screenshot failed", e);
            Toast.makeText(mContext, "保存截图失败", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * JavaScript接口类
     */
    private class JavaScriptInterface {

        @JavascriptInterface
        public void onImageClick(String imageUrl) {
            // 处理图片点击事件
            android.util.Log.d(TAG, "Image clicked: " + imageUrl);

            // 可以在这里实现图片查看器或下载功能
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(mContext, "无法打开图片", Toast.LENGTH_SHORT).show();
            }
        }

        @JavascriptInterface
        public void onContextMenu(String text, String imageUrl, String tagName) {
            // 处理长按菜单
            android.util.Log.d(TAG, "Context menu - Text: " + text + ", Image: " + imageUrl + ", Tag: " + tagName);

            // 这里可以实现自定义长按菜单
            if (text != null && !text.isEmpty()) {
                // 复制文本
                android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", text));
                    Toast.makeText(mContext, "文本已复制", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @JavascriptInterface
        public void onScrollToBottom() {
            // 处理滚动到底部事件
            android.util.Log.d(TAG, "Scrolled to bottom");

            // 可以在这里实现自动加载更多内容等功能
        }

        @JavascriptInterface
        public void onUsernameFocus(String domain, String currentValue) {
            // 处理用户名字段获得焦点事件
            android.util.Log.d(TAG, "Username field focused for domain: " + domain);

            if (mPasswordManager != null && mPasswordManager.isUnlocked()) {
                // 可以在这里显示自动填充建议
                List<String> suggestions = mPasswordManager.getSuggestedUsernames(domain);
                if (!suggestions.isEmpty()) {
                    android.util.Log.d(TAG, "Available usernames: " + suggestions.size());
                }
            }
        }

        @JavascriptInterface
        public void onPasswordSubmit(String domain, String username, String password) {
            // 处理密码提交事件（自动保存密码）
            android.util.Log.d(TAG, "Password submitted for domain: " + domain);

            if (mPasswordManager != null && mPasswordManager.isUnlocked()) {
                // 检查是否应该保存密码
                PasswordManager.PasswordEntry existing = mPasswordManager.getPassword(domain, username);
                if (existing == null || !existing.password.equals(password)) {
                    // 保存新密码或更新现有密码
                    boolean saved = mPasswordManager.savePassword(domain, username, password);
                    if (saved) {
                        android.util.Log.d(TAG, "Password saved automatically for " + domain);
                        Toast.makeText(mContext, "密码已自动保存", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        @JavascriptInterface
        public void fillPassword(String domain, String username) {
            // 填充密码到当前页面
            if (mPasswordManager != null && mPasswordManager.isUnlocked()) {
                PasswordManager.PasswordEntry entry = mPasswordManager.getPassword(domain, username);
                if (entry != null) {
                    // 使用JavaScript填充密码字段
                    String fillScript = "(function() {" +
                        "var passwordFields = document.querySelectorAll('input[type=\"password\"]');" +
                        "var usernameFields = document.querySelectorAll('input[type=\"text\"], input[type=\"email\"]');" +
                        "" +
                        "if (passwordFields.length > 0) {" +
                        "    passwordFields[0].value = '" + entry.password.replace("'", "\\'") + "';" +
                        "}" +
                        "" +
                        "if (usernameFields.length > 0) {" +
                        "    usernameFields[0].value = '" + entry.username.replace("'", "\\'") + "';" +
                        "}" +
                        "})();";

                    if (mWebView != null) {
                        mWebView.evaluateJavascript(fillScript, null);
                        Toast.makeText(mContext, "密码已自动填充", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /**
     * 设置回调监听器
     */
    public void setProgressCallback(ProgressCallback callback) {
        this.mProgressCallback = callback;
    }

    public void setErrorCallback(ErrorCallback callback) {
        this.mErrorCallback = callback;
    }

    public void setDownloadCallback(DownloadCallback callback) {
        this.mDownloadCallback = callback;
    }
    
    public void setVideoPlaybackCallback(VideoPlaybackCallback callback) {
        this.mVideoPlaybackCallback = callback;
    }

    /**
     * 保存历史记录
     */
    private void saveHistoryRecord(String url, String title) {
        try {
            if (mHistoryManager != null && url != null && !url.isEmpty()) {
                // 检查是否是有效的URL（不是about:blank等）
                if (!url.startsWith("about:") && !url.startsWith("chrome://") && !url.startsWith("data:")) {
                    String pageTitle = title != null && !title.isEmpty() ? title : url;
                    mHistoryManager.addHistory(pageTitle, url);
                    android.util.Log.d(TAG, "History saved: " + pageTitle + " - " + url);
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error saving history record", e);
        }
    }

    /**
     * 注入Ajax兼容性脚本
     */
    private void injectAjaxCompatibilityScript(WebView view) {
        String ajaxScript = "(function() {" +
            "console.log('Injecting Ajax compatibility enhancements for pixiv...');" +
            "" +
            "// 添加全局错误监听" +
            "window.addEventListener('error', function(e) {" +
            "    console.error('Global error:', e.error, e.filename, e.lineno);" +
            "});" +
            "" +
            "// 监听未处理的Promise拒绝" +
            "window.addEventListener('unhandledrejection', function(e) {" +
            "    console.error('Unhandled promise rejection:', e.reason);" +
            "});" +
            "" +
            "// 保存原始XMLHttpRequest" +
            "var originalXHR = window.XMLHttpRequest;" +
            "" +
            "// 创建增强的XMLHttpRequest" +
            "function EnhancedXMLHttpRequest() {" +
            "    var xhr = new originalXHR();" +
            "    var originalOpen = xhr.open;" +
            "    var originalSend = xhr.send;" +
            "    var originalSetRequestHeader = xhr.setRequestHeader;" +
            "    " +
            "    xhr.open = function(method, url, async, user, password) {" +
            "        // 记录请求信息" +
            "        this._method = method;" +
            "        this._url = url;" +
            "        console.log('XHR Request:', method, url);" +
            "        " +
            "        return originalOpen.apply(this, arguments);" +
            "    };" +
            "    " +
            "    xhr.setRequestHeader = function(header, value) {" +
            "        // 确保必要的请求头被设置" +
            "        if (!this._headers) this._headers = {};" +
            "        this._headers[header] = value;" +
            "        " +
            "        return originalSetRequestHeader.apply(this, arguments);" +
            "    };" +
            "    " +
            "    xhr.send = function(data) {" +
            "        console.log('XHR send called for:', this._url);" +
            "        " +
            "        // 为Ajax请求自动添加必要的请求头" +
            "        if (this._url && (this._url.indexOf('/ajax/') !== -1 || this._url.indexOf('/api/') !== -1 || this._url.indexOf('/touch/ajax/') !== -1)) {" +
            "            console.log('Processing Ajax request:', this._url);" +
            "            " +
            "            // 基本请求头" +
            "            if (!this._headers || !this._headers['X-Requested-With']) {" +
            "                try {" +
            "                    originalSetRequestHeader.call(this, 'X-Requested-With', 'XMLHttpRequest');" +
            "                    console.log('Added X-Requested-With header');" +
            "                } catch (e) { console.error('Failed to set X-Requested-With:', e); }" +
            "            }" +
            "            " +
            "            if (!this._headers || !this._headers['Accept']) {" +
            "                try {" +
            "                    originalSetRequestHeader.call(this, 'Accept', 'application/json, text/javascript, */*; q=0.01');" +
            "                    console.log('Added Accept header');" +
            "                } catch (e) { console.error('Failed to set Accept:', e); }" +
            "            }" +
            "            " +
            "            // pixiv特殊处理" +
            "            if (this._url.indexOf('pixiv.net') !== -1) {" +
            "                console.log('Applying pixiv-specific headers');" +
            "                try {" +
            "                    originalSetRequestHeader.call(this, 'Accept', 'application/json');" +
            "                    originalSetRequestHeader.call(this, 'Accept-Language', 'zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7');" +
            "                    originalSetRequestHeader.call(this, 'Cache-Control', 'no-cache');" +
            "                    originalSetRequestHeader.call(this, 'Pragma', 'no-cache');" +
            "                    originalSetRequestHeader.call(this, 'Sec-Fetch-Dest', 'empty');" +
            "                    originalSetRequestHeader.call(this, 'Sec-Fetch-Mode', 'cors');" +
            "                    originalSetRequestHeader.call(this, 'Sec-Fetch-Site', 'same-origin');" +
            "                    " +
            "                    if (window.location.href && !this._headers['Referer']) {" +
            "                        originalSetRequestHeader.call(this, 'Referer', window.location.href);" +
            "                        console.log('Set Referer to:', window.location.href);" +
            "                    }" +
            "                    " +
            "                    if (!this._headers['Origin']) {" +
            "                        originalSetRequestHeader.call(this, 'Origin', 'https://www.pixiv.net');" +
            "                        console.log('Set Origin to: https://www.pixiv.net');" +
            "                    }" +
            "                    " +
            "                    console.log('Applied pixiv headers successfully');" +
            "                } catch (e) { " +
            "                    console.error('Failed to set pixiv headers:', e); " +
            "                }" +
            "            }" +
            "        }" +
            "        " +
            "        // 添加错误处理" +
            "        var originalOnError = this.onerror;" +
            "        this.onerror = function() {" +
            "            console.error('XHR Error for:', xhr._url, 'Status:', xhr.status, 'StatusText:', xhr.statusText);" +
            "            if (originalOnError) originalOnError.apply(this, arguments);" +
            "        };" +
            "        " +
            "        var originalOnLoad = this.onload;" +
            "        this.onload = function() {" +
            "            console.log('XHR Success for:', xhr._url, 'Status:', xhr.status);" +
            "            if (originalOnLoad) originalOnLoad.apply(this, arguments);" +
            "        };" +
            "        " +
            "        return originalSend.apply(this, arguments);" +
            "    };" +
            "    " +
            "    return xhr;" +
            "}" +
            "" +
            "// 替换全局XMLHttpRequest" +
            "window.XMLHttpRequest = EnhancedXMLHttpRequest;" +
            "" +
            "// 增强fetch API" +
            "if (window.fetch) {" +
            "    var originalFetch = window.fetch;" +
            "    window.fetch = function(url, options) {" +
            "        options = options || {};" +
            "        options.headers = options.headers || {};" +
            "        " +
            "        // 检查是否是Ajax/API请求" +
            "        if (typeof url === 'string' && (url.indexOf('/ajax/') !== -1 || url.indexOf('/api/') !== -1 || url.indexOf('/touch/ajax/') !== -1)) {" +
            "            console.log('Processing fetch request:', url);" +
            "            " +
            "            // 添加必要的请求头" +
            "            if (!options.headers['X-Requested-With']) {" +
            "                options.headers['X-Requested-With'] = 'XMLHttpRequest';" +
            "            }" +
            "            " +
            "            if (!options.headers['Accept']) {" +
            "                options.headers['Accept'] = 'application/json, text/javascript, */*; q=0.01';" +
            "            }" +
            "            " +
            "            // pixiv特殊处理" +
            "            if (url.indexOf('pixiv.net') !== -1) {" +
            "                console.log('Applying pixiv-specific fetch headers');" +
            "                options.headers['Accept'] = 'application/json';" +
            "                options.headers['Accept-Language'] = 'zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7';" +
            "                options.headers['Cache-Control'] = 'no-cache';" +
            "                options.headers['Pragma'] = 'no-cache';" +
            "                options.headers['Sec-Fetch-Dest'] = 'empty';" +
            "                options.headers['Sec-Fetch-Mode'] = 'cors';" +
            "                options.headers['Sec-Fetch-Site'] = 'same-origin';" +
            "                " +
            "                if (window.location.href && !options.headers['Referer']) {" +
            "                    options.headers['Referer'] = window.location.href;" +
            "                }" +
            "                " +
            "                if (!options.headers['Origin']) {" +
            "                    options.headers['Origin'] = 'https://www.pixiv.net';" +
            "                }" +
            "            }" +
            "            " +
            "            console.log('Enhanced fetch request:', url, options.headers);" +
            "        }" +
            "        " +
            "        // 添加错误处理" +
            "        return originalFetch.apply(this, arguments).then(function(response) {" +
            "            console.log('Fetch response for:', url, 'Status:', response.status, response.statusText);" +
            "            if (!response.ok) {" +
            "                console.error('Fetch error:', url, response.status, response.statusText);" +
            "            }" +
            "            return response;" +
            "        }).catch(function(error) {" +
            "            console.error('Fetch failed:', url, error);" +
            "            throw error;" +
            "        });" +
            "        } else {" +
            "            // 非-Ajax请求直接返回" +
            "            return originalFetch.apply(this, arguments);" +
            "        }" +
            "    };" +
            "}" +
            "" +
            "// 监听所有网络错误" +
            "window.addEventListener('online', function() { console.log('Network online'); });" +
            "window.addEventListener('offline', function() { console.log('Network offline'); });" +
            "" +
            "console.log('Ajax compatibility enhancements loaded successfully');" +
            "})();";

        view.evaluateJavascript(ajaxScript, null);
    }

    /**
     * 处理视频全屏播放
     */
    private void handleVideoFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
        if (mCustomVideoView != null) {
            // 如果已经有全屏视频，先隐藏
            callback.onCustomViewHidden();
            return;
        }
        
        mCustomVideoView = view;
        mCustomViewCallback = callback;
        mIsVideoFullscreen = true;
        
        android.util.Log.d(TAG, "Video entering fullscreen mode");
        
        if (mVideoPlaybackCallback != null) {
            mVideoPlaybackCallback.onShowVideoFullscreen(view, callback);
        } else {
            // 启动独立的视频播放Activity
            startVideoPlayerActivity(view);
        }
    }
    
    /**
     * 隐藏视频全屏播放
     */
    private void hideVideoFullscreen() {
        if (mCustomVideoView == null) return;
        
        android.util.Log.d(TAG, "Video exiting fullscreen mode");
        
        if (mVideoPlaybackCallback != null) {
            mVideoPlaybackCallback.onHideVideoFullscreen();
        }
        
        mCustomVideoView = null;
        if (mCustomViewCallback != null) {
            mCustomViewCallback.onCustomViewHidden();
            mCustomViewCallback = null;
        }
        mIsVideoFullscreen = false;
    }
    
    /**
     * 启动视频播放Activity
     */
    private void startVideoPlayerActivity(View videoView) {
        try {
            // 这里可以提取视频URL并启动MediaPlayerActivity
            // 由于这是一个自定义视图，我们需要通过JavaScript获取视频信息
            String script = "(function() {" +
                "var videos = document.querySelectorAll('video');" +
                "if (videos.length > 0) {" +
                "    var video = videos[0];" +
                "    return {" +
                "        src: video.currentSrc || video.src," +
                "        title: document.title," +
                "        currentTime: video.currentTime" +
                "    };" +
                "}" +
                "return null;" +
                "})();";
                
            mWebView.evaluateJavascript(script, result -> {
                android.util.Log.d(TAG, "Video info result: " + result);
                // 这里可以解析result并启动MediaPlayerActivity
                // 但由于复杂性，我们暂时使用回调方式
            });
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to start video player", e);
        }
    }
    
    /**
     * 创建视频加载视图
     */
    private View createVideoLoadingView() {
        // 创建一个简单的加载视图
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(mContext);
        progressBar.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER));
        return progressBar;
    }
    
    /**
     * 检查是否处于视频全屏模式
     */
    public boolean isVideoFullscreen() {
        return mIsVideoFullscreen;
    }
    
    /**
     * 退出视频全屏模式
     */
    public void exitVideoFullscreen() {
        if (mIsVideoFullscreen) {
            hideVideoFullscreen();
        }
    }

    /**
     * 视频相关的JavaScript接口
     */
    private class VideoJavaScriptInterface {
        
        @JavascriptInterface
        public void requestFullscreen() {
            android.util.Log.d(TAG, "Video fullscreen requested from JavaScript");
            if (mVideoPlaybackCallback != null) {
                // 这里需要获取视频元素，暂时使用null
                mVideoPlaybackCallback.onShowVideoFullscreen(null, null);
            }
        }
        
        @JavascriptInterface
        public void log(String message) {
            android.util.Log.d(TAG + "_JS", message);
        }
    }

    /**
     * 清理资源
     */
    public void destroy() {
        if (mIsVideoFullscreen) {
            exitVideoFullscreen();
        }
        if (mWebView != null) {
            mWebView.destroy();
        }
    }
}
