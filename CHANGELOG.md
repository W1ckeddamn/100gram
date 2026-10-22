# Журнал изменений (100gram Changelog)

Все заметные изменения в проекте **100gram** (кастомный форк Telegram для Android) документируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.0.0/), и проект следует принципам семантического версионирования в связке с апстрим-версией Telegram: `[ВЕРСИЯ_TELEGRAM-vX.Y.Z]`.

---

## [Unreleased]

- [Vault/Viewer] Масштабное улучшение Сейфа медиа: контекстное меню, статистика по типам и чатам, единые фильтры и непрерывный мультивыбор по краю:
  - Восстановлено контекстное меню при долгом нажатии (`showItemContextMenu`), а также добавлена кнопка «три точки» на карточках файлов и голосовых сообщений для мгновенного доступа к действиям: открыть, сохранить в галерею/загрузки, перейти к исходному сообщению в чате, поделиться, посмотреть детальные метаданные, удалить или выбрать.
  - Добавлено отображение занимаемого дискового пространства по каждому типу файлов: подсчёт и вывод размера прямо во вкладках (`Фото (XX МБ)`, `Видео (XX МБ)`, `Голосовые (XX МБ)`, `Файлы (XX МБ)`), а также в информационной полосе быстрого обзора под вкладками.
  - Реализован единый экран «Хранилище и фильтры» (`showChatsAndStorageBottomSheet`), устраняющий дублирующие меню: отображение детальной статистики хранилища по типам, быстрых фильтров по размеру (>10 МБ, >50 МБ, >100 МБ) и списка всех чатов с сортировкой по занимаемому месту (название, количество файлов, точный объём в МБ/ГБ) и фильтрацией в один клик.
  - Внедрён непрерывный жестовый мультивыбор (`VaultEdgeDragSelector`): при проведении пальцем к правому краю строки и движении вниз/вверх вдоль края приложение автоматически активирует режим выбора, выделяет строки на лету с тактильным откликом (`vibrateCursor`) и плавно прокручивает список в зонах автоскролла.

- [Gifts/Avatar] Отображение аватарки дарителя на карточках уникальных подарков в списке/сетке профиля:
  - В карточках подарков профиля ([`GiftSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Gifts/GiftSheet.java)) снято официальное ограничение Telegram Android, скрывавшее аватарку отправителя для уникальных и коллекционных подарков (`TL_starGiftUnique`).
  - Добавлено извлечение дарителя из `userGift.from_id` и атрибутов минта `starGiftAttributeOriginalDetails.sender_id`, с поддержкой маски инкогнито для скрытых имен (`name_hidden`) и фоллбэком на SQLite БД `MessagesStorage`.
  - Реализовано динамическое смещение аватарки `translationX(+22dp)` при закреплении уникального подарка вверху профиля (`pinnedView`), исключающее визуальное наложение аватарки на значок булавки (pin icon).
  - Обеспечена поддержка чтения дарителя для специальных возможностей (`AccessibilityNodeInfo`) и отрисовки в контекстном меню `customDraw`.

- [Gifts/Fix] Отображение отправителя («От кого») для уникальных и коллекционных подарков (Unique / NFT Gifts):
  - Исправлен баг Telegram Android, из-за которого в карточке уникального/улучшенного подарка ([`StarGiftSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Stars/StarGiftSheet.java)) строка «От кого» (`Gift2From`) не отображалась вовсе, в отличие от Telegram Desktop.
  - Добавлено корректное извлечение отправителя подарка с поддержкой всех источников данных: `savedStarGift.from_id`, действие в чате `TL_messageActionStarGiftUnique.from_id` и исходные атрибуты `starGiftAttributeOriginalDetails.sender_id`.
  - Реализовано отображение строки отправителя под строкой владельца с аватаром, эмодзи-статусом, кликом для перехода в профиль, поддержкой анонимных отправителей («Анонимный даритель»), а также даты дарения и прикрепленного комментария к подарку.
  - В [`TableView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/TableView.java) добавлен защитный фоллбэк на локальную БД SQLite (`MessagesStorage`), предотвращающий появление пустых имен или статуса удаленного аккаунта, если профиль дарителя еще не был предварительно закэширован в оперативной памяти.

- [UI/Settings] Универсальный поиск по настройкам и Редизайн интерфейса 2.0:
  - Создан поисковый индекс [`HundredGramSettingsSearchIndex.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramSettingsSearchIndex.java), индексирующий свыше 35 ключевых параметров форка 100gram на двух языках (русский и английский), с синонимами, ранжированием релевантности и историей недавних запросов.
  - В [`HundredGramSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java) интегрирован поиск: поисковая кнопка в ActionBar, плавающая карточка поиска в шапке, горизонтальная панель фильтрации по категориям (Все, Призрак, Хранилище, Интерфейс, Сеть, Имитатор, ИИ), карточки результатов поиска [`HundredGramSearchResultCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramSearchResultCell.java) с подсветкой совпадений и глубокий переход к искомой настройке.
  - В [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) реализована автоматическая плавная прокрутка к найденному элементу и визуальная пульсирующая подсветка строки через `listView.highlightRow()`.
  - В поисковый адаптер основного экрана настроек Telegram ([`ProfileActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java)) добавлены параметры 100gram для сквозного обнаружения настроек форка из общего поиска Telegram.
  - Обновлена Hero-панель [`HundredGramSettingsHeroCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramSettingsHeroCell.java) со статусами и быстрым переходом к симулятору активности и сетевому бусту.
  - Добавлен тактильный отклик (haptic feedback) при нажатиях на табы нижней навигации Liquid Glass в [`MainTabsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java).

- [Presence/Simulator] Имитация активности в сети и планировщик сессий:
  - Создан менеджер симуляции [`HundredGramActivitySimulator.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramActivitySimulator.java), обеспечивающий поддержание статуса «В сети» на протяжении заданного пользователем времени (быстрый запуск на 15/30/45 мин, 1/2/4/8 часов или произвольное время).
  - Реализован планировщик сессий: автоматический вход в сеть в точно заданное время (через `AlarmManager` с пробуждением устройства) и нахождение в сети заданное количество минут/часов с возможностью однократного или ежедневного повторения.
  - Добавлен системный ресивер [`HundredGramActivitySimulatorReceiver.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramActivitySimulatorReceiver.java) и восстановление запланированных задач после перезагрузки устройства в [`HundredGramBootReceiver.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramBootReceiver.java).
  - Интеграция в сетевой цикл [`MessagesController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java) и [`SendMessagesHelper.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java): блокировка отправки `offline` во время активной симуляции даже при выключенном экране или включенном «Призрачном режиме».
  - Поддержка реалистичных микро-перерывов (1.5 мин каждые 15 мин) для имитации естественного поведения живого человека.
  - Экран управления [`HundredGramActivitySimulatorActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramActivitySimulatorActivity.java) с живой карточкой статуса, таймером обратного отсчета в реальном времени, карточками настроек в [`HundredGramSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java) и разделах Призрачного режима и Трекера активности [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java).

- [Chat/Menu] Удалена кнопка «Стереть навсегда из базы» из контекстного меню сообщений:
  - В [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) удалено добавление кнопки `DeletePermanently` («Стереть навсегда из базы») из контекстного меню удалённых сообщений, исключая загромождение меню и случайное безвозвратное стирание локальной истории anti-delete.

- [Vault/Limit] Опция «Без ограничений» для максимального размера файлов в архиваторе:
  - В настройках Архиватора и медиа ([`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java)) и персональных настройках чатов ([`HundredGramChatVaultBottomSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramChatVaultBottomSheet.java), [`HundredGramChatSettingsBottomSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramChatSettingsBottomSheet.java)) добавлена опция «Без ограничений» (Unlimited) для максимального размера сохраняемых файлов.
  - В менеджере хранилища ([`HundredGramVaultManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramVaultManager.java)), фоновом архиваторе ([`HundredGramBackgroundArchiver.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramBackgroundArchiver.java)) и правилах фильтрации ([`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java)) реализована поддержка снятия ограничения размера (пропуск проверки размера при значении `<= 0` / `Long.MAX_VALUE`).
  - Расширен спектр доступных лимитов (5 МБ, 15 МБ, 25 МБ, 50 МБ, 100 МБ, 250 МБ, 500 МБ, 1 ГБ, Без ограничений) с корректным форматированием единиц измерения.

- [Feed/Core] Полная интеграция Ленты каналов (Channels Feed) в чат-движок:
  - Интеграция `FeedChatIntegration` в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java), объединяющая пагинацию постов каналов, разделитель непрочитанных постов, контекстные меню, реакции и нативный переход к оригинальному посту по нажатию на аватар канала.
  - Поддержка широких постов (`Wide Posts`) для постов каналов в ленте в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java).
  - Интеграция переключателя ленты в боковом меню (Drawer) и вкладки «Лента» с динамическим счётчиком непрочитанных в нижней навигационной панели [`DialogsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java).

- [Music/Player] Редизайн экрана музыки и плавающий мини-плеер:
  - В [`HundredGramMusicActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramMusicActivity.java) реализован современный плавающий мини-плеер внизу экрана с обложкой трека, названием, исполнителем, кнопками play/pause/next, линией прогресса и отступом над нижней навигационной панелью.
  - Селектор источника музыки (Избранное или любой канал/чат) с мгновенным переключением библиотеки треков.
  - Плейлисты прямо на экране музыки: воспроизведение в один клик без перехода на другие экраны, меню переименования и удаления плейлистов.

- [Dialogs/Fix] Исправление наложения миниатюры медиа на имя отправителя в списке чатов:
  - В [`DialogCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java) устранён рассинхрон позиционирования превью медиа (`thumbImage`) при включённых мини-аватарках отправителя (`senderMiniAvatars`).
  - Ранее отступ-разделитель после мини-аватарки создавался как `FixedWidthSpan`, из-за чего метод `updateThumbsPosition()` ошибочно принимал 1dp разделитель за спан превью и позиционировал картинку поверх первых букв имени автора сообщения, оставляя пустой зазор между двоеточием и текстом с эмодзи.
  - Введён специализированный класс `ThumbFixedWidthSpan` для превью медиа и независимый `AvatarGapSpan` для отступа мини-аватарки; позиция `thumbImage` теперь вычисляется строго после имени отправителя в соответствии с зарезервированным местом.
  - Учтён отступ мини-аватарки при расчёте ширины обрезки (`TextUtils.ellipsize`) и добавлена корректная поддержка для трёхстрочного макета диалогов (`threeLinesLayout`).

- [Dialogs/Avatar] Мини-аватары авторов сообщений только в группах:
  - В [`DialogCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java) мини-аватар автора в строке последнего сообщения отключен для личных диалогов (1-на-1) и каналов; отображается исключительно в группах и супергруппах.

- [Chat/Status] Исправление бага дублирования галочки статуса:
  - В [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) исправлена анимация смены статуса сообщения, отменяющая предыдущие аниматоры и предотвращающая визуальное дублирование одинарной галочки левее текущей иконки.

- [Feed/CustomFeeds] Пользовательские кастомные ленты каналов (Custom Feeds):
  - Создан менеджер [`CustomFeedManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/feed/CustomFeedManager.java) и модель [`CustomFeed.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/feed/CustomFeed.java) с JSON-сохранением конфигураций.
  - Поддержка создания неограниченного числа тематических лент с индивидуальным набором выбранных каналов.
  - Интерактивный диалог переключения и создания лент в ActionBar [`FeedActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/feed/ui/FeedActivity.java).
  - Персонализированный выбор каналов для каждой ленты в [`FeedChannelsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/feed/ui/FeedChannelsActivity.java).

- [Navigation/BottomBar] Добавление Архива и Ленты каналов в нижнюю навигацию:
  - В [`MainTabsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java) и [`GlassTabView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabView.java) добавлены табы «Архив» и «Лента».
  - В [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) добавлены настройки отображения вкладок в нижней панели.

- [Player/Playlists] Пользовательские плейлисты в музыкальном плеере:
  - Создан менеджер [`HundredGramPlaylistManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramPlaylistManager.java) для локального хранения музыкальных плейлистов с полной сериализацией TL-структур треков.
  - В аудиоплеере [`AudioPlayerAlert.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/AudioPlayerAlert.java) добавлены кнопки управления плейлистами в ActionBar, меню действий и пункт добавления текущего трека в любой плейлист из списка воспроизведения.

- [Chat/ReadHistory] Локальное сохранение времени прочтения сообщений (Read History Forever):
  - Создан контроллер и SQLite база [`HundredGramReadHistoryManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramReadHistoryManager.java), отслеживающий входящие socket-обновления `TL_updateReadHistoryOutbox` и кэширующий точные отметки времени прочтения для групп и ЛС.
  - В [`MessageSeenView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/MessageSeenView.java) и [`MessagePrivateSeenView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/MessagePrivateSeenView.java) интегрировано локальное чтение истории прочтений в обход 7-дневного серверного тайм-аута Telegram.
  - Сняты ограничения в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) на просмотр времени прочтения старых сообщений.

- [Chat/QuickBan] Быстрый бан в группах:
  - В контекстное меню сообщений в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) для администраторов групп с правом бана добавлен пункт «Быстрый бан» с диалогом подтверждения и опцией удаления всех сообщений пользователя.

- [Browser/AdBlock] Встроенный блокировщик рекламы в веб-браузере (AdBlock):
  - Полноценная интеграция нативного движка блокировки рекламы `libetgadblock.so` и списков фильтров EasyList / uBlock Origin.
  - Модули автоматической загрузки и кеширования списков фильтров [`SubscriptionsManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/adblock/SubscriptionsManager.java) и скриптлетов [`ScriptletsManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/adblock/ScriptletsManager.java).
  - Перехват сетевых запросов трекеров и рекламных баннеров через `shouldInterceptRequest` в [`BotWebViewContainer.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/web/BotWebViewContainer.java) с инъекцией CSS-селекторов сокрытия элементов и JS-скриптлетов в DOM страниц.
  - Отображение счетчика заблокированной рекламы в меню веб-действий [`WebActionBar.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/web/WebActionBar.java), экран предупреждения о фишинговых доменах и тумблер в настройках [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java).

- [Profile/Music] Музыкальный статус Now Playing в профилях пользователей:
  - Интерактивная карточка воспроизводимого трека [`NowPlayingCard.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/NowPlayingCard.java) с динамическим акцентным градиентом, векторными плавающими нотами, бейджем стримингового сервиса (Apple Music, Spotify, VK, Telegram, SoundCloud, Яндекс.Музыка) и встроенным ExoPlayer плеером для 30-секундных превью.
  - Контроллер [`NowPlayingController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/NowPlayingController.java) с резолвингом Saved Music Telegram (`userInfo.saved_music`) и аудиопоиском через iTunes Search API.
  - Полная интеграция в экран профиля [`ProfileActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java) с периодическим опросом статуса и обработкой кликов по разделу музыки.

