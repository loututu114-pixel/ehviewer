package com.hippo.ehviewer.util;

import android.content.Context;
import android.util.Log;
import java.util.regex.Pattern;

/**
 * 智能URL处理器
 * 自动检测输入内容并决定是直接访问还是搜索
 */
public class SmartUrlProcessor {
    private static final String TAG = "SmartUrlProcessor";

    // URL模式正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?" +                                    // 可选的http://或https://
        "([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+" + // 域名部分
        "[a-zA-Z]{2,}" +                                     // TLD
        "(:\\d{1,5})?" +                                     // 可选端口
        "(/.*)?$",                                           // 可选路径
        Pattern.CASE_INSENSITIVE
    );

    // IP地址模式
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(https?://)?" +
        "((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}" +
        "(:\\d{1,5})?" +
        "(/.*)?$"
    );

    // 本地地址模式
    private static final Pattern LOCAL_PATTERN = Pattern.compile(
        "^(https?://)?" +
        "(localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0)" +
        "(:\\d{1,5})?" +
        "(/.*)?$",
        Pattern.CASE_INSENSITIVE
    );

    // 文件路径模式
    private static final Pattern FILE_PATTERN = Pattern.compile(
        "^file://.*",
        Pattern.CASE_INSENSITIVE
    );

    // 特殊协议模式
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile(
        "^(mailto|tel|sms|geo|market|intent):.*",
        Pattern.CASE_INSENSITIVE
    );

    private final Context context;

    public SmartUrlProcessor(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 处理用户输入的URL或搜索关键词
     */
    public String processInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return getDefaultHomePage();
        }

        String trimmedInput = input.trim();

        // 检查是否是有效的URL
        if (isValidUrl(trimmedInput)) {
            return normalizeUrl(trimmedInput);
        }

        // 检查是否是特殊协议
        if (isSpecialProtocol(trimmedInput)) {
            return trimmedInput;
        }

        // 检查是否是文件路径
        if (isFilePath(trimmedInput)) {
            return trimmedInput;
        }

        // 如果都不是，则进行搜索
        return performSearch(trimmedInput);
    }

    /**
     * 检查是否是有效的URL
     */
    public boolean isValidUrl(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // 检查是否包含协议
        boolean hasProtocol = input.toLowerCase().startsWith("http://") ||
                             input.toLowerCase().startsWith("https://");

        // 如果有协议，检查完整URL格式
        if (hasProtocol) {
            return URL_PATTERN.matcher(input).matches() ||
                   IP_PATTERN.matcher(input).matches() ||
                   LOCAL_PATTERN.matcher(input).matches();
        }

        // 如果没有协议，检查域名格式
        return URL_PATTERN.matcher("http://" + input).matches() ||
               IP_PATTERN.matcher("http://" + input).matches() ||
               LOCAL_PATTERN.matcher("http://" + input).matches();
    }

    /**
     * 检查是否是特殊协议
     */
    public boolean isSpecialProtocol(String input) {
        return PROTOCOL_PATTERN.matcher(input).matches();
    }

    /**
     * 检查是否是文件路径
     */
    public boolean isFilePath(String input) {
        return FILE_PATTERN.matcher(input).matches();
    }

    /**
     * 规范化URL
     */
    public String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        String lowerUrl = url.toLowerCase();

        // 如果没有协议，添加https
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            // 特殊处理localhost和IP地址
            if (lowerUrl.startsWith("localhost") ||
                lowerUrl.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
                url = "http://" + url;
            } else {
                url = "https://" + url;
            }
        }

        return url;
    }

    /**
     * 执行搜索
     */
    public String performSearch(String query) {
        // 检测用户地区选择搜索引擎
        boolean isChineseUser = isChineseUser();

        if (isChineseUser) {
            // 国内用户使用百度
            return "https://www.baidu.com/s?wd=" + encodeUrl(query);
        } else {
            // 国外用户使用Google
            return "https://www.google.com/search?q=" + encodeUrl(query);
        }
    }

    /**
     * 检测是否为中国用户
     */
    private boolean isChineseUser() {
        try {
            // 可以通过多种方式检测：
            // 1. 系统语言设置
            String language = context.getResources().getConfiguration().locale.getLanguage();
            if ("zh".equals(language)) {
                return true;
            }

            // 2. 国家代码
            String country = context.getResources().getConfiguration().locale.getCountry();
            if ("CN".equals(country) || "HK".equals(country) || "TW".equals(country)) {
                return true;
            }

            // 3. 时区检测
            java.util.TimeZone tz = java.util.TimeZone.getDefault();
            String tzId = tz.getID();
            if (tzId.contains("Asia/Shanghai") || tzId.contains("Asia/Hong_Kong") ||
                tzId.contains("Asia/Taipei")) {
                return true;
            }

            // 4. 可以添加网络检测（检查是否能访问Google）

        } catch (Exception e) {
            Log.e(TAG, "Error detecting user region", e);
        }

        // 默认返回false（使用Google）
        return false;
    }

    /**
     * 获取默认主页
     */
    public String getDefaultHomePage() {
        boolean isChineseUser = isChineseUser();

        if (isChineseUser) {
            return "https://www.baidu.com";
        } else {
            return "https://www.google.com";
        }
    }

    /**
     * URL编码
     */
    private String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Error encoding URL", e);
            return url;
        }
    }

    /**
     * 检查输入是否看起来像搜索关键词
     */
    public boolean looksLikeSearchQuery(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // 包含空格或特殊搜索符号
        if (input.contains(" ") || input.contains("+") || input.contains("-") ||
            input.contains("\"") || input.contains("OR") || input.contains("AND")) {
            return true;
        }

        // 不包含点号且长度超过3个字符
        if (!input.contains(".") && input.length() > 3) {
            return true;
        }

        // 纯数字（可能是搜索编号）
        if (input.matches("\\d+")) {
            return true;
        }

        // 中文字符
        if (input.matches(".*[\\u4e00-\\u9fa5].*")) {
            return true;
        }

        return false;
    }

    /**
     * 获取输入类型的描述
     */
    public String getInputTypeDescription(String input) {
        if (isValidUrl(input)) {
            return "有效URL";
        } else if (isSpecialProtocol(input)) {
            return "特殊协议";
        } else if (isFilePath(input)) {
            return "文件路径";
        } else if (looksLikeSearchQuery(input)) {
            return "搜索关键词";
        } else {
            return "未知类型";
        }
    }
}
