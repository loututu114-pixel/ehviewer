package com.hippo.ehviewer.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.hippo.ehviewer.R;

/**
 * 系统设置管理界面
 * 帮助用户配置默认应用、权限等系统级设置
 */
public class SystemSettingsActivity extends AppCompatActivity {

    private static final String TAG = "SystemSettingsActivity";

    // 请求码
    private static final int REQUEST_SET_DEFAULT_BROWSER = 1001;
    private static final int REQUEST_MANAGE_ALL_FILES = 1002;
    private static final int REQUEST_INSTALL_PACKAGES = 1003;
    private static final int REQUEST_SYSTEM_ALERT_WINDOW = 1004;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATION = 1005;

    // UI组件
    private CardView mDefaultBrowserCard;
    private TextView mDefaultBrowserStatus;
    private Button mSetDefaultBrowserButton;

    private CardView mFileManagerCard;
    private TextView mFileManagerStatus;
    private Button mSetFileManagerButton;

    private CardView mPermissionsCard;
    private LinearLayout mPermissionsLayout;

    private CardView mSystemIntegrationCard;
    private Switch mBrowserAliasSwitch;
    private Switch mFileManagerAliasSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_settings);

        initViews();
        setupToolbar();
        checkCurrentSettings();
    }

    private void initViews() {
        // 默认浏览器设置
        mDefaultBrowserCard = findViewById(R.id.default_browser_card);
        mDefaultBrowserStatus = findViewById(R.id.default_browser_status);
        mSetDefaultBrowserButton = findViewById(R.id.set_default_browser_button);

        // 文件管理器设置
        mFileManagerCard = findViewById(R.id.file_manager_card);
        mFileManagerStatus = findViewById(R.id.file_manager_status);
        mSetFileManagerButton = findViewById(R.id.set_file_manager_button);

        // 权限设置
        mPermissionsCard = findViewById(R.id.permissions_card);
        mPermissionsLayout = findViewById(R.id.permissions_layout);

        // 系统集成设置
        mSystemIntegrationCard = findViewById(R.id.system_integration_card);
        mBrowserAliasSwitch = findViewById(R.id.browser_alias_switch);
        mFileManagerAliasSwitch = findViewById(R.id.file_manager_alias_switch);

        setupListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("系统设置管理");
        }
    }

    private void setupListeners() {
        // 设置默认浏览器
        mSetDefaultBrowserButton.setOnClickListener(v -> setDefaultBrowser());

        // 设置文件管理器
        mSetFileManagerButton.setOnClickListener(v -> setFileManagerDefault());

        // 浏览器别名开关
        mBrowserAliasSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleBrowserAlias(isChecked);
        });

        // 文件管理器别名开关
        mFileManagerAliasSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleFileManagerAlias(isChecked);
        });

        // 权限管理按钮
        findViewById(R.id.manage_storage_permission_button).setOnClickListener(v -> requestStoragePermission());
        findViewById(R.id.manage_install_permission_button).setOnClickListener(v -> requestInstallPermission());
        findViewById(R.id.manage_overlay_permission_button).setOnClickListener(v -> requestOverlayPermission());
        findViewById(R.id.manage_battery_optimization_button).setOnClickListener(v -> requestBatteryOptimization());
        
        // 快捷设置按钮
        findViewById(R.id.open_app_settings_button).setOnClickListener(v -> openAppSettings());
        findViewById(R.id.open_default_apps_button).setOnClickListener(v -> openDefaultAppsSettings());
        findViewById(R.id.reset_all_defaults_button).setOnClickListener(v -> resetAllDefaults());
    }

    /**
     * 检查当前系统设置状态
     */
    private void checkCurrentSettings() {
        checkDefaultBrowserStatus();
        checkFileManagerStatus();
        checkPermissionsStatus();
        checkSystemIntegrationStatus();
    }

    private void checkDefaultBrowserStatus() {
        // TODO: 检查是否为默认浏览器
        if (isDefaultBrowser()) {
            mDefaultBrowserStatus.setText("✓ 已设为默认浏览器");
            mDefaultBrowserStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            mSetDefaultBrowserButton.setText("重新设置");
        } else {
            mDefaultBrowserStatus.setText("⚠ 未设为默认浏览器");
            mDefaultBrowserStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            mSetDefaultBrowserButton.setText("设为默认");
        }
    }

    private void checkFileManagerStatus() {
        // TODO: 检查文件管理器状态
        mFileManagerStatus.setText("⚠ 未配置为默认文件管理器");
        mFileManagerStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
    }

    private void checkPermissionsStatus() {
        // 动态生成权限检查项
        mPermissionsLayout.removeAllViews();

        addPermissionStatusItem("存储访问权限", hasStoragePermission());
        addPermissionStatusItem("应用安装权限", hasInstallPermission());
        addPermissionStatusItem("悬浮窗权限", hasOverlayPermission());
        addPermissionStatusItem("电池优化白名单", isBatteryOptimizationIgnored());
    }

    private void addPermissionStatusItem(String permissionName, boolean granted) {
        View itemView = getLayoutInflater().inflate(R.layout.item_permission_status, null);
        TextView nameView = itemView.findViewById(R.id.permission_name);
        TextView statusView = itemView.findViewById(R.id.permission_status);

        nameView.setText(permissionName);
        if (granted) {
            statusView.setText("✓ 已授权");
            statusView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusView.setText("⚠ 未授权");
            statusView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }

        mPermissionsLayout.addView(itemView);
    }

    private void checkSystemIntegrationStatus() {
        PackageManager pm = getPackageManager();
        
        // 检查浏览器别名状态
        ComponentName browserAlias = new ComponentName(this, "com.hippo.ehviewer.BrowserAliasActivity");
        int browserState = pm.getComponentEnabledSetting(browserAlias);
        mBrowserAliasSwitch.setChecked(browserState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        // 检查文件管理器别名状态
        ComponentName fileManagerAlias = new ComponentName(this, "com.hippo.ehviewer.FileOpenerActivity");
        int fileManagerState = pm.getComponentEnabledSetting(fileManagerAlias);
        mFileManagerAliasSwitch.setChecked(fileManagerState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    /**
     * 设置为默认浏览器
     */
    private void setDefaultBrowser() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用Role Manager
                Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                startActivityForResult(intent, REQUEST_SET_DEFAULT_BROWSER);
            } else {
                // Android 9及以下，引导用户手动设置
                showDefaultBrowserGuide();
            }
        } catch (Exception e) {
            showDefaultBrowserGuide();
        }
    }

    private void showDefaultBrowserGuide() {
        new AlertDialog.Builder(this)
            .setTitle("设置默认浏览器")
            .setMessage("请按以下步骤操作:\n\n1. 在弹出的设置页面中找到\"默认应用\"\n2. 选择\"浏览器应用\"\n3. 选择\"EhViewer浏览器\"")
            .setPositiveButton("去设置", (dialog, which) -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "无法打开设置页面", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 设置文件管理器默认
     */
    private void setFileManagerDefault() {
        new AlertDialog.Builder(this)
            .setTitle("设置文件管理器")
            .setMessage("EhViewer文件管理器已配置为支持多种文件类型。\n\n在打开文件时选择\"EhViewer文件管理器\"并设为默认即可。")
            .setPositiveButton("了解", null)
            .show();
    }

    /**
     * 切换浏览器别名状态
     */
    private void toggleBrowserAlias(boolean enabled) {
        PackageManager pm = getPackageManager();
        ComponentName browserAlias = new ComponentName(this, "com.hippo.ehviewer.BrowserAliasActivity");
        
        int newState = enabled ? 
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED : 
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            
        pm.setComponentEnabledSetting(browserAlias, newState, PackageManager.DONT_KILL_APP);
        
        String message = enabled ? "浏览器功能已启用" : "浏览器功能已禁用";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 切换文件管理器别名状态
     */
    private void toggleFileManagerAlias(boolean enabled) {
        PackageManager pm = getPackageManager();
        ComponentName fileManagerAlias = new ComponentName(this, "com.hippo.ehviewer.FileOpenerActivity");
        
        int newState = enabled ? 
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED : 
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            
        pm.setComponentEnabledSetting(fileManagerAlias, newState, PackageManager.DONT_KILL_APP);
        
        String message = enabled ? "文件管理功能已启用" : "文件管理功能已禁用";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // 权限检查方法
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return android.os.Environment.isExternalStorageManager();
        } else {
            return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean hasInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private boolean isBatteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            return powerManager.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private boolean isDefaultBrowser() {
        // TODO: 实现默认浏览器检查逻辑
        return false;
    }

    // 权限请求方法
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES);
        } else {
            requestPermissions(new String[]{
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_MANAGE_ALL_FILES);
        }
    }

    private void requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_INSTALL_PACKAGES);
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_SYSTEM_ALERT_WINDOW);
        }
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATION);
        }
    }

    // 快捷设置方法
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openDefaultAppsSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开默认应用设置", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetAllDefaults() {
        new AlertDialog.Builder(this)
            .setTitle("重置默认应用设置")
            .setMessage("这将清除EhViewer的所有默认应用设置，您需要重新配置。是否继续？")
            .setPositiveButton("重置", (dialog, which) -> {
                try {
                    // 重置组件状态
                    PackageManager pm = getPackageManager();
                    pm.setComponentEnabledSetting(
                        new ComponentName(this, "com.hippo.ehviewer.BrowserAliasActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        PackageManager.DONT_KILL_APP);
                    pm.setComponentEnabledSetting(
                        new ComponentName(this, "com.hippo.ehviewer.FileOpenerActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        PackageManager.DONT_KILL_APP);
                    
                    Toast.makeText(this, "默认设置已重置", Toast.LENGTH_SHORT).show();
                    checkCurrentSettings();
                } catch (Exception e) {
                    Toast.makeText(this, "重置失败", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // 返回后重新检查状态
        checkCurrentSettings();
        
        switch (requestCode) {
            case REQUEST_SET_DEFAULT_BROWSER:
                Toast.makeText(this, "请在默认应用中选择EhViewer浏览器", Toast.LENGTH_LONG).show();
                break;
            case REQUEST_MANAGE_ALL_FILES:
                if (hasStoragePermission()) {
                    Toast.makeText(this, "存储权限已授权", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_INSTALL_PACKAGES:
                if (hasInstallPermission()) {
                    Toast.makeText(this, "安装权限已授权", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_SYSTEM_ALERT_WINDOW:
                if (hasOverlayPermission()) {
                    Toast.makeText(this, "悬浮窗权限已授权", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_IGNORE_BATTERY_OPTIMIZATION:
                if (isBatteryOptimizationIgnored()) {
                    Toast.makeText(this, "已加入电池优化白名单", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkCurrentSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCurrentSettings();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}