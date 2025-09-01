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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 推荐网站管理器
 * 管理世界顶级网站列表和常用网站推荐
 */
public class TopSitesManager {

    /**
     * 网站信息类
     */
    public static class SiteInfo {
        public final String name;
        public final String url;
        public final String description;
        public final String category;
        public final String icon;

        public SiteInfo(String name, String url, String description, String category, String icon) {
            this.name = name;
            this.url = url;
            this.description = description;
            this.category = category;
            this.icon = icon;
        }
    }

    // 搜索引擎
    private static final List<SiteInfo> SEARCH_ENGINES = Arrays.asList(
        new SiteInfo("百度", "https://www.baidu.com", "中国最大的搜索引擎", "搜索", "🔍"),
        new SiteInfo("Google", "https://www.google.com", "全球最大的搜索引擎", "搜索", "🌐"),
        new SiteInfo("必应", "https://www.bing.com", "微软搜索引擎", "搜索", "🔎"),
        new SiteInfo("搜狗", "https://www.sogou.com", "中文搜索引擎", "搜索", "🐕"),
        new SiteInfo("360搜索", "https://www.so.com", "360安全搜索", "搜索", "🛡️"),
        new SiteInfo("DuckDuckGo", "https://duckduckgo.com", "保护隐私的搜索引擎", "搜索", "🦆")
    );

    // 社交媒体
    private static final List<SiteInfo> SOCIAL_MEDIA = Arrays.asList(
        new SiteInfo("微信", "https://weixin.qq.com", "腾讯即时通讯", "社交", "💬"),
        new SiteInfo("微博", "https://weibo.com", "中国微博客平台", "社交", "📱"),
        new SiteInfo("QQ", "https://www.qq.com", "腾讯QQ官网", "社交", "🐧"),
        new SiteInfo("Facebook", "https://www.facebook.com", "全球最大社交网络", "社交", "👥"),
        new SiteInfo("Twitter", "https://twitter.com", "全球实时信息网络", "社交", "🐦"),
        new SiteInfo("Instagram", "https://www.instagram.com", "图片和视频分享", "社交", "📸"),
        new SiteInfo("LinkedIn", "https://www.linkedin.com", "职业社交网络", "社交", "💼"),
        new SiteInfo("TikTok", "https://www.tiktok.com", "短视频分享平台", "社交", "🎵"),
        new SiteInfo("知乎", "https://www.zhihu.com", "中文问答社区", "社交", "🤔"),
        new SiteInfo("小红书", "https://www.xiaohongshu.com", "生活方式分享平台", "社交", "📝")
    );

    // 视频网站
    private static final List<SiteInfo> VIDEO_SITES = Arrays.asList(
        new SiteInfo("YouTube", "https://www.youtube.com", "全球最大视频分享网站", "视频", "📺"),
        new SiteInfo("哔哩哔哩", "https://www.bilibili.com", "年轻人喜爱的视频社区", "视频", "📹"),
        new SiteInfo("腾讯视频", "https://v.qq.com", "腾讯视频平台", "视频", "🎬"),
        new SiteInfo("爱奇艺", "https://www.iqiyi.com", "爱奇艺视频网站", "视频", "🎭"),
        new SiteInfo("优酷", "https://www.youku.com", "优酷视频平台", "视频", "🎪"),
        new SiteInfo("Netflix", "https://www.netflix.com", "全球流媒体平台", "视频", "🎦"),
        new SiteInfo("抖音", "https://www.douyin.com", "短视频平台", "视频", "🎵")
    );

    // 电商网站
    private static final List<SiteInfo> ECOMMERCE_SITES = Arrays.asList(
        new SiteInfo("淘宝", "https://www.taobao.com", "中国最大的网购平台", "购物", "🛒"),
        new SiteInfo("京东", "https://www.jd.com", "中国自营电商平台", "购物", "🏪"),
        new SiteInfo("天猫", "https://www.tmall.com", "品牌商城平台", "购物", "🐱"),
        new SiteInfo("Amazon", "https://www.amazon.com", "全球最大电商平台", "购物", "📦"),
        new SiteInfo("拼多多", "https://www.pinduoduo.com", "社交电商平台", "购物", "🛍️"),
        new SiteInfo("eBay", "https://www.ebay.com", "全球在线拍卖及购物网站", "购物", "🏷️"),
        new SiteInfo("苏宁易购", "https://www.suning.com", "综合网上购物平台", "购物", "🏬")
    );

