# ExteraGram Settings Inventory

This document provides a comprehensive inventory of all settings, toggles, and features found in the decompiled `com.exteragram.messenger.*` package and core patches of exteraGram.

## Overview

ExteraGram features an extensive configuration system with 137+ user-controllable settings organized across multiple preference screens. Settings are implemented using a delegate pattern where each preference is backed by a `BasePref` implementation (BooleanPref, StringPref, IntegerPref, etc.) that handles persistence via SharedPreferences.

## Preference Screens

Settings are organized into the following preference activities:

1. **MainPreferencesActivity** - Top-level settings hub
2. **GeneralPreferencesActivity** - General interface and behavior settings
3. **AppearancePreferencesActivity** - Theme, visuals, and UI customization
4. **ChatsPreferencesActivity** - Chat-specific settings and swipe actions
5. **OtherPreferencesActivity** - Advanced, debug, and experimental options
6. **PillStackPreferencesActivity** - Quick access panel settings
7. **AiPreferencesActivity** - AI assistant configuration
8. **AppNavigationPreferencesActivity** - Bottom navigation bar settings
9. **IconPacksActivity** - Custom icon pack management
10. **AppIconsActivity** - Application icon customization

## Settings Categories

### General Settings
These settings control core interface behavior and functionality:

| Setting | Type | Description | Default |
|---------|------|-------------|---------|
| Translation Provider | Enum | Select AI translation service | 0 (likely default provider) |
| Translation Target Language | String | Target language for translations | System language |
| Do Not Translate Languages | String Set | Languages to exclude from translation | Empty set |
| Disable Number Rounding | Boolean | Show full numbers instead of abbreviated (1.23K → 1,234) | false |
| Format Time With Seconds | Boolean | Show seconds in time stamps | false |
| Relative Last Seen | Boolean | Show relative time (5m ago) vs absolute time | true |
| In App Vibration | Boolean | Enable vibration feedback for interactions | true |
| Disable Notification Delay | Boolean | Remove delay before showing notifications | false |
| Filter Zalgo | Boolean | Filter Zalgo text in messages | true |
| Upload Speed Boost | Boolean | Increase upload speed at cost of battery/data | false |
| Custom Save Path | String | Custom directory for saving media | Empty (default) |
| Hide Phone Number | Boolean | Hide phone number in profile | false |
| Show ID and DC | Enum | Display account info: Hide, Telegram API, Bot API | 0 (Hide) |
| Hide Archive Folder | Boolean | Hide archived chats folder | false |
| Archive On Pull | Boolean | Archive chats when pulling down chat list | false |
| Disable Unarchive Swipe | Boolean | Prevent swiping to unarchive chats | false |

### Appearance Settings
These settings control visual appearance and theming:

| Setting | Type | Description | Default |
|---------|------|-------------|---------|
| Single Corner Radius | Boolean | Use same corner radius for all UI elements | false |
| Action Bar Title | Enum | Title display in action bar: App Name, Username, Name, Filter Chats | 0 (App Name) |
| Pill Stack | Boolean | Enable quick access panel | true |
| Hide Stories | Boolean | Hide stories section from UI | false |
| Hide Action Bar Status | Boolean | Hide online status in action bar (Premium) | false |
| Center Title | Boolean | Center-align action bar title | false |
| Hide Floating Button | Boolean | Hide main floating action button | false |
| Hide Dialogs Search Bar | Boolean | Hide search bar in dialogs | false |
| Sender Mini Avatars | Boolean | Show small sender avatars in messages | true |
| Tab Title Style | Enum | Tab appearance: Text with Icons, Text Only, Icons Only | 0 (Text with Icons) |
| Tab Counter Mode | Enum | Tab counter behavior: All Chats, Unmuted Only, Blur Effect | 0 (All Chats) |
| Hide All Chats | Boolean | Hide chats list when filters applied | false |
| App Navigation Settings | Button | Configure bottom navigation bar | N/A |
| App Icon | Button | Change application icon | N/A |
| Icon Packs | Button | Install and manage icon packs | N/A |
| Separated Headers | Boolean | Separate chat section headers visually | true |
| Divider Style | Enum | Divider appearance: Hidden, Line, Segments | 1 (Line) |
| Use System Fonts | Boolean | Use device system fonts | false |
| Use System Emoji | Boolean | Use device emoji instead of custom | false |
| MD3 Styles | Switch Group | Material Design 3 theme components | N/A |
| - New Loading Style | Boolean | Updated loading animation | false |
| - New Slider Style | Boolean | Updated slider UI | false |
| - New Switch Style | Boolean | Updated switch/toggle UI | false |
| - New Chat Header Style | Boolean | Updated chat header design | false |
| - New Navigation Bar Style | Boolean | Updated bottom navigation | false |
| - New FAB Style | Boolean | Updated floating action button | false |
| Gooey Avatar Animation | Boolean | Animated avatar transitions | true |
| Custom Themes | Boolean | Enable custom chat themes | false |
| Force Snow | Boolean | Show snow effect in UI | false |
| Glass Outline Style | Enum | Glassmorphism effect: Glare, Solid, Hidden | 0 (Glare) |
| Force Blur | Boolean | Force background blur effect | false |
| Glass Message Menu | Boolean | Apply glass effect to message menu | false |

