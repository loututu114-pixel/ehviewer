package com.hippo.ehviewer.util;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 色情小说检测器
 * 用于识别网页内容是否包含色情小说
 */
public class EroNovelDetector {

    private static final String TAG = "EroNovelDetector";
    private static EroNovelDetector instance;

    // 色情小说特征关键词
    private static final Set<String> ERO_KEYWORDS = new HashSet<>(Arrays.asList(
        // 中文色情关键词
        "性", "爱", "欲", "情", "欲", "欢", "淫", "荡", "浪", "骚", "贱", "骚",
        "乳", "胸", "臀", "屁股", "阴", "茎", "屌", "鸡巴", "逼", "穴", "骚穴",
        "高潮", "射精", "内射", "口交", "乳交", "肛交", "自慰", "手淫",
        "处女", "破处", "第一次", "初夜", "强奸", "轮奸", "群交", "乱交",
        "母子", "父女", "师生", "兄妹", "姐弟", "姨妈", "姑姑", "嫂子",
        "小姨子", "继母", "后妈", "岳母", "丈母娘", "婆婆", "媳妇",
        "女仆", "御姐", "萝莉", "熟女", "人妻", "寡妇", "学生妹",
        "护士", "老师", "医生", "空姐", "模特", "明星", "明星",
        "绿帽", "戴绿帽", "NTR", "ntr", "被绿", "出轨", "偷情",
        "艳照", "裸照", "自拍", "私照", "床照", "果照", "裸体",
        "换妻", "群P", "多人运动", "SM", "sm", "捆绑", "调教",
        "肉便器", "性奴", "奴隶", "女王", "女王大人", "主人",
        "深喉", "颜射", "吞精", "饮精", "精液", "爱液", "淫水",
        "呻吟", "娇喘", "浪叫", "淫叫", "浪叫", "骚叫", "贱叫",
        "性感", "妖娆", "魅惑", "诱惑", "勾引", "挑逗", "撩拨",
        "欲火焚身", "欲罢不能", "欲仙欲死", "欲火中烧", "欲壑难填",
        "春药", "媚药", "催情", "壮阳", "伟哥", "避孕药",
        "性病", "艾滋", "性病", "梅毒", "淋病", "尖锐湿疣",
        "堕胎", "流产", "意外怀孕", "人流", "药流", "人流手术",
        "私生子", "非婚生子", "婚外情", "第三者", "小三",

        // 英文色情关键词
        "sex", "fuck", "porn", "nude", "naked", "erotic", "adult",
        "xxx", "hentai", "anime", "manga", "doujin", "lolicon",
        "incest", "mother", "son", "father", "daughter", "sister",
        "brother", "teacher", "student", "rape", "gangbang",
        "orgasm", "cum", "sperm", "ejaculation", "masturbation",
        "oral", "anal", "vaginal", "penis", "vagina", "breasts",
        "tits", "ass", "butt", "pussy", "cock", "dick", "blowjob",
        "handjob", "titjob", "rimjob", "creampie", "facial",
        "swallow", "cumshot", "deepthroat", "gangbang", "threesome",
        "BDSM", "bondage", "domination", "submission", "slave",
        "master", "mistress", "queen", "fetish", "kink", "pervert",
        "slut", "whore", "bitch", "cuckold", "cheating", "affair",
        "adultery", "infidelity", "seduction", "temptation", "lust",
        "desire", "passion", "arousal", "horny", "wet", "hard",
        "erection", "climax", "orgasm", "pleasure", "ecstasy"
    ));

    // 色情小说网站特征
    private static final Set<String> ERO_SITES = new HashSet<>(Arrays.asList(
        "18xs.org", "sewenhua.com", "18novel.com", "qingdou.net",
        "dajianet.com", "ranwen.net", "xxbiquge.com", "luoqiuzw.com",
        "yushuwu.com", "hentai2read.com", "69shu.com", "uukanshu.com",
        "shubaow.net", "bqg5200.com", "kanshu8.net", "zhuishushenqi.com",
        "qixiaoshuo.com", "booktxt.net", "xbiquge.so", "biquge.info"
    ));

