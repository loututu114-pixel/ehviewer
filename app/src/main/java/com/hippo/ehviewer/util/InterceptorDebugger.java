package com.hippo.ehviewer.util;

import android.content.Context;
import android.util.Log;
import com.hippo.ehviewer.client.AdBlockManager;
import com.hippo.ehviewer.client.BrowserCoreManager;
import com.hippo.ehviewer.client.SmartRequestProcessor;

/**
 * 拦截器调试工具
 * 提供全面的拦截器状态诊断和问题排查功能
 *
 * @author EhViewer Team
 * @version 1.0.0
 */
public class InterceptorDebugger {
    private static final String TAG = "InterceptorDebugger";

    private final Context context;
    private final StringBuilder debugReport;

    public InterceptorDebugger(Context context) {
        this.context = context.getApplicationContext();
        this.debugReport = new StringBuilder();
    }

    /**
     * 执行全面的拦截器诊断
     */
    public String performFullDiagnosis() {
        debugReport.setLength(0); // 清空报告
        debugReport.append("=== EhViewer 拦截器诊断报告 ===\n");
        debugReport.append("诊断时间: ").append(new java.util.Date()).append("\n\n");

        // 1. 检查AdBlockManager状态
        diagnoseAdBlockManager();

        // 2. 检查SmartRequestProcessor状态
        diagnoseSmartRequestProcessor();

        // 3. 检查BrowserCoreManager状态
        diagnoseBrowserCoreManager();

        // 4. 检查网络状态
        diagnoseNetworkStatus();

        // 5. 生成建议
        generateRecommendations();

        debugReport.append("=== 诊断完成 ===\n");

        String report = debugReport.toString();
        Log.i(TAG, "Full diagnosis completed:\n" + report);
        return report;
    }

