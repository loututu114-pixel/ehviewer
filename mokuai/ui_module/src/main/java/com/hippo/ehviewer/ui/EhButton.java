/*
 * EhViewer UI Module - EhButton
 * EhViewer自定义按钮 - 具有Material Design风格的自定义按钮
 */

package com.hippo.ehviewer.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

/**
 * EhViewer自定义按钮
 * 具有Material Design风格的自定义按钮组件
 */
public class EhButton extends AppCompatButton {

    public EhButton(Context context) {
        super(context);
        init();
    }

    public EhButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EhButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 设置默认样式
        setAllCaps(false);
        setTextColor(Color.WHITE);
        setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

        // 设置内边距
        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        setPadding(padding * 2, padding, padding * 2, padding);

        // 设置圆角背景
        setBackgroundResource(android.R.drawable.btn_default);
    }

    /**
     * 设置按钮主题颜色
     */
    public void setThemeColor(int color) {
        setBackgroundTintList(ColorStateList.valueOf(color));
    }

    /**
     * 设置为主要按钮样式
     */
    public void setPrimaryStyle() {
        setThemeColor(Color.parseColor("#4CAF50"));
        setTextColor(Color.WHITE);
    }

    /**
     * 设置为次要按钮样式
     */
    public void setSecondaryStyle() {
        setThemeColor(Color.parseColor("#2196F3"));
        setTextColor(Color.WHITE);
    }

    /**
     * 设置为危险按钮样式
     */
    public void setDangerStyle() {
        setThemeColor(Color.parseColor("#F44336"));
        setTextColor(Color.WHITE);
    }

    /**
     * 设置为禁用状态
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            setAlpha(1.0f);
        } else {
            setAlpha(0.5f);
        }
    }
}
