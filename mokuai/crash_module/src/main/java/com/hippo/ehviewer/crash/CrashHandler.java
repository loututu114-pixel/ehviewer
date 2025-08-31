package com.hippo.ehviewer.crash;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 崩溃处理器 - 统一处理应用崩溃和异常
 * 提供崩溃日志记录、异常上报、恢复机制等功能
 *
 * @author EhViewer Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static volatile CrashHandler instance;

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private CrashCallback callback;
    private boolean isEnabled = true;

    /**
     * 崩溃回调接口
     */
    public interface CrashCallback {
        void onCrash(Thread thread, Throwable throwable);
        void onCrashReported(String crashLog);
        boolean shouldRestartApp();
    }

    /**
     * 获取单例实例
     *
     * @param context 应用上下文
     * @return CrashHandler实例
     */
    public static CrashHandler getInstance(Context context) {
        if (instance == null) {
            synchronized (CrashHandler.class) {
                if (instance == null) {
                    instance = new CrashHandler(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private CrashHandler(Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 设置崩溃回调
     *
     * @param callback 崩溃回调
     */
    public void setCallback(CrashCallback callback) {
        this.callback = callback;
    }

    /**
     * 设置是否启用
     *
     * @param enabled 是否启用崩溃处理
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        if (!isEnabled) {
            // 如果未启用，使用默认处理器
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
            return;
        }

        try {
            // 处理崩溃
            handleCrash(thread, throwable);

            // 通知回调
            if (callback != null) {
                callback.onCrash(thread, throwable);
            }

            // 保存崩溃日志
            String crashLog = saveCrashLog(thread, throwable);

            // 上报崩溃日志
            if (callback != null) {
                callback.onCrashReported(crashLog);
            }

            // 延迟退出，让日志保存完成
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling crash", e);
        } finally {
            // 调用默认处理器或直接退出
            if (defaultHandler != null && defaultHandler != this) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                // 直接退出应用
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        }
    }

    /**
     * 处理崩溃
     */
    private void handleCrash(Thread thread, Throwable throwable) {
        Log.e(TAG, "Application crashed in thread: " + thread.getName(), throwable);

        // 可以在这里执行一些清理工作
        // 比如保存重要数据、清理缓存等
    }

    /**
     * 保存崩溃日志
     */
    private String saveCrashLog(Thread thread, Throwable throwable) {
        try {
            // 生成崩溃日志
            String crashLog = generateCrashLog(thread, throwable);

            // 保存到文件
            saveLogToFile(crashLog);

            // 保存到SharedPreferences（用于快速访问）
            saveLogToPreferences(crashLog);

            return crashLog;

        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash log", e);
            return "Failed to generate crash log: " + e.getMessage();
        }
    }

    /**
     * 生成崩溃日志
     */
    private String generateCrashLog(Thread thread, Throwable throwable) {
        StringBuilder log = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        log.append("=== EhViewer Crash Report ===\n");
        log.append("Time: ").append(dateFormat.format(new Date())).append("\n");
        log.append("Thread: ").append(thread.getName()).append(" (ID: ").append(thread.getId()).append(")\n");

        // 应用信息
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            log.append("App Version: ").append(pi.versionName).append(" (").append(pi.versionCode).append(")\n");
        } catch (Exception e) {
            log.append("App Version: Unknown\n");
        }

        // 设备信息
        log.append("Device: ").append(Build.MODEL).append(" (").append(Build.BRAND).append(")\n");
        log.append("Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        log.append("CPU ABI: ").append(Build.CPU_ABI).append("\n");

        // 堆栈信息
        log.append("\n=== Stack Trace ===\n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        log.append(sw.toString());

        return log.toString();
    }

    /**
     * 保存日志到文件
     */
    private void saveLogToFile(String crashLog) {
        try {
            File logDir = new File(context.getExternalFilesDir(null), "crash_logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String fileName = "crash_" + System.currentTimeMillis() + ".log";
            File logFile = new File(logDir, fileName);

            FileWriter writer = new FileWriter(logFile);
            writer.write(crashLog);
            writer.close();

            Log.i(TAG, "Crash log saved to: " + logFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash log to file", e);
        }
    }

    /**
     * 保存日志到SharedPreferences
     */
    private void saveLogToPreferences(String crashLog) {
        try {
            context.getSharedPreferences("crash_logs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_crash", crashLog)
                .putLong("last_crash_time", System.currentTimeMillis())
                .apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash log to preferences", e);
        }
    }

    /**
     * 获取上次崩溃日志
     */
    public String getLastCrashLog() {
        return context.getSharedPreferences("crash_logs", Context.MODE_PRIVATE)
            .getString("last_crash", "No crash log available");
    }

    /**
     * 获取上次崩溃时间
     */
    public long getLastCrashTime() {
        return context.getSharedPreferences("crash_logs", Context.MODE_PRIVATE)
            .getLong("last_crash_time", 0);
    }

    /**
     * 清除崩溃日志
     */
    public void clearCrashLogs() {
        context.getSharedPreferences("crash_logs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply();
    }

    /**
     * 获取所有崩溃日志文件
     */
    public File[] getCrashLogFiles() {
        File logDir = new File(context.getExternalFilesDir(null), "crash_logs");
        if (logDir.exists() && logDir.isDirectory()) {
            return logDir.listFiles((dir, name) -> name.startsWith("crash_") && name.endsWith(".log"));
        }
        return new File[0];
    }

    /**
     * 手动触发测试崩溃
     */
    public void triggerTestCrash() {
        throw new RuntimeException("Test crash triggered by CrashHandler");
    }

    /**
     * 手动记录异常
     */
    public void logException(Throwable throwable) {
        logException(Thread.currentThread(), throwable, "Manual exception logging");
    }

    /**
     * 手动记录异常（带标签）
     */
    public void logException(Thread thread, Throwable throwable, String tag) {
        String crashLog = generateCrashLog(thread, throwable);
        saveLogToFile(crashLog);

        Log.w(TAG, "Exception logged: " + tag, throwable);

        if (callback != null) {
            callback.onCrashReported(crashLog);
        }
    }
}
