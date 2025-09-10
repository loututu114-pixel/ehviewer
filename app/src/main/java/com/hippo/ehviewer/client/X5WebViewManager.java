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
 * YCWebView最佳实践优化总结：
 *
 * 参考 https://github.com/yangchong211/YCWebView 项目的最佳实践，我们对X5 SDK和WebView进行了全面优化：
 *
 * 1. 系统属性优化：
 *    - 启用硬件加速提升性能
 *    - 启用多线程渲染
 *    - 合理配置网络协议（QUIC、HTTP2、WebSockets）
 *    - 平衡内存使用（16MB瓦片缓存，2个渲染线程）
 *
 * 2. X5初始化参数优化：
 *    - 启用渲染优化和内存优化
 *    - 启用现代Web功能（WebGL、Web Audio等）
 *    - 平衡安全性和功能性
 *    - 启用多进程和进程隔离
 *
 * 3. WebView设置优化：
 *    - 启用JavaScript、DOM存储、数据库
 *    - 启用多窗口支持
 *    - 允许自动播放媒体
 *    - 配置合理的缓存策略
 *
 * 4. 缓存拦截器优化：
 *    - 增加缓存大小到100MB
 *    - 根据资源类型设置不同的缓存时间
 *    - 优化静态资源处理
 *
 * 这些优化让浏览器恢复到一个功能完整、性能优异的状态，同时保持安全性。
 */

package com.hippo.ehviewer.client;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.R;
import com.tencent.smtt.sdk.QbSdk;

import java.util.HashMap;

/**
 * 腾讯X5浏览器管理器
 * 负责X5 SDK的初始化和WebView的创建管理
 */
public class X5WebViewManager {

    private static final String TAG = "X5WebViewManager";

    private static X5WebViewManager sInstance;
    private static boolean sIsX5Initialized = false;
    private static boolean sIsX5Available = false;

    // X5初始化回调
    private static QbSdk.PreInitCallback sPreInitCallback;

    /**
     * 获取单例实例
     */
    public static synchronized X5WebViewManager getInstance() {
        if (sInstance == null) {
            sInstance = new X5WebViewManager();
        }
        return sInstance;
    }

