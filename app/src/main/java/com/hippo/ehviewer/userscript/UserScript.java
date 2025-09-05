package com.hippo.ehviewer.userscript;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * 用户脚本类 - 表示一个Tampermonkey用户脚本
 */
public class UserScript {

    private String id;
    private String name;
    private String version;
    private String author;
    private String description;
    private String content;
    private List<String> includePatterns;
    private List<String> excludePatterns;
    private boolean enabled;
    private String updateUrl;
    private long lastUpdateTime;

    public UserScript() {
        this.includePatterns = new ArrayList<>();
        this.excludePatterns = new ArrayList<>();
        this.enabled = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getIncludePatterns() { return includePatterns; }
    public void setIncludePatterns(List<String> includePatterns) { this.includePatterns = includePatterns; }

    public List<String> getExcludePatterns() { return excludePatterns; }
    public void setExcludePatterns(List<String> excludePatterns) { this.excludePatterns = excludePatterns; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getUpdateUrl() { return updateUrl; }
    public void setUpdateUrl(String updateUrl) { this.updateUrl = updateUrl; }

    public long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    /**
     * 检查URL是否匹配脚本的运行条件
     */
    public boolean matchesUrl(String url) {
        if (url == null || url.isEmpty()) return false;

        // 检查排除模式
        for (String pattern : excludePatterns) {
            if (matchesPattern(url, pattern)) {
                return false;
            }
        }

        // 检查包含模式
        if (includePatterns.isEmpty()) {
            return true; // 如果没有包含模式，则对所有URL生效
        }

        for (String pattern : includePatterns) {
            if (matchesPattern(url, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 匹配URL模式
     */
    private boolean matchesPattern(String url, String pattern) {
        try {
            // 转换为正则表达式
            String regex = pattern.replace("*", ".*").replace("?", ".");
            Pattern p = Pattern.compile(regex);
            return p.matcher(url).matches();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取脚本摘要信息
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("名称: ").append(name != null ? name : "未知").append("\n");
        sb.append("版本: ").append(version != null ? version : "未知").append("\n");
        sb.append("作者: ").append(author != null ? author : "未知").append("\n");
        sb.append("状态: ").append(enabled ? "已启用" : "已禁用").append("\n");
        if (description != null && !description.isEmpty()) {
            sb.append("描述: ").append(description);
        }
        return sb.toString();
    }
}
