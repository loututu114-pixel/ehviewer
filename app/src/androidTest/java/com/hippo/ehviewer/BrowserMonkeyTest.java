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

package com.hippo.ehviewer;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.hippo.ehviewer.ui.WebViewActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 浏览器Monkey测试
 * 模拟随机用户操作，测试应用稳定性
 */
@RunWith(AndroidJUnit4.class)
public class BrowserMonkeyTest {

    private static final String TAG = "BrowserMonkeyTest";

    private UiDevice mDevice;

    @Rule
    public ActivityTestRule<WebViewActivity> mActivityRule =
            new ActivityTestRule<>(WebViewActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // 启动应用
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation()
                .getTargetContext(), WebViewActivity.class);
        mActivityRule.launchActivity(intent);

        // 等待应用启动完成
        Thread.sleep(3000);
    }

    @After
    public void tearDown() {
        if (mActivityRule.getActivity() != null) {
            mActivityRule.getActivity().finish();
        }
    }

    /**
     * 轻量级Monkey测试
     * 执行100个随机操作
     */
    @Test
    public void testLightMonkeyTest() throws Exception {
        final int EVENT_COUNT = 100;
        final int DELAY_BETWEEN_EVENTS = 500; // 500ms

        android.util.Log.d(TAG, "Starting light monkey test with " + EVENT_COUNT + " events");

        long startTime = System.currentTimeMillis();
        int crashCount = 0;

        for (int i = 0; i < EVENT_COUNT; i++) {
            try {
                performRandomAction();
                Thread.sleep(DELAY_BETWEEN_EVENTS);

                // 检查应用是否还在运行
                if (!isApplicationRunning()) {
                    crashCount++;
                    android.util.Log.w(TAG, "Application crashed at event " + i);

                    // 重新启动应用
                    restartApplication();
                    Thread.sleep(2000);
                }

            } catch (Exception e) {
                android.util.Log.w(TAG, "Exception at event " + i + ": " + e.getMessage());
                crashCount++;
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        android.util.Log.d(TAG, "Light monkey test completed:");
        android.util.Log.d(TAG, "  Total events: " + EVENT_COUNT);
        android.util.Log.d(TAG, "  Total time: " + totalTime + "ms");
        android.util.Log.d(TAG, "  Average time per event: " + (totalTime / EVENT_COUNT) + "ms");
        android.util.Log.d(TAG, "  Crash count: " + crashCount);

        // 崩溃率应该低于5%
        double crashRate = (double) crashCount / EVENT_COUNT * 100;
        assertTrue("Crash rate too high: " + crashRate + "%", crashRate < 5.0);
    }

    /**
     * 网页浏览Monkey测试
     */
    @Test
    public void testBrowsingMonkeyTest() throws Exception {
        final int BROWSE_COUNT = 20;
        final String[] testUrls = {
            "https://www.baidu.com",
            "https://www.sina.com.cn",
            "https://www.sohu.com",
            "https://www.qq.com",
            "https://www.163.com",
            "https://www.taobao.com",
            "https://www.jd.com"
        };

        android.util.Log.d(TAG, "Starting browsing monkey test with " + BROWSE_COUNT + " pages");

        for (int i = 0; i < BROWSE_COUNT; i++) {
            String url = testUrls[i % testUrls.length];
            android.util.Log.d(TAG, "Loading page " + (i + 1) + ": " + url);

            try {
                // 加载页面
                loadUrl(url);
                Thread.sleep(3000); // 等待页面加载

                // 执行随机浏览操作
                performBrowsingActions();

                // 验证页面是否正常加载
                assertPageLoaded();

            } catch (Exception e) {
                android.util.Log.w(TAG, "Failed to load page " + url + ": " + e.getMessage());
                // 继续下一个页面
            }
        }

        android.util.Log.d(TAG, "Browsing monkey test completed successfully");
    }

    /**
     * 标签页操作Monkey测试
     */
    @Test
    public void testTabMonkeyTest() throws Exception {
        final int TAB_OPERATIONS = 50;

        android.util.Log.d(TAG, "Starting tab monkey test with " + TAB_OPERATIONS + " operations");

        for (int i = 0; i < TAB_OPERATIONS; i++) {
            try {
                int operation = (int) (Math.random() * 4);

                switch (operation) {
                    case 0:
                        // 创建新标签页
                        createNewTab();
                        android.util.Log.d(TAG, "Created new tab");
                        break;
                    case 1:
                        // 切换标签页
                        switchToRandomTab();
                        android.util.Log.d(TAG, "Switched tab");
                        break;
                    case 2:
                        // 关闭标签页
                        closeRandomTab();
                        android.util.Log.d(TAG, "Closed tab");
                        break;
                    case 3:
                        // 刷新页面
                        refreshPage();
                        android.util.Log.d(TAG, "Refreshed page");
                        break;
                }

                Thread.sleep(1000);

                // 验证应用状态
                assertApplicationStable();

            } catch (Exception e) {
                android.util.Log.w(TAG, "Tab operation failed: " + e.getMessage());
            }
        }

        android.util.Log.d(TAG, "Tab monkey test completed");
    }

    /**
     * 搜索功能Monkey测试
     */
    @Test
    public void testSearchMonkeyTest() throws Exception {
        final int SEARCH_COUNT = 15;
        final String[] searchTerms = {
            "新闻", "天气", "地图", "视频", "音乐",
            "购物", "游戏", "电影", "小说", "科技",
            "体育", "财经", "娱乐", "教育", "医疗"
        };

        android.util.Log.d(TAG, "Starting search monkey test with " + SEARCH_COUNT + " searches");

        for (int i = 0; i < SEARCH_COUNT; i++) {
            String searchTerm = searchTerms[i % searchTerms.length];
            android.util.Log.d(TAG, "Searching for: " + searchTerm);

            try {
                // 执行搜索
                performSearch(searchTerm);
                Thread.sleep(3000); // 等待搜索结果

                // 验证搜索结果
                assertSearchResultsPresent();

                // 随机点击搜索结果
                clickRandomSearchResult();

            } catch (Exception e) {
                android.util.Log.w(TAG, "Search failed for term: " + searchTerm + ", " + e.getMessage());
            }
        }

        android.util.Log.d(TAG, "Search monkey test completed");
    }

    /**
     * 内存压力Monkey测试
     */
    @Test
    public void testMemoryPressureMonkeyTest() throws Exception {
        final int MEMORY_TEST_DURATION = 30000; // 30秒
        final int EVENT_INTERVAL = 200; // 200ms

        android.util.Log.d(TAG, "Starting memory pressure monkey test for " + MEMORY_TEST_DURATION + "ms");

        long startTime = System.currentTimeMillis();
        int eventCount = 0;
        long initialMemory = getCurrentMemoryUsage();

        while (System.currentTimeMillis() - startTime < MEMORY_TEST_DURATION) {
            try {
                performRandomAction();
                eventCount++;

                Thread.sleep(EVENT_INTERVAL);

                // 每10个事件检查一次内存
                if (eventCount % 10 == 0) {
                    long currentMemory = getCurrentMemoryUsage();
                    long memoryIncrease = currentMemory - initialMemory;

                    android.util.Log.d(TAG, "Memory usage after " + eventCount + " events: " +
                            currentMemory / 1024 / 1024 + "MB (+" + memoryIncrease / 1024 / 1024 + "MB)");

                    // 如果内存增长过大，记录警告
                    if (memoryIncrease > 50 * 1024 * 1024) { // 50MB
                        android.util.Log.w(TAG, "High memory usage detected: " + memoryIncrease / 1024 / 1024 + "MB");
                    }
                }

            } catch (Exception e) {
                android.util.Log.w(TAG, "Memory pressure test exception: " + e.getMessage());
            }
        }

        long finalMemory = getCurrentMemoryUsage();
        long totalMemoryIncrease = finalMemory - initialMemory;

        android.util.Log.d(TAG, "Memory pressure test completed:");
        android.util.Log.d(TAG, "  Total events: " + eventCount);
        android.util.Log.d(TAG, "  Total memory increase: " + totalMemoryIncrease / 1024 / 1024 + "MB");

        // 内存增长应该在合理范围内
        assertTrue("Memory increase too high: " + totalMemoryIncrease / 1024 / 1024 + "MB",
                totalMemoryIncrease < 100 * 1024 * 1024); // 100MB
    }

    // ==================== 辅助方法 ====================

    private void performRandomAction() throws UiObjectNotFoundException {
        int action = (int) (Math.random() * 10);

        switch (action) {
            case 0:
                // 点击随机位置
                clickRandomPosition();
                break;
            case 1:
                // 滚动页面
                scrollRandomDirection();
                break;
            case 2:
                // 打开菜单
                openMenu();
                break;
            case 3:
                // 点击后退
                pressBack();
                break;
            case 4:
                // 点击前进
                pressForward();
                break;
            case 5:
                // 刷新页面
                refreshPage();
                break;
            case 6:
                // 缩放操作
                performZoom();
                break;
            case 7:
                // 创建新标签页
                createNewTab();
                break;
            case 8:
                // 切换标签页
                switchToRandomTab();
                break;
            case 9:
                // 执行搜索
                performRandomSearch();
                break;
        }
    }

    private void performBrowsingActions() throws UiObjectNotFoundException {
        // 随机执行浏览操作
        int actionCount = (int) (Math.random() * 5) + 1;

        for (int i = 0; i < actionCount; i++) {
            int action = (int) (Math.random() * 4);

            switch (action) {
                case 0:
                    scrollRandomDirection();
                    break;
                case 1:
                    clickRandomPosition();
                    break;
                case 2:
                    performZoom();
                    break;
                case 3:
                    // 等待一下
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    break;
            }

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void loadUrl(String url) throws UiObjectNotFoundException {
        UiObject addressBar = mDevice.findObject(new UiSelector()
                .className("android.widget.EditText")
                .description("Address bar"));

        if (addressBar.exists()) {
            addressBar.setText(url);
            mDevice.pressEnter();
        }
    }

    private void createNewTab() throws UiObjectNotFoundException {
        UiObject newTabButton = mDevice.findObject(new UiSelector()
                .description("New tab"));

        if (newTabButton.exists()) {
            newTabButton.click();
        } else {
            // 尝试通过菜单创建
            openMenu();
            UiObject newTabMenuItem = mDevice.findObject(new UiSelector()
                    .text("新建标签页"));
            if (newTabMenuItem.exists()) {
                newTabMenuItem.click();
            }
        }
    }

    private void switchToRandomTab() throws UiObjectNotFoundException {
        UiObject tabContainer = mDevice.findObject(new UiSelector()
                .resourceId("com.hippo.ehviewer:id/tab_container"));

        if (tabContainer.exists() && tabContainer.getChildCount() > 1) {
            int tabCount = tabContainer.getChildCount();
            int randomTab = (int) (Math.random() * tabCount);

            UiObject tab = tabContainer.getChild(new UiSelector().index(randomTab));
            if (tab.exists()) {
                tab.click();
            }
        }
    }

    private void closeRandomTab() throws UiObjectNotFoundException {
        UiObject tabContainer = mDevice.findObject(new UiSelector()
                .resourceId("com.hippo.ehviewer:id/tab_container"));

        if (tabContainer.exists() && tabContainer.getChildCount() > 1) {
            int tabCount = tabContainer.getChildCount();
            int randomTab = (int) (Math.random() * tabCount);

            UiObject tab = tabContainer.getChild(new UiSelector().index(randomTab));
            if (tab.exists()) {
                // 长按关闭
                tab.longClick();

                UiObject closeButton = mDevice.findObject(new UiSelector()
                        .text("关闭"));
                if (closeButton.exists()) {
                    closeButton.click();
                }
            }
        }
    }

    private void performSearch(String query) throws UiObjectNotFoundException {
        UiObject searchBox = mDevice.findObject(new UiSelector()
                .className("android.widget.EditText")
                .textContains("搜索"));

        if (searchBox.exists()) {
            searchBox.setText(query);
            mDevice.pressEnter();
        }
    }

    private void performRandomSearch() throws UiObjectNotFoundException {
        String[] randomTerms = {"test", "news", "weather", "map", "video"};
        String randomTerm = randomTerms[(int) (Math.random() * randomTerms.length)];
        performSearch(randomTerm);
    }

    private void clickRandomPosition() {
        int screenWidth = mDevice.getDisplayWidth();
        int screenHeight = mDevice.getDisplayHeight();

        int randomX = (int) (Math.random() * screenWidth);
        int randomY = (int) (Math.random() * screenHeight);

        mDevice.click(randomX, randomY);
    }

    private void scrollRandomDirection() {
        int direction = (int) (Math.random() * 4);

        switch (direction) {
            case 0: // 上滑
                mDevice.swipe(500, 1000, 500, 500, 10);
                break;
            case 1: // 下滑
                mDevice.swipe(500, 500, 500, 1000, 10);
                break;
            case 2: // 左滑
                mDevice.swipe(800, 500, 200, 500, 10);
                break;
            case 3: // 右滑
                mDevice.swipe(200, 500, 800, 500, 10);
                break;
        }
    }

    private void openMenu() throws UiObjectNotFoundException {
        UiObject menuButton = mDevice.findObject(new UiSelector()
                .description("More options"));

        if (menuButton.exists()) {
            menuButton.click();
        } else {
            mDevice.pressMenu();
        }
    }

    private void pressBack() {
        mDevice.pressBack();
    }

    private void pressForward() throws UiObjectNotFoundException {
        UiObject forwardButton = mDevice.findObject(new UiSelector()
                .description("Navigate forward"));

        if (forwardButton.exists()) {
            forwardButton.click();
        }
    }

    private void refreshPage() throws UiObjectNotFoundException {
        UiObject refreshButton = mDevice.findObject(new UiSelector()
                .description("Refresh"));

        if (refreshButton.exists()) {
            refreshButton.click();
        } else {
            openMenu();
            UiObject refreshMenuItem = mDevice.findObject(new UiSelector()
                    .text("刷新"));
            if (refreshMenuItem.exists()) {
                refreshMenuItem.click();
            }
        }
    }

    private void performZoom() {
        // 模拟双指缩放
        int centerX = mDevice.getDisplayWidth() / 2;
        int centerY = mDevice.getDisplayHeight() / 2;

        // 放大
        mDevice.pinchIn(centerX, centerY, 100);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 缩小
        mDevice.pinchOut(centerX, centerY, 100);
    }

    private void clickRandomSearchResult() throws UiObjectNotFoundException {
        // 随机点击搜索结果
        UiObject resultList = mDevice.findObject(new UiSelector()
                .className("android.webkit.WebView"));

        if (resultList.exists()) {
            int resultCount = 5; // 假设有5个结果
            int randomResult = (int) (Math.random() * resultCount);

            // 计算点击位置（大致估算）
            int resultY = 300 + (randomResult * 100); // 从300px开始，每100px一个结果
            mDevice.click(mDevice.getDisplayWidth() / 2, resultY);
        }
    }

    // ==================== 断言方法 ====================

    private void assertPageLoaded() throws UiObjectNotFoundException {
        UiObject webView = mDevice.findObject(new UiSelector()
                .className("android.webkit.WebView"));

        assertTrue("WebView not found", webView.exists());
        assertTrue("WebView not visible", webView.isDisplayed());
    }

    private void assertSearchResultsPresent() throws UiObjectNotFoundException {
        UiObject webView = mDevice.findObject(new UiSelector()
                .className("android.webkit.WebView"));

        assertTrue("Search results WebView not found", webView.exists());
    }

    private void assertApplicationStable() {
        assertNotNull("Activity is null", mActivityRule.getActivity());
        assertTrue("Activity not running", !mActivityRule.getActivity().isFinishing());
    }

    private boolean isApplicationRunning() {
        return mActivityRule.getActivity() != null && !mActivityRule.getActivity().isFinishing();
    }

    private void restartApplication() {
        // 重新启动应用
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation()
                .getTargetContext(), WebViewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        InstrumentationRegistry.getInstrumentation().getTargetContext().startActivity(intent);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
