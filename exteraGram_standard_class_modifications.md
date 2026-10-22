# ExteraGram Standard Class Modifications vs. Stock Telegram

This document details the specific modifications made to standard Telegram (org.telegram.*) classes in exteraGram, based on analysis of the hooked classes and source code.

## Overview

ExteraGram modifies hundreds of standard Telegram classes through a hooking/patching mechanism. These modifications range from simple additions to complete reimplementations of core functionality. The modifications are primarily focused on:

1. **UI/Animation Enhancements** - Adding glass effects, gooey animations, custom transitions
2. **Feature Extensions** - Adding new settings, toggles, and UI options
3. **Performance Optimizations** - Improving rendering and animation efficiency
4. **Custom Visual Effects** - Implementing Material Design 3, glassmorphism, fluid animations

## Key Modified Classes by Category

### 1. UI Components & Views

#### ProfileGooeyView (org.telegram.ui.Components.ProfileGooeyView)
- **Complete reimplementation** of avatar rendering
- Adds **gooey/fluid avatar animations** with elastic physics
- Dual implementation: CPU (stack blur) and GPU (RenderNode) versions
- Uses `ExteraConfig.getGooeyAvatarAnimation()` to toggle feature
- Modifications in both `$CPUImpl` and `$GPUImpl` inner classes

#### GlassTabView (org.telegram.ui.Components.glass.GlassTabView)
- **Custom glass-effect tab implementation**
- Uses `GlassOutlineStyle` (GLARE/SOLID/HIDDEN) from `ExteraConfig`
- Implements spring animations for tab indicators
- Custom drawing with `RenderNode` for efficient glass rendering
- Supports both Lottie and static tab icons with glass overlay

#### LiquidGlassEffect (org.telegram.ui.Components.blur3.LiquidGlassEffect)
- **Shader-based glass effect** implementation
- Uses `RenderEffect` and `RuntimeShader` for real-time glass distortion
- Configurable intensity, resolution, and color properties
- Applied to backgrounds and overlays for glassmorphism effect

#### ChatMessageCell (org.telegram.ui.Cells.ChatMessageCell)
- **Extensive modifications** to message rendering
- Added support for quick transitions (channels/topics)
- Custom animation handling for message appearance/disappearance
- Modified transition parameters via `TransitionParams` inner class
- Enhanced sticker size animation controls

### 2. Animation & Transition Classes

#### TransitionExt (org.telegram.ui.Components.TransitionExt)
- **Extended transition functionality**
- Builds upon standard Telegram transitions
- Adds spring physics and custom timing curves
- Used for predictive back animations and window transitions

#### VoiceMessageEnterTransition (org.telegram.ui.VoiceMessageEnterTransition)
- **Custom voice message entry animations**
- Modified standard Telegram voice message popup animations
- Added spring-based entrance effects
- Configurable via exteraGram settings

#### EmojiAnimationsOverlay (org.telegram.ui.EmojiAnimationsOverlay)
- **Modified emoji burst animations**
- Enhanced emoji animation overlays with custom timing
- Added support for additional emoji effects
- Modified inner classes like `$3` for custom behavior

### 3. Utility Classes

#### AndroidUtilities (org.telegram.messenger.AndroidUtilities)
- **Significant additions** to core utilities
- Added imports for exteraGram-specific classes (`ExteraConfig`, `DividerStyle`, etc.)
- Extended functionality for glass effects and animations
- Added utility methods for theme calculations and resource access

#### ApplicationLoader (org.telegram.messenger.ApplicationLoader)
- **Extended application initialization**
- Added exteraGram-specific initialization code
- Modified to support hooked class loading
- Added configuration for exteraGram features

#### BuildVars (org.telegram.messenger.BuildVars)
- **Modified build variables**
- Added exteraGram-specific build flags
- Modified version tracking for fork identification
- Added constants for exteraGram features

### 4. Settings & Preferences