    // 新闻网站
    private static final List<SiteInfo> NEWS_SITES = Arrays.asList(
        new SiteInfo("新浪", "https://www.sina.com.cn", "综合门户网站", "新闻", "📰"),
        new SiteInfo("网易", "https://www.163.com", "综合互联网服务", "新闻", "📋"),
        new SiteInfo("腾讯网", "https://www.qq.com", "腾讯门户网站", "新闻", "📄"),
        new SiteInfo("搜狐", "https://www.sohu.com", "综合门户网站", "新闻", "📌"),
        new SiteInfo("今日头条", "https://www.toutiao.com", "个性化资讯推荐", "新闻", "📱"),
        new SiteInfo("BBC", "https://www.bbc.com", "英国广播公司", "新闻", "📻"),
        new SiteInfo("CNN", "https://www.cnn.com", "美国有线电视新闻网", "新闻", "📺"),
        new SiteInfo("Reuters", "https://www.reuters.com", "路透社", "新闻", "🌍")
    );

    // 工具网站
    private static final List<SiteInfo> TOOL_SITES = Arrays.asList(
        new SiteInfo("GitHub", "https://github.com", "全球最大代码托管平台", "工具", "💻"),
        new SiteInfo("Stack Overflow", "https://stackoverflow.com", "程序员问答社区", "工具", "🔧"),
        new SiteInfo("Wikipedia", "https://www.wikipedia.org", "全球最大百科全书", "工具", "📚"),
        new SiteInfo("翻译", "https://translate.google.com", "谷歌翻译", "工具", "🌐"),
        new SiteInfo("有道翻译", "https://fanyi.youdao.com", "网易有道翻译", "工具", "📖"),
        new SiteInfo("百度翻译", "https://fanyi.baidu.com", "百度在线翻译", "工具", "🔤"),
        new SiteInfo("Dropbox", "https://www.dropbox.com", "云存储服务", "工具", "☁️"),
        new SiteInfo("OneDrive", "https://onedrive.live.com", "微软云存储", "工具", "📁")
    );

    // 教育网站
    private static final List<SiteInfo> EDUCATION_SITES = Arrays.asList(
        new SiteInfo("学习强国", "https://www.xuexi.cn", "新时代学习平台", "教育", "🎓"),
        new SiteInfo("中国大学MOOC", "https://www.icourse163.org", "在线教育平台", "教育", "🏫"),
        new SiteInfo("腾讯课堂", "https://ke.qq.com", "腾讯在线教育", "教育", "📖"),
        new SiteInfo("网易云课堂", "https://study.163.com", "网易在线教育", "教育", "💡"),
        new SiteInfo("Khan Academy", "https://www.khanacademy.org", "免费在线学习", "教育", "🧠"),
        new SiteInfo("Coursera", "https://www.coursera.org", "在线课程平台", "教育", "🎯"),
        new SiteInfo("edX", "https://www.edx.org", "在线教育平台", "教育", "📚")
    );

    // 娱乐网站
    private static final List<SiteInfo> ENTERTAINMENT_SITES = Arrays.asList(
        new SiteInfo("Steam", "https://store.steampowered.com", "PC游戏平台", "娱乐", "🎮"),
        new SiteInfo("网易云音乐", "https://music.163.com", "在线音乐平台", "娱乐", "🎵"),
        new SiteInfo("QQ音乐", "https://y.qq.com", "腾讯音乐平台", "娱乐", "🎶"),
        new SiteInfo("Spotify", "https://www.spotify.com", "全球音乐流媒体", "娱乐", "🎧"),
        new SiteInfo("豆瓣", "https://www.douban.com", "文化生活社区", "娱乐", "📚"),
        new SiteInfo("起点中文网", "https://www.qidian.com", "网络小说平台", "娱乐", "📖"),
        new SiteInfo("Twitch", "https://www.twitch.tv", "游戏直播平台", "娱乐", "📡")
    );

