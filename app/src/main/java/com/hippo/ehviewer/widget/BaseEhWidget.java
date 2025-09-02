package com.hippo.ehviewer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.WebViewActivity;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * EhViewer桌面小部件基类
 * 提供通用的小部件功能和浏览器集成
 */
public abstract class BaseEhWidget extends AppWidgetProvider {
    
    private static final String TAG = "BaseEhWidget";
    
    // 通用Actions
    public static final String ACTION_WIDGET_CLICK = "com.hippo.ehviewer.widget.CLICK";
    public static final String ACTION_WIDGET_REFRESH = "com.hippo.ehviewer.widget.REFRESH";
    public static final String ACTION_OPEN_BROWSER = "com.hippo.ehviewer.widget.OPEN_BROWSER";
    
    // 线程池用于异步更新
    protected static ScheduledExecutorService widgetExecutor = Executors.newScheduledThreadPool(2);
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "Updating widget: " + getClass().getSimpleName());
        
        // 异步更新所有小部件实例
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
        
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        String action = intent.getAction();
        if (action == null) return;
        
        Log.d(TAG, "Widget received action: " + action);
        
        switch (action) {
            case ACTION_WIDGET_CLICK:
                handleWidgetClick(context, intent);
                break;
                
            case ACTION_WIDGET_REFRESH:
                handleWidgetRefresh(context, intent);
                break;
                
            case ACTION_OPEN_BROWSER:
                handleOpenBrowser(context, intent);
                break;
                
            default:
                // 让子类处理特定的action
                handleCustomAction(context, intent, action);
                break;
        }
    }
    
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        
        // 当小部件大小改变时更新
        updateWidget(context, appWidgetManager, appWidgetId);
    }
    
    /**
     * 更新单个小部件
     */
    protected void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        widgetExecutor.submit(() -> {
            try {
                RemoteViews views = createRemoteViews(context, appWidgetId);
                if (views != null) {
                    // 设置通用点击事件
                    setupCommonClickActions(context, views, appWidgetId);
                    
                    // 让子类设置特定的点击事件
                    setupCustomClickActions(context, views, appWidgetId);
                    
                    // 更新小部件
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update widget", e);
            }
        });
    }
    
    /**
     * 创建RemoteViews - 子类必须实现
     */
    protected abstract RemoteViews createRemoteViews(Context context, int appWidgetId);
    
    /**
     * 设置通用点击事件
     */
    protected void setupCommonClickActions(Context context, RemoteViews views, int appWidgetId) {
        // 点击打开应用主界面
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 
            appWidgetId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // 设置整个小部件的默认点击行为
        views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent);
        
        // 刷新按钮（如果存在）
        Intent refreshIntent = new Intent(context, getClass());
        refreshIntent.setAction(ACTION_WIDGET_REFRESH);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context,
            appWidgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        try {
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent);
        } catch (Exception e) {
            // 刷新按钮不存在，忽略
        }
    }
    
    /**
     * 设置自定义点击事件 - 子类可以重写
     */
    protected void setupCustomClickActions(Context context, RemoteViews views, int appWidgetId) {
        // 子类重写此方法设置特定的点击事件
    }
    
    /**
     * 处理小部件点击
     */
    protected void handleWidgetClick(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        String clickType = intent.getStringExtra("click_type");
        
        if ("browser".equals(clickType)) {
            // 打开浏览器
            openBrowser(context, intent.getStringExtra("url"));
        } else {
            // 默认打开主界面
            openMainActivity(context);
        }
    }
    
    /**
     * 处理小部件刷新
     */
    protected void handleWidgetRefresh(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId != -1) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }
    
    /**
     * 处理打开浏览器
     */
    protected void handleOpenBrowser(Context context, Intent intent) {
        String url = intent.getStringExtra("url");
        String title = intent.getStringExtra("title");
        openBrowser(context, url, title);
    }
    
    /**
     * 处理自定义action - 子类可以重写
     */
    protected void handleCustomAction(Context context, Intent intent, String action) {
        // 子类重写此方法处理特定的action
    }
    
    /**
     * 打开浏览器
     */
    protected void openBrowser(Context context, String url) {
        openBrowser(context, url, null);
    }
    
    /**
     * 打开浏览器（带标题）
     */
    protected void openBrowser(Context context, String url, String title) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        if (url != null) {
            intent.setData(android.net.Uri.parse(url));
        } else {
            intent.setData(android.net.Uri.parse("https://www.google.com"));
        }
        
        if (title != null) {
            intent.putExtra("title", title);
        }
        
        intent.putExtra("from_widget", true);
        
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open browser", e);
            // 降级到主界面
            openMainActivity(context);
        }
    }
    
    /**
     * 打开主界面
     */
    protected void openMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("from_widget", true);
        
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open main activity", e);
        }
    }
    
    /**
     * 创建带URL的PendingIntent
     */
    protected PendingIntent createBrowserPendingIntent(Context context, int requestCode, String url, String title) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(ACTION_OPEN_BROWSER);
        intent.putExtra("url", url);
        if (title != null) {
            intent.putExtra("title", title);
        }
        
        return PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    
    /**
     * 创建点击PendingIntent
     */
    protected PendingIntent createClickPendingIntent(Context context, int requestCode, String clickType, Bundle extras) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(ACTION_WIDGET_CLICK);
        intent.putExtra("click_type", clickType);
        if (extras != null) {
            intent.putExtras(extras);
        }
        
        return PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    
    /**
     * 获取小部件的默认大小信息
     */
    protected Bundle getWidgetSize(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            return appWidgetManager.getAppWidgetOptions(appWidgetId);
        }
        return new Bundle();
    }
    
    /**
     * 强制更新所有该类型的小部件
     */
    protected void forceUpdateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, getClass());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        
        if (appWidgetIds.length > 0) {
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
    
    /**
     * 格式化时间显示
     */
    protected String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
    
    /**
     * 格式化日期显示
     */
    protected String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
    
    /**
     * 格式化文件大小
     */
    protected String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.d(TAG, "Widget deleted: " + getClass().getSimpleName());
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widget disabled: " + getClass().getSimpleName());
    }
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled: " + getClass().getSimpleName());
    }
}