- [Feed/Channels] Агрегированная единая лента каналов (Channels Feed):
  - Модуль общей ленты всех каналов в хронологическом порядке [`FeedActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/feed/ui/FeedActivity.java) и экран гибкого управления подписками ленты [`FeedChannelsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/feed/ui/FeedChannelsActivity.java).
  - Архитектура таймлайн-загрузчика сообщений [`FeedTimelineLoader.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/feed/loader/FeedTimelineLoader.java) и контроллера [`FeedController.java`](file:///D:/100gram/TMessagesProj/src/main/java/com/exteragram/messenger/feed/FeedController.java) с поддержкой отметки всех постов прочитанными, фильтрацией архивных каналов и полноэкранного скроллинга.
  - Удобные точки входа: кнопка «Лента каналов» в главном меню [`DialogsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java) рядом с «Избранным» и карточка конфигурации ленты в [`HundredGramSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java).

- [Vault/UI] Выделение проведением (Drag-to-select) в Сейфе:
  - В [`HundredGramVaultViewerActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramVaultViewerActivity.java) интегрирован `RecyclerViewItemRangeSelector` для всех типов контента (фото, видео, аудио, файлы). Долгое нажатие включает режим выбора и позволяет непрерывно выделять или снимать выделение с файлов скольжением пальца с виброоткликом, анимацией чекбоксов и автоскроллом у краев экрана.

- [Fix/Mentions] Устранение вылета при нажатии на пользователя / упоминания:
  - В [`URLSpanUserMention.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/URLSpanUserMention.java) переписан метод `onClick(View widget)`: клики по текстовым ссылкам на пользователей теперь открывают карточку профиля или диалог через `MessagesController` / `ProfileActivity` и изолированы от системного браузера (предотвращён краш `ActivityNotFoundException`).
  - В [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) добавлен перехват кликов по подсказкам массовых упоминаний `@all` и `@admins` (`MentionAction`) и защищен переход по ссылкам на пользователей в переписке.

- [Dialogs/UI] Отключение мини-аватарок авторов в каналах:
  - В [`DialogCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java) метод `addSenderAvatar` теперь пропускает каналы-бродкасты (`ChatObject.isChannelAndNotMegaGroup`) и сообщения от имени самого канала, исключая избыточный дубликат аватарки в строке предпросмотра последнего сообщения списка чатов.

- [Search/Regex] Поиск по регулярным выражениям в переписке (Regex Search):
  - В стандартную строку поиска по переписке [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) добавлен интерактивный переключатель `.*` (Regex Mode) с активной подсветкой.
  - Одиночный тап переключает режим поиска с Regex-компиляцией на лету и уведомлением через Bulletin. Долгий тап открывает шторку шаблонов и заготовок [`HundredGramRegexPresetsBottomSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramRegexPresetsBottomSheet.java).
  - Создан контроллер [`HundredGramRegexSearchController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramRegexSearchController.java) со встроенными шаблонами: Номера телефонов, Банковские карты (Luhn-ready), Криптокошельки (Bitcoin, Ethereum/EVM, TON, USDT TRC20), Ссылки и URL, Email, IPv4-адреса, Даты (ISO/EU/RU).
  - Реализовано создание, сохранение и удаление собственных кастомных Regex-шаблонов с живой валидацией синтаксиса и защитой от ReDoS.
  - В [`MessagesStorage.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesStorage.java) реализован асинхронный поиск `searchMessagesByRegex(dialogId, pattern, maxCount, callback)` по SQLite-базе `messages_v2` с поддержкой полнотекстового поиска и навигации по результатам вверх/вниз в чате.

- [Chat/Mentions] Упоминание всех участников группы (@all / @everyone / @admins):
  - Создан контроллер [`HundredGramMentionController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramMentionController.java), обеспечивающий загрузку и фильтрацию участников групп и супергрупп (исключение ботов, удалённых аккаунтов и текущего пользователя).
  - Интегрированы синтетические подсказки `@all` и `@admins` в верхнюю часть списка автодополнения [`MentionsAdapter.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Adapters/MentionsAdapter.java) и [`MentionsContainerView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/MentionsContainerView.java) при вводе `@`, `@all`, `@everyone`, `@admins`.
  - Реализована шторка настроек упоминания [`HundredGramMentionBottomSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramMentionBottomSheet.java):
    - Выбор целевой аудитории: «Все участники», «Только онлайн / недавние», «Только администраторы».
    - Выбор формата упоминания: «Скрытый (Аккуратный)» (невидимые символы нулевой ширины `\u2060` с текстовыми ссылками `tg://user?id=` без загромождения текста переписки) или «Явный список» (`@username` и текстовые ссылки).
    - Автоматический подсчёт участников и интеллектуальная обработка лимита Telegram (до 50 сущностей на сообщение).


- [Vault/SavePresets] Комплексная переработка системы сохранения: Пресеты, Источники и Типы данных (Saving Presets & Retention Scopes):
  - Решена ключевая проблема неконтролируемого накопления данных: устранён безусловный возврат `true` в `HundredGramConfig.isChatAntiDelete`, из-за которого медиафайлы и сообщения бесконтрольно изолировались в Vault со всех публичных каналов и чатов.
  - Реализована интеллектуальная система пресетов (`savePreset`): «Умный (Рекомендуется)», «Только личные чаты (DMs Only)», «Максимальный архив», «Экономия памяти (Storage Saver)», «Пользовательский».
  - Добавлены гранулярные переключатели областей сохранения (Scopes): Личные сообщения (`saveScopePrivate`), Группы и супергруппы (`saveScopeGroups`), Каналы (`saveScopeChannels`), Сообщения ботов (`saveScopeBots`), Архивированные чаты (`saveScopeArchived`).
  - Добавлены гранулярные переключатели категорий сохраняемого контента (Data Types): Удалённые сообщения (`saveAntiDeleteText`), История правок (`saveEditHistory`), Фотографии (`saveMediaPhotos`), Кружочки и голосовые (`saveMediaVoiceRounds`), Видео (`saveMediaVideos`), Файлы и документы (`saveMediaFiles`).
  - В [`MessagesStorage.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesStorage.java) и [`HundredGramVaultManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramVaultManager.java) интегрированы строгие проверки `isDialogInSaveScope(dialogId)` и типов контента перед сохранением правок, удалённых записей и изоляцией медиа.
  - Экран настроек «Хранилище медиа (Vault)» в [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) полностью переработан в наглядную структурированную панель управления с интерактивным диалогом выбора пресетов, раздельными секциями источников, типов контента, лимитов, квот и инструментов обслуживания хранилища.
  - Добавлена карточка новой функциональности в экран «Что нового» [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java).

- [Spy/ReadTracking] Фиксация точного времени прочтения сообщений (Exact Read Timestamps):
  - Создана таблица `hundredgram_read_dates(mid INTEGER, uid INTEGER, read_date INTEGER, PRIMARY KEY(mid, uid))` с индексами в [`MessagesStorage.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesStorage.java) и оперативным in-memory кэшем `ConcurrentHashMap<String, Integer>`.
  - При обработке подтверждений прочтения от сервера (`TL_updateReadHistoryOutbox` и содержимого медиа) сохраняется точный Unix-таймстемп момента открытия сообщения собеседником.
  - В шторку подробностей сообщения [`HundredGramMessageDetailsBottomSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramMessageDetailsBottomSheet.java) добавлена строка с датой/временем прочтения и относительной дельтой `(+Xs, +Xm)` с момента отправки, а также возможность копирования в буфер обмена.
  - Опция настраивается в меню «Режим невидимки» (`trackReadDate`).

- [Ghost/Stealth] Режим невидимки плюс: Мгновенный офлайн и скрытие загрузки медиа (Ghost Mode Plus):
  - Реализован метод `sendOffline()` в [`MessagesController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java), отправляющий `TL_account.updateStatus(offline = true)` для немедленного снятия 5–15-секундного всплеска статуса «В сети» при фактической отправке сообщений.
  - В [`SendMessagesHelper.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java) (`processSentMessage`) интегрирован автоматический вызов мгновенного офлайна при включенном `sendOfflineAfterOnline`.
  - В [`MessagesController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java) (`sendTyping`) добавлено подавление сетевых пакетов загрузки медиа (`upload_photo`, `upload_video`, `upload_document`, `upload_audio`, `upload_round`) при активном `ghostUploadProgress`.
  - Добавлены переключатели в раздел «Режим невидимки» [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java).

- [Vault/SmartProtection] Защита хранилища Vault от спама ботов (`vaultSaveForBots`):
  - По умолчанию выключено сохранение в Media Vault и историю Anti-Delete временных сообщений и инлайн-клавиатур от ботов (`user.bot`).
  - Добавлены проверки в [`HundredGramVaultManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramVaultManager.java) (`isolateMediaForMessage`, `isolateMediaForEdit`) и [`MessagesStorage.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesStorage.java) (`markMessagesAsIsDeletedInternal`, `saveMessageEditVersionInternal`).
  - Переключатель доступен в разделе «Хранилище медиа (Vault)».

- [Chat/Filtering] Фильтрация каналов и чатов по ключевым словам и Regex (Content Filter):
  - Создан контроллер [`HundredGramContentFilterController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramContentFilterController.java), предоставляющий гибкую систему правил со встроенными пресетами («Крипто-скам и казино», «Спам-ссылки и инвайты») и поддержкой кастомных стоп-слов и регулярных выражений с валидацией синтаксиса и кэшированием компилированных `Pattern`.
  - Реализован экран настройки правил [`HundredGramContentFilterActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramContentFilterActivity.java) с переключателями областей действия (каналы, группы, личные сообщения), добавлением ключевых слов/регулярных выражений и счётчиком скрытых сообщений со сбросом статистики.
  - Интегрировано в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) (с возвратом типа `-1000` для скрытия сообщения через [`HundredGramDummyView`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramDummyView.java)), [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) (`onMeasure` / `onDraw`) и [`NotificationsController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/NotificationsController.java) для полного подавления всплывающих push-уведомлений и звуков для отфильтрованного контента.
  - Настройка доступна в разделах «Конфиденциальность» и «Интерфейс».

- [Chat/Styling] Стилизация меток удаленных сообщений (Deleted Mark Customization):
  - Добавлен выбор значка удаленного сообщения в [`HundredGramConfig`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java): Корзина (`msg_delete`), Крестик (`msg_clear_input`), Перечеркнутый глаз (`msg_archive_hide`).
  - Добавлен выбор цвета значка удаленного сообщения: «В цвет времени (неброский)» (`DELETED_MARK_COLOR_THEME`) или «Красный акцент» (`0xFFE53935`, `DELETED_MARK_COLOR_RED`).
  - Реализована опция «Полупрозрачные удалённые сообщения» (альфа 0.65f vs 1.0f) в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java).
  - В [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) добавлены диалоги выбора значка и цвета метки, а также переключатель полупрозрачности удаленных сообщений.
  - Кастомный значок удаленного сообщения синхронизирован с баннером удаления в шторке истории правок [`HundredGramEditHistoryBottomSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramEditHistoryBottomSheet.java).

- [Chat/EditHistory] Визуальный Diff правок и относительный таймлайн (Visual Diff & Time Delta):
  - В шторку истории правок [`HundredGramEditHistoryBottomSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramEditHistoryBottomSheet.java) интегрирован пословный алгоритм вычисления разницы (LCS-based Word Diff) с защитой от перегрузок памяти на гигантских сообщениях.
  - Визуальная подсветка изменений: добавленный текст выделяется мягким зелёным цветом (`0x334CAF50`), а удалённый текст отображается красным с зачёркиванием (`StrikethroughSpan`, `0x33E53935`), адаптируясь под светлые и тёмные темы.
  - Переключатель режима отображения («Diff» / «Обычный») на каждой карточке редакции с легендой `🟢 добавлено · 🔴 удалено`.
  - Относительный расчёт времени изменения относительно момента отправки исходного сообщения (`+15s`, `+2m`, `+3h`, `+2d`) рядом с абсолютной датой редакции.

