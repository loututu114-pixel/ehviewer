package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class TabManager {
    private static final String TAG = "TabManager";
    private static final int MAX_TABS = 99;
    private static final int MAX_INCOGNITO_TABS = 99;
    
    private static TabManager instance;
    private Context context;
    
    private List<TabAdapter.BrowserTab> normalTabs;
    private List<TabAdapter.BrowserTab> incognitoTabs;
    private int currentNormalIndex = -1;
    private int currentIncognitoIndex = -1;
    private boolean isIncognitoMode = false;
    
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
    
    // 创建新标签页
    public TabAdapter.BrowserTab createNewTab(boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        int maxTabs = incognito ? MAX_INCOGNITO_TABS : MAX_TABS;
        
        if (targetList.size() >= maxTabs) {
            Log.w(TAG, "Maximum number of tabs reached");
            return null;
        }
        
        TabAdapter.BrowserTab newTab = new TabAdapter.BrowserTab();
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
    
    // 关闭标签页
    public boolean closeTab(int index, boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        
        if (index < 0 || index >= targetList.size()) {
            return false;
        }
        
        TabAdapter.BrowserTab tab = targetList.get(index);
        
        // 清理WebView资源
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
        
        targetList.remove(index);
        
        // 调整当前索引
        if (incognito) {
            if (currentIncognitoIndex >= targetList.size()) {
                currentIncognitoIndex = targetList.size() - 1;
            }
        } else {
            if (currentNormalIndex >= targetList.size()) {
                currentNormalIndex = targetList.size() - 1;
            }
        }
        
        Log.d(TAG, "Tab closed at index: " + index);
        return true;
    }
    
    // 关闭所有标签页
    public void closeAllTabs(boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        
        // 清理所有WebView资源
        for (TabAdapter.BrowserTab tab : targetList) {
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
        
        targetList.clear();
        
        if (incognito) {
            currentIncognitoIndex = -1;
        } else {
            currentNormalIndex = -1;
        }
        
        Log.d(TAG, "All " + (incognito ? "incognito" : "normal") + " tabs closed");
    }
    
    // 切换到指定标签页
    public TabAdapter.BrowserTab switchToTab(int index, boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        
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
    
    // 获取当前标签页
    public TabAdapter.BrowserTab getCurrentTab() {
        List<TabAdapter.BrowserTab> targetList = isIncognitoMode ? incognitoTabs : normalTabs;
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
    
    // 获取所有标签页
    public List<TabAdapter.BrowserTab> getAllTabs() {
        return isIncognitoMode ? new ArrayList<>(incognitoTabs) : new ArrayList<>(normalTabs);
    }
    
    // 获取标签页数量
    public int getTabCount() {
        return isIncognitoMode ? incognitoTabs.size() : normalTabs.size();
    }
    
    // 获取当前标签页索引
    public int getCurrentIndex() {
        return isIncognitoMode ? currentIncognitoIndex : currentNormalIndex;
    }
    
    // 切换隐私模式
    public void setIncognitoMode(boolean incognito) {
        this.isIncognitoMode = incognito;
        Log.d(TAG, "Incognito mode: " + incognito);
    }
    
    // 是否为隐私模式
    public boolean isIncognitoMode() {
        return isIncognitoMode;
    }
    
    // 更新标签页信息
    public void updateTab(int index, String title, String url, Bitmap favicon) {
        List<TabAdapter.BrowserTab> targetList = isIncognitoMode ? incognitoTabs : normalTabs;
        
        if (index >= 0 && index < targetList.size()) {
            TabAdapter.BrowserTab tab = targetList.get(index);
            if (title != null) tab.setTitle(title);
            if (url != null) tab.setUrl(url);
            if (favicon != null) tab.setFavicon(favicon);
        }
    }
    
    // 更新标签页预览图
    public void updateTabPreview(int index, Bitmap preview) {
        List<TabAdapter.BrowserTab> targetList = isIncognitoMode ? incognitoTabs : normalTabs;
        
        if (index >= 0 && index < targetList.size()) {
            targetList.get(index).setPreview(preview);
        }
    }
    
    // 保存标签页状态
    public void saveState() {
        // TODO: 实现标签页状态持久化
        // 保存到SharedPreferences或数据库
    }
    
    // 恢复标签页状态
    public void restoreState() {
        // TODO: 实现标签页状态恢复
        // 从SharedPreferences或数据库恢复
    }
    
    // 根据分组获取标签页
    public List<TabAdapter.BrowserTab> getTabsByGroup(String group, boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        List<TabAdapter.BrowserTab> result = new ArrayList<>();
        
        for (TabAdapter.BrowserTab tab : targetList) {
            if ((group == null && (tab.getGroup() == null || tab.getGroup().isEmpty())) ||
                (group != null && group.equals(tab.getGroup()))) {
                result.add(tab);
            }
        }
        return result;
    }
    
    // 根据分类获取标签页
    public List<TabAdapter.BrowserTab> getTabsByCategory(String category, boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        List<TabAdapter.BrowserTab> result = new ArrayList<>();
        
        for (TabAdapter.BrowserTab tab : targetList) {
            String tabCategory = tab.getCategory();
            if (tabCategory == null) {
                tabCategory = tab.getSmartCategory();
            }
            if (category.equals(tabCategory)) {
                result.add(tab);
            }
        }
        return result;
    }
    
    // 获取所有分组
    public List<String> getAllGroups(boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        List<String> groups = new ArrayList<>();
        
        for (TabAdapter.BrowserTab tab : targetList) {
            String group = tab.getGroup();
            if (group != null && !group.isEmpty() && !groups.contains(group)) {
                groups.add(group);
            }
        }
        return groups;
    }
    
    // 获取所有分类及其数量
    public Map<String, Integer> getCategoryCounts(boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        Map<String, Integer> categoryMap = new HashMap<>();
        
        for (TabAdapter.BrowserTab tab : targetList) {
            String category = tab.getCategory();
            if (category == null) {
                category = tab.getSmartCategory();
            }
            categoryMap.put(category, categoryMap.getOrDefault(category, 0) + 1);
        }
        return categoryMap;
    }
    
    // 批量设置分组
    public void setTabsGroup(List<Integer> tabIndices, String group, boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        
        for (int index : tabIndices) {
            if (index >= 0 && index < targetList.size()) {
                targetList.get(index).setGroup(group);
            }
        }
        Log.d(TAG, "Set group '" + group + "' for " + tabIndices.size() + " tabs");
    }
    
    // 自动分组相似标签页
    public void autoGroupSimilarTabs(boolean incognito) {
        List<TabAdapter.BrowserTab> targetList = incognito ? incognitoTabs : normalTabs;
        Map<String, List<TabAdapter.BrowserTab>> domainMap = new HashMap<>();
        
        // 按域名分组
        for (TabAdapter.BrowserTab tab : targetList) {
            String domain = tab.getDomain();
            if (!domain.isEmpty()) {
                domainMap.computeIfAbsent(domain, k -> new ArrayList<>()).add(tab);
            }
        }
        
        // 为有多个标签页的域名创建分组
        for (Map.Entry<String, List<TabAdapter.BrowserTab>> entry : domainMap.entrySet()) {
            List<TabAdapter.BrowserTab> tabs = entry.getValue();
            if (tabs.size() >= 2) {
                String groupName = entry.getKey();
                for (TabAdapter.BrowserTab tab : tabs) {
                    tab.setGroup(groupName);
                }
                Log.d(TAG, "Auto-grouped " + tabs.size() + " tabs under '" + groupName + "'");
            }
        }
    }
    
    // 清理资源
    public void cleanup() {
        closeAllTabs(false);
        closeAllTabs(true);
    }
}