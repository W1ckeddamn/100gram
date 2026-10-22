/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.text.TextPaint;
import android.view.View;

import org.telegram.ui.ActionBar.Theme;

import android.os.Bundle;
import android.text.TextUtils;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ProfileActivity;

public class URLSpanUserMention extends URLSpanNoUnderline {

    private int currentType;
    private TextStyleSpan.TextStyleRun style;

    public URLSpanUserMention(String url, int type) {
        this(url, type, null);
    }

    public URLSpanUserMention(String url, int type, TextStyleSpan.TextStyleRun run) {
        super(url);
        currentType = type;
        style = run;
    }

    @Override
    public void onClick(View widget) {
        try {
            String url = getURL();
            if (TextUtils.isEmpty(url)) {
                return;
            }
            if (url.startsWith("@")) {
                super.onClick(widget);
                return;
            }
            long userId = Utilities.parseLong(url);
            if (userId != 0) {
                int currentAccount = UserConfig.selectedAccount;
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
                BaseFragment fragment = null;
                if (widget != null && widget.getContext() instanceof LaunchActivity) {
                    fragment = ((LaunchActivity) widget.getContext()).getActionBarLayout().getLastFragment();
                }
                if (fragment != null) {
                    if (user != null) {
                        MessagesController.getInstance(currentAccount).openChatOrProfileWith(user, null, fragment, 0, false);
                    } else {
                        Bundle args = new Bundle();
                        args.putLong("user_id", userId);
                        fragment.presentFragment(new ProfileActivity(args));
                    }
                    return;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @Override
    public void updateDrawState(TextPaint p) {
        super.updateDrawState(p);
        if (currentType == 3) {
            p.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        } else if (currentType == 2) {
            p.setColor(0xffffffff);
        } else if (currentType == 1) {
            p.setColor(Theme.getColor(Theme.key_chat_messageLinkOut));
        } else {
            p.setColor(Theme.getColor(Theme.key_chat_messageLinkIn));
        }
        if (style != null) {
            style.applyStyle(p);
        } else {
            p.setUnderlineText(false);
        }
    }
}
