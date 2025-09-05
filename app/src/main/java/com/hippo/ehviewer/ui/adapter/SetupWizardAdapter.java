package com.hippo.ehviewer.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.SystemSettingsActivity;

/**
 * 设置向导适配器
 */
public class SetupWizardAdapter extends PagerAdapter {

    private Context mContext;
    private LayoutInflater mInflater;

    // 向导页面数据
    private SetupPage[] mPages = {
        new SetupPage(
            R.drawable.ic_welcome,
            "欢迎使用 EhViewer",
            "一个功能强大的多合一应用\n集浏览器、文件管理、APK安装于一体",
            "开始设置",
            null
        ),
        new SetupPage(
            R.drawable.ic_browser,
            "设置默认浏览器",
            "将 EhViewer 设为默认浏览器\n享受更好的网页浏览体验",
            "去设置",
            SystemSettingsActivity.class
        ),
        new SetupPage(
            R.drawable.ic_file,
            "文件管理功能",
            "EhViewer 可以打开和管理各种文件类型\n包括 APK、图片、文档等",
            "配置权限",
            SystemSettingsActivity.class
        ),
        new SetupPage(
            R.drawable.ic_browser,
            "设置完成",
            "恭喜！您已完成基本设置\n现在可以开始使用 EhViewer 的强大功能了",
            "开始使用",
            null
        )
    };

    public SetupWizardAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mPages.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = mInflater.inflate(R.layout.item_setup_wizard_page, container, false);
        
        SetupPage page = mPages[position];
        
        ImageView iconView = view.findViewById(R.id.setup_icon);
        TextView titleView = view.findViewById(R.id.setup_title);
        TextView descriptionView = view.findViewById(R.id.setup_description);
        Button actionButton = view.findViewById(R.id.setup_action_button);

        iconView.setImageResource(page.iconRes);
        titleView.setText(page.title);
        descriptionView.setText(page.description);
        actionButton.setText(page.actionText);

        // 设置按钮点击事件
        if (page.targetActivity != null) {
            actionButton.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, page.targetActivity);
                mContext.startActivity(intent);
            });
        } else {
            // 对于欢迎页和完成页，隐藏按钮
            if (position == 0 || position == mPages.length - 1) {
                actionButton.setVisibility(View.GONE);
            }
        }

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    // 设置页面数据类
    private static class SetupPage {
        final int iconRes;
        final String title;
        final String description;
        final String actionText;
        final Class<?> targetActivity;

        SetupPage(int iconRes, String title, String description, String actionText, Class<?> targetActivity) {
            this.iconRes = iconRes;
            this.title = title;
            this.description = description;
            this.actionText = actionText;
            this.targetActivity = targetActivity;
        }
    }
}