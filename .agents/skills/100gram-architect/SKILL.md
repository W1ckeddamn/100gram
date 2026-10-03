---
name: 100gram-architect
description: >-
  Core architectural guidelines, threading models, defensive null safety,
  and coding conventions for the 100gram Telegram-Android fork.
  Use when designing new components, hooking into Telegram lifecycle,
  or refactoring existing fork modules.
---

# 100gram Telegram-Android Architecture & Coding Guide

Этот навык описывает архитектурный фундамент и правила надёжности для кастомного форка Telegram для Android ("100gram", Java/C++/SQLite/MTProto).

---

## 1. Архитектура UI и навигация

- **Запрет стандартных Activity:** 
  Никогда не создавай стандартные `android.app.Activity` для экранов приложения. Все экраны обязаны наследоваться от `org.telegram.ui.ActionBar.BaseFragment`.
- **Навигационный стек:** 
  Переходы между фрагментами осуществляются строго через нативный стек:
  ```java
  presentFragment(new SomeCustomFragment());
  // или из родительских контекстов:
  actionBarLayout.presentFragment(new SomeCustomFragment());
  ```
- **Загрузка и отображение изображений:** 
  Используй нативный `BackupImageView` и `ImageLocation.getForPath(...)`. Запрещено подключать и использовать сторонние библиотеки загрузки изображений (Glide, Picasso, Coil).
- **Списки и скроллинг:** 
  Всегда используй `org.telegram.ui.Components.RecyclerListView` вместо классического Android `RecyclerView`. Он поддерживает нативные Telegram-темы, быстродействующий скролл и контекстные действия.

---

## 2. Модель многопоточности (Concurrency)

Telegram-Android использует строгую модель очередей на базе `DispatchQueue`:

- **UI-поток:** Любые операции с View, анимации и вызовы `NotificationCenter.postNotificationName` выполняй строго на главном потоке:
  ```java
  AndroidUtilities.runOnUIThread(() -> {
      // обновление UI
  });
  ```
- **База данных (SQLite):** Все транзакции, вставки и выборки должны исполняться асинхронно в очереди хранилища:
  ```java
  MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
      // чтение или запись в базу данных
  });
  ```
  Категорически запрещены блокирующие транзакции к БД на главном потоке!
- **Фоновые вычисления:** Для тяжелых вычислений, операций с файловой системой или декодирования используй:
  ```java
  Utilities.globalQueue.postRunnable(() -> {
      // тяжелые вычисления
  });
  ```

---

## 3. База данных и хранилище (Vault & SQLite)

- **Таблицы базы данных форка в `MessagesStorage`:**
  - `hundredgram_vault`: поля `mid INTEGER, uid INTEGER, file_name TEXT, vault_path TEXT, edit_date INTEGER`.
  - `hundredgram_message_edits`: BLOB-сериализация ревизий сообщений.
  - `telegraher_message_deletions`: сохранение статусов и метаданных удалённых сообщений.
  - `user_activity_log`: сессии активности пользователей (`user_id INTEGER, time_start INTEGER, time_end INTEGER, trigger_type INTEGER`).
- **Путь к Vault:** Всегда получай через `HundredGramVaultManager.getInstance(currentAccount).getVaultDir()`. Никогда не хардкодь путь строками.
- **Изоляция медиафайлов:** При сохранении файлов в Vault всегда используй hardlink (`android.system.Os.link`) через метод `HundredGramVaultManager.linkOrCopyFile(...)`. Это исключает дублирование места на накопителе и защищает файл от удаления системным клинером кэша.

---

## 4. Энергопотребление и фоновая служба

- **Запрет Wakelock-таймеров:** Запрещено использовать бесконечные фоновые циклические `Runnable` или `Handler` с малыми интервалами (< 5–10 минут). Любая активность должна быть реактивной (просыпаться по сетевым пакетам MTProto или изменениям системных ресиверов).
- **Foreground Service:** `NotificationsService` удерживается как Foreground Service с типом `dataSync` при активных фоновых задачах.
- **Boot Receiver:** В `HundredGramBootReceiver` при получении `ACTION_BOOT_COMPLETED` обязательно оборачивай весь запуск в `try { ... } catch (Throwable e) { FileLog.e(e); }`.

---

## 5. Defensive Null Safety (Обязательный чеклист)

1. **Разбор сетевых сообщений (`TL_updateNewMessage`, `TL_updateNewChannelMessage`):**
   ```java
   if (message != null && !message.out && message.from_id instanceof TLRPC.TL_peerUser) {
       long senderUserId = message.from_id.user_id;
       // ...
   }
   ```
   В публичных каналах `from_id` может быть `null` или экземпляром `TL_peerChannel`.
2. **Коллбэки сетевых запросов MTProto (`sendRequest`):**
   ```java
   ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
       if (error != null || response == null) {
           return;
       }
       // безопасный парсинг response
   });
   ```
3. **Исключение автоматического анбоксинга (Auto-unboxing):**
   При извлечении из коллекций (`LongSparseArray`, `Map<Long, Long>`):
   ```java
   Long startVal = activeSessions.get(userId);
   if (startVal != null && startVal > 0) { ... }
   ```
   Никогда не присваивай `long val = map.get(key);` напрямую!
4. **Файловая система:**
   Вызовы `file.listFiles()` всегда проверяй на `null`:
   ```java
   File[] files = dir.listFiles();
   if (files != null) {
       for (File f : files) { ... }
   }
   ```
