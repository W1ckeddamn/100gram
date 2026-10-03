# Журнал изменений (100gram Changelog)

Все заметные изменения в проекте **100gram** (кастомный форк Telegram для Android) документируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.0.0/), и проект следует принципам семантического версионирования в связке с апстрим-версией Telegram: `[ВЕРСИЯ_TELEGRAM-vX.Y.Z]`.

---

## [Unreleased]

### Added
- [Tracker] Добавлен переключатель «Отслеживать активность» (`TextCheckCell`) в профиле любого пользователя (`ProfileActivity`) для быстрого включения мониторинга онлайна.
- [Tracker] Создан экран управления отслеживаемыми контактами [`HundredGramTrackedUsersActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramTrackedUsersActivity.java): список пользователей с аватарками и статусами, выбор любых личных чатов (включая не-контакты) через `DialogsActivity`, удаление и переход к журналу активности.
- [Tracker] Добавлена поддержка опроса статусов не-контактов и отслеживаемых пользователей через MTProto `users.getUsers` (`TLRPC.TL_users_getUsers`) в `HundredGramActivityTracker`.
- [Docs] Набор руководств и навыков для ИИ-агентов в каталоге `.agents/skills/` (`100gram-architect`, `100gram-build`, `100gram-tracker-vault`, `100gram-ui-components`, `100gram-changelog`).
- [Automation] Скрипт управления версиями и релизами `.agents/skills/100gram-changelog/scripts/changelog-helper.ps1`.

### Fixed
- [Tracker] Исправлен подсчёт времени сессий и суммарного времени за день: внедрён таймаут автозакрытия висячих сессий (90 секунд), устранены сессии с `end_time = 0`, стандартизированы секунды Unix-времени и форматирование в UI (`X мин` / `X ч Y мин`).

### Changed
- [Git] Нормализация репозитория: переключение рабочей ветки на `main`, удаление устаревших веток (`backup-12.92-full`, `graher_12.92.1`, `upgrade-12.10.5`).
- [Git] Строгая настройка `.gitignore` для NDK/C++ артефактов (`.cxx`, `*.so`), Android Studio, секретов и APK.
- [Docs] Полный редизайн `README.md` с актуальной архитектурой и инструкцией по сборке.

---

## [12.10.5-v1.0.0] - 2026-10-03

### Added
- [Vault] **HundredGram Media Vault**: Защищённое изолированное медиахранилище (`HundredGramVaultManager`, `HundredGramVaultViewerActivity`).
  - Автоматическое скрытие и организация фото, видео и документов.
  - Индексация медиафайлов в выделенной базе SQLite.
  - Поддержка жестких ссылок (hardlinks) файловой системы для исключения дублирования дискового пространства.
  - Фоновый ротатор хранилища (`HundredGramVaultRotator`).
- [Tracker] **HundredGram Activity Tracker**: Фоновый трекер присутствия и активности пользователей (`HundredGramActivityTracker`).
  - Фиксация переходов в онлайн/оффлайн через апдейты MTProto.
  - Детальный журнал заходов в BottomSheet (`HundredGramUserActivityBottomSheet`).
  - Автоматическое закрытие зависших сессий (таймаут 90 секунд) для предотвращения бесконечного онлайна.
- [Camera] **CameraX & Square Video Notes**: Модуль улучшенной видеозаписи (`RoundVideoSession`, `RoundVideoGlProcessor`).
  - Запись видеосообщений («кружков») в повышенном битрейте и 60 FPS.
  - Поддержка квадратных видеосообщений.
  - Быстрое переключение между фронтальной и основной камерой на лету во время записи (`RoundVideoCameraController`).
- [Network] **HundredGram Network Boost**: Модуль оптимизации соединения (`HundredGramNetworkBoostActivity`).
  - Увеличенные сокетные буферы приема и передачи.
  - Параллельные потоки загрузки тяжелых медиа.
- [UI] **История изменений и аватарок**:
  - `HundredGramEditHistoryBottomSheet` — просмотр истории редакций сообщений.
  - `HundredGramAvatarHistoryActivity` — сохранение и просмотр всех ранее установленных аватарок пользователя.
- [UI] **Правила автоматизации**:
  - `HundredGramAutomationActivity` и `HundredGramAutomationEngine` — гибкая настройка автоматических действий по событиям.
- [UI] Современные WebP иконки приложения высокой четкости (от mdpi до xxxhdpi).

### Fixed
- [Billing] Восстановлен нативный `BillingController.java`:
  - Полная поддержка форматирования цен для Telegram Stars (XTR), TON и фиатных валют без внешних сервисов Google Play.
- [Billing] **Полная зачистка текстовых инъекций и пасхалок**:
  - Устранены несанкционированные подмены валют ("Slava Ukraini", "Free Palestine") в 21 UI-классе подарков, розыгрышей, каналов и подписок.
- [Core] Восстановлены официальные телефонные префиксы для Израиля (+972) и России (+7) в `countries.txt`.
- [Build] Исправлены конфликты CMake и NDK r27c при компиляции C++ библиотек на Windows.

### Removed
- [Security] Полностью удалены нежелательные сторонние SDK:
  - Google Play Services, Google Play Billing, Google MLKit / Vision, Google Wallet, SafetyNet, Firebase/FCM.
  - Huawei Mobile Services (HMS / AppGallery).
  - Stripe SDK.

---

## [12.9.2] - 2026-08-29

### Added
- [Security] **Duress PIN (Тревожный PIN-код / KABOOM)**:
  - Экстренное стирание всех пользовательских данных, ключей и сессий (`ActivityManager.clearApplicationUserData()`) при вводе тревожного кода.
  - Авто-уничтожение данных после 10 последовательных ошибок ввода PIN.
  - Перемешивание раскладки цифровой клавиатуры (Scramble PIN layout).
- [Security] **Anti-Restrictions & Bypasses**:
  - Снятие запрета на копирование и пересылку (`noforwards`) в каналах и приватных чатах.
  - Полное удаление `FLAG_SECURE`: скриншоты и захват видео разрешены во всех разделах приложения, включая секретные чаты, самоуничтожающиеся фото и истории.
  - Сохранение самоуничтожающихся TTL-медиа с расшифровкой из кэша AES-CTR.
- [Security] **Anti-Delete**:
  - Сохранение удалённых собеседником сообщений с маркировкой удаления.
- [Core] **Неограниченный мультиаккаунт**:
  - Снятие лимита в 3 аккаунта (динамический пул в `SharedConfig`).
  - Поддержка нескольких одновременных авторизаций на один номер.
- [UI] Поддержка динамических системных тем Android 12+ Material You (Monet Light, Monet Dark, Monet AMOLED).
- [UI] Увеличение разрешения отправляемых фото (до 3840px), расширенный размер шрифта чатов (до 72pt).
- [Removed] 100% блокировка рекламы и спонсорских постов Telegram Ads.
