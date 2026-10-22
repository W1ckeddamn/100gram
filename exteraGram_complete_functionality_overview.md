# ExteraGram Complete Functionality Overview

This document provides a comprehensive overview of all functionality discovered in exteraGram based on extensive code analysis. It synthesizes findings from multiple deep dives into the codebase.

## 📋 Table of Contents
1. [Configuration System](#-configuration-system)
2. [UI & Animation Enhancements](#-ui--animation-enhancements)
3. [Standard Class Modifications](#-standard-class-modifications)
4. [Plugins System](#-plugins-system)
5. [Feed (Лента) Integration](#-feed-интеграция)
6. [AI Features](#-ai-features)
7. [Preference Screens & Settings](#-preference-screens--settings)
8. [Performance & Safety Features](#-performance--safety-features)
9. [Related Documentation](#-related-documentation)

---

## ⚙️ Configuration System

ExteraGram features a sophisticated configuration system that controls all custom features:

### Core Configuration Files
- **ExteraConfig.java** (D:\extragram\ExteraConfig.java) - Main configuration hub (2,201 lines)
  - Manages all exteraGram-specific settings via SharedPreferences
  - Uses delegate pattern: `PrefsDelegate` for different types (boolean, int, string, enum)
  - Features migration functions for backward compatibility
  - Controls: transitions, glass effects, gooey animations, plugins, AI features, etc.

- **AiConfig.java** (D:\extragram\sources_exteragram\com\exteragram\messenger\ai\AiConfig.java) - AI-related configuration (416 lines)
  - Manages AI assistant features and settings
  - Controls AI model selection, behavior, and integration points

### Configuration Architecture
- **BooleanPrefs**: For toggles (e.g., gooey avatar animation, quick transitions)
- **EnumPrefs**: For multi-option settings (e.g., TransitionAnimation, GlassOutlineStyle)
- **IntegerPrefs**: For numeric values (e.g., sticker size, avatar corner radius)
- **StringPrefs**: For text values
- **Migration System**: Functions like `migrateTransitionAnimation()` ensure settings persist across updates

### Key Configuration Categories
1. **Appearance**: Glass effects, gooey animations, MD3 customization
2. **Chats**: Quick transitions, sticker animations, swipe actions
3. **Plugins**: Engine toggle, individual plugin controls
4. **AI**: Assistant features, model selection
5. **Advanced**: Performance options, experimental features

---

## 🎨 UI & Animation Enhancements

ExteraGram significantly enhances the visual experience beyond stock Telegram:

### Animation System
- **Transition Animations**: Three options via `TransitionAnimation.java` enum
  - `DEFAULT`: Standard Telegram transitions
  - `AOSP`: Android Open Project style
  - `SPRING`: Physics-based spring animations (enhanced)
- **Window Transitions**: Enhanced via `WindowTransitionAnimationHelper.java`
  - Custom timing curves using `CubicBezierInterpolator.Emphasized`
  - Dynamic duration based on system animation scale
  - Window edge extensions for smooth cross-activity transitions
- **Predictive Back**: `PredictiveBackAnimationHelper.java` for iOS-style back gestures
  - Shows destination screen during swipe-back
  - Spring-based cancellation when gesture incomplete

### Visual Effects
- **Glassmorphism**: Advanced frosted glass effects
  - `GlassOutlineStyle.java`: GLARE/SOLID/HIDDEN options
  - `GlassTabView`: Glass effect on tab bars with spring animations
  - `LiquidGlassEffect`: Shader-based real-time glass distortion
  - Applied to backgrounds, overlays, menu interfaces
- **Gooey Avatar Animations**: Fluid, elastic avatar transitions
  - `ProfileGooeyView`: CPU/GPU implementations with physics-based motion
  - Controlled by `ExteraConfig.getGooeyAvatarAnimation()`
- **Math-Based Reveals**: `MathRevealAnimation.java` for custom reveal effects
  - Sophisticated material-reveal style animations
  - Custom mathematical functions for unique patterns

### UI Customization Controls
Exposed through preference screens:
- **From AppearancePreferencesActivity**:
  - Gooey avatar animation toggle
  - Glass outline style selector (GLARE/SOLID/HIDDEN)
  - Glass message menu effect toggle
  - MD3 switch group with granular controls:
    - New loading style
    - New slider style
    - New switch style
    - New chat header style
    - New navigation bar style
    - New FAB style
- **From ChatsPreferencesActivity**:
  - Quick transition toggles for channels/topics
  - Sticker size animation controls
  - Various interface animation switches

---

## 🔧 Standard Class Modifications

ExteraGram modifies hundreds of standard Telegram (org.telegram.*) classes through hooking:

### Scale of Modifications
- **968+ hooked classes** identified in `hooked_all_classes.txt`
- Modifications range from minor additions to complete reimplementations
- Changes span UI components, animation systems, utilities, and core functionality

### Key Modified Components

#### UI Components & Views
1. **ProfileGooeyView** (`org.telegram.ui.Components.ProfileGooeyView`)
   - Complete reimplementation of avatar rendering
   - Adds gooey/fluid avatar animations with elastic physics
   - Dual CPU/GPU implementations for performance options

2. **GlassTabView** (`org.telegram.ui.Components.glass.GlassTabView`)
   - Custom glass-effect tab implementation
   - Uses GlassOutlineStyle (GLARE/SOLID/HIDDEN)
   - Spring animations for tab indicators
   - Efficient RenderNode-based glass rendering

3. **LiquidGlassEffect** (`org.telegram.ui.Components.blur3.LiquidGlassEffect`)
   - Shader-based glass effect using RenderEffect and RuntimeShader
   - Configurable intensity, resolution, and color properties
   - Applied to backgrounds and overlays

4. **ChatMessageCell** (`org.telegram.ui.Cells.ChatMessageCell`)
   - Extensive modifications to message rendering
   - Added quick transition support (channels/topics)
   - Custom animation handling and transition parameters
   - Enhanced sticker size animation controls

#### Animation & Transition Classes
1. **TransitionExt** (`org.telegram.ui.Components.TransitionExt`)
   - Extended transition functionality with spring physics
   - Used for predictive back animations and window transitions

2. **VoiceMessageEnterTransition** (`org.telegram.ui.VoiceMessageEnterTransition`)
   - Custom voice message entry animations with spring effects

3. **EmojiAnimationsOverlay** (`org.telegram.ui.EmojiAnimationsOverlay`)
   - Modified emoji burst animations with enhanced overlays

#### Utility Classes
1. **AndroidUtilities** (`org.telegram.messenger.AndroidUtilities`)
   - Significant additions for glass effects and animations
   - Extended utility methods for theme calculations

2. **ApplicationLoader** (`org.telegram.messenger.ApplicationLoader`)
   - Extended initialization for exteraGram features
   - Modified to support hooked class loading

3. **BuildVars** (`org.telegram.messenger.BuildVars`)
   - Modified build variables with exteraGram-specific flags

### Modification Patterns
- **Extension over Replacement**: Most modifications extend existing functionality
- **Configuration-Driven**: Features toggled via ExteraConfig settings
- **Performance Conscious**: Heavy effects use efficient rendering (RenderNode, shaders)
- **Backward Compatibility**: Standard Telegram interface maintained when features disabled
- **Layered Approach**: Visual effects often layered (e.g., glass over standard UI)

---

## 🔌 Plugins System

ExteraGram features a robust plugin system for extending functionality:

### Core Components
1. **PluginsController.java** - Central plugin management
   - Handles plugin loading, enabling/disabling, lifecycle
   - Manages plugin settings and hooks
   - Provides interfaces for plugin integration with Telegram core

2. **PluginsConstants.java** - Plugin-related constants
   - Events: APP_START, APP_STOP, APP_PAUSE, APP_RESUME
   - Hooks: SEND_MESSAGE_HOOK, ON_PLUGIN_LOAD, ON_PLUGIN_UNLOAD
   - Settings structure: Types (switch, input, selector, header, divider, custom, text)
   - Strategies: DEFAULT, MODIFY, MODIFY_FINAL, CANCEL
   - Menu item types for plugin integration

3. **PluginsActivity.java** - Main plugin management UI
   - Toggle for enabling/disabling entire plugin engine
   - List of installed plugins with individual controls
   - Search functionality and visual status indicators

### Plugin Features
- **Settings System**: Plugins define their own settings via SettingItem model
  - Supported types: Switch, Input, Selector, Header, Divider, Custom, Text
  - Persisted via plugin-specific SharedPreferences
- **Hook System**: Plugins can hook into Telegram functionality
  - Event hooks (APP_START, APP_STOP, etc.)
  - Message hooks (SEND_MESSAGE_HOOK for intercepting/modifying messages)
  - Request/response hooks (for network requests)
  - Update hooks (for processing Telegram updates)
  - Menu item hooks (adding items to chat action, drawer, main menus, etc.)
- **Safety Features**: 
  - Safe mode activation on plugin crashes
  - Crash detection and reporting (NativeCrashHandler)
  - Ability to disable problematic plugins
  - Isolation of plugin errors from core application
- **Performance**: 
  - Plugin engine can be completely disabled
  - Hook caching minimizes runtime cost
  - Dedicated dispatcher queue for plugin execution
  - Watchdog monitors for unresponsive plugins

### Plugin Feed Integration
The plugin system is tightly integrated with the feed (лента) functionality:
- Plugins can contribute to or modify the feed through various hooks
- Feed-related settings accessible through standard preferences
- Plugins can add feed-related settings via the settings system
- Suggested plugin types for feed: enhancement, source, processing, export/import plugins

---

## 📰 Feed (Лента) Integration

The feed feature appears to be both a core feature and a primary extension point for plugins:

### Feed-Related Resources
- **String Resources**: Extensive feed support in strings.xml and locale files
  - FeedShownChannels, FeedMarkAllReadConfirm, FeedIncludeArchivedInfo
  - FeedChannelsInfo, FeedChannelHidden, FeedBottomTabInfo
  - FeedAllChannelsExcluded, FeedSettings
- **UI Components**: 
  - FeedChannelsActivity.java - Dedicated activity for managing feed channels
  - Feed widget layouts (feed_widget_item.xml, feed_widget_layout.xml)
  - Feed icons (ic_feed, ic_feed_filled, etc.)

### Feed Management Capabilities
From the preferences system, users can:
- Manage which channels appear in feed (likely via FeedChannelsActivity)
- Include/exclude archived chats from feed
- Show feed as bottom tab replacing Contacts tab
- Mark all feed posts as read when needed
- Configure feed-related preferences through standard settings screens

### Plugin-Feed Connection
Evidence suggests plugins can interact with the feed:
- **Menu Item Types**: Plugins can add items to various menus that may be feed-related
- **Event Hooks**: Plugins could listen to app lifecycle events to update feed
- **Message Hooks**: Could hook into message sending/receiving to add content to feed
- **Settings Integration**: Plugins could add their own feed-related settings
- **Direct API Access**: Potential access to feed APIs if exposed

---

## 🤖 AI Features

ExteraGram includes AI assistant integration:

### AI Configuration
- **AiConfig.java**: Manages AI assistant features and settings
- Controls AI model selection, behavior, and integration points
- Likely integrates with external AI services for chat assistance

### AI-Related Preferences
- Found in AiPreferencesActivity.java (referenced in SettingsRegistry)
- Controls for AI assistant features, model selection, and behavior
- Integration with chat interface for AI-powered assistance

### AI Functionality
While specific AI features weren't deeply analyzed in this session, the presence of:
- Dedicated AiConfig.java
- AiPreferencesActivity in SettingsRegistry
- References in hook files and preferences
- Suggests comprehensive AI assistant capabilities comparable to other Telegram forks with AI features

---

## ⚙️ Preference Screens & Settings

ExteraGram exposes extensive customization through its preference system:

### Main Preference Activities
1. **MainPreferencesActivity.java** - Central hub
   - Categories: General, Chats, Appearance, Notifications, etc.
   - Plugins category opening PluginsActivity
   - Appearance category opening AppearancePreferencesActivity
   - Chats category opening ChatsPreferencesActivity

2. **AppearancePreferencesActivity.java** - Visual customization
   - Gooey avatar animation toggle
   - Glass outline style selector
   - Glass message menu effect toggle
   - MD3 customization group (loading, slider, switch, header, nav bar, FAB)

3. **ChatsPreferencesActivity.java** - Chat-specific settings
   - Quick transition toggles for channels/topics
   - Sticker size animation controls
   - Swipe actions configuration
   - Various interface animation switches

4. **GeneralPreferencesActivity.java** - General settings
   - Basic app configuration options
   - Plugin engine toggle (in some versions)

5. **Other Specialized Activities**:
   - FeedChannelsActivity.java - Feed channel management
   - AiPreferencesActivity.ai - AI assistant settings
   - PillStackPreferencesActivity - Media vault/PillStack features
   - AppNavigationPreferencesActivity - Navigation customization
   - SwipeActionsPreferencesActivity - Gesture customization

### Settings Registry System
- **SettingsRegistry.java**: Maps activities to icons and categories
- Ensures consistent organization across preference screens
- Registers all preference activities with appropriate icons and ordering

---

## ⚡ Performance & Safety Features

Despite adding extensive features, exteraGram maintains performance and safety:

### Performance Optimizations
- **Efficient Rendering**: Glass effects use RenderNode rather than heavy overlays
- **Resource Cleanup**: Proper disposal of animation resources and render nodes
- **Frame-Rate Conscious**: Animations respect system scales and device capabilities
- **Selective Application**: Effects applied only where visually beneficial
- **Plugin Engine Toggle**: Complete disable to avoid overhead when not used
- **Hook Caching**: Minimizes runtime cost of plugin hook execution
- **Dedicated Queues**: Plugins run on dispatcher queue to avoid UI blocking

### Safety Features
- **Configuration Migration**: Ensures settings persist across updates without loss
- **Feature Toggles**: All major features user-controllable via ExteraConfig
- **Plugin Safety**: 
  - Safe mode activation on plugin crashes
  - Crash detection and reporting
  - Ability to disable problematic plugins
  - Isolation of plugin errors from core application
- **Performance Monitoring**: Watchdog monitors for unresponsive plugins
- **Fallback Mechanisms**: Graceful degradation when features unsupported

### Backward Compatibility
- When features disabled, exteraGram behaves nearly identically to stock Telegram
- Migration functions preserve user settings across version updates
- Modifications designed to not break core Telegram functionality

---

## 📚 Related Documentation

For deeper dives into specific areas, see these documents:

1. **exteraGram_settings_inventory.md** - Complete inventory of all settings, toggles, and hidden features
2. **exteraGram_interface_animations_improvements.md** - Detailed analysis of UI and animation improvements vs. stock Telegram
3. **exteraGram_standard_class_modifications.md** - Analysis of modifications to standard Telegram (org.telegram.*) classes
4. **exteraGram_plugins_and_feed.md** - Documentation of plugins system and feed (лента) integration
5. **AGENTS.md** - Setup instructions, build commands, and environment configuration for 100gram

---

## 🎯 Summary

ExteraGram represents a comprehensive enhancement of the standard Telegram client featuring:

1. **Deep Customization**: Hundreds of user-controllable settings across appearance, animations, plugins, and AI
2. **Advanced Visual Effects**: Glassmorphism, gooey animations, spring transitions, and predictive back gestures
3. **Robust Plugin System**: Safe, performant extension mechanism with deep hooks into Telegram core
4. **Integrated Feed**:Лента functionality with potential for plugin enhancement
5. **AI Integration**: Assistant features for enhanced productivity
6. **Performance Conscious**: Despite rich features, maintains efficiency through smart implementation
7. **User-Centric Design**: Extensive preference system puts control in user's hands
8. **Backward Compatible**: Can be used as stock Telegram when all enhancements disabled

The modifications are systematic, well-organized, and provide significant enhancements while maintaining the core stability and familiarity of the Telegram experience.

---
*Document generated based on analysis of exteraGram source code in D:\extragram\ as of October 2026.*