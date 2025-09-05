package com.hippo.ehviewer.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Debug;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存优化器
 * 负责管理应用内存使用，防止内存泄漏，优化内存分配
 */
public class MemoryOptimizer {
    
    private static final String TAG = "MemoryOptimizer";
    
    // 内存警告阈值
    private static final float MEMORY_WARNING_THRESHOLD = 0.85f;  // 85%
    private static final float MEMORY_CRITICAL_THRESHOLD = 0.95f; // 95%
    
    // 清理间隔
    private static final long ROUTINE_CLEANUP_INTERVAL = 5 * 60 * 1000; // 5分钟
    private static final long AGGRESSIVE_CLEANUP_INTERVAL = 30 * 1000;   // 30秒
    
    private static volatile MemoryOptimizer sInstance;
    
    private final Context mContext;
    private final ActivityManager mActivityManager;
    private final ScheduledExecutorService mScheduler;
    private final ExecutorService mExecutor;
    
    // 内存监控
    private final List<WeakReference<MemoryPressureListener>> mListeners;
    private boolean mIsLowMemoryMode = false;
    private long mLastCleanupTime = 0;
    
    // 缓存管理
    private final LruCache<String, Bitmap> mBitmapCache;
    private final List<WeakReference<Object>> mManagedObjects;
    
    // 性能统计
    private long mTotalMemoryFreed = 0;
    private int mCleanupCount = 0;
    private long mPeakMemoryUsage = 0;
    
    public interface MemoryPressureListener {
        void onMemoryPressure(MemoryPressureLevel level);
        void onMemoryRecovered();
    }
    
    public enum MemoryPressureLevel {
        LOW,      // 内存使用正常
        MODERATE, // 内存使用较高，建议清理
        HIGH,     // 内存使用很高，需要立即清理
        CRITICAL  // 内存即将耗尽，紧急清理
    }
    
