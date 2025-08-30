/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client.data;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

/**
 * 历史记录信息类
 */
public class HistoryInfo implements Parcelable {

    public long id;
    public String title;
    public String url;
    public long visitTime;
    public int visitCount;

    public HistoryInfo() {
        this.visitTime = System.currentTimeMillis();
        this.visitCount = 1;
    }

    public HistoryInfo(String title, String url) {
        this();
        this.title = title;
        this.url = url;
    }

    protected HistoryInfo(Parcel in) {
        id = in.readLong();
        title = in.readString();
        url = in.readString();
        visitTime = in.readLong();
        visitCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(url);
        dest.writeLong(visitTime);
        dest.writeInt(visitCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HistoryInfo> CREATOR = new Creator<HistoryInfo>() {
        @Override
        public HistoryInfo createFromParcel(Parcel in) {
            return new HistoryInfo(in);
        }

        @Override
        public HistoryInfo[] newArray(int size) {
            return new HistoryInfo[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "HistoryInfo{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", visitTime=" + visitTime +
                ", visitCount=" + visitCount +
                '}';
    }

    /**
     * 获取显示标题
     */
    public String getDisplayTitle() {
        return title != null && !title.trim().isEmpty() ? title : url;
    }

    /**
     * 获取域名
     */
    public String getDomain() {
        if (url == null) return "";
        try {
            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * 获取相对时间描述
     */
    public String getRelativeTime() {
        long now = System.currentTimeMillis();
        long diff = now - visitTime;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return "刚刚";
        }
    }
}
