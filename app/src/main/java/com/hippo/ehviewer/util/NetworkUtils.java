package com.hippo.ehviewer.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * 网络状态工具类
 * 用于检测网络类型和连接质量，支持智能加载策略
 */
public class NetworkUtils {
    
    private static final String TAG = "NetworkUtils";
    
    public enum NetworkType {
        NONE,       // 无网络连接
        WIFI,       // WiFi连接
        MOBILE_4G,  // 4G移动网络
        MOBILE_3G,  // 3G移动网络
        MOBILE_2G,  // 2G移动网络
        MOBILE_OTHER, // 其他移动网络
        ETHERNET,   // 以太网连接
        UNKNOWN     // 未知网络类型
    }
    
    public enum NetworkSpeed {
        VERY_FAST,  // 非常快 (WiFi, 4G+)
        FAST,       // 快速 (4G)
        MODERATE,   // 中等 (3G)
        SLOW,       // 缓慢 (2G, 边缘网络)
        UNKNOWN     // 未知速度
    }
    
    /**
     * 获取当前网络类型
     */
    public static NetworkType getNetworkType(Context context) {
        if (context == null) {
            return NetworkType.UNKNOWN;
        }
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return NetworkType.NONE;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getNetworkTypeModern(cm);
        } else {
            return getNetworkTypeLegacy(cm);
        }
    }
    
    /**
     * Android M及以上版本的网络检测
     */
    private static NetworkType getNetworkTypeModern(ConnectivityManager cm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) {
                return NetworkType.NONE;
            }
            
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) {
                return NetworkType.UNKNOWN;
            }
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return NetworkType.WIFI;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return getMobileNetworkType(cm);
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return NetworkType.ETHERNET;
            }
        }
        
        return NetworkType.UNKNOWN;
    }
    
    /**
     * Android M以下版本的网络检测
     */
    @SuppressWarnings("deprecation")
    private static NetworkType getNetworkTypeLegacy(ConnectivityManager cm) {
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            return NetworkType.NONE;
        }
        
        int networkType = activeNetworkInfo.getType();
        if (networkType == ConnectivityManager.TYPE_WIFI) {
            return NetworkType.WIFI;
        } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
            return getMobileNetworkTypeLegacy(activeNetworkInfo);
        } else if (networkType == ConnectivityManager.TYPE_ETHERNET) {
            return NetworkType.ETHERNET;
        }
        
        return NetworkType.UNKNOWN;
    }
    
    /**
     * 获取移动网络类型（现代版本）
     */
    private static NetworkType getMobileNetworkType(ConnectivityManager cm) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                    if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        // 现代版本无法直接获取移动网络子类型，返回通用4G
                        return NetworkType.MOBILE_4G;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get mobile network type", e);
        }
        
        return NetworkType.MOBILE_OTHER;
    }
    
    /**
     * 获取移动网络类型（传统版本）
     */
    @SuppressWarnings("deprecation")
    private static NetworkType getMobileNetworkTypeLegacy(NetworkInfo networkInfo) {
        int subType = networkInfo.getSubtype();
        
        switch (subType) {
            // 2G网络
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NetworkType.MOBILE_2G;
                
            // 3G网络
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NetworkType.MOBILE_3G;
                
            // 4G网络
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NetworkType.MOBILE_4G;
                
            default:
                return NetworkType.MOBILE_OTHER;
        }
    }
    
    /**
     * 获取网络速度等级
     */
    public static NetworkSpeed getNetworkSpeed(Context context) {
        NetworkType networkType = getNetworkType(context);
        
        switch (networkType) {
            case WIFI:
            case ETHERNET:
                return NetworkSpeed.VERY_FAST;
                
            case MOBILE_4G:
                return NetworkSpeed.FAST;
                
            case MOBILE_3G:
                return NetworkSpeed.MODERATE;
                
            case MOBILE_2G:
                return NetworkSpeed.SLOW;
                
            case MOBILE_OTHER:
                return NetworkSpeed.MODERATE; // 假设中等速度
                
            default:
                return NetworkSpeed.UNKNOWN;
        }
    }
    
    /**
     * 检查网络是否连接
     */
    public static boolean isNetworkConnected(Context context) {
        return getNetworkType(context) != NetworkType.NONE;
    }
    
    /**
     * 检查是否为WiFi网络
     */
    public static boolean isWifiConnected(Context context) {
        return getNetworkType(context) == NetworkType.WIFI;
    }
    
    /**
     * 检查是否为移动网络
     */
    public static boolean isMobileConnected(Context context) {
        NetworkType type = getNetworkType(context);
        return type == NetworkType.MOBILE_2G || 
               type == NetworkType.MOBILE_3G || 
               type == NetworkType.MOBILE_4G || 
               type == NetworkType.MOBILE_OTHER;
    }
    
    /**
     * 检查网络是否适合大量数据传输
     */
    public static boolean isHighSpeedNetwork(Context context) {
        NetworkSpeed speed = getNetworkSpeed(context);
        return speed == NetworkSpeed.VERY_FAST || speed == NetworkSpeed.FAST;
    }
    
    /**
     * 获取网络类型的描述字符串
     */
    public static String getNetworkTypeString(Context context) {
        NetworkType type = getNetworkType(context);
        switch (type) {
            case NONE:
                return "无网络连接";
            case WIFI:
                return "WiFi";
            case MOBILE_4G:
                return "4G网络";
            case MOBILE_3G:
                return "3G网络";
            case MOBILE_2G:
                return "2G网络";
            case MOBILE_OTHER:
                return "移动网络";
            case ETHERNET:
                return "以太网";
            default:
                return "未知网络";
        }
    }
    
    /**
     * 根据网络类型获取建议的超时时间（毫秒）
     */
    public static int getRecommendedTimeout(Context context) {
        NetworkSpeed speed = getNetworkSpeed(context);
        switch (speed) {
            case VERY_FAST:
                return 10000;  // 10秒
            case FAST:
                return 15000;  // 15秒
            case MODERATE:
                return 20000;  // 20秒
            case SLOW:
                return 30000;  // 30秒
            default:
                return 15000;  // 默认15秒
        }
    }
    
    /**
     * 根据网络类型获取建议的并发连接数
     */
    public static int getRecommendedConcurrency(Context context) {
        NetworkSpeed speed = getNetworkSpeed(context);
        switch (speed) {
            case VERY_FAST:
                return 8;      // 最多8个并发连接
            case FAST:
                return 6;      // 6个并发连接
            case MODERATE:
                return 4;      // 4个并发连接
            case SLOW:
                return 2;      // 2个并发连接
            default:
                return 4;      // 默认4个
        }
    }
    
    /**
     * 根据网络类型获取建议的预加载页数
     */
    public static int getRecommendedPreloadPages(Context context) {
        NetworkSpeed speed = getNetworkSpeed(context);
        switch (speed) {
            case VERY_FAST:
                return 5;      // WiFi下预加载5页
            case FAST:
                return 3;      // 4G下预加载3页
            case MODERATE:
                return 2;      // 3G下预加载2页
            case SLOW:
                return 1;      // 2G下只预加载1页
            default:
                return 3;      // 默认3页
        }
    }
    
    /**
     * 网络状态监听器接口
     */
    public interface NetworkStateListener {
        void onNetworkChanged(NetworkType networkType);
        void onNetworkSpeedChanged(NetworkSpeed networkSpeed);
        void onConnectionLost();
        void onConnectionRestored();
    }
    
    /**
     * 获取网络统计信息
     */
    public static String getNetworkStatsString(Context context) {
        NetworkType type = getNetworkType(context);
        NetworkSpeed speed = getNetworkSpeed(context);
        
        return String.format("网络: %s | 速度: %s | 超时: %dms | 并发: %d | 预加载: %d页",
            getNetworkTypeString(context),
            speed.name(),
            getRecommendedTimeout(context),
            getRecommendedConcurrency(context),
            getRecommendedPreloadPages(context)
        );
    }
}