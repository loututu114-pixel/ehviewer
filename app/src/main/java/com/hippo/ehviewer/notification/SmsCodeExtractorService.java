package com.hippo.ehviewer.notification;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import com.hippo.ehviewer.R;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * 短信验证码提取服务
 * 自动识别短信中的验证码并提供快速复制/填充功能
 */
public class SmsCodeExtractorService extends Service {
    
    private static final String TAG = "SmsCodeExtractor";
    
    // 验证码模式
    private static final Pattern[] CODE_PATTERNS = {
        Pattern.compile("(?:验证码|校验码|确认码|code)[：:是为]?\\s*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:您的|你的)?(?:验证码|动态码|校验码)[：:是为]?\\s*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9]{4,8})\\s*(?:是您的验证码|为您的验证码|是你的验证码)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b([0-9]{6})\\b"), // 独立的6位数字
        Pattern.compile("\\b([0-9]{4})\\b"), // 独立的4位数字
        Pattern.compile("(?:SMS|OTP|PIN)[：:是为]?\\s*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:认证码|授权码|激活码)[：:是为]?\\s*([0-9A-Z]{4,8})", Pattern.CASE_INSENSITIVE)
    };
    
    // 发送者模式（用于识别验证码短信）
    private static final String[] CODE_SENDERS = {
        "银行", "支付", "验证", "认证", "登录", "注册",
        "淘宝", "京东", "美团", "滴滴", "微信", "支付宝",
        "Bank", "Verify", "Auth", "Login", "Register"
    };
    
    private NotificationManager notificationManager;
    private ClipboardManager clipboardManager;
    private Handler mainHandler;
    