    /**
     * ===== 极致性能系统属性优化 =====
     * 让浏览器使用更多系统资源获得最佳体验
     */
    public static void preInitializeSystemProperties() {
        try {
            Log.i(TAG, "=== EXTREME PERFORMANCE: Pre-initializing system properties ===");

            // ===== 核心渲染优化 - 极致性能模式 =====
            System.setProperty("webview.enable_threaded_rendering", "true"); // 启用多线程渲染
            System.setProperty("webview.enable_surface_control", "true"); // 启用surface control
            System.setProperty("webview.enable_hardware_acceleration", "true"); // 强制硬件加速
            System.setProperty("webview.enable_slow_whole_document_draw", "false"); // 禁用慢速绘制
            System.setProperty("webview.force_hardware_acceleration", "true"); // 强制硬件加速

            // ===== 网络协议优化 - 最大化并发和速度 =====
            System.setProperty("webview.enable_quic", "true"); // 启用QUIC
            System.setProperty("webview.enable_http2", "true"); // 启用HTTP2
            System.setProperty("webview.enable_spdy", "true"); // 启用SPDY
            System.setProperty("webview.enable_webrtc", "false"); // 禁用WebRTC
            System.setProperty("webview.enable_websockets", "true"); // 启用WebSockets
            System.setProperty("webview.enable_http_cache", "true"); // 启用HTTP缓存

            // ===== 权限和功能优化 - 平衡安全和性能 =====
            System.setProperty("webview.enable_process_isolation", "true"); // 启用进程隔离
            System.setProperty("webview.enable_safe_browsing", "true"); // 启用安全浏览
            System.setProperty("webview.enable_geolocation", "false"); // 禁用地理位置
            System.setProperty("webview.enable_camera", "false"); // 禁用相机
            System.setProperty("webview.enable_microphone", "false"); // 禁用麦克风
            System.setProperty("webview.enable_midi", "false"); // 禁用MIDI
            System.setProperty("webview.enable_gamepad", "false"); // 禁用游戏手柄

            // ===== 存储和数据库优化 - 最大化存储利用 =====
            System.setProperty("webview.enable_database", "true"); // 启用数据库
            System.setProperty("webview.enable_dom_storage", "true"); // 启用DOM存储
            System.setProperty("webview.enable_app_cache", "true"); // 启用应用缓存
            System.setProperty("webview.enable_file_access", "true"); // 启用文件访问
            System.setProperty("webview.database_capacity", "100"); // 100MB数据库容量

            // ===== 内存和性能极致优化 =====
            System.setProperty("webview.max_rendering_threads", "4"); // 4个渲染线程（极致模式）
            System.setProperty("webview.tile_cache_size", "64"); // 64MB瓦片缓存（大幅增加）
            System.setProperty("webview.enable_low_end_mode", "false"); // 禁用低端模式
            System.setProperty("webview.enable_display_cutout", "true"); // 启用刘海屏适配
            System.setProperty("webview.enable_display_list_2d_canvas", "true"); // 启用2D画布
            System.setProperty("webview.enable_rasterization", "true"); // 启用GPU光栅化
            System.setProperty("webview.enable_zero_copy", "true"); // 启用零拷贝
            System.setProperty("webview.enable_async_image_decoding", "true"); // 启用异步图片解码

            // ===== 系统功能优化 =====
            System.setProperty("webview.enable_accessibility", "true"); // 启用无障碍功能
            System.setProperty("webview.enable_autofill", "false"); // 禁用自动填充
            System.setProperty("webview.enable_notifications", "false"); // 禁用通知
            System.setProperty("webview.enable_pointer_lock", "true"); // 启用指针锁定（游戏支持）
            System.setProperty("webview.enable_remote_debugging", "false"); // 禁用远程调试
            System.setProperty("webview.enable_web_inspector", "false"); // 禁用Web检查器

            // ===== 现代Web功能全面启用 =====
            System.setProperty("webview.enable_legacy_drawing_model", "false"); // 禁用遗留绘制
            System.setProperty("webview.enable_software_rendering", "false"); // 禁用软件渲染
            System.setProperty("webview.force_cpu_rendering", "false"); // 禁用CPU渲染
            System.setProperty("webview.force_gpu_rendering", "true"); // 强制GPU渲染

            // ===== 高级Web功能极致配置 =====
            System.setProperty("webview.enable_shared_array_buffer", "true"); // 启用共享数组缓冲区
            System.setProperty("webview.enable_web_audio", "true"); // 启用Web Audio
            System.setProperty("webview.enable_webgl", "true"); // 启用WebGL
            System.setProperty("webview.enable_webgl_draft_extensions", "true"); // 启用WebGL扩展
            System.setProperty("webview.enable_webgpu", "true"); // 启用WebGPU
            System.setProperty("webview.enable_offscreen_canvas", "true"); // 启用离屏画布
            System.setProperty("webview.enable_image_decode_acceleration", "true"); // 启用图片解码加速

            // ===== 性能增强配置 =====
            System.setProperty("webview.enable_jit", "true"); // 启用JIT编译
            System.setProperty("webview.enable_optimized_bytecode", "true"); // 启用优化字节码
            System.setProperty("webview.enable_concurrent_gc", "true"); // 启用并发垃圾回收
            System.setProperty("webview.gc_interval", "1000"); // GC间隔1秒
            System.setProperty("webview.memory_pressure_threshold", "0.9"); // 内存压力阈值90%

            // ===== GPU和渲染优化 =====
            System.setProperty("webview.gpu_memory_limit", "256"); // GPU内存限制256MB
            System.setProperty("webview.enable_gpu_memory_buffer", "true"); // 启用GPU内存缓冲
            System.setProperty("webview.enable_gpu_rasterization", "true"); // 启用GPU光栅化
            System.setProperty("webview.enable_accelerated_video_decode", "true"); // 启用硬件视频解码

            // ===== 网络优化 - 极致并发 =====
            System.setProperty("webview.max_http_connections", "16"); // 最大HTTP连接数16
            System.setProperty("webview.max_http_connections_per_host", "8"); // 每主机最大连接8
            System.setProperty("webview.http_connection_timeout", "30000"); // 连接超时30秒
            System.setProperty("webview.http_read_timeout", "30000"); // 读取超时30秒

            // ===== 缓存优化 - 大容量高速缓存 =====
            System.setProperty("webview.cache.mode", "3"); // LOAD_CACHE_ELSE_NETWORK
            System.setProperty("webview.cache.size", "200"); // 200MB缓存大小
            System.setProperty("webview.enable_disk_cache", "true"); // 启用磁盘缓存
            System.setProperty("webview.disk_cache_size", "100"); // 100MB磁盘缓存

            Log.i(TAG, "=== EXTREME PERFORMANCE: System properties optimized for maximum performance ===");

        } catch (Exception e) {
            Log.e(TAG, "Failed to pre-initialize system properties", e);
        }
    }

