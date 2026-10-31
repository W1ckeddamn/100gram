#!/usr/bin/env bash
# ==============================================================================
# 100gram Upstream Rebase & Sync Helper
# ==============================================================================
# Этот скрипт помогает безопасно проверить и перенести форк на новый Telegram.
# ==============================================================================

set -euo pipefail

UPSTREAM_REMOTE="upstream"
UPSTREAM_BRANCH="master"
BASE_BRANCH="upstream-base"
FEATURE_BRANCH="main"

echo "=== 1. Получение последних изменений из официального Telegram ==="
git fetch "$UPSTREAM_REMOTE" --tags

LATEST_TAG=$(git describe --tags $(git rev-list --tags --max-count=1 "$UPSTREAM_REMOTE/$UPSTREAM_BRANCH"))
echo "Текущая база: $(git describe --tags $BASE_BRANCH)"
echo "Последний релиз Telegram: $LATEST_TAG"

echo ""
echo "=== 2. Проверка тестового слияния (Dry-Run Merge) ==="
TEST_BRANCH="test-sync-$(date +%s)"
git checkout -b "$TEST_BRANCH" "$FEATURE_BRANCH"

if git merge --no-commit --no-ff "$UPSTREAM_REMOTE/$UPSTREAM_BRANCH"; then
    echo "✅ Отлично! Слияние с последним Telegram проходит БЕЗ КОНФЛИКТОВ."
    git merge --abort
else
    echo "⚠️ Внимание: Есть конфликтующие файлы:"
    git diff --name-only --diff-filter=U
    git merge --abort
fi

git checkout "$FEATURE_BRANCH"
git branch -D "$TEST_BRANCH"

echo ""
echo "Инструкция для переноса на новый тег:"
echo "1. git checkout -b rebase-$LATEST_TAG"
echo "2. git rebase --onto $LATEST_TAG $BASE_BRANCH"
echo "3. Разрешить мелкие конфликты в вызовах HundredGramHooks и закончить: git rebase --continue"
