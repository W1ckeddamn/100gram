# 100gram Project State & Handoff

> **Generated:** 2026-10-31 16:15 UTC  
> **Branch:** `main`  
> **Head Commit:** `6cd3d0977` (docs: update PROJECT_STATE.md with dynamic feature tracking and 12-icon pack commit)  
> **Working Tree:** `DIRTY (31 changes)`

---

## 1. Multi-Environment Ecosystem

| Source | Role | Key Function / Content |
|---|---|---|
| **`D:\100gram`** | Primary Git Repo | Active Telegram fork (`main`), C++ JNI (`merge=ours`), standalone APK target |
| **`D:\opexgram`** | Feature Donor (13.1.1) | Decompiled OpexGram beta10 APK (`opexgram_src`), strings index, feature maps |
| **`D:\exteragram`** | Donor Reference (extera/Ayu) | Extracted classes (`AiController`, `AppearancePreferencesActivity`, etc.) |
| **OpenCode DB** | Session Logs | `~/.local/share/opencode/opencode.db` (OpexGram parity batches 1–10) |
| **Antigravity Brain** | Architecture & Core Specs | `~/.gemini/antigravity-cli/brain/` (AyuGram/extera modules, Vault, 21 plans) |

---

## 2. Core Philosophy: User Sovereignty & Total Customizability

> **Cardinal Rule:** Nothing is forced. Every feature, visual tweak, animation, or behavior modification **MUST** be optional and controlled by a user setting.

- **No Unconditional Mutations:** Never alter Telegram's UI or behavior without a toggle.
- **Vanilla Baseline:** If all 100gram toggles are off, the app must behave identically to vanilla official Telegram.
- **Granularity over Monoliths:** Provide fine-grained toggles rather than monolithic bundles (e.g. separate MD3 toggles for Chat List, Sections, Controls, Menus, Navigation, and Media Loaders).
- **The 6-Point Feature Contract:** Every feature must implement: (1) `HundredGramConfig` prefs, (2) `HundredGramSubSettingsActivity` UI, (3) EN/RU strings, (4) `HundredGramSettingsSearchIndex`, (5) `HundredGramBackupManager` JSON support, and (6) `resetAllSettings()` handling.

---

## 3. Antigravity Historical Foundations

Antigravity sessions established the core architectural foundations of 100gram:

- **`fa0e7e88-df3f-4c38-88eb-1148e800ed67`** — Foundational architecture, exteraGram/AyuGram parity, Vault, AI assistant & C++ JNI sync
  *Key artifacts (21):* `ayugram_features_audit_and_plan.md, bugfix_plan.md, build_apk_plan.md, changelog_system_plan.md, exteragram_audit_and_ui_plan.md`
- **`34a35936-5716-4850-97ce-a5a3742fbccc`** — REA reverse engineering of exteraGram & AyuGram APKs into D:\exteragram
- **`c62ff3ad-d3c3-41c1-99af-c5f825ca711a`** — State inspection & synchronization between OpenCode and Antigravity
  *Key artifacts (2):* `BADGES_VECTOR_PREVIEW.md, ROADMAP_PLAN.md`

---

## 4. Recent Commits (Last 10)

```text
6cd3d0977 docs: update PROJECT_STATE.md with dynamic feature tracking and 12-icon pack commit
7ad190da0 feat: Add 12 custom vector badges, update dynamic roadmap parity and sync script
69f03668a feat: Add remote GitHub badges sync, owner ID 1211607237 role access and admin management
06605a865 feat: Add Custom User Badges with icon, title, description and management UI
4821a0375 feat(media): implement song lyrics, md3 audio player, and slowmode queue (Phase 5)
a5e541068 feat(visuals): implement Phase 4 - glass bubbles, expressive input box, and sticker quotes
0459f1b50 feat: complete Roadmap Phases 1, 2, and 3 (MD3 Suite, Sliders Engine, Sticker Owner, TTL Secret View, Social Link Fixers)
eb607d491 docs: sync project state for interactive round video crop
03b25261c feat(media): interactive circular crop for round video messages with 6-point contract
04f4ba316 docs: update PROJECT_STATE.md
```

