package com.hippo.ehviewer.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import com.hippo.ehviewer.R;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 时钟和天气桌面小部件
 * 显示时间、日期、天气信息，点击可快速打开天气网站
 */
public class ClockWeatherWidget extends BaseEhWidget {
    
    private static final String TAG = "ClockWeatherWidget";
    private static final String PREFS_NAME = "clock_weather_widget";
    
    // 天气API配置（使用免费的OpenWeatherMap API）
    private static final String WEATHER_API_KEY = "your_api_key_here"; // 需要替换为实际的API key
    private static final String WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    // 自定义Actions
    private static final String ACTION_UPDATE_TIME = "com.hippo.ehviewer.widget.UPDATE_TIME";
    private static final String ACTION_UPDATE_WEATHER = "com.hippo.ehviewer.widget.UPDATE_WEATHER";
    private static final String ACTION_OPEN_WEATHER = "com.hippo.ehviewer.widget.OPEN_WEATHER";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        
        // 启动定时更新
        startPeriodicUpdates(context);
    }
    
    @Override
    protected RemoteViews createRemoteViews(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_clock_weather);
        
        // 更新时间显示
        updateTimeDisplay(context, views);
        
        // 更新天气显示
        updateWeatherDisplay(context, views);
        
        return views;
    }
    
    @Override
    protected void setupCustomClickActions(Context context, RemoteViews views, int appWidgetId) {
        // 点击时间区域打开时钟应用
        views.setOnClickPendingIntent(R.id.time_container, 
            createBrowserPendingIntent(context, appWidgetId * 10, 
                "https://time.is/", "世界时钟"));
        
        // 点击天气区域打开天气网站
        views.setOnClickPendingIntent(R.id.weather_container,
            createBrowserPendingIntent(context, appWidgetId * 10 + 1,
                "https://weather.com/", "天气预报"));
                
        // 点击日期区域打开日历
        views.setOnClickPendingIntent(R.id.date_container,
            createBrowserPendingIntent(context, appWidgetId * 10 + 2,
                "https://calendar.google.com/", "Google日历"));
    }
    
    @Override
    protected void handleCustomAction(Context context, Intent intent, String action) {
        switch (action) {
            case ACTION_UPDATE_TIME:
                updateAllWidgetsTime(context);
                break;
                
            case ACTION_UPDATE_WEATHER:
                updateAllWidgetsWeather(context);
                break;
                
            case ACTION_OPEN_WEATHER:
                String city = intent.getStringExtra("city");
                openWeatherPage(context, city);
                break;
        }
    }
    
    /**
     * 更新时间显示
     */
    private void updateTimeDisplay(Context context, RemoteViews views) {
        Calendar calendar = Calendar.getInstance();
        
        // 格式化时间
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault());
        
        String currentTime = timeFormat.format(calendar.getTime());
        String currentDate = dateFormat.format(calendar.getTime());
        
        // 更新视图
        views.setTextViewText(R.id.widget_time, currentTime);
        views.setTextViewText(R.id.widget_date, currentDate);
        
        // 根据时间改变问候语
        String greeting = getTimeBasedGreeting(calendar.get(Calendar.HOUR_OF_DAY));
        views.setTextViewText(R.id.widget_greeting, greeting);
    }
    
    /**
     * 更新天气显示
     */
    private void updateWeatherDisplay(Context context, RemoteViews views) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 从缓存获取天气信息
        String weatherData = prefs.getString("weather_data", "");
        long lastUpdate = prefs.getLong("weather_last_update", 0);
        
        // 如果缓存超过30分钟，异步更新天气
        if (System.currentTimeMillis() - lastUpdate > 30 * 60 * 1000) {
            fetchWeatherAsync(context);
        }
        
        if (!weatherData.isEmpty()) {
            try {
                JSONObject weather = new JSONObject(weatherData);
                displayWeatherInfo(views, weather);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse weather data", e);
                displayDefaultWeather(views);
            }
        } else {
            displayDefaultWeather(views);
        }
    }
    
    /**
     * 显示天气信息
     */
    private void displayWeatherInfo(RemoteViews views, JSONObject weather) {
        try {
            // 获取温度
            JSONObject main = weather.getJSONObject("main");
            double temp = main.getDouble("temp");
            int temperature = (int) Math.round(temp - 273.15); // 转换为摄氏度
            
            // 获取天气描述
            JSONObject weatherObj = weather.getJSONArray("weather").getJSONObject(0);
            String description = weatherObj.getString("description");
            String icon = weatherObj.getString("icon");
            
            // 获取城市名
            String cityName = weather.getString("name");
            
            // 更新视图
            views.setTextViewText(R.id.widget_temperature, temperature + "°C");
            views.setTextViewText(R.id.widget_weather_desc, description);
            views.setTextViewText(R.id.widget_location, cityName);
            
            // 根据天气图标设置背景或颜色
            updateWeatherIcon(views, icon);
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to display weather info", e);
            displayDefaultWeather(views);
        }
    }
    
    /**
     * 显示默认天气信息
     */
    private void displayDefaultWeather(RemoteViews views) {
        views.setTextViewText(R.id.widget_temperature, "--°");
        views.setTextViewText(R.id.widget_weather_desc, "获取天气中...");
        views.setTextViewText(R.id.widget_location, "定位中...");
    }
    
    /**
     * 异步获取天气信息
     */
    private void fetchWeatherAsync(Context context) {
        widgetExecutor.submit(() -> {
            try {
                // 获取位置信息
                Location location = getCurrentLocation(context);
                if (location != null) {
                    // 调用天气API
                    String weatherData = fetchWeatherData(location.getLatitude(), location.getLongitude());
                    
                    if (weatherData != null) {
                        // 保存到缓存
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                            .putString("weather_data", weatherData)
                            .putLong("weather_last_update", System.currentTimeMillis())
                            .apply();
                        
                        // 更新所有小部件
                        forceUpdateAllWidgets(context);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch weather data", e);
            }
        });
    }
    
    /**
     * 获取当前位置
     */
    private Location getCurrentLocation(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            
            // 检查权限
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Location permission not granted");
                return getDefaultLocation(); // 返回默认位置（如北京）
            }
            
            // 获取最后已知位置
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location == null) {
                location = getDefaultLocation();
            }
            
            return location;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get location", e);
            return getDefaultLocation();
        }
    }
    
    /**
     * 获取默认位置（北京）
     */
    private Location getDefaultLocation() {
        Location location = new Location("default");
        location.setLatitude(39.9042); // 北京纬度
        location.setLongitude(116.4074); // 北京经度
        return location;
    }
    
    /**
     * 从API获取天气数据
     */
    private String fetchWeatherData(double lat, double lon) {
        try {
            // 如果没有API key，返回模拟数据
            if ("your_api_key_here".equals(WEATHER_API_KEY)) {
                return createMockWeatherData();
            }
            
            String urlString = String.format("%s?lat=%.2f&lon=%.2f&appid=%s&lang=zh_cn", 
                WEATHER_BASE_URL, lat, lon, WEATHER_API_KEY);
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                Log.w(TAG, "Weather API returned code: " + responseCode);
                return createMockWeatherData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch weather data from API", e);
            return createMockWeatherData();
        }
    }
    
    /**
     * 创建模拟天气数据
     */
    private String createMockWeatherData() {
        try {
            JSONObject mockData = new JSONObject();
            mockData.put("name", "北京");
            
            JSONObject main = new JSONObject();
            main.put("temp", 295.15); // 22°C in Kelvin
            mockData.put("main", main);
            
            JSONObject weather = new JSONObject();
            weather.put("description", "晴朗");
            weather.put("icon", "01d");
            
            org.json.JSONArray weatherArray = new org.json.JSONArray();
            weatherArray.put(weather);
            mockData.put("weather", weatherArray);
            
            return mockData.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }
    
    /**
     * 根据时间获取问候语
     */
    private String getTimeBasedGreeting(int hour) {
        if (hour >= 5 && hour < 12) {
            return "早上好";
        } else if (hour >= 12 && hour < 18) {
            return "下午好";
        } else if (hour >= 18 && hour < 22) {
            return "晚上好";
        } else {
            return "夜深了";
        }
    }
    
    /**
     * 更新天气图标
     */
    private void updateWeatherIcon(RemoteViews views, String iconCode) {
        // 根据天气图标代码设置相应的背景或图标
        // 这里可以根据iconCode设置不同的颜色主题
        int backgroundResource = R.drawable.widget_background_default;
        
        switch (iconCode.substring(0, 2)) {
            case "01": // 晴天
                backgroundResource = R.drawable.widget_background_sunny;
                break;
            case "02":
            case "03":
            case "04": // 多云
                backgroundResource = R.drawable.widget_background_cloudy;
                break;
            case "09":
            case "10": // 雨天
                backgroundResource = R.drawable.widget_background_rainy;
                break;
            case "11": // 雷雨
                backgroundResource = R.drawable.widget_background_stormy;
                break;
            case "13": // 雪天
                backgroundResource = R.drawable.widget_background_snowy;
                break;
        }
        
        try {
            views.setInt(R.id.widget_container, "setBackgroundResource", backgroundResource);
        } catch (Exception e) {
            // 如果背景资源不存在，使用默认
            Log.w(TAG, "Weather background not found, using default");
        }
    }
    
    /**
     * 启动定时更新
     */
    private void startPeriodicUpdates(Context context) {
        // 每分钟更新时间
        widgetExecutor.scheduleAtFixedRate(() -> {
            updateAllWidgetsTime(context);
        }, 0, 1, TimeUnit.MINUTES);
        
        // 每30分钟更新天气
        widgetExecutor.scheduleAtFixedRate(() -> {
            updateAllWidgetsWeather(context);
        }, 5, 30, TimeUnit.MINUTES);
    }
    
    /**
     * 更新所有小部件的时间
     */
    private void updateAllWidgetsTime(Context context) {
        try {
            forceUpdateAllWidgets(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update widget time", e);
        }
    }
    
    /**
     * 更新所有小部件的天气
     */
    private void updateAllWidgetsWeather(Context context) {
        fetchWeatherAsync(context);
    }
    
    /**
     * 打开天气页面
     */
    private void openWeatherPage(Context context, String city) {
        String url = "https://weather.com/";
        if (city != null && !city.isEmpty()) {
            url = "https://weather.com/weather/today/l/" + city;
        }
        
        openBrowser(context, url, "天气预报");
    }
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        startPeriodicUpdates(context);
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // 清理定时任务在基类的静态executor中处理
    }
}