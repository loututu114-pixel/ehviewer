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

package com.hippo.ehviewer.client;

import android.content.Context;
import android.webkit.WebView;
import androidx.annotation.NonNull;

/**
 * 广告管理器
 * 管理应用内的广告展示和变现
 */
public class AdManager {

    private static final String TAG = "AdManager";

    private static AdManager sInstance;

    private Context mContext;
    private boolean mAdsEnabled = true;
    private AdConfig mAdConfig;

    /**
     * 广告配置
     */
    public static class AdConfig {
        public boolean showSearchAds = true;
        public boolean showFixedAds = false; // 默认关闭固定广告，保持用户体验
        public int searchAdFrequency = 3; // 每3个搜索结果显示1个广告
        public String googleAdClientId = "your-google-ad-client-id";
        public String[] adKeywords = {"shopping", "travel", "finance", "technology"};

        // 广告位置配置
        public boolean showAdsInSearchResults = true;
        public boolean showAdsInBookmarks = false;
        public boolean showAdsInHistory = false;
        public boolean showAdsInSettings = false;
    }

    /**
     * 获取单例实例
     */
    public static synchronized AdManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new AdManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private AdManager(Context context) {
        this.mContext = context;
        this.mAdConfig = new AdConfig();
        initializeAds();
    }

    /**
     * 初始化广告系统
     */
    private void initializeAds() {
        // 这里可以初始化Google AdMob或其他广告SDK
        // 由于是概念验证，我们使用模拟实现
    }

    /**
     * 为搜索结果添加广告
     */
    public String injectAdsIntoSearchResults(String searchQuery, String searchResults) {
        if (!mAdsEnabled || !mAdConfig.showSearchAds) {
            return searchResults;
        }

        // 简单的广告注入逻辑
        StringBuilder enhancedResults = new StringBuilder();

        // 在搜索结果中注入Google广告
        String adHtml = generateSearchAdHtml(searchQuery);

        // 按照配置的频率插入广告
        String[] resultLines = searchResults.split("\n");
        int adIndex = 0;

        for (int i = 0; i < resultLines.length; i++) {
            enhancedResults.append(resultLines[i]).append("\n");

            // 每隔指定数量的结果插入广告
            if ((i + 1) % mAdConfig.searchAdFrequency == 0 && adIndex < 2) {
                enhancedResults.append(adHtml).append("\n");
                adIndex++;
            }
        }

        return enhancedResults.toString();
    }

    /**
     * 生成搜索广告HTML
     */
    private String generateSearchAdHtml(String searchQuery) {
        return "<div class='ehviewer-ad' style='border: 1px solid #ddd; padding: 10px; margin: 10px 0; background: #f8f9fa;'>" +
               "<div style='font-size: 12px; color: #666; margin-bottom: 5px;'>广告</div>" +
               "<div style='font-size: 14px; color: #1a0dab; cursor: pointer;' onclick='window.open(\"https://www.google.com/search?q=" + searchQuery + "\", \"_blank\")'>" +
               "点击查看更多关于 " + searchQuery + " 的结果</div>" +
               "<div style='font-size: 12px; color: #006621;'>www.google.com</div>" +
               "</div>";
    }

    /**
     * 在WebView中注入广告脚本
     */
    public void injectAdScripts(WebView webView) {
        if (webView == null) {
            return;
        }

        String adScript =
            "(function() {" +
            "  console.log('EhViewer Ad scripts loaded');" +
            "" +
            "  // 广告点击跟踪（隐私保护模式）" +
            "  function trackAdClick(adType, adPosition) {" +
            "    console.log('Ad clicked:', adType, adPosition);" +
            "    // 这里可以发送匿名统计数据" +
            "  }" +
            "" +
            "  // 为广告元素添加点击跟踪" +
            "  document.addEventListener('click', function(e) {" +
            "    var target = e.target;" +
            "    while (target && target !== document) {" +
            "      if (target.classList && target.classList.contains('ehviewer-ad')) {" +
            "        trackAdClick('search', target.dataset.position || 'unknown');" +
            "        break;" +
            "      }" +
            "      target = target.parentElement;" +
            "    }" +
            "  });" +
            "" +
            "  console.log('Ad tracking enabled');" +
            "})();";

        webView.evaluateJavascript(adScript, null);
    }

    /**
     * 获取推荐的广告关键词
     */
    public String[] getRecommendedAdKeywords(String searchQuery) {
        // 基于搜索查询推荐广告关键词
        if (searchQuery == null) {
            return new String[0];
        }

        String lowerQuery = searchQuery.toLowerCase();

        // 简单的关键词匹配逻辑
        if (lowerQuery.contains("buy") || lowerQuery.contains("purchase") || lowerQuery.contains("shopping")) {
            return new String[]{"shopping", "deals", "offers"};
        } else if (lowerQuery.contains("travel") || lowerQuery.contains("hotel") || lowerQuery.contains("flight")) {
            return new String[]{"travel deals", "hotels", "flights"};
        } else if (lowerQuery.contains("finance") || lowerQuery.contains("money") || lowerQuery.contains("bank")) {
            return new String[]{"financial services", "loans", "investing"};
        } else {
            return new String[]{"related products", "services", "information"};
        }
    }

    /**
     * 检查是否应该显示广告
     */
    public boolean shouldShowAds() {
        return mAdsEnabled && (mAdConfig.showSearchAds || mAdConfig.showFixedAds);
    }

    /**
     * 设置广告启用状态
     */
    public void setAdsEnabled(boolean enabled) {
        this.mAdsEnabled = enabled;
    }

    /**
     * 获取广告配置
     */
    public AdConfig getAdConfig() {
        return mAdConfig;
    }

    /**
     * 更新广告配置
     */
    public void updateAdConfig(AdConfig config) {
        this.mAdConfig = config;
    }

    /**
     * 生成隐私保护的广告HTML
     */
    public String generatePrivacyFriendlyAd(String keyword, String position) {
        return "<div class='ehviewer-ad' data-position='" + position + "' " +
               "style='border: 1px solid #e0e0e0; border-radius: 4px; padding: 12px; margin: 8px 0; background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);'>" +
               "<div style='font-size: 11px; color: #666; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px;'>赞助内容</div>" +
               "<div style='font-size: 14px; color: #2c3e50; font-weight: 500; margin-bottom: 4px;'>探索更多关于 " + keyword + " 的精彩内容</div>" +
               "<div style='font-size: 12px; color: #7f8c8d;'>点击查看相关推荐</div>" +
               "</div>";
    }

    /**
     * 获取广告展示统计（用于优化）
     */
    public AdStats getAdStats() {
        AdStats stats = new AdStats();
        // 这里应该从存储中获取实际统计数据
        stats.totalAdsShown = 0;
        stats.totalClicks = 0;
        stats.clickThroughRate = 0.0f;
        return stats;
    }

    /**
     * 广告统计信息
     */
    public static class AdStats {
        public int totalAdsShown;
        public int totalClicks;
        public float clickThroughRate;

        public String getFormattedCTR() {
            return String.format("%.2f%%", clickThroughRate * 100);
        }
    }

    /**
     * 清理广告相关数据
     */
    public void clearAdData() {
        // 清理广告缓存和统计数据
    }

    /**
     * 根据用户偏好调整广告策略
     */
    public void adjustAdStrategyBasedOnUserBehavior() {
        // 分析用户行为，调整广告展示策略
        // 例如：如果用户经常点击某种类型的广告，可以多展示此类广告
        // 如果用户经常跳过某种广告，可以减少此类广告
    }
}
