---
name: 100gram-state-sync
description: >-
  Inspects git state, commit history, and OpenCode/agent session databases to generate
  compact, high-density project state markdown files (e.g. PROJECT_STATE.md) for
  seamless handoffs between AI agents in 100gram / opexgram / exteragram.
---

# 100gram State Sync Skill

Этот навык предназначен для создания, обновления и сжатия файлов состояния проекта (`PROJECT_STATE.md`, `OPEXGRAM_PARITY.md`) в репозитории [100gram](file:///D:/100gram).

Файлы состояния служат «внешней памятью» (Working Memory on Disk) и позволяют новому агенту мгновенно восстановить контекст:
1. Текущую рабочую ветку, коммит и статус Git.
2. Состояние мультидиректорийного окружения (`D:\100gram`, `D:\opexgram`, `D:\exteragram`).
3. Точный перечень завершённых и находящихся в разработке фич (паритет с OpexGram).
4. Точку передачи задачи (Handoff Point) из базы данных сессий (`opencode.db`).
5. Архитектурные файлы-маяки и команды сборки.

---

## ⚡ Быстрый запуск

Для автоматической генерации / актуализации `PROJECT_STATE.md` выполни команду:

```powershell
python D:\100gram\.agents\skills\100gram-state-sync\scripts\generate_state.py
```

Скрипт автоматически:
- Считывает текущую ветку, HEAD-коммит и статус изменений Git.
- Проверяет базу данных OpenCode (`C:\Users\wicked\.local\share\opencode\opencode.db`) на наличие последней активной сессии и запроса пользователя.
- Формирует компактный, токен-эффективный Markdown-документ [PROJECT_STATE.md](file:///D:/100gram/PROJECT_STATE.md).

---

## 📐 Правила компактности (Anti-Slop State Design)

При ручном или полуавтоматическом редактировании файлов состояния строго соблюдай правила:

1. **Таблицы вместо повествования:**
   Всегда перечисляй фичи, директории и точки касания в виде Markdown-таблиц. Это снижает расход токенов на 60–70% по сравнению со связным текстом.
2. **Точные идентификаторы:**
   Указывай точные имена классов (`HundredGramConfig.java`, `HundredGramHaptics.java`), хэши коммитов (`5224a973c`) и строковые ключи (`OpexgramWeekdayInDate`), а не общие описания.
3. **Фиксация точки останова (Handoff Point):**
   Всегда явно выделяй раздел `Active Task / Handoff Point` с точным вопросом/задачей, на которой остановился предыдущий агент или пользователь.
4. **Связь с донором (`D:\opexgram`):**
   При переносе фич фиксируй оригинальные имена методов и классов из декомпилированного APK (`D:\opexgram\opexgram_src`), чтобы следующему агенту не приходилось повторно запускать глубокий поиск.

---

## 🔄 Структура файлов состояния

В проекте поддерживаются два ключевых документа:

1. [**`PROJECT_STATE.md`**](file:///D:/100gram/PROJECT_STATE.md) (Оперативное состояние):
   - Окружение (`100gram`, `opexgram`, `exteragram`, `opencode.db`).
   - Статус Git (`branch`, `HEAD`, `status -s`).
   - Последние 10 коммитов.
   - Текущая активная задача и точка передачи.
   - Команды сборки и проверки (`gradlew`).

2. [**`OPEXGRAM_PARITY.md`**](file:///D:/100gram/OPEXGRAM_PARITY.md) (Матрица паритета):
   - Посегментный трекер портирования фич из OpexGram 13.1.1 beta 10:
     - Batch 1..10 (Завершено)
     - Missing — feasible, next batches (Бэклог: MD3, Glass bubbles, Chat input renderer, Quotes)
     - Not implementable (Серверные зависимости, сторонние бэкенды)

---

## 🛠️ Рабочий процесс передачи (Handoff Workflow)

Когда пользователь или агент завершает этап работы:
1. Закоммить все рабочие изменения: `git commit -m "feat/fix: ..."`
2. Запусти генератор состояния:
   ```powershell
   python D:\100gram\.agents\skills\100gram-state-sync\scripts\generate_state.py
   ```
3. Если внедрялись фичи из OpexGram, добавь строки в таблицу [OPEXGRAM_PARITY.md](file:///D:/100gram/OPEXGRAM_PARITY.md).
4. Проверь отсутствие конфликтов ресурсов:
   ```powershell
   .\gradlew.bat :TMessagesProj:packageDebugResources --daemon
   ```
