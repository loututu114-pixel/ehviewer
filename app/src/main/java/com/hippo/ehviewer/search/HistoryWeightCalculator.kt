package com.hippo.ehviewer.search

import com.hippo.ehviewer.client.data.HistoryInfo
import com.hippo.ehviewer.client.data.BookmarkInfo
import kotlin.math.*

/**
 * 历史权重计算器 - 基于多源混合策略计算建议项权重
 * 支持用户行为学习和个性化权重调整
 */
object HistoryWeightCalculator {
    
    // 权重配置
    data class WeightConfig(
        val historyWeight: Double = 0.40,      // 历史记录权重 40%
        val bookmarkWeight: Double = 0.30,     // 书签权重 30%
        val searchWeight: Double = 0.20,       // 搜索建议权重 20%
        val domainWeight: Double = 0.10        // 热门网站权重 10%
    )
    
    // 时间衰减参数
    private const val TIME_DECAY_FACTOR = 0.1  // 时间衰减系数
    private const val MAX_TIME_DAYS = 365      // 最大有效时间（天）
    
    // 访问频率参数
    private const val FREQUENCY_BOOST_FACTOR = 1.5  // 频率加成系数
    private const val MIN_VISITS_FOR_BOOST = 3      // 获得频率加成的最小访问次数
    
    // 匹配类型权重
    private val MATCH_TYPE_WEIGHTS = mapOf(
        FuzzyMatchEngine.MatchType.EXACT to 1.0,
        FuzzyMatchEngine.MatchType.START_WITH to 0.95,
        FuzzyMatchEngine.MatchType.CONTAIN to 0.85,
        FuzzyMatchEngine.MatchType.PINYIN_EXACT to 0.90,
        FuzzyMatchEngine.MatchType.PINYIN_PARTIAL to 0.80,
        FuzzyMatchEngine.MatchType.ABBREVIATION to 0.85,
        FuzzyMatchEngine.MatchType.EDIT_DISTANCE to 0.70,
        FuzzyMatchEngine.MatchType.SEMANTIC to 0.60,
        FuzzyMatchEngine.MatchType.NONE to 0.0
    )
    
    private val config = WeightConfig()
    
    /**
     * 计算历史记录权重分数
     */
    fun calculateHistoryScore(
        historyInfo: HistoryInfo,
        matchResult: FuzzyMatchEngine.MatchResult,
        query: String
    ): Double {
        val baseScore = matchResult.score.toDouble()
        
        // 时间衰减计算
        val timeDecay = calculateTimeDecay(historyInfo.visitTime)
        
        // 访问频率加成
        val frequencyBoost = calculateFrequencyBoost(historyInfo.visitCount)
        
        // 匹配类型权重
        val matchTypeWeight = MATCH_TYPE_WEIGHTS[matchResult.matchType] ?: 0.0
        
        // 查询长度相关性（越长越精确）
        val queryLengthFactor = calculateQueryLengthFactor(query, historyInfo.title)
        
        // 综合计算
        val finalScore = baseScore * matchTypeWeight * timeDecay * frequencyBoost * queryLengthFactor * config.historyWeight
        
        return finalScore.coerceAtMost(100.0)
    }
    
    /**
     * 计算书签权重分数
     */
    fun calculateBookmarkScore(
        bookmarkInfo: BookmarkInfo,
        matchResult: FuzzyMatchEngine.MatchResult,
        query: String
    ): Double {
        val baseScore = matchResult.score.toDouble()
        
        // 书签通常没有时间衰减（用户主动收藏）
        val bookmarkBonus = 1.2  // 书签额外加成
        
        // 匹配类型权重
        val matchTypeWeight = MATCH_TYPE_WEIGHTS[matchResult.matchType] ?: 0.0
        
        // 查询长度相关性
        val queryLengthFactor = calculateQueryLengthFactor(query, bookmarkInfo.title)
        
        // 分类权重（书签没有分类信息，使用域名相关性）
        val categoryBonus = calculateDomainBonus(bookmarkInfo.url, query)
        
        // 综合计算
        val finalScore = baseScore * matchTypeWeight * bookmarkBonus * queryLengthFactor * categoryBonus * config.bookmarkWeight
        
        return finalScore.coerceAtMost(100.0)
    }
    
