package com.hippo.ehviewer.ui;

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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.R;

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
public class SmartAddressBar {
    private static final String TAG = "SmartAddressBar";

    // 配置常量 - 优化后的建议数量
    private static final int MAX_SUGGESTIONS = 8;
    private static final int MAX_SEARCH_SUGGESTIONS = 2;
    private static final int SEARCH_DELAY_MS = 300;
    private static final int MIN_QUERY_LENGTH = 2;

    // SharedPreferences键值
    private static final String PREFS_NAME = "addressbar_settings";
    private static final String PREF_SUGGESTIONS_ENABLED = "suggestions_enabled";
    private static final String PREF_HISTORY_ENABLED = "history_enabled";
    private static final String PREF_BOOKMARKS_ENABLED = "bookmarks_enabled";

    // 搜索引擎配置
    private static final String[] SEARCH_ENGINES = {
        "Google", "Bing", "百度", "DuckDuckGo", "Yahoo", "Sogou"
    };
    private static final String[] SEARCH_URLS = {
        "https://www.google.com/search?q=",
        "https://www.bing.com/search?q=",
        "https://www.baidu.com/s?wd=",
        "https://duckduckgo.com/?q=",
        "https://search.yahoo.com/search?p=",
        "https://www.sogou.com/web?query="
    };

    // 组件引用
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final EditText mAddressBar;
    private final YCWebViewActivity mActivity;

    // 建议相关
    private PopupWindow mSuggestionsPopup;
    private ListView mSuggestionsListView;
    private SmartSuggestionAdapter mSuggestionAdapter;
    private final List<SmartSuggestion> mSuggestions = new ArrayList<>();

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
    private boolean mSuggestionsDisabled = false; // 临时禁用建议

    // 监听器
    private SmartAddressBarListener mListener;

    /**
     * 智能建议项
     */
    public static class SmartSuggestion {
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

        public SmartSuggestion(String title, String url, int type) {
            this.title = title;
            this.url = url;
            this.type = type;
            this.frequency = 1;
            this.lastAccessTime = System.currentTimeMillis();

            // 设置描述
            switch (type) {
                case TYPE_SEARCH:
                    this.description = "在 " + title + " 中搜索";
                    break;
                case TYPE_HISTORY:
                    this.description = "历史记录";
                    break;
                case TYPE_BOOKMARK:
                    this.description = "书签";
                    break;
                case TYPE_URL:
                    this.description = url;
                    break;
            }
        }

        public String getDisplayText() {
            return title + " - " + description;
        }
    }

    /**
     * 智能建议适配器
     */
    private static class SmartSuggestionAdapter extends ArrayAdapter<SmartSuggestion> {
        private final Context mContext;

        public SmartSuggestionAdapter(Context context, List<SmartSuggestion> suggestions) {
            super(context, 0, suggestions);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(
                    android.R.layout.simple_list_item_2, parent, false);

                // 设置每项的高度和内边距
                convertView.setPadding(16, 12, 16, 12);
                convertView.setMinimumHeight(48); // 最小高度
            }

            SmartSuggestion suggestion = getItem(position);
            if (suggestion != null) {
                TextView titleView = convertView.findViewById(android.R.id.text1);
                TextView descView = convertView.findViewById(android.R.id.text2);

                // 设置文本
                titleView.setText(suggestion.title);
                descView.setText(suggestion.description);

                // 设置字体大小和样式
                titleView.setTextSize(16);
                titleView.setTypeface(null, android.graphics.Typeface.BOLD);
                descView.setTextSize(12);

                // 根据类型设置不同的颜色
                int titleColor, descColor = 0xFF666666; // 默认灰色

                switch (suggestion.type) {
                    case SmartSuggestion.TYPE_SEARCH:
                        titleColor = 0xFF4285F4; // Google Blue
                        break;
                    case SmartSuggestion.TYPE_HISTORY:
                        titleColor = 0xFF34A853; // Green
                        break;
                    case SmartSuggestion.TYPE_BOOKMARK:
                        titleColor = 0xFFEA4335; // Red
                        break;
                    case SmartSuggestion.TYPE_URL:
                        titleColor = 0xFFFBBC04; // Yellow
                        break;
                    default:
                        titleColor = 0xFF333333; // 默认黑色
                        break;
                }

                titleView.setTextColor(titleColor);
                descView.setTextColor(descColor);

                // 设置点击效果
                convertView.setBackgroundResource(android.R.drawable.list_selector_background);
            }

