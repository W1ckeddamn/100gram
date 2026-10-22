package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.HundredGramVaultManager;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.PhotoViewer;

import org.telegram.messenger.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        public boolean showDiff;

        public RevisionItem(TLRPC.Message message, int versionDate, boolean isCurrent, boolean isOriginal, boolean isDeleted, int revisionNumber) {
            this.message = message;
            this.versionDate = versionDate;
            this.isCurrent = isCurrent;
            this.isOriginal = isOriginal;
            this.isDeleted = isDeleted;
            this.revisionNumber = revisionNumber;
            this.showDiff = !isOriginal;
        }
    }

    public static void show(BaseFragment fragment, MessageObject messageObject) {
        if (fragment != null && messageObject != null) {
            new HundredGramEditHistoryBottomSheet(fragment, messageObject).show();
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
            items.add(UItem.asCustom(createRevisionCardView(getContext(), rev, i)));
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
        int iconRes;
        if (HundredGramConfig.deletedMarkIcon == HundredGramConfig.DELETED_MARK_CROSS) {
            iconRes = R.drawable.msg_clear_input;
        } else if (HundredGramConfig.deletedMarkIcon == HundredGramConfig.DELETED_MARK_EYE) {
            iconRes = R.drawable.msg_archive_hide;
        } else {
            iconRes = R.drawable.msg_delete;
        }
        icon.setImageResource(iconRes);
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

    private View createRevisionCardView(Context context, RevisionItem rev, int index) {
        FrameLayout root = new FrameLayout(context);
        root.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(5), AndroidUtilities.dp(14), AndroidUtilities.dp(5));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(AndroidUtilities.dp(12));
        cardBg.setColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));
        card.setBackground(cardBg);

        CharSequence copyText = getMessageText(rev.message);
        RevisionItem prevRev = (index + 1 < revisions.size()) ? revisions.get(index + 1) : null;
        CharSequence prevText = prevRev != null ? getMessageText(prevRev.message) : null;
        boolean diffAvailable = prevRev != null && !TextUtils.isEmpty(copyText) && !TextUtils.isEmpty(prevText) && !TextUtils.equals(copyText, prevText);

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

        TextView diffToggleBtn = null;
        if (diffAvailable) {
            diffToggleBtn = new TextView(context);
            diffToggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            diffToggleBtn.setTypeface(AndroidUtilities.bold());
            diffToggleBtn.setPadding(AndroidUtilities.dp(7), AndroidUtilities.dp(3), AndroidUtilities.dp(7), AndroidUtilities.dp(3));
            diffToggleBtn.setTextColor(Theme.getColor(Theme.key_dialogButton, resourcesProvider));
            GradientDrawable diffToggleBg = new GradientDrawable();
            diffToggleBg.setCornerRadius(AndroidUtilities.dp(6));
            diffToggleBg.setColor(Theme.getColor(Theme.key_dialogButton, resourcesProvider) & 0x22FFFFFF);
            diffToggleBtn.setBackground(diffToggleBg);
            diffToggleBtn.setText(rev.showDiff ? LocaleController.getString("EditHistoryPlain", R.string.EditHistoryPlain) : LocaleController.getString("EditHistoryDiff", R.string.EditHistoryDiff));
            header.addView(diffToggleBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        }

        // Date
        TextView dateView = new TextView(context);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        dateView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        if (rev.versionDate > 0) {
            String dateStr = LocaleController.formatDateTime(rev.versionDate, false);
            if (!rev.isOriginal && !revisions.isEmpty()) {
                int originalDate = revisions.get(revisions.size() - 1).versionDate;
                if (originalDate > 0 && rev.versionDate > originalDate) {
                    dateStr += " (" + formatDeltaTime(rev.versionDate - originalDate) + ")";
                }
            }
            dateView.setText(dateStr);
        }
        header.addView(dateView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        // Copy button
        ImageView copyBtn = new ImageView(context);
        copyBtn.setImageResource(R.drawable.msg_copy);
        copyBtn.setColorFilter(Theme.getColor(Theme.key_dialogIcon, resourcesProvider));
        copyBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), 1));
        copyBtn.setScaleType(ImageView.ScaleType.CENTER);
        copyBtn.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        copyBtn.setOnClickListener(v -> copyTextToClipboard(copyText));
        header.addView(copyBtn, LayoutHelper.createLinear(28, 28));

        card.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Media preview — determines type and renders appropriately
        CharSequence mediaLabel = getMediaDescription(rev.message);
        boolean hasMediaPreview = false;

        if (!TextUtils.isEmpty(mediaLabel) && rev.message.media != null
                && !(rev.message.media instanceof TLRPC.TL_messageMediaEmpty)) {

            long dialogId = messageObject.getDialogId();
            int mid = rev.message.id != 0 ? rev.message.id : messageObject.getId();

            File vaultFile = null;
            if (rev.isCurrent) {
                vaultFile = FileLoader.getInstance(currentAccount).getPathToMessage(rev.message, true);
                if (vaultFile == null || !vaultFile.exists()) {
                    vaultFile = HundredGramVaultManager.getInstance(currentAccount).getVaultFile(rev.message);
                }
            }
            if (vaultFile == null || !vaultFile.exists()) {
                vaultFile = HundredGramVaultManager.getInstance(currentAccount).getVaultFileForEdit(mid, dialogId, rev.versionDate);
            }
            if (vaultFile == null || !vaultFile.exists()) {
                vaultFile = FileLoader.getInstance(currentAccount).getPathToMessage(rev.message, true);
            }
            if (vaultFile == null || !vaultFile.exists()) {
                vaultFile = HundredGramVaultManager.getInstance(currentAccount).getVaultFile(rev.message);
            }
            if (vaultFile == null || !vaultFile.exists()) {
                String attachName = HundredGramVaultManager.getAttachFileName(rev.message);
                if (attachName != null) {
                    vaultFile = HundredGramVaultManager.getInstance(currentAccount).getVaultFileByName(attachName);
                }
            }

            if (isPhotoOrVideo(rev.message)) {
                // ── Photo / Video / GIF: render thumbnail ──────────────────────────────
                hasMediaPreview = true;
                FrameLayout thumbContainer = new FrameLayout(context);
                thumbContainer.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8),
                        0x18000000));

                BackupImageView thumbView = new BackupImageView(context);
                thumbView.setRoundRadius(AndroidUtilities.dp(8));

                final File finalVaultFile = vaultFile;
                final boolean isVideoFile = isVideoDocument(
                        rev.message.media instanceof TLRPC.TL_messageMediaDocument
                                ? rev.message.media.document : null);
                final int targetPx = AndroidUtilities.dp(320); // ~160dp × 2 for density

                // Background decode if local file exists, else use ImageLocation
                Utilities.globalQueue.postRunnable(() -> {
                    Bitmap bmp = null;
                    try {
                        if (isVideoFile && finalVaultFile != null && finalVaultFile.exists()) {
                            // Extract a single frame from the video
                            bmp = android.media.ThumbnailUtils.createVideoThumbnail(
                                    finalVaultFile.getAbsolutePath(),
                                    android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                        } else if (finalVaultFile != null && finalVaultFile.exists()) {
                            // Phase 1 — measure dimensions only (no alloc)
                            BitmapFactory.Options bounds = new BitmapFactory.Options();
                            bounds.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(finalVaultFile.getAbsolutePath(), bounds);
                            // Phase 2 — compute inSampleSize
                            int sampleSize = 1;
                            int w = bounds.outWidth, h = bounds.outHeight;
                            while (w > targetPx * 2 || h > targetPx * 2) {
                                sampleSize *= 2;
                                w /= 2;
                                h /= 2;
                            }
                            // Phase 3 — decode at reduced size with RGB_565
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inSampleSize = sampleSize;
                            opts.inPreferredConfig = Bitmap.Config.RGB_565;
                            bmp = BitmapFactory.decodeFile(finalVaultFile.getAbsolutePath(), opts);
                        }
                    } catch (Throwable ignore) {}
                    final Bitmap finalBmp = bmp;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (finalBmp != null) {
                            thumbView.setImageBitmap(finalBmp);
                        } else {
                            if (rev.message.media instanceof TLRPC.TL_messageMediaPhoto && rev.message.media.photo != null) {
                                TLRPC.Photo photo = rev.message.media.photo;
                                TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 480);
                                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 50);
                                thumbView.setImage(ImageLocation.getForPhoto(size, photo), "320_160", ImageLocation.getForPhoto(thumb, photo), "b", null, photo);
                            } else if (rev.message.media instanceof TLRPC.TL_messageMediaDocument && rev.message.media.document != null) {
                                TLRPC.Document doc = rev.message.media.document;
                                if (doc.thumbs != null && !doc.thumbs.isEmpty()) {
                                    TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(doc.thumbs, 320);
                                    thumbView.setImage(ImageLocation.getForDocument(thumb, doc), "320_160", (android.graphics.drawable.Drawable) null, null);
                                } else {
                                    thumbView.setBackgroundColor(0x20808080);
                                }
                            } else {
                                thumbView.setBackgroundColor(0x20808080);
                            }
                        }
                    });
                });

                thumbContainer.addView(thumbView,
                        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 160));

                // Media type badge (bottom-left)
                TextView mediaTypeBadge = new TextView(context);
                mediaTypeBadge.setText(mediaLabel);
                mediaTypeBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                mediaTypeBadge.setTypeface(AndroidUtilities.bold());
                mediaTypeBadge.setTextColor(0xFFFFFFFF);
                mediaTypeBadge.setBackgroundColor(0x99000000);
                mediaTypeBadge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(3),
                        AndroidUtilities.dp(6), AndroidUtilities.dp(3));
                thumbContainer.addView(mediaTypeBadge,
                        LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                                Gravity.BOTTOM | Gravity.START, 8, 0, 8, 6));

                // Play icon overlay for video
                if (isVideoFile) {
                    ImageView playIcon = new ImageView(context);
                    playIcon.setImageResource(R.drawable.ic_play);
                    playIcon.setAlpha(0.85f);
                    thumbContainer.addView(playIcon,
                            LayoutHelper.createFrame(48, 48, Gravity.CENTER));
                }

                // Tap → open in PhotoViewer
                thumbContainer.setOnClickListener(v -> {
                    try {
                        TLRPC.Message histMsg = rev.message;
                        histMsg.id = messageObject.getId();
                        histMsg.dialog_id = messageObject.getDialogId();
                        if (finalVaultFile != null && finalVaultFile.exists()) {
                            histMsg.attachPath = finalVaultFile.getAbsolutePath();
                        }
                        MessageObject histObj = new MessageObject(currentAccount, histMsg, false, false);
                        if (finalVaultFile != null && finalVaultFile.exists()) {
                            histObj.messageOwner.attachPath = finalVaultFile.getAbsolutePath();
                        }
                        ArrayList<MessageObject> list = new ArrayList<>();
                        list.add(histObj);
                        PhotoViewer.getInstance().setParentActivity(
                                fragment.getParentActivity(), resourcesProvider);
                        PhotoViewer.getInstance().openPhoto(list, 0,
                                messageObject.getDialogId(), 0, 0,
                                new PhotoViewer.EmptyPhotoViewerProvider());
                        dismiss();
                    } catch (Exception ignore) {}
                });

                card.addView(thumbContainer,
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 160, 0, 8, 0, 0));

            } else if (isAudioOrDocument(rev.message)) {
                // ── Audio / Voice / Document: file info row ────────────────────────────
                hasMediaPreview = true;
                LinearLayout attachRow = new LinearLayout(context);
                attachRow.setOrientation(LinearLayout.HORIZONTAL);
                attachRow.setGravity(Gravity.CENTER_VERTICAL);
                attachRow.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8),
                        AndroidUtilities.dp(10), AndroidUtilities.dp(8));
                GradientDrawable attachBg = new GradientDrawable();
                attachBg.setCornerRadius(AndroidUtilities.dp(8));
                attachBg.setColor(0x12000000);
                attachRow.setBackground(attachBg);

                // File type icon
                ImageView fileIcon = new ImageView(context);
                int iconRes = getDocumentIconRes(rev.message);
                fileIcon.setImageResource(iconRes);
                fileIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider));
                attachRow.addView(fileIcon,
                        LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

                // File name + size column
                LinearLayout fileInfo = new LinearLayout(context);
                fileInfo.setOrientation(LinearLayout.VERTICAL);

                TextView fileNameView = new TextView(context);
                String displayName = getDocumentDisplayName(rev.message, vaultFile);
                fileNameView.setText(displayName);
                fileNameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                fileNameView.setTypeface(AndroidUtilities.bold());
                fileNameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
                fileNameView.setMaxLines(1);
                fileNameView.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
                fileInfo.addView(fileNameView,
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                long fileSize = (vaultFile != null && vaultFile.exists()) ? vaultFile.length() :
                        (rev.message.media != null && rev.message.media.document != null ? rev.message.media.document.size : 0);

                TextView fileSizeView = new TextView(context);
                fileSizeView.setText((fileSize > 0 ? AndroidUtilities.formatFileSize(fileSize) + " · " : "") + mediaLabel);
                fileSizeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                fileSizeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
                fileInfo.addView(fileSizeView,
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

                attachRow.addView(fileInfo, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

                final File finalDocVaultFile = vaultFile;
                attachRow.setOnClickListener(v -> {
                    if (finalDocVaultFile != null && finalDocVaultFile.exists()) {
                        AndroidUtilities.openForView(finalDocVaultFile, displayName,
                                rev.message.media != null && rev.message.media.document != null ? rev.message.media.document.mime_type : null,
                                fragment.getParentActivity(), resourcesProvider, false);
                    } else if (rev.message.media != null && rev.message.media.document != null) {
                        MessageObject docObj = new MessageObject(currentAccount, rev.message, false, false);
                        FileLoader.getInstance(currentAccount).loadFile(rev.message.media.document, docObj, 1, 0);
                        android.widget.Toast.makeText(context, LocaleController.getString(R.string.Downloading), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });

                card.addView(attachRow,
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

            } else if (!TextUtils.isEmpty(mediaLabel)) {
                // Fallback text label (no vault file, or unsupported type like poll/location)
                TextView mediaView = new TextView(context);
                mediaView.setText(mediaLabel);
                mediaView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                mediaView.setTypeface(AndroidUtilities.bold());
                mediaView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider));
                card.addView(mediaView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                        LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
            }
        } else if (!TextUtils.isEmpty(mediaLabel)) {
            // Non-visual media with no vault file (audio, poll, location, contact) — text label only
            TextView mediaView = new TextView(context);
            mediaView.setText(mediaLabel);
            mediaView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            mediaView.setTypeface(AndroidUtilities.bold());
            mediaView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider));
            card.addView(mediaView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
        }

        // Diff Legend (if diff is available)
        TextView legendView = null;
        SpannableStringBuilder diffSpannable = null;
        if (diffAvailable) {
            boolean isDark = Theme.isCurrentThemeDark();
            diffSpannable = computeWordDiff(prevText.toString(), copyText.toString(), isDark);

            legendView = new TextView(context);
            legendView.setText(LocaleController.getString("EditHistoryDiffLegend", R.string.EditHistoryDiffLegend));
            legendView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            legendView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            legendView.setVisibility(rev.showDiff ? View.VISIBLE : View.GONE);
            card.addView(legendView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
        }

        // Text Content
        TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setTextIsSelectable(true);

        if (diffAvailable && rev.showDiff && diffSpannable != null) {
            textView.setText(diffSpannable);
        } else if (!TextUtils.isEmpty(copyText)) {
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

        if (diffAvailable && diffToggleBtn != null) {
            final TextView finalLegend = legendView;
            final TextView finalToggle = diffToggleBtn;
            final SpannableStringBuilder finalDiff = diffSpannable;
            diffToggleBtn.setOnClickListener(v -> {
                rev.showDiff = !rev.showDiff;
                finalToggle.setText(rev.showDiff ? LocaleController.getString("EditHistoryPlain", R.string.EditHistoryPlain) : LocaleController.getString("EditHistoryDiff", R.string.EditHistoryDiff));
                if (finalLegend != null) {
                    finalLegend.setVisibility(rev.showDiff ? View.VISIBLE : View.GONE);
                }
                if (rev.showDiff && finalDiff != null) {
                    textView.setText(finalDiff);
                } else {
                    textView.setText(copyText);
                }
            });
        }

        card.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        // Click on entire card copies text (but not if user tapped the media preview)
        if (!hasMediaPreview) {
            card.setOnClickListener(v -> copyTextToClipboard(copyText));
        }

        root.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return root;
    }

    /** Returns true if the message media is a photo, video, gif, or round video (previewable). */
    private boolean isPhotoOrVideo(TLRPC.Message message) {
        if (message == null || message.media == null) return false;
        if (message.media instanceof TLRPC.TL_messageMediaPhoto) return true;
        if (message.media instanceof TLRPC.TL_messageMediaDocument && message.media.document != null) {
            for (TLRPC.DocumentAttribute attr : message.media.document.attributes) {
                if (attr instanceof TLRPC.TL_documentAttributeVideo
                        || attr instanceof TLRPC.TL_documentAttributeAnimated) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns true if the document is a video (including round video). */
    private boolean isVideoDocument(TLRPC.Document doc) {
        if (doc == null) return false;
        for (TLRPC.DocumentAttribute attr : doc.attributes) {
            if (attr instanceof TLRPC.TL_documentAttributeVideo) return true;
        }
        return false;
    }

    /** Returns true if the media is audio, voice, round video, or a generic document (not visual). */
    private boolean isAudioOrDocument(TLRPC.Message message) {
        if (message == null || message.media == null) return false;
        if (!(message.media instanceof TLRPC.TL_messageMediaDocument)) return false;
        if (message.media.document == null) return false;
        for (TLRPC.DocumentAttribute attr : message.media.document.attributes) {
            if (attr instanceof TLRPC.TL_documentAttributeAudio) return true;
            if (attr instanceof TLRPC.TL_documentAttributeVideo && ((TLRPC.TL_documentAttributeVideo) attr).round_message) return true;
        }
        // Generic document (PDF, ZIP, etc.)
        return true;
    }

    /** Returns appropriate icon resource for the document type. */
    private int getDocumentIconRes(TLRPC.Message message) {
        if (message == null || message.media == null || message.media.document == null) {
            return R.drawable.msg_filehq;
        }
        for (TLRPC.DocumentAttribute attr : message.media.document.attributes) {
            if (attr instanceof TLRPC.TL_documentAttributeAudio) {
                return ((TLRPC.TL_documentAttributeAudio) attr).voice
                        ? R.drawable.msg_filled_data_voice : R.drawable.msg_filled_data_music;
            }
            if (attr instanceof TLRPC.TL_documentAttributeVideo) {
                return R.drawable.msg_video;
            }
        }
        return R.drawable.msg_filehq;
    }

    /** Returns a human-readable display name for the document. */
    private String getDocumentDisplayName(TLRPC.Message message, File fallbackFile) {
        if (message != null && message.media != null && message.media.document != null) {
            String name = FileLoader.getDocumentFileName(message.media.document);
            if (!TextUtils.isEmpty(name)) return name;
        }
        if (fallbackFile != null) {
            String name = fallbackFile.getName();
            // Strip the "edit_{mid}_{date}_" prefix we added when saving
            int lastUnderscore = -1;
            if (name.startsWith("edit_")) {
                int count = 0;
                for (int i = 0; i < name.length(); i++) {
                    if (name.charAt(i) == '_') {
                        count++;
                        if (count == 3) { lastUnderscore = i; break; }
                    }
                }
            }
            return lastUnderscore >= 0 ? name.substring(lastUnderscore + 1) : name;
        }
        return "file";
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

    private CharSequence createMediaSpan(int iconRes, String title) {
        if (title == null) return null;
        SpannableStringBuilder ssb = new SpannableStringBuilder("  " + title);
        ColoredImageSpan span = new ColoredImageSpan(iconRes);
        span.setSize(AndroidUtilities.dp(14));
        span.translate(0, AndroidUtilities.dp(-1));
        ssb.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    private CharSequence getMediaDescription(TLRPC.Message message) {
        if (message == null || message.media == null || message.media instanceof TLRPC.TL_messageMediaEmpty) {
            return null;
        }
        TLRPC.MessageMedia m = message.media;
        if (m instanceof TLRPC.TL_messageMediaPhoto) {
            return createMediaSpan(R.drawable.msg_photos, LocaleController.getString("AttachPhoto", R.string.AttachPhoto));
        } else if (m instanceof TLRPC.TL_messageMediaDocument) {
            if (m.document != null) {
                for (TLRPC.DocumentAttribute attr : m.document.attributes) {
                    if (attr instanceof TLRPC.TL_documentAttributeAudio) {
                        if (attr.voice) {
                            return createMediaSpan(R.drawable.msg_filled_data_voice, LocaleController.getString("AttachAudio", R.string.AttachAudio));
                        } else {
                            return createMediaSpan(R.drawable.msg_filled_data_music, LocaleController.getString("AttachMusic", R.string.AttachMusic));
                        }
                    } else if (attr instanceof TLRPC.TL_documentAttributeVideo) {
                        if (attr.round_message) {
                            return createMediaSpan(R.drawable.msg_video, LocaleController.getString("AttachRound", R.string.AttachRound));
                        } else {
                            return createMediaSpan(R.drawable.msg_video, LocaleController.getString("AttachVideo", R.string.AttachVideo));
                        }
                    } else if (attr instanceof TLRPC.TL_documentAttributeAnimated) {
                        return createMediaSpan(R.drawable.msg_gif, "GIF");
                    } else if (attr instanceof TLRPC.TL_documentAttributeSticker) {
                        return createMediaSpan(R.drawable.msg_photo_sticker, LocaleController.getString("AttachSticker", R.string.AttachSticker));
                    }
                }
                String name = FileLoader.getDocumentFileName(m.document);
                if (!TextUtils.isEmpty(name)) {
                    return createMediaSpan(R.drawable.msg_filehq, name);
                }
            }
            return createMediaSpan(R.drawable.msg_filehq, LocaleController.getString("AttachDocument", R.string.AttachDocument));
        } else if (m instanceof TLRPC.TL_messageMediaPoll) {
            TLRPC.TL_messageMediaPoll pollMedia = (TLRPC.TL_messageMediaPoll) m;
            if (pollMedia.poll != null && pollMedia.poll.question != null) {
                return createMediaSpan(R.drawable.msg_list, pollMedia.poll.question.text);
            }
            return createMediaSpan(R.drawable.msg_list, LocaleController.getString("Poll", R.string.Poll));
        } else if (m instanceof TLRPC.TL_messageMediaGeo || m instanceof TLRPC.TL_messageMediaGeoLive) {
            return createMediaSpan(R.drawable.msg_location, LocaleController.getString("AttachLocation", R.string.AttachLocation));
        } else if (m instanceof TLRPC.TL_messageMediaContact) {
            return createMediaSpan(R.drawable.msg_contacts, LocaleController.getString("AttachContact", R.string.AttachContact));
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

    private enum DiffType {
        EQUAL, INSERT, DELETE
    }

    private static class DiffChunk {
        final DiffType type;
        final String text;

        DiffChunk(DiffType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private static String formatDeltaTime(int seconds) {
        if (seconds < 60) {
            return "+" + Math.max(1, seconds) + "s";
        } else if (seconds < 3600) {
            return "+" + (seconds / 60) + "m";
        } else if (seconds < 86400) {
            return "+" + (seconds / 3600) + "h";
        } else {
            return "+" + (seconds / 86400) + "d";
        }
    }

    private static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        int len = text.length();
        int i = 0;
        while (i < len) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                int start = i;
                while (i < len && Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
                tokens.add(text.substring(start, i));
            } else if (Character.isLetterOrDigit(c)) {
                int start = i;
                while (i < len && Character.isLetterOrDigit(text.charAt(i))) {
                    i++;
                }
                tokens.add(text.substring(start, i));
            } else {
                tokens.add(String.valueOf(c));
                i++;
            }
        }
        return tokens;
    }

    private static SpannableStringBuilder computeWordDiff(String oldText, String newText, boolean isDark) {
        if (oldText == null) oldText = "";
        if (newText == null) newText = "";

        List<String> oldTokens = tokenize(oldText);
        List<String> newTokens = tokenize(newText);
        int m = oldTokens.size();
        int n = newTokens.size();

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if ((long) m * n > 40000L) {
            ssb.append(newText);
            return ssb;
        }

        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (oldTokens.get(i).equals(newTokens.get(j))) {
                    dp[i + 1][j + 1] = dp[i][j] + 1;
                } else {
                    dp[i + 1][j + 1] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        List<DiffChunk> chunks = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldTokens.get(i - 1).equals(newTokens.get(j - 1))) {
                chunks.add(new DiffChunk(DiffType.EQUAL, oldTokens.get(i - 1)));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                chunks.add(new DiffChunk(DiffType.INSERT, newTokens.get(j - 1)));
                j--;
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                chunks.add(new DiffChunk(DiffType.DELETE, oldTokens.get(i - 1)));
                i--;
            }
        }
        Collections.reverse(chunks);

        int addFg = isDark ? 0xFF81C784 : 0xFF2E7D32;
        int addBg = isDark ? 0x334CAF50 : 0x224CAF50;
        int delFg = isDark ? 0xFFE57373 : 0xFFC62828;
        int delBg = isDark ? 0x33E53935 : 0x22E53935;

        for (DiffChunk chunk : chunks) {
            int start = ssb.length();
            ssb.append(chunk.text);
            int end = ssb.length();
            if (chunk.type == DiffType.INSERT) {
                ssb.setSpan(new ForegroundColorSpan(addFg), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new BackgroundColorSpan(addBg), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (chunk.type == DiffType.DELETE) {
                ssb.setSpan(new ForegroundColorSpan(delFg), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new BackgroundColorSpan(delBg), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return ssb;
    }
}
