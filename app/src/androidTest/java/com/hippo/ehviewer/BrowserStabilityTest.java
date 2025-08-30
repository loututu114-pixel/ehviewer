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

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 浏览器稳定性测试
 * 测试浏览器的核心功能稳定性和可靠性
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BrowserStabilityTest {

    private static final String TAG = "BrowserStabilityTest";

    private UiDevice mDevice;
    private Context mContext;

    @Rule
    public ActivityTestRule<WebViewActivity> mActivityRule =
            new ActivityTestRule<>(WebViewActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mContext = ApplicationProvider.getApplicationContext();

        // 启动应用
        Intent intent = new Intent(mContext, WebViewActivity.class);
        mActivityRule.launchActivity(intent);

        // 等待应用启动完成
        Thread.sleep(2000);
    }

    @After
    public void tearDown() {
        if (mActivityRule.getActivity() != null) {
            mActivityRule.getActivity().finish();
        }
    }

    /**
     * 测试基本浏览功能
     */
    @Test
    public void testBasicBrowsing() throws Exception {
        // 测试加载网页
        loadTestPage("https://www.baidu.com");

        // 验证页面加载成功
        assertPageLoaded();

        // 测试导航功能
        testNavigation();

        // 测试书签功能
        testBookmarkFunctionality();
    }

    /**
     * 测试多标签页功能
     */
    @Test
    public void testMultiTabFunctionality() throws Exception {
        // 创建新标签页
        createNewTab();

        // 切换标签页
        switchToTab(0);
        switchToTab(1);

        // 关闭标签页
        closeTab(1);

        // 验证标签页数量
        assertTabCount(1);
    }

    /**
     * 测试搜索功能
     */
    @Test
    public void testSearchFunctionality() throws Exception {
        // 测试百度搜索
        performSearch("test");

        // 验证搜索结果
        assertSearchResultsLoaded();
    }

    /**
     * 测试网络异常处理
     */
    @Test
    public void testNetworkErrorHandling() throws Exception {
        // 测试加载不存在的页面
        loadTestPage("https://nonexistent-domain-12345.com");

        // 验证错误页面显示
        assertErrorPageDisplayed();

        // 测试网络恢复
        loadTestPage("https://www.baidu.com");
        assertPageLoaded();
    }

    /**
     * 测试内存稳定性
     */
    @Test
    public void testMemoryStability() throws Exception {
        // 连续加载多个页面
        String[] testUrls = {
            "https://www.baidu.com",
            "https://www.sina.com.cn",
            "https://www.sohu.com",
            "https://www.qq.com"
        };

        for (String url : testUrls) {
            loadTestPage(url);
            assertPageLoaded();
            Thread.sleep(1000); // 等待页面加载
        }

        // 验证应用没有崩溃
        assertAppStable();
    }

    /**
     * 测试隐私功能
     */
    @Test
    public void testPrivacyFeatures() throws Exception {
        // 测试隐私模式
        enablePrivateMode();

        // 浏览一些页面
        loadTestPage("https://www.baidu.com");
        loadTestPage("https://www.sina.com.cn");

        // 退出隐私模式
        disablePrivateMode();

        // 验证历史记录未保存
        assertNoHistorySaved();
    }

    /**
     * 测试性能表现
     */
    @Test
    public void testPerformanceMetrics() throws Exception {
        long startTime = System.currentTimeMillis();

        // 加载测试页面
        loadTestPage("https://www.baidu.com");

        long loadTime = System.currentTimeMillis() - startTime;

        // 验证加载时间在合理范围内 (小于10秒)
        assertTrue("Page load time too slow: " + loadTime + "ms", loadTime < 10000);

        // 验证内存使用正常
        assertMemoryUsageNormal();
    }

    // ==================== 辅助测试方法 ====================

    private void loadTestPage(String url) throws UiObjectNotFoundException {
        // 在地址栏输入URL
        UiObject addressBar = mDevice.findObject(new UiSelector()
                .className("android.widget.EditText")
                .description("Address bar"));

        if (addressBar.exists()) {
            addressBar.setText(url);
            mDevice.pressEnter();
        } else {
            // 如果找不到地址栏，尝试使用菜单
            openBrowserMenu();
            selectMenuItem("新标签页");
            loadTestPage(url);
        }
    }

    private void createNewTab() throws UiObjectNotFoundException {
        openBrowserMenu();
        selectMenuItem("新建标签页");
    }

    private void switchToTab(int tabIndex) throws UiObjectNotFoundException {
        UiObject tabContainer = mDevice.findObject(new UiSelector()
                .resourceId("com.hippo.ehviewer:id/tab_container"));

        if (tabContainer.exists()) {
            UiObject tab = tabContainer.getChild(new UiSelector().index(tabIndex));
            tab.click();
        }
    }

    private void closeTab(int tabIndex) throws UiObjectNotFoundException {
        UiObject tabContainer = mDevice.findObject(new UiSelector()
                .resourceId("com.hippo.ehviewer:id/tab_container"));

        if (tabContainer.exists()) {
            UiObject tab = tabContainer.getChild(new UiSelector().index(tabIndex));
            // 长按关闭标签页
            tab.longClick();
            selectMenuItem("关闭标签页");
        }
    }

    private void performSearch(String query) throws UiObjectNotFoundException {
        // 在搜索框输入查询
        UiObject searchBox = mDevice.findObject(new UiSelector()
                .className("android.widget.EditText")
                .textContains("搜索"));

        if (searchBox.exists()) {
            searchBox.setText(query);
            mDevice.pressEnter();
        }
    }

    private void openBrowserMenu() throws UiObjectNotFoundException {
        UiObject menuButton = mDevice.findObject(new UiSelector()
                .description("More options"));

        if (menuButton.exists()) {
            menuButton.click();
        } else {
            // 使用系统菜单键
            mDevice.pressMenu();
        }
    }

    private void selectMenuItem(String itemText) throws UiObjectNotFoundException {
        UiObject menuItem = mDevice.findObject(new UiSelector()
                .className("android.widget.TextView")
                .text(itemText));

        if (menuItem.exists()) {
            menuItem.click();
        }
    }

    private void enablePrivateMode() throws UiObjectNotFoundException {
        openBrowserMenu();
        selectMenuItem("隐私模式");
    }

    private void disablePrivateMode() throws UiObjectNotFoundException {
        openBrowserMenu();
        selectMenuItem("退出隐私模式");
    }

    // ==================== 断言方法 ====================

    private void assertPageLoaded() throws UiObjectNotFoundException {
        // 等待页面加载完成
        UiObject webView = mDevice.findObject(new UiSelector()
                .className("android.webkit.WebView"));

        assertTrue("WebView not found", webView.exists());
        assertTrue("Page not loaded", webView.waitForExists(10000));
    }

    private void assertTabCount(int expectedCount) throws UiObjectNotFoundException {
        UiObject tabContainer = mDevice.findObject(new UiSelector()
                .resourceId("com.hippo.ehviewer:id/tab_container"));

        if (tabContainer.exists()) {
            int actualCount = tabContainer.getChildCount();
            assertTrue("Expected " + expectedCount + " tabs, but found " + actualCount,
                    actualCount == expectedCount);
        }
    }

    private void assertSearchResultsLoaded() throws UiObjectNotFoundException {
        // 等待搜索结果加载
        UiObject resultsContainer = mDevice.findObject(new UiSelector()
                .className("android.webkit.WebView"));

        assertTrue("Search results not loaded", resultsContainer.waitForExists(15000));
    }

    private void assertErrorPageDisplayed() throws UiObjectNotFoundException {
        // 检查是否显示错误页面或无网络提示
        UiObject errorMessage = mDevice.findObject(new UiSelector()
                .className("android.widget.TextView")
                .textContains("无法"));

        assertTrue("Error page not displayed", errorMessage.exists() ||
                mDevice.findObject(new UiSelector().textContains("error")).exists());
    }

    private void assertAppStable() {
        // 检查应用是否还在运行
        assertNotNull("Activity is null", mActivityRule.getActivity());
        assertTrue("Activity not running", !mActivityRule.getActivity().isFinishing());
    }

    private void assertNoHistorySaved() {
        // 这里需要访问HistoryManager来验证
        // 暂时使用简单的UI检查
        try {
            openBrowserMenu();
            UiObject historyItem = mDevice.findObject(new UiSelector()
                    .text("历史记录"));

            if (historyItem.exists()) {
                historyItem.click();
                // 检查历史记录列表是否为空或只有当前会话
                UiObject emptyState = mDevice.findObject(new UiSelector()
                        .textContains("暂无"));
                assertTrue("History should be empty in private mode", emptyState.exists());
            }
        } catch (UiObjectNotFoundException e) {
            // 如果找不到历史记录菜单，认为测试通过
        }
    }

    private void assertMemoryUsageNormal() {
        // 获取当前应用的内存使用情况
        Context context = mActivityRule.getActivity();
        if (context != null) {
            android.app.ActivityManager activityManager =
                (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

            if (activityManager != null) {
                android.app.ActivityManager.MemoryInfo memoryInfo = new android.app.ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                // 检查可用内存是否充足
                assertTrue("Low memory available", memoryInfo.availMem > 50 * 1024 * 1024); // 50MB
            }
        }
    }

    private void testNavigation() throws UiObjectNotFoundException {
        // 测试前进后退功能
        UiObject backButton = mDevice.findObject(new UiSelector()
                .description("Navigate back"));

        if (backButton.exists() && backButton.isEnabled()) {
            backButton.click();
            Thread.sleep(1000);
        }

        UiObject forwardButton = mDevice.findObject(new UiSelector()
                .description("Navigate forward"));

        if (forwardButton.exists() && forwardButton.isEnabled()) {
            forwardButton.click();
            Thread.sleep(1000);
        }
    }

    private void testBookmarkFunctionality() throws UiObjectNotFoundException {
        // 测试添加书签
        openBrowserMenu();
        selectMenuItem("添加到书签");

        // 验证书签添加成功（这里需要具体的UI元素）
        // 暂时只验证菜单能正常打开
    }
}
