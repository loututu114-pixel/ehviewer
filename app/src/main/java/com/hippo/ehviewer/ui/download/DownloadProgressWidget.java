package com.hippo.ehviewer.ui.download;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.download.EnhancedDownloadManager;

import java.text.DecimalFormat;

/**
 * 下载进度显示组件 - 增强版
 * 
 * 功能特性：
 * 1. 实时进度显示和动画
 * 2. 下载速度和剩余时间估算
 * 3. 文件大小格式化显示
 * 4. 暂停/恢复/取消操作
 * 5. 状态指示和错误显示
 */
public class DownloadProgressWidget extends LinearLayout {
    private static final String TAG = "DownloadProgressWidget";
    
    // UI组件
    private TextView mFileNameText;
    private TextView mProgressText;
    private TextView mSizeText; 
    private TextView mSpeedText;
    private TextView mStatusText;
    private ProgressBar mProgressBar;
    private Button mPauseResumeButton;
    private Button mCancelButton;
    
    // 数据
    private String mTaskId;
    private String mFileName;
    private long mTotalSize;
    private long mDownloadedSize;
    private int mProgress;
    private long mSpeed;
    private String mStatus = "准备中";
    private boolean mIsPaused = false;
    
    // 动画
    private ObjectAnimator mProgressAnimator;
    private ValueAnimator mSpeedAnimator;
    
    // 监听器
    private OnDownloadActionListener mActionListener;
    
    public interface OnDownloadActionListener {
        void onPauseDownload(String taskId);
        void onResumeDownload(String taskId);
        void onCancelDownload(String taskId);
    }
    
    public DownloadProgressWidget(Context context) {
        super(context);
        init();
    }
    
    public DownloadProgressWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public DownloadProgressWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setOrientation(VERTICAL);
        LayoutInflater.from(getContext()).inflate(R.layout.widget_download_progress, this, true);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        mFileNameText = findViewById(R.id.tv_file_name);
        mProgressText = findViewById(R.id.tv_progress);
        mSizeText = findViewById(R.id.tv_size);
        mSpeedText = findViewById(R.id.tv_speed);
        mStatusText = findViewById(R.id.tv_status);
        mProgressBar = findViewById(R.id.progress_bar);
        mPauseResumeButton = findViewById(R.id.btn_pause_resume);
        mCancelButton = findViewById(R.id.btn_cancel);
        
        // 设置初始状态
        updateUI();
    }
    
    private void setupListeners() {
        mPauseResumeButton.setOnClickListener(v -> {
            if (mActionListener != null && mTaskId != null) {
                if (mIsPaused) {
                    mActionListener.onResumeDownload(mTaskId);
                } else {
                    mActionListener.onPauseDownload(mTaskId);
                }
            }
        });
        
        mCancelButton.setOnClickListener(v -> {
            if (mActionListener != null && mTaskId != null) {
                mActionListener.onCancelDownload(mTaskId);
            }
        });
    }
    
    /**
     * 设置下载任务信息
     */
    public void setDownloadTask(String taskId, String fileName) {
        mTaskId = taskId;
        mFileName = fileName;
        updateUI();
    }
    
    /**
     * 更新下载进度
     */
    public void updateProgress(EnhancedDownloadManager.DownloadProgress progress) {
        if (progress == null) return;
        
        mTotalSize = progress.totalBytes;
        mDownloadedSize = progress.downloadedBytes;
        mProgress = progress.progress;
        mSpeed = progress.speed;
        mStatus = progress.status;
        mIsPaused = "已暂停".equals(progress.status);
        
        updateUI();
    }
    
    /**
     * 更新UI显示
     */
    private void updateUI() {
        // 文件名
        if (mFileName != null) {
            mFileNameText.setText(mFileName);
        }
        
        // 进度百分比
        mProgressText.setText(mProgress + "%");
        
        // 文件大小信息
        String sizeInfo = formatFileSize(mDownloadedSize);
        if (mTotalSize > 0) {
            sizeInfo += " / " + formatFileSize(mTotalSize);
        }
        mSizeText.setText(sizeInfo);
        
        // 下载速度
        if (mSpeed > 0) {
            mSpeedText.setText(formatFileSize(mSpeed) + "/s");
            mSpeedText.setVisibility(VISIBLE);
        } else {
            mSpeedText.setVisibility(GONE);
        }
        
        // 状态信息
        mStatusText.setText(mStatus);
        
        // 进度条动画
        updateProgressBar();
        
        // 按钮状态
        updateButtons();
    }
    
    /**
     * 更新进度条
     */
    private void updateProgressBar() {
        if (mProgressAnimator != null) {
            mProgressAnimator.cancel();
        }
        
        // 平滑动画更新进度条
        mProgressAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", mProgressBar.getProgress(), mProgress);
        mProgressAnimator.setDuration(300);
        mProgressAnimator.setInterpolator(new DecelerateInterpolator());
        mProgressAnimator.start();
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtons() {
        if (mIsPaused) {
            mPauseResumeButton.setText("继续");
            mPauseResumeButton.setEnabled(true);
        } else if ("下载中".equals(mStatus)) {
            mPauseResumeButton.setText("暂停");
            mPauseResumeButton.setEnabled(true);
        } else if ("已完成".equals(mStatus)) {
            mPauseResumeButton.setText("完成");
            mPauseResumeButton.setEnabled(false);
            mCancelButton.setEnabled(false);
        } else if ("下载失败".equals(mStatus)) {
            mPauseResumeButton.setText("重试");
            mPauseResumeButton.setEnabled(true);
        } else {
            mPauseResumeButton.setText("暂停");
            mPauseResumeButton.setEnabled(false);
        }
    }
    
    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        return decimalFormat.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    /**
     * 估算剩余时间
     */
    private String estimateRemainingTime() {
        if (mSpeed <= 0 || mTotalSize <= 0 || mDownloadedSize >= mTotalSize) {
            return "";
        }
        
        long remainingBytes = mTotalSize - mDownloadedSize;
        long remainingSeconds = remainingBytes / mSpeed;
        
        if (remainingSeconds < 60) {
            return remainingSeconds + "秒";
        } else if (remainingSeconds < 3600) {
            return (remainingSeconds / 60) + "分钟";
        } else {
            return (remainingSeconds / 3600) + "小时";
        }
    }
    
    /**
     * 设置操作监听器
     */
    public void setOnDownloadActionListener(OnDownloadActionListener listener) {
        mActionListener = listener;
    }
    
    /**
     * 获取任务ID
     */
    public String getTaskId() {
        return mTaskId;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (mProgressAnimator != null) {
            mProgressAnimator.cancel();
        }
        if (mSpeedAnimator != null) {
            mSpeedAnimator.cancel();
        }
    }
}