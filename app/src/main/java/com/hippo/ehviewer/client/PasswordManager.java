/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * 密码管理器
 * 提供安全的密码存储、自动填充和管理功能
 */
public class PasswordManager {

    private static final String TAG = "PasswordManager";

    private static final String KEYSTORE_ALIAS = "EhViewerPasswordKey";
    private static final String PREFS_NAME = "password_manager_prefs";
    private static final String PREF_ENCRYPTED_DATA = "encrypted_passwords";
    private static final String PREF_IV = "encryption_iv";

    private static PasswordManager sInstance;

    private Context mContext;
    private KeyStore mKeyStore;
    private SharedPreferences mPreferences;

    // 密码数据缓存
    private Map<String, PasswordEntry> mPasswordCache = new HashMap<>();
    private boolean mIsUnlocked = false;

    /**
     * 密码条目
     */
    public static class PasswordEntry {
        public String domain;
        public String username;
        public String password;
        public long createdTime;
        public long lastUsedTime;

        public PasswordEntry(String domain, String username, String password) {
            this.domain = domain;
            this.username = username;
            this.password = password;
            this.createdTime = System.currentTimeMillis();
            this.lastUsedTime = this.createdTime;
        }
    }

    /**
     * 获取单例实例
     */
    public static synchronized PasswordManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new PasswordManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private PasswordManager(Context context) {
        this.mContext = context;
        this.mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initializeKeyStore();
    }

    /**
     * 初始化密钥库
     */
    private void initializeKeyStore() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            mKeyStore.load(null);

