/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client;

import android.webkit.WebView;
import android.util.Log;

/**
 * JavaScript优化管理器
 * 优化WebView中JavaScript的执行性能
 */
public class JavaScriptOptimizer {

    private static final String TAG = "JavaScriptOptimizer";

    private static JavaScriptOptimizer sInstance;

    /**
     * 获取单例实例
     */
    public static synchronized JavaScriptOptimizer getInstance() {
        if (sInstance == null) {
            sInstance = new JavaScriptOptimizer();
        }
        return sInstance;
    }

    private JavaScriptOptimizer() {
        // 私有构造函数
    }

    /**
     * 为WebView应用JavaScript优化
     */
    public void optimizeWebView(WebView webView) {
        if (webView == null) {
            return;
        }

        try {
            // 注入性能优化JavaScript
            injectPerformanceOptimizations(webView);

            // 注入内存优化
            injectMemoryOptimizations(webView);

            // 注入渲染优化
            injectRenderingOptimizations(webView);

            Log.d(TAG, "JavaScript optimizations applied to WebView");
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply JavaScript optimizations", e);
        }
    }

    /**
     * 注入性能优化JavaScript
     */
    private void injectPerformanceOptimizations(WebView webView) {
        String performanceScript =
            "(function() {" +
            "  // 禁用不必要的动画" +
            "  var style = document.createElement('style');" +
            "  style.textContent = '* { animation-duration: 0.01ms !important; animation-delay: 0ms !important; }';" +
            "  document.head.appendChild(style);" +
            "" +
            "  // 优化事件处理" +
            "  window.addEventListener('load', function() {" +
            "    // 延迟加载非关键资源" +
            "    setTimeout(function() {" +
            "      var lazyElements = document.querySelectorAll('[data-lazy]');" +
            "      lazyElements.forEach(function(el) {" +
            "        el.style.display = 'block';" +
            "      });" +
            "    }, 100);" +
            "  });" +
            "" +
            "  // 优化滚动性能" +
            "  var scrollOptimization = function() {" +
            "    document.body.style.webkitTransform = 'translateZ(0)';" +
            "    document.body.style.transform = 'translateZ(0)';" +
            "  };" +
            "  window.addEventListener('scroll', scrollOptimization);" +
            "" +
            "  console.log('Performance optimizations applied');" +
            "})();";

        webView.evaluateJavascript(performanceScript, null);
    }

    /**
     * 注入内存优化JavaScript
     */
    private void injectMemoryOptimizations(WebView webView) {
        String memoryScript =
            "(function() {" +
            "  // 清理不必要的DOM元素" +
            "  var cleanupInterval = setInterval(function() {" +
            "    // 清理离屏的图片" +
            "    var images = document.getElementsByTagName('img');" +
            "    for (var i = 0; i < images.length; i++) {" +
            "      var img = images[i];" +
            "      var rect = img.getBoundingClientRect();" +
            "      if (rect.bottom < -100 || rect.top > window.innerHeight + 100) {" +
            "        if (img.hasAttribute('data-original-src')) {" +
            "          img.src = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';" +
            "        }" +
            "      }" +
            "    }" +
            "  }, 5000);" +
            "" +
            "  // 监听页面卸载事件" +
            "  window.addEventListener('beforeunload', function() {" +
            "    clearInterval(cleanupInterval);" +
            "    // 清理事件监听器" +
            "    window.removeEventListener('scroll', scrollOptimization);" +
            "  });" +
            "" +
            "  console.log('Memory optimizations applied');" +
            "})();";

        webView.evaluateJavascript(memoryScript, null);
    }

    /**
     * 注入渲染优化JavaScript
     */
    private void injectRenderingOptimizations(WebView webView) {
        String renderingScript =
            "(function() {" +
            "  // 启用硬件加速" +
            "  var acceleratedElements = document.querySelectorAll('.accelerated, canvas, video');" +
            "  acceleratedElements.forEach(function(el) {" +
            "    el.style.webkitTransform = 'translateZ(0)';" +
            "    el.style.transform = 'translateZ(0)';" +
            "    el.style.webkitBackfaceVisibility = 'hidden';" +
            "    el.style.backfaceVisibility = 'hidden';" +
            "  });" +
            "" +
            "  // 优化字体渲染" +
            "  document.body.style.webkitFontSmoothing = 'antialiased';" +
            "  document.body.style.textRendering = 'optimizeLegibility';" +
            "" +
            "  // 减少重排重绘" +
            "  var optimizeLayout = function() {" +
            "    document.body.style.willChange = 'auto';" +
            "    setTimeout(function() {" +
            "      document.body.style.willChange = 'auto';" +
            "    }, 1000);" +
            "  };" +
            "  window.addEventListener('resize', optimizeLayout);" +
            "" +
            "  console.log('Rendering optimizations applied');" +
            "})();";

        webView.evaluateJavascript(renderingScript, null);
    }