- [Tracker/Activity] Перехват активности в HundredGramActivityTracker:
  - Добавлен триггер `TRIGGER_REACTION = 5` и метод `onUserReaction(long userId)` в [`HundredGramActivityTracker.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramActivityTracker.java).
  - Интегрирован автоматический перехват активности при получении входящих сообщений (в ЛС, группах, каналах) и реакций (`TL_updateMessageReactions`) в [`MessagesController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java).
  - В [`HundredGramUserActivityBottomSheet.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramUserActivityBottomSheet.java) поддержано отображение реакций с иконкой `msg_reactions2` и локализацией `ActivityTriggerReaction`.

- [Privacy/Security] Локальный теневой бан пользователей и каналов (Local Shadowban):
  - Создан контроллер [`HundredGramShadowbanController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramShadowbanController.java), управляющий списком заблокированных ID пользователей и каналов с персистентным хранением в SharedPreferences и рассылкой уведомлений интерфейсу.
  - В контекстное меню сообщений [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) добавлен пункт «В теневой бан» / «Снять теневой бан» для любых входящих или пересланных сообщений (по `from_id` и `fwd_from`).
  - Разработана фиктивная ячейка нулевого размера [`HundredGramDummyView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramDummyView.java), полностью скрывающая сообщения тенезабаненных авторов в чатах, группах и комментариях без сдвига индексов сообщений Telegram и без удаления сообщений из базы данных.
  - Разработан полнофункциональный экран управления [`HundredGramShadowbanActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramShadowbanActivity.java) с поддержкой добавления через диалоговый пикер, быстрого удаления свайпом/кнопкой, полной очистки списка и перехода в профиль.
  - Интегрировано в профили пользователей и каналов [`ProfileActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java) в выпадающее меню действий.
  - Добавлен пункт и счётчик в раздел «Конфиденциальность» [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) и динамический бейдж на карточку настроек [`HundredGramSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java).

- [Proxy/Network] Автоматическое отключение прокси при определённых типах сети (`proxyDisableMask`):
  - В [`ApplicationLoader.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java) добавлен `checkProxyForNetworkState()` — метод, который при смене сетевого интерфейса автоматически деактивирует или восстанавливает прокси согласно пользовательской маске условий.
  - В [`ProxyListActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ProxyListActivity.java) добавлена строка «Отключать прокси при» с диалогом множественного выбора (VPN / Мобильный интернет / Wi-Fi).
  - Сохранение маски в [`HundredGramConfig`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java).

- [Media/Storage] Пользовательская папка для сохранения медиафайлов (`customSavePath`):
  - В [`MediaController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MediaController.java) (метод `saveFileInternal`) заменены жёстко заданные пути `"Telegram"` на `HundredGramConfig.customSavePath` для всех типов файлов (изображения, видео, загрузки, аудио).
  - Настройка доступна в разделе «Медиа» — текстовое поле ввода имени подпапки с кнопкой «По умолчанию».

- [Chat/Visual] Оформление цитат и ответов (`replyBackground`, `replyColors`, `replyEmoji`):
  - В [`ReplyMessageLine.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/ReplyMessageLine.java) добавлены хуки:
    - `replyBackground` — скрывает полупрозрачный фон за цитируемым сообщением.
    - `replyColors` — использует нейтральные цвета темы вместо персональных цветов автора.
    - `replyEmoji` — скрывает кастомное эмодзи в заголовке ответа.
  - Три переключателя доступны в разделе «Интерфейс» → «Оформление ответов».

- [Stickers/Visual] Форма углов стикеров (`stickerShape`):
  - В [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) логика `photoImage.setRoundRadius(0)` заменена на `stickerShape`-зависимый радиус:
    - Острые (по умолчанию) — без скругления.
    - Скруглённые — 4dp.
    - Радиус пузыря — `SharedConfig.bubbleRadius`.
  - Настройка доступна в разделе «Интерфейс» → «Стикеры и GIF».

- [Notifications/Push] Отключение искусственной задержки push-уведомлений (`disableNotificationDelay`):
  - В классе [`NotificationsController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/NotificationsController.java) реализован моментальный пропуск 1–3 секундной таймерной задержки Telegram при доставке входящих сообщений.
  - Уведомления появляются на устройстве мгновенно без удержания в планировщике задержек.
  - Настройка доступна в разделе «Интерфейс» (`HundredGramConfig.disableNotificationDelay`).

- [Dialogs/Gestures] Открытие архива свайпом вниз в списке чатов (`archiveOnPull`):
  - В классе [`DialogsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java) поддержано немедленное раскрытие экрана «Архив» при оттягивании списка диалогов вниз свайпом.
  - Устраняет необходимость ждать долгой анимации или целиться в верхнюю закрепленную строку.
  - Настройка доступна в разделе «Интерфейс» (`HundredGramConfig.archiveOnPull`).

- [Channels/ActionBar] Настраиваемая нижняя кнопка в каналах (`channelBottomButton`):
  - В классе [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) добавлена гибкая конфигурация плавающей нижней кнопки в ленте каналов:
    - Без звука / Включить звук (`CHANNEL_BOTTOM_BUTTON_MUTE`).
    - Обсудить / Открыть чат комментариев (`CHANNEL_BOTTOM_BUTTON_DISCUSS`).
    - Скрыть кнопку полностью для максимального обзора постов (`CHANNEL_BOTTOM_BUTTON_HIDE`).
  - Настройка доступна в разделе «Интерфейс» (`HundredGramConfig.channelBottomButton`).

- [Stickers/Recent] Неограниченное количество недавних стикеров (`unlimitedRecentStickers`):
  - В классе [`MediaDataController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MediaDataController.java) снят стандартный лимит Telegram в 20 стикеров: вкладка «Недавние» теперь сохраняет и отображает полную коллекцию использованных стикеров.
  - Настройка доступна в разделе «Интерфейс» и «Медиа» (`HundredGramConfig.unlimitedRecentStickers`).

- [Stickers/Ergonomics] Гибкое позиционирование времени на стикерах (`stickerTimeMode`):
  - Портирован модуль позиционирования [`StickerTime.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/chats/StickerTime.java) и интегрирован в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java):
    - По умолчанию: поверх стикера в правом нижнем углу (`STICKER_TIME_DEFAULT`).
    - Рядом со стикером: вынос времени и статуса прочтения на свободное место за границу стикера (`STICKER_TIME_SIDE`), сохраняя видимость изображения.
    - Скрыто: полное отключение отметки времени на стикерах (`STICKER_TIME_HIDDEN`).
  - Настройка доступна в разделе «Интерфейс» (`HundredGramConfig.stickerTimeMode`).

- [Camera/RoundVideo] Запоминание последней использованной камеры для видеосообщений (`rememberLastUsedCamera`):
  - В классах [`InstantCameraView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/InstantCameraView.java) и [`HundredGramCameraActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramCameraActivity.java) реализовано сохранение выбранной камеры (фронтальной или основной) при переключении во время записи или из меню.
  - При следующем открытии записи кружочка автоматически запускается последняя выбранная камера.
  - Настройка доступна в разделе «Камера» и «Медиа» (`HundredGramConfig.rememberLastUsedCamera`).

- [Media/Player] Автовыбор исходного/максимального качества видео (`preferOriginalQuality`):
  - В классе [`VideoPlayer.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/VideoPlayer.java) реализован автоматический выбор потока с оригинальным качеством (`Quality.original`) при воспроизведении видеороликов с адаптивными потоками нескольких разрешений.
  - Устраняет размытие при старте воспроизведения без необходимости вручную переключать шестеренку качества.
  - Настройка доступна в разделе «Медиа» (`HundredGramConfig.preferOriginalQuality`).

- [Dialogs/Avatars] Мини-аватары отправителей в списке диалогов (`senderMiniAvatars`):
  - В классе [`DialogCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java) реализовано отображение миниатюрных круглых аватарок авторов сообщений прямо в строке предпросмотра последнего сообщения для групповых чатов и каналов.
  - Использован класс аватара `AvatarSpan` с фиксированным отступом `DialogCell.FixedWidthSpan` и динамическим расчетом доступной ширины текста без наложений.
  - Настройка включается в разделе «Интерфейс» (`HundredGramConfig.senderMiniAvatars`).

- [Media/Playback] Приостановка медиа при сворачивании приложения:
  - Добавлены раздельные настройки для автоматической паузы воспроизведения при уходе приложения в фон:
    - Видео в полноэкранном просмотрщике (`pauseOnMinimizeVideo` в [`PhotoViewer.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/PhotoViewer.java)).
    - Голосовые сообщения (`pauseOnMinimizeVoice` в [`MediaController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MediaController.java)).
    - Круглые видеосообщения (`pauseOnMinimizeRound` в [`MediaController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MediaController.java)).
  - Интеграция с системным событием `NotificationCenter.stopAllHeavyOperations` и `PhotoViewer.onPause()` с корректным сохранением позиции и восстановлением.
  - Опции вынесены в категорию «Медиа» настроек 100gram.

- [Chat/Input] Скрытие кнопки «Отправить от имени» с раскрытием по долгому нажатию на эмодзи (`hideSendAsPeer`):
  - В классе [`ChatActivityEnterView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java) реализовано скрытие кнопки выбора профиля/канала для отправки, освобождая горизонтальное пространство в строке ввода.
  - Меню выбора профиля отправки открывается по долгому нажатию на кнопку эмодзи/стикеров (`emojiButton.setOnLongClickListener`), после чего кнопка «Отправить от имени» временно появляется до выбора или закрытия меню.
  - Настройка доступна в разделе «Интерфейс» (`HundredGramConfig.hideSendAsPeer`).

- [Chat/Ergonomics] Скрытие стрелки «Поделиться» у сообщений каналов (`hideShareButton`):
  - В классе [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) поддержано скрытие круглой кнопки пересылки/поделиться рядом с постами в каналах.
  - Устраняет случайные нажатия на стрелку при быстром скролле ленты каналов.
  - Настройка доступна в разделе «Интерфейс» (`HundredGramConfig.hideShareButton`).

- [Chat/Reactions] Скрытие блока реакций в каналах и группах:
  - Реализованы гранулярные переключатели для отключения отображения реакций под сообщениями:
    - `hideReactionsInChannels` — скрытие блока реакций в каналах ([`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java)).
    - `hideReactionsInGroups` — скрытие блока реакций в группах и супергруппах ([`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java)).
  - При отключенных реакциях также подавляется плавающая кнопка быстрого перехода к непрочитанным реакциям в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java).
  - Настройки доступны в разделе «Интерфейс».

- [Chat/Input] Быстрый расчет математических выражений прямо в поле ввода сообщений (`org.telegram.messenger.math.*`):
  - Портирован полный математический парсер и рекурсивно-нисходящий вычислительный движок для чата.
  - Поддерживает стандартные операторы (`+`, `-`, `*`, `/`, `^`, `√`, `%`, факториал `!`), математические функции (`sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `sinh`, `cosh`, `tanh`, `log`, `ln`, `abs`, `sqrt`, `cbrt`, `ceil`, `floor`, `round`, `exp`, `deg`, `rad`, `min`, `max`) и фундаментальные константы (`pi`, `e`, `phi`).
  - Умная фильтрация дат (`looksLikeDate`): не перехватывает даты вида `12.05.2024` и не предлагает нерелевантные подсказки.
  - Полупрозрачный фантомный предпросмотр результата (`GhostTextLayout`), анимированное появление (`AnimatedFloat`) и плавная трансформация вычисленного выражения (`MathRevealAnimation`).
  - Удобная подстановка: результат вычисления вставляется по нажатию пробела или знака `=`, поддерживается мгновенная отмена (undo) по клавише Backspace.
  - Управление через настройку `inlineMath` в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и выключатель в настройках интерфейса ([`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java)).

- [Chat/Security] Инспектор IP-адресов и прокси-соединений (`HundredGramIpInfoController.java`):
  - Реализован контроллер автоматического извлечения IPv4 и IPv6 адресов из ссылок и текста сообщений с фильтрацией локальных/loopback диапазонов.
  - Интерактивное контекстное меню по долгому нажатию на IP-адрес или прокси-ссылку в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java):
    - Быстрое копирование IP в буфер обмена.
    - Геолокация (страна, регион, город).
    - Интернет-провайдер (ISP) и организация.
    - Ассоциированный домен и часовой пояс.
  - Асинхронные безопасные запросы к `ipwho.is` с LRU-кэшированием и всплывающими копируемыми строками по нажатию на любой параметр.

- [Notifications/MultiAccount] Выборочные уведомления для каждого аккаунта (`HundredGramAccountNotifications.java`, `HundredGramAccountNotificationsActivity.java`):
  - Реализован гранулярный контроль push-уведомлений для мульти-аккаунтов Telegram (вместо жесткого переключателя «все или ничего»).
  - В экране настроек уведомлений ([`NotificationsSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/NotificationsSettingsActivity.java)) под переключателем «Все аккаунты» добавлена строка перехода «Аккаунты» с индикацией количества активных профилей (например, «2 из 4»).
  - Создан специализированный экран настройки уведомлений аккаунтов ([`HundredGramAccountNotificationsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramAccountNotificationsActivity.java)) с аватарами, именами пользователей и переключателями.
  - Встроена защита активного профиля: при попытке выключить уведомления текущего активного аккаунта элемент анимированно встряхивается (`shakeViewSpring`), подается тактильный отклик об ошибке и отображается всплывающее уведомление о том, что уведомления активного профиля всегда включены.
  - Контроллер [`HundredGramAccountNotifications.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramAccountNotifications.java) интегрирован в методы диспетчеризации и счетчиков [`NotificationsController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/NotificationsController.java).

