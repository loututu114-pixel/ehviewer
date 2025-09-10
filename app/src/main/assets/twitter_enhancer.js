// ==UserScript==
// @name         EhViewer Twitter/X 增强器
// @namespace    http://ehviewer.com/
// @version      2.0.0
// @description  深度优化Twitter/X体验：智能广告拦截、算法推荐过滤、时间线优化、交互增强
// @author       EhViewer Team
// @match        *://*.x.com/*
// @match        *://*.twitter.com/*
// @exclude      *://*.google.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer Twitter/X 增强器已启动');

    // 配置选项
    const config = {
        enabled: GM_getValue('twitterEnabled', true),
        adBlock: GM_getValue('twitterAdBlock', true),
        algorithmFilter: GM_getValue('twitterAlgorithmFilter', true),
        timelineOptimize: GM_getValue('twitterTimelineOptimize', true),
        interactionEnhance: GM_getValue('twitterInteractionEnhance', true),
        searchOptimize: GM_getValue('twitterSearchOptimize', true),
        themeEnhance: GM_getValue('twitterThemeEnhance', false),
        keyboardShortcuts: GM_getValue('twitterKeyboardShortcuts', true),
        autoScroll: GM_getValue('twitterAutoScroll', false),
        contentFilter: GM_getValue('twitterContentFilter', true)
    };

    // Twitter选择器映射
    const selectors = {
        tweet: '[data-testid="tweet"], [data-testid="Tweet-User-Text"], article[data-testid*="tweet"]',
        promoted: '[data-testid*="promoted"], .promoted-tweet, [aria-label*="Promoted"]',
        timeline: '[data-testid="primaryColumn"], [role="main"] [data-testid*="primaryColumn"]',
        sidebar: '[data-testid="sidebarColumn"], [role="complementary"]',
        searchInput: '[data-testid="SearchBox_Search_Input"], input[placeholder*="Search"]',
        likeBtn: '[data-testid*="like"], [data-testid*="unlike"]',
        retweetBtn: '[data-testid*="retweet"], [data-testid*="unretweet"]',
        replyBtn: '[data-testid*="reply"]',
        shareBtn: '[data-testid*="share"]',
        followBtn: '[data-testid*="follow"], [data-testid*="unfollow"]',
        trending: '[data-testid*="trend"], .r-1wtj0ep',
        whoToFollow: '[data-testid*="UserCell"], .r-1wtj0ep [role="link"]',
        explore: '[data-testid*="explore"]'
    };

    // 创建Twitter专用控制面板
    function createTwitterPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-twitter-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #1DA1F2, #42A5F5); color: white;
                        padding: 12px; border-radius: 8px; font-size: 11px; font-family: Arial;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3); min-width: 200px;">
                <div style="display: flex; align-items: center; margin-bottom: 8px;">
                    <span style="font-size: 16px; margin-right: 6px;">🐦</span>
                    <strong>Twitter/X 增强器</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-bottom: 8px;">
                    <button id="block-ads" title="拦截广告">🚫</button>
                    <button id="filter-algo" title="过滤算法">🎯</button>
                    <button id="optimize-timeline" title="优化时间线">📋</button>
                    <button id="enhance-ui" title="界面增强">✨</button>
                    <button id="auto-scroll" title="自动滚动">📜</button>
                    <button id="settings-btn" title="设置">⚙️</button>
                </div>
                <div style="font-size: 9px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 4px;">
                    L点赞 | R转发 | C评论 | S搜索
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('block-ads').onclick = () => blockTwitterAds();
        document.getElementById('filter-algo').onclick = () => filterAlgorithmContent();
        document.getElementById('optimize-timeline').onclick = () => optimizeTimeline();
        document.getElementById('enhance-ui').onclick = () => enhanceUI();
        document.getElementById('auto-scroll').onclick = toggleAutoScroll;
        document.getElementById('settings-btn').onclick = showTwitterSettings;
    }

    // 深度广告拦截
    function blockTwitterAds() {
        if (!config.adBlock) return;

        // 移除推广推文
        const promotedTweets = document.querySelectorAll(selectors.promoted);
        promotedTweets.forEach(tweet => {
            tweet.remove();
            GM_log('已移除推广推文');
        });

        // 移除时间线中的广告
        const timelineAds = document.querySelectorAll('[data-testid*="promoted"], [aria-label*="Promoted"]');
        timelineAds.forEach(ad => {
            const tweet = ad.closest('[data-testid="tweet"], article');
            if (tweet) {
                tweet.remove();
                GM_log('已移除时间线广告');
            }
        });

        // 移除侧边栏广告
        const sidebarAds = document.querySelectorAll(`${selectors.sidebar} [data-testid*="promoted"]`);
        sidebarAds.forEach(ad => ad.remove());

        // 持续监控新广告
        const adObserver = new MutationObserver(() => {
            blockTwitterAds();
        });

        const timeline = document.querySelector(selectors.timeline);
        if (timeline) {
            adObserver.observe(timeline, {
                childList: true,
                subtree: true
            });
        }

        showNotification('Twitter广告已清理！', 'success');
    }

    // 算法推荐过滤
    function filterAlgorithmContent() {
        if (!config.algorithmFilter) return;

        // 过滤低质量推文
        const tweets = document.querySelectorAll(selectors.tweet);
        tweets.forEach(tweet => {
            const metrics = getTweetMetrics(tweet);
            const content = getTweetContent(tweet);

            if (shouldFilterTweet(metrics, content)) {
                tweet.style.opacity = '0.4';
                tweet.style.maxHeight = '100px';
                tweet.style.overflow = 'hidden';

                // 添加"显示"按钮
                if (!tweet.querySelector('.filter-notice')) {
                    const notice = document.createElement('div');
                    notice.className = 'filter-notice';
                    notice.innerHTML = `
                        <div style="text-align: center; padding: 8px; color: #666; font-size: 12px;">
                            此推文已被过滤 (点赞: ${metrics.likes})
                            <button onclick="showFilteredTweet(this)" style="margin-left: 8px; padding: 2px 8px; background: #1DA1F2; color: white; border: none; border-radius: 3px; cursor: pointer;">显示</button>
                        </div>
                    `;
                    tweet.appendChild(notice);

                    // 添加全局函数
                    window.showFilteredTweet = (btn) => {
                        const tweet = btn.closest('[data-testid="tweet"], article');
                        tweet.style.opacity = '1';
                        tweet.style.maxHeight = 'none';
                        btn.parentElement.remove();
                    };
                }
            }
        });

        showNotification('算法推荐已过滤！', 'success');
    }

    function getTweetMetrics(tweet) {
        return {
            likes: parseInt(tweet.querySelector('[data-testid*="like"]')?.textContent || '0'),
            retweets: parseInt(tweet.querySelector('[data-testid*="retweet"]')?.textContent || '0'),
            replies: parseInt(tweet.querySelector('[data-testid*="reply"]')?.textContent || '0'),
            views: parseInt(tweet.querySelector('[role="group"] span')?.textContent?.replace(/[^0-9]/g, '') || '0')
        };
    }

    function getTweetContent(tweet) {
        const textElement = tweet.querySelector('[data-testid="Tweet-User-Text"] p, [lang]');
        return textElement ? textElement.textContent : '';
    }

    function shouldFilterTweet(metrics, content) {
        // 过滤条件
        if (metrics.likes < 5 && metrics.retweets < 2) return true; // 互动太少
        if (content.length < 10) return true; // 内容太短
        if (content.includes('广告') || content.includes('推广')) return true; // 广告内容
        if (content.match(/(.)\1{5,}/)) return true; // 重复字符太多

        return false;
    }

    // 时间线优化
    function optimizeTimeline() {
        if (!config.timelineOptimize) return;

        GM_addStyle(`
            /* Twitter时间线优化 */
            ${selectors.tweet} {
                border-radius: 12px !important;
                margin-bottom: 12px !important;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
                transition: all 0.2s ease !important;
                border: 1px solid #e1e8ed !important;
            }

            ${selectors.tweet}:hover {
                transform: translateY(-2px) !important;
                box-shadow: 0 4px 16px rgba(0,0,0,0.15) !important;
                border-color: #1DA1F2 !important;
            }

            /* 优化推文内容 */
            [data-testid="Tweet-User-Text"] {
                font-size: 15px !important;
                line-height: 1.6 !important;
                color: #14171a !important;
            }

            /* 优化互动按钮 */
            [data-testid*="like"], [data-testid*="retweet"],
            [data-testid*="reply"], [data-testid*="share"] {
                border-radius: 50% !important;
                transition: all 0.2s ease !important;
                background: transparent !important;
            }

            [data-testid*="like"]:hover, [data-testid*="retweet"]:hover,
            [data-testid*="reply"]:hover, [data-testid*="share"]:hover {
                background: rgba(29, 161, 242, 0.1) !important;
                transform: scale(1.1) !important;
            }

            /* 优化头像 */
            [data-testid="Tweet-User-Avatar"] img {
                border-radius: 50% !important;
                border: 2px solid #e1e8ed !important;
                transition: border-color 0.2s ease !important;
            }

            [data-testid="Tweet-User-Avatar"]:hover img {
                border-color: #1DA1F2 !important;
            }
        `);

        // 添加推文增强功能
        enhanceTweets();

        showNotification('时间线已优化！', 'success');
    }

    function enhanceTweets() {
        const tweets = document.querySelectorAll(selectors.tweet);
        tweets.forEach(tweet => {
            if (tweet.dataset.enhanced) return;
            tweet.dataset.enhanced = 'true';

            // 添加快速操作按钮
            addQuickActions(tweet);

            // 添加阅读时间估算
            addReadingTime(tweet);

            // 添加内容质量评分
            addContentScore(tweet);
        });
    }

    function addQuickActions(tweet) {
        const actionBar = tweet.querySelector('[role="group"]');
        if (!actionBar || tweet.querySelector('.quick-actions')) return;

        const quickActions = document.createElement('div');
        quickActions.className = 'quick-actions';
        quickActions.innerHTML = `
            <div style="display: flex; gap: 8px; margin-top: 8px; padding: 8px; background: #f7f9fa; border-radius: 8px;">
                <button onclick="quickLike(this)" style="padding: 4px 8px; background: #1DA1F2; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">👍 点赞</button>
                <button onclick="quickRetweet(this)" style="padding: 4px 8px; background: #17bf63; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">🔄 转发</button>
                <button onclick="quickReply(this)" style="padding: 4px 8px; background: #657786; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">💬 回复</button>
            </div>
        `;

        actionBar.parentElement.insertBefore(quickActions, actionBar);

        // 添加全局函数
        window.quickLike = (btn) => {
            const tweet = btn.closest('[data-testid="tweet"], article');
            const likeBtn = tweet.querySelector('[data-testid*="like"], [data-testid*="unlike"]');
            if (likeBtn) likeBtn.click();
        };

        window.quickRetweet = (btn) => {
            const tweet = btn.closest('[data-testid="tweet"], article');
            const retweetBtn = tweet.querySelector('[data-testid*="retweet"], [data-testid*="unretweet"]');
            if (retweetBtn) retweetBtn.click();
        };

        window.quickReply = (btn) => {
            const tweet = btn.closest('[data-testid="tweet"], article');
            const replyBtn = tweet.querySelector('[data-testid*="reply"]');
            if (replyBtn) replyBtn.click();
        };
    }

    function addReadingTime(tweet) {
        const textElement = tweet.querySelector('[data-testid="Tweet-User-Text"] p, [lang]');
        if (!textElement || tweet.querySelector('.reading-time')) return;

        const wordCount = textElement.textContent.split(' ').length;
        const readingTime = Math.ceil(wordCount / 200); // 假设每分钟阅读200个单词

        const timeBadge = document.createElement('span');
        timeBadge.className = 'reading-time';
        timeBadge.textContent = `${readingTime}分钟阅读`;
        timeBadge.style.cssText = `
            position: absolute; top: 8px; right: 8px;
            background: rgba(0,0,0,0.6); color: white;
            padding: 2px 6px; border-radius: 3px; font-size: 10px;
        `;

        tweet.style.position = 'relative';
        tweet.appendChild(timeBadge);
    }

    function addContentScore(tweet) {
        if (tweet.querySelector('.content-score')) return;

        const metrics = getTweetMetrics(tweet);
        const score = calculateContentScore(metrics);

        const scoreBadge = document.createElement('span');
        scoreBadge.className = 'content-score';
        scoreBadge.textContent = `质量: ${score}分`;
        scoreBadge.style.cssText = `
            position: absolute; top: 8px; left: 8px;
            background: ${score > 80 ? '#28a745' : score > 60 ? '#ffc107' : '#dc3545'};
            color: white; padding: 2px 6px; border-radius: 3px; font-size: 10px;
        `;

        tweet.style.position = 'relative';
        tweet.appendChild(scoreBadge);
    }

    function calculateContentScore(metrics) {
        let score = 50;

        // 互动评分
        score += Math.min(metrics.likes / 10, 20);
        score += Math.min(metrics.retweets / 5, 15);
        score += Math.min(metrics.replies / 2, 10);

        // 浏览量评分
        score += Math.min(metrics.views / 1000, 5);

        return Math.min(100, Math.max(0, Math.round(score)));
    }

    // 界面增强
    function enhanceUI() {
        if (!config.interactionEnhance) return;

        GM_addStyle(`
            /* Twitter界面增强 */
            [data-testid="primaryColumn"] {
                border-radius: 12px !important;
                background: #ffffff !important;
                box-shadow: 0 2px 16px rgba(0,0,0,0.1) !important;
            }

            /* 优化搜索框 */
            ${selectors.searchInput} {
                border-radius: 25px !important;
                border: 2px solid #e1e8ed !important;
                padding: 12px 20px !important;
                font-size: 16px !important;
                transition: all 0.2s ease !important;
            }

            ${selectors.searchInput}:focus {
                border-color: #1DA1F2 !important;
                box-shadow: 0 0 0 3px rgba(29, 161, 242, 0.1) !important;
            }

            /* 优化按钮样式 */
            [role="button"], button {
                border-radius: 8px !important;
                transition: all 0.2s ease !important;
            }

            [role="button"]:hover, button:hover {
                transform: translateY(-1px) !important;
                box-shadow: 0 2px 8px rgba(0,0,0,0.2) !important;
            }

            /* 优化导航栏 */
            [data-testid="AppTabBar"] {
                background: linear-gradient(135deg, #1DA1F2, #42A5F5) !important;
                border-radius: 0 0 12px 12px !important;
                box-shadow: 0 2px 16px rgba(0,0,0,0.1) !important;
            }

            /* 优化用户信息卡片 */
            [data-testid="UserCell"] {
                border-radius: 12px !important;
                margin-bottom: 8px !important;
                box-shadow: 0 1px 4px rgba(0,0,0,0.1) !important;
                transition: transform 0.2s ease !important;
            }

            [data-testid="UserCell"]:hover {
                transform: translateY(-2px) !important;
            }
        `);

        // 添加悬停效果
        addHoverEffects();

        // 优化侧边栏
        optimizeSidebar();

        showNotification('界面已增强！', 'success');
    }

    function addHoverEffects() {
        const interactiveElements = document.querySelectorAll('a, button, [role="button"]');
        interactiveElements.forEach(element => {
            if (element.dataset.hoverEnhanced) return;
            element.dataset.hoverEnhanced = 'true';

            element.addEventListener('mouseenter', function() {
                this.style.transform = 'scale(1.02)';
            });

            element.addEventListener('mouseleave', function() {
                this.style.transform = 'scale(1)';
            });
        });
    }

    function optimizeSidebar() {
        const sidebar = document.querySelector(selectors.sidebar);
        if (sidebar) {
            sidebar.style.borderRadius = '12px';
            sidebar.style.boxShadow = '0 2px 16px rgba(0,0,0,0.1)';
            sidebar.style.background = '#ffffff';
        }
    }

    // 自动滚动
    let autoScrollInterval = null;
    function toggleAutoScroll() {
        config.autoScroll = !config.autoScroll;
        GM_setValue('twitterAutoScroll', config.autoScroll);

        if (config.autoScroll) {
            startAutoScroll();
            showNotification('自动滚动已启用', 'info');
        } else {
            stopAutoScroll();
            showNotification('自动滚动已停止', 'info');
        }
    }

    function startAutoScroll() {
        if (autoScrollInterval) clearInterval(autoScrollInterval);

        autoScrollInterval = setInterval(() => {
            window.scrollBy(0, 2); // 缓慢滚动
        }, 100);
    }

    function stopAutoScroll() {
        if (autoScrollInterval) {
            clearInterval(autoScrollInterval);
            autoScrollInterval = null;
        }
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        if (!config.keyboardShortcuts) return;

        document.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            switch (e.keyCode) {
                case 76: // L - 点赞当前推文
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        likeCurrentTweet();
                    }
                    break;
                case 82: // R - 转发当前推文
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        retweetCurrentTweet();
                    }
                    break;
                case 67: // C - 回复当前推文
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        replyToCurrentTweet();
                    }
                    break;
                case 83: // S - 聚焦搜索框
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        focusSearch();
                    }
                    break;
                case 70: // F - 刷新时间线
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        refreshTimeline();
                    }
                    break;
                case 78: // N - 新推文
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        composeNewTweet();
                    }
                    break;
            }
        });
    }

    function likeCurrentTweet() {
        const tweets = document.querySelectorAll(selectors.tweet);
        if (tweets.length > 0) {
            const firstVisibleTweet = Array.from(tweets).find(tweet => {
                const rect = tweet.getBoundingClientRect();
                return rect.top >= 0 && rect.bottom <= window.innerHeight;
            });

            if (firstVisibleTweet) {
                const likeBtn = firstVisibleTweet.querySelector(selectors.likeBtn);
                if (likeBtn) likeBtn.click();
            }
        }
    }

    function retweetCurrentTweet() {
        const tweets = document.querySelectorAll(selectors.tweet);
        if (tweets.length > 0) {
            const firstVisibleTweet = Array.from(tweets).find(tweet => {
                const rect = tweet.getBoundingClientRect();
                return rect.top >= 0 && rect.bottom <= window.innerHeight;
            });

            if (firstVisibleTweet) {
                const retweetBtn = firstVisibleTweet.querySelector(selectors.retweetBtn);
                if (retweetBtn) retweetBtn.click();
            }
        }
    }

    function replyToCurrentTweet() {
        const tweets = document.querySelectorAll(selectors.tweet);
        if (tweets.length > 0) {
            const firstVisibleTweet = Array.from(tweets).find(tweet => {
                const rect = tweet.getBoundingClientRect();
                return rect.top >= 0 && rect.bottom <= window.innerHeight;
            });

            if (firstVisibleTweet) {
                const replyBtn = firstVisibleTweet.querySelector(selectors.replyBtn);
                if (replyBtn) replyBtn.click();
            }
        }
    }

    function focusSearch() {
        const searchInput = document.querySelector(selectors.searchInput);
        if (searchInput) {
            searchInput.focus();
            searchInput.select();
        }
    }

    function refreshTimeline() {
        location.reload();
    }

    function composeNewTweet() {
        const composeBtn = document.querySelector('[data-testid="SideNav_NewTweet_Button"], [data-testid="tweetTextarea"]');
        if (composeBtn) {
            composeBtn.click();
        }
    }

    // 设置面板
    function showTwitterSettings() {
        const settings = document.createElement('div');
        settings.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                        box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 500px;">
                <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">Twitter/X 增强器设置</h3>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-enabled" style="margin-right: 8px;" ${config.enabled ? 'checked' : ''}>
                        启用增强器
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-keyboard" style="margin-right: 8px;" ${config.keyboardShortcuts ? 'checked' : ''}>
                        键盘快捷键
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-ads" style="margin-right: 8px;" ${config.adBlock ? 'checked' : ''}>
                        广告拦截
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-filter" style="margin-right: 8px;" ${config.algorithmFilter ? 'checked' : ''}>
                        算法过滤
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-timeline" style="margin-right: 8px;" ${config.timelineOptimize ? 'checked' : ''}>
                        时间线优化
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-ui" style="margin-right: 8px;" ${config.interactionEnhance ? 'checked' : ''}>
                        界面增强
                    </label>
                </div>

                <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                    <h4 style="margin: 0 0 10px 0; color: #666;">高级选项</h4>
                    <label style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" id="setting-theme" style="margin-right: 8px;" ${config.themeEnhance ? 'checked' : ''}>
                        深色主题增强
                    </label>
                    <label style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" id="setting-autoscroll" style="margin-right: 8px;" ${config.autoScroll ? 'checked' : ''}>
                        自动滚动
                    </label>
                    <label style="display: block;">
                        <input type="checkbox" id="setting-content" style="margin-right: 8px;" ${config.contentFilter ? 'checked' : ''}>
                        内容过滤
                    </label>
                </div>

                <div style="text-align: right;">
                    <button id="save-twitter-settings" style="padding: 10px 20px;
                                                    background: linear-gradient(135deg, #1DA1F2, #42A5F5);
                                                    color: white; border: none; border-radius: 6px;
                                                    cursor: pointer; margin-right: 10px;">
                        保存设置
                    </button>
                    <button id="close-twitter-settings" style="padding: 10px 20px;
                                                     background: #6c757d; color: white; border: none;
                                                     border-radius: 6px; cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(settings);

        document.getElementById('save-twitter-settings').onclick = saveTwitterSettings;
        document.getElementById('close-twitter-settings').onclick = () => settings.remove();
    }

    function saveTwitterSettings() {
        config.enabled = document.getElementById('setting-enabled').checked;
        config.keyboardShortcuts = document.getElementById('setting-keyboard').checked;
        config.adBlock = document.getElementById('setting-ads').checked;
        config.algorithmFilter = document.getElementById('setting-filter').checked;
        config.timelineOptimize = document.getElementById('setting-timeline').checked;
        config.interactionEnhance = document.getElementById('setting-ui').checked;
        config.themeEnhance = document.getElementById('setting-theme').checked;
        config.autoScroll = document.getElementById('setting-autoscroll').checked;
        config.contentFilter = document.getElementById('setting-content').checked;

        // 保存设置
        Object.keys(config).forEach(key => {
            GM_setValue('twitter' + key.charAt(0).toUpperCase() + key.slice(1), config[key]);
        });

        location.reload();
    }

    // 通知系统
    function showNotification(message, type = 'info') {
        const colors = {
            success: '#28a745',
            error: '#dc3545',
            warning: '#ffc107',
            info: '#17a2b8'
        };

        const notification = document.createElement('div');
        notification.innerHTML = `
            <div style="position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
                        background: ${colors[type]}; color: white; padding: 12px 24px;
                        border-radius: 8px; z-index: 10001; box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                        font-size: 14px; font-weight: bold;">
                ${message}
            </div>
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    // 初始化
    function init() {
        if (!config.enabled) {
            GM_log('Twitter/X 增强器已禁用');
            return;
        }

        setTimeout(() => {
            createTwitterPanel();
            setupKeyboardShortcuts();

            // 根据配置自动应用功能
            if (config.adBlock) setTimeout(() => blockTwitterAds(), 1000);
            if (config.algorithmFilter) setTimeout(() => filterAlgorithmContent(), 1500);
            if (config.timelineOptimize) setTimeout(() => optimizeTimeline(), 2000);
            if (config.interactionEnhance) setTimeout(() => enhanceUI(), 2500);
            if (config.autoScroll) setTimeout(() => startAutoScroll(), 3000);

            GM_log('EhViewer Twitter/X 增强器初始化完成');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
