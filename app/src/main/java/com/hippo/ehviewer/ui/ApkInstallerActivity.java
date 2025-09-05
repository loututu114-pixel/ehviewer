package com.hippo.ehviewer.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.hippo.ehviewer.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * APK安装器界面
 * 支持APK文件解析、预览和安装
 */
public class ApkInstallerActivity extends AppCompatActivity {

    private static final String TAG = "ApkInstallerActivity";
    private static final int REQUEST_INSTALL_PACKAGES = 1001;

    // UI组件
    private ImageView mAppIcon;
    private TextView mAppName;
    private TextView mAppVersion;
    private TextView mAppPackage;
    private TextView mAppSize;
    private TextView mAppPermissions;
    private Button mInstallButton;
    private Button mCancelButton;
    private ProgressBar mProgressBar;
    private TextView mStatusText;

    // 数据
    private Uri mApkUri;
    private File mTempApkFile;
    private PackageInfo mPackageInfo;
    private String mApkFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apk_installer);

        initViews();
        setupToolbar();
        handleIntent();
    }

    private void initViews() {
        mAppIcon = findViewById(R.id.app_icon);
        mAppName = findViewById(R.id.app_name);
        mAppVersion = findViewById(R.id.app_version);
        mAppPackage = findViewById(R.id.app_package);
        mAppSize = findViewById(R.id.app_size);
        mAppPermissions = findViewById(R.id.app_permissions);
        mInstallButton = findViewById(R.id.install_button);
        mCancelButton = findViewById(R.id.cancel_button);
        mProgressBar = findViewById(R.id.progress_bar);
        mStatusText = findViewById(R.id.status_text);

        mInstallButton.setOnClickListener(v -> installApk());
        mCancelButton.setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("APK安装器");
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            mApkUri = intent.getData();
            if (mApkUri != null) {
                parseApkFile();
            } else {
                showError("无效的APK文件");
            }
        }
    }

    private void parseApkFile() {
        showProgress("正在解析APK文件...");
        
        new Thread(() -> {
            try {
                // 处理不同类型的URI
                if ("content".equals(mApkUri.getScheme())) {
                    // 从content URI复制到临时文件
                    mTempApkFile = copyToTempFile();
                    mApkFilePath = mTempApkFile.getAbsolutePath();
                } else if ("file".equals(mApkUri.getScheme())) {
                    // 直接使用文件路径
                    mApkFilePath = mApkUri.getPath();
                } else {
                    runOnUiThread(() -> showError("不支持的文件类型"));
                    return;
                }

                // 解析APK包信息
                PackageManager pm = getPackageManager();
                mPackageInfo = pm.getPackageArchiveInfo(mApkFilePath, PackageManager.GET_PERMISSIONS);
                
                if (mPackageInfo != null) {
                    runOnUiThread(this::displayApkInfo);
                } else {
                    runOnUiThread(() -> showError("无法解析APK文件"));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "解析APK失败", e);
                runOnUiThread(() -> showError("解析APK失败: " + e.getMessage()));
            }
        }).start();
    }

    private File copyToTempFile() throws IOException {
        File tempFile = new File(getCacheDir(), "temp_install.apk");
        
        try (InputStream inputStream = getContentResolver().openInputStream(mApkUri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            
            if (inputStream == null) {
                throw new IOException("无法打开输入流");
            }
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile;
    }

    private void displayApkInfo() {
        hideProgress();
        
        try {
            // 获取应用信息
            ApplicationInfo appInfo = mPackageInfo.applicationInfo;
            appInfo.sourceDir = mApkFilePath;
            appInfo.publicSourceDir = mApkFilePath;
            
            PackageManager pm = getPackageManager();
            
            // 显示应用图标
            Drawable icon = appInfo.loadIcon(pm);
            mAppIcon.setImageDrawable(icon);
            
            // 显示应用名称
            String appName = appInfo.loadLabel(pm).toString();
            mAppName.setText(appName);
            
            // 显示版本信息
            String versionName = mPackageInfo.versionName != null ? mPackageInfo.versionName : "未知";
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? 
                mPackageInfo.getLongVersionCode() : mPackageInfo.versionCode;
            mAppVersion.setText(String.format("版本: %s (%d)", versionName, versionCode));
            
            // 显示包名
            mAppPackage.setText("包名: " + mPackageInfo.packageName);
            
            // 显示文件大小
            File apkFile = new File(mApkFilePath);
            String fileSize = Formatter.formatFileSize(this, apkFile.length());
            mAppSize.setText("大小: " + fileSize);
            
            // 显示权限信息
            displayPermissions();
            
            // 检查是否已安装
            checkInstallationStatus();
            
        } catch (Exception e) {
            Log.e(TAG, "显示APK信息失败", e);
            showError("显示APK信息失败: " + e.getMessage());
        }
    }

    private void displayPermissions() {
        if (mPackageInfo.requestedPermissions != null && mPackageInfo.requestedPermissions.length > 0) {
            StringBuilder permissions = new StringBuilder("权限:\n");
            for (String permission : mPackageInfo.requestedPermissions) {
                String permissionName = getPermissionName(permission);
                permissions.append("• ").append(permissionName).append("\n");
            }
            mAppPermissions.setText(permissions.toString());
        } else {
            mAppPermissions.setText("权限: 无特殊权限");
        }
    }

    private String getPermissionName(String permission) {
        switch (permission) {
            case Manifest.permission.INTERNET:
                return "网络访问";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "读取存储";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "写入存储";
            case Manifest.permission.CAMERA:
                return "摄像头";
            case Manifest.permission.RECORD_AUDIO:
                return "录音";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "精确位置";
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "大致位置";
            case Manifest.permission.READ_PHONE_STATE:
                return "读取设备信息";
            case Manifest.permission.CALL_PHONE:
                return "拨打电话";
            case Manifest.permission.READ_CONTACTS:
                return "读取联系人";
            case Manifest.permission.WRITE_CONTACTS:
                return "写入联系人";
            case Manifest.permission.READ_SMS:
                return "读取短信";
            case Manifest.permission.SEND_SMS:
                return "发送短信";
            default:
                return permission.substring(permission.lastIndexOf('.') + 1);
        }
    }

    private void checkInstallationStatus() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo installedPackage = pm.getPackageInfo(mPackageInfo.packageName, 0);
            
            long installedVersionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? 
                installedPackage.getLongVersionCode() : installedPackage.versionCode;
            long apkVersionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? 
                mPackageInfo.getLongVersionCode() : mPackageInfo.versionCode;
            
            if (installedVersionCode == apkVersionCode) {
                mInstallButton.setText("重新安装");
            } else if (installedVersionCode < apkVersionCode) {
                mInstallButton.setText("升级安装");
            } else {
                mInstallButton.setText("降级安装");
                mInstallButton.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            }
            
        } catch (PackageManager.NameNotFoundException e) {
            // 应用未安装
            mInstallButton.setText("安装");
        }
    }

    private void installApk() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                requestInstallPermission();
                return;
            }
        }
        
        performInstallation();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestInstallPermission() {
        new AlertDialog.Builder(this)
            .setTitle("需要安装权限")
            .setMessage("为了安装APK文件，需要允许此应用安装未知来源的应用。")
            .setPositiveButton("去设置", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_INSTALL_PACKAGES);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void performInstallation() {
        try {
            showProgress("正在安装应用...");
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 使用FileProvider
                File apkFile = new File(mApkFilePath);
                apkUri = FileProvider.getUriForFile(
                    this, 
                    getPackageName() + ".fileprovider", 
                    apkFile
                );
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                // Android 7.0以下直接使用文件URI
                apkUri = Uri.fromFile(new File(mApkFilePath));
            }
            
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            startActivity(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "安装APK失败", e);
            hideProgress();
            showError("安装失败: " + e.getMessage());
        }
    }

    private void showProgress(String message) {
        mProgressBar.setVisibility(View.VISIBLE);
        mStatusText.setVisibility(View.VISIBLE);
        mStatusText.setText(message);
        mInstallButton.setEnabled(false);
    }

    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
        mStatusText.setVisibility(View.GONE);
        mInstallButton.setEnabled(true);
    }

    private void showError(String message) {
        hideProgress();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        new AlertDialog.Builder(this)
            .setTitle("错误")
            .setMessage(message)
            .setPositiveButton("确定", (dialog, which) -> finish())
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_PACKAGES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls()) {
                    performInstallation();
                } else {
                    Toast.makeText(this, "未获得安装权限，无法安装APK", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.apk_installer_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 只在有包信息时显示相关菜单
        boolean hasPackageInfo = mPackageInfo != null;
        MenuItem viewAppItem = menu.findItem(R.id.action_view_app_info);
        MenuItem uninstallAppItem = menu.findItem(R.id.action_uninstall_app);
        MenuItem manageAppsItem = menu.findItem(R.id.action_manage_apps);
        
        if (viewAppItem != null) {
            viewAppItem.setVisible(hasPackageInfo);
        }
        if (uninstallAppItem != null) {
            uninstallAppItem.setVisible(hasPackageInfo && isAppInstalled());
        }
        if (manageAppsItem != null) {
            manageAppsItem.setVisible(true); // 始终显示应用管理
        }
        
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_view_app_info) {
            viewInstalledAppInfo();
            return true;
        } else if (itemId == R.id.action_uninstall_app) {
            uninstallApp();
            return true;
        } else if (itemId == R.id.action_manage_apps) {
            openAppManager();
            return true;
        } else if (itemId == R.id.action_extract_apk) {
            extractInstalledApk();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * 检查应用是否已安装
     */
    private boolean isAppInstalled() {
        if (mPackageInfo == null) return false;
        
        try {
            getPackageManager().getPackageInfo(mPackageInfo.packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 查看已安装应用的详细信息
     */
    private void viewInstalledAppInfo() {
        if (mPackageInfo == null) return;
        
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + mPackageInfo.packageName));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开应用信息", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 卸载应用
     */
    private void uninstallApp() {
        if (mPackageInfo == null || !isAppInstalled()) return;
        
        new AlertDialog.Builder(this)
            .setTitle("卸载应用")
            .setMessage("确定要卸载 \"" + mPackageInfo.packageName + "\" 吗？")
            .setPositiveButton("卸载", (dialog, which) -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_DELETE);
                    intent.setData(Uri.parse("package:" + mPackageInfo.packageName));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "无法卸载应用", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 打开应用管理器
     */
    private void openAppManager() {
        Intent intent = new Intent(this, AppManagerActivity.class);
        startActivity(intent);
    }

    /**
     * 提取已安装应用的APK文件
     */
    private void extractInstalledApk() {
        if (mPackageInfo == null || !isAppInstalled()) return;
        
        new AlertDialog.Builder(this)
            .setTitle("提取APK")
            .setMessage("将应用APK文件复制到EhViewer目录？")
            .setPositiveButton("提取", (dialog, which) -> performApkExtraction())
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 执行APK提取
     */
    private void performApkExtraction() {
        new Thread(() -> {
            try {
                runOnUiThread(() -> showProgress("正在提取APK..."));
                
                // 获取已安装应用的APK路径
                PackageManager pm = getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(mPackageInfo.packageName, 0);
                File sourceApk = new File(appInfo.sourceDir);
                
                // 创建目标文件
                File ehViewerDir = new File(getExternalFilesDir(null), "ExtractedAPKs");
                if (!ehViewerDir.exists()) {
                    ehViewerDir.mkdirs();
                }
                
                String appName = appInfo.loadLabel(pm).toString().replaceAll("[^a-zA-Z0-9]", "_");
                File targetApk = new File(ehViewerDir, appName + "_" + mPackageInfo.versionName + ".apk");
                
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
                    showError("提取APK失败: " + e.getMessage());
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理临时文件
        if (mTempApkFile != null && mTempApkFile.exists()) {
            mTempApkFile.delete();
        }
    }
}