    /**
     * 计算搜索建议权重分数
     */
    fun calculateSearchSuggestionScore(
        suggestion: String,
        query: String,
        popularity: Int = 50  // 搜索建议的流行度
    ): Double {
        // 搜索建议的匹配计算
        val matchResult = FuzzyMatchEngine.match(query, suggestion)
        val baseScore = matchResult.score.toDouble()
        
        // 流行度加成
        val popularityBonus = (popularity / 100.0).coerceIn(0.5, 1.5)
        
        // 匹配类型权重
        val matchTypeWeight = MATCH_TYPE_WEIGHTS[matchResult.matchType] ?: 0.0
        
        // 搜索建议通常较新，给予时效性加成
        val freshnessBonus = 1.1
        
        // 综合计算
        val finalScore = baseScore * matchTypeWeight * popularityBonus * freshnessBonus * config.searchWeight
        
        return finalScore.coerceAtMost(100.0)
    }
    
    /**
     * 计算域名建议权重分数
     */
    fun calculateDomainScore(
        domain: String,
        query: String,
        priority: Int = 50
    ): Double {
        val matchResult = FuzzyMatchEngine.match(query, domain)
        val baseScore = matchResult.score.toDouble()
        
        // 域名优先级加成
        val priorityBonus = (priority / 50.0).coerceIn(0.8, 1.5)
        
        // 常用域名额外加成
        val commonDomainBonus = calculateCommonDomainBonus(domain)
        
        // 匹配类型权重
        val matchTypeWeight = MATCH_TYPE_WEIGHTS[matchResult.matchType] ?: 0.0
        
        // 综合计算
        val finalScore = baseScore * matchTypeWeight * priorityBonus * commonDomainBonus * config.domainWeight
        
        return finalScore.coerceAtMost(100.0)
    }
    
    /**
     * 计算时间衰减因子
     */
    private fun calculateTimeDecay(visitTime: Long): Double {
        val currentTime = System.currentTimeMillis()
        val daysPassed = (currentTime - visitTime) / (24 * 60 * 60 * 1000)
        
        // 使用指数衰减函数
        return exp(-TIME_DECAY_FACTOR * daysPassed / MAX_TIME_DAYS).coerceAtLeast(0.1)
    }
    
    /**
     * 计算访问频率加成
     */
    private fun calculateFrequencyBoost(visitCount: Int): Double {
        return if (visitCount >= MIN_VISITS_FOR_BOOST) {
            1.0 + (visitCount - MIN_VISITS_FOR_BOOST) * 0.1 * FREQUENCY_BOOST_FACTOR
        } else {
            1.0
        }.coerceAtMost(2.0)
    }
    
    /**
     * 计算查询长度相关因子
     */
    private fun calculateQueryLengthFactor(query: String, title: String): Double {
        val queryLength = query.length
        val titleLength = title.length
        
        // 查询越长，匹配越精确，给予更高权重
        val lengthRatio = queryLength.toDouble() / titleLength.coerceAtLeast(1)
        
        return (0.8 + lengthRatio * 0.4).coerceIn(0.5, 1.3)
    }
    
    /**
     * 计算域名相关性加成
     */
    private fun calculateDomainBonus(url: String?, query: String): Double {
        if (url.isNullOrEmpty()) return 1.0
        
        val urlLower = url.lowercase()
        val queryLower = query.lowercase()
        
        // 如果查询与URL域名相关，给予加成
        return when {
            urlLower.contains(queryLower) || queryLower.contains(urlLower) -> 1.2
            else -> 1.0
        }
    }
    
    /**
     * 计算常用域名加成
     */
    private fun calculateCommonDomainBonus(domain: String): Double {
        val commonDomains = listOf(
            "google.com", "facebook.com", "youtube.com", "twitter.com",
            "github.com", "stackoverflow.com", "baidu.com", "zhihu.com",
            "bilibili.com", "taobao.com", "jd.com", "weibo.com"
        )
        
        return if (commonDomains.any { domain.contains(it) }) {
            1.3
        } else {
            1.0
        }
    }
    
