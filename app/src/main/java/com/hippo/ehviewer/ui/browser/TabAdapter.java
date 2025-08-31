package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.hippo.ehviewer.R;

import java.util.ArrayList;
import java.util.List;

public class TabAdapter extends RecyclerView.Adapter<TabAdapter.TabViewHolder> {
    
    private Context context;
    private List<BrowserTab> tabs;
    private OnTabClickListener listener;
    private int selectedPosition = 0;
    
    public interface OnTabClickListener {
        void onTabClick(int position);
        void onTabClose(int position);
        void onTabLongClick(int position);
    }
    
    public TabAdapter(Context context) {
        this.context = context;
        this.tabs = new ArrayList<>();
    }
    
    public void setOnTabClickListener(OnTabClickListener listener) {
        this.listener = listener;
    }
    
    public void setTabs(List<BrowserTab> tabs) {
        this.tabs = tabs;
        notifyDataSetChanged();
    }
    
    public void addTab(BrowserTab tab) {
        tabs.add(tab);
        notifyItemInserted(tabs.size() - 1);
    }
    
    public void removeTab(int position) {
        if (position >= 0 && position < tabs.size()) {
            tabs.remove(position);
            notifyItemRemoved(position);
            
            // 调整选中位置
            if (selectedPosition >= tabs.size() && tabs.size() > 0) {
                selectedPosition = tabs.size() - 1;
            }
        }
    }
    
    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }
    
    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_browser_tab, parent, false);
        return new TabViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        BrowserTab tab = tabs.get(position);
        
        // 设置标题和URL
        holder.tabTitle.setText(tab.getTitle());
        holder.tabUrl.setText(tab.getUrl());
        
        // 设置预览图
        if (tab.getPreview() != null) {
            holder.tabPreview.setImageBitmap(tab.getPreview());
        } else {
            holder.tabPreview.setImageResource(R.drawable.ic_web);
        }
        
        // 设置网站图标
        if (tab.getFavicon() != null) {
            holder.tabIcon.setImageBitmap(tab.getFavicon());
        } else {
            holder.tabIcon.setImageResource(R.drawable.ic_web);
        }
        
        // 显示或隐藏隐私模式标识
        holder.incognitoBadge.setVisibility(tab.isIncognito() ? View.VISIBLE : View.GONE);
        
        // 显示或隐藏选中状态
        holder.selectedIndicator.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTabClick(holder.getAdapterPosition());
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTabLongClick(holder.getAdapterPosition());
            }
            return true;
        });
        
        holder.tabClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTabClose(holder.getAdapterPosition());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return tabs.size();
    }
    
    static class TabViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView tabPreview;
        ImageButton tabClose;
        ImageView tabIcon;
        TextView tabTitle;
        TextView tabUrl;
        ImageView incognitoBadge;
        View selectedIndicator;
        
        TabViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tabPreview = itemView.findViewById(R.id.tab_preview);
            tabClose = itemView.findViewById(R.id.tab_close);
            tabIcon = itemView.findViewById(R.id.tab_icon);
            tabTitle = itemView.findViewById(R.id.tab_title);
            tabUrl = itemView.findViewById(R.id.tab_url);
            incognitoBadge = itemView.findViewById(R.id.tab_incognito_badge);
            selectedIndicator = itemView.findViewById(R.id.tab_selected_indicator);
        }
    }
    
    public static class BrowserTab {
        private String title = "新标签页";
        private String url = "";
        private Bitmap preview;
        private Bitmap favicon;
        private boolean isIncognito = false;
        private boolean isLoading = false;
        private Object webView;  // WebView instance
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public Bitmap getPreview() {
            return preview;
        }
        
        public void setPreview(Bitmap preview) {
            this.preview = preview;
        }
        
        public Bitmap getFavicon() {
            return favicon;
        }
        
        public void setFavicon(Bitmap favicon) {
            this.favicon = favicon;
        }
        
        public boolean isIncognito() {
            return isIncognito;
        }
        
        public void setIncognito(boolean incognito) {
            isIncognito = incognito;
        }
        
        public boolean isLoading() {
            return isLoading;
        }
        
        public void setLoading(boolean loading) {
            isLoading = loading;
        }
        
        public Object getWebView() {
            return webView;
        }
        
        public void setWebView(Object webView) {
            this.webView = webView;
        }
    }
}