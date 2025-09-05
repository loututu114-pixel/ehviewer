package com.hippo.ehviewer.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.hippo.ehviewer.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 用户反馈管理器
 */
public class UserFeedbackManager {
    
    private static final String TAG = "UserFeedbackManager";
    private static final String PREFS_NAME = "user_feedback";
    private static final String KEY_FEEDBACK_HISTORY = "feedback_history";
    private static final String KEY_LAST_FEEDBACK_TIME = "last_feedback_time";
    private static final String KEY_FEEDBACK_COUNT = "feedback_count";
    
    // 反馈类型
    public enum FeedbackType {
        BUG_REPORT(0, "错误报告", "🐛"),
        FEATURE_REQUEST(1, "功能建议", "💡"), 
        PERFORMANCE_ISSUE(2, "性能问题", "⚡"),
        UI_UX_FEEDBACK(3, "界面体验", "🎨"),
        GENERAL_SUGGESTION(4, "一般建议", "💭"),
        PRAISE(5, "表扬夸奖", "👏");
        
        private final int id;
        private final String displayName;
        private final String emoji;
        
        FeedbackType(int id, String displayName, String emoji) {
            this.id = id;
            this.displayName = displayName;
            this.emoji = emoji;
        }
        
        public int getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getEmoji() { return emoji; }
        
        public static FeedbackType fromId(int id) {
            for (FeedbackType type : values()) {
                if (type.id == id) return type;
            }
            return GENERAL_SUGGESTION;
        }
    }
    
    private final Context mContext;
    private final SharedPreferences mPrefs;
    
    public UserFeedbackManager(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 显示反馈对话框
     */
    public void showFeedbackDialog(@NonNull Activity activity) {
        try {
            LayoutInflater inflater = LayoutInflater.from(activity);
            View dialogView = inflater.inflate(R.layout.dialog_user_feedback, null);
            
            RadioGroup typeGroup = dialogView.findViewById(R.id.feedback_type_group);
            EditText contentEdit = dialogView.findViewById(R.id.feedback_content);
            EditText contactEdit = dialogView.findViewById(R.id.feedback_contact);
            RadioGroup ratingGroup = dialogView.findViewById(R.id.feedback_rating_group);
            
            AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("用户反馈")
                .setView(dialogView)
                .setPositiveButton("提交反馈", null)
                .setNegativeButton("取消", null)
                .create();
            
            dialog.setOnShowListener(d -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (submitFeedback(typeGroup, contentEdit, contactEdit, ratingGroup)) {
                        dialog.dismiss();
                        showThankYouMessage(activity);
                    }
                });
            });
            
            dialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing feedback dialog", e);
            Toast.makeText(activity, "无法打开反馈对话框", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 提交反馈
     */
    private boolean submitFeedback(@NonNull RadioGroup typeGroup, @NonNull EditText contentEdit, 
                                   @NonNull EditText contactEdit, @NonNull RadioGroup ratingGroup) {
        try {
            // 获取反馈类型
            int typeId = typeGroup.getCheckedRadioButtonId();
            FeedbackType type = getFeedbackTypeFromRadioId(typeId);
            
            // 获取反馈内容
            String content = contentEdit.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(mContext, "请填写反馈内容", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            // 获取评分
            int ratingId = ratingGroup.getCheckedRadioButtonId();
            int rating = getRatingFromRadioId(ratingId);
            
            // 获取联系方式（可选）
            String contactInfo = contactEdit.getText().toString().trim();
            
            // 保存反馈并显示成功消息
            Log.i(TAG, "反馈提交成功: " + type.getDisplayName() + " - " + content);
            
            // 更新统计信息
            int count = mPrefs.getInt(KEY_FEEDBACK_COUNT, 0) + 1;
            mPrefs.edit()
                .putInt(KEY_FEEDBACK_COUNT, count)
                .putLong(KEY_LAST_FEEDBACK_TIME, System.currentTimeMillis())
                .apply();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error submitting feedback", e);
            Toast.makeText(mContext, "提交反馈时出错", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * 显示感谢信息
     */
    private void showThankYouMessage(@NonNull Activity activity) {
        try {
            int feedbackCount = mPrefs.getInt(KEY_FEEDBACK_COUNT, 0);
            String message;
            
            if (feedbackCount == 1) {
                message = "感谢您的第一次反馈！\n\n您的意见对我们非常宝贵，我们会认真对待每一条反馈。";
            } else if (feedbackCount <= 3) {
                message = "再次感谢您的反馈！\n\n这是您的第" + feedbackCount + "次反馈，您的每一条建议都在帮助EhViewer改进。";
            } else {
                message = "感谢您一如既往的支持！\n\n您已经提交了" + feedbackCount + "次反馈，是我们的超级用户！";
            }
            
            new AlertDialog.Builder(activity)
                .setTitle("反馈提交成功")
                .setMessage(message)
                .setPositiveButton("继续使用", null)
                .show();
                
        } catch (Exception e) {
            Log.e(TAG, "Error showing thank you message", e);
            Toast.makeText(activity, "反馈提交成功！感谢您的支持！", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 智能反馈提醒
     */
    public boolean shouldShowFeedbackReminder() {
        long lastFeedbackTime = mPrefs.getLong(KEY_LAST_FEEDBACK_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long daysSinceLastFeedback = (currentTime - lastFeedbackTime) / (24 * 60 * 60 * 1000);
        
        // 如果从未反馈过，使用超过7天后提醒
        if (lastFeedbackTime == 0) {
            return daysSinceLastFeedback > 7;
        }
        
        // 如果上次反馈是30天前，可以再次提醒
        return daysSinceLastFeedback > 30;
    }
    
    /**
     * 获取反馈统计信息
     */
    public String getFeedbackStats() {
        int count = mPrefs.getInt(KEY_FEEDBACK_COUNT, 0);
        if (count == 0) {
            return "暂无反馈记录";
        }
        return "已提交" + count + "次反馈";
    }
    
    // 辅助方法
    private FeedbackType getFeedbackTypeFromRadioId(int radioId) {
        if (radioId == R.id.feedback_type_bug) {
            return FeedbackType.BUG_REPORT;
        } else if (radioId == R.id.feedback_type_feature) {
            return FeedbackType.FEATURE_REQUEST;
        } else if (radioId == R.id.feedback_type_performance) {
            return FeedbackType.PERFORMANCE_ISSUE;
        } else if (radioId == R.id.feedback_type_ui) {
            return FeedbackType.UI_UX_FEEDBACK;
        } else if (radioId == R.id.feedback_type_general) {
            return FeedbackType.GENERAL_SUGGESTION;
        } else if (radioId == R.id.feedback_type_praise) {
            return FeedbackType.PRAISE;
        }
        return FeedbackType.GENERAL_SUGGESTION; // 默认
    }
    
    private int getRatingFromRadioId(int radioId) {
        if (radioId == R.id.rating_5) {
            return 5;
        } else if (radioId == R.id.rating_4) {
            return 4;
        } else if (radioId == R.id.rating_3) {
            return 3;
        } else if (radioId == R.id.rating_2) {
            return 2;
        } else if (radioId == R.id.rating_1) {
            return 1;
        }
        return 5; // 默认5星
    }
}