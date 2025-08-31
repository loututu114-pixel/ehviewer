package com.hippo.ehviewer.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机启动广播接收器
 * 在设备启动时自动启动监控服务
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            Log.d(TAG, "Device boot completed, starting services...");
            
            // 启动任务触发服务
            Intent serviceIntent = new Intent(context, TaskTriggerService.class);
            context.startService(serviceIntent);
            
            // 初始化推送通知
            NotificationManager.getInstance(context);
            
            Log.d(TAG, "Services started successfully");
        }
    }
}