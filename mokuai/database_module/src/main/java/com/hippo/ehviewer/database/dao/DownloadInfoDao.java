/*
 * EhViewer Database Module - DownloadInfoDao
 * 下载信息数据访问对象
 */

package com.hippo.ehviewer.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hippo.ehviewer.database.DatabaseManager;
import com.hippo.ehviewer.database.entity.DownloadInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载信息数据访问对象
 */
public class DownloadInfoDao {

    private static final String TAG = DownloadInfoDao.class.getSimpleName();
    private static final String TABLE_NAME = "downloads";

    private final DatabaseManager mDatabaseManager;

    public DownloadInfoDao(DatabaseManager databaseManager) {
        mDatabaseManager = databaseManager;
    }

    /**
     * 插入下载信息
     */
    public long insert(DownloadInfo downloadInfo) {
        ContentValues values = new ContentValues();
        values.put("gid", downloadInfo.getGid());
        values.put("token", downloadInfo.getToken());
        values.put("title", downloadInfo.getTitle());
        values.put("category", downloadInfo.getCategory());
        values.put("thumb", downloadInfo.getThumb());
        values.put("uploader", downloadInfo.getUploader());
        values.put("rating", downloadInfo.getRating());
        values.put("state", downloadInfo.getState());
        values.put("legacy", downloadInfo.getLegacy());
        values.put("time", downloadInfo.getTime());

        long id = mDatabaseManager.executeUpdate(
            "INSERT OR REPLACE INTO " + TABLE_NAME + " " +
            "(gid, token, title, category, thumb, uploader, rating, state, legacy, time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            new Object[]{
                downloadInfo.getGid(),
                downloadInfo.getToken(),
                downloadInfo.getTitle(),
                downloadInfo.getCategory(),
                downloadInfo.getThumb(),
                downloadInfo.getUploader(),
                downloadInfo.getRating(),
                downloadInfo.getState(),
                downloadInfo.getLegacy(),
                downloadInfo.getTime()
            }
        );

        if (id > 0) {
            downloadInfo.setId(id);
            mDatabaseManager.notifyListeners(TABLE_NAME, DatabaseManager.DatabaseOperation.INSERT);
            Log.d(TAG, "DownloadInfo inserted: " + downloadInfo.getTitle());
        }

