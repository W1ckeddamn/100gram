package org.telegram.messenger.chats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

public enum SwipeAction {
    REACTION(1, R.drawable.msg_reactions2, R.string.DoubleTapSetting, 1.0f),
    REPLY(2, R.drawable.menu_reply, R.string.Reply, 1.09f),
    COPY(3, R.drawable.msg_copy, R.string.Copy, 1.0f),
    FORWARD(4, R.drawable.msg_forward, R.string.Forward, 1.09f),
    EDIT(5, R.drawable.msg_edit, R.string.Edit, 1.0f),
    SAVE(6, R.drawable.msg_saved, R.string.Save, 1.0f),
    REPEAT(7, R.drawable.msg_repeat, R.string.RepeatMessage, 1.09f),
    DELETE(8, R.drawable.msg_delete, R.string.Delete, 1.09f),
    TRANSLATE(9, R.drawable.msg_translate, R.string.TranslateMessage, 1.09f);

    public final int actionId;
    public final int iconRes;
    public final float iconTrim;
    public final int titleRes;

    SwipeAction(int actionId, int iconRes, int titleRes, float iconTrim) {
        this.actionId = actionId;
        this.iconRes = iconRes;
        this.titleRes = titleRes;
        this.iconTrim = iconTrim;
    }

    public static String quickReactionEmoticon(int account) {
        MediaDataController mediaDataController = MediaDataController.getInstance(account);
        String doubleTapReaction = mediaDataController.getDoubleTapReaction();
        if (doubleTapReaction == null) {
            return null;
        }
        if (doubleTapReaction.startsWith("animated_")) {
            return doubleTapReaction;
        }
        TLRPC.TL_availableReaction tL_availableReaction = mediaDataController.getReactionsMap().get(doubleTapReaction);
        if (tL_availableReaction != null) {
            return tL_availableReaction.reaction;
        }
        return null;
    }

    public static SwipeAction of(int actionId) {
        for (SwipeAction swipeAction : values()) {
            if (swipeAction.actionId == actionId) {
                return swipeAction;
            }
        }
        return null;
    }

    public static List<SwipeAction> enabled() {
        ArrayList<SwipeAction> arrayList = new ArrayList<>();
        String pref = HundredGramConfig.getSwipeActions();
        if (pref != null) {
            for (String str : pref.split(",")) {
                try {
                    SwipeAction of = of(Integer.parseInt(str.trim()));
                    if (of != null && !arrayList.contains(of)) {
                        arrayList.add(of);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return arrayList;
    }

    public static List<SwipeAction> disabled() {
        ArrayList<SwipeAction> arrayList = new ArrayList<>(Arrays.asList(values()));
        arrayList.removeAll(enabled());
        return arrayList;
    }

    public static void setEnabled(List<SwipeAction> list) {
        StringBuilder sb = new StringBuilder();
        for (SwipeAction swipeAction : list) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(swipeAction.actionId);
        }
        HundredGramConfig.setSwipeActions(sb.toString());
    }

    public boolean isEnabled() {
        return enabled().contains(this);
    }

    public void setEnabled(boolean enabled) {
        List<SwipeAction> current = enabled();
        if (enabled == current.contains(this)) {
            return;
        }
        if (enabled) {
            int i = 0;
            for (SwipeAction swipeAction : values()) {
                if (swipeAction == this) {
                    break;
                }
                if (current.contains(swipeAction)) {
                    i++;
                }
            }
            current.add(i, this);
        } else {
            current.remove(this);
        }
        setEnabled(current);
    }
}