#### ExteraConfig (com.exteragram.messenger.ExteraConfig)
- **Central configuration system** (not a hooked class but central to modifications)
- Manages all exteraGram-specific settings and toggles
- Uses `SharedPreferences` with custom delegates
- Controls feature flags for all modified classes

#### AppearancePreferencesActivity & ChatsPreferencesActivity
- **Extended preference screens**
- Added controls for glass effects, gooey animations, transition styles
- MD3 customization options (loading, slider, switch, header, nav bar, FAB styles)
- Quick transition toggles for channels and topics

## Specific Examples of Modifications

### Example 1: ProfileGooeyView Modification
**Standard Telegram**: Simple avatar drawing with basic fade/scale animations
**ExteraGram**: 
- Complete rewrite using physics-based gooey effects
- CPU implementation uses `BlurMaskFilter` for fluid appearance
- GPU implementation uses `RenderNode` with displacement shaders
- Animation responds to touch/drag with elastic recoil
- Visibility controlled by `ExteraConfig.getGooeyAvatarAnimation()`

### Example 2: GlassTabView Modification
**Standard Telegram**: Basic tab icons with selected/unselected states
**ExteraGram**:
- Glass effect rendering using custom shaders
- Spring animations for selection indicators
- Dynamic color blending based on selection state
- Support for both outlined and filled icon states with glass overlay
- Configurable glass style via `ExteraConfig.getGlassOutlineStyle()`

### Example 3: LiquidGlassEffect Modification
**Standard Telegram**: No native glass effect support
**ExteraGram**:
- Custom GLSL shader for liquid glass distortion
- Real-time refraction simulation using `RuntimeShader`
- Configurable parameters: intensity, resolution, center point
- Applied to backgrounds, overlays, and UI elements for depth effect
- Performance optimized using `RenderNode` caching

### Example 4: ChatMessageCell Modification
**Standard Telegram**: Basic message appearance/disappearance animations
**ExteraGram**:
- Added quick transition options for channels/topics
- Custom transition parameters via inner `TransitionParams` class
- Enhanced sticker size animation with `ValueAnimator` preview
- Modified appearance timing based on exteraGram transition settings
- Support for glass backgrounds on messages when enabled

## Pattern Analysis

Examining the hooked classes reveals several patterns:

1. **Extension over Replacement**: Most modifications extend existing functionality rather than completely replacing it
2. **Configuration-Driven**: Features are toggled via `ExteraConfig` settings
3. **Performance Conscious**: Heavy effects use efficient rendering techniques (`RenderNode`, shaders)
4. **Backward Compatibility**: Modifications maintain standard Telegram interface when features disabled
5. **Layered Approach**: Visual effects are often layered (e.g., glass over standard UI)

## Impact on User Experience

These modifications collectively create:

1. **Enhanced Visual Depth**: Glassmorphism effects add layering and depth perception
2. **Fluid Interactions**: Gooey animations provide organic, responsive feel
3. **Customizable Transitions**: Users can choose between different animation styles
4. **Modern Aesthetics**: MD3 customization brings Material Design 3 enhancements
5. **Performance Optimization**: Despite added effects, performance is maintained through efficient rendering

## Verification Methodology

The modifications were identified through:
1. Analysis of `hooked_all_classes.txt` (968+ hooked classes)
2. Examination of source files in `sources_extera_hooked/` directory
3. Comparison with upstream vanilla Telegram sources in `sources_upstream_vanilla/`
4. Review of preference activities showing exposed controls
5. Analysis of patch file `exteragram_hooks_isolated.patch` showing low-level changes

## Conclusion

ExteraGram modifies a substantial number of standard Telegram classes to implement its unique features. These modifications are not superficial but represent deep integrations into the UI rendering, animation systems, and utility classes. The changes are systematically organized around user-facing toggles in `ExteraConfig`, allowing users to enable/disable features while maintaining core Telegram functionality when features are disabled.

The modifications demonstrate a sophisticated approach to feature enhancement that balances visual innovation with performance considerations and user customization options.