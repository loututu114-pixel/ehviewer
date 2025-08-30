---
created: 2025-08-30T04:26:53Z
last_updated: 2025-08-30T04:26:53Z
version: 1.0
author: Claude Code PM System
---

# System Patterns

## Architectural Style
- **Pattern**: Modified MVC with Activity-based architecture
- **Separation**: Clear distinction between UI, business logic, and data layers
- **Component Communication**: Intent-based navigation and callbacks

## Design Patterns Observed

### Manager Pattern
Centralized management classes for core functionality:
- `AdBlockManager`: Singleton-style ad blocking service
- `BookmarkManager`: Centralized bookmark operations
- `HistoryManager`: History tracking and retrieval
- `X5WebViewManager`: WebView lifecycle and configuration

### Data Access Pattern
- **SQLite Helpers**: Custom `MSQLiteOpenHelper` and `MSQLiteBuilder`
- **Data Models**: Separate info classes (BookmarkInfo, HistoryInfo)
- **Abstraction**: Database operations abstracted from UI layer

### Activity Inheritance Hierarchy
```
EhActivity (Base)
    ├── ToolbarActivity
    ├── MainActivity
    ├── WebViewActivity
    ├── BookmarksActivity
    ├── HistoryActivity
    └── Other specialized activities
```

### Scene-Based Navigation
- **Scene Pattern**: Complex UI flows organized as scenes
- **Gallery Detail Scene**: Modular view components
- **State Management**: Scene-based state preservation

## Component Organization

### Layered Architecture
1. **Presentation Layer** (`ui/`)
   - Activities and UI components
   - Custom widgets
   - Layout inflation and event handling

2. **Business Logic Layer** (`client/`)
   - Managers for core features
   - Business rules and validation
   - Data transformation

3. **Data Layer** (`database/`, `client/data/`)
   - Database helpers
   - Data models
   - Persistence operations

### Module Boundaries
- **WiFi Module**: Separate server/client activities
- **Gallery Module**: Scene-based gallery viewing
- **Browser Module**: WebView and related browsing features
- **Data Management**: Bookmarks and history subsystems

## Communication Patterns

### Event Handling
- **Activity Lifecycle**: Standard Android lifecycle management
- **Intent-Based**: Activity communication via Intents
- **Callback Interfaces**: For asynchronous operations

### Data Flow
```
User Input → Activity → Manager → Database/Network
                ↓          ↓            ↓
            UI Update ← Process ← Data Response
```

### Resource Management
- **Lazy Initialization**: Resources loaded on demand
- **Singleton Managers**: Shared instances for global features
- **Context Passing**: Activities provide context to managers

## WebView Integration Pattern
- **Unified WebView**: Custom widget for consistent behavior
- **X5 Integration**: Enhanced WebView with Tencent X5 core
- **Feature Injection**: Ad blocking and custom JavaScript

## State Management
- **Activity State**: SavedInstanceState for configuration changes
- **Persistent State**: Database for user data
- **Shared Preferences**: App settings and configurations

## Security Patterns
- **Permission Handling**: Runtime permissions for sensitive features
- **Data Validation**: Input validation in managers
- **Secure Storage**: Sensitive data in internal storage

## UI Patterns

### Custom Widget Development
- **UnifiedWebView**: Encapsulated WebView functionality
- **Reusable Components**: Common UI elements as widgets
- **Style Inheritance**: Consistent theming through styles

### Layout Strategy
- **XML-Based**: Declarative layouts in XML
- **ViewBinding**: Type-safe view references (implied)
- **Resource Organization**: Logical grouping of layouts

## Navigation Patterns
- **Activity Stack**: Standard back stack navigation
- **Tab Navigation**: Tab-based browsing interface
- **Dialog Flows**: Modal dialogs for user interactions

## Data Persistence Strategy
- **SQLite**: Primary data storage
- **File System**: APK variants and downloads
- **Memory Cache**: Temporary data in memory

## Build Patterns
- **Product Flavors**: Multiple build variants
- **Conditional Compilation**: Optional Firebase integration
- **Resource Filtering**: Optimized APK size

## Testing Patterns
- **Test Namespace**: Separate debug namespace
- **Unit Testing**: Tests with Android resources
- **Modular Testing**: Component-level test organization

## Error Handling
- **Try-Catch Blocks**: Exception handling in managers
- **Graceful Degradation**: Fallback for missing features
- **User Feedback**: Toast messages and dialogs

## Performance Patterns
- **Lazy Loading**: On-demand resource loading
- **MultiDex**: Method count optimization
- **Native Code**: Performance-critical operations in C++

## Localization Pattern
- **Resource Bundles**: Locale-specific strings
- **Locale Filtering**: APK size optimization
- **Default Fallback**: English as default language