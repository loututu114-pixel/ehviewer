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
import androidx.core.app.NotificationCompat;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.MainActivity;
import java.util.concurrent.TimeUnit;

/**
 * 应用保活服务 - 多策略组合保证应用持续运行
 * 使用前台服务、JobScheduler、无声音频等技术
 */
public class AppKeepAliveService extends Service {
    private static final String TAG = "AppKeepAliveService";
    private static final int NOTIFICATION_ID = 10001;
    private static final String CHANNEL_ID = "keep_alive_channel";
    private static final int JOB_ID = 10002;
    
    private PowerManager.WakeLock wakeLock;
    private AudioTrack silentAudioTrack;
    private Handler handler;
    private ScreenStateReceiver screenReceiver;
    private boolean isServiceRunning = false;
    
    // 单例模式
    private static AppKeepAliveService instance;
    
    public static AppKeepAliveService getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        isServiceRunning = true;
        
        // 初始化Handler
        handler = new Handler(Looper.getMainLooper());
        
        // 启动前台服务
        startForegroundService();
        
        // 获取WakeLock
        acquireWakeLock();
        
        // 播放无声音频（某些设备有效）
        playSilentAudio();
        
        // 注册屏幕状态监听
        registerScreenStateReceiver();
        
        // 启动JobScheduler定时任务
        scheduleJob();
        
        // 定期检查服务状态
        startServiceCheck();
    }
    
    /**
     * 启动前台服务
     */
    private void startForegroundService() {
        createNotificationChannel();
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EhViewer 浏览器")
            .setContentText("浏览器服务运行中，点击打开")
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
        startForeground(NOTIFICATION_ID, notification);
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "浏览器后台服务",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("保持浏览器在后台运行");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 获取WakeLock保持CPU运行
     */
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EhViewer::KeepAliveWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L); // 10分钟后自动释放
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
                while (isServiceRunning) {
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
     * 注册屏幕状态监听器
     */
    private void registerScreenStateReceiver() {
        screenReceiver = new ScreenStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
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
     * 使用JobScheduler定期唤醒
     */
    private void scheduleJob() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID,
                new ComponentName(this, KeepAliveJobService.class));
            
            // 设置定期执行（15分钟）
            builder.setPeriodic(TimeUnit.MINUTES.toMillis(15));
            
            // 设置持久化
            builder.setPersisted(true);
            
            // 设置重要性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setImportantWhileForeground(true);
            }
            
            scheduler.schedule(builder.build());
        }
    }
    
    /**
     * 定期检查服务状态
     */
    private void startServiceCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    // 检查并刷新WakeLock
                    if (wakeLock != null && !wakeLock.isHeld()) {
                        acquireWakeLock();
                    }
                    
                    // 继续下一次检查
                    handler.postDelayed(this, 60000); // 每分钟检查一次
                }
            }
        }, 60000);
    }
    
    /**
     * 确保服务正在运行
     */
    private void ensureServiceRunning() {
        if (!isServiceRunning) {
            Intent intent = new Intent(this, AppKeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 返回START_STICKY，系统会尝试重新创建服务
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        isServiceRunning = false;
        instance = null;
        
        // 释放WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // 停止音频
        if (silentAudioTrack != null) {
            silentAudioTrack.stop();
            silentAudioTrack.release();
        }
        
        // 注销接收器
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
        
        // 移除Handler回调
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        // 尝试重启服务
        Intent restartIntent = new Intent(this, AppKeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
        
        super.onDestroy();
    }
    
    /**
     * JobService用于定期唤醒
     */
    public static class KeepAliveJobService extends JobService {
        @Override
        public boolean onStartJob(JobParameters params) {
            // 确保主服务运行
            Intent intent = new Intent(this, AppKeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            
            // 任务完成
            jobFinished(params, false);
            return false;
        }
        
        @Override
        public boolean onStopJob(JobParameters params) {
            return false;
        }
    }
}