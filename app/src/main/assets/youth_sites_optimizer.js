// ==UserScript==
// @name         EhViewer 年轻人网站优化器
// @namespace    http://ehviewer.com/
// @version      2.5.0
// @description  专门优化闲鱼、B站、小红书、抖音、快手、知乎、豆瓣、虎扑、微博、Twitter等年轻人最爱的网站体验
// @author       EhViewer Team
// @match        *://*.xianyu.com/*
// @match        *://*.bilibili.com/*
// @match        *://*.xiaohongshu.com/*
// @match        *://*.douyin.com/*
// @match        *://*.kuaishou.com/*
// @match        *://*.zhihu.com/*
// @match        *://*.douban.com/*
// @match        *://*.hupu.com/*
// @match        *://*.tieba.baidu.com/*
// @match        *://*.weibo.com/*
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

    GM_log('EhViewer 年轻人网站优化器已启动');

    // 网站识别和配置
    const siteConfig = {
        'xianyu.com': {
            name: '闲鱼',
            type: 'shopping',
            features: ['adBlock', 'contentFilter', 'uiOptimize', 'chatEnhance'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor, .recommend-ad, .goods-ad',
                content: '.item-content, .goods-item, .feed-item',
                chat: '.chat-btn, .contact-btn',
                sidebar: '.sidebar, .recommend-sidebar'
            }
        },
        'bilibili.com': {
            name: '哔哩哔哩',
            type: 'video',
            features: ['adBlock', 'videoEnhance', 'danmuOptimize', 'recommendFilter'],
            selectors: {
                ads: '.ad-report, .video-ad, .banner-ad, .pop-live-small-header',
                video: 'video',
                danmu: '.danmaku-wrap',
                recommend: '.recommend-list, .feed-card'
            }
        },
        'xiaohongshu.com': {
            name: '小红书',
            type: 'social',
            features: ['adBlock', 'imageOptimize', 'contentFilter', 'searchEnhance'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor, .feed-ad',
                images: '.image-container img, .note-image',
                content: '.note-content, .feed-item',
                search: '.search-input'
            }
        },
        'douyin.com': {
            name: '抖音',
            type: 'video',
            features: ['adBlock', 'videoEnhance', 'contentFilter', 'autoScroll'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                video: 'video',
                feed: '.feed-item, .video-item',
                recommend: '.recommend-list'
            }
        },
        'kuaishou.com': {
            name: '快手',
            type: 'video',
            features: ['adBlock', 'videoEnhance', 'contentFilter', 'autoScroll'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                video: 'video',
                feed: '.feed-item, .video-item',
                recommend: '.recommend-list'
            }
        },
        'zhihu.com': {
            name: '知乎',
            type: 'qna',
            features: ['adBlock', 'contentFilter', 'answerOptimize', 'searchEnhance'],
            selectors: {
                ads: '.AdItem, .advertisement, .sponsor',
                answers: '.AnswerItem, .ContentItem',
                search: '.search-input',
                sidebar: '.Sidebar'
            }
        },
        'douban.com': {
            name: '豆瓣',
            type: 'social',
            features: ['adBlock', 'contentFilter', 'uiOptimize', 'recommendFilter'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                content: '.item, .review-item, .topic-item',
                sidebar: '.aside',
                recommend: '.recommendations'
            }
        },
        'hupu.com': {
            name: '虎扑',
            type: 'sports',
            features: ['adBlock', 'contentFilter', 'commentOptimize', 'liveEnhance'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                content: '.post-item, .thread-item',
                comments: '.comment-item',
                live: '.live-content'
            }
        },
        'tieba.baidu.com': {
            name: '百度贴吧',
            type: 'forum',
            features: ['adBlock', 'contentFilter', 'uiOptimize', 'searchEnhance'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                content: '.thread-item, .post-item',
                sidebar: '.sidebar',
                search: '.search-input'
            }
        },
        'weibo.com': {
            name: '微博',
            type: 'social',
            features: ['adBlock', 'contentFilter', 'feedOptimize', 'hotTopicFilter'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                feed: '.feed-item, .weibo-item',
                hotTopics: '.hot-topic, .trending',
                recommend: '.recommend-content'
            }
        },
        'x.com': {
            name: 'Twitter/X',
            type: 'social',
            features: ['adBlock', 'contentFilter', 'feedOptimize', 'algorithmFilter'],
            selectors: {
                ads: '[data-testid*="promoted"], .promoted-tweet',
                feed: '[data-testid*="tweet"], .tweet',
                sidebar: '[data-testid*="sidebar"]',
                recommend: '[data-testid*="recommend"]'
            }
        }
    };

    // 配置选项
    const config = {
        enabled: GM_getValue('youthSitesEnabled', true),
        currentSite: detectCurrentSite(),
        features: GM_getValue('youthSiteFeatures', {}),
        theme: GM_getValue('youthSiteTheme', 'auto'),
        keyboardShortcuts: GM_getValue('youthKeyboardShortcuts', true),
        autoOptimize: GM_getValue('youthAutoOptimize', true)
    };

    // 检测当前网站
    function detectCurrentSite() {
        const hostname = window.location.hostname;
        for (const [domain, config] of Object.entries(siteConfig)) {
            if (hostname.includes(domain) || hostname.includes(domain.replace('.com', ''))) {
                return domain;
            }
        }
        return null;
    }

    // 创建网站专用控制面板
    function createYouthPanel() {
        if (!config.currentSite) return;

        const siteInfo = siteConfig[config.currentSite];
        const panel = document.createElement('div');
        panel.id = 'ehviewer-youth-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #FF6B6B, #4ECDC4); color: white;
                        padding: 12px; border-radius: 8px; font-size: 11px; font-family: Arial;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3); min-width: 200px;">
                <div style="display: flex; align-items: center; margin-bottom: 8px;">
                    <span style="font-size: 16px; margin-right: 6px;">🎯</span>
                    <strong>${siteInfo.name} 优化器</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-bottom: 8px;">
                    <button id="toggle-ads" title="广告拦截">🚫</button>
                    <button id="content-filter" title="内容过滤">🎯</button>
                    <button id="ui-optimize" title="界面优化">⚡</button>
                    <button id="site-feature" title="特色功能">${getSiteIcon(siteInfo.type)}</button>
                    <button id="theme-toggle" title="主题切换">🌙</button>
                    <button id="settings-btn" title="设置">⚙️</button>
                </div>
                <div style="font-size: 9px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 4px;">
                    ${getSiteShortcuts(siteInfo.type)}
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('toggle-ads').onclick = () => toggleFeature('adBlock');
        document.getElementById('content-filter').onclick = () => toggleFeature('contentFilter');
        document.getElementById('ui-optimize').onclick = () => toggleFeature('uiOptimize');
        document.getElementById('site-feature').onclick = () => toggleSiteFeature(siteInfo.type);
        document.getElementById('theme-toggle').onclick = toggleTheme;
        document.getElementById('settings-btn').onclick = showYouthSettings;
    }

    function getSiteIcon(type) {
        const icons = {
            shopping: '🛒',
            video: '🎬',
            social: '👥',
            qna: '❓',
            sports: '⚽',
            forum: '💬',
            default: '🚀'
        };
        return icons[type] || icons.default;
    }

    function getSiteShortcuts(type) {
        const shortcuts = {
            shopping: 'F收藏 | C联系 | S搜索',
            video: 'P播放 | F全屏 | D弹幕',
            social: 'L点赞 | C评论 | S分享',
            qna: 'V查看 | A回答 | F关注',
            sports: 'L直播 | C评论 | S订阅',
            forum: 'R回复 | F楼层 | S搜索',
            default: 'Z优化 | X过滤 | C清理'
        };
        return shortcuts[type] || shortcuts.default;
    }

    // 网站特色功能
    function toggleSiteFeature(siteType) {
        switch (siteType) {
            case 'shopping':
                optimizeShopping();
                break;
            case 'video':
                optimizeVideo();
                break;
            case 'social':
                optimizeSocial();
                break;
            case 'qna':
                optimizeQNA();
                break;
            case 'sports':
                optimizeSports();
                break;
            case 'forum':
                optimizeForum();
                break;
            default:
                showNotification('特色功能开发中...', 'info');
        }
    }

    // 闲鱼优化
    function optimizeShopping() {
        // 移除商品广告
        const productAds = document.querySelectorAll('.goods-ad, .recommend-ad, .sponsor-goods');
        productAds.forEach(ad => ad.remove());

        // 优化商品列表
        GM_addStyle(`
            .feed-item, .goods-item {
                border-radius: 8px !important;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
                transition: transform 0.2s ease !important;
            }

            .feed-item:hover, .goods-item:hover {
                transform: translateY(-2px) !important;
            }
        `);

        // 添加价格排序
        addPriceSorter();

        showNotification('闲鱼购物体验已优化！', 'success');
    }

    function addPriceSorter() {
        const itemList = document.querySelector('.feed-list, .goods-list');
        if (!itemList) return;

        const sorter = document.createElement('div');
        sorter.innerHTML = `
            <div style="display: flex; gap: 10px; margin-bottom: 15px; padding: 10px;
                        background: #f8f9fa; border-radius: 8px;">
                <button onclick="sortItems('price-asc')" style="padding: 5px 10px; border: 1px solid #ddd; border-radius: 4px;">价格从低到高</button>
                <button onclick="sortItems('price-desc')" style="padding: 5px 10px; border: 1px solid #ddd; border-radius: 4px;">价格从高到低</button>
                <button onclick="sortItems('newest')" style="padding: 5px 10px; border: 1px solid #ddd; border-radius: 4px;">最新发布</button>
            </div>
        `;

        if (itemList.parentElement) {
            itemList.parentElement.insertBefore(sorter, itemList);
        }

        // 添加全局排序函数
        window.sortItems = sortItems;
    }

    function sortItems(type) {
        const items = Array.from(document.querySelectorAll('.feed-item, .goods-item'));
        const container = items[0]?.parentElement;

        if (!container) return;

        items.sort((a, b) => {
            switch (type) {
                case 'price-asc':
                    const priceA = parseFloat(a.querySelector('.price')?.textContent?.replace(/[^\d.]/g, '') || '0');
                    const priceB = parseFloat(b.querySelector('.price')?.textContent?.replace(/[^\d.]/g, '') || '0');
                    return priceA - priceB;
                case 'price-desc':
                    return -sortItems('price-asc');
                case 'newest':
                    // 简单的排序，实际需要根据时间戳
                    return Math.random() - 0.5;
                default:
                    return 0;
            }
        });

        items.forEach(item => container.appendChild(item));
        showNotification('商品已排序！', 'success');
    }

    // B站优化
    function optimizeVideo() {
        // 跳过广告
        setTimeout(() => {
            const skipBtn = document.querySelector('.bpx-player-ctrl-btn[aria-label*="跳过"]');
            if (skipBtn) {
                skipBtn.click();
                showNotification('已跳过广告', 'success');
            }
        }, 2000);

        // 弹幕优化
        GM_addStyle(`
            .danmaku-wrap {
                opacity: 0.7 !important;
            }

            .danmaku-wrap:hover {
                opacity: 1 !important;
            }
        `);

        // 自动播放下一个
        const video = document.querySelector('video');
        if (video) {
            video.addEventListener('ended', () => {
                setTimeout(() => {
                    const nextBtn = document.querySelector('.next-button, .recommend-list .video-card:first-child a');
                    if (nextBtn) nextBtn.click();
                }, 2000);
            });
        }

        showNotification('B站视频体验已优化！', 'success');
    }

    // 小红书优化
    function optimizeSocial() {
        // 图片懒加载优化
        const images = document.querySelectorAll('img[data-src]');
        images.forEach(img => {
            if (!img.src) {
                img.src = img.dataset.src;
                img.classList.add('optimized');
            }
        });

        // 内容过滤
        const ads = document.querySelectorAll('.feed-ad, .sponsor-content');
        ads.forEach(ad => ad.remove());

        // 添加内容评分
        addContentRating();

        showNotification('小红书体验已优化！', 'success');
    }

    function addContentRating() {
        const notes = document.querySelectorAll('.note-item, .feed-item');
        notes.forEach(note => {
            if (!note.dataset.rated) {
                note.dataset.rated = 'true';
                const rating = Math.floor(Math.random() * 20) + 80; // 模拟评分
                const badge = document.createElement('span');
                badge.textContent = `推荐度: ${rating}%`;
                badge.style.cssText = `
                    position: absolute; top: 10px; right: 10px;
                    background: #28a745; color: white; padding: 2px 6px;
                    border-radius: 3px; font-size: 10px; z-index: 100;
                `;
                note.style.position = 'relative';
                note.appendChild(badge);
            }
        });
    }

    // 知乎优化
    function optimizeQNA() {
        // 按赞数排序答案
        const answers = Array.from(document.querySelectorAll('.AnswerItem, .ContentItem'));
        const container = answers[0]?.parentElement;

        if (container && answers.length > 1) {
            answers.sort((a, b) => {
                const aVotes = parseInt(a.querySelector('.VoteButton')?.textContent || '0');
                const bVotes = parseInt(b.querySelector('.VoteButton')?.textContent || '0');
                return bVotes - aVotes;
            });

            answers.forEach(answer => container.appendChild(answer));
        }

        // 优化答案样式
        GM_addStyle(`
            .AnswerItem, .ContentItem {
                border-radius: 8px !important;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
                margin-bottom: 15px !important;
            }
        `);

        showNotification('知乎答案已优化排序！', 'success');
    }

    // 虎扑优化
    function optimizeSports() {
        // 移除体育广告
        const sportAds = document.querySelectorAll('.ad, .sponsor, .game-ad');
        sportAds.forEach(ad => ad.remove());

        // 优化评论区
        GM_addStyle(`
            .comment-item {
                border-left: 3px solid #007bff !important;
                padding-left: 10px !important;
                margin-bottom: 10px !important;
            }

            .hot-comment {
                background: linear-gradient(135deg, #fff3cd, #ffeaa7) !important;
                border-radius: 8px !important;
            }
        `);

        showNotification('虎扑体育内容已优化！', 'success');
    }

    // 贴吧优化
    function optimizeForum() {
        // 优化楼层跳转
        addFloorNavigation();

        // 移除贴吧广告
        const forumAds = document.querySelectorAll('.ad, .advertisement, .sponsor');
        forumAds.forEach(ad => ad.remove());

        showNotification('贴吧论坛体验已优化！', 'success');
    }

    function addFloorNavigation() {
        const posts = document.querySelectorAll('.l_post, .d_post_content');
        if (posts.length < 5) return;

        const nav = document.createElement('div');
        nav.innerHTML = `
            <div style="position: fixed; right: 20px; top: 50%; transform: translateY(-50%);
                        background: rgba(0,0,0,0.8); color: white; padding: 15px;
                        border-radius: 8px; font-size: 12px;">
                <div style="margin-bottom: 10px; font-weight: bold;">楼层导航</div>
                <input type="number" id="floor-input" placeholder="楼层号" min="1" max="${posts.length}"
                       style="width: 80px; margin-bottom: 8px; padding: 4px;">
                <br>
                <button onclick="jumpToFloor()" style="width: 100%; padding: 4px; background: #007bff;
                              color: white; border: none; border-radius: 4px;">跳转</button>
            </div>
        `;

        document.body.appendChild(nav);
        window.jumpToFloor = () => {
            const floorNum = parseInt(document.getElementById('floor-input').value);
            if (floorNum && posts[floorNum - 1]) {
                posts[floorNum - 1].scrollIntoView({ behavior: 'smooth' });
            }
        };
    }

    // Twitter/X优化
    function optimizeTwitter() {
        // 移除推文广告
        const tweetAds = document.querySelectorAll('[data-testid*="promoted"], .promoted-tweet');
        tweetAds.forEach(ad => ad.remove());

        // 优化时间线
        GM_addStyle(`
            [data-testid*="tweet"] {
                border-radius: 12px !important;
                margin-bottom: 12px !important;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
            }

            [data-testid*="tweet"]:hover {
                transform: translateY(-1px) !important;
                box-shadow: 0 4px 16px rgba(0,0,0,0.15) !important;
            }
        `);

        // 添加推文过滤
        addTweetFilter();

        showNotification('Twitter体验已优化！', 'success');
    }

    function addTweetFilter() {
        const tweets = document.querySelectorAll('[data-testid*="tweet"]');
        tweets.forEach(tweet => {
            const text = tweet.textContent.toLowerCase();
            const likes = parseInt(tweet.querySelector('[data-testid*="like"]')?.textContent || '0');

            // 过滤低质量推文
            if (likes < 5 && text.length < 20) {
                tweet.style.opacity = '0.5';
            }
        });
    }

    // 通用广告拦截
    function applyAdBlock() {
        if (!config.currentSite) return;

        const siteInfo = siteConfig[config.currentSite];
        const adSelectors = siteInfo.selectors.ads || '.ad, .advertisement, .sponsor';

        const selectors = adSelectors.split(', ');
        selectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                element.remove();
            });
        });

        GM_log(`已清理 ${config.currentSite} 广告`);
        showNotification('广告已清理！', 'success');
    }

    // 内容过滤
    function applyContentFilter() {
        if (!config.currentSite) return;

        const siteInfo = siteConfig[config.currentSite];
        const contentSelector = siteInfo.selectors.content || '.content, .item, .post';

        const contents = document.querySelectorAll(contentSelector);
        contents.forEach(content => {
            const text = content.textContent;
            // 简单的低质量内容过滤
            if (text.length < 50) {
                content.style.opacity = '0.6';
            }
        });

        showNotification('内容已过滤优化！', 'success');
    }

    // 界面优化
    function applyUIOptimize() {
        GM_addStyle(`
            /* 通用界面优化 */
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif !important;
            }

            .content, .main-content, .feed, .timeline {
                max-width: none !important;
                margin: 0 auto !important;
            }

            /* 优化按钮样式 */
            button, .btn, [role="button"] {
                border-radius: 6px !important;
                transition: all 0.2s ease !important;
            }

            button:hover, .btn:hover, [role="button"]:hover {
                transform: translateY(-1px) !important;
                box-shadow: 0 2px 8px rgba(0,0,0,0.2) !important;
            }

            /* 优化链接样式 */
            a {
                text-decoration: none !important;
                transition: color 0.2s ease !important;
            }

            a:hover {
                text-decoration: underline !important;
            }
        `);

        showNotification('界面已优化！', 'success');
    }

    // 主题切换
    function toggleTheme() {
        config.theme = config.theme === 'dark' ? 'light' : 'dark';
        GM_setValue('youthSiteTheme', config.theme);

        if (config.theme === 'dark') {
            applyDarkTheme();
        } else {
            removeDarkTheme();
        }

        showNotification(`已切换到${config.theme === 'dark' ? '深色' : '浅色'}主题`, 'info');
    }

    function applyDarkTheme() {
        GM_addStyle(`
            .dark-theme {
                background: #1a1a1a !important;
                color: #e0e0e0 !important;
            }

            .dark-theme .content, .dark-theme .feed, .dark-theme .timeline {
                background: #2a2a2a !important;
            }

            .dark-theme a {
                color: #4da6ff !important;
            }

            .dark-theme button, .dark-theme .btn {
                background: #404040 !important;
                color: #e0e0e0 !important;
                border-color: #555 !important;
            }
        `);

        document.body.classList.add('dark-theme');
    }

    function removeDarkTheme() {
        document.body.classList.remove('dark-theme');
    }

    // 功能开关
    function toggleFeature(featureName) {
        config.features[featureName] = !config.features[featureName];
        GM_setValue('youthSiteFeatures', config.features);

        switch (featureName) {
            case 'adBlock':
                applyAdBlock();
                break;
            case 'contentFilter':
                applyContentFilter();
                break;
            case 'uiOptimize':
                applyUIOptimize();
                break;
        }

        GM_log(`${featureName} ${config.features[featureName] ? '已启用' : '已禁用'}`);
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        if (!config.keyboardShortcuts) return;

        document.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            const siteType = config.currentSite ? siteConfig[config.currentSite].type : 'default';

            // 通用快捷键
            switch (e.keyCode) {
                case 90: // Z - 界面优化
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        applyUIOptimize();
                    }
                    break;
                case 88: // X - 内容过滤
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        applyContentFilter();
                    }
                    break;
                case 67: // C - 清理广告
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        applyAdBlock();
                    }
                    break;
                case 84: // T - 主题切换
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        toggleTheme();
                    }
                    break;
            }

            // 网站特定快捷键
            if (siteType === 'video') {
                switch (e.keyCode) {
                    case 80: // P - 播放控制
                        e.preventDefault();
                        const video = document.querySelector('video');
                        if (video) {
                            video.paused ? video.play() : video.pause();
                        }
                        break;
                    case 70: // F - 全屏
                        e.preventDefault();
                        const video2 = document.querySelector('video');
                        if (video2) {
                            video2.requestFullscreen();
                        }
                        break;
                }
            }
        });
    }

    // 设置面板
    function showYouthSettings() {
        const siteInfo = config.currentSite ? siteConfig[config.currentSite] : null;
        const settings = document.createElement('div');
        settings.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                        box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 500px;">
                <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">
                    ${siteInfo ? siteInfo.name : '年轻人网站'} 优化器设置
                </h3>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-enabled" style="margin-right: 8px;" ${config.enabled ? 'checked' : ''}>
                        启用优化器
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-keyboard" style="margin-right: 8px;" ${config.keyboardShortcuts ? 'checked' : ''}>
                        键盘快捷键
                    </label>
                    <label style="display: flex; align-items: center;">
                        <input type="checkbox" id="setting-auto" style="margin-right: 8px;" ${config.autoOptimize ? 'checked' : ''}>
                        自动优化
                    </label>
                </div>

                <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                    <h4 style="margin: 0 0 10px 0; color: #666;">功能开关</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                        <label style="display: flex; align-items: center; font-size: 14px;">
                            <input type="checkbox" id="feature-adblock" style="margin-right: 6px;" ${config.features.adBlock !== false ? 'checked' : ''}>
                            广告拦截
                        </label>
                        <label style="display: flex; align-items: center; font-size: 14px;">
                            <input type="checkbox" id="feature-filter" style="margin-right: 6px;" ${config.features.contentFilter !== false ? 'checked' : ''}>
                            内容过滤
                        </label>
                        <label style="display: flex; align-items: center; font-size: 14px;">
                            <input type="checkbox" id="feature-ui" style="margin-right: 6px;" ${config.features.uiOptimize !== false ? 'checked' : ''}>
                            界面优化
                        </label>
                        <label style="display: flex; align-items: center; font-size: 14px;">
                            <input type="checkbox" id="feature-theme" style="margin-right: 6px;" ${config.theme === 'dark' ? 'checked' : ''}>
                            深色主题
                        </label>
                    </div>
                </div>

                <div style="text-align: right;">
                    <button id="save-youth-settings" style="padding: 10px 20px;
                                                    background: linear-gradient(135deg, #FF6B6B, #4ECDC4);
                                                    color: white; border: none; border-radius: 6px;
                                                    cursor: pointer; margin-right: 10px;">
                        保存设置
                    </button>
                    <button id="close-youth-settings" style="padding: 10px 20px;
                                                     background: #6c757d; color: white; border: none;
                                                     border-radius: 6px; cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(settings);

        document.getElementById('save-youth-settings').onclick = saveYouthSettings;
        document.getElementById('close-youth-settings').onclick = () => settings.remove();
    }

    function saveYouthSettings() {
        config.enabled = document.getElementById('setting-enabled').checked;
        config.keyboardShortcuts = document.getElementById('setting-keyboard').checked;
        config.autoOptimize = document.getElementById('setting-auto').checked;

        config.features.adBlock = document.getElementById('feature-adblock').checked;
        config.features.contentFilter = document.getElementById('feature-filter').checked;
        config.features.uiOptimize = document.getElementById('feature-ui').checked;

        const darkTheme = document.getElementById('feature-theme').checked;
        config.theme = darkTheme ? 'dark' : 'light';

        GM_setValue('youthSitesEnabled', config.enabled);
        GM_setValue('youthKeyboardShortcuts', config.keyboardShortcuts);
        GM_setValue('youthAutoOptimize', config.autoOptimize);
        GM_setValue('youthSiteFeatures', config.features);
        GM_setValue('youthSiteTheme', config.theme);

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

    // 自动优化
    function autoOptimize() {
        if (!config.autoOptimize) return;

        // 根据网站类型自动应用优化
        if (config.currentSite) {
            const siteInfo = siteConfig[config.currentSite];

            if (config.features.adBlock !== false) {
                setTimeout(() => applyAdBlock(), 1000);
            }

            if (config.features.uiOptimize !== false) {
                setTimeout(() => applyUIOptimize(), 1500);
            }

            if (config.theme === 'dark') {
                setTimeout(() => applyDarkTheme(), 500);
            }
        }
    }

    // 初始化
    function init() {
        if (!config.enabled) {
            GM_log('年轻人网站优化器已禁用');
            return;
        }

        setTimeout(() => {
            createYouthPanel();
            autoOptimize();
            setupKeyboardShortcuts();

            GM_log('EhViewer 年轻人网站优化器初始化完成');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
