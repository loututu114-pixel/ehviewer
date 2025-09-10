package com.hippo.ehviewer.ui.theme;

import android.os.Build;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * WebView深色模式适配器
 * 处理WebView的深色模式设置和CSS注入
 * 
 * 核心特性：
 * 1. Android 10+ 原生深色模式支持
 * 2. CSS深色模式样式注入
 * 3. 网站特定的深色模式适配
 * 4. 自动颜色反转功能
 */
public class WebViewDarkModeAdapter {
    private static final String TAG = "WebViewDarkModeAdapter";
    
    // 深色模式CSS样式
    private static final String DARK_MODE_CSS = """
        /* 全局深色模式样式 */
        :root {
            color-scheme: dark;
        }
        
        /* 基础元素深色化 */
        html, body {
            background-color: #1a1a1a !important;
            color: #e0e0e0 !important;
        }
        
        /* 链接颜色优化 */
        a, a:visited {
            color: #64b5f6 !important;
        }
        
        a:hover {
            color: #90caf9 !important;
        }
        
        /* 输入框深色化 */
        input, textarea, select {
            background-color: #2d2d2d !important;
            color: #e0e0e0 !important;
            border: 1px solid #404040 !important;
        }
        
        /* 按钮深色化 */
        button {
            background-color: #424242 !important;
            color: #e0e0e0 !important;
            border: 1px solid #616161 !important;
        }
        
        button:hover {
            background-color: #616161 !important;
        }
        
        /* 表格深色化 */
        table {
            background-color: #1a1a1a !important;
            color: #e0e0e0 !important;
        }
        
        th, td {
            border-color: #404040 !important;
        }
        
        th {
            background-color: #2d2d2d !important;
        }
        
        /* 图片反色处理 */
        img {
            filter: none !important;
        }
        
        /* 视频不处理 */
        video {
            filter: none !important;
        }
        
        /* 代码块深色化 */
        pre, code {
            background-color: #2d2d2d !important;
            color: #e0e0e0 !important;
            border: 1px solid #404040 !important;
        }
        
        /* 引用块深色化 */
        blockquote {
            background-color: #2d2d2d !important;
            color: #e0e0e0 !important;
            border-left: 4px solid #64b5f6 !important;
        }
    """;
    
    // 网站特定CSS适配
    private static final String SITE_SPECIFIC_CSS = """
        /* 百度搜索深色适配 */
        .baidu-search-dark {
            background-color: #1a1a1a !important;
        }
        
        /* 知乎深色适配 */
        .zhihu-dark .Card {
            background-color: #2d2d2d !important;
            color: #e0e0e0 !important;
        }
        
        /* B站深色适配 */
        .bilibili-dark .video-info {
            background-color: #1a1a1a !important;
            color: #e0e0e0 !important;
        }
    """;
    
    /**
     * 应用深色模式到WebView
     */
    public static void applyDarkMode(@NonNull WebView webView, boolean enabled) {
        if (webView == null) return;
        
        Log.d(TAG, "Applying dark mode to WebView: " + enabled);
        
        try {
            WebSettings settings = webView.getSettings();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+：使用最新的深色模式API
                applyDarkModeApi33(settings, enabled);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-12：使用旧版深色模式API
                applyDarkModeApi29(settings, enabled);
            } else {
                // Android 10以下：使用CSS注入
                applyDarkModeWithCSS(webView, enabled);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply dark mode", e);
            
            // 降级到CSS注入
            try {
                applyDarkModeWithCSS(webView, enabled);
            } catch (Exception fallbackException) {
                Log.e(TAG, "CSS fallback also failed", fallbackException);
            }
        }
    }
    
