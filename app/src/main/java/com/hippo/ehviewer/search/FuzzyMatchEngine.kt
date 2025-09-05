package com.hippo.ehviewer.search

import android.text.TextUtils
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * 模糊匹配引擎 - 实现多种智能匹配算法
 * 支持：精确匹配、部分匹配、拼音匹配、编辑距离匹配、缩写匹配
 */
object FuzzyMatchEngine {
    
    // 匹配结果数据类
    data class MatchResult(
        val score: Int,              // 匹配分数 (0-100)
        val matchType: MatchType,    // 匹配类型
        val matchPositions: List<IntRange> = emptyList() // 匹配位置范围
    ) {
        companion object {
            @JvmField
            val NO_MATCH = MatchResult(0, MatchType.NONE)
        }
    }
    
    // 匹配类型枚举
    enum class MatchType(val priority: Int) {
        EXACT(100),           // 精确匹配
        START_WITH(90),       // 开头匹配
        CONTAIN(80),          // 包含匹配
        PINYIN_EXACT(85),     // 拼音精确匹配
        PINYIN_PARTIAL(75),   // 拼音部分匹配
        ABBREVIATION(70),     // 缩写匹配
        EDIT_DISTANCE(60),    // 编辑距离匹配
        SEMANTIC(50),         // 语义关联匹配
        NONE(0)               // 无匹配
    }
    
    // 常用网站域名映射（支持缩写匹配）
    private val COMMON_SITES = mapOf(
        "goog" to listOf("google.com", "谷歌"),
        "fb" to listOf("facebook.com", "脸书"),
        "tw" to listOf("twitter.com", "推特"),
        "yt" to listOf("youtube.com", "油管"),
        "gh" to listOf("github.com", "GitHub"),
        "bidu" to listOf("baidu.com", "百度"),
        "zhihu" to listOf("zhihu.com", "知乎"),
        "bili" to listOf("bilibili.com", "哔哩哔哩"),
        "xv" to listOf("xvideos.com"),
        "ph" to listOf("pornhub.com"),
        "xh" to listOf("xhamster.com"),
        "rt" to listOf("redtube.com")
    )
    
    // 拼音映射表（简化版）
    private val PINYIN_MAP = mapOf(
        'a' to "a",
        'b' to "b", 'p' to "p", 'm' to "m", 'f' to "f",
        'd' to "d", 't' to "t", 'n' to "n", 'l' to "l",
        'g' to "g", 'k' to "k", 'h' to "h",
        'j' to "j", 'q' to "q", 'x' to "x",
        'z' to "zh|z", 'c' to "ch|c", 's' to "sh|s",
        'r' to "r", 'y' to "y", 'w' to "w"
    )
    
