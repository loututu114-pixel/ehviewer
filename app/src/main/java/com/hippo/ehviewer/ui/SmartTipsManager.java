package com.hippo.ehviewer.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.DefaultBrowserHelper;
import com.hippo.ehviewer.util.UserEnvironmentDetector;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * 智能小贴士管理器
 * 根据用户状态和时间智能显示有用的功能提示
 */
public class SmartTipsManager {
    
    private static final String TAG = "SmartTipsManager";
    private static final String PREFS_NAME = "smart_tips_prefs";
    
    private final AppCompatActivity mActivity;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    
    private LinearLayout mTipsContainer;
    
    public SmartTipsManager(@NonNull AppCompatActivity activity) {
        mActivity = activity;
        mContext = activity.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 初始化小贴士容器
     */
    public void initializeTipsContainer(@NonNull ViewGroup parentContainer) {
        try {
            // 查找Tips ScrollView容器
            ScrollView tipsScrollView = parentContainer.findViewById(R.id.smart_tips_container);
            if (tipsScrollView != null) {
                // 获取ScrollView内部的LinearLayout
                mTipsContainer = tipsScrollView.findViewById(R.id.smart_tips_layout);
                
                if (mTipsContainer != null) {
                    // 显示智能小贴士
                    showSmartTips();
                    
                    // 当有tips显示时，让ScrollView可见
                    tipsScrollView.setVisibility(View.VISIBLE);
                } else {
                    Log.w(TAG, "smart_tips_layout not found in ScrollView");
                }
            } else {
                Log.w(TAG, "smart_tips_container ScrollView not found");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing tips container", e);
        }
    }
    
    /**
     * 显示智能小贴士
     */
    public void showSmartTips() {
        try {
            if (mTipsContainer == null) return;
            
            // 清空现有小贴士
            mTipsContainer.removeAllViews();
            
            // 获取当前状态
            boolean isDefaultBrowser = DefaultBrowserHelper.isDefaultBrowser(mContext);
            UserEnvironmentDetector.EnvironmentInfo envInfo = getUserEnvironmentInfo();
            
            // 生成智能小贴士
            List<SmartTip> tips = generateSmartTips(isDefaultBrowser, envInfo);
            
            // 显示小贴士（最多显示3个）
            int maxTips = Math.min(tips.size(), 3);
            for (int i = 0; i < maxTips; i++) {
                showTip(tips.get(i));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing smart tips", e);
        }
    }
    
    /**
     * 生成智能小贴士
     */
    private List<SmartTip> generateSmartTips(boolean isDefaultBrowser, UserEnvironmentDetector.EnvironmentInfo envInfo) {
        List<SmartTip> tips = new ArrayList<>();
        
        try {
            // 优先级0: 免费开源软件声明
            tips.add(new SmartTip(
                R.drawable.ic_favorite,
                "💝 免费开源软件",
                "EhViewer是完全免费的开源软件，为爱发电，不参与任何商业化行为。感谢您的使用和支持！",
                "了解更多",
                TipAction.SHOW_OPEN_SOURCE_INFO,
                TipPriority.HIGH
            ));
            
            // 优先级1: 默认浏览器设置
            if (!isDefaultBrowser) {
                tips.add(new SmartTip(
                    R.drawable.ic_star,
                    "🚀 设置默认浏览器",
                    "将EhViewer设为默认浏览器，所有链接都将在EhViewer中打开，享受最佳浏览体验！",
                    "立即设置",
                    TipAction.SET_DEFAULT_BROWSER,
                    TipPriority.HIGH
                ));
            }
            
            // 优先级2: 地区相关功能
            if (envInfo != null && envInfo.isChineseUser) {
                if (!envInfo.canAccessGoogle) {
                    tips.add(new SmartTip(
                        R.drawable.ic_search,
                        "🔍 智能搜索引擎",
                        "检测到您在中国，已为您配置百度搜索引擎，获得更快的搜索体验！",
                        "试试搜索",
                        TipAction.OPEN_SEARCH,
                        TipPriority.MEDIUM
                    ));
                }
            }
            
            // 优先级3: 功能发现
            tips.add(new SmartTip(
                R.drawable.ic_download,
                "📁 APK安装器",
                "EhViewer内置强大的APK安装器，支持安装.apk、.apk.1等各种格式的安装包！",
                "立即体验",
                TipAction.OPEN_APK_INSTALLER,
                TipPriority.MEDIUM
            ));
            
            tips.add(new SmartTip(
                R.drawable.ic_folder,
                "📂 文件管理器", 
                "内置专业文件管理器，轻松管理设备文件，支持多种文件格式预览！",
                "打开文件管理",
                TipAction.OPEN_FILE_MANAGER,
                TipPriority.MEDIUM
            ));
            
            // 优先级4: 时间相关提示
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            
            if (hour >= 6 && hour < 12) {
                tips.add(new SmartTip(
                    R.drawable.ic_sun,
                    "🌅 早安！",
                    "新的一天开始了！用EhViewer浏览最新资讯，开启美好的一天吧～",
                    "看看新闻",
                    TipAction.OPEN_NEWS,
                    TipPriority.LOW
                ));
            } else if (hour >= 12 && hour < 18) {
                tips.add(new SmartTip(
                    R.drawable.ic_sun,
                    "☀️ 午间休息",
                    "中午好！休息时间可以用EhViewer浏览一些轻松的内容放松一下～",
                    "随便看看",
                    TipAction.OPEN_RANDOM_SITE,
                    TipPriority.LOW
                ));
            } else if (hour >= 18 && hour < 24) {
                tips.add(new SmartTip(
                    R.drawable.ic_moon,
                    "🌙 晚间时光",
                    "晚上好！可以尝试EhViewer的私密模式，保护您的隐私浏览～",
                    "开启私密模式",
                    TipAction.OPEN_PRIVATE_MODE,
                    TipPriority.LOW
                ));
            }
            
            // 优先级5: 功能提示
            if (isDefaultBrowser) {
                tips.add(new SmartTip(
                    R.drawable.ic_flash_on,
                    "⚡ 快捷操作",
                    "您已成为EhViewer用户！点击菜单中的【快捷操作】可以快速访问常用功能！",
                    "查看快捷操作", 
                    TipAction.SHOW_QUICK_ACTIONS,
                    TipPriority.LOW
                ));
            }
            
            // 优先级6: 隐藏功能介绍
            tips.add(new SmartTip(
                R.drawable.ic_magic_wand,
                "✨ 隐藏功能",
                "长按地址栏可以快速粘贴剪贴板中的链接，双击可以全选地址！",
                "知道了",
                TipAction.DISMISS,
                TipPriority.LOW
            ));
            
            tips.add(new SmartTip(
                R.drawable.ic_gesture,
                "👆 手势操作",
                "支持左右滑动切换页面，双指缩放调整字体大小，让浏览更便捷！",
                "知道了",
                TipAction.DISMISS,
                TipPriority.LOW
            ));
            
            // 按优先级排序
            tips.sort((t1, t2) -> t2.priority.ordinal() - t1.priority.ordinal());
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating smart tips", e);
        }
        
        return tips;
    }
    
    /**
     * 显示单个小贴士
     */
    private void showTip(@NonNull SmartTip tip) {
        try {
            // 检查是否已经被忽略
            String dismissKey = "tip_dismissed_" + tip.title.hashCode();
            if (mPrefs.getBoolean(dismissKey, false)) {
                return;
            }
            
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            View tipView = inflater.inflate(R.layout.smart_tip_item, mTipsContainer, false);
            
            ImageView iconView = tipView.findViewById(R.id.smart_tip_icon);
            TextView titleView = tipView.findViewById(R.id.smart_tip_title);
            TextView contentView = tipView.findViewById(R.id.smart_tip_content);
            TextView actionButton = tipView.findViewById(R.id.smart_tip_action);
            ImageView dismissButton = tipView.findViewById(R.id.smart_tip_dismiss);
            
            iconView.setImageResource(tip.iconRes);
            titleView.setText(tip.title);
            contentView.setText(tip.content);
            actionButton.setText(tip.actionText);
            
            // 设置点击事件
            actionButton.setOnClickListener(v -> handleTipAction(tip.action, tip));
            dismissButton.setOnClickListener(v -> dismissTip(tipView, tip));
            
            // 设置背景颜色根据优先级
            switch (tip.priority) {
                case HIGH:
                    tipView.setBackgroundResource(R.drawable.smart_tip_high_priority_bg);
                    break;
                case MEDIUM:
                    tipView.setBackgroundResource(R.drawable.smart_tip_medium_priority_bg);
                    break;
                case LOW:
                    tipView.setBackgroundResource(R.drawable.smart_tip_low_priority_bg);
                    break;
            }
            
            // 添加到容器并播放动画
            mTipsContainer.addView(tipView);
            animateIn(tipView);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing tip", e);
        }
    }
    
    /**
     * 处理小贴士操作
     */
    private void handleTipAction(@NonNull TipAction action, @NonNull SmartTip tip) {
        try {
            switch (action) {
                case SET_DEFAULT_BROWSER:
                    DefaultBrowserHelper.trySetAsDefaultBrowser(mContext);
                    break;
                    
                case OPEN_SEARCH:
                    openUrlInBrowser("https://www.baidu.com");
                    break;
                    
                case OPEN_APK_INSTALLER:
                    openActivity(ApkInstallerActivity.class);
                    break;
                    
                case OPEN_FILE_MANAGER:
                    openActivity(FileManagerActivity.class);
                    break;
                    
                case OPEN_NEWS:
                    if (isChineseUser()) {
                        openUrlInBrowser("https://news.baidu.com");
                    } else {
                        openUrlInBrowser("https://news.google.com");
                    }
                    break;
                    
                case OPEN_RANDOM_SITE:
                    String[] sites = {
                        "https://www.zhihu.com",
                        "https://www.bilibili.com", 
                        "https://www.douban.com",
                        "https://www.reddit.com",
                        "https://www.youtube.com"
                    };
                    String randomSite = sites[new Random().nextInt(sites.length)];
                    openUrlInBrowser(randomSite);
                    break;
                    
                case OPEN_PRIVATE_MODE:
                    openBrowserWithPrivateMode();
                    break;
                    
                case SHOW_QUICK_ACTIONS:
                    showQuickActionsFromTip();
                    break;
                    
                case SHOW_OPEN_SOURCE_INFO:
                    showOpenSourceInfo();
                    break;
                    
                case DISMISS:
                    // 只是关闭，不做其他操作
                    break;
            }
            
            // 记录用户交互
            recordTipInteraction(tip, "action_clicked");
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling tip action", e);
            Toast.makeText(mContext, "操作失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 在EhViewer浏览器中打开URL
     */
    private void openUrlInBrowser(@NonNull String url) {
        try {
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.setData(Uri.parse(url));
            intent.putExtra("from_smart_tip", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening URL in browser", e);
            Toast.makeText(mContext, "打开链接失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开Activity
     */
    private void openActivity(@NonNull Class<?> activityClass) {
        try {
            Intent intent = new Intent(mContext, activityClass);
            intent.putExtra("from_smart_tip", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening activity", e);
            Toast.makeText(mContext, "打开页面失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开私密模式浏览器
     */
    private void openBrowserWithPrivateMode() {
        try {
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.putExtra("private_mode", true);
            intent.putExtra("from_smart_tip", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening private mode", e);
            Toast.makeText(mContext, "打开私密模式失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 从小贴士显示快捷操作
     */
    private void showQuickActionsFromTip() {
        SmartMenuManager menuManager = new SmartMenuManager(mActivity);
        // 这里可以直接调用快捷操作对话框，但需要先在SmartMenuManager中暴露这个方法
        Toast.makeText(mContext, "点击菜单按钮可查看更多快捷操作！", Toast.LENGTH_LONG).show();
    }
    
    /**
     * 显示开源信息和免费声明
     */
    private void showOpenSourceInfo() {
        try {
            // 创建对话框显示开源信息
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(mActivity)
                .setTitle("💝 EhViewer - 免费开源软件")
                .setMessage("📝 关于EhViewer：\n\n" +
                          "🆓 完全免费使用，无任何隐藏收费\n" +
                          "⭐ 开源项目，代码公开透明\n" +
                          "❤️ 为爱发电，不参与商业化\n" +
                          "🚫 拒绝广告，拒绝数据收集\n" +
                          "🤝 欢迎社区贡献和反馈\n\n" +
                          "感谢您选择使用EhViewer！\n" +
                          "您的支持是我们持续开发的动力！")
                .setPositiveButton("❤️ 支持开发者", (d, w) -> {
                    // 可以跳转到项目页面或捐赠页面
                    openUrlInBrowser("https://github.com/EhViewer-NekoInverter/EhViewer");
                })
                .setNegativeButton("知道了", null)
                .create();
                
            dialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing open source info", e);
            Toast.makeText(mContext, "EhViewer是免费开源软件，为爱发电！❤️", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 忽略小贴士
     */
    private void dismissTip(@NonNull View tipView, @NonNull SmartTip tip) {
        try {
            // 播放消失动画
            animateOut(tipView, () -> {
                mTipsContainer.removeView(tipView);
                
                // 检查是否还有其他tips，如果没有了就隐藏容器
                if (mTipsContainer.getChildCount() == 0) {
                    hideAllTips();
                }
            });
            
            // 记录忽略状态
            String dismissKey = "tip_dismissed_" + tip.title.hashCode();
            mPrefs.edit().putBoolean(dismissKey, true).apply();
            
            // 记录用户交互
            recordTipInteraction(tip, "dismissed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing tip", e);
        }
    }
    
    /**
     * 隐藏所有小贴士
     */
    public void hideAllTips() {
        try {
            ViewGroup rootLayout = (ViewGroup) mTipsContainer.getParent().getParent();
            ScrollView tipsScrollView = rootLayout.findViewById(R.id.smart_tips_container);
            if (tipsScrollView != null) {
                tipsScrollView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding all tips", e);
        }
    }
    
    /**
     * 播放进入动画
     */
    private void animateIn(@NonNull View view) {
        try {
            view.setAlpha(0f);
            view.setTranslationY(50f);
            
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
        } catch (Exception e) {
            Log.e(TAG, "Error in animate in", e);
        }
    }
    
    /**
     * 播放退出动画
     */
    private void animateOut(@NonNull View view, @NonNull Runnable onComplete) {
        try {
            view.animate()
                .alpha(0f)
                .translationY(-50f)
                .setDuration(250)
                .withEndAction(onComplete)
                .start();
        } catch (Exception e) {
            Log.e(TAG, "Error in animate out", e);
            onComplete.run();
        }
    }
    
    /**
     * 记录小贴士交互
     */
    private void recordTipInteraction(@NonNull SmartTip tip, @NonNull String action) {
        try {
            String interactionKey = "tip_interaction_" + tip.title.hashCode() + "_" + action;
            long currentTime = System.currentTimeMillis();
            mPrefs.edit().putLong(interactionKey, currentTime).apply();
            
            Log.d(TAG, "Tip interaction recorded: " + tip.title + " - " + action);
        } catch (Exception e) {
            Log.e(TAG, "Error recording tip interaction", e);
        }
    }
    
    /**
     * 获取用户环境信息
     */
    private UserEnvironmentDetector.EnvironmentInfo getUserEnvironmentInfo() {
        try {
            UserEnvironmentDetector detector = UserEnvironmentDetector.getInstance(mContext);
            return detector.getEnvironmentInfo();
        } catch (Exception e) {
            Log.e(TAG, "Error getting user environment info", e);
            return null;
        }
    }
    
    /**
     * 判断是否为中国用户
     */
    private boolean isChineseUser() {
        try {
            UserEnvironmentDetector detector = UserEnvironmentDetector.getInstance(mContext);
            return detector.isChineseUser();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if Chinese user", e);
            return false;
        }
    }
    
    /**
     * 清空所有已忽略的小贴士（用于调试）
     */
    public void resetAllDismissedTips() {
        try {
            SharedPreferences.Editor editor = mPrefs.edit();
            for (String key : mPrefs.getAll().keySet()) {
                if (key.startsWith("tip_dismissed_")) {
                    editor.remove(key);
                }
            }
            editor.apply();
            
            // 重新显示小贴士
            showSmartTips();
            
            Toast.makeText(mContext, "所有小贴士已重置", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error resetting dismissed tips", e);
        }
    }
    
    // ===== 内部类定义 =====
    
    /**
     * 智能小贴士数据类
     */
    public static class SmartTip {
        @DrawableRes public final int iconRes;
        public final String title;
        public final String content;
        public final String actionText;
        public final TipAction action;
        public final TipPriority priority;
        
        public SmartTip(@DrawableRes int iconRes, String title, String content, 
                       String actionText, TipAction action, TipPriority priority) {
            this.iconRes = iconRes;
            this.title = title;
            this.content = content;
            this.actionText = actionText;
            this.action = action;
            this.priority = priority;
        }
    }
    
    /**
     * 小贴士操作类型
     */
    public enum TipAction {
        SET_DEFAULT_BROWSER,
        OPEN_SEARCH,
        OPEN_APK_INSTALLER,
        OPEN_FILE_MANAGER,
        OPEN_NEWS,
        OPEN_RANDOM_SITE,
        OPEN_PRIVATE_MODE,
        SHOW_QUICK_ACTIONS,
        SHOW_OPEN_SOURCE_INFO,
        DISMISS
    }
    
    /**
     * 小贴士优先级
     */
    public enum TipPriority {
        HIGH,    // 高优先级（红色系）
        MEDIUM,  // 中优先级（蓝色系）
        LOW      // 低优先级（绿色系）
    }
}