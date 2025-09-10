package com.hippo.ehviewer.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowInsetsController;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.os.Build;

import androidx.annotation.Nullable;

import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsVideo;

import java.io.File;

/**
 * 基于YCWebView的全新浏览器管理器
 * 参考YCWebView最佳实践，完全重构浏览器实现
 *
 * 核心特性：
 * 1. 腾讯X5内核深度优化
 * 2. 完善的视频播放支持
 * 3. 文件加载功能(Word,Excel,PPT,PDF等)
 * 4. JS交互无耦合设计
 * 5. 进度条和错误状态监听
 * 6. 应用间跳转支持
 */
public class YCWebViewBrowserManager {
    private static final String TAG = "YCWebViewBrowser";

    private Context mContext;
    private android.webkit.WebView mSystemWebView;
    private com.tencent.smtt.sdk.WebView mX5WebView;
    private boolean mIsX5Available = false;

    // 全屏视频相关字段
    private android.view.View mFullscreenView;
    private com.tencent.smtt.export.external.interfaces.IX5WebChromeClient.CustomViewCallback mFullscreenCallback;
    private android.webkit.WebChromeClient.CustomViewCallback mSystemFullscreenCallback;
    private boolean mIsFullscreen = false;
    private android.widget.FrameLayout mFullscreenContainer;
    private ViewGroup mWebViewParent;

    // JavaScript接口相关字段
    private YCJavaScriptInterface mJavaScriptInterface;

    // 性能监控相关字段
    private long mPageStartTime;
    private long mPageLoadTime;
    private int mResourceCount;
    private long mTotalResourceSize;

    // 回调接口
    public interface BrowserCallback {
        void onProgressChanged(int progress);
        void onTitleChanged(String title);
        void onUrlChanged(String url);
        void onError(String error);
        void onVideoReady();
        void onFileReady(String filePath);
        void onPageStarted(String url);
        void onPageFinished(String url);
        void onSecurityStatusChanged(boolean isSecure);
        void onBookmarkStatusChanged(boolean isBookmarked);
    }

    private BrowserCallback mCallback;

    public YCWebViewBrowserManager(Context context) {
        mContext = context;
        initX5();
        initJavaScriptInterface();
    }

    /**
     * 初始化JavaScript接口
     */
    private void initJavaScriptInterface() {
        mJavaScriptInterface = new YCJavaScriptInterface();
    }

    /**
     * 初始化X5内核
     * 参考YCWebView的X5初始化最佳实践
     */
    private void initX5() {
        try {
            // 预初始化X5
            QbSdk.preInit(mContext, null);

            // 检查X5是否可用
            int tbsVersion = QbSdk.getTbsVersion(mContext);
            mIsX5Available = (tbsVersion > 0);

            Log.d(TAG, "X5 initialization - Available: " + mIsX5Available + ", Version: " + tbsVersion);

            if (mIsX5Available) {
                // 设置X5优化参数
                setX5OptimizationParams();
            }

        } catch (Exception e) {
            Log.w(TAG, "X5 initialization failed, fallback to system WebView", e);
            mIsX5Available = false;
        }
    }

    /**
     * 设置X5优化参数
     * 参考YCWebView的系统属性配置
     */
    private void setX5OptimizationParams() {
        try {
            // ===== 硬件加速优化 =====
            System.setProperty("webview.x5.enable_hw_decode", "true");
            System.setProperty("webview.x5.hw_decode_max_resolution", "4096x2160");

            // ===== 网络优化 =====
            System.setProperty("webview.x5.enable_quic", "true");
            System.setProperty("webview.x5.enable_http2", "true");

            // ===== 性能优化 =====
            System.setProperty("webview.x5.enable_performance_monitor", "true");
            System.setProperty("webview.x5.video.decode_priority", "high");

            // ===== 缓存优化 =====
            System.setProperty("webview.x5.cache.size", "200");
            System.setProperty("webview.x5.disk_cache_size", "500");

            Log.d(TAG, "X5 optimization parameters set successfully");

        } catch (Exception e) {
            Log.w(TAG, "Failed to set X5 optimization parameters", e);
        }
    }

    /**
     * 创建优化的WebView
     * 参考YCWebView的WebView配置最佳实践
     */
    public android.view.View createOptimizedWebView(ViewGroup parent) {
        if (mIsX5Available) {
            return createX5WebView(parent);
        } else {
            return createSystemWebView(parent);
        }
    }

    /**
     * 创建X5 WebView
     */
    private android.view.View createX5WebView(ViewGroup parent) {
        try {
            mX5WebView = new com.tencent.smtt.sdk.WebView(mContext);

            // 设置WebViewClient
            mX5WebView.setWebViewClient(createX5WebViewClient());

            // 设置WebChromeClient
            mX5WebView.setWebChromeClient(createX5WebChromeClient());

            // 配置WebSettings
            configureX5WebSettings();

            // 添加到父容器
            parent.addView(mX5WebView);

            Log.d(TAG, "X5 WebView created successfully");
            return mX5WebView;

        } catch (Exception e) {
            Log.w(TAG, "Failed to create X5 WebView, fallback to system", e);
            return createSystemWebView(parent);
        }
    }

    /**
     * 创建系统WebView
     */
    private android.view.View createSystemWebView(ViewGroup parent) {
        mSystemWebView = new android.webkit.WebView(mContext);

        // 设置WebViewClient
        mSystemWebView.setWebViewClient(createSystemWebViewClient());

        // 设置WebChromeClient
        mSystemWebView.setWebChromeClient(createSystemWebChromeClient());

        // 配置WebSettings
        configureSystemWebSettings();

        // 添加到父容器
        parent.addView(mSystemWebView);

        Log.d(TAG, "System WebView created successfully");
        return mSystemWebView;
    }

