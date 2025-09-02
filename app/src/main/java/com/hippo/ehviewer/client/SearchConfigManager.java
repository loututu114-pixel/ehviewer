package com.hippo.ehviewer.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.R;
import com.hippo.util.AppHelper;
import com.hippo.util.IoThreadPoolExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 搜索配置管理器
 * 负责管理搜索配置的下载、解析和应用
 */
public class SearchConfigManager {

    private static final String TAG = "SearchConfigManager";
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/sou.json";
    private static final String PREFS_NAME = "search_config";
    private static final String KEY_CONFIG_JSON = "config_json";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final long UPDATE_INTERVAL = TimeUnit.HOURS.toMillis(24); // 24小时更新一次
    private static final long CACHE_VALID_TIME = TimeUnit.DAYS.toMillis(7); // 缓存7天有效
    private static final int MAX_RETRY_COUNT = 3; // 最大重试次数

    private static SearchConfigManager sInstance;

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private JSONObject mConfigJson;
    private String mCountryCode;
    private String mChannelCode;
    private boolean mInitialized = false;

    // 缓存的配置
    private SearchEngine mCurrentEngine;
    private List<SearchEngine> mAvailableEngines;
    private Map<String, Pattern> mUrlPatterns;

    /**
     * 搜索引擎配置类
     */
    public static class SearchEngine {
        public final String id;
        public final String name;
        public final String url;
        public final String icon;
        public final String suggestUrl;

        public SearchEngine(String id, String name, String url, String icon, String suggestUrl) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.icon = icon;
            this.suggestUrl = suggestUrl;
        }

        public String getSearchUrl(String query) {
            return url.replace("%s", Uri.encode(query));
        }

        public String getSuggestUrl(String query) {
            return suggestUrl != null ? suggestUrl.replace("%s", Uri.encode(query)) : null;
        }

        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    private SearchConfigManager(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mAvailableEngines = new ArrayList<>();
        mUrlPatterns = new HashMap<>();
    }