    private MemoryOptimizer(Context context) {
        mContext = context.getApplicationContext();
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mScheduler = Executors.newScheduledThreadPool(1);
        mExecutor = Executors.newSingleThreadExecutor();
        
        mListeners = new ArrayList<>();
        mManagedObjects = new ArrayList<>();
        
        // 初始化Bitmap缓存
        int cacheSize = calculateOptimalCacheSize();
        mBitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (evicted && !oldValue.isRecycled()) {
                    Log.d(TAG, "Bitmap evicted from cache: " + key);
                }
            }
        };
        
        // 启动内存监控
        startMemoryMonitoring();
    }
    
    public static MemoryOptimizer getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MemoryOptimizer.class) {
                if (sInstance == null) {
                    sInstance = new MemoryOptimizer(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 启动内存监控
     */
    private void startMemoryMonitoring() {
        // 定期内存检查
        mScheduler.scheduleWithFixedDelay(() -> {
            try {
                checkMemoryPressure();
                performRoutineCleanup();
            } catch (Exception e) {
                Log.e(TAG, "Error in memory monitoring", e);
            }
        }, 30, 30, TimeUnit.SECONDS); // 每30秒检查一次
        
        Log.d(TAG, "Memory monitoring started");
    }
    
    /**
     * 检查内存压力
     */
    private void checkMemoryPressure() {
        try {
            MemoryInfo memInfo = getCurrentMemoryInfo();
            float memoryUsage = 1.0f - (float) memInfo.availableMem / memInfo.totalMem;
            
            MemoryPressureLevel currentLevel = determineMemoryPressureLevel(memoryUsage);
            
            // 更新峰值内存使用
            long currentUsage = memInfo.totalMem - memInfo.availableMem;
            if (currentUsage > mPeakMemoryUsage) {
                mPeakMemoryUsage = currentUsage;
            }
            
            // 根据内存压力调整策略
            handleMemoryPressure(currentLevel, memoryUsage);
            
            Log.d(TAG, String.format("Memory usage: %.1f%%, Level: %s", 
                memoryUsage * 100, currentLevel));
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking memory pressure", e);
        }
    }
    
    /**
     * 获取当前内存信息
     */
    private MemoryInfo getCurrentMemoryInfo() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(memInfo);
        
        // 获取更详细的内存信息
        Debug.MemoryInfo debugMemInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(debugMemInfo);
        
        return new MemoryInfo(memInfo, debugMemInfo);
    }
    
    /**
     * 内存信息包装类
     */
    private static class MemoryInfo {
        public final long availableMem;
        public final long totalMem;
        public final boolean lowMemory;
        public final int dalvikPss;
        public final int nativePss;
        public final int otherPss;
        
        public MemoryInfo(ActivityManager.MemoryInfo memInfo, Debug.MemoryInfo debugInfo) {
            this.availableMem = memInfo.availMem;
            this.totalMem = memInfo.totalMem;
            this.lowMemory = memInfo.lowMemory;
            this.dalvikPss = debugInfo.dalvikPss;
            this.nativePss = debugInfo.nativePss;
            this.otherPss = debugInfo.otherPss;
        }
        
        public int getTotalPss() {
            return dalvikPss + nativePss + otherPss;
        }
    }
    
    /**
     * 确定内存压力级别
     */
    private MemoryPressureLevel determineMemoryPressureLevel(float memoryUsage) {
        if (memoryUsage >= MEMORY_CRITICAL_THRESHOLD) {
            return MemoryPressureLevel.CRITICAL;
        } else if (memoryUsage >= MEMORY_WARNING_THRESHOLD) {
            return MemoryPressureLevel.HIGH;
        } else if (memoryUsage >= 0.7f) {
            return MemoryPressureLevel.MODERATE;
        } else {
            return MemoryPressureLevel.LOW;
        }
    }
    
    /**
     * 处理内存压力
     */
    private void handleMemoryPressure(MemoryPressureLevel level, float memoryUsage) {
        boolean wasLowMemory = mIsLowMemoryMode;
        mIsLowMemoryMode = level.ordinal() >= MemoryPressureLevel.MODERATE.ordinal();
        
        // 通知监听器
        notifyMemoryPressureListeners(level);
        
        // 根据压力级别执行不同的清理策略
        switch (level) {
            case CRITICAL:
                performAggressiveCleanup();
                break;
            case HIGH:
                performModerateCleanup();
                break;
            case MODERATE:
                if (System.currentTimeMillis() - mLastCleanupTime > ROUTINE_CLEANUP_INTERVAL) {
                    performRoutineCleanup();
                }
                break;
            case LOW:
                if (wasLowMemory) {
                    notifyMemoryRecovered();
                }
                break;
        }
    }
    
    /**
     * 例行清理
     */
    public void performRoutineCleanup() {
        mExecutor.execute(() -> {
            try {
                long startMem = getUsedMemory();
                
                // 1. 清理弱引用对象
                cleanupWeakReferences();
                
                // 2. 清理缓存中的过期项
                cleanupExpiredCacheItems();
                
                // 3. 建议GC
                System.gc();
                
                long endMem = getUsedMemory();
                long freed = startMem - endMem;
                
                mTotalMemoryFreed += Math.max(0, freed);
                mCleanupCount++;
                mLastCleanupTime = System.currentTimeMillis();
                
                Log.d(TAG, "Routine cleanup completed. Freed: " + formatMemorySize(freed));
                
            } catch (Exception e) {
                Log.e(TAG, "Error in routine cleanup", e);
            }
        });
    }
    
    /**
     * 适度清理
     */
    public void performModerateCleanup() {
        mExecutor.execute(() -> {
            try {
                long startMem = getUsedMemory();
                
                // 执行例行清理
                cleanupWeakReferences();
                cleanupExpiredCacheItems();
                
                // 额外清理：压缩缓存
                compressCaches();
                
                // 强制GC
                System.gc();
                
                long endMem = getUsedMemory();
                long freed = startMem - endMem;
                
                mTotalMemoryFreed += Math.max(0, freed);
                mCleanupCount++;
                mLastCleanupTime = System.currentTimeMillis();
                
                Log.d(TAG, "Moderate cleanup completed. Freed: " + formatMemorySize(freed));
                
            } catch (Exception e) {
                Log.e(TAG, "Error in moderate cleanup", e);
            }
        });
    }
    
    /**
     * 激进清理
     */
    public void performAggressiveCleanup() {
        mExecutor.execute(() -> {
            try {
                long startMem = getUsedMemory();
                
                // 执行所有清理操作
                cleanupWeakReferences();
                cleanupExpiredCacheItems();
                compressCaches();
                
                // 清空非必要缓存
                clearNonEssentialCaches();
                
                // 回收Bitmap
                recycleBitmaps();
                
                // 多次GC
                for (int i = 0; i < 3; i++) {
                    System.gc();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                long endMem = getUsedMemory();
                long freed = startMem - endMem;
                
                mTotalMemoryFreed += Math.max(0, freed);
                mCleanupCount++;
                mLastCleanupTime = System.currentTimeMillis();
                
                Log.w(TAG, "Aggressive cleanup completed. Freed: " + formatMemorySize(freed));
                
            } catch (Exception e) {
                Log.e(TAG, "Error in aggressive cleanup", e);
            }
        });
    }
    
    /**
     * 清理弱引用对象
     */
    private void cleanupWeakReferences() {
        synchronized (mManagedObjects) {
            int initialSize = mManagedObjects.size();
            mManagedObjects.removeIf(ref -> ref.get() == null);
            int removed = initialSize - mManagedObjects.size();
            if (removed > 0) {
                Log.d(TAG, "Cleaned up " + removed + " weak references");
            }
        }
        
        synchronized (mListeners) {
            mListeners.removeIf(ref -> ref.get() == null);
        }
    }
    
    /**
     * 清理过期缓存项
     */
    private void cleanupExpiredCacheItems() {
        // 清理地址栏缓存
        AddressBarCache.getInstance(mContext).cleanExpiredCache();
        
        // TODO: 清理其他缓存
    }
    
    /**
     * 压缩缓存
     */
    private void compressCaches() {
        // 减小Bitmap缓存大小
        int currentSize = mBitmapCache.size();
        int targetSize = currentSize / 2;
        
        while (mBitmapCache.size() > targetSize) {
            // LruCache会自动移除最老的项
            mBitmapCache.trimToSize(targetSize);
        }
        
        Log.d(TAG, "Compressed bitmap cache from " + currentSize + " to " + mBitmapCache.size());
    }
    
    /**
     * 清空非必要缓存
     */
    private void clearNonEssentialCaches() {
        // 清空Bitmap缓存（除了最近使用的几个）
        mBitmapCache.trimToSize(mBitmapCache.maxSize() / 10);
        
        Log.d(TAG, "Cleared non-essential caches");
    }
    
    /**
     * 回收Bitmap
     */
    private void recycleBitmaps() {
        // 遍历缓存中的Bitmap并回收不再使用的
        for (Bitmap bitmap : mBitmapCache.snapshot().values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                // 注意：只有确定不再使用时才回收
                // bitmap.recycle();
            }
        }
    }
    
    /**
     * 添加内存压力监听器
     */
    public void addMemoryPressureListener(@NonNull MemoryPressureListener listener) {
        synchronized (mListeners) {
            mListeners.add(new WeakReference<>(listener));
        }
    }
    
    /**
     * 移除内存压力监听器
     */
    public void removeMemoryPressureListener(@NonNull MemoryPressureListener listener) {
        synchronized (mListeners) {
            mListeners.removeIf(ref -> ref.get() == listener);
        }
    }
    
    /**
     * 通知内存压力监听器
     */
    private void notifyMemoryPressureListeners(MemoryPressureLevel level) {
        synchronized (mListeners) {
            for (WeakReference<MemoryPressureListener> ref : mListeners) {
                MemoryPressureListener listener = ref.get();
                if (listener != null) {
                    try {
                        listener.onMemoryPressure(level);
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying memory pressure listener", e);
                    }
                }
            }
        }
    }
    
    /**
     * 通知内存恢复
     */
    private void notifyMemoryRecovered() {
        synchronized (mListeners) {
            for (WeakReference<MemoryPressureListener> ref : mListeners) {
                MemoryPressureListener listener = ref.get();
                if (listener != null) {
                    try {
                        listener.onMemoryRecovered();
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying memory recovery listener", e);
                    }
                }
            }
        }
    }
    
    /**
     * 注册对象进行内存管理
     */
    public void registerManagedObject(@NonNull Object object) {
        synchronized (mManagedObjects) {
            mManagedObjects.add(new WeakReference<>(object));
        }
    }
    
    /**
     * 获取Bitmap缓存
     */
    public LruCache<String, Bitmap> getBitmapCache() {
        return mBitmapCache;
    }
    
    /**
     * 是否处于低内存模式
     */
    public boolean isLowMemoryMode() {
        return mIsLowMemoryMode;
    }
    
    /**
     * 获取当前内存压力级别
     */
    public MemoryPressureLevel getCurrentMemoryPressure() {
        try {
            MemoryInfo memInfo = getCurrentMemoryInfo();
            float memoryUsage = 1.0f - (float) memInfo.availableMem / memInfo.totalMem;
            return determineMemoryPressureLevel(memoryUsage);
        } catch (Exception e) {
            Log.e(TAG, "Error getting current memory pressure", e);
            return MemoryPressureLevel.LOW;
        }
    }

    /**
     * 获取优化建议列表
     */
    public List<String> getOptimizationSuggestions() {
        List<String> suggestions = new ArrayList<>();
        MemoryPressureLevel currentLevel = getCurrentMemoryPressure();
        
        switch (currentLevel) {
            case CRITICAL:
                suggestions.add("立即关闭不必要的应用");
                suggestions.add("清理大型缓存文件");
                suggestions.add("重启应用释放内存");
                break;
            case HIGH:
                suggestions.add("清理图片缓存");
                suggestions.add("关闭后台标签页");
                suggestions.add("执行垃圾回收");
                break;
            case MODERATE:
                suggestions.add("清理临时文件");
                suggestions.add("压缩缓存数据");
                break;
            case LOW:
                suggestions.add("内存使用正常");
                break;
        }
        
        return suggestions;
    }

    /**
     * 手动触发内存优化
     */
    public void triggerOptimization() {
        mExecutor.execute(() -> {
            MemoryPressureLevel currentLevel = getCurrentMemoryPressure();
            Log.i(TAG, "Manual optimization triggered, current level: " + currentLevel);
            
            switch (currentLevel) {
                case CRITICAL:
                    performAggressiveCleanup();
                    break;
                case HIGH:
                    performModerateCleanup();
                    break;
                default:
                    performRoutineCleanup();
                    break;
            }
        });
    }

    /**
     * 获取内存使用统计
     */
    public MemoryStats getMemoryStats() {
        MemoryInfo memInfo = getCurrentMemoryInfo();
        return new MemoryStats(
            memInfo.getTotalPss() * 1024L, // PSS转换为字节
            mPeakMemoryUsage,
            mTotalMemoryFreed,
            mCleanupCount,
            mBitmapCache.size(),
            mBitmapCache.maxSize()
        );
    }
    
    /**
     * 内存统计信息
     */
    public static class MemoryStats {
        public final long currentMemoryUsage;
        public final long peakMemoryUsage;
        public final long totalMemoryFreed;
        public final int cleanupCount;
        public final int bitmapCacheSize;
        public final int bitmapCacheMaxSize;
        
        public MemoryStats(long currentMemoryUsage, long peakMemoryUsage, 
                          long totalMemoryFreed, int cleanupCount,
                          int bitmapCacheSize, int bitmapCacheMaxSize) {
            this.currentMemoryUsage = currentMemoryUsage;
            this.peakMemoryUsage = peakMemoryUsage;
            this.totalMemoryFreed = totalMemoryFreed;
            this.cleanupCount = cleanupCount;
            this.bitmapCacheSize = bitmapCacheSize;
            this.bitmapCacheMaxSize = bitmapCacheMaxSize;
        }
        
        public String getFormattedStats() {
            return String.format("当前内存: %s | 峰值: %s | 已释放: %s | 清理次数: %d | 缓存: %d/%d",
                formatMemorySize(currentMemoryUsage),
                formatMemorySize(peakMemoryUsage),
                formatMemorySize(totalMemoryFreed),
                cleanupCount,
                bitmapCacheSize,
                bitmapCacheMaxSize
            );
        }
    }
    
    /**
     * 销毁资源
     */
    public void destroy() {
        try {
            mScheduler.shutdown();
            mExecutor.shutdown();
            
            if (!mScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                mScheduler.shutdownNow();
            }
            if (!mExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                mExecutor.shutdownNow();
            }
            
            mListeners.clear();
            mManagedObjects.clear();
            
        } catch (Exception e) {
            Log.e(TAG, "Error destroying MemoryOptimizer", e);
        }
    }
    
    // 辅助方法
    private int calculateOptimalCacheSize() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        
        // 使用可用内存的1/8作为缓存大小
        return memoryClass * 1024 * 1024 / 8;
    }
    
    private long getUsedMemory() {
        MemoryInfo memInfo = getCurrentMemoryInfo();
        return memInfo.getTotalPss() * 1024L;
    }
    
    private static String formatMemorySize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}