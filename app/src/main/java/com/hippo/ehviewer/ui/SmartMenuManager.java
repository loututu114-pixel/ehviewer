package com.hippo.ehviewer.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.DefaultBrowserHelper;

/**
 * 智能菜单管理器
 * 提供实用的系统功能调用和EhViewer浏览器整合
 */
public class SmartMenuManager {
    
    private static final String TAG = "SmartMenuManager";
    
    private final AppCompatActivity mActivity;
    private final Context mContext;
    
    public SmartMenuManager(@NonNull AppCompatActivity activity) {
        mActivity = activity;
        mContext = activity.getApplicationContext();
    }
    
    /**
     * 创建智能菜单
     */
    public void createSmartMenu(@NonNull Menu menu) {
        try {
            // 浏览器功能组
            menu.add(0, R.id.menu_browser_home, 1, "🏠 浏览器首页")
                .setIcon(R.drawable.ic_home)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                
            menu.add(0, R.id.menu_new_tab, 2, "➕ 新建标签页")
                .setIcon(R.drawable.ic_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            
            menu.add(0, R.id.menu_bookmarks, 3, "⭐ 书签管理")
                .setIcon(R.drawable.ic_bookmark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            
            menu.add(0, R.id.menu_history, 4, "📜 浏览历史")
                .setIcon(R.drawable.ic_history)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            
            menu.add(0, R.id.menu_downloads, 5, "📁 下载管理")
                .setIcon(R.drawable.ic_download)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            
            // 系统功能组
            menu.add(1, R.id.menu_system_search, 10, "🔍 全局搜索")
                .setIcon(R.drawable.ic_search)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            
            menu.add(1, R.id.menu_file_manager, 11, "📂 文件管理器")
                .setIcon(R.drawable.ic_folder)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            
            menu.add(1, R.id.menu_app_manager, 12, "📱 应用管理")
                .setIcon(R.drawable.ic_apps)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            
            menu.add(1, R.id.menu_system_settings, 13, "⚙️ 系统设置")
                .setIcon(R.drawable.ic_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            
            // EhViewer特色功能组  
            menu.add(2, R.id.menu_set_default, 20, "🚀 设为默认浏览器")
                .setIcon(R.drawable.ic_star)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
            menu.add(2, R.id.menu_private_mode, 21, "🔐 私密模式")
                .setIcon(R.drawable.ic_lock)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
            menu.add(2, R.id.menu_quick_actions, 22, "⚡ 快捷操作")
                .setIcon(R.drawable.ic_flash_on)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
        } catch (Exception e) {
            Log.e(TAG, "Error creating smart menu", e);
        }
    }
    
    /**
     * 处理菜单项点击
     */
    public boolean handleMenuItemClick(@NonNull MenuItem item) {
        try {
            int itemId = item.getItemId();
            
            // 浏览器功能组
            if (itemId == R.id.menu_browser_home) {
                openBrowserHome();
                return true;
            } else if (itemId == R.id.menu_new_tab) {
                openNewTab();
                return true;
            } else if (itemId == R.id.menu_bookmarks) {
                openBookmarks();
                return true;
            } else if (itemId == R.id.menu_history) {
                openHistory();
                return true;
            } else if (itemId == R.id.menu_downloads) {
                openDownloads();
                return true;
                
            // 系统功能组
            } else if (itemId == R.id.menu_system_search) {
                openSystemSearch();
                return true;
            } else if (itemId == R.id.menu_file_manager) {
                openFileManager();
                return true;
            } else if (itemId == R.id.menu_app_manager) {
                openAppManager();
                return true;
            } else if (itemId == R.id.menu_system_settings) {
                openSystemSettings();
                return true;
                
            // EhViewer特色功能组
            } else if (itemId == R.id.menu_set_default) {
                setAsDefaultBrowser();
                return true;
            } else if (itemId == R.id.menu_private_mode) {
                openPrivateMode();
                return true;
            } else if (itemId == R.id.menu_quick_actions) {
                showQuickActionsDialog();
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling menu item click", e);
            Toast.makeText(mContext, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    // ===== 浏览器功能实现 =====
    
    /**
     * 打开浏览器首页
     */
    private void openBrowserHome() {
        try {
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.setData(Uri.parse("https://main.eh-viewer.com/"));
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening browser home", e);
            Toast.makeText(mContext, "打开首页失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 新建标签页
     */
    private void openNewTab() {
        try {
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.putExtra("new_tab", true);
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening new tab", e);
            Toast.makeText(mContext, "新建标签页失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开书签管理
     */
    private void openBookmarks() {
        try {
            // 通过WebViewActivity的书签功能
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.putExtra("show_bookmarks", true);
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening bookmarks", e);
            Toast.makeText(mContext, "打开书签失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开浏览历史
     */
    private void openHistory() {
        try {
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.putExtra("show_history", true);
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening history", e);
            Toast.makeText(mContext, "打开历史失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开下载管理
     */
    private void openDownloads() {
        try {
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.putExtra("show_downloads", true);
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening downloads", e);
            Toast.makeText(mContext, "打开下载管理失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    // ===== 系统功能实现 =====
    
    /**
     * 调用系统全局搜索
     */
    private void openSystemSearch() {
        try {
            // 方法1: Android搜索助手
            Intent searchIntent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
            if (searchIntent.resolveActivity(mContext.getPackageManager()) != null) {
                mActivity.startActivity(searchIntent);
                return;
            }
            
            // 方法2: Google搜索应用
            Intent googleSearchIntent = new Intent();
            googleSearchIntent.setPackage("com.google.android.googlequicksearchbox");
            googleSearchIntent.setAction("android.search.action.GLOBAL_SEARCH");
            if (googleSearchIntent.resolveActivity(mContext.getPackageManager()) != null) {
                mActivity.startActivity(googleSearchIntent);
                return;
            }
            
            // 方法3: 备用 - 打开Google搜索网页
            Intent webSearchIntent = new Intent(mContext, WebViewActivity.class);
            webSearchIntent.setData(Uri.parse("https://www.google.com"));
            webSearchIntent.putExtra("from_smart_menu", true);
            mActivity.startActivity(webSearchIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening system search", e);
            Toast.makeText(mContext, "打开搜索失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开文件管理器
     */
    private void openFileManager() {
        try {
            // 优先使用EhViewer内置文件管理器
            Intent intent = new Intent(mContext, FileManagerActivity.class);
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file manager", e);
            // 备用方案：系统文件管理器
            try {
                Intent systemFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                systemFileIntent.setType("*/*");
                mActivity.startActivity(Intent.createChooser(systemFileIntent, "选择文件管理器"));
            } catch (Exception e2) {
                Toast.makeText(mContext, "打开文件管理器失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 打开应用管理
     */
    private void openAppManager() {
        try {
            // 系统应用管理
            Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening app manager", e);
            // 备用方案
            try {
                Intent backupIntent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                mActivity.startActivity(backupIntent);
            } catch (Exception e2) {
                Toast.makeText(mContext, "打开应用管理失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 打开系统设置
     */
    private void openSystemSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening system settings", e);
            Toast.makeText(mContext, "打开系统设置失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    // ===== EhViewer特色功能实现 =====
    
    /**
     * 设为默认浏览器
     */
    private void setAsDefaultBrowser() {
        try {
            if (DefaultBrowserHelper.isDefaultBrowser(mContext)) {
                Toast.makeText(mContext, "✅ EhViewer已是默认浏览器", Toast.LENGTH_SHORT).show();
                return;
            }
            
            boolean success = DefaultBrowserHelper.trySetAsDefaultBrowser(mContext);
            if (!success) {
                showDefaultBrowserHelpDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting as default browser", e);
            Toast.makeText(mContext, "设置默认浏览器失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示默认浏览器设置帮助对话框
     */
    private void showDefaultBrowserHelpDialog() {
        new AlertDialog.Builder(mActivity)
            .setTitle("🚀 设置默认浏览器")
            .setMessage("请在系统设置中将EhViewer设为默认浏览器:\n\n" +
                      "1. 在弹出的设置页面中找到【浏览器】选项\n" +
                      "2. 选择【EhViewer浏览器】\n" +
                      "3. 返回即可完成设置\n\n" +
                      "设置后所有链接都将用EhViewer打开！")
            .setPositiveButton("🎯 立即设置", (dialog, which) -> {
                DefaultBrowserHelper.trySetAsDefaultBrowser(mContext);
            })
            .setNegativeButton("取消", null)
            .setNeutralButton("❓ 详细教程", (dialog, which) -> {
                showDetailedBrowserGuide();
            })
            .show();
    }
    
    /**
     * 显示详细浏览器设置教程
     */
    private void showDetailedBrowserGuide() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String guide = getDeviceSpecificGuide(manufacturer);
        
        new AlertDialog.Builder(mActivity)
            .setTitle("📱 " + Build.MANUFACTURER + " 设置教程")
            .setMessage(guide)
            .setPositiveButton("🚀 跳转设置", (dialog, which) -> {
                DefaultBrowserHelper.trySetAsDefaultBrowser(mContext);
            })
            .setNegativeButton("我知道了", null)
            .show();
    }
    
    /**
     * 获取设备特定设置教程
     */
    private String getDeviceSpecificGuide(String manufacturer) {
        switch (manufacturer) {
            case "xiaomi":
            case "redmi":
                return "小米/红米设备设置步骤:\n\n" +
                       "设置 → 应用设置 → 应用管理 → 默认应用设置 → 浏览器 → EhViewer";
            case "huawei":
            case "honor":
                return "华为/荣耀设备设置步骤:\n\n" +
                       "设置 → 应用和服务 → 默认应用 → 浏览器 → EhViewer";
            case "oppo":
            case "oneplus":
                return "OPPO/一加设备设置步骤:\n\n" +
                       "设置 → 应用管理 → 默认应用 → 浏览器应用 → EhViewer";
            case "vivo":
                return "vivo设备设置步骤:\n\n" +
                       "设置 → 更多设置 → 应用管理 → 默认应用 → 浏览器 → EhViewer";
            case "samsung":
                return "三星设备设置步骤:\n\n" +
                       "设置 → 应用程序 → 选择默认应用 → 浏览器 → EhViewer";
            default:
                return "通用设置步骤:\n\n" +
                       "方法1: 设置 → 应用和通知 → 默认应用 → 浏览器应用 → EhViewer\n\n" +
                       "方法2: 长按EhViewer图标 → 应用信息 → 设为默认 → 浏览器";
        }
    }
    
    /**
     * 打开私密模式
     */
    private void openPrivateMode() {
        try {
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.putExtra("private_mode", true);
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening private mode", e);
            Toast.makeText(mContext, "打开私密模式失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示快捷操作对话框
     */
    private void showQuickActionsDialog() {
        String[] actions = {
            "🏠 打开首页",
            "🔍 Google搜索", 
            "📱 APK安装器",
            "📂 文件管理器",
            "⚙️ 浏览器设置",
            "🚀 设为默认浏览器",
            "🔐 私密模式",
            "📋 剪贴板链接"
        };
        
        new AlertDialog.Builder(mActivity)
            .setTitle("⚡ 快捷操作")
            .setItems(actions, (dialog, which) -> {
                switch (which) {
                    case 0: openBrowserHome(); break;
                    case 1: openGoogleSearch(); break;
                    case 2: openApkInstaller(); break;
                    case 3: openFileManager(); break;
                    case 4: openBrowserSettings(); break;
                    case 5: setAsDefaultBrowser(); break;
                    case 6: openPrivateMode(); break;
                    case 7: openClipboardLink(); break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 打开Google搜索
     */
    private void openGoogleSearch() {
        try {
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.setData(Uri.parse("https://www.google.com"));
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening Google search", e);
            Toast.makeText(mContext, "打开Google搜索失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开APK安装器
     */
    private void openApkInstaller() {
        try {
            Intent intent = new Intent(mContext, ApkInstallerActivity.class);
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening APK installer", e);
            Toast.makeText(mContext, "打开APK安装器失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开浏览器设置
     */
    private void openBrowserSettings() {
        try {
            Intent intent = new Intent(mContext, BrowserSettingsActivity.class);
            intent.putExtra("from_smart_menu", true);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening browser settings", e);
            Toast.makeText(mContext, "打开浏览器设置失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开剪贴板链接
     */
    private void openClipboardLink() {
        try {
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                String text = item.getText().toString().trim();
                
                if (isValidUrl(text)) {
                    Intent intent = new Intent(mContext, WebViewActivity.class);
                    intent.setData(Uri.parse(text));
                    intent.putExtra("from_clipboard", true);
                    intent.putExtra("from_smart_menu", true);
                    mActivity.startActivity(intent);
                } else {
                    Toast.makeText(mContext, "剪贴板中没有有效的链接", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mContext, "剪贴板为空", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening clipboard link", e);
            Toast.makeText(mContext, "读取剪贴板失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 验证URL有效性
     */
    private boolean isValidUrl(String url) {
        try {
            return url.startsWith("http://") || url.startsWith("https://") || 
                   url.startsWith("ftp://") || url.contains(".");
        } catch (Exception e) {
            return false;
        }
    }
}