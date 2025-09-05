package com.hippo.ehviewer.userscript;

import android.util.Log;

/**
 * X5 WebView专用脚本注入器
 * 针对腾讯X5内核优化脚本注入功能
 */
public class X5ScriptInjector {

    private static final String TAG = "X5ScriptInjector";

    /**
     * 生成X5兼容的GM API
     */
    public static String generateX5GMAPI(UserScript script) {
        StringBuilder sb = new StringBuilder();

        sb.append("(function() {");
        sb.append("try {");

        // 基本GM API - X5兼容版本
        sb.append("if (typeof GM_getValue === 'undefined') {");
        sb.append("window.GM_getValue = function(key, defaultValue) {");
        sb.append("try {");
        sb.append("var storageKey = 'X5_GM_' + '").append(script.getId()).append("' + '_' + key;");
        sb.append("var value = localStorage.getItem(storageKey);");
        sb.append("return value !== null ? JSON.parse(value) : defaultValue;");
        sb.append("} catch(e) {");
        sb.append("console.warn('[X5 GM_getValue] Error:', e);");
        sb.append("return defaultValue;");
        sb.append("}");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_setValue === 'undefined') {");
        sb.append("window.GM_setValue = function(key, value) {");
        sb.append("try {");
        sb.append("var storageKey = 'X5_GM_' + '").append(script.getId()).append("' + '_' + key;");
        sb.append("localStorage.setItem(storageKey, JSON.stringify(value));");
        sb.append("return true;");
        sb.append("} catch(e) {");
        sb.append("console.warn('[X5 GM_setValue] Error:', e);");
        sb.append("return false;");
        sb.append("}");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_deleteValue === 'undefined') {");
        sb.append("window.GM_deleteValue = function(key) {");
        sb.append("try {");
        sb.append("var storageKey = 'X5_GM_' + '").append(script.getId()).append("' + '_' + key;");
        sb.append("localStorage.removeItem(storageKey);");
        sb.append("return true;");
        sb.append("} catch(e) {");
        sb.append("console.warn('[X5 GM_deleteValue] Error:', e);");
        sb.append("return false;");
        sb.append("}");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_addStyle === 'undefined') {");
        sb.append("window.GM_addStyle = function(css) {");
        sb.append("try {");
        sb.append("var style = document.createElement('style');");
        sb.append("style.type = 'text/css';");
        sb.append("style.textContent = css;");
        sb.append("if (document.head) {");
        sb.append("document.head.appendChild(style);");
        sb.append("} else {");
        sb.append("document.addEventListener('DOMContentLoaded', function() {");
        sb.append("document.head.appendChild(style);");
        sb.append("});");
        sb.append("}");
        sb.append("return style;");
        sb.append("} catch(e) {");
        sb.append("console.warn('[X5 GM_addStyle] Error:', e);");
        sb.append("return null;");
        sb.append("}");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_log === 'undefined') {");
        sb.append("window.GM_log = function(message) {");
        sb.append("console.log('[X5 UserScript: ").append(escapeJavaScriptString(script.getName())).append("] ' + message);");
        sb.append("};");
        sb.append("}");

        // X5兼容的XMLHttpRequest实现
        sb.append("if (typeof GM_xmlhttpRequest === 'undefined') {");
        sb.append("window.GM_xmlhttpRequest = function(details) {");
        sb.append("try {");
        sb.append("var xhr = new XMLHttpRequest();");
        sb.append("xhr.open(details.method || 'GET', details.url, true);");

        sb.append("if (details.headers) {");
        sb.append("for (var header in details.headers) {");
        sb.append("if (details.headers.hasOwnProperty(header)) {");
        sb.append("xhr.setRequestHeader(header, details.headers[header]);");
        sb.append("}");
        sb.append("}");
        sb.append("}");

        sb.append("if (details.timeout) {");
        sb.append("xhr.timeout = details.timeout;");
        sb.append("}");

        sb.append("xhr.onload = function() {");
        sb.append("if (details.onload) {");
        sb.append("details.onload({");
        sb.append("responseText: xhr.responseText,");
        sb.append("responseXML: xhr.responseXML,");
        sb.append("readyState: xhr.readyState,");
        sb.append("responseHeaders: xhr.getAllResponseHeaders(),");
        sb.append("status: xhr.status,");
        sb.append("statusText: xhr.statusText,");
        sb.append("finalUrl: xhr.responseURL || details.url");
        sb.append("});");
        sb.append("}");
        sb.append("};");

        sb.append("xhr.onerror = function() {");
        sb.append("if (details.onerror) {");
        sb.append("details.onerror({");
        sb.append("error: 'Network Error',");
        sb.append("status: xhr.status,");
        sb.append("statusText: xhr.statusText");
        sb.append("});");
        sb.append("}");
        sb.append("};");

        sb.append("xhr.ontimeout = function() {");
        sb.append("if (details.ontimeout) {");
        sb.append("details.ontimeout({");
        sb.append("error: 'Timeout',");
        sb.append("status: xhr.status,");
        sb.append("statusText: xhr.statusText");
        sb.append("});");
        sb.append("}");
        sb.append("};");

        sb.append("xhr.send(details.data);");
        sb.append("return xhr;");
        sb.append("} catch(e) {");
        sb.append("console.error('[X5 GM_xmlhttpRequest] Error:', e);");
        sb.append("if (details.onerror) {");
        sb.append("details.onerror({error: e.message});");
        sb.append("}");
        sb.append("return null;");
        sb.append("}");
        sb.append("};");
        sb.append("}");

        // X5特定的兼容性函数
        sb.append("if (typeof GM_getResourceText === 'undefined') {");
        sb.append("window.GM_getResourceText = function(resourceName) {");
        sb.append("console.warn('[X5] GM_getResourceText not implemented for resource:', resourceName);");
        sb.append("return null;");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_getResourceURL === 'undefined') {");
        sb.append("window.GM_getResourceURL = function(resourceName) {");
        sb.append("console.warn('[X5] GM_getResourceURL not implemented for resource:', resourceName);");
        sb.append("return null;");
        sb.append("};");
        sb.append("}");

        sb.append("console.log('[X5 UserScript] GM API initialized for script: ").append(escapeJavaScriptString(script.getName())).append("');");

        sb.append("} catch(e) {");
        sb.append("console.error('[X5 UserScript] Failed to initialize GM API:', e);");
        sb.append("}");
        sb.append("})();");

        return sb.toString();
    }

