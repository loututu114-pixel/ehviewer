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

import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 广告拦截管理器
 */
public class AdBlockManager {

    private static final String TAG = "AdBlockManager";

    // 广告域名黑名单
    private static final Set<String> AD_DOMAINS = new HashSet<>(Arrays.asList(
            // Google Ads
            "googlesyndication.com",
            "googleadservices.com",
            "googletagservices.com",
            "googletagmanager.com",
            "doubleclick.net",
            "googletagmanager.com",

            // Facebook Ads
            "facebook.com/tr",
            "facebook.net",
            "connect.facebook.net",

            // Amazon Ads
            "amazon-adsystem.com",

            // Other common ad networks
            "adsystem.amazon.com",
            "aax.amazon-adsystem.com",
            "s.amazon-adsystem.com",
            "c.amazon-adsystem.com",

            // Analytics (optional blocking)
            "google-analytics.com",
            "googletagmanager.com",
            "analytics.google.com",

            // Common ad servers
            "ads.twitter.com",
            "t.co",
            "bit.ly",
            "tinyurl.com",

            // Porn site ads
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "exoclick.com",
            "trafficfactory.biz",
            "trafficjunky.net",
            "fapceo.com",
            "juicyads.com",
            "luckypushh.com",
            "porngames.com",
            "adultgamesworld.com",
            "gamcore.com",

            // 博彩广告 - Gambling Ads
            "bet365.com",
            "betway.com",
            "888.com",
            "pokerstars.com",
            "partypoker.com",
            "bwin.com",
            "williamhill.com",
            "ladbrokes.com",
            "coral.co.uk",
            "paddypower.com",
            "unibet.com",
            "betfair.com",
            "skybet.com",
            "betfred.com",
            "betvictor.com",
            "totesport.com",
            "boylesports.com",
            "grosvenorcasinos.com",
            "gala-casino.com",
            "casino.com",
            "slotland.com",
            "jackpot.com",
            "spinpalace.com",
            "royalvegas.com",
            "platinumplay.com",
            "captaincookscasino.com",
            "rubyfortune.com",
            "quatro-casino.com",
            "nostalgicacai.com",
            "mummysgold.com",
            "luckynugget.com",
            "luckynuggetcasino.com",
            "casinobarcelona.com",
            "dazzlecasino.com",
            "cashpotcasino.com",
            "cashpot.com",
            "cashpotcasino.com",
            "luckynugget.com",
            "luckynuggetcasino.com",
            "luckynuggetcasino.com",

            // 更多博彩域名
            "bet365affiliates.com",
            "betwaypartners.com",
            "888affiliates.com",
            "pokerstarsaffiliates.com",
            "partypokeraffiliates.com",
            "bwinpartners.com",
            "williamhillaffiliates.com",
            "ladbrokesaffiliates.com",
            "coralaffiliates.com",
            "paddypoweraffiliates.com",
            "unibetpartners.com",
            "betfairaffiliates.com",
            "skybetaffiliates.com",
            "betfredaffiliates.com",

            // 色情网站广告网络
            "adultfriendfinder.com",
            "bangbros.com",
            "realitykings.com",
            "naughtyamerica.com",
            "teamskeet.com",
            "blacked.com",
            "tushy.com",
            "vixen.com",
            "digitalplayground.com",
            "evilangel.com",
            "julesjordan.com",

            // 更多广告网络
            "adroll.com",
            "criteo.com",
            "outbrain.com",
            "taboola.com",
            "revcontent.com",
            "content.ad",
            "adsystem.",
            "advertising.com",
            "adtechus.com",
            "adnxs.com",
            "pubmatic.com",
            "openx.com",
            "rubiconproject.com",
            "appnexus.com",
            "thetradeDesk.com",
            "media.net",
            "contextweb.com",
            "lijit.com",
            "sovrn.com",
            "gumgum.com",
            "sharethrough.com",
            "triplelift.com",
            "yieldmo.com",
            "33across.com",
            "conversantmedia.com",
            "smartadserver.com",
            "adition.com",
            "stroeer.de",
            "emetriq.de",
            "yieldlab.net",
            "adscale.de",
            "plista.com",
            "themediagrid.com",
            "e-planning.net"
    ));

    // 广告URL模式
    private static final Set<String> AD_URL_PATTERNS = new HashSet<>(Arrays.asList(
            "googlesyndication.com",
            "googleadservices.com",
            "doubleclick.net",
            "adsystem.",
            "amazon-adsystem.com",
            "facebook.com/tr",
            "analytics.",
            "googletagmanager.com",
            "googletagservices.com",
            "popads.",
            "propellerads.",
            "exoclick.",
            "trafficfactory.",
            "trafficjunky.",
            "fapceo.",
            "juicyads.",
            "luckypushh.",
            "porngames.",
            "adultgamesworld.",
            "gamcore.",
            // 博彩广告模式
            "bet365",
            "betway",
            "888.",
            "pokerstars",
            "partypoker",
            "bwin.",
            "williamhill",
            "ladbrokes",
            "coral.",
            "paddypower",
            "unibet",
            "betfair",
            "skybet",
            "betfred",
            "betvictor",
            "totesport",
            "boylesports",
            "grosvenorcasinos",
            "gala-casino",
            "casino.",
            "slotland",
            "jackpot",
            "spinpalace",
            "royalvegas",
            "platinumplay",
            "captaincookscasino",
            "rubyfortune",
            "quatro-casino",
            "nostalgicacai",
            "mummysgold",
            "luckynugget",
            "casinobarcelona",
            "dazzlecasino",
            "cashpot",
            // 更多广告网络模式
            "adroll.",
            "criteo.",
            "outbrain.",
            "taboola.",
            "revcontent.",
            "content.ad",
            "advertising.",
            "adtechus.",
            "adnxs.",
            "pubmatic.",
            "openx.",
            "rubiconproject.",
            "appnexus.",
            "thetradeDesk.",
            "media.net",
            "contextweb.",
            "lijit.",
            "sovrn.",
            "gumgum.",
            "sharethrough.",
            "triplelift.",
            "yieldmo.",
            "33across.",
            "conversantmedia.",
            "smartadserver.",
            "adition.",
            "stroeer.",
            "emetriq.",
            "yieldlab.",
            "adscale.",
            "plista.",
            "themediagrid.",
            "e-planning."
    ));

