# 100gram

```
   _  ___   ___                             
  / |/ _ \ / _ \ ___ ________ ___ _         
  | | | | | | | / _ `/ __/ _ `/  ' \        
  |_|\___/ \___/\_, /_/  \_,_/_/_/_/        
                /___/                       
```

> **PROJECT STATUS: VIBE-CODED**  
> This repository is vibe-coded in continuous collaboration with autonomous AI coding agents (Antigravity) and human direction.  
> Architecture decisions, native patches, and UI additions are written rapidly through LLM iterations and validated against the Android NDK compiler, Gradle toolchains, and daily-driver dogfooding.  
> No corporate bloat. Pragmatic code, direct patches, clean compilation.

---

## Overview

**100gram** is an enhanced, privacy-hardened, and performance-tuned Telegram client fork for Android, built on top of [Telegram for Android](https://github.com/DrKLO/Telegram) v12.10.5 (Build 7105).

The fork bypasses upstream product restrictions, optimizes native MTProto throughput, embeds local machine learning models for speech recognition, and adds power-user media and stealth tools directly into the core application runtime.

---

## Feature Matrix

| Subsystem | Capabilities |
| :--- | :--- |
| **Networking** | Parallel MTProto downloads/uploads (up to 8 concurrent connections), real-time bandwidth monitor, DC1–DC5 round-trip latency ping diagnostics. |
| **Camera & Video Notes** | Multi-engine backend (CameraX, Camera2, Camera1, Native), square video notes with rounded corners (16dp), background blur removal during recording, bitrate control (up to 3.0 Mbps Ultra), 60 FPS mode, ultra-wide lens (0.6x) support. |
| **Media Vault** | Anti-deletion interception for chats and channels, edit-history persistence, zero-copy storage via filesystem hardlinks (`android.system.Os.link`), isolated SQLite-indexed media gallery. |
| **Activity Tracker** | Contact online/offline presence logger, typing activity detector, 90s auto-closing timeout heuristic for hidden last-seen users, daily cumulative time analytics. |
| **Offline STT** | On-device voice and video note transcription powered by embedded Vosk models (no Telegram Premium required), real-time streaming output, 3:1 audio downsampling, AGC. |
| **Gestures & Swipe** | Multi-slot swipe-to-left action selection (Reply, Saved Messages, Copy, Forward, Pin, Edit, Delete), vertical swipe selector with haptic feedback, real-time settings preview. |
| **Navigation & Layout** | Fully dynamic bottom navigation tabs (Chats, Contacts, Settings, Profile) with ViewPager page unbinding, centered action bar titles and avatars, real-time preview dials for chat bubbles, stickers, and avatar corners. |
| **AI Assistant** | Multi-service engine (Gemini, OpenAI, OpenRouter, Groq, Perplexity, Custom endpoints) with reasoning model support, custom personas/roles with animated emoji icons, live ping validation, and persistent conversation history. |
| **Privacy & Stealth** | `FLAG_SECURE` window shield (blocks screenshots and recording in OBS, Discord, AnyDesk), text redaction (`••••••`), ghost mode (read receipt suppression, anonymous story viewing, typing state masking), contact profile audit log. |
| **Plugins & Feeds** | Sandboxed JavaScript plugin engine, aggregated multi-channel feed reader, custom icon pack support. |

---

## Build Guide

### Requirements

- **OS**: Windows 10/11, Linux, or macOS
- **JDK**: OpenJDK 17 (`jbr` from Android Studio)
- **Android SDK**: API 35 (Build-tools 35.0.0)
- **Android NDK**: 26.1.10909125
- **CMake**: 3.22.1

### Configuration (`local.properties`)

```properties
sdk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
ndk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125
```

### Commands (Windows PowerShell)

```powershell
# Set JBR path
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Verify resources for collisions
.\gradlew.bat :TMessagesProj:packageDebugResources --daemon

# Compile Debug APK (skips lints and tests for fast build)
.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatDebug -x lint -x test --daemon
# Output: TMessagesProj_AppStandalone/build/outputs/apk/afat/debug/app.apk

# Compile Release APK
.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatRelease -x lint -x test --daemon
# Output: TMessagesProj_AppStandalone/build/outputs/apk/afat/release/app.apk
```

### Commands (Linux / macOS)

```bash
export JAVA_HOME="/path/to/android-studio/jbr"
./gradlew :TMessagesProj_AppStandalone:assembleAfatDebug -x lint -x test --daemon
```

---

## Upstream Synchronization

Upstream Telegram replaces `TMessagesProj/jni/` with Git submodules. A naive `git merge upstream/master` will wipe all native C++ build sources.

To prevent native source deletion, `.gitattributes` enforces:
```gitattributes
TMessagesProj/jni/** merge=ours
.gitmodules merge=ours
```

Enable the merge driver once per clone:
```bash
git config merge.ours.driver true
```

When pulling upstream updates (`git merge upstream/master`), Git automatically preserves all local native C++ files while updating Java/Kotlin code, layouts, and assets.

---

## Repository Layout

```text
.
|-- TMessagesProj/                   Core library module (Java and C/C++ native source)
|   |-- src/main/java/org/telegram/
|   |   |-- messenger/               Core controllers, storage, network, MTProto
|   |   `-- ui/                      BaseFragment screens, UI cells, bottom sheets
|   |-- jni/                         Native C/C++ source (TgNet, WebRTC, SQLite, BoringSSL)
|   `-- src/main/res/                Resources, styles, drawables, localizations
|-- TMessagesProj_AppStandalone/     Standalone APK build module (no Google Play Services)
|-- .agents/skills/                  Agent engineering guides and architectural rules
|-- CHANGELOG.md                     Detailed version change log
|-- FEATURES.md                      Comprehensive feature documentation
`-- HANDOVER.md                      Technical handover guide
```

---

## License

This project is licensed under the terms of the GNU General Public License v2.0 or later. Based on [Telegram for Android](https://github.com/DrKLO/Telegram).