---

## 5. Current Focus & Handoff Point

- **Active OpenCode Session:** `ses_e816b9ef8ffeMVX1FeSi525T7i` (Opexgram 13.1.1 beta10 features)
- **Last Updated:** 2026-10-31 18:15:19
- **Last Request:** "давай переработай все тогда и своих добавь может есть идеи"
- **Active Focus:** Custom User Badges (`HundredGramBadgesController`), remote GitHub JSON sync (`badges.json`), Lead Dev role (ID `1211607237L`), and 12-icon VectorDrawable expansion.

---

## 6. Feature Status & Roadmap Parity (Dynamic from Git)

### 🟢 Implemented Features

| Feature | Category | Commit | Key References |
|---|---|:---:|---|
| **MD3 Theme & Toggles** | UI | `0459f1b50` | `HundredGramConfig.md3*`, `Theme.java` |
| **Sliders Engine (MD3 SeekBar)** | UI | `0459f1b50` | `HundredGramConfig.newSliderStyle`, `SeekBarView` |
| **Sticker Owner & TTL Secret View** | Chats | `0459f1b50` | `HundredGramConfig.secretViewTTL`, `StickersAlert` |
| **Social Link Fixers** | Chats | `0459f1b50` | `HundredGramConfig.socialLinkFixer`, `ChatActivity` |
| **Glass Message Bubbles** | Visuals | `a5e541068` | `HundredGramConfig.glassMessages*`, `MessageDrawable` |
| **Expressive Chat Input** | Visuals | `a5e541068` | `HundredGramConfig.chatBoxStyle`, `ChatActivityEnterView` |
| **Sticker Quote Generator** | Visuals | `a5e541068` | `HundredGramConfig.stickerQuotes`, `HundredGramQuoteStickerAlert` |
| **Song Lyrics (ID3 + LRCLIB)** | Media | `4821a0375` | `HundredGramConfig.lyricsInPlayer`, `HundredGramLyricsHelper` |
| **MD3 Audio Player** | Media | `4821a0375` | `HundredGramConfig.md3AudioPlayer`, `AudioPlayerAlert` |
| **Slowmode Queue** | Chats | `4821a0375` | `HundredGramConfig.slowmodeQueue`, `HundredGramSlowmodeController` |
| **Custom User Badges & Remote Sync** | Badges | `69f03668a` | `HundredGramConfig.customBadgesEnabled`, `HundredGramBadgesController` |
| **12 Vector Badges Icon Pack** | Assets | `7ad190da0` | 12 VectorDrawable XML assets (`badge_*.xml`) |

### ⏳ Remaining Backlog & Active Work

| Feature | Category | Status / Priority | Key References |
|---|---|---|---|
| **Scheduled / Bulk Gifts** | Gifts | ⏳ Backlog | `StarGiftSheet.java`, TL send flows |
| **Icon Color Source (Monet / Rainbow)** | Theming | ⏳ Backlog | `IconPacksActivity` |
| **AI Chat FX Glow** | AI | ⏳ Backlog | Visual glow shader on AI responses |

> Full parity matrix available in [`OPEXGRAM_PARITY.md`](file:///D:/100gram/OPEXGRAM_PARITY.md) and [`ROADMAP_PLAN.md`](file:///C:/Users/wicked/.gemini/antigravity-cli/brain/c62ff3ad-d3c3-41c1-99af-c5f825ca711a/ROADMAP_PLAN.md).

---

## 7. Key Architectural Touchpoints

- **Configs:** `HundredGramConfig.java`
- **Settings Activities:** `HundredGramSubSettingsActivity.java`, `HundredGramMessageMenuActivity.java`, `HundredGramSettingsActivity.java`
- **Custom Engines:** `HundredGramHaptics.java`, `HundredGramLocks.java`, `HundredGramDeleteMyMessages.java`, `HundredGramRealNameCache.java`, `HundredGramIconManager.java`, `HundredGramAiClient.java`
- **Strings:** `hundredgram_strings.xml`, `hundredgram_strings_ru.xml`

---

## 8. Verification Command

```powershell
.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatDebug -x lint -x test --daemon
```
