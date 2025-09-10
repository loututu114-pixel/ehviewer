# Database 数据库模块

> [根目录](../../../CLAUDE.md) > [hippo-library](../../) > [modules](../) > **database**

---

## 模块职责

Database模块是hippo-library的数据库管理子模块，提供了标准化的数据库配置、连接管理、迁移助手等基础设施。该模块为上层应用提供统一的数据库访问接口和最佳实践。

### 核心功能
- **数据库配置管理**: 统一的数据库连接参数配置
- **连接池管理**: 高效的数据库连接复用机制
- **数据库迁移**: 版本升级时的数据迁移助手
- **配置接口定义**: 标准化的数据库配置接口

---

## 入口与启动

### 主要入口点
- **DatabaseManager**: 数据库管理器主类
- **DatabaseConfig**: 数据库配置类
- **DatabaseHelper**: 数据库操作助手类

### 初始化流程
```java
// 典型的初始化流程
DatabaseConfig config = new DatabaseConfig.Builder()
    .setDatabaseName("app_database.db")
    .setVersion(1)
    .build();
    
DatabaseManager manager = new DatabaseManager(context, config);
SQLiteDatabase db = manager.getReadableDatabase();
```

---

## 对外接口

### 核心接口类

#### INetworkConfig 接口
```java
public interface INetworkConfig {
    // 网络配置相关接口定义
    String getBaseUrl();
    int getConnectTimeout();
    int getReadTimeout();
    boolean isDebugEnabled();
}
```

#### INetworkCallback 接口  
```java
public interface INetworkCallback {
    // 网络回调接口定义
    void onSuccess(Object result);
    void onError(NetworkException exception);
    void onProgress(int progress);
}
```

### 主要API类

#### DatabaseManager
```java
public class DatabaseManager {
    // 数据库管理器主要方法
    public SQLiteDatabase getReadableDatabase()
    public SQLiteDatabase getWritableDatabase()
    public void close()
    public boolean isOpen()
    public void execSQL(String sql)
}
```

#### DatabaseConfig
```java
public class DatabaseConfig {
    // 数据库配置参数
    private String databaseName;
    private int version;
    private String databasePath;
    
    public static class Builder {
        public Builder setDatabaseName(String name)
        public Builder setVersion(int version)
        public DatabaseConfig build()
    }
}
```

#### DatabaseHelper
```java
public class DatabaseHelper {
    // 数据库操作辅助方法
    public void createTable(SQLiteDatabase db, String tableName, String[] columns)
    public void dropTable(SQLiteDatabase db, String tableName)
    public boolean tableExists(SQLiteDatabase db, String tableName)
    public void execSqlFromAssets(SQLiteDatabase db, String fileName)
}
```

---

## 关键依赖与配置

### 模块依赖
```kotlin
// 假设的依赖配置（基于标准Android项目）
dependencies {
    implementation 'androidx.sqlite:sqlite:2.x.x'
    implementation 'androidx.room:room-runtime:2.x.x'
    
    // 测试依赖
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'androidx.test:core:1.4.0'
}
```

### 配置参数
```java
// DatabaseConfig支持的配置选项
public class DatabaseConfig {
    public static final int DEFAULT_VERSION = 1;
    public static final String DEFAULT_DATABASE_NAME = "database.db";
    
    // 连接池配置
    private int maxConnections = 5;
    private int connectionTimeout = 30000; // 30秒
    
    // 日志配置
    private boolean enableSqlLog = false;
    private boolean enablePerformanceLog = false;
}
```

---

## 数据模型

### 核心实体类

#### DatabaseConfig 配置实体
```java
public class DatabaseConfig {
    private String databaseName;        // 数据库名称
    private int version;               // 数据库版本
    private String databasePath;       // 数据库路径
    private int maxConnections;        // 最大连接数
    private int connectionTimeout;     // 连接超时
    private boolean enableSqlLog;      // SQL日志开关
    private boolean enablePerformanceLog; // 性能日志开关
}
```

#### MigrationInfo 迁移信息
```java
public class MigrationInfo {
    private int fromVersion;           // 源版本
    private int toVersion;            // 目标版本
    private String[] migrationSqls;   // 迁移SQL语句
    private boolean requiresBackup;   // 是否需要备份
}
```

### 数据库表设计

由于这是基础框架模块，不直接包含业务数据表，但可能包含：

#### 系统配置表
```sql
CREATE TABLE system_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key TEXT UNIQUE NOT NULL,
    config_value TEXT,
    create_time INTEGER,
    update_time INTEGER
);
```

#### 迁移历史表
```sql
CREATE TABLE migration_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    from_version INTEGER NOT NULL,
    to_version INTEGER NOT NULL,
    migration_time INTEGER NOT NULL,
    success INTEGER NOT NULL DEFAULT 0
);
```

---

## 测试与质量

### 测试文件结构
```
src/test/java/
└── com/hippo/library/module/database/
    ├── DatabaseManagerTest.java        # 数据库管理器测试
    ├── DatabaseConfigTest.java         # 配置类测试
    ├── DatabaseHelperTest.java         # 助手类测试
    └── migration/
        └── MigrationHelperTest.java    # 迁移助手测试
```

### 测试覆盖目标

#### ✅ 应该覆盖的测试
- **配置管理测试**: 配置参数的验证和默认值
- **连接管理测试**: 连接池的创建、复用、释放
- **SQL执行测试**: 基本的SQL执行和异常处理
- **迁移功能测试**: 数据库版本升级迁移逻辑
- **并发访问测试**: 多线程环境下的数据库访问

