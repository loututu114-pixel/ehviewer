/*
 * EhViewer Download Module
 * 下载管理模块 - 提供文件下载、多线程下载、断点续传等功能
 *
 * 主要功能：
 * - 多线程文件下载
 * - 断点续传
 * - 下载队列管理
 * - 下载状态监听
 * - 网络状态检测
 * - 存储空间管理
 * - 下载速度限制
 */

package com.hippo.ehviewer.download;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下载管理器
 * 负责管理所有的下载任务
 */
public class DownloadManager {

    private static final String TAG = DownloadManager.class.getSimpleName();
    private static final int DEFAULT_MAX_CONCURRENT_DOWNLOADS = 3;

    private static DownloadManager sInstance;

    private final Context mContext;
    private final ExecutorService mExecutorService;
    private final PriorityBlockingQueue<DownloadTask> mDownloadQueue;
    private final ConcurrentHashMap<String, DownloadTask> mActiveDownloads;
    private final AtomicInteger mActiveDownloadCount;

    private int mMaxConcurrentDownloads = DEFAULT_MAX_CONCURRENT_DOWNLOADS;
    private DownloadListener mGlobalListener;

    /**
     * 下载监听器接口
     */
    public interface DownloadListener {
        void onDownloadStart(DownloadTask task);
        void onDownloadProgress(DownloadTask task);
        void onDownloadComplete(DownloadTask task);
        void onDownloadError(DownloadTask task, Exception e);
        void onDownloadPause(DownloadTask task);
        void onDownloadResume(DownloadTask task);
        void onDownloadCancel(DownloadTask task);
    }

    /**
     * 获取单例实例
     */
    public static synchronized DownloadManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DownloadManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private DownloadManager(Context context) {
        mContext = context;
        mExecutorService = Executors.newFixedThreadPool(mMaxConcurrentDownloads);
        mDownloadQueue = new PriorityBlockingQueue<>();
        mActiveDownloads = new ConcurrentHashMap<>();
        mActiveDownloadCount = new AtomicInteger(0);

        startDownloadProcessor();
    }

    /**
     * 设置全局下载监听器
     */
    public void setGlobalListener(DownloadListener listener) {
        mGlobalListener = listener;
    }

