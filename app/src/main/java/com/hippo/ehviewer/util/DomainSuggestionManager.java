package com.hippo.ehviewer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.client.BookmarkManager;
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.client.data.BookmarkInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 域名智能补全管理器
 * 提供域名、协议、历史记录、书签的智能补全建议
 */
public class DomainSuggestionManager {
    private static final String TAG = "DomainSuggestionManager";
    private static final String PREF_NAME = "domain_suggestions";
    private static final String KEY_POPULARITY = "popularity_";

    private final Context mContext;
    private final BookmarkManager mBookmarkManager;
    private final HistoryManager mHistoryManager;
    private final SharedPreferences mPreferences;

    // 主流网站域名库
    private final List<String> mPopularDomains = Arrays.asList(
        // 搜索引擎
        "google.com", "baidu.com", "bing.com", "yahoo.com", "duckduckgo.com", "sogou.com", "so.com",
        // 社交媒体
        "facebook.com", "twitter.com", "instagram.com", "linkedin.com", "weibo.com", "zhihu.com",
        "bilibili.com", "youtube.com", "douyin.com", "tiktok.com", "reddit.com", "discord.com",
        // 电商平台
        "amazon.com", "taobao.com", "tmall.com", "jd.com", "ebay.com", "alibaba.com", "1688.com",
        // 新闻媒体
        "news.google.com", "bbc.com", "cnn.com", "nytimes.com", "reuters.com", "sina.com.cn",
        "sohu.com", "163.com", "qq.com", "ifeng.com", "xinhuanet.com", "people.com.cn",
        // 视频网站
        "youtube.com", "bilibili.com", "iqiyi.com", "youku.com", "tencent.com", "vimeo.com",
        "dailymotion.com", "twitch.tv", "hulu.com", "netflix.com", "pornhub.com", "xvideos.com",
        // 开发者工具
        "github.com", "gitlab.com", "stackoverflow.com", "csdn.net", "cnblogs.com", "juejin.cn",
        "segmentfault.com", "leetcode.com", "codeforces.com", "hackerrank.com",
        // 邮箱服务
        "gmail.com", "outlook.com", "163.com", "qq.com", "126.com", "sina.com", "yahoo.com",
        "hotmail.com", "live.com", "mail.ru", "yandex.com",
        // 工具网站
        "wikipedia.org", "translate.google.com", "fanyi.baidu.com", "deepl.com", "grammarly.com",
        "wolframalpha.com", "archive.org", "speedtest.net", "whatismyipaddress.com",
        // 购物娱乐
        "steam.com", "epicgames.com", "origin.com", "gog.com", "itch.io", "nintendo.com",
        "playstation.com", "xbox.com", "minecraft.net", "roblox.com",
        // 学术教育
        "coursera.org", "edx.org", "udacity.com", "khanacademy.org", "ted.com",
        "wikipedia.org", "arxiv.org", "scholar.google.com", "researchgate.net",
        // 政府机构
        "gov.cn", "gov.uk", "gov.us", "europa.eu", "who.int", "un.org",
        // 金融服务
        "bankofchina.com", "icbc.com.cn", "ccb.com", "alipay.com", "paypal.com",
        "bankcomm.com", "cmbchina.com", "spdb.com.cn", "citicbank.com",
        // 旅游交通
        "ctrip.com", "qunar.com", "booking.com", "airbnb.com", "expedia.com",
        "12306.cn", "trip.com", "elong.com", "meituan.com", "dianping.com",
        // 其他常用
        "weather.com", "accuweather.com", "cnbeta.com", "36kr.com", "huxiu.com",
        "guokr.com", "douban.com", "tieba.baidu.com", "nga.cn", "chiphell.com"
    );

    // 协议补全映射
    private final Map<String, String> mProtocolMappings = new HashMap<String, String>() {{
        put("htt", "https://");
        put("http", "https://");
        put("ftp", "ftp://");
        put("ftps", "ftps://");
        put("ssh", "ssh://");
        put("git", "git://");
        put("file", "file://");
        put("mailto", "mailto:");
        put("tel", "tel:");
        put("sms", "sms:");
        put("geo", "geo:");
        put("market", "market:");
        put("intent", "intent:");
    }};

