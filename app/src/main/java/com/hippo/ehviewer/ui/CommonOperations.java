/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

import android.app.Activity;
import android.content.Intent;

import com.hippo.app.ListCheckBoxDialogBuilder;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.DownloadLabel;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.download.DownloadService;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.ehviewer.permission.DeferredPermissionManager;
import com.hippo.ehviewer.permission.DownloadPermissionDialog;
import com.hippo.unifile.UniFile;
import com.hippo.lib.yorozuya.IOUtils;
import com.hippo.lib.yorozuya.collect.LongList;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CommonOperations {

    private static void doAddToFavorites(Activity activity, GalleryInfo galleryInfo,
                                         int slot, EhClient.Callback<Void> listener) {
        if (slot == -1) {
            EhDB.putLocalFavorite(galleryInfo);
            listener.onSuccess(null);
        } else if (slot >= 0 && slot <= 9) {
            EhClient client = EhApplication.getEhClient(activity);
            EhRequest request = new EhRequest();
            request.setMethod(EhClient.METHOD_ADD_FAVORITES);
            request.setArgs(galleryInfo.gid, galleryInfo.token, slot, "");
            request.setCallback(listener);
            client.execute(request);
        } else {
            listener.onFailure(new Exception()); // TODO Add text
        }
    }

    public static void addToFavorites(final Activity activity, final GalleryInfo galleryInfo,
                                      final EhClient.Callback<Void> listener, boolean isDefaultFavSolt) {
        int slot = Settings.getDefaultFavSlot();
        String[] items = new String[11];
        items[0] = activity.getString(R.string.local_favorites);
        String[] favCat = Settings.getFavCat();
        System.arraycopy(favCat, 0, items, 1, 10);
        if ((slot >= -1 && slot <= 9)&&!isDefaultFavSolt) {
            String newFavoriteName = slot >= 0 ? items[slot + 1] : null;
            doAddToFavorites(activity, galleryInfo, slot, new DelegateFavoriteCallback(listener, galleryInfo, newFavoriteName, slot));
        } else {
            new ListCheckBoxDialogBuilder(activity, items,
                    (builder, dialog, position) -> {
                        int slot1 = position - 1;
                        String newFavoriteName = (slot1 >= 0 && slot1 <= 9) ? items[slot1 + 1] : null;
                        doAddToFavorites(activity, galleryInfo, slot1, new DelegateFavoriteCallback(listener, galleryInfo, newFavoriteName, slot1));
                        if (builder.isChecked()) {
                            Settings.putDefaultFavSlot(slot1);
                        } else {
                            Settings.putDefaultFavSlot(Settings.INVALID_DEFAULT_FAV_SLOT);
                        }
                    }, activity.getString(R.string.remember_favorite_collection), false)
                    .setTitle(R.string.add_favorites_dialog_title)
                    .setOnCancelListener(dialog -> listener.onCancel())
                    .show();
        }
    }

    public static void removeFromFavorites(Activity activity, GalleryInfo galleryInfo,
                                           final EhClient.Callback<Void> listener) {
        EhDB.removeLocalFavorites(galleryInfo.gid);
        EhClient client = EhApplication.getEhClient(activity);
        EhRequest request = new EhRequest();
        request.setMethod(EhClient.METHOD_ADD_FAVORITES);
        request.setArgs(galleryInfo.gid, galleryInfo.token, -1, "");
        request.setCallback(new DelegateFavoriteCallback(listener, galleryInfo, null, -2));
        client.execute(request);
    }

    private static class DelegateFavoriteCallback implements EhClient.Callback<Void> {

        private final EhClient.Callback<Void> delegate;
        private final GalleryInfo info;
        private final String newFavoriteName;
        private final int slot;

        DelegateFavoriteCallback(EhClient.Callback<Void> delegate, GalleryInfo info,
                                 String newFavoriteName, int slot) {
            this.delegate = delegate;
            this.info = info;
            this.newFavoriteName = newFavoriteName;
            this.slot = slot;
        }

        @Override
        public void onSuccess(Void result) {
            info.favoriteName = newFavoriteName;
            info.favoriteSlot = slot;
            delegate.onSuccess(result);
            EhApplication.getFavouriteStatusRouter().modifyFavourites(info.gid, slot);
        }

        @Override
        public void onFailure(Exception e) {
            delegate.onFailure(e);
        }

        @Override
        public void onCancel() {
            delegate.onCancel();
        }
    }

    public static void startDownload(final MainActivity activity, final GalleryInfo galleryInfo, boolean forceDefault) {
        startDownload(activity, Collections.singletonList(galleryInfo), forceDefault);
    }

    // TODO Add context if activity and context are different style
    public static void startDownload(final MainActivity activity, final List<GalleryInfo> galleryInfos, boolean forceDefault) {
        try {
            android.util.Log.d("CommonOperations", "Starting download process for " + galleryInfos.size() + " galleries");
            
            // 🎯 新的延迟权限检查逻辑
            DeferredPermissionManager permissionManager = DeferredPermissionManager.getInstance(activity);
            
            if (permissionManager.shouldRequestDownloadPermissions(activity)) {
                android.util.Log.d("CommonOperations", "需要请求下载权限，显示权限对话框");
                
                DownloadPermissionDialog.showDownloadPermissionDialog(activity, new DownloadPermissionDialog.PermissionCallback() {
                    @Override
                    public void onPermissionGranted() {
                        android.util.Log.d("CommonOperations", "权限已授予，开始下载");
                        // 权限授予后，继续原有的下载逻辑
                        proceedWithDownload(activity, galleryInfos, forceDefault);
                    }
                    
                    @Override
                    public void onPermissionDenied() {
                        android.util.Log.d("CommonOperations", "权限被拒绝，取消下载");
                        activity.showTip("需要存储权限才能下载文件", BaseScene.LENGTH_SHORT);
                    }
                    
                    @Override
                    public void onPermissionSkipped() {
                        android.util.Log.d("CommonOperations", "用户跳过权限设置，尝试继续下载");
                        // 用户跳过权限，仍然尝试下载（可能会失败，但让用户知道原因）
                        proceedWithDownload(activity, galleryInfos, forceDefault);
                    }
                });
                
                return; // 等待用户权限操作，不继续执行下面的代码
            }
            
            // 权限已具备，直接执行下载
            android.util.Log.d("CommonOperations", "权限检查通过，直接开始下载");
            proceedWithDownload(activity, galleryInfos, forceDefault);
            
        } catch (Exception e) {
            android.util.Log.e("CommonOperations", "Unexpected error in startDownload", e);
            if (activity != null) {
                activity.showTip("下载设置出现异常", BaseScene.LENGTH_SHORT);
            }
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
    
    /**
     * 执行实际的下载逻辑 (从原startDownload方法中提取)
     */
    private static void proceedWithDownload(final MainActivity activity, final List<GalleryInfo> galleryInfos, boolean forceDefault) {
        try {

            // 验证输入参数
            if (activity == null) {
                android.util.Log.e("CommonOperations", "Activity is null");
                return;
            }

            if (galleryInfos == null || galleryInfos.isEmpty()) {
                android.util.Log.e("CommonOperations", "GalleryInfo list is null or empty");
                activity.showTip("没有可下载的项目", BaseScene.LENGTH_SHORT);
                return;
            }

            // 获取下载管理器
            final DownloadManager dm;
            try {
                dm = EhApplication.getDownloadManager(activity);
                if (dm == null) {
                    android.util.Log.e("CommonOperations", "DownloadManager is null");
                    activity.showTip("下载服务不可用", BaseScene.LENGTH_SHORT);
                    return;
                }
            } catch (Exception e) {
                android.util.Log.e("CommonOperations", "Error getting DownloadManager", e);
                activity.showTip("下载服务初始化失败", BaseScene.LENGTH_SHORT);
                return;
            }

            LongList toStart = new LongList();
            List<GalleryInfo> toAdd = new ArrayList<>();

            // 验证每个GalleryInfo
            for (GalleryInfo gi : galleryInfos) {
                if (gi == null) {
                    android.util.Log.w("CommonOperations", "Skipping null GalleryInfo");
                    continue;
                }
                if (gi.gid <= 0) {
                    android.util.Log.w("CommonOperations", "Skipping invalid GID: " + gi.gid);
                    continue;
                }

                try {
                    if (dm.containDownloadInfo(gi.gid)) {
                        toStart.add(gi.gid);
                        android.util.Log.d("CommonOperations", "Will restart download for GID: " + gi.gid);
                    } else {
                        toAdd.add(gi);
                        android.util.Log.d("CommonOperations", "Will add new download for GID: " + gi.gid);
                    }
                } catch (Exception e) {
                    android.util.Log.e("CommonOperations", "Error checking download info for GID: " + gi.gid, e);
                }
            }

            // 启动已存在的下载
            if (!toStart.isEmpty()) {
                try {
                    android.util.Log.d("CommonOperations", "Starting " + toStart.size() + " existing downloads");
                    Intent intent = new Intent(activity, DownloadService.class);
                    intent.setAction(DownloadService.ACTION_START_RANGE);
                    intent.putExtra(DownloadService.KEY_GID_LIST, toStart);
                    activity.startService(intent);
                    android.util.Log.d("CommonOperations", "Started existing downloads successfully");
                } catch (Exception e) {
                    android.util.Log.e("CommonOperations", "Error starting existing downloads", e);
                    activity.showTip("重启下载失败", BaseScene.LENGTH_SHORT);
                }
            }

            if (toAdd.isEmpty()) {
                activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT);
                return;
            }

            android.util.Log.d("CommonOperations", "Processing " + toAdd.size() + " new downloads");

        boolean justStart = forceDefault;
        String label = null;
        // Get default download label
        if (!justStart && Settings.getHasDefaultDownloadLabel()) {
            label = Settings.getDefaultDownloadLabel();
            justStart = label == null || dm.containLabel(label);
        }
        // If there is no other label, just use null label
        if (!justStart && 0 == dm.getLabelList().size()) {
            justStart = true;
            label = null;
        }

            if (justStart) {
                // Got default label
                try {
                    android.util.Log.d("CommonOperations", "Starting downloads with default label: " + label);
                    for (GalleryInfo gi : toAdd) {
                        try {
                            Intent intent = new Intent(activity, DownloadService.class);
                            intent.setAction(DownloadService.ACTION_START);
                            intent.putExtra(DownloadService.KEY_LABEL, label);
                            intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi);
                            activity.startService(intent);
                            android.util.Log.d("CommonOperations", "Started download service for GID: " + gi.gid);
                        } catch (Exception e) {
                            android.util.Log.e("CommonOperations", "Error starting download service for GID: " + gi.gid, e);
                        }
                    }
                    // Notify
                    activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT);
                    android.util.Log.d("CommonOperations", "Download process completed successfully");
                } catch (Exception e) {
                    android.util.Log.e("CommonOperations", "Error in justStart download process", e);
                    activity.showTip("下载过程出现异常", BaseScene.LENGTH_SHORT);
                }
        } else {
            // Let use chose label
            List<DownloadLabel> list = dm.getLabelList();
            final String[] items = new String[list.size() + 1];
            items[0] = activity.getString(R.string.default_download_label_name);
            for (int i = 0, n = list.size(); i < n; i++) {
                items[i + 1] = list.get(i).getLabel();
            }

            new ListCheckBoxDialogBuilder(activity, items,
                    (builder, dialog, position) -> {
                        String label1;
                        if (position == 0) {
                            label1 = null;
                        } else {
                            label1 = items[position];
                            if (!dm.containLabel(label1)) {
                                label1 = null;
                            }
                        }
                        // Start download
                        for (GalleryInfo gi : toAdd) {
                            Intent intent = new Intent(activity, DownloadService.class);
                            intent.setAction(DownloadService.ACTION_START);
                            intent.putExtra(DownloadService.KEY_LABEL, label1);
                            intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi);
                            activity.startService(intent);
                        }
                        // Save settings
                        if (builder.isChecked()) {
                            Settings.putHasDefaultDownloadLabel(true);
                            Settings.putDefaultDownloadLabel(label1);
                        } else {
                            Settings.putHasDefaultDownloadLabel(false);
                        }
                        // Notify
                        activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT);
                    }, activity.getString(R.string.remember_download_label), false)
                    .setTitle(R.string.download)
                    .show();
            }

            android.util.Log.d("CommonOperations", "Download setup completed successfully");

        } catch (Exception e) {
            android.util.Log.e("CommonOperations", "Unexpected error in startDownload", e);
            if (activity != null) {
                activity.showTip("下载设置出现异常", BaseScene.LENGTH_SHORT);
            }

            // 记录到Firebase Crashlytics
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void ensureNoMediaFile(UniFile file) {
        if (null == file) {
            return;
        }

        try {
            // 检查.nomedia文件是否已存在
            UniFile noMedia = file.subFile(".nomedia");
            if (noMedia != null && noMedia.isFile()) {
                android.util.Log.d("CommonOperations", ".nomedia file already exists");
                return;
            }

            // 创建.nomedia文件
            noMedia = file.createFile(".nomedia");
            if (null == noMedia) {
                android.util.Log.w("CommonOperations", "Failed to create .nomedia file");
                return;
            }

            // 验证文件创建成功
            if (noMedia.isFile()) {
                android.util.Log.d("CommonOperations", ".nomedia file created successfully at: " + noMedia.getUri());
            } else {
                android.util.Log.w("CommonOperations", ".nomedia file creation verification failed");
            }

        } catch (Exception e) {
            android.util.Log.w("CommonOperations", "Error ensuring .nomedia file", e);
            // 不要重新抛出异常，避免影响应用启动
        }
    }

    public static void removeNoMediaFile(UniFile file) {
        if (null == file) {
            return;
        }

        UniFile noMedia = file.subFile(".nomedia");
        if (null != noMedia && noMedia.isFile()) {
            noMedia.delete();
        }
    }
}
