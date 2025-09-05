package com.hippo.ehviewer.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.HistoryManager;
import com.hippo.ehviewer.client.SearchConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索建议管理器
 * 提供智能搜索建议功能，包含历史记录、书签、搜索建议等
 */
public class SearchSuggestionsManager {

    private static final String TAG = "SearchSuggestionsManager";

    private final Context mContext;
    private final SearchConfigManager mSearchConfigManager;
    private final HistoryManager mHistoryManager;

    private PopupWindow mSuggestionsPopup;
    private RecyclerView mSuggestionsRecyclerView;
    private SuggestionsAdapter mAdapter;

    private OnSuggestionClickListener mListener;
    private String mCurrentQuery = "";

    // 搜索建议类型
    public enum SuggestionType {
        HISTORY,        // 历史记录
        BOOKMARK,       // 书签
        SEARCH_SUGGESTION, // 搜索引擎建议
        QUICK_ACTION,   // 快捷操作
        NEW_TAB_ACTION  // 新标签页操作
    }

    // 搜索建议项
    public static class SuggestionItem {
        public final SuggestionType type;
        public final String title;
        public final String subtitle;
        public final String url;
        public final int iconResId;
        public final Object data; // 额外数据

        public SuggestionItem(SuggestionType type, String title, String subtitle,
                            String url, int iconResId, Object data) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.url = url;
            this.iconResId = iconResId;
            this.data = data;
        }

