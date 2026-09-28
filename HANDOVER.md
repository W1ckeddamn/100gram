# 100gram (Telegraher) — Руководство по передаче проекта (Handover Guide)

> **Для нового разработчика / сопровождающего проекта**  
> Документ содержит полную информацию об архитектуре, структуре веток, сборке, ключевых модулях и критических нюансах сопровождения форка 100gram.

---

## 1. Обзор проекта

- **Название проекта**: 100gram (ранее Telegraher)
- **База**: Официальный клиент [Telegram for Android](https://github.com/DrKLO/Telegram)
- **Текущая версия Telegram**: **12.10.5** (код версии: `7105010`, build `7105`)
- **Стек технологий**: Java 8/11/17, Kotlin, C++20 (Android NDK), CMake 3.22.1, Gradle 8.14.5, Android Gradle Plugin 8.8+
- **Минимальная версия Android**: Android 6.0 (API 23)
- **Целевая версия Android**: Android 15 (API 35)
- **Поддерживаемые ABI**: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` (мульти-архитектурный бандл `afat`)

---

## 2. Структура веток в Git

| Ветка | Назначение |
|---|---|
| **`upgrade-12.10.5`** / **`graher_12.10.5`** | **Основная рабочая ветка.** Содержит обновленную базу Telegram 12.10.5, все кастомные системы (Network Boost, Camera Engine, Media Vault, Streamer, Ghost Mode) и проверена сборкой. |
| **`backup-12.92-full`** | Полный резервный снимок кодовой базы до обновления до 12.10.5 (коммит `99917ae1f`). |
| **`graher_12.92.1`** | Исходная ветка предыдущего релиза. |
| **`upstream/master`** | Удаленный репозиторий официального Telegram (`https://github.com/DrKLO/Telegram.git`). |

---

## 3. Архитектура и карта кодовой базы

Все кастомные функции форка изолированы и аккуратно встроены в ядро Telegram через контроллеры и хуки:

### 3.1. Центральная конфигурация
- **[`HundredGramConfig.java`](TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java)**
  - Хранит все флаги и настройки в `SharedPreferences ("hundredgram_config")`.
  - Предоставляет методы `loadConfig()`, `save()`, `saveInt()`, `saveFloat()`.
  - Синхронизирует системные параметры Telegram (например, `SharedConfig.useCamera2Force`, `SharedConfig.roundCamera16to9`).

### 3.2. Пользовательский интерфейс и экраны настроек
- **[`HundredGramSettingsActivity.java`](TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java)**: главный экран настроек форка с категоризацией, поиском и бейджами.
- **[`HundredGramCameraActivity.java`](TMessagesProj/src/main/java/org/telegram/ui/HundredGramCameraActivity.java)**: расширенные настройки камеры (диагностика возможностей сенсора, CameraX, выбор разрешения, 60 FPS, квадратные видеосообщения, битрейт, формат сенсора).
- **[`HundredGramSubSettingsActivity.java`](TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java)**: подэкраны детальных настроек (Сейф медиа, Приватность, Интерфейс, Автоматизация).
- **[`HundredGramVaultViewerActivity.java`](TMessagesProj/src/main/java/org/telegram/ui/HundredGramVaultViewerActivity.java)**: чистый интерфейс просмотра сохраненных медиафайлов Сейфа с поиском, чипами фильтрации (`[Все] [Архивные] [Личные] [Группы]`) и контекстным меню.
- **[`HundredGramRulesActivity.java`](TMessagesProj/src/main/java/org/telegram/ui/HundredGramRulesActivity.java)**: конструктор правил автоматизации сообщений.

---

### 3.3. Ключевые системы и их реализация

#### ⚡ Сетевой ускоритель (Network Boost)
- **Контроллер**: [`HundredGramNetworkBoostController.java`](TMessagesProj/src/main/java/org/telegram/messenger/HundredGramNetworkBoostController.java)
- **Хуки**:
  - [`FileLoadOperation.java`](TMessagesProj/src/main/java/org/telegram/messenger/FileLoadOperation.java): параллельная загрузка до 8 потоков MTProto для скачивания больших файлов.
  - [`FileUploadOperation.java`](TMessagesProj/src/main/java/org/telegram/messenger/FileUploadOperation.java): параллельная выгрузка чанков с адаптивным режимом по размеру файла (<10 МБ, 10–100 МБ, >100 МБ).
- **Мониторинг**: встроенный спидометр в реальном времени, подсчет активных сокетов и встроенный тест скорости/пинга до DC Telegram.

#### 📷 Advanced Camera & Video Notes Engine
- **Поддерживаемые движки**:
  1. `CAMERA_ENGINE_SYSTEM (0)`: системная логика Telegram;
  2. `CAMERA_ENGINE_CAMERAX (1)`: Google Jetpack CameraX ([`CameraXUtils.java`](TMessagesProj/src/main/java/org/telegram/messenger/camera/camerax/CameraXUtils.java)) с поддержкой 60 FPS, ультраширокоугольного объектива (0.6x) и плавного зум-слайдера;
  3. `CAMERA_ENGINE_CAMERA2 (2)`: принудительный Camera2 API;
  4. `CAMERA_ENGINE_CAMERA1_LEGACY (3)`: устаревший Camera1 API для слабых и старых чипсетов.
- **Квадратные видеосообщения («Квадратные кружочки»)**:
  - Видоискатель ([`InstantCameraView.java`](TMessagesProj/src/main/java/org/telegram/ui/Components/InstantCameraView.java)): переключение контура на `outline.setRoundRect(..., AndroidUtilities.dp(16))`.
  - Отображение в чате ([`ChatMessageCell.java`](TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java)): маска `photoImage.setRoundRadius(AndroidUtilities.dp(16))`.
- **Отключение фонового размытия при записи**:
  - [`ChatActivity.java`](TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java#L46585): скрытие `roundVideoRecordBackground` во время записи кружка.
- **Ступенчатый битрейт**:
  - Пресеты: Авто, 750 Кбит/с, 1.2 Мбит/с, 2.0 Мбит/с, 3.0 Мбит/с передаются в кодек видео `MediaFormat.KEY_BIT_RATE` в `TextureMovieEncoder`.
- **Формат сенсора (16:9 vs 4:3)**:
  - `HundredGramConfig.cameraRoundSensor16to9` управляет `SharedConfig.roundCamera16to9` для корректного отображения без искажений на сенсорах 4:3.

#### 🗄️ Сейф медиа (Media Vault) и Фоновый архиватор
- **Контроллер**: [`HundredGramVaultController.java`](TMessagesProj/src/main/java/org/telegram/messenger/HundredGramVaultController.java)
- **Фоновый архиватор**: [`HundredGramBackgroundArchiver.java`](TMessagesProj/src/main/java/org/telegram/messenger/HundredGramBackgroundArchiver.java)
- **Возможности**:
  - Сканирует локальную базу SQLite Telegram (`SELECT did, folder_id FROM dialogs`) для охвата 100% диалогов устройства, включая архивные папки.
  - Автоскачивание медиа (фото, видео, голосовые, документы) с фильтрацией по объему (до N МБ) и типу сети (Wi-Fi/любая).
  - Авторотация: TTL (срок хранения 7–90 дней) и квота диска (2–20 ГБ) с защитой выбранных чатов (`vaultProtectedDialogs`).

#### 🎭 Режим стримера (Streamer Mode)
- **Защита экрана**: установка флага окна `WindowManager.LayoutParams.FLAG_SECURE` — делает экран полностью черным в Discord, OBS, при захвате экрана и блокирует системные скриншоты.
- **Маскирование текста**: в [`DialogCell.java`](TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java) мгновенно заменяет текст на `••••••••••`.
- **Защита профиля**: в [`SettingsActivity.java`](TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java) скрывает аватарку, номер, юзернейм и подставляет фейковое имя стримера.

#### 👻 Режим невидимки (Ghost Mode)
- **Нечиталка**: перехват исходящих пакетов прочтения сообщений в `MessagesController.java` (`sendReadMessages`).
- **Скрытные истории**: просмотр чужих Stories без отправки отчета на сервер (`TL_stories_readStories`).
- **Скрытие набора текста**: подавление статусов тайпинга и записи аудио/видео.

#### 🤖 Автоматизация (Automation Engine)
- **Контроллер**: [`AutomationController.java`](TMessagesProj/src/main/java/org/telegram/messenger/AutomationController.java)
- Правила по ключевым словам и типам медиа: автосохранение в «Избранное», сохранение в галерею смартфона, автопересылка в целевой чат.

#### 🛰️ Keep-Alive Foreground Service
- **Сервис**: [`HundredGramKeepAliveService.java`](TMessagesProj/src/main/java/org/telegram/messenger/HundredGramKeepAliveService.java)
- Предотвращает засыпание процессов приложения на агрессивных прошивках (Xiaomi MIUI/HyperOS, Huawei EMUI, Samsung OneUI).

---

## 4. Инструкция по сборке проекта

### 4.1. Системные требования
- **ОС**: Windows 10/11, Linux (Ubuntu 22.04+), macOS
- **JDK**: Java Development Kit **17** (рекомендуется Eclipse Temurin 17 или OpenJDK 17)
- **Android SDK**: Build-Tools `35.0.0`, Compile SDK `35`, Min SDK `23`
- **Android NDK**: версия `26.1.10909125` (или совместимая 27+)
- **CMake**: `3.22.1`

### 4.2. Настройка `local.properties`
В корне проекта создайте/проверьте файл `local.properties`:
```properties
sdk.dir=C\:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
ndk.dir=C\:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125
```

### 4.3. Сборка отладочной версии (Debug APK)
Отладочная сборка компилируется быстро, без минификации ProGuard/R8, и содержит все отладочные символы:
```powershell
.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatDebug -x lint -x test --daemon
```
**Выходной файл**:
`TMessagesProj_AppStandalone\build\outputs\apk\afat\debug\app.apk` (~124 МБ)

### 4.4. Сборка релизной версии (Release APK)
Релизная сборка оптимизируется через R8/ProGuard и сжимается:
```powershell
.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatRelease -x lint -x test --daemon
```
**Выходной файл**:
`TMessagesProj_AppStandalone\build\outputs\apk\afat\release\app.apk` (~80 МБ)

---

## 5. ⚠️ КРИТИЧЕСКИЕ ПРЕДОСТЕРЕЖЕНИЯ (Обязательно к прочтению!)

### 1. Ловушка подмодулей C++ NDK (The Git Submodule Trap)
В апстриме Telegram (DrKLO) начиная с веток 12.10.x разработчики удалили встроенные C++ исходники `third_party` (BoringSSL, libyuv, openh264, wamr, absl) и перевели их на внешние git submodules.
> [!CAUTION]
> **НИКОГДА не выполняйте `git merge upstream/master` или `git pull upstream` вслепую!**  
> Это удалит локальные файлы JNI/C++ и полностью сломает сборку CMake на Windows/Linux.  
> **Правильный способ обновления из апстрима:** переносить только необходимые Java-классы (`TMessagesProj/src/main/java/...`) и ресурсы (`strings.xml`), сохраняя C++ исходники форка в директории `TMessagesProj/jni/`.

### 2. Упаковка ресурсов и дубликаты строк
Перед полной сборкой рекомендуется проверять упаковку ресурсов:
```powershell
.\gradlew.bat :TMessagesProj:packageDebugResources --daemon
```
Если имя строки объявлено одновременно в `strings.xml` и в `hundredgram_strings.xml`, Gradle выдаст ошибку `Duplicate resources`. Кастомные строки форка всегда должны иметь уникальные имена или добавляться только в один из файлов.

### 3. Подпись релизных APK
Для подписи релизов пароли и алиас берутся из переменных окружения либо `local.properties`:
- `RELEASE_KEY_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_STORE_PASSWORD`
В `build.gradle` настроена автоматическая fallback-генерация дебажного сертификата, если релизный ключ не задан.

---

## 6. Чеклист для передачи

- [x] Кодовая база обновлена до актуального Telegram 12.10.5 (Build 7105).
- [x] Все нативные библиотеки C++ компилируются без ошибок под 4 архитектуры.
- [x] Отладочный APK (`assembleAfatDebug`) успешно собран: `100gram-debug.apk`.
- [x] Релизный APK (`assembleAfatRelease`) проверен: `100gram-release.apk`.
- [x] Git-дерево очищено от временных файлов, мусора и кэшей.
- [x] Ветка `graher_12.10.5` зафиксирована и готова к пушу в origin.
- [x] Подготовлена документация `HANDOVER.md` и обновлен `README.md`.
