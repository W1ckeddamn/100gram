---
name: 100gram-ui-components
description: >-
  Guidelines and boilerplate patterns for creating Telegram-Android UI in 100gram:
  BaseFragment screens, BottomSheets, RecyclerListView, custom cells, action bars,
  theme colors, and localization.
---

# 100gram Telegram-Android UI Components Guide

Справочник и шаблоны для реализации экранов, шторок и компонентов пользовательского интерфейса в Telegram-Android ("100gram").

---

## 1. Анатомия экрана (`BaseFragment`)

Любой экран в Telegram наследуется от `BaseFragment`. Стандартная структура класса:

```java
package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class CustomActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        // Инициализация данных и подписка на NotificationCenter
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        // Отписка от NotificationCenter
    }

    @Override
    public View createView(Context context) {
        // 1. Настройка нативного ActionBar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("CustomTitle", R.string.CustomTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        // 2. Корневой контейнер
        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        // 3. Список RecyclerListView
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            // Обработка клика по строкам
        });

        return fragmentView;
    }
}
```

---

## 2. Каталог стандартных ячеек Telegram

При формировании настроек и списков используй готовые нативные ячейки:

1. **`TextCheckCell`** — строка с переключателем (Checkbox / Switch):
   ```java
   TextCheckCell checkCell = (TextCheckCell) holder.itemView;
   checkCell.setTextAndCheck(
       "Отслеживать активность",  // Заголовок
       "Логировать время в сети", // Подзаголовок (опционально)
       isChecked,                // Состояние
       true,                     // true = мультилайн/подзаголовок
       true                      // true = рисовать разделитель снизу
   );
   ```

2. **`TextSettingsCell`** — строка настройки (текст слева, текущее значение справа):
   ```java
   TextSettingsCell cell = (TextSettingsCell) holder.itemView;
   cell.setTextAndValue("Качество видео", "1080p", true);
   ```

3. **`HeaderCell`** — заголовок секции:
   ```java
   HeaderCell headerCell = (HeaderCell) holder.itemView;
   headerCell.setText("Безопасность и приватность");
   ```

4. **`UserCell`** — строка пользователя с круглым аватаром, именем и статусом:
   ```java
   UserCell userCell = (UserCell) holder.itemView;
   userCell.setData(user, null, "онлайн", 0);
   ```

5. **`ShadowSectionCell`** — отступ между секциями:
   ```java
   // Ячейка-разделитель между логическими блоками списка
   ```

---

## 3. Стилизация и цвета (Telegram Theme Engine)

Запрещено использовать цвета из `android.R.color` или хардкодить HEX-цвета (`#FFFFFF`, `#000000`) в кастомном UI. Всегда запрашивай цвет из текущей темы:

```java
// Фон экрана настроек
Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider);

// Фон ячейки/карточки
Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider);

// Основной текст
Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);

// Серый текст / подзаголовок
Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider);

// Акцентный цвет (кнопки, активные чекбоксы)
Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider);

// Красный цвет (удаление, ошибки)
Theme.getColor(Theme.key_text_RedRegular, resourcesProvider);
```

---

## 4. Локализация и строки

- Добавляй новые строки в `TMessagesProj/src/main/res/values/hundredgram_strings.xml` и русский перевод в `values-ru/hundredgram_strings.xml`.
- В коде читай строки строго через `LocaleController`:
  ```java
  // Простая строка
  String title = LocaleController.getString("HundredGramFeatureTitle", R.string.HundredGramFeatureTitle);

  // Форматированная строка с аргументами
  String text = LocaleController.formatString("HundredGramUsersCount", R.string.HundredGramUsersCount, count);
  ```

---

## 5. Модальные шторки (`BottomSheet`)

Для модальных диалогов снизу используй `BottomSheet` или `BottomSheetWithRecyclerListView`:
```java
BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
builder.setTitle("Заголовок шторки");
builder.setCustomView(customView);
builder.show();
```
Для закрытия шторки: `dismiss()`.
