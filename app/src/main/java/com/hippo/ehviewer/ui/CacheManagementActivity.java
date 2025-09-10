package com.hippo.ehviewer.ui;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.slider.Slider;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.offline.OfflineCacheManager;

import java.util.Locale;

/**
 * 缓存管理界面
 * 提供友好的缓存清理和管理功能
 */
public class CacheManagementActivity extends AppCompatActivity {
    
    private OfflineCacheManager cacheManager;
    
    // UI组件
    private TextView tvTotalCacheSize;
    private TextView tvImageCacheSize;
    private TextView tvVideoCacheSize;
    private TextView tvMangaCacheSize;
    private TextView tvNovelCacheSize;
    private TextView tvOfflineStorageInfo;
    
    private ProgressBar pbCacheUsage;
    private ProgressBar pbClearing;
    
    private Switch switchOfflineMode;
    private Switch switchAutoCache;
    private Switch switchSmartClean;
    
    private RadioGroup rgCacheStrategy;
    private Slider sliderMaxCacheSize;
    private TextView tvMaxCacheSize;
    
    private LinearLayout llClearingProgress;
    private TextView tvClearingStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache_management);
        
        cacheManager = OfflineCacheManager.getInstance(this);
        
        initViews();
        setupListeners();
        updateCacheInfo();
    }
    
    private void initViews() {
        // 工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("离线缓存管理");
        
        // 缓存大小显示
        tvTotalCacheSize = findViewById(R.id.tv_total_cache_size);
        tvImageCacheSize = findViewById(R.id.tv_image_cache_size);
        tvVideoCacheSize = findViewById(R.id.tv_video_cache_size);
        tvMangaCacheSize = findViewById(R.id.tv_manga_cache_size);
        tvNovelCacheSize = findViewById(R.id.tv_novel_cache_size);
        tvOfflineStorageInfo = findViewById(R.id.tv_offline_storage_info);
        
        // 进度条
        pbCacheUsage = findViewById(R.id.pb_cache_usage);
        pbClearing = findViewById(R.id.pb_clearing);
        llClearingProgress = findViewById(R.id.ll_clearing_progress);
        tvClearingStatus = findViewById(R.id.tv_clearing_status);
        
        // 开关
        switchOfflineMode = findViewById(R.id.switch_offline_mode);
        switchAutoCache = findViewById(R.id.switch_auto_cache);
        switchSmartClean = findViewById(R.id.switch_smart_clean);
        
        // 缓存策略
        rgCacheStrategy = findViewById(R.id.rg_cache_strategy);
        
        // 缓存大小滑块
        sliderMaxCacheSize = findViewById(R.id.slider_max_cache_size);
        tvMaxCacheSize = findViewById(R.id.tv_max_cache_size);
        
        // 设置初始值
        switchOfflineMode.setChecked(cacheManager.isOfflineMode());
        updateStrategySelection();
    }
    
    private void setupListeners() {
        // 离线模式开关
        switchOfflineMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cacheManager.setOfflineMode(isChecked);
            showToast(isChecked ? "已开启离线模式" : "已关闭离线模式");
            updateCacheInfo();
        });
        
        // 自动缓存开关
        switchAutoCache.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cacheManager.setCacheStrategy(OfflineCacheManager.CacheStrategy.SMART);
            } else {
                cacheManager.setCacheStrategy(OfflineCacheManager.CacheStrategy.MANUAL);
            }
        });
        
        // 智能清理开关
        switchSmartClean.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("cache_settings", MODE_PRIVATE)
                .edit()
                .putBoolean("smart_clean", isChecked)
                .apply();
        });
        
        // 缓存策略选择
        rgCacheStrategy.setOnCheckedChangeListener((group, checkedId) -> {
            OfflineCacheManager.CacheStrategy strategy;
            if (checkedId == R.id.rb_wifi_only) {
                strategy = OfflineCacheManager.CacheStrategy.WIFI_ONLY;
            } else if (checkedId == R.id.rb_always) {
                strategy = OfflineCacheManager.CacheStrategy.ALWAYS;
            } else if (checkedId == R.id.rb_smart) {
                strategy = OfflineCacheManager.CacheStrategy.SMART;
            } else {
                strategy = OfflineCacheManager.CacheStrategy.MANUAL;
            }
            cacheManager.setCacheStrategy(strategy);
        });
        
        // 最大缓存大小滑块
        sliderMaxCacheSize.addOnChangeListener((slider, value, fromUser) -> {
            int sizeMB = (int) value;
            tvMaxCacheSize.setText(String.format(Locale.getDefault(), 
                "最大缓存: %d MB", sizeMB));
            
            if (fromUser) {
                getSharedPreferences("cache_settings", MODE_PRIVATE)
                    .edit()
                    .putInt("max_cache_mb", sizeMB)
                    .apply();
            }
        });
        
        // 清理按钮点击事件
        findViewById(R.id.btn_clear_images).setOnClickListener(v -> 
            confirmClearCache(OfflineCacheManager.CacheType.IMAGE));
        
        findViewById(R.id.btn_clear_videos).setOnClickListener(v -> 
            confirmClearCache(OfflineCacheManager.CacheType.VIDEO));
        
        findViewById(R.id.btn_clear_manga).setOnClickListener(v -> 
            confirmClearCache(OfflineCacheManager.CacheType.MANGA));
        
        findViewById(R.id.btn_clear_novels).setOnClickListener(v -> 
            confirmClearCache(OfflineCacheManager.CacheType.NOVEL));
        
        findViewById(R.id.btn_clear_all).setOnClickListener(v -> 
            confirmClearAllCache());
        
        findViewById(R.id.btn_smart_clean).setOnClickListener(v -> 
            performSmartClean());
    }
    
    /**
     * 更新缓存信息显示
     */
    private void updateCacheInfo() {
        new AsyncTask<Void, Void, CacheInfo>() {
            @Override
            protected CacheInfo doInBackground(Void... params) {
                CacheInfo info = new CacheInfo();
                info.totalSize = cacheManager.getTotalCacheSize();
                info.imageSize = cacheManager.getCacheSize(OfflineCacheManager.CacheType.IMAGE);
                info.videoSize = cacheManager.getCacheSize(OfflineCacheManager.CacheType.VIDEO);
                info.mangaSize = cacheManager.getCacheSize(OfflineCacheManager.CacheType.MANGA);
                info.novelSize = cacheManager.getCacheSize(OfflineCacheManager.CacheType.NOVEL);
                return info;
            }
            
            @Override
            protected void onPostExecute(CacheInfo info) {
                // 更新UI
                tvTotalCacheSize.setText(formatFileSize(info.totalSize));
                tvImageCacheSize.setText(formatFileSize(info.imageSize));
                tvVideoCacheSize.setText(formatFileSize(info.videoSize));
                tvMangaCacheSize.setText(formatFileSize(info.mangaSize));
                tvNovelCacheSize.setText(formatFileSize(info.novelSize));
                
                // 更新进度条
                long maxCache = getSharedPreferences("cache_settings", MODE_PRIVATE)
                    .getInt("max_cache_mb", 500) * 1024L * 1024L;
                int progress = (int) ((info.totalSize * 100) / maxCache);
                pbCacheUsage.setProgress(Math.min(progress, 100));
                
                // 更新存储信息
                updateStorageInfo();
                
                // 根据使用情况改变颜色
                if (progress > 90) {
                    pbCacheUsage.setProgressTintList(
                        getResources().getColorStateList(android.R.color.holo_red_light));
                } else if (progress > 70) {
                    pbCacheUsage.setProgressTintList(
                        getResources().getColorStateList(android.R.color.holo_orange_light));
                } else {
                    pbCacheUsage.setProgressTintList(
                        getResources().getColorStateList(android.R.color.holo_green_light));
                }
            }
        }.execute();
    }
    
    /**
     * 更新存储信息
     */
    private void updateStorageInfo() {
        long freeSpace = getExternalFilesDir(null).getFreeSpace();
        long totalSpace = getExternalFilesDir(null).getTotalSpace();
        long usedSpace = totalSpace - freeSpace;
        
        String info = String.format(Locale.getDefault(),
            "存储空间: %s / %s (剩余 %s)",
            formatFileSize(usedSpace),
            formatFileSize(totalSpace),
            formatFileSize(freeSpace));
        
        tvOfflineStorageInfo.setText(info);
    }
    
    /**
     * 确认清理缓存
     */
    private void confirmClearCache(OfflineCacheManager.CacheType type) {
        String typeName = getTypeName(type);
        new AlertDialog.Builder(this)
            .setTitle("清理" + typeName + "缓存")
            .setMessage("确定要清理所有" + typeName + "缓存吗？\n离线内容将被删除。")
            .setPositiveButton("清理", (dialog, which) -> clearCache(type))
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 确认清理所有缓存
     */
    private void confirmClearAllCache() {
        new AlertDialog.Builder(this)
            .setTitle("清理所有缓存")
            .setMessage("确定要清理所有缓存吗？\n所有离线内容将被删除，此操作不可恢复。")
            .setPositiveButton("全部清理", (dialog, which) -> clearAllCache())
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 清理指定类型缓存
     */
    private void clearCache(OfflineCacheManager.CacheType type) {
        llClearingProgress.setVisibility(View.VISIBLE);
        tvClearingStatus.setText("正在清理" + getTypeName(type) + "缓存...");
        
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    cacheManager.clearCache(type);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                llClearingProgress.setVisibility(View.GONE);
                if (success) {
                    showToast(getTypeName(type) + "缓存已清理");
                    updateCacheInfo();
                } else {
                    showToast("清理失败");
                }
            }
        }.execute();
    }
    
    /**
     * 清理所有缓存
     */
    private void clearAllCache() {
        llClearingProgress.setVisibility(View.VISIBLE);
        tvClearingStatus.setText("正在清理所有缓存...");
        
        new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    OfflineCacheManager.CacheType[] types = 
                        OfflineCacheManager.CacheType.values();
                    
                    for (int i = 0; i < types.length; i++) {
                        cacheManager.clearCache(types[i]);
                        publishProgress((i + 1) * 100 / types.length);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void onProgressUpdate(Integer... values) {
                pbClearing.setProgress(values[0]);
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                llClearingProgress.setVisibility(View.GONE);
                if (success) {
                    showToast("所有缓存已清理");
                    updateCacheInfo();
                } else {
                    showToast("清理失败");
                }
            }
        }.execute();
    }
    
    /**
     * 执行智能清理
     */
    private void performSmartClean() {
        new AlertDialog.Builder(this)
            .setTitle("智能清理")
            .setMessage("将自动清理最旧的缓存文件，保留最近使用的内容。是否继续？")
            .setPositiveButton("开始清理", (dialog, which) -> {
                llClearingProgress.setVisibility(View.VISIBLE);
                tvClearingStatus.setText("正在智能清理...");
                
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... params) {
                        try {
                            cacheManager.smartCleanCache();
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                    
                    @Override
                    protected void onPostExecute(Boolean success) {
                        llClearingProgress.setVisibility(View.GONE);
                        if (success) {
                            showToast("智能清理完成");
                            updateCacheInfo();
                        } else {
                            showToast("清理失败");
                        }
                    }
                }.execute();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 更新策略选择
     */
    private void updateStrategySelection() {
        OfflineCacheManager.CacheStrategy strategy = cacheManager.getCacheStrategy();
        int checkedId = R.id.rb_smart;
        
        switch (strategy) {
            case WIFI_ONLY:
                checkedId = R.id.rb_wifi_only;
                break;
            case ALWAYS:
                checkedId = R.id.rb_always;
                break;
            case SMART:
                checkedId = R.id.rb_smart;
                break;
            case MANUAL:
                checkedId = R.id.rb_manual;
                break;
        }
        
        rgCacheStrategy.check(checkedId);
    }
    
    /**
     * 获取类型名称
     */
    private String getTypeName(OfflineCacheManager.CacheType type) {
        switch (type) {
            case IMAGE:
                return "图片";
            case VIDEO:
                return "视频";
            case MANGA:
                return "漫画";
            case NOVEL:
                return "小说";
            case TEMP:
                return "临时";
            default:
                return "";
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 缓存信息
     */
    private static class CacheInfo {
        long totalSize;
        long imageSize;
        long videoSize;
        long mangaSize;
        long novelSize;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateCacheInfo();
    }
}