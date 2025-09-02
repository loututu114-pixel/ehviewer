---
created: 2025-08-30T04:26:53Z
last_updated: 2025-09-01T13:06:42Z
version: 1.3
author: Claude Code PM System
---

# Product Context

## Product Overview
**EhViewer** - A comprehensive Android application ecosystem that combines gallery browsing, full-featured web browsing, novel reading, and advanced content management. Features modular architecture, extensive notification systems, and enterprise-level browser capabilities with error recovery and performance optimization.

## Target Users

### Primary Users
- **Content Browsers**: Users who frequently browse online galleries and novels
- **Privacy-Conscious Users**: Those who value ad-free browsing with advanced security
- **Power Users**: Users who need enterprise-level browser features and performance
- **Multi-Language Users**: Especially Chinese, Japanese, and Korean speakers
- **Novel Readers**: Users seeking advanced text reading experiences
- **Performance Enthusiasts**: Users requiring optimized, modular applications

### User Personas
1. **Gallery Enthusiast**
   - Needs: Fast browsing, bookmark management, history tracking
   - Pain Points: Ads, slow loading, lost browsing sessions

2. **Casual Browser**
   - Needs: Simple interface, easy navigation
   - Pain Points: Complex features, confusing UI

3. **Privacy Advocate**
   - Needs: Ad blocking, secure browsing
   - Pain Points: Tracking, advertisements, data collection

## Core Features

### Gallery Viewing
- **Gallery Detail View**: Rich media display with optimized loading
- **Scene-Based Navigation**: Smooth transitions between galleries
- **Multiple View Modes**: Different layouts for various content types

### Advanced Web Browser
- **Enterprise-Level Browser Core**: Enhanced browser with error recovery
- **BrowserCoreManager**: Advanced browser engine management
- **ErrorRecoveryManager**: Intelligent error handling and recovery
- **Cache Management**: Advanced offline caching system
- **Performance Monitoring**: Real-time performance tracking
- **Preload Management**: Intelligent content preloading
- **Render Engine**: Enhanced rendering capabilities
- **Security Manager**: Comprehensive security features

### Novel Library System (NEW)
- **Complete Reading Platform**: NovelLibraryManager with content extraction
- **Advanced Text Processing**: Content purification and formatting
- **Reading Interface**: NovelLibraryActivity and NovelReaderActivity
- **Text Optimization**: Enhanced reading experience with smart formatting
  - Fullscreen and rotation support
  - Volume control integration
- **Multi-Tab Management**: Browser-style tab switching and management
- **Enhanced WebView Manager**: Extensions, developer tools, browser controls
- **Tencent X5 Engine**: Advanced rendering with X5 browser SDK
- **WebView Pool**: Instance recycling for performance
- **JavaScript Optimizer**: Enhanced JS execution speed
- **Cache Management**: Smart caching strategies
- **Reading Mode**: Optimized content display
- **Download Manager**: Integrated download handling
- **Error Handling**: Graceful error recovery with user-friendly messages

### Password & Security Management
- **Password Manager**: Complete credential management system
  - Encrypted storage with AES encryption
  - Biometric authentication (fingerprint/face)
  - Password generation with customizable rules
  - Import/export functionality
  - System-level autofill service
- **Multiple Password UI Components**:
  - Password list with search
  - Add/edit password dialogs
  - Password generator dialog
  - Password details viewer
  - Settings for password management

### Enhanced Data Management
- **Cloud Sync Manager**: Cross-device synchronization
- **Advanced Bookmarks**: 
  - Cloud sync support
  - Categories and tags
  - Import/export
  - Search functionality
  
- **Enhanced History**:
  - Full-text search
  - Visit frequency tracking
  - Grouped by date
  - Privacy mode

### Media & File Handling
- **Media Player**: Built-in media playback
- **File Viewer**: Document and file viewing
- **Universal File Opener**: Smart file type handling
- **File Settings**: Custom file type associations

### Privacy & Security
- **Advanced Ad Blocking**: Multi-layer ad filtering
- **No Mandatory Analytics**: Optional Firebase integration
- **Encrypted Storage**: Secure credential storage
- **Biometric Auth**: Fingerprint/face unlock
- **Privacy Mode**: Incognito browsing

### System Services & Background Operations (NEW)
- **AppKeepAliveService**: Background application optimization
- **NotificationManager**: Advanced notification system with rich features
- **PushMessageService**: Comprehensive push notification handling
- **SmsCodeExtractorService**: Intelligent SMS code extraction
- **SystemMonitor**: Real-time system performance monitoring
- **TaskTriggerService**: Advanced task scheduling and execution
- **WifiMonitorService**: WiFi connectivity monitoring and optimization

### Modular Architecture (NEW)
- **Pluggable Module System**: 13 independent feature modules
- **Standardized Interfaces**: Consistent APIs across all modules
- **Easy Integration**: Example implementations for quick setup
- **Core Modules Available**:
  - Database operations with migration support
  - Advanced networking with enhanced connectivity
  - Security management with comprehensive protection
  - UI enhancements and modern components
  - Performance monitoring and optimization
  - Memory management and crash reporting
  - Analytics, bookmarks, passwords, and more