    /**
     * 主要匹配函数 - 对查询和目标文本进行智能匹配
     */
    fun match(query: String, targetText: String, targetUrl: String? = null): MatchResult {
        if (query.isBlank() || targetText.isBlank()) {
            return MatchResult.NO_MATCH
        }
        
        val normalizedQuery = query.trim().lowercase()
        val normalizedText = targetText.trim().lowercase()
        val normalizedUrl = targetUrl?.trim()?.lowercase() ?: ""
        
        // 1. 精确匹配
        if (normalizedQuery == normalizedText) {
            return MatchResult(100, MatchType.EXACT)
        }
        
        // 2. 开头匹配
        if (normalizedText.startsWith(normalizedQuery)) {
            val score = calculateStartsWithScore(normalizedQuery, normalizedText)
            return MatchResult(score, MatchType.START_WITH, listOf(IntRange(0, normalizedQuery.length - 1)))
        }
        
        // 3. URL匹配
        if (normalizedUrl.isNotEmpty()) {
            val urlMatch = matchUrl(normalizedQuery, normalizedUrl)
            if (urlMatch.score > 0) {
                return urlMatch
            }
        }
        
        // 4. 包含匹配
        val containIndex = normalizedText.indexOf(normalizedQuery)
        if (containIndex >= 0) {
            val score = calculateContainScore(normalizedQuery, normalizedText, containIndex)
            return MatchResult(score, MatchType.CONTAIN, 
                listOf(IntRange(containIndex, containIndex + normalizedQuery.length - 1)))
        }
        
        // 5. 缩写匹配
        val abbreviationMatch = matchAbbreviation(normalizedQuery, normalizedText, normalizedUrl)
        if (abbreviationMatch.score > 0) {
            return abbreviationMatch
        }
        
        // 6. 拼音匹配
        val pinyinMatch = matchPinyin(normalizedQuery, normalizedText)
        if (pinyinMatch.score > 0) {
            return pinyinMatch
        }
        
        // 7. 编辑距离匹配
        val editDistanceMatch = matchEditDistance(normalizedQuery, normalizedText)
        if (editDistanceMatch.score > 0) {
            return editDistanceMatch
        }
        
        // 8. 语义关联匹配
        val semanticMatch = matchSemantic(normalizedQuery, normalizedText, normalizedUrl)
        if (semanticMatch.score > 0) {
            return semanticMatch
        }
        
        return MatchResult.NO_MATCH
    }
    
    /**
     * URL匹配
     */
    private fun matchUrl(query: String, url: String): MatchResult {
        // 域名提取
        val domain = extractDomain(url)
        
        // 精确域名匹配
        if (domain.contains(query)) {
            val score = if (domain.startsWith(query)) 95 else 85
            return MatchResult(score, MatchType.START_WITH)
        }
        
        // 路径匹配
        if (url.contains(query)) {
            return MatchResult(75, MatchType.CONTAIN)
        }
        
        return MatchResult.NO_MATCH
    }
    
    /**
     * 缩写匹配
     */
    private fun matchAbbreviation(query: String, text: String, url: String): MatchResult {
        // 检查常用网站缩写
        COMMON_SITES[query]?.let { aliases ->
            for (alias in aliases) {
                if (text.contains(alias) || url.contains(alias)) {
                    return MatchResult(85, MatchType.ABBREVIATION)
                }
            }
        }
        
        // 通用缩写匹配（首字母）
        val words = text.split(" ", ".", "-", "_")
        if (words.size >= query.length) {
            var matchCount = 0
            for (i in query.indices) {
                if (i < words.size && words[i].isNotEmpty() && 
                    words[i][0].lowercase() == query[i].toString()) {
                    matchCount++
                }
            }
            
            if (matchCount == query.length) {
                val score = (matchCount * 100 / query.length).coerceAtMost(80)
                return MatchResult(score, MatchType.ABBREVIATION)
            }
        }
        
        return MatchResult.NO_MATCH
    }
    
    /**
     * 拼音匹配（简化版）
     */
    private fun matchPinyin(query: String, text: String): MatchResult {
        // 简化的拼音匹配，主要针对中文内容
        if (!containsChinese(text)) {
            return MatchResult.NO_MATCH
        }
        
        // 提取中文字符的拼音首字母
        val pinyinInitials = extractPinyinInitials(text)
        
        if (pinyinInitials.contains(query)) {
            val score = if (pinyinInitials.startsWith(query)) 80 else 70
            return MatchResult(score, if (pinyinInitials.startsWith(query)) 
                MatchType.PINYIN_EXACT else MatchType.PINYIN_PARTIAL)
        }
        
        return MatchResult.NO_MATCH
    }
    
    /**
     * 编辑距离匹配
     */
    private fun matchEditDistance(query: String, text: String): MatchResult {
        // 只对长度相近的字符串计算编辑距离
        if (kotlin.math.abs(query.length - text.length) > query.length / 2) {
            return MatchResult.NO_MATCH
        }
        
        val distance = calculateEditDistance(query, text)
        val maxLength = max(query.length, text.length)
        val similarity = ((maxLength - distance) * 100 / maxLength)
        
        // 只有相似度超过60%才认为匹配
        return if (similarity >= 60) {
            MatchResult(similarity, MatchType.EDIT_DISTANCE)
        } else {
            MatchResult.NO_MATCH
        }
    }
    