- [Text/Safety] Фильтрация нагроможденных диакритических знаков и глитчей Zalgo ([`HundredGramZalgoFilter.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramZalgoFilter.java)):
  - Реализован высокопроизводительный фильтр против вредоносного Zalgo-текста (спам-комбинации юникод-диакритики в диапазонах 0x0300-0x036F, 0x1AB0-0x1AFF, 0x1DC0-0x1DFF, 0x20D0-0x20FF, 0xFE20-0xFE2F и контрольные символы смены направления письма), предотвращающий зависание UI-потока, лаги и сбои отрисовки шрифтов на Android.
  - Фильтр бесшовно интегрирован в движок рендеринга текста и эмодзи [`Emoji.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/Emoji.java) и активируется тумблером в настройках (`HundredGramConfig.filterZalgo`).

- [UI/Material3] Закругленная квадратная кнопка действий (Square FAB в стиле Material 3):
  - Добавлена опция `squareFab` в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и выключатель в категорию «Интерфейс» в [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java).
  - В [`FragmentFloatingButton.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/FragmentFloatingButton.java) кнопка создания нового сообщения и подкнопка историй теперь могут отображаться с закругленными углами в современном стиле Material 3 (16dp для основной кнопки и 12dp для суб-кнопки) вместо стандартного круга.

- [Vault/UX] Восстановление контекстного меню по долгому нажатию и диалог свойств файла («Свойства / Откуда это»):
  - В просмотрщике медиа-сейфа [`HundredGramVaultViewerActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramVaultViewerActivity.java) восстановлено открытие контекстного меню действий при обычном долгом нажатии на файл/медиа (ранее длинный тап перехватывался исключительно режимом мульти-выбора).
  - В контекстное меню добавлен пункт «Свойства файла» (`R.string.HundredGramVaultDetails`) с иконкой `R.drawable.msg_info`.
  - Реализован информационный экран свойств файла (`showFileDetailsDialog`), отображающий:
    - Название файла и его точный размер.
    - Дату и время сохранения в сейф.
    - Исходный чат или пользователя (название/имя и ID с иконкой канала/пользователя).
    - Номер (ID) сообщения в чате (с возможностью скопировать или перейти к сообщению через пункт меню «Перейти к сообщению»).
    - Полный абсолютный путь к файлу в локальном зашифрованном хранилище Vault с быстрым копированием в буфер обмена по нажатию.
  - Режим множественного выделения (Action Mode) теперь активируется как из верхнего меню (иконка галочки), так и выбором пункта «Выбрать» из контекстного меню файла. При уже активном режиме мульти-выбора клик и долгий тап продолжают управлять чекбоксами элементов.

- [Profile/RegDate] Определение и отображение даты регистрации аккаунта и создания чата (`HundredGramRegDateController.java`):
  - Портирован и оптимизирован алгоритм вычисления даты регистрации Telegram по ID пользователя и точкам интерполяции контрольной таблицы (`registration_dates.json`).
  - Оценка кросс-валидируется с датой самого первого аватара пользователя из профиля и истории (`userFull.profile_photo.date` и `dialogPhotos.photos`), что обеспечивает максимальную точность.
  - В профиле пользователя и чатов ([`ProfileActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java)) в строке ID и DC добавлена кнопка с иконкой календаря (`R.drawable.msg_calendar2`).
  - При нажатии на иконку календаря отображается нативный плавающий баннер `Bulletin` с примерной датой создания («Eva создал(а) аккаунт примерно Май 2019» или «Вы создали аккаунт примерно Ноябрь 2017»), а для каналов и супергрупп — дата создания чата.

- [Chats/Ergonomics] Отключение стикера приветствия в пустых диалогах:
  - Добавлена настройка `disableGreetingSticker` («Отключить приветственный стикер») в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и выключатель в категорию «Интерфейс» в [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java).
  - В [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) подавляется показ большого анимированного стикера машущей руки (`ChatGreetingsView`) в новых и пустых чатах, оставляя чистый минималистичный экран с сервисным лейблом отсутствия сообщений.

- [UI/Preview] Полная переработка превью размера стикеров и GIF (`StickerSizePreviewCell.java`):
  - Окно предпросмотра кардинально переработано в реалистичный фрагмент чата Telegram вместо искусственных карточек.
  - Удален чужеродный белый/темный прямоугольник с рамкой и иконкой палитры вокруг стикера. Стикер теперь отображается как настоящий прозрачный контур прямо на актуальных обоях чата (`Theme.getCachedWallpaper()`) с подгрузкой недавних стикеров пользователя через `ImageReceiver` из `MediaDataController` или нативного маскота Telegram.
  - Слева добавлены круглый аватар контакта «Eva» и заголовок имени в официальном цвете темы, а также парящая капсула времени «11:58».
  - Медиа-карточка GIF теперь выровнена по правому краю как настоящее исходящее сообщение: скругление углов 16dp, подгрузка реальных GIF пользователя, центрированная стеклянная кнопка воспроизведения, бейдж «GIF» и капсула времени «12:00» с двойными галочками прочтения «✓✓».
  - Сообщения разнесены вертикально по своим колонкам, давая честное и наглядное представление об их реальных габаритах относительно ширины экрана.
  - В строках управления добавлены динамические бейджи текущего пресета («Обычный • 100%», «Большой • 125%», «Компактный • 75%» и т.д.), синхронизированные с выбором чипов и плавной пружинной анимацией изменения масштаба.

- [Fix/Camera] Устранение багов записи видеосообщений (кружочков) и кнопки отправки:
  - В [`ChatActivityEnterView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java) переменные `videoToSendMessageObject` и `audioToSendPath` теперь сбрасываются в `null` сразу после отправки кружочка. Ранее `checkSendButton` видела неудаленный объект и оставляла кнопку в постоянном состоянии стрелки отправки вместо возврата к микрофону/камере.
  - В `RecordCircle.onDraw()` добавлен вызов `invalidate()` в ветке интерполяции `progressToSendButton < 1f`. Ранее при записи видеосообщений отключение звуковых волн приводило к отсутствию перерисовки, из-за чего иконка замороженного замка визуально не трансформировалась в стрелку отправки.
  - В контейнере `audioVideoButtonContainer.onTouchEvent()` и методе `openCamera()` синхронизированы флаги `calledRecordRunnable` и `sendButtonVisible`, устраняя проблему, при которой нажатие на заблокированную кнопку отправки видеосообщения игнорировалось.
  - В [`InstantCameraView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/InstantCameraView.java) добавлен защитный fallback на базовые параметры (360x360, 30fps) при сбоях инициализации MediaCodec энкодера. При критическом отказе кодека вызывается безопасный сброс `handleEncoderInitError` с уведомлением `NotificationCenter.recordStopped` и закрытием камеры, предотвращая зависание UI.
  - В `CameraGLThread.handleMessage(DO_SHUTDOWN_MESSAGE)` энкодер теперь гарантированно останавливается независимо от флага `recording`, устраняя утечки и зависания камеры при быстрой остановке.
  - В [`ChatActivityEnterViewAnimatedIconView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterViewAnimatedIconView.java) созданные `RLottieDrawable` теперь сохраняются в `stateMap`, предотвращая пересоздание и утечки памяти при анимациях иконок ввода.

- [Vault/Archiver] Улучшение Медиа Сейфа и автозагрузчика для групп и архивов (Media Vault & Archiver Subsystems):
  - Полнофункциональный режим множественного выбора в просмотрщике сейфа [`HundredGramVaultViewerActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramVaultViewerActivity.java):
    - Режим выбора (Action Mode) с анимированным счетчиком выделенных файлов, кнопками «Выбрать все», пакетом сохранения в галерею, пакетным экспортом в ZIP и пакетным удалением.
    - Встроенные чекбоксы и затемняющий оверлей выделения в ячейках медиа (`VaultMediaCell`) и файлов (`VaultFileCell`).
    - Плавный вход в режим выделения по долгому нажатию с виброоткликом, переключение чекбоксов по обычному клику и чистый выход по кнопке «Назад».
  - Потоковый ZIP-экспорт архива [`HundredGramVaultManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramVaultManager.java):
    - Метод `exportVaultToZip` с защитой от коллизий имен и колбэком прогресса сжатия.
    - Экспорт всего содержимого сейфа в ZIP-архив с вызовом нативного диалога «Поделиться» из настроек [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java).
  - Сортировка и быстрая фильтрация:
    - Сортировка по дате (сначала новые / старые), размеру (сначала большие / маленькие) и имени (А-Я).
    - Быстрые фильтры по размеру файлов (> 10 МБ, > 50 МБ, > 100 МБ, Все).
  - Самовосстановление и очистка БД:
    - Инструмент `pruneOrphanDatabaseEntries` удаляет битые записи базы данных, ссылающиеся на отсутствующие или пустые (0 байт) файлы, с автоочисткой при открытии сейфа и ручной кнопкой в настройках.
    - Ротация файлов хранилища [`HundredGramVaultRotator.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramVaultRotator.java) с соблюдением максимального размера (ГБ) и срока хранения (TTL).
  - Оптимизация работы автозагрузчика с группами и архивом:
    - Исправлена логика проверки архивных чатов (`vaultIncludeArchivedChats`) в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java): теперь настройка гарантированно исключает или включает архивированные чаты как для личных диалогов, так и для групп и каналов.
    - Внедрен быстрый кэш папок диалогов (`dialogFolderCache`), исключающий пропуски при получении входящих сообщений.
    - Поддержка супергрупп и форумных тем (топиков) при автозагрузке с разграничением областей (`autoDownloadChatsScope`).
    - Фильтрация мусорного трафика: стикеры и анимированные стикеры автоматически исключаются из автосохранения в сейф, предотвращая забивание хранилища стикерами из активных групп.
    - Механизм противодавления (Backpressure) и квотирования в [`HundredGramBackgroundArchiver.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramBackgroundArchiver.java): лимит 15 медиафайлов на диалог за проход и приостановка при 50+ активных загрузках для предотвращения перегрузки сетевого стека.
    - Исправлена изоляция файлов в [`HundredGramVaultManager.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramVaultManager.java): сохранение в сейф теперь работает независимо от локального статуса Анти-удаления для чата.

- [AI/Assistant] Мульти-сервисная система ИИ, кастомные роли и история диалогов (AI Assistant Engine):
  - Полнофункциональный центр управления искусственным интеллектом [`HundredGramAiSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramAiSettingsActivity.java), перенесенный и адаптированный из exteraGram.
  - Мульти-провайдерная архитектура сервисов [`HundredGramAiServicesActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramAiServicesActivity.java) и [`HundredGramEditServiceActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramEditServiceActivity.java):
    - Поддержка провайдеров: Google Gemini, OpenAI, OpenRouter, Groq, Perplexity, а также произвольных Custom совместимых эндпоинтов.
    - Автоматический парсинг ключей и конфигураций из буфера обмена («Вставить из буфера»).
    - Переключатель поддержки reasoning/thinking для современных моделей (DeepSeek-R1, Gemini 2.0 Flash Thinking, o3-mini).
    - Интерактивная валидация подключения и тестовый запрос прямо из редактора сервиса.
  - Кастомные и системные роли (AI Personas) [`HundredGramRolesActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramRolesActivity.java) и [`RoleCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/RoleCell.java):
    - Выбор и создание собственных ролей с системными промптами.
    - Нативная поддержка анимированных Telegram-эмодзи для ролей без необходимости сетевых запросов к MTProto API ([`AIEditorAlert.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/AIEditorAlert.java)).
    - Быстрое копирование промптов и удаление пользовательских ролей по долгому нажатию.
  - Просмотр и управление историей диалогов [`HundredGramAiHistoryActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramAiHistoryActivity.java) с отображением сообщений пользователя и ассистента и полной очисткой сессий.
  - Тонкая настройка генерации: ползунок температуры (0.0 — 2.0), потоковый вывод (Streaming), скрытие промптов в чате (Show Response Only), цитирование исходных сообщений и интеграция с Telegram AI.
  - Исправлена визуальная анимация тумблеров в настройках ИИ ([`HundredGramAiSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramAiSettingsActivity.java)) и редакторе сервиса ([`HundredGramEditServiceActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramEditServiceActivity.java)): добавлена явная поддержка типов ячеек `NotificationsCheckCell` и `TextCheckCell` при клике.

- [Fix/ActionBar] Исправление центрирования заголовка и шапки чата (Title & Chat Header Centering):
  - В [`ActionBar.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java) добавлены вызовы `requestLayout()` при установке нового заголовка и подзаголовка (`setTitle`, `setSubtitle`, `setTitleAnimated`, `centerTitle`). Ранее `SimpleTextView` при смене текста без пересоздания вызывал только `invalidate()`, из-за чего при включенном центрировании (`centerActionBarTitle`) новый заголовок не измерялся заново и оставался в старой координате вплоть до вертикального скролла списка чатов.
  - В [`FilterTabsView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/FilterTabsView.java) добавлен метод `getTabTitle(int id)` для прямого и точного получения заголовка вкладки без задержки аниматора.
  - В [`DialogsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java) метод `updateHundredGramActionBarTitle` перегружен для поддержки целевого `targetTabId`, вызывается с целевым ID при переключении табов (`onPageSelected`), обновляется при завершении свайпа страниц (`onAnimationEnd`), в `onResume()` при возврате из настроек, а также по глобальному событию `NotificationCenter.updateInterfaces`.
  - В [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) в диалогах выбора режима заголовка, стиля названий и счетчиков папок добавлен постинг `NotificationCenter.updateInterfaces` и `NotificationCenter.dialogFiltersUpdated`, что приводит к моментальному обновлению Action Bar без необходимости перезахода на экран.
  - В [`DialogStoriesCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Stories/DialogStoriesCell.java) выравнивание заголовка и логотипа по центру теперь применяется гарантированно при включенной настройке `centerActionBarTitle`, а не только после прокрутки вниз (`progress != 0`).
  - В [`SimpleTextView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/SimpleTextView.java) восстановлена стандартная реализация `setGravity` без вызова `calcOffset` на еще не измеренной вьюхе с нулевой шириной, из-за чего текст заголовка и статуса сдвигался за пределы области отсечения (`clipRect`) и пропадал при входе в чаты.
  - В [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) контейнер заголовка и аватара собеседника/канала/группы (`avatarContainer`) центрируется по горизонтали (`Gravity.CENTER_HORIZONTAL`) с симметричными отступами по 56dp, гарантируя идеальное положение между кнопкой «Назад» и меню действий.
- [Fix/SwipeActions] Исправление действия «Ответить» при свайпе сообщения:
  - В [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) устранена ошибка, из-за которой действие «Ответить» (`SwipeAction.REPLY`) не появлялось при свайпе сообщений в личных чатах и стандартных группах: для определения доступности ответа использовалось только форумное условие `canSendMessageToTopic(message)`, всегда возвращавшее `false` для не-форумных чатов. Добавлен полноценный расчет `allowReply` с учетом типа чата, прав на отправку и режима.
- [Fix/Navigation] Динамическая синхронизация ViewPager при скрытии вкладок нижней панели:
  - В [`MainTabsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java) и [`ViewPagerActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ViewPagerActivity.java) внедрен динамический двунаправленный маппинг страниц (`indexToPosition` / `positionToIndex`) и динамическое число страниц `getFragmentsCount()`. При отключении вкладки «Контакты» (или «Настройки») страница полностью исключается из `ViewPager`, предотвращая нежелательный переход на скрытый экран при свайпе влево/вправо.
  - По запросу пользователя полностью удалена опция «Вкладка «Звонки» вместо «Настроек»» из [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) и сопутствующая подмена страниц в `MainTabsActivity.java`. Настройки всегда остаются настройками.

- [UI/ChatSettings] Редизайн живого превью стикеров и GIF в виде реального окна чата и перенос в «Настройки чатов»:
  - Окно симуляции чата [`StickerSizePreviewCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/StickerSizePreviewCell.java):
    - Полная переработка визуализации: взамен абстрактной палетки создано полноценное окно чата с поддержкой текущих обоев темы пользователя (`Theme.getCachedWallpaperNonBlocking()`), градиентным фолбэком, плашкой даты («Сегодня»), входящим стикером со скругленной die-cut карточкой, мягкой тенью и капсулой времени («11:58»), а также исходящим GIF-сообщением со значком «GIF», кнопкой воспроизведения и таймстампом с двумя галочками («11:59 ✓✓»).
    - Плавное независимое масштабирование с физикой `OvershootInterpolator` для стикеров и GIF при смене размера.
    - Двойная панель интерактивных чипов выбора масштаба (50%, 75%, 100%, 125%, 150%) для стикеров и GIF прямо под окном чата.
  - Интеграция в настройки чатов:
    - Настройки размера стикеров и GIF перенесены из раздела медиа в раздел «Интерфейс и чаты» (`CATEGORY_INTERFACE`) в [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) рядом с Message Bubbles.
    - В нативные настройки чатов Telegram [`ThemeActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ThemeActivity.java) добавлен пункт «Размер стикеров и GIF» (`stickerSizeRow`) сразу под разделом «Стикеры и эмодзи».
    - Синхронизирована карточка фичи на экране «Что нового» [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java).
- [Fix/Navigation] Исправление скрытия вкладок нижней панели навигации (Bottom Navigation Tabs Visibility):
  - Починено скрытие вкладки «Контакты» и добавлено скрытие вкладки «Настройки» в [`MainTabsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java) и [`AnimatedLinearLayout.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/AnimatedLinearLayout.java): гарантированная установка `View.GONE`/`View.VISIBLE` на целевой таб, мгновенный пересчет ширин и центрирования оставшихся вкладок `tabsView.requestLayout()`.
  - Устранена путаница с вкладкой «Звонки»: по умолчанию третьей вкладкой на экране являются «Настройки». В раздел «Нижняя панель навигации» [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) добавлены точные переключатели: «Скрыть вкладку Контакты», «Скрыть вкладку Настройки» и «Вкладка Звонки вместо Настроек».
  - Подписка `MainTabsActivity` на глобальные уведомления `updateInterfaces` и `callTabsVisibleToggled`, а также гарантированное обновление состояния панели при возврате на экран в `onResume()`.

- [UI/Gestures] Мульти-действия свайпом по сообщениям и векторная иконка пересылок (Swipe Actions & Forwards Count):
  - Многослотовое меню свайпа по сообщению (Swipe Actions): полноценный перенос и интеграция продвинутой системы свайпов из exteraGram:
    - Хелпер жестов и отрисовки [`SwipeActionsHelper.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/SwipeActionsHelper.java) в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java): при свайпе влево и ведении пальца вверх/вниз открывается вертикальная колонка доступных действий (Ответить, В «Избранное», Скопировать, Переслать, Закрепить, Редактировать, Удалить, Без звука) с плавной интерполяцией масштаба (scale), прозрачности (alpha) и тактильным откликом при переключении слотов.
    - Экран детальной настройки [`HundredGramSwipeActionsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSwipeActionsActivity.java) с интерактивным живым превью [`SwipeActionsCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/SwipeActionsCell.java), возможностью включения/выключения действий кликом, перетаскивания (drag-and-drop) для изменения приоритета, опциями «Зацикливание» (Loop Actions) и «Обратная прокрутка» (Reverse Scrolling).
    - Модель действий [`SwipeAction.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/chats/SwipeAction.java) с валидацией прав на сообщение и сохранением порядка действий в `HundredGramConfig`.
  - Векторная иконка счетчика пересылок сообщений (Forwards Count Icon):
    - Внедрен компонент [`ForwardsCountUiHelper.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/ForwardsCountUiHelper.java) взамен текстовой стрелки Unicode.
    - Отрисовка векторной иконки зеркальным отражением по горизонтали стрелки реплая `R.drawable.msg_reply_small` с поддержкой текущей темы оформления, альфа-переходов, плавных анимаций изменения значений в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) и доступности (Accessibility).

- [UI/Settings] Интерактивный редизайн настроек, живые превью и физика переходов (Interactive Settings Redesign & Transitions):
  - Hero Hub и OneUI/iOS карточки: экран [`HundredGramSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java) полностью переработан в современный центр управления:
    - Профиль-герой [`HundredGramSettingsHeroCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramSettingsHeroCell.java) с динамическим аватаром, юзернеймом, версией и 4 быстрыми индикаторами состояния (AI, Vault, Ghost, Plugins).
    - Категории с карточным стилем [`HundredGramCardCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramCardCell.java) со скругленными цветными бейджами иконок, подзаголовками и динамическими статусами (пилюлями).
    - Виджет хранилища Медиа-сейфа [`HundredGramVaultStorageCard.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/HundredGramVaultStorageCard.java) с прогресс-баром занятого места и квоты.
  - Живые интерактивные превью-компоненты:
    - Интерактивный превью свайпов [`SwipeActionsPreviewCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/SwipeActionsPreviewCell.java) с жестом смахивания влево, тактильным откликом при достижении порога, цветным индикатором действия и горизонтальной линейкой чипов быстрых действий.
    - Интерактивный превью баблов [`MessageBubblePreviewCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/MessageBubblePreviewCell.java) с отображением входящего и исходящего сообщений, мгновенно реагирующий на переключение скрытия хвостиков (`removeMessageTail`), компактного карандаша (`replaceEditedWithIcon`), секунд во времени (`showSecondsInTime`) и счетчика пересылок (`showForwardsCount`).
    - Пружинный превью размера стикеров [`StickerSizePreviewCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/StickerSizePreviewCell.java) с физикой `OvershootInterpolator` и пятиуровневым переключателем масштаба (50%-150%).
  - Физика анимаций переходов (Spring Screen Transitions): внедрен [`SpringInterpolator.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/SpringInterpolator.java) (затухающие колебания с damping = 0.78f и реакцией 280ms) в [`ActionBarLayout.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarLayout.java) с возможностью выбора стиля перехода (Default, AOSP, Spring).
  - Стили разделителей списков: настройка `dividerStyle` в [`UniversalAdapter.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/UniversalAdapter.java) (Линия, Островные сегменты, Без разделителей).
  - Все опции интегрированы в [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java), зарегистрированы в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и выведены на экран «Что нового» [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java).

- [Media/Storage] Эргономика медиа и хранилища (Media & Storage Ergonomics):
  - Отображение количества пересылок на постах каналов: опция `showForwardsCount` в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) выводит счетчик репостов сообщения рядом с количеством просмотров (например, `12.4K • 342 ↗`) с динамическим пересчетом ширины таймстампа и плавной сменой значений.
  - Скрытие статуса в шапке чата: опция `hideActionBarStatus` в [`ChatAvatarContainer.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAvatarContainer.java) скрывает статус «в сети», время последнего визита, число участников и индикатор набора текста в верхней панели чата для конфиденциальности при создании скриншотов и стриминге, аккуратно центрируя заголовок по вертикали.
  - Настраиваемый шаг перемотки видео по двойному нажатию: опция `doubleTapSeekDuration` (5, 10, 15, 20 секунд) в [`PhotoViewer.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/PhotoViewer.java) заменяет захардкоженный шаг 10 секунд настраиваемым интервалом с адаптивной индикацией на анимационной дуге `VideoForwardDrawable`.
  - Быстрый переход между темами форумов: опция `quickTopicTransition` в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) уменьшает время анимации переключения и перехода жестом свайпа между темами (топиками) с 150ms до 60ms без искусственных задержек.
  - Настройки интегрированы в разделы «Интерфейс» и «Медиа и качество» [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java), сохранены в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и представлены на экране «Что нового» [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java).

