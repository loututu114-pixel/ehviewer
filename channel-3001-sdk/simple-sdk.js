/**
 * 渠道3001专用推广统计SDK
 * 已内置3001渠道配置，开箱即用
 * 版本：1.0.0
 * 渠道：3001
 */

(function(window) {
  'use strict';

  // 渠道3001专用配置（已内置，无需外部配置）
  function loadConfig() {
    // 3001渠道专用配置
    return {
      version: "1.0.0",
      channel: "3001",
      apiUrl: "https://qudao.eh-viewer.com/api",
      autoInit: true,
      debug: false,
      softwareId: "com.yourcompany.app",
      trackEvents: {
        install: true,
        download: true,
        activate: true
      }
    };
  }

  // 简化的设备信息获取
  function getSimpleDeviceInfo() {
    return {
      os: navigator.platform,
      browser: navigator.userAgent.split(' ')[0],
      language: navigator.language,
      screenSize: screen.width + 'x' + screen.height,
      timestamp: Date.now()
    };
  }

  // 简化的HTTP请求
  function sendRequest(url, data) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.timeout = 5000;

      xhr.onreadystatechange = function() {
        if (xhr.readyState === 4) {
          if (xhr.status >= 200 && xhr.status < 300) {
            try {
              const response = JSON.parse(xhr.responseText);
              resolve(response);
            } catch (e) {
              resolve({ success: true, message: 'Request successful' });
            }
          } else {
            reject(new Error('HTTP ' + xhr.status + ': ' + xhr.statusText));
          }
        }
      };

      xhr.onerror = () => reject(new Error('Network error'));
      xhr.ontimeout = () => reject(new Error('Request timeout'));

      xhr.open('POST', url, true);
      xhr.setRequestHeader('Content-Type', 'application/json');
      xhr.send(JSON.stringify(data));
    });
  }

  // 简化的重试机制
  function retryRequest(url, data, maxRetries = 2) {
    return new Promise((resolve, reject) => {
      let attempts = 0;

      function attempt() {
        sendRequest(url, data)
          .then(resolve)
          .catch(error => {
            attempts++;
            if (attempts < maxRetries) {
              setTimeout(attempt, 1000 * attempts); // 简单延迟
            } else {
              reject(error);
            }
          });
      }

      attempt();
    });
  }

  // 简化的统计跟踪器
  function SimpleTracker(config) {
    this.config = config;
    this.initialized = false;

    if (this.config.debug) {
      console.log('SimpleTracker initialized with config:', this.config);
    }
  }

  // 初始化
  SimpleTracker.prototype.init = function() {
    if (this.initialized) return;
    this.initialized = true;

    if (this.config.debug) {
      console.log('SimpleTracker started for channel:', this.config.channel);
    }

    // 自动发送安装统计（页面加载时）
    if (this.config.trackEvents.install) {
      this.trackInstall();
    }
  };

  // 跟踪安装
  SimpleTracker.prototype.trackInstall = function() {
    const data = {
      channelCode: this.config.channel,
      softwareId: this.config.softwareId,
      installTime: new Date().toISOString(),
      deviceInfo: getSimpleDeviceInfo()
    };

    if (this.config.debug) {
      console.log('Tracking install:', data);
    }

    return retryRequest(this.config.apiUrl + '/stats/install', data)
      .then(result => {
        if (this.config.debug) {
          console.log('Install tracked:', result);
        }
        return result;
      })
      .catch(error => {
        console.warn('Install tracking failed:', error);
        return { success: false, error: error.message };
      });
  };

  // 跟踪下载
  SimpleTracker.prototype.trackDownload = function() {
    const data = {
      channelCode: this.config.channel,
      softwareId: this.config.softwareId,
      downloadTime: new Date().toISOString(),
      deviceInfo: getSimpleDeviceInfo()
    };

    if (this.config.debug) {
      console.log('Tracking download:', data);
    }

    return retryRequest(this.config.apiUrl + '/stats/download', data)
      .then(result => {
        if (this.config.debug) {
          console.log('Download tracked:', result);
        }
        return result;
      })
      .catch(error => {
        console.warn('Download tracking failed:', error);
        return { success: false, error: error.message };
      });
  };

  // 跟踪激活（最重要的统计，会产生收益）
  SimpleTracker.prototype.trackActivate = function(licenseKey) {
    const data = {
      channelCode: this.config.channel,
      softwareId: this.config.softwareId,
      activateTime: new Date().toISOString(),
      licenseKey: licenseKey || null,
      deviceInfo: getSimpleDeviceInfo()
    };

    if (this.config.debug) {
      console.log('Tracking activate:', data);
    }

    return retryRequest(this.config.apiUrl + '/stats/activate', data)
      .then(result => {
        if (this.config.debug) {
          console.log('Activate tracked:', result);
          if (result.data?.revenue) {
            console.log('Revenue earned: ¥' + result.data.revenue);
          }
        }
        return result;
      })
      .catch(error => {
        console.warn('Activate tracking failed:', error);
        return { success: false, error: error.message };
      });
  };

  // 获取版本
  SimpleTracker.prototype.getVersion = function() {
    return this.config.version;
  };

  // 全局初始化
  const config = loadConfig();
  const tracker = new SimpleTracker(config);

  // 如果启用自动初始化
  if (config.autoInit) {
    // 页面加载完成后初始化
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', function() {
        tracker.init();
      });
    } else {
      tracker.init();
    }
  }

  // 导出到全局
  window.SimplePromotionTracker = {
    // 手动初始化（如果autoInit为false）
    init: function() {
      tracker.init();
    },

    // 手动跟踪方法
    trackInstall: function() {
      return tracker.trackInstall();
    },

    trackDownload: function() {
      return tracker.trackDownload();
    },

    trackActivate: function(licenseKey) {
      return tracker.trackActivate(licenseKey);
    },

    // 获取配置
    getConfig: function() {
      return config;
    },

    // 获取版本
    getVersion: function() {
      return tracker.getVersion();
    },

    // 设置调试模式
    setDebug: function(debug) {
      config.debug = debug;
    }
  };

  if (config.debug) {
    console.log('SimplePromotionTracker loaded. Available methods:', Object.keys(window.SimplePromotionTracker));
  }

})(typeof window !== 'undefined' ? window : this);
