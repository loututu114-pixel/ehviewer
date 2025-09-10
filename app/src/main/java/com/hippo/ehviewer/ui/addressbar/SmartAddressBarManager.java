package com.hippo.ehviewer.ui.addressbar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.HistoryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 智能地址栏管理器
 * 提供现代浏览器级别的地址栏体验
 * 
 * 核心特性：
 * 1. 智能搜索建议（搜索引擎补全）
 * 2. 历史记录匹配
 * 3. 书签快速访问
 * 4. 实时输入预测
 * 5. 频率排序优化
 */
public class SmartAddressBarManager {
    private static final String TAG = "SmartAddressBarManager";
    
    // 配置常量 - 优化后的建议数量
    private static final int MAX_SUGGESTIONS = 5;
    private static final int MAX_SEARCH_SUGGESTIONS = 1;  // 最多1个搜索建议
    private static final int SEARCH_DELAY_MS = 300;
    private static final int MIN_QUERY_LENGTH = 2;
    
    // SharedPreferences键值
    private static final String PREFS_NAME = "addressbar_settings";
    private static final String PREF_SUGGESTIONS_ENABLED = "suggestions_enabled";
    private static final String PREF_HISTORY_ENABLED = "history_enabled";
    private static final String PREF_BOOKMARKS_ENABLED = "bookmarks_enabled";
    
    // 组件引用
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final EditText mAddressBar;
    private final HistoryManager mHistoryManager;
    
    // 建议相关
    private PopupWindow mSuggestionsPopup;
    private ListView mSuggestionsListView;
    private AddressBarSuggestionAdapter mSuggestionAdapter;
    private final List<AddressBarSuggestion> mSuggestions = new ArrayList<>();
    
    // 异步处理
    private final ExecutorService mBackgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Runnable mSearchRunnable;
    
    // 状态配置
    private boolean mSuggestionsEnabled = true;
    private boolean mHistoryEnabled = true;
    private boolean mBookmarksEnabled = true;
    private boolean mIsShowing = false;
    
    // 用户交互状态跟踪
    private boolean mUserHasStartedTyping = false;
    private boolean mFirstFocusGain = true;
    
    // 监听器
    private AddressBarListener mListener;
    
    /**
     * 地址栏建议项
     */
    public static class AddressBarSuggestion {
        public static final int TYPE_SEARCH = 0;
        public static final int TYPE_HISTORY = 1;
        public static final int TYPE_BOOKMARK = 2;
        public static final int TYPE_URL = 3;
        
        public String title;
        public String url;
        public String description;
        public int type;
        public int frequency;
        public long lastAccessTime;
        
        public AddressBarSuggestion(String title, String url, int type) {
            this.title = title;
            this.url = url;
            this.type = type;
            this.frequency = 0;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public AddressBarSuggestion(String title, String url, String description, int type) {
            this(title, url, type);
            this.description = description;
        }
    }
    
    /**
     * 地址栏监听器
     */
    public interface AddressBarListener {
        void onSuggestionSelected(AddressBarSuggestion suggestion);
        void onUrlEntered(String url);
        void onSearchRequested(String query);
    }
    
    public SmartAddressBarManager(@NonNull Context context, @NonNull EditText addressBar) {
        mContext = context;
        mAddressBar = addressBar;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mHistoryManager = HistoryManager.getInstance(context);
        
        // 加载设置
        loadSettings();
        
        // 初始化建议系统
        initializeSuggestionSystem();
        
        // 设置文本监听器
        setupTextWatcher();
        
        Log.d(TAG, "SmartAddressBarManager initialized");
    }
    
    /**
     * 加载设置
     */
    private void loadSettings() {
        mSuggestionsEnabled = mPrefs.getBoolean(PREF_SUGGESTIONS_ENABLED, true);
        mHistoryEnabled = mPrefs.getBoolean(PREF_HISTORY_ENABLED, true);
        mBookmarksEnabled = mPrefs.getBoolean(PREF_BOOKMARKS_ENABLED, true);
        
        Log.d(TAG, "Settings loaded: suggestions=" + mSuggestionsEnabled + 
               ", history=" + mHistoryEnabled + ", bookmarks=" + mBookmarksEnabled);
    }
    
