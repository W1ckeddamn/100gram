package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramWhatsNewActivity extends BaseFragment {

    private static final int ITEM_CHECK_UPDATES = 1;
    private static final int ITEM_GITHUB_REPO = 2;
    private static final int ITEM_AUTHOR_LINK = 3;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramWhatsNew", R.string.HundredGramWhatsNew));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Раздел: Быстрые действия
        items.add(UItem.asHeader("100gram Telegram Fork"));

        items.add(UItem.asButton(ITEM_CHECK_UPDATES, R.drawable.msg_help,
                LocaleController.getString("HundredGramCheckUpdates", R.string.HundredGramCheckUpdates),
                "GitHub Releases"));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCheckUpdatesSub", R.string.HundredGramCheckUpdatesSub)));

        items.add(UItem.asButton(ITEM_AUTHOR_LINK, R.drawable.msg_openprofile,
                LocaleController.getString("HundredGramAuthor", R.string.HundredGramAuthor),
                "@w1ckeddamn"));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramAuthorSub", R.string.HundredGramAuthorSub)));

        items.add(UItem.asButton(ITEM_GITHUB_REPO, R.drawable.msg_link,
                LocaleController.getString("HundredGramGitHub", R.string.HundredGramGitHub),
                "github.com/W1ckeddamn/100gram"));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramGitHubSub", R.string.HundredGramGitHubSub)));

        // Раздел: Версия 1.2
        items.add(UItem.asHeader("Версия 1.2 (Текущее обновление)"));

        items.add(UItem.asButton(101, R.drawable.msg_media,
                "Размер стикеров и GIF",
                "50% – 150%"));
        items.add(UItem.asShadow("Настройки масштаба отображения стикеров и GIF-анимаций в чатах (5 вариантов масштабирования)"));

        items.add(UItem.asButton(102, R.drawable.msg_camera,
                "Кружочки из обычных видео",
                "1:1 видеосообщение"));
        items.add(UItem.asShadow("Отправка любого видео из галереи в виде круглого видеосообщения в один клик"));

        items.add(UItem.asButton(103, R.drawable.msg_bot,
                "Layer 229 & RichMessage",
                "Инлайн-кнопки"));
        items.add(UItem.asShadow("Поддержка интерактивных кнопок в тексте, Mini Apps, WebApps и уникальных подарков Layer 229"));

        // Раздел: Версия 1.1
        items.add(UItem.asHeader("Версия 1.1"));

        items.add(UItem.asButton(201, R.drawable.msg_download,
                "Медиасейф и архиватор",
                "2 режима работы"));
        items.add(UItem.asShadow("Режимы «Только новые» и «Полная синхронизация», глубокий скан всей истории чатов, архивация комментариев"));

        items.add(UItem.asButton(202, R.drawable.msg_usersearch,
                "Трекер онлайна и активности",
                "Мониторинг сессий"));
        items.add(UItem.asShadow("Точный учёт активности пользователей со скрытым онлайном, журнал посещений и уведомления"));

        // Раздел: Версия 1.0
        items.add(UItem.asHeader("Версия 1.0"));

        items.add(UItem.asButton(301, R.drawable.msg_secret,
                "Режим невидимки (Ghost Mode)",
                "Полная скрытность"));
        items.add(UItem.asShadow("Чтение без галочек, скрытие онлайна, набора текста и тайный просмотр историй"));

        items.add(UItem.asButton(302, R.drawable.msg_edit,
                "Анти-удаление и история правок",
                "Защита переписки"));
        items.add(UItem.asShadow("Сохранение удалённых сообщений и всех редакций текста со специальными бейджами"));

        items.add(UItem.asButton(303, R.drawable.msg_stats,
                "Network Boost & CameraX 60 FPS",
                "Производительность"));
        items.add(UItem.asShadow("Параллельная загрузка чанков, видеосообщения 60 FPS и квадратные кружочки"));

        items.add(UItem.asButton(304, R.drawable.msg_channel,
                "Режим стримера & Приватность",
                "Безопасность"));
        items.add(UItem.asShadow("Скрытие телефонных номеров, юзернеймов, аватарок и отключение скриншотов"));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_CHECK_UPDATES:
                Browser.openUrl(getParentActivity(), "https://github.com/W1ckeddamn/100gram/releases");
                break;
            case ITEM_GITHUB_REPO:
                Browser.openUrl(getParentActivity(), "https://github.com/W1ckeddamn/100gram");
                break;
            case ITEM_AUTHOR_LINK:
                Browser.openUrl(getParentActivity(), "https://t.me/w1ckeddamn");
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
