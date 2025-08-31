package com.hippo.ehviewer.ui.widget;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.hippo.ehviewer.notification.SmsCodeExtractorService;

/**
 * 验证码自动填充助手
 * 支持验证码输入框的自动填充和快速输入
 */
public class CodeAutoFillHelper {
    
    private Context context;
    private EditText targetEditText;
    private ViewGroup codeHintView;
    private SmsCodeExtractorService smsService;
    private boolean isServiceBound = false;
    
    // 广播接收器
    private BroadcastReceiver codeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.hippo.ehviewer.FILL_CODE".equals(intent.getAction())) {
                String code = intent.getStringExtra("code");
                if (code != null && targetEditText != null) {
                    fillCode(code);
                }
            }
        }
    };
    
    // 服务连接
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SmsCodeExtractorService.LocalBinder binder = 
                (SmsCodeExtractorService.LocalBinder) service;
            smsService = binder.getService();
            isServiceBound = true;
            
            // 检查最近的验证码
            checkRecentCodes();
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            smsService = null;
            isServiceBound = false;
        }
    };
    
    public CodeAutoFillHelper(Context context) {
        this.context = context;
        
        // 注册广播接收器
        IntentFilter filter = new IntentFilter("com.hippo.ehviewer.FILL_CODE");
        context.registerReceiver(codeReceiver, filter);
        
        // 绑定服务
        bindSmsService();
    }
    
    /**
     * 设置目标输入框
     */
    public void attachToEditText(EditText editText) {
        this.targetEditText = editText;
        
        // 设置输入框提示
        setupEditTextHint();
        
        // 添加文本监听
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 检查是否输入了验证码格式
                if (isCodeFormat(s.toString())) {
                    hideCodeHint();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 检查最近的验证码
        checkRecentCodes();
    }
    
    /**
     * 设置输入框提示
     */
    private void setupEditTextHint() {
        if (targetEditText == null) return;
        
        // 创建提示视图
        ViewGroup parent = (ViewGroup) targetEditText.getParent();
        if (parent != null) {
            codeHintView = createCodeHintView();
            
            // 将提示视图添加到输入框上方
            int index = parent.indexOfChild(targetEditText);
            parent.addView(codeHintView, index);
            
            // 默认隐藏
            codeHintView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 创建验证码提示视图
     */
    private ViewGroup createCodeHintView() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(0, 8, 0, 8);
        
        TextView hintText = new TextView(context);
        hintText.setText("最近的验证码：");
        hintText.setTextSize(12);
        layout.addView(hintText);
        
        return layout;
    }
    
    /**
     * 检查最近的验证码
     */
    private void checkRecentCodes() {
        if (!isServiceBound || smsService == null) return;
        
        String latestCode = smsService.getLatestCode();
        if (latestCode != null) {
            showCodeHint(latestCode);
        }
    }
    
    /**
     * 显示验证码提示
     */
    private void showCodeHint(String code) {
        if (codeHintView == null) return;
        
        codeHintView.removeAllViews();
        
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setBackgroundResource(android.R.drawable.edit_text);
        container.setPadding(16, 8, 16, 8);
        
        TextView hintText = new TextView(context);
        hintText.setText("最近的验证码：");
        hintText.setTextSize(12);
        container.addView(hintText);
        
        TextView codeText = new TextView(context);
        codeText.setText(code);
        codeText.setTextSize(14);
        codeText.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        codeText.setPadding(8, 0, 8, 0);
        container.addView(codeText);
        
        TextView fillButton = new TextView(context);
        fillButton.setText("[点击填充]");
        fillButton.setTextSize(12);
        fillButton.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        fillButton.setOnClickListener(v -> fillCode(code));
        container.addView(fillButton);
        
        codeHintView.addView(container);
        codeHintView.setVisibility(View.VISIBLE);
        
        // 5秒后自动隐藏
        codeHintView.postDelayed(this::hideCodeHint, 5000);
    }
    
    /**
     * 隐藏验证码提示
     */
    private void hideCodeHint() {
        if (codeHintView != null) {
            codeHintView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 填充验证码
     */
    private void fillCode(String code) {
        if (targetEditText != null) {
            targetEditText.setText(code);
            targetEditText.setSelection(code.length());
            
            // 隐藏提示
            hideCodeHint();
            
            // 显示填充成功提示
            Toast.makeText(context, "验证码已填充", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查是否为验证码格式
     */
    private boolean isCodeFormat(String text) {
        return text.matches("\\d{4,8}");
    }
    
    /**
     * 绑定短信服务
     */
    private void bindSmsService() {
        Intent intent = new Intent(context, SmsCodeExtractorService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        // 注销广播接收器
        try {
            context.unregisterReceiver(codeReceiver);
        } catch (Exception e) {
            // 忽略
        }
        
        // 解绑服务
        if (isServiceBound) {
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}