#### ❌ 当前缺失的测试
- 所有测试文件都需要创建
- 需要Mock SQLite环境进行单元测试
- 需要集成测试验证实际数据库操作

### 建议的测试用例

#### DatabaseManagerTest.java
```java
@Test
public void testGetReadableDatabase() {
    // 测试只读数据库获取
}

@Test  
public void testGetWritableDatabase() {
    // 测试可写数据库获取
}

@Test
public void testConcurrentAccess() {
    // 测试并发数据库访问
}
```

#### MigrationHelperTest.java
```java
@Test
public void testVersionUpgrade() {
    // 测试版本升级迁移
}

@Test
public void testMigrationRollback() {
    // 测试迁移回滚
}
```

---

## 常见问题 (FAQ)

### Q: 如何配置数据库连接池大小？
A: 通过DatabaseConfig的maxConnections参数配置：
```java
DatabaseConfig config = new DatabaseConfig.Builder()
    .setMaxConnections(10)  // 设置最大连接数为10
    .build();
```

### Q: 数据库迁移失败怎么办？
A: MigrationHelper提供了回滚机制：
```java
try {
    MigrationHelper.migrate(db, fromVersion, toVersion);
} catch (MigrationException e) {
    MigrationHelper.rollback(db, fromVersion);
}
```

### Q: 如何启用SQL调试日志？
A: 在DatabaseConfig中启用日志：
```java
DatabaseConfig config = new DatabaseConfig.Builder()
    .setEnableSqlLog(true)
    .setEnablePerformanceLog(true)
    .build();
```

### Q: 支持哪些数据库类型？
A: 当前主要支持：
- SQLite（Android默认）
- 可扩展支持Room数据库
- 理论上支持任何符合JDBC标准的数据库

---

## 相关文件清单

### 核心源代码文件
```
src/main/java/com/hippo/library/module/database/
├── DatabaseManager.java              # 数据库管理器主类
├── DatabaseConfig.java               # 数据库配置类
├── DatabaseHelper.java               # 数据库操作助手
├── migration/
│   └── MigrationHelper.java         # 数据库迁移助手
├── interfaces/
│   ├── IDatabaseConfig.java         # 数据库配置接口
│   └── IDatabaseCallback.java       # 数据库回调接口
└── exception/
    ├── DatabaseException.java       # 数据库异常
    └── MigrationException.java      # 迁移异常
```

### 配置文件
```
src/main/resources/
├── database-config.properties       # 默认配置文件
└── sql/
    ├── init.sql                     # 初始化SQL脚本
    └── migrations/                  # 迁移SQL脚本
        ├── v1_to_v2.sql
        └── v2_to_v3.sql
```

### 测试文件（需要创建）
```
src/test/java/com/hippo/library/module/database/
├── DatabaseManagerTest.java
├── DatabaseConfigTest.java  
├── DatabaseHelperTest.java
└── migration/
    └── MigrationHelperTest.java

src/androidTest/java/com/hippo/library/module/database/
├── DatabaseIntegrationTest.java
└── MigrationIntegrationTest.java
```

---

## API使用示例

### 基础使用
```java
// 1. 创建配置
DatabaseConfig config = new DatabaseConfig.Builder()
    .setDatabaseName("my_app.db")
    .setVersion(2)
    .setMaxConnections(5)
    .setEnableSqlLog(BuildConfig.DEBUG)
    .build();

// 2. 初始化管理器
DatabaseManager dbManager = new DatabaseManager(context, config);

// 3. 获取数据库实例
SQLiteDatabase db = dbManager.getWritableDatabase();

// 4. 执行SQL操作
db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT)");
```

### 数据库迁移
```java
// 版本升级时的迁移处理
public class MyDatabaseHelper extends SQLiteOpenHelper {
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MigrationHelper migrationHelper = new MigrationHelper();
        try {
            migrationHelper.migrate(db, oldVersion, newVersion);
        } catch (MigrationException e) {
            Log.e(TAG, "Database migration failed", e);
            migrationHelper.rollback(db, oldVersion);
        }
    }
}
```

### 高级配置
```java
DatabaseConfig config = new DatabaseConfig.Builder()
    .setDatabaseName("advanced_app.db")
    .setVersion(3)
    .setMaxConnections(10)
    .setConnectionTimeout(60000)  // 60秒超时
    .setEnableSqlLog(true)
    .setEnablePerformanceLog(true)
    .setDatabasePath("/custom/path/")
    .build();
```

---

## 变更记录 (Changelog)

### 2025-09-10
- **模块文档创建**: 完成database模块的架构分析
- **API接口整理**: 梳理了核心类和接口的设计
- **测试策略规划**: 制定了完整的测试覆盖计划
- **使用示例**: 提供了详细的API使用示例

### 待办事项
- [ ] 创建完整的单元测试套件
- [ ] 实现数据库连接池优化
- [ ] 添加性能监控和统计功能
- [ ] 完善数据库迁移工具
- [ ] 增加数据库备份和恢复功能

---

<div align="center">

[⬆ 返回根目录](../../../CLAUDE.md) | [📚 hippo-library](../../) | [🗄️ Database模块](./CLAUDE.md)

**Database模块文档** - EhViewerh v2.0.0.5

</div>