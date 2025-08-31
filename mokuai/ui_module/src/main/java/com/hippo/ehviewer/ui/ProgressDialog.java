/*
 * EhViewer UI Module - ProgressDialog
 * 进度对话框 - 显示操作进度的对话框组件
 */

package com.hippo.ehviewer.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 进度对话框
 * 显示操作进度的对话框组件
 */
public class ProgressDialog extends Dialog {

    private ProgressBar mProgressBar;
    private TextView mMessageText;
    private TextView mProgressText;

    public ProgressDialog(Context context) {
        super(context);
        init(context);
    }

    public ProgressDialog(Context context, int theme) {
        super(context, theme);
        init(context);
    }

    private void init(Context context) {
        // 设置对话框样式
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        // 加载布局
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(getLayoutResource(), null);
        setContentView(view);

        // 初始化视图
        initViews(view);
    }

    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        mProgressBar = view.findViewById(android.R.id.progress);
        mMessageText = view.findViewById(android.R.id.message);
        mProgressText = view.findViewById(android.R.id.text1);

        if (mProgressBar == null) {
            // 如果没有找到ProgressBar，创建一个
            mProgressBar = new ProgressBar(getContext());
            mProgressBar.setIndeterminate(true);
        }
    }

    /**
     * 设置消息文本
     */
    public void setMessage(CharSequence message) {
        if (mMessageText != null) {
            mMessageText.setText(message);
        }
    }

    /**
     * 设置消息文本（资源ID）
     */
    public void setMessage(int resId) {
        setMessage(getContext().getString(resId));
    }

    /**
     * 设置进度
     */
    public void setProgress(int progress) {
        if (mProgressBar != null) {
            mProgressBar.setProgress(progress);
            updateProgressText(progress);
        }
    }

    /**
     * 设置最大进度值
     */
    public void setMax(int max) {
        if (mProgressBar != null) {
            mProgressBar.setMax(max);
        }
    }

    /**
     * 设置是否为不确定进度
     */
    public void setIndeterminate(boolean indeterminate) {
        if (mProgressBar != null) {
            mProgressBar.setIndeterminate(indeterminate);
        }
    }

    /**
     * 更新进度文本
     */
    private void updateProgressText(int progress) {
        if (mProgressText != null) {
            mProgressText.setText(progress + "%");
        }
    }

    /**
     * 显示对话框
     */
    @Override
    public void show() {
        if (!isShowing()) {
            super.show();
        }
    }

    /**
     * 隐藏对话框
     */
    @Override
    public void dismiss() {
        if (isShowing()) {
            super.dismiss();
        }
    }

    /**
     * 获取布局资源ID
     * 子类可以重写此方法来使用自定义布局
     */
    protected int getLayoutResource() {
        // 返回一个简单的布局资源ID
        // 在实际使用中，应该定义一个具体的布局文件
        return android.R.layout.progress_dialog;
    }

    /**
     * 链式调用：设置消息
     */
    public ProgressDialog message(CharSequence message) {
        setMessage(message);
        return this;
    }

    /**
     * 链式调用：设置进度
     */
    public ProgressDialog progress(int progress) {
        setProgress(progress);
        return this;
    }

    /**
     * 链式调用：设置最大值
     */
    public ProgressDialog max(int max) {
        setMax(max);
        return this;
    }

    /**
     * 链式调用：设置不确定模式
     */
    public ProgressDialog indeterminate(boolean indeterminate) {
        setIndeterminate(indeterminate);
        return this;
    }
}