    /**
     * 注入图片懒加载优化
     */
    public void injectImageLazyLoad(WebView webView) {
        String lazyLoadScript =
            "(function() {" +
            "  // 图片懒加载实现" +
            "  function isElementInViewport(el) {" +
            "    var rect = el.getBoundingClientRect();" +
            "    return (" +
            "      rect.top >= 0 &&" +
            "      rect.left >= 0 &&" +
            "      rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&" +
            "      rect.right <= (window.innerWidth || document.documentElement.clientWidth)" +
            "    );" +
            "  }" +
            "" +
            "  function lazyLoadImages() {" +
            "    var images = document.querySelectorAll('img[data-src]');" +
            "    images.forEach(function(img) {" +
            "      if (isElementInViewport(img)) {" +
            "        img.src = img.getAttribute('data-src');" +
            "        img.removeAttribute('data-src');" +
            "      }" +
            "    });" +
            "  }" +
            "" +
            "  // 初始加载" +
            "  lazyLoadImages();" +
            "" +
            "  // 滚动时加载" +
            "  window.addEventListener('scroll', lazyLoadImages);" +
            "  window.addEventListener('resize', lazyLoadImages);" +
            "" +
            "  console.log('Image lazy loading enabled');" +
            "})();";

        webView.evaluateJavascript(lazyLoadScript, null);
    }

    /**
     * 注入广告拦截优化
     */
    public void injectAdBlocking(WebView webView) {
        String adBlockScript =
            "(function() {" +
            "  // 常见的广告选择器" +
            "  var adSelectors = [" +
            "    '[id*=\"ad\"]'," +
            "    '[class*=\"ad\"]'," +
            "    '[id*=\"banner\"]'," +
            "    '[class*=\"banner\"]'," +
            "    'iframe[src*=\"ads\"]'," +
            "    'iframe[src*=\"doubleclick\"]'," +
            "    'iframe[src*=\"googlesyndication\"]'," +
            "    '.adsbygoogle'," +
            "    '.ad-container'," +
            "    '#ad-banner'" +
            "  ];" +
            "" +
            "  function hideAds() {" +
            "    adSelectors.forEach(function(selector) {" +
            "      var elements = document.querySelectorAll(selector);" +
            "      elements.forEach(function(el) {" +
            "        el.style.display = 'none !important';" +
            "        console.log('Ad hidden:', selector);" +
            "      });" +
            "    });" +
            "  }" +
            "" +
            "  // 页面加载完成后隐藏广告" +
            "  if (document.readyState === 'complete') {" +
            "    hideAds();" +
            "  } else {" +
            "    window.addEventListener('load', hideAds);" +
            "  }" +
            "" +
            "  // 使用MutationObserver监听DOM变化" +
            "  var observer = new MutationObserver(function(mutations) {" +
            "    mutations.forEach(function(mutation) {" +
            "      if (mutation.type === 'childList') {" +
            "        hideAds();" +
            "      }" +
            "    });" +
            "  });" +
            "" +
            "  observer.observe(document.body, {" +
            "    childList: true," +
            "    subtree: true" +
            "  });" +
            "" +
            "  console.log('Ad blocking enabled');" +
            "})();";

        webView.evaluateJavascript(adBlockScript, null);
    }

    /**
     * 注入用户体验优化
     */
    public void injectUXOptimizations(WebView webView) {
        String uxScript =
            "(function() {" +
            "  // 优化触摸响应" +
            "  document.addEventListener('touchstart', function() {}, { passive: true });" +
            "  document.addEventListener('touchmove', function() {}, { passive: true });" +
            "" +
            "  // 优化点击响应" +
            "  var clickElements = document.querySelectorAll('a, button, [onclick]');" +
            "  clickElements.forEach(function(el) {" +
            "    el.style.webkitTapHighlightColor = 'rgba(0,0,0,0.1)';" +
            "    el.style.tapHighlightColor = 'rgba(0,0,0,0.1)';" +
            "  });" +
            "" +
            "  // 优化表单输入" +
            "  var inputs = document.querySelectorAll('input, textarea');" +
            "  inputs.forEach(function(input) {" +
            "    input.style.webkitAppearance = 'none';" +
            "    input.style.appearance = 'none';" +
            "    input.style.borderRadius = '4px';" +
            "  });" +
            "" +
            "  console.log('UX optimizations applied');" +
            "})();";

        webView.evaluateJavascript(uxScript, null);
    }

    /**
     * 获取性能统计JavaScript
     */
    public String getPerformanceStatsScript() {
        return "(function() {" +
                "  var perfData = {" +
                "    domContentLoaded: 0," +
                "    loadComplete: 0," +
                "    firstPaint: 0," +
                "    domElements: 0" +
                "  };" +
                "" +
                "  window.addEventListener('DOMContentLoaded', function() {" +
                "    perfData.domContentLoaded = performance.now();" +
                "  });" +
                "" +
                "  window.addEventListener('load', function() {" +
                "    perfData.loadComplete = performance.now();" +
                "    perfData.domElements = document.getElementsByTagName('*').length;" +
                "    console.log('Performance Stats:', JSON.stringify(perfData));" +
                "  });" +
                "" +
                "  if (window.performance && window.performance.timing) {" +
                "    var timing = window.performance.timing;" +
                "    perfData.firstPaint = timing.loadEventEnd - timing.navigationStart;" +
                "  }" +
                "" +
                "  return perfData;" +
                "})();";
    }
}
