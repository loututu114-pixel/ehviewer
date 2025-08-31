# 🚀 EhViewer 模块库快速使用指南

## 5分钟上手EhViewer模块库

### 目标
在5分钟内搭建一个具有完整功能的Android应用，使用EhViewer模块库的核心功能。

### 步骤1：项目创建（1分钟）

1. **创建新项目**
```bash
# 创建项目目录
mkdir MyAwesomeApp
cd MyAwesomeApp

# 初始化Android项目
# 使用Android Studio创建新项目，或复制现有项目结构
```

2. **复制模块库**
```bash
# 从EhViewer项目复制模块库
cp -r /path/to/ehviewer/mokuai ./libraries
```

3. **配置项目结构**
```
MyAwesomeApp/
├── app/                    # 主应用
├── libraries/             # 模块库
│   ├── network/
│   ├── database/
│   ├── settings/
│   └── utils/
└── settings.gradle       # 项目配置
```

### 步骤2：配置依赖（2分钟）

#### settings.gradle
```gradle
include ':app'
include ':libraries:network'
include ':libraries:database'
include ':libraries:settings'
include ':libraries:utils'
```

#### app/build.gradle
```gradle
dependencies {
    // EhViewer模块库
    implementation project(':libraries:network')
    implementation project(':libraries:database')
    implementation project(':libraries:settings')
    implementation project(':libraries:utils')

    // 其他依赖
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

### 步骤3：应用初始化（1分钟）

#### 创建Application类
```java
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化网络模块
        NetworkClient.init(this);

        // 初始化数据库模块
        DatabaseManager.init(this);

        // 初始化设置模块
        SettingsManager.init(this);

        // 初始化工具类
        Utils.init(this);
    }
}
```

#### 配置AndroidManifest.xml
```xml
<application
    android:name=".MyApp"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/AppTheme">

    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />

    <!-- 主要Activity -->
    <activity android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

</application>
```

### 步骤4：核心功能实现（1分钟）

#### 创建数据模型
```java
public class User {
    private long id;
    private String name;
    private String email;

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

#### 实现网络请求
```java
public class ApiService {

    private NetworkClient networkClient;

    public ApiService(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public void getUsers(NetworkCallback<List<User>> callback) {
        Object[] args = {"https://api.example.com/users"};
        NetworkRequest request = new NetworkRequest(NetworkClient.METHOD_GET, args, callback);
        networkClient.execute(request);
    }
}
```

#### 实现数据存储
```java
public class UserRepository {

    private DatabaseManager databaseManager;

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveUser(User user) {
        // 实现保存逻辑
    }

    public User getUser(long userId) {
        // 实现查询逻辑
        return null;
    }

    public List<User> getAllUsers() {
        // 实现查询所有用户逻辑
        return new ArrayList<>();
    }
}
```

#### 实现设置管理
```java
public class AppSettings {

    private SettingsManager settingsManager;

    public AppSettings(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void setTheme(String theme) {
        settingsManager.putString("app_theme", theme);
    }

    public String getTheme() {
        return settingsManager.getString("app_theme", "light");
    }

    public void setNotificationEnabled(boolean enabled) {
        settingsManager.putBoolean("notification_enabled", enabled);
    }

    public boolean isNotificationEnabled() {
        return settingsManager.getBoolean("notification_enabled", true);
    }
}
```

### 步骤5：UI实现（0分钟）

#### 创建主Activity
```java
public class MainActivity extends AppCompatActivity {

    private NetworkClient networkClient;
    private DatabaseManager databaseManager;
    private SettingsManager settingsManager;

    private TextView userInfoText;
    private Button loadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        userInfoText = findViewById(R.id.userInfoText);
        loadButton = findViewById(R.id.loadButton);

        // 获取模块实例
        networkClient = NetworkClient.getInstance(this);
        databaseManager = DatabaseManager.getInstance(this);
        settingsManager = SettingsManager.getInstance(this);

        // 设置按钮点击事件
        loadButton.setOnClickListener(v -> loadUserData());
    }

    private void loadUserData() {
        // 显示加载状态
        userInfoText.setText("加载中...");

        // 执行网络请求
        Object[] args = {"https://api.example.com/user/1"};
        NetworkCallback<User> callback = new NetworkCallback<User>() {
            @Override
            public void onSuccess(User user) {
                // 更新UI
                userInfoText.setText("用户名: " + user.getName() + "\n邮箱: " + user.getEmail());
            }

            @Override
            public void onFailure(Exception e) {
                userInfoText.setText("加载失败: " + e.getMessage());
            }

            @Override
            public void onCancel() {
                userInfoText.setText("请求已取消");
            }
        };

        NetworkRequest request = new NetworkRequest(NetworkClient.METHOD_GET, args, callback);
        networkClient.execute(request);
    }
}
```

#### 创建布局文件
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/userInfoText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="用户信息将显示在这里"
        android:textSize="16sp"
        android:layout_marginBottom="16dp" />

