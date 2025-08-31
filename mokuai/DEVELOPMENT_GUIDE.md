# 🛠️ EhViewer 模块库开发规范

## 📋 概述

本文档定义了EhViewer模块库的开发规范和最佳实践，确保所有模块遵循统一的代码风格、架构模式和质量标准。

## 🎯 开发原则

### 1. SOLID原则
- **单一职责**：每个模块只负责一个明确的功能
- **开闭原则**：对扩展开放，对修改封闭
- **里氏替换**：子类可以替换父类使用
- **接口隔离**：使用小而专一的接口
- **依赖倒置**：依赖抽象而非具体实现

### 2. DRY原则（Don't Repeat Yourself）
- 避免代码重复
- 提取公共逻辑到工具类
- 使用统一的错误处理机制

### 3. KISS原则（Keep It Simple, Stupid）
- 保持代码简洁明了
- 避免过度设计
- 使用最简单的解决方案

## 📁 项目结构规范

### 模块目录结构
```bash
module_name/
├── src/main/java/com/hippo/ehviewer/modulename/
│   ├── ModuleNameManager.java          # 核心管理类
│   ├── ModuleNameConfig.java           # 配置类
│   ├── interfaces/                     # 接口定义
│   ├── impl/                          # 实现类
│   ├── utils/                         # 工具类
│   ├── exception/                     # 异常类
│   └── constants/                     # 常量定义
├── src/main/res/                       # 资源文件
│   ├── layout/                        # 布局文件
│   ├── values/                        # 值文件
│   ├── drawable/                      # 图片资源
│   └── xml/                           # XML配置
├── src/androidTest/                    # 仪器化测试
├── src/test/                          # 单元测试
├── proguard-rules.pro                 # 混淆规则
├── build.gradle                      # 构建配置
└── README.md                         # 模块文档
```

### 包命名规范
```java
com.hippo.ehviewer.{module_name}           // 主包
com.hippo.ehviewer.{module_name}.interfaces // 接口包
com.hippo.ehviewer.{module_name}.impl       // 实现包
com.hippo.ehviewer.{module_name}.utils      // 工具包
com.hippo.ehviewer.{module_name}.exception  // 异常包
com.hippo.ehviewer.{module_name}.constants  // 常量包
```

## 🔧 代码规范

### 命名规范

#### 类命名
```java
// 正确示例
public class NetworkClient {}           // 客户端类
public class DatabaseManager {}         // 管理器类
public class ImageLoader {}             // 加载器类
public class SettingsActivity {}        // Activity类
public class DownloadService {}         // Service类

// 错误示例
public class networkClient {}           // 驼峰命名错误
public class DBManager {}               // 缩写过多
public class imgLoader {}               // 缩写不规范
```

#### 方法命名
```java
// 动词 + 名词形式
public void loadImage(String url) {}           // 加载图片
public void saveData(Object data) {}           // 保存数据
public void deleteFile(String path) {}         // 删除文件
public boolean isConnected() {}                // 检查连接状态
public List<User> getUsers() {}                // 获取用户列表
public void setUserName(String name) {}        // 设置用户名
```

#### 变量命名
```java
// 局部变量
String userName;                    // 驼峰命名
int maxRetryCount;                  // 完整单词
List<String> imageUrls;             // 复数形式

// 常量
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_USER_NAME = "guest";
public static final long CACHE_SIZE_LIMIT = 50 * 1024 * 1024;
```

### 代码风格

#### 缩进和空格
```java
// 正确示例
public class ExampleClass {

    private static final int MAX_COUNT = 10;

    public void exampleMethod() {
        if (condition) {
            doSomething();
        } else {
            doOtherThing();
        }
    }

    private void doSomething() {
        // 方法实现
    }
}
```

#### 注释规范
```java
/**
 * 用户管理器
 * 负责用户的登录、注册、信息管理等功能
 *
 * @author Developer Name
 * @version 1.0.0
 * @since 2024-01-01
 */
public class UserManager {

    /**
     * 用户登录
     *
     * @param username 用户名，不能为空
     * @param password 密码，不能为空
     * @return 登录结果
     * @throws IllegalArgumentException 当用户名或密码为空时抛出
     */
    public LoginResult login(String username, String password) {
        // 参数校验
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        // 登录逻辑
        return performLogin(username, password);
    }

    /**
     * 执行登录操作（私有方法）
     */
    private LoginResult performLogin(String username, String password) {
        // 实现细节
        return new LoginResult(true, "Login successful");
    }
}
```

