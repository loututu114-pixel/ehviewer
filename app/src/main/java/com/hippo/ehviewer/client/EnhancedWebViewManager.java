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
import android.util.Log;
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
    private ConnectionRetryManager mRetryManager;

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
        this.mRetryManager = new ConnectionRetryManager();

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

        // User Agent设置 - 避免与桌面/移动模式切换冲突
        // 不在这里设置固定的UA，让上层Activity根据用户选择设置
        // String userAgent = "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 EhViewer/2.0";
        // webSettings.setUserAgentString(userAgent);

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
        
        // 允许通过网络加载资源 - 增强连接稳定性
        webSettings.setBlockNetworkLoads(false);
        webSettings.setBlockNetworkImage(false);
        
        // 彻底禁用所有安全浏览功能，确保最大兼容性
        disableAllSafeBrowsingFeatures(webSettings);
        
        // 增强连接稳定性设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(false); // 关闭安全浏览可能的干扰
        }

        // 设置缓存模式以提升连接稳定性
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 增强连接稳定性 - 新增设置
        enhanceConnectionStability(webSettings);

            // 添加JavaScript接口
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "EhViewer");
        
        // 添加Android接口供VideoPlayerEnhancer使用
        mWebView.addJavascriptInterface(new VideoJavaScriptInterface(), "Android");

        // 设置密码管理器引用
        mPasswordManager = PasswordManager.getInstance(mContext);
        
        // 初始化广告拦截管理器
        AdBlockManager adBlockManager = AdBlockManager.getInstance();
        adBlockManager.initialize(mContext);
    }
    
    /**
     * 彻底禁用所有安全浏览功能
     */
    private void disableAllSafeBrowsingFeatures(WebSettings webSettings) {
        try {
            // 禁用安全浏览
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webSettings.setSafeBrowsingEnabled(false);
            }
            
            // 允许所有混合内容
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            
            // 允许所有文件访问
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            
            // 允许JavaScript全功能
            webSettings.setJavaScriptEnabled(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            
            // 禁用网络封锁
            webSettings.setBlockNetworkLoads(false);
            webSettings.setBlockNetworkImage(false);
            
            Log.d(TAG, "All safe browsing features disabled for maximum compatibility");
        } catch (Exception e) {
            Log.e(TAG, "Error disabling safe browsing features", e);
        }
    }

    /**
     * 增强连接稳定性设置
     */
    private void enhanceConnectionStability(WebSettings webSettings) {
        try {
            // 设置连接超时和读取超时
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 允许混合内容以避免SSL重定向问题
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }

            // 启用应用缓存以减少网络请求
            try {
                java.lang.reflect.Method setAppCacheEnabled = webSettings.getClass().getMethod("setAppCacheEnabled", boolean.class);
                java.lang.reflect.Method setAppCachePath = webSettings.getClass().getMethod("setAppCachePath", String.class);
                java.lang.reflect.Method setAppCacheMaxSize = webSettings.getClass().getMethod("setAppCacheMaxSize", long.class);

                setAppCacheEnabled.invoke(webSettings, true);
                setAppCachePath.invoke(webSettings, mContext.getCacheDir().getAbsolutePath());
                setAppCacheMaxSize.invoke(webSettings, 50 * 1024 * 1024L); // 50MB缓存
            } catch (Exception e) {
                Log.w(TAG, "App cache settings not available", e);
            }

            // 设置DNS预解析
            webSettings.setLoadsImagesAutomatically(true);

            // 优化缓存策略
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

            // 设置数据库和本地存储
            webSettings.setDatabaseEnabled(true);
            webSettings.setDomStorageEnabled(true);

            // 允许地理位置（某些网站需要）
            webSettings.setGeolocationEnabled(true);

            // 设置更长的超时时间
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // 这些设置可能在不同Android版本中有不同的表现
                try {
                    java.lang.reflect.Field field = webSettings.getClass().getDeclaredField("mMaximumDecodedImageSizeBytes");
                    field.setAccessible(true);
                    field.setLong(webSettings, 10 * 1024 * 1024L); // 10MB
                } catch (Exception e) {
                    Log.w(TAG, "Failed to set maximum decoded image size", e);
                }
            }

            Log.d(TAG, "Connection stability enhancements applied");

        } catch (Exception e) {
            Log.e(TAG, "Failed to enhance connection stability", e);
        }
    }

    /**
     * 处理URL scheme
     */
    private boolean handleUrlScheme(WebView view, String url) {
        try {
            return AppLauncher.handleUniversalUrl(mContext, url);
        } catch (Exception e) {
            Log.e(TAG, "Error handling URL scheme: " + url, e);
            return false;
        }
    }
    
    /**
     * 从自定义scheme中提取HTTP URL
     */
    private String extractHttpFromCustomScheme(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return null;
            }
            
            // 处理TikTok scheme
            if (url.contains("snssdk1180://") || url.contains("tiktok://")) {
                // 提取参数中的URL
                if (url.contains("url=")) {
                    String[] parts = url.split("url=");
                    if (parts.length > 1) {
                        String extractedUrl = java.net.URLDecoder.decode(parts[1], "UTF-8");
                        if (extractedUrl.startsWith("http")) {
                            return extractedUrl;
                        }
                    }
                }
                // 默认跳转到TikTok网页版
                return "https://www.tiktok.com";
            }
            
            // 处理其他常见scheme
            if (url.startsWith("intent://")) {
                try {
                    android.content.Intent intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME);
                    String fallbackUrl = intent.getStringExtra("S.browser_fallback_url");
                    if (fallbackUrl != null && fallbackUrl.startsWith("http")) {
                        return fallbackUrl;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Failed to parse intent URL: " + url);
                }
            }
            
            // 查找内嵌HTTP URL
            if (url.contains("http://") || url.contains("https://")) {
                int httpIndex = url.indexOf("http");
                String candidate = url.substring(httpIndex);
                // 去除可能的参数
                if (candidate.contains("&")) {
                    candidate = candidate.split("&")[0];
                }
                if (candidate.contains("?") && candidate.indexOf("?") > candidate.indexOf("://")) {
                    // 保留查询参数
                }
                return candidate;
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting HTTP URL from scheme: " + url, e);
            return null;
        }
    }
    
    /**
     * 注入元素选择和屏蔽JavaScript
     */
    public void injectElementBlockingScript(String currentDomain) {
        if (mWebView == null) return;
        
        AdBlockManager adBlockManager = AdBlockManager.getInstance();
        String domain = adBlockManager.normalizeDomain(currentDomain != null ? currentDomain : mWebView.getUrl());
        
        String script = generateElementBlockingScript(domain);
        
        mWebView.evaluateJavascript(script, null);
        Log.d(TAG, "Injected element blocking script for domain: " + domain);
    }
    
    /**
     * 应用CSS屏蔽规则
     */
    public void applyElementBlocking(String currentDomain) {
        if (mWebView == null) return;
        
        AdBlockManager adBlockManager = AdBlockManager.getInstance();
        String domain = adBlockManager.normalizeDomain(currentDomain != null ? currentDomain : mWebView.getUrl());
        String css = adBlockManager.generateBlockCSS(domain);
        
        if (!css.isEmpty()) {
            String script = "(function() {" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.innerHTML = '" + css.replace("'", "\\\\''") + "';" +
                "document.head.appendChild(style);" +
                "})();";
            
            mWebView.evaluateJavascript(script, null);
            Log.d(TAG, "Applied element blocking CSS for domain: " + domain);
        }
    }
    
    /**
     * 生成元素选择和屏蔽JavaScript代码
     */
    private String generateElementBlockingScript(String domain) {
        return "(function() {" +
            "var isSelectionMode = false;" +
            "var overlay = null;" +
            "var selectedElement = null;" +
            "var originalStyles = new Map();" +
            "\n" +
            "function createOverlay() {" +
            "  overlay = document.createElement('div');" +
            "  overlay.style.cssText = 'position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(255, 0, 0, 0.1); z-index: 999999; pointer-events: none; display: none;';" +
            "  document.body.appendChild(overlay);" +
            "}" +
            "\n" +
            "function highlightElement(element) {" +
            "  if (selectedElement && originalStyles.has(selectedElement)) {" +
            "    selectedElement.style.outline = originalStyles.get(selectedElement);" +
            "  }" +
            "  selectedElement = element;" +
            "  originalStyles.set(element, element.style.outline || '');" +
            "  element.style.outline = '3px solid red';" +
            "}" +
            "\n" +
            "function generateCSSSelector(element) {" +
            "  if (element.id) {" +
            "    return '#' + element.id;" +
            "  }" +
            "  if (element.className && typeof element.className === 'string') {" +
            "    var classes = element.className.trim().split(/\\s+/);" +
            "    if (classes.length > 0) {" +
            "      return '.' + classes.join('.');" +
            "    }" +
            "  }" +
            "  var tag = element.tagName.toLowerCase();" +
            "  var parent = element.parentElement;" +
            "  if (parent) {" +
            "    var siblings = Array.from(parent.children).filter(e => e.tagName === element.tagName);" +
            "    if (siblings.length > 1) {" +
            "      var index = siblings.indexOf(element) + 1;" +
            "      return generateCSSSelector(parent) + ' > ' + tag + ':nth-child(' + index + ')';" +
            "    } else {" +
            "      return generateCSSSelector(parent) + ' > ' + tag;" +
            "    }" +
            "  }" +
            "  return tag;" +
            "}" +
            "\n" +
            "function handleElementClick(event) {" +
            "  if (!isSelectionMode) return;" +
            "  event.preventDefault();" +
            "  event.stopPropagation();" +
            "  highlightElement(event.target);" +
            "}" +
            "\n" +
            "function handleLongPress(event) {" +
            "  if (!isSelectionMode || !selectedElement) return;" +
            "  event.preventDefault();" +
            "  event.stopPropagation();" +
            "  \n" +
            "  var cssSelector = generateCSSSelector(selectedElement);" +
            "  if (confirm('确定要永久屏蔽这个元素吗？\\n选择器：' + cssSelector)) {" +
            "    EhViewer.blockElement('" + domain + "', cssSelector);" +
            "    selectedElement.style.display = 'none';" +
            "    exitSelectionMode();" +
            "  }" +
            "}" +
            "\n" +
            "function enterSelectionMode() {" +
            "  isSelectionMode = true;" +
            "  if (!overlay) createOverlay();" +
            "  overlay.style.display = 'block';" +
            "  document.addEventListener('click', handleElementClick, true);" +
            "  document.addEventListener('contextmenu', handleLongPress, true);" +
            "  document.body.style.userSelect = 'none';" +
            "}" +
            "\n" +
            "function exitSelectionMode() {" +
            "  isSelectionMode = false;" +
            "  if (overlay) overlay.style.display = 'none';" +
            "  document.removeEventListener('click', handleElementClick, true);" +
            "  document.removeEventListener('contextmenu', handleLongPress, true);" +
            "  if (selectedElement && originalStyles.has(selectedElement)) {" +
            "    selectedElement.style.outline = originalStyles.get(selectedElement);" +
            "    originalStyles.delete(selectedElement);" +
            "  }" +
            "  selectedElement = null;" +
            "  document.body.style.userSelect = '';" +
            "}" +
            "\n" +
            "// 全局函数" +
            "window.startElementSelection = enterSelectionMode;" +
            "window.stopElementSelection = exitSelectionMode;" +
            "\n" +
            "console.log('元素屏蔽功能已加载，调用 startElementSelection() 开始选择');" +
            "})();";
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
                
                // 清除成功加载页面的重试信息
                clearRetryInfoOnSuccess(url);
                
                if (mProgressCallback != null) {
                    mProgressCallback.onPageFinished(url, view.getTitle());
                }

                // 保存历史记录
                saveHistoryRecord(url, view.getTitle());

                // 注入增强功能脚本
                injectEnhancedScripts(view);
                
                // 注入Ajax兼容性脚本
                injectAjaxCompatibilityScript(view);
                
                // 注入广告屏蔽功能
                view.postDelayed(() -> {
                    injectElementBlockingScript(url);
                    applyElementBlocking(url);
                }, 1000); // 延迟1秒确保页面完全加载
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                
                Log.e(TAG, "WebView error: Code=" + errorCode + ", Description=" + description + ", URL=" + failingUrl);
                
                // 处理URL scheme错误
                if (description.contains("ERR_UNKNOWN_URL_SCHEME") || 
                    description.contains("net::ERR_UNKNOWN_URL_SCHEME")) {
                    handleUrlSchemeError(view, failingUrl);
                    return;
                }
                
                // 增强的连接错误处理，使用智能重试机制
                if (isConnectionError(errorCode, description)) {
                    if (mRetryManager.shouldRetry(failingUrl, description)) {
                        long retryDelay = mRetryManager.getRetryDelay(failingUrl);
                        int retryCount = mRetryManager.getRetryCount(failingUrl);
                        
                        Log.d(TAG, "Scheduling intelligent retry #" + retryCount + " for: " + failingUrl + " after " + retryDelay + "ms");
                        
                        view.postDelayed(() -> {
                            Log.d(TAG, "Performing enhanced retry for: " + failingUrl);
                            performEnhancedRetry(view, failingUrl);
                        }, retryDelay);
                        return;
                    } else {
                        Log.w(TAG, "Max retry attempts reached for: " + failingUrl);
                        mRetryManager.clearRetryInfo(failingUrl);
                    }
                }
                
                // 通知错误回调
                if (mErrorCallback != null) {
                    mErrorCallback.onReceivedError(errorCode, description, failingUrl);
                }

                // 显示增强的错误页面
                showEnhancedErrorPage(view, errorCode, description, failingUrl);
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
                // 处理权限请求 - 增强视频播放支持
                String[] resources = request.getResources();
                java.util.List<String> grantedResources = new java.util.ArrayList<>();
                
                for (String resource : resources) {
                    android.util.Log.d(TAG, "Permission requested: " + resource);
                    
                    // 自动授权媒体相关权限
                    if (android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource) ||
                        android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource) ||
                        android.webkit.PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                        grantedResources.add(resource);
                        android.util.Log.d(TAG, "Auto-granted permission: " + resource);
                    }
                }
                
                if (!grantedResources.isEmpty()) {
                    request.grant(grantedResources.toArray(new String[0]));
                } else {
                    // 如果没有可授权的权限，仍然尝试授权所有请求的权限以支持视频播放
                    request.grant(resources);
                    android.util.Log.d(TAG, "Granted all requested permissions for video playback");
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
            "        video.setAttribute('autoplay', 'false');" +
            "        video.muted = false;" +
            "        " +
            "        // 设置视频样式确保正确显示" +
            "        video.style.width = '100%';" +
            "        video.style.height = 'auto';" +
            "        video.style.maxWidth = '100%';" +
            "        video.style.display = 'block';" +
            "        video.style.objectFit = 'contain';" +
            "        " +
            "        // xvideos特殊处理" +
            "        if (window.location.hostname.indexOf('xvideos') !== -1) {" +
            "            console.log('Applying xvideos video enhancements');" +
            "            video.style.position = 'relative';" +
            "            video.style.zIndex = '999999';" +
            "            " +
            "            // 修复xvideos的CSS冲突" +
            "            var parent = video.parentElement;" +
            "            while (parent) {" +
            "                if (parent.style) {" +
            "                    parent.style.position = 'relative';" +
            "                    parent.style.overflow = 'visible';" +
            "                }" +
            "                parent = parent.parentElement;" +
            "            }" +
            "        }" +
            "        " +
            "        // YouTube Shorts特殊处理" +
            "        if (window.location.hostname.indexOf('youtube') !== -1 || window.location.hostname.indexOf('youtu.be') !== -1) {" +
            "            console.log('Applying YouTube enhancements');" +
            "            video.style.maxHeight = '100vh';" +
            "            " +
            "            // 强制启用控件" +
            "            video.controls = true;" +
            "            video.setAttribute('controlsList', '');" +
            "            " +
            "            // 处理YouTube的特殊播放器" +
            "            setTimeout(function() {" +
            "                var ytPlayer = document.querySelector('.html5-video-player');" +
            "                if (ytPlayer) {" +
            "                    ytPlayer.style.position = 'relative !important';" +
            "                    ytPlayer.style.zIndex = '999999 !important';" +
            "                }" +
            "            }, 1000);" +
            "        }" +
            "        " +
            "        // 添加全屏双击事件" +
            "        video.addEventListener('dblclick', function(e) {" +
            "            console.log('Video double-clicked for fullscreen');" +
            "            e.preventDefault();" +
            "            e.stopPropagation();" +
            "            " +
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
            "        // 添加点击播放事件（避免与双击冲突）" +
            "        var clickTimer = null;" +
            "        video.addEventListener('click', function(e) {" +
            "            var self = this;" +
            "            if (clickTimer) {" +
            "                clearTimeout(clickTimer);" +
            "                clickTimer = null;" +
            "                return; // 双击时不执行单击" +
            "            }" +
            "            " +
            "            clickTimer = setTimeout(function() {" +
            "                console.log('Video single-clicked');" +
            "                if (self.paused) {" +
            "                    self.play().catch(function(error) {" +
            "                        console.error('Video play failed:', error);" +
            "                        // 尝试绕过autoplay限制" +
            "                        if (error.name === 'NotAllowedError') {" +
            "                            self.muted = true;" +
            "                            self.play().then(function() {" +
            "                                console.log('Video playing with muted audio');" +
            "                            });" +
            "                        }" +
            "                    });" +
            "                } else {" +
            "                    self.pause();" +
            "                }" +
            "                clickTimer = null;" +
            "            }, 200);" +
            "        });" +
            "        " +
            "        // 添加加载事件监听" +
            "        video.addEventListener('loadstart', function() {" +
            "            console.log('Video loading started:', this.src);" +
            "        });" +
            "        " +
            "        video.addEventListener('canplay', function() {" +
            "            console.log('Video can start playing');" +
            "        });" +
            "        " +
            "        video.addEventListener('error', function(e) {" +
            "            console.error('Video error:', e, 'Source:', this.src);" +
            "            // 尝试重新加载视频" +
            "            setTimeout((function(vid) {" +
            "                return function() {" +
            "                    console.log('Attempting video reload...');" +
            "                    vid.load();" +
            "                };" +
            "            })(this), 2000);" +
            "        });" +
            "        " +
            "        // 监听全屏变化事件" +
            "        video.addEventListener('fullscreenchange', function() {" +
            "            console.log('Video fullscreen state changed');" +
            "        });" +
            "        " +
            "        // 强制重新加载有问题的视频" +
            "        if (video.readyState === 0 && video.src) {" +
            "            setTimeout(function() {" +
            "                if (video.readyState === 0) {" +
            "                    console.log('Forcing video reload due to loading failure');" +
            "                    video.load();" +
            "                }" +
            "            }, 3000);" +
            "        }" +
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
     * 处理URL scheme错误
     */
    private void handleUrlSchemeError(WebView view, String failingUrl) {
        Log.d(TAG, "Handling unknown URL scheme: " + failingUrl);
        
        // 尝试使用AppLauncher处理特殊scheme
        if (handleUrlScheme(view, failingUrl)) {
            Log.d(TAG, "URL scheme handled successfully: " + failingUrl);
            return;
        }
        
        // 尝试提取HTTP URL
        String extractedUrl = extractHttpFromCustomScheme(failingUrl);
        if (extractedUrl != null && !extractedUrl.equals(failingUrl)) {
            Log.d(TAG, "Extracted HTTP URL: " + extractedUrl);
            view.loadUrl(extractedUrl);
            return;
        }
        
        // 如果都失败了，显示错误页面
        showEnhancedErrorPage(view, WebViewClient.ERROR_UNKNOWN, "Unknown URL scheme", failingUrl);
    }

    /**
     * 检查是否为连接错误
     */
    private boolean isConnectionError(int errorCode, String description) {
        return errorCode == WebViewClient.ERROR_CONNECT || 
               errorCode == WebViewClient.ERROR_TIMEOUT ||
               errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
               description.contains("ERR_CONNECTION_CLOSED") ||
               description.contains("ERR_CONNECTION_RESET") ||
               description.contains("ERR_CONNECTION_REFUSED") ||
               description.contains("ERR_NETWORK_CHANGED") ||
               description.contains("ERR_CLEARTEXT_NOT_PERMITTED") ||
               description.contains("ERR_CONNECTION_TIMED_OUT") ||
               description.contains("ERR_NETWORK_ACCESS_DENIED");
    }

    /**
     * 执行增强的重试策略
     */
    private void performEnhancedRetry(WebView view, String failingUrl) {
        try {
            Log.d(TAG, "Starting enhanced retry for: " + failingUrl);

            // 1. 清除各种缓存以获得最佳连接
            view.clearCache(true);
            view.clearHistory();

            // 2. 临时调整WebView设置以提高连接成功率
            WebSettings settings = view.getSettings();
            int originalCacheMode = settings.getCacheMode();
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 强制刷新

            // 3. 准备增强的请求头 - 专门针对连接关闭错误优化
            Map<String, String> headers = new HashMap<>();
            headers.put("Connection", "keep-alive");
            headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.put("Pragma", "no-cache");
            headers.put("Expires", "0");

            // 添加Keep-Alive相关头
            headers.put("Keep-Alive", "timeout=30, max=1000");

            // 添加Accept-Encoding以支持压缩
            headers.put("Accept-Encoding", "gzip, deflate, br");

            // 4. 使用系统默认UA - 不伪造UA，尊重网站的适配逻辑
            // 只在必要时添加应用标识，避免干扰网站的正常显示
            if (view != null && view.getSettings() != null) {
                String currentUA = view.getSettings().getUserAgentString();
                if (currentUA != null && !currentUA.isEmpty()) {
                    // 如果当前有UA，只添加最小标识，不改变UA内容
                    if (!currentUA.contains("EhViewer")) {
                        headers.put("User-Agent", currentUA + " EhViewer/2.0");
                    } else {
                        headers.put("User-Agent", currentUA);
                    }
                } else {
                    // 使用系统默认UA，不进行任何修改
                    headers.put("User-Agent", WebSettings.getDefaultUserAgent(view.getContext()));
                }
            } else {
                // 使用系统默认UA
                headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            }

            // 5. 对于连接关闭错误，添加特殊的重试策略
            int retryCount = mRetryManager.getRetryCount(failingUrl);
            if (retryCount > 1) {
                // 多次重试后，使用更保守的策略
                headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                headers.put("Upgrade-Insecure-Requests", "1");

                // 延迟更长时间
                long delay = Math.min(5000 + (retryCount * 2000), 30000); // 最大30秒
                Log.d(TAG, "Using conservative retry strategy with " + delay + "ms delay");

                view.postDelayed(() -> {
                    try {
                        Log.d(TAG, "Performing conservative retry #" + retryCount + " for: " + failingUrl);
                        view.loadUrl(failingUrl, headers);
                    } catch (Exception e) {
                        Log.e(TAG, "Error during conservative retry", e);
                    }
                }, delay);
            } else {
                // 首次重试，直接加载
                Log.d(TAG, "Performing immediate enhanced retry for: " + failingUrl);
                view.loadUrl(failingUrl, headers);
            }

            // 6. 恢复原始缓存设置 - 延迟更长时间
            view.postDelayed(() -> {
                try {
                    settings.setCacheMode(originalCacheMode);
                    Log.d(TAG, "Restored original cache mode after retry");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to restore cache mode", e);
                }
            }, 10000); // 10秒后恢复

        } catch (Exception e) {
            Log.e(TAG, "Error during enhanced retry", e);
            // 回退到简单重试
            view.loadUrl(failingUrl);
        }
    }

    /**
     * 显示增强的错误页面
     */
    private void showEnhancedErrorPage(WebView view, int errorCode, String description, String failingUrl) {
        String errorHtml = generateEnhancedErrorPageHtml(errorCode, description, failingUrl);
        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    /**
     * 生成增强的错误页面HTML
     */
    private String generateEnhancedErrorPageHtml(int errorCode, String description, String failingUrl) {
        String errorType = getErrorTypeDescription(errorCode);
        String errorAdvice = getErrorAdvice(errorCode, description);
        int retryCount = mRetryManager.getRetryCount(failingUrl);
        
        String errorHtml = "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>" +
                "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;margin:0;padding:20px;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;display:flex;align-items:center;justify-content:center;}" +
                ".error-container{background:rgba(255,255,255,0.95);padding:30px;border-radius:15px;box-shadow:0 10px 30px rgba(0,0,0,0.2);max-width:500px;width:100%;text-align:center;}" +
                "h1{color:#e74c3c;font-size:2.5em;margin:0 0 20px 0;text-shadow:2px 2px 4px rgba(0,0,0,0.1);}" +
                ".error-type{color:#2c3e50;font-size:1.2em;font-weight:bold;margin:15px 0;}" +
                ".error-desc{color:#7f8c8d;font-size:0.9em;margin:10px 0;line-height:1.4;}" +
                ".url-container{background:#ecf0f1;padding:10px;border-radius:8px;margin:15px 0;word-break:break-all;font-size:0.8em;color:#34495e;}" +
                ".button{padding:12px 20px;margin:8px;border:none;border-radius:25px;cursor:pointer;font-size:14px;font-weight:bold;transition:all 0.3s ease;text-decoration:none;display:inline-block;}" +
                ".btn-primary{background:linear-gradient(45deg,#3498db,#2980b9);color:white;}" +
                ".btn-secondary{background:linear-gradient(45deg,#95a5a6,#7f8c8d);color:white;}" +
                ".btn-success{background:linear-gradient(45deg,#27ae60,#229954);color:white;}" +
                ".btn-warning{background:linear-gradient(45deg,#f39c12,#e67e22);color:white;}" +
                ".button:hover{transform:translateY(-2px);box-shadow:0 5px 15px rgba(0,0,0,0.2);}" +
                ".retry-info{background:#fff3cd;border:1px solid #ffeaa7;color:#856404;padding:10px;border-radius:8px;margin:15px 0;font-size:0.85em;}" +
                ".advice{background:#d1ecf1;border:1px solid #bee5eb;color:#0c5460;padding:15px;border-radius:8px;margin:20px 0;font-size:0.9em;text-align:left;}" +
                "</style></head><body>" +
                "<div class=\"error-container\">" +
                "<h1>🌐 连接异常</h1>" +
                "<div class=\"error-type\">" + errorType + "</div>" +
                "<div class=\"error-desc\">错误代码: " + errorCode + "</div>" +
                "<div class=\"error-desc\">" + description + "</div>" +
                "<div class=\"url-container\">📍 " + failingUrl + "</div>";

        if (retryCount > 0) {
            errorHtml += "<div class=\"retry-info\">⚠️ 已自动重试 " + retryCount + " 次，仍然无法连接</div>";
        }

        if (!errorAdvice.isEmpty()) {
            errorHtml += "<div class=\"advice\">💡 <strong>建议:</strong><br>" + errorAdvice + "</div>";
        }

        errorHtml += "<div style=\"margin-top:25px;\">" +
                    "<button class=\"button btn-primary\" onclick=\"smartRetry()\">🔄 智能重试</button>" +
                    "<button class=\"button btn-secondary\" onclick=\"location.reload()\">⚡ 强制刷新</button><br>" +
                    "<button class=\"button btn-secondary\" onclick=\"history.back()\">⬅️ 返回上页</button>" +
                    "<button class=\"button btn-success\" onclick=\"window.location.href='https://www.baidu.com'\">🏠 访问百度</button>" +
                    "</div>" +
                    "</div>" +
                    "<script>" +
                    "function smartRetry() {" +
                    "    document.querySelector('.error-container').innerHTML = '<h1>🔄 智能重试中...</h1><p>正在尝试重新连接，请稍候</p>';" +
                    "    setTimeout(() => {" +
                    "        const url = '" + failingUrl + "';" +
                    "        window.location.href = url;" +
                    "    }, 2000);" +
                    "}" +
                    "</script>" +
                    "</body></html>";

        return errorHtml;
    }

    /**
     * 获取错误类型描述
     */
    private String getErrorTypeDescription(int errorCode) {
        switch (errorCode) {
            case WebViewClient.ERROR_CONNECT:
                return "连接服务器失败";
            case WebViewClient.ERROR_TIMEOUT:
                return "连接超时";
            case WebViewClient.ERROR_HOST_LOOKUP:
                return "域名解析失败";
            case WebViewClient.ERROR_UNKNOWN:
                return "未知错误";
            case WebViewClient.ERROR_BAD_URL:
                return "网址格式错误";
            case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
                return "不支持的协议";
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                return "SSL握手失败";
            default:
                return "网络连接异常";
        }
    }

    /**
     * 获取错误建议
     */
    private String getErrorAdvice(int errorCode, String description) {
        if (description.contains("ERR_CONNECTION_CLOSED")) {
            return "服务器主动关闭连接，可能是服务器繁忙、网络不稳定或连接超时。<br><br>" +
                   "建议解决方案：<br>" +
                   "• 等待30秒后重试<br>" +
                   "• 检查网络连接稳定性<br>" +
                   "• 尝试刷新页面<br>" +
                   "• 如果持续出现，可能是服务器问题";
        } else if (description.contains("ERR_CONNECTION_RESET")) {
            return "连接被重置，可能是网络中断、防火墙阻挡或服务器重启。<br><br>" +
                   "建议解决方案：<br>" +
                   "• 检查网络连接<br>" +
                   "• 尝试切换网络（如从WiFi切换到移动数据）<br>" +
                   "• 清除浏览器缓存后重试<br>" +
                   "• 检查是否有VPN或代理干扰";
        } else if (description.contains("ERR_CLEARTEXT_NOT_PERMITTED")) {
            return "应用不允许明文HTTP连接。网站可能需要HTTPS访问。<br><br>" +
                   "建议解决方案：<br>" +
                   "• 尝试访问网站的HTTPS版本<br>" +
                   "• 检查网址是否正确（http:// 改为 https://）<br>" +
                   "• 如果是本地网站，可能需要配置网络安全策略";
        } else if (errorCode == WebViewClient.ERROR_TIMEOUT) {
            return "连接超时，可能是网络较慢、服务器响应缓慢或网络拥堵。<br><br>" +
                   "建议解决方案：<br>" +
                   "• 检查网络连接速度<br>" +
                   "• 等待网络情况改善后重试<br>" +
                   "• 尝试在不同时间段访问<br>" +
                   "• 检查是否需要代理或VPN";
        } else if (errorCode == WebViewClient.ERROR_HOST_LOOKUP) {
            return "无法找到服务器地址，可能是DNS解析问题或网址错误。<br><br>" +
                   "建议解决方案：<br>" +
                   "• 检查网址是否正确<br>" +
                   "• 尝试清除DNS缓存<br>" +
                   "• 切换DNS服务器（如8.8.8.8）<br>" +
                   "• 检查网络设置";
        } else if (errorCode == WebViewClient.ERROR_CONNECT) {
            return "无法连接到服务器，可能是服务器宕机、网络问题或防火墙阻挡。<br><br>" +
                   "建议解决方案：<br>" +
                   "• 检查服务器是否正常运行<br>" +
                   "• 尝试不同的网络环境<br>" +
                   "• 检查防火墙和安全软件设置<br>" +
                   "• 联系网站管理员";
        } else {
            return "发生未知网络错误。<br><br>" +
                   "建议解决方案：<br>" +
                   "• 检查网络连接是否正常<br>" +
                   "• 尝试访问其他网站验证网络状态<br>" +
                   "• 清除浏览器缓存和历史记录<br>" +
                   "• 重启应用后重试";
        }
    }

    /**
     * 清除成功加载页面的重试信息
     */
    private void clearRetryInfoOnSuccess(String url) {
        if (mRetryManager != null && url != null) {
            mRetryManager.clearRetryInfo(url);
        }
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
        
        @JavascriptInterface
        public void blockElement(String domain, String cssSelector) {
            // 处理元素屏蔽请求
            android.util.Log.d(TAG, "Blocking element for " + domain + ": " + cssSelector);
            
            AdBlockManager adBlockManager = AdBlockManager.getInstance();
            adBlockManager.addBlockedElement(domain, cssSelector);
            
            Toast.makeText(mContext, "元素已添加到屏蔽列表", Toast.LENGTH_SHORT).show();
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
