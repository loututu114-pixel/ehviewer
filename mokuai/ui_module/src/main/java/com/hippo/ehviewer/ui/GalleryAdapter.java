/*
 * EhViewer UI Module - GalleryAdapter
 * 画廊适配器 - 为画廊列表提供数据适配
 */

package com.hippo.ehviewer.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * 画廊适配器
 * 为画廊列表提供数据绑定和视图渲染
 */
public class GalleryAdapter extends BaseAdapter {

    private final Context mContext;
    private final List<Object> mGalleryList;
    private final LayoutInflater mInflater;

    public GalleryAdapter(Context context, List<Object> galleryList) {
        mContext = context;
        mGalleryList = galleryList;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mGalleryList != null ? mGalleryList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mGalleryList != null && position < mGalleryList.size() ?
               mGalleryList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // 创建新的视图
            convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            holder = new ViewHolder();
            holder.titleText = convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // 绑定数据
        Object galleryItem = getItem(position);
        if (galleryItem != null && holder.titleText != null) {
            // 这里应该根据实际的画廊数据结构来设置文本
            holder.titleText.setText("画廊项目 " + (position + 1));
        }

        return convertView;
    }

    /**
     * 更新数据列表
     */
    public void updateData(List<Object> newData) {
        if (mGalleryList != null) {
            mGalleryList.clear();
            if (newData != null) {
                mGalleryList.addAll(newData);
            }
            notifyDataSetChanged();
        }
    }

    /**
     * 添加数据
     */
    public void addData(List<Object> newData) {
        if (mGalleryList != null && newData != null) {
            mGalleryList.addAll(newData);
            notifyDataSetChanged();
        }
    }

    /**
     * 清空数据
     */
    public void clearData() {
        if (mGalleryList != null) {
            mGalleryList.clear();
            notifyDataSetChanged();
        }
    }

    /**
     * 获取数据列表
     */
    public List<Object> getData() {
        return mGalleryList;
    }

    /**
     * 视图持有者
     */
    private static class ViewHolder {
        TextView titleText;
    }
}