        return id;
    }

    /**
     * 更新下载信息
     */
    public int update(DownloadInfo downloadInfo) {
        int result = mDatabaseManager.executeUpdate(
            "UPDATE " + TABLE_NAME + " SET " +
            "token=?, title=?, category=?, thumb=?, uploader=?, rating=?, state=?, legacy=?, time=? " +
            "WHERE gid=?",
            new Object[]{
                downloadInfo.getToken(),
                downloadInfo.getTitle(),
                downloadInfo.getCategory(),
                downloadInfo.getThumb(),
                downloadInfo.getUploader(),
                downloadInfo.getRating(),
                downloadInfo.getState(),
                downloadInfo.getLegacy(),
                downloadInfo.getTime(),
                downloadInfo.getGid()
            }
        );

        if (result > 0) {
            mDatabaseManager.notifyListeners(TABLE_NAME, DatabaseManager.DatabaseOperation.UPDATE);
            Log.d(TAG, "DownloadInfo updated: " + downloadInfo.getTitle());
        }

        return result;
    }

    /**
     * 根据GID查询下载信息
     */
    public DownloadInfo queryByGid(long gid) {
        Cursor cursor = mDatabaseManager.executeQuery(
            "SELECT * FROM " + TABLE_NAME + " WHERE gid=?",
            new String[]{String.valueOf(gid)}
        );

        DownloadInfo downloadInfo = null;
        if (cursor != null && cursor.moveToFirst()) {
            downloadInfo = cursorToDownloadInfo(cursor);
            cursor.close();
        }

        return downloadInfo;
    }

    /**
     * 查询所有下载信息
     */
    public List<DownloadInfo> queryAll() {
        return queryAll("time DESC");
    }

    /**
     * 查询所有下载信息（指定排序）
     */
    public List<DownloadInfo> queryAll(String orderBy) {
        Cursor cursor = mDatabaseManager.executeQuery(
            "SELECT * FROM " + TABLE_NAME + " ORDER BY " + orderBy,
            null
        );

        List<DownloadInfo> downloadInfos = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadInfo downloadInfo = cursorToDownloadInfo(cursor);
                if (downloadInfo != null) {
                    downloadInfos.add(downloadInfo);
                }
            }
            cursor.close();
        }

        return downloadInfos;
    }

    /**
     * 根据状态查询下载信息
     */
    public List<DownloadInfo> queryByState(int state) {
        Cursor cursor = mDatabaseManager.executeQuery(
            "SELECT * FROM " + TABLE_NAME + " WHERE state=? ORDER BY time DESC",
            new String[]{String.valueOf(state)}
        );

        List<DownloadInfo> downloadInfos = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadInfo downloadInfo = cursorToDownloadInfo(cursor);
                if (downloadInfo != null) {
                    downloadInfos.add(downloadInfo);
                }
            }
            cursor.close();
        }

        return downloadInfos;
    }

    /**
     * 根据标题搜索下载信息
     */
    public List<DownloadInfo> searchByTitle(String keyword) {
        Cursor cursor = mDatabaseManager.executeQuery(
            "SELECT * FROM " + TABLE_NAME + " WHERE title LIKE ? ORDER BY time DESC",
            new String[]{"%" + keyword + "%"}
        );

        List<DownloadInfo> downloadInfos = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadInfo downloadInfo = cursorToDownloadInfo(cursor);
                if (downloadInfo != null) {
                    downloadInfos.add(downloadInfo);
                }
            }
            cursor.close();
        }

        return downloadInfos;
    }

    /**
     * 删除下载信息
     */
    public int delete(long gid) {
        int result = mDatabaseManager.executeUpdate(
            "DELETE FROM " + TABLE_NAME + " WHERE gid=?",
            new Object[]{gid}
        );

        if (result > 0) {
            mDatabaseManager.notifyListeners(TABLE_NAME, DatabaseManager.DatabaseOperation.DELETE);
            Log.d(TAG, "DownloadInfo deleted: " + gid);
        }

        return result;
    }

    /**
     * 删除所有下载信息
     */
    public int deleteAll() {
        int result = mDatabaseManager.executeUpdate(
            "DELETE FROM " + TABLE_NAME,
            null
        );

        if (result > 0) {
            mDatabaseManager.notifyListeners(TABLE_NAME, DatabaseManager.DatabaseOperation.DELETE);
            Log.d(TAG, "All DownloadInfo deleted");
        }

        return result;
    }

    /**
     * 获取下载信息数量
     */
    public int getCount() {
        Cursor cursor = mDatabaseManager.executeQuery(
            "SELECT COUNT(*) FROM " + TABLE_NAME,
            null
        );

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }

        return count;
    }

    /**
     * 检查是否存在指定GID的下载信息
     */
    public boolean exists(long gid) {
        Cursor cursor = mDatabaseManager.executeQuery(
            "SELECT 1 FROM " + TABLE_NAME + " WHERE gid=? LIMIT 1",
            new String[]{String.valueOf(gid)}
        );

        boolean exists = false;
        if (cursor != null) {
            exists = cursor.moveToFirst();
            cursor.close();
        }

        return exists;
    }

    /**
     * 将Cursor转换为DownloadInfo对象
     */
    private DownloadInfo cursorToDownloadInfo(Cursor cursor) {
        try {
            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.setId(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
            downloadInfo.setGid(cursor.getLong(cursor.getColumnIndexOrThrow("gid")));
            downloadInfo.setToken(cursor.getString(cursor.getColumnIndexOrThrow("token")));
            downloadInfo.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            downloadInfo.setCategory(cursor.getInt(cursor.getColumnIndexOrThrow("category")));
            downloadInfo.setThumb(cursor.getString(cursor.getColumnIndexOrThrow("thumb")));
            downloadInfo.setUploader(cursor.getString(cursor.getColumnIndexOrThrow("uploader")));
            downloadInfo.setRating(cursor.getFloat(cursor.getColumnIndexOrThrow("rating")));
            downloadInfo.setState(cursor.getInt(cursor.getColumnIndexOrThrow("state")));
            downloadInfo.setLegacy(cursor.getInt(cursor.getColumnIndexOrThrow("legacy")));
            downloadInfo.setTime(cursor.getLong(cursor.getColumnIndexOrThrow("time")));

            return downloadInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to DownloadInfo", e);
            return null;
        }
    }
}
