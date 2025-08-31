# 📋 EhViewer 模块库项目模板

## 概述

本文档提供了基于EhViewer模块库的新项目快速搭建模板，帮助开发者快速创建高质量的Android应用。

## 🚀 快速开始模板

### 1. 项目结构模板

```bash
your-awesome-app/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # 应用清单
│   │   ├── java/com/example/yourapp/
│   │   │   ├── AppApplication.kt # 应用入口
│   │   │   ├── di/               # 依赖注入
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── NetworkModule.kt
│   │   │   │   └── DatabaseModule.kt
│   │   │   ├── ui/               # UI层
│   │   │   │   ├── activities/   # 活动
│   │   │   │   ├── fragments/    # 片段
│   │   │   │   ├── adapters/     # 适配器
│   │   │   │   └── viewmodels/   # 视图模型
│   │   │   ├── data/             # 数据层
│   │   │   │   ├── repository/   # 数据仓库
│   │   │   │   ├── model/        # 数据模型
│   │   │   │   └── local/        # 本地数据源
│   │   │   ├── business/         # 业务逻辑层
│   │   │   │   ├── usecase/      # 用例
│   │   │   │   └── service/      # 服务
│   │   │   └── utils/            # 工具类
│   │   └── res/                  # 资源文件
│   ├── src/androidTest/          # 仪器化测试
│   └── src/test/                 # 单元测试
├── libraries/                    # 模块库
│   ├── network/                  # 网络模块
│   ├── database/                 # 数据库模块
│   ├── image/                    # 图片处理模块
│   ├── settings/                 # 设置管理模块
│   └── utils/                    # 工具类模块
├── gradle.properties            # 全局配置
├── settings.gradle.kts          # 项目设置
├── build.gradle.kts             # 根构建文件
└── README.md                    # 项目文档
```

### 2. 核心配置文件

#### settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Your Awesome App"

