package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 导入Suggestion类
import com.hippo.ehviewer.ui.browser.SmartAddressBar.Suggestion;

public class BookmarkMatcher {
    
    private Context context;
    
    // 模拟书签数据 - 实际应用中应该从数据库读取
    private static final BookmarkEntry[] MOCK_BOOKMARKS = {
        new BookmarkEntry("Google Search", "https://www.google.com", "搜索"),
        new BookmarkEntry("GitHub - Home", "https://github.com", "开发"),
        new BookmarkEntry("Stack Overflow", "https://stackoverflow.com", "开发"),
        new BookmarkEntry("MDN Web Docs", "https://developer.mozilla.org", "开发"),
        new BookmarkEntry("Reddit", "https://www.reddit.com", "社交"),
        new BookmarkEntry("Hacker News", "https://news.ycombinator.com", "科技")
    };
    
    public BookmarkMatcher(Context context) {
        this.context = context;
    }
    
    public List<Suggestion> findMatches(String query) {
        List<Suggestion> matches = new ArrayList<>();
        
        if (TextUtils.isEmpty(query)) {
            return matches;
        }
        
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        
        // 搜索书签
        for (BookmarkEntry entry : MOCK_BOOKMARKS) {
            if (isMatch(entry, lowerQuery)) {
                Suggestion bookmarkSuggestion = new Suggestion();
                bookmarkSuggestion.type = Suggestion.SuggestionType.BOOKMARK;
                bookmarkSuggestion.title = entry.title;
                bookmarkSuggestion.subtitle = extractDomain(entry.url);
                bookmarkSuggestion.url = entry.url;
                bookmarkSuggestion.priority = 3;
                matches.add(bookmarkSuggestion);
            }
        }
        
        // 按照匹配度排序
        matches.sort((s1, s2) -> {
            int score1 = calculateMatchScore(s1.title, lowerQuery);
            int score2 = calculateMatchScore(s2.title, lowerQuery);
            return Integer.compare(score2, score1);
        });
        
        // 限制书签建议数量
        return matches.subList(0, Math.min(matches.size(), 3));
    }
    
    private boolean isMatch(BookmarkEntry entry, String query) {
        String lowerTitle = entry.title.toLowerCase(Locale.getDefault());
        String lowerUrl = entry.url.toLowerCase(Locale.getDefault());
        String lowerTag = entry.tag != null ? entry.tag.toLowerCase(Locale.getDefault()) : "";
        
        // 标题匹配
        if (lowerTitle.contains(query)) {
            return true;
        }
        
        // URL匹配
        if (lowerUrl.contains(query)) {
            return true;
        }
        
        // 标签匹配
        if (lowerTag.contains(query)) {
            return true;
        }
        
        return false;
    }
    
    private int calculateMatchScore(String text, String query) {
        String lowerText = text.toLowerCase(Locale.getDefault());
        
        if (lowerText.equals(query)) {
            return 100;
        }
        
        if (lowerText.startsWith(query)) {
            return 80;
        }
        
        if (lowerText.contains(" " + query)) {
            return 60;
        }
        
        if (lowerText.contains(query)) {
            return 40;
        }
        
        return 0;
    }
    
    private String extractDomain(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                int startIndex = url.indexOf("://") + 3;
                int endIndex = url.indexOf("/", startIndex);
                if (endIndex == -1) {
                    endIndex = url.length();
                }
                String domain = url.substring(startIndex, endIndex);
                
                // 移除 www. 前缀
                if (domain.startsWith("www.")) {
                    domain = domain.substring(4);
                }
                
                return domain;
            }
        } catch (Exception e) {
            // 如果解析失败，返回原始URL
        }
        return url;
    }
    
    private static class BookmarkEntry {
        final String title;
        final String url;
        final String tag;
        
        BookmarkEntry(String title, String url, String tag) {
            this.title = title;
            this.url = url;
            this.tag = tag;
        }
    }
}