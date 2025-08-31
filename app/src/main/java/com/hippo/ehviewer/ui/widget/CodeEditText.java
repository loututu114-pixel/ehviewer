package com.hippo.ehviewer.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * 验证码输入框
 * 自带验证码自动填充功能
 */
public class CodeEditText extends AppCompatEditText {
    
    private CodeAutoFillHelper autoFillHelper;
    
    public CodeEditText(Context context) {
        super(context);
        init();
    }
    
    public CodeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CodeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 初始化自动填充助手
        autoFillHelper = new CodeAutoFillHelper(getContext());
        autoFillHelper.attachToEditText(this);
        
        // 设置输入类型为数字
        setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        // 设置提示文本
        setHint("请输入验证码");
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
        // 清理资源
        if (autoFillHelper != null) {
            autoFillHelper.cleanup();
        }
    }
}