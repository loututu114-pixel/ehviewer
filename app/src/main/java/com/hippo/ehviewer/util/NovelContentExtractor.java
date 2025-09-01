package com.hippo.ehviewer.util;

import android.util.Log;
import android.webkit.WebView;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 小说内容提取器
 * 从网页中提取小说内容
 */
public class NovelContentExtractor {

    private static final String TAG = "NovelContentExtractor";
    private static NovelContentExtractor instance;

    // 常见的小说内容选择器
    private static final String[] CONTENT_SELECTORS = {
        // 通用选择器
        "#content",
        ".content",
        "#chapter_content",
        ".chapter-content",
        "#novel_content",
        ".novel-content",
        "#text",
        ".text",
        "#chapter",
        ".chapter",
        ".read-content",
        "#read_content",
        ".article-content",
        "#article_content",

        // 起点中文网
        "#chapter_content",
        ".read-section",

        // 晋江文学城
        ".noveltext",
        "#noveltext",

        // 纵横中文网
        ".content",

        // 17K小说网
        "#chapterContent",
        ".chapterContent",

        // 番茄小说
        ".article-content",

        // 色情小说网站
        "#content",
        ".content",
        "#text",
        ".text",
        ".novel-content",
        "#novel-content",

        // 其他常见选择器
        ".chapter-body",
        ".read-body",
        ".novel-body",
        ".text-content",
        ".content-body",
        ".main-content",
        ".chapter-text"
    };

    // 需要过滤掉的元素选择器
    private static final String[] FILTER_SELECTORS = {
        "script", "style", "nav", "header", "footer", "aside",
        ".ad", ".advertisement", ".banner", ".popup",
        ".sidebar", ".recommend", ".comment",
        ".share", ".social", ".related",
        ".copyright", ".author-info", ".chapter-info"
    };

    public static NovelContentExtractor getInstance() {
        if (instance == null) {
            instance = new NovelContentExtractor();
        }
        return instance;
    }

