package com.hippo.ehviewer.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.analytics.UserBehaviorAnalyzer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 浏览器快捷方式桌面小部件
 * 提供常用网站快速访问，个性化推荐，搜索功能
 */
public class BrowserShortcutWidget extends BaseEhWidget {
    
    private static final String TAG = "BrowserShortcutWidget";
    private static final String PREFS_NAME = "browser_shortcut_widget";
    
    // 自定义Actions
    private static final String ACTION_OPEN_SITE = "com.hippo.ehviewer.widget.OPEN_SITE";
    private static final String ACTION_SEARCH = "com.hippo.ehviewer.widget.SEARCH";
    private static final String ACTION_ADD_BOOKMARK = "com.hippo.ehviewer.widget.ADD_BOOKMARK";
    private static final String ACTION_SETTINGS = "com.hippo.ehviewer.widget.SETTINGS";
    
    // 预定义的常用网站
    private static final Website[] DEFAULT_WEBSITES = {
        new Website("谷歌", "https://www.google.com", "🔍"),
        new Website("百度", "https://www.baidu.com", "🔍"),
        new Website("微博", "https://weibo.com", "📱"),
        new Website("知乎", "https://www.zhihu.com", "🤔"),
        new Website("B站", "https://www.bilibili.com", "📺"),
        new Website("淘宝", "https://www.taobao.com", "🛒"),
        new Website("京东", "https://www.jd.com", "📦"),
        new Website("GitHub", "https://github.com", "💻"),
        new Website("YouTube", "https://www.youtube.com", "▶️"),
        new Website("Twitter", "https://twitter.com", "🐦"),
        new Website("新闻", "https://news.google.com", "📰"),
        new Website("天气", "https://weather.com", "🌤️"),
        new Website("地图", "https://maps.google.com", "🗺️"),
        new Website("邮箱", "https://gmail.com", "📧"),
        new Website("云盘", "https://drive.google.com", "☁️"),
        new Website("音乐", "https://music.163.com", "🎵")
    };
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        
        // 初始化用户行为分析
        UserBehaviorAnalyzer.getInstance(context).recordFeatureUsage("browser_widget_update");
    }
    
    @Override
    protected RemoteViews createRemoteViews(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_browser_shortcuts);
        
        // 更新搜索框
        updateSearchBox(context, views);
        
        // 更新网站快捷方式
        updateWebsiteShortcuts(context, views, appWidgetId);
        
        // 更新个性化推荐
        updatePersonalizedRecommendations(context, views, appWidgetId);
        
        // 更新统计信息
        updateUsageStats(context, views);
        
        return views;
    }
    
    @Override
    protected void setupCustomClickActions(Context context, RemoteViews views, int appWidgetId) {
        // 搜索框点击
        Bundle searchExtras = new Bundle();
        searchExtras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.search_box,
            createClickPendingIntent(context, appWidgetId * 100, "search", searchExtras));
        
        // 设置按钮点击
        Bundle settingsExtras = new Bundle();
        settingsExtras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.settings_button,
            createClickPendingIntent(context, appWidgetId * 100 + 1, "settings", settingsExtras));
        
        // 添加书签按钮
        Bundle bookmarkExtras = new Bundle();
        bookmarkExtras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.add_bookmark_button,
            createClickPendingIntent(context, appWidgetId * 100 + 2, "add_bookmark", bookmarkExtras));
        
        // 网站快捷方式点击（通过设置每个按钮的点击事件）
        setupWebsiteClickActions(context, views, appWidgetId);
    }
    
    @Override
    protected void handleCustomAction(Context context, Intent intent, String action) {
        switch (action) {
            case ACTION_OPEN_SITE:
                String url = intent.getStringExtra("url");
                String title = intent.getStringExtra("title");
                openWebsite(context, url, title);
                break;
                
            case ACTION_SEARCH:
                openSearchPage(context);
                break;
                
            case ACTION_ADD_BOOKMARK:
                openBookmarkManager(context);
                break;
                
            case ACTION_SETTINGS:
                openWidgetSettings(context);
                break;
        }
    }
    
    @Override
    protected void handleWidgetClick(Context context, Intent intent) {
        String clickType = intent.getStringExtra("click_type");
        
        switch (clickType) {
            case "search":
                openSearchPage(context);
                break;
                
            case "add_bookmark":
                openBookmarkManager(context);
                break;
                
            case "settings":
                openWidgetSettings(context);
                break;
                
            case "website":
                String url = intent.getStringExtra("url");
                String title = intent.getStringExtra("title");
                openWebsite(context, url, title);
                break;
                
            default:
                super.handleWidgetClick(context, intent);
                break;
        }
    }
    
    /**
     * 更新搜索框
     */
    private void updateSearchBox(Context context, RemoteViews views) {
        // 获取用户偏好的搜索引擎
        String searchEngine = getPreferredSearchEngine(context);
        String searchHint = getSearchHint(searchEngine);
        
        views.setTextViewText(R.id.search_hint, searchHint);
        
        // 设置搜索引擎图标
        int searchIcon = getSearchEngineIcon(searchEngine);
        views.setInt(R.id.search_icon, "setImageResource", searchIcon);
    }
    
    /**
     * 更新网站快捷方式
     */
    private void updateWebsiteShortcuts(Context context, RemoteViews views, int appWidgetId) {
        List<Website> websites = getUserWebsites(context);
        
        // 显示前6个网站（2x3网格）
        int[] buttonIds = {
            R.id.website_button_1, R.id.website_button_2, R.id.website_button_3,
            R.id.website_button_4, R.id.website_button_5, R.id.website_button_6
        };
        
        int[] textIds = {
            R.id.website_text_1, R.id.website_text_2, R.id.website_text_3,
            R.id.website_text_4, R.id.website_text_5, R.id.website_text_6
        };
        
        for (int i = 0; i < buttonIds.length; i++) {
            if (i < websites.size()) {
                Website website = websites.get(i);
                
                // 设置网站图标（使用emoji或生成图标）
                views.setTextViewText(buttonIds[i], website.icon);
                views.setTextViewText(textIds[i], website.name);
                
                // 设置点击事件
                Bundle extras = new Bundle();
                extras.putString("url", website.url);
                extras.putString("title", website.name);
                views.setOnClickPendingIntent(buttonIds[i],
                    createClickPendingIntent(context, appWidgetId * 100 + 10 + i, "website", extras));
                    
            } else {
                // 隐藏未使用的按钮
                views.setTextViewText(buttonIds[i], "");
                views.setTextViewText(textIds[i], "");
            }
        }
    }
    
    /**
     * 更新个性化推荐
     */
    private void updatePersonalizedRecommendations(Context context, RemoteViews views, int appWidgetId) {
        UserBehaviorAnalyzer analyzer = UserBehaviorAnalyzer.getInstance(context);
        UserBehaviorAnalyzer.UserBehaviorSummary summary = analyzer.getBehaviorSummary();
        
        // 根据用户行为生成推荐
        List<Website> recommendations = generateRecommendations(context, summary);
        
        if (!recommendations.isEmpty()) {
            Website recommendation = recommendations.get(0);
            views.setTextViewText(R.id.recommendation_title, "为您推荐: " + recommendation.name);
            views.setTextViewText(R.id.recommendation_desc, "基于您的浏览习惯");
            
            // 设置推荐点击
            Bundle extras = new Bundle();
            extras.putString("url", recommendation.url);
            extras.putString("title", recommendation.name);
            views.setOnClickPendingIntent(R.id.recommendation_container,
                createClickPendingIntent(context, appWidgetId * 100 + 20, "website", extras));
        } else {
            views.setTextViewText(R.id.recommendation_title, "探索更多网站");
            views.setTextViewText(R.id.recommendation_desc, "点击发现精彩内容");
        }
    }
    
    /**
     * 更新使用统计
     */
    private void updateUsageStats(Context context, RemoteViews views) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int totalClicks = prefs.getInt("total_clicks", 0);
        long lastUsed = prefs.getLong("last_used", 0);
        
        String statsText = "";
        if (totalClicks > 0) {
            statsText = String.format("已使用 %d 次", totalClicks);
            
            if (lastUsed > 0) {
                long hoursSinceLastUse = (System.currentTimeMillis() - lastUsed) / (1000 * 60 * 60);
                if (hoursSinceLastUse < 1) {
                    statsText += " • 刚刚使用";
                } else if (hoursSinceLastUse < 24) {
                    statsText += String.format(" • %d小时前", hoursSinceLastUse);
                } else {
                    long daysSinceLastUse = hoursSinceLastUse / 24;
                    statsText += String.format(" • %d天前", daysSinceLastUse);
                }
            }
        } else {
            statsText = "点击开始浏览";
        }
        
        views.setTextViewText(R.id.usage_stats, statsText);
    }
    
    /**
     * 设置网站点击事件
     */
    private void setupWebsiteClickActions(Context context, RemoteViews views, int appWidgetId) {
        List<Website> websites = getUserWebsites(context);
        
        int[] buttonIds = {
            R.id.website_button_1, R.id.website_button_2, R.id.website_button_3,
            R.id.website_button_4, R.id.website_button_5, R.id.website_button_6
        };
        
        for (int i = 0; i < buttonIds.length && i < websites.size(); i++) {
            Website website = websites.get(i);
            
            Bundle extras = new Bundle();
            extras.putString("url", website.url);
            extras.putString("title", website.name);
            
            views.setOnClickPendingIntent(buttonIds[i],
                createClickPendingIntent(context, appWidgetId * 100 + 30 + i, "website", extras));
        }
    }
    
    /**
     * 打开网站
     */
    private void openWebsite(Context context, String url, String title) {
        if (url == null || url.isEmpty()) {
            url = "https://www.google.com";
            title = "谷歌搜索";
        }
        
        // 记录网站访问
        recordWebsiteVisit(context, url, title);
        
        // 记录用户行为
        UserBehaviorAnalyzer analyzer = UserBehaviorAnalyzer.getInstance(context);
        analyzer.recordFeatureUsage("browser_widget_website_click", 
            java.util.Collections.singletonMap("url", url));
        
        // 打开浏览器
        openBrowser(context, url, title);
        
        // 更新统计
        updateClickStats(context);
    }
    
    /**
     * 打开搜索页面
     */
    private void openSearchPage(Context context) {
        String searchEngine = getPreferredSearchEngine(context);
        String searchUrl = getSearchEngineUrl(searchEngine);
        
        UserBehaviorAnalyzer.getInstance(context).recordFeatureUsage("browser_widget_search");
        
        openBrowser(context, searchUrl, "搜索");
        updateClickStats(context);
    }
    
    /**
     * 打开书签管理器
     */
    private void openBookmarkManager(Context context) {
        // 打开一个书签管理页面或者显示添加书签的界面
        openBrowser(context, "chrome://bookmarks/", "书签管理");
        updateClickStats(context);
    }
    
    /**
     * 打开小部件设置
     */
    private void openWidgetSettings(Context context) {
        // 可以打开一个设置页面或者显示设置选项
        openBrowser(context, "https://settings.local/browser-widget", "小部件设置");
    }
    
    /**
     * 获取用户网站列表
     */
    private List<Website> getUserWebsites(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String websitesJson = prefs.getString("user_websites", "");
        
        List<Website> websites = new ArrayList<>();
        
        if (!websitesJson.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(websitesJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    websites.add(Website.fromJson(jsonObject));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse user websites", e);
            }
        }
        
        // 如果没有用户自定义网站，使用默认网站
        if (websites.isEmpty()) {
            websites.addAll(Arrays.asList(DEFAULT_WEBSITES));
            // 保存默认网站
            saveUserWebsites(context, websites);
        }
        
        return websites;
    }
    
    /**
     * 保存用户网站列表
     */
    private void saveUserWebsites(Context context, List<Website> websites) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Website website : websites) {
                jsonArray.put(website.toJson());
            }
            
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("user_websites", jsonArray.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save user websites", e);
        }
    }
    
    /**
     * 生成个性化推荐
     */
    private List<Website> generateRecommendations(Context context, UserBehaviorAnalyzer.UserBehaviorSummary summary) {
        List<Website> recommendations = new ArrayList<>();
        
        // 基于时间推荐
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 9) {
            // 早上推荐新闻
            recommendations.add(new Website("新闻资讯", "https://news.google.com", "📰"));
        } else if (hour >= 12 && hour < 14) {
            // 午餐时间推荐外卖
            recommendations.add(new Website("外卖订餐", "https://www.meituan.com", "🍜"));
        } else if (hour >= 18 && hour < 22) {
            // 晚上推荐娱乐
            recommendations.add(new Website("视频娱乐", "https://www.bilibili.com", "📺"));
        } else {
            // 默认推荐搜索
            recommendations.add(new Website("搜索引擎", "https://www.google.com", "🔍"));
        }
        
        // 基于使用模式推荐
        if ("深度用户".equals(summary.usagePattern)) {
            recommendations.add(new Website("开发工具", "https://github.com", "💻"));
        } else if ("频繁用户".equals(summary.usagePattern)) {
            recommendations.add(new Website("社交媒体", "https://weibo.com", "📱"));
        }
        
        return recommendations;
    }
    
    /**
     * 获取偏好搜索引擎
     */
    private String getPreferredSearchEngine(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("search_engine", "google");
    }
    
    /**
     * 获取搜索提示文本
     */
    private String getSearchHint(String searchEngine) {
        switch (searchEngine) {
            case "baidu": return "百度一下，你就知道";
            case "bing": return "必应搜索";
            case "yahoo": return "雅虎搜索";
            default: return "Google 搜索";
        }
    }
    
    /**
     * 获取搜索引擎图标
     */
    private int getSearchEngineIcon(String searchEngine) {
        switch (searchEngine) {
            case "baidu": return R.drawable.ic_baidu;
            case "bing": return R.drawable.ic_bing;
            case "yahoo": return R.drawable.ic_yahoo;
            default: return R.drawable.ic_google;
        }
    }
    
    /**
     * 获取搜索引擎URL
     */
    private String getSearchEngineUrl(String searchEngine) {
        switch (searchEngine) {
            case "baidu": return "https://www.baidu.com";
            case "bing": return "https://www.bing.com";
            case "yahoo": return "https://www.yahoo.com";
            default: return "https://www.google.com";
        }
    }
    
    /**
     * 记录网站访问
     */
    private void recordWebsiteVisit(Context context, String url, String title) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 更新访问次数
        String visitKey = "visit_" + url.hashCode();
        int visitCount = prefs.getInt(visitKey, 0) + 1;
        
        prefs.edit()
            .putInt(visitKey, visitCount)
            .putLong("last_visit_" + url.hashCode(), System.currentTimeMillis())
            .apply();
    }
    
    /**
     * 更新点击统计
     */
    private void updateClickStats(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int totalClicks = prefs.getInt("total_clicks", 0) + 1;
        
        prefs.edit()
            .putInt("total_clicks", totalClicks)
            .putLong("last_used", System.currentTimeMillis())
            .apply();
        
        // 异步更新小部件显示
        widgetExecutor.submit(() -> {
            forceUpdateAllWidgets(context);
        });
    }
    
    /**
     * 网站数据类
     */
    private static class Website {
        String name;
        String url;
        String icon;
        
        Website(String name, String url, String icon) {
            this.name = name;
            this.url = url;
            this.icon = icon;
        }
        
        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("url", url);
            json.put("icon", icon);
            return json;
        }
        
        static Website fromJson(JSONObject json) throws JSONException {
            return new Website(
                json.getString("name"),
                json.getString("url"),
                json.optString("icon", "🌐")
            );
        }
    }
}