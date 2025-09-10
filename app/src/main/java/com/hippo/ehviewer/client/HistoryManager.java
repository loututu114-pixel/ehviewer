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
import android.util.Log;
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

    // 历史记录最大数量 - 增加到300条，避免记录被过度清理
    private static final int MAX_HISTORY_COUNT = 300;

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

        try {
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

                int updateResult = db.update(TABLE_HISTORY, values, "id = ?", new String[]{String.valueOf(id)});
                Log.d("HistoryManager", "Updated existing history record: " + url + ", result: " + updateResult);
            } else {
                // 插入新记录
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("url", url);
                values.put("visit_time", System.currentTimeMillis());
                values.put("visit_count", 1);

                long insertResult = db.insert(TABLE_HISTORY, null, values);
                Log.d("HistoryManager", "Inserted new history record: " + url + ", result: " + insertResult);

                // 检查并清理超出数量的记录
                cleanOldHistory(db);
            }

            cursor.close();
        } catch (Exception e) {
            Log.e("HistoryManager", "Error adding history record", e);
        } finally {
            // 确保数据库连接被关闭
            if (db != null && db.isOpen()) {
                // 注意：这里不应该关闭数据库连接，因为它是单例模式
                // db.close();
            }
        }
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
        Log.d("HistoryManager", "All history records cleared");
    }

    /**
     * 获取历史记录限制数量
     */
    public int getMaxHistoryCount() {
        return MAX_HISTORY_COUNT;
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
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = mDbHelper.getReadableDatabase();
            cursor = db.query(TABLE_HISTORY, null, null, null, null, null,
                    "visit_time DESC");

            Log.d("HistoryManager", "Querying all history records, cursor count: " + cursor.getCount());

            while (cursor.moveToNext()) {
                try {
                    HistoryInfo history = cursorToHistory(cursor);
                    historyList.add(history);
                    Log.d("HistoryManager", "Loaded history: " + history.title + " - " + history.url);
                } catch (Exception e) {
                    Log.e("HistoryManager", "Error parsing history record", e);
                }
            }

            Log.d("HistoryManager", "Total history records loaded: " + historyList.size());

        } catch (Exception e) {
            Log.e("HistoryManager", "Error getting all history records", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

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
        return searchHistory(query, -1); // 无限制
    }
    
    /**
     * 搜索历史记录（带数量限制）
     */
    @NonNull
    public List<HistoryInfo> searchHistory(String query, int limit) {
        List<HistoryInfo> historyList = new ArrayList<>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String selection = "title LIKE ? OR url LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        
        String limitClause = limit > 0 ? String.valueOf(limit) : null;

        Cursor cursor = db.query(TABLE_HISTORY, null, selection, selectionArgs,
                null, null, "visit_time DESC", limitClause);

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
     * 只保留最近的30条记录，自动删除更早的记录
     */
    private void cleanOldHistory(SQLiteDatabase db) {
        try {
            // 检查当前记录数量
            Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY, null);
            int currentCount = 0;
            if (countCursor.moveToFirst()) {
                currentCount = countCursor.getInt(0);
            }
            countCursor.close();

            if (currentCount > MAX_HISTORY_COUNT) {
                // 保留最近的记录，删除超出数量的旧记录
                String deleteSql = "DELETE FROM " + TABLE_HISTORY + " WHERE id NOT IN (" +
                        "SELECT id FROM " + TABLE_HISTORY + " ORDER BY visit_time DESC LIMIT " + MAX_HISTORY_COUNT + ")";
                db.execSQL(deleteSql);

                Log.d("HistoryManager", "Cleaned old history records: removed " + (currentCount - MAX_HISTORY_COUNT) +
                      " records, kept " + MAX_HISTORY_COUNT + " recent records");
            }
        } catch (Exception e) {
            Log.e("HistoryManager", "Error cleaning old history records", e);
        }
    }

    /**
     * 将Cursor转换为HistoryInfo
     */
    private HistoryInfo cursorToHistory(Cursor cursor) {
        HistoryInfo history = new HistoryInfo();

        try {
            history.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            history.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            history.url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
            history.visitTime = cursor.getLong(cursor.getColumnIndexOrThrow("visit_time"));
            history.visitCount = cursor.getInt(cursor.getColumnIndexOrThrow("visit_count"));

            // 确保URL不为空
            if (history.url == null || history.url.trim().isEmpty()) {
                Log.w("HistoryManager", "Found history record with empty URL, id: " + history.id);
                history.url = "about:blank";
            }

            // 确保标题不为空
            if (history.title == null || history.title.trim().isEmpty()) {
                history.title = history.url;
            }

        } catch (Exception e) {
            Log.e("HistoryManager", "Error converting cursor to HistoryInfo", e);
            // 返回一个默认的HistoryInfo对象
            history.url = "about:blank";
            history.title = "未知页面";
        }

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
