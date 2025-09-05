package com.hippo.ehviewer.client;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.EhApplication;

/**
 * 自适应带宽管理器
 * 根据网络状况动态调整请求策略
 */
public class BandwidthManager {

    private static final String TAG = "BandwidthManager";

    // 网络类型常量
    public enum NetworkType {
        WIFI_FAST,      // 高速WiFi
        WIFI_SLOW,      // 慢速WiFi
        MOBILE_4G,      // 4G移动网络
        MOBILE_3G,      // 3G移动网络
        MOBILE_2G,      // 2G移动网络
        UNKNOWN         // 未知网络
    }

    // 并发控制参数
    private static final int WIFI_FAST_MAX_CONCURRENT = 8;
    private static final int WIFI_SLOW_MAX_CONCURRENT = 4;
    private static final int MOBILE_4G_MAX_CONCURRENT = 4;
    private static final int MOBILE_3G_MAX_CONCURRENT = 2;
    private static final int MOBILE_2G_MAX_CONCURRENT = 1;

    // 超时时间参数（毫秒）
    private static final int WIFI_CONNECT_TIMEOUT = 5000;
    private static final int WIFI_READ_TIMEOUT = 10000;
    private static final int MOBILE_CONNECT_TIMEOUT = 8000;
    private static final int MOBILE_READ_TIMEOUT = 15000;

    private static BandwidthManager sInstance;

    private NetworkType mCurrentNetworkType = NetworkType.UNKNOWN;
    private ConnectivityManager mConnectivityManager;

    private BandwidthManager(Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        detectNetworkType();
    }

