package com.hippo.ehviewer.userscript;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用户脚本更新器
 */
public class ScriptUpdater {

    private Context context;
    private UserScriptManager scriptManager;
    private ScriptStorage storage;
    private ExecutorService executor;
    private Handler mainHandler;

    public interface UpdateCallback {
        void onUpdateStart();
        void onUpdateProgress(String scriptName, int progress);
        void onUpdateSuccess(UserScript script);
        void onUpdateFailed(String scriptName, String error);
        void onUpdateComplete();
    }

    public ScriptUpdater(Context context, UserScriptManager scriptManager, ScriptStorage storage) {
        this.context = context;
        this.scriptManager = scriptManager;
        this.storage = storage;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 检查所有脚本更新
     */
    public void checkAllUpdates(UpdateCallback callback) {
        executor.execute(() -> {
            mainHandler.post(callback::onUpdateStart);

            List<UserScript> scripts = scriptManager.getAllScripts();
            int totalScripts = scripts.size();
            final java.util.concurrent.atomic.AtomicInteger processed = new java.util.concurrent.atomic.AtomicInteger(0);

            for (UserScript script : scripts) {
                if (script.getUpdateUrl() != null && !script.getUpdateUrl().isEmpty()) {
                    updateScript(script, new UpdateCallback() {
                        @Override
                        public void onUpdateStart() {}

                        @Override
                        public void onUpdateProgress(String scriptName, int progress) {
                            mainHandler.post(() -> callback.onUpdateProgress(scriptName, progress));
                        }

                        @Override
                        public void onUpdateSuccess(UserScript updatedScript) {
                            mainHandler.post(() -> callback.onUpdateSuccess(updatedScript));
                        }

                        @Override
                        public void onUpdateFailed(String scriptName, String error) {
                            mainHandler.post(() -> callback.onUpdateFailed(scriptName, error));
                        }

                        @Override
                        public void onUpdateComplete() {
                            if (processed.incrementAndGet() >= totalScripts) {
                                mainHandler.post(callback::onUpdateComplete);
                            }
                        }
                    });
                } else {
                    processed.incrementAndGet();
                }
            }

            if (processed.get() >= totalScripts) {
                mainHandler.post(callback::onUpdateComplete);
            }
        });
    }

    /**
     * 更新单个脚本
     */
    public void updateScript(UserScript script, UpdateCallback callback) {
        if (script.getUpdateUrl() == null || script.getUpdateUrl().isEmpty()) {
            callback.onUpdateFailed(script.getName(), "没有更新URL");
            callback.onUpdateComplete();
            return;
        }

        executor.execute(() -> {
            try {
                mainHandler.post(() -> callback.onUpdateProgress(script.getName(), 10));

                // 下载新版本
                String newContent = downloadScript(script.getUpdateUrl());
                if (newContent == null) {
                    mainHandler.post(() -> {
                        callback.onUpdateFailed(script.getName(), "下载失败");
                        callback.onUpdateComplete();
                    });
                    return;
                }

                mainHandler.post(() -> callback.onUpdateProgress(script.getName(), 50));

                // 解析新版本
                UserScript newScript = UserScriptParser.parse(newContent);
                if (newScript == null) {
                    mainHandler.post(() -> {
                        callback.onUpdateFailed(script.getName(), "解析失败");
                        callback.onUpdateComplete();
                    });
                    return;
                }

                mainHandler.post(() -> callback.onUpdateProgress(script.getName(), 80));

                // 检查版本是否更新
                if (isNewerVersion(script.getVersion(), newScript.getVersion())) {
                    // 更新脚本
                    script.setContent(newScript.getContent());
                    script.setVersion(newScript.getVersion());
                    script.setLastUpdateTime(System.currentTimeMillis());

                    // 保存到文件
                    storage.saveScriptToFile(script);

                    mainHandler.post(() -> {
                        callback.onUpdateProgress(script.getName(), 100);
                        callback.onUpdateSuccess(script);
                        callback.onUpdateComplete();
                    });
                } else {
                    mainHandler.post(() -> {
                        callback.onUpdateProgress(script.getName(), 100);
                        callback.onUpdateComplete();
                    });
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    callback.onUpdateFailed(script.getName(), e.getMessage());
                    callback.onUpdateComplete();
                });
            }
        });
    }

    /**
     * 下载脚本内容
     */
    private String downloadScript(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "EhViewer/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                return content.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 比较版本号
     */
    private boolean isNewerVersion(String currentVersion, String newVersion) {
        if (currentVersion == null) return true;
        if (newVersion == null) return false;

        try {
            String[] current = currentVersion.split("\\.");
            String[] latest = newVersion.split("\\.");

            int length = Math.max(current.length, latest.length);
            for (int i = 0; i < length; i++) {
                int currentPart = i < current.length ? Integer.parseInt(current[i]) : 0;
                int latestPart = i < latest.length ? Integer.parseInt(latest[i]) : 0;

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            return false; // 版本相同
        } catch (Exception e) {
            // 如果版本格式不正确，假设新版本更新
            return !currentVersion.equals(newVersion);
        }
    }

    /**
     * 停止更新服务
     */
    public void shutdown() {
        executor.shutdown();
    }
}
