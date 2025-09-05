package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SearchView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.adapter.InstalledAppsAdapter;
import com.hippo.ehviewer.ui.model.AppInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 应用管理器
 * 显示和管理所有已安装的应用
 */
public class AppManagerActivity extends AppCompatActivity {

    private static final String TAG = "AppManagerActivity";
    
    // UI组件
    private ListView mAppsListView;
    private ProgressBar mProgressBar;
    private TextView mStatusText;
    private TextView mAppsCountText;
    private SearchView mSearchView;
    
    // 数据
    private List<AppInfo> mAllApps;
    private List<AppInfo> mFilteredApps;
    private InstalledAppsAdapter mAdapter;
    private String mCurrentSearchQuery = "";
    private int mSortMode = SORT_BY_NAME; // 排序模式
    
    // 排序模式常量
    private static final int SORT_BY_NAME = 0;
    private static final int SORT_BY_SIZE = 1;
    private static final int SORT_BY_INSTALL_TIME = 2;
    private static final int SORT_BY_UPDATE_TIME = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);

        initViews();
        setupToolbar();
        loadInstalledApps();
    }

    private void initViews() {
        mAppsListView = findViewById(R.id.apps_list_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mStatusText = findViewById(R.id.status_text);
        mAppsCountText = findViewById(R.id.apps_count_text);

        mAllApps = new ArrayList<>();
        mFilteredApps = new ArrayList<>();
        mAdapter = new InstalledAppsAdapter(this, mFilteredApps);
        mAppsListView.setAdapter(mAdapter);

        mAppsListView.setOnItemClickListener(this::onAppItemClick);
        mAppsListView.setOnItemLongClickListener(this::onAppItemLongClick);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("应用管理器");
        }
    }

    private void loadInstalledApps() {
        showProgress("正在加载应用列表...");
        
        new Thread(() -> {
            try {
                List<AppInfo> apps = getInstalledApps();
                
                runOnUiThread(() -> {
                    mAllApps.clear();
                    mAllApps.addAll(apps);
                    applyFilter();
                    hideProgress();
                    updateAppsCount();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "加载应用列表失败", e);
                runOnUiThread(() -> {
                    hideProgress();
                    showError("加载应用列表失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private List<AppInfo> getInstalledApps() {
        List<AppInfo> apps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        
        for (PackageInfo packageInfo : installedPackages) {
            try {
                AppInfo appInfo = new AppInfo();
                ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                
                // 基本信息
                appInfo.packageName = packageInfo.packageName;
                appInfo.appName = applicationInfo.loadLabel(pm).toString();
                appInfo.versionName = packageInfo.versionName;
                appInfo.versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? 
                    packageInfo.getLongVersionCode() : packageInfo.versionCode;
                    
                // 图标
                appInfo.icon = applicationInfo.loadIcon(pm);
                
                // 安装时间和更新时间
                appInfo.firstInstallTime = packageInfo.firstInstallTime;
                appInfo.lastUpdateTime = packageInfo.lastUpdateTime;
                
                // APK文件信息
                File apkFile = new File(applicationInfo.sourceDir);
                appInfo.apkSize = apkFile.length();
                appInfo.apkPath = applicationInfo.sourceDir;
                
                // 应用类型
                appInfo.isSystemApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                appInfo.isUserApp = !appInfo.isSystemApp;
                
                // 可启动性
                Intent launchIntent = pm.getLaunchIntentForPackage(packageInfo.packageName);
                appInfo.canLaunch = launchIntent != null;
                
                apps.add(appInfo);
                
            } catch (Exception e) {
                Log.w(TAG, "获取应用信息失败: " + packageInfo.packageName, e);
            }
        }
        
        // 排序
        sortApps(apps);
        
        return apps;
    }

    private void sortApps(List<AppInfo> apps) {
        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo app1, AppInfo app2) {
                switch (mSortMode) {
                    case SORT_BY_SIZE:
                        return Long.compare(app2.apkSize, app1.apkSize);
                    case SORT_BY_INSTALL_TIME:
                        return Long.compare(app2.firstInstallTime, app1.firstInstallTime);
                    case SORT_BY_UPDATE_TIME:
                        return Long.compare(app2.lastUpdateTime, app1.lastUpdateTime);
                    case SORT_BY_NAME:
                    default:
                        return app1.appName.compareToIgnoreCase(app2.appName);
                }
            }
        });
    }

    private void applyFilter() {
        mFilteredApps.clear();
        
        if (mCurrentSearchQuery.isEmpty()) {
            mFilteredApps.addAll(mAllApps);
        } else {
            String query = mCurrentSearchQuery.toLowerCase();
            for (AppInfo app : mAllApps) {
                if (app.appName.toLowerCase().contains(query) ||
                    app.packageName.toLowerCase().contains(query)) {
                    mFilteredApps.add(app);
                }
            }
        }
        
        mAdapter.notifyDataSetChanged();
        updateAppsCount();
    }

    private void updateAppsCount() {
        int totalCount = mAllApps.size();
        int filteredCount = mFilteredApps.size();
        
        String countText;
        if (mCurrentSearchQuery.isEmpty()) {
            countText = String.format("共 %d 个应用", totalCount);
        } else {
            countText = String.format("找到 %d 个应用 (共 %d 个)", filteredCount, totalCount);
        }
        
        mAppsCountText.setText(countText);
    }

    private void onAppItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppInfo appInfo = mFilteredApps.get(position);
        showAppOptionsDialog(appInfo);
    }

    private boolean onAppItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        AppInfo appInfo = mFilteredApps.get(position);
        showAppDetailsDialog(appInfo);
        return true;
    }

    private void showAppOptionsDialog(AppInfo appInfo) {
        List<String> options = new ArrayList<>();
        options.add("启动应用");
        options.add("应用信息");
        options.add("提取APK");
        
        if (!appInfo.isSystemApp) {
            options.add("卸载");
        }
        
        String[] optionArray = options.toArray(new String[0]);
        
        new AlertDialog.Builder(this)
            .setTitle(appInfo.appName)
            .setItems(optionArray, (dialog, which) -> {
                switch (which) {
                    case 0: // 启动应用
                        launchApp(appInfo);
                        break;
                    case 1: // 应用信息
                        viewAppInfo(appInfo);
                        break;
                    case 2: // 提取APK
                        extractApk(appInfo);
                        break;
                    case 3: // 卸载
                        if (!appInfo.isSystemApp) {
                            uninstallApp(appInfo);
                        }
                        break;
                }
            })
            .show();
    }

    private void showAppDetailsDialog(AppInfo appInfo) {
        StringBuilder details = new StringBuilder();
        details.append("应用名称: ").append(appInfo.appName).append("\n");
        details.append("包名: ").append(appInfo.packageName).append("\n");
        details.append("版本: ").append(appInfo.versionName).append(" (").append(appInfo.versionCode).append(")\n");
        details.append("APK大小: ").append(Formatter.formatFileSize(this, appInfo.apkSize)).append("\n");
        details.append("安装时间: ").append(new java.util.Date(appInfo.firstInstallTime)).append("\n");
        details.append("更新时间: ").append(new java.util.Date(appInfo.lastUpdateTime)).append("\n");
        details.append("应用类型: ").append(appInfo.isSystemApp ? "系统应用" : "用户应用").append("\n");
        details.append("APK路径: ").append(appInfo.apkPath);

        new AlertDialog.Builder(this)
            .setTitle("应用详情")
            .setMessage(details.toString())
            .setPositiveButton("确定", null)
            .show();
    }

    private void launchApp(AppInfo appInfo) {
        try {
            if (appInfo.canLaunch) {
                Intent intent = getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
                if (intent != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "应用无法启动", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "应用无法启动", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void viewAppInfo(AppInfo appInfo) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + appInfo.packageName));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开应用信息", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractApk(AppInfo appInfo) {
        new AlertDialog.Builder(this)
            .setTitle("提取APK")
            .setMessage("将 \"" + appInfo.appName + "\" 的APK文件提取到EhViewer目录？")
            .setPositiveButton("提取", (dialog, which) -> performApkExtraction(appInfo))
            .setNegativeButton("取消", null)
            .show();
    }

    private void performApkExtraction(AppInfo appInfo) {
        new Thread(() -> {
            try {
                runOnUiThread(() -> showProgress("正在提取APK..."));
                
                File sourceApk = new File(appInfo.apkPath);
                
                // 创建目标目录
                File extractDir = new File(getExternalFilesDir(null), "ExtractedAPKs");
                if (!extractDir.exists()) {
                    extractDir.mkdirs();
                }
                
                String safeAppName = appInfo.appName.replaceAll("[^a-zA-Z0-9]", "_");
                File targetApk = new File(extractDir, safeAppName + "_" + appInfo.versionName + ".apk");
                
                // 复制APK文件
                try (FileInputStream in = new FileInputStream(sourceApk);
                     FileOutputStream out = new FileOutputStream(targetApk)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(this, "APK已提取到: " + targetApk.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "提取APK失败", e);
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(this, "提取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void uninstallApp(AppInfo appInfo) {
        new AlertDialog.Builder(this)
            .setTitle("卸载应用")
            .setMessage("确定要卸载 \"" + appInfo.appName + "\" 吗？")
            .setPositiveButton("卸载", (dialog, which) -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_DELETE);
                    intent.setData(Uri.parse("package:" + appInfo.packageName));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "无法卸载应用", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showProgress(String message) {
        mProgressBar.setVisibility(View.VISIBLE);
        mStatusText.setVisibility(View.VISIBLE);
        mStatusText.setText(message);
    }

    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
        mStatusText.setVisibility(View.GONE);
    }

    private void showError(String message) {
        hideProgress();
        new AlertDialog.Builder(this)
            .setTitle("错误")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_manager_menu, menu);
        
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setQueryHint("搜索应用...");
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mCurrentSearchQuery = newText.trim();
                applyFilter();
                return true;
            }
        });
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_refresh) {
            loadInstalledApps();
            return true;
        } else if (itemId == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (itemId == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (itemId == R.id.action_batch_export) {
            showBatchExportDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        String[] sortOptions = {"按名称", "按大小", "按安装时间", "按更新时间"};
        
        new AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setSingleChoiceItems(sortOptions, mSortMode, (dialog, which) -> {
                mSortMode = which;
                sortApps(mAllApps);
                applyFilter();
                dialog.dismiss();
            })
            .show();
    }

    private void showFilterDialog() {
        // TODO: 实现过滤功能 (用户应用、系统应用等)
        Toast.makeText(this, "过滤功能开发中...", Toast.LENGTH_SHORT).show();
    }

    private void showBatchExportDialog() {
        // TODO: 实现批量导出功能
        Toast.makeText(this, "批量导出功能开发中...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从应用信息页面返回时刷新列表
        if (mAllApps.size() > 0) {
            loadInstalledApps();
        }
    }
}