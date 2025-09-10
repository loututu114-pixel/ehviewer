# Main-Base 基础框架模块

> [根目录](../../CLAUDE.md) > [hippo-library](../) > **main-base**

---

## 模块职责

Main-Base模块是hippo-library的基础应用框架子模块，提供了Android应用开发的通用组件、示例代码和最佳实践模板。该模块展示了现代Android开发的标准架构模式。

### 核心功能
- **MVVM架构示例**: 展示ViewModel和LiveData的使用模式
- **基础Activity模板**: 通用Activity基类和生命周期管理
- **应用初始化框架**: 标准的Application类和初始化流程
- **Kotlin最佳实践**: 展示Kotlin在Android开发中的应用

---

## 入口与启动

### 主要入口点
- **AppApplication**: 应用程序主类
- **MainActivity**: 主Activity示例
- **MainViewModel**: 主ViewModel示例

### 应用结构
```kotlin
// 典型的MVVM应用架构
AppApplication -> MainActivity -> MainViewModel
     ↓              ↓               ↓
应用初始化      UI控制器        业务逻辑
```

### 初始化流程
```kotlin
// AppApplication.kt中的初始化
class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化各种组件
        initializeComponents()
    }
}
```

---

## 对外接口

### 主要组件类

#### AppApplication
```kotlin
class AppApplication : Application() {
    companion object {
        lateinit var instance: AppApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeComponents()
    }
    
    private fun initializeComponents() {
        // 组件初始化逻辑
    }
}
```

#### MainActivity
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBinding()
        setupViewModel()
        setupObservers()
    }
    
    private fun setupBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }
    
    private fun setupObservers() {
        // 观察者模式设置
    }
}
```

#### MainViewModel
```kotlin
class MainViewModel : ViewModel() {
    private val _data = MutableLiveData<String>()
    val data: LiveData<String> = _data
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    fun loadData() {
        _loading.value = true
        // 数据加载逻辑
        // ...
        _loading.value = false
    }
}
```

---

## 关键依赖与配置

### 模块依赖
```kotlin
// build.gradle.kts (推测)
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("com.google.android.material:material:1.10.0")
    
    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

### Kotlin配置
```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        minSdk = 21
        targetSdk = 34
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}
```

---

## 数据模型

### 基础数据类

#### 应用状态数据
```kotlin
data class AppState(
    val isInitialized: Boolean = false,
    val currentUser: User? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "zh-TW"
)
```

#### 用户数据模型
```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### UI状态数据
```kotlin
data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: Any? = null
)

sealed class UiEvent {
    object Loading : UiEvent()
    data class Success(val data: Any) : UiEvent()
    data class Error(val message: String) : UiEvent()
}
```

### 资源管理
```kotlin
// 资源配置类
data class ResourceConfig(
    val apiBaseUrl: String,
    val imageBaseUrl: String,
    val cacheSize: Long,
    val requestTimeout: Int
)
```

---

## 测试与质量

### 测试文件结构
```
src/test/kotlin/
└── com/example/mainbase/
    ├── ui/
    │   ├── MainActivityTest.kt         # Activity测试
    │   └── MainViewModelTest.kt        # ViewModel测试
    ├── AppApplicationTest.kt           # Application测试
    └── utils/
        └── TestUtils.kt                # 测试工具类

src/androidTest/kotlin/
└── com/example/mainbase/
    ├── ui/
    │   └── MainActivityInstrumentedTest.kt  # UI集成测试
    └── AppApplicationInstrumentedTest.kt    # Application集成测试
```

### 测试覆盖目标

#### ✅ 应该覆盖的测试
- **ViewModel测试**: 业务逻辑和数据流测试
- **Activity测试**: UI交互和生命周期测试
- **Application测试**: 初始化逻辑和全局状态测试
- **工具类测试**: 辅助方法和扩展函数测试

#### ❌ 当前缺失的测试
- 所有测试文件都需要创建
- ViewModel的LiveData测试
- Activity的ViewBinding测试
- Application的组件初始化测试

### 建议的测试用例

#### MainViewModelTest.kt
```kotlin
@ExtendWith(MockitoExtension::class)
class MainViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: MainViewModel
    
    @BeforeEach
    fun setup() {
        viewModel = MainViewModel()
    }
    
    @Test
    fun `when loadData called, should update loading state`() {
        // 测试加载状态变化
        viewModel.loadData()
        
        // 验证loading状态
        assertThat(viewModel.loading.value).isTrue()
    }
    
    @Test
    fun `when data loaded successfully, should update data`() {
        // 测试数据加载成功
    }
}
```

#### MainActivityTest.kt
```kotlin
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    
    private lateinit var activity: MainActivity
    
    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
    }
    
    @Test
    fun `activity should initialize correctly`() {
        assertThat(activity).isNotNull()
        assertThat(activity.isFinishing).isFalse()
    }
    
    @Test
    fun `viewModel should be initialized`() {
        // 测试ViewModel初始化
    }
}
```

---

## 常见问题 (FAQ)

### Q: 如何集成这个基础框架到新项目？
A: 按以下步骤集成：
```kotlin
// 1. 添加依赖
implementation project(':hippo-library:main-base')

// 2. 继承AppApplication
class MyApplication : AppApplication() {
    override fun onCreate() {
        super.onCreate()
        // 自定义初始化逻辑
    }
}

// 3. 在AndroidManifest.xml中声明
<application
    android:name=".MyApplication"
    ... >