### Enhanced Activities & UI (NEW)
- **CacheManagementActivity**: Comprehensive cache administration
- **DeepLinkHandlerActivity**: Advanced deep link processing
- **EnhancedImageViewerActivity**: Feature-rich image viewing
- **GalleryHistoryActivity**: Gallery viewing history management
- **ModernBrowserActivity**: Contemporary browser interface
- **PrivateModeSettingsActivity**: Privacy mode configuration
- **TabsManagerActivity**: Advanced tab management system

### Connectivity Features
- **WiFi Transfer**:
  - Server mode for sharing
  - Client mode for receiving
  - Direct device-to-device transfer

### Customization
- **Multiple Themes**: 5 different UI themes available
  - Standard
  - Modern
  - Refined
  - Cartoon
  - Cartoon with round buttons
  
- **Multi-Language Support**: 8 languages
  - Chinese (Multiple variants)
  - English
  - Japanese, Korean
  - Spanish, French, German, Thai

## User Requirements

### Functional Requirements
1. **Fast Loading**: Quick gallery and image loading with lazy loading
2. **Offline Access**: Cache management for offline viewing
3. **Search Capability**: Multi-engine search support
4. **Advanced Bookmarks**: Cloud sync, categories, import/export
5. **Enhanced History**: Full-text search, frequency tracking
6. **Ad-Free Experience**: Multi-layer ad blocking
7. **Password Management**: Secure storage with autofill
8. **Tab Browsing**: Multi-tab with session persistence
9. **Media Playback**: Built-in media player
10. **File Handling**: Smart file type associations

### Non-Functional Requirements
1. **Performance**: Smooth scrolling and transitions
2. **Reliability**: Stable operation without crashes
3. **Compatibility**: Android 6.0+ support
4. **Localization**: Native language support
5. **Privacy**: No unauthorized data collection

## Use Cases

### Primary Use Cases
1. **Browse Galleries**
   - Open app → Navigate to gallery → View content
   - Expected: Fast loading, smooth navigation

2. **Save Bookmarks**
   - Find interesting content → Add bookmark → Access later
   - Expected: Quick save, easy retrieval

3. **Review History**
   - Open history → Find previous session → Resume browsing
   - Expected: Complete history, quick access

4. **Block Advertisements**
   - Browse content → Ads automatically blocked
   - Expected: Clean, ad-free experience

5. **Share via WiFi**
   - Select content → Start WiFi server → Other device connects
   - Expected: Fast transfer, reliable connection

### Secondary Use Cases
1. **Switch Themes**
   - Settings → Select theme → Apply
   - Expected: Instant theme change

2. **Change Language**
   - Settings → Select language → Restart app
   - Expected: Full translation

3. **Manage Black List**
   - Settings → Black list → Add/remove items
   - Expected: Content filtering

## Success Metrics

### User Engagement
- Daily active users
- Session duration
- Pages viewed per session
- Bookmark creation rate

### Performance Metrics
- App launch time < 2 seconds
- Page load time < 3 seconds
- Crash rate < 1%
- Memory usage optimization

### Feature Adoption
- Bookmark feature usage > 60%
- History feature usage > 40%
- Ad block effectiveness > 95%
- Theme customization > 30%

## Competitive Advantages
1. **Full Browser Integration**: Complete browser with tabs and extensions
2. **Advanced Password Manager**: Biometric auth and autofill
3. **Tencent X5 Engine**: Superior rendering performance
4. **WebView Pooling**: Resource optimization
5. **Cloud Sync**: Cross-device data synchronization
6. **No Ads**: Multi-layer ad blocking
7. **Privacy Focus**: Encrypted storage, optional analytics
8. **Multiple Themes**: 5 unique visual styles
9. **Performance Optimization**: Memory management, lazy loading
10. **Comprehensive Testing**: 4 dedicated browser test suites

## Recent Product Enhancements (v1.9.9.18)

### Bug Fixes & Quality Improvements
- **Browser History Issue Resolution**: 
  - Diagnosed and documented empty history display problem
  - Identified root cause: restrictive 30-record limit causing data loss
  - Solution path: Increase limit to 100+ records and improve filtering

- **Android System Error Mitigation**:
  - Comprehensive analysis of 6 major error categories
  - Implementation of LogMonitor for real-time error tracking
  - SystemErrorHandler for graceful degradation during system issues
  - RetryHandler with exponential backoff for transient failures

### Reliability & User Experience
- **Enhanced Error Handling**: Improved app stability during system-level issues
- **Better Diagnostics**: Added extensive logging for troubleshooting
- **Database Optimization**: Validated and optimized history/bookmark database schemas
- **Documentation**: Created comprehensive fix guides for common issues

### Technical Debt Reduction
- **Systematic Bug Investigation**: Deep analysis of user-reported issues
- **Error Pattern Recognition**: Automated detection of recurring problems
- **Proactive Monitoring**: Real-time system health monitoring
11. **Media Integration**: Built-in player and viewer
12. **Smart File Handling**: Universal file opener

## User Feedback Integration
- Regular updates based on user needs
- Theme variants created from user requests
- Performance optimizations from usage patterns
- Feature additions based on community feedback