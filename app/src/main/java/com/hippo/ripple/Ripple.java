package com.hippo.ripple;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

/**
 * 安全的 Ripple 效果工具类
 * 修复了 RippleDrawable.mDensity 字段访问问题
 */
public class Ripple {
    
    private static final String TAG = "Ripple";
    
    /**
     * 生成安全的 Ripple 效果
     * 避免使用反射访问 RippleDrawable.mDensity 字段
     */
    public static Drawable generateRippleDrawable(Context context, boolean isDark, Drawable background) {
        try {
            // 使用安全的颜色选择
            int rippleColor = getRippleColor(context, isDark);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 使用 RippleDrawable，但不访问 mDensity 字段
                return new RippleDrawable(
                    android.content.res.ColorStateList.valueOf(rippleColor),
                    background,
                    null
                );
            } else {
                // 对于旧版本，使用 StateListDrawable 作为替代
                return createStateListDrawable(rippleColor, background);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to create ripple drawable, using fallback", e);
            // 返回简单的背景作为后备方案
            return background != null ? background : new ColorDrawable(Color.TRANSPARENT);
        }
    }
    
    /**
     * 获取 Ripple 颜色
     */
    private static int getRippleColor(Context context, boolean isDark) {
        try {
            // 使用主题属性获取颜色
            TypedValue typedValue = new TypedValue();
            int colorRes = isDark ? android.R.attr.colorControlHighlight : android.R.attr.colorControlHighlight;
            
            if (context.getTheme().resolveAttribute(colorRes, typedValue, true)) {
                return typedValue.data;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get theme color", e);
        }
        
        // 默认颜色
        return isDark ? Color.WHITE : Color.BLACK;
    }
    
    /**
     * 创建 StateListDrawable 作为 Ripple 的替代
     */
    private static StateListDrawable createStateListDrawable(int rippleColor, Drawable background) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        
        // 按下状态
        stateListDrawable.addState(
            new int[]{android.R.attr.state_pressed},
            new ColorDrawable(applyAlpha(rippleColor, 0.3f))
        );
        
        // 焦点状态
        stateListDrawable.addState(
            new int[]{android.R.attr.state_focused},
            new ColorDrawable(applyAlpha(rippleColor, 0.2f))
        );
        
        // 默认状态
        stateListDrawable.addState(
            new int[]{},
            background != null ? background : new ColorDrawable(Color.TRANSPARENT)
        );
        
        return stateListDrawable;
    }
    
    /**
     * 应用透明度到颜色
     */
    private static int applyAlpha(int color, float alpha) {
        int alphaValue = (int) (255 * alpha);
        return (alphaValue << 24) | (color & 0x00FFFFFF);
    }
    
    /**
     * 创建简单的点击效果
     */
    public static Drawable createSimpleClickEffect(Context context, boolean isDark) {
        int color = getRippleColor(context, isDark);
        return new ColorDrawable(applyAlpha(color, 0.1f));
    }
    
    /**
     * 为 View 添加 Ripple 效果
     * 这是兼容性方法，用于替代原来的 addRipple 方法
     */
    public static void addRipple(android.view.View view, boolean isDark) {
        if (view == null) {
            return;
        }
        
        try {
            Context context = view.getContext();
            Drawable rippleDrawable = generateRippleDrawable(context, isDark, view.getBackground());
            view.setBackground(rippleDrawable);
        } catch (Exception e) {
            Log.w(TAG, "Failed to add ripple effect to view", e);
            // 如果失败，使用简单的点击效果
            try {
                Context context = view.getContext();
                Drawable simpleEffect = createSimpleClickEffect(context, isDark);
                view.setBackground(simpleEffect);
            } catch (Exception ex) {
                Log.w(TAG, "Failed to add simple click effect", ex);
            }
        }
    }
}
