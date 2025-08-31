/*
 * EhViewer Network Module - NetworkRequest
 * 网络请求封装类
 */

package com.hippo.ehviewer.network;

/**
 * 网络请求封装类
 * 封装请求参数、回调等信息
 */
public class NetworkRequest {

    private int method;
    private Object[] args;
    private NetworkClient.NetworkCallback callback;
    private NetworkClient.NetworkConfig config;
    private NetworkClient.NetworkTask task;
    private boolean cancelled = false;

    public NetworkRequest(int method, Object[] args, NetworkClient.NetworkCallback callback) {
        this(method, args, callback, new NetworkClient.NetworkConfig());
    }

    public NetworkRequest(int method, Object[] args, NetworkClient.NetworkCallback callback, NetworkClient.NetworkConfig config) {
        this.method = method;
        this.args = args;
        this.callback = callback;
        this.config = config;
    }

    // Getters and setters
    public int getMethod() { return method; }
    public void setMethod(int method) { this.method = method; }

    public Object[] getArgs() { return args; }
    public void setArgs(Object[] args) { this.args = args; }

    public NetworkClient.NetworkCallback getCallback() { return callback; }
    public void setCallback(NetworkClient.NetworkCallback callback) { this.callback = callback; }

    public NetworkClient.NetworkConfig getConfig() { return config; }
    public void setConfig(NetworkClient.NetworkConfig config) { this.config = config; }

    public NetworkClient.NetworkTask getTask() { return task; }
    public void setTask(NetworkClient.NetworkTask task) { this.task = task; }

    public boolean isCancelled() { return cancelled; }

    /**
     * 取消请求
     */
    public void cancel() {
        cancelled = true;
        if (task != null) {
            task.stop();
        }
    }
}
