/*
 * Copyright 2025 EhViewer
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

package com.hippo.ehviewer.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.dao.GalleryTags;
import com.hippo.ehviewer.dao.GalleryTagsDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Database error handler for EhViewer
 * Handles SQLite exceptions and provides fallback mechanisms
 */
public class DatabaseErrorHandler {
    private static final String TAG = "DatabaseErrorHandler";

    /**
     * Safe gallery tags query with error handling
     */
    @NonNull
    public static List<GalleryTags> safeQueryGalleryTags(GalleryTagsDao dao, long gid) {
        try {
            List<GalleryTags> list = dao.queryRaw("where gid =" + gid);
            if (list == null) {
                Log.w(TAG, "GalleryTags query returned null for gid: " + gid);
                return new ArrayList<>();
            }
            return list;
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLite error in GalleryTags query for gid: " + gid, e);
            return handleGalleryTagsQueryError(dao, gid, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in GalleryTags query for gid: " + gid, e);
            return new ArrayList<>();
        }
    }

    /**
     * Safe gallery tags existence check
     */
    public static boolean safeInGalleryTags(GalleryTagsDao dao, long gid) {
        try {
            List<GalleryTags> list = dao.queryRaw("where gid =" + gid);
            return list != null && !list.isEmpty();
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLite error in GalleryTags existence check for gid: " + gid, e);
            return handleGalleryTagsExistenceError(dao, gid, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in GalleryTags existence check for gid: " + gid, e);
            return false;
        }
    }

    /**
     * Handle GalleryTags query errors with fallback mechanisms
     */
    @NonNull
    private static List<GalleryTags> handleGalleryTagsQueryError(GalleryTagsDao dao, long gid, SQLiteException e) {
        String errorMessage = e.getMessage();
        if (errorMessage != null && errorMessage.contains("incomplete input")) {
            Log.w(TAG, "Incomplete SQL input error, using fallback query for gid: " + gid);
            return fallbackQueryGalleryTags(dao, gid);
        }

        // For other SQLite errors, try alternative query methods
        try {
            return dao.queryBuilder().where(GalleryTagsDao.Properties.Gid.eq(gid)).list();
        } catch (Exception fallbackError) {
            Log.e(TAG, "Fallback query also failed for gid: " + gid, fallbackError);
            return new ArrayList<>();
        }
    }

    /**
     * Handle GalleryTags existence check errors
     */
    private static boolean handleGalleryTagsExistenceError(GalleryTagsDao dao, long gid, SQLiteException e) {
        try {
            // Try using queryBuilder as fallback
            return dao.queryBuilder().where(GalleryTagsDao.Properties.Gid.eq(gid)).count() > 0;
        } catch (Exception fallbackError) {
            Log.e(TAG, "Fallback existence check also failed for gid: " + gid, fallbackError);
            return false;
        }
    }

    /**
     * Fallback query method for incomplete SQL errors
     */
    @NonNull
    private static List<GalleryTags> fallbackQueryGalleryTags(GalleryTagsDao dao, long gid) {
        try {
            // Use queryBuilder instead of raw query
            return dao.queryBuilder()
                    .where(GalleryTagsDao.Properties.Gid.eq(gid))
                    .list();
        } catch (Exception e) {
            Log.e(TAG, "Fallback query failed for gid: " + gid, e);
            return new ArrayList<>();
        }
    }

    /**
     * Safe raw query execution with error handling
     */
    @Nullable
    public static Cursor safeRawQuery(SQLiteDatabase db, String sql, String[] selectionArgs) {
        try {
            return db.rawQuery(sql, selectionArgs);
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLite error in raw query: " + sql, e);
            return handleRawQueryError(db, sql, selectionArgs, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in raw query: " + sql, e);
            return null;
        }
    }

    /**
     * Handle raw query errors
     */
    @Nullable
    private static Cursor handleRawQueryError(SQLiteDatabase db, String sql, String[] selectionArgs, SQLiteException e) {
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("incomplete input")) {
                Log.w(TAG, "Incomplete SQL input, attempting to fix query");
                return fixIncompleteQuery(db, sql, selectionArgs);
            } else if (errorMessage.contains("no such table")) {
                Log.w(TAG, "Table doesn't exist, query cannot be executed: " + sql);
                return null;
            } else if (errorMessage.contains("no such column")) {
                Log.w(TAG, "Column doesn't exist, query cannot be executed: " + sql);
                return null;
            }
        }
        return null;
    }

    /**
     * Attempt to fix incomplete SQL queries
     */
    @Nullable
    private static Cursor fixIncompleteQuery(SQLiteDatabase db, String sql, String[] selectionArgs) {
        try {
            // Remove trailing incomplete parts
            if (sql.trim().endsWith("SELECT * FROM")) {
                Log.w(TAG, "Incomplete SELECT query detected, skipping execution");
                return null;
            }

            // Try to complete the query if possible
            if (sql.contains("SELECT * FROM") && !sql.contains("WHERE")) {
                String fixedSql = sql + " WHERE 1=1";
                Log.i(TAG, "Attempting to fix incomplete query: " + sql + " -> " + fixedSql);
                return db.rawQuery(fixedSql, selectionArgs);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to fix incomplete query: " + sql, e);
        }
        return null;
    }

    /**
     * Safe cursor operations with null checks
     */
    public static boolean safeCursorMoveToFirst(Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "Cursor is null in moveToFirst");
            return false;
        }
        try {
            return cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error in cursor moveToFirst", e);
            return false;
        }
    }

    public static boolean safeCursorMoveToNext(Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "Cursor is null in moveToNext");
            return false;
        }
        try {
            return cursor.moveToNext();
        } catch (Exception e) {
            Log.e(TAG, "Error in cursor moveToNext", e);
            return false;
        }
    }

    /**
     * Safe cursor close with null checks
     */
    public static void safeCloseCursor(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            try {
                cursor.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing cursor", e);
            }
        }
    }

    /**
     * Database integrity check
     */
    public static boolean checkDatabaseIntegrity(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("PRAGMA integrity_check", null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String result = cursor.getString(0);
                        return "ok".equalsIgnoreCase(result);
                    }
                } finally {
                    cursor.close();
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Database integrity check failed", e);
            return false;
        }
    }

    /**
     * Database vacuum operation (safe)
     */
    public static void safeVacuumDatabase(SQLiteDatabase db) {
        try {
            db.execSQL("VACUUM");
            Log.i(TAG, "Database vacuum completed successfully");
        } catch (SQLiteException e) {
            Log.w(TAG, "Database vacuum failed", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during database vacuum", e);
        }
    }
}
