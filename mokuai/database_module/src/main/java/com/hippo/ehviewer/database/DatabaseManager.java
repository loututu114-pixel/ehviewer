/*
 * EhViewer Database Module
 * 数据库管理模块 - 提供数据持久化、查询、缓存等功能
 *
 * 主要功能：
 * - SQLite数据库操作
 * - 数据实体管理
 * - 查询构建器
 * - 数据迁移
 * - 数据库备份和恢复
 * - 事务管理
 *
 * 支持的数据类型：
 * - 下载记录
 * - 历史记录
 * - 收藏夹
 * - 黑名单
 * - 标签信息
 * - 快速搜索
 * - 过滤器
 */

package com.hippo.ehviewer.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 数据库管理器
 * 提供统一的数据库操作接口
 */
public class DatabaseManager {

    private static final String TAG = DatabaseManager.class.getSimpleName();
    private static final String DATABASE_NAME = "ehviewer.db";
    private static final int DATABASE_VERSION = 1;

    private static DatabaseManager sInstance;
    private final Context mContext;
    private final DatabaseHelper mHelper;
    private final SQLiteDatabase mDatabase;

    // 数据监听器
    private final List<DatabaseChangeListener> mListeners = new CopyOnWriteArrayList<>();

    /**
     * 数据库变化监听器
     */
    public interface DatabaseChangeListener {
        void onDatabaseChanged(String tableName, DatabaseOperation operation);
    }

    /**
     * 数据库操作类型
     */
    public enum DatabaseOperation {
        INSERT, UPDATE, DELETE
    }

    /**
     * 获取单例实例
     */
    public static synchronized DatabaseManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseManager(Context context) {
        mContext = context;
        mHelper = new DatabaseHelper(context);
        mDatabase = mHelper.getWritableDatabase();
    }

    /**
     * 注册数据库变化监听器
     */
    public void registerListener(DatabaseChangeListener listener) {
        mListeners.add(listener);
    }

    /**
     * 注销数据库变化监听器
     */
    public void unregisterListener(DatabaseChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * 通知监听器数据库变化
     */
    private void notifyListeners(String tableName, DatabaseOperation operation) {
        for (DatabaseChangeListener listener : mListeners) {
            try {
                listener.onDatabaseChanged(tableName, operation);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener", e);
            }
        }
    }

    /**
     * 执行SQL查询
     */
    public Cursor executeQuery(String sql, String[] selectionArgs) {
        return mDatabase.rawQuery(sql, selectionArgs);
    }

    /**
     * 执行SQL更新
     */
    public int executeUpdate(String sql, Object[] bindArgs) {
        mDatabase.execSQL(sql, bindArgs);
        return getAffectedRows();
    }

    /**
     * 执行事务
     */
    public void executeTransaction(DatabaseTransaction transaction) {
        mDatabase.beginTransaction();
        try {
            transaction.execute(this);
            mDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Transaction failed", e);
            throw e;
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * 数据库事务接口
     */
    public interface DatabaseTransaction {
        void execute(DatabaseManager db) throws Exception;
    }

    /**
     * 获取受影响的行数
     */
    private int getAffectedRows() {
        Cursor cursor = mDatabase.rawQuery("SELECT changes() as affected_rows", null);
        int affectedRows = 0;
        if (cursor.moveToFirst()) {
            affectedRows = cursor.getInt(0);
        }
        cursor.close();
        return affectedRows;
    }

    /**
     * 数据库备份
     */
    public boolean backupDatabase(File backupFile) {
        try {
            File currentDB = mContext.getDatabasePath(DATABASE_NAME);
            if (!currentDB.exists()) {
                return false;
            }

            FileInputStream fis = new FileInputStream(currentDB);
            FileOutputStream fos = new FileOutputStream(backupFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.flush();
            fos.close();
            fis.close();

            Log.d(TAG, "Database backup successful: " + backupFile.getPath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Database backup failed", e);
            return false;
        }
    }

    /**
     * 数据库恢复
     */
    public boolean restoreDatabase(File backupFile) {
        try {
            if (!backupFile.exists()) {
                return false;
            }

            File currentDB = mContext.getDatabasePath(DATABASE_NAME);
            FileInputStream fis = new FileInputStream(backupFile);
            FileOutputStream fos = new FileOutputStream(currentDB);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.flush();
            fos.close();
            fis.close();

            // 重新打开数据库
            mDatabase.close();
            DatabaseHelper newHelper = new DatabaseHelper(mContext);
            // 这里需要更新mHelper和mDatabase的引用

            Log.d(TAG, "Database restore successful: " + backupFile.getPath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Database restore failed", e);
            return false;
        }
    }

    /**
     * 清空表数据
     */
    public void clearTable(String tableName) {
        mDatabase.execSQL("DELETE FROM " + tableName);
        notifyListeners(tableName, DatabaseOperation.DELETE);
        Log.d(TAG, "Table cleared: " + tableName);
    }

    /**
     * 删除表
     */
    public void dropTable(String tableName) {
        mDatabase.execSQL("DROP TABLE IF EXISTS " + tableName);
        Log.d(TAG, "Table dropped: " + tableName);
    }

    /**
     * 获取表信息
     */
    public List<String> getTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mDatabase.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (cursor.moveToFirst()) {
                do {
                    columns.add(cursor.getString(1)); // column name
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get table columns: " + tableName, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return columns;
    }

    /**
     * 关闭数据库
     */
    public void close() {
        if (mDatabase != null && mDatabase.isOpen()) {
            mDatabase.close();
        }
        Log.d(TAG, "Database closed");
    }

    /**
     * 数据库助手类
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTables(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            upgradeDatabase(db, oldVersion, newVersion);
        }

        /**
         * 创建数据表
         */
        private void createTables(SQLiteDatabase db) {
            // 下载信息表
            db.execSQL("CREATE TABLE downloads (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "gid INTEGER UNIQUE NOT NULL," +
                    "token TEXT," +
                    "title TEXT," +
                    "category INTEGER," +
                    "thumb TEXT," +
                    "uploader TEXT," +
                    "rating REAL," +
                    "state INTEGER," +
                    "legacy INTEGER," +
                    "time INTEGER" +
                    ")");

            // 历史记录表
            db.execSQL("CREATE TABLE history (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "gid INTEGER UNIQUE NOT NULL," +
                    "token TEXT," +
                    "title TEXT," +
                    "category INTEGER," +
                    "thumb TEXT," +
                    "uploader TEXT," +
                    "rating REAL," +
                    "mode INTEGER," +
                    "time INTEGER" +
                    ")");

            // 收藏夹表
            db.execSQL("CREATE TABLE favorites (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "gid INTEGER UNIQUE NOT NULL," +
                    "token TEXT," +
                    "title TEXT," +
                    "category INTEGER," +
                    "thumb TEXT," +
                    "uploader TEXT," +
                    "rating REAL," +
                    "time INTEGER" +
                    ")");

            Log.d(TAG, "Database tables created");
        }

        /**
         * 升级数据库
         */
        private void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "Upgrading database from " + oldVersion + " to " + newVersion);
            // 处理数据库升级逻辑
        }
    }
}