    // 小说内容特征模式
    private static final Pattern[] NOVEL_PATTERNS = {
        Pattern.compile("第[一二三四五六七八九十百千万]+章.*"),
        Pattern.compile("第\\d+章.*"),
        Pattern.compile("正文.*第.*章"),
        Pattern.compile("章节目录"),
        Pattern.compile("最新章节"),
        Pattern.compile("上一章.*下一章"),
        Pattern.compile("目录.*章节"),
        Pattern.compile("连载中"),
        Pattern.compile("完本"),
        Pattern.compile("作者[:：].*"),
        Pattern.compile("简介[:：].*"),
        Pattern.compile("内容简介"),
        Pattern.compile("小说下载"),
        Pattern.compile("TXT下载"),
        Pattern.compile("全文阅读")
    };

    public static EroNovelDetector getInstance() {
        if (instance == null) {
            instance = new EroNovelDetector();
        }
        return instance;
    }

    /**
     * 检测网页是否包含色情小说内容
     */
    public boolean isEroNovelPage(String url, String title, String content) {
        if (url == null || content == null) {
            return false;
        }

        // 1. 检查URL是否为已知色情小说网站
        String domain = extractDomain(url);
        if (ERO_SITES.contains(domain)) {
            Log.d(TAG, "Detected known ero novel site: " + domain);
            return true;
        }

        // 2. 检查标题是否包含色情关键词
        if (title != null && containsEroKeywords(title)) {
            Log.d(TAG, "Detected ero keywords in title: " + title);
            return true;
        }

        // 3. 检查内容是否包含小说特征
        if (hasNovelStructure(content)) {
            Log.d(TAG, "Detected novel structure in content");
            return true;
        }

        // 4. 检查内容是否包含色情关键词
        if (containsEroKeywords(content)) {
            Log.d(TAG, "Detected ero keywords in content");
            return true;
        }

        // 5. 检查URL路径是否包含小说特征
        if (hasNovelUrlPattern(url)) {
            Log.d(TAG, "Detected novel URL pattern: " + url);
            return true;
        }

        return false;
    }

    /**
     * 检查文本是否包含色情关键词
     */
    public boolean containsEroKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        int keywordCount = 0;

        // 计算色情关键词出现次数
        for (String keyword : ERO_KEYWORDS) {
            if (lowerText.contains(keyword.toLowerCase())) {
                keywordCount++;
                // 如果找到多个关键词，基本可以确定是色情内容
                if (keywordCount >= 3) {
                    return true;
                }
            }
        }

        // 对于较长的文本，关键词密度也要考虑
        if (text.length() > 1000 && keywordCount >= 5) {
            return true;
        }

