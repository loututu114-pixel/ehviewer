package com.hippo.ehviewer.util;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.WebViewClient;
import android.util.Log;

/**
 * 增强的错误页面生成器
 * 提供美化的错误页面和智能错误处理
 */
public class EnhancedErrorPageGenerator {
    private static final String TAG = "ErrorPageGenerator";
    
    private final Context context;
    private static final String ERROR_PAGE_TEMPLATE = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>访问出错</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #333;
            line-height: 1.6;
        }
        
        .error-container {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-radius: 20px;
            padding: 40px 30px;
            max-width: 480px;
            width: 90%%;
            text-align: center;
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
            animation: slideUp 0.6s ease-out;
        }
        
        @keyframes slideUp {
            from {
                opacity: 0;
                transform: translateY(30px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        
        .error-icon {
            font-size: 64px;
            margin-bottom: 20px;
            animation: bounce 2s infinite;
        }
        
        @keyframes bounce {
            0%%, 20%%, 50%%, 80%%, 100%% {
                transform: translateY(0);
            }
            40%% {
                transform: translateY(-10px);
            }
            60%% {
                transform: translateY(-5px);
            }
        }
        
        .error-title {
            font-size: 24px;
            font-weight: 600;
            color: #2d3748;
            margin-bottom: 15px;
        }
        
        .error-description {
            font-size: 16px;
            color: #718096;
            margin-bottom: 30px;
            line-height: 1.5;
        }
        
        .error-details {
            background: #f7fafc;
            border: 1px solid #e2e8f0;
            border-radius: 10px;
            padding: 15px;
            margin: 20px 0;
            text-align: left;
            font-size: 14px;
            color: #4a5568;
        }
        
        .error-details strong {
            color: #2d3748;
        }
        
        .action-buttons {
            display: flex;
            gap: 15px;
            justify-content: center;
            flex-wrap: wrap;
            margin-top: 30px;
        }
        
        .btn {
            padding: 12px 24px;
            border: none;
            border-radius: 25px;
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.3s ease;
            text-decoration: none;
            display: inline-block;
            min-width: 120px;
        }
        
        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            color: white;
        }
        
        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
        }
        
        .btn-secondary {
            background: #f7fafc;
            color: #4a5568;
            border: 1px solid #e2e8f0;
        }
        
        .btn-secondary:hover {
            background: #edf2f7;
            transform: translateY(-1px);
        }
        
        .suggestions {
            text-align: left;
            margin-top: 25px;
            padding: 20px;
            background: #f0fff4;
            border: 1px solid #9ae6b4;
            border-radius: 10px;
        }
        
        .suggestions h4 {
            color: #22543d;
            margin-bottom: 10px;
            font-size: 16px;
        }
        
        .suggestions ul {
            list-style: none;
            padding-left: 0;
        }
        
        .suggestions li {
            padding: 5px 0;
            color: #2f855a;
            position: relative;
            padding-left: 20px;
        }
        
        .suggestions li:before {
            content: "✓";
            position: absolute;
            left: 0;
            color: #38a169;
            font-weight: bold;
        }
        
        .network-info {
            margin-top: 20px;
            padding: 15px;
            background: #ebf8ff;
            border: 1px solid #90cdf4;
            border-radius: 10px;
            font-size: 14px;
            color: #2c5282;
        }
        
        @media (max-width: 480px) {
            .error-container {
                padding: 30px 20px;
                margin: 20px;
            }
            
            .action-buttons {
                flex-direction: column;
                align-items: center;
            }
            
            .btn {
                width: 100%%;
                max-width: 200px;
            }
        }
    </style>
</head>
<body>
    <div class="error-container">
        <div class="error-icon">%s</div>
        <h1 class="error-title">%s</h1>
        <p class="error-description">%s</p>
        
        <div class="error-details">
            <strong>错误代码:</strong> %d<br>
            <strong>访问地址:</strong> %s<br>
            <strong>时间:</strong> %s
        </div>
        
        <div class="action-buttons">
            <button class="btn btn-primary" onclick="window.location.reload();">🔄 重新加载</button>
            <button class="btn btn-secondary" onclick="history.back();">← 返回上页</button>
        </div>
        
        %s
        
        %s
        
        <script>
            // 自动重试机制
            let retryCount = 0;
            const maxRetries = 3;
            const retryDelay = 5000; // 5秒
            
            function autoRetry() {
                if (retryCount < maxRetries) {
                    retryCount++;
                    console.log('自动重试第 ' + retryCount + ' 次...');
                    setTimeout(() => {
                        window.location.reload();
                    }, retryDelay);
                }
            }
            
            // 网络状态检测
            function checkNetworkStatus() {
                if (navigator.onLine) {
                    document.getElementById('network-status').innerHTML = 
                        '<span style="color: #38a169;">✓ 网络连接正常</span>';
                } else {
                    document.getElementById('network-status').innerHTML = 
                        '<span style="color: #e53e3e;">✗ 网络连接异常</span>';
                }
            }
            
            // 页面加载完成后检查网络
            document.addEventListener('DOMContentLoaded', function() {
                checkNetworkStatus();
                
                // 监听网络状态变化
                window.addEventListener('online', checkNetworkStatus);
                window.addEventListener('offline', checkNetworkStatus);
                
                // 特定错误的自动重试
                if (window.shouldAutoRetry === true) {
                    setTimeout(autoRetry, 3000);
                }
            });
        </script>
    </div>
</body>
</html>
""";

    public EnhancedErrorPageGenerator(Context context) {
        this.context = context;
    }

    /**
     * 生成美化的错误页面
     */
    public String generateErrorPage(int errorCode, String description, String failingUrl) {
        ErrorInfo errorInfo = getErrorInfo(errorCode);
        String suggestions = generateSuggestions(errorCode, failingUrl);
        String networkInfo = generateNetworkInfo();
        String timestamp = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
        
        return String.format(ERROR_PAGE_TEMPLATE,
            errorInfo.icon,
            errorInfo.title,
            errorInfo.description,
            errorCode,
            failingUrl != null ? failingUrl : "未知",
            timestamp,
            suggestions,
            networkInfo
        );
    }

    /**
     * 根据错误代码获取错误信息
     */
    private ErrorInfo getErrorInfo(int errorCode) {
        switch (errorCode) {
            case WebViewClient.ERROR_HOST_LOOKUP:
                return new ErrorInfo("🔍", "找不到网站", "域名解析失败，无法找到该网站的服务器");
                
            case WebViewClient.ERROR_CONNECT:
                return new ErrorInfo("🔌", "连接失败", "无法连接到服务器，请检查网络连接");
                
            case WebViewClient.ERROR_TIMEOUT:
                return new ErrorInfo("⏰", "连接超时", "服务器响应时间过长，请稍后重试");
                
            case WebViewClient.ERROR_REDIRECT_LOOP:
                return new ErrorInfo("🔄", "重定向循环", "网站配置错误，出现了无限重定向");
                
            case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
                return new ErrorInfo("❓", "不支持的协议", "该链接使用了不支持的协议");
                
            case WebViewClient.ERROR_AUTHENTICATION:
                return new ErrorInfo("🔐", "需要身份验证", "访问此页面需要登录或身份验证");
                
            case WebViewClient.ERROR_PROXY_AUTHENTICATION:
                return new ErrorInfo("🛡️", "代理验证失败", "代理服务器需要身份验证");
                
            case WebViewClient.ERROR_IO:
                return new ErrorInfo("📡", "网络IO错误", "数据传输过程中出现错误");
                
            case WebViewClient.ERROR_FILE_NOT_FOUND:
                return new ErrorInfo("📄", "页面不存在", "请求的页面或文件不存在");
                
            case WebViewClient.ERROR_TOO_MANY_REQUESTS:
                return new ErrorInfo("🚫", "请求过于频繁", "服务器限制了访问频率，请稍后重试");
                
            case WebViewClient.ERROR_UNSAFE_RESOURCE:
                return new ErrorInfo("⚠️", "不安全的资源", "该资源可能存在安全风险");
                
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                return new ErrorInfo("🔒", "SSL连接失败", "安全连接建立失败，证书可能有问题");
                
            default:
                return new ErrorInfo("❌", "加载失败", "页面加载时遇到未知错误");
        }
    }

    /**
     * 生成针对性建议
     */
    private String generateSuggestions(int errorCode, String failingUrl) {
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("<div class=\"suggestions\"><h4>💡 解决建议:</h4><ul>");
        
        switch (errorCode) {
            case WebViewClient.ERROR_HOST_LOOKUP:
                suggestions.append("<li>检查网址拼写是否正确</li>");
                suggestions.append("<li>尝试切换到移动网络或WiFi</li>");
                suggestions.append("<li>清除DNS缓存后重试</li>");
                break;
                
            case WebViewClient.ERROR_CONNECT:
            case WebViewClient.ERROR_TIMEOUT:
                suggestions.append("<li>检查网络连接状态</li>");
                suggestions.append("<li>尝试刷新页面或稍后重试</li>");
                suggestions.append("<li>如果使用VPN，尝试切换节点</li>");
                break;
                
            case WebViewClient.ERROR_FILE_NOT_FOUND:
                suggestions.append("<li>检查网址是否正确</li>");
                suggestions.append("<li>该页面可能已被移动或删除</li>");
                suggestions.append("<li>尝试访问网站首页</li>");
                break;
                
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                suggestions.append("<li>检查设备时间设置是否正确</li>");
                suggestions.append("<li>尝试使用HTTP而非HTTPS访问</li>");
                suggestions.append("<li>该网站的安全证书可能过期</li>");
                break;
                
            default:
                suggestions.append("<li>检查网络连接状态</li>");
                suggestions.append("<li>清除浏览器缓存后重试</li>");
                suggestions.append("<li>尝试使用其他网络访问</li>");
        }
        
        // 添加通用建议
        suggestions.append("<li>如问题持续，请联系网站管理员</li>");
        suggestions.append("</ul></div>");
        
        return suggestions.toString();
    }

    /**
     * 生成网络信息
     */
    private String generateNetworkInfo() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    String networkType = activeNetwork.getTypeName();
                    String extraInfo = activeNetwork.getExtraInfo();
                    
                    return String.format(
                        "<div class=\"network-info\">" +
                        "<strong>网络状态:</strong> <span id=\"network-status\">已连接 (%s)</span><br>" +
                        "<strong>连接信息:</strong> %s" +
                        "</div>",
                        networkType,
                        extraInfo != null ? extraInfo : "无附加信息"
                    );
                } else {
                    return "<div class=\"network-info\" style=\"background: #fed7d7; border-color: #fc8181; color: #c53030;\">" +
                           "<strong>网络状态:</strong> <span id=\"network-status\">未连接</span>" +
                           "</div>";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting network info", e);
        }
        
        return "<div class=\"network-info\">" +
               "<strong>网络状态:</strong> <span id=\"network-status\">检测中...</span>" +
               "</div>";
    }

    /**
     * 生成简化错误页面（用于严重错误情况）
     */
    public String generateSimpleErrorPage(String title, String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>" + title + "</title></head>" +
               "<body style=\"font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5;\">" +
               "<div style=\"background: white; padding: 30px; border-radius: 10px; max-width: 400px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,0.1);\">" +
               "<h1 style=\"color: #e74c3c; margin-bottom: 20px;\">⚠️ " + title + "</h1>" +
               "<p style=\"color: #666; line-height: 1.6;\">" + message + "</p>" +
               "<button onclick=\"window.location.reload();\" style=\"margin: 20px 10px; padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 5px; cursor: pointer;\">重新加载</button>" +
               "<button onclick=\"history.back();\" style=\"margin: 20px 10px; padding: 10px 20px; background: #95a5a6; color: white; border: none; border-radius: 5px; cursor: pointer;\">返回</button>" +
               "</div></body></html>";
    }

    /**
     * 检查是否应该自动重试
     */
    public boolean shouldAutoRetry(int errorCode) {
        return errorCode == WebViewClient.ERROR_TIMEOUT ||
               errorCode == WebViewClient.ERROR_CONNECT ||
               errorCode == WebViewClient.ERROR_HOST_LOOKUP;
    }

    /**
     * 错误信息数据类
     */
    private static class ErrorInfo {
        final String icon;
        final String title;
        final String description;

        ErrorInfo(String icon, String title, String description) {
            this.icon = icon;
            this.title = title;
            this.description = description;
        }
    }
}