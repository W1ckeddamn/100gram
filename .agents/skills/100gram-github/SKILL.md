---
name: 100gram-github
description: >-
  Workflows, GitHub CLI integration, branch/PR conventions, automated upstream sync
  without breaking C++ NDK sources, and release management for 100gram (Telegram-Android fork).
---

# 100gram GitHub & Repository Synchronization Guide

Регламент работы с GitHub-репозиторием **100gram**, синхронизации с официальным апстримом (Telegram-Android / DrKLO), оформления веток, коммитов, PR, релизов и защиты нативного C++ кода.

---

## 1. Защита от «Ловушки подмодулей C++ NDK» (Submodule Trap Immunity)

### Проблема
Официальный репозиторий Telegram (`DrKLO/Telegram`) перевёл сторонние нативные библиотеки в `TMessagesProj/jni/` (BoringSSL, libyuv, openh264, wamr, absl и др.) на git submodules. При выполнении стандартного `git merge upstream/master` или `git pull upstream` Git удаляет все локальные C++ файлы в `TMessagesProj/jni/`, заменяя их пустыми ссылками на сабмодули, что полностью ломает компиляцию NDK/CMake.

### Решение раз и навсегда (Custom Merge Driver `ours`)

В корне репозитория настроен файл `.gitattributes`:
```gitattributes
# Защита локальных нативных исходников C++ NDK и .gitmodules от удаления апстримом (DrKLO)
TMessagesProj/jni/** merge=ours
.gitmodules merge=ours
```

И активирован драйвер `ours` в конфигурации Git:
```powershell
git config merge.ours.driver true
```

#### Как это работает:
1. При выполнении `git merge upstream/master` или `git pull upstream` Git автоматически обновляет все 5000+ Java/Kotlin-файлов, макетов, XML-ресурсов и ассетов Telegram.
2. Но для любых файлов в `TMessagesProj/jni/**` и `.gitmodules` срабатывает драйвер `merge=ours`: Git безусловно сохраняет **нашу локальную версию файлов**, полностью игнорируя попытки апстрима их удалить или заменить на сабмодули.
3. Процесс обновления до новой версии Telegram занимает 2-3 минуты вместо многочасового ручного перетягивания файлов.

---

## 2. Синхронизация с официальным апстримом Telegram (DrKLO)

### Первичная настройка remote:
```powershell
git remote add upstream https://github.com/DrKLO/Telegram.git
git fetch upstream --tags
```

### Регламент обновления версии:
1. **Убедиться, что рабочая директория чистая:**
   ```powershell
   git status
   ```
2. **Создать ветку для обновления:**
   ```powershell
   git checkout -b sync/upstream-12.x
   ```
3. **Слияние с апстримом (или конкретным тегом релиза DrKLO):**
   ```powershell
   # Проверяем, что merge driver активен
   git config merge.ours.driver true

   # Сливаем релизный тег или master
   git merge upstream/master --no-commit
   ```
4. **Проверка интактности C++ каталога:**
   ```powershell
   # Убеждаемся, что jni не удален и содержит CMakeLists.txt и библиотеки
   Test-Path TMessagesProj/jni/CMakeLists.txt
   Test-Path TMessagesProj/jni/boringssl
   ```
5. **Проверка конфликтов ресурсов и компиляции:**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat :TMessagesProj:packageDebugResources --daemon
   .\gradlew.bat :TMessagesProj:compileDebugJavaWithJavac --daemon
   ```
6. **Завершение слияния и коммит:**
   ```powershell
   git commit -m "sync(upstream): merge Telegram Android <version>"
   ```

---

## 3. Соглашения по коммитам и веткам (Git Conventions)

Используется стандарт **Conventional Commits**:
- `feat(scope):` — Новая функциональность (например, `feat(settings): add bubble preview`).
- `fix(scope):` — Исправление бага (например, `fix(actionbar): center title properly`).
- `sync(upstream):` — Синхронизация с Telegram апстримом.
- `refactor(scope):` — Рефакторинг без изменения внешней логики.
- `docs(scope):` — Изменения в документации или README.

### Основные scopes проекта:
- `settings` — Настройки, карточки, поднастройки (`HundredGramSettingsActivity`, `HundredGramSubSettingsActivity`).
- `ui` — UI-компоненты, ячейки, диалоги, шторки, Action Bar.
- `vault` — Media Vault (SQLite индекс, хранилище, шифрование).
- `tracker` — Activity Tracker (онлайн-мониторинг, сессии).
- `camera` — Камера (CameraX, кружочки, видеоноты).
- `plugins` — Модули и плагины JS/Python.
- `whatsnew` — Экран «Что нового» и журнал изменений.

---

## 4. Работа с GitHub CLI (`gh`)

Для автоматизации взаимодействия с репозиторием используется утилита `gh`:

### Просмотр и создание Issues:
```powershell
# Список открытых задач
gh issue list --limit 20

# Создание задачи с метками
gh issue create --title "fix(actionbar): Title alignment on RTL" --body "Описание проблемы..." --label "bug"
```

### Создание Pull Request:
```powershell
# Создание PR из текущей ветки
gh pr create --title "feat(ui): Custom sticker and gif preview in chat settings" --body "Подробное описание изменений..." --base master
```

### Статус CI и сборок:
```powershell
# Просмотр статуса проверок текущей ветки
gh pr checks

# Просмотр последних GitHub Actions workflow запусков
gh run list --limit 10
```

---

## 5. Выпуск релизов и тегов (Release Workflow)

При подготовке публичного релиза:

1. **Обновить версию в коде:**
   - `TMessagesProj/build.gradle` / `gradle.properties`: увеличить `versionCode` и `versionName`.
2. **Синхронизировать документацию:**
   - Добавить изменения в `CHANGELOG.md` под заголовком `## [vX.Y.Z] - YYYY-MM-DD`.
   - Проверить `FEATURES.md`.
   - Зарегистрировать новую карточку в [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java) согласно регламенту `100gram-whatsnew`.
3. **Собрать релизный APK:**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatRelease -x lint -x test --daemon
   ```
   Файл: `TMessagesProj_AppStandalone\build\outputs\apk\afat\release\app.apk`.
4. **Создать Git Tag и GitHub Release:**
   ```powershell
   git tag -a v1.x.x -m "Release v1.x.x"
   git push origin v1.x.x

   gh release create v1.x.x "TMessagesProj_AppStandalone\build\outputs\apk\afat\release\app.apk#100gram-v1.x.x-arm64-v8a.apk" --title "100gram v1.x.x" --notes-file RELEASE_NOTES.md
   ```