    <Button
        android:id="@+id/loadButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="加载用户数据" />

</LinearLayout>
```

## 🎯 验证功能

### 测试网络功能
```java
// 测试网络连接
NetworkCallback<String> testCallback = new NetworkCallback<String>() {
    @Override
    public void onSuccess(String result) {
        Log.d("Test", "网络测试成功: " + result);
    }

    @Override
    public void onFailure(Exception e) {
        Log.e("Test", "网络测试失败", e);
    }
};

Object[] testArgs = {"https://httpbin.org/get"};
NetworkRequest testRequest = new NetworkRequest(NetworkClient.METHOD_GET, testArgs, testCallback);
networkClient.execute(testRequest);
```

### 测试数据库功能
```java
// 测试数据库操作
User testUser = new User();
testUser.setId(1);
testUser.setName("Test User");
testUser.setEmail("test@example.com");

// 保存用户
// databaseManager.saveUser(testUser);

// 查询用户
// User savedUser = databaseManager.getUser(1);
```

### 测试设置功能
```java
// 测试设置操作
settingsManager.putString("test_key", "test_value");
String value = settingsManager.getString("test_key", "");
Log.d("Test", "设置测试: " + value);
```

## 🚀 扩展功能

### 添加图片功能
```gradle
// 添加图片模块依赖
implementation project(':libraries:image')
```

```java
// 初始化图片模块
ImageLoader imageLoader = ImageLoader.getInstance(this);

// 加载图片
imageLoader.loadImage("https://example.com/avatar.jpg", imageView);
```

### 添加下载功能
```gradle
// 添加下载模块依赖
implementation project(':libraries:download')
```

```java
// 初始化下载模块
DownloadManager downloadManager = DownloadManager.getInstance(this);

// 创建下载任务
DownloadTask task = new DownloadTask();
task.setUrl("https://example.com/file.zip");
task.setLocalPath("/sdcard/downloads/file.zip");

// 开始下载
downloadManager.addDownloadTask(task);
```

## 📋 完整功能清单

✅ **已实现的核心功能**：
- 网络请求（GET/POST/PUT/DELETE）
- 数据持久化（SQLite数据库）
- 设置管理（键值对存储）
- 工具类（文件、网络、字符串操作）
- 异步处理和错误处理
- 内存管理和资源回收

## 🎉 总结

使用EhViewer模块库，你可以在**5分钟内**创建一个具有以下功能的Android应用：

- ✅ 网络数据请求和处理
- ✅ 本地数据存储和查询
- ✅ 用户设置管理和持久化
- ✅ 完整的错误处理机制
- ✅ 高效的内存管理
- ✅ 可扩展的架构设计

这个模块库为快速开发高质量的Android应用提供了完整的解决方案！

---

**💡 提示**：这个快速指南展示了最基础的使用方式。在实际项目中，你可以根据需要添加更多模块和功能。查看各个模块的详细文档了解更多高级用法。