- [Navigation/Stickers] Навигация, стикеры и безопасность жестов (Navigation, Stickers & Gesture Safety):
  - Защита от случайной разархивации свайпом: опция `disableUnarchiveSwipe` в [`DialogsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java) блокирует жест горизонтального смахивания (swipe-to-unarchive) по диалогам внутри папки «Архив», исключая случайное выпадение чатов обратно в основной список. Разархивация остается доступной через контекстное меню и верхнюю панель действий.
  - Скрытие строки поиска над чатами: опция `hideDialogsSearchBar` в [`DialogsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java) скрывает постоянное поле поиска над списком чатов, освобождая вертикальное пространство экрана. Поиск мгновенно открывается по клику на иконку лупы в Action Bar с сохранением всех поисковых фильтров и вкладок.
  - Расширенный пул недавних стикеров: опция `extendedRecentStickers` в [`MediaDataController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MediaDataController.java) увеличивает лимит хранилища недавних стикеров со стандартных 20 до 100+, сохраняя полный локальный набор любимых наклеек в базе данных `web_recent_v3`.
  - Быстрый переход между каналами: опция `quickChannelTransition` в [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) устраняет искусственные задержки анимации отскока (bounce delay) и сокращает длительность перехода к следующему непрочитанному каналу при свайпе вверх с 300ms до 80ms, делая чтение каналов плавным и молниеносным.
  - Настройки интегрированы в разделы «Действия», «Интерфейс» и «Медиа» [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java), сохранены в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и представлены на экране «Что нового» [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java).

- [Multimedia/Player] Мультимедиа, эргономика видеоплеера и стиль облачков (Multimedia, Player Ergonomics & Bubble Tail Styling):
  - Включение звука кнопками громкости: в [`PhotoViewer.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/PhotoViewer.java) нажатие физических кнопок громкости (Volume Up / Volume Down) при просмотре видео с выключенным звуком мгновенно разглушает воспроизведение (`unmuteWithVolumeButtons`), восстанавливая громкость и обновляя пиктограмму в плеере.
  - Свайп в «Картинка в картинке» (Swipe to PiP): жест вертикального свайпа вверх в плеере [`PhotoViewer.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/PhotoViewer.java) плавно переводит воспроизводящееся видео в режим плавающего окна (Picture-in-Picture) с плавной системной анимацией перехода.
  - Облачка сообщений без хвостиков: опция `removeMessageTail` в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) и [`Theme.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java) убирает выступающие нижние хвостики у всех пузырей сообщений, создавая аккуратный минималистичный плоский вид со скругленными углами.
  - Широкие медиапосты в каналах: опция `widePostsInChannels` в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) растягивает фотографии и видеозаписи в публичных каналах на всю доступную ширину экрана без избыточных отступов по бокам.
  - Настройки интегрированы в разделы «Медиа и качество» и «Интерфейс» [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java), сохранены в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и представлены на экране «Что нового» [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java).

- [UI/HeaderTabs] Кастомизация Action Bar и вкладок папок (Header & Tabs Customization):
  - Режимы заголовка Action Bar: добавлена опция `actionBarTitleMode` в [`DialogsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java) с выбором заголовка верхней панели главного экрана:
    - `0`: Название приложения («100gram»)
    - `1`: Юзернейм аккаунта («@username») с автоматическим фолбэком на имя профиля
    - `2`: Имя профиля («First Last»)
    - `3`: Активная папка чатов («Все чаты», «Работа», «Новости») с динамическим обновлением при переключении и скролле вкладок.
  - Центрирование заголовков Action Bar: опция `centerActionBarTitle` выравнивает заголовок, подзаголовок и аватар по центру в [`ActionBar.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java) и [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) с симметричными отступами для кнопок навигации и меню.
  - Стили отображения вкладок папок: настройка `tabTitleStyle` в [`FilterTabsView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/FilterTabsView.java):
    - `0` (Текст и иконки): классический вид с текстом и анимированными/юникод-эмодзи
    - `1` (Только текст): лаконичный текстовый вид с автоматической очисткой эмодзи
    - `2` (Только иконки): ультра-компактный минималистичный режим, извлекающий первую эмодзи или кастомную анимированную иконку папки (с векторным фолбэком `R.drawable.msg_folders`).
  - Режимы счетчиков на вкладках: опция `tabCounterMode` в [`DialogsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java):
    - `0` (Все чаты): стандартный подсчет всех непрочитанных сообщений
    - `1` (Без беззвучных): исключение заглушенных каналов и чатов для фокусировки на важных диалогах
    - `2` (Скрыть счетчики): скрытие бейджей непрочитанного для чистого минималистичного вида.
  - Настройки интегрированы в [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) (раздел «Главный экран»), сохранены в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и представлены на экране «Что нового» [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java).

