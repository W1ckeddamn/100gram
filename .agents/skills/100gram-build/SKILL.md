---
name: 100gram-build
description: >-
  Build, compilation, and environment guide for 100gram (Telegram-Android) on Windows.
  Use when compiling modules, assembling debug or release APKs, configuring NDK/CMake,
  or diagnosing Gradle build failures.
---

# 100gram Telegram-Android Build & Compilation Guide

Справочник по сборке, компиляции и диагностике ошибок проекта **100gram** (Telegram-Android) на платформе Windows.

---

## 1. Настройка окружения (Environment)

Перед любым запуском Gradle в PowerShell необходимо экспортировать корректный путь к JDK 17 (поставляется вместе с Android Studio):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

Проверка версии Java:
```powershell
& "$env:JAVA_HOME\bin\java.exe" -version
```
> [!IMPORTANT]
> Проект требует JDK 17. Использование JDK 21+ без флагов совместимости может приводить к сбоям Gradle Daemon и аннотационных процессоров.

---

## 2. Основные команды сборки (Gradle Tasks)

Проект использует модульную структуру. Основной исполняемый модуль форка — `:TMessagesProj_AppStandalone`.

### Быстрая проверка компиляции Java (Javac)
Используй для мгновенной проверки синтаксиса, типов и импортов без долгой упаковки ресурсов и native .so:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :TMessagesProj_AppStandalone:compileAfatDebugJavaWithJavac
```

### Полная сборка отладочного APK (Debug)
Компилирует C++ (CMake), генерирует ресурсы, собирает Dex и упаковывает APK:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatDebug
```
- **Расположение собранного APK:**
  `TMessagesProj_AppStandalone\build\outputs\apk\afat\debug\`

### Сборка релизного APK (Release)
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatRelease
```
- **Расположение собранного APK:**
  `TMessagesProj_AppStandalone\build\outputs\apk\afat\release\`

---

## 3. Нативная часть (C++ / CMake / NDK)

Сборка нативных библиотек (`libtmessages.so`) управляется через CMake:
- **Поддерживаемые ABI:** `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- **Конфигурация CMake:** задачи вида `:TMessagesProj:configureCMakeDebug[arm64-v8a]`.
- При редактировании файлов в `TMessagesProj/jni/` полная сборка автоматически пересобирает затронутые C++ файлы.

---

## 4. Решение типовых проблем сборки

1. **`Cannot locate tasks that match ':TMessagesProj:compileAfatDebugJavaWithJavac'`**
   - **Причина:** Задача `compileAfatDebugJavaWithJavac` принадлежит модулю `:TMessagesProj_AppStandalone`, а не библиотечному `:TMessagesProj`.
   - **Решение:** Всегда указывай `:TMessagesProj_AppStandalone:compileAfatDebugJavaWithJavac`.

2. **Зависшие демоны Gradle (Out of memory / Locked files):**
   ```powershell
   .\gradlew.bat --stop
   ```

3. **Сбои кэша конфигурации (Configuration Cache):**
   Если сборка падает из-за невалидного состояния кэша:
   ```powershell
   .\gradlew.bat --no-configuration-cache :TMessagesProj_AppStandalone:assembleAfatDebug
   ```

4. **Ошибки дублирования ресурсов (`mergeAfatDebugResources`):**
   - Убедись, что добавленные иконки/строки не дублируют имена в модулях `TMessagesProj` и `TMessagesProj_AppStandalone`.
   - Проверяй уникальность ключей в `hundredgram_strings.xml`.
