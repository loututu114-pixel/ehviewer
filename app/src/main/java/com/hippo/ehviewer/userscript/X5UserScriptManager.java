package com.hippo.ehviewer.userscript;

import android.content.Context;
import android.util.Log;
import android.webkit.ValueCallback;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.X5WebViewManager;
import com.tencent.smtt.sdk.WebView;

import java.util.List;

/**
 * X5内核专用用户脚本管理器
 * 为腾讯X5 WebView提供油猴脚本兼容性
 */
public class X5UserScriptManager extends UserScriptManager {

    private static final String TAG = "X5UserScriptManager";
    private X5WebViewManager x5Manager;

    private X5UserScriptManager(Context context) {
        super(context);
        this.x5Manager = X5WebViewManager.getInstance();
    }

    private static X5UserScriptManager instance;

    public static synchronized X5UserScriptManager getInstance(Context context) {
        if (instance == null) {
            instance = new X5UserScriptManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 为X5 WebView注入用户脚本
     */
    public void injectScriptsForX5(android.webkit.WebView systemWebView, String url) {
        if (!isEnabled()) return;

        // 检查是否为X5 WebView包装器
        Object x5WebViewTag = systemWebView.getTag(R.id.x5_webview_tag);
        if (x5WebViewTag instanceof WebView) {
            WebView x5WebView = (WebView) x5WebViewTag;
            injectScriptsToX5WebView(x5WebView, url);
        } else {
            // 回退到系统WebView
            Log.w(TAG, "Not an X5 WebView wrapper, fallback to system WebView injection");
            injectScripts(systemWebView, url);
        }
    }

    /**
     * 直接为X5 WebView注入脚本
     */
    private void injectScriptsToX5WebView(WebView x5WebView, String url) {
        if (!isEnabled()) return;

        List<UserScript> matchingScripts = getMatchingScripts(url);
        Log.d(TAG, "Injecting " + matchingScripts.size() + " scripts for X5 WebView: " + url);

        for (UserScript script : matchingScripts) {
            injectScriptToX5WebView(x5WebView, script);
        }
    }

    /**
     * 注入单个脚本到X5 WebView
     */
    private void injectScriptToX5WebView(WebView x5WebView, UserScript script) {
        try {
            if (!isValidScript(script)) {
                Log.w(TAG, "Script validation failed: " + script.getId());
                return;
            }

            // X5 WebView的脚本注入实现
            injectX5GMAPI(x5WebView, script);
            injectX5ScriptContent(x5WebView, script);

            Log.d(TAG, "Successfully injected script to X5 WebView: " + script.getName());

        } catch (Exception e) {
            Log.e(TAG, "Error injecting script to X5 WebView: " + script.getId(), e);
        }
    }

    /**
     * 为X5 WebView注入GM API
     */
    private void injectX5GMAPI(WebView x5WebView, UserScript script) {
        String gmApi = X5ScriptInjector.generateX5GMAPI(script);

        x5WebView.evaluateJavascript(gmApi, new com.tencent.smtt.sdk.ValueCallback<String>() {
            @Override
            public void onReceiveValue(String result) {
                if (result != null && result.contains("error")) {
                    Log.w(TAG, "X5 GM API injection failed for script: " + script.getId() + ", result: " + result);
                } else {
                    Log.d(TAG, "X5 GM API injected successfully for script: " + script.getId());
                }
            }
        });
    }

    /**
     * 为X5 WebView注入脚本内容
     */
    private void injectX5ScriptContent(WebView x5WebView, UserScript script) {
        String injectedScript = X5ScriptInjector.wrapX5Script(script);

        x5WebView.evaluateJavascript(injectedScript, new com.tencent.smtt.sdk.ValueCallback<String>() {
            @Override
            public void onReceiveValue(String result) {
                if (result != null && result.contains("error")) {
                    Log.e(TAG, "X5 Script injection failed: " + result + " for script: " + script.getId());
                } else {
                    Log.d(TAG, "X5 Script injected successfully: " + script.getId());
                }
            }
        });
    }

    /**
     * 检查是否支持X5内核脚本注入
     */
    public boolean isX5ScriptInjectionSupported() {
        return x5Manager.isX5Available() && x5Manager.isX5Initialized();
    }

    /**
     * 获取X5脚本注入状态信息
     */
    public String getX5ScriptInjectionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("X5可用: ").append(x5Manager.isX5Available()).append("\n");
        status.append("X5已初始化: ").append(x5Manager.isX5Initialized()).append("\n");
        status.append("脚本注入支持: ").append(isX5ScriptInjectionSupported()).append("\n");
        status.append("X5版本: ").append(x5Manager.getX5Version() != null ? x5Manager.getX5Version() : "未知");

        return status.toString();
    }
}
