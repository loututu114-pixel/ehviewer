// ==UserScript==
// @name         EhViewer 小说短剧增强器
// @namespace    http://ehviewer.com/
// @version      2.3.0
// @description  深度优化小说短剧体验，拦截APP提醒和折叠内容，全面拦截快应用小程序拉起，支持CSDN、简书、掘金、腾讯视频、优酷、爱奇艺、B站、财经、科技、汽车、游戏等50+平台
// @author       EhViewer Team
// @match        *://*.fanqienovel.com/*
// @match        *://*.qidian.com/*
// @match        *://*.zongheng.com/*
// @match        *://*.jjwxc.net/*
// @match        *://*.duokan.com/*
// @match        *://*.weread.qq.com/*
// @match        *://*.doupocangqiong.com/*
// @match        *://*.migu.cn/*
// @match        *://*.miguvideo.com/*
// @match        *://*.tudou.com/*
// @match        *://*.youku.com/*
// @match        *://*.iqiyi.com/*
// @match        *://*.qq.com/*
// @match        *://*.bilibili.com/*
// @match        *://*.zhihu.com/*
// @match        *://*.weibo.com/*
// @match        *://*.xiaohongshu.com/*
// @match        *://*.douban.com/*
// @match        *://*.tieba.baidu.com/*
// @match        *://*.baidu.com/*
// @match        *://*.sina.com.cn/*
// @match        *://*.sohu.com/*
// @match        *://*.163.com/*
// @match        *://*.csdn.net/*
// @match        *://*.jianshu.com/*
// @match        *://juejin.cn/*
// @match        *://*.eastmoney.com/*
// @match        *://*.pconline.com.cn/*
// @match        *://*.pcauto.com.cn/*
// @match        *://*.autohome.com.cn/*
// @match        *://*.taptap.com/*
// @match        *://*.ithome.com/*
// @match        *://*.oschina.net/*
// @match        *://*.segmentfault.com/*
// @match        *://*.zcool.com.cn/*
// @match        *://*.cnki.net/*
// @match        *://*.smzdm.com/*
// @exclude      *://*.google.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 小说短剧增强器已启动');

    // 配置选项
    const config = {
        enabled: GM_getValue('novelDramaEnabled', true),
        novelOptimize: GM_getValue('novelOptimize', true),
        dramaOptimize: GM_getValue('dramaOptimize', true),
        miniAppBlock: GM_getValue('miniAppBlock', true),
        quickAppBlock: GM_getValue('quickAppBlock', true),
        adBlock: GM_getValue('novelDramaAdBlock', true),
        readingProgress: GM_getValue('readingProgress', true),
        autoScroll: GM_getValue('novelAutoScroll', false),
        nightMode: GM_getValue('novelNightMode', false),
        fontSize: GM_getValue('novelFontSize', 'medium'),
        theme: GM_getValue('novelTheme', 'auto'),
        appReminderBlock: GM_getValue('appReminderBlock', true),
        foldContentExpand: GM_getValue('foldContentExpand', true),
        autoExpandDelay: GM_getValue('autoExpandDelay', 1000)
    };

    // 网站类型检测和配置
    const siteConfigs = {
        // 小说网站
        'fanqienovel.com': {
            type: 'novel',
            name: '番茄小说',
            features: ['readingMode', 'progressSave', 'adBlock', 'miniAppBlock'],
            selectors: {
                content: '.chapter-content, .content',
                title: '.chapter-title, .title',
                navigation: '.chapter-nav, .nav',
                ads: '.ad, .advertisement, .sponsor'
            },
            miniAppSchemes: ['snssdk1128://', 'aweme://', 'douyin://']
        },
        'qidian.com': {
            type: 'novel',
            name: '起点中文网',
            features: ['readingMode', 'progressSave', 'adBlock', 'vipOptimize'],
            selectors: {
                content: '.read-section, .chapter-content',
                title: '.book-name, .chapter-title',
                navigation: '.chapter-control',
                ads: '.ad, .advertisement, .game-ad'
            }
        },
        'zongheng.com': {
            type: 'novel',
            name: '纵横中文网',
            features: ['readingMode', 'progressSave', 'adBlock'],
            selectors: {
                content: '.content, .chapter-content',
                title: '.title, .chapter-title',
                navigation: '.nav, .chapter-nav'
            }
        },
        'jjwxc.net': {
            type: 'novel',
            name: '晋江文学城',
            features: ['readingMode', 'progressSave', 'adBlock', 'commentEnhance'],
            selectors: {
                content: '.noveltext, .content',
                title: '.title, .chapter-title',
                navigation: '.nav'
            }
        },

        // 短剧视频网站
        'migu.cn': {
            type: 'drama',
            name: '咪咕视频',
            features: ['autoPlay', 'qualitySelect', 'adSkip', 'progressSave'],
            selectors: {
                video: 'video',
                playBtn: '.play-btn, .play-button',
                ads: '.ad, .advertisement',
                quality: '.quality-selector'
            },
            miniAppSchemes: ['migu://', 'cmvideo://']
        },
        'miguvideo.com': {
            type: 'drama',
            name: '咪咕视频',
            features: ['autoPlay', 'qualitySelect', 'adSkip'],
            selectors: {
                video: 'video',
                controls: '.video-controls',
                ads: '.ad-overlay, .advertisement'
            }
        },
        'tudou.com': {
            type: 'drama',
            name: '土豆视频',
            features: ['autoPlay', 'qualitySelect', 'adSkip'],
            selectors: {
                video: 'video',
                player: '.td-player'
            }
        },

        // 社交媒体平台
        'zhihu.com': {
            type: 'social',
            name: '知乎',
            features: ['appReminderBlock', 'foldContentExpand', 'adBlock', 'readingMode'],
            selectors: {
                appReminders: '.AppBanner, .DownloadBanner, .OpenInApp, .app-promotion, [data-za-detail-view-name*="app"]',
                foldContent: '.RichContent-collapsed, .ContentItem-expand, .QuestionRichText-more',
                content: '.RichContent-inner, .AnswerItem, .PostItem',
                ads: '.AdItem, .advertisement, .sponsor'
            },
            appReminderPatterns: [
                /在.*APP.*中打开/,
                /下载.*APP.*查看/,
                /用.*客户端.*浏览/,
                /在.*中.*看完整内容/
            ]
        },

        // 开发者社区平台
        'csdn.net': {
            type: 'dev',
            name: 'CSDN',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor, .recommend-ad, .csdn-side-toolbar',
                appReminders: '.download-app, .app-download, .open-in-app',
                content: '.article-content, .blog-content',
                toolbar: '.csdn-toolbar'
            }
        },
        'jianshu.com': {
            type: 'dev',
            name: '简书',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-open, .download-app',
                content: '.content, .article-content'
            }
        },
        'juejin.cn': {
            type: 'dev',
            name: '掘金',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.download-app, .open-in-app',
                content: '.article-content, .markdown-body'
            }
        },

        // 新闻资讯平台
        'sina.com.cn': {
            type: 'news',
            name: '新浪新闻',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .news-content'
            }
        },
        'sohu.com': {
            type: 'news',
            name: '搜狐新闻',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-open, .download-app',
                content: '.article-content, .news-content'
            }
        },

        // 视频平台
        'qq.com': {
            type: 'video',
            name: '腾讯视频',
            features: ['adBlock', 'appReminderBlock', 'videoOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                video: 'video',
                controls: '.video-controls'
            }
        },
        'youku.com': {
            type: 'video',
            name: '优酷视频',
            features: ['adBlock', 'appReminderBlock', 'videoOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                video: 'video'
            }
        },
        'iqiyi.com': {
            type: 'video',
            name: '爱奇艺',
            features: ['adBlock', 'appReminderBlock', 'videoOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                video: 'video'
            }
        },
        'bilibili.com': {
            type: 'video',
            name: '哔哩哔哩',
            features: ['adBlock', 'appReminderBlock', 'videoOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                video: 'video',
                danmu: '.danmaku-wrap'
            }
        },

        // 财经金融平台
        'sina.com.cn': {
            type: 'finance',
            name: '新浪财经',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .finance-content'
            }
        },
        'eastmoney.com': {
            type: 'finance',
            name: '东方财富网',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .news-content'
            }
        },

        // 汽车科技平台
        'pconline.com.cn': {
            type: 'tech',
            name: '太平洋电脑',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .news-content'
            }
        },
        'pcauto.com.cn': {
            type: 'auto',
            name: '太平洋汽车',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .news-content'
            }
        },
        'autohome.com.cn': {
            type: 'auto',
            name: '汽车之家',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .news-content'
            }
        },

        // 游戏应用平台
        'taptap.com': {
            type: 'game',
            name: 'TapTap',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .game-content'
            }
        },

        // 科技资讯平台
        'ithome.com': {
            type: 'tech',
            name: 'IT之家',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .news-content'
            }
        },

        // 开发者社区
        'oschina.net': {
            type: 'dev',
            name: '开源中国',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .news-content'
            }
        },
        'segmentfault.com': {
            type: 'dev',
            name: 'SegmentFault',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app',
                content: '.article-content, .question-content'
            }
        },

        // 设计创意平台
        'zcool.com.cn': {
            type: 'design',
            name: '站酷',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app, .bottom-App',
                content: '.work-content, .article-content'
            }
        },

        // 学术文献平台
        'cnki.net': {
            type: 'academic',
            name: '中国知网',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.Appopen, .app-download',
                content: '.article-content, .doc-content'
            }
        },

        // 购物比价平台
        'smzdm.com': {
            type: 'shopping',
            name: '什么值得买',
            features: ['adBlock', 'appReminderBlock', 'contentOptimize'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                appReminders: '.app-download, .open-in-app, .footer-banner-wrap',
                content: '.article-content, .news-content'
            }
        },
        'weibo.com': {
            type: 'social',
            name: '微博',
            features: ['appReminderBlock', 'foldContentExpand', 'adBlock'],
            selectors: {
                appReminders: '.app-download, .weibo-app, .download-banner, .open-in-app',
                foldContent: '.more, .expand, .show-full, .content-more',
                content: '.weibo-text, .feed-item, .card',
                ads: '.ad, .advertisement, .sponsor'
            },
            appReminderPatterns: [
                /在.*微博.*中打开/,
                /用.*客户端.*查看/,
                /下载.*APP.*看完整/
            ]
        },
        'xiaohongshu.com': {
            type: 'social',
            name: '小红书',
            features: ['appReminderBlock', 'foldContentExpand', 'adBlock', 'imageOptimize'],
            selectors: {
                appReminders: '.app-download, .download-guide, .open-in-app, .app-banner',
                foldContent: '.expand-btn, .show-more, .content-fold, .note-content-folded',
                content: '.note-content, .feed-item, .content',
                ads: '.ad, .advertisement, .sponsor',
                images: '.image-container img, .note-image'
            },
            appReminderPatterns: [
                /在.*小红书.*中打开/,
                /下载.*APP.*查看/,
                /用.*客户端.*浏览/
            ]
        },

        // 通用配置
        'default_novel': {
            type: 'novel',
            features: ['readingMode', 'progressSave', 'adBlock', 'foldContentExpand'],
            selectors: {
                content: '.content, .chapter-content, .novel-content',
                title: '.title, .chapter-title, h1',
                navigation: '.nav, .navigation, .chapter-nav',
                foldContent: '.expand, .more, .show-full, .collapsed'
            }
        },
        'default_drama': {
            type: 'drama',
            features: ['autoPlay', 'adSkip', 'progressSave', 'foldContentExpand'],
            selectors: {
                video: 'video',
                player: '.player, .video-player',
                controls: '.controls, .video-controls',
                foldContent: '.expand, .more, .show-full'
            }
        },
        'default_social': {
            type: 'social',
            features: ['appReminderBlock', 'foldContentExpand', 'adBlock'],
            selectors: {
                appReminders: '.app-download, .download-banner, .open-in-app, .app-banner',
                foldContent: '.expand, .more, .show-full, .collapsed, .content-more',
                content: '.content, .feed-item, .post',
                ads: '.ad, .advertisement, .sponsor'
            },
            appReminderPatterns: [
                /在.*APP.*中打开/,
                /下载.*APP.*查看/,
                /用.*客户端.*浏览/,
                /在.*中.*看完整/
            ]
        }
    };

    // 快应用和小程序Scheme列表
    const quickAppSchemes = [
        // 快应用
        'hap://', 'quickapp://', 'kwai://', 'kuaishou://',
        // 小程序
        'weixin://', 'wechat://', 'alipay://', 'dingtalk://',
        'qq://', 'weibo://', 'zhihu://', 'bilibili://',
        // 其他应用
        'taobao://', 'jd://', 'pinduoduo://', 'meituan://',
        'eleme://', 'didi://', 'ctrip://', 'qunar://',
        // 视频应用
        'youku://', 'iqiyi://', 'tencentvideo://', 'douyin://',
        'kuaishou://', 'huoshan://', 'xigua://',
        // 新闻资讯
        'toutiao://', 'snssdk1128://', 'sinaweibo://',
        // 工具应用
        'ucbrowser://', 'quark://', 'baidu://', 'sogou://'
    ];

    // 创建小说短剧增强控制面板
    function createNovelDramaPanel() {
        const siteConfig = detectSiteConfig();
        const panel = document.createElement('div');
        panel.id = 'ehviewer-novel-drama-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #FF6B6B, #4ECDC4); color: white;
                        padding: 10px; border-radius: 8px; font-size: 11px; font-family: Arial;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3); min-width: 180px;">
                <div style="display: flex; align-items: center; margin-bottom: 6px;">
                    <span style="font-size: 14px; margin-right: 6px;">${getSiteIcon(siteConfig.type)}</span>
                    <strong>${siteConfig.name}增强器</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 3px; margin-bottom: 6px;">
                    <button id="reading-mode" title="阅读模式">📖</button>
                    <button id="ad-block" title="广告拦截">🚫</button>
                    <button id="progress-save" title="进度保存">💾</button>
                    <button id="night-mode" title="夜间模式">🌙</button>
                    <button id="app-reminder-block" title="APP提醒拦截">🚫提醒</button>
                    <button id="fold-expand" title="展开折叠内容">📂展开</button>
                    <button id="mini-app-block" title="小程序拦截">🚫小程序</button>
                    <button id="settings-btn" title="设置">⚙️</button>
                </div>
                <div style="font-size: 8px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 3px;">
                    ${getSiteShortcuts(siteConfig.type)}
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('reading-mode').onclick = () => toggleReadingMode();
        document.getElementById('ad-block').onclick = () => toggleAdBlock();
        document.getElementById('progress-save').onclick = () => toggleProgressSave();
        document.getElementById('night-mode').onclick = toggleNightMode;
        document.getElementById('app-reminder-block').onclick = () => toggleAppReminderBlock();
        document.getElementById('fold-expand').onclick = () => toggleFoldContentExpand();
        document.getElementById('mini-app-block').onclick = () => toggleMiniAppBlock();
        document.getElementById('settings-btn').onclick = showNovelDramaSettings();
    }

    function getSiteIcon(type) {
        const icons = {
            novel: '📚',
            drama: '🎬',
            social: '💬',
            dev: '💻',
            news: '📰',
            video: '🎥',
            finance: '💰',
            tech: '🖥️',
            auto: '🚗',
            game: '🎮',
            design: '🎨',
            academic: '📖',
            shopping: '🛒',
            default: '🚀'
        };
        return icons[type] || icons.default;
    }

    function getSiteShortcuts(type) {
        const shortcuts = {
            novel: 'N夜间 | F字体 | S滚动 | E展开',
            drama: 'P播放 | Q画质 | A广告 | E展开',
            social: 'R拦截提醒 | E展开 | A广告',
            dev: 'C清理 | A广告 | R提醒拦截',
            news: 'R提醒拦截 | A广告 | E展开',
            video: 'A广告 | R提醒拦截 | P播放',
            finance: 'R提醒拦截 | A广告 | C内容优化',
            tech: 'A广告 | R提醒拦截 | C清理',
            auto: 'A广告 | R提醒拦截 | C内容优化',
            game: 'A广告 | R提醒拦截 | D下载拦截',
            design: 'A广告 | R提醒拦截 | C内容优化',
            academic: 'A广告 | R提醒拦截 | C内容优化',
            shopping: 'A广告 | R提醒拦截 | P价格优化',
            default: 'Z增强 | X拦截 | C清理 | E展开'
        };
        return shortcuts[type] || shortcuts.default;
    }

    function getSiteTypeName(type) {
        const typeNames = {
            novel: '小说网站',
            drama: '短剧视频',
            social: '社交媒体',
            dev: '开发者社区',
            news: '新闻资讯',
            video: '视频平台',
            finance: '财经金融',
            tech: '科技资讯',
            auto: '汽车科技',
            game: '游戏应用',
            design: '设计创意',
            academic: '学术文献',
            shopping: '购物比价',
            default: '其他网站'
        };
        return typeNames[type] || typeNames.default;
    }

    // 检测当前网站配置
    function detectSiteConfig() {
        const hostname = window.location.hostname.toLowerCase();

        // 精确匹配
        if (siteConfigs[hostname]) {
            return siteConfigs[hostname];
        }

        // 域名匹配
        for (const [domain, config] of Object.entries(siteConfigs)) {
            if (hostname.includes(domain.replace('www.', '').replace('.com', ''))) {
                return config;
            }
        }

        // 根据内容判断类型
        if (document.querySelector('.chapter-content, .novel-content, .book-content')) {
            return siteConfigs['default_novel'];
        }
        if (document.querySelector('video, .video-player, .player')) {
            return siteConfigs['default_drama'];
        }

        return siteConfigs['default_novel'];
    }

    // 小说阅读模式优化
    function toggleReadingMode() {
        if (!config.novelOptimize) return;

        const siteConfig = detectSiteConfig();
        if (siteConfig.type !== 'novel') return;

        GM_addStyle(`
            /* 小说阅读模式优化 */
            body {
                background: #f5f5f5 !important;
                font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif !important;
                line-height: 1.8 !important;
                color: #333 !important;
            }

            .chapter-content, .content, .noveltext {
                max-width: 800px !important;
                margin: 0 auto !important;
                padding: 20px !important;
                background: white !important;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1) !important;
                border-radius: 8px !important;
                font-size: ${getFontSize()} !important;
                line-height: 2 !important;
                text-align: justify !important;
            }

            .chapter-title, .title {
                text-align: center !important;
                font-size: 24px !important;
                margin-bottom: 30px !important;
                color: #2c3e50 !important;
                border-bottom: 2px solid #3498db !important;
                padding-bottom: 10px !important;
            }

            /* 段落优化 */
            p {
                margin-bottom: 1.2em !important;
                text-indent: 2em !important;
                font-size: ${getFontSize()} !important;
            }

            /* 隐藏无关元素 */
            .sidebar, .advertisement, .ad, .recommend,
            .comment-section, .related-books {
                display: none !important;
            }

            /* 导航优化 */
            .chapter-nav, .nav {
                position: fixed !important;
                bottom: 20px !important;
                left: 50% !important;
                transform: translateX(-50%) !important;
                background: rgba(0,0,0,0.8) !important;
                color: white !important;
                padding: 10px 20px !important;
                border-radius: 25px !important;
                display: flex !important;
                gap: 20px !important;
                z-index: 1000 !important;
            }

            .chapter-nav a, .nav a {
                color: white !important;
                text-decoration: none !important;
                padding: 5px 10px !important;
                border-radius: 15px !important;
                transition: background 0.3s !important;
            }

            .chapter-nav a:hover, .nav a:hover {
                background: rgba(255,255,255,0.2) !important;
            }
        `);

        // 优化段落格式
        optimizeParagraphs();

        // 添加阅读进度条
        addReadingProgress();

        showNotification('小说阅读模式已启用', 'success');
    }

    function getFontSize() {
        const sizes = {
            small: '16px',
            medium: '18px',
            large: '20px',
            xlarge: '22px'
        };
        return sizes[config.fontSize] || sizes.medium;
    }

    function optimizeParagraphs() {
        const content = document.querySelector('.chapter-content, .content, .noveltext');
        if (!content) return;

        const paragraphs = content.querySelectorAll('p');
        paragraphs.forEach(p => {
            // 清理多余的换行和空格
            p.innerHTML = p.innerHTML.replace(/\s+/g, ' ').trim();

            // 确保段首缩进
            if (!p.style.textIndent) {
                p.style.textIndent = '2em';
            }

            // 优化行高
            p.style.lineHeight = '2';
            p.style.marginBottom = '1.2em';
        });
    }

    function addReadingProgress() {
        const progressBar = document.createElement('div');
        progressBar.id = 'reading-progress';
        progressBar.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 0%;
            height: 3px;
            background: linear-gradient(90deg, #3498db, #2ecc71);
            z-index: 1001;
            transition: width 0.3s ease;
        `;
        document.body.appendChild(progressBar);

        // 更新进度条
        function updateProgress() {
            const content = document.querySelector('.chapter-content, .content, .noveltext');
            if (!content) return;

            const windowHeight = window.innerHeight;
            const contentTop = content.offsetTop;
            const contentHeight = content.scrollHeight;
            const scrollTop = window.pageYOffset;

            const progress = Math.min(100, Math.max(0,
                ((scrollTop - contentTop + windowHeight) / contentHeight) * 100
            ));

            progressBar.style.width = progress + '%';
        }

        window.addEventListener('scroll', updateProgress);
        updateProgress();

        // 保存阅读进度
        if (config.readingProgress) {
            saveReadingProgress();
        }
    }

    function saveReadingProgress() {
        const bookId = getBookId();
        const chapterId = getChapterId();
        const scrollPosition = window.pageYOffset;
        const maxScroll = document.documentElement.scrollHeight - window.innerHeight;

        const progress = {
            bookId: bookId,
            chapterId: chapterId,
            position: scrollPosition,
            percentage: (scrollPosition / maxScroll) * 100,
            timestamp: Date.now(),
            url: window.location.href
        };

        GM_setValue('reading_progress_' + bookId + '_' + chapterId, JSON.stringify(progress));

        // 每30秒自动保存
        setInterval(() => {
            const currentProgress = {
                ...progress,
                position: window.pageYOffset,
                percentage: (window.pageYOffset / maxScroll) * 100,
                timestamp: Date.now()
            };
            GM_setValue('reading_progress_' + bookId + '_' + chapterId, JSON.stringify(currentProgress));
        }, 30000);
    }

    function getBookId() {
        // 从URL或页面元素中提取书籍ID
        const urlMatch = window.location.href.match(/book\/(\d+)/) || window.location.href.match(/novel\/(\d+)/);
        if (urlMatch) return urlMatch[1];

        const bookElement = document.querySelector('[data-book-id], [data-novel-id]');
        if (bookElement) {
            return bookElement.getAttribute('data-book-id') || bookElement.getAttribute('data-novel-id');
        }

        return 'unknown';
    }

    function getChapterId() {
        // 从URL或页面元素中提取章节ID
        const urlMatch = window.location.href.match(/chapter\/(\d+)/) || window.location.href.match(/cid\/(\d+)/);
        if (urlMatch) return urlMatch[1];

        const chapterElement = document.querySelector('[data-chapter-id], [data-cid]');
        if (chapterElement) {
            return chapterElement.getAttribute('data-chapter-id') || chapterElement.getAttribute('data-cid');
        }

        return 'unknown';
    }

    // 短剧视频优化
    function optimizeDramaVideo() {
        if (!config.dramaOptimize) return;

        const siteConfig = detectSiteConfig();
        if (siteConfig.type !== 'drama') return;

        // 视频播放优化
        GM_addStyle(`
            /* 短剧视频播放优化 */
            video {
                width: 100% !important;
                height: auto !important;
                border-radius: 8px !important;
                box-shadow: 0 4px 16px rgba(0,0,0,0.2) !important;
            }

            .video-player, .player {
                position: relative !important;
                background: #000 !important;
                border-radius: 8px !important;
                overflow: hidden !important;
            }

            .video-controls {
                background: rgba(0,0,0,0.8) !important;
                color: white !important;
                padding: 10px !important;
                border-radius: 0 0 8px 8px !important;
            }

            /* 播放按钮优化 */
            .play-btn, .play-button {
                background: #e74c3c !important;
                color: white !important;
                border: none !important;
                border-radius: 50% !important;
                width: 60px !important;
                height: 60px !important;
                font-size: 20px !important;
                cursor: pointer !important;
                transition: all 0.3s ease !important;
                box-shadow: 0 4px 12px rgba(231, 76, 60, 0.3) !important;
            }

            .play-btn:hover, .play-button:hover {
                transform: scale(1.1) !important;
                box-shadow: 0 6px 20px rgba(231, 76, 60, 0.4) !important;
            }
        `);

        // 自动播放控制
        setupAutoPlay();

        // 画质选择优化
        optimizeQualitySelection();

        // 广告跳过
        setupAdSkip();

        showNotification('短剧视频体验已优化', 'success');
    }

    function setupAutoPlay() {
        const videos = document.querySelectorAll('video');
        videos.forEach(video => {
            if (config.autoScroll) {
                // 监听视频结束事件
                video.addEventListener('ended', () => {
                    setTimeout(() => {
                        // 尝试播放下一集
                        const nextBtn = document.querySelector('.next-episode, .next-btn, [aria-label*="下一集"]');
                        if (nextBtn) {
                            nextBtn.click();
                        }
                    }, 2000);
                });
            }

            // 添加播放控制提示
            video.addEventListener('play', () => {
                showNotification('视频播放中...', 'info');
            });

            video.addEventListener('pause', () => {
                showNotification('视频已暂停', 'info');
            });
        });
    }

    function optimizeQualitySelection() {
        const qualitySelectors = document.querySelectorAll('.quality-selector, .quality-btn, .resolution');
        qualitySelectors.forEach(selector => {
            // 自动选择最佳画质
            const bestQuality = selector.querySelector('[data-quality*="1080"], [data-quality*="720"], [data-quality*="HD"]');
            if (bestQuality && !bestQuality.classList.contains('active')) {
                bestQuality.click();
            }
        });
    }

    function setupAdSkip() {
        // 监听广告出现
        const adObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        const ads = node.querySelectorAll('.ad, .advertisement, .ad-overlay');
                        ads.forEach(ad => {
                            // 添加跳过按钮
                            if (!ad.querySelector('.skip-ad-btn')) {
                                const skipBtn = document.createElement('button');
                                skipBtn.className = 'skip-ad-btn';
                                skipBtn.textContent = '跳过广告';
                                skipBtn.style.cssText = `
                                    position: absolute;
                                    top: 10px;
                                    right: 10px;
                                    background: rgba(231, 76, 60, 0.9);
                                    color: white;
                                    border: none;
                                    border-radius: 4px;
                                    padding: 5px 10px;
                                    cursor: pointer;
                                    z-index: 1000;
                                `;
                                skipBtn.onclick = () => {
                                    ad.remove();
                                    showNotification('广告已跳过', 'success');
                                };
                                ad.appendChild(skipBtn);

                                // 自动跳过
                                setTimeout(() => {
                                    if (ad.parentNode) {
                                        ad.remove();
                                        showNotification('广告已自动跳过', 'success');
                                    }
                                }, 5000);
                            }
                        });
                    }
                });
            });
        });

        adObserver.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    // 快应用和小程序拦截
    function toggleMiniAppBlock() {
        if (!config.miniAppBlock && !config.quickAppBlock) return;

        // 拦截所有已知的应用scheme
        quickAppSchemes.forEach(scheme => {
            // 拦截点击事件
            document.addEventListener('click', function(e) {
                const target = e.target.closest('a');
                if (target && target.href && target.href.startsWith(scheme)) {
                    e.preventDefault();
                    e.stopPropagation();
                    logBlocked('快应用/小程序', target.href);
                    showInterceptNotification('已拦截快应用/小程序跳转', 'warning');
                    return false;
                }
            }, true);
        });

        // 拦截JavaScript调用
        const originalOpen = window.open;
        window.open = function(url, ...args) {
            if (url && quickAppSchemes.some(scheme => url.startsWith(scheme))) {
                logBlocked('JavaScript快应用调用', url);
                showInterceptNotification('已拦截JavaScript快应用调用', 'warning');
                return null;
            }
            return originalOpen.apply(this, [url, ...args]);
        };

        // 拦截动态生成的链接
        const linkObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        const links = node.querySelectorAll ? node.querySelectorAll('a') : [];
                        links.forEach(link => {
                            if (link.href && quickAppSchemes.some(scheme => link.href.startsWith(scheme))) {
                                link.href = 'javascript:void(0)';
                                link.setAttribute('data-blocked', 'true');
                                link.style.opacity = '0.5';
                                link.title = '已拦截的快应用/小程序链接';

                                // 添加提示文字
                                const notice = document.createElement('span');
                                notice.textContent = ' (已拦截)';
                                notice.style.color = '#e74c3c';
                                notice.style.fontSize = '12px';
                                link.appendChild(notice);

                                logBlocked('动态快应用链接', link.href);
                            }
                        });
                    }
                });
            });
        });

        linkObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        // 拦截各种形式的唤起
        interceptAppLaunches();

        showInterceptNotification('快应用/小程序拦截已启用', 'success');
    }

    function interceptAppLaunches() {
        // 拦截常见的APP唤起方法
        const appLaunchMethods = [
            'openApp', 'launchApp', 'callApp', 'startApp',
            'openMiniApp', 'launchMiniApp', 'callMiniApp',
            'openQuickApp', 'launchQuickApp', 'callQuickApp'
        ];

        appLaunchMethods.forEach(method => {
            if (window[method]) {
                window[method] = function() {
                    logBlocked('APP唤起方法', method);
                    showInterceptNotification(`已拦截 ${method} 调用`, 'warning');
                    return false;
                };
            }
        });

        // 拦截URL scheme跳转
        const originalHref = location.href;
        Object.defineProperty(location, 'href', {
            get: function() {
                return originalHref;
            },
            set: function(value) {
                if (value && quickAppSchemes.some(scheme => value.startsWith(scheme))) {
                    logBlocked('Location.href 跳转', value);
                    showInterceptNotification('已拦截location.href跳转', 'warning');
                    return;
                }
                originalHref = value;
            }
        });
    }

    // APP提醒拦截
    function toggleAppReminderBlock() {
        if (!config.appReminderBlock) return;

        const siteConfig = detectSiteConfig();

        // 移除APP提醒元素
        const reminderSelectors = siteConfig.selectors?.appReminders ||
            '.app-download, .download-banner, .open-in-app, .app-banner, .app-promotion';

        const selectors = reminderSelectors.split(', ');
        selectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                element.remove();
                logBlocked('APP提醒元素', selector);
            });
        });

        // 移除基于文本内容的提醒
        const textPatterns = siteConfig.appReminderPatterns || [
            /在.*APP.*中打开/,
            /下载.*APP.*查看/,
            /用.*客户端.*浏览/,
            /在.*中.*看完整/
        ];

        // 扫描所有文本元素
        const allElements = document.querySelectorAll('*');
        allElements.forEach(element => {
            if (element.children.length === 0 && element.textContent) {
                const text = element.textContent.trim();
                textPatterns.forEach(pattern => {
                    if (pattern.test(text)) {
                        const parent = element.parentElement;
                        if (parent && !parent.classList.contains('app-reminder-processed')) {
                            parent.style.display = 'none';
                            parent.classList.add('app-reminder-processed');
                            logBlocked('APP提醒文本', text.substring(0, 30) + '...');
                        }
                    }
                });
            }
        });

        // 持续监控新的APP提醒
        const reminderObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        // 检查新添加的元素是否包含APP提醒
                        const newElements = node.querySelectorAll ?
                            node.querySelectorAll(reminderSelectors) : [];
                        newElements.forEach(element => {
                            element.remove();
                            logBlocked('新APP提醒元素', reminderSelectors);
                        });

                        // 检查文本内容
                        const textElements = node.querySelectorAll ?
                            node.querySelectorAll('*') : [node];
                        textElements.forEach(element => {
                            if (element.children && element.children.length === 0 && element.textContent) {
                                const text = element.textContent.trim();
                                textPatterns.forEach(pattern => {
                                    if (pattern.test(text)) {
                                        const parent = element.parentElement;
                                        if (parent && !parent.classList.contains('app-reminder-processed')) {
                                            parent.style.display = 'none';
                                            parent.classList.add('app-reminder-processed');
                                            logBlocked('新APP提醒文本', text.substring(0, 30) + '...');
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            });
        });

        reminderObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        showNotification('APP提醒已清理', 'success');
    }

    // 折叠内容展开
    function toggleFoldContentExpand() {
        if (!config.foldContentExpand) return;

        const siteConfig = detectSiteConfig();

        // 获取折叠内容选择器
        const foldSelectors = siteConfig.selectors?.foldContent ||
            '.expand, .more, .show-full, .collapsed, .content-more, .RichContent-collapsed';

        const selectors = foldSelectors.split(', ');

        // 点击展开按钮
        selectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                if (element.onclick) {
                    // 如果元素有点击事件，模拟点击
                    element.click();
                    logBlocked('折叠内容展开', selector);
                } else if (element.tagName === 'BUTTON' || element.tagName === 'A' ||
                          element.classList.contains('expand') || element.classList.contains('more')) {
                    // 模拟点击
                    element.click();
                    logBlocked('折叠内容展开', selector);
                }
            });
        });

        // 处理知乎特殊的折叠内容
        if (window.location.hostname.includes('zhihu.com')) {
            expandZhihuContent();
        }

        // 处理微博的折叠内容
        if (window.location.hostname.includes('weibo.com')) {
            expandWeiboContent();
        }

        // 处理小红书的折叠内容
        if (window.location.hostname.includes('xiaohongshu.com')) {
            expandXiaohongshuContent();
        }

        // 通用折叠内容处理
        expandGenericFoldedContent();

        // 设置延迟自动展开（处理动态加载的内容）
        setTimeout(() => {
            expandGenericFoldedContent();
        }, config.autoExpandDelay);

        showNotification('折叠内容已展开', 'success');
    }

    function expandZhihuContent() {
        // 知乎答案展开
        const collapsedAnswers = document.querySelectorAll('.RichContent-collapsed');
        collapsedAnswers.forEach(answer => {
            const expandBtn = answer.querySelector('.ContentItem-expand') ||
                            answer.querySelector('[data-za-detail-view-name*="expand"]');
            if (expandBtn) {
                expandBtn.click();
            }
        });

        // 知乎问题描述展开
        const questionMore = document.querySelector('.QuestionRichText-more');
        if (questionMore) {
            questionMore.click();
        }

        // 移除"继续查看"提示
        const continueHints = document.querySelectorAll('[data-za-detail-view-name*="continue"]');
        continueHints.forEach(hint => {
            hint.style.display = 'none';
        });
    }

    function expandWeiboContent() {
        // 微博长文本展开
        const moreLinks = document.querySelectorAll('.more, .expand, .show-full');
        moreLinks.forEach(link => {
            if (link.textContent.includes('展开') || link.textContent.includes('全文') ||
                link.textContent.includes('more')) {
                link.click();
            }
        });

        // 微博评论展开
        const commentMore = document.querySelectorAll('.comment-more, .show-more-comments');
        commentMore.forEach(btn => {
            btn.click();
        });
    }

    function expandXiaohongshuContent() {
        // 小红书笔记内容展开
        const expandBtns = document.querySelectorAll('.expand-btn, .show-more, .content-fold');
        expandBtns.forEach(btn => {
            btn.click();
        });

        // 小红书评论展开
        const commentExpand = document.querySelectorAll('.comment-expand, .show-all-comments');
        commentExpand.forEach(btn => {
            btn.click();
        });

        // 处理图片懒加载
        const lazyImages = document.querySelectorAll('img[data-src]');
        lazyImages.forEach(img => {
            if (!img.src) {
                img.src = img.getAttribute('data-src');
            }
        });
    }

    function expandGenericFoldedContent() {
        // 通用折叠内容处理
        const genericSelectors = [
            '.expand', '.more', '.show-more', '.show-full', '.collapsed',
            '.content-more', '.read-more', '.see-more', '.expand-content',
            '[aria-label*="展开"]', '[aria-label*="更多"]', '[aria-label*="全文"]',
            'button:contains("展开")', 'a:contains("更多")', 'span:contains("全文")'
        ];

        genericSelectors.forEach(selector => {
            try {
                const elements = document.querySelectorAll(selector);
                elements.forEach(element => {
                    const text = element.textContent?.toLowerCase() || '';
                    if (text.includes('展开') || text.includes('更多') ||
                        text.includes('全文') || text.includes('查看') ||
                        text.includes('显示') || text.includes('show') ||
                        text.includes('more') || text.includes('expand')) {

                        if (!element.classList.contains('fold-expanded')) {
                            element.click();
                            element.classList.add('fold-expanded');
                            logBlocked('通用折叠内容', selector);
                        }
                    }
                });
            } catch (e) {
                // 忽略选择器错误
            }
        });

        // 处理CSS隐藏的内容
        const hiddenContents = document.querySelectorAll('[style*="display: none"], [style*="height: 0"]');
        hiddenContents.forEach(element => {
            const text = element.textContent?.toLowerCase() || '';
            const parentText = element.parentElement?.textContent?.toLowerCase() || '';

            // 检查是否是折叠内容
            if ((text.includes('展开') || text.includes('更多') || text.includes('全文')) ||
                (parentText.includes('在app中') || parentText.includes('下载') ||
                 parentText.includes('客户端'))) {
                element.style.display = 'none'; // 隐藏提示，继续隐藏折叠提示
            }
        });
    }

    // 广告拦截
    function toggleAdBlock() {
        if (!config.adBlock) return;

        const siteConfig = detectSiteConfig();
        const adSelectors = siteConfig.selectors.ads || '.ad, .advertisement, .sponsor, .recommend-ad';

        const selectors = adSelectors.split(', ');
        selectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                element.remove();
                logBlocked('广告元素', selector);
            });
        });

        // 持续监控新广告
        const adObserver = new MutationObserver(() => {
            toggleAdBlock();
        });

        adObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        showNotification('广告已清理', 'success');
    }

    // 夜间模式
    function toggleNightMode() {
        config.nightMode = !config.nightMode;
        GM_setValue('novelNightMode', config.nightMode);

        if (config.nightMode) {
            applyNightMode();
        } else {
            removeNightMode();
        }

        showNotification(`夜间模式${config.nightMode ? '已启用' : '已禁用'}`, 'info');
    }

    function applyNightMode() {
        GM_addStyle(`
            .night-mode {
                background: #1a1a1a !important;
                color: #e0e0e0 !important;
            }

            .night-mode .chapter-content, .night-mode .content {
                background: #2a2a2a !important;
                color: #e0e0e0 !important;
                border: 1px solid #444 !important;
            }

            .night-mode .chapter-title, .night-mode .title {
                color: #ffffff !important;
                border-bottom-color: #555 !important;
            }

            .night-mode a {
                color: #4da6ff !important;
            }

            .night-mode .chapter-nav, .night-mode .nav {
                background: rgba(42, 42, 42, 0.9) !important;
            }
        `);

        document.body.classList.add('night-mode');
    }

    function removeNightMode() {
        document.body.classList.remove('night-mode');
    }

    // 进度保存
    function toggleProgressSave() {
        config.readingProgress = !config.readingProgress;
        GM_setValue('readingProgress', config.readingProgress);

        if (config.readingProgress) {
            saveReadingProgress();
            showNotification('阅读进度保存已启用', 'success');
        } else {
            showNotification('阅读进度保存已禁用', 'info');
        }
    }

    // 拦截统计
    let interceptStats = {
        schemes: 0,
        javascript: 0,
        ads: 0,
        total: 0
    };

    function logBlocked(type, details) {
        if (!config.logBlocked) return;

        interceptStats.total++;

        switch (type) {
            case '快应用/小程序':
            case '动态快应用链接':
            case 'JavaScript快应用调用':
            case 'Location.href 跳转':
            case 'APP唤起方法':
                interceptStats.schemes++;
                break;
            case '广告元素':
                interceptStats.ads++;
                break;
            default:
                interceptStats.javascript++;
        }

        GM_log(`拦截${type}: ${details}`);
    }

    // 设置面板
    function showNovelDramaSettings() {
        return function() {
            const siteConfig = detectSiteConfig();
            const settings = document.createElement('div');
            settings.innerHTML = `
                <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                            background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                            box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 400px;">
                    <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">
                        ${siteConfig.name} 增强器设置
                    </h3>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-enabled" style="margin-right: 8px;" ${config.enabled ? 'checked' : ''}>
                            启用增强器
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-novel" style="margin-right: 8px;" ${config.novelOptimize ? 'checked' : ''}>
                            小说优化
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-drama" style="margin-right: 8px;" ${config.dramaOptimize ? 'checked' : ''}>
                            短剧优化
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-adblock" style="margin-right: 8px;" ${config.adBlock ? 'checked' : ''}>
                            广告拦截
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-progress" style="margin-right: 8px;" ${config.readingProgress ? 'checked' : ''}>
                            阅读进度
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-autoscroll" style="margin-right: 8px;" ${config.autoScroll ? 'checked' : ''}>
                            自动滚动
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-app-reminder" style="margin-right: 8px;" ${config.appReminderBlock ? 'checked' : ''}>
                            APP提醒拦截
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-fold-expand" style="margin-right: 8px;" ${config.foldContentExpand ? 'checked' : ''}>
                            自动展开折叠
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-miniapp" style="margin-right: 8px;" ${config.miniAppBlock ? 'checked' : ''}>
                            小程序拦截
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-quickapp" style="margin-right: 8px;" ${config.quickAppBlock ? 'checked' : ''}>
                            快应用拦截
                        </label>
                    </div>

                    <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                        <h4 style="margin: 0 0 10px 0; color: #666;">字体大小</h4>
                        <select id="font-size-select" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                            <option value="small" ${config.fontSize === 'small' ? 'selected' : ''}>小号 (16px)</option>
                            <option value="medium" ${config.fontSize === 'medium' ? 'selected' : ''}>中号 (18px)</option>
                            <option value="large" ${config.fontSize === 'large' ? 'selected' : ''}>大号 (20px)</option>
                            <option value="xlarge" ${config.fontSize === 'xlarge' ? 'selected' : ''}>超大 (22px)</option>
                        </select>
                    </div>

                    <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                        <h4 style="margin: 0 0 10px 0; color: #666;">自动展开延迟 (毫秒)</h4>
                        <select id="expand-delay-select" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                            <option value="500" ${config.autoExpandDelay === 500 ? 'selected' : ''}>快速 (500ms)</option>
                            <option value="1000" ${config.autoExpandDelay === 1000 ? 'selected' : ''}>正常 (1000ms)</option>
                            <option value="2000" ${config.autoExpandDelay === 2000 ? 'selected' : ''}>慢速 (2000ms)</option>
                            <option value="3000" ${config.autoExpandDelay === 3000 ? 'selected' : ''}>很慢 (3000ms)</option>
                        </select>
                    </div>

                    <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                        <h4 style="margin: 0 0 10px 0; color: #666;">当前网站信息</h4>
                        <div style="font-size: 12px; color: #666;">
                            <div>网站类型: ${getSiteTypeName(siteConfig.type)}</div>
                            <div>网站名称: ${siteConfig.name}</div>
                            <div>拦截统计: ${interceptStats.total} 次</div>
                        </div>
                    </div>

                    <div style="text-align: right;">
                        <button id="save-novel-drama-settings" style="padding: 10px 20px;
                                        background: linear-gradient(135deg, #FF6B6B, #4ECDC4);
                                        color: white; border: none; border-radius: 6px;
                                        cursor: pointer; margin-right: 10px;">
                            保存设置
                        </button>
                        <button id="close-novel-drama-settings" style="padding: 10px 20px;
                                         background: #6c757d; color: white; border: none;
                                         border-radius: 6px; cursor: pointer;">
                            关闭
                        </button>
                    </div>
                </div>
            `;

            document.body.appendChild(settings);

            document.getElementById('save-novel-drama-settings').onclick = saveNovelDramaSettings;
            document.getElementById('close-novel-drama-settings').onclick = () => settings.remove();
        };
    }

    function saveNovelDramaSettings() {
        config.enabled = document.getElementById('setting-enabled').checked;
        config.novelOptimize = document.getElementById('setting-novel').checked;
        config.dramaOptimize = document.getElementById('setting-drama').checked;
        config.adBlock = document.getElementById('setting-adblock').checked;
        config.readingProgress = document.getElementById('setting-progress').checked;
        config.autoScroll = document.getElementById('setting-autoscroll').checked;
        config.appReminderBlock = document.getElementById('setting-app-reminder').checked;
        config.foldContentExpand = document.getElementById('setting-fold-expand').checked;
        config.miniAppBlock = document.getElementById('setting-miniapp').checked;
        config.quickAppBlock = document.getElementById('setting-quickapp').checked;
        config.fontSize = document.getElementById('font-size-select').value;
        config.autoExpandDelay = parseInt(document.getElementById('expand-delay-select').value);

        // 保存设置
        Object.keys(config).forEach(key => {
            GM_setValue('novelDrama' + key.charAt(0).toUpperCase() + key.slice(1), config[key]);
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

    function showInterceptNotification(message, type = 'info') {
        showNotification(message, type);
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        document.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            const siteConfig = detectSiteConfig();

            // 通用快捷键
            switch (e.keyCode) {
                case 78: // N - 夜间模式
                    if (e.ctrlKey) {
                        e.preventDefault();
                        toggleNightMode();
                    }
                    break;
                case 70: // F - 字体大小
                    if (e.ctrlKey) {
                        e.preventDefault();
                        cycleFontSize();
                    }
                    break;
                case 83: // S - 自动滚动
                    if (e.ctrlKey) {
                        e.preventDefault();
                        config.autoScroll = !config.autoScroll;
                        GM_setValue('novelAutoScroll', config.autoScroll);
                        showNotification(`自动滚动${config.autoScroll ? '已启用' : '已禁用'}`, 'info');
                    }
                    break;
                case 69: // E - 展开折叠内容
                    if (e.ctrlKey) {
                        e.preventDefault();
                        toggleFoldContentExpand();
                    }
                    break;
                case 82: // R - APP提醒拦截
                    if (e.ctrlKey) {
                        e.preventDefault();
                        toggleAppReminderBlock();
                    }
                    break;
            }

            // 短剧相关快捷键
            if (siteConfig.type === 'drama') {
                switch (e.keyCode) {
                    case 80: // P - 播放/暂停
                        e.preventDefault();
                        const video = document.querySelector('video');
                        if (video) {
                            video.paused ? video.play() : video.pause();
                        }
                        break;
                    case 81: // Q - 画质选择
                        e.preventDefault();
                        optimizeQualitySelection();
                        break;
                    case 65: // A - 广告跳过
                        e.preventDefault();
                        const ads = document.querySelectorAll('.ad, .advertisement');
                        ads.forEach(ad => ad.remove());
                        showNotification('广告已跳过', 'success');
                        break;
                }
            }
        });
    }

    function cycleFontSize() {
        const sizes = ['small', 'medium', 'large', 'xlarge'];
        const currentIndex = sizes.indexOf(config.fontSize);
        const nextIndex = (currentIndex + 1) % sizes.length;
        config.fontSize = sizes[nextIndex];
        GM_setValue('novelFontSize', config.fontSize);

        // 重新应用阅读模式以更新字体大小
        if (document.querySelector('.chapter-content, .content')) {
            toggleReadingMode();
        }

        showNotification(`字体大小: ${config.fontSize}`, 'info');
    }

    // 初始化
    function init() {
        if (!config.enabled) {
            GM_log('小说短剧增强器已禁用');
            return;
        }

        setTimeout(() => {
            createNovelDramaPanel();
            setupKeyboardShortcuts();

            const siteConfig = detectSiteConfig();

            // 根据网站类型自动应用功能
            if (siteConfig.type === 'novel' && config.novelOptimize) {
                toggleReadingMode();
            } else if (siteConfig.type === 'drama' && config.dramaOptimize) {
                optimizeDramaVideo();
            }

            // 为所有网站类型启用通用功能
            if (siteConfig.features?.includes('contentOptimize') && config.novelOptimize) {
                // 内容优化适用于多种网站类型
                toggleReadingMode();
            }

            if (config.adBlock) {
                toggleAdBlock();
            }

            if (config.appReminderBlock) {
                toggleAppReminderBlock();
            }

            if (config.foldContentExpand) {
                // 延迟执行，确保页面元素加载完成
                setTimeout(() => {
                    toggleFoldContentExpand();
                }, 1500);
            }

            if (config.miniAppBlock || config.quickAppBlock) {
                toggleMiniAppBlock();
            }

            if (config.nightMode) {
                applyNightMode();
            }

            GM_log('EhViewer 小说短剧增强器初始化完成');

            // 显示欢迎信息
            showNotification(`${siteConfig.name} 增强器已启动！`, 'success');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
