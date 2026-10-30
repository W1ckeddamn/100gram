package org.telegram.ui.Components;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramUtils;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Диалог создания стикер-цитаты из сообщений.
 * Позволяет настраивать отображение автора, аватара, времени и медиа,
 * а затем отправлять результат как стикер, копировать или делиться им.
 */
public class HundredGramQuoteStickerAlert extends BottomSheet {

    private final ChatActivity parentFragment;
    private final MessageObject messageObject;

    private FrameLayout quoteCard;
    private BackupImageView avatarImageView;
    private TextView nameTextView;
    private TextView timeTextView;
    private TextView messageTextView;
    private BackupImageView mediaImageView;

    private boolean hideName = false;
    private boolean hideAvatar = false;
    private boolean hideTime = false;
    private boolean hideMedia = false;

    public HundredGramQuoteStickerAlert(ChatActivity parentFragment, MessageObject messageObject) {
        super(parentFragment.getParentActivity(), false);
        this.parentFragment = parentFragment;
        this.messageObject = messageObject;

        Context context = getContext();
        setApplyBottomPadding(false);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        // Header Title
        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString("QuoteStickerTitle", R.string.QuoteStickerTitle));
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER);
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Quote Card Container
        quoteCard = new FrameLayout(context);
        quoteCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), getThemedColor(Theme.key_chat_inBubble)));
        quoteCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(14));

        LinearLayout cardContent = new LinearLayout(context);
        cardContent.setOrientation(LinearLayout.VERTICAL);

        // Author Header Row (Avatar + Name + Time)
        LinearLayout authorRow = new LinearLayout(context);
        authorRow.setOrientation(LinearLayout.HORIZONTAL);
        authorRow.setGravity(Gravity.CENTER_VERTICAL);

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(20));
        authorRow.addView(avatarImageView, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        LinearLayout nameTimeLayout = new LinearLayout(context);
        nameTimeLayout.setOrientation(LinearLayout.VERTICAL);

        nameTextView = new TextView(context);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        nameTextView.setTypeface(AndroidUtilities.bold());
        nameTimeLayout.addView(nameTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        timeTextView = new TextView(context);
        timeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        timeTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        nameTimeLayout.addView(timeTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        authorRow.addView(nameTimeLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        cardContent.addView(authorRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Message Text
        messageTextView = new TextView(context);
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        messageTextView.setTextColor(getThemedColor(Theme.key_chat_messageTextIn));
        cardContent.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

        // Media thumbnail if present
        final boolean hasMedia = messageObject.isPhoto() || messageObject.isVideo() || (messageObject.photoThumbs != null && !messageObject.photoThumbs.isEmpty());
        if (hasMedia) {
            mediaImageView = new BackupImageView(context);
            mediaImageView.setRoundRadius(AndroidUtilities.dp(12));
            cardContent.addView(mediaImageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 160, 0, 10, 0, 0));
        }

        quoteCard.addView(cardContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(quoteCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Populate Card Data
        populateCardData();

        // Customization Chips Row
        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipsContainer = new LinearLayout(context);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);

        TextView chipHideName = createChip(context, LocaleController.getString("QuoteHideName", R.string.QuoteHideName), hideName);
        chipHideName.setOnClickListener(v -> {
            hideName = !hideName;
            updateChip(chipHideName, hideName);
            nameTextView.setVisibility(hideName ? View.GONE : View.VISIBLE);
        });
        chipsContainer.addView(chipHideName, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        TextView chipHideAvatar = createChip(context, LocaleController.getString("QuoteHideAvatar", R.string.QuoteHideAvatar), hideAvatar);
        chipHideAvatar.setOnClickListener(v -> {
            hideAvatar = !hideAvatar;
            updateChip(chipHideAvatar, hideAvatar);
            avatarImageView.setVisibility(hideAvatar ? View.GONE : View.VISIBLE);
        });
        chipsContainer.addView(chipHideAvatar, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        TextView chipHideTime = createChip(context, LocaleController.getString("QuoteHideTime", R.string.QuoteHideTime), hideTime);
        chipHideTime.setOnClickListener(v -> {
            hideTime = !hideTime;
            updateChip(chipHideTime, hideTime);
            timeTextView.setVisibility(hideTime ? View.GONE : View.VISIBLE);
        });
        chipsContainer.addView(chipHideTime, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        if (hasMedia && mediaImageView != null) {
            TextView chipHideMedia = createChip(context, LocaleController.getString("QuoteHideMedia", R.string.QuoteHideMedia), hideMedia);
            chipHideMedia.setOnClickListener(v -> {
                hideMedia = !hideMedia;
                updateChip(chipHideMedia, hideMedia);
                mediaImageView.setVisibility(hideMedia ? View.GONE : View.VISIBLE);
            });
            chipsContainer.addView(chipHideMedia, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        }

        chipsScroll.addView(chipsContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        container.addView(chipsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // Action Buttons Row
        LinearLayout buttonsRow = new LinearLayout(context);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView sendButton = new TextView(context);
        sendButton.setText(LocaleController.getString("QuoteSendSticker", R.string.QuoteSendSticker));
        sendButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        sendButton.setTypeface(AndroidUtilities.bold());
        sendButton.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        sendButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_featuredStickers_addButton)));
        sendButton.setGravity(Gravity.CENTER);
        sendButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        sendButton.setOnClickListener(v -> sendQuoteAsSticker());
        buttonsRow.addView(sendButton, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER, 0, 0, 8, 0));

        TextView copyButton = new TextView(context);
        copyButton.setText(LocaleController.getString("QuoteCopy", R.string.QuoteCopy));
        copyButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        copyButton.setTypeface(AndroidUtilities.bold());
        copyButton.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        copyButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.multAlpha(getThemedColor(Theme.key_dialogTextBlack), 0.1f)));
        copyButton.setGravity(Gravity.CENTER);
        copyButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        copyButton.setOnClickListener(v -> copyQuoteBitmap());
        buttonsRow.addView(copyButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        TextView shareButton = new TextView(context);
        shareButton.setText(LocaleController.getString("QuoteShare", R.string.QuoteShare));
        shareButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        shareButton.setTypeface(AndroidUtilities.bold());
        shareButton.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        shareButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.multAlpha(getThemedColor(Theme.key_dialogTextBlack), 0.1f)));
        shareButton.setGravity(Gravity.CENTER);
        shareButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        shareButton.setOnClickListener(v -> shareQuoteBitmap());
        buttonsRow.addView(shareButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

        container.addView(buttonsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollView.addView(container);
        setCustomView(scrollView);
    }

    private void populateCardData() {
        int currentAccount = parentFragment.getCurrentAccount();
        TLRPC.User user = null;
        TLRPC.Chat chat = null;

        if (messageObject.messageOwner != null && messageObject.messageOwner.from_id != null) {
            long fromId = MessageObject.getPeerId(messageObject.messageOwner.from_id);
            if (fromId > 0) {
                user = MessagesController.getInstance(currentAccount).getUser(fromId);
            } else {
                chat = MessagesController.getInstance(currentAccount).getChat(-fromId);
            }
        }
        if (user == null && chat == null) {
            long dialogId = messageObject.getDialogId();
            if (dialogId > 0) {
                user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            } else {
                chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            }
        }

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (user != null) {
            avatarDrawable.setInfo(currentAccount, user);
            avatarImageView.setForUserOrChat(user, avatarDrawable);
            nameTextView.setText(UserObject.getUserName(user));
        } else if (chat != null) {
            avatarDrawable.setInfo(currentAccount, chat);
            avatarImageView.setForUserOrChat(chat, avatarDrawable);
            nameTextView.setText(chat.title);
        } else {
            avatarDrawable.setInfo(5, "User", "");
            avatarImageView.setImageDrawable(avatarDrawable);
            nameTextView.setText("User");
        }

        int colorId = 0;
        if (user != null) {
            colorId = UserObject.getColorId(user);
        } else if (chat != null) {
            colorId = ChatObject.getColorId(chat);
        }
        int nameColor;
        if (colorId < 7) {
            nameColor = getThemedColor(Theme.keys_avatar_nameInMessage[colorId]);
        } else {
            MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).peerColors;
            MessagesController.PeerColor pc = peerColors != null ? peerColors.getColor(colorId) : null;
            nameColor = pc != null ? pc.getColor(0, null) : getThemedColor(Theme.keys_avatar_nameInMessage[0]);
        }
        nameTextView.setTextColor(nameColor);

        if (messageObject.messageOwner != null && messageObject.messageOwner.date != 0) {
            timeTextView.setText(LocaleController.formatDateChat(messageObject.messageOwner.date));
        } else {
            timeTextView.setText("");
        }

        CharSequence text = messageObject.messageText;
        if (TextUtils.isEmpty(text)) {
            text = messageObject.caption;
        }
        if (TextUtils.isEmpty(text) && messageObject.messageOwner != null) {
            text = messageObject.messageOwner.message;
        }
        messageTextView.setText(text != null ? text : "");

        if (mediaImageView != null) {
            if (messageObject.photoThumbs != null && !messageObject.photoThumbs.isEmpty()) {
                TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize());
                if (size != null) {
                    mediaImageView.setImage(ImageLocation.getForObject(size, messageObject.photoThumbsObject), "160_160", null, null, messageObject);
                }
            }
        }
    }

    private TextView createChip(Context context, String text, boolean active) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        chip.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        updateChip(chip, active);
        return chip;
    }

    private void updateChip(TextView chip, boolean active) {
        if (active) {
            chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_featuredStickers_addButton)));
            chip.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        } else {
            chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), Theme.multAlpha(getThemedColor(Theme.key_dialogTextBlack), 0.08f)));
            chip.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        }
    }

    private Bitmap renderCardBitmap() {
        int specW = View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(300), View.MeasureSpec.EXACTLY);
        int specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        quoteCard.measure(specW, specH);
        int width = quoteCard.getMeasuredWidth();
        int height = quoteCard.getMeasuredHeight();
        quoteCard.layout(0, 0, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        quoteCard.draw(canvas);
        return bitmap;
    }

    private Bitmap getStickerScaledBitmap(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        float maxDim = Math.max(w, h);
        if (maxDim <= 512) {
            return src;
        }
        float scale = 512.0f / maxDim;
        int targetW = Math.max(1, Math.round(w * scale));
        int targetH = Math.max(1, Math.round(h * scale));
        return Bitmap.createScaledBitmap(src, targetW, targetH, true);
    }

    private void sendQuoteAsSticker() {
        Bitmap cardBitmap = renderCardBitmap();
        Bitmap stickerBitmap = getStickerScaledBitmap(cardBitmap);

        Utilities.globalQueue.postRunnable(() -> {
            try {
                Context context = ApplicationLoader.applicationContext;
                File cacheDir = context.getCacheDir();
                File stickerFile = new File(cacheDir, "quote_sticker_" + System.currentTimeMillis() + ".webp");
                try (FileOutputStream fos = new FileOutputStream(stickerFile)) {
                    if (Build.VERSION.SDK_INT >= 30) {
                        stickerBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 92, fos);
                    } else {
                        stickerBitmap.compress(Bitmap.CompressFormat.WEBP, 92, fos);
                    }
                    fos.flush();
                }

                AndroidUtilities.runOnUIThread(() -> {
                    SendMessagesHelper.prepareSendingDocument(
                            parentFragment.getAccountInstance(),
                            stickerFile.getAbsolutePath(),
                            stickerFile.getAbsolutePath(),
                            null,
                            null,
                            "image/webp",
                            parentFragment.getDialogId(),
                            messageObject,
                            null,
                            null,
                            null,
                            null,
                            true,
                            0,
                            null,
                            null,
                            false
                    );
                    dismiss();
                    if (BulletinFactory.canShowBulletin(parentFragment)) {
                        BulletinFactory.of(parentFragment).createSimpleBulletin(R.drawable.msg_sticker, LocaleController.getString("QuoteStickerSent", R.string.QuoteStickerSent)).show();
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void copyQuoteBitmap() {
        Bitmap cardBitmap = renderCardBitmap();
        HundredGramUtils.copyBitmapToClipboard(cardBitmap, () -> {
            if (BulletinFactory.canShowBulletin(parentFragment)) {
                BulletinFactory.of(parentFragment).createCopyBulletin(LocaleController.getString("QuoteStickerCopied", R.string.QuoteStickerCopied)).show();
            }
            dismiss();
        });
    }

    private void shareQuoteBitmap() {
        Bitmap cardBitmap = renderCardBitmap();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                Context context = ApplicationLoader.applicationContext;
                File cacheDir = context.getCacheDir();
                File file = new File(cacheDir, "quote_" + System.currentTimeMillis() + ".png");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                }
                Uri uri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", file);

                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("image/png");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        parentFragment.getParentActivity().startActivity(Intent.createChooser(shareIntent, LocaleController.getString("QuoteShare", R.string.QuoteShare)));
                        dismiss();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }
}
