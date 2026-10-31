# ExteraGram Interface and Animation Improvements vs. Stock Telegram

This document details the specific interface and animation enhancements found in exteraGram that distinguish it from the standard Telegram application.

## 🎨 Advanced Animation System

### Transition Animations
exteraGram replaces Telegram's basic transition system with a configurable animation framework offering three distinct styles:

1. **DEFAULT**: Standard Telegram transitions (fade/slide)
2. **AOSP**: Android Open Source Project style transitions
3. **SPRING**: Physics-based spring animations with natural overshoot and settling

**Implementation Files:**
- `TransitionAnimation.java` - Enum defining the three animation styles
- Configuration via `ExteraConfig.getTransitionAnimation()` and setter methods
- Migration logic in `ExteraConfig.migrateTransitionAnimation()` for backward compatibility
- Usage throughout UI code via `ExteraConfig.getTransitionAnimation() == TransitionAnimation.SPRING` etc.

### Window Transition Animations
Enhanced window transition system found in `WindowTransitionAnimationHelper.java`:

- **Custom Timing Curves**: Uses `CubicBezierInterpolator.Emphasized` instead of standard Android interpolators
- **Dynamic Duration**: Animation duration calculated as `max(1.0f, system_scale * 450.0f)` 
- **Window Edge Extensions**: Special rendering nodes for capturing and animating window edges during transitions
- **Slide Distance**: Fixed at 96dp for enter/exit transitions
- **Alpha Fading**: Custom fade functions based on animation progress with clamped values

## 💎 Glassmorphism Effects

### Glass Outline Styles
Defined in `GlassOutlineStyle.java` with three preset options:

1. **GLARE**: Glass effect with simulated glare/highlight for depth
2. **SOLID**: Uniform glass appearance without highlights
3. **HIDDEN**: Disabled glass effect (falls back to standard rendering)

**Applied To:**
- Tab bars and navigation components (`GlassTabView`)
- Background elements and overlays
- Menu interfaces and popups
- Controlled via `ExteraConfig.getGlassOutlineStyle()` returning enum value
- UI binding in `AppearancePreferencesActivity` via button showing current style name

### Liquid Glass Effect
Advanced glass rendering implementations found in hooked upstream files:
- Custom shaders for liquid-like glass distortions
- Blur and noise suppression techniques
- Depth-based glass intensity variations

## 💧 Gooey Avatar Animations

Fluid, viscous avatar transition effects that provide elastic, springy appearance/disappearance animations:

**Implementation:**
- Controlled by `ExteraConfig.getGooeyAvatarAnimation()` boolean flag
- Implemented in `ProfileGooeyView` components with both CPU and GPU implementations
- Provides organic, springy physics-based avatar motions instead of linear transitions
- Found referenced in hooked classes: `ProfileGooeyView$CPUImpl` and `ProfileGooeyView$GPUImpl`

## ← Predictive Back Animations

Enhanced back gesture navigation with predictive preview:

**Implementation:**
- `PredictiveBackAnimationHelper.java` handles back gesture animations
- Predictively renders destination screen during swipe-back gesture
- Uses spring physics for smooth completion or cancellation
- When gesture is incomplete, animates back to source with spring tension
- Provides iOS-style predictive back navigation on Android

## 🎛️ Enhanced UI Customization Controls

### From ChatsPreferencesActivity
- **Quick Transition Toggles**: 
  - `getQuickTransitionForChannels()` / `setQuickTransitionForChannels()` 
  - `getQuickTransitionForTopics()` / `setQuickTransitionForTopics()`
  - Enable faster animations when navigating to channels/topics
- **Sticker Size Animation**: 
  - ValueAnimator used in preference screen for sticker size preview (line 930-943)

### From AppearancePreferencesActivity
- **Gooey Avatar Animation Toggle**: Direct checkbox binding to `ExteraConfig.getGooeyAvatarAnimation()`
- **Glass Outline Style Selector**: Button showing current style (GLARE/SOLID/HIDDEN) 
- **Glass Message Menu Effect**: Toggle for applying glass effect to message context menus
- **MD3 Switch Group**: Granular control over Material Design 3 components:
  - New loading style
  - New slider style  
  - New switch style
  - New chat header style
  - New navigation bar style
  - New FAB (Floating Action Button) style

## 📐 Math-Based Reveal Animations

Advanced reveal effects using mathematical functions:

**Implementation:**
- `MathRevealAnimation.java` - Custom reveal animations using mathematical equations
- Provides material-reveal style effects on UI elements
- More sophisticated than standard Android reveal animations
- Allows custom easing functions and reveal patterns

## ⚡ Performance Optimizations

Despite advanced effects, exteraGram maintains performance through:

- **Efficient Rendering**: Uses `RenderingNode` and `Canvas` for glass effects rather than heavy overlays
- **Resource Cleanup**: Proper disposal of animation resources and render nodes
- **Frame-Rate Conscious**: Animations respect system animation scales and device capabilities
- **Selective Application**: Effects applied only where visually beneficial, not universally

## 📊 Key Differences from Stock Telegram

| Feature Category | Stock Telegram | exteraGram Improvement |
|------------------|----------------|------------------------|
| **Transitions** | Basic fade/slide transitions | Three-option system: DEFAULT/AOSP/SPRING with spring physics |
| **Glass Effects** | None | GLARE/SOLID/HIDDEN glassmorphism with liquid variants |
| **Avatar Animations** | Standard linear transitions | Gooey/fluid avatar animations with elastic physics |
| **Back Gestures** | Basic swipe-back | Predictive back with destination preview and spring cancellation |
| **Animation Controls** | Limited/no user options | Granular user controls for transitions, glass effects, avatar animations |
| **Interpolators** | Standard Android interpolators | Custom CubicBezierInterpolator.Emphasized for window transitions |
| **Reveal Effects** | Basic reveal animations | MathRevealAnimation with custom mathematical functions |
| **MD3 Implementation** | Basic or partial | Granular control over individual MD3 component styles |

## 🔧 Technical Implementation Notes

1. **Configuration Architecture**: All enhancements controlled via `ExteraConfig` delegate pattern:
   - BooleanPrefs for toggles (gooey avatar, quick transitions, etc.)
   - EnumPrefs for multi-option styles (transition animation, glass outline)
   - IntegerPrefs for numeric values (sticker size, etc.)

2. **UI Integration**: Preferences directly bound to UI elements:
   - Checkboxes for boolean features
   - Buttons showing current value for enums
   - Custom views for complex previews (sticker size animator)

3. **Performance Considerations**: 
   - Glass effects use efficient RenderNode approach
   - Animations respect system scales and can be disabled
   - Preview animations in settings use lightweight ValueAnimator

These improvements collectively create a more modern, visually engaging, and customizable user experience while maintaining the core functionality and performance expectations of a messaging application.