    /**
     * 设置最大并发下载数
     */
    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        mMaxConcurrentDownloads = maxConcurrentDownloads;
    }

    /**
     * 添加下载任务
     */
    public void addDownloadTask(DownloadTask task) {
        if (task == null) {
            return;
        }

        task.setDownloadManager(this);
        mDownloadQueue.offer(task);

        Log.d(TAG, "Download task added: " + task.getUrl());
    }

    /**
     * 暂停下载任务
     */
    public void pauseDownloadTask(String url) {
        DownloadTask task = mActiveDownloads.get(url);
        if (task != null) {
            task.pause();
        }
    }

    /**
     * 恢复下载任务
     */
    public void resumeDownloadTask(String url) {
        DownloadTask task = mActiveDownloads.get(url);
        if (task != null) {
            task.resume();
        }
    }

    /**
     * 取消下载任务
     */
    public void cancelDownloadTask(String url) {
        DownloadTask task = mActiveDownloads.remove(url);
        if (task != null) {
            task.cancel();
        }

        // 从队列中移除
        mDownloadQueue.removeIf(t -> t.getUrl().equals(url));
    }

    /**
     * 取消所有下载任务
     */
    public void cancelAllDownloadTasks() {
        // 取消活跃任务
        for (DownloadTask task : mActiveDownloads.values()) {
            task.cancel();
        }
        mActiveDownloads.clear();

        // 清空队列
        mDownloadQueue.clear();

        Log.d(TAG, "All download tasks cancelled");
    }

    /**
     * 获取下载任务
     */
    public DownloadTask getDownloadTask(String url) {
        DownloadTask task = mActiveDownloads.get(url);
        if (task != null) {
            return task;
        }

        // 从队列中查找
        for (DownloadTask queuedTask : mDownloadQueue) {
            if (queuedTask.getUrl().equals(url)) {
                return queuedTask;
            }
        }

        return null;
    }

    /**
     * 获取所有下载任务
     */
    public java.util.List<DownloadTask> getAllDownloadTasks() {
        java.util.List<DownloadTask> allTasks = new java.util.ArrayList<>();
        allTasks.addAll(mActiveDownloads.values());
        allTasks.addAll(mDownloadQueue);
        return allTasks;
    }

    /**
     * 获取活跃下载数
     */
    public int getActiveDownloadCount() {
        return mActiveDownloadCount.get();
    }

    /**
     * 获取队列中的任务数
     */
    public int getQueuedDownloadCount() {
        return mDownloadQueue.size();
    }

    /**
     * 通知下载开始
     */
    void notifyDownloadStart(DownloadTask task) {
        if (mGlobalListener != null) {
            mGlobalListener.onDownloadStart(task);
        }
    }

    /**
     * 通知下载进度
     */
    void notifyDownloadProgress(DownloadTask task) {
        if (mGlobalListener != null) {
            mGlobalListener.onDownloadProgress(task);
        }
    }

    /**
     * 通知下载完成
     */
    void notifyDownloadComplete(DownloadTask task) {
        mActiveDownloads.remove(task.getUrl());
        mActiveDownloadCount.decrementAndGet();

        if (mGlobalListener != null) {
            mGlobalListener.onDownloadComplete(task);
        }

        // 处理下一个队列任务
        processNextQueuedTask();
    }

    /**
     * 通知下载错误
     */
    void notifyDownloadError(DownloadTask task, Exception e) {
        mActiveDownloads.remove(task.getUrl());
        mActiveDownloadCount.decrementAndGet();

        if (mGlobalListener != null) {
            mGlobalListener.onDownloadError(task, e);
        }

        // 处理下一个队列任务
        processNextQueuedTask();
    }

    /**
     * 通知下载暂停
     */
    void notifyDownloadPause(DownloadTask task) {
        if (mGlobalListener != null) {
            mGlobalListener.onDownloadPause(task);
        }
    }

    /**
     * 通知下载恢复
     */
    void notifyDownloadResume(DownloadTask task) {
        if (mGlobalListener != null) {
            mGlobalListener.onDownloadResume(task);
        }
    }

    /**
     * 通知下载取消
     */
    void notifyDownloadCancel(DownloadTask task) {
        mActiveDownloads.remove(task.getUrl());
        mActiveDownloadCount.decrementAndGet();

        if (mGlobalListener != null) {
            mGlobalListener.onDownloadCancel(task);
        }

        // 处理下一个队列任务
        processNextQueuedTask();
    }

    /**
     * 启动下载处理器
     */
    private void startDownloadProcessor() {
        Thread processorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DownloadTask task = mDownloadQueue.take();
                    if (task != null && mActiveDownloadCount.get() < mMaxConcurrentDownloads) {
                        mActiveDownloads.put(task.getUrl(), task);
                        mActiveDownloadCount.incrementAndGet();
                        mExecutorService.execute(task);
                    } else if (task != null) {
                        // 重新放回队列
                        mDownloadQueue.offer(task);
                        Thread.sleep(1000); // 等待1秒后重试
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error processing download queue", e);
                }
            }
        });

        processorThread.setName("DownloadProcessor");
        processorThread.setDaemon(true);
        processorThread.start();
    }

    /**
     * 处理下一个队列任务
     */
    private void processNextQueuedTask() {
        if (!mDownloadQueue.isEmpty() && mActiveDownloadCount.get() < mMaxConcurrentDownloads) {
            try {
                DownloadTask task = mDownloadQueue.poll();
                if (task != null) {
                    mActiveDownloads.put(task.getUrl(), task);
                    mActiveDownloadCount.incrementAndGet();
                    mExecutorService.execute(task);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing next queued task", e);
            }
        }
    }

    /**
     * 关闭下载管理器
     */
    public void shutdown() {
        mExecutorService.shutdown();
        cancelAllDownloadTasks();
        Log.d(TAG, "DownloadManager shutdown");
    }
}