### Chat Settings
These settings control chat-specific behavior and interactions:

| Setting | Type | Description | Default |
|---------|------|-------------|---------|
| Swipe Actions | Enum | Primary swipe action configuration | 0 (default) |
| Swipe Actions Loop | Boolean | Allow continuous swipe actions | false |
| Swipe Actions Reversed | Boolean | Reverse swipe action direction | false |
| Swipe to Pip | Boolean | Swipe video to picture-in-picture | false |
| Double Tap Action | Enum | Double-tap action on media | 0 (default) |
| Double Tap Seek Duration | Integer | Seconds to seek on double tap | 5 |
| Reply Background | Boolean | Show background on replied messages | true |
| Reply Colors | Boolean | Color-code reply lines | true |
| Reply Emoji | Boolean | Show emoji reactions on replies | true |
| Show Clear Button | Boolean | Show clear text button in input | true |
| Show Copy Photo Button | Boolean | Show copy button on photos | true |
| Show Details Button | Boolean | Show details button in media viewer | true |
| Show Feed Tab | Boolean | Show feed tab in interface | true |
| Show Feed Unread Counter | Boolean | Show unread count on feed tab | true |
| Show Forwards Count | Boolean | Show forward counter on messages | true |
| Show Generate Button | Boolean | Show AI generate button in input | true |
| Show History Button | Boolean | Show edit history button | true |
| Show Repeat Message Button | Boolean | Show repeat message button | true |
| Show Report Button | Boolean | Show report message button | true |
| Show Results Before Voting | Boolean | Show poll results before voting | false |
| Show Save Message Button | Boolean | Show save button on messages | true |
| Show Online Status | Boolean | Show contact online status | true |
| Unmute With Volume Buttons | Boolean | Unmute videos with volume keys | false |
| Zoom Slider | Boolean | Show zoom slider on media | true |
| Wide Posts in Channels | Boolean | Allow wide media posts in channels | true |
| Wide Posts in Feed | Boolean | Allow wide media posts in feed | true |

### Advanced/Experimental Settings
These settings are found in OtherPreferencesActivity and control experimental features:

| Setting | Type | Description | Default |
|---------|------|-------------|---------|
| Use Google Analytics | Boolean | Enable Google Analytics tracking | true |
| Use Google Crashlytics | Boolean | Enable crash reporting | true |
| Export Settings | Button | Backup settings to file | N/A |
| Export Data | Button | Export chat history and media | N/A |
| Reset Settings | Button | Reset all preferences to defaults | N/A |
| Delete Account | Button | Permanently delete account | N/A |
| Donate | Button | Support development via donations | N/A |

### AI Assistant Settings
These settings control the AI assistant functionality:

| Setting | Type | Description | Default |
|---------|------|-------------|---------|
| Save History | Boolean | Save conversation history with AI | true |
| Response Streaming | Boolean | Stream AI responses in real-time | true |
| Temperature | Integer | AI creativity/randomness (0-20) | 10 |
| Show Response Only | Boolean | Hide user messages in AI chat view | false |
| Insert As Quote | Boolean | Format AI replies as block quotes | true |
| On Device Reasoning | Boolean | Use local AI model for reasoning | false |
| On Device Preview Model | Boolean | Use local model for preview generation | false |
| On Device Fast Model | Boolean | Use local model for fast responses | false |
| Selected Service ID | String | Currently selected AI service | "default" |
| Selected Service Hash | Integer | Legacy hash of selected service | Service-dependent |
| Selected Role | String | AI assistant role/personality | "default" |