- [UI/Chats] Пакет улучшений работы с чатами и вводом сообщений (Chats & Input Experience):
  - Повтор сообщений: добавлена опция «Повторить» (`OPTION_REPEAT`) в контекстное меню сообщения [`ChatActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) для мгновенной повторной отправки текста и медиа без цитирования и необходимости копирования/вставки.
  - Просмотр результатов опросов: опция `showResultsBeforeVoting` в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) позволяет видеть проценты и распределение голосов в публичных опросах без обязательной отправки собственного голоса.
  - Быстрая очистка ввода: анимированная кнопка очистки (✕) `clearInputButton` в [`ChatActivityEnterView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java) при наличии набранного текста для мгновенной очистки поля ввода в одно касание.
  - Фильтрация Zalgo-текста: алгоритм `filterZalgo` в [`AndroidUtilities.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java) и [`MessageObject.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessageObject.java) отсекает избыточные накладывающиеся диакритические знаки (больше 2 подряд), предотвращая графические глитчи пузырей чата и сохраняя легитимные языковые символы и спаны форматирования.
  - Иконка карандаша для отредактированных сообщений: опция `replaceEditedWithIcon` в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) заменяет текстовую метку «изменено» на компактную векторную иконку карандаша возле времени сообщения, экономя пространство в строке таймстампа.
  - Все опции выведены в настройки [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java) (разделы «Действия» и «Интерфейс»), зарегистрированы в [`HundredGramConfig.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramConfig.java) и представлены на экране «Что нового» [`HundredGramWhatsNewActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java).

- [Core/Plugins] Полнофункциональная система плагинов и поддержка расширений exteraGram:
  - Разработана расширяемая мульти-движковая архитектура управления плагинами [`PluginsController`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/plugins/PluginsController.java) с потокобезопасным хранением и горячим включением/отключением модулей.
  - Поддержка нативного легковесного движка [`NativeDexPluginsEngine`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/plugins/NativeDexPluginsEngine.java) на базе `DexClassLoader` с загрузкой `.100p` и `.dex` пакетов, изолированным хранением и защитой от Zip Slip атак.
  - Интеграция движка совместимости [`ExteraPythonPluginsEngine`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/plugins/ExteraPythonPluginsEngine.java) с поддержкой скриптов exteraGram (`.plugin`, `.elyx`, `.eaf`), автоматическим парсингом манифестов (`__id__`, `__name__`, `__version__`, `__author__`), трансляцией `BasePlugin` и вызовом хуков.
  - Точки перехвата и расширения меню:
    - Контекстное меню сообщений в [`ChatActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java) с динамической инъекцией пунктов плагинов [`MenuItemRecord`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/plugins/hooks/MenuItemRecord.java) и безопасной проверкой условий.
    - Перехват отправки сообщений в [`SendMessagesHelper`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java) с поддержкой стратегий `MODIFY`, `CANCEL`, `DEFAULT`.
    - Сетевые Pre/Post RPC хуки (`TLObject`, `TLRPC.TL_error`) и события жизненного цикла.
  - Декларативная система настроек плагинов:
    - Модели виджетов [`SettingItem`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/plugins/models/SettingItem.java), `SwitchSetting`, `InputSetting`, `SelectorSetting`, `HeaderSetting`, `TextSetting`, `DividerSetting`.
    - Экран динамического рендеринга настроек плагина [`HundredGramPluginSettingsActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramPluginSettingsActivity.java) с сохранением конфигурации в изолированные `SharedPreferences`.
  - Пользовательский интерфейс управления плагинами:
    - Экран [`HundredGramPluginsActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramPluginsActivity.java) со списком установленных модулей, поиском, переключателями Safe Mode, режима совместимости (ART Opts) и диалогом установки плагинов из файлового хранилища устройства.
    - Интеграция кнопки в главное меню настроек форка [`HundredGramSettingsActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java).
    - Карточка фичи в экране «Что нового» [`HundredGramWhatsNewActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java) и полная двуязычная локализация (русский и английский языки).

- [Services/Translators] Мульти-провайдерный переводчик сообщений (Google, Yandex, DeepL, Telegram):
  - Интегрирован синглтон [`HundredGramTranslator`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/translator/HundredGramTranslator.java) с поддержкой провайдеров: Google Translate Web API, Yandex Translate Mobile API, DeepL и Telegram.
  - Поддержка альтернативного перевода одиночных сообщений в [`TranslateAlert2`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/TranslateAlert2.java) и снятие ограничений на перевод целых чатов без подписки Telegram Premium.
  - Встроенный LRU-кэш на 500 записей для мгновенного отображения повторно переводимых фраз без сетевых задержек.
- [Services/AntiSpam] Автоматический щит от спама в личных сообщениях (Anti-Spam Shield):
  - Создан защитный контроллер [`HundredGramAntiSpam`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/antispam/HundredGramAntiSpam.java), автоматически анализирующий входящие личные сообщения от пользователей, не входящих в список контактов.
  - Автоматическая фильтрация ссылок от незнакомцев и настраиваемый список стоп-слов (крипто-скам, казино, бесплатный заработок, скам-боты).
  - Настраиваемые действия при обнаружении спама: заглушение уведомлений, перемещение в папку «Архив» с выключением звука или полная блокировка пользователя с удалением спам-диалога.
  - Счетчик предотвращенных спам-атак и информирующие всплывающие карточки `Bulletin`.
- [UI/Services] Экран управления сервисами и инструментами [`HundredGramServicesSettingsActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramServicesSettingsActivity.java), интеграция в главное меню [`HundredGramSettingsActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSettingsActivity.java), карточка в [`HundredGramWhatsNewActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java) и полная локализация на русский и английский языки.
- [AI/Assistant] Полнофункциональный AI-ассистент на базе Google Gemini и OpenAI ChatGPT:
  - Поддержка провайдеров Google Gemini (официальный OpenAI-совместимый эндпоинт `generativelanguage.googleapis.com/v1beta/openai/chat/completions`), OpenAI (`api.openai.com/v1/chat/completions`) и кастомных совместимых эндпоинтов (Groq, OpenRouter, DeepSeek, vLLM).
  - Потоковая генерация ответов в реальном времени (Server-Sent Events / SSE-стриминг) через легковесный клиент `HundredGramAiClient` на базе стандартных компонентов Android SDK без тяжелых внешних библиотек.
  - Интеграция в чаты: опция «Спросить AI» в контекстном меню сообщения в [`ChatActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java).
  - Шторка быстрых промптов [`HundredGramAiPromptBottomSheet`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramAiPromptBottomSheet.java): пресеты суммаризации («Кратко»), улучшения текста («Исправить»), перевода («Перевести») и поле свободного вопроса.
  - Шторка отображения ответов со стримингом [`HundredGramAiResponseBottomSheet`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramAiResponseBottomSheet.java) с кнопками копирования в буфер обмена и мгновенной подстановки в поле ввода чата.
  - Замена платного редактора Telegram Premium AI Composer в [`AIEditorAlert`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/AIEditorAlert.java) через [`TelegramAiReplacement`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/ai/TelegramAiReplacement.java), позволяющая использовать AI-перефразирование и генерацию текста без подписки Telegram Premium.
  - Экран управления [`HundredGramAiSettingsActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramAiSettingsActivity.java) с выбором модели, безопасным локальным хранением API-ключа, управлением контекстной памятью диалогов и быстрым получением бесплатного ключа Google Gemini.
  - Карточка фичи в экране «Что нового» [`HundredGramWhatsNewActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java) и полная локализация на русский и английский языки.
- [UI/Customization] Кастомизация интерфейса: форма аватарок, табы навигации и стиль переключателей MD3:
  - Регулировка скругления аватарок со слайдером от 0 (квадрат) до 28 (круг):
    - Добавлены параметры и методы `HundredGramConfig.avatarCorners` и `HundredGramConfig.getAvatarCorners(...)` для унифицированного расчета радиуса в dp и px.
    - Внедрен интерактивный блок превью [`AvatarCornersPreviewCell`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/AvatarCornersPreviewCell.java) со слайдером [`AltSeekbar`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/AltSeekbar.java), скругленной карточкой [`PreviewBackgroundDrawable`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/PreviewBackgroundDrawable.java) и живой симуляцией списка чатов с онлайн-индикатором.
    - Отрегулирована отрисовка аватарок в [`AvatarDrawable`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/AvatarDrawable.java), [`BackupImageView`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/BackupImageView.java) и [`DialogCell`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java).
  - Управление вкладками нижней панели навигации [`MainTabsActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java):
    - Опции скрытия вкладок «Звонки» (`bottomNavHideCallsTab`) и «Контакты» (`bottomNavHideContactsTab`) с мгновенным динамическим перестроением через `NotificationCenter.updateInterfaces`.
  - Стиль переключателей Material Design 3 (`md3SwitchStyle`):
    - В [`Switch.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/Switch.java) добавлен режим MD3 с увеличенным треком, укрупненным ползунком и анимированными векторными индикаторами галочки и крестика.
  - Настройки интегрированы в подраздел «Интерфейс» (`HundredGramSubSettingsActivity`) с полной локализацией на русском и английском языках.
- [UI/PillStack] Расширение стека плашек (WeatherPill & CryptoPill) и экран настроек:
  - Разработан погодный виджет [`WeatherPill`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/PillStack/WeatherPill.java) с отображением температуры, погодных иконок, интеграцией с системной геолокацией и фоновым обновлением раз в 20 минут.
  - Интегрированы криптовалютные виджеты [`GramPill`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/PillStack/GramPill.java) (TON), [`BtcPill`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/PillStack/BtcPill.java) (BTC) и [`UsdPill`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/PillStack/UsdPill.java) (USD) с котировками через Coinbase REST API [`ExchangeRates`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/PillStack/ExchangeRates.java) и фоновым кешированием на 5 минут.
  - Создан менеджер валют [`PillStackCurrencies`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/PillStack/PillStackCurrencies.java) с поддержкой 15 фиатных валют (USD, EUR, RUB, KZT, UAH, PLN, TRY и др.) и быстрым выбором валюты отображения по долгому нажатию.
  - Реализован централизованный реестр [`PillRegistry`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/PillStack/PillRegistry.java) и экран настроек [`HundredGramPillStackSettingsActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramPillStackSettingsActivity.java) для включения, скрытия и сброса плашек.
- [Privacy/Telemetry] Полное подавление фоновой телеметрии (Telemetry Killswitch):
  - Добавлен глобальный рубильник `disableAllTelemetry` в `HundredGramConfig` для гарантированной блокировки отправки диагностических логов и внешних метрик.
  - Настройка вынесена в подраздел «Конфиденциальность» (`HundredGramSubSettingsActivity`).
- [Core/Backup] Резервное копирование и восстановление настроек (JSON Backup):
  - Создан менеджер [`HundredGramBackupManager`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramBackupManager.java) для полного экспорта конфигурации форка в структурированный JSON-файл с метаданными схемы.
  - Поддержка мгновенной отправки файла бэкапа в «Избранное» (Saved Messages) или любое приложение через системный Android Share Sheet.
  - Импорт и валидация настроек из любого сохраненного JSON-бэкапа с динамическим уведомлением UI (`didUpdateConfig`).
  - Разработан экран управления [`HundredGramBackupActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramBackupActivity.java) с функцией безопасного сброса всех настроек форка к заводским значениям по умолчанию.
  - Полная двуязычная локализация и интеграция в секцию «Данные» главного меню `HundredGramSettingsActivity`.
- [UI/IconPacks] Система кастомных наборов иконок (Icon Packs):
  - Полноценная архитектура перехвата ресурсов `HundredGramResources extends Resources` в `LaunchActivity` с динамической заменой векторных и растровых иконок интерфейса без необходимости перезапуска приложения.
  - Менеджер наборов иконок `HundredGramIconManager` с многоуровневым `LruCache` для высокой производительности и отсутствия фризов UI.
  - Защищенное изолированное хранилище `HundredGramIconPackStorage` с поддержкой установки архивов `.icons` и `.zip` из хранилища устройства и защитой от Zip Slip.
  - Специализированный экран настроек [`HundredGramIconPacksActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramIconPacksActivity.java) с радио-выбором активного пака, превью количества иконок и автора, установкой и удалением пользовательских пакетов.
  - Двуязычная локализация строковых ресурсов (русский и английский) и интеграция в главное меню `HundredGramSettingsActivity`.
- [UI/PillStack] Интерактивные информационные плашки `PillStack` в поисковой строке (`FragmentSearchField`):
  - Интегрированы компактные виджеты: `ProxyPill` (статус прокси-соединения, отображение реального пинга в мс) и `CachePill` (текущий размер кэша приложения, круговой индикатор занятого пространства `StorageProgressDrawable`, быстрый переход в `CacheControlActivity`).
  - Поддержка вертикального переключения между плашками жестом свайпа с физической анимацией масштабирования и затухания.
  - Интеграция переключателя виджетов `pillStackEnabled` в раздел настроек «Главный экран» (`HundredGramSubSettingsActivity`).
- [UI/Gestures] Кастомизация действия при свайпе сообщения влево (`chatMessageSwipeAction`):
  - Возможность выбора действия при свайпе сообщения влево в чате: Ответить (`REPLY`), Сохранить в Избранное (`SAVE`), Копировать текст (`COPY`), Переслать (`FORWARD`), Редактировать (`EDIT`), Удалить (`DELETE`).
  - Динамическая подстановка векторных иконок действия с автоматическим наложением цвета темы оформления сервисных иконок (`key_chat_serviceIcon`) в `ChatActivity`.
  - Интеграция пункта настройки в категорию «Действия и жесты» (`HundredGramSubSettingsActivity`).
- [Camera/RoundVideo] Эргономика выбора камеры и зума при записи кружочков:
  - Выбор объектива (передняя / задняя камера) по долгому нажатию на кнопку записи кружочка с возможностью выбора без отрыва пальца от экрана.
  - Интерактивный слайдер плавного масштабирования (зума) во время записи круглых видеосообщений.
  - Предотвращение крашей `MediaCodec` / `MediaMuxer` при ультракороткой записи (<0.5 сек) за счет мягкой отмены сессии (`cancel()`).
- [Audio/STT] Потоковое декодирование голосовых сообщений в `HundredGramSttManager`:
  - Замена буферизации всего аудиофайла в RAM на потоковую чанковую обработку через нарезку аудиопотока и прямое скармливание в `VoskRecognizer`, предотвращающая ошибки `OutOfMemoryError` на длинных аудиозаписях.
