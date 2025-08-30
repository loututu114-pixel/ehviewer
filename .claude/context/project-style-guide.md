---
created: 2025-08-30T04:26:53Z
last_updated: 2025-08-30T04:26:53Z
version: 1.0
author: Claude Code PM System
---

# Project Style Guide

## Code Conventions

### Java Code Style

#### Naming Conventions
- **Classes**: PascalCase (e.g., `MainActivity`, `BookmarkManager`)
- **Interfaces**: PascalCase with descriptive names (e.g., `OnClickListener`)
- **Methods**: camelCase (e.g., `loadBookmarks()`, `getUserData()`)
- **Variables**: camelCase (e.g., `bookmarkList`, `isLoading`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRIES`, `DEFAULT_TIMEOUT`)
- **Packages**: lowercase (e.g., `com.hippo.ehviewer.client`)

#### Class Organization
```java
public class ExampleActivity extends BaseActivity {
    // 1. Constants
    private static final String TAG = "ExampleActivity";
    
    // 2. Static variables
    private static int instanceCount = 0;
    
    // 3. Instance variables
    private BookmarkManager bookmarkManager;
    private boolean isLoading;
    
    // 4. Lifecycle methods (in lifecycle order)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    // 5. Override methods
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    
    // 6. Public methods
    public void loadData() {
        // Implementation
    }
    
    // 7. Private methods
    private void initializeViews() {
        // Implementation
    }
    
    // 8. Inner classes/interfaces
    private class DataLoader {
        // Implementation
    }
}
```

#### Method Guidelines
- Keep methods short (< 30 lines preferred)
- Single responsibility principle
- Descriptive names that explain what, not how
- Use early returns to reduce nesting

### XML Conventions

#### Layout Files
- **Naming**: `{type}_{description}.xml`
  - `activity_main.xml`
  - `fragment_gallery.xml`
  - `item_bookmark.xml`
  - `dialog_add_bookmark.xml`

#### Resource IDs
- **Format**: `{type}_{description}`
  - TextView: `tv_title`, `tv_description`
  - Button: `btn_submit`, `btn_cancel`
  - EditText: `et_search`, `et_username`
  - ImageView: `iv_thumbnail`, `iv_avatar`
  - RecyclerView: `rv_bookmarks`, `rv_history`
  - Layout: `layout_header`, `layout_footer`

#### Attribute Ordering
```xml
<TextView
    android:id="@+id/tv_title"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    android:text="@string/title"
    android:textColor="@color/text_primary"
    android:textSize="18sp"
    style="@style/TitleText" />
```

Order:
1. `android:id`
2. Layout dimensions (`width`, `height`)
3. Layout positioning
4. Margins and padding
5. Content attributes
6. Style attributes
7. Custom style

### Resource Files

#### Strings
- **File**: `strings.xml`
- **Format**: lowercase with underscores
- **Examples**:
  ```xml
  <string name="app_name">EhViewer</string>
  <string name="btn_save">Save</string>
  <string name="error_network">Network error occurred</string>
  <string name="dialog_title_confirm">Confirm Action</string>
  ```

#### Colors
- **File**: `colors.xml`
- **Format**: lowercase with underscores
- **Examples**:
  ```xml
  <color name="primary">#FF6750A4</color>
  <color name="text_primary">#212121</color>
  <color name="background_light">#FFFFFF</color>
  ```

#### Dimensions
- **File**: `dimens.xml`
- **Format**: lowercase with underscores
- **Examples**:
  ```xml
  <dimen name="margin_small">8dp</dimen>
  <dimen name="margin_medium">16dp</dimen>
  <dimen name="text_size_title">18sp</dimen>
  ```

### Kotlin Build Scripts

#### Gradle KTS Style
```kotlin
android {
    namespace = "com.hippo.ehviewer"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.hippo.ehviewer"
        minSdk = 23
        targetSdk = 29
    }
}

