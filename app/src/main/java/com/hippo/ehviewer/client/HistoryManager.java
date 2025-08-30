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
import com.hippo.ehviewer.client.data.HistoryInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史记录管理器
 */
public class HistoryManager {

    private static final String DATABASE_NAME = "history.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_HISTORY = "history";

    // 历史记录最大数量
    private static final int MAX_HISTORY_COUNT = 1000;

    private static HistoryManager sInstance;
    private final DatabaseHelper mDbHelper;

    private HistoryManager(Context context) {
        mDbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static synchronized HistoryManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new HistoryManager(context);
        }
        return sInstance;
    }

    /**
     * 添加或更新历史记录
     */
    public void addHistory(String title, String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // 检查是否已存在
        Cursor cursor = db.query(TABLE_HISTORY, new String[]{"id", "visit_count"},
                "url = ?", new String[]{url}, null, null, null);

        if (cursor.moveToFirst()) {
            // 更新现有记录
            long id = cursor.getLong(0);
            int visitCount = cursor.getInt(1) + 1;

            ContentValues values = new ContentValues();
            values.put("title", title);
            values.put("visit_time", System.currentTimeMillis());
            values.put("visit_count", visitCount);

            db.update(TABLE_HISTORY, values, "id = ?", new String[]{String.valueOf(id)});
        } else {
            // 插入新记录
            ContentValues values = new ContentValues();
            values.put("title", title);
            values.put("url", url);
            values.put("visit_time", System.currentTimeMillis());
            values.put("visit_count", 1);

            db.insert(TABLE_HISTORY, null, values);

            // 检查并清理超出数量的记录
            cleanOldHistory(db);
        }

        cursor.close();
    }

    /**
     * 删除历史记录
     */
    public boolean deleteHistory(long id) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        return db.delete(TABLE_HISTORY, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    /**
     * 清空所有历史记录
     */
    public void clearAllHistory() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
    }

    /**
     * 根据URL查找历史记录
     */
    @Nullable
    public HistoryInfo findHistoryByUrl(String url) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, "url = ?",
                new String[]{url}, null, null, null);

        HistoryInfo history = null;
        if (cursor.moveToFirst()) {
            history = cursorToHistory(cursor);
        }
        cursor.close();
        return history;
    }

    /**
     * 获取所有历史记录（按访问时间倒序）
     */
    @NonNull
    public List<HistoryInfo> getAllHistory() {
        List<HistoryInfo> historyList = new ArrayList<>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null,
                "visit_time DESC");

        while (cursor.moveToNext()) {
            historyList.add(cursorToHistory(cursor));
        }
        cursor.close();
        return historyList;
    }

    /**
     * 获取最近的历史记录
     */
    @NonNull
    public List<HistoryInfo> getRecentHistory(int limit) {
        List<HistoryInfo> historyList = new ArrayList<>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null,
                "visit_time DESC", String.valueOf(limit));

        while (cursor.moveToNext()) {
            historyList.add(cursorToHistory(cursor));
        }
        cursor.close();
        return historyList;
    }

    /**
     * 搜索历史记录
     */
    @NonNull
    public List<HistoryInfo> searchHistory(String query) {
        List<HistoryInfo> historyList = new ArrayList<>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String selection = "title LIKE ? OR url LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};

        Cursor cursor = db.query(TABLE_HISTORY, null, selection, selectionArgs,
                null, null, "visit_time DESC");

        while (cursor.moveToNext()) {
            historyList.add(cursorToHistory(cursor));
        }
        cursor.close();
        return historyList;
    }

    /**
     * 获取历史记录数量
     */
    public int getHistoryCount() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * 清理超出数量的历史记录
     */
    private void cleanOldHistory(SQLiteDatabase db) {
        // 保留最近的记录，删除超出数量的旧记录
        String deleteSql = "DELETE FROM " + TABLE_HISTORY + " WHERE id NOT IN (" +
                "SELECT id FROM " + TABLE_HISTORY + " ORDER BY visit_time DESC LIMIT " + MAX_HISTORY_COUNT + ")";
        db.execSQL(deleteSql);
    }

    /**
     * 将Cursor转换为HistoryInfo
     */
    private HistoryInfo cursorToHistory(Cursor cursor) {
        HistoryInfo history = new HistoryInfo();
        history.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        history.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        history.url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
        history.visitTime = cursor.getLong(cursor.getColumnIndexOrThrow("visit_time"));
        history.visitCount = cursor.getInt(cursor.getColumnIndexOrThrow("visit_count"));
        return history;
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
            String createTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "url TEXT UNIQUE," +
                    "visit_time INTEGER," +
                    "visit_count INTEGER DEFAULT 1" +
                    ")";
            db.execSQL(createTable);

            // 创建索引以提高查询性能
            db.execSQL("CREATE INDEX idx_visit_time ON " + TABLE_HISTORY + "(visit_time)");
            db.execSQL("CREATE INDEX idx_url ON " + TABLE_HISTORY + "(url)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 简单升级策略：删除旧表重建
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
            onCreate(db);
        }
    }
}
