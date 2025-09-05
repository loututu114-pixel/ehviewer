package com.hippo.ehviewer.userscript;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

/**
 * 用户脚本解析器 - 解析Tampermonkey用户脚本的头部信息
 */
public class UserScriptParser {

    private static final Pattern HEADER_PATTERN = Pattern.compile(
        "// ==UserScript==([\\s\\S]*?)// ==/UserScript==", Pattern.DOTALL);

    private static final Pattern META_PATTERN = Pattern.compile(
        "// @(\\w+)\\s+(.+)", Pattern.MULTILINE);

    /**
     * 解析用户脚本
     */
    public static UserScript parse(String scriptContent) {
        if (scriptContent == null || scriptContent.isEmpty()) {
            return null;
        }

        UserScript script = new UserScript();
        Matcher headerMatcher = HEADER_PATTERN.matcher(scriptContent);

        if (headerMatcher.find()) {
            String header = headerMatcher.group(1);
            parseHeader(script, header);
            script.setContent(extractScriptContent(scriptContent, headerMatcher.end()));
        } else {
            // 如果没有头部信息，创建一个基本的脚本
            script.setName("未命名脚本");
            script.setContent(scriptContent);
        }

        // 生成唯一ID
        if (script.getId() == null) {
            script.setId(generateScriptId(script));
        }

        return script;
    }

    /**
     * 解析脚本头部
     */
    private static void parseHeader(UserScript script, String header) {
        Matcher metaMatcher = META_PATTERN.matcher(header);

        while (metaMatcher.find()) {
            String key = metaMatcher.group(1);
            String value = metaMatcher.group(2).trim();

            switch (key.toLowerCase()) {
                case "name":
                    script.setName(value);
                    break;
                case "version":
                    script.setVersion(value);
                    break;
                case "author":
                    script.setAuthor(value);
                    break;
                case "description":
                    script.setDescription(value);
                    break;
                case "include":
                    script.getIncludePatterns().add(value);
                    break;
                case "exclude":
                    script.getExcludePatterns().add(value);
                    break;
                case "match":
                    script.getIncludePatterns().add(value);
                    break;
                case "updateurl":
                    script.setUpdateUrl(value);
                    break;
                case "id":
                    script.setId(value);
                    break;
            }
        }
    }

    /**
     * 提取脚本内容（去掉头部注释）
     */
    private static String extractScriptContent(String fullContent, int headerEnd) {
        if (headerEnd < fullContent.length()) {
            return fullContent.substring(headerEnd).trim();
        }
        return "";
    }

    /**
     * 生成脚本ID
     */
    private static String generateScriptId(UserScript script) {
        String base = script.getName() != null ? script.getName() : "unknown";
        return base.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase() + "_" +
               System.currentTimeMillis();
    }

    /**
     * 验证脚本格式
     */
    public static boolean isValidScript(String scriptContent) {
        return scriptContent != null && !scriptContent.trim().isEmpty() &&
               HEADER_PATTERN.matcher(scriptContent).find();
    }

    /**
     * 获取脚本的基本信息（不解析完整内容）
     */
    public static ScriptInfo getScriptInfo(String scriptContent) {
        ScriptInfo info = new ScriptInfo();
        Matcher headerMatcher = HEADER_PATTERN.matcher(scriptContent);

        if (headerMatcher.find()) {
            String header = headerMatcher.group(1);
            Matcher metaMatcher = META_PATTERN.matcher(header);

            while (metaMatcher.find()) {
                String key = metaMatcher.group(1);
                String value = metaMatcher.group(2).trim();

                switch (key.toLowerCase()) {
                    case "name":
                        info.name = value;
                        break;
                    case "author":
                        info.author = value;
                        break;
                    case "description":
                        info.description = value;
                        break;
                    case "version":
                        info.version = value;
                        break;
                }
            }
        }

        return info;
    }

    /**
     * 脚本信息类
     */
    public static class ScriptInfo {
        public String name = "未知脚本";
        public String author = "未知作者";
        public String description = "";
        public String version = "1.0.0";
    }
}
