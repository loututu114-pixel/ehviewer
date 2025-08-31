package com.hippo.ehviewer.notification;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Debug;
import android.os.Process;
import android.telephony.TelephonyManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 系统监控工具
 * 监控CPU、内存、网络等系统资源
 */
public class SystemMonitor {
    
    private static final String TAG = "SystemMonitor";
    
    /**
     * 获取CPU使用率
     */
    public static float getCPUUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();
            
            String[] toks = load.split(" +");
            
            long idle1 = Long.parseLong(toks[4]);
            long cpu1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + 
                       Long.parseLong(toks[3]) + Long.parseLong(toks[5]) +
                       Long.parseLong(toks[6]) + Long.parseLong(toks[7]);
            
            try {
                Thread.sleep(360);
            } catch (Exception e) {}
            
            reader.seek(0);
            load = reader.readLine();
            reader.close();
            
            toks = load.split(" +");
            
            long idle2 = Long.parseLong(toks[4]);
            long cpu2 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + 
                       Long.parseLong(toks[3]) + Long.parseLong(toks[5]) +
                       Long.parseLong(toks[6]) + Long.parseLong(toks[7]);
            
            float cpuUsage = (float)((cpu2 - cpu1) * 100) / ((cpu2 + idle2) - (cpu1 + idle1));
            
            return cpuUsage;
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * 获取应用CPU使用率
     */
    public static float getAppCPUUsage() {
        try {
            int pid = Process.myPid();
            RandomAccessFile reader = new RandomAccessFile("/proc/" + pid + "/stat", "r");
            String load = reader.readLine();
            reader.close();
            
            String[] toks = load.split(" ");
            
            long utime = Long.parseLong(toks[13]);
            long stime = Long.parseLong(toks[14]);
            long cutime = Long.parseLong(toks[15]);
            long cstime = Long.parseLong(toks[16]);
            
            long totalTime = utime + stime + cutime + cstime;
            
            return totalTime;
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * 获取内存信息
     */
    public static MemoryInfo getMemoryInfo(Context context) {
        ActivityManager activityManager = (ActivityManager) 
            context.getSystemService(Context.ACTIVITY_SERVICE);
        
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        MemoryInfo info = new MemoryInfo();
        info.totalMemory = memoryInfo.totalMem;
        info.availableMemory = memoryInfo.availMem;
        info.usedMemory = info.totalMemory - info.availableMemory;
        info.threshold = memoryInfo.threshold;
        info.lowMemory = memoryInfo.lowMemory;
        
        // 获取应用内存使用
        Debug.MemoryInfo debugInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(debugInfo);
        info.appMemory = debugInfo.getTotalPss() * 1024L;
        
        return info;
    }
    
    /**
     * 检查网络连接
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
    
    /**
     * 获取网络类型
     */
    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null) {
            return "NONE";
        }
        
        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            return "WIFI";
        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
            TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
            
            int networkType = tm.getNetworkType();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "4G";
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "5G";
                default:
                    return "MOBILE";
            }
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 获取电池信息
     */
    public static BatteryInfo getBatteryInfo(Context context) {
        BatteryInfo info = new BatteryInfo();
        
        try {
            // 读取电池电量
            BufferedReader br = new BufferedReader(
                new FileReader("/sys/class/power_supply/battery/capacity"));
            info.level = Integer.parseInt(br.readLine());
            br.close();
            
            // 读取充电状态
            br = new BufferedReader(
                new FileReader("/sys/class/power_supply/battery/status"));
            info.status = br.readLine();
            br.close();
            
            // 读取温度
            br = new BufferedReader(
                new FileReader("/sys/class/power_supply/battery/temp"));
            info.temperature = Integer.parseInt(br.readLine()) / 10.0f;
            br.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return info;
    }
    
    /**
     * 内存信息类
     */
    public static class MemoryInfo {
        public long totalMemory;
        public long availableMemory;
        public long usedMemory;
        public long threshold;
        public long appMemory;
        public boolean lowMemory;
        
        public float getUsagePercent() {
            return (float) usedMemory / totalMemory * 100;
        }
    }
    
    /**
     * 电池信息类
     */
    public static class BatteryInfo {
        public int level;
        public String status;
        public float temperature;
        
        public boolean isCharging() {
            return "Charging".equals(status) || "Full".equals(status);
        }
        
        public boolean isLowBattery() {
            return level < 15;
        }
    }
}