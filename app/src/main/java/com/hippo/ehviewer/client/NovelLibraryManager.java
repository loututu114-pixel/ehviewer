package com.hippo.ehviewer.client;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.util.EroNovelDetector;
import java.util.ArrayList;
import java.util.List;

/**
 * 小说书库管理器
 * 管理本地收藏的小说信息
 */
public class NovelLibraryManager {

    private static final String TAG = "NovelLibraryManager";
    private static final String DATABASE_NAME = "novel_library.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NOVELS = "novels";
    private static final String TABLE_CHAPTERS = "chapters";

    private static NovelLibraryManager instance;
    private final DatabaseHelper dbHelper;
    private final EroNovelDetector detector;

    private NovelLibraryManager(Context context) {
        this.dbHelper = new DatabaseHelper(context.getApplicationContext());
        this.detector = EroNovelDetector.getInstance();
    }

    public static synchronized NovelLibraryManager getInstance(Context context) {
        if (instance == null) {
            instance = new NovelLibraryManager(context);
        }
        return instance;
    }

    /**
     * 添加小说到书库
     */
    public long addNovel(EroNovelDetector.NovelInfo novelInfo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // 插入小说基本信息
            ContentValues novelValues = new ContentValues();
            novelValues.put("url", novelInfo.url);
            novelValues.put("title", novelInfo.title);
            novelValues.put("author", novelInfo.author);
            novelValues.put("description", novelInfo.description);
            novelValues.put("domain", novelInfo.domain);
            novelValues.put("is_ero", novelInfo.isEro ? 1 : 0);
            novelValues.put("create_time", novelInfo.createTime);
            novelValues.put("last_read_time", novelInfo.lastReadTime);
            novelValues.put("read_progress", novelInfo.readProgress);

            long novelId = db.insert(TABLE_NOVELS, null, novelValues);

            if (novelId > 0 && novelInfo.chapters != null && !novelInfo.chapters.isEmpty()) {
                // 插入章节信息
                for (int i = 0; i < novelInfo.chapters.size(); i++) {
                    ContentValues chapterValues = new ContentValues();
                    chapterValues.put("novel_id", novelId);
                    chapterValues.put("chapter_index", i);
                    chapterValues.put("chapter_title", novelInfo.chapters.get(i));
                    chapterValues.put("chapter_url", novelInfo.url + "#chapter_" + i);
                    db.insert(TABLE_CHAPTERS, null, chapterValues);
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Added novel to library: " + novelInfo.title);

            return novelId;
        } catch (Exception e) {
            Log.e(TAG, "Error adding novel to library", e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 更新小说阅读进度
     */
    public boolean updateReadProgress(String url, int progress) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("read_progress", progress);
        values.put("last_read_time", System.currentTimeMillis());

        int rowsAffected = db.update(TABLE_NOVELS, values, "url = ?",
                new String[]{url});

        return rowsAffected > 0;
    }

    /**
     * 获取所有收藏的小说
     */
    @NonNull
    public List<EroNovelDetector.NovelInfo> getAllNovels() {
        List<EroNovelDetector.NovelInfo> novels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NOVELS, null, null, null, null, null,
                "last_read_time DESC");

        try {
            while (cursor.moveToNext()) {
                EroNovelDetector.NovelInfo novel = cursorToNovel(cursor);

                // 获取章节信息
                novel.chapters = getNovelChapters(novel.url);

                novels.add(novel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading novels from database", e);
        } finally {
            cursor.close();
        }

        return novels;
    }

    /**
     * 获取色情小说
     */
    @NonNull
    public List<EroNovelDetector.NovelInfo> getEroNovels() {
        List<EroNovelDetector.NovelInfo> novels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NOVELS, null, "is_ero = ?",
                new String[]{"1"}, null, null, "last_read_time DESC");

        try {
            while (cursor.moveToNext()) {
                EroNovelDetector.NovelInfo novel = cursorToNovel(cursor);
                novel.chapters = getNovelChapters(novel.url);
                novels.add(novel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading ero novels from database", e);
        } finally {
            cursor.close();
        }

        return novels;
    }

    /**
     * 获取普通小说
     */
    @NonNull
    public List<EroNovelDetector.NovelInfo> getNormalNovels() {
        List<EroNovelDetector.NovelInfo> novels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NOVELS, null, "is_ero = ?",
                new String[]{"0"}, null, null, "last_read_time DESC");

        try {
            while (cursor.moveToNext()) {
                EroNovelDetector.NovelInfo novel = cursorToNovel(cursor);
                novel.chapters = getNovelChapters(novel.url);
                novels.add(novel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading normal novels from database", e);
        } finally {
            cursor.close();
        }

        return novels;
    }

    /**
     * 根据URL查找小说
     */
    @Nullable
    public EroNovelDetector.NovelInfo findNovelByUrl(String url) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOVELS, null, "url = ?",
                new String[]{url}, null, null, null);

        EroNovelDetector.NovelInfo novel = null;
        try {
            if (cursor.moveToFirst()) {
                novel = cursorToNovel(cursor);
                novel.chapters = getNovelChapters(url);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding novel by URL", e);
        } finally {
            cursor.close();
        }

        return novel;
    }

    /**
     * 删除小说
     */
    public boolean deleteNovel(String url) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // 删除小说基本信息
            int novelDeleted = db.delete(TABLE_NOVELS, "url = ?", new String[]{url});

            // 删除相关章节
            int chaptersDeleted = db.delete(TABLE_CHAPTERS, "novel_id IN (SELECT id FROM " +
                    TABLE_NOVELS + " WHERE url = ?)", new String[]{url});

            db.setTransactionSuccessful();

            Log.d(TAG, "Deleted novel: " + url + " (novel: " + novelDeleted +
                    ", chapters: " + chaptersDeleted + ")");

            return novelDeleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting novel", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 检查小说是否已收藏
     */
    public boolean isNovelCollected(String url) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOVELS, new String[]{"id"}, "url = ?",
                new String[]{url}, null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();

        return exists;
    }

    /**
     * 获取小说数量
     */
    public int getNovelCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOVELS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * 获取色情小说数量
     */
    public int getEroNovelCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOVELS +
                " WHERE is_ero = 1", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * 获取小说的章节列表
     */
    private List<String> getNovelChapters(String novelUrl) {
        List<String> chapters = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CHAPTERS + " c INNER JOIN " + TABLE_NOVELS + " n ON c.novel_id = n.id",
                new String[]{"c.chapter_title"}, "n.url = ?",
                new String[]{novelUrl}, null, null, "c.chapter_index ASC");

        try {
            while (cursor.moveToNext()) {
                chapters.add(cursor.getString(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading chapters", e);
        } finally {
            cursor.close();
        }

        return chapters;
    }

    /**
     * 将Cursor转换为NovelInfo对象
     */
    private EroNovelDetector.NovelInfo cursorToNovel(Cursor cursor) {
        EroNovelDetector.NovelInfo novel = new EroNovelDetector.NovelInfo();
        novel.url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
        novel.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        novel.author = cursor.getString(cursor.getColumnIndexOrThrow("author"));
        novel.description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        novel.domain = cursor.getString(cursor.getColumnIndexOrThrow("domain"));
        novel.isEro = cursor.getInt(cursor.getColumnIndexOrThrow("is_ero")) == 1;
        novel.createTime = cursor.getLong(cursor.getColumnIndexOrThrow("create_time"));
        novel.lastReadTime = cursor.getLong(cursor.getColumnIndexOrThrow("last_read_time"));
        novel.readProgress = cursor.getInt(cursor.getColumnIndexOrThrow("read_progress"));
        return novel;
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
            // 创建小说表
            String createNovelsTable = "CREATE TABLE " + TABLE_NOVELS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "url TEXT UNIQUE NOT NULL," +
                    "title TEXT NOT NULL," +
                    "author TEXT," +
                    "description TEXT," +
                    "domain TEXT," +
                    "is_ero INTEGER DEFAULT 0," +
                    "create_time INTEGER," +
                    "last_read_time INTEGER DEFAULT 0," +
                    "read_progress INTEGER DEFAULT 0" +
                    ")";

            // 创建章节表
            String createChaptersTable = "CREATE TABLE " + TABLE_CHAPTERS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "novel_id INTEGER," +
                    "chapter_index INTEGER," +
                    "chapter_title TEXT," +
                    "chapter_url TEXT," +
                    "chapter_content TEXT," +
                    "FOREIGN KEY (novel_id) REFERENCES " + TABLE_NOVELS + "(id) ON DELETE CASCADE" +
                    ")";

            db.execSQL(createNovelsTable);
            db.execSQL(createChaptersTable);

            Log.d(TAG, "Novel library database created");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 简单升级策略：删除旧表重建
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAPTERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVELS);
            onCreate(db);
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            super.onConfigure(db);
            // 启用外键约束
            db.setForeignKeyConstraintsEnabled(true);
        }
    }
}
