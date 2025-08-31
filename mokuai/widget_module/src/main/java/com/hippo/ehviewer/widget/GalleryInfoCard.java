/*
 * EhViewer Widget Module - GalleryInfoCard
 * 画廊信息卡片 - 显示画廊信息的卡片组件
 */

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 画廊信息卡片
 * 显示画廊基本信息的卡片组件
 */
public class GalleryInfoCard extends LinearLayout {

    private TextView mTitleText;
    private TextView mUploaderText;
    private TextView mCategoryText;

    public GalleryInfoCard(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setPadding(16, 16, 16, 16);

        mTitleText = new TextView(context);
        mUploaderText = new TextView(context);
        mCategoryText = new TextView(context);

        addView(mTitleText);
        addView(mUploaderText);
        addView(mCategoryText);
    }

    /**
     * 设置画廊信息
     */
    public void setGalleryInfo(Object galleryInfo) {
        // 设置画廊信息到各个TextView
        mTitleText.setText("画廊标题");
        mUploaderText.setText("上传者");
        mCategoryText.setText("分类");
    }
}
