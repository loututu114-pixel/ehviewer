package com.hippo.ehviewer.ui.browser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.card.MaterialCardView

/**
 * 地址栏动画助手类 - 处理Material Design 3动画效果
 * 支持建议列表展开/收起、聚焦状态变化、选择高亮等动画
 */
class AddressBarAnimator {
    
    companion object {
        // 动画时长常量
        private const val SUGGESTIONS_SHOW_DURATION = 250L
        private const val SUGGESTIONS_HIDE_DURATION = 200L
        private const val FOCUS_CHANGE_DURATION = 200L
        private const val ELEVATION_CHANGE_DURATION = 150L
        
        // 动画插值器
        private val FAST_OUT_SLOW_IN = FastOutSlowInInterpolator()
        private val DECELERATE = DecelerateInterpolator(1.5f)
        private val ACCELERATE_DECELERATE = AccelerateDecelerateInterpolator()
    }
    
    private var suggestionsAnimator: AnimatorSet? = null
    private var focusAnimator: AnimatorSet? = null
    
    /**
     * 显示建议列表动画
     */
    fun showSuggestions(
        suggestionsContainer: MaterialCardView,
        onAnimationEnd: Runnable? = null
    ) {
        // 取消之前的动画
        suggestionsAnimator?.cancel()
        
        // 设置初始状态
        suggestionsContainer.visibility = View.VISIBLE
        
        // 创建动画集合
        val animatorSet = AnimatorSet()
        
        // 透明度动画
        val alphaAnimator = ObjectAnimator.ofFloat(
            suggestionsContainer, "alpha", 0f, 1f
        ).apply {
            duration = SUGGESTIONS_SHOW_DURATION
            interpolator = FAST_OUT_SLOW_IN
        }
        
        // Y轴位移动画
        val translationYAnimator = ObjectAnimator.ofFloat(
            suggestionsContainer, "translationY", -16f, 0f
        ).apply {
            duration = SUGGESTIONS_SHOW_DURATION
            interpolator = FAST_OUT_SLOW_IN
        }
        
        // 缩放Y动画
        val scaleYAnimator = ObjectAnimator.ofFloat(
            suggestionsContainer, "scaleY", 0.8f, 1f
        ).apply {
            duration = SUGGESTIONS_SHOW_DURATION
            interpolator = FAST_OUT_SLOW_IN
        }
        
        // 高度动画（展开效果）
        val heightAnimator = createExpandHeightAnimator(suggestionsContainer)
        
        // 组合动画
        animatorSet.apply {
            playTogether(alphaAnimator, translationYAnimator, scaleYAnimator, heightAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd?.run()
                    suggestionsAnimator = null
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    suggestionsAnimator = null
                }
            })
            start()
        }
        
        suggestionsAnimator = animatorSet
    }
    
    /**
     * 隐藏建议列表动画
     */
    fun hideSuggestions(
        suggestionsContainer: MaterialCardView,
        onAnimationEnd: Runnable? = null
    ) {
        // 取消之前的动画
        suggestionsAnimator?.cancel()
        
        // 创建动画集合
        val animatorSet = AnimatorSet()
        
        // 透明度动画
        val alphaAnimator = ObjectAnimator.ofFloat(
            suggestionsContainer, "alpha", 1f, 0f
        ).apply {
            duration = SUGGESTIONS_HIDE_DURATION
            interpolator = ACCELERATE_DECELERATE
        }
        
        // Y轴位移动画
        val translationYAnimator = ObjectAnimator.ofFloat(
            suggestionsContainer, "translationY", 0f, -8f
        ).apply {
            duration = SUGGESTIONS_HIDE_DURATION
            interpolator = ACCELERATE_DECELERATE
        }
        
        // 缩放Y动画
        val scaleYAnimator = ObjectAnimator.ofFloat(
            suggestionsContainer, "scaleY", 1f, 0.9f
        ).apply {
            duration = SUGGESTIONS_HIDE_DURATION
            interpolator = ACCELERATE_DECELERATE
        }
        
        // 组合动画
        animatorSet.apply {
            playTogether(alphaAnimator, translationYAnimator, scaleYAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    suggestionsContainer.visibility = View.GONE
                    // 重置属性
                    suggestionsContainer.alpha = 0f
                    suggestionsContainer.scaleY = 0.8f
                    suggestionsContainer.translationY = -16f
                    onAnimationEnd?.run()
                    suggestionsAnimator = null
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    suggestionsAnimator = null
                }
            })
            start()
        }
        
        suggestionsAnimator = animatorSet
    }
    
    /**
     * 地址栏聚焦状态动画
     */
    fun animateFocusChange(
        addressBarContainer: MaterialCardView,
        focused: Boolean,
        onAnimationEnd: Runnable? = null
    ) {
        // 取消之前的动画
        focusAnimator?.cancel()
        
        val animatorSet = AnimatorSet()
        
        // 高度动画
        val elevationAnimator = ObjectAnimator.ofFloat(
            addressBarContainer, "cardElevation",
            addressBarContainer.cardElevation,
            if (focused) 8f else 2f
        ).apply {
            duration = FOCUS_CHANGE_DURATION
            interpolator = FAST_OUT_SLOW_IN
        }
        
        // 边框宽度动画（如果支持）
        val strokeWidthAnimator = ValueAnimator.ofInt(
            if (focused) 1 else 2,
            if (focused) 2 else 1
        ).apply {
            duration = FOCUS_CHANGE_DURATION
            interpolator = FAST_OUT_SLOW_IN
            addUpdateListener { animator ->
                addressBarContainer.strokeWidth = animator.animatedValue as Int
            }
        }
        
        // 轻微的缩放效果
        val scaleAnimator = ObjectAnimator.ofFloat(
            addressBarContainer, "scaleX",
            1f, if (focused) 1.02f else 1f
        ).apply {
            duration = FOCUS_CHANGE_DURATION
            interpolator = FAST_OUT_SLOW_IN
        }
        
        animatorSet.apply {
            playTogether(elevationAnimator, strokeWidthAnimator, scaleAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd?.run()
                    focusAnimator = null
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    focusAnimator = null
                }
            })
            start()
        }
        
        focusAnimator = animatorSet
    }
    
    /**
     * 建议项选择高亮动画
     */
    fun animateItemSelection(itemView: View, selected: Boolean) {
        // 取消之前的动画
        itemView.animate().cancel()
        
        val targetElevation = if (selected) 2f else 0f
        val targetScale = if (selected) 1.02f else 1f
        
        itemView.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationZ(targetElevation)
            .setDuration(ELEVATION_CHANGE_DURATION)
            .setInterpolator(FAST_OUT_SLOW_IN)
            .start()
    }
    
    /**
     * 清除按钮显示/隐藏动画
     */
    fun animateClearButtonVisibility(clearButton: View, show: Boolean) {
        clearButton.animate().cancel()
        
        if (show && clearButton.visibility != View.VISIBLE) {
            clearButton.visibility = View.VISIBLE
            clearButton.alpha = 0f
            clearButton.scaleX = 0.8f
            clearButton.scaleY = 0.8f
        }
        
        clearButton.animate()
            .alpha(if (show) 1f else 0f)
            .scaleX(if (show) 1f else 0.8f)
            .scaleY(if (show) 1f else 0.8f)
            .setDuration(150)
            .setInterpolator(FAST_OUT_SLOW_IN)
            .withEndAction {
                if (!show) {
                    clearButton.visibility = View.GONE
                }
            }
            .start()
    }
    
    /**
     * 刷新按钮旋转动画
     */
    fun animateRefreshButton(refreshButton: View, loading: Boolean) {
        if (loading) {
            // 开始旋转动画
            refreshButton.animate()
                .rotationBy(360f)
                .setDuration(1000)
                .setInterpolator(ACCELERATE_DECELERATE)
                .withEndAction {
                    // 如果仍在加载，继续旋转
                    if (refreshButton.tag == "loading") {
                        animateRefreshButton(refreshButton, true)
                    }
                }
                .start()
            refreshButton.tag = "loading"
        } else {
            // 停止旋转动画
            refreshButton.tag = null
            refreshButton.animate()
                .rotation(0f)
                .setDuration(200)
                .setInterpolator(DECELERATE)
                .start()
        }
    }
    
    /**
     * 创建高度展开动画器
     */
    private fun createExpandHeightAnimator(view: ViewGroup): ValueAnimator {
        // 测量目标高度
        view.measure(
            View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = view.measuredHeight
        
        return ValueAnimator.ofInt(0, targetHeight).apply {
            duration = SUGGESTIONS_SHOW_DURATION
            interpolator = FAST_OUT_SLOW_IN
            addUpdateListener { animator ->
                val height = animator.animatedValue as Int
                view.layoutParams = view.layoutParams.apply {
                    this.height = height
                }
                view.requestLayout()
            }
        }
    }
    
    /**
     * 取消所有动画
     */
    fun cancelAllAnimations() {
        suggestionsAnimator?.cancel()
        focusAnimator?.cancel()
        suggestionsAnimator = null
        focusAnimator = null
    }
    
    /**
     * 入场动画 - 整个地址栏组件首次显示
     */
    fun animateInitialEntry(
        addressBarContainer: MaterialCardView,
        suggestionsContainer: MaterialCardView
    ) {
        // 地址栏从上方滑入
        addressBarContainer.translationY = -addressBarContainer.height.toFloat()
        addressBarContainer.alpha = 0f
        
        addressBarContainer.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DECELERATE)
            .start()
        
        // 确保建议容器初始状态正确
        suggestionsContainer.alpha = 0f
        suggestionsContainer.scaleY = 0.8f
        suggestionsContainer.translationY = -16f
        suggestionsContainer.visibility = View.GONE
    }
}