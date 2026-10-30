# 📋 Дорожная карта реализации и интеграции (100gram)

Все этапы выполняются со строгим соблюдением **«Контракта фичи из 6 пунктов»**:
1. Переменная, геттер, сеттер в `HundredGramConfig.java` (дефолт всегда ванильный, `false` / `0`).
2. Строка управления в `HundredGramSubSettingsActivity.java` или отдельный экран.
3. Локализация в `hundredgram_strings.xml` (EN) и `hundredgram_strings_ru.xml` (RU).
4. Регистрация в `HundredGramSettingsSearchIndex.java`.
5. Поддержка в `HundredGramBackupManager.java` (экспорт/импорт).
6. Сброс к ванильному дефолту в `HundredGramConfig.resetAllSettings()`.

---

## 🗺️ Обзор этапов

| Этап | Направление | Ключевые фичи | Источник | Статус |
|:---:|---|---|:---:|:---:|
| **Этап 1** | **Дизайн и UI** | **Material Design 3 (MD3) Suite**: Мастер-переключатель + ChatList, Sections, Controls, Menus, Navigation, MediaLoader | OpexGram | 🟡 **В работе** |
| **Этап 2** | **Интерфейс** | **Глобальный движок ползунков (Sliders Engine)**: Стили Classic / Expressive + геометрия трека, бегунка, зазора и точек | OpexGram (`kc.z2`) | ⚪ Ожидает |
| **Этап 3** | **Чаты и утилиты** | **Отправка видео как кружочков** + **Секретный просмотр таймерных медиа** + **Владелец стикерпака** + **Фикс-ссылки** (fxtwitter/ddinstagram) | OpexGram / exteraGram | ⚪ Ожидает |
| **Этап 4** | **Продвинутый визуал** | **Стеклянные пузыри (Glass bubbles)** + **Стили поля ввода (Expressive Input Box)** + **Стикер-цитаты (Quotes)** | OpexGram (`lc.a2`, `kc.e2`) | ⚪ Ожидает |
| **Этап 5** | **Медиа и плеер** | **Тексты песен (Lyrics)** + **MD3-плеер с волной** + **Очередь Slowmode** + **Regex-поиск** | OpexGram / exteraGram | ⚪ Ожидает |

---

## 🛠️ Детализация Этапа 1 (Текущий шаг): Material Design 3 (MD3) Suite

### 1. Конфигурация в `HundredGramConfig.java`:
* `md3MasterEnabled` (boolean, false) — главный мастер-переключатель Material 3.
* `md3ChatList` (boolean, false) — стиль списка чатов (скругление карточек диалогов).
* `md3Sections` (boolean, false) — оформление блоков и секций настроек.
* `md3Controls` (boolean, false) — кнопки, чекбоксы и чипы.
* `md3Menus` (boolean, false) — скруглённые плавающие контекстные меню.
* `md3NavigationStyle` (int, 0) — стиль нижней панели: 0 = Classic, 1 = Floating Pill, 2 = Expressive.
* `md3MediaLoader` (boolean, false) — круглый индикатор загрузки с эффектом волны.
* `md3MediaLoaderThickness` (int, 4) — толщина кольца загрузчика (2..8 dp).
* `md3MediaLoaderWave` (boolean, false) — волновая анимация кольца.

### 2. Экран настроек:
* Добавление секции «Material Design 3» в `HundredGramSubSettingsActivity` (раздел «Интерфейс») с интерактивными переключателями.

### 3. Хуки применения:
* Связка с `Theme.java`, `ActionBar`, стилями меню и списком чатов.
