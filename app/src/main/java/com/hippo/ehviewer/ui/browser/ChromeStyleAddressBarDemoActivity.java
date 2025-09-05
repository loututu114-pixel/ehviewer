package com.hippo.ehviewer.ui.browser;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.browser.RealtimeSuggestionManager.SuggestionItem;

/**
 * Chrome 风格地址栏演示活动
 * 用于展示和测试地址栏功能
 */
public class ChromeStyleAddressBarDemoActivity extends AppCompatActivity {

    private SmartAddressBarWidget mAddressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chrome_style_demo);

        initViews();
        setupAddressBar();
    }

    private void initViews() {
        mAddressBar = findViewById(R.id.smart_address_bar);
    }

    private void setupAddressBar() {
        // 设置地址栏监听器
        mAddressBar.setOnAddressBarListener(new SmartAddressBarWidget.OnAddressBarListener() {
            @Override
            public void onUrlSubmit(String url) {
                handleUrlSubmit(url);
            }

            @Override
            public void onSuggestionClick(SuggestionItem item) {
                handleSuggestionClick(item);
            }

            @Override
            public void onSuggestionLongClick(SuggestionItem item) {
                handleSuggestionLongClick(item);
            }
        });

        // 设置演示数据
        setupDemoSuggestionsListener();
    }

    private void handleUrlSubmit(String url) {
        Toast.makeText(this, "提交URL: " + url, Toast.LENGTH_SHORT).show();

        // 模拟加载状态
        mAddressBar.showLoadingState();
        mAddressBar.postDelayed(() -> mAddressBar.showNormalState(), 2000);
    }

    private void handleSuggestionClick(SuggestionItem item) {
        Toast.makeText(this, "点击建议: " + item.text, Toast.LENGTH_SHORT).show();
    }

    private void handleSuggestionLongClick(SuggestionItem item) {
        Toast.makeText(this, "长按建议: " + item.displayText + "\n类型: " + item.type.getDisplayName(),
            Toast.LENGTH_LONG).show();
    }

    private void setupDemoSuggestionsListener() {
        // 这里可以设置一些演示建议数据
        // 实际应用中，建议会自动从 RealtimeSuggestionManager 获取
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        RealtimeSuggestionManager.getInstance(this).destroy();
        NetworkSuggestionProvider.getInstance().destroy();
    }
}
