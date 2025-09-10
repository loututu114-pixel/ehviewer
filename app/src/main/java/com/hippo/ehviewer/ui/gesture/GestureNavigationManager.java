package com.hippo.ehviewer.ui.gesture;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * 手势导航管理器
 * 提供现代浏览器的手势操作体验
 * 
 * 核心特性：
 * 1. 滑动前进后退导航
 * 2. 双击缩放功能
 * 3. 长按菜单支持
 * 4. 可配置的手势敏感度
 */
public class GestureNavigationManager {
    private static final String TAG = "GestureNavigationManager";
    
    // SharedPreferences键值
    private static final String PREFS_NAME = "gesture_settings";
    private static final String PREF_SWIPE_NAVIGATION = "swipe_navigation";
    private static final String PREF_DOUBLE_TAP_ZOOM = "double_tap_zoom";
    private static final String PREF_GESTURE_SENSITIVITY = "gesture_sensitivity";
    private static final String PREF_EDGE_SWIPE_ONLY = "edge_swipe_only";
    
    // 手势参数
    private static final int MIN_SWIPE_DISTANCE = 100; // 最小滑动距离
    private static final int MAX_SWIPE_OFF_PATH = 200; // 最大偏移距离
    private static final int MIN_SWIPE_VELOCITY = 200; // 最小滑动速度
    private static final float EDGE_SWIPE_THRESHOLD = 50; // 边缘滑动阈值
    
    // 缩放参数
    private static final float MIN_ZOOM_SCALE = 0.5f;
    private static final float MAX_ZOOM_SCALE = 5.0f;
    private static final float DEFAULT_ZOOM_SCALE = 1.0f;
    
    // 组件引用
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final WebView mWebView;
    
    // 手势检测器
    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleGestureDetector;
    
    // 设置状态
    private boolean mSwipeNavigationEnabled = true;
    private boolean mDoubleTabZoomEnabled = true;
    private boolean mEdgeSwipeOnly = true;
    private float mGestureSensitivity = 1.0f;
    
    // 当前状态
    private boolean mIsScaling = false;
    private float mCurrentScale = DEFAULT_ZOOM_SCALE;
    private long mLastBackPressTime = 0;
    
    // 手势监听器
    private GestureNavigationListener mListener;
    
    /**
     * 手势导航监听器
     */
    public interface GestureNavigationListener {
        boolean onSwipeBack();
        boolean onSwipeForward();
        void onDoubleTabZoom(float centerX, float centerY);
        void onLongPress(float x, float y);
        void onScaleChanged(float scaleFactor);
    }
    
