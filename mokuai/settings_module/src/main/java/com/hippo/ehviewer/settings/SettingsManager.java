/*
 * EhViewer Settings Module - SettingsManager
 * 设置管理器 - 提供统一的设置管理功能
 */

package com.hippo.ehviewer.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * 设置管理器
 * 提供统一的设置存储、读取和管理功能
 */
public class SettingsManager {

    private static final String TAG = SettingsManager.class.getSimpleName();
    private static final String PREF_NAME = "ehviewer_settings";

    private static SettingsManager sInstance;

    private final SharedPreferences mPreferences;
    private final SharedPreferences.Editor mEditor;
    private final Set<SettingChangeListener> mListeners;

    /**
     * 设置变化监听器
     */
    public interface SettingChangeListener {
        void onSettingChanged(String key, Object newValue);
    }

    /**
     * 获取单例实例
     */
    public static synchronized SettingsManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SettingsManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private SettingsManager(Context context) {
        mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mEditor = mPreferences.edit();
        mListeners = new HashSet<>();
    }

    /**
     * 注册设置变化监听器
     */
    public void registerListener(SettingChangeListener listener) {
        mListeners.add(listener);
    }

    /**
     * 注册指定键的监听器
     */
    public void registerListener(String key, SettingChangeListener listener) {
        // 这里可以实现更精细的监听器管理
        registerListener(listener);
    }

    /**
     * 注销设置变化监听器
     */
    public void unregisterListener(SettingChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * 通知监听器设置变化
     */
    private void notifyListeners(String key, Object newValue) {
        for (SettingChangeListener listener : mListeners) {
            try {
                listener.onSettingChanged(key, newValue);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener", e);
            }
        }
    }

    /**
     * 存储布尔值
     */
    public void putBoolean(String key, boolean value) {
        mEditor.putBoolean(key, value);
        mEditor.apply();
        notifyListeners(key, value);
        Log.d(TAG, "Saved boolean setting: " + key + " = " + value);
    }

    /**
     * 获取布尔值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return mPreferences.getBoolean(key, defaultValue);
    }

    /**
     * 存储整数值
     */
    public void putInt(String key, int value) {
        mEditor.putInt(key, value);
        mEditor.apply();
        notifyListeners(key, value);
        Log.d(TAG, "Saved int setting: " + key + " = " + value);
    }

    /**
     * 获取整数值
     */
    public int getInt(String key, int defaultValue) {
        return mPreferences.getInt(key, defaultValue);
    }

    /**
     * 存储长整数值
     */
    public void putLong(String key, long value) {
        mEditor.putLong(key, value);
        mEditor.apply();
        notifyListeners(key, value);
        Log.d(TAG, "Saved long setting: " + key + " = " + value);
    }

    /**
     * 获取长整数值
     */
    public long getLong(String key, long defaultValue) {
        return mPreferences.getLong(key, defaultValue);
    }

    /**
     * 存储浮点数值
     */
    public void putFloat(String key, float value) {
        mEditor.putFloat(key, value);
        mEditor.apply();
        notifyListeners(key, value);
        Log.d(TAG, "Saved float setting: " + key + " = " + value);
    }

    /**
     * 获取浮点数值
     */
    public float getFloat(String key, float defaultValue) {
        return mPreferences.getFloat(key, defaultValue);
    }

    /**
     * 存储字符串值
     */
    public void putString(String key, String value) {
        mEditor.putString(key, value);
        mEditor.apply();
        notifyListeners(key, value);
        Log.d(TAG, "Saved string setting: " + key + " = " + value);
    }

    /**
     * 获取字符串值
     */
    public String getString(String key, String defaultValue) {
        return mPreferences.getString(key, defaultValue);
    }

    /**
     * 存储字符串集合
     */
    public void putStringSet(String key, Set<String> values) {
        mEditor.putStringSet(key, values);
        mEditor.apply();
        notifyListeners(key, values);
        Log.d(TAG, "Saved string set setting: " + key);
    }

    /**
     * 获取字符串集合
     */
    public Set<String> getStringSet(String key, Set<String> defaultValues) {
        return mPreferences.getStringSet(key, defaultValues);
    }

    /**
     * 移除设置
     */
    public void remove(String key) {
        mEditor.remove(key);
        mEditor.apply();
        notifyListeners(key, null);
        Log.d(TAG, "Removed setting: " + key);
    }

    /**
     * 清空所有设置
     */
    public void clear() {
        mEditor.clear();
        mEditor.apply();
        notifyListeners(null, null);
        Log.d(TAG, "Cleared all settings");
    }

    /**
     * 检查是否包含指定键
     */
    public boolean contains(String key) {
        return mPreferences.contains(key);
    }

    /**
     * 获取所有设置的键
     */
    public Set<String> getAllKeys() {
        return mPreferences.getAll().keySet();
    }

    /**
     * 获取设置数量
     */
    public int size() {
        return mPreferences.getAll().size();
    }

    /**
     * 批量存储设置
     */
    public void putBatch(SettingsBatch batch) {
        SharedPreferences.Editor editor = mPreferences.edit();

        for (SettingItem item : batch.getItems()) {
            String key = item.getKey();
            Object value = item.getValue();

            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Set) {
                editor.putStringSet(key, (Set<String>) value);
            }
        }

        editor.apply();

        // 通知所有变化
        for (SettingItem item : batch.getItems()) {
            notifyListeners(item.getKey(), item.getValue());
        }

        Log.d(TAG, "Batch saved " + batch.getItems().size() + " settings");
    }

