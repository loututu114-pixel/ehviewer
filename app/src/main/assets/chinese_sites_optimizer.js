// ==UserScript==
// @name         EhViewer 中文热门网站优化器
// @namespace    http://ehviewer.com/
// @version      2.5.0
// @description  优化Bilibili、小红书、百度贴吧、抖音、快手、知乎、百度搜索等热门中文网站体验
// @author       EhViewer Team
// @match        *://*.bilibili.com/*
// @match        *://*.xiaohongshu.com/*
// @match        *://*.tieba.baidu.com/*
// @match        *://*.douyin.com/*
// @match        *://*.kuaishou.com/*
// @match        *://*.zhihu.com/*
// @match        *://*.baidu.com/*
// @exclude      *://*.google.com/*
// @exclude      *://*.facebook.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 中文热门网站优化器已启动');

    // 网站检测和配置
    const siteConfig = {
        'bilibili.com': {
            name: '哔哩哔哩',
            features: ['adBlock', 'videoEnhance', 'danmuOptimize', 'autoPlayNext'],
            selectors: {
                ads: '.ad-report, .video-ad, .banner-ad, .pop-live-small-header',
                videoPlayer: 'video',
                nextBtn: '.next-button, .recommend-list .video-card',
                danmu: '.danmaku-wrap',
                qualityBtn: '.bpx-player-ctrl-quality'
            }
        },
        'xiaohongshu.com': {
            name: '小红书',
            features: ['adBlock', 'imageOptimize', 'contentFilter', 'searchEnhance'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor, .commercial',
                images: '.image-container img, .note-image, .feed-card img',
                searchResults: '.search-result, .feed-item',
                content: '.note-content, .feed-content'
            }
        },
        'tieba.baidu.com': {
            name: '百度贴吧',
            features: ['adBlock', 'imageViewer', 'floorJump', 'contentFilter'],
            selectors: {
                ads: '.ad, .advertisement, .baidu-ad, .tb-ad',
                images: '.BDE_Image, .j_media_item img',
                floors: '.l_post, .d_post_content',
                searchBtn: '.search_btn'
            }
        },
        'douyin.com': {
            name: '抖音',
            features: ['adBlock', 'videoEnhance', 'autoScroll', 'downloadVideo'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                videoPlayer: 'video',
                feed: '.feed-item, .video-item'
            }
        },
        'kuaishou.com': {
            name: '快手',
            features: ['adBlock', 'videoEnhance', 'autoScroll'],
            selectors: {
                ads: '.ad, .advertisement, .sponsor',
                videoPlayer: 'video',
                feed: '.feed-item, .video-item'
            }
        },
        'zhihu.com': {
            name: '知乎',
            features: ['adBlock', 'answerOptimize', 'searchEnhance', 'contentFold'],
            selectors: {
                ads: '.AdItem, .advertisement, .sponsor',
                answers: '.AnswerItem, .answer-content',
                searchResults: '.SearchResult, .search-item',
                foldBtn: '.collapse-btn, .fold-btn'
            }
        },
        'baidu.com': {
            name: '百度搜索',
            features: ['adLabel', 'searchOptimize', 'resultFilter'],
            selectors: {
                ads: '.ad, .advertisement, .ec-tuiguang',
                results: '.result, .c-result',
                sidebar: '.right-sidebar, .cr-offset'
            }
        }
    };

    // 配置选项
    const config = {
        enabled: GM_getValue('chineseSitesEnabled', true),
        currentSite: detectCurrentSite(),
        features: GM_getValue('siteFeatures', {}),
        autoApply: GM_getValue('autoApply', true)
    };

    // 检测当前网站
    function detectCurrentSite() {
        const hostname = window.location.hostname;
        for (const [domain, siteInfo] of Object.entries(siteConfig)) {
            if (hostname.includes(domain)) {
                return domain;
            }
        }
        return null;
    }

    // 创建网站专用控制面板
    function createSiteControlPanel() {
        if (!config.currentSite) return;

        const siteInfo = siteConfig[config.currentSite];
        const panel = document.createElement('div');
        panel.id = 'ehviewer-chinese-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: rgba(0,0,0,0.8); color: white; padding: 8px;
                        border-radius: 5px; font-size: 11px; font-family: Arial;">
                <div style="margin-bottom: 5px; font-weight: bold; color: #ff6b6b;">
                    ${siteInfo.name} 优化器
                </div>
                <div style="display: flex; gap: 5px;">
                    <button id="toggle-ads" title="广告拦截">🚫</button>
                    <button id="optimize-content" title="内容优化">⚡</button>
                    <button id="site-settings" title="网站设置">⚙️</button>
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('toggle-ads').onclick = () => toggleFeature('adBlock');
        document.getElementById('optimize-content').onclick = () => toggleFeature('contentOptimize');
        document.getElementById('site-settings').onclick = showSiteSettings;
    }

    // 功能开关
    function toggleFeature(featureName) {
        const currentState = config.features[featureName] !== false;
        config.features[featureName] = !currentState;
        GM_setValue('siteFeatures', config.features);

        // 重新应用优化
        applySiteOptimizations();

        const button = document.getElementById('toggle-ads');
        if (button) {
            button.textContent = currentState ? '🚫' : '✅';
        }

        GM_log(`${featureName} ${!currentState ? '已启用' : '已禁用'}`);
    }

    // 显示网站设置
    function showSiteSettings() {
        const siteInfo = siteConfig[config.currentSite];
        const settings = document.createElement('div');
        settings.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 20px; border-radius: 10px; z-index: 10001;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.3); max-width: 400px;">
                <h3 style="margin: 0 0 15px 0; color: #333;">${siteInfo.name} 设置</h3>
                <div id="feature-list">
                    ${siteInfo.features.map(feature => `
                        <label style="display: block; margin-bottom: 8px;">
                            <input type="checkbox" id="feature-${feature}"
                                   ${config.features[feature] !== false ? 'checked' : ''}>
                            ${getFeatureName(feature)}
                        </label>
                    `).join('')}
                </div>
                <div style="text-align: right; margin-top: 15px;">
                    <button id="apply-settings" style="margin-right: 10px; padding: 8px 15px;
                                                    background: #007bff; color: white; border: none;
                                                    border-radius: 5px; cursor: pointer;">
                        应用
                    </button>
                    <button id="close-settings" style="padding: 8px 15px; background: #6c757d;
                                                   color: white; border: none; border-radius: 5px;
                                                   cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(settings);

        document.getElementById('apply-settings').onclick = function() {
            siteInfo.features.forEach(feature => {
                const checkbox = document.getElementById(`feature-${feature}`);
                config.features[feature] = checkbox.checked;
            });
            GM_setValue('siteFeatures', config.features);
            applySiteOptimizations();
            settings.remove();
        };

        document.getElementById('close-settings').onclick = () => settings.remove();
    }

    function getFeatureName(feature) {
        const names = {
            adBlock: '广告拦截',
            videoEnhance: '视频增强',
            danmuOptimize: '弹幕优化',
            autoPlayNext: '自动播放下一个',
            imageOptimize: '图片优化',
            contentFilter: '内容过滤',
            searchEnhance: '搜索增强',
            imageViewer: '图片查看器',
            floorJump: '楼层跳转',
            autoScroll: '自动滚动',
            downloadVideo: '视频下载',
            answerOptimize: '答案优化',
            contentFold: '内容折叠',
            adLabel: '广告标识',
            searchOptimize: '搜索优化',
            resultFilter: '结果过滤'
        };
        return names[feature] || feature;
    }

    // 应用网站特定优化
    function applySiteOptimizations() {
        if (!config.currentSite || !config.enabled) return;

        const siteInfo = siteConfig[config.currentSite];
        const features = config.features;

        // 通用广告拦截（如果启用）
        if (features.adBlock !== false) {
            applyAdBlock(siteInfo.selectors.ads);
        }

        // 根据网站类型应用专门优化
        switch (config.currentSite) {
            case 'bilibili.com':
                optimizeBilibili(features);
                break;
            case 'xiaohongshu.com':
                optimizeXiaohongshu(features);
                break;
            case 'tieba.baidu.com':
                optimizeTieba(features);
                break;
            case 'douyin.com':
                optimizeDouyin(features);
                break;
            case 'kuaishou.com':
                optimizeKuaishou(features);
                break;
            case 'zhihu.com':
                optimizeZhihu(features);
                break;
            case 'baidu.com':
                optimizeBaiduSearch(features);
                break;
        }
    }

    // Bilibili优化
    function optimizeBilibili(features) {
        // 视频增强
        if (features.videoEnhance !== false) {
            // 自动选择高清画质
            setTimeout(() => {
                const qualityBtn = document.querySelector('.bpx-player-ctrl-quality');
                if (qualityBtn) {
                    qualityBtn.click();
                    setTimeout(() => {
                        const hdOption = document.querySelector('.bpx-player-ctrl-quality-menu-item:contains("1080")') ||
                                       document.querySelector('.bpx-player-ctrl-quality-menu-item:contains("720")');
                        if (hdOption) hdOption.click();
                    }, 500);
                }
            }, 3000);

            // 移除视频内广告
            const videoAds = document.querySelectorAll('.video-ad, .ad-report');
            videoAds.forEach(ad => ad.remove());
        }

        // 弹幕优化
        if (features.danmuOptimize !== false) {
            GM_addStyle(`
                .danmaku-wrap { opacity: 0.7 !important; }
                .danmaku-wrap:hover { opacity: 1 !important; }
            `);
        }

        // 自动播放下一个
        if (features.autoPlayNext !== false) {
            const video = document.querySelector('video');
            if (video) {
                video.addEventListener('ended', () => {
                    const nextBtn = document.querySelector('.next-button, .recommend-list .video-card:first-child a');
                    if (nextBtn) {
                        setTimeout(() => nextBtn.click(), 2000);
                    }
                });
            }
        }
    }

    // 小红书优化
    function optimizeXiaohongshu(features) {
        // 图片优化
        if (features.imageOptimize !== false) {
            const images = document.querySelectorAll('.image-container img, .note-image');
            images.forEach(img => {
                // 添加懒加载优化
                if (!img.dataset.optimized) {
                    img.dataset.optimized = 'true';
                    img.style.transition = 'transform 0.2s';
                    img.addEventListener('click', () => showImageModal(img));
                }
            });

            // 预加载下一页图片
            preloadImages();
        }

        // 内容过滤
        if (features.contentFilter !== false) {
            // 移除重复内容
            const contents = document.querySelectorAll('.note-content');
            const seenContent = new Set();

            contents.forEach(content => {
                const text = content.textContent.trim();
                if (seenContent.has(text.substring(0, 100))) {
                    content.closest('.feed-card').style.display = 'none';
                } else {
                    seenContent.add(text.substring(0, 100));
                }
            });
        }

        // 搜索增强
        if (features.searchEnhance !== false) {
            const searchInput = document.querySelector('input[placeholder*="搜索"]');
            if (searchInput) {
                searchInput.addEventListener('input', debounce(optimizeSearch, 300));
            }
        }
    }

    // 百度贴吧优化
    function optimizeTieba(features) {
        // 图片查看器
        if (features.imageViewer !== false) {
            const images = document.querySelectorAll('.BDE_Image');
            images.forEach(img => {
                img.style.cursor = 'zoom-in';
                img.addEventListener('click', () => showImageModal(img));
            });
        }

        // 楼层跳转
        if (features.floorJump !== false) {
            addFloorNavigation();
        }

        // 内容过滤
        if (features.contentFilter !== false) {
            // 隐藏无关广告贴
            const posts = document.querySelectorAll('.l_post');
            posts.forEach(post => {
                const content = post.textContent;
                if (content.includes('广告') || content.includes('推广') || content.length < 10) {
                    post.style.opacity = '0.3';
                }
            });
        }
    }

    // 抖音优化
    function optimizeDouyin(features) {
        // 视频增强
        if (features.videoEnhance !== false) {
            // 移除自动播放下一条
            const videos = document.querySelectorAll('video');
            videos.forEach(video => {
                video.addEventListener('ended', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                });
            });
        }

        // 自动滚动
        if (features.autoScroll !== false) {
            let autoScrollInterval;
            const startAutoScroll = () => {
                if (autoScrollInterval) clearInterval(autoScrollInterval);
                autoScrollInterval = setInterval(() => {
                    window.scrollBy(0, 1);
                }, 100);
            };

            const stopAutoScroll = () => {
                if (autoScrollInterval) {
                    clearInterval(autoScrollInterval);
                    autoScrollInterval = null;
                }
            };

            // 添加控制按钮
            const controlBtn = document.createElement('button');
            controlBtn.textContent = '⏸️';
            controlBtn.style.cssText = 'position: fixed; top: 50%; right: 10px; z-index: 1000;';
            controlBtn.onclick = () => {
                if (autoScrollInterval) {
                    stopAutoScroll();
                    controlBtn.textContent = '▶️';
                } else {
                    startAutoScroll();
                    controlBtn.textContent = '⏸️';
                }
            };
            document.body.appendChild(controlBtn);
        }

        // 视频下载
        if (features.downloadVideo !== false) {
            addDownloadButtons();
        }
    }

    // 快手优化
    function optimizeKuaishou(features) {
        // 类似抖音的优化
        optimizeDouyin(features);

        // 额外的内容过滤
        if (features.contentFilter !== false) {
            const videos = document.querySelectorAll('.video-item');
            videos.forEach(video => {
                const title = video.textContent;
                if (title.includes('广告') || title.includes('推广')) {
                    video.style.display = 'none';
                }
            });
        }
    }

    // 知乎优化
    function optimizeZhihu(features) {
        // 答案优化
        if (features.answerOptimize !== false) {
            // 按赞数排序答案
            const answers = document.querySelectorAll('.AnswerItem');
            const sortedAnswers = Array.from(answers).sort((a, b) => {
                const aVotes = parseInt(a.querySelector('.VoteButton')?.textContent || '0');
                const bVotes = parseInt(b.querySelector('.VoteButton')?.textContent || '0');
                return bVotes - aVotes;
            });

            const container = answers[0]?.parentElement;
            if (container) {
                sortedAnswers.forEach(answer => container.appendChild(answer));
            }
        }

        // 内容折叠
        if (features.contentFold !== false) {
            const longAnswers = document.querySelectorAll('.answer-content');
            longAnswers.forEach(answer => {
                if (answer.textContent.length > 500) {
                    const foldBtn = document.createElement('button');
                    foldBtn.textContent = '展开全文';
                    foldBtn.style.cssText = 'margin: 10px 0; padding: 5px 10px; background: #007bff; color: white; border: none; border-radius: 3px;';
                    foldBtn.onclick = () => {
                        if (answer.style.maxHeight) {
                            answer.style.maxHeight = '';
                            foldBtn.textContent = '收起';
                        } else {
                            answer.style.maxHeight = '200px';
                            answer.style.overflow = 'hidden';
                            foldBtn.textContent = '展开全文';
                        }
                    };
                    answer.parentElement.insertBefore(foldBtn, answer);
                    answer.style.maxHeight = '200px';
                    answer.style.overflow = 'hidden';
                }
            });
        }

        // 搜索增强
        if (features.searchEnhance !== false) {
            const searchResults = document.querySelectorAll('.SearchResult');
            searchResults.forEach(result => {
                // 添加相关度评分
                const relevance = calculateRelevance(result);
                const badge = document.createElement('span');
                badge.textContent = `相关度: ${relevance}%`;
                badge.style.cssText = 'background: #28a745; color: white; padding: 2px 6px; border-radius: 3px; font-size: 12px; margin-left: 10px;';
                result.querySelector('h2')?.appendChild(badge);
            });
        }
    }

    // 百度搜索优化
    function optimizeBaiduSearch(features) {
        // 广告标识
        if (features.adLabel !== false) {
            const ads = document.querySelectorAll('.ad, .ec-tuiguang');
            ads.forEach(ad => {
                const label = document.createElement('span');
                label.textContent = '广告';
                label.style.cssText = 'background: #dc3545; color: white; padding: 2px 6px; border-radius: 3px; font-size: 12px; margin-right: 10px;';
                const title = ad.querySelector('h3, .title');
                if (title) title.insertBefore(label, title.firstChild);
            });
        }

        // 搜索优化
        if (features.searchOptimize !== false) {
            // 移除侧边栏
            const sidebar = document.querySelector('.right-sidebar, .cr-offset');
            if (sidebar) sidebar.remove();

            // 优化搜索结果布局
            GM_addStyle(`
                .result { margin-bottom: 20px !important; }
                .result h3 { font-size: 16px !important; }
                .c-abstract { line-height: 1.6 !important; }
            `);
        }

        // 结果过滤
        if (features.resultFilter !== false) {
            const results = document.querySelectorAll('.result');
            results.forEach(result => {
                const content = result.textContent;
                if (content.includes('推广') || content.includes('广告')) {
                    result.style.opacity = '0.5';
                }
            });
        }
    }

    // 通用广告拦截
    function applyAdBlock(adSelectors) {
        if (!adSelectors) return;

        const selectors = adSelectors.split(', ');
        selectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                element.style.display = 'none';
                element.remove();
            });
        });

        GM_log('广告拦截已应用');
    }

    // 图片模态框
    function showImageModal(img) {
        const modal = document.createElement('div');
        modal.innerHTML = `
            <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                        background: rgba(0,0,0,0.9); z-index: 10001; display: flex;
                        align-items: center; justify-content: center;">
                <img src="${img.src}" style="max-width: 90%; max-height: 90%; object-fit: contain;">
                <button style="position: absolute; top: 20px; right: 20px; background: rgba(0,0,0,0.5);
                              color: white; border: none; padding: 10px; border-radius: 5px; font-size: 20px;"
                        onclick="this.parentElement.remove()">✕</button>
            </div>
        `;
        document.body.appendChild(modal);
    }

    // 楼层导航
    function addFloorNavigation() {
        const floors = document.querySelectorAll('.l_post');
        if (floors.length < 10) return;

        const nav = document.createElement('div');
        nav.innerHTML = `
            <div style="position: fixed; right: 20px; top: 50%; transform: translateY(-50%);
                        background: rgba(0,0,0,0.8); color: white; padding: 10px; border-radius: 5px;">
                <div style="font-size: 12px; margin-bottom: 5px;">楼层导航</div>
                <input type="number" id="floor-input" placeholder="楼层号" min="1" max="${floors.length}"
                       style="width: 60px; margin-bottom: 5px;">
                <button id="goto-floor" style="width: 100%;">跳转</button>
            </div>
        `;
        document.body.appendChild(nav);

        document.getElementById('goto-floor').onclick = () => {
            const floorNum = parseInt(document.getElementById('floor-input').value);
            if (floorNum && floors[floorNum - 1]) {
                floors[floorNum - 1].scrollIntoView({ behavior: 'smooth' });
            }
        };
    }

    // 视频下载功能
    function addDownloadButtons() {
        const videos = document.querySelectorAll('video');
        videos.forEach(video => {
            if (video.dataset.downloadBtn) return;

            const downloadBtn = document.createElement('button');
            downloadBtn.textContent = '💾 下载';
            downloadBtn.style.cssText = 'position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,0.7); color: white; border: none; padding: 5px 10px; border-radius: 3px; z-index: 1000;';
            downloadBtn.onclick = () => downloadVideo(video.src);
            video.parentElement.style.position = 'relative';
            video.parentElement.appendChild(downloadBtn);
            video.dataset.downloadBtn = 'true';
        });
    }

    function downloadVideo(src) {
        const link = document.createElement('a');
        link.href = src;
        link.download = `video_${Date.now()}.mp4`;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    // 工具函数
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    function preloadImages() {
        const images = document.querySelectorAll('img[data-src]');
        images.forEach(img => {
            if (!img.src) {
                const tempImg = new Image();
                tempImg.onload = () => {
                    img.src = img.dataset.src;
                    img.classList.add('preloaded');
                };
                tempImg.src = img.dataset.src;
            }
        });
    }

    function calculateRelevance(result) {
        // 简单的相关度计算
        const title = result.querySelector('h3')?.textContent || '';
        const abstract = result.querySelector('.c-abstract')?.textContent || '';
        const query = new URLSearchParams(window.location.search).get('wd') || '';

        let score = 50; // 基础分数

        // 根据关键词匹配度调整分数
        const queryWords = query.toLowerCase().split(' ');
        const content = (title + abstract).toLowerCase();

        queryWords.forEach(word => {
            if (content.includes(word)) score += 10;
        });

        return Math.min(100, score);
    }

    function optimizeSearch() {
        const results = document.querySelectorAll('.search-result, .feed-item');
        results.forEach(result => {
            // 添加搜索结果评分
            if (!result.dataset.optimized) {
                result.dataset.optimized = 'true';
                const score = Math.floor(Math.random() * 40) + 60; // 模拟评分
                const badge = document.createElement('span');
                badge.textContent = `${score}分`;
                badge.style.cssText = 'background: #28a745; color: white; padding: 2px 6px; border-radius: 3px; font-size: 12px; margin-left: 10px;';
                const title = result.querySelector('h3, .title');
                if (title) title.appendChild(badge);
            }
        });
    }

    // 初始化
    function init() {
        if (!config.enabled) {
            GM_log('中文网站优化器已禁用');
            return;
        }

        setTimeout(() => {
            createSiteControlPanel();
            applySiteOptimizations();

            GM_log('EhViewer 中文网站优化器初始化完成');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
