package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder> {
    
    public static class SearchSuggestion {
        public enum Type {
            HISTORY,      // 历史记录
            BOOKMARK,     // 书签
            SEARCH,       // 搜索建议
            URL           // URL建议
        }
        
        private String title;
        private String subtitle;
        private String url;
        private Type type;
        private int iconResId;
        
        public SearchSuggestion(String title, String subtitle, String url, Type type) {
            this.title = title;
            this.subtitle = subtitle;
            this.url = url;
            this.type = type;
            
            // 根据类型设置图标
            switch (type) {
                case HISTORY:
                    this.iconResId = R.drawable.ic_history;
                    break;
                case BOOKMARK:
                    this.iconResId = R.drawable.ic_bookmark;
                    break;
                case SEARCH:
                    this.iconResId = R.drawable.ic_search;
                    break;
                case URL:
                    this.iconResId = R.drawable.ic_web;
                    break;
            }
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getUrl() { return url; }
        public Type getType() { return type; }
        public int getIconResId() { return iconResId; }
    }
    
    private Context context;
    private List<SearchSuggestion> suggestions;
    private OnSuggestionClickListener listener;
    
    public interface OnSuggestionClickListener {
        void onSuggestionClick(SearchSuggestion suggestion);
        void onSuggestionLongClick(SearchSuggestion suggestion);
    }
    
    public SearchSuggestionAdapter(Context context) {
        this.context = context;
        this.suggestions = new ArrayList<>();
    }
    
    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        this.listener = listener;
    }
    
    public void setSuggestions(List<SearchSuggestion> suggestions) {
        this.suggestions = suggestions;
        notifyDataSetChanged();
    }
    
    public void clearSuggestions() {
        suggestions.clear();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        SearchSuggestion suggestion = suggestions.get(position);
        
        holder.icon.setImageResource(suggestion.getIconResId());
        holder.title.setText(suggestion.getTitle());
        
        if (suggestion.getSubtitle() != null && !suggestion.getSubtitle().isEmpty()) {
            holder.subtitle.setText(suggestion.getSubtitle());
            holder.subtitle.setVisibility(View.VISIBLE);
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionClick(suggestion);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionLongClick(suggestion);
            }
            return true;
        });
    }
    
    @Override
    public int getItemCount() {
        return suggestions.size();
    }
    
    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView subtitle;
        
        SuggestionViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.suggestion_icon);
            title = itemView.findViewById(R.id.suggestion_title);
            subtitle = itemView.findViewById(R.id.suggestion_subtitle);
        }
    }
}