### Plugin System Settings
These settings manage the plugin extension system:

| Setting | Type | Description | Default |
|---------|------|-------------|---------|
| Enable AdBlock | Boolean | Enable ad blocking functionality | false |
| Pinned Plugins | String Set | Pinned (always visible) plugins | Empty set |
| Plugins Compact View | Boolean | Use compact layout for plugins | false |
| Plugins Dev Mode | Boolean | Enable developer mode for plugins | false |
| Plugins Disable Art Options | Boolean | Disable plugin art/customization options | false |
| Plugins PySdk Auto Update | Boolean | Auto-update Python SDK for plugins | true |
| Plugins PySdk Beta Versions | Boolean | Use beta versions of Python SDK | false |
| Plugins Safe Mode | Boolean | Start plugins in safe mode | false |
| Plugins Unknown Sources | Boolean | Allow installation from unknown sources | false |
| Postprocessing With AI | Boolean | Apply AI post-processing to media | false |
| Predictive Back Intensity | Integer | Predictive back gesture sensitivity | 0 |
| Prefer Original Quality | Boolean | Prefer original media quality over optimized | false |
| Quick Admin Shortcuts | Boolean | Enable quick admin actions | false |
| Quick Transition For Channels | Boolean | Faster channel transition animations | false |
| Quick Transition For Topics | Boolean | Faster topic transition animations | false |
| Recognition Language | String | Language for speech recognition | "en-US" |
| Remember Last Used Camera | Boolean | Remember last camera selection | true |
| Remove Message Tail | Boolean | Remove animated tails from messages | false |
| Replace Edited With Icon | Boolean | Show edit icon instead of "edited" label | false |
| Schedule Updates | Timestamp | Scheduled update check timestamp | 0 |
| Section Radius | Integer | Radius for section headers (dp) | 0 |
| Sections Separated Headers Preference | Boolean | Preference for separated headers | true |
| Sender Mini Avatars | Boolean | Show small sender avatars | true |
| Show Clear Button | Boolean | Show clear text button | true |
| Show Copy Photo Button | Boolean | Show copy button on photos | true |
| Show Details Button | Boolean | Show details button in viewer | true |
| Show Feed Tab | Boolean | Show feed tab | true |
| Show Feed Unread Counter | Boolean | Show unread counter on feed | true |
| Show Forwards Count | Boolean | Show forward counter | true |
| Show Generate Button | Boolean | Show AI generate button | true |
| Show History Button | Boolean | Show history button | true |
| Show Id And Dc | Integer | Show account ID/DC: Hide(0), Telegram API(1), Bot API(2) | 0 |
| Show Online Status | Boolean | Show online status | true |
| Show Repeat Message Button | Boolean | Show repeat message button | true |
| Show Report Button | Boolean | Show report message button | true |
| Show Results Before Voting | Boolean | Show poll results before voting | false |
| Show Save Message Button | Boolean | Show save message button | true |
| Square FAB | Boolean | Use square floating action button | false |
| Start With Wide Angle Camera | Boolean | Default to wide-angle camera | false |
| Static Zoom | Integer | Fixed zoom level for camera | 0 |
| Sticker Shape | Enum | Sticker corner shape: Rounded, Squircle, etc. | 0 (Rounded) |
| Sticker Size | Enum | Sticker size: Small, Medium, Large | 1 (Medium) |
| Sticker Time Mode | Enum | Sticker timing: Forever, 1sec, 3sec, etc. | 0 (Forever) |
| Swipe Actions | Enum | Chat swipe action configuration | 0 (default) |
| Swipe Actions Loop | Boolean | Enable continuous swipe actions | false |
| Swipe Actions Reversed | Boolean | Reverse swipe action direction | false |
| Swipe to Pip | Boolean | Enable swipe-to-picture-in-picture | false |
| Target Language | String | Target language for translation | System language |
| Telegram Ai Editor | Boolean | Use AI-powered text editor | false |
| Telegram Ai Instant View Summaries | Boolean | Generate AI summaries for Instant View | false |
| Telegram Ai Summaries | Boolean | Generate AI summaries for links/articles | false |
| Title Text | Enum | Action bar title text source | 0 (App Name) |
| Transition Animation | Enum | Screen transition animation type | 0 (default) |
| Translation Provider | Enum | AI translation service selection | 0 (default) |
| Unlimited Recent Stickers | Boolean | Show unlimited recent stickers | false |
| Unmute With Volume Buttons | Boolean | Unmute media with volume keys | false |
| Update Schedule Timestamp | Timestamp | Last update check time | 0 |
| Upload Speed Boost | Boolean | Increase upload speed | false |
| Use Google Analytics | Boolean | Enable analytics tracking | true |
| Use Google Crashlytics | Boolean | Enable crash reporting | true |
| Use System Fonts | Boolean | Use device system fonts | false |
| Use System Icon Shape | Boolean | Use device adaptive icon shape | false |
| Video Messages Camera | Enum | Camera for video messages: Front, Back | 0 (Front) |
| Wide Posts In Channels | Boolean | Allow wide media in channels | true |
| Wide Posts In Feed | Boolean | Allow wide media in feed | true |
| Zoom Slider | Boolean | Show zoom slider on media | true |