- [UI/Batch] Мультивыбор чатов и контактов при добавлении в мониторинг активности ([`HundredGramTrackedUsersActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramTrackedUsersActivity.java)) и в список исключений автозагрузки сейфа ([`HundredGramVaultExceptionsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramVaultExceptionsActivity.java)): массовое добавление чатов за один раз через полноэкранный селектор с галочками и плавающей кнопкой подтверждения.
- [Profile/Auditor] Расширенный аудит профилей (Profile Auditor & Change Log):
  - Создан сервис отслеживания изменений профилей [`HundredGramProfileLogger`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramProfileLogger.java) с сохранением в таблицу базы данных SQLite `profile_changes_log`.
  - Автоматическое логирование при обновлениях через `MessagesController.putUser` и `loadFullUser`: смена имени и фамилии (`first_name`, `last_name`), юзернейма (`@username`), номера телефона (появление, скрытие или изменение), описания профиля (`about` / Bio), дня рождения (`birthday`) и счетчика подарков (`stargifts_count`).
  - Разработан специализированный экран истории профиля [`HundredGramProfileLogActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramProfileLogActivity.java) с отображением старого и нового значений, форматированной даты и времени, копированием значений по клику и возможностью очистки истории.
  - Добавлен пункт меню «История профиля» в меню три точки на экране профиля пользователя (`ProfileActivity`).
  - В подраздел «Мониторинг активности» добавлен переключатель «Аудит изменений профиля» (`HundredGramConfig.profileHistoryEnabled`) для включения/выключения фонового отслеживания.
- [Audio/STT] Локальный офлайн-перевод голосовых сообщений в текст (Speech-to-Text без Telegram Premium):
  - Интегрирована нейросетевая модель Vosk (`vosk-android:0.3.47`) для полностью локального распознавания речи на устройстве пользователя с нулевым внешним трафиком.
  - Динамическая загрузка компактной языковой модели (~43 МБ) по требованию без утяжеления установочного APK. Модель загружается в фоновом режиме с индикатором прогресса и может быть удалена в любой момент из настроек для освобождения памяти.
  - Нативное декодирование аудио `OGG Opus` Telegram в 16 кГц 16-бит моно PCM через аппаратно-ускоренные `MediaExtractor` и `MediaCodec`.
  - Бесшовная интеграция в нативный интерфейс Telegram: нажатие на кнопку «А» (распознавание речи) на голосовом сообщении у пользователей без подписки Premium больше не вызывает экран покупки подписки, а запускает локальную транскрипцию с отображением текста в стандартном облачке расшифровки Telegram и сохранением в базу данных `MessagesStorage`.
  - Управление функцией в настройках («Действия и жесты» -> «Локальная расшифровка речи»).
- [UI/Settings] Экран обновлений «Что нового» и секция «О приложении»:
  - Создан специализированный экран [`HundredGramWhatsNewActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramWhatsNewActivity.java) со списком изменений по версиям клиента (v1.2, v1.1, v1.0), быстрым переходом к настройкам и прямыми ссылками на релизы GitHub.
  - В главное меню «Настройки 100gram» (`HundredGramSettingsActivity`) добавлен блок «О приложении» с пунктами «Что нового», «Автор: @w1ckeddamn» (открытие Telegram `@w1ckeddamn`) и «Исходный код: github.com/W1ckeddamn/100gram».
  - Полная двуязычная локализация (русский и английский) всех заголовков и описаний.
- [UI/Media] Настройки размера отображения стикеров и GIF-анимаций в чатах:
  - Добавлены настройки масштабирования размера стикеров и GIF (`HundredGramConfig.stickerSize`, `HundredGramConfig.gifSize`) с сохранением в конфигурации.
  - Поддержка 5 градаций размера: Минимальный (50%), Компактный (75%), Стандартный (100%), Увеличенный (125%), Огромный (150%).
  - Интеграция в раздел настроек «Медиа» (`HundredGramSubSettingsActivity`) в блоке «Стикеры и GIF» с диалоговыми окнами выбора.
  - Динамическое масштабирование в `ChatMessageCell`: пропорциональный пересчёт размеров обычных, анимированных и видеостикеров, а также GIF-сообщений и прикреплённых документов без нарушения пропорций и разметки сообщений.
  - Полная двуязычная локализация (русский и английский) строковых ресурсов.
- [Camera/UI] Отправка обычных видео из галереи в виде круглых видеосообщений (кружочков):
  - Добавлена кнопка переключения в режим видеосообщения (`roundVideoButton`) в нижнюю панель просмотрщика `PhotoViewer` рядом с кнопкой отключения звука с эффектом матового стекла и адаптивной анимацией.
  - Добавлена опция «Отправить как видеосообщение» в выпадающее меню по долгому нажатию на кнопку отправки (`pickerViewSendButton`).
  - Автоматическое кадрирование видео в пропорции 1:1, ограничение максимальной длительности таймлайна до 60 секунд.
  - Нативное кодирование через `MediaCodecVideoConvertor` и отправка с атрибутом `TLRPC.TL_documentAttributeVideo.round_message = true`, разрешением до 480x480 (в соответствии с настройкой качества видеосообщений форка) и круглой размытой миниатюрой.
  - Автоматическое разделение группировки (не объединяются в альбомы) и соблюдение прав чата (`ChatObject.canSendRoundVideo`).
- [Vault/Archiver] Режимы работы архиватора (виды работы архиватора):
  - Добавлен выбор режима в настройках «Медиасейф и архиватор» (`HundredGramSubSettingsActivity`): **«Только новые (в реальном времени)»** (`ARCHIVER_MODE_NEW_ONLY`, по умолчанию) и **«Полная синхронизация (с историей)»** (`ARCHIVER_MODE_FULL_SYNC`).
  - В режиме «Только новые» архиватор перехватывает поступающие сообщения в реальном времени через `NotificationCenter.didReceiveNewMessages` и сохраняет только новые медиа и сообщения, пришедшие строго после момента активации архиватора (`msg.date >= backgroundArchiverActivationTime`), полностью отключая фоновое сканирование диалогов и запросы истории `TL_messages_getHistory` по старым чатам.
  - В режиме «Полная синхронизация» сохраняется периодический фоновый обход всех диалогов с подгрузкой недавней истории сообщений.
  - Локализация настроек режимов на русском и английском языках (`HundredGramArchiverMode`, `HundredGramArchiverModeNewOnly`, `HundredGramArchiverModeFullSync`).
- [Core/MTProto] Синхронизация схемы MTProto со слоем **Layer 229** (Telegram 12.10.6):
  - Новые конструкторы драфтов сообщений `TL_sendMessageTextDraftAction` (`0x3630b85a`) и `TL_sendMessageRichMessageDraftAction` (`0x52564893`) с обратной совместимостью через слой Layer 228.
  - Поддержка уникальных подарков Layer 229 `TL_messageActionStarGiftUnique` (`0x7E1C1187`) и слоя 228 (`0xe6c31522`).
  - Поддержка прав администратора на приветственные сообщения (`manage_welcome_messages`) и флагов `has_welcome_messages` в `chatFull` и `channelFull`.
  - Интеграция модуля `TL_keyboard.java` для полной поддержки современных инлайн-кнопок.
