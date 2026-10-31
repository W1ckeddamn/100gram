# 100gram Project State & Handoff

> **Generated:** 2026-10-31 16:01 UTC  
> **Branch:** `main`  
> **Head Commit:** `69f03668a` (feat: Add remote GitHub badges sync, owner ID 1211607237 role access and admin management)  
> **Working Tree:** `DIRTY (19 changes)`

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
69f03668a feat: Add remote GitHub badges sync, owner ID 1211607237 role access and admin management
06605a865 feat: Add Custom User Badges with icon, title, description and management UI
4821a0375 feat(media): implement song lyrics, md3 audio player, and slowmode queue (Phase 5)
a5e541068 feat(visuals): implement Phase 4 - glass bubbles, expressive input box, and sticker quotes
0459f1b50 feat: complete Roadmap Phases 1, 2, and 3 (MD3 Suite, Sliders Engine, Sticker Owner, TTL Secret View, Social Link Fixers)
eb607d491 docs: sync project state for interactive round video crop
03b25261c feat(media): interactive circular crop for round video messages with 6-point contract
04f4ba316 docs: update PROJECT_STATE.md
2295007ab fix(policy): enforce vanilla defaults and 6-point contract for sliders and UI mods
5224a973c feat: peek warning once, big reactions, transcript quotes, swipe-to-hide keyboard (from OpexGram)
```

---

## 5. Current Focus & Handoff Point

- **Active OpenCode Session:** `ses_e816b9ef8ffeMVX1FeSi525T7i` (Opexgram 13.1.1 beta10 features)
- **Last Updated:** 2026-10-31 18:01:40
- **Last Request:** "### 🎯 Техническое задание: Разработка 12 векторных иконок для бейджей Telegram (100gram)

    #### 1. Контекст проекта
    В форке Telegram для Android (**100gram**, кодовая база `D:\100gram`) реализована система кастомных бейджей для
    пользователей (отображение значков возле никнейма в списке диалогов, в сообщениях чата и в профиле).
    Базовая архитектура бейджей уже написана и закоммичена в `main`:
    - Контроллер: `TMessagesProj/src/main/java/org/telegram/messenger/HundredGramBadgesController.java`
    - Редактор бейджей (BottomSheet):
    `TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramBadgeEditorBottomSheet.java`
    - Просмотр деталей бейджа: `TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramBadgeAlert.java`
    - Отрисовка в UI: `ChatMessageCell.java`, `DialogCell.java`, `ProfileActivity.java`

    #### 2. Что требуется сделать прямо сейчас
    Необходимо спроектировать и создать **12 векторных иконок** в формате **Android VectorDrawable XML** для
    размещения в каталоге:
    `TMessagesProj/src/main/res/drawable/`

    Иконки должны быть выполнены в лаконичном, фирменном минималистичном стиле Telegram (Material/Telegram Design),
    аналогично нативным `filled_rating_crown.xml` и `outline_shield_check.xml`.

    #### 3. Технические требования к VectorDrawable XML
    1. **Размер холста**: `android:width="24dp"`, `android:height="24dp"`, `android:viewportWidth="24"`,
    `android:viewportHeight="24"`.
    2. **Базовый цвет заливки**: `android:fillColor="#FFFFFFFF"` (чистый белый).
       - В рантайме Telegram иконки динамически окрашиваются в любой цвет через:
         `d.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN))`
    3. **Отрицательное пространство (Negative Space)**:
       - Внутренние детали (глаза черепа/робота, грани кристалла, иллюминатор ракеты) должны быть вырезами, чтобы при
       монохромной заливке значок не сливался в сплошное пятно. Используй составные контуры либо
       `android:fillType="evenodd"`.
    4. **Читаемость в микро-размере**:
       - Бейдж отображается в чате при высоте ~14–16dp рядом с текстом имени. Геометрия должна быть чистой, без
       сверхмелких деталей и зазоров тоньше 1dp.
    5. **Файлы**: только чистый векторный XML, без растровых png/webp.

    #### 4. Список 12 требуемых иконок и имена файлов:
    1. `badge_diamond.xml` — **Бриллиант (Diamond)**: ограненный кристалл с четкими гранями через negative space
    (VIP, спонсоры).
    2. `badge_rocket.xml` — **Ракета (Rocket)**: устремленный вверх корпус, иллюминатор, боковые стабилизаторы и
    сопло/пламя (Бустеры, первопроходцы).
    3. `badge_trophy.xml` — **Кубок (Trophy)**: классический кубок с двумя ручками на устойчивом постаменте
    (Победители, чемпионы).
    4. `badge_heart.xml` — **Сердце (Heart)**: аккуратное, гладкое, гармонично скругленное сердце (Семья, близкие
    друзья).
    5. `badge_target.xml` — **Мишень (Bug Hunter)**: концентрические окружности с центральной точкой/перекрестием
    (Багхантеры, QA).
    6. `badge_coffee.xml` — **Кофе (Coffee Cup)**: чашка с ручкой, блюдцем и поднимающимися витками горячего пара
    (Донатеры).
    7. `badge_gamepad.xml` — **Геймпад (Gamepad)**: современный силуэт консольного контроллера, D-pad слева, 4 кнопки
    справа (Геймеры, стримеры).
    8. `badge_skull.xml` — **Череп (Skull / Hacker)**: стильный монохромный череп с крупными глазницами, носовой
    впадиной и зубами (Хакеры, реверсеры, SecOps).
    9. `badge_wand.xml` — **Палочка (Magic / Creator)**: наклонная волшебная палочка с 3–4 искорками-звездочками у
    кончика (Дизайнеры, авторы тем).
    10. `badge_robot.xml` — **Робот (Bot Maker / AI)**: квадратная голова робота со скругленными углами, антенной с
    шариком, круглыми глазами и решеткой рта (Разработчики ботов).
    11. `badge_glasses.xml` — **Очки (Agent / Incognito)**: темные очки в стиле Wayfarer/Agent с переносицей и
    дужками (Инкогнито, тайные агенты, модерация).
    12. `badge_music.xml` — **Нота (Music / Artist)**: сдвоенная восьмая нота с объединяющим горизонтальным
    ребром/штрилем (Музыканты, саунд-продюсеры).

    #### 5. Точки интеграции в коде проекта:
    - `TMessagesProj/src/main/java/org/telegram/messenger/HundredGramBadgesController.java`:
      - Добавить константы `ICON_DIAMOND = 7` ... `ICON_MUSIC = 18`.
      - Замапить их в `getBadgeDrawable()` на созданные `R.drawable.badge_*`.
      - Добавить названия в `getIconTypeName()`.
    - `TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramBadgeEditorBottomSheet.java`:
      - Расширить массив `ICON_TYPES` всеми новыми ID иконок для выбора в палитре.
    - Локализация:
      - `TMessagesProj/src/main/res/values/hundredgram_strings.xml` (EN)
      - `TMessagesProj/src/main/res/values-ru/hundredgram_strings.xml` (RU)
    - Верификация сборки:
      - `.\gradlew.bat :TMessagesProj:packageDebugResources --daemon`
      - `.\gradlew.bat :TMessagesProj_AppStandalone:compileDebugJavaWithJavac --daemon`"
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

### ⏳ Remaining Backlog & Active Work

| Feature | Category | Status / Priority | Key References |
|---|---|---|---|
| **12 Vector Badges Icon Pack** | Assets | 🟡 In Progress | 12 VectorDrawable XML assets (`badge_*.xml`) |
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
