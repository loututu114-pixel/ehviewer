---
created: 2025-08-30T04:26:53Z
last_updated: 2025-08-30T16:23:24Z
version: 1.2
author: Claude Code PM System
---

# Project Structure

## Root Directory Organization
```
EhViewerh/
├── app/                    # Main application module
├── gradle/                 # Gradle wrapper and configuration
├── .claude/               # Claude AI context and configuration
├── .idea/                 # Android Studio IDE configuration
├── install/               # Installation scripts and resources
├── release_package/       # Release build outputs
├── Ehviewer_CN_SXJ/      # Chinese localization resources
└── *.apk                  # Built APK variants (5 themed versions)
```

## Application Module Structure (`app/`)
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/hippo/
│   │   │   ├── ehviewer/         # Main application code
│   │   │   │   ├── client/      # Network and data clients
│   │   │   │   │   ├── data/    # Data models
│   │   │   │   │   ├── AdBlockManager.java
│   │   │   │   │   ├── BookmarkManager.java
│   │   │   │   │   ├── CloudSyncManager.java
│   │   │   │   │   ├── EnhancedWebViewManager.java
│   │   │   │   │   ├── HistoryManager.java
│   │   │   │   │   ├── ImageLazyLoader.java
│   │   │   │   │   ├── JavaScriptOptimizer.java
│   │   │   │   │   ├── MemoryManager.java
│   │   │   │   │   ├── NetworkDetector.java
│   │   │   │   │   ├── PasswordAutofillService.java
│   │   │   │   │   ├── PasswordManager.java
│   │   │   │   │   ├── ReadingModeManager.java
│   │   │   │   │   ├── SearchEngineManager.java
│   │   │   │   │   ├── WebViewCacheManager.java
│   │   │   │   │   ├── WebViewPoolManager.java
│   │   │   │   │   └── X5WebViewManager.java
│   │   │   │   ├── ui/          # User interface components
│   │   │   │   │   ├── scene/   # App scenes/screens
│   │   │   │   │   ├── wifi/    # WiFi features
│   │   │   │   │   ├── AddPasswordDialog.java
│   │   │   │   │   ├── BookmarksActivity.java
│   │   │   │   │   ├── BrowserLauncherActivity.java
│   │   │   │   │   ├── BrowserSettingsActivity.java
│   │   │   │   │   ├── BrowserTestActivity.java
│   │   │   │   │   ├── DownloadManagerActivity.java
│   │   │   │   │   ├── EhBrowserActivity.java
│   │   │   │   │   ├── FileOpenerSettingsActivity.java
│   │   │   │   │   ├── FileViewerActivity.java
│   │   │   │   │   ├── HistoryActivity.java
│   │   │   │   │   ├── MainActivity.java
│   │   │   │   │   ├── MediaPlayerActivity.java
│   │   │   │   │   ├── PasswordActionsDialog.java
│   │   │   │   │   ├── PasswordAdapter.java
│   │   │   │   │   ├── PasswordDetailsDialog.java
│   │   │   │   │   ├── PasswordGeneratorDialog.java
│   │   │   │   │   ├── PasswordManagerActivity.java
│   │   │   │   │   ├── PasswordSettingsActivity.java
│   │   │   │   │   ├── ReadingModeSettingsActivity.java
│   │   │   │   │   ├── UrlSuggestionAdapter.java
│   │   │   │   │   ├── VideoControlView.java
│   │   │   │   │   └── WebViewActivity.java (2000+ lines)
│   │   │   │   ├── util/        # Utility classes
│   │   │   │   │   ├── AppLauncher.java
│   │   │   │   │   ├── BrowserRegistrationManager.java
│   │   │   │   │   ├── DefaultBrowserHelper.java
│   │   │   │   │   ├── DomainSuggestionManager.java
│   │   │   │   │   ├── SmartUrlProcessor.java
│   │   │   │   │   ├── SmartUrlProcessorTest.java
│   │   │   │   │   ├── UniversalFileOpener.java
│   │   │   │   │   ├── UrlSuggestionTest.java
│   │   │   │   │   ├── VideoPlayerEnhancer.java
│   │   │   │   │   ├── WebViewErrorHandler.java
│   │   │   │   │   └── YouTubeCompatibilityManager.java
│   │   │   │   └── widget/      # Custom UI widgets
│   │   │   │       └── UnifiedWebView.java
│   │   │   └── database/        # Database utilities
│   │   ├── res/
│   │   │   ├── layout/          # XML layouts (40+ new layouts)
│   │   │   ├── values/          # Strings, IDs, styles
│   │   │   ├── drawable/        # Images and drawables (10+ new icons)
│   │   │   ├── menu/            # Menu resources
│   │   │   └── xml/             # XML configurations
│   │   └── AndroidManifest.xml
│   ├── test/                    # Unit tests
│   └── androidTest/             # Instrumented tests
│       └── java/com/hippo/ehviewer/
│           ├── BrowserCompatibilityTest.java
│           ├── BrowserMonkeyTest.java
│           ├── BrowserPerformanceTest.java
│           └── BrowserStabilityTest.java
├── build.gradle.kts             # Module build configuration
└── google-services.json         # Firebase configuration
```

## Key Directories

### Client Layer (`client/`)
- **Purpose**: Handles data management, network operations, and browser infrastructure
- **Core Managers**:
  - `EnhancedWebViewManager`: Advanced tab and extension management
  - `PasswordManager`: Secure credential storage with encryption
  - `CloudSyncManager`: Data synchronization across devices
  - `WebViewPoolManager`: WebView instance recycling
  - `WebViewCacheManager`: Cache optimization
  - `MemoryManager`: Memory usage optimization
  - `JavaScriptOptimizer`: JS performance enhancement
- **User Data**:
  - `BookmarkManager`: Bookmark storage with cloud sync
  - `HistoryManager`: Browsing history with search
  - `PasswordAutofillService`: System-level autofill
- **Performance**:
  - `ImageLazyLoader`: Efficient image loading
  - `NetworkDetector`: Connection monitoring
  - `SearchEngineManager`: Custom search providers
  - `ReadingModeManager`: Reading experience optimization
- **Data Models**: BookmarkInfo, HistoryInfo, PasswordInfo

### UI Layer (`ui/`)
- **Purpose**: User interface components and activities
- **Major Activities**:
  - `WebViewActivity`: Full-featured browser (2000+ lines)
  - `PasswordManagerActivity`: Credential management
  - `BrowserSettingsActivity`: Browser configuration
  - `DownloadManagerActivity`: Download management
  - `MediaPlayerActivity`: Media playback
- **Dialogs & Adapters**:
  - Password dialogs (Add, Details, Generator, Actions)
  - `PasswordAdapter`: List adapter for passwords
- **Organization**:
  - Root level: Core activities (20+ activities)
  - `scene/`: Complex UI scenes (gallery, downloads, etc.)
  - `wifi/`: WiFi-related features
  - `widget/`: Reusable UI components

### Resources (`res/`)
- **Layout Files**: Activity and fragment layouts
- **Drawables**: Tab backgrounds, icons
- **Values**: Strings (multi-language), IDs, dimensions
- **Menus**: Context and options menus

## Configuration Files

### Build Configuration
- `build.gradle.kts`: Kotlin DSL build script
- `settings.gradle.kts`: Project settings
- `gradle.properties`: Gradle properties
- `local.properties`: Local SDK paths

### Development Tools
- `.gitignore`: Git ignore patterns
- `gradlew`, `gradlew.bat`: Gradle wrapper scripts
- Various shell scripts for setup and updates

## APK Variants
The project produces multiple themed APK variants:
1. Standard version
2. Modern theme
3. Refined theme
4. Cartoon style
5. Cartoon with round buttons

## Module Dependencies
- Android Application Plugin
- Kotlin Android Plugin
- Kotlin Parcelize
- License Plugin
- Google Services (optional)
- Firebase Crashlytics (optional)

## File Naming Conventions
- **Java Classes**: PascalCase (e.g., `MainActivity.java`)
- **Layout Files**: snake_case with prefix (e.g., `activity_web_view.xml`)
- **Resource IDs**: snake_case (e.g., `browser_menu.xml`)
- **Drawable Resources**: snake_case (e.g., `tab_background.xml`)