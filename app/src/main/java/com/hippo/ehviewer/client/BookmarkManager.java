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

package com.hippo.ehviewer.client;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.client.data.BookmarkInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 书签管理器
 */
public class BookmarkManager {

    private static final String DATABASE_NAME = "bookmarks.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_BOOKMARKS = "bookmarks";

    private static BookmarkManager sInstance;
    private final DatabaseHelper mDbHelper;

    private BookmarkManager(Context context) {
        mDbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static synchronized BookmarkManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BookmarkManager(context);
        }
        return sInstance;
    }

    /**
     * 添加书签
     */
    public long addBookmark(String title, String url) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("url", url);
        values.put("create_time", System.currentTimeMillis());
        values.put("last_visit_time", System.currentTimeMillis());
        values.put("visit_count", 0);

        return db.insert(TABLE_BOOKMARKS, null, values);
    }

    /**
     * 添加书签（使用BookmarkInfo对象）
     */
    public long addBookmark(BookmarkInfo bookmark) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", bookmark.title);
        values.put("url", bookmark.url);
        values.put("favicon_url", bookmark.faviconUrl);
        values.put("create_time", bookmark.createTime);
        values.put("last_visit_time", bookmark.lastVisitTime);
        values.put("visit_count", bookmark.visitCount);

        long id = db.insert(TABLE_BOOKMARKS, null, values);
        bookmark.id = id;
        return id;
    }

    /**
     * 删除书签
     */
    public boolean deleteBookmark(long id) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        return db.delete(TABLE_BOOKMARKS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    /**
     * 更新书签
     */
    public boolean updateBookmark(BookmarkInfo bookmark) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", bookmark.title);
        values.put("url", bookmark.url);
        values.put("favicon_url", bookmark.faviconUrl);
        values.put("last_visit_time", bookmark.lastVisitTime);
        values.put("visit_count", bookmark.visitCount);

        return db.update(TABLE_BOOKMARKS, values, "id = ?",
                new String[]{String.valueOf(bookmark.id)}) > 0;
    }

    /**
     * 根据URL查找书签
     */
    @Nullable
    public BookmarkInfo findBookmarkByUrl(String url) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKMARKS, null, "url = ?",
                new String[]{url}, null, null, null);

        BookmarkInfo bookmark = null;
        if (cursor.moveToFirst()) {
            bookmark = cursorToBookmark(cursor);
        }
        cursor.close();
        return bookmark;
    }

    /**
     * 获取所有书签
     */
    @NonNull
    public List<BookmarkInfo> getAllBookmarks() {
        List<BookmarkInfo> bookmarks = new ArrayList<>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKMARKS, null, null, null, null, null,
                "last_visit_time DESC");

        while (cursor.moveToNext()) {
            bookmarks.add(cursorToBookmark(cursor));
        }
        cursor.close();
        return bookmarks;
    }

    /**
     * 记录访问
     */
    public void recordVisit(String url) {
        BookmarkInfo bookmark = findBookmarkByUrl(url);
        if (bookmark != null) {
            bookmark.lastVisitTime = System.currentTimeMillis();
            bookmark.visitCount++;
            updateBookmark(bookmark);
        }
    }

    /**
     * 检查URL是否已收藏
     */
    public boolean isBookmarked(String url) {
        return findBookmarkByUrl(url) != null;
    }

    /**
     * 获取书签数量
     */
    public int getBookmarkCount() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BOOKMARKS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * 将Cursor转换为BookmarkInfo
     */
    private BookmarkInfo cursorToBookmark(Cursor cursor) {
        BookmarkInfo bookmark = new BookmarkInfo();
        bookmark.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        bookmark.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        bookmark.url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
        bookmark.faviconUrl = cursor.getString(cursor.getColumnIndexOrThrow("favicon_url"));
        bookmark.createTime = cursor.getLong(cursor.getColumnIndexOrThrow("create_time"));
        bookmark.lastVisitTime = cursor.getLong(cursor.getColumnIndexOrThrow("last_visit_time"));
        bookmark.visitCount = cursor.getInt(cursor.getColumnIndexOrThrow("visit_count"));
        return bookmark;
    }

    /**
     * 数据库帮助类
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_BOOKMARKS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "url TEXT UNIQUE," +
                    "favicon_url TEXT," +
                    "create_time INTEGER," +
                    "last_visit_time INTEGER," +
                    "visit_count INTEGER DEFAULT 0" +
                    ")";
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 简单升级策略：删除旧表重建
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
            onCreate(db);
        }
    }
}
