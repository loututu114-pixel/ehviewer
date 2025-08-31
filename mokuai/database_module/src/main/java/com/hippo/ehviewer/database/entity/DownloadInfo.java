/*
 * EhViewer Database Module - DownloadInfo Entity
 * 下载信息实体类
 */

package com.hippo.ehviewer.database.entity;

import androidx.annotation.NonNull;

/**
 * 下载信息实体
 */
public class DownloadInfo {

    // 下载状态常量
    public static final int STATE_NONE = 0;
    public static final int STATE_WAIT = 1;
    public static final int STATE_DOWNLOAD = 2;
    public static final int STATE_FINISH = 3;
    public static final int STATE_FAILED = 4;

    private long id;
    private long gid;
    private String token;
    private String title;
    private int category;
    private String thumb;
    private String uploader;
    private float rating;
    private int state = STATE_NONE;
    private int legacy;
    private long time;

    public DownloadInfo() {
    }

    public DownloadInfo(long gid, String token, String title, int category,
                       String thumb, String uploader, float rating) {
        this.gid = gid;
        this.token = token;
        this.title = title;
        this.category = category;
        this.thumb = thumb;
        this.uploader = uploader;
        this.rating = rating;
        this.time = System.currentTimeMillis();
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getGid() { return gid; }
    public void setGid(long gid) { this.gid = gid; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getCategory() { return category; }
    public void setCategory(int category) { this.category = category; }

    public String getThumb() { return thumb; }
    public void setThumb(String thumb) { this.thumb = thumb; }

    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getState() { return state; }
    public void setState(int state) { this.state = state; }

    public int getLegacy() { return legacy; }
    public void setLegacy(int legacy) { this.legacy = legacy; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    /**
     * 获取状态描述
     */
    public String getStateText() {
        switch (state) {
            case STATE_NONE:
                return "未开始";
            case STATE_WAIT:
                return "等待中";
            case STATE_DOWNLOAD:
                return "下载中";
            case STATE_FINISH:
                return "已完成";
            case STATE_FAILED:
                return "失败";
            default:
                return "未知";
        }
    }

    /**
     * 是否已完成
     */
    public boolean isFinished() {
        return state == STATE_FINISH;
    }

    /**
     * 是否正在下载
     */
    public boolean isDownloading() {
        return state == STATE_DOWNLOAD;
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {
        return state == STATE_FAILED;
    }

    @NonNull
    @Override
    public String toString() {
        return "DownloadInfo{" +
                "gid=" + gid +
                ", title='" + title + '\'' +
                ", state=" + getStateText() +
                ", time=" + time +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DownloadInfo that = (DownloadInfo) obj;
        return gid == that.gid;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(gid).hashCode();
    }
}
