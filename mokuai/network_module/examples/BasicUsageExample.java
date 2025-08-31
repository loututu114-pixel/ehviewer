/*
 * EhViewer Network Module - Basic Usage Example
 * 网络模块基本使用示例
 */

package com.hippo.ehviewer.network.examples;

import android.content.Context;
import android.util.Log;

import com.hippo.ehviewer.network.NetworkClient;
import com.hippo.ehviewer.network.NetworkRequest;

/**
 * 基本使用示例
 * 演示如何使用网络模块进行基本的HTTP请求
 */
public class BasicUsageExample {

    private static final String TAG = BasicUsageExample.class.getSimpleName();
    private final Context mContext;
    private final NetworkClient mNetworkClient;

    public BasicUsageExample(Context context) {
        mContext = context;
        mNetworkClient = new NetworkClient(context);
    }

    /**
     * 执行GET请求示例
     */
    public void performGetRequest() {
        String url = "https://jsonplaceholder.typicode.com/posts/1";

        NetworkClient.NetworkCallback<String> callback = new NetworkClient.NetworkCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "GET request success: " + result);
                // 处理成功结果
                // 例如：解析JSON数据，更新UI等
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "GET request failed: " + e.getMessage());
                // 处理失败情况
                // 例如：显示错误提示，重试请求等
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "GET request cancelled");
                // 处理取消情况
            }
        };

        Object[] args = {url};
        NetworkRequest request = new NetworkRequest(NetworkClient.METHOD_GET, args, callback);
        mNetworkClient.execute(request);
    }

    /**
     * 执行POST请求示例
     */
    public void performPostRequest() {
        String url = "https://jsonplaceholder.typicode.com/posts";
        String postData = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";

        NetworkClient.NetworkCallback<String> callback = new NetworkClient.NetworkCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "POST request success: " + result);
                // 处理POST响应
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "POST request failed: " + e.getMessage());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "POST request cancelled");
            }
        };

        Object[] args = {url, postData};
        NetworkRequest request = new NetworkRequest(NetworkClient.METHOD_POST, args, callback);
        mNetworkClient.execute(request);
    }

    /**
     * 演示请求取消
     */
    public void demonstrateRequestCancellation() {
        String url = "https://httpbin.org/delay/10"; // 延迟10秒的请求

        NetworkClient.NetworkCallback<String> callback = new NetworkClient.NetworkCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Request completed: " + result);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Request was cancelled");
            }
        };

        Object[] args = {url};
        NetworkRequest request = new NetworkRequest(NetworkClient.METHOD_GET, args, callback);
        mNetworkClient.execute(request);

        // 5秒后取消请求
        new android.os.Handler().postDelayed(() -> {
            Log.d(TAG, "Cancelling request...");
            request.cancel();
        }, 5000);
    }

    /**
     * 演示带配置的请求
     */
    public void performConfiguredRequest() {
        String url = "https://api.example.com/data";

        // 创建自定义配置
        NetworkClient.NetworkConfig config = new NetworkClient.NetworkConfig();
        config.setTimeout(10000); // 10秒超时
        config.setEnableCache(true);
        config.setEnableRetry(true);
        config.setMaxRetries(2);

        NetworkClient.NetworkCallback<String> callback = new NetworkClient.NetworkCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Configured request success: " + result);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Configured request failed: " + e.getMessage());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Configured request cancelled");
            }
        };

        Object[] args = {url};
        NetworkRequest request = new NetworkRequest(NetworkClient.METHOD_GET, args, callback, config);
        mNetworkClient.execute(request);
    }
}
