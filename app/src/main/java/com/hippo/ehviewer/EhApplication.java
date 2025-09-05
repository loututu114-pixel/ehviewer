/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Debug;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.LruCache;

import com.hippo.Native;
//import com.gu.toolargetool.TooLargeTool;
import com.hippo.a7zip.A7Zip;
import com.hippo.beerbelly.SimpleDiskCache;
import com.hippo.conaco.Conaco;
import com.hippo.content.RecordingApplication;
import com.hippo.ehviewer.client.BandwidthManager;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhEngine;
import com.hippo.ehviewer.client.EhHosts;
import com.hippo.ehviewer.client.X5WebViewManager;
import com.hippo.ehviewer.client.MemoryManager;
import com.hippo.ehviewer.util.AppOptimizationManager;
import com.hippo.ehviewer.util.SystemErrorHandler;
import com.hippo.ehviewer.util.LogMonitor;
import com.hippo.ehviewer.util.SystemMonitor;
import com.hippo.ehviewer.util.StartupLogger;
import com.hippo.ehviewer.util.UserEnvironmentDetector;
import com.hippo.ehviewer.SystemCompatibilityManager;
import com.hippo.ehviewer.client.data.EhNewsDetail;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.userTag.UserTagList;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.lib.image.Image;
import com.hippo.network.EhSSLSocketFactory;
import com.hippo.network.EhSSLSocketFactoryLowSDK;
import com.hippo.network.EhX509TrustManager;
import com.hippo.network.StatusCodeException;
import com.hippo.text.Html;
import com.hippo.unifile.UniFile;
import com.hippo.util.AppHelper;
import com.hippo.util.BitmapUtils;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.IoThreadPoolExecutor;
import com.hippo.util.ReadableTime;
import com.hippo.lib.yorozuya.FileUtils;
import com.hippo.lib.yorozuya.IntIdGenerator;
import com.hippo.lib.yorozuya.OSUtils;
import com.hippo.lib.yorozuya.SimpleHandler;
import com.hippo.ehviewer.analytics.ChannelTracker;

