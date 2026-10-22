package org.telegram.messenger;

import android.content.Context;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.HundredGramVaultManager;
import org.telegram.tgnet.TLRPC;

/**
 * HundredGramHooks - Централизованный фасад хуков 100gram для интеграции в оригинальный код Telegram.
 * 
 * ВАЖНО: Вместо того чтобы писать сложную логику прямо в ChatActivity или MessagesController,
 * вставляйте в оригинальный код Telegram ТОЛЬКО однострочные вызовы методов из этого класса.
 * 
 * Это позволяет безболезненно обновлять базу Telegram до любой новой версии (rebase/merge).
 */
public final class HundredGramHooks {

    private HundredGramHooks() {}

    /* =========================================================================
     * МЕССЕНДЖЕР И КОНФИГУРАЦИЯ (ApplicationLoader / AndroidUtilities)
     * ========================================================================= */

    public static void onAppInit(Context context) {
        HundredGramConfig.init(context);
    }

    public static boolean isGhostMode() {
        return HundredGramConfig.isGhostMode();
    }

    public static boolean isReadWithoutAck() {
        return HundredGramConfig.isGhostReadWithoutAck();
    }

    /* =========================================================================
     * СООБЩЕНИЯ И ХРАНИЛИЩЕ (MessagesController / MessagesStorage)
     * ========================================================================= */

    public static boolean shouldInterceptIncomingMessage(int currentAccount, TLRPC.Message message) {
        if (message == null) return false;
        if (HundredGramConfig.isVaultEnabled()) {
            return HundredGramVaultManager.getInstance(currentAccount).shouldInterceptMessage(message);
        }
        return false;
    }

    public static boolean shouldPreventMessageDelete(int currentAccount, long dialogId, int messageId) {
        return HundredGramConfig.isAntiDeleteMessagesEnabled();
    }

    public static boolean shouldShowEditedHistory(TLRPC.Message message) {
        return HundredGramConfig.isShowEditedMessageHistory();
    }

    /* =========================================================================
     * ЧАТЫ И UI (ChatActivity / DialogCell / ChatMessageCell)
     * ========================================================================= */

    public static boolean isSecretChatScreenshotsAllowed() {
        return HundredGramConfig.isAllowScreenshotsInSecretChats();
    }

    public static boolean shouldHideKeyboardOnScroll() {
        return HundredGramConfig.isHideKeyboardOnScroll();
    }

    public static boolean isFastShareButtonVisible() {
        return HundredGramConfig.isShowDirectShareButton();
    }

    public static int getCustomMessageBubbleRadius(int defaultRadius) {
        int custom = HundredGramConfig.getMessageBubbleCornerRadius();
        return custom > 0 ? custom : defaultRadius;
    }

    public static int getCustomAvatarCornerRadius(int defaultRadius) {
        int custom = HundredGramConfig.getAvatarCornerRadius();
        return custom > 0 ? custom : defaultRadius;
    }
}
