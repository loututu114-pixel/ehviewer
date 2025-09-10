package com.hippo.ehviewer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.VideoPlayerEnhancer;

/**
 * 视频控制界面
 * 提供全屏、音量、方向控制等功能
 */
public class VideoControlView extends LinearLayout {
    private VideoPlayerEnhancer videoEnhancer;

    // UI控件
    private ImageButton fullscreenButton;
    private ImageButton orientationButton;
    private ImageButton volumeUpButton;
    private ImageButton volumeDownButton;
    private ImageButton muteButton;
    private SeekBar volumeSeekBar;
    private TextView volumeText;

    // 状态变量
    private boolean isMuted = false;
    private int currentVolume = 0;
    private int maxVolume = 15;

    public VideoControlView(Context context) {
        super(context);
        init(context);
    }

    public VideoControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_video_controls, this, true);

        // 初始化控件
        fullscreenButton = findViewById(R.id.fullscreen_button);
        orientationButton = findViewById(R.id.orientation_button);
        volumeUpButton = findViewById(R.id.volume_up_button);
        volumeDownButton = findViewById(R.id.volume_down_button);
        muteButton = findViewById(R.id.mute_button);
        volumeSeekBar = findViewById(R.id.volume_seekbar);
        volumeText = findViewById(R.id.volume_text);

        // 设置事件监听器
        setupListeners();

        // 初始化状态
        updateVolumeDisplay();
    }

    /**
     * 设置视频增强器
     */
    public void setVideoEnhancer(VideoPlayerEnhancer enhancer) {
        this.videoEnhancer = enhancer;
        updateControlsState();
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 全屏按钮
        if (fullscreenButton != null) {
            fullscreenButton.setOnClickListener(v -> {
                if (videoEnhancer != null) {
                    if (videoEnhancer.isFullscreen()) {
                        // 这里需要通过WebView退出全屏
                        exitFullscreen();
                    } else {
                        videoEnhancer.requestFullscreen();
                    }
                }
            });
        }

        // 方向切换按钮
        if (orientationButton != null) {
            orientationButton.setOnClickListener(v -> {
                if (videoEnhancer != null) {
                    videoEnhancer.toggleOrientation();
                }
            });
        }

        // 音量增加按钮
        if (volumeUpButton != null) {
            volumeUpButton.setOnClickListener(v -> {
                if (videoEnhancer != null) {
                    videoEnhancer.increaseVolume();
                    updateVolumeDisplay();
                }
            });
        }

        // 音量减少按钮
        if (volumeDownButton != null) {
            volumeDownButton.setOnClickListener(v -> {
                if (videoEnhancer != null) {
                    videoEnhancer.decreaseVolume();
                    updateVolumeDisplay();
                }
            });
        }

        // 静音按钮
        if (muteButton != null) {
            muteButton.setOnClickListener(v -> {
                if (videoEnhancer != null) {
                    videoEnhancer.toggleMute();
                    isMuted = !isMuted;
                    updateMuteButtonState();
                }
            });
        }

        // 音量滑块
        if (volumeSeekBar != null) {
            volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && videoEnhancer != null) {
                        videoEnhancer.setVolume(progress);
                        currentVolume = progress;
                        updateVolumeDisplay();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    /**
     * 退出全屏
     */
    private void exitFullscreen() {
        // 这里需要通过JavaScript或者其他方式通知WebView退出全屏
        // 由于我们没有直接访问WebView的引用，我们可以通过回调或者其他方式处理
        if (getContext() instanceof YCWebViewActivity) {
            ((YCWebViewActivity) getContext()).exitVideoFullscreen();
        }
    }

    /**
     * 更新控件状态
     */
    private void updateControlsState() {
        if (videoEnhancer != null) {
            // 更新全屏按钮状态
            updateFullscreenButtonState();

            // 更新音量信息
            maxVolume = videoEnhancer.getMaxVolume();
            currentVolume = videoEnhancer.getCurrentVolume();

            if (volumeSeekBar != null) {
                volumeSeekBar.setMax(maxVolume);
                volumeSeekBar.setProgress(currentVolume);
            }

            updateVolumeDisplay();
        }
    }

    /**
     * 更新全屏按钮状态
     */
    private void updateFullscreenButtonState() {
        if (fullscreenButton != null && videoEnhancer != null) {
            if (videoEnhancer.isFullscreen()) {
                fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit);
                fullscreenButton.setContentDescription("退出全屏");
            } else {
                fullscreenButton.setImageResource(R.drawable.ic_fullscreen);
                fullscreenButton.setContentDescription("全屏");
            }
        }
    }

    /**
     * 更新静音按钮状态
     */
    private void updateMuteButtonState() {
        if (muteButton != null) {
            if (isMuted) {
                muteButton.setImageResource(R.drawable.ic_volume_off);
                muteButton.setContentDescription("取消静音");
            } else {
                muteButton.setImageResource(R.drawable.ic_volume_up);
                muteButton.setContentDescription("静音");
            }
        }
    }

    /**
     * 更新音量显示
     */
    private void updateVolumeDisplay() {
        if (volumeText != null) {
            volumeText.setText(currentVolume + "/" + maxVolume);
        }
    }

    /**
     * 显示控制界面
     */
    public void show() {
        setVisibility(View.VISIBLE);
        updateControlsState();
    }

    /**
     * 隐藏控制界面
     */
    public void hide() {
        setVisibility(View.GONE);
    }

    /**
     * 切换显示状态
     */
    public void toggle() {
        if (getVisibility() == View.VISIBLE) {
            hide();
        } else {
            show();
        }
    }

    /**
     * 设置当前音量
     */
    public void setCurrentVolume(int volume) {
        this.currentVolume = volume;
        if (volumeSeekBar != null) {
            volumeSeekBar.setProgress(volume);
        }
        updateVolumeDisplay();
    }

    /**
     * 设置全屏状态
     */
    public void setFullscreen(boolean isFullscreen) {
        updateFullscreenButtonState();
    }
}
