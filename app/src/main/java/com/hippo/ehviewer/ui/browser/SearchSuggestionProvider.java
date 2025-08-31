package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchSuggestionProvider {
    private static final String TAG = "SearchSuggestionProvider";
    private static final String PREF_NAME = "browser_history";
    private static final String KEY_HISTORY = "search_history";
    private static final String KEY_BOOKMARKS = "bookmarks";
    
    private Context context;
    private SharedPreferences prefs;
    private SuggestionCallback callback;
    
    public interface SuggestionCallback {
        void onSuggestionsReady(List<SearchSuggestionAdapter.SearchSuggestion> suggestions);
    }
    
    public SearchSuggestionProvider(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void setSuggestionCallback(SuggestionCallback callback) {
        this.callback = callback;
    }
    
    // 获取搜索建议
    public void getSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (callback != null) {
                callback.onSuggestionsReady(new ArrayList<>());
            }
            return;
        }
        
        new SuggestionTask().execute(query);
    }
    
    // 添加到历史记录
    public void addToHistory(String title, String url) {
        Set<String> history = prefs.getStringSet(KEY_HISTORY, new HashSet<>());
        history.add(title + "|" + url);
        
        // 限制历史记录数量
        if (history.size() > 100) {
            List<String> list = new ArrayList<>(history);
            history = new HashSet<>(list.subList(list.size() - 100, list.size()));
        }
        
        prefs.edit().putStringSet(KEY_HISTORY, history).apply();
    }
    
    // 添加书签
    public void addBookmark(String title, String url) {
        Set<String> bookmarks = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
        bookmarks.add(title + "|" + url);
        prefs.edit().putStringSet(KEY_BOOKMARKS, bookmarks).apply();
    }
    
    // 移除书签
    public void removeBookmark(String url) {
        Set<String> bookmarks = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
        bookmarks.removeIf(bookmark -> bookmark.contains("|" + url));
        prefs.edit().putStringSet(KEY_BOOKMARKS, bookmarks).apply();
    }
    
    // 检查是否已收藏
    public boolean isBookmarked(String url) {
        Set<String> bookmarks = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
        for (String bookmark : bookmarks) {
            if (bookmark.contains("|" + url)) {
                return true;
            }
        }
        return false;
    }
    
    // 清除历史记录
    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
    
    // 清除所有数据
    public void clearAll() {
        prefs.edit().clear().apply();
    }
    
    private class SuggestionTask extends AsyncTask<String, Void, List<SearchSuggestionAdapter.SearchSuggestion>> {
        
        @Override
        protected List<SearchSuggestionAdapter.SearchSuggestion> doInBackground(String... params) {
            String query = params[0].toLowerCase();
            List<SearchSuggestionAdapter.SearchSuggestion> suggestions = new ArrayList<>();
            
            // 1. 从历史记录中查找
            Set<String> history = prefs.getStringSet(KEY_HISTORY, new HashSet<>());
            for (String item : history) {
                String[] parts = item.split("\\|");
                if (parts.length == 2) {
                    String title = parts[0];
                    String url = parts[1];
                    if (title.toLowerCase().contains(query) || url.toLowerCase().contains(query)) {
                        suggestions.add(new SearchSuggestionAdapter.SearchSuggestion(
                            title, url, url, 
                            SearchSuggestionAdapter.SearchSuggestion.Type.HISTORY
                        ));
                    }
                }
            }
            
            // 2. 从书签中查找
            Set<String> bookmarks = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
            for (String item : bookmarks) {
                String[] parts = item.split("\\|");
                if (parts.length == 2) {
                    String title = parts[0];
                    String url = parts[1];
                    if (title.toLowerCase().contains(query) || url.toLowerCase().contains(query)) {
                        suggestions.add(new SearchSuggestionAdapter.SearchSuggestion(
                            title, url, url,
                            SearchSuggestionAdapter.SearchSuggestion.Type.BOOKMARK
                        ));
                    }
                }
            }
            
            // 3. 检查是否为URL
            if (isValidUrl(query)) {
                String url = query;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                suggestions.add(new SearchSuggestionAdapter.SearchSuggestion(
                    "访问 " + query, url, url,
                    SearchSuggestionAdapter.SearchSuggestion.Type.URL
                ));
            }
            
            // 4. 获取搜索建议（使用百度搜索建议API）
            try {
                List<String> searchSuggestions = getSearchSuggestions(query);
                for (String suggestion : searchSuggestions) {
                    suggestions.add(new SearchSuggestionAdapter.SearchSuggestion(
                        suggestion, "搜索", 
                        "https://www.baidu.com/s?wd=" + URLEncoder.encode(suggestion, "UTF-8"),
                        SearchSuggestionAdapter.SearchSuggestion.Type.SEARCH
                    ));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get search suggestions", e);
            }
            
            // 限制建议数量
            if (suggestions.size() > 10) {
                suggestions = suggestions.subList(0, 10);
            }
            
            return suggestions;
        }
        
        @Override
        protected void onPostExecute(List<SearchSuggestionAdapter.SearchSuggestion> suggestions) {
            if (callback != null) {
                callback.onSuggestionsReady(suggestions);
            }
        }
        
        private boolean isValidUrl(String text) {
            return text.contains(".") && !text.contains(" ") && 
                   (text.startsWith("http://") || text.startsWith("https://") || 
                    text.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*$"));
        }
        
        private List<String> getSearchSuggestions(String query) {
            List<String> suggestions = new ArrayList<>();
            
            try {
                // 使用百度搜索建议API
                String apiUrl = "https://suggestion.baidu.com/su?wd=" + 
                               URLEncoder.encode(query, "UTF-8") + "&cb=";
                
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "GBK")
                );
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // 解析JSONP响应
                String jsonStr = response.toString();
                int start = jsonStr.indexOf("[");
                int end = jsonStr.lastIndexOf("]") + 1;
                
                if (start >= 0 && end > start) {
                    jsonStr = jsonStr.substring(start, end);
                    JSONArray jsonArray = new JSONArray(jsonStr);
                    
                    for (int i = 0; i < jsonArray.length() && i < 5; i++) {
                        suggestions.add(jsonArray.getString(i));
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting search suggestions", e);
            }
            
            return suggestions;
        }
    }
}