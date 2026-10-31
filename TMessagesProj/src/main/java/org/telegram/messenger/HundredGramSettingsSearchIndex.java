package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.HundredGramActivitySimulatorActivity;
import org.telegram.ui.HundredGramAiSettingsActivity;
import org.telegram.ui.HundredGramAutomationActivity;
import org.telegram.ui.HundredGramBackupActivity;
import org.telegram.ui.HundredGramCameraActivity;
import org.telegram.ui.HundredGramIconPacksActivity;
import org.telegram.ui.HundredGramMessageMenuActivity;
import org.telegram.ui.HundredGramMusicActivity;
import org.telegram.ui.HundredGramNetworkBoostActivity;
import org.telegram.ui.HundredGramPillStackSettingsActivity;
import org.telegram.ui.HundredGramPluginsActivity;
import org.telegram.ui.HundredGramSubSettingsActivity;
import org.telegram.ui.HundredGramSwipeActionsActivity;
import org.telegram.ui.HundredGramVaultViewerActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HundredGramSettingsSearchIndex {

    private static volatile HundredGramSettingsSearchIndex instance;

    public static HundredGramSettingsSearchIndex getInstance() {
        if (instance == null) {
            synchronized (HundredGramSettingsSearchIndex.class) {
                if (instance == null) {
                    instance = new HundredGramSettingsSearchIndex();
                }
            }
        }
        return instance;
    }

    public static class Entry {
        public final int id;
        public final String titleKey;
        public final String defaultTitle;
        public final String subtitleKey;
        public final String defaultSubtitle;
        public final String categoryTitleKey;
        public final String defaultCategoryTitle;
        public final int categoryIcon;
        public final int category; // for HundredGramSubSettingsActivity
        public final int itemId;   // row itemId to scroll and pulse highlight
        public final Class<? extends BaseFragment> targetClass;
        public final String[] keywords;

        public Entry(int id, String titleKey, String defaultTitle,
                     String subtitleKey, String defaultSubtitle,
                     String categoryTitleKey, String defaultCategoryTitle,
                     int categoryIcon, int category, int itemId,
                     Class<? extends BaseFragment> targetClass,
                     String... keywords) {
            this.id = id;
            this.titleKey = titleKey;
            this.defaultTitle = defaultTitle;
            this.subtitleKey = subtitleKey;
            this.defaultSubtitle = defaultSubtitle;
            this.categoryTitleKey = categoryTitleKey;
            this.defaultCategoryTitle = defaultCategoryTitle;
            this.categoryIcon = categoryIcon;
            this.category = category;
            this.itemId = itemId;
            this.targetClass = targetClass;
            this.keywords = keywords;
        }

        public String getTitle() {
            if (TextUtils.isEmpty(titleKey)) return defaultTitle;
            int resId = ApplicationLoader.applicationContext.getResources().getIdentifier(titleKey, "string", ApplicationLoader.applicationContext.getPackageName());
            return resId != 0 ? LocaleController.getString(titleKey, resId) : defaultTitle;
        }

        public String getSubtitle() {
            if (TextUtils.isEmpty(subtitleKey)) return defaultSubtitle != null ? defaultSubtitle : "";
            int resId = ApplicationLoader.applicationContext.getResources().getIdentifier(subtitleKey, "string", ApplicationLoader.applicationContext.getPackageName());
            return resId != 0 ? LocaleController.getString(subtitleKey, resId) : (defaultSubtitle != null ? defaultSubtitle : "");
        }

        public String getCategoryTitle() {
            if (TextUtils.isEmpty(categoryTitleKey)) return defaultCategoryTitle;
            int resId = ApplicationLoader.applicationContext.getResources().getIdentifier(categoryTitleKey, "string", ApplicationLoader.applicationContext.getPackageName());
            return resId != 0 ? LocaleController.getString(categoryTitleKey, resId) : defaultCategoryTitle;
        }
    }

    public static class MatchResult implements Comparable<MatchResult> {
        public final Entry entry;
        public final int score;
        public final int matchTitleStart;
        public final int matchTitleEnd;

        public MatchResult(Entry entry, int score, int matchTitleStart, int matchTitleEnd) {
            this.entry = entry;
            this.score = score;
            this.matchTitleStart = matchTitleStart;
            this.matchTitleEnd = matchTitleEnd;
        }

        @Override
        public int compareTo(MatchResult o) {
            return Integer.compare(o.score, this.score);
        }
    }

    private final ArrayList<Entry> entries = new ArrayList<>();
    private final SharedPreferences preferences;

    private HundredGramSettingsSearchIndex() {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_settings_search", Context.MODE_PRIVATE);
        buildIndex();
    }

    private void buildIndex() {
        entries.clear();
        int id = 1;

        // 1. Призрачный режим (Ghost Mode)
        addEntry(id++, "GhostOnline", "Призрачный режим (онлайн)",
                "GhostOnlineInfo", "Скрывать статус «В сети» и не отправлять статус присутствия",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 1,
                HundredGramSubSettingsActivity.class,
                "призрак", "онлайн", "ghost", "online", "невидимка", "статус", "присутствие");

        addEntry(id++, "SendOfflineAfterOnline", "Отправлять «Не в сети» после сообщений",
                null, "Мгновенно переводить аккаунт в офлайн после отправки сообщений",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 184,
                HundredGramSubSettingsActivity.class,
                "офлайн", "offline", "призрак", "сообщение");

        addEntry(id++, "GhostTyping", "Скрывать «Печатает…»",
                null, "Не отправлять собеседнику статус набора текста или записи голосового",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 2,
                HundredGramSubSettingsActivity.class,
                "печатает", "typing", "набор", "текст", "голос", "запись");

        addEntry(id++, "GhostUploadProgress", "Скрывать выгрузку файлов",
                null, "Не показывать индикатор отправки медиа и документов",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 185,
                HundredGramSubSettingsActivity.class,
                "выгрузка", "upload", "прогресс", "файл", "отправка");

        addEntry(id++, "GhostReadMessages", "Не читать входящие сообщения",
                "GhostReadMessagesInfo", "Оставлять сообщения непрочитанными для собеседника",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 3,
                HundredGramSubSettingsActivity.class,
                "прочитано", "галочки", "read", "непрочитано", "шпион");

        addEntry(id++, "GhostReadOnReply", "Читать при ответе",
                null, "Автоматически помечать сообщение прочитанным при ответе на него",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 4,
                HundredGramSubSettingsActivity.class,
                "ответ", "reply", "прочтение");

        addEntry(id++, "GhostReadMediaContents", "Не читать медиа",
                null, "Не отмечать голосовые сообщения и кругляшки как прослушанные",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 5,
                HundredGramSubSettingsActivity.class,
                "кругляшки", "голосовые", "прослушано", "voice", "round");

        addEntry(id++, "GhostShowMarkAsRead", "Кнопка «Прочитать» в контекстном меню",
                null, "Добавляет кнопку ручной отметки сообщения прочитанным",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 6,
                HundredGramSubSettingsActivity.class,
                "пометить", "прочитано", "кнопка", "меню");

        addEntry(id++, "TrackReadDate", "Точное время прочтения сообщений",
                null, "Показывает точную дату и время, когда собеседник открыл ваше сообщение",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 7,
                HundredGramSubSettingsActivity.class,
                "время", "прочтение", "шпион", "date", "read", "spy");

        addEntry(id++, "GhostHideReadDate", "Скрывать время прочтения для других",
                null, "Не отправлять другим точное время прочтения сообщений в группах",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 183,
                HundredGramSubSettingsActivity.class,
                "скрыть", "время", "прочтение", "группы");

        addEntry(id++, "GhostStoriesMode", "Режим просмотра историй",
                "GhostStoriesModeInfo", "Анонимный просмотр историй без попадания в список просмотров",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 8,
                HundredGramSubSettingsActivity.class,
                "истории", "stories", "анонимно", "инкогнито", "сторис");

        addEntry(id++, "UnlockStorySaving", "Скачивание историй в галерею",
                null, "Разрешить сохранение чужих историй на устройство",
                "HundredGramCategoryGhost", "Призрачный режим",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_GHOST, 9,
                HundredGramSubSettingsActivity.class,
                "скачать", "сохранить", "истории", "stories", "галерея");

        // 2. Имитация активности (Activity Simulator)
        addEntry(id++, "ActivitySimulatorTitle", "Имитация активности",
                "ActivitySimulatorSubtitle", "Поддержание статуса «В сети» на заданное время и планировщик входа",
                "ActivitySimulatorTitle", "Имитация активности",
                R.drawable.msg_usersearch, 0, 0,
                HundredGramActivitySimulatorActivity.class,
                "имитация", "активность", "онлайн", "симуляция", "таймер", "расписание", "планировщик", "simulator", "presence");

        addEntry(id++, "ActivitySimulatorScheduleToggle", "Запланировать вход в сеть",
                "ActivitySimulatorScheduleInfo", "Автоматический вход в сеть в указанное время суток на заданную длительность",
                "ActivitySimulatorTitle", "Имитация активности",
                R.drawable.msg_calendar2, 0, 4,
                HundredGramActivitySimulatorActivity.class,
                "запланировать", "расписание", "вход", "время", "автоматически", "schedule");

        addEntry(id++, "ActivitySimulatorMicroBreaksToggle", "Естественные микро-перерывы",
                "ActivitySimulatorMicroBreaksInfo", "Короткие паузы (1-2 мин.) каждые 15 минут для реалистичности симуляции",
                "ActivitySimulatorTitle", "Имитация активности",
                R.drawable.msg_usersearch, 0, 8,
                HundredGramActivitySimulatorActivity.class,
                "паузы", "перерывы", "реализм", "микро", "breaks");

        // 3. Хранилище и медиа (Vault & Anti-Delete)
        addEntry(id++, "SaveAntiDeleteText", "Архиватор удаленных сообщений (Anti-Delete)",
                "SaveTypesHeaderInfo", "Сохранять текст удаленных сообщений с пометкой об удалении",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_archive, HundredGramSubSettingsActivity.CATEGORY_VAULT, 202,
                HundredGramSubSettingsActivity.class,
                "удаленные", "антиделит", "anti-delete", "сообщения", "хранилище", "сохранение", "текст");

        addEntry(id++, "SaveEditHistory", "История редактирования сообщений",
                null, "Сохранять все промежуточные версии отредактированных сообщений",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_archive, HundredGramSubSettingsActivity.CATEGORY_VAULT, 203,
                HundredGramSubSettingsActivity.class,
                "редактирование", "история", "версии", "edit", "history", "правки");

        addEntry(id++, "SaveMediaPhotos", "Сохранять удаленные фото",
                null, "Автоматическое сохранение удаленных фотографий в хранилище",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_media, HundredGramSubSettingsActivity.CATEGORY_VAULT, 204,
                HundredGramSubSettingsActivity.class,
                "фото", "картинки", "photo", "медиа", "удаленные");

        addEntry(id++, "SaveMediaVoiceRounds", "Сохранять голосовые и кругляшки",
                null, "Сохранение удаленных голосовых сообщений и видеосообщений в Vault",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_camera, HundredGramSubSettingsActivity.CATEGORY_VAULT, 205,
                HundredGramSubSettingsActivity.class,
                "голосовые", "кругляшки", "voice", "round", "видеосообщения");

        addEntry(id++, "SaveMediaVideos", "Сохранять видеофайлы",
                null, "Сохранение удаленных видео в защищенное хранилище",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_media, HundredGramSubSettingsActivity.CATEGORY_VAULT, 206,
                HundredGramSubSettingsActivity.class,
                "видео", "video", "ролики");

        addEntry(id++, "SaveMediaFiles", "Сохранять файлы и документы",
                null, "Сохранение документов, архивов и других файлов",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_download, HundredGramSubSettingsActivity.CATEGORY_VAULT, 207,
                HundredGramSubSettingsActivity.class,
                "файлы", "документы", "архивы", "document", "files");

        addEntry(id++, "HundredGramMediaVaultLimit", "Лимит размера автозагрузки в хранилище",
                "HundredGramMediaVaultInfo", "Максимальный размер сохраняемого файла (включая режим «Без ограничений»)",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_archive, HundredGramSubSettingsActivity.CATEGORY_VAULT, 1,
                HundredGramSubSettingsActivity.class,
                "лимит", "размер", "мегабайт", "без ограничений", "unlimited", "максимальный");

        addEntry(id++, "HundredGramVaultViewer", "Просмотрщик медиа хранилища",
                "HundredGramVaultViewerSub", "Галерея всех сохраненных и удаленных медиафайлов",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_media, 0, 0,
                HundredGramVaultViewerActivity.class,
                "просмотрщик", "галерея", "хранилище", "viewer", "vault");

        addEntry(id++, "HundredGramVaultExportAllZip", "Экспорт хранилища в ZIP",
                null, "Выгрузка всех сохраненных сообщений и файлов в архив",
                "HundredGramCategoryVault", "Хранилище и медиа",
                R.drawable.msg_share, HundredGramSubSettingsActivity.CATEGORY_VAULT, 190,
                HundredGramSubSettingsActivity.class,
                "экспорт", "zip", "архив", "бэкап", "выгрузка");

        // 4. Интерфейс и оформление (Interface)
        addEntry(id++, "AvatarCornersRadius", "Закругление аватарок",
                "AvatarCornersInfo", "Настройка радиуса скругления аватарок пользователей и каналов (круглые/квадратные)",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_theme, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 195,
                HundredGramSubSettingsActivity.class,
                "аватарка", "скругление", "радиус", "круглые", "квадратные", "avatar", "corners");

        addEntry(id++, "MD3SwitchStyle", "Стиль переключателей Material 3 (MD3)",
                "MD3SwitchStyleInfo", "Современный дизайн тумблеров и чекбоксов в стиле Android 14/15",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 196,
                HundredGramSubSettingsActivity.class,
                "переключатели", "md3", "material", "тумблеры", "switch");

        addEntry(id++, "NewSliderStyle", "Стиль ползунков Material",
                "NewSliderStyleInfo", "Округлый трек и увеличенный бегунок в стиле Material",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_theme, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 391,
                HundredGramSubSettingsActivity.class,
                "ползунок", "слайдер", "slider", "seekbar", "material", "стиль");

        addEntry(id++, "HundredGramRoundVideoCrop", "Интерактивная обрезка кружочков",
                "HundredGramRoundVideoCropInfo", "Выбор области кадрирования перед отправкой видеосообщения",
                "RoundVideoHeader", "Кружочки",
                R.drawable.photo_circle, HundredGramSubSettingsActivity.CATEGORY_MEDIA, 392,
                HundredGramCameraActivity.class,
                "кружок", "видеосообщение", "обрезка", "round", "video", "crop", "кадрирование");

        addEntry(id++, "BottomNavShowArchive", "Вкладка «Архив» в нижней панели",
                null, "Отображение кнопки архива в нижней навигационной панели Liquid Glass",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_archive, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 380,
                HundredGramSubSettingsActivity.class,
                "архив", "панель", "нижняя", "навигация", "archive", "tabs");

        addEntry(id++, "BottomNavShowFeed", "Вкладка «Лента» в нижней панели",
                null, "Быстрый доступ к объединенной ленте публикаций каналов внизу экрана",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_channel, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 381,
                HundredGramSubSettingsActivity.class,
                "лента", "feed", "каналы", "посты", "вкладка");

        addEntry(id++, "BottomNavShowMusic", "Вкладка «Музыка» в нижней панели",
                null, "Вкладка плеера и плейлистов в нижней навигации",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_played, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 382,
                HundredGramSubSettingsActivity.class,
                "музыка", "плеер", "music", "аудио", "треки");

        addEntry(id++, "WidePostsInChannels", "Широкие посты в каналах",
                null, "Полноэкранное отображение публикаций на всю ширину экрана",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 215,
                HundredGramSubSettingsActivity.class,
                "широкие", "посты", "каналы", "wide", "posts");

        addEntry(id++, "RemoveMessageTail", "Убрать хвосты сообщений",
                null, "Скрыть выступающие стрелки/хвостики у облачков сообщений",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 214,
                HundredGramSubSettingsActivity.class,
                "хвосты", "хвостики", "облачка", "tail", "bubbles");

        addEntry(id++, "CenterActionBarTitle", "Заголовок чата по центру",
                null, "Центрирование имени собеседника и статуса в шапке чата",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 209,
                HundredGramSubSettingsActivity.class,
                "по центру", "заголовок", "шапка", "center", "actionbar");

        addEntry(id++, "ShowSecondsInTime", "Отображать секунды во времени сообщений",
                null, "Показывать часы, минуты и секунды (ЧЧ:ММ:СС) в чатах",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 95,
                HundredGramSubSettingsActivity.class,
                "секунды", "время", "часы", "seconds", "time");

        addEntry(id++, "ShowWeekdayInDate", "День недели в дате",
                null, "Показывать день недели в заголовках дат чата",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 300,
                HundredGramSubSettingsActivity.class,
                "день недели", "дата", "weekday", "date");

        addEntry(id++, "ForwardTime", "Время пересылки",
                null, "Показывать время оригинала в шапке пересланного сообщения",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 301,
                HundredGramSubSettingsActivity.class,
                "пересылка", "время", "forward", "time");

        addEntry(id++, "SelectionSpeed", "Скорость выделения сообщений",
                null, "Скорость анимации галочки при выборе сообщений",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 302,
                HundredGramSubSettingsActivity.class,
                "выделение", "скорость", "selection", "speed");

        addEntry(id++, "SelectionNoLimit", "Без лимита выделения",
                null, "Разрешить выбирать больше 100 сообщений одновременно",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 303,
                HundredGramSubSettingsActivity.class,
                "лимит", "выделение", "100", "selection", "limit");

        addEntry(id++, "MediaGlow", "Свечение медиа",
                null, "Подсветка фотографий и видео в чате",
                "HundredGramCategoryMediaQuality", "Медиа",
                R.drawable.msg_media, HundredGramSubSettingsActivity.CATEGORY_MEDIA, 304,
                HundredGramSubSettingsActivity.class,
                "свечение", "медиа", "glow", "media");

        addEntry(id++, "ThanosDeleteEffect", "Эффект удаления Thanos",
                null, "Анимация рассыпания сообщений при удалении",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 305,
                HundredGramSubSettingsActivity.class,
                "thanos", "удаление", "эффект", "delete", "effect");

        addEntry(id++, "ChatPageUpButton", "Кнопка «страница вверх»",
                null, "Кнопка для перехода на 100 сообщений назад в чате",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.pagedown, HundredGramSubSettingsActivity.CATEGORY_ACTIONS, 0,
                HundredGramSubSettingsActivity.class,
                "страница", "вверх", "page", "up", "100", "сообщений");

        addEntry(id++, "ResetAllSettings", "Сбросить все настройки",
                null, "Вернуть все параметры 100gram к значениям по умолчанию",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_reset, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 0,
                HundredGramSubSettingsActivity.class,
                "сброс", "настройки", "reset", "default");

        addEntry(id++, "HideStoriesProfile", "Скрыть истории в профилях",
                null, "Убрать кольцо историй с аватарок в профилях",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_stories_archive, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 0,
                HundredGramSubSettingsActivity.class,
                "истории", "профиль", "stories", "profile", "скрыть");

        addEntry(id++, "HideStoriesPosting", "Скрыть публикацию историй",
                null, "Убрать кнопки добавления и публикации историй",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_stories_add, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 0,
                HundredGramSubSettingsActivity.class,
                "истории", "публикация", "stories", "posting", "скрыть");

        addEntry(id++, "DeleteMyMessages", "Удалить мои сообщения",
                null, "Удалить все ваши сообщения в выбранных чатах",
                "HundredGramCategoryActions", "Действия",
                R.drawable.msg_chats_remove, HundredGramSubSettingsActivity.CATEGORY_ACTIONS, 0,
                HundredGramSubSettingsActivity.class,
                "удалить", "мои", "сообщения", "delete", "my", "messages");

        addEntry(id++, "LockHeader", "Блокировка",
                null, "Защита чатов, архива и удаления чатов биометрией или паролем устройства",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_permissions, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 0,
                HundredGramSubSettingsActivity.class,
                "блокировка", "lock", "биометрия", "отпечаток", "пароль");

        addEntry(id++, "Haptics", "Вибрация",
                null, "Сила вибрации и эффект для каждого действия",
                "HundredGramCategoryActions", "Действия",
                R.drawable.msg_settings, HundredGramSubSettingsActivity.CATEGORY_ACTIONS, 0,
                HundredGramSubSettingsActivity.class,
                "вибрация", "вибро", "haptics", "vibration", "тактильный");

        addEntry(id++, "FolderSwipe", "Анимация свайпа папок",
                null, "Уменьшение страниц и пружинная анимация при перелистывании папок",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_folders, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 383,
                HundredGramSubSettingsActivity.class,
                "папки", "свайп", "анимация", "folder", "swipe", "spring");

        addEntry(id++, "HighlightDeleted", "Выделять удаленные сообщения цветом",
                null, "Визуальная цветная рамка или плашка вокруг удаленных сообщений",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 37,
                HundredGramSubSettingsActivity.class,
                "удаленные", "подсветка", "выделение", "highlight", "deleted");

        addEntry(id++, "SquareFab", "Квадратная плавающая кнопка (Square FAB)",
                null, "Современный квадратный стиль кнопки создания нового чата",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_discussion, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 233,
                HundredGramSubSettingsActivity.class,
                "кнопка", "fab", "квадратная", "создать чат");

        // 5. Конфиденциальность (Privacy)
        addEntry(id++, "HidePhoneNumber", "Скрыть номер телефона в меню",
                null, "Скрывает номер телефона из бокового меню и настроек профиля",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_permissions, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 20,
                HundredGramSubSettingsActivity.class,
                "номер", "телефон", "скрыть", "phone", "number");

        addEntry(id++, "HideOtherPhoneNumbers", "Скрыть чужие номера телефонов",
                null, "Не отображать номера телефонов других пользователей в их профилях",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_permissions, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 21,
                HundredGramSubSettingsActivity.class,
                "чужие", "номера", "профиль", "конфиденциальность");

        addEntry(id++, "HideUsername", "Скрыть имя пользователя (юзернейм)",
                null, "Скрывает ваш юзернейм в профиле",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_permissions, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 22,
                HundredGramSubSettingsActivity.class,
                "юзернейм", "username", "логин", "собачка");

        addEntry(id++, "ConfirmCalls", "Подтверждение перед звонком",
                null, "Предотвращает случайные нажатия на кнопку звонка диалогом подтверждения",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_permissions, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 38,
                HundredGramSubSettingsActivity.class,
                "звонки", "подтверждение", "случайный вызов", "calls", "confirm");

        addEntry(id++, "CleanUrls", "Очистка ссылок от трекинга (Clean URLs)",
                null, "Автоматическое удаление параметров utm_source, fbclid, yclid из пересылаемых ссылок",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_permissions, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 39,
                HundredGramSubSettingsActivity.class,
                "ссылки", "трекинг", "слежка", "utm", "clean", "urls");

        addEntry(id++, "AdBlock", "Блокировка рекламы в каналах",
                null, "Скрывает официальные спонсированные сообщения Telegram в каналах",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_permissions, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 350,
                HundredGramSubSettingsActivity.class,
                "реклама", "адблок", "adblock", "спонсорские", "блокировка");

        addEntry(id++, "BypassContentProtection", "Обход защиты контента (копирование и сохранение)",
                null, "Разрешает копирование текста, сохранение медиа и снятие скриншотов в защищенных чатах",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_permissions, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 351,
                HundredGramSubSettingsActivity.class,
                "защита", "копирование", "скриншот", "запрет", "noforwards", "protect", "copy");

        // 6. Сеть и буст (Network Boost)
        addEntry(id++, "NetworkBoostTitle", "Буст скорости загрузки и сети",
                "NetworkBoostSubtitle", "Многопоточная параллельная загрузка файлов и ускорение выгрузки",
                "NetworkBoostTitle", "Буст сети",
                R.drawable.msg_stats, 0, 0,
                HundredGramNetworkBoostActivity.class,
                "сеть", "буст", "скорость", "загрузка", "скачивание", "потоки", "ultra", "fast", "speed");

        // 7. Трекер активности (Activity Tracker)
        addEntry(id++, "HundredGramTrackOnlineStatus", "Отслеживание онлайна пользователей",
                "HundredGramTrackOnlineStatusInfo", "Ведение точного журнала времени в сети выбранных контактов",
                "HundredGramCategoryTracker", "Трекер активности",
                R.drawable.msg_usersearch, HundredGramSubSettingsActivity.CATEGORY_TRACKER, 10,
                HundredGramSubSettingsActivity.class,
                "трекер", "слежка", "онлайн", "сессии", "tracker", "activity", "online");

        addEntry(id++, "HundredGramNotifyUserOnline", "Уведомлять при входе в сеть",
                null, "Всплывающее push-уведомление, когда отслеживаемый контакт заходит в сеть",
                "HundredGramCategoryTracker", "Трекер активности",
                R.drawable.msg_usersearch, HundredGramSubSettingsActivity.CATEGORY_TRACKER, 11,
                HundredGramSubSettingsActivity.class,
                "уведомление", "вход", "онлайн", "пуш", "notify");

        addEntry(id++, "HundredGramTrackSelfActivity", "Логирование собственной активности",
                null, "Календарь и статистика собственного времени, проведенного в Telegram",
                "HundredGramCategoryTracker", "Трекер активности",
                R.drawable.msg_calendar2, HundredGramSubSettingsActivity.CATEGORY_TRACKER, 187,
                HundredGramSubSettingsActivity.class,
                "календарь", "мое время", "статистика", "self", "log");

        addEntry(id++, "AvatarHistory", "История смены аватарок",
                "AvatarHistoryInfo", "Сохранение всех старых аватарок собеседников и каналов локально",
                "HundredGramCategoryTracker", "Трекер активности",
                R.drawable.msg_usersearch, HundredGramSubSettingsActivity.CATEGORY_TRACKER, 97,
                HundredGramSubSettingsActivity.class,
                "история аватарок", "аватарки", "фото профиля", "avatar", "history");

        // 8. ИИ и нейросети (AI Settings)
        addEntry(id++, "AiServicesTitle", "ИИ Помощник и кастомные сервисы",
                "AiServicesSubtitle", "Подключение OpenAI, DeepSeek, Claude, локальных моделей, системные роли и промпты",
                "AiServicesTitle", "ИИ Помощник",
                R.drawable.msg_bot, 0, 0,
                HundredGramAiSettingsActivity.class,
                "ии", "нейросеть", "ai", "chatgpt", "openai", "deepseek", "claude", "промпты", "роли");

        // 9. Плагины (Plugins)
        addEntry(id++, "PluginsTitle", "Менеджер плагинов",
                "PluginsSubtitle", "Управление расширениями, модулями и скриптами форка",
                "PluginsTitle", "Плагины",
                R.drawable.msg_bot, 0, 0,
                HundredGramPluginsActivity.class,
                "плагины", "расширения", "plugins", "модули", "скрипты");

        // 10. Аудиоплеер и Музыка (Music & Playlists)
        addEntry(id++, "HundredGramMusicTitle", "Музыка и плейлисты",
                "HundredGramMusicSubtitle", "Аудиоплеер с плавающим мини-баром, плейлистами и селектором каналов",
                "HundredGramMusicTitle", "Музыка",
                R.drawable.msg_played, 0, 0,
                HundredGramMusicActivity.class,
                "музыка", "плеер", "плейлисты", "песни", "треки", "music", "player", "playlist");

        // 11. Наборы иконок (Icon Packs)
        addEntry(id++, "HundredGramIconPacks", "Наборы иконок приложения",
                "HundredGramIconPacksSub", "Кастомизация стиля иконок меню, чатов и табов",
                "HundredGramIconPacks", "Иконки",
                R.drawable.msg_theme, 0, 0,
                HundredGramIconPacksActivity.class,
                "иконки", "паки", "темы", "стиль", "icon", "packs");

        // 12. Виджеты PillStack
        addEntry(id++, "PillStackPills", "Виджеты PillStack",
                "PillStackPillsInfo", "Курсы криптовалют (BTC/USD) и погода в чатах",
                "PillStackPills", "Виджеты",
                R.drawable.weather_cloudy, 0, 0,
                HundredGramPillStackSettingsActivity.class,
                "виджеты", "погода", "крипта", "биткоин", "pillstack", "btc", "курс");

        // 13. Действия по свайпу (Swipe Actions)
        addEntry(id++, "SwipeActionsTitle", "Действия по свайпу",
                "SwipeActionsSubtitle", "Настройка действий при свайпе влево и вправо по сообщению в чате",
                "HundredGramCategoryActions", "Действия",
                R.drawable.msg_actions, 0, 0,
                HundredGramSwipeActionsActivity.class,
                "свайп", "жесты", "ответ", "перевод", "цитирование", "swipe", "actions");

        // 13b. Пункты меню сообщения (Message Menu)
        addEntry(id++, "MessageMenuCustomize", "Пункты меню сообщения",
                "MessageMenuInfo", "Порядок и видимость пунктов контекстного меню сообщения",
                "HundredGramCategoryActions", "Действия",
                R.drawable.msg_list, 0, 0,
                HundredGramMessageMenuActivity.class,
                "меню", "сообщение", "пункты", "порядок", "скрыть", "menu", "items", "context", "reorder");

        // 14. Режим стримера (Streamer Mode)
        addEntry(id++, "StreamerMode", "Режим стримера",
                "StreamerModeInfo", "Скрытие баланса звезд, телефонного номера, ID и подмена имени во время трансляций",
                "HundredGramCategoryStreamer", "Режим стримера",
                R.drawable.msg_channel, HundredGramSubSettingsActivity.CATEGORY_STREAMER, 1,
                HundredGramSubSettingsActivity.class,
                "стример", "streamer", "трансляция", "скрыть баланс", "фейковое имя");

        // 15. Material Design 3 (MD3) Suite
        addEntry(id++, "Md3Master", "Material Design 3 (MD3)",
                "Md3MasterInfo", "Включить современный дизайн Material 3 для интерфейса приложения",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_theme, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 401,
                HundredGramSubSettingsActivity.class,
                "material", "md3", "материал", "дизайн", "тема");

        addEntry(id++, "Md3ChatList", "Список чатов MD3",
                "Md3ChatListInfo", "Отображение диалогов в виде скруглённых карточек с отступами",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_chats_remove, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 402,
                HundredGramSubSettingsActivity.class,
                "material", "md3", "чаты", "карточки", "dialogs", "cards");

        addEntry(id++, "Md3Sections", "Секции и блоки MD3",
                "Md3SectionsInfo", "Скруглённые контейнеры разделов и списков с разделяющими отступами",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_list, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 403,
                HundredGramSubSettingsActivity.class,
                "material", "md3", "секции", "блоки", "containers", "sections");

        addEntry(id++, "Md3Controls", "Элементы управления MD3",
                "Md3ControlsInfo", "Стилизованные кнопки, чипы и поверхности нажатия Material 3",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_settings, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 404,
                HundredGramSubSettingsActivity.class,
                "material", "md3", "кнопки", "чипы", "controls", "buttons");

        addEntry(id++, "Md3Menus", "Контекстные меню MD3",
                "Md3MenusInfo", "Плавающие скруглённые всплывающие и контекстные меню",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_openin, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 405,
                HundredGramSubSettingsActivity.class,
                "material", "md3", "меню", "всплывающие", "контекстное", "popup");

        addEntry(id++, "Md3NavigationStyle", "Стиль нижней навигации",
                null, "Классический, плавающая капсула или Material 3 Expressive",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_theme, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 406,
                HundredGramSubSettingsActivity.class,
                "material", "md3", "навигация", "капсула", "pill", "navigation", "expressive");

        addEntry(id++, "Md3MediaLoader", "Индикатор загрузки MD3",
                "Md3MediaLoaderInfo", "Круглый анимированный индикатор загрузки медиафайлов",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_download, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 407,
                HundredGramSubSettingsActivity.class,
                "material", "md3", "загрузка", "индикатор", "кольцо", "loader");

        addEntry(id++, "Md3MediaLoaderThickness", "Толщина кольца загрузчика",
                null, "Настройка толщины обводки индикатора загрузки от 2 до 8 dp",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_download, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 408,
                HundredGramSubSettingsActivity.class,
                "толщина", "кольцо", "loader", "thickness");

        addEntry(id++, "Md3MediaLoaderWave", "Волновой эффект кольца",
                "Md3MediaLoaderWaveInfo", "Плавная динамическая волна кольца при загрузке медиа",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_download, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 409,
                HundredGramSubSettingsActivity.class,
                "волна", "эффект", "wave", "loader");

        addEntry(id++, "StickerPackOwnerTitle", "Показывать автора стикерпака",
                "StickerPackOwnerInfo", "Отображать имя или юзернейм создателя в окне предпросмотра стикеров с переходом в профиль",
                "HundredGramCategoryMediaQuality", "Медиа",
                R.drawable.msg_sticker, HundredGramSubSettingsActivity.CATEGORY_MEDIA, 410,
                HundredGramSubSettingsActivity.class,
                "стикер", "автор", "создатель", "пак", "sticker", "owner", "author");

        addEntry(id++, "SecretViewTTL", "Секретный просмотр таймерных медиа",
                "SecretViewTTLInfo", "Просмотр самоуничтожающихся фото и видео без запуска таймера удаления и отметки о прочтении",
                "HundredGramCategoryPrivacy", "Конфиденциальность",
                R.drawable.msg_secret, HundredGramSubSettingsActivity.CATEGORY_PRIVACY, 411,
                HundredGramSubSettingsActivity.class,
                "таймер", "самоуничтожение", "секретный", "просмотр", "ttl", "secret", "view");

        addEntry(id++, "SocialLinkFixer", "Автофикс ссылок соцсетей",
                "SocialLinkFixerInfo", "Автоматическая замена ссылок X (Twitter), Instagram, TikTok и Pixiv на сервисы с поддержкой полного предпросмотра",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_link, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 412,
                HundredGramSubSettingsActivity.class,
                "ссылка", "фикс", "твиттер", "инстаграм", "превью", "fxtwitter", "fixupx", "ddinstagram");

        addEntry(id++, "ChatBoxStyle", "Стиль поля ввода",
                "ChatBoxStyleInfo", "Настройка внешнего вида панели ввода сообщений в стиле Material Design",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_edit, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 413,
                HundredGramSubSettingsActivity.class,
                "поле ввода", "панель ввода", "капсула", "плавающий", "chatbox", "pill", "input");

        addEntry(id++, "GlassMessages", "Стеклянные пузыри сообщений",
                "GlassMessagesInfo", "Полупрозрачный матовый стеклянный эффект для пузырей сообщений в чате",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_theme, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 414,
                HundredGramSubSettingsActivity.class,
                "стекло", "прозрачность", "пузыри", "сообщения", "glass", "bubble", "transparent");

        addEntry(id++, "GlassMessagesTransparency", "Прозрачность пузырей",
                "GlassMessagesInfo", "Степень прозрачности стеклянных пузырей сообщений",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_theme, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 415,
                HundredGramSubSettingsActivity.class,
                "прозрачность", "уровень", "стекло", "transparency", "glass");

        addEntry(id++, "GlassMessagesBorder", "Контур пузырей",
                "GlassMessagesBorderInfo", "Тонкая аккуратная обводка вокруг контура пузырей сообщений",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_theme, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 416,
                HundredGramSubSettingsActivity.class,
                "контур", "обводка", "граница", "border", "outline", "glass");

        addEntry(id++, "StickerQuotes", "Стикер-цитаты",
                "StickerQuotesInfo", "Создание красивых стикер-карточек из сообщений для отправки в чат",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.msg_sticker, HundredGramSubSettingsActivity.CATEGORY_ACTIONS, 417,
                HundredGramSubSettingsActivity.class,
                "стикер", "цитата", "стикерпак", "карточка", "quote", "sticker");

        addEntry(id++, "LyricsInPlayer", "Тексты песен",
                "LyricsInPlayerDesc", "Извлекать и отображать тексты песен из ID3-тегов и LRCLIB в аудиоплеере",
                "HundredGramCategoryMedia", "Медиа",
                R.drawable.msg_text_outlined, HundredGramSubSettingsActivity.CATEGORY_MEDIA, 418,
                HundredGramSubSettingsActivity.class,
                "текст", "слова", "песня", "lyrics", "lrc", "музыка", "трек", "плеер");

        addEntry(id++, "Md3AudioPlayer", "MD3 аудиоплеер",
                "Md3AudioPlayerDesc", "Material You скругление обложки, акцентные кнопки и современный стиль в плеере",
                "HundredGramCategoryMedia", "Медиа",
                R.drawable.msg_media, HundredGramSubSettingsActivity.CATEGORY_MEDIA, 419,
                HundredGramSubSettingsActivity.class,
                "плеер", "музыка", "аудио", "md3", "player", "material", "обложка");

        addEntry(id++, "SlowmodeQueue", "Очередь Slowmode",
                "SlowmodeQueueDesc", "Ставить сообщения в очередь и отправлять автоматически по истечении таймера slowmode",
                "HundredGramCategoryActions", "Действия",
                R.drawable.input_schedule, HundredGramSubSettingsActivity.CATEGORY_ACTIONS, 420,
                HundredGramSubSettingsActivity.class,
                "слоумод", "очередь", "медленный режим", "таймер", "slowmode", "queue");

        addEntry(id++, "HundredGramCustomBadges", "Кастомные значки пользователей",
                "HundredGramCustomBadgesInfo", "Локальные значки пользователей с иконкой возле ника, описанием и заголовком",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.verified_check, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 395,
                HundredGramSubSettingsActivity.class,
                "значок", "бейдж", "иконка", "badge", "badges", "кастомные бейджи", "статус", "вериф");

        addEntry(id++, "HundredGramManageBadges", "Управление значками",
                "HundredGramManageBadgesDesc", "Просмотр, создание и редактирование кастомных значков для пользователей",
                "HundredGramCategoryInterface", "Интерфейс",
                R.drawable.verified_check, HundredGramSubSettingsActivity.CATEGORY_INTERFACE, 396,
                org.telegram.ui.HundredGramBadgesActivity.class,
                "значки", "бейджи", "управление значками", "badges", "custom badge");
    }

    private void addEntry(int id, String titleKey, String defaultTitle,
                          String subtitleKey, String defaultSubtitle,
                          String categoryTitleKey, String defaultCategoryTitle,
                          int categoryIcon, int category, int itemId,
                          Class<? extends BaseFragment> targetClass,
                          String... keywords) {
        entries.add(new Entry(id, titleKey, defaultTitle, subtitleKey, defaultSubtitle,
                categoryTitleKey, defaultCategoryTitle, categoryIcon, category, itemId, targetClass, keywords));
    }

    public List<MatchResult> search(String query) {
        return search(query, null);
    }

    public List<MatchResult> search(String query, String categoryFilter) {
        if (TextUtils.isEmpty(query)) {
            return Collections.emptyList();
        }

        String q = query.trim().toLowerCase(Locale.ROOT);
        String[] queryTokens = q.split("\\s+");

        ArrayList<MatchResult> results = new ArrayList<>();

        for (Entry entry : entries) {
            if (!TextUtils.isEmpty(categoryFilter) && !"all".equalsIgnoreCase(categoryFilter)) {
                String catName = entry.getCategoryTitle().toLowerCase(Locale.ROOT);
                if (!catName.contains(categoryFilter.toLowerCase(Locale.ROOT))) {
                    continue;
                }
            }

            String title = entry.getTitle().toLowerCase(Locale.ROOT);
            String subtitle = entry.getSubtitle().toLowerCase(Locale.ROOT);
            String category = entry.getCategoryTitle().toLowerCase(Locale.ROOT);

            int score = 0;
            int matchTitleStart = -1;
            int matchTitleEnd = -1;

            boolean allTokensMatched = true;

            for (String token : queryTokens) {
                boolean tokenFound = false;

                // Match in Title
                int titleIdx = title.indexOf(token);
                if (titleIdx >= 0) {
                    tokenFound = true;
                    if (titleIdx == 0) {
                        score += 100; // Starts with query
                    } else {
                        score += 50;  // Contains in title
                    }
                    if (matchTitleStart == -1) {
                        matchTitleStart = titleIdx;
                        matchTitleEnd = titleIdx + token.length();
                    }
                }

                // Match in Subtitle
                if (subtitle.contains(token)) {
                    tokenFound = true;
                    score += 25;
                }

                // Match in Category
                if (category.contains(token)) {
                    tokenFound = true;
                    score += 20;
                }

                // Match in Keywords
                if (entry.keywords != null) {
                    for (String kw : entry.keywords) {
                        if (kw.toLowerCase(Locale.ROOT).contains(token)) {
                            tokenFound = true;
                            score += 35;
                            break;
                        }
                    }
                }

                if (!tokenFound) {
                    allTokensMatched = false;
                    break;
                }
            }

            if (allTokensMatched && score > 0) {
                // Bonus for shorter exact match
                if (title.equals(q)) {
                    score += 200;
                }
                results.add(new MatchResult(entry, score, matchTitleStart, matchTitleEnd));
            }
        }

        Collections.sort(results);
        return results;
    }

    public List<String> getRecentSearches() {
        Set<String> set = preferences.getStringSet("recent_searches", null);
        if (set == null || set.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    public void addRecentSearch(String query) {
        if (TextUtils.isEmpty(query)) return;
        query = query.trim();
        if (query.length() < 2) return;

        Set<String> set = preferences.getStringSet("recent_searches", null);
        Set<String> updated = set != null ? new HashSet<>(set) : new HashSet<>();
        updated.add(query);
        if (updated.size() > 10) {
            // keep up to 10
            String oldest = updated.iterator().next();
            updated.remove(oldest);
        }
        preferences.edit().putStringSet("recent_searches", updated).apply();
    }

    public void clearRecentSearches() {
        preferences.edit().remove("recent_searches").apply();
    }
}
