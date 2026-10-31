# 100gram Project State & Handoff

> **Generated:** 2026-10-31 21:17 UTC  
> **Branch:** `main`  
> **Head Commit:** `9bbe30801` (fix(core): prevent Activity Simulator and Badge sync crashes, fix exact alarms and Android 14 FGS)  
> **Working Tree:** `Clean`

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
9bbe30801 fix(core): prevent Activity Simulator and Badge sync crashes, fix exact alarms and Android 14 FGS
b189c1c6e refactor(ui): redesign settings and sub-tabs to match native Telegram style
81b8b114d feat(privacy): add bypass content protection toggle with full 6-point contract
67c4eaf01 chore(docs): sync PROJECT_STATE.md with Telegram 13.1.1 merge
fa58c15af sync(upstream): merge Telegram Android 13.1.1 (7237)
57f5475fc chore(docs): sync PROJECT_STATE.md
ea3ffd0f6 fix(profile): fix registration date icon being overwritten to null and support row tap
35fccfb02 docs: sync PROJECT_STATE.md with latest bug fixes
1b2f92679 fix(ui): resolve feed avatar notch, header offsets, now playing redirect, avatar preview collision, and player playlists
3dbde83dd docs: archive stale handover/features, move dev scripts to Tools, add docs index
```

---

## 5. Current Focus & Handoff Point

- **Active OpenCode Session:** `ses_e816b9ef8ffeMVX1FeSi525T7i` (Opexgram 13.1.1 beta10 features)
- **Last Updated:** 2026-10-31 18:31:32
- **Last Request:** "[26.10.2026 13:44] 𝐰𝖏𝖈𝖐𝖊𝐝𝐝𝖆𝖒𝖓 🎗️: фикс верхней панели с надписью лента и аватаркой смещение плюс на аватарке вырезан кружочек какой то справа сверху откусан так сказать

сообщение в ленте слишком низко опускаются под панель главную с вкладками нужно приподнять
[26.10.2026 19:35] 𝐰𝖏𝖈𝖐𝖊𝐝𝐝𝖆𝖒𝖓 🎗️: https://github.com/YouRooni/plugins/tree/master/plugins/md3_player
[26.10.2026 19:36] 𝐰𝖏𝖈𝖐𝖊𝐝𝐝𝖆𝖒𝖓 🎗️: now playing или как то так в профиле пользователя карточка аудио переводит почему то в избранное
[26.10.2026 19:38] 𝐰𝖏𝖈𝖐𝖊𝐝𝐝𝖆𝖒𝖓 🎗️: внешний вид и чаты нажатие по превью скругление аватарок переносит в меню теневого бана
[26.10.2026 19:39] 𝐰𝖏𝖈𝖐𝖊𝐝𝐝𝖆𝖒𝖓 🎗️: в плеере плейлисты не работают вовсе создаешь но добавить или открыть не можешь"
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
| **12 Vector Badges Icon Pack** | Assets | `f9978124a` | 12 VectorDrawable XML assets (`badge_*.xml`) |

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