import org.conscrypt.Conscrypt;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class EhApplication extends RecordingApplication {

    private static final String TAG = EhApplication.class.getSimpleName();
    private static final String KEY_GLOBAL_STUFF_NEXT_ID = "global_stuff_next_id";

    public static final boolean BETA = false;

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

    private static final boolean DEBUG_CONACO = false;
    private static final boolean DEBUG_PRINT_NATIVE_MEMORY = false;
    private static final boolean DEBUG_PRINT_IMAGE_COUNT = false;
    private static final long DEBUG_PRINT_INTERVAL = 3000L;

    private static EhApplication instance;

    private final IntIdGenerator mIdGenerator = new IntIdGenerator();
    private final HashMap<Integer, Object> mGlobalStuffMap = new HashMap<>();

    private final HashMap<String, Object> mTempCacheMap = new HashMap<>();

    private EhCookieStore mEhCookieStore;
    private EhClient mEhClient;
    private EhProxySelector mEhProxySelector;
    private OkHttpClient mOkHttpClient;
    private OkHttpClient mImageOkHttpClient;
    private Cache mOkHttpCache;
    private ImageBitmapHelper mImageBitmapHelper;
    private Conaco<Image> mConaco;
    private LruCache<Long, GalleryDetail> mGalleryDetailCache;
    private SimpleDiskCache mSpiderInfoCache;
    private DownloadManager mDownloadManager;
    private Hosts mHosts;
    private FavouriteStatusRouter mFavouriteStatusRouter;
    @Nullable
    private UserTagList userTagList;
    @Nullable
    private EhNewsDetail ehNewsDetail;

    private final List<Activity> mActivityList = new ArrayList<>();

    private final List<String> torrentList = new ArrayList<>();

    private boolean initialized = false;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public static EhApplication getInstance() {
        return instance;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onCreate() {
        instance = this;

        // 初始化启动日志记录器
        StartupLogger startupLogger = StartupLogger.getInstance(this);
        startupLogger.logStartupStepStart("Application.onCreate");

        // 尽早设置系统属性以避免ColorX和其他系统服务错误
        setupEarlySystemProperties();

        startupLogger.logStartupStep("ExceptionHandler", "Setting up uncaught exception handler");
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        // 设置全局错误处理器来捕获顽固的系统错误
        setupGlobalErrorHandler();

        Thread.UncaughtExceptionHandler enhancedHandler = (t, e) -> {
            try {
                // Always save crash file if onCreate() is not done
                if (!initialized || Settings.getSaveCrashLog()) {
                    Crash.saveCrashLog(instance, e);
                }

                // Log additional context for system service errors
                if (e != null && e.getMessage() != null) {
                    if (e.getMessage().contains("RemoteFillService") ||
                        e.getMessage().contains("Autofill") ||
                        e.getMessage().contains("SystemService")) {
                        Log.w(TAG, "System service error detected: " + e.getMessage());
                    }
                }
            } catch (Throwable ignored) {
            }

            if (defaultExceptionHandler != null) {
                defaultExceptionHandler.uncaughtException(t, e);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(enhancedHandler);

        super.onCreate();
//        if(BuildConfig.DEBUG){
//            TooLargeTool.startLogging(this);
//        }

        startupLogger.logStartupStep("Initialization", "Starting core components initialization");

        long initStartTime = System.currentTimeMillis();
        GetText.initialize(this);
        startupLogger.logStartupStep("GetText", "Initialized in " + (System.currentTimeMillis() - initStartTime) + "ms");

        StatusCodeException.initialize(this);
        startupLogger.logStartupStep("StatusCodeException", "Initialized");

        Settings.initialize(this);
        startupLogger.logStartupStep("Settings", "Initialized");

        ReadableTime.initialize(this);
        startupLogger.logStartupStep("ReadableTime", "Initialized");

        Html.initialize(this);
        startupLogger.logStartupStep("Html", "Initialized");

        AppConfig.initialize(this);
        startupLogger.logStartupStep("AppConfig", "Initialized");

        SpiderDen.initialize(this);
        startupLogger.logStartupStep("SpiderDen", "Initialized");

        EhDB.initialize(this);
        startupLogger.logStartupStep("EhDB", "Initialized");

        EhEngine.initialize();
        startupLogger.logStartupStep("EhEngine", "Initialized");

        BitmapUtils.initialize(this);
        startupLogger.logStartupStep("BitmapUtils", "Initialized");

        Image.initialize(this);
        startupLogger.logStartupStep("Image", "Initialized");

        Native.initialize();
        startupLogger.logStartupStep("Native", "Initialized");

        // 初始化腾讯X5浏览器
        X5WebViewManager.getInstance().initX5(this);

        // 初始化内存管理器
        MemoryManager.getInstance(this);

        // 初始化系统兼容性管理器
        SystemCompatibilityManager.getInstance().initialize(this);

        // 处理Google Play服务兼容性
        handleGooglePlayServicesCompatibility();

        // 处理系统服务兼容性
        handleSystemServiceCompatibility();
        
        // 初始化应用优化管理器
        AppOptimizationManager.getInstance(this).initializeOnAppCreate(this);

        // 初始化系统错误处理器
        SystemErrorHandler.getInstance(this);

        // 初始化系统监控器
        SystemMonitor.getInstance(this).startMonitoring();

        // 启动日志监控器
        LogMonitor.getInstance(this).startMonitoring();

        // 实际作用不确定，但是与64位应用有冲突
//        A7Zip.loadLibrary(A7ZipExtractLite.LIBRARY, libname -> ReLinker.loadLibrary(EhApplication.this, libname));
        // 64位适配
        startupLogger.logStartupStep("A7Zip", "Initializing A7Zip library");
        A7Zip.initialize(this);

        if (EhDB.needMerge()) {
            startupLogger.logStartupStep("EhDB", "Merging old database");
            EhDB.mergeOldDB(this);
        }

        if (Settings.getEnableAnalytics()) {
            startupLogger.logStartupStep("Analytics", "Starting analytics");
            Analytics.start(this);
        }

        // 初始化渠道统计SDK
        startupLogger.logStartupStep("ChannelTracker", "Initializing channel tracker");
        try {
            ChannelTracker.initialize(this, BuildConfig.CHANNEL_CODE);
        } catch (Exception e) {
            startupLogger.logWarning("Failed to initialize ChannelTracker", e);
            Log.w(TAG, "Failed to initialize ChannelTracker", e);
        }

        // Do io tasks in new thread
        startupLogger.logStartupStep("IOTasks", "Starting background I/O tasks");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                // Check no media file
                try {
                    UniFile downloadLocation = Settings.getDownloadLocation();
                    if (Settings.getMediaScan()) {
                        CommonOperations.removeNoMediaFile(downloadLocation);
                    } else {
                        CommonOperations.ensureNoMediaFile(downloadLocation);
                    }
                    startupLogger.logStartupStep("MediaScan", "Media scan configuration applied");
                } catch (Throwable t) {
                    startupLogger.logError("Media scan configuration failed", t);
                    ExceptionUtils.throwIfFatal(t);
                }

                // Clear temp files
                try {
                    clearTempDir();
                    startupLogger.logStartupStep("TempCleanup", "Temporary files cleared");
                } catch (Throwable t) {
                    startupLogger.logError("Temp cleanup failed", t);
                    ExceptionUtils.throwIfFatal(t);
                }

                return null;
            }
        }.executeOnExecutor(IoThreadPoolExecutor.getInstance());

        // Check app update
        startupLogger.logStartupStep("UpdateCheck", "Checking for updates");
        update();

        // Update version code
        startupLogger.logStartupStep("VersionUpdate", "Updating version code");
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            Settings.putVersionCode(pi.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            startupLogger.logWarning("Failed to get package info", e);
        }

        mIdGenerator.setNextId(Settings.getInt(KEY_GLOBAL_STUFF_NEXT_ID, 0));

        if (DEBUG_PRINT_NATIVE_MEMORY || DEBUG_PRINT_IMAGE_COUNT) {
            debugPrint();
        }

        // 初始化用户环境检测
        startupLogger.logStartupStep("UserEnvironment", "Initializing user environment detection");
        initializeUserEnvironmentDetection();

        // 验证关键资源
        startupLogger.logStartupStep("ResourceValidation", "Validating critical resources");
        validateCriticalResources();

        // 再次确保系统属性设置（以防被覆盖）
        ensureSystemProperties();

        initialized = true;

        startupLogger.logStartupStepEnd("Application.onCreate", startupLogger.getStartupDuration());
        startupLogger.logStartupComplete();
    }

    /**
     * 初始化用户环境检测
     */
    private void initializeUserEnvironmentDetection() {
        Log.d(TAG, "Initializing user environment detection...");
        
        UserEnvironmentDetector detector = UserEnvironmentDetector.getInstance(this);
        detector.startDetection(new UserEnvironmentDetector.DetectionCallback() {
            @Override
            public void onDetectionSuccess(UserEnvironmentDetector.EnvironmentInfo environmentInfo) {
                Log.i(TAG, "User environment detected successfully: " + environmentInfo.toString());
                
                // 可以在这里根据环境信息调整应用行为
                applyEnvironmentSettings(environmentInfo);
            }
            
            @Override
            public void onDetectionFailed(String error) {
                Log.w(TAG, "User environment detection failed: " + error);
                // 检测失败时使用默认设置
                UserEnvironmentDetector.EnvironmentInfo defaultInfo = 
                    UserEnvironmentDetector.getInstance(EhApplication.this).getEnvironmentInfo();
                applyEnvironmentSettings(defaultInfo);
            }
            
            @Override
            public void onDetectionProgress(String message) {
                Log.d(TAG, "Environment detection progress: " + message);
            }
        });
    }
    
    /**
     * 根据环境信息应用设置
     */
    private void applyEnvironmentSettings(UserEnvironmentDetector.EnvironmentInfo environmentInfo) {
        // 这里可以根据环境信息调整全局设置
        // 比如设置默认搜索引擎、首页等
        Log.d(TAG, "Applied environment settings for region: " + environmentInfo.regionType + 
              ", network: " + environmentInfo.networkEnvironment);
    }

    private void clearTempDir() {
        File dir = AppConfig.getTempDir();
        if (null != dir) {
            FileUtils.deleteContent(dir);
        }
        dir = AppConfig.getExternalTempDir();
        if (null != dir) {
            FileUtils.deleteContent(dir);
        }

        // Add .nomedia to external temp dir
        CommonOperations.ensureNoMediaFile(UniFile.fromFile(AppConfig.getExternalTempDir()));
    }

    public EhCookieStore getmEhCookieStore() {
        return mEhCookieStore;
    }

    private void update() {
        int version = Settings.getVersionCode();
        if (version < 52) {
            Settings.putGuideGallery(true);
        }
    }

    public void clearMemoryCache() {
        if (null != mConaco) {
            mConaco.getBeerBelly().clearMemory();
        }
        if (null != mGalleryDetailCache) {
            mGalleryDetailCache.evictAll();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            clearMemoryCache();
        }
    }

    private void debugPrint() {
        new Runnable() {
            @Override
            public void run() {
                if (DEBUG_PRINT_NATIVE_MEMORY) {
                    Log.i(TAG, "Native memory: " + FileUtils.humanReadableByteCount(
                            Debug.getNativeHeapAllocatedSize(), false));
                }
                SimpleHandler.getInstance().postDelayed(this, DEBUG_PRINT_INTERVAL);
            }
        }.run();
    }

    public int putGlobalStuff(@NonNull Object o) {
        int id = mIdGenerator.nextId();
        mGlobalStuffMap.put(id, o);
        Settings.putInt(KEY_GLOBAL_STUFF_NEXT_ID, mIdGenerator.nextId());
        return id;
    }

    public boolean containGlobalStuff(int id) {
        return mGlobalStuffMap.containsKey(id);
    }

    public Object getGlobalStuff(int id) {
        return mGlobalStuffMap.get(id);
    }

    public Object removeGlobalStuff(int id) {
        return mGlobalStuffMap.remove(id);
    }

    public String putTempCache(@NonNull String key,@NonNull Object o) {
        mTempCacheMap.put(key, o);
        return key;
    }

    public boolean containTempCache(@NonNull String key) {
        return mTempCacheMap.containsKey(key);
    }

    public Object getTempCache(@NonNull String key) {
        return mTempCacheMap.get(key);
    }

    public Object removeTempCache(@NonNull String key) {
        return mTempCacheMap.remove(key);
    }

    public void removeGlobalStuff(Object o) {
        mGlobalStuffMap.values().removeAll(Collections.singleton(o));
    }

    public static EhCookieStore getEhCookieStore(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhCookieStore == null) {
            application.mEhCookieStore = new EhCookieStore(context);
        }
        return application.mEhCookieStore;
    }

    @NonNull
    public static EhClient getEhClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhClient == null) {
            application.mEhClient = new EhClient(application);
        }
        return application.mEhClient;
    }

    @NonNull
    public static EhProxySelector getEhProxySelector(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhProxySelector == null) {
            application.mEhProxySelector = new EhProxySelector();
        }
        return application.mEhProxySelector;
    }

    @NonNull
    public static OkHttpClient getOkHttpClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mOkHttpClient == null) {
            // HTTP连接优化配置 - 自适应带宽管理
            BandwidthManager bandwidthManager = BandwidthManager.getInstance(application);
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(bandwidthManager.getRecommendedMaxConcurrentRequests() * 2);
            dispatcher.setMaxRequestsPerHost(bandwidthManager.getRecommendedMaxConcurrentRequests());

            // 连接池优化
            ConnectionPool connectionPool = new ConnectionPool(
                10,     // 最大空闲连接数
                5,      // 保持连接时间（分钟）
                TimeUnit.MINUTES
            );

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .callTimeout(30, TimeUnit.SECONDS)  // 总超时时间
                    .retryOnConnectionFailure(true)     // 连接失败自动重试
                    .cookieJar(getEhCookieStore(application))
                    .cache(getOkHttpCache(application))
                    .dispatcher(dispatcher)
                    .connectionPool(connectionPool)
                    .dns(new EhHosts(application))
                    // Keep-Alive优化
                    .pingInterval(30, TimeUnit.SECONDS)  // 定期ping保持连接
                    .addNetworkInterceptor(sprocket -> {
                        try {
                            return sprocket.proceed(sprocket.request());
                        } catch (NullPointerException e) {
                            throw new NullPointerException(e.getMessage());
                        }
                    })
                    .addNetworkInterceptor(chain -> {
                        Response response = chain.proceed(chain.request());
                        // 同步Cookie到WebView
                        if (response.headers("Set-Cookie") != null) {
                            CookieManager cookieManager = CookieManager.getInstance();
                            String url =chain.request().url().toString();
                            for (String header : response.headers("Set-Cookie")) {
                                cookieManager.setCookie(url, header);
                            }
                            cookieManager.flush();
                        }
                        return response;
                    })
                    .proxySelector(getEhProxySelector(application));
            if (Settings.getDF() && AppHelper.checkVPN(context)) {
                if (Build.VERSION.SDK_INT < 29) {
                    Security.insertProviderAt(Conscrypt.newProvider(), 1);
                    builder.connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS));
                    try {
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                                TrustManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init((KeyStore) null);
                        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
                        }
                        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
//                        X509TrustManager tm = Conscrypt.getDefaultX509TrustManager();
                        SSLContext sslContext = SSLContext.getInstance("TLS", "Conscrypt");
                        sslContext.init(null, trustManagers, null);
                        builder.sslSocketFactory(new EhSSLSocketFactoryLowSDK(sslContext.getSocketFactory()), trustManager);
                    } catch (Exception e) {
                        e.printStackTrace();
                        builder.sslSocketFactory(new EhSSLSocketFactoryLowSDK(new EhSSLSocketFactory()), new EhX509TrustManager());
                    }
                } else {
                    try {
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                                TrustManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init((KeyStore) null);
                        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
                        }
                        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
                        builder.sslSocketFactory(new EhSSLSocketFactory(), trustManager);
                    } catch (Exception e) {
                        e.printStackTrace();
                        builder.sslSocketFactory(new EhSSLSocketFactory(), new EhX509TrustManager());
                    }
                }
            }
            application.mOkHttpClient = builder.build();
        }

        return application.mOkHttpClient;
    }

    @NonNull
    public static OkHttpClient getImageOkHttpClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mImageOkHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .callTimeout(20, TimeUnit.SECONDS)
                    .cookieJar(getEhCookieStore(application))
                    .cache(getOkHttpCache(application))