    /**
     * 导出设置到JSON字符串
     */
    public String exportToJson() {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            for (String key : getAllKeys()) {
                Object value = mPreferences.getAll().get(key);
                json.put(key, value);
            }
            return json.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to export settings to JSON", e);
            return null;
        }
    }

    /**
     * 从JSON字符串导入设置
     */
    public boolean importFromJson(String jsonString) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonString);
            SharedPreferences.Editor editor = mPreferences.edit();

            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);

                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                }
                // 其他类型暂时不支持
            }

            editor.apply();
            Log.d(TAG, "Settings imported from JSON");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to import settings from JSON", e);
            return false;
        }
    }

    /**
     * 获取设置类型
     */
    public SettingType getType(String key) {
        if (!contains(key)) {
            return SettingType.NONE;
        }

        Object value = mPreferences.getAll().get(key);
        if (value instanceof Boolean) {
            return SettingType.BOOLEAN;
        } else if (value instanceof Integer) {
            return SettingType.INT;
        } else if (value instanceof Long) {
            return SettingType.LONG;
        } else if (value instanceof Float) {
            return SettingType.FLOAT;
        } else if (value instanceof String) {
            return SettingType.STRING;
        } else if (value instanceof Set) {
            return SettingType.STRING_SET;
        } else {
            return SettingType.UNKNOWN;
        }
    }

    /**
     * 获取设置值（通用方法）
     */
    public Object getValue(String key) {
        return mPreferences.getAll().get(key);
    }

    /**
     * 设置类型枚举
     */
    public enum SettingType {
        NONE, BOOLEAN, INT, LONG, FLOAT, STRING, STRING_SET, UNKNOWN
    }

    /**
     * 设置项
     */
    public static class SettingItem {
        private final String key;
        private final Object value;

        public SettingItem(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public Object getValue() { return value; }
    }

    /**
     * 批量设置
     */
    public static class SettingsBatch {
        private final java.util.List<SettingItem> items = new java.util.ArrayList<>();

        public void add(String key, Object value) {
            items.add(new SettingItem(key, value));
        }

        public java.util.List<SettingItem> getItems() {
            return items;
        }

        public void clear() {
            items.clear();
        }
    }
}
