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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EnhancedWebViewManager;

/**
 * 文件查看器Activity
 * 支持PDF、Word、Excel、PPT等文件格式的查看
 */
public class FileViewerActivity extends AppCompatActivity {

    private static final String TAG = "FileViewerActivity";

    public static final String EXTRA_FILE_URI = "file_uri";
    public static final String EXTRA_FILE_TYPE = "file_type";

    private WebView mWebView;
    private EnhancedWebViewManager mWebViewManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer);

        mWebView = findViewById(R.id.file_web_view);

        // 初始化增强WebView管理器
        mWebViewManager = new EnhancedWebViewManager(this, mWebView);

        // 处理Intent
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * 处理Intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null) return;

        Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
        String fileType = intent.getStringExtra(EXTRA_FILE_TYPE);

        if (fileUri == null) {
            Toast.makeText(this, "文件URI为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFile(fileUri, fileType);
    }

    /**
     * 加载文件
     */
    private void loadFile(Uri fileUri, String fileType) {
        try {
            String fileUrl = fileUri.toString();

            // 根据文件类型处理
            if (isDocumentFile(fileType)) {
                // 文档文件使用Google Docs Viewer
                String viewerUrl = "https://docs.google.com/viewer?url=" + java.net.URLEncoder.encode(fileUrl, "UTF-8");
                mWebView.loadUrl(viewerUrl);
            } else if (isVideoFile(fileType)) {
                // 视频文件直接播放
                String html = generateVideoPlayerHtml(fileUrl);
                mWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            } else if (isAudioFile(fileType)) {
                // 音频文件
                String html = generateAudioPlayerHtml(fileUrl);
                mWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            } else {
                // 其他文件尝试直接打开
                mWebView.loadUrl(fileUrl);
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to load file", e);
            Toast.makeText(this, "无法加载文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 生成视频播放器HTML
     */
    private String generateVideoPlayerHtml(String videoUrl) {
        return "<html><head><style>" +
                "body { margin: 0; padding: 0; background: #000; display: flex; align-items: center; justify-content: center; min-height: 100vh; }" +
                "video { max-width: 100%; max-height: 100%; }" +
                ".controls { position: absolute; bottom: 20px; left: 20px; right: 20px; color: white; background: rgba(0,0,0,0.5); padding: 10px; border-radius: 5px; }" +
                "</style></head><body>" +
                "<video id=\"videoPlayer\" controls autoplay>" +
                "<source src=\"" + videoUrl + "\" type=\"video/mp4\">" +
                "您的浏览器不支持视频播放" +
                "</video>" +
                "<script>" +
                "var video = document.getElementById('videoPlayer');" +
                "video.addEventListener('loadedmetadata', function() {" +
                "    video.style.display = 'block';" +
                "});" +
                "</script>" +
                "</body></html>";
    }

    /**
     * 生成音频播放器HTML
     */
    private String generateAudioPlayerHtml(String audioUrl) {
        return "<html><head><style>" +
                "body { margin: 0; padding: 20px; background: #f5f5f5; font-family: Arial, sans-serif; }" +
                ".player { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 400px; margin: 50px auto; }" +
                "h2 { color: #333; text-align: center; margin-bottom: 30px; }" +
                "audio { width: 100%; margin-bottom: 20px; }" +
                ".info { color: #666; text-align: center; }" +
                "</style></head><body>" +
                "<div class=\"player\">" +
                "<h2>🎵 音频播放器</h2>" +
                "<audio controls>" +
                "<source src=\"" + audioUrl + "\" type=\"audio/mpeg\">" +
                "您的浏览器不支持音频播放" +
                "</audio>" +
                "<div class=\"info\">文件: " + audioUrl.substring(audioUrl.lastIndexOf('/') + 1) + "</div>" +
                "</div>" +
                "</body></html>";
    }

    /**
     * 判断是否为文档文件
     */
    private boolean isDocumentFile(String mimeType) {
        if (mimeType == null) return false;

        return mimeType.equals("application/pdf") ||
               mimeType.equals("application/msword") ||
               mimeType.equals("application/vnd.ms-excel") ||
               mimeType.equals("application/vnd.ms-powerpoint") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
               mimeType.equals("text/plain");
    }

    /**
     * 判断是否为视频文件
     */
    private boolean isVideoFile(String mimeType) {
        if (mimeType == null) return false;

        return mimeType.startsWith("video/");
    }

    /**
     * 判断是否为音频文件
     */
    private boolean isAudioFile(String mimeType) {
        if (mimeType == null) return false;

        return mimeType.startsWith("audio/");
    }

    /**
     * 启动文件查看器
     */
    public static void startFileViewer(android.content.Context context, Uri fileUri, String mimeType) {
        try {
            Intent intent = new Intent(context, FileViewerActivity.class);
            intent.putExtra(EXTRA_FILE_URI, fileUri);
            intent.putExtra(EXTRA_FILE_TYPE, mimeType);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to start FileViewerActivity", e);
            Toast.makeText(context, "无法打开文件查看器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebViewManager != null) {
            mWebViewManager.destroy();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mWebViewManager != null) {
            mWebViewManager.onActivityResult(requestCode, resultCode, data);
        }
    }
}
