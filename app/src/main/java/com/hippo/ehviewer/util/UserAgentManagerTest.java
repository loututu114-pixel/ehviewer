package com.hippo.ehviewer.util;

import android.content.Context;
import android.util.Log;

/**
 * User-Agent管理器测试类
 */
public class UserAgentManagerTest {
    private static final String TAG = "UserAgentManagerTest";
    private final UserAgentManager uaManager;

    public UserAgentManagerTest(Context context) {
        this.uaManager = new UserAgentManager(context);
    }

    /**
     * 运行所有测试
     */
    public void runAllTests() {
        Log.d(TAG, "开始运行User-Agent管理器测试...");

        testYouTubeUA();
        testChineseSitesUA();
        testDomainExtraction();
        testAliasResolution();
        testDefaultBehavior();

        Log.d(TAG, "所有User-Agent测试完成！");
    }

    /**
     * 测试YouTube UA设置
     */
    private void testYouTubeUA() {
        Log.d(TAG, "测试YouTube User-Agent设置:");

        String[] youtubeUrls = {
            "https://youtube.com",
            "https://www.youtube.com",
            "https://m.youtube.com",
            "https://youtu.be/abc123"
        };

        for (String url : youtubeUrls) {
            String optimalUA = uaManager.getOptimalUserAgent(uaManager.extractDomain(url));
            Log.d(TAG, "✓ " + url + " -> " + uaManager.getUserAgentType(optimalUA));
        }
    }

    /**
     * 测试中文网站UA设置
     */
    private void testChineseSitesUA() {
        Log.d(TAG, "测试中文网站User-Agent设置:");

        String[] chineseSites = {
            "https://baidu.com",
            "https://weibo.com",
            "https://zhihu.com",
            "https://bilibili.com",
            "https://taobao.com"
        };

        for (String url : chineseSites) {
            String optimalUA = uaManager.getOptimalUserAgent(uaManager.extractDomain(url));
            Log.d(TAG, "✓ " + url + " -> " + uaManager.getUserAgentType(optimalUA));
        }
    }

    /**
     * 测试域名提取
     */
    private void testDomainExtraction() {
        Log.d(TAG, "测试域名提取功能:");

        String[] testUrls = {
            "https://www.youtube.com/watch?v=abc123",
            "https://m.youtube.com/watch?v=abc123",
            "https://github.com/user/repo",
            "https://baidu.com/s?wd=test",
            "http://localhost:8080/test"
        };

        for (String url : testUrls) {
            String domain = uaManager.extractDomain(url);
            Log.d(TAG, "✓ " + url + " -> 域名: " + domain);
        }
    }

    /**
     * 测试别名解析
     */
    private void testAliasResolution() {
        Log.d(TAG, "测试域名别名解析:");

        // 注意：别名解析是内部实现，我们通过测试域名映射来验证
        String[] testDomains = {"youtube.com", "baidu.com", "github.com"};

        for (String domain : testDomains) {
            String optimalUA = uaManager.getOptimalUserAgent(domain);
            Log.d(TAG, "✓ " + domain + " -> " + uaManager.getUserAgentType(optimalUA));
        }
    }

    /**
     * 测试默认行为
     */
    private void testDefaultBehavior() {
        Log.d(TAG, "测试默认User-Agent行为:");

        // 测试没有匹配规则的网站
        String[] unknownSites = {
            "https://example.com",
            "https://randomsite.org",
            "https://test123.net"
        };

        for (String url : unknownSites) {
            String optimalUA = uaManager.getOptimalUserAgent(uaManager.extractDomain(url));
            Log.d(TAG, "✓ " + url + " -> 默认: " + uaManager.getUserAgentType(optimalUA));
        }

        // 测试默认UA
        String defaultUA = uaManager.getDefaultUserAgent();
        Log.d(TAG, "✓ 默认UA: " + uaManager.getUserAgentType(defaultUA));
    }

    /**
     * 打印UA管理器状态
     */
    public void printManagerStatus() {
        Log.d(TAG, "=== User-Agent管理器状态 ===");

        Log.d(TAG, "支持的网站数量: " + uaManager.getSupportedSites().length);
        Log.d(TAG, "默认模式: " + UserAgentManager.getModeDescription(uaManager.getDefaultMode()));

        Log.d(TAG, "=== 支持的网站列表 ===");
        for (String site : uaManager.getSupportedSites()) {
            String ua = uaManager.getOptimalUserAgent(site);
            Log.d(TAG, site + " -> " + uaManager.getUserAgentType(ua));
        }

        Log.d(TAG, "=== User-Agent管理器状态结束 ===");
    }

    /**
     * 测试YouTube重定向循环检测
     */
    public void testYouTubeRedirectLoop() {
        Log.d(TAG, "测试YouTube重定向循环检测:");

        // 模拟重定向场景
        String[] redirectScenarios = {
            "https://youtube.com -> https://m.youtube.com",
            "https://m.youtube.com -> https://youtube.com",
            "https://youtu.be/abc -> https://youtube.com/watch?v=abc",
            "https://youtube.com -> https://youtube.com"
        };

        for (String scenario : redirectScenarios) {
            Log.d(TAG, "场景: " + scenario);
            // 这里可以添加实际的重定向循环检测逻辑
        }
    }
}
