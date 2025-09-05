package com.hippo.ehviewer.ui.browser;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.browser.RealtimeSuggestionManager;
import com.hippo.ehviewer.ui.browser.RealtimeSuggestionManager.SuggestionItem;

import java.util.List;

/**
 * 智能地址栏组件 - Chrome风格的地址栏和建议列表
 */
public class SmartAddressBarWidget extends FrameLayout {

    // UI组件
    private EditText mAddressEditText;
    private com.google.android.material.button.MaterialButton mClearButton;
    private com.google.android.material.button.MaterialButton mRefreshButton;
    private RecyclerView mSuggestionsRecyclerView;
    private com.google.android.material.card.MaterialCardView mAddressBarContainer;
    private com.google.android.material.card.MaterialCardView mSuggestionsContainer;

    // 数据组件
    private RealtimeSuggestionManager mSuggestionManager;
    private EnhancedSuggestionAdapter mSuggestionAdapter;
    private AddressBarAnimator mAnimator;

    // 状态变量
    private String mCurrentUrl = "";
    private boolean mKeyboardNavigationActive = false;
    private int mSelectedSuggestionIndex = -1;
    private List<SuggestionItem> mCurrentSuggestions;

    // 回调接口
    public interface OnAddressBarListener {
        void onUrlSubmit(String url);
        void onSuggestionClick(SuggestionItem item);
        void onSuggestionLongClick(SuggestionItem item);
    }

    private OnAddressBarListener mListener;

    public SmartAddressBarWidget(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SmartAddressBarWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SmartAddressBarWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.widget_smart_address_bar, this, true);

        // 初始化UI组件
        mAddressBarContainer = findViewById(R.id.address_bar_container);
        mSuggestionsContainer = findViewById(R.id.suggestions_container);
        mAddressEditText = findViewById(R.id.address_edit_text);
        mClearButton = findViewById(R.id.clear_button);
        mRefreshButton = findViewById(R.id.refresh_button);
        mSuggestionsRecyclerView = findViewById(R.id.suggestions_recycler_view);

        // 初始化数据组件
        mSuggestionManager = RealtimeSuggestionManager.getInstance(getContext());
        mSuggestionAdapter = new EnhancedSuggestionAdapter(getContext());
        mAnimator = new AddressBarAnimator();

        // 设置RecyclerView
        mSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mSuggestionsRecyclerView.setAdapter(mSuggestionAdapter);
        
        // 初始化动画状态
        mAnimator.animateInitialEntry(mAddressBarContainer, mSuggestionsContainer);