    /**
     * 创建X5 WebViewClient
     * 参考YCWebView的文件处理和错误处理
     */
    private com.tencent.smtt.sdk.WebViewClient createX5WebViewClient() {
        return new com.tencent.smtt.sdk.WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(com.tencent.smtt.sdk.WebView view, String url) {
                return handleUrlOverride(url);
            }

            @Override
            public void onPageStarted(com.tencent.smtt.sdk.WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "X5 Page started: " + url);
                if (mCallback != null) {
                    mCallback.onPageStarted(url);
                    mCallback.onUrlChanged(url);
                }
            }

            @Override
            public void onPageFinished(com.tencent.smtt.sdk.WebView view, String url) {
                Log.d(TAG, "X5 Page finished: " + url);

                // 记录页面加载时间
                mPageLoadTime = System.currentTimeMillis() - mPageStartTime;
                Log.d(TAG, "X5 Page load time: " + mPageLoadTime + "ms");

                // 检查SSL状态
                boolean isSecure = url.startsWith("https://");
                if (mCallback != null) {
                    mCallback.onSecurityStatusChanged(isSecure);
                }

                injectVideoEnhancementScript();
                injectFileViewerScript();

                // 注入性能监控脚本
                injectPerformanceMonitoringScript();

                // 调用页面完成回调
                if (mCallback != null) {
                    mCallback.onPageFinished(url);
                }
            }

