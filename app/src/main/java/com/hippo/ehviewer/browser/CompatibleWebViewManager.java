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

/*
 * EhViewer兼容性WebView管理器
 * 
 * 专门修复视频播放黑屏和文字压扁问题
 * 采用保守而稳定的GPU配置策略
 */

package com.hippo.ehviewer.browser;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.R;
import com.tencent.smtt.sdk.QbSdk;

import java.util.HashMap;

/**
 * 兼容性优先的WebView管理器
 * 修复视频黑屏和文字压扁问题
 */
public class CompatibleWebViewManager {

    private static final String TAG = "CompatibleWebViewManager";

    private static CompatibleWebViewManager sInstance;
    private static boolean sIsX5Initialized = false;
    private static boolean sIsX5Available = false;

    // X5初始化回调
    private static QbSdk.PreInitCallback sPreInitCallback;

    /**
     * 获取单例实例
     */
    public static synchronized CompatibleWebViewManager getInstance() {
        if (sInstance == null) {
            sInstance = new CompatibleWebViewManager();
        }
        return sInstance;
    }

    /**
     * ===== 兼容性优先的系统属性配置 =====
     * 修复视频黑屏和文字压扁问题
     */
    public static void preInitializeCompatibleProperties() {
        try {
            Log.i(TAG, "=== COMPATIBILITY FIRST: Pre-initializing stable properties ===");

            // ===== 核心渲染优化 - 保守稳定模式 =====
            System.setProperty("webview.enable_threaded_rendering", "false"); // 禁用多线程渲染（避免冲突）
            System.setProperty("webview.enable_surface_control", "false"); // 禁用surface control
            System.setProperty("webview.enable_hardware_acceleration", "false"); // 禁用强制硬件加速
            System.setProperty("webview.enable_slow_whole_document_draw", "true"); // 启用安全绘制
            System.setProperty("webview.force_hardware_acceleration", "false"); // 不强制硬件加速
            System.setProperty("webview.force_gpu_rendering", "false"); // 不强制GPU渲染
            System.setProperty("webview.force_cpu_rendering", "true"); // 优先CPU渲染（稳定）

            // ===== 网络协议优化 - 基础稳定配置 =====
            System.setProperty("webview.enable_quic", "false"); // 禁用QUIC（避免兼容性问题）
            System.setProperty("webview.enable_http2", "true"); // 保留HTTP2
            System.setProperty("webview.enable_spdy", "false"); // 禁用SPDY
            System.setProperty("webview.enable_webrtc", "false"); // 禁用WebRTC
            System.setProperty("webview.enable_websockets", "true"); // 启用WebSockets
            System.setProperty("webview.enable_http_cache", "true"); // 启用HTTP缓存

            // ===== 权限和功能优化 - 安全第一 =====
            System.setProperty("webview.enable_process_isolation", "false"); // 禁用进程隔离（避免冲突）
            System.setProperty("webview.enable_safe_browsing", "true"); // 启用安全浏览
            System.setProperty("webview.enable_geolocation", "false"); // 禁用地理位置
            System.setProperty("webview.enable_camera", "false"); // 禁用相机
            System.setProperty("webview.enable_microphone", "false"); // 禁用麦克风
            System.setProperty("webview.enable_midi", "false"); // 禁用MIDI
            System.setProperty("webview.enable_gamepad", "false"); // 禁用游戏手柄

            // ===== 存储和数据库优化 - 基础配置 =====
            System.setProperty("webview.enable_database", "true"); // 启用数据库
            System.setProperty("webview.enable_dom_storage", "true"); // 启用DOM存储
            System.setProperty("webview.enable_app_cache", "true"); // 启用应用缓存
            System.setProperty("webview.enable_file_access", "true"); // 启用文件访问
            System.setProperty("webview.database_capacity", "50"); // 50MB数据库容量（适中）

            // ===== 内存和性能优化 - 保守配置 =====
            System.setProperty("webview.max_rendering_threads", "1"); // 1个渲染线程（稳定）
            System.setProperty("webview.tile_cache_size", "8"); // 8MB瓦片缓存（适中）
            System.setProperty("webview.enable_low_end_mode", "true"); // 启用低端模式（兼容性）
            System.setProperty("webview.enable_display_cutout", "false"); // 禁用刘海屏适配
            System.setProperty("webview.enable_display_list_2d_canvas", "false"); // 禁用2D画布
            System.setProperty("webview.enable_rasterization", "false"); // 禁用GPU光栅化
            System.setProperty("webview.enable_zero_copy", "false"); // 禁用零拷贝
            System.setProperty("webview.enable_async_image_decoding", "false"); // 禁用异步图片解码

            // ===== 系统功能优化 - 基础功能 =====
            System.setProperty("webview.enable_accessibility", "true"); // 启用无障碍功能
            System.setProperty("webview.enable_autofill", "false"); // 禁用自动填充
            System.setProperty("webview.enable_notifications", "false"); // 禁用通知
            System.setProperty("webview.enable_pointer_lock", "false"); // 禁用指针锁定
            System.setProperty("webview.enable_remote_debugging", "false"); // 禁用远程调试
            System.setProperty("webview.enable_web_inspector", "false"); // 禁用Web检查器

            // ===== 现代Web功能 - 保守启用 =====
            System.setProperty("webview.enable_legacy_drawing_model", "true"); // 启用遗留绘制模式
            System.setProperty("webview.enable_software_rendering", "true"); // 启用软件渲染
            System.setProperty("webview.enable_shared_array_buffer", "false"); // 禁用共享数组缓冲区
            System.setProperty("webview.enable_web_audio", "true"); // 启用Web Audio
            System.setProperty("webview.enable_webgl", "false"); // 禁用WebGL（避免GPU问题）
            System.setProperty("webview.enable_webgl_draft_extensions", "false"); // 禁用WebGL扩展
            System.setProperty("webview.enable_webgpu", "false"); // 禁用WebGPU
            System.setProperty("webview.enable_offscreen_canvas", "false"); // 禁用离屏画布
            System.setProperty("webview.enable_image_decode_acceleration", "false"); // 禁用图片解码加速

            // ===== 性能配置 - 基础稳定 =====
            System.setProperty("webview.enable_jit", "false"); // 禁用JIT编译
            System.setProperty("webview.enable_optimized_bytecode", "false"); // 禁用优化字节码
            System.setProperty("webview.enable_concurrent_gc", "false"); // 禁用并发垃圾回收
            System.setProperty("webview.gc_interval", "5000"); // GC间隔5秒
            System.setProperty("webview.memory_pressure_threshold", "0.7"); // 内存压力阈值70%

            // ===== GPU和渲染优化 - 禁用GPU加速 =====
            System.setProperty("webview.gpu_memory_limit", "64"); // GPU内存限制64MB（最小）
            System.setProperty("webview.enable_gpu_memory_buffer", "false"); // 禁用GPU内存缓冲
            System.setProperty("webview.enable_gpu_rasterization", "false"); // 禁用GPU光栅化
            System.setProperty("webview.enable_accelerated_video_decode", "false"); // 禁用硬件视频解码

            // ===== 网络优化 - 保守配置 =====
            System.setProperty("webview.max_http_connections", "6"); // 最大HTTP连接数6
            System.setProperty("webview.max_http_connections_per_host", "4"); // 每主机最大连接4
            System.setProperty("webview.http_connection_timeout", "15000"); // 连接超时15秒
            System.setProperty("webview.http_read_timeout", "30000"); // 读取超时30秒

            // ===== 缓存优化 - 适中配置 =====
            System.setProperty("webview.cache.mode", "1"); // LOAD_DEFAULT
            System.setProperty("webview.cache.size", "50"); // 50MB缓存大小
            System.setProperty("webview.enable_disk_cache", "true"); // 启用磁盘缓存
            System.setProperty("webview.disk_cache_size", "30"); // 30MB磁盘缓存

            Log.i(TAG, "=== COMPATIBILITY: System properties optimized for stability ===");

        } catch (Exception e) {
            Log.e(TAG, "Failed to pre-initialize compatible properties", e);
        }
    }

