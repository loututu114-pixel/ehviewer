package com.hippo.ehviewer.ui.browser;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.browser.RealtimeSuggestionManager.SuggestionItem;
import com.hippo.ehviewer.ui.browser.RealtimeSuggestionManager.SuggestionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强建议适配器 - 支持分组显示和键盘导航
 */
public class EnhancedSuggestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 视图类型常量
    public static final int VIEW_TYPE_SUGGESTION = 0;
    public static final int VIEW_TYPE_GROUP_HEADER = 1;

    // 分组建议项
    public static class GroupedSuggestionItem {
        public final SuggestionItem suggestion;
        public final SuggestionType groupType;
        public final boolean isHeader;

        public GroupedSuggestionItem(SuggestionItem suggestion, SuggestionType groupType, boolean isHeader) {
            this.suggestion = suggestion;
            this.groupType = groupType;
            this.isHeader = isHeader;
        }
    }

    // 数据和回调
    private List<GroupedSuggestionItem> mItems = new ArrayList<>();
    private String mCurrentQuery = "";
    private int mSelectedPosition = -1;
    private OnSuggestionClickListener mListener;

    // 回调接口
    public interface OnSuggestionClickListener {
        void onSuggestionClick(SuggestionItem item);
        void onSuggestionLongClick(SuggestionItem item);
    }

    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        mListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).isHeader ? VIEW_TYPE_GROUP_HEADER : VIEW_TYPE_SUGGESTION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_GROUP_HEADER) {
            View view = inflater.inflate(R.layout.item_suggestion_group_header, parent, false);
            return new GroupHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_enhanced_suggestion, parent, false);
            return new SuggestionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GroupedSuggestionItem item = mItems.get(position);

        if (holder instanceof GroupHeaderViewHolder) {
            setupGroupHeader((GroupHeaderViewHolder) holder, item.groupType);
        } else if (holder instanceof SuggestionViewHolder) {
            setupSuggestion((SuggestionViewHolder) holder, item.suggestion, position);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /**
     * 设置建议数据（自动分组）
     */
    public void setSuggestions(List<SuggestionItem> suggestions) {
        mItems.clear();

        if (suggestions == null || suggestions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // 按类型分组
        Map<SuggestionType, List<SuggestionItem>> grouped = new LinkedHashMap<>();
        for (SuggestionItem item : suggestions) {
            grouped.computeIfAbsent(item.type, k -> new ArrayList<>()).add(item);
        }

        // 按优先级排序的类型列表
        SuggestionType[] sortedTypes = getSortedTypes();

        // 创建分组后的列表
        for (SuggestionType type : sortedTypes) {
            List<SuggestionItem> typeSuggestions = grouped.get(type);
            if (typeSuggestions != null && !typeSuggestions.isEmpty()) {
                // 添加组头
                mItems.add(new GroupedSuggestionItem(null, type, true));

                // 添加该组的所有建议
                for (SuggestionItem suggestion : typeSuggestions) {
                    mItems.add(new GroupedSuggestionItem(suggestion, type, false));
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * 设置当前查询（用于高亮）
     */
    public void setCurrentQuery(String query) {
        mCurrentQuery = query != null ? query : "";
        notifyDataSetChanged();
    }

    /**
     * 设置选中位置（键盘导航）
     */
    public void setSelectedPosition(int position) {
        if (position == mSelectedPosition) return;

        int oldPosition = mSelectedPosition;
        mSelectedPosition = position;

        if (oldPosition >= 0 && oldPosition < mItems.size()) {
            notifyItemChanged(oldPosition);
        }
        if (position >= 0 && position < mItems.size()) {
            notifyItemChanged(position);
        }
    }

    /**
     * 获取选中位置
     */
    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    /**
     * 清除选择
     */
    public void clearSelection() {
        setSelectedPosition(-1);
    }

    /**
     * 获取指定位置的建议项
     */
    public SuggestionItem getSuggestionAt(int position) {
        if (position < 0 || position >= mItems.size()) return null;
        GroupedSuggestionItem item = mItems.get(position);
        return item.isHeader ? null : item.suggestion;
    }

    /**
     * 获取建议类型优先级排序
     */
    private SuggestionType[] getSortedTypes() {
        return new SuggestionType[]{
            SuggestionType.HISTORY,
            SuggestionType.BOOKMARK,
            SuggestionType.SEARCH,
            SuggestionType.DOMAIN,
            SuggestionType.POPULAR
        };
    }

    /**
     * 设置组头
     */
    private void setupGroupHeader(GroupHeaderViewHolder holder, SuggestionType type) {
        holder.titleText.setText(type.getEmoji() + " " + type.getDisplayName());

        // 设置颜色
        int color = getTypeColor(holder.itemView.getContext(), type);
        holder.titleText.setTextColor(color);
    }

    /**
     * 设置建议项
     */
    private void setupSuggestion(SuggestionViewHolder holder, SuggestionItem item, int position) {
        // 设置文本和高亮
        SpannableString highlightedText = createHighlightedText(item.displayText, mCurrentQuery);
        holder.titleText.setText(highlightedText);

        // 设置URL
        if (item.url != null && !item.url.isEmpty()) {
            holder.urlText.setText(shortenUrl(item.url));
            holder.urlText.setVisibility(View.VISIBLE);
        } else {
            holder.urlText.setVisibility(View.GONE);
        }

        // 设置类型显示
        setupTypeDisplay(holder, item);

        // 设置选中状态
        boolean isSelected = position == mSelectedPosition;
        holder.itemView.setBackgroundResource(
            isSelected ? R.drawable.suggestion_item_background : android.R.color.transparent
        );

        // 设置点击监听
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onSuggestionClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mListener != null) {
                mListener.onSuggestionLongClick(item);
                return true;
            }
            return false;
        });
    }

    /**
     * 创建高亮文本
     */
    private SpannableString createHighlightedText(String text, String query) {
        if (text == null) text = "";
        if (query == null || query.isEmpty()) {
            return new SpannableString(text);
        }

        SpannableString spannable = new SpannableString(text);
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int index = lowerText.indexOf(lowerQuery);
        if (index >= 0) {
            // 高亮匹配部分
            spannable.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(spannable.getSpans(0, spannable.length(), Object.class)[0].getClass().getEnclosingClass().getResourceId("colorAccent", "color", null), android.R.color.holo_blue_light)),
                index, index + query.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD),
                index, index + query.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    /**
     * 缩短URL显示
     */
    private String shortenUrl(String url) {
        if (url == null) return "";
        if (url.length() <= 50) return url;

        // 移除协议
        String noProtocol = url.replaceFirst("^https?://", "");

        if (noProtocol.length() <= 50) return noProtocol;

        // 截取并添加省略号
        return noProtocol.substring(0, 47) + "...";
    }

    /**
     * 设置类型显示
     */
    private void setupTypeDisplay(SuggestionViewHolder holder, SuggestionItem item) {
        holder.typeIcon.setVisibility(View.VISIBLE);
        holder.typeText.setVisibility(View.VISIBLE);

        int iconRes = getTypeIcon(item.type);
        int color = getTypeColor(holder.itemView.getContext(), item.type);

        holder.typeIcon.setImageResource(iconRes);
        holder.typeText.setText(item.type.getEmoji());
        holder.typeText.setTextColor(color);
    }

    /**
     * 获取类型图标
     */
    private int getTypeIcon(SuggestionType type) {
        switch (type) {
            case HISTORY: return R.drawable.ic_history;
            case BOOKMARK: return R.drawable.ic_bookmark;
            case SEARCH: return R.drawable.ic_search;
            case DOMAIN: return R.drawable.ic_domain;
            case POPULAR: return R.drawable.ic_trending;
            default: return R.drawable.ic_suggestion;
        }
    }

    /**
     * 获取类型颜色
     */
    private int getTypeColor(android.content.Context context, SuggestionType type) {
        int colorRes;
        switch (type) {
            case HISTORY: colorRes = android.R.color.holo_blue_dark; break;
            case BOOKMARK: colorRes = android.R.color.holo_green_dark; break;
            case SEARCH: colorRes = android.R.color.holo_orange_dark; break;
            case DOMAIN: colorRes = android.R.color.holo_purple; break;
            case POPULAR: colorRes = android.R.color.holo_red_dark; break;
            default: colorRes = android.R.color.darker_gray; break;
        }
        return ContextCompat.getColor(context, colorRes);
    }

    // ViewHolder 类
    static class GroupHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;

        GroupHeaderViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.group_title_text);
        }
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView urlText;
        ImageView typeIcon;
        TextView typeText;

        SuggestionViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.suggestion_title);
            urlText = itemView.findViewById(R.id.suggestion_url);
            typeIcon = itemView.findViewById(R.id.suggestion_type_icon);
            typeText = itemView.findViewById(R.id.suggestion_type_text);
        }
    }
}
