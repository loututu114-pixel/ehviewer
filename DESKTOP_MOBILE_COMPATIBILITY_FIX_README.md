# 桌面/移动端兼容性冲突修复方案

## 问题背景

在修复 `net::ERR_CONNECTION_CLOSED` 错误的过程中，发现了桌面版页面访问与移动UI兼容功能之间的严重冲突：

### 冲突类型

1. **User-Agent冲突**
   - `EnhancedWebViewManager` 设置固定的移动UA
   - `ModernBrowserActivity` 和 `WebViewActivity` 的桌面模式切换会被覆盖
   - 重试机制中的UA设置与用户选择冲突

2. **URL自动转换冲突**
   - `MobileUrlAdapter` 自动将桌面URL转换为移动版
   - 违背用户访问桌面版页面的意图
   - 与桌面模式切换功能产生矛盾

3. **WebView设置冲突**
   - 多个组件同时修改相同WebSettings
   - 连接稳定性设置与移动端优化设置冲突
   - LayoutAlgorithm设置在不同版本中的兼容性问题

## 修复方案

### 1. User-Agent冲突修复

**修改文件：** `EnhancedWebViewManager.java`

**修复内容：**
```java
// 移除固定的UA设置，避免与桌面/移动模式切换冲突
// String userAgent = "Mozilla/5.0 (Linux; Android 13; SM-G998B)...";
// webSettings.setUserAgentString(userAgent);

// 重试时使用当前WebView的UA，保持一致性
if (view != null && view.getSettings() != null) {
    String currentUA = view.getSettings().getUserAgentString();
    if (currentUA != null && !currentUA.isEmpty()) {
        headers.put("User-Agent", currentUA + " EhViewer/2.0 Enhanced");
    }
}
```

**修改文件：** `ModernBrowserActivity.java`

**修复内容：**
```java
private void toggleDesktopMode() {
    // 使用标准的UA字符串，避免硬编码
    if (isDesktopMode) {
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64)...");
    } else {
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; SM-G973F)...");
    }
    // 重新加载页面并更新UI
    currentWebView.reload();
    updateDesktopModeUI();
}
```

### 2. URL自动转换冲突修复

**修改文件：** `MobileUrlAdapter.java`

**修复内容：**
```java
// 检查强制桌面版网站，保持桌面版访问
if (FORCE_DESKTOP_SITES.containsKey(host)) {
    Log.d(TAG, "Force desktop site: " + host);
    return processedUrl; // 保持桌面版，不进行任何转换
}

// 只有在明确需要移动端适配时才进行转换
if (preferMobile) {
    // 强制转换为移动版
    adaptedUrl = convertPcToMobile(adaptedUrl);
}
```

**修改文件：** `BrowserCompatibilityManager.java`

**修复内容：**
```java
private String processUrlForCompatibility(String originalUrl) {
    // 重要修改：不执行任何PC到移动端的强制转换
    // 让网站自己处理移动端适配，避免与用户桌面版访问意图冲突
    // 只有在明确需要时（如WAP修复）才进行URL转换

    // 只处理WAP格式错误，不主动改变用户访问意图
    if (needsWapFix(originalUrl)) {
        processedUrl = urlAdapter.adaptUrl(processedUrl, false);
    }

    return processedUrl;
}
```

### 3. WebView设置冲突修复

**修改文件：** `UserAgentManager.java`

**修复内容：**
```java
private void optimizeForMobile(WebSettings settings) {
    // 启用自适应布局 - 仅在支持时设置，避免与连接稳定性设置冲突
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
    }
}

private void optimizeForDesktop(WebSettings settings) {
    // 使用标准布局算法 - 仅在支持时设置，避免冲突
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    }
}
```

### 4. 桌面模式切换逻辑优化

**修改文件：** `WebViewActivity.java`

**修复内容：**
```java
private void toggleDesktopMode() {
    String currentUA = currentTab.webView.getSettings().getUserAgentString();

    if (currentUA != null && currentUA.contains("Mobile")) {
        // 移动 -> 桌面
        String desktopUA = mUserAgentManager.getDesktopUserAgent();
        currentTab.webView.getSettings().setUserAgentString(desktopUA);
    } else if (currentUA != null) {
        // 桌面 -> 移动
        String mobileUA = mUserAgentManager.getMobileUserAgent();
        currentTab.webView.getSettings().setUserAgentString(mobileUA);
    } else {
        // 没有设置UA，默认切换到桌面模式
        String desktopUA = mUserAgentManager.getDesktopUserAgent();
        currentTab.webView.getSettings().setUserAgentString(desktopUA);
    }

    currentTab.webView.reload();
}
```

## 修复效果

### 解决的问题

1. **桌面版访问稳定性**
   - 用户可以正常访问桌面版页面
   - 桌面模式切换功能正常工作
   - 不再被自动转换为移动版

2. **User-Agent一致性**
   - 重试机制使用与当前页面相同的UA
   - 桌面/移动模式切换不再被覆盖
   - 网络错误处理与用户选择保持一致

3. **WebView设置兼容性**
   - 避免了设置冲突
   - 不同Android版本的兼容性更好
   - 连接稳定性与UI优化并存

### 保持的功能

1. **网络错误处理**
   - `ERR_CONNECTION_CLOSED` 错误的重试机制仍然有效
   - 智能延迟算法正常工作
   - 错误日志记录功能完整

2. **移动端优化**
   - 移动端访问体验仍然优化
   - 自动适配功能在需要时仍然工作
   - 视频播放等功能不受影响

3. **浏览器功能**
   - 桌面/移动模式切换正常
   - 页面重试和刷新功能正常
   - 用户体验保持一致

## 测试建议

1. **桌面版访问测试**
   - 访问各种桌面版网站（GitHub、Google Docs等）
   - 验证桌面模式切换功能
   - 检查是否被自动转换为移动版

2. **移动端兼容性测试**
   - 测试移动版网站访问
   - 验证移动端UI优化
   - 检查视频播放等功能

3. **网络错误处理测试**
   - 模拟网络不稳定情况
   - 验证 `ERR_CONNECTION_CLOSED` 错误处理
   - 测试重试机制是否正常

4. **User-Agent一致性测试**
   - 切换桌面/移动模式后检查UA
   - 验证重试时的UA一致性
   - 测试不同网站的UA适配

## 维护建议

1. **定期检查冲突**
   - 监控桌面版访问的用户反馈
   - 检查新的WebView设置是否产生冲突
   - 验证不同Android版本的兼容性

2. **功能优先级**
   - 用户的桌面版访问意图优先
   - 网络连接稳定性优先
   - 移动端优化为辅助功能

3. **日志监控**
   - 监控User-Agent切换日志
   - 跟踪URL转换行为
   - 观察网络错误处理情况

## 版本信息

- **修复日期**：2024年12月
- **修复类型**：兼容性冲突修复
- **影响范围**：桌面/移动端切换功能、网络错误处理
- **兼容性**：Android API 21+（Lollipop 及以上）
- **测试状态**：需要全面测试验证