    public static synchronized BandwidthManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new BandwidthManager(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * 检测当前网络类型
     */
    public void detectNetworkType() {
        if (mConnectivityManager == null) {
            mCurrentNetworkType = NetworkType.UNKNOWN;
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = mConnectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
                    if (capabilities != null) {
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            // 检查WiFi带宽
                            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                                mCurrentNetworkType = NetworkType.WIFI_FAST;
                            } else {
                                mCurrentNetworkType = NetworkType.WIFI_SLOW;
                            }
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            // 检查移动网络类型
                            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                                mCurrentNetworkType = NetworkType.MOBILE_4G;
                            } else {
                                // 简单检测，通过下行带宽估算
                                mCurrentNetworkType = NetworkType.MOBILE_3G;
                            }
                        } else {
                            mCurrentNetworkType = NetworkType.UNKNOWN;
                        }
                    }
                }
            } else {
                // API 23以下的兼容处理
                NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    int type = activeNetwork.getType();
                    int subtype = activeNetwork.getSubtype();

                    if (type == ConnectivityManager.TYPE_WIFI) {
                        mCurrentNetworkType = NetworkType.WIFI_FAST;
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        switch (subtype) {
                            case 1:  // GPRS
                            case 2:  // EDGE
                            case 4:  // CDMA
                                mCurrentNetworkType = NetworkType.MOBILE_2G;
                                break;
                            case 3:  // UMTS
                            case 5:  // EVDO_0
                            case 6:  // EVDO_A
                            case 8:  // HSDPA
                            case 9:  // HSUPA
                            case 10: // HSPA
                                mCurrentNetworkType = NetworkType.MOBILE_3G;
                                break;
                            case 13: // LTE
                            case 14: // EHRPD
                            case 15: // HSPAP
                            case 17: // TD_SCDMA
                                mCurrentNetworkType = NetworkType.MOBILE_4G;
                                break;
                            default:
                                mCurrentNetworkType = NetworkType.MOBILE_3G;
                                break;
                        }
                    } else {
                        mCurrentNetworkType = NetworkType.UNKNOWN;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting network type", e);
            mCurrentNetworkType = NetworkType.UNKNOWN;
        }

        Log.d(TAG, "Detected network type: " + mCurrentNetworkType);
    }

    /**
     * 获取推荐的最大并发请求数
     */
    public int getRecommendedMaxConcurrentRequests() {
        switch (mCurrentNetworkType) {
            case WIFI_FAST:
                return WIFI_FAST_MAX_CONCURRENT;
            case WIFI_SLOW:
                return WIFI_SLOW_MAX_CONCURRENT;
            case MOBILE_4G:
                return MOBILE_4G_MAX_CONCURRENT;
            case MOBILE_3G:
                return MOBILE_3G_MAX_CONCURRENT;
            case MOBILE_2G:
                return MOBILE_2G_MAX_CONCURRENT;
            default:
                return MOBILE_3G_MAX_CONCURRENT; // 默认中等配置
        }
    }

    /**
     * 获取推荐的连接超时时间
     */
    public int getRecommendedConnectTimeout() {
        switch (mCurrentNetworkType) {
            case WIFI_FAST:
            case WIFI_SLOW:
                return WIFI_CONNECT_TIMEOUT;
            case MOBILE_4G:
            case MOBILE_3G:
            case MOBILE_2G:
                return MOBILE_CONNECT_TIMEOUT;
            default:
                return WIFI_CONNECT_TIMEOUT;
        }
    }

    /**
     * 获取推荐的读取超时时间
     */
    public int getRecommendedReadTimeout() {
        switch (mCurrentNetworkType) {
            case WIFI_FAST:
            case WIFI_SLOW:
                return WIFI_READ_TIMEOUT;
            case MOBILE_4G:
            case MOBILE_3G:
            case MOBILE_2G:
                return MOBILE_READ_TIMEOUT;
            default:
                return WIFI_READ_TIMEOUT;
        }
    }

    /**
     * 判断是否应该启用预加载
     */
    public boolean shouldEnablePreloading() {
        switch (mCurrentNetworkType) {
            case WIFI_FAST:
                return true;    // 高速WiFi，启用预加载
            case WIFI_SLOW:
                return true;    // 慢速WiFi，仍启用预加载
            case MOBILE_4G:
                return false;   // 4G网络，不启用预加载节省流量
            case MOBILE_3G:
            case MOBILE_2G:
                return false;   // 低速移动网络，不启用预加载
            default:
                return false;   // 未知网络，保守策略
        }
    }

    /**
     * 判断是否应该启用图片压缩
     */
    public boolean shouldEnableImageCompression() {
        switch (mCurrentNetworkType) {
            case WIFI_FAST:
                return false;   // 高速WiFi，不需要压缩
            case WIFI_SLOW:
                return false;   // 慢速WiFi，仍不压缩
            case MOBILE_4G:
                return true;    // 4G网络，启用轻度压缩
            case MOBILE_3G:
            case MOBILE_2G:
                return true;    // 低速移动网络，启用压缩
            default:
                return true;    // 未知网络，启用压缩
        }
    }

    /**
     * 获取当前网络类型
     */
    public NetworkType getCurrentNetworkType() {
        return mCurrentNetworkType;
    }

    /**
     * 刷新网络状态
     */
    public void refreshNetworkStatus() {
        detectNetworkType();
    }

    /**
     * 获取网络状态描述
     */
    public String getNetworkStatusDescription() {
        String typeDesc;
        switch (mCurrentNetworkType) {
            case WIFI_FAST:
                typeDesc = "高速WiFi";
                break;
            case WIFI_SLOW:
                typeDesc = "WiFi";
                break;
            case MOBILE_4G:
                typeDesc = "4G移动网络";
                break;
            case MOBILE_3G:
                typeDesc = "3G移动网络";
                break;
            case MOBILE_2G:
                typeDesc = "2G移动网络";
                break;
            default:
                typeDesc = "未知网络";
                break;
        }
        return "当前网络: " + typeDesc +
               " | 并发请求: " + getRecommendedMaxConcurrentRequests() +
               " | 预加载: " + (shouldEnablePreloading() ? "启用" : "禁用");
    }
}
