package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.NovelLibraryManager;
import com.hippo.ehviewer.util.EroNovelDetector;
import com.hippo.ehviewer.util.NovelContentExtractor;

/**
 * 小说阅读器Activity
 * 类似图片查看器的沉浸式小说阅读体验
 */
public class NovelReaderActivity extends AppCompatActivity {

    private static final String TAG = "NovelReaderActivity";

    public static final String EXTRA_NOVEL_URL = "novel_url";
    public static final String EXTRA_NOVEL_TITLE = "novel_title";
    public static final String EXTRA_NOVEL_CONTENT = "novel_content";

    // UI控件
    private ScrollView scrollView;
    private TextView titleText;
    private TextView contentText;
    private TextView progressText;
    private ProgressBar loadingProgress;
    private LinearLayout controlPanel;
    private SeekBar progressSeekBar;

    // 控制按钮
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private ImageButton btnBookmark;
    private ImageButton btnSettings;
    private ImageButton btnLibrary;

    // 手势检测
    private GestureDetector gestureDetector;
    private boolean isFullscreen = true;

    // 数据
    private String novelUrl;
    private String novelTitle;
    private String novelContent;
    private EroNovelDetector.NovelInfo novelInfo;
    private NovelLibraryManager libraryManager;

    // 阅读设置
    private float textSize = 18f;
    private int textColor = Color.BLACK;
    private int backgroundColor = Color.WHITE;
    private boolean isNightMode = false;

