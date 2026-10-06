package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import android.text.TextUtils;
import org.telegram.tgnet.TLRPC;

public class HundredGramConfig {

    private static final Object sync = new Object();
    private static boolean configLoaded;

    // Interface & Chats
    public static boolean relativeLastSeen;
    public static boolean hideArchiveFolder;
    public static boolean hideFloatingButton;
    public static boolean disableJumpToNextChannel;
    public static boolean hideKeyboardOnScroll;
    public static boolean addCommaAfterMention;
    public static boolean disableNumberRounding;
    public static boolean highlightDeletedMessages;
    public static float deletedMessagesAlpha = 0.65f;

    // Privacy & Profile
    public static boolean hidePhoneNumber;
    public static boolean hideOtherPhoneNumbers;
    public static boolean hideUsername;
    public static boolean hideOtherUsernames;
    public static int showIdAndDc; // 0 = off, 1 = ID, 2 = ID and DC

    // Streamer Mode & Privacy Blur
    public static boolean streamerMode;
    public static String streamerFakeName = "Streamer";
    public static boolean streamerHidePhoneNumbers = true;
    public static boolean streamerHideUsernames = true;
    public static boolean streamerHideAvatars = true;
    public static boolean streamerHideDialogMessages = true;
    public static boolean streamerHideIdAndDc = true;
    public static boolean streamerHideNotificationPreview = true;
    public static boolean streamerHideOtherAccounts = true;
    public static boolean streamerPreventAccidentalCalls = true;
    public static boolean streamerPreventAccidentalDelete = true;
    public static boolean streamerBlockScreenshots = true;
    public static boolean privacyBlurDialogMessages;
    public static boolean privacyBlurAvatars;

    // Double tap actions
    public static final int DOUBLE_TAP_NONE = 0;
    public static final int DOUBLE_TAP_REACTION = 1;
    public static final int DOUBLE_TAP_REPLY = 2;
    public static final int DOUBLE_TAP_COPY = 3;
    public static final int DOUBLE_TAP_FORWARD = 4;
    public static final int DOUBLE_TAP_EDIT = 5;
    public static final int DOUBLE_TAP_SAVE = 6;
    public static final int DOUBLE_TAP_DELETE = 7;

    public static int doubleTapAction;
    public static int doubleTapActionOutOwner;

    // Context menu options
    public static boolean showSaveMessageButton;
    public static boolean showDetailsButton;
    public static boolean showCopyPhotoButton;
    public static boolean showEditHistoryButton;

    // Stage 3: Interface, Navigation & Avatar History
    public static boolean hideSimilarChannels = true;
    public static boolean hideStoriesBar;
    public static boolean showSecondsInTime;
    public static boolean avatarHistoryEnabled = true;
    public static boolean developerModeEnabled = true;
    public static boolean copyCodeButtonEnabled = true;
    public static boolean prettyJsonViewerEnabled = true;
    public static boolean showForwardWithoutAuthorButton = true;

    // Camera & Camera Engine
    public static final int CAMERA_ENGINE_SYSTEM = 0;
    public static final int CAMERA_ENGINE_CAMERAX = 1;
    public static final int CAMERA_ENGINE_CAMERA2 = 2;
    public static final int CAMERA_ENGINE_CAMERA1_LEGACY = 3;
    public static int cameraEngine = CAMERA_ENGINE_CAMERAX;
    public static boolean cameraXEnabled = true;
    public static boolean cameraX60Fps = true;
    public static boolean cameraXZoomSlider = true;
    public static boolean cameraXWideAngle;
    public static boolean cameraPerformanceMode;
    public static int cameraQuality = 2160; // 2160 (4K), 1440 (2K), 1080 (FHD), 720 (HD)
    public static int cameraFps = 60; // 0 = Auto, 24, 30, 60
    public static int cameraDefaultLens = 0; // 0 = Back, 1 = Front, 2 = Last used
    public static int cameraLastUsedLens = 0;
    public static boolean cameraStabilization = true;
    public static int cameraHdr = 1; // 0 = Off, 1 = Auto, 2 = Force On

    // Round Video (Кружочки)
    public static final int CAMERA_ROUND_START_FRONT = 0;     // Front (Selfie)
    public static final int CAMERA_ROUND_START_BACK = 1;      // Back (Main 1x)
    public static final int CAMERA_ROUND_START_MENU = 2;      // Menu upon tap to choose
    public static final int CAMERA_ROUND_START_LAST_USED = 3; // Last used camera

    public static int cameraRoundStartCamera = CAMERA_ROUND_START_FRONT;
    public static boolean cameraRoundLastUsedFront = true;
    public static int cameraRoundResolution = 360; // 240, 360, 480, 720
    public static int cameraRoundFps = 60; // 30, 60
    public static boolean cameraRoundHighBitrate = true;
    public static int cameraRoundBitrate; // 0 = Auto, 750, 1200, 2000, 3000
    public static int cameraRoundCodec = 0; // 0 = H.264, 1 = H.265 (HEVC)
    public static boolean cameraRoundSquare; // Square format for round video notes
    public static boolean cameraRoundDisableBlur; // Disable background dimming/blur during recording
    public static boolean cameraRoundSensor16to9 = true; // 16:9 vs 4:3 sensor for legacy devices

    // Media Vault & Archiver
    public static final int ARCHIVER_MODE_NEW_ONLY = 0; // Только новые сообщения (в реальном времени после включения)
    public static final int ARCHIVER_MODE_FULL_SYNC = 1; // Полная синхронизация (с недавней историей всех чатов)
    public static int backgroundArchiverMode = ARCHIVER_MODE_NEW_ONLY;
    public static long backgroundArchiverActivationTime;
    public static boolean backgroundArchiverEnabled;
    public static boolean autoDownloadMediaVault;
    public static int autoDownloadMaxSizeMb = 20;
    public static int autoDownloadChatsScope; // 0 = private chats, 1 = private & groups, 2 = all including channels
    public static boolean autoDownloadOnlyWifi;
    public static boolean vaultIncludeArchivedChats;
    public static int vaultTtlDays; // 0 = Keep forever, 7, 30, 60, 90
    public static int vaultMaxSizeGb; // 0 = Unlimited, 2, 5, 10, 20
    public static final Set<Long> vaultProtectedDialogs = Collections.synchronizedSet(new HashSet<>());
    public static boolean vaultArchiveChannelComments;

    // Activity Logger & Online Tracker
    public static boolean trackOnlineStatus;
    public static boolean trackSelfActivity = true;
    public static boolean notifyOnUserOnline;
    public static boolean notifyAllContactsOnline;
    public static final Set<Long> trackedOnlineUsers = Collections.synchronizedSet(new HashSet<>());

    // Media & Quality
    public static boolean alwaysHighQualityPhotos = true;
    public static int photoMaxResolution = 3840; // 1280, 2560, 3840
    public static boolean sendVideoAsDocument;

    // Network Boost (Сетевой ускоритель параллельных загрузок и выгрузок)
    public static final int DOWNLOAD_BOOST_OFF = 0;
    public static final int DOWNLOAD_BOOST_FAST = 1;
    public static final int DOWNLOAD_BOOST_ULTRA = 2;
    public static final int DOWNLOAD_BOOST_AUTO = 3;

    public static int downloadBoost = DOWNLOAD_BOOST_FAST;

    public static final int UPLOAD_BOOST_OFF = 0;
    public static final int UPLOAD_BOOST_FAST = 1;
    public static final int UPLOAD_BOOST_ULTRA = 2;
    public static final int UPLOAD_BOOST_AUTO = 3;

    public static int uploadBoost = UPLOAD_BOOST_AUTO;
    public static boolean uploadBoostFileSizeAware = true;
    public static boolean boostDownloadSpeed = true;

    // Throughput & Speed trackers
    private static final Object downloadBytesLock = new Object();
    private static final Object uploadBytesLock = new Object();
    private static long downloadAccumulatedBytes = 0;
    private static long lastDownloadTime = 0;
    private static volatile float currentDownloadSpeedMBps = 0f;

    private static long uploadAccumulatedBytes = 0;
    private static long lastUploadTime = 0;
    private static volatile float currentUploadSpeedMBps = 0f;

    private static final AtomicInteger activeDownloadRequests = new AtomicInteger(0);
    private static final AtomicInteger activeUploadRequests = new AtomicInteger(0);

    // Calls & Links
    public static boolean confirmCalls = true;
    public static boolean cleanUrls = true;

    // Ghost Mode (Режим невидимки)
    public static boolean ghostReadMessages;
    public static boolean ghostReadOnReply = true;
    public static boolean ghostReadMediaContents = true;
    public static boolean ghostShowMarkAsReadOption = true;
    public static boolean ghostTyping;
    public static final int GHOST_STORIES_OFF = 0;
    public static final int GHOST_STORIES_ALWAYS = 1;
    public static final int GHOST_STORIES_PROMPT = 2;
    public static int ghostStoriesMode = GHOST_STORIES_OFF;
    public static boolean unlockStorySaving = true;

