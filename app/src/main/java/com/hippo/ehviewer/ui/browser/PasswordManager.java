package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.webkit.WebView;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

/**
 * 密码管理器
 * 参考YCWebView实现，提供安全的密码存储和自动填充功能
 */
public class PasswordManager {

    private static final String TAG = "PasswordManager";
    private static final String PREF_NAME = "password_manager";
    private static final String KEY_PASSWORDS = "saved_passwords";
    private static final String KEY_AUTO_FILL_ENABLED = "auto_fill_enabled";
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String CRYPTO_KEY = "EhViewerPasswordKey";
    
    private Context mContext;
    private SharedPreferences mPrefs;
    private boolean mAutoFillEnabled = true;
    private Map<String, PasswordEntry> mPasswordCache = new HashMap<>();
    
    // 常见的登录表单字段匹配模式
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "(?i)(user|account|login|email|mail|id|name)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(pass|pwd|password)", Pattern.CASE_INSENSITIVE);
    
    public PasswordManager(Context context) {
        this.mContext = context;
        this.mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadSettings();
        loadPasswords();
    }
    
    /**
     * 密码条目类
     */
    public static class PasswordEntry {
        public String domain;
        public String username;
        public String password;
        public String formAction;
        public long lastUsed;
        
        public PasswordEntry(String domain, String username, String password) {
            this.domain = domain;
            this.username = username;
            this.password = password;
            this.lastUsed = System.currentTimeMillis();
        }
        
        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("domain", domain);
                json.put("username", username);
                json.put("password", password);
                json.put("formAction", formAction != null ? formAction : "");
                json.put("lastUsed", lastUsed);
                return json;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to convert password entry to JSON", e);
                return null;
            }
        }
        
        public static PasswordEntry fromJson(JSONObject json) {
            try {
                PasswordEntry entry = new PasswordEntry(
                    json.getString("domain"),
                    json.getString("username"),
                    json.getString("password")
                );
                entry.formAction = json.optString("formAction", "");
                entry.lastUsed = json.optLong("lastUsed", System.currentTimeMillis());
                return entry;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse password entry from JSON", e);
                return null;
            }
        }
    }
    
    /**
     * 加载设置
     */
    private void loadSettings() {
        mAutoFillEnabled = mPrefs.getBoolean(KEY_AUTO_FILL_ENABLED, true);
        Log.d(TAG, "Password manager settings loaded: autoFill=" + mAutoFillEnabled);
    }
    
    /**
     * 加载已保存的密码
     */
    private void loadPasswords() {
        try {
            String encryptedPasswords = mPrefs.getString(KEY_PASSWORDS, "{}");
            String decryptedPasswords = decrypt(encryptedPasswords);
            
            JSONObject passwordsJson = new JSONObject(decryptedPasswords);
            mPasswordCache.clear();
            
            java.util.Iterator<String> keys = passwordsJson.keys();
            while (keys.hasNext()) {
                String domain = keys.next();
                JSONObject entryJson = passwordsJson.getJSONObject(domain);
                PasswordEntry entry = PasswordEntry.fromJson(entryJson);
                if (entry != null) {
                    mPasswordCache.put(domain, entry);
                }
            }
            
            Log.d(TAG, "Loaded " + mPasswordCache.size() + " password entries");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load passwords", e);
            mPasswordCache.clear();
        }
    }
    
    /**
     * 保存密码到存储
     */
    private void savePasswords() {
        try {
            JSONObject passwordsJson = new JSONObject();
            
            for (Map.Entry<String, PasswordEntry> entry : mPasswordCache.entrySet()) {
                JSONObject entryJson = entry.getValue().toJson();
                if (entryJson != null) {
                    passwordsJson.put(entry.getKey(), entryJson);
                }
            }
            
            String encryptedPasswords = encrypt(passwordsJson.toString());
            mPrefs.edit()
                .putString(KEY_PASSWORDS, encryptedPasswords)
                .putBoolean(KEY_AUTO_FILL_ENABLED, mAutoFillEnabled)
                .apply();
            
            Log.d(TAG, "Saved " + mPasswordCache.size() + " password entries");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save passwords", e);
        }
    }
    
    /**
     * 保存新的密码条目
     */
    public void savePassword(String url, String username, String password) {
        try {
            String domain = extractDomain(url);
            if (domain == null || username == null || password == null) {
                return;
            }
            
            PasswordEntry entry = new PasswordEntry(domain, username, password);
            entry.formAction = url;
            
            mPasswordCache.put(domain, entry);
            savePasswords();
            
            Log.d(TAG, "Password saved for domain: " + domain);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save password", e);
        }
    }
    
    /**
     * 获取指定域名的密码
     */
    public PasswordEntry getPassword(String url) {
        try {
            String domain = extractDomain(url);
            if (domain == null) return null;
            
            PasswordEntry entry = mPasswordCache.get(domain);
            if (entry != null) {
                entry.lastUsed = System.currentTimeMillis();
                savePasswords(); // 更新最后使用时间
            }
            
            return entry;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get password", e);
            return null;
        }
    }
    
    /**
     * 删除指定域名的密码
     */
    public boolean deletePassword(String url) {
        try {
            String domain = extractDomain(url);
            if (domain == null) return false;
            
            boolean removed = mPasswordCache.remove(domain) != null;
            if (removed) {
                savePasswords();
                Log.d(TAG, "Password deleted for domain: " + domain);
            }
            
            return removed;
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete password", e);
            return false;
        }
    }
    
    /**
     * 自动填充密码到WebView
     */
    public void autoFillPassword(WebView webView, String url) {
        if (!mAutoFillEnabled || webView == null) return;
        
        PasswordEntry entry = getPassword(url);
        if (entry == null) return;
        
        // 构建自动填充的JavaScript代码
        String autoFillJs = buildAutoFillJavaScript(entry.username, entry.password);
        
        try {
            webView.evaluateJavascript(autoFillJs, result -> {
                Log.d(TAG, "Auto-fill attempted for: " + entry.domain);
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to auto-fill password", e);
        }
    }
    
    /**
     * 构建自动填充JavaScript代码
     */
    private String buildAutoFillJavaScript(String username, String password) {
        String escapedUsername = escapeJavaScript(username);
        String escapedPassword = escapeJavaScript(password);
        
        return "(function() {" +
               "  try {" +
               "    var usernameFields = [];" +
               "    var passwordFields = [];" +
               "    " +
               "    var inputs = document.querySelectorAll('input');" +
               "    for (var i = 0; i < inputs.length; i++) {" +
               "      var input = inputs[i];" +
               "      var type = input.type ? input.type.toLowerCase() : '';" +
               "      var name = input.name ? input.name.toLowerCase() : '';" +
               "      var id = input.id ? input.id.toLowerCase() : '';" +
               "      " +
               "      if (type === 'password') {" +
               "        passwordFields.push(input);" +
               "      } else if (type === 'text' || type === 'email' || type === '') {" +
               "        var fieldText = (name + id).toLowerCase();" +
               "        if (/user|account|login|email|mail/.test(fieldText)) {" +
               "          usernameFields.push(input);" +
               "        }" +
               "      }" +
               "    }" +
               "    " +
               "    if (usernameFields.length > 0 && passwordFields.length > 0) {" +
               "      usernameFields[0].value = '" + escapedUsername + "';" +
               "      passwordFields[0].value = '" + escapedPassword + "';" +
               "      " +
               "      usernameFields[0].dispatchEvent(new Event('input', {bubbles: true}));" +
               "      passwordFields[0].dispatchEvent(new Event('input', {bubbles: true}));" +
               "      " +
               "      return 'AutoFill completed';" +
               "    }" +
               "    return 'No suitable fields found';" +
               "  } catch (e) {" +
               "    return 'AutoFill error: ' + e.message;" +
               "  }" +
               "})();";
    }
    
    /**
     * 转义JavaScript字符串
     */
    private String escapeJavaScript(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                 .replace("'", "\\'")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r");
    }
    
    /**
     * 从URL提取域名
     */
    private String extractDomain(String url) {
        try {
            Uri uri = Uri.parse(url);
            return uri.getHost();
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract domain from URL: " + url, e);
            return null;
        }
    }
    
    /**
     * 加密字符串
     */
    private String encrypt(String plainText) {
        try {
            SecretKeySpec key = new SecretKeySpec(CRYPTO_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return plainText; // 回退到明文存储（仅用于调试）
        }
    }
    
    /**
     * 解密字符串
     */
    private String decrypt(String encryptedText) {
        try {
            SecretKeySpec key = new SecretKeySpec(CRYPTO_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT));
            return new String(decryptedBytes);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return encryptedText; // 可能是明文存储的旧数据
        }
    }
    
    /**
     * 设置自动填充状态
     */
    public void setAutoFillEnabled(boolean enabled) {
        if (mAutoFillEnabled != enabled) {
            mAutoFillEnabled = enabled;
            mPrefs.edit().putBoolean(KEY_AUTO_FILL_ENABLED, enabled).apply();
            Log.d(TAG, "Auto-fill " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 是否启用自动填充
     */
    public boolean isAutoFillEnabled() {
        return mAutoFillEnabled;
    }
    
    /**
     * 获取保存的密码数量
     */
    public int getPasswordCount() {
        return mPasswordCache.size();
    }
    
    /**
     * 清空所有密码
     */
    public void clearAllPasswords() {
        mPasswordCache.clear();
        mPrefs.edit().remove(KEY_PASSWORDS).apply();
        Log.d(TAG, "All passwords cleared");
    }
    
    /**
     * 获取所有域名列表（用于管理界面）
     */
    public java.util.Set<String> getAllDomains() {
        return new java.util.HashSet<>(mPasswordCache.keySet());
    }
}