    /**
     * 获取所有推荐网站
     */
    public static List<SiteInfo> getAllTopSites() {
        List<SiteInfo> allSites = new ArrayList<>();
        allSites.addAll(SEARCH_ENGINES);
        allSites.addAll(SOCIAL_MEDIA);
        allSites.addAll(VIDEO_SITES);
        allSites.addAll(ECOMMERCE_SITES);
        allSites.addAll(NEWS_SITES);
        allSites.addAll(TOOL_SITES);
        allSites.addAll(EDUCATION_SITES);
        allSites.addAll(ENTERTAINMENT_SITES);
        return allSites;
    }

    /**
     * 根据分类获取网站
     */
    public static List<SiteInfo> getSitesByCategory(String category) {
        switch (category.toLowerCase()) {
            case "搜索":
                return new ArrayList<>(SEARCH_ENGINES);
            case "社交":
                return new ArrayList<>(SOCIAL_MEDIA);
            case "视频":
                return new ArrayList<>(VIDEO_SITES);
            case "购物":
                return new ArrayList<>(ECOMMERCE_SITES);
            case "新闻":
                return new ArrayList<>(NEWS_SITES);
            case "工具":
                return new ArrayList<>(TOOL_SITES);
            case "教育":
                return new ArrayList<>(EDUCATION_SITES);
            case "娱乐":
                return new ArrayList<>(ENTERTAINMENT_SITES);
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 搜索网站
     */
    public static List<SiteInfo> searchSites(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllTopSites();
        }

        List<SiteInfo> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (SiteInfo site : getAllTopSites()) {
            if (site.name.toLowerCase().contains(lowerQuery) ||
                site.description.toLowerCase().contains(lowerQuery) ||
                site.url.toLowerCase().contains(lowerQuery) ||
                site.category.toLowerCase().contains(lowerQuery)) {
                results.add(site);
            }
        }

        return results;
    }

    /**
     * 获取热门网站（前20个）
     */
    public static List<SiteInfo> getPopularSites() {
        List<SiteInfo> popularSites = new ArrayList<>();
        
        // 选择每个分类中最流行的网站
        popularSites.addAll(SEARCH_ENGINES.subList(0, Math.min(3, SEARCH_ENGINES.size())));
        popularSites.addAll(SOCIAL_MEDIA.subList(0, Math.min(4, SOCIAL_MEDIA.size())));
        popularSites.addAll(VIDEO_SITES.subList(0, Math.min(3, VIDEO_SITES.size())));
        popularSites.addAll(ECOMMERCE_SITES.subList(0, Math.min(3, ECOMMERCE_SITES.size())));
        popularSites.addAll(NEWS_SITES.subList(0, Math.min(3, NEWS_SITES.size())));
        popularSites.addAll(TOOL_SITES.subList(0, Math.min(2, TOOL_SITES.size())));
        popularSites.addAll(ENTERTAINMENT_SITES.subList(0, Math.min(2, ENTERTAINMENT_SITES.size())));
        
        return popularSites;
    }

    /**
     * 获取所有分类
     */
    public static List<String> getAllCategories() {
        return Arrays.asList("搜索", "社交", "视频", "购物", "新闻", "工具", "教育", "娱乐");
    }

    /**
     * 根据关键字获取建议网站
     */
    public static List<SiteInfo> getSuggestionsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPopularSites();
        }

        List<SiteInfo> suggestions = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        // 优先匹配名称
        for (SiteInfo site : getAllTopSites()) {
            if (site.name.toLowerCase().startsWith(lowerKeyword)) {
                suggestions.add(site);
            }
        }

        // 再匹配描述和URL
        if (suggestions.size() < 10) {
            for (SiteInfo site : getAllTopSites()) {
                if (!suggestions.contains(site) && 
                    (site.description.toLowerCase().contains(lowerKeyword) ||
                     site.url.toLowerCase().contains(lowerKeyword))) {
                    suggestions.add(site);
                    if (suggestions.size() >= 10) break;
                }
            }
        }

        return suggestions;
    }
}