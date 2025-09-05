package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 导入Suggestion类
import com.hippo.ehviewer.ui.browser.SmartAddressBar.Suggestion;

public class HistoryMatcher {
    
    private Context context;
    
    // 模拟历史记录数据 - 实际应用中应该从数据库读取
    private static final HistoryEntry[] MOCK_HISTORY = {
        new HistoryEntry("Google", "https://www.google.com", "www.google.com"),
        new HistoryEntry("GitHub", "https://github.com", "github.com"),
        new HistoryEntry("百度", "https://www.baidu.com", "www.baidu.com"),
        new HistoryEntry("Stack Overflow", "https://stackoverflow.com", "stackoverflow.com"),
        new HistoryEntry("YouTube", "https://www.youtube.com", "www.youtube.com"),
        new HistoryEntry("Wikipedia", "https://www.wikipedia.org", "www.wikipedia.org")
    };
    
    public HistoryMatcher(Context context) {
        this.context = context;
    }
    
    public List<Suggestion> findMatches(String query) {
        List<Suggestion> matches = new ArrayList<>();
        
        if (TextUtils.isEmpty(query)) {
            return matches;
        }
        
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        
        // 搜索历史记录
        for (HistoryEntry entry : MOCK_HISTORY) {
            if (isMatch(entry, lowerQuery)) {
                Suggestion historySuggestion = new Suggestion();
                historySuggestion.type = Suggestion.SuggestionType.HISTORY;
                historySuggestion.title = entry.title;
                historySuggestion.subtitle = entry.displayUrl;
                historySuggestion.url = entry.url;
                historySuggestion.priority = 2;
                matches.add(historySuggestion);
            }
        }
        
        // 按照匹配度排序（简单实现）
        matches.sort((s1, s2) -> {
            int score1 = calculateMatchScore(s1.title, lowerQuery);
            int score2 = calculateMatchScore(s2.title, lowerQuery);
            return Integer.compare(score2, score1); // 降序排列
        });
        
        // 限制历史记录建议数量
        return matches.subList(0, Math.min(matches.size(), 5));
    }
    
    private boolean isMatch(HistoryEntry entry, String query) {
        String lowerTitle = entry.title.toLowerCase(Locale.getDefault());
        String lowerUrl = entry.displayUrl.toLowerCase(Locale.getDefault());
        
        // 标题匹配
        if (lowerTitle.contains(query)) {
            return true;
        }
        
        // URL匹配
        if (lowerUrl.contains(query)) {
            return true;
        }
        
        // 域名前缀匹配（如 "goo" 匹配 "google.com"）
        if (lowerUrl.startsWith("www." + query) || 
            lowerUrl.startsWith(query + ".") ||
            lowerUrl.contains("." + query)) {
            return true;
        }
        
        return false;
    }
    
    private int calculateMatchScore(String text, String query) {
        String lowerText = text.toLowerCase(Locale.getDefault());
        
        if (lowerText.equals(query)) {
            return 100; // 完全匹配
        }
        
        if (lowerText.startsWith(query)) {
            return 90; // 前缀匹配
        }
        
        if (lowerText.contains(query)) {
            return 50; // 包含匹配
        }
        
        return 0;
    }
    
    private static class HistoryEntry {
        final String title;
        final String url;
        final String displayUrl;
        
        HistoryEntry(String title, String url, String displayUrl) {
            this.title = title;
            this.url = url;
            this.displayUrl = displayUrl;
        }
    }
}