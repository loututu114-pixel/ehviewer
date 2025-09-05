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

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.hippo.scene.SceneFragment;
import com.hippo.ehviewer.Settings;

import java.util.Locale;

/**
 * Google Analytics 4 integration for 王子的公主 app
 */
public final class Analytics {

    private static final String TAG = "Analytics";
    private static final String DEVICE_LANGUAGE = "device_language";
    private static final String DEVICE_COUNTRY = "device_country";
    private static final String APP_NAME = "王子公主";

    private static FirebaseAnalytics firebaseAnalytics;

    private Analytics() {}

    public static void start(Context context) {
        try {
            // 检查是否处于降级模式
            if (Settings.getGooglePlayServicesFallback()) {
                Log.w(TAG, "Google Play Services fallback mode enabled, skipping Firebase Analytics initialization");
                return;
            }

            // 检查Google Play服务是否可用
            if (!isGooglePlayServicesAvailable(context)) {
                Log.w(TAG, "Google Play Services not available, Firebase Analytics will be disabled");
                Settings.putGooglePlayServicesFallback(true);
                return;
            }

            // Initialize Firebase Analytics
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);

            // Set user properties
            String userId = Settings.getUserID();
            if (!TextUtils.isEmpty(userId)) {
                firebaseAnalytics.setUserId(userId);
            }

            // Set device language
            Locale locale = Locale.getDefault();
            String language = locale.getLanguage();
            if (TextUtils.isEmpty(language)) {
                language = "none";
            } else {
                language = language.toLowerCase();
            }

            String country = locale.getCountry();
            if (!TextUtils.isEmpty(country)) {
                country = country.toUpperCase();
            } else {
                country = "none";
            }

            firebaseAnalytics.setUserProperty(DEVICE_LANGUAGE, language);
            firebaseAnalytics.setUserProperty(DEVICE_COUNTRY, country);

            // Set app name
            firebaseAnalytics.setUserProperty("app_name", APP_NAME);

            Log.i(TAG, "Google Analytics initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize analytics", e);
            // 启用降级模式
            Settings.putGooglePlayServicesFallback(true);
        }
    }

    public static boolean isEnabled() {
        return firebaseAnalytics != null && Settings.getEnableAnalytics();
    }

    public static void onSceneView(SceneFragment scene) {
        if (isEnabled()) {
            try {
                Bundle bundle = new Bundle();
                bundle.putString("scene_simple_class", scene.getClass().getSimpleName());
                bundle.putString("scene_class", scene.getClass().getName());
                bundle.putString("app_name", APP_NAME);

                firebaseAnalytics.logEvent("scene_view", bundle);

            } catch (Exception e) {
                Log.e(TAG, "Failed to track scene view", e);
            }
        }
    }

    public static void logEvent(String eventName, Bundle parameters) {
        if (isEnabled()) {
            try {
                // Add app name to all events
                if (parameters == null) {
                    parameters = new Bundle();
                }
                parameters.putString("app_name", APP_NAME);

                firebaseAnalytics.logEvent(eventName, parameters);

            } catch (Exception e) {
                Log.e(TAG, "Failed to log event: " + eventName, e);
            }
        }
    }

    public static void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    public static void setUserProperty(String name, String value) {
        if (isEnabled()) {
            try {
                firebaseAnalytics.setUserProperty(name, value);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set user property: " + name, e);
            }
        }
    }

    public static void trackScreen(String screenName) {
        if (isEnabled()) {
            try {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
                bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName);
                bundle.putString("app_name", APP_NAME);

                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);

            } catch (Exception e) {
                Log.e(TAG, "Failed to track screen: " + screenName, e);
            }
        }
    }

    public static void trackUserAction(String action, String category) {
        if (isEnabled()) {
            try {
                Bundle bundle = new Bundle();
                bundle.putString("action", action);
                bundle.putString("category", category);
                bundle.putString("app_name", APP_NAME);

                firebaseAnalytics.logEvent("user_action", bundle);

            } catch (Exception e) {
                Log.e(TAG, "Failed to track user action: " + action, e);
            }
        }
    }

    public static void trackAppException(Throwable throwable) {
        if (isEnabled()) {
            try {
                Bundle bundle = new Bundle();
                bundle.putString("exception_message", throwable.getMessage());
                bundle.putString("exception_class", throwable.getClass().getName());
                bundle.putString("app_name", APP_NAME);

                firebaseAnalytics.logEvent("app_exception", bundle);

            } catch (Exception e) {
                Log.e(TAG, "Failed to track app exception", e);
            }
        }
    }

    /**
     * 检查Google Play服务是否可用
     */
    private static boolean isGooglePlayServicesAvailable(Context context) {
        try {
            // 检查Google Play服务包是否存在
            android.content.pm.PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.google.android.gms", 0);

            // 检查Google Play服务版本是否足够
            try {
                Class<?> googleApiAvailabilityClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability");
                Object instance = googleApiAvailabilityClass.getMethod("getInstance").invoke(null);
                int result = (Integer) googleApiAvailabilityClass.getMethod("isGooglePlayServicesAvailable", android.content.Context.class)
                    .invoke(instance, context);

                // GoogleApiAvailability.SUCCESS = 0
                return result == 0;
            } catch (Exception e) {
                Log.w(TAG, "Could not check Google Play Services availability via API, assuming available", e);
                return true; // 如果无法检查API，假设可用
            }

        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Google Play Services package not found");
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error checking Google Play Services availability", e);
            return false;
        }
    }

    /**
     * 检查是否处于降级模式
     */
    public static boolean isInFallbackMode() {
        return Settings.getGooglePlayServicesFallback();
    }
}