        // 设置监听器
        setupListeners();
    }

    private void setupListeners() {
        // 地址栏文本变化监听
        mAddressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                handleTextChanged(query);
            }
        });

        // 地址栏键盘事件监听
        mAddressEditText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return handleKeyEvent(keyCode, event);
            }
            return false;
        });

        // 地址栏焦点变化监听
        mAddressEditText.setOnFocusChangeListener((v, hasFocus) -> {
            mAnimator.animateFocusChange(mAddressBarContainer, hasFocus, null);
        });
        
        // 地址栏编辑器动作监听
        mAddressEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleEnterKey();
                return true;
            }
            return false;
        });

        // 建议项点击监听
        mSuggestionAdapter.setOnSuggestionClickListener(new EnhancedSuggestionAdapter.OnSuggestionClickListener() {
            @Override
            public void onSuggestionClick(SuggestionItem item) {
                handleSuggestionClick(item);
            }

            @Override
            public void onSuggestionLongClick(SuggestionItem item) {
                handleSuggestionLongClick(item);
            }
        });

        // 清除按钮点击监听
        mClearButton.setOnClickListener(v -> {
            mAddressEditText.setText("");
            mAddressEditText.requestFocus();
            mAnimator.animateClearButtonVisibility(mClearButton, false);
        });

        // 刷新按钮点击监听
        mRefreshButton.setOnClickListener(v -> {
            String currentText = mAddressEditText.getText().toString().trim();
            if (!currentText.isEmpty()) {
                mAnimator.animateRefreshButton(mRefreshButton, true);
                submitUrl(currentText);
            }
        });
    }

    /**
     * 处理文本变化
     */
    private void handleTextChanged(String query) {
        // 更新清除按钮可见性
        updateClearButtonVisibility(query);
        
        if (query.isEmpty()) {
            hideSuggestions();
            return;
        }

        // 请求建议
        mSuggestionManager.requestSuggestions(query, new RealtimeSuggestionManager.SuggestionCallback() {
            @Override
            public void onSuggestionsReady(List<SuggestionItem> suggestions) {
                showSuggestions(suggestions, query);
            }

            @Override
            public void onError(String error) {
                hideSuggestions();
            }
        });
    }

    /**
     * 处理键盘事件
     */
    private boolean handleKeyEvent(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return handleArrowKey(true);
            case KeyEvent.KEYCODE_DPAD_UP:
                return handleArrowKey(false);
            case KeyEvent.KEYCODE_TAB:
                return handleTabKey();
            case KeyEvent.KEYCODE_ENTER:
                return handleEnterKey();
            case KeyEvent.KEYCODE_ESCAPE:
                return handleEscapeKey();
            default:
                return false;
        }
    }

    /**
     * 处理方向键导航
     */
    private boolean handleArrowKey(boolean down) {
        if (!mKeyboardNavigationActive) {
            mKeyboardNavigationActive = true;
            mSelectedSuggestionIndex = down ? 0 : getLastSelectableIndex();
        } else {
            if (down) {
                mSelectedSuggestionIndex = Math.min(mSelectedSuggestionIndex + 1, getLastSelectableIndex());
            } else {
                mSelectedSuggestionIndex = Math.max(mSelectedSuggestionIndex - 1, 0);
            }
        }

        updateSelectionHighlight();
        return true;
    }

    /**
     * 处理Tab键补全
     */
    private boolean handleTabKey() {
        if (mCurrentSuggestions != null && !mCurrentSuggestions.isEmpty()) {
            int actualIndex = findActualPositionForSuggestionIndex(mSelectedSuggestionIndex);
            if (actualIndex >= 0 && actualIndex < mCurrentSuggestions.size()) {
                SuggestionItem item = mCurrentSuggestions.get(actualIndex);
                setAddressText(item.text);
                clearSelectionState();
                return true;
            }
        }
        return false;
    }

    /**
     * 处理Enter键提交
     */
    private boolean handleEnterKey() {
        String text = mAddressEditText.getText().toString().trim();
        if (!text.isEmpty()) {
            submitUrl(text);
            return true;
        }
        return false;
    }

    /**
     * 处理Escape键取消
     */
    private boolean handleEscapeKey() {
        clearSelectionState();
        hideSuggestions();
        return true;
    }

    /**
     * 提交URL
     */
    private void submitUrl(String text) {
        if (mListener != null) {
            mListener.onUrlSubmit(text);
        }
        mSuggestionManager.recordUrlVisit(text, text);
        hideSuggestions();
        clearSelectionState();
    }

    /**
     * 处理建议项点击
     */
    private void handleSuggestionClick(SuggestionItem item) {
        // 记录用户点击行为用于智能学习
        String currentQuery = mAddressEditText.getText().toString().trim();
        mSuggestionManager.recordSuggestionClick(currentQuery, item);
        
        if (item.url != null && !item.url.isEmpty()) {
            setAddressText(item.url);
            submitUrl(item.url);
        } else {
            // 搜索建议
            setAddressText(item.text);
            submitUrl(item.text);
        }

        if (mListener != null) {
            mListener.onSuggestionClick(item);
        }
    }

    /**
     * 处理建议项长按
     */
    private void handleSuggestionLongClick(SuggestionItem item) {
        if (mListener != null) {
            mListener.onSuggestionLongClick(item);
        }
    }

    /**
     * 显示建议列表
     */
    private void showSuggestions(List<SuggestionItem> suggestions, String query) {
        mCurrentSuggestions = suggestions;
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionAdapter.setCurrentQuery(query);
        
        // 使用动画显示建议列表
        mAnimator.showSuggestions(mSuggestionsContainer, () -> {
            // 动画完成后的回调
            clearSelectionState();
        });
    }

    /**
     * 隐藏建议列表
     */
    private void hideSuggestions() {
        // 使用动画隐藏建议列表
        mAnimator.hideSuggestions(mSuggestionsContainer, () -> {
            // 动画完成后的回调
            mCurrentSuggestions = null;
            clearSelectionState();
        });
    }

    /**
     * 更新选择高亮
     */
    private void updateSelectionHighlight() {
        mSuggestionAdapter.setSelectedPosition(
            findActualPositionForSuggestionIndex(mSelectedSuggestionIndex)
        );
        scrollToSelectedItem();
    }

    /**
     * 滚动到选中项
     */
    private void scrollToSelectedItem() {
        int actualPosition = findActualPositionForSuggestionIndex(mSelectedSuggestionIndex);
        if (actualPosition >= 0) {
            mSuggestionsRecyclerView.smoothScrollToPosition(actualPosition);
        }
    }

    /**
     * 清除选择状态
     */
    private void clearSelectionState() {
        mSelectedSuggestionIndex = -1;
        mKeyboardNavigationActive = false;
        mSuggestionAdapter.clearSelection();
    }

    /**
     * 查找建议项的实际位置
     */
    private int findActualPositionForSuggestionIndex(int suggestionIndex) {
        if (mCurrentSuggestions == null || suggestionIndex < 0 || suggestionIndex >= mCurrentSuggestions.size()) {
            return -1;
        }

        int currentSuggestionIndex = 0;
        for (int i = 0; i < mSuggestionAdapter.getItemCount(); i++) {
            if (mSuggestionAdapter.getSuggestionAt(i) != null) {
                if (currentSuggestionIndex == suggestionIndex) {
                    return i;
                }
                currentSuggestionIndex++;
            }
        }
        return -1;
    }

    /**
     * 获取最后一个可选择的位置
     */
    private int getLastSelectableIndex() {
        return mCurrentSuggestions != null ? mCurrentSuggestions.size() - 1 : 0;
    }

    /**
     * 设置地址栏文本
     */
    public void setAddressText(String text) {
        mAddressEditText.setText(text);
        mAddressEditText.setSelection(text.length());
        updateClearButtonVisibility(text);
    }

    /**
     * 获取当前地址栏文本
     */
    public String getAddressText() {
        return mAddressEditText.getText().toString();
    }

    /**
     * 设置监听器
     */
    public void setOnAddressBarListener(OnAddressBarListener listener) {
        mListener = listener;
    }

    /**
     * 设置当前URL
     */
    public void setCurrentUrl(String url) {
        mCurrentUrl = url != null ? url : "";
        if (!mAddressEditText.isFocused()) {
            setAddressText(mCurrentUrl);
        }
    }

    /**
     * 获取当前URL
     */
    public String getCurrentUrl() {
        return mCurrentUrl;
    }

    /**
     * 请求焦点
     */
    public void requestAddressFocus() {
        mAddressEditText.requestFocus();
    }

    /**
     * 清除焦点
     */
    public void clearAddressFocus() {
        mAddressEditText.clearFocus();
    }

    /**
     * 显示加载状态
     */
    public void showLoadingState() {
        mAnimator.animateRefreshButton(mRefreshButton, true);
    }

    /**
     * 显示正常状态
     */
    public void showNormalState() {
        mAnimator.animateRefreshButton(mRefreshButton, false);
    }
    
    /**
     * 销毁时取消所有动画
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnimator != null) {
            mAnimator.cancelAllAnimations();
        }
    }

    /**
     * 更新清除按钮可见性
     */
    private void updateClearButtonVisibility(String text) {
        if (mClearButton != null) {
            boolean shouldShow = !text.isEmpty();
            mAnimator.animateClearButtonVisibility(mClearButton, shouldShow);
        }
    }
}
