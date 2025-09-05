package com.hippo.ehviewer.ui.browser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.JsResult;
import android.webkit.JsPromptResult;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.R;

/**
 * 增强版WebChromeClient
 * 参考YCWebView实现，提供完整的浏览器功能支持
 */
public class EnhancedWebChromeClient extends WebChromeClient {

    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final String TAG = "EnhancedWebChromeClient";

    private Activity mActivity;
    private ValueCallback<Uri[]> mFilePathCallback;
    private ValueCallback<Uri> mFilePathCallbackLegacy;

    public EnhancedWebChromeClient(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        // 进度变化由Activity处理
        if (mActivity instanceof ProgressCallback) {
            ((ProgressCallback) mActivity).onProgressChanged(newProgress);
        }
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        // 标题更新由Activity处理
        if (mActivity instanceof TitleCallback) {
            ((TitleCallback) mActivity).onTitleReceived(title);
        }
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                   FileChooserParams fileChooserParams) {
        // 处理现代的文件选择器
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;

        try {
            Intent intent = fileChooserParams.createIntent();
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // 设置文件类型过滤
            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            if (acceptTypes != null && acceptTypes.length > 0) {
                intent.setType(acceptTypes[0]);
            } else {
                intent.setType("*/*");
            }

            // 支持多选
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.getMode() ==
                FileChooserParams.MODE_OPEN_MULTIPLE);

            mActivity.startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
            return true;
        } catch (Exception e) {
            android.util.Log.e(TAG, "File chooser failed", e);
            mFilePathCallback = null;
            return false;
        }
    }

    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        super.onConsoleMessage(message, lineNumber, sourceID);
        // 控制台消息监听
        android.util.Log.d(TAG, "Console: " + sourceID + ":" + lineNumber + " " + message);
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        // 定制alert对话框
        if (mActivity instanceof JsDialogCallback) {
            ((JsDialogCallback) mActivity).onJsAlert(message, result);
            return true;
        }
        return super.onJsAlert(view, url, message, result);
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        // 定制confirm对话框
        if (mActivity instanceof JsDialogCallback) {
            ((JsDialogCallback) mActivity).onJsConfirm(message, result);
            return true;
        }
        return super.onJsConfirm(view, url, message, result);
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        // 定制prompt对话框
        if (mActivity instanceof JsDialogCallback) {
            ((JsDialogCallback) mActivity).onJsPrompt(message, defaultValue, result);
            return true;
        }
        return super.onJsPrompt(view, url, message, defaultValue, result);
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        // 处理地理位置权限
        callback.invoke(origin, true, false);
        super.onGeolocationPermissionsShowPrompt(origin, callback);
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        // 处理权限请求
        String[] resources = request.getResources();
        for (String resource : resources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                // 授予摄像头权限用于视频通话等
                request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                break;
            }
        }
        super.onPermissionRequest(request);
    }

    /**
     * 处理文件选择结果
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (mFilePathCallback != null) {
                Uri[] results = null;

                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        String dataString = data.getDataString();
                        android.net.Uri dataUri = data.getData();

                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        } else if (dataUri != null) {
                            results = new Uri[]{dataUri};
                        }
                    }
                }

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            }

            if (mFilePathCallbackLegacy != null) {
                Uri result = (resultCode == Activity.RESULT_OK && data != null) ?
                    data.getData() : null;
                mFilePathCallbackLegacy.onReceiveValue(result);
                mFilePathCallbackLegacy = null;
            }
        }
    }

    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        void onProgressChanged(int progress);
    }

    /**
     * 标题回调接口
     */
    public interface TitleCallback {
        void onTitleReceived(String title);
    }

    /**
     * JavaScript对话框回调接口
     */
    public interface JsDialogCallback {
        void onJsAlert(String message, JsResult result);
        void onJsConfirm(String message, JsResult result);
        void onJsPrompt(String message, String defaultValue, JsPromptResult result);
    }
}