    /**
     * 包装X5兼容的脚本内容
     */
    public static String wrapX5Script(UserScript script) {
        StringBuilder sb = new StringBuilder();

        sb.append("(function() {");
        sb.append("try {");

        // X5脚本环境检测
        sb.append("if (typeof window.X5ScriptEnvironment === 'undefined') {");
        sb.append("window.X5ScriptEnvironment = {");
        sb.append("version: '1.0',");
        sb.append("kernel: 'X5',");
        sb.append("scripts: []");
        sb.append("};");
        sb.append("}");

        // 脚本信息
        sb.append("var X5_SCRIPT_INFO = {");
        sb.append("id: '").append(escapeJavaScriptString(script.getId())).append("',");
        sb.append("name: '").append(escapeJavaScriptString(script.getName())).append("',");
        sb.append("version: '").append(escapeJavaScriptString(script.getVersion())).append("',");
        sb.append("author: '").append(escapeJavaScriptString(script.getAuthor())).append("',");
        sb.append("description: '").append(escapeJavaScriptString(script.getDescription() != null ? script.getDescription() : "")).append("',");
        sb.append("kernel: 'X5'");
        sb.append("};");

        // 注册脚本到X5环境
        sb.append("window.X5ScriptEnvironment.scripts.push(X5_SCRIPT_INFO);");

        // X5兼容性检查
        sb.append("if (typeof console === 'undefined') {");
        sb.append("window.console = {");
        sb.append("log: function() {},");
        sb.append("warn: function() {},");
        sb.append("error: function() {}");
        sb.append("};");
        sb.append("}");

        // 注入脚本内容
        sb.append(script.getContent());

        // 脚本执行成功标记
        sb.append("console.log('[X5 UserScript] Script loaded successfully: ").append(escapeJavaScriptString(script.getName())).append("');");

        sb.append("} catch(e) {");
        sb.append("console.error('[X5 UserScript Error: ").append(escapeJavaScriptString(script.getName())).append("] ' + e.message);");
        sb.append("console.error('[X5 UserScript] Stack:', e.stack);");

        // 错误恢复机制
        sb.append("if (typeof window.X5ScriptErrorHandler === 'function') {");
        sb.append("window.X5ScriptErrorHandler(e, X5_SCRIPT_INFO);");
        sb.append("}");
        sb.append("}");
        sb.append("})();");

        return sb.toString();
    }

    /**
     * 生成X5脚本加载器
     */
    public static String generateX5ScriptLoader() {
        return "(function() {" +
               "if (typeof window.X5ScriptLoader === 'undefined') {" +
               "window.X5ScriptLoader = {" +
               "loadedScripts: []," +
               "loadScript: function(id, content) {" +
               "if (this.loadedScripts.indexOf(id) === -1) {" +
               "this.loadedScripts.push(id);" +
               "try {" +
               "var script = document.createElement('script');" +
               "script.type = 'text/javascript';" +
               "script.textContent = content;" +
               "script.setAttribute('data-x5-script-id', id);" +
               "(document.head || document.documentElement).appendChild(script);" +
               "console.log('[X5] Script loaded via DOM injection:', id);" +
               "return true;" +
               "} catch(e) {" +
               "console.error('[X5] Failed to load script via DOM:', id, e);" +
               "return false;" +
               "}" +
               "}" +
               "return true;" +
               "}," +
               "unloadScript: function(id) {" +
               "var scripts = document.querySelectorAll('script[data-x5-script-id=\"' + id + '\"]');" +
               "for (var i = 0; i < scripts.length; i++) {" +
               "scripts[i].parentNode.removeChild(scripts[i]);" +
               "}" +
               "var index = this.loadedScripts.indexOf(id);" +
               "if (index > -1) {" +
               "this.loadedScripts.splice(index, 1);" +
               "}" +
               "console.log('[X5] Script unloaded:', id);" +
               "}" +
               "};" +
               "}" +
               "console.log('[X5] Script loader initialized');" +
               "})();";
    }

    /**
     * JavaScript字符串转义
     */
    private static String escapeJavaScriptString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f");
    }
}
