// ==UserScript==
// @name         EhViewer 下载站深度优化器
// @namespace    http://ehviewer.com/
// @version      2.0.0
// @description  深度优化下载站体验：智能拦截360等垃圾广告、优化下载链接、安全下载保护
// @author       EhViewer Team
// @match        *://*.huajun.com/*
// @match        *://*.pcdown.com/*
// @match        *://*.skycn.com/*
// @match        *://*.greenxf.com/*
// @match        *://*.softonic.com/*
// @match        *://*.download.com/*
// @match        *://*.filehippo.com/*
// @match        *://*.softpedia.com/*
// @match        *://*.majorgeeks.com/*
// @match        *://*.snapfiles.com/*
// @match        *://*.soft32.com/*
// @match        *://*.brothersoft.com/*
// @match        *://*.uptodown.com/*
// @match        *://*.malavida.com/*
// @match        *://*.soft82.com/*
// @exclude      *://*.google.com/*
// @exclude      *://*.baidu.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 下载站深度优化器已启动');

    // 配置选项
    const config = {
        enabled: GM_getValue('downloadSitesEnabled', true),
        block360Ads: GM_getValue('block360Ads', true),
        blockMalware: GM_getValue('blockMalware', true),
        optimizeDownloads: GM_getValue('optimizeDownloads', true),
        safeDownload: GM_getValue('safeDownload', true),
        cleanInterface: GM_getValue('cleanInterface', true),
        keyboardShortcuts: GM_getValue('downloadKeyboardShortcuts', true),
        autoExtractLinks: GM_getValue('autoExtractLinks', true)
    };

    // 垃圾软件关键词库
    const malwareKeywords = [
        // 360系列
        '360安全卫士', '360安全浏览器', '360杀毒', '360加速器', '360压缩',
        '360安全下载', '360手机卫士', '360驱动', '360手游', '360清理大师',
        '360软件管家', '360金山毒霸', '360上网导航',

        // 其他恶意软件
        '金山毒霸', '瑞星杀毒', '江民杀毒', '卡巴斯基', '诺顿',
        '迈克菲', '趋势科技', 'Avast', 'AVG', 'Avira',
        '百度卫士', '百度杀毒', '百度浏览器', '百度输入法',
        '腾讯电脑管家', '腾讯视频', 'QQ浏览器', '微信',
        '搜狗输入法', '搜狗浏览器', '搜狗高速下载',
        '猎豹浏览器', '猎豹清理大师', '金山卫士',
        '2345加速浏览器', '2345王牌助手', '2345好压',
        '傲游浏览器', '世界之窗浏览器', '闪游浏览器',

        // 捆绑软件
        '迅雷看看', '迅雷影音', '迅雷游戏', '迅雷直播',
        '暴风影音', '暴风云', '暴风游戏',
        '快压', '好压', '360压缩', '2345好压',
        '驱动精灵', '驱动人生', '驱动总裁',
        '鲁大师', '电脑管家', '软件管家',

        // 其他推广
        '安全下载', '高速下载', '下载大师', '下载加速器',
        '游戏加速器', '网游加速器', '手游加速器',
        '系统优化', '系统清理', '系统加速',
        '免费杀毒', '免费杀毒软件', '免费安全软件'
    ];

    // 网站特定配置
    const siteConfig = {
        'huajun.com': {
            name: '华军软件园',
            adSelectors: ['.ad', '.gg', '.guanggao', '.sponsor', '.download-ad'],
            downloadSelectors: ['.download-btn', '.down-btn', '.download-link'],
            malwarePatterns: ['360', '金山', '百度', '腾讯']
        },
        'pcdown.com': {
            name: '太平洋下载',
            adSelectors: ['.ad', '.advertisement', '.sponsor', '.promotion'],
            downloadSelectors: ['.download-button', '.down-link', '.soft-down'],
            malwarePatterns: ['360', '百度', '金山', '搜狗']
        },
        'skycn.com': {
            name: '天空软件站',
            adSelectors: ['.ad', '.gg', '.guanggao', '.sponsor'],
            downloadSelectors: ['.download', '.down', '.softdownload'],
            malwarePatterns: ['360', '金山', '百度']
        },
        'greenxf.com': {
            name: '绿软联盟',
            adSelectors: ['.ad', '.advertisement', '.sponsor'],
            downloadSelectors: ['.download-btn', '.down-link'],
            malwarePatterns: ['360', '金山', '百度', '腾讯']
        }
    };

    // 获取当前网站配置
    function getCurrentSiteConfig() {
        const hostname = window.location.hostname;
        for (const [domain, config] of Object.entries(siteConfig)) {
            if (hostname.includes(domain)) {
                return config;
            }
        }
        return null;
    }

    // 创建下载站专用控制面板
    function createDownloadControlPanel() {
        const siteConfig = getCurrentSiteConfig();
        const siteName = siteConfig ? siteConfig.name : '下载站';

        const panel = document.createElement('div');
        panel.id = 'ehviewer-download-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #FF6B6B, #4ECDC4); color: white;
                        padding: 12px; border-radius: 8px; font-size: 11px; font-family: Arial;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3); min-width: 200px;">
                <div style="display: flex; align-items: center; margin-bottom: 8px;">
                    <span style="font-size: 16px; margin-right: 6px;">🛡️</span>
                    <strong>${siteName} 优化器</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-bottom: 8px;">
                    <button id="block-malware" title="拦截恶意软件">🚫</button>
                    <button id="clean-ads" title="清理广告">🧹</button>
                    <button id="extract-links" title="提取链接">🔗</button>
                    <button id="safe-download" title="安全下载">🛡️</button>
                    <button id="optimize-ui" title="界面优化">⚡</button>
                    <button id="settings-btn" title="设置">⚙️</button>
                </div>
                <div style="font-size: 9px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 4px;">
                    S安全下载 | E提取链接 | C清理界面
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('block-malware').onclick = () => blockMalwareAds();
        document.getElementById('clean-ads').onclick = () => cleanAds();
        document.getElementById('extract-links').onclick = () => extractDownloadLinks();
        document.getElementById('safe-download').onclick = () => enableSafeDownload();
        document.getElementById('optimize-ui').onclick = () => optimizeInterface();
        document.getElementById('settings-btn').onclick = showDownloadSettings;
    }

    // 深度拦截360等恶意软件广告
    function blockMalwareAds() {
        if (!config.blockMalware) return;

        GM_addStyle(`
            /* 360安全卫士等恶意软件广告拦截 */
            .ad-360, .ad-360safe, .ad-360browser, .ad-360antivirus,
            .ad-kingsoft, .ad-baidu, .ad-qq, .ad-sogou, .ad-liebao,
            .ad-2345, .ad-xunlei, .ad-baidu, .ad-tencent,
            [class*="360"], [id*="360"], [class*="kingsoft"], [id*="kingsoft"],
            [class*="baidu"], [id*="baidu"], [class*="qq"], [id*="qq"],
            [class*="sogou"], [id*="sogou"], [class*="liebao"], [id*="liebao"],
            [class*="2345"], [id*="2345"], [class*="xunlei"], [id*="xunlei"] {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                overflow: hidden !important;
            }

            /* 隐藏恶意软件下载链接 */
            a[href*="360safe"], a[href*="360.cn"], a[href*="kingsoft"],
            a[href*="baidu.com"], a[href*="qq.com"], a[href*="sogou.com"],
            a[href*="liebao.cn"], a[href*="2345.com"], a[href*="xunlei.com"] {
                display: none !important;
            }

            /* 隐藏捆绑下载提示 */
            .bundle-download, .bundled-software, .recommended-software,
            .additional-software, .optional-software {
                display: none !important;
            }
        `);

        // 动态移除恶意软件元素
        removeMalwareElements();

        // 拦截恶意软件链接点击
        interceptMalwareLinks();

        GM_log('恶意软件广告拦截已启用');
    }

    function removeMalwareElements() {
        // 移除包含恶意软件关键词的元素
        const allElements = document.querySelectorAll('*');
        allElements.forEach(element => {
            const text = element.textContent || '';
            const hasMalwareKeyword = malwareKeywords.some(keyword =>
                text.includes(keyword)
            );

            if (hasMalwareKeyword && element.offsetParent) {
                // 检查是否是下载相关的重要元素
                if (!isImportantDownloadElement(element)) {
                    element.style.display = 'none';
                    GM_log('移除恶意软件元素: ' + text.substring(0, 50) + '...');
                }
            }
        });
    }

    function isImportantDownloadElement(element) {
        // 检查是否是重要的下载元素
        const importantSelectors = [
            '.download', '.down', '.btn-download', '.download-btn',
            '.software-download', '.file-download'
        ];

        for (const selector of importantSelectors) {
            if (element.matches(selector) || element.closest(selector)) {
                return true;
            }
        }

        return false;
    }

    function interceptMalwareLinks() {
        document.addEventListener('click', function(e) {
            const target = e.target.closest('a');
            if (target && target.href) {
                const href = target.href.toLowerCase();
                const isMalwareLink = malwareKeywords.some(keyword =>
                    href.includes(keyword.toLowerCase().replace(/\s+/g, ''))
                );

                if (isMalwareLink) {
                    e.preventDefault();
                    e.stopPropagation();
                    showWarningNotification('已拦截恶意软件下载链接！');
                    GM_log('拦截恶意软件链接: ' + href);
                }
            }
        }, true);
    }

    // 清理所有广告
    function cleanAds() {
        const siteConfig = getCurrentSiteConfig();
        const adSelectors = siteConfig ? siteConfig.adSelectors : [
            '.ad', '.ads', '.advertisement', '.gg', '.guanggao',
            '.sponsor', '.promotion', '.banner', '.popup'
        ];

        adSelectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                element.remove();
            });
        });

        // 移除弹窗
        removePopups();

        // 移除固定广告
        removeFixedAds();

        GM_log('广告清理完成');
        showNotification('广告清理完成！');
    }

    function removePopups() {
        const popups = document.querySelectorAll('.popup, .modal, .dialog, .overlay');
        popups.forEach(popup => {
            if (popup.offsetParent) {
                popup.remove();
            }
        });
    }

    function removeFixedAds() {
        const fixedElements = document.querySelectorAll('*[style*="position: fixed"], *[style*="position: absolute"]');
        fixedElements.forEach(element => {
            const rect = element.getBoundingClientRect();
            if (rect.width > 100 && rect.height > 50) {
                // 检查是否可能是广告
                const text = element.textContent || '';
                if (text.includes('广告') || text.includes('下载') || text.includes('安装')) {
                    element.remove();
                }
            }
        });
    }

    // 提取和优化下载链接
    function extractDownloadLinks() {
        if (!config.autoExtractLinks) return;

        const downloadLinks = findDownloadLinks();
        const safeLinks = filterSafeLinks(downloadLinks);

        if (safeLinks.length > 0) {
            createDownloadPanel(safeLinks);
            GM_log('找到 ' + safeLinks.length + ' 个安全下载链接');
        } else {
            showWarningNotification('未找到安全的下载链接');
        }
    }

    function findDownloadLinks() {
        const siteConfig = getCurrentSiteConfig();
        const downloadSelectors = siteConfig ? siteConfig.downloadSelectors : [
            '.download', '.down', '.download-btn', '.down-btn',
            '.download-link', '.soft-download', 'a[href*="download"]',
            'a[href*="down"]', 'a[href*=".exe"]', 'a[href*=".zip"]',
            'a[href*=".rar"]', 'a[href*=".msi"]'
        ];

        const links = new Set();

        downloadSelectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                if (element.href) {
                    links.add({
                        url: element.href,
                        text: element.textContent.trim(),
                        element: element
                    });
                }
            });
        });

        return Array.from(links);
    }

    function filterSafeLinks(links) {
        return links.filter(link => {
            const url = link.url.toLowerCase();
            const text = link.text.toLowerCase();

            // 过滤恶意软件链接
            const isMalware = malwareKeywords.some(keyword =>
                url.includes(keyword.toLowerCase().replace(/\s+/g, '')) ||
                text.includes(keyword.toLowerCase())
            );

            // 检查文件扩展名
            const safeExtensions = ['.exe', '.zip', '.rar', '.msi', '.dmg', '.pkg', '.deb', '.rpm'];
            const hasSafeExtension = safeExtensions.some(ext => url.includes(ext));

            return !isMalware && hasSafeExtension;
        });
    }

    function createDownloadPanel(links) {
        const panel = document.createElement('div');
        panel.id = 'safe-download-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; border-radius: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.3);
                        z-index: 10001; max-width: 500px; max-height: 70vh; overflow-y: auto;">
                <div style="padding: 20px; border-bottom: 1px solid #eee;">
                    <h3 style="margin: 0 0 10px 0; color: #333;">🛡️ 安全下载链接</h3>
                    <p style="margin: 0; color: #666; font-size: 14px;">
                        已过滤掉 ${findDownloadLinks().length - links.length} 个可疑链接
                    </p>
                </div>
                <div style="padding: 20px;">
                    ${links.map((link, index) => `
                        <div style="margin-bottom: 12px; padding: 12px; border: 1px solid #e0e0e0;
                                  border-radius: 8px; display: flex; align-items: center; gap: 12px;">
                            <div style="flex: 1;">
                                <div style="font-weight: bold; color: #333; margin-bottom: 4px;">
                                    ${link.text || '下载链接 ' + (index + 1)}
                                </div>
                                <div style="font-size: 12px; color: #666; word-break: break-all;">
                                    ${link.url}
                                </div>
                            </div>
                            <button onclick="safeDownload('${link.url}', '${link.text}')"
                                    style="padding: 8px 16px; background: #28a745; color: white;
                                           border: none; border-radius: 6px; cursor: pointer;">
                                下载
                            </button>
                        </div>
                    `).join('')}
                </div>
                <div style="padding: 15px; border-top: 1px solid #eee; text-align: right;">
                    <button onclick="this.parentElement.parentElement.remove()"
                            style="padding: 8px 16px; background: #6c757d; color: white;
                                   border: none; border-radius: 6px; cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(panel);

        // 添加全局下载函数
        window.safeDownload = safeDownload;
    }

    function safeDownload(url, filename) {
        // 创建安全的下载链接
        const link = document.createElement('a');
        link.href = url;
        link.download = filename || 'download';
        link.style.display = 'none';

        // 添加下载属性以提高安全性
        link.setAttribute('rel', 'noopener noreferrer');
        link.setAttribute('target', '_blank');

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        showNotification('安全下载已开始，请检查下载文件夹');
        GM_log('安全下载: ' + url);
    }

    // 启用安全下载模式
    function enableSafeDownload() {
        if (!config.safeDownload) return;

        // 添加安全下载提示
        GM_addStyle(`
            .download-link, .download-btn, .down-btn {
                position: relative !important;
            }

            .download-link::before, .download-btn::before, .down-btn::before {
                content: '🛡️';
                position: absolute;
                top: -5px;
                right: -5px;
                background: #28a745;
                color: white;
                border-radius: 50%;
                width: 20px;
                height: 20px;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 10px;
                z-index: 10;
            }

            .malware-link {
                position: relative !important;
                opacity: 0.5 !important;
            }

            .malware-link::after {
                content: '⚠️ 可疑链接';
                position: absolute;
                top: 100%;
                left: 0;
                background: #dc3545;
                color: white;
                padding: 4px 8px;
                border-radius: 4px;
                font-size: 12px;
                white-space: nowrap;
                z-index: 1000;
            }
        `);

        // 标记可疑链接
        markSuspiciousLinks();

        showNotification('安全下载模式已启用');
    }

    function markSuspiciousLinks() {
        const links = document.querySelectorAll('a');
        links.forEach(link => {
            const href = link.href.toLowerCase();
            const isMalware = malwareKeywords.some(keyword =>
                href.includes(keyword.toLowerCase().replace(/\s+/g, ''))
            );

            if (isMalware) {
                link.classList.add('malware-link');
            } else if (href.includes('download') || href.includes('down') ||
                      href.match(/\.(exe|zip|rar|msi)$/i)) {
                link.classList.add('safe-download-link');
            }
        });
    }

    // 优化界面
    function optimizeInterface() {
        if (!config.cleanInterface) return;

        GM_addStyle(`
            /* 清理下载站界面 */
            .ad, .ads, .advertisement, .gg, .guanggao,
            .sponsor, .promotion, .banner, .popup, .modal,
            .sidebar-ad, .footer-ad, .header-ad {
                display: none !important;
            }

            /* 优化主要内容区域 */
            .main-content, .content, .software-info {
                max-width: 1200px !important;
                margin: 0 auto !important;
                padding: 20px !important;
            }

            /* 优化下载按钮样式 */
            .download-btn, .down-btn, .download-link {
                background: linear-gradient(135deg, #28a745, #20c997) !important;
                color: white !important;
                border: none !important;
                border-radius: 8px !important;
                padding: 12px 24px !important;
                font-size: 16px !important;
                font-weight: bold !important;
                text-decoration: none !important;
                display: inline-block !important;
                transition: transform 0.2s ease !important;
                box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3) !important;
            }

            .download-btn:hover, .down-btn:hover, .download-link:hover {
                transform: translateY(-2px) !important;
                box-shadow: 0 6px 20px rgba(40, 167, 69, 0.4) !important;
            }

            /* 隐藏不必要的元素 */
            .recommend-software, .related-software, .bundle-software,
            .optional-install, .additional-software {
                display: none !important;
            }

            /* 优化搜索框 */
            .search-input, .search-box input {
                width: 100% !important;
                padding: 12px 16px !important;
                border: 2px solid #e0e0e0 !important;
                border-radius: 8px !important;
                font-size: 16px !important;
            }

            .search-input:focus, .search-box input:focus {
                border-color: #007bff !important;
                box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1) !important;
            }
        `);

        // 重新排列元素布局
        reorganizeLayout();

        GM_log('界面优化完成');
        showNotification('界面优化完成！');
    }

    function reorganizeLayout() {
        // 将下载按钮移到显眼位置
        const downloadButtons = document.querySelectorAll('.download-btn, .down-btn');
        if (downloadButtons.length > 0) {
            const firstButton = downloadButtons[0];
            const buttonRect = firstButton.getBoundingClientRect();

            if (buttonRect.top > window.innerHeight / 2) {
                // 如果下载按钮在页面下方，滚动到可见位置
                firstButton.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        if (!config.keyboardShortcuts) return;

        document.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            switch (e.keyCode) {
                case 83: // S - 安全下载
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        enableSafeDownload();
                    }
                    break;
                case 69: // E - 提取链接
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        extractDownloadLinks();
                    }
                    break;
                case 67: // C - 清理界面
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        optimizeInterface();
                    }
                    break;
                case 77: // M - 拦截恶意软件
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        blockMalwareAds();
                    }
                    break;
                case 70: // F - 聚焦搜索框
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        focusSearchBox();
                    }
                    break;
            }
        });
    }

    function focusSearchBox() {
        const searchInputs = document.querySelectorAll('input[placeholder*="搜索"], input[type="search"], .search-input');
        if (searchInputs.length > 0) {
            searchInputs[0].focus();
            searchInputs[0].select();
        }
    }

    // 设置面板
    function showDownloadSettings() {
        const settings = document.createElement('div');
        settings.innerHTML = `
            <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                        background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                        box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 500px;">
                <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">下载站优化器设置</h3>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    ${Object.entries(config).slice(1).map(([key, value]) => `
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-${key}" style="margin-right: 8px;" ${value ? 'checked' : ''}>
                            ${getSettingName(key)}
                        </label>
                    `).join('')}
                </div>

                <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                    <h4 style="margin: 0 0 10px 0; color: #666;">高级选项</h4>
                    <label style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" id="setting-strict-mode" style="margin-right: 8px;" ${GM_getValue('strictMode', false) ? 'checked' : ''}>
                        严格模式（拦截更多可疑内容）
                    </label>
                    <label style="display: block;">
                        <input type="checkbox" id="setting-show-warnings" style="margin-right: 8px;" ${GM_getValue('showWarnings', true) ? 'checked' : ''}>
                        显示安全警告
                    </label>
                </div>

                <div style="text-align: right;">
                    <button id="reset-download-settings" style="margin-right: 10px; padding: 8px 15px;
                                                    background: #ffc107; color: black; border: none;
                                                    border-radius: 5px; cursor: pointer;">
                        重置设置
                    </button>
                    <button id="save-download-settings" style="padding: 8px 15px;
                                                    background: #FF6B6B; color: white; border: none;
                                                    border-radius: 5px; cursor: pointer;">
                        保存设置
                    </button>
                    <button id="close-download-settings" style="margin-left: 10px; padding: 8px 15px;
                                                     background: #6c757d; color: white; border: none;
                                                     border-radius: 5px; cursor: pointer;">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(settings);

        document.getElementById('save-download-settings').onclick = saveDownloadSettings;
        document.getElementById('reset-download-settings').onclick = resetDownloadSettings;
        document.getElementById('close-download-settings').onclick = () => settings.remove();
    }

    function getSettingName(key) {
        const names = {
            block360Ads: '拦截360广告',
            blockMalware: '拦截恶意软件',
            optimizeDownloads: '优化下载',
            safeDownload: '安全下载',
            cleanInterface: '清理界面',
            keyboardShortcuts: '键盘快捷键',
            autoExtractLinks: '自动提取链接'
        };
        return names[key] || key;
    }

    function saveDownloadSettings() {
        Object.keys(config).forEach(key => {
            if (key !== 'enabled') {
                const checkbox = document.getElementById(`setting-${key}`);
                config[key] = checkbox.checked;
                GM_setValue('download' + key.charAt(0).toUpperCase() + key.slice(1), config[key]);
            }
        });

        // 保存高级设置
        GM_setValue('strictMode', document.getElementById('setting-strict-mode').checked);
        GM_setValue('showWarnings', document.getElementById('setting-show-warnings').checked);

        location.reload();
    }

    function resetDownloadSettings() {
        if (confirm('确定要重置所有设置为默认值吗？')) {
            Object.keys(config).forEach(key => {
                GM_setValue('download' + key.charAt(0).toUpperCase() + key.slice(1), true);
            });
            GM_setValue('strictMode', false);
            GM_setValue('showWarnings', true);

            location.reload();
        }
    }

    // 通知系统
    function showNotification(message) {
        const notification = document.createElement('div');
        notification.innerHTML = `
            <div style="position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
                        background: #28a745; color: white; padding: 12px 24px;
                        border-radius: 8px; z-index: 10001; box-shadow: 0 4px 12px rgba(0,0,0,0.3);">
                ${message}
            </div>
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    function showWarningNotification(message) {
        const notification = document.createElement('div');
        notification.innerHTML = `
            <div style="position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
                        background: #dc3545; color: white; padding: 12px 24px;
                        border-radius: 8px; z-index: 10001; box-shadow: 0 4px 12px rgba(0,0,0,0.3);">
                ⚠️ ${message}
            </div>
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 5000);
    }

    // 页面分析和优化
    function analyzeAndOptimizePage() {
        // 检测页面类型
        const pageType = detectPageType();

        switch (pageType) {
            case 'software-list':
                optimizeSoftwareListPage();
                break;
            case 'software-detail':
                optimizeSoftwareDetailPage();
                break;
            case 'download-page':
                optimizeDownloadPage();
                break;
            case 'search-results':
                optimizeSearchResultsPage();
                break;
        }

        // 通用优化
        removeGenericAds();
        enhanceDownloadButtons();
    }

    function detectPageType() {
        const url = window.location.href.toLowerCase();
        const title = document.title.toLowerCase();

        if (url.includes('/soft/') || url.includes('/list/') || title.includes('软件下载')) {
            return 'software-list';
        } else if (url.includes('/soft/') && url.match(/\d+\.html$/)) {
            return 'software-detail';
        } else if (url.includes('download') || title.includes('下载')) {
            return 'download-page';
        } else if (url.includes('search') || url.includes('s?')) {
            return 'search-results';
        }

        return 'unknown';
    }

    function optimizeSoftwareListPage() {
        // 优化软件列表页面
        GM_addStyle(`
            .software-list, .soft-list, .download-list {
                display: grid !important;
                grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)) !important;
                gap: 20px !important;
                margin: 20px 0 !important;
            }

            .software-item, .soft-item {
                border: 1px solid #e0e0e0 !important;
                border-radius: 8px !important;
                padding: 15px !important;
                transition: transform 0.2s ease, box-shadow 0.2s ease !important;
            }

            .software-item:hover, .soft-item:hover {
                transform: translateY(-2px) !important;
                box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
            }
        `);
    }

    function optimizeSoftwareDetailPage() {
        // 优化软件详情页面
        GM_addStyle(`
            .software-info, .soft-info {
                max-width: 800px !important;
                margin: 0 auto !important;
                padding: 20px !important;
            }

            .download-section, .down-section {
                background: #f8f9fa !important;
                border-radius: 8px !important;
                padding: 20px !important;
                margin: 20px 0 !important;
                border: 2px solid #007bff !important;
            }
        `);

        // 自动滚动到下载区域
        setTimeout(() => {
            const downloadSection = document.querySelector('.download-section, .down-section');
            if (downloadSection) {
                downloadSection.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }, 1000);
    }

    function optimizeDownloadPage() {
        // 优化下载页面
        GM_addStyle(`
            .download-options, .down-options {
                display: flex !important;
                flex-direction: column !important;
                gap: 10px !important;
                margin: 20px 0 !important;
            }

            .download-option, .down-option {
                padding: 15px !important;
                border: 1px solid #e0e0e0 !important;
                border-radius: 8px !important;
                background: white !important;
                transition: all 0.2s ease !important;
            }

            .download-option:hover, .down-option:hover {
                border-color: #007bff !important;
                box-shadow: 0 2px 8px rgba(0, 123, 255, 0.1) !important;
            }
        `);
    }

    function optimizeSearchResultsPage() {
        // 优化搜索结果页面
        GM_addStyle(`
            .search-results, .search-list {
                max-width: 1000px !important;
                margin: 0 auto !important;
            }

            .search-item, .result-item {
                border-bottom: 1px solid #e0e0e0 !important;
                padding: 15px 0 !important;
                transition: background 0.2s ease !important;
            }

            .search-item:hover, .result-item:hover {
                background: #f8f9fa !important;
            }
        `);

        // 添加搜索结果评分
        const searchItems = document.querySelectorAll('.search-item, .result-item');
        searchItems.forEach((item, index) => {
            if (!item.dataset.scored) {
                item.dataset.scored = 'true';
                const score = calculateDownloadScore(item);
                addDownloadScore(item, score);
            }
        });
    }

    function calculateDownloadScore(item) {
        let score = 50;

        const text = item.textContent.toLowerCase();

        // 检查是否有恶意软件关键词
        const hasMalware = malwareKeywords.some(keyword =>
            text.includes(keyword.toLowerCase())
        );
        if (hasMalware) score -= 30;

        // 检查下载链接质量
        const links = item.querySelectorAll('a');
        links.forEach(link => {
            if (link.href.match(/\.(exe|zip|rar|msi)$/i)) {
                score += 10;
            }
        });

        // 检查是否有大小信息
        if (text.includes('mb') || text.includes('gb') || text.includes('kb')) {
            score += 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    function addDownloadScore(item, score) {
        const scoreBadge = document.createElement('span');
        scoreBadge.textContent = `${score}分`;
        scoreBadge.style.cssText = `
            position: absolute; top: 10px; right: 10px;
            background: ${score > 70 ? '#28a745' : score > 40 ? '#ffc107' : '#dc3545'};
            color: white; padding: 2px 6px; border-radius: 3px;
            font-size: 10px; z-index: 100;
        `;
        item.style.position = 'relative';
        item.appendChild(scoreBadge);
    }

    function removeGenericAds() {
        const genericAdSelectors = [
            '.ad', '.ads', '.advertisement', '.gg', '.guanggao',
            '.sponsor', '.promotion', '.banner', '.popup', '.modal',
            '.overlay', '.dialog', '.lightbox', '.tooltip'
        ];

        genericAdSelectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                element.remove();
            });
        });
    }

    function enhanceDownloadButtons() {
        const downloadButtons = document.querySelectorAll('.download-btn, .down-btn, .download-link');
        downloadButtons.forEach(button => {
            if (!button.dataset.enhanced) {
                button.dataset.enhanced = 'true';

                // 添加安全标识
                const safetyIcon = document.createElement('span');
                safetyIcon.textContent = '🛡️';
                safetyIcon.style.cssText = 'margin-right: 5px;';
                button.insertBefore(safetyIcon, button.firstChild);

                // 添加点击事件
                button.addEventListener('click', function(e) {
                    const href = this.href || this.getAttribute('data-href');
                    if (href) {
                        const isMalware = malwareKeywords.some(keyword =>
                            href.toLowerCase().includes(keyword.toLowerCase().replace(/\s+/g, ''))
                        );

                        if (isMalware) {
                            e.preventDefault();
                            showWarningNotification('检测到可疑下载链接，已阻止！');
                            return;
                        }

                        showNotification('安全下载已开始...');
                    }
                });
            }
        });
    }

    // 初始化
    function init() {
        if (!config.enabled) {
            GM_log('下载站优化器已禁用');
            return;
        }

        setTimeout(() => {
            createDownloadControlPanel();
            analyzeAndOptimizePage();

            if (config.blockMalware) blockMalwareAds();
            if (config.safeDownload) enableSafeDownload();
            if (config.cleanInterface) optimizeInterface();

            setupKeyboardShortcuts();

            GM_log('EhViewer 下载站优化器初始化完成');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
