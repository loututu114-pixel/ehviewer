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

package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.hippo.preference.ListPreference;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.gallery.GalleryReadingModeManager;

public class ReadFragment extends BasePreferenceFragmentCompat {

    private static final String TAG = "ReadFragment";
    
    private ListPreference mReadingModePresetPref;
    private ListPreference mReadingDirectionPref;
    private ListPreference mPageScalingPref; 
    private ListPreference mStartPositionPref;

    /**
     * 设置->阅读界面
     * xml/read_settings.xml
     * @param savedInstanceState
     */
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.read_settings);
        
        // 获取Preference引用
        mReadingModePresetPref = findPreference("gallery_reading_mode_preset");
        mReadingDirectionPref = findPreference("reading_direction");
        mPageScalingPref = findPreference("page_scaling");
        mStartPositionPref = findPreference("start_position");
        
        // 设置当前选中的预设模式
        if (mReadingModePresetPref != null) {
            updateReadingModePresetSelection();
            
            // 设置预设模式变更监听器
            mReadingModePresetPref.setOnPreferenceChangeListener(this::onReadingModePresetChanged);
        }
        
        // 为个别设置添加监听器，用于检测是否为自定义模式
        if (mReadingDirectionPref != null) {
            mReadingDirectionPref.setOnPreferenceChangeListener(this::onIndividualSettingChanged);
        }
        if (mPageScalingPref != null) {
            mPageScalingPref.setOnPreferenceChangeListener(this::onIndividualSettingChanged);
        }
        if (mStartPositionPref != null) {
            mStartPositionPref.setOnPreferenceChangeListener(this::onIndividualSettingChanged);
        }
    }
    
    /**
     * 阅读模式预设变更处理
     */
    private boolean onReadingModePresetChanged(Preference preference, Object newValue) {
        int presetMode = Integer.parseInt((String) newValue);
        Log.d(TAG, "Reading mode preset changed to: " + presetMode);
        
        // 应用预设模式
        GalleryReadingModeManager.applyPresetMode(presetMode);
        
        // 更新其他相关设置项的显示值
        updateRelatedPreferences();
        
        return true;
    }
    
    /**
     * 个别设置变更处理（用于检测自定义模式）
     */
    private boolean onIndividualSettingChanged(Preference preference, Object newValue) {
        // 延迟执行，确保新值已保存
        preference.getPreferenceManager().getSharedPreferences().edit()
                .putString(preference.getKey(), (String) newValue)
                .apply();
        
        // 延迟更新预设模式选择
        getView().post(this::updateReadingModePresetSelection);
        
        return true;
    }
    
    /**
     * 更新阅读模式预设的选择状态
     */
    private void updateReadingModePresetSelection() {
        if (mReadingModePresetPref != null) {
            int currentPreset = GalleryReadingModeManager.getCurrentPresetMode();
            mReadingModePresetPref.setValue(String.valueOf(currentPreset));
            
            Log.d(TAG, "Updated reading mode preset selection to: " + currentPreset + 
                      " (" + GalleryReadingModeManager.getPresetName(currentPreset) + ")");
        }
    }
    
    /**
     * 更新相关设置项的显示值
     */
    private void updateRelatedPreferences() {
        if (mReadingDirectionPref != null) {
            mReadingDirectionPref.setValue(String.valueOf(Settings.getReadingDirection()));
        }
        if (mPageScalingPref != null) {
            mPageScalingPref.setValue(String.valueOf(Settings.getPageScaling()));
        }
        if (mStartPositionPref != null) {
            mStartPositionPref.setValue(String.valueOf(Settings.getStartPosition()));
        }
    }
}
