package com.hippo.ehviewer.ui.scene.gallery.list;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.gallery.enhanced.EnhancedGalleryListProvider;
import com.hippo.ehviewer.util.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 增强型画廊列表适配器
 * 集成优化的加载机制，提供平滑的用户体验
 * 
 * 特性：
 * 1. 平滑加载动画
 * 2. 智能加载状态管理
 * 3. 错误重试机制
 * 4. 网络状态感知
 * 5. 缓存优先显示
 */
public class EnhancedGalleryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final String TAG = "EnhancedGalleryListAdapter";
    
    // ViewType常量
    private static final int VIEW_TYPE_GALLERY = 0;
    private static final int VIEW_TYPE_LOADING = 1;
    private static final int VIEW_TYPE_ERROR = 2;
    private static final int VIEW_TYPE_EMPTY = 3;
    
    // 加载状态
    public enum LoadState {
        IDLE,           // 空闲状态
        LOADING,        // 加载中
        LOADED,         // 加载完成
        ERROR,          // 加载错误
        EMPTY           // 空结果
    }
    
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final Handler mMainHandler;
    private final EnhancedGalleryListProvider mProvider;
    
    // 数据管理
    private final List<GalleryInfo> mGalleryList;
    private LoadState mLoadState = LoadState.IDLE;
    private String mCurrentUrl;
    private String mErrorMessage;
    private boolean mHasMore = true;
    
    // 监听器
    private OnGalleryClickListener mOnGalleryClickListener;
    private OnLoadMoreListener mOnLoadMoreListener;
    private OnRetryListener mOnRetryListener;
    
    // 动画配置
    private static final int FADE_DURATION = 300;
    private static final int SLIDE_DURATION = 200;
    
    /**
     * 画廊点击监听器
     */
    public interface OnGalleryClickListener {
        void onGalleryClick(GalleryInfo gallery, int position);
        void onGalleryLongClick(GalleryInfo gallery, int position);
    }
    
    /**
     * 加载更多监听器
     */
    public interface OnLoadMoreListener {
        void onLoadMore();
    }
    
    /**
     * 重试监听器
     */
    public interface OnRetryListener {
        void onRetry();
    }
    
    /**
     * 画廊ViewHolder
     */
    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        private GalleryInfo mGalleryInfo;
        
        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        
        public void bind(GalleryInfo galleryInfo, OnGalleryClickListener listener) {
            mGalleryInfo = galleryInfo;
            
            if (listener != null) {
                itemView.setOnClickListener(v -> 
                    listener.onGalleryClick(galleryInfo, getAdapterPosition()));
                itemView.setOnLongClickListener(v -> {
                    listener.onGalleryLongClick(galleryInfo, getAdapterPosition());
                    return true;
                });
            }
            
            // 添加淡入动画
            itemView.setAlpha(0f);
            itemView.animate()
                .alpha(1f)
                .setDuration(FADE_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
    }
    
    /**
     * 加载状态ViewHolder
     */
    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public final ProgressBar progressBar;
        public final TextView statusText;
        
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            statusText = itemView.findViewById(R.id.status_text);
        }
        
        public void bind(LoadState state, String message, NetworkUtils.NetworkType networkType) {
            switch (state) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    statusText.setText("正在加载...");
                    if (networkType == NetworkUtils.NetworkType.MOBILE_2G) {
                        statusText.append(" (网络较慢，请耐心等待)");
                    }
                    break;
                    
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("加载失败: " + (message != null ? message : "网络错误"));
                    break;
                    
                case EMPTY:
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("没有找到相关内容");
                    break;
                    
                default:
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("");
                    break;
            }
        }
    }
    
    /**
     * 错误重试ViewHolder
     */
    public static class ErrorViewHolder extends RecyclerView.ViewHolder {
        public final TextView errorText;
        public final View retryButton;
        
        public ErrorViewHolder(@NonNull View itemView) {
            super(itemView);
            errorText = itemView.findViewById(R.id.error_text);
            retryButton = itemView.findViewById(R.id.retry_button);
        }
        
        public void bind(String errorMessage, OnRetryListener listener) {
            errorText.setText(errorMessage != null ? errorMessage : "加载失败，请重试");
            
            if (listener != null) {
                retryButton.setOnClickListener(v -> listener.onRetry());
            }
            
            // 添加脉冲动画提示用户可以重试
            ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0.8f, 1.2f);
            pulseAnimator.setDuration(1000);
            pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnimator.addUpdateListener(animation -> {
                float scale = (Float) animation.getAnimatedValue();
                retryButton.setScaleX(scale);
                retryButton.setScaleY(scale);
            });
            pulseAnimator.start();
            
            // ViewHolder被回收时停止动画
            itemView.setTag(pulseAnimator);
        }
    }
    
    public EnhancedGalleryListAdapter(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mInflater = LayoutInflater.from(context);
        mMainHandler = new Handler(Looper.getMainLooper());
        mProvider = new EnhancedGalleryListProvider(context);
        
        mGalleryList = new ArrayList<>();
        
        Log.d(TAG, "Enhanced Gallery List Adapter initialized");
    }
    
    @Override
    public int getItemCount() {
        // 画廊数量 + 1个状态项（加载/错误/空状态）
        return mGalleryList.size() + (mLoadState != LoadState.LOADED || mHasMore ? 1 : 0);
    }
    
    @Override
    public int getItemViewType(int position) {
        if (position < mGalleryList.size()) {
            return VIEW_TYPE_GALLERY;
        } else {
            // 状态项
            switch (mLoadState) {
                case LOADING:
                    return VIEW_TYPE_LOADING;
                case ERROR:
                    return VIEW_TYPE_ERROR;
                case EMPTY:
                    return VIEW_TYPE_EMPTY;
                default:
                    return VIEW_TYPE_LOADING;
            }
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_GALLERY:
                View galleryView = mInflater.inflate(R.layout.item_gallery, parent, false);
                return new GalleryViewHolder(galleryView);
                
            case VIEW_TYPE_LOADING:
            case VIEW_TYPE_EMPTY:
                View loadingView = mInflater.inflate(R.layout.item_loading_state, parent, false);
                return new LoadingViewHolder(loadingView);
                
            case VIEW_TYPE_ERROR:
                View errorView = mInflater.inflate(R.layout.item_error_retry, parent, false);
                return new ErrorViewHolder(errorView);
                
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        
        switch (viewType) {
            case VIEW_TYPE_GALLERY:
                GalleryViewHolder galleryHolder = (GalleryViewHolder) holder;
                GalleryInfo gallery = mGalleryList.get(position);
                galleryHolder.bind(gallery, mOnGalleryClickListener);
                break;
                
            case VIEW_TYPE_LOADING:
            case VIEW_TYPE_EMPTY:
                LoadingViewHolder loadingHolder = (LoadingViewHolder) holder;
                NetworkUtils.NetworkType networkType = NetworkUtils.getNetworkType(mContext);
                loadingHolder.bind(mLoadState, mErrorMessage, networkType);
                
                // 如果是加载状态且接近底部，触发加载更多
                if (mLoadState == LoadState.LOADING && position == getItemCount() - 1) {
                    triggerLoadMore();
                }
                break;
                
            case VIEW_TYPE_ERROR:
                ErrorViewHolder errorHolder = (ErrorViewHolder) holder;
                errorHolder.bind(mErrorMessage, mOnRetryListener);
                break;
        }
    }
    
    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        
        // 停止动画避免内存泄漏
        if (holder instanceof ErrorViewHolder) {
            ValueAnimator animator = (ValueAnimator) holder.itemView.getTag();
            if (animator != null) {
                animator.cancel();
                holder.itemView.setTag(null);
            }
        }
    }
    
    /**
     * 加载画廊列表
     */
    public void loadGalleryList(@NonNull String url, int mode, boolean isRefresh) {
        Log.d(TAG, "Loading gallery list: " + url + " (refresh: " + isRefresh + ")");
        
        mCurrentUrl = url;
        
        if (isRefresh) {
            mGalleryList.clear();
            mHasMore = true;
        }
        
        setLoadState(LoadState.LOADING, null);
        
        // 使用增强型提供者加载
        mProvider.loadGalleryList(url, mode, true, new EnhancedGalleryListProvider.LoadListener() {
            @Override
            public void onLoadStart(String url) {
                Log.d(TAG, "Load started: " + url);
            }
            
            @Override
            public void onLoadProgress(String url, int progress) {
                // 可以在这里更新进度条
            }
            
            @Override
            public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                Log.d(TAG, "Load success: " + url + " (cached: " + isFromCache + 
                      ", items: " + result.galleryInfoList.size() + ")");
                
                mMainHandler.post(() -> {
                    if (result.galleryInfoList.isEmpty()) {
                        setLoadState(LoadState.EMPTY, "没有找到相关内容");
                    } else {
                        // 添加新数据
                        int startPosition = mGalleryList.size();
                        mGalleryList.addAll(result.galleryInfoList);
                        
                        // 更新是否还有更多数据
                        mHasMore = result.nextPage > 0 || result.nextHref != null;
                        
                        setLoadState(LoadState.LOADED, null);
                        
                        // 通知数据变化
                        if (startPosition == 0) {
                            notifyDataSetChanged(); // 首次加载或刷新
                        } else {
                            notifyItemRangeInserted(startPosition, result.galleryInfoList.size());
                        }
                        
                        // 添加加载成功的动画提示
                        if (isFromCache) {
                            showCacheHitFeedback();
                        }
                    }
                });
            }
            
            @Override
            public void onLoadError(String url, Exception error, boolean canRetry) {
                Log.e(TAG, "Load error: " + url + " (canRetry: " + canRetry + ")", error);
                
                mMainHandler.post(() -> {
                    String errorMsg = parseErrorMessage(error);
                    setLoadState(LoadState.ERROR, errorMsg);
                });
            }
        });
    }
    
    /**
     * 设置加载状态
     */
    private void setLoadState(LoadState state, @Nullable String message) {
        LoadState oldState = mLoadState;
        mLoadState = state;
        mErrorMessage = message;
        
        // 通知状态项变化
        int statusPosition = mGalleryList.size();
        if (statusPosition < getItemCount()) {
            notifyItemChanged(statusPosition);
        }
        
        Log.d(TAG, "Load state changed: " + oldState + " -> " + state + 
              (message != null ? " (" + message + ")" : ""));
    }
    
    /**
     * 触发加载更多
     */
    private void triggerLoadMore() {
        if (mLoadState != LoadState.LOADING && mHasMore && mOnLoadMoreListener != null) {
            mOnLoadMoreListener.onLoadMore();
        }
    }
    
    /**
     * 解析错误消息
     */
    private String parseErrorMessage(Exception error) {
        if (error == null) {
            return "未知错误";
        }
        
        String message = error.getMessage();
        if (message == null || message.isEmpty()) {
            return error.getClass().getSimpleName();
        }
        
        // 处理常见错误类型
        if (message.contains("timeout") || message.contains("Time out")) {
            return "连接超时，请检查网络连接";
        } else if (message.contains("No address associated with hostname") || 
                   message.contains("Unable to resolve host")) {
            return "无法解析服务器地址，请检查网络设置";
        } else if (message.contains("Connection refused")) {
            return "服务器拒绝连接，请稍后重试";
        } else if (message.contains("SSLException")) {
            return "SSL连接失败，请检查证书设置";
        } else {
            return message.length() > 50 ? message.substring(0, 50) + "..." : message;
        }
    }
    
    /**
     * 显示缓存命中的反馈
     */
    private void showCacheHitFeedback() {
        // 可以显示一个小的提示动画或Toast
        Log.d(TAG, "Gallery list loaded from cache - instant display!");
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        return mProvider.getPerformanceStats();
    }
    
    /**
     * 刷新缓存
     */
    public void refreshCache(String url, int mode) {
        if (url != null) {
            mProvider.refreshCache(url, mode, new EnhancedGalleryListProvider.LoadListener() {
                @Override
                public void onLoadStart(String url) {}
                
                @Override
                public void onLoadProgress(String url, int progress) {}
                
                @Override
                public void onLoadSuccess(String url, GalleryListParser.Result result, boolean isFromCache) {
                    Log.d(TAG, "Cache refreshed for: " + url);
                }
                
                @Override
                public void onLoadError(String url, Exception error, boolean canRetry) {
                    Log.w(TAG, "Failed to refresh cache for: " + url, error);
                }
            });
        }
    }
    
    /**
     * 检查URL是否已缓存
     */
    public boolean isCached(String url) {
        return mProvider.isCached(url);
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupCache() {
        mProvider.cleanupCache();
    }
    
    // Setters for listeners
    public void setOnGalleryClickListener(OnGalleryClickListener listener) {
        mOnGalleryClickListener = listener;
    }
    
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }
    
    public void setOnRetryListener(OnRetryListener listener) {
        mOnRetryListener = listener;
    }
    
    // Getters
    public List<GalleryInfo> getGalleryList() {
        return new ArrayList<>(mGalleryList);
    }
    
    public LoadState getLoadState() {
        return mLoadState;
    }
    
    public boolean hasMore() {
        return mHasMore;
    }
    
    /**
     * 销毁适配器，释放资源
     */
    public void destroy() {
        Log.d(TAG, "Destroying Enhanced Gallery List Adapter");
        
        mGalleryList.clear();
        mProvider.destroy();
        
        // 清理监听器
        mOnGalleryClickListener = null;
        mOnLoadMoreListener = null;
        mOnRetryListener = null;
    }
}