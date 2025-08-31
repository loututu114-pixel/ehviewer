/*
 * EhViewer Spider Module - SpiderQueen
 * 爬虫女王 - 网络爬虫的核心管理器
 */

package com.hippo.ehviewer.spider;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 爬虫女王
 * 管理所有的爬虫任务和资源
 */
public class SpiderQueen {

    private static final String TAG = SpiderQueen.class.getSimpleName();
    private static SpiderQueen sInstance;

    private final Context mContext;
    private final ExecutorService mExecutorService;
    private final SpiderDen mSpiderDen;

    /**
     * 获取单例实例
     */
    public static synchronized SpiderQueen getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SpiderQueen(context.getApplicationContext());
        }
        return sInstance;
    }

    private SpiderQueen(Context context) {
        mContext = context;
        mExecutorService = Executors.newFixedThreadPool(4);
        mSpiderDen = new SpiderDen();
    }

    /**
     * 创建新的爬虫任务
     */
    public SpiderInfo createSpider(String url) {
        SpiderInfo spiderInfo = new SpiderInfo();
        spiderInfo.setUrl(url);
        spiderInfo.setStatus(SpiderInfo.STATUS_PENDING);

        Log.d(TAG, "Created spider for URL: " + url);
        return spiderInfo;
    }

    /**
     * 启动爬虫任务
     */
    public void startSpider(SpiderInfo spiderInfo) {
        if (spiderInfo == null) {
            return;
        }

        spiderInfo.setStatus(SpiderInfo.STATUS_RUNNING);
        SpiderWorker worker = new SpiderWorker(spiderInfo);
        mExecutorService.execute(worker);

        Log.d(TAG, "Started spider: " + spiderInfo.getUrl());
    }

    /**
     * 停止爬虫任务
     */
    public void stopSpider(SpiderInfo spiderInfo) {
        if (spiderInfo != null) {
            spiderInfo.setStatus(SpiderInfo.STATUS_STOPPED);
            Log.d(TAG, "Stopped spider: " + spiderInfo.getUrl());
        }
    }

    /**
     * 获取爬虫巢穴
     */
    public SpiderDen getSpiderDen() {
        return mSpiderDen;
    }

    /**
     * 关闭爬虫女王
     */
    public void shutdown() {
        mExecutorService.shutdown();
        Log.d(TAG, "SpiderQueen shutdown");
    }
}
