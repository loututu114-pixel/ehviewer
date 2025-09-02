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

package com.hippo.ehviewer.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.callBack.ImageChangeCallBack;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhTagDatabase;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.NetworkDetector;
import com.hippo.ehviewer.client.SearchEngineManager;
import com.hippo.ehviewer.client.EhUrlOpener;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser;
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser;
import com.hippo.ehviewer.ui.main.UserImageChange;
import com.hippo.ehviewer.ui.scene.AnalyticsScene;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.ehviewer.ui.scene.CookieSignInScene;
import com.hippo.ehviewer.ui.scene.download.DownloadLabelsScene;
import com.hippo.ehviewer.ui.scene.download.DownloadsScene;
import com.hippo.ehviewer.ui.scene.gallery.list.FavoritesScene;
import com.hippo.ehviewer.ui.scene.GalleryCommentsScene;
import com.hippo.ehviewer.ui.scene.gallery.detail.GalleryDetailScene;
import com.hippo.ehviewer.ui.scene.GalleryInfoScene;
import com.hippo.ehviewer.ui.scene.gallery.list.GalleryListScene;
import com.hippo.ehviewer.ui.scene.GalleryPreviewsScene;
import com.hippo.ehviewer.ui.scene.gallery.list.SubscriptionsScene;
import com.hippo.ehviewer.ui.scene.topList.EhTopListScene;

