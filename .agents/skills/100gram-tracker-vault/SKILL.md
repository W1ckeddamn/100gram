---
name: 100gram-tracker-vault
description: >-
  Operational guide and reference for HundredGram Activity Tracker (online sessions,
  auto-close timeouts, Unix timestamp standards, daily totals) and Media Vault (media isolation,
  SQLite indexing, hardlinks).
---

# 100gram Activity Tracker & Media Vault Guide

Специализированное руководство по работе с подсистемой отслеживания активности (`HundredGramActivityTracker`) и медиа-сейфом (`HundredGramVaultManager`).

---

## 1. Трекер активности (`HundredGramActivityTracker`)

### Единый стандарт времени (СТРОГО СЕКУНДЫ)
- Все таймстемпы в коде и SQLite таблице `user_activity_log` (`time_start`, `time_end`) сохраняются **строго в секундах Unix-времени**:
  ```java
  long now = System.currentTimeMillis() / 1000L;
  ```
- Данные из MTProto (`status.expires`, `status.was_online`, `message.date`) уже приходят в секундах. **Категорически запрещено** умножать их на 1000 при записи в базу.
- Все операции разницы времени (`duration = end_time - start_time`) должны оперировать секундами.

### Обработка триггеров активности
1. **`TRIGGER_STATUS_ONLINE` (1):** Получен статус `TL_userStatusOnline`. Запускает сессию или продлевает текущую.
2. **`TRIGGER_TYPING` (2):** Пользователь печатает сообщение.
   - Таймаут тайпинга равен **6 секундам**.
   - Не сшивается в длинную сессию, если других действий нет.
3. **`TRIGGER_MESSAGE` (3):** Пришло входящее сообщение от пользователя.
   - Апгрейдит тайпинг-сессию до полноценной онлайн-сессии.
4. **`TRIGGER_APP_OPEN` (4):** Заход в приложение самим владельцем аккаунта (при `trackSelfActivity = true`).

### Гарантированное закрытие висячих сессий (Auto-Close 90s)
Если у отслеживаемого пользователя скрыт статус ("был недавно"), сервер Telegram никогда не присылает `TL_userStatusOffline`.
Поэтому применяется правило автозакрытия:
- Если с момента последнего действия (`lastUserActivityTimes`) прошло более **90 секунд**, сессия принудительно закрывается:
  ```java
  long estimatedEnd = (lastActionTime > startTime) ? lastActionTime : (startTime + 45L);
  MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(userId, startTime, estimatedEnd);
  activeSessions.remove(userId);
  activeSessionTypes.remove(userId);
  lastUserActivityTimes.remove(userId);
  ```

### Расчет суммарного времени за день
1. **Запрос к базе данных:**
   ```sql
   SELECT SUM(CASE 
       WHEN time_end > time_start THEN (time_end - time_start) 
       ELSE 30 
   END) 
   FROM user_activity_log 
   WHERE user_id = ? AND time_start >= ?
   ```
2. **Учёт текущей живой сессии:**
   Если пользователь онлайн прямо сейчас:
   ```java
   long liveSessionTime = (System.currentTimeMillis() / 1000L) - activeSessionStart;
   totalSeconds += Math.max(0, liveSessionTime);
   ```
3. **Форматирование в интерфейсе:**
   ```java
   public static String formatDuration(long totalSeconds) {
       long minutes = totalSeconds / 60;
       long hours = minutes / 60;
       long remMinutes = minutes % 60;
       if (hours > 0) {
           return hours + " ч " + remMinutes + " мин";
       }
       return Math.max(1, minutes) + " мин";
   }
   ```

---

## 2. Медиа-сейф (`HundredGramVaultManager`)

### Задачи подсистемы
- Изоляция удаляемых и редактируемых файлов (фото, видео, голосовые, видеозаметки, документы) до того, как они будут зачищены сервером Telegram или локальным кэшем.

### Индексация в SQLite
Таблица `hundredgram_vault` в `MessagesStorage`:
```sql
CREATE TABLE IF NOT EXISTS hundredgram_vault(
    mid INTEGER,
    uid INTEGER,
    file_name TEXT,
    vault_path TEXT,
    edit_date INTEGER,
    PRIMARY KEY(mid, vault_path)
);
```

### Безопасное связывание файлов (Hardlinks)
Для экономии дискового пространства и моментального копирования без I/O нагрузки:
```java
try {
    android.system.Os.link(sourceFile.getAbsolutePath(), vaultFile.getAbsolutePath());
} catch (Throwable e) {
    // Fallback на прямое побайтовое копирование, если файловые системы не поддерживают hardlinks
    copyFile(sourceFile, vaultFile);
}
```
Директория сейфа всегда запрашивается через:
```java
File vaultDir = HundredGramVaultManager.getInstance(currentAccount).getVaultDir();
```
