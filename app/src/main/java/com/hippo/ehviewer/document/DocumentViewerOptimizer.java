package com.hippo.ehviewer.document;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;

/**
 * 文档查看器优化器
 * 参考YCWebView的文档加载功能，支持多种文档格式
 * 
 * 支持格式：
 * 1. Office文档 - Word (.doc, .docx), Excel (.xls, .xlsx), PowerPoint (.ppt, .pptx)
 * 2. PDF文档 (.pdf)
 * 3. 文本文档 (.txt, .md, .json, .xml)
 * 4. 图片文档 (.jpg, .png, .gif, .webp)
 * 5. 压缩文件 (.zip, .rar, .7z)
 */
public class DocumentViewerOptimizer {
    private static final String TAG = "DocumentViewerOptimizer";
    
    // 支持的文档类型
    private static final String[] OFFICE_EXTENSIONS = {
        ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
    };
    
    private static final String[] PDF_EXTENSIONS = {
        ".pdf"
    };
    
    private static final String[] TEXT_EXTENSIONS = {
        ".txt", ".md", ".json", ".xml", ".csv", ".log"
    };
    
    private static final String[] IMAGE_EXTENSIONS = {
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg"
    };
    
    private static final String[] ARCHIVE_EXTENSIONS = {
        ".zip", ".rar", ".7z", ".tar", ".gz"
    };
    
    // 在线文档查看器服务
    private static final String GOOGLE_DOCS_VIEWER = "https://docs.google.com/viewer?url=";
    private static final String MICROSOFT_VIEWER = "https://view.officeapps.live.com/op/view.aspx?src=";
    
    private final Context mContext;
    
    public DocumentViewerOptimizer(Context context) {
        mContext = context;
        Log.d(TAG, "DocumentViewerOptimizer initialized");
    }
    
    /**
     * 检查是否是支持的文档类型
     */
    public boolean isSupportedDocument(String url) {
        if (url == null) return false;
        
        String lowerUrl = url.toLowerCase();
        
        // 检查Office文档
        for (String ext : OFFICE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return true;
        }
        
        // 检查PDF文档
        for (String ext : PDF_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return true;
        }
        
        // 检查文本文档
        for (String ext : TEXT_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return true;
        }
        
        // 检查图片文档
        for (String ext : IMAGE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return true;
        }
        
        // 检查压缩文件
        for (String ext : ARCHIVE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return true;
        }
        
        return false;
    }
    
    /**
     * 获取文档类型
     */
    public DocumentType getDocumentType(String url) {
        if (url == null) return DocumentType.UNKNOWN;
        
        String lowerUrl = url.toLowerCase();
        
        for (String ext : OFFICE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return DocumentType.OFFICE;
        }
        
        for (String ext : PDF_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return DocumentType.PDF;
        }
        
        for (String ext : TEXT_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return DocumentType.TEXT;
        }
        
        for (String ext : IMAGE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return DocumentType.IMAGE;
        }
        
        for (String ext : ARCHIVE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return DocumentType.ARCHIVE;
        }
        
        return DocumentType.UNKNOWN;
    }
    
    /**
     * 优化文档加载URL
     */
    public String optimizeDocumentUrl(String originalUrl) {
        if (originalUrl == null || !isSupportedDocument(originalUrl)) {
            return originalUrl;
        }
        
        DocumentType type = getDocumentType(originalUrl);
        
        try {
            switch (type) {
                case OFFICE:
                    return optimizeOfficeDocument(originalUrl);
                
                case PDF:
                    return optimizePdfDocument(originalUrl);
                
                case TEXT:
                    return optimizeTextDocument(originalUrl);
                
                case IMAGE:
                    return optimizeImageDocument(originalUrl);
                
                case ARCHIVE:
                    return optimizeArchiveDocument(originalUrl);
                
                default:
                    return originalUrl;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to optimize document URL: " + originalUrl, e);
            return originalUrl;
        }
    }
    
    /**
     * 优化Office文档
     */
    private String optimizeOfficeDocument(String url) {
        // 优先使用Google Docs查看器
        if (url.startsWith("http")) {
            String optimizedUrl = GOOGLE_DOCS_VIEWER + Uri.encode(url);
            Log.d(TAG, "Office document optimized with Google Docs viewer: " + url);
            return optimizedUrl;
        }
        
        return url;
    }
    
    /**
     * 优化PDF文档
     */
    private String optimizePdfDocument(String url) {
        // PDF可以直接在WebView中显示
        if (url.startsWith("http")) {
            Log.d(TAG, "PDF document will be displayed directly: " + url);
            return url;
        }
        
        return url;
    }
    
    /**
     * 优化文本文档
     */
    private String optimizeTextDocument(String url) {
        // 文本文档直接显示
        Log.d(TAG, "Text document will be displayed directly: " + url);
        return url;
    }
    
    /**
     * 优化图片文档
     */
    private String optimizeImageDocument(String url) {
        // 图片直接显示
        Log.d(TAG, "Image document will be displayed directly: " + url);
        return url;
    }
    
    /**
     * 优化压缩文件
     */
    private String optimizeArchiveDocument(String url) {
        // 压缩文件提示下载
        Log.d(TAG, "Archive document detected: " + url);
        return url;
    }
    
    /**
     * 为文档查看优化WebView设置
     */
    public void optimizeWebViewForDocuments(WebView webView) {
        if (webView == null) return;
        
        try {
            android.webkit.WebSettings settings = webView.getSettings();
            
            // 启用缩放支持
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            
            // 设置视口
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            
            // 启用JavaScript（某些在线查看器需要）
            settings.setJavaScriptEnabled(true);
            
            // 设置缓存模式
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            
            // 启用DOM存储
            settings.setDomStorageEnabled(true);
            
            // 设置文本编码
            settings.setDefaultTextEncodingName("UTF-8");
            
            // 优化文档显示的User Agent
            String userAgent = settings.getUserAgentString();
            if (userAgent != null && !userAgent.contains("DocumentViewer")) {
                settings.setUserAgentString(userAgent + " DocumentViewer/1.0");
            }
            
            Log.d(TAG, "WebView optimized for document viewing");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to optimize WebView for documents", e);
        }
    }
    
    /**
     * 处理文档下载
     */
    public boolean handleDocumentDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        try {
            if (!isSupportedDocument(url)) {
                return false;
            }
            
            DocumentType type = getDocumentType(url);
            
            // 对于某些文档类型，建议下载而不是在线查看
            if (type == DocumentType.ARCHIVE) {
                startDownload(url, userAgent, contentDisposition, mimeType);
                return true;
            }
            
            Log.d(TAG, "Document download handled: " + url);
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to handle document download", e);
            return false;
        }
    }
    
