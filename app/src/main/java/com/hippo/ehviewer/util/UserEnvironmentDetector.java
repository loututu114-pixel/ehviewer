package com.hippo.ehviewer.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全局用户环境检测器
 * 在应用启动时检测用户IP、地区、网络环境等信息
 * 供全局使用，避免重复检测
 */
public class UserEnvironmentDetector {
    
    private static final String TAG = "UserEnvironmentDetector";
    private static UserEnvironmentDetector sInstance;
    private static final Object sLock = new Object();
    
    // 检测状态
    public enum DetectionStatus {
        NOT_STARTED,    // 未开始
        DETECTING,      // 检测中
        SUCCESS,        // 检测成功
        FAILED          // 检测失败
    }
    
    // 地区类型
    public enum RegionType {
        CHINA,          // 中国大陆
        CHINA_HK,       // 中国香港
        CHINA_TW,       // 中国台湾
        CHINA_MO,       // 中国澳门
        INTERNATIONAL,  // 国际
        UNKNOWN         // 未知
    }
    
    // 网络环境类型
    public enum NetworkEnvironment {
        DOMESTIC,       // 国内网络
        INTERNATIONAL,  // 国际网络
        MIXED,          // 混合网络
        UNKNOWN         // 未知
    }
    
    // 环境信息结果类
    public static class EnvironmentInfo {
        public String ipAddress = "";
        public String country = "";
        public String region = "";
        public String city = "";
        public String timezone = "";
        public RegionType regionType = RegionType.UNKNOWN;
        public NetworkEnvironment networkEnvironment = NetworkEnvironment.UNKNOWN;
        public boolean isChineseUser = false;
        public boolean canAccessGoogle = false;
        public boolean canAccessBaidu = true;
        public String preferredSearchEngine = "baidu";
        public String preferredHomepage = "https://main.eh-viewer.com/";
        public long detectionTime = System.currentTimeMillis();
        
        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                "IP: %s, Country: %s, Region: %s, City: %s, Type: %s, Network: %s", 
                ipAddress, country, region, city, regionType, networkEnvironment);
        }
    }
    
    // 检测结果回调接口
    public interface DetectionCallback {
        void onDetectionSuccess(EnvironmentInfo environmentInfo);
        void onDetectionFailed(String error);
        void onDetectionProgress(String message);
    }
    
    private Context mContext;
    private EnvironmentInfo mEnvironmentInfo;
    private DetectionStatus mDetectionStatus = DetectionStatus.NOT_STARTED;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    
    private UserEnvironmentDetector(Context context) {
        mContext = context.getApplicationContext();
        mEnvironmentInfo = new EnvironmentInfo();
    }
    
    public static UserEnvironmentDetector getInstance(Context context) {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new UserEnvironmentDetector(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 开始环境检测
     */
    public void startDetection(DetectionCallback callback) {
        if (mDetectionStatus == DetectionStatus.DETECTING) {
            Log.d(TAG, "Detection already in progress");
            return;
        }
        
        if (mDetectionStatus == DetectionStatus.SUCCESS) {
            // 如果已经成功检测过，直接返回结果
            if (callback != null) {
                callback.onDetectionSuccess(mEnvironmentInfo);
            }
            return;
        }
        
        mDetectionStatus = DetectionStatus.DETECTING;
        Log.d(TAG, "Starting user environment detection...");
        
        if (callback != null) {
            mMainHandler.post(() -> callback.onDetectionProgress("正在检测用户环境..."));
        }
        
        mExecutor.execute(() -> performDetection(callback));
    }
    
    /**
     * 执行检测逻辑
     */
    private void performDetection(DetectionCallback callback) {
        try {
            // 1. 检测系统地区设置
            detectSystemLocale();
            
            if (callback != null) {
                mMainHandler.post(() -> callback.onDetectionProgress("正在获取IP地址信息..."));
            }
            
            // 2. 检测IP地址和地理位置
            boolean ipDetected = detectIPAndLocation();
            
            if (callback != null) {
                mMainHandler.post(() -> callback.onDetectionProgress("正在检测网络环境..."));
            }
            
            // 3. 检测网络环境
            detectNetworkEnvironment();
            
            // 4. 根据检测结果配置环境
            configureEnvironmentSettings();
            
            mDetectionStatus = DetectionStatus.SUCCESS;
            Log.d(TAG, "Detection completed successfully: " + mEnvironmentInfo.toString());
            
            if (callback != null) {
                mMainHandler.post(() -> callback.onDetectionSuccess(mEnvironmentInfo));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Detection failed", e);
            mDetectionStatus = DetectionStatus.FAILED;
            
            // 使用系统设置作为fallback
            configureEnvironmentSettings();
            
            if (callback != null) {
                mMainHandler.post(() -> callback.onDetectionFailed(e.getMessage()));
            }
        }
    }
    
    /**
     * 检测系统地区设置
     */
    private void detectSystemLocale() {
        Locale locale = Locale.getDefault();
        String country = locale.getCountry();
        String language = locale.getLanguage();
        
        mEnvironmentInfo.country = country;
        
        // 根据系统设置判断是否为中文用户
        if ("zh".equals(language) || "CN".equals(country) || 
            "HK".equals(country) || "TW".equals(country) || "MO".equals(country)) {
            mEnvironmentInfo.isChineseUser = true;
            
            switch (country) {
                case "CN":
                    mEnvironmentInfo.regionType = RegionType.CHINA;
                    break;
                case "HK":
                    mEnvironmentInfo.regionType = RegionType.CHINA_HK;
                    break;
                case "TW":
                    mEnvironmentInfo.regionType = RegionType.CHINA_TW;
                    break;
                case "MO":
                    mEnvironmentInfo.regionType = RegionType.CHINA_MO;
                    break;
                default:
                    mEnvironmentInfo.regionType = RegionType.CHINA;
                    break;
            }
        } else {
            mEnvironmentInfo.regionType = RegionType.INTERNATIONAL;
        }
        
        Log.d(TAG, "System locale detected: " + locale.toString());
    }
    
    /**
     * 检测IP地址和地理位置
     */
    private boolean detectIPAndLocation() {
        try {
            // 使用多个IP检测服务，提高成功率
            String[] ipServices = {
                "https://ipapi.co/json/",
                "http://ip-api.com/json/",
                "https://httpbin.org/ip"
            };
            
            for (String serviceUrl : ipServices) {
                try {
                    if (detectFromService(serviceUrl)) {
                        return true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "IP service failed: " + serviceUrl, e);
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "IP detection failed", e);
            return false;
        }
    }
    
    /**
     * 从指定服务检测IP信息
     */
    private boolean detectFromService(String serviceUrl) throws Exception {
        URL url = new URL(serviceUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "EhViewer/1.0");
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return parseIPResponse(response.toString(), serviceUrl);
        }
        
        return false;
    }
    
    /**
     * 解析IP服务响应
     */
    private boolean parseIPResponse(String response, String serviceUrl) {
        try {
            JSONObject json = new JSONObject(response);
            
            if (serviceUrl.contains("ipapi.co")) {
                mEnvironmentInfo.ipAddress = json.optString("ip", "");
                mEnvironmentInfo.country = json.optString("country_code", "");
                mEnvironmentInfo.region = json.optString("region", "");
                mEnvironmentInfo.city = json.optString("city", "");
                mEnvironmentInfo.timezone = json.optString("timezone", "");
                
            } else if (serviceUrl.contains("ip-api.com")) {
                mEnvironmentInfo.ipAddress = json.optString("query", "");
                mEnvironmentInfo.country = json.optString("countryCode", "");
                mEnvironmentInfo.region = json.optString("regionName", "");
                mEnvironmentInfo.city = json.optString("city", "");
                mEnvironmentInfo.timezone = json.optString("timezone", "");
                
            } else if (serviceUrl.contains("httpbin.org")) {
                mEnvironmentInfo.ipAddress = json.optString("origin", "");
            }
            
            // 更新地区类型
            updateRegionTypeFromIP();
            
            return !mEnvironmentInfo.ipAddress.isEmpty();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse IP response", e);
            return false;
        }
    }
    
    /**
     * 根据IP检测结果更新地区类型
     */
    private void updateRegionTypeFromIP() {
        String country = mEnvironmentInfo.country;
        
        if ("CN".equals(country)) {
            mEnvironmentInfo.regionType = RegionType.CHINA;
            mEnvironmentInfo.isChineseUser = true;
        } else if ("HK".equals(country)) {
            mEnvironmentInfo.regionType = RegionType.CHINA_HK;
            mEnvironmentInfo.isChineseUser = true;
        } else if ("TW".equals(country)) {
            mEnvironmentInfo.regionType = RegionType.CHINA_TW;
            mEnvironmentInfo.isChineseUser = true;
        } else if ("MO".equals(country)) {
            mEnvironmentInfo.regionType = RegionType.CHINA_MO;
            mEnvironmentInfo.isChineseUser = true;
        } else if (!country.isEmpty()) {
            mEnvironmentInfo.regionType = RegionType.INTERNATIONAL;
            mEnvironmentInfo.isChineseUser = false;
        }
    }
    
    /**
     * 检测网络环境
     */
    private void detectNetworkEnvironment() {
        try {
            // 测试Google访问性
            mEnvironmentInfo.canAccessGoogle = testConnectivity("https://www.google.com", 3000);
            
            // 测试百度访问性
            mEnvironmentInfo.canAccessBaidu = testConnectivity("https://www.baidu.com", 3000);
            
            // 判断网络环境
            if (mEnvironmentInfo.canAccessGoogle && mEnvironmentInfo.canAccessBaidu) {
                mEnvironmentInfo.networkEnvironment = NetworkEnvironment.MIXED;
            } else if (mEnvironmentInfo.canAccessGoogle) {
                mEnvironmentInfo.networkEnvironment = NetworkEnvironment.INTERNATIONAL;
            } else if (mEnvironmentInfo.canAccessBaidu) {
                mEnvironmentInfo.networkEnvironment = NetworkEnvironment.DOMESTIC;
            } else {
                mEnvironmentInfo.networkEnvironment = NetworkEnvironment.UNKNOWN;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Network environment detection failed", e);
            mEnvironmentInfo.networkEnvironment = NetworkEnvironment.UNKNOWN;
        }
    }
    
    /**
     * 测试连接性
     */
    private boolean testConnectivity(String urlString, int timeout) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("HEAD");
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode >= 200 && responseCode < 400;
            
        } catch (Exception e) {
            Log.d(TAG, "Connectivity test failed for " + urlString + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据检测结果配置环境设置
     */
    private void configureEnvironmentSettings() {
        // 配置搜索引擎偏好
        if (mEnvironmentInfo.isChineseUser || 
            mEnvironmentInfo.networkEnvironment == NetworkEnvironment.DOMESTIC) {
            mEnvironmentInfo.preferredSearchEngine = "baidu";
        } else if (mEnvironmentInfo.canAccessGoogle) {
            mEnvironmentInfo.preferredSearchEngine = "google";
        } else {
            mEnvironmentInfo.preferredSearchEngine = "bing";
        }
        
        // 配置首页偏好
        mEnvironmentInfo.preferredHomepage = "https://main.eh-viewer.com/";
        
        Log.d(TAG, "Environment configured: SearchEngine=" + mEnvironmentInfo.preferredSearchEngine + 
              ", Homepage=" + mEnvironmentInfo.preferredHomepage);
    }
    
    /**
     * 获取当前环境信息
     */
    public EnvironmentInfo getEnvironmentInfo() {
        return mEnvironmentInfo;
    }
    
    /**
     * 获取检测状态
     */
    public DetectionStatus getDetectionStatus() {
        return mDetectionStatus;
    }
    
    /**
     * 是否为中国用户
     */
    public boolean isChineseUser() {
        return mEnvironmentInfo.isChineseUser;
    }
    
    /**
     * 获取地区类型
     */
    public RegionType getRegionType() {
        return mEnvironmentInfo.regionType;
    }
    
    /**
     * 获取网络环境
     */
    public NetworkEnvironment getNetworkEnvironment() {
        return mEnvironmentInfo.networkEnvironment;
    }
    
    /**
     * 获取首选搜索引擎
     */
    public String getPreferredSearchEngine() {
        return mEnvironmentInfo.preferredSearchEngine;
    }
    
    /**
     * 获取首选首页
     */
    public String getPreferredHomepage() {
        return mEnvironmentInfo.preferredHomepage;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.shutdown();
        }
    }
}