            @Override
            public void onReceivedError(com.tencent.smtt.sdk.WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "X5 Error: " + errorCode + " - " + description);
                if (mCallback != null) {
                    mCallback.onError("Error " + errorCode + ": " + description);
                }
            }
        };
    }

    /**
     * 创建X5 WebChromeClient
     * 参考YCWebView的进度条和视频处理
     */
    private com.tencent.smtt.sdk.WebChromeClient createX5WebChromeClient() {
        return new com.tencent.smtt.sdk.WebChromeClient() {

            @Override
            public void onProgressChanged(com.tencent.smtt.sdk.WebView view, int newProgress) {
                if (mCallback != null) {
                    mCallback.onProgressChanged(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(com.tencent.smtt.sdk.WebView view, String title) {
                if (mCallback != null) {
                    mCallback.onTitleChanged(title);
                }
            }

            @Override
            public void onShowCustomView(android.view.View view, com.tencent.smtt.export.external.interfaces.IX5WebChromeClient.CustomViewCallback callback) {
                Log.d(TAG, "X5 Video fullscreen requested");
                if (mCallback != null) {
                    mCallback.onVideoReady();
                }
                // 处理全屏视频
                handleX5FullscreenVideo(view, callback);
            }

            @Override
            public void onHideCustomView() {
                Log.d(TAG, "X5 Video fullscreen exited");
                hideX5FullscreenVideo();
            }

            @Override
            public boolean onConsoleMessage(com.tencent.smtt.export.external.interfaces.ConsoleMessage consoleMessage) {
                Log.d(TAG, "X5 Console: " + consoleMessage.message());
                return true;
            }

            @Override
            public boolean onJsAlert(com.tencent.smtt.sdk.WebView view, String url, String message, com.tencent.smtt.export.external.interfaces.JsResult result) {
                Log.d(TAG, "X5 JS Alert: " + message);
                result.confirm();
                return true;
            }
        };
    }

    /**
     * 处理URL跳转
     * 参考YCWebView的应用间跳转处理
     */
    private boolean handleUrlOverride(String url) {
        try {
            Uri uri = Uri.parse(url);

            // 处理应用间跳转 - 参考YCWebView的移动设备功能
            if ("tel".equals(uri.getScheme())) {
                // 打电话
                Intent intent = new Intent(Intent.ACTION_DIAL, uri);
                mContext.startActivity(intent);
                Log.d(TAG, "Handled tel scheme: " + url);
                return true;
            } else if ("sms".equals(uri.getScheme())) {
                // 发短信
                Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                mContext.startActivity(intent);
                Log.d(TAG, "Handled sms scheme: " + url);
                return true;
            } else if ("mailto".equals(uri.getScheme())) {
                // 发邮件
                Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                mContext.startActivity(intent);
                Log.d(TAG, "Handled mailto scheme: " + url);
                return true;
            }

            // 处理文件类型 - 参考YCWebView的文件查看功能
            String lowerUrl = url.toLowerCase();

            // 文档文件
            if (lowerUrl.contains(".pdf") || lowerUrl.contains(".doc") ||
                lowerUrl.contains(".docx") || lowerUrl.contains(".xls") ||
                lowerUrl.contains(".xlsx") || lowerUrl.contains(".ppt") ||
                lowerUrl.contains(".pptx") || lowerUrl.contains(".txt") ||
                lowerUrl.contains(".rtf")) {
                openDocumentFile(url);
                Log.d(TAG, "Handled document file: " + url);
                return true;
            }

            // 压缩文件
            if (lowerUrl.contains(".zip") || lowerUrl.contains(".rar") ||
                lowerUrl.contains(".7z") || lowerUrl.contains(".tar") ||
                lowerUrl.contains(".gz")) {
                downloadFile(url);
                Log.d(TAG, "Handled archive file: " + url);
                return true;
            }

            // 图片文件
            if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") ||
                lowerUrl.contains(".png") || lowerUrl.contains(".gif") ||
                lowerUrl.contains(".bmp") || lowerUrl.contains(".webp")) {
                openImageFile(url);
                Log.d(TAG, "Handled image file: " + url);
                return true;
            }

            // 音频文件
            if (lowerUrl.contains(".mp3") || lowerUrl.contains(".wav") ||
                lowerUrl.contains(".ogg") || lowerUrl.contains(".aac") ||
                lowerUrl.contains(".flac")) {
                openAudioFile(url);
                Log.d(TAG, "Handled audio file: " + url);
                return true;
            }

            // 视频文件
            if (lowerUrl.contains(".mp4") || lowerUrl.contains(".avi") ||
                lowerUrl.contains(".mkv") || lowerUrl.contains(".mov") ||
                lowerUrl.contains(".wmv") || lowerUrl.contains(".flv")) {
                openVideoFile(url);
                Log.d(TAG, "Handled video file: " + url);
                return true;
            }

            // APK文件
            if (lowerUrl.contains(".apk")) {
                installApkFile(url);
                Log.d(TAG, "Handled APK file: " + url);
                return true;
            }

            return false;

        } catch (Exception e) {
            Log.w(TAG, "Failed to handle URL override: " + url, e);
            return false;
        }
    }

    /**
     * 配置X5 WebSettings
     * 参考YCWebView的WebSettings最佳配置
     */
    private void configureX5WebSettings() {
        try {
            com.tencent.smtt.sdk.WebSettings settings = mX5WebView.getSettings();

            // ===== 基础设置 =====
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setLoadsImagesAutomatically(true);

            // ===== 缓存设置 =====
            settings.setCacheMode(com.tencent.smtt.sdk.WebSettings.LOAD_CACHE_ELSE_NETWORK);
            settings.setAppCacheEnabled(true);
            settings.setAppCacheMaxSize(100 * 1024 * 1024); // 100MB

            // ===== DOM和数据库 =====
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);

            // ===== 文件访问 =====
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);

            // ===== 媒体设置 =====
            settings.setMediaPlaybackRequiresUserGesture(false);

            // ===== User Agent =====
            String ua = settings.getUserAgentString();
            settings.setUserAgentString(ua + " YCWebView/1.0");

            // ===== JavaScript接口 =====
            if (mJavaScriptInterface != null) {
                mX5WebView.addJavascriptInterface(mJavaScriptInterface, "YCWebView");
            }

            Log.d(TAG, "X5 WebSettings configured successfully");

        } catch (Exception e) {
            Log.w(TAG, "Failed to configure X5 WebSettings", e);
        }
    }

    /**
     * 创建系统WebViewClient
     */
    private android.webkit.WebViewClient createSystemWebViewClient() {
        return new android.webkit.WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
                return handleUrlOverride(url);
            }

            @Override
            public void onPageStarted(android.webkit.WebView view, String url, Bitmap favicon) {
                if (mCallback != null) {
                    mCallback.onUrlChanged(url);
                }
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                // 记录页面加载时间
                mPageLoadTime = System.currentTimeMillis() - mPageStartTime;
                Log.d(TAG, "System WebView Page load time: " + mPageLoadTime + "ms");

                injectVideoEnhancementScript();
                injectFileViewerScript();

                // 注入性能监控脚本
                injectPerformanceMonitoringScript();
            }
        };
    }

    /**
     * 创建系统WebChromeClient
     */
    private android.webkit.WebChromeClient createSystemWebChromeClient() {
        return new android.webkit.WebChromeClient() {
            @Override
            public void onProgressChanged(android.webkit.WebView view, int newProgress) {
                if (mCallback != null) {
                    mCallback.onProgressChanged(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(android.webkit.WebView view, String title) {
                if (mCallback != null) {
                    mCallback.onTitleChanged(title);
                }
            }

            @Override
            public void onShowCustomView(android.view.View view, android.webkit.WebChromeClient.CustomViewCallback callback) {
                Log.d(TAG, "System Video fullscreen requested");
                if (mCallback != null) {
                    mCallback.onVideoReady();
                }
                // 处理全屏视频
                handleSystemFullscreenVideo(view, callback);
            }

            @Override
            public void onHideCustomView() {
                Log.d(TAG, "System Video fullscreen exited");
                hideSystemFullscreenVideo();
            }

            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d(TAG, "System Console: " + consoleMessage.message());
                return true;
            }

            @Override
            public boolean onJsAlert(android.webkit.WebView view, String url, String message, android.webkit.JsResult result) {
                Log.d(TAG, "System JS Alert: " + message);
                result.confirm();
                return true;
            }
        };
    }

    /**
     * 配置系统WebSettings
     */
    private void configureSystemWebSettings() {
        try {
            android.webkit.WebSettings settings = mSystemWebView.getSettings();

            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setMediaPlaybackRequiresUserGesture(false);

            // ===== JavaScript接口 =====
            if (mJavaScriptInterface != null) {
                mSystemWebView.addJavascriptInterface(mJavaScriptInterface, "YCWebView");
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to configure system WebSettings", e);
        }
    }

    /**
     * 注入视频增强脚本
     * 参考YCWebView的视频优化
     */
    private void injectVideoEnhancementScript() {
        String script = "(function() {" +
            "console.log('YCWebView: Video enhancement script loaded');" +

            "// 优化视频播放" +
            "var videos = document.getElementsByTagName('video');" +
            "for (var i = 0; i < videos.length; i++) {" +
            "  var video = videos[i];" +
            "  video.preload = 'metadata';" +
            "  video.playsInline = true;" +
            "  video.style.objectFit = 'contain';" +

            "  // 添加事件监听" +
            "  video.addEventListener('play', function() {" +
            "    console.log('YCWebView: Video started playing');" +
            "    if (window.YCWebView && window.YCWebView.onVideoPlay) {" +
            "      window.YCWebView.onVideoPlay();" +
            "    }" +
            "  });" +

            "  video.addEventListener('pause', function() {" +
            "    console.log('YCWebView: Video paused');" +
            "    if (window.YCWebView && window.YCWebView.onVideoPause) {" +
            "      window.YCWebView.onVideoPause();" +
            "    }" +
            "  });" +
            "}" +

            "// YouTube广告跳过" +
            "if (window.location.hostname.includes('youtube.com')) {" +
            "  setInterval(function() {" +
            "    var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern');" +
            "    if (skipBtn) {" +
            "      console.log('YCWebView: Skipping YouTube ad');" +
            "      skipBtn.click();" +
            "    }" +
            "  }, 1000);" +
            "}" +

            "console.log('YCWebView: Video enhancement completed');" +
            "})();";

        try {
            if (mIsX5Available && mX5WebView != null) {
                mX5WebView.evaluateJavascript(script, null);
            } else if (mSystemWebView != null) {
                mSystemWebView.evaluateJavascript(script, null);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to inject video enhancement script", e);
        }
    }

    /**
     * 注入文件查看器脚本
     */
    private void injectFileViewerScript() {
        String script = "(function() {" +
            "console.log('YCWebView: File viewer script loaded');" +

            "// 检测文件链接" +
            "var links = document.getElementsByTagName('a');" +
            "for (var i = 0; i < links.length; i++) {" +
            "  var link = links[i];" +
            "  var href = link.href;" +
            "  if (href) {" +
            "    // 检测Office文档" +
            "    if (href.match(/\\.(doc|docx|xls|xlsx|ppt|pptx|pdf|txt)$/i)) {" +
            "      link.onclick = function(e) {" +
            "        console.log('YCWebView: File link clicked: ' + this.href);" +
            "        if (window.YCWebView && window.YCWebView.onFileClick) {" +
            "          window.YCWebView.onFileClick(this.href);" +
            "          e.preventDefault();" +
            "          return false;" +
            "        }" +
            "      };" +
            "    }" +
            "  }" +
            "}" +

            "console.log('YCWebView: File viewer script completed');" +
            "})();";

        try {
            if (mIsX5Available && mX5WebView != null) {
                mX5WebView.evaluateJavascript(script, null);
            } else if (mSystemWebView != null) {
                mSystemWebView.evaluateJavascript(script, null);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to inject file viewer script", e);
        }
    }

    /**
     * 处理X5全屏视频
     * 参考YCWebView的全屏视频最佳实践
     */
    private void handleX5FullscreenVideo(android.view.View view, com.tencent.smtt.export.external.interfaces.IX5WebChromeClient.CustomViewCallback callback) {
        try {
            Log.d(TAG, "X5 fullscreen video handling started");

            if (mIsFullscreen) {
                Log.w(TAG, "Already in fullscreen mode");
                return;
            }

            mFullscreenView = view;
            mFullscreenCallback = callback;
            mIsFullscreen = true;

            // 获取Activity并设置全屏
            if (mContext instanceof Activity) {
                Activity activity = (Activity) mContext;

                // 保存WebView的父容器
                if (mX5WebView != null) {
                    mWebViewParent = (ViewGroup) mX5WebView.getParent();
                }

                // 创建全屏容器
                mFullscreenContainer = new FrameLayout(mContext);
                mFullscreenContainer.setBackgroundColor(0xFF000000); // 黑色背景

                // 添加全屏视图
                if (view != null) {
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    );
                    mFullscreenContainer.addView(view, params);
                }

                // 设置Activity全屏
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                // 设置横屏方向（可选）
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                // 将全屏容器添加到Activity的根视图
                ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                decorView.addView(mFullscreenContainer);

                Log.d(TAG, "X5 fullscreen video entered successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to handle X5 fullscreen video", e);
            // 出错时尝试退出全屏
            hideX5FullscreenVideo();
        }
    }

    /**
     * 处理系统全屏视频
     */
    private void handleSystemFullscreenVideo(android.view.View view, android.webkit.WebChromeClient.CustomViewCallback callback) {
        try {
            Log.d(TAG, "System fullscreen video handling started");

            if (mIsFullscreen) {
                Log.w(TAG, "Already in fullscreen mode");
                return;
            }

            mFullscreenView = view;
            mSystemFullscreenCallback = callback;
            mIsFullscreen = true;

            // 获取Activity并设置全屏
            if (mContext instanceof Activity) {
                Activity activity = (Activity) mContext;

                // 保存WebView的父容器
                if (mSystemWebView != null) {
                    mWebViewParent = (ViewGroup) mSystemWebView.getParent();
                }

                // 创建全屏容器
                mFullscreenContainer = new FrameLayout(mContext);
                mFullscreenContainer.setBackgroundColor(0xFF000000); // 黑色背景

                // 添加全屏视图
                if (view != null) {
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    );
                    mFullscreenContainer.addView(view, params);
                }

                // 设置Activity全屏
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // 设置横屏方向（可选）
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                // 隐藏系统UI
                hideSystemUI(activity);

                // 将全屏容器添加到Activity的根视图
                ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                decorView.addView(mFullscreenContainer);

                Log.d(TAG, "System fullscreen video entered successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to handle system fullscreen video", e);
        }
    }

    /**
     * 隐藏系统全屏视频
     */
    private void hideSystemFullscreenVideo() {
        try {
            Log.d(TAG, "System hide fullscreen video started");

            if (!mIsFullscreen) {
                Log.w(TAG, "Not in fullscreen mode");
                return;
            }

            // 获取Activity
            if (mContext instanceof Activity) {
                Activity activity = (Activity) mContext;

                // 移除全屏容器
                if (mFullscreenContainer != null) {
                    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                    decorView.removeView(mFullscreenContainer);

                    // 如果有全屏视图，从容器中移除
                    if (mFullscreenView != null) {
                        mFullscreenContainer.removeView(mFullscreenView);
                    }
                }

                // 清除全屏标志
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // 显示系统UI
                showSystemUI(activity);

                // 恢复竖屏方向
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                // 调用回调
                if (mSystemFullscreenCallback != null) {
                    mSystemFullscreenCallback.onCustomViewHidden();
                }
            }

            // 重置状态
            mFullscreenView = null;
            mSystemFullscreenCallback = null;
            mFullscreenContainer = null;
            mIsFullscreen = false;

            Log.d(TAG, "System fullscreen video exited successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to hide system fullscreen video", e);
            // 强制重置状态
            mFullscreenView = null;
            mSystemFullscreenCallback = null;
            mFullscreenContainer = null;
            mIsFullscreen = false;
        }
    }

    /**
     * 隐藏系统UI
     */
    private void hideSystemUI(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController controller = activity.getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide system UI", e);
        }
    }

    /**
     * 显示系统UI
     */
    private void showSystemUI(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.getWindow().setDecorFitsSystemWindows(true);
                WindowInsetsController controller = activity.getWindow().getInsetsController();
                if (controller != null) {
                    controller.show(WindowInsets.Type.systemBars());
                }
            } else {
                activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show system UI", e);
        }
    }

    /**
     * 隐藏X5全屏视频
     * 参考YCWebView的退出全屏最佳实践
     */
    private void hideX5FullscreenVideo() {
        try {
            Log.d(TAG, "X5 hide fullscreen video started");

            if (!mIsFullscreen) {
                Log.w(TAG, "Not in fullscreen mode");
                return;
            }

            // 获取Activity
            if (mContext instanceof Activity) {
                Activity activity = (Activity) mContext;

                // 移除全屏容器
                if (mFullscreenContainer != null) {
                    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                    decorView.removeView(mFullscreenContainer);

                    // 如果有全屏视图，从容器中移除
                    if (mFullscreenView != null) {
                        mFullscreenContainer.removeView(mFullscreenView);
                    }
                }

                // 清除全屏标志
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                // 恢复竖屏方向
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                // 调用回调
                if (mFullscreenCallback != null) {
                    mFullscreenCallback.onCustomViewHidden();
                }
            }

            // 重置状态
            mFullscreenView = null;
            mFullscreenCallback = null;
            mFullscreenContainer = null;
            mIsFullscreen = false;

            Log.d(TAG, "X5 fullscreen video exited successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to hide X5 fullscreen video", e);
            // 强制重置状态
            mFullscreenView = null;
            mFullscreenCallback = null;
            mFullscreenContainer = null;
            mIsFullscreen = false;
        }
    }

    /**
     * 下载文件
     * 参考YCWebView的文件处理最佳实践
     */
    private void downloadFile(String url) {
        try {
            Log.d(TAG, "Downloading file: " + url);

            Uri uri = Uri.parse(url);

            // 检查是否为外部链接
            if (url.startsWith("http://") || url.startsWith("https://")) {
                // 使用系统下载管理器
                downloadWithSystemDownloader(url);
            } else {
                // 本地文件直接打开
                openLocalFile(url);
            }

            if (mCallback != null) {
                mCallback.onFileReady(url);
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to download file: " + url, e);
        }
    }

    /**
     * 使用系统下载管理器下载文件
     */
    private void downloadWithSystemDownloader(String url) {
        try {
            android.app.DownloadManager downloadManager = (android.app.DownloadManager)
                mContext.getSystemService(Context.DOWNLOAD_SERVICE);

            if (downloadManager != null) {
                android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));

                // 设置文件信息
                String fileName = getFileNameFromUrl(url);
                request.setTitle(fileName);
                request.setDescription("Downloading file from YCWebView");

                // 设置MIME类型
                String mimeType = getMimeTypeFromUrl(url);
                if (mimeType != null) {
                    request.setMimeType(mimeType);
                }

                // 设置下载路径
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);

                // 设置下载属性
                request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setAllowedOverMetered(true);
                request.setAllowedOverRoaming(true);

                // 开始下载
                long downloadId = downloadManager.enqueue(request);
                Log.d(TAG, "Download started with ID: " + downloadId);
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to start system download", e);
        }
    }

    /**
     * 打开本地文件
     */
    private void openLocalFile(String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 根据文件类型设置MIME类型
            String mimeType = getMimeTypeFromUrl(url);
            if (mimeType != null) {
                intent.setDataAndType(uri, mimeType);
            } else {
                intent.setData(uri);
            }

            mContext.startActivity(intent);

        } catch (Exception e) {
            Log.w(TAG, "Failed to open local file: " + url, e);
        }
    }

    /**
     * 从URL提取文件名
     */
    private String getFileNameFromUrl(String url) {
        try {
            String decodedUrl = java.net.URLDecoder.decode(url, "UTF-8");
            int lastSlashIndex = decodedUrl.lastIndexOf('/');
            if (lastSlashIndex != -1) {
                String fileName = decodedUrl.substring(lastSlashIndex + 1);
                // 移除查询参数
                int questionMarkIndex = fileName.indexOf('?');
                if (questionMarkIndex != -1) {
                    fileName = fileName.substring(0, questionMarkIndex);
                }
                return fileName.isEmpty() ? "downloaded_file" : fileName;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract filename from URL: " + url, e);
        }
        return "downloaded_file";
    }


    /**
     * 设置回调
     */
    public void setBrowserCallback(BrowserCallback callback) {
        mCallback = callback;
    }

    /**
     * 加载URL
     */
    public void loadUrl(String url) {
        try {
            // 重置性能监控数据
            resetPerformanceMetrics();
            mPageStartTime = System.currentTimeMillis();

            if (mIsX5Available && mX5WebView != null) {
                mX5WebView.loadUrl(url);
            } else if (mSystemWebView != null) {
                mSystemWebView.loadUrl(url);
            }

            Log.d(TAG, "Loading URL with performance monitoring: " + url);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load URL: " + url, e);
        }
    }

    /**
     * 后退
     */
    public boolean goBack() {
        try {
            if (mIsX5Available && mX5WebView != null && mX5WebView.canGoBack()) {
                mX5WebView.goBack();
                return true;
            } else if (mSystemWebView != null && mSystemWebView.canGoBack()) {
                mSystemWebView.goBack();
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to go back", e);
        }
        return false;
    }

    /**
     * 前进
     */
    public boolean goForward() {
        try {
            if (mIsX5Available && mX5WebView != null && mX5WebView.canGoForward()) {
                mX5WebView.goForward();
                return true;
            } else if (mSystemWebView != null && mSystemWebView.canGoForward()) {
                mSystemWebView.goForward();
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to go forward", e);
        }
        return false;
    }

    /**
     * 刷新
     */
    public void reload() {
        try {
            if (mIsX5Available && mX5WebView != null) {
                mX5WebView.reload();
            } else if (mSystemWebView != null) {
                mSystemWebView.reload();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to reload", e);
        }
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        try {
            if (mX5WebView != null) {
                mX5WebView.destroy();
                mX5WebView = null;
            }
            if (mSystemWebView != null) {
                mSystemWebView.destroy();
                mSystemWebView = null;
            }
            Log.d(TAG, "YCWebViewBrowserManager destroyed");
        } catch (Exception e) {
            Log.w(TAG, "Error during destruction", e);
        }
    }

    /**
     * 检查X5是否可用
     */
    public boolean isX5Available() {
        return mIsX5Available;
    }

    /**
     * 获取当前的WebView实例
     */
    public android.view.View getWebView() {
        if (mIsX5Available && mX5WebView != null) {
            return mX5WebView;
        } else if (mSystemWebView != null) {
            return mSystemWebView;
        }
        return null;
    }

    /**
     * 清除WebView缓存
     */
    public void clearCache() {
        try {
            if (mIsX5Available && mX5WebView != null) {
                // 清除X5 WebView缓存
                mX5WebView.clearCache(true);
                mX5WebView.clearHistory();
                mX5WebView.clearFormData();
                mX5WebView.clearMatches();
            } else if (mSystemWebView != null) {
                // 清除系统WebView缓存
                mSystemWebView.clearCache(true);
                mSystemWebView.clearHistory();
                mSystemWebView.clearFormData();
                mSystemWebView.clearMatches();
            }

            Log.d(TAG, "WebView cache cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear WebView cache", e);
        }
    }

    /**
     * 打开文档文件 - 参考YCWebView的文件查看功能
     */
    private void openDocumentFile(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);

            // 根据文件类型设置MIME类型
            String mimeType = getMimeTypeFromUrl(url);
            if (mimeType != null) {
                intent.setDataAndType(uri, mimeType);
            } else {
                intent.setData(uri);
            }

            // 添加FLAG_GRANT_READ_URI_PERMISSION
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 尝试启动合适的应用程序
            mContext.startActivity(intent);

        } catch (Exception e) {
            Log.w(TAG, "Failed to open document file, falling back to download", e);
            downloadFile(url);
        }
    }

    /**
     * 打开图片文件
     */
    private void openImageFile(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            intent.setDataAndType(uri, "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to open image file", e);
            downloadFile(url);
        }
    }

    /**
     * 打开音频文件
     */
    private void openAudioFile(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            intent.setDataAndType(uri, "audio/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to open audio file", e);
            downloadFile(url);
        }
    }

    /**
     * 打开视频文件
     */
    private void openVideoFile(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            intent.setDataAndType(uri, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to open video file", e);
            downloadFile(url);
        }
    }

    /**
     * 安装APK文件
     */
    private void installApkFile(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to install APK file", e);
            downloadFile(url);
        }
    }

    /**
     * 获取文件MIME类型
     */
    private String getMimeTypeFromUrl(String url) {
        String lowerUrl = url.toLowerCase();

        if (lowerUrl.contains(".pdf")) return "application/pdf";
        if (lowerUrl.contains(".doc")) return "application/msword";
        if (lowerUrl.contains(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerUrl.contains(".xls")) return "application/vnd.ms-excel";
        if (lowerUrl.contains(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lowerUrl.contains(".ppt")) return "application/vnd.ms-powerpoint";
        if (lowerUrl.contains(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lowerUrl.contains(".txt")) return "text/plain";
        if (lowerUrl.contains(".rtf")) return "application/rtf";

        return null;
    }

    /**
     * 截取WebView页面截图 - 参考YCWebView的截图功能
     */
    public Bitmap captureScreenshot() {
        try {
            if (mIsX5Available && mX5WebView != null) {
                // X5 WebView截图 - 使用绘图缓存
                mX5WebView.setDrawingCacheEnabled(true);
                mX5WebView.buildDrawingCache();
                Bitmap screenshot = Bitmap.createBitmap(mX5WebView.getDrawingCache());
                mX5WebView.setDrawingCacheEnabled(false);
                return screenshot;
            } else if (mSystemWebView != null) {
                // 系统WebView截图
                return captureSystemWebViewScreenshot();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture screenshot", e);
        }
        return null;
    }

    /**
     * 截取系统WebView的页面截图
     */
    private Bitmap captureSystemWebViewScreenshot() {
        try {
            if (mSystemWebView != null) {
                // 启用绘图缓存
                mSystemWebView.setDrawingCacheEnabled(true);
                mSystemWebView.buildDrawingCache();

                // 获取截图
                Bitmap screenshot = Bitmap.createBitmap(mSystemWebView.getDrawingCache());
                mSystemWebView.setDrawingCacheEnabled(false);

                return screenshot;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture system WebView screenshot", e);
        }
        return null;
    }

    /**
     * 保存截图到文件
     */
    public String saveScreenshot(Bitmap bitmap) {
        if (bitmap == null) return null;

        try {
            // 创建截图保存目录
            java.io.File screenshotDir = new java.io.File(mContext.getExternalFilesDir(null), "screenshots");
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }

            // 生成文件名
            String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
            java.io.File screenshotFile = new java.io.File(screenshotDir, fileName);

            // 保存截图
            java.io.FileOutputStream fos = new java.io.FileOutputStream(screenshotFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "Screenshot saved: " + screenshotFile.getAbsolutePath());
            return screenshotFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Failed to save screenshot", e);
            return null;
        }
    }

    /**
     * 截取长图（整个页面）
     */
    public Bitmap captureLongScreenshot() {
        try {
            if (mIsX5Available && mX5WebView != null) {
                // X5 WebView长图截取
                return captureX5LongScreenshot();
            } else if (mSystemWebView != null) {
                // 系统WebView长图截取
                return captureSystemLongScreenshot();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture long screenshot", e);
        }
        return null;
    }

    /**
     * 截取X5 WebView长图
     */
    private Bitmap captureX5LongScreenshot() {
        try {
            if (mX5WebView != null) {
                // 计算页面总高度
                int contentHeight = (int) Math.ceil(mX5WebView.getContentHeight() * mX5WebView.getScale());
                int contentWidth = mX5WebView.getWidth();

                if (contentWidth == 0) return null;

                // 创建长图Bitmap
                Bitmap longScreenshot = Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.RGB_565);

                // 创建Canvas
                android.graphics.Canvas canvas = new android.graphics.Canvas(longScreenshot);
                canvas.drawColor(android.graphics.Color.WHITE);

                // 滚动截取每一部分
                int scrollY = 0;
                int stepHeight = mX5WebView.getHeight();
                Bitmap tempBitmap;

                while (scrollY < contentHeight) {
                    // 滚动到指定位置
                    mX5WebView.scrollTo(0, scrollY);

                    // 等待滚动完成
                    Thread.sleep(100);

                    // 截取当前可见部分 - 使用绘图缓存
                    mX5WebView.setDrawingCacheEnabled(true);
                    mX5WebView.buildDrawingCache();
                    tempBitmap = Bitmap.createBitmap(mX5WebView.getDrawingCache());
                    mX5WebView.setDrawingCacheEnabled(false);

                    if (tempBitmap != null) {
                        // 绘制到长图中
                        android.graphics.Rect srcRect = new android.graphics.Rect(0, 0, tempBitmap.getWidth(), Math.min(tempBitmap.getHeight(), contentHeight - scrollY));
                        android.graphics.Rect dstRect = new android.graphics.Rect(0, scrollY, contentWidth, scrollY + Math.min(stepHeight, contentHeight - scrollY));
                        canvas.drawBitmap(tempBitmap, srcRect, dstRect, null);
                        tempBitmap.recycle();
                    }

                    scrollY += stepHeight;
                }

                return longScreenshot;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture X5 long screenshot", e);
        }
        return null;
    }

    /**
     * 截取系统WebView长图
     */
    private Bitmap captureSystemLongScreenshot() {
        try {
            if (mSystemWebView != null) {
                // 计算页面总高度
                int contentHeight = (int) Math.ceil(mSystemWebView.getContentHeight() * mSystemWebView.getScale());
                int contentWidth = mSystemWebView.getWidth();

                if (contentWidth == 0) return null;

                // 创建长图Bitmap
                Bitmap longScreenshot = Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.RGB_565);

                // 创建Canvas
                android.graphics.Canvas canvas = new android.graphics.Canvas(longScreenshot);
                canvas.drawColor(android.graphics.Color.WHITE);

                // 滚动截取每一部分
                int scrollY = 0;
                int stepHeight = mSystemWebView.getHeight();

                while (scrollY < contentHeight) {
                    // 滚动到指定位置
                    mSystemWebView.scrollTo(0, scrollY);

                    // 等待滚动完成
                    Thread.sleep(100);

                    // 截取当前可见部分
                    mSystemWebView.setDrawingCacheEnabled(true);
                    mSystemWebView.buildDrawingCache();
                    Bitmap tempBitmap = Bitmap.createBitmap(mSystemWebView.getDrawingCache());
                    mSystemWebView.setDrawingCacheEnabled(false);

                    if (tempBitmap != null) {
                        // 绘制到长图中
                        android.graphics.Rect srcRect = new android.graphics.Rect(0, 0, tempBitmap.getWidth(), Math.min(tempBitmap.getHeight(), contentHeight - scrollY));
                        android.graphics.Rect dstRect = new android.graphics.Rect(0, scrollY, contentWidth, scrollY + Math.min(stepHeight, contentHeight - scrollY));
                        canvas.drawBitmap(tempBitmap, srcRect, dstRect, null);
                        tempBitmap.recycle();
                    }

                    scrollY += stepHeight;
                }

                return longScreenshot;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture system long screenshot", e);
        }
        return null;
    }

    /**
     * 重置性能监控指标
     */
    private void resetPerformanceMetrics() {
        mPageStartTime = 0;
        mPageLoadTime = 0;
        mResourceCount = 0;
        mTotalResourceSize = 0;
    }

    /**
     * 注入性能监控脚本
     */
    private void injectPerformanceMonitoringScript() {
        String script = "(function() {" +
            "console.log('YCWebView: Performance monitoring started');" +

            "// 监控页面性能" +
            "if (window.performance && window.performance.timing) {" +
            "  var timing = window.performance.timing;" +
            "  var loadTime = timing.loadEventEnd - timing.navigationStart;" +
            "  console.log('YCWebView: Page load time from navigation: ' + loadTime + 'ms');" +

            "  // 通过JavaScript接口报告性能数据" +
            "  if (window.YCWebView && window.YCWebView.onPerformanceData) {" +
            "    window.YCWebView.onPerformanceData(loadTime, timing.domContentLoadedEventEnd - timing.navigationStart);" +
            "  }" +
            "}" +

            "// 监控资源加载" +
            "var resources = window.performance.getEntriesByType('resource');" +
            "console.log('YCWebView: Loaded ' + resources.length + ' resources');" +

            "// 监控内存使用（如果可用）" +
            "if (window.performance.memory) {" +
            "  var mem = window.performance.memory;" +
            "  console.log('YCWebView: Memory - Used: ' + Math.round(mem.usedJSHeapSize / 1024) + 'KB, Total: ' + Math.round(mem.totalJSHeapSize / 1024) + 'KB');" +
            "}" +

            "console.log('YCWebView: Performance monitoring completed');" +
            "})();";

        try {
            if (mIsX5Available && mX5WebView != null) {
                mX5WebView.evaluateJavascript(script, null);
            } else if (mSystemWebView != null) {
                mSystemWebView.evaluateJavascript(script, null);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to inject performance monitoring script", e);
        }
    }

    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== YCWebView 性能统计 ===\\n");
        stats.append("X5可用: ").append(mIsX5Available ? "是" : "否").append("\\n");
        stats.append("页面加载时间: ").append(mPageLoadTime > 0 ? mPageLoadTime + "ms" : "未完成").append("\\n");
        stats.append("资源数量: ").append(mResourceCount).append("\\n");
        stats.append("总资源大小: ").append(mTotalResourceSize / 1024).append("KB\\n");

        // 添加内存信息
        try {
            android.app.ActivityManager activityManager = (android.app.ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                android.app.ActivityManager.MemoryInfo memoryInfo = new android.app.ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                stats.append("可用内存: ").append(memoryInfo.availMem / 1024 / 1024).append("MB\\n");
                stats.append("内存不足: ").append(memoryInfo.lowMemory ? "是" : "否").append("\\n");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get memory info", e);
        }

        return stats.toString();
    }

    /**
     * YCWebView JavaScript接口类
     * 提供JavaScript与Native代码的交互桥梁
     */
    public class YCJavaScriptInterface {
        private static final String TAG = "YCJavaScriptInterface";

        /**
         * 视频播放回调
         */
        @android.webkit.JavascriptInterface
        public void onVideoPlay() {
            Log.d(TAG, "JavaScript: Video started playing");
            if (mCallback != null) {
                mCallback.onVideoReady();
            }
        }

        /**
         * 视频暂停回调
         */
        @android.webkit.JavascriptInterface
        public void onVideoPause() {
            Log.d(TAG, "JavaScript: Video paused");
        }

        /**
         * 文件点击回调
         */
        @android.webkit.JavascriptInterface
        public void onFileClick(String fileUrl) {
            Log.d(TAG, "JavaScript: File clicked - " + fileUrl);
            if (mCallback != null) {
                mCallback.onFileReady(fileUrl);
            }
        }

        /**
         * 页面加载完成回调
         */
        @android.webkit.JavascriptInterface
        public void onPageLoaded(String title, String url) {
            Log.d(TAG, "JavaScript: Page loaded - " + title + " (" + url + ")");
            if (mCallback != null) {
                mCallback.onTitleChanged(title);
                mCallback.onUrlChanged(url);
            }
        }

        /**
         * 错误回调
         */
        @android.webkit.JavascriptInterface
        public void onError(String errorMessage) {
            Log.e(TAG, "JavaScript: Error - " + errorMessage);
            if (mCallback != null) {
                mCallback.onError(errorMessage);
            }
        }

        /**
         * 获取设备信息
         */
        @android.webkit.JavascriptInterface
        public String getDeviceInfo() {
            try {
                android.os.Build build = new android.os.Build();
                return android.os.Build.MODEL + " (" + android.os.Build.VERSION.RELEASE + ")";
            } catch (Exception e) {
                Log.w(TAG, "Failed to get device info", e);
                return "Unknown Device";
            }
        }

        /**
         * 检查网络状态
         */
        @android.webkit.JavascriptInterface
        public boolean isNetworkAvailable() {
            try {
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    return activeNetwork != null && activeNetwork.isConnected();
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to check network status", e);
            }
            return false;
        }

        /**
         * 显示Toast消息
         */
        @android.webkit.JavascriptInterface
        public void showToast(String message) {
            try {
                if (mContext instanceof android.app.Activity) {
                    ((android.app.Activity) mContext).runOnUiThread(() -> {
                        android.widget.Toast.makeText(mContext, message, android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to show toast", e);
            }
        }

        /**
         * 打开外部应用
         */
        @android.webkit.JavascriptInterface
        public void openExternalApp(String url) {
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to open external app", e);
            }
        }

        /**
         * 获取应用版本信息
         */
        @android.webkit.JavascriptInterface
        public String getAppVersion() {
            try {
                android.content.pm.PackageManager pm = mContext.getPackageManager();
                android.content.pm.PackageInfo info = pm.getPackageInfo(mContext.getPackageName(), 0);
                return info.versionName;
            } catch (Exception e) {
                Log.w(TAG, "Failed to get app version", e);
                return "1.0.0";
            }
        }

        /**
         * 性能数据回调
         */
        @android.webkit.JavascriptInterface
        public void onPerformanceData(long loadTime, long domContentLoadedTime) {
            Log.d(TAG, "JavaScript: Performance data - Load time: " + loadTime + "ms, DOM Content Loaded: " + domContentLoadedTime + "ms");

            // 更新性能指标
            mPageLoadTime = loadTime;

            // 可以在这里添加更多性能分析逻辑
            if (loadTime > 3000) {
                Log.w(TAG, "JavaScript: Slow page load detected (>3s)");
            } else if (loadTime < 1000) {
                Log.d(TAG, "JavaScript: Fast page load (<1s)");
            }
        }
    }
}
