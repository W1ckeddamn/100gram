package org.telegram.messenger.chats;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.Theme;

public abstract class StickerTime {

    public static final class Row {
        public float anchorY;
        public float cellWidth;
        public boolean hasName;
        public MessageObject message;
        public float nameHeight;
        public float nameLeft;
        public float nameTop;
        public float nameWidth;
        public float overStickerOffsetX;
        public boolean sideButton;
        public float stickerLeft;
        public float stickerRight;
        public float stickerWidth;
        public float timeWidth;
    }

    public static boolean isHidden(MessageObject messageObject) {
        return HundredGramConfig.stickerTimeMode == HundredGramConfig.STICKER_TIME_HIDDEN && messageObject != null && messageObject.isAnyKindOfSticker();
    }

    public static boolean shouldPreserveOnPreview(MessageObject messageObject, MessageObject messageObject2) {
        return messageObject != null && messageObject2 != null && messageObject.getId() == messageObject2.getId() && messageObject2.preview && messageObject2.isAnyKindOfSticker() && HundredGramConfig.stickerTimeMode != HundredGramConfig.STICKER_TIME_HIDDEN;
    }

    public static float getTimeOffsetX(Row row, float f) {
        MessageObject messageObject = row.message;
        if (messageObject == null || !messageObject.isAnyKindOfSticker()) {
            return 0.0f;
        }
        if (!isBeside(row)) {
            return row.overStickerOffsetX;
        }
        return (getTimeLeft(row) + AndroidUtilities.dp(6.0f)) - f;
    }

    public static float getSideButtonX(Row row, float f) {
        if (!isBeside(row)) {
            return f;
        }
        float dp = AndroidUtilities.dp(8.0f) + AndroidUtilities.dp(32.0f);
        if (row.message != null && row.message.isOutOwner()) {
            return Math.min(f, getTimeLeft(row) - dp);
        }
        return Math.max(f, getTimeLeft(row) + getTimeWidth(row) + AndroidUtilities.dp(8.0f));
    }

    public static float getSideButtonTouchLeft(Row row, float f) {
        return (!isBeside(row) || (row.message != null && row.message.isOutOwner())) ? f : AndroidUtilities.dp(8.0f);
    }

    public static float getSideButtonTouchRight(Row row, float f) {
        return (isBeside(row) && row.message != null && row.message.isOutOwner()) ? AndroidUtilities.dp(8.0f) + AndroidUtilities.dp(32.0f) : f;
    }

    public static boolean isBeside(Row row) {
        MessageObject messageObject = row.message;
        if (messageObject != null && messageObject.isAnyKindOfSticker() && HundredGramConfig.stickerTimeMode == HundredGramConfig.STICKER_TIME_SIDE && messageObject.type != 19 && row.stickerWidth > 0.0f) {
            float dp = AndroidUtilities.dp(8.0f);
            float timeLeft = getTimeLeft(row);
            float timeWidth = getTimeWidth(row) + timeLeft;
            if (row.sideButton) {
                if (messageObject.isOutOwner()) {
                    timeLeft -= AndroidUtilities.dp(32.0f) + dp;
                } else {
                    timeWidth += AndroidUtilities.dp(32.0f) + dp;
                }
            }
            if (timeLeft >= dp && timeWidth <= row.cellWidth - dp) {
                return true;
            }
        }
        return false;
    }

    private static float getTimeWidth(Row row) {
        return row.timeWidth + AndroidUtilities.dp(12.0f) + (row.message.isOutOwner() ? AndroidUtilities.dp(20.0f) : 0);
    }

    private static float getTimeLeft(Row row) {
        float padding = AndroidUtilities.dp(8.0f);
        boolean nameOverlaps = row.hasName && overlapsRow(row, row.nameTop, row.nameHeight + row.nameTop);
        if (row.message.isOutOwner()) {
            float left = row.stickerLeft - padding;
            if (nameOverlaps) {
                left = Math.min(left, row.nameLeft - padding);
            }
            return left - getTimeWidth(row);
        } else {
            float left = row.stickerRight + padding;
            if (nameOverlaps) {
                left = Math.max(left, row.nameLeft + row.nameWidth + padding);
            }
            return left;
        }
    }

    private static boolean overlapsRow(Row row, float f, float f2) {
        float dp = row.anchorY - AndroidUtilities.dp(23.0f);
        return f < Math.max((float) AndroidUtilities.dp(17.0f), Theme.chat_timePaint.getTextSize() + ((float) AndroidUtilities.dp(5.0f))) + dp && f2 > dp;
    }
}
