---
created: 2025-08-30T04:26:53Z
last_updated: 2025-08-30T04:26:53Z
version: 1.0
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
│   │   │   │   │   ├── HistoryManager.java
│   │   │   │   │   └── X5WebViewManager.java
│   │   │   │   ├── ui/          # User interface components
│   │   │   │   │   ├── scene/   # App scenes/screens
│   │   │   │   │   ├── wifi/    # WiFi features
│   │   │   │   │   ├── MainActivity.java
│   │   │   │   │   ├── WebViewActivity.java
│   │   │   │   │   ├── BookmarksActivity.java
│   │   │   │   │   └── HistoryActivity.java
│   │   │   │   └── widget/      # Custom UI widgets
│   │   │   │       └── UnifiedWebView.java
│   │   │   └── database/        # Database utilities
│   │   ├── res/
│   │   │   ├── layout/          # XML layouts
│   │   │   ├── values/          # Strings, IDs, styles
│   │   │   ├── drawable/        # Images and drawables
│   │   │   └── menu/            # Menu resources
│   │   └── AndroidManifest.xml
│   └── test/                    # Unit tests
├── build.gradle.kts             # Module build configuration
└── google-services.json         # Firebase configuration
```

## Key Directories

### Client Layer (`client/`)
- **Purpose**: Handles data management and network operations
- **Components**:
  - `AdBlockManager`: Advertisement blocking logic
  - `BookmarkManager`: Bookmark storage and retrieval
  - `HistoryManager`: Browsing history tracking
  - `X5WebViewManager`: Advanced WebView management
  - `data/`: Data models (BookmarkInfo, HistoryInfo)

### UI Layer (`ui/`)
- **Purpose**: User interface components and activities
- **Organization**:
  - Root level: Core activities
  - `scene/`: Complex UI scenes (gallery, history)
  - `wifi/`: WiFi-related features (server, client)
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