    // 域名别名映射
    private final Map<String, String> mDomainAliases = new HashMap<String, String>() {{
        put("goo", "google.com");
        put("fb", "facebook.com");
        put("tw", "twitter.com");
        put("ig", "instagram.com");
        put("gh", "github.com");
        put("yt", "youtube.com");
        put("bd", "baidu.com");
        put("wb", "weibo.com");
        put("zh", "zhihu.com");
        put("bili", "bilibili.com");
        put("tb", "taobao.com");
        put("tm", "tmall.com");
        put("jd", "jd.com");
        put("amz", "amazon.com");
        put("bbc", "bbc.com");
        put("cnn", "cnn.com");
        put("so", "stackoverflow.com");
        put("gm", "gmail.com");
        put("wiki", "wikipedia.org");
        put("translate", "translate.google.com");
        put("fanyi", "fanyi.baidu.com");
        put("deepl", "deepl.com");
        put("coursera", "coursera.org");
        put("edx", "edx.org");
        put("udacity", "udacity.com");
        put("khan", "khanacademy.org");
        put("ted", "ted.com");
        put("steam", "steam.com");
        put("epic", "epicgames.com");
        put("paypal", "paypal.com");
        put("booking", "booking.com");
        put("airbnb", "airbnb.com");
        put("trip", "trip.com");
        put("ctrip", "ctrip.com");
        put("qunar", "qunar.com");
        put("12306", "12306.cn");
        put("meituan", "meituan.com");
        put("dianping", "dianping.com");
    }};

    public DomainSuggestionManager(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
        this.mBookmarkManager = BookmarkManager.getInstance(mContext);
        this.mHistoryManager = HistoryManager.getInstance(mContext);
        this.mPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取智能补全建议
     */
    public List<SuggestionItem> getSuggestions(@NonNull String input) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        if (input.trim().isEmpty()) {
            return suggestions;
        }

        String lowerInput = input.toLowerCase().trim();

        // 1. 协议补全（优先级最高）
        if (!lowerInput.contains("://") && !lowerInput.contains(":")) {
            String protocolSuggestion = getProtocolSuggestion(lowerInput);
            if (protocolSuggestion != null) {
                suggestions.add(new SuggestionItem(protocolSuggestion, SuggestionType.PROTOCOL, 100));
            }
        }

        // 2. 别名补全
        if (!lowerInput.contains(".")) {
            String aliasSuggestion = getAliasSuggestion(lowerInput);
            if (aliasSuggestion != null) {
                suggestions.add(new SuggestionItem("https://" + aliasSuggestion, SuggestionType.DOMAIN, 95));
            }
        }

        // 3. 域名补全
        List<String> domainSuggestions = getDomainSuggestions(lowerInput);
        for (String domain : domainSuggestions) {
            String fullUrl = lowerInput.startsWith("http") ? domain : "https://" + domain;
            suggestions.add(new SuggestionItem(fullUrl, SuggestionType.DOMAIN, 90));
        }

        // 4. 历史记录建议
        List<String> historySuggestions = getHistorySuggestions(lowerInput);
        for (String historyUrl : historySuggestions) {
            suggestions.add(new SuggestionItem(historyUrl, SuggestionType.HISTORY, 80));
        }

        // 5. 书签建议
        List<String> bookmarkSuggestions = getBookmarkSuggestions(lowerInput);
        for (String bookmarkUrl : bookmarkSuggestions) {
            suggestions.add(new SuggestionItem(bookmarkUrl, SuggestionType.BOOKMARK, 85));
        }

        // 6. 搜索建议（如果没有其他建议）
        if (suggestions.isEmpty() && !lowerInput.contains(" ") && lowerInput.length() > 2) {
            String searchUrl = "https://www.google.com/search?q=" + android.net.Uri.encode(lowerInput);
            suggestions.add(new SuggestionItem(searchUrl, SuggestionType.SEARCH, 60));
        }

        // 根据优先级排序
        Collections.sort(suggestions, (a, b) -> Integer.compare(b.priority, a.priority));

        // 限制返回数量
        return suggestions.size() > 10 ? suggestions.subList(0, 10) : suggestions;
    }