        public SuggestionItem(SuggestionType type, String title, String subtitle,
                            String url, int iconResId) {
            this(type, title, subtitle, url, iconResId, null);
        }
    }

    public interface OnSuggestionClickListener {
        void onSuggestionClick(SuggestionItem item);
        void onSuggestionLongClick(SuggestionItem item);
    }

    public SearchSuggestionsManager(Context context) {
        this.mContext = context.getApplicationContext();
        this.mSearchConfigManager = SearchConfigManager.getInstance(context);
        this.mHistoryManager = HistoryManager.getInstance(context);
        initializePopup();
    }

    private void initializePopup() {
        View popupView = LayoutInflater.from(mContext).inflate(
            R.layout.search_suggestions_popup, null);

        mSuggestionsRecyclerView = popupView.findViewById(R.id.suggestions_recycler_view);
        mSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mAdapter = new SuggestionsAdapter();
        mSuggestionsRecyclerView.setAdapter(mAdapter);

        mSuggestionsPopup = new PopupWindow(popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        mSuggestionsPopup.setFocusable(false);
        mSuggestionsPopup.setOutsideTouchable(true);
        mSuggestionsPopup.setBackgroundDrawable(null);
    }

    /**
     * 显示搜索建议
     */
    public void showSuggestions(View anchor, String query) {
        mCurrentQuery = query != null ? query : "";

        // 生成建议列表
        List<SuggestionItem> suggestions = generateSuggestions(mCurrentQuery);

        if (suggestions.isEmpty()) {
            hideSuggestions();
            return;
        }

        mAdapter.setSuggestions(suggestions);
        mAdapter.notifyDataSetChanged();

        // 设置最大高度
        int maxHeight = (int) (mContext.getResources().getDisplayMetrics().heightPixels * 0.4);
        ViewGroup.LayoutParams params = mSuggestionsRecyclerView.getLayoutParams();
        if (params != null) {
            params.height = maxHeight;
            mSuggestionsRecyclerView.setLayoutParams(params);
        } else {
            mSuggestionsRecyclerView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight));
        }

        if (!mSuggestionsPopup.isShowing()) {
            mSuggestionsPopup.showAsDropDown(anchor, 0, 8);
        }
    }

    /**
     * 隐藏搜索建议
     */
    public void hideSuggestions() {
        if (mSuggestionsPopup.isShowing()) {
            mSuggestionsPopup.dismiss();
        }
    }

    /**
     * 生成搜索建议列表
     */
    private List<SuggestionItem> generateSuggestions(String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        // 添加历史记录建议
        if (query.isEmpty()) {
            suggestions.addAll(getHistorySuggestions());
        } else {
            suggestions.addAll(getFilteredHistorySuggestions(query));
        }

        // 添加书签建议
        if (!query.isEmpty()) {
            suggestions.addAll(getBookmarkSuggestions(query));
        }

        // 添加搜索建议
        if (!query.isEmpty()) {
            suggestions.addAll(getSearchSuggestions(query));
            suggestions.addAll(getNewTabSuggestions(query));
        }

        // 添加快捷操作
        suggestions.addAll(getQuickActionSuggestions(query));

        // 限制建议数量，最多显示10个
        if (suggestions.size() > 10) {
            suggestions = suggestions.subList(0, 10);
        }

        return suggestions;
    }

    /**
     * 获取历史记录建议
     */
    private List<SuggestionItem> getHistorySuggestions() {
        List<SuggestionItem> suggestions = new ArrayList<>();

        try {
            // 获取最近的历史记录
            List<com.hippo.ehviewer.client.data.HistoryInfo> historyItems = mHistoryManager.getRecentHistory(5);

            for (com.hippo.ehviewer.client.data.HistoryInfo item : historyItems) {
                suggestions.add(new SuggestionItem(
                    SuggestionType.HISTORY,
                    item.title != null ? item.title : item.url,
                    item.url,
                    item.url,
                    R.drawable.ic_history
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get history suggestions", e);
        }

        return suggestions;
    }

    /**
     * 获取筛选的历史记录建议
     */
    private List<SuggestionItem> getFilteredHistorySuggestions(String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        try {
            List<com.hippo.ehviewer.client.data.HistoryInfo> historyItems = mHistoryManager.searchHistory(query);

            for (com.hippo.ehviewer.client.data.HistoryInfo item : historyItems) {
                suggestions.add(new SuggestionItem(
                    SuggestionType.HISTORY,
                    item.title != null ? item.title : item.url,
                    item.url,
                    item.url,
                    R.drawable.ic_history
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get filtered history suggestions", e);
        }

        return suggestions;
    }

    /**
     * 获取书签建议
     */
    private List<SuggestionItem> getBookmarkSuggestions(String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        // 这里可以添加书签搜索逻辑
        // 暂时返回空列表

        return suggestions;
    }

    /**
     * 获取搜索建议
     */
    private List<SuggestionItem> getSearchSuggestions(String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        // 添加直接搜索选项
        suggestions.add(new SuggestionItem(
            SuggestionType.SEARCH_SUGGESTION,
            query,
            "在 " + getCurrentEngineName() + " 中搜索",
            mSearchConfigManager.getSearchUrl(query),
            R.drawable.ic_search
        ));

        // 添加其他搜索引擎选项
        List<SearchConfigManager.SearchEngine> engines = mSearchConfigManager.getAvailableEngines();
        for (SearchConfigManager.SearchEngine engine : engines) {
            if (!engine.id.equals(mSearchConfigManager.getCurrentEngine().id)) {
                suggestions.add(new SuggestionItem(
                    SuggestionType.SEARCH_SUGGESTION,
                    query,
                    "在 " + engine.name + " 中搜索",
                    engine.getSearchUrl(query),
                    R.drawable.ic_search
                ));
            }
        }

        return suggestions;
    }

    /**
     * 获取新标签页建议
     */
    private List<SuggestionItem> getNewTabSuggestions(String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        // 添加在新标签页中搜索的选项
        suggestions.add(new SuggestionItem(
            SuggestionType.NEW_TAB_ACTION,
            "在标签页中搜索 \"" + query + "\"",
            "在新标签页中打开搜索结果",
            mSearchConfigManager.getSearchUrl(query),
            R.drawable.ic_tab
        ));

        // 如果是URL，也添加在新标签页中打开的选项
        if (mSearchConfigManager.isUrl(query)) {
            suggestions.add(new SuggestionItem(
                SuggestionType.NEW_TAB_ACTION,
                "在新标签页中打开",
                query,
                query.startsWith("http") ? query : "https://" + query,
                R.drawable.ic_tab
            ));
        }

        return suggestions;
    }

    /**
     * 获取快捷操作建议
     */
    private List<SuggestionItem> getQuickActionSuggestions(String query) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        // 智能匹配常用操作
        if (query.toLowerCase().startsWith("go")) {
            suggestions.add(new SuggestionItem(
                SuggestionType.QUICK_ACTION,
                "Google",
                "打开 Google 搜索",
                "https://www.google.com",
                R.drawable.ic_search
            ));
        }

        if (query.toLowerCase().contains("map") || query.toLowerCase().contains("地图")) {
            suggestions.add(new SuggestionItem(
                SuggestionType.QUICK_ACTION,
                "地图",
                "打开地图应用",
                "geo:0,0?q=",
                R.drawable.ic_home // 使用已存在的图标
            ));
        }

        if (query.toLowerCase().contains("tel") || query.toLowerCase().contains("电话")) {
            suggestions.add(new SuggestionItem(
                SuggestionType.QUICK_ACTION,
                "电话",
                "拨打电话",
                "tel:",
                R.drawable.ic_settings // 使用已存在的图标
            ));
        }

        return suggestions;
    }

    /**
     * 获取当前搜索引擎名称
     */
    private String getCurrentEngineName() {
        SearchConfigManager.SearchEngine engine = mSearchConfigManager.getCurrentEngine();
        return engine != null ? engine.name : "Google";
    }

    /**
     * 设置建议点击监听器
     */
    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        mListener = listener;
        mAdapter.setOnSuggestionClickListener(listener);
    }

    /**
     * 释放资源
     */
    public void destroy() {
        hideSuggestions();
        mSuggestionsPopup = null;
        mSuggestionsRecyclerView = null;
        mAdapter = null;
    }

    /**
     * 建议列表适配器
     */
    private static class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsViewHolder> {

        private List<SuggestionItem> mSuggestions = new ArrayList<>();
        private OnSuggestionClickListener mListener;

        public void setSuggestions(List<SuggestionItem> suggestions) {
            mSuggestions = suggestions != null ? suggestions : new ArrayList<>();
        }

        public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
            mListener = listener;
        }

        @NonNull
        @Override
        public SuggestionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
            return new SuggestionsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SuggestionsViewHolder holder, int position) {
            SuggestionItem item = mSuggestions.get(position);
            holder.bind(item, mListener);
        }

        @Override
        public int getItemCount() {
            return mSuggestions.size();
        }
    }

    /**
     * 建议项视图持有器
     */
    private static class SuggestionsViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mIconView;
        private final TextView mTitleView;
        private final TextView mSubtitleView;
        private final ImageView mActionView;

        public SuggestionsViewHolder(@NonNull View itemView) {
            super(itemView);
            mIconView = itemView.findViewById(R.id.suggestion_icon);
            mTitleView = itemView.findViewById(R.id.suggestion_title);
            mSubtitleView = itemView.findViewById(R.id.suggestion_subtitle);
            mActionView = itemView.findViewById(R.id.suggestion_action);
        }

        public void bind(SuggestionItem item, OnSuggestionClickListener listener) {
            mTitleView.setText(item.title);

            if (item.subtitle != null && !item.subtitle.isEmpty()) {
                mSubtitleView.setText(item.subtitle);
                mSubtitleView.setVisibility(View.VISIBLE);
            } else {
                mSubtitleView.setVisibility(View.GONE);
            }

            if (item.iconResId != 0) {
                mIconView.setImageResource(item.iconResId);
                mIconView.setVisibility(View.VISIBLE);
            } else {
                mIconView.setVisibility(View.GONE);
            }

            // 根据建议类型显示不同的操作按钮
            switch (item.type) {
                case HISTORY:
                    mActionView.setImageResource(R.drawable.ic_history);
                    mActionView.setVisibility(View.VISIBLE);
                    break;
                case BOOKMARK:
                    mActionView.setImageResource(R.drawable.ic_bookmark_border);
                    mActionView.setVisibility(View.VISIBLE);
                    break;
                default:
                    mActionView.setVisibility(View.GONE);
                    break;
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick(item);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionLongClick(item);
                    return true;
                }
                return false;
            });
        }
    }
}