    private static AdBlockManager sInstance;
    private boolean mAdBlockEnabled = true;

    private AdBlockManager() {
    }

    public static synchronized AdBlockManager getInstance() {
        if (sInstance == null) {
            sInstance = new AdBlockManager();
        }
        return sInstance;
    }

    /**
     * 设置广告拦截是否启用
     */
    public void setAdBlockEnabled(boolean enabled) {
        mAdBlockEnabled = enabled;
    }

    /**
     * 获取广告拦截状态
     */
    public boolean isAdBlockEnabled() {
        return mAdBlockEnabled;
    }

    /**
     * 检查URL是否为广告
     */
    public boolean isAd(String url) {
        if (!mAdBlockEnabled || url == null) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        // 检查域名黑名单
        for (String domain : AD_DOMAINS) {
            if (lowerUrl.contains(domain)) {
                android.util.Log.d(TAG, "Blocked ad domain: " + domain + " in URL: " + url);
                return true;
            }
        }

        // 检查URL模式
        for (String pattern : AD_URL_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                android.util.Log.d(TAG, "Blocked ad pattern: " + pattern + " in URL: " + url);
                return true;
            }
        }

        // 检查常见的广告文件类型
        if (lowerUrl.contains("/ads/") ||
            lowerUrl.contains("banner") ||
            lowerUrl.contains("popup") ||
            lowerUrl.contains("interstitial") ||
            lowerUrl.endsWith(".gif") && lowerUrl.contains("ad") ||
            lowerUrl.contains("advertisement")) {
            android.util.Log.d(TAG, "Blocked ad file pattern in URL: " + url);
            return true;
        }

        return false;
    }

    /**
     * 创建拦截广告的WebViewClient
     */
    public WebViewClient createAdBlockWebViewClient(WebViewClient originalClient) {
        return new AdBlockWebViewClient(originalClient);
    }

    /**
     * 广告拦截WebViewClient
     */
    private class AdBlockWebViewClient extends WebViewClient {

        private final WebViewClient mOriginalClient;

        AdBlockWebViewClient(WebViewClient originalClient) {
            this.mOriginalClient = originalClient;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            if (isAd(url)) {
                android.util.Log.d(TAG, "Blocked ad request: " + url);
                // 返回空的响应来阻止广告加载
                return new WebResourceResponse("text/plain", "utf-8",
                    new ByteArrayInputStream("".getBytes()));
            }

            // 让原始客户端处理非广告请求
            if (mOriginalClient != null) {
                return mOriginalClient.shouldInterceptRequest(view, request);
            }

            return super.shouldInterceptRequest(view, request);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (isAd(url)) {
                android.util.Log.d(TAG, "Blocked ad request: " + url);
                // 返回空的响应来阻止广告加载
                return new WebResourceResponse("text/plain", "utf-8",
                    new ByteArrayInputStream("".getBytes()));
            }

            // 让原始客户端处理非广告请求
            if (mOriginalClient != null) {
                return mOriginalClient.shouldInterceptRequest(view, url);
            }

            return super.shouldInterceptRequest(view, url);
        }

        // 代理其他方法到原始客户端
        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            if (mOriginalClient != null) {
                mOriginalClient.onPageStarted(view, url, favicon);
            } else {
                super.onPageStarted(view, url, favicon);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (mOriginalClient != null) {
                mOriginalClient.onPageFinished(view, url);
            } else {
                super.onPageFinished(view, url);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (mOriginalClient != null) {
                return mOriginalClient.shouldOverrideUrlLoading(view, request);
            }
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mOriginalClient != null) {
                return mOriginalClient.shouldOverrideUrlLoading(view, url);
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    /**
     * 添加自定义广告域名
     */
    public void addAdDomain(String domain) {
        if (domain != null && !domain.trim().isEmpty()) {
            AD_DOMAINS.add(domain.toLowerCase());
        }
    }

    /**
     * 移除广告域名
     */
    public void removeAdDomain(String domain) {
        if (domain != null) {
            AD_DOMAINS.remove(domain.toLowerCase());
        }
    }

    /**
     * 获取广告域名列表
     */
    public Set<String> getAdDomains() {
        return new HashSet<>(AD_DOMAINS);
    }
}