    /**
     * 初始化X5 - 兼容性优先配置
     */
    public void initX5(Context context) {
        if (sIsX5Initialized) {
            Log.d(TAG, "X5 already initialized");
            return;
        }

        Log.i(TAG, "Initializing X5 WebView SDK with compatibility settings...");

        try {
            // 设置X5初始化参数 - 兼容性优先
            HashMap<String, Object> initParams = new HashMap<>();

            // ===== 基础功能 - 保守启用 =====
            initParams.put("QBSDK_DISABLE_UNIFY_REQUEST", "false"); // 启用统一请求
            initParams.put("QBSDK_DISABLE_CRASH_HANDLE", "false"); // 启用崩溃处理
            initParams.put("QBSDK_DISABLE_DOWNLOAD", "false"); // 启用下载功能
            initParams.put("QBSDK_DISABLE_UPDATE", "false"); // 启用自动更新

            // ===== 禁用可能有问题的功能 =====
            initParams.put("QBSDK_DISABLE_MINI_PROGRAM", "true"); // 禁用小程序功能
            initParams.put("QBSDK_DISABLE_FILE_PROVIDER", "false"); // 启用文件提供者
            initParams.put("QBSDK_DISABLE_WEBRTC", "true"); // 禁用WebRTC
            initParams.put("QBSDK_DISABLE_GAME_OPTIMIZE", "true"); // 禁用游戏优化
            initParams.put("QBSDK_DISABLE_BOUNCE_SCROLL", "false"); // 启用弹跳滚动
            initParams.put("QBSDK_DISABLE_MULTI_PROCESS", "true"); // 禁用多进程
            initParams.put("QBSDK_DISABLE_EXCEL", "false"); // 启用Excel功能
            initParams.put("QBSDK_DISABLE_WORD", "false"); // 启用Word功能
            initParams.put("QBSDK_DISABLE_POWERPOINT", "false"); // 启用PowerPoint功能
            initParams.put("QBSDK_DISABLE_PDF", "false"); // 启用PDF功能
            initParams.put("QBSDK_DISABLE_VIDEO", "false"); // 启用视频功能

            // ===== 兼容性优先配置 =====
            initParams.put("QBSDK_ENABLE_RENDER_OPTIMIZE", "false"); // 禁用渲染优化
            initParams.put("QBSDK_ENABLE_MEMORY_OPTIMIZE", "true"); // 启用内存优化
            initParams.put("QBSDK_LOW_MEMORY_MODE", "true"); // 启用低内存模式
            initParams.put("QBSDK_DISABLE_SAFEBROWSING", "false"); // 启用安全浏览
            initParams.put("QBSDK_DISABLE_NOTIFICATION", "true"); // 禁用通知
            initParams.put("QBSDK_DISABLE_GEOLOCATION", "true"); // 禁用地理位置
            initParams.put("QBSDK_DISABLE_CAMERA", "true"); // 禁用相机访问
            initParams.put("QBSDK_DISABLE_MICROPHONE", "true"); // 禁用麦克风
            initParams.put("QBSDK_DISABLE_STORAGE", "false"); // 启用存储

            // ===== 进程和系统优化 - 单进程模式 =====
            initParams.put("QBSDK_ENABLE_PROCESS_ISOLATION", "false"); // 禁用进程隔离
            initParams.put("QBSDK_DISABLE_SYSTEM_LOAD_SO", "false"); // 启用系统库加载
            initParams.put("QBSDK_USE_SOFT_KEYBOARD", "true"); // 使用软键盘
            initParams.put("QBSDK_DISABLE_ACCESSIBILITY", "false"); // 启用无障碍功能
            initParams.put("QBSDK_DISABLE_AUTOFILL", "true"); // 禁用自动填充
            initParams.put("QBSDK_DISABLE_REMOTE_DEBUGGING", "true"); // 禁用远程调试

            // ===== 网络基础配置 =====
            initParams.put("QBSDK_ENABLE_QUIC", "false"); // 禁用QUIC
            initParams.put("QBSDK_DISABLE_HTTP2", "false"); // 启用HTTP2
            initParams.put("QBSDK_DISABLE_SPDY", "true"); // 禁用SPDY
            initParams.put("QBSDK_DISABLE_WEBSOCKETS", "false"); // 启用WebSockets
            initParams.put("QBSDK_ENABLE_HTTP_CACHE", "true"); // 启用HTTP缓存

            // ===== 现代Web功能 - 保守启用 =====
            initParams.put("QBSDK_DISABLE_WEBGL", "true"); // 禁用WebGL
            initParams.put("QBSDK_DISABLE_WEB_AUDIO", "false"); // 启用Web Audio
            initParams.put("QBSDK_DISABLE_SHARED_ARRAY_BUFFER", "true"); // 禁用共享数组缓冲区
            initParams.put("QBSDK_DISABLE_GAMEPAD", "true"); // 禁用游戏手柄
            initParams.put("QBSDK_DISABLE_MIDI", "true"); // 禁用MIDI
            initParams.put("QBSDK_DISABLE_SPEECH_SYNTHESIS", "false"); // 启用语音合成
            initParams.put("QBSDK_DISABLE_SPEECH_RECOGNITION", "false"); // 启用语音识别

            // ===== 渲染优化 - 兼容性优先 =====
            initParams.put("QBSDK_DISABLE_HARDWARE_ACCELERATION", "true"); // 禁用硬件加速
            initParams.put("QBSDK_FORCE_SOFTWARE_RENDERING", "true"); // 强制软件渲染
            initParams.put("QBSDK_DISABLE_GPU_RASTERIZATION", "true"); // 禁用GPU光栅化
            initParams.put("QBSDK_DISABLE_DISPLAY_LIST_2D_CANVAS", "true"); // 禁用2D画布

            // ===== 基础性能配置 =====
            initParams.put("QBSDK_ENABLE_JIT", "false"); // 禁用JIT编译
            initParams.put("QBSDK_ENABLE_CONCURRENT_GC", "false"); // 禁用并发GC
            initParams.put("QBSDK_GPU_MEMORY_LIMIT", "32"); // GPU内存32MB
            initParams.put("QBSDK_MAX_RENDERING_THREADS", "1"); // 最大1个渲染线程
            initParams.put("QBSDK_TILE_CACHE_SIZE", "8"); // 8MB瓦片缓存

            // ===== 网络基础配置 =====
            initParams.put("QBSDK_MAX_HTTP_CONNECTIONS", "6"); // 最大6个HTTP连接
            initParams.put("QBSDK_MAX_HTTP_CONNECTIONS_PER_HOST", "4"); // 每主机4个连接
            initParams.put("QBSDK_HTTP_CONNECTION_TIMEOUT", "15000"); // 连接超时15秒
            initParams.put("QBSDK_HTTP_READ_TIMEOUT", "30000"); // 读取超时30秒

            // ===== 缓存适中配置 =====
            initParams.put("QBSDK_CACHE_MODE", "1"); // LOAD_DEFAULT
            initParams.put("QBSDK_CACHE_SIZE", "50"); // 50MB缓存大小
            initParams.put("QBSDK_ENABLE_DISK_CACHE", "true"); // 启用磁盘缓存
            initParams.put("QBSDK_DISK_CACHE_SIZE", "30"); // 30MB磁盘缓存

            // 设置初始化参数
            QbSdk.initTbsSettings(initParams);

        } catch (Exception e) {
            Log.w(TAG, "Failed to set TBS parameters, continue with default", e);
        }

        // 设置X5初始化回调
        sPreInitCallback = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean success) {
                Log.i(TAG, "X5 WebView init finished, success: " + success);
                sIsX5Initialized = true;
                sIsX5Available = success;

                if (success) {
                    Log.i(TAG, "X5 WebView is available for use with compatibility mode");
                    try {
                        int tbsVersion = QbSdk.getTbsVersion(context);
                        Log.i(TAG, "X5 TBS version: " + tbsVersion);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to get TBS version", e);
                    }
                } else {
                    Log.w(TAG, "X5 WebView init failed, fallback to system WebView");
                }
            }

