# Документация 100gram

Индекс всех документов репозитория. В корне остаются только `README.md`, `CHANGELOG.md`,
`AGENTS.md`, `LICENSE`, оперативные файлы состояния (`PROJECT_STATE.md`, `OPEXGRAM_PARITY.md`)
и `badges.json` (URL синка захардкожен в клиенте).

## Навигация

| Раздел | Что внутри |
|---|---|
| [exteragram/](exteragram/) | Аудит донора exteraGram: полный обзор функциональности, инвентарь настроек, модификации стандартных классов, плагины и лента, правки анимаций, финальная верификация |
| [guides/](guides/) | `WINDOWS_BUILD_FIXES.md` — почему на Windows ломаются темы при сборке и как это лечится |
| [plans/](plans/) | `settings_redesign_plan.md` — план переработки экрана настроек |
| [archive/](archive/) | **Устаревшие документы, не обновляются:** `HANDOVER.md` (29.09.2026), `FEATURES.md` (реестр для ветки 12.9.2) |

## Документы верхнего уровня

| Файл | Назначение |
|---|---|
| [ai-development-workflow.md](ai-development-workflow.md) | Цикл AI-разработки: правка → сборка → установка → скриншоты/UI-дамп |
| [command-contract.md](command-contract.md) | Контракт единого интерфейса `./tools/android` |

## Актуальное состояние проекта (в корне)

- [`PROJECT_STATE.md`](../PROJECT_STATE.md) — git-состояние и точка передачи между агентами
- [`OPEXGRAM_PARITY.md`](../OPEXGRAM_PARITY.md) — матрица паритета с OpexGram
- [`CHANGELOG.md`](../CHANGELOG.md) — журнал изменений
- [`ROADMAP_PLAN.md`](../ROADMAP_PLAN.md) — дорожная карта этапов