## 🏗️ 架构规范

### 设计模式应用

#### 单例模式
```java
public class NetworkManager {

    private static volatile NetworkManager instance;

    private NetworkManager() {
        // 私有构造函数
    }

    public static NetworkManager getInstance() {
        if (instance == null) {
            synchronized (NetworkManager.class) {
                if (instance == null) {
                    instance = new NetworkManager();
                }
            }
        }
        return instance;
    }
}
```

#### 工厂模式
```java
public interface ParserFactory {

    Parser createParser(String type);

    static ParserFactory getFactory() {
        return new DefaultParserFactory();
    }
}

class DefaultParserFactory implements ParserFactory {

    @Override
    public Parser createParser(String type) {
        switch (type.toLowerCase()) {
            case "json":
                return new JsonParser();
            case "xml":
                return new XmlParser();
            case "html":
                return new HtmlParser();
            default:
                throw new IllegalArgumentException("Unsupported parser type: " + type);
        }
    }
}
```

#### 观察者模式
```java
public interface DownloadListener {

    void onProgress(int progress);

    void onComplete(File file);

    void onError(Exception e);
}

public class DownloadManager {

    private final List<DownloadListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(DownloadListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DownloadListener listener) {
        listeners.remove(listener);
    }

    private void notifyProgress(int progress) {
        for (DownloadListener listener : listeners) {
            try {
                listener.onProgress(progress);
            } catch (Exception e) {
                // 记录错误但不中断其他监听器
                Log.e(TAG, "Error notifying progress", e);
            }
        }
    }
}
```

### 异常处理规范

#### 自定义异常
```java
public class NetworkException extends Exception {

    public static final int ERROR_TIMEOUT = 1;
    public static final int ERROR_CONNECTION = 2;
    public static final int ERROR_SERVER = 3;

    private final int errorCode;

    public NetworkException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NetworkException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
```

#### 异常处理最佳实践
```java
public class NetworkClient {

    public void sendRequest(String url, Callback callback) {
        try {
            validateUrl(url);
            HttpResponse response = executeRequest(url);
            callback.onSuccess(response);
        } catch (IllegalArgumentException e) {
            callback.onError(new NetworkException(NetworkException.ERROR_INVALID_URL, "Invalid URL", e));
        } catch (IOException e) {
            callback.onError(new NetworkException(NetworkException.ERROR_CONNECTION, "Connection failed", e));
        } catch (Exception e) {
            callback.onError(new NetworkException(NetworkException.ERROR_UNKNOWN, "Unknown error", e));
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
    }
}
```

## 🧪 测试规范

### 单元测试
```java
public class CalculatorTest {

    private Calculator calculator;

    @Before
    public void setUp() {
        calculator = new Calculator();
    }

    @Test
    public void testAddition() {
        // Given
        int a = 2;
        int b = 3;

        // When
        int result = calculator.add(a, b);

        // Then
        assertEquals(5, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdditionWithNegativeNumbers() {
        // 测试异常情况
        calculator.add(-1, 5);
    }
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4.class)
public class DatabaseIntegrationTest {

    private DatabaseManager databaseManager;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        databaseManager = DatabaseManager.getInstance(context);
    }

    @Test
    public void testUserPersistence() {
        // 创建测试用户
        User user = new User("test_user", "test@example.com");

        // 保存用户
        long userId = databaseManager.saveUser(user);
        assertTrue(userId > 0);

        // 查询用户
        User savedUser = databaseManager.getUserById(userId);
        assertNotNull(savedUser);
        assertEquals("test_user", savedUser.getUsername());

        // 清理测试数据
        databaseManager.deleteUser(userId);
    }
}
```

### UI测试
```java
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityTestRule<LoginActivity> activityRule =
            new ActivityTestRule<>(LoginActivity.class);

    @Test
    public void testLoginSuccess() {
        // 输入用户名和密码
        onView(withId(R.id.username_edit_text))
                .perform(typeText("testuser"), closeSoftKeyboard());

        onView(withId(R.id.password_edit_text))
                .perform(typeText("password123"), closeSoftKeyboard());

        // 点击登录按钮
        onView(withId(R.id.login_button))
                .perform(click());

        // 验证登录成功
        onView(withId(R.id.welcome_text))
                .check(matches(isDisplayed()));
    }
}
```

