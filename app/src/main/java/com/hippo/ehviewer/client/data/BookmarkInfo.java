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

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 书签信息类
 */
public class BookmarkInfo implements Parcelable {

    public long id;
    public String title;
    public String url;
    public String faviconUrl;
    public long createTime;
    public long lastVisitTime;
    public int visitCount;

    public BookmarkInfo() {
        this.createTime = System.currentTimeMillis();
        this.lastVisitTime = this.createTime;
        this.visitCount = 0;
    }

    public BookmarkInfo(String title, String url) {
        this();
        this.title = title;
        this.url = url;
    }

    protected BookmarkInfo(Parcel in) {
        id = in.readLong();
        title = in.readString();
        url = in.readString();
        faviconUrl = in.readString();
        createTime = in.readLong();
        lastVisitTime = in.readLong();
        visitCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(url);
        dest.writeString(faviconUrl);
        dest.writeLong(createTime);
        dest.writeLong(lastVisitTime);
        dest.writeInt(visitCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BookmarkInfo> CREATOR = new Creator<BookmarkInfo>() {
        @Override
        public BookmarkInfo createFromParcel(Parcel in) {
            return new BookmarkInfo(in);
        }

        @Override
        public BookmarkInfo[] newArray(int size) {
            return new BookmarkInfo[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "BookmarkInfo{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", createTime=" + createTime +
                ", lastVisitTime=" + lastVisitTime +
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
}
