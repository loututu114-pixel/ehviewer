package com.hippo.ehviewer.ui.browser;

// 移除Android依赖，使用纯Java实现

import java.util.regex.Pattern;

public class InputValidator {
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\da-z\\.-]+\\.([a-z\\.]{2,6})|localhost|([\\da-z\\.-]+))(:\\d+)?([/\\w \\.-]*)*/?$",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+" +
        "[a-zA-Z]{2,}$"
    );
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    public boolean isValidUrl(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String trimmed = input.trim();
        
        // 检查是否已经是完整URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return URL_PATTERN.matcher(trimmed).matches();
        }
        
        // 检查是否是域名格式
        if (DOMAIN_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        
        // 检查是否是IP地址
        if (IP_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        
        // 检查是否是localhost或IP地址（带端口）
        if (trimmed.startsWith("localhost") || trimmed.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+(:\\d+)?$")) {
            return true;
        }
        
        // 检查是否包含点号且不包含空格（简单域名检测）
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            // 更宽松的域名检测，支持端口
            return trimmed.matches("^[a-zA-Z0-9][a-zA-Z0-9\\.-]*[a-zA-Z0-9]\\.[a-zA-Z]{2,}(:\\d+)?$");
        }
        
        return false;
    }
    
    public String normalizeUrl(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String trimmed = input.trim();
        
        // 如果已经有协议，直接返回
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        
        // 对于其他情况，添加https://前缀
        return "https://" + trimmed;
    }
    
    public boolean isSearchQuery(String input) {
        return !isValidUrl(input);
    }
}