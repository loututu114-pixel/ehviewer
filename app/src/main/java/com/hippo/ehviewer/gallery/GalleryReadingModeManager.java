/*
 * Copyright 2025 EhViewer Contributors
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

package com.hippo.ehviewer.gallery;

import android.content.Context;
import android.util.Log;

import com.hippo.ehviewer.Settings;
import com.hippo.lib.glgallery.GalleryView;

/**
 * 画廊阅读模式管理器
 * 
 * 提供人性化的阅读模式设置，让用户在阅读时能方便地切换不同的显示模式
 */
public class GalleryReadingModeManager {
    
    private static final String TAG = "GalleryReadingModeManager";
    
    // 阅读模式预设
    public static final int PRESET_MANGA_RTL = 0;           // 漫画模式（从右到左）
    public static final int PRESET_MANGA_LTR = 1;           // 漫画模式（从左到右）
    public static final int PRESET_WEBTOON = 2;             // 长条漫画模式（从上到下）
    public static final int PRESET_BOOK_MODE = 3;           // 图书模式（页面适配）
    public static final int PRESET_FULLSCREEN = 4;          // 全屏模式（原始大小）
    public static final int PRESET_CUSTOM = 5;              // 自定义模式
    
    /**
     * 阅读模式预设配置
     */
    public static class ReadingModePreset {
        public final int layoutMode;
        public final int scaleMode;
        public final int startPosition;
        public final String name;
        public final String description;
        
        public ReadingModePreset(int layoutMode, int scaleMode, int startPosition, 
                               String name, String description) {
            this.layoutMode = layoutMode;
            this.scaleMode = scaleMode;
            this.startPosition = startPosition;
            this.name = name;
            this.description = description;
        }
    }
    
    /**
     * 获取所有预设模式
     */
    public static ReadingModePreset[] getAllPresets() {
        return new ReadingModePreset[] {
            new ReadingModePreset(
                GalleryView.LAYOUT_RIGHT_TO_LEFT,
                GalleryView.SCALE_FIT,
                GalleryView.START_POSITION_TOP_RIGHT,
                "漫画模式（右到左）",
                "适合阅读日本漫画，从右页到左页翻页"
            ),
            new ReadingModePreset(
                GalleryView.LAYOUT_LEFT_TO_RIGHT,
                GalleryView.SCALE_FIT,
                GalleryView.START_POSITION_TOP_LEFT,
                "漫画模式（左到右）",
                "适合阅读欧美漫画，从左页到右页翻页"
            ),
            new ReadingModePreset(
                GalleryView.LAYOUT_TOP_TO_BOTTOM,
                GalleryView.SCALE_FIT_WIDTH,
                GalleryView.START_POSITION_TOP_LEFT,
                "长条模式",
                "适合阅读韩国webtoon，从上到下滚动"
            ),
            new ReadingModePreset(
                GalleryView.LAYOUT_RIGHT_TO_LEFT,
                GalleryView.SCALE_FIT,
                GalleryView.START_POSITION_CENTER,
                "图书模式",
                "页面居中显示，适合阅读图书类内容"
            ),
            new ReadingModePreset(
                GalleryView.LAYOUT_RIGHT_TO_LEFT,
                GalleryView.SCALE_ORIGIN,
                GalleryView.START_POSITION_TOP_LEFT,
                "全屏模式",
                "图片原始大小显示，可自由缩放平移"
            ),
            new ReadingModePreset(
                -1, -1, -1,
                "自定义模式",
                "使用自定义的布局、缩放和位置设置"
            )
        };
    }
    
    /**
     * 获取当前使用的预设模式
     */
    public static int getCurrentPresetMode() {
        int currentLayout = Settings.getReadingDirection();
        int currentScale = Settings.getPageScaling();
        int currentPosition = Settings.getStartPosition();
        
        ReadingModePreset[] presets = getAllPresets();
        
        // 检查是否匹配某个预设模式
        for (int i = 0; i < presets.length - 1; i++) { // 排除最后的自定义模式
            ReadingModePreset preset = presets[i];
            if (preset.layoutMode == currentLayout && 
                preset.scaleMode == currentScale && 
                preset.startPosition == currentPosition) {
                return i;
            }
        }
        
        return PRESET_CUSTOM; // 没有匹配到预设，返回自定义模式
    }
    
    /**
     * 应用预设模式
     */
    public static void applyPresetMode(int presetMode) {
        if (presetMode < 0 || presetMode >= getAllPresets().length) {
            Log.w(TAG, "Invalid preset mode: " + presetMode);
            return;
        }
        
        ReadingModePreset preset = getAllPresets()[presetMode];
        
        if (presetMode != PRESET_CUSTOM) {
            Settings.putReadingDirection(preset.layoutMode);
            Settings.putPageScaling(preset.scaleMode);
            Settings.putStartPosition(preset.startPosition);
            Settings.putGalleryReadingModePreset(presetMode);
            
            Log.d(TAG, String.format("Applied preset mode %d: %s", presetMode, preset.name));
        } else {
            // 自定义模式时只保存预设选择，不改变具体设置
            Settings.putGalleryReadingModePreset(presetMode);
        }
    }
    
    /**
     * 获取模式的显示名称
     */
    public static String getPresetName(int presetMode) {
        if (presetMode >= 0 && presetMode < getAllPresets().length) {
            return getAllPresets()[presetMode].name;
        }
        return "未知模式";
    }
    
