/*
 * Copyright 2025 EhViewer Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.hippo.ehviewer.permission;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.hippo.ehviewer.Settings;

/**
 * 延迟权限管理器 - 只在用户真正需要功能时才请求权限
 * 
 * 核心设计理念：
 * 1. 让用户先体验应用的基本功能 (浏览、搜索)
 * 2. 仅在用户主动触发需要权限的功能时才请求
 * 3. 3天提醒一次机制，避免过度打扰
 * 4. 智能区分必需权限和可选权限
 */
public class DeferredPermissionManager {
    
    private static final String TAG = "DeferredPermissionManager";
    private static final String PREFS_NAME = "deferred_permissions";
    
    // 权限相关Key
    private static final String KEY_STORAGE_PERMISSION_REQUESTED = "storage_permission_requested";
    private static final String KEY_DEFAULT_BROWSER_REQUESTED = "default_browser_requested";
    private static final String KEY_LAST_REMINDER_TIME = "last_reminder_time";
    private static final String KEY_REMINDER_COUNT = "reminder_count";
    
    // 提醒间隔 (3天)
    private static final long REMINDER_INTERVAL = 3 * 24 * 60 * 60 * 1000L;
    
    private static DeferredPermissionManager instance;
    private SharedPreferences prefs;
    
    public static DeferredPermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new DeferredPermissionManager(context);
        }
        return instance;
    }
    
    private DeferredPermissionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 检查是否应该在下载时请求权限
     * @param activity 当前Activity
     * @return true如果需要显示权限请求对话框
     */
    public boolean shouldRequestDownloadPermissions(Activity activity) {
        Log.d(TAG, "检查下载权限请求需求");
        
        // 1. 检查存储权限
        boolean needStoragePermission = !hasStoragePermission(activity);
        
        // 2. 检查是否已经请求过且被拒绝
        boolean storageRequested = prefs.getBoolean(KEY_STORAGE_PERMISSION_REQUESTED, false);
        
        // 3. 检查提醒间隔
        boolean canRemind = canShowReminder();
        
        Log.d(TAG, "权限状态 - Storage需要: " + needStoragePermission + 
                ", 已请求: " + storageRequested + 
                ", 可提醒: " + canRemind);
        
        return needStoragePermission && (!storageRequested || canRemind);
    }
    
    /**
     * 检查是否应该提醒设置默认浏览器
     */
    public boolean shouldRemindDefaultBrowser(Activity activity) {
        Log.d(TAG, "检查默认浏览器提醒需求");
        
        // 检查是否已经是默认浏览器
        if (isDefaultBrowser(activity)) {
            Log.d(TAG, "已经是默认浏览器，无需提醒");
            return false;
        }
        
        // 检查是否已经请求过
        boolean browserRequested = prefs.getBoolean(KEY_DEFAULT_BROWSER_REQUESTED, false);
        
        // 检查提醒间隔
        boolean canRemind = canShowReminder();
        
        Log.d(TAG, "浏览器状态 - 已请求: " + browserRequested + ", 可提醒: " + canRemind);
        
        return !browserRequested || canRemind;
    }
    
    /**
     * 标记存储权限已请求
     */
    public void markStoragePermissionRequested() {
        Log.d(TAG, "标记存储权限已请求");
        prefs.edit().putBoolean(KEY_STORAGE_PERMISSION_REQUESTED, true).apply();
        updateReminderTime();
    }
    
    /**
     * 标记默认浏览器已请求
     */
    public void markDefaultBrowserRequested() {
        Log.d(TAG, "标记默认浏览器已请求");
        prefs.edit().putBoolean(KEY_DEFAULT_BROWSER_REQUESTED, true).apply();
        updateReminderTime();
    }
    
    /**
     * 更新提醒时间
     */
    private void updateReminderTime() {
        long currentTime = System.currentTimeMillis();
        int reminderCount = prefs.getInt(KEY_REMINDER_COUNT, 0) + 1;
        
        prefs.edit()
            .putLong(KEY_LAST_REMINDER_TIME, currentTime)
            .putInt(KEY_REMINDER_COUNT, reminderCount)
            .apply();
            
        Log.d(TAG, "更新提醒时间，第" + reminderCount + "次提醒");
    }
    
    /**
     * 检查是否可以显示提醒 (距离上次提醒超过3天)
     */
    private boolean canShowReminder() {
        long lastReminderTime = prefs.getLong(KEY_LAST_REMINDER_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        if (lastReminderTime == 0) {
            // 从未提醒过，可以提醒
            return true;
        }
        
        boolean canRemind = (currentTime - lastReminderTime) >= REMINDER_INTERVAL;
        Log.d(TAG, "提醒间隔检查 - 上次: " + lastReminderTime + 
                ", 当前: " + currentTime + 
                ", 可提醒: " + canRemind);
        
        return canRemind;
    }
    
    /**
     * 检查是否有存储权限
     */
    private boolean hasStoragePermission(Context context) {
        try {
            return android.os.Environment.isExternalStorageManager();
        } catch (Exception e) {
            Log.w(TAG, "检查存储权限失败", e);
            return false;
        }
    }
    
    /**
     * 检查是否是默认浏览器
     */
    private boolean isDefaultBrowser(Context context) {
        try {
            // 使用现有的DefaultBrowserHelper检查
            return com.hippo.ehviewer.util.DefaultBrowserHelper.isDefaultBrowser(context);
        } catch (Exception e) {
            Log.w(TAG, "检查默认浏览器状态失败", e);
            return false;
        }
    }
    
    /**
     * 重置所有权限请求状态 (用于测试或重置)
     */
    public void resetPermissionRequests() {
        Log.d(TAG, "重置所有权限请求状态");
        prefs.edit()
            .remove(KEY_STORAGE_PERMISSION_REQUESTED)
            .remove(KEY_DEFAULT_BROWSER_REQUESTED)
            .remove(KEY_LAST_REMINDER_TIME)
            .remove(KEY_REMINDER_COUNT)
            .apply();
    }
    
    /**
     * 获取提醒统计信息 (用于调试)
     */
    public String getReminderStats() {
        int reminderCount = prefs.getInt(KEY_REMINDER_COUNT, 0);
        long lastReminderTime = prefs.getLong(KEY_LAST_REMINDER_TIME, 0);
        boolean storageRequested = prefs.getBoolean(KEY_STORAGE_PERMISSION_REQUESTED, false);
        boolean browserRequested = prefs.getBoolean(KEY_DEFAULT_BROWSER_REQUESTED, false);
        
        return String.format(
            "提醒统计 - 次数: %d, 上次时间: %d, 存储已请求: %s, 浏览器已请求: %s",
            reminderCount, lastReminderTime, storageRequested, browserRequested
        );
    }
}