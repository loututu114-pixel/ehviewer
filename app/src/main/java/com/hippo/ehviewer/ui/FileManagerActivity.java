package com.hippo.ehviewer.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.adapter.FileListAdapter;
import com.hippo.ehviewer.ui.model.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 文件管理器界面
 * 支持文件浏览、预览、管理等功能
 */
public class FileManagerActivity extends AppCompatActivity {

    private static final String TAG = "FileManagerActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    // UI组件
    private ListView mFileListView;
    private TextView mCurrentPathText;
    private TextView mStorageInfoText;
    private FileListAdapter mAdapter;

    // 数据
    private List<FileItem> mFileList;
    private File mCurrentDirectory;
    private File mAppDirectory; // 应用专用目录

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);

        initViews();
        setupToolbar();
        initAppDirectory();
        
        if (checkStoragePermission()) {
            initializeFileList();
        } else {
            requestStoragePermission();
        }
    }

    private void initViews() {
        mFileListView = findViewById(R.id.file_list_view);
        mCurrentPathText = findViewById(R.id.current_path_text);
        mStorageInfoText = findViewById(R.id.storage_info_text);

        mFileList = new ArrayList<>();
        mAdapter = new FileListAdapter(this, mFileList);
        mFileListView.setAdapter(mAdapter);

        mFileListView.setOnItemClickListener(this::onFileItemClick);
        mFileListView.setOnItemLongClickListener(this::onFileItemLongClick);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("文件管理器");
        }
    }

    private void initAppDirectory() {
        // 创建应用专用存储目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用分区存储
            mAppDirectory = new File(getExternalFilesDir(null), "EhViewer");
        } else {
            // Android 9及以下使用传统存储
            mAppDirectory = new File(Environment.getExternalStorageDirectory(), "EhViewer");
        }
        
        if (!mAppDirectory.exists()) {
            mAppDirectory.mkdirs();
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 检查所有文件访问权限
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 检查读写存储权限
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 请求所有文件访问权限
            new AlertDialog.Builder(this)
                .setTitle("需要存储权限")
                .setMessage("文件管理器需要访问设备存储空间。请在设置中授予\"所有文件访问权限\"。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("取消", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
        } else {
            // Android 6-10 请求读写权限
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
        }
    }

    private void initializeFileList() {
        // 默认显示应用目录
        mCurrentDirectory = mAppDirectory;
        loadFileList();
        updateStorageInfo();
    }

    private void loadFileList() {
        mFileList.clear();
        
        if (mCurrentDirectory == null || !mCurrentDirectory.exists()) {
            mCurrentDirectory = mAppDirectory;
        }

        // 更新路径显示
        mCurrentPathText.setText(mCurrentDirectory.getAbsolutePath());

        // 添加返回上级目录项
        if (!isRootDirectory(mCurrentDirectory)) {
            FileItem parentItem = new FileItem();
            parentItem.name = "..";
            parentItem.path = mCurrentDirectory.getParent();
            parentItem.isDirectory = true;
            parentItem.isParent = true;
            mFileList.add(parentItem);
        }

        // 读取当前目录内容
        File[] files = mCurrentDirectory.listFiles();
        if (files != null) {
            List<FileItem> fileItems = new ArrayList<>();
            
            for (File file : files) {
                if (file.isHidden()) continue; // 跳过隐藏文件
                
                FileItem item = new FileItem();
                item.name = file.getName();
                item.path = file.getAbsolutePath();
                item.isDirectory = file.isDirectory();
                item.size = file.length();
                item.lastModified = file.lastModified();
                item.canRead = file.canRead();
                item.canWrite = file.canWrite();
                
                fileItems.add(item);
            }
            
            // 排序：目录在前，按名称排序
            Collections.sort(fileItems, new Comparator<FileItem>() {
                @Override
                public int compare(FileItem f1, FileItem f2) {
                    if (f1.isDirectory != f2.isDirectory) {
                        return f1.isDirectory ? -1 : 1;
                    }
                    return f1.name.compareToIgnoreCase(f2.name);
                }
            });
            
            mFileList.addAll(fileItems);
        }

        mAdapter.notifyDataSetChanged();
    }

    private boolean isRootDirectory(File dir) {
        if (dir == null) return true;
        
        // 检查是否为根目录或应用目录
        return dir.equals(Environment.getExternalStorageDirectory()) ||
               dir.equals(Environment.getRootDirectory()) ||
               dir.getParent() == null;
    }

    private void updateStorageInfo() {
        try {
            long totalSpace = Environment.getExternalStorageDirectory().getTotalSpace();
            long freeSpace = Environment.getExternalStorageDirectory().getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            String totalStr = Formatter.formatFileSize(this, totalSpace);
            String freeStr = Formatter.formatFileSize(this, freeSpace);
            String usedStr = Formatter.formatFileSize(this, usedSpace);

            String storageInfo = String.format("存储空间: %s 已用 / %s 总计 (%s 可用)", 
                usedStr, totalStr, freeStr);
            mStorageInfoText.setText(storageInfo);
        } catch (Exception e) {
            mStorageInfoText.setText("存储信息不可用");
        }
    }

    private void onFileItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileItem item = mFileList.get(position);
        
        if (item.isDirectory) {
            // 进入目录
            File newDir = new File(item.path);
            if (newDir.exists() && newDir.canRead()) {
                mCurrentDirectory = newDir;
                loadFileList();
            } else {
                Toast.makeText(this, "无法访问目录", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 打开文件
            openFile(item);
        }
    }

    private boolean onFileItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        FileItem item = mFileList.get(position);
        if (item.isParent) return false;
        
        showFileOptionsDialog(item);
        return true;
    }

    private void openFile(FileItem item) {
        try {
            File file = new File(item.path);
            if (!file.exists()) {
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }

            // 根据文件类型选择打开方式
            String fileName = item.name.toLowerCase();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            if (fileName.endsWith(".apk") || fileName.contains(".apk.")) {
                // APK文件用APK安装器打开
                intent = new Intent(this, ApkInstallerActivity.class);
                intent.setData(Uri.fromFile(file));
            } else {
                // 其他文件用WebViewActivity打开
                intent = new Intent(this, WebViewActivity.class);
                intent.setData(Uri.fromFile(file));
            }
            
            startActivity(intent);
            
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFileOptionsDialog(FileItem item) {
        String[] options = {"打开", "详情", "删除", "重命名", "复制路径"};
        
        new AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 打开
                        if (item.isDirectory) {
                            mCurrentDirectory = new File(item.path);
                            loadFileList();
                        } else {
                            openFile(item);
                        }
                        break;
                    case 1: // 详情
                        showFileInfo(item);
                        break;
                    case 2: // 删除
                        confirmDeleteFile(item);
                        break;
                    case 3: // 重命名
                        showRenameDialog(item);
                        break;
                    case 4: // 复制路径
                        copyPathToClipboard(item);
                        break;
                }
            })
            .show();
    }

    private void showFileInfo(FileItem item) {
        StringBuilder info = new StringBuilder();
        info.append("名称: ").append(item.name).append("\n");
        info.append("路径: ").append(item.path).append("\n");
        info.append("类型: ").append(item.isDirectory ? "目录" : "文件").append("\n");
        
        if (!item.isDirectory) {
            info.append("大小: ").append(Formatter.formatFileSize(this, item.size)).append("\n");
        }
        
        info.append("修改时间: ").append(new java.util.Date(item.lastModified).toString()).append("\n");
        info.append("可读: ").append(item.canRead ? "是" : "否").append("\n");
        info.append("可写: ").append(item.canWrite ? "是" : "否");

        new AlertDialog.Builder(this)
            .setTitle("文件信息")
            .setMessage(info.toString())
            .setPositiveButton("确定", null)
            .show();
    }

    private void confirmDeleteFile(FileItem item) {
        new AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除 \"" + item.name + "\" 吗？")
            .setPositiveButton("删除", (dialog, which) -> deleteFile(item))
            .setNegativeButton("取消", null)
            .show();
    }

    private void deleteFile(FileItem item) {
        try {
            File file = new File(item.path);
            if (file.delete()) {
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                loadFileList(); // 刷新列表
            } else {
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameDialog(FileItem item) {
        // TODO: 实现重命名功能
        Toast.makeText(this, "重命名功能开发中", Toast.LENGTH_SHORT).show();
    }

    private void copyPathToClipboard(FileItem item) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("文件路径", item.path);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "路径已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (mCurrentDirectory != null && !isRootDirectory(mCurrentDirectory)) {
            // 返回上级目录
            File parent = mCurrentDirectory.getParentFile();
            if (parent != null && parent.canRead()) {
                mCurrentDirectory = parent;
                loadFileList();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manager_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_home) {
            mCurrentDirectory = mAppDirectory;
            loadFileList();
            return true;
        } else if (itemId == R.id.action_root) {
            if (checkStoragePermission()) {
                mCurrentDirectory = Environment.getExternalStorageDirectory();
                loadFileList();
            } else {
                Toast.makeText(this, "需要存储权限", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (itemId == R.id.action_refresh) {
            loadFileList();
            updateStorageInfo();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkStoragePermission()) {
                initializeFileList();
            } else {
                Toast.makeText(this, "需要存储权限才能使用文件管理器", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                initializeFileList();
            } else {
                Toast.makeText(this, "需要存储权限才能使用文件管理器", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}