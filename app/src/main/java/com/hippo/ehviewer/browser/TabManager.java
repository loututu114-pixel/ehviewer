package com.hippo.ehviewer.browser;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import com.hippo.ehviewer.performance.WebViewMemoryManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 多标签页管理器
 * 提供现代浏览器的多标签页功能，集成智能内存管理
 * 
 * 核心特性：
 * 1. 多标签页创建、切换、关闭管理
 * 2. 标签页状态保存和恢复
 * 3. 后台标签页资源优化
 * 4. 标签页历史和书签集成
 */
public class TabManager {
    private static final String TAG = "TabManager";
    
    // 标签页配置
    private static final int MAX_TABS = 20; // 增加标签页限制到20个
    private static final int DEFAULT_TAB_INDEX = 0;
    
    // 标签页列表和当前索引
    private final List<BrowserTab> mTabs = new ArrayList<>();
    private int mCurrentTabIndex = -1;
    
    // 依赖组件
    private final Context mContext;
    private final WebViewMemoryManager mMemoryManager;
    private TabManagerListener mListener;
    
    /**
     * 浏览器标签页
     */
    public static class BrowserTab {
        public String id;
        public String title;
        public String url;
        public WebView webView;
        public boolean isLoading;
        public long createdTime;
        public long lastAccessTime;
        
        public BrowserTab(String title, String url) {
            this.id = "tab_" + System.currentTimeMillis();
            this.title = title != null ? title : "新标签页";
            this.url = url != null ? url : "about:blank";
            this.isLoading = false;
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = this.createdTime;
        }
        
        public BrowserTab(String title, String url, WebView webView) {
            this(title, url);
            this.webView = webView;
        }
        
        public void updateAccess() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public boolean isValid() {
            return webView != null;
        }
        
        @Override
        public String toString() {
            return String.format("BrowserTab{id='%s', title='%s', url='%s'}", 
                               id, title, url);
        }
    }
    
    /**
     * 标签页管理监听器
     */
    public interface TabManagerListener {
        void onTabCreated(BrowserTab tab, int index);
        void onTabRemoved(BrowserTab tab, int index);
        void onTabSwitched(BrowserTab fromTab, BrowserTab toTab, int newIndex);
        void onTabUpdated(BrowserTab tab, int index);
        void onAllTabsClosed();
    }
    
    public TabManager(Context context) {
        mContext = context;
        mMemoryManager = WebViewMemoryManager.getInstance(context);
        Log.d(TAG, "TabManager initialized");
    }
    
    /**
     * 设置标签页管理监听器
     */
    public void setTabManagerListener(TabManagerListener listener) {
        mListener = listener;
    }
    
    /**
     * 创建新标签页
     */
    public BrowserTab createTab(String title, String url) {
        return createTab(title, url, true);
    }
    
    /**
     * 使用现有WebView创建标签页（用于初始化）
     */
    public BrowserTab createTabWithExistingWebView(String title, String url, WebView existingWebView) {
        // 检查标签页数量限制
        if (mTabs.size() >= MAX_TABS) {
            Log.w(TAG, "Maximum tabs reached (" + MAX_TABS + "), cannot create new tab");
            return null;
        }
        
        if (existingWebView == null) {
            Log.w(TAG, "Cannot create tab with null WebView");
            return null;
        }
        
        Log.d(TAG, "Creating tab with existing WebView: " + title + " -> " + url);
        
        // 创建标签页对象（使用现有构造函数）
        BrowserTab tab = new BrowserTab(title, url, existingWebView);
        
        // 添加到标签页列表
        mTabs.add(tab);
        mCurrentTabIndex = mTabs.size() - 1;
        
        Log.d(TAG, "Tab created successfully with existing WebView: " + tab.id + 
              " (Total tabs: " + mTabs.size() + ")");
        
        // 通知监听器
        if (mListener != null) {
            mListener.onTabCreated(tab, mCurrentTabIndex);
        }
        
        return tab;
    }
    
