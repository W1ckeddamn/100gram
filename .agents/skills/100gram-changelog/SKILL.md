---
name: 100gram-changelog
description: >-
  Guidelines, standards, and automation for tracking changes, registering new features,
  and maintaining CHANGELOG.md, FEATURES.md, and in-app What's New screen (HundredGramWhatsNewActivity)
  in the 100gram Telegram-Android fork. Use when completing tasks, adding new settings, refactoring modules,
  or preparing releases.
---

# 100gram Changelog, Features & In-App Updates Guide

Этот навык определяет стандарт ведения истории изменений, каталога функций, экрана обновлений в интерфейсе клиента и подготовки релизов в проекте **100gram** (Telegram-Android fork).

---

## 1. Главный принцип: «Definition of Done» (Критерий завершённости)

Любая работа над проектом (новая фича, исправление бага, оптимизация, рефакторинг) **НЕ считается завершённой**, пока не синхронизирована документация и экран обновлений:

1. **Если исправлен баг или изменено поведение:**
   - Добавь лаконичную запись в [`CHANGELOG.md`](file:///D:/100gram/CHANGELOG.md) в секцию `[Unreleased]`.
2. **Если добавлена новая функция, настройка или UI-переключатель:**
   - Добавь пункт в [`CHANGELOG.md`](file:///D:/100gram/CHANGELOG.md) в секцию `[Unreleased] -> ### Added`.
   - Зарегистрируй функцию в каталоге [`FEATURES.md`](file:///D:/100gram/FEATURES.md) с указанием ключа конфигурации (`HundredGramConfig`), пути в меню настроек и ключевых классов.
   - **Добавь информацию о функции во внутриклиентский экран обновлений** [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java) в актуальную версию.
3. **Если вырезан сторонний сервис, реклама или трекер:**
   - Добавь пункт в [`CHANGELOG.md`](file:///D:/100gram/CHANGELOG.md) в секцию `[Unreleased] -> ### Removed` или `### Security`.
4. **Атрибуция автора и репозитория:**
   - Все ссылки на автора и репозиторий проекта должны оставаться актуальными:
     - **Автор:** `@w1ckeddamn` (`https://t.me/w1ckeddamn`)
     - **GitHub репозиторий:** `https://github.com/W1ckeddamn/100gram`
     - **Релизы GitHub:** `https://github.com/W1ckeddamn/100gram/releases`
   - Они отображаются в `Настройки 100gram` -> блок «О приложении» и на экране «Что нового».

---

## 2. Стандарт структуры `CHANGELOG.md`

`CHANGELOG.md` ведётся по стандарту [Keep a Changelog](https://keepachangelog.com/ru/1.0.0/) и использует семантическое версионирование (`MAJOR.MINOR.PATCH`, привязанное к Telegram core, например `12.10.5-v1.0`).

### Стандартные категории секции:
- **`### Added`** — новые функции, настройки, экраны интерфейса.
- **`### Changed`** — изменения в существующей логике или поведении клиента.
- **`### Fixed`** — устранение багов, утечек памяти, сбоев UI, некорректных расчётов.
- **`### Security`** — функции защиты (PIN, Duress, обход цензуры, изоляция хранилища).
- **`### Removed`** — удаление устаревших компонентов, библиотек, трекеров, рекламы.

### Обязательные теги компонентов:
Каждая запись предваряется квадратными скобками с указанием подсистемы:
- `[Tracker]` — трекер онлайна, сессии активности, экспорт активности.
- `[Vault]` — скрытое медиахранилище, SQLite база, хардлинки.
- `[Camera]` — CameraX, круглые видео, квадратные кружки, отправка видео кружком из галереи.
- `[Network]` — Network Boost, сокетные тюнинги, пулы потоков.
- `[Billing]` — конвертация валют, звезды Telegram, подарки, Fragment API.
- `[UI]` — темы, Monet, шрифты, контекстные меню, размеры стикеров/GIF, диалоги.
- `[Core]` — база данных, MTProto (Layer 229), фоновые службы, общие конфиги.
- `[Build]` — Gradle, NDK/CMake, ProGuard, скрипты сборки.

### Пример записи в `CHANGELOG.md`:
```markdown
## [Unreleased]

### Added
- [UI/Media] Настройки размера отображения стикеров и GIF-анимаций в чатах (5 градаций от 50% до 150%).
- [Camera/UI] Отправка обычных видео из галереи в виде круглых видеосообщений (кружочков).
- [UI] Экран «Что нового» (HundredGramWhatsNewActivity) и блок «О приложении» с ссылками на автора @w1ckeddamn и GitHub.

### Fixed
- [Core/MTProto] Синхронизированы недостающие конструкторы Layer 229 в TL_iv.java и TL_update.java.
```

---

## 3. Стандарт ведения реестра функций `FEATURES.md`

[`FEATURES.md`](file:///D:/100gram/FEATURES.md) является полной спецификацией всех кастомных возможностей форка 100gram.

При добавлении новой фичи добавь запись следующего формата:

```markdown
### [Название функции на русском] (English Name)
- **Описание:** Что делает эта функция для конечного пользователя.
- **Где включается (UI):** `Настройки 100gram` -> `Подраздел` -> `Название тумблера/пункта`.
- **Ключ конфигурации:** `HundredGramConfig.SOME_SETTING_KEY` (тип `boolean`/`int`).
- **Ключевые классы:** `HundredGramSomeManager.java`, `SomeActivity.java`.
- **Статус:** `В релизе` / `Экспериментально` / `В разработке`.
```

---

## 4. Внутриклиентский экран «Что нового» (`HundredGramWhatsNewActivity`)

Экран [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java) доступен прямо из меню «Настройки 100gram» (`HundredGramSettingsActivity`) через пункт **«Что нового»**.

### Когда обновлять `HundredGramWhatsNewActivity.java`:
- При добавлении заметных для пользователя фич (новые экраны, тумблеры, режимы медиа/камеры/трекера).
- При подготовке новой версии или крупного апдейта.

### Структура файла и правила редактирования:
1. Экран построен на базе `UniversalRecyclerView` и адаптера `UniversalAdapter`.
2. Элементы формируются в методе `fillItems(ArrayList<UItem> items, UniversalAdapter adapter)`:
   - **Заголовок версии:**
     ```java
     items.add(UItem.asHeader("Версия 1.x — Название темы релиза"));
     ```
   - **Карточка новшества:**
     ```java
     items.add(UItem.asButton(ITEM_CUSTOM_ACTION, "Краткое название фичи", "Подробное описание того, что изменилось и где найти")
             .setIcon(R.drawable.msg_arrowforward));
     ```
   - **Разделитель между версиями:**
     ```java
     items.add(UItem.asShadow(null));
     ```
3. **Обработка нажатий в `onClick`:**
   - Если элемент ведет в настройки клиента — открывать соответствующий экран (например, `HundredGramSubSettingsActivity` с нужным подразделом).
   - Если элемент информационный — клик может переводить в профильные настройки или открывать веб-ссылку (`Browser.openUrl(getParentActivity(), url)`).
4. **Блок ссылок проекта:**
   В конце списка всегда должен присутствовать блок «О проекте»:
   - Кнопка **«Релизы и APK (GitHub)»** -> `https://github.com/W1ckeddamn/100gram/releases`
   - Кнопка **«Исходный код (GitHub)»** -> `https://github.com/W1ckeddamn/100gram`
   - Кнопка **«Автор: @w1ckeddamn»** -> `https://t.me/w1ckeddamn`

---

## 5. Интеграция в меню настроек (`HundredGramSettingsActivity`)

Главный экран настроек форка [`HundredGramSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java) содержит секцию **«О приложении»**:

```java
// Секция "О приложении"
items.add(UItem.asHeader(LocaleController.getString("HundredGramAboutSection", R.string.HundredGramAboutSection)));
items.add(UItem.asButton(ITEM_WHATS_NEW,
        LocaleController.getString("HundredGramWhatsNew", R.string.HundredGramWhatsNew),
        LocaleController.getString("HundredGramWhatsNewSub", R.string.HundredGramWhatsNewSub))
        .setIcon(R.drawable.msg_info));
items.add(UItem.asButton(ITEM_AUTHOR,
        LocaleController.getString("HundredGramAuthor", R.string.HundredGramAuthor),
        LocaleController.getString("HundredGramAuthorSub", R.string.HundredGramAuthorSub))
        .setIcon(R.drawable.msg_contact));
items.add(UItem.asButton(ITEM_GITHUB,
        LocaleController.getString("HundredGramGitHub", R.string.HundredGramGitHub),
        LocaleController.getString("HundredGramGitHubSub", R.string.HundredGramGitHubSub))
        .setIcon(R.drawable.msg_open));
```

При переходе по пункту `ITEM_WHATS_NEW` вызывается:
```java
presentFragment(new HundredGramWhatsNewActivity());
```

---

## 6. Скрипт автоматизации `changelog-helper.ps1`

В каталоге `.agents/skills/100gram-changelog/scripts/` находится утилита автоматизации:

```powershell
# 1. Посмотреть свежие коммиты и сформировать сводку для CHANGELOG.md:
powershell -ExecutionPolicy Bypass -File .agents/skills/100gram-changelog/scripts/changelog-helper.ps1 -Action Summary

# 2. Проверить валидность CHANGELOG.md и наличие незакрытых тегов:
powershell -ExecutionPolicy Bypass -File .agents/skills/100gram-changelog/scripts/changelog-helper.ps1 -Action Validate

# 3. Закрыть текущий релиз и перенести [Unreleased] в новую версию:
powershell -ExecutionPolicy Bypass -File .agents/skills/100gram-changelog/scripts/changelog-helper.ps1 -Action Release -Version "12.10.6-v1.2"
```

---

## 7. Чеклист перед коммитом и завершением задачи

- [ ] Внесены ли изменения в [`CHANGELOG.md`](file:///D:/100gram/CHANGELOG.md) в секцию `[Unreleased]`?
- [ ] Использованы ли корректные теги подсистем (`[UI]`, `[Camera]`, `[Tracker]`, `[Vault]`, `[Core]`)?
- [ ] Если добавлен новый тумблер или экран — внесен ли он в [`FEATURES.md`](file:///D:/100gram/FEATURES.md)?
- [ ] Если добавлены пользовательские функции — обновлен ли экран [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java) по регламенту [`100gram-whatsnew`](file:///D:/100gram/.agents/skills/100gram-whatsnew/SKILL.md)?
- [ ] Сохранены ли ссылки на автора `@w1ckeddamn` и репозиторий `https://github.com/W1ckeddamn/100gram`?
- [ ] Проверена ли компиляция проекта через `compileAfatDebugJavaWithJavac`?