    /**
     * 从WebView中提取小说内容
     */
    public void extractContent(WebView webView, ContentCallback callback) {
        if (webView == null || callback == null) return;

        String script = buildExtractionScript();
        webView.evaluateJavascript(script, result -> {
            try {
                if (result != null && !"null".equals(result)) {
                    // 清理JSON结果
                    String jsonResult = cleanJsonResult(result);

                    // 解析内容
                    ExtractedContent content = parseExtractedContent(jsonResult);

                    if (content != null && content.text != null && !content.text.trim().isEmpty()) {
                        callback.onContentExtracted(content);
                    } else {
                        callback.onExtractionFailed("未能提取到有效内容");
                    }
                } else {
                    callback.onExtractionFailed("提取脚本执行失败");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing extracted content", e);
                callback.onExtractionFailed("内容解析失败: " + e.getMessage());
            }
        });
    }

    /**
     * 构建内容提取脚本
     */
    private String buildExtractionScript() {
        StringBuilder script = new StringBuilder();
        script.append("(function() {");
        script.append("    try {");

        // 尝试不同的选择器
        script.append("        var selectors = [");
        for (int i = 0; i < CONTENT_SELECTORS.length; i++) {
            script.append("'").append(CONTENT_SELECTORS[i]).append("'");
            if (i < CONTENT_SELECTORS.length - 1) {
                script.append(",");
            }
        }
        script.append("        ];");

        // 过滤器选择器
        script.append("        var filters = [");
        for (int i = 0; i < FILTER_SELECTORS.length; i++) {
            script.append("'").append(FILTER_SELECTORS[i]).append("'");
            if (i < FILTER_SELECTORS.length - 1) {
                script.append(",");
            }
        }
        script.append("        ];");

        script.append("        var content = null;");
        script.append("        var title = document.title || '';");
        script.append("        var author = '';");
        script.append("        var chapter = '';");
        script.append("        var wordCount = 0;");

        // 查找内容元素
        script.append("        for (var i = 0; i < selectors.length; i++) {");
        script.append("            var elements = document.querySelectorAll(selectors[i]);");
        script.append("            for (var j = 0; j < elements.length; j++) {");
        script.append("                var element = elements[j];");
        script.append("                var text = element.textContent || element.innerText || '';");
        script.append("                text = text.trim();");

        // 检查内容质量
        script.append("                if (text.length > 100) {");
        script.append("                    var paragraphs = text.split(/[\\n\\r]+/).length;");
        script.append("                    if (paragraphs > 3) {");
        script.append("                        content = element;");
        script.append("                        wordCount = text.length;");
        script.append("                        break;");
        script.append("                    }");
        script.append("                }");
        script.append("            }");
        script.append("            if (content) break;");
        script.append("        }");

        // 如果没找到，使用body作为备选
        script.append("        if (!content) {");
        script.append("            content = document.body;");
        script.append("        }");

        // 提取纯文本内容
        script.append("        var extractedText = '';");
        script.append("        if (content) {");
        script.append("            extractedText = extractTextContent(content, filters);");
        script.append("        }");

        // 提取章节标题
        script.append("        chapter = extractChapterTitle(document);");

        // 提取作者信息
        script.append("        author = extractAuthor(document);");

        // 返回结果
        script.append("        return JSON.stringify({");
        script.append("            title: title,");
        script.append("            author: author,");
        script.append("            chapter: chapter,");
        script.append("            text: extractedText,");
        script.append("            wordCount: wordCount,");
        script.append("            url: window.location.href");
        script.append("        });");

        // 辅助函数
        script.append("        function extractTextContent(element, filters) {");
        script.append("            var clone = element.cloneNode(true);");

        // 移除不需要的元素
        script.append("            for (var i = 0; i < filters.length; i++) {");
        script.append("                var filterElements = clone.querySelectorAll(filters[i]);");
        script.append("                for (var j = 0; j < filterElements.length; j++) {");
        script.append("                    filterElements[j].parentNode.removeChild(filterElements[j]);");
        script.append("                }");
        script.append("            }");

        // 提取文本
        script.append("            return clone.textContent || clone.innerText || '';");
        script.append("        }");

        script.append("        function extractChapterTitle(doc) {");
        script.append("            var patterns = [");
        script.append("                /第[一二三四五六七八九十百千万\\d]+章[\\s\\n\\r]*([^\\n\\r]{1,50})/i,");
        script.append("                /章节[:：\\s]*([^\\n\\r]{1,50})/i,");
        script.append("                /(\\d+\\.[^\\n\\r]{1,50})/i");
        script.append("            ];");

        script.append("            for (var i = 0; i < patterns.length; i++) {");
        script.append("                var match = doc.title.match(patterns[i]);");
        script.append("                if (match && match[1]) {");
        script.append("                    return match[1].trim();");
        script.append("                }");
        script.append("            }");

        script.append("            var h1 = doc.querySelector('h1');");
        script.append("            if (h1) {");
        script.append("                return h1.textContent.trim();");
        script.append("            }");

        script.append("            return '';");
        script.append("        }");

        script.append("        function extractAuthor(doc) {");
        script.append("            var patterns = [");
        script.append("                /作者[:：\\s]*([^\\n\\r]+)/i,");
        script.append("                /文[:：\\s]*([^\\n\\r]+)/i,");
        script.append("                /by[:：\\s]*([^\\n\\r]+)/i");
        script.append("            ];");

        script.append("            for (var i = 0; i < patterns.length; i++) {");
        script.append("                var elements = doc.querySelectorAll('*');");
        script.append("                for (var j = 0; j < elements.length; j++) {");
        script.append("                    var text = elements[j].textContent || elements[j].innerText;");
        script.append("                    var match = text.match(patterns[i]);");
        script.append("                    if (match && match[1]) {");
        script.append("                        var author = match[1].trim();");
        script.append("                        if (author.length < 50) {");
        script.append("                            return author;");
        script.append("                        }");
        script.append("                    }");
        script.append("                }");
        script.append("            }");
        script.append("            return '';");
        script.append("        }");

        script.append("    } catch (e) {");
        script.append("        console.error('Content extraction error:', e);");
        script.append("        return JSON.stringify({");
        script.append("            error: e.message,");
        script.append("            title: document.title || '',");
        script.append("            text: '',");
        script.append("            wordCount: 0");
        script.append("        });");
        script.append("    }");
        script.append("})();");

        return script.toString();
    }

    /**
     * 清理JSON结果
     */
    private String cleanJsonResult(String result) {
        if (result == null) return null;

        // 移除JavaScript的引号包装
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }

        // 移除转义字符
        result = result.replace("\\\"", "\"");
        result = result.replace("\\\\", "\\");

        return result;
    }

    /**
     * 解析提取的内容
     */
    private ExtractedContent parseExtractedContent(String jsonStr) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonStr);

            ExtractedContent content = new ExtractedContent();
            content.title = json.optString("title", "");
            content.author = json.optString("author", "");
            content.chapter = json.optString("chapter", "");
            content.text = json.optString("text", "");
            content.wordCount = json.optInt("wordCount", 0);
            content.url = json.optString("url", "");

            // 清理和格式化文本
            if (content.text != null) {
                content.text = cleanText(content.text);
            }

            return content;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON content", e);
            return null;
        }
    }

    /**
     * 清理和格式化文本
     */
    private String cleanText(String text) {
        if (text == null) return "";

        // 移除多余的空白字符
        text = text.replaceAll("\\s+", " ");

        // 移除重复的换行符
        text = text.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");

        // 移除开头和结尾的空白
        text = text.trim();

        // 如果文本太短，可能是提取错误
        if (text.length() < 50) {
            return "";
        }

        return text;
    }

    /**
     * 提取的内容类
     */
    public static class ExtractedContent {
        public String title;
        public String author;
        public String chapter;
        public String text;
        public int wordCount;
        public String url;

        public boolean isValid() {
            return text != null && !text.trim().isEmpty() && wordCount > 50;
        }

        @Override
        public String toString() {
            return "ExtractedContent{" +
                    "title='" + title + '\'' +
                    ", author='" + author + '\'' +
                    ", chapter='" + chapter + '\'' +
                    ", wordCount=" + wordCount +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    /**
     * 内容提取回调接口
     */
    public interface ContentCallback {
        void onContentExtracted(ExtractedContent content);
        void onExtractionFailed(String error);
    }
}
