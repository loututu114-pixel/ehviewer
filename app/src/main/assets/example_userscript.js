// ==UserScript==
// @name         EhViewer 广告拦截脚本
// @namespace    http://tampermonkey.net/
// @version      1.0.0
// @description  自动拦截EhViewer中的广告元素
// @author       EhViewer Team
// @match        *://*/*
// @grant        GM_addStyle
// @grant        GM_log
// ==/UserScript==

(function() {
    'use strict';

    GM_log('EhViewer 广告拦截脚本已加载');

    // 拦截常见的广告选择器
    var adSelectors = [
        '.ads',
        '.advertisement',
        '.ad-banner',
        '.ad-container',
        '.popup-ad',
        '.overlay-ad',
        '[class*="ad-"]',
        '[id*="ad-"]',
        '[class*="advert"]',
        '[id*="advert"]'
    ];

    // 添加CSS样式来隐藏广告
    var css = '';
    for (var i = 0; i < adSelectors.length; i++) {
        css += adSelectors[i] + ' { display: none !important; } ';
    }

    GM_addStyle(css);

    // 监听DOM变化，动态隐藏新出现的广告
    var observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'childList') {
                for (var i = 0; i < mutation.addedNodes.length; i++) {
                    var node = mutation.addedNodes[i];
                    if (node.nodeType === 1) { // Element节点
                        hideAdsInElement(node);
                    }
                }
            }
        });
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

    function hideAdsInElement(element) {
        for (var i = 0; i < adSelectors.length; i++) {
            var ads = element.querySelectorAll(adSelectors[i]);
            for (var j = 0; j < ads.length; j++) {
                ads[j].style.display = 'none';
                GM_log('隐藏广告元素: ' + adSelectors[i]);
            }
        }
    }

    GM_log('EhViewer 广告拦截脚本初始化完成');
})();