    public GestureNavigationManager(@NonNull Context context, @NonNull WebView webView) {
        mContext = context;
        mWebView = webView;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 加载设置
        loadSettings();
        
        // 创建手势检测器
        mGestureDetector = new GestureDetector(context, new SwipeGestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
        
        // 设置WebView的触摸监听器
        setupWebViewTouchListener();
        
        Log.d(TAG, "GestureNavigationManager initialized with WebView");
    }
    
    /**
     * 加载设置
     */
    private void loadSettings() {
        mSwipeNavigationEnabled = mPrefs.getBoolean(PREF_SWIPE_NAVIGATION, true);
        mDoubleTabZoomEnabled = mPrefs.getBoolean(PREF_DOUBLE_TAP_ZOOM, true);
        mEdgeSwipeOnly = mPrefs.getBoolean(PREF_EDGE_SWIPE_ONLY, true);
        mGestureSensitivity = mPrefs.getFloat(PREF_GESTURE_SENSITIVITY, 1.0f);
        
        Log.d(TAG, "Settings loaded: swipe=" + mSwipeNavigationEnabled + 
               ", doubleTap=" + mDoubleTabZoomEnabled + 
               ", edgeOnly=" + mEdgeSwipeOnly + 
               ", sensitivity=" + mGestureSensitivity);
    }
    
    /**
     * 设置WebView触摸监听器
     */
    private void setupWebViewTouchListener() {
        mWebView.setOnTouchListener((v, event) -> {
            boolean handled = false;
            
            // 先处理缩放手势
            if (mDoubleTabZoomEnabled) {
                handled = mScaleGestureDetector.onTouchEvent(event);
            }
            
            // 再处理滑动手势（如果不在缩放中）
            if (!mIsScaling && mSwipeNavigationEnabled) {
                handled = mGestureDetector.onTouchEvent(event) || handled;
            }
            
            // 如果手势被处理，不传递给WebView
            return handled;
        });
    }
    
    /**
     * 滑动手势监听器
     */
    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            
            try {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                
                // 检查是否为有效的水平滑动
                if (Math.abs(diffX) > Math.abs(diffY) && 
                    Math.abs(diffX) > MIN_SWIPE_DISTANCE * mGestureSensitivity &&
                    Math.abs(diffY) < MAX_SWIPE_OFF_PATH &&
                    Math.abs(velocityX) > MIN_SWIPE_VELOCITY * mGestureSensitivity) {
                    
                    // 检查边缘滑动限制
                    if (mEdgeSwipeOnly && !isEdgeSwipe(e1.getX())) {
                        return false;
                    }
                    
                    if (diffX > 0) {
                        // 向右滑动 - 后退
                        return handleSwipeBack();
                    } else {
                        // 向左滑动 - 前进
                        return handleSwipeForward();
                    }
                }
                
            } catch (Exception e) {
                Log.w(TAG, "Error handling swipe gesture", e);
            }
            
            return false;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mDoubleTabZoomEnabled && mListener != null) {
                Log.d(TAG, "Double tap detected at (" + e.getX() + ", " + e.getY() + ")");
                mListener.onDoubleTabZoom(e.getX(), e.getY());
                return true;
            }
            return false;
        }
        
        @Override
        public void onLongPress(MotionEvent e) {
            if (mListener != null) {
                Log.d(TAG, "Long press detected at (" + e.getX() + ", " + e.getY() + ")");
                mListener.onLongPress(e.getX(), e.getY());
            }
        }
    }
    
    /**
     * 缩放手势监听器
     */
    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mIsScaling = true;
            Log.d(TAG, "Scale gesture began");
            return true;
        }
        
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = mCurrentScale * scaleFactor;
            
            // 限制缩放范围
            newScale = Math.max(MIN_ZOOM_SCALE, Math.min(newScale, MAX_ZOOM_SCALE));
            
            if (newScale != mCurrentScale) {
                mCurrentScale = newScale;
                if (mListener != null) {
                    mListener.onScaleChanged(scaleFactor);
                }
            }
            
            return true;
        }
        
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mIsScaling = false;
            Log.d(TAG, "Scale gesture ended, final scale: " + mCurrentScale);
        }
    }
    
    /**
     * 处理向后滑动
     */
    private boolean handleSwipeBack() {
        if (mListener != null) {
            boolean handled = mListener.onSwipeBack();
            if (handled) {
                Log.d(TAG, "Swipe back handled by listener");
                showSwipeHint("后退");
            }
            return handled;
        }
        return false;
    }
    
    /**
     * 处理向前滑动
     */
    private boolean handleSwipeForward() {
        if (mListener != null) {
            boolean handled = mListener.onSwipeForward();
            if (handled) {
                Log.d(TAG, "Swipe forward handled by listener");
                showSwipeHint("前进");
            }
            return handled;
        }
        return false;
    }
    
    /**
     * 检查是否为边缘滑动
     */
    private boolean isEdgeSwipe(float startX) {
        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        return startX < EDGE_SWIPE_THRESHOLD || startX > (screenWidth - EDGE_SWIPE_THRESHOLD);
    }
    
    /**
     * 显示滑动提示
     */
    private void showSwipeHint(String action) {
        try {
            Toast.makeText(mContext, "手势" + action, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w(TAG, "Failed to show swipe hint", e);
        }
    }
    
    /**
     * 设置手势监听器
     */
    public void setGestureNavigationListener(GestureNavigationListener listener) {
        mListener = listener;
    }
    
    /**
     * 启用/禁用滑动导航
     */
    public void setSwipeNavigationEnabled(boolean enabled) {
        if (mSwipeNavigationEnabled != enabled) {
            mSwipeNavigationEnabled = enabled;
            saveSwipeNavigationSetting();
            Log.d(TAG, "Swipe navigation " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 是否启用滑动导航
     */
    public boolean isSwipeNavigationEnabled() {
        return mSwipeNavigationEnabled;
    }
    
    /**
     * 启用/禁用双击缩放
     */
    public void setDoubleTabZoomEnabled(boolean enabled) {
        if (mDoubleTabZoomEnabled != enabled) {
            mDoubleTabZoomEnabled = enabled;
            saveDoubleTabZoomSetting();
            Log.d(TAG, "Double tap zoom " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 是否启用双击缩放
     */
    public boolean isDoubleTabZoomEnabled() {
        return mDoubleTabZoomEnabled;
    }
    
    /**
     * 设置边缘滑动限制
     */
    public void setEdgeSwipeOnly(boolean enabled) {
        if (mEdgeSwipeOnly != enabled) {
            mEdgeSwipeOnly = enabled;
            saveEdgeSwipeOnlySetting();
            Log.d(TAG, "Edge swipe only " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 是否只允许边缘滑动
     */
    public boolean isEdgeSwipeOnly() {
        return mEdgeSwipeOnly;
    }
    
    /**
     * 设置手势敏感度
     */
    public void setGestureSensitivity(float sensitivity) {
        sensitivity = Math.max(0.5f, Math.min(sensitivity, 2.0f)); // 限制范围
        if (Math.abs(mGestureSensitivity - sensitivity) > 0.1f) {
            mGestureSensitivity = sensitivity;
            saveGestureSensitivitySetting();
            Log.d(TAG, "Gesture sensitivity set to: " + sensitivity);
        }
    }
    
    /**
     * 获取手势敏感度
     */
    public float getGestureSensitivity() {
        return mGestureSensitivity;
    }
    
    /**
     * 重置当前缩放
     */
    public void resetZoom() {
        mCurrentScale = DEFAULT_ZOOM_SCALE;
        Log.d(TAG, "Zoom reset to default scale");
    }
    
    /**
     * 获取当前缩放比例
     */
    public float getCurrentScale() {
        return mCurrentScale;
    }
    
    /**
     * 获取手势设置统计信息
     */
    public String getGestureStats() {
        return String.format("滑动导航: %s\n双击缩放: %s\n边缘限制: %s\n敏感度: %.1f\n当前缩放: %.1fx",
                           mSwipeNavigationEnabled ? "启用" : "禁用",
                           mDoubleTabZoomEnabled ? "启用" : "禁用", 
                           mEdgeSwipeOnly ? "启用" : "禁用",
                           mGestureSensitivity,
                           mCurrentScale);
    }
    
    /**
     * 保存滑动导航设置
     */
    private void saveSwipeNavigationSetting() {
        mPrefs.edit().putBoolean(PREF_SWIPE_NAVIGATION, mSwipeNavigationEnabled).apply();
    }
    
    /**
     * 保存双击缩放设置
     */
    private void saveDoubleTabZoomSetting() {
        mPrefs.edit().putBoolean(PREF_DOUBLE_TAP_ZOOM, mDoubleTabZoomEnabled).apply();
    }
    
    /**
     * 保存边缘滑动设置
     */
    private void saveEdgeSwipeOnlySetting() {
        mPrefs.edit().putBoolean(PREF_EDGE_SWIPE_ONLY, mEdgeSwipeOnly).apply();
    }
    
    /**
     * 保存手势敏感度设置
     */
    private void saveGestureSensitivitySetting() {
        mPrefs.edit().putFloat(PREF_GESTURE_SENSITIVITY, mGestureSensitivity).apply();
    }
}