    /**
     * 用户行为学习 - 根据用户点击行为调整权重
     */
    data class UserBehavior(
        val query: String,
        val clickedItem: String,
        val clickedType: String,  // "history", "bookmark", "search", "domain"
        val timestamp: Long
    )
    
    private val userBehaviorHistory = mutableListOf<UserBehavior>()
    private val adaptiveWeights = mutableMapOf<String, WeightConfig>()
    
    /**
     * 记录用户点击行为
     */
    fun recordUserClick(behavior: UserBehavior) {
        userBehaviorHistory.add(behavior)
        
        // 保持历史记录在合理范围内
        if (userBehaviorHistory.size > 1000) {
            userBehaviorHistory.removeAt(0)
        }
        
        // 更新自适应权重
        updateAdaptiveWeights(behavior)
    }
    
    /**
     * 更新自适应权重
     */
    private fun updateAdaptiveWeights(behavior: UserBehavior) {
        val queryPrefix = behavior.query.take(2) // 使用查询前缀作为权重分组
        val currentWeights = adaptiveWeights[queryPrefix] ?: config
        
        // 根据点击类型调整权重
        val adjustment = 0.05
        val newWeights = when (behavior.clickedType) {
            "history" -> currentWeights.copy(
                historyWeight = (currentWeights.historyWeight + adjustment).coerceAtMost(0.7),
                searchWeight = (currentWeights.searchWeight - adjustment/3).coerceAtLeast(0.1)
            )
            "bookmark" -> currentWeights.copy(
                bookmarkWeight = (currentWeights.bookmarkWeight + adjustment).coerceAtMost(0.6),
                searchWeight = (currentWeights.searchWeight - adjustment/3).coerceAtLeast(0.1)
            )
            "search" -> currentWeights.copy(
                searchWeight = (currentWeights.searchWeight + adjustment).coerceAtMost(0.5),
                historyWeight = (currentWeights.historyWeight - adjustment/3).coerceAtLeast(0.2)
            )
            "domain" -> currentWeights.copy(
                domainWeight = (currentWeights.domainWeight + adjustment).coerceAtMost(0.3),
                searchWeight = (currentWeights.searchWeight - adjustment/3).coerceAtLeast(0.1)
            )
            else -> currentWeights
        }
        
        adaptiveWeights[queryPrefix] = newWeights
    }
    
    /**
     * 获取查询的自适应权重配置
     */
    fun getAdaptiveWeights(query: String): WeightConfig {
        val queryPrefix = query.take(2)
        return adaptiveWeights[queryPrefix] ?: config
    }
    
    /**
     * 综合评分计算 - 考虑多种因素的最终评分
     */
    fun calculateFinalScore(
        baseScore: Double,
        matchResult: FuzzyMatchEngine.MatchResult,
        itemType: String,
        query: String,
        additionalFactors: Map<String, Double> = emptyMap()
    ): Double {
        val adaptiveWeights = getAdaptiveWeights(query)
        val matchTypeWeight = MATCH_TYPE_WEIGHTS[matchResult.matchType] ?: 0.0
        
        // 基础权重
        val typeWeight = when (itemType) {
            "history" -> adaptiveWeights.historyWeight
            "bookmark" -> adaptiveWeights.bookmarkWeight
            "search" -> adaptiveWeights.searchWeight
            "domain" -> adaptiveWeights.domainWeight
            else -> 0.1
        }
        
        // 附加因素
        val additionalBonus = additionalFactors.values.fold(1.0) { acc, factor -> acc * factor }
        
        return (baseScore * matchTypeWeight * typeWeight * additionalBonus).coerceAtMost(100.0)
    }
    
    /**
     * 清理过期的行为数据
     */
    fun cleanupOldBehaviors(maxAge: Long = 30L * 24 * 60 * 60 * 1000) {
        val cutoffTime = System.currentTimeMillis() - maxAge
        userBehaviorHistory.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * 获取用户行为统计
     */
    fun getUserBehaviorStats(): Map<String, Int> {
        return userBehaviorHistory.groupBy { it.clickedType }
            .mapValues { it.value.size }
    }
}