## Technical Implementation Details

### Configuration Architecture
ExteraGram uses a delegate-based configuration system:
- Each setting is defined as a `private static final BasePref \w+\$delegate`
- Delegates are initialized in static blocks using specific Pref classes:
  - `BooleanPref` for true/false settings
  - `StringPref` for text values
  - `IntegerPref`/`LongPref` for numeric values
  - `EnumPref` for enumerated options
  - `NullableStringPref` for nullable strings
  - `StringSetPref` for string collections
- Getter/setter methods provide typed access to preference values
- Preferences are persisted via SharedPreferences with keys matching delegate names

### Preference Activity Mapping
Each preference screen maps UI elements to configuration settings:
- **Checkboxes** (`uCheck$TMessagesProj$default`) map to boolean preferences
- **Buttons** (`uButton$TMessagesProj$default`) map to enum/string preferences with value display
- **Sliders/Seekbars** map to integer range preferences
- **Custom views** handle complex settings like color pickers or previewers
- Settings groups are organized with headers and separators for usability

### Core Integration Points
While full telegrambase.org tracing requires access to upstream sources, exteraGram settings integrate with the Telegram core through:
- Direct calls to `ExteraConfig.get*()` methods throughout the UI code
- Configuration-dependent UI rendering in custom views and cells
- Behavior modifiers in chat rendering, message processing, and interaction handlers
- Feature flags that enable/disable upstream Telegram functionality
- Custom implementations that extend or replace base Telegram features

## Status in 100gram

Based on the comparative analysis in `exteragram_audit_and_ui_plan.md` and feature roadmap in `exteragram_features_roadmap_plan.md`:

### Fully Implemented (100%)
- Icon packs system
- Settings backup/PillStack functionality  
- UI customization options
- AI assistant modules

### Partially Implemented or Requires Adaptation
- Many general settings have equivalents but may differ in implementation
- Appearance settings often require theme engine modifications
- Chat settings may need integration with modified chat systems
- Advanced/experimental features may need selective porting

### Unique to ExteraGram (Not in Base 100gram)
- Specific AI integration patterns
- Certain experimental features
- Specialized plugin system extensions
- Custom analytics/crash reporting integrations
- Specific donation/support systems

## Recommendations for Feature Porting

When considering porting exteraGram features to 100gram:

1. **Prioritize by User Value**: Focus on settings that provide meaningful customization or privacy enhancements
2. **Check for Conflicts**: Verify thatported settings don't conflict with existing 100gram features
3. **Adapt to 100gram Patterns**: Convert exteraGram's preference implementation to match 100gram's settings architecture
4. **Localize Properly**: Ensure all UI strings are added to both Russian and English string resources
5. **Consider Performance**: Evaluate impact of each setting on app performance and battery usage
6. **Maintain Consistency**: Follow 100gram's existing settings organization and naming conventions

This inventory provides the foundation for making informed decisions about which exteraGram features to integrate into 100gram based on technical feasibility, user value, and alignment with project goals.