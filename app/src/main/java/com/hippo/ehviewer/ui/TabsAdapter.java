package com.hippo.ehviewer.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;

import java.util.List;

/**
 * 标签页RecyclerView适配器
 */
public class TabsAdapter extends RecyclerView.Adapter<TabsAdapter.TabViewHolder> {
    private static final String TAG = "TabsAdapter";
    
    private final Context mContext;
    private List<TabsManagerActivity.TabInfo> mTabs;
    
    // 回调接口
    private OnTabClickListener mOnTabClickListener;
    private OnTabCloseListener mOnTabCloseListener;
    
    public interface OnTabClickListener {
        void onTabClicked(TabsManagerActivity.TabInfo tab);
    }
    
    public interface OnTabCloseListener {
        void onTabClosed(TabsManagerActivity.TabInfo tab);
    }
    
    public TabsAdapter(Context context, List<TabsManagerActivity.TabInfo> tabs) {
        this.mContext = context;
        this.mTabs = tabs;
    }
    
    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_tab, parent, false);
        return new TabViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        if (position < 0 || position >= mTabs.size()) return;
        
        TabsManagerActivity.TabInfo tab = mTabs.get(position);
        holder.bind(tab);
    }
    
    @Override
    public int getItemCount() {
        return mTabs != null ? mTabs.size() : 0;
    }
    
    /**
     * 更新标签列表
     */
    public void updateTabs(List<TabsManagerActivity.TabInfo> tabs) {
        this.mTabs = tabs;
        notifyDataSetChanged();
        Log.d(TAG, "Tabs updated: " + (tabs != null ? tabs.size() : 0) + " tabs");
    }
    
    /**
     * 设置标签点击监听器
     */
    public void setOnTabClickListener(OnTabClickListener listener) {
        this.mOnTabClickListener = listener;
    }
    
    /**
     * 设置标签关闭监听器
     */
    public void setOnTabCloseListener(OnTabCloseListener listener) {
        this.mOnTabCloseListener = listener;
    }
    
    /**
     * ViewHolder
     */
    class TabViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mTabPreview;
        private final View mIncognitoOverlay;
        private final ImageView mIncognitoIcon;
        private final ImageButton mCloseButton;
        private final ImageView mTabFavicon;
        private final TextView mTabTitle;
        private final TextView mTabUrl;
        private final View mActiveIndicator;
        
        public TabViewHolder(@NonNull View itemView) {
            super(itemView);
            
            mTabPreview = itemView.findViewById(R.id.tab_preview);
            mIncognitoOverlay = itemView.findViewById(R.id.incognito_overlay);
            mIncognitoIcon = itemView.findViewById(R.id.incognito_icon);
            mCloseButton = itemView.findViewById(R.id.close_button);
            mTabFavicon = itemView.findViewById(R.id.tab_favicon);
            mTabTitle = itemView.findViewById(R.id.tab_title);
            mTabUrl = itemView.findViewById(R.id.tab_url);
            mActiveIndicator = itemView.findViewById(R.id.active_indicator);
        }
        
        public void bind(TabsManagerActivity.TabInfo tab) {
            try {
                // 设置标题和URL
                mTabTitle.setText(tab.title);
                mTabUrl.setText(tab.url);
                
                // 设置预览图
                if (tab.preview != null) {
                    mTabPreview.setImageBitmap(tab.preview);
                } else {
                    // 默认预览图
                    mTabPreview.setImageResource(R.drawable.ic_web);
                    mTabPreview.setScaleType(ImageView.ScaleType.CENTER);
                }
                
                // 设置网站图标
                if (tab.favicon != null) {
                    mTabFavicon.setImageBitmap(tab.favicon);
                } else {
                    mTabFavicon.setImageResource(R.drawable.ic_web);
                }
                
                // 无痕模式处理
                if (tab.isIncognito) {
                    mIncognitoOverlay.setVisibility(View.VISIBLE);
                    mIncognitoIcon.setVisibility(View.VISIBLE);
                    // 为无痕标签设置深色主题
                    itemView.setAlpha(0.9f);
                } else {
                    mIncognitoOverlay.setVisibility(View.GONE);
                    mIncognitoIcon.setVisibility(View.GONE);
                    itemView.setAlpha(1.0f);
                }
                
                // 活跃状态指示
                if (tab.isActive) {
                    mActiveIndicator.setVisibility(View.VISIBLE);
                    itemView.setBackgroundColor(mContext.getColor(R.color.tab_active_background));
                } else {
                    mActiveIndicator.setVisibility(View.GONE);
                    itemView.setBackgroundColor(mContext.getColor(android.R.color.transparent));
                }
                
                // 设置点击事件
                itemView.setOnClickListener(v -> {
                    if (mOnTabClickListener != null) {
                        mOnTabClickListener.onTabClicked(tab);
                    }
                });
                
                // 设置关闭按钮事件
                mCloseButton.setOnClickListener(v -> {
                    if (mOnTabCloseListener != null) {
                        mOnTabCloseListener.onTabClosed(tab);
                    }
                });
                
                // 长按显示更多选项
                itemView.setOnLongClickListener(v -> {
                    showTabContextMenu(tab);
                    return true;
                });
                
                Log.d(TAG, "Tab bound: " + tab.title + " (ID: " + tab.tabId + ")");
                
            } catch (Exception e) {
                Log.e(TAG, "Error binding tab", e);
            }
        }
        
        /**
         * 显示简单的标签页操作菜单
         */
        private void showTabContextMenu(TabsManagerActivity.TabInfo tab) {
            try {
                String[] menuItems = {
                    "关闭标签页",
                    "复制链接"
                };

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
                builder.setTitle(tab.title);

                builder.setItems(menuItems, (dialog, which) -> {
                    switch (which) {
                        case 0: // 关闭标签
                            if (mOnTabCloseListener != null) {
                                mOnTabCloseListener.onTabClosed(tab);
                            }
                            break;
                        case 1: // 复制链接
                            copyUrlToClipboard(tab.url);
                            break;
                    }
                });

                builder.setNegativeButton("取消", null);
                builder.show();

            } catch (Exception e) {
                Log.e(TAG, "Error showing context menu", e);
            }
        }
        
        /**
         * 复制URL到剪贴板
         */
        private void copyUrlToClipboard(String url) {
            try {
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("URL", url);
                clipboard.setPrimaryClip(clip);
                
                android.widget.Toast.makeText(mContext, "链接已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show();
                
            } catch (Exception e) {
                Log.e(TAG, "Error copying URL to clipboard", e);
            }
        }
    }
}