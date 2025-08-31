package com.hippo.ehviewer.modernbrowser;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签页管理器 - 管理浏览器的标签页
 * 支持普通模式和隐私模式的标签页管理
 */
public class TabManager {
    private static final String TAG = "TabManager";
    private static final int MAX_TABS = 99;
    private static final int MAX_INCOGNITO_TABS = 99;

    private static TabManager instance;
    private Context context;

    private List<BrowserTab> normalTabs;
    private List<BrowserTab> incognitoTabs;
    private int currentNormalIndex = -1;
    private int currentIncognitoIndex = -1;
    private boolean isIncognitoMode = false;

    /**
     * 浏览器标签页类
     */
    public static class BrowserTab {
        private WebView webView;
        private String url = "";
        private String title = "新标签页";
        private Bitmap favicon;
        private boolean isIncognito = false;
        private long createTime;
        private Bitmap preview;

        public BrowserTab() {
            this.createTime = System.currentTimeMillis();
        }

        public WebView getWebView() { return webView; }
        public void setWebView(WebView webView) { this.webView = webView; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public Bitmap getFavicon() { return favicon; }
        public void setFavicon(Bitmap favicon) { this.favicon = favicon; }

        public boolean isIncognito() { return isIncognito; }
        public void setIncognito(boolean incognito) { this.isIncognito = incognito; }

        public long getCreateTime() { return createTime; }

        public Bitmap getPreview() { return preview; }
        public void setPreview(Bitmap preview) { this.preview = preview; }
    }

    private TabManager(Context context) {
        this.context = context.getApplicationContext();
        this.normalTabs = new ArrayList<>();
        this.incognitoTabs = new ArrayList<>();
    }

    public static synchronized TabManager getInstance(Context context) {
        if (instance == null) {
            instance = new TabManager(context);
        }
        return instance;
    }

    /**
     * 创建新标签页
     */
    public BrowserTab createNewTab(boolean incognito) {
        List<BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        int maxTabs = incognito ? MAX_INCOGNITO_TABS : MAX_TABS;

        if (targetList.size() >= maxTabs) {
            Log.w(TAG, "Maximum number of tabs reached");
            return null;
        }

        BrowserTab newTab = new BrowserTab();
        newTab.setIncognito(incognito);
        targetList.add(newTab);

        if (incognito) {
            currentIncognitoIndex = targetList.size() - 1;
        } else {
            currentNormalIndex = targetList.size() - 1;
        }

        Log.d(TAG, "New tab created. Total tabs: " + targetList.size());
        return newTab;
    }

    /**
     * 关闭标签页
     */
    public boolean closeTab(int index, boolean incognito) {
        List<BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;

        if (index < 0 || index >= targetList.size()) {
            return false;
        }

        BrowserTab tab = targetList.get(index);

        // 清理WebView资源
        cleanupTabWebView(tab);

        targetList.remove(index);

        // 调整当前索引
        if (incognito) {
            adjustIncognitoIndex(index);
        } else {
            adjustNormalIndex(index);
        }

        Log.d(TAG, "Tab closed at index: " + index);
        return true;
    }

    /**
     * 关闭所有标签页
     */
    public void closeAllTabs(boolean incognito) {
        List<BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;

        // 清理所有WebView资源
        for (BrowserTab tab : targetList) {
            cleanupTabWebView(tab);
        }

        targetList.clear();

        if (incognito) {
            currentIncognitoIndex = -1;
        } else {
            currentNormalIndex = -1;
        }

        Log.d(TAG, "All " + (incognito ? "incognito" : "normal") + " tabs closed");
    }

    /**
     * 切换到指定标签页
     */
    public BrowserTab switchToTab(int index, boolean incognito) {
        List<BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;

        if (index < 0 || index >= targetList.size()) {
            return null;
        }

        if (incognito) {
            currentIncognitoIndex = index;
        } else {
            currentNormalIndex = index;
        }

        Log.d(TAG, "Switched to tab at index: " + index);
        return targetList.get(index);
    }

