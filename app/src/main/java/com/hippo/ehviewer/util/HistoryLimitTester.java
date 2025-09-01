package com.hippo.ehviewer.util;

import android.content.Context;
import android.util.Log;
import com.hippo.ehviewer.client.HistoryManager;

/**
 * 历史记录限制功能测试工具
 * 用于验证历史记录自动清理功能是否正常工作
 */
public class HistoryLimitTester {
    private static final String TAG = "HistoryLimitTester";
    private final Context context;
    private final HistoryManager historyManager;

    public HistoryLimitTester(Context context) {
        this.context = context.getApplicationContext();
        this.historyManager = HistoryManager.getInstance(context);
    }

    /**
     * 执行完整的历史记录限制测试
     */
    public void runFullTest() {
        Log.i(TAG, "=== 开始历史记录限制功能测试 ===");

        // 1. 测试当前状态
        testCurrentState();

        // 2. 生成测试数据
        generateTestData();

        // 3. 验证清理功能
        testCleanupFunction();

        // 4. 验证统计功能
        testStatistics();

        Log.i(TAG, "=== 历史记录限制功能测试完成 ===");
    }

    /**
     * 测试当前历史记录状态
     */
    private void testCurrentState() {
        Log.d(TAG, "测试当前历史记录状态...");

        int currentCount = historyManager.getHistoryCount();
        int maxCount = historyManager.getMaxHistoryCount();

        Log.d(TAG, "当前记录数: " + currentCount);
        Log.d(TAG, "最大限制数: " + maxCount);
        Log.d(TAG, "限制状态: " + (currentCount <= maxCount ? "正常" : "超出限制"));

        if (currentCount > maxCount) {
            Log.w(TAG, "警告: 当前记录数超出限制，建议清理");
        }
    }

    /**
     * 生成测试历史记录数据
     */
    private void generateTestData() {
        Log.d(TAG, "生成测试历史记录数据...");

        int currentCount = historyManager.getHistoryCount();
        int maxCount = historyManager.getMaxHistoryCount();
        int targetCount = maxCount + 5; // 生成超出限制的记录

        Log.d(TAG, "计划生成 " + (targetCount - currentCount) + " 条测试记录");

        for (int i = currentCount; i < targetCount; i++) {
            String url = "https://test-history-site-" + i + ".com";
            String title = "测试历史记录网站 " + i;
            historyManager.addHistory(title, url);

            if ((i - currentCount + 1) % 5 == 0) {
                Log.d(TAG, "已生成 " + (i - currentCount + 1) + " 条测试记录");
            }
        }

        Log.d(TAG, "测试数据生成完成");
    }

    /**
     * 测试清理功能
     */
    private void testCleanupFunction() {
        Log.d(TAG, "测试历史记录清理功能...");

        int beforeCount = historyManager.getHistoryCount();
        int maxCount = historyManager.getMaxHistoryCount();

        Log.d(TAG, "清理前记录数: " + beforeCount);

        if (beforeCount > maxCount) {
            Log.d(TAG, "记录数超出限制，等待自动清理...");

            // 等待一小段时间让清理操作完成
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "等待清理操作时被中断", e);
            }

            int afterCount = historyManager.getHistoryCount();
            Log.d(TAG, "清理后记录数: " + afterCount);

            if (afterCount <= maxCount) {
                Log.i(TAG, "✅ 清理功能正常工作");
            } else {
                Log.e(TAG, "❌ 清理功能异常，记录数仍超出限制");
            }
        } else {
            Log.i(TAG, "记录数未超出限制，无需清理");
        }
    }

    /**
     * 测试统计功能
     */
    private void testStatistics() {
        Log.d(TAG, "测试统计功能...");

        int count = historyManager.getHistoryCount();
        int maxCount = historyManager.getMaxHistoryCount();

        Log.d(TAG, "统计信息: " + count + "/" + maxCount);

        if (count <= maxCount) {
            Log.i(TAG, "✅ 统计功能正常");
        } else {
            Log.e(TAG, "❌ 统计数据异常");
        }
    }

    /**
     * 快速测试方法
     */
    public void quickTest() {
        Log.i(TAG, "=== 快速历史记录限制测试 ===");

        int count = historyManager.getHistoryCount();
        int maxCount = historyManager.getMaxHistoryCount();

        Log.i(TAG, "历史记录状态: " + count + "/" + maxCount +
                   " (" + (count <= maxCount ? "正常" : "超出限制") + ")");

        Log.i(TAG, "=== 快速测试完成 ===");
    }

    /**
     * 清理测试数据
     */
    public void cleanupTestData() {
        Log.d(TAG, "清理测试历史记录数据...");

        // 注意：这个方法会清除所有历史记录，只在测试时使用
        // historyManager.clearAllHistory();

        Log.w(TAG, "⚠️  测试数据清理功能已禁用，避免误删用户数据");
        Log.w(TAG, "如需清理测试数据，请手动调用clearAllHistory()");
    }
}
