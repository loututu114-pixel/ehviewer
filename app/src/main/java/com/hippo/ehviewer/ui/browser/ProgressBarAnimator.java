package com.hippo.ehviewer.ui.browser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

/**
 * 仿Chrome/微信进度条动画器
 * 提供流畅的加载进度动画效果
 */
public class ProgressBarAnimator {

    private static final String TAG = "ProgressBarAnimator";
    private static final int ANIMATION_DURATION = 200;
    private static final int HIDE_DELAY = 500;
    
    private ProgressBar mProgressBar;
    private ValueAnimator mProgressAnimator;
    private ValueAnimator mFadeAnimator;
    private int mCurrentProgress = 0;
    private boolean mIsVisible = false;
    
    public ProgressBarAnimator(ProgressBar progressBar) {
        this.mProgressBar = progressBar;
        setupProgressBar();
    }
    
    private void setupProgressBar() {
        // 设置进度条样式
        mProgressBar.setMax(100);
        mProgressBar.setProgress(0);
        mProgressBar.setVisibility(View.GONE);
        
        // 设置进度条颜色 - Chrome风格
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(0xFF4285F4) // Chrome蓝色
            );
            mProgressBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0x1A000000) // 10%透明黑色
            );
        }
    }
    
    /**
     * 更新进度 - 带动画效果
     */
    public void updateProgress(int progress) {
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;
        
        // 如果进度条未显示，先显示
        if (!mIsVisible && progress > 0 && progress < 100) {
            showProgressBar();
        }
        
        // 动画更新进度
        animateProgress(mCurrentProgress, progress);
        mCurrentProgress = progress;
        
        // 如果加载完成，延迟隐藏进度条
        if (progress >= 100) {
            hideProgressBarDelayed();
        }
    }
    
    /**
     * 显示进度条
     */
    private void showProgressBar() {
        if (mIsVisible) return;
        
        mIsVisible = true;
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setAlpha(0f);
        
        // 淡入动画
        mProgressBar.animate()
            .alpha(1f)
            .setDuration(150)
            .setInterpolator(new DecelerateInterpolator())
            .start();
        
        android.util.Log.d(TAG, "Progress bar shown");
    }
    
    /**
     * 延迟隐藏进度条
     */
    private void hideProgressBarDelayed() {
        mProgressBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideProgressBar();
            }
        }, HIDE_DELAY);
    }
    
    /**
     * 隐藏进度条
     */
    private void hideProgressBar() {
        if (!mIsVisible) return;
        
        // 淡出动画
        mProgressBar.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(new DecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressBar.setVisibility(View.GONE);
                    mProgressBar.setProgress(0);
                    mCurrentProgress = 0;
                    mIsVisible = false;
                    android.util.Log.d(TAG, "Progress bar hidden");
                }
            })
            .start();
    }
    
    /**
     * 动画更新进度值
     */
    private void animateProgress(int fromProgress, int toProgress) {
        if (mProgressAnimator != null) {
            mProgressAnimator.cancel();
        }
        
        mProgressAnimator = ValueAnimator.ofInt(fromProgress, toProgress);
        mProgressAnimator.setDuration(ANIMATION_DURATION);
        mProgressAnimator.setInterpolator(new DecelerateInterpolator());
        
        mProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (Integer) animation.getAnimatedValue();
                mProgressBar.setProgress(animatedValue);
            }
        });
        
        mProgressAnimator.start();
    }
    
    /**
     * 强制隐藏进度条
     */
    public void forceHide() {
        if (mProgressAnimator != null) {
            mProgressAnimator.cancel();
        }
        if (mFadeAnimator != null) {
            mFadeAnimator.cancel();
        }
        
        mProgressBar.setVisibility(View.GONE);
        mProgressBar.setProgress(0);
        mCurrentProgress = 0;
        mIsVisible = false;
    }
    
    /**
     * 重置进度条状态
     */
    public void reset() {
        forceHide();
        android.util.Log.d(TAG, "Progress bar reset");
    }
    
    /**
     * 获取当前进度
     */
    public int getCurrentProgress() {
        return mCurrentProgress;
    }
    
    /**
     * 是否正在显示
     */
    public boolean isVisible() {
        return mIsVisible;
    }
}