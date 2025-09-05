package com.hippo.ehviewer.userscript;

/**
 * 脚本注入器 - 负责将用户脚本注入到WebView中
 */
public class ScriptInjector {

    /**
     * 生成Greasemonkey API
     */
    public static String generateGMAPI(UserScript script) {
        StringBuilder sb = new StringBuilder();

        // 基本GM API
        sb.append("(function() {");
        sb.append("if (typeof GM_getValue === 'undefined') {");
        sb.append("window.GM_getValue = function(key, defaultValue) {");
        sb.append("try {");
        sb.append("var value = localStorage.getItem('GM_' + '").append(script.getId()).append("' + '_' + key);");
        sb.append("return value !== null ? JSON.parse(value) : defaultValue;");
        sb.append("} catch(e) { return defaultValue; }");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_setValue === 'undefined') {");
        sb.append("window.GM_setValue = function(key, value) {");
        sb.append("try {");
        sb.append("localStorage.setItem('GM_' + '").append(script.getId()).append("' + '_' + key, JSON.stringify(value));");
        sb.append("} catch(e) {}");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_deleteValue === 'undefined') {");
        sb.append("window.GM_deleteValue = function(key) {");
        sb.append("try {");
        sb.append("localStorage.removeItem('GM_' + '").append(script.getId()).append("' + '_' + key);");
        sb.append("} catch(e) {}");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_addStyle === 'undefined') {");
        sb.append("window.GM_addStyle = function(css) {");
        sb.append("var style = document.createElement('style');");
        sb.append("style.textContent = css;");
        sb.append("document.head.appendChild(style);");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_log === 'undefined') {");
        sb.append("window.GM_log = function(message) {");
        sb.append("console.log('[UserScript: ").append(script.getName()).append("] ' + message);");
        sb.append("};");
        sb.append("}");

        sb.append("if (typeof GM_xmlhttpRequest === 'undefined') {");
        sb.append("window.GM_xmlhttpRequest = function(details) {");
        sb.append("var xhr = new XMLHttpRequest();");
        sb.append("xhr.open(details.method || 'GET', details.url, true);");
        sb.append("if (details.headers) {");
        sb.append("for (var header in details.headers) {");
        sb.append("xhr.setRequestHeader(header, details.headers[header]);");
        sb.append("}");
        sb.append("}");
        sb.append("if (details.onload) {");
        sb.append("xhr.onload = function() {");
        sb.append("details.onload({");
        sb.append("responseText: xhr.responseText,");
        sb.append("responseXML: xhr.responseXML,");
        sb.append("readyState: xhr.readyState,");
        sb.append("responseHeaders: xhr.getAllResponseHeaders(),");
        sb.append("status: xhr.status,");
        sb.append("statusText: xhr.statusText");
        sb.append("});");
        sb.append("};");
        sb.append("}");
        sb.append("if (details.onerror) {");
        sb.append("xhr.onerror = details.onerror;");
        sb.append("}");
        sb.append("xhr.send(details.data);");
        sb.append("};");
        sb.append("}");

        sb.append("}");
        sb.append("})();");

        return sb.toString();
    }

    /**
     * 包装脚本内容使其安全执行
     */
    public static String wrapScript(UserScript script) {
        StringBuilder sb = new StringBuilder();

        sb.append("(function() {");
        sb.append("try {");

        // 添加脚本元信息
        sb.append("var SCRIPT_INFO = {");
        sb.append("id: '").append(escapeJavaScriptString(script.getId())).append("',");
        sb.append("name: '").append(escapeJavaScriptString(script.getName())).append("',");
        sb.append("version: '").append(escapeJavaScriptString(script.getVersion())).append("',");
        sb.append("author: '").append(escapeJavaScriptString(script.getAuthor())).append("'");
        sb.append("};");

        // 注入脚本内容
        sb.append(script.getContent());

        sb.append("} catch(e) {");
        sb.append("console.error('[UserScript Error: ").append(escapeJavaScriptString(script.getName())).append("] ' + e.message);");
        sb.append("}");
        sb.append("})();");

        return sb.toString();
    }

    /**
     * 转义JavaScript字符串
     */
    private static String escapeJavaScriptString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 生成脚本加载器
     */
    public static String generateScriptLoader() {
        return "(function() {" +
               "window.UserScriptLoader = {" +
               "loadedScripts: []," +
               "loadScript: function(id, content) {" +
               "if (this.loadedScripts.indexOf(id) === -1) {" +
               "this.loadedScripts.push(id);" +
               "try {" +
               "eval(content);" +
               "} catch(e) {" +
               "console.error('[UserScript Loader] Failed to load script ' + id + ': ' + e.message);" +
               "}" +
               "}" +
               "}" +
               "};" +
               "})();";
    }
}