    /**
     * 语义关联匹配
     */
    private fun matchSemantic(query: String, text: String, url: String): MatchResult {
        val semanticGroups = mapOf(
            listOf("porn", "sex", "adult", "video", "xv", "成人", "色情") to 
                listOf("pornhub", "xvideos", "xhamster", "redtube", "成人", "视频"),
            listOf("news", "新闻", "资讯") to 
                listOf("新浪", "腾讯", "网易", "搜狐", "新闻"),
            listOf("shop", "buy", "购物", "商城") to 
                listOf("淘宝", "京东", "天猫", "购物", "商城"),
            listOf("game", "游戏") to 
                listOf("steam", "游戏", "娱乐"),
            listOf("social", "社交") to 
                listOf("微博", "qq", "微信", "facebook", "twitter")
        )
        
        for ((keywords, targets) in semanticGroups) {
            if (keywords.any { query.contains(it) }) {
                if (targets.any { text.contains(it) || url.contains(it) }) {
                    return MatchResult(55, MatchType.SEMANTIC)
                }
            }
        }
        
        return MatchResult.NO_MATCH
    }
    
    /**
     * 计算开头匹配分数
     */
    private fun calculateStartsWithScore(query: String, text: String): Int {
        val ratio = query.length.toDouble() / text.length
        return (90 + ratio * 10).toInt().coerceAtMost(100)
    }
    
    /**
     * 计算包含匹配分数
     */
    private fun calculateContainScore(query: String, text: String, position: Int): Int {
        val positionRatio = 1.0 - (position.toDouble() / text.length)
        val lengthRatio = query.length.toDouble() / text.length
        return (70 + positionRatio * 10 + lengthRatio * 10).toInt().coerceAtMost(85)
    }
    
    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private fun calculateEditDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // 删除
                    dp[i][j - 1] + 1,      // 插入
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * 提取域名
     */
    private fun extractDomain(url: String): String {
        return try {
            val cleanUrl = url.replace("https://", "").replace("http://", "")
            val slashIndex = cleanUrl.indexOf('/')
            if (slashIndex > 0) cleanUrl.substring(0, slashIndex) else cleanUrl
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * 检查是否包含中文
     */
    private fun containsChinese(text: String): Boolean {
        return text.any { it.code in 0x4E00..0x9FFF }
    }
    
    /**
     * 提取拼音首字母（简化版）
     */
    private fun extractPinyinInitials(text: String): String {
        // 这是一个简化的实现，实际项目中应该使用专业的拼音库
        return text.filter { it.isLetter() }.take(10).lowercase()
    }
    
    /**
     * 批量匹配 - 对多个目标进行匹配并排序
     */
    fun batchMatch(query: String, targets: List<Pair<String, String?>>): List<Pair<Int, MatchResult>> {
        return targets.mapIndexed { index, (text, url) ->
            index to match(query, text, url)
        }.filter { it.second.score > 0 }
         .sortedByDescending { it.second.score }
    }
    
    /**
     * 高亮匹配部分
     */
    fun highlightMatches(text: String, matchResult: MatchResult): String {
        if (matchResult.matchPositions.isEmpty()) {
            return text
        }
        
        val highlighted = StringBuilder()
        var lastIndex = 0
        
        for (range in matchResult.matchPositions) {
            highlighted.append(text.substring(lastIndex, range.first))
            highlighted.append("<b>")
            highlighted.append(text.substring(range.first, range.last + 1))
            highlighted.append("</b>")
            lastIndex = range.last + 1
        }
        
        if (lastIndex < text.length) {
            highlighted.append(text.substring(lastIndex))
        }
        
        return highlighted.toString()
    }
}