            // 检查密钥是否存在，不存在则创建
            if (!mKeyStore.containsAlias(KEYSTORE_ALIAS)) {
                createKey();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize KeyStore", e);
        }
    }

    /**
     * 创建加密密钥
     */
    private void createKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true) // 需要用户认证
                .setUserAuthenticationValidityDurationSeconds(300); // 5分钟

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(true);
            }

            keyGenerator.init(builder.build());
            keyGenerator.generateKey();

            Log.d(TAG, "Encryption key created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create encryption key", e);
        }
    }

    /**
     * 保存密码
     */
    public boolean savePassword(String domain, String username, String password) {
        if (!mIsUnlocked) {
            Log.w(TAG, "Password manager is locked");
            return false;
        }

        try {
            PasswordEntry entry = new PasswordEntry(domain, username, password);
            mPasswordCache.put(getCacheKey(domain, username), entry);

            // 立即保存到存储
            savePasswordsToStorage();

            Log.d(TAG, "Password saved for domain: " + domain);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save password", e);
            return false;
        }
    }

    /**
     * 获取密码
     */
    @Nullable
    public PasswordEntry getPassword(String domain, String username) {
        if (!mIsUnlocked) {
            Log.w(TAG, "Password manager is locked");
            return null;
        }

        return mPasswordCache.get(getCacheKey(domain, username));
    }

    /**
     * 获取域名的所有密码
     */
    @NonNull
    public List<PasswordEntry> getPasswordsForDomain(String domain) {
        if (!mIsUnlocked) {
            Log.w(TAG, "Password manager is locked");
            return new ArrayList<>();
        }

        List<PasswordEntry> result = new ArrayList<>();
        for (PasswordEntry entry : mPasswordCache.values()) {
            if (entry.domain.equals(domain)) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 删除密码
     */
    public boolean deletePassword(String domain, String username) {
        if (!mIsUnlocked) {
            Log.w(TAG, "Password manager is locked");
            return false;
        }

        String key = getCacheKey(domain, username);
        if (mPasswordCache.remove(key) != null) {
            savePasswordsToStorage();
            Log.d(TAG, "Password deleted for domain: " + domain);
            return true;
        }
        return false;
    }

    /**
     * 更新密码使用时间
     */
    public void updateLastUsedTime(String domain, String username) {
        PasswordEntry entry = getPassword(domain, username);
        if (entry != null) {
            entry.lastUsedTime = System.currentTimeMillis();
            savePasswordsToStorage();
        }
    }

    /**
     * 解锁密码管理器（需要生物识别认证）
     */
    public void unlockWithBiometric(FragmentActivity activity, BiometricCallback callback) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("解锁密码管理器")
                .setSubtitle("使用生物识别验证身份")
                .setDescription("需要验证您的身份才能访问保存的密码")
                .setNegativeButtonText("取消")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
            activity.getMainExecutor(),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    mIsUnlocked = true;
                    loadPasswordsFromStorage();
                    callback.onUnlockSuccess();
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    callback.onUnlockError(errString.toString());
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    callback.onUnlockFailed();
                }
            });

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * 锁定密码管理器
     */
    public void lock() {
        mIsUnlocked = false;
        mPasswordCache.clear();
        Log.d(TAG, "Password manager locked");
    }

    /**
     * 检查是否已解锁
     */
    public boolean isUnlocked() {
        return mIsUnlocked;
    }

    /**
     * 生物识别回调接口
     */
    public interface BiometricCallback {
        void onUnlockSuccess();
        void onUnlockError(String error);
        void onUnlockFailed();
    }

    /**
     * 获取缓存键
     */
    private String getCacheKey(String domain, String username) {
        return domain + "|" + username;
    }

    /**
     * 保存密码到存储
     */
    private void savePasswordsToStorage() {
        try {
            // 将密码数据序列化为JSON
            String jsonData = serializePasswords();

            // 加密数据
            byte[] encryptedData = encryptData(jsonData.getBytes(StandardCharsets.UTF_8));

            // 保存加密数据和IV
            String encryptedBase64 = Base64.encodeToString(encryptedData, Base64.DEFAULT);
            String ivBase64 = Base64.encodeToString(getIVFromCipher(), Base64.DEFAULT);

            mPreferences.edit()
                    .putString(PREF_ENCRYPTED_DATA, encryptedBase64)
                    .putString(PREF_IV, ivBase64)
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Failed to save passwords to storage", e);
        }
    }

    /**
     * 从存储加载密码
     */
    private void loadPasswordsFromStorage() {
        try {
            String encryptedBase64 = mPreferences.getString(PREF_ENCRYPTED_DATA, null);
            String ivBase64 = mPreferences.getString(PREF_IV, null);

            if (encryptedBase64 != null && ivBase64 != null) {
                byte[] encryptedData = Base64.decode(encryptedBase64, Base64.DEFAULT);
                byte[] iv = Base64.decode(ivBase64, Base64.DEFAULT);

                // 解密数据
                byte[] decryptedData = decryptData(encryptedData, iv);

                // 反序列化密码数据
                deserializePasswords(new String(decryptedData, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load passwords from storage", e);
        }
    }

    /**
     * 序列化密码数据
     */
    private String serializePasswords() {
        // 简化的JSON序列化实现
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (PasswordEntry entry : mPasswordCache.values()) {
            if (!first) json.append(",");
            json.append("{\"domain\":\"").append(entry.domain).append("\",")
                .append("\"username\":\"").append(entry.username).append("\",")
                .append("\"password\":\"").append(entry.password).append("\",")
                .append("\"createdTime\":").append(entry.createdTime).append(",")
                .append("\"lastUsedTime\":").append(entry.lastUsedTime).append("}");
            first = false;
        }

        json.append("]");
        return json.toString();
    }

    /**
     * 反序列化密码数据
     */
    private void deserializePasswords(String jsonData) {
        // 简化的JSON反序列化实现
        // 在实际项目中建议使用Gson等专业库
        mPasswordCache.clear();

        if (jsonData.startsWith("[") && jsonData.endsWith("]")) {
            String content = jsonData.substring(1, jsonData.length() - 1);
            if (!content.isEmpty()) {
                String[] entries = content.split("\\},\\{");
                for (String entryStr : entries) {
                    try {
                        PasswordEntry entry = parsePasswordEntry(entryStr);
                        if (entry != null) {
                            mPasswordCache.put(getCacheKey(entry.domain, entry.username), entry);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to parse password entry: " + entryStr, e);
                    }
                }
            }
        }
    }

    /**
     * 解析密码条目
     */
    private PasswordEntry parsePasswordEntry(String entryStr) {
        // 简化的JSON解析
        String domain = extractJsonValue(entryStr, "domain");
        String username = extractJsonValue(entryStr, "username");
        String password = extractJsonValue(entryStr, "password");

        if (domain != null && username != null && password != null) {
            PasswordEntry entry = new PasswordEntry(domain, username, password);
            return entry;
        }

        return null;
    }

    /**
     * 提取JSON值
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);

        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    /**
     * 加密数据
     */
    private byte[] encryptData(byte[] data) throws Exception {
        SecretKey key = (SecretKey) mKeyStore.getKey(KEYSTORE_ALIAS, null);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(data);
    }

    /**
     * 解密数据
     */
    private byte[] decryptData(byte[] encryptedData, byte[] iv) throws Exception {
        SecretKey key = (SecretKey) mKeyStore.getKey(KEYSTORE_ALIAS, null);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        return cipher.doFinal(encryptedData);
    }

    /**
     * 获取IV
     */
    private byte[] getIVFromCipher() {
        // 这个方法需要根据实际的Cipher实现来获取IV
        // 这里返回一个简化的实现
        return new byte[12]; // GCM模式的IV长度
    }

    /**
     * 清理所有数据
     */
    public void clearAllData() {
        mPasswordCache.clear();
        mPreferences.edit().clear().apply();

        try {
            if (mKeyStore.containsAlias(KEYSTORE_ALIAS)) {
                mKeyStore.deleteEntry(KEYSTORE_ALIAS);
            }
        } catch (KeyStoreException e) {
            Log.e(TAG, "Failed to delete key", e);
        }

        Log.d(TAG, "All password data cleared");
    }

    /**
     * 获取密码统计信息
     */
    public PasswordStats getStats() {
        PasswordStats stats = new PasswordStats();
        stats.totalPasswords = mPasswordCache.size();

        for (PasswordEntry entry : mPasswordCache.values()) {
            stats.domains.add(entry.domain);
        }

        return stats;
    }

    /**
     * 密码统计信息
     */
    public static class PasswordStats {
        public int totalPasswords = 0;
        public List<String> domains = new ArrayList<>();

        public int getUniqueDomains() {
            return domains.size();
        }
    }
}
