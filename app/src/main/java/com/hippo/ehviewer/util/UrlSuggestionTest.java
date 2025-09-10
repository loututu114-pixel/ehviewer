package com.hippo.ehviewer.util;

import android.content.Context;
import android.util.Log;
import com.hippo.ehviewer.client.data.HistoryInfo;
import java.util.List;

/**
 * URL补全功能测试类
 * 用于验证智能补全功能是否正常工作
 */
public class UrlSuggestionTest {
    private static final String TAG = "UrlSuggestionTest";
    private final Context mContext;
    private final DomainSuggestionManager mManager;

    public UrlSuggestionTest(Context context) {
        this.mContext = context;
        this.mManager = new DomainSuggestionManager(context);
    }

    /**
     * 运行所有测试
     */
    public void runAllTests() {
        Log.d(TAG, "开始运行URL补全功能测试...");

        testProtocolSuggestions();
        testAliasSuggestions();
        testDomainSuggestions();
        testSearchSuggestions();
        testPopularityTracking();

        Log.d(TAG, "所有测试完成！");
    }

    /**
     * 测试协议补全
     */
    private void testProtocolSuggestions() {
        Log.d(TAG, "测试协议补全功能:");

        String[] testInputs = {"htt", "http", "ftp", "ftps", "ssh", "git", "mailto", "tel", "sms"};

        for (String input : testInputs) {
            List<DomainSuggestionManager.SuggestionItem> suggestions = mManager.getSuggestions(input);
            if (!suggestions.isEmpty()) {
                DomainSuggestionManager.SuggestionItem first = suggestions.get(0);
                if (first.type == DomainSuggestionManager.SuggestionType.PROTOCOL) {
                    Log.d(TAG, "✓ " + input + " → " + first.url);
                }
            }
        }
    }

    /**
     * 测试别名补全
     */
    private void testAliasSuggestions() {
        Log.d(TAG, "测试别名补全功能:");

        String[] testInputs = {"goo", "fb", "tw", "yt", "bd", "tb", "wb", "zh", "amz", "bbc"};

        for (String input : testInputs) {
            List<DomainSuggestionManager.SuggestionItem> suggestions = mManager.getSuggestions(input);
            for (DomainSuggestionManager.SuggestionItem item : suggestions) {
                if (item.type == DomainSuggestionManager.SuggestionType.DOMAIN ||
                    item.type == DomainSuggestionManager.SuggestionType.ALIAS) {
                    Log.d(TAG, "✓ " + input + " → " + item.url);
                    break;
                }
            }
        }
    }

    /**
     * 测试域名补全
     */
    private void testDomainSuggestions() {
        Log.d(TAG, "测试域名补全功能:");

        String[] testInputs = {"google", "baidu", "youtube", "github", "stackoverflow"};

        for (String input : testInputs) {
            List<DomainSuggestionManager.SuggestionItem> suggestions = mManager.getSuggestions(input);
            for (DomainSuggestionManager.SuggestionItem item : suggestions) {
                if (item.type == DomainSuggestionManager.SuggestionType.DOMAIN) {
                    Log.d(TAG, "✓ " + input + " → " + item.url);
                    break;
                }
            }
        }
    }

    /**
     * 测试搜索建议
     */
    private void testSearchSuggestions() {
        Log.d(TAG, "测试搜索建议功能:");

        String[] testInputs = {"java tutorial", "android development", "machine learning"};

        for (String input : testInputs) {
            List<DomainSuggestionManager.SuggestionItem> suggestions = mManager.getSuggestions(input);
            for (DomainSuggestionManager.SuggestionItem item : suggestions) {
                if (item.type == DomainSuggestionManager.SuggestionType.SEARCH) {
                    Log.d(TAG, "✓ " + input + " → " + item.url);
                    break;
                }
            }
        }
    }

    /**
     * 测试使用频率跟踪
     */
    private void testPopularityTracking() {
        Log.d(TAG, "测试使用频率跟踪功能:");

        // 模拟使用一些域名
        String[] domains = {"google.com", "github.com", "stackoverflow.com"};
        for (String domain : domains) {
            for (int i = 0; i < 3; i++) {
                mManager.increaseDomainPopularity(domain);
            }
        }

        // 检查使用频率
        for (String domain : domains) {
            int popularity = mManager.getDomainPopularity(domain);
            Log.d(TAG, "✓ " + domain + " 使用频率: " + popularity);
        }
    }

    /**
     * 打印所有主流域名
     */
    public void printPopularDomains() {
        Log.d(TAG, "主流网站域名列表:");

        // 这里我们无法直接访问私有字段，但可以通过测试输入来展示
        String[] sampleInputs = {"google", "baidu", "youtube", "github", "amazon", "facebook"};

        for (String input : sampleInputs) {
            List<DomainSuggestionManager.SuggestionItem> suggestions = mManager.getSuggestions(input);
            Log.d(TAG, input + " 补全建议:");
            for (int i = 0; i < Math.min(3, suggestions.size()); i++) {
                DomainSuggestionManager.SuggestionItem item = suggestions.get(i);
                Log.d(TAG, "  " + item.getDisplayText());
            }
        }
    }
}
