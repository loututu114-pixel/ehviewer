---
created: 2025-08-30T04:26:53Z
last_updated: 2025-08-30T16:08:18Z
version: 1.1
author: Claude Code PM System
---

# Technology Context

## Platform & SDK Configuration
- **Platform**: Android
- **Compile SDK**: 35 (Latest Android 15)
- **Target SDK**: 29 (Android 10)
- **Minimum SDK**: 23 (Android 6.0 Marshmallow)
- **Build Tools**: Gradle with Kotlin DSL

## Core Technologies

### Programming Languages
- **Java**: Primary language for application logic
- **Kotlin**: Used for build scripts (Gradle KTS)
- **XML**: Layouts and resource definitions
- **Native Code Support**: C++ via CMake

### Build System
- **Gradle Version**: Modern with Kotlin DSL
- **Android Gradle Plugin**: Latest compatible version
- **Build Variants**: 
  - Flavor Dimension: "distribute"
  - Product Flavor: "appRelease"

## Dependencies & Libraries

### Core Android Components
- AndroidX Support Libraries
- Vector Drawables Support
- MultiDex Support (enabled)
- androidx.webkit:webkit:1.13.0

### Kotlin Extensions
- Kotlin Android Plugin
- Kotlin Parcelize Plugin

### Optional Services
- **Firebase** (when google-services.json present):
  - Google Services Plugin
  - Firebase Crashlytics
  - Analytics support infrastructure

### Native Development
- **NDK Configuration**:
  - Supported ABIs: armeabi-v7a, arm64-v8a, x86, x86_64
  - CMake for native builds
  - C++ support configured

### Third-Party Components
- **Tencent X5 Browser SDK** (v44286): Advanced browser engine
- **X5 WebView**: Enhanced WebView capabilities
- **Ad Blocking**: Custom implementation
- **License Plugin**: Dependency license management

### Testing Dependencies
- JUnit for unit testing
- AndroidX Test libraries
- Espresso for UI testing
- Custom browser test suites (Compatibility, Monkey, Performance, Stability)

## Development Tools

### IDE & Environment
- **Primary IDE**: Android Studio
- **Version Control**: Git
- **Shell Scripts**: 
  - Setup and configuration automation
  - Firebase status checking
  - GitHub repository setup
  - Analytics configuration updates

### Code Quality
- **Lint Configuration**:
  - Disabled: MissingTranslation
  - Abort on Error: true
- **Testing Framework**:
  - Unit Tests with Android Resources support
  - Test namespace: com.hippo.ehviewer.debug

## Localization Support
Supported locales:
- Chinese (zh, zh-rCN, zh-rHK, zh-rTW)
- English (default)
- Spanish (es)
- Japanese (ja)
- Korean (ko)
- French (fr)
- German (de)
- Thai (th)

## Application Configuration
- **Package Name**: com.hippo.ehviewer
- **Version Name**: 1.9.9.17
- **Version Code**: 111
- **Application ID**: com.hippo.ehviewer

## Resource Management
- **Locale Filtering**: Optimized APK size by filtering locales
- **Vector Graphics**: Using support library for compatibility
- **Resource Organization**: Standard Android structure

## Security & Privacy
- **Target SDK 29**: Avoids scoped storage restrictions
- **Optional Analytics**: Firebase integration is optional
- **Ad Blocking**: Built-in ad blocking capabilities

## Build Optimization
- **ProGuard/R8**: Configuration for release builds
- **MultiDex**: Enabled for method count optimization
- **Native Libraries**: Multiple architecture support

## External Integrations
- **WiFi Features**: Server and client capabilities
- **WebView Enhancement**: X5 WebView for better rendering
- **Data Persistence**: SQLite with custom helpers

## Development Scripts
Key automation scripts:
- `check_firebase_status.sh`: Firebase configuration validation
- `setup_github_repo.sh`: Repository initialization
- `update_analytics_config.sh`: Analytics setup
- `fix_switch_statements.sh`: Code maintenance
- `update_measurement_id.sh`: Analytics ID configuration

## Package Distribution
Multiple APK variants available:
1. Standard build
2. Modern theme variant
3. Refined theme variant
4. Cartoon style variant
5. Cartoon with round buttons variant

Each variant maintains the same version (1.9.9.17) with different UI themes.