```

### Q: 如何自定义ViewModel？
A: 继承基础ViewModel模式：
```kotlin
class CustomViewModel : ViewModel() {
    private val _customData = MutableLiveData<CustomData>()
    val customData: LiveData<CustomData> = _customData
    
    fun loadCustomData() {
        // 自定义加载逻辑
    }
}
```

### Q: 如何处理Configuration Changes？
A: 使用ViewModel和SavedStateHandle：
```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ViewModel自动处理配置变化
    }
}
```

### Q: 支持哪些架构模式？
A: 当前支持：
- **MVVM**: Model-View-ViewModel（推荐）
- **MVP**: Model-View-Presenter（可扩展）
- **MVI**: Model-View-Intent（可扩展）
- **Clean Architecture**: 分层架构（部分支持）

---

## 相关文件清单

### 核心源代码文件
```
app/src/main/java/com/example/mainbase/
├── AppApplication.kt                # 应用程序主类
├── ui/
│   ├── MainActivity.kt             # 主Activity
│   └── MainViewModel.kt            # 主ViewModel
├── data/
│   ├── model/                      # 数据模型
│   ├── repository/                 # 数据仓库
│   └── source/                     # 数据源
├── utils/                          # 工具类
├── extension/                      # Kotlin扩展函数
└── base/                           # 基础类
    ├── BaseActivity.kt
    ├── BaseFragment.kt
    └── BaseViewModel.kt
```

### 资源文件
```
app/src/main/res/
├── layout/
│   ├── activity_main.xml           # 主Activity布局
│   └── fragment_main.xml           # 主Fragment布局
├── values/
│   ├── strings.xml                 # 字符串资源
│   ├── colors.xml                  # 颜色资源
│   ├── dimens.xml                  # 尺寸资源
│   └── styles.xml                  # 样式资源
└── drawable/                       # 图片资源
```

### 配置文件
```
app/
├── build.gradle.kts                # 构建脚本
└── src/main/
    └── AndroidManifest.xml         # 应用清单
```

### 测试文件（需要创建）
```
app/src/test/kotlin/com/example/mainbase/
├── AppApplicationTest.kt
├── ui/
│   ├── MainActivityTest.kt
│   └── MainViewModelTest.kt
└── utils/
    └── TestUtils.kt

app/src/androidTest/kotlin/com/example/mainbase/
├── AppApplicationInstrumentedTest.kt
└── ui/
    └── MainActivityInstrumentedTest.kt
```

---

## 架构设计模式

### MVVM架构实现
```kotlin
// View层 (Activity/Fragment)
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> showLoading()
                is UiState.Success -> showData(state.data)
                is UiState.Error -> showError(state.message)
            }
        }
    }
}

// ViewModel层
class MainViewModel : ViewModel() {
    private val repository = MainRepository()
    
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val data = repository.getData()
                _uiState.value = UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Model层 (Repository)
class MainRepository {
    private val dataSource = MainDataSource()
    
    suspend fun getData(): MainData {
        return dataSource.fetchData()
    }
}
```

### Dependency Injection设置
```kotlin
// 简化的DI实现
object ServiceLocator {
    private val repositories = mutableMapOf<String, Any>()
    
    inline fun <reified T> getRepository(): T {
        val key = T::class.java.simpleName
        return repositories[key] as T ?: run {
            val instance = createRepository<T>()
            repositories[key] = instance
            instance
        }
    }
    
    inline fun <reified T> createRepository(): T {
        return when (T::class) {
            MainRepository::class -> MainRepository() as T
            else -> throw IllegalArgumentException("Unknown repository type")
        }
    }
}
```

---

## API使用示例

### 基础使用
```kotlin
// 1. 创建Activity
class CustomActivity : AppCompatActivity() {
    private val viewModel: CustomViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        observeData()
    }
    
    private fun setupUI() {
        // UI设置逻辑
    }
    
    private fun observeData() {
        viewModel.data.observe(this) { data ->
            // 数据处理
        }
    }
}

// 2. 创建ViewModel
class CustomViewModel : ViewModel() {
    private val repository = ServiceLocator.getRepository<CustomRepository>()
    
    fun performAction() {
        viewModelScope.launch {
            // 异步操作
        }
    }
}
```

### 扩展功能
```kotlin
// 扩展基础Activity
abstract class BaseListActivity<T> : AppCompatActivity() {
    protected abstract val viewModel: BaseListViewModel<T>
    protected abstract val adapter: BaseAdapter<T>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupRecyclerView()
        observeList()
    }
    
    protected open fun setupRecyclerView() {
        // RecyclerView通用设置
    }
    
    protected open fun observeList() {
        viewModel.items.observe(this) { items ->
            adapter.submitList(items)
        }
    }
}
```

---

## 变更记录 (Changelog)

### 2025-09-10
- **模块文档创建**: 完成main-base模块的架构分析
- **MVVM模式整理**: 详细描述了架构模式和最佳实践
- **测试策略规划**: 制定了完整的测试覆盖计划
- **示例代码**: 提供了详细的使用示例和扩展方法

### 待办事项
- [ ] 创建完整的单元测试和集成测试套件
- [ ] 实现Dependency Injection框架
- [ ] 添加Navigation组件支持
- [ ] 完善数据绑定和LiveData使用
- [ ] 增加Fragment基类和导航管理
- [ ] 实现主题和多语言支持

---

<div align="center">

[⬆ 返回根目录](../../CLAUDE.md) | [📚 hippo-library](../) | [🏗️ Main-Base模块](./CLAUDE.md)

**Main-Base模块文档** - EhViewerh v2.0.0.5

</div>