    public BrowserTab createTab(String title, String url, boolean switchToTab) {
        // 检查标签页数量限制
        if (mTabs.size() >= MAX_TABS) {
            Log.w(TAG, "Maximum tabs reached (" + MAX_TABS + "), cannot create new tab");
            return null;
        }
        
        Log.d(TAG, "Creating new tab: " + title + " -> " + url);
        
        // 使用X5WebViewManager创建WebView实例（优先X5内核）
        WebView webView = null;
        try {
            com.hippo.ehviewer.client.X5WebViewManager x5Manager = 
                com.hippo.ehviewer.client.X5WebViewManager.getInstance();
            Object webViewObject = x5Manager.createWebView(mContext);
            
            if (webViewObject instanceof android.webkit.WebView) {
                webView = (android.webkit.WebView) webViewObject;
                Log.d(TAG, "Created system WebView for tab");
            } else if (webViewObject instanceof com.tencent.smtt.sdk.WebView) {
                // X5 WebView需要适配，暂时回退到系统WebView
                Log.i(TAG, "X5 WebView created but using system WebView fallback for compatibility");
                webView = new android.webkit.WebView(mContext);
                
                // 配置系统WebView以模拟X5的设置
                android.webkit.WebSettings settings = webView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setUseWideViewPort(true);
                settings.setLoadWithOverviewMode(true);
                settings.setSupportZoom(true);
                settings.setBuiltInZoomControls(true);
                settings.setDisplayZoomControls(false);
            } else {
                Log.w(TAG, "Unknown WebView type, using system WebView");
                webView = new android.webkit.WebView(mContext);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create WebView with X5WebViewManager, using system WebView", e);
            webView = new android.webkit.WebView(mContext);
        }
        
        if (webView == null) {
            Log.e(TAG, "Failed to create WebView for new tab");
            return null;
        }
        
        // 创建标签页
        BrowserTab tab = new BrowserTab(title, url, webView);
        mTabs.add(tab);
        
        int tabIndex = mTabs.size() - 1;
        
        // 通知监听器
        if (mListener != null) {
            mListener.onTabCreated(tab, tabIndex);
        }
        
        // 切换到新标签页
        if (switchToTab) {
            switchToTab(tabIndex);
        }
        
        Log.d(TAG, "Tab created successfully: " + tab + " at index " + tabIndex);
        return tab;
    }
    
    /**
     * 关闭标签页
     */
    public boolean closeTab(int index) {
        if (index < 0 || index >= mTabs.size()) {
            Log.w(TAG, "Invalid tab index for close: " + index);
            return false;
        }
        
        BrowserTab tab = mTabs.get(index);
        Log.d(TAG, "Closing tab at index " + index + ": " + tab);
        
        // 回收WebView
        if (tab.webView != null) {
            mMemoryManager.recycleWebView(tab.webView);
            tab.webView = null;
        }
        
        // 从列表移除
        mTabs.remove(index);
        
        // 调整当前标签页索引
        if (mCurrentTabIndex == index) {
            // 当前标签页被关闭
            if (mTabs.isEmpty()) {
                mCurrentTabIndex = -1;
            } else if (mCurrentTabIndex >= mTabs.size()) {
                mCurrentTabIndex = mTabs.size() - 1;
            }
            // 如果不是最后一个标签页，索引保持不变，会自动切换到下一个标签页
        } else if (mCurrentTabIndex > index) {
            // 关闭的标签页在当前标签页之前，需要调整索引
            mCurrentTabIndex--;
        }
        
        // 通知监听器
        if (mListener != null) {
            mListener.onTabRemoved(tab, index);
            
            if (mTabs.isEmpty()) {
                mListener.onAllTabsClosed();
            }
        }
        
        Log.d(TAG, "Tab closed. Remaining tabs: " + mTabs.size() + 
              ", Current index: " + mCurrentTabIndex);
        
        return true;
    }
    
    /**
     * 关闭当前标签页
     */
    public boolean closeCurrentTab() {
        if (mCurrentTabIndex >= 0) {
            return closeTab(mCurrentTabIndex);
        }
        return false;
    }
    
    /**
     * 关闭所有标签页
     */
    public void closeAllTabs() {
        Log.d(TAG, "Closing all tabs (" + mTabs.size() + ")");
        
        // 从后向前关闭，避免索引问题
        for (int i = mTabs.size() - 1; i >= 0; i--) {
            closeTab(i);
        }
        
        Log.d(TAG, "All tabs closed");
    }
    
    /**
     * 切换到指定标签页
     */
    public boolean switchToTab(int index) {
        if (index < 0 || index >= mTabs.size()) {
            Log.w(TAG, "Invalid tab index for switch: " + index);
            return false;
        }
        
        if (index == mCurrentTabIndex) {
            Log.d(TAG, "Already on tab " + index);
            return true;
        }
        
        BrowserTab fromTab = getCurrentTab();
        BrowserTab toTab = mTabs.get(index);
        
        Log.d(TAG, "Switching from tab " + mCurrentTabIndex + " to tab " + index);
        
        // 暂停当前标签页的WebView
        if (fromTab != null && fromTab.webView != null) {
            mMemoryManager.pauseBackgroundWebViews(toTab.webView);
        }
        
        // 恢复目标标签页的WebView
        if (toTab.webView != null) {
            mMemoryManager.resumeWebView(toTab.webView);
            toTab.updateAccess();
        }
        
        // 更新当前索引
        int oldIndex = mCurrentTabIndex;
        mCurrentTabIndex = index;
        
        // 通知监听器
        if (mListener != null) {
            mListener.onTabSwitched(fromTab, toTab, index);
        }
        
        Log.d(TAG, "Successfully switched to tab " + index + ": " + toTab);
        return true;
    }
    
    /**
     * 切换到下一个标签页
     */
    public boolean switchToNextTab() {
        if (mTabs.isEmpty()) return false;
        
        int nextIndex = (mCurrentTabIndex + 1) % mTabs.size();
        return switchToTab(nextIndex);
    }
    
    /**
     * 切换到上一个标签页
     */
    public boolean switchToPreviousTab() {
        if (mTabs.isEmpty()) return false;
        
        int prevIndex = mCurrentTabIndex - 1;
        if (prevIndex < 0) {
            prevIndex = mTabs.size() - 1;
        }
        return switchToTab(prevIndex);
    }
    
    /**
     * 获取当前标签页
     */
    public BrowserTab getCurrentTab() {
        if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabs.size()) {
            return mTabs.get(mCurrentTabIndex);
        }
        return null;
    }
    
