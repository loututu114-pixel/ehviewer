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

package com.hippo.ehviewer.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * 应用启动器 - 统一处理各种应用间的跳转
 */
public class AppLauncher {

    private static final String TAG = "AppLauncher";

    /**
     * 打电话
     */
    public static boolean dialPhone(@NonNull Context context, String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No dialer app found", e);
            Toast.makeText(context, "未找到拨号应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 直接打电话（需要权限）
     */
    public static boolean callPhone(@NonNull Context context, String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            context.startActivity(intent);
            return true;
        } catch (SecurityException e) {
            android.util.Log.e(TAG, "Call permission denied", e);
            Toast.makeText(context, "拨打电话权限被拒绝", Toast.LENGTH_SHORT).show();
            return false;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No call app found", e);
            Toast.makeText(context, "未找到通话应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 发短信
     */
    public static boolean sendSms(@NonNull Context context, String phoneNumber, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phoneNumber));
            if (message != null && !message.isEmpty()) {
                intent.putExtra("sms_body", message);
            }
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No SMS app found", e);
            Toast.makeText(context, "未找到短信应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 发邮件
     */
    public static boolean sendEmail(@NonNull Context context, String[] to, String[] cc, String subject, String body) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));

            if (to != null && to.length > 0) {
                intent.putExtra(Intent.EXTRA_EMAIL, to);
            }
            if (cc != null && cc.length > 0) {
                intent.putExtra(Intent.EXTRA_CC, cc);
            }
            if (subject != null) {
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            }
            if (body != null) {
                intent.putExtra(Intent.EXTRA_TEXT, body);
            }

            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No email app found", e);
            Toast.makeText(context, "未找到邮件应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开地图
     */
    public static boolean openMap(@NonNull Context context, double latitude, double longitude, String label) {
        try {
            // 尝试使用Google Maps
            Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" +
                    latitude + "," + longitude + "(" + (label != null ? label : "Location") + ")");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (isIntentAvailable(context, mapIntent)) {
                context.startActivity(mapIntent);
                return true;
            }

            // 如果没有Google Maps，使用通用地图意图
            Intent generalMapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            context.startActivity(generalMapIntent);
            return true;

        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No map app found", e);
            Toast.makeText(context, "未找到地图应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开网页
     */
    public static boolean openWebPage(@NonNull Context context, String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No browser app found", e);
            Toast.makeText(context, "未找到浏览器应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开应用市场
     */
    public static boolean openAppMarket(@NonNull Context context, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));

            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }

            // 如果没有应用市场，打开网页版
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            context.startActivity(webIntent);
            return true;

        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No app market found", e);
            Toast.makeText(context, "未找到应用市场", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 分享文本
     */
    public static boolean shareText(@NonNull Context context, String text, String title) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            if (title != null) {
                intent.putExtra(Intent.EXTRA_TITLE, title);
            }

            Intent chooser = Intent.createChooser(intent, "分享");
            context.startActivity(chooser);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No share app found", e);
            Toast.makeText(context, "未找到分享应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 分享文件
     */
    public static boolean shareFile(@NonNull Context context, Uri fileUri, String mimeType, String title) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType != null ? mimeType : "*/*");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            if (title != null) {
                intent.putExtra(Intent.EXTRA_TITLE, title);
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, "分享文件");
            context.startActivity(chooser);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No share app found", e);
            Toast.makeText(context, "未找到分享应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开相机
     */
    public static boolean openCamera(@NonNull Context context) {
        try {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No camera app found", e);
            Toast.makeText(context, "未找到相机应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开相册
     */
    public static boolean openGallery(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No gallery app found", e);
            Toast.makeText(context, "未找到相册应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开联系人
     */
    public static boolean openContacts(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.ContactsContract.Contacts.CONTENT_URI);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No contacts app found", e);
            Toast.makeText(context, "未找到联系人应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开日历
     */
    public static boolean openCalendar(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No calendar app found", e);
            Toast.makeText(context, "未找到日历应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开计算器
     */
    public static boolean openCalculator(@NonNull Context context) {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_CALCULATOR);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No calculator app found", e);
            Toast.makeText(context, "未找到计算器应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开时钟/闹钟
     */
    public static boolean openClock(@NonNull Context context) {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage("com.android.deskclock");
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No clock app found", e);
            Toast.makeText(context, "未找到时钟应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开设置
     */
    public static boolean openSettings(@NonNull Context context) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No settings app found", e);
            Toast.makeText(context, "无法打开设置", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开WIFI设置
     */
    public static boolean openWifiSettings(@NonNull Context context) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No wifi settings found", e);
            Toast.makeText(context, "无法打开WIFI设置", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 打开蓝牙设置
     */
    public static boolean openBluetoothSettings(@NonNull Context context) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            android.util.Log.e(TAG, "No bluetooth settings found", e);
            Toast.makeText(context, "无法打开蓝牙设置", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 根据包名打开应用
     */
    public static boolean openAppByPackage(@NonNull Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                context.startActivity(intent);
                return true;
            } else {
                android.util.Log.e(TAG, "App not found: " + packageName);
                Toast.makeText(context, "应用未安装", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to open app: " + packageName, e);
            Toast.makeText(context, "无法打开应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 检查意图是否可用
     */
    private static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    /**
     * 通用意图处理 - 处理各种URL scheme
     */
    public static boolean handleUniversalUrl(@NonNull Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();

            if (scheme == null) return false;

            switch (scheme.toLowerCase()) {
                case "tel":
                    return dialPhone(context, uri.getSchemeSpecificPart());
                case "mailto":
                    String email = uri.getSchemeSpecificPart();
                    return sendEmail(context, new String[]{email}, null, null, null);
                case "sms":
                    String phone = uri.getSchemeSpecificPart();
                    return sendSms(context, phone, null);
                case "geo":
                    // 处理地理位置
                    return openMap(context, 0, 0, "Location");
                case "http":
                case "https":
                    return openWebPage(context, url);
                case "intent":
                    // 处理intent URL
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        context.startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Failed to parse intent URL", e);
                        return false;
                    }
                default:
                    // 尝试通用处理
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    context.startActivity(intent);
                    return true;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to handle universal URL", e);
            Toast.makeText(context, "无法处理链接: " + url, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
