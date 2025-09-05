package com.hippo.ehviewer.userscript;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * 用户脚本存储管理器
 */
public class ScriptStorage {

    private static final String PREF_NAME = "userscript_prefs";
    private static final String KEY_SCRIPTS = "installed_scripts";
    private static final String SCRIPTS_DIR = "userscripts";

    private Context context;
    private SharedPreferences prefs;
    private Gson gson;
    private File scriptsDir;

    public ScriptStorage(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.scriptsDir = new File(context.getFilesDir(), SCRIPTS_DIR);

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
        }
    }

    /**
     * 保存脚本列表到SharedPreferences
     */
    public void saveScripts(List<UserScript> scripts) {
        String json = gson.toJson(scripts);
        prefs.edit().putString(KEY_SCRIPTS, json).apply();
    }

    /**
     * 从SharedPreferences加载脚本列表
     */
    public List<UserScript> loadScripts() {
        String json = prefs.getString(KEY_SCRIPTS, null);
        if (json != null) {
            try {
                return gson.fromJson(json, new TypeToken<List<UserScript>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

    /**
     * 保存单个脚本到文件
     */
    public boolean saveScriptToFile(UserScript script) {
        try {
            File scriptFile = new File(scriptsDir, script.getId() + ".js");
            try (FileOutputStream fos = new FileOutputStream(scriptFile)) {
                fos.write(script.getContent().getBytes("UTF-8"));
                fos.flush();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从文件加载脚本
     */
    public String loadScriptFromFile(String scriptId) {
        try {
            File scriptFile = new File(scriptsDir, scriptId + ".js");
            if (!scriptFile.exists()) return null;

            try (FileInputStream fis = new FileInputStream(scriptFile)) {
                byte[] data = new byte[(int) scriptFile.length()];
                int bytesRead = fis.read(data);
                if (bytesRead != data.length) {
                    throw new IOException("Failed to read complete file");
                }
                return new String(data, "UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删除脚本文件
     */
    public boolean deleteScriptFile(String scriptId) {
        File scriptFile = new File(scriptsDir, scriptId + ".js");
        return scriptFile.delete();
    }

    /**
     * 获取所有脚本文件
     */
    public File[] getAllScriptFiles() {
        return scriptsDir.listFiles((dir, name) -> name.endsWith(".js"));
    }

    /**
     * 清理无效的脚本文件
     */
    public void cleanupInvalidScripts(List<String> validScriptIds) {
        File[] files = getAllScriptFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String scriptId = fileName.substring(0, fileName.lastIndexOf('.'));
                if (!validScriptIds.contains(scriptId)) {
                    file.delete();
                }
            }
        }
    }

    /**
     * 导出脚本到外部存储
     */
    public boolean exportScript(UserScript script, File exportDir) {
        try {
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File exportFile = new File(exportDir, script.getName() + ".js");
            try (FileOutputStream fos = new FileOutputStream(exportFile)) {
                // 生成完整的脚本内容（包含头部信息）
                String fullContent = generateFullScriptContent(script);
                fos.write(fullContent.getBytes("UTF-8"));
                fos.flush();
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 生成完整的脚本内容
     */
    private String generateFullScriptContent(UserScript script) {
        StringBuilder sb = new StringBuilder();

        // 添加脚本头部
        sb.append("// ==UserScript==\n");
        if (script.getName() != null) sb.append("// @name ").append(script.getName()).append("\n");
        if (script.getVersion() != null) sb.append("// @version ").append(script.getVersion()).append("\n");
        if (script.getAuthor() != null) sb.append("// @author ").append(script.getAuthor()).append("\n");
        if (script.getDescription() != null) sb.append("// @description ").append(script.getDescription()).append("\n");

        // 添加匹配规则
        for (String pattern : script.getIncludePatterns()) {
            sb.append("// @include ").append(pattern).append("\n");
        }

        for (String pattern : script.getExcludePatterns()) {
            sb.append("// @exclude ").append(pattern).append("\n");
        }

        sb.append("// ==/UserScript==\n\n");

        // 添加脚本内容
        sb.append(script.getContent());

        return sb.toString();
    }

    /**
     * 获取存储使用情况
     */
    public long getStorageUsage() {
        return calculateDirectorySize(scriptsDir);
    }

    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(File dir) {
        if (dir == null || !dir.exists()) return 0;

        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }
}