    /**
     * 获取指定索引的标签页
     */
    public BrowserTab getTab(int index) {
        if (index >= 0 && index < mTabs.size()) {
            return mTabs.get(index);
        }
        return null;
    }
    
    /**
     * 更新标签页信息
     */
    public void updateTabInfo(int index, String title, String url) {
        BrowserTab tab = getTab(index);
        if (tab != null) {
            boolean changed = false;
            
            if (title != null && !title.equals(tab.title)) {
                tab.title = title;
                changed = true;
            }
            
            if (url != null && !url.equals(tab.url)) {
                tab.url = url;
                changed = true;
            }
            
            if (changed) {
                tab.updateAccess();
                
                if (mListener != null) {
                    mListener.onTabUpdated(tab, index);
                }
                
                Log.d(TAG, "Tab updated: " + tab);
            }
        }
    }
    
    /**
     * 更新当前标签页信息
     */
    public void updateCurrentTabInfo(String title, String url) {
        if (mCurrentTabIndex >= 0) {
            updateTabInfo(mCurrentTabIndex, title, url);
        }
    }
    
    /**
     * 设置标签页加载状态
     */
    public void setTabLoading(int index, boolean isLoading) {
        BrowserTab tab = getTab(index);
        if (tab != null && tab.isLoading != isLoading) {
            tab.isLoading = isLoading;
            tab.updateAccess();
            
            if (mListener != null) {
                mListener.onTabUpdated(tab, index);
            }
        }
    }
    