    // Auto check delete for all
    public static boolean autoCheckDeleteForAll = true;

    // Ghost Online
    public static boolean ghostOnline;
    public static boolean ghostHideReadDate = true;

    // Messaging & Safety
    public static boolean silentSendByDefault;
    public static boolean confirmSendStickers;
    public static boolean confirmSendGifs;
    public static boolean alwaysForwardWithoutAuthor;

    // Sticker & GIF sizes
    public static final int MEDIA_SIZE_MINIMAL = 0;   // 50%
    public static final int MEDIA_SIZE_COMPACT = 1;   // 75%
    public static final int MEDIA_SIZE_DEFAULT = 2;   // 100%
    public static final int MEDIA_SIZE_LARGE = 3;     // 125%
    public static final int MEDIA_SIZE_HUGE = 4;      // 150%

    public static int stickerSize = MEDIA_SIZE_DEFAULT;
    public static int gifSize = MEDIA_SIZE_DEFAULT;

    // Keep-Alive Service
    public static boolean keepAliveForegroundService = true;

    // Chat Translation
    public static boolean unlockChatTranslation = true;

    // Runtime state (current session)
    public static boolean currentStoryGhostSession;

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }
            if (ApplicationLoader.applicationContext == null) {
                return;
            }
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_config", Activity.MODE_PRIVATE);

            // Interface
            relativeLastSeen = preferences.getBoolean("relativeLastSeen", true);
            hideArchiveFolder = preferences.getBoolean("hideArchiveFolder", false);
            hideFloatingButton = preferences.getBoolean("hideFloatingButton", false);
            disableJumpToNextChannel = preferences.getBoolean("disableJumpToNextChannel", true);
            hideKeyboardOnScroll = preferences.getBoolean("hideKeyboardOnScroll", true);
            addCommaAfterMention = preferences.getBoolean("addCommaAfterMention", true);
            disableNumberRounding = preferences.getBoolean("disableNumberRounding", true);
            highlightDeletedMessages = preferences.getBoolean("highlightDeletedMessages", true);
            deletedMessagesAlpha = preferences.getFloat("deletedMessagesAlpha", 0.65f);

            // Privacy & Profile
            hidePhoneNumber = preferences.getBoolean("hidePhoneNumber", false);
            hideOtherPhoneNumbers = preferences.getBoolean("hideOtherPhoneNumbers", false);
            hideUsername = preferences.getBoolean("hideUsername", false);
            hideOtherUsernames = preferences.getBoolean("hideOtherUsernames", false);
            showIdAndDc = preferences.getInt("showIdAndDc", 2);

            // Double tap & Context menu
            doubleTapAction = preferences.getInt("doubleTapAction", DOUBLE_TAP_REACTION);
            doubleTapActionOutOwner = preferences.getInt("doubleTapActionOutOwner", DOUBLE_TAP_REACTION);
            showSaveMessageButton = preferences.getBoolean("showSaveMessageButton", true);
            showDetailsButton = preferences.getBoolean("showDetailsButton", true);
            showCopyPhotoButton = preferences.getBoolean("showCopyPhotoButton", true);
            showEditHistoryButton = preferences.getBoolean("showEditHistoryButton", true);
            showForwardWithoutAuthorButton = preferences.getBoolean("showForwardWithoutAuthorButton", true);

            // Camera Engine & CameraX
            cameraEngine = preferences.getInt("cameraEngine", CAMERA_ENGINE_CAMERAX);
            cameraXEnabled = (cameraEngine == CAMERA_ENGINE_CAMERAX);
            if (cameraEngine == CAMERA_ENGINE_CAMERA2) {
                SharedConfig.useCamera2Force = Boolean.TRUE;
            } else if (cameraEngine == CAMERA_ENGINE_CAMERA1_LEGACY) {
                SharedConfig.useCamera2Force = Boolean.FALSE;
            }
            cameraX60Fps = preferences.getBoolean("cameraX60Fps", true);
            cameraXZoomSlider = preferences.getBoolean("cameraXZoomSlider", true);
            cameraXWideAngle = preferences.getBoolean("cameraXWideAngle", false);
            cameraPerformanceMode = preferences.getBoolean("cameraPerformanceMode", false);
            cameraQuality = preferences.getInt("cameraQuality", 2160);
            cameraFps = preferences.getInt("cameraFps", 60);
            cameraDefaultLens = preferences.getInt("cameraDefaultLens", 0);
            cameraLastUsedLens = preferences.getInt("cameraLastUsedLens", 0);
            cameraStabilization = preferences.getBoolean("cameraStabilization", true);
            cameraHdr = preferences.getInt("cameraHdr", 1);

            // Round Video
            cameraRoundStartCamera = preferences.getInt("cameraRoundStartCamera", CAMERA_ROUND_START_FRONT);
            cameraRoundLastUsedFront = preferences.getBoolean("cameraRoundLastUsedFront", true);
            cameraRoundResolution = preferences.getInt("cameraRoundResolution", 360);
            // Допустимые значения: 240, 360, 480 (720 не поддерживается сервером Telegram и превращается в обычное видео)
            if (cameraRoundResolution > 480 || (cameraRoundResolution != 240 && cameraRoundResolution != 360 && cameraRoundResolution != 480)) {
                cameraRoundResolution = 360;
                saveInt("cameraRoundResolution", 360);
            }
            cameraRoundFps = preferences.getInt("cameraRoundFps", 60);
            cameraRoundHighBitrate = preferences.getBoolean("cameraRoundHighBitrate", true);
            cameraRoundBitrate = preferences.getInt("cameraRoundBitrate", 0);
            cameraRoundCodec = preferences.getInt("cameraRoundCodec", 0);
            if (cameraRoundCodec != 0) {
                cameraRoundCodec = 0;
                saveInt("cameraRoundCodec", 0);
            }
            cameraRoundSquare = preferences.getBoolean("cameraRoundSquare", false);
            cameraRoundDisableBlur = preferences.getBoolean("cameraRoundDisableBlur", false);
            cameraRoundSensor16to9 = preferences.getBoolean("cameraRoundSensor16to9", true);
            SharedConfig.roundCamera16to9 = cameraRoundSensor16to9;

            // Media Vault & Archiver
            backgroundArchiverEnabled = preferences.getBoolean("backgroundArchiverEnabled", false);
            backgroundArchiverMode = preferences.getInt("backgroundArchiverMode", ARCHIVER_MODE_NEW_ONLY);
            backgroundArchiverActivationTime = preferences.getLong("backgroundArchiverActivationTime", 0L);
            if (backgroundArchiverEnabled && backgroundArchiverActivationTime == 0L) {
                backgroundArchiverActivationTime = System.currentTimeMillis() / 1000L;
                saveLong("backgroundArchiverActivationTime", backgroundArchiverActivationTime);
            }
            autoDownloadMediaVault = preferences.getBoolean("autoDownloadMediaVault", false);
            autoDownloadMaxSizeMb = preferences.getInt("autoDownloadMaxSizeMb", 20);
            autoDownloadChatsScope = preferences.getInt("autoDownloadChatsScope", 0);
            autoDownloadOnlyWifi = preferences.getBoolean("autoDownloadOnlyWifi", false);
            vaultIncludeArchivedChats = preferences.getBoolean("vaultIncludeArchivedChats", false);
            vaultTtlDays = preferences.getInt("vaultTtlDays", 0);
            vaultMaxSizeGb = preferences.getInt("vaultMaxSizeGb", 0);
            vaultArchiveChannelComments = preferences.getBoolean("vaultArchiveChannelComments", false);
            String protectedStr = preferences.getString("vaultProtectedDialogs", "");
            vaultProtectedDialogs.clear();
            if (!TextUtils.isEmpty(protectedStr)) {
                String[] parts = protectedStr.split(",");
                for (String p : parts) {
                    try {
                        long did = Long.parseLong(p.trim());
                        if (did != 0) vaultProtectedDialogs.add(did);
                    } catch (Exception ignored) {}
                }
            }

            // Activity Logger & Online Tracker
            trackOnlineStatus = preferences.getBoolean("trackOnlineStatus", true);
            trackSelfActivity = preferences.getBoolean("trackSelfActivity", true);
            notifyOnUserOnline = preferences.getBoolean("notifyOnUserOnline", false);
            notifyAllContactsOnline = preferences.getBoolean("notifyAllContactsOnline", false);
            String trackedStr = preferences.getString("trackedOnlineUsers", "");
            trackedOnlineUsers.clear();
            if (!TextUtils.isEmpty(trackedStr)) {
                String[] parts = trackedStr.split(",");
                for (String p : parts) {
                    try {
                        long uid = Long.parseLong(p.trim());
                        if (uid > 0) trackedOnlineUsers.add(uid);
                    } catch (Exception ignored) {}
                }
            }

            // Media & Quality
            alwaysHighQualityPhotos = preferences.getBoolean("alwaysHighQualityPhotos", true);
            photoMaxResolution = preferences.getInt("photoMaxResolution", 3840);
            sendVideoAsDocument = preferences.getBoolean("sendVideoAsDocument", false);
            // Network Boost
            downloadBoost = preferences.getInt("downloadBoost", DOWNLOAD_BOOST_FAST);
            uploadBoost = preferences.getInt("uploadBoost", UPLOAD_BOOST_AUTO);
            uploadBoostFileSizeAware = preferences.getBoolean("uploadBoostFileSizeAware", true);
            // boostDownloadSpeed — производный флаг, не загружаем отдельно
            // (ранее вызывал конфликт: перезаписывал намеренный downloadBoost=OFF на FAST)
            boostDownloadSpeed = (downloadBoost != DOWNLOAD_BOOST_OFF);

            // Calls & Links
            confirmCalls = preferences.getBoolean("confirmCalls", true);
            cleanUrls = preferences.getBoolean("cleanUrls", true);

            // Ghost Mode
            ghostReadMessages = preferences.getBoolean("ghostReadMessages", false);
            ghostReadOnReply = preferences.getBoolean("ghostReadOnReply", true);
            ghostReadMediaContents = preferences.getBoolean("ghostReadMediaContents", true);
            ghostShowMarkAsReadOption = preferences.getBoolean("ghostShowMarkAsReadOption", true);
            ghostTyping = preferences.getBoolean("ghostTyping", false);
            ghostStoriesMode = preferences.getInt("ghostStoriesMode", GHOST_STORIES_OFF);
            unlockStorySaving = preferences.getBoolean("unlockStorySaving", true);
            autoCheckDeleteForAll = preferences.getBoolean("autoCheckDeleteForAll", true);
            ghostOnline = preferences.getBoolean("ghostOnline", false);
            ghostHideReadDate = preferences.getBoolean("ghostHideReadDate", true);
            silentSendByDefault = preferences.getBoolean("silentSendByDefault", false);
            confirmSendStickers = preferences.getBoolean("confirmSendStickers", false);
            confirmSendGifs = preferences.getBoolean("confirmSendGifs", false);
            stickerSize = preferences.getInt("stickerSize", MEDIA_SIZE_DEFAULT);
            gifSize = preferences.getInt("gifSize", MEDIA_SIZE_DEFAULT);
            alwaysForwardWithoutAuthor = preferences.getBoolean("alwaysForwardWithoutAuthor", false);
            keepAliveForegroundService = preferences.getBoolean("keepAliveForegroundService", true);
            unlockChatTranslation = preferences.getBoolean("unlockChatTranslation", true);

            streamerMode = preferences.getBoolean("streamerMode", false);
            streamerFakeName = preferences.getString("streamerFakeName", "Streamer");
            streamerHidePhoneNumbers = preferences.getBoolean("streamerHidePhoneNumbers", true);
            streamerHideUsernames = preferences.getBoolean("streamerHideUsernames", true);
            streamerHideAvatars = preferences.getBoolean("streamerHideAvatars", true);
            streamerHideDialogMessages = preferences.getBoolean("streamerHideDialogMessages", true);
            streamerHideIdAndDc = preferences.getBoolean("streamerHideIdAndDc", true);
            streamerHideNotificationPreview = preferences.getBoolean("streamerHideNotificationPreview", true);
            streamerHideOtherAccounts = preferences.getBoolean("streamerHideOtherAccounts", true);
            streamerPreventAccidentalCalls = preferences.getBoolean("streamerPreventAccidentalCalls", true);
            streamerPreventAccidentalDelete = preferences.getBoolean("streamerPreventAccidentalDelete", true);
            streamerBlockScreenshots = preferences.getBoolean("streamerBlockScreenshots", true);
            privacyBlurDialogMessages = preferences.getBoolean("privacyBlurDialogMessages", false);
            privacyBlurAvatars = preferences.getBoolean("privacyBlurAvatars", false);

            hideSimilarChannels = preferences.getBoolean("hideSimilarChannels", true);
            hideStoriesBar = preferences.getBoolean("hideStoriesBar", false);
            showSecondsInTime = preferences.getBoolean("showSecondsInTime", false);
            avatarHistoryEnabled = preferences.getBoolean("avatarHistoryEnabled", true);
            developerModeEnabled = preferences.getBoolean("developerModeEnabled", true);
            copyCodeButtonEnabled = preferences.getBoolean("copyCodeButtonEnabled", true);
            prettyJsonViewerEnabled = preferences.getBoolean("prettyJsonViewerEnabled", true);

            configLoaded = true;
        }
    }

    public static void postNotificationToAllAccounts(int id, Object... args) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            NotificationCenter.getInstance(a).postNotificationName(id, args);
        }
        NotificationCenter.getGlobalInstance().postNotificationName(id, args);
    }

    public static void toggleRelativeLastSeen() {
        relativeLastSeen = !relativeLastSeen;
        save("relativeLastSeen", relativeLastSeen);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
    }

    public static void toggleHideArchiveFolder() {
        hideArchiveFolder = !hideArchiveFolder;
        save("hideArchiveFolder", hideArchiveFolder);
        postNotificationToAllAccounts(NotificationCenter.dialogsNeedReload);
    }

    public static void toggleHideFloatingButton() {
        hideFloatingButton = !hideFloatingButton;
        save("hideFloatingButton", hideFloatingButton);
        postNotificationToAllAccounts(NotificationCenter.dialogsNeedReload);
    }

    public static void toggleDisableJumpToNextChannel() {
        disableJumpToNextChannel = !disableJumpToNextChannel;
        save("disableJumpToNextChannel", disableJumpToNextChannel);
    }

    public static void toggleHideKeyboardOnScroll() {
        hideKeyboardOnScroll = !hideKeyboardOnScroll;
        save("hideKeyboardOnScroll", hideKeyboardOnScroll);
    }

    public static void toggleAddCommaAfterMention() {
        addCommaAfterMention = !addCommaAfterMention;
        save("addCommaAfterMention", addCommaAfterMention);
    }

    public static void toggleDisableNumberRounding() {
        disableNumberRounding = !disableNumberRounding;
        save("disableNumberRounding", disableNumberRounding);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleHighlightDeletedMessages() {
        highlightDeletedMessages = !highlightDeletedMessages;
        save("highlightDeletedMessages", highlightDeletedMessages);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void setDeletedMessagesAlpha(float alpha) {
        deletedMessagesAlpha = alpha;
        saveFloat("deletedMessagesAlpha", alpha);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleHidePhoneNumber() {
        hidePhoneNumber = !hidePhoneNumber;
        save("hidePhoneNumber", hidePhoneNumber);
        postNotificationToAllAccounts(NotificationCenter.mainUserInfoChanged);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleHideOtherPhoneNumbers() {
        hideOtherPhoneNumbers = !hideOtherPhoneNumbers;
        save("hideOtherPhoneNumbers", hideOtherPhoneNumbers);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleHideUsername() {
        hideUsername = !hideUsername;
        save("hideUsername", hideUsername);
        postNotificationToAllAccounts(NotificationCenter.mainUserInfoChanged);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleHideOtherUsernames() {
        hideOtherUsernames = !hideOtherUsernames;
        save("hideOtherUsernames", hideOtherUsernames);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleShowIdAndDc() {
        showIdAndDc = (showIdAndDc + 1) % 3;
        saveInt("showIdAndDc", showIdAndDc);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void setDoubleTapAction(int action) {
        doubleTapAction = action;
        saveInt("doubleTapAction", doubleTapAction);
    }

    public static void setDoubleTapActionOutOwner(int action) {
        doubleTapActionOutOwner = action;
        saveInt("doubleTapActionOutOwner", doubleTapActionOutOwner);
    }

    public static void toggleShowSaveMessageButton() {
        showSaveMessageButton = !showSaveMessageButton;
        save("showSaveMessageButton", showSaveMessageButton);
    }

    public static void toggleShowDetailsButton() {
        showDetailsButton = !showDetailsButton;
        save("showDetailsButton", showDetailsButton);
    }

    public static void toggleShowCopyPhotoButton() {
        showCopyPhotoButton = !showCopyPhotoButton;
        save("showCopyPhotoButton", showCopyPhotoButton);
    }

    public static void toggleShowEditHistoryButton() {
        showEditHistoryButton = !showEditHistoryButton;
        save("showEditHistoryButton", showEditHistoryButton);
    }

    public static void toggleShowForwardWithoutAuthorButton() {
        showForwardWithoutAuthorButton = !showForwardWithoutAuthorButton;
        save("showForwardWithoutAuthorButton", showForwardWithoutAuthorButton);
    }

    public static void toggleCameraXEnabled() {
        cameraXEnabled = !cameraXEnabled;
        save("cameraXEnabled", cameraXEnabled);
    }

    public static void toggleCameraX60Fps() {
        cameraX60Fps = !cameraX60Fps;
        save("cameraX60Fps", cameraX60Fps);
    }

    public static void toggleCameraXZoomSlider() {
        cameraXZoomSlider = !cameraXZoomSlider;
        save("cameraXZoomSlider", cameraXZoomSlider);
    }

    public static void toggleCameraXWideAngle() {
        cameraXWideAngle = !cameraXWideAngle;
        save("cameraXWideAngle", cameraXWideAngle);
    }

    public static void setCameraEngine(int engine) {
        cameraEngine = engine;
        cameraXEnabled = (engine == CAMERA_ENGINE_CAMERAX);
        if (engine == CAMERA_ENGINE_CAMERA2) {
            SharedConfig.useCamera2Force = Boolean.TRUE;
        } else if (engine == CAMERA_ENGINE_CAMERA1_LEGACY) {
            SharedConfig.useCamera2Force = Boolean.FALSE;
        } else if (engine == CAMERA_ENGINE_SYSTEM) {
            SharedConfig.useCamera2Force = null;
        }
        saveInt("cameraEngine", cameraEngine);
        save("cameraXEnabled", cameraXEnabled);
        try {
            if (ApplicationLoader.applicationContext != null) {
                ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE)
                        .edit()
                        .putBoolean("useCamera2Force_2", SharedConfig.useCamera2Force != null && SharedConfig.useCamera2Force)
                        .apply();
            }
        } catch (Exception ignored) {}
    }

    public static void toggleCameraPerformanceMode() {
        cameraPerformanceMode = !cameraPerformanceMode;
        save("cameraPerformanceMode", cameraPerformanceMode);
    }

    public static void setCameraQuality(int quality) {
        cameraQuality = quality;
        saveInt("cameraQuality", cameraQuality);
    }

    public static void setCameraFps(int fps) {
        cameraFps = fps;
        saveInt("cameraFps", cameraFps);
    }

    public static void setCameraDefaultLens(int lens) {
        cameraDefaultLens = lens;
        saveInt("cameraDefaultLens", cameraDefaultLens);
    }

    public static void setCameraLastUsedLens(int lens) {
        cameraLastUsedLens = lens;
        saveInt("cameraLastUsedLens", cameraLastUsedLens);
    }

    public static void toggleCameraStabilization() {
        cameraStabilization = !cameraStabilization;
        save("cameraStabilization", cameraStabilization);
    }

    public static void setCameraHdr(int hdr) {
        cameraHdr = hdr;
        saveInt("cameraHdr", cameraHdr);
    }

    public static void setCameraRoundStartCamera(int startMode) {
        cameraRoundStartCamera = startMode;
        saveInt("cameraRoundStartCamera", cameraRoundStartCamera);
    }

    public static void setCameraRoundLastUsedFront(boolean front) {
        cameraRoundLastUsedFront = front;
        save("cameraRoundLastUsedFront", cameraRoundLastUsedFront);
    }

    public static void setCameraRoundResolution(int res) {
        // Нормализуем к ближайшему допустимому значению: 240, 360, 480 (максимум Telegram)
        if (res > 360) res = 480;
        else if (res > 240) res = 360;
        else res = 240;
        cameraRoundResolution = res;
        saveInt("cameraRoundResolution", cameraRoundResolution);
    }

    public static void setCameraRoundFps(int fps) {
        cameraRoundFps = (fps == 60 ? 60 : 30);
        saveInt("cameraRoundFps", cameraRoundFps);
    }

    public static void toggleCameraRoundHighBitrate() {
        cameraRoundHighBitrate = !cameraRoundHighBitrate;
        save("cameraRoundHighBitrate", cameraRoundHighBitrate);
    }

    public static void setCameraRoundBitrate(int bitrate) {
        cameraRoundBitrate = bitrate;
        saveInt("cameraRoundBitrate", cameraRoundBitrate);
    }

    public static void toggleCameraRoundSquare() {
        cameraRoundSquare = !cameraRoundSquare;
        save("cameraRoundSquare", cameraRoundSquare);
    }

    public static void toggleCameraRoundDisableBlur() {
        cameraRoundDisableBlur = !cameraRoundDisableBlur;
        save("cameraRoundDisableBlur", cameraRoundDisableBlur);
    }

    public static void toggleCameraRoundSensor16to9() {
        cameraRoundSensor16to9 = !cameraRoundSensor16to9;
        SharedConfig.roundCamera16to9 = cameraRoundSensor16to9;
        save("cameraRoundSensor16to9", cameraRoundSensor16to9);
        try {
            MessagesController.getGlobalMainSettings().edit().putBoolean("roundCamera16to9", cameraRoundSensor16to9).apply();
        } catch (Exception ignored) {}
    }

    /**
     * H.265/HEVC не поддерживается Telegram-сервером для видеосообщений (round video notes).
     * Параметр {@code codec} игнорируется — всегда принудительно устанавливается H.264 (AVC).
     */
    public static void setCameraRoundCodec(int codec) {
        // Сервер Telegram поддерживает только H.264 для round video notes
        if (cameraRoundCodec != 0) {
            cameraRoundCodec = 0;
            saveInt("cameraRoundCodec", 0);
        }
    }

    public static void setDownloadBoost(int mode) {
        downloadBoost = mode;
        boostDownloadSpeed = (downloadBoost != DOWNLOAD_BOOST_OFF);
        saveInt("downloadBoost", downloadBoost);
        save("boostDownloadSpeed", boostDownloadSpeed);
    }

    public static void setUploadBoost(int mode) {
        uploadBoost = mode;
        saveInt("uploadBoost", uploadBoost);
    }

    public static void toggleUploadBoostFileSizeAware() {
        uploadBoostFileSizeAware = !uploadBoostFileSizeAware;
        save("uploadBoostFileSizeAware", uploadBoostFileSizeAware);
    }

    public static int getDownloadParallelRequests(long totalBytesCount) {
        int mode = downloadBoost;
        if (mode == DOWNLOAD_BOOST_AUTO) {
            int net = ApplicationLoader.getAutodownloadNetworkType();
            if (net == StatsController.TYPE_WIFI) {
                mode = DOWNLOAD_BOOST_ULTRA;
            } else if (net == StatsController.TYPE_MOBILE) {
                mode = DOWNLOAD_BOOST_FAST;
            } else {
                mode = DOWNLOAD_BOOST_OFF;
            }
        }
        switch (mode) {
            case DOWNLOAD_BOOST_ULTRA:
                return 12;
            case DOWNLOAD_BOOST_FAST:
                return 8;
            case DOWNLOAD_BOOST_OFF:
            default:
                return 4;
        }
    }

    public static int getDownloadChunkSize(long totalBytesCount) {
        int mode = downloadBoost;
        if (mode == DOWNLOAD_BOOST_AUTO) {
            int net = ApplicationLoader.getAutodownloadNetworkType();
            if (net == StatsController.TYPE_WIFI) {
                mode = DOWNLOAD_BOOST_ULTRA;
            } else if (net == StatsController.TYPE_MOBILE) {
                mode = DOWNLOAD_BOOST_FAST;
            } else {
                mode = DOWNLOAD_BOOST_OFF;
            }
        }
        switch (mode) {
            case DOWNLOAD_BOOST_ULTRA:
                return 1024 * 1024; // 1 MB
            case DOWNLOAD_BOOST_FAST:
                return 1024 * 512;  // 512 KB
            case DOWNLOAD_BOOST_OFF:
            default:
                return 1024 * 128;  // 128 KB
        }
    }

    public static int getUploadParallelRequests(long totalFileSize) {
        int mode = uploadBoost;
        if (mode == UPLOAD_BOOST_OFF) {
            return 4;
        }
        if (mode == UPLOAD_BOOST_AUTO) {
            int net = ApplicationLoader.getAutodownloadNetworkType();
            if (net == StatsController.TYPE_WIFI) {
                mode = UPLOAD_BOOST_ULTRA;
            } else if (net == StatsController.TYPE_MOBILE) {
                mode = UPLOAD_BOOST_FAST;
            } else {
                mode = UPLOAD_BOOST_OFF;
            }
        }
        if (uploadBoostFileSizeAware) {
            if (totalFileSize > 0 && totalFileSize < 10 * 1024 * 1024L) {
                return 4;
            }
            if (totalFileSize >= 100 * 1024 * 1024L && mode == UPLOAD_BOOST_ULTRA) {
                return 8;
            }
            return 6;
        }
        return mode == UPLOAD_BOOST_ULTRA ? 8 : (mode == UPLOAD_BOOST_FAST ? 6 : 4);
    }

    public static int getUploadMaxKBytes(long totalFileSize) {
        int parallel = getUploadParallelRequests(totalFileSize);
        return parallel * 1024;
    }

    public static void trackBytes(boolean isUpload, int bytes) {
        if (bytes <= 0) return;
        long now = System.currentTimeMillis();
        if (isUpload) {
            synchronized (uploadBytesLock) {
                uploadAccumulatedBytes += bytes;
                if (lastUploadTime == 0) lastUploadTime = now;
                if (now - lastUploadTime >= 1000) {
                    float elapsedSec = Math.max(0.1f, (now - lastUploadTime) / 1000f);
                    currentUploadSpeedMBps = (uploadAccumulatedBytes / (1024f * 1024f)) / elapsedSec;
                    uploadAccumulatedBytes = 0;
                    lastUploadTime = now;
                }
            }
        } else {
            synchronized (downloadBytesLock) {
                downloadAccumulatedBytes += bytes;
                if (lastDownloadTime == 0) lastDownloadTime = now;
                if (now - lastDownloadTime >= 1000) {
                    float elapsedSec = Math.max(0.1f, (now - lastDownloadTime) / 1000f);
                    currentDownloadSpeedMBps = (downloadAccumulatedBytes / (1024f * 1024f)) / elapsedSec;
                    downloadAccumulatedBytes = 0;
                    lastDownloadTime = now;
                }
            }
        }
    }

    public static float getDownloadSpeedMBps() {
        synchronized (downloadBytesLock) {
            if (System.currentTimeMillis() - lastDownloadTime > 2500) {
                currentDownloadSpeedMBps = 0f;
            }
        }
        return currentDownloadSpeedMBps;
    }

    public static float getUploadSpeedMBps() {
        synchronized (uploadBytesLock) {
            if (System.currentTimeMillis() - lastUploadTime > 2500) {
                currentUploadSpeedMBps = 0f;
            }
        }
        return currentUploadSpeedMBps;
    }

    public static void incrementActiveDownloadThreads() {
        activeDownloadRequests.incrementAndGet();
    }

    public static void decrementActiveDownloadThreads() {
        activeDownloadRequests.updateAndGet(c -> Math.max(0, c - 1));
    }

    public static int getActiveDownloadThreads() {
        return activeDownloadRequests.get();
    }

    public static void incrementActiveUploadThreads() {
        activeUploadRequests.incrementAndGet();
    }

    public static void decrementActiveUploadThreads() {
        activeUploadRequests.updateAndGet(c -> Math.max(0, c - 1));
    }

    public static int getActiveUploadThreads() {
        return activeUploadRequests.get();
    }

    public static void toggleBackgroundArchiverEnabled() {
        backgroundArchiverEnabled = !backgroundArchiverEnabled;
        if (backgroundArchiverEnabled) {
            backgroundArchiverActivationTime = System.currentTimeMillis() / 1000L;
            saveLong("backgroundArchiverActivationTime", backgroundArchiverActivationTime);
        }
        save("backgroundArchiverEnabled", backgroundArchiverEnabled);
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                try {
                    if (backgroundArchiverEnabled) {
                        HundredGramBackgroundArchiver.getInstance(a).start();
                        if (backgroundArchiverMode == ARCHIVER_MODE_FULL_SYNC) {
                            HundredGramBackgroundArchiver.getInstance(a).triggerSync(true);
                        }
                    } else {
                        HundredGramBackgroundArchiver.getInstance(a).stop();
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    public static void setBackgroundArchiverMode(int mode) {
        if (backgroundArchiverMode != mode) {
            backgroundArchiverMode = mode;
            saveInt("backgroundArchiverMode", backgroundArchiverMode);
            if (backgroundArchiverEnabled) {
                if (backgroundArchiverMode == ARCHIVER_MODE_FULL_SYNC) {
                    for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        if (UserConfig.getInstance(a).isClientActivated()) {
                            HundredGramBackgroundArchiver.getInstance(a).triggerSync(true);
                        }
                    }
                } else if (backgroundArchiverMode == ARCHIVER_MODE_NEW_ONLY) {
                    backgroundArchiverActivationTime = System.currentTimeMillis() / 1000L;
                    saveLong("backgroundArchiverActivationTime", backgroundArchiverActivationTime);
                }
            }
        }
    }

    public static void toggleAutoDownloadMediaVault() {
        autoDownloadMediaVault = !autoDownloadMediaVault;
        save("autoDownloadMediaVault", autoDownloadMediaVault);
    }

    public static void setAutoDownloadMaxSizeMb(int mb) {
        autoDownloadMaxSizeMb = mb;
        saveInt("autoDownloadMaxSizeMb", autoDownloadMaxSizeMb);
    }

    public static void setAutoDownloadChatsScope(int scope) {
        autoDownloadChatsScope = scope;
        saveInt("autoDownloadChatsScope", autoDownloadChatsScope);
    }

    public static void toggleAutoDownloadOnlyWifi() {
        autoDownloadOnlyWifi = !autoDownloadOnlyWifi;
        save("autoDownloadOnlyWifi", autoDownloadOnlyWifi);
    }

    public static void toggleVaultIncludeArchivedChats() {
        vaultIncludeArchivedChats = !vaultIncludeArchivedChats;
        save("vaultIncludeArchivedChats", vaultIncludeArchivedChats);
    }

    public static void toggleVaultArchiveChannelComments() {
        vaultArchiveChannelComments = !vaultArchiveChannelComments;
        save("vaultArchiveChannelComments", vaultArchiveChannelComments);
    }

    public static void setVaultTtlDays(int days) {
        vaultTtlDays = days;
        saveInt("vaultTtlDays", vaultTtlDays);
    }

    public static void setVaultMaxSizeGb(int gb) {
        vaultMaxSizeGb = gb;
        saveInt("vaultMaxSizeGb", vaultMaxSizeGb);
    }

    public static boolean isVaultNeverExpire(long dialogId) {
        return vaultProtectedDialogs.contains(dialogId);
    }

    public static void setVaultNeverExpire(long dialogId, boolean protect) {
        if (protect) {
            vaultProtectedDialogs.add(dialogId);
        } else {
            vaultProtectedDialogs.remove(dialogId);
        }
        saveVaultProtectedDialogs();
    }

    private static void saveVaultProtectedDialogs() {
        if (ApplicationLoader.applicationContext == null) return;
        StringBuilder sb = new StringBuilder();
        for (Long did : vaultProtectedDialogs) {
            if (sb.length() > 0) sb.append(",");
            sb.append(did);
        }
        ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_config", Activity.MODE_PRIVATE)
                .edit()
                .putString("vaultProtectedDialogs", sb.toString())
                .apply();
    }

    public static void toggleTrackOnlineStatus() {
        trackOnlineStatus = !trackOnlineStatus;
        save("trackOnlineStatus", trackOnlineStatus);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
    }

    public static void toggleTrackSelfActivity() {
        trackSelfActivity = !trackSelfActivity;
        save("trackSelfActivity", trackSelfActivity);
    }

    public static void toggleNotifyOnUserOnline() {
        notifyOnUserOnline = !notifyOnUserOnline;
        save("notifyOnUserOnline", notifyOnUserOnline);
    }

    public static void toggleNotifyAllContactsOnline() {
        notifyAllContactsOnline = !notifyAllContactsOnline;
        save("notifyAllContactsOnline", notifyAllContactsOnline);
    }

    public static boolean isTrackedUser(long userId) {
        return trackedOnlineUsers.contains(userId);
    }

    public static boolean isUserTrackedForOnline(long userId) {
        return trackedOnlineUsers.contains(userId);
    }

    public static void setTrackedUser(long userId, boolean enabled) {
        if (userId <= 0) return;
        if (enabled) {
            trackedOnlineUsers.add(userId);
            if (!notifyOnUserOnline) {
                notifyOnUserOnline = true;
                save("notifyOnUserOnline", true);
            }
        } else {
            trackedOnlineUsers.remove(userId);
        }
        saveTrackedOnlineUsers();
    }

    public static java.util.Set<Long> getTrackedOnlineUsers() {
        return new java.util.HashSet<>(trackedOnlineUsers);
    }

    public static boolean toggleUserTrackedForOnline(long userId) {
        boolean tracked;
        if (trackedOnlineUsers.contains(userId)) {
            trackedOnlineUsers.remove(userId);
            tracked = false;
        } else {
            trackedOnlineUsers.add(userId);
            tracked = true;
            if (!notifyOnUserOnline) {
                notifyOnUserOnline = true;
                save("notifyOnUserOnline", true);
            }
        }
        saveTrackedOnlineUsers();
        return tracked;
    }

    public static void clearTrackedOnlineUsers() {
        trackedOnlineUsers.clear();
        saveTrackedOnlineUsers();
    }

    public static void saveTrackedOnlineUsers() {
        if (ApplicationLoader.applicationContext == null) return;
        StringBuilder sb = new StringBuilder();
        for (Long uid : trackedOnlineUsers) {
            if (sb.length() > 0) sb.append(",");
            sb.append(uid);
        }
        ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_config", Activity.MODE_PRIVATE)
                .edit()
                .putString("trackedOnlineUsers", sb.toString())
                .apply();
    }

    private static void save(String key, boolean val) {
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_config", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean(key, val)
                    .apply();
        }
    }

    private static void saveInt(String key, int val) {
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_config", Activity.MODE_PRIVATE)
                    .edit()
                    .putInt(key, val)
                    .apply();
        }
    }

    private static void saveFloat(String key, float val) {
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_config", Activity.MODE_PRIVATE)
                    .edit()
                    .putFloat(key, val)
                    .apply();
        }
    }

    private static void saveString(String key, String val) {
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_config", Activity.MODE_PRIVATE)
                    .edit()
                    .putString(key, val)
                    .apply();
        }
    }

    private static void saveLong(String key, long val) {
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_config", Activity.MODE_PRIVATE)
                    .edit()
                    .putLong(key, val)
                    .apply();
        }
    }

    // --- Chat Vault & Per-Chat Overrides ---
    public static final int OVERRIDE_DEFAULT = 0;
    public static final int OVERRIDE_ENABLED = 1;
    public static final int OVERRIDE_DISABLED = 2;

    public static final int VAULT_MODE_DEFAULT = 0;
    public static final int VAULT_MODE_ALWAYS = 1;
    public static final int VAULT_MODE_NEVER = 2;

    // 100gram: Global fallback auto-download size limit for vault emergency downloads (MB)
    public static int vaultMaxAutodownloadSizeMb = 100;

    public static final int VAULT_TYPE_PHOTO = 1;
    public static final int VAULT_TYPE_VIDEO = 2;
    public static final int VAULT_TYPE_VOICE_ROUND = 3;
    public static final int VAULT_TYPE_DOCUMENT = 4;

    public static int getChatVaultMode(long dialogId) {
        if (ApplicationLoader.applicationContext == null) return VAULT_MODE_DEFAULT;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE);
        return prefs.getInt("mode_" + dialogId, VAULT_MODE_DEFAULT);
    }

    public static void setChatVaultMode(long dialogId, int mode) {
        if (ApplicationLoader.applicationContext == null) return;
        ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE)
                .edit()
                .putInt("mode_" + dialogId, mode)
                .apply();
    }

    public static boolean isChatVaultTypeAllowed(long dialogId, int type) {
        if (ApplicationLoader.applicationContext == null) return true;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE);
        switch (type) {
            case VAULT_TYPE_PHOTO:
                return prefs.getBoolean("photos_" + dialogId, true);
            case VAULT_TYPE_VIDEO:
                return prefs.getBoolean("videos_" + dialogId, true);
            case VAULT_TYPE_VOICE_ROUND:
                return prefs.getBoolean("voices_" + dialogId, true);
            case VAULT_TYPE_DOCUMENT:
                return prefs.getBoolean("docs_" + dialogId, true);
            default:
                return true;
        }
    }

    public static void setChatVaultTypeAllowed(long dialogId, int type, boolean allowed) {
        if (ApplicationLoader.applicationContext == null) return;
        String key;
        switch (type) {
            case VAULT_TYPE_PHOTO: key = "photos_" + dialogId; break;
            case VAULT_TYPE_VIDEO: key = "videos_" + dialogId; break;
            case VAULT_TYPE_VOICE_ROUND: key = "voices_" + dialogId; break;
            case VAULT_TYPE_DOCUMENT: key = "docs_" + dialogId; break;
            default: return;
        }
        ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE)
                .edit()
                .putBoolean(key, allowed)
                .apply();
    }

    public static int getChatVaultMaxSizeMb(long dialogId) {
        if (ApplicationLoader.applicationContext == null) return 0;
        return ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE)
                .getInt("max_size_" + dialogId, 0);
    }

    public static void setChatVaultMaxSizeMb(long dialogId, int maxSizeMb) {
        if (ApplicationLoader.applicationContext == null) return;
        ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE)
                .edit()
                .putInt("max_size_" + dialogId, maxSizeMb)
                .apply();
    }

    public static boolean hasChatVaultException(long dialogId) {
        if (ApplicationLoader.applicationContext == null) return false;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE);
        if (prefs.getInt("mode_" + dialogId, VAULT_MODE_DEFAULT) != VAULT_MODE_DEFAULT) {
            return true;
        }
        if (prefs.contains("photos_" + dialogId) || prefs.contains("videos_" + dialogId) ||
            prefs.contains("voices_" + dialogId) || prefs.contains("docs_" + dialogId)) {
            return true;
        }
        if (prefs.getInt("max_size_" + dialogId, 0) != 0) {
            return true;
        }
        return false;
    }

    public static void clearChatVaultSettings(long dialogId) {
        if (ApplicationLoader.applicationContext == null) return;
        ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE)
                .edit()
                .remove("mode_" + dialogId)
                .remove("photos_" + dialogId)
                .remove("videos_" + dialogId)
                .remove("voices_" + dialogId)
                .remove("docs_" + dialogId)
                .remove("max_size_" + dialogId)
                .apply();
    }

    public static int getChatOverride(long dialogId, String key) {
        if (ApplicationLoader.applicationContext == null) return OVERRIDE_DEFAULT;
        return ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_chat_overrides", Activity.MODE_PRIVATE)
                .getInt(key + "_" + dialogId, OVERRIDE_DEFAULT);
    }

    public static void setChatOverride(long dialogId, String key, int value) {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_chat_overrides", Activity.MODE_PRIVATE).edit();
        if (value == OVERRIDE_DEFAULT) {
            editor.remove(key + "_" + dialogId);
        } else {
            editor.putInt(key + "_" + dialogId, value);
        }
        editor.apply();
    }

    public static boolean isChatGhostRead(long dialogId) {
        int val = getChatOverride(dialogId, "ghost_read");
        if (val == OVERRIDE_ENABLED) return true;
        if (val == OVERRIDE_DISABLED) return false;
        return ghostReadMessages;
    }

    public static boolean isChatGhostTyping(long dialogId) {
        int val = getChatOverride(dialogId, "ghost_typing");
        if (val == OVERRIDE_ENABLED) return true;
        if (val == OVERRIDE_DISABLED) return false;
        return ghostTyping;
    }

    public static boolean isChatHidePhone(long dialogId) {
        return isChatHidePhone(dialogId, false);
    }

    public static boolean isChatHidePhone(long dialogId, boolean isSelf) {
        if (streamerMode && streamerHidePhoneNumbers) return true;
        int val = getChatOverride(dialogId, "hide_phone");
        if (val == OVERRIDE_ENABLED) return true;
        if (val == OVERRIDE_DISABLED) return false;
        return isSelf ? hidePhoneNumber : hideOtherPhoneNumbers;
    }

    public static boolean isHideUsername(long dialogId, boolean isSelf) {
        if (streamerMode && streamerHideUsernames) return true;
        return isSelf ? hideUsername : hideOtherUsernames;
    }

    public static boolean isStreamerMode() {
        return streamerMode;
    }

    public static boolean toggleStreamerMode() {
        streamerMode = !streamerMode;
        save("streamerMode", streamerMode);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        postNotificationToAllAccounts(NotificationCenter.dialogsNeedReload);
        if (ApplicationLoader.applicationContext != null) {
            AndroidUtilities.runOnUIThread(() -> {
                if (org.telegram.ui.LaunchActivity.instance != null) {
                    org.telegram.ui.LaunchActivity.instance.updateStreamerSecureFlag();
                }
            });
        }
        return streamerMode;
    }

    public static void toggleStreamerBlockScreenshots() {
        streamerBlockScreenshots = !streamerBlockScreenshots;
        save("streamerBlockScreenshots", streamerBlockScreenshots);
        if (ApplicationLoader.applicationContext != null) {
            AndroidUtilities.runOnUIThread(() -> {
                if (org.telegram.ui.LaunchActivity.instance != null) {
                    org.telegram.ui.LaunchActivity.instance.updateStreamerSecureFlag();
                }
            });
        }
    }

    public static void setStreamerFakeName(String name) {
        streamerFakeName = !TextUtils.isEmpty(name) ? name : "Streamer";
        saveString("streamerFakeName", streamerFakeName);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
    }

    public static void toggleStreamerHidePhoneNumbers() {
        streamerHidePhoneNumbers = !streamerHidePhoneNumbers;
        save("streamerHidePhoneNumbers", streamerHidePhoneNumbers);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleStreamerHideUsernames() {
        streamerHideUsernames = !streamerHideUsernames;
        save("streamerHideUsernames", streamerHideUsernames);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleStreamerHideAvatars() {
        streamerHideAvatars = !streamerHideAvatars;
        save("streamerHideAvatars", streamerHideAvatars);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_AVATAR);
        postNotificationToAllAccounts(NotificationCenter.dialogsNeedReload);
    }

    public static void toggleStreamerHideDialogMessages() {
        streamerHideDialogMessages = !streamerHideDialogMessages;
        save("streamerHideDialogMessages", streamerHideDialogMessages);
        postNotificationToAllAccounts(NotificationCenter.dialogsNeedReload);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleStreamerHideIdAndDc() {
        streamerHideIdAndDc = !streamerHideIdAndDc;
        save("streamerHideIdAndDc", streamerHideIdAndDc);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleStreamerHideNotificationPreview() {
        streamerHideNotificationPreview = !streamerHideNotificationPreview;
        save("streamerHideNotificationPreview", streamerHideNotificationPreview);
    }

    public static void toggleStreamerHideOtherAccounts() {
        streamerHideOtherAccounts = !streamerHideOtherAccounts;
        save("streamerHideOtherAccounts", streamerHideOtherAccounts);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleStreamerPreventAccidentalCalls() {
        streamerPreventAccidentalCalls = !streamerPreventAccidentalCalls;
        save("streamerPreventAccidentalCalls", streamerPreventAccidentalCalls);
    }

    public static void toggleStreamerPreventAccidentalDelete() {
        streamerPreventAccidentalDelete = !streamerPreventAccidentalDelete;
        save("streamerPreventAccidentalDelete", streamerPreventAccidentalDelete);
    }

    public static void togglePrivacyBlurDialogMessages() {
        privacyBlurDialogMessages = !privacyBlurDialogMessages;
        save("privacyBlurDialogMessages", privacyBlurDialogMessages);
        postNotificationToAllAccounts(NotificationCenter.dialogsNeedReload);
    }

    public static void togglePrivacyBlurAvatars() {
        privacyBlurAvatars = !privacyBlurAvatars;
        save("privacyBlurAvatars", privacyBlurAvatars);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_AVATAR);
        postNotificationToAllAccounts(NotificationCenter.dialogsNeedReload);
    }

    public static void toggleHideSimilarChannels() {
        hideSimilarChannels = !hideSimilarChannels;
        save("hideSimilarChannels", hideSimilarChannels);
    }

    public static void toggleHideStoriesBar() {
        hideStoriesBar = !hideStoriesBar;
        save("hideStoriesBar", hideStoriesBar);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        postNotificationToAllAccounts(NotificationCenter.dialogsNeedReload);
    }

    public static void toggleShowSecondsInTime() {
        showSecondsInTime = !showSecondsInTime;
        save("showSecondsInTime", showSecondsInTime);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleAvatarHistoryEnabled() {
        avatarHistoryEnabled = !avatarHistoryEnabled;
        save("avatarHistoryEnabled", avatarHistoryEnabled);
    }

    public static void toggleDeveloperMode() {
        developerModeEnabled = !developerModeEnabled;
        save("developerModeEnabled", developerModeEnabled);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void toggleCopyCodeButton() {
        copyCodeButtonEnabled = !copyCodeButtonEnabled;
        save("copyCodeButtonEnabled", copyCodeButtonEnabled);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static void togglePrettyJsonViewer() {
        prettyJsonViewerEnabled = !prettyJsonViewerEnabled;
        save("prettyJsonViewerEnabled", prettyJsonViewerEnabled);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static boolean isStreamerHideAvatars() {
        if (streamerMode) {
            return streamerHideAvatars;
        }
        return privacyBlurAvatars;
    }

    public static boolean isStreamerHideDialogMessages() {
        if (streamerMode) {
            return streamerHideDialogMessages;
        }
        return privacyBlurDialogMessages;
    }

    public static boolean isStreamerHideIdAndDc() {
        if (streamerMode && streamerHideIdAndDc) return true;
        return showIdAndDc == 0;
    }

    public static boolean isStreamerHideNotificationPreview() {
        return streamerMode && streamerHideNotificationPreview;
    }

    public static boolean isStreamerHideOtherAccounts() {
        return streamerMode && streamerHideOtherAccounts;
    }

    public static boolean isStreamerPreventAccidentalCalls() {
        return streamerMode && streamerPreventAccidentalCalls;
    }

    public static boolean isStreamerPreventAccidentalDelete() {
        return streamerMode && streamerPreventAccidentalDelete;
    }

    public static boolean isChatAntiDelete(long dialogId) {
        int val = getChatOverride(dialogId, "anti_delete");
        if (val == OVERRIDE_DISABLED) return false;
        return true;
    }

    public static boolean isChatSilentSend(long dialogId) {
        int val = getChatOverride(dialogId, "silent_send");
        if (val == OVERRIDE_ENABLED) return true;
        if (val == OVERRIDE_DISABLED) return false;
        return silentSendByDefault;
    }

    public static boolean isChatAutoTranslate(long dialogId) {
        int val = getChatOverride(dialogId, "auto_translate");
        if (val == OVERRIDE_ENABLED) return true;
        if (val == OVERRIDE_DISABLED) return false;
        return unlockChatTranslation;
    }

    public static boolean isChatAutoArchive(long dialogId) {
        int val = getChatOverride(dialogId, "auto_archive");
        if (val == OVERRIDE_ENABLED) return true;
        if (val == OVERRIDE_DISABLED) return false;
        return backgroundArchiverEnabled;
    }

    public static int getChatPhotoResolution(long dialogId) {
        if (ApplicationLoader.applicationContext == null) return 0;
        return ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_chat_overrides", Activity.MODE_PRIVATE)
                .getInt("photo_res_" + dialogId, 0);
    }

    public static void setChatPhotoResolution(long dialogId, int resolution) {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_chat_overrides", Activity.MODE_PRIVATE).edit();
        if (resolution == 0) {
            editor.remove("photo_res_" + dialogId);
        } else {
            editor.putInt("photo_res_" + dialogId, resolution);
        }
        editor.apply();
    }

    public static int getEffectivePhotoResolution(long dialogId) {
        int res = getChatPhotoResolution(dialogId);
        if (res > 0) return res;
        return photoMaxResolution;
    }

    public static boolean isChatSendVideoAsDoc(long dialogId) {
        int val = getChatOverride(dialogId, "video_as_doc");
        if (val == OVERRIDE_ENABLED) return true;
        if (val == OVERRIDE_DISABLED) return false;
        return sendVideoAsDocument;
    }

    public static int getChatCacheLimitMb(long dialogId) {
        if (ApplicationLoader.applicationContext == null) return 0;
        return ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_chat_overrides", Activity.MODE_PRIVATE)
                .getInt("cache_limit_" + dialogId, 0);
    }

    public static void setChatCacheLimitMb(long dialogId, int limitMb) {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_chat_overrides", Activity.MODE_PRIVATE).edit();
        if (limitMb == 0) {
            editor.remove("cache_limit_" + dialogId);
        } else {
            editor.putInt("cache_limit_" + dialogId, limitMb);
        }
        editor.apply();
    }

    public static boolean hasChatOverrides(long dialogId) {
        if (hasChatVaultException(dialogId)) return true;
        if (ApplicationLoader.applicationContext == null) return false;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_chat_overrides", Activity.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        String suffix = "_" + dialogId;
        for (String key : all.keySet()) {
            if (key.endsWith(suffix)) return true;
        }
        return false;
    }

    public static void clearAllChatOverrides(long dialogId) {
        clearChatVaultSettings(dialogId);
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_chat_overrides", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String suffix = "_" + dialogId;
        for (String key : prefs.getAll().keySet()) {
            if (key.endsWith(suffix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public static ArrayList<Long> getChatVaultExceptions() {
        ArrayList<Long> result = new ArrayList<>();
        if (ApplicationLoader.applicationContext == null) return result;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_vault_chats", Activity.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        HashSet<Long> dialogIds = new HashSet<>();
        for (String key : all.keySet()) {
            try {
                if (key.startsWith("mode_")) {
                    dialogIds.add(Long.parseLong(key.substring(5)));
                } else if (key.startsWith("photos_")) {
                    dialogIds.add(Long.parseLong(key.substring(7)));
                } else if (key.startsWith("videos_")) {
                    dialogIds.add(Long.parseLong(key.substring(7)));
                } else if (key.startsWith("voices_")) {
                    dialogIds.add(Long.parseLong(key.substring(7)));
                } else if (key.startsWith("docs_")) {
                    dialogIds.add(Long.parseLong(key.substring(5)));
                } else if (key.startsWith("max_size_")) {
                    dialogIds.add(Long.parseLong(key.substring(9)));
                }
            } catch (Exception ignored) {}
        }
        for (Long did : dialogIds) {
            if (hasChatVaultException(did)) {
                result.add(did);
            }
        }
        return result;
    }

    public static boolean canVaultDownload(long dialogId) {
        int mode = getChatVaultMode(dialogId);
        if (mode == VAULT_MODE_NEVER) return false;
        if (mode == VAULT_MODE_ALWAYS) return true;
        if (!autoDownloadMediaVault && !backgroundArchiverEnabled) return false;
        if (autoDownloadOnlyWifi && ApplicationLoader.getAutodownloadNetworkType() != StatsController.TYPE_WIFI) {
            return false;
        }
        if (dialogId > 0) return true;
        if (dialogId < 0) {
            TLRPC.Dialog d = null;
            try {
                d = MessagesController.getInstance(UserConfig.selectedAccount).dialogs_dict.get(dialogId);
            } catch (Throwable ignored) {}
            if (d != null && d.folder_id == 1 && vaultIncludeArchivedChats) {
                return true;
            }
            if (autoDownloadChatsScope == 2) {
                return true;
            }
            if (autoDownloadChatsScope == 1) {
                TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(-dialogId);
                return chat == null || !ChatObject.isChannelAndNotMegaGroup(chat);
            }
            return false;
        }
        return false;
    }

    public static boolean canVaultDownload(long dialogId, TLRPC.Message message) {
        return canVaultDownload(dialogId, message, -1);
    }

    public static boolean canVaultDownload(long dialogId, TLRPC.Message message, int folderId) {
        if (message == null || message.media == null) {
            return false;
        }
        if (autoDownloadOnlyWifi && ApplicationLoader.getAutodownloadNetworkType() != StatsController.TYPE_WIFI) {
            return false;
        }

        int mode = getChatVaultMode(dialogId);
        if (mode == VAULT_MODE_NEVER) {
            return false;
        }

        if (mode == VAULT_MODE_DEFAULT) {
            if (!autoDownloadMediaVault && !backgroundArchiverEnabled) {
                return false;
            }
            if (!vaultIncludeArchivedChats) {
                if (folderId == -1) {
                    try {
                        TLRPC.Dialog d = MessagesController.getInstance(UserConfig.selectedAccount).dialogs_dict.get(dialogId);
                        if (d != null) {
                            folderId = d.folder_id;
                        }
                    } catch (Throwable ignored) {}
                }
                if (folderId == 1) {
                    return false;
                }
            }
            int scope = autoDownloadChatsScope;
            boolean inScope;
            if (scope == 0) {
                inScope = dialogId > 0 || (message.peer_id != null && message.peer_id.user_id != 0);
            } else if (scope == 1) {
                inScope = dialogId > 0 || (message.peer_id != null && (message.peer_id.user_id != 0 || message.peer_id.chat_id != 0 || (message.peer_id.channel_id != 0 && !message.post)));
            } else {
                inScope = true;
            }
            if (!inScope) {
                return false;
            }
        }

        // Check media type filter
        if (MessageObject.isPhoto(message)) {
            if (!isChatVaultTypeAllowed(dialogId, VAULT_TYPE_PHOTO)) {
                return false;
            }
        } else if (MessageObject.isVideoMessage(message)) {
            if (!isChatVaultTypeAllowed(dialogId, VAULT_TYPE_VIDEO)) {
                return false;
            }
        } else if (MessageObject.isVoiceMessage(message) || MessageObject.isRoundVideoMessage(message)) {
            if (!isChatVaultTypeAllowed(dialogId, VAULT_TYPE_VOICE_ROUND)) {
                return false;
            }
        } else if (message.media instanceof TLRPC.TL_messageMediaDocument) {
            if (!isChatVaultTypeAllowed(dialogId, VAULT_TYPE_DOCUMENT)) {
                return false;
            }
        }

        // Check max size
        int maxMb = getChatVaultMaxSizeMb(dialogId);
        if (maxMb == 0) {
            maxMb = autoDownloadMaxSizeMb;
        }
        if (maxMb > 0) {
            long maxBytes = (long) maxMb * 1024L * 1024L;
            long mediaSize = MessageObject.getMediaSize(message.media);
            if (mediaSize > 0 && mediaSize > maxBytes) {
                return false;
            }
        }

        return true;
    }

    public static void toggleAlwaysHighQualityPhotos() {
        alwaysHighQualityPhotos = !alwaysHighQualityPhotos;
        save("alwaysHighQualityPhotos", alwaysHighQualityPhotos);
    }

    public static void setPhotoMaxResolution(int resolution) {
        photoMaxResolution = resolution;
        saveInt("photoMaxResolution", photoMaxResolution);
    }

    public static void toggleSendVideoAsDocument() {
        sendVideoAsDocument = !sendVideoAsDocument;
        save("sendVideoAsDocument", sendVideoAsDocument);
    }

    public static void toggleBoostDownloadSpeed() {
        boostDownloadSpeed = !boostDownloadSpeed;
        save("boostDownloadSpeed", boostDownloadSpeed);
    }

    public static void toggleConfirmCalls() {
        confirmCalls = !confirmCalls;
        save("confirmCalls", confirmCalls);
    }

    public static void toggleCleanUrls() {
        cleanUrls = !cleanUrls;
        save("cleanUrls", cleanUrls);
    }

    // Ghost Mode
    public static boolean isGhostModeActive() {
        return ghostReadMessages && ghostTyping && ghostOnline;
    }

    public static boolean toggleGhostModeMaster() {
        boolean newState = !isGhostModeActive();
        ghostReadMessages = newState;
        ghostTyping = newState;
        ghostOnline = newState;
        save("ghostReadMessages", ghostReadMessages);
        save("ghostTyping", ghostTyping);
        save("ghostOnline", ghostOnline);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        return newState;
    }

    public static void toggleGhostReadMessages() {
        ghostReadMessages = !ghostReadMessages;
        save("ghostReadMessages", ghostReadMessages);
    }

    public static void toggleGhostReadOnReply() {
        ghostReadOnReply = !ghostReadOnReply;
        save("ghostReadOnReply", ghostReadOnReply);
    }

    public static void toggleGhostReadMediaContents() {
        ghostReadMediaContents = !ghostReadMediaContents;
        save("ghostReadMediaContents", ghostReadMediaContents);
    }

    public static void toggleGhostShowMarkAsReadOption() {
        ghostShowMarkAsReadOption = !ghostShowMarkAsReadOption;
        save("ghostShowMarkAsReadOption", ghostShowMarkAsReadOption);
    }

    public static void toggleGhostTyping() {
        ghostTyping = !ghostTyping;
        save("ghostTyping", ghostTyping);
    }

    public static void setGhostStoriesMode(int mode) {
        ghostStoriesMode = mode;
        saveInt("ghostStoriesMode", ghostStoriesMode);
    }

    public static void toggleUnlockStorySaving() {
        unlockStorySaving = !unlockStorySaving;
        save("unlockStorySaving", unlockStorySaving);
    }

    public static void toggleAutoCheckDeleteForAll() {
        autoCheckDeleteForAll = !autoCheckDeleteForAll;
        save("autoCheckDeleteForAll", autoCheckDeleteForAll);
    }

    public static void toggleGhostOnline() {
        ghostOnline = !ghostOnline;
        save("ghostOnline", ghostOnline);
    }

    public static void toggleGhostHideReadDate() {
        ghostHideReadDate = !ghostHideReadDate;
        save("ghostHideReadDate", ghostHideReadDate);
    }

    public static void toggleSilentSendByDefault() {
        silentSendByDefault = !silentSendByDefault;
        save("silentSendByDefault", silentSendByDefault);
    }

    public static void toggleConfirmSendStickers() {
        confirmSendStickers = !confirmSendStickers;
        save("confirmSendStickers", confirmSendStickers);
    }

    public static void toggleConfirmSendGifs() {
        confirmSendGifs = !confirmSendGifs;
        save("confirmSendGifs", confirmSendGifs);
    }

    public static void toggleAlwaysForwardWithoutAuthor() {
        alwaysForwardWithoutAuthor = !alwaysForwardWithoutAuthor;
        save("alwaysForwardWithoutAuthor", alwaysForwardWithoutAuthor);
    }

    public static boolean isPersistentBackgroundEnabled() {
        return keepAliveForegroundService;
    }

    public static void toggleKeepAliveForegroundService() {
        keepAliveForegroundService = !keepAliveForegroundService;
        save("keepAliveForegroundService", keepAliveForegroundService);
        ApplicationLoader.startPushService();
    }

    public static void toggleUnlockChatTranslation() {
        unlockChatTranslation = !unlockChatTranslation;
        save("unlockChatTranslation", unlockChatTranslation);
        postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }

    public static boolean isGhostStoryViewActive() {
        if (ghostStoriesMode == GHOST_STORIES_ALWAYS) {
            return true;
        }
        if (ghostStoriesMode == GHOST_STORIES_PROMPT) {
            return currentStoryGhostSession;
        }
        return false;
    }

    public static float getStickerScale() {
        switch (stickerSize) {
            case MEDIA_SIZE_MINIMAL:
                return 0.50f;
            case MEDIA_SIZE_COMPACT:
                return 0.75f;
            case MEDIA_SIZE_LARGE:
                return 1.25f;
            case MEDIA_SIZE_HUGE:
                return 1.50f;
            case MEDIA_SIZE_DEFAULT:
            default:
                return 1.0f;
        }
    }

    public static float getGifScale() {
        switch (gifSize) {
            case MEDIA_SIZE_MINIMAL:
                return 0.50f;
            case MEDIA_SIZE_COMPACT:
                return 0.75f;
            case MEDIA_SIZE_LARGE:
                return 1.25f;
            case MEDIA_SIZE_HUGE:
                return 1.50f;
            case MEDIA_SIZE_DEFAULT:
            default:
                return 1.0f;
        }
    }

    public static void setStickerSize(int size) {
        stickerSize = size;
        saveInt("stickerSize", stickerSize);
    }

    public static void setGifSize(int size) {
        gifSize = size;
        saveInt("gifSize", gifSize);
    }
}
