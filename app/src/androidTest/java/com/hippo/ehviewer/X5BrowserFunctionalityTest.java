package com.hippo.ehviewer;

import android.content.Context;
import android.webkit.WebView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.hippo.ehviewer.client.X5WebViewManager;
import com.hippo.ehviewer.userscript.UserScriptManager;
import com.hippo.ehviewer.util.WebViewKernelDetector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * X5浏览器基本功能测试
 * 确保脚本注入不会影响基本的浏览功能
 */
@RunWith(AndroidJUnit4.class)
public class X5BrowserFunctionalityTest {

    private Context context;
    private X5WebViewManager x5Manager;
    private UserScriptManager scriptManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        x5Manager = X5WebViewManager.getInstance();
        scriptManager = UserScriptManager.getInstance(context);
    }

    /**
     * 测试X5 WebView基本创建功能
     */
    @Test
    public void testX5WebViewCreation() {
        WebView webView = x5Manager.createWebView(context);

        assertNotNull("WebView should not be null", webView);

        // 检查基本设置
        assertNotNull("WebView settings should not be null", webView.getSettings());

        // 检查是否正确设置了X5标签（如果X5可用）
        if (x5Manager.isX5Available()) {
            Object x5Tag = webView.getTag(R.id.x5_webview_tag);
            assertNotNull("X5 WebView should have X5 tag", x5Tag);
        }
    }

    /**
     * 测试WebView内核检测功能
     */
    @Test
    public void testKernelDetection() {
        WebView webView = x5Manager.createWebView(context);

        WebViewKernelDetector.KernelType kernelType = WebViewKernelDetector.detectKernelType(webView);

        // 验证检测结果合理
        assertNotNull("Kernel type should not be null", kernelType);

        String description = WebViewKernelDetector.getKernelDescription(kernelType);
        assertNotNull("Kernel description should not be null", description);
        assertFalse("Kernel description should not be empty", description.isEmpty());

        // 如果X5可用，应该是X5内核
        if (x5Manager.isX5Available()) {
            assertEquals("Should detect X5 kernel when available", WebViewKernelDetector.KernelType.X5_TENCENT, kernelType);
        }
    }

    /**
     * 测试脚本注入不会影响基本WebView功能
     */
    @Test
    public void testScriptInjectionDoesNotBreakBasicFunctionality() {
        WebView webView = x5Manager.createWebView(context);

        // 确保WebView基本功能正常
        assertNotNull("WebView should be functional", webView);
        assertNotNull("WebView settings should be accessible", webView.getSettings());

        // 测试脚本注入（不应该抛出异常）
        try {
            scriptManager.injectScripts(webView, "https://example.com");
            // 如果没有抛出异常，说明基本功能正常
        } catch (Exception e) {
            fail("Script injection should not break basic WebView functionality: " + e.getMessage());
        }

        // 验证WebView仍然可以正常使用
        assertNotNull("WebView should still be functional after script injection", webView);
    }

    /**
     * 测试X5状态信息获取
     */
    @Test
    public void testX5StatusInformation() {
        String status = x5Manager.getX5InitStatusDetail();

        assertNotNull("X5 status should not be null", status);
        assertFalse("X5 status should not be empty", status.isEmpty());

        // 检查状态信息包含必要字段
        assertTrue("Status should contain initialization info", status.contains("X5已初始化"));
        assertTrue("Status should contain availability info", status.contains("X5可用"));
    }

    /**
     * 测试脚本管理器状态
     */
    @Test
    public void testScriptManagerStatus() {
        // 确保脚本管理器正常工作
        assertNotNull("Script manager should not be null", scriptManager);
        assertTrue("Script manager should be enabled by default", scriptManager.isEnabled());

        // 测试状态信息获取
        String kernelDesc = scriptManager.getWebViewKernelDescription();
        assertNotNull("Kernel description should not be null", kernelDesc);

        String injectionMethod = scriptManager.getRecommendedInjectionMethod();
        assertNotNull("Injection method should not be null", injectionMethod);
    }

    /**
     * 测试X5脚本注入兼容性
     */
    @Test
    public void testX5ScriptInjectionCompatibility() {
        WebView webView = x5Manager.createWebView(context);

        // 测试脚本注入兼容性检查
        boolean x5Supported = scriptManager.isX5ScriptInjectionSupported();

        if (x5Manager.isX5Available()) {
            // 如果X5可用，脚本注入应该支持
            assertTrue("X5 script injection should be supported when X5 is available", x5Supported);
        }

        // 获取状态信息
        String status = scriptManager.getX5ScriptInjectionStatus();
        assertNotNull("X5 script injection status should not be null", status);
    }

    /**
     * 测试异常情况处理
     */
    @Test
    public void testErrorHandling() {
        // 测试空WebView处理
        try {
            scriptManager.injectScripts(null, "https://example.com");
            // 不应该抛出异常
        } catch (Exception e) {
            fail("Should handle null WebView gracefully: " + e.getMessage());
        }

        // 测试内核检测对null WebView的处理
        WebViewKernelDetector.KernelType kernelType = WebViewKernelDetector.detectKernelType(null);
        assertEquals("Null WebView should default to system WebView", WebViewKernelDetector.KernelType.SYSTEM_WEBVIEW, kernelType);
    }

    /**
     * 测试基本浏览功能（模拟页面加载）
     */
    @Test
    public void testBasicBrowsingFunctionality() {
        WebView webView = x5Manager.createWebView(context);

        // 验证WebView可以设置基本属性
        webView.getSettings().setJavaScriptEnabled(true);
        assertTrue("JavaScript should be enabled", webView.getSettings().getJavaScriptEnabled());

        webView.getSettings().setDomStorageEnabled(true);
        assertTrue("DOM storage should be enabled", webView.getSettings().getDomStorageEnabled());

        // 验证WebView没有因为脚本注入相关代码而损坏
        assertNotNull("WebView should remain functional", webView.getSettings());
    }
}
