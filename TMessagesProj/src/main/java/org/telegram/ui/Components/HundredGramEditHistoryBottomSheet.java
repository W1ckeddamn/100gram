package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class HundredGramEditHistoryBottomSheet extends BottomSheetWithRecyclerListView {

    private final BaseFragment fragment;
    private final MessageObject messageObject;
    private UniversalAdapter adapter;
    private final ArrayList<RevisionItem> revisions = new ArrayList<>();
    private boolean loaded;

    public static class RevisionItem {
        public final TLRPC.Message message;
        public final int versionDate;
        public final boolean isCurrent;
        public final boolean isOriginal;
        public final boolean isDeleted;
        public final int revisionNumber;

        public RevisionItem(TLRPC.Message message, int versionDate, boolean isCurrent, boolean isOriginal, boolean isDeleted, int revisionNumber) {
            this.message = message;
            this.versionDate = versionDate;
            this.isCurrent = isCurrent;
            this.isOriginal = isOriginal;
            this.isDeleted = isDeleted;
            this.revisionNumber = revisionNumber;
        }
    }

    public HundredGramEditHistoryBottomSheet(BaseFragment fragment, MessageObject messageObject) {
        super(fragment.getContext(), fragment, false, false, false, true, ActionBarType.SLIDING, fragment.getResourceProvider());
        this.fragment = fragment;
        this.messageObject = messageObject;
        topPadding = 0.30f;
        fixNavigationBar();
        setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));
        setSlidingActionBar();
        setShowHandle(true);

        loadData();
    }

    private void loadData() {
        if (messageObject == null || messageObject.messageOwner == null) {
            loaded = true;
            if (adapter != null) adapter.update(true);
            return;
        }

        long dialogId = messageObject.getDialogId();
        int mid = messageObject.getId();

        MessagesStorage.getInstance(currentAccount).loadMessageEdits(dialogId, mid, edits -> {
            revisions.clear();

            ArrayList<TLRPC.Message> allVersions = new ArrayList<>();
            if (edits != null) {
                allVersions.addAll(edits);
            }

            TLRPC.Message currentOwner = messageObject.messageOwner;
            boolean isDeleted = currentOwner.isDeleted;

            // Check if currentOwner is already represented in edits
            boolean currentFound = false;
            for (TLRPC.Message m : allVersions) {
                int date = m.edit_date != 0 ? m.edit_date : m.date;
                int currentDate = currentOwner.edit_date != 0 ? currentOwner.edit_date : currentOwner.date;
                if (date == currentDate && TextUtils.equals(m.message, currentOwner.message)) {
                    currentFound = true;
                    break;
                }
            }
            if (!currentFound && (currentOwner.edit_date > 0 || isDeleted || allVersions.isEmpty())) {
                allVersions.add(currentOwner);
            }

            // Remove adjacent duplicates
            ArrayList<TLRPC.Message> distinct = new ArrayList<>();
            for (int i = 0; i < allVersions.size(); i++) {
                TLRPC.Message msg = allVersions.get(i);
                if (!distinct.isEmpty()) {
                    TLRPC.Message prev = distinct.get(distinct.size() - 1);
                    int prevDate = prev.edit_date != 0 ? prev.edit_date : prev.date;
                    int curDate = msg.edit_date != 0 ? msg.edit_date : msg.date;
                    boolean sameText = TextUtils.equals(prev.message, msg.message);
                    boolean sameMedia = (prev.media == null && msg.media == null) || (prev.media != null && msg.media != null && prev.media.getClass() == msg.media.getClass());
                    if ((prevDate == curDate && sameText) || (sameText && sameMedia)) {
                        continue;
                    }
                }
                distinct.add(msg);
            }

            int count = distinct.size();
            for (int i = count - 1; i >= 0; i--) {
                TLRPC.Message msg = distinct.get(i);
                int date = msg.edit_date != 0 ? msg.edit_date : msg.date;
                boolean isCur = (i == count - 1);
                boolean isOrig = (i == 0);
                boolean del = isCur && isDeleted;
                revisions.add(new RevisionItem(msg, date, isCur, isOrig, del, i));
            }

            loaded = true;
            if (adapter != null) {
                adapter.update(true);
            }
        });
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString("MessageEditHistory", R.string.MessageEditHistory);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }

        boolean isDeleted = messageObject.messageOwner.isDeleted;

        // Header
        items.add(UItem.asHeader(LocaleController.getString("MessageEditHistory", R.string.MessageEditHistory)));

        // Deleted Banner
        if (isDeleted) {
            items.add(UItem.asCustom(createDeletedBannerView(getContext())));
        }

        if (!loaded) {
            items.add(UItem.asShadow(LocaleController.getString("Loading", R.string.Loading)));
            return;
        }

        if (revisions.isEmpty()) {
            if (messageObject.messageOwner.edit_date > 0) {
                items.add(UItem.asShadow(LocaleController.getString("MessageEditHistoryHint", R.string.MessageEditHistoryHint)));
            } else {
                items.add(UItem.asShadow(LocaleController.getString("MessageNotEdited", R.string.MessageNotEdited)));
            }
            return;
        }

        for (int i = 0; i < revisions.size(); i++) {
            RevisionItem rev = revisions.get(i);
            items.add(UItem.asCustom(createRevisionCardView(getContext(), rev)));
        }

        items.add(UItem.asShadow(LocaleController.getString("MessageEditHistoryHint", R.string.MessageEditHistoryHint)));
    }

    private View createDeletedBannerView(Context context) {
        FrameLayout container = new FrameLayout(context);
        container.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        LinearLayout banner = new LinearLayout(context);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        bg.setColor(0x20E53935);
        bg.setStroke(AndroidUtilities.dp(1), 0x50E53935);
        banner.setBackground(bg);

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.msg_delete);
        icon.setColorFilter(0xFFE53935);
        banner.addView(icon, LayoutHelper.createLinear(22, 22, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText(LocaleController.getString("MessageIsDeleted", R.string.MessageIsDeleted));
        title.setTextColor(0xFFE53935);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        title.setTypeface(AndroidUtilities.bold());
        textLayout.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(LocaleController.getString("DeletedMessage", R.string.DeletedMessage));
        subtitle.setTextColor(0xCCE53935);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        textLayout.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        banner.addView(textLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(banner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return container;
    }

    private View createRevisionCardView(Context context, RevisionItem rev) {
        FrameLayout root = new FrameLayout(context);
        root.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(5), AndroidUtilities.dp(14), AndroidUtilities.dp(5));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(AndroidUtilities.dp(12));
        cardBg.setColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));
        card.setBackground(cardBg);

        // Header Row
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        // Title
        TextView titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));

        if (rev.isCurrent) {
            titleView.setText(LocaleController.getString("MessageCurrentVersion", R.string.MessageCurrentVersion));
        } else if (rev.isOriginal) {
            titleView.setText(LocaleController.getString("MessageOriginalVersion", R.string.MessageOriginalVersion));
        } else {
            titleView.setText(String.format(LocaleController.getString("MessageEditedVersion", R.string.MessageEditedVersion), rev.revisionNumber));
        }
        header.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        // Badge
        TextView badge = new TextView(context);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(AndroidUtilities.dp(6));

        if (rev.isDeleted) {
            badge.setText(LocaleController.getString("MessageDeletedBadge", R.string.MessageDeletedBadge).toUpperCase());
            badge.setTextColor(0xFFE53935);
            badgeBg.setColor(0x25E53935);
        } else if (rev.isCurrent) {
            badge.setText(LocaleController.getString("MessageCurrentVersion", R.string.MessageCurrentVersion).toUpperCase());
            badge.setTextColor(0xFF2196F3);
            badgeBg.setColor(0x252196F3);
        } else if (rev.isOriginal) {
            badge.setText(LocaleController.getString("MessageOriginalVersion", R.string.MessageOriginalVersion).toUpperCase());
            badge.setTextColor(0xFF4CAF50);
            badgeBg.setColor(0x254CAF50);
        } else {
            badge.setText(("#" + rev.revisionNumber).toUpperCase());
            badge.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            badgeBg.setColor(0x18000000);
        }
        badge.setBackground(badgeBg);
        header.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

        // Spacer
        View spacer = new View(context);
        header.addView(spacer, LayoutHelper.createLinear(0, 0, 1.0f));

        // Date
        TextView dateView = new TextView(context);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        dateView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        if (rev.versionDate > 0) {
            dateView.setText(LocaleController.formatDateTime(rev.versionDate, false));
        }
        header.addView(dateView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        // Copy button
        ImageView copyBtn = new ImageView(context);
        copyBtn.setImageResource(R.drawable.msg_copy);
        copyBtn.setColorFilter(Theme.getColor(Theme.key_dialogIcon, resourcesProvider));
        copyBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), 1));
        copyBtn.setScaleType(ImageView.ScaleType.CENTER);
        copyBtn.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

        CharSequence copyText = getMessageText(rev.message);
        copyBtn.setOnClickListener(v -> copyTextToClipboard(copyText));
        header.addView(copyBtn, LayoutHelper.createLinear(28, 28));

        card.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Media Label (if any)
        String mediaLabel = getMediaDescription(rev.message);
        if (!TextUtils.isEmpty(mediaLabel)) {
            TextView mediaView = new TextView(context);
            mediaView.setText(mediaLabel);
            mediaView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            mediaView.setTypeface(AndroidUtilities.bold());
            mediaView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider));
            card.addView(mediaView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
        }

        // Text Content
        TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setTextIsSelectable(true);

        if (!TextUtils.isEmpty(copyText)) {
            textView.setText(copyText);
        } else if (!TextUtils.isEmpty(mediaLabel)) {
            textView.setText("(" + LocaleController.getString("NoCaption", R.string.NoCaption) + ")");
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            textView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        } else {
            textView.setText("(" + LocaleController.getString("EmptyMessage", R.string.EmptyMessage) + ")");
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            textView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        }

        card.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        // Click on entire card copies as well
        card.setOnClickListener(v -> copyTextToClipboard(copyText));

        root.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return root;
    }

    private CharSequence getMessageText(TLRPC.Message message) {
        if (message == null) {
            return "";
        }
        if (message.message != null && message.entities != null && !message.entities.isEmpty()) {
            try {
                MessageObject temp = new MessageObject(currentAccount, message, false, false);
                if (!TextUtils.isEmpty(temp.messageText)) {
                    return temp.messageText;
                }
            } catch (Exception ignore) {}
        }
        return message.message != null ? message.message : "";
    }

    private String getMediaDescription(TLRPC.Message message) {
        if (message == null || message.media == null || message.media instanceof TLRPC.TL_messageMediaEmpty) {
            return null;
        }
        TLRPC.MessageMedia m = message.media;
        if (m instanceof TLRPC.TL_messageMediaPhoto) {
            return "📷 " + LocaleController.getString("AttachPhoto", R.string.AttachPhoto);
        } else if (m instanceof TLRPC.TL_messageMediaDocument) {
            if (m.document != null) {
                for (TLRPC.DocumentAttribute attr : m.document.attributes) {
                    if (attr instanceof TLRPC.TL_documentAttributeAudio) {
                        if (attr.voice) {
                            return "🎤 " + LocaleController.getString("AttachAudio", R.string.AttachAudio);
                        } else {
                            return "🎵 " + LocaleController.getString("AttachMusic", R.string.AttachMusic);
                        }
                    } else if (attr instanceof TLRPC.TL_documentAttributeVideo) {
                        if (attr.round_message) {
                            return "⭕ " + LocaleController.getString("AttachRound", R.string.AttachRound);
                        } else {
                            return "🎬 " + LocaleController.getString("AttachVideo", R.string.AttachVideo);
                        }
                    } else if (attr instanceof TLRPC.TL_documentAttributeAnimated) {
                        return "GIF";
                    } else if (attr instanceof TLRPC.TL_documentAttributeSticker) {
                        return "🎭 " + LocaleController.getString("AttachSticker", R.string.AttachSticker);
                    }
                }
                String name = FileLoader.getDocumentFileName(m.document);
                if (!TextUtils.isEmpty(name)) {
                    return "📁 " + name;
                }
            }
            return "📁 " + LocaleController.getString("AttachDocument", R.string.AttachDocument);
        } else if (m instanceof TLRPC.TL_messageMediaPoll) {
            TLRPC.TL_messageMediaPoll pollMedia = (TLRPC.TL_messageMediaPoll) m;
            if (pollMedia.poll != null && pollMedia.poll.question != null) {
                return "📊 " + pollMedia.poll.question.text;
            }
            return "📊 " + LocaleController.getString("Poll", R.string.Poll);
        } else if (m instanceof TLRPC.TL_messageMediaGeo || m instanceof TLRPC.TL_messageMediaGeoLive) {
            return "📍 " + LocaleController.getString("AttachLocation", R.string.AttachLocation);
        } else if (m instanceof TLRPC.TL_messageMediaContact) {
            return "👤 " + LocaleController.getString("AttachContact", R.string.AttachContact);
        }
        return null;
    }

    private void copyTextToClipboard(CharSequence text) {
        if (!TextUtils.isEmpty(text)) {
            AndroidUtilities.addToClipboard(text);
            if (BulletinFactory.canShowBulletin(fragment)) {
                BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
            }
        }
    }
}
