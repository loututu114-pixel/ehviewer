// ==UserScript==
// @name         EhViewer 移动端适配增强器
// @namespace    http://ehviewer.com/
// @version      2.0.0
// @description  智能适配PC网页到移动端，解决不支持移动适配的网站访问问题
// @author       EhViewer Team
// @match        *://*/*
// @exclude      *://*.google.com/*
// @exclude      *://*.youtube.com/*
// @exclude      *://*.facebook.com/*
// @exclude      *://*.twitter.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_log
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 移动端适配增强器已启动');

    // 设备检测和配置
    const deviceConfig = {
        isMobile: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
        isTablet: /iPad|Android(?=.*\bMobile\b)|Tablet|PlayBook/i.test(navigator.userAgent),
        isIOS: /iPad|iPhone|iPod/.test(navigator.userAgent),
        isAndroid: /Android/.test(navigator.userAgent),
        screenWidth: window.screen.width,
        screenHeight: window.screen.height,
        viewportWidth: Math.max(document.documentElement.clientWidth || 0, window.innerWidth || 0)
    };

    // 配置选项
    const config = {
        enabled: GM_getValue('mobileAdaptEnabled', true),
        forceMobileUA: GM_getValue('forceMobileUA', false),
        autoRedirect: GM_getValue('autoRedirect', true),
        viewportOverride: GM_getValue('viewportOverride', true),
        cssAdaptation: GM_getValue('cssAdaptation', true),
        imageOptimization: GM_getValue('imageOptimization', true),
        fontOptimization: GM_getValue('fontOptimization', true),
        touchEnhance: GM_getValue('touchEnhance', true),
        zoomControl: GM_getValue('zoomControl', true),
        debugMode: GM_getValue('debugMode', false)
    };

    // 网站适配规则库
    const siteAdaptationRules = {
        // 新闻资讯类
        'sina.com.cn': {
            mobileUA: true,
            viewport: 'width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no',
            cssOverrides: {
                '.main-content': 'width: 100% !important; padding: 0 10px !important;',
                '.sidebar': 'display: none !important;',
                '.header': 'position: fixed !important; top: 0 !important; width: 100% !important; z-index: 1000 !important;'
            }
        },
        'sohu.com': {
            mobileUA: true,
            redirectPattern: /\/www\//,
            mobileDomain: 'm.sohu.com'
        },
        '163.com': {
            mobileUA: true,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '.post_content': 'font-size: 16px !important; line-height: 1.6 !important;',
                '.side-bar': 'display: none !important;'
            }
        },
        'qq.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0,maximum-scale=3.0',
            cssOverrides: {
                '.content': 'max-width: none !important;',
                '.nav': 'overflow-x: auto !important; white-space: nowrap !important;'
            }
        },

        // 电商平台类
        'taobao.com': {
            mobileUA: true,
            redirectPattern: /www\.taobao\.com/,
            mobileDomain: 'm.taobao.com',
            cssOverrides: {
                '.item-list': 'display: flex !important; flex-wrap: wrap !important;',
                '.item': 'width: 50% !important; box-sizing: border-box !important;'
            }
        },
        'jd.com': {
            mobileUA: true,
            redirectPattern: /www\.jd\.com/,
            mobileDomain: 'm.jd.com'
        },
        'tmall.com': {
            mobileUA: true,
            redirectPattern: /www\.tmall\.com/,
            mobileDomain: 'm.tmall.com'
        },

        // 内容平台类
        'zhihu.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '.QuestionHeader-title': 'font-size: 18px !important; line-height: 1.4 !important;',
                '.AnswerItem': 'margin-bottom: 15px !important;',
                '.RichContent-inner': 'font-size: 15px !important; line-height: 1.6 !important;'
            }
        },
        'xiaohongshu.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0,maximum-scale=2.0',
            cssOverrides: {
                '.note-content': 'font-size: 16px !important;',
                '.image-container': 'max-width: 100% !important; height: auto !important;'
            }
        },
        'douban.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '.subject': 'padding: 10px !important;',
                '.review': 'font-size: 14px !important; line-height: 1.5 !important;'
            }
        },

        // 搜索引擎类
        'baidu.com': {
            mobileUA: true,
            redirectPattern: /www\.baidu\.com/,
            mobileDomain: 'm.baidu.com',
            cssOverrides: {
                '#results': 'padding: 0 10px !important;',
                '.result': 'margin-bottom: 15px !important;'
            }
        },
        'sogou.com': {
            mobileUA: true,
            mobileDomain: 'm.sogou.com'
        },

        // 视频平台类
        'bilibili.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '.video-info': 'padding: 10px !important;',
                '.comment-list': 'font-size: 14px !important;'
            }
        },
        'youku.com': {
            mobileUA: true,
            mobileDomain: 'm.youku.com'
        },
        'iqiyi.com': {
            mobileUA: true,
            mobileDomain: 'm.iqiyi.com'
        },

        // 社交平台类
        'weibo.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '.weibo-text': 'font-size: 15px !important; line-height: 1.6 !important;',
                '.card': 'margin-bottom: 10px !important;'
            }
        },

        // 论坛社区类
        'tieba.baidu.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '.post-content': 'font-size: 14px !important; line-height: 1.5 !important;',
                '.floor': 'padding: 8px !important;'
            }
        },
        'hupu.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '.post-content': 'font-size: 14px !important;',
                '.reply-list': 'padding: 0 10px !important;'
            }
        },

        // 其他常用网站
        'github.com': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '.repository-content': 'padding: 10px !important;',
                '.readme': 'font-size: 14px !important;'
            }
        },
        'wikipedia.org': {
            mobileUA: false,
            viewport: 'width=device-width,initial-scale=1.0',
            cssOverrides: {
                '#content': 'padding: 10px !important;',
                '.mw-parser-output': 'font-size: 14px !important; line-height: 1.6 !important;'
            }
        }
    };

    // 通用适配规则
    const generalRules = {
        viewport: 'width=device-width,initial-scale=1.0,maximum-scale=3.0,user-scalable=yes',
        cssOverrides: {
            // 通用样式优化
            '*': 'box-sizing: border-box !important;',
            'img': 'max-width: 100% !important; height: auto !important;',
            'video': 'max-width: 100% !important; height: auto !important;',
            'iframe': 'max-width: 100% !important; height: auto !important;',

            // 文本优化
            'body': 'font-size: 14px !important; line-height: 1.5 !important;',
            'p': 'margin-bottom: 10px !important;',
            'h1': 'font-size: 22px !important; margin-bottom: 15px !important;',
            'h2': 'font-size: 20px !important; margin-bottom: 12px !important;',
            'h3': 'font-size: 18px !important; margin-bottom: 10px !important;',

            // 布局优化
            '.container, .wrapper, .main': 'max-width: none !important; padding: 0 10px !important;',
            '.sidebar, .aside': 'display: none !important;',
            '.footer': 'margin-top: 20px !important; padding: 10px !important;',

            // 导航优化
            'nav': 'overflow-x: auto !important; white-space: nowrap !important;',
            'nav a': 'display: inline-block !important; padding: 8px 12px !important;',

            // 表格优化
            'table': 'width: 100% !important; font-size: 12px !important;',
            'th, td': 'padding: 5px !important; border: 1px solid #ddd !important;',

            // 表单优化
            'input, textarea, select': 'width: 100% !important; padding: 8px !important; font-size: 14px !important;',
            'button': 'padding: 8px 16px !important; font-size: 14px !important;'
        }
    };

    // 创建移动端适配控制面板
    function createAdaptationPanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-mobile-adaptation-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; left: 10px; z-index: 10000;
                        background: linear-gradient(135deg, #667eea, #764ba2); color: white;
                        padding: 10px; border-radius: 8px; font-size: 11px; font-family: Arial;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3); min-width: 180px;">
                <div style="display: flex; align-items: center; margin-bottom: 6px;">
                    <span style="font-size: 14px; margin-right: 6px;">📱</span>
                    <strong>移动适配器</strong>
                </div>
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 3px; margin-bottom: 6px;">
                    <button id="force-mobile" title="强制移动UA">📱UA</button>
                    <button id="adapt-layout" title="适配布局">⚡布局</button>
                    <button id="optimize-images" title="优化图片">🖼️图片</button>
                    <button id="touch-mode" title="触摸模式">👆触摸</button>
                    <button id="zoom-control" title="缩放控制">🔍缩放</button>
                    <button id="settings-btn" title="设置">⚙️</button>
                </div>
                <div style="font-size: 8px; color: rgba(255,255,255,0.8); text-align: center;
                           border-top: 1px solid rgba(255,255,255,0.2); padding-top: 3px;">
                    F1切换 | F2重载 | F3调试
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('force-mobile').onclick = () => toggleMobileUA();
        document.getElementById('adapt-layout').onclick = () => adaptLayout();
        document.getElementById('optimize-images').onclick = () => optimizeImages();
        document.getElementById('touch-mode').onclick = () => toggleTouchMode();
        document.getElementById('zoom-control').onclick = () => toggleZoomControl();
        document.getElementById('settings-btn').onclick = showAdaptationSettings();

        // 快捷键绑定
        setupKeyboardShortcuts();
    }

    // 检测当前网站适配规则
    function detectSiteRules() {
        const hostname = window.location.hostname.toLowerCase();

        // 精确匹配
        if (siteAdaptationRules[hostname]) {
            return siteAdaptationRules[hostname];
        }

        // 域名匹配
        for (const [domain, rules] of Object.entries(siteAdaptationRules)) {
            if (hostname.includes(domain.replace('www.', '').replace('.com', ''))) {
                return rules;
            }
        }

        // 返回通用规则
        return generalRules;
    }

    // 强制使用移动端User Agent
    function toggleMobileUA() {
        config.forceMobileUA = !config.forceMobileUA;
        GM_setValue('forceMobileUA', config.forceMobileUA);

        if (config.forceMobileUA) {
            // 修改User Agent为移动端
            const mobileUA = deviceConfig.isIOS
                ? 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1'
                : 'Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36';

            // 尝试修改navigator.userAgent（可能不生效，取决于浏览器）
            try {
                Object.defineProperty(navigator, 'userAgent', {
                    value: mobileUA,
                    writable: false
                });
            } catch (e) {
                GM_log('无法修改User Agent:', e);
            }

            showNotification('已切换到移动端User Agent', 'success');
        } else {
            showNotification('已恢复原始User Agent', 'info');
        }
    }

    // 适配页面布局
    function adaptLayout() {
        const rules = detectSiteRules();

        // 设置viewport
        if (config.viewportOverride) {
            const viewport = document.querySelector('meta[name="viewport"]');
            if (viewport) {
                viewport.setAttribute('content', rules.viewport || generalRules.viewport);
            } else {
                const meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = rules.viewport || generalRules.viewport;
                document.head.appendChild(meta);
            }
        }

        // 应用CSS覆盖
        const cssOverrides = { ...generalRules.cssOverrides, ...(rules.cssOverrides || {}) };
        let cssText = '';

        for (const [selector, styles] of Object.entries(cssOverrides)) {
            cssText += `${selector} { ${styles} }\n`;
        }

        GM_addStyle(cssText);

        // 处理重定向
        if (config.autoRedirect && rules.redirectPattern && rules.mobileDomain) {
            const currentUrl = window.location.href;
            if (rules.redirectPattern.test(currentUrl)) {
                const mobileUrl = currentUrl.replace(/https?:\/\/[^\/]+/, `https://${rules.mobileDomain}`);
                if (confirm(`检测到此网站有移动端版本，是否跳转到：\n${mobileUrl}`)) {
                    window.location.href = mobileUrl;
                    return;
                }
            }
        }

        showNotification('页面布局已适配！', 'success');
    }

    // 优化图片显示
    function optimizeImages() {
        if (!config.imageOptimization) return;

        const images = document.querySelectorAll('img');

        images.forEach(img => {
            // 确保图片自适应
            img.style.maxWidth = '100%';
            img.style.height = 'auto';

            // 添加懒加载
            if (!img.hasAttribute('loading')) {
                img.setAttribute('loading', 'lazy');
            }

            // 优化大图
            if (img.naturalWidth > deviceConfig.viewportWidth * 2) {
                img.style.width = '100%';
                img.style.height = 'auto';
            }

            // 添加点击放大功能
            img.addEventListener('click', function() {
                if (!this.classList.contains('zoomed')) {
                    this.classList.add('zoomed');
                    this.style.position = 'fixed';
                    this.style.top = '50%';
                    this.style.left = '50%';
                    this.style.transform = 'translate(-50%, -50%)';
                    this.style.zIndex = '9999';
                    this.style.maxWidth = '90vw';
                    this.style.maxHeight = '90vh';
                    this.style.cursor = 'zoom-out';
                } else {
                    this.classList.remove('zoomed');
                    this.style.position = '';
                    this.style.top = '';
                    this.style.left = '';
                    this.style.transform = '';
                    this.style.zIndex = '';
                    this.style.maxWidth = '';
                    this.style.maxHeight = '';
                    this.style.cursor = '';
                }
            });
        });

        showNotification('图片已优化！', 'success');
    }

    // 触摸模式增强
    function toggleTouchMode() {
        config.touchEnhance = !config.touchEnhance;
        GM_setValue('touchEnhance', config.touchEnhance);

        if (config.touchEnhance) {
            // 增强触摸交互
            GM_addStyle(`
                /* 触摸优化 */
                * {
                    -webkit-tap-highlight-color: rgba(0,0,0,0.1) !important;
                    touch-action: manipulation !important;
                }

                button, a, [role="button"] {
                    min-height: 44px !important;
                    min-width: 44px !important;
                }

                /* 滑动优化 */
                .scrollable {
                    -webkit-overflow-scrolling: touch !important;
                    overflow-x: auto !important;
                }

                /* 长按菜单 */
                .long-press-menu {
                    position: fixed;
                    background: white;
                    border-radius: 8px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                    z-index: 10000;
                    display: none;
                }
            `);

            // 添加长按功能
            setupLongPress();
        }

        showNotification(`触摸模式${config.touchEnhance ? '已启用' : '已禁用'}`, 'info');
    }

    // 长按功能
    function setupLongPress() {
        let longPressTimer;
        let longPressElement;

        document.addEventListener('touchstart', function(e) {
            longPressTimer = setTimeout(() => {
                longPressElement = e.target;
                showLongPressMenu(e.touches[0].clientX, e.touches[0].clientY, longPressElement);
            }, 500);
        });

        document.addEventListener('touchend', function() {
            clearTimeout(longPressTimer);
        });

        document.addEventListener('touchmove', function() {
            clearTimeout(longPressTimer);
        });
    }

    function showLongPressMenu(x, y, element) {
        const menu = document.createElement('div');
        menu.className = 'long-press-menu';
        menu.style.left = x + 'px';
        menu.style.top = y + 'px';
        menu.innerHTML = `
            <div style="padding: 10px;">
                <button onclick="copyText()">复制文本</button>
                <button onclick="searchText()">搜索文本</button>
                <button onclick="shareLink()">分享链接</button>
            </div>
        `;
        document.body.appendChild(menu);
        menu.style.display = 'block';

        // 点击其他地方关闭菜单
        document.addEventListener('click', function closeMenu() {
            menu.remove();
            document.removeEventListener('click', closeMenu);
        });
    }

    // 缩放控制
    function toggleZoomControl() {
        config.zoomControl = !config.zoomControl;
        GM_setValue('zoomControl', config.zoomControl);

        if (config.zoomControl) {
            // 添加缩放控制
            GM_addStyle(`
                /* 缩放控制 */
                body {
                    zoom: 1 !important;
                    -webkit-text-size-adjust: 100% !important;
                    -moz-text-size-adjust: 100% !important;
                    -ms-text-size-adjust: 100% !important;
                    text-size-adjust: 100% !important;
                }

                /* 字体缩放 */
                .zoom-text-small { font-size: 12px !important; }
                .zoom-text-normal { font-size: 14px !important; }
                .zoom-text-large { font-size: 16px !important; }
                .zoom-text-xlarge { font-size: 18px !important; }
            `);

            // 添加缩放控制按钮
            addZoomControls();
        }

        showNotification(`缩放控制${config.zoomControl ? '已启用' : '已禁用'}`, 'info');
    }

    function addZoomControls() {
        const controls = document.createElement('div');
        controls.id = 'zoom-controls';
        controls.innerHTML = `
            <div style="position: fixed; bottom: 20px; right: 20px; z-index: 10000;
                        background: rgba(0,0,0,0.8); color: white; padding: 10px;
                        border-radius: 8px; font-size: 12px;">
                <div style="margin-bottom: 8px; font-weight: bold;">文字大小</div>
                <div style="display: flex; gap: 5px;">
                    <button onclick="setFontSize('small')" style="padding: 5px; background: #666; border: none; border-radius: 4px; color: white;">小</button>
                    <button onclick="setFontSize('normal')" style="padding: 5px; background: #666; border: none; border-radius: 4px; color: white;">中</button>
                    <button onclick="setFontSize('large')" style="padding: 5px; background: #666; border: none; border-radius: 4px; color: white;">大</button>
                    <button onclick="setFontSize('xlarge')" style="padding: 5px; background: #666; border: none; border-radius: 4px; color: white;">超大</button>
                </div>
            </div>
        `;
        document.body.appendChild(controls);

        // 添加全局函数
        window.setFontSize = (size) => {
            const body = document.body;
            body.classList.remove('zoom-text-small', 'zoom-text-normal', 'zoom-text-large', 'zoom-text-xlarge');
            body.classList.add(`zoom-text-${size}`);
            localStorage.setItem('ehviewer_font_size', size);
        };

        // 恢复上次设置
        const savedSize = localStorage.getItem('ehviewer_font_size') || 'normal';
        window.setFontSize(savedSize);
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        document.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            switch (e.keyCode) {
                case 112: // F1 - 切换移动UA
                    e.preventDefault();
                    toggleMobileUA();
                    break;
                case 113: // F2 - 重新适配
                    e.preventDefault();
                    adaptLayout();
                    break;
                case 114: // F3 - 调试模式
                    e.preventDefault();
                    toggleDebugMode();
                    break;
                case 107: // + 放大
                case 187: // = 放大
                    if (e.ctrlKey) {
                        e.preventDefault();
                        increaseFontSize();
                    }
                    break;
                case 109: // - 缩小
                case 189: // - 缩小
                    if (e.ctrlKey) {
                        e.preventDefault();
                        decreaseFontSize();
                    }
                    break;
            }
        });
    }

    function increaseFontSize() {
        const sizes = ['small', 'normal', 'large', 'xlarge'];
        const currentSize = localStorage.getItem('ehviewer_font_size') || 'normal';
        const currentIndex = sizes.indexOf(currentSize);
        if (currentIndex < sizes.length - 1) {
            window.setFontSize(sizes[currentIndex + 1]);
        }
    }

    function decreaseFontSize() {
        const sizes = ['small', 'normal', 'large', 'xlarge'];
        const currentSize = localStorage.getItem('ehviewer_font_size') || 'normal';
        const currentIndex = sizes.indexOf(currentSize);
        if (currentIndex > 0) {
            window.setFontSize(sizes[currentIndex - 1]);
        }
    }

    function toggleDebugMode() {
        config.debugMode = !config.debugMode;
        GM_setValue('debugMode', config.debugMode);

        if (config.debugMode) {
            GM_addStyle(`
                /* 调试模式 */
                * {
                    outline: 1px solid rgba(255,0,0,0.1) !important;
                }

                [data-debug] {
                    position: relative !important;
                }

                [data-debug]:after {
                    content: attr(data-debug) !important;
                    position: absolute !important;
                    top: 0 !important;
                    left: 0 !important;
                    background: rgba(255,0,0,0.8) !important;
                    color: white !important;
                    font-size: 10px !important;
                    padding: 2px 4px !important;
                    z-index: 10000 !important;
                }
            `);

            // 添加调试信息
            const elements = document.querySelectorAll('*');
            elements.forEach((el, index) => {
                if (index < 100) { // 只为前100个元素添加调试信息
                    el.setAttribute('data-debug', `${el.tagName}${el.className ? '.' + el.className.split(' ')[0] : ''}`);
                }
            });
        }

        showNotification(`调试模式${config.debugMode ? '已启用' : '已禁用'}`, 'info');
    }

    // 设置面板
    function showAdaptationSettings() {
        return function() {
            const settings = document.createElement('div');
            settings.innerHTML = `
                <div style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                            background: white; padding: 25px; border-radius: 12px; z-index: 10001;
                            box-shadow: 0 6px 24px rgba(0,0,0,0.3); max-width: 400px;">
                    <h3 style="margin: 0 0 20px 0; color: #333; text-align: center;">移动端适配设置</h3>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-enabled" style="margin-right: 8px;" ${config.enabled ? 'checked' : ''}>
                            启用适配器
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-mobile-ua" style="margin-right: 8px;" ${config.forceMobileUA ? 'checked' : ''}>
                            强制移动UA
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-auto-redirect" style="margin-right: 8px;" ${config.autoRedirect ? 'checked' : ''}>
                            自动重定向
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-viewport" style="margin-right: 8px;" ${config.viewportOverride ? 'checked' : ''}>
                            Viewport覆盖
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-css-adapt" style="margin-right: 8px;" ${config.cssAdaptation ? 'checked' : ''}>
                            CSS适配
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-image-opt" style="margin-right: 8px;" ${config.imageOptimization ? 'checked' : ''}>
                            图片优化
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-font-opt" style="margin-right: 8px;" ${config.fontOptimization ? 'checked' : ''}>
                            字体优化
                        </label>
                        <label style="display: flex; align-items: center;">
                            <input type="checkbox" id="setting-touch" style="margin-right: 8px;" ${config.touchEnhance ? 'checked' : ''}>
                            触摸增强
                        </label>
                    </div>

                    <div style="border-top: 1px solid #dee2e6; padding-top: 15px; margin-bottom: 20px;">
                        <h4 style="margin: 0 0 10px 0; color: #666;">设备信息</h4>
                        <div style="font-size: 12px; color: #666;">
                            <div>设备类型: ${deviceConfig.isMobile ? '移动设备' : '桌面设备'}</div>
                            <div>屏幕尺寸: ${deviceConfig.screenWidth} x ${deviceConfig.screenHeight}</div>
                            <div>视口宽度: ${deviceConfig.viewportWidth}px</div>
                            <div>操作系统: ${deviceConfig.isIOS ? 'iOS' : deviceConfig.isAndroid ? 'Android' : '其他'}</div>
                        </div>
                    </div>

                    <div style="text-align: right;">
                        <button id="save-adaptation-settings" style="padding: 10px 20px;
                                        background: linear-gradient(135deg, #667eea, #764ba2);
                                        color: white; border: none; border-radius: 6px;
                                        cursor: pointer; margin-right: 10px;">
                            保存设置
                        </button>
                        <button id="close-adaptation-settings" style="padding: 10px 20px;
                                         background: #6c757d; color: white; border: none;
                                         border-radius: 6px; cursor: pointer;">
                            关闭
                        </button>
                    </div>
                </div>
            `;

            document.body.appendChild(settings);

            document.getElementById('save-adaptation-settings').onclick = saveAdaptationSettings;
            document.getElementById('close-adaptation-settings').onclick = () => settings.remove();
        };
    }

    function saveAdaptationSettings() {
        config.enabled = document.getElementById('setting-enabled').checked;
        config.forceMobileUA = document.getElementById('setting-mobile-ua').checked;
        config.autoRedirect = document.getElementById('setting-auto-redirect').checked;
        config.viewportOverride = document.getElementById('setting-viewport').checked;
        config.cssAdaptation = document.getElementById('setting-css-adapt').checked;
        config.imageOptimization = document.getElementById('setting-image-opt').checked;
        config.fontOptimization = document.getElementById('setting-font-opt').checked;
        config.touchEnhance = document.getElementById('setting-touch').checked;

        // 保存设置
        Object.keys(config).forEach(key => {
            GM_setValue('mobileAdapt' + key.charAt(0).toUpperCase() + key.slice(1), config[key]);
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

    // 自动初始化
    function init() {
        if (!config.enabled) {
            GM_log('移动端适配增强器已禁用');
            return;
        }

        setTimeout(() => {
            createAdaptationPanel();

            // 根据配置自动应用功能
            if (config.cssAdaptation) adaptLayout();
            if (config.imageOptimization) optimizeImages();
            if (config.touchEnhance) toggleTouchMode();
            if (config.zoomControl) toggleZoomControl();

            GM_log('EhViewer 移动端适配增强器初始化完成');

            // 显示欢迎信息
            showNotification('移动端适配增强器已启动！按F1切换UA，F2重新适配', 'success');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
