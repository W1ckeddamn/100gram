# Журнал изменений (100gram Changelog)

Все заметные изменения в проекте **100gram** (кастомный форк Telegram для Android) документируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.0.0/), и проект следует принципам семантического версионирования в связке с апстрим-версией Telegram: `[ВЕРСИЯ_TELEGRAM-vX.Y.Z]`.

---

## [Unreleased]

### Added
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
