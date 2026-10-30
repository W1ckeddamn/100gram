# 100gram Project State & Handoff

> **Generated:** 2026-10-30 20:30 UTC  
> **Branch:** `main`  
> **Head Commit:** `0459f1b50` (feat: complete Roadmap Phases 1, 2, and 3 (MD3 Suite, Sliders Engine, Sticker Owner, TTL Secret View, Social Link Fixers))  
> **Working Tree:** `DIRTY (10 changes)`

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
  *Key artifacts (1):* `ROADMAP_PLAN.md`

---

## 4. Recent Commits (Last 10)

```text
0459f1b50 feat: complete Roadmap Phases 1, 2, and 3 (MD3 Suite, Sliders Engine, Sticker Owner, TTL Secret View, Social Link Fixers)
eb607d491 docs: sync project state for interactive round video crop
03b25261c feat(media): interactive circular crop for round video messages with 6-point contract
04f4ba316 docs: update PROJECT_STATE.md
2295007ab fix(policy): enforce vanilla defaults and 6-point contract for sliders and UI mods
5224a973c feat: peek warning once, big reactions, transcript quotes, swipe-to-hide keyboard (from OpexGram)
61f638df9 feat: profile join date and poll results peek (from OpexGram)
577b809ca feat: remember last folder (from OpexGram)
b0cadbb57 feat: autoplay toggles for voice, round video and music (from OpexGram)
8ff3d2aa7 feat: bubble tails toggle now works, pinned downloads, hide all-chats folder (from OpexGram)
```

---

## 5. Current Handoff Point (From OpenCode)

- **Active Session:** `ses_e816b9ef8ffeMVX1FeSi525T7i` (Opexgram 13.1.1 beta10 features)
- **Last Updated:** 2026-10-30 20:14:23
- **Last Request:** "можешь состояние что щас мы делаем что делали записать что бы передать другому агенту"
- **Investigation:** Material Design 3 (MD3) style toggles in `SharedConfig.java` (`md3ChatList`, `md3Sections`, `md3Controls`, `md3Menus`, `md3NavigationStyle`, `md3MediaLoader`).

---

## 6. Remaining Feature Backlog (From OpexGram)

| Feature | Status / Priority | Key References |
|---|---|---|
| **MD3 Theme & Toggles** | Next in line | `SharedConfig.md3*`, `Theme.java` |
| **Expressive Chat Input** | Backlog | `ChatActivityEnterView`, renderer `lc.u` |
| **Glass Message Bubbles** | Backlog | `ChatMessageCell.drawBackground`, blur effects |
| **Sticker Quote Generator** | Backlog | Selected messages → sticker bitmap |
| **Scheduled / Bulk Gifts** | Backlog | `GiftSheet.java`, TL send flows |

> Full parity matrix available in [`OPEXGRAM_PARITY.md`](file:///D:/100gram/OPEXGRAM_PARITY.md).

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
