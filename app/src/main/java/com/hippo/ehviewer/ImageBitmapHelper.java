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

package com.hippo.ehviewer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.conaco.ValueHelper;
import com.hippo.lib.image.Image;
import com.hippo.streampipe.InputStreamPipe;

import java.io.FileInputStream;
import java.io.IOException;

public class ImageBitmapHelper implements ValueHelper<Image> {

    // 基于设备内存动态调整图片缓存大小
    private static final int MAX_CACHE_SIZE = getOptimalImageCacheSize();
    
    private static int getOptimalImageCacheSize() {
        long totalMemory = Runtime.getRuntime().maxMemory();
        
        if (totalMemory >= 8L * 1024 * 1024 * 1024) { // 8GB+设备
            return 1024 * 1024; // 1M像素
        } else if (totalMemory >= 6L * 1024 * 1024 * 1024) { // 6GB设备
            return 768 * 768; // 约590K像素
        } else if (totalMemory >= 4L * 1024 * 1024 * 1024) { // 4GB设备
            return 640 * 640; // 约410K像素
        } else {
            return 512 * 512; // 原配置 262K像素
        }
    }

    @Nullable
    @Override
    public Image decode(@NonNull InputStreamPipe isPipe) {
        try {
            isPipe.obtain();
            FileInputStream is = (FileInputStream) isPipe.open();
            return Image.decode(is,true);
        } catch (OutOfMemoryError e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            isPipe.close();
            isPipe.release();
        }
    }

    @Override
    public int sizeOf(@NonNull String key, @NonNull Image value) {
        return value.getWidth() * value.getHeight() * 4 /* value.getByteCount() TODO Update Image */;
    }

//    @Override
//    public void onAddToMemoryCache(@NonNull String key, @NonNull ImageBitmap value) {
//        value.obtain();
//    }

    @Override
    public void onAddToMemoryCache(@NonNull Image oldValue) {
        oldValue.obtain();
    }

    @Override
    public void onRemoveFromMemoryCache(@NonNull String key, @NonNull Image oldValue) {
        oldValue.release();
    }

    @Override
    public boolean useMemoryCache(@NonNull String key, Image value) {
        if (value != null) {
            return value.getWidth() * value.getHeight() <= MAX_CACHE_SIZE
                    /* value.getByteCount() <= MAX_CACHE_BYTE_COUNT TODO Update Image */;
        } else {
            return true;
        }
    }
}