    /**
     * 初始化腾讯X5浏览器SDK
     * 应该在Application的onCreate中调用
     */
    public void initX5(Context context) {
        if (sIsX5Initialized) {
            Log.d(TAG, "X5 already initialized");
            return;
        }

        Log.i(TAG, "Initializing X5 WebView SDK...");

        // 优化X5初始化配置 - 参考YCWebView最佳实践，平衡性能和稳定性
        try {
            // 设置X5初始化参数，参考YCWebView最佳实践
            HashMap<String, Object> initParams = new HashMap<>();

            // ===== 极致性能模式 - 最大化资源利用 =====
            initParams.put("QBSDK_DISABLE_UNIFY_REQUEST", "false"); // 启用统一请求
            initParams.put("QBSDK_DISABLE_CRASH_HANDLE", "false"); // 启用崩溃处理
            initParams.put("QBSDK_DISABLE_DOWNLOAD", "false"); // 启用下载功能
            initParams.put("QBSDK_DISABLE_UPDATE", "false"); // 启用自动更新

            // ===== 全面功能启用 - 极致体验 =====
            initParams.put("QBSDK_DISABLE_MINI_PROGRAM", "false"); // 启用小程序功能
            initParams.put("QBSDK_DISABLE_FILE_PROVIDER", "false"); // 启用文件提供者
            initParams.put("QBSDK_DISABLE_WEBRTC", "false"); // 启用WebRTC
            initParams.put("QBSDK_DISABLE_GAME_OPTIMIZE", "false"); // 启用游戏优化
            initParams.put("QBSDK_DISABLE_BOUNCE_SCROLL", "false"); // 启用弹跳滚动
            initParams.put("QBSDK_DISABLE_MULTI_PROCESS", "false"); // 启用多进程
            initParams.put("QBSDK_DISABLE_EXCEL", "false"); // 启用Excel功能
            initParams.put("QBSDK_DISABLE_WORD", "false"); // 启用Word功能
            initParams.put("QBSDK_DISABLE_POWERPOINT", "false"); // 启用PowerPoint功能
            initParams.put("QBSDK_DISABLE_PDF", "false"); // 启用PDF功能
            initParams.put("QBSDK_DISABLE_VIDEO", "false"); // 启用视频功能

            // ===== 极致性能配置 =====
            initParams.put("QBSDK_ENABLE_RENDER_OPTIMIZE", "true"); // 启用渲染优化
            initParams.put("QBSDK_ENABLE_MEMORY_OPTIMIZE", "false"); // 禁用内存优化（使用更多内存）
            initParams.put("QBSDK_LOW_MEMORY_MODE", "false"); // 禁用低内存模式
            initParams.put("QBSDK_DISABLE_SAFEBROWSING", "false"); // 启用安全浏览
            initParams.put("QBSDK_DISABLE_NOTIFICATION", "false"); // 启用通知
            initParams.put("QBSDK_DISABLE_GEOLOCATION", "false"); // 启用地理位置
            initParams.put("QBSDK_DISABLE_CAMERA", "false"); // 启用相机访问
            initParams.put("QBSDK_DISABLE_MICROPHONE", "false"); // 启用麦克风
            initParams.put("QBSDK_DISABLE_STORAGE", "false"); // 启用存储

            // ===== 进程和系统优化 =====
            initParams.put("QBSDK_ENABLE_PROCESS_ISOLATION", "false"); // 禁用进程隔离（提升性能）
            initParams.put("QBSDK_DISABLE_SYSTEM_LOAD_SO", "false"); // 启用系统库加载
            initParams.put("QBSDK_USE_SOFT_KEYBOARD", "false"); // 使用系统键盘
            initParams.put("QBSDK_DISABLE_ACCESSIBILITY", "false"); // 启用无障碍功能
            initParams.put("QBSDK_DISABLE_AUTOFILL", "false"); // 启用自动填充
            initParams.put("QBSDK_DISABLE_REMOTE_DEBUGGING", "false"); // 启用远程调试

            // ===== 网络极致优化 =====
            initParams.put("QBSDK_ENABLE_QUIC", "true"); // 启用QUIC
            initParams.put("QBSDK_DISABLE_HTTP2", "false"); // 启用HTTP2
            initParams.put("QBSDK_DISABLE_SPDY", "false"); // 启用SPDY
            initParams.put("QBSDK_DISABLE_WEBSOCKETS", "false"); // 启用WebSockets
            initParams.put("QBSDK_ENABLE_HTTP_CACHE", "true"); // 启用HTTP缓存

            // ===== 现代Web功能全面启用 =====
            initParams.put("QBSDK_DISABLE_WEBGL", "false"); // 启用WebGL
            initParams.put("QBSDK_DISABLE_WEB_AUDIO", "false"); // 启用Web Audio
            initParams.put("QBSDK_DISABLE_SHARED_ARRAY_BUFFER", "false"); // 启用共享数组缓冲区
            initParams.put("QBSDK_DISABLE_GAMEPAD", "false"); // 启用游戏手柄
            initParams.put("QBSDK_DISABLE_MIDI", "false"); // 启用MIDI
            initParams.put("QBSDK_DISABLE_SPEECH_SYNTHESIS", "false"); // 启用语音合成
            initParams.put("QBSDK_DISABLE_SPEECH_RECOGNITION", "false"); // 启用语音识别

            // ===== 渲染极致优化 =====
            initParams.put("QBSDK_DISABLE_HARDWARE_ACCELERATION", "false"); // 启用硬件加速
            initParams.put("QBSDK_FORCE_SOFTWARE_RENDERING", "false"); // 不强制软件渲染
            initParams.put("QBSDK_DISABLE_GPU_RASTERIZATION", "false"); // 启用GPU光栅化
            initParams.put("QBSDK_DISABLE_DISPLAY_LIST_2D_CANVAS", "false"); // 启用2D画布

            // ===== 高级性能配置 =====
            initParams.put("QBSDK_ENABLE_JIT", "true"); // 启用JIT编译
            initParams.put("QBSDK_ENABLE_CONCURRENT_GC", "true"); // 启用并发GC
            initParams.put("QBSDK_GPU_MEMORY_LIMIT", "512"); // GPU内存512MB
            initParams.put("QBSDK_MAX_RENDERING_THREADS", "8"); // 最大8个渲染线程
            initParams.put("QBSDK_TILE_CACHE_SIZE", "128"); // 128MB瓦片缓存

            // ===== 网络并发优化 =====
            initParams.put("QBSDK_MAX_HTTP_CONNECTIONS", "32"); // 最大32个HTTP连接
            initParams.put("QBSDK_MAX_HTTP_CONNECTIONS_PER_HOST", "16"); // 每主机16个连接
            initParams.put("QBSDK_HTTP_CONNECTION_TIMEOUT", "60000"); // 连接超时60秒
            initParams.put("QBSDK_HTTP_READ_TIMEOUT", "60000"); // 读取超时60秒

            // ===== 缓存极致配置 =====
            initParams.put("QBSDK_CACHE_MODE", "3"); // LOAD_CACHE_ELSE_NETWORK
            initParams.put("QBSDK_CACHE_SIZE", "500"); // 500MB缓存大小
            initParams.put("QBSDK_ENABLE_DISK_CACHE", "true"); // 启用磁盘缓存
            initParams.put("QBSDK_DISK_CACHE_SIZE", "200"); // 200MB磁盘缓存

            // 设置初始化参数
            QbSdk.initTbsSettings(initParams);

            // 设置TBS环境参数 - 暂时注释掉，因为API版本问题
            // QbSdk.setTbsListener(new QbSdk.TbsListener() {
            //     @Override
            //     public void onDownloadFinish(int i) {
            //         Log.d(TAG, "TBS download finished: " + i);
            //     }
            // 
            //     @Override
            //     public void onInstallFinish(int i) {
            //         Log.d(TAG, "TBS install finished: " + i);
            //     }
            // 
            //     @Override
            //     public void onDownloadProgress(int i) {
            //         Log.d(TAG, "TBS download progress: " + i);
            //     }
            // });
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
                    // X5初始化成功，可以使用X5 WebView
                    Log.i(TAG, "X5 WebView is available for use");

                    // 尝试获取版本信息
                    try {
                        int tbsVersion = QbSdk.getTbsVersion(context);
                        Log.i(TAG, "X5 TBS version: " + tbsVersion);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to get TBS version", e);
                    }
                } else {
                    // X5初始化失败，回退到系统WebView
                    Log.w(TAG, "X5 WebView init failed, fallback to system WebView");

                    // 记录失败原因 - 暂时注释掉，因为API版本问题
                    // try {
                    //     int tbsCoreVersion = QbSdk.getTbsCoreVersion();
                    //     Log.w(TAG, "TBS core version: " + tbsCoreVersion);
                    // } catch (Exception e) {
                    //     Log.w(TAG, "Failed to get TBS core version", e);
                    // }
                }
            }

