package com.hippo.ehviewer.util;

import android.content.Context;
import android.webkit.WebView;
import android.util.Log;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.X5WebViewManager;

/**
 * WebView内核检测器
 * 自动检测和选择合适的WebView内核进行脚本注入
 */
public class WebViewKernelDetector {

    private static final String TAG = "WebViewKernelDetector";

    /**
     * WebView内核类型枚举
     */
    public enum KernelType {
        X5_TENCENT,    // 腾讯X5内核
        CHROMIUM,      // Chromium内核
        SYSTEM_WEBVIEW // 系统WebView
    }

    /**
     * 检测WebView的内核类型
     */
    public static KernelType detectKernelType(WebView webView) {
        if (webView == null) {
            return KernelType.SYSTEM_WEBVIEW;
        }

        try {
            // 检查是否为X5 WebView包装器
            Object x5Tag = webView.getTag(R.id.x5_webview_tag);
            if (x5Tag != null) {
                // 检查X5是否可用
                X5WebViewManager x5Manager = X5WebViewManager.getInstance();
                if (x5Manager.isX5Available() && x5Manager.isX5Initialized()) {
                    return KernelType.X5_TENCENT;
                }
            }

            // 通过User-Agent检测Chromium版本
            String userAgent = webView.getSettings().getUserAgentString();
            if (userAgent != null) {
                if (userAgent.contains("Chrome/") && userAgent.contains("Safari/")) {
                    // 检查是否为现代Chromium
                    if (userAgent.contains("Chrome/7") || userAgent.contains("Chrome/8") ||
                        userAgent.contains("Chrome/9") || userAgent.contains("Chrome/10")) {
                        return KernelType.CHROMIUM;
                    }
                }
            }

            return KernelType.SYSTEM_WEBVIEW;

        } catch (Exception e) {
            Log.e(TAG, "Failed to detect WebView kernel type", e);
            return KernelType.SYSTEM_WEBVIEW;
        }
    }

    /**
     * 获取内核类型的描述信息
     */
    public static String getKernelDescription(KernelType kernelType) {
        switch (kernelType) {
            case X5_TENCENT:
                X5WebViewManager x5Manager = X5WebViewManager.getInstance();
                return "腾讯X5内核 (版本: " + (x5Manager.getX5Version() != null ? x5Manager.getX5Version() : "未知") + ")";

            case CHROMIUM:
                return "Chromium内核 (现代浏览器内核)";

            case SYSTEM_WEBVIEW:
            default:
                return "系统WebView内核 (Android系统内置)";
        }
    }

    /**
     * 获取内核的脚本注入兼容性说明
     */
    public static String getInjectionCompatibility(KernelType kernelType) {
        switch (kernelType) {
            case X5_TENCENT:
                return "支持X5专用脚本注入器，兼容Tampermonkey GM API";

            case CHROMIUM:
                return "支持标准Chromium脚本注入，完全兼容Tampermonkey";

            case SYSTEM_WEBVIEW:
            default:
                return "支持基础脚本注入，部分GM API可能受限";
        }
    }

    /**
     * 检查内核是否支持油猴脚本
     */
    public static boolean isTampermonkeyCompatible(KernelType kernelType) {
        switch (kernelType) {
            case X5_TENCENT:
                X5WebViewManager x5Manager = X5WebViewManager.getInstance();
                return x5Manager.isX5Available() && x5Manager.isX5Initialized();

            case CHROMIUM:
                return true;

            case SYSTEM_WEBVIEW:
            default:
                // 系统WebView通常支持基础脚本注入
                return true;
        }
    }

    /**
     * 获取推荐的脚本注入策略
     */
    public static String getRecommendedInjectionStrategy(KernelType kernelType) {
        switch (kernelType) {
            case X5_TENCENT:
                return "使用X5专用脚本注入器，启用X5兼容模式";

            case CHROMIUM:
                return "使用标准Chromium脚本注入，启用完整GM API支持";

            case SYSTEM_WEBVIEW:
            default:
                return "使用兼容模式脚本注入，避免使用复杂的GM API";
        }
    }

    /**
     * 获取内核的性能特点
     */
    public static String getPerformanceCharacteristics(KernelType kernelType) {
        switch (kernelType) {
            case X5_TENCENT:
                return "性能优秀，内存占用低，启动速度快";

            case CHROMIUM:
                return "功能完整，性能稳定，资源占用适中";

            case SYSTEM_WEBVIEW:
            default:
                return "性能因设备而异，可能存在兼容性问题";
        }
    }

    /**
     * 检测WebView的详细状态信息
     */
    public static String getDetailedStatus(WebView webView) {
        if (webView == null) {
            return "WebView为空";
        }

        KernelType kernelType = detectKernelType(webView);
        StringBuilder status = new StringBuilder();

        status.append("内核类型: ").append(getKernelDescription(kernelType)).append("\n");
        status.append("脚本兼容性: ").append(getInjectionCompatibility(kernelType)).append("\n");
        status.append("推荐策略: ").append(getRecommendedInjectionStrategy(kernelType)).append("\n");
        status.append("性能特点: ").append(getPerformanceCharacteristics(kernelType)).append("\n");
        status.append("油猴支持: ").append(isTampermonkeyCompatible(kernelType) ? "支持" : "不支持");

        return status.toString();
    }

    /**
     * 检查WebView是否需要特殊处理
     */
    public static boolean requiresSpecialHandling(WebView webView) {
        KernelType kernelType = detectKernelType(webView);
        return kernelType == KernelType.X5_TENCENT;
    }

    /**
     * 获取内核特定的配置建议
     */
    public static String getKernelSpecificConfigAdvice(KernelType kernelType) {
        switch (kernelType) {
            case X5_TENCENT:
                return "建议启用X5硬件加速，设置合适的缓存策略";

            case CHROMIUM:
                return "建议启用现代Web API，设置合理的内存限制";

            case SYSTEM_WEBVIEW:
            default:
                return "建议检查系统WebView版本，考虑更新到最新版本";
        }
    }
}
