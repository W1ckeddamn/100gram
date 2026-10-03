---
name: 100gram-changelog
description: >-
  Guidelines, standards, and automation for tracking changes, registering new features,
  and maintaining CHANGELOG.md and FEATURES.md in the 100gram Telegram-Android fork.
  Use when completing tasks, adding new settings, refactoring modules, or preparing releases.
---

# 100gram Changelog & Feature Tracking Guide

Этот навык определяет стандарт ведения истории изменений, регистрации добавленных функций и подготовки релизов в проекте **100gram** (Telegram-Android fork).

---

## 1. Главный принцип: «Definition of Done» (Критерий завершённости)

Любая работа над проектом (новая фича, исправление бага, оптимизация, рефакторинг) **НЕ считается завершённой**, пока не обновлена соответствующая документация:

1. **Если исправлен баг или изменено поведение:**
   - Добавь лаконичную запись в [`CHANGELOG.md`](file:///D:/100gram/CHANGELOG.md) в секцию `[Unreleased]`.
2. **Если добавлена новая функция или UI-переключатель:**
   - Добавь пункт в [`CHANGELOG.md`](file:///D:/100gram/CHANGELOG.md) в `[Unreleased] -> ### Added`.
   - Зарегистрируй функцию в каталоге [`FEATURES.md`](file:///D:/100gram/FEATURES.md) с указанием ключа конфигурации (`HundredGramConfig`), экрана настроек и ключевых классов.
3. **Если вырезан сторонний сервис, реклама или трекер:**
   - Добавь пункт в `[Unreleased] -> ### Removed` или `### Security`.

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
Каждая запись должна предваряться квадратными скобками с указанием подсистемы:
- `[Tracker]` — трекер онлайна, сессии активности, экспорт активности.
- `[Vault]` — скрытое медиахранилище, SQLite база, хардлинки.
- `[Camera]` — CameraX, круглые видео, квадратные кружки, переключение камер.
- `[Network]` — Network Boost, сокетные тюнинги, пулы потоков.
- `[Billing]` — конвертация валют, звезды Telegram, подарки, Fragment API.
- `[UI]` — темы, Monet, шрифты, контекстные меню, диалоги.
- `[Core]` — база данных, MTProto, фоновые службы, общие конфиги.
- `[Build]` — Gradle, NDK/CMake, ProGuard, скрипты сборки.

### Пример записи в `CHANGELOG.md`:
```markdown
## [Unreleased]

### Added
- [Tracker] Добавлен переключатель «Отслеживать активность» в профиле пользователя (`ProfileActivity`).
- [Vault] Поддержка автоматического перемещения медиа в изолированное хранилище без дублирования на диске (hardlinks).

### Fixed
- [Tracker] Исправлен подсчет суммарного времени за день: устранены сессии с `end_time = 0` благодаря таймауту автозакрытия 90 секунд.
- [Billing] Устранены подмены текста в ценах подарков: все карточки переведены на нативный `BillingController`.
```

---

## 3. Стандарт ведения реестра функций `FEATURES.md`

[`FEATURES.md`](file:///D:/100gram/FEATURES.md) является полной спецификацией всех кастомных возможностей форка 100gram.

При добавлении новой фичи добавь запись следующего формата:

```markdown
### [Название функции на русском] (English Name)
- **Описание:** Что делает эта функция для конечного пользователя.
- **Где включается (UI):** `Настройки 100gram` -> `Подраздел` -> `Название тумблера`.
- **Ключ конфигурации:** `HundredGramConfig.SOME_SETTING_KEY` (тип `boolean`/`int`).
- **Ключевые классы:** `HundredGramSomeManager.java`, `SomeActivity.java`.
- **Статус:** `В релизе` / `Экспериментально` / `В разработке`.
```

---

## 4. Скрипт автоматизации `changelog-helper.ps1`

В директории `.agents/skills/100gram-changelog/scripts/` находится утилита автоматизации:

```powershell
# 1. Посмотреть свежие коммиты и сформировать сводку для CHANGELOG.md:
powershell -ExecutionPolicy Bypass -File .agents/skills/100gram-changelog/scripts/changelog-helper.ps1 -Action Summary

# 2. Проверить валидность CHANGELOG.md и наличие незакрытых тегов:
powershell -ExecutionPolicy Bypass -File .agents/skills/100gram-changelog/scripts/changelog-helper.ps1 -Action Validate

# 3. Закрыть текущий релиз и перенести [Unreleased] в новую версию:
powershell -ExecutionPolicy Bypass -File .agents/skills/100gram-changelog/scripts/changelog-helper.ps1 -Action Release -Version "12.10.5-v1.0"
```

---

## 5. Чеклист для агентов перед коммитом
- [ ] Отражены ли все изменения в `CHANGELOG.md`?
- [ ] Использованы ли корректные теги подсистем (`[Tracker]`, `[Vault]` и т.д.)?
- [ ] Если добавлен новый тумблер или экран — внесен ли он в `FEATURES.md`?
- [ ] Не затронуты ли посторонние файлы и нет ли лишнего мусора в `git status`?