    /**
     * 初始化建议系统
     */
    private void initializeSuggestionSystem() {
        try {
            // 创建建议列表适配器
            mSuggestionAdapter = new AddressBarSuggestionAdapter(mContext, mSuggestions);
            
            // 创建弹出窗口
            View popupView = LayoutInflater.from(mContext).inflate(R.layout.popup_addressbar_suggestions, null);
            mSuggestionsListView = popupView.findViewById(R.id.suggestions_list);
            mSuggestionsListView.setAdapter(mSuggestionAdapter);
            
            mSuggestionsPopup = new PopupWindow(popupView, 
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT);
            mSuggestionsPopup.setOutsideTouchable(true);
            mSuggestionsPopup.setFocusable(false);
            
            // 设置建议点击监听器
            mSuggestionsListView.setOnItemClickListener((parent, view, position, id) -> {
                if (position < mSuggestions.size()) {
                    AddressBarSuggestion suggestion = mSuggestions.get(position);
                    handleSuggestionClick(suggestion);
                }
            });
            
            Log.d(TAG, "Suggestion system initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize suggestion system", e);
        }
    }
    
    /**
     * 设置文本监听器
     */
    private void setupTextWatcher() {
        mAddressBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                handleTextChanged(s.toString());
            }
        });
        
        // 处理焦点变化
        mAddressBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 获得焦点时，只在用户开始输入后才可能显示建议
                if (mFirstFocusGain) {
                    mFirstFocusGain = false;
                    mUserHasStartedTyping = false; // 重置输入状态
                } else {
                    // 非首次获得焦点，检查是否有内容且用户已开始输入
                    String currentText = mAddressBar.getText().toString().trim();
                    if (!currentText.isEmpty() && mUserHasStartedTyping) {
                        handleTextChanged(currentText);
                    }
                }
            } else {
                // 失去焦点时隐藏建议并重置状态
                hideSuggestions();
                mUserHasStartedTyping = false;
            }
        });
    }
    
    /**
     * 处理文本变化
     */
    private void handleTextChanged(String query) {
        // 标记用户已开始输入
        mUserHasStartedTyping = true;
        
        // 取消之前的搜索任务
        if (mSearchRunnable != null) {
            mMainHandler.removeCallbacks(mSearchRunnable);
        }
        
        // 如果查询太短或功能未启用，隐藏建议
        if (!mSuggestionsEnabled || query.trim().length() < MIN_QUERY_LENGTH) {
            hideSuggestions();
            return;
        }
        
        // 只有在用户主动输入且有焦点时才显示建议
        if (mUserHasStartedTyping && mAddressBar.hasFocus()) {
            // 延迟搜索以避免频繁请求
            mSearchRunnable = () -> searchSuggestions(query.trim());
            mMainHandler.postDelayed(mSearchRunnable, SEARCH_DELAY_MS);
        } else {
            hideSuggestions();
        }
    }
    
    /**
     * 搜索建议
     */
    private void searchSuggestions(String query) {
        mBackgroundExecutor.execute(() -> {
            try {
                List<AddressBarSuggestion> suggestions = new ArrayList<>();
                
                // 1. 优先：如果看起来像URL，添加直接访问选项（放在第一位）
                if (isUrl(query)) {
                    suggestions.add(new AddressBarSuggestion(
                        "访问 " + query, 
                        normalizeUrl(query), 
                        "直接访问此URL",
                        AddressBarSuggestion.TYPE_URL
                    ));
                }
                
                // 2. 添加历史记录匹配（优先级高）
                if (mHistoryEnabled) {
                    suggestions.addAll(searchHistorySuggestions(query));
                }
                
                // 3. 添加书签匹配  
                if (mBookmarksEnabled) {
                    suggestions.addAll(searchBookmarkSuggestions(query));
                }
                
                // 4. 最后：添加搜索建议（只有1个，优先级最低）
                if (mSuggestionsEnabled && !isUrl(query) && suggestions.size() < MAX_SUGGESTIONS) {
                    suggestions.addAll(generateSearchSuggestions(query));
                }
                
                // 限制建议数量并按相关性排序
                final List<AddressBarSuggestion> finalSuggestions = optimizeSuggestions(suggestions, query);
                
                // 在主线程中更新UI
                mMainHandler.post(() -> displaySuggestions(finalSuggestions));
                
            } catch (Exception e) {
                Log.w(TAG, "Error searching suggestions for: " + query, e);
            }
        });
    }
    
    /**
     * 搜索历史记录建议
     */
    private List<AddressBarSuggestion> searchHistorySuggestions(String query) {
        List<AddressBarSuggestion> suggestions = new ArrayList<>();
        
        try {
            // 从HistoryManager获取匹配的历史记录 - 优先显示更多历史记录
            List<com.hippo.ehviewer.client.data.HistoryInfo> historyItems = 
                mHistoryManager.searchHistory(query, MAX_SUGGESTIONS - MAX_SEARCH_SUGGESTIONS);
            
            for (com.hippo.ehviewer.client.data.HistoryInfo item : historyItems) {
                suggestions.add(new AddressBarSuggestion(
                    item.title != null ? item.title : item.url,
                    item.url,
                    "历史记录 • " + formatTime(item.visitTime),
                    AddressBarSuggestion.TYPE_HISTORY
                ));
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to search history suggestions", e);
        }
        
        return suggestions;
    }
    
    /**
     * 搜索书签建议
     */
    private List<AddressBarSuggestion> searchBookmarkSuggestions(String query) {
        List<AddressBarSuggestion> suggestions = new ArrayList<>();
        
        try {
            // 这里需要集成BookmarkManager的搜索功能
            // 暂时添加示例书签建议
            if (query.toLowerCase().contains("google")) {
                suggestions.add(new AddressBarSuggestion(
                    "Google 搜索", 
                    "https://www.google.com",
                    "书签 • 搜索引擎",
                    AddressBarSuggestion.TYPE_BOOKMARK
                ));
            }
            
            if (query.toLowerCase().contains("github")) {
                suggestions.add(new AddressBarSuggestion(
                    "GitHub", 
                    "https://github.com",
                    "书签 • 代码托管",
                    AddressBarSuggestion.TYPE_BOOKMARK
                ));
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to search bookmark suggestions", e);
        }
        
        return suggestions;
    }
    
    /**
     * 生成搜索建议
     */
    private List<AddressBarSuggestion> generateSearchSuggestions(String query) {
        List<AddressBarSuggestion> suggestions = new ArrayList<>();
        
        try {
            // 只添加1个默认搜索引擎建议
            if (suggestions.size() < MAX_SEARCH_SUGGESTIONS) {
                suggestions.add(new AddressBarSuggestion(
                    query,
                    "https://www.google.com/search?q=" + java.net.URLEncoder.encode(query, "UTF-8"),
                    "Google 搜索",
                    AddressBarSuggestion.TYPE_SEARCH
                ));
            }
            
            // 不再添加额外的搜索建议，只保留一个默认的
            // 这样可以为链接地址补全留出更多空间
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to generate search suggestions", e);
        }
        
        return suggestions;
    }
    
    /**
     * 生成常见搜索建议
     */
    private String[] generateCommonSuggestions(String query) {
        // 这里可以集成真实的搜索建议API，目前使用简单的本地生成
        List<String> suggestions = new ArrayList<>();
        
        // 根据查询生成相关建议
        if (query.toLowerCase().startsWith("android")) {
            suggestions.add("android开发教程");
            suggestions.add("android studio");
            suggestions.add("android系统");
        } else if (query.toLowerCase().startsWith("java")) {
            suggestions.add("java编程");
            suggestions.add("java教程");
            suggestions.add("java面试题");
        } else {
            // 通用建议
            suggestions.add(query + "是什么");
            suggestions.add(query + "怎么用");
            suggestions.add(query + "教程");
        }
        
        return suggestions.toArray(new String[0]);
    }
    
    /**
     * 优化建议列表
     */
    private List<AddressBarSuggestion> optimizeSuggestions(List<AddressBarSuggestion> suggestions, String query) {
        // 按相关性和频率排序
        suggestions.sort((s1, s2) -> {
            // 优先级：完全匹配 > 开头匹配 > 包含匹配
            int score1 = calculateRelevanceScore(s1, query);
            int score2 = calculateRelevanceScore(s2, query);
            
            if (score1 != score2) {
                return Integer.compare(score2, score1); // 降序
            }
            
            // 如果相关性相同，按频率和时间排序
            if (s1.frequency != s2.frequency) {
                return Integer.compare(s2.frequency, s1.frequency);
            }
            
            return Long.compare(s2.lastAccessTime, s1.lastAccessTime);
        });
        
        // 限制结果数量
        if (suggestions.size() > MAX_SUGGESTIONS) {
            suggestions = suggestions.subList(0, MAX_SUGGESTIONS);
        }
        
        return suggestions;
    }
    
    /**
     * 计算相关性分数
     */
    private int calculateRelevanceScore(AddressBarSuggestion suggestion, String query) {
        String title = suggestion.title.toLowerCase();
        String url = suggestion.url.toLowerCase();
        query = query.toLowerCase();
        
        int score = 0;
        
        // 标题匹配
        if (title.equals(query)) score += 100;
        else if (title.startsWith(query)) score += 50;
        else if (title.contains(query)) score += 25;
        
        // URL匹配
        if (url.contains(query)) score += 10;
        
        // 类型加权
        switch (suggestion.type) {
            case AddressBarSuggestion.TYPE_HISTORY:
                score += 20; // 历史记录优先级较高
                break;
            case AddressBarSuggestion.TYPE_BOOKMARK:
                score += 30; // 书签优先级最高
                break;
            case AddressBarSuggestion.TYPE_URL:
                score += 40; // 直接URL访问优先级很高
                break;
        }
        
        return score;
    }
    
    /**
     * 显示建议
     */
    private void displaySuggestions(List<AddressBarSuggestion> suggestions) {
        try {
            if (suggestions.isEmpty() || !mSuggestionsEnabled) {
                hideSuggestions();
                return;
            }
            
            // 更新建议列表
            mSuggestions.clear();
            mSuggestions.addAll(suggestions);
            mSuggestionAdapter.notifyDataSetChanged();
            
            // 显示弹出窗口
            if (!mIsShowing) {
                mSuggestionsPopup.showAsDropDown(mAddressBar);
                mIsShowing = true;
            }
            
            Log.d(TAG, "Displayed " + suggestions.size() + " suggestions");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to display suggestions", e);
        }
    }
    
    /**
     * 隐藏建议
     */
    public void hideSuggestions() {
        if (mIsShowing && mSuggestionsPopup != null) {
            mSuggestionsPopup.dismiss();
            mIsShowing = false;
        }
    }
    
    /**
     * 处理建议点击
     */
    private void handleSuggestionClick(AddressBarSuggestion suggestion) {
        // 更新频率统计
        suggestion.frequency++;
        suggestion.lastAccessTime = System.currentTimeMillis();
        
        // 隐藏建议
        hideSuggestions();
        
        // 更新地址栏文本
        mAddressBar.setText(suggestion.url);
        
        // 通知监听器
        if (mListener != null) {
            mListener.onSuggestionSelected(suggestion);
        }
        
        Log.d(TAG, "Suggestion selected: " + suggestion.title);
    }
    
    /**
     * 检查是否为URL
     */
    private boolean isUrl(String input) {
        return input.startsWith("http://") || 
               input.startsWith("https://") || 
               input.startsWith("ftp://") ||
               input.contains(".") && !input.contains(" ");
    }
    
    /**
     * 规范化URL
     */
    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }
    
    /**
     * 格式化时间
     */
    private String formatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60 * 1000) {
            return "刚刚";
        } else if (diff < 60 * 60 * 1000) {
            return (diff / (60 * 1000)) + "分钟前";
        } else if (diff < 24 * 60 * 60 * 1000) {
            return (diff / (60 * 60 * 1000)) + "小时前";
        } else {
            return (diff / (24 * 60 * 60 * 1000)) + "天前";
        }
    }
    
    /**
     * 设置地址栏监听器
     */
    public void setAddressBarListener(AddressBarListener listener) {
        mListener = listener;
    }
    
    /**
     * 启用/禁用搜索建议
     */
    public void setSuggestionsEnabled(boolean enabled) {
        if (mSuggestionsEnabled != enabled) {
            mSuggestionsEnabled = enabled;
            mPrefs.edit().putBoolean(PREF_SUGGESTIONS_ENABLED, enabled).apply();
            
            if (!enabled) {
                hideSuggestions();
            }
            
            Log.d(TAG, "Suggestions " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 启用/禁用历史记录建议
     */
    public void setHistoryEnabled(boolean enabled) {
        if (mHistoryEnabled != enabled) {
            mHistoryEnabled = enabled;
            mPrefs.edit().putBoolean(PREF_HISTORY_ENABLED, enabled).apply();
            Log.d(TAG, "History suggestions " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 启用/禁用书签建议
     */
    public void setBookmarksEnabled(boolean enabled) {
        if (mBookmarksEnabled != enabled) {
            mBookmarksEnabled = enabled;
            mPrefs.edit().putBoolean(PREF_BOOKMARKS_ENABLED, enabled).apply();
            Log.d(TAG, "Bookmark suggestions " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 获取设置统计信息
     */
    public String getAddressBarStats() {
        return String.format("搜索建议: %s\n历史记录: %s\n书签建议: %s\n建议数量: %d",
                           mSuggestionsEnabled ? "启用" : "禁用",
                           mHistoryEnabled ? "启用" : "禁用",
                           mBookmarksEnabled ? "启用" : "禁用",
                           MAX_SUGGESTIONS);
    }
    
    /**
     * 清理资源
     */
    public void destroy() {
        hideSuggestions();
        
        if (mSearchRunnable != null) {
            mMainHandler.removeCallbacks(mSearchRunnable);
        }
        
        mBackgroundExecutor.shutdown();
        
        Log.d(TAG, "SmartAddressBarManager destroyed");
    }
}