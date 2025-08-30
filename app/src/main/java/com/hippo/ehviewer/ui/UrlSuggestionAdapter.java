package com.hippo.ehviewer.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.DomainSuggestionManager;
import java.util.ArrayList;
import java.util.List;

/**
 * URL补全建议适配器
 * 为AutoCompleteTextView提供智能补全建议
 */
public class UrlSuggestionAdapter extends ArrayAdapter<DomainSuggestionManager.SuggestionItem> {
    private final LayoutInflater mInflater;
    private final DomainSuggestionManager mSuggestionManager;
    private final List<DomainSuggestionManager.SuggestionItem> mSuggestions = new ArrayList<>();
    private final List<DomainSuggestionManager.SuggestionItem> mFilteredSuggestions = new ArrayList<>();
    private final UrlFilter mFilter = new UrlFilter();

    public UrlSuggestionAdapter(@NonNull Context context) {
        super(context, R.layout.item_url_suggestion);
        this.mInflater = LayoutInflater.from(context);
        this.mSuggestionManager = new DomainSuggestionManager(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_url_suggestion, parent, false);
            holder = new ViewHolder();
            holder.titleText = convertView.findViewById(R.id.suggestion_title);
            holder.urlText = convertView.findViewById(R.id.suggestion_url);
            holder.typeIcon = convertView.findViewById(R.id.suggestion_type_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DomainSuggestionManager.SuggestionItem item = mFilteredSuggestions.get(position);

        // 设置显示文本
        String displayText = item.getDisplayText();
        holder.titleText.setText(displayText);

        // 设置URL（简化显示）
        String shortUrl = shortenUrl(item.url);
        holder.urlText.setText(shortUrl);

        // 根据类型设置图标和颜色
        setupTypeDisplay(holder, item.type);

        return convertView;
    }

    /**
     * 设置类型显示样式
     */
    private void setupTypeDisplay(ViewHolder holder, DomainSuggestionManager.SuggestionType type) {
        if (holder.typeIcon != null) {
            switch (type) {
                case PROTOCOL:
                    holder.typeIcon.setText("🔗");
                    holder.typeIcon.setTextColor(getContext().getColor(android.R.color.holo_blue_dark));
                    break;
                case DOMAIN:
                    holder.typeIcon.setText("🌐");
                    holder.typeIcon.setTextColor(getContext().getColor(android.R.color.holo_green_dark));
                    break;
                case ALIAS:
                    holder.typeIcon.setText("⭐");
                    holder.typeIcon.setTextColor(getContext().getColor(android.R.color.holo_orange_dark));
                    break;
                case HISTORY:
                    holder.typeIcon.setText("🕐");
                    holder.typeIcon.setTextColor(getContext().getColor(android.R.color.holo_purple));
                    break;
                case BOOKMARK:
                    holder.typeIcon.setText("⭐");
                    holder.typeIcon.setTextColor(getContext().getColor(android.R.color.holo_red_dark));
                    break;
                case SEARCH:
                    holder.typeIcon.setText("🔍");
                    holder.typeIcon.setTextColor(getContext().getColor(android.R.color.darker_gray));
                    break;
            }
        }
    }

    /**
     * 缩短URL显示
     */
    private String shortenUrl(String url) {
        if (url == null) return "";

        // 移除协议前缀以节省空间
        if (url.startsWith("https://")) {
            url = url.substring(8);
        } else if (url.startsWith("http://")) {
            url = url.substring(7);
        }

        // 限制长度
        if (url.length() > 40) {
            url = url.substring(0, 37) + "...";
        }

        return url;
    }

    @Override
    public int getCount() {
        return mFilteredSuggestions.size();
    }

    @Override
    public DomainSuggestionManager.SuggestionItem getItem(int position) {
        return mFilteredSuggestions.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return mFilter;
    }

    /**
     * 自定义过滤器
     */
    private class UrlFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                // 如果没有输入，清空建议
                results.values = new ArrayList<DomainSuggestionManager.SuggestionItem>();
                results.count = 0;
            } else {
                // 获取建议
                List<DomainSuggestionManager.SuggestionItem> suggestions =
                    mSuggestionManager.getSuggestions(constraint.toString());

                results.values = suggestions;
                results.count = suggestions.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredSuggestions.clear();

            if (results.values != null) {
                @SuppressWarnings("unchecked")
                List<DomainSuggestionManager.SuggestionItem> suggestions =
                    (List<DomainSuggestionManager.SuggestionItem>) results.values;
                mFilteredSuggestions.addAll(suggestions);
            }

            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

    /**
     * ViewHolder模式优化
     */
    private static class ViewHolder {
        TextView titleText;
        TextView urlText;
        TextView typeIcon;
    }
}