    // 进度相关
    private int currentProgress = 0;
    private Handler progressHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏设置
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_novel_reader);

        // 初始化数据
        initData();

        // 初始化视图
        initViews();

        // 初始化手势
        initGestures();

        // 加载小说内容
        loadNovelContent();
    }

    private void initData() {
        Intent intent = getIntent();
        novelUrl = intent.getStringExtra(EXTRA_NOVEL_URL);
        novelTitle = intent.getStringExtra(EXTRA_NOVEL_TITLE);
        novelContent = intent.getStringExtra(EXTRA_NOVEL_CONTENT);

        libraryManager = NovelLibraryManager.getInstance(this);

        // 如果有URL，尝试从书库中获取信息
        if (novelUrl != null) {
            novelInfo = libraryManager.findNovelByUrl(novelUrl);
        }
    }

    private void initViews() {
        // 初始化主要视图
        scrollView = findViewById(R.id.scroll_view);
        titleText = findViewById(R.id.title_text);
        contentText = findViewById(R.id.content_text);
        progressText = findViewById(R.id.progress_text);
        loadingProgress = findViewById(R.id.loading_progress);
        controlPanel = findViewById(R.id.control_panel);
        progressSeekBar = findViewById(R.id.progress_seekbar);

        // 初始化控制按钮
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        btnBookmark = findViewById(R.id.btn_bookmark);
        btnSettings = findViewById(R.id.btn_settings);
        btnLibrary = findViewById(R.id.btn_library);

        // 设置按钮监听器
        setupButtonListeners();

        // 设置进度条
        setupProgressControls();

        // 设置初始UI状态
        updateFullscreenMode();
        updateReadingSettings();
    }

    private void setupButtonListeners() {
        // 上一章
        btnPrevious.setOnClickListener(v -> {
            showToast("上一章功能开发中");
        });

        // 下一章
        btnNext.setOnClickListener(v -> {
            showToast("下一章功能开发中");
        });

        // 书签
        btnBookmark.setOnClickListener(v -> {
            toggleBookmark();
        });

        // 设置
        btnSettings.setOnClickListener(v -> {
            showReadingSettings();
        });

        // 书库
        btnLibrary.setOnClickListener(v -> {
            openNovelLibrary();
        });
    }

    private void setupProgressControls() {
        if (progressSeekBar != null) {
            progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentProgress = progress;
                        updateProgressText();
                        saveReadingProgress();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void initGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControlPanel();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                toggleFullscreen();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                showContextMenu();
            }
        });

        // 设置触摸监听器
        scrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    private void loadNovelContent() {
        loadingProgress.setVisibility(View.VISIBLE);

        if (novelContent != null && !novelContent.isEmpty()) {
            // 直接显示提供的内容
            displayNovelContent(novelTitle, novelContent);
            loadingProgress.setVisibility(View.GONE);
        } else if (novelUrl != null) {
            // 从网页提取内容
            extractContentFromWeb();
        } else {
            showToast("没有小说内容可显示");
            finish();
        }
    }

    private void extractContentFromWeb() {
        // 这里应该使用NovelContentExtractor从网页提取内容
        // 暂时使用模拟数据
        displayNovelContent("示例小说", "这是小说内容...\n\n第二段内容...");
        loadingProgress.setVisibility(View.GONE);
    }

    private void displayNovelContent(String title, String content) {
        // 设置标题
        if (titleText != null) {
            titleText.setText(title != null ? title : "无标题");
        }

        // 设置内容
        if (contentText != null && content != null) {
            // 格式化内容
            SpannableString formattedContent = formatNovelContent(content);
            contentText.setText(formattedContent);
            contentText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        // 更新进度
        updateProgressText();

        Log.d(TAG, "Displayed novel content: " + title);
    }

    private SpannableString formatNovelContent(String content) {
        SpannableString spannable = new SpannableString(content);

        // 简单的段落格式化
        String[] paragraphs = content.split("\n");
        int start = 0;

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }

            // 为段落添加样式
            spannable.setSpan(new StyleSpan(Typeface.NORMAL), start, start + paragraph.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            start += paragraph.length() + 1; // +1 for newline
        }

        return spannable;
    }

    private void toggleControlPanel() {
        if (controlPanel.getVisibility() == View.VISIBLE) {
            controlPanel.setVisibility(View.GONE);
        } else {
            controlPanel.setVisibility(View.VISIBLE);
            // 自动隐藏
            progressHandler.postDelayed(() -> {
                if (controlPanel.getVisibility() == View.VISIBLE) {
                    controlPanel.setVisibility(View.GONE);
                }
            }, 3000);
        }
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        updateFullscreenMode();
    }

    private void updateFullscreenMode() {
        if (isFullscreen) {
            // 隐藏系统UI
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            // 显示系统UI
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void updateReadingSettings() {
        // 更新字体大小
        if (contentText != null) {
            contentText.setTextSize(textSize);
            contentText.setTextColor(textColor);
        }

        // 更新背景色
        if (scrollView != null) {
            scrollView.setBackgroundColor(backgroundColor);
        }

        // 更新夜间模式
        if (isNightMode) {
            textColor = Color.parseColor("#E0E0E0");
            backgroundColor = Color.parseColor("#1A1A1A");
        } else {
            textColor = Color.BLACK;
            backgroundColor = Color.WHITE;
        }

        applyReadingSettings();
    }

    private void applyReadingSettings() {
        if (contentText != null) {
            contentText.setTextColor(textColor);
            contentText.setTextSize(textSize);
        }

        if (scrollView != null) {
            scrollView.setBackgroundColor(backgroundColor);
        }
    }

    private void updateProgressText() {
        if (progressText != null && contentText != null) {
            int totalLength = contentText.getText().length();
            if (totalLength > 0) {
                int progress = (int) ((float) currentProgress / 100 * totalLength);
                progressText.setText("阅读进度: " + currentProgress + "%");
            }
        }
    }

    private void saveReadingProgress() {
        if (novelInfo != null && libraryManager != null) {
            novelInfo.readProgress = currentProgress;
            novelInfo.lastReadTime = System.currentTimeMillis();
            // 这里可以调用libraryManager更新进度
        }
    }

    private void toggleBookmark() {
        if (novelInfo != null) {
            showToast("书签功能开发中");
        } else {
            showToast("请先收藏此小说");
        }
    }

    private void showReadingSettings() {
        // 简单的设置面板
        LinearLayout settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setPadding(32, 32, 32, 32);
        settingsPanel.setBackgroundColor(Color.parseColor("#80000000"));

        // 字体大小调节
        TextView sizeLabel = new TextView(this);
        sizeLabel.setText("字体大小");
        sizeLabel.setTextColor(Color.WHITE);
        settingsPanel.addView(sizeLabel);

        SeekBar sizeSeekBar = new SeekBar(this);
        sizeSeekBar.setMax(30);
        sizeSeekBar.setProgress((int) textSize);
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSize = progress + 12; // 最小12sp
                updateReadingSettings();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        settingsPanel.addView(sizeSeekBar);

        // 夜间模式切换
        TextView nightLabel = new TextView(this);
        nightLabel.setText("夜间模式");
        nightLabel.setTextColor(Color.WHITE);
        nightLabel.setOnClickListener(v -> {
            isNightMode = !isNightMode;
            updateReadingSettings();
            showToast("夜间模式: " + (isNightMode ? "开" : "关"));
        });
        settingsPanel.addView(nightLabel);

        // 显示设置面板
        controlPanel.addView(settingsPanel, 0);
    }

    private void openNovelLibrary() {
        Intent intent = new Intent(this, NovelLibraryActivity.class);
        startActivity(intent);
    }

    private void showContextMenu() {
        // 显示上下文菜单
        showToast("长按菜单功能开发中");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFullscreenMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveReadingProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressHandler != null) {
            progressHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onBackPressed() {
        if (controlPanel.getVisibility() == View.VISIBLE) {
            controlPanel.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