include(":app")
include(":libraries:network")
include(":libraries:database")
include(":libraries:image")
include(":libraries:settings")
include(":libraries:utils")
```

#### build.gradle.kts (根目录)
```kotlin
plugins {
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.android.library") version "8.1.4" apply false
    id("dagger.hilt.android.plugin") version "2.48" apply false
    id("kotlin-kapt") version "1.9.10" apply false
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
```

#### gradle.properties
```properties
# Project-wide Gradle settings
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetpackCompose=true

# Kotlin
kotlin.code.style=official

# Version
appVersionName=1.0.0
appVersionCode=1

# API Configuration
apiBaseUrl=https://api.example.com
apiTimeout=30000

# Database Configuration
databaseName=your_app.db
databaseVersion=1

# Build Configuration
buildToolsVersion=34.0.0
compileSdkVersion=34
targetSdkVersion=34
minSdkVersion=21

# Signing Configuration (for release builds)
signingEnabled=false
```

#### app/build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.yourapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.yourapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://api.dev.example.com\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Image Processing
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // EhViewer Modules
    implementation(project(":libraries:network"))
    implementation(project(":libraries:database"))
    implementation(project(":libraries:image"))
    implementation(project(":libraries:settings"))
    implementation(project(":libraries:utils"))

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}
```

### 3. 核心代码模板

#### AppApplication.kt
```kotlin
package com.example.yourapp

import android.app.Application
import com.hippo.ehviewer.analytics.AnalyticsManager
import com.hippo.ehviewer.database.DatabaseManager
import com.hippo.ehviewer.network.NetworkClient
import com.hippo.ehviewer.settings.SettingsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppApplication : Application() {

    @Inject
    lateinit var networkClient: NetworkClient

    @Inject
    lateinit var databaseManager: DatabaseManager

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onCreate() {
        super.onCreate()

        // 初始化模块
        initModules()

        // 配置分析
        configureAnalytics()
    }

    private fun initModules() {
        try {
            // 网络模块已在Hilt中注入

            // 数据库模块已在Hilt中注入

            // 设置模块已在Hilt中注入

            Log.d(TAG, "All modules initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize modules", e)
        }
    }

    private fun configureAnalytics() {
        analyticsManager.setEnabled(!BuildConfig.DEBUG)
        analyticsManager.logAppStart()
    }

    companion object {
        private const val TAG = "AppApplication"
    }
}
```

#### 依赖注入配置

##### AppModule.kt
```kotlin
package com.example.yourapp.di

import android.content.Context
import com.hippo.ehviewer.analytics.AnalyticsManager
import com.hippo.ehviewer.database.DatabaseManager
import com.hippo.ehviewer.network.NetworkClient
import com.hippo.ehviewer.settings.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkClient(@ApplicationContext context: Context): NetworkClient {
        return NetworkClient.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDatabaseManager(@ApplicationContext context: Context): DatabaseManager {
        return DatabaseManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAnalyticsManager(@ApplicationContext context: Context): AnalyticsManager {
        return AnalyticsManager.getInstance(context)
    }
}
```

##### NetworkModule.kt
```kotlin
package com.example.yourapp.di

import com.example.yourapp.data.api.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
```

##### DatabaseModule.kt
```kotlin
package com.example.yourapp.di

import android.content.Context
import com.example.yourapp.data.local.AppDatabase
import com.example.yourapp.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }
}
```

### 4. 数据层模板

#### 数据模型
```kotlin
package com.example.yourapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "users")
@JsonClass(generateAdapter = true)
data class User(
    @PrimaryKey
    @Json(name = "id")
    val id: Long,

    @Json(name = "username")
    val username: String,

    @Json(name = "email")
    val email: String,

    @Json(name = "avatar_url")
    val avatarUrl: String? = null,

    @Json(name = "created_at")
    val createdAt: String
)
```

#### API服务接口
```kotlin
package com.example.yourapp.data.api

import com.example.yourapp.data.model.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<List<User>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: Long): Response<User>

    @GET("search/users")
    suspend fun searchUsers(@Query("q") query: String): Response<SearchResponse<User>>
}
```

#### 数据仓库
```kotlin
package com.example.yourapp.data.repository

import com.example.yourapp.data.api.ApiService
import com.example.yourapp.data.local.dao.UserDao
import com.example.yourapp.data.model.User
import com.hippo.ehviewer.network.NetworkClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val networkClient: NetworkClient
) {

    fun getUsers(page: Int = 1): Flow<Result<List<User>>> = flow {
        try {
            // 先尝试从本地数据库获取
            val localUsers = userDao.getUsers()
            if (localUsers.isNotEmpty()) {
                emit(Result.success(localUsers))
            }

            // 从网络获取最新数据
            val response = apiService.getUsers(page)
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    // 保存到本地数据库
                    userDao.insertUsers(users)
                    emit(Result.success(users))
                } ?: emit(Result.failure(Exception("Empty response")))
            } else {
                emit(Result.failure(Exception("API error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getUser(userId: Long): Flow<Result<User>> = flow {
        try {
            val response = apiService.getUser(userId)
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    emit(Result.success(user))
                } ?: emit(Result.failure(Exception("User not found")))
            } else {
                emit(Result.failure(Exception("API error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
```

### 5. UI层模板

#### BaseActivity
```kotlin
package com.example.yourapp.ui.base

import androidx.appcompat.app.AppCompatActivity
import com.hippo.ehviewer.analytics.AnalyticsManager
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onResume() {
        super.onResume()
        // 记录页面访问
        analyticsManager.logScreenView(this::class.java.simpleName)
    }

    protected fun showError(message: String) {
        // 统一的错误显示逻辑
        analyticsManager.logError("ui_error", message)
        // 显示错误提示
    }

    protected fun showLoading(show: Boolean) {
        // 统一的加载状态显示
    }
}
```

#### BaseFragment
```kotlin
package com.example.yourapp.ui.base

import androidx.fragment.app.Fragment
import com.hippo.ehviewer.analytics.AnalyticsManager
import javax.inject.Inject

abstract class BaseFragment : Fragment() {

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onResume() {
        super.onResume()
        // 记录页面访问
        analyticsManager.logScreenView(this::class.java.simpleName)
    }

    protected fun showError(message: String) {
        // 统一的错误显示逻辑
        analyticsManager.logError("fragment_error", message)
        // 显示错误提示
    }

    protected fun showLoading(show: Boolean) {
        // 统一的加载状态显示
    }
}
```

#### BaseViewModel
```kotlin
package com.example.yourapp.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hippo.ehviewer.analytics.AnalyticsManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseViewModel : ViewModel() {

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    protected fun launchWithErrorHandling(block: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }

    protected open fun handleError(throwable: Throwable) {
        analyticsManager.logError("viewmodel_error", throwable.message ?: "Unknown error")
    }
}
```

### 6. 业务逻辑模板

#### UseCase模板
```kotlin
package com.example.yourapp.domain.usecase

import com.example.yourapp.data.repository.UserRepository
import com.example.yourapp.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    operator fun invoke(page: Int = 1): Flow<Result<List<User>>> {
        return userRepository.getUsers(page).map { result ->
            result.map { users ->
                users.map { it.toDomainModel() }
            }
        }
    }
}
```

#### Service模板
```kotlin
package com.example.yourapp.business.service

import com.example.yourapp.data.repository.UserRepository
import com.hippo.ehviewer.network.NetworkClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserService @Inject constructor(
    private val userRepository: UserRepository,
    private val networkClient: NetworkClient
) {

    fun syncUserData() {
        // 同步用户数据的业务逻辑
    }

    fun validateUserCredentials(username: String, password: String): Boolean {
        // 验证用户凭据的业务逻辑
        return true
    }

    fun updateUserProfile(userId: Long, updates: Map<String, Any>) {
        // 更新用户资料的业务逻辑
    }
}
```

### 7. 测试模板

#### 单元测试模板
```kotlin
package com.example.yourapp.domain.usecase

import com.example.yourapp.data.repository.UserRepository
import com.example.yourapp.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class GetUsersUseCaseTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var getUsersUseCase: GetUsersUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getUsersUseCase = GetUsersUseCase(userRepository)
    }

    @Test
    fun `get users returns success result`() = runTest {
        // Given
        val users = listOf(
            User(id = 1, username = "user1", email = "user1@example.com"),
            User(id = 2, username = "user2", email = "user2@example.com")
        )
        `when`(userRepository.getUsers(1)).thenReturn(flowOf(Result.success(users)))

        // When
        val result = getUsersUseCase(1).first()

        // Then
        assert(result.isSuccess)
        assertEquals(users, result.getOrNull())
    }
}
```

#### UI测试模板
```kotlin
package com.example.yourapp.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.yourapp.R
import com.example.yourapp.ui.login.LoginActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun loginSuccess_displaysWelcomeMessage() {
        // 输入用户名
        onView(withId(R.id.usernameEditText))
            .perform(typeText("testuser"))

        // 输入密码
        onView(withId(R.id.passwordEditText))
            .perform(typeText("password123"))

        // 点击登录按钮
        onView(withId(R.id.loginButton))
            .perform(click())

        // 验证显示欢迎消息
        onView(withId(R.id.welcomeTextView))
            .check(matches(isDisplayed()))
    }
}
```

### 8. 工具类模板

#### 常用工具类
```kotlin
package com.example.yourapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

object NetworkUtils {

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.isConnected
        }
    }
}

object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun parseDate(dateString: String): Long {
        return try {
            dateFormat.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        return email.matches(emailRegex)
    }

    fun isValidPassword(password: String): Boolean {
        // 至少8位，包含字母和数字
        val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
        return password.matches(passwordRegex)
    }
}
```

### 9. 配置文件模板

#### AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.yourapp">

    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- 存储权限 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <application
        android:name=".AppApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <activity
            android:name=".ui.main.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

### 10. 文档模板

#### README.md
```markdown
# Your Awesome App

一个基于EhViewer模块库构建的现代化Android应用。

## 功能特性

- ✨ 现代化UI设计
- 🚀 高效性能
- 🔒 安全可靠
- 📱 跨设备同步
- 🎯 个性化体验

## 技术栈

- **架构**: MVVM + Repository
- **依赖注入**: Hilt
- **网络**: Retrofit + OkHttp
- **数据库**: Room
- **图片**: Glide
- **模块化**: EhViewer Module Library

## 快速开始

1. 克隆项目
```bash
git clone https://github.com/your-repo/your-awesome-app.git
cd your-awesome-app
```

2. 安装依赖
```bash
./gradlew build
```

3. 运行应用
```bash
./gradlew installDebug
```

## 项目结构

```
app/
├── src/main/java/com/example/yourapp/
│   ├── di/           # 依赖注入
│   ├── ui/           # UI层
│   ├── data/         # 数据层
│   ├── business/     # 业务逻辑
│   └── utils/        # 工具类
├── src/androidTest/  # 仪器化测试
└── src/test/         # 单元测试

libraries/            # EhViewer模块库
├── network/
├── database/
├── image/
├── settings/
└── utils/
```

## 开发规范

- [开发规范](DEVELOPMENT_GUIDE.md)
- [代码风格指南](CODE_STYLE.md)
- [API文档](API_DOCS.md)

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情
```

---

## 📋 使用建议

### 项目规模分层

**小型项目（1-2人开发）**：
- 使用核心模块：network、database、settings、utils
- 简化架构：MVVM基础模式
- 快速原型开发

**中型项目（3-5人开发）**：
- 使用完整模块：添加image、download、cache、ui
- 标准架构：MVVM + Repository + UseCase
- 模块化开发

**大型项目（5人以上开发）**：
- 使用所有模块：包含analytics、spider等高级模块
- 完整架构：Clean Architecture + 多模块
- 微服务架构

### 开发阶段建议

1. **概念验证阶段**：
   - 选择2-3个核心模块
   - 快速搭建原型
   - 验证核心功能

2. **MVP阶段**：
   - 完善基础功能
   - 添加用户界面
   - 实现核心业务流程

3. **生产就绪阶段**：
   - 添加错误处理
   - 实现数据持久化
   - 完善用户体验

4. **扩展优化阶段**：
   - 添加高级功能
   - 性能优化
   - 用户行为分析

### 最佳实践

- **从小开始**：从核心模块开始，逐步扩展
- **模块隔离**：保持模块间的松耦合
- **测试驱动**：编写单元测试和集成测试
- **文档优先**：及时更新文档和注释
- **持续重构**：定期重构和优化代码

---

**🎉 使用这个模板，快速搭建你的下一个优秀应用！**
