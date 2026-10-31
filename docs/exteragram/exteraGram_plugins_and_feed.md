# ExteraGram Plugins System and Feed (Лента) Integration

This document details the plugins system in exteraGram and its integration with the feed (лента) feature, based on analysis of the source code.

## 🔌 Plugins System Overview

ExteraGram includes a comprehensive plugin system that allows extending functionality through external modules. The system is implemented in the `com.exteragram.messenger.plugins` package.

### Core Components

1. **PluginsController.java** - Central plugin management
   - Handles plugin loading, enabling/disabling, and lifecycle
   - Manages plugin settings and hooks
   - Provides interfaces for plugin integration with Telegram core

2. **PluginsConstants.java** - Defines plugin-related constants
   - Plugin events: `APP_START`, `APP_STOP`, `APP_PAUSE`, `APP_RESUME`
   - Hook types: `SEND_MESSAGE_HOOK`, `ON_PLUGIN_LOAD`, `ON_PLUGIN_UNLOAD`
   - Settings structure: `Settings` inner class with types (switch, input, selector, etc.)
   - Strategies: `DEFAULT`, `MODIFY`, `MODIFY_FINAL`, `CANCEL`
   - Menu item types for plugin integration

3. **PluginsActivity.java** - Main plugin management UI
   - Toggle for enabling/disabling the entire plugin engine
   - List of installed plugins with individual controls
   - Search functionality for plugins
   - Visual indicators for plugin status

### Plugin Configuration in Settings

Plugins are configured through multiple preference screens:

#### Main Preferences (MainPreferencesActivity.java)
- Plugins category in main settings
- Button opens PluginsActivity
- Controlled by `PluginsController.INSTANCE.isPluginEngineSupported()`

#### Plugin Engine Toggle (PluginsActivity.java)
- Master switch: "Enable Plugins Engine"
- Toggles via `ExteraConfig.getPluginsEngine()` and `ExteraConfig.setPluginsEngine()`
- Persists setting to SharedPreferences key "pluginsEngine"
- When disabled: shuts down plugin controller
- When enabled: initializes plugin controller

#### Individual Plugin Controls
Each plugin in the list shows:
- Plugin name and icon
- Enable/disable toggle per plugin
- Settings button (if plugin provides settings)
- Information about plugin status

### Plugin Settings System

Plugins can define their own settings through:
- **SettingItem model** (`com.exteragram.messenger.plugins.models.SettingItem`)
- Supported types:
  - `TYPE_SWITCH` - Boolean toggle
  - `TYPE_INPUT` - Text input
  - `TYPE_SELECTOR` - Dropdown selection
  - `TYPE_HEADER` - Section header
  - `TYPE_DIVIDER` - Visual separator
  - `TYPE_CUSTOM` - Custom view
  - `TYPE_TEXT` - Read-only text

Settings are persisted via:
- Plugin-specific SharedPreferences
- Key prefix: `PREF_PLUGIN_ENABLED_KEY_PREFIX = "plugin_enabled_"`
- Individual settings stored with plugin ID as scope

### Plugin Hooks and Integration

Plugins can hook into Telegram functionality through:
- **Event hooks**: `APP_START`, `APP_STOP`, `ON_APP_EVENT`, etc.
- **Message hooks**: `SEND_MESSAGE_HOOK` for intercepting/modifying messages
- **Request/response hooks**: For network requests
- **Update hooks**: For processing Telegram updates
- **Menu item hooks**: Adding items to various menus (chat action, drawer, main, etc.)

The hook system uses:
- `HookRecord` and `EventHookRecord` for defining hooks
- Filtering capabilities (argument equality, null checks, etc.)
- Priority-based execution
- Ability to modify, cancel, or finalize operations

## 📰 Feed (Лента) Integration

The feed feature in exteraGram appears to be closely integrated with the plugin system, suggesting that plugins can contribute to or modify the feed.

### Feed-Related Strings and UI

Analysis of resources shows extensive feed support:

#### Strings (res/values/strings.xml and locale files)
- `FeedShownChannels` - "Shown in feed"
- `FeedMarkAllReadConfirm` - Confirmation for marking all feed posts as read
- `FeedIncludeArchivedInfo` - Include archived chats in feed
- `FeedChannelsInfo` - Instructions for hiding/showing channels in feed
- `FeedChannelHidden` - Notification when channel hidden from feed
- `FeedBottomTabInfo` - Show feed as bottom navigation tab
- `FeedAllChannelsExcluded` - When all channels excluded from feed
- `FeedSettings` - Feed settings title