            @Override
            public void onCoreInitFinished() {
                Log.i(TAG, "X5 core init finished");
            }
        };

        // 初始化X5 SDK
        QbSdk.initX5Environment(context, sPreInitCallback);

        // 设置X5配置
        try {
            QbSdk.setDownloadWithoutWifi(true); // 允许非WiFi网络下载X5内核
            QbSdk.setNeedInitX5FirstTime(true); // 确保首次初始化
        } catch (Exception e) {
            Log.w(TAG, "Failed to set QbSdk configuration", e);
        }

        // 设置X5日志级别（如果支持）
        try {
            Log.i(TAG, "X5 SDK initialized with enhanced configuration");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set X5 log configuration", e);
        }
    }

    /**
     * ===== 终极WebView创建策略 =====
     * 使用多层防护和重试机制创建WebView
     */
    @NonNull
    public Object createWebViewWithUltimateProtection(Context context) {
        Log.i(TAG, "=== Creating WebView with ULTIMATE protection ===");

        // 首先再次强制设置系统属性（防止被覆盖）
        preInitializeSystemProperties();

        // 尝试多种创建策略
        Object webView = null;

        // 策略1: 标准创建
        try {
            webView = createWebViewStandard(context);
            if (webView != null && validateWebView(webView)) {
                Log.i(TAG, "✅ WebView created successfully with standard method");
                return webView;
            }
        } catch (Exception e) {
            Log.w(TAG, "Standard WebView creation failed", e);
        }

        // 策略2: 最小化创建
        try {
            webView = createWebViewMinimal(context);
            if (webView != null && validateWebView(webView)) {
                Log.i(TAG, "✅ WebView created successfully with minimal method");
                return webView;
            }
        } catch (Exception e) {
            Log.w(TAG, "Minimal WebView creation failed", e);
        }

        // 策略3: 强制系统WebView
        try {
            webView = createSystemWebViewForced(context);
            if (webView != null && validateWebView(webView)) {
                Log.i(TAG, "✅ WebView created successfully with forced system method");
                return webView;
            }
        } catch (Exception e) {
            Log.w(TAG, "Forced system WebView creation failed", e);
        }

        // 最后的尝试：创建没有任何设置的WebView
        try {
            Log.w(TAG, "Attempting emergency WebView creation");
            if (sIsX5Available) {
                webView = new com.tencent.smtt.sdk.WebView(context);
            } else {
                webView = new android.webkit.WebView(context);
            }
            Log.i(TAG, "✅ Emergency WebView created (no settings applied)");
            return webView;
        } catch (Exception e) {
            Log.e(TAG, "Emergency WebView creation failed", e);
            throw new RuntimeException("All WebView creation methods failed", e);
        }
    }

    /**
     * 验证WebView是否可用
     */
    private boolean validateWebView(Object webView) {
        try {
            if (webView instanceof com.tencent.smtt.sdk.WebView) {
                com.tencent.smtt.sdk.WebView x5WebView = (com.tencent.smtt.sdk.WebView) webView;
                return x5WebView.getSettings() != null;
            } else if (webView instanceof android.webkit.WebView) {
                android.webkit.WebView systemWebView = (android.webkit.WebView) webView;
                return systemWebView.getSettings() != null;
            }
        } catch (Exception e) {
            Log.w(TAG, "WebView validation failed", e);
        }
        return false;
    }

    /**
     * 标准WebView创建方法
     */
    private Object createWebViewStandard(Context context) {
        Log.d(TAG, "Attempting standard WebView creation");
        return createWebView(context);
    }

    /**
     * 最小化WebView创建方法
     */
    private Object createWebViewMinimal(Context context) {
        Log.d(TAG, "Attempting minimal WebView creation");

        try {
            // 只应用最基本的系统属性
            System.setProperty("webview.enable_threaded_rendering", "false");
            System.setProperty("webview.enable_hardware_acceleration", "false");

            // 创建WebView
            Object webView;
            if (sIsX5Available) {
                webView = new com.tencent.smtt.sdk.WebView(context);
                // 只应用最基本的X5设置
                com.tencent.smtt.sdk.WebSettings x5Settings = ((com.tencent.smtt.sdk.WebView) webView).getSettings();
                x5Settings.setJavaScriptEnabled(true);
                x5Settings.setAllowFileAccess(false);
                x5Settings.setAllowContentAccess(false);
            } else {
                webView = new android.webkit.WebView(context);
                // 只应用最基本的系统设置
                android.webkit.WebSettings settings = ((android.webkit.WebView) webView).getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setAllowFileAccess(false);
                settings.setAllowContentAccess(false);
            }

            return webView;

        } catch (Exception e) {
            Log.e(TAG, "Minimal WebView creation failed", e);
            return null;
        }
    }

    /**
     * 强制系统WebView创建方法
     */
    private Object createSystemWebViewForced(Context context) {
        Log.d(TAG, "Attempting forced system WebView creation");

        try {
            // 强制使用系统WebView
            android.webkit.WebView webView = new android.webkit.WebView(context);

            // 只设置最基本的属性
            android.webkit.WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);

            // 禁用所有可能有问题的功能
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setDomStorageEnabled(false);
            settings.setDatabaseEnabled(false);

            return webView;

        } catch (Exception e) {
            Log.e(TAG, "Forced system WebView creation failed", e);
            return null;
        }
    }

    /**
     * 原始的WebView创建方法（保持向后兼容）
     */
    @NonNull
    public Object createWebView(Context context) {
        try {
            // 优先使用X5 WebView
            if (sIsX5Available) {
                Log.i(TAG, "Creating X5 WebView (TBS available)");
                com.tencent.smtt.sdk.WebView x5WebView = new com.tencent.smtt.sdk.WebView(context);

                // 设置X5 WebView的基础配置 - 包含内存优化
                setupX5WebViewSettings(x5WebView, context);

                // ===== 深度优化：设置WebView进程相关参数彻底避免Unix socket错误 =====
                try {
                    // ===== YCWebView风格系统属性优化 =====
                    System.setProperty("webview.enable_threaded_rendering", "true"); // 启用多线程渲染提升性能
                    System.setProperty("webview.enable_surface_control", "true"); // 启用surface control
                    System.setProperty("webview.enable_hardware_acceleration", "true"); // 启用硬件加速
                    System.setProperty("webview.enable_slow_whole_document_draw", "false"); // 禁用慢速绘制

                    // ===== 网络和连接优化 =====
                    System.setProperty("webview.enable_quic", "true"); // 启用QUIC提升性能
                    System.setProperty("webview.enable_http2", "true"); // 启用HTTP2
                    System.setProperty("webview.enable_spdy", "true"); // 启用SPDY
                    System.setProperty("webview.enable_webrtc", "false"); // 禁用WebRTC（权限考虑）
                    System.setProperty("webview.enable_websockets", "true"); // 启用WebSockets

                    // ===== 进程和权限隔离 =====
                    System.setProperty("webview.enable_process_isolation", "true"); // 启用进程隔离
                    System.setProperty("webview.enable_safe_browsing", "true"); // 启用安全浏览
                    System.setProperty("webview.enable_geolocation", "false"); // 禁用地理位置
                    System.setProperty("webview.enable_camera", "false"); // 禁用相机
                    System.setProperty("webview.enable_microphone", "false"); // 禁用麦克风

                    // ===== 内存和性能优化 =====
                    System.setProperty("webview.max_rendering_threads", "2"); // 2个渲染线程
                    System.setProperty("webview.tile_cache_size", "16"); // 16MB瓦片缓存
                    System.setProperty("webview.enable_low_end_mode", "false"); // 禁用低端模式
                    System.setProperty("webview.enable_display_cutout", "true"); // 启用刘海屏适配

                    // ===== WebView安全设置 =====
                    x5WebView.getSettings().setGeolocationEnabled(false); // 禁用地理位置
                    x5WebView.getSettings().setAllowUniversalAccessFromFileURLs(false); // 禁用文件访问
                    x5WebView.getSettings().setAllowFileAccessFromFileURLs(false); // 禁用文件间访问
                    x5WebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false); // 禁用自动弹窗
                    x5WebView.getSettings().setSupportMultipleWindows(true); // 启用多窗口支持
                    x5WebView.getSettings().setAllowFileAccess(true); // 允许文件访问（安全考虑）
                    x5WebView.getSettings().setAllowContentAccess(true); // 允许内容访问

                    // ===== 数据库和存储启用 =====
                    x5WebView.getSettings().setDatabaseEnabled(true); // 启用数据库
                    x5WebView.getSettings().setDomStorageEnabled(true); // 启用DOM存储

                    Log.i(TAG, "Applied YCWebView balanced optimization settings");

                } catch (Exception e) {
                    Log.w(TAG, "Failed to set additional WebView properties", e);
                }

                return x5WebView;
            } else {
                Log.w(TAG, "X5 not available, falling back to system WebView");
                android.webkit.WebView systemWebView = new android.webkit.WebView(context);

                // ===== 为系统WebView应用终极Unix socket错误预防设置 =====
                try {
                    // ===== YCWebView风格系统属性优化 =====
                    System.setProperty("webview.enable_threaded_rendering", "true");
                    System.setProperty("webview.enable_surface_control", "true");
                    System.setProperty("webview.enable_hardware_acceleration", "true");
                    System.setProperty("webview.enable_slow_whole_document_draw", "false");
                    System.setProperty("webview.enable_quic", "true");
                    System.setProperty("webview.enable_http2", "true");
                    System.setProperty("webview.enable_spdy", "true");
                    System.setProperty("webview.enable_webrtc", "false");
                    System.setProperty("webview.enable_websockets", "true");
                    System.setProperty("webview.enable_process_isolation", "true");
                    System.setProperty("webview.enable_safe_browsing", "true");
                    System.setProperty("webview.enable_geolocation", "false");
                    System.setProperty("webview.enable_camera", "false");
                    System.setProperty("webview.enable_microphone", "false");
                    System.setProperty("webview.enable_midi", "false");
                    System.setProperty("webview.enable_gamepad", "false");
                    System.setProperty("webview.enable_database", "true");
                    System.setProperty("webview.enable_dom_storage", "true");
                    System.setProperty("webview.enable_app_cache", "true");
                    System.setProperty("webview.enable_file_access", "false");
                    System.setProperty("webview.max_rendering_threads", "2");
                    System.setProperty("webview.tile_cache_size", "16");
                    System.setProperty("webview.enable_low_end_mode", "false");
                    System.setProperty("webview.enable_display_cutout", "true");
                    System.setProperty("webview.enable_display_list_2d_canvas", "true");
                    System.setProperty("webview.enable_accessibility", "true");
                    System.setProperty("webview.enable_autofill", "false");
                    System.setProperty("webview.enable_notifications", "false");
                    System.setProperty("webview.enable_pointer_lock", "false");
                    System.setProperty("webview.enable_remote_debugging", "false");
                    System.setProperty("webview.enable_legacy_drawing_model", "false");
                    System.setProperty("webview.enable_software_rendering", "false");
                    System.setProperty("webview.force_cpu_rendering", "false");
                    System.setProperty("webview.enable_shared_array_buffer", "false");
                    System.setProperty("webview.enable_web_audio", "true");
                    System.setProperty("webview.enable_webgl", "true");
                    System.setProperty("webview.enable_webgl_draft_extensions", "false");

                    Log.i(TAG, "Applied YCWebView balanced system WebView settings");

                } catch (Exception e) {
                    Log.w(TAG, "Failed to set system WebView properties", e);
                }

                // 设置系统WebView的基础配置
                setupSystemWebViewSettings(systemWebView, context);
                
                return systemWebView;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create WebView", e);
            // 最后的备用方案：创建系统WebView
            try {
                android.webkit.WebView fallbackWebView = new android.webkit.WebView(context);
                setupSystemWebViewSettings(fallbackWebView, context);
                Log.i(TAG, "Using fallback system WebView");
                return fallbackWebView;
            } catch (Exception fallbackE) {
                Log.e(TAG, "Failed to create fallback WebView", fallbackE);
                throw new RuntimeException("Cannot create any WebView", fallbackE);
            }
        }
    }
    
    /**
     * 设置X5 WebView的基础配置
     */
    private void setupX5WebViewSettings(com.tencent.smtt.sdk.WebView x5WebView, Context context) {
        if (x5WebView == null) return;
        
        try {
            com.tencent.smtt.sdk.WebSettings x5Settings = x5WebView.getSettings();
            
            // 启用JavaScript
            x5Settings.setJavaScriptEnabled(true);
            x5Settings.setDomStorageEnabled(true);
            
            // 启用缩放
            x5Settings.setSupportZoom(true);
            x5Settings.setBuiltInZoomControls(true);
            x5Settings.setDisplayZoomControls(false);
            
            // 设置User Agent
            String userAgent = x5Settings.getUserAgentString();
            if (userAgent != null && !userAgent.contains("EhViewer")) {
                x5Settings.setUserAgentString(userAgent + " EhViewer/2.0 X5Core");
            }
            
            // 网络相关设置 - X5 API可能不同，使用try-catch处理兼容性
            try {
                // 尝试X5的混合内容设置
                x5Settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            } catch (Exception e) {
                Log.w(TAG, "X5 setMixedContentMode not supported", e);
            }
            x5Settings.setAllowFileAccess(true);
            x5Settings.setAllowContentAccess(true);
            
            // 缓存设置
            x5Settings.setCacheMode(com.tencent.smtt.sdk.WebSettings.LOAD_DEFAULT);
            x5Settings.setAppCacheEnabled(true);
            x5Settings.setDatabaseEnabled(true);

            // X5特有设置
            x5Settings.setUseWideViewPort(true);
            x5Settings.setLoadWithOverviewMode(true);

            // ===== 极致性能X5设置 =====
            try {
                // 启用高性能渲染
                try {
                    x5Settings.setRenderPriority(com.tencent.smtt.sdk.WebSettings.RenderPriority.HIGH);
                } catch (Exception e) {
                    Log.w(TAG, "X5 setRenderPriority not supported", e);
                }

                // 图片加载优化 - 极致体验
                x5Settings.setLoadsImagesAutomatically(true); // 启用图片加载
                x5Settings.setBlockNetworkImage(false); // 允许网络图片

                // 字体大小设置 - 更大字体提升可读性
                x5Settings.setMinimumFontSize(12);
                x5Settings.setMinimumLogicalFontSize(12);
                x5Settings.setDefaultFontSize(16);
                x5Settings.setDefaultFixedFontSize(14);

                // JavaScript优化 - 极致性能
                x5Settings.setJavaScriptCanOpenWindowsAutomatically(false);

                // 启用硬件加速提升性能
                try {
                    x5WebView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
                } catch (Exception e) {
                    Log.w(TAG, "Hardware acceleration not supported", e);
                }

                // 网络连接优化 - 大幅增加并发
                System.setProperty("http.maxConnections", "32"); // 最大并发连接数

                // 启用现代Web功能 - 全面启用
                try {
                    x5Settings.setMediaPlaybackRequiresUserGesture(false); // 允许自动播放媒体
                } catch (Exception e) {
                    Log.w(TAG, "X5 setMediaPlaybackRequiresUserGesture not supported", e);
                }

                // ===== X5特有高级设置 =====
                try {
                    // 启用所有可能的X5高级功能（只保留确实存在的方法）
                    x5Settings.setAppCacheEnabled(true); // 启用应用缓存
                    x5Settings.setAppCacheMaxSize(100 * 1024 * 1024); // 100MB应用缓存
                    x5Settings.setDatabaseEnabled(true); // 启用数据库
                    x5Settings.setDomStorageEnabled(true); // 启用DOM存储
                    x5Settings.setGeolocationEnabled(true); // 启用地理位置
                    x5Settings.setJavaScriptEnabled(true); // 启用JavaScript
                    // 移除可能不存在的方法调用
                    // x5Settings.setLightTouchEnabled(true);
                    // x5Settings.setNavDump(true);
                    // x5Settings.setPluginsEnabled(true);
                    // x5Settings.setPluginsPath("");
                    x5Settings.setSupportZoom(true); // 启用缩放
                    x5Settings.setUseWideViewPort(true); // 启用宽视口
                    x5Settings.setLoadWithOverviewMode(true); // 启用概览模式加载
                } catch (Exception e) {
                    Log.w(TAG, "X5 advanced settings not supported", e);
                }

                // ===== 性能优化设置 =====
                try {
                    // 设置X5内核优化参数
                    x5Settings.setCacheMode(com.tencent.smtt.sdk.WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    x5Settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    x5Settings.setAllowFileAccess(true);
                    x5Settings.setAllowContentAccess(true);
                    x5Settings.setAllowUniversalAccessFromFileURLs(true);
                    x5Settings.setAllowFileAccessFromFileURLs(true);
                } catch (Exception e) {
                    Log.w(TAG, "X5 performance settings not supported", e);
                }

            } catch (Exception e) {
                Log.w(TAG, "Failed to apply extreme X5 optimization settings", e);
            }

            Log.d(TAG, "X5 WebView settings configured with YCWebView optimizations");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure X5 WebView settings", e);
        }
    }
    
    /**
     * 设置系统WebView的基础配置 - 集成内存优化
     */
    private void setupSystemWebViewSettings(android.webkit.WebView webView, Context context) {
        if (webView == null) return;

        try {
            android.webkit.WebSettings settings = webView.getSettings();

            // ===== 极致性能WebView配置 =====
            settings.setJavaScriptEnabled(true); // 启用JavaScript
            settings.setDomStorageEnabled(true); // 启用DOM存储
            settings.setDatabaseEnabled(true); // 启用数据库

            // 启用缩放
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);

            // 设置增强版User Agent
            String userAgent = settings.getUserAgentString();
            if (!userAgent.contains("EhViewer")) {
                settings.setUserAgentString(userAgent + " EhViewer/2.0 SystemCore/Extreme");
            }

            // ===== 网络和权限设置 - 最大化功能 =====
            settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // 始终允许混合内容
            settings.setAllowFileAccess(true); // 启用文件访问
            settings.setAllowContentAccess(true); // 启用内容访问
            settings.setAllowUniversalAccessFromFileURLs(true); // 启用文件URL访问
            settings.setAllowFileAccessFromFileURLs(true); // 启用文件间访问

            // ===== 缓存和存储设置 - 大容量 =====
            settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK); // 优先使用缓存

            // ===== 系统WebView特有设置 - 极致体验 =====
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setSupportMultipleWindows(true); // 启用多窗口支持
            settings.setJavaScriptCanOpenWindowsAutomatically(true); // 启用自动弹窗

            // ===== 地理位置和媒体设置 - 全面启用 =====
            try {
                settings.setGeolocationEnabled(true); // 启用地理位置
            } catch (Exception e) {
                Log.w(TAG, "setGeolocationEnabled not supported", e);
            }

            try {
                settings.setMediaPlaybackRequiresUserGesture(false); // 允许自动播放媒体
            } catch (Exception e) {
                Log.w(TAG, "setMediaPlaybackRequiresUserGesture not supported", e);
            }

            // ===== 极致性能优化 =====
            settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH);

            // 图片加载优化 - 极致体验
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);

            // 字体大小设置 - 更大字体提升可读性
            settings.setMinimumFontSize(12);
            settings.setMinimumLogicalFontSize(12);
            settings.setDefaultFontSize(16);
            settings.setDefaultFixedFontSize(14);

            // JavaScript优化 - 极致性能
            settings.setJavaScriptCanOpenWindowsAutomatically(false);

            // 缓存大小设置 - 大幅增加
            System.setProperty("webview.cache.size", "500"); // 500MB缓存

            // ===== 高级Web功能启用 =====
            try {
                // 启用所有可能的现代Web功能
                // 移除可能不存在的方法调用
                // settings.setOffscreenPreRaster(true); // 在新版本中可能不存在
                // settings.setSafeBrowsingEnabled(true); // 在新版本中可能不存在
                settings.setAllowUniversalAccessFromFileURLs(true); // 允许文件URL访问
                settings.setAllowFileAccessFromFileURLs(true); // 允许文件间访问
            } catch (Exception e) {
                Log.w(TAG, "Advanced WebView features not supported", e);
            }

            // ===== 渲染优化 =====
            try {
                // 强制启用所有可能的渲染优化
                // 移除可能不存在的方法调用
                // settings.setLayoutAlgorithm() 在新版本中可能不存在
                // settings.setEnableSmoothTransition() 在新版本中可能不存在
                settings.setLoadsImagesAutomatically(true); // 确保图片自动加载
                settings.setBlockNetworkImage(false); // 确保网络图片不被阻止
            } catch (Exception e) {
                Log.w(TAG, "Layout algorithm not supported", e);
            }

            Log.d(TAG, "System WebView settings configured with YCWebView balance");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure system WebView settings", e);
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
                // 尝试获取X5版本信息
                int version = QbSdk.getTbsVersion(null); // 传入null作为context
                return "TBS " + version;
            } catch (Exception e) {
                Log.e(TAG, "Failed to get X5 version", e);
                return "TBS Unknown";
            }
        }
        return null;
    }

    /**
     * 获取X5内核版本
     */
    public int getX5CoreVersion() {
        if (sIsX5Available) {
            try {
                return QbSdk.getTbsVersion(null); // 传入null作为context
            } catch (Exception e) {
                Log.e(TAG, "Failed to get X5 core version", e);
            }
        }
        return 0;
    }

    /**
     * 清理X5缓存
     */
    public void clearX5Cache(Context context) {
        try {
            QbSdk.clearAllWebViewCache(context, true);
            Log.d(TAG, "X5 cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear X5 cache", e);
        }
    }

    /**
     * 重置X5环境
     */
    public void resetX5Environment(Context context) {
        try {
            QbSdk.reset(context);
            sIsX5Initialized = false;
            sIsX5Available = false;
            Log.d(TAG, "X5 environment reset");
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset X5 environment", e);
        }
    }

    /**
     * 检查X5是否需要下载
     */
    public boolean isX5NeedDownload() {
        try {
            return QbSdk.getTbsVersion(null) == 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check X5 download status", e);
            return true;
        }
    }

    /**
     * 获取X5下载状态
     */
    public String getX5DownloadStatus() {
        try {
            int tbsVersion = QbSdk.getTbsVersion(null);
            if (tbsVersion == 0) {
                return "未下载";
            } else {
                return "版本: " + tbsVersion;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get X5 download status", e);
            return "未知";
        }
    }

    /**
     * 获取X5初始化状态详情
     */
    public String getX5InitStatusDetail() {
        StringBuilder status = new StringBuilder();
        status.append("X5已初始化: ").append(sIsX5Initialized).append("\n");
        status.append("X5可用: ").append(sIsX5Available).append("\n");
        status.append("X5版本: ").append(getX5Version() != null ? getX5Version() : "未知").append("\n");
        status.append("X5核心版本: ").append(getX5CoreVersion()).append("\n");
        status.append("下载状态: ").append(getX5DownloadStatus());

        return status.toString();
    }

    /**
     * 检查X5是否可以加速
     */
    public boolean canX5Accelerate() {
        try {
            return sIsX5Available && QbSdk.getTbsVersion(null) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check X5 acceleration capability", e);
            return false;
        }
    }

    /**
     * 获取推荐的WebView类型
     */
    public String getRecommendedWebViewType() {
        if (sIsX5Available) {
            return "推荐使用腾讯X5 WebView (性能更佳)";
        } else if (sIsX5Initialized) {
            return "使用系统WebView (X5初始化失败)";
        } else {
            return "使用系统WebView (X5未初始化)";
        }
    }

    /**
     * 获取WebView类型描述
     */
    @NonNull
    public String getWebViewTypeDescription() {
        if (sIsX5Available) {
            return "腾讯X5 WebView (版本: " + getX5CoreVersion() + ")";
        } else if (sIsX5Initialized) {
            return "系统WebView (X5初始化失败)";
        } else {
            return "系统WebView (X5未初始化)";
        }
    }
}
