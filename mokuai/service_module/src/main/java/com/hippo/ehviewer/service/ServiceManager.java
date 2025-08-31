package com.hippo.ehviewer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.TimeUnit;

/**
 * 服务管理器 - 统一管理各种后台服务
 * 基于AppKeepAliveService的多策略保活功能
 *
 * @author EhViewer Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ServiceManager {

    private static final String TAG = "ServiceManager";
    private static final int NOTIFICATION_ID = 10001;
    private static final String CHANNEL_ID = "keep_alive_channel";
    private static final int JOB_ID = 10002;

    private static volatile ServiceManager instance;

    private final Context context;
    private final Handler mainHandler;

    // 保活相关组件
    private PowerManager.WakeLock wakeLock;
    private AudioTrack silentAudioTrack;
    private ScreenStateReceiver screenReceiver;
    private ServiceStatusListener statusListener;

    // 服务状态
    private boolean isKeepAliveEnabled = false;
    private boolean isServiceRunning = false;

    // 服务配置
    public static class ServiceConfig {
        private boolean enableForegroundService = true;
        private boolean enableWakeLock = true;
        private boolean enableSilentAudio = true;
        private boolean enableJobScheduler = true;
        private boolean enableScreenReceiver = true;
        private int serviceCheckInterval = 30000; // 30秒
        private String notificationTitle = "EhViewer 服务";
        private String notificationText = "后台服务运行中";

        public static class Builder {
            private final ServiceConfig config = new ServiceConfig();

            public Builder enableForegroundService(boolean enable) {
                config.enableForegroundService = enable;
                return this;
            }

            public Builder enableWakeLock(boolean enable) {
                config.enableWakeLock = enable;
                return this;
            }

            public Builder enableSilentAudio(boolean enable) {
                config.enableSilentAudio = enable;
                return this;
            }

            public Builder enableJobScheduler(boolean enable) {
                config.enableJobScheduler = enable;
                return this;
            }

            public Builder enableScreenReceiver(boolean enable) {
                config.enableScreenReceiver = enable;
                return this;
            }

            public Builder setServiceCheckInterval(int interval) {
                config.serviceCheckInterval = interval;
                return this;
            }

            public Builder setNotificationTitle(String title) {
                config.notificationTitle = title;
                return this;
            }

            public Builder setNotificationText(String text) {
                config.notificationText = text;
                return this;
            }

            public ServiceConfig build() {
                return config;
            }
        }
    }

    // 默认配置
    private ServiceConfig config = new ServiceConfig();

    /**
     * 服务状态监听器接口
     */
    public interface ServiceStatusListener {
        void onServiceStarted();
        void onServiceStopped();
        void onServiceError(String error);
        void onWakeLockAcquired();
        void onWakeLockReleased();
    }

    /**
     * 获取单例实例
     */
    public static ServiceManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ServiceManager.class) {
                if (instance == null) {
                    instance = new ServiceManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ServiceManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 设置配置
     */
    public void setConfig(ServiceConfig config) {
        this.config = config != null ? config : new ServiceConfig();
    }

    /**
     * 设置状态监听器
     */
    public void setStatusListener(ServiceStatusListener listener) {
        this.statusListener = listener;
    }

    /**
     * 启动应用保活服务
     */
    public void startKeepAliveService(boolean enable) {
        if (enable) {
            startKeepAliveService();
        } else {
            stopKeepAliveService();
        }
    }

    /**
     * 启动保活服务
     */
    private void startKeepAliveService() {
        if (isServiceRunning) {
            Log.d(TAG, "Keep alive service is already running");
            return;
        }

        try {
            Intent serviceIntent = new Intent(context, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            isKeepAliveEnabled = true;
            isServiceRunning = true;

            if (statusListener != null) {
                mainHandler.post(() -> statusListener.onServiceStarted());
            }

            Log.i(TAG, "Keep alive service started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start keep alive service", e);
            if (statusListener != null) {
                mainHandler.post(() -> statusListener.onServiceError("Failed to start service: " + e.getMessage()));
            }
        }
    }

    /**
     * 停止保活服务
     */
    public void stopKeepAliveService() {
        if (!isServiceRunning) {
            Log.d(TAG, "Keep alive service is not running");
            return;
        }

        try {
            Intent serviceIntent = new Intent(context, KeepAliveService.class);
            context.stopService(serviceIntent);

            isKeepAliveEnabled = false;
            isServiceRunning = false;

            // 清理资源
            releaseWakeLock();
            stopSilentAudio();
            unregisterScreenStateReceiver();

            if (statusListener != null) {
                mainHandler.post(() -> statusListener.onServiceStopped());
            }

            Log.i(TAG, "Keep alive service stopped successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop keep alive service", e);
            if (statusListener != null) {
                mainHandler.post(() -> statusListener.onServiceError("Failed to stop service: " + e.getMessage()));
            }
        }
    }

    /**
     * 检查服务是否正在运行
     */
    public boolean isKeepAliveServiceRunning() {
        return isServiceRunning && KeepAliveService.getInstance() != null;
    }

    /**
     * 启动定时任务
     */
    public void startScheduledTask(int intervalMinutes, int jobId) {
        if (!config.enableJobScheduler) {
            Log.d(TAG, "JobScheduler is disabled");
            return;
        }

        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                Log.e(TAG, "JobScheduler not available");
                return;
            }

            JobInfo.Builder builder = new JobInfo.Builder(jobId,
                new ComponentName(context, ScheduledTaskService.class))
                .setPeriodic(intervalMinutes * 60 * 1000L) // 分钟转毫秒
                .setPersisted(true); // 设备重启后仍然有效

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setEstimatedNetworkBytes(0, 0); // 不使用网络
            }

            JobInfo jobInfo = builder.build();
            int result = jobScheduler.schedule(jobInfo);

            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.i(TAG, "Scheduled task started successfully with job ID: " + jobId);
            } else {
                Log.e(TAG, "Failed to schedule task, result: " + result);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to start scheduled task", e);
        }
    }

    /**
     * 停止定时任务
     */
    public void stopScheduledTask(int jobId) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancel(jobId);
                Log.i(TAG, "Scheduled task stopped with job ID: " + jobId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop scheduled task", e);
        }
    }

    /**
     * 获取WakeLock
     */
    private void acquireWakeLock() {
        if (!config.enableWakeLock) {
            return;
        }

        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                return;
            }

            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EhViewer:KeepAliveWakeLock"
            );

            wakeLock.acquire(10 * 60 * 1000L); // 10分钟

            if (statusListener != null) {
                mainHandler.post(() -> statusListener.onWakeLockAcquired());
            }

            Log.d(TAG, "WakeLock acquired");

        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire WakeLock", e);
        }
    }

    /**
     * 释放WakeLock
     */
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
                wakeLock = null;

                if (statusListener != null) {
                    mainHandler.post(() -> statusListener.onWakeLockReleased());
                }

                Log.d(TAG, "WakeLock released");

            } catch (Exception e) {
                Log.e(TAG, "Failed to release WakeLock", e);
            }
        }
    }

    /**
     * 播放无声音频
     */
    private void playSilentAudio() {
        if (!config.enableSilentAudio) {
            return;
        }

        try {
            // 创建无声音频轨道
            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

            int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

            silentAudioTrack = new AudioTrack(
                audioAttributes,
                new AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(audioFormat)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                0
            );

            silentAudioTrack.play();

            // 生成静音数据
            byte[] silentBuffer = new byte[bufferSize];
            silentAudioTrack.write(silentBuffer, 0, silentBuffer.length);

            Log.d(TAG, "Silent audio started");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start silent audio", e);
        }
    }

    /**
     * 停止无声音频
     */
    private void stopSilentAudio() {
        if (silentAudioTrack != null) {
            try {
                if (silentAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    silentAudioTrack.stop();
                }
                silentAudioTrack.release();
                silentAudioTrack = null;

                Log.d(TAG, "Silent audio stopped");

            } catch (Exception e) {
                Log.e(TAG, "Failed to stop silent audio", e);
            }
        }
    }

    /**
     * 注册屏幕状态接收器
     */
    private void registerScreenStateReceiver() {
        if (!config.enableScreenReceiver) {
            return;
        }

        try {
            screenReceiver = new ScreenStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);

            context.registerReceiver(screenReceiver, filter);
            Log.d(TAG, "Screen state receiver registered");

        } catch (Exception e) {
            Log.e(TAG, "Failed to register screen state receiver", e);
        }
    }

    /**
     * 注销屏幕状态接收器
     */
    private void unregisterScreenStateReceiver() {
        if (screenReceiver != null) {
            try {
                context.unregisterReceiver(screenReceiver);
                screenReceiver = null;
                Log.d(TAG, "Screen state receiver unregistered");

            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister screen state receiver", e);
            }
        }
    }

    /**
     * 屏幕状态接收器
     */
    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case Intent.ACTION_SCREEN_ON:
                    Log.d(TAG, "Screen turned on");
                    ensureServiceRunning();
                    break;

                case Intent.ACTION_SCREEN_OFF:
                    Log.d(TAG, "Screen turned off");
                    // 屏幕关闭时确保服务仍在运行
                    ensureServiceRunning();
                    break;

                case Intent.ACTION_USER_PRESENT:
                    Log.d(TAG, "User present");
                    break;
            }
        }
    }

    /**
     * 确保服务正在运行
     */
    private void ensureServiceRunning() {
        if (!isServiceRunning && isKeepAliveEnabled) {
            Log.i(TAG, "Service not running, restarting...");
            startKeepAliveService();
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        stopKeepAliveService();
        instance = null;
    }

    /**
     * 保活服务
     */
    public static class KeepAliveService extends Service {
        private static KeepAliveService instance;
        private Handler handler;
        private ServiceManager serviceManager;

        @Override
        public void onCreate() {
            super.onCreate();
            instance = this;
            handler = new Handler(Looper.getMainLooper());
            serviceManager = ServiceManager.getInstance(this);

            startForegroundService();
            serviceManager.acquireWakeLock();
            serviceManager.playSilentAudio();
            serviceManager.registerScreenStateReceiver();
            serviceManager.scheduleJob();
            startServiceCheck();
        }

        private void startForegroundService() {
            createNotificationChannel();

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(serviceManager.config.notificationTitle)
                .setContentText(serviceManager.config.notificationText)
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "应用保活服务",
                    NotificationManager.IMPORTANCE_MIN
                );
                channel.setDescription("保持应用后台运行的服务");
                channel.setShowBadge(false);

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }
        }

        private void scheduleJob() {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                return;
            }

            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID,
                new ComponentName(this, ScheduledTaskService.class))
                .setPeriodic(15 * 60 * 1000L) // 15分钟
                .setPersisted(true);

            JobInfo jobInfo = builder.build();
            jobScheduler.schedule(jobInfo);
        }

        private void startServiceCheck() {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 检查服务是否正常运行
                    if (instance != null) {
                        handler.postDelayed(this, serviceManager.config.serviceCheckInterval);
                    }
                }
            }, serviceManager.config.serviceCheckInterval);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            instance = null;
            serviceManager.releaseWakeLock();
            serviceManager.stopSilentAudio();
            serviceManager.unregisterScreenStateReceiver();
        }

        public static KeepAliveService getInstance() {
            return instance;
        }

        // 占位符类，需要用户提供
        private static class MainActivity {}
    }

    /**
     * 定时任务服务
     */
    public static class ScheduledTaskService extends JobService {
        @Override
        public boolean onStartJob(JobParameters params) {
            // 执行定时任务
            performScheduledTask();
            return false; // 任务已完成
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            return false; // 不需要重试
        }

        private void performScheduledTask() {
            // 执行预定的后台任务
            Log.d(TAG, "Performing scheduled task");
        }
    }
}
 * 支持应用保活、定时任务、资源监控等功能
 *
 * @author EhViewer Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class ServiceManager {

    private static final String TAG = "ServiceManager";
    private static volatile ServiceManager instance;

    private final Context context;
    private final Handler mainHandler;
    private PowerManager.WakeLock wakeLock;
    private AudioTrack silentAudioTrack;
    private ScreenStateReceiver screenReceiver;
    private ServiceStatusListener statusListener;
    private boolean isKeepAliveEnabled = false;

    /**
     * 服务状态监听器
     */
    public interface ServiceStatusListener {
        void onServiceStarted();
        void onServiceStopped();
        void onServiceError(String error);
    }

    /**
     * 获取单例实例
     *
     * @param context 应用上下文
     * @return ServiceManager实例
     */
    public static ServiceManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ServiceManager.class) {
                if (instance == null) {
                    instance = new ServiceManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ServiceManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 设置状态监听器
     *
     * @param listener 状态监听器
     */
    public void setStatusListener(ServiceStatusListener listener) {
        this.statusListener = listener;
    }

    /**
     * 启动应用保活服务
     *
     * @param enable 是否启用保活
     */
    public void startKeepAliveService(boolean enable) {
        this.isKeepAliveEnabled = enable;

        if (!enable) {
            stopKeepAliveService();
            return;
        }

        Intent intent = new Intent(context, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        if (statusListener != null) {
            statusListener.onServiceStarted();
        }
    }

    /**
     * 停止应用保活服务
     */
    public void stopKeepAliveService() {
        Intent intent = new Intent(context, KeepAliveService.class);
        context.stopService(intent);
        isKeepAliveEnabled = false;

        if (statusListener != null) {
            statusListener.onServiceStopped();
        }
    }

    /**
     * 检查服务是否正在运行
     *
     * @return true如果服务正在运行
     */
    public boolean isKeepAliveServiceRunning() {
        return isKeepAliveEnabled && KeepAliveService.getInstance() != null;
    }

    /**
     * 启动定时任务服务
     *
     * @param intervalMinutes 执行间隔（分钟）
     * @param jobId 任务ID
     */
    public void startScheduledTask(int intervalMinutes, int jobId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            JobInfo.Builder builder = new JobInfo.Builder(jobId,
                new ComponentName(context, ScheduledTaskService.class));

            builder.setPeriodic(TimeUnit.MINUTES.toMillis(intervalMinutes));
            builder.setPersisted(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setImportantWhileForeground(true);
            }

            scheduler.schedule(builder.build());
        }
    }

    /**
     * 停止定时任务服务
     *
     * @param jobId 任务ID
     */
    public void stopScheduledTask(int jobId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.cancel(jobId);
        }
    }

    /**
     * 获取WakeLock保持CPU运行
     */
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EhViewer::ServiceWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L); // 10分钟后自动释放
        }
    }

    /**
     * 释放WakeLock
     */
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    /**
     * 播放无声音频（提高优先级）
     */
    private void playSilentAudio() {
        try {
            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

                AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(audioFormat)
                    .setChannelMask(channelConfig)
                    .build();

                silentAudioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            } else {
                silentAudioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                );
            }

            // 播放无声音频
            silentAudioTrack.play();

            // 写入静音数据
            byte[] silentBuffer = new byte[bufferSize];
            new Thread(() -> {
                while (isKeepAliveEnabled) {
                    try {
                        silentAudioTrack.write(silentBuffer, 0, bufferSize);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止无声音频
     */
    private void stopSilentAudio() {
        if (silentAudioTrack != null) {
            silentAudioTrack.stop();
            silentAudioTrack.release();
            silentAudioTrack = null;
        }
    }

    /**
     * 注册屏幕状态监听器
     */
    private void registerScreenStateReceiver() {
        screenReceiver = new ScreenStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        context.registerReceiver(screenReceiver, filter);
    }

    /**
     * 注销屏幕状态监听器
     */
    private void unregisterScreenStateReceiver() {
        if (screenReceiver != null) {
            context.unregisterReceiver(screenReceiver);
            screenReceiver = null;
        }
    }

    /**
     * 屏幕状态接收器
     */
    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // 屏幕关闭时，重新获取WakeLock
                acquireWakeLock();
            } else if (Intent.ACTION_SCREEN_ON.equals(action) ||
                      Intent.ACTION_USER_PRESENT.equals(action)) {
                // 屏幕打开或解锁时，确保服务正常
                ensureServiceRunning();
            }
        }
    }

    /**
     * 确保服务正在运行
     */
    private void ensureServiceRunning() {
        if (isKeepAliveEnabled && KeepAliveService.getInstance() == null) {
            startKeepAliveService(true);
        }
    }

    /**
     * 应用保活服务
     */
    public static class KeepAliveService extends Service {

        private static KeepAliveService instance;
        private ServiceManager serviceManager;

        @Override
        public void onCreate() {
            super.onCreate();
            instance = this;

            // 获取ServiceManager实例并初始化
            serviceManager = ServiceManager.getInstance(this);

            // 启动前台服务
            startForegroundService();

            // 获取WakeLock
            serviceManager.acquireWakeLock();

            // 播放无声音频
            serviceManager.playSilentAudio();

            // 注册屏幕状态监听
            serviceManager.registerScreenStateReceiver();

            // 启动JobScheduler定时任务
            serviceManager.startScheduledTask(15, 10002);

            // 定期检查服务状态
            startServiceCheck();
        }

        private void startForegroundService() {
            createNotificationChannel();

            Intent notificationIntent = new Intent(this, com.hippo.ehviewer.ui.MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "keep_alive_channel")
                .setContentTitle("EhViewer 服务")
                .setContentText("后台服务运行中")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
            }

            Notification notification = builder.build();
            startForeground(10001, notification);
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    "keep_alive_channel",
                    "后台服务",
                    NotificationManager.IMPORTANCE_MIN
                );
                channel.setDescription("保持应用在后台运行");
                channel.setShowBadge(false);
                channel.setSound(null, null);
                channel.enableVibration(false);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
        }

        private void startServiceCheck() {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (serviceManager.isKeepAliveEnabled) {
                        // 检查并刷新WakeLock
                        if (serviceManager.wakeLock != null && !serviceManager.wakeLock.isHeld()) {
                            serviceManager.acquireWakeLock();
                        }

                        // 继续下一次检查
                        handler.postDelayed(this, 60000); // 每分钟检查一次
                    }
                }
            }, 60000);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            instance = null;

            // 释放资源
            serviceManager.releaseWakeLock();
            serviceManager.stopSilentAudio();
            serviceManager.unregisterScreenStateReceiver();

            // 尝试重启服务
            Intent restartIntent = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }

            super.onDestroy();
        }

        public static KeepAliveService getInstance() {
            return instance;
        }
    }

    /**
     * 定时任务服务
     */
    public static class ScheduledTaskService extends JobService {

        @Override
        public boolean onStartJob(JobParameters params) {
            // 执行定时任务
            performScheduledTask();

            // 任务完成
            jobFinished(params, false);
            return false;
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            return false;
        }

        private void performScheduledTask() {
            // 执行具体的定时任务逻辑
            // 例如：清理缓存、检查更新、数据同步等
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        stopKeepAliveService();
        releaseWakeLock();
        stopSilentAudio();
        unregisterScreenStateReceiver();
        instance = null;
    }
}
