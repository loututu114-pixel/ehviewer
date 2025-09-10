package com.hippo.ehviewer.util;

import android.content.Context;
import android.util.Log;

/**
 * 智能URL处理器测试类
 */
public class SmartUrlProcessorTest {
    private static final String TAG = "SmartUrlProcessorTest";
    private final SmartUrlProcessor processor;

    public SmartUrlProcessorTest(Context context) {
        this.processor = new SmartUrlProcessor(context);
    }

    /**
     * 运行所有测试
     */
    public void runAllTests() {
        Log.d(TAG, "开始运行智能URL处理器测试...");

        testValidUrls();
        testInvalidUrls();
        testSearchQueries();
        testSpecialProtocols();
        testFilePaths();
        testChineseUser();

        Log.d(TAG, "所有测试完成！");
    }

    /**
     * 测试有效URL
     */
    private void testValidUrls() {
        Log.d(TAG, "测试有效URL处理:");

        String[] validUrls = {
            "google.com",
            "https://google.com",
            "http://google.com",
            "www.google.com",
            "https://www.google.com/search?q=test",
            "baidu.com",
            "github.com/user/repo",
            "192.168.1.1",
            "localhost:8080",
            "127.0.0.1/test"
        };

        for (String url : validUrls) {
            String result = processor.processInput(url);
            boolean isValid = processor.isValidUrl(url);
            Log.d(TAG, "✓ " + url + " -> " + result + " (有效: " + isValid + ")");
        }
    }

    /**
     * 测试无效URL（应该触发搜索）
     */
    private void testInvalidUrls() {
        Log.d(TAG, "测试无效URL处理:");

        String[] invalidUrls = {
            "hello world",
            "java tutorial",
            "android development",
            "人工智能",
            "机器学习",
            "hello",
            "test",
            "12345",
            "search me"
        };

        for (String url : invalidUrls) {
            String result = processor.processInput(url);
            boolean isValid = processor.isValidUrl(url);
            Log.d(TAG, "✓ " + url + " -> " + result + " (有效: " + isValid + ")");
        }
    }

    /**
     * 测试搜索查询
     */
    private void testSearchQueries() {
        Log.d(TAG, "测试搜索查询检测:");

        String[] queries = {
            "java tutorial",
            "android development",
            "hello world",
            "人工智能 教程",
            "machine learning",
            "test query",
            "multiple words here"
        };

        for (String query : queries) {
            boolean looksLikeSearch = processor.looksLikeSearchQuery(query);
            String result = processor.processInput(query);
            Log.d(TAG, "✓ '" + query + "' -> 搜索查询: " + looksLikeSearch + " -> " + result);
        }
    }

    /**
     * 测试特殊协议
     */
    private void testSpecialProtocols() {
        Log.d(TAG, "测试特殊协议处理:");

        String[] protocols = {
            "mailto:test@example.com",
            "tel:+1234567890",
            "sms:+1234567890",
            "geo:37.7749,-122.4194",
            "market://details?id=com.example.app",
            "intent://example.com#Intent;scheme=https;end"
        };

        for (String protocol : protocols) {
            String result = processor.processInput(protocol);
            boolean isSpecial = processor.isSpecialProtocol(protocol);
            Log.d(TAG, "✓ " + protocol + " -> " + result + " (特殊协议: " + isSpecial + ")");
        }
    }

    /**
     * 测试文件路径
     */
    private void testFilePaths() {
        Log.d(TAG, "测试文件路径处理:");

        String[] filePaths = {
            "file:///sdcard/test.html",
            "file:///storage/emulated/0/test.pdf",
            "/sdcard/test.txt",
            "C:\\test.html"
        };

        for (String path : filePaths) {
            String result = processor.processInput(path);
            boolean isFile = processor.isFilePath(path);
            Log.d(TAG, "✓ " + path + " -> " + result + " (文件: " + isFile + ")");
        }
    }

    /**
     * 测试中文用户检测
     */
    private void testChineseUser() {
        Log.d(TAG, "测试中文用户检测:");

        String defaultHome = processor.getDefaultHomePage();
        Log.d(TAG, "✓ 默认主页: " + defaultHome);

        // 测试一些中文搜索
        String[] chineseQueries = {"人工智能", "机器学习", "Java教程", "Android开发"};

        for (String query : chineseQueries) {
            String result = processor.processInput(query);
            Log.d(TAG, "✓ 中文搜索 '" + query + "' -> " + result);
        }
    }

    /**
     * 打印所有测试结果摘要
     */
    public void printTestSummary() {
        Log.d(TAG, "=== 智能URL处理器测试摘要 ===");

        // 测试各种输入类型
        String[] testInputs = {
            "google.com",           // 有效域名
            "hello world",          // 搜索查询
            "mailto:test@test.com", // 特殊协议
            "file:///test.html",    // 文件路径
            "192.168.1.1",         // IP地址
            "人工智能",             // 中文搜索
            "https://example.com",  // 完整URL
            "invalid.domain",       // 无效域名
            "",                     // 空输入
            "   ",                  // 空白输入
        };

        for (String input : testInputs) {
            String type = processor.getInputTypeDescription(input);
            String result = processor.processInput(input);
            Log.d(TAG, "输入: '" + input + "' -> 类型: " + type + " -> 结果: " + result);
        }

        Log.d(TAG, "=== 测试摘要结束 ===");
    }
}