//                    .hostnameVerifier((hostname, session) -> true)
                    .dns(new EhHosts(application))
                    .addNetworkInterceptor(sprocket -> {
                        try {
                            return sprocket.proceed(sprocket.request());
                        } catch (NullPointerException e) {
                            throw new NullPointerException(e.getMessage());
                        }
                    })
                    .proxySelector(getEhProxySelector(application));
            if (Settings.getDF() && AppHelper.checkVPN(context)) {
                if (Build.VERSION.SDK_INT < 29) {
                    Security.insertProviderAt(Conscrypt.newProvider(), 1);
                    builder.connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS));
                    try {
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                                TrustManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init((KeyStore) null);
                        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
                        }
                        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
                        SSLContext sslContext = SSLContext.getInstance("TLS", "Conscrypt");
                        sslContext.init(null, trustManagers, null);
                        builder.sslSocketFactory(new EhSSLSocketFactoryLowSDK(sslContext.getSocketFactory()), trustManager);
                    } catch (Exception e) {
                        e.printStackTrace();
                        builder.sslSocketFactory(new EhSSLSocketFactoryLowSDK(new EhSSLSocketFactory()), new EhX509TrustManager());
                    }
                } else {
                    try {
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                                TrustManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init((KeyStore) null);
                        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
                        }
                        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
                        builder.sslSocketFactory(new EhSSLSocketFactory(), trustManager);
                    } catch (Exception e) {
                        e.printStackTrace();
                        builder.sslSocketFactory(new EhSSLSocketFactory(), new EhX509TrustManager());
                    }
                }
            }
            application.mImageOkHttpClient = builder.build();
        }

        return application.mImageOkHttpClient;
    }

    @NonNull
    public static ImageBitmapHelper getImageBitmapHelper(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mImageBitmapHelper == null) {
            application.mImageBitmapHelper = new ImageBitmapHelper();
        }
        return application.mImageBitmapHelper;
    }

    private static int getMemoryCacheMaxSize() {
        return Math.min(20 * 1024 * 1024, (int) OSUtils.getAppMaxMemory());
    }

    @NonNull
    public static Conaco<Image> getConaco(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mConaco == null) {
            Conaco.Builder<Image> builder = new Conaco.Builder<>();
            builder.hasMemoryCache = true;
            builder.memoryCacheMaxSize = getMemoryCacheMaxSize();
            builder.hasDiskCache = true;
            builder.diskCacheDir = new File(context.getCacheDir(), "thumb");
            builder.diskCacheMaxSize = 320 * 1024 * 1024; // 320MB
            builder.okHttpClient = getOkHttpClient(context);
//            builder.okHttpClient = getImageOkHttpClient(context);
            builder.objectHelper = getImageBitmapHelper(context);
            builder.debug = DEBUG_CONACO;
            application.mConaco = builder.build();
        }
        return application.mConaco;
    }


    @NonNull
    public static LruCache<Long, GalleryDetail> getGalleryDetailCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mGalleryDetailCache == null) {
            // Max size 25, 3 min timeout
            application.mGalleryDetailCache = new LruCache<>(25);
            getFavouriteStatusRouter().addListener((gid, slot) -> {
                GalleryDetail gd = application.mGalleryDetailCache.get(gid);
                if (gd != null) {
                    gd.favoriteSlot = slot;
                }
            });
        }
        return application.mGalleryDetailCache;
    }

    @NonNull
    public static SimpleDiskCache getSpiderInfoCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (null == application.mSpiderInfoCache) {
            application.mSpiderInfoCache = new SimpleDiskCache(
                    new File(context.getCacheDir(), "spider_info"), 5 * 1024 * 1024); // 5M
        }
        return application.mSpiderInfoCache;
    }

    @NonNull
    public static DownloadManager getDownloadManager() {
        return getDownloadManager(instance);
    }

    @NonNull
    public static DownloadManager getDownloadManager(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mDownloadManager == null) {
            application.mDownloadManager = new DownloadManager(application);
        }
        return application.mDownloadManager;
    }

    @NonNull
    public static Hosts getHosts(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mHosts == null) {
            application.mHosts = new Hosts(application, "hosts.db");
        }
        return application.mHosts;
    }

    @NonNull
    public static FavouriteStatusRouter getFavouriteStatusRouter() {
        return getFavouriteStatusRouter(getInstance());
    }

    @NonNull
    public static FavouriteStatusRouter getFavouriteStatusRouter(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mFavouriteStatusRouter == null) {
            application.mFavouriteStatusRouter = new FavouriteStatusRouter();
        }
        return application.mFavouriteStatusRouter;
    }

    @NonNull
    public static String getDeveloperEmail() {
        return "loututu114@gmail.com";
    }

    public void registerActivity(Activity activity) {
        mActivityList.add(activity);
    }

    public void unregisterActivity(Activity activity) {
        mActivityList.remove(activity);
    }

    @Nullable
    public Activity getTopActivity() {
        if (!mActivityList.isEmpty()) {
            return mActivityList.get(mActivityList.size() - 1);
        } else {
            return null;
        }
    }

    @NonNull
    public static Cache getOkHttpCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mOkHttpCache == null) {
            application.mOkHttpCache = new Cache(new File(application.getCacheDir(), "http_cache"), 50L * 1024L * 1024L);
        }
        return application.mOkHttpCache;
    }

    // Avoid crash on some "energy saving" devices
    @Override
    public ComponentName startService(Intent service) {
        try {
            return super.startService(service);
        } catch (Throwable t) {
            ExceptionUtils.throwIfFatal(t);
            return null;
        }
    }

    // Avoid crash on some "energy saving" devices
    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        try {
            return super.bindService(service, conn, flags);
        } catch (Throwable t) {
            ExceptionUtils.throwIfFatal(t);
            return false;
        }
    }

    // Avoid crash on some "energy saving" devices
    @Override
    public void unbindService(ServiceConnection conn) {
        try {
            super.unbindService(conn);
        } catch (Throwable t) {
            ExceptionUtils.throwIfFatal(t);
        }
    }

    public static boolean addDownloadTorrent(@NonNull Context context, String url) {
        EhApplication application = ((EhApplication) context.getApplicationContext());

        if (application.torrentList.contains(url)) {
            return false;
        }

        application.torrentList.add(url);
        return true;
    }

    public static void removeDownloadTorrent(@NonNull Context context, String url) {
        EhApplication application = ((EhApplication) context.getApplicationContext());

        application.torrentList.remove(url);
    }

    /**
     * 将用户订阅标签列表存入内存缓存
     *
     */
    public static void saveUserTagList(@NonNull Context context, UserTagList userTagList) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        application.userTagList = userTagList;
    }

    /**
     * 从内存缓存中获取用户订阅标签列表
     *
     */
    public static UserTagList getUserTagList(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.userTagList;
    }

    public void showEventPane(String html){
        if (!Settings.getShowEhEvents()){
            return;
        }
        if (html==null){
            return;
        }
        Activity activity = getTopActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setMessage(Html.fromHtml(html))
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                dialog.setOnShowListener(d -> {
                    final View messageView = dialog.findViewById(android.R.id.message);
                    if (messageView instanceof TextView) {
                        ((TextView) messageView).setMovementMethod(LinkMovementMethod.getInstance());
                    }
                });
                try {
                    dialog.show();
                } catch (Throwable t) {
                    // ignore
                }
            });
        }
    }

    /**
     * 显示eh事件
     *
     */
    public void showEventPane(EhNewsDetail result) {
        ehNewsDetail = result;
        String html = result.getEventPane();
        showEventPane(html);
    }

    @Nullable
    public EhNewsDetail getEhNewsDetail(){
        return ehNewsDetail;
    }

    public static ExecutorService getExecutorService(@NonNull Context context){
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return  application.executorService;
    }

    /**
     * 处理Google Play服务兼容性问题
     */
    private void handleGooglePlayServicesCompatibility() {
        try {
            // 检查Google Play服务是否可用
            boolean isGooglePlayServicesAvailable = checkGooglePlayServicesAvailability();

            if (!isGooglePlayServicesAvailable) {
                Log.w(TAG, "Google Play Services not available, using fallback mode");
                // 在这里可以设置一些降级方案
                Settings.putGooglePlayServicesFallback(true);
            } else {
                Settings.putGooglePlayServicesFallback(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Google Play Services compatibility", e);
        }
    }

    /**
     * 处理系统服务兼容性问题
     */
    private void handleSystemServiceCompatibility() {
        try {
            // 检查系统服务是否可用，避免系统服务错误
            checkSystemServiceAvailability();

            // 处理OPPO/ColorOS特定配置
            handleOppoColorOsCompatibility();

            // 处理调度助手兼容性
            handleSchedAssistCompatibility();

            // 处理图形渲染兼容性
            handleGraphicsCompatibility();

        } catch (Exception e) {
            Log.e(TAG, "Error handling system service compatibility", e);
        }
    }

    /**
     * 检查系统服务可用性
     */
    private void checkSystemServiceAvailability() {
        try {
            // 检查天气服务
            if (getSystemService(Context.CONNECTIVITY_SERVICE) == null) {
                Log.w(TAG, "Connectivity service not available");
            }

            // 检查NFC服务
            if (getSystemService(Context.NFC_SERVICE) == null) {
                Log.w(TAG, "NFC service not available");
            }

            // 检查电池服务
            if (getSystemService(Context.BATTERY_SERVICE) == null) {
                Log.w(TAG, "Battery service not available");
            }

        } catch (Exception e) {
            Log.w(TAG, "Error checking system services", e);
        }
    }

    /**
     * 处理OPPO/ColorOS兼容性
     */
    private void handleOppoColorOsCompatibility() {
        try {
            // 检查是否为OPPO/ColorOS设备
            String manufacturer = android.os.Build.MANUFACTURER;
            String brand = android.os.Build.BRAND;

            if ("OPPO".equalsIgnoreCase(manufacturer) || "ColorOS".equalsIgnoreCase(brand)) {
                Log.i(TAG, "OPPO/ColorOS device detected, applying compatibility fixes");

                // 设置多媒体白名单
                Settings.putOppoMultimediaWhitelist(true);

                // 处理天气服务兼容性
                handleWeatherServiceCompatibility();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error handling OPPO/ColorOS compatibility", e);
        }
    }

    /**
     * 处理天气服务兼容性
     */
    private void handleWeatherServiceCompatibility() {
        try {
            // 检查天气服务权限
            PackageManager pm = getPackageManager();
            if (pm.checkPermission("com.coloros.weather.service.PERMISSION", getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Weather service permission not granted");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking weather service compatibility", e);
        }
    }

    /**
     * 处理调度助手兼容性
     */
    private void handleSchedAssistCompatibility() {
        try {
            // 检查是否为OPPO/ColorOS设备
            String manufacturer = android.os.Build.MANUFACTURER;
            String brand = android.os.Build.BRAND;

            if ("OPPO".equalsIgnoreCase(manufacturer) || "ColorOS".equalsIgnoreCase(brand)) {
                Log.i(TAG, "OPPO/ColorOS device detected, configuring SchedAssist compatibility");

                // 禁用调度助手以避免权限错误
                System.setProperty("oplus.schedassist.enabled", "false");
                System.setProperty("coloros.schedassist.enabled", "false");

                // 设置系统属性避免权限检查
                try {
                    @SuppressLint("PrivateApi")
                    Class<?> systemProperties = Class.forName("android.os.SystemProperties");
                    systemProperties.getMethod("set", String.class, String.class)
                            .invoke(null, "oplus.schedassist.enabled", "false");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to set system property for SchedAssist", e);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error handling SchedAssist compatibility", e);
        }
    }

    /**
     * 处理图形渲染兼容性
     */
    private void handleGraphicsCompatibility() {
        try {
            // 设置OpenGL兼容性
            System.setProperty("debug.hwui.renderer", "skiavk");
            System.setProperty("debug.hwui.use_vulkan", "false");

            // 禁用ColorX以避免加载错误
            System.setProperty("oplus.colorx.enabled", "false");
            System.setProperty("coloros.colorx.enabled", "false");

            Log.i(TAG, "Graphics compatibility configured");
        } catch (Exception e) {
            Log.w(TAG, "Error handling graphics compatibility", e);
        }
    }

    /**
     * 尽早设置系统属性
     */
    private void setupEarlySystemProperties() {
        try {
            // 强制禁用ColorX以避免加载错误
            System.setProperty("oplus.colorx.enabled", "false");
            System.setProperty("coloros.colorx.enabled", "false");
            System.setProperty("persist.sys.colorx.enable", "false");
            System.setProperty("ro.oppo.colorx.enable", "false");

            // 禁用调度助手
            System.setProperty("oplus.schedassist.enabled", "false");
            System.setProperty("coloros.schedassist.enabled", "false");

            // 设置OpenGL兼容性
            System.setProperty("debug.hwui.renderer", "skiavk");
            System.setProperty("debug.hwui.use_vulkan", "false");
            System.setProperty("debug.hwui.disable_vulkan", "true");

            // 禁用硬件加速的一些特性
            System.setProperty("persist.sys.ui.hw", "false");
            System.setProperty("debug.egl.force_msaa", "false");

            Log.i(TAG, "Early system properties configured");
        } catch (Exception e) {
            Log.w(TAG, "Error setting early system properties", e);
        }
    }

    /**
     * 确保系统属性设置
     */
    private void ensureSystemProperties() {
        try {
            // 再次确保关键属性设置
            setupEarlySystemProperties();

            // 额外的兼容性设置
            System.setProperty("persist.sys.ui.hw", "false");
            System.setProperty("debug.egl.swapinterval", "1");

            Log.i(TAG, "System properties ensured");
        } catch (Exception e) {
            Log.w(TAG, "Error ensuring system properties", e);
        }
    }

    /**
     * 设置全局错误处理器
     */
    private void setupGlobalErrorHandler() {
        try {
            // 设置默认异常处理器来捕获顽固的系统错误
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                // 处理特定的顽固错误
                if (throwable != null && throwable.getMessage() != null) {
                    String message = throwable.getMessage();

                    // 处理资源ID错误
                    if (message.contains("No package ID") && message.contains("found for resource ID")) {
                        Log.w(TAG, "Resource ID error caught and suppressed: " + message);
                        return; // 不让应用崩溃
                    }

                    // 处理ColorX加载错误
                    if (message.contains("ColorX_Check") || message.contains("libcolorx-loader")) {
                        Log.w(TAG, "ColorX loader error caught and suppressed: " + message);
                        return; // 不让应用崩溃
                    }

                    // 处理Ripple动画错误
                    if (message.contains("RippleDrawable") || message.contains("mDensity")) {
                        Log.w(TAG, "Ripple animation error caught and suppressed: " + message);
                        return; // 不让应用崩溃
                    }

                    // 处理OpenGL渲染错误
                    if (message.contains("swap behavior") || message.contains("OpenGL")) {
                        Log.w(TAG, "OpenGL rendering error caught and suppressed: " + message);
                        return; // 不让应用崩溃
                    }
                }

                // 对于其他错误，使用默认处理器
                if (defaultExceptionHandler != null) {
                    defaultExceptionHandler.uncaughtException(thread, throwable);
                }
            });

            Log.i(TAG, "Global error handler configured");
        } catch (Exception e) {
            Log.w(TAG, "Error setting up global error handler", e);
        }
    }

    /**
     * 验证关键资源
     */
    private void validateCriticalResources() {
        try {
            // 验证关键资源ID
            int[] criticalResources = {
                R.string.app_name,
                R.mipmap.ic_launcher,
                R.layout.activity_main,
                R.id.search_category_table
            };

            for (int resId : criticalResources) {
                try {
                    getResources().getResourceName(resId);
                } catch (Exception e) {
                    Log.e(TAG, "Critical resource missing: " + Integer.toHexString(resId), e);
                }
            }

            // 特殊处理资源ID 0x6a0b000f问题
            handleResourceId6aIssue();

            Log.i(TAG, "Critical resources validation completed");
        } catch (Exception e) {
            Log.w(TAG, "Error validating critical resources", e);
        }
    }

    /**
     * 处理资源ID 0x6a0b000f的问题
     */
    private void handleResourceId6aIssue() {
        try {
            // 尝试清理资源缓存
            getResources().flushLayoutCache();

            // 验证资源表的一致性
            android.content.res.Resources res = getResources();
            String packageName = getPackageName();

            // 检查包ID映射
            try {
                int appNameId = res.getIdentifier("app_name", "string", packageName);
                if (appNameId != 0) {
                    String appName = res.getString(appNameId);
                    Log.d(TAG, "App name resource validated: " + appName);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error validating app_name resource", e);
            }

            // 特殊处理：设置资源包ID映射
            try {
                // 强制重新加载资源配置
                android.content.res.Configuration config = res.getConfiguration();
                res.updateConfiguration(config, res.getDisplayMetrics());

                // 验证关键资源是否可访问
                int[] criticalIds = {
                    android.R.id.content,
                    android.R.id.message,
                    android.R.layout.activity_list_item
                };

                for (int id : criticalIds) {
                    try {
                        res.getResourceName(id);
                    } catch (Exception e) {
                        Log.d(TAG, "System resource validation: " + Integer.toHexString(id));
                    }
                }

            } catch (Exception e) {
                Log.w(TAG, "Error in resource configuration update", e);
            }

            Log.i(TAG, "Resource ID 6a issue handled");
        } catch (Exception e) {
            Log.w(TAG, "Error handling resource ID 6a issue", e);
        }
    }

    /**
     * 检查Google Play服务是否可用
     */
    private boolean checkGooglePlayServicesAvailability() {
        try {
            // 尝试获取Google Play服务的包信息
            PackageManager pm = getPackageManager();
            pm.getPackageInfo("com.google.android.gms", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Google Play Services package not found");
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error checking Google Play Services", e);
            return false;
        }
    }

}

