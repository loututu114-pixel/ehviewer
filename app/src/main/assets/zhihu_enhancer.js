// ==UserScript==
// @name         EhViewer 知乎增强器
// @namespace    http://ehviewer.com/
// @version      2.5.0
// @description  深度优化知乎体验：内容过滤、搜索增强、阅读体验优化、智能推荐
// @author       EhViewer Team
// @match        *://*.zhihu.com/*
// @exclude      *://*.google.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 知乎增强器已启动');

    // 配置选项
    const config = {
        enabled: GM_getValue('zhihuEnabled', true),
        adBlock: GM_getValue('zhihuAdBlock', true),
        contentFilter: GM_getValue('zhihuContentFilter', true),
        searchEnhance: GM_getValue('zhihuSearchEnhance', true),
        answerOptimize: GM_getValue('zhihuAnswerOptimize', true),
        readingMode: GM_getValue('zhihuReadingMode', false),
        smartRecommend: GM_getValue('zhihuSmartRecommend', false),
        keyboardShortcuts: GM_getValue('zhihuKeyboardShortcuts', true),
        autoExpand: GM_getValue('zhihuAutoExpand', true),
        hideSidebar: GM_getValue('zhihuHideSidebar', false),
        nightMode: GM_getValue('zhihuNightMode', false)
    };

    // 创建知乎专用控制面板
    function createZhihuControlPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-zhihu-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #0084FF, #44C8F5); color: white;
                        padding: 12px; border-radius: 8px; font-size: 11px; font-family: Arial;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3); min-width: 180px;">
                <div style="display: flex; align-items: center; margin-bottom: 8px;">
                    <span style="font-size: 16px; margin-right: 6px;">💡</span>
                    <strong>知乎增强器</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-bottom: 8px;">
                    <button id="toggle-ads" title="广告拦截">🚫</button>
                    <button id="toggle-filter" title="内容过滤">🎯</button>
                    <button id="reading-mode" title="阅读模式">📖</button>
                    <button id="night-mode" title="夜间模式">🌙</button>
                    <button id="expand-all" title="展开全文">📄</button>
                    <button id="settings-btn" title="设置">⚙️</button>
                </div>
                <div style="font-size: 9px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 4px;">
                    R阅读模式 | E展开 | N夜间 | S搜索
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('toggle-ads').onclick = () => toggleFeature('adBlock');
        document.getElementById('toggle-filter').onclick = () => toggleFeature('contentFilter');
        document.getElementById('reading-mode').onclick = toggleReadingMode;
        document.getElementById('night-mode').onclick = toggleNightMode;
        document.getElementById('expand-all').onclick = expandAllAnswers;
        document.getElementById('settings-btn').onclick = showZhihuSettings;
    }

    // 深度广告拦截
    function advancedAdBlock() {
        if (!config.adBlock) return;

        GM_addStyle(`
            /* 知乎广告拦截 */
            .AdItem, .advertisement, .sponsor, .promotion,
            .recommend-ad, .feed-ad, .sidebar-ad,
            [class*="ad"], [id*="ad"], [data-ad],
            .Pc-word, .HotQuestions-item-ad,
            .VideoAnswerPlayer-ad, .ContentItem-actions-ad {
                display: none !important;
                visibility: hidden !important;
            }

            /* 隐藏推广标识 */
            .KfeCollection-PcWordCard,
            .PcWord-content,
            .HotQuestions-item[data-za-extra-module*="Ad"] {
                display: none !important;
            }

            /* 隐藏商业内容 */
            .VideoAnswerPlayer-commercial,
            .ContentItem-commercial {
                display: none !important;
            }
        `);

        // 动态移除广告
        const adObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === 1) {
                            removeAdsFromElement(node);
                        }
                    });
                }
            });
        });

        adObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        removeAdsFromElement(document.body);
    }

    function removeAdsFromElement(root) {
        const adSelectors = [
            '.AdItem', '.advertisement', '.sponsor', '.promotion',
            '.Pc-word', '.HotQuestions-item-ad', '.recommend-ad'
        ];

        adSelectors.forEach(selector => {
            const elements = root.querySelectorAll(selector);
            elements.forEach(element => {
                element.remove();
                GM_log('已移除知乎广告: ' + selector);
            });
        });
    }

    // 智能内容过滤
    function smartContentFilter() {
        if (!config.contentFilter) return;

        // 过滤低质量内容
        const answers = document.querySelectorAll('.AnswerItem, .ContentItem');
        answers.forEach(answer => {
            const content = answer.textContent;
            const voteCount = parseInt(answer.querySelector('.VoteButton')?.textContent || '0');

            // 过滤条件
            if (shouldFilterAnswer(content, voteCount)) {
                answer.style.opacity = '0.3';
                answer.style.maxHeight = '100px';
                answer.style.overflow = 'hidden';

                // 添加"显示"按钮
                if (!answer.querySelector('.filter-notice')) {
                    const notice = document.createElement('div');
                    notice.className = 'filter-notice';
                    notice.innerHTML = `
                        <div style="text-align: center; padding: 10px; color: #999;">
                            此回答已被过滤 (赞数: ${voteCount})
                            <button onclick="this.parentElement.parentElement.style.opacity='1';
                                           this.parentElement.parentElement.style.maxHeight='none';
                                           this.parentElement.remove()">
                                显示内容
                            </button>
                        </div>
                    `;
                    answer.appendChild(notice);
                }
            }
        });

        // 过滤问题
        filterQuestions();

        // 过滤评论
        filterComments();
    }

    function shouldFilterAnswer(content, voteCount) {
        // 过滤条件
        if (voteCount < 10) return true; // 赞数太少
        if (content.length < 50) return true; // 内容太短
        if (content.includes('复制') || content.includes('转载')) return true; // 复制内容
        if (content.match(/(.)\1{10,}/)) return true; // 重复字符太多
        if (content.includes('广告') || content.includes('推广')) return true; // 广告内容

        return false;
    }

    function filterQuestions() {
        const questions = document.querySelectorAll('.HotQuestions-item, .QuestionItem');
        questions.forEach(question => {
            const title = question.textContent;
            const followCount = parseInt(question.querySelector('.NumberBoard-itemValue')?.textContent || '0');

            if (followCount < 100) {
                question.style.opacity = '0.5';
            }
        });
    }

    function filterComments() {
        const comments = document.querySelectorAll('.CommentItem, .SubCommentItem');
        comments.forEach(comment => {
            const content = comment.textContent;
            const likeCount = parseInt(comment.querySelector('.CommentItem-likeCount')?.textContent || '0');

            if (likeCount < 5 && content.length < 20) {
                comment.style.display = 'none';
            }
        });
    }

    // 搜索增强
    function enhanceSearch() {
        if (!config.searchEnhance) return;

        const searchInput = document.querySelector('input[placeholder*="搜索"]');
        if (searchInput) {
            // 添加搜索建议
            searchInput.addEventListener('input', debounce(showSearchSuggestions, 300));
            searchInput.addEventListener('keydown', handleSearchKeydown);
        }

        // 优化搜索结果
        optimizeSearchResults();
    }

    function showSearchSuggestions(e) {
        const input = e.target;
        const value = input.value.trim();
        if (value.length < 2) return;

        // 创建建议下拉框
        let suggestionsBox = document.getElementById('search-suggestions');
        if (!suggestionsBox) {
            suggestionsBox = document.createElement('div');
            suggestionsBox.id = 'search-suggestions';
            suggestionsBox.style.cssText = `
                position: absolute; top: 100%; left: 0; right: 0;
                background: white; border: 1px solid #ddd; border-radius: 4px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1); z-index: 1000;
                max-height: 200px; overflow-y: auto;
            `;
            input.parentElement.style.position = 'relative';
            input.parentElement.appendChild(suggestionsBox);
        }

        // 生成建议
        const suggestions = generateSearchSuggestions(value);
        suggestionsBox.innerHTML = suggestions.map(suggestion => `
            <div style="padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #eee;
                       hover: background: #f5f5f5;" onclick="selectSuggestion('${suggestion}')">
                ${suggestion}
            </div>
        `).join('');
    }

    function generateSearchSuggestions(query) {
        const commonSuggestions = [
            query + ' 是什么',
            query + ' 怎么做',
            query + ' 教程',
            query + ' 推荐',
            query + ' 区别',
            query + ' 为什么'
        ];
        return commonSuggestions.slice(0, 5);
    }

    function handleSearchKeydown(e) {
        if (e.keyCode === 13) { // Enter
            const suggestions = document.getElementById('search-suggestions');
            if (suggestions && suggestions.children.length > 0) {
                selectSuggestion(suggestions.children[0].textContent.trim());
                e.preventDefault();
            }
        }
    }

    function selectSuggestion(suggestion) {
        const searchInput = document.querySelector('input[placeholder*="搜索"]');
        if (searchInput) {
            searchInput.value = suggestion;
            searchInput.form.submit();
        }
        document.getElementById('search-suggestions')?.remove();
    }

    function optimizeSearchResults() {
        // 为搜索结果添加评分
        const results = document.querySelectorAll('.SearchResult, .ContentItem');
        results.forEach(result => {
            if (!result.dataset.scored) {
                result.dataset.scored = 'true';
                const score = calculateResultScore(result);
                addResultScore(result, score);
            }
        });
    }

    function calculateResultScore(result) {
        let score = 50;

        // 赞数评分
        const voteCount = parseInt(result.querySelector('.VoteButton')?.textContent || '0');
        score += Math.min(voteCount / 10, 30);

        // 评论数评分
        const commentCount = parseInt(result.querySelector('.CommentItem-count')?.textContent || '0');
        score += Math.min(commentCount / 5, 20);

        // 作者认证加分
        if (result.querySelector('.AuthorInfo-badge')) score += 10;

        return Math.min(100, score);
    }

    function addResultScore(result, score) {
        const scoreBadge = document.createElement('span');
        scoreBadge.textContent = `质量: ${score}分`;
        scoreBadge.style.cssText = `
            position: absolute; top: 10px; right: 10px;
            background: ${score > 70 ? '#28a745' : score > 40 ? '#ffc107' : '#dc3545'};
            color: white; padding: 2px 6px; border-radius: 3px;
            font-size: 10px; z-index: 100;
        `;
        result.style.position = 'relative';
        result.appendChild(scoreBadge);
    }

    // 答案优化
    function optimizeAnswers() {
        if (!config.answerOptimize) return;

        // 按赞数排序答案
        sortAnswersByVotes();

        // 优化答案布局
        enhanceAnswerLayout();

        // 添加答案统计
        addAnswerStats();
    }

    function sortAnswersByVotes() {
        const answerList = document.querySelector('.AnswerList, .ContentList');
        if (!answerList) return;

        const answers = Array.from(answerList.children).filter(child =>
            child.classList.contains('AnswerItem') || child.classList.contains('ContentItem')
        );

        answers.sort((a, b) => {
            const aVotes = parseInt(a.querySelector('.VoteButton')?.textContent || '0');
            const bVotes = parseInt(b.querySelector('.VoteButton')?.textContent || '0');
            return bVotes - aVotes;
        });

        answers.forEach(answer => answerList.appendChild(answer));
        GM_log('答案已按赞数排序');
    }

    function enhanceAnswerLayout() {
        GM_addStyle(`
            .AnswerItem, .ContentItem {
                margin-bottom: 20px !important;
                border-radius: 8px !important;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
                transition: transform 0.2s ease !important;
            }

            .AnswerItem:hover, .ContentItem:hover {
                transform: translateY(-2px) !important;
            }

            .VoteButton {
                background: linear-gradient(135deg, #0084FF, #44C8F5) !important;
                color: white !important;
                border: none !important;
                border-radius: 20px !important;
                padding: 6px 12px !important;
            }
        `);
    }

    function addAnswerStats() {
        const answers = document.querySelectorAll('.AnswerItem, .ContentItem');
        answers.forEach(answer => {
            if (!answer.dataset.statsAdded) {
                answer.dataset.statsAdded = 'true';

                const voteCount = parseInt(answer.querySelector('.VoteButton')?.textContent || '0');
                const commentCount = parseInt(answer.querySelector('.CommentItem-count')?.textContent || '0');

                const stats = document.createElement('div');
                stats.innerHTML = `
                    <div style="display: flex; gap: 15px; font-size: 12px; color: #666; margin-top: 10px;">
                        <span>👍 ${voteCount} 赞</span>
                        <span>💬 ${commentCount} 评论</span>
                        <span>📊 质量评分: ${calculateResultScore(answer)}分</span>
                    </div>
                `;
                answer.appendChild(stats);
            }
        });
    }

    // 阅读模式
    function toggleReadingMode() {
        config.readingMode = !config.readingMode;
        GM_setValue('zhihuReadingMode', config.readingMode);

        if (config.readingMode) {
            enterReadingMode();
        } else {
            exitReadingMode();
        }
    }

    function enterReadingMode() {
        GM_addStyle(`
            .zhihu-reading-mode {
                max-width: 800px !important;
                margin: 0 auto !important;
                padding: 40px !important;
                font-size: 18px !important;
                line-height: 1.8 !important;
                background: #f8f9fa !important;
                box-shadow: 0 0 20px rgba(0,0,0,0.1) !important;
            }

            .zhihu-reading-mode .AnswerItem,
            .zhihu-reading-mode .ContentItem {
                box-shadow: none !important;
                border: none !important;
                margin-bottom: 30px !important;
            }

            /* 隐藏无关元素 */
            .zhihu-reading-mode .Sidebar,
            .zhihu-reading-mode .Header,
            .zhihu-reading-mode .Footer,
            .zhihu-reading-mode .RelatedQuestions {
                display: none !important;
            }
        `);

        document.body.classList.add('zhihu-reading-mode');
        GM_log('已进入阅读模式');
    }

    function exitReadingMode() {
        document.body.classList.remove('zhihu-reading-mode');
        GM_log('已退出阅读模式');
    }

    // 夜间模式
    function toggleNightMode() {
        config.nightMode = !config.nightMode;
        GM_setValue('zhihuNightMode', config.nightMode);

        if (config.nightMode) {
            applyNightMode();
        } else {
            removeNightMode();
        }
    }

    function applyNightMode() {
        GM_addStyle(`
            .zhihu-night-mode {
                background: #1a1a1a !important;
                color: #e0e0e0 !important;
            }

            .zhihu-night-mode .AnswerItem,
            .zhihu-night-mode .ContentItem {
                background: #2a2a2a !important;
                border-color: #404040 !important;
            }

            .zhihu-night-mode a {
                color: #4da6ff !important;
            }

            .zhihu-night-mode .VoteButton {
                background: #404040 !important;
                color: #e0e0e0 !important;
            }
        `);

        document.body.classList.add('zhihu-night-mode');
    }

    function removeNightMode() {
        document.body.classList.remove('zhihu-night-mode');
    }

    // 展开全文
    function expandAllAnswers() {
        const foldButtons = document.querySelectorAll('.collapse-btn, .fold-btn, button:contains("展开")');
        foldButtons.forEach(button => {
            if (button.offsetParent !== null) {
                button.click();
            }
        });

        // 自动展开答案
        const answers = document.querySelectorAll('.AnswerItem, .ContentItem');
        answers.forEach(answer => {
            const content = answer.querySelector('.RichContent, .RichText');
            if (content && content.style.maxHeight) {
                content.style.maxHeight = 'none';
            }
        });

        GM_log('已展开所有答案');
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        if (!config.keyboardShortcuts) return;

        document.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            switch (e.keyCode) {
                case 82: // R - 阅读模式
                    e.preventDefault();
                    toggleReadingMode();
                    break;
                case 69: // E - 展开全文
                    e.preventDefault();
                    expandAllAnswers();
                    break;
                case 78: // N - 夜间模式
                    e.preventDefault();
                    toggleNightMode();
                    break;
                case 83: // S - 搜索增强
                    e.preventDefault();
                    focusSearchInput();
                    break;
                case 70: // F - 内容过滤
                    e.preventDefault();
                    toggleFeature('contentFilter');
                    break;
            }
        });
    }

    function focusSearchInput() {
        const searchInput = document.querySelector('input[placeholder*="搜索"]');
        if (searchInput) {
            searchInput.focus();
            searchInput.select();
        }
    }

    // 自动展开
    function autoExpandContent() {
        if (!config.autoExpand) return;

        // 自动展开被折叠的内容
        setInterval(() => {
            const foldButtons = document.querySelectorAll('.collapse-btn:not(.expanded)');
            foldButtons.forEach(button => {
                if (button.offsetParent !== null) {
                    button.classList.add('expanded');
                    button.click();
                }
            });
        }, 2000);
    }

    // 隐藏侧边栏
    function hideSidebar() {
        if (!config.hideSidebar) return;

        GM_addStyle(`
            .Sidebar, .RightSidebar, .Question-sideColumn {
                display: none !important;
            }

            .Question-main, .Answer-main {
                width: 100% !important;
                max-width: none !important;
            }
        `);
    }

    // 智能推荐
    function smartRecommend() {
        if (!config.smartRecommend) return;

        // 分析用户兴趣
        const userInterests = analyzeUserInterests();

        // 优化推荐内容
        optimizeRecommendations(userInterests);
    }

    function analyzeUserInterests() {
        const interests = {
            categories: [],
            keywords: []
        };

        // 分析浏览历史和点赞记录
        const likedAnswers = document.querySelectorAll('.VoteButton.active');
        likedAnswers.forEach(button => {
            const answer = button.closest('.AnswerItem, .ContentItem');
            if (answer) {
                const content = answer.textContent;
                // 提取关键词
                interests.keywords.push(...extractKeywords(content));
            }
        });

        return interests;
    }

    function extractKeywords(text) {
        // 简单的关键词提取
        const keywords = [];
        const commonWords = ['的', '了', '和', '是', '在', '有', '这', '那', '我', '你'];

        const words = text.split(/\s+/);
        words.forEach(word => {
            if (word.length > 1 && !commonWords.includes(word)) {
                keywords.push(word);
            }
        });

        return keywords.slice(0, 5);
    }

    function optimizeRecommendations(interests) {
        // 优化相关问题推荐
        const recommendations = document.querySelectorAll('.RelatedQuestions-item, .Recommend-item');
        recommendations.forEach(item => {
            const title = item.textContent;
            let relevanceScore = 0;

            interests.keywords.forEach(keyword => {
                if (title.includes(keyword)) {
                    relevanceScore += 20;
                }
            });

            if (relevanceScore > 0) {
                item.style.borderLeft = `4px solid ${relevanceScore > 40 ? '#28a745' : '#ffc107'}`;
            }
        });
    }

    // 设置面板
    function showZhihuSettings() {
        const settings = document.createElement('div');
        settings.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                        box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 500px;">
                <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">知乎增强器设置</h3>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    ${Object.entries(config).slice(1).map(([key, value]) => `
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-${key}" style="margin-right: 8px;" ${value ? 'checked' : ''}>
                            ${getSettingName(key)}
                        </label>
                    `).join('')}
                </div>

                <div style="text-align: right;">
                    <button id="save-zhihu-settings" style="padding: 10px 20px;
                                                    background: #0084FF; color: white; border: none;
                                                    border-radius: 6px; cursor: pointer; margin-right: 10px;">
                        保存设置
                    </button>
                    <button id="close-zhihu-settings" style="padding: 10px 20px;
                                                     background: #6c757d; color: white; border: none;
                                                     border-radius: 6px; cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(settings);

        document.getElementById('save-zhihu-settings').onclick = saveZhihuSettings;
        document.getElementById('close-zhihu-settings').onclick = () => settings.remove();
    }

    function getSettingName(key) {
        const names = {
            adBlock: '广告拦截',
            contentFilter: '内容过滤',
            searchEnhance: '搜索增强',
            answerOptimize: '答案优化',
            readingMode: '阅读模式',
            smartRecommend: '智能推荐',
            keyboardShortcuts: '键盘快捷键',
            autoExpand: '自动展开',
            hideSidebar: '隐藏侧边栏',
            nightMode: '夜间模式'
        };
        return names[key] || key;
    }

    function saveZhihuSettings() {
        Object.keys(config).forEach(key => {
            if (key !== 'enabled') {
                const checkbox = document.getElementById(`setting-${key}`);
                config[key] = checkbox.checked;
                GM_setValue('zhihu' + key.charAt(0).toUpperCase() + key.slice(1), config[key]);
            }
        });

        location.reload();
    }

    // 功能开关
    function toggleFeature(featureName) {
        config[featureName] = !config[featureName];
        GM_setValue('zhihu' + featureName.charAt(0).toUpperCase() + featureName.slice(1), config[featureName]);

        applyFeatures();
        GM_log(`${featureName} ${config[featureName] ? '已启用' : '已禁用'}`);
    }

    function applyFeatures() {
        if (config.adBlock) advancedAdBlock();
        if (config.contentFilter) smartContentFilter();
        if (config.searchEnhance) enhanceSearch();
        if (config.answerOptimize) optimizeAnswers();
        if (config.autoExpand) autoExpandContent();
        if (config.hideSidebar) hideSidebar();
        if (config.smartRecommend) smartRecommend();
        if (config.nightMode) applyNightMode();
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

    // 初始化
    function init() {
        if (!config.enabled) {
            GM_log('知乎增强器已禁用');
            return;
        }

        setTimeout(() => {
            createZhihuControlPanel();
            applyFeatures();
            setupKeyboardShortcuts();

            GM_log('EhViewer 知乎增强器初始化完成');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