    // 短信接收器
    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                handleSmsReceived(intent);
            }
        }
    };
    
    // 短信内容观察者
    private ContentObserver smsObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            
            if (uri != null) {
                handleNewSms(uri);
            }
        }
    };
    
    // 最近的验证码
    private static class VerificationCode {
        String code;
        String sender;
        String fullMessage;
        long timestamp;
        
        VerificationCode(String code, String sender, String fullMessage) {
            this.code = code;
            this.sender = sender;
            this.fullMessage = fullMessage;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private List<VerificationCode> recentCodes = new ArrayList<>();
    private static final int MAX_RECENT_CODES = 5;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        notificationManager = NotificationManager.getInstance(this);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 注册短信接收器
        registerSmsReceiver();
        
        // 注册短信内容观察者
        registerSmsObserver();
        
        Log.d(TAG, "SMS Code Extractor Service started");
    }
    
    /**
     * 注册短信接收器
     */
    private void registerSmsReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        
        registerReceiver(smsReceiver, filter);
    }
    
    /**
     * 注册短信观察者
     */
    private void registerSmsObserver() {
        getContentResolver().registerContentObserver(
            Uri.parse("content://sms/inbox"),
            true,
            smsObserver
        );
    }
    
    /**
     * 处理接收到的短信
     */
    private void handleSmsReceived(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        
        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) return;
            
            for (Object pdu : pdus) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                
                String sender = smsMessage.getDisplayOriginatingAddress();
                String messageBody = smsMessage.getMessageBody();
                
                // 处理短信内容
                processMessage(sender, messageBody);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS", e);
        }
    }
    
    /**
     * 处理新短信（从数据库）
     */
    private void handleNewSms(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                uri,
                new String[]{"address", "body", "date"},
                null,
                null,
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                String sender = cursor.getString(0);
                String body = cursor.getString(1);
                
                processMessage(sender, body);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading SMS", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    /**
     * 处理消息内容
     */
    private void processMessage(String sender, String messageBody) {
        if (sender == null || messageBody == null) return;
        
        // 检查是否可能是验证码短信
        if (!isPotentialCodeMessage(sender, messageBody)) {
            return;
        }
        
        // 提取验证码
        String code = extractVerificationCode(messageBody);
        
        if (code != null) {
            Log.d(TAG, "Verification code detected: " + code);
            
            // 保存验证码
            VerificationCode verificationCode = new VerificationCode(code, sender, messageBody);
            saveRecentCode(verificationCode);
            
            // 发送通知
            sendCodeNotification(verificationCode);
        }
    }
    
    /**
     * 检查是否可能是验证码短信
     */
    private boolean isPotentialCodeMessage(String sender, String message) {
        // 检查发送者
        for (String keyword : CODE_SENDERS) {
            if (sender.toLowerCase().contains(keyword.toLowerCase()) ||
                message.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        // 检查消息内容关键词
        String[] keywords = {"验证码", "校验码", "确认码", "动态码", "认证码", 
                           "code", "otp", "pin", "verification", "verify"};
        
        for (String keyword : keywords) {
            if (message.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        // 检查是否包含4-8位数字
        return message.matches(".*\\b[0-9]{4,8}\\b.*");
    }
    
    /**
     * 提取验证码
     */
    private String extractVerificationCode(String message) {
        // 尝试各种模式
        for (Pattern pattern : CODE_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return null;
    }
    
    /**
     * 保存最近的验证码
     */
    private void saveRecentCode(VerificationCode code) {
        recentCodes.add(0, code);
        
        // 限制数量
        while (recentCodes.size() > MAX_RECENT_CODES) {
            recentCodes.remove(recentCodes.size() - 1);
        }
    }
    
    /**
     * 发送验证码通知
     */
    private void sendCodeNotification(VerificationCode verificationCode) {
        String title = "验证码：" + verificationCode.code;
        String message = "来自 " + verificationCode.sender;
        
        NotificationManager.NotificationData data = 
            new NotificationManager.NotificationData(title, message);
        
        // 设置大文本
        data.setBigText(verificationCode.fullMessage);
        
        // 创建复制和填充操作
        Intent copyIntent = new Intent(this, CodeActionReceiver.class);
        copyIntent.setAction("com.hippo.ehviewer.ACTION_COPY_CODE");
        copyIntent.putExtra("code", verificationCode.code);
        
        PendingIntent copyPendingIntent = PendingIntent.getBroadcast(
            this, verificationCode.code.hashCode(), copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Intent fillIntent = new Intent(this, CodeActionReceiver.class);
        fillIntent.setAction("com.hippo.ehviewer.ACTION_FILL_CODE");
        fillIntent.putExtra("code", verificationCode.code);
        
        PendingIntent fillPendingIntent = PendingIntent.getBroadcast(
            this, verificationCode.code.hashCode() + 1, fillIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // 添加操作按钮
        NotificationManager.NotificationAction[] actions = new NotificationManager.NotificationAction[]{
            new NotificationManager.NotificationAction(
                R.drawable.ic_content_copy, "复制", copyPendingIntent),
            new NotificationManager.NotificationAction(
                R.drawable.ic_edit, "填充", fillPendingIntent)
        };
        
        data.setType(NotificationManager.NotificationType.MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .actions = actions;
        
        notificationManager.showNotification(data);
        
        // 自动复制到剪贴板（可选）
        if (shouldAutoCopy()) {
            copyToClipboard(verificationCode.code);
        }
    }
    
    /**
     * 复制到剪贴板
     */
    private void copyToClipboard(String code) {
        ClipData clip = ClipData.newPlainText("验证码", code);
        clipboardManager.setPrimaryClip(clip);
        
        // 显示提示
        mainHandler.post(() -> {
            Toast.makeText(this, "验证码已复制：" + code, Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * 是否自动复制
     */
    private boolean shouldAutoCopy() {
        // 从设置中读取用户偏好
        return getSharedPreferences("sms_settings", MODE_PRIVATE)
            .getBoolean("auto_copy_code", true);
    }
    
    /**
     * 获取最近的验证码
     */
    public String getLatestCode() {
        if (!recentCodes.isEmpty()) {
            return recentCodes.get(0).code;
        }
        return null;
    }
    
    /**
     * 获取所有最近的验证码
     */
    public List<String> getRecentCodes() {
        List<String> codes = new ArrayList<>();
        for (VerificationCode vc : recentCodes) {
            codes.add(vc.code);
        }
        return codes;
    }
    
    /**
     * 验证码操作接收器
     */
    public static class CodeActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String code = intent.getStringExtra("code");
            
            if (code == null) return;
            
            if ("com.hippo.ehviewer.ACTION_COPY_CODE".equals(action)) {
                // 复制验证码
                ClipboardManager clipboard = (ClipboardManager) 
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("验证码", code);
                clipboard.setPrimaryClip(clip);
                
                Toast.makeText(context, "验证码已复制", Toast.LENGTH_SHORT).show();
                
            } else if ("com.hippo.ehviewer.ACTION_FILL_CODE".equals(action)) {
                // 填充验证码（发送广播给需要的Activity）
                Intent fillIntent = new Intent("com.hippo.ehviewer.FILL_CODE");
                fillIntent.putExtra("code", code);
                context.sendBroadcast(fillIntent);
                
                Toast.makeText(context, "正在填充验证码", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }
    
    /**
     * 本地绑定器
     */
    public class LocalBinder extends android.os.Binder {
        public SmsCodeExtractorService getService() {
            return SmsCodeExtractorService.this;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 注销接收器
        try {
            unregisterReceiver(smsReceiver);
        } catch (Exception e) {
            // 忽略
        }
        
        // 注销观察者
        getContentResolver().unregisterContentObserver(smsObserver);
    }
}