    /**
     * 获取模式的描述
     */
    public static String getPresetDescription(int presetMode) {
        if (presetMode >= 0 && presetMode < getAllPresets().length) {
            return getAllPresets()[presetMode].description;
        }
        return "";
    }
    
    /**
     * 获取下一个预设模式（循环切换）
     */
    public static int getNextPresetMode() {
        int current = getCurrentPresetMode();
        int next = (current + 1) % (getAllPresets().length - 1); // 排除自定义模式
        return next;
    }
    
    /**
     * 快速切换到下一个模式
     */
    public static void switchToNextMode() {
        int nextMode = getNextPresetMode();
        applyPresetMode(nextMode);
        Log.d(TAG, "Switched to next mode: " + getPresetName(nextMode));
    }
    
    /**
     * 获取当前设置的详细信息
     */
    public static String getCurrentSettingsInfo() {
        StringBuilder info = new StringBuilder();
        info.append("当前阅读设置:\n");
        info.append("模式: ").append(getPresetName(getCurrentPresetMode())).append("\n");
        
        // 布局方向
        int layout = Settings.getReadingDirection();
        String layoutStr = "";
        switch (layout) {
            case GalleryView.LAYOUT_LEFT_TO_RIGHT:
                layoutStr = "从左到右";
                break;
            case GalleryView.LAYOUT_RIGHT_TO_LEFT:
                layoutStr = "从右到左";
                break;
            case GalleryView.LAYOUT_TOP_TO_BOTTOM:
                layoutStr = "从上到下";
                break;
        }
        info.append("布局: ").append(layoutStr).append("\n");
        
        // 缩放模式
        int scale = Settings.getPageScaling();
        String scaleStr = "";
        switch (scale) {
            case GalleryView.SCALE_ORIGIN:
                scaleStr = "原始大小";
                break;
            case GalleryView.SCALE_FIT_WIDTH:
                scaleStr = "适应宽度";
                break;
            case GalleryView.SCALE_FIT_HEIGHT:
                scaleStr = "适应高度";
                break;
            case GalleryView.SCALE_FIT:
                scaleStr = "适应页面";
                break;
            case GalleryView.SCALE_FIXED:
                scaleStr = "固定比例";
                break;
        }
        info.append("缩放: ").append(scaleStr).append("\n");
        
        // 起始位置
        int position = Settings.getStartPosition();
        String positionStr = "";
        switch (position) {
            case GalleryView.START_POSITION_TOP_LEFT:
                positionStr = "左上角";
                break;
            case GalleryView.START_POSITION_TOP_RIGHT:
                positionStr = "右上角";
                break;
            case GalleryView.START_POSITION_BOTTOM_LEFT:
                positionStr = "左下角";
                break;
            case GalleryView.START_POSITION_BOTTOM_RIGHT:
                positionStr = "右下角";
                break;
            case GalleryView.START_POSITION_CENTER:
                positionStr = "中央";
                break;
        }
        info.append("位置: ").append(positionStr);
        
        return info.toString();
    }
    
    /**
     * 检查是否推荐某个模式（基于内容类型）
     */
    public static int getRecommendedMode(String galleryTitle, String galleryCategory) {
        if (galleryTitle == null) galleryTitle = "";
        if (galleryCategory == null) galleryCategory = "";
        
        String title = galleryTitle.toLowerCase();
        String category = galleryCategory.toLowerCase();
        
        // 根据标题和分类推荐模式
        if (title.contains("webtoon") || title.contains("manhwa") || 
            category.contains("webtoon") || title.contains("长条")) {
            return PRESET_WEBTOON;
        }
        
        if (title.contains("manga") || title.contains("doujinshi") ||
            category.contains("manga") || category.contains("doujinshi")) {
            return PRESET_MANGA_RTL;
        }
        
        if (title.contains("comic") || title.contains("western") ||
            category.contains("western")) {
            return PRESET_MANGA_LTR;
        }
        
        if (title.contains("artbook") || title.contains("image") ||
            category.contains("image") || title.contains("图集")) {
            return PRESET_BOOK_MODE;
        }
        
        // 默认返回日漫模式
        return PRESET_MANGA_RTL;
    }
    
    /**
     * 保存自定义模式配置
     */
    public static void saveCustomMode() {
        int currentLayout = Settings.getReadingDirection();
        int currentScale = Settings.getPageScaling();
        int currentPosition = Settings.getStartPosition();
        
        // 保存自定义配置
        Settings.putInt("custom_reading_layout", currentLayout);
        Settings.putInt("custom_reading_scale", currentScale);
        Settings.putInt("custom_reading_position", currentPosition);
        
        Log.d(TAG, "Custom reading mode saved");
    }
    
    /**
     * 加载自定义模式配置
     */
    public static void loadCustomMode() {
        int customLayout = Settings.getInt("custom_reading_layout", GalleryView.LAYOUT_RIGHT_TO_LEFT);
        int customScale = Settings.getInt("custom_reading_scale", GalleryView.SCALE_FIT);
        int customPosition = Settings.getInt("custom_reading_position", GalleryView.START_POSITION_TOP_RIGHT);
        
        Settings.putReadingDirection(customLayout);
        Settings.putPageScaling(customScale);
        Settings.putStartPosition(customPosition);
        
        Log.d(TAG, "Custom reading mode loaded");
    }
}