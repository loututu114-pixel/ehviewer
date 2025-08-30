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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.R;

/**
 * 增强版媒体播放器Activity
 * 支持视频全屏播放、多种格式和自定义播放器
 */
public class MediaPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MediaPlayerActivity";

    public static final String EXTRA_MEDIA_URI = "media_uri";
    public static final String EXTRA_MEDIA_TYPE = "media_type";
    public static final String EXTRA_TITLE = "title";

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private FrameLayout mVideoContainer;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    private boolean mIsFullscreen = false;
    private int mOriginalOrientation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mWebView = findViewById(R.id.media_web_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mVideoContainer = findViewById(R.id.video_container);

        setupWebView();
        handleIntent(getIntent());
    }

    private void setupWebView() {
        WebSettings webSettings = mWebView.getSettings();

        // 启用必要的功能
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // 硬件加速
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // 设置WebViewClient
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlOverride(url);
            }
        });

        // 设置WebChromeClient用于全屏视频播放
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgressBar.setProgress(newProgress);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                showFullscreenVideo(view, callback);
            }

            @Override
            public void onHideCustomView() {
                hideFullscreenVideo();
            }

            @Override
            public View getVideoLoadingProgressView() {
                return mProgressBar;
            }
        });
    }

    private boolean handleUrlOverride(String url) {
        // 处理特殊URL
        if (url.startsWith("intent://")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to handle intent URL", e);
            }
        }
        return false;
    }

    private void showFullscreenVideo(View view, WebChromeClient.CustomViewCallback callback) {
        if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }

        mCustomView = view;
        mCustomViewCallback = callback;

        // 进入全屏模式
        mOriginalOrientation = getRequestedOrientation();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // 隐藏系统UI
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        mVideoContainer.addView(mCustomView);
        mVideoContainer.setVisibility(View.VISIBLE);
        mWebView.setVisibility(View.GONE);

        mIsFullscreen = true;
    }

    private void hideFullscreenVideo() {
        if (mCustomView == null) return;

        // 退出全屏模式
        setRequestedOrientation(mOriginalOrientation);

        // 显示系统UI
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        mVideoContainer.removeView(mCustomView);
        mVideoContainer.setVisibility(View.GONE);
        mWebView.setVisibility(View.VISIBLE);

        mCustomView = null;
        mCustomViewCallback.onCustomViewHidden();

        mIsFullscreen = false;
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        Uri mediaUri = intent.getParcelableExtra(EXTRA_MEDIA_URI);
        String mediaType = intent.getStringExtra(EXTRA_MEDIA_TYPE);
        String title = intent.getStringExtra(EXTRA_TITLE);

        if (mediaUri == null) {
            Toast.makeText(this, "媒体文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (title != null && !title.isEmpty()) {
            setTitle(title);
        }

        loadMedia(mediaUri, mediaType);
    }

    private void loadMedia(Uri mediaUri, String mediaType) {
        mProgressBar.setVisibility(View.VISIBLE);

        String mediaUrl = mediaUri.toString();

        if (isVideoFile(mediaType)) {
            // 加载视频播放器
            String videoHtml = generateVideoPlayerHtml(mediaUrl);
            mWebView.loadDataWithBaseURL(null, videoHtml, "text/html", "UTF-8", null);
        } else if (isAudioFile(mediaType)) {
            // 加载音频播放器
            String audioHtml = generateAudioPlayerHtml(mediaUrl);
            mWebView.loadDataWithBaseURL(null, audioHtml, "text/html", "UTF-8", null);
        } else {
            // 尝试直接播放
            mWebView.loadUrl(mediaUrl);
        }
    }

    private String generateVideoPlayerHtml(String videoUrl) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
            "<style>" +
            "body { margin: 0; padding: 0; background: #000; overflow: hidden; }" +
            "video { width: 100%; height: 100vh; object-fit: contain; }" +
            ".controls { position: absolute; bottom: 0; left: 0; right: 0; background: rgba(0,0,0,0.7); padding: 10px; opacity: 0; transition: opacity 0.3s; }" +
            "video:hover + .controls, .controls:hover { opacity: 1; }" +
            ".control-btn { background: none; border: none; color: white; font-size: 18px; margin: 0 10px; cursor: pointer; }" +
            ".progress-bar { width: 100%; height: 4px; background: #333; margin: 10px 0; cursor: pointer; }" +
            ".progress-fill { height: 100%; background: #ff6b6b; width: 0%; transition: width 0.1s; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<video id='videoPlayer' controls playsinline>" +
            "<source src='" + videoUrl + "' type='" + getVideoMimeType(videoUrl) + "'>" +
            "您的浏览器不支持视频播放" +
            "</video>" +
            "<div class='controls' id='controls'>" +
            "<button class='control-btn' onclick='togglePlay()'>▶️</button>" +
            "<button class='control-btn' onclick='toggleFullscreen()'>⛶</button>" +
            "<div class='progress-bar' onclick='seek(event)' id='progressBar'>" +
            "<div class='progress-fill' id='progressFill'></div>" +
            "</div>" +
            "</div>" +
            "<script>" +
            "var video = document.getElementById('videoPlayer');" +
            "var controls = document.getElementById('controls');" +
            "var progressBar = document.getElementById('progressBar');" +
            "var progressFill = document.getElementById('progressFill');" +
            "" +
            "function togglePlay() {" +
            "    if (video.paused) {" +
            "        video.play();" +
            "    } else {" +
            "        video.pause();" +
            "    }" +
            "}" +
            "" +
            "function toggleFullscreen() {" +
            "    if (video.requestFullscreen) {" +
            "        video.requestFullscreen();" +
            "    } else if (video.webkitRequestFullscreen) {" +
            "        video.webkitRequestFullscreen();" +
            "    } else if (video.msRequestFullscreen) {" +
            "        video.msRequestFullscreen();" +
            "    }" +
            "}" +
            "" +
            "function seek(event) {" +
            "    var rect = progressBar.getBoundingClientRect();" +
            "    var pos = (event.clientX - rect.left) / rect.width;" +
            "    video.currentTime = pos * video.duration;" +
            "}" +
            "" +
            "video.addEventListener('timeupdate', function() {" +
            "    var progress = (video.currentTime / video.duration) * 100;" +
            "    progressFill.style.width = progress + '%';" +
            "});" +
            "" +
            "video.addEventListener('loadedmetadata', function() {" +
            "    console.log('Video loaded, duration:', video.duration);" +
            "});" +
            "" +
            "video.addEventListener('error', function(e) {" +
            "    console.error('Video error:', e);" +
            "});" +
            "</script>" +
            "</body>" +
            "</html>";
    }

    private String generateAudioPlayerHtml(String audioUrl) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<style>" +
            "body { margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; font-family: 'Arial', sans-serif; }" +
            ".player { background: rgba(255,255,255,0.95); padding: 40px; border-radius: 20px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); text-align: center; max-width: 400px; width: 100%; }" +
            ".title { color: #333; margin-bottom: 30px; font-size: 24px; font-weight: bold; }" +
            "audio { width: 100%; margin: 20px 0; }" +
            ".info { color: #666; margin-top: 20px; font-size: 14px; }" +
            ".waveform { height: 60px; background: linear-gradient(90deg, #ff6b6b, #4ecdc4, #45b7d1); border-radius: 30px; margin: 20px 0; animation: wave 2s ease-in-out infinite; }" +
            "@keyframes wave { 0%, 100% { transform: scaleY(1); } 50% { transform: scaleY(1.3); } }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='player'>" +
            "<div class='title'>🎵 音频播放器</div>" +
            "<div class='waveform'></div>" +
            "<audio id='audioPlayer' controls autoplay>" +
            "<source src='" + audioUrl + "' type='" + getAudioMimeType(audioUrl) + "'>" +
            "您的浏览器不支持音频播放" +
            "</audio>" +
            "<div class='info'>文件: " + getFileNameFromUrl(audioUrl) + "</div>" +
            "</div>" +
            "<script>" +
            "var audio = document.getElementById('audioPlayer');" +
            "audio.addEventListener('loadedmetadata', function() {" +
            "    console.log('Audio loaded, duration:', audio.duration);" +
            "});" +
            "audio.addEventListener('error', function(e) {" +
            "    console.error('Audio error:', e);" +
            "});" +
            "</script>" +
            "</body>" +
            "</html>";
    }

    private String getVideoMimeType(String url) {
        String extension = getFileExtension(url).toLowerCase();
        switch (extension) {
            case "mp4": return "video/mp4";
            case "webm": return "video/webm";
            case "ogg": return "video/ogg";
            case "avi": return "video/avi";
            case "mov": return "video/quicktime";
            case "wmv": return "video/x-ms-wmv";
            case "flv": return "video/x-flv";
            case "mkv": return "video/x-matroska";
            default: return "video/mp4";
        }
    }

    private String getAudioMimeType(String url) {
        String extension = getFileExtension(url).toLowerCase();
        switch (extension) {
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "aac": return "audio/aac";
            case "m4a": return "audio/mp4";
            case "flac": return "audio/flac";
            default: return "audio/mpeg";
        }
    }

    private String getFileExtension(String url) {
        int lastDot = url.lastIndexOf('.');
        if (lastDot > 0 && lastDot < url.length() - 1) {
            return url.substring(lastDot + 1);
        }
        return "";
    }

    private String getFileNameFromUrl(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        return "未知文件";
    }

    private boolean isVideoFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    private boolean isAudioFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 处理屏幕方向变化
    }

    @Override
    public void onBackPressed() {
        if (mIsFullscreen) {
            hideFullscreenVideo();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停音频焦点
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.destroy();
        }
    }

    /**
     * 启动媒体播放器
     */
    public static void startMediaPlayer(android.content.Context context, Uri mediaUri, String mimeType, String title) {
        try {
            Intent intent = new Intent(context, MediaPlayerActivity.class);
            intent.putExtra(EXTRA_MEDIA_URI, mediaUri);
            intent.putExtra(EXTRA_MEDIA_TYPE, mimeType);
            intent.putExtra(EXTRA_TITLE, title);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to start MediaPlayerActivity", e);
            Toast.makeText(context, "无法播放媒体文件", Toast.LENGTH_SHORT).show();
        }
    }
}