    /**
     * 诊断AdBlockManager状态
     */
    private void diagnoseAdBlockManager() {
        debugReport.append("🔍 AdBlockManager 诊断:\n");

        try {
            AdBlockManager adBlockManager = AdBlockManager.getInstance();
            boolean isEnabled = adBlockManager.isAdBlockEnabled();

            debugReport.append("  状态: ").append(isEnabled ? "✅ 已启用" : "❌ 已禁用").append("\n");
            debugReport.append("  屏蔽域名数量: ").append(adBlockManager.getAdDomains().size()).append("\n");
            debugReport.append("  屏蔽元素数量: ").append(adBlockManager.getBlockedDomains().size()).append("\n");

            if (isEnabled) {
                debugReport.append("  ⚠️  警告: 广告拦截已启用，可能影响网站功能\n");
            } else {
                debugReport.append("  ✅ 建议: 广告拦截已禁用，有助于提高网站兼容性\n");
            }

        } catch (Exception e) {
            debugReport.append("  ❌ 错误: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "AdBlockManager diagnosis failed", e);
        }

        debugReport.append("\n");
    }

    /**
     * 诊断SmartRequestProcessor状态
     */
    private void diagnoseSmartRequestProcessor() {
        debugReport.append("🔍 SmartRequestProcessor 诊断:\n");

        try {
            BrowserCoreManager browserCoreManager = BrowserCoreManager.getInstance(context);
            SmartRequestProcessor requestProcessor = browserCoreManager.getRequestProcessor();

            if (requestProcessor != null) {
                debugReport.append("  状态: ✅ 已初始化\n");

                // 调用测试方法获取状态
                java.lang.reflect.Method testMethod = requestProcessor.getClass().getMethod("testInterceptorStatus");
                testMethod.invoke(requestProcessor);

                debugReport.append("  请求规则数量: ").append(requestProcessor.getRequestStats().size()).append("\n");
            } else {
                debugReport.append("  状态: ❌ 未初始化\n");
            }

        } catch (Exception e) {
            debugReport.append("  状态: ⚠️  诊断失败 - ").append(e.getMessage()).append("\n");
            Log.e(TAG, "SmartRequestProcessor diagnosis failed", e);
        }

        debugReport.append("\n");
    }

    /**
     * 诊断BrowserCoreManager状态
     */
    private void diagnoseBrowserCoreManager() {
        debugReport.append("🔍 BrowserCoreManager 诊断:\n");

        try {
            BrowserCoreManager browserCoreManager = BrowserCoreManager.getInstance(context);
            debugReport.append("  状态: ✅ 已初始化\n");

            // 检查请求处理器
            SmartRequestProcessor requestProcessor = browserCoreManager.getRequestProcessor();
            debugReport.append("  请求处理器: ").append(requestProcessor != null ? "✅ 正常" : "❌ 异常").append("\n");

        } catch (Exception e) {
            debugReport.append("  状态: ❌ 异常 - ").append(e.getMessage()).append("\n");
            Log.e(TAG, "BrowserCoreManager diagnosis failed", e);
        }

        debugReport.append("\n");
    }

    /**
     * 诊断网络状态
     */
    private void diagnoseNetworkStatus() {
        debugReport.append("🔍 网络状态诊断:\n");

        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm != null) {
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    debugReport.append("  网络连接: ✅ ").append(activeNetwork.getTypeName()).append("\n");
                    debugReport.append("  连接状态: ✅ 已连接\n");
                } else {
                    debugReport.append("  网络连接: ❌ 未连接\n");
                    debugReport.append("  ⚠️  警告: 网络连接问题可能导致网页无法打开\n");
                }
            } else {
                debugReport.append("  网络服务: ❌ 不可用\n");
            }

        } catch (Exception e) {
            debugReport.append("  网络诊断: ❌ 失败 - ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Network diagnosis failed", e);
        }

        debugReport.append("\n");
    }

    /**
     * 生成修复建议
     */
    private void generateRecommendations() {
        debugReport.append("💡 修复建议:\n");

        // 检查是否需要禁用广告拦截
        try {
            AdBlockManager adBlockManager = AdBlockManager.getInstance();
            if (adBlockManager.isAdBlockEnabled()) {
                debugReport.append("  1. 考虑临时禁用广告拦截来提高网站兼容性\n");
                debugReport.append("     AdBlockManager.setAdBlockEnabled(false);\n\n");
            }
        } catch (Exception e) {
            debugReport.append("  1. 检查AdBlockManager是否正常初始化\n\n");
        }

        debugReport.append("  2. 清除应用缓存和数据重新测试\n");
        debugReport.append("  3. 检查网络连接稳定性\n");
        debugReport.append("  4. 更新到最新版本的EhViewer\n");
        debugReport.append("  5. 如果问题持续，请提供此诊断报告\n\n");

        debugReport.append("🔧 快速修复命令:\n");
        debugReport.append("  adb shell pm clear com.hippo.ehviewer\n");
        debugReport.append("  # 清除应用数据后重新测试\n\n");
    }

    /**
     * 获取简化诊断结果
     */
    public String getQuickDiagnosis() {
        StringBuilder quick = new StringBuilder();
        quick.append("🚀 快速诊断:\n");

        try {
            // 检查广告拦截状态
            AdBlockManager adBlockManager = AdBlockManager.getInstance();
            quick.append("广告拦截: ").append(adBlockManager.isAdBlockEnabled() ? "启用" : "禁用").append("\n");

            // 检查网络状态
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            quick.append("网络状态: ").append(activeNetwork != null && activeNetwork.isConnected() ? "正常" : "异常").append("\n");

            quick.append("诊断完成 ✅");

        } catch (Exception e) {
            quick.append("诊断失败 ❌: ").append(e.getMessage());
        }

        return quick.toString();
    }

    /**
     * 导出诊断报告到文件
     */
    public boolean exportReportToFile(String filePath) {
        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath);
            fos.write(debugReport.toString().getBytes());
            fos.close();
            Log.i(TAG, "Diagnosis report exported to: " + filePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to export diagnosis report", e);
            return false;
        }
    }
}
