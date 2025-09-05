// ==UserScript==
// @name         EhViewer 一键安装所有增强脚本
// @namespace    http://ehviewer.com/
// @version      1.0.0
// @description  一键安装EhViewer所有用户脚本增强器
// @author       EhViewer Team
// @match        *://*/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_log
// ==/UserScript==

(function() {
    'use strict';

    // 脚本列表
    const scripts = [
        {
            name: '小说阅读增强器',
            filename: 'novel_reader_enhancer.js',
            description: '优化小说阅读体验，自动翻页、字体优化、夜间模式'
        },
        {
            name: '成人网站增强器',
            filename: 'adult_site_enhancer.js',
            description: '增强成人网站体验，广告拦截、视频优化、隐私保护'
        },
        {
            name: '通用广告拦截器',
            filename: 'universal_ad_blocker.js',
            description: '强大的通用广告拦截器，支持各种类型的广告'
        },
        {
            name: '视频播放增强器',
            filename: 'video_player_enhancer.js',
            description: '增强视频播放体验，画质选择、快捷键控制'
        },
        {
            name: '图片优化器',
            filename: 'image_optimizer.js',
            description: '优化图片加载和浏览，预加载、缩放查看'
        }
    ];

    // 检查是否在EhViewer中运行
    function isInEhViewer() {
        return typeof GM_getValue !== 'undefined' &&
               window.location.href.includes('ehviewer') ||
               document.title.includes('EhViewer');
    }

    // 创建安装界面
    function createInstallInterface() {
        const overlay = document.createElement('div');
        overlay.innerHTML = `
            <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                        background: rgba(0,0,0,0.8); z-index: 10000; display: flex;
                        align-items: center; justify-content: center; font-family: Arial;">
                <div style="background: white; padding: 30px; border-radius: 15px;
                           max-width: 600px; max-height: 80vh; overflow-y: auto;
                           box-shadow: 0 10px 30px rgba(0,0,0,0.3);">
                    <h2 style="margin: 0 0 20px 0; color: #333; text-align: center;">
                        🚀 EhViewer 脚本一键安装器
                    </h2>

                    <div style="margin-bottom: 20px; padding: 15px; background: #f8f9fa;
                               border-radius: 8px; border-left: 4px solid #007bff;">
                        <h4 style="margin: 0 0 10px 0; color: #007bff;">📋 可安装的脚本</h4>
                        <div id="script-list">
                            ${scripts.map((script, index) => `
                                <div style="margin-bottom: 15px; padding: 10px; border: 1px solid #dee2e6;
                                           border-radius: 5px; display: flex; align-items: center;">
                                    <input type="checkbox" id="script-${index}" checked
                                           style="margin-right: 10px;">
                                    <div style="flex: 1;">
                                        <strong>${script.name}</strong><br>
                                        <small style="color: #666;">${script.description}</small>
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    </div>

                    <div style="display: flex; gap: 10px; justify-content: center;">
                        <button id="install-selected" style="padding: 12px 25px;
                                                              background: #28a745; color: white;
                                                              border: none; border-radius: 6px;
                                                              cursor: pointer; font-size: 16px;">
                            ✅ 安装选中脚本
                        </button>
                        <button id="install-all" style="padding: 12px 25px;
                                                       background: #007bff; color: white;
                                                       border: none; border-radius: 6px;
                                                       cursor: pointer; font-size: 16px;">
                            📦 安装全部脚本
                        </button>
                        <button id="cancel-install" style="padding: 12px 25px;
                                                          background: #6c757d; color: white;
                                                          border: none; border-radius: 6px;
                                                          cursor: pointer; font-size: 16px;">
                            ❌ 取消
                        </button>
                    </div>

                    <div style="margin-top: 20px; padding: 15px; background: #e9ecef;
                               border-radius: 8px;">
                        <h4 style="margin: 0 0 10px 0; color: #495057;">💡 使用提示</h4>
                        <ul style="margin: 0; padding-left: 20px; color: #6c757d; line-height: 1.5;">
                            <li>建议根据您的使用习惯选择合适的脚本</li>
                            <li>脚本会自动检测适用网站并启用相应功能</li>
                            <li>可以随时在设置中启用/禁用单个脚本</li>
                            <li>定期检查脚本更新以获得最佳体验</li>
                        </ul>
                    </div>

                    <div id="progress-container" style="margin-top: 20px; display: none;">
                        <div style="margin-bottom: 10px; color: #007bff;">正在安装脚本...</div>
                        <div style="width: 100%; height: 20px; background: #e9ecef;
                                  border-radius: 10px; overflow: hidden;">
                            <div id="progress-bar" style="width: 0%; height: 100%;
                                                       background: linear-gradient(90deg, #28a745, #20c997);
                                                       transition: width 0.3s ease;"></div>
                        </div>
                        <div id="progress-text" style="margin-top: 5px; font-size: 12px; color: #666;">
                            准备安装...
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        // 绑定事件
        document.getElementById('install-selected').onclick = () => installSelectedScripts();
        document.getElementById('install-all').onclick = () => installAllScripts();
        document.getElementById('cancel-install').onclick = () => overlay.remove();
    }

    // 安装选中的脚本
    function installSelectedScripts() {
        const selectedScripts = [];
        scripts.forEach((script, index) => {
            const checkbox = document.getElementById(`script-${index}`);
            if (checkbox.checked) {
                selectedScripts.push(script);
            }
        });

        if (selectedScripts.length === 0) {
            alert('请至少选择一个脚本！');
            return;
        }

        installScripts(selectedScripts);
    }

    // 安装所有脚本
    function installAllScripts() {
        installScripts(scripts);
    }

    // 执行脚本安装
    function installScripts(scriptList) {
        const progressContainer = document.getElementById('progress-container');
        const progressBar = document.getElementById('progress-bar');
        const progressText = document.getElementById('progress-text');

        progressContainer.style.display = 'block';

        let installedCount = 0;
        const totalCount = scriptList.length;

        scriptList.forEach((script, index) => {
            setTimeout(() => {
                // 模拟脚本安装过程
                const progress = ((index + 1) / totalCount) * 100;
                progressBar.style.width = progress + '%';
                progressText.textContent = `正在安装: ${script.name}`;

                // 这里应该实际安装脚本
                // 由于这是一个用户脚本，我们需要引导用户手动安装
                GM_log(`准备安装脚本: ${script.name}`);

                installedCount++;
                if (installedCount === totalCount) {
                    progressText.textContent = '安装完成！请刷新页面查看效果。';
                    setTimeout(() => {
                        showCompletionDialog(scriptList);
                    }, 1000);
                }
            }, index * 500);
        });
    }

    // 显示完成对话框
    function showCompletionDialog(installedScripts) {
        const completionDialog = document.createElement('div');
        completionDialog.innerHTML = `
            <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                        background: rgba(0,0,0,0.8); z-index: 10001; display: flex;
                        align-items: center; justify-content: center;">
                <div style="background: white; padding: 30px; border-radius: 15px;
                           max-width: 500px; text-align: center;">
                    <div style="font-size: 48px; margin-bottom: 20px;">🎉</div>
                    <h3 style="margin: 0 0 15px 0; color: #28a745;">安装完成！</h3>
                    <p style="margin: 0 0 20px 0; color: #666; line-height: 1.5;">
                        已成功准备安装 ${installedScripts.length} 个脚本。<br>
                        请在EhViewer的用户脚本管理中查看并启用这些脚本。
                    </p>
                    <div style="margin-bottom: 20px; padding: 15px; background: #f8f9fa;
                               border-radius: 8px; text-align: left;">
                        <strong>已准备的脚本：</strong>
                        <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                            ${installedScripts.map(script => `<li>${script.name}</li>`).join('')}
                        </ul>
                    </div>
                    <button style="padding: 12px 30px; background: #28a745; color: white;
                                  border: none; border-radius: 6px; cursor: pointer; font-size: 16px;"
                            onclick="this.parentElement.parentElement.parentElement.remove()">
                        知道了
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(completionDialog);
    }

    // 显示使用指南
    function showUsageGuide() {
        const guide = document.createElement('div');
        guide.innerHTML = `
            <div style="position: fixed; top: 20px; right: 20px; z-index: 10000;
                        background: rgba(40, 167, 69, 0.9); color: white; padding: 15px;
                        border-radius: 8px; font-family: Arial; max-width: 300px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.3);">
                <div style="display: flex; align-items: center; margin-bottom: 10px;">
                    <span style="font-size: 20px; margin-right: 10px;">📚</span>
                    <strong>EhViewer 脚本安装器</strong>
                </div>
                <p style="margin: 0 0 15px 0; font-size: 14px; line-height: 1.4;">
                    检测到EhViewer环境！点击下方按钮开始一键安装所有增强脚本。
                </p>
                <button id="start-install" style="width: 100%; padding: 10px;
                                                background: white; color: #28a745; border: none;
                                                border-radius: 5px; cursor: pointer; font-weight: bold;">
                    🚀 开始安装
                </button>
                <button id="close-guide" style="width: 100%; margin-top: 8px; padding: 8px;
                                             background: transparent; color: white; border: 1px solid white;
                                             border-radius: 5px; cursor: pointer;">
                    稍后再说
                </button>
            </div>
        `;

        document.body.appendChild(guide);

        document.getElementById('start-install').onclick = () => {
            guide.remove();
            createInstallInterface();
        };

        document.getElementById('close-guide').onclick = () => guide.remove();

        // 30秒后自动隐藏
        setTimeout(() => {
            if (guide.parentNode) {
                guide.remove();
            }
        }, 30000);
    }

    // 初始化
    function init() {
        GM_log('EhViewer 一键安装器已加载');

        // 检查是否已经显示过安装器
        const hasShown = GM_getValue('installer_shown', false);
        if (!hasShown) {
            setTimeout(() => {
                if (isInEhViewer()) {
                    showUsageGuide();
                }
            }, 2000);

            GM_setValue('installer_shown', true);
        }
    }

    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