            @Override
            public void onCoreInitFinished() {
                Log.i(TAG, "X5 core init finished with compatibility settings");
            }
        };

        // 初始化X5 SDK
        QbSdk.initX5Environment(context, sPreInitCallback);

        try {
            QbSdk.setDownloadWithoutWifi(true);
            QbSdk.setNeedInitX5FirstTime(true);
        } catch (Exception e) {
            Log.w(TAG, "Failed to set QbSdk configuration", e);
        }

        Log.i(TAG, "X5 SDK initialized with compatibility-first configuration");
    }

    /**
     * ===== 兼容性优先WebView创建 =====
     */
    @NonNull
    public Object createCompatibleWebView(Context context) {
        Log.i(TAG, "=== Creating WebView with compatibility-first approach ===");

        // 首先应用兼容性属性
        preInitializeCompatibleProperties();

        try {
            // 优先使用X5 WebView（如果可用）
            if (sIsX5Available) {
                Log.i(TAG, "Creating X5 WebView with compatibility settings");
                com.tencent.smtt.sdk.WebView x5WebView = new com.tencent.smtt.sdk.WebView(context);

                // 设置兼容性优先的X5配置
                setupCompatibleX5Settings(x5WebView, context);

                return x5WebView;
            } else {
                Log.w(TAG, "X5 not available, using system WebView with compatibility settings");
                android.webkit.WebView systemWebView = new android.webkit.WebView(context);

                // 设置兼容性优先的系统WebView配置
                setupCompatibleSystemWebViewSettings(systemWebView, context);

                return systemWebView;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create WebView", e);
            // 最后的备用方案
            try {
                android.webkit.WebView fallbackWebView = new android.webkit.WebView(context);
                setupMinimalWebViewSettings(fallbackWebView);
                Log.i(TAG, "Using minimal fallback system WebView");
                return fallbackWebView;
            } catch (Exception fallbackE) {
                Log.e(TAG, "Failed to create fallback WebView", fallbackE);
                throw new RuntimeException("Cannot create any WebView", fallbackE);
            }
        }
    }

    /**
     * 设置兼容性优先的X5配置
     */
    private void setupCompatibleX5Settings(com.tencent.smtt.sdk.WebView x5WebView, Context context) {
        if (x5WebView == null) return;

        try {
            com.tencent.smtt.sdk.WebSettings x5Settings = x5WebView.getSettings();

            // ===== 基础功能启用 =====
            x5Settings.setJavaScriptEnabled(true);
            x5Settings.setDomStorageEnabled(true);

            // ===== 缩放设置 =====
            x5Settings.setSupportZoom(true);
            x5Settings.setBuiltInZoomControls(true);
            x5Settings.setDisplayZoomControls(false);

            // ===== User Agent设置 =====
            String userAgent = x5Settings.getUserAgentString();
            if (userAgent != null && !userAgent.contains("EhViewer")) {
                x5Settings.setUserAgentString(userAgent + " EhViewer/2.0 X5Compatible");
            }

            // ===== 网络和安全设置 =====
            try {
                x5Settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            } catch (Exception e) {
                Log.w(TAG, "X5 setMixedContentMode not supported", e);
            }
            x5Settings.setAllowFileAccess(true);
            x5Settings.setAllowContentAccess(true);

            // ===== 缓存设置 =====
            x5Settings.setCacheMode(com.tencent.smtt.sdk.WebSettings.LOAD_DEFAULT);
            x5Settings.setAppCacheEnabled(true);
            x5Settings.setDatabaseEnabled(true);

            // ===== X5特有设置 =====
            x5Settings.setUseWideViewPort(true);
            x5Settings.setLoadWithOverviewMode(true);

            // ===== 兼容性优先设置 =====
            try {
                // 禁用可能有问题的渲染优化
                x5Settings.setLoadsImagesAutomatically(true);
                x5Settings.setBlockNetworkImage(false);

                // 字体大小设置 - 正常范围
                x5Settings.setMinimumFontSize(8);
                x5Settings.setMinimumLogicalFontSize(8);
                x5Settings.setDefaultFontSize(16);
                x5Settings.setDefaultFixedFontSize(13);

                // JavaScript设置
                x5Settings.setJavaScriptCanOpenWindowsAutomatically(false);

                // 禁用硬件加速（关键修复）
                x5WebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                // 媒体播放设置
                try {
                    x5Settings.setMediaPlaybackRequiresUserGesture(false);
                } catch (Exception e) {
                    Log.w(TAG, "X5 setMediaPlaybackRequiresUserGesture not supported", e);
                }

                // ===== 安全设置 =====
                x5Settings.setAppCacheEnabled(true);
                x5Settings.setAppCacheMaxSize(50 * 1024 * 1024); // 50MB
                x5Settings.setDatabaseEnabled(true);
                x5Settings.setDomStorageEnabled(true);
                x5Settings.setGeolocationEnabled(false);
                x5Settings.setSupportZoom(true);
                x5Settings.setUseWideViewPort(true);
                x5Settings.setLoadWithOverviewMode(true);

                // ===== 保守的渲染设置 =====
                x5Settings.setCacheMode(com.tencent.smtt.sdk.WebSettings.LOAD_DEFAULT);
                x5Settings.setAllowFileAccess(true);
                x5Settings.setAllowContentAccess(true);

            } catch (Exception e) {
                Log.w(TAG, "Failed to apply compatible X5 settings", e);
            }

            Log.d(TAG, "X5 WebView settings configured for compatibility");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure compatible X5 WebView settings", e);
        }
    }

    /**
     * 设置兼容性优先的系统WebView配置
     */
    private void setupCompatibleSystemWebViewSettings(android.webkit.WebView webView, Context context) {
        if (webView == null) return;

        try {
            android.webkit.WebSettings settings = webView.getSettings();

            // ===== 基础功能 =====
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);

            // ===== 缩放设置 =====
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);

            // ===== User Agent =====
            String userAgent = settings.getUserAgentString();
            if (!userAgent.contains("EhViewer")) {
                settings.setUserAgentString(userAgent + " EhViewer/2.0 SystemCompatible");
            }

            // ===== 网络和权限设置 =====
            settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowUniversalAccessFromFileURLs(false); // 安全考虑
            settings.setAllowFileAccessFromFileURLs(false); // 安全考虑

            // ===== 缓存设置 =====
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);

            // ===== 视口和布局 =====
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setSupportMultipleWindows(false); // 禁用多窗口

            // ===== 媒体设置 =====
            try {
                settings.setMediaPlaybackRequiresUserGesture(false);
            } catch (Exception e) {
                Log.w(TAG, "setMediaPlaybackRequiresUserGesture not supported", e);
            }

            // ===== 渲染优化 - 保守设置 =====
            settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.NORMAL);

            // ===== 图片加载 =====
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);

            // ===== 字体设置 - 正常范围 =====
            settings.setMinimumFontSize(8);
            settings.setMinimumLogicalFontSize(8);
            settings.setDefaultFontSize(16);
            settings.setDefaultFixedFontSize(13);

            // ===== 关键修复：禁用硬件加速 =====
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            // ===== JavaScript设置 =====
            settings.setJavaScriptCanOpenWindowsAutomatically(false);

            Log.d(TAG, "System WebView settings configured for compatibility");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure compatible system WebView settings", e);
        }
    }

    /**
     * 设置最小化WebView配置（紧急情况使用）
     */
    private void setupMinimalWebViewSettings(android.webkit.WebView webView) {
        try {
            android.webkit.WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            
            // 确保软件渲染
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            
            Log.d(TAG, "Minimal WebView settings applied");
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply minimal settings", e);
        }
    }

    /**
     * 检查X5是否可用
     */
    public boolean isX5Available() {
        return sIsX5Available;
    }

    /**
     * 检查X5是否已初始化
     */
    public boolean isX5Initialized() {
        return sIsX5Initialized;
    }

    /**
     * 获取X5版本信息
     */
    @Nullable
    public String getX5Version() {
        if (sIsX5Available) {
            try {
                int version = QbSdk.getTbsVersion(null);
                return "TBS " + version + " (Compatible)";
            } catch (Exception e) {
                Log.e(TAG, "Failed to get X5 version", e);
                return "TBS Unknown";
            }
        }
        return null;
    }

    /**
     * 获取WebView类型描述
     */
    @NonNull
    public String getWebViewTypeDescription() {
        if (sIsX5Available) {
            return "腾讯X5 WebView (兼容模式)";
        } else if (sIsX5Initialized) {
            return "系统WebView (兼容模式)";
        } else {
            return "系统WebView (未初始化X5)";
        }
    }
}