## 📊 性能优化规范

### 内存管理
```java
public class BitmapManager {

    private final Set<Bitmap> bitmaps = new HashSet<>();

    public Bitmap loadBitmap(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        bitmaps.add(bitmap); // 跟踪Bitmap对象
        return bitmap;
    }

    public void releaseBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmaps.remove(bitmap);
        }
    }

    public void releaseAll() {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        bitmaps.clear();
    }
}
```

### 异步处理
```java
public class AsyncTaskManager {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public <T> void executeAsync(Callable<T> task, Callback<T> callback) {
        executor.submit(() -> {
            try {
                T result = task.call();
                runOnUiThread(() -> callback.onSuccess(result));
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError(e));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
```

## 🔒 安全规范

### 数据验证
```java
public class InputValidator {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // 移除潜在的XSS攻击向量
        return input.replaceAll("<script[^>]*>.*?</script>", "")
                   .replaceAll("<[^>]+>", "");
    }
}
```

### 网络安全
```java
public class SecureNetworkClient {

    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 30000;

    private final OkHttpClient client;

    public SecureNetworkClient() {
        client = new OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .sslSocketFactory(getSSLSocketFactory(), getTrustManager())
            .hostnameVerifier(getHostnameVerifier())
            .build();
    }

    private SSLSocketFactory getSSLSocketFactory() {
        // 实现安全的SSL配置
        return sslSocketFactory;
    }

    private X509TrustManager getTrustManager() {
        // 实现证书验证
        return trustManager;
    }

    private HostnameVerifier getHostnameVerifier() {
        // 实现主机名验证
        return hostnameVerifier;
    }
}
```

## 📝 文档规范

### README.md 模板
```markdown
# 模块名称

## 概述
简要描述模块的功能和用途。

## 主要功能
- 功能点1
- 功能点2
- 功能点3

## 使用方法

### 基本使用
```java
// 示例代码
```

### 高级配置
```java
// 高级配置示例
```

## 依赖项
```gradle
// 依赖配置
```

## 注意事项
- 注意事项1
- 注意事项2

## 许可证
遵循Apache License 2.0协议。
```

### API文档
```java
/**
 * 用户服务类
 * 提供用户相关的业务逻辑处理
 *
 * <p>该类主要负责：</p>
 * <ul>
 *   <li>用户注册和登录</li>
 *   <li>用户信息管理</li>
 *   <li>用户权限控制</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * UserService userService = new UserService();
 * User user = userService.login("username", "password");
 * </pre>
 *
 * @author 开发者姓名
 * @version 1.0.0
 * @since 2024-01-01
 * @see User
 * @see UserRepository
 */
public class UserService {

    /**
     * 用户登录
     *
     * @param username 用户名，必须非空且长度大于3
     * @param password 密码，必须非空且长度大于6
     * @return 登录成功的用户信息，失败时抛出异常
     * @throws IllegalArgumentException 当用户名或密码不符合要求时
     * @throws AuthenticationException 当认证失败时
     * @see User#login(String, String)
     */
    public User login(String username, String password) {
        // 实现代码
    }
}
```

## 🔄 版本管理

### 版本号规范
```
主版本号.次版本号.修订号[-预发布版本]
```

- **主版本号**：不兼容的API变更
- **次版本号**：新增功能，向后兼容
- **修订号**：修复bug，向后兼容
- **预发布版本**：alpha、beta、rc等

### 更新日志格式
```markdown
## [1.2.0] - 2024-01-15
### 新增
- 新功能1
- 新功能2

### 优化
- 性能优化1
- 性能优化2

### 修复
- 修复bug1
- 修复bug2

### 破坏性变更
- 破坏性变更1
- 破坏性变更2
```

## 🤝 代码审查规范

### 审查清单
- [ ] 代码符合命名规范
- [ ] 代码有适当的注释
- [ ] 代码经过单元测试
- [ ] 代码处理了边界情况
- [ ] 代码遵循SOLID原则
- [ ] 代码没有安全漏洞
- [ ] 代码经过性能测试
- [ ] 文档已更新

### 审查意见分类
- **必须修复**：影响功能或安全的严重问题
- **建议修复**：影响代码质量但不影响功能的问题
- **可选优化**：可以提升代码质量的建议
- **信息**：一般性信息或解释说明

---

**注意**：本规范会随着项目的发展不断更新，请定期查看最新版本。