    /**
     * 启动下载
     */
    private void startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                mContext.startActivity(intent);
                Log.d(TAG, "Download started for: " + url);
            } else {
                Log.w(TAG, "No app available to handle download: " + url);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to start download", e);
        }
    }
    
    /**
     * 注入文档查看优化脚本
     */
    public void injectDocumentOptimizationScript(WebView webView, String url) {
        if (webView == null || !isSupportedDocument(url)) return;
        
        DocumentType type = getDocumentType(url);
        String script = buildDocumentOptimizationScript(type);
        
        try {
            webView.evaluateJavascript(script, result -> {
                Log.d(TAG, "Document optimization script executed for: " + type);
            });
        } catch (Exception e) {
            Log.w(TAG, "Failed to inject document optimization script", e);
        }
    }
    
    /**
     * 构建文档优化脚本
     */
    private String buildDocumentOptimizationScript(DocumentType type) {
        return 
            "(function() {" +
            "  console.log('Document Optimizer: Script loaded for type: " + type + "');" +
            "  " +
            "  // 通用文档优化" +
            "  function optimizeDocumentDisplay() {" +
            "    try {" +
            "      // 隐藏不必要的元素" +
            "      var elementsToHide = [" +
            "        '.ads', '.advertisement', '.sidebar', '.toolbar'" +
            "      ];" +
            "      " +
            "      elementsToHide.forEach(function(selector) {" +
            "        var elements = document.querySelectorAll(selector);" +
            "        elements.forEach(function(el) {" +
            "          if (el) el.style.display = 'none';" +
            "        });" +
            "      });" +
            "      " +
            "      // 优化文档容器" +
            "      var containers = document.querySelectorAll('[id*=\"content\"], [class*=\"document\"], [class*=\"viewer\"]');" +
            "      containers.forEach(function(container) {" +
            "        if (container) {" +
            "          container.style.width = '100%';" +
            "          container.style.maxWidth = '100%';" +
            "          container.style.margin = '0';" +
            "          container.style.padding = '10px';" +
            "        }" +
            "      });" +
            "      " +
            "      console.log('Document Optimizer: Display optimized');" +
            "    } catch (e) {" +
            "      console.warn('Document Optimizer: Failed to optimize display', e);" +
            "    }" +
            "  }" +
            "  " +
            "  // 立即执行优化" +
            "  if (document.readyState === 'loading') {" +
            "    document.addEventListener('DOMContentLoaded', optimizeDocumentDisplay);" +
            "  } else {" +
            "    optimizeDocumentDisplay();" +
            "  }" +
            "  " +
            "  // 延迟执行优化（处理动态加载的内容）" +
            "  setTimeout(optimizeDocumentDisplay, 2000);" +
            "})();";
    }
    
    /**
     * 获取支持的文档格式信息
     */
    public String getSupportedFormatsInfo() {
        return "Supported Document Formats:\n" +
               "• Office: .doc, .docx, .xls, .xlsx, .ppt, .pptx\n" +
               "• PDF: .pdf\n" +
               "• Text: .txt, .md, .json, .xml, .csv, .log\n" +
               "• Images: .jpg, .png, .gif, .webp, .bmp, .svg\n" +
               "• Archives: .zip, .rar, .7z, .tar, .gz";
    }
    
    /**
     * 文档类型枚举
     */
    public enum DocumentType {
        OFFICE,
        PDF,
        TEXT,
        IMAGE,
        ARCHIVE,
        UNKNOWN
    }
}