dependencies {
    implementation(libs.androidx.core)
    testImplementation(libs.junit)
}
```

## Documentation Standards

### Code Comments

#### Class Documentation
```java
/**
 * Manages bookmark operations including adding, removing, and retrieving bookmarks.
 * This class uses a singleton pattern to ensure consistent bookmark state across
 * the application.
 *
 * @author EhViewer Team
 * @since 1.9.9
 */
public class BookmarkManager {
    // Implementation
}
```

#### Method Documentation
```java
/**
 * Loads bookmarks from the database asynchronously.
 *
 * @param callback The callback to invoke when loading completes
 * @param limit Maximum number of bookmarks to load (0 for all)
 * @return true if loading started successfully, false otherwise
 */
public boolean loadBookmarks(LoadCallback callback, int limit) {
    // Implementation
}
```

### Inline Comments
- Use sparingly, code should be self-documenting
- Explain why, not what
- Keep comments up-to-date with code changes

## Git Conventions

### Branch Naming
- `main` - Main development branch
- `feature/bookmark-improvements`
- `bugfix/crash-on-startup`
- `hotfix/security-issue`

### Commit Messages
Chinese or English commit messages following pattern:
- Feature: `添加书签管理功能` or `Add bookmark management`
- Fix: `修复启动崩溃` or `Fix startup crash`
- Update: `更新依赖版本` or `Update dependencies`
- Refactor: `重构网络层` or `Refactor network layer`

## File Organization

### Package Structure
```
com.hippo.ehviewer/
├── client/          # Network and data clients
│   └── data/        # Data models
├── database/        # Database utilities
├── ui/              # UI components
│   ├── scene/       # Complex UI scenes
│   └── wifi/        # WiFi-related UI
└── widget/          # Custom widgets
```

### File Naming
- **Java Classes**: Match class name exactly
- **Layout Files**: Prefix with type (activity_, fragment_, item_, dialog_)
- **Drawable Files**: Descriptive names (ic_bookmark, bg_button, selector_tab)
- **Menu Files**: Feature-based naming (menu_bookmarks, menu_browser)

## Best Practices

### General Guidelines
1. **DRY (Don't Repeat Yourself)**: Extract common code
2. **KISS (Keep It Simple)**: Avoid over-engineering
3. **YAGNI (You Aren't Gonna Need It)**: Don't add unused features
4. **Single Responsibility**: One class, one purpose

### Android Specific
1. **Avoid Memory Leaks**: Properly handle context references
2. **Use ViewBinding**: Type-safe view references
3. **Handle Configuration Changes**: Save/restore state properly
4. **Optimize Layouts**: Minimize view hierarchy depth
5. **Use String Resources**: Never hardcode strings

### Performance
1. **Lazy Initialization**: Initialize only when needed
2. **Recycle Views**: Use RecyclerView for lists
3. **Async Operations**: Keep UI thread responsive
4. **Image Loading**: Use appropriate image sizes

### Security
1. **Input Validation**: Always validate user input
2. **Secure Storage**: Use Android Keystore for sensitive data
3. **Network Security**: Use HTTPS, validate certificates
4. **Permission Handling**: Request only necessary permissions

## Testing Conventions

### Test Naming
```java
@Test
public void loadBookmarks_withValidData_returnsBookmarkList() {
    // Test implementation
}

@Test
public void saveBookmark_withNullTitle_throwsException() {
    // Test implementation
}
```

Pattern: `methodName_condition_expectedResult`

### Test Organization
- Unit tests: `/src/test/java/`
- Instrumented tests: `/src/androidTest/java/`
- Test data: `/src/test/resources/`

## Localization Guidelines

### String Resources
- Always use string resources
- Provide context in string names
- Use placeholders for dynamic content
- Include quantity strings for plurals

### Language Support
Maintain translations for:
- Chinese (Simplified, Traditional, HK, TW)
- English
- Japanese, Korean
- Spanish, French, German, Thai

## Version Control

### Files to Ignore
- Build outputs (`/build`, `*.apk`)
- Local properties (`local.properties`)
- IDE files (`.idea/workspace.xml`)
- OS files (`.DS_Store`, `Thumbs.db`)

### Files to Track
- Source code
- Resource files
- Build configurations
- Documentation
- Scripts