/*
 * EhViewer Network Module - CookieManager
 * Cookie管理器 - 处理HTTP Cookie的存储、管理和自动添加
 */

package com.hippo.ehviewer.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cookie管理器
 * 负责Cookie的存储、读取和管理
 */
public class CookieManager implements CookieStore {

    private static final String TAG = CookieManager.class.getSimpleName();
    private static final String PREF_NAME = "network_cookies";
    private static final String KEY_COOKIES = "cookies_data";

    private final SharedPreferences mPreferences;
    private final Map<String, List<HttpCookie>> mCookies;

    public CookieManager(Context context) {
        mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mCookies = new HashMap<>();
        loadCookies();
    }

    /**
     * 添加Cookie
     * @param uri URI
     * @param cookie Cookie对象
     */
    @Override
    public void add(URI uri, HttpCookie cookie) {
        String domain = getDomain(uri);
        List<HttpCookie> cookies = mCookies.get(domain);
        if (cookies == null) {
            cookies = new ArrayList<>();
            mCookies.put(domain, cookies);
        }

        // 移除同名的旧Cookie
        cookies.removeIf(c -> c.getName().equals(cookie.getName()));
        cookies.add(cookie);

        saveCookies();
        Log.d(TAG, "Added cookie: " + cookie.getName() + "=" + cookie.getValue());
    }

    /**
     * 获取指定URI的所有Cookie
     * @param uri URI
     * @return Cookie列表
     */
    @Override
    public List<HttpCookie> get(URI uri) {
        String domain = getDomain(uri);
        List<HttpCookie> cookies = mCookies.get(domain);
        if (cookies == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(cookies);
    }

    /**
     * 获取所有URI的所有Cookie
     * @return 所有Cookie的Map
     */
    @Override
    public List<HttpCookie> getCookies() {
        List<HttpCookie> allCookies = new ArrayList<>();
        for (List<HttpCookie> cookies : mCookies.values()) {
            allCookies.addAll(cookies);
        }
        return allCookies;
    }

    /**
     * 获取所有Cookie的URI
     * @return URI列表
     */
    @Override
    public List<URI> getURIs() {
        List<URI> uris = new ArrayList<>();
        for (String domain : mCookies.keySet()) {
            try {
                uris.add(new URI("http://" + domain));
            } catch (Exception e) {
                Log.e(TAG, "Invalid domain: " + domain, e);
            }
        }
        return uris;
    }

    /**
     * 移除指定URI的指定Cookie
     * @param uri URI
     * @param cookie Cookie对象
     * @return 是否移除成功
     */
    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        String domain = getDomain(uri);
        List<HttpCookie> cookies = mCookies.get(domain);
        if (cookies != null) {
            boolean removed = cookies.remove(cookie);
            if (removed) {
                saveCookies();
                Log.d(TAG, "Removed cookie: " + cookie.getName());
            }
            return removed;
        }
        return false;
    }

    /**
     * 移除所有Cookie
     * @return 是否移除成功
     */
    @Override
    public boolean removeAll() {
        mCookies.clear();
        saveCookies();
        Log.d(TAG, "Removed all cookies");
        return true;
    }

    /**
     * 获取域名
     */
    private String getDomain(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return "";
        }
        return host.toLowerCase();
    }

    /**
     * 从SharedPreferences加载Cookie
     */
    private void loadCookies() {
        String cookiesData = mPreferences.getString(KEY_COOKIES, "");
        if (!cookiesData.isEmpty()) {
            // 解析Cookie数据并恢复到内存中
            // 这里需要实现Cookie的序列化/反序列化逻辑
            Log.d(TAG, "Loaded cookies from storage");
        }
    }

    /**
     * 保存Cookie到SharedPreferences
     */
    private void saveCookies() {
        // 将Cookie数据序列化并保存到SharedPreferences
        // 这里需要实现Cookie的序列化逻辑
        String cookiesData = serializeCookies();
        mPreferences.edit().putString(KEY_COOKIES, cookiesData).apply();
        Log.d(TAG, "Saved cookies to storage");
    }

    /**
     * 序列化Cookie数据
     */
    private String serializeCookies() {
        // 实现Cookie序列化逻辑
        return "";
    }

    /**
     * 获取指定域名的Cookie
     * @param domain 域名
     * @return Cookie列表
     */
    public List<HttpCookie> getCookiesForDomain(String domain) {
        return mCookies.get(domain.toLowerCase());
    }

    /**
     * 设置指定域名的Cookie
     * @param domain 域名
     * @param cookies Cookie列表
     */
    public void setCookiesForDomain(String domain, List<HttpCookie> cookies) {
        mCookies.put(domain.toLowerCase(), new ArrayList<>(cookies));
        saveCookies();
    }

    /**
     * 清除指定域名的所有Cookie
     * @param domain 域名
     */
    public void clearCookiesForDomain(String domain) {
        mCookies.remove(domain.toLowerCase());
        saveCookies();
    }
}