import com.hippo.ehviewer.ui.scene.ProgressScene;
import com.hippo.ehviewer.ui.scene.gallery.list.QuickSearchScene;
import com.hippo.ehviewer.ui.scene.SecurityScene;
import com.hippo.ehviewer.ui.scene.SelectSiteScene;
import com.hippo.ehviewer.ui.scene.SignInScene;
import com.hippo.ehviewer.ui.scene.SolidScene;
import com.hippo.ehviewer.ui.scene.WarningScene;
import com.hippo.ehviewer.ui.scene.WebViewSignInScene;
import com.hippo.ehviewer.ui.splash.SplashActivity;
// import com.hippo.ehviewer.updater.AppUpdater; // 移除强制升级功能
import com.hippo.ehviewer.widget.EhDrawerLayout;
import com.hippo.ehviewer.widget.LimitsCountView;
import com.hippo.io.UniFileInputStreamPipe;
import com.hippo.network.Network;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.unifile.UniFile;
import com.hippo.util.BitmapUtils;
import com.hippo.util.GifHandler;
import com.hippo.util.PermissionRequester;
import com.hippo.widget.AvatarImageView;
import com.hippo.lib.yorozuya.IOUtils;
import com.hippo.lib.yorozuya.ResourcesUtils;
import com.hippo.lib.yorozuya.SimpleHandler;
import com.hippo.lib.yorozuya.ViewUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public final class MainActivity extends StageActivity
        implements NavigationView.OnNavigationItemSelectedListener, ImageChangeCallBack, DrawerLayout.DrawerListener {

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    private static final int REQUEST_CODE_SETTINGS = 0;

    private static final String KEY_NAV_CHECKED_ITEM = "nav_checked_item";
//    private static final String KEY_CLIP_TEXT_HASH_CODE = "clip_text_hash_code";

    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private EhDrawerLayout mDrawerLayout;
    @Nullable
    private NavigationView mNavView;
    @Nullable
    private BottomNavigationView mBottomNavView;
    @Nullable
    private FrameLayout mRightDrawer;
    @Nullable
    private AvatarImageView mAvatar;
    @Nullable
    private ImageView mHeaderBackground;
    @Nullable
    private TextView mDisplayName;
    @Nullable
    private LimitsCountView limitsCountView;
    @Nullable
    UserImageChange userImageChange;

    // 浏览器相关组件
    private NetworkDetector mNetworkDetector;
    private SearchEngineManager mSearchEngineManager;

    private int mNavCheckedItem = 0;

    GifHandler gifHandler;

    Bitmap backgroundBit;

    Handler handlerB = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int mNextFrame = gifHandler.updateFrame(backgroundBit);
            handlerB.sendEmptyMessageDelayed(1, mNextFrame);
            mHeaderBackground.setImageBitmap(backgroundBit);
        }
    };

    static {
        registerLaunchMode(SecurityScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(WarningScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(AnalyticsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(SignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(WebViewSignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(CookieSignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(SelectSiteScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(GalleryListScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TOP);
        registerLaunchMode(EhTopListScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TOP);
        registerLaunchMode(QuickSearchScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(SubscriptionsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(GalleryDetailScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryInfoScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryCommentsScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryPreviewsScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(DownloadsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(DownloadLabelsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(FavoritesScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(com.hippo.ehviewer.ui.scene.history.HistoryScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);

        registerLaunchMode(ProgressScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
    }

    @Override
    protected int getThemeResId(int theme) {
        switch (theme) {
            case Settings.THEME_LIGHT:
            default:
                return R.style.AppTheme_Main;
            case Settings.THEME_DARK:
                return R.style.AppTheme_Main_Dark;
            case Settings.THEME_BLACK:
                return R.style.AppTheme_Main_Black;
        }
    }

    @Override
    public int getContainerViewId() {
        return R.id.fragment_container;
    }

    @NonNull
    @Override
    protected Announcer getLaunchAnnouncer() {
        if (!TextUtils.isEmpty(Settings.getSecurity())) {
            return new Announcer(SecurityScene.class);
        } else if (Settings.getShowWarning()) {
            return new Announcer(WarningScene.class);
        } else if (Settings.getAskAnalytics()) {
            return new Announcer(AnalyticsScene.class);
        } else if (EhUtils.needSignedIn(this)) {
            return new Announcer(SignInScene.class);
        } else if (Settings.getSelectSite()) {
            return new Announcer(SelectSiteScene.class);
        } else {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, Settings.getLaunchPageGalleryListSceneAction());
            return new Announcer(GalleryListScene.class).setArgs(args);
        }
    }

    // Sometimes scene can't show directly
    private Announcer processAnnouncer(Announcer announcer) {
        if (0 == getSceneCount()) {
            if (!TextUtils.isEmpty(Settings.getSecurity())) {
                Bundle newArgs = new Bundle();
                newArgs.putString(SecurityScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(SecurityScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(SecurityScene.class).setArgs(newArgs);
            } else if (Settings.getShowWarning()) {
                Bundle newArgs = new Bundle();
                newArgs.putString(WarningScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(WarningScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(WarningScene.class).setArgs(newArgs);
            } else if (Settings.getAskAnalytics()) {
                Bundle newArgs = new Bundle();
                newArgs.putString(AnalyticsScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(AnalyticsScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(AnalyticsScene.class).setArgs(newArgs);
            } else if (EhUtils.needSignedIn(this)) {
                Bundle newArgs = new Bundle();
                newArgs.putString(SignInScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(SignInScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(SignInScene.class).setArgs(newArgs);
            } else if (Settings.getSelectSite()) {
                Bundle newArgs = new Bundle();
                newArgs.putString(SelectSiteScene.KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(SelectSiteScene.KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(SelectSiteScene.class).setArgs(newArgs);
            }
        }
        return announcer;
    }

    private File saveImageToTempFile(UniFile file) {
        if (null == file) {
            return null;
        }

        Bitmap bitmap = null;
        try {
            bitmap = BitmapUtils.decodeStream(new UniFileInputStreamPipe(file),
                    -1, -1, 500 * 500, false, false, null);
        } catch (OutOfMemoryError e) {
            // Ignore
        }
        if (null == bitmap) {
            return null;
        }

        File temp = AppConfig.createTempFile();
        if (null == temp) {
            return null;
        }

        OutputStream os = null;
        try {
            os = new FileOutputStream(temp);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
            return temp;
        } catch (IOException e) {
            return null;
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private boolean handleIntent(Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri == null) {
                return false;
            }
            Announcer announcer = EhUrlOpener.parseUrl(uri.toString());
            if (announcer != null) {
                startScene(processAnnouncer(announcer));
                return true;
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            String type = intent.getType();
            if ("text/plain".equals(type)) {
                ListUrlBuilder builder = new ListUrlBuilder();
                builder.setKeyword(intent.getStringExtra(Intent.EXTRA_TEXT));
                startScene(processAnnouncer(GalleryListScene.getStartAnnouncer(builder)));
                return true;
            } else {
                assert type != null;
                if (type.startsWith("image/")) {
                    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (null != uri) {
                        UniFile file = UniFile.fromUri(this, uri);
                        File temp = saveImageToTempFile(file);
                        if (null != temp) {
                            ListUrlBuilder builder = new ListUrlBuilder();
                            builder.setMode(ListUrlBuilder.MODE_IMAGE_SEARCH);
                            builder.setImagePath(temp.getPath());
                            builder.setUseSimilarityScan(true);
                            builder.setShowExpunged(true);
                            startScene(processAnnouncer(GalleryListScene.getStartAnnouncer(builder)));
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 强化应用的存在感（检查默认浏览器设置和注册状态）
     */
    private void strengthenAppPresence() {
        try {
            // 使用Handler延迟执行，避免在应用启动初期影响性能
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 强制检查和设置默认浏览器 - 用户必须完成设置
                com.hippo.ehviewer.util.DefaultBrowserHelper.checkAndForceDefaultBrowser(this);

                // 检查浏览器注册状态，如果有问题则尝试修复
                checkBrowserRegistration();

                // 注册浏览器拦截器，确保所有链接都在EhViewer中打开
                com.hippo.ehviewer.ui.WebViewActivity.registerBrowserInterceptor(this);
                android.util.Log.d("MainActivity", "Browser interceptor registered at app launch");
            }, 1500); // 减少延迟时间，让设置更早出现
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error strengthening app presence", e);
        }
    }

    /**
     * 检查浏览器注册状态
     */
    private void checkBrowserRegistration() {
        try {
            com.hippo.ehviewer.util.BrowserRegistrationManager registrationManager =
                new com.hippo.ehviewer.util.BrowserRegistrationManager(this);

            boolean isVisible = registrationManager.isBrowserVisible();

            if (!isVisible) {
                android.util.Log.w("MainActivity", "EhViewer not visible in browser list, attempting to fix");
                // 如果不在浏览器列表中，尝试修复注册
                boolean fixed = registrationManager.fixBrowserRegistration();
                if (fixed) {
                    android.util.Log.d("MainActivity", "Browser registration fixed successfully");
                } else {
                    android.util.Log.w("MainActivity", "Failed to fix browser registration");
                }
            } else {
                android.util.Log.d("MainActivity", "EhViewer is properly registered as browser");
            }

        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error checking browser registration", e);
        }
    }

    @Override
    protected void onUnrecognizedIntent(@Nullable Intent intent) {
        Class<?> clazz = getTopSceneClass();
        if (clazz != null && SolidScene.class.isAssignableFrom(clazz)) {
            // TODO the intent lost
            return;
        }

        if (!handleIntent(intent)) {
            boolean handleUrl = false;
            if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                handleUrl = true;
                Toast.makeText(this, R.string.error_cannot_parse_the_url, Toast.LENGTH_SHORT).show();
            }

            if (0 == getSceneCount()) {
                if (handleUrl) {
                    finish();
                } else {
                    Bundle args = new Bundle();
                    args.putString(GalleryListScene.KEY_ACTION, Settings.getLaunchPageGalleryListSceneAction());
                    startScene(processAnnouncer(new Announcer(GalleryListScene.class).setArgs(args)));
                }
            }
        }
    }

    @Nullable
    @Override
    protected Announcer onStartSceneFromIntent(@NonNull Class<?> clazz, @Nullable Bundle args) {
        return processAnnouncer(new Announcer(clazz).setArgs(args));
    }

    @Override
    protected void onCreate2(@Nullable Bundle savedInstanceState) {
        // 检查并强化应用的存在感（默认浏览器设置）
        strengthenAppPresence();
        Intent intent = getIntent();
        if (intent != null) {
            boolean res = intent.getBooleanExtra(SplashActivity.KEY_RESTART,false);
            if (res){
                savedInstanceState = null;
            }
        }
        setContentView(R.layout.activity_main);

        mDrawerLayout = (EhDrawerLayout) ViewUtils.$$(this, R.id.draw_view);
        mDrawerLayout.setDrawerListener(this);
        mNavView = (NavigationView) ViewUtils.$$(this, R.id.nav_view);
        mBottomNavView = (BottomNavigationView) ViewUtils.$$(this, R.id.bottom_navigation);
        mRightDrawer = (FrameLayout) ViewUtils.$$(this, R.id.right_drawer);
        View headerLayout = mNavView.getHeaderView(0);
        mAvatar = (AvatarImageView) ViewUtils.$$(headerLayout, R.id.avatar);
        mAvatar.setOnClickListener(l -> onAvatarChange());
        mHeaderBackground = (ImageView) ViewUtils.$$(headerLayout, R.id.header_background);
        mHeaderBackground.setOnClickListener(l -> onBackgroundChange());
        initUserImage();
        updateProfile();
        mDisplayName = (TextView) ViewUtils.$$(headerLayout, R.id.display_name);
        TextView mChangeTheme = (TextView) ViewUtils.$$(this, R.id.change_theme);

        limitsCountView = (LimitsCountView) ViewUtils.$$(this, R.id.limits_count_view);

        mDrawerLayout.setStatusBarColor(ResourcesUtils.getAttrColor(this, androidx.appcompat.R.attr.colorPrimaryDark));
//        mDrawerLayout.setStatusBarColor(0);

        // 初始化导航组件
        if (mNavView != null) {
//            if (Settings.isLogin()){
//                MenuItem newsItem = mNavView.getMenu().findItem(R.id.nav_eh_news);
//                newsItem.setVisible(true);
//            }
            mNavView.setNavigationItemSelectedListener(this);
        }

        // 初始化底部导航栏
        if (mBottomNavView != null) {
            android.util.Log.d("MainActivity", "Setting up bottom navigation listener");
            mBottomNavView.setOnNavigationItemSelectedListener(this::onBottomNavigationItemSelected);
            // 默认选中Eh主页
            mBottomNavView.setSelectedItemId(R.id.nav_bottom_eh);
            android.util.Log.d("MainActivity", "Bottom navigation setup completed");
        } else {
            android.util.Log.e("MainActivity", "Bottom navigation view is null!");
        }

        // 初始化浏览器组件
        initializeBrowserComponents();

        // 测试底部导航栏
        testBottomNavigation();
        if (Settings.getTheme() == 0) {
            mChangeTheme.setTextColor(getColor(R.color.theme_change_light));

            mChangeTheme.setBackgroundColor(getColor(R.color.white));
        } else if (Settings.getTheme() == 1) {
            mChangeTheme.setTextColor(getColor(R.color.theme_change_other));
            mChangeTheme.setBackgroundColor(getColor(R.color.grey_850));
        } else {
            mChangeTheme.setTextColor(getColor(R.color.theme_change_other));
            mChangeTheme.setBackgroundColor(getColor(R.color.black));
        }

        mChangeTheme.setText(getThemeText());
        mChangeTheme.setOnClickListener(v -> {
            Settings.putTheme(getNextTheme());
            ((EhApplication) getApplication()).recreate();
        });

        if (savedInstanceState == null) {
            onInit();
            checkDownloadLocation();
            if (Settings.getCellularNetworkWarning()) {
                checkCellularNetwork();
            }
        } else {
            onRestore(savedInstanceState);
        }
        EhTagDatabase.update(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 移除自动更新功能，用户可以手动在设置中检查更新
        // if (!Settings.getCloseAutoUpdate()){
        //     AppUpdater.update(this,false);
        // }

        // Track app start event for 王子的公主 app
        Analytics.trackScreen("MainActivity");
        Analytics.logEvent("app_start");
    }

    private void initUserImage() {
        File headerBackgroundFile = Settings.getUserImageFile(Settings.USER_BACKGROUND_IMAGE);
        initBackgroundImageData(headerBackgroundFile);
    }

    private void initBackgroundImageData(File file) {
        if (file != null) {
            String name = file.getName();
            String[] ns = name.split("\\.");
            if (ns[1].equals("gif") || ns[1].equals("GIF")) {
                gifHandler = new GifHandler(file.getAbsolutePath());
                int width = gifHandler.getWidth();
                int height = gifHandler.getHeight();
                backgroundBit = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                int nextFrame = gifHandler.updateFrame(backgroundBit);
                handlerB.sendEmptyMessageDelayed(1, nextFrame);
            } else {
                backgroundBit = BitmapFactory.decodeFile(file.getPath());
                assert mHeaderBackground != null;
                mHeaderBackground.setImageBitmap(backgroundBit);
            }
        }
    }

    @Override
    public void backgroundSourceChange(File file) {
        initBackgroundImageData(file);
    }

    private String getThemeText() {
        int resId;
        switch (Settings.getTheme()) {
            default:
            case Settings.THEME_LIGHT:
                resId = R.string.theme_light;
                break;
            case Settings.THEME_DARK:
                resId = R.string.theme_dark;
                break;
            case Settings.THEME_BLACK:
                resId = R.string.theme_black;
                break;
        }
        return getString(resId);
    }

    private int getNextTheme() {
        switch (Settings.getTheme()) {
            default:
            case Settings.THEME_LIGHT:
                return Settings.THEME_DARK;
            case Settings.THEME_DARK:
                return Settings.THEME_BLACK;
            case Settings.THEME_BLACK:
                return Settings.THEME_LIGHT;
        }
    }

    private void checkDownloadLocation() {
        UniFile uniFile = Settings.getDownloadLocation();
        // null == uniFile for first start
        if (null == uniFile || uniFile.ensureDir()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.waring)
                .setMessage(R.string.invalid_download_location)
                .setPositiveButton(R.string.get_it, null)
                .show();
    }

    private void checkCellularNetwork() {
        if (Network.getActiveNetworkType(this) == ConnectivityManager.TYPE_MOBILE) {
            showTip(R.string.cellular_network_warning, BaseScene.LENGTH_SHORT);
        }
    }

    private void onInit() {
        // Check permission
        PermissionRequester.request(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                getString(R.string.write_rationale), PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        EhCookieStore store = EhApplication.getEhCookieStore(getApplicationContext());
        List<Cookie> eCookies = store.getCookies(HttpUrl.get(EhUrl.HOST_E));
        List<Cookie> exCookies = store.getCookies(HttpUrl.get(EhUrl.HOST_EX));
        List<Cookie> cookies = new LinkedList<>(eCookies);
        cookies.addAll(exCookies);

        String ipbMemberId = null;
        String ipbPassHash = null;
        String igneous = null;

        for (int i = 0, n = cookies.size(); i < n; i++) {
            Cookie cookie = cookies.get(i);
            switch (cookie.name()) {
                case EhCookieStore.KEY_IPD_MEMBER_ID:
                    ipbMemberId = cookie.value();
                    break;
                case EhCookieStore.KEY_IPD_PASS_HASH:
                    ipbPassHash = cookie.value();
                    break;
                case EhCookieStore.KEY_IGNEOUS:
                    igneous = cookie.value();
                    break;
            }
        }
//        if (ipbMemberId != null || ipbPassHash != null || igneous != null) {
//            Settings.setLoginState(true);
//        } else {
//            Settings.setLoginState(false);
//        }
        Settings.setLoginState(ipbMemberId != null || ipbPassHash != null || igneous != null);
    }

    private void onRestore(Bundle savedInstanceState) {
        mNavCheckedItem = savedInstanceState.getInt(KEY_NAV_CHECKED_ITEM);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
//        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_NAV_CHECKED_ITEM, mNavCheckedItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDrawerLayout = null;
        mNavView = null;
        mRightDrawer = null;
        mAvatar = null;
        mDisplayName = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setNavCheckedItem(mNavCheckedItem);

        checkClipboardUrl();
    }

    @Override
    protected void onTransactScene() {
        super.onTransactScene();

        checkClipboardUrl();
    }

    private void checkClipboardUrl() {
        SimpleHandler.getInstance().postDelayed(() -> {
            if (!isSolid()) {
                checkClipboardUrlInternal();
            }
        }, 300);
    }

    private boolean isSolid() {
        Class<?> topClass = getTopSceneClass();
        return topClass == null || SolidScene.class.isAssignableFrom(topClass);
    }

    private String getTextFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if (clipboard != null) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0 && clip.getItemAt(0).getText() != null) {
                    return clip.getItemAt(0).getText().toString();
                }
            }
        } catch (RuntimeException ignore) {
        }
        return null;
    }

    @Nullable
    public static Announcer createAnnouncerFromClipboardUrl(String url) {
        GalleryDetailUrlParser.Result result1 = GalleryDetailUrlParser.parse(url, false);
        if (result1 != null) {
            Bundle args = new Bundle();
            args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN);
            args.putLong(GalleryDetailScene.KEY_GID, result1.gid);
            args.putString(GalleryDetailScene.KEY_TOKEN, result1.token);
            return new Announcer(GalleryDetailScene.class).setArgs(args);
        }

        GalleryPageUrlParser.Result result2 = GalleryPageUrlParser.parse(url, false);
        if (result2 != null) {
            Bundle args = new Bundle();
            args.putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN);
            args.putLong(ProgressScene.KEY_GID, result2.gid);
            args.putString(ProgressScene.KEY_PTOKEN, result2.pToken);
            args.putInt(ProgressScene.KEY_PAGE, result2.page);
            return new Announcer(ProgressScene.class).setArgs(args);
        }

        return null;
    }

    private void checkClipboardUrlInternal() {
        String text = getTextFromClipboard();
        int hashCode = text != null ? text.hashCode() : 0;

        if (text != null && hashCode != 0 && Settings.getClipboardTextHashCode() != hashCode) {
            Announcer announcer = createAnnouncerFromClipboardUrl(text);
            if (announcer != null && mDrawerLayout != null) {
                Snackbar snackbar = Snackbar.make(mDrawerLayout, R.string.clipboard_gallery_url_snack_message, Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.clipboard_gallery_url_snack_action, v -> startScene(announcer));
                snackbar.show();
            }
        }

        Settings.putClipboardTextHashCode(hashCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.you_rejected_me, Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onSceneViewCreated(SceneFragment scene, Bundle savedInstanceState) {
        super.onSceneViewCreated(scene, savedInstanceState);

        if (scene instanceof BaseScene && mRightDrawer != null && mDrawerLayout != null) {
            BaseScene baseScene = (BaseScene) scene;
            mRightDrawer.removeAllViews();
            View drawerView = baseScene.createDrawerView(
                    baseScene.getLayoutInflater2(), mRightDrawer, savedInstanceState);
            if (drawerView != null) {
                mRightDrawer.addView(drawerView);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
            }
        }
    }

    @Override
    public void onSceneViewDestroyed(SceneFragment scene) {
        super.onSceneViewDestroyed(scene);

        if (scene instanceof BaseScene) {
            BaseScene baseScene = (BaseScene) scene;
            baseScene.destroyDrawerView();
        }
    }

    public void updateProfile() {
        if (null != mAvatar) {
            String avatarUrl = Settings.getAvatar();
            if (TextUtils.isEmpty(avatarUrl)) {
                File userAvatarFile = Settings.getUserImageFile(Settings.USER_AVATAR_IMAGE);
                if (userAvatarFile != null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(userAvatarFile.getPath());
                    Drawable drawable = new BitmapDrawable(mAvatar.getResources(), bitmap);
                    mAvatar.load(drawable);
                } else {
                    mAvatar.load(R.drawable.default_avatar);
                }
            } else {
                mAvatar.load(avatarUrl, avatarUrl);
            }
        }

        if (null != mDisplayName) {
            String displayName = Settings.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                displayName = getString(R.string.default_display_name);
            }
            Toast.makeText(this, displayName, Toast.LENGTH_LONG).show();
            mDisplayName.setText(displayName);
        }

    }

    public void addAboveSnackView(View view) {
        if (mDrawerLayout != null) {
            mDrawerLayout.addAboveSnackView(view);
        }
    }

    public void removeAboveSnackView(View view) {
        if (mDrawerLayout != null) {
            mDrawerLayout.removeAboveSnackView(view);
        }
    }

    /**
     * 更换壁纸
     */
    public void onBackgroundChange() {
        if (userImageChange != null) {
            userImageChange = null;
        }
        userImageChange = new UserImageChange(MainActivity.this,
                UserImageChange.CHANGE_BACKGROUND,
                getLayoutInflater(),
                LayoutInflater.from(MainActivity.this),
                this
        );
        userImageChange.showImageChangeDialog();
    }

    /**
     * 更换头像
     */
    public void onAvatarChange() {
        if (userImageChange != null) {
            userImageChange = null;
        }
        userImageChange = new UserImageChange(MainActivity.this,
                UserImageChange.CHANGE_AVATAR,
                getLayoutInflater(),
                LayoutInflater.from(MainActivity.this),
                this
        );

        userImageChange.showImageChangeDialog();
    }

    public void setDrawerLockMode(int lockMode, int edgeGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerLockMode(lockMode, edgeGravity);
        }
    }

    public void openDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(drawerGravity);
        }
    }

    public void closeDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(drawerGravity);
        }
    }

    public void toggleDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(drawerGravity)) {
                mDrawerLayout.closeDrawer(drawerGravity);
            } else {
                mDrawerLayout.openDrawer(drawerGravity);
            }
        }
    }

    public void setDrawerGestureBlocker(DrawerLayout.GestureBlocker gestureBlocker) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setGestureBlocker(gestureBlocker);
        }
    }

    public boolean isDrawersVisible() {
        if (mDrawerLayout != null) {
            return mDrawerLayout.isDrawersVisible();
        } else {
            return false;
        }
    }

    public void setNavCheckedItem(@IdRes int resId) {
        mNavCheckedItem = resId;
        if (mNavView != null) {
            if (resId == 0) {
                mNavView.setCheckedItem(R.id.nav_stub);
            } else {
                mNavView.setCheckedItem(resId);
            }
        }
    }

    public void showTip(@StringRes int id, int length) {
        showTip(getString(id), length);
    }

    /**
     * If activity is running, show snack bar, otherwise show toast
     */
    public void showTip(CharSequence message, int length) {
        if (null != mDrawerLayout) {
            Snackbar.make(mDrawerLayout, message,
                    length == BaseScene.LENGTH_LONG ? 5000 : 3000).show();
        } else {
            Toast.makeText(this, message,
                    length == BaseScene.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && (mDrawerLayout.isDrawerOpen(Gravity.LEFT) ||
                mDrawerLayout.isDrawerOpen(Gravity.RIGHT))) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @SuppressLint({"NonConstantResourceId", "RtlHardcoded"})
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Don't select twice
        if (item.isChecked()) {
            return false;
        }

        int id = item.getItemId();

        int itemId = item.getItemId();
        if (itemId == R.id.nav_homepage) {
            Bundle nav_homepage = new Bundle();
            nav_homepage.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(nav_homepage));
        } else if (itemId == R.id.nav_subscription) {
            Bundle nav_subscription = new Bundle();
            nav_subscription.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_SUBSCRIPTION);
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(nav_subscription));
        } else if (itemId == R.id.nav_whats_hot) {
            Bundle nav_whats_hot = new Bundle();
            nav_whats_hot.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_WHATS_HOT);
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(nav_whats_hot));
        } else if (itemId == R.id.nav_top_lists) {
            Bundle nav_top_lists = new Bundle();
            nav_top_lists.putString(EhTopListScene.KEY_ACTION, EhTopListScene.ACTION_TOP_LIST);
            startSceneFirstly(new Announcer(EhTopListScene.class)
                    .setArgs(nav_top_lists));
        } else if (itemId == R.id.nav_favourite) {
            startScene(new Announcer(FavoritesScene.class));
        } else if (itemId == R.id.nav_history) {
            startScene(new Announcer(com.hippo.ehviewer.ui.scene.history.HistoryScene.class));
        } else if (itemId == R.id.nav_downloads) {
            startScene(new Announcer(DownloadsScene.class));
        } else if (itemId == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        } else {
            throw new IllegalStateException("Unexpected value: " + item.getItemId());
        }

        if (id != R.id.nav_stub && mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }

        if (limitsCountView != null) {
            limitsCountView.hide();
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS) {
            if (RESULT_OK == resultCode) {
                refreshTopScene();
            }
            return;
        }
        if (resultCode == RESULT_OK)
            if ((requestCode == UserImageChange.TAKE_CAMERA || requestCode == UserImageChange.PICK_PHOTO) && userImageChange != null) {
                userImageChange.saveImageForResult(requestCode, resultCode, data, mAvatar);
                return;
            }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDrawerSlide(View drawerView, float percent) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {
        if (limitsCountView != null) {
            limitsCountView.onLoadData(drawerView, true);
        }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        if (limitsCountView != null) {
            limitsCountView.hide();
        }
    }

    @Override
    public void onDrawerStateChanged(View drawerView, int newState) {

    }

    /**
     * 初始化浏览器组件
     */
    private void initializeBrowserComponents() {
        // 初始化网络检测器
        mNetworkDetector = NetworkDetector.getInstance(this);

        // 初始化搜索引擎管理器
        mSearchEngineManager = SearchEngineManager.getInstance(this);

        // 启动网络检测
        mNetworkDetector.detectNetworkStatus(new NetworkDetector.NetworkCallback() {
            @Override
            public void onNetworkStatusDetected(NetworkDetector.NetworkStatus status) {
                // 根据网络状态调整搜索引擎
                updateSearchEngineForNetwork(status);
            }

            @Override
            public void onDetectionFailed(String error) {
                // 检测失败时使用默认设置
                android.util.Log.w("MainActivity", "Network detection failed: " + error);
            }
        });
    }

    /**
     * 根据网络状态更新搜索引擎
     */
    private void updateSearchEngineForNetwork(NetworkDetector.NetworkStatus status) {
        if (mSearchEngineManager == null) return;

        if (status == NetworkDetector.NetworkStatus.GFW_BLOCKED) {
            // 被GFW屏蔽时自动切换到百度
            mSearchEngineManager.setSearchEngine(SearchEngineManager.SearchEngine.BAIDU);
            showTip("检测到网络限制，已自动切换到百度搜索", BaseScene.LENGTH_SHORT);
        } else {
            // 正常网络使用自动选择
            mSearchEngineManager.setSearchEngine(SearchEngineManager.SearchEngine.AUTO);
        }
    }

    /**
     * 底部导航栏项目选择处理
     */
    private boolean onBottomNavigationItemSelected(android.view.MenuItem item) {
        int itemId = item.getItemId();

        android.util.Log.d("MainActivity", "Bottom navigation item selected: " + itemId);

        if (itemId == R.id.nav_bottom_eh) {
            // Eh主页 - 默认进入Eh当前页面
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, Settings.getLaunchPageGalleryListSceneAction());
            startSceneFirstly(new Announcer(GalleryListScene.class).setArgs(args));
        } else if (itemId == R.id.nav_bottom_browser) {
            // 浏览器 - 直接进入浏览器界面，默认访问Google或百度
            android.util.Log.d("MainActivity", "Browser button clicked, calling openBrowserWithSmartUrl");
            openBrowserWithSmartUrl();
        } else if (itemId == R.id.nav_bottom_bookmarks) {
            // 书签 - 根据模式选择打开浏览器书签或EhViewer图库收藏
            if (Settings.isBrowserMode()) {
                // 浏览器模式：打开浏览器书签
                try {
                    com.hippo.ehviewer.ui.BookmarksActivity.startBookmarks(this);
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Failed to start BookmarksActivity", e);
                    showTip("无法打开浏览器书签", BaseScene.LENGTH_SHORT);
                }
            } else {
                // EhViewer图库模式：打开图库收藏
                startScene(new Announcer(com.hippo.ehviewer.ui.scene.gallery.list.FavoritesScene.class));
            }
        } else if (itemId == R.id.nav_bottom_settings) {
            // 设置 - 打开设置页面
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        }

        // 关闭抽屉（如果打开的话）
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }

        return true;
    }

    /**
     * 打开浏览器主页
     */
    private void openBrowserHome() {
        // 打开浏览器主页，可以是自定义的浏览器启动页面
        // 或者根据用户偏好打开特定的页面

        // 检查是否有设置的浏览器主页
        String browserHomeUrl = Settings.getString("browser_home_url", null);
        if (browserHomeUrl == null || browserHomeUrl.trim().isEmpty()) {
            // 默认打开百度或Google（根据网络状况）
            if (mNetworkDetector != null &&
                mNetworkDetector.getCurrentStatus() == NetworkDetector.NetworkStatus.GFW_BLOCKED) {
                browserHomeUrl = "https://www.baidu.com";
            } else {
                browserHomeUrl = "https://www.google.com";
            }
        }

        // 使用WebViewActivity打开浏览器
        Intent intent = new Intent(this, com.hippo.ehviewer.ui.WebViewActivity.class);
        intent.setData(android.net.Uri.parse(browserHomeUrl));
        startActivity(intent);
    }

    /**
     * 智能打开浏览器（根据GFW状态自动选择搜索引擎）
     */
    private void openBrowserWithSmartUrl() {
        // 直接进入浏览器界面，默认访问Google，如果GFW拦截则访问百度
        String defaultUrl;

        // 检查网络状态，决定默认搜索引擎
        if (mNetworkDetector != null &&
            mNetworkDetector.getCurrentStatus() == NetworkDetector.NetworkStatus.GFW_BLOCKED) {
            // 被GFW屏蔽，使用百度
            defaultUrl = "https://www.baidu.com";
            android.util.Log.d("MainActivity", "GFW detected, using Baidu as default");
        } else {
            // 正常网络，使用Google
            defaultUrl = "https://www.google.com";
            android.util.Log.d("MainActivity", "Normal network, using Google as default");
        }

        // 直接启动WebViewActivity进入浏览器界面
        try {
            android.util.Log.d("MainActivity", "Starting browser with URL: " + defaultUrl);
            Intent intent = new Intent(this, com.hippo.ehviewer.ui.WebViewActivity.class);
            intent.putExtra("url", defaultUrl);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            android.util.Log.d("MainActivity", "Intent created with URL: " + defaultUrl);
            startActivity(intent);
            android.util.Log.d("MainActivity", "Browser started successfully");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Failed to start browser", e);
            showTip("无法启动浏览器: " + e.getMessage(), BaseScene.LENGTH_SHORT);
        }
    }

    /**
     * 启动浏览器搜索
     */
    public void startBrowserSearch(String query) {
        if (mSearchEngineManager != null) {
            mSearchEngineManager.performSearch(query, new SearchEngineManager.SearchCallback() {
                @Override
                public void onSearchUrlGenerated(String url, String engineName) {
                    // 使用WebViewActivity打开搜索结果
                    Intent intent = new Intent(MainActivity.this, com.hippo.ehviewer.ui.WebViewActivity.class);
                    intent.setData(android.net.Uri.parse(url));
                    startActivity(intent);
                }

                @Override
                public void onSearchFailed(String error) {
                    showTip("搜索失败: " + error, BaseScene.LENGTH_SHORT);
                }
            });
        }
    }

    /**
     * 获取网络状态信息
     */
    public NetworkDetector.NetworkStatus getCurrentNetworkStatus() {
        return mNetworkDetector != null ? mNetworkDetector.getCurrentStatus() :
               NetworkDetector.NetworkStatus.UNKNOWN;
    }

    /**
     * 测试底部导航栏是否正常工作
     */
    public void testBottomNavigation() {
        if (mBottomNavView != null) {
            android.util.Log.d("MainActivity", "Bottom navigation test - view is not null");
            android.util.Log.d("MainActivity", "Menu items count: " + mBottomNavView.getMenu().size());

            // 遍历所有菜单项
            for (int i = 0; i < mBottomNavView.getMenu().size(); i++) {
                android.view.MenuItem item = mBottomNavView.getMenu().getItem(i);
                android.util.Log.d("MainActivity", "Menu item " + i + ": ID=" + item.getItemId() + ", Title=" + item.getTitle());
            }
        } else {
            android.util.Log.e("MainActivity", "Bottom navigation test - view is null");
        }
    }

    /**
     * 获取当前搜索引擎信息
     */
    public String getCurrentSearchEngineInfo() {
        if (mSearchEngineManager != null) {
            SearchEngineManager.SearchEngine engine = mSearchEngineManager.getCurrentSearchEngine();
            return mSearchEngineManager.getSearchEngineDisplayName(engine);
        }
        return "未知";
    }
}
