---
name: 100gram-whatsnew
description: Обязательный регламент синхронизации экрана «Что нового» (HundredGramWhatsNewActivity), строковых ресурсов, CHANGELOG.md и FEATURES.md после реализации любой фичи или исправления бага в 100gram. Гарантирует, что ни одно изменение не будет забыто.
---

# 100gram What's New & Feature Keeper

Этот навык устанавливает **железное правило финализации задач**: любая реализованная фича, существенное исправление бага или изменение UI в форке 100gram **обязаны быть отражены во внутриклиентском экране «Что нового»**, файлах локализации, журнале изменений `CHANGELOG.md` и реестре `FEATURES.md`.

---

## 1. Когда применять (Триггеры)

Применяй этот регламент **всегда перед завершением задачи**:
- Добавлена новая пользовательская функция, переключатель, сервис или экран.
- Исправлен заметный баг (сохранение медиа, утечка ресурсов, ошибка синхронизации, трекер).
- Проведён рефакторинг настроек, разделов меню или внешнего вида.
- Пользователь просит «добавить в Что нового» или подготовить апдейт.

> [!IMPORTANT]
> **Definition of Done:** Задача считается завершённой ТОЛЬКО тогда, когда все 4 артефакта синхронизированы и проект успешно скомпилирован.

---

## 2. Обязательный протокол синхронизации (4 шага)

### Шаг 1. Экран «Что нового» (`HundredGramWhatsNewActivity.java`)
Файл: [`TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java)

Добавь карточку фичи в секцию актуальной версии в методе `fillItems()`:

```java
items.add(UItem.asCustom(createFeatureCard(context,
        R.drawable.msg_FEATURE_ICON, // иконка из res/drawable
        LocaleController.getString("WhatsNewFeatureTitle", R.string.WhatsNewFeatureTitle),
        LocaleController.getString("WhatsNewFeatureDesc", R.string.WhatsNewFeatureDesc),
        LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew), // бейдж "✦ НОВОЕ"
        true, // подсветка новой фичи
        v -> presentFragment(new TargetActivity()) // кликабельный переход (или null)
)));
items.add(UItem.asShadow(null));
```

**Рекомендации по клику:**
- Если фича настраивается в меню: `v -> presentFragment(new HundredGramSubSettingsActivity(CATEGORY_ID))`
- Если фича открывает отдельный экран: `v -> presentFragment(new MyCustomActivity())`
- Если у фичи нет прямого экрана: передавать `null`

---

### Шаг 2. Двуязычная локализация (`hundredgram_strings.xml`)
Каждая карточка экрана «Что нового» обязана иметь перевод на русский и английский языки.

1. **Русский язык:** [`TMessagesProj/src/main/res/values-ru/hundredgram_strings.xml`](file:///D:/100gram/TMessagesProj/src/main/res/values-ru/hundredgram_strings.xml)
   ```xml
   <string name="WhatsNewFeatureTitle">Название функции на русском</string>
   <string name="WhatsNewFeatureDesc">Понятное пользователю описание пользы функции и где её найти</string>
   ```

2. **Английский язык:** [`TMessagesProj/src/main/res/values/hundredgram_strings.xml`](file:///D:/100gram/TMessagesProj/src/main/res/values/hundredgram_strings.xml)
   ```xml
   <string name="WhatsNewFeatureTitle">Feature Title in English</string>
   <string name="WhatsNewFeatureDesc">User-facing description of the feature benefit and where to find it</string>
   ```

---

### Шаг 3. Журнал изменений (`CHANGELOG.md`)
Файл: [`CHANGELOG.md`](file:///D:/100gram/CHANGELOG.md)

Добавь запись в секцию `## [Unreleased]` в соответствующую категорию:
- `### Added` — если добавлена новая функциональность
- `### Fixed` — если исправлен баг или некорректная работа
- `### Changed` — если изменено существующее поведение

**Формат записи:**
```markdown
- [Компонент/Подсистема] Краткое описание сути изменения ([`ИмяКласса.java`](file:///D:/100gram/path/to/Class.java)): детали реализации и польза для пользователя.
```

**Теги компонентов:**
`[Audio/STT]`, `[Profile/Auditor]`, `[Tracker]`, `[Vault]`, `[Camera]`, `[UI]`, `[Network]`, `[Core]`, `[Security]`.

---

### Шаг 4. Реестр возможностей (`FEATURES.md`)
Файл: [`FEATURES.md`](file:///D:/100gram/FEATURES.md)

Если была добавлена полноценная пользовательская фича или раздел настроек, добавь новую пронумерованную секцию в конец файла:

```markdown
---

## N. Название функции (English Name)

* **Краткий тезис функции**:
  * **Описание:** Что функция делает и какую ценность приносит.
  * **Где включается (UI):** Точный путь в меню настроек (`Настройки 100gram -> ...`).
  * **Ключ конфигурации:** `HundredGramConfig.mySettingKey` (`boolean`/`int`).
  * **Ключевые классы:** [`MyClass.java`](file:///D:/100gram/path/to/MyClass.java).
  * **Статус:** В релизе / Экспериментально.
```

---

## 3. Чеклист самопроверки («Ничего не забыл?»)

Перед отправкой ответа пользователю пройди по списку:
- [ ] Карточка добавлена в `HundredGramWhatsNewActivity.java` с подходящей иконкой и кликом
- [ ] Строки добавлены в `values-ru/hundredgram_strings.xml`
- [ ] Строки добавлены в `values/hundredgram_strings.xml`
- [ ] Запись внесена в `CHANGELOG.md` (`[Unreleased]`)
- [ ] Фича зарегистрирована в `FEATURES.md`
- [ ] Проект успешно компилируется через Gradle:
  ```powershell
  cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat compileAfatDebugJavaWithJavac --daemon 2>&1"
  ```
- [ ] Изменения зафиксированы в git с понятным коммит-сообщением.
