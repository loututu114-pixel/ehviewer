# 搜索下拉列表显示问题修复指南

## 问题描述
EhViewer应用中，顶部搜索框的下拉建议列表被下面的浏览框（SearchLayout）覆盖，导致输入建议无法正常显示。

## 修复方案

### 1. 布局层级调整
**修改文件**: `app/src/main/res/layout/scene_gallery_list.xml`

**主要变更**:
- 将 `SearchBar` 移至布局最上层
- 添加 `android:elevation="8dp"` 提升显示层级
- 将 `SearchLayout` 默认设为 `android:visibility="gone"`

### 2. 状态管理优化
**修改文件**: `app/src/main/java/com/hippo/ehviewer/ui/scene/gallery/list/GalleryListScene.java`

**主要变更**:
- 在 `onStateChange` 方法中添加布局可见性控制
- 当显示搜索下拉列表时，隐藏 `SearchLayout` 和 FAB 按钮
- 确保下拉列表不会被其他UI元素覆盖

### 3. SearchBarMover协调
**修改文件**: `app/src/main/java/com/hippo/ehviewer/ui/scene/gallery/list/GalleryListScene.java`

**主要变更**:
- 修改 `forceShowSearchBar` 方法
- 当下拉列表显示时，强制保持搜索栏位置不变
- 防止滚动时搜索栏移动导致下拉列表错位

## 测试步骤

### 功能测试清单

#### ✅ 正常状态测试
1. 打开EhViewer应用主界面
2. 确认搜索栏显示在顶部
3. 确认搜索布局（SearchLayout）默认隐藏
4. 确认FAB按钮正常显示

#### ✅ 搜索模式测试
1. 点击搜索栏进入搜索模式
2. 确认搜索布局（SearchLayout）显示
3. 确认可以正常输入文字
4. 确认FAB按钮仍然可见

#### ✅ 下拉列表测试
1. 在搜索框中输入文字（如"test"）
2. 确认下拉建议列表正常显示
3. 确认下拉列表不被其他UI元素覆盖
4. 确认可以正常选择建议项

#### ✅ 滚动测试
1. 显示下拉列表后滚动页面
2. 确认搜索栏位置保持不变
3. 确认下拉列表仍然可见
4. 确认下拉列表内容不被遮挡

#### ✅ 退出搜索测试
1. 关闭搜索模式回到正常状态
2. 确认搜索布局隐藏
3. 确认FAB按钮重新显示
4. 确认搜索栏回到原始位置

### 兼容性测试

#### ✅ 不同设备测试
- 小屏幕设备（确保下拉列表不超出屏幕边界）
- 大屏幕设备（确保布局正常显示）
- 横屏模式（确保布局适应性）

#### ✅ Android版本测试
- Android 7.0+ （最低支持版本）
- Android 10+ （确保新版本兼容）
- Android 13+ （确保最新版本兼容）

## 预期效果

### 修复前的问题：
- [x] 下拉建议列表被SearchLayout覆盖
- [x] 输入建议无法正常显示
- [x] 滚动时搜索栏移动导致下拉列表错位

### 修复后的效果：
- [x] 下拉建议列表正常显示在最上层
- [x] 搜索布局在下拉列表显示时自动隐藏
- [x] 滚动时搜索栏位置保持稳定
- [x] FAB按钮在适当时候隐藏/显示

## 技术细节

### 布局层级结构（修复后）
```
FrameLayout (main_layout)
├── ContentLayout (content_layout) - 内容区域
├── SearchLayout (search_layout) - 搜索选项界面 (动态显示/隐藏)
├── FabLayout (fab_layout) - 浮动按钮 (动态显示/隐藏)
└── SearchBar (search_bar) - 搜索输入框 (最上层，elevation=8dp)
    └── 下拉建议列表 (通过ListView实现)
```

### 状态管理逻辑
```java
// 当显示下拉列表时
if (newState == SearchBar.STATE_SEARCH_LIST) {
    mSearchLayout.setVisibility(View.GONE);  // 隐藏搜索布局
    mFabLayout.setVisibility(View.GONE);     // 隐藏FAB按钮
    // 保持SearchBar位置不变
}

// 当回到搜索模式时
if (newState == SearchBar.STATE_SEARCH) {
    mSearchLayout.setVisibility(View.VISIBLE); // 显示搜索布局
    mFabLayout.setVisibility(View.VISIBLE);    // 显示FAB按钮
}
```

## 性能影响

### 修复前：
- UI层级冲突导致渲染问题
- 滚动时频繁重新布局
- 可能的内存泄漏（UI元素重叠）

### 修复后：
- 清晰的UI层级结构
- 减少不必要的布局计算
- 更好的用户体验

## 注意事项

1. **向后兼容性**: 确保修复不影响现有功能
2. **性能优化**: 动态显示/隐藏操作已优化，避免频繁布局
3. **用户体验**: 保持原有的交互逻辑，只修复显示问题
4. **测试覆盖**: 覆盖各种使用场景和边界情况

## 后续改进建议

1. **动画优化**: 可以为布局的显示/隐藏添加平滑动画
2. **触摸处理**: 优化下拉列表外的触摸事件处理
3. **键盘管理**: 改进软键盘与下拉列表的协同工作
4. **主题适配**: 确保在不同主题下显示效果良好