    /**
     * Android 13+ 深色模式设置
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static void applyDarkModeApi33(WebSettings settings, boolean enabled) {
        try {
            if (enabled) {
                settings.setAlgorithmicDarkeningAllowed(true);
                Log.d(TAG, "Enabled algorithmic darkening (API 33)");
            } else {
                settings.setAlgorithmicDarkeningAllowed(false);
                Log.d(TAG, "Disabled algorithmic darkening (API 33)");
            }
        } catch (Exception e) {
            Log.w(TAG, "API 33 dark mode setting failed", e);
            throw e;
        }
    }
    
    /**
     * Android 10-12 深色模式设置
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static void applyDarkModeApi29(WebSettings settings, boolean enabled) {
        try {
            // 使用反射调用旧版API
            if (enabled) {
                // settings.setForceDark(WebSettings.FORCE_DARK_ON);
                java.lang.reflect.Method setForceDark = settings.getClass()
                    .getMethod("setForceDark", int.class);
                setForceDark.invoke(settings, 2); // FORCE_DARK_ON = 2
                Log.d(TAG, "Enabled force dark (API 29)");
            } else {
                // settings.setForceDark(WebSettings.FORCE_DARK_OFF);
                java.lang.reflect.Method setForceDark = settings.getClass()
                    .getMethod("setForceDark", int.class);
                setForceDark.invoke(settings, 0); // FORCE_DARK_OFF = 0
                Log.d(TAG, "Disabled force dark (API 29)");
            }
        } catch (Exception e) {
            Log.w(TAG, "API 29 dark mode setting failed", e);
            // 降级到CSS注入方式
        }
    }
    
    /**
     * 使用CSS注入实现深色模式
     */
    private static void applyDarkModeWithCSS(WebView webView, boolean enabled) {
        try {
            if (enabled) {
                // 注入深色模式CSS
                String javascript = String.format(
                    "javascript:(function(){" +
                    "var style=document.createElement('style');" +
                    "style.textContent='%s';" +
                    "document.head.appendChild(style);" +
                    "})()",
                    escapeCSSForJavaScript(DARK_MODE_CSS)
                );
                
                webView.loadUrl(javascript);
                Log.d(TAG, "Injected dark mode CSS");
                
                // 添加网站特定适配
                webView.postDelayed(() -> {
                    try {
                        String siteSpecificJs = String.format(
                            "javascript:(function(){" +
                            "var style=document.createElement('style');" +
                            "style.textContent='%s';" +
                            "document.head.appendChild(style);" +
                            "})()",
                            escapeCSSForJavaScript(SITE_SPECIFIC_CSS)
                        );
                        webView.loadUrl(siteSpecificJs);
                        Log.d(TAG, "Injected site-specific dark CSS");
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to inject site-specific CSS", e);
                    }
                }, 1000);
                
            } else {
                // 移除深色模式样式
                String removeScript = 
                    "javascript:(function(){" +
                    "var styles=document.querySelectorAll('style');" +
                    "for(var i=0;i<styles.length;i++){" +
                    "if(styles[i].textContent.includes('color-scheme: dark')){" +
                    "styles[i].remove();" +
                    "}" +
                    "}" +
                    "})()";
                
                webView.loadUrl(removeScript);
                Log.d(TAG, "Removed dark mode CSS");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "CSS injection failed", e);
            throw e;
        }
    }
    
    /**
     * 为JavaScript注入转义CSS内容
     */
    private static String escapeCSSForJavaScript(String css) {
        return css.replace("'", "\\'")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "")
                 .replace("\t", " ");
    }
    
    /**
     * 检查WebView是否支持原生深色模式
     */
    public static boolean isNativeDarkModeSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
    
    /**
     * 检查是否需要CSS降级
     */
    public static boolean needsCSSFallback() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }
    
    /**
     * 为特定URL应用深色模式
     */
    public static void applyDarkModeForUrl(@NonNull WebView webView, String url, boolean enabled) {
        if (webView == null || url == null) return;
        
        // 先应用通用深色模式
        applyDarkMode(webView, enabled);
        
        if (!enabled) return;
        
        // 根据URL应用特定适配
        webView.postDelayed(() -> {
            try {
                if (url.contains("baidu.com")) {
                    applyBaiduDarkMode(webView);
                } else if (url.contains("zhihu.com")) {
                    applyZhihuDarkMode(webView);
                } else if (url.contains("bilibili.com")) {
                    applyBilibiliDarkMode(webView);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to apply site-specific dark mode for " + url, e);
            }
        }, 2000);
    }
    
    /**
     * 百度搜索深色模式适配
     */
    private static void applyBaiduDarkMode(WebView webView) {
        String script = 
            "javascript:(function(){" +
            "document.body.classList.add('baidu-search-dark');" +
            "})()";
        webView.loadUrl(script);
        Log.d(TAG, "Applied Baidu dark mode adaptation");
    }
    
    /**
     * 知乎深色模式适配
     */
    private static void applyZhihuDarkMode(WebView webView) {
        String script = 
            "javascript:(function(){" +
            "document.body.classList.add('zhihu-dark');" +
            "})()";
        webView.loadUrl(script);
        Log.d(TAG, "Applied Zhihu dark mode adaptation");
    }
    
    /**
     * B站深色模式适配
     */
    private static void applyBilibiliDarkMode(WebView webView) {
        String script = 
            "javascript:(function(){" +
            "document.body.classList.add('bilibili-dark');" +
            "})()";
        webView.loadUrl(script);
        Log.d(TAG, "Applied Bilibili dark mode adaptation");
    }
    
    /**
     * 获取深色模式适配统计
     */
    public static String getDarkModeStats() {
        return String.format("原生支持: %s\n需要CSS降级: %s\nAPI级别: %d",
                           isNativeDarkModeSupported() ? "是" : "否",
                           needsCSSFallback() ? "是" : "否",
                           Build.VERSION.SDK_INT);
    }
}