    /**
     * 获取当前标签页
     */
    public BrowserTab getCurrentTab() {
        List<BrowserTab> targetList = isIncognitoMode ? incognitoTabs : normalTabs;
        int currentIndex = isIncognitoMode ? currentIncognitoIndex : currentNormalIndex;

        if (currentIndex >= 0 && currentIndex < targetList.size()) {
            return targetList.get(currentIndex);
        }

        // 如果没有标签页，创建一个新的
        if (targetList.isEmpty()) {
            return createNewTab(isIncognitoMode);
        }

        return null;
    }

    /**
     * 获取所有标签页
     */
    public List<BrowserTab> getAllTabs() {
        return isIncognitoMode ? new ArrayList<>(incognitoTabs) : new ArrayList<>(normalTabs);
    }

    /**
     * 获取标签页数量
     */
    public int getTabCount() {
        return isIncognitoMode ? incognitoTabs.size() : normalTabs.size();
    }

    /**
     * 获取当前标签页索引
     */
    public int getCurrentIndex() {
        return isIncognitoMode ? currentIncognitoIndex : currentNormalIndex;
    }

    /**
     * 切换隐私模式
     */
    public void setIncognitoMode(boolean incognito) {
        this.isIncognitoMode = incognito;
        Log.d(TAG, "Incognito mode: " + incognito);
    }

    /**
     * 是否为隐私模式
     */
    public boolean isIncognitoMode() {
        return isIncognitoMode;
    }

    /**
     * 更新标签页信息
     */
    public void updateTab(int index, String title, String url, Bitmap favicon) {
        List<BrowserTab> targetList = isIncognitoMode ? incognitoTabs : normalTabs;

        if (index >= 0 && index < targetList.size()) {
            BrowserTab tab = targetList.get(index);
            if (title != null) tab.setTitle(title);
            if (url != null) tab.setUrl(url);
            if (favicon != null) tab.setFavicon(favicon);
        }
    }

    /**
     * 更新标签页预览图
     */
    public void updateTabPreview(int index, Bitmap preview) {
        List<BrowserTab> targetList = isIncognitoMode ? incognitoTabs : normalTabs;

        if (index >= 0 && index < targetList.size()) {
            targetList.get(index).setPreview(preview);
        }
    }

    /**
     * 保存标签页状态
     */
    public void saveState() {
        // TODO: 实现标签页状态持久化
        // 保存到SharedPreferences或数据库
    }

    /**
     * 恢复标签页状态
     */
    public void restoreState() {
        // TODO: 实现标签页状态恢复
        // 从SharedPreferences或数据库恢复
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        closeAllTabs(false);
        closeAllTabs(true);
    }

    /**
     * 清理标签页WebView资源
     */
    private void cleanupTabWebView(BrowserTab tab) {
        if (tab.getWebView() instanceof WebView) {
            WebView webView = (WebView) tab.getWebView();
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");
            webView.onPause();
            webView.removeAllViews();
            webView.destroyDrawingCache();
            webView.destroy();
        }
    }

    /**
     * 调整普通标签页索引
     */
    private void adjustNormalIndex(int removedIndex) {
        if (currentNormalIndex >= normalTabs.size()) {
            currentNormalIndex = normalTabs.size() - 1;
        }
        if (currentNormalIndex < 0 && !normalTabs.isEmpty()) {
            currentNormalIndex = 0;
        }
    }

    /**
     * 调整隐私标签页索引
     */
    private void adjustIncognitoIndex(int removedIndex) {
        if (currentIncognitoIndex >= incognitoTabs.size()) {
            currentIncognitoIndex = incognitoTabs.size() - 1;
        }
        if (currentIncognitoIndex < 0 && !incognitoTabs.isEmpty()) {
            currentIncognitoIndex = 0;
        }
    }

    /**
     * 获取普通标签页列表
     */
    public List<BrowserTab> getNormalTabs() {
        return new ArrayList<>(normalTabs);
    }

    /**
     * 获取隐私标签页列表
     */
    public List<BrowserTab> getIncognitoTabs() {
        return new ArrayList<>(incognitoTabs);
    }

    /**
     * 获取普通标签页数量
     */
    public int getNormalTabCount() {
        return normalTabs.size();
    }

    /**
     * 获取隐私标签页数量
     */
    public int getIncognitoTabCount() {
        return incognitoTabs.size();
    }
}