- [UI/RichMessage] Реализована полноценная поддержка интерактивных кнопок внутри текста форматированных сообщений и Instant View (`pageBlockButtonRow` и `textButton` в [`RichMessageLayout.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/RichMessageLayout.java)):
  - Реализованы компоненты отрисовки `RichButtonRowBlock`, `RichButton` и `RichButtonSpan` с поддержкой стилизации (`DEFAULT`, `PRIMARY`, `DANGER`, `SUCCESS`, `DEFAULT_IN_TEXT`), гибкого выравнивания (`LEFT`, `RIGHT`, `CENTER`, `FILL`), пропорционального сжатия при переполнении и пружинящих анимаций нажатия.
  - Добавлены векторные ресурсы иконок кнопок (`mini_inline_arrow_16`, `mini_inline_copy_16`, `mini_inline_switch_16`, `mini_inline_profile_16`, `bot_webview`, `bot_card`).
  - Реализована безопасная конвертация и диспетчеризация кликов `convertPageButton` в `ChatMessageCell.delegate.didPressBotButton`, обеспечивающая нативную обработку ссылок, открытие Telegram Mini Apps (Web Apps), копирование данных в буфер и callback-запросы ботам без изменения внешних сигнатур.
  - Добавлена палитра `Theme.IvButtonColors` и соответствующие ключи темы в [`Theme.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java), а также поддержка динамической альфы в [`ForegroundColorSpanThemable.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/ForegroundColorSpanThemable.java).
- [UI/RichMessage] Полноценная поддержка неподдерживаемых блоков `RichUnsupportedBlock` в `RichMessageLayout`:
  - Устранена проблема заглушек «unsupported block»: неподдерживаемые элементы в Instant View и форматированных сообщениях теперь отрисовываются через `UnsupportedBlockDrawable` с интерактивной кнопкой обновления.
  - Добавлены русские локализации строк `UnsupportedBlockTitle`, `UnsupportedBlockMessage` и `UnsupportedUpdate`.
- [Vault/Archiver] Реализована принудительная фоновая предзагрузка медиаданных через `FileLoader.loadFile` со сниженным приоритетом `FileLoader.PRIORITY_LOW` (фотографии, документы, видео, аудио, видеосообщения) в `HundredGramBackgroundArchiver`.
- [Vault] Добавлен перехват системных уведомлений `NotificationCenter.fileLoaded`: изоляция и hardlink в `HundredGramVaultManager.isolateMediaForMessage` происходят строго после завершения записи файла на диск.
- [Service] Добавлен запуск службы `HundredGramBackgroundArchiver` в `NotificationsService.onCreate()` для всех активных учетных записей.
- [Tracker] Добавлен переключатель «Отслеживать активность» (`TextCheckCell`) в профиле любого пользователя (`ProfileActivity`) для быстрого включения мониторинга онлайна.
- [Tracker] Создан экран управления отслеживаемыми контактами [`HundredGramTrackedUsersActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramTrackedUsersActivity.java): список пользователей с аватарками и статусами, выбор любых личных чатов (включая не-контакты) через `DialogsActivity`, удаление и переход к журналу активности.
- [Tracker] Добавлена поддержка опроса статусов не-контактов и отслеживаемых пользователей через MTProto `users.getUsers` (`TLRPC.TL_users_getUsers`) в `HundredGramActivityTracker`.
- [UI/Search] Поиск каналов, супергрупп и пользователей по числовому ID в поисковой строке списка диалогов ([`MessagesStorage.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/MessagesStorage.java)):
  - Поддержка форматов `-1001234567890`, `1001234567890`, `1234567890`, `id:...`, `#...`.
  - Мгновенный поиск по базе данных SQLite (`chats`, `users`) и оперативной памяти `MessagesController`.
  - Найденный канал/чат выводится на самом верху результатов поиска с указанием `ID: -100...`, а по нажатию сразу открывается диалог.
- [Docs] Набор руководств и навыков для ИИ-агентов в каталоге `.agents/skills/` (`100gram-architect`, `100gram-build`, `100gram-tracker-vault`, `100gram-ui-components`, `100gram-changelog`).
### Fixed
- [Camera/RoundVideo] Исправлена ошибка отправки круглых видеосообщений (кружочков) с камеры ([`InstantCameraView.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Components/InstantCameraView.java)):
  - Восстановлен флаг `fromCamera = true` для объектов `VideoEditedInfo` при записи видеосообщений с камеры.
  - Ранее из-за отсутствия флага кружочки с камеры ошибочно расценивались как требующие конвертации (`needConvert() == true`), что приводило к отмене потоковой отправки файла (`FileLoader.cancelFileUpload`), сбросу ключей шифрования и зависанию отправки в чатах.
  - Сохранено корректное преобразование и нарезка обычных видео из галереи при отправке в виде кружочков (`fromCamera = false`).
- [Core/MTProto] Синхронизированы недостающие конструкторы блоков страниц и RichMessage со схемой Layer 229 из upstream ([`TL_iv.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/tgnet/tl/TL_iv.java)):
  - Добавлен конструктор строки кнопок `pageBlockButtonRow` (`0x6D640318`) с флагами выравнивания (`align_left`, `align_center`, `align_right`) и десериализацией `TL_keyboard.PageButton`. Устранен вылет `TLParseException: can't parse magic 6d640318 in org.telegram.tgnet.tl.TL_iv$PageBlock`.
  - Добавлены конструкторы `textButton` (`0xAFC79CD6`) и `pageBlockDocument` (`0x38FA3BA3`), а также поддержка флага `compact` в `pageBlockTable`.
- [Core/MTProto] Добавлена регистрация апдейта `TL_updateEphemeralBotCallbackQuery` (`0x7C1079D6`) в [`TL_update.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/tgnet/tl/TL_update.java) и [`TLRPC.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/tgnet/TLRPC.java).
- [Vault/Archiver] Исправлена ошибка SQLite `no such column: top_message` в [`HundredGramBackgroundArchiver.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/HundredGramBackgroundArchiver.java): имя столбца последнего сообщения в таблице `dialogs` изменено с `top_message` на `last_mid`.
- [Core/MTProto] Добавлена поддержка конструктора цитат `pageBlockBlockquote` Layer 229 (`0x66d1670b`) в [`TL_iv.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/tgnet/tl/TL_iv.java) с флагами и свойством `collapsed`, а также сохранена обратная совместимость со старым форматом через `pageBlockBlockquote_layer82` (`0x263d7c26`). Устранен фатальный вылет `TLParseException: can't parse magic 66d1670b in org.telegram.tgnet.tl.TL_iv$PageBlock` при рендеринге постов каналов и страниц Instant View с цитатами.
- [Core/MTProto] Безопасная обработка ответов отмены сетевых запросов `rpc_answer_dropped_running` (`0xcd78e586`) и `rpc_answer_dropped` (`0xd18be2ef`) в [`TLRPC.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/tgnet/TLRPC.java) и [`TLParseException.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/tgnet/TLParseException.java). Предотвращен фатальный сбой `TLParseException: can't parse magic cd78e586 in org.telegram.tgnet.TLRPC$upload_File` при отмене загрузок файлов при быстрой прокрутке чатов и каналов.
- [Core/MTProto] Устранены критические сбои `TLParseException` при десериализации клавиатур Layer 229 ([`TLRPC.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/tgnet/TLRPC.java)):
  - Добавлена поддержка конструктора строк инлайн-клавиатур `TL_keyboardInlineButtonRow` (`0x19420AF6`).
  - Добавлена десериализация инлайн-кнопок Layer 229 (`0x11C1A322` и `0x9C1C0C55`) с преобразованием типов кнопок в соответствующие классы `TLRPC` (`TL_keyboardButtonUrl`, `TL_keyboardButtonCallback`, `TL_keyboardButtonWebView`, `TL_keyboardButtonSwitchInline`, `TL_keyboardButtonCopy` и др.).
  - Добавлена десериализация кнопок обычных reply-клавиатур ботов Layer 229 `TL_keyboardButton` (`0x2F67A72F`) с поддержкой всех подтипов `ButtonType`.
  - Устранена первопричина вылетов при открытии каналов и бесконечного зависания диалогов на статусе «Обновление...».
- [Vault/Archiver] Устранено бесконечное зависание интерфейса на статусе «Обновление...» при включенном архиваторе:
  - Устранена рекурсивная петля уведомлений `dialogsNeedReload` и убран принудительный вызов `loadDialogs(1)` при пустом архиве.
  - Убрана паразитная запись сообщений в базу истории Telegram (`MessagesStorage.putMessages`), провоцировавшая постоянную перерисовку списка диалогов и сброс счётчиков.
  - Реализована пауза архиватора при активной синхронизации Telegram (`ConnectionStateUpdating` / `getDifference`).
  - Исправлена фильтрация области видимости `canVaultDownload`: каналы и группы теперь корректно отсекаются согласно настройке `autoDownloadChatsScope`, а неизмененные каналы пропускаются по `top_message` без сетевых запросов.
- [Core/Chat] Исправлена ошибка подписки на каналы: восстановлена валидация членства в чатах в [`ChatObject.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/messenger/ChatObject.java) (`isNotInChat`, `isLeftFromChat`, `isKickedFromChat`, `isInChat`). Ранее из-за жесткой заглушки `return false` приложение считало пользователя участником всех каналов, из-за чего вместо кнопки «Подписаться» отображалось «Вкл./Выкл. звук», а нажатие переключало уведомления вместо вступления в канал.
- [Core/MessagesController] Разрешено удаление диалогов, инициированное пользователем через UI (`deleteDialog` с флагом `isSelf = true`).
- [Core/CrashFixes] Перенесены критические исправления стабильности из Telegram 12.10.6:
  - `MessageObject.java`: валидация границ `run.start` и `run.end` в `TextStyleRun` для предотвращения `StringIndexOutOfBoundsException`.
  - `Utilities.java`: проверка одиночного минуса в `parseInt` во избежание сбоя парсинга некорректных чисел.
- [Vault] Устранена ошибка «файл не найден» при архивации: устранена попытка создания ссылки до скачивания бинарного содержимого.
- [Vault] Исправлен фильтр `canVaultDownload`: гарантировано включение всех личных чатов (`dialogId > 0`) и архивированных диалогов (`folder_id == 1`).
- [Tracker] Исправлен подсчёт времени сессий и суммарного времени за день: внедрён таймаут автозакрытия висячих сессий (90 секунд), устранены сессии с `end_time = 0`, стандартизированы секунды Unix-времени и форматирование в UI (`X мин` / `X ч Y мин`).
- [Settings/Ghost] Подключен режим невидимки для историй в [`StoriesController.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Stories/StoriesController.java): при активном `HundredGramConfig.isGhostStoryViewActive()` блокируется отправка `TL_stories_readStories`.
- [Settings/Media] Подключена глобальная отправка видео как документа [`PhotoViewer.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/PhotoViewer.java) (`HundredGramConfig.sendVideoAsDocument`).
- [Settings/Streamer] Подключена маскировка текста превью диалогов в режиме стримера в [`DialogCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java) (`HundredGramConfig.isStreamerHideDialogMessages()`).
- [Settings/Chat] Подключено выделение удалённых сообщений через значок корзины в [`ChatMessageCell.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) (`HundredGramConfig.highlightDeletedMessages`).
- [Tracker/UI] Восстановлен доступ к управлению отслеживаемыми контактами ([`HundredGramTrackedUsersActivity`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramTrackedUsersActivity.java)): пункт вынесен из условного блока `notifyOnUserOnline` в поднастройках трекера и возвращён в главное меню «Настройки 100gram» с прямым счётчиком добавленных пользователей.
- [Vault/Gallery] Исправлена ошибка сохранения фото и видео в галерею при изоляции в Media Vault: медиафайлы теперь гарантированно экспортируются в публичное системное хранилище и `Pictures/Telegram` через `MediaController.saveFile`, устраняя сбои при сохранении из кэша приватного хранилища.
- [Audio/STT] Исправлена точность офлайн-распознавания Vosk STT: внедрена потоковая разбивка фраз, интерполяция 3:1 для Opus 48 кГц -> PCM 16 кГц, автоматическая регулировка уровня (AGC) и обход проверки Premium для круглых видеосообщений.
- [Core/Cleanup] Устранена потенциальная утечка файловых дескрипторов в `HundredGramAvatarHistory.copyFile` (добавлен `try-with-resources`), поток-безопасный `volatile isDownloading` в `HundredGramSttManager`, и удалены неиспользуемые импорты из заглушки `HundredGramDebugLog`.
- [Camera] Исправлена автонастройка камеры [`HundredGramCameraActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramCameraActivity.java): `autoTuneSettings` теперь детерминированно выставляет оптимальный битрейт 2000 кбит/с вместо инвертирования текущего состояния.

### Changed
- [UI/Settings] Переключение режима отображения ID и DC в профиле ([`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java)): циклический переключатель по тапу заменён на диалоговый список вариантов выбора («Отключено», «Только ID», «ID и датацентр»).
- [Vault/UI] Устранено дублирование кнопок в меню «Медиасейф и архиватор» ([`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java)): убран лишний пункт просмотра файлов из верхней части экрана и объединены дублирующиеся кнопки очистки («Очистить сейф» и «Очистить весь офлайн архив») в одну комплексную операцию с одновременным удалением физических файлов сейфа и записей базы данных.
- [Vault/Archiver] Фоновый архиватор (`backgroundArchiverEnabled`), автозагрузка в сейф (`autoDownloadMediaVault`), архивация комментариев и включение архивированных чатов переведены в отключенное по умолчанию состояние (`false`). Функции активируются пользователем вручную в меню «Настройки 100gram → Медиасейф и архиватор».
- [Localization] Полная зачистка захардкоженных строк: свыше 40 строк интерфейса в [`HundredGramSubSettingsActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramSubSettingsActivity.java), [`HundredGramCameraActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramCameraActivity.java) и [`HundredGramNetworkBoostActivity.java`](file:///D:/100gram/TMessagesProj/src/main/java/org/telegram/ui/HundredGramNetworkBoostActivity.java) переведены на официальную схему локализации через `hundredgram_strings.xml` (русский и английский языки).
- [Build] Базовая версия форка синхронизирована с **Telegram 12.10.6** (код сборки `7112`, `APP_VERSION_CODE = 7112010`).
- [Git] Нормализация репозитория: переключение рабочей ветки на `main`, удаление устаревших веток (`backup-12.92-full`, `graher_12.92.1`, `upgrade-12.10.5`).
- [Git] Строгая настройка `.gitignore` для NDK/C++ артефактов (`.cxx`, `*.so`), Android Studio, секретов и APK.
- [Docs] Полный редизайн `README.md` с актуальной архитектурой и инструкцией по сборке.

### Removed
- [Settings/Cleanup] Удалены дублирующиеся, устаревшие и нереализованные переключатели:
  - Удалён дублирующий пункт «Разблокировать сохранение историй» из категории «Медиа и качество» (оставлен только в «Невидимке»).
  - Удалён устаревший чекбокс «Ускорение загрузки» из настроек медиа (сетевое ускорение полностью настраивается в выделенном разделе «Ускоритель сети»).
  - Удалена мёртвая категория `CATEGORY_CAMERAX` из `HundredGramSubSettingsActivity` (экран камеры вызывается напрямую через `HundredGramCameraActivity`).
  - Удален избыточный блок «Размытие и приватность» (`privacyBlurDialogMessages`, `privacyBlurAvatars`) из настроек приватности (реального блюра не производилось, тумблеры дублировали скрытие из «Режима стримера»).
  - Удалены устаревшие неиспользуемые ID и константы (`ITEM_ROUND_HIGH_BITRATE`, `ITEM_NETWORK_BOOST_PANEL`, `ITEM_AUTOMATION_RULES`, `ITEM_CONFIRM_LEAVE_CHAT`, `ITEM_BOOST_DOWNLOAD_SPEED`, `ITEM_PRIVACY_BLUR_MESSAGES`, `ITEM_PRIVACY_BLUR_AVATARS`).

---

## [12.10.5-v1.0.0] - 2026-10-03

### Added
- [Vault] **HundredGram Media Vault**: Защищённое изолированное медиахранилище (`HundredGramVaultManager`, `HundredGramVaultViewerActivity`).
  - Автоматическое скрытие и организация фото, видео и документов.
  - Индексация медиафайлов в выделенной базе SQLite.
  - Поддержка жестких ссылок (hardlinks) файловой системы для исключения дублирования дискового пространства.
  - Фоновый ротатор хранилища (`HundredGramVaultRotator`).
- [Tracker] **HundredGram Activity Tracker**: Фоновый трекер присутствия и активности пользователей (`HundredGramActivityTracker`).
  - Фиксация переходов в онлайн/оффлайн через апдейты MTProto.
  - Детальный журнал заходов в BottomSheet (`HundredGramUserActivityBottomSheet`).
  - Автоматическое закрытие зависших сессий (таймаут 90 секунд) для предотвращения бесконечного онлайна.
- [Camera] **CameraX & Square Video Notes**: Модуль улучшенной видеозаписи (`RoundVideoSession`, `RoundVideoGlProcessor`).
  - Запись видеосообщений («кружков») в повышенном битрейте и 60 FPS.
  - Поддержка квадратных видеосообщений.
  - Быстрое переключение между фронтальной и основной камерой на лету во время записи (`RoundVideoCameraController`).
- [Network] **HundredGram Network Boost**: Модуль оптимизации соединения (`HundredGramNetworkBoostActivity`).
  - Увеличенные сокетные буферы приема и передачи.
  - Параллельные потоки загрузки тяжелых медиа.
- [UI] **История изменений и аватарок**:
  - `HundredGramEditHistoryBottomSheet` — просмотр истории редакций сообщений.
  - `HundredGramAvatarHistoryActivity` — сохранение и просмотр всех ранее установленных аватарок пользователя.
- [UI] **Правила автоматизации**:
  - `HundredGramAutomationActivity` и `HundredGramAutomationEngine` — гибкая настройка автоматических действий по событиям.
- [UI] Современные WebP иконки приложения высокой четкости (от mdpi до xxxhdpi).

### Fixed
- [Billing] Восстановлен нативный `BillingController.java`:
  - Полная поддержка форматирования цен для Telegram Stars (XTR), TON и фиатных валют без внешних сервисов Google Play.
- [Billing] **Полная зачистка текстовых инъекций и пасхалок**:
  - Устранены несанкционированные подмены валют ("Slava Ukraini", "Free Palestine") в 21 UI-классе подарков, розыгрышей, каналов и подписок.
- [Core] Восстановлены официальные телефонные префиксы для Израиля (+972) и России (+7) в `countries.txt`.
- [Build] Исправлены конфликты CMake и NDK r27c при компиляции C++ библиотек на Windows.

### Removed
- [Security] Полностью удалены нежелательные сторонние SDK:
  - Google Play Services, Google Play Billing, Google MLKit / Vision, Google Wallet, SafetyNet, Firebase/FCM.
  - Huawei Mobile Services (HMS / AppGallery).
  - Stripe SDK.

---

## [12.9.2] - 2026-08-29

### Added
- [Security] **Duress PIN (Тревожный PIN-код / KABOOM)**:
  - Экстренное стирание всех пользовательских данных, ключей и сессий (`ActivityManager.clearApplicationUserData()`) при вводе тревожного кода.
  - Авто-уничтожение данных после 10 последовательных ошибок ввода PIN.
  - Перемешивание раскладки цифровой клавиатуры (Scramble PIN layout).
- [Security] **Anti-Restrictions & Bypasses**:
  - Снятие запрета на копирование и пересылку (`noforwards`) в каналах и приватных чатах.
  - Полное удаление `FLAG_SECURE`: скриншоты и захват видео разрешены во всех разделах приложения, включая секретные чаты, самоуничтожающиеся фото и истории.
  - Сохранение самоуничтожающихся TTL-медиа с расшифровкой из кэша AES-CTR.
- [Security] **Anti-Delete**:
  - Сохранение удалённых собеседником сообщений с маркировкой удаления.
- [Core] **Неограниченный мультиаккаунт**:
  - Снятие лимита в 3 аккаунта (динамический пул в `SharedConfig`).
  - Поддержка нескольких одновременных авторизаций на один номер.
- [UI] Поддержка динамических системных тем Android 12+ Material You (Monet Light, Monet Dark, Monet AMOLED).
- [UI] Увеличение разрешения отправляемых фото (до 3840px), расширенный размер шрифта чатов (до 72pt).
- [Removed] 100% блокировка рекламы и спонсорских постов Telegram Ads.