        return false;
    }

    /**
     * 检查内容是否具有小说结构特征
     */
    public boolean hasNovelStructure(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // 检查是否包含小说结构特征
        for (Pattern pattern : NOVEL_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查URL是否具有小说特征
     */
    public boolean hasNovelUrlPattern(String url) {
        if (url == null) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        // 检查URL路径是否包含小说特征
        if (lowerUrl.contains("/novel/") ||
            lowerUrl.contains("/book/") ||
            lowerUrl.contains("/read/") ||
            lowerUrl.contains("/txt/") ||
            lowerUrl.contains("/chapter/") ||
            lowerUrl.contains("/xiaoshuo/") ||
            lowerUrl.contains("/shu/") ||
            lowerUrl.contains("/zhangjie/")) {
            return true;
        }

        // 检查是否包含章节相关的查询参数
        if (lowerUrl.contains("chapter=") ||
            lowerUrl.contains("cid=") ||
            lowerUrl.contains("id=")) {
            return true;
        }

        return false;
    }

    /**
     * 提取小说信息
     */
    public NovelInfo extractNovelInfo(String url, String title, String content) {
        NovelInfo info = new NovelInfo();
        info.url = url;
        info.title = title != null ? title : "未知小说";
        info.domain = extractDomain(url);

        // 尝试提取作者信息
        info.author = extractAuthor(content);

        // 尝试提取简介
        info.description = extractDescription(content);

        // 尝试提取章节信息
        info.chapters = extractChapters(content);

        // 判断是否为色情小说
        info.isEro = isEroNovelPage(url, title, content);

        return info;
    }

    /**
     * 提取作者信息
     */
    private String extractAuthor(String content) {
        if (content == null) return "未知作者";

        // 尝试匹配各种作者信息格式
        String[] authorPatterns = {
            "作者[:：]\\s*([^\\n\\r]+)",
            "作者介绍[:：]\\s*([^\\n\\r]+)",
            "文[:：]\\s*([^\\n\\r]+)",
            "by[:：]\\s*([^\\n\\r]+)",
            "作者：([^\\n\\r]+)",
            "作者:([^\\n\\r]+)"
        };

        for (String pattern : authorPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(content);
            if (m.find()) {
                String author = m.group(1).trim();
                if (!author.isEmpty() && author.length() < 50) {
                    return author;
                }
            }
        }

        return "未知作者";
    }

    /**
     * 提取小说简介
     */
    private String extractDescription(String content) {
        if (content == null) return "暂无简介";

        // 尝试匹配简介信息
        String[] descPatterns = {
            "简介[:：]\\s*([^\\n\\r]+(?:\\n[^\\n\\r]+)*)",
            "内容简介[:：]\\s*([^\\n\\r]+(?:\\n[^\\n\\r]+)*)",
            "剧情简介[:：]\\s*([^\\n\\r]+(?:\\n[^\\n\\r]+)*)",
            "小说简介[:：]\\s*([^\\n\\r]+(?:\\n[^\\n\\r]+)*)",
            "作品简介[:：]\\s*([^\\n\\r]+(?:\\n[^\\n\\r]+)*)"
        };

        for (String pattern : descPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(content);
            if (m.find()) {
                String desc = m.group(1).trim();
                if (!desc.isEmpty() && desc.length() < 500) {
                    // 清理多余的空白字符
                    return desc.replaceAll("\\s+", " ");
                }
            }
        }

        return "暂无简介";
    }

    /**
     * 提取章节信息
     */
    private java.util.List<String> extractChapters(String content) {
        java.util.List<String> chapters = new java.util.ArrayList<>();

        if (content == null) return chapters;

        // 匹配章节标题
        java.util.regex.Pattern chapterPattern = java.util.regex.Pattern.compile(
            "(第[一二三四五六七八九十百千万\\d]+章[\\s\\n\\r]*[^\\n\\r]{1,50})",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );

        java.util.regex.Matcher matcher = chapterPattern.matcher(content);
        while (matcher.find() && chapters.size() < 50) { // 限制最多提取50章
            String chapter = matcher.group(1).trim();
            if (!chapters.contains(chapter)) {
                chapters.add(chapter);
            }
        }

        return chapters;
    }

    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        if (url == null) return "";

        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }

            int slashIndex = url.indexOf('/');
            if (slashIndex > 0) {
                url = url.substring(0, slashIndex);
            }

            return url.toLowerCase();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting domain", e);
            return "";
        }
    }

    /**
     * 小说信息类
     */
    public static class NovelInfo {
        public String url;
        public String title;
        public String author;
        public String description;
        public String domain;
        public java.util.List<String> chapters;
        public boolean isEro;
        public long createTime;
        public long lastReadTime;
        public int readProgress; // 0-100

        public NovelInfo() {
            this.chapters = new java.util.ArrayList<>();
            this.createTime = System.currentTimeMillis();
            this.lastReadTime = 0;
            this.readProgress = 0;
        }

        @Override
        public String toString() {
            return "NovelInfo{" +
                    "title='" + title + '\'' +
                    ", author='" + author + '\'' +
                    ", domain='" + domain + '\'' +
                    ", isEro=" + isEro +
                    ", chapters=" + chapters.size() +
                    '}';
        }
    }
}
