package com.hippo.ehviewer.ui.addressbar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.ehviewer.R;

import java.util.List;

/**
 * 地址栏建议适配器
 * 为地址栏建议提供UI渲染
 */
public class AddressBarSuggestionAdapter extends BaseAdapter {
    
    private final Context mContext;
    private final List<SmartAddressBarManager.AddressBarSuggestion> mSuggestions;
    private final LayoutInflater mInflater;
    
    public AddressBarSuggestionAdapter(Context context, List<SmartAddressBarManager.AddressBarSuggestion> suggestions) {
        mContext = context;
        mSuggestions = suggestions;
        mInflater = LayoutInflater.from(context);
    }
    
    @Override
    public int getCount() {
        return mSuggestions.size();
    }
    
    @Override
    public SmartAddressBarManager.AddressBarSuggestion getItem(int position) {
        return mSuggestions.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_addressbar_suggestion, parent, false);
            holder = new ViewHolder();
            holder.iconView = convertView.findViewById(R.id.suggestion_icon);
            holder.titleView = convertView.findViewById(R.id.suggestion_title);
            holder.descriptionView = convertView.findViewById(R.id.suggestion_description);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        SmartAddressBarManager.AddressBarSuggestion suggestion = getItem(position);
        
        // 设置图标
        int iconRes = getSuggestionIcon(suggestion.type);
        holder.iconView.setImageResource(iconRes);
        
        // 设置标题
        holder.titleView.setText(suggestion.title);
        
        // 设置描述
        if (suggestion.description != null && !suggestion.description.isEmpty()) {
            holder.descriptionView.setText(suggestion.description);
            holder.descriptionView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionView.setVisibility(View.GONE);
        }
        
        return convertView;
    }
    
    /**
     * 根据建议类型获取图标
     */
    private int getSuggestionIcon(int type) {
        switch (type) {
            case SmartAddressBarManager.AddressBarSuggestion.TYPE_SEARCH:
                return R.drawable.ic_search;
            case SmartAddressBarManager.AddressBarSuggestion.TYPE_HISTORY:
                return R.drawable.ic_history;
            case SmartAddressBarManager.AddressBarSuggestion.TYPE_BOOKMARK:
                return R.drawable.ic_bookmark;
            case SmartAddressBarManager.AddressBarSuggestion.TYPE_URL:
                return R.drawable.ic_link;
            default:
                return R.drawable.ic_search;
        }
    }
    
    /**
     * ViewHolder模式优化
     */
    private static class ViewHolder {
        ImageView iconView;
        TextView titleView;
        TextView descriptionView;
    }
}