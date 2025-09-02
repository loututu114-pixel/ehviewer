package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;

/**
 * EhViewer现代浏览器界面 - 简化版本
 * 直接跳转到主浏览器，避免功能重复
 */
public class ModernBrowserActivity extends AppCompatActivity {

    private static final String TAG = "ModernBrowserActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // 直接启动主浏览器Activity，避免功能重复
            Intent browserIntent = new Intent(this, WebViewActivity.class);
            browserIntent.setAction(Intent.ACTION_VIEW);

            // 如果有URL数据，传递给浏览器
            Intent originalIntent = getIntent();
            if (originalIntent != null && originalIntent.getData() != null) {
                browserIntent.setData(originalIntent.getData());
            }

            // 传递原始intent的所有额外参数
            if (originalIntent != null && originalIntent.getExtras() != null) {
                browserIntent.putExtras(originalIntent.getExtras());
            }

            android.util.Log.d(TAG, "Redirecting to WebViewActivity to avoid duplication");
            startActivity(browserIntent);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error launching browser", e);
            Toast.makeText(this, "启动浏览器失败", Toast.LENGTH_SHORT).show();
        }

        finish(); // 立即关闭这个Activity
    }
}