#### Feed UI Components
- `FeedChannelsActivity.java` - Dedicated activity for managing feed channels
- Feed widget layouts (`feed_widget_item.xml`, `feed_widget_layout.xml`)
- Feed icons (`ic_feed`, `ic_feed_filled`, etc.)

### Plugin-Feed Connection

Evidence suggests plugins can interact with the feed:

1. **Menu Item Types** (PluginsConstants.java)
   - Plugins can add items to various menus that might feed-related contexts
   - While not explicitly "feed_menu" defined, plugins could hook into:
     - `CHAT_ACTION_MENU`
     - `DRAWER_MENU` 
     - `MAIN_MENU`
     - `MESSAGE_CONTEXT_MENU`
     - `PROFILE_ACTION_MENU`

2. **Event Hooks** 
   - Plugins could listen to app lifecycle events (`APP_START`, `APP_RESUME`) to update feed
   - Could hook into message sending/receiving to add content to feed

3. **Settings Integration**
   - Feed settings likely accessible through standard preferences
   - Plugins could add their own feed-related settings via the settings system

4. **Direct API Access**
   - PluginsController provides access to plugin engines and settings
   - Plugins could potentially access feed APIs if exposed

### Feed Management

From the preferences system, users can manage feed through:
- **FeedChannelsActivity** - Likely manages which channels appear in feed
- Settings to include/exclude archived chats
- Option to show feed as bottom tab replacing Contacts tab
- Controls for marking all feed posts as read

## 🔧 Technical Implementation Details

### Plugin Loading and Storage

- Plugins stored in: `<app_files_dir>/plugins/` directory
- Plugin files have `.plugin` extension (PluginsConstants.PLUGINS_EXT)
- Metadata stored in SharedPreferences under "plugin_settings"
- Enabled plugins tracked with keys like `plugin_enabled_<plugin_id>`

### Plugin Lifecycle

1. **Loading**: 
   - PluginsController scans plugins directory
   - Loads plugin metadata and validates
   - Initializes plugin engines based on strategy

2. **Enable/Disable**:
   - Per-plugin toggles via `setPluginEnabled()`
   - Master toggle via `ExteraConfig.setPluginsEngine()`

3. **Settings**:
   - Plugins can provide settings UI via `openPluginSetting()`
   - Settings persisted through PluginsController settings map

### Hook Execution

- Plugins register hooks through the PluginsController
- Hooks cached for performance (`exactMatchEventHooksCache`, `substringMatchEventHooksCache`)
- Execution order determined by priority and registration time
- Ability to modify parameters or cancel operations

## 📱 User Experience

### Accessing Plugins
1. Open Settings → Plugins (in main settings)
2. Toggle "Enable Plugins Engine" to activate system
3. Browse list of installed plugins
4. Toggle individual plugins on/off
5. Access individual plugin settings via gear icon

### Managing Feed
1. Access through Settings → Feed (likely)
2. Or via dedicated FeedChannelsActivity
3. Toggle channels to show/hide in feed
4. Configure archived chats inclusion
5. Set feed as bottom tab if desired
6. Mark all feed posts as read when needed

## ⚡ Performance and Safety

### Performance Considerations
- Plugin engine can be completely disabled to avoid overhead
- Hook caching minimizes runtime cost
- Plugins run on dedicated dispatcher queue (`runOnPluginsQueue`)
- Watchdog monitors for unresponsive plugins (`PluginsWatchdog`)

### Safety Features
- Safe mode activation on plugin crashes
- Crash detection and reporting (`NativeCrashHandler`)
- Ability to disable problematic plugins
- Isolation of plugin errors from core application

## 🔄 Relationship Between Plugins and Feed

While the feed appears to be a core feature of exteraGram, the plugin system likely enables:
1. **Feed Enhancement Plugins** - Plugins that add content or modify feed presentation
2. **Feed Source Plugins** - Plugins that provide additional content sources for the feed
3. **Feed Processing Plugins** - Plugins that filter, sort, or transform feed items
4. **Feed Export/Import Plugins** - Plugins for backing up or migrating feed data

The tight integration suggested by shared constants, preference organization, and hook availability indicates that plugins are a primary mechanism for extending feed functionality in exteraGram.

## Conclusion

ExteraGram features a robust plugin system that:
- Provides deep integration with Telegram core through hooks
- Offers extensive customization via settings UI
- Enables feed enhancement and modification capabilities
- Maintains performance through intelligent caching and queuing
- Ensures safety with crash detection and safe mode recovery

The feed (лента) functionality appears to be both a core feature and a primary extension point for plugins, allowing users to customize their feed experience through third-party modules while maintaining the stability and performance of the core application.