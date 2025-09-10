package com.hippo.ehviewer.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * 深度链接处理Activity
 * 处理应用的深度链接和URL Scheme跳转
 */
public class DeepLinkHandlerActivity extends Activity {
    
    private static final String TAG = "DeepLinkHandler";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            handleDeepLink(intent.getData());
        } else {
            // 没有数据，直接打开主界面
            openMainActivity(null);
        }
        
        finish();
    }
    
    /**
     * 处理深度链接
     */
    private void handleDeepLink(Uri uri) {
        Log.d(TAG, "Handling deep link: " + uri.toString());
        
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();
        
        // 处理自定义scheme: ehviewer://
        if ("ehviewer".equals(scheme)) {
            handleCustomScheme(uri);
        }
        // 处理应用链接: https://ehviewer.app/open/...
        else if ("https".equals(scheme) && "ehviewer.app".equals(host)) {
            handleAppLink(uri);
        }
        // 其他情况
        else {
            openMainActivity(uri);
        }
    }
    
    /**
     * 处理自定义URL Scheme
     * ehviewer://action/params
     */
    private void handleCustomScheme(Uri uri) {
        String host = uri.getHost();
        
        if (host == null) {
            openMainActivity(uri);
            return;
        }
        
        switch (host) {
            case "gallery":
                // ehviewer://gallery/123456
                openGallery(uri);
                break;
                
            case "search":
                // ehviewer://search?q=keyword
                openSearch(uri);
                break;
                
            case "download":
                // ehviewer://download/123456
                openDownload(uri);
                break;
                
            case "settings":
                // ehviewer://settings
                openSettings();
                break;
                
            case "browser":
                // ehviewer://browser?url=https://example.com
                openBrowser(uri);
                break;
                
            case "file":
                // ehviewer://file?path=/storage/file.pdf
                openFile(uri);
                break;
                
            default:
                openMainActivity(uri);
                break;
        }
    }
    
    /**
     * 处理应用链接
     * https://ehviewer.app/open/...
     */
    private void handleAppLink(Uri uri) {
        String path = uri.getPath();
        
        if (path == null) {
            openMainActivity(uri);
            return;
        }
        
        if (path.startsWith("/open/gallery/")) {
            // https://ehviewer.app/open/gallery/123456
            String galleryId = path.substring("/open/gallery/".length());
            openGalleryById(galleryId);
        } else if (path.startsWith("/open/news/")) {
            // https://ehviewer.app/open/news/123
            String newsId = path.substring("/open/news/".length());
            openNews(newsId);
        } else if (path.startsWith("/open/file/")) {
            // https://ehviewer.app/open/file/...
            String fileType = path.substring("/open/file/".length());
            openFileByType(fileType, uri);
        } else {
            openMainActivity(uri);
        }
    }
    
    /**
     * 打开画廊
     */
    private void openGallery(Uri uri) {
        String galleryId = uri.getLastPathSegment();
        if (galleryId != null) {
            Intent intent = new Intent(this, GalleryActivity.class);
            intent.putExtra("gallery_id", galleryId);
            startActivity(intent);
        } else {
            openMainActivity(uri);
        }
    }
    
    /**
     * 根据ID打开画廊
     */
    private void openGalleryById(String galleryId) {
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra("gallery_id", galleryId);
        startActivity(intent);
    }
    
    /**
     * 打开搜索
     */
    private void openSearch(Uri uri) {
        String query = uri.getQueryParameter("q");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("com.hippo.ehviewer.ACTION_SEARCH");
        if (query != null) {
            intent.putExtra("query", query);
        }
        startActivity(intent);
    }
    
    /**
     * 打开下载
     */
    private void openDownload(Uri uri) {
        String downloadId = uri.getLastPathSegment();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("com.hippo.ehviewer.ACTION_VIEW_DOWNLOAD");
        if (downloadId != null) {
            intent.putExtra("download_id", downloadId);
        }
        startActivity(intent);
    }
    
    /**
     * 打开设置
     */
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    /**
     * 打开浏览器
     */
    private void openBrowser(Uri uri) {
        String url = uri.getQueryParameter("url");
        if (url != null) {
            Intent intent = new Intent(this, YCWebViewActivity.class);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } else {
            openMainActivity(uri);
        }
    }
    
    /**
     * 打开文件
     */
    private void openFile(Uri uri) {
        String filePath = uri.getQueryParameter("path");
        if (filePath != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + filePath), getMimeType(filePath));
            intent.setPackage(getPackageName());
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
                openMainActivity(uri);
            }
        } else {
            openMainActivity(uri);
        }
    }
    
    /**
     * 根据类型打开文件
     */
    private void openFileByType(String fileType, Uri uri) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("com.hippo.ehviewer.ACTION_OPEN_FILE");
        intent.putExtra("file_type", fileType);
        intent.setData(uri);
        startActivity(intent);
    }
    
    /**
     * 打开新闻
     */
    private void openNews(String newsId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("com.hippo.ehviewer.ACTION_VIEW_NEWS");
        intent.putExtra("news_id", newsId);
        startActivity(intent);
    }
    
    /**
     * 打开主界面
     */
    private void openMainActivity(Uri uri) {
        Intent intent = new Intent(this, MainActivity.class);
        if (uri != null) {
            intent.setData(uri);
        }
        startActivity(intent);
    }
    
    /**
     * 获取文件MIME类型
     */
    private String getMimeType(String filePath) {
        if (filePath == null) return "*/*";
        
        String extension = "";
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filePath.substring(lastDot + 1).toLowerCase();
        }
        
        switch (extension) {
            case "pdf": return "application/pdf";
            case "epub": return "application/epub+zip";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp4": return "video/mp4";
            case "txt": return "text/plain";
            case "html": return "text/html";
            default: return "*/*";
        }
    }
}