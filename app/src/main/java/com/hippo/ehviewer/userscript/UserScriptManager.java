package com.hippo.ehviewer.userscript;

import android.content.Context;
import android.webkit.WebView;
import android.util.Log;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.X5WebViewManager;
import com.hippo.ehviewer.util.WebViewKernelDetector;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用户脚本管理器 - 负责管理用户脚本的安装、更新、启用/禁用等功能
 */
public class UserScriptManager {

    private static UserScriptManager instance;
    private Context context;
    private List<UserScript> userScripts;
    private ScriptStorage scriptStorage;
    private boolean isEnabled;
    private X5WebViewManager x5Manager;
    private X5UserScriptManager x5ScriptManager;

    protected UserScriptManager(Context context) {
        this.context = context;
        this.userScripts = new CopyOnWriteArrayList<>();
        this.scriptStorage = new ScriptStorage(context);
        this.isEnabled = true;

        // 延迟初始化X5相关组件，避免循环依赖
        this.x5Manager = null;
        this.x5ScriptManager = null;

        // 加载已保存的脚本
        loadPersistedScripts();
    }

    public static synchronized UserScriptManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserScriptManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 加载已保存的脚本
     */
    private void loadPersistedScripts() {
        try {
            List<UserScript> savedScripts = scriptStorage.loadScripts();
            if (savedScripts != null) {
                userScripts.addAll(savedScripts);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载assets中的默认脚本
     */
    public void loadDefaultScriptsFromAssets() {
        try {
            String[] assetFiles = context.getAssets().list("");
            if (assetFiles != null) {
                for (String fileName : assetFiles) {
                    if (fileName.endsWith(".js") && (fileName.contains("enhancer") || fileName.contains("blocker") || fileName.contains("intercept") || fileName.contains("app"))) {
                        try {
                            // 检查是否已经安装
                            boolean alreadyInstalled = false;
                            for (UserScript script : userScripts) {
                                if (script.getId().equals(fileName)) {
                                    alreadyInstalled = true;
                                    break;
                                }
                            }

                            if (!alreadyInstalled) {
                                // 从assets加载脚本内容
                                String scriptContent = loadAssetScript(fileName);
                                if (scriptContent != null) {
                                    UserScript script = UserScriptParser.parse(scriptContent);
                                    if (script != null) {
                                        userScripts.add(script);
                                        android.util.Log.d("UserScriptManager", "Loaded default script: " + fileName);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("UserScriptManager", "Failed to load script: " + fileName, e);
                        }
                    }
                }
                // 保存到存储
                scriptStorage.saveScripts(userScripts);
            }
        } catch (Exception e) {
            android.util.Log.e("UserScriptManager", "Failed to load default scripts from assets", e);
        }
    }

    /**
     * 从assets加载脚本内容
     */
    private String loadAssetScript(String fileName) {
        try {
            java.io.InputStream is = context.getAssets().open(fileName);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            is.close();
            return baos.toString("UTF-8");
        } catch (Exception e) {
            android.util.Log.e("UserScriptManager", "Failed to load asset script: " + fileName, e);
            return null;
        }
    }

    /**
     * 安装用户脚本
     */
    public synchronized boolean installScript(String scriptContent) {
        try {
            UserScript script = UserScriptParser.parse(scriptContent);
            if (script != null && isValidScript(script)) {
                userScripts.add(script);
                // 保存到存储
                scriptStorage.saveScripts(userScripts);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 验证脚本安全性
     */
    protected boolean isValidScript(UserScript script) {
        if (script == null || script.getContent() == null) {
            return false;
        }
        
        String content = script.getContent().toLowerCase();
        // 检查是否包含危险操作
        String[] dangerousPatterns = {
            "eval(",
            "innerhtml",
            "outerhtml",
            "document.write",
            "execscript",
            "javascript:",
            "data:text/html"
        };
        
        for (String pattern : dangerousPatterns) {
            if (content.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 卸载用户脚本
     */
    public synchronized boolean uninstallScript(String scriptId) {
        boolean removed = userScripts.removeIf(script -> script.getId().equals(scriptId));
        if (removed) {
            scriptStorage.saveScripts(userScripts);
        }
        return removed;
    }

    /**
     * 启用/禁用用户脚本
     */
    public synchronized void setScriptEnabled(String scriptId, boolean enabled) {
        for (UserScript script : userScripts) {
            if (script.getId().equals(scriptId)) {
                script.setEnabled(enabled);
                scriptStorage.saveScripts(userScripts);
                break;
            }
        }
    }

    /**
     * 获取所有用户脚本
     */
    public List<UserScript> getAllScripts() {
        return new ArrayList<>(userScripts);
    }

    /**
     * 根据URL获取适用的脚本
     */
    public List<UserScript> getMatchingScripts(String url) {
        List<UserScript> matchingScripts = new ArrayList<>();
        for (UserScript script : userScripts) {
            if (script.isEnabled() && script.matchesUrl(url)) {
                matchingScripts.add(script);
            }
        }
        return matchingScripts;
    }

    /**
     * 为WebView注入用户脚本（自动检测内核类型）
     */
    public void injectScripts(WebView webView, String url) {
        if (!isEnabled) return;

        // 使用内核检测器进行精确检测
        WebViewKernelDetector.KernelType kernelType = WebViewKernelDetector.detectKernelType(webView);

        Log.d("UserScriptManager", "Detected WebView kernel: " + WebViewKernelDetector.getKernelDescription(kernelType));

        try {
            switch (kernelType) {
                case X5_TENCENT:
                    // 确保X5组件已初始化
                    initializeX5Components();
                    // 使用X5专用脚本管理器
                    Log.d("UserScriptManager", "Using X5 script injection for X5 WebView");
                    if (x5ScriptManager != null) {
                        x5ScriptManager.injectScriptsForX5(webView, url);
                    } else {
                        Log.w("UserScriptManager", "X5 script manager not available, fallback to system WebView");
                        injectScriptsToSystemWebView(webView, url);
                    }
                    break;

                case CHROMIUM:
                    // 使用标准Chromium脚本注入
                    Log.d("UserScriptManager", "Using Chromium-compatible script injection");
                    injectScriptsToSystemWebView(webView, url);
                    break;

                case SYSTEM_WEBVIEW:
                default:
                    // 使用系统WebView脚本注入
                    Log.d("UserScriptManager", "Using system WebView script injection");
                    injectScriptsToSystemWebView(webView, url);
                    break;
            }
        } catch (Exception e) {
            Log.e("UserScriptManager", "Script injection failed, kernel: " + kernelType, e);
            // 发生错误时不影响基本浏览功能
        }
    }

    /**
     * 为系统WebView注入用户脚本
     */
    private void injectScriptsToSystemWebView(WebView webView, String url) {
        List<UserScript> matchingScripts = getMatchingScripts(url);
        for (UserScript script : matchingScripts) {
            injectScript(webView, script);
        }
    }

    /**
     * 注入单个脚本
     */
    private void injectScript(WebView webView, UserScript script) {
        try {
            if (!isValidScript(script)) {
                return;
            }
            
            // 注入GM API
            String gmApi = ScriptInjector.generateGMAPI(script);
            webView.evaluateJavascript(gmApi, result -> {
                if (result == null || result.contains("error")) {
                    // GM API注入失败，记录错误但继续执行
                    android.util.Log.w("UserScriptManager", "GM API injection failed for script: " + script.getId());
                }
            });

            // 注入脚本内容（添加重试机制）
            String injectedScript = ScriptInjector.wrapScript(script);
            webView.evaluateJavascript(injectedScript, result -> {
                if (result != null && result.contains("error")) {
                    android.util.Log.e("UserScriptManager", "Script injection failed: " + result);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("UserScriptManager", "Error injecting script: " + script.getId(), e);
        }
    }

    /**
     * 设置整体启用状态
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * 获取启用状态
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * 延迟初始化X5管理器
     */
    private void initializeX5Components() {
        if (x5Manager == null) {
            x5Manager = X5WebViewManager.getInstance();
        }
        if (x5ScriptManager == null && x5Manager.isX5Available()) {
            x5ScriptManager = X5UserScriptManager.getInstance(context);
        }
    }

    /**
     * 检查X5脚本注入是否支持
     */
    public boolean isX5ScriptInjectionSupported() {
        initializeX5Components();
        return x5ScriptManager != null && x5ScriptManager.isX5ScriptInjectionSupported();
    }

    /**
     * 获取X5脚本注入状态信息
     */
    public String getX5ScriptInjectionStatus() {
        initializeX5Components();
        if (x5ScriptManager != null) {
            return x5ScriptManager.getX5ScriptInjectionStatus();
        }
        return "X5脚本管理器未初始化";
    }

    /**
     * 获取WebView内核类型描述
     */
    public String getWebViewKernelDescription() {
        initializeX5Components();
        if (x5Manager != null && x5Manager.isX5Available() && x5Manager.isX5Initialized()) {
            return "腾讯X5内核 (版本: " + (x5Manager.getX5Version() != null ? x5Manager.getX5Version() : "未知") + ")";
        } else {
            return "系统WebView内核";
        }
    }

    /**
     * 获取推荐的脚本注入方式
     */
    public String getRecommendedInjectionMethod() {
        initializeX5Components();
        if (isX5ScriptInjectionSupported()) {
            return "推荐使用X5专用脚本注入器 (更好的兼容性)";
        } else if (x5Manager != null && x5Manager.isX5Initialized() && !x5Manager.isX5Available()) {
            return "X5初始化失败，使用系统WebView脚本注入";
        } else {
            return "使用系统WebView脚本注入 (X5未初始化)";
        }
    }
}
