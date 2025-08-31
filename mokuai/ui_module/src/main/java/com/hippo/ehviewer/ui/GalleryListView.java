/*
 * EhViewer UI Module - GalleryListView
 * 画廊列表视图 - 显示画廊列表的自定义ListView
 */

package com.hippo.ehviewer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * 画廊列表视图
 * 专门用于显示画廊列表的自定义ListView组件
 */
public class GalleryListView extends ListView {

    public GalleryListView(Context context) {
        super(context);
        init();
    }

    public GalleryListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GalleryListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 配置ListView属性
        setDivider(null);
        setVerticalScrollBarEnabled(true);
        setFastScrollEnabled(true);
        setSmoothScrollbarEnabled(true);
    }

    /**
     * 设置画廊列表适配器
     */
    public void setGalleryAdapter(GalleryAdapter adapter) {
        setAdapter(adapter);
    }

    /**
     * 设置项目点击监听器
     */
    public void setOnGalleryItemClickListener(OnGalleryItemClickListener listener) {
        setOnItemClickListener((parent, view, position, id) -> {
            Object item = getItemAtPosition(position);
            if (item != null && listener != null) {
                listener.onGalleryItemClick(item, position);
            }
        });
    }

    /**
     * 画廊项目点击监听器接口
     */
    public interface OnGalleryItemClickListener {
        void onGalleryItemClick(Object galleryItem, int position);
    }
}