    public static synchronized SearchConfigManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SearchConfigManager(context);
        }
        return sInstance;
    }

    /**
     * 初始化配置管理器
     */
    public synchronized void initialize() {
        if (mInitialized) return;

        // 检测国家代码
        detectCountryCode();

        // 检测渠道代码
        detectChannelCode();

        // 加载本地配置
        loadLocalConfig();

        // 检查是否需要更新配置
        if (shouldUpdateConfig()) {
            updateConfigAsync();
        } else {
            parseConfig();
        }

        mInitialized = true;
    }

    /**
     * 检测国家代码
     */
    private void detectCountryCode() {
        try {
            mCountryCode = Locale.getDefault().getCountry();
            if (mCountryCode == null || mCountryCode.isEmpty()) {
                // 尝试通过Geocoder获取国家代码
                Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(39.9042, 116.4074, 1); // 北京坐标作为默认
                if (!addresses.isEmpty()) {
                    mCountryCode = addresses.get(0).getCountryCode();
                }
            }
            Log.d(TAG, "Detected country code: " + mCountryCode);
        } catch (Exception e) {
            Log.e(TAG, "Failed to detect country code", e);
            mCountryCode = "US"; // 默认美国
        }
    }

    /**
     * 检测渠道代码
     */
    private void detectChannelCode() {
        try {
            // 从应用包名或其他方式获取渠道信息
            String packageName = mContext.getPackageName();
            if (packageName.contains("xiaomi")) {
                mChannelCode = "xiaomi";
            } else if (packageName.contains("huawei")) {
                mChannelCode = "huawei";
            } else if (packageName.contains("oppo")) {
                mChannelCode = "oppo";
            } else if (packageName.contains("vivo")) {
                mChannelCode = "vivo";
            } else {
                mChannelCode = "google"; // 默认Google Play
            }
            Log.d(TAG, "Detected channel code: " + mChannelCode);
        } catch (Exception e) {
            Log.e(TAG, "Failed to detect channel code", e);
            mChannelCode = "google";
        }
    }

    /**
     * 加载本地配置
     */
    private void loadLocalConfig() {
        try {
            String configJson = mPrefs.getString(KEY_CONFIG_JSON, null);
            if (configJson != null) {
                mConfigJson = new JSONObject(configJson);
                Log.d(TAG, "Loaded local config");
            } else {
                // 加载默认配置
                loadDefaultConfig();
                Log.d(TAG, "Loaded default config");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load local config", e);
            loadDefaultConfig();
        }
    }

    /**
     * 加载默认配置
     */
    private void loadDefaultConfig() {
        try {
            InputStream is = mContext.getResources().openRawResource(R.raw.sou);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            mConfigJson = new JSONObject(sb.toString());
            Log.d(TAG, "Loaded default config from raw resource");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load default config", e);
            createFallbackConfig();
        }
    }

    /**
     * 创建后备配置
     */
    private void createFallbackConfig() {
        try {
            mConfigJson = new JSONObject();
            JSONObject searchConfig = new JSONObject();

            // 默认搜索引擎配置
            JSONObject defaultEngine = new JSONObject();
            defaultEngine.put("engine", "google");
            defaultEngine.put("name", "Google");
            defaultEngine.put("url", "https://www.google.com/search?q=%s");
            defaultEngine.put("icon", "https://www.google.com/favicon.ico");
            searchConfig.put("default", defaultEngine);

            // 中国搜索引擎配置
            JSONObject chinaEngine = new JSONObject();
            chinaEngine.put("engine", "baidu");
            chinaEngine.put("name", "百度");
            chinaEngine.put("url", "https://www.baidu.com/s?wd=%s");
            chinaEngine.put("icon", "https://www.baidu.com/favicon.ico");
            searchConfig.put("china", chinaEngine);

            mConfigJson.put("searchConfig", searchConfig);
            mConfigJson.put("version", "fallback");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create fallback config", e);
        }
    }

    /**
     * 检查是否需要更新配置
     */
    private boolean shouldUpdateConfig() {
        long lastUpdate = mPrefs.getLong(KEY_LAST_UPDATE, 0);
        long currentTime = System.currentTimeMillis();

        // 如果缓存过期（7天），强制更新
        if (currentTime - lastUpdate > CACHE_VALID_TIME) {
            return true;
        }

        // 如果是第一次启动，尝试更新
        if (lastUpdate == 0) {
            return true;
        }

        // 定期更新（24小时）
        return currentTime - lastUpdate > UPDATE_INTERVAL;
    }

    /**
     * 检查网络是否可用
     */
    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager connectivityManager =
                (android.net.ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check network availability", e);
        }
        return false;
    }

    /**
     * 异步更新配置
     */
    private void updateConfigAsync() {
        // 检查网络是否可用
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Network not available, skipping config update");
            return;
        }

        IoThreadPoolExecutor.getInstance().execute(() -> {
            String newConfig = null;
            int retryCount = 0;

            // 重试机制
            while (retryCount < MAX_RETRY_COUNT && newConfig == null) {
                try {
                    newConfig = downloadConfig();
                    if (newConfig != null) {
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to download config, retry " + (retryCount + 1), e);
                }

                retryCount++;
                if (retryCount < MAX_RETRY_COUNT) {
                    try {
                        // 等待一段时间后重试
                        Thread.sleep(2000 * retryCount); // 2秒、4秒递增
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (newConfig != null) {
                try {
                    JSONObject newConfigJson = new JSONObject(newConfig);
                    mConfigJson = newConfigJson;

                    // 保存到本地
                    mPrefs.edit()
                        .putString(KEY_CONFIG_JSON, newConfig)
                        .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                        .apply();

                    // 重新解析配置
                    parseConfig();
                    Log.d(TAG, "Config updated successfully from remote");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse downloaded config", e);
                }
            } else {
                Log.w(TAG, "Failed to update config after " + MAX_RETRY_COUNT + " retries");
            }
        });
    }

    /**
     * 下载远程配置
     */
    private String downloadConfig() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(CONFIG_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000); // 15秒连接超时
            connection.setReadTimeout(20000); // 20秒读取超时
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "EhViewer/" + getVersionName());
            connection.setRequestProperty("Accept", "application/json");
            connection.setUseCaches(false); // 不使用缓存

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n'); // 保持原始格式
                }

                String result = sb.toString().trim();
                if (result.isEmpty()) {
                    Log.w(TAG, "Downloaded config is empty");
                    return null;
                }

                // 验证JSON格式
                try {
                    new JSONObject(result);
                    Log.d(TAG, "Successfully downloaded config, size: " + result.length() + " bytes");
                    return result;
                } catch (JSONException e) {
                    Log.e(TAG, "Downloaded config is not valid JSON", e);
                    return null;
                }
            } else {
                Log.w(TAG, "Failed to download config, response code: " + responseCode);
            }
        } catch (java.net.SocketTimeoutException e) {
            Log.w(TAG, "Config download timeout", e);
        } catch (java.net.UnknownHostException e) {
            Log.w(TAG, "Network unreachable, cannot download config", e);
        } catch (java.io.IOException e) {
            Log.e(TAG, "IO error during config download", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during config download", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close reader", e);
                }
            }
        }

        return null;
    }

    /**
     * 获取应用版本名称
     */
    private String getVersionName() {
        try {
            android.content.pm.PackageManager pm = mContext.getPackageManager();
            android.content.pm.PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    /**
     * 解析配置
     */
    private void parseConfig() {
        if (mConfigJson == null) return;

        try {
            // 解析搜索引擎
            parseEngines();

            // 选择当前搜索引擎
            selectCurrentEngine();

            // 解析URL模式
            parseUrlPatterns();

            Log.d(TAG, "Config parsed successfully, current engine: " + (mCurrentEngine != null ? mCurrentEngine.name : "null"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse config", e);
        }
    }

    /**
     * 解析搜索引擎配置
     */
    private void parseEngines() throws JSONException {
        mAvailableEngines.clear();

        JSONObject searchConfig = mConfigJson.optJSONObject("searchConfig");
        if (searchConfig == null) return;

        JSONObject engines = searchConfig.optJSONObject("engines");
        if (engines == null) return;

        JSONArray names = engines.names();
        if (names == null) return;

        for (int i = 0; i < names.length(); i++) {
            String engineId = names.getString(i);
            JSONObject engineConfig = engines.getJSONObject(engineId);

            String name = engineConfig.optString("name", engineId);
            String url = engineConfig.optString("url", "");
            String icon = engineConfig.optString("icon", "");
            String suggestUrl = engineConfig.optString("suggest", null);

            if (!url.isEmpty()) {
                SearchEngine engine = new SearchEngine(engineId, name, url, icon, suggestUrl);
                mAvailableEngines.add(engine);
            }
        }

        Log.d(TAG, "Parsed " + mAvailableEngines.size() + " search engines");
    }

    /**
     * 选择当前搜索引擎
     */
    private void selectCurrentEngine() {
        if (mConfigJson == null) return;

        try {
            JSONObject searchConfig = mConfigJson.optJSONObject("searchConfig");
            if (searchConfig == null) return;

            String engineId = null;

            // 优先级：渠道配置 > 国家配置 > 默认配置
            JSONObject channelMapping = searchConfig.optJSONObject("channelMapping");
            if (channelMapping != null && mChannelCode != null) {
                JSONObject channelConfig = channelMapping.optJSONObject(mChannelCode);
                if (channelConfig != null) {
                    engineId = channelConfig.optString("engine", null);
                    Log.d(TAG, "Selected engine by channel: " + engineId);
                }
            }

            if (engineId == null) {
                JSONObject countryMapping = searchConfig.optJSONObject("countryMapping");
                if (countryMapping != null && mCountryCode != null) {
                    String countryEngine = countryMapping.optString(mCountryCode, null);
                    if (countryEngine != null) {
                        JSONObject countryConfig = searchConfig.optJSONObject(countryEngine);
                        if (countryConfig != null) {
                            engineId = countryConfig.optString("engine", null);
                            Log.d(TAG, "Selected engine by country: " + engineId);
                        }
                    }
                }
            }

            if (engineId == null) {
                JSONObject defaultConfig = searchConfig.optJSONObject("default");
                if (defaultConfig != null) {
                    engineId = defaultConfig.optString("engine", null);
                    Log.d(TAG, "Selected default engine: " + engineId);
                }
            }

            // 查找对应的搜索引擎
            if (engineId != null) {
                for (SearchEngine engine : mAvailableEngines) {
                    if (engineId.equals(engine.id)) {
                        mCurrentEngine = engine;
                        break;
                    }
                }
            }

            // 如果没找到，使用第一个可用的搜索引擎
            if (mCurrentEngine == null && !mAvailableEngines.isEmpty()) {
                mCurrentEngine = mAvailableEngines.get(0);
                Log.d(TAG, "Using first available engine: " + mCurrentEngine.name);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to select current engine", e);
        }
    }

    /**
     * 解析URL模式
     */
    private void parseUrlPatterns() throws JSONException {
        mUrlPatterns.clear();

        JSONObject urlPatterns = mConfigJson.optJSONObject("urlPatterns");
        if (urlPatterns == null) return;

        String urlRegex = urlPatterns.optString("urlRegex", "");
        String ipRegex = urlPatterns.optString("ipRegex", "");
        String domainRegex = urlPatterns.optString("domainRegex", "");
        String localhostRegex = urlPatterns.optString("localhostRegex", "");

        if (!urlRegex.isEmpty()) {
            mUrlPatterns.put("url", Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE));
        }
        if (!ipRegex.isEmpty()) {
            mUrlPatterns.put("ip", Pattern.compile(ipRegex, Pattern.CASE_INSENSITIVE));
        }
        if (!domainRegex.isEmpty()) {
            mUrlPatterns.put("domain", Pattern.compile(domainRegex, Pattern.CASE_INSENSITIVE));
        }
        if (!localhostRegex.isEmpty()) {
            mUrlPatterns.put("localhost", Pattern.compile(localhostRegex, Pattern.CASE_INSENSITIVE));
        }
    }

    /**
     * 判断输入是否为URL
     */
    public boolean isUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String trimmed = input.trim();

        // 检查协议
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
            trimmed.startsWith("ftp://") || trimmed.startsWith("file://")) {
            return true;
        }

        // 使用正则表达式检查
        for (Map.Entry<String, Pattern> entry : mUrlPatterns.entrySet()) {
            if (entry.getValue().matcher(trimmed).matches()) {
                return true;
            }
        }

        // 检查是否包含点号且不含空格（简单的域名判断）
        return trimmed.contains(".") && !trimmed.contains(" ");
    }

    /**
     * 处理搜索或URL加载
     */
    public String processInput(String input) {
        if (isUrl(input)) {
            // 如果没有协议，添加https
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                return "https://" + input;
            }
            return input;
        } else {
            // 使用搜索引擎
            return getSearchUrl(input);
        }
    }

    /**
     * 获取搜索URL
     */
    public String getSearchUrl(String query) {
        if (mCurrentEngine != null) {
            return mCurrentEngine.getSearchUrl(query);
        }

        // 后备方案：使用Google
        return "https://www.google.com/search?q=" + Uri.encode(query);
    }

    /**
     * 获取建议URL
     */
    public String getSuggestUrl(String query) {
        if (mCurrentEngine != null && mCurrentEngine.suggestUrl != null) {
            return mCurrentEngine.getSuggestUrl(query);
        }
        return null;
    }

    /**
     * 获取当前搜索引擎
     */
    public SearchEngine getCurrentEngine() {
        return mCurrentEngine;
    }

    /**
     * 获取所有可用搜索引擎
     */
    public List<SearchEngine> getAvailableEngines() {
        return new ArrayList<>(mAvailableEngines);
    }

    /**
     * 切换搜索引擎
     */
    public void switchEngine(String engineId) {
        for (SearchEngine engine : mAvailableEngines) {
            if (engineId.equals(engine.id)) {
                mCurrentEngine = engine;
                Log.d(TAG, "Switched to engine: " + engine.name);
                break;
            }
        }
    }

    /**
     * 获取国家代码
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    /**
     * 获取渠道代码
     */
    public String getChannelCode() {
        return mChannelCode;
    }

    /**
     * 获取默认首页URL
     */
    public String getDefaultHomepageUrl() {
        if (mConfigJson == null) return "https://www.google.com";

        try {
            JSONObject searchConfig = mConfigJson.optJSONObject("searchConfig");
            if (searchConfig == null) return "https://www.google.com";

            JSONObject homepage = searchConfig.optJSONObject("homepage");
            if (homepage == null || !homepage.optBoolean("enabled", true)) {
                return "https://www.google.com";
            }

            // 根据国家选择不同的首页
            String homepageUrl = null;
            if ("CN".equals(mCountryCode) || "china".equals(getCountryEngineType())) {
                homepageUrl = homepage.optString("china", null);
            }

            if (homepageUrl == null) {
                homepageUrl = homepage.optString("url", "https://www.google.com");
            }

            Log.d(TAG, "Default homepage URL: " + homepageUrl);
            return homepageUrl;

        } catch (Exception e) {
            Log.e(TAG, "Failed to get default homepage URL", e);
            return "https://www.google.com";
        }
    }

    /**
     * 获取自定义首页选项
     */
    public List<HomepageOption> getCustomHomepageOptions() {
        List<HomepageOption> options = new ArrayList<>();

        if (mConfigJson == null) return options;

        try {
            JSONObject searchConfig = mConfigJson.optJSONObject("searchConfig");
            if (searchConfig == null) return options;

            JSONObject homepage = searchConfig.optJSONObject("homepage");
            if (homepage == null) return options;

            JSONArray customOptions = homepage.optJSONArray("customOptions");
            if (customOptions == null) return options;

            for (int i = 0; i < customOptions.length(); i++) {
                JSONObject option = customOptions.getJSONObject(i);
                String name = option.optString("name", "");
                String url = option.optString("url", "");
                String chinaUrl = option.optString("china", null);

                if (!name.isEmpty() && !url.isEmpty()) {
                    String finalUrl = url;
                    if ("CN".equals(mCountryCode) || "china".equals(getCountryEngineType())) {
                        if (chinaUrl != null && !chinaUrl.isEmpty()) {
                            finalUrl = chinaUrl;
                        }
                    }
                    options.add(new HomepageOption(name, finalUrl));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to get custom homepage options", e);
        }

        return options;
    }

    /**
     * 首页选项类
     */
    public static class HomepageOption {
        public final String name;
        public final String url;

        public HomepageOption(String name, String url) {
            this.name = name;
            this.url = url;
        }

        @Override
        public String toString() {
            return name + " (" + url + ")";
        }
    }

    /**
     * 检查是否启用默认首页
     */
    public boolean isHomepageEnabled() {
        if (mConfigJson == null) return true;

        try {
            JSONObject searchConfig = mConfigJson.optJSONObject("searchConfig");
            if (searchConfig == null) return true;

            JSONObject homepage = searchConfig.optJSONObject("homepage");
            if (homepage == null) return true;

            return homepage.optBoolean("enabled", true);

        } catch (Exception e) {
            Log.e(TAG, "Failed to check homepage enabled", e);
            return true;
        }
    }

    /**
     * 获取国家引擎类型
     */
    private String getCountryEngineType() {
        if (mConfigJson == null) return "default";

        try {
            JSONObject searchConfig = mConfigJson.optJSONObject("searchConfig");
            if (searchConfig == null) return "default";

            JSONObject countryMapping = searchConfig.optJSONObject("countryMapping");
            if (countryMapping != null && mCountryCode != null) {
                return countryMapping.optString(mCountryCode, "default");
            }

            return "default";

        } catch (Exception e) {
            Log.e(TAG, "Failed to get country engine type", e);
            return "default";
        }
    }

    /**
     * 强制更新配置
     */
    public void forceUpdate() {
        updateConfigAsync();
    }
}
