// ==UserScript==
// @name         EhViewer 图片优化器
// @namespace    http://ehviewer.com/
// @version      2.0.0
// @description  优化图片加载和浏览体验：预加载、懒加载优化、图片查看器增强
// @author       EhViewer Team
// @match        *://*/*
// @include      *://*/gallery/*
// @include      *://*/album/*
// @include      *://*/photo/*
// @include      *://*/image/*
// @include      *://*/pic/*
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

    GM_log('EhViewer 图片优化器已启动');

    // 配置选项
    const config = {
        enablePreload: GM_getValue('enablePreload', true),
        enableLazyLoad: GM_getValue('enableLazyLoad', true),
        enableZoom: GM_getValue('enableZoom', true),
        enableGallery: GM_getValue('enableGallery', false),
        autoCompress: GM_getValue('autoCompress', false),
        keyboardShortcuts: GM_getValue('keyboardShortcuts', true),
        preloadCount: GM_getValue('preloadCount', 3)
    };

    // 创建控制面板
    function createImagePanel() {
        const panel = document.createElement('div');
        panel.id = 'ehviewer-image-panel';
        panel.innerHTML = `
            <div style="position: fixed; top: 10px; right: 10px; z-index: 10000;
                        background: rgba(0,0,0,0.8); color: white; padding: 8px;
                        border-radius: 5px; font-size: 11px; font-family: Arial;">
                <div style="display: flex; gap: 5px; margin-bottom: 5px;">
                    <button id="toggle-preload" title="预加载">📥</button>
                    <button id="toggle-gallery" title="画廊模式">🖼️</button>
                    <button id="toggle-zoom" title="缩放模式">🔍</button>
                    <button id="download-all" title="下载全部">💾</button>
                </div>
                <div style="font-size: 9px; color: #ccc;">
                    ←→切换 | ESC关闭 | S保存
                </div>
            </div>
        `;
        document.body.appendChild(panel);

        // 绑定事件
        document.getElementById('toggle-preload').onclick = togglePreload;
        document.getElementById('toggle-gallery').onclick = toggleGalleryMode;
        document.getElementById('toggle-zoom').onclick = toggleZoomMode;
        document.getElementById('download-all').onclick = downloadAllImages;
    }

    // 图片预加载
    function setupPreloading() {
        if (!config.enablePreload) return;

        const images = document.querySelectorAll('img[data-src], img[data-original], img[data-lazy]');
        const preloadQueue = [];

        images.forEach((img, index) => {
            const lazySrc = img.dataset.src || img.dataset.original || img.dataset.lazy;
            if (lazySrc && !img.src) {
                preloadQueue.push({ img, src: lazySrc, index });
            }
        });

        // 预加载前N张图片
        const preloadPromises = preloadQueue.slice(0, config.preloadCount).map(item => {
            return new Promise((resolve, reject) => {
                const img = new Image();
                img.onload = () => {
                    item.img.src = item.src;
                    item.img.classList.add('ehviewer-preloaded');
                    GM_log(`预加载完成: ${item.src}`);
                    resolve();
                };
                img.onerror = () => {
                    GM_log(`预加载失败: ${item.src}`);
                    reject();
                };
                img.src = item.src;
            });
        });

        Promise.allSettled(preloadPromises).then(() => {
            GM_log(`预加载完成，共处理 ${preloadPromises.length} 张图片`);
        });
    }

    // 智能懒加载优化
    function optimizeLazyLoading() {
        if (!config.enableLazyLoad) return;

        GM_addStyle(`
            .ehviewer-lazy {
                opacity: 0;
                transition: opacity 0.3s ease;
            }

            .ehviewer-lazy.loaded {
                opacity: 1;
            }

            .ehviewer-loading {
                background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
                background-size: 200% 100%;
                animation: ehviewer-loading 1.5s infinite;
            }

            @keyframes ehviewer-loading {
                0% { background-position: 200% 0; }
                100% { background-position: -200% 0; }
            }
        `);

        const imageObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    const lazySrc = img.dataset.src || img.dataset.original || img.dataset.lazy;

                    if (lazySrc && !img.src) {
                        img.classList.add('ehviewer-lazy', 'ehviewer-loading');

                        const tempImg = new Image();
                        tempImg.onload = () => {
                            img.src = lazySrc;
                            img.classList.remove('ehviewer-loading');
                            img.classList.add('loaded');
                            GM_log(`懒加载完成: ${lazySrc}`);
                        };
                        tempImg.src = lazySrc;

                        imageObserver.unobserve(img);
                    }
                }
            });
        }, {
            rootMargin: '50px 0px',
            threshold: 0.1
        });

        // 观察所有需要懒加载的图片
        const lazyImages = document.querySelectorAll('img[data-src], img[data-original], img[data-lazy]');
        lazyImages.forEach(img => {
            imageObserver.observe(img);
        });
    }

    // 图片查看器增强
    function enhanceImageViewer() {
        // 为大图添加点击放大功能
        const images = document.querySelectorAll('img');
        images.forEach((img, index) => {
            if (img.offsetWidth > 200 && img.offsetHeight > 200) {
                img.style.cursor = 'zoom-in';
                img.addEventListener('click', () => showImageModal(img, index));
            }
        });
    }

    function showImageModal(img, index) {
        const modal = document.createElement('div');
        modal.id = 'ehviewer-image-modal';
        modal.innerHTML = `
            <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                        background: rgba(0,0,0,0.9); z-index: 10001; display: flex;
                        align-items: center; justify-content: center; cursor: zoom-out;">
                <img src="${img.src}" style="max-width: 90%; max-height: 90%;
                                           object-fit: contain;" id="modal-image">
                <div style="position: absolute; top: 20px; right: 20px; color: white;">
                    <button id="close-modal" style="background: rgba(0,0,0,0.5);
                                                   border: none; color: white; padding: 10px;
                                                   border-radius: 5px; margin-left: 10px;">✕</button>
                </div>
                <div style="position: absolute; bottom: 20px; left: 50%;
                           transform: translateX(-50%); color: white; font-size: 14px;">
                    <button id="prev-image" style="margin-right: 20px;">◀</button>
                    <span id="image-counter">${index + 1} / ${document.querySelectorAll('img').length}</span>
                    <button id="next-image" style="margin-left: 20px;">▶</button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        const modalImg = document.getElementById('modal-image');
        let currentIndex = index;

        // 绑定事件
        document.getElementById('close-modal').onclick = () => modal.remove();
        document.getElementById('prev-image').onclick = () => showPrevImage();
        document.getElementById('next-image').onclick = () => showNextImage();

        modal.onclick = (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        };

        function showPrevImage() {
            const allImages = document.querySelectorAll('img');
            currentIndex = (currentIndex - 1 + allImages.length) % allImages.length;
            modalImg.src = allImages[currentIndex].src;
            updateCounter();
        }

        function showNextImage() {
            const allImages = document.querySelectorAll('img');
            currentIndex = (currentIndex + 1) % allImages.length;
            modalImg.src = allImages[currentIndex].src;
            updateCounter();
        }

        function updateCounter() {
            const allImages = document.querySelectorAll('img');
            document.getElementById('image-counter').textContent =
                `${currentIndex + 1} / ${allImages.length}`;
        }

        // 键盘控制
        document.addEventListener('keydown', function modalKeyHandler(e) {
            switch (e.keyCode) {
                case 27: // ESC
                    modal.remove();
                    document.removeEventListener('keydown', modalKeyHandler);
                    break;
                case 37: // ←
                    showPrevImage();
                    break;
                case 39: // →
                    showNextImage();
                    break;
            }
        });
    }

    // 画廊模式
    function toggleGalleryMode() {
        config.enableGallery = !config.enableGallery;
        GM_setValue('enableGallery', config.enableGallery);

        if (config.enableGallery) {
            createGalleryView();
        } else {
            exitGalleryView();
        }
    }

    function createGalleryView() {
        const images = document.querySelectorAll('img');
        const gallery = document.createElement('div');
        gallery.id = 'ehviewer-gallery';
        gallery.innerHTML = `
            <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                        background: #111; z-index: 10000; overflow-y: auto; padding: 20px;">
                <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
                           gap: 15px; max-width: 1200px; margin: 0 auto;">
                    ${Array.from(images).map((img, index) => `
                        <div style="background: #222; border-radius: 8px; overflow: hidden;
                                  cursor: pointer; transition: transform 0.2s;"
                             onclick="showImageModal(${index})">
                            <img src="${img.src}" style="width: 100%; height: 150px; object-fit: cover;">
                        </div>
                    `).join('')}
                </div>
                <button style="position: fixed; top: 20px; right: 20px; background: #333;
                              color: white; border: none; padding: 10px 15px; border-radius: 5px;
                              cursor: pointer;" onclick="exitGalleryView()">
                    退出画廊
                </button>
            </div>
        `;

        document.body.appendChild(gallery);
        window.showImageModal = (index) => showImageModal(images[index], index);
        window.exitGalleryView = exitGalleryView;
    }

    function exitGalleryView() {
        const gallery = document.getElementById('ehviewer-gallery');
        if (gallery) {
            gallery.remove();
        }
    }

    // 缩放模式
    function toggleZoomMode() {
        config.enableZoom = !config.enableZoom;
        GM_setValue('enableZoom', config.enableZoom);

        if (config.enableZoom) {
            enableZoomMode();
        } else {
            disableZoomMode();
        }
    }

    function enableZoomMode() {
        GM_addStyle(`
            .ehviewer-zoom-enabled {
                cursor: zoom-in !important;
            }

            .ehviewer-zoom-enabled:hover {
                transform: scale(1.05);
                transition: transform 0.2s;
            }
        `);

        const images = document.querySelectorAll('img');
        images.forEach(img => {
            img.classList.add('ehviewer-zoom-enabled');
            img.addEventListener('click', handleZoomClick);
        });
    }

    function disableZoomMode() {
        const images = document.querySelectorAll('img');
        images.forEach(img => {
            img.classList.remove('ehviewer-zoom-enabled');
            img.removeEventListener('click', handleZoomClick);
        });
    }

    function handleZoomClick(e) {
        const img = e.target;
        const rect = img.getBoundingClientRect();
        const scale = Math.min(window.innerWidth / rect.width, window.innerHeight / rect.height) * 0.9;

        img.style.transform = `scale(${scale})`;
        img.style.transformOrigin = 'center center';
        img.style.position = 'fixed';
        img.style.top = '50%';
        img.style.left = '50%';
        img.style.zIndex = '10001';
        img.style.cursor = 'zoom-out';

        const overlay = document.createElement('div');
        overlay.style.position = 'fixed';
        overlay.style.top = '0';
        overlay.style.left = '0';
        overlay.style.right = '0';
        overlay.style.bottom = '0';
        overlay.style.background = 'rgba(0,0,0,0.8)';
        overlay.style.zIndex = '10000';
        overlay.onclick = () => {
            img.style.transform = '';
            img.style.position = '';
            img.style.top = '';
            img.style.left = '';
            img.style.zIndex = '';
            img.style.cursor = '';
            overlay.remove();
        };

        document.body.appendChild(overlay);
    }

    // 下载功能
    function downloadAllImages() {
        const images = document.querySelectorAll('img');
        const downloadList = [];

        images.forEach((img, index) => {
            if (img.src && img.src.startsWith('http') && img.offsetWidth > 100) {
                downloadList.push({
                    url: img.src,
                    filename: `image_${index + 1}.${getImageExtension(img.src)}`
                });
            }
        });

        if (downloadList.length === 0) {
            alert('没有找到可下载的图片');
            return;
        }

        if (confirm(`找到 ${downloadList.length} 张图片，确定要下载全部吗？`)) {
            downloadList.forEach((item, index) => {
                setTimeout(() => {
                    downloadImage(item.url, item.filename);
                }, index * 1000); // 每秒下载一张，避免并发过多
            });
        }
    }

    function downloadImage(url, filename) {
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        GM_log(`开始下载: ${filename}`);
    }

    function getImageExtension(url) {
        const match = url.match(/\.([a-zA-Z]+)(?:\?|$)/);
        return match ? match[1] : 'jpg';
    }

    // 图片压缩优化
    function setupImageCompression() {
        if (!config.autoCompress) return;

        const images = document.querySelectorAll('img');
        images.forEach(img => {
            if (img.complete && img.naturalWidth > 1920) {
                // 对于超大图片，尝试加载压缩版本
                const compressedSrc = getCompressedUrl(img.src);
                if (compressedSrc !== img.src) {
                    img.dataset.original = img.src;
                    img.src = compressedSrc;
                    GM_log('已切换到压缩版本: ' + compressedSrc);
                }
            }
        });
    }

    function getCompressedUrl(originalUrl) {
        // 这里可以根据不同网站实现压缩URL转换逻辑
        // 例如为某些CDN添加压缩参数
        if (originalUrl.includes('imgur.com')) {
            return originalUrl.replace(/\.(\w+)$/, 'l.$1'); // Imgur压缩
        }
        return originalUrl;
    }

    // 键盘快捷键
    function setupKeyboardShortcuts() {
        if (!config.keyboardShortcuts) return;

        document.addEventListener('keydown', function(e) {
            switch (e.keyCode) {
                case 71: // G - 画廊模式
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        toggleGalleryMode();
                    }
                    break;
                case 83: // S - 保存当前图片
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        saveCurrentImage();
                    }
                    break;
                case 90: // Z - 缩放模式
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        toggleZoomMode();
                    }
                    break;
                case 80: // P - 预加载切换
                    if (e.ctrlKey || e.metaKey) {
                        e.preventDefault();
                        togglePreload();
                    }
                    break;
            }
        });
    }

    function saveCurrentImage() {
        const focusedImg = document.activeElement;
        if (focusedImg && focusedImg.tagName === 'IMG') {
            downloadImage(focusedImg.src, `saved_image.${getImageExtension(focusedImg.src)}`);
        } else {
            // 保存页面中最大的图片
            const images = document.querySelectorAll('img');
            let largestImg = null;
            let maxArea = 0;

            images.forEach(img => {
                const area = img.offsetWidth * img.offsetHeight;
                if (area > maxArea) {
                    maxArea = area;
                    largestImg = img;
                }
            });

            if (largestImg) {
                downloadImage(largestImg.src, `saved_image.${getImageExtension(largestImg.src)}`);
            }
        }
    }

    function togglePreload() {
        config.enablePreload = !config.enablePreload;
        GM_setValue('enablePreload', config.enablePreload);

        if (config.enablePreload) {
            setupPreloading();
        }

        const button = document.getElementById('toggle-preload');
        if (button) {
            button.textContent = config.enablePreload ? '📥' : '📴';
        }
    }

    // 性能监控
    function setupPerformanceMonitoring() {
        const perfData = {
            imagesLoaded: 0,
            loadTime: 0,
            bandwidthSaved: 0
        };

        const originalImage = new Image();
        const observer = new PerformanceObserver((list) => {
            list.getEntries().forEach((entry) => {
                if (entry.name.includes('.jpg') || entry.name.includes('.png') ||
                    entry.name.includes('.gif') || entry.name.includes('.webp')) {
                    perfData.imagesLoaded++;
                    perfData.loadTime += entry.duration;
                }
            });
        });

        try {
            observer.observe({ entryTypes: ['resource'] });
        } catch (e) {
            GM_log('性能监控初始化失败: ' + e.message);
        }

        // 定期报告性能数据
        setInterval(() => {
            GM_log(`图片加载统计: ${perfData.imagesLoaded}张, 总耗时: ${perfData.loadTime.toFixed(2)}ms`);
        }, 30000);
    }

    // 初始化
    function init() {
        setTimeout(() => {
            createImagePanel();
            setupPreloading();
            optimizeLazyLoading();
            enhanceImageViewer();
            setupImageCompression();
            setupKeyboardShortcuts();
            setupPerformanceMonitoring();

            GM_log('EhViewer 图片优化器初始化完成');
        }, 1000);
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