    /**
     * 获取标签页数量
     */
    public int getTabCount() {
        return mTabs.size();
    }
    
    /**
     * 获取当前标签页索引
     */
    public int getCurrentTabIndex() {
        return mCurrentTabIndex;
    }
    
    /**
     * 获取所有标签页
     */
    public List<BrowserTab> getAllTabs() {
        return new ArrayList<>(mTabs);
    }
    
    /**
     * 查找标签页索引
     */
    public int findTabIndex(BrowserTab tab) {
        return mTabs.indexOf(tab);
    }
    
    public int findTabIndexByUrl(String url) {
        for (int i = 0; i < mTabs.size(); i++) {
            if (mTabs.get(i).url.equals(url)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 检查是否有标签页
     */
    public boolean hasTab() {
        return !mTabs.isEmpty();
    }
    
    /**
     * 检查是否达到最大标签页数量
     */
    public boolean isMaxTabsReached() {
        return mTabs.size() >= MAX_TABS;
    }
    
    /**
     * 获取标签页统计信息
     */
    public String getTabStats() {
        int activeCount = 0;
        int loadingCount = 0;
        
        for (BrowserTab tab : mTabs) {
            if (tab.isValid()) {
                activeCount++;
            }
            if (tab.isLoading) {
                loadingCount++;
            }
        }
        
        return String.format("Tabs: %d/%d, Active: %d, Loading: %d, Current: %d",
                           mTabs.size(), MAX_TABS, activeCount, loadingCount, mCurrentTabIndex);
    }
    
    /**
     * 清理无效标签页
     */
    public void cleanupInvalidTabs() {
        Iterator<BrowserTab> iterator = mTabs.iterator();
        int removedCount = 0;
        int index = 0;
        
        while (iterator.hasNext()) {
            BrowserTab tab = iterator.next();
            
            if (!tab.isValid()) {
                Log.d(TAG, "Removing invalid tab: " + tab);
                iterator.remove();
                removedCount++;
                
                // 调整当前标签页索引
                if (mCurrentTabIndex > index) {
                    mCurrentTabIndex--;
                } else if (mCurrentTabIndex == index) {
                    if (mTabs.isEmpty()) {
                        mCurrentTabIndex = -1;
                    } else if (mCurrentTabIndex >= mTabs.size()) {
                        mCurrentTabIndex = mTabs.size() - 1;
                    }
                }
            } else {
                index++;
            }
        }
        
        if (removedCount > 0) {
            Log.d(TAG, "Cleaned up " + removedCount + " invalid tabs");
        }
    }
    
    /**
     * 暂停所有后台标签页
     */
    public void pauseBackgroundTabs() {
        BrowserTab currentTab = getCurrentTab();
        WebView activeWebView = currentTab != null ? currentTab.webView : null;
        
        mMemoryManager.pauseBackgroundWebViews(activeWebView);
        Log.d(TAG, "Background tabs paused");
    }
    
    /**
     * 恢复当前标签页
     */
    public void resumeCurrentTab() {
        BrowserTab currentTab = getCurrentTab();
        if (currentTab != null && currentTab.webView != null) {
            mMemoryManager.resumeWebView(currentTab.webView);
            Log.d(TAG, "Current tab resumed");
        }
    }
    
    /**
     * 销毁标签页管理器
     */
    public void destroy() {
        Log.d(TAG, "Destroying TabManager");
        
        // 关闭所有标签页
        closeAllTabs();
        
        // 清理引用
        mListener = null;
        
        Log.d(TAG, "TabManager destroyed");
    }
}