    /**
     * 获取协议补全建议
     */
    @Nullable
    private String getProtocolSuggestion(String input) {
        for (Map.Entry<String, String> entry : mProtocolMappings.entrySet()) {
            if (input.startsWith(entry.getKey()) && input.length() <= entry.getKey().length() + 2) {
                return entry.getValue() + input.substring(entry.getKey().length());
            }
        }
        return null;
    }

    /**
     * 获取别名补全建议
     */
    @Nullable
    private String getAliasSuggestion(String input) {
        for (Map.Entry<String, String> entry : mDomainAliases.entrySet()) {
            if (entry.getKey().startsWith(input) && input.length() >= 2) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 获取域名补全建议
     */
    private List<String> getDomainSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();
        Set<String> addedDomains = new HashSet<>();

        // 检查是否包含点号（可能是域名的一部分）
        boolean hasDot = input.contains(".");

        for (String domain : mPopularDomains) {
            if (addedDomains.size() >= 8) break; // 限制数量

            if (hasDot) {
                // 如果输入包含点号，尝试补全域名
                if (domain.startsWith(input) || domain.contains(input)) {
                    if (addedDomains.add(domain)) {
                        suggestions.add(domain);
                    }
                }
            } else {
                // 如果不包含点号，尝试前缀匹配
                if (domain.startsWith(input) && input.length() >= 2) {
                    if (addedDomains.add(domain)) {
                        suggestions.add(domain);
                    }
                }
            }
        }

        // 按域名长度排序，短的域名优先
        Collections.sort(suggestions, Comparator.comparingInt(String::length));

        return suggestions;
    }

    /**
     * 获取历史记录建议
     */
    private List<String> getHistorySuggestions(String input) {
        List<String> suggestions = new ArrayList<>();
        try {
            List<HistoryManager.HistoryItem> historyItems = mHistoryManager.getAllHistory();

            for (HistoryManager.HistoryItem item : historyItems) {
                if (item.url != null && item.url.toLowerCase().contains(input.toLowerCase())) {
                    suggestions.add(item.url);
                    if (suggestions.size() >= 5) break; // 限制数量
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting history suggestions", e);
        }

        return suggestions;
    }

    /**
     * 获取书签建议
     */
    private List<String> getBookmarkSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();
        try {
            List<BookmarkInfo> bookmarks = mBookmarkManager.getAllBookmarks();

            for (BookmarkInfo bookmark : bookmarks) {
                if (bookmark.url != null && bookmark.url.toLowerCase().contains(input.toLowerCase())) {
                    suggestions.add(bookmark.url);
                    if (suggestions.size() >= 5) break; // 限制数量
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting bookmark suggestions", e);
        }

        return suggestions;
    }

    /**
     * 增加域名的使用频率（用于排序优化）
     */
    public void increaseDomainPopularity(String domain) {
        try {
            String key = KEY_POPULARITY + domain;
            int currentPopularity = mPreferences.getInt(key, 0);
            mPreferences.edit().putInt(key, currentPopularity + 1).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error increasing domain popularity", e);
        }
    }

    /**
     * 获取域名的使用频率
     */
    public int getDomainPopularity(String domain) {
        return mPreferences.getInt(KEY_POPULARITY + domain, 0);
    }

    /**
     * 建议项类型枚举
     */
    public enum SuggestionType {
        PROTOCOL,    // 协议补全
        DOMAIN,      // 域名补全
        ALIAS,       // 别名补全
        HISTORY,     // 历史记录
        BOOKMARK,    // 书签
        SEARCH       // 搜索建议
    }

    /**
     * 建议项数据类
     */
    public static class SuggestionItem {
        public final String url;
        public final SuggestionType type;
        public final int priority;

        public SuggestionItem(String url, SuggestionType type, int priority) {
            this.url = url;
            this.type = type;
            this.priority = priority;
        }

        public String getDisplayText() {
            switch (type) {
                case PROTOCOL:
                    return "🔗 " + url;
                case DOMAIN:
                    return "🌐 " + url;
                case ALIAS:
                    return "⭐ " + url;
                case HISTORY:
                    return "🕐 " + url;
                case BOOKMARK:
                    return "⭐ " + url;
                case SEARCH:
                    return "🔍 搜索: " + url.substring(url.indexOf('=') + 1);
                default:
                    return url;
            }
        }

        @Override
        public String toString() {
            return getDisplayText();
        }
    }
}