            return convertView;
        }
    }

    /**
     * 构造函数
     */
    public SmartAddressBar(Context context, EditText addressBar, YCWebViewActivity activity) {
        mContext = context;
        mAddressBar = addressBar;
        mActivity = activity;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 加载设置
        loadSettings();

        // 初始化建议弹窗
        initSuggestionsPopup();

        // 设置输入监听器
        setupInputListeners();
    }

    /**
     * 初始化建议弹窗
     */
    private void initSuggestionsPopup() {
        mSuggestionsListView = new ListView(mContext);
        mSuggestionAdapter = new SmartSuggestionAdapter(mContext, mSuggestions);
        mSuggestionsListView.setAdapter(mSuggestionAdapter);

        // 设置ListView样式
        mSuggestionsListView.setDivider(new android.graphics.drawable.ColorDrawable(0xFFE0E0E0));
        mSuggestionsListView.setDividerHeight(1);
        mSuggestionsListView.setBackgroundColor(0xFFFFFFFF); // 白色背景
        mSuggestionsListView.setPadding(0, 8, 0, 8); // 添加内边距

        mSuggestionsPopup = new PopupWindow(mContext);
        mSuggestionsPopup.setContentView(mSuggestionsListView);
        mSuggestionsPopup.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        mSuggestionsPopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mSuggestionsPopup.setFocusable(false);

        // 设置有阴影的背景
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(0xFFFFFFFF); // 白色背景
        background.setCornerRadius(8); // 圆角
        background.setStroke(1, 0xFFE0E0E0); // 边框

        // 添加阴影效果
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mSuggestionsPopup.setElevation(8);
        }

        mSuggestionsPopup.setBackgroundDrawable(background);

        // 设置动画
        mSuggestionsPopup.setAnimationStyle(android.R.style.Animation_Dialog);

        // 设置点击监听器
        mSuggestionsListView.setOnItemClickListener((parent, view, position, id) -> {
            SmartSuggestion suggestion = mSuggestions.get(position);
            if (suggestion != null) {
                onSuggestionSelected(suggestion);
                hideSuggestions();
            }
        });
    }

    /**
     * 设置输入监听器
     */
    private void setupInputListeners() {
        mAddressBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 如果是用户输入的字符，标记用户已开始输入
                if (count > 0) {
                    mUserHasStartedTyping = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();

                // 只有在建议未被禁用且用户已开始输入的情况下才显示建议
                if (query.length() >= MIN_QUERY_LENGTH && mSuggestionsEnabled && !mSuggestionsDisabled && mUserHasStartedTyping) {
                    showSuggestionsForQuery(query);
                } else {
                    hideSuggestions();
                }
            }
        });

        mAddressBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && mFirstFocusGain) {
                mFirstFocusGain = false;
                // 首次获得焦点时不显示建议
                return;
            }

            if (hasFocus && !mAddressBar.getText().toString().trim().isEmpty() && !mSuggestionsDisabled && mUserHasStartedTyping) {
                String query = mAddressBar.getText().toString().trim();
                if (query.length() >= MIN_QUERY_LENGTH) {
                    showSuggestionsForQuery(query);
                }
            } else {
                hideSuggestions();
            }
        });
    }

    /**
     * 显示查询建议
     */
    private void showSuggestionsForQuery(String query) {
        Log.d(TAG, "SmartAddressBar: 处理查询 - " + query);
        if (mSearchRunnable != null) {
            mMainHandler.removeCallbacks(mSearchRunnable);
        }

        mSearchRunnable = () -> {
            mBackgroundExecutor.execute(() -> {
                List<SmartSuggestion> suggestions = generateSuggestions(query);
                Log.d(TAG, "SmartAddressBar: 生成了 " + suggestions.size() + " 个建议");
                mMainHandler.post(() -> {
                    updateSuggestions(suggestions);
                });
            });
        };

        mMainHandler.postDelayed(mSearchRunnable, SEARCH_DELAY_MS);
    }

    /**
     * 生成智能建议
     */
    private List<SmartSuggestion> generateSuggestions(String query) {
        List<SmartSuggestion> suggestions = new ArrayList<>();

        // 🎯 1. 智能域名补全 - 最高优先级
        SmartSuggestion domainSuggestion = generateDomainSuggestion(query);
        if (domainSuggestion != null) {
            suggestions.add(domainSuggestion);
        }

        // 2. 添加搜索引擎建议
        if (mSuggestionsEnabled) {
            for (int i = 0; i < Math.min(SEARCH_ENGINES.length, MAX_SEARCH_SUGGESTIONS); i++) {
                suggestions.add(new SmartSuggestion(SEARCH_ENGINES[i], SEARCH_URLS[i] + query, SmartSuggestion.TYPE_SEARCH));
            }
        }

        // 3. 检查是否是URL
        if (query.contains(".") && !query.contains(" ")) {
            suggestions.add(new SmartSuggestion(query, query, SmartSuggestion.TYPE_URL));
        }

        // 4. 添加历史记录匹配
        if (mHistoryEnabled) {
            List<String> history = mActivity.getHistory();
            for (String historyItem : history) {
                if (historyItem.toLowerCase().contains(query.toLowerCase())) {
                    suggestions.add(new SmartSuggestion(historyItem, historyItem, SmartSuggestion.TYPE_HISTORY));
                    if (suggestions.size() >= MAX_SUGGESTIONS) break;
                }
            }
        }

        // 5. 添加书签匹配
        if (mBookmarksEnabled) {
            List<String> bookmarks = mActivity.getBookmarks();
            for (String bookmark : bookmarks) {
                if (bookmark.toLowerCase().contains(query.toLowerCase())) {
                    String title = mActivity.getBookmarkTitle(bookmark);
                    suggestions.add(new SmartSuggestion(title != null ? title : bookmark, bookmark, SmartSuggestion.TYPE_BOOKMARK));
                    if (suggestions.size() >= MAX_SUGGESTIONS) break;
                }
            }
        }

        return suggestions;
    }

    /**
     * 🎯 生成智能域名补全建议
     * 支持：goog -> www.google.com, xvide -> xvideos.com 等
     */
    private SmartSuggestion generateDomainSuggestion(String query) {
        if (query == null || query.length() < 2) {
            return null;
        }

        String lowerQuery = query.toLowerCase();
        
        // 常用网站快速补全映射
        java.util.Map<String, String> quickDomains = new java.util.HashMap<>();
        quickDomains.put("goog", "www.google.com");
        quickDomains.put("google", "www.google.com");
        quickDomains.put("xvide", "xvideos.com");
        quickDomains.put("xvideos", "xvideos.com");
        quickDomains.put("youtu", "www.youtube.com");
        quickDomains.put("youtube", "www.youtube.com");
        quickDomains.put("face", "www.facebook.com");
        quickDomains.put("facebook", "www.facebook.com");
        quickDomains.put("twit", "www.twitter.com");
        quickDomains.put("twitter", "www.twitter.com");
        quickDomains.put("insta", "www.instagram.com");
        quickDomains.put("instagram", "www.instagram.com");
        quickDomains.put("redd", "www.reddit.com");
        quickDomains.put("reddit", "www.reddit.com");
        quickDomains.put("wiki", "www.wikipedia.org");
        quickDomains.put("wikipedia", "www.wikipedia.org");
        quickDomains.put("amaz", "www.amazon.com");
        quickDomains.put("amazon", "www.amazon.com");
        quickDomains.put("netfl", "www.netflix.com");
        quickDomains.put("netflix", "www.netflix.com");
        quickDomains.put("gith", "www.github.com");
        quickDomains.put("github", "www.github.com");
        quickDomains.put("stack", "stackoverflow.com");
        quickDomains.put("stackoverflow", "stackoverflow.com");
        quickDomains.put("baidu", "www.baidu.com");
        quickDomains.put("bai", "www.baidu.com");
        quickDomains.put("taobao", "www.taobao.com");
        quickDomains.put("tao", "www.taobao.com");
        quickDomains.put("jd", "www.jd.com");
        quickDomains.put("jingdong", "www.jd.com");
        quickDomains.put("weibo", "www.weibo.com");
        quickDomains.put("weib", "www.weibo.com");
        quickDomains.put("zhihu", "www.zhihu.com");
        quickDomains.put("zhi", "www.zhihu.com");
        quickDomains.put("bili", "www.bilibili.com");
        quickDomains.put("bilibili", "www.bilibili.com");
        quickDomains.put("douyin", "www.douyin.com");
        quickDomains.put("douy", "www.douyin.com");
        quickDomains.put("tiktok", "www.tiktok.com");
        quickDomains.put("tik", "www.tiktok.com");

        // 精确匹配
        if (quickDomains.containsKey(lowerQuery)) {
            String fullDomain = quickDomains.get(lowerQuery);
            return new SmartSuggestion("🌐 " + fullDomain, "https://" + fullDomain, SmartSuggestion.TYPE_URL);
        }

        // 模糊匹配 - 查找包含输入内容的域名
        for (java.util.Map.Entry<String, String> entry : quickDomains.entrySet()) {
            if (entry.getKey().startsWith(lowerQuery) && lowerQuery.length() >= 3) {
                String fullDomain = entry.getValue();
                return new SmartSuggestion("🌐 " + fullDomain, "https://" + fullDomain, SmartSuggestion.TYPE_URL);
            }
        }

        // 自动添加协议前缀
        if (!lowerQuery.startsWith("http://") && !lowerQuery.startsWith("https://") && 
            !lowerQuery.contains(".") && lowerQuery.length() >= 3) {
            
            // 检查是否可能是域名（不包含空格）
            if (!lowerQuery.contains(" ")) {
                String suggestedUrl = "https://www." + lowerQuery + ".com";
                return new SmartSuggestion("🌐 " + suggestedUrl, suggestedUrl, SmartSuggestion.TYPE_URL);
            }
        }

        return null;
    }

    /**
     * 更新建议列表
     */
    private void updateSuggestions(List<SmartSuggestion> suggestions) {
        mSuggestions.clear();
        mSuggestions.addAll(suggestions);
        mSuggestionAdapter.notifyDataSetChanged();

        if (!suggestions.isEmpty()) {
            showSuggestions();
        } else {
            hideSuggestions();
        }
    }

    /**
     * 显示建议弹窗
     */
    private void showSuggestions() {
        if (!mIsShowing && !mSuggestions.isEmpty()) {
            try {
                Log.d(TAG, "SmartAddressBar: 显示建议弹窗，包含 " + mSuggestions.size() + " 个建议");
                mSuggestionsPopup.showAsDropDown(mAddressBar, 0, 0);
                mIsShowing = true;
            } catch (Exception e) {
                Log.e(TAG, "SmartAddressBar: 显示建议弹窗失败", e);
            }
        }
    }

    /**
     * 隐藏建议弹窗
     */
    public void hideSuggestions() {
        if (mIsShowing) {
            Log.d(TAG, "SmartAddressBar: 隐藏建议弹窗");
            mSuggestionsPopup.dismiss();
            mIsShowing = false;
        }
    }

    /**
     * 处理建议选择
     */
    private void onSuggestionSelected(SmartSuggestion suggestion) {
        switch (suggestion.type) {
            case SmartSuggestion.TYPE_SEARCH:
                mAddressBar.setText(suggestion.url);
                mAddressBar.setSelection(mAddressBar.getText().length());
                if (mListener != null) {
                    mListener.onSearchSelected(suggestion.url);
                }
                break;
            case SmartSuggestion.TYPE_HISTORY:
            case SmartSuggestion.TYPE_BOOKMARK:
            case SmartSuggestion.TYPE_URL:
                if (mListener != null) {
                    mListener.onUrlSelected(suggestion.url);
                }
                break;
        }
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        mSuggestionsEnabled = mPrefs.getBoolean(PREF_SUGGESTIONS_ENABLED, true);
        mHistoryEnabled = mPrefs.getBoolean(PREF_HISTORY_ENABLED, true);
        mBookmarksEnabled = mPrefs.getBoolean(PREF_BOOKMARKS_ENABLED, true);
    }

    /**
     * 保存设置
     */
    private void saveSettings() {
        mPrefs.edit()
            .putBoolean(PREF_SUGGESTIONS_ENABLED, mSuggestionsEnabled)
            .putBoolean(PREF_HISTORY_ENABLED, mHistoryEnabled)
            .putBoolean(PREF_BOOKMARKS_ENABLED, mBookmarksEnabled)
            .apply();
    }

    /**
     * 设置监听器
     */
    public void setListener(SmartAddressBarListener listener) {
        mListener = listener;
    }

    /**
     * 临时禁用建议显示
     */
    public void disableSuggestions() {
        mSuggestionsDisabled = true;
        hideSuggestions();
    }

    /**
     * 重新启用建议显示
     */
    public void enableSuggestions() {
        mSuggestionsDisabled = false;
    }

    /**
     * 重置用户输入状态
     */
    public void resetUserTypingState() {
        mUserHasStartedTyping = false;
    }

    /**
     * 检查用户是否已开始输入
     */
    public boolean hasUserStartedTyping() {
        return mUserHasStartedTyping;
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        hideSuggestions();
        if (mSearchRunnable != null) {
            mMainHandler.removeCallbacks(mSearchRunnable);
        }
        mBackgroundExecutor.shutdown();
    }

    /**
     * 智能地址栏监听器
     */
    public interface SmartAddressBarListener {
        void onUrlSelected(String url);
        void onSearchSelected(String searchUrl);
    }
}
