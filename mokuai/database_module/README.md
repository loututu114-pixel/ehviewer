# EhViewer 数据库模块 (Database Module)

## 概述

数据库模块为EhViewer应用提供完整的数据持久化功能，包括SQLite数据库操作、数据实体管理、查询构建器、数据迁移等。该模块采用轻量级设计，支持事务管理、数据备份恢复等高级功能。

## 主要功能

### 1. 数据库管理
- SQLite数据库初始化和管理
- 数据库版本控制和升级
- 数据库备份和恢复
- 事务管理

### 2. 数据实体管理
- 下载信息 (DownloadInfo)
- 历史记录 (HistoryInfo)
- 收藏夹 (FavoriteInfo)
- 黑名单 (BlackList)
- 标签信息 (GalleryTags)
- 快速搜索 (QuickSearch)

### 3. 数据访问对象 (DAO)
- 统一的CRUD操作接口
- 查询构建器
- 数据监听器
- 批量操作支持

### 4. 查询功能
- 基础查询和高级查询
- 条件查询和模糊搜索
- 分页查询
- 排序和分组

### 5. 数据迁移
- 数据库版本升级
- 数据结构迁移
- 数据导入导出

## 使用方法

### 基本使用

```java
// 1. 初始化数据库管理器
Context context = getApplicationContext();
DatabaseManager dbManager = DatabaseManager.getInstance(context);

// 2. 创建数据访问对象
DownloadInfoDao downloadDao = new DownloadInfoDao(dbManager);

// 3. 创建数据实体
DownloadInfo downloadInfo = new DownloadInfo();
downloadInfo.setGid(123456);
downloadInfo.setTitle("Sample Gallery");
downloadInfo.setState(DownloadInfo.STATE_NONE);

// 4. 插入数据
long id = downloadDao.insert(downloadInfo);

// 5. 查询数据
DownloadInfo queriedInfo = downloadDao.queryByGid(123456);

// 6. 更新数据
queriedInfo.setState(DownloadInfo.STATE_FINISH);
downloadDao.update(queriedInfo);

// 7. 删除数据
downloadDao.delete(123456);
```

### 高级查询

```java
// 根据状态查询
List<DownloadInfo> downloadingItems = downloadDao.queryByState(DownloadInfo.STATE_DOWNLOAD);

// 搜索标题
List<DownloadInfo> searchResults = downloadDao.searchByTitle("keyword");

// 分页查询
List<DownloadInfo> pageResults = downloadDao.queryPage(0, 20, "time DESC");

// 条件查询
List<DownloadInfo> filteredResults = downloadDao.queryByCondition(
    "state=? AND rating>?", new String[]{"3", "4.0"}
);
```

### 事务管理

```java
// 执行事务
dbManager.executeTransaction(new DatabaseManager.DatabaseTransaction() {
    @Override
    public void execute(DatabaseManager db) throws Exception {
        DownloadInfoDao dao = new DownloadInfoDao(db);

        // 在事务中执行多个操作
        DownloadInfo info1 = new DownloadInfo(/*...*/);
        DownloadInfo info2 = new DownloadInfo(/*...*/);

        dao.insert(info1);
        dao.insert(info2);

        // 如果这里抛出异常，整个事务会回滚
        if (someCondition) {
            throw new Exception("Transaction failed");
        }
    }
});
```

### 数据监听器

```java
// 注册数据变化监听器
dbManager.registerListener(new DatabaseManager.DatabaseChangeListener() {
    @Override
    public void onDatabaseChanged(String tableName, DatabaseManager.DatabaseOperation operation) {
        if ("downloads".equals(tableName)) {
            switch (operation) {
                case INSERT:
                    Log.d(TAG, "New download added");
                    // 刷新UI
                    break;
                case UPDATE:
                    Log.d(TAG, "Download updated");
                    break;
                case DELETE:
                    Log.d(TAG, "Download deleted");
                    break;
            }
        }
    }
});
```

## 数据实体

### DownloadInfo (下载信息)
```java
public class DownloadInfo {
    private long id;           // 主键ID
    private long gid;          // 画廊ID
    private String token;      // 访问令牌
    private String title;      // 标题
    private int category;      // 分类
    private String thumb;      // 缩略图URL
    private String uploader;   // 上传者
    private float rating;      // 评分
    private int state;         // 下载状态
    private int legacy;        // 遗留数据标识
    private long time;         // 时间戳

    // 状态常量
    public static final int STATE_NONE = 0;      // 未开始
    public static final int STATE_WAIT = 1;      // 等待中
    public static final int STATE_DOWNLOAD = 2;  // 下载中
    public static final int STATE_FINISH = 3;    // 已完成
    public static final int STATE_FAILED = 4;    // 失败
}
```

### HistoryInfo (历史记录)
```java
public class HistoryInfo {
    private long id;
    private long gid;
    private String token;
    private String title;
    private int category;
    private String thumb;
    private String uploader;
    private float rating;
    private int mode;      // 阅读模式
    private long time;     // 阅读时间
}
```

## 依赖项

在你的`build.gradle`文件中添加必要的依赖：

```gradle
dependencies {
    implementation 'androidx.sqlite:sqlite:2.2.0'
    // 如果使用GreenDAO
    // implementation 'org.greenrobot:greendao:3.3.0'
}
```

## 权限配置

在`AndroidManifest.xml`中无需特殊权限，数据库文件会自动存储在应用的私有目录中。

## 数据库结构

### downloads 表
```sql
CREATE TABLE downloads (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    gid INTEGER UNIQUE NOT NULL,
    token TEXT,
    title TEXT,
    category INTEGER,
    thumb TEXT,
    uploader TEXT,
    rating REAL,
    state INTEGER,
    legacy INTEGER,
    time INTEGER
);
```

### history 表
```sql
CREATE TABLE history (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    gid INTEGER UNIQUE NOT NULL,
    token TEXT,
    title TEXT,
    category INTEGER,
    thumb TEXT,
    uploader TEXT,
    rating REAL,
    mode INTEGER,
    time INTEGER
);
```

### favorites 表
```sql
CREATE TABLE favorites (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    gid INTEGER UNIQUE NOT NULL,
    token TEXT,
    title TEXT,
    category INTEGER,
    thumb TEXT,
    uploader TEXT,
    rating REAL,
    time INTEGER
);
```

## 备份和恢复

```java
// 数据库备份
File backupFile = new File(getExternalFilesDir(null), "database_backup.db");
boolean backupSuccess = dbManager.backupDatabase(backupFile);

// 数据库恢复
boolean restoreSuccess = dbManager.restoreDatabase(backupFile);
```

## 性能优化

1. **索引优化**: 为常用查询字段添加索引
2. **分页查询**: 大数据量时使用分页加载
3. **批量操作**: 使用事务进行批量插入/更新
4. **内存缓存**: 对频繁查询的数据进行内存缓存

## 注意事项

1. **线程安全**: 数据库操作需要在工作线程中执行，避免阻塞UI线程
2. **资源管理**: 使用完Cursor后及时关闭，避免内存泄漏
3. **事务管理**: 涉及多个表操作时使用事务确保数据一致性
4. **版本兼容**: 数据库升级时注意数据迁移和兼容性
5. **存储空间**: 定期清理不需要的历史数据

## 示例项目

查看`examples/`目录中的完整示例代码，了解如何在实际项目中使用数据库模块。

## 许可证

